/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.core;

import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractRevisionDAO;
import java.io.Serializable;


/**
 *
 * @author alexb
 */
public class Revision implements ScooldObject, Serializable{

	private Long id;
	@Stored private String body;
	@Stored private String description;
	@Stored private Long timestamp;
	@Stored private Long userid;
	@Stored private String title;
	@Stored private String tags;
	@Stored private Boolean original;
	@Stored private Long parentid;
	@Stored public static final String classtype = Revision.class.getSimpleName().toLowerCase();

	private transient User author;
	private transient static AbstractRevisionDAO<Revision, Long> mydao;

	public static AbstractRevisionDAO<Revision, Long> getRevisionDao(){
		return (mydao != null) ? mydao : (AbstractRevisionDAO<Revision, Long>)
				AbstractDAOFactory.getDefaultDAOFactory().getDAO(Revision.class);
	}

	public Revision() {
	}

	public Revision(Long id) {
		this.id = id;
	}

	public String getClasstype() {
		return classtype;
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
	 * Get the value of original
	 *
	 * @return the value of original
	 */
	public Boolean getOriginal() {
		return original;
	}

	/**
	 * Set the value of original
	 *
	 * @param original new value of original
	 */
	public void setOriginal(Boolean original) {
		this.original = original;
	}

	/**
	 * Get the value of tags
	 *
	 * @return the value of tags
	 */
	public String getTags() {
		return tags;
	}

	/**
	 * Set the value of tags
	 *
	 * @param tags new value of tags
	 */
	public void setTags(String tags) {
		this.tags = tags;
	}


	/**
	 * Get the value of title
	 *
	 * @return the value of title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Set the value of title
	 *
	 * @param title new value of title
	 */
	public void setTitle(String title) {
		this.title = title;
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
	 * Get the value of description
	 *
	 * @return the value of description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the value of description
	 *
	 * @param description new value of description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Get the value of body
	 *
	 * @return the value of body
	 */
	public String getBody() {
		return body;
	}

	/**
	 * Set the value of body
	 *
	 * @param body new value of body
	 */
	public void setBody(String body) {
		this.body = body;
	}
	
	public Long getId(){
		return id;
	}

	public void setId(Long id){
		this.id = id;
	}

	public Long create() {
		this.id = getRevisionDao().create(this);
		return this.id;
	}

	public void update() {
		getRevisionDao().update(this);
	}

	public void delete() {
		getRevisionDao().delete(this);
	}

	public User getAuthor(){
		if(userid == null) return null;
		if(author == null) author = User.getUser(userid);
		return author;
	}

	public static Revision createRevisionFromPost(Post post, boolean orig){
		Long revUserid = post.getLasteditby();
		if (revUserid == null) {
			revUserid = post.getUserid();
		}
		Revision postrev = new Revision();
//		postrev.setId(post.getRevisionid());
		postrev.setUserid(revUserid);
		postrev.setParentid(post.getId());
		postrev.setTitle(post.getTitle());
		postrev.setBody(post.getBody());
		postrev.setTags(post.getTags());
		postrev.setOriginal(orig);

		return postrev;
	}


	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Revision other = (Revision) obj;
		if ((this.body == null) ? (other.body != null) : !this.body.equals(other.body)) {
			return false;
		}
		if ((this.description == null) ? (other.description != null) : !this.description.equals(other.description)) {
			return false;
		}
		if (this.timestamp == null || !this.timestamp.equals(other.timestamp)) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + (this.body != null ? this.body.hashCode() : 0);
		hash = 31 * hash + (this.description != null ? this.description.hashCode() : 0);
		hash = 31 * hash + (this.timestamp != null ? this.timestamp.hashCode() : 0);
		return hash;
	}


}
