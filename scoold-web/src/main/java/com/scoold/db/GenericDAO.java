package com.scoold.db;

import java.util.ArrayList;
import org.apache.commons.lang.mutable.MutableLong;

/**
 * 
 * @author alexb
 * @param <T>
 * @param <Long>
 */
	public interface GenericDAO <T, PK> {

    public T read (Long id);

    public T read (String uuid);

	public ArrayList<T> readAllSortedBy(String field, MutableLong page, MutableLong itemcount, boolean desc);

    public PK create (T newInstance);

    public void update (T transientObject);

    public void delete (T persistentObject);

}

