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

import com.erudika.scoold.core.Profile;
import static com.erudika.scoold.mcp.MCPUtils.jsonResource;
import static com.erudika.scoold.mcp.MCPUtils.textResource;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

/**
 * Minimal MCP resources.
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Component
public class MCPResources {

	private static final Logger logger = LoggerFactory.getLogger(MCPResources.class);

	private final MCPUtils utils;

	/**
	 * Default constructor.
	 *
	 * @param utils MCP utils
	 */
	public MCPResources(MCPUtils utils) {
		this.utils = utils;
	}

	/**
	 * Returns the root index of available MCP resources and tools.
	 *
	 * @return the root resource result
	 */
	@McpResource(
			uri = "scoold:///",
			name = "scoold-index",
			title = "Scoold MCP Index",
			description = "Lists Scoold MCP server capability groups and tool mapping.",
			mimeType = "application/json")
	public ReadResourceResult readRoot() {
		Profile authUser = utils.authUser();
		Map<String, Object> index = new LinkedHashMap<>();
		index.put("server", "Scoold MCP Server");
		index.put("version", "1.0");
		index.put("userId", authUser.getId());
		index.put("userName", authUser.getName());
		index.put("mcpMode", utils.getMcpMode());
		index.put("tools", utils.listAllTools());

		return jsonResource("scoold:///", index);
	}

	/**
	 * Returns the latest health status and metadata for this Scoold server.
	 *
	 * @return the health resource result
	 */
	@McpResource(uri = "scoold:///metadata",
			name = "metadata",
			title = "Scoold Server Information, Health and Metadata",
			description = "Returns the latest health status and metadata for this Scoold server.",
			mimeType = "application/json")
	public ReadResourceResult readMetadata() {
		Profile authUser = utils.authUser();
		logger.debug("[MCP] Metadata resource accessed by user={}", authUser.getId());
		return jsonResource("scoold:///metadata", utils.serverMetadata(authUser));
	}

	/**
	 * Returns the configuration documentation in Markdown format.
	 *
	 * @return the config documentation resource result
	 */
	@McpResource(uri = "scoold:///config",
			name = "config-docs",
			title = "Scoold Configuration Reference",
			description = "Browse the complete Scoold configuration reference with all available settings, "
			+ "descriptions, and default values. Use this to explore what's configurable before using specific tools.",
			mimeType = "text/markdown")
	public ReadResourceResult renderConfigDocumentation() {
		Profile authUser = utils.authUser();
		logger.debug("[MCP] Config documentation accessed by user={}", authUser.getId());
		return textResource("scoold:///config", "text/markdown", utils.renderConfigDocumentation());
	}
}
