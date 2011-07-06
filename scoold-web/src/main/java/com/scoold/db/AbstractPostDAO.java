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
	public abstract ArrayList<T> readAllPostsForUUID(PostType type, String uuid, String field, MutableLong page, MutableLong itemcount);
	public abstract ArrayList<T> readAllForKeys(ArrayList<String> keys);
	public abstract <N> ArrayList<T> readFeedbackSortedBy(String sortField, MutableLong page, MutableLong itemcount, boolean reverse);

	public abstract void readAllCommentsForPosts(ArrayList<T> list, int maxPerPage);
	public abstract void deleteAllAnswersForUUID(String parentUUID);
}
