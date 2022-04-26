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
import com.erudika.para.core.User;
import static com.erudika.para.core.User.Groups.MODS;
import static com.erudika.para.core.User.Groups.USERS;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.PEOPLELINK;
import static com.erudika.scoold.ScooldServer.PROFILELINK;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Profile.Badge;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.utils.ScooldUtils;
import com.erudika.scoold.utils.avatars.*;
import java.util.*;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
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

	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private final ScooldUtils utils;
	private final GravatarAvatarGenerator gravatarAvatarGenerator;
	private final AvatarRepository avatarRepository;

	@Inject
	public ProfileController(ScooldUtils utils, GravatarAvatarGenerator gravatarAvatarGenerator, AvatarRepositoryProxy avatarRepository) {
		this.utils = utils;
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
			showUser = utils.getParaClient().read(Profile.id(id));
			isMyProfile = isMyid(authUser, Profile.id(id));
		}

		if (showUser == null || !ParaObjectUtils.typesMatch(showUser)) {
			return "redirect:" + PROFILELINK;
		}

		boolean protekted = !utils.isDefaultSpacePublic() && !utils.isAuthenticated(req);
		boolean sameSpace = (utils.canAccessSpace(showUser, "default") && utils.canAccessSpace(authUser, "default")) ||
				(authUser != null && showUser.getSpaces().stream().anyMatch(s -> utils.canAccessSpace(authUser, s)));
		if (protekted || !sameSpace) {
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
		model.addAttribute("badgesCount", showUser.getBadgesMap().size());
		model.addAttribute("canEdit", isMyProfile || canEditProfile(authUser, id));
		model.addAttribute("canEditAvatar", CONF.avatarEditsEnabled());
		model.addAttribute("gravatarPicture", gravatarAvatarGenerator.getLink(showUser, AvatarFormat.Profile));
		model.addAttribute("isGravatarPicture", gravatarAvatarGenerator.isLink(showUser.getPicture()));
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
			Profile showUser = utils.getParaClient().read(Profile.id(id));
			if (showUser != null) {
				if (utils.isAdmin(authUser) && !utils.isAdmin(showUser)) {
					showUser.setGroups(utils.isMod(showUser) ? USERS.toString() : MODS.toString());
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
			@RequestParam(required = false) String picture, HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		Profile showUser = getProfileForEditing(id, authUser);
		if (showUser != null) {
			boolean updateProfile = false;
			if (!isMyid(authUser, id)) {
				showUser = utils.getParaClient().read(Profile.id(id));
			}
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

			updateProfile = updateUserPictureAndName(showUser, picture, name) || updateProfile;

			boolean isComplete = showUser.isComplete() && isMyid(authUser, showUser.getId());
			if (updateProfile || utils.addBadgeOnce(showUser, Badge.NICEPROFILE, isComplete)) {
				showUser.update();
			}
			model.addAttribute("user", showUser);
		}
		return "redirect:" + PROFILELINK + (isMyid(authUser, id) ? "" : "/" + id);
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

	private Profile getProfileForEditing(String id, Profile authUser) {
		if (!canEditProfile(authUser, id)) {
			return null;
		}
		return isMyid(authUser, id) ? authUser : (Profile) utils.getParaClient().read(Profile.id(id));
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
			utils.getParaClient().update(u);
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
		if (utils.postsNeedApproval() && (isMyProfile || utils.isMod(authUser))) {
			List<Question> qlist = new ArrayList<>();
			Pager p = new Pager(itemcount.getPage(), itemcount.getLimit());
			qlist.addAll(showUser.getAllQuestions(itemcount));
			qlist.addAll(showUser.getAllUnapprovedQuestions(p));
			itemcount.setCount(itemcount.getCount() + p.getCount());
			return qlist;
		} else {
			return showUser.getAllQuestions(itemcount);
		}
	}

	public List<? extends Post> getAnswers(Profile authUser, Profile showUser, boolean isMyProfile, Pager itemcount) {
		if (utils.postsNeedApproval() && (isMyProfile || utils.isMod(authUser))) {
			List<Reply> alist = new ArrayList<>();
			Pager p = new Pager(itemcount.getPage(), itemcount.getLimit());
			alist.addAll(showUser.getAllAnswers(itemcount));
			alist.addAll(showUser.getAllUnapprovedAnswers(p));
			itemcount.setCount(itemcount.getCount() + p.getCount());
			return alist;
		} else {
			return showUser.getAllAnswers(itemcount);
		}
	}
}
