/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.db;

import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Revision;
import java.util.ArrayList;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public abstract class AbstractRevisionDAO<T extends Revision>
        implements GenericDAO<Revision> {

    public abstract ArrayList<T> readAllRevisionsForPost(String parentid, MutableLong page, MutableLong itemcount);
	public abstract void restoreRevision(String revisionid, Post transientPost);
	public abstract void deleteAllRevisionsForID(String parentid);
}