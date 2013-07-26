/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.db.dynamodb;

import com.erudika.scoold.db.cassandra.*;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Revision;
import com.erudika.scoold.db.AbstractRevisionDAO;
import com.erudika.scoold.db.cassandra.CasDAOFactory.Column;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 *
 * @author alexb
 */
final class DynRevisionDAO<T> extends AbstractRevisionDAO<Revision> {

	
    public DynRevisionDAO() {
    }

	public Revision read(String id) {
//		return cdu.read(Revision.class, id);
		return null;
	}

	public String create(Revision newInstance) {
//		Mutator<String> mut = cdu.createMutator();
//		String id = cdu.create(newInstance, mut);
//
//		if(id != null){
//			cdu.addInsertion(new Column<String, String>(newInstance.getParentid(),
//				CasDAOFactory.REVISIONS_PARENTS, id, id), mut);
//		}
//		mut.execute();
//		return id;
		return null;
	}

	public void update(Revision transientObject) {
//		cdu.update(transientObject);
	}

	public void delete(Revision persistentObject){
//		Mutator<String> mut = cdu.createMutator();
//		cdu.delete(persistentObject, mut);
//
//		cdu.removeColumn(persistentObject.getParentid(),
//				CasDAOFactory.REVISIONS_PARENTS, persistentObject.getId());
//		
//		mut.execute();
	}

	public ArrayList<Revision> readAllRevisionsForPost (String parentid, MutableLong page,
			MutableLong itemcount) {

//		ArrayList<Revision> revlist = cdu.readAll(Revision.class, null, parentid,
//				CasDAOFactory.REVISIONS_PARENTS, String.class,
//				Utils.toLong(page).toString(), page, itemcount,
//				Utils.MAX_ITEMS_PER_PAGE + 1, true, false, true);
//
//		if(revlist != null && !revlist.isEmpty()){
//			Revision last = revlist.get(revlist.size() - 1);
//			page.setValue(NumberUtils.toLong(last.getId()));
//		}
//
//		return revlist;
		return null;
	}
	
	public void restoreRevision (String revisionid, Post transientPost){
//		if(transientPost == null) return;
//		//read revision
//		Revision rev = read(revisionid);
//		if(rev != null && transientPost != null){
//			//copy rev data to post
//			transientPost.setTitle(rev.getTitle());
//			transientPost.setBody(rev.getBody());
//			transientPost.setTags(rev.getTags());
//
//			transientPost.updateLastActivity();
//			transientPost.setRevisionid(rev.getId());
//			//update post
//			cdu.update(transientPost);
//		}
	}

	public void deleteAllRevisionsForID(String parentid) {
//		Mutator<String> mut = cdu.createMutator();
//		deleteAllRevisionsForID(parentid, mut);
//		mut.execute();
	}

	protected void deleteAllRevisionsForID(String parentid, Mutator<String> mut) {
//		List<HColumn<String, String>> keys = cdu.readRow(parentid,
//				CasDAOFactory.REVISIONS_PARENTS, String.class,
//				null, null, null, Utils.DEFAULT_LIMIT, false);
//
//		for (HColumn<String, String> hColumn : keys) {
//			cdu.addDeletion(new Column<String, String>(hColumn.getName(),
//					CasDAOFactory.OBJECTS), mut);
//		}
//
//		cdu.addDeletion(new Column<String, String>(parentid,
//				CasDAOFactory.REVISIONS_PARENTS), mut);
	}

}

