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

	public abstract void deleteAllMessagesForID(Long parentid);
    public abstract ArrayList<T> readAllMessagesForID(Long id, MutableLong page, MutableLong itemcount);
	public abstract int countNewMessagesForID(Long parentid);
	public abstract void markAllAsReadForID(Long parentid);
}
