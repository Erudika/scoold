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

import com.erudika.para.core.Tag;
import com.erudika.para.core.User;
import com.erudika.para.core.utils.Config;
import static com.erudika.scoold.ScooldServer.REST_ENTITY_ATTRIBUTE;
import com.erudika.scoold.api.ApiController;
import com.erudika.scoold.core.Profile;
import static com.erudika.scoold.mcp.MCPException.ErrorType.INTERNAL_ERROR;
import static com.erudika.scoold.mcp.MCPException.ErrorType.INVALID_INPUT;
import static com.erudika.scoold.mcp.MCPException.ErrorType.NOT_FOUND;
import static com.erudika.scoold.mcp.MCPUtils.requireWritePermission;
import com.erudika.scoold.utils.ScooldUtils;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * Tools mapped to Scoold REST API features.
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Component
@SuppressWarnings("unchecked")
public class MCPTools {

	private static final Logger logger = LoggerFactory.getLogger(MCPTools.class);
	private final MCPUtils utils;
	private final ApiController api;

	public MCPTools(MCPUtils utils, ApiController api) {
		this.utils = utils;
		this.api = api;
	}

	@McpTool(
			name = "scoold_api_request",
			title = "Scoold API Request",
			description = "Generic Scoold REST API bridge for complete endpoint coverage not exposed by specialized "
			+ "tools. Supports custom method/path/query/body calls, including post (question, reply, sticky) flows; "
			+ "if a post type is required, allowed values are: question, reply, sticky, unapprovedquestion, "
			+ "unapprovedreply.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult apiRequest(
			@McpToolParam(description = "HTTP method (GET, POST, PATCH, PUT, DELETE).", required = true) String method,
			@McpToolParam(description = "API path relative to /api (for example /posts or /users/123).", required = true) String path,
			@McpToolParam(description = "Optional query params.", required = false) Map<String, Object> query,
			@McpToolParam(description = "Optional JSON body object.", required = false) Map<String, Object> body) {
		if (!"GET".equalsIgnoreCase(method)) {
			requireWritePermission();
		}
		String bearerToken = utils.authenticateBearerToken();
		Object result = utils.call(bearerToken, method, path, query, body);
		if (!"GET".equalsIgnoreCase(method)) {
			logger.info("[MCP] Successfully executed {} request on '{}'.", method.toUpperCase(), path);
		}
		return utils.asStructuredResult(result);
	}

	@McpTool(
			name = "get_stats",
			title = "Get Stats",
			description = "Returns aggregated platform counters, component/version metadata, and optionally recent "
			+ "server log lines for operational troubleshooting.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getStats(
			@McpToolParam(description = "Include server logs in the response.", required = false) Boolean includeLogs,
			@McpToolParam(description = "Maximum log lines when includeLogs=true.", required = false) Integer maxLogLines) {
		return utils.asStructuredResult(api.stats(includeLogs, maxLogLines, request()));
	}

	@McpTool(name = "config_search",
			title = "Search Scoold Configuration",
			description = "Searches Para/Scoold configuration metadata and returns the closest matching properties "
			+ "with descriptions and resolved values. Use this when you don't know the exact key name.",
			annotations = @McpTool.McpAnnotations(
					readOnlyHint = true,
					destructiveHint = false,
					idempotentHint = true,
					openWorldHint = false
			))
	public McpSchema.CallToolResult searchConfig(
			@McpToolParam(description = "Case-insensitive substring to match (empty returns the first entries).") String query,
			@McpToolParam(description = "Maximum number of matches to return (default 10, max 30).") Integer limit) {
		try {
			Profile authUser = utils.authUser();
			logger.debug("[MCP] Config search by user={} query={} limit={}", authUser.getId(), query, limit);

			List<Map<String, Object>> results = utils.searchConfig(query, limit);
			Map<String, Object> response = new LinkedHashMap<>();
			response.put("results", results);
			response.put("count", results.size());
			response.put("query", StringUtils.trimToEmpty(query));

			return McpSchema.CallToolResult.builder().structuredContent(response).build();
		} catch (MCPException e) {
			throw e;
		} catch (Exception e) {
			throw new MCPException(INTERNAL_ERROR, "Failed to search config: " + e.getMessage(), e);
		}
	}

	@McpTool(name = "get_config_by_key",
			title = "Get Configuration Property by Key",
			description = "Returns detailed metadata and current resolved value for one specific Para/Scoold "
			+ "configuration key. Use this for exact key lookups and validation.",
			annotations = @McpTool.McpAnnotations(
					readOnlyHint = true,
					destructiveHint = false,
					idempotentHint = true,
					openWorldHint = false
			))
	public McpSchema.CallToolResult getConfigurationByKey(
			@McpToolParam(description = "Configuration property key (e.g., 'para.mcp_server_mode').", required = true) String key) {
		try {
			Profile authUser = utils.authUser();
			String normalizedKey = StringUtils.trimToEmpty(key);

			if (StringUtils.isBlank(normalizedKey)) {
				throw new MCPException(INVALID_INPUT, "Configuration key cannot be empty");
			}

			logger.debug("[MCP] Get config by key={} by user={}", normalizedKey, authUser.getId());

			Map<String, Object> configEntry = utils.describeConfig(normalizedKey)
					.orElseThrow(() -> new MCPException(NOT_FOUND, utils.buildUnknownKeyMessage(normalizedKey)));

			return McpSchema.CallToolResult.builder().structuredContent(configEntry).build();
		} catch (MCPException e) {
			throw e;
		} catch (Exception e) {
			throw new MCPException(INTERNAL_ERROR, "Failed to get configuration: " + e.getMessage(), e);
		}
	}

