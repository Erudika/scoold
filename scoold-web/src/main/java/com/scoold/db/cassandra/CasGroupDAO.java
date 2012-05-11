/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scoold.db.cassandra;

import com.scoold.core.Group;
import com.scoold.db.AbstractGroupDAO;
import com.scoold.db.cassandra.CasDAOFactory.Column;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;

/**
 *
 * @author alexb
 */
final class CasGroupDAO<T, PK> extends AbstractGroupDAO<Group, Long> {

	private static final Logger logger = Logger.getLogger(CasGroupDAO.class.getName());
	private CasDAOUtils cdu = (CasDAOUtils) CasDAOFactory.getInstance().getDAOUtils();
	
	public CasGroupDAO () {
    }
	
	public Group read(Long id) {
		return cdu.read(Group.class, id.toString());
	}

	public Long create(Group newGroup) {
		return cdu.create(newGroup);
	}

	public void update(Group transientGroup) {
		cdu.update(transientGroup);
	}

	public void delete(Group persistentGroup) {
		Long id = persistentGroup.getId();
		Mutator<String> mut = cdu.createMutator();

		cdu.delete(persistentGroup, mut);
		
		// remove all users from this group
		List<HColumn<Long, String>> userids = cdu.readRow(id.toString(),
				CasDAOFactory.USERS_PARENTS, Long.class, null, null, null,
				CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<Long, String> hColumn : userids) {
			cdu.addDeletion(new Column(hColumn.getName().toString(),
					CasDAOFactory.GROUPS_PARENTS, id, null), mut);
		}
		
		cdu.addDeletion(new Column<Long, String>(id.toString(), 
				CasDAOFactory.USERS_PARENTS), mut);
		
		mut.execute();
	}
	
	public boolean createUserGroupLink(Long userid, Group group) {
		List<Column> list = getGroupLinkColumnsWithChecks(userid, group);
		cdu.batchPut(list);

		return !list.isEmpty();
	}
	
	public void createUserGroupLinks(ArrayList<Long> userids, Group group) {
		Mutator<String> mut = cdu.createMutator();
		for (Long id : userids) {
			cdu.addInsertions(getGroupLinkColumnsWithChecks(id, group), mut);
		}
		mut.execute();
	}
	
	private List<Column> getGroupLinkColumnsWithChecks(Long userid, Group group){
		ArrayList<Column> list = new ArrayList<Column>();
		Long groupid = group.getId();
		if(groupid != null && !isLinkedToUser(groupid, userid)){
			int count = cdu.countColumns(userid.toString(),
					CasDAOFactory.GROUPS_PARENTS, Long.class);

			if(count < CasDAOFactory.MAX_GROUPS_PER_USER)	{
				list.add(new Column<Long, String>(userid.toString(),
					CasDAOFactory.GROUPS_PARENTS, groupid, groupid.toString()));
				list.add(new Column<Long, String>(groupid.toString(),
					CasDAOFactory.USERS_PARENTS, userid, userid.toString()));
			}
		}

		return list;
	}

	public void deleteUserGroupLink(Long userid, Group group) {
		ArrayList<Column> list = new ArrayList<Column>();
		Long groupid = group.getId();

		list.add(new Column<Long, String>(userid.toString(),
				CasDAOFactory.GROUPS_PARENTS, groupid, groupid.toString()));
		list.add(new Column<Long, String>(groupid.toString(),
				CasDAOFactory.USERS_PARENTS, userid, userid.toString()));
		cdu.batchRemove(list);
	}

	public boolean isLinkedToUser(Long groupid, Long userid) {
		return cdu.existsColumn(groupid.toString(),	CasDAOFactory.USERS_PARENTS, userid);
	}

	public boolean groupExists(Long groupid) {
		return cdu.existsColumn(groupid.toString(),
				CasDAOFactory.OBJECTS, CasDAOFactory.CN_ID);
	}
	
	public int countUsersForGroup(Long groupid) {
		return cdu.countColumns(groupid.toString(),
				CasDAOFactory.USERS_PARENTS, Long.class);
	}
}
