/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
 
package com.erudika.scoold.pages;

import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Classunit;
import com.erudika.scoold.core.ContactDetail.ContactDetailType;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.User.Badge;
import com.erudika.scoold.core.User;
import com.erudika.scoold.core.School;
import com.erudika.scoold.utils.AppConfig;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import static com.erudika.para.core.User.Groups.*;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */ 
public class Profile extends Base{
	
    public String title;
    public boolean isMyProfile;
	public boolean canEdit;
	public User showUser;
	public ArrayList<ContactDetailType> contactDetailTypes;
	public ArrayList<User> contactlist;
	public ArrayList<School> schoollist;
	public ArrayList<Classunit> classlist;
	public ArrayList<? extends Post> postlist;
	public boolean ADMIN_INIT = true;
	private boolean isMyid;
	
	public String photoslink;
	public String drawerlink;

    public Profile(){
        title = lang.get("profile.title");
		contactDetailTypes = getContactDetailTypeArray();
		canEdit = false;
		isMyid = false;
		String showuid = getParamValue("id");

		isMyid = authenticated && showuid.equals(authUser.getId());

		if(isMyid){
			//requested userid !exists or = my userid => show my profile
			showUser = authUser;
			isMyProfile = true;
		}else if(showuid != null){
			showUser = dao.read(showuid);
			isMyProfile = false;
		}

		if (showUser == null || !utils.typesMatch(showUser)) {
			setRedirect(profilelink);
			return;
		}

		if(authenticated && (isMyid || inRole("admin"))){
			canEdit = true;
		}

		title = lang.get("profile.title") + " - " + showUser.getName();
		photoslink = profilelink + "/" + showUser.getId() + "/photos";
		drawerlink = profilelink + "/" + showUser.getId() + "/drawer";

		if ("contacts".equals(showParam)){
			title += " - " + lang.get("contacts.title");
			contactlist = showUser.getAllContacts(pagenum, itemcount);
		} else if("questions".equals(showParam)) {
			title += " - " + lang.get("questions.title");
			postlist = showUser.getAllQuestions(pagenum, itemcount);
		} else if("answers".equals(showParam)) {
			title += " - " + lang.get("answers.title");
			postlist = showUser.getAllAnswers(pagenum, itemcount);
		} else if("photos".equals(showParam)) {
			if(!authenticated){
				setRedirect("/p" + photoslink);
				return;
			}
			title += " - " + lang.get("photos.title");
			if(param("label")) title += " - " + getParamValue("label");
		} else if("drawer".equals(showParam)) {
			if(!authenticated){
				setRedirect("/p" + drawerlink);
				return;
			}
			title += " - " + lang.get("drawer.title");
		}else if(!isAjaxRequest()){
			schoollist = showUser.getAllSchools(null, itemcount);
			classlist = showUser.getAllClassUnits(null, itemcount);
		}

		if(param("getsmallpersonbox")){
			addModel("showAjaxUser", showUser);
		}
    }

    public void onGet(){
		if(!authenticated || showUser == null) return;
		
		if ("photos".equals(showParam)) {
			processGalleryRequest(showUser, photoslink, canEdit);
		} else if("drawer".equals(showParam)) {
			proccessDrawerRequest(showUser, drawerlink, canEdit);
		}
		
		if(!isMyProfile){
			if(param("makemod") && inRole("admin") && !showUser.isAdmin()){
				boolean makemod = Boolean.parseBoolean(getParamValue("makemod")) && !showUser.isModerator();
				showUser.setGroups(makemod ? MODS.toString() : USERS.toString());
				showUser.update();
			}
		}
    }

    public void onPost(){
		if(canEdit){
			if("photos".equals(showParam)){
				processImageEmbedRequest(showUser, photoslink, canEdit);
				processGalleryRequest(showUser, photoslink, canEdit);
			}else if("drawer".equals(showParam)) {
				proccessDrawerRequest(showUser, drawerlink, canEdit);
			}
			
			if(param("name")){
				String newFname = getParamValue("name");
				if(newFname != null && newFname.length() >= 4){
					showUser.setName(newFname);
					showUser.update();
				}
			}else if(param("status")){
				String status = StringUtils.abbreviate(getParamValue("status"), 160);
				showUser.setStatus(status);
				showUser.update();
			}else if(param("getmyschoolscode")){
				String id = getParamValue("sid");
				int from = NumberUtils.toInt(getParamValue("fromyear"), 0);
				int to = NumberUtils.toInt(getParamValue("toyear"), 0);
				if(id != null){
					School school = new School(id);
					school.setFromyear(from);
					school.setToyear(to);
					//update school
					school.linkToUser(authUser.getId());
					if(isAjaxRequest()){
						schoollist = showUser.getAllSchools(null, itemcount);
					}
				}
			}else{
				//update profile
				Utils.populate(showUser, req.getParameterMap());
				processContactsRequest(showUser);
				addBadgeOnce(Badge.NICEPROFILE, showUser.isComplete() && isMyid);
				showUser.update();

				if(!isAjaxRequest())
					setRedirect(profilelink); //redirect after post
			}
		}
		if(!isMyProfile){
			if(param("addcontact")){
				int count = authUser.addContact(showUser);
				if(!addBadgeOnce(Badge.CONNECTED, count >= AppConfig.CONNECTED_IFHAS) && !isAjaxRequest())
					setRedirect(profilelink+"/"+showUser.getId()+"?code=12&success=true");
			}else if(param("removecontact")){
				authUser.removeContact(showUser);
				if(!isAjaxRequest())
					setRedirect(profilelink+"/"+showUser.getId()+"?code=13");
			}
		}
	}
}
