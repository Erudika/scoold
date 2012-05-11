/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db;

import com.scoold.core.Comment;
import java.util.ArrayList;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public abstract class AbstractCommentDAO<T extends Comment, PK>
        implements GenericDAO<Comment, Long> {

	public abstract ArrayList<T> readAllCommentsForID(Long parentid, MutableLong page, MutableLong itemcount);
	public abstract ArrayList<T> readAllCommentsForID(Long parentid);
	public abstract void deleteAllCommentsForID(Long parentid);
}
