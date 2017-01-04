package com.erudika.scoold.core;

import com.erudika.para.core.Linker;
import com.erudika.para.core.Sysprop;
import com.erudika.para.annotations.Stored;
import com.erudika.para.core.User;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.erudika.scoold.utils.AppConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class School extends Sysprop {
	private static final long serialVersionUID = 1L;

    @Stored private String location;
    @Stored private String subType;
    @Stored private Integer fromyear;
    @Stored private Integer toyear;
	@Stored private String about;
	@Stored private String contacts;
	@Stored private String iconurl;

	public static enum SchoolType{
		UNKNOWN, HIGHSCHOOL, LYCEUM, COLLEGE, THEOLOGY, SEMINARY, ACADEMY, SPECIALIZED,
		PRIVATE, PRIMARY, SECONDARY, UNIVERSITY, ELEMENTARY, GYMNASIUM, MIDDLE,
		ARTS, SPORTS;

		public String toString() {
			return super.toString().toLowerCase();
		}
	};

	private static final AsyncHttpClient httpClient;
	private static int redirectCount = 0;
	static {
		final AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
		builder.setCompressionEnforced(true)
			.setAllowPoolingConnections(true)
			.setRequestTimeout(5000)
			.build();

		httpClient = new AsyncHttpClient(builder.build());
	}

	public School() {
		this("", SchoolType.UNKNOWN.toString(), "");
    }

	public School(String name, String subType, String location) {
		setName(name);
		this.location = location;
		this.about = "";
		this.subType = getSchoolType(subType).toString();
	}

	public School(String id) {
		this();
        setId(id);
    }

	public void setContacts(String contacts) {
		this.contacts = contacts;
	}

	public String getIconurl() {
		return iconurl;
	}

	public void setIconurl(String iconurl) {
		this.iconurl = iconurl;
	}


	public String getContacts() {
		return contacts;
	}

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = getSchoolType(subType).toString();
    }

    public Integer getToyear() {
        return toyear;
    }

    public void setToyear(Integer toyear) {
        this.toyear = toyear;
    }

    public Integer getFromyear() {
        return fromyear;
    }

    public void setFromyear(Integer fromyear) {
        this.fromyear = fromyear;
    }

	public String getAbout() {
		return about;
	}

	public void setAbout(String about) {
		this.about = about;
	}

	public List<ScooldUser> getAllUsers(Pager pager) {
		return AppConfig.client().getLinkedObjects(this, Utils.type(User.class), pager);
	}

    public List<Classunit> getAllClassUnits(Pager pager) {
		return AppConfig.client().getLinkedObjects(this, Utils.type(Classunit.class), pager);
    }

    public boolean linkToUser(String userid) {
		if (userid == null) return false;
		// auto add to my schools
		if (AppConfig.client().countLinks(new User(userid), Utils.type(School.class)) < AppConfig.MAX_SCHOOLS_PER_USER) {
			AppConfig.client().link(this, userid);
			return true;
		}
		return false;
    }

    public void unlinkFromUser(String userid) {
		AppConfig.client().unlink(this, Utils.type(User.class), userid);
    }

	public boolean isLinkedTo(User u) {
		return AppConfig.client().isLinked(this, new User(u.getId()));
	}

	private SchoolType getSchoolType(String type) {
		if (type == null) return SchoolType.UNKNOWN;
		try{
			return SchoolType.valueOf(type.trim().toUpperCase());
		}catch(IllegalArgumentException e) {
            //oh shit!
			return SchoolType.UNKNOWN;
        }
	}

    public static  Map<String, String> getSchoolTypeMap(Map<String, String> lang) {
        SchoolType[] starr = SchoolType.values();
        Map<String, String> st = new HashMap<String, String>();
		if (lang == null) return st;

        for(SchoolType s : starr) {
			if (s != SchoolType.UNKNOWN) {
				String locs = lang.get("school."+s.toString());
				if (locs == null) locs = s.toString();
				st.put(s.toString(), locs);
			}
        }
		//st.remove(0);	// removes "unknown" from the list
        return st;
    }

	public List<ContactDetail> getAllContactDetails() {
		return ContactDetail.toContactsList(contacts);
	}

	public String getWebsite() {
		String ws = ContactDetail.ContactDetailType.WEBSITE.name();
		int wsl = ws.length() + 1;
		if (!StringUtils.isBlank(contacts) && StringUtils.contains(contacts, ws)) {
			String www = contacts.substring(contacts.indexOf(ws) + wsl);
			if (StringUtils.contains(www, ContactDetail.SEPARATOR)) {
				www = www.substring(0, www.indexOf(ContactDetail.SEPARATOR));
			}
			return Utils.isValidURL(www) ? www : null;
		}
		return null;
	}

	public String create() {
		linkToUser(getCreatorid());
		School s = AppConfig.client().create(this);
		if (s != null) {
			setId(s.getId());
			setTimestamp(s.getTimestamp());
			return s.getId();
		}
		return null;
	}

    public void update() {
		AppConfig.client().update(this);
    }

    public void delete() {
		AppConfig.client().delete(this);
		AppConfig.client().deleteChildren(this, Utils.type(Classunit.class));
		AppConfig.client().unlinkAll(this);
    }

	////////////////////////////////////////////////
	//		/\	EXPERIMENTAL FEATURE /\
	////////////////////////////////////////////////

	public boolean mergeWith(String duplicateSchoolid) {
		School primarySchool = this;
		School duplicateSchool = AppConfig.client().read(duplicateSchoolid);

		if (primarySchool == null || duplicateSchool == null) return false;
		else if (!duplicateSchool.getParentid().equals(primarySchool.getParentid())) return false;

		// STEP 1:
		// Move every user to the primary class
		// STEP 2:
		// move all classes to primary school
		ArrayList<Linker> allLinks = new ArrayList<Linker>();
		allLinks.addAll(duplicateSchool.getLinks(Utils.type(User.class)));
		allLinks.addAll(duplicateSchool.getLinks(Utils.type(Classunit.class)));

		for (Linker link : allLinks) {
			try {
				PropertyUtils.setProperty(link, link.getIdFieldNameFor(Utils.type(School.class)), getId());
			} catch (Exception ex) {
				LoggerFactory.getLogger(Classunit.class).error(null, ex);
			}
		}

		AppConfig.client().updateAll(allLinks);

		// STEP 4:
		// delete duplicate
		AppConfig.client().delete(duplicateSchool);

		return true;
	}

    public boolean equals(Object obj) {
        if (this == obj)
                return true;
        if ((obj == null) || (obj.getClass() != this.getClass()) ||
				getName() == null || this.location == null)
                return false;
        School school = (School)obj;
        return (getName().equals(school.getName()) &&
				this.location.equals(school.getLocation()));
    }

    public int hashCode() {
        return (getName() + this.location).hashCode();
    }

	public List<Question> getQuestions(Pager pager) {
		return AppConfig.client().getChildren(this, Utils.type(Question.class), pager);
	}
}

