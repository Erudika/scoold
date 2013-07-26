/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.core;

import com.erudika.para.core.PObject;
import static com.erudika.para.core.PObject.classname;
import com.erudika.para.utils.DAO;
import com.erudika.para.utils.Search;
import com.erudika.para.utils.Stored;
import com.erudika.scoold.util.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author alexb
 */
public class Message extends PObject{
	private static final long serialVersionUID = 1L;
  
	@Stored private String toid;
    @Stored private Boolean isread;
    @Stored private String body;
	
	private Set<String> toids;
	private transient User author;

    public Message() {
		this(null, null, null, null);        
    }

	public Message(Set<String> toids, String creatorid, Boolean isread, String body) {
		this.toids = toids;
		setCreatorid(creatorid);
		this.isread = isread;
		this.body = body;
        this.isread = false;
	}

	public Message(String id, String toid) {
		this();
		setId(id);
		this.toid = toid;        
	}

	public String getToid() {
		return toid;
	}

	public void setToid(String toid) {
		this.toid = toid;
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
		if(author == null) author = User.getUser(getCreatorid());
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
	
	public static int countNewMessages(String parentid){
		return Search.getCount(classname(Message.class), DAO.CN_PARENTID, parentid, "isread", false).intValue();
	}

	public static void markAllRead(String parentid){
		if(StringUtils.isBlank(parentid)) return;
		ArrayList<PObject> list = new ArrayList<PObject>();
		List<Message> unread = Search.findTwoTerms(classname(Message.class), null, null, 
				DAO.CN_PARENTID, parentid, "isread", false);
		for (Message message : unread) {
			message.setIsread(Boolean.TRUE);
			list.add(message);
		}
		DAO.getInstance().updateAll(list);
	}

	public String create() {
		boolean existsUser = User.exists(toid);
		int count = Search.getCount(getClassname(), DAO.CN_PARENTID, getParentid()).intValue();
		if (!existsUser || count > Constants.MAX_MESSAGES_PER_USER) {
			return null;
		}

		String id = super.create();
		Integer newmessages = countNewMessages(id) + 1;
		DAO.getInstance().putColumn(toid, DAO.OBJECTS, "newmessages", newmessages.toString());
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
		if (this.toid != other.toid && (this.toid == null || !this.toid.equals(other.toid))) {
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
		hash = 79 * hash + (this.toid != null ? this.toid.hashCode() : 0);
		hash = 79 * hash + (getCreatorid() != null ? getCreatorid().hashCode() : 0);
		hash = 79 * hash + (this.body != null ? this.body.hashCode() : 0);
		hash = 79 * hash + (getTimestamp() != null ? getTimestamp().hashCode() : 0);
		return hash;
	}
     
    
}
