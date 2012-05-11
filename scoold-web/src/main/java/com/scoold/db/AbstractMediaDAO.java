/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db;

import com.scoold.core.Media;
import com.scoold.core.Media.MediaType;
import java.util.ArrayList;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @param <T>
 * @param <PK>
 * @author alexb
 */
public abstract class AbstractMediaDAO<T extends Media, PK>
        implements GenericDAO<Media, Long> {

	public abstract ArrayList<T> readAllMediaForID(Long parentid, MediaType type, String label, MutableLong page, MutableLong itemcount, boolean reverse);
	public abstract ArrayList<T> readAllMediaForID(Long parentid, MediaType type, String label, MutableLong page, MutableLong itemcount, int maxPerPage, boolean reverse);
	public abstract ArrayList<Media> readPhotosAndCommentsForID(Long id, String label, Long photoid, MutableLong itemcount);
	public abstract ArrayList<String> readAllLabelsForID(Long paretid);
	public abstract void deleteAllMediaForID(Long parentid);
}
