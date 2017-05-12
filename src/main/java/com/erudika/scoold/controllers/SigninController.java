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
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Utils;
import static com.erudika.scoold.ScooldServer.CSRF_COOKIE;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
import static com.erudika.scoold.ScooldServer.signinlink;
import com.erudika.scoold.utils.ScooldUtils;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SigninController {

	private final ScooldUtils utils;

	@Inject
    public SigninController(ScooldUtils utils) {
		this.utils = utils;
    }

	@GetMapping("/signin")
    public String get(@RequestParam(name = "returnto", required = false, defaultValue = HOMEPAGE) String returnto,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		if (utils.isAuthenticated(req)) {
			return "redirect:" + HOMEPAGE;
		}
		if (!HOMEPAGE.equals(returnto) && !signinlink.equals(returnto)) {
			Utils.setStateParam("returnto", Utils.urlEncode(returnto), req, res);
		} else {
			Utils.removeStateParam("returnto", req, res);
		}
		model.addAttribute("path", "signin.vm");
		model.addAttribute("title", utils.getLang(req).get("signin.title"));
		model.addAttribute("signinSelected", "navbtn-hover");
		model.addAttribute("fbLoginEnabled", !Config.FB_APP_ID.isEmpty());
		model.addAttribute("gpLoginEnabled", !Config.getConfigParam("google_client_id", "").isEmpty());
		model.addAttribute("ghLoginEnabled", !Config.GITHUB_APP_ID.isEmpty());
		model.addAttribute("inLoginEnabled", !Config.LINKEDIN_APP_ID.isEmpty());
		model.addAttribute("twLoginEnabled", !Config.TWITTER_APP_ID.isEmpty());
		model.addAttribute("msLoginEnabled", !Config.MICROSOFT_APP_ID.isEmpty());
        return "base";
    }

	@GetMapping(path = "/signin", params = {"access_token", "provider"})
    public String getAuth(@RequestParam("access_token") String accessToken, @RequestParam("provider") String provider,
			HttpServletRequest req, HttpServletResponse res) {
		if (!utils.isAuthenticated(req)) {
			User u = utils.getParaClient().signIn(provider, accessToken, false);
			if (u != null) {
				Utils.setStateParam(Config.AUTH_COOKIE, u.getPassword(), req, res, true);
			} else {
				return "redirect:" + signinlink + "?code=3&error=true";
			}
		}
		return "redirect:" + getBackToUrl(req);
	}

	@GetMapping("/signin/success")
    public String signinSuccess(@RequestParam String jwt, HttpServletRequest req, HttpServletResponse res, Model model) {
		if (!StringUtils.isBlank(jwt) && !"?".equals(jwt)) {
			Utils.setStateParam(Config.AUTH_COOKIE, jwt, req, res, true);
		} else {
			return "redirect:" + signinlink + "?code=3&error=true";
		}
		return "redirect:" + getBackToUrl(req);
	}

	@PostMapping("/signout")
    public String post(HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			utils.clearSession(req, res);
			return "redirect:" + signinlink + "?code=5&success=true";
		}
        return "redirect:" + HOMEPAGE;
    }

	@ResponseBody
	@GetMapping("/scripts/globals.js")
    public String globals(HttpServletRequest req, HttpServletResponse res) {
		res.setContentType("text/javascript");
		StringBuilder sb = new StringBuilder();
		sb.append("APPID = \"").append(Config.getConfigParam("access_key", "app:scoold").substring(4)).append("\"; ");
		sb.append("ENDPOINT = \"").append(utils.getParaClient().getEndpoint()).append("\"; ");
		sb.append("CSRF_COOKIE = \"").append(CSRF_COOKIE).append("\"; ");
		sb.append("FB_APP_ID = \"").append(Config.FB_APP_ID).append("\"; ");
		sb.append("GOOGLE_CLIENT_ID = \"").append(Config.getConfigParam("google_client_id", "")).append("\"; ");
		sb.append("GOOGLE_ANALYTICS_ID = \"").append(Config.getConfigParam("google_analytics_id", "")).append("\"; ");
		sb.append("GITHUB_APP_ID = \"").append(Config.GITHUB_APP_ID).append("\"; ");
		sb.append("LINKEDIN_APP_ID = \"").append(Config.LINKEDIN_APP_ID).append("\"; ");
		sb.append("TWITTER_APP_ID = \"").append(Config.TWITTER_APP_ID).append("\"; ");
		sb.append("MICROSOFT_APP_ID = \"").append(Config.MICROSOFT_APP_ID).append("\"; ");
		return sb.toString();
	}

	private String getBackToUrl(HttpServletRequest req) {
		String backtoFromCookie = Utils.urlDecode(Utils.getStateParam("returnto", req));
		return (StringUtils.isBlank(backtoFromCookie) ? HOMEPAGE : backtoFromCookie);
	}
}
