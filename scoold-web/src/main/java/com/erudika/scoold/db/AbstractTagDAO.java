/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.db;

import com.erudika.scoold.core.Tag;

/**
 *
 * @author alexb
 */
public abstract  class AbstractTagDAO<T extends Tag>
		implements GenericDAO <Tag>{

	public abstract T read(String tag);
}
