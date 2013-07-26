package com.erudika.scoold.db.dynamodb;

import com.erudika.scoold.core.School;
import com.erudika.scoold.core.Group;
import com.erudika.scoold.core.Classunit;
import com.erudika.scoold.core.User;
import com.erudika.scoold.db.AbstractUserDAO;
import java.util.*;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 *
 * @author alexb
 */
final class DynUserDAO<T> extends AbstractUserDAO<User>{

    public DynUserDAO(){
    }

    public User read(String id) {
//		return cdu.read(User.class, id);
		return null;
    }

	public String create(User newUser) {
//		if(newUser == null) return null;
//		//save auth identifier if there is one
//		String ident = newUser.getIdentifier();
//		// check if identifier is claimed
//		User u = readUserForIdentifier(ident);
//		if(u != null) return null;
//
//		Mutator<String> mut = cdu.createMutator();
//		newUser.setLastseen(System.currentTimeMillis());
//		String id = cdu.create(newUser, mut);
//		
//		if(!StringUtils.isBlank(ident)){
//			attachIdentifierToUser(ident, id, mut);
//		}
//
//		mut.execute();
//
//		return id;
		return null;
    }

    public void update(User transientUser) {
//		cdu.update(transientUser);
    }

    public void delete(User persistentUser) {
//		String uid = persistentUser.getId();
//
//		Mutator<String> mut = cdu.createMutator();
//		cdu.delete(persistentUser, mut);
//
//		// delete all auth keys for user
//		List<HColumn<String, String>> userAuthKeys = cdu.readRow(uid,
//				CasDAOFactory.USER_AUTH, String.class,
//				null, null, null, Utils.DEFAULT_LIMIT, true);
//
//		if(userAuthKeys != null && !userAuthKeys.isEmpty()){
//			ArrayList<String> cflist0 = new ArrayList<String> ();
//			cflist0.add(CasDAOFactory.AUTH_KEYS.getName());
//
//			for (HColumn<String, String> authKey : userAuthKeys) {
//				cdu.addDeletion(new Column<String, String>(authKey.getName(),
//						CasDAOFactory.AUTH_KEYS), mut);
//			}
//		}
//
//		cdu.addDeletions(Arrays.asList(new Column[]{
//			new Column<String, String>(uid, CasDAOFactory.USER_AUTH),		
//			new Column<String, String>(uid, CasDAOFactory.SCHOOLS_PARENTS),		
//			new Column<String, String>(uid, CasDAOFactory.CLASSES_PARENTS),		
//			new Column<String, String>(uid, CasDAOFactory.USERS_PARENTS),
//			new Column<String, String>(uid, CasDAOFactory.GROUPS_PARENTS),		
//			new Column<String, String>(uid, CasDAOFactory.POSTS_PARENTS)		
//		}), mut);
//
//		// delete messages
//		msgdao.deleteAllMessagesForID(persistentUser.getId(), mut);
//
//		// delete media
//		mdao.deleteAllMediaForID(persistentUser.getId(), mut);
//		
//		mut.execute();
    }

    /**********************************************************************
     *                  METHODS FOR READING USERS
     **********************************************************************/
	
    public ArrayList<User> readAllUsersForID(String parentid, MutableLong page,
			MutableLong itemcount){
//		if(parentid == null) return new ArrayList<User>();
//
//		ArrayList<String> keyz = cdu.readAllKeys(PObject.classname(User.class), parentid, 
//				CasDAOFactory.USERS_PARENTS, String.class, Utils.toLong(page).toString(), 
//				page, itemcount, Utils.MAX_ITEMS_PER_PAGE, true, false, true);
//		
//		ArrayList<User> list = cdu.readAll(User.class, keyz);
//		Mutator<String> mut = cdu.createMutator();
//		int countRemoved = 0;
//		
//		// read repair
//		if(list.contains(null)){
//			for (int i = 0; i < list.size(); i++) {
//				User user = list.get(i);
//				if(user == null){
//					String id = keyz.get(i);
//					if(id != null){
//						cdu.addDeletion(new Column<String, String>(parentid, 
//								CasDAOFactory.USERS_PARENTS, id, null), mut);
//						countRemoved++;
//					}
//				}
//			}
//		}
//		mut.execute();
//		list.removeAll(Collections.singleton(null));
//		
//		if(itemcount != null && countRemoved > 0) {
//			itemcount.setValue(itemcount.toLong() - countRemoved);
//		}
//		
//		return list;
		return null;
		
    }

	public boolean isFriendWith (String userid, String contactid){
//		if(userid == null) return false;
//		return cdu.existsColumn(userid, CasDAOFactory.USERS_PARENTS, contactid);
		return false;
	}

//	public boolean userExists (String userid) {
//		if(userid == null) return false;
//		return cdu.existsColumn(userid, CasDAOFactory.OBJECTS, DAO.CN_ID);
//	}

	public boolean userExists (String identifier) {
//		if(StringUtils.isBlank(identifier)) return false;
//		List<?> row = cdu.readRow(identifier, CasDAOFactory.AUTH_KEYS,
//				String.class, null, null, null, 1, true);
//
//		return (row != null && !row.isEmpty());
		return false;
	}
	
	public int countContacts (String userid){
		return 0;
	}

