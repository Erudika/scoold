/*
 * Copyright 2013-2018 Erudika. https://erudika.com
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
import com.erudika.para.core.Address;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.utils.ScooldUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import static com.erudika.scoold.ScooldServer.QUESTIONSLINK;
import static com.erudika.scoold.ScooldServer.SPACE_COOKIE;
import static com.erudika.scoold.utils.HttpUtils.getCookieValue;
import static com.erudika.scoold.utils.HttpUtils.setRawCookie;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class QuestionsController {

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public QuestionsController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping({"/", "/questions"})
	public String get(@RequestParam(required = false) String sortby, HttpServletRequest req, Model model) {
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + QUESTIONSLINK;
		}
		getQuestions(sortby, null, req, model);
		model.addAttribute("path", "questions.vm");
		model.addAttribute("title", utils.getLang(req).get("questions.title"));
		model.addAttribute("questionsSelected", "navbtn-hover");
		return "base";
	}

	@GetMapping("/questions/tag/{tag}")
	public String getTagged(@PathVariable String tag, HttpServletRequest req, Model model) {
		Pager itemcount = utils.getPager("page", req);
		List<Question> questionslist = Collections.emptyList();
		String type = Utils.type(Question.class);
		String qf = utils.getSpaceFilteredQuery(req);
		if (!qf.isEmpty()) {
			if (qf.equals("*")) {
				questionslist = pc.findTagged(type, new String[]{tag}, itemcount);
			} else {
				questionslist = pc.findQuery(type, qf + " AND " + Config._TAGS + ":" + tag, itemcount);
			}
		}
		utils.fetchProfiles(questionslist);
		model.addAttribute("path", "questions.vm");
		model.addAttribute("title", utils.getLang(req).get("posts.tagged") + " - " + tag);
		model.addAttribute("questionsSelected", "navbtn-hover");
		model.addAttribute("tag", tag);
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("questionslist", questionslist);
		return "base";
	}

	@GetMapping("/questions/similar/{like}")
	public void getSimilarAjax(@PathVariable String like, HttpServletRequest req, HttpServletResponse res) throws IOException {
		Profile authUser = utils.getAuthUser(req);
		StringBuilder sb = new StringBuilder();
		Question q = new Question();
		q.setTitle(like);
		q.setBody("");
		q.setTags(Arrays.asList(""));
		for (Post similarPost : utils.getSimilarPosts(q, new Pager(Config.getConfigInt("max_similar_posts", 7)))) {
			if (utils.isMod(authUser) || utils.canAccessSpace(authUser, similarPost.getSpace())) {
				boolean hasAnswer = !StringUtils.isBlank(similarPost.getAnswerid());
				sb.append("<span class=\"lightborder phm").append(hasAnswer ? " light-green white-text" : "").append("\">");
				sb.append(similarPost.getVotes());
				sb.append("</span> <a href=\"").append(similarPost.getPostLink(false, false)).append("\">");
				sb.append(similarPost.getTitle()).append("</a><br>");
			}
		}
		res.setCharacterEncoding("UTF-8");
		res.getWriter().print(sb.toString());
		res.setStatus(200);
	}

	@GetMapping("/questions/{filter}")
	public String getSorted(@PathVariable(required = false) String filter,
			@RequestParam(required = false) String sortby, HttpServletRequest req, Model model) {
		getQuestions(sortby, filter, req, model);
		model.addAttribute("path", "questions.vm");
		model.addAttribute("title", utils.getLang(req).get("questions.title"));
		model.addAttribute("questionsSelected", "navbtn-hover");
		return "base";
	}

	@GetMapping("/questions/ask")
	public String ask(HttpServletRequest req, Model model) {
		if (!utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + QUESTIONSLINK + "/ask";
		}
		model.addAttribute("path", "questions.vm");
		model.addAttribute("askSelected", "navbtn-hover");
		model.addAttribute("includeGMapsScripts", utils.isNearMeFeatureEnabled());
		model.addAttribute("title", utils.getLang(req).get("questions.title") + " - "
				+ utils.getLang(req).get("posts.ask"));
		return "base";
	}

	@PostMapping("/questions/ask")
	public String post(@RequestParam(required = false) String location, @RequestParam(required = false) String latlng,
			@RequestParam(required = false) String address, HttpServletRequest req, Model model) {
		if (utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			String currentSpace = utils.getValidSpaceId(authUser, getCookieValue(req, SPACE_COOKIE));
			Question q = utils.populate(req, new Question(), "title", "body", "tags|,", "location");
			q.setCreatorid(authUser.getId());
			q.setSpace(currentSpace);
			Map<String, String> error = utils.validate(q);
			if (error.isEmpty()) {
				q.setLocation(location);
				String qid = q.create();
				if (!StringUtils.isBlank(latlng)) {
					Address addr = new Address();
					addr.setAddress(address);
					addr.setCountry(location);
					addr.setLatlng(latlng);
					addr.setParentid(qid);
					addr.setCreatorid(authUser.getId());
					pc.create(addr);
				}
				authUser.setLastseen(System.currentTimeMillis());
			} else {
				model.addAttribute("error", error);
				model.addAttribute("path", "questions.vm");
				model.addAttribute("includeGMapsScripts", utils.isNearMeFeatureEnabled());
				model.addAttribute("askSelected", "navbtn-hover");
				return "base";
			}
			return "redirect:" + q.getPostLink(false, false);
		}
		return "redirect:" + SIGNINLINK + "?returnto=" + QUESTIONSLINK + "/ask";
	}

	@PostMapping({"/questions/space/{space}", "/questions/space"})
	public String setSpace(@PathVariable(required = false) String space, HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = utils.getAuthUser(req);
		if (authUser != null) {
			if (StringUtils.isBlank(space) || pc.read(utils.getSpaceId(space)) == null) {
				if (utils.canAccessSpace(authUser, space)) {
					authUser.getSpaces().remove(space);
					authUser.update();
				}
				space = "";
			}
			setRawCookie(SPACE_COOKIE, space, req, res, false, StringUtils.isBlank(space) ? 0 : 365 * 24 * 60 * 60);
		}
		res.setStatus(200);
		return "base";
	}

	private List<Question> getQuestions(String sortby, String filter, HttpServletRequest req, Model model) {
		Pager itemcount = utils.getPager("page", req);
		List<Question> questionslist = Collections.emptyList();
		String type = Utils.type(Question.class);
		Profile authUser = utils.getAuthUser(req);
		String currentSpace = utils.getValidSpaceId(authUser, getCookieValue(req, SPACE_COOKIE));
		boolean spaceFiltered = !StringUtils.isBlank(currentSpace);
		String spaceFilter = "properties.space:\"" + currentSpace + "\"";
		String query = spaceFiltered ? spaceFilter : (utils.canAccessSpace(authUser, currentSpace) ? "*" : "");

		if ("activity".equals(sortby)) {
			itemcount.setSortby("updated");
		} else if ("votes".equals(sortby)) {
			itemcount.setSortby("votes");
		} else if ("unanswered".equals(sortby)) {
			itemcount.setSortby("timestamp");
			itemcount.setDesc(false);
			query = spaceFiltered ? spaceFilter + " AND properties.answercount:0" :
					(utils.canAccessSpace(authUser, currentSpace) ? "properties.answercount:0" : "");
		}

		if (!StringUtils.isBlank(filter) && authUser != null) {
			if ("favtags".equals(filter)) {
				if (spaceFiltered) {
					questionslist = pc.findQuery(type, getSpaceFilteredFavtagsQuery(currentSpace, authUser), itemcount);
				} else {
					questionslist = pc.findTermInList(type, Config._TAGS, authUser.getFavtags(), itemcount);
				}
			} else if ("local".equals(filter)) {
				String[] ll = authUser.getLatlng() == null ? new String[0] : authUser.getLatlng().split(",");
				if (ll.length == 2) {
					double lat = NumberUtils.toDouble(ll[0]);
					double lng = NumberUtils.toDouble(ll[1]);
					questionslist = pc.findNearby(type, query, 25, lat, lng, itemcount);
				}
			}
			model.addAttribute("localFilterOn", "local".equals(filter));
			model.addAttribute("tagFilterOn", "favtags".equals(filter));
			model.addAttribute("filter", "/" + Utils.stripAndTrim(filter));
		} else {
			questionslist = pc.findQuery(type, query, itemcount);
		}

		utils.fetchProfiles(questionslist);
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("questionslist", questionslist);
		return questionslist;
	}

	private String getSpaceFilteredFavtagsQuery(String currentSpace, Profile authUser) {
		if (utils.canAccessSpace(authUser, currentSpace)) {
			StringBuilder sb = new StringBuilder("properties.space:");
			sb.append(currentSpace).append(" AND (");
			for (int i = 0; i < authUser.getFavtags().size(); i++) {
				sb.append(Config._TAGS).append(":").append(authUser.getFavtags().get(i));
				if (i < authUser.getFavtags().size() - 1) {
					sb.append(" OR ");
				}
			}
			sb.append(")");
			return sb.toString();
		}
		return "";
	}
}
