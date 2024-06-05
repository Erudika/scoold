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
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import static com.erudika.scoold.core.Profile.Badge.COMMENTATOR;
import static com.erudika.scoold.core.Profile.Badge.DISCIPLINED;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.utils.ScooldUtils;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/comment")
public class CommentController {

	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public CommentController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping("/{id}")
	public String get(@PathVariable String id, HttpServletRequest req, Model model) {
		Comment showComment = pc.read(id);
		if (showComment == null || !ParaObjectUtils.typesMatch(showComment)) {
			return "redirect:" + HOMEPAGE;
		}
		model.addAttribute("path", "comment.vm");
		model.addAttribute("title", utils.getLang(req).get("comment.title"));
		model.addAttribute("showComment", showComment);
		return "base";
	}

	@GetMapping(params = {Config._PARENTID, "getcomments"})
	public String getAjax(@RequestParam String parentid, @RequestParam Boolean getcomments,
			@RequestParam(required = false, defaultValue = "1") Integer page, HttpServletRequest req, Model model) {
		Post parent = pc.read(parentid);
		if (parent != null) {
			parent.getItemcount().setPage(page);
			List<Comment> commentslist = pc.getChildren(parent, Utils.type(Comment.class), parent.getItemcount());
			parent.setComments(commentslist);
			model.addAttribute("showpost", parent);
			model.addAttribute("itemcount", parent.getItemcount());
		}
		return "comment";
	}

	@PostMapping("/{id}/delete")
	public void deleteAjax(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			Comment comment = pc.read(id);
			Profile authUser = utils.getAuthUser(req);
			boolean isMod = utils.isMod(authUser);
			if (comment != null && (comment.getCreatorid().equals(authUser.getId()) || isMod)) {
				// check parent and correct (for multi-parent-object pages)
				comment.delete();
				if (!isMod) {
					utils.addBadgeAndUpdate(authUser, DISCIPLINED, true);
				}
			}
		}
		res.setStatus(200);
	}

	@PostMapping
	public String createAjax(@RequestParam String comment, @RequestParam String parentid,
			HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (utils.canComment(authUser, req) && !StringUtils.isBlank(comment) && !StringUtils.isBlank(parentid)) {
			Comment showComment = utils.populate(req, new Comment(), "comment");
			showComment.setCreatorid(authUser.getId());
			Map<String, String> error = utils.validate(showComment);
			handleSpam(showComment, authUser, error, req);
			if (error.isEmpty()) {
				showComment.setComment(comment);
				showComment.setParentid(parentid);
				showComment.setAuthorName(authUser.getName());

				if (showComment.create() != null) {
					long commentCount = authUser.getComments();
					utils.addBadgeOnce(authUser, COMMENTATOR, commentCount >= CONF.commentatorIfHasRep());
					authUser.setComments(commentCount + 1);
					authUser.update();
					model.addAttribute("showComment", showComment);
					// send email to the author of parent post
					Post parentPost = pc.read(parentid);
					if (parentPost != null && parentPost.addCommentId(showComment.getId())) {
						pc.update(parentPost); // update without adding revisions
					}
					utils.sendCommentNotifications(parentPost, showComment, authUser, req);
				}
			}
		}
		return "comment";
	}

	private void handleSpam(Comment c, Profile authUser, Map<String, String> error, HttpServletRequest req) {
		boolean isSpam = utils.isSpam(c, authUser, req);
		if (isSpam && CONF.automaticSpamProtectionEnabled()) {
			error.put("comment", "spam");
		} else if (isSpam && !CONF.automaticSpamProtectionEnabled()) {
			Report rep = new Report();
			rep.setContent(Utils.abbreviate(Utils.markdownToHtml(c.getComment()), 2000));
			rep.setParentid(c.getId());
			rep.setCreatorid(authUser.getId());
			rep.setDescription("SPAM detected");
			rep.setSubType(Report.ReportType.SPAM);
			rep.setLink(CONF.serverUrl() + "/comment/" + c.getId());
			rep.setAuthorName(authUser.getName());
			rep.create();
		}
	}
}
