package com.scoold.db.cassandra;

import com.scoold.core.*;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractUserDAO;
import com.scoold.db.cassandra.CasDAOFactory.Column;
import java.util.*;
import java.util.logging.Logger;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
final class CasUserDAO<T, PK> extends AbstractUserDAO<User, Long>{

    private static final Logger logger = Logger.getLogger(CasUserDAO.class.getName());
	private CasDAOUtils cdu = (CasDAOUtils) CasDAOFactory.getInstance().getDAOUtils();
	private CasMessageDAO<Message, Long> msgdao = new CasMessageDAO<Message, Long>();
	private CasMediaDAO<Media, Long> mdao = new CasMediaDAO<Media, Long>();
	
    public CasUserDAO(){
    }

    public User read(Long id) {
		return cdu.read(User.class, id.toString());
    }

	public Long create(User newUser) {
		if(newUser == null) return null;
		//save auth identifier if there is one
		String ident = newUser.getIdentifier();
		// check if identifier is claimed
		User u = readUserForIdentifier(ident);
		if(u != null) return null;

		Mutator<String> mut = cdu.createMutator();
		newUser.setLastseen(System.currentTimeMillis());
		Long id = cdu.create(newUser, mut);
		
		if(!StringUtils.isBlank(ident)){
			attachIdentifierToUser(ident, id, mut);
		}

		mut.execute();

		return id;
    }

    public void update(User transientUser) {
		cdu.update(transientUser);
    }

    public void delete(User persistentUser) {
		String uid = persistentUser.getId().toString();

		Mutator<String> mut = cdu.createMutator();
		cdu.delete(persistentUser, mut);

		// delete all auth keys for user
		List<HColumn<String, String>> userAuthKeys = cdu.readRow(uid,
				CasDAOFactory.USER_AUTH, String.class,
				null, null, null, CasDAOFactory.DEFAULT_LIMIT, true);

		if(userAuthKeys != null && !userAuthKeys.isEmpty()){
			ArrayList<String> cflist0 = new ArrayList<String> ();
			cflist0.add(CasDAOFactory.AUTH_KEYS.getName());

			for (HColumn<String, String> authKey : userAuthKeys) {
				cdu.addDeletion(new Column<String, String>(authKey.getName(),
						CasDAOFactory.AUTH_KEYS), mut);
			}
		}

		cdu.addDeletions(Arrays.asList(new Column[]{
			new Column<String, String>(uid, CasDAOFactory.USER_AUTH),		
			new Column<Long, String>(uid, CasDAOFactory.SCHOOLS_PARENTS),		
			new Column<Long, String>(uid, CasDAOFactory.CLASSES_PARENTS),		
			new Column<Long, String>(uid, CasDAOFactory.USERS_PARENTS),
			new Column<Long, String>(uid, CasDAOFactory.GROUPS_PARENTS),		
			new Column<Long, String>(uid, CasDAOFactory.POSTS_PARENTS)		
		}), mut);

		// delete messages
		msgdao.deleteAllMessagesForID(persistentUser.getId(), mut);

		// delete media
		mdao.deleteAllMediaForID(persistentUser.getId(), mut);
		
		mut.execute();
    }

    /**********************************************************************
     *                  METHODS FOR READING USERS
     **********************************************************************/
	
    public ArrayList<User> readAllUsersForID(Long parentid, MutableLong page,
			MutableLong itemcount){
		if(parentid == null) return new ArrayList<User>();

		ArrayList<String> keyz = cdu.readAllKeys(User.classtype, parentid.toString(), 
				CasDAOFactory.USERS_PARENTS, Long.class, CasDAOUtils.toLong(page), 
				page, itemcount, CasDAOFactory.MAX_ITEMS_PER_PAGE, true, false, true);
		
		ArrayList<User> list = cdu.readAll(User.class, keyz);
		Mutator<String> mut = cdu.createMutator();
		int countRemoved = 0;
		
		// read repair
		if(list.contains(null)){
			for (int i = 0; i < list.size(); i++) {
				User user = list.get(i);
				if(user == null){
					String id = keyz.get(i);
					if(id != null){
						cdu.addDeletion(new Column<Long, Long>(parentid.toString(), 
								CasDAOFactory.USERS_PARENTS, NumberUtils.toLong(id), null), mut);
						countRemoved++;
					}
				}
			}
		}
		mut.execute();
		list.removeAll(Collections.singleton(null));
		
		if(itemcount != null && countRemoved > 0) {
			itemcount.setValue(itemcount.toLong() - countRemoved);
		}
		
		return list;
    }

	public boolean isFriendWith (Long userid, Long contactid){
		return cdu.existsColumn(userid.toString(),
				CasDAOFactory.USERS_PARENTS, contactid);
	}

	public boolean userExists (Long userid) {
		return cdu.existsColumn(userid.toString(), CasDAOFactory.OBJECTS, CasDAOFactory.CN_ID);
	}

	public boolean userExists (String identifier) {
		if(StringUtils.isBlank(identifier)) return false;
		List<?> row = cdu.readRow(identifier, CasDAOFactory.AUTH_KEYS,
				String.class, null, null, null, 1, true);

		return (row != null && !row.isEmpty());
	}

	public int countContacts (Long userid){
		return cdu.countColumns(userid.toString(),
				CasDAOFactory.USERS_PARENTS, Long.class);
	}

