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
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import static com.erudika.scoold.ScooldServer.feedbacklink;
import com.erudika.scoold.core.Feedback;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.ScooldUtils;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
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
@RequestMapping("/feedback")
public class FeedbackController {

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public FeedbackController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping
	public String get(@RequestParam(required = false, defaultValue = Config._TIMESTAMP) String sortby,
			HttpServletRequest req, Model model) {
		Pager itemcount = utils.getPager("page", req);
		itemcount.setSortby(sortby);
		List<Post> feedbacklist = pc.findQuery(Utils.type(Feedback.class), "*", itemcount);
		model.addAttribute("path", "feedback.vm");
		model.addAttribute("title", utils.getLang(req).get("feedback.title"));
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("feedbacklist", feedbacklist);
		return "base";
	}

	@GetMapping("/{id}")
	public String getById(@PathVariable String id, HttpServletRequest req, Model model) {
		Feedback showPost = pc.read(id);
		if (showPost == null) {
			return "redirect:" + feedbacklink;
		}
		model.addAttribute("path", "feedback.vm");
		model.addAttribute("title", utils.getLang(req).get("feedback.title") + " - " + showPost.getTitle());
		return "base";
	}

	@GetMapping("/{sortby}")
	public String sorted(@PathVariable String sortby, HttpServletRequest req, Model model) {
		return get(sortby, req, model);
	}

	@GetMapping("/write")
	public String write(HttpServletRequest req, Model model) {
		if (!utils.isAuthenticated(req)) {
			return "redirect:" + feedbacklink;
		}
		model.addAttribute("write", true);
		model.addAttribute("path", "feedback.vm");
		model.addAttribute("title", utils.getLang(req).get("feedback.title") + " - " +
				utils.getLang(req).get("feedback.write"));
		return "base";
	}

	@GetMapping("/tag/{tag}")
	public String getTagged(@PathVariable String tag, HttpServletRequest req, Model model) {
		Pager itemcount = utils.getPager("page", req);
		List<Post> feedbacklist = pc.findTagged(Utils.type(Feedback.class), new String[]{tag}, itemcount);
		model.addAttribute("path", "feedback.vm");
		model.addAttribute("title", utils.getLang(req).get("feedback.title"));
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("feedbacklist", feedbacklist);
		return "base";
	}


	@PostMapping
    public String createAjax(@RequestParam String comment, @RequestParam String parentid,
			HttpServletRequest req, Model model) {
		model.addAttribute("path", "feedback.vm");
		if (utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			Post post = utils.populate(req, new Feedback(), "title", "body", "tags|,");
			Map<String, String> error = utils.validate(post);
			if (error.isEmpty()) {
				post.create();
				authUser.setLastseen(System.currentTimeMillis());
				return "redirect:" + feedbacklink;
			} else {
				model.addAttribute("error", error);
				return "base";
			}
		}
		return "redirect:" + feedbacklink;
	}

	@PostMapping("/{id}/delete")
    public String deleteAjax(@RequestParam String id, HttpServletRequest req) {
		if (utils.isAuthenticated(req)) {
			Feedback showPost = pc.read(id);
			if (showPost != null) {
				showPost.delete();
			}
		}
		return "redirect:" + feedbacklink;
	}
}
