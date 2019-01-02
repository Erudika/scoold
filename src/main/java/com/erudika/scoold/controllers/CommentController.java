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

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.User;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.email.Emailer;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Utils;
import static com.erudika.scoold.ScooldServer.COMMENTATOR_IFHAS;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
import static com.erudika.scoold.ScooldServer.getServerURL;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import static com.erudika.scoold.core.Profile.Badge.COMMENTATOR;
import static com.erudika.scoold.core.Profile.Badge.DISCIPLINED;
import com.erudika.scoold.utils.ScooldUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

	private final ScooldUtils utils;
	private final ParaClient pc;
	private final Emailer emailer;

	@Inject
	public CommentController(ScooldUtils utils, Emailer emailer) {
		this.utils = utils;
		this.pc = utils.getParaClient();
		this.emailer = emailer;
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
			if (error.isEmpty()) {
				showComment.setComment(comment);
				showComment.setParentid(parentid);
				showComment.setAuthorName(authUser.getName());

				if (showComment.create() != null) {
					long commentCount = authUser.getComments();
					utils.addBadgeOnce(authUser, COMMENTATOR, commentCount >= COMMENTATOR_IFHAS);
					authUser.setComments(commentCount + 1);
					authUser.update();
					model.addAttribute("showComment", showComment);
					// send email to the author of parent post
					Post parentPost = pc.read(parentid);
					if (parentPost != null) {
						parentPost.addCommentId(showComment.getId());
						parentPost.update();
					}
					sendCommentNotification(parentPost, showComment, authUser);
				}
			}
		}
		return "comment";
	}

	private void sendCommentNotification(Post parentPost, Comment comment, Profile commentAuthor) {
		// send email notification to author of post except when the comment is by the same person
		if (parentPost != null && comment != null && !StringUtils.equals(parentPost.getCreatorid(), comment.getCreatorid())) {
			Profile authorProfile = pc.read(parentPost.getCreatorid());
			if (authorProfile != null && authorProfile.getCommentEmailsEnabled()) {
				User author = authorProfile.getUser();
				if (author != null) {
					Map<String, Object> model = new HashMap<String, Object>();
					String name = commentAuthor.getName();
					String body = Utils.markdownToHtml(Utils.abbreviate(comment.getComment(), 255));
					String pic = Utils.formatMessage("<img src='{0}' width='25'>", commentAuthor.getPicture());
					String postURL = getServerURL() + parentPost.getPostLink(false, false);
					model.put("logourl", Config.getConfigParam("small_logo_url", "https://scoold.com/logo.png"));
					model.put("heading", Utils.formatMessage("New comment on <a href='{0}'>{1}</a>", postURL, parentPost.getTitle()));
					model.put("body", Utils.formatMessage("<h2>{0} {1}:</h2><div class='panel'>{2}</div>", pic, name, body));
					emailer.sendEmail(Arrays.asList(author.getEmail()), name + " commented on your post",
							Utils.compileMustache(model, utils.loadEmailTemplate("notify")));
				}
			}
		}
	}
}
