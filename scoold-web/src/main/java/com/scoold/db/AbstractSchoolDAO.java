/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db;

import com.scoold.core.Classunit;
import com.scoold.core.School;
import java.util.ArrayList;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @param <T>
 * @param <PK> 
 * @author alexb
 */
public abstract class AbstractSchoolDAO<T extends School, PK>
        implements GenericDAO<School, Long> {

    public abstract boolean createUserSchoolLink(PK userid, PK schoolid, Integer from, Integer to);
    public abstract void deleteUserSchoolLink(PK userid, T s);
    public abstract ArrayList<Classunit> readAllClassUnitsForSchool(PK schoolid, MutableLong page, MutableLong itemcount);
	
	public abstract boolean isLinkedToUser(PK schoolid, PK userid);
	public abstract boolean schoolExists(PK schoolid);

	public abstract boolean mergeSchools(PK primarySchoolid, PK duplicateSchoolid);
}
