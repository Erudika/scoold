/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db.cassandra;

import com.scoold.core.Comment;
import com.scoold.core.Post;
import com.scoold.core.Post.PostType;
import com.scoold.core.Revision;
import com.scoold.core.Tag;
import com.scoold.db.AbstractPostDAO;
import com.scoold.db.cassandra.CasDAOFactory.Column;
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
import org.apache.commons.lang.mutable.MutableLong;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

/**
 *
 * @author alexb
 */
final class CasPostDAO<T, PK> extends AbstractPostDAO<Post, Long>{

	private static final Logger logger = Logger.getLogger(CasPostDAO.class.getName());
	private CasDAOUtils cdu = (CasDAOUtils) CasDAOFactory.getInstance().getDAOUtils();
	private CasCommentDAO<Long, Comment> commentdao = new CasCommentDAO<Long, Comment>();
	private CasRevisionDAO<Long, Revision> revisiondao = new CasRevisionDAO<Long, Revision>();
	private CasTagDAO<Tag, Long> tagDao = new CasTagDAO<Tag, Long>();
	
    public CasPostDAO() { }

	public Post read (Long id) {
		return cdu.read(Post.class, id.toString());
	}

	public Long create (Post newInstance) {
		Mutator<String> mut = cdu.createMutator();
		
		// ID for the new revision is generated here!
		if(newInstance.getRevisionid() == null){
			//create initial revision = simply create a copy
			Long revid = cdu.create(Revision.createRevisionFromPost(newInstance, true), mut);
			newInstance.setRevisionid(revid);
//			newInstance.setTimestamp(System.currentTimeMillis());
		}
		
		Long id = cdu.create(newInstance, mut);
		
		String key = newInstance.getParentid() == null ? newInstance.getClasstype() : 
				newInstance.getParentid().toString();
		
		if(!newInstance.isBlackboard()){
			cdu.addInsertion(new Column<Long, String>(key, CasDAOFactory.POSTS_PARENTS, 
					id, id.toString()), mut);

			cdu.addInsertion(new Column<Long, String>(newInstance.getUserid().toString(), 
					CasDAOFactory.POSTS_PARENTS, id, id.toString()), mut);
		}
		
		mut.execute();
		
		//add/remove tags to/from tags table
		if(cdu.isIndexable(newInstance)){
			createUpdateOrDeleteTags(newInstance.getTags(), null);
			if(!newInstance.isReply()){
				String bodyOrig = newInstance.getBody();
				newInstance.setBody(getCombinedPostBody(newInstance));
				cdu.index(newInstance, newInstance.getClasstype());
				newInstance.setBody(bodyOrig);
			}
		}

		return id;
	}

	public void update (Post transientObject) {
		cdu.update(transientObject);
		
		if(cdu.isIndexable(transientObject)){
			createUpdateOrDeleteTags(transientObject.getTags(), transientObject.getOldtags());
			if(!transientObject.isReply()){
				String bodyOrig = transientObject.getBody();
				transientObject.setBody(getCombinedPostBody(transientObject));
				cdu.index(transientObject, transientObject.getClasstype());
				transientObject.setBody(bodyOrig);
			}
		}
	}

	public void delete (Post persistentObject) {
		Mutator<String> mut = cdu.createMutator();
		
		Long id = persistentObject.getId();
		String key = persistentObject.getParentid() == null ? persistentObject.getClasstype() : 
				persistentObject.getParentid().toString();
		
		// delete post
		cdu.delete(persistentObject, mut);
		
		if(!persistentObject.isBlackboard()){
			// delete Comments
			commentdao.deleteAllCommentsForID(id, mut);
			// delete Revisions
			revisiondao.deleteAllRevisionsForID(id, mut);
			// delete Replies
			if(!persistentObject.isReply()){
				deleteAllRepliesForID(id, mut);
			}
			
			cdu.addDeletion(new Column<Long, String>(key, CasDAOFactory.POSTS_PARENTS, 
					id, null), mut);

			cdu.addDeletion(new Column<Long, String>(persistentObject.getUserid().toString(), 
					CasDAOFactory.POSTS_PARENTS, id, null), mut);
		}	

		mut.execute();
		
		if(cdu.isIndexable(persistentObject)){
			createUpdateOrDeleteTags(null, persistentObject.getTags());
		}
	}

	private void deleteAllRepliesForID(Long parentid, Mutator<String> mut){
		// read all keys first
		ArrayList<Post> answers = readAllPostsForID(PostType.REPLY, parentid, "votes", 
				null, null, CasDAOFactory.MAX_ITEMS_PER_PAGE);

		// delete all answers
		for (Post ans : answers) {
			cdu.addDeletion(new Column<String, String>(ans.getId().toString(), 
					CasDAOFactory.OBJECTS), mut);
			cdu.addDeletion(new Column<Long, String>(ans.getUserid().toString(),
					CasDAOFactory.POSTS_PARENTS), mut);
			// delete Comments
			commentdao.deleteAllCommentsForID(ans.getId(), mut);
			// delete Revisions
			revisiondao.deleteAllRevisionsForID(ans.getId(), mut);
		}

		// delete from answers
		cdu.addDeletion(new Column<Long, String>(parentid.toString(),
				CasDAOFactory.POSTS_PARENTS), mut);
	}

