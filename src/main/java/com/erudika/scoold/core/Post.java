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
import com.erudika.para.client.ParaClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.User;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.utils.ScooldUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.Size;
import jersey.repackaged.com.google.common.base.Objects;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public abstract class Post extends Sysprop {
	private static final long serialVersionUID = 1L;

	@Stored @NotBlank @Size(min = 2, max = 20000)
	private String body;
	@Stored @NotBlank @Size(min = 6, max = 255)
	private String title;
	@Stored @NotEmpty @Size(min = 1, max = 5)
	private List<String> tags;

	@Stored private Long viewcount;
	@Stored private String answerid;
	@Stored private String revisionid;
	@Stored private String closerid;
	@Stored private Long answercount;
	@Stored private Long lastedited;
	@Stored private String lasteditby;
	@Stored private String deletereportid;
	@Stored private String location;
	@Stored private List<String> commentIds;
	@Stored private Map<String, String> followers;

	private transient Profile author;
	private transient Profile lastEditor;
	private transient List<Comment> comments;
	private transient Pager itemcount;

	public Post() {
		this.answercount = 0L;
		this.viewcount = 0L;
	}

	private ParaClient client() {
		return ScooldUtils.getInstance().getParaClient();
	}

	public Long getLastedited() {
		if (lastedited == null || lastedited <= 0) {
			lastedited = getUpdated();
		}
		return lastedited;
	}

	public void setLastedited(Long lastedited) {
		this.lastedited = lastedited;
	}

	public Pager getItemcount() {
		if (itemcount == null) {
			itemcount = new Pager(5);
			itemcount.setDesc(false);
		}
		return itemcount;
	}

	public void setItemcount(Pager itemcount) {
		this.itemcount = itemcount;
	}

	public Map<String, String> getFollowers() {
		return followers;
	}

	public void setFollowers(Map<String, String> followers) {
		this.followers = followers;
	}

	public String getDeletereportid() {
		return deletereportid;
	}

	public void setDeletereportid(String deletereportid) {
		this.deletereportid = deletereportid;
	}

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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
		if (StringUtils.isBlank(title)) {
			return getId();
		}
		return title;
	}

	public void setTitle(String title) {
		if (!StringUtils.isBlank(title)) {
			this.title = StringUtils.trimToEmpty(title);
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
		Collections.sort(getTags());
		return StringUtils.join(getTags(), ",");
	}

	public String create() {
		createTags();
		Post p = client().create(this);
		if (p != null) {
			if (canHaveRevisions()) {
				setRevisionid(Revision.createRevisionFromPost(p, true).create());
			}
			setId(p.getId());
			setTimestamp(p.getTimestamp());
			return p.getId();
		}
		return null;
	}

	public void update() {
		if (canHaveRevisions()) {
			setRevisionid(Revision.createRevisionFromPost(this, false).create());
		}
		createTags();
		client().update(this);
	}

	public void delete() {
		// delete post
		ArrayList<ParaObject> children = new ArrayList<ParaObject>();
		ArrayList<String> ids = new ArrayList<String>();
		// delete Comments
		children.addAll(client().getChildren(this, Utils.type(Comment.class)));
		// delete Revisions
		children.addAll(client().getChildren(this, Utils.type(Revision.class)));

		if (canHaveChildren()) {
			for (ParaObject reply : client().getChildren(this, Utils.type(Reply.class))) {
				// delete Comments
				children.addAll(client().getChildren(reply, Utils.type(Comment.class)));
				// delete Revisions
				children.addAll(client().getChildren(reply, Utils.type(Revision.class)));
			}
		}
		for (ParaObject child : children) {
			ids.add(child.getId());
		}
		client().deleteAll(ids);
		client().delete(this);
	}

	private void createTags() {
		if (getTags() == null || getTags().isEmpty()) return;
		ArrayList<Tag> tagz = new ArrayList<Tag>();
		for (int i = 0; i < getTags().size(); i++) {
			String ntag = getTags().get(i);
			Tag t = new Tag(StringUtils.truncate(Utils.noSpaces(Utils.stripAndTrim(ntag, " "), "-"), 35));
			if (!StringUtils.isBlank(t.getTag())) {
				Pager tagged = new Pager(1);
				client().findTagged(getType(), new String[]{t.getTag()}, tagged);
				t.setCount((int) tagged.getCount() + 1);
				getTags().set(i, t.getTag());
				tagz.add(t);
			}
		}
		client().createAll(tagz);
	}

	@JsonIgnore
	public Profile getAuthor() {
		return author;
	}

	public void setAuthor(Profile author) {
		this.author = author;
	}

	@JsonIgnore
	public Profile getLastEditor() {
		return lastEditor;
	}

	public void setLastEditor(Profile lastEditor) {
		this.lastEditor = lastEditor;
	}

	public void setComments(List<Comment> comments) {
		this.comments = comments;
	}

	@JsonIgnore
	public List<Comment> getComments() {
		return this.comments;
	}

	public List<String> getCommentIds() {
		return commentIds;
	}

	public void setCommentIds(List<String> commentIds) {
		this.commentIds = commentIds;
	}

	public void addCommentId(String id) {
		if (getCommentIds() != null && getCommentIds().size() < getItemcount().getLimit()) {
			getCommentIds().add(id);
		}
	}

	@JsonIgnore
	public List<Reply> getAnswers(Pager pager) {
		if (isReply()) {
			return Collections.emptyList();
		}

		List<Reply> answers = client().getChildren(this, Utils.type(Reply.class), pager);
		// we try to find the accepted answer inside the answers list, in not there, read it from db
		if (pager.getPage() < 2 && !StringUtils.isBlank(getAnswerid())) {
			Reply acceptedAnswer = null;
			for (Iterator<Reply> iterator = answers.iterator(); iterator.hasNext();) {
				Reply answer = iterator.next();
				if (getAnswerid().equals(answer.getId())) {
					acceptedAnswer = answer;
					iterator.remove();
					break;
				}
			}
			if (acceptedAnswer == null) {
				acceptedAnswer = client().read(getAnswerid());
			}
			if (acceptedAnswer != null) {
				ArrayList<Reply> sortedAnswers = new ArrayList<Reply>(answers.size() + 1);
				if (pager.isDesc()) {
					sortedAnswers.add(acceptedAnswer);
					sortedAnswers.addAll(answers);
				} else {
					sortedAnswers.addAll(answers);
					sortedAnswers.add(acceptedAnswer);
				}
				return sortedAnswers;
			}
		}
		return answers;
	}

	@JsonIgnore
	public List<Revision> getRevisions(Pager pager) {
		return client().getChildren(this, Utils.type(Revision.class), pager);
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

	public String getPostLink(boolean plural, boolean noid) {
		Post p = this;
		String ptitle = Utils.noSpaces(Utils.stripAndTrim(p.getTitle()), "-");
		String pid = (noid ? "" : "/" + p.getId() + "/" + ptitle);
		if (p.isQuestion()) {
			return plural ? "/questions" : "/question" + pid;
		} else if (p.isFeedback()) {
			return plural ? "/feedback" : "/feedback" + pid;
		} else if (p.isReply()) {
			return "/question" + (noid ?  "" : "/" + p.getParentid());
		}
		return "";
	}

	public void restoreRevisionAndUpdate(String revisionid) {
		Revision rev = client().read(revisionid);
		if (rev != null) {
			//copy rev data to post
			setTitle(rev.getTitle());
			setBody(rev.getBody());
			setTags(rev.getTags());
			setRevisionid(rev.getId());
			//update post without creating a new revision
			client().update(this);
		}
	}

	public void addFollower(User user) {
		if (followers == null) {
			followers = new LinkedHashMap<String, String>();
		}
		if (user != null && !StringUtils.isBlank(user.getEmail())) {
			followers.put(user.getId(), user.getEmail());
		}
	}

	public void removeFollower(User user) {
		if (followers != null && user != null) {
			followers.remove(user.getId());
		}
	}

	public boolean hasFollowers() {
		return (followers != null && !followers.isEmpty());
	}

	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return Objects.equal(getTitle(), ((Post) obj).getTitle()) &&
				Objects.equal(getBody(), ((Post) obj).getBody()) &&
				Objects.equal(getTags(), ((Post) obj).getTags());
	}

	public int hashCode() {
		return Objects.hashCode(getTitle(), getBody(), getTags());
	}

	public abstract boolean canHaveChildren();

	public abstract boolean canHaveRevisions();
}
