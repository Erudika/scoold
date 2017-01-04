/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.core;

import com.erudika.para.utils.Config;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Blackboard extends Post{
	private static final long serialVersionUID = 1L;

	public Blackboard() {
		super();
	}

	public boolean canHaveChildren() {
		return false;
	}

	public boolean canHaveRevisions() {
		return false;
	}

	public static final String getBlackboardId(String parentid) {
		if (parentid == null) {
			return null;
		}
		return parentid + Config.SEPARATOR + "blackboard";
	}

}
