package com.scoold.core;

import com.scoold.core.Media.MediaType;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractUserDAO;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.mutable.MutableLong;


public class User implements ScooldObject, Comparable<User>, 
		CanHasMedia, Searchable<User>, Serializable{

    private Long id;
	private String uuid;
	@Indexed
    @Stored private String fullname;
    @Stored private String email;
    @Stored private Boolean active;
    @Stored private Long lastseen;
    @Stored private Long timestamp;
	@Indexed
    @Stored private String type;
	@Stored private Long locktill;
	@Stored private String groups;
    @Indexed
	@Stored private String location;
	@Stored private Long dob;
	@Indexed
    @Stored private String status;
	@Indexed
    @Stored private String ilike;
	@Indexed
    @Stored private String aboutme;
	@Indexed
	@Stored private String badges;
	@Stored private Long upvotes;
	@Stored private Long downvotes;
	@Stored private Long comments;
	@Indexed
	@Stored private Long reputation;
	@Stored private String contacts;
	@Stored private String identifier;
	@Stored private Long photos;
	@Stored private String favtags;

	private Long oldreputation;
	private int newmessages;

	private MutableLong reportcount = new MutableLong(0L);

	public static enum UserGroup{
		ALUMNI, STUDENTS, TEACHERS, ADMINS, MODS;
		
		public String toString(){
			return super.toString().toLowerCase();
		}
	}

	public static final int ANSWER_VOTEUP_REWARD_AUTHOR = 10;	//
	public static final int QUESTION_VOTEUP_REWARD_AUTHOR = 5;	//
	public static final int POST_VOTEUP_REWARD_AUTHOR = 2;		//
	public static final int ANSWER_ACCEPT_REWARD_AUTHOR = 10;	//
	public static final int ANSWER_ACCEPT_REWARD_VOTER = 3;		//
	public static final int POST_VOTEDOWN_PENALTY_AUTHOR = 4;	//
	public static final int POST_VOTEDOWN_PENALTY_VOTER = 2;	//

	public static final int VOTER_IFHAS = 100; // votes			//
	public static final int COMMENTATOR_IFHAS = 100; // coments	//
	public static final int CRITIC_IFHAS = 10; // downvotes		//
	public static final int SUPPORTER_IFHAS = 50; // upvotes	//

	public static final int GOODQUESTION_IFHAS = 20; // votes	//
	public static final int GOODANSWER_IFHAS = 10; // votes		//
	public static final int PHOTOLOVER_IFHAS = 20; // photos	//
	public static final int CONNECTED_IFHAS = 10; // contacts	//

	public static final int ENTHUSIAST_IFHAS = 100; // rep		//
	public static final int FRESHMAN_IFHAS = 300; // rep		//
	public static final int SCHOLAR_IFHAS = 500; // rep			//
	public static final int TEACHER_IFHAS = 1000; // rep		//
	public static final int PROFESSOR_IFHAS = 5000; // rep		//
	public static final int GEEK_IFHAS = 9000; // rep IT"S OVER NINE THOUSAND!


	public static enum Badge{
		REGULAR(10),		//regular visitor		//TODO: IMPLEMENT!

		NICEPROFILE(10),	//100% profile completed
		TESTER(0),			//for testers only
		REPORTER(0),		//for every report
		VOTER(0),			//100+ votes
		COMMENTATOR(0),		//100+ comments
		CRITIC(0),			//10+ downvotes
		SUPPORTER(10),		//50+ upvotes
		EDITOR(0),			//first edit of post
		BACKINTIME(0),		//for each rollback of post
		NOOB(10),			//first question + first accepted answer
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
		POLYGLOT(10);		//author of an approved translation

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

    private transient static AbstractUserDAO<User, Long> mydao;

    public static AbstractUserDAO<User, Long> getUserDao() {
        return (mydao != null) ? mydao : (AbstractUserDAO<User, Long>)
				AbstractDAOFactory.getDefaultDAOFactory().getDAO(User.class);
    }

    public User () {
		this.fullname = "";
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
		this.groups = getUserType(this.type).toGroupString();
	}

	public User(String uuid) {
		this.uuid = uuid;
		this.upvotes = 0L;
		this.downvotes = 0L;
		this.comments = 0L;
		this.reputation = 0L;
		this.photos = 0L;
		this.groups = getUserType(this.type).toGroupString();
	}

	public User (Long id){
		this.fullname = "";
		this.id = id;
		this.upvotes = 0L;
		this.downvotes = 0L;
		this.comments = 0L;
		this.reputation = 0L;
		this.photos = 0L;
		this.groups = getUserType(this.type).toGroupString();
	}

    public User (String email, Boolean active, UserType type,
			String fullname) {
        
		this.fullname = fullname;
        this.email = email;
        this.active = active;
		this.type =  type.toString();
		this.groups = getUserType(this.type).toGroupString();
		this.upvotes = 0L;
		this.downvotes = 0L;
		this.comments = 0L;
		this.reputation = 0L;
		this.photos = 0L;
    }

	public String getFavtags() {
		return favtags;
	}

	public void setFavtags(String favtags) {
		this.favtags = favtags;
	}

	public int getNewmessages() {
		return newmessages;
	}

	public void setNewmessages(int newmessages) {
		this.newmessages = newmessages;
	}

	public Long getPhotos() {
		return photos;
	}

	public void setPhotos(Long photos) {
		this.photos = photos;
	}

	public Long getOldreputation() {
		return oldreputation;
	}

	public void setOldreputation(Long oldreputation) {
		this.oldreputation = oldreputation;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public Long getLocktill() {
		return locktill;
	}

	public void setLocktill(Long locktill) {
		this.locktill = locktill;
	}

    public Boolean getActive () {
        return active;
    }

    public void setActive (Boolean val) {
        this.active = val;
    }

    public String getEmail () {
        return email;
    }

    public void setEmail (String val) {
        this.email = val;
    }

    public Long getLastseen () {
        return lastseen;
    }

    public void setLastseen (Long val) {
        this.lastseen = val;
    }
        
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getType() {
        return type;
    }
    
	public void setType(String type){
		this.type = getUserType(type).toString();
	}

	public String getGroups() {
		return groups;
	}

	public void setGroups(String groups) {
		this.groups = groups.toLowerCase();
	}

    public String getFullname(){
        return fullname;
    }

    public final void setFullname(String fullname){
		this.fullname = fullname;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id){
		this.id = id;
	}

	/**
	 * Get the value of contacts
	 *
	 * @return the value of contacts
	 */
	public String getContacts() {
		return contacts;
	}

	/**
	 * Set the value of contacts
	 *
	 * @param contacts new value of contacts
	 */
	public void setContacts(String contacts) {
		this.contacts = contacts;
	}

	/**
	 * Get the value of reputation
	 *
	 * @return the value of reputation
	 */
	public Long getReputation() {
		return reputation;
	}

	/**
	 * Set the value of reputation
	 *
	 * @param reputation new value of reputation
	 */
	public void setReputation(Long reputation) {
		this.reputation = reputation;
	}

	/**
	 * Get the value of comments
	 *
	 * @return the value of comments
	 */
	public Long getComments() {
		return comments;
	}

	/**
	 * Set the value of comments
	 *
	 * @param comments new value of comments
	 */
	public void setComments(Long comments) {
		this.comments = comments;
	}

	/**
	 * Get the value of downvotes
	 *
	 * @return the value of downvotes
	 */
	public Long getDownvotes() {
		return downvotes;
	}

	/**
	 * Set the value of downvotes
	 *
	 * @param downvotes new value of downvotes
	 */
	public void setDownvotes(Long downvotes) {
		this.downvotes = downvotes;
	}

	/**
	 * Get the value of upvotes
	 *
	 * @return the value of upvotes
	 */
	public Long getUpvotes() {
		return upvotes;
	}

	/**
	 * Set the value of upvotes
	 *
	 * @param upvotes new value of upvotes
	 */
	public void setUpvotes(Long upvotes) {
		this.upvotes = upvotes;
	}

	/**
	 * Get the value of badges
	 *
	 * @return the value of badges
	 */
	public String getBadges() {
		return badges;
	}

	/**
	 * Set the value of badges
	 *
	 * @param badges new value of badges
	 */
	public void setBadges(String badges) {
		this.badges = badges;
	}

    /**
     * Get the value of dob
     *
     * @return the value of dob
     */
    public Long getDob() {
        return dob;
    }

    /**
     * Set the value of dob
     *
     * @param dob new value of dob
     */
    public void setDob(Long dob) {
        this.dob = dob;
    }

    /**
     * Get the value of country
     *
     * @return the value of country
     */
    public String getLocation() {
        return location;
    }

    /**
     * Set the value of country
     *
     * @param location
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Get the value of status
     *
     * @return the value of status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Set the value of status
     *
     * @param status new value of status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Get the value of ilike
     *
     * @return the value of ilike
     */
    public String getIlike() {
        return ilike;
    }

    /**
     * Set the value of ilike
     *
     * @param ilike new value of ilike
     */
    public void setIlike(String ilike) {
        this.ilike = ilike;
    }

    /**
     * Get the value of aboutme
     *
     * @return the value of aboutme
     */
    public String getAboutme() {
        return this.aboutme;
    }

    /**
     * Set the value of aboutme
     *
     * @param aboutme new value of aboutme
     */
    public void setAboutme(String aboutme) {
        this.aboutme = aboutme;
    }

	public ArrayList<Post> getAllQuestions(MutableLong pagenum, MutableLong itemcount){
		if(id == null) return new ArrayList<Post> ();
		return getUserDao().readAllPostsForUser(id,
				Post.PostType.QUESTION, pagenum, itemcount);
	}

	public ArrayList<Post> getAllAnswers(MutableLong pagenum, MutableLong itemcount){
		if(id == null) return new ArrayList<Post> ();
		return getUserDao().readAllPostsForUser(id,
				Post.PostType.ANSWER, pagenum, itemcount);
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

		return upvotes+downvotes;
	}

	public void addRep(int rep){
		oldreputation = reputation;
		if(reputation == null ) reputation = 0L;
		reputation += rep;
	}

	public void removeRep(int rep){
		oldreputation = reputation;
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
		return StringUtils.contains(badges, ",".concat(b.toString()).concat(","));
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

    public Long create() {
		this.id = getUserDao().create(this);
        return this.id;
    }

    public void update(){
        getUserDao().update(this);
    }

    public void delete(){
        getUserDao().delete(this);
    }

	public ArrayList<Media> getMedia(MediaType type, String label, MutableLong pagenum,
			MutableLong itemcount, int maxItems, boolean reverse) {
		return Media.getMediaDao().readAllMediaForUUID(this.uuid, type, label,
				pagenum, itemcount, maxItems, reverse);
	}

	public void deleteAllMedia(){
		Media.getMediaDao().deleteAllMediaForUUID(uuid);
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
		String[] s = fullname.split("\\s");
        return s[s.length - 1];
    }

    public String getFirstname() {
        return fullname.split("\\s")[0];
    }
       
    public int addContact(User contact){
        return getUserDao().createContactForUser(this.id, contact);
    }    
    
    public int removeContact(User contact){
        return getUserDao().deleteContactForUser(this.id, contact);
    }
	
    public ArrayList<User> getAllContacts(MutableLong page, MutableLong itemcount){
        return getUserDao().readAllContactsForUser(this.id,page, itemcount);
    }

    public ArrayList<Classunit> getAllClassUnits(MutableLong page, MutableLong itemcount){
        return getUserDao().readAllClassUnitsForUser(this.id, page, itemcount);
    }

    public ArrayList<School> getAllSchools(MutableLong page, MutableLong itemcount, int howMany){
        return getUserDao().readAllSchoolsForUser(this.id, page, itemcount, howMany);
    }

    public ArrayList<School> getAllSchools(MutableLong page, MutableLong itemcount){
        return getAllSchools(page, itemcount, AbstractDAOFactory.MAX_ITEMS_PER_PAGE);
    }
   
    public ArrayList<Message> getAllMessages(MutableLong page, MutableLong itemcount){
        return Message.getMessageDao().readAllMessagesForUUID(uuid, page, itemcount);
    }
        
    public void deleteAllMessages(){
        Message.getMessageDao().deleteAllMessagesForUUID(uuid);
    }
    
    public boolean isFriendWith(User user){
		if(user == null) return false;
		else if(user.getId().equals(this.id)) return true;
		
        return getUserDao().isFriendWith(this.id, user);
    }

	public Map<Long, String> getSimpleSchoolsMap(){
		Map<Long, String> m = new HashMap<Long, String>();
		int max = AbstractDAOFactory.MAX_SCHOOLS_PER_USER;
        for(School school : this.getAllSchools(null, null, max)){
			m.put(school.getId(), school.getName());
        }		
		return m;
	}

	public Map<Long, School> getSchoolsMap(){
		Map<Long, School> m = new HashMap<Long, School>();

		int max = AbstractDAOFactory.MAX_SCHOOLS_PER_USER;
        for(School school : this.getAllSchools(null, null, max)){
			m.put(school.getId(), school);
        }
		return m;
	}
    
    public static User getUser(Long uid){
		if(uid == null) return null;
		User user = getUserDao().read(uid);
		return user;
    }

    public static User getUser(String identifier){
		if(StringUtils.isBlank(identifier)) return null;
		if(identifier.startsWith("http") || NumberUtils.isDigits(identifier)){
			//identifier is an openid url
			return getUserDao().readUserForIdentifier(identifier);
		}else if(identifier.contains("@")){
			//identifier is an email
			return getUserDao().readUserByEmail(identifier);
		}else{
			return null;
		}
    }

	public boolean isFacebookUser(){
		return identifier != null && !identifier.startsWith("http");
	}

	public static boolean exists(Long uid){
		return getUserDao().userExists(uid);
	}

	public static boolean exists(String identifier){
		return getUserDao().userExists(identifier);
	}

	public ArrayList<String> getIdentifiers(){
		return getUserDao().readAllIdentifiersForUser(id);
	}

	public void attachIdentifier(String identifier){
		getUserDao().attachIdentifierToUser(identifier, id);
	}

	public void detachIdentifier(String identifier){
		getUserDao().detachIdentifierFromUser(identifier, id);
	}

	public boolean isModerator(){
		if(isAdmin()) return true;
		return StringUtils.equalsIgnoreCase(groups, UserGroup.MODS.toString());
	}

	public boolean isAdmin(){
		return StringUtils.equalsIgnoreCase(groups, UserGroup.ADMINS.toString());
	}

	public boolean hasNewMessages(){
		return Message.getMessageDao().countNewMessages(id) != 0;
	}

	public int countNewMessages(){
		return Message.getMessageDao().countNewMessages(id);
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final User other = (User) obj;
		if ((this.fullname == null) ? (other.fullname != null) : !this.fullname.equals(other.fullname)) {
			return false;
		}
		if ((this.email == null) ? (other.email != null) : !this.email.equals(other.email)) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int hash = 3;
		hash = 89 * hash + (this.fullname != null ? this.fullname.hashCode() : 0);
		hash = 89 * hash + (this.email != null ? this.email.hashCode() : 0);
		return hash;
	}

	public int compareTo(User u) {
		int deptComp = -1;
		if(fullname != null)
			deptComp = this.fullname.compareTo(u.getFullname());

		return deptComp;
	}

	public void index() {
		Search.indexCreate(this);
	}

	public void reindex() {
		Search.indexUpdate(this);
	}

	public void unindex() {
		Search.indexDelete(this);
	}

	public ArrayList<User> readAllForKeys(ArrayList<String> keys) {
		if(keys == null || keys.isEmpty()) return new ArrayList<User> ();
		return getUserDao().readAllForKeys(keys);
	}

}

