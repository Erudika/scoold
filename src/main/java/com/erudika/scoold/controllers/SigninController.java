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

@Controller
public class SigninController {

	private final ScooldUtils utils;

	@Inject
    public SigninController(ScooldUtils utils) {
		this.utils = utils;
    }

	@GetMapping("/signin")
    public String get(HttpServletRequest req, Model model) {
		if (utils.isAuthenticated(req)) {
			return "redirect:" + HOMEPAGE;
		}
		model.addAttribute("path", "signin.vm");
		model.addAttribute("title", utils.getLang(req).get("signin.title"));
        return "base";
    }

	@GetMapping(path = "/signin", params = {"access_token", "provider"})
    public String getAuth(@RequestParam("access_token") String accessToken, @RequestParam("provider") String provider,
			@RequestParam(name = "returnto", required = false, defaultValue = HOMEPAGE) String returnto,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		if (!utils.isAuthenticated(req)) {
			User u = utils.getParaClient().signIn(provider, accessToken, false);
			if (u != null) {
				Utils.setStateParam(Config.AUTH_COOKIE, u.getPassword(), req, res, true);
				String backto = Utils.urlDecode(returnto);
				return "redirect:" + (StringUtils.isBlank(backto) ? HOMEPAGE : backto);
			} else {
				return "redirect:" + signinlink + "?code=3&error=true";
			}
		}
		if (!HOMEPAGE.equals(returnto)) {
			Utils.setStateParam("returnto", Utils.urlEncode(returnto), req, res);
		} else {
			Utils.removeStateParam("returnto", req, res);
		}
		return "redirect:" + returnto;
	}

	@PostMapping("/signout")
    public String post(HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			utils.clearSession(req, res);
			return "redirect:" + signinlink + "?code=5&success=true";
		}
        return "redirect:" + HOMEPAGE;
    }
}
