package com.erudika.scoold.core;

import com.erudika.para.core.PObject;
import com.erudika.para.utils.DAO;
import com.erudika.para.utils.Search;
import com.erudika.para.utils.Stored;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Media.MediaType;
import com.erudika.scoold.util.Constants;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.codehaus.jackson.type.TypeReference;

public class User extends com.erudika.para.core.User implements Comparable<User>{
	private static final long serialVersionUID = 1L;

    @Stored private Long lastseen;
    @Stored private String type;
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
	@Stored private Long reputation;
	@Stored private String contacts;
	@Stored private String identifier;
	@Stored private Long photos;
	@Stored private String favtags;
	@Stored private String newbadges;
	@Stored private Integer newmessages;
	@Stored private String eduperiods; // JSON {schoolid: 1234(from):1234(to)}, schoolid: {...}}
	@Stored private String identifiertwo;
	
	private transient Integer newreports;
	private transient boolean isGroupMember;
	private transient Long authstamp;

	public static enum UserGroup{
		ALUMNI, STUDENTS, TEACHERS, ADMINS, MODS;
		
		public String toString(){
			return super.toString().toLowerCase();
		}
	}

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
		PHOTOLOVER(0),		//20+ photos added
		FIRSTCLASS(0),		//first class joined
		BACKTOSCHOOL(0),	//first school joined
		CONNECTED(10),		//10+ contacts added
		DISCIPLINED(0),		//each time user deletes own comment
		POLYGLOT(5);		//for every approved translation

		private int reward;

		Badge(int reward){
			this.reward = reward;
		}

		public String toString() {
			return super.toString().toLowerCase();
		}

