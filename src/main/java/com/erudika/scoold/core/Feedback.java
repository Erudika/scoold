/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.core;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Feedback extends Post{
	private static final long serialVersionUID = 1L;

	public Feedback() {
		super();
	}
	
	public boolean canHaveChildren() {
		return true;
	}
	
	public boolean canHaveRevisions() {
		return true;
	}
	
	public static enum FeedbackType{
		BUG, QUESTION, SUGGESTION;
		
		public String toString() {
			return super.toString().toLowerCase();
		}
	}
	
}
