package com.scoold.core;

import com.scoold.core.Media.MediaType;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractClassUnitDAO;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableLong;

public class Classunit implements ScooldObject,	CanHasMedia,
		Searchable<Classunit>, Serializable {

    private Long id;
	private String uuid;
    @Indexed
	@Stored private String identifier;
	@Indexed
	@Stored private Integer gradyear;
    @Stored private Long schoolid;
    @Stored private Integer yearbegin;
	@Stored private Long timestamp;
	@Stored private Long userid;
	@Stored private Long blackboardid;
	@Indexed
	@Stored private String inactiveusers;

	private transient Integer count;
	private transient School school;
    private transient static AbstractClassUnitDAO<Classunit, Long> mydao;

    public static AbstractClassUnitDAO<Classunit, Long> getClassUnitDao(){
       return (mydao != null) ? mydao : (AbstractClassUnitDAO<Classunit, Long>)
			   AbstractDAOFactory.getDefaultDAOFactory().getDAO(Classunit.class);
    }

    public Classunit() {
		this.identifier = "";
    }
    public Classunit(Long id) {
        this.id = id;
    }

	/**
	 * Get the value of incativeusers
	 *
	 * @return the value of incativeusers
	 */
	public String getInactiveusers() {
		return inactiveusers;
	}

	/**
	 * Set the value of incativeusers
	 *
	 * @param inactiveusers new value of incativeusers
	 */
	public void setInactiveusers(String inactiveusers) {
		this.inactiveusers = inactiveusers;
	}

	/**
	 * Get the value of blackboardid
	 *
	 * @return the value of blackboardid
	 */
	public Long getBlackboardid() {
		return blackboardid;
	}

	/**
	 * Set the value of blackboardid
	 *
	 * @param blackboardid new value of blackboardid
	 */
	public void setBlackboardid(Long blackboardid) {
		this.blackboardid = blackboardid;
	}

	/**
	 * Get the value of userid
	 *
	 * @return the value of userid
	 */
	public Long getUserid() {
		return userid;
	}

	/**
	 * Set the value of userid
	 *
	 * @param userid new value of userid
	 */
	public void setUserid(Long userid) {
		this.userid = userid;
	}

	/**
	 * Get the value of timestamp
	 *
	 * @return the value of timestamp
	 */
	public Long getTimestamp() {
		return timestamp;
	}

	/**
	 * Set the value of timestamp
	 *
	 * @param timestamp new value of timestamp
	 */
	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Get the value of uuid
	 *
	 * @return the value of uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the value of uuid
	 *
	 * @param uuid new value of uuid
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
    
	/**
	 * Get the value of gradyear
	 *
	 * @return the value of gradyear
	 */
	public Integer getGradyear() {
		return gradyear;
	}

	/**
	 * Set the value of gradyear
	 *
	 * @param gradyear new value of gradyear
	 */
	public void setGradyear(Integer gradyear) {
		this.gradyear = gradyear;
	}

	/**
	 * Get the value of id
	 *
	 * @return the value of id
	 */
	public Long getId() {
		return id;
	}

	public void setId(Long id){
		this.id = id;
	}

    /**
     * Get the value of yearbegin
     *
     * @return the value of yearbegin
     */
    public Integer getYearbegin() {
        return yearbegin;
    }

    /**
     * Set the value of yearbegin
     *
     * @param yearbegin new value of yearbegin
     */
    public void setYearbegin(Integer yearbegin) {
        this.yearbegin = yearbegin;
    }

    /**
     * Get the value of schoolid
     *
     * @return the value of schoolid
     */
    public Long getSchoolid() {
        return schoolid;
    }

    /**
     * Set the value of schoolid
     *
     * @param schoolid new value of schoolid
     */
    public void setSchoolid(Long schoolid) {
        this.schoolid = schoolid;
    }

    /**
     * Get the value of identifier
     *
     * @return the value of identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Set the value of identifier
     *
     * @param identifier new value of identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
   
    public ArrayList<User> getAllUsers(MutableLong page, MutableLong itemcount){
        return getClassUnitDao().readAllUsersForClassUnit(id, page, itemcount);
    }
        
	public School getSchool(){
		if(schoolid == null) return null;
		if(school == null) school = School.getSchoolDao().read(schoolid);
		return school;
	}

	public boolean isLinkedTo(User u){
		return getClassUnitDao().isLinkedToUser(this.id, u.getId());
	}

	public boolean linkToUser(Long userid){
        return getClassUnitDao().createUserClassLink(userid, this);
    }
	
    public void unlinkFromUser(Long userid){
        getClassUnitDao().deleteUserClassLink(userid, this.id);
    }

    public Long create() {
        this.id = getClassUnitDao().create(this);
        return this.id;
    }
	
    public void update() {
        getClassUnitDao().update(this);
    }

    public void delete() {
        getClassUnitDao().delete(this);
    }

	public int getClassSize(){
		if(id == null) return 0;
		return getClassUnitDao().countUsersForClassUnit(id);
	}

	public Post getBlackboard(){
		if(blackboardid == null) return null;
		return Post.getPostDao().read(this.blackboardid);
	}
	
	public boolean equals(Object obj){
        if(this == obj)
                return true;
        if((obj == null) || (obj.getClass() != this.getClass()))
                return false;
        Classunit cunit = (Classunit)obj;
        
		return (this.identifier.equals(cunit.getIdentifier()) &&
				this.schoolid.equals(cunit.getSchoolid()));
    }
    
    public int hashCode() {
        return (identifier+schoolid).hashCode();
    }

	public ArrayList<Media> getMedia(MediaType type, String label, MutableLong pagenum,
			MutableLong itemcount, int maxItems, boolean reverse) {
		return Media.getMediaDao().readAllMediaForUUID(this.uuid, type, label,
				pagenum, itemcount, maxItems, reverse);
	}

	public void deleteAllMedia(){
		Media.getMediaDao().deleteAllMediaForUUID(uuid);
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

	public Set<String> getInactiveusersList(){
		if(StringUtils.isBlank(inactiveusers)) return new HashSet<String> ();
		HashSet<String> list = new HashSet<String> ();
		list.addAll(Arrays.asList(inactiveusers.split(",")));
		list.remove("");
		return list;
	}
	
	public int getCount(){		
		if(count == null){
			count = getClassSize() + getInactiveusersList().size();			
		}
		return count;		
	}

	public ArrayList<Classunit> readAllForKeys(ArrayList<String> keys) {
		return getClassUnitDao().readAllForKeys(keys);
	}
	
}

