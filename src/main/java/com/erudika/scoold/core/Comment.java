/*
 * Copyright 2013-2019 Erudika. https://erudika.com
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
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.utils.ScooldUtils;
import java.util.Collections;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Comment extends Sysprop {

	private static final long serialVersionUID = 1L;
	public static final int MAX_COMMENTS_PER_ID = Config.getConfigInt("max_comments_per_id", 1000);
	public static final int MAX_COMMENT_LENGTH = Config.getConfigInt("max_comment_length", 255);

	@Stored private String comment;
	@Stored private Boolean hidden;
	@Stored private String authorName;

	public Comment() {
		this(null, null, null);
	}

	public Comment(String creatorid, String comment, String parentid) {
		setCreatorid(creatorid);
		this.comment = comment;
		setParentid(parentid);
		setTimestamp(System.currentTimeMillis()); //now
	}

	private ParaClient client() {
		return ScooldUtils.getInstance().getParaClient();
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
		if (StringUtils.isBlank(comment) || StringUtils.isBlank(getParentid())) {
			return null;
		}
		int count = client().getCount(getType(), Collections.singletonMap(Config._PARENTID, getParentid())).intValue();
		if (count > MAX_COMMENTS_PER_ID) {
			return null;
		}
		this.comment = Utils.abbreviate(this.comment, MAX_COMMENT_LENGTH);
		Comment c = client().create(this);
		if (c != null) {
			setId(c.getId());
			setTimestamp(c.getTimestamp());
			return c.getId();
		}
		return null;
	}

	public void update() {
		client().update(this);
	}

	public void delete() {
		client().delete(this);
	}

	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return Objects.equals(getComment(), ((Comment) obj).getComment())
				&& Objects.equals(getCreatorid(), ((Comment) obj).getCreatorid());
	}

	public int hashCode() {
		return Objects.hashCode(getComment()) + Objects.hashCode(getCreatorid());
	}

}
