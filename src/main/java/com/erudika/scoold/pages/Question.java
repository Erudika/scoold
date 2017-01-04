/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.School;
import com.erudika.scoold.core.Revision;
import com.erudika.scoold.core.ScooldUser.Badge;
import static com.erudika.scoold.pages.Base.MAX_ITEMS_PER_PAGE;
import com.erudika.scoold.utils.AppConfig;
import java.util.List;
import org.apache.click.control.Form;
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
	public List<Revision> revisionslist;
	public Form aForm;
	public String markdownHtml;

	private String postlink;

	public Question() {
		title = lang.get("questions.title");
		showPost = null;
		canEdit = false;
		aForm = getAnswerForm();
		postlink = "";

		String id = getParamValue("id");
		// override id if it's an edit request
		if (param("editpostid")) {
			id = getParamValue("editpostid");
		}

        if (id == null) {
			if (!isAjaxRequest()) setRedirect(HOMEPAGE);
			return;
		} else {
            showPost = pc.read(id);

			if (showPost != null && ParaObjectUtils.typesMatch(showPost)) {
				if (showPost.getTitle() != null)
					title = title + " - " + showPost.getTitle();

				isMine = (authenticated) ?
					authUser.getId().equals(showPost.getCreatorid()) : false;

				canEdit = (authenticated) ?
						(authUser.hasBadge(Badge.FRESHMAN) || inRole("mod") || isMine) : false;

				// author can edit, mods can edit & ppl with rep > 100 can edit
				if (!isMine && !inRole("mod")) {
					if (showPost.isFeedback()) {
						canEdit = false;
					} else if (showPost.isQuestion()) {
						if (!authenticated || !authUser.hasBadge(Badge.TEACHER)) {
							canEdit = false;
						}
					}
				}
			} else {
				setRedirect(HOMEPAGE);
				return;
			}
        }

		postlink = getPostLink(showPost, false, false);
	}

	public void onGet() {
		if (showPost != null) {
			if ("revisions".equals(showParam)) {
				title = title + " - " + lang.get("revisions.title");
				revisionslist = showPost.getRevisions(itemcount);
			} else {
				if (updateViewCount(showPost)) showPost.update();

				if (!showPost.isReply()) {
					if (!isAjaxRequest()) {
						String likeTxt = (showPost.getTitle() + " " + showPost.getBody() + " " + showPost.getTags()).trim();
						if (StringUtils.isBlank(likeTxt)) {
							similarquestions = pc.findSimilar(showPost.getType(), showPost.getId().toString(),
									new String[]{"title", "body", "tags"}, likeTxt, itemcount);
						}
						if (showPost.isQuestion()) {
							School s = pc.read(showPost.getParentid());
							if (s != null) addModel("showSchool", s);
						}
					}

					if (param("getcomments") && param(Config._PARENTID)) {
						String parentid = getParamValue(Config._PARENTID);
						Comment parent = new Comment(parentid);
						commentslist = AppConfig.client().getChildren(parent, Utils.type(Comment.class), itemcount);
					} else {
						String sortby = "votes";
						if ("newest".equals(getParamValue("sortby"))) {
							sortby = "";
						}
						itemcount.setSortby(sortby);
						if (showPost.getAnswercount() > 0) {
							answerslist = showPost.getAnswers(itemcount);
							//get the comments for each answer
							Post.readAllCommentsForPosts(answerslist, MAX_ITEMS_PER_PAGE);
						}
					}
				}
			}
		}
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

	public boolean onPostEditClick() {
		if (isValidPostEditForm(showPost.getEditForm())) {
			processPostEditRequest(showPost, postlink, canEdit);
		}
		return false;
	}

	private boolean updateViewCount(Post showPost) {
		//do not count views from author
		if (authenticated && authUser.getId().equals(showPost.getCreatorid())) return false;
		// inaccurate but... KISS!
		String list = getStateParam("postviews");
		if (list == null) list = "";

		if (!list.contains(showPost.getId().toString())) {
			long views = (showPost.getViewcount() == null) ? 0 : showPost.getViewcount();
			showPost.setViewcount(views + 1); //increment count
			if (!list.isEmpty() && !list.endsWith(",")) list = list.concat(",");
			list = list.concat(showPost.getId().toString());
			setStateParam("postviews", list);
			return true;
		}
		return false;
	}
}
