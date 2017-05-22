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
import com.erudika.para.core.Address;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import static com.erudika.scoold.ScooldServer.questionslink;
import static com.erudika.scoold.ScooldServer.signinlink;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.utils.ScooldUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    public String get(HttpServletRequest req, Model model) {
		Pager itemcount = utils.getPager("page", req);
		List<Question> questionslist = pc.findQuery(Utils.type(Question.class), "*", itemcount);
		utils.fetchProfiles(questionslist);
		model.addAttribute("path", "questions.vm");
		model.addAttribute("title", utils.getLang(req).get("questions.title"));
		model.addAttribute("questionsSelected", "navbtn-hover");
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("questionslist", questionslist);
        return "base";
    }

	@GetMapping("/questions/tag/{tag}")
    public String getTagged(@PathVariable String tag, HttpServletRequest req, Model model) {
		Pager itemcount = utils.getPager("page", req);
		List<Question> questionslist = pc.findTagged(Utils.type(Question.class), new String[]{tag}, itemcount);
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
		StringBuilder sb = new StringBuilder();
		Question q = new Question();
		q.setTitle(like);
		q.setBody("");
		q.setTags(Arrays.asList(""));
		for (Post similarPost : utils.getSimilarPosts(q, new Pager(Config.getConfigInt("max_similar_posts", 7)))) {
			boolean hasAnswer = !StringUtils.isBlank(similarPost.getAnswerid());
			sb.append("<span class=\"lightborder phm").append(hasAnswer ? " light-green white-text" : "").append("\">");
			sb.append(similarPost.getVotes());
			sb.append("</span> <a href=\"").append(similarPost.getPostLink(false, false)).append("\">");
			sb.append(similarPost.getTitle()).append("</a><br>");
		}
		res.getWriter().print(sb.toString());
		res.setStatus(200);
	}

	@GetMapping("/questions/{filter}")
    public String getSorted(@PathVariable(required = false) String filter,
			@RequestParam(required = false) String sortby, HttpServletRequest req, Model model) {
		Pager itemcount = utils.getPager("page", req);
		List<Question> questionslist = Collections.emptyList();
		String type = Utils.type(Question.class);

		if ("activity".equals(sortby)) {
			itemcount.setSortby("updated");
			questionslist = pc.findQuery(type, "*", itemcount);
		} else if ("votes".equals(sortby)) {
			itemcount.setSortby("votes");
			questionslist = pc.findQuery(type, "*", itemcount);
		} else if ("unanswered".equals(sortby)) {
			itemcount.setSortby("timestamp");
			itemcount.setDesc(false);
			questionslist = pc.findQuery(type, "properties.answercount:0", itemcount);
		} else if ("filter".equals(sortby) && !StringUtils.isBlank(filter) && utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			if ("favtags".equals(filter) && authUser.hasFavtags()) {
				model.addAttribute("tagFilterOn", true);
				questionslist = pc.findTermInList(type, Config._TAGS,
						new ArrayList<String>(authUser.getFavtags()), itemcount);
			} else if (!StringUtils.isBlank(authUser.getLatlng())) {
				model.addAttribute("localFilterOn", true);
				String[] ll = authUser.getLatlng().split(",");
				if (ll.length == 2) {
					double lat = NumberUtils.toDouble(ll[0]);
					double lng = NumberUtils.toDouble(ll[1]);
					questionslist = pc.findNearby(type, "*", 25, lat, lng, itemcount);
				}
			}
		}
		utils.fetchProfiles(questionslist);
		model.addAttribute("path", "questions.vm");
		model.addAttribute("title", utils.getLang(req).get("questions.title"));
		model.addAttribute("questionsSelected", "navbtn-hover");
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("questionslist", questionslist);
        return "base";
    }

	@GetMapping("/questions/ask")
    public String ask(HttpServletRequest req, Model model) {
		if (!utils.isAuthenticated(req)) {
			return "redirect:" + signinlink + "?returnto=" + questionslink + "/ask";
		}
		model.addAttribute("path", "questions.vm");
		model.addAttribute("askSelected", "navbtn-hover");
		model.addAttribute("includeGMapsScripts", true);
		model.addAttribute("title", utils.getLang(req).get("questions.title") + " - " +
				utils.getLang(req).get("posts.ask"));
        return "base";
	}

	@PostMapping("/questions/ask")
    public String post(@RequestParam(required = false) String location, @RequestParam(required = false) String latlng,
			@RequestParam(required = false) String address, HttpServletRequest req, Model model) {
		if (utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			Question q = utils.populate(req, new Question(), "title", "body", "tags|,", "location");
			q.setCreatorid(authUser.getId());
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
				model.addAttribute("includeGMapsScripts", true);
				model.addAttribute("askSelected", "navbtn-hover");
				return "base";
			}
			return "redirect:" + q.getPostLink(false, false);
		}
		return "redirect:" + signinlink + "?returnto=" + questionslink + "/ask";
    }
}
