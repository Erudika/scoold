package com.erudika.scoold.db;

/**
 * 
 * @author alexb
 * @param <T>
 * @param <String>
 */
	public interface GenericDAO <T> {

    public T read (String id);

    public String create (T newInstance);

    public void update (T transientObject);

    public void delete (T persistentObject);

}

