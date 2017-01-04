/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.scoold.core.Classunit;
import com.erudika.scoold.core.ContactDetail.ContactDetailType;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.ScooldUser.Badge;
import com.erudika.scoold.core.ScooldUser;
import com.erudika.scoold.core.School;
import com.erudika.scoold.utils.AppConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import static com.erudika.para.core.User.Groups.*;
import com.erudika.para.core.utils.ParaObjectUtils;
import java.util.List;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Profile extends Base{

    public String title;
    public boolean isMyProfile;
	public boolean canEdit;
	public ScooldUser showUser;
	public List<ContactDetailType> contactDetailTypes;
	public List<ScooldUser> contactlist;
	public List<School> schoollist;
	public List<Classunit> classlist;
	public List<? extends Post> postlist;
	public boolean ADMIN_INIT = true;
	private boolean isMyid;

    public Profile() {
        title = lang.get("profile.title");
		contactDetailTypes = getContactDetailTypeArray();
		canEdit = false;
		isMyid = false;

		if (!authenticated && !param("id")) {
			setRedirect(HOMEPAGE);
			return;
		}

		String showuid = param("id") ? getParamValue("id") : authUser.getId();
		isMyid = authenticated && authUser.getId().equals(showuid);

		if (isMyid) {
			//requested userid !exists or = my userid => show my profile
			showUser = authUser;
			isMyProfile = true;
		} else if (showuid != null) {
			showUser = pc.read(showuid);
			isMyProfile = false;
		}

//		if (showUser == null || !ParaObjectUtils.typesMatch(showUser)) {
//			setRedirect(profilelink);
//			return;
//		}

		if (authenticated && (isMyid || inRole("admin"))) {
			canEdit = true;
		}

		title = lang.get("profile.title") + " - " + showUser.getName();

		if ("contacts".equals(showParam)) {
			title += " - " + lang.get("contacts.title");
			contactlist = showUser.getAllContacts(itemcount);
		} else if ("questions".equals(showParam)) {
			title += " - " + lang.get("questions.title");
			postlist = showUser.getAllQuestions(itemcount);
		} else if ("answers".equals(showParam)) {
			title += " - " + lang.get("answers.title");
			postlist = showUser.getAllAnswers(itemcount);
		} else if (!isAjaxRequest()) {
			schoollist = showUser.getAllSchools(itemcount);
			classlist = showUser.getAllClassUnits(itemcount);
		}

		if (param("getsmallpersonbox")) {
			addModel("showAjaxUser", showUser);
		}
    }

    public void onGet() {
		if (!authenticated || showUser == null) return;

		if (!isMyProfile) {
			if (param("makemod") && inRole("admin") && !showUser.isAdmin()) {
				boolean makemod = Boolean.parseBoolean(getParamValue("makemod")) && !showUser.isModerator();
				showUser.setGroups(makemod ? MODS.toString() : USERS.toString());
				showUser.update();
			}
		}
    }

    public void onPost() {
		if (canEdit) {
			if (param("name")) {
				String newFname = getParamValue("name");
				if (newFname != null && newFname.length() >= 4) {
					showUser.setName(newFname);
					showUser.update();
				}
			} else if (param("status")) {
				String status = StringUtils.abbreviate(getParamValue("status"), 160);
				showUser.setStatus(status);
				showUser.update();
			} else if (param("getmyschoolscode")) {
				String id = getParamValue("sid");
				int from = NumberUtils.toInt(getParamValue("fromyear"), 0);
				int to = NumberUtils.toInt(getParamValue("toyear"), 0);
				if (id != null) {
					School school = new School(id);
					school.setFromyear(from);
					school.setToyear(to);
					//update school
					school.linkToUser(authUser.getId());
					if (isAjaxRequest()) {
						schoollist = showUser.getAllSchools(itemcount);
					}
				}
			} else {
				//update profile
				ParaObjectUtils.populate(showUser, req.getParameterMap());
				processContactsRequest(showUser);
				addBadgeOnce(Badge.NICEPROFILE, showUser.isComplete() && isMyid);
				showUser.update();

				if (!isAjaxRequest())
					setRedirect(profilelink); //redirect after post
			}
		}
		if (!isMyProfile) {
			if (param("addcontact")) {
				int count = authUser.addContact(showUser);
				if (!addBadgeOnce(Badge.CONNECTED, count >= AppConfig.CONNECTED_IFHAS) && !isAjaxRequest())
					setRedirect(profilelink+"/"+showUser.getId()+"?code=12&success=true");
			} else if (param("removecontact")) {
				authUser.removeContact(showUser);
				if (!isAjaxRequest())
					setRedirect(profilelink+"/"+showUser.getId()+"?code=13");
			}
		}
	}
}
