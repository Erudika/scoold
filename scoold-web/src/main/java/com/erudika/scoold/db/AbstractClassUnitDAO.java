
package com.erudika.scoold.db;

import com.erudika.scoold.core.Classunit;

/**
 *
 * @param <String>
 * @param <T>
 * @author alexb
 */
public abstract class AbstractClassUnitDAO<T extends Classunit>
        implements GenericDAO<Classunit> {

	public abstract boolean createUserClassLink(String userid, T klass);
    public abstract void deleteUserClassLink(String userid, String classid);
    public abstract int countUsersForClassUnit(String classid);
	public abstract boolean isLinkedToUser(String classid, String userid);
	public abstract boolean classExists(String classid);
	public abstract boolean mergeClasses(String primaryClassid, String duplicateClassid);
	public abstract String sendChat(String primaryClassid, String chat);
	public abstract String receiveChat(String primaryClassid);
}
