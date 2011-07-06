/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db;

import com.scoold.core.Classunit;
import com.scoold.core.Post;
import com.scoold.core.Post.PostType;
import com.scoold.core.School;
import com.scoold.core.User;
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

	public abstract T readUserByEmail(String identifier);

    public abstract T readUserForIdentifier(String openidURL);
	public abstract ArrayList<String> readAllIdentifiersForUser(PK userid);
    public abstract void attachIdentifierToUser(String openidurl, PK userid);
    public abstract void detachIdentifierFromUser(String openidurl, PK userid);
    public abstract void deleteAllOpenidsForUser(PK userid);

    public abstract ArrayList<Classunit> readAllClassUnitsForUser(PK userid, MutableLong page, MutableLong itemcount);
    public abstract ArrayList<T> readAllContactsForUser(PK userid, MutableLong page, MutableLong itemcount);
    
    public abstract ArrayList<School> readAllSchoolsForUser(PK userid, MutableLong page, MutableLong itemcount, int howMany);

	public abstract boolean isFriendWith(PK userid, T contact);
	public abstract int createContactForUser(PK userid, T contact);
	public abstract int deleteContactForUser(PK userid, T contact);
	public abstract boolean userExists(PK userid);
	public abstract boolean userExists(String identifier);
	public abstract int countContacts(PK userid);

	public abstract ArrayList<T> readAllForKeys(ArrayList<String> keys);
	public abstract ArrayList<String> getFavouriteTagsForUser(PK userid);

	public abstract ArrayList<Post> readAllPostsForUser(PK userid, PostType type, MutableLong page, MutableLong itemcount);
}
