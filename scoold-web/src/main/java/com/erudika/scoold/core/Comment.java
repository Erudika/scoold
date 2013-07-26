/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.core;

import com.erudika.para.core.PObject;
import com.erudika.para.utils.DAO;
import com.erudika.para.utils.Search;
import com.erudika.para.utils.Stored;
import com.erudika.scoold.util.Constants;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author alexb
 */
public class Comment extends PObject implements Comparable<Comment> {
	private static final long serialVersionUID = 1L;

	@Stored private String comment;
	@Stored private Boolean hidden;
	@Stored private Integer votes;
	@Stored private String author;

	public Comment() {
		this(null, null, null);
	}

	public Comment(String id) {
		this();
		setId(id);
	}

	public Comment(String creatorid, String comment, String parentid) {
		setCreatorid(creatorid);
		this.comment = comment;
		setParentid(parentid);
		setTimestamp(System.currentTimeMillis()); //now
		this.votes = 0;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Boolean getHidden() {
		return hidden;
	}

	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Integer getVotes() {
		return votes;
	}

	public void setVotes(Integer votes) {
		this.votes = votes;
	}
	
	public String create() {
		if(StringUtils.isBlank(comment) || StringUtils.isBlank(getParentid())) return null;
		int count = Search.getCount(getClassname(), DAO.CN_PARENTID, getParentid()).intValue();
		if(count > Constants.MAX_COMMENTS_PER_ID) return null;
		return super.create();
	}
	
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Comment other = (Comment) obj;
		if ((getId() == null) ? (other.getId() != null) : !getId().equals(other.getId())) {
			return false;
		}
		if (getCreatorid() == null || !getCreatorid().equals(other.getCreatorid())) {
			return false;
		}		
		if ((this.comment == null) ? (other.comment != null) : !this.comment.equals(other.comment)) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int hash = 3;
		hash = 17 * hash + (getId() != null ? getId().hashCode() : 0);
		hash = 17 * hash + (getCreatorid() != null ? getCreatorid().hashCode() : 0);
		hash = 17 * hash + (this.comment != null ? this.comment.hashCode() : 0);
		return hash;
	}

	public int compareTo(Comment o) {
		int deptComp = 0;
		if(getTimestamp() != null)
			deptComp = getTimestamp().compareTo(o.getTimestamp());

		return deptComp;
	}
	
}