	@McpTool(
			name = "get_search_results",
			title = "Get Search Results",
			description = "Searches objects by type and query string with pagination/sorting support. "
			+ "When a post type is expected, allowed values are: question, reply, sticky, unapprovedquestion, "
			+ "unapprovedreply.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getSearchResults(
			@McpToolParam(description = "Entity type (question, reply, comment, sticky, unapprovedquestion, "
					+ "unapprovedreply, profile, tag, revision, badge).", required = true) String type,
			@McpToolParam(description = "Search query.", required = true) String queryText,
			@McpToolParam(description = "Optional page number.", required = false) Integer page,
			@McpToolParam(description = "Optional page size limit. Max. 30.", required = false) Integer limit,
			@McpToolParam(description = "Optional sort field.", required = false) String sortby,
			@McpToolParam(description = "Optional sort order - descending if true.", required = false) Boolean desc) {
		return utils.asStructuredResult(api.search(safe(type), safe(queryText), "", page, limit, desc,
				sortby, sortby, request()));
	}

	@McpTool(
			name = "list_posts",
			title = "List Posts",
			description = "Lists post (question, reply, sticky) entries available through the API, with optional "
			+ "query-based filtering and pagination.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult listPosts(
			@McpToolParam(description = "Optional 'sortby' parameter - could be a property name like votes or "
					+ "properties.answercount or a category like activity, unanswered, unapproved "
					+ "(posts without an approved answer)", required = false) String sortby,
			@McpToolParam(description = "Optional filter field -  'favtags' or 'local'.", required = false) String filter,
			@McpToolParam(description = "Whether to include answers in the response or not.", required = false) Boolean includeReplies) {
		return utils.asStructuredResult(api.listQuestions(sortby, filter, includeReplies, request()));
	}

	@McpTool(
			name = "get_post",
			title = "Get Post",
			description = "Retrieves one post (question, reply, sticky) by ID, including API-provided details and "
			+ "related collections where applicable.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getPost(
			@McpToolParam(description = "Post ID.", required = true) String id,
			@McpToolParam(description = "Optional sort field.", required = false) String sortby) {
		return utils.asStructuredResult(api.getPost(safe(id), sortby, request(), response()));
	}

	@McpTool(
			name = "create_post",
			title = "Create Post",
			description = "Creates a new post (question, reply, sticky) from the payload. If a post type is "
			+ "specified, allowed values are: question, reply, sticky, unapprovedquestion, unapprovedreply.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult createPost(
			@McpToolParam(description = "The title of the post.", required = true) String title,
			@McpToolParam(description = "The body of the post.", required = true) String body,
			@McpToolParam(description = "Comma separated list of tags for the post.", required = false) String tags,
			@McpToolParam(description = "Body object with properties.", required = true) Map<String, Object> data) {
		requireWritePermission();
		data.put("title", title);
		data.put("body", body);
		data.put("tags", new LinkedList<>(Arrays.asList(StringUtils.trimToEmpty(tags).split("\\s*,\\s*"))));
		McpSchema.CallToolResult result = utils.asStructuredResult(api.createPost(request(data), response()));
		logger.info("[MCP] Successfully created post of type '{}' with title '{}'.",
				data.get("type"), data.get("title"));
		return result;
	}

