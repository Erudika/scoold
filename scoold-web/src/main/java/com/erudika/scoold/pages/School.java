/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Classunit;
import com.erudika.scoold.core.User;
import com.erudika.scoold.core.ContactDetail.ContactDetailType;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.User.Badge;
import java.util.ArrayList;

/**
 *
 * @author alexb
 */
public class School extends BasePage{
    
    public String title;
    public com.erudika.scoold.core.School showSchool;
	public boolean canEdit;
	public ArrayList<ContactDetailType> contactDetailTypes;
	public ArrayList<User> peoplelist;
	public ArrayList<Classunit> classeslist;
	public ArrayList<Post> questionslist;
	public boolean isLinkedToMe;
	public boolean isMine;
	
	public String photoslink;
	public String drawerlink;

    public School() {
        title = lang.get("school.title");
		
		showSchool = null;
		contactDetailTypes = getContactDetailTypeArray();
		String schoolid = getParamValue("schoolid");

        if(schoolid == null){
			setRedirect(schoolslink);
			return;
		}else{
            showSchool = dao.read(schoolid);

            if(showSchool != null && utils.typesMatch(showSchool)){
                title = title + " - " + showSchool.getName();
            }else{
				setRedirect(schoolslink);
				return;
			}
        }
		
		isLinkedToMe = (authenticated) ? showSchool.isLinkedTo(authUser) : false;
		isMine = (authenticated) ? authUser.getId().equals(showSchool.getCreatorid()) : false;
		isMine = isMine || inRole("admin");
		
		photoslink = schoollink + "/" + showSchool.getId() + "/photos";
		drawerlink = schoollink + "/" + showSchool.getId() + "/drawer";

		if(authenticated && (isMine || authUser.hasBadge(Badge.ENTHUSIAST)
				|| inRole("mod"))){
			canEdit = true;
		}else{
			canEdit = false;
		}

		if ("classes".equals(showParam)){
			//get all classes
			classeslist = showSchool.getAllClassUnits(pagenum, itemcount);
			title = title + " - " + lang.get("classes.title");
		}else if ("people".equals(showParam)) {
			peoplelist = showSchool.getAllUsers(pagenum, itemcount);
			title = title + " - " + lang.get("people.title");
		} else if("photos".equals(showParam)){
			if(!authenticated){
				setRedirect("/p" + photoslink);
				return;
			}
			title = title + " - " + lang.get("photos.title");
			if(param("label")) title += " - " + getParamValue("label");
		} else if("drawer".equals(showParam)){
			if(!authenticated){
				setRedirect("/p" + drawerlink);
				return;
			}
			title += " - " + lang.get("drawer.title");
		} else {
			String sortBy = "";
			if("activity".equals(getParamValue("sortby"))) sortBy = "lastactivity";
			else if("votes".equals(getParamValue("sortby"))) sortBy = "votes";
			questionslist = showSchool.getQuestions(sortBy, pagenum, itemcount);
			addModel("includeGMapsScripts", true);
		}
    }

    public void onGet(){
		if(!authenticated || showSchool == null) return;

		if ("photos".equals(showParam)) {
			processGalleryRequest(showSchool, photoslink, canEdit);
		} else if("drawer".equals(showParam)) {
			proccessDrawerRequest(showSchool, drawerlink, canEdit);
		}
		
		if(param("join") && !showSchool.isLinkedTo(authUser)){
			//add school to user
			boolean linked = showSchool.linkToUser(authUser.getId());
			isLinkedToMe = true;
			if(!isAjaxRequest() && !linked){
				setRedirect(schoollink+"/"+showSchool.getId()+"?code=9&error=true");
			}else if(linked){
				if(!addBadgeOnce(Badge.BACKTOSCHOOL, true) && !isAjaxRequest())
					setRedirect(schoollink+"/"+showSchool.getId()+"?code=11&success=true");
			}
		}else if(param("leave") && showSchool.isLinkedTo(authUser)){ 
			//delete school from user
			showSchool.unlinkFromUser(authUser.getId());
			isLinkedToMe = false;
			if(!isAjaxRequest()) setRedirect(schoollink+"/"+showSchool.getId()+"?code=10");
		}
	}

    public void onPost(){
		if(canEdit){
			if(param("name")){
				String newName = getParamValue("name");
				if(newName != null && newName.length() >= 4){
					showSchool.setName(newName);
					showSchool.update();
				}
			}else if("photos".equals(showParam)){
				processImageEmbedRequest(showSchool, photoslink, canEdit);
				processGalleryRequest(showSchool, photoslink, canEdit);
			}else if("drawer".equals(showParam)){
				proccessDrawerRequest(showSchool, drawerlink, canEdit);
			}else{
				String snameOrig = showSchool.getName();
				//update school
				Utils.populate(showSchool, req.getParameterMap());
				processContactsRequest(showSchool);
				if(showSchool.getName() == null || showSchool.getName().length() < 3){
					showSchool.setName(snameOrig);
				}
				showSchool.update();
				if(!isAjaxRequest()) setRedirect(schoollink+"/"+showSchool.getId());
			}
		}
    }
}
