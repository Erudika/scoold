/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.db.dynamodb;

import com.erudika.scoold.core.Tag;
import com.erudika.scoold.db.AbstractTagDAO;

/**
 *
 * @author alexb
 */
final class DynTagDAO<T> extends AbstractTagDAO<Tag> {

	
    public DynTagDAO () {
    }

//	public Tag read (String id) {
//		throw new UnsupportedOperationException("not supported.");
//	}

	public Tag read (String tag) {
//		return cdu.read(Tag.class, tag);
		return null;
	}

	public String create (Tag newInstance) {
//		if(StringUtils.isBlank(newInstance.getTag())) return null;
//		Mutator<String> mut = cdu.createMutator();
//		String id = cdu.create(newInstance.getTag(), newInstance, mut);
//		mut.execute();
//		return id;
		return null;
	}

	public void update (Tag transientObject) {
//		if(StringUtils.isBlank(transientObject.getTag())) return;
//		cdu.putColumn(transientObject.getTag(), CasDAOFactory.OBJECTS, "count", 
//				transientObject.getCount().toString());
	}

	public void delete (Tag persistentObject) {
//		if(StringUtils.isBlank(persistentObject.getTag())) return;
//		Mutator<String> mut = cdu.createMutator();
//		cdu.delete(persistentObject.getTag(), persistentObject, mut);
//		mut.execute();
	}
}
