/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.db.dynamodb;

import com.erudika.scoold.core.Comment;
import com.erudika.scoold.db.AbstractCommentDAO;
import java.util.ArrayList;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 *
 * @author alexb
 */
final class DynCommentDAO<T> extends AbstractCommentDAO<Comment> {

	
	public DynCommentDAO(){
	}

	public Comment read (String id) {
//		return cdu.read(Comment.class, id);
		return null;
	}

	public String create (Comment newInstance) {
		return "";
	}

	public void update (Comment transientObject) {
	}

	public void delete (Comment persistentObject) {
		
	}

	public ArrayList<Comment> readAllCommentsForID (String parentid, MutableLong page, MutableLong itemcount){
		return null;
	}

	public ArrayList<Comment> readAllCommentsForID (String parentid){
		return null;
	}

	public void deleteAllCommentsForID(String parentid) {
//		Mutator<String> mut = cdu.createMutator();
//		deleteAllCommentsForID(parentid, mut);
//		mut.execute();
	}

	protected void deleteAllCommentsForID(String parentid, Mutator<String> mut) {
		
	}

}
