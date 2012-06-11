package com.scoold.db.cassandra;

import com.scoold.core.Classunit;
import com.scoold.core.Media;
import com.scoold.core.Post;
import com.scoold.db.AbstractClassUnitDAO;
import com.scoold.db.cassandra.CasDAOFactory.Column;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;

/**
 *
 * @author alexb
 */
final class CasClassUnitDAO<T, PK> extends AbstractClassUnitDAO<Classunit, Long>{

    private static final Logger logger = Logger.getLogger(CasClassUnitDAO.class.getName());
	private CasDAOUtils cdu = (CasDAOUtils) CasDAOFactory.getInstance().getDAOUtils();
	private CasMediaDAO<Media, Long> mdao = new CasMediaDAO<Media, Long>();
	
    public CasClassUnitDAO () {
	}

    public Classunit read(Long id) {
		return cdu.read(Classunit.class, id.toString());
    }

    public Long create(Classunit newClassUnit) {
		Long id = newClassUnit.getId() != null ? newClassUnit.getId() : cdu.getNewId();
		newClassUnit.setId(id);
		
		// attach a new clean blackboard to class
		Post bb = new Post(Post.PostType.BLACKBOARD);
//		bb.setTitle("Blackboard");
		bb.setBody(" ");
		bb.setUserid(newClassUnit.getUserid());
		bb.setParentid(newClassUnit.getId());

		Mutator<String> mut = cdu.createMutator();

		Long bbid = cdu.create(bb, mut);
		newClassUnit.setBlackboardid(bbid);

		cdu.create(newClassUnit, mut);
		cdu.addInsertion(new Column<Long, String>(newClassUnit.getSchoolid().toString(),
				CasDAOFactory.CLASSES_PARENTS, id, id.toString()), mut);

		// auto add to my classes
		cdu.addInsertions(getClassLinkColumnsWithChecks(newClassUnit.getUserid(),
				newClassUnit), mut);

		mut.execute();
		
		return id;
    }

    public void update(Classunit transientClassUnit) {
		cdu.update(transientClassUnit);
    }

    public void delete(Classunit persistentClassUnit) {
		Mutator<String> mut = cdu.createMutator();
		deleteClass(persistentClassUnit, mut);
		mut.execute();
    }

	protected void deleteClass(Classunit persistentClassUnit, Mutator<String> mut){
		Long id = persistentClassUnit.getId();

		Post bb = cdu.read(Post.class, persistentClassUnit.getBlackboardid().toString());
		if(bb != null) cdu.delete(bb, mut);

		cdu.delete(persistentClassUnit, mut);

		cdu.addDeletion(new Column<Long, String>(persistentClassUnit.getSchoolid().toString(),
				CasDAOFactory.CLASSES_PARENTS, id, null), mut);

		// remove any references of this class in profile pages
		List<HColumn<Long, String>> userids = cdu.readRow(id.toString(),
				CasDAOFactory.USERS_PARENTS, Long.class, null, null, null,
				CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<Long, String> hColumn : userids) {
			cdu.addDeletion(new Column<Long, String>(hColumn.getName().toString(),
					CasDAOFactory.CLASSES_PARENTS, id, null), mut);
		}

		cdu.addDeletion(new Column<Long, String>(id.toString(), CasDAOFactory.USERS_PARENTS), mut);
		
		mdao.deleteAllMediaForID(id, mut);
	}

	public boolean createUserClassLink (Long userid, Classunit klass){
		List<Column> list = getClassLinkColumnsWithChecks(userid, klass);
		cdu.batchPut(list);

		return !list.isEmpty();
    }

	private List<Column> getClassLinkColumnsWithChecks(Long userid, Classunit klass){
		ArrayList<Column> list = new ArrayList<Column>();
		if(klass.getId() != null && !isLinkedToUser(klass.getId(), userid)){
			int count1 = cdu.countColumns(userid.toString(),
					CasDAOFactory.CLASSES_PARENTS, Long.class);
			
			int count2 = cdu.countColumns(userid.toString(),
					CasDAOFactory.SCHOOLS_PARENTS, Long.class);

			if(count1 < CasDAOFactory.MAX_CLASSES_PER_USER && 
					count2 < CasDAOFactory.MAX_SCHOOLS_PER_USER) {
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
				CasDAOFactory.CLASSES_PARENTS, classid, classid.toString()));
		list.add(new Column<Long, String>(classid.toString(),
				CasDAOFactory.USERS_PARENTS, userid, userid.toString()));
		return list;
	}

    public void deleteUserClassLink (Long userid, Long classid){
		ArrayList<Column> list = new ArrayList<Column>();

		list.add(new Column<Long, String>(userid.toString(),
				CasDAOFactory.CLASSES_PARENTS, classid, classid.toString()));
		list.add(new Column<Long, String>(classid.toString(),
				CasDAOFactory.USERS_PARENTS, userid, userid.toString()));
		cdu.batchRemove(list);
    }

	public boolean isLinkedToUser (Long classid, Long userid) {
		return cdu.existsColumn(classid.toString(),	CasDAOFactory.USERS_PARENTS, userid);
	}

	public boolean classExists (Long classid) {
		return cdu.existsColumn(classid.toString(),
				CasDAOFactory.OBJECTS, CasDAOFactory.CN_ID);
	}

	public boolean mergeClasses (Long primaryClassid, Long duplicateClassid){
		Classunit primaryClass = cdu.read(Classunit.class,
						primaryClassid.toString());
		Classunit duplicateClass = cdu.read(Classunit.class,
						duplicateClassid.toString());
		
		if(primaryClass == null || duplicateClass == null) return false;
		else if(!duplicateClass.getSchoolid().equals(primaryClass.getSchoolid())) return false;

		String primaryId = primaryClass.getId().toString();
		String duplicateId = duplicateClass.getId().toString();

		Mutator<String> mut = cdu.createMutator();
		// STEP 1:
		// Move every user to the primary class
		List<HColumn<Long, String>> userids = cdu.readRow(duplicateClassid.toString(),
				CasDAOFactory.USERS_PARENTS, Long.class, null, null, null,
				CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<Long, String> hColumn : userids) {
			//create new user-class link
			cdu.addInsertions(getClassLinkColumnsOnly(hColumn.getName(),
					primaryClassid), mut);
		}

		// STEP 2:
		// move media to primary class
		ArrayList<Media> photos = mdao.readAllMediaForID(duplicateClass.getId(), 
				Media.MediaType.PHOTO, null, null, null, true);
		
		for (Media photo : photos) {
			cdu.addInsertion(new Column<Long, String>(primaryId, CasDAOFactory.MEDIA_PARENTS, 
					photo.getId(), photo.getId().toString()), mut);
		}

		ArrayList<Media> riches = mdao.readAllMediaForID(duplicateClass.getId(), 
				Media.MediaType.RICH, null, null, null, true);

		for (Media rich : riches) {
			cdu.addInsertion(new Column<Long, String>(primaryId, CasDAOFactory.MEDIA_PARENTS, 
					rich.getId(), rich.getId().toString()), mut);
		}

		// STEP 3:
		// delete duplicate
		cdu.delete(duplicateClass, mut);

		mut.execute();

		return true;
	}

	public int countUsersForClassUnit(Long classid) {
		return cdu.countColumns(classid.toString(),
				CasDAOFactory.USERS_PARENTS, Long.class);
	}

}

