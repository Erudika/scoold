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

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.PEOPLELINK;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.HttpUtils;
import com.erudika.scoold.utils.ScooldUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/people")
public class PeopleController {

	private static final ScooldConfig CONF = ScooldUtils.getConfig();

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public PeopleController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping(path = {"", "/bulk-edit"})
	public String get(@RequestParam(required = false, defaultValue = Config._TIMESTAMP) String sortby,
			@RequestParam(required = false, defaultValue = "*") String q, HttpServletRequest req, Model model) {
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + PEOPLELINK;
		}
		if (req.getRequestURI().endsWith("/bulk-edit")) {
			return "redirect:" + PEOPLELINK + "?bulkedit=true";
		}
		Profile authUser = utils.getAuthUser(req);
		getUsers(q, sortby, authUser, req, model);
		model.addAttribute("path", "people.vm");
		model.addAttribute("title", utils.getLang(req).get("people.title"));
		model.addAttribute("peopleSelected", "navbtn-hover");

		if (req.getParameter("bulkedit") != null && utils.isAdmin(authUser)) {
			List<ParaObject> spaces = pc.findQuery("scooldspace", "*", new Pager(Config.DEFAULT_LIMIT));
			model.addAttribute("spaces", spaces);
		}
		return "base";
	}

	@PostMapping("/bulk-edit")
	public String bulkEdit(@RequestParam(required = false) String[] selectedUsers,
			@RequestParam(required = false) final String[] selectedSpaces, HttpServletRequest req) {
		Profile authUser = utils.getAuthUser(req);
		boolean isAdmin = utils.isAdmin(authUser);
		String operation = req.getParameter("operation");
		String selection = req.getParameter("selection");
		if (isAdmin && ("all".equals(selection) || selectedUsers != null)) {
			// find all user objects even if there are more than 10000 users in the system
			Pager pager = new Pager(1, "_docid", false, ScooldUtils.getConfig().maxItemsPerPage());
			List<Profile> profiles;
			LinkedList<Map<String, Object>> toUpdate = new LinkedList<>();
			List<String> spaces = (selectedSpaces == null || selectedSpaces.length == 0) ?
					Collections.emptyList() : Arrays.asList(selectedSpaces);
			do {
				String query = (selection == null || "selected".equals(selection)) ?
						Config._ID + ":(\"" + String.join("\" \"", selectedUsers) + "\")" : "*";
				profiles = pc.findQuery(Utils.type(Profile.class), query, pager);
				profiles.stream().filter(p -> !utils.isMod(p)).forEach(p -> {
					if ("add".equals(operation)) {
						p.getSpaces().addAll(spaces);
					} else if ("remove".equals(operation)) {
						p.getSpaces().removeAll(spaces);
					} else {
						p.setSpaces(new HashSet<String>(spaces));
					}
					Map<String, Object> profile = new HashMap<>();
					profile.put(Config._ID, p.getId());
					profile.put("spaces", p.getSpaces());
					toUpdate.add(profile);
				});
			} while (!profiles.isEmpty());
			// always patch outside the loop because we modify _docid values!!!
			LinkedList<Map<String, Object>> batch = new LinkedList<>();
			while (!toUpdate.isEmpty()) {
				batch.add(toUpdate.pop());
				if (batch.size() >= 100) {
					// partial batch update
					pc.invokePatch("_batch", batch, Map.class);
					batch.clear();
				}
			}
			if (!batch.isEmpty()) {
				pc.invokePatch("_batch", batch, Map.class);
			}
		}
		return "redirect:" + PEOPLELINK + (isAdmin ? "?" + req.getQueryString() : "");
	}

	@GetMapping("/avatar")
	public void avatar(HttpServletRequest req, HttpServletResponse res, Model model) {
		// prevents reflected XSS. see https://brutelogic.com.br/poc.svg
		// for some reason the CSP header is not sent on these responses by the ScooldInterceptor
		utils.setSecurityHeaders(utils.getCSPNonce(), req, res);
		HttpUtils.getDefaultAvatarImage(res);
	}

	@PostMapping("/apply-filter")
	public String applyFilter(@RequestParam(required = false) String sortby, @RequestParam(required = false) String tab,
			@RequestParam(required = false, defaultValue = "false") Boolean bulkedit,
			@RequestParam(required = false) String[] havingSelectedSpaces,
			@RequestParam(required = false) String[] notHavingSelectedSpaces,
			@RequestParam(required = false, defaultValue = "false") String compactViewEnabled,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (utils.isMod(authUser)) {
			if (req.getParameter("clear") != null) {
				HttpUtils.removeStateParam("users-filter", req, res);
				HttpUtils.removeStateParam("users-view-compact", req, res);
			} else {
				List<String> havingSpaces = (havingSelectedSpaces == null || havingSelectedSpaces.length == 0) ?
						Collections.emptyList() : Arrays.asList(havingSelectedSpaces);
				List<String> notHavingSpaces = (notHavingSelectedSpaces == null || notHavingSelectedSpaces.length == 0) ?
						Collections.emptyList() : Arrays.asList(notHavingSelectedSpaces);

				Pager p = utils.pagerFromParams(req);
				List<String> spacesList = new ArrayList<String>();
				for (String s : havingSpaces) {
					spacesList.add(s);
				}
				for (String s : notHavingSpaces) {
					spacesList.add("-" + s);
				}
				p.setSelect(spacesList);
				savePagerToCookie(req, res, p);
				HttpUtils.setRawCookie("users-view-compact", compactViewEnabled,
						req, res, false, "Strict", (int) TimeUnit.DAYS.toSeconds(365));
			}
		}
		return "redirect:" + PEOPLELINK + (bulkedit ? "/bulk-edit" : "") + (StringUtils.isBlank(sortby) ? "" : "?sortby="
				+ Optional.ofNullable(StringUtils.trimToNull(sortby)).orElse(tab));
	}

	@SuppressWarnings("unchecked")
	public List<Profile> getUsers(String q, String sortby, Profile authUser, HttpServletRequest req, Model model) {
		Pager itemcount = getPagerFromCookie(req, utils.getPager("page", req), model);
		itemcount.setSortby(sortby);
		// [space query filter] + original query string
		String qs = utils.sanitizeQueryString(q, req);
		if (req.getParameter("bulkedit") != null && utils.isAdmin(authUser)) {
			qs = q;
		} else {
			qs = qs.replaceAll("properties\\.space:", "properties.spaces:");
		}

		if (!qs.endsWith("*") && q.equals("*")) {
			qs += " OR properties.groups:(admins OR mods)"; // admins are members of every space and always visible
		}

		Set<String> havingSpaces = Optional.ofNullable((Set<String>) model.getAttribute("havingSpaces")).orElse(Set.of());
		Set<String> notHavingSpaces = Optional.ofNullable((Set<String>) model.getAttribute("notHavingSpaces")).orElse(Set.of());
		String havingSpacesFilter = "";
		String notHavingSpacesFilter = "";
		if (!havingSpaces.isEmpty()) {
			havingSpacesFilter = "+\"" + String.join("\" +\"", havingSpaces) + "\" ";
		}
		if (!notHavingSpaces.isEmpty()) {
			notHavingSpacesFilter = "-\"" + String.join("\" -\"", notHavingSpaces) + "\"";
			if (havingSpaces.isEmpty()) {
				// at least one + keyword is needed otherwise no search results are returned
				havingSpacesFilter = "+\"" + utils.getDefaultSpace() + "\"";
			}
		}
		if (utils.isMod(authUser) && (!havingSpaces.isEmpty() || !notHavingSpaces.isEmpty())) {
			StringBuilder sb = new StringBuilder("*".equals(qs) ? "" : "(".concat(qs).concat(") AND "));
			sb.append("properties.spaces").append(":(").append(havingSpacesFilter).append(notHavingSpacesFilter).append(")");
			qs = sb.toString();
		}

		List<Profile> userlist = pc.findQuery(Utils.type(Profile.class), qs, itemcount);
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("userlist", userlist);
		return userlist;
	}

	private Pager getPagerFromCookie(HttpServletRequest req, Pager defaultPager, Model model) {
		try {
			defaultPager.setName("default_pager");
			String cookie = HttpUtils.getCookieValue(req, "users-filter");
			if (StringUtils.isBlank(cookie)) {
				return defaultPager;
			}
			Pager pager = ParaObjectUtils.getJsonReader(Pager.class).readValue(Utils.base64dec(cookie));
			pager.setPage(defaultPager.getPage());
			pager.setLastKey(null);
			pager.setCount(0);
			if (!pager.getSelect().isEmpty()) {
				Set<String> havingSpaces = new HashSet<String>();
				Set<String> notHavingSpaces = new HashSet<String>();
				pager.getSelect().stream().forEach((s) -> {
					if (s.startsWith("-")) {
						notHavingSpaces.add(StringUtils.removeStart(s, "-"));
					} else {
						havingSpaces.add(s);
					}
				});
				pager.setSelect(null);
				model.addAttribute("havingSpaces", havingSpaces);
				model.addAttribute("notHavingSpaces", notHavingSpaces);
			}
			return pager;
		} catch (JsonProcessingException ex) {
			return Optional.ofNullable(defaultPager).orElse(new Pager(CONF.maxItemsPerPage()) {
				public String getName() {
					return "default_pager";
				}
			});
		}
	}

	private void savePagerToCookie(HttpServletRequest req, HttpServletResponse res, Pager p) {
		try {
			HttpUtils.setRawCookie("users-filter", Utils.base64enc(ParaObjectUtils.getJsonWriterNoIdent().
					writeValueAsBytes(p)), req, res, false, "Strict", (int) TimeUnit.DAYS.toSeconds(365));
		} catch (JsonProcessingException ex) { }
	}
}
