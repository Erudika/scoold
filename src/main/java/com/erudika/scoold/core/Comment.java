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
import com.erudika.para.utils.Config;
import com.erudika.scoold.utils.AppConfig;
import java.util.Collections;
import jersey.repackaged.com.google.common.base.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Comment extends Sysprop implements Comparable<Comment> {
	private static final long serialVersionUID = 1L;

	@Stored private String comment;
	@Stored private Boolean hidden;
	@Stored private String authorName;

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
	}

	public String getAuthorName() {
		return authorName;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
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

	public String create() {
		if (StringUtils.isBlank(comment) || StringUtils.isBlank(getParentid())) return null;
		int count = AppConfig.client().getCount(getType(),
				Collections.singletonMap(Config._PARENTID, getParentid())).intValue();
		if (count > AppConfig.MAX_COMMENTS_PER_ID) return null;
		Comment c = AppConfig.client().create(this);
		if (c != null) {
			setId(c.getId());
			setTimestamp(c.getTimestamp());
			return c.getId();
		}
		return null;
	}

	public void update() {
		AppConfig.client().update(this);
	}

	public void delete() {
		AppConfig.client().delete(this);
	}

	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return Objects.equal(getComment(), ((Comment) obj).getComment()) &&
				Objects.equal(getCreatorid(), ((Comment) obj).getCreatorid());
	}

	public int hashCode() {
		return Objects.hashCode(getComment(), getCreatorid());
	}

	public int compareTo(Comment o) {
		int deptComp = 0;
		if (getTimestamp() != null)
			deptComp = getTimestamp().compareTo(o.getTimestamp());

		return deptComp;
	}

}
