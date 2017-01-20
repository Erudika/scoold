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

import com.erudika.para.core.Tag;
import com.erudika.para.annotations.Stored;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Sysprop;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.utils.AppConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public abstract class Post extends Sysprop {
	private static final long serialVersionUID = 1L;

	@Stored @NotBlank @Size(min = 10, max = AppConfig.MAX_TEXT_LENGTH)
	private String body;
	@Stored @NotBlank @Size(min = 6, max = 255)
	private String title;
	@Stored @NotEmpty @Size(min = 1, max = AppConfig.MAX_TAGS_PER_POST)
	private List<String> tags;

	@Stored private Long viewcount;
	@Stored private String answerid;
	@Stored private String revisionid;
	@Stored private String closerid;
	@Stored private Long answercount;
	@Stored private Long lastactivity;
	@Stored private String lasteditby;
	@Stored private Long commentcount;
	@Stored private String deletereportid;

	private transient Profile author;
	private transient Profile lastEditor;
	private transient List<Comment> comments;
	private transient Long pagenum;

	public Post() {
		this.answercount = 0L;
		this.viewcount = 0L;
		this.commentcount = 0L;
		this.pagenum = 0L;
	}

	public Long getPagenum() {
		return pagenum;
	}

	public void setPagenum(Long pagenum) {
		this.pagenum = pagenum;
	}

	public String getDeletereportid() {
		return deletereportid;
	}

	public void setDeletereportid(String deletereportid) {
		this.deletereportid = deletereportid;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public String getLasteditby() {
		return lasteditby;
	}

	public void setLasteditby(String lasteditby) {
		this.lasteditby = lasteditby;
	}

	public Long getLastactivity() {
		return lastactivity;
	}

	public void setLastactivity(Long lastactivity) {
		this.lastactivity = lastactivity;
	}

	public Long getAnswercount() {
		return answercount;
	}

	public void setAnswercount(Long answercount) {
		this.answercount = answercount;
	}

	public String getCloserid() {
		return closerid;
	}

	public void setCloserid(String closed) {
		this.closerid = closed;
	}

	public String getRevisionid() {
		return revisionid;
	}

	public void setRevisionid(String revisionid) {
		this.revisionid = revisionid;
	}

	public String getAnswerid() {
		return answerid;
	}

	public void setAnswerid(String answerid) {
		this.answerid = answerid;
	}

	public Long getViewcount() {
		return viewcount;
	}

	public void setViewcount(Long viewcount) {
		this.viewcount = viewcount;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		title = Utils.stripAndTrim(title);
		if (!StringUtils.isBlank(title)) {
			this.title = title;
			setName(title);
		}
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public boolean isClosed() {
		return this.closerid != null;
	}

	public String getTagsString() {
		if (getTags() == null || getTags().isEmpty()) return "";
		return StringUtils.join(getTags(), ", ");
	}

	public String create() {
		Post p = AppConfig.client().create(this);
		if (p != null) {
			if (canHaveRevisions()) {
				setRevisionid(Revision.createRevisionFromPost(p, true).create());
			}
			createTags();
			setId(p.getId());
			setTimestamp(p.getTimestamp());
			return p.getId();
		}
		return null;
	}

	public void update() {
		updateLastActivity();
		if (canHaveRevisions()) {
			setRevisionid(Revision.createRevisionFromPost(this, false).create());
		}
		createTags();
		AppConfig.client().update(this);
	}

	public void delete() {
		// delete post
		ArrayList<ParaObject> children = new ArrayList<ParaObject>();
		ArrayList<String> ids = new ArrayList<String>();
		// delete Comments
		children.addAll(AppConfig.client().getChildren(this, Utils.type(Comment.class)));
		// delete Revisions
		children.addAll(AppConfig.client().getChildren(this, Utils.type(Revision.class)));

		if (canHaveChildren()) {
			for (ParaObject reply : AppConfig.client().getChildren(this, Utils.type(Reply.class))) {
				// delete Comments
				children.addAll(AppConfig.client().getChildren(reply, Utils.type(Comment.class)));
				// delete Revisions
				children.addAll(AppConfig.client().getChildren(reply, Utils.type(Revision.class)));
			}
		}
		for (ParaObject child : children) {
			ids.add(child.getId());
		}
		AppConfig.client().deleteAll(ids);
		AppConfig.client().delete(this);
	}

	private void createTags() {
		if (getTags() == null) return;
		ArrayList<Tag> tags = new ArrayList<Tag>();
		for (String ntag : getTags()) {
			Tag t = new Tag(StringUtils.trimToEmpty(ntag));
			if (!StringUtils.isBlank(ntag) && AppConfig.client().findTags(t.getId()).isEmpty()) {
				tags.add(t);
			}
		}
		AppConfig.client().createAll(tags);
	}

	public static <P extends Post> void readAllCommentsForPosts(List<P> list, int maxPerPage) {
		if (list != null) {
			for (P post : list) {
				Pager page = new Pager(post.getPagenum(), null, true, 5);
				List<Comment> commentz = AppConfig.client().getChildren(post, Utils.type(Comment.class), page);
				post.setComments(commentz);
				post.setPagenum(page.getPage());
			}
		}
	}

	@JsonIgnore
	public Profile getAuthor() {
		if (getCreatorid() == null) return null;
		if (author == null) author = AppConfig.client().read(getCreatorid());
		return author;
	}

	@JsonIgnore
	public Profile getLastEditor() {
		if (lasteditby == null) return null;
		if (lastEditor == null) lastEditor = AppConfig.client().read(lasteditby);
		return lastEditor;
	}

	public void restoreRevisionAndUpdate(String revisionid) {
		Revision rev = AppConfig.client().read(revisionid);
		if (rev != null) {
			//copy rev data to post
			setTitle(rev.getTitle());
			setBody(rev.getBody());
			setTags(rev.getTags());

			updateLastActivity();
			setRevisionid(rev.getId());
			//update post
			update();
		}
	}

	public void setComments(List<Comment> comments) {
		this.comments = comments;
	}

	public List<Comment> getComments() {
		return this.comments;
	}

	public List<Comment> getComments(Pager pager) {
		this.comments = AppConfig.client().getChildren(this, Utils.type(Comment.class), pager);
		this.commentcount = pager.getCount();
		this.pagenum = pager.getPage();
		return this.comments;
	}

	public Long getCommentcount() {
		return this.commentcount;
	}

	public void setCommentcount(Long count) {
		this.commentcount = count;
	}

	@JsonIgnore
	public List<Reply> getAnswers(Pager pager) {
		if (isReply()) {
			return Collections.emptyList();
		}
		return AppConfig.client().getChildren(this, Utils.type(Reply.class), pager);
	}

	@JsonIgnore
	public List<Revision> getRevisions(Pager pager) {
		return AppConfig.client().getChildren(this, Utils.type(Revision.class), pager);
	}

	@JsonIgnore
	public boolean isReply() {
		return this instanceof Reply;
	}

	@JsonIgnore
	public boolean isQuestion() {
		return this instanceof Question;
	}

	@JsonIgnore
	public boolean isFeedback() {
		return this instanceof Feedback;
	}

	public void updateLastActivity() {
		setLastactivity(System.currentTimeMillis());
	}

	public String getPostLink(boolean plural, boolean noid, String questionslink, String questionlink, String feedbacklink) {
		Post p = this;
		if (p == null) return "/";
		String ptitle = Utils.noSpaces(p.getTitle(), "-");
		String pid = (noid ? "" : "/"+p.getId()+"/"+ ptitle);
		if (p.isQuestion()) {
			return plural ? questionslink : questionlink + pid;
		} else if (p.isFeedback()) {
			return plural ? feedbacklink : feedbacklink + pid;
		} else if (p.isReply()) {
			Post parentp = AppConfig.client().read(p.getParentid());
			if (parentp != null) {
				return parentp.getPostLink(plural, noid, questionslink, questionlink, feedbacklink);
			}
		}
		return "";
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Post other = (Post) obj;
		if ((this.body == null) ? (other.body != null) : !this.body.equals(other.body)) {
			return false;
		}
		if ((this.title == null) ? (other.title != null) : !this.title.equals(other.title)) {
			return false;
		}
		if ((getTags() == null) ? (other.getTags() != null) : !getTags().equals(other.getTags())) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + (this.body != null ? this.body.hashCode() : 0);
		return hash;
	}

	public abstract boolean canHaveChildren();

	public abstract boolean canHaveRevisions();
}
