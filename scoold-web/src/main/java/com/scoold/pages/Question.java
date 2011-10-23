/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import com.scoold.core.Post;
import com.scoold.core.School;
import com.scoold.core.Post.PostType;
import com.scoold.core.Revision;
import com.scoold.core.User.Badge;
import java.util.ArrayList;
import org.apache.click.control.Form;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 *
 * @author alexb
 */
public class Question extends BasePage{

	public String title;
	public boolean canEdit;
	public boolean isMine;
	public Post showPost;
	public ArrayList<Post> answerslist;
	public ArrayList<Post> similarquestions;
	public ArrayList<Revision> revisionslist;	
	public Form aForm;
	public String markdownHtml;

	private String actionlink;

	public Question() {
		title = lang.get("questions.title");
		showPost = null;
		canEdit = false;
		aForm = getAnswerForm(PostType.QUESTION);
		actionlink = ""; 

		Long id = NumberUtils.toLong(getParamValue("id"));
		// override id if it's an edit request
		if(param("editpostid")){
			id = NumberUtils.toLong(getParamValue("editpostid"));
		}
		String uuid = getParamValue("uuid");

		String uri = (String) req.getAttribute("javax.servlet.forward.request_uri");
		if (StringUtils.containsIgnoreCase(uri, "question")) {
			actionlink = questionlink;
		} else if(StringUtils.containsIgnoreCase(uri, "feedback")) {
			actionlink = feedbacklink;
		} else if(StringUtils.containsIgnoreCase(uri, "translate")) {
			actionlink = translatelink;
		}

        if(id.longValue() == 0L && StringUtils.isBlank(uuid)){
			setRedirect(actionlink);
			return;
		}else {
            showPost = (id.longValue() == 0L) ? Post.getPostDao().read(uuid) :
				Post.getPostDao().read(id);
			
			if(showPost != null){
				if(showPost.getTitle() != null)
					title = title + " - " + showPost.getTitle();

				isMine = (authenticated) ?
					authUser.getId().equals(showPost.getUserid()) : false;
				
				canEdit = (authenticated) ? 
						(authUser.hasBadge(Badge.FRESHMAN) || inRole("mod") || isMine) : false;
				
				// author can edit, mods can edit & ppl with rep > 100 can edit
				if(!isMine && !inRole("mod")){
					if (showPost.isFeedback()) {
						canEdit = false;						
					}else if(showPost.isQuestion()){
						if (!authenticated || !authUser.hasBadge(Badge.TEACHER)) {
							canEdit = false;
						}
					}
				}

			}else{
				setRedirect(actionlink);
				return;
			}
        }

		if("revisions".equals(showParam)){
			title = title + " - " + lang.get("revisions.title");
			pageMacroCode = "#revisionspage($revisionslist $canEdit $showPost)";
			revisionslist = showPost.getRevisions(pagenum, itemcount);
		}
	}

	public void onGet() {
		if(showPost != null){
			processPostRequest(showPost, actionlink, canEdit, isMine);

			if(showPost.isQuestion() || showPost.isFeedback()){
				if(!isAjaxRequest()){
					similarquestions = search.findSimilarQuestions(showPost, 15);
					if(showPost.isQuestion()){
						addModel("showSchool", School.getSchoolDao().
								read(showPost.getParentuuid())); 
					}
				}
				String sortby = "votes";
				if("newest".equals(getParamValue("sortby"))){
					sortby = "timestamp";
				}
				if(showPost.getAnswercount() > 0){
					pageMacroCode = "#answerspage($answerslist $showPost)";
					answerslist = showPost.getAnswers(sortby, pagenum, itemcount);
					//get the comments for each answer
					Post.getPostDao().readAllCommentsForPosts(answerslist, MAX_ITEMS_PER_PAGE);
				}
			}			
		}
	}

	public void onRender(){
		if(canEdit && showPost != null){
			// attach edit form to each post
			attachEditForm(showPost);
			if(showPost.isQuestion() && answerslist != null){
				for (Post answr : answerslist)
					attachEditForm(answr);
			}
		}
	}

	public void onPost(){
		processNewCommentRequest(showPost);
		// catch "accept" requests!
		processPostEditRequest(showPost, actionlink, canEdit);
	}

	private void attachEditForm(Post post){
		Form f = getPostEditForm(post);
		post.setEditForm(f);
	}

	public boolean onAnswerClick(){
		if(isValidAnswerForm(aForm, showPost)){
			processPostEditRequest(showPost, actionlink, canEdit);
		}
		return false;
	}

	public boolean onPostEditClick(){
		if(isValidPostEditForm(showPost.getEditForm())){
			processPostEditRequest(showPost, actionlink, canEdit);
		}
		return false;
	}

	public boolean onSecurityCheck() {
        return aForm.onSubmitCheck(this, actionlink+"/?code=7&error=true");
    }

}
