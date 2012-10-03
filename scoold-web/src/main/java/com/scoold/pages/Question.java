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

	private String postlink;

	public Question() {
		title = lang.get("questions.title");
		showPost = null;
		canEdit = false;
		aForm = getAnswerForm(PostType.QUESTION);
		postlink = ""; 

		Long id = NumberUtils.toLong(getParamValue("id"));
		// override id if it's an edit request
		if(param("editpostid")){
			id = NumberUtils.toLong(getParamValue("editpostid"));
		}

        if(id.longValue() == 0L){
			if(!isAjaxRequest()) setRedirect(HOMEPAGE); 
			return;
		}else {
            showPost = Post.getPostDao().read(id);
			
			if(showPost != null && daoutils.typesMatch(showPost)){
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
				setRedirect(HOMEPAGE);
				return;
			}
        }
		
		postlink = getPostLink(showPost, false, false);
	}

	public void onGet() {
		if(showPost != null){
			if("revisions".equals(showParam)){
				title = title + " - " + lang.get("revisions.title");
				revisionslist = showPost.getRevisions(pagenum, itemcount);
			}else{
				if(updateViewCount(showPost)) showPost.update();

				if(daoutils.isIndexable(showPost) && !showPost.isReply()){
					if(!isAjaxRequest()){
						String likeTxt = showPost.getTitle().concat(" ").concat(showPost.getBody()).
								concat(" ").concat(showPost.getTags());
						similarquestions = daoutils.readAndRepair(Post.class, 
								daoutils.findSimilar(showPost.getClasstype(), 
								showPost.getId().toString(), new String[]{"title", "body", "tags"}, 
								likeTxt, MAX_ITEMS_PER_PAGE), itemcount);
						if(showPost.isQuestion()){
							School s = School.getSchoolDao().read(showPost.getParentid());
							if(s != null) addModel("showSchool", s); 
						}
					}

					if(param("getcomments") && param("parentid")){
						Long parentid = NumberUtils.toLong(getParamValue("parentid"), 0L);
						commentslist = com.scoold.core.Comment.getCommentDao().readAllCommentsForID(parentid,
								pagenum, null);
					}else{
						String sortby = "votes";
						if("newest".equals(getParamValue("sortby"))){
							sortby = "";
						}
						if(showPost.getAnswercount() > 0){
							answerslist = showPost.getAnswers(sortby, pagenum, itemcount);
							//get the comments for each answer
							Post.getPostDao().readAllCommentsForPosts(answerslist, MAX_ITEMS_PER_PAGE);
						}
					}
				}			
			}
		}
	}

	public void onRender(){
		if(canEdit && showPost != null){
			// attach edit form to each post
			attachEditForm(showPost);
			if(answerslist != null && !answerslist.isEmpty()){
				for (Post answr : answerslist) attachEditForm(answr);
			}
		}
	}

	public void onPost(){
		processNewCommentRequest(showPost);
		processPostEditRequest(showPost, postlink, canEdit);
		
		
	}

	private void attachEditForm(Post post){
		Form f = getPostEditForm(post);
		post.setEditForm(f);
	}

	public boolean onAnswerClick(){
		if(isValidAnswerForm(aForm, showPost)){
			processPostEditRequest(showPost, postlink, canEdit);
		}
		return false;
	}

	public boolean onPostEditClick(){
		if(isValidPostEditForm(showPost.getEditForm())){
			processPostEditRequest(showPost, postlink, canEdit);
		}
		return false;
	}
	
	private boolean updateViewCount(Post showPost){
		//do not count views from author
		if(authenticated && authUser.getId().equals(showPost.getUserid())) return false;
		// inaccurate but... KISS!
		String list = getStateParam("postviews");
		if(list == null) list = "";
		
		if (!list.contains(showPost.getId().toString())) {
			long views = (showPost.getViewcount() == null) ? 0 : showPost.getViewcount();
			showPost.setViewcount(views + 1); //increment count
			list = list.concat(",").concat(showPost.getId().toString());
			setStateParam("postviews", list);
			return true;
		}
		return false;
	}
}
