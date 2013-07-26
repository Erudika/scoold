/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.db.dynamodb;

import com.erudika.scoold.core.Message;
import com.erudika.scoold.db.AbstractMessageDAO;
import java.util.ArrayList;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang3.mutable.MutableLong;
/**
 *
 * @author alexb
 */
final class DynMessageDAO<T> extends AbstractMessageDAO<Message> {
	
	
    public DynMessageDAO() {
	}

    public Message read (String id) {
//		return cdu.read(Message.class, id);
		return null;
    }

    public String create (Message newMessage) {
		return null;
    }

    public void update (Message transientMessage) {
		
    }

    public void delete (Message persistentMessage) {
    }

	public void deleteAllMessagesForID (String parentid) {
	}

	protected void deleteAllMessagesForID (String parentid, Mutator<String> mut) {
		
	}
	
	public ArrayList<Message> readAllMessagesForID (String parentid, MutableLong page,
			MutableLong itemcount) {
		return null;
	}

	public int countNewMessagesForID (String parentid){
		return 0;
	}

	public void markAllAsReadForID (String parentid){
	}
}
