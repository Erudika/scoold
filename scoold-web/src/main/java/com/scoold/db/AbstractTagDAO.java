/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db;

import com.scoold.core.Tag;
import java.util.ArrayList;

/**
 *
 * @author alexb
 */
public abstract  class AbstractTagDAO<T extends Tag, PK>
		implements GenericDAO <Tag, Long>{

	public abstract T readTag(String tag);
	public abstract ArrayList<T> readAllForKeys(ArrayList<String> keys);

}
