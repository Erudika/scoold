/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.db.cassandra;

import com.erudika.para.utils.DAO;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Message;
import com.erudika.scoold.db.AbstractMessageDAO;
import com.erudika.scoold.db.cassandra.CasDAOFactory.Column;
import com.erudika.scoold.util.Constants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang3.mutable.MutableLong;
/**
 *
 * @author alexb
 */
final class CasMessageDAO<T> extends AbstractMessageDAO<Message> {

    private static final Logger logger = Logger.getLogger(CasMessageDAO.class.getName());
	private CasDAOUtils cdu = new CasDAOUtils(CasDAOFactory.CASSANDRA_PORT);
	private String newMsgColumnSuffix = Utils.SEPARATOR.concat("new");
	
	
    public CasMessageDAO() {
	}

    public Message read (String id) {
		return cdu.read(Message.class, id);
    }

    public String create (Message newMessage) {
		String parentid = newMessage.getToid();
		
		boolean existsUser = cdu.existsColumn(parentid, 
				CasDAOFactory.OBJECTS, DAO.CN_ID);

		int count = cdu.countColumns(parentid,
				CasDAOFactory.MESSAGES_PARENTS, String.class);

		if(!existsUser || count > Constants.MAX_MESSAGES_PER_USER) return null;

		Mutator<String> mut = cdu.createMutator();
		String id = cdu.create(newMessage, mut);

		if(id != null){
			String idstr = id;
			String newMsgColumn = parentid.concat(newMsgColumnSuffix);
			cdu.addInsertions(Arrays.asList(new Column[]{
				new Column(newMsgColumn, CasDAOFactory.MESSAGES_PARENTS, id, idstr),
				new Column(parentid, CasDAOFactory.MESSAGES_PARENTS, id, idstr)
			}), mut);
		}

		mut.execute();

		return id;
    }

    public void update (Message transientMessage) {
		cdu.update(transientMessage);
    }

    public void delete (Message persistentMessage) {
		if(persistentMessage.getToid() == null || persistentMessage.getId() == null)
			return;
		// delete the message object
		Mutator<String> mut = cdu.createMutator();
		cdu.delete(persistentMessage, mut);
		String parentid = persistentMessage.getToid();
		String newMsgColumn = parentid.concat(newMsgColumnSuffix);
		// delete linker row
		cdu.addDeletion(new Column<String, String>(newMsgColumn,
				CasDAOFactory.MESSAGES_PARENTS, persistentMessage.getId(), null), mut);
		cdu.addDeletion(new Column<String, String>(parentid, 
				CasDAOFactory.MESSAGES_PARENTS, persistentMessage.getId(), null), mut);
				
		mut.execute();
    }

	public void deleteAllMessagesForID (String parentid) {
		Mutator<String> mut = cdu.createMutator();
		deleteAllMessagesForID(parentid, mut);
		mut.execute();
	}

	protected void deleteAllMessagesForID (String parentid, Mutator<String> mut) {
		List<HColumn<String, String>> keys = cdu.readRow(parentid,
				CasDAOFactory.MESSAGES_PARENTS, String.class,
				null, null, null, Utils.DEFAULT_LIMIT, false);
		
		for (HColumn<String, String> hColumn : keys) {
			cdu.addDeletion(new Column<String, String>(hColumn.getName(), 
					CasDAOFactory.OBJECTS), mut);
		}
		
		String newMsgColumn = parentid.concat(newMsgColumnSuffix);
		
		cdu.addDeletion(new Column<String, String>(newMsgColumn,
			CasDAOFactory.MESSAGES_PARENTS), mut);
		
		cdu.addDeletion(new Column<String, String>(parentid, 
				CasDAOFactory.MESSAGES_PARENTS), mut);


	}
	
	public ArrayList<Message> readAllMessagesForID (String parentid, MutableLong page,
			MutableLong itemcount) {

		ArrayList<Message> messages = cdu.readAll(Message.class, null,
				parentid, CasDAOFactory.MESSAGES_PARENTS, 
				String.class, Utils.toLong(page).toString(),
				page, itemcount, Utils.MAX_ITEMS_PER_PAGE, true, false, true);

		if(messages == null || messages.isEmpty())
			return new ArrayList<Message> ();

		String newMsgColumn = parentid.concat(newMsgColumnSuffix);
		
		// get the ids of unread messages so that we can fil
		List<HColumn<String, String>> row = cdu.readRow(newMsgColumn,
				CasDAOFactory.MESSAGES_PARENTS, String.class, null, null, null,
				Utils.DEFAULT_LIMIT, true);
		
		Map<String, Integer> newIdsMap = new HashMap<String, Integer>();
		//map contains new msg ids
		for (HColumn<String, String> hColumn : row) {
			newIdsMap.put(hColumn.getName(), 1);
		}
		// set isread flag
		for (Message message : messages) {
			if(newIdsMap.containsKey(message.getId())){
				message.setIsread(false);
			}else{
				message.setIsread(true);
			}
		}

		return messages;
	}

	public int countNewMessagesForID (String parentid){
		String newMsgColumn = parentid.concat(newMsgColumnSuffix);
		return cdu.countColumns(newMsgColumn, CasDAOFactory.MESSAGES_PARENTS, String.class);
	}

	public void markAllAsReadForID (String parentid){
		String newMsgColumn = parentid.concat(newMsgColumnSuffix);
		Mutator<String> mut = cdu.createMutator();
		cdu.deleteRow(newMsgColumn, CasDAOFactory.MESSAGES_PARENTS, mut);
		mut.execute();
	}
}
