/*
 * Copyright 2013-2017 Erudika. https://erudika.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For issues and patches go to: https://github.com/erudika
 */

package com.erudika.scoold.core;

import com.erudika.para.core.Sysprop;
import com.erudika.para.annotations.Stored;
import com.erudika.para.core.User;
import com.erudika.scoold.utils.AppConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Revision extends Sysprop {
	private static final long serialVersionUID = 1L;

	@Stored private String body;
	@Stored private String description;
	@Stored private String title;
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

	@JsonIgnore
	public User getAuthor() {
		if (getCreatorid() == null) return null;
		if (author == null) author = AppConfig.client().read(getCreatorid());
		return author;
	}

	public static Revision createRevisionFromPost(Post post, boolean orig) {
		if (post != null && post.getId() != null) {
			String revUserid = post.getLasteditby();
			if (revUserid == null) {
				revUserid = post.getCreatorid();
			}
			Revision postrev = new Revision();
			postrev.setCreatorid(revUserid);
			postrev.setParentid(post.getId());
			postrev.setTitle(post.getTitle());
			postrev.setBody(post.getBody());
			postrev.setTags(post.getTags());
			postrev.setOriginal(orig);
			return postrev;
		}
		return null;
	}

	public void delete() {
		AppConfig.client().delete(this);
	}

	public void update() {
		AppConfig.client().update(this);
	}

	public String create() {
		Revision r = AppConfig.client().create(this);
		if (r != null) {
			setId(r.getId());
			setTimestamp(r.getTimestamp());
			return r.getId();
		}
		return null;
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
