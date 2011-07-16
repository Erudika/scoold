package com.scoold.db.cassandra;

import com.scoold.core.Classunit;
import com.scoold.core.Media;
import com.scoold.core.Message;
import com.scoold.core.Post;
import com.scoold.core.Post.PostType;
import com.scoold.core.School;
import com.scoold.core.User;
import com.scoold.db.AbstractUserDAO;
import com.scoold.db.cassandra.CasDAOFactory.CF;
import com.scoold.db.cassandra.CasDAOFactory.Column;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
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
public final class CasUserDAO<T, PK> extends AbstractUserDAO<User, Long>{

    private static final Logger logger = Logger.getLogger(CasUserDAO.class.getName());
	private CasDAOUtils cdu = new CasDAOUtils();
	
    public CasUserDAO(){
    }

    public User read(Long id) {
		return cdu.read(User.class, id.toString(), CasDAOFactory.USERS);
    }

    public User readUserByEmail(String email) {
		ArrayList<User> user = cdu.readAll(User.class, email, 
				CasDAOFactory.EMAILS, CasDAOFactory.USERS, String.class,
				null, null, null, 1, false, false, false);

		if(user == null || user.isEmpty()) return null;

		return user.get(0);
    }

    public User read(String uuid) {
		ArrayList<User> user = cdu.readAll(User.class, uuid, 
				CasDAOFactory.USERS_UUIDS, CasDAOFactory.USERS, String.class,
				null, null, null, 1, true, false, false);

		if(user == null || user.isEmpty()) return null;

		return user.get(0);
    }

	public Long create(User newUser) {
		if(newUser == null) return null;

		Mutator<String> mut = CasDAOUtils.createMutator();
		newUser.setLastseen(System.currentTimeMillis());
		Long id = cdu.create(newUser, CasDAOFactory.USERS, mut);
		if(id == null) return null;

		CasDAOUtils.addInsertion(new Column(newUser.getUuid(),
				CasDAOFactory.USERS_UUIDS, id.toString(), id.toString()), mut);

		//save auth identifier if there is one
		String ident = newUser.getIdentifier();
		if(!StringUtils.isBlank(ident)){
			attachIdentifierToUser(ident, id, mut);
		}

		if(!userExists(newUser.getEmail()) && !StringUtils.isBlank(newUser.getEmail())){
			CasDAOUtils.addInsertion(new Column(newUser.getEmail(), CasDAOFactory.EMAILS,
					id.toString(), id.toString()), mut);
		}

		cdu.addNumbersortColumn(null, CasDAOFactory.USERS_BY_REPUTATION,
				id, newUser.getReputation(), newUser.getOldreputation(), mut);

		cdu.addTimesortColumn(null, id, CasDAOFactory.USERS_BY_TIMESTAMP, id, null, mut);
		mut.execute();

		newUser.index();

		return id;
    }

    public void update(User transientUser) {
		Mutator<String> mut = CasDAOUtils.createMutator();
		cdu.update(transientUser, CasDAOFactory.USERS, mut);

		cdu.addNumbersortColumn(null, CasDAOFactory.USERS_BY_REPUTATION,
				transientUser.getId(), transientUser.getReputation(),
				transientUser.getOldreputation(), mut);

		mut.execute();

		transientUser.reindex();
    }

    public void delete(User persistentUser) {
		//this will completely wipe off any user data from db!
		//only admins should be able to do this
		String uid = persistentUser.getId().toString();
		String uuid = persistentUser.getUuid();

		Mutator<String> mut = CasDAOUtils.createMutator();
		cdu.delete(persistentUser, CasDAOFactory.USERS, mut);

		// delete all auth keys for user
		List<HColumn<String, String>> userAuthKeys = cdu.readRow(uid,
				CasDAOFactory.USER_AUTH, String.class,
				null, null, null, CasDAOFactory.DEFAULT_LIMIT, true);

		if(userAuthKeys != null && !userAuthKeys.isEmpty()){
			ArrayList<String> cflist0 = new ArrayList<String> ();
			cflist0.add(CasDAOFactory.AUTH_KEYS.getName());

			for (HColumn<String, String> authKey : userAuthKeys) {
				CasDAOUtils.addDeletion(new Column<String, String>(authKey.getName(),
						CasDAOFactory.AUTH_KEYS), mut);
			}
		}

		CasDAOUtils.addInsertions(Arrays.asList(new Column[]{
			new Column(persistentUser.getEmail(), CasDAOFactory.EMAILS),
			new Column(uid, CasDAOFactory.USER_AUTH),		
			new Column(uid, CasDAOFactory.USER_SCHOOLS),		
			new Column(uid, CasDAOFactory.USER_SCHOOLS),		
			new Column(uid, CasDAOFactory.USER_CLASSES),		
			new Column(uid, CasDAOFactory.USER_ANSWERS),		
			new Column(uid, CasDAOFactory.USER_QUESTIONS),	
			new Column(uid, CasDAOFactory.CONTACTS),
			new Column(uuid, CasDAOFactory.USERS_UUIDS)
		}), mut);

		// delete messages
		new CasMessageDAO<Message, Long>().deleteAllMessagesForUUID(uuid, mut);

		// delete media
		new CasMediaDAO<Media, Long>().deleteAllMediaForUUID(uuid, mut);

		// clean timesort cols
		cdu.removeTimesortColumn(null, CasDAOFactory.USERS_BY_TIMESTAMP,
			persistentUser.getId(), mut);

		// clean timesort rep sort cols
		cdu.removeNumbersortColumn(null, CasDAOFactory.USERS_BY_REPUTATION,
			persistentUser.getId(), persistentUser.getReputation(), mut);

		mut.execute();

		persistentUser.unindex();
    }

