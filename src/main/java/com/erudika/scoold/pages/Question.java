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
import com.erudika.scoold.core.ScooldUser;
import com.erudika.scoold.core.ScooldUser.Badge;
import static com.erudika.scoold.pages.Base.logger;
import com.erudika.scoold.utils.AppConfig;
import java.util.Arrays;
import java.util.List;
import org.apache.click.control.Form;
import org.apache.click.control.Submit;
import org.apache.click.control.TextArea;
import org.apache.click.control.TextField;
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
	public Form aForm;
	public String markdownHtml;

	private String postlink;

	public Question() {
		title = lang.get("questions.title");
		showPost = null;
		canEdit = false;
		aForm = getAnswerForm();
		postlink = "";
		String id = param("editpostid") ? getParamValue("editpostid") : getParamValue("id");
		showPost = pc.read(id);

		if (showPost != null && ParaObjectUtils.typesMatch(showPost)) {
			if (showPost.getTitle() != null) {
				title = title + " - " + showPost.getTitle();
			}

			// author can edit, mods can edit & ppl with rep > 100 can edit
			isMine = (authenticated) ? authUser.getId().equals(showPost.getCreatorid()) : false;
			canEdit = (authenticated) ?	(authUser.hasBadge(Badge.TEACHER) || inRole("mod") || isMine) : false;
			if (!isMine && !inRole("mod") && showPost.isFeedback()) {
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

	private Form getPostEditForm(Post post) {
		if (post == null) return null;
		Form form = new Form("editPostForm"+post.getId());
		form.setId("post-edit-form-"+post.getId());

		TextArea  body = new TextArea("body", true);
		if (post.isReply()) {
			body.setLabel(lang.get("posts.answer"));
		} else {
			body.setLabel(lang.get("posts.question"));
		}
		body.setMinLength(15);
		body.setMaxLength(AppConfig.MAX_TEXT_LENGTH);
		body.setRows(4);
		body.setCols(5);
		body.setValue(post.getBody());

		if (post.isQuestion()) {
			TextField tags = new TextField("tags", false);
			tags.setLabel(lang.get("tags.tags"));
			tags.setMaxLength(255);
			tags.setValue(post.getTagsString());
			form.add(tags);
		}

		TextField hideme = new TextField("additional", false);
		hideme.setLabel("Leave blank!");
		hideme.setAttribute("class", "hide");

        Submit submit = new Submit("editbtn",
				lang.get("save"), this, "onPostEditClick");
        submit.setAttribute("class", "btn waves-effect waves-light post-edit-btn");
		submit.setId("post-edit-btn-"+post.getId());

		form.add(hideme);
        form.add(body);
        form.add(submit);

		return form;
	}

	public void onRender() {
		if (canEdit && showPost != null) {
			// attach edit form to each post
			attachEditForm(showPost);
			if (answerslist != null && !answerslist.isEmpty()) {
				for (Post answr : answerslist) attachEditForm(answr);
			}
		}
	}

	public void onPost() {
		processNewCommentRequest(showPost);
		processPostEditRequest(showPost, postlink, canEdit);
	}

	private void attachEditForm(Post post) {
		Form f = getPostEditForm(post);
		post.setEditForm(f);
	}

	public boolean onAnswerClick() {
		if (isValidAnswerForm(aForm, showPost)) {
			processPostEditRequest(showPost, postlink, canEdit);
		}
		return false;
	}

//	public boolean onPostEditClick() {
//		if (isValidPostEditForm(showPost.getEditForm())) {
//			processPostEditRequest(showPost, postlink, canEdit);
//		}
//		return false;
//	}

	private void updateViewCount() {
		//do not count views from author
		if (showPost == null || authenticated && authUser.getId().equals(showPost.getCreatorid())) return;
		// inaccurate but... KISS!
		String postviews = getStateParam("postviews");
		if (postviews == null) postviews = "";
		if (!postviews.contains(showPost.getId())) {
			long views = (showPost.getViewcount() == null) ? 0 : showPost.getViewcount();
			showPost.setViewcount(views + 1); //increment count
			setStateParam("postviews", postviews + "," + showPost.getId());
			AppConfig.client().update(showPost);
		}
	}

	private void processPostEditRequest(Post post, String escapelink, boolean canEdit) {
		if (!canEdit || post == null) {
			return;
		}

		if (param("answer")) {
			// add new answer
			if (!post.isClosed() && !post.isReply() && post.getAnswercount() < AppConfig.MAX_REPLIES_PER_POST) {
				//create new answer
				Reply newq = new Reply();
				newq.setCreatorid(authUser.getId());
				newq.setParentid(post.getId());
				newq.setBody(getParamValue("body"));
				newq.create();

				post.setAnswercount(post.getAnswercount() + 1);
				if (post.getAnswercount() >= AppConfig.MAX_REPLIES_PER_POST) {
					post.setCloserid("0");
				}
				post.updateLastActivity();
				// update without adding revisions
				AppConfig.client().update(post);

				addBadge(Badge.EUREKA, newq.getCreatorid().equals(post.getCreatorid()));
//				if (!isAjaxRequest()) setRedirect(escapelink+"#post-"+newq.getId());
			}
		} else if (param("approve")) {
			String ansid = getParamValue("answerid");
			if (canEdit && ansid != null && isMine) {
				Reply answer = (Reply) pc.read(ansid);

				if (answer != null && answer.isReply()) {
					ScooldUser author = pc.read(answer.getCreatorid());
					if (author != null && authenticated) {
						boolean same = author.equals(authUser);

						if (ansid.equals(post.getAnswerid())) {
							// Answer approved award - UNDO
							post.setAnswerid(null);
							if (!same) {
								author.removeRep(AppConfig.ANSWER_APPROVE_REWARD_AUTHOR);
								authUser.removeRep(AppConfig.ANSWER_APPROVE_REWARD_VOTER);
								author.update();
								authUser.update();
							}
						} else {
							// Answer approved award - GIVE
							post.setAnswerid(ansid);
							if (!same) {
								author.addRep(AppConfig.ANSWER_APPROVE_REWARD_AUTHOR);
								authUser.addRep(AppConfig.ANSWER_APPROVE_REWARD_VOTER);
								addBadgeOnce(Badge.NOOB, true);
								author.update();
								authUser.update();
							}
						}
						post.update();
					}
				}
			}
		} else if (param("editpostid") || param("title")) {
			Post beforeUpdate = null;
			try {
				beforeUpdate = (Post) BeanUtils.cloneBean(post);
			} catch (Exception ex) {
				logger.error(null, ex);
			}

			String postTitle = getParamValue("title");
			if (!StringUtils.isBlank(postTitle) && postTitle.length() > 10) {
				post.setTitle(postTitle);
			}
			if (param("body")) {
				post.setBody(getParamValue("body"));
			}
			if (param("tags") && post.isQuestion()) {
				post.setTags(Arrays.asList(StringUtils.split(getParamValue("tags"), ",")));
			}

			post.setLasteditby(authUser.getId());
			//note: update only happens if something has changed
			if (!post.equals(beforeUpdate)) {
				post.update();
				addBadgeOnce(Badge.EDITOR, true);
			}
		} else if (param("close")) {
			if (inRole("mod")) {
				if (post.isClosed()) {
					post.setCloserid(null);
				} else {
					post.setCloserid(authUser.getId());
				}
				post.update();
			}
		} else if (param("restore")) {
			String revid = getParamValue("revisionid");
			if (canEdit && revid != null) {
				addBadge(Badge.BACKINTIME, true);
				post.restoreRevisionAndUpdate(revid);
			}
		} else if (param("delete")) {
			if (!post.isReply()) {
				if ((isMine || inRole("mod"))) {
					Report rep = new Report();
					rep.setParentid(post.getId());
					rep.setLink(escapelink);
					rep.setDescription(lang.get("posts.marked"));
					rep.setSubType(Report.ReportType.OTHER);
					rep.setAuthor(authUser.getName());
					rep.setCreatorid(authUser.getId());

					rep.create();
					post.delete();
					escapelink = questionslink + "?success=true&code=16";
				}
			} else if (post.isReply()) {
				if (isMine || inRole("mod")) {
					Post parent = pc.read(post.getParentid());
					parent.setAnswercount(parent.getAnswercount() - 1);
					parent.update();
					post.delete();
				}
			}
		}

		if (escapelink != null) {
			setRedirect(escapelink);
		}
	}

}
