/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db;

import com.scoold.core.Post;
import com.scoold.core.Revision;
import java.util.ArrayList;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public abstract class AbstractRevisionDAO<T extends Revision, PK>
        implements GenericDAO<Revision, Long> {

    public abstract ArrayList<T> readAllRevisionsForPost(Long parentid, MutableLong page, MutableLong itemcount);
	public abstract void restoreRevision(PK revisionid, Post transientPost);
	public abstract void deleteAllRevisionsForID(Long parentid);
}