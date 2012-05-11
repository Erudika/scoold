package com.scoold.db;

/**
 * 
 * @author alexb
 * @param <T>
 * @param <Long>
 */
	public interface GenericDAO <T, PK> {

    public T read (Long id);

    public PK create (T newInstance);

    public void update (T transientObject);

    public void delete (T persistentObject);

}

