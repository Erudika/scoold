/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.db;

import com.erudika.scoold.core.Post;
import java.util.ArrayList;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public abstract class AbstractPostDAO <T>
		implements GenericDAO <Post>{

	public abstract boolean updateAndCreateRevision(T transientPost, T originalPost);
	public abstract ArrayList<T> readAllPostsForID(String type, String id, String field, MutableLong page, MutableLong itemcount, int max);
	public abstract void readAllCommentsForPosts(ArrayList<T> list, int maxPerPage);
}
