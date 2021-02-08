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
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import static com.erudika.scoold.ScooldServer.MAX_REPLIES_PER_POST;

import com.erudika.scoold.core.Feedback;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.utils.ScooldUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import static com.erudika.scoold.ScooldServer.FEEDBACKLINK;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;

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
		if (!utils.isFeedbackEnabled()) {
			return "redirect:" + HOMEPAGE;
		}
		Pager itemcount = utils.getPager("page", req);
		itemcount.setSortby(sortby);
		List<Post> feedbacklist = pc.findQuery(Utils.type(Feedback.class), "*", itemcount);
		utils.fetchProfiles(feedbacklist);
		model.addAttribute("path", "feedback.vm");
		model.addAttribute("title", utils.getLang(req).get("feedback.title"));
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("feedbacklist", feedbacklist);
		return "base";
	}

	@GetMapping({"/{id}", "/{id}/{title}"})
	public String getById(@PathVariable String id, @PathVariable(required = false) String title,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		if (!utils.isFeedbackEnabled()) {
			return "redirect:" + HOMEPAGE;
		}
		Feedback showPost = pc.read(id);
		if (showPost == null) {
			return "redirect:" + FEEDBACKLINK;
		}
		Pager itemcount = utils.getPager("page", req);
		List<Reply> answerslist = showPost.getAnswers(itemcount);
		LinkedList<Post> allPosts = new LinkedList<Post>();
		allPosts.add(showPost);
		allPosts.addAll(answerslist);
		utils.fetchProfiles(allPosts);
		utils.getComments(allPosts);
		utils.updateViewCount(showPost, req, res);

		model.addAttribute("path", "feedback.vm");
		model.addAttribute("title", utils.getLang(req).get("feedback.title") + " - " + showPost.getTitle());
		model.addAttribute("description", Utils.abbreviate(Utils.stripAndTrim(showPost.getBody(), " "), 195));
		model.addAttribute("showPost", allPosts.removeFirst());
		model.addAttribute("answerslist", allPosts);
		model.addAttribute("itemcount", itemcount);
		return "base";
	}

	@GetMapping("/write")
	public String write(HttpServletRequest req, Model model) {
		if (!utils.isFeedbackEnabled()) {
			return "redirect:" + HOMEPAGE;
		}
		if (!utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + FEEDBACKLINK + "/write";
		}
		model.addAttribute("write", true);
		model.addAttribute("path", "feedback.vm");
		model.addAttribute("title", utils.getLang(req).get("feedback.title") + " - "
				+ utils.getLang(req).get("feedback.write"));
		return "base";
	}

	@GetMapping("/tag/{tag}")
	public String getTagged(@PathVariable String tag, HttpServletRequest req, Model model) {
		if (!utils.isFeedbackEnabled()) {
			return "redirect:" + HOMEPAGE;
		}
		Pager itemcount = utils.getPager("page", req);
		List<Post> feedbacklist = pc.findTagged(Utils.type(Feedback.class), new String[]{tag}, itemcount);
		model.addAttribute("path", "feedback.vm");
		model.addAttribute("title", utils.getLang(req).get("feedback.title"));
		model.addAttribute("tag", tag);
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("feedbacklist", feedbacklist);
		return "base";
	}

	@PostMapping
	public String createAjax(HttpServletRequest req, Model model) {
		if (!utils.isFeedbackEnabled()) {
			return "redirect:" + HOMEPAGE;
		}
		model.addAttribute("path", "feedback.vm");
		if (utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			Post post = utils.populate(req, new Feedback(), "title", "body", "tags|,");
			Map<String, String> error = utils.validate(post);
			if (error.isEmpty()) {
				post.setCreatorid(authUser.getId());
				post.create();
				authUser.setLastseen(System.currentTimeMillis());
				return "redirect:" + FEEDBACKLINK;
			} else {
				model.addAttribute("error", error);
				model.addAttribute("write", true);
				return "base";
			}
		}
		return "redirect:" + FEEDBACKLINK;
	}

	@PostMapping({"/{id}", "/{id}/{title}"})
	public String replyAjax(@PathVariable String id, @PathVariable(required = false) String title,
			HttpServletRequest req, HttpServletResponse res, Model model) throws IOException {
		if (!utils.isFeedbackEnabled()) {
			return "redirect:" + HOMEPAGE;
		}
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (showPost != null && !showPost.isClosed() && !showPost.isReply()) {
			//create new answer
			Reply answer = utils.populate(req, new Reply(), "body");
			Map<String, String> error = utils.validate(answer);
			if (!error.containsKey("body")) {
				answer.setTitle(showPost.getTitle());
				answer.setCreatorid(authUser.getId());
				answer.setParentid(showPost.getId());
				answer.create();

				showPost.setAnswercount(showPost.getAnswercount() + 1);
				if (showPost.getAnswercount() >= MAX_REPLIES_PER_POST) {
					showPost.setCloserid("0");
				}
				// update without adding revisions
				pc.update(showPost);
				utils.addBadgeAndUpdate(authUser, Profile.Badge.EUREKA, answer.getCreatorid().equals(showPost.getCreatorid()));
				answer.setAuthor(authUser);
				model.addAttribute("showPost", showPost);
				model.addAttribute("answerslist", Collections.singletonList(answer));
			} else {
				model.addAttribute("error", error);
				model.addAttribute("path", "feedback.vm");
				res.setStatus(400);
			}
			return "reply";
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "reply";
		} else {
			return "redirect:" + FEEDBACKLINK + "/" + id;
		}
	}

	@PostMapping("/{id}/delete")
	public String deleteAjax(@PathVariable String id, HttpServletRequest req) {
		if (!utils.isFeedbackEnabled()) {
			return "redirect:" + HOMEPAGE;
		}
		if (utils.isAuthenticated(req)) {
			Feedback showPost = pc.read(id);
			if (showPost != null) {
				showPost.delete();
			}
		}
		return "redirect:" + FEEDBACKLINK;
	}
}
