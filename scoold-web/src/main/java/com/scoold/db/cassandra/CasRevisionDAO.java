/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db.cassandra;

import com.scoold.core.Post;
import com.scoold.core.Revision;
import com.scoold.db.AbstractRevisionDAO;
import com.scoold.db.cassandra.CasDAOFactory.Column;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
final class CasRevisionDAO<T, PK> extends AbstractRevisionDAO<Revision, Long> {

	private static final Logger logger = Logger.getLogger(CasRevisionDAO.class.getName());
	private CasDAOUtils cdu = (CasDAOUtils) CasDAOFactory.getInstance().getDAOUtils();
	
    public CasRevisionDAO() {
    }

	public Revision read(Long id) {
		return cdu.read(Revision.class, id.toString());
	}

	public Long create(Revision newInstance) {
		Mutator<String> mut = cdu.createMutator();
		Long id = cdu.create(newInstance, mut);

		if(id != null){
			cdu.addInsertion(new Column<Long, String>(newInstance.getParentid().toString(),
				CasDAOFactory.REVISIONS_PARENTS, id, id.toString()), mut);
		}
		mut.execute();
		return id;
	}

	public void update(Revision transientObject) {
		cdu.update(transientObject);
	}

	public void delete(Revision persistentObject){
		Mutator<String> mut = cdu.createMutator();
		cdu.delete(persistentObject, mut);

		cdu.removeColumn(persistentObject.getParentid().toString(),
				CasDAOFactory.REVISIONS_PARENTS, persistentObject.getId());
		
		mut.execute();
	}

	public ArrayList<Revision> readAllRevisionsForPost (Long parentid, MutableLong page,
			MutableLong itemcount) {

		ArrayList<Revision> revlist = cdu.readAll(Revision.class, null, parentid.toString(),
				CasDAOFactory.REVISIONS_PARENTS, Long.class,
				CasDAOUtils.toLong(page), page, itemcount,
				CasDAOFactory.MAX_ITEMS_PER_PAGE + 1, true, false, true);

		if(revlist != null && !revlist.isEmpty()){
			Revision last = revlist.get(revlist.size() - 1);
			page.setValue(last.getId());
		}

		return revlist;
	}
	
	public void restoreRevision (Long revisionid, Post transientPost){
		if(transientPost == null) return;
		//read revision
		Revision rev = read(revisionid);
		if(rev != null && transientPost != null){
			//copy rev data to post
			transientPost.setTitle(rev.getTitle());
			transientPost.setBody(rev.getBody());
			transientPost.setTags(rev.getTags());

			transientPost.updateLastActivity();
			transientPost.setRevisionid(rev.getId());
			//update post
			cdu.update(transientPost);
		}
	}

	public void deleteAllRevisionsForID(Long parentid) {
		Mutator<String> mut = cdu.createMutator();
		deleteAllRevisionsForID(parentid, mut);
		mut.execute();
	}

	protected void deleteAllRevisionsForID(Long parentid, Mutator<String> mut) {
		List<HColumn<Long, String>> keys = cdu.readRow(parentid.toString(),
				CasDAOFactory.REVISIONS_PARENTS, Long.class,
				null, null, null, CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<Long, String> hColumn : keys) {
			cdu.addDeletion(new Column<String, String>(hColumn.getName().toString(),
					CasDAOFactory.OBJECTS), mut);
		}

		cdu.addDeletion(new Column<Long, String>(parentid.toString(),
				CasDAOFactory.REVISIONS_PARENTS), mut);
	}

}

