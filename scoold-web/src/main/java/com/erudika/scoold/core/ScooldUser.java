package com.erudika.scoold.core;

import com.erudika.para.annotations.Stored;
import com.erudika.para.core.User;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.utils.AppConfig;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class ScooldUser extends User implements Comparable<ScooldUser>{
	private static final long serialVersionUID = 1L;

    @Stored private Long lastseen;
    @Stored private String subType;
	@Stored private String location;
	@Stored private Long dob;
	@Stored private Integer dobday;
	@Stored private Integer dobmonth;
	@Stored private Integer dobyear;
    @Stored private String status;
    @Stored private String ilike;
    @Stored private String aboutme;
	@Stored private String badges;
	@Stored private Long upvotes;
	@Stored private Long downvotes;
	@Stored private Long comments;
	@Stored private String contacts;
	@Stored private List<String> favtags;
	@Stored private String newbadges;
	@Stored private String eduperiods; // JSON {schoolid: 1234(from):1234(to)}, schoolid: {...}}

	private transient Integer newreports;
	private transient boolean isGroupMember;

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
		FIRSTCLASS(0),		//first class joined
		BACKTOSCHOOL(0),	//first school joined
		CONNECTED(10),		//10+ contacts added
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

	public static enum UserType{
		ALUMNUS, STUDENT, TEACHER;

		public String toString() {
			return super.toString().toLowerCase();
		}
	};

    public ScooldUser () {
		this.status = "";
        this.ilike = "";
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
		setId(id);
	}

    public ScooldUser (String email, Boolean active, UserType type, String name) {
        this();
		setName(name);
        setEmail(email);
		setActive(active);
		this.subType =  type.toString();
    }

	public String getEduperiods() {
		return eduperiods;
	}

	public void setEduperiods(String eduperiods) {
		this.eduperiods = eduperiods;
	}

	public boolean getIsGroupMember() {
		return isGroupMember;
	}

	public void setIsGroupMember(boolean isGroupMember) {
		this.isGroupMember = isGroupMember;
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

    public String getSubType() {
        return subType;
    }

	public void setSubType(String subType) {
		this.subType = getUserType(subType).toString();
	}

	public String getContacts() {
		return contacts;
	}

	public void setContacts(String contacts) {
		this.contacts = contacts;
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

    public Long getDob() {
		if (dobday != null && dobmonth != null && dobyear != null) {
			String date = dobyear + "-" + dobmonth + "-" + dobday;
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			try {
				dob = sdf.parse(date).getTime();
			} catch (ParseException ex) {}
		}
		return dob;
    }

    public void setDob(Long dob) {
        this.dob = dob;
    }

	public Integer getDobday() {
		return dobday;
	}

	public void setDobday(Integer dobday) {
		this.dobday = dobday;
	}

	public Integer getDobmonth() {
		return dobmonth;
	}

	public void setDobmonth(Integer dobmonth) {
		this.dobmonth = dobmonth;
	}

	public Integer getDobyear() {
		return dobyear;
	}

	public void setDobyear(Integer dobyear) {
		this.dobyear = dobyear;
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

    public String getIlike() {
        return ilike;
    }

    public void setIlike(String ilike) {
        this.ilike = ilike;
    }

    public String getAboutme() {
        return this.aboutme;
    }

    public void setAboutme(String aboutme) {
        this.aboutme = aboutme;
    }

	public void addSchoolPeriod(String id, Integer from, Integer to) {
		if (StringUtils.isBlank(id)) return;
		Map<String, String> map = getSchoolPeriodsMap();
		map.put(id, getLinkMetadata(from, to));
		try {
			eduperiods = ParaObjectUtils.getJsonWriter().writeValueAsString(map);
		} catch (Exception e) {}
	}

	public void removeSchoolPeriod(String id) {
		if (StringUtils.isBlank(id)) return;
		Map<String, String> map = getSchoolPeriodsMap();
		map.remove(id);
		try {
			eduperiods = ParaObjectUtils.getJsonWriter().writeValueAsString(map);
		} catch (Exception e) {}
	}

	public Map<String, String> getSchoolPeriodsMap() {
		if (StringUtils.isBlank(eduperiods)) eduperiods = "{}";
		Map<String, String> map;
		try {
			map = ParaObjectUtils.getJsonReader(Map.class).readValue(eduperiods);
		} catch (Exception e) { map = new HashMap<String,String>();	}
		return map;
	}

	private String getLinkMetadata(Integer from, Integer to) {
		int min = 1900; int max = 3000;
		Integer fyear = (from == null || from < min || from > max) ? Integer.valueOf(0) : from;
		Integer tyear = (to == null || to < min || to > max) ? Integer.valueOf(0) : to;
		// format: [id -> "fromyear=0:toyear=2000"]
		return fyear.toString().concat(Config.SEPARATOR).concat(tyear.toString());
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

	public Badge getTitleBadge() {
		if (hasBadge(Badge.GEEK)) {
			return Badge.GEEK;
		} else if (hasBadge(Badge.PROFESSOR)) {
			return Badge.PROFESSOR;
		} else if (hasBadge(Badge.SCHOLAR)) {
			return Badge.SCHOLAR;
		} else if (hasBadge(Badge.TEACHER)) {
			return Badge.TEACHER;
		} else if (hasBadge(Badge.FRESHMAN)) {
			return Badge.FRESHMAN;
		} else if (hasBadge(Badge.ENTHUSIAST)) {
			return Badge.ENTHUSIAST;
		} else if (hasBadge(Badge.NOOB)) {
			return Badge.NOOB;
		} else {
			return null;
		}
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

    public String getRawDobString() {
        if (dob == null) return "";
        return dob.toString();
    }

	public boolean isComplete() {
		return (dob != null &&
			!StringUtils.isBlank(location) &&
			!StringUtils.isBlank(aboutme) &&
			!StringUtils.isBlank(ilike) &&
			!StringUtils.isBlank(contacts)) ? true : false;
	}

    public List<ContactDetail> getAllContactDetails() {
		return ContactDetail.toContactsList(contacts);
	}

    public int getAge() {
        if (dob == null)
            return 0;

        //dob cal
        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(dob);
        //now
        Calendar c2 = new GregorianCalendar();
        int y = c2.get(Calendar.YEAR) - c1.get(Calendar.YEAR);
        int m = c2.get(Calendar.MONTH) - c1.get(Calendar.MONTH);
        int d = c2.get(Calendar.DAY_OF_MONTH) - c1.get(Calendar.DAY_OF_MONTH);
        if (m < 0) //birthday month is ahead
            y--;
        else if (m == 0 && d < 0) //birthday month is now but bday is not today
            y--;

		if (y < 0) y = 0;

        return y;
    }
    //AKA Birthday
    public int getDobDay() {
        if (dob == null)
            return 0;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dob);
        return c.get(Calendar.DAY_OF_MONTH);
    }
    public int getDobMonth() {
        if (dob == null)
            return 0;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dob);

        return c.get(Calendar.MONTH) +1;
    }
    public int getDobYear() {
        if (dob == null)
            return 0;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dob);
        return c.get(Calendar.YEAR);
    }

    public String create() {
		setLastseen(System.currentTimeMillis());
		User u = AppConfig.client().create(this);
		if (u != null) {
			setId(u.getId());
			setTimestamp(u.getTimestamp());
			return u.getId();
		}
		return null;
    }

    public void delete() {
		AppConfig.client().delete(this);
    }

	private UserType getUserType(String type) {
		if (type == null) return UserType.STUDENT;
		try{
            return UserType.valueOf(type.trim().toUpperCase());
        }catch(IllegalArgumentException e) {
            //oh shit!
			return UserType.STUDENT;
        }
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

    public List<ScooldUser> getAllContacts(Pager pager) {
        return AppConfig.client().getLinkedObjects(this, Utils.type(User.class), pager);
    }

    public List<Classunit> getAllClassUnits(Pager pager) {
        return AppConfig.client().getLinkedObjects(this, Utils.type(Classunit.class), pager);
    }

    public List<School> getAllSchools(Pager pager) {
        return AppConfig.client().getLinkedObjects(new User(getId()), Utils.type(School.class), pager);
    }

    public List<Group> getAllGroups(Pager pager) {
        return AppConfig.client().getLinkedObjects(this, Utils.type(Group.class), pager);
    }

    public boolean isFriendWith(ScooldUser user) {
		if (user == null) return false;
		else if (user.getId().equals(getId())) return true;

        return AppConfig.client().isLinked(this, Utils.type(User.class), user.getId());
    }

	public Map<String, String> getSimpleSchoolsMap() {
		Map<String, String> m = new HashMap<String, String>();
        for(School school : this.getAllSchools(new Pager(AppConfig.MAX_SCHOOLS_PER_USER))) {
			m.put(school.getId(), school.getName());
        }
		return m;
	}

	public Map<String, School> getSchoolsMap() {
		Map<String, School> m = new HashMap<String, School>();
        for(School school : this.getAllSchools(new Pager(AppConfig.MAX_SCHOOLS_PER_USER))) {
			m.put(school.getId(), school);
        }
		return m;
	}

	public Map<String, String> getSimpleGroupsMap() {
		Map<String, String> m = new HashMap<String, String>();
        for(Group group : this.getAllGroups(new Pager(AppConfig.MAX_GROUPS_PER_USER))) {
			m.put(group.getId(), group.getName());
        }
		return m;
	}

	public Map<String, Group> getGroupsMap() {
		Map<String, Group> m = new HashMap<String, Group>();

        for(Group group : this.getAllGroups(new Pager(AppConfig.MAX_GROUPS_PER_USER))) {
			m.put(group.getId(), group);
        }
		return m;
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

