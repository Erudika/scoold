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
import com.erudika.scoold.utils.ScooldUtils;
import jersey.repackaged.com.google.common.base.Objects;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Report extends Sysprop {
	private static final long serialVersionUID = 1L;

	@Stored private String subType;
	@Stored private String description;
	@Stored private String authorName;
	@Stored private String link;
	@Stored private String grandparentid;
	@Stored private String solution;
	@Stored private Boolean closed;

	public static enum ReportType{
		SPAM, OFFENSIVE, DUPLICATE, INCORRECT, OTHER;

		public String toString() {
			return super.toString().toLowerCase();
		}
	}

	public Report() {
		this(null, null, null, null);
	}

	public Report(String id) {
		this(null, null, null, null);
		setId(id);
	}

	public Report(String parentid, String type, String description, String creatorid) {
		setParentid(parentid);
		setCreatorid(creatorid);
		this.subType = ReportType.OTHER.toString();
		this.description = subType;
		this.closed = false;
	}

	private ParaClient client() {
		return ScooldUtils.getInstance().getParaClient();
	}

	public Boolean getClosed() {
		return closed;
	}

	public void setClosed(Boolean closed) {
		this.closed = closed;
	}

	public String getSolution() {
		return solution;
	}

	public void setSolution(String solution) {
		this.solution = solution;
	}

	public String getGrandparentid() {
		return grandparentid;
	}

	public void setGrandparentid(String grandparentid) {
		this.grandparentid = grandparentid;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getAuthorName() {
		return authorName;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSubType() {
		if (subType == null) {
			subType = ReportType.OTHER.toString();
		}
		return subType;
	}

	public void setSubType(String subType) {
		this.subType = subType;
	}

	public void setSubType(ReportType subType) {
		if (subType != null) this.subType = subType.name();
	}

	public void delete() {
		client().delete(this);
	}

	public void update() {
		client().update(this);
	}

	public String create() {
		Report r = client().create(this);
		if (r != null) {
			setId(r.getId());
			setTimestamp(r.getTimestamp());
			return r.getId();
		}
		return null;
	}

	public int hashCode() {
		return Objects.hashCode(getSubType(), getDescription(), getCreatorid(), getParentid());
	}

	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return Objects.equal(getSubType(), ((Report) obj).getSubType()) &&
				Objects.equal(getDescription(), ((Report) obj).getDescription()) &&
				Objects.equal(getCreatorid(), ((Report) obj).getCreatorid());
	}

}
