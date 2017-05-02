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
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import static com.erudika.scoold.ScooldServer.ANSWER_APPROVE_REWARD_AUTHOR;
import static com.erudika.scoold.ScooldServer.ANSWER_APPROVE_REWARD_VOTER;
import static com.erudika.scoold.ScooldServer.MAX_REPLIES_PER_POST;
import static com.erudika.scoold.ScooldServer.questionslink;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Profile.Badge;
import com.erudika.scoold.core.Reply;
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
import javax.ws.rs.ForbiddenException;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/question")
public class QuestionController {

	public static final Logger logger = LoggerFactory.getLogger(QuestionController.class);

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public QuestionController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping({"/{id}", "/{id}/{title}"})
    public String get(@PathVariable String id, @PathVariable(required = false) String title,
			@RequestParam(required = false) String sortby, HttpServletRequest req, HttpServletResponse res, Model model) {
		Post showPost = pc.read(id);
		if (showPost == null || !ParaObjectUtils.typesMatch(showPost)) {
			return "redirect:" + questionslink;
		}

		Pager itemcount = utils.getPager("page", req);
		itemcount.setSortby("newest".equals(sortby) ? "" : "votes");
		List<Reply> answerslist = showPost.getAnswers(itemcount);
		ArrayList<Post> list = new ArrayList<Post>(answerslist);
		list.add(showPost);
		utils.fetchProfiles(list);
		//get the comments for each answer
		for (Post post : list) {
			List<Comment> commentz = pc.getChildren(post, Utils.type(Comment.class), post.getItemcount());
			post.setComments(commentz);
		}
		updateViewCount(showPost, req, res);

		List<Post> similarquestions = Collections.emptyList();
		if (!showPost.isReply() && !utils.isAjaxRequest(req)) {
			String likeTxt = (showPost.getTitle() + " " + showPost.getBody() + " " + showPost.getTags()).trim();
			if (!StringUtils.isBlank(likeTxt)) {
				similarquestions = pc.findSimilar(showPost.getType(), showPost.getId(),
						new String[]{"title", "body", "tags"}, likeTxt, new Pager(10));
			}
		}

		model.addAttribute("path", "question.vm");
		model.addAttribute("title", utils.getLang(req).get("questions.title") + " - " + showPost.getTitle());
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("isMine", isMine(showPost, utils.getAuthUser(req)));
		model.addAttribute("canEdit", canEdit(showPost, utils.getAuthUser(req)));
		model.addAttribute("showPost", showPost);
		model.addAttribute("answerslist", answerslist);
		model.addAttribute("similarquestions", similarquestions);
        return "base";
    }

	@PostMapping("/{id}/edit")
    public String edit(@PathVariable String id, @RequestParam(required = false) String title,
			@RequestParam(required = false) String body, @RequestParam(required = false) String tags,
			HttpServletRequest req) {

		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (!canEdit(showPost, authUser) || showPost == null) {
			return "redirect:" + req.getRequestURI();
		}
		Post beforeUpdate = null;
		try {
			beforeUpdate = (Post) BeanUtils.cloneBean(showPost);
		} catch (Exception ex) {
			logger.error(null, ex);
		}

		if (!StringUtils.isBlank(title) && title.length() > 10) {
			showPost.setTitle(title);
		}
		if (!StringUtils.isBlank(body)) {
			showPost.setBody(body);
		}
		if (!StringUtils.isBlank(tags) && showPost.isQuestion()) {
			showPost.setTags(Arrays.asList(StringUtils.split(tags, ",")));
		}

		showPost.setLasteditby(authUser.getId());
		//note: update only happens if something has changed
		if (!showPost.equals(beforeUpdate)) {
			showPost.update();
			utils.addBadgeOnceAndUpdate(authUser, Badge.EDITOR, true);
		}
        return "redirect:" + showPost.getPostLink(false, false);
    }

