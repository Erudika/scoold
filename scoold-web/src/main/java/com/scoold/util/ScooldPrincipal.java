/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.util;

import name.aikesommer.authenticator.SimplePrincipal;

/**
 *
 * @param <T>
 * @author alexb
 *
 * A very simple approach to returning a principal together with its
 * group-information. You can subclass this if u wanna store more information
 * for ur principal.
 */
public class ScooldPrincipal<T> extends SimplePrincipal {
	private T user;

	/**
     * Create a ScooldPrincipal with username and user.
     *
     * @param name The username of the principal.
	 * @param user The user object
	 * @param groups The groups for that user
     */
    public ScooldPrincipal(String name, T user, String groups) {
		super(name, groups);
		this.user = user;
    }

	public T getUser(){
		return this.user;
	}

}
