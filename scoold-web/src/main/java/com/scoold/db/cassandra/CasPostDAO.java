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
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractPostDAO;
import com.scoold.db.cassandra.CasDAOFactory.CF;
import com.scoold.db.cassandra.CasDAOFactory.Column;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.MultigetSliceQuery;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public class CasPostDAO<T, PK> extends AbstractPostDAO<Post, Long>{

	private static final Logger logger = Logger.getLogger(CasPostDAO.class.getName());
	private CasDAOUtils cdu = new CasDAOUtils();
	
    public CasPostDAO() { }

	public Post read (Long id) {
		return cdu.read(Post.class, id.toString(), CasDAOFactory.POSTS);
	}

	public Post read (String uuid) {
		ArrayList<Post> post = cdu.readAll(Post.class, uuid,
				CasDAOFactory.POSTS_UUIDS, CasDAOFactory.POSTS, String.class,
				null, null, null, 1, true, false, false);
		
		if(post == null || post.isEmpty()) return null;

		return post.get(0);
	}

	public Long create (Post newInstance) {
		newInstance.fixTags();
		newInstance.fixTitle();

		newInstance.setUuid(null);
		newInstance.setId(null);
		// UUID for the new revision is generated here!
		newInstance.setRevisionuuid(UUID.randomUUID().toString());
		newInstance.setTimestamp(System.currentTimeMillis());
		
		Mutator<String> mut = CasDAOUtils.createMutator();
		Long id = cdu.create(newInstance, CasDAOFactory.POSTS, mut);

		if(id == null) return null;

		CasDAOUtils.addInsertion(new Column<String, String>(newInstance.getUuid(),
				CasDAOFactory.POSTS_UUIDS, id.toString(), id.toString()), mut);

		switch(newInstance.getPostType()){
			case ANSWER:
				//add to answers
				cdu.addTimesortColumn(newInstance.getParentuuid(), id,
						CasDAOFactory.ANSWERS, id, null, mut);
				// add to sort columns
				cdu.addNumbersortColumn(newInstance.getParentuuid(),
						CasDAOFactory.ANSWERS_BY_VOTES,
						id, newInstance.getVotes(), null, mut);

				CasDAOUtils.addInsertion(new Column<Long, String>(
						newInstance.getUserid().toString(), CasDAOFactory.USER_ANSWERS,
						id, id.toString()), mut);

				break;
			case QUESTION:
				//add to questions
				cdu.addTimesortColumn(newInstance.getParentuuid(), id,
						CasDAOFactory.QUESTIONS, id, null, mut);

				cdu.addNumbersortColumn(newInstance.getParentuuid(),
						CasDAOFactory.QUESTIONS_BY_VOTES,
						id, newInstance.getVotes(), null, mut);

				cdu.addTimesortColumn(null, id, CasDAOFactory.ALL_QUESTIONS, id, null, mut);
				cdu.addNumbersortColumn(null, CasDAOFactory.ALL_QUESTIONS_BY_VOTES,
						id, newInstance.getVotes(), null, mut);

				CasDAOUtils.addInsertion(new Column<Long, String>(
						newInstance.getUserid().toString(), CasDAOFactory.USER_QUESTIONS,
						id, id.toString()), mut);
				break;

			case BLACKBOARD:
				break;

			case FEEDBACK:
				// add to feedback
				cdu.addTimesortColumn(null, id, CasDAOFactory.FEEDBACK, id, null, mut);
				cdu.addNumbersortColumn(null, CasDAOFactory.FEEDBACK_BY_VOTES,
						id, newInstance.getVotes(), null, mut);

				break;
		}

		mut.execute();

		//create initial revision = simply create a copy
		Revision.getRevisionDao().create(Revision.createRevisionFromPost(newInstance, true));
		//add/remove tags to/from tags table
		if(newInstance.isQuestion() || newInstance.isFeedback())
			createUpdateOrDeleteTags(newInstance.getTags(), null);

		if(newInstance.isAnswer() || newInstance.isQuestion() || newInstance.isFeedback())
			newInstance.index();

		return id;
	}

	public void update (Post transientObject) {
		transientObject.fixTags();
		transientObject.fixTitle();
		Long id = transientObject.getId();

		Long lastact = (transientObject.getLastactivity() == null) ? 
			System.currentTimeMillis() : transientObject.getLastactivity();
		Long oldact = (transientObject.getOldactivity() == null) ? lastact :
			transientObject.getOldactivity();

		Mutator<String> mut = CasDAOUtils.createMutator();

		switch (transientObject.getPostType()) {
			case ANSWER:
				cdu.addNumbersortColumn(transientObject.getParentuuid(),
					CasDAOFactory.ANSWERS_BY_VOTES,
					id, transientObject.getVotes(),
					transientObject.getOldvotes(), mut);
				break;
			case QUESTION:
				cdu.addNumbersortColumn(transientObject.getParentuuid(),
					CasDAOFactory.QUESTIONS_BY_VOTES,
					id, transientObject.getVotes(),
					transientObject.getOldvotes(), mut);

				cdu.addTimesortColumn(transientObject.getParentuuid(), id,
					CasDAOFactory.QUESTIONS_BY_ACTIVITY, lastact, oldact, mut);

				cdu.addNumbersortColumn(null,
					CasDAOFactory.ALL_QUESTIONS_BY_VOTES,
					id, transientObject.getVotes(),
					transientObject.getOldvotes(), mut);

				cdu.addTimesortColumn(null, id, CasDAOFactory.ALL_QUESTIONS_BY_ACTIVITY,
						lastact, oldact, mut);
				break;

			case FEEDBACK:
				cdu.addNumbersortColumn(null, CasDAOFactory.FEEDBACK_BY_VOTES,
					id, transientObject.getVotes(), transientObject.getOldvotes(), mut);

				cdu.addTimesortColumn(null, id, 
						CasDAOFactory.FEEDBACK_BY_ACTIVITY, lastact, oldact, mut);

				break;
		}

		cdu.update(transientObject, CasDAOFactory.POSTS, mut);

		mut.execute();
		
		if(transientObject.isAnswer() || transientObject.isQuestion())
			transientObject.index();
	}

	public void delete (Post persistentObject) {
		Mutator<String> mut = CasDAOUtils.createMutator();

		String uuid = persistentObject.getUuid();
		String parentuuid = persistentObject.getParentuuid();
		Long id = persistentObject.getId();
		Long lastact = (persistentObject.getLastactivity() == null) ?
			System.currentTimeMillis() : persistentObject.getLastactivity();

		// delete post
		cdu.delete(persistentObject, CasDAOFactory.POSTS, mut);

		CasDAOUtils.addDeletion(new Column<String, String>(uuid, CasDAOFactory.POSTS_UUIDS,
				id.toString(), null), mut);

		if(!persistentObject.isBlackboard()){
			// delete Comments
			new CasCommentDAO<Comment, Long>().deleteAllCommentsForUUID(uuid, mut);
			// delete Revisions
			new CasRevisionDAO<Revision, Long>().deleteAllRevisionsForUUID(uuid, mut);
			// delete Answers
			if(!persistentObject.isAnswer()){
				deleteAllAnswersForUUID(uuid, mut);
			}
		}

		switch(persistentObject.getPostType()){
			case ANSWER:
				// delete from answers
				cdu.removeTimesortColumn(parentuuid, CasDAOFactory.ANSWERS, id, mut);
				cdu.removeNumbersortColumn(parentuuid, CasDAOFactory.ANSWERS_BY_VOTES, id,
						persistentObject.getVotes(), mut);

				CasDAOUtils.addDeletion(new Column<Long, String>(
						persistentObject.getUserid().toString(),
						CasDAOFactory.USER_ANSWERS, id, null), mut);

				break;
			case QUESTION:
				// delete from questions
				cdu.removeTimesortColumn(parentuuid, CasDAOFactory.QUESTIONS, id, mut);
				// delete all sort columns
				cdu.removeNumbersortColumn(parentuuid, CasDAOFactory.QUESTIONS_BY_VOTES,
						id, persistentObject.getVotes(), mut);

				cdu.removeTimesortColumn(null, CasDAOFactory.ALL_QUESTIONS,
						persistentObject.getId(), mut);

				cdu.removeNumbersortColumn(null,CasDAOFactory.ALL_QUESTIONS_BY_VOTES,
						id, persistentObject.getVotes(), mut);

				cdu.removeTimesortColumn(parentuuid, CasDAOFactory.QUESTIONS_BY_ACTIVITY,
						lastact, mut);

				cdu.removeTimesortColumn(null, CasDAOFactory.ALL_QUESTIONS_BY_ACTIVITY,
						lastact, mut);

				CasDAOUtils.addDeletion(new Column<Long, String>(
						persistentObject.getUserid().toString(),
						CasDAOFactory.USER_QUESTIONS, id, null), mut);

				break;
			case BLACKBOARD: break;

			case FEEDBACK:
				// delete from feedback
				cdu.removeTimesortColumn(null, CasDAOFactory.FEEDBACK, id, mut);
				cdu.removeNumbersortColumn(null, CasDAOFactory.FEEDBACK_BY_VOTES,
						id, persistentObject.getVotes(), mut);

				cdu.removeTimesortColumn(null, CasDAOFactory.FEEDBACK_BY_ACTIVITY,
						lastact, mut);

				break;
		}

		mut.execute();
		
		// delete all tags for post
		if(persistentObject.isQuestion() || persistentObject.isFeedback())
			createUpdateOrDeleteTags("", persistentObject.getTags());

		if(persistentObject.isAnswer() || persistentObject.isQuestion())
			persistentObject.unindex();
	}

	public void deleteAllAnswersForUUID(String parentUUID){
		Mutator<String> mut = CasDAOUtils.createMutator();
		deleteAllAnswersForUUID(parentUUID, mut);
		mut.execute();
	}

	private void deleteAllAnswersForUUID(String parentUUID, Mutator<String> mut){
		// read all keys first
		ArrayList<Post> answers = cdu.readAll(Post.class, parentUUID, 
				CasDAOFactory.ANSWERS, CasDAOFactory.POSTS, Long.class,
				null, null, null, CasDAOFactory.DEFAULT_LIMIT, false, false, false);

		CasCommentDAO<Comment, Long> cdao = new CasCommentDAO<Comment, Long>();
		CasRevisionDAO<Revision, Long> rdao = new CasRevisionDAO<Revision, Long>();

		// delete all answers
		for (Post ans : answers) {
			CasDAOUtils.addDeletion(new Column<String, String>(ans.getId().toString(), 
					CasDAOFactory.POSTS), mut);
			CasDAOUtils.addDeletion(new Column<String, String>(ans.getUuid(), 
					CasDAOFactory.POSTS_UUIDS), mut);
			CasDAOUtils.addDeletion(new Column<Long, String>(ans.getUserid().toString(),
					CasDAOFactory.USER_ANSWERS), mut);
			// delete Comments
			cdao.deleteAllCommentsForUUID(ans.getUuid(), mut);
			// delete Revisions
			rdao.deleteAllRevisionsForUUID(ans.getUuid(), mut);
		}

		// delete from answers
		CasDAOUtils.addDeletion(new Column<Long, String>(parentUUID,
				CasDAOFactory.ANSWERS), mut);
		CasDAOUtils.addDeletion(new Column<String, String>(parentUUID,
				CasDAOFactory.ANSWERS_BY_VOTES), mut);
	}

	public ArrayList<Post> readAllPostsForUUID (PostType type, String uuid,
			String sortField, MutableLong page, MutableLong itemcount){

		ArrayList<Post> list = null;

		switch(type){
			case QUESTION:
				list = readQuestionsSortedBy(uuid,
						sortField, page, itemcount, true);
				break;
			case ANSWER:
				list = readAnswersSortedBy(uuid,
						sortField, page, itemcount, true);
				break;
			default: list = new ArrayList<Post>();
		}

		return list;
	}
		
	private void createUpdateOrDeleteTags(String newTags, String oldTags){
		if(StringUtils.isBlank(newTags)) return ;
		Map<String, Integer> newTagIndex = new HashMap<String, Integer>();
		CasTagDAO<Tag, Long> tagDao = new CasTagDAO<Tag, Long>();
		//parse
		for (String ntag : newTags.split(",")) {
			newTagIndex.put(ntag.trim(), 1);
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
			Tag tag = tagDao.readTag(tagEntry.getKey());
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

	public ArrayList<Post> readAllSortedBy(String sortColumnFamilyName,
			MutableLong page, MutableLong itemcount, boolean reverse) {
		return readQuestionsSortedBy(null, sortColumnFamilyName, page, itemcount, reverse);
	}

	private <N> ArrayList<Post> readQuestionsSortedBy(String key, String sortField,
			MutableLong page, MutableLong itemcount, boolean reverse){

		CF<N> colFamily = null;
		N startKey = null;
		Class<N> colNameClass = (Class<N>) Long.class;
		boolean countOnlyColumns = false;
		boolean isPageValid = page != null && page.longValue() > 1;
		//check if the sort order is defined as a column family
		if(StringUtils.isBlank(key)){
			key = CasDAOFactory.DEFAULT_KEY;
			if(sortField.equalsIgnoreCase("timestamp")){
				colFamily = (CF<N>) CasDAOFactory.ALL_QUESTIONS;
				startKey = (N) CasDAOUtils.toLong(page);
			}else if(sortField.equalsIgnoreCase("lastactivity")){
				colFamily = (CF<N>) CasDAOFactory.ALL_QUESTIONS_BY_ACTIVITY;
				startKey = (N) CasDAOUtils.toLong(page);
			}else if(sortField.equalsIgnoreCase("votes")){
				colNameClass = (Class<N>) String.class;
				colFamily = (CF<N>) CasDAOFactory.ALL_QUESTIONS_BY_VOTES;
				if(isPageValid){
					String votes = cdu.getColumn(page.toString(),
							CasDAOFactory.POSTS, sortField);
					if(votes != null){
						startKey = (N) votes.concat(AbstractDAOFactory.SEPARATOR)
								.concat(page.toString());
					}
				}
			}else{
				return new ArrayList<Post>();
			}
		}else{
			countOnlyColumns = true;
			//check if the sort order is defined as a column family
			if(sortField.equalsIgnoreCase("timestamp")){
				colFamily = (CF<N>) CasDAOFactory.QUESTIONS;
				startKey = (N) CasDAOUtils.toLong(page);
			}else if(sortField.equalsIgnoreCase("lastactivity")){
				colFamily = (CF<N>) CasDAOFactory.QUESTIONS_BY_ACTIVITY;
				startKey = (N) CasDAOUtils.toLong(page);
			}else if(sortField.equalsIgnoreCase("votes")){
				colNameClass = (Class<N>) String.class;
				colFamily = (CF<N>) CasDAOFactory.QUESTIONS_BY_VOTES;
				if(isPageValid){
					String votes = cdu.getColumn(page.toString(),
							CasDAOFactory.POSTS, sortField);
					if(votes != null){
						startKey = (N) votes.concat(AbstractDAOFactory.SEPARATOR)
								.concat(page.toString());
					}
				}
			}else{
				return new ArrayList<Post>();
			}
		}

		cdu.setTotalCount(Post.Question.class, key, colFamily, colNameClass,
				itemcount, countOnlyColumns);
		
		return cdu.readAll(Post.class, key, colFamily,
			CasDAOFactory.POSTS, colNameClass, startKey, page, null,
			CasDAOFactory.MAX_ITEMS_PER_PAGE, reverse, false, countOnlyColumns);
	}

	private <N> ArrayList<Post> readAnswersSortedBy(String key, String sortField,
			MutableLong page, MutableLong itemcount, boolean reverse){

		CF<N> colFamily = null;
		N startKey = null;
		Class<N> colNameClass = (Class<N>) Long.class;
		boolean isPageValid = page != null && page.longValue() > 1;
		//check if the sort order is defined as a column family
		if(sortField.equalsIgnoreCase("timestamp")){
			colFamily = (CF<N>) CasDAOFactory.ANSWERS;
			startKey = (N) CasDAOUtils.toLong(page);
		}else if(sortField.equalsIgnoreCase("votes")){
			colNameClass = (Class<N>) String.class;
			colFamily = (CF<N>) CasDAOFactory.ANSWERS_BY_VOTES;
			if(isPageValid){
				String votes = cdu.getColumn(page.toString(),
							CasDAOFactory.POSTS, sortField);
				if(votes != null){
					startKey = (N) votes.concat(AbstractDAOFactory.SEPARATOR)
							.concat(page.toString());
				}
			}
		}else{
			return new ArrayList<Post>();
		}

		cdu.setTotalCount(Post.Answer.class, key, colFamily, colNameClass,
				itemcount, true);

		return cdu.readAll(Post.class, key,	colFamily,
			CasDAOFactory.POSTS, colNameClass, startKey, page, null,
			CasDAOFactory.MAX_ITEMS_PER_PAGE, reverse, false, false);
	}

	public <N> ArrayList<Post> readFeedbackSortedBy(String sortField,
			MutableLong page, MutableLong itemcount, boolean reverse){

		CF<N> colFamily = null;
		N startKey = null;
		Class<N> colNameClass = (Class<N>) Long.class;
		boolean isPageValid = page != null && page.longValue() > 1;
		//check if the sort order is defined as a column family
		if(sortField.equalsIgnoreCase("timestamp")){
			colFamily = (CF<N>) CasDAOFactory.FEEDBACK;
			startKey = (N) CasDAOUtils.toLong(page);
		}else if(sortField.equalsIgnoreCase("lastactivity")){
			colFamily = (CF<N>) CasDAOFactory.FEEDBACK_BY_ACTIVITY;
			startKey = (N) CasDAOUtils.toLong(page);
		}else if(sortField.equalsIgnoreCase("votes")){
			colNameClass = (Class<N>) String.class;
			colFamily = (CF<N>) CasDAOFactory.FEEDBACK_BY_VOTES;
			if(isPageValid){
				String votes = cdu.getColumn(page.toString(),
							CasDAOFactory.POSTS, sortField);
				if(votes != null){
					startKey = (N) votes.concat(AbstractDAOFactory.SEPARATOR)
							.concat(page.toString());
				}
			}
		}else{
			return new ArrayList<Post>();
		}

		cdu.setTotalCount(Post.Feedback.class, CasDAOFactory.DEFAULT_KEY, colFamily,
				colNameClass, itemcount, true);

		return cdu.readAll(Post.class, CasDAOFactory.DEFAULT_KEY, colFamily,
			CasDAOFactory.POSTS, colNameClass, startKey, page, null,
			CasDAOFactory.MAX_ITEMS_PER_PAGE, reverse, false, false);
	}

	public ArrayList<Post> readAllForKeys (ArrayList<String> keys) {
		return cdu.readAll(Post.class, keys, CasDAOFactory.POSTS);
	}

	public boolean updateAndCreateRevision (Post transientPost, Post originalPost){
		//only update and create revision if something has changed
		if(transientPost.equals(originalPost)) return false;

		transientPost.updateLastActivity();
		transientPost.setRevisionuuid(UUID.randomUUID().toString());
		update(transientPost);

		//create a new revision
		Revision.getRevisionDao().create(Revision.createRevisionFromPost(transientPost, false));

		//add/remove tags to/from tags table
		createUpdateOrDeleteTags(transientPost.getTags(), originalPost.getTags());
		return true;
	}

	public void readAllCommentsForPosts (ArrayList<Post> list, int maxPerPage) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		ArrayList<String> uuids = new ArrayList<String>();

		for (int i = 0; i < list.size(); i++) {
			Post post = list.get(i);
			uuids.add(post.getUuid());
			map.put(post.getUuid(), i);
		}
		
		Serializer<String> strser = CasDAOUtils.getSerializer(String.class);

		MultigetSliceQuery<String, Long, String> q =
					HFactory.createMultigetSliceQuery(cdu.getKeyspace(),
					strser, CasDAOUtils.getSerializer(Long.class), strser);

		q.setKeys(uuids);
		q.setColumnFamily(CasDAOFactory.COMMENTS_PARENTUUIDS.getName());
		q.setRange(null, null, true, CasDAOFactory.MAX_ITEMS_PER_PAGE + 1);

		try{
			for (Row<String, Long, String> row : q.execute().get()) {
				ArrayList<String> keyz = new ArrayList<String> ();
				for (HColumn<Long, String> col : row.getColumnSlice().getColumns()){
					keyz.add(col.getName().toString());
				}

				ArrayList<Comment> comments = cdu.readAll(Comment.class,
						keyz, CasDAOFactory.COMMENTS);

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

}
