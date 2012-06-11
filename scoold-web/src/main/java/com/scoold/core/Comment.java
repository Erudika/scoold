/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.core;

import com.scoold.db.AbstractCommentDAO;
import com.scoold.db.AbstractDAOFactory;
import java.io.Serializable;

/**
 *
 * @author alexb
 */
public class Comment implements ScooldObject, Votable<Long>, Comparable<Comment>, Serializable {
	private static final long serialVersionUID = 1L;

	private Long id;
	@Stored private Long userid;
	@Stored private Long timestamp;
	@Stored private String comment;
	@Stored private Long parentid;
	@Stored private Boolean hidden;
	@Stored private Integer votes;
	@Stored private String author;
	@Stored private String classname;
	@Stored public static final String classtype = Comment.class.getSimpleName().toLowerCase();

	private transient static AbstractCommentDAO<Comment, Long> mydao;

    public static AbstractCommentDAO<Comment, Long> getCommentDao(){
        return (mydao != null) ? mydao : (AbstractCommentDAO<Comment, Long>)
				AbstractDAOFactory.getDefaultDAOFactory().getDAO(Comment.class);
    }

	public Comment() {
		this(null, null, null);
	}

	public Comment(Long id) {
		this();
		this.id = id;
	}

	public Comment(Long userid, String comment, Long parentid) {
		this.userid = userid;
		this.comment = comment;
		this.parentid = parentid;
		this.timestamp = Long.valueOf(System.currentTimeMillis()); //now
		this.votes = 0;
	}

	public String getClasstype() {
		return classtype;
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
	 * Get the value of parentid
	 *
	 * @return the value of parentid
	 */
	public Long getParentid() {
		return parentid;
	}

	/**
	 * Set the value of parentid
	 *
	 * @param parentid new value of parentid
	 */
	public void setParentid(Long parentid) {
		this.parentid = parentid;
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
		if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
			return false;
		}
		if (this.userid == null || !this.userid.equals(other.userid)) {
			return false;
		}		
		if ((this.comment == null) ? (other.comment != null) : !this.comment.equals(other.comment)) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int hash = 3;
		hash = 17 * hash + (this.id != null ? this.id.hashCode() : 0);
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