		public Integer getReward(){
			return this.reward;
		}
	};

	public static enum UserType{
		ALUMNUS, STUDENT, TEACHER;

		public String toString(){
			return super.toString().toLowerCase();
		}

		public String toGroupString(){
			switch(this){
				case ALUMNUS: return UserGroup.ALUMNI.toString();
				case STUDENT: return UserGroup.STUDENTS.toString();
				case TEACHER: return UserGroup.TEACHERS.toString();
				default: return UserGroup.STUDENTS.toString();
			}
		}		
	};

    public User () {
		setName("");
		this.status = "";
        this.ilike = "";
        this.aboutme = "";
        this.location = "";
		this.badges = "";
		this.upvotes = 0L;
		this.downvotes = 0L;
		this.comments = 0L;
		this.reputation = 0L;
		this.photos = 0L;
		setGroups(UserGroup.STUDENTS.toString());
	}

	public User (String id){
		this();
		setId(id);
	}

    public User (String email, Boolean active, UserType type, String name) {
        this();
		setName(name);
        setEmail(email);
		setActive(active);
		this.type =  type.toString();
    }

	public String getIdentifiertwo() {
		return identifiertwo;
	}

	public void setIdentifiertwo(String identifiertwo) {
		this.identifiertwo = identifiertwo;
	}
	
	public String getEduperiods() {
		return eduperiods;
	}

	public void setEduperiods(String eduperiods) {
		this.eduperiods = eduperiods;
	}
	
	public Integer getNewmessages() {
		return newmessages;
	}

	public void setNewmessages(Integer newmessages) {
		this.newmessages = newmessages;
	}

	public boolean isIsGroupMember() {
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
	
	public String getFavtags() {
		return favtags;
	}
	
	public void setFavtags(String favtags) {
		this.favtags = favtags;
	}

	public Long getPhotos() {
		return photos;
	}

	public void setPhotos(Long photos) {
		this.photos = photos;
	}

	public Long getLastseen () {
        return lastseen;
    }

    public void setLastseen (Long val) {
        this.lastseen = val;
    }
      
    public String getType() {
        return type;
    }
    
	public void setType(String type){
		this.type = getUserType(type).toString();
	}

	public String getContacts() {
		return contacts;
	}

	public void setContacts(String contacts) {
		this.contacts = contacts;
	}

	public Long getReputation() {
		return reputation;
	}

	public void setReputation(Long reputation) {
		this.reputation = reputation;
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
		if(dobday != null && dobmonth != null && dobyear != null) {
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
	
	public void addSchoolPeriod(String id, Integer from, Integer to){
		if(StringUtils.isBlank(id)) return;
		Map<String, String> map = getSchoolPeriodsMap();
		map.put(id, getLinkMetadata(from, to));
		try {
			eduperiods = Utils.getObjectMapper().writeValueAsString(map);
		} catch (Exception e) {}
	}
	
	public void removeSchoolPeriod(String id){
		if(StringUtils.isBlank(id)) return;
		Map<String, String> map = getSchoolPeriodsMap();
		map.remove(id);
		try {
			eduperiods = Utils.getObjectMapper().writeValueAsString(map);
		} catch (Exception e) {}
	}
	
	public Map<String, String> getSchoolPeriodsMap(){
		if(StringUtils.isBlank(eduperiods)) eduperiods = "{}";
		Map<String, String> map = null;
		try {
			map = Utils.getObjectMapper().readValue(eduperiods, new TypeReference<Map<String, String>>() {});
//			eduperiods = Utils.getObjectMapper().writeValueAsString(map);
		} catch (Exception e) { map = new HashMap<String,String>();	}
		return map;
	}

	private String getLinkMetadata(Integer from, Integer to){
		int min = 1900; int max = 3000;
		Integer fyear = (from == null || from < min || from > max) ? Integer.valueOf(0) : from;
		Integer tyear = (to == null || to < min || to > max) ? Integer.valueOf(0) : to;
		// format: [id -> "fromyear=0:toyear=2000"]
		return fyear.toString().concat(Utils.SEPARATOR).concat(tyear.toString());
	}
	
	public ArrayList<Question> getAllQuestions(MutableLong pagenum, MutableLong itemcount){
		if(getId() == null) return new ArrayList<Question> ();
		return (ArrayList<Question>) getPostsForUser(PObject.classname(Question.class), pagenum, itemcount);
	}

	public ArrayList<Reply> getAllAnswers(MutableLong pagenum, MutableLong itemcount){
		if(getId() == null) return new ArrayList<Reply> ();
		return (ArrayList<Reply>) getPostsForUser(PObject.classname(Reply.class), pagenum, itemcount);
	}
	
	private ArrayList<? extends Post> getPostsForUser(String type, MutableLong pagenum, MutableLong itemcount){
		return Search.findTerm(type, pagenum, itemcount, DAO.CN_CREATORID, getId(), "votes", true, Utils.MAX_ITEMS_PER_PAGE);
	}

	public String getFavtagsString(){
		if(StringUtils.isBlank(favtags)) return "";
		return favtags.substring(1, favtags.length() - 1).replaceAll(",", ", ");
	}

	public boolean hasFavtags(){
		return !StringUtils.isBlank(favtags);
	}

	public ArrayList<String> getFavtagsList(){
		if(StringUtils.isBlank(favtags)) return new ArrayList<String> ();
		ArrayList<String> list = new ArrayList<String>();
		list.addAll(Arrays.asList(favtags.substring(1, favtags.length() - 1).split(",")));
		return list;
	}

	public long getTotalVotes(){
		if(upvotes == null) upvotes = 0L;
		if(downvotes == null) downvotes = 0L;

		return upvotes + downvotes;
	}

	public void addRep(int rep){
		if(reputation == null ) reputation = 0L;
		reputation += rep;
	}

	public void removeRep(int rep){
		if(reputation == null ) reputation = 0L;
		reputation -= rep;
 		if(reputation < 0) reputation = 0L;
	}

	public void incrementUpvotes(){
		if(this.upvotes == null) this.upvotes = 1L;
		else	this.upvotes = this.upvotes + 1L;
	}

	public void incrementDownvotes(){
		if(this.downvotes == null) this.downvotes = 1L;
		else	this.downvotes = this.downvotes + 1L;
	}

	public boolean hasBadge(Badge b){
		return StringUtils.containsIgnoreCase(badges, ",".concat(b.toString()).concat(","));
	}

	public void addBadge(Badge b){
		String badge = b.toString();
		if(StringUtils.isBlank(badges)) badges = ",";
		badges = badges.concat(badge).concat(",");
		addRep(b.getReward());
	}

	public void addBadges(Badge[] larr){
		for (Badge badge : larr){
			addBadge(badge);
			addRep(badge.getReward());
		}
	}

	public void removeBadge(Badge b){
		String badge = b.toString();
		if(StringUtils.isBlank(badges)) return;
		badge = ",".concat(badge).concat(",");

		if(badges.contains(badge)){
			badges = badges.replaceFirst(badge, ",");
			removeRep(b.getReward());
		}
		if(StringUtils.isBlank(badges.replaceAll(",", ""))){
			badges = "";
		}
	}

	public Badge getTitleBadge(){
		if(hasBadge(Badge.GEEK)){
			return Badge.GEEK;
		}else if(hasBadge(Badge.PROFESSOR)){
			return Badge.PROFESSOR;
		}else if(hasBadge(Badge.SCHOLAR)){
			return Badge.SCHOLAR;
		}else if(hasBadge(Badge.TEACHER)){
			return Badge.TEACHER;
		}else if(hasBadge(Badge.FRESHMAN)){
			return Badge.FRESHMAN;
		}else if(hasBadge(Badge.ENTHUSIAST)){
			return Badge.ENTHUSIAST;
		}else if(hasBadge(Badge.NOOB)){
			return Badge.NOOB;
		}else{
			return null;
		}
	}

	public HashMap<String, Integer> getBadgesMap(){
		HashMap<String, Integer> badgeMap = new HashMap<String, Integer>(0);
		if(StringUtils.isBlank(badges)) return badgeMap;

		for (String badge : badges.split(",")) {
			Integer val = badgeMap.get(badge);
			int count = (val == null) ? 0 : val.intValue();
			badgeMap.put(badge, ++count);
		}

		badgeMap.remove("");
		return badgeMap;
	}

    public String getRawDobString(){
        if(dob == null) return "";
        return dob.toString();
    }

	public boolean isComplete(){
		return (dob != null &&
			!StringUtils.isBlank(location) &&
			!StringUtils.isBlank(aboutme) &&
			!StringUtils.isBlank(ilike) &&
			!StringUtils.isBlank(contacts)) ? true : false;
	}

    public ArrayList<ContactDetail> getAllContactDetails(){
		return ContactDetail.toContactsList(contacts);
	}

    public int getAge(){
        if(dob == null)
            return 0;

        //dob cal
        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(dob);
        //now
        Calendar c2 = new GregorianCalendar();
        int y = c2.get(Calendar.YEAR) - c1.get(Calendar.YEAR);
        int m = c2.get(Calendar.MONTH) - c1.get(Calendar.MONTH);
        int d = c2.get(Calendar.DAY_OF_MONTH) - c1.get(Calendar.DAY_OF_MONTH);
        if(m < 0) //birthday month is ahead
            y--;
        else if(m == 0 && d < 0) //birthday month is now but bday is not today
            y--;

		if(y < 0) y = 0;

        return y;
    }
    //AKA Birthday
    public int getDobDay(){
        if(dob == null)
            return 0;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dob);
        return c.get(Calendar.DAY_OF_MONTH);
    }
    public int getDobMonth(){
        if(dob == null)
            return 0;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dob);

        return c.get(Calendar.MONTH) +1;
    }
    public int getDobYear(){
        if(dob == null)
            return 0;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dob);
        return c.get(Calendar.YEAR);
    }

    public String create() {
		setLastseen(System.currentTimeMillis());
		return DAO.getInstance().createUser(this);
    }

    public void update(){
		DAO.getInstance().updateUser(this);
    }

    public void delete(){
		deleteChildren(Message.class);
		deleteChildren(Media.class);
		DAO.getInstance().deleteUser(this);
		DAO.getInstance().deleteIdentifier(identifiertwo);
    }

	public ArrayList<Media> getMedia(MediaType type, String label, MutableLong pagenum,
			MutableLong itemcount, int maxItems, boolean reverse) {
		return Media.getAllMedia(getId(), type, pagenum, itemcount, reverse, maxItems);
	}

	public void deleteAllMedia(){
		deleteChildren(Media.class);
	}
    
	private UserType getUserType(String type){
		if(type == null) return UserType.STUDENT;
		try{
            return UserType.valueOf(type.trim().toUpperCase());
        }catch(IllegalArgumentException e){
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
       
    public int addContact(User contact){
        int count = countLinks(User.class).intValue();
		this.link(User.class, contact.getId());
		return count + 1;
    }    
    
    public int removeContact(User contact){
        int count = countLinks(User.class).intValue();
		this.unlink(User.class, contact.getId());
		return count - 1;
    }
		
    public ArrayList<User> getAllContacts(MutableLong page, MutableLong itemcount){
        return this.getLinkedObjects(User.class, page, itemcount);
    }

    public ArrayList<Classunit> getAllClassUnits(MutableLong page, MutableLong itemcount){
        return this.getLinkedObjects(Classunit.class, page, itemcount);
    }

    public ArrayList<School> getAllSchools(MutableLong page, MutableLong itemcount, int howMany){
        return this.getLinkedObjects(School.class, page, itemcount);
    }

    public ArrayList<School> getAllSchools(MutableLong page, MutableLong itemcount){
        return getAllSchools(page, itemcount, Utils.MAX_ITEMS_PER_PAGE);
    }
	
    public ArrayList<Group> getAllGroups(MutableLong page, MutableLong itemcount){
        return this.getLinkedObjects(Group.class, page, itemcount);
    }
   
    public ArrayList<Message> getAllMessages(MutableLong page, MutableLong itemcount){
        return this.getLinkedObjects(Message.class, page, itemcount);
    }
        
    public void deleteAllMessages(){
        deleteChildren(Message.class);
    }
    
    public boolean isFriendWith(User user){
		if(user == null) return false;
		else if(user.getId().equals(getId())) return true;
		
        return this.isLinked(User.class, user.getId());
    }

	public Map<String, String> getSimpleSchoolsMap(){
		Map<String, String> m = new HashMap<String, String>();
		int max = Constants.MAX_SCHOOLS_PER_USER;
        for(School school : this.getAllSchools(null, null, max)){
			m.put(school.getId(), school.getName());
        }		
		return m;
	}

	public Map<String, School> getSchoolsMap(){
		Map<String, School> m = new HashMap<String, School>();

		int max = Constants.MAX_SCHOOLS_PER_USER;
        for(School school : this.getAllSchools(null, null, max)){
			m.put(school.getId(), school);
        }
		return m;
	}
	
	public Map<String, String> getSimpleGroupsMap(){
		Map<String, String> m = new HashMap<String, String>();
        for(Group group : this.getAllGroups(null, null)){
			m.put(group.getId(), group.getName());
        }		
		return m;
	}
	
	public Map<String, Group> getGroupsMap(){
		Map<String, Group> m = new HashMap<String, Group>();

        for(Group group : this.getAllGroups(null, null)){
			m.put(group.getId(), group);
        }
		return m;
	}
    
    public static User getUser(String identifier){
		return (User) DAO.getInstance().readUserForIdentifier(identifier);
    }

	public boolean isFacebookUser(){
		return identifier != null && NumberUtils.isDigits(identifier);
	}

	public static boolean exists(String identifier){
		return Search.getCount(classname(User.class), "identifier", identifier) > 0;
	}
	
	public List<String> getIdentifiers(){
		return Arrays.asList(identifier, identifiertwo);
	}

	public void attachIdentifier(String identifier){
		setIdentifiertwo(identifier);
		DAO.getInstance().createIdentifier(getId(), identifier);
	}

	public void detachIdentifier(String identifier){
		DAO.getInstance().deleteIdentifier(identifier);
	}

	public boolean isModerator(){
		if(isAdmin()) return true;
		return StringUtils.equalsIgnoreCase(getGroups(), UserGroup.MODS.toString());
	}

	public boolean isAdmin(){
		return StringUtils.equalsIgnoreCase(getGroups(), UserGroup.ADMINS.toString());
	}

	public int countNewMessages(){
		if(newmessages == null)
			newmessages = Message.countNewMessages(getId());
		return newmessages;
	}
	
	public int countNewReports(){
		if(!isModerator()) return 0;
		if(newreports == null)
			newreports = Search.getBeanCount(PObject.classname(Report.class)).intValue();
			
		return newreports;
	}
	
	public Long getAuthstamp(){
		if(authstamp == null)
			authstamp = NumberUtils.toLong(DAO.getInstance().loadAuthMap(getIdentifier()).get(DAO.CN_AUTHSTAMP));
		return authstamp;
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final User other = (User) obj;
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

	public int compareTo(User u) {
		int deptComp = -1;
		if(getName() != null)
			deptComp = getName().compareTo(u.getName());

		return deptComp;
	}
}

