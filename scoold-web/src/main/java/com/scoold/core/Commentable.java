/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.core;

import java.util.ArrayList;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public interface Commentable extends ScooldObject{

	public void setComments(ArrayList<Comment> comments);
	public ArrayList<Comment> getComments(MutableLong page);
	public ArrayList<Comment> getComments();
	public Long getCommentcount();
	public void setCommentcount(Long count);
}
