
package com.scoold.db;

import com.scoold.core.Classunit;
import com.scoold.core.User;
import java.util.ArrayList;
import org.apache.commons.lang.mutable.MutableLong;

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
    public abstract ArrayList<User> readAllUsersForClassUnit(PK classid, MutableLong page, MutableLong itemcount);
    public abstract int countUsersForClassUnit(PK classid);
	public abstract boolean isLinkedToUser(PK classid, PK userid);

	public abstract ArrayList<T> readLatestClasses(int howMany);

	public abstract boolean classExists(PK classid);

	public abstract boolean mergeClasses(PK primaryClassid, PK duplicateClassid);

	public abstract ArrayList<T> readAllForKeys(ArrayList<String> keys);
}