	public ArrayList<User> readAllSortedBy(String sortColumnFamilyName,
			MutableLong page, MutableLong itemcount, boolean reverse){
		return readAllSorted(sortColumnFamilyName, page, itemcount, reverse);
	}

	private <N> ArrayList<User> readAllSorted(String sortColumnFamilyName,
			MutableLong page, MutableLong itemcount, boolean reverse){

		CF<N> colFamily = null;
		N startKey = null;
		Class<N> colNameClass = null;
		//check if the sort order is defined as a column family
		if(sortColumnFamilyName.equalsIgnoreCase("timestamp")){
			colNameClass = (Class<N>) Long.class;
			colFamily = (CF<N>) CasDAOFactory.USERS_BY_TIMESTAMP;
			startKey = (N) CasDAOUtils.toLong(page);
		}else if(sortColumnFamilyName.equalsIgnoreCase("reputation")){
			colNameClass = (Class<N>) String.class;
			colFamily = (CF<N>) CasDAOFactory.USERS_BY_REPUTATION;
			String rep = cdu.getColumn(page.toString(),	CasDAOFactory.USERS, "reputation");
			if(rep != null){
				startKey = (N) rep.concat(CasDAOFactory.SEPARATOR).concat(page.toString());
			}
		}else{
			return new ArrayList<User>();
		}

		return cdu.readAll(User.class, CasDAOFactory.DEFAULT_KEY,
			colFamily, CasDAOFactory.USERS, colNameClass, startKey, page, itemcount,
			CasDAOFactory.MAX_ITEMS_PER_PAGE, reverse, false, false);
    }

    /**********************************************************************
     *                  METHODS FOR READING CONTACTS
     **********************************************************************/

    //Get all contacts for user with userid = id
    public ArrayList<User> readAllContactsForUser(Long userid, MutableLong page,
			MutableLong itemcount){

		return cdu.readAll(User.class, userid.toString(),
			CasDAOFactory.CONTACTS, CasDAOFactory.USERS, Long.class,
			CasDAOUtils.toLong(page), page, itemcount,
			CasDAOFactory.MAX_ITEMS_PER_PAGE, false, true, true);
    }

	public boolean isFriendWith (Long userid, User contact){
		return cdu.existsColumn(userid.toString(),
				CasDAOFactory.CONTACTS, contact.getId());
	}

	public boolean userExists (Long userid) {
		return cdu.existsColumn(userid.toString(), 
				CasDAOFactory.USERS, CasDAOFactory.CN_ID);
	}

	public boolean userExists (String identifier) {
		if(StringUtils.isBlank(identifier)) return false;
		List<?> row = null;
		if(identifier.startsWith("http") || NumberUtils.isDigits(identifier)){
			//identifier is an openid url
			row = cdu.readRow(identifier, CasDAOFactory.AUTH_KEYS,
					String.class, null, null, null, 1, true);
		}else if(identifier.contains("@")){
			row = cdu.readRow(identifier, CasDAOFactory.EMAILS,
					String.class, null, null, null, 1, true);
		}

		return (row != null && !row.isEmpty());
	}

	public int countContacts (Long userid){
		return cdu.countColumns(userid.toString(),
				CasDAOFactory.CONTACTS, Long.class);
	}

