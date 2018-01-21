/*
 * Copyright 2013-2018 Erudika. https://erudika.com
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

import com.erudika.para.annotations.Locked;
import com.erudika.para.annotations.Stored;
import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.User;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.utils.ScooldUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.URL;

public class Profile extends Sysprop {

	private static final long serialVersionUID = 1L;

	@Stored private Long lastseen;
	@Stored private String location;
	@Stored private String latlng;
	@Stored private String status;
	@Stored private String aboutme;
	@Stored private String badges;
	@Stored private String groups;
	@Stored private Long upvotes;
	@Stored private Long downvotes;
	@Stored private Long comments;
	@Stored @URL private String picture;
	@Stored @URL private String website;
	@Stored private List<String> favtags;
	@Stored @Locked private List<String> spaces;
	@Stored private Boolean replyEmailsEnabled;
	@Stored private Boolean commentEmailsEnabled;

	private transient String newbadges;
	private transient Integer newreports;
	private transient User user;

	public enum Badge {
		VETERAN(10),		//regular visitor		//NOT IMPLEMENTED

		NICEPROFILE(10),	//100% profile completed
		TESTER(0),			//for testers only
		REPORTER(0),		//for every report
		VOTER(0),			//100 total votes
		COMMENTATOR(0),		//100+ comments
		CRITIC(0),			//10+ downvotes
		SUPPORTER(10),		//50+ upvotes
		EDITOR(0),			//first edit of post
		BACKINTIME(0),		//for each rollback of post
		NOOB(10),			//first question + first approved answer
		ENTHUSIAST(0),		//100+ rep  [//			 ]
		FRESHMAN(0),		//300+ rep	[////		 ]
		SCHOLAR(0),			//500+ rep	[//////		 ]
		TEACHER(0),			//1000+ rep	[////////	 ]
		PROFESSOR(0),		//5000+ rep	[//////////	 ]
		GEEK(0),			//9000+ rep	[////////////]
		GOODQUESTION(10),	//20+ votes
		GOODANSWER(10),		//10+ votes
		EUREKA(0),			//for every answer to own question
		SENIOR(0),			//one year + member
		DISCIPLINED(0),		//each time user deletes own comment
		POLYGLOT(5);		//for every approved translation

		private final int reward;

		Badge(int reward) {
			this.reward = reward;
		}

		public String toString() {
			return super.toString().toLowerCase();
		}

		public Integer getReward() {
			return this.reward;
		}
	}

	public Profile() {
		this(null, null);
	}

	public Profile(String id) {
		this(id, null);
	}

	public Profile(String userid, String name) {
		setId(id(userid));
		setName(name);
		this.status = "";
		this.aboutme = "";
		this.location = "";
		this.badges = "";
		this.upvotes = 0L;
		this.downvotes = 0L;
		this.comments = 0L;
		this.replyEmailsEnabled = Config.getConfigBoolean("reply_emails_enabled", false);
		this.commentEmailsEnabled = Config.getConfigBoolean("comment_emails_enabled", false);
	}

	public static final String id(String userid) {
		if (StringUtils.endsWith(userid, Config.SEPARATOR + "profile")) {
			return userid;
		} else {
			return userid != null ? userid + Config.SEPARATOR + "profile" : null;
		}
	}

	private ParaClient client() {
		return ScooldUtils.getInstance().getParaClient();
	}

	@JsonIgnore
	public User getUser() {
		if (user == null) {
			user = client().read(getCreatorid() == null
					? StringUtils.removeEnd(getId(), Config.SEPARATOR + "profile") : getCreatorid());
		}
		return user;
	}

	public Boolean getReplyEmailsEnabled() {
		return replyEmailsEnabled;
	}

	public void setReplyEmailsEnabled(Boolean replyEmailsEnabled) {
		this.replyEmailsEnabled = replyEmailsEnabled;
	}

	public Boolean getCommentEmailsEnabled() {
		return commentEmailsEnabled;
	}

	public void setCommentEmailsEnabled(Boolean commentEmailsEnabled) {
		this.commentEmailsEnabled = commentEmailsEnabled;
	}

	public String getGroups() {
		return groups;
	}

	public void setGroups(String groups) {
		this.groups = groups;
	}

	public String getPicture() {
		return picture;
	}

	public void setPicture(String picture) {
		this.picture = picture;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getLatlng() {
		return latlng;
	}

	public void setLatlng(String latlng) {
		this.latlng = latlng;
	}

	public String getNewbadges() {
		return newbadges;
	}

	public void setNewbadges(String newbadges) {
		this.newbadges = newbadges;
	}

	public List<String> getFavtags() {
		if (favtags == null) {
			favtags = new LinkedList<String>();
		}
		return favtags;
	}

	public void setFavtags(List<String> favtags) {
		this.favtags = favtags;
	}

	public List<String> getSpaces() {
		if (spaces == null) {
			spaces = new LinkedList<String>();
			spaces.add("default");
		}
		return spaces;
	}

	public void setSpaces(List<String> spaces) {
		this.spaces = spaces;
	}

	public Long getLastseen() {
		return lastseen;
	}

	public void setLastseen(Long val) {
		this.lastseen = val;
	}

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public Long getComments() {
		return comments;
	}

	public void setComments(Long comments) {
		this.comments = comments;
	}

	public Long getDownvotes() {
		return downvotes;
	}

	public void setDownvotes(Long downvotes) {
		this.downvotes = downvotes;
	}

	public Long getUpvotes() {
		return upvotes;
	}

	public void setUpvotes(Long upvotes) {
		this.upvotes = upvotes;
	}

	public String getBadges() {
		return badges;
	}

	public void setBadges(String badges) {
		this.badges = badges;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getAboutme() {
		return this.aboutme;
	}

	public void setAboutme(String aboutme) {
		this.aboutme = aboutme;
	}

	@SuppressWarnings("unchecked")
	public List<Question> getAllQuestions(Pager pager) {
		if (getId() == null) {
			return new ArrayList<Question>();
		}
		return (List<Question>) getPostsForUser(Utils.type(Question.class), pager);
	}

	@SuppressWarnings("unchecked")
	public List<Reply> getAllAnswers(Pager pager) {
		if (getId() == null) {
			return new ArrayList<Reply>();
		}
		return (List<Reply>) getPostsForUser(Utils.type(Reply.class), pager);
	}

	private List<? extends Post> getPostsForUser(String type, Pager pager) {
		pager.setSortby("votes");
		return client().findTerms(type, Collections.singletonMap(Config._CREATORID, getId()), true, pager);
	}

	public String getFavtagsString() {
		if (getFavtags().isEmpty()) {
			return "";
		}
		return StringUtils.join(getFavtags(), ", ");
	}

	public boolean hasFavtags() {
		return !getFavtags().isEmpty();
	}

	public long getTotalVotes() {
		if (upvotes == null) {
			upvotes = 0L;
		}
		if (downvotes == null) {
			downvotes = 0L;
		}

		return upvotes + downvotes;
	}

	public void addRep(int rep) {
		if (getVotes() == null) {
			setVotes(0);
		}
		setVotes(getVotes() + rep);
	}

	public void removeRep(int rep) {
		if (getVotes() == null) {
			setVotes(0);
		}
		setVotes(getVotes() - rep);
		if (getVotes() < 0) {
			setVotes(0);
		}
	}

	public void incrementUpvotes() {
		if (this.upvotes == null) {
			this.upvotes = 1L;
		} else {
			this.upvotes = this.upvotes + 1L;
		}
	}

	public void incrementDownvotes() {
		if (this.downvotes == null) {
			this.downvotes = 1L;
		} else {
			this.downvotes = this.downvotes + 1L;
		}
	}

	public boolean hasBadge(Badge b) {
		return StringUtils.containsIgnoreCase(badges, ",".concat(b.toString()).concat(","));
	}

	public void addBadge(Badge b) {
		String badge = b.toString();
		if (StringUtils.isBlank(badges)) {
			badges = ",";
		}
		badges = badges.concat(badge).concat(",");
		addRep(b.getReward());
	}

	public void addBadges(Badge[] larr) {
		for (Badge badge : larr) {
			addBadge(badge);
			addRep(badge.getReward());
		}
	}

	public void removeBadge(Badge b) {
		String badge = b.toString();
		if (StringUtils.isBlank(badges)) {
			return;
		}
		badge = ",".concat(badge).concat(",");

		if (badges.contains(badge)) {
			badges = badges.replaceFirst(badge, ",");
			removeRep(b.getReward());
		}
		if (StringUtils.isBlank(badges.replaceAll(",", ""))) {
			badges = "";
		}
	}

	public HashMap<String, Integer> getBadgesMap() {
		HashMap<String, Integer> badgeMap = new HashMap<String, Integer>(0);
		if (StringUtils.isBlank(badges)) {
			return badgeMap;
		}

		for (String badge : badges.split(",")) {
			Integer val = badgeMap.get(badge);
			int count = (val == null) ? 0 : val.intValue();
			badgeMap.put(badge, ++count);
		}

		badgeMap.remove("");
		return badgeMap;
	}

	public boolean isComplete() {
		return (!StringUtils.isBlank(location)
				&& !StringUtils.isBlank(aboutme)
				&& !StringUtils.isBlank(website));
	}

	public String create() {
		setLastseen(System.currentTimeMillis());
		client().create(this);
		return getId();
	}

	public void update() {
		setLastseen(System.currentTimeMillis());
		client().update(this);
	}

	public void delete() {
		client().delete(this);
		client().delete(getUser());
	}

	public String getLastname() {
		String[] s = getName().split("\\s");
		return s[s.length - 1];
	}

	public String getFirstname() {
		return getName().split("\\s")[0];
	}

	public int countNewReports() {
		if (newreports == null) {
			newreports = client().getCount(Utils.type(Report.class),
					Collections.singletonMap("properties.closed", false)).intValue();
		}
		return newreports;
	}

	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return Objects.equals(getName(), ((Profile) obj).getName())
				&& Objects.equals(getLocation(), ((Profile) obj).getLocation())
				&& Objects.equals(getId(), ((Profile) obj).getId());
	}

	public int hashCode() {
		return Objects.hashCode(getName()) + Objects.hashCode(getId());
	}
}
