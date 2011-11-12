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
public abstract class QueueFactory {
	
	public static <E extends Serializable> Queue<E> getQueue(String name){
//		return new HazelcastQueue<E>(name);
		return new AmazonQueue<E>(name);
	}
}
