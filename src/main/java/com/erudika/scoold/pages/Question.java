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
package com.erudika.scoold.pages;

import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Profile.Badge;
import static com.erudika.scoold.pages.Base.logger;
import com.erudika.scoold.utils.AppConfig;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Question extends Base{

	public String title;
	public boolean canEdit;
	public boolean isMine;
	public Post showPost;
	public List<Reply> answerslist;
	public List<Post> similarquestions;
	public String markdownHtml;

	private String postlink;

	public Question() {
		title = lang.get("questions.title");
		showPost = null;
		canEdit = false;
		postlink = "";
		String id = param("editpostid") ? getParamValue("editpostid") : getParamValue("id");
		showPost = pc.read(id);

		if (showPost != null && ParaObjectUtils.typesMatch(showPost)) {
			if (showPost.getTitle() != null) {
				title = title + " - " + showPost.getTitle();
			}

			// author can edit, mods can edit & ppl with rep > 100 can edit
			isMine = (authenticated) ? authUser.getId().equals(showPost.getCreatorid()) : false;
			canEdit = (authenticated) ?	(authUser.hasBadge(Badge.TEACHER) || isMod || isMine) : false;
			if (!isMine && !isMod && showPost.isFeedback()) {
				canEdit = false;
			}
			itemcount.setSortby("newest".equals(getParamValue("sortby")) ? "" : "votes");
			answerslist = showPost.getAnswers(itemcount);
			postlink = getPostLink(showPost, false, false);
		}
	}

	public void onGet() {
		if (showPost == null) {
			setRedirect(questionslink + (param("delete") ? "?success=true&code=16" : ""));
			return;
		}
		//get the comments for each answer
		Post.readAllCommentsForPosts(answerslist, MAX_ITEMS_PER_PAGE);
		updateViewCount();

		if (!showPost.isReply()) {
			if (!isAjaxRequest()) {
				String likeTxt = (showPost.getTitle() + " " + showPost.getBody() + " " + showPost.getTags()).trim();
				if (StringUtils.isBlank(likeTxt)) {
					similarquestions = pc.findSimilar(showPost.getType(), showPost.getId(),
							new String[]{"title", "body", "tags"}, likeTxt, itemcount);
				}
			}

			if (param("getcomments") && param(Config._PARENTID)) {
				String parentid = getParamValue(Config._PARENTID);
				Comment parent = new Comment(parentid);
				commentslist = AppConfig.client().getChildren(parent, Utils.type(Comment.class), itemcount);
			}
		}
	}
//
//	private Form getPostEditForm(Post post) {
//		if (post == null) return null;
//		Form form = new Form("editPostForm"+post.getId());
//		form.setId("post-edit-form-"+post.getId());
//
//		TextArea  body = new TextArea("body", true);
//		if (post.isReply()) {
//			body.setLabel(lang.get("posts.answer"));
//		} else {
//			body.setLabel(lang.get("posts.question"));
//		}
//		body.setMinLength(15);
//		body.setMaxLength(AppConfig.MAX_TEXT_LENGTH);
//		body.setRows(4);
//		body.setCols(5);
//		body.setValue(post.getBody());
//
//		if (post.isQuestion()) {
//			TextField tags = new TextField("tags", false);
//			tags.setLabel(lang.get("tags.tags"));
//			tags.setMaxLength(255);
//			tags.setValue(post.getTagsString());
//			form.add(tags);
//		}
//
//        Submit submit = new Submit("editbtn",
//				lang.get("save"), this, "onPostEditClick");
//        submit.setAttribute("class", "btn waves-effect waves-light post-edit-btn");
//		submit.setId("post-edit-btn-"+post.getId());
//
//        form.add(body);
//        form.add(submit);
//
//		return form;
//	}

	public void onPost() {
		processNewCommentRequest(showPost);
		if (!canEdit || showPost == null) {
			return;
		}

		if (param("answer")) {
			// add new answer
			if (!showPost.isClosed() && !showPost.isReply() && showPost.getAnswercount() < AppConfig.MAX_REPLIES_PER_POST) {
				//create new answer
				Reply newq = new Reply();
				newq.setCreatorid(authUser.getId());
				newq.setParentid(showPost.getId());
				newq.setBody(getParamValue("body"));
				newq.create();

				showPost.setAnswercount(showPost.getAnswercount() + 1);
				if (showPost.getAnswercount() >= AppConfig.MAX_REPLIES_PER_POST) {
					showPost.setCloserid("0");
				}
				// update without adding revisions
				AppConfig.client().update(showPost);

				addBadge(Badge.EUREKA, newq.getCreatorid().equals(showPost.getCreatorid()));
//				if (!isAjaxRequest()) setRedirect(escapelink+"#post-"+newq.getId());
			}
		} else if (param("approve")) {
			String ansid = getParamValue("answerid");
			if (canEdit && ansid != null && isMine) {
				Reply answer = (Reply) pc.read(ansid);

				if (answer != null && answer.isReply()) {
					Profile author = pc.read(answer.getCreatorid());
					if (author != null && authenticated) {
						boolean same = author.equals(authUser);

						if (ansid.equals(showPost.getAnswerid())) {
							// Answer approved award - UNDO
							showPost.setAnswerid(null);
							if (!same) {
								author.removeRep(AppConfig.ANSWER_APPROVE_REWARD_AUTHOR);
								authUser.removeRep(AppConfig.ANSWER_APPROVE_REWARD_VOTER);
								author.update();
								authUser.update();
							}
						} else {
							// Answer approved award - GIVE
							showPost.setAnswerid(ansid);
							if (!same) {
								author.addRep(AppConfig.ANSWER_APPROVE_REWARD_AUTHOR);
								authUser.addRep(AppConfig.ANSWER_APPROVE_REWARD_VOTER);
								addBadgeOnce(Badge.NOOB, true);
								author.update();
								authUser.update();
							}
						}
						showPost.update();
					}
				}
			}
		} else if (param("editpostid") || param("title")) {
			Post beforeUpdate = null;
			try {
				beforeUpdate = (Post) BeanUtils.cloneBean(showPost);
			} catch (Exception ex) {
				logger.error(null, ex);
			}

			String postTitle = getParamValue("title");
			if (!StringUtils.isBlank(postTitle) && postTitle.length() > 10) {
				showPost.setTitle(postTitle);
			}
			if (param("body")) {
				showPost.setBody(getParamValue("body"));
			}
			if (param("tags") && showPost.isQuestion()) {
				showPost.setTags(Arrays.asList(StringUtils.split(getParamValue("tags"), ",")));
			}

			showPost.setLasteditby(authUser.getId());
			//note: update only happens if something has changed
			if (!showPost.equals(beforeUpdate)) {
				showPost.update();
				addBadgeOnce(Badge.EDITOR, true);
			}
		} else if (param("close")) {
			if (isMod) {
				if (showPost.isClosed()) {
					showPost.setCloserid(null);
				} else {
					showPost.setCloserid(authUser.getId());
				}
				showPost.update();
			}
		} else if (param("restore")) {
			String revid = getParamValue("revisionid");
			if (canEdit && revid != null) {
				addBadge(Badge.BACKINTIME, true);
				showPost.restoreRevisionAndUpdate(revid);
			}
		} else if (param("delete")) {
			if (!showPost.isReply()) {
				if ((isMine || isMod)) {
					Report rep = new Report();
					rep.setParentid(showPost.getId());
					rep.setLink(postlink);
					rep.setDescription(lang.get("posts.marked"));
					rep.setSubType(Report.ReportType.OTHER);
					rep.setAuthor(authUser.getName());
					rep.setCreatorid(authUser.getId());
					rep.create();
					showPost.delete();
					postlink = questionslink + "?success=true&code=16";
				}
			} else if (showPost.isReply()) {
				if (isMine || isMod) {
					Post parent = pc.read(showPost.getParentid());
					parent.setAnswercount(parent.getAnswercount() - 1);
					parent.update();
					showPost.delete();
				}
			}
		}

		if (postlink != null) {
			setRedirect(postlink);
		}
	}

//	public boolean onAnswerClick() {
//		if (isValidAnswerForm(aForm, showPost)) {
//			processPostEditRequest(showPost, postlink, canEdit);
//		}
//		return false;
//	}

//	public boolean onPostEditClick() {
//		if (isValidPostEditForm(showPost.getEditForm())) {
//			processPostEditRequest(showPost, postlink, canEdit);
//		}
//		return false;
//	}

	private void updateViewCount() {
		//do not count views from author
		if (showPost == null || isMine) return;
		String postviews = getStateParam("postviews");
		if (postviews == null) postviews = "";
		if (!postviews.contains(showPost.getId())) {
			long views = (showPost.getViewcount() == null) ? 0 : showPost.getViewcount();
			showPost.setViewcount(views + 1); //increment count
			setStateParam("postviews", postviews + "," + showPost.getId());
			AppConfig.client().update(showPost);
		}
	}

}
