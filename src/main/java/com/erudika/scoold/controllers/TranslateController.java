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

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Translation;
import com.erudika.para.utils.Pager;
import static com.erudika.scoold.ScooldServer.languageslink;
import static com.erudika.scoold.ScooldServer.translatelink;
import com.erudika.scoold.core.Profile;
import static com.erudika.scoold.core.Profile.Badge.POLYGLOT;
import com.erudika.scoold.utils.ScooldUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
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
@RequestMapping("/translate")
public class TranslateController {

	private final ScooldUtils utils;
	private final ParaClient pc;
	private List<String> langkeys;
	private Map<String, String> deflang;

	@Inject
	public TranslateController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
		langkeys = new ArrayList<String>();
		deflang = utils.getLangutils().getDefaultLanguage();
		for (String key : deflang.keySet()) {
			langkeys.add(key);
		}
	}

	@GetMapping
    public String get(HttpServletRequest req, Model model) {
		return "redirect:" + languageslink;
	}

	@GetMapping({"/{locale}", "/{locale}/{index}"})
    public String translate(@PathVariable String locale, @PathVariable(required = false) String index,
			HttpServletRequest req, Model model) {

		Locale showLocale = utils.getLangutils().getProperLocale(locale);
		if (showLocale == null || "en".equals(showLocale.getLanguage())) {
			// can't translate default language
			return "redirect:" + languageslink;
		}

		int showIndex = -1;
		List<Translation> translationslist = Collections.emptyList();
		Pager itemcount = utils.getPager("page", req);
		if (!StringUtils.isBlank(index)) {
			showIndex = getIndex(index, langkeys);
			// this is what is currently shown for translation
			translationslist = utils.getLangutils().
					readAllTranslationsForKey(showLocale.getLanguage(), langkeys.get(showIndex), itemcount);
		}

		String title = utils.getLang(req).get("translate.title") + " - " + showLocale.getDisplayName(showLocale);
		int showLocaleProgress = utils.getLangutils().getTranslationProgressMap().get(showLocale.getLanguage());
		model.addAttribute("path", "translate.vm");
		model.addAttribute("title", title);
		model.addAttribute("showIndex", showIndex);
		model.addAttribute("showLocale", showLocale);
		model.addAttribute("langkeys", langkeys);
		model.addAttribute("deflang", deflang);
		model.addAttribute("showLocaleProgress", showLocaleProgress);
		model.addAttribute("translationslist", translationslist);
		model.addAttribute("itemcount", itemcount);
        return "base";
    }

	@PostMapping("/{locale}/{index}")
    public String post(@PathVariable String locale, @PathVariable String index, @RequestParam String value,
			HttpServletRequest req, Model model) {
		Locale showLocale = utils.getLangutils().getProperLocale(locale);
		if (utils.isAuthenticated(req) && showLocale != null && !"en".equals(showLocale.getLanguage())) {
			Set<String> approved = utils.getLangutils().getApprovedTransKeys(showLocale.getLanguage());
			Profile authUser = utils.getAuthUser(req);
			String langkey = langkeys.get(getIndex(index, langkeys));
			boolean isTranslated = approved.contains(langkey);
			if (!StringUtils.isBlank(value) && (!isTranslated || utils.isAdmin(authUser))) {
				Translation trans = new Translation(showLocale.getLanguage(), langkey, StringUtils.trim(value));
				trans.setCreatorid(authUser.getId());
				trans.setAuthorName(authUser.getName());
				trans.setTimestamp(System.currentTimeMillis());
				pc.create(trans);
				model.addAttribute("newtranslation", trans);
			}
			if (!utils.isAjaxRequest(req)) {
				return "redirect:" + translatelink + "/" + showLocale.getLanguage() + "/" +
						getNextIndex(getIndex(index, langkeys), approved, langkeys);
			}
		}

		return "base";
    }

	@PostMapping("/approve/{id}")
    public String approve(@PathVariable String id, HttpServletRequest req, Model model) {
		Translation trans = (Translation) pc.read(id);
		if (trans != null && utils.isAuthenticated(req)) {
			if (trans.getApproved()) {
				trans.setApproved(false);
				utils.getLangutils().disapproveTranslation(trans.getLocale(), trans.getId());
			} else {
				trans.setApproved(true);
				utils.getLangutils().approveTranslation(trans.getLocale(), trans.getThekey(), trans.getValue());
				utils.addBadge((Profile) pc.read(trans.getCreatorid()), POLYGLOT, true, true);
			}
			pc.update(trans);
		}
		if (!utils.isAjaxRequest(req)) {
			return "redirect:" + req.getRequestURI();
		}
		return "base";
	}

	@PostMapping("/delete/{id}")
    public String delete(@PathVariable String id, HttpServletRequest req, Model model) {
		if (id != null && utils.isAuthenticated(req)) {
			Translation trans = (Translation) pc.read(id);
			Profile authUser = utils.getAuthUser(req);
			if (authUser.getId().equals(trans.getCreatorid()) || utils.isAdmin(authUser)) {
				utils.getLangutils().disapproveTranslation(trans.getLocale(), trans.getId());
				pc.delete(trans);
			}
			if (!utils.isAjaxRequest(req)) {
				return "redirect:" + req.getRequestURI();
			}
		}
		return "base";
	}

	private int getNextIndex(int fromIndex, Set<String> approved, List<String> langkeys) {
		int start = fromIndex;
		if (start < 0) start = 0;
		if (start >= approved.size()) start = approved.size() - 1;
		int nexti = (start + 1) >= langkeys.size() ? 0 : (start + 1);

		// if there are untranslated strings go directly there
		if (approved.size() != langkeys.size()) {
			while(approved.contains(langkeys.get(nexti))) {
				nexti = (nexti + 1) >= langkeys.size() ? 0 : (nexti + 1);
			}
		}
		return nexti;
	}

	private int getIndex(String index, List<String> langkeys) {
		int showIndex = NumberUtils.toInt(index, 1) - 1;
		if (showIndex <= 0) {
			showIndex = 0;
		} else if (showIndex >= langkeys.size()) {
			showIndex = langkeys.size() - 1;
		}
		return showIndex;
	}
}
