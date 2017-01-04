/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.core;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Reply extends Post{
	private static final long serialVersionUID = 1L;

	public Reply() {
		super();
	}
	
	public boolean canHaveChildren() {
		return true;
	}

	public boolean canHaveRevisions() {
		return true;
	}
}
