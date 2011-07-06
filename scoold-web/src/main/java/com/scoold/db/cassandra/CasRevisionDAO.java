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
public class CasRevisionDAO<T, PK> extends AbstractRevisionDAO<Revision, Long> {

	private static final Logger logger = Logger.getLogger(CasRevisionDAO.class.getName());
	private CasDAOUtils cdu = new CasDAOUtils();
	
    public CasRevisionDAO() {
    }

	public Revision read(Long id) {
		return cdu.read(Revision.class, id.toString(), CasDAOFactory.REVISIONS);
	}

	public Revision read(String uuid) {
		ArrayList<Revision> revision = cdu.readAll(Revision.class, uuid,
				CasDAOFactory.REVISIONS_UUIDS, CasDAOFactory.REVISIONS, String.class,
				null, null, null, 1, true, false, false);

		if(revision == null || revision.isEmpty()) return null;

		return revision.get(0);
	}

	public Long create(Revision newInstance) {
		Mutator<String> mut = CasDAOUtils.createMutator();
		Long id = cdu.create(newInstance, CasDAOFactory.REVISIONS, mut);

		if(id != null){
			CasDAOUtils.addInsertion(new Column<String, String>(newInstance.getUuid(),
				CasDAOFactory.REVISIONS_UUIDS, id.toString(), id.toString()), mut);
			CasDAOUtils.addInsertion(new Column<Long, String>(newInstance.getParentuuid(),
				CasDAOFactory.REVISIONS_PARENTUUIDS, id, id.toString()), mut);
		}
		mut.execute();
		return id;
	}

	public void update(Revision transientObject) {
		cdu.update(transientObject, CasDAOFactory.REVISIONS);
	}

	public void delete(Revision persistentObject){
		Mutator<String> mut = CasDAOUtils.createMutator();
		cdu.delete(persistentObject, CasDAOFactory.REVISIONS, mut);

		cdu.removeColumn(persistentObject.getParentuuid(),
				CasDAOFactory.REVISIONS_PARENTUUIDS, persistentObject.getId());
		cdu.deleteRow(persistentObject.getUuid(), CasDAOFactory.REVISIONS_UUIDS, mut);
		
		mut.execute();
	}

	public ArrayList<Revision> readAllRevisionsForPost (String parentUUID, MutableLong page,
			MutableLong itemcount) {

		ArrayList<Revision> revlist = cdu.readAll(Revision.class, parentUUID,
				CasDAOFactory.REVISIONS_PARENTUUIDS, CasDAOFactory.REVISIONS, Long.class,
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
			transientPost.setRevisionuuid(rev.getUuid());
			//update post
			cdu.update(transientPost, CasDAOFactory.POSTS);
		}
	}

	public ArrayList<Revision> readAllSortedBy(String sortColumnFamilyName,
			MutableLong page, MutableLong itemcount, boolean desc) {
		throw new UnsupportedOperationException("not supported.");
	}

	public void deleteAllRevisionsForUUID(String parentUUID) {
		Mutator<String> mut = CasDAOUtils.createMutator();
		deleteAllRevisionsForUUID(parentUUID, mut);
		mut.execute();
	}

	protected void deleteAllRevisionsForUUID(String parentUUID, Mutator<String> mut) {
		List<HColumn<Long, String>> keys = cdu.readRow(parentUUID,
				CasDAOFactory.REVISIONS_PARENTUUIDS, Long.class,
				null, null, null, CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<Long, String> hColumn : keys) {
			CasDAOUtils.addDeletion(new Column<String, String>(hColumn.getName().toString(),
					CasDAOFactory.REVISIONS), mut);
		}

		CasDAOUtils.addDeletion(new Column<Long, String>(parentUUID,
				CasDAOFactory.REVISIONS_PARENTUUIDS), mut);
	}

}

