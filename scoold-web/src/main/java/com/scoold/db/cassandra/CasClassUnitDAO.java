package com.scoold.db.cassandra;

import com.scoold.core.Classunit;
import com.scoold.core.Media;
import com.scoold.core.Post;
import com.scoold.core.User;
import com.scoold.db.AbstractClassUnitDAO;
import com.scoold.db.cassandra.CasDAOFactory.CF;
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
public final class CasClassUnitDAO<T, PK> extends AbstractClassUnitDAO<Classunit, Long>{

    private static final Logger logger = Logger.getLogger(CasClassUnitDAO.class.getName());
	private CasDAOUtils cdu = new CasDAOUtils();
	
    public CasClassUnitDAO () {
	}

    public Classunit read(Long id) {
		return cdu.read(Classunit.class, id.toString(), CasDAOFactory.CLASSES);
    }

    public Classunit read(String uuid) {
		ArrayList<Classunit> classunit = cdu.readAll(Classunit.class, uuid, 
			CasDAOFactory.CLASSES_UUIDS, CasDAOFactory.CLASSES, String.class,
			null, null, null, 1, true, false, false);

		if(classunit == null || classunit.isEmpty()) return null;

		return classunit.get(0);
    }

    public Long create(Classunit newClassUnit) {
		Long id = cdu.getNewId();
		newClassUnit.setId(id);
		newClassUnit.setUuid(CasDAOUtils.getUUID());
		
		// attach a new clean blackboard to class
		Post bb = new Post();
		bb.setPostType(Post.PostType.BLACKBOARD);		
		bb.setTitle("Blackboard");
		bb.setBody(" ");
		bb.setParentuuid(newClassUnit.getUuid());

		CasPostDAO<Post, Long> pdao = new CasPostDAO<Post, Long>();
		Mutator<String> mut = CasDAOUtils.createMutator();

		Long bbid = pdao.create(bb);
		newClassUnit.setBlackboardid(bbid);

		cdu.create(newClassUnit, CasDAOFactory.CLASSES, mut);

		CasDAOUtils.addInsertion(new Column<String, String>(newClassUnit.getUuid(),
				CasDAOFactory.CLASSES_UUIDS, id.toString(), id.toString()), mut);
		CasDAOUtils.addInsertion(new Column<Long, String>(newClassUnit.getSchoolid().toString(),
				CasDAOFactory.SCHOOL_CLASSES, id, id.toString()), mut);

		// auto add to my classes
		CasDAOUtils.addInsertions(getClassLinkColumnsWithChecks(newClassUnit.getUserid(),
				newClassUnit), mut);

		cdu.addTimesortColumn(null, id,	CasDAOFactory.CLASSES_BY_TIMESTAMP, id, null, mut);

		mut.execute();
		
		newClassUnit.index();

		return id;
    }

    public void update(Classunit transientClassUnit) {
		cdu.update(transientClassUnit, CasDAOFactory.CLASSES);
		transientClassUnit.reindex();
    }

    public void delete(Classunit persistentClassUnit) {
		Mutator<String> mut = CasDAOUtils.createMutator();
		deleteClass(persistentClassUnit, mut);
		mut.execute();
    }

	protected void deleteClass(Classunit persistentClassUnit, Mutator<String> mut){
		String uuid = persistentClassUnit.getUuid();
		Long id = persistentClassUnit.getId();

		CasPostDAO<Post, Long> pdao = new CasPostDAO<Post, Long>();
		Post bb = pdao.read(persistentClassUnit.getBlackboardid());
		if(bb != null) pdao.delete(bb);

		cdu.delete(persistentClassUnit, CasDAOFactory.CLASSES, mut);

		CasDAOUtils.addDeletion(new Column(persistentClassUnit.getSchoolid().toString(),
				CasDAOFactory.SCHOOL_CLASSES, id, null), mut);

		cdu.removeTimesortColumn(null, CasDAOFactory.CLASSES_BY_TIMESTAMP, id, mut);

		// remove any references of this class in profile pages
		List<HColumn<Long, String>> userids = cdu.readRow(id.toString(),
				CasDAOFactory.CLASS_USERS, Long.class, null, null, null,
				CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<Long, String> hColumn : userids) {
			CasDAOUtils.addDeletion(new Column(hColumn.getName().toString(),
					CasDAOFactory.USER_CLASSES, id, null), mut);
		}

		CasDAOUtils.addDeletion(new Column(id.toString(), CasDAOFactory.CLASS_USERS), mut);
		CasDAOUtils.addDeletion(new Column(uuid, CasDAOFactory.CLASSES_UUIDS), mut);

		// delete all media for class
		CasMediaDAO<Media, Long> mdao = new CasMediaDAO<Media, Long>();
		mdao.deleteAllMediaForUUID(uuid, mut);

		persistentClassUnit.unindex();
	}

    public ArrayList<User> readAllUsersForClassUnit (Long classid, MutableLong page,
			MutableLong itemcount){
		return cdu.readAll(User.class, classid.toString(),
			CasDAOFactory.CLASS_USERS, CasDAOFactory.USERS, Long.class,
			CasDAOUtils.toLong(page), page, itemcount,
			CasDAOFactory.MAX_ITEMS_PER_PAGE, true, false, true);
    }

