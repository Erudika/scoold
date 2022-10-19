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

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.Tag;
import com.erudika.para.core.User;
import com.erudika.para.core.Vote;
import com.erudika.para.core.annotations.Stored;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import com.erudika.scoold.ScooldServer;
import com.erudika.scoold.utils.ScooldUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public abstract class Post extends Sysprop {

	private static final long serialVersionUID = 1L;
	private static final ScooldConfig CONF = ScooldUtils.getConfig();

	public static final String DEFAULT_SPACE = "scooldspace:default";
	public static final String ALL_MY_SPACES = "scooldspace:*";

	@Stored
	private String body;
	@Stored @NotBlank @Size(min = 2, max = 255)
	private String title;
	@Stored @NotEmpty @Size(min = 1)
	private List<String> tags;

	@Stored private Long viewcount;
	@Stored private String answerid;
	@Stored private String revisionid;
	@Stored private String closerid;
	@Stored private Long answercount;
	@Stored private Long lastactivity;
	@Stored private Long lastedited;
	@Stored private String lasteditby;
	@Stored private String deletereportid;
	@Stored private String location;
	@Stored private String address;
	@Stored private String latlng;
	@Stored private List<String> commentIds;
	@Stored private String space;
	@Stored private Map<String, String> followers;
	@Stored private Boolean deprecated;

	private transient Profile author;
	private transient Profile lastEditor;
	private transient List<Comment> comments;
	private transient Pager itemcount;
	private transient Vote vote;

	public Post() {
		this.answercount = 0L;
		this.viewcount = 0L;
	}

	private ParaClient client() {
		return ScooldUtils.getInstance().getParaClient();
	}

	public Boolean getDeprecated() {
		if (deprecated == null || isReply()) {
			deprecated = false;
		}
		return deprecated;
	}

	public void setDeprecated(Boolean deprecated) {
		this.deprecated = deprecated;
	}

	public Long getLastactivity() {
		if (lastactivity == null || lastactivity <= 0) {
			lastactivity = getUpdated();
		}
		return lastactivity;
	}

	public void setLastactivity(Long lastactivity) {
		this.lastactivity = lastactivity;
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

	@JsonIgnore
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

	public Vote getVote() {
		return vote;
	}

	public void setVote(Vote vote) {
		this.vote = vote;
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

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getLatlng() {
		return latlng;
	}

	public void setLatlng(String latlng) {
		this.latlng = latlng;
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
		return !StringUtils.isBlank(this.closerid);
	}

	public String getTagsString() {
		if (getTags() == null || getTags().isEmpty()) {
			return "";
		}
		Collections.sort(getTags());
		return StringUtils.join(getTags(), ",");
	}

	public String create() {
		updateTags(null, getTags());
		this.body = Utils.abbreviate(this.body, ScooldUtils.getConfig().maxPostLength());
		Post p = client().create(this);
		if (p != null) {
			Revision.createRevisionFromPost(p, true);
			setId(p.getId());
			setTimestamp(p.getTimestamp());
			return p.getId();
		}
		return null;
	}

	public void update() {
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

		for (ParaObject reply : client().getChildren(this, Utils.type(Reply.class))) {
			// delete answer
			children.add(reply);
			// delete Comments
			children.addAll(client().getChildren(reply, Utils.type(Comment.class)));
			// delete Revisions
			children.addAll(client().getChildren(reply, Utils.type(Revision.class)));
		}
		for (ParaObject child : children) {
			ids.add(child.getId());
		}
		updateTags(getTags(), null);
		client().deleteAll(ids);
		client().delete(this);
	}

	public static String getTagString(String tag) {
		return StringUtils.truncate(tag, 35);
	}

	public void updateTags(List<String> oldTags, List<String> newTags) {
		List<String> deleteUs = new LinkedList<>();
		List<Tag> updateUs = new LinkedList<>();
		Map<String, Tag> oldTagz = Optional.ofNullable(oldTags).orElse(Collections.emptyList()).stream().
				map(t -> new Tag(getTagString(t))).distinct().collect(Collectors.toMap(t -> t.getId(), t -> t));
		Map<String, Tag> newTagz = Optional.ofNullable(newTags).orElse(Collections.emptyList()).stream().
				map(t -> new Tag(getTagString(t))).distinct().collect(Collectors.toMap(t -> t.getId(), t -> t));
		Map<String, Tag> existingTagz = client().readAll(Stream.concat(oldTagz.keySet().stream(), newTagz.keySet().
				stream()).distinct().collect(Collectors.toList())).
				stream().collect(Collectors.toMap(t -> t.getId(), t -> (Tag) t));
		// add newly created tags
		client().createAll(newTagz.values().stream().filter(t -> {
			t.setCount(1);
			return !existingTagz.containsKey(t.getId());
		}).collect(Collectors.toList()));
		// increment or decrement the count of the rest
		existingTagz.values().forEach(t -> {
			if (!oldTagz.containsKey(t.getId()) && newTagz.containsKey(t.getId())) {
				t.setCount(t.getCount() + 1);
				updateUs.add(t);
			} else if (oldTagz.containsKey(t.getId()) && (newTags == null || !newTagz.containsKey(t.getId()))) {
				t.setCount(t.getCount() - 1);
				if (t.getCount() <= 0) {
					// check if actual count is different
					int c = client().getCount(Utils.type(Question.class),
							Collections.singletonMap(Config._TAGS, t.getTag())).intValue();
					if (c <= 1) {
						deleteUs.add(t.getId());
					} else {
						t.setCount(c);
					}
				} else {
					updateUs.add(t);
				}
			} // else: count remains unchanged
		});
		client().updateAll(updateUs);
		client().deleteAll(deleteUs);
		int tagsLimit = Math.min(CONF.maxTagsPerPost(), 100);
		setTags(newTagz.values().stream().limit(tagsLimit).map(t -> t.getTag()).collect(Collectors.toList()));
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

	@JsonIgnore // DO NOT REMOVE! clashes with User.getComments() field in index
	public List<Comment> getComments() {
		return this.comments;
	}

	public List<String> getCommentIds() {
		return commentIds;
	}

	public String getSpace() {
		if (StringUtils.isBlank(space)) {
			space = DEFAULT_SPACE;
		}
		return space;
	}

	public void setSpace(String space) {
		this.space = space;
	}

	public void setCommentIds(List<String> commentIds) {
		this.commentIds = commentIds;
	}

	public boolean addCommentId(String id) {
		if (getCommentIds() != null && getCommentIds().size() < getItemcount().getLimit()) {
			return getCommentIds().add(id);
		}
		return false;
	}

	@JsonIgnore
	public List<Reply> getAnswers(Pager pager) {
		return getAnswers(Reply.class, pager);
	}

	@JsonIgnore
	public List<Reply> getUnapprovedAnswers(Pager pager) {
		if (isReply()) {
			return Collections.emptyList();
		}
		return client().getChildren(this, Utils.type(UnapprovedReply.class), pager);
	}

	private List<Reply> getAnswers(Class<? extends Reply> type, Pager pager) {
		if (isReply()) {
			return Collections.emptyList();
		}

		List<Reply> answers = client().getChildren(this, Utils.type(type), pager);
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
		String ctx = CONF.serverContextPath();
		if (p.isQuestion()) {
			return ctx + (plural ? ScooldServer.QUESTIONSLINK : ScooldServer.QUESTIONLINK + pid);
		} else if (p.isFeedback()) {
			return ctx + ScooldServer.FEEDBACKLINK + (plural ? "" : pid);
		} else if (p.isReply()) {
			return ctx + ScooldServer.QUESTIONLINK + (noid ? "" : "/" + p.getParentid());
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
			setLastactivity(System.currentTimeMillis());
			//update post without creating a new revision
			client().update(this);
			ScooldUtils.getInstance().triggerHookEvent("revision.restore", rev);
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

	public boolean hasUpdatedContent(Post beforeUpdate) {
		if (beforeUpdate == null) {
			return false;
		}
		return !StringUtils.equals(getTitle(), beforeUpdate.getTitle())
				|| !StringUtils.equals(getBody(), beforeUpdate.getBody())
				|| !Objects.equals(getTags(), beforeUpdate.getTags());
	}

	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return Objects.equals(getTitle(), ((Post) obj).getTitle())
				&& Objects.equals(getBody(), ((Post) obj).getBody())
				&& Objects.equals(getLocation(), ((Post) obj).getLocation())
				&& Objects.equals(getSpace(), ((Post) obj).getSpace())
				&& Objects.equals(getTags(), ((Post) obj).getTags());
	}

	public int hashCode() {
		return Objects.hashCode(getTitle()) + Objects.hashCode(getBody()) + Objects.hashCode(getTags());
	}
}
