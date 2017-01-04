/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.core;

import com.erudika.para.core.Sysprop;
import com.erudika.para.annotations.Stored;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Report extends Sysprop{
	private static final long serialVersionUID = 1L;

	@Stored private String subType;
	@Stored private String description;
	@Stored private String author;
	@Stored private String link;
	@Stored private String grandparentid;
	@Stored private String solution;
	@Stored private Boolean closed;

	public static enum ReportType{
		SPAM, OFFENSIVE, DUPLICATE, INCORRECT, OTHER;

		public String toString() {
			return super.toString().toLowerCase();
		}
	}

	public Report() {
		this.closed = false;
	}

	public Report(String id) {
		setId(id);
		this.closed = false;
	}

	public Report(String parentid, String type, String description,
			String creatorid) {
		setParentid(parentid);
		this.subType = type;
		this.description = description;
		setCreatorid(creatorid);
		this.closed = false;
	}

	public Boolean getClosed() {
		return closed;
	}

	public void setClosed(Boolean closed) {
		this.closed = closed;
	}

	public String getSolution() {
		return solution;
	}

	public void setSolution(String solution) {
		this.solution = solution;
	}

	public String getGrandparentid() {
		return grandparentid;
	}

	public void setGrandparentid(String grandparentid) {
		this.grandparentid = grandparentid;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSubType() {
		return subType;
	}

	public void setSubType(String subType) {
		this.subType = subType;
	}

	public void setSubType(ReportType subType) {
		if (subType != null) this.subType = subType.name();
	}
	
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Report other = (Report) obj;
		if ((getParentid() == null) ? (other.getParentid() != null) : !getParentid().equals(other.getParentid())) {
			return false;
		}
		if ((this.subType == null) ? (other.subType != null) : !this.subType.equals(other.subType)) {
			return false;
		}
		if (getCreatorid() == null || !getCreatorid().equals(other.getCreatorid())) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int hash = 5;
		hash = 37 * hash + (getParentid() != null ? getParentid().hashCode() : 0);
		hash = 37 * hash + (this.subType != null ? this.subType.hashCode() : 0);
		hash = 37 * hash + (getCreatorid() != null ? getCreatorid().hashCode() : 0);
		return hash;
	}

}
