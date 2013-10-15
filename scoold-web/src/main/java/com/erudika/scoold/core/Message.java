/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.core;

import com.erudika.para.core.PObject;
import com.erudika.para.annotations.Stored;
import com.erudika.para.persistence.DAO;
import com.erudika.scoold.util.Constants;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class Message extends PObject{
	private static final long serialVersionUID = 1L;
  
    @Stored private Boolean isread;
    @Stored private String body;
	
	private Set<String> toids;
	private transient User author;

    public Message() {
		this(null, null, null, null);        
    }

	public Message(Set<String> toids, String creatorid, Boolean isread, String body) {
		setCreatorid(creatorid);
		this.toids = toids;
		this.isread = isread;
		this.body = body;
	}

	public Message(String id, String toid) {
		this();
		setId(id);
		setParentid(toid);
	}

	public String getToid() {
		return getParentid();
	}

	public void setToid(String toid) {
		setParentid(toid);
	}

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Boolean getIsread() {
        return isread;
    }

    public void setIsread(Boolean read) {
        this.isread = read;
    }

	public User getAuthor(){
		if(getCreatorid() == null) return null;
		if(author == null) author = getDao().read(getCreatorid());
		return author;
	}

	public boolean send(){
		if(toids == null || toids.isEmpty()) return false;
		if(StringUtils.isBlank(body) || getCreatorid() == null || toids.isEmpty()) return false;
		else if(toids.size() > Constants.MAX_MULTIPLE_RECIPIENTS) return false;

		//send to many recepients or just one
		for (String toid : toids) {
			this.setToid(toid);
			create();
		}
		
		return true;
	}

	public String create() {
		DAO dao = getDao();
		boolean existsUser = dao.existsColumn(getParentid(), DAO.OBJECTS, DAO.CN_ID);
		int count = getSearch().getCount(getClassname(), DAO.CN_PARENTID, getParentid()).intValue();
		if (!existsUser || count > Constants.MAX_MESSAGES_PER_USER) {
			return null;
		}

		String id = super.create();
		Integer newmessages = new User(getParentid()).countNewMessages() + 1;
		dao.putColumn(getParentid(), DAO.OBJECTS, "newmessages", newmessages.toString());
		return id;
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
		if (this.getParentid() != other.getParentid() && (this.getParentid() == null || 
				!this.getParentid().equals(other.getParentid()))) {
			return false;
		}
		if (getCreatorid() == null || !getCreatorid().equals(other.getCreatorid())) {
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
		hash = 79 * hash + (this.getParentid() != null ? this.getParentid().hashCode() : 0);
		hash = 79 * hash + (getCreatorid() != null ? getCreatorid().hashCode() : 0);
		hash = 79 * hash + (this.body != null ? this.body.hashCode() : 0);
		hash = 79 * hash + (getTimestamp() != null ? getTimestamp().hashCode() : 0);
		return hash;
	}
    
}
