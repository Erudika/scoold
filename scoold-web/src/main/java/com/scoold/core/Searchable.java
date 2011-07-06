/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.core;

import java.util.ArrayList;

/**
 *
 * @author alexb
 */
public interface Searchable<T> {

	public ArrayList<T> readAllForKeys(ArrayList<String> keys);

	public void index();

	public void reindex();

	public void unindex();

}