	public ArrayList<Post> readAllPostsForID(PostType type, Long parentid,
			String sortField, MutableLong page, MutableLong itemcount, int max){
		String classtype = type.toString();
		if (StringUtils.isBlank(sortField)) {
			String key = parentid == null ? classtype : parentid.toString();
			return cdu.readAll(Post.class, classtype, key, CasDAOFactory.POSTS_PARENTS,
				Long.class, CasDAOUtils.toLong(page), page, itemcount, max, true, false, true);
		}else{
			ArrayList<String> keys = cdu.findTerm(classtype, page, itemcount, "parentid", 
					parentid, sortField, true, max);
			return cdu.readAndRepair(Post.class, keys, itemcount);
		}
	}
		
	private void createUpdateOrDeleteTags(String newTags, String oldTags){
		Map<String, Integer> newTagIndex = new HashMap<String, Integer>();
		//parse
		if(newTags != null){
			for (String ntag : newTags.split(",")) {
				newTagIndex.put(ntag.trim(), 1);
			}
		}
		
		//organize
		if(oldTags != null){
			for (String otag : oldTags.split(",")) {
				otag = otag.trim();
				if(newTagIndex.containsKey(otag)){
					newTagIndex.remove(otag);	// no change so remove
				}else{
					newTagIndex.put(otag, -1); // tag missing so deleteRow and update count
				}
			}
		}
		//clean up the empty tag
		newTagIndex.remove("");

		//create update or deleteRow a given tag
		for (Map.Entry<String, Integer> tagEntry : newTagIndex.entrySet()) {
			Tag tag = tagDao.read(tagEntry.getKey());
			switch(tagEntry.getValue()){
				case 1:
					if(tag == null){
						//create tag
						tag = new Tag(tagEntry.getKey());
						tagDao.create(tag);
					}else{
						//update tag count
						tag.incrementCount();
						tagDao.update(tag);
					}
					break;
				case -1:
					if(tag != null){
						if(tag.getCount() - 1 == 0){
							// delete tag
							tagDao.delete(tag);
						}else{
							//update tag count
							tag.decrementCount();
							tagDao.update(tag);
						}
					}
					break;
			}
		}
	}

	public boolean updateAndCreateRevision (Post transientPost, Post originalPost){
		//only update and create revision if something has changed
		if(transientPost.equals(originalPost)) return false;

		//create a new revision
		Long newrevid = cdu.create(Revision.createRevisionFromPost(transientPost, false));
		
		transientPost.updateLastActivity();
		transientPost.setRevisionid(newrevid);
		update(transientPost);

		//add/remove tags to/from tags table
		createUpdateOrDeleteTags(transientPost.getTags(), originalPost.getTags());
		return true;
	}

	public void readAllCommentsForPosts (ArrayList<Post> list, int maxPerPage) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		ArrayList<String> ids = new ArrayList<String>();

		for (int i = 0; i < list.size(); i++) {
			Post post = list.get(i);
			ids.add(post.getId().toString());
			map.put(post.getId().toString(), i);
		}
		
		Serializer<String> strser = cdu.getSerializer(String.class);

		MultigetSliceQuery<String, Long, String> q =
					HFactory.createMultigetSliceQuery(cdu.getKeyspace(),
					strser, cdu.getSerializer(Long.class), strser);

		q.setKeys(ids);
		q.setColumnFamily(CasDAOFactory.COMMENTS_PARENTS.getName());
		q.setRange(null, null, true, CasDAOFactory.MAX_ITEMS_PER_PAGE + 1);

		try{
			for (Row<String, Long, String> row : q.execute().get()) {
				ArrayList<String> keyz = new ArrayList<String> ();
				for (HColumn<Long, String> col : row.getColumnSlice().getColumns()){
					keyz.add(col.getName().toString());
				}

				ArrayList<Comment> comments = cdu.readAll(Comment.class, keyz);

				if(comments != null && !comments.isEmpty()){
					Comment last = null;
					if(keyz.size() > CasDAOFactory.MAX_ITEMS_PER_PAGE){
						last = comments.remove(keyz.size() - 1);
					}else{
						last = comments.get(keyz.size() - 1);
					}
					list.get(map.get(row.getKey())).setComments(comments);
					list.get(map.get(row.getKey())).setPagenum(last.getId());
				}
			}
		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
		}
	}
	
	private String getCombinedPostBody(Post post){
		ArrayList<Post> replies = readAllPostsForID(post.getPostType(), post.getParentid(), 
				null, null, null, 3);
		
		StringBuilder sb = new StringBuilder(post.getBody());
		for (Post reply : replies) {
			sb.append(" ").append(reply.getBody());
		}
		
		return sb.toString().trim();
	}

}
