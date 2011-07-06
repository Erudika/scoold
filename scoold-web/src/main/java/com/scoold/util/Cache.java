/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.util;

/**
 *
 * @author alexb
 */
public interface Cache {

	public <V> V get(String key);

	public <V> V put(String key, V o);

	public <V> V putIfAbsent(String key, V o);

	public <V> V remove(String key);

}
