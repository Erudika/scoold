/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.core;

import com.scoold.db.AbstractCommentDAO;
import com.scoold.db.AbstractDAOFactory;

/**
 *
 * @author alexb
 */
public class Comment implements ScooldObject, Votable<Long>,
		Comparable<Comment> {

	private Long id;
	private String uuid;
	@Stored private Long userid;
	@Stored private Long timestamp;
	@Stored private String comment;
	@Stored private String parentuuid;
	@Stored private Boolean hidden;
	@Stored private Integer votes;
	@Stored private String author;
	@Stored private String classname;

	private transient static AbstractCommentDAO<Comment, Long> mydao;

    public static AbstractCommentDAO<Comment, Long> getCommentDao(){
        return (mydao != null) ? mydao : (AbstractCommentDAO<Comment, Long>)
				AbstractDAOFactory.getDefaultDAOFactory().getDAO(Comment.class);
    }

	public Comment() {
	}

	public Comment(String uuid) {
		this.uuid = uuid;
	}
	
	public Comment(Long id) {
		this.id = id;
	}

	public Comment(Long id, Long userid) {
		this.id = id;
		this.userid = userid;
	}

	public Comment(Long userid, String comment, String parentuuid) {
		this.userid = userid;
		this.comment = comment;
		this.parentuuid = parentuuid;
		this.timestamp = new Long(System.currentTimeMillis()); //now
	}

	/**
	 * Get the value of classname
	 *
	 * @return the value of classname
	 */
	public String getClassname() {
		return classname;
	}

	/**
	 * Set the value of classname
	 *
	 * @param classname new value of classname
	 */
	public void setClassname(String classname) {
		this.classname = classname;
	}

	/**
	 * Get the value of author
	 *
	 * @return the value of author
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * Set the value of author
	 *
	 * @param author new value of author
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * Get the value of hidden
	 *
	 * @return the value of hidden
	 */
	public Boolean getHidden() {
		return hidden;
	}

	/**
	 * Set the value of hidden
	 *
	 * @param hidden new value of hidden
	 */
	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	/**
	 * Get the value of parentuuid
	 *
	 * @return the value of parentuuid
	 */
	public String getParentuuid() {
		return parentuuid;
	}

	/**
	 * Set the value of parentuuid
	 *
	 * @param parentuuid new value of parentuuid
	 */
	public void setParentuuid(String parentuuid) {
		this.parentuuid = parentuuid;
	}

	/**
	 * Get the value of comment
	 *
	 * @return the value of comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Set the value of comment
	 *
	 * @param comment new value of comment
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * Get the value of timestamp
	 *
	 * @return the value of timestamp
	 */
	public Long getTimestamp() {
		return timestamp;
	}

	/**
	 * Set the value of timestamp
	 *
	 * @param timestamp new value of timestamp
	 */
	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Get the value of userid
	 *
	 * @return the value of userid
	 */
	public Long getUserid() {
		return userid;
	}

	/**
	 * Set the value of userid
	 *
	 * @param userid new value of userid
	 */
	public void setUserid(Long userid) {
		this.userid = userid;
	}

	/**
	 * Get the value of uuid
	 *
	 * @return the value of uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the value of uuid
	 *
	 * @param uuid new value of uuid
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	/**
	 * Get the value of votes
	 *
	 * @return the value of votes
	 */
	public Integer getVotes() {
		return votes;
	}

	/**
	 * Set the value of votes
	 *
	 * @param votes new value of votes
	 */
	public void setVotes(Integer votes) {
		this.votes = votes;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id){
		this.id = id;
	}
	
	public boolean voteUp(Long userid) {
		return AbstractDAOFactory.getDefaultDAOFactory().getDAOUtils().voteUp(userid, this);
	}

	public boolean voteDown(Long userid) {
		return AbstractDAOFactory.getDefaultDAOFactory().getDAOUtils().voteDown(userid, this);
	}

	public Long create() {
		this.id = getCommentDao().create(this);
		return this.id;
	}

	public void update() {
		getCommentDao().update(this);
	}

	public void delete() {
		getCommentDao().delete(this);
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Comment other = (Comment) obj;
		if ((this.uuid == null) ? (other.uuid != null) : !this.uuid.equals(other.uuid)) {
			return false;
		}
		if (this.userid != other.userid && (this.userid == null || !this.userid.equals(other.userid))) {
			return false;
		}		
		if ((this.comment == null) ? (other.comment != null) : !this.comment.equals(other.comment)) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int hash = 3;
		hash = 17 * hash + (this.uuid != null ? this.uuid.hashCode() : 0);
		hash = 17 * hash + (this.userid != null ? this.userid.hashCode() : 0);
		hash = 17 * hash + (this.comment != null ? this.comment.hashCode() : 0);
		return hash;
	}

	public int compareTo(Comment o) {
		int deptComp = 0;
		if(timestamp != null)
			deptComp = this.timestamp.compareTo(o.getTimestamp());

		return deptComp;
	}
	
}
