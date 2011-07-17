/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
 
package com.scoold.pages;

import com.scoold.core.Classunit;
import com.scoold.core.ContactDetail.ContactDetailType;
import com.scoold.core.Post;
import com.scoold.core.User.Badge;
import com.scoold.core.User;
import com.scoold.core.School;
import com.scoold.core.User.UserGroup;
import com.scoold.core.User.UserType;
import com.scoold.db.AbstractDAOUtils;
import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.geonames.ToponymSearchResult;
/**
 *
 * @author alexb
 */ 
public class Profile extends BasePage{
	
    public String title;
    public boolean isMyProfile;
	public boolean canEdit;
    public ToponymSearchResult locationSearchResult;
	public User showUser;
	public ArrayList<ContactDetailType> contactDetailTypes;
	public ArrayList<User> contactlist;
	public ArrayList<School> schoollist;
	public ArrayList<Classunit> classlist;
	public ArrayList<Post> postlist;
	public boolean ADMIN_INIT = true;

	private boolean isMyid;

    public Profile(){
        title = lang.get("profile.title");
		contactDetailTypes = getContactDetailTypeArray();
		canEdit = false;
		isMyid = false;

		Long authid = authUser != null ? authUser.getId() : 0L;
		Long showuid = NumberUtils.toLong(getParamValue("id"), authid);
		String uuid = getParamValue("uuid");

		isMyid = authenticated && showuid.equals(authid);

		if(isMyid){
			//requested userid !exists or = my userid => show my profile
			showUser = authUser;
			isMyProfile = true;
		}else if(showuid.longValue() != 0 || !StringUtils.isBlank(uuid)){
			showUser = (showuid.longValue() == 0) ? User.getUserDao().read(uuid) :
				User.getUserDao().read(showuid);
			isMyProfile = false;
		}

		if (showUser == null) {
			setRedirect(profilelink);
			return;
		}

		if(authenticated && (isMyid || inRole("admin"))){
			canEdit = true;
		}

		myprofilelink = profilelink + "/" + showUser.getId();
		mycontactslink = myprofilelink + "/contacts";
		myquestionslink = myprofilelink + "/questions";
		myanswerslink = myprofilelink + "/answers";
		myphotoslink = myprofilelink + "/photos";
		mydrawerlink = myprofilelink + "/drawer";

		if(!authenticated){
			myphotoslink = profilelink + "/p/" + showUser.getId() + "/photos";
			mydrawerlink = profilelink + "/p/" + showUser.getId() + "/drawer";
		}

		title = lang.get("profile.title") + " - " + showUser.getFullname();

		if ("contacts".equals(showParam)){
			title += " - " + lang.get("contacts.title");
			contactlist = showUser.getAllContacts(pagenum, itemcount);
			pageMacroCode = "#peoplepage($contactlist)";
		} else if("questions".equals(showParam)) {
			title += " - " + lang.get("questions.title");
			postlist = showUser.getAllQuestions(pagenum, itemcount);
			pageMacroCode = "#questionspage($postlist)";
		} else if("answers".equals(showParam)) {
			title += " - " + lang.get("answers.title");
			postlist = showUser.getAllAnswers(pagenum, itemcount);
			pageMacroCode = "#compactanswerspage($postlist)";
		} else if("photos".equals(showParam)) {
			if(!authenticated){
				setRedirect(myphotoslink);
				return;
			}
			title += " - " + lang.get("photos.title");
			if(param("label")) title += " - " + getParamValue("label");
		} else if("drawer".equals(showParam)) {
			if(!authenticated){
				setRedirect(mydrawerlink);
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
		
		if("photos".equals(showParam)){
			processGalleryRequest(showUser, myphotoslink, canEdit);
		}else if("drawer".equals(showParam)){
			proccessDrawerRequest(showUser, mydrawerlink, canEdit);
		}

		if(!isMyProfile){
			if(param("addcontact")){
				int count = authUser.addContact(showUser);
				if(!addBadgeOnce(Badge.CONNECTED, count >= User.CONNECTED_IFHAS) && !isAjaxRequest())
					setRedirect(profilelink+"/"+showUser.getId()+"?code=12&success=true");
			}else if(param("removecontact")){
				authUser.removeContact(showUser);
				if(!isAjaxRequest())
					setRedirect(profilelink+"/"+showUser.getId()+"?code=13");
			}else if(param("makemod") && inRole("admin")){
				boolean makemod = Boolean.parseBoolean(getParamValue("makemod"));
				if(makemod == true && !showUser.isModerator()){
					showUser.setGroups(UserGroup.MODS.toString());
					showUser.update();
				}else if(makemod == false && showUser.isModerator() && !showUser.isAdmin()){
					try {
						UserType ut = UserType.valueOf(showUser.getType().toUpperCase());
						showUser.setGroups(ut.toGroupString());
						showUser.update();
					} catch (Exception e) {
						logger.severe(e.toString());
					}
				}
			}
		}
    }

    public void onPost(){
		if(canEdit){
			if("photos".equals(showParam)){
				processImageEmbedRequest(showUser, myphotoslink, canEdit);
			}else if("drawer".equals(showParam)) {
				proccessDrawerRequest(showUser, mydrawerlink, canEdit);
			}
			
			if(param("fullname")){
				String newFname = getParamValue("fullname");
				if(newFname != null && newFname.length() >= 3){
					showUser.setFullname(newFname);
					showUser.update();
				}
			}else if(param("status")){
				String status = StringUtils.abbreviate(getParamValue("status"), 160);
				showUser.setStatus(status);
				showUser.update();
			}else if(param("getmyschoolscode")){
				Long id = NumberUtils.toLong(getParamValue("sid"), 0);
				int from = NumberUtils.toInt(getParamValue("fromyear"), 0);
				int to = NumberUtils.toInt(getParamValue("toyear"), 0);
				if(id > 0){
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
				AbstractDAOUtils.populate(showUser, req.getParameterMap());
				addBadgeOnce(Badge.NICEPROFILE, showUser.isComplete() && isMyid);
				showUser.update();

				if(!isAjaxRequest())
					setRedirect(profilelink); //redirect after post
			}
		}
    }
}
