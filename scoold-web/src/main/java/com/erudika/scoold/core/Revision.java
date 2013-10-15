/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.core;

import com.erudika.para.core.PObject;
import com.erudika.para.annotations.Stored;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class Revision extends PObject{
	private static final long serialVersionUID = 1L;

	@Stored private String body;
	@Stored private String description;
	@Stored private String title;
	@Stored private String tags;
	@Stored private Boolean original;

	private transient User author;

	public Revision() {
	}

	public Revision(String id) {
		setId(id);
	}

	public Boolean getOriginal() {
		return original;
	}

	public void setOriginal(Boolean original) {
		this.original = original;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		setName(title);
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public User getAuthor(){
		if(getCreatorid() == null) return null;
		if(author == null) author = getDao().read(getCreatorid());
		return author;
	}

	public static Revision createRevisionFromPost(Post post, boolean orig){
		String revUserid = post.getLasteditby();
		if (revUserid == null) {
			revUserid = post.getCreatorid();
		}
		// TODO check if revision's parentid is set
		assert post.getId() != null;
		Revision postrev = new Revision();
//		postrev.setId(post.getRevisionid());
		postrev.setCreatorid(revUserid);
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
		if (getTimestamp() == null || !getTimestamp().equals(other.getTimestamp())) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + (this.body != null ? this.body.hashCode() : 0);
		hash = 31 * hash + (this.description != null ? this.description.hashCode() : 0);
		hash = 31 * hash + (getTimestamp() != null ? getTimestamp().hashCode() : 0);
		return hash;
	}


}
