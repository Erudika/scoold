/*
 * Copyright 2013-2026 Erudika. https://erudika.com
 *
 * Licensed under the EULA - use is subject to license terms.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For issues and patches go to: https://github.com/erudika/scoold-pro
 */
package com.erudika.scoold.mcp;

import com.erudika.para.core.App;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.annotations.Documented;
import com.erudika.para.core.annotations.Stored;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.scoold.ScooldConfig;
import com.erudika.scoold.core.Profile;
import static com.erudika.scoold.mcp.MCPException.ErrorType.INTERNAL_ERROR;
import static com.erudika.scoold.mcp.MCPException.ErrorType.INVALID_INPUT;
import static com.erudika.scoold.mcp.MCPException.ErrorType.NOT_FOUND;
import com.erudika.scoold.utils.ScooldUtils;
import com.erudika.scoold.utils.Version;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * MCP helpers.
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Component
public class MCPUtils {

	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private final ScooldUtils utils;
	private static final int DEFAULT_SEARCH_LIMIT = 10;
	private static final int MAX_SEARCH_RESULTS = 30;
	private final List<ConfigEntry> orderedEntries;
	private final Map<String, ConfigEntry> configIndex;
	private CloseableHttpClient httpclient;
	private final Set<String> ignoredFields = Set.of("id", "timestamp", "appid", "parentid", "creatorid",
			"updated", "tags", "votes", "version", "stored", "indexed", "cached");

	/**
	 * Default constructor.
	 *
	 * @param utils Scoold utils
	 */
	public MCPUtils(ScooldUtils utils) {
		this.utils = utils;
		this.orderedEntries = Collections.unmodifiableList(buildConfigEntries());
		this.configIndex = orderedEntries.stream().
				collect(Collectors.toMap(entry -> entry.key().
				toLowerCase(Locale.ROOT), entry -> entry, (left, right) -> left, LinkedHashMap::new));
	}

	CloseableHttpClient getHttpClient() {
		if (httpclient == null) {
			int timeout = 30;
			httpclient = HttpClientBuilder.create().
					setDefaultRequestConfig(RequestConfig.custom().
							setConnectionRequestTimeout(timeout, TimeUnit.SECONDS).
							build()).
					build();
		}
		return httpclient;
	}

	Profile authUser() {
		try {
			return Objects.requireNonNull(utils.getAuthUser(getCurrentRequest()));
		} catch (Exception ex) {
			throw new MCPException(INVALID_INPUT, "Authentication required. Provide a valid Scoold API token.");
		}
	}

	String authenticateBearerToken() {
		if (!utils.isApiEnabled()) {
			throw new MCPException(NOT_FOUND, "Scoold API is disabled.");
		}
		HttpServletRequest req = getCurrentRequest();
		String token = Strings.CS.removeStart(req.getHeader("Authorization"), "Bearer ");
		if (!utils.isValidJWToken(token)) {
			throw new MCPException(INVALID_INPUT, "Authentication required. Provide a valid Scoold API token.");
		}
		return token;
	}

