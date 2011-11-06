/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import com.scoold.core.Classunit;
import com.scoold.core.User;
import com.scoold.core.ContactDetail.ContactDetailType;
import com.scoold.core.Post;
import com.scoold.core.Post.PostType;
import com.scoold.core.User.Badge;
import com.scoold.db.AbstractDAOUtils;
import java.util.ArrayList;
import org.apache.click.control.Form;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 *
 * @author alexb
 */
public class School extends BasePage{
    
    public String title;
    public com.scoold.core.School showSchool;
	public boolean canEdit;
	public ArrayList<ContactDetailType> contactDetailTypes;
	public ArrayList<User> peoplelist;
	public ArrayList<Classunit> classeslist;
	public ArrayList<Post> questionslist;
	public Form qForm;
	public String galleryUri;
	public boolean isLinkedToMe;
	public boolean isMine;

    public School() {
        title = lang.get("school.title");
		
		showSchool = null;
		contactDetailTypes = getContactDetailTypeArray();
		Long schoolid = NumberUtils.toLong(getParamValue("schoolid"));
		String uuid = getParamValue("uuid");

        if(schoolid.longValue() == 0L && StringUtils.isBlank(uuid)){
			setRedirect(schoolslink);
			return;
		}else{
            showSchool = (schoolid.longValue() == 0L) ?
				com.scoold.core.School.getSchoolDao().read(uuid) :
				com.scoold.core.School.getSchoolDao().read(schoolid);

            if(showSchool != null){
                title = title + " - " + showSchool.getName();
				galleryUri = schoollink.concat("/").concat(
						showSchool.getId().toString()).concat("/photos");
            }else{
				setRedirect(schoolslink);
				return;
			}
        }

		qForm = getQuestionForm(showSchool.getUuid(), null);

		isLinkedToMe = (authenticated) ? showSchool.isLinkedTo(authUser) : false;

		isMine = (authenticated) ? authUser.getId().
				equals(showSchool.getUserid()) : false;

		isMine = isMine || inRole("admin");

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
			pageMacroCode = "#classespage($classeslist)";
		}else if ("people".equals(showParam)) {
			peoplelist = showSchool.getAllUsers(pagenum, itemcount);
			title = title + " - " + lang.get("people.title");
			pageMacroCode = "#peoplepage($peoplelist)";
		} else if("photos".equals(showParam)){
			if(!authenticated){
				setRedirect(schoollink + "/p/" + showSchool.getId() + "/photos");
				return;
			}
			title = title + " - " + lang.get("photos.title");
			if(param("label")) title += " - " + getParamValue("label");
		} else if("drawer".equals(showParam)){
			if(!authenticated){
				setRedirect(schoollink + "/p/" + showSchool.getId() + "/drawer");
				return;
			}
			title += " - " + lang.get("drawer.title");
		} else {
			String sortBy = "timestamp";
			if("activity".equals(getParamValue("sortby"))) sortBy = "lastactivity";
			else if("votes".equals(getParamValue("sortby"))) sortBy = "votes";

			questionslist = showSchool.getQuestions(sortBy, pagenum, itemcount);
			addModel("includeGMapsScripts", true);
		}
    }

    public void onGet(){
        if(showSchool == null){
			setRedirect(schoolslink);
			return;
		}

		if("photos".equals(showParam)){
			processGalleryRequest(showSchool, galleryUri, canEdit);
		}else if("drawer".equals(showParam)){
			proccessDrawerRequest(showSchool, schoollink+"/"+showSchool.getId()+"/drawer", canEdit);
		}

		if(!authenticated) return;

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
			if(!isAjaxRequest())
				setRedirect(schoollink+"/"+showSchool.getId()+"?code=10");
		}

	}

    public void onPost(){
		if(canEdit){
			if("photos".equals(showParam)){
				processImageEmbedRequest(showSchool, galleryUri, canEdit);
				processGalleryRequest(showSchool, galleryUri, canEdit);
			}else if("drawer".equals(showParam)){
				proccessDrawerRequest(showSchool, schoollink+"/"+showSchool.getId()+"/drawer", canEdit);
			}else{
				String snameOrig = showSchool.getName();
				//update school
				AbstractDAOUtils.populate(showSchool, req.getParameterMap());
				if(showSchool.getName() == null || showSchool.getName().length() < 3){
					showSchool.setName(snameOrig);
				}
				showSchool.update();
			}
		}
    }

	public boolean onAskClick(){
		if(isValidQuestionForm(qForm)){
			createAndGoToPost(PostType.QUESTION);
		}
		return false;
	}
}
