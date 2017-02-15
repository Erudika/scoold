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

import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Reply;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class QuestionController {

	public String title;
	public boolean canEdit;
	public boolean isMine;
	public Post showPost;
	public List<Reply> answerslist;
	public List<Post> similarquestions;
	public String markdownHtml;
	public Map<String, String> error = Collections.emptyMap();

//	public QuestionController() {
//		title = lang.get("questions.title");
//		canEdit = false;
//		String id = param("editpostid") ? getParamValue("editpostid") : getParamValue("id");
//		showPost = pc.read(id);
//
//		if (showPost != null && ParaObjectUtils.typesMatch(showPost)) {
//			if (showPost.getTitle() != null) {
//				title = title + " - " + showPost.getTitle();
//			}
//
//			// author can edit, mods can edit & ppl with rep > 100 can edit
//			isMine = (authenticated) ? authUser.getId().equals(showPost.getCreatorid()) : false;
//			canEdit = (authenticated) ?	(authUser.hasBadge(Badge.TEACHER) || isMod || isMine) : false;
//			if (!isMine && !isMod && showPost.isFeedback()) {
//				canEdit = false;
//			}
//			itemcount.setSortby("newest".equals(getParamValue("sortby")) ? "" : "votes");
//		}
//	}
//
//	public void onGet() {
//		if (showPost == null) {
//			setRedirect(questionslink + (param("delete") ? "?success=true&code=16" : ""));
//			return;
//		}
//		answerslist = showPost.getAnswers(itemcount);
//		ArrayList<Post> list = new ArrayList<Post>(answerslist);
//		list.add(showPost);
//		fetchProfiles(list);
//		//get the comments for each answer
//		Post.readAllCommentsForPosts(list, MAX_ITEMS_PER_PAGE);
//		if (list != null) {
//			for (P post : list) {
//				List<com.erudika.scoold.core.Comment> commentz = pc.getChildren(post, Utils.type(com.erudika.scoold.core.Comment.class), post.getItemcount());
//				post.setComments(commentz);
//			}
//		}
//		updateViewCount();
//
//		if (!showPost.isReply()) {
//			if (!isAjaxRequest()) {
//				String likeTxt = (showPost.getTitle() + " " + showPost.getBody() + " " + showPost.getTags()).trim();
//				if (!StringUtils.isBlank(likeTxt)) {
//					similarquestions = pc.findSimilar(showPost.getType(), showPost.getId(),
//							new String[]{"title", "body", "tags"}, likeTxt, new Pager(10));
//				}
//			}
//		}
//	}
//
//	public void onPost() {
//		if (!canEdit || showPost == null) {
//			return;
//		}
//
//		String next = getPostLink(showPost, false, false);
//
//		if (param("answer")) {
//			// add new answer
//			if (!showPost.isClosed() && !showPost.isReply()) {
//				//create new answer
//				Reply answer = populate(new Reply(), "body");
//				error = validate(answer);
//				if (!error.containsKey("body")) {
//					answer.setTitle(showPost.getTitle());
//					answer.setCreatorid(authUser.getId());
//					answer.setParentid(showPost.getId());
//					answer.create();
//
//					showPost.setAnswercount(showPost.getAnswercount() + 1);
//					if (showPost.getAnswercount() >= AppConfig.MAX_REPLIES_PER_POST) {
//						showPost.setCloserid("0");
//					}
//					// update without adding revisions
//					pc.update(showPost);
//					addBadgeAndUpdate(Badge.EUREKA, answer.getCreatorid().equals(showPost.getCreatorid()));
//				} else {
//					next = null;
//				}
//			}
//		} else if (param("approve")) {
//			String ansid = getParamValue("answerid");
//			if (canEdit && ansid != null && isMine) {
//				Reply answer = (Reply) pc.read(ansid);
//
//				if (answer != null && answer.isReply()) {
//					Profile author = pc.read(answer.getCreatorid());
//					if (author != null && authenticated) {
//						boolean same = author.equals(authUser);
//
//						if (ansid.equals(showPost.getAnswerid())) {
//							// Answer approved award - UNDO
//							showPost.setAnswerid("");
//							if (!same) {
//								author.removeRep(AppConfig.ANSWER_APPROVE_REWARD_AUTHOR);
//								authUser.removeRep(AppConfig.ANSWER_APPROVE_REWARD_VOTER);
//								pc.updateAll(Arrays.asList(author, authUser));
//							}
//						} else {
//							// Answer approved award - GIVE
//							showPost.setAnswerid(ansid);
//							if (!same) {
//								author.addRep(AppConfig.ANSWER_APPROVE_REWARD_AUTHOR);
//								authUser.addRep(AppConfig.ANSWER_APPROVE_REWARD_VOTER);
//								addBadgeOnce(Badge.NOOB, true);
//								pc.updateAll(Arrays.asList(author, authUser));
//							}
//						}
//						showPost.update();
//					}
//				}
//			}
//		} else if (param("editpostid")) {
//			Post beforeUpdate = null;
//			try {
//				beforeUpdate = (Post) BeanUtils.cloneBean(showPost);
//			} catch (Exception ex) {
//				logger.error(null, ex);
//			}
//
//			String postTitle = getParamValue("title");
//			if (!StringUtils.isBlank(postTitle) && postTitle.length() > 10) {
//				showPost.setTitle(postTitle);
//			}
//			if (param("body")) {
//				showPost.setBody(getParamValue("body"));
//			}
//			if (param("tags") && showPost.isQuestion()) {
//				showPost.setTags(Arrays.asList(StringUtils.split(getParamValue("tags"), ",")));
//			}
//
//			showPost.setLasteditby(authUser.getId());
//			//note: update only happens if something has changed
//			if (!showPost.equals(beforeUpdate)) {
//				showPost.update();
//				addBadgeOnceAndUpdate(Badge.EDITOR, true);
//			}
//		} else if (param("close")) {
//			if (isMod) {
//				if (showPost.isClosed()) {
//					showPost.setCloserid(null);
//				} else {
//					showPost.setCloserid(authUser.getId());
//				}
//				showPost.update();
//			}
//		} else if (param("restore")) {
//			String revid = getParamValue("revisionid");
//			if (canEdit && revid != null) {
//				addBadgeAndUpdate(Badge.BACKINTIME, true);
//				showPost.restoreRevisionAndUpdate(revid);
//			}
//		} else if (param("delete")) {
//			if (!showPost.isReply()) {
//				if ((isMine || isMod)) {
//					showPost.delete();
//					next = questionslink + "?success=true&code=16";
//				}
//			} else if (showPost.isReply()) {
//				if (isMine || isMod) {
//					Post parent = pc.read(showPost.getParentid());
//					parent.setAnswercount(parent.getAnswercount() - 1);
//					parent.update();
//					showPost.delete();
//				}
//			}
//		}
//
//		if (next != null) {
//			setRedirect(next);
//		}
//	}
//
//	private void updateViewCount() {
//		//do not count views from author
//		if (showPost == null || isMine) return;
//		String postviews = getStateParam("postviews");
//		if (postviews == null) postviews = "";
//		if (!postviews.contains(showPost.getId())) {
//			long views = (showPost.getViewcount() == null) ? 0 : showPost.getViewcount();
//			showPost.setViewcount(views + 1); //increment count
//			setStateParam("postviews", postviews + "," + showPost.getId());
//			pc.update(showPost);
//		}
//	}

}
