/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
import com.erudika.para.core.annotations.Stored;
import com.erudika.para.client.ParaClient;
import com.erudika.scoold.utils.ScooldUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;

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

	private transient Profile author;

	public Revision() {
		this(null);
	}

	public Revision(String id) {
		setId(id);
	}

	private ParaClient client() {
		return ScooldUtils.getInstance().getParaClient();
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
	public Profile getAuthor() {
		return author;
	}

	public void setAuthor(Profile author) {
		this.author = author;
	}

	public static void createRevisionFromPost(Post post, boolean orig) {
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
			String rid = postrev.create();
			if (rid != null) {
				post.setRevisionid(rid);
			}
		}
	}

	public void delete() {
		client().delete(this);
	}

	public void update() {
		client().update(this);
	}

	public String create() {
		Revision r = client().create(this);
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
		return Objects.equals(getBody(), ((Revision) obj).getBody()) &&
				Objects.equals(getDescription(), ((Revision) obj).getDescription());
	}

	public int hashCode() {
		return Objects.hashCode(getBody()) + Objects.hashCode(getDescription()) + Objects.hashCode(getTimestamp());
	}

}