	public int createContactForUser (Long userid, User contact) {
		int count = countContacts(userid);
		if(count > CasDAOFactory.MAX_CONTACTS_PER_USER) return count;

		cdu.putColumn(userid.toString(), CasDAOFactory.CONTACTS,
				contact.getId(), contact.getUuid());

		return ++count;
	}

	public int deleteContactForUser (Long userid, User contact) {
		int count = countContacts(userid);

		cdu.removeColumn(userid.toString(),
				CasDAOFactory.CONTACTS, contact.getId());

		return --count;
	}

    /**********************************************************************
     *                 METHODS FOR READING CLASSUNITS
     **********************************************************************/

    public ArrayList<Classunit> readAllClassUnitsForUser(Long userid, MutableLong page,
			MutableLong itemcount){

		return cdu.readAll(Classunit.class, userid.toString(), 
			CasDAOFactory.USER_CLASSES, CasDAOFactory.CLASSES, Long.class,
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
				CasDAOFactory.USER_SCHOOLS, Long.class,
				CasDAOUtils.toLong(page), page, itemcount, howMany, true);

		Map<Long, String> extraProps = new HashMap<Long, String>();

		for (HColumn<Long, String> hColumn : list) {
			extraProps.put(hColumn.getName(), hColumn.getValue());
			keys.add(hColumn.getName().toString());
		}

		ArrayList<School> schools = cdu.readAll(School.class, keys, 
				CasDAOFactory.SCHOOLS);


		//assign additional properties like from/to year
		String fyear = "0";
		String tyear = "0";
		for (School school : schools) {
			String linkDetails = extraProps.get(school.getId());
			int sepindx = linkDetails.indexOf(CasDAOFactory.SEPARATOR);
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
		ArrayList<User> user = cdu.readAll(User.class, identifier, 
				CasDAOFactory.AUTH_KEYS, CasDAOFactory.USERS, String.class,
				null, null, null, 1, true, false, false);

		if(user == null || user.isEmpty()) return null;

		return user.get(0);
    }

    public void attachIdentifierToUser(String identifier, Long userid) {
		Mutator<String> mut = CasDAOUtils.createMutator();
		attachIdentifierToUser(identifier, userid, mut);
		mut.execute();
	}

    private void attachIdentifierToUser(String identifier, Long userid, Mutator<String> mut) {
		if(StringUtils.isBlank(identifier) || userid == null) return;
		CasDAOUtils.addInsertion(new Column(userid.toString(), CasDAOFactory.USER_AUTH,
				identifier, identifier), mut);
		CasDAOUtils.addInsertion(new Column(identifier, CasDAOFactory.AUTH_KEYS,
				userid.toString(), userid.toString()), mut);
    }

    public void deleteAllOpenidsForUser (Long userid) {
		Mutator<String> mut = CasDAOUtils.createMutator();
		cdu.deleteRow(userid.toString(), CasDAOFactory.USER_AUTH, mut);

		for (String identifier : readAllIdentifiersForUser(userid)) {
			cdu.deleteRow(identifier, CasDAOFactory.AUTH_KEYS, mut);
		}
		
		mut.execute();
    }

    public void detachIdentifierFromUser (String openidurl, Long userid) {
		CasDAOUtils.batchRemove(
			new Column(openidurl, CasDAOFactory.AUTH_KEYS, userid.toString(), null),
			new Column(userid.toString(), CasDAOFactory.USER_AUTH, openidurl, null)
		);
    }

	public ArrayList<User> readAllForKeys (ArrayList<String> keys) {
		return cdu.readAll(User.class, keys, CasDAOFactory.USERS);
	}

	public ArrayList<String> getFavouriteTagsForUser(Long userid) {
		String favtags = cdu.getColumn(userid.toString(),
				CasDAOFactory.USERS, "favtags");

		User u = new User();
		u.setFavtags(favtags);
		return u.getFavtagsList();
	}

	public ArrayList<Post> readAllPostsForUser(Long userid, PostType type,
			MutableLong page, MutableLong itemcount) {

		CF<Long> cf = null;
		if (type == PostType.ANSWER) {
			cf = CasDAOFactory.USER_ANSWERS;
		} else if(type == PostType.QUESTION) {
			cf = CasDAOFactory.USER_QUESTIONS;
		}else{
			return new ArrayList<Post> ();
		}

		return cdu.readAll(Post.class, userid.toString(),
			cf, CasDAOFactory.POSTS, Long.class,
			CasDAOUtils.toLong(page), page, itemcount,
			CasDAOFactory.MAX_ITEMS_PER_PAGE, true, false, true);
	}
}

