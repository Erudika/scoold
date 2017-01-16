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

import com.erudika.para.annotations.Stored;
import com.erudika.para.core.User;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.utils.AppConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.URL;

public class ScooldUser extends User implements Comparable<ScooldUser>{
	private static final long serialVersionUID = 1L;

    @Stored private Long lastseen;
	@Stored private String location;
    @Stored private String status;
    @Stored private String aboutme;
	@Stored private String badges;
	@Stored private Long upvotes;
	@Stored private Long downvotes;
	@Stored private Long comments;
	@Stored @URL private String website;
	@Stored private List<String> favtags;

	private transient String newbadges;
	private transient Integer newreports;
	private transient User user;

	public static enum Badge{
		VETERAN(10),		//regular visitor		//TODO: IMPLEMENT!

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

		private int reward;

		Badge(int reward) {
			this.reward = reward;
		}

		public String toString() {
			return super.toString().toLowerCase();
		}

		public Integer getReward() {
			return this.reward;
		}
	};

    public ScooldUser () {
		this.status = "";
        this.aboutme = "";
        this.location = "";
		this.badges = "";
		this.upvotes = 0L;
		this.downvotes = 0L;
		this.comments = 0L;
		setGroups(Groups.USERS.toString());
	}

	public ScooldUser (String id) {
		this();
		setId(id(id));
	}

    public ScooldUser (String userid, String email, String name) {
        this();
		setId(id(userid));
		setName(name);
        setEmail(email);
    }

	public static final String id(String userid) {
		if (StringUtils.endsWith(userid, Config.SEPARATOR + "profile")) {
			return userid;
		} else {
			return userid != null ? userid + Config.SEPARATOR + "profile" : null;
		}
	}

	@JsonIgnore
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
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

	public Long getLastseen () {
        return lastseen;
    }

    public void setLastseen (Long val) {
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

	public List<Question> getAllQuestions(Pager pager) {
		if (getId() == null) return new ArrayList<Question>();
		return (List<Question>) getPostsForUser(Utils.type(Question.class), pager);
	}

	public List<Reply> getAllAnswers(Pager pager) {
		if (getId() == null) return new ArrayList<Reply>();
		return (List<Reply>) getPostsForUser(Utils.type(Reply.class), pager);
	}

	private List<? extends Post> getPostsForUser(String type, Pager pager) {
		pager.setSortby("votes");
		return AppConfig.client().findTerms(type, Collections.singletonMap(Config._CREATORID, getId()), true, pager);
	}

	public String getFavtagsString() {
		if (getFavtags().isEmpty()) return "";
		return StringUtils.join(getFavtags(), ", ");
	}

	public boolean hasFavtags() {
		return !getFavtags().isEmpty();
	}

	public long getTotalVotes() {
		if (upvotes == null) upvotes = 0L;
		if (downvotes == null) downvotes = 0L;

		return upvotes + downvotes;
	}

	public void addRep(int rep) {
		if (getVotes() == null) setVotes(0);
		setVotes(getVotes() + rep);
	}

	public void removeRep(int rep) {
		if (getVotes() == null) setVotes(0);
		setVotes(getVotes() - rep);
		if (getVotes() < 0) setVotes(0);
	}

	public void incrementUpvotes() {
		if (this.upvotes == null) this.upvotes = 1L;
		else	this.upvotes = this.upvotes + 1L;
	}

	public void incrementDownvotes() {
		if (this.downvotes == null) this.downvotes = 1L;
		else	this.downvotes = this.downvotes + 1L;
	}

	public boolean hasBadge(Badge b) {
		return StringUtils.containsIgnoreCase(badges, ",".concat(b.toString()).concat(","));
	}

	public void addBadge(Badge b) {
		String badge = b.toString();
		if (StringUtils.isBlank(badges)) badges = ",";
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
		if (StringUtils.isBlank(badges)) return;
		badge = ",".concat(badge).concat(",");

		if (badges.contains(badge)) {
			badges = badges.replaceFirst(badge, ",");
			removeRep(b.getReward());
		}
		if (StringUtils.isBlank(badges.replaceAll(",", ""))) {
			badges = "";
		}
	}

	public String getPicture() {
		if (StringUtils.isBlank(super.getPicture())) {
			setPicture("https://www.gravatar.com/avatar/" + Utils.md5(getEmail()) + "?size=400&d=mm&r=pg");
		}
		return super.getPicture();
	}

	public HashMap<String, Integer> getBadgesMap() {
		HashMap<String, Integer> badgeMap = new HashMap<String, Integer>(0);
		if (StringUtils.isBlank(badges)) return badgeMap;

		for (String badge : badges.split(",")) {
			Integer val = badgeMap.get(badge);
			int count = (val == null) ? 0 : val.intValue();
			badgeMap.put(badge, ++count);
		}

		badgeMap.remove("");
		return badgeMap;
	}

	public boolean isComplete() {
		return (!StringUtils.isBlank(location) &&
				!StringUtils.isBlank(aboutme) &&
				!StringUtils.isBlank(website));
	}

	public String create() {
		setLastseen(System.currentTimeMillis());
		AppConfig.client().create(this);
		return getId();
    }

    public void update() {
		AppConfig.client().update(this);
    }

    public void delete() {
		AppConfig.client().delete(this);
    }

    public String getLastname() {
		String[] s = getName().split("\\s");
        return s[s.length - 1];
    }

    public String getFirstname() {
        return getName().split("\\s")[0];
    }

    public int addContact(ScooldUser contact) {
        int count = AppConfig.client().countLinks(this, Utils.type(User.class)).intValue();
		AppConfig.client().link(this, contact.getId());
		return count + 1;
    }

    public int removeContact(ScooldUser contact) {
        int count = AppConfig.client().countLinks(this, Utils.type(User.class)).intValue();
		AppConfig.client().unlink(this, Utils.type(User.class), contact.getId());
		return count - 1;
    }

    public List<User> getAllContacts(Pager pager) {
        return AppConfig.client().getLinkedObjects(this, Utils.type(User.class), pager);
    }

	public int countNewReports() {
		if (!isModerator()) return 0;
		if (newreports == null)
			newreports = AppConfig.client().getCount(Utils.type(Report.class)).intValue();

		return newreports;
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ScooldUser other = (ScooldUser) obj;
		if ((getName() == null) ? (other.getName() != null) : !getName().equals(other.getName())) {
			return false;
		}
		if ((getEmail() == null) ? (other.getEmail() != null) : !getEmail().equals(other.getEmail())) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int hash = 3;
		hash = 89 * hash + (getName() != null ? getName().hashCode() : 0);
		hash = 89 * hash + (getEmail() != null ? getEmail().hashCode() : 0);
		return hash;
	}

	public int compareTo(ScooldUser u) {
		int deptComp = -1;
		if (getName() != null)
			deptComp = getName().compareTo(u.getName());

		return deptComp;
	}
}

