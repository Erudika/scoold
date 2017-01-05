/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.core;

import com.erudika.para.core.Linker;
import com.erudika.para.core.Sysprop;
import com.erudika.para.annotations.Stored;
import com.erudika.para.core.User;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.utils.AppConfig;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Group extends Sysprop{
	private static final long serialVersionUID = 1L;

	@Stored private String description;
	@Stored private String imageurl;

	private transient Integer count;

	public Group() {
		this("group", "");
	}

	public Group(String id) {
		this();
		setId(id);
	}

	public Group(String name, String description) {
		this.description = description;
	}

	public String getImageurl() {
		return imageurl;
	}

	public void setImageurl(String imageurl) {
		this.imageurl = imageurl;
		if (StringUtils.length(imageurl) <= 10 || !StringUtils.startsWith(imageurl, "http")) {
			this.imageurl = null;
		}
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<Post> getQuestions(String sortBy, Pager pager) {
		return AppConfig.client().getChildren(this, Utils.type(Post.class), pager);
	}

	public String create() {
		Group g = AppConfig.client().create(this);
		if (g != null) {
			linkToUser(getCreatorid());
			setId(g.getId());
			setTimestamp(g.getTimestamp());
			return g.getId();
		}
		return null;
	}

	public void update() {
		AppConfig.client().update(this);
	}

	public void delete() {
		AppConfig.client().delete(this);
		unlinkAll();
	}

	public boolean isLinkedTo(User u) {
		return AppConfig.client().isLinked(this, Utils.type(User.class), u.getId());
	}

    public boolean linkToUser(String userid) {
		return AppConfig.client().link(this, userid) != null;
    }

    public void linkToUsers(List<String> userids) {
		ArrayList<Linker> list = new ArrayList<Linker>();
		for (String userid : userids) {
			if (AppConfig.client().countLinks(new User(userid), Utils.type(Group.class)) < AppConfig.MAX_GROUPS_PER_USER) {
				AppConfig.client().link(this, userid);
			}
		}
    }

    public void unlinkFromUser(String userid) {
        AppConfig.client().unlink(this, Utils.type(User.class), userid);
    }

	public int getCount() {
		if (count == null) {
			return AppConfig.client().countLinks(this, Utils.type(User.class)).intValue();
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
