/*
 * Copyright 2013-2020 Erudika. https://erudika.com
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
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
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
import com.erudika.scoold.utils.HttpUtils;
import java.util.concurrent.TimeUnit;

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
			return "redirect:" + HOMEPAGE;
		}
		model.addAttribute("path", "settings.vm");
		model.addAttribute("title", utils.getLang(req).get("settings.title"));
		model.addAttribute("newpostEmailsEnabled", utils.isSubscribedToNewPosts(req));
		model.addAttribute("includeGMapsScripts", utils.isNearMeFeatureEnabled());
		return "base";
	}

	@PostMapping
	public String post(@RequestParam(required = false) String tags, @RequestParam(required = false) String latlng,
			@RequestParam(required = false) String replyEmailsOn, @RequestParam(required = false) String commentEmailsOn,
			@RequestParam(required = false) String oldpassword, @RequestParam(required = false) String newpassword,
			@RequestParam(required = false) String newpostEmailsOn, @RequestParam(required = false) String favtagsEmailsOn,
			HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			setFavTags(authUser, tags);
			if (!StringUtils.isBlank(latlng)) {
				authUser.setLatlng(latlng);
			}
			setAnonymity(authUser, req.getParameter("anon"));
			setDarkMode(authUser, req.getParameter("dark"), req, res);
			authUser.setReplyEmailsEnabled(Boolean.valueOf(replyEmailsOn));
			authUser.setCommentEmailsEnabled(Boolean.valueOf(commentEmailsOn));
			authUser.setFavtagsEmailsEnabled(Boolean.valueOf(favtagsEmailsOn));
			authUser.update();

			if (Boolean.valueOf(newpostEmailsOn)) {
				utils.subscribeToNewPosts(authUser.getUser());
			} else {
				utils.unsubscribeFromNewPosts(authUser.getUser());
			}

			if (resetPasswordAndUpdate(authUser.getUser(), oldpassword, newpassword)) {
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
		return "redirect:" + SIGNINLINK + "?code=4&success=true";
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

	private void setAnonymity(Profile authUser, String anonParam) {
		if ("true".equalsIgnoreCase(anonParam)) {
			anonymizeProfile(authUser);
		} else if (authUser.getAnonymityEnabled()) {
			deanonymizeProfile(authUser);
		}
	}

	private void setDarkMode(Profile authUser, String darkParam, HttpServletRequest req, HttpServletResponse res) {
		if ("true".equalsIgnoreCase(darkParam)) {
			HttpUtils.setRawCookie("dark-mode", "1", req, res, false, (int) TimeUnit.DAYS.toSeconds(2 * 365));
		} else {
			HttpUtils.removeStateParam("dark-mode", req, res);
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
