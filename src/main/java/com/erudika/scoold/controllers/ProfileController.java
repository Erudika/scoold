/*
 * Copyright 2013-2022 Erudika. https://erudika.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For issues and patches go to: https://github.com/erudika
 */
package com.erudika.scoold.controllers;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.User;
import static com.erudika.para.core.User.Groups.MODS;
import static com.erudika.para.core.User.Groups.USERS;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.PEOPLELINK;
import static com.erudika.scoold.ScooldServer.PROFILELINK;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Badge;
import com.erudika.scoold.core.Post;
import static com.erudika.scoold.core.Post.DEFAULT_SPACE;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.Sticky;
import com.erudika.scoold.core.UnapprovedQuestion;
import com.erudika.scoold.core.UnapprovedReply;
import com.erudika.scoold.utils.HttpUtils;
import com.erudika.scoold.utils.ScooldUtils;
import com.erudika.scoold.utils.avatars.*;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

	private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);
	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private final ScooldUtils utils;
	private final ParaClient pc;
	private final GravatarAvatarGenerator gravatarAvatarGenerator;
	private final AvatarRepository avatarRepository;

	@Inject
	public ProfileController(ScooldUtils utils, GravatarAvatarGenerator gravatarAvatarGenerator, AvatarRepositoryProxy avatarRepository) {
		this.utils = utils;
		this.pc = utils.getParaClient();
		this.gravatarAvatarGenerator = gravatarAvatarGenerator;
		this.avatarRepository = avatarRepository;
	}

	@GetMapping({"", "/{id}/**"})
	public String get(@PathVariable(required = false) String id, HttpServletRequest req, Model model) {
		if (!utils.isAuthenticated(req) && StringUtils.isBlank(id)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + PROFILELINK;
		}
		Profile authUser = utils.getAuthUser(req);
		Profile showUser;
		boolean isMyProfile;

		if (StringUtils.isBlank(id) || isMyid(authUser, Profile.id(id))) {
			//requested userid !exists or = my userid => show my profile
			showUser = authUser;
			isMyProfile = true;
		} else {
			showUser = pc.read(Profile.id(id));
			isMyProfile = isMyid(authUser, Profile.id(id));
		}

		if (showUser == null || !ParaObjectUtils.typesMatch(showUser)) {
			return "redirect:" + PROFILELINK;
		}

		boolean protekted = !utils.isDefaultSpacePublic() && !utils.isAuthenticated(req);
		boolean sameSpace = (utils.canAccessSpace(showUser, "default") && utils.canAccessSpace(authUser, "default")) ||
				(authUser != null && showUser.getSpaces().stream().anyMatch(s -> utils.canAccessSpace(authUser, s)));
		if (protekted || !sameSpace || !CONF.usersDiscoverabilityEnabled(utils.isAdmin(authUser))) {
			return "redirect:" + PEOPLELINK;
		}

		Pager itemcount1 = utils.getPager("page1", req);
		Pager itemcount2 = utils.getPager("page2", req);
		List<? extends Post> questionslist = getQuestions(authUser, showUser, isMyProfile, itemcount1);
		List<? extends Post> answerslist = getAnswers(authUser, showUser, isMyProfile, itemcount2);

		model.addAttribute("path", "profile.vm");
		model.addAttribute("title", showUser.getName());
		model.addAttribute("description", getUserDescription(showUser, itemcount1.getCount(), itemcount2.getCount()));
		model.addAttribute("ogimage", utils.getFullAvatarURL(showUser, AvatarFormat.Profile));
		model.addAttribute("includeGMapsScripts", utils.isNearMeFeatureEnabled());
		model.addAttribute("showUser", showUser);
		model.addAttribute("isMyProfile", isMyProfile);
		model.addAttribute("badgesCount", showUser.getBadgesMap().size() + showUser.getTags().size());
		model.addAttribute("tagsSet", new HashSet<>(showUser.getTags()));
		model.addAttribute("customBadgesMap", pc.findQuery(Utils.type(Badge.class), "*", new Pager(100)).stream().
				collect(Collectors.toMap(k -> ((Badge) k).getTag(), v -> v)));
		model.addAttribute("canEdit", isMyProfile || canEditProfile(authUser, id));
		model.addAttribute("canEditAvatar", CONF.avatarEditsEnabled());
		model.addAttribute("gravatarPicture", gravatarAvatarGenerator.getLink(showUser, AvatarFormat.Profile));
		model.addAttribute("isGravatarPicture", gravatarAvatarGenerator.isLink(showUser.getPicture()));
		model.addAttribute("includeEmojiPicker", true);
		model.addAttribute("itemcount1", itemcount1);
		model.addAttribute("itemcount2", itemcount2);
		model.addAttribute("questionslist", questionslist);
		model.addAttribute("answerslist", answerslist);
		model.addAttribute("nameEditsAllowed", CONF.nameEditsEnabled());
		return "base";
	}

	@PostMapping("/{id}/make-mod")
	public String makeMod(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = utils.getAuthUser(req);
		if (!isMyid(authUser, Profile.id(id))) {
			Profile showUser = pc.read(Profile.id(id));
			if (showUser != null) {
				if (utils.isAdmin(authUser) && !utils.isAdmin(showUser)) {
					if (CONF.modsAccessAllSpaces()) {
						showUser.setGroups(utils.isMod(showUser) ? USERS.toString() : MODS.toString());
					} else {
						String space = req.getParameter("space");
						if (showUser.isModInSpace(space)) {
							showUser.getModspaces().remove(space);
						} else {
							showUser.getModspaces().add(space);
						}
					}
					showUser.update();
				}
			}
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "base";
		} else {
			return "redirect:" + PROFILELINK + "/" + id;
		}
	}

	@PostMapping("/{id}")
	public String edit(@PathVariable(required = false) String id, @RequestParam(required = false) String name,
			@RequestParam(required = false) String location, @RequestParam(required = false) String latlng,
			@RequestParam(required = false) String website, @RequestParam(required = false) String aboutme,
			@RequestParam(required = false) String picture, @RequestParam(required = false) String email,
			HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		Profile showUser = getProfileForEditing(id, authUser);
		String queryString = "";
		if (showUser != null) {
			boolean updateProfile = false;
			if (!StringUtils.equals(showUser.getLocation(), location)) {
				showUser.setLatlng(latlng);
				showUser.setLocation(location);
				updateProfile = true;
			}
			if (!StringUtils.equals(showUser.getWebsite(), website) &&
					(StringUtils.isBlank(website) || Utils.isValidURL(website))) {
				showUser.setWebsite(website);
				updateProfile = true;
			}
			if (!StringUtils.equals(showUser.getAboutme(), aboutme)) {
				showUser.setAboutme(aboutme);
				updateProfile = true;
			}
			if (Utils.isValidEmail(email) && canChangeEmail(showUser.getUser(), email)) {
				if (utils.isAdmin(authUser) || CONF.allowUnverifiedEmails()) {
					changeEmail(showUser.getUser(), showUser, email);
				} else {
					if (!utils.isEmailDomainApproved(email)) {
						queryString = "?code=9&error=true";
					} else if (!isAvailableEmail(email)) {
						queryString = "?code=1&error=true";
					} else if (sendConfirmationEmail(showUser.getUser(), showUser, email, req)) {
						updateProfile = true;
						queryString = "?code=signin.verify.start&success=true";
					} else {
						queryString = "?code=signin.verify.fail&error=true";
					}
				}
			}

			updateProfile = updateUserPictureAndName(showUser, picture, name) || updateProfile;

			if (updateProfile) {
				showUser.update();
			}
			model.addAttribute("user", showUser);
		}
		return "redirect:" + PROFILELINK + (isMyid(authUser, id) ? "" : "/" + id) + queryString;
	}

	@SuppressWarnings("unchecked")
	@ResponseBody
	@PostMapping(value = "/{id}/cloudinary-upload-link", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> generateCloudinaryUploadLink(@PathVariable String id, HttpServletRequest req) {
		if (!ScooldUtils.isCloudinaryAvatarRepositoryEnabled()) {
			return ResponseEntity.status(404).build();
		}

		Profile authUser = utils.getAuthUser(req);
		Profile showUser = getProfileForEditing(id, authUser);
		if (showUser == null) {
			return ResponseEntity.status(403).build();
		}

		String preset = "avatar";
		String publicId = "avatars/" + id;
		long timestamp = Utils.timestamp() / 1000;
		Cloudinary cloudinary = new Cloudinary(CONF.cloudinaryUrl());
		String signature = cloudinary.apiSignRequest(ObjectUtils.asMap(
			"public_id", publicId,
			"timestamp", String.valueOf(timestamp),
			"upload_preset", preset
		), cloudinary.config.apiSecret);

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("url", "https://api.cloudinary.com/v1_1/" + cloudinary.config.cloudName + "/image/upload");
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("resource_type", "image");
		data.put("public_id", publicId);
		data.put("upload_preset", preset);
		data.put("filename", id);
		data.put("timestamp", timestamp);
		data.put("api_key", cloudinary.config.apiKey);
		data.put("signature", signature);
		response.put("data", data);

		return ResponseEntity.ok().body(response);
	}

	@PostMapping("/{id}/create-badge")
	public String createBadge(@PathVariable String id, @RequestParam String tag,
			@RequestParam(required = false, defaultValue = "") String description,
			@RequestParam(required = false, defaultValue = "#FFFFFF") String color,
			@RequestParam(required = false, defaultValue = "#555555") String background,
			@RequestParam(required = false, defaultValue = "") String icon,
			HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		Profile showUser = getProfileForEditing(id, authUser);
		if (showUser != null && utils.isMod(authUser)) {
			Badge b = new Badge(tag);
			b.setIcon(icon);
			b.setStyle(Utils.formatMessage("background-color: {0}; color: {1};", background, color));
			b.setDescription(StringUtils.isBlank(description) ? tag : description);
			b.setCreatorid(authUser.getCreatorid());
			pc.create(b);
			showUser.addCustomBadge(b);
			showUser.update();
		}
		return "redirect:" + PROFILELINK + (isMyid(authUser, id) ? "" : "/" + id);
	}

	@PostMapping("/delete-badge/{id}")
	public String deleteBadge(@PathVariable String id, HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (id != null && utils.isMod(authUser)) {
			Badge b = pc.read(new Badge(id).getId());
			if (b != null) {
				pc.delete(b);
				pc.updateAllPartially((toUpdate, pager) -> {
					List<Profile> profiles = pc.findTagged(Utils.type(Profile.class), new String[]{id}, pager);
					for (Profile p : profiles) {
						p.removeCustomBadge(b.getTag());
						Map<String, Object> profile = new HashMap<>();
						profile.put(Config._ID, p.getId());
						profile.put(Config._TAGS, p.getTags().stream().
								filter(t -> !t.equals(id)).distinct().collect(Collectors.toList()));
						profile.put("customBadges", p.getCustomBadges());
						toUpdate.add(profile);
					}
					return profiles;
				});
			}
		}
		return "redirect:" + PROFILELINK + (isMyid(authUser, id) ? "" : "/" + id);
	}

	@PostMapping("/{id}/toggle-badge/{tag}")
	public ResponseEntity<?> toggleBadge(@PathVariable String id, @PathVariable String tag, HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		Profile showUser = getProfileForEditing(id, authUser);
		if (showUser != null && utils.isMod(authUser)) {
			Badge b = new Badge(tag);
			if (!showUser.removeCustomBadge(b.getTag())) {
				showUser.addCustomBadge(pc.read(b.getId()));
			}
			showUser.update();
		}
		return ResponseEntity.ok().build();
	}

	@GetMapping(path = "/confirm-email")
	public String confirmEmail(@RequestParam(name = "id", required = false) String id,
			@RequestParam(name = "token", required = false) String token,
			@RequestParam(name = "token2", required = false) String token2,
			HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (authUser == null || !CONF.passwordAuthEnabled()) {
			return "redirect:" + SIGNINLINK + "?returnto=" + PROFILELINK;
		}
		if (id != null && (!StringUtils.isBlank(token) || !StringUtils.isBlank(token2))) {
			User u = (User) pc.read(id);
			Sysprop s = pc.read(u.getIdentifier());
			if (s != null && StringUtils.equals(token, (String) s.getProperty(Config._EMAIL_TOKEN))) {
				s.addProperty(Config._EMAIL_TOKEN, "");
				pc.update(s);
				if (StringUtils.isBlank((String) s.getProperty(Config._EMAIL_TOKEN + "2"))) {
					return changeEmail(u, authUser, authUser.getPendingEmail());
				}
				return "redirect:" + PROFILELINK + "?code=signin.verify.start&success=true";
			} else if (s != null && StringUtils.equals(token2, (String) s.getProperty(Config._EMAIL_TOKEN + "2"))) {
				s.addProperty(Config._EMAIL_TOKEN + "2", "");
				pc.update(s);
				if (StringUtils.isBlank((String) s.getProperty(Config._EMAIL_TOKEN))) {
					return changeEmail(u, authUser, authUser.getPendingEmail());
				}
				return "redirect:" + PROFILELINK + "?code=signin.verify.start&success=true";
			} else {
				return "redirect:" + SIGNINLINK;
			}
		}
		return "redirect:" + PROFILELINK;
	}

	@PostMapping(path = "/retry-change-email")
	public String retryChangeEmail(HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (authUser == null) {
			return "redirect:" + SIGNINLINK + "?returnto=" + PROFILELINK;
		}
		if (!StringUtils.isBlank(authUser.getPendingEmail())) {
			if (!isAvailableEmail(authUser.getPendingEmail())) {
				return "redirect:" + PROFILELINK + "?code=1&error=true";
			}
			if (!sendConfirmationEmail(authUser.getUser(), authUser, authUser.getPendingEmail(), req)) {
				return "redirect:" + PROFILELINK + "?code=signin.verify.fail&error=true";
			}
		}
		return "redirect:" + PROFILELINK + "?code=signin.verify.start&success=true";
	}

	@PostMapping(path = "/cancel-change-email")
	public String cancelChangeEmail(HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (authUser == null) {
			return "redirect:" + SIGNINLINK + "?returnto=" + PROFILELINK;
		}
		if (!StringUtils.isBlank(authUser.getPendingEmail())) {
			authUser.setPendingEmail("");
			User u = (User) pc.read(authUser.getCreatorid());
			Sysprop s = pc.read(u.getIdentifier());
			if (s != null) {
				s.removeProperty(Config._EMAIL_TOKEN);
				s.removeProperty(Config._EMAIL_TOKEN + "2");
				s.removeProperty("confirmationTimestamp");
				pc.updateAll(List.of(s, authUser));
			} else {
				authUser.update();
			}
		}
		return "redirect:" + PROFILELINK;
	}

	@PostMapping("/toggle-editor-role")
	public String toggleEditorRole(HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (authUser != null && StringUtils.equalsAny(authUser.getGroups(),
				User.Groups.ADMINS.toString(), User.Groups.MODS.toString())) {
			authUser.setEditorRoleEnabled(!authUser.getEditorRoleEnabled());
			authUser.update();
		}
		return "redirect:" + HttpUtils.getBackToUrl(req, true);
	}

	private String changeEmail(User u, Profile showUser, String email) {
		boolean approvedDomain = utils.isEmailDomainApproved(email);
		if (approvedDomain && canChangeEmail(u, email)) {
			Sysprop s = pc.read(u.getEmail());
			if (s != null && pc.read(email) == null) {
				pc.delete(s);
				s.setId(email);
				pc.create(s);
				u.setEmail(email);
				showUser.setPendingEmail("");
				pc.updateAll(List.of(u, showUser));
				return "redirect:" + PROFILELINK + "?code=signin.verify.changed&success=true";
			} else {
				logger.info("Failed to change email for user {} - email {} has already been taken.", u.getId(), email);
				return "redirect:" + PROFILELINK + "?code=1&error=true";
			}
		}
		return "redirect:" + PROFILELINK + "?code=9&error=true";
	}

	private boolean sendConfirmationEmail(User user, Profile showUser, String email, HttpServletRequest req) {
		Sysprop ident = pc.read(user.getEmail());
		if (ident != null) {
			if (!ident.hasProperty("confirmationTimestamp") || Utils.timestamp() >
				((long) ident.getProperty("confirmationTimestamp") + TimeUnit.HOURS.toMillis(6))) {
				showUser.setPendingEmail(email);
				utils.sendVerificationEmail(ident, email, PROFILELINK + "/confirm-email", req);
				return true;
			} else {
				logger.warn("Failed to send email confirmation to '{}' - this can only be done once every 6h.", email);
			}
		}
		return false;
	}

	private boolean isAvailableEmail(String email) {
		boolean b = pc.read(email) == null && pc.findTerms(Utils.type(User.class), Map.of(Config._EMAIL, email), true).isEmpty();
		if (!b) {
			logger.info("Failed to send confirmation email to user - email {} has already been taken.", email);
		}
		return b;
	}

	private boolean canChangeEmail(User u, String email) {
		return "generic".equals(u.getIdentityProvider()) && !StringUtils.equals(u.getEmail(), email);
	}

	private Profile getProfileForEditing(String id, Profile authUser) {
		if (!canEditProfile(authUser, id)) {
			return null;
		}
		return isMyid(authUser, id) ? authUser : (Profile) pc.read(Profile.id(id));
	}

	private boolean updateUserPictureAndName(Profile showUser, String picture, String name) {
		boolean updateProfile = false;
		boolean updateUser = false;
		User u = showUser.getUser();

		if (CONF.avatarEditsEnabled() && !StringUtils.isBlank(picture)) {
			updateProfile = avatarRepository.store(showUser, picture);
		}

		if (CONF.nameEditsEnabled() && !StringUtils.isBlank(name)) {
			showUser.setName(StringUtils.abbreviate(name, 256));
			if (StringUtils.isBlank(showUser.getOriginalName())) {
				showUser.setOriginalName(name);
			}
			if (!u.getName().equals(name)) {
				u.setName(name);
				updateUser = true;
			}
			updateProfile = true;
		}

		if (updateUser) {
			pc.update(u);
		}
		return updateProfile;
	}

	private boolean isMyid(Profile authUser, String id) {
		return authUser != null && (StringUtils.isBlank(id) || authUser.getId().equals(Profile.id(id)));
	}

	private boolean canEditProfile(Profile authUser, String id) {
		return isMyid(authUser, id) || utils.isAdmin(authUser);
	}

	private Object getUserDescription(Profile showUser, Long questions, Long answers) {
		if (showUser == null) {
			return "";
		}
		return showUser.getVotes() + " points, "
				+ showUser.getBadgesMap().size() + " badges, "
				+ questions + " questions, "
				+ answers + " answers "
				+ Utils.abbreviate(showUser.getAboutme(), 150);
	}

	public List<? extends Post> getQuestions(Profile authUser, Profile showUser, boolean isMyProfile, Pager itemcount) {
		String spaceFilter = getSpaceFilter(authUser, isMyProfile);
		if (isMyProfile || utils.isMod(authUser)) {
			return pc.findQuery("", getTypeQuery(Utils.type(Question.class), Utils.type(Sticky.class),
					Utils.type(UnapprovedQuestion.class)) + " AND " + getAuthorQuery(showUser) + spaceFilter, itemcount);
		} else {
			return pc.findQuery("", getTypeQuery(Utils.type(Question.class), Utils.type(Sticky.class))
								+ " AND " + getAuthorQuery(showUser) + spaceFilter, itemcount);
		}
	}

	public List<? extends Post> getAnswers(Profile authUser, Profile showUser, boolean isMyProfile, Pager itemcount) {
		String spaceFilter = getSpaceFilter(authUser, isMyProfile);
		if (isMyProfile || utils.isMod(authUser)) {
			return pc.findQuery("", getTypeQuery(Utils.type(Reply.class), Utils.type(UnapprovedReply.class))
								+ " AND " + getAuthorQuery(showUser) + spaceFilter, itemcount);
		} else {
			return pc.findQuery("", getTypeQuery(Utils.type(Reply.class))
					+ " AND " + getAuthorQuery(showUser) + spaceFilter, itemcount);
		}
	}

	private String getTypeQuery(String... types) {
		return Config._TYPE + ":(" + String.join(" OR ", types) + ")";
	}

	private String getAuthorQuery(Profile showUser) {
		return Config._CREATORID + ":(\"" + showUser.getId() + "\")";
	}

	private String getSpaceFilter(Profile authUser, boolean isMyProfile) {
		String spaceFilter;
		if (utils.isMod(authUser) || isMyProfile) {
			spaceFilter = "";
		} else if (authUser != null && authUser.hasSpaces()) {
			spaceFilter = "(" + authUser.getSpaces().stream().map(s -> "properties.space:\"" + s + "\"").
					collect(Collectors.joining(" OR ")) + ")";
		} else {
			spaceFilter = "properties.space:\"" + DEFAULT_SPACE + "\"";
		}
		spaceFilter = StringUtils.isBlank(spaceFilter) ? "" : " AND " + spaceFilter;
		return spaceFilter;
	}
}
