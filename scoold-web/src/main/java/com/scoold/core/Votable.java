/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.core;

/**
 *
 * @author alexb
 */
public interface Votable<PK> extends ScooldObject{

	public boolean voteUp(PK userid);
	public boolean voteDown(PK userid);
	public Integer getVotes();
	public void setVotes(Integer votes);
	public Long getUserid();

}
