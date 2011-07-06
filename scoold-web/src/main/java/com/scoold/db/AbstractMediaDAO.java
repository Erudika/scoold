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

	public abstract ArrayList<T> readAllMediaForUUID(String uuid, MediaType type, String label, MutableLong page, MutableLong itemcount, boolean reverse);
	public abstract ArrayList<T> readAllMediaForUUID(String uuid, MediaType type, String label, MutableLong page, MutableLong itemcount, int maxPerPage, boolean reverse);
	public abstract ArrayList<Media> readPhotosAndCommentsForUUID(String uuid, String label, Long photoid, int nextPrevAll, MutableLong itemcount);
	public abstract ArrayList<String> readAllLabelsForUUID(String uuid);
	public abstract void deleteAllMediaForUUID(String uuid);
}
