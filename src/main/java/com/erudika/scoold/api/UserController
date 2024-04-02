package com.erudika.scoold.api;

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.User;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.para.core.validation.ValidationUtils;
import com.erudika.scoold.controllers.PeopleController;
import com.erudika.scoold.controllers.ProfileController;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.BadRequestException;
import com.erudika.scoold.utils.ScooldUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.erudika.scoold.ScooldServer.REST_ENTITY_ATTRIBUTE;

/**
 * Controller contains API to get user related information.
 */
@RestController
@RequestMapping(value = "/api/users", produces = "application/json")
public class UserController {

	private final ScooldUtils utils;

	public static final Logger logger = LoggerFactory.getLogger(UserController.class);
	private final ParaClient pc;
	@Inject
	private PeopleController peopleController;
	@Inject
	private ProfileController profileController;

	@Inject
	public UserController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@PostMapping
	public Map<String, Object> createUser(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing or invalid request body.");
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
		newUser.setPassword((String) entity.getOrDefault(Config._PASSWORD, Utils.generateSecurityToken(10)));
		newUser.setIdentifier(StringUtils.isBlank(newUser.getIdentifier()) ? newUser.getEmail() : newUser.getIdentifier());
		String[] errors = ValidationUtils.validateObject(newUser);

		if (errors.length == 0) {
			// generic and password providers are identical but this was fixed in Para 1.37.1 (backwards compatibility)
			String provider = "generic".equals(newUser.getIdentityProvider()) ? "password" : newUser.getIdentityProvider();
			User createdUser = pc.signIn(provider, newUser.getIdentifier() + Para.getConfig().separator() +
				newUser.getName() + Para.getConfig().separator() + newUser.getPassword(), false);
			// user is probably active:false so activate them
			List<User> created = pc.findTerms(newUser.getType(), Collections.singletonMap(Config._EMAIL, newUser.getEmail()), true);
			if (createdUser == null && !created.isEmpty()) {
				createdUser = created.iterator().next();
				if (Utils.timestamp() - createdUser.getTimestamp() > TimeUnit.SECONDS.toMillis(20)) {
					createdUser = null; // user existed previously
				} else if (newUser.getActive() && !createdUser.getActive()) {
					createdUser.setActive(true);
					createdUser.setPicture(newUser.getPicture());
					pc.update(createdUser);
				}
			}
			if (createdUser == null) {
				badReq("Failed to create user. User may already exist.");
			} else {
				Profile profile = Profile.fromUser(createdUser);
				profile.setPicture(newUser.getPicture());
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

	@GetMapping
	public List<Map<String, Object>> listUsers(@RequestParam(required = false, defaultValue = Config._TIMESTAMP) String sortby,
											   @RequestParam(required = false, defaultValue = "*") String q, HttpServletRequest req) {
		Model model = new ExtendedModelMap();
		peopleController.get(req.getParameter("tag"), sortby, q, req, model);
		List<Profile> profiles = (List<Profile>) model.getAttribute("userlist");
		List<Map<String, Object>> results = new LinkedList<>();
		Map<String, User> usersMap = new HashMap<>();
		if (profiles != null && !profiles.isEmpty()) {
			List<User> users = pc.readAll(profiles.stream().map(p -> p.getCreatorid()).collect(Collectors.toList()));
			for (User user : users) {
				usersMap.put(user.getId(), user);
			}
			for (Profile profile : profiles) {
				Map<String, Object> u = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(profile, false));
				u.put("user", usersMap.get(profile.getCreatorid()));
				results.add(u);
			}
		}
		return results;
	}

	@GetMapping("/{id}")
	public Map<String, Object> getUser(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		List<?> usrProfile = pc.readAll(Arrays.asList(StringUtils.substringBefore(id, Para.getConfig().separator()), Profile.id(id)));
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

	@PatchMapping("/{id}")
	@SuppressWarnings("unchecked")
	public Profile updateUser(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing or invalid request body.");
		}
		String name = (String) entity.get(Config._NAME);
		String email = (String) entity.get(Config._EMAIL);
		String password = (String) entity.get(Config._PASSWORD);
		String location = (String) entity.get("location");
		String latlng = (String) entity.get("latlng");
		String website = (String) entity.get("website");
		String aboutme = (String) entity.get("aboutme");
		String picture = (String) entity.get("picture");

		Model model = new ExtendedModelMap();
		profileController.edit(id, name, location, latlng, website, aboutme, picture, email, req, model);

		Profile profile = (Profile) model.getAttribute("user");
		if (profile == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		boolean update = false;
		if (entity.containsKey("spaces")) {
			profile.setSpaces(new HashSet<>(readSpaces(((List<String>) entity.getOrDefault("spaces",
				Collections.emptyList())).toArray(new String[0]))));
			update = true;
		}
		if (entity.containsKey("replyEmailsEnabled")) {
			profile.setReplyEmailsEnabled((Boolean) entity.get("replyEmailsEnabled"));
			update = true;
		}
		if (entity.containsKey("commentEmailsEnabled")) {
			profile.setCommentEmailsEnabled((Boolean) entity.get("commentEmailsEnabled"));
			update = true;
		}
		if (entity.containsKey("favtagsEmailsEnabled")) {
			profile.setFavtagsEmailsEnabled((Boolean) entity.get("favtagsEmailsEnabled"));
			update = true;
		}
		if (entity.containsKey("favtags") && entity.get("favtags") instanceof List) {
			profile.setFavtags((List<String>) entity.get("favtags"));
			update = true;
		}
		if (update) {
			pc.update(profile);
		}
		if (!StringUtils.isBlank(password)) {
			User u = profile.getUser();
			if (u == null || !StringUtils.equalsAny(u.getIdentityProvider(), "password", "generic")) {
				badReq("User's password cannot be modified.");
			}
			if (!utils.isPasswordStrongEnough(password)) {
				badReq("Password is not strong enough.");
			}
			Sysprop identifier = pc.read(u.getEmail());
			identifier.addProperty(Config._RESET_TOKEN, ""); // avoid removeProperty method because it won't be seen by server
			identifier.addProperty("iforgotTimestamp", 0);
			identifier.addProperty(Config._PASSWORD, Utils.bcrypt(password));
			pc.update(identifier);
		}
		return profile;
	}

	@DeleteMapping("/{id}")
	public void deleteUser(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Profile profile = pc.read(Profile.id(id));
		if (profile == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return;
		}
		profile.delete();
	}

	@GetMapping("/{id}/questions")
	public List<? extends Post> getUserQuestions(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Profile p = pc.read(Profile.id(id));
		if (p == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return profileController.getQuestions(utils.getAuthUser(req), p, true, utils.pagerFromParams(req));
	}

	@GetMapping("/{id}/replies")
	public List<? extends Post> getUserReplies(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Profile p = pc.read(Profile.id(id));
		if (p == null) {
			res.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return profileController.getAnswers(utils.getAuthUser(req), p, true, utils.pagerFromParams(req));
	}

	@GetMapping("/{id}/favorites")
	public List<? extends Post> getUserFavorites(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		badReq("Not supported");
		return null;
	}

	@PutMapping("/{id}/moderator")
	public void makeUserMod(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		profileController.makeMod(id, req, res);
	}

	@PutMapping("/{id}/ban")
	public void banUser(@PathVariable String id, @RequestParam(defaultValue = "0") String banuntil,
						HttpServletRequest req, HttpServletResponse res) {
		badReq("Not supported");
	}

	@PutMapping("/spaces")
	public void bulkEditSpaces(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> entity = readEntity(req);
		if (entity.isEmpty()) {
			badReq("Missing or invalid request body.");
		}
		Set<String> selectedUsers = ((List<String>) entity.getOrDefault("users",
			Collections.emptyList())).stream().map(id -> Profile.id(id)).collect(Collectors.toSet());
		Set<String> selectedSpaces = ((List<String>) entity.getOrDefault("spaces",
			Collections.emptyList())).stream().distinct().collect(Collectors.toSet());
		Set<String> selectedBadges = ((List<String>) entity.getOrDefault("badges",
			Collections.emptyList())).stream().distinct().collect(Collectors.toSet());
		peopleController.bulkEdit(selectedUsers.toArray(new String[0]),
			readSpaces(selectedSpaces).toArray(new String[0]),
			selectedBadges.toArray(new String[0]), req);
	}

	private void badReq(String error) {
		if (!StringUtils.isBlank(error)) {
			throw new BadRequestException(error);
		}
	}

	private List<String> readSpaces(String... spaces) {
		return readSpaces(Arrays.asList(spaces));
	}

	private List<String> readSpaces(Collection<String> spaces) {
		if (spaces == null || spaces.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> ids = spaces.stream().map(s -> utils.getSpaceId(s)).
			filter(s -> !s.isEmpty() && !utils.isDefaultSpace(s)).distinct().collect(Collectors.toList());
		List<Sysprop> existing = pc.readAll(ids);
		if (spaces.contains(Post.DEFAULT_SPACE) || spaces.contains("default")) {
			existing.add(utils.buildSpaceObject(Post.DEFAULT_SPACE));
		}
		return existing.stream().map(s -> s.getId() + Para.getConfig().separator() + s.getName()).collect(Collectors.toList());
	}

	private Map<String, Object> readEntity(HttpServletRequest req) {
		try {
			Map<String, Object> entity = ParaObjectUtils.getJsonReader(Map.class).readValue(req.getInputStream());
			req.setAttribute(REST_ENTITY_ATTRIBUTE, entity);
			return entity;
		} catch (IOException ex) {
			badReq("Missing or invalid request body.");
		} catch (Exception ex) {
			logger.error(null, ex);
		}
		return Collections.emptyMap();
	}
}
