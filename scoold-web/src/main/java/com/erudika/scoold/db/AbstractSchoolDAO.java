/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.db;

import com.erudika.scoold.core.Classunit;
import com.erudika.scoold.core.School;
import java.util.ArrayList;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 *
 * @param <T>
 * @param <String> 
 * @author alexb
 */
public abstract class AbstractSchoolDAO<T extends School>
        implements GenericDAO<School> {

    public abstract boolean createUserSchoolLink(String userid, String schoolid, Integer from, Integer to);
    public abstract void deleteUserSchoolLink(String userid, T s);
    public abstract ArrayList<Classunit> readAllClassUnitsForSchool(String schoolid, MutableLong page, MutableLong itemcount);
	
	public abstract boolean isLinkedToUser(String schoolid, String userid);
	public abstract boolean schoolExists(String schoolid);

	public abstract boolean mergeSchools(String primarySchoolid, String duplicateSchoolid);
}
