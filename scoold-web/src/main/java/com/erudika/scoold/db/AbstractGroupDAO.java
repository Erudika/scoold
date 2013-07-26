/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.db;

import com.erudika.scoold.core.Group;
import java.util.ArrayList;

/**
 *
 * @author alexb
 */
public abstract class AbstractGroupDAO<T extends Group>
        implements GenericDAO<Group> {

	public abstract boolean createUserGroupLink(String userid, T group);
    public abstract void deleteUserGroupLink(String userid, T group);
	public abstract void createUserGroupLinks(ArrayList<String> userids, T group);
	public abstract boolean isLinkedToUser(String groupid, String userid);
	public abstract boolean groupExists(String groupid);
	public abstract int countUsersForGroup(String groupid);
}
