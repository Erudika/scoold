/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.core;

import com.scoold.core.Media.MediaType;
import java.util.ArrayList;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public interface CanHasMedia{

	public ArrayList<Media> getMedia(MediaType type, String label, MutableLong pagenum, MutableLong itemcount, int maxItems, boolean reverse);
	
	public void deleteAllMedia();

	public String getUuid();
}
