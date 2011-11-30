/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db.cassandra;

import com.scoold.core.Tag;
import com.scoold.db.AbstractTagDAO;
import java.util.ArrayList;
import java.util.logging.Logger;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public class CasTagDAO<T, PK> extends AbstractTagDAO<Tag, Long> {

	private static final Logger logger = Logger.getLogger(CasTagDAO.class.getName());
	private CasDAOUtils cdu = new CasDAOUtils();
	
    public CasTagDAO () {
    }

	public Tag read (Long id) {
		throw new UnsupportedOperationException("not supported.");
	}

	public Tag read (String uuid) {
		throw new UnsupportedOperationException("not supported.");
	}

	public Tag readTag (String tag) {
		return cdu.read(Tag.class, tag, CasDAOFactory.TAGS);
	}

	public Long create (Tag newInstance) {
		if(StringUtils.isBlank(newInstance.getTag())) return null;
		Mutator<String> mut = cdu.createMutator();
		Long id =  cdu.create(newInstance.getTag(), newInstance, CasDAOFactory.TAGS, mut);
		mut.execute();
		if(id != null) newInstance.index();
		return id;
	}

	public void update (Tag transientObject) {
		if(StringUtils.isBlank(transientObject.getTag())) return;
		cdu.putColumn(transientObject.getTag(), 
				CasDAOFactory.TAGS, "count", transientObject.getCount().toString());
	}

	public void delete (Tag persistentObject) {
		if(StringUtils.isBlank(persistentObject.getTag())) return;
		Mutator<String> mut = cdu.createMutator();
		cdu.delete(persistentObject.getTag(), persistentObject, CasDAOFactory.TAGS, mut);
		mut.execute();
		persistentObject.unindex();
	}

	public ArrayList<Tag> readAllSortedBy(String sortColumnFamilyName, 
			MutableLong page, MutableLong itemcount, boolean reverse) {
		throw new UnsupportedOperationException("not supported.");
	}

	public ArrayList<Tag> readAllForKeys (ArrayList<String> keys) {
		throw new UnsupportedOperationException("not supported.");
//		return cdu.executeOne(new Command<ArrayList<Tag>>() {
//			public ArrayList<Tag> execute Keyspace ks) {
//				return cdu.readAll(Tag.class, keys,
//						CasDAOFactory.TAGS);
//			}
//		});
	}

}
