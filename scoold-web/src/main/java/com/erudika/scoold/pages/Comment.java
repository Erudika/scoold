/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class Comment extends Base{

	public com.erudika.scoold.core.Comment showComment;

	public String title;

	public Comment() {
		title = lang.get("comment.title");
	}

	public void onGet() {
		if(param("id")){
			String id = getParamValue("id");
			showComment = dao.read(id);
			if(showComment == null || !utils.typesMatch(showComment)){
				setRedirect(HOMEPAGE);
			}
		}
	}

	public void onPost(){
		processNewCommentRequest(null);
	}

}
