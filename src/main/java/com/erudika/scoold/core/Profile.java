/*
 * Copyright 2013-2021 Erudika. https://erudika.com
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

import com.erudika.para.core.annotations.Stored;
import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.User;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.utils.ScooldUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.URL;

public class Profile extends Sysprop {

	private static final long serialVersionUID = 1L;

	@Stored private String originalName;
	@Stored private String originalPicture;
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
	@Stored private Set<String> favspaces;
	@Stored private Set<String> spaces;
	@Stored private Boolean replyEmailsEnabled;
	@Stored private Boolean commentEmailsEnabled;
	@Stored private Boolean favtagsEmailsEnabled;
	@Stored private Boolean anonymityEnabled;
	@Stored private Boolean darkmodeEnabled;
	@Stored private Integer yearlyVotes;
	@Stored private Integer quarterlyVotes;
	@Stored private Integer monthlyVotes;
	@Stored private Integer weeklyVotes;

	private transient String newbadges;
	private transient Integer newreports;
	private transient User user;

	public enum Badge {
		VETERAN(10),		//regular visitor		//NOT IMPLEMENTED

		NICEPROFILE(10),	//100% profile completed
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
		DISCIPLINED(0);		//each time user deletes own comment

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
		this.website = "";
		this.badges = "";
		this.upvotes = 0L;
		this.downvotes = 0L;
		this.comments = 0L;
		this.yearlyVotes = 0;
		this.quarterlyVotes = 0;
		this.monthlyVotes = 0;
		this.weeklyVotes = 0;
		this.anonymityEnabled = false;
		this.darkmodeEnabled = false;
		this.favtagsEmailsEnabled = Config.getConfigBoolean("favtags_emails_enabled", false);
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

	public static Profile fromUser(User u) {
		Profile p = new Profile(u.getId(), u.getName());
		p.setUser(u);
		p.setOriginalName(u.getName());
		p.setPicture(u.getPicture());
		p.setAppid(u.getAppid());
		p.setCreatorid(u.getId());
		p.setTimestamp(u.getTimestamp());
		p.setGroups(ScooldUtils.getInstance().isRecognizedAsAdmin(u)
				? User.Groups.ADMINS.toString() : u.getGroups());
		// auto-assign spaces to new users
		String space = StringUtils.substringBefore(Config.getConfigParam("auto_assign_spaces", ""), ",");
		if (!StringUtils.isBlank(space) && !ScooldUtils.getInstance().isDefaultSpace(space)) {
			Sysprop s = client().read(ScooldUtils.getInstance().getSpaceId(space));
			if (s != null) {
				if (Config.getConfigBoolean("reset_spaces_on_new_assignment",
						u.isOAuth2User() || u.isLDAPUser() || u.isSAMLUser())) {
					p.setSpaces(Collections.singleton(s.getId() + Config.SEPARATOR + s.getName()));
				} else {
					p.getSpaces().add(s.getId() + Config.SEPARATOR + s.getName());
				}
			}
		}
		return p;
	}

	private static ParaClient client() {
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

	public Integer getYearlyVotes() {
		if (yearlyVotes < 0) {
			yearlyVotes = 0;
		}
		return yearlyVotes;
	}

	public void setYearlyVotes(Integer yearlyVotes) {
		this.yearlyVotes = yearlyVotes;
	}

	public Integer getQuarterlyVotes() {
		if (quarterlyVotes < 0) {
			quarterlyVotes = 0;
		}
		return quarterlyVotes;
	}

	public void setQuarterlyVotes(Integer quarterlyVotes) {
		this.quarterlyVotes = quarterlyVotes;
	}

	public Integer getMonthlyVotes() {
		if (monthlyVotes < 0) {
			monthlyVotes = 0;
		}
		return monthlyVotes;
	}

	public void setMonthlyVotes(Integer monthlyVotes) {
		this.monthlyVotes = monthlyVotes;
	}

	public Integer getWeeklyVotes() {
		if (weeklyVotes < 0) {
			weeklyVotes = 0;
		}
		return weeklyVotes;
	}

	public void setWeeklyVotes(Integer weeklyVotes) {
		this.weeklyVotes = weeklyVotes;
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

	public Boolean getFavtagsEmailsEnabled() {
		return favtagsEmailsEnabled;
	}

	public void setFavtagsEmailsEnabled(Boolean favtagsEmailsEnabled) {
		this.favtagsEmailsEnabled = favtagsEmailsEnabled;
	}

	public Boolean getAnonymityEnabled() {
		return anonymityEnabled;
	}

	public void setAnonymityEnabled(Boolean anonymityEnabled) {
		this.anonymityEnabled = anonymityEnabled;
	}

	public Boolean getDarkmodeEnabled() {
		return darkmodeEnabled;
	}

	public void setDarkmodeEnabled(Boolean darkmodeEnabled) {
		this.darkmodeEnabled = darkmodeEnabled;
	}

	public String getGroups() {
		return groups;
	}

	public void setGroups(String groups) {
		this.groups = groups;
	}

	public String getPicture() {
		if (StringUtils.contains(picture, "gravatar.com") && !ScooldUtils.isGravatarEnabled()) {
			if (StringUtils.contains(originalPicture, "gravatar.com")) {
				return ScooldUtils.getGravatar(this); // returns default image, not gravatar
			} else {
				return StringUtils.isBlank(originalPicture) ? ScooldUtils.getGravatar(this) : originalPicture;
			}
		}
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

	public Set<String> getFavspaces() {
		if (favspaces == null) {
			favspaces = new LinkedHashSet<String>();
		}
		return favspaces;
	}

	public void setFavspaces(Set<String> favspaces) {
		this.favspaces = favspaces;
	}

	public Set<String> getSpaces() {
		if (ScooldUtils.getInstance().isMod(this)) {
			spaces = ScooldUtils.getInstance().getAllSpaces().stream().
					map(s -> s.getId() + Config.SEPARATOR + s.getName()).collect(Collectors.toSet());
		}
		if (spaces == null) {
			spaces = new LinkedHashSet<String>();
		}
		if (spaces.isEmpty()) {
			spaces.add(Post.DEFAULT_SPACE);
		}
		// this is confusing - let admins control who is in the default space
		//if (spaces.size() > 1 && spaces.contains(Post.DEFAULT_SPACE)) {
		//	spaces.remove(Post.DEFAULT_SPACE);
		//}
		return spaces;
	}

	public void setSpaces(Set<String> spaces) {
		this.spaces = spaces;
	}

	@JsonIgnore
	public Set<String> getAllSpaces() {
		return getSpaces().stream().filter(s -> !s.equalsIgnoreCase(Post.DEFAULT_SPACE)).collect(Collectors.toSet());
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

	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}

	public String getOriginalPicture() {
		return originalPicture;
	}

	public void setOriginalPicture(String originalPicture) {
		this.originalPicture = originalPicture;
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

	@SuppressWarnings("unchecked")
	public List<Question> getAllUnapprovedQuestions(Pager pager) {
		if (getId() == null) {
			return new ArrayList<Question>();
		}
		return (List<Question>) getPostsForUser(Utils.type(UnapprovedQuestion.class), pager);
	}

	@SuppressWarnings("unchecked")
	public List<Reply> getAllUnapprovedAnswers(Pager pager) {
		if (getId() == null) {
			return new ArrayList<Reply>();
		}
		return (List<Reply>) getPostsForUser(Utils.type(UnapprovedReply.class), pager);
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

	public boolean hasSpaces() {
		return !(getSpaces().size() <= 1 && getSpaces().contains(Post.DEFAULT_SPACE));
	}

	public void removeSpace(String space) {
		String sid = ScooldUtils.getInstance().getSpaceId(space);
		Iterator<String> it = getSpaces().iterator();
		while (it.hasNext()) {
			if (it.next().startsWith(sid + Config.SEPARATOR)) {
				it.remove();
			}
		}
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
		updateVoteGains(rep);
	}

	public void removeRep(int rep) {
		if (getVotes() == null) {
			setVotes(0);
		}
		setVotes(getVotes() - rep);
		updateVoteGains(-rep);
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

	private void updateVoteGains(int rep) {
		Long updated = Optional.ofNullable(getUpdated()).orElse(getTimestamp());
		LocalDateTime lastUpdate = LocalDateTime.ofInstant(Instant.ofEpochMilli(updated), ZoneId.systemDefault());
		LocalDate now = LocalDate.now();
		if (now.getYear() != lastUpdate.getYear()) {
			yearlyVotes = rep;
		} else {
			yearlyVotes += rep;
		}
		if (now.get(IsoFields.QUARTER_OF_YEAR) != lastUpdate.get(IsoFields.QUARTER_OF_YEAR)) {
			quarterlyVotes = rep;
		} else {
			quarterlyVotes += rep;
		}
		if (now.getMonthValue() != lastUpdate.getMonthValue()) {
			monthlyVotes = rep;
		} else {
			monthlyVotes += rep;
		}
		if (now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) != lastUpdate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)) {
			weeklyVotes = rep;
		} else {
			weeklyVotes += rep;
		}
		setUpdated(Utils.timestamp());
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
		updateVoteGains(0); // reset vote gains if they we're past the time frame
		client().update(this);
	}

	public void delete() {
		client().delete(this);
		client().delete(getUser());
		ScooldUtils.getInstance().unsubscribeFromAllNotifications(this);
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
