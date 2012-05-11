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
final class CasCommentDAO<T, PK> extends AbstractCommentDAO<Comment, Long> {

    private static final Logger logger = Logger.getLogger(CasCommentDAO.class.getName());
	private CasDAOUtils cdu = (CasDAOUtils) CasDAOFactory.getInstance().getDAOUtils();
	
	public CasCommentDAO(){
	}

	public Comment read (Long id) {
		return cdu.read(Comment.class, id.toString());
	}

	public Long create (Comment newInstance) {
		if(newInstance.getUserid() == null || StringUtils.isBlank(newInstance.getComment()) ||
				newInstance.getParentid() == null)
			return null;

		Mutator<String> mut = cdu.createMutator();
		Long parentid = newInstance.getParentid();
		
		int count = cdu.countColumns(parentid.toString(), CasDAOFactory.COMMENTS_PARENTS, Long.class);
		if(count > CasDAOFactory.MAX_COMMENTS_PER_ID) return 0L;
		
		Long id = cdu.create(newInstance, mut);

		if(id != null){
			cdu.addInsertion(new Column<Long, String>(parentid.toString(),
					CasDAOFactory.COMMENTS_PARENTS, id, id.toString()), mut);
		}
		mut.execute();
		
		return id;
	}

	public void update (Comment transientObject) {
		cdu.update(transientObject);
	}

	public void delete (Comment persistentObject) {
		if(persistentObject.getParentid() == null ||
				persistentObject.getId() == null) return;

		Mutator<String> mut = cdu.createMutator();

		// delete the comment object
		cdu.delete(persistentObject, mut);

		cdu.addDeletion(new Column<Long, String>(persistentObject.getParentid().toString(),
				CasDAOFactory.COMMENTS_PARENTS, persistentObject.getId(), null), mut);

		mut.execute();
	}

	public ArrayList<Comment> readAllCommentsForID (Long parentid,
			MutableLong page, MutableLong itemcount){
		return cdu.readAll(Comment.class, null, parentid.toString(),
				CasDAOFactory.COMMENTS_PARENTS, 
				Long.class, CasDAOUtils.toLong(page), page, itemcount,
				CasDAOFactory.MAX_ITEMS_PER_PAGE, true, false, true);
	}

	public ArrayList<Comment> readAllCommentsForID (Long parentid){
		return readAllCommentsForID(parentid, null, null);
	}

	public void deleteAllCommentsForID(Long parentid) {
		Mutator<String> mut = cdu.createMutator();
		deleteAllCommentsForID(parentid, mut);
		mut.execute();
	}

	protected void deleteAllCommentsForID(Long parentid, Mutator<String> mut) {
		List<HColumn<Long, String>> keys = cdu.readRow(parentid.toString(),
				CasDAOFactory.COMMENTS_PARENTS, Long.class,
				null, null, null, CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<Long, String> hColumn : keys) {
			// delete all comments
			cdu.addDeletion(new Column<String, String>(hColumn.getName().toString(),
					CasDAOFactory.OBJECTS), mut);			
		}
		// delete from parentids
		cdu.addDeletion(new Column<Long, String>(parentid.toString(),
				CasDAOFactory.COMMENTS_PARENTS), mut);
	}

}
