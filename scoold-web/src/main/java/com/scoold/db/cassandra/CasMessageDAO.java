/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db.cassandra;

import com.scoold.core.Message;
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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableLong;
/**
 *
 * @author alexb
 */
public final class CasMessageDAO<T, PK> extends AbstractMessageDAO<Message, Long> {

    private static final Logger logger = Logger.getLogger(CasMessageDAO.class.getName());
	private CasDAOUtils cdu = new CasDAOUtils();
	
    public CasMessageDAO() {
	}

    public Message read (Long id) {
		return cdu.read(Message.class, id.toString(), CasDAOFactory.MESSAGES);
    }

	public Message read (String uuid) {
        ArrayList<Message> message = cdu.readAll(Message.class, uuid,
				CasDAOFactory.MESSAGES_UUIDS, CasDAOFactory.MESSAGES, String.class,
				null, null, null, 1, false, false, false);

		if(message == null || message.isEmpty()) return null;

		return message.get(0);
    }

    public Long create (Message newMessage) {
		String parentUUID = newMessage.getTouuid();
		List<?> row = cdu.readRow(parentUUID, CasDAOFactory.USERS_UUIDS,
				String.class, null, null, null, 1, true);

		boolean existsUser = (row != null && !row.isEmpty());

		int count = cdu.countColumns(parentUUID,
				CasDAOFactory.MESSAGES_PARENTUUIDS, Long.class);

		if(!existsUser || count > CasDAOFactory.MAX_MESSAGES_PER_USER) return null;

		Mutator<String> mut = CasDAOUtils.createMutator();
		Long id = cdu.create(newMessage, CasDAOFactory.MESSAGES, mut);
		String idstr = id.toString();

		if(id != null){
			CasDAOUtils.addInsertions(Arrays.asList(new Column[]{
				new Column(parentUUID, CasDAOFactory.NEW_MESSAGES, id, idstr),
				new Column(newMessage.getUuid(), CasDAOFactory.MESSAGES_UUIDS, idstr, idstr),
				new Column(parentUUID, CasDAOFactory.MESSAGES_PARENTUUIDS, id, idstr)
			}), mut);
		}

		mut.execute();

		return id;
    }

    public void update (Message transientMessage) {
		cdu.update(transientMessage, CasDAOFactory.MESSAGES);
    }

    public void delete (Message persistentMessage) {
		if(persistentMessage.getTouuid() == null || persistentMessage.getId() == null)
			return;
		// delete the message object
		Mutator<String> mut = CasDAOUtils.createMutator();
		cdu.delete(persistentMessage, CasDAOFactory.MESSAGES, mut);

		// delete linker row
		CasDAOUtils.addDeletion(new Column<Long, String>(persistentMessage.getTouuid(), 
				CasDAOFactory.MESSAGES_PARENTUUIDS, persistentMessage.getId(), null), mut);
		CasDAOUtils.addDeletion(new Column<Long, String>(persistentMessage.getTouuid(),
				CasDAOFactory.NEW_MESSAGES, persistentMessage.getId(), null), mut);

		cdu.deleteRow(persistentMessage.getUuid(), CasDAOFactory.MESSAGES_UUIDS, mut);
				
		mut.execute();
    }

	public void deleteAllMessagesForUUID (String parentUUID) {
		Mutator<String> mut = CasDAOUtils.createMutator();
		deleteAllMessagesForUUID(parentUUID, mut);
		mut.execute();
	}

	protected void deleteAllMessagesForUUID (String parentUUID, Mutator<String> mut) {
		List<HColumn<Long, String>> keys = cdu.readRow(parentUUID,
				CasDAOFactory.MESSAGES_PARENTUUIDS, Long.class,
				null, null, null, CasDAOFactory.DEFAULT_LIMIT, false);
		
		for (HColumn<Long, String> hColumn : keys) {
			CasDAOUtils.addDeletion(new Column<String, String>(hColumn.getName().toString(), 
					CasDAOFactory.MESSAGES), mut);
		}
		
		CasDAOUtils.addDeletion(new Column<Long, String>(parentUUID,
				CasDAOFactory.MESSAGES_PARENTUUIDS), mut);

		CasDAOUtils.addDeletion(new Column<Long, String>(parentUUID,
			CasDAOFactory.NEW_MESSAGES), mut);

	}
	
	public ArrayList<Message> readAllMessagesForUUID (String parentUUID, MutableLong page,
			MutableLong itemcount) {

		ArrayList<Message> messages = cdu.readAll(Message.class,
				parentUUID, CasDAOFactory.MESSAGES_PARENTUUIDS,
				CasDAOFactory.MESSAGES, Long.class, CasDAOUtils.toLong(page),
				page, itemcount, CasDAOFactory.MAX_ITEMS_PER_PAGE, true, false, true);

		if(messages == null || messages.isEmpty())
			return new ArrayList<Message> ();

		// get the ids of unread messages so that we can fil
		List<HColumn<Long, String>> row = cdu.readRow(parentUUID,
				CasDAOFactory.NEW_MESSAGES, Long.class, null, null, null,
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

	public int countNewMessagesForUUID (String uuid){
		if(StringUtils.isBlank(uuid)) return 0;
		return cdu.countColumns(uuid, CasDAOFactory.NEW_MESSAGES,
				Long.class);
	}

	public void markAllAsReadForUUID (String uuid){
		Mutator<String> mut = CasDAOUtils.createMutator();
		cdu.deleteRow(uuid, CasDAOFactory.NEW_MESSAGES, mut);
		mut.execute();
	}

	public ArrayList<Message> readAllSortedBy(String sortColumnFamilyName,
			MutableLong page, MutableLong itemcount, boolean reverse) {
		
		throw new UnsupportedOperationException("not supported");
//		return cdu.readSlice(Message.class, CasDAOFactory.DEFAULT_KEY,
//				sortColumnFamilyName,
//				CasDAOFactory.MESSAGES,
//				page, itemcount, reverse, false);
	}

}
