package com.erudika.scoold.db.cassandra;

import com.erudika.para.utils.DAO;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Classunit;
import com.erudika.scoold.core.Media;
import com.erudika.scoold.core.School;
import com.erudika.scoold.db.AbstractSchoolDAO;
import com.erudika.scoold.db.cassandra.CasDAOFactory.Column;
import com.erudika.scoold.util.Constants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 *
 * @author alexb
 */
final class CasSchoolDAO<T> extends AbstractSchoolDAO<School> {

    private static final Logger logger = Logger.getLogger(CasSchoolDAO.class.getName());
	private CasDAOUtils cdu = new CasDAOUtils(CasDAOFactory.CASSANDRA_PORT);
	private CasClassUnitDAO<Classunit> cdao = new CasClassUnitDAO<Classunit>();
	private CasMediaDAO<Media> mdao = new CasMediaDAO<Media>();
	
	public CasSchoolDAO () {
    }

    public School read(String id) {
		return cdu.read(School.class, id);
    }

    public String create(School newSchool) {
		return cdu.create(newSchool);
    }

    public void update(School transientSchool) {
		cdu.update(transientSchool);
    }

    public void delete(School persistentSchool) {
		String id = persistentSchool.getId();
		
		Mutator<String> mut = cdu.createMutator();
		cdu.delete(persistentSchool, mut);

		// remove any references of this school in profile pages
		List<HColumn<String, String>> userids = cdu.readRow(id,
				CasDAOFactory.USERS_PARENTS, String.class, null, null, null,
				Utils.DEFAULT_LIMIT, false);

		for (HColumn<String, String> hColumn : userids) {
			cdu.addDeletion(new Column(hColumn.getName(),
					CasDAOFactory.SCHOOLS_PARENTS, id, null), mut);
		}

		// delete all classes for school
		for (Classunit klass : readAllClassUnitsForSchool(id, null, null)) {
			cdao.deleteClass(klass, mut);
		}

		cdu.addDeletions(Arrays.asList(new Column[]{
			new Column<String, String>(id, CasDAOFactory.CLASSES_PARENTS),
			new Column<String, String>(id, CasDAOFactory.USERS_PARENTS),
		}), mut);

		// delete all media for school
		mdao.deleteAllMediaForID(id, mut);

		mut.execute();
    }

    //get all classes that are part of a school
    public ArrayList<Classunit> readAllClassUnitsForSchool(String schoolid, 
			MutableLong page, MutableLong itemcount){

		return cdu.readAll(Classunit.class, null, schoolid, 
			CasDAOFactory.CLASSES_PARENTS, String.class,
			Utils.toLong(page).toString(), page, itemcount,
			Utils.MAX_ITEMS_PER_PAGE, true, false, true);
    }

    public boolean createUserSchoolLink (String userid, String id, Integer from, Integer to){
		boolean b = false;
		// if is linked already return false
		if(id != null){
			int count = cdu.countColumns(userid,
					CasDAOFactory.SCHOOLS_PARENTS, String.class);

			if(count >= Constants.MAX_SCHOOLS_PER_USER) return false;
			// insert into user_schools and school_users
			cdu.batchPut(getSchoolLinkColumns(userid, id, from, to));

			b = true;
		}
		return b;
    }

	protected static List<Column> getSchoolLinkColumns(String userid, String id,
			Integer from, Integer to){
		int min = 1900; int max = 3000;
		Integer fyear = (from == null || from < min || from > max) ? Integer.valueOf(0) : from;
		Integer tyear = (to == null || to < min || to > max) ? Integer.valueOf(0) : to;
		
		// format: [id -> "fromyear=0:toyear=2000"]
		String linkDetails = "fromyear=".concat(fyear.toString()).
				concat(Utils.SEPARATOR).
				concat("toyear=").concat(tyear.toString());

		ArrayList<Column> list = new ArrayList<Column>();
		list.add(new Column<String, String> (userid,
				CasDAOFactory.SCHOOLS_PARENTS, id, linkDetails));
		list.add(new Column<String, String> (id,
				CasDAOFactory.USERS_PARENTS, userid, userid));
		return list;
	}

    public void deleteUserSchoolLink (String userid, School school){
		if(school == null || userid == null) return;

		cdu.batchRemove(
			new Column(userid, CasDAOFactory.SCHOOLS_PARENTS,
				school.getId(), null),
			new Column(school.getId(),
				CasDAOFactory.USERS_PARENTS, userid, null)
		);
    }

	public boolean isLinkedToUser (String schoolid, String userid) {
		return cdu.existsColumn(schoolid, CasDAOFactory.USERS_PARENTS, userid);
	}

	public boolean schoolExists (String schoolid) {
		if(schoolid == null) return false;
		return cdu.existsColumn(schoolid, CasDAOFactory.OBJECTS, DAO.CN_ID);
	}

	public boolean mergeSchools (String primarySchoolid, String duplicateSchoolid) {
		School primarySchool = cdu.read(School.class, primarySchoolid);
		School duplicateSchool = cdu.read(School.class, duplicateSchoolid);

		if(primarySchool == null || duplicateSchool == null) return false;
		
		String primaryId = primarySchool.getId();
		String duplicateId = duplicateSchool.getId();

		Mutator<String> mut = cdu.createMutator();

		// STEP 1:
		// Move all users to the primary school
		List<HColumn<String, String>> userids = cdu.readRow(duplicateSchoolid,
				CasDAOFactory.USERS_PARENTS, String.class, null, null, null,
				Utils.DEFAULT_LIMIT, false);

		for (HColumn<String, String> hColumn : userids) {
			//create new user-class link
			cdu.addInsertions(getSchoolLinkColumns(hColumn.getName(),
					primarySchoolid, 0, 0), mut);
		}

		// STEP 2:
		// move all classes to primary school
		List<HColumn<String, String>> classids = cdu.readRow(duplicateSchoolid,
				CasDAOFactory.CLASSES_PARENTS, String.class, null, null, null,
				Utils.DEFAULT_LIMIT, false);

		for (HColumn<String, String> hColumn : classids) {
			cdu.addInsertion(new Column<String, String>(primarySchoolid,
				CasDAOFactory.CLASSES_PARENTS, hColumn.getName(), hColumn.getValue()), mut);
		}

		// STEP 3:
		// move media to primary school
		ArrayList<Media> photos = mdao.readAllMediaForID(duplicateSchool.getId(), 
				Media.MediaType.PHOTO, null, null, null, true);
		
		for (Media photo : photos) {
			cdu.addInsertion(new Column<String, String>(primaryId, CasDAOFactory.MEDIA_PARENTS, 
					photo.getId(), photo.getId()), mut);
		}

		ArrayList<Media> riches = mdao.readAllMediaForID(duplicateSchool.getId(), 
				Media.MediaType.RICH, null, null, null, true);

		for (Media rich : riches) {
			cdu.addInsertion(new Column<String, String>(primaryId, CasDAOFactory.MEDIA_PARENTS, 
					rich.getId(), rich.getId()), mut);
		}

		// STEP 4:
		// delete duplicate
		cdu.delete(duplicateSchool, mut);
		mut.execute();

		return true;
	}
}

