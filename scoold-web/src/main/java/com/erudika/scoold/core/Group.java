/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.core;

import com.erudika.para.core.Linker;
import com.erudika.para.core.PObject;
import com.erudika.para.annotations.Stored;
import com.erudika.para.utils.Config;
import com.erudika.scoold.utils.AppConfig;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class Group extends PObject{
	private static final long serialVersionUID = 1L;

	@Stored private String description;
	@Stored private String imageurl;
	
	private transient Integer count;
	
	public Group() {
		this("group", "");
	}
	
	public Group(String id){
		this();
		setId(id);
	}

	public Group(String name, String description) {
		setName(name);
		this.description = description;
	}

	public String getImageurl() {
		return imageurl;
	}

	public void setImageurl(String imageurl) {
		this.imageurl = imageurl;
		if(StringUtils.length(imageurl) <= 10 || !StringUtils.startsWith(imageurl, "http")){
			this.imageurl = null;
		}
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ArrayList<Post> getQuestions(String sortBy, MutableLong pagenum, MutableLong itemcount) {
		return getChildren(Post.class, pagenum, itemcount, sortBy, Config.MAX_ITEMS_PER_PAGE);
	}

	public String create() {
		super.create();
		if(getCreatorid() != null) this.linkToUser(getCreatorid());
		return getId();
	}

	public void delete() {
		super.delete();
		unlinkAll();
	}
	
	public boolean isLinkedTo(User u){
		return this.isLinked(User.class, u.getId());
	}
	
    public boolean linkToUser(String userid){
		return this.link(User.class, userid) != null;
    }
	
    public void linkToUsers(ArrayList<String> userids){
		ArrayList<Linker> list = new ArrayList<Linker>();
		for (String userid : userids) {
			String groupid = getId();
			User u = new User(userid);
			if(u.countLinks(Group.class) < AppConfig.MAX_GROUPS_PER_USER) {
				list.add(new Linker(User.class, Group.class, userid, groupid));
			}
		}
		getDao().createAll(list);
    }

    public void unlinkFromUser(String userid){
        this.unlink(User.class, userid);
    }
		
	public int getCount(){		
		if(count == null){
			return this.countLinks(User.class).intValue();
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
		if (getId() == null || !getId().equals(other.getId())) {
			return false;
		}
		if ((getName() == null) ? (other.getName() != null) : !getName().equals(other.getName())) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int hash = 5;
		hash = 89 * hash + (getId() != null ? getId().hashCode() : 0);
		hash = 89 * hash + (getName() != null ? getName().hashCode() : 0);
		return hash;
	}
	
}
