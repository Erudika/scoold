/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db;

import com.scoold.core.Message;
import java.util.ArrayList;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @param <T>
 * @param <PK> 
 * @author alexb
 */
public abstract class AbstractMessageDAO<T extends Message, PK>
        implements GenericDAO<Message, Long> {

	public abstract void deleteAllMessagesForUUID(String parentUUID);
    public abstract ArrayList<T> readAllMessagesForUUID(String uuid, MutableLong page, MutableLong itemcount);
    
	public abstract int countNewMessages(PK userid);
	public abstract void markAllAsRead(PK userid);
}