	public ArrayList<Classunit> readAllSortedBy (String sortColumnFamilyName,
			MutableLong page, MutableLong itemcount, boolean reverse){

		CF<Long> colFamily = null;
		//check if the sort order is defined as a column family
		if(sortColumnFamilyName.equalsIgnoreCase("timestamp")){
			colFamily = CasDAOFactory.CLASSES_BY_TIMESTAMP;
		}else{
			return new ArrayList<Classunit>();
		}
		
		return cdu.readAll(Classunit.class, CasDAOFactory.DEFAULT_KEY, 
			colFamily, CasDAOFactory.CLASSES, Long.class, CasDAOUtils.toLong(page),
			page, itemcount, CasDAOFactory.MAX_ITEMS_PER_PAGE, reverse, false, false);
	}

	public boolean createUserClassLink (Long userid, Classunit klass){
		Mutator<String> mut = CasDAOUtils.createMutator();
		List<Column> list = getClassLinkColumnsWithChecks(userid, klass);
		CasDAOUtils.addInsertions(list, mut);
		mut.execute();

		return !list.isEmpty();
    }

	private List<Column> getClassLinkColumnsWithChecks(Long userid, Classunit klass){
		ArrayList<Column> list = new ArrayList<Column>();
		if(klass.getId() != null && !isLinkedToUser(klass.getId(), userid)){
			int count = cdu.countColumns(userid.toString(),
					CasDAOFactory.USER_CLASSES, Long.class);

			if(count < CasDAOFactory.MAX_CLASSES_PER_USER)	{
				list.addAll(getClassLinkColumnsOnly(userid, klass.getId()));
				list.addAll(CasSchoolDAO.getSchoolLinkColumns(userid,
						klass.getSchoolid(), 0, 0));
			}
		}

		return list;
	}

	private List<Column> getClassLinkColumnsOnly(Long userid, Long classid){
		ArrayList<Column> list = new ArrayList<Column>();
		list.add(new Column<Long, String>(userid.toString(),
				CasDAOFactory.USER_CLASSES, classid, classid.toString()));
		list.add(new Column<Long, String>(classid.toString(),
				CasDAOFactory.CLASS_USERS, userid, userid.toString()));
		return list;
	}

    public void deleteUserClassLink (Long userid, Long classid){
		ArrayList<Column> list = new ArrayList<Column>();

		list.add(new Column<Long, String>(userid.toString(),
				CasDAOFactory.USER_CLASSES, classid, classid.toString()));
		list.add(new Column<Long, String>(classid.toString(),
				CasDAOFactory.CLASS_USERS, userid, userid.toString()));
		CasDAOUtils.batchRemove(list);
    }

	public boolean isLinkedToUser (Long classid, Long userid) {
		return cdu.existsColumn(classid.toString(),
			CasDAOFactory.CLASS_USERS, userid);
	}

	public ArrayList<Classunit> readLatestClasses (int howMany) {
		return cdu.readAll(Classunit.class, CasDAOFactory.DEFAULT_KEY,  
			CasDAOFactory.CLASSES_BY_TIMESTAMP,CasDAOFactory.CLASSES, Long.class,
			null, null, null, howMany, true, false, false);
	}

	public boolean classExists (Long classid) {
		return cdu.existsColumn(classid.toString(),
				CasDAOFactory.CLASSES, CasDAOFactory.CN_ID);
	}

	public boolean mergeClasses (Long primaryClassid, Long duplicateClassid){
		Classunit primaryClass = cdu.read(Classunit.class,
						primaryClassid.toString(), CasDAOFactory.CLASSES);
		Classunit duplicateClass = cdu.read(Classunit.class,
						duplicateClassid.toString(), CasDAOFactory.CLASSES);

		String primaryUuid = primaryClass.getUuid();
		String duplicateUuid = duplicateClass.getUuid();

		if(primaryClass == null || duplicateClass == null) return false;
		else if(!duplicateClass.getSchoolid().equals(primaryClass.getSchoolid())) return false;

		Mutator<String> mut = CasDAOUtils.createMutator();
		// STEP 1:
		// Move every user to the primary class
		List<HColumn<Long, String>> userids = cdu.readRow(duplicateClassid.toString(),
				CasDAOFactory.CLASS_USERS, Long.class, null, null, null,
				CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<Long, String> hColumn : userids) {
			//create new user-class link
			CasDAOUtils.addInsertions(getClassLinkColumnsOnly(hColumn.getName(),
					primaryClassid), mut);
		}

		// STEP 2:
		// move media to primary class
		List<HColumn<Long, String>> photoids = cdu.readRow(duplicateUuid,
				CasDAOFactory.PHOTOS, Long.class, null, null, null,
				CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<Long, String> hColumn : photoids) {
			CasDAOUtils.addInsertion(new Column<Long, String>(primaryUuid,
					CasDAOFactory.PHOTOS, hColumn.getName(), hColumn.getValue()), mut);
		}

		List<HColumn<Long, String>> drawerids = cdu.readRow(duplicateUuid,
				CasDAOFactory.DRAWER, Long.class, null, null, null,
				CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<Long, String> hColumn : drawerids) {
			CasDAOUtils.addInsertion(new Column<Long, String>(primaryUuid,
					CasDAOFactory.DRAWER, hColumn.getName(), hColumn.getValue()), mut);
		}

		// STEP 3:
		// delete duplicate
		cdu.delete(duplicateClass, CasDAOFactory.CLASSES, mut);

		mut.execute();

		return true;
	}

	public ArrayList<Classunit> readAllForKeys (ArrayList<String> keys) {
		return cdu.readAll(Classunit.class, keys,
				CasDAOFactory.CLASSES);
	}

	public int countUsersForClassUnit(Long classid) {
		return cdu.countColumns(classid.toString(),
				CasDAOFactory.CLASS_USERS, Long.class);
	}

}

