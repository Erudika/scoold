/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.util;

import com.scoold.db.AbstractDAOFactory;
import java.io.Serializable;

/**
 *
 * @author alexb
 */
public abstract class QueueFactory {
	
	private static Queue<String> q = new AmazonQueue<String>(AbstractDAOFactory.SCOOLD_INDEX);
	
	public static <E extends Serializable> Queue<E> getQueue(String name){
//		return new HazelcastQueue<E>(name);
		return new AmazonQueue<E>(name);
	}
	
	public static Queue<String> getDefaultQueue(){
		return q;
	}
}