	HttpServletRequest getCurrentRequest() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null || attrs.getRequest() == null) {
			throw new MCPException(INVALID_INPUT, "No active HTTP request context available.");
		}
		return attrs.getRequest();
	}

	HttpServletResponse getCurrentResponse() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null || attrs.getResponse() == null) {
			throw new MCPException(INVALID_INPUT, "No active HTTP response context available.");
		}
		return attrs.getResponse();
	}

	static boolean isMcpEnabled() {
		return !CONF.mcpServerMode().equalsIgnoreCase("off");
	}

	static boolean isWriteEnabled() {
		return isMcpEnabled() && CONF.mcpServerMode().equalsIgnoreCase("rw");
	}

	static void requireWritePermission() {
		if (!isWriteEnabled()) {
			throw new MCPException(MCPException.ErrorType.INVALID_INPUT,
					"MCP server is in read-only mode - this operation requires 'rw' (read-write) mode.");
		}
	}

	String getMcpMode() {
		return CONF.mcpServerMode();
	}

	boolean isReadOnly() {
		return !isWriteEnabled();
	}

	Map<String, Object> serverMetadata(Profile authUser) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		Map<String, Object> server = new LinkedHashMap<>();
		App app = utils.getParaClient().me();
		server.put("port", CONF.serverPort());
		server.put("healthy", app != null);
		server.put("version", Version.getVersion());
		server.put("revision", Version.getRevision());
		snapshot.put("server", server);

		Map<String, Object> appInfo = new LinkedHashMap<>();
		appInfo.put("userId", authUser.getId());
		appInfo.put("userName", authUser.getName());
		appInfo.put("appId", app == null ? null : app.getId());
		server.put("authentication", appInfo);

		Map<String, Object> environment = new LinkedHashMap<>();
		environment.put("appName", CONF.appName());
		environment.put("environment", CONF.environment());
		environment.put("hostUrl", CONF.serverUrl());
		snapshot.put("environment", environment);

		Map<String, Object> features = new LinkedHashMap<>();
		features.put("apiEnabled", CONF.apiEnabled());
		features.put("apiUserAccessEnabled", CONF.apiUserAccessEnabled());
		features.put("isDefaultSpacePublic", CONF.isDefaultSpacePublic());
		features.put("webhooksEnabled", CONF.webhooksEnabled());
		features.put("corsEnabled", CONF.corsEnabled());
		snapshot.put("features", features);

		Map<String, Object> configMetadata = new LinkedHashMap<>();
		configMetadata.put("totalConfigurationProperties", orderedEntries.size());
		configMetadata.put("referenceResource", "scoold:///config");
		configMetadata.put("searchTool", "config_search");
		snapshot.put("configurationMetadata", configMetadata);

		snapshot.put("mcpTransport", resolveMcpTransportMetadata());
		snapshot.put("instructions", "Use config_search to search for configuration options,"
				+ "scoold:///config to read all the configuration documentation.");
		return snapshot;
	}

	Optional<Map<String, Object>> describeConfig(String identifier) {
		return resolveEntry(identifier).map(entry -> entry.toMap(readConfigValue(entry)));
	}

	List<Map<String, Object>> searchConfig(String query, Integer limit) {
		String needle = StringUtils.trimToEmpty(query).toLowerCase(Locale.ROOT);
		int normalizedLimit = normalizeLimit(limit);
		return orderedEntries.stream()
				.filter(entry -> entry.matches(needle))
				.limit(normalizedLimit)
				.map(entry -> entry.toMap(readConfigValue(entry)))
				.collect(Collectors.toList());
	}

	String renderConfigDocumentation() {
		return CONF.renderConfigDocumentation("markdown", true);
	}

	List<String> suggestions(String identifier, int limit) {
		String needle = StringUtils.trimToEmpty(identifier).toLowerCase(Locale.ROOT);
		int normalizedLimit = Math.max(1, limit);
		return orderedEntries.stream()
				.map(ConfigEntry::key)
				.filter(key -> needle.isEmpty() || key.toLowerCase(Locale.ROOT).contains(needle))
				.limit(normalizedLimit)
				.collect(Collectors.toList());
	}

	Map<String, Class<?>> getRequiredFields(ParaObject obj) {
		Map<String, Class<?>> required = new LinkedHashMap<>();
		Class<?> clazz = obj.getClass();
		while (clazz != null && !clazz.equals(Object.class)) {
			for (Field field : clazz.getDeclaredFields()) {
				String fieldName = field.getName();
				// Skip ignored fields
				if (ignoredFields.contains(fieldName)) {
					continue;
				}
				boolean isStored = field.isAnnotationPresent(Stored.class);
				boolean isRequired = field.isAnnotationPresent(NotBlank.class)
						|| field.isAnnotationPresent(NotEmpty.class)
						|| field.isAnnotationPresent(NotNull.class);

				if (isStored && isRequired && !required.containsKey(fieldName)) {
					required.put(fieldName, field.getType());
				}
			}
			clazz = clazz.getSuperclass();
		}
		return required;
	}

	static McpSchema.ReadResourceResult jsonResource(String uri, Object payload) {
		return textResource(uri, "application/json", toJson(payload));
	}

	static McpSchema.ReadResourceResult textResource(String uri, String mimeType, String body) {
		return new McpSchema.ReadResourceResult(List.of(new McpSchema.TextResourceContents(uri, mimeType, body)));
	}

	String buildUnknownKeyMessage(String key) {
		String safeKey = StringUtils.defaultIfBlank(key, "<empty>");
		List<String> suggestions = suggestions(key, 5);
		if (suggestions.isEmpty()) {
			return "Unknown configuration key: " + safeKey;
		}
		return "Unknown configuration key: " + safeKey + ". Did you mean: " + String.join(", ", suggestions) + "?";
	}

	static String toJson(Object payload) {
		try {
			return ParaObjectUtils.getJsonMapper().writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize MCP payload.", e);
		}
	}

	private Optional<ConfigEntry> resolveEntry(String identifier) {
		if (StringUtils.isBlank(identifier)) {
			return Optional.empty();
		}
		return Optional.ofNullable(configIndex.get(identifier.trim().toLowerCase(Locale.ROOT)));
	}

	private Object readConfigValue(ConfigEntry entry) {
		Object rawValue = CONF.getConfigValue(entry.key(), entry.defaultValue());
		return maskIfSensitive(entry, rawValue);
	}

	private Object maskIfSensitive(ConfigEntry entry, Object rawValue) {
		if (rawValue == null) {
			return null;
		}
		if (isSensitiveData(entry.key())) {
			return "<redacted>";
		}
		// If value looks like a credential pattern, redact it
		if (rawValue instanceof String strValue) {
			// Redact values that look like tokens (long alphanumeric strings)
			if (strValue.length() > 32 && strValue.matches("[A-Za-z0-9+/=_-]+")) {
				return "<redacted>";
			}
		}
		return rawValue;
	}

	/**
	 * Simple heuristic for determining if a config key contains sensistive data.
	 *
	 * @param configKey config key
	 * @return true if key name contains: "secret", "credential", "sensitive", "password", "pass", "privatekey".
	 */
	public static boolean isSensitiveData(String configKey) {
		return Strings.CI.containsAny(configKey, "secret", "credential", "sensitive", "password", "pass", "privatekey");
	}

	private int normalizeLimit(Integer limit) {
		if (limit == null || limit < 1) {
			return DEFAULT_SEARCH_LIMIT;
		}
		return Math.min(limit, MAX_SEARCH_RESULTS);
	}

	private Map<String, Object> resolveMcpTransportMetadata() {
		Map<String, Object> transport = new LinkedHashMap<>();
		transport.put("protocol", "streamable");
		transport.put("baseUrl", apiBaseUrl() + "/mcp");
		return transport;
	}

	private List<ConfigEntry> buildConfigEntries() {
		String prefix = CONF.getConfigRootPrefix();
		return Arrays.stream(CONF.getClass().getMethods())
				.filter(method -> method.isAnnotationPresent(Documented.class))
				.map(method -> method.getAnnotation(Documented.class))
				.filter(doc -> StringUtils.isNotBlank(doc.identifier()))
				.sorted(Comparator.comparingInt(Documented::position))
				.map(doc -> new ConfigEntry(prefix, doc))
				.collect(Collectors.toList());
	}


	String apiBaseUrl() {
		return Strings.CS.appendIfMissing(CONF.serverUrl() + CONF.serverContextPath(), "/") + "api";
	}

	McpSchema.CallToolResult asStructuredResult(Object data) {
		if (data instanceof Map) {
			return McpSchema.CallToolResult.builder().structuredContent(data).build();
		}
		if (data instanceof List) {
			Map<String, Object> wrapped = new LinkedHashMap<>();
			wrapped.put("items", data);
			return McpSchema.CallToolResult.builder().structuredContent(wrapped).build();
		}
		return McpSchema.CallToolResult.builder().structuredContent(Map.of("result", data)).build();
	}

	String normalizeMethod(String method) {
		return StringUtils.upperCase(StringUtils.defaultIfBlank(method, "GET"), Locale.ROOT);
	}

	Map<String, String> listAllTools() {
		return Arrays.stream(MCPTools.class.getDeclaredMethods())
				.filter(method -> method.isAnnotationPresent(McpTool.class))
				.filter(method -> !isReadOnly() || !method.getAnnotation(McpTool.class).annotations().destructiveHint())
				.collect(Collectors.toMap(m1 -> m1.getAnnotation(McpTool.class).name(),
						m2 -> m2.getAnnotation(McpTool.class).description()));
	}

	Object call(String bearerToken, String method, String path, Map<String, Object> query, Object body) {
		URI uri = buildUrl(path, query);
		HttpUriRequestBase request = new HttpUriRequestBase(normalizeMethod(method), uri);
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
		request.addHeader(HttpHeaders.ACCEPT, "application/json");
		if (body != null) {
			request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
			request.setEntity(new StringEntity(serialize(body), ContentType.APPLICATION_JSON));
		}

		try {
			return getHttpClient().execute(request, (response) -> {
				int code = response.getCode();
				String responseBody = response.getEntity() == null ? ""
						: EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
				if (code < 200 || code >= 300) {
					throw new MCPException(INVALID_INPUT, "REST API call failed (" + code + "): " + responseBody);
				}
				if (StringUtils.isBlank(responseBody)) {
					return Collections.singletonMap("status", code);
				}
				String contentType = response.getFirstHeader(HttpHeaders.CONTENT_TYPE) == null ? ""
						: response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue();
				if (Strings.CI.contains(contentType, "application/json") || looksLikeJson(responseBody)) {
					return ParaObjectUtils.getJsonReader(Object.class).readValue(responseBody);
				}
				Map<String, Object> result = new LinkedHashMap<>();
				result.put("status", code);
				result.put("contentType", contentType);
				result.put("body", responseBody);
				return result;
			});
		} catch (MCPException e) {
			throw e;
		} catch (Exception e) {
			throw new MCPException(INTERNAL_ERROR, "REST API call failed: " + e.getMessage(), e);
		}
	}

	URI buildUrl(String path, Map<String, Object> query) {
		try {
			String cleanPath = Strings.CS.prependIfMissing(StringUtils.defaultIfBlank(path, ""), "/");
			String root = apiBaseUrl();
			if (cleanPath.startsWith("/api/")) {
				cleanPath = Strings.CS.removeStart(cleanPath, "/api");
			} else if ("/api".equals(cleanPath)) {
				cleanPath = "";
			}
			URIBuilder builder = new URIBuilder(root + cleanPath);
			if (query != null) {
				query.forEach((k, v) -> {
					if (v != null && !StringUtils.isBlank(k)) {
						builder.addParameter(k, String.valueOf(v));
					}
				});
			}
			return builder.build();
		} catch (Exception e) {
			throw new MCPException(INTERNAL_ERROR, "Failed to build API URL for path '" + path + "'.", e);
		}
	}

	String serialize(Object body) {
		try {
			return ParaObjectUtils.getJsonWriterNoIdent().writeValueAsString(body);
		} catch (Exception e) {
			throw new MCPException(INTERNAL_ERROR, "Failed to serialize request body.", e);
		}
	}

	boolean looksLikeJson(String value) {
		String trimmed = StringUtils.trimToEmpty(value);
		return trimmed.startsWith("{") || trimmed.startsWith("[");
	}

	McpSchema.CallToolResult rebuildIndex() {
		try {
			utils.getParaClient().rebuildIndex();
			return McpSchema.CallToolResult.builder().textContent(List.of("Search index rebuilt.")).build();
		} catch (MCPException e) {
			throw e;
		} catch (Exception e) {
			throw new MCPException(INTERNAL_ERROR, "Failed to rebuild search index: " + e.getMessage(), e);
		}
	}

	String getAppId() {
		return App.identifier(CONF.paraAccessKey());
	}

	private static final class ConfigEntry {

		private final String key;
		private final String prefixedKey;
		private final String category;
		private final String description;
		private final String defaultValue;
		private final Class<?> type;
		private final List<String> tags;
		private final int position;
		private final String searchText;

		ConfigEntry(String rootPrefix, Documented doc) {
			this.key = doc.identifier();
			this.prefixedKey = StringUtils.isBlank(rootPrefix) ? key : rootPrefix + "." + key;
			this.category = doc.category();
			this.description = doc.description();
			this.defaultValue = doc.value();
			this.type = doc.type();
			this.tags = Collections.unmodifiableList(Arrays.stream(doc.tags())
					.map(String::trim)
					.filter(StringUtils::isNotBlank)
					.collect(Collectors.toList()));
			this.position = doc.position();
			this.searchText = (key + " " + description + " " + category + " "
					+ String.join(" ", tags)).toLowerCase(Locale.ROOT);
		}

		String key() {
			return key;
		}

		String defaultValue() {
			return defaultValue;
		}

		List<String> tags() {
			return tags;
		}

		boolean matches(String needle) {
			return StringUtils.isBlank(needle) || searchText.contains(needle);
		}

		Map<String, Object> toMap(Object sanitizedValue) {
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("key", key);
			payload.put("prefixedKey", prefixedKey);
			payload.put("category", category);
			payload.put("description", description);
			payload.put("type", type.getSimpleName());
			payload.put("defaultValue", defaultValue);

			Set<String> orderedTags = new LinkedHashSet<>(tags);
			payload.put("tags", orderedTags);
			payload.put("requiresRestart", orderedTags.stream()
					.anyMatch(tag -> "requires restart".equalsIgnoreCase(tag)));
			payload.put("position", position);
			if (sanitizedValue != null) {
				payload.put("value", sanitizedValue);
			}
			return payload;
		}
	}
}
