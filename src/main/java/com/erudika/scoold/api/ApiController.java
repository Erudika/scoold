/*
 * Copyright 2013-2020 Erudika. https://erudika.com
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
package com.erudika.scoold.api;

import com.erudika.para.annotations.Locked;
import com.erudika.para.client.ParaClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.Tag;
import com.erudika.para.core.User;
import com.erudika.para.core.Webhook;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.erudika.para.validation.ValidationUtils;
import com.erudika.scoold.ScooldServer;
import static com.erudika.scoold.ScooldServer.AUTH_USER_ATTRIBUTE;
import static com.erudika.scoold.ScooldServer.CONTEXT_PATH;
import static com.erudika.scoold.ScooldServer.REST_ENTITY_ATTRIBUTE;
import com.erudika.scoold.controllers.CommentController;
import com.erudika.scoold.controllers.PeopleController;
import com.erudika.scoold.controllers.ProfileController;
import com.erudika.scoold.controllers.QuestionController;
import com.erudika.scoold.controllers.QuestionsController;
import com.erudika.scoold.controllers.ReportsController;
import com.erudika.scoold.controllers.RevisionsController;
import com.erudika.scoold.controllers.TagsController;
import com.erudika.scoold.controllers.VoteController;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.core.Revision;
import com.erudika.scoold.core.UnapprovedQuestion;
import com.erudika.scoold.core.UnapprovedReply;
import com.erudika.scoold.utils.ScooldUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

/**
 * Scoold REST API
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@RestController
@RequestMapping("/api")
@SuppressWarnings("unchecked")
public class ApiController {

	public static final Logger logger = LoggerFactory.getLogger(ApiController.class);
	private static final String[] POST_TYPES = new String[] {Utils.type(Question.class), Utils.type(Reply.class)};

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	private QuestionsController questionsController;
	@Inject
	private QuestionController questionController;
	@Inject
	private VoteController voteController;
	@Inject
	private CommentController commentController;
	@Inject
	private PeopleController peopleController;
	@Inject
	private ProfileController profileController;
	@Inject
	private RevisionsController revisionsController;
	@Inject
	private TagsController tagsController;
	@Inject
	private ReportsController reportsController;

	@Inject
	public ApiController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping
	public Map<String, Object> get(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> intro = new HashMap<>();
		intro.put("message", Config.APP_NAME + " API, see docs at " + ScooldServer.getServerURL()
				+ CONTEXT_PATH + "/api.html");
		boolean healthy;
		try {
			healthy = pc != null && pc.getTimestamp() > 0;
		} catch (Exception e) {
			healthy = false;
		}
		intro.put("healthy", healthy);
		intro.put("pro", false);
		if (!healthy) {
			res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		return intro;
	}

	@PostMapping("/posts")
	public Map<String, Object> createPost(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (!entity.containsKey(Config._TYPE)) {
			entity.put(Config._TYPE, POST_TYPES[0]);
		} else if (!StringUtils.equalsAnyIgnoreCase((CharSequence) entity.get(Config._TYPE), POST_TYPES)) {
			badReq("Invalid post type - could be one of " + Arrays.toString(POST_TYPES));
		}
		Post post = ParaObjectUtils.setAnnotatedFields(entity);

		if (!StringUtils.isBlank(post.getCreatorid())) {
			Profile authUser = pc.read(Profile.id(post.getCreatorid()));
			if (authUser != null) {
				req.setAttribute(AUTH_USER_ATTRIBUTE, authUser);
			}
		}
		Model model = new ExtendedModelMap();
		List<String> spaces = readSpaces(post.getSpace());
		post.setSpace(spaces.iterator().hasNext() ? spaces.iterator().next() : null);

		if (post.isQuestion()) {
			questionsController.post(post.getLocation(), post.getLatlng(), post.getAddress(), post.getSpace(), req, model);
		} else if (post.isReply()) {
			questionController.reply(post.getParentid(), "", null, req, res, model);
		} else {
			badReq("Invalid post type - could be one of " + Arrays.toString(POST_TYPES));
		}

		checkForErrorsAndThrow(model);
		Map<String, Object> newpost = (Map<String, Object>) model.getAttribute("newpost");
		res.setStatus(HttpStatus.CREATED.value());
		return newpost;
	}

	@GetMapping("/posts")
	public List<Map<String, Object>> listQuestions(HttpServletRequest req) {
		Model model = new ExtendedModelMap();
		questionsController.getQuestions(req.getParameter("sortby"), req.getParameter("filter"), req, model);
		return ((List<Question>) model.getAttribute("questionslist")).stream().map(p -> {
			Map<String, Object> post = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(p, false));
			post.put("author", p.getAuthor());
			return post;
		}).collect(Collectors.toList());
	}

	@GetMapping("/posts/{id}")
	public Map<String, Object> getPost(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Model model = new ExtendedModelMap();
		questionController.get(id, "", req.getParameter("sortby"), req, res, model);
		Post showPost = (Post) model.getAttribute("showPost");
		List<Post> answers = (List<Post>) model.getAttribute("answerslist");
		List<Post> similar = (List<Post>) model.getAttribute("similarquestions");
		if (showPost == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		Map<String, Object> result = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(showPost, false));
		List<Map<String, Object>> answerz = answers.stream().map(p -> {
			Map<String, Object> post = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(p, false));
			post.put("author", p.getAuthor());
			return post;
		}).collect(Collectors.toList());
		result.put("comments", showPost.getComments());
		result.put("author", showPost.getAuthor());
		showPost.setItemcount(null);
		if (!showPost.isReply()) {
			result.put("children", answerz);
			result.put("similar", similar);
		}
		return result;
	}

	@PatchMapping("/posts/{id}")
	public Post updatePost(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing request body.");
		}
		String editorid = (String) entity.get("lasteditby");
		if (!StringUtils.isBlank(editorid)) {
			Profile authUser = pc.read(Profile.id(editorid));
			if (authUser != null) {
				req.setAttribute(AUTH_USER_ATTRIBUTE, authUser);
			}
		}
		String space = (String) entity.get("space");
		String title = (String) entity.get("title");
		String body = (String) entity.get("body");
		String location = (String) entity.get("location");
		String latlng = (String) entity.get("latlng");
		List<String> spaces = readSpaces(space);
		space = spaces.iterator().hasNext() ? spaces.iterator().next() : null;
		Model model = new ExtendedModelMap();
		questionController.edit(id, title, body, String.join(",", (List<String>) entity.get("tags")),
				location, latlng, space, req, model);

		Post post = (Post) model.getAttribute("post");
		if (post == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
		} else if (!utils.canEdit(post, utils.getAuthUser(req))) {
			badReq("Update failed - user " + editorid + " is not allowed to update post.");
		}
		return post;
	}

	@DeleteMapping("/posts/{id}")
	public void deletePost(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Model model = new ExtendedModelMap();
		questionController.delete(id, req, model);
		res.setStatus(model.containsAttribute("deleted") ? 200 : HttpStatus.NOT_FOUND.value());
	}

	@PutMapping("/posts/{id}/approve")
	public void approvePost(@PathVariable String id, HttpServletRequest req) {
		questionController.modApprove(id, req);
	}

	@PutMapping("/posts/{id}/accept/{replyid}")
	public void acceptReply(@PathVariable String id, @PathVariable String replyid, HttpServletRequest req) {
		questionController.approve(id, replyid, req);
	}

	@PutMapping("/posts/{id}/close")
	public void closePost(@PathVariable String id, HttpServletRequest req) {
		questionController.close(id, req);
	}

	@PutMapping("/posts/{id}/pin")
	public void pinPost(@PathVariable String id, HttpServletRequest req) {
		badReq("Not supported");
	}

	@PutMapping("/posts/{id}/restore/{revisionid}")
	public void restoreRevision(@PathVariable String id, @PathVariable String revisionid, HttpServletRequest req) {
		questionController.restore(id, revisionid, req);
	}

	@PutMapping("/posts/{id}/like")
	public void favPost(@PathVariable String id, HttpServletRequest req) {
		badReq("Not supported");
	}

	@PutMapping("/posts/{id}/voteup")
	public void upvotePost(@PathVariable String id, @RequestParam(required = false) String userid,
			HttpServletRequest req, HttpServletResponse res) {
		if (!voteRequest(true, id, userid, req)) {
			badReq("Vote request failed.");
		}
	}

	@PutMapping("/posts/{id}/votedown")
	public void downvotePost(@PathVariable String id, @RequestParam(required = false) String userid,
			HttpServletRequest req) {
		if (!voteRequest(false, id, userid, req)) {
			badReq("Vote request failed.");
		}
	}

	@GetMapping("/posts/{id}/comments")
	public List<Comment> getPostComments(@PathVariable String id,
			@RequestParam(required = false, defaultValue = "5") String limit,
			@RequestParam(required = false, defaultValue = "1") String page,
			@RequestParam(required = false, defaultValue = Config._TIMESTAMP) String sortby,
			@RequestParam(required = false, defaultValue = "false") String desc,
			HttpServletRequest req, HttpServletResponse res) {
		Post post = pc.read(id);
		if (post == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		post.getItemcount().setLimit(NumberUtils.toInt(limit));
		post.getItemcount().setPage(NumberUtils.toInt(page));
		post.getItemcount().setSortby(sortby);
		post.getItemcount().setDesc(Boolean.parseBoolean(desc));
		utils.reloadFirstPageOfComments(post);
		return post.getComments();
	}

	@GetMapping("/posts/{id}/revisions")
	public List<Map<String, Object>> getPostRevisions(@PathVariable String id,
			HttpServletRequest req, HttpServletResponse res) {
		Model model = new ExtendedModelMap();
		revisionsController.get(id, req, model);
		Post post = (Post) model.getAttribute("showPost");
		if (post == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return ((List<Revision>) model.getAttribute("revisionslist")).stream().map(r -> {
			Map<String, Object> rev = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(r, false));
			rev.put("author", r.getAuthor());
			return rev;
		}).collect(Collectors.toList());
	}


	@PostMapping("/users")
	public Map<String, Object> createUser(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing request body.");
		}
		Map<String, Object> userEntity = new HashMap<>();
		userEntity.put(Config._TYPE, Utils.type(User.class));
		userEntity.put(Config._NAME, entity.get(Config._NAME));
		userEntity.put(Config._EMAIL, entity.get(Config._EMAIL));
		userEntity.put(Config._IDENTIFIER, entity.get(Config._IDENTIFIER));
		userEntity.put(Config._GROUPS, entity.get(Config._GROUPS));
		userEntity.put("active", entity.getOrDefault("active", true));
		userEntity.put("picture", entity.get("picture"));

		User newUser = ParaObjectUtils.setAnnotatedFields(new User(), userEntity, null);
		newUser.setPassword((String) entity.get(Config._PASSWORD));
		newUser.setIdentifier(StringUtils.isBlank(newUser.getIdentifier()) ? newUser.getEmail() : newUser.getIdentifier());
		String[] errors = ValidationUtils.validateObject(newUser);

		if (errors.length == 0) {
			User createdUser = pc.create(newUser);
			if (createdUser == null) {
				badReq("Failed to create user.");
			} else {
				Profile profile = Profile.fromUser(createdUser);
				profile.getSpaces().addAll(readSpaces(((List<String>) entity.getOrDefault("spaces",
						Collections.emptyList())).toArray(new String[0])));
				res.setStatus(HttpStatus.CREATED.value());
				pc.create(profile);

				Map<String, Object> payload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(profile, false));
				payload.put("user", createdUser);
				utils.triggerHookEvent("user.signup", payload);
				logger.info("Created new user through API '{}' with id={}, groups={}, spaces={}.",
					createdUser.getName(), profile.getId(), profile.getGroups(), profile.getSpaces());
				Map<String, Object> result = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(profile, false));
				result.put("user", createdUser);
				return result;
			}
		}
		badReq("Failed to create user - " + String.join("; ", errors));
		return null;
	}

	@GetMapping("/users")
	public List<Profile> listUsers(@RequestParam(required = false, defaultValue = Config._TIMESTAMP) String sortby,
			@RequestParam(required = false, defaultValue = "*") String q, HttpServletRequest req) {
		Model model = new ExtendedModelMap();
		peopleController.get(sortby, q, req, model);
		return (List<Profile>) model.getAttribute("userlist");
	}

	@GetMapping("/users/{id}")
	public Map<String, Object> getUser(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		List<?> usrProfile = pc.readAll(Arrays.asList(StringUtils.substringBefore(id, Config.SEPARATOR), Profile.id(id)));
		Iterator<?> it = usrProfile.iterator();
		User u = it.hasNext() ? (User) it.next() : null;
		Profile p = it.hasNext() ? (Profile) it.next() : null;
		if (p == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		Map<String, Object> result = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(p, false));
		result.put("user", u);
		return result;
	}

	@PatchMapping("/users/{id}")
	public Profile updateUser(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing request body.");
		}
		String name = (String) entity.get("name");
		String location = (String) entity.get("location");
		String latlng = (String) entity.get("latlng");
		String website = (String) entity.get("website");
		String aboutme = (String) entity.get("aboutme");
		String picture = (String) entity.get("picture");

		Model model = new ExtendedModelMap();
		profileController.edit(id, name, location, latlng, website, aboutme, picture, req, model);

		Profile profile = (Profile) model.getAttribute("user");
		if (profile == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		if (entity.containsKey("spaces")) {
			profile.setSpaces(new HashSet<>(readSpaces(((List<String>) entity.getOrDefault("spaces",
						Collections.emptyList())).toArray(new String[0]))));
			pc.update(profile);
		}
		return profile;
	}

	@DeleteMapping("/users/{id}")
	public void deleteUser(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Profile profile = pc.read(Profile.id(id));
		if (profile == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return;
		}
		profile.delete();
	}

	@GetMapping("/users/{id}/questions")
	public List<? extends Post> getUserQuestions(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Profile p = pc.read(Profile.id(id));
		if (p == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return profileController.getQuestions(utils.getAuthUser(req), p, true, utils.pagerFromParams(req));
	}

	@GetMapping("/users/{id}/replies")
	public List<? extends Post> getUserReplies(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Profile p = pc.read(Profile.id(id));
		if (p == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return profileController.getAnswers(utils.getAuthUser(req), p, true, utils.pagerFromParams(req));
	}

	@GetMapping("/users/{id}/favorites")
	public List<? extends Post> getUserFavorites(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		badReq("Not supported");
		return null;
	}

	@PutMapping("/users/{id}/moderator")
	public void makeUserMod(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		profileController.makeMod(id, req, res);
	}

	@PutMapping("/users/{id}/ban")
	public void banUser(@PathVariable String id, @RequestParam(defaultValue = "0") String banuntil,
			HttpServletRequest req, HttpServletResponse res) {
		badReq("Not supported");
	}

	@PutMapping("/users/spaces")
	public void bulkEditSpaces(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing request body.");
		}
		Set<String> selectedUsers = ((List<String>) entity.getOrDefault("users",
				Collections.emptyList())).stream().map(id -> Profile.id(id)).collect(Collectors.toSet());
		Set<String> selectedSpaces = ((List<String>) entity.getOrDefault("spaces",
				Collections.emptyList())).stream().distinct().collect(Collectors.toSet());
		peopleController.bulkEdit(selectedUsers.toArray(new String[0]),
				readSpaces(selectedSpaces).toArray(new String[0]), req);
	}

	@PostMapping("/tags")
	public Tag createTag(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing request body.");
		}
		if (pc.read(new Tag((String) entity.get("tag")).getId()) != null) {
			badReq("Tag already exists.");
		}
		Tag newTag = ParaObjectUtils.setAnnotatedFields(new Tag((String) entity.get("tag")), entity, null);
		String[] errors = ValidationUtils.validateObject(newTag);
		if (errors.length == 0) {
			res.setStatus(HttpStatus.CREATED.value());
			return pc.create(newTag);
		}
		badReq("Failed to create tag - " + String.join("; ", errors));
		return null;
	}

	@GetMapping("/tags")
	public List<Tag> listTags(@RequestParam(required = false, defaultValue = "count") String sortby,
			HttpServletRequest req, HttpServletResponse res) {
		Model model = new ExtendedModelMap();
		tagsController.get(sortby, req, model);
		return (List<Tag>) model.getAttribute("tagslist");
	}

	@GetMapping("/tags/{id}")
	public Tag getTag(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Tag tag = pc.read(new Tag(id).getId());
		if (tag == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return tag;
	}

	@PatchMapping("/tags/{id}")
	public Tag updateTag(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing request body.");
		}
		Model model = new ExtendedModelMap();
		tagsController.rename(id, (String) entity.get("tag"), req, model);
		if (!model.containsAttribute("tag")) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return (Tag) model.getAttribute("tag");
	}

	@DeleteMapping("/tags/{id}")
	public void deleteTag(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		pc.delete(new Tag(id));
	}

	@GetMapping("/tags/{id}/questions")
	public List<Map<String, Object>> listTaggedQuestions(@PathVariable String id,
			HttpServletRequest req, HttpServletResponse res) {
		Model model = new ExtendedModelMap();
		questionsController.getTagged(new Tag(id).getTag(), req, model);
		return ((List<Post>) model.getAttribute("questionslist")).stream().map(p -> {
			Map<String, Object> post = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(p, false));
			post.put("author", p.getAuthor());
			return post;
		}).collect(Collectors.toList());
	}

	@PostMapping("/comments")
	public Comment createComment(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing request body.");
		}
		String comment = (String) entity.get("comment");
		String parentid = (String) entity.get(Config._PARENTID);
		String creatorid = (String) entity.get(Config._CREATORID);
		ParaObject parent = pc.read(parentid);
		if (parent == null) {
			badReq("Parent object not found. Provide a valid parentid.");
			return null;
		}
		if (!StringUtils.isBlank(creatorid)) {
			Profile authUser = pc.read(Profile.id(creatorid));
			if (authUser != null) {
				req.setAttribute(AUTH_USER_ATTRIBUTE, authUser);
			}
		}
		Model model = new ExtendedModelMap();
		commentController.createAjax(comment, parentid, req, model);
		Comment created = (Comment) model.getAttribute("showComment");
		if (created == null || StringUtils.isBlank(comment)) {
			badReq("Failed to create comment.");
			return null;
		}
		res.setStatus(HttpStatus.CREATED.value());
		return created;
	}

	@GetMapping("/comments/{id}")
	public Comment getComment(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Comment comment = pc.read(id);
		if (comment == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return comment;
	}

	@DeleteMapping("/comments/{id}")
	public void deleteComment(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		commentController.deleteAjax(id, req, res);
	}

	@PostMapping("/reports")
	public Report createReport(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing request body.");
		}
		String creatorid = (String) entity.get(Config._CREATORID);
		if (!StringUtils.isBlank(creatorid)) {
			Profile authUser = pc.read(Profile.id(creatorid));
			if (authUser != null) {
				req.setAttribute(AUTH_USER_ATTRIBUTE, authUser);
			}
		}
		Model model = new ExtendedModelMap();
		reportsController.create(req, res, model);
		checkForErrorsAndThrow(model);
		Report newreport = (Report) model.getAttribute("newreport");
		res.setStatus(HttpStatus.CREATED.value());
		return newreport;
	}

	@GetMapping("/reports")
	public List<Report> listReports(@RequestParam(required = false, defaultValue = "count") String sortby,
			HttpServletRequest req, HttpServletResponse res) {
		Model model = new ExtendedModelMap();
		reportsController.get(sortby, req, model);
		return (List<Report>) model.getAttribute("reportslist");
	}

	@GetMapping("/reports/{id}")
	public Report getReport(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Report report = pc.read(id);
		if (report == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return report;
	}

	@DeleteMapping("/reports/{id}")
	public void deleteReport(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		reportsController.delete(id, req, res);
	}

	@PutMapping("/reports/{id}/close")
	public void closeReport(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		String solution = (String) entity.getOrDefault("solution", "Closed via API.");
		reportsController.close(id, solution, req, res);
	}

	@PostMapping("/spaces")
	public Sysprop createSpace(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing request body.");
		}
		String name = (String) entity.get(Config._NAME);
		if (StringUtils.isBlank(name)) {
			badReq("Property 'name' cannot be blank.");
			return null;
		}
		if (pc.read(utils.getSpaceId(name)) != null) {
			badReq("Space already exists.");
			return null;
		}
		Sysprop s = utils.buildSpaceObject(name);
		res.setStatus(HttpStatus.CREATED.value());
		return pc.create(s);
	}

	@GetMapping("/spaces")
	public List<Sysprop> listSpaces(HttpServletRequest req, HttpServletResponse res) {
		return pc.findQuery("scooldspace", "*", utils.pagerFromParams(req));
	}

	@GetMapping("/spaces/{id}")
	public Sysprop getSpace(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Sysprop space = pc.read(utils.getSpaceId(id));
		if (space == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return space;
	}

	@PostMapping("/webhooks")
	public Webhook createWebhook(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing request body.");
		}
		String targetUrl = (String) entity.get(Config._NAME);
		if (!Utils.isValidURL(targetUrl)) {
			badReq("Property 'targetUrl' must be a valid URL.");
			return null;
		}
		Webhook webhook = pc.create(ParaObjectUtils.setAnnotatedFields(entity));
		res.setStatus(HttpStatus.CREATED.value());
		return webhook;
	}

	@GetMapping("/webhooks")
	public List<Webhook> listWebhooks(HttpServletRequest req, HttpServletResponse res) {
		return pc.findQuery(Utils.type(Webhook.class), "*", utils.pagerFromParams(req));
	}

	@GetMapping("/webhooks/{id}")
	public Webhook getWebhook(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Webhook webhook = pc.read(Utils.type(Webhook.class), id);
		if (webhook == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return webhook;
	}

	@PatchMapping("/webhooks/{id}")
	public Webhook updateWebhook(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Webhook webhook = pc.read(id);
		if (webhook == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		Map<String, Object> entity = readEntity(req);
		return pc.update(ParaObjectUtils.setAnnotatedFields(webhook, entity, Locked.class));
	}

	@DeleteMapping("/webhooks/{id}")
	public void deleteWebhook(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Webhook webhook = pc.read(id);
		if (webhook == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return;
		}
		pc.delete(webhook);
	}

	@GetMapping("/events")
	public Set<String> listHookEvents(HttpServletRequest req, HttpServletResponse res) {
		return utils.getCustomHookEvents();
	}

	@GetMapping("/types")
	public Set<String> listCoreTypes(HttpServletRequest req, HttpServletResponse res) {
		return utils.getCoreScooldTypes();
	}

	@GetMapping("/search/{type}/{query}")
	public Map<String, Object> search(@PathVariable String type, @PathVariable String query, HttpServletRequest req) {
		if ("user".equals(type)) {
			type = Utils.type(Profile.class);
		}
		if ("answer".equals(type)) {
			type = Utils.type(Reply.class);
		}
		Pager pager = utils.pagerFromParams(req);
		Map<String, Object> result = new HashMap<>();
		result.put("items", pc.findQuery(type, query, pager));
		result.put("page", pager.getPage());
		result.put("totalHits", pager.getCount());
		if (!StringUtils.isBlank(pager.getLastKey())) {
			result.put("lastKey", pager.getLastKey());
		}
		return result;
	}

	@GetMapping("/stats")
	public Map<String, Object> stats(HttpServletRequest req) {
		Map<String, Object> stats = new LinkedHashMap<>();
		stats.put("questions", pc.getCount(Utils.type(Question.class)));
		stats.put("replies", pc.getCount(Utils.type(Reply.class)));
		stats.put("spaces", pc.getCount("scooldspace"));
		stats.put("users", pc.getCount(Utils.type(Profile.class)));
		stats.put("tags", pc.getCount(Utils.type(Tag.class)));
		stats.put("reports", pc.getCount(Utils.type(Report.class)));
		stats.put("comments", pc.getCount(Utils.type(Comment.class)));
		stats.put("revisions", pc.getCount(Utils.type(Revision.class)));
		stats.put("unapproved_questions", pc.getCount(Utils.type(UnapprovedQuestion.class)));
		stats.put("unapproved_replies", pc.getCount(Utils.type(UnapprovedReply.class)));
		stats.put("para_version", pc.getServerVersion());
		stats.put("scoold_version",  Optional.ofNullable(getClass().getPackage().getImplementationVersion()).
				orElse("unknown"));
		return stats;
	}

	@DeleteMapping("/spaces/{id}")
	public void deleteSpace(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		pc.delete(new Sysprop(utils.getSpaceId(id)));
	}

	private boolean voteRequest(boolean isUpvote, String id, String userid, HttpServletRequest req) {
		if (!StringUtils.isBlank(userid)) {
			Profile authUser = pc.read(Profile.id(userid));
			if (authUser != null) {
				req.setAttribute(AUTH_USER_ATTRIBUTE, authUser);
			}
		}
		ParaObject votable = pc.read(id);
		return votable != null && (isUpvote ? voteController.voteup(id, votable.getType(), req) :
				voteController.votedown(id, votable.getType(), req));
	}

	private List<String> readSpaces(Collection<String> spaces) {
		if (spaces == null || spaces.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> ids = spaces.stream().map(s -> utils.getSpaceId(s)).
				filter(s -> !s.isEmpty() && !utils.isDefaultSpace(s)).distinct().collect(Collectors.toList());
		List<Sysprop> existing = pc.readAll(ids);
		return existing.stream().map(s -> s.getId() + Config.SEPARATOR + s.getName()).collect(Collectors.toList());
	}

	private List<String> readSpaces(String... spaces) {
		return readSpaces(Arrays.asList(spaces));
	}

	@ExceptionHandler({Exception.class})
	public Map<String, Object> handleException(Exception ex, WebRequest request, HttpServletResponse res) {
		Map<String, Object> error = new HashMap<>(2);
		int code = 500;
		if (ex instanceof BadRequestException) {
			code = 400;
		}
		res.setStatus(code);
		error.put("code", code);
		error.put("message", ex.getMessage());
		return error;
	}

	private Map<String, Object> readEntity(HttpServletRequest req) {
		try {
			Map<String, Object> entity = ParaObjectUtils.getJsonReader(Map.class).readValue(req.getInputStream());
			req.setAttribute(REST_ENTITY_ATTRIBUTE, entity);
			return entity;
		} catch (IOException ex) {
			badReq("Missing request body.");
		} catch (Exception ex) {
			logger.error(null, ex);
		}
		return Collections.emptyMap();
	}

	private void checkForErrorsAndThrow(Model model) {
		if (model != null && model.containsAttribute("error")) {
			Object err = model.getAttribute("error");
			if (err instanceof String) {
				badReq((String) err);
			} else if (err instanceof Map) {
				Map<String, String> error = (Map<String, String>) err;
				badReq(error.entrySet().stream().map(e -> "'" + e.getKey() + "' " +
						e.getValue()).collect(Collectors.joining("; ")));
			}
		}
	}

	private void badReq(String error) {
		if (!StringUtils.isBlank(error)) {
			throw new BadRequestException(error);
		}
	}

}
