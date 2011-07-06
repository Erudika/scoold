/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.util;

import java.io.Serializable;

/**
 *
 * @author alexb
 */
public interface Queue<E extends Serializable> {

	public void push(E task);

	public E pull();

}
