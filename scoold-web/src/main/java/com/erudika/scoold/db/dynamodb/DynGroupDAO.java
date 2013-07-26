/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.db.dynamodb;

import com.erudika.scoold.core.Group;
import com.erudika.scoold.db.AbstractGroupDAO;
import com.erudika.scoold.db.cassandra.CasDAOFactory.Column;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author alexb
 */
final class DynGroupDAO<T> extends AbstractGroupDAO<Group> {

	
	public DynGroupDAO () {
    }
	
	public Group read(String id) {
//		return cdu.read(Group.class, id);
		return null;
	}

	public String create(Group newGroup) {
//		return cdu.create(newGroup);
		return "";
	}

	public void update(Group transientGroup) {
//		cdu.update(transientGroup);
	}

	public void delete(Group persistentGroup) {
		
	}
	
	public boolean createUserGroupLink(String userid, Group group) {
		return false;
	}
	
	public void createUserGroupLinks(ArrayList<String> userids, Group group) {
		
	}
	
	private List<Column> getGroupLinkColumnsWithChecks(String userid, Group group){
//		ArrayList<Column> list = new ArrayList<Column>();
//		String groupid = group.getId();
//		if(groupid != null && !isLinkedToUser(groupid, userid)){
//			int count = cdu.countColumns(userid,
//					CasDAOFactory.GROUPS_PARENTS, String.class);
//
//			if(count < CasDAOFactory.MAX_GROUPS_PER_USER)	{
//				list.add(new Column<String, String>(userid, CasDAOFactory.GROUPS_PARENTS, groupid, groupid));
//				list.add(new Column<String, String>(groupid, CasDAOFactory.USERS_PARENTS, userid, userid));
//			}
//		}
//
//		return list;
		return null;
	}

	public void deleteUserGroupLink(String userid, Group group) {
		
	}

	public boolean isLinkedToUser(String groupid, String userid) {
//		return cdu.existsColumn(groupid, CasDAOFactory.USERS_PARENTS, userid);
		return false;
	}

	public boolean groupExists(String groupid) {
//		return cdu.existsColumn(groupid, CasDAOFactory.OBJECTS, DAO.CN_ID);
		return false;
	}
	
	public int countUsersForGroup(String groupid) {
//		return cdu.countColumns(groupid, CasDAOFactory.USERS_PARENTS, String.class);
		return 0;
	}
}
