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
import com.erudika.para.core.Address;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.User;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.QUESTIONSLINK;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Profile.Badge;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.Revision;
import com.erudika.scoold.core.UnapprovedQuestion;
import com.erudika.scoold.core.UnapprovedReply;
import com.erudika.scoold.utils.ScooldUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Produces;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
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
	private static final ScooldConfig CONF = ScooldUtils.getConfig();

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public QuestionController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping({"/{id}", "/{id}/{title}", "/{id}/{title}/*"})
	public String get(@PathVariable String id, @PathVariable(required = false) String title,
			@RequestParam(required = false) String sortby, HttpServletRequest req, HttpServletResponse res, Model model) {

		Post showPost = pc.read(id);
		if (showPost == null || !ParaObjectUtils.typesMatch(showPost)) {
			return "redirect:" + QUESTIONSLINK;
		}
		Profile authUser = utils.getAuthUser(req);
		if (!utils.canAccessSpace(authUser, showPost.getSpace())) {
			return "redirect:" + (utils.isDefaultSpacePublic() || utils.isAuthenticated(req) ?
					QUESTIONSLINK : SIGNINLINK + "?returnto=" + req.getRequestURI());
		} else if (!utils.isDefaultSpace(showPost.getSpace()) && pc.read(utils.getSpaceId(showPost.getSpace())) == null) {
			showPost.setSpace(Post.DEFAULT_SPACE);
			pc.update(showPost);
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
		model.addAttribute("title", showPost.getTitle());
		model.addAttribute("description", Utils.abbreviate(Utils.stripAndTrim(showPost.getBody(), " "), 195));
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("showPost", allPosts.removeFirst());
		model.addAttribute("answerslist", allPosts);
		model.addAttribute("similarquestions", utils.getSimilarPosts(showPost, new Pager(10)));
		model.addAttribute("maxCommentLength", CONF.maxCommentLength());
		model.addAttribute("includeGMapsScripts", utils.isNearMeFeatureEnabled());
		model.addAttribute("maxCommentLengthError", Utils.formatMessage(utils.getLang(req).get("maxlength"),
				CONF.maxCommentLength()));
		return "base";
	}

	@PostMapping("/{id}/edit")
	public String edit(@PathVariable String id, @RequestParam(required = false) String title,
			@RequestParam(required = false) String body, @RequestParam(required = false) String tags,
			@RequestParam(required = false) String location, @RequestParam(required = false) String latlng,
			@RequestParam(required = false) String space, HttpServletRequest req, HttpServletResponse res, Model model) {

		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (!utils.canEdit(showPost, authUser) || showPost == null) {
			model.addAttribute("post", showPost);
			if (utils.isAjaxRequest(req)) {
				res.setStatus(400);
				return "blank";
			} else {
				return "redirect:" + req.getRequestURI(); // + "/edit-post-12345" ?
			}
		}
		boolean isQuestion = !showPost.isReply();
		Set<String> addedTags = new HashSet<>();
		Post beforeUpdate = null;
		try {
			beforeUpdate = (Post) BeanUtils.cloneBean(showPost);
		} catch (Exception ex) {
			logger.error(null, ex);
		}

		// body can be blank
		showPost.setBody(body);
		showPost.setLocation(location);
		showPost.setAuthor(authUser);
		if (isQuestion) {
			if (StringUtils.length(title) > 2) {
				showPost.setTitle(title);
			}
			addedTags = updateTags(showPost, tags);
			updateSpaces(showPost, authUser, space, req);
		}
		//note: update only happens if something has changed
		if (!showPost.equals(beforeUpdate)) {
			// create revision manually
			if (showPost.hasUpdatedContent(beforeUpdate)) {
				Revision.createRevisionFromPost(showPost, false);
			}
			updatePost(showPost, authUser);
			updateLocation(showPost, authUser, location, latlng);
			utils.addBadgeOnceAndUpdate(authUser, Badge.EDITOR, true);
			utils.sendUpdatedFavTagsNotifications(showPost, new ArrayList<>(addedTags), req);
		}
		model.addAttribute("post", showPost);
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			res.setContentType("application/json");
			try {
				res.getWriter().println("{\"url\":\"" + getPostLink(showPost) + "\"}");
			} catch (IOException ex) { }
			return "blank";
		} else {
			return "redirect:" + showPost.getPostLink(false, false);
		}
	}

	@PostMapping({"/{id}", "/{id}/{title}", "/{id}/{title}/write"})
	public String reply(@PathVariable String id, @PathVariable(required = false) String title,
			@RequestParam(required = false) Boolean emailme, HttpServletRequest req,
			HttpServletResponse res, Model model) {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (authUser == null || showPost == null) {
			if (utils.isAjaxRequest(req)) {
				res.setStatus(400);
				return "base";
			} else {
				return "redirect:" + QUESTIONSLINK + "/" + id;
			}
		}
		if (emailme != null) {
			followPost(showPost, authUser, emailme);
		} else if (!showPost.isClosed() && !showPost.isReply()) {
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
				if (showPost.getAnswercount() >= CONF.maxRepliesPerPost()) {
					showPost.setCloserid("0");
				}
				// update without adding revisions
				pc.update(showPost);
				utils.addBadgeAndUpdate(authUser, Badge.EUREKA, answer.getCreatorid().equals(showPost.getCreatorid()));
				answer.setAuthor(authUser);
				model.addAttribute("showPost", showPost);
				model.addAttribute("answerslist", Collections.singletonList(answer));
				// send email to the question author
				utils.sendReplyNotifications(showPost, answer, req);
				model.addAttribute("newpost", getNewAnswerPayload(answer));
			} else {
				model.addAttribute("error", error);
				model.addAttribute("path", "question.vm");
				res.setStatus(400);
			}
			return "reply";
		} else {
			model.addAttribute("error", "Parent post doesn't exist or cannot have children.");
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
				utils.sendNewPostNotifications(showPost, req);
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
		if (utils.canEdit(showPost, authUser) && answerid != null &&
				(utils.isMine(showPost, authUser) || utils.isAdmin(authUser))) {
			Reply answer = (Reply) pc.read(answerid);

			if (answer != null && answer.isReply()) {
				Profile author = pc.read(answer.getCreatorid());
				if (author != null && utils.isAuthenticated(req)) {
					boolean samePerson = author.equals(authUser);

					if (answerid.equals(showPost.getAnswerid())) {
						// Answer approved award - UNDO
						showPost.setAnswerid("");
						if (!samePerson) {
							author.removeRep(CONF.answerApprovedRewardAuthor());
							authUser.removeRep(CONF.answerApprovedRewardVoter());
							pc.updateAll(Arrays.asList(author, authUser));
						}
					} else {
						// Answer approved award - GIVE
						showPost.setAnswerid(answerid);
						if (!samePerson) {
							author.addRep(CONF.answerApprovedRewardAuthor());
							authUser.addRep(CONF.answerApprovedRewardVoter());
							utils.addBadgeOnce(authUser, Badge.NOOB, true);
							pc.updateAll(Arrays.asList(author, authUser));
						}
						utils.triggerHookEvent("answer.accept",
								getAcceptedAnswerPayload(showPost, answer, authUser, author));
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
		if (showPost == null) {
			return "redirect:" + req.getRequestURI();
		}
		if (utils.isMod(authUser) && !showPost.isReply()) {
			if (showPost.isClosed()) {
				showPost.setCloserid("");
			} else {
				showPost.setCloserid(authUser.getId());
				utils.triggerHookEvent("question.close", showPost);
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
	public String delete(@PathVariable String id, HttpServletRequest req, Model model) {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (!utils.canEdit(showPost, authUser) || showPost == null) {
			model.addAttribute("post", showPost);
			return "redirect:" + req.getRequestURI();
		}
		if (!showPost.isReply()) {
			if ((utils.isMine(showPost, authUser) && utils.canDelete(showPost, authUser)) || utils.isMod(authUser)) {
				showPost.delete();
				model.addAttribute("deleted", true);
				return "redirect:" + QUESTIONSLINK + "?success=true&code=16";
			}
		} else if (showPost.isReply()) {
			Post parent = pc.read(showPost.getParentid());
			if ((utils.isMine(showPost, authUser) && utils.canDelete(showPost, authUser, parent.getAnswerid())) ||
					utils.isMod(authUser)) {
				parent.setAnswercount(parent.getAnswercount() - 1);
				parent.setAnswerid(showPost.getId().equals(parent.getAnswerid()) ? "" : parent.getAnswerid());
				parent.update();
				showPost.delete();
				model.addAttribute("deleted", true);
			}
		}
		return "redirect:" + showPost.getPostLink(false, false);
	}

	@PostMapping("/{id}/deprecate")
	public String deprecate(@PathVariable String id, HttpServletRequest req) {
		Post showPost = pc.read(id);
		Profile authUser = utils.getAuthUser(req);
		if (!utils.canEdit(showPost, authUser) || showPost == null) {
			return "redirect:" + req.getRequestURI();
		}
		if (utils.canEdit(showPost, authUser)) {
			showPost.setDeprecated(!showPost.getDeprecated());
			showPost.update();
		}
		return "redirect:" + showPost.getPostLink(false, false);
	}

	@PostMapping("/{id}/merge-into")
	public String merge(@PathVariable String id, @RequestParam String id2, HttpServletRequest req) {
		Post showPost = pc.read(id);
		Post targetPost = pc.read(id2);
		Profile authUser = utils.getAuthUser(req);
		if (!(utils.canEdit(showPost, authUser) && utils.canEdit(targetPost, authUser)) || showPost == null ||
				targetPost == null || showPost.isReply() || targetPost.isReply() || showPost.equals(targetPost)) {
			return "redirect:" + req.getRequestURI();
		}
		if (utils.canEdit(showPost, authUser) && utils.canEdit(targetPost, authUser)) {
			if (CONF.mergeQuestionBodies()) {
				targetPost.setBody(targetPost.getBody() + "\n\n" + showPost.getBody());
			}
			targetPost.setAnswercount(targetPost.getAnswercount() + showPost.getAnswercount());
			targetPost.setViewcount(targetPost.getViewcount() + showPost.getViewcount());
			if (showPost.hasFollowers()) {
				for (Map.Entry<String, String> entry : showPost.getFollowers().entrySet()) {
					User u = new User(entry.getKey());
					u.setEmail(entry.getValue());
					targetPost.addFollower(u);
				}
			}
			Pager pager = new Pager(1, "_docid", false, CONF.maxItemsPerPage());
			List<Reply> answers;
			do {
				answers = pc.getChildren(showPost, Utils.type(Reply.class), pager);
				for (Reply answer : answers) {
					answer.setParentid(targetPost.getId());
					answer.setTitle(targetPost.getTitle());
				}
				pc.createAll(answers); // overwrite
			} while (!answers.isEmpty());
			targetPost.update();
			showPost.delete();
		}
		return "redirect:" + targetPost.getPostLink(false, false);
	}

	@GetMapping("/find/{q}")
	@Produces("application/json")
	public ResponseEntity<List<ParaObject>> findAjax(@PathVariable String q, HttpServletRequest req, HttpServletResponse res) {
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			res.setStatus(401);
			return ResponseEntity.status(401).body(Collections.emptyList());
		}
		String qs = utils.sanitizeQueryString(q + "*", req);
		Pager pager = new Pager(1, "votes", true, 10);
		return ResponseEntity.ok(pc.findQuery(Utils.type(Question.class), qs, pager));
	}

	private void changeSpaceForAllAnswers(Post showPost, String space) {
		if (showPost == null || showPost.isReply()) {
			return;
		}
		Pager pager = new Pager(1, "_docid", false, CONF.maxItemsPerPage());
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
		if (showPost == null || showPost.isReply()) {
			return Collections.emptyList();
		}
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
				// create revision here
				showPost.update();
			}
		}
	}

	private void updateLocation(Post showPost, Profile authUser, String location, String latlng) {
		if (!showPost.isReply() && !StringUtils.isBlank(latlng)) {
			Address addr = new Address(showPost.getId() + Para.getConfig().separator() + Utils.type(Address.class));
			addr.setAddress(location);
			addr.setCountry(location);
			addr.setLatlng(latlng);
			addr.setParentid(showPost.getId());
			addr.setCreatorid(authUser.getId());
			pc.create(addr);
		}
	}

	private Set<String> updateTags(Post showPost, String tags) {
		if (!StringUtils.isBlank(tags)) {
			List<String> newTags = Arrays.asList(StringUtils.split(tags, ","));
			HashSet<String> addedTags = new HashSet<>(newTags);
			addedTags.removeAll(new HashSet<>(Optional.ofNullable(showPost.getTags()).orElse(Collections.emptyList())));
			showPost.updateTags(showPost.getTags(), newTags);
			return addedTags;
		}
		return Collections.emptySet();
	}

	private void updateSpaces(Post showPost, Profile authUser, String space, HttpServletRequest req) {
		String validSpace = utils.getValidSpaceIdExcludingAll(authUser,
				Optional.ofNullable(space).orElse(showPost.getSpace()), req);
		if (utils.canAccessSpace(authUser, validSpace) && validSpace != null
				&& !utils.getSpaceId(validSpace).equals(utils.getSpaceId(showPost.getSpace()))) {
			showPost.setSpace(validSpace);
			changeSpaceForAllAnswers(showPost, validSpace);
		}
	}

	private Map<String, Object> getNewAnswerPayload(Reply answer) {
		Map<String, Object> payload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(answer, false));
		payload.put("author", answer == null ? null : answer.getAuthor());
		utils.triggerHookEvent("answer.create", payload);
		return payload;
	}

	private Object getAcceptedAnswerPayload(Post showPost, Reply answer, Profile authUser, Profile author) {
		Map<String, Object> payload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(showPost, false));
		Map<String, Object> answerPayload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(answer, false));
		answerPayload.put("author", author);
		payload.put("children", answerPayload);
		payload.put("authUser", authUser);
		return payload;
	}

	private void followPost(Post showPost, Profile authUser, Boolean emailme) {
		if (emailme) {
			showPost.addFollower(authUser.getUser());
		} else {
			showPost.removeFollower(authUser.getUser());
		}
		pc.update(showPost); // update without adding revisions
	}

	private String getPostLink(Post showPost) {
		return showPost.getPostLink(false, false) + (showPost.isQuestion() ? "" :  "#post-" + showPost.getId());
	}
}
