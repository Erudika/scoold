/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db.cassandra;

import com.scoold.core.Comment;
import com.scoold.db.AbstractCommentDAO;
import com.scoold.db.cassandra.CasDAOFactory.Column;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public class CasCommentDAO<T, PK> extends AbstractCommentDAO<Comment, Long> {

    private static final Logger logger = Logger.getLogger(CasCommentDAO.class.getName());
	private CasDAOUtils cdu = new CasDAOUtils();
	
	public CasCommentDAO(){
	}

	public Comment read (Long id) {
		return cdu.read(Comment.class, id.toString(),
				CasDAOFactory.COMMENTS);
	}

	public Comment read (String uuid) {
		ArrayList<Comment> comment = cdu.readAll(Comment.class, uuid, 
				CasDAOFactory.COMMENTS_UUIDS, CasDAOFactory.COMMENTS, String.class,
				null, null, null, 1, false, false, false);

		if(comment == null || comment.isEmpty()) return null;

		return comment.get(0);
	}

	public Long create (Comment newInstance) {
		if(newInstance.getUserid() == null ||
				StringUtils.isBlank(newInstance.getComment()) ||
				StringUtils.isBlank(newInstance.getParentuuid()))
			return null;

		Mutator<String> mut = CasDAOUtils.createMutator();
		String key = newInstance.getParentuuid();
		int count = cdu.countColumns(key,
				CasDAOFactory.COMMENTS_PARENTUUIDS, Long.class);

		if(count > CasDAOFactory.MAX_COMMENTS_PER_UUID){
			deleteLastComment(key);
		}

		Long id = cdu.create(newInstance, CasDAOFactory.COMMENTS, mut);

		if(id != null){
			CasDAOUtils.addInsertion(new Column<String, String>(newInstance.getUuid(), 
					CasDAOFactory.COMMENTS_UUIDS, id.toString(), id.toString()), mut);
			CasDAOUtils.addInsertion(new Column<Long, String>(key,
					CasDAOFactory.COMMENTS_PARENTUUIDS, id, id.toString()), mut);
		}

		mut.execute();
		
		return id;
	}

	public void update (Comment transientObject) {
		cdu.update(transientObject, CasDAOFactory.COMMENTS);
	}

	public void delete (Comment persistentObject) {
		if(persistentObject.getParentuuid() == null ||
				persistentObject.getId() == null) return;

		Mutator<String> mut = CasDAOUtils.createMutator();

		// delete the comment object
		cdu.delete(persistentObject, CasDAOFactory.COMMENTS, mut);

		CasDAOUtils.addDeletion(new Column<Long, String>(persistentObject.getParentuuid(),
				CasDAOFactory.COMMENTS_PARENTUUIDS, persistentObject.getId(), null), mut);
		cdu.deleteRow(persistentObject.getUuid(), CasDAOFactory.COMMENTS_UUIDS, mut);

		mut.execute();
	}

	private void deleteLastComment(String parentUUID){
		// remove last (oldest) comment
		HColumn<Long, String> last = cdu.getLastColumn(parentUUID,
				CasDAOFactory.COMMENTS_PARENTUUIDS, Long.class, false);
		
		if(last != null){
			Long id = last.getName();
			delete(new Comment(id));
		}
	}

	public ArrayList<Comment> readAllCommentsForUUID (String parentUUID,
			MutableLong page, MutableLong itemcount){
		return cdu.readAll(Comment.class, parentUUID, 
				CasDAOFactory.COMMENTS_PARENTUUIDS, CasDAOFactory.COMMENTS,
				Long.class, CasDAOUtils.toLong(page), page, itemcount,
				CasDAOFactory.MAX_ITEMS_PER_PAGE, true, false, true);
	}

	public ArrayList<Comment> readAllCommentsForUUID (String parentUUID){
		return readAllCommentsForUUID(parentUUID, null, null);
	}

	public ArrayList<Comment> readAllSortedBy(String sortColumnFamilyName, 
		MutableLong page, MutableLong itemcount, boolean reverse) {
		throw new UnsupportedOperationException("not supported");
//		return cdu.readSlice(Comment.class, CasDAOFactory.DEFAULT_KEY,
//				sortColumnFamilyName,
//				CasDAOFactory.COMMENTS,
//				page, itemcount, reverse, false);
	}

	public void deleteAllCommentsForUUID(String parentUUID) {
		Mutator<String> mut = CasDAOUtils.createMutator();
		deleteAllCommentsForUUID(parentUUID, mut);
		mut.execute();
	}

	protected void deleteAllCommentsForUUID(String parentUUID, Mutator<String> mut) {
		List<HColumn<Long, String>> keys = cdu.readRow(parentUUID,
				CasDAOFactory.COMMENTS_PARENTUUIDS, Long.class,
				null, null, null, CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<Long, String> hColumn : keys) {
			// delete all comments
			CasDAOUtils.addDeletion(new Column<String, String>(hColumn.getName().toString(),
					CasDAOFactory.COMMENTS), mut);			
		}
		// delete from parentuuids
		CasDAOUtils.addDeletion(new Column<Long, String>(parentUUID,
				CasDAOFactory.COMMENTS_PARENTUUIDS), mut);
	}

}
