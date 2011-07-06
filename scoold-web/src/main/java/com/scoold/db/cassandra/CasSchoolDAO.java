package com.scoold.db.cassandra;

import com.scoold.core.Classunit;
import com.scoold.core.Media;
import com.scoold.core.School;
import com.scoold.core.User;
import com.scoold.db.AbstractSchoolDAO;
import com.scoold.db.cassandra.CasDAOFactory.CF;
import com.scoold.db.cassandra.CasDAOFactory.Column;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public final class CasSchoolDAO<T, PK> extends AbstractSchoolDAO<School, Long> {

    private static final Logger logger = Logger.getLogger(CasSchoolDAO.class.getName());
	private CasDAOUtils cdu = new CasDAOUtils();
	
	public CasSchoolDAO () {
    }

    public School read(Long id) {
		return cdu.read(School.class, id.toString(), CasDAOFactory.SCHOOLS);
    }

    public School read(String uuid) {
		ArrayList<School> school = cdu.readAll(School.class, uuid,
				CasDAOFactory.SCHOOLS_UUIDS, CasDAOFactory.SCHOOLS, String.class,
				null, null, null, 1, true, false, false);

		if(school == null || school.isEmpty()) return null;

		return school.get(0);
    }

    public Long create(School newSchool) {
		Mutator<String> mut = CasDAOUtils.createMutator();

		Long id =  cdu.create(newSchool, CasDAOFactory.SCHOOLS, mut);
		if(id == null) return null;

		CasDAOUtils.addInsertion(new Column(newSchool.getUuid(),
				CasDAOFactory.SCHOOLS_UUIDS, id.toString(), id.toString()), mut);
		
		cdu.addTimesortColumn(null, id, CasDAOFactory.SCHOOLS_BY_TIMESTAMP, id, null, mut);
		cdu.addNumbersortColumn(null, CasDAOFactory.SCHOOLS_BY_VOTES,
				id, newSchool.getVotes(), newSchool.getOldvotes(), mut);

		mut.execute();

		newSchool.index();

		return id;
    }

    public void update(School transientSchool) {
		Mutator<String> mut = CasDAOUtils.createMutator();
		cdu.update(transientSchool, CasDAOFactory.SCHOOLS, mut);
		cdu.addNumbersortColumn(null, CasDAOFactory.SCHOOLS_BY_VOTES, 
				transientSchool.getId(), transientSchool.getVotes(),
				transientSchool.getOldvotes(), mut);

		mut.execute();
		
		transientSchool.reindex();
    }

    public void delete(School persistentSchool) {
		String uuid = persistentSchool.getUuid();
		Long id = persistentSchool.getId();
		
		Mutator<String> mut = CasDAOUtils.createMutator();
		cdu.delete(persistentSchool, CasDAOFactory.SCHOOLS, mut);

		cdu.removeTimesortColumn(null, CasDAOFactory.SCHOOLS_BY_TIMESTAMP,
			persistentSchool.getId(), mut);

		// remove any references of this school in profile pages
		List<HColumn<Long, String>> userids = cdu.readRow(id.toString(),
				CasDAOFactory.SCHOOL_USERS, Long.class, null, null, null,
				CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<Long, String> hColumn : userids) {
			CasDAOUtils.addDeletion(new Column(hColumn.getName().toString(),
					CasDAOFactory.USER_SCHOOLS, id, null), mut);
		}

		// delete all classes for school
		CasClassUnitDAO<Classunit, Long> cdao = new CasClassUnitDAO<Classunit, Long>();
		for (Classunit klass : readAllClassUnitsForSchool(id, null, null)) {
			cdao.deleteClass(klass, mut);
		}

		cdu.removeNumbersortColumn(null, CasDAOFactory.SCHOOLS_BY_VOTES,
			persistentSchool.getId(), persistentSchool.getVotes(), mut);

		CasDAOUtils.addDeletions(Arrays.asList(new Column[]{
			new Column(id.toString(), CasDAOFactory.SCHOOL_CLASSES),
			new Column(id.toString(), CasDAOFactory.SCHOOL_USERS),
			new Column(uuid, CasDAOFactory.SCHOOLS_UUIDS)
		}), mut);

		// delete all media for school
		CasMediaDAO<Media, Long> mdao = new CasMediaDAO<Media, Long>();
		mdao.deleteAllMediaForUUID(uuid, mut);

		mut.execute();

		persistentSchool.unindex();
    }

	public ArrayList<School> readAllSortedBy(String sortColumnFamilyName,
			MutableLong page, MutableLong itemcount, boolean reverse){
		return readAllSorted(sortColumnFamilyName, page, itemcount, reverse);
	}

	private <N> ArrayList<School> readAllSorted(String sortColumnFamilyName,
			MutableLong page, MutableLong itemcount, boolean reverse){

		CF<N> colFamily = null;
		N startKey = null;
		Class<N> colNameClass = null;
		//check if the sort order is defined as a column family
		if(sortColumnFamilyName.equalsIgnoreCase("timestamp")){
			colNameClass = (Class<N>) Long.class;
			colFamily = (CF<N>) CasDAOFactory.SCHOOLS_BY_TIMESTAMP;
			startKey = (N) CasDAOUtils.toLong(page);
		}else if(sortColumnFamilyName.equalsIgnoreCase("votes")){
			colNameClass = (Class<N>) String.class;
			colFamily = (CF<N>) CasDAOFactory.SCHOOLS_BY_VOTES;
			String votes = cdu.getColumn(page.toString(), CasDAOFactory.SCHOOLS, "votes");
			if(votes != null){
				startKey = (N) votes.concat(CasDAOFactory.SEPARATOR).concat(page.toString());
			}
		}else{
			return new ArrayList<School>();
		}

		return cdu.readAll(School.class, CasDAOFactory.DEFAULT_KEY,
			colFamily, CasDAOFactory.SCHOOLS, colNameClass, startKey, page, itemcount,
			CasDAOFactory.MAX_ITEMS_PER_PAGE, reverse, false, false);

	}

    //get all classes that are part of a school
    public ArrayList<Classunit> readAllClassUnitsForSchool(Long schoolid, 
			MutableLong page, MutableLong itemcount){

		return cdu.readAll(Classunit.class, schoolid.toString(), 
			CasDAOFactory.SCHOOL_CLASSES, CasDAOFactory.CLASSES, Long.class,
			CasDAOUtils.toLong(page), page, itemcount,
			CasDAOFactory.MAX_ITEMS_PER_PAGE, true, false, true);
    }

    public boolean createUserSchoolLink (Long userid, Long id, Integer from, Integer to){
		boolean b = false;
		// if is linked already return false
		if(id != null){
			int count = cdu.countColumns(userid.toString(),
					CasDAOFactory.USER_SCHOOLS, Long.class);

			if(count >= CasDAOFactory.MAX_SCHOOLS_PER_USER) return false;
			// insert into user_schools and school_users
			CasDAOUtils.batchPut(getSchoolLinkColumns(userid, id, from, to));

			b = true;
		}
		return b;
    }

	protected static List<Column> getSchoolLinkColumns(Long userid, Long id,
			Integer from, Integer to){
		int min = 1900; int max = 3000;
		Integer fyear = (from == null || from < min || from > max) ? 0 : from;
		Integer tyear = (to == null || to < min || to > max) ? 0 : to;
		
		// format: [id -> "fromyear=0:toyear=2000"]
		String linkDetails = "fromyear=".concat(fyear.toString()).
				concat(CasDAOFactory.SEPARATOR).
				concat("toyear=").concat(tyear.toString());

		ArrayList<Column> list = new ArrayList<Column>();
		list.add(new Column<Long, String> (userid.toString(),
				CasDAOFactory.USER_SCHOOLS, id, linkDetails));
		list.add(new Column<Long, String> (id.toString(),
				CasDAOFactory.SCHOOL_USERS, userid, userid.toString()));
		return list;
	}

    public void deleteUserSchoolLink (Long userid, School school){
		if(school == null || userid == null) return;

		CasDAOUtils.batchRemove(
			new Column(userid.toString(), CasDAOFactory.USER_SCHOOLS,
				school.getId(), null),
			new Column(school.getId().toString(),
				CasDAOFactory.SCHOOL_USERS, userid, null)
		);
    }

	public boolean isLinkedToUser (Long schoolid, Long userid) {
		return cdu.existsColumn(schoolid.toString(), 
				CasDAOFactory.SCHOOL_USERS, userid);
	}

	public ArrayList<User> readRandomTeachersForSchool(Long schoolid, int howMany) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public ArrayList<User> readAllUsersForSchool(Long schoolid, MutableLong page,
			MutableLong itemcount) {
		if(schoolid == null) return new ArrayList<User>();

		return cdu.readAll(User.class, schoolid.toString(), 
			CasDAOFactory.SCHOOL_USERS, CasDAOFactory.USERS, Long.class,
			CasDAOUtils.toLong(page), page, itemcount,
			CasDAOFactory.MAX_ITEMS_PER_PAGE, true, false, true);
	}

	public boolean schoolExists (Long schoolid) {
		if(schoolid == null) return false;
		return cdu.existsColumn(schoolid.toString(),
				CasDAOFactory.SCHOOLS, CasDAOFactory.CN_ID);
	}

	public boolean mergeSchools (Long primarySchoolid, Long duplicateSchoolid) {
		School primarySchool = cdu.read(School.class,
				primarySchoolid.toString(), CasDAOFactory.SCHOOLS);
		School duplicateSchool = cdu.read(School.class,
				duplicateSchoolid.toString(), CasDAOFactory.SCHOOLS);

		String primaryUuid = primarySchool.getUuid();
		String duplicateUuid = duplicateSchool.getUuid();

		if(primarySchool == null || duplicateSchool == null) return false;

		Mutator<String> mut = CasDAOUtils.createMutator();

		// STEP 1:
		// Move all users to the primary school
		List<HColumn<Long, String>> userids = cdu.readRow(duplicateSchoolid.toString(),
				CasDAOFactory.SCHOOL_USERS, Long.class, null, null, null,
				CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<Long, String> hColumn : userids) {
			//create new user-class link
			CasDAOUtils.addInsertions(getSchoolLinkColumns(hColumn.getName(),
					primarySchoolid, 0, 0), mut);
		}

		// STEP 2:
		// move all classes to primary school
		List<HColumn<Long, String>> classids = cdu.readRow(duplicateSchoolid.toString(),
				CasDAOFactory.SCHOOL_CLASSES, Long.class, null, null, null,
				CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<Long, String> hColumn : classids) {
			CasDAOUtils.addInsertion(new Column<Long, String>(primarySchoolid.toString(),
				CasDAOFactory.SCHOOL_CLASSES, hColumn.getName(), hColumn.getValue()), mut);
		}

		// STEP 3:
		// move media to primary school
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
			CasDAOUtils.addInsertion(new Column<Long, String>(primaryUuid, CasDAOFactory.DRAWER,
					hColumn.getName(), hColumn.getValue()), mut);
		}

		// STEP 4:
		// delete duplicate
		cdu.delete(duplicateSchool, CasDAOFactory.SCHOOLS, mut);
		mut.execute();

		return true;
	}

	public ArrayList<School> readAllForKeys (ArrayList<String> keys) {
		return cdu.readAll(School.class, keys, CasDAOFactory.SCHOOLS);
	}

}

