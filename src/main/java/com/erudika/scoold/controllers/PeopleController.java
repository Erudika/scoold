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

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Utils;
import static com.erudika.scoold.ScooldServer.PEOPLELINK;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.HttpUtils;
import com.erudika.scoold.utils.ScooldUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
			return "redirect:" + PEOPLELINK + "?bulk-edit=true";
		}
		Profile authUser = utils.getAuthUser(req);
		Pager itemcount = utils.getPager("page", req);
		itemcount.setSortby(sortby);
		// [space query filter] + original query string
		String qs = utils.sanitizeQueryString(q, req);
		if (req.getParameter("bulkedit") != null && utils.isAdmin(authUser)) {
			qs = q;
		} else {
			qs = qs.replaceAll("properties\\.space:", "properties.spaces:");
		}

		if (!qs.endsWith("*")) {
			qs += " OR properties.groups:(admins)"; // admins are members of every space and always visible
		}

		List<Profile> userlist = pc.findQuery(Utils.type(Profile.class), qs, itemcount);
		model.addAttribute("path", "people.vm");
		model.addAttribute("title", utils.getLang(req).get("people.title"));
		model.addAttribute("peopleSelected", "navbtn-hover");
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("userlist", userlist);
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
			Pager pager = new Pager(1, "_docid", false, Config.MAX_ITEMS_PER_PAGE);
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
}