	public int createContactForUser (Long userid, Long contactid) {
		int count = countContacts(userid);
		if(count > CasDAOFactory.MAX_CONTACTS_PER_USER) return count;
		cdu.putColumn(userid.toString(), CasDAOFactory.USERS_PARENTS, contactid, contactid);
		return ++count;
	}

	public int deleteContactForUser (Long userid, Long contactid) {
		int count = countContacts(userid);

		cdu.removeColumn(userid.toString(), CasDAOFactory.USERS_PARENTS, contactid);

		return --count;
	}

    /**********************************************************************
     *                 METHODS FOR READING CLASSUNITS
     **********************************************************************/

    public ArrayList<Classunit> readAllClassUnitsForUser(Long userid, MutableLong page,
			MutableLong itemcount){

		return cdu.readAll(Classunit.class, null, userid.toString(), 
			CasDAOFactory.CLASSES_PARENTS,Long.class,
			CasDAOUtils.toLong(page), page, itemcount,
			CasDAOFactory.MAX_ITEMS_PER_PAGE, true, false, true);
    }

    /**********************************************************************
     *               METHODS FOR READING/WRITING SCHOOLS
     **********************************************************************/

    public ArrayList<School> readAllSchoolsForUser (Long userid, MutableLong page,
			MutableLong itemcount, int howMany){

		ArrayList<String> keys = new ArrayList<String>();

		List<HColumn<Long, String>> list = cdu.readRow(userid.toString(),
				CasDAOFactory.SCHOOLS_PARENTS, Long.class,
				CasDAOUtils.toLong(page), page, itemcount, howMany, true);

		Map<Long, String> extraProps = new HashMap<Long, String>();

		for (HColumn<Long, String> hColumn : list) {
			extraProps.put(hColumn.getName(), hColumn.getValue());
			keys.add(hColumn.getName().toString());
		}

		ArrayList<School> schools = cdu.readAll(School.class, keys);

		//assign additional properties like from/to year
		String fyear = "0";
		String tyear = "0";
		for (School school : schools) {
			String linkDetails = extraProps.get(school.getId());
			int sepindx = linkDetails.indexOf(AbstractDAOFactory.SEPARATOR);
			if(!StringUtils.isBlank(linkDetails)){
				
				fyear = linkDetails.substring(9, sepindx);
				tyear = linkDetails.substring(sepindx+8);

				school.setFromyear(NumberUtils.toInt(fyear));
				school.setToyear(NumberUtils.toInt(tyear));
			}
		}

		return schools;
    }
	
	/**********************************************************************
     *                 METHODS FOR READING GROUPS
     **********************************************************************/

    public ArrayList<Group> readAllGroupsForUser(Long userid, MutableLong page,
			MutableLong itemcount){

		return cdu.readAll(Group.class, null, userid.toString(), 
			CasDAOFactory.GROUPS_PARENTS, Long.class,
			CasDAOUtils.toLong(page), page, itemcount,
			CasDAOFactory.MAX_ITEMS_PER_PAGE, true, false, true);
    }

	/**********************************************************************
     *          METHODS FOR READING, CREATING & DELETING OPENIDs
     **********************************************************************/

    public ArrayList<String> readAllIdentifiersForUser (Long userid){
		ArrayList<String> list = new ArrayList<String>();

		List<HColumn<String, String>> cl = cdu.readRow(userid.toString(),
				CasDAOFactory.USER_AUTH, String.class,
				null, null, null, CasDAOFactory.DEFAULT_LIMIT, true);

		for (HColumn<String, String> cosc : cl) {
			list.add(cosc.getName());
		}

		return list;
    }

    public User readUserForIdentifier (String identifier){
		if(StringUtils.isBlank(identifier)) return null;
		ArrayList<User> user = cdu.readAll(User.class, null, identifier, 
				CasDAOFactory.AUTH_KEYS, String.class, null, null, null, 1, true, false, false);

		if(user == null || user.isEmpty()) return null;

		return user.get(0);
    }

    public void attachIdentifierToUser(String identifier, Long userid) {
		Mutator<String> mut = cdu.createMutator();
		attachIdentifierToUser(identifier, userid, mut);
		mut.execute();
	}

    private void attachIdentifierToUser(String identifier, Long userid, Mutator<String> mut) {
		if(StringUtils.isBlank(identifier) || userid == null) return;
		cdu.addInsertion(new Column(userid.toString(), CasDAOFactory.USER_AUTH,
				identifier, identifier), mut);
		cdu.addInsertion(new Column(identifier, CasDAOFactory.AUTH_KEYS,
				userid.toString(), userid.toString()), mut);
    }

    public void deleteAllOpenidsForUser (Long userid) {
		Mutator<String> mut = cdu.createMutator();
		cdu.deleteRow(userid.toString(), CasDAOFactory.USER_AUTH, mut);

		for (String identifier : readAllIdentifiersForUser(userid)) {
			cdu.deleteRow(identifier, CasDAOFactory.AUTH_KEYS, mut);
		}
		
		mut.execute();
    }

    public void detachIdentifierFromUser (String identifier, Long userid) {
		cdu.batchRemove(
			new Column(identifier, CasDAOFactory.AUTH_KEYS, userid.toString(), null),
			new Column(userid.toString(), CasDAOFactory.USER_AUTH, identifier, null)
		);
    }

	public ArrayList<String> getFavouriteTagsForUser(Long userid) {
		String favtags = cdu.getColumn(userid.toString(), CasDAOFactory.OBJECTS, "favtags");
		User u = new User();
		u.setFavtags(favtags);
		return u.getFavtagsList();
	}

}

