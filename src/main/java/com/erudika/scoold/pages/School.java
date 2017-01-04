/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.scoold.core.Classunit;
import com.erudika.scoold.core.ScooldUser;
import com.erudika.scoold.core.ContactDetail.ContactDetailType;
import com.erudika.scoold.core.ScooldUser.Badge;
import java.util.List;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class School extends Base{

    public String title;
    public com.erudika.scoold.core.School showSchool;
	public boolean canEdit;
	public List<ContactDetailType> contactDetailTypes;
	public List<ScooldUser> peoplelist;
	public List<Classunit> classeslist;
	public List<com.erudika.scoold.core.Question> questionslist;
	public boolean isLinkedToMe;
	public boolean isMine;

    public School() {
        title = lang.get("school.title");

		showSchool = null;
		contactDetailTypes = getContactDetailTypeArray();
		String schoolid = getParamValue("schoolid");

        if (schoolid == null) {
			setRedirect(schoolslink);
			return;
		} else {
            showSchool = pc.read(schoolid);

            if (showSchool != null && ParaObjectUtils.typesMatch(showSchool)) {
                title = title + " - " + showSchool.getName();
            } else {
				setRedirect(schoolslink);
				return;
			}
        }

		isLinkedToMe = (authenticated) ? showSchool.isLinkedTo(authUser) : false;
		isMine = (authenticated) ? authUser.getId().equals(showSchool.getCreatorid()) : false;
		isMine = isMine || inRole("admin");

		if (authenticated && (isMine || authUser.hasBadge(Badge.ENTHUSIAST)
				|| inRole("mod"))) {
			canEdit = true;
		} else {
			canEdit = false;
		}

		if ("classes".equals(showParam)) {
			//get all classes
			classeslist = showSchool.getAllClassUnits(itemcount);
			title = title + " - " + lang.get("classes.title");
		} else if ("people".equals(showParam)) {
			peoplelist = showSchool.getAllUsers(itemcount);
			title = title + " - " + lang.get("people.title");
		} else {
			String sortBy = "";
			if ("activity".equals(getParamValue("sortby"))) sortBy = "lastactivity";
			else if ("votes".equals(getParamValue("sortby"))) sortBy = "votes";
			itemcount.setSortby(sortBy);
			questionslist = showSchool.getQuestions(itemcount);
			addModel("includeGMapsScripts", true);
		}
    }

    public void onGet() {
		if (!authenticated || showSchool == null) return;

		if (param("join") && !showSchool.isLinkedTo(authUser)) {
			//add school to user
			boolean linked = showSchool.linkToUser(authUser.getId());
			isLinkedToMe = true;
			if (!isAjaxRequest() && !linked) {
				setRedirect(schoollink+"/"+showSchool.getId()+"?code=9&error=true");
			} else if (linked) {
				if (!addBadgeOnce(Badge.BACKTOSCHOOL, true) && !isAjaxRequest())
					setRedirect(schoollink+"/"+showSchool.getId()+"?code=11&success=true");
			}
		} else if (param("leave") && showSchool.isLinkedTo(authUser)) {
			//delete school from user
			showSchool.unlinkFromUser(authUser.getId());
			isLinkedToMe = false;
			if (!isAjaxRequest()) setRedirect(schoollink+"/"+showSchool.getId()+"?code=10");
		}
	}

    public void onPost() {
		if (canEdit) {
			if (param("name")) {
				String newName = getParamValue("name");
				if (newName != null && newName.length() >= 4) {
					showSchool.setName(newName);
					showSchool.update();
				}
			} else {
				String snameOrig = showSchool.getName();
				//update school
				ParaObjectUtils.populate(showSchool, req.getParameterMap());
				processContactsRequest(showSchool);
				if (showSchool.getName() == null || showSchool.getName().length() < 3) {
					showSchool.setName(snameOrig);
				}
				showSchool.update();
				if (!isAjaxRequest()) setRedirect(schoollink+"/"+showSchool.getId());
			}
		}
    }
}
