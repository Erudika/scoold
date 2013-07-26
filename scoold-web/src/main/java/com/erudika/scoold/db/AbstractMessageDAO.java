/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.db;

import com.erudika.scoold.core.Message;
import java.util.ArrayList;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 *
 * @param <T>
 * @param <String> 
 * @author alexb
 */
public abstract class AbstractMessageDAO<T extends Message>
        implements GenericDAO<Message> {

	public abstract void deleteAllMessagesForID(String parentid);
    public abstract ArrayList<T> readAllMessagesForID(String id, MutableLong page, MutableLong itemcount);
	public abstract int countNewMessagesForID(String parentid);
	public abstract void markAllAsReadForID(String parentid);
}
