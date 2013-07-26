package com.erudika.scoold.db.cassandra;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.erudika.para.utils.DAO;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Blackboard;
import com.erudika.scoold.core.Classunit;
import com.erudika.scoold.core.Media;
import com.erudika.scoold.db.AbstractClassUnitDAO;
import com.erudika.scoold.db.cassandra.CasDAOFactory.Column;
import com.erudika.scoold.util.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author alexb
 */
final class CasClassUnitDAO<T> extends AbstractClassUnitDAO<Classunit>{

    private static final Logger logger = Logger.getLogger(CasClassUnitDAO.class.getName());
	private CasDAOUtils cdu = new CasDAOUtils(CasDAOFactory.CASSANDRA_PORT);
	private CasMediaDAO<Media> mdao = new CasMediaDAO<Media>();
	
    public CasClassUnitDAO () {
	}

    public Classunit read(String id) {
		return cdu.read(Classunit.class, id);
    }

    public String create(Classunit newClassUnit) {
		String id = newClassUnit.getId() != null ? newClassUnit.getId() : Utils.getNewId();
		newClassUnit.setId(id);
		
		// attach a new clean blackboard to class
		Blackboard bb = new Blackboard();
//		bb.setTitle("Blackboard");
		bb.setBody(" ");
		bb.setCreatorid(newClassUnit.getCreatorid());
		bb.setParentid(newClassUnit.getId());

		Mutator<String> mut = cdu.createMutator();

		String bbid = cdu.create(bb, mut);
//		newClassUnit.setBlackboardid(bbid);

		cdu.create(newClassUnit, mut);
		cdu.addInsertion(new Column<String, String>(newClassUnit.getParentid(), 
				CasDAOFactory.CLASSES_PARENTS, id, id), mut);

		// auto add to my classes
		cdu.addInsertions(getClassLinkColumnsWithChecks(newClassUnit.getCreatorid(),
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
		String id = persistentClassUnit.getId();

//		Post bb = cdu.read(Post.class, persistentClassUnit.getBlackboardid());
//		if(bb != null) cdu.delete(bb, mut);

		cdu.delete(persistentClassUnit, mut);

		cdu.addDeletion(new Column<String, String>(persistentClassUnit.getParentid(),
				CasDAOFactory.CLASSES_PARENTS, id, null), mut);

		// remove any references of this class in profile pages
		List<HColumn<String, String>> userids = cdu.readRow(id,
				CasDAOFactory.USERS_PARENTS, String.class, null, null, null,
				Utils.DEFAULT_LIMIT, false);

		for (HColumn<String, String> hColumn : userids) {
			cdu.addDeletion(new Column<String, String>(hColumn.getName(),
					CasDAOFactory.CLASSES_PARENTS, id, null), mut);
		}

		cdu.addDeletion(new Column<String, String>(id, CasDAOFactory.USERS_PARENTS), mut);
		
		mdao.deleteAllMediaForID(id, mut);
	}

	public boolean createUserClassLink (String userid, Classunit klass){
		List<Column> list = getClassLinkColumnsWithChecks(userid, klass);
		cdu.batchPut(list);

		return !list.isEmpty();
    }

	private List<Column> getClassLinkColumnsWithChecks(String userid, Classunit klass){
		ArrayList<Column> list = new ArrayList<Column>();
		if(klass.getId() != null && !isLinkedToUser(klass.getId(), userid)){
			int count1 = cdu.countColumns(userid, CasDAOFactory.CLASSES_PARENTS, String.class);
			int count2 = cdu.countColumns(userid, CasDAOFactory.SCHOOLS_PARENTS, String.class);

			if(count1 < Constants.MAX_CLASSES_PER_USER && count2 < Constants.MAX_SCHOOLS_PER_USER) {
				list.addAll(getClassLinkColumnsOnly(userid, klass.getId()));
				list.addAll(CasSchoolDAO.getSchoolLinkColumns(userid, klass.getParentid(), 0, 0));
			}
		}

		return list;
	}

	private List<Column> getClassLinkColumnsOnly(String userid, String classid){
		ArrayList<Column> list = new ArrayList<Column>();
		list.add(new Column<String, String>(userid, CasDAOFactory.CLASSES_PARENTS, classid, classid));
		list.add(new Column<String, String>(classid, CasDAOFactory.USERS_PARENTS, userid, userid));
		return list;
	}

    public void deleteUserClassLink (String userid, String classid){
		ArrayList<Column> list = new ArrayList<Column>();

		list.add(new Column<String, String>(userid,
				CasDAOFactory.CLASSES_PARENTS, classid, classid));
		list.add(new Column<String, String>(classid,
				CasDAOFactory.USERS_PARENTS, userid, userid));
		cdu.batchRemove(list);
    }

	public boolean isLinkedToUser (String classid, String userid) {
		return cdu.existsColumn(classid, CasDAOFactory.USERS_PARENTS, userid);
	}

	public boolean classExists (String classid) {
		return cdu.existsColumn(classid,
				CasDAOFactory.OBJECTS, DAO.CN_ID);
	}

	public boolean mergeClasses (String primaryClassid, String duplicateClassid){
		Classunit primaryClass = cdu.read(Classunit.class,
						primaryClassid);
		Classunit duplicateClass = cdu.read(Classunit.class,
						duplicateClassid);
		
		if(primaryClass == null || duplicateClass == null) return false;
		else if(!duplicateClass.getParentid().equals(primaryClass.getParentid())) return false;

		String primaryId = primaryClass.getId();
		String duplicateId = duplicateClass.getId();

		Mutator<String> mut = cdu.createMutator();
		// STEP 1:
		// Move every user to the primary class
		List<HColumn<String, String>> userids = cdu.readRow(duplicateClassid, 
				CasDAOFactory.USERS_PARENTS, String.class, null, null, null,
				Utils.DEFAULT_LIMIT, false);

		for (HColumn<String, String> hColumn : userids) {
			//create new user-class link
			cdu.addInsertions(getClassLinkColumnsOnly(hColumn.getName(), primaryClassid), mut);
		}

		// STEP 2:
		// move media to primary class
		ArrayList<Media> photos = mdao.readAllMediaForID(duplicateClass.getId(), 
				Media.MediaType.PHOTO, null, null, null, true);
		
		for (Media photo : photos) {
			cdu.addInsertion(new Column<String, String>(primaryId, CasDAOFactory.MEDIA_PARENTS, 
					photo.getId(), photo.getId()), mut);
		}

		ArrayList<Media> riches = mdao.readAllMediaForID(duplicateClass.getId(), 
				Media.MediaType.RICH, null, null, null, true);

		for (Media rich : riches) {
			cdu.addInsertion(new Column<String, String>(primaryId, CasDAOFactory.MEDIA_PARENTS, 
					rich.getId(), rich.getId()), mut);
		}

		// STEP 3:
		// delete duplicate
		cdu.delete(duplicateClass, mut);

		mut.execute();

		return true;
	}

	public int countUsersForClassUnit(String classid) {
		return cdu.countColumns(classid, CasDAOFactory.USERS_PARENTS, String.class);
	}

	public String sendChat(String primaryClassid, String chat) {
		if(primaryClassid == null || StringUtils.isBlank(chat)) return "[]";
		String chad = receiveChat(primaryClassid);
		try {
			StringBuilder sb = new StringBuilder("[");
			JSONArray arr = new JSONArray(chad);
			int start = (arr.length() >= Utils.MAX_ITEMS_PER_PAGE) ? 1 : 0;
			
			for (int i = start; i < arr.length(); i++) {
				JSONObject object = arr.getJSONObject(i);
				sb.append(object.toString()).append(",");
			}	
			
			sb.append(chat);			
			sb.append("]");
			chad = sb.toString().replaceAll(",]", "]");
		} catch (JSONException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		
		cdu.putColumn(primaryClassid, CasDAOFactory.OBJECTS, "chat", chad);
		return chad;
		
//		if(primaryClassid == null || StringUtils.isBlank(chat)) return "[]";
//		String chad = receiveChat(primaryClassid);
//		try {
//			StringBuilder sb = new StringBuilder("[");
//			JSONArray arr = new JSONArray(chad);
//			ArrayList<String> list = new ArrayList<String>();
//			TreeMap<String, String> map = new TreeMap<String, String>();
//			JSONObject obj = new JSONObject(chat);
//			map.put(obj.getLong("stamp"), chat);
//			
//			for (int i = 0; i < arr.length(); i++) {
//				JSONObject object = arr.getJSONObject(i);
//				map.put(object.getLong("stamp"), object.toStr ing());				
//			}			
//			
//			if(arr.length() >= Utils.MAX_ITEMS_PER_PAGE){
//				list.addAll(map.values());
//				list.remove(0);
//			}
//			
//			for (String string : list) {
//				sb.append(string).append(",");
//			}
//			sb.append("]");
//			chad = sb.toSt ring().replaceAll(",]", "]");
//		} catch (JSONException ex) {
//			logger.log(Level.SEVERE, null, ex);
//		}
//		
//		cdu.putColumn(primaryClassid, CasDAOFactory.OBJECTS, "chat", chad);
//		return chad;
	}

	public String receiveChat(String primaryClassid) {
		String chat = cdu.getColumn(primaryClassid, CasDAOFactory.OBJECTS, "chat");
		if(StringUtils.isBlank(chat)) chat = "[]";
		return chat;
	}

}

