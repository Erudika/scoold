/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db;

import com.scoold.core.Post;
import com.scoold.core.Post.PostType;
import java.util.ArrayList;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public abstract class AbstractPostDAO <T extends Post, PK>
		implements GenericDAO <Post, Long>{

	public abstract boolean updateAndCreateRevision(T transientPost, T originalPost);
	public abstract ArrayList<T> readAllPostsForID(PostType type, Long id, String field, MutableLong page, MutableLong itemcount, int max);
	public abstract void readAllCommentsForPosts(ArrayList<T> list, int maxPerPage);
}