	@PostMapping({"/{id}", "/{id}/{title}"})
    public String replyAjax(@PathVariable String id, @PathVariable(required = false) String title,
			HttpServletRequest req, Model model) throws IOException {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		// add new answer
		String errorMsg = "";
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
				utils.addBadgeAndUpdate(authUser, Badge.EUREKA, answer.getCreatorid().equals(showPost.getCreatorid()));
				answer.setAuthor(authUser);
				model.addAttribute("showPost", showPost);
				model.addAttribute("answerslist", Collections.singletonList(answer));
				return "reply";
			}
			errorMsg = error.get("body");
		}
		throw new ForbiddenException(errorMsg);
    }

	@PostMapping("/{id}/approve/{answerid}")
    public String approve(@PathVariable String id, @PathVariable String answerid, HttpServletRequest req) {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (!canEdit(showPost, authUser) || showPost == null) {
			return "redirect:" + req.getRequestURI();
		}
		if (canEdit(showPost, authUser) && answerid != null && isMine(showPost, authUser)) {
			Reply answer = (Reply) pc.read(answerid);

			if (answer != null && answer.isReply()) {
				Profile author = pc.read(answer.getCreatorid());
				if (author != null && utils.isAuthenticated(req)) {
					boolean same = author.equals(authUser);

					if (answerid.equals(showPost.getAnswerid())) {
						// Answer approved award - UNDO
						showPost.setAnswerid("");
						if (!same) {
							author.removeRep(ANSWER_APPROVE_REWARD_AUTHOR);
							authUser.removeRep(ANSWER_APPROVE_REWARD_VOTER);
							pc.updateAll(Arrays.asList(author, authUser));
						}
					} else {
						// Answer approved award - GIVE
						showPost.setAnswerid(answerid);
						if (!same) {
							author.addRep(ANSWER_APPROVE_REWARD_AUTHOR);
							authUser.addRep(ANSWER_APPROVE_REWARD_VOTER);
							utils.addBadgeOnce(authUser, Badge.NOOB, true);
							pc.updateAll(Arrays.asList(author, authUser));
						}
					}
					showPost.update();
				}
			}
		}
        return "redirect:" + showPost.getPostLink(false, false);
    }

	@PostMapping("/{id}/close")
    public String close(@PathVariable String id, HttpServletRequest req) {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (!canEdit(showPost, authUser) || showPost == null) {
			return "redirect:" + req.getRequestURI();
		}
		if (utils.isMod(authUser)) {
			if (showPost.isClosed()) {
				showPost.setCloserid(null);
			} else {
				showPost.setCloserid(authUser.getId());
			}
			showPost.update();
		}
        return "redirect:" + showPost.getPostLink(false, false);
    }

	@PostMapping("/{id}/restore/{revisionid}")
    public String restore(@PathVariable String id, @PathVariable String revisionid, HttpServletRequest req) {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (!canEdit(showPost, authUser) || showPost == null) {
			return "redirect:" + req.getRequestURI();
		}
		if (canEdit(showPost, authUser)) {
			utils.addBadgeAndUpdate(authUser, Badge.BACKINTIME, true);
			showPost.restoreRevisionAndUpdate(revisionid);
		}
        return "redirect:" + showPost.getPostLink(false, false);
    }

	@PostMapping("/{id}/delete")
    public String delete(@PathVariable String id, HttpServletRequest req) {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (!canEdit(showPost, authUser) || showPost == null) {
			return "redirect:" + req.getRequestURI();
		}
		if (!showPost.isReply()) {
			if ((isMine(showPost, authUser) || utils.isMod(authUser))) {
				showPost.delete();
				return "redirect:" + questionslink + "?success=true&code=16";
			}
		} else if (showPost.isReply()) {
			if (isMine(showPost, authUser) || utils.isMod(authUser)) {
				Post parent = pc.read(showPost.getParentid());
				parent.setAnswercount(parent.getAnswercount() - 1);
				parent.update();
				showPost.delete();
			}
		}
        return "redirect:" + showPost.getPostLink(false, false);
    }

	private boolean isMine(Post showPost, Profile authUser) {
		// author can edit, mods can edit & ppl with rep > 100 can edit
		return showPost != null && authUser != null ? authUser.getId().equals(showPost.getCreatorid()) : false;
	}

	private boolean canEdit(Post showPost, Profile authUser) {
		return authUser != null ? (authUser.hasBadge(Badge.TEACHER) || utils.isMod(authUser) ||
				isMine(showPost, authUser)) : false;
	}

	private void updateViewCount(Post showPost, HttpServletRequest req, HttpServletResponse res) {
		//do not count views from author
		if (showPost != null && !isMine(showPost, utils.getAuthUser(req))) {
			String postviews = Utils.getStateParam("postviews", req);
			if (!StringUtils.contains(postviews, showPost.getId())) {
				long views = (showPost.getViewcount() == null) ? 0 : showPost.getViewcount();
				showPost.setViewcount(views + 1); //increment count
				Utils.setStateParam("postviews", postviews + "," + showPost.getId(), req, res);
				pc.update(showPost);
			}
		}
	}
}
