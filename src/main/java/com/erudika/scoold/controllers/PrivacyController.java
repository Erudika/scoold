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
import com.erudika.para.utils.Config;
import static com.erudika.scoold.ScooldServer.PRIVACYLINK;
import com.erudika.scoold.utils.ScooldUtils;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/privacy")
public class PrivacyController {

	private final ScooldUtils utils;

	@Inject
	public PrivacyController(ScooldUtils utils) {
		this.utils = utils;
	}

	@GetMapping
	public String get(HttpServletRequest req, Model model) {
		model.addAttribute("path", "privacy.vm");
		model.addAttribute("title", utils.getLang(req).get("privacy.title"));
		model.addAttribute("privacyhtml", utils.getParaClient().read("template" + Config.SEPARATOR + "privacy"));
		return "base";
	}

	@PostMapping
	public String edit(@RequestParam String privacyhtml, HttpServletRequest req, Model model) {
		if (!utils.isAuthenticated(req) || !utils.isAdmin(utils.getAuthUser(req))) {
			return "redirect:" + PRIVACYLINK;
		}
		Sysprop privacy = new Sysprop("template" + Config.SEPARATOR + "privacy");
		if (StringUtils.isBlank(privacyhtml)) {
			utils.getParaClient().delete(privacy);
		} else {
			privacy.addProperty("html", privacyhtml);
			utils.getParaClient().create(privacy);
		}
		return "redirect:" + PRIVACYLINK;
	}
}
