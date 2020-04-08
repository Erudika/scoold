/*
 * Copyright 2013-2020 Erudika. https://erudika.com
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
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.utils.ScooldUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public abstract class Post extends Sysprop {

	private static final long serialVersionUID = 1L;

	public static final String DEFAULT_SPACE = "scooldspace:integrations";
	public static final String ALL_MY_SPACES = "scooldspace:*";

	@Stored
	private String body;
	@Stored @NotBlank @Size(min = 2, max = 255)
	private String title;
	@Stored @NotEmpty @Size(min = 1, max = 5)
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
		createTags();
		this.body = Utils.abbreviate(this.body, Config.getConfigInt("max_post_length", 20000));
		Post p = client().create(this);
		if (p != null) {
			setRevisionid(Revision.createRevisionFromPost(p, true).create());
			setId(p.getId());
			setTimestamp(p.getTimestamp());
			return p.getId();
		}
		return null;
	}

	public void update() {
		setRevisionid(Revision.createRevisionFromPost(this, false).create());
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
		client().deleteAll(ids);
		client().delete(this);
		updateTags(getTags(), null);
	}

	public static String getTagString(String tag) {
		if (StringUtils.isBlank(tag)) {
			return "";
		}
		String s = tag.replaceAll("[\\p{S}\\p{P}\\p{C}&&[^+\\.]]", " ").replaceAll("\\p{Z}+", " ").trim();
		return StringUtils.truncate(Utils.noSpaces(s, "-"), 35);
	}

	private void createTags() {
		if (getTags() == null || getTags().isEmpty()) {
			return;
		}
		ArrayList<Tag> tagz = new ArrayList<Tag>();
		Pager tagged = new Pager(0);
		for (int i = 0; i < getTags().size(); i++) {
			String ntag = getTags().get(i);
			Tag t = new Tag(getTagString(ntag));
			if (!StringUtils.isBlank(t.getTag())) {
				tagged.setCount(0);
				client().findTagged(getType(), new String[]{t.getTag()}, tagged);
				t.setCount((int) tagged.getCount() + 1);
				getTags().set(i, t.getTag());
				tagz.add(t);
			}
		}
		client().createAll(tagz);
	}

	public void updateTags(List<String> oldTags, List<String> newTags) {
		if (oldTags == null || oldTags.isEmpty()) {
			return;
		}
		List<String> toDelete = new LinkedList<>();
		List<Tag> toCreate = new LinkedList<>();
		Map<String, Tag> idTags = new LinkedHashMap<>();
		Set<String> removedTags = new HashSet<>();
		Set<String> addedTags = new HashSet<>();
		Set<String> oldTagsSet = new HashSet<>();
		Set<String> newTagsSet = new HashSet<>();
		Pager tagged = new Pager(1);
		oldTagsSet.addAll(oldTags);

		if (newTags != null) {
			for (String newTag : newTags) {
				String tag = getTagString(newTag);
				if (!StringUtils.isBlank(tag)) {
					newTagsSet.add(tag);
					if (!oldTagsSet.contains(tag)) {
						Tag t = new Tag(tag);
						t.setCount(1);
						addedTags.add(tag);
						idTags.put(t.getId(), t);
					}
				}
			}
		}
		for (String oldTag : oldTags) {
			if (!newTagsSet.contains(oldTag)) {
				Tag t = new Tag(oldTag);
				t.setCount(0);
				removedTags.add(oldTag);
				idTags.put(t.getId(), t);
			}
		}
		for (String tag : idTags.keySet()) {
			tagged.setCount(0);
			Tag t = new Tag(tag);
			client().findTagged(getType(), new String[]{t.getTag()}, tagged);
			if (addedTags.contains(t.getTag())) {
				t.setCount((int) tagged.getCount() + 1);
			} else if (removedTags.contains(t.getTag())) {
				t.setCount((int) tagged.getCount() - 1);
			}
			idTags.put(t.getId(), t);
		}
		for (Tag tag : idTags.values()) {
			if (tag.getCount() <= 0) {
				toDelete.add(tag.getId());
			} else {
				toCreate.add(tag);
			}
		}
		client().deleteAll(toDelete);
		client().createAll(toCreate);
		setTags(new ArrayList<>(newTagsSet));
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

	public void addCommentId(String id) {
		if (getCommentIds() != null && getCommentIds().size() < getItemcount().getLimit()) {
			getCommentIds().add(id);
		}
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
		if (p.isQuestion()) {
			return plural ? "/questions" : "/question" + pid;
		} else if (p.isFeedback()) {
			return plural ? "/feedback" : "/feedback" + pid;
		} else if (p.isReply()) {
			return "/question" + (noid ? "" : "/" + p.getParentid());
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

	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return Objects.equals(getTitle(), ((Post) obj).getTitle())
				&& Objects.equals(getBody(), ((Post) obj).getBody())
				&& Objects.equals(getSpace(), ((Post) obj).getSpace())
				&& Objects.equals(getTags(), ((Post) obj).getTags());
	}

	public int hashCode() {
		return Objects.hashCode(getTitle()) + Objects.hashCode(getBody()) + Objects.hashCode(getTags());
	}
}
