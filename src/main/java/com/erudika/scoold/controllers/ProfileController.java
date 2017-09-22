/*
 * Copyright 2013-2017 Erudika. https://erudika.com
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

import com.erudika.para.core.User;
import static com.erudika.para.core.User.Groups.MODS;
import static com.erudika.para.core.User.Groups.USERS;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import static com.erudika.scoold.ScooldServer.profilelink;
import static com.erudika.scoold.ScooldServer.signinlink;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Profile.Badge;
import com.erudika.scoold.utils.ScooldUtils;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

	private final ScooldUtils utils;

	@Inject
	public ProfileController(ScooldUtils utils) {
		this.utils = utils;
	}

	@GetMapping({"", "/{id}/**"})
    public String get(@PathVariable(required = false) String id, HttpServletRequest req, Model model) {
		if (!utils.isAuthenticated(req) && StringUtils.isBlank(id)) {
			return "redirect:" + signinlink + "?returnto=" + profilelink;
		}
		Profile authUser = utils.getAuthUser(req);
		Profile showUser;
		boolean isMyProfile;

		if (StringUtils.isBlank(id)) {
			//requested userid !exists or = my userid => show my profile
			showUser = authUser;
			isMyProfile = true;
		} else {
			showUser = utils.getParaClient().read(Profile.id(id));
			isMyProfile = isMyid(authUser, Profile.id(id));
		}

		if (showUser == null || !ParaObjectUtils.typesMatch(showUser)) {
			return "redirect:" + profilelink;
		}
		Pager itemcount1 = utils.getPager("page1", req);
		Pager itemcount2 = utils.getPager("page2", req);
		List<? extends Post> questionslist = showUser.getAllQuestions(itemcount1);
		List<? extends Post> answerslist = showUser.getAllAnswers(itemcount2);

		model.addAttribute("path", "profile.vm");
		model.addAttribute("title", utils.getLang(req).get("profile.title") + " - " + showUser.getName());
		model.addAttribute("description", getUserDescription(showUser, itemcount1.getCount(), itemcount2.getCount()));
		model.addAttribute("ogimage", showUser.getPicture());
		model.addAttribute("includeGMapsScripts", true);
		model.addAttribute("showUser", showUser);
		model.addAttribute("isMyProfile", isMyProfile);
		model.addAttribute("badgesCount", showUser.getBadgesMap().size());
		model.addAttribute("canEdit", isMyProfile || canEditProfile(authUser, id));
		model.addAttribute("gravatarPicture", utils.getGravatar(showUser));
		model.addAttribute("itemcount1", itemcount1);
		model.addAttribute("itemcount2", itemcount2);
		model.addAttribute("questionslist", questionslist);
		model.addAttribute("answerslist", answerslist);
        return "base";
    }

	@PostMapping(path = "/{id}", params = {"makemod"})
    public String mods(@PathVariable String id, @RequestParam Boolean makemod, HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = utils.getAuthUser(req);
		if (!isMyid(authUser, Profile.id(id))) {
			Profile showUser = utils.getParaClient().read(Profile.id(id));
			if (showUser != null) {
				boolean isShowUserAdmin = User.Groups.ADMINS.toString().equals(showUser.getGroups());
				if (utils.isAdmin(authUser) && !isShowUserAdmin) {
					showUser.setGroups(makemod ? MODS.toString() : USERS.toString());
					showUser.update();
				}
			}
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "base";
		} else {
			return "redirect:" + profilelink + "/" + id;
		}
    }

	@PostMapping("/{id}")
    public String edit(@PathVariable(required = false) String id, @RequestParam(required = false) String name,
			@RequestParam(required = false) String location, @RequestParam(required = false) String website,
			@RequestParam(required = false) String aboutme, @RequestParam(required = false) String picture, HttpServletRequest req) {
		Profile authUser = utils.getAuthUser(req);
		String showId = StringUtils.isBlank(id) ? authUser.getId() : id;
		if (canEditProfile(authUser, showId)) {
			Profile showUser = utils.getParaClient().read(Profile.id(id));
			boolean update = false;

			if (!StringUtils.isBlank(name)) {
				showUser.setName(name);
				update = true;
			}
			if (!StringUtils.isBlank(location)) {
				showUser.setLocation(location);
				update = true;
			}
			if (!StringUtils.isBlank(website)) {
				showUser.setWebsite(website);
				update = true;
			}
			if (!StringUtils.isBlank(aboutme)) {
				showUser.setAboutme(aboutme);
				update = true;
			}
			if (!StringUtils.isBlank(picture)) {
				showUser.setPicture(picture);
				update = true;
			}

			boolean isComplete = showUser.isComplete() && isMyid(authUser, showUser.getId());
			if (update || utils.addBadgeOnce(authUser, Badge.NICEPROFILE, isComplete)) {
				showUser.update();
			}
		}
		return "redirect:" + profilelink;
    }

	private boolean isMyid(Profile authUser, String id) {
		return authUser != null && authUser.getId().equals(id);
	}

	private boolean canEditProfile(Profile authUser, String id) {
		return isMyid(authUser, Profile.id(id)) || utils.isAdmin(authUser);
	}

	private Object getUserDescription(Profile showUser, Long questions, Long answers) {
		if (showUser == null) {
			return "";
		}
		return showUser.getVotes() + " points, " +
				showUser.getBadgesMap().size() + " badges, " +
				questions + " questions, " +
				answers + " answers " +
				Utils.abbreviate(showUser.getAboutme(), 150);
	}
}
