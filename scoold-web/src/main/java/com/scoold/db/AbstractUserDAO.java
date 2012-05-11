/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db;

import com.scoold.core.*;
import java.util.ArrayList;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @param <T>
 * @param <PK>
 * @author alexb
 */
public abstract class AbstractUserDAO<T extends User, PK>
        implements GenericDAO<User, Long> {

    public abstract T readUserForIdentifier(String openidURL);
	public abstract ArrayList<String> readAllIdentifiersForUser(PK userid);
    public abstract void attachIdentifierToUser(String openidurl, PK userid);
    public abstract void detachIdentifierFromUser(String openidurl, PK userid);
    public abstract void deleteAllOpenidsForUser(PK userid);
	
    public abstract ArrayList<Classunit> readAllClassUnitsForUser(PK userid, MutableLong page, MutableLong itemcount);
    public abstract ArrayList<T> readAllUsersForID(PK parentid, MutableLong page, MutableLong itemcount);
    public abstract ArrayList<Group> readAllGroupsForUser(PK userid, MutableLong page, MutableLong itemcount);
    public abstract ArrayList<School> readAllSchoolsForUser(PK userid, MutableLong page, MutableLong itemcount, int howMany);

	public abstract boolean isFriendWith(PK userid, PK contactid);
	public abstract int createContactForUser(PK userid, PK contactid);
	public abstract int deleteContactForUser(PK userid, PK contactid);
	public abstract boolean userExists(PK userid);
	public abstract boolean userExists(String identifier);
	public abstract int countContacts(PK userid);

	public abstract ArrayList<String> getFavouriteTagsForUser(PK userid);
}
