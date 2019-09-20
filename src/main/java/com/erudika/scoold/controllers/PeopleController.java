/*
 * Copyright 2013-2019 Erudika. https://erudika.com
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

import com.erudika.para.core.ParaObject;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import static com.erudika.scoold.ScooldServer.PEOPLELINK;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.HttpUtils;
import com.erudika.scoold.utils.ScooldUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Entity;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
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

	@Inject
	public PeopleController(ScooldUtils utils) {
		this.utils = utils;
	}

	@GetMapping
	public String get(@RequestParam(required = false, defaultValue = Config._TIMESTAMP) String sortby,
			@RequestParam(required = false, defaultValue = "*") String q, HttpServletRequest req, Model model) {
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + PEOPLELINK;
		}
		Pager itemcount = utils.getPager("page", req);
		itemcount.setSortby(sortby);
		// [space query filter] + original query string
		String qs = utils.sanitizeQueryString(q, req);
		qs = qs.replaceAll("properties\\.space:", "properties.spaces:");

		List<Profile> userlist = utils.getParaClient().findQuery(Utils.type(Profile.class), qs, itemcount);
		model.addAttribute("path", "people.vm");
		model.addAttribute("title", utils.getLang(req).get("people.title"));
		model.addAttribute("peopleSelected", "navbtn-hover");
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("userlist", userlist);
		if (req.getParameter("bulkedit") != null) {
			List<ParaObject> spaces = utils.getParaClient().findQuery("scooldspace", "*", new Pager(Config.DEFAULT_LIMIT));
			model.addAttribute("spaces", spaces);
		}
		return "base";
	}

	@PostMapping("/bulk-edit")
	public String bulkEdit(@RequestParam(required = false) String[] selectedUsers,
			@RequestParam(required = false) String[] selectedSpaces, HttpServletRequest req) {
		Profile authUser = utils.getAuthUser(req);
		boolean isAdmin = utils.isAdmin(authUser);
		if (isAdmin && selectedUsers != null) {
			ArrayList<Map<String, Object>> toUpdate = new ArrayList<>();
			for (String selectedUser : selectedUsers) {
				if (!StringUtils.isBlank(selectedUser)) {
					Map<String, Object> profile = new HashMap<>();
					profile.put(Config._ID, selectedUser);
					if (selectedSpaces == null) {
						selectedSpaces = new String[]{};
					}
					profile.put("spaces", Arrays.asList(selectedSpaces));
					toUpdate.add(profile);
				}
			}
			if (!toUpdate.isEmpty()) {
				// partial batch update
				utils.getParaClient().invokePatch("_batch", Entity.json(toUpdate));
			}
		}
		return "redirect:" + PEOPLELINK + (isAdmin ? "?" + req.getQueryString() : "");
	}

	@GetMapping(path = {"/avatar/**", "/avatar"})
	public void avatar(HttpServletRequest req, HttpServletResponse res, Model model) throws IOException {
		String url = StringUtils.removeStart(StringUtils.removeStart(req.getRequestURI(), "/people/avatar"), "/");
		try (CloseableHttpResponse img = HttpUtils.getAvatar(Utils.urlDecode(url))) {
			if (img != null) {
				for (Header header : img.getAllHeaders()) {
					res.setHeader(header.getName(), header.getValue());
				}
				if (!res.containsHeader(HttpHeaders.CACHE_CONTROL)) {
					res.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=" + TimeUnit.HOURS.toSeconds(24));
				}
				IOUtils.copy(img.getEntity().getContent(), res.getOutputStream());
			} else {
				String anon = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
						+ "<svg xmlns=\"http://www.w3.org/2000/svg\" id=\"svg8\" width=\"756\" height=\"756\" "
						+ "version=\"1\" viewBox=\"0 0 200 200\">\n"
						+ "  <g id=\"layer1\" transform=\"translate(0 -97)\">\n"
						+ "    <rect id=\"rect1433\" width=\"282\" height=\"245\" x=\"-34\" y=\"79\" fill=\"#ececec\" rx=\"2\"/>\n"
						+ "  </g>\n"
						+ "  <g id=\"layer2\" fill=\"gray\">\n"
						+ "    <circle id=\"path1421\" cx=\"102\" cy=\"-70\" r=\"42\" transform=\"scale(1 -1)\"/>\n"
						+ "    <ellipse id=\"path1423\" cx=\"101\" cy=\"201\" rx=\"71\" ry=\"95\"/>\n"
						+ "  </g>\n"
						+ "</svg>";
				res.setContentType("image/svg+xml");
				res.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=" + TimeUnit.HOURS.toSeconds(24));
				res.setHeader(HttpHeaders.ETAG, Utils.md5(anon));
				IOUtils.copy(new ByteArrayInputStream(anon.getBytes()), res.getOutputStream());
			}
		}
	}
}
