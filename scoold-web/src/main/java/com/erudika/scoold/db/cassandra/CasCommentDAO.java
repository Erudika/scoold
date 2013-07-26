/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.db.cassandra;

import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.db.AbstractCommentDAO;
import com.erudika.scoold.db.cassandra.CasDAOFactory.Column;
import com.erudika.scoold.util.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 *
 * @author alexb
 */
final class CasCommentDAO<T> extends AbstractCommentDAO<Comment> {

    private static final Logger logger = Logger.getLogger(CasCommentDAO.class.getName());
	private CasDAOUtils cdu = new CasDAOUtils(CasDAOFactory.CASSANDRA_PORT);
	
	public CasCommentDAO(){
	}

	public Comment read (String id) {
		return cdu.read(Comment.class, id);
	}

	public String create (Comment newInstance) {
		if(newInstance.getCreatorid() == null || StringUtils.isBlank(newInstance.getComment()) ||
				newInstance.getParentid() == null)
			return null;

		Mutator<String> mut = cdu.createMutator();
		String parentid = newInstance.getParentid();
		
		int count = cdu.countColumns(parentid, CasDAOFactory.COMMENTS_PARENTS, String.class);
		if(count > Constants.MAX_COMMENTS_PER_ID) return null;
		
		String id = cdu.create(newInstance, mut);

		if(id != null){
			cdu.addInsertion(new Column<String, String>(parentid,
					CasDAOFactory.COMMENTS_PARENTS, id, id), mut);
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

		cdu.addDeletion(new Column<String, String>(persistentObject.getParentid(),
				CasDAOFactory.COMMENTS_PARENTS, persistentObject.getId(), null), mut);

		mut.execute();
	}

	public ArrayList<Comment> readAllCommentsForID (String parentid,
			MutableLong page, MutableLong itemcount){
		return cdu.readAll(Comment.class, null, parentid,
				CasDAOFactory.COMMENTS_PARENTS, 
				String.class, Utils.toLong(page).toString(), page, itemcount,
				Utils.MAX_ITEMS_PER_PAGE, true, false, true);
	}

	public ArrayList<Comment> readAllCommentsForID (String parentid){
		return readAllCommentsForID(parentid, null, null);
	}

	public void deleteAllCommentsForID(String parentid) {
		Mutator<String> mut = cdu.createMutator();
		deleteAllCommentsForID(parentid, mut);
		mut.execute();
	}

	protected void deleteAllCommentsForID(String parentid, Mutator<String> mut) {
		List<HColumn<String, String>> keys = cdu.readRow(parentid,
				CasDAOFactory.COMMENTS_PARENTS, String.class,
				null, null, null, Utils.DEFAULT_LIMIT, false);

		for (HColumn<String, String> hColumn : keys) {
			// delete all comments
			cdu.addDeletion(new Column<String, String>(hColumn.getName(),
					CasDAOFactory.OBJECTS), mut);			
		}
		// delete from parentids
		cdu.addDeletion(new Column<String, String>(parentid,
				CasDAOFactory.COMMENTS_PARENTS), mut);
	}

}
