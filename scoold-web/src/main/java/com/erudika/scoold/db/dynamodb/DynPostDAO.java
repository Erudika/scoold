/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.db.dynamodb;

import com.erudika.scoold.db.cassandra.*;
import com.erudika.para.core.PObject;
import com.erudika.para.utils.Search;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.Revision;
import com.erudika.scoold.core.Tag;
import com.erudika.scoold.db.AbstractPostDAO;
import com.erudika.scoold.db.cassandra.CasDAOFactory.Column;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.MultigetSliceQuery;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

/**
 *
 * @author alexb
 */
final class DynPostDAO<T> extends AbstractPostDAO<Post>{

	
    public DynPostDAO() { }

	public Post read (String id) {
//		return cdu.read(Post.class, id);
		return null;
	}

	public String create (Post newInstance) {
//		Mutator<String> mut = cdu.createMutator();
//		
//		// ID for the new revision is generated here!
//		if(newInstance.getRevisionid() == null){
//			//create initial revision = simply create a copy
//			String revid = cdu.create(Revision.createRevisionFromPost(newInstance, true), mut);
//			newInstance.setRevisionid(revid);
////			newInstance.setTimestamp(System.currentTimeMillis());
//		}
//		
//		String id = cdu.create(newInstance, mut);
//		
//		String key = newInstance.getParentid() == null ? newInstance.getClassname() : 
//				newInstance.getParentid();
//		
//		if(!newInstance.isBlackboard()){
//			cdu.addInsertion(new Column<String, String>(key, CasDAOFactory.POSTS_PARENTS, 
//					id, id), mut);
//
//			cdu.addInsertion(new Column<String, String>(newInstance.getCreatorid(), 
//					CasDAOFactory.POSTS_PARENTS, id, id), mut);
//		}
//		
//		mut.execute();
//		
//		//add/remove tags to/from tags table
//		createUpdateOrDeleteTags(newInstance.getTags(), null);
//		if(!newInstance.isReply()){
//			String bodyOrig = newInstance.getBody();
//			newInstance.setBody(getCombinedPostBody(newInstance));
//			Search.index(newInstance, newInstance.getClassname());
//			newInstance.setBody(bodyOrig);
//		}
		return null;
	}

	public void update (Post transientObject) {
//		cdu.update(transientObject);
//		
//		createUpdateOrDeleteTags(transientObject.getTags(), transientObject.getOldtags());
//		if(!transientObject.isReply()){
//			String bodyOrig = transientObject.getBody();
//			transientObject.setBody(getCombinedPostBody(transientObject));
//			Search.index(transientObject, transientObject.getClassname());
//			transientObject.setBody(bodyOrig);
//		}
		return;
	}

	public void delete (Post persistentObject) {
//		Mutator<String> mut = cdu.createMutator();
//		
//		String id = persistentObject.getId();
//		String key = persistentObject.getParentid() == null ? persistentObject.getClassname() : 
//				persistentObject.getParentid();
//		
//		// delete post
//		cdu.delete(persistentObject, mut);
//		
//		if(!persistentObject.isBlackboard()){
//			// delete Comments
//			commentdao.deleteAllCommentsForID(id, mut);
//			// delete Revisions
//			revisiondao.deleteAllRevisionsForID(id, mut);
//			// delete Replies
//			if(!persistentObject.isReply()){
//				deleteAllRepliesForID(id, mut);
//			}
//			
//			cdu.addDeletion(new Column<String, String>(key, CasDAOFactory.POSTS_PARENTS, 
//					id, null), mut);
//
//			cdu.addDeletion(new Column<String, String>(persistentObject.getCreatorid(), 
//					CasDAOFactory.POSTS_PARENTS, id, null), mut);
//		}	
//
//		mut.execute();
//		
//		createUpdateOrDeleteTags(null, persistentObject.getTags());
	}

