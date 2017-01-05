package com.erudika.scoold.core;

import com.erudika.para.core.Linker;
import com.erudika.para.annotations.Stored;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.User;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.utils.AppConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

public class Classunit extends Sysprop {
	private static final long serialVersionUID = 1L;

	@Stored private String identifier;
	@Stored private Integer gradyear;
    @Stored private Integer yearbegin;
	@Stored private String inactiveusers;
	@Stored private String chat;	// JSON ARRAY [{id: id, msg: msg}, ...]

	private transient Integer count;
	private transient School school;
	private transient Blackboard blackboard;


    public Classunit() {
		this.identifier = "";
    }

    public Classunit(String id) {
        setId(id);
    }

	public String getChat() {
		return chat;
	}

	public void setChat(String chat) {
		this.chat = chat;
	}

	public String getInactiveusers() {
		return inactiveusers;
	}

	public void setInactiveusers(String inactiveusers) {
		this.inactiveusers = inactiveusers;
	}

	public Integer getGradyear() {
		return gradyear;
	}

	public void setGradyear(Integer gradyear) {
		this.gradyear = gradyear;
	}

	public Integer getYearbegin() {
        return yearbegin;
    }

    public void setYearbegin(Integer yearbegin) {
        this.yearbegin = yearbegin;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
		this.identifier = identifier;
    }

	@JsonIgnore
	public School getSchool() {
		if (getParentid() == null) return null;
		if (school == null) school = AppConfig.client().read(getParentid());
		return school;
	}

	@JsonIgnore
	public Post getBlackboard() {
		if (blackboard == null) {
			blackboard = AppConfig.client().read(Blackboard.getBlackboardId(getId()));
		}
		return blackboard;
	}

	public boolean isLinkedTo(User u) {
		return AppConfig.client().isLinked(this, Utils.type(User.class), u.getId());
	}

	public boolean linkToUser(String userid) {
		if (userid == null) return false;
		// auto add to my classes
		User u = new User(userid);
		long count1 = AppConfig.client().countLinks(u, Utils.type(Classunit.class));
		long count2 = AppConfig.client().countLinks(u, Utils.type(School.class));
		if ((count1 < AppConfig.MAX_CLASSES_PER_USER && count2 < AppConfig.MAX_SCHOOLS_PER_USER)) {
			AppConfig.client().link(u, getId());
			AppConfig.client().link(u, getParentid());
			return true;
		}
		return false;
    }

    public void unlinkFromUser(String userid) {
        AppConfig.client().unlink(this, Utils.type(User.class), userid);
    }

    public String create() {
		Classunit cu = AppConfig.client().create(this);
		if (cu != null) {
			linkToUser(getCreatorid());
			setId(cu.getId());
			setTimestamp(cu.getTimestamp());
			// attach a new clean blackboard to class
			Blackboard bb = new Blackboard();
			bb.setBody(" ");
			bb.setId(Blackboard.getBlackboardId(getId()));
			bb.setCreatorid(getCreatorid());
			bb.setParentid(getId());
			bb.create();
			return cu.getId();
		}
        return null;
    }

	public void update() {
		setName(getIdentifier());
		AppConfig.client().update(this);
	}

    public void delete() {
		AppConfig.client().delete(this);
		getBlackboard().delete();
		AppConfig.client().unlinkAll(this);
    }

	public int getClassSize() {
		if (getId() == null) return 0;
		return AppConfig.client().countLinks(this, Utils.type(User.class)).intValue();
	}

	public boolean mergeWith(String duplicateClassid) {
		Classunit primaryClass = this;
		Classunit duplicateClass = AppConfig.client().read(duplicateClassid);

		if (primaryClass == null || duplicateClass == null) return false;
		else if (!duplicateClass.getParentid().equals(primaryClass.getParentid())) return false;

		// STEP 1:
		// Move every user to the primary class
		ArrayList<Linker> allLinks = new ArrayList<Linker>();
		allLinks.addAll(duplicateClass.getLinks(Utils.type(User.class)));

		for (Linker link : allLinks) {
			try {
				PropertyUtils.setProperty(link, link.getIdFieldNameFor(Utils.type(Classunit.class)), getId());
			} catch (Exception ex) {
				LoggerFactory.getLogger(Classunit.class).error(null, ex);
			}
		}

		AppConfig.client().updateAll(allLinks);

		// STEP 3:
		// delete duplicate
		AppConfig.client().delete(duplicateClass);

		return true;
	}

	public String sendChat(String chat) {
		if (getId() == null || StringUtils.isBlank(chat)) return "[]";
		String chad = receiveChat();
		try {
//			StringBuilder sb = new StringBuilder("[");
//			JSONArray arr = new JSONArray(chad);
//			int start = (arr.size() >= Config.MAX_ITEMS_PER_PAGE) ? 1 : 0;
//
//			for (int i = start; i < arr.length(); i++) {
//				JSONObject object = arr.getJSONObject(i);
//				sb.append(object.toString()).append(",");
//			}
//
//			sb.append(chat);
//			sb.append("]");
//			chad = sb.toString().replaceAll(",]", "]");

			ArrayList<Object> arr = ParaObjectUtils.getJsonReader(ArrayList.class).readValue(chad);
			arr.add(ParaObjectUtils.getJsonReader(Map.class).readValue(chat));
			chad = ParaObjectUtils.getJsonWriterNoIdent().writeValueAsString(arr);
		} catch (Exception ex) {
			LoggerFactory.getLogger(Classunit.class).error(null, ex);
		}
//		AppConfig.client().putColumn(getId(), "chat", chad);
		setChat(chad);
		update();
		return chad;
	}

	public String receiveChat() {
		String chad = "[]";
		Classunit that = AppConfig.client().read(getId());
		if (that != null && !StringUtils.isBlank(that.getChat())) {
			chad = that.getChat();
		}
		return chad;
	}

	public boolean equals(Object obj) {
        if (this == obj)
                return true;
        if ((obj == null) || (obj.getClass() != this.getClass()))
                return false;
        Classunit cunit = (Classunit)obj;

		return (getIdentifier().equals(cunit.getIdentifier()) &&
				getParentid().equals(cunit.getParentid()));
    }

    public int hashCode() {
        return (identifier+getParentid()).hashCode();
    }

	public Set<String> getInactiveusersList() {
		if (StringUtils.isBlank(inactiveusers)) return new HashSet<String>();
		HashSet<String> list = new HashSet<String>();
		list.addAll(Arrays.asList(inactiveusers.split(",")));
		list.remove("");
		return list;
	}

	public int getCount() {
		if (count == null) {
			count = getClassSize() + getInactiveusersList().size();
		}
		return count;
	}
}

