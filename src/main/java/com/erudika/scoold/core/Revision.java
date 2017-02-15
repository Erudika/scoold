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
import com.erudika.para.client.ParaClient;
import com.erudika.para.core.User;
import com.erudika.scoold.ScooldServer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import jersey.repackaged.com.google.common.base.Objects;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Revision extends Sysprop {
	private static final long serialVersionUID = 1L;
	private ParaClient pc;

	@Stored private String body;
	@Stored private String description;
	@Stored private String title;
	@Stored private Boolean original;

	private transient User author;

	public Revision() {
		this(null);
	}

	public Revision(String id) {
		setId(id);
		this.pc = ScooldServer.getContext().getBean(ParaClient.class);
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
		return author;
	}

	public void setAuthor(User author) {
		this.author = author;
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
		pc.delete(this);
	}

	public void update() {
		pc.update(this);
	}

	public String create() {
		Revision r = pc.create(this);
		if (r != null) {
			setId(r.getId());
			setTimestamp(r.getTimestamp());
			return r.getId();
		}
		return null;
	}

	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return Objects.equal(getBody(), ((Revision) obj).getBody()) &&
				Objects.equal(getDescription(), ((Revision) obj).getDescription());
	}

	public int hashCode() {
		return Objects.hashCode(getBody(), getDescription(), getTimestamp());
	}


}
