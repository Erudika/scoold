/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scoold.core;

import com.scoold.core.Post.PostType;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractGroupDAO;
import java.io.Serializable;
import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public class Group implements Askable, ScooldObject, Serializable{

	private Long id;
	@Stored private Long userid;
	@Stored private String name;
	@Stored private String description;
	@Stored private String imageurl;
	@Stored private Long timestamp;
	@Stored public static String classtype = Group.class.getSimpleName().toLowerCase();
	
	private transient Integer count;
	private transient static AbstractGroupDAO<Group, Long> mydao;

    public static AbstractGroupDAO<Group, Long> getGroupDao() {
        return (mydao != null) ? mydao : (AbstractGroupDAO<Group, Long>)
				AbstractDAOFactory.getDefaultDAOFactory().getDAO(Group.class);
    }
	
	public Group() {
		this("group", "");
	}
	
	public Group(Long id){
		this();
		this.id = id;
	}

	public Group(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public String getClasstype() {
		return classtype;
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
	 * Get the value of imageurl
	 *
	 * @return the value of imageurl
	 */
	public String getImageurl() {
		return imageurl;
	}

	/**
	 * Set the value of imageurl
	 *
	 * @param imageurl new value of imageurl
	 */
	public void setImageurl(String imageurl) {
		this.imageurl = imageurl;
		if(StringUtils.length(imageurl) <= 10 || !StringUtils.startsWith(imageurl, "http")){
			this.imageurl = null;
		}
	}

	/**
	 * Get the value of description
	 *
	 * @return the value of description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the value of description
	 *
	 * @param description new value of description
	 */
	public void setDescription(String description) {
		this.description = description;
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
	 * Get the value of id
	 *
	 * @return the value of id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Set the value of id
	 *
	 * @param id new value of id
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Get the value of name
	 *
	 * @return the value of name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the value of name
	 *
	 * @param name new value of name
	 */
	public void setName(String name) {
		this.name = name;
	}

	public ArrayList<Post> getQuestions(String sortBy, MutableLong pagenum, MutableLong itemcount) {
		return Post.getPostDao().readAllPostsForID(PostType.GROUPPOST, this.id,
				sortBy, pagenum, itemcount, AbstractDAOFactory.MAX_ITEMS_PER_PAGE);
	}

	public Long create() {
		this.id = getGroupDao().create(this);
		if(this.userid != null) this.linkToUser(this.userid);
		return this.id;
	}

	public void update() {
		getGroupDao().update(this);
	}

	public void delete() {
		getGroupDao().delete(this);
	}
	
	public boolean isLinkedTo(User u){
		return getGroupDao().isLinkedToUser(this.id, u.getId());
	}
	
    public boolean linkToUser(Long userid){
		return getGroupDao().createUserGroupLink(userid, this);
    }
	
    public void linkToUsers(ArrayList<Long> userids){
		getGroupDao().createUserGroupLinks(userids, this);
    }

    public void unlinkFromUser(Long userid){
        getGroupDao().deleteUserGroupLink(userid, this);
    }
	
	public ArrayList<User> getAllUsers(MutableLong page, MutableLong itemcount){
		return User.getUserDao().readAllUsersForID(this.id, page, itemcount);
	}
	
	public int getCount(){		
		if(count == null){
			count = getGroupDao().countUsersForGroup(id);			
		}
		return count;		
	}
	
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Group other = (Group) obj;
		if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
			return false;
		}
		if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int hash = 5;
		hash = 89 * hash + (this.id != null ? this.id.hashCode() : 0);
		hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
		return hash;
	}
	
}
