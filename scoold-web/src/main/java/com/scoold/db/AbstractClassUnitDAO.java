
package com.scoold.db;

import com.scoold.core.Classunit;

/**
 *
 * @param <Long>
 * @param <T>
 * @author alexb
 */
public abstract class AbstractClassUnitDAO<T extends Classunit, PK>
        implements GenericDAO<Classunit, Long> {

	public abstract boolean createUserClassLink(PK userid, T klass);
    public abstract void deleteUserClassLink(PK userid, PK classid);
    public abstract int countUsersForClassUnit(PK classid);
	public abstract boolean isLinkedToUser(PK classid, PK userid);
	public abstract boolean classExists(PK classid);
	public abstract boolean mergeClasses(PK primaryClassid, PK duplicateClassid);
}