	public int createContactForUser (String userid, String contactid) {
//		int count = countContacts(userid);
//		if(count > CasDAOFactory.MAX_CONTACTS_PER_USER) return count;
//		cdu.putColumn(userid, CasDAOFactory.USERS_PARENTS, contactid, contactid);
//		return ++count;
		return 0;
	}

	public int deleteContactForUser (String userid, String contactid) {
		return 0;
	}

    /**********************************************************************
     *                 METHODS FOR READING CLASSUNITS
     **********************************************************************/

    public ArrayList<Classunit> readAllClassUnitsForUser(String userid, MutableLong page,
			MutableLong itemcount){

		return null;
    }

    /**********************************************************************
     *               METHODS FOR READING/WRITING SCHOOLS
     **********************************************************************/

    public ArrayList<School> readAllSchoolsForUser (String userid, MutableLong page,
			MutableLong itemcount, int howMany){
//
//		ArrayList<String> keys = new ArrayList<String>();
//
//		List<HColumn<String, String>> list = cdu.readRow(userid,
//				CasDAOFactory.SCHOOLS_PARENTS, String.class,
//				Utils.toLong(page).toString(), page, itemcount, howMany, true);
//
//		Map<String, String> extraProps = new HashMap<String, String>();
//
//		for (HColumn<String, String> hColumn : list) {
//			extraProps.put(hColumn.getName(), hColumn.getValue());
//			keys.add(hColumn.getName());
//		}
//
//		ArrayList<School> schools = cdu.readAll(School.class, keys);
//
//		//assign additional properties like from/to year
//		String fyear = "0";
//		String tyear = "0";
//		for (School school : schools) {
//			String linkDetails = extraProps.get(school.getId());
//			int sepindx = linkDetails.indexOf(Utils.SEPARATOR);
//			if(!StringUtils.isBlank(linkDetails)){
//				
//				fyear = linkDetails.substring(9, sepindx);
//				tyear = linkDetails.substring(sepindx+8);
//
//				school.setFromyear(NumberUtils.toInt(fyear));
//				school.setToyear(NumberUtils.toInt(tyear));
//			}
//		}
//
//		return schools;
		return null;
    }
	
	/**********************************************************************
     *                 METHODS FOR READING GROUPS
     **********************************************************************/

    public ArrayList<Group> readAllGroupsForUser(String userid, MutableLong page, MutableLong itemcount){
//		return cdu.readAll(Group.class, null, userid, 
//			CasDAOFactory.GROUPS_PARENTS, String.class,
//			Utils.toLong(page).toString(), page, itemcount,
//			Utils.MAX_ITEMS_PER_PAGE, true, false, true);
		return null;
    }

	/**********************************************************************
     *          METHODS FOR READING, CREATING & DELETING OPENIDs
     **********************************************************************/

    public ArrayList<String> readAllIdentifiersForUser (String userid){
//		ArrayList<String> list = new ArrayList<String>();
//
//		List<HColumn<String, String>> cl = cdu.readRow(userid,
//				CasDAOFactory.USER_AUTH, String.class,
//				null, null, null, Utils.DEFAULT_LIMIT, true);
//
//		for (HColumn<String, String> cosc : cl) {
//			list.add(cosc.getName());
//		}
//
//		return list;
		return null;
    }

    public User readUserForIdentifier (String identifier){
//		if(StringUtils.isBlank(identifier)) return null;
//		ArrayList<User> user = cdu.readAll(User.class, null, identifier, 
//				CasDAOFactory.AUTH_KEYS, String.class, null, null, null, 1, true, false, false);
//
//		if(user == null || user.isEmpty()) return null;
//
//		return user.get(0);
		return null;
    }

    public void attachIdentifierToUser(String identifier, String userid) {
//		Mutator<String> mut = cdu.createMutator();
//		attachIdentifierToUser(identifier, userid, mut);
//		mut.execute();
	}

    private void attachIdentifierToUser(String identifier, String userid, Mutator<String> mut) {
//		if(StringUtils.isBlank(identifier) || userid == null) return;
//		cdu.addInsertion(new Column(userid, CasDAOFactory.USER_AUTH,
//				identifier, identifier), mut);
//		cdu.addInsertion(new Column(identifier, CasDAOFactory.AUTH_KEYS,
//				userid, userid), mut);
    }

    public void deleteAllOpenidsForUser (String userid) {
//		Mutator<String> mut = cdu.createMutator();
//		cdu.deleteRow(userid, CasDAOFactory.USER_AUTH, mut);
//
//		for (String identifier : readAllIdentifiersForUser(userid)) {
//			cdu.deleteRow(identifier, CasDAOFactory.AUTH_KEYS, mut);
//		}
//		
//		mut.execute();
    }

    public void detachIdentifierFromUser (String identifier, String userid) {
//		cdu.batchRemove(
//			new Column(identifier, CasDAOFactory.AUTH_KEYS, userid, null),
//			new Column(userid, CasDAOFactory.USER_AUTH, identifier, null)
//		);
    }

	public ArrayList<String> getFavouriteTagsForUser(String userid) {
//		String favtags = cdu.getColumn(userid, CasDAOFactory.OBJECTS, "favtags");
//		User u = new User();
//		u.setFavtags(favtags);
//		return u.getFavtagsList();
		return null;
	}

}