	private void deleteAllRepliesForID(String parentid, Mutator<String> mut){
//		// read all keys first
//		ArrayList<Post> answers = readAllPostsForID(PObject.classname(Reply.class), parentid, "votes", 
//				null, null, Utils.MAX_ITEMS_PER_PAGE);
//
//		// delete all answers
//		for (Post ans : answers) {
//			cdu.addDeletion(new Column<String, String>(ans.getId(), 
//					CasDAOFactory.OBJECTS), mut);
//			cdu.addDeletion(new Column<String, String>(ans.getCreatorid(),
//					CasDAOFactory.POSTS_PARENTS), mut);
//			// delete Comments
//			commentdao.deleteAllCommentsForID(ans.getId(), mut);
//			// delete Revisions
//			revisiondao.deleteAllRevisionsForID(ans.getId(), mut);
//		}
//
//		// delete from answers
//		cdu.addDeletion(new Column<String, String>(parentid,
//				CasDAOFactory.POSTS_PARENTS), mut);
	}

	public ArrayList<Post> readAllPostsForID(String type, String parentid,
			String sortField, MutableLong page, MutableLong itemcount, int max){
//		String classtype = type;
//		if (StringUtils.isBlank(sortField)) {
//			String key = parentid == null ? classtype : parentid;
//			return cdu.readAll(Post.class, classtype, key, CasDAOFactory.POSTS_PARENTS,
//				String.class, Utils.toLong(page).toString(), page, itemcount, max, true, false, true);
//		}else{
//			return Search.findTerm(classtype, page, itemcount, DAO.CN_PARENTID, parentid, sortField, true, max);
//		}
		return null;
	}
	

	public boolean updateAndCreateRevision (Post transientPost, Post originalPost){
//		//only update and create revision if something has changed
//		if(transientPost.equals(originalPost)) return false;
//
//		//create a new revision
//		String newrevid = cdu.create(Revision.createRevisionFromPost(transientPost, false));
//		
//		transientPost.updateLastActivity();
//		transientPost.setRevisionid(newrevid);
//		update(transientPost);
//
//		//add/remove tags to/from tags table
//		createUpdateOrDeleteTags(transientPost.getTags(), originalPost.getTags());
//		return true;
		return false;
	}

	public void readAllCommentsForPosts (ArrayList<Post> list, int maxPerPage) {
//		Map<String, Integer> map = new HashMap<String, Integer>();
//		ArrayList<String> ids = new ArrayList<String>();
//
//		for (int i = 0; i < list.size(); i++) {
//			Post post = list.get(i);
//			ids.add(post.getId());
//			map.put(post.getId(), i);
//		}
//		
//		Serializer<String> strser = cdu.getSerializer(String.class);
//
//		MultigetSliceQuery<String, String, String> q =
//					HFactory.createMultigetSliceQuery(cdu.getKeyspace(),
//					strser, cdu.getSerializer(String.class), strser);
//
//		q.setKeys(ids);
//		q.setColumnFamily(CasDAOFactory.COMMENTS_PARENTS.getName());
//		q.setRange(null, null, true, Utils.MAX_ITEMS_PER_PAGE + 1);
//
//		try{
//			for (Row<String, String, String> row : q.execute().get()) {
//				ArrayList<String> keyz = new ArrayList<String> ();
//				for (HColumn<String, String> col : row.getColumnSlice().getColumns()){
//					keyz.add(col.getName());
//				}
//
//				ArrayList<Comment> comments = cdu.readAll(Comment.class, keyz);
//
//				if(comments != null && !comments.isEmpty()){
//					Comment last = null;
//					if(keyz.size() > Utils.MAX_ITEMS_PER_PAGE){
//						last = comments.remove(keyz.size() - 1);
//					}else{
//						last = comments.get(keyz.size() - 1);
//					}
//					list.get(map.get(row.getKey())).setComments(comments);
//					list.get(map.get(row.getKey())).setPagenum(NumberUtils.toLong(last.getId()));
//				}
//			}
//		} catch (Exception ex) {
//			logger.log(Level.SEVERE, null, ex);
//		}
	}

}
