/*
 * Copyright 2013-2020 Erudika. https://erudika.com
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
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import static com.erudika.scoold.ScooldServer.ANSWER_APPROVE_REWARD_AUTHOR;
import static com.erudika.scoold.ScooldServer.ANSWER_APPROVE_REWARD_VOTER;
import static com.erudika.scoold.ScooldServer.MAX_REPLIES_PER_POST;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import static com.erudika.scoold.ScooldServer.QUESTIONSLINK;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.UnapprovedQuestion;
import com.erudika.scoold.core.UnapprovedReply;
import java.util.HashSet;
import java.util.Optional;

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
			return "redirect:" + QUESTIONSLINK;
		}
		Profile authUser = utils.getAuthUser(req);
		if (!utils.canAccessSpace(authUser, showPost.getSpace())) {
			return "redirect:" + (utils.isDefaultSpacePublic() || utils.isAuthenticated(req) ?
					QUESTIONSLINK : SIGNINLINK + "?returnto=" + showPost.getPostLink(false, false));
		}

		if (showPost instanceof UnapprovedQuestion && !(utils.isMine(showPost, authUser) || utils.isMod(authUser))) {
			return "redirect:" + QUESTIONSLINK;
		}

		Pager itemcount = utils.getPager("page", req);
		itemcount.setSortby("newest".equals(sortby) ? "timestamp" : "votes");
		List<Reply> answerslist = getAllAnswers(authUser, showPost, itemcount);
		LinkedList<Post> allPosts = new LinkedList<Post>();
		allPosts.add(showPost);
		allPosts.addAll(answerslist);
		utils.fetchProfiles(allPosts);
		utils.getComments(allPosts);
		utils.updateViewCount(showPost, req, res);

		model.addAttribute("path", "question.vm");
		model.addAttribute("title", utils.getLang(req).get("questions.title") + " - " + showPost.getTitle());
		model.addAttribute("description", Utils.abbreviate(Utils.stripAndTrim(showPost.getBody(), " "), 195));
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("showPost", allPosts.removeFirst());
		model.addAttribute("answerslist", allPosts);
		model.addAttribute("similarquestions", utils.getSimilarPosts(showPost, new Pager(10)));
		model.addAttribute("maxCommentLength", Comment.MAX_COMMENT_LENGTH);
		model.addAttribute("includeGMapsScripts", utils.isNearMeFeatureEnabled());
		model.addAttribute("maxCommentLengthError", Utils.formatMessage(utils.getLang(req).get("maxlength"),
				Comment.MAX_COMMENT_LENGTH));
		return "base";
	}

	@PostMapping("/{id}/edit")
	public String edit(@PathVariable String id, @RequestParam(required = false) String title,
			@RequestParam(required = false) String body, @RequestParam(required = false) String tags,
			@RequestParam(required = false) String location, @RequestParam(required = false) String latlng,
			@RequestParam(required = false) String space, HttpServletRequest req) {

		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (!utils.canEdit(showPost, authUser) || showPost == null) {
			return "redirect:" + req.getRequestURI();
		}
		boolean isQuestion = !showPost.isReply();
		HashSet<String> addedTags = new HashSet<>();
		Post beforeUpdate = null;
		try {
			beforeUpdate = (Post) BeanUtils.cloneBean(showPost);
		} catch (Exception ex) {
			logger.error(null, ex);
		}

		if (!StringUtils.isBlank(title) && title.length() > 10) {
			showPost.setTitle(title);
		}
		// body can be blank
		showPost.setBody(body);
		showPost.setLocation(location);
		showPost.setAuthor(authUser);
		if (!StringUtils.isBlank(tags) && isQuestion) {
			showPost.updateTags(showPost.getTags(), Arrays.asList(StringUtils.split(tags, ",")));
			addedTags.addAll(showPost.getTags());
			addedTags.removeAll(new HashSet<>(Optional.ofNullable(showPost.getTags()).orElse(Collections.emptyList())));
		}
		if (isQuestion) {
			String validSpace = utils.getValidSpaceIdExcludingAll(authUser, space, req);
			if (utils.canAccessSpace(authUser, validSpace) && validSpace != null &&
					!utils.getSpaceId(validSpace).equals(utils.getSpaceId(showPost.getSpace()))) {
				showPost.setSpace(validSpace);
				changeSpaceForAllAnswers(showPost, validSpace);
			}
		}
		//note: update only happens if something has changed
		if (!showPost.equals(beforeUpdate)) {
			updatePost(showPost, authUser);
			updateLocation(showPost, authUser, location, latlng);
			utils.addBadgeOnceAndUpdate(authUser, Badge.EDITOR, true);
			utils.sendUpdatedFavTagsNotifications(showPost, new ArrayList<>(addedTags));
		}
		return "redirect:" + showPost.getPostLink(false, false);
	}

	@PostMapping({"/{id}", "/{id}/{title}"})
	public String replyAjax(@PathVariable String id, @PathVariable(required = false) String title,
			@RequestParam(required = false) Boolean emailme, HttpServletRequest req,
			HttpServletResponse res, Model model) throws IOException {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (showPost != null && emailme != null) {
			if (emailme) {
				showPost.addFollower(authUser.getUser());
			} else {
				showPost.removeFollower(authUser.getUser());
			}
			pc.update(showPost); // update without adding revisions
		} else if (showPost != null && !showPost.isClosed() && !showPost.isReply()) {
			//create new answer
			boolean needsApproval = utils.postNeedsApproval(authUser);
			Reply answer = utils.populate(req, needsApproval ? new UnapprovedReply() : new Reply(), "body");
			Map<String, String> error = utils.validate(answer);
			if (!error.containsKey("body") && !StringUtils.isBlank(answer.getBody())) {
				answer.setTitle(showPost.getTitle());
				answer.setCreatorid(authUser.getId());
				answer.setParentid(showPost.getId());
				answer.setSpace(showPost.getSpace());
				answer.create();

				showPost.setAnswercount(showPost.getAnswercount() + 1);
				showPost.setLastactivity(System.currentTimeMillis());
				if (showPost.getAnswercount() >= MAX_REPLIES_PER_POST) {
					showPost.setCloserid("0");
				}
				// update without adding revisions
				pc.update(showPost);
				utils.addBadgeAndUpdate(authUser, Badge.EUREKA, answer.getCreatorid().equals(showPost.getCreatorid()));
				answer.setAuthor(authUser);
				model.addAttribute("showPost", showPost);
				model.addAttribute("answerslist", Collections.singletonList(answer));
				// send email to the question author
				utils.sendReplyNotifications(showPost, answer);
			} else {
				model.addAttribute("error", error);
				model.addAttribute("path", "question.vm");
				res.setStatus(400);
			}
			return "reply";
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "reply";
		} else {
			return "redirect:" + QUESTIONSLINK + "/" + id;
		}
	}

	@PostMapping("/{id}/approve")
	public String modApprove(@PathVariable String id, HttpServletRequest req) {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (utils.isMod(authUser)) {
			if (showPost instanceof UnapprovedQuestion) {
				showPost.setType(Utils.type(Question.class));
				pc.create(showPost);
				utils.sendNewPostNotifications(showPost);
			} else if (showPost instanceof UnapprovedReply) {
				showPost.setType(Utils.type(Reply.class));
				pc.create(showPost);
			}
		}
		return "redirect:" + showPost.getPostLink(false, false);
	}

	@PostMapping("/{id}/approve/{answerid}")
	public String approve(@PathVariable String id, @PathVariable String answerid, HttpServletRequest req) {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (!utils.canEdit(showPost, authUser) || showPost == null) {
			return "redirect:" + req.getRequestURI();
		}
		if (utils.canEdit(showPost, authUser) && answerid != null && utils.isMine(showPost, authUser)) {
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
		if (!utils.canEdit(showPost, authUser) || showPost == null) {
			return "redirect:" + req.getRequestURI();
		}
		if (utils.isMod(authUser)) {
			if (showPost.isClosed()) {
				showPost.setCloserid("");
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
		if (!utils.canEdit(showPost, authUser) || showPost == null) {
			return "redirect:" + req.getRequestURI();
		}
		if (utils.canEdit(showPost, authUser)) {
			utils.addBadgeAndUpdate(authUser, Badge.BACKINTIME, true);
			showPost.restoreRevisionAndUpdate(revisionid);
		}
		return "redirect:" + showPost.getPostLink(false, false);
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable String id, HttpServletRequest req) {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (!utils.canEdit(showPost, authUser) || showPost == null) {
			return "redirect:" + req.getRequestURI();
		}
		if (!showPost.isReply()) {
			if ((utils.isMine(showPost, authUser) || utils.isMod(authUser))) {
				showPost.delete();
				return "redirect:" + QUESTIONSLINK + "?success=true&code=16";
			}
		} else if (showPost.isReply()) {
			if (utils.isMine(showPost, authUser) || utils.isMod(authUser)) {
				Post parent = pc.read(showPost.getParentid());
				parent.setAnswercount(parent.getAnswercount() - 1);
				parent.update();
				showPost.delete();
			}
		}
		return "redirect:" + showPost.getPostLink(false, false);
	}

	private void changeSpaceForAllAnswers(Post showPost, String space) {
		Pager pager = new Pager(1, "_docid", false, Config.MAX_ITEMS_PER_PAGE);
		List<Reply> answerslist;
		try {
			do {
				answerslist = pc.getChildren(showPost, Utils.type(Reply.class), pager);
				for (Reply reply : answerslist) {
					reply.setSpace(space);
				}
				if (!answerslist.isEmpty()) {
					pc.updateAll(answerslist);
					Thread.sleep(500);
				}
			} while (!answerslist.isEmpty());
		} catch (InterruptedException ex) {
			logger.error(null, ex);
			Thread.currentThread().interrupt();
		}
	}

	private List<Reply> getAllAnswers(Profile authUser, Post showPost, Pager itemcount) {
		List<Reply> answers = new ArrayList<>();
		Pager p = new Pager(itemcount.getPage(), itemcount.getLimit());
		if (utils.postsNeedApproval() && (utils.isMine(showPost, authUser) || utils.isMod(authUser))) {
			answers.addAll(showPost.getUnapprovedAnswers(p));
		}
		answers.addAll(showPost.getAnswers(itemcount));
		itemcount.setCount(itemcount.getCount() + p.getCount());
		return answers;
	}

	private void updatePost(Post showPost, Profile authUser) {
		showPost.setLasteditby(authUser.getId());
		showPost.setLastedited(System.currentTimeMillis());
		if (showPost.isQuestion()) {
			showPost.setLastactivity(System.currentTimeMillis());
			showPost.update();
		} else if (showPost.isReply()) {
			Post questionPost = pc.read(showPost.getParentid());
			if (questionPost != null) {
				showPost.setSpace(questionPost.getSpace());
				questionPost.setLastactivity(System.currentTimeMillis());
				pc.updateAll(Arrays.asList(showPost, questionPost));
			} else {
				showPost.update();
			}
		}
	}

	private void updateLocation(Post showPost, Profile authUser, String location, String latlng) {
		if (!showPost.isReply() && !StringUtils.isBlank(latlng)) {
			Address addr = new Address(showPost.getId() + Config.SEPARATOR + Utils.type(Address.class));
			addr.setAddress(location);
			addr.setCountry(location);
			addr.setLatlng(latlng);
			addr.setParentid(showPost.getId());
			addr.setCreatorid(authUser.getId());
			pc.create(addr);
		}
	}
}
