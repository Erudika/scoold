/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.core;

import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractMessageDAO;
import com.scoold.db.cassandra.CasDAOFactory;
import java.util.ArrayList;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public class Message implements ScooldObject{
  
    private Long id;
	private String uuid;
	@Stored private String touuid;
    @Stored private Long userid;
    @Stored private String title;
    @Stored private Boolean isread;
    @Stored private String body;
	@Stored private Long timestamp;

	private Set<String> touuids;
	private transient User author;
	private transient static AbstractMessageDAO<Message, Long> mydao;

    public static AbstractMessageDAO<Message, Long> getMessageDao(){
        return (mydao != null) ? mydao : (AbstractMessageDAO<Message, Long>)
				AbstractDAOFactory.getDefaultDAOFactory().getDAO(Message.class);
    }

    public Message() {
        title = "untitled";
        isread = false;
        body = "";
    }

	public Message(Set<String> touuids, Long userid, String title,
			Boolean isread, String body) {
		this.touuids = touuids;
		this.userid = userid;
		this.title = title;
		this.isread = isread;
		this.body = body;
	}

	public Message(Long id) {
		this.id = id;
        this.isread = false;
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
	 * Get the value of touuid
	 *
	 * @return the value of touuid
	 */
	public String getTouuid() {
		return touuid;
	}

	/**
	 * Set the value of touuid
	 *
	 * @param toUserid new value of touuid
	 */
	public void setTouuid(String touuid) {
		this.touuid = touuid;
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
     * Get the value of title
     *
     * @return the value of title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the value of title
     *
     * @param title new value of title
     */
    public void setTitle(String title) {
        this.title = title;
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
		if(touuids == null || touuids.isEmpty()) return false;
		if(StringUtils.isBlank(body) || userid == null || touuids.isEmpty()) return false;
		else if(touuids.size() > CasDAOFactory.MAX_MULTIPLE_RECIPIENTS) return false;

		//send to many recepients or just one
		for (String toUUID : touuids) {
			this.setTouuid(toUUID);
			create();
		}
		
		return true;
	}

	public static void markAllRead(Long uid){
		getMessageDao().markAllAsRead(uid);
	}

	public static void deleteAll(String parentUUID){
		getMessageDao().deleteAllMessagesForUUID(parentUUID);
	}

	public static ArrayList<Message> getMessages(String parentUUID, MutableLong page, MutableLong itemcount){
		return getMessageDao().readAllMessagesForUUID(parentUUID, page, itemcount);
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
		if (this.touuid != other.touuid && (this.touuid == null || !this.touuid.equals(other.touuid))) {
			return false;
		}
		if (this.userid != other.userid && (this.userid == null || !this.userid.equals(other.userid))) {
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
		hash = 79 * hash + (this.touuid != null ? this.touuid.hashCode() : 0);
		hash = 79 * hash + (this.userid != null ? this.userid.hashCode() : 0);
		hash = 79 * hash + (this.title != null ? this.title.hashCode() : 0);
		hash = 79 * hash + (this.body != null ? this.body.hashCode() : 0);
		hash = 79 * hash + (this.timestamp != null ? this.timestamp.hashCode() : 0);
		return hash;
	}
     
    
}
