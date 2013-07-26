/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.db.cassandra;

import com.erudika.para.utils.DAO;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Group;
import com.erudika.scoold.db.AbstractGroupDAO;
import com.erudika.scoold.db.cassandra.CasDAOFactory.Column;
import com.erudika.scoold.util.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;

/**
 *
 * @author alexb
 */
final class CasGroupDAO<T> extends AbstractGroupDAO<Group> {

	private static final Logger logger = Logger.getLogger(CasGroupDAO.class.getName());
	private CasDAOUtils cdu = new CasDAOUtils(CasDAOFactory.CASSANDRA_PORT);
	
	public CasGroupDAO () {
    }
	
	public Group read(String id) {
		return cdu.read(Group.class, id);
	}

	public String create(Group newGroup) {
		return cdu.create(newGroup);
	}

	public void update(Group transientGroup) {
		cdu.update(transientGroup);
	}

	public void delete(Group persistentGroup) {
		String id = persistentGroup.getId();
		Mutator<String> mut = cdu.createMutator();

		cdu.delete(persistentGroup, mut);
		
		// remove all users from this group
		List<HColumn<String, String>> userids = cdu.readRow(id,
				CasDAOFactory.USERS_PARENTS, String.class, null, null, null,
				Utils.DEFAULT_LIMIT, false);

		for (HColumn<String, String> hColumn : userids) {
			cdu.addDeletion(new Column(hColumn.getName(),
					CasDAOFactory.GROUPS_PARENTS, id, null), mut);
		}
		
		cdu.addDeletion(new Column<String, String>(id, 
				CasDAOFactory.USERS_PARENTS), mut);
		
		mut.execute();
	}
	
	public boolean createUserGroupLink(String userid, Group group) {
		List<Column> list = getGroupLinkColumnsWithChecks(userid, group);
		cdu.batchPut(list);

		return !list.isEmpty();
	}
	
	public void createUserGroupLinks(ArrayList<String> userids, Group group) {
		Mutator<String> mut = cdu.createMutator();
		for (String id : userids) {
			cdu.addInsertions(getGroupLinkColumnsWithChecks(id, group), mut);
		}
		mut.execute();
	}
	
	private List<Column> getGroupLinkColumnsWithChecks(String userid, Group group){
		ArrayList<Column> list = new ArrayList<Column>();
		String groupid = group.getId();
		if(groupid != null && !isLinkedToUser(groupid, userid)){
			int count = cdu.countColumns(userid,
					CasDAOFactory.GROUPS_PARENTS, String.class);

			if(count < Constants.MAX_GROUPS_PER_USER)	{
				list.add(new Column<String, String>(userid, CasDAOFactory.GROUPS_PARENTS, groupid, groupid));
				list.add(new Column<String, String>(groupid, CasDAOFactory.USERS_PARENTS, userid, userid));
			}
		}

		return list;
	}

	public void deleteUserGroupLink(String userid, Group group) {
		ArrayList<Column> list = new ArrayList<Column>();
		String groupid = group.getId();

		list.add(new Column<String, String>(userid, CasDAOFactory.GROUPS_PARENTS, groupid, groupid));
		list.add(new Column<String, String>(groupid, CasDAOFactory.USERS_PARENTS, userid, userid));
		cdu.batchRemove(list);
	}

	public boolean isLinkedToUser(String groupid, String userid) {
		return cdu.existsColumn(groupid, CasDAOFactory.USERS_PARENTS, userid);
	}

	public boolean groupExists(String groupid) {
		return cdu.existsColumn(groupid, CasDAOFactory.OBJECTS, DAO.CN_ID);
	}
	
	public int countUsersForGroup(String groupid) {
		return cdu.countColumns(groupid, CasDAOFactory.USERS_PARENTS, String.class);
	}
}
