package com.erudika.scoold.core;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.erudika.para.core.Linker;
import com.erudika.para.core.PObject;
import com.erudika.para.annotations.Stored;
import com.erudika.para.persistence.DAO;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Media.MediaType;
import com.erudika.scoold.util.Constants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;

public class Classunit extends PObject {
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
   
	public School getSchool(){
		if(getParentid() == null) return null;
		if(school == null) school = getDao().read(getParentid());
		return school;
	}
	
	public Post getBlackboard(){
		if(blackboard == null){
			ArrayList<PObject> list = getSearch().findTerm(classname(Blackboard.class), 
					null, null, DAO.CN_PARENTID, getId());
			if(list.isEmpty()){
				blackboard = (Blackboard) list.get(0);				
			}
		}
		return blackboard;
	}

	public boolean isLinkedTo(User u){
		return this.isLinked(User.class, u.getId());
	}

	public boolean linkToUser(String userid){
		if(userid == null) return false;
		// auto add to my classes
		User u = new User(userid);
		long count1 = u.countLinks(Classunit.class);
		long count2 = u.countLinks(School.class);
		if((count1 < Constants.MAX_CLASSES_PER_USER && count2 < Constants.MAX_SCHOOLS_PER_USER)){
			u.link(Classunit.class, getId());
			u.link(School.class, getParentid());
			return true;
		}
		return false;
    }
	
    public void unlinkFromUser(String userid){
        this.unlink(User.class, userid);
    }

    public String create() {
		super.create();
		// attach a new clean blackboard to class
		Blackboard bb = new Blackboard();
		bb.setBody(" ");
		bb.setCreatorid(getCreatorid());
		bb.setParentid(getId());
		bb.create();
		linkToUser(getCreatorid());
		
        return getId();
    }
	
    public void delete() {
		super.delete();
		getBlackboard().delete();
		deleteChildren(Media.class);
		unlinkAll();
    }
	
	public int getClassSize(){
		if(getId() == null) return 0;
		return this.countLinks(User.class).intValue();
	}
	
	public boolean mergeWith(String duplicateClassid){
		Classunit primaryClass = this;
		Classunit duplicateClass = getDao().read(duplicateClassid);
		
		if(primaryClass == null || duplicateClass == null) return false;
		else if(!duplicateClass.getParentid().equals(primaryClass.getParentid())) return false;

		// STEP 1:
		// Move every user to the primary class
		// STEP 2:
		// move media to primary class
		ArrayList<Linker> allLinks = new ArrayList<Linker>();
		allLinks.addAll(duplicateClass.getAllLinks(User.class));
		allLinks.addAll(duplicateClass.getAllLinks(Media.class));
		
		for (Linker link : allLinks) {
			try {
				PropertyUtils.setProperty(link, link.getIdFieldNameFor(Classunit.class), getId());
			} catch (Exception ex) {
				Logger.getLogger(Classunit.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		
		getDao().updateAll(allLinks);

		// STEP 3:
		// delete duplicate
		duplicateClass.delete();

		return true;
	}
	
	public String sendChat(String chat) {
		if(getId() == null || StringUtils.isBlank(chat)) return "[]";
		String chad = receiveChat();
		try {
			StringBuilder sb = new StringBuilder("[");
			JSONArray arr = new JSONArray(chad);
			int start = (arr.length() >= Utils.MAX_ITEMS_PER_PAGE) ? 1 : 0;
			
			for (int i = start; i < arr.length(); i++) {
				JSONObject object = arr.getJSONObject(i);
				sb.append(object.toString()).append(",");
			}	
			
			sb.append(chat);			
			sb.append("]");
			chad = sb.toString().replaceAll(",]", "]");
		} catch (JSONException ex) {
			Logger.getLogger(Classunit.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		getDao().putColumn(getId(), DAO.OBJECTS, "chat", chad);
		return chad;
	}
	
	public String receiveChat() {
		String chad = getDao().getColumn(getId(), DAO.OBJECTS, "chat");
		if(StringUtils.isBlank(chad)) chad = "[]";
		return chad;
	}

	public boolean equals(Object obj){
        if(this == obj)
                return true;
        if((obj == null) || (obj.getClass() != this.getClass()))
                return false;
        Classunit cunit = (Classunit)obj;
        
		return (getIdentifier().equals(cunit.getIdentifier()) &&
				getParentid().equals(cunit.getParentid()));
    }
    
    public int hashCode() {
        return (identifier+getParentid()).hashCode();
    }

	public ArrayList<Media> getMedia(MediaType type, String label, MutableLong pagenum,
			MutableLong itemcount, int maxItems, boolean reverse) {
		return Media.getAllMedia(getId(), type, pagenum, itemcount, reverse, maxItems, getSearch());
	}

	public void deleteAllMedia(){
		deleteChildren(Media.class);
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
}

