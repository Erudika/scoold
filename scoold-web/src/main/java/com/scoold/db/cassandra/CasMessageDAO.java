/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db.cassandra;

import com.scoold.core.Message;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractMessageDAO;
import com.scoold.db.cassandra.CasDAOFactory.Column;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang.mutable.MutableLong;
/**
 *
 * @author alexb
 */
final class CasMessageDAO<T, PK> extends AbstractMessageDAO<Message, Long> {

    private static final Logger logger = Logger.getLogger(CasMessageDAO.class.getName());
	private CasDAOUtils cdu = (CasDAOUtils) CasDAOFactory.getInstance().getDAOUtils();
	private String newMsgColumnSuffix = AbstractDAOFactory.SEPARATOR.concat("new");
	
	
    public CasMessageDAO() {
	}

    public Message read (Long id) {
		return cdu.read(Message.class, id.toString());
    }

    public Long create (Message newMessage) {
		Long parentid = newMessage.getToid();
		
		boolean existsUser = cdu.existsColumn(parentid.toString(), 
				CasDAOFactory.OBJECTS, CasDAOFactory.CN_ID);

		int count = cdu.countColumns(parentid.toString(),
				CasDAOFactory.MESSAGES_PARENTS, Long.class);

		if(!existsUser || count > CasDAOFactory.MAX_MESSAGES_PER_USER) return 0L;

		Mutator<String> mut = cdu.createMutator();
		Long id = cdu.create(newMessage, mut);
		String idstr = id.toString();
		String newMsgColumn = parentid.toString().concat(newMsgColumnSuffix);

		if(id != null){
			cdu.addInsertions(Arrays.asList(new Column[]{
				new Column(newMsgColumn, CasDAOFactory.MESSAGES_PARENTS, id, idstr),
				new Column(parentid.toString(), CasDAOFactory.MESSAGES_PARENTS, id, idstr)
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
		Long parentid = persistentMessage.getToid();
		String newMsgColumn = parentid.toString().concat(newMsgColumnSuffix);
		// delete linker row
		cdu.addDeletion(new Column<Long, String>(newMsgColumn,
				CasDAOFactory.MESSAGES_PARENTS, persistentMessage.getId(), null), mut);
		cdu.addDeletion(new Column<Long, String>(parentid.toString(), 
				CasDAOFactory.MESSAGES_PARENTS, persistentMessage.getId(), null), mut);
				
		mut.execute();
    }

	public void deleteAllMessagesForID (Long parentid) {
		Mutator<String> mut = cdu.createMutator();
		deleteAllMessagesForID(parentid, mut);
		mut.execute();
	}

	protected void deleteAllMessagesForID (Long parentid, Mutator<String> mut) {
		List<HColumn<Long, String>> keys = cdu.readRow(parentid.toString(),
				CasDAOFactory.MESSAGES_PARENTS, Long.class,
				null, null, null, CasDAOFactory.DEFAULT_LIMIT, false);
		
		for (HColumn<Long, String> hColumn : keys) {
			cdu.addDeletion(new Column<String, String>(hColumn.getName().toString(), 
					CasDAOFactory.OBJECTS), mut);
		}
		
		String newMsgColumn = parentid.toString().concat(newMsgColumnSuffix);
		
		cdu.addDeletion(new Column<Long, String>(newMsgColumn,
			CasDAOFactory.MESSAGES_PARENTS), mut);
		
		cdu.addDeletion(new Column<Long, String>(parentid.toString(), 
				CasDAOFactory.MESSAGES_PARENTS), mut);


	}
	
	public ArrayList<Message> readAllMessagesForID (Long parentid, MutableLong page,
			MutableLong itemcount) {

		ArrayList<Message> messages = cdu.readAll(Message.class, null,
				parentid.toString(), CasDAOFactory.MESSAGES_PARENTS, 
				Long.class, CasDAOUtils.toLong(page),
				page, itemcount, CasDAOFactory.MAX_ITEMS_PER_PAGE, true, false, true);

		if(messages == null || messages.isEmpty())
			return new ArrayList<Message> ();

		String newMsgColumn = parentid.toString().concat(newMsgColumnSuffix);
		
		// get the ids of unread messages so that we can fil
		List<HColumn<Long, String>> row = cdu.readRow(newMsgColumn,
				CasDAOFactory.MESSAGES_PARENTS, Long.class, null, null, null,
				CasDAOFactory.DEFAULT_LIMIT, true);
		
		Map<Long, Integer> newIdsMap = new HashMap<Long, Integer>();
		//map contains new msg ids
		for (HColumn<Long, String> hColumn : row) {
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

	public int countNewMessagesForID (Long parentid){
		String newMsgColumn = parentid.toString().concat(newMsgColumnSuffix);
		return cdu.countColumns(newMsgColumn, CasDAOFactory.MESSAGES_PARENTS, Long.class);
	}

	public void markAllAsReadForID (Long parentid){
		String newMsgColumn = parentid.toString().concat(newMsgColumnSuffix);
		Mutator<String> mut = cdu.createMutator();
		cdu.deleteRow(newMsgColumn, CasDAOFactory.MESSAGES_PARENTS, mut);
		mut.execute();
	}
}
