/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.core;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class Question extends Post{
	private static final long serialVersionUID = 1L;
	
	public boolean canHaveChildren() {
		return true;
	}

	public boolean canHaveRevisions() {
		return true;
	}
}
