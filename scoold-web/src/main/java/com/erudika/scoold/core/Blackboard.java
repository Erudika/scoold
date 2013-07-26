/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.core;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class Blackboard extends Post{
	private static final long serialVersionUID = 1L;

	public boolean canHaveChildren() {
		return false;
	}

	public boolean canHaveRevisions() {
		return false;
	}
	
}
