/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.core;

import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractMessageDAO;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public class Message implements ScooldObject, Serializable{
	private static final long serialVersionUID = 1L;
  
    private Long id;
	@Stored private Long toid;
    @Stored private Long userid;
    @Stored private Boolean isread;
    @Stored private String body;
	@Stored private Long timestamp;
	@Stored public static final String classtype = Media.class.getSimpleName().toLowerCase();
	
	private Set<String> toids;
	private transient User author;
	private transient static AbstractMessageDAO<Message, Long> mydao;

    public static AbstractMessageDAO<Message, Long> getMessageDao(){
        return (mydao != null) ? mydao : (AbstractMessageDAO<Message, Long>)
				AbstractDAOFactory.getDefaultDAOFactory().getDAO(Message.class);
    }

    public Message() {
		this(null, null, null, null);        
    }

	public Message(Set<String> toids, Long userid, Boolean isread, String body) {
		this.toids = toids;
		this.userid = userid;
		this.isread = isread;
		this.body = body;
        this.isread = false;
	}

	public Message(Long id, Long toid) {
		this();
		this.id = id;
		this.toid = toid;        
	}

	public String getClasstype() {
		return classtype;
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
	 * Get the value of toid
	 *
	 * @return the value of toid
	 */
	public Long getToid() {
		return toid;
	}

	/**
	 * Set the value of toid
	 *
	 * @param toUserid new value of toid
	 */
	public void setToid(Long toid) {
		this.toid = toid;
	}

    /**
     * Get the value of body
     *
     * @return the value of body
     */
    public String getBody() {
        return body;
    }

    /**
     * Set the value of body
     *
     * @param body new value of body
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * Get the value of read
     *
     * @return the value of read
     */
    public Boolean getIsread() {
        return isread;
    }

    /**
     * Set the value of read
     *
     * @param read new value of read
     */
    public void setIsread(Boolean read) {
        this.isread = read;
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

	public Long getId() {
		return id;
	}

	public void setId(Long id){
		this.id = id;
	}

	public User getAuthor(){
		if(userid == null) return null;
		if(author == null) author = User.getUser(userid);
		return author;
	}

	public boolean send(){
		if(toids == null || toids.isEmpty()) return false;
		if(StringUtils.isBlank(body) || userid == null || toids.isEmpty()) return false;
		else if(toids.size() > AbstractDAOFactory.MAX_MULTIPLE_RECIPIENTS) return false;

		//send to many recepients or just one
		for (String ts : toids) {
			Long toID = NumberUtils.toLong(ts, 0L);
			if(toID != 0L){
				this.setToid(toID);
				create();
			}
		}
		
		return true;
	}

	public static void markAllRead(Long id){
		getMessageDao().markAllAsReadForID(id);
	}

	public static void deleteAll(Long userid){
		getMessageDao().deleteAllMessagesForID(userid);
	}

	public static ArrayList<Message> getMessages(Long userid, MutableLong page, MutableLong itemcount){
		return getMessageDao().readAllMessagesForID(userid, page, itemcount);
	}

    public Long create() {
        this.id = getMessageDao().create(this);
        return this.id;
    }

    public void update() {
        getMessageDao().update(this);
    }

    public void delete() {
        getMessageDao().delete(this);
    }

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Message other = (Message) obj;
		if (this.toid != other.toid && (this.toid == null || !this.toid.equals(other.toid))) {
			return false;
		}
		if (this.userid == null || !this.userid.equals(other.userid)) {
			return false;
		}
		if ((this.body == null) ? (other.body != null) : !this.body.equals(other.body)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 79 * hash + (this.toid != null ? this.toid.hashCode() : 0);
		hash = 79 * hash + (this.userid != null ? this.userid.hashCode() : 0);
		hash = 79 * hash + (this.body != null ? this.body.hashCode() : 0);
		hash = 79 * hash + (this.timestamp != null ? this.timestamp.hashCode() : 0);
		return hash;
	}
     
    
}
