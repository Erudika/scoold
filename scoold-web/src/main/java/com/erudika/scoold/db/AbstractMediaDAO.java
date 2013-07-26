/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.db;

import com.erudika.scoold.core.Media;
import com.erudika.scoold.core.Media.MediaType;
import java.util.ArrayList;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 *
 * @param <T>
 * @param <String>
 * @author alexb
 */
public abstract class AbstractMediaDAO<T extends Media>
        implements GenericDAO<Media> {

	public abstract ArrayList<T> readAllMediaForID(String parentid, MediaType type, String label, MutableLong page, MutableLong itemcount, boolean reverse);
	public abstract ArrayList<T> readAllMediaForID(String parentid, MediaType type, String label, MutableLong page, MutableLong itemcount, int maxPerPage, boolean reverse);
	public abstract ArrayList<Media> readPhotosAndCommentsForID(String id, String label, Long photoid, MutableLong itemcount);
	public abstract void deleteAllMediaForID(String parentid);
}
