/*
 * Copyright 2013-2026 Erudika. https://erudika.com
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

import static com.erudika.scoold.ScooldServer.LANGUAGESLINK;
import com.erudika.scoold.utils.HttpUtils;
import com.erudika.scoold.utils.ScooldUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/languages")
public class LanguagesController {

	private final ScooldUtils utils;

	public LanguagesController(ScooldUtils utils) {
		this.utils = utils;
	}

	@GetMapping
	public String get(HttpServletRequest req, Model model) {
		model.addAttribute("path", "languages.vm");
		model.addAttribute("title", utils.getLang(req).get("translate.select"));
		Map<String, Integer> langProgressMap = utils.getLangutils().getTranslationProgressMap();
		model.addAttribute("langProgressMap", langProgressMap);
		model.addAttribute("allLocales", new TreeMap<>(utils.getLangutils().getAllLocales().entrySet().stream().
				filter(e -> langProgressMap.getOrDefault(e.getKey(), 0) > 70).
				collect(Collectors.toMap(k -> k.getKey(), v -> v.getValue()))));
		return "base";
	}

	@PostMapping("/{langkey}")
	public String post(@PathVariable String langkey, HttpServletRequest req, HttpServletResponse res) {
		Locale locale = utils.getCurrentLocale(langkey);
		if (locale != null) {
			int maxAge = 60 * 60 * 24 * 365;  //1 year
			HttpUtils.setRawCookie(ScooldUtils.getConfig().localeCookie(), locale.toString(), req, res, "Strict", maxAge);
		}
		return "redirect:" + LANGUAGESLINK;
	}
}
