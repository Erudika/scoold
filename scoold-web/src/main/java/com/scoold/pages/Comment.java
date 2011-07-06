/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import org.apache.commons.lang.math.NumberUtils;

/**
 *
 * @author alexb
 */
public class Comment extends BasePage{


	public com.scoold.core.Comment showComment;

	public String title;

	public Comment() {
		title = lang.get("comment.title");
	}

	public void onGet() {
		if(param("id")){
			Long id = NumberUtils.toLong(getParamValue("id"));
			showComment = com.scoold.core.Comment.getCommentDao().read(id);
		}
	}

	public void onPost(){
		processNewCommentRequest(null);
	}

}
