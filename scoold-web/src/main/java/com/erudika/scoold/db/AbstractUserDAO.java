/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.db;

import com.erudika.scoold.core.School;
import com.erudika.scoold.core.Group;
import com.erudika.scoold.core.Classunit;
import com.erudika.scoold.core.User;
import java.util.ArrayList;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 *
 * @param <T>
 * @param <String>
 * @author alexb
 */
public abstract class AbstractUserDAO<T extends User>
        implements GenericDAO<User> {

    public abstract T readUserForIdentifier(String openidURL);
	public abstract ArrayList<String> readAllIdentifiersForUser(String userid);
    public abstract void attachIdentifierToUser(String openidurl, String userid);
    public abstract void detachIdentifierFromUser(String openidurl, String userid);
    public abstract void deleteAllOpenidsForUser(String userid);
	
    public abstract ArrayList<Classunit> readAllClassUnitsForUser(String userid, MutableLong page, MutableLong itemcount);
    public abstract ArrayList<T> readAllUsersForID(String parentid, MutableLong page, MutableLong itemcount);
    public abstract ArrayList<Group> readAllGroupsForUser(String userid, MutableLong page, MutableLong itemcount);
    public abstract ArrayList<School> readAllSchoolsForUser(String userid, MutableLong page, MutableLong itemcount, int howMany);

	public abstract boolean isFriendWith(String userid, String contactid);
	public abstract int createContactForUser(String userid, String contactid);
	public abstract int deleteContactForUser(String userid, String contactid);
	public abstract boolean userExists(String userid);
//	public abstract boolean userExists(String identifier);
	public abstract int countContacts(String userid);

	public abstract ArrayList<String> getFavouriteTagsForUser(String userid);
}
