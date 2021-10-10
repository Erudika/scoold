/*
 * Copyright 2013-2021 Erudika. https://erudika.com
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

import com.erudika.para.core.Sysprop;
import com.erudika.para.core.User;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Utils;
import static com.erudika.scoold.ScooldServer.MAX_FAV_TAGS;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.ScooldUtils;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import static com.erudika.scoold.ScooldServer.SETTINGSLINK;
import java.util.List;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/settings")
public class SettingsController {

	private final ScooldUtils utils;

	@Inject
	public SettingsController(ScooldUtils utils) {
		this.utils = utils;
	}

	@GetMapping
	public String get(HttpServletRequest req, Model model) {
		if (!utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + SETTINGSLINK;
		}
		model.addAttribute("path", "settings.vm");
		model.addAttribute("title", utils.getLang(req).get("settings.title"));
		model.addAttribute("newpostEmailsEnabled", utils.isSubscribedToNewPosts(req));
		model.addAttribute("emailsAllowed", utils.isNotificationsAllowed());
		model.addAttribute("newpostEmailsAllowed", utils.isNewPostNotificationAllowed());
		model.addAttribute("favtagsEmailsAllowed", utils.isFavTagsNotificationAllowed());
		model.addAttribute("replyEmailsAllowed", utils.isReplyNotificationAllowed());
		model.addAttribute("commentEmailsAllowed", utils.isCommentNotificationAllowed());
		model.addAttribute("includeGMapsScripts", utils.isNearMeFeatureEnabled());
		return "base";
	}

	@PostMapping
	public String post(@RequestParam(required = false) String tags, @RequestParam(required = false) String latlng,
			@RequestParam(required = false) String replyEmailsOn, @RequestParam(required = false) String commentEmailsOn,
			@RequestParam(required = false) String oldpassword, @RequestParam(required = false) String newpassword,
			@RequestParam(required = false) String newpostEmailsOn, @RequestParam(required = false) String favtagsEmailsOn,
			@RequestParam(required = false) List<String> favspaces, HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			setFavTags(authUser, tags);
			setFavSpaces(authUser, favspaces);
			if (!StringUtils.isBlank(latlng)) {
				authUser.setLatlng(latlng);
			}
			setAnonymity(authUser, req.getParameter("anon"));
			setDarkMode(authUser, req.getParameter("dark"));
			authUser.setReplyEmailsEnabled(Boolean.valueOf(replyEmailsOn) && utils.isReplyNotificationAllowed());
			authUser.setCommentEmailsEnabled(Boolean.valueOf(commentEmailsOn) && utils.isCommentNotificationAllowed());
			authUser.setFavtagsEmailsEnabled(Boolean.valueOf(favtagsEmailsOn) && utils.isFavTagsNotificationAllowed());
			authUser.update();

			if (Boolean.valueOf(newpostEmailsOn) && utils.isNewPostNotificationAllowed()) {
				utils.subscribeToNewPosts(authUser.getUser());
			} else {
				utils.unsubscribeFromNewPosts(authUser.getUser());
			}

			if (resetPasswordAndUpdate(authUser.getUser(), oldpassword, newpassword)) {
				utils.clearSession(req, res);
				return "redirect:" + SETTINGSLINK + "?passChanged=true";
			}
		}
		return "redirect:" + SETTINGSLINK;
	}

	@PostMapping("/goodbye")
	public String deleteAccount(HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			utils.getAuthUser(req).delete();
			utils.clearSession(req, res);
		}
		return "redirect:" + Config.getConfigParam("signout_url", SIGNINLINK + "?code=4&success=true");
	}

	private boolean resetPasswordAndUpdate(User u, String pass, String newpass) {
		if (u != null && !StringUtils.isBlank(pass) && !StringUtils.isBlank(newpass) &&
				u.getIdentityProvider().equals("generic")) {
			Sysprop s = utils.getParaClient().read(u.getEmail());
			if (s != null && Utils.bcryptMatches(pass, (String) s.getProperty(Config._PASSWORD))) {
				String hashed = Utils.bcrypt(newpass);
				s.addProperty(Config._PASSWORD, hashed);
				u.setPassword(hashed);
				utils.getParaClient().update(s);
				return true;
			}
		}
		return false;
	}

	private void setFavTags(Profile authUser, String tags) {
		if (!StringUtils.isBlank(tags)) {
			Set<String> ts = new LinkedHashSet<String>();
			for (String tag : tags.split(",")) {
				if (!StringUtils.isBlank(tag) && ts.size() <= MAX_FAV_TAGS) {
					ts.add(tag);
				}
			}
			authUser.setFavtags(new LinkedList<String>(ts));
		} else {
			authUser.setFavtags(null);
		}
	}

	private void setFavSpaces(Profile authUser, List<String> spaces) {
		authUser.setFavspaces(null);
		if (spaces != null && !spaces.isEmpty()) {
			for (String space : spaces) {
				String spaceId = utils.getSpaceId(space);
				if (!StringUtils.isBlank(spaceId) && utils.canAccessSpace(authUser, spaceId)) {
					authUser.getFavspaces().add(spaceId);
				}
			}
		}
	}

	private void setAnonymity(Profile authUser, String anonParam) {
		if (utils.isAnonymityEnabled()) {
			if ("true".equalsIgnoreCase(anonParam)) {
				anonymizeProfile(authUser);
			} else if (authUser.getAnonymityEnabled()) {
				deanonymizeProfile(authUser);
			}
		}
	}

	private void setDarkMode(Profile authUser, String darkParam) {
		if (utils.isDarkModeEnabled()) {
			authUser.setDarkmodeEnabled("true".equalsIgnoreCase(darkParam));
			utils.getParaClient().update(authUser);
		}
	}

	private void anonymizeProfile(Profile authUser) {
		authUser.setName("Anonymous");
		authUser.setOriginalPicture(authUser.getPicture());
		authUser.setPicture(utils.getGravatar(authUser.getId() + "@scooldemail.com"));
		authUser.setAnonymityEnabled(true);
	}

	private void deanonymizeProfile(Profile authUser) {
		authUser.setName(authUser.getOriginalName());
		authUser.setPicture(authUser.getOriginalPicture());
		authUser.setAnonymityEnabled(false);
	}
}
