/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scoold.db;

import com.scoold.core.Group;
import java.util.ArrayList;

/**
 *
 * @author alexb
 */
public abstract class AbstractGroupDAO<T extends Group, PK>
        implements GenericDAO<Group, Long> {

	public abstract boolean createUserGroupLink(PK userid, T group);
    public abstract void deleteUserGroupLink(PK userid, T group);
	public abstract void createUserGroupLinks(ArrayList<PK> userids, T group);
	public abstract boolean isLinkedToUser(PK groupid, PK userid);
	public abstract boolean groupExists(PK groupid);
	public abstract int countUsersForGroup(Long groupid);
}
