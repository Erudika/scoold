/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.db.cassandra;

import com.erudika.scoold.core.Tag;
import com.erudika.scoold.db.AbstractTagDAO;
import java.util.logging.Logger;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author alexb
 */
final class CasTagDAO<T> extends AbstractTagDAO<Tag> {

	private static final Logger logger = Logger.getLogger(CasTagDAO.class.getName());
	private CasDAOUtils cdu = new CasDAOUtils(CasDAOFactory.CASSANDRA_PORT);
	
    public CasTagDAO () {
    }

//	public Tag read (String id) {
//		throw new UnsupportedOperationException("not supported.");
//	}

	public Tag read (String tag) {
		return cdu.read(Tag.class, tag);
	}

	public String create (Tag newInstance) {
		if(StringUtils.isBlank(newInstance.getTag())) return null;
		Mutator<String> mut = cdu.createMutator();
		String id = cdu.create(newInstance.getTag(), newInstance, mut);
		mut.execute();
		return id;
	}

	public void update (Tag transientObject) {
		if(StringUtils.isBlank(transientObject.getTag())) return;
		cdu.putColumn(transientObject.getTag(), CasDAOFactory.OBJECTS, "count", 
				transientObject.getCount().toString());
	}

	public void delete (Tag persistentObject) {
		if(StringUtils.isBlank(persistentObject.getTag())) return;
		Mutator<String> mut = cdu.createMutator();
		cdu.delete(persistentObject.getTag(), persistentObject, mut);
		mut.execute();
	}
}
