/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.db;

import com.erudika.scoold.core.Comment;
import java.util.ArrayList;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public abstract class AbstractCommentDAO<T extends Comment>
        implements GenericDAO<Comment> {

	public abstract ArrayList<T> readAllCommentsForID(String parentid, MutableLong page, MutableLong itemcount);
	public abstract ArrayList<T> readAllCommentsForID(String parentid);
	public abstract void deleteAllCommentsForID(String parentid);
}
