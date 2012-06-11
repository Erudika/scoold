package com.scoold.db.cassandra;

import com.scoold.core.Classunit;
import com.scoold.core.Media;
import com.scoold.core.School;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractSchoolDAO;
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
final class CasSchoolDAO<T, PK> extends AbstractSchoolDAO<School, Long> {

    private static final Logger logger = Logger.getLogger(CasSchoolDAO.class.getName());
	private CasDAOUtils cdu = (CasDAOUtils) CasDAOFactory.getInstance().getDAOUtils();
	private CasClassUnitDAO<Classunit, Long> cdao = new CasClassUnitDAO<Classunit, Long>();
	private CasMediaDAO<Media, Long> mdao = new CasMediaDAO<Media, Long>();
	
	public CasSchoolDAO () {
    }

    public School read(Long id) {
		return cdu.read(School.class, id.toString());
    }

    public Long create(School newSchool) {
		return cdu.create(newSchool);
    }

    public void update(School transientSchool) {
		cdu.update(transientSchool);
    }

    public void delete(School persistentSchool) {
		Long id = persistentSchool.getId();
		
		Mutator<String> mut = cdu.createMutator();
		cdu.delete(persistentSchool, mut);

		// remove any references of this school in profile pages
		List<HColumn<Long, String>> userids = cdu.readRow(id.toString(),
				CasDAOFactory.USERS_PARENTS, Long.class, null, null, null,
				CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<Long, String> hColumn : userids) {
			cdu.addDeletion(new Column(hColumn.getName().toString(),
					CasDAOFactory.SCHOOLS_PARENTS, id, null), mut);
		}

		// delete all classes for school
		for (Classunit klass : readAllClassUnitsForSchool(id, null, null)) {
			cdao.deleteClass(klass, mut);
		}

		cdu.addDeletions(Arrays.asList(new Column[]{
			new Column<Long, String>(id.toString(), CasDAOFactory.CLASSES_PARENTS),
			new Column<Long, String>(id.toString(), CasDAOFactory.USERS_PARENTS),
		}), mut);

		// delete all media for school
		mdao.deleteAllMediaForID(id, mut);

		mut.execute();
    }

    //get all classes that are part of a school
    public ArrayList<Classunit> readAllClassUnitsForSchool(Long schoolid, 
			MutableLong page, MutableLong itemcount){

		return cdu.readAll(Classunit.class, null, schoolid.toString(), 
			CasDAOFactory.CLASSES_PARENTS, Long.class,
			CasDAOUtils.toLong(page), page, itemcount,
			CasDAOFactory.MAX_ITEMS_PER_PAGE, true, false, true);
    }

    public boolean createUserSchoolLink (Long userid, Long id, Integer from, Integer to){
		boolean b = false;
		// if is linked already return false
		if(id != null){
			int count = cdu.countColumns(userid.toString(),
					CasDAOFactory.SCHOOLS_PARENTS, Long.class);

			if(count >= CasDAOFactory.MAX_SCHOOLS_PER_USER) return false;
			// insert into user_schools and school_users
			cdu.batchPut(getSchoolLinkColumns(userid, id, from, to));

			b = true;
		}
		return b;
    }

	protected static List<Column> getSchoolLinkColumns(Long userid, Long id,
			Integer from, Integer to){
		int min = 1900; int max = 3000;
		Integer fyear = (from == null || from < min || from > max) ? Integer.valueOf(0) : from;
		Integer tyear = (to == null || to < min || to > max) ? Integer.valueOf(0) : to;
		
		// format: [id -> "fromyear=0:toyear=2000"]
		String linkDetails = "fromyear=".concat(fyear.toString()).
				concat(AbstractDAOFactory.SEPARATOR).
				concat("toyear=").concat(tyear.toString());

		ArrayList<Column> list = new ArrayList<Column>();
		list.add(new Column<Long, String> (userid.toString(),
				CasDAOFactory.SCHOOLS_PARENTS, id, linkDetails));
		list.add(new Column<Long, String> (id.toString(),
				CasDAOFactory.USERS_PARENTS, userid, userid.toString()));
		return list;
	}

    public void deleteUserSchoolLink (Long userid, School school){
		if(school == null || userid == null) return;

		cdu.batchRemove(
			new Column(userid.toString(), CasDAOFactory.SCHOOLS_PARENTS,
				school.getId(), null),
			new Column(school.getId().toString(),
				CasDAOFactory.USERS_PARENTS, userid, null)
		);
    }

	public boolean isLinkedToUser (Long schoolid, Long userid) {
		return cdu.existsColumn(schoolid.toString(), 
				CasDAOFactory.USERS_PARENTS, userid);
	}

	public boolean schoolExists (Long schoolid) {
		if(schoolid == null) return false;
		return cdu.existsColumn(schoolid.toString(), CasDAOFactory.OBJECTS, CasDAOFactory.CN_ID);
	}

	public boolean mergeSchools (Long primarySchoolid, Long duplicateSchoolid) {
		School primarySchool = cdu.read(School.class, primarySchoolid.toString());
		School duplicateSchool = cdu.read(School.class, duplicateSchoolid.toString());

		if(primarySchool == null || duplicateSchool == null) return false;
		
		String primaryId = primarySchool.getId().toString();
		String duplicateId = duplicateSchool.getId().toString();

		Mutator<String> mut = cdu.createMutator();

		// STEP 1:
		// Move all users to the primary school
		List<HColumn<Long, String>> userids = cdu.readRow(duplicateSchoolid.toString(),
				CasDAOFactory.USERS_PARENTS, Long.class, null, null, null,
				CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<Long, String> hColumn : userids) {
			//create new user-class link
			cdu.addInsertions(getSchoolLinkColumns(hColumn.getName(),
					primarySchoolid, 0, 0), mut);
		}

		// STEP 2:
		// move all classes to primary school
		List<HColumn<Long, String>> classids = cdu.readRow(duplicateSchoolid.toString(),
				CasDAOFactory.CLASSES_PARENTS, Long.class, null, null, null,
				CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<Long, String> hColumn : classids) {
			cdu.addInsertion(new Column<Long, String>(primarySchoolid.toString(),
				CasDAOFactory.CLASSES_PARENTS, hColumn.getName(), hColumn.getValue()), mut);
		}

		// STEP 3:
		// move media to primary school
		ArrayList<Media> photos = mdao.readAllMediaForID(duplicateSchool.getId(), 
				Media.MediaType.PHOTO, null, null, null, true);
		
		for (Media photo : photos) {
			cdu.addInsertion(new Column<Long, String>(primaryId, CasDAOFactory.MEDIA_PARENTS, 
					photo.getId(), photo.getId().toString()), mut);
		}

		ArrayList<Media> riches = mdao.readAllMediaForID(duplicateSchool.getId(), 
				Media.MediaType.RICH, null, null, null, true);

		for (Media rich : riches) {
			cdu.addInsertion(new Column<Long, String>(primaryId, CasDAOFactory.MEDIA_PARENTS, 
					rich.getId(), rich.getId().toString()), mut);
		}

		// STEP 4:
		// delete duplicate
		cdu.delete(duplicateSchool, mut);
		mut.execute();

		return true;
	}
}