	@McpTool(
			name = "update_post",
			title = "Update Post",
			description = "Updates an existing post (question, reply, sticky) by ID with the provided mutable "
			+ "fields (for example title, body, tags, or metadata).",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult updatePost(
			@McpToolParam(description = "Post ID.", required = true) String id,
			@McpToolParam(description = "Body object with properties.", required = true) Map<String, Object> data) {
		requireWritePermission();
		McpSchema.CallToolResult result = utils.asStructuredResult(api.updatePost(safe(id), request(data), response()));
		logger.info("[MCP] Successfully updated post '{}'.", id);
		return result;
	}

	@McpTool(
			name = "delete_post",
			title = "Delete Post",
			description = "Deletes a post (question, reply, sticky) by ID using moderation/permission rules.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult deletePost(
			@McpToolParam(description = "Post ID.", required = true) String id) {
		requireWritePermission();
		api.deletePost(safe(id), request(), response());
		logger.info("[MCP] Successfully deleted post '{}'.", id);
		return McpSchema.CallToolResult.builder().addTextContent("Deleted.").build();
	}

	@McpTool(
			name = "approve_post",
			title = "Approve Post",
			description = "Approves a pending post (question, reply, sticky) so it becomes visible as moderated "
			+ "content.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult approvePost(
			@McpToolParam(description = "Post ID.", required = true) String id) {
		requireWritePermission();
		api.approvePost(safe(id), request());
		logger.info("[MCP] Successfully approved post '{}'.", id);
		return McpSchema.CallToolResult.builder().addTextContent("Post approved.").build();
	}

	@McpTool(
			name = "accept_post_reply",
			title = "Accept Post Reply",
			description = "Marks a reply as accepted for a parent post (question, reply, sticky) using post ID and "
			+ "reply ID.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult acceptPostReply(
			@McpToolParam(description = "Post ID.", required = true) String id,
			@McpToolParam(description = "Reply ID.", required = true) String replyId) {
		requireWritePermission();
		api.acceptReply(safe(id), safe(replyId), request());
		logger.info("[MCP] Successfully accepted reply '{}' for post '{}'.", replyId, id);
		return McpSchema.CallToolResult.builder().addTextContent("Answer marked as accepted.").build();
	}

	@McpTool(
			name = "close_post",
			title = "Close Post",
			description = "Closes a post (question, reply, sticky), typically preventing further replies or edits "
			+ "based on policy.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult closePost(
			@McpToolParam(description = "Post ID.", required = true) String id) {
		requireWritePermission();
		api.closePost(safe(id), request());
		logger.info("[MCP] Successfully toggled closed state for post '{}'.", id);
		return McpSchema.CallToolResult.builder().addTextContent("Post marked as closed.").build();
	}

	@McpTool(
			name = "pin_post",
			title = "Pin Post",
			description = "Pins a post (question, reply, sticky) so it is prioritized in listing views.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult pinPost(
			@McpToolParam(description = "Post ID.", required = true) String id) {
		requireWritePermission();
		api.pinPost(safe(id), request());
		logger.info("[MCP] Successfully toggled pinned state for post '{}'.", id);
		return McpSchema.CallToolResult.builder().addTextContent("Post was pinned to the top (sticky).").build();
	}

	@McpTool(
			name = "restore_post_revision",
			title = "Restore Post Revision",
			description = "Restores a post (question, reply, sticky) to a specific historical revision.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult restorePostRevision(
			@McpToolParam(description = "Post ID.", required = true) String id,
			@McpToolParam(description = "Revision ID.", required = true) String revisionId) {
		requireWritePermission();
		api.restoreRevision(safe(id), safe(revisionId), request());
		logger.info("[MCP] Successfully restored revision '{}' for post '{}'.", revisionId, id);
		return McpSchema.CallToolResult.builder().addTextContent("Revision restored.").build();
	}

	@McpTool(
			name = "like_post",
			title = "Like Post",
			description = "Adds or toggles a favorite/like on a post (question, reply, sticky) in the current "
			+ "user context.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult likePost(
			@McpToolParam(description = "Post ID.", required = true) String id) {
		requireWritePermission();
		api.favPost(safe(id), request());
		logger.info("[MCP] Successfully toggled favorite state for post '{}'.", id);
		return McpSchema.CallToolResult.builder().addTextContent("Post was added to favorites.").build();
	}

	@McpTool(
			name = "vote_up_post",
			title = "Vote Up Post",
			description = "Registers an upvote on a post (question, reply, sticky), optionally using an explicit "
			+ "user vote context.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult voteUpPost(
			@McpToolParam(description = "Post ID.", required = true) String id,
			@McpToolParam(description = "Optional user ID for vote context.", required = false) String userId) {
		requireWritePermission();
		String effectiveUserId = StringUtils.defaultIfBlank(userId, utils.authUser().getId());
		api.upvotePost(safe(id), effectiveUserId, request());
		logger.info("[MCP] Successfully upvoted post '{}' as user '{}'.", id, effectiveUserId);
		return McpSchema.CallToolResult.builder().addTextContent("Post upvoted.").build();
	}

	@McpTool(
			name = "vote_down_post",
			title = "Vote Down Post",
			description = "Registers a downvote on a post (question, reply, sticky), optionally using an explicit "
			+ "user vote context.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult voteDownPost(
			@McpToolParam(description = "Post ID.", required = true) String id,
			@McpToolParam(description = "Optional user ID for vote context.", required = false) String userId) {
		requireWritePermission();
		String effectiveUserId = StringUtils.defaultIfBlank(userId, utils.authUser().getId());
		api.downvotePost(safe(id), effectiveUserId, request());
		logger.info("[MCP] Successfully downvoted post '{}' as user '{}'.", id, effectiveUserId);
		return McpSchema.CallToolResult.builder().addTextContent("Post dowvoted.").build();
	}

	@McpTool(
			name = "update_post_tags",
			title = "Update Post Tags",
			description = "Adds and/or removes tags on a post (question, reply, sticky) using payload fields such "
			+ "as `add` and `remove`.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult updatePostTags(
			@McpToolParam(description = "Post ID.", required = true) String id,
			@McpToolParam(description = "Body object with properties.", required = true) Map<String, Object> data) {
		requireWritePermission();
		McpSchema.CallToolResult result = utils.asStructuredResult(api.updatePostTags(safe(id), request(data), response()));
		logger.info("[MCP] Successfully updated tags for post '{}'.", id);
		return result;
	}

	@McpTool(
			name = "get_post_answers",
			title = "Get Post Answers",
			description = "Retrieves answers/replies associated with a parent post (question, reply, sticky).",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getPostAnswers(
			@McpToolParam(description = "Post ID.", required = true) String id) {
		return utils.asStructuredResult(api.getPostReplies(safe(id), request(), response()));
	}

	@McpTool(
			name = "get_post_comments",
			title = "Get Post Comments",
			description = "Retrieves comments associated with a post (question, reply, sticky), with optional "
			+ "query-based pagination and sorting.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getPostComments(
			@McpToolParam(description = "Post ID.", required = true) String id,
			@McpToolParam(description = "Optional page number.", required = false) Integer page,
			@McpToolParam(description = "Optional page size limit. Max. 30.", required = false) Integer limit,
			@McpToolParam(description = "Optional sort field.", required = false) String sortby,
			@McpToolParam(description = "Optional sort order - descending if true.", required = false) Boolean desc) {
		return utils.asStructuredResult(api.getPostComments(safe(id), limit, page, sortby, desc, request(), response()));
	}

	@McpTool(
			name = "get_post_revisions",
			title = "Get Post Revisions",
			description = "Retrieves revision history entries for a post (question, reply, sticky).",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getPostRevisions(
			@McpToolParam(description = "Post ID.", required = true) String id) {
		return utils.asStructuredResult(api.getPostRevisions(safe(id), request(), response()));
	}

	@McpTool(
			name = "list_users",
			title = "List Users",
			description = "Lists user profiles and linked account metadata, with optional filtering and pagination "
			+ "through query parameters.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult listUsers(
			@McpToolParam(description = "Optional badge tag field.", required = false) String tag,
			@McpToolParam(description = "Optional sort field.", required = false) String sortby,
			@McpToolParam(description = "Search query.", required = true) String queryText) {
		return utils.asStructuredResult(api.listUsers(tag, sortby, queryText, request()));
	}

	@McpTool(
			name = "get_user",
			title = "Get User",
			description = "Retrieves one user profile and linked user account details by user ID.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getUser(
			@McpToolParam(description = "User ID.", required = true) String id) {
		return utils.asStructuredResult(api.getUser(safe(id), request(), response()));
	}

	@McpTool(
			name = "create_user",
			title = "Create User",
			description = "Creates a new user/profile pair from the provided payload (identity, groups, spaces, and "
			+ "optional profile attributes). To assign spaces to users, use the spaces field. Otherwise the user will "
			+ "be assigned to the default space. The user field contains the core Para User corresponding "
			+ "to the new Profile object.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult createUser(
			@McpToolParam(description = "The person's name.", required = true) String name,
			@McpToolParam(description = "The person's email address.", required = true) String email,
			@McpToolParam(description = "Unique id from the identity provider or email.", required = true) String identifier,
			@McpToolParam(description = "Groups: users, mods or admins.", required = false) String groups,
			@McpToolParam(description = "Body object with properties.", required = true) Map<String, Object> data) {
		requireWritePermission();
		data.put("name", name);
		data.put("email", email);
		data.put("identifier", StringUtils.defaultIfBlank(identifier, email));
		data.put("groups", StringUtils.defaultIfBlank(groups, User.Groups.USERS.toString()));
		McpSchema.CallToolResult result = utils.asStructuredResult(api.createUser(request(data), response()));
		logger.info("[MCP] Successfully created user '{}' with email '{}'.", data.get("name"), data.get("email"));
		return result;
	}

	@McpTool(
			name = "update_user",
			title = "Update User",
			description = "Updates mutable fields for an existing user/profile, including settings and profile "
			+ "attributes where supported.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult updateUser(
			@McpToolParam(description = "User ID.", required = true) String id,
			@McpToolParam(description = "Body object with properties.", required = true) Map<String, Object> data) {
		requireWritePermission();
		McpSchema.CallToolResult result = utils.asStructuredResult(api.updateUser(safe(id), request(data), response()));
		logger.info("[MCP] Successfully updated user '{}'.", id);
		return result;
	}

	@McpTool(
			name = "delete_user",
			title = "Delete User",
			description = "Deletes a user/profile by ID according to platform permissions and safeguards.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult deleteUser(
			@McpToolParam(description = "User ID.", required = true) String id) {
		requireWritePermission();
		api.deleteUser(safe(id), request(), response());
		logger.info("[MCP] Successfully deleted user '{}'.", id);
		return McpSchema.CallToolResult.builder().addTextContent("User deleted.").build();
	}

	@McpTool(
			name = "get_user_questions",
			title = "Get User Questions",
			description = "Retrieves question post (question, reply, sticky) results authored by the given user.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getUserQuestions(
			@McpToolParam(description = "User ID.", required = true) String id,
			@McpToolParam(description = "Optional page number.", required = false) Integer page,
			@McpToolParam(description = "Optional page size limit. Max. 30.", required = false) Integer limit,
			@McpToolParam(description = "Optional sort field.", required = false) String sortby,
			@McpToolParam(description = "Optional sort order - descending if true.", required = false) Boolean desc) {
		return utils.asStructuredResult(api.getUserQuestions(safe(id), page, limit, desc,
				sortby, sortby, request(), response()));
	}

	@McpTool(
			name = "get_user_replies",
			title = "Get User Replies",
			description = "Retrieves reply post (question, reply, sticky) results authored by the given user.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getUserReplies(
			@McpToolParam(description = "User ID.", required = true) String id,
			@McpToolParam(description = "Optional page number.", required = false) Integer page,
			@McpToolParam(description = "Optional page size limit. Max. 30.", required = false) Integer limit,
			@McpToolParam(description = "Optional sort field.", required = false) String sortby,
			@McpToolParam(description = "Optional sort order - descending if true.", required = false) Boolean desc) {
		return utils.asStructuredResult(api.getUserReplies(safe(id), page, limit, desc,
				sortby, sortby, request(), response()));
	}

	@McpTool(
			name = "get_user_favorites",
			title = "Get User Favorites",
			description = "Retrieves the given user's favorited post (question, reply, sticky) entries and related "
			+ "metadata.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getUserFavorites(
			@McpToolParam(description = "User ID.", required = true) String id,
			@McpToolParam(description = "Optional page number.", required = false) Integer page,
			@McpToolParam(description = "Optional page size limit. Max. 30.", required = false) Integer limit,
			@McpToolParam(description = "Optional sort field.", required = false) String sortby,
			@McpToolParam(description = "Optional sort order - descending if true.", required = false) Boolean desc) {
		return utils.asStructuredResult(api.getUserFavorites(safe(id), page, limit, desc,
				sortby, sortby, request(), response()));
	}

	@McpTool(
			name = "update_user_moderator",
			title = "Update User Moderator",
			description = "Grants or updates moderator privileges for a user, optionally scoped by query parameters.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult updateUserModerator(
			@McpToolParam(description = "User ID.", required = true) String id,
			@McpToolParam(description = "A list of comma-separated spaces.", required = true) String spaces) {
		requireWritePermission();
		api.makeUserMod(safe(id), List.of(spaces.split("\\s*,\\s*")), request(), response());
		logger.info("[MCP] Successfully updated moderator privileges for user '{}'.", id);
		return McpSchema.CallToolResult.builder().addTextContent("User updated.").build();
	}

	@McpTool(
			name = "ban_user",
			title = "Ban User",
			description = "Bans or unbans a user, including temporary or indefinite ban flows based on query "
			+ "parameters.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult banUser(
			@McpToolParam(description = "User ID.", required = true) String id,
			@McpToolParam(description = "Ban until date string.", required = false) String banuntil,
			@McpToolParam(description = "Ban permanently.", required = false) Boolean permaBan) {
		requireWritePermission();
		api.banUser(safe(id), banuntil, permaBan, request(), response());
		logger.info("[MCP] Successfully updated ban state for user '{}'.", id);
		return McpSchema.CallToolResult.builder().addTextContent("User banned.").build();
	}

	@McpTool(
			name = "update_users_spaces",
			title = "Update Users Spaces",
			description = "Bulk-updates selected users' space assignments and badge associations in one operation.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult updateUsersSpaces(
			@McpToolParam(description = "Body object with properties.", required = true) Map<String, Object> data) {
		requireWritePermission();
		api.bulkEditSpaces(request(data), response());
		logger.info("[MCP] Successfully updated spaces for users.");
		return McpSchema.CallToolResult.builder().addTextContent("Spaces updated.").build();
	}

	@McpTool(
			name = "create_tag",
			title = "Create Tag",
			description = "Creates a new tag with optional metadata such as description and category fields.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult createTag(
			@McpToolParam(description = "The name of the tag.", required = true) String tag,
			@McpToolParam(description = "Body object with properties.", required = true) Map<String, Object> data) {
		requireWritePermission();
		data.put("tag", new Tag(tag).getId());
		McpSchema.CallToolResult result = utils.asStructuredResult(api.createTag(request(data), response()));
		logger.info("[MCP] Successfully created tag '{}'.", data.get("tag"));
		return result;
	}

	@McpTool(
			name = "list_tags",
			title = "List Tags",
			description = "Lists tags with optional sorting/filtering query parameters.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult listTags(
			@McpToolParam(description = "Optional sort field.", required = false) String sortby) {
		return utils.asStructuredResult(api.listTags(sortby, request(), response()));
	}

	@McpTool(
			name = "get_tag",
			title = "Get Tag",
			description = "Retrieves a single tag by ID including configured metadata.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getTag(
			@McpToolParam(description = "Tag ID.", required = true) String id) {
		return utils.asStructuredResult(api.getTag(safe(id), request(), response()));
	}

	@McpTool(
			name = "update_tag",
			title = "Update Tag",
			description = "Updates tag fields such as display name and description.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult updateTag(
			@McpToolParam(description = "Tag ID.", required = true) String id,
			@McpToolParam(description = "Body object with properties.", required = true) Map<String, Object> data) {
		requireWritePermission();
		McpSchema.CallToolResult result = utils.asStructuredResult(api.updateTag(safe(id), request(data), response()));
		logger.info("[MCP] Successfully updated tag '{}'.", id);
		return result;
	}

	@McpTool(
			name = "delete_tag",
			title = "Delete Tag",
			description = "Deletes a tag by ID and removes it from active tag listings.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult deleteTag(
			@McpToolParam(description = "Tag ID.", required = true) String id) {
		requireWritePermission();
		api.deleteTag(safe(id), request(), response());
		logger.info("[MCP] Successfully deleted tag '{}'.", id);
		return McpSchema.CallToolResult.builder().addTextContent("Tag deleted.").build();
	}

	@McpTool(
			name = "get_tag_questions",
			title = "Get Tag Questions",
			description = "Retrieves question post (question, reply, sticky) entries associated with a specific "
			+ "tag.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getTagQuestions(
			@McpToolParam(description = "Tag ID.", required = true) String id) {
		return utils.asStructuredResult(api.listTaggedQuestions(safe(id), request(), response()));
	}

	@McpTool(
			name = "create_comment",
			title = "Create Comment",
			description = "Creates a new comment on a supported parent entity using the provided payload.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult createComment(
			@McpToolParam(description = "The text value of the comment.", required = true) String comment,
			@McpToolParam(description = "The ID of the user who authored the comment.", required = true) String creatorid,
			@McpToolParam(description = "The ID of the post to attach the comment to.", required = true) String parentid,
			@McpToolParam(description = "Body object with properties.", required = true) Map<String, Object> data) {
		requireWritePermission();
		data.put("comment", comment);
		data.put("creatorid", creatorid);
		data.put("parentid", parentid);
		McpSchema.CallToolResult result = utils.asStructuredResult(api.createComment(request(data), response()));
		logger.info("[MCP] Successfully created comment for parent '{}'.", data.get("parentid"));
		return result;
	}

	@McpTool(
			name = "get_comment",
			title = "Get Comment",
			description = "Retrieves a comment by ID.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getComment(
			@McpToolParam(description = "Comment ID.", required = true) String id) {
		return utils.asStructuredResult(api.getComment(safe(id), request(), response()));
	}

	@McpTool(
			name = "delete_comment",
			title = "Delete Comment",
			description = "Deletes a comment by ID according to moderation permissions.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult deleteComment(
			@McpToolParam(description = "Comment ID.", required = true) String id) {
		requireWritePermission();
		api.deleteComment(safe(id), request(), response());
		logger.info("[MCP] Successfully deleted comment '{}'.", id);
		return McpSchema.CallToolResult.builder().addTextContent("Comment deleted.").build();
	}

	@McpTool(
			name = "create_report",
			title = "Create Report",
			description = "Creates a moderation report entry for content or user-related issues.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult createReport(
			@McpToolParam(description = "Report type: offensive, duplicate, spam, incorrect.", required = true) String type,
			@McpToolParam(description = "Report description.", required = true) String description,
			@McpToolParam(description = "Link to reported content.", required = false) String link,
			@McpToolParam(description = "Body object with properties.", required = true) Map<String, Object> data) {
		requireWritePermission();
		data.put("subType", type);
		data.put("description", description);
		data.put("link", link);
		data.put("creatorid", utils.authUser().getId());
		data.put("authorName", utils.authUser().getName());
		McpSchema.CallToolResult result = utils.asStructuredResult(api.createReport(request(data), response()));
		logger.info("[MCP] Successfully created report for parent '{}' and link '{}'.",
				data.get("parentid"), data.get("link"));
		return result;
	}

	@McpTool(
			name = "list_reports",
			title = "List Reports",
			description = "Lists moderation reports with optional sort/filter query parameters.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult listReports(
			@McpToolParam(description = "Optional sort field.", required = false) String sortby) {
		return utils.asStructuredResult(api.listReports(sortby, request(), response()));
	}

	@McpTool(
			name = "get_report",
			title = "Get Report",
			description = "Retrieves one moderation report by ID.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getReport(
			@McpToolParam(description = "Report ID.", required = true) String id) {
		return utils.asStructuredResult(api.getReport(safe(id), request(), response()));
	}

	@McpTool(
			name = "delete_report",
			title = "Delete Report",
			description = "Deletes a moderation report by ID.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult deleteReport(
			@McpToolParam(description = "Report ID.", required = true) String id) {
		requireWritePermission();
		api.deleteReport(safe(id), request(), response());
		logger.info("[MCP] Successfully deleted report '{}'.", id);
		return McpSchema.CallToolResult.builder().addTextContent("Report deleted.").build();
	}

	@McpTool(
			name = "close_report",
			title = "Close Report",
			description = "Closes a moderation report and optionally stores a resolution payload.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult closeReport(
			@McpToolParam(description = "Report ID.", required = true) String id,
			@McpToolParam(description = "Solution to the issue in the report.", required = false) String solution) {
		requireWritePermission();
		api.closeReport(safe(id), solution, request(), response());
		logger.info("[MCP] Successfully closed report '{}'.", id);
		return McpSchema.CallToolResult.builder().addTextContent("Report closed.").build();
	}

	@McpTool(
			name = "create_space",
			title = "Create Space",
			description = "Creates a new collaborative space with optional assignment behavior defined by query "
			+ "parameters.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult createSpace(
			@McpToolParam(description = "The name of the space.", required = true) String name,
			@McpToolParam(description = "Assign space to all users or not.", required = false) Boolean assigntoall,
			@McpToolParam(description = "Body object with properties.", required = true) Map<String, Object> data) {
		requireWritePermission();
		data.put("name", name);
		McpSchema.CallToolResult result = utils.asStructuredResult(api.createSpace(assigntoall, request(data), response()));
		logger.info("[MCP] Successfully created space '{}'.", data.get("name"));
		return result;
	}

	@McpTool(
			name = "update_space",
			title = "Update Space",
			description = "Updates space settings and/or name for an existing space ID.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult updateSpace(
			@McpToolParam(description = "Space ID.", required = true) String id,
			@McpToolParam(description = "Assign space to all users or not.", required = false) Boolean assigntoall,
			@McpToolParam(description = "Is posting need to be approved by mo"
					+ "derators in that spacee.", required = false) Boolean needsapproval,
			@McpToolParam(description = "Body object with properties.", required = true) Map<String, Object> data) {
		requireWritePermission();
		api.updateSpace(safe(id), assigntoall, needsapproval, request(data), response());
		logger.info("[MCP] Successfully updated space '{}'.", id);
		return McpSchema.CallToolResult.builder().addTextContent("Space updated.").build();
	}

	@McpTool(
			name = "list_spaces",
			title = "List Spaces",
			description = "Lists all spaces visible to the current API principal.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult listSpaces(
			@McpToolParam(description = "Optional page number.", required = false) Integer page,
			@McpToolParam(description = "Optional page size limit. Max. 30.", required = false) Integer limit,
			@McpToolParam(description = "Optional sort field.", required = false) String sortby,
			@McpToolParam(description = "Optional sort order - descending if true.", required = false) Boolean desc) {
		return utils.asStructuredResult(api.listSpaces(page, limit, desc, sortby, sortby, request(), response()));
	}

	@McpTool(
			name = "get_space",
			title = "Get Space",
			description = "Retrieves one space by ID with its persisted metadata.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getSpace(
			@McpToolParam(description = "Space ID.", required = true) String id) {
		return utils.asStructuredResult(api.getSpace(safe(id), request(), response()));
	}

	@McpTool(
			name = "delete_space",
			title = "Delete Space",
			description = "Deletes a space by ID and removes associated access context.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult deleteSpace(
			@McpToolParam(description = "Space ID.", required = true) String id) {
		requireWritePermission();
		api.deleteSpace(safe(id), request(), response());
		logger.info("[MCP] Successfully deleted space '{}'.", id);
		return McpSchema.CallToolResult.builder().addTextContent("Space deleted.").build();
	}

	@McpTool(
			name = "create_webhook",
			title = "Create Webhook",
			description = "Creates a webhook subscription with target URL, event, and delivery options from the "
			+ "payload.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult createWebhook(
			@McpToolParam(description = "Body object with properties.", required = true) Map<String, Object> data) {
		requireWritePermission();
		McpSchema.CallToolResult result = utils.asStructuredResult(api.createWebhook(request(data), response()));
		logger.info("[MCP] Successfully created webhook for target '{}'.", data.get("targetUrl"));
		return result;
	}

	@McpTool(
			name = "list_webhooks",
			title = "List Webhooks",
			description = "Lists configured webhooks and their core metadata.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult listWebhooks(
			@McpToolParam(description = "Optional page number.", required = false) Integer page,
			@McpToolParam(description = "Optional page size limit. Max. 30.", required = false) Integer limit,
			@McpToolParam(description = "Optional sort field.", required = false) String sortby,
			@McpToolParam(description = "Optional sort order - descending if true.", required = false) Boolean desc) {
		return utils.asStructuredResult(api.listWebhooks(page, limit, desc, sortby, sortby, request(), response()));
	}

	@McpTool(
			name = "get_webhook",
			title = "Get Webhook",
			description = "Retrieves one webhook by ID.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getWebhook(
			@McpToolParam(description = "Webhook ID.", required = true) String id) {
		return utils.asStructuredResult(api.getWebhook(safe(id), request(), response()));
	}

	@McpTool(
			name = "update_webhook",
			title = "Update Webhook",
			description = "Updates fields on an existing webhook (for example event, URL, active flag, or payload "
			+ "settings).",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult updateWebhook(
			@McpToolParam(description = "Webhook ID.", required = true) String id,
			@McpToolParam(description = "Body object with properties.", required = true) Map<String, Object> data) {
		requireWritePermission();
		McpSchema.CallToolResult result = utils.asStructuredResult(api.updateWebhook(safe(id), request(data), response()));
		logger.info("[MCP] Successfully updated webhook '{}'.", id);
		return result;
	}

	@McpTool(
			name = "delete_webhook",
			title = "Delete Webhook",
			description = "Deletes a webhook by ID and disables future deliveries.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult deleteWebhook(
			@McpToolParam(description = "Webhook ID.", required = true) String id) {
		requireWritePermission();
		api.deleteWebhook(safe(id), request(), response());
		logger.info("[MCP] Successfully deleted webhook '{}'.", id);
		return McpSchema.CallToolResult.builder().addTextContent("Webhook deleted.").build();
	}

	@McpTool(
			name = "get_config",
			title = "Get Config",
			description = "Retrieves the full runtime configuration in JSON or HOCON format.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getConfig(
			@McpToolParam(description = "Configuration format - hocon or json.", required = false) String format) {
		return utils.asStructuredResult(api.config(format, request(), response()));
	}

	@McpTool(
			name = "update_config",
			title = "Update Config",
			description = "Updates runtime configuration entries from the provided payload and persists changes "
			+ "through the Scoold config pipeline.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult updateConfig(
			@McpToolParam(description = "Body object with properties.", required = true) Map<String, Object> data) {
		requireWritePermission();
		McpSchema.CallToolResult result = utils.asStructuredResult(api.configSet(request(data), response()));
		logger.info("[MCP] Successfully updated '{}' configuration keys.", data.size());
		return result;
	}

	@McpTool(
			name = "get_config_key",
			title = "Get Config Key",
			description = "Retrieves the current value for one specific configuration key.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getConfigKey(
			@McpToolParam(description = "Config key.", required = true) String key) {
		return utils.asStructuredResult(api.configGet(safe(key), request(), response()));
	}

	@McpTool(
			name = "update_config_key",
			title = "Update Config Key",
			description = "Updates or clears one specific configuration key using the payload value.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
					idempotentHint = false, openWorldHint = false))
	public McpSchema.CallToolResult updateConfigKey(
			@McpToolParam(description = "Config key.", required = true) String key,
			@McpToolParam(description = "Body object with properties.", required = true) Map<String, Object> data) {
		requireWritePermission();
		api.configSet(safe(key), request(data), response());
		logger.info("[MCP] Successfully updated configuration key '{}'.", key);
		return McpSchema.CallToolResult.builder().addTextContent("Configuration updated.").build();
	}

	@McpTool(
			name = "get_config_options",
			title = "Get Config Options",
			description = "Retrieves generated configuration reference/options documentation in the requested format.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getConfigOptions(
			@McpToolParam(description = "Configuration format - hocon or json.", required = false) String format,
			@McpToolParam(description = "Group by - category or blank (alphabetic order).", required = false) String groupby) {
		return utils.asStructuredResult(api.configOptions(format, groupby, response()));
	}

	@McpTool(
			name = "get_hook_events",
			title = "Get Hook Events",
			description = "Lists all supported webhook event names available for webhook subscriptions.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getHookEvents() {
		return utils.asStructuredResult(api.listHookEvents(request(), response()));
	}

	@McpTool(
			name = "get_core_types",
			title = "Get Core Types",
			description = "Lists core Scoold object types exposed by the platform.",
			annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
					idempotentHint = true, openWorldHint = false))
	public McpSchema.CallToolResult getCoreTypes() {
		return utils.asStructuredResult(api.listCoreTypes(request(), response()));
	}

	@McpTool(name = "rebuild_index",
			title = "Rebuild Search Index",
			description = "Rebuilds the search index for all objects in the current app. This operation reindexes "
			+ "all objects from the database. Only available when MCP server is in read-write mode. "
			+ "Use this when search results are inconsistent or after bulk data changes.",
			annotations = @McpTool.McpAnnotations(
					readOnlyHint = false,
					destructiveHint = true,
					idempotentHint = true,
					openWorldHint = false
			))
	public McpSchema.CallToolResult rebuildIndex() {
		requireWritePermission();
		logger.info("[MCP] Rebuilding search index for app={}", utils.getAppId());
		return utils.rebuildIndex();
	}

	private HttpServletRequest request() {
		return utils.getCurrentRequest();
	}
	private HttpServletRequest request(Map<String, Object> entity) {
		HttpServletRequest req = utils.getCurrentRequest();
		if (entity != null) {
			Profile authUser = utils.authUser();
			if (!ScooldUtils.getInstance().isAdmin(authUser)) {
				entity.put(Config._CREATORID, authUser.getId());
			}
			req.setAttribute(REST_ENTITY_ATTRIBUTE, entity);
		}
		return req;
	}

	private HttpServletResponse response() {
		return utils.getCurrentResponse();
	}

	private String safe(String value) {
		if (StringUtils.isBlank(value)) {
			throw new MCPException(INVALID_INPUT, "Required parameter is missing.");
		}
		return value;
	}
}
