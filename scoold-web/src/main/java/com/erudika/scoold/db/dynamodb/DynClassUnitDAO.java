package com.erudika.scoold.db.dynamodb;

import com.erudika.scoold.core.Classunit;
import com.erudika.scoold.db.AbstractClassUnitDAO;
import me.prettyprint.hector.api.mutation.Mutator;

/**
 *
 * @author alexb
 */
final class DynClassUnitDAO<T> extends AbstractClassUnitDAO<Classunit>{
	
    public DynClassUnitDAO () {
	}

    public Classunit read(String id) {
		return null;
    }

    public String create(Classunit newClassUnit) {
		return "";
    }

    public void update(Classunit transientClassUnit) {
//		cdu.update(transientClassUnit);
    }

    public void delete(Classunit persistentClassUnit) {
//		Mutator<String> mut = cdu.createMutator();
//		deleteClass(persistentClassUnit, mut);
//		mut.execute();
    }

	protected void deleteClass(Classunit persistentClassUnit, Mutator<String> mut){
		
	}

    public void deleteUserClassLink (String userid, String classid){
		
    }

	public boolean isLinkedToUser (String classid, String userid) {
		return false;
	}

	public boolean classExists (String classid) {
		return false;
	}

	public boolean mergeClasses (String primaryClassid, String duplicateClassid){
		return false;
	}

	public int countUsersForClassUnit(String classid) {
		return 0;
	}

	public String sendChat(String primaryClassid, String chat) {
		return null;
	}

	public String receiveChat(String primaryClassid) {
		return null;
	}

	public boolean createUserClassLink(String userid, Classunit klass) {
		return false;
	}

}

