/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.erudika.para.email.Emailer;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.User;
import com.erudika.scoold.core.User.Badge;
import com.erudika.scoold.core.User.UserType;
import com.erudika.scoold.util.Constants;
import java.util.ArrayList;
import java.util.logging.Level;
import javax.inject.Inject;
import org.apache.click.control.RadioGroup;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class Classunit extends BasePage{

	public String title;
    public boolean fullaccess;
	public com.erudika.scoold.core.Classunit showClass;
	public ArrayList<User> peoplelist;
	public RadioGroup radioGroup;
	public boolean canEdit;
	public Post showPost;
	public boolean isLinkedToMe;
	public boolean isMine;

	public String photoslink;
	public String drawerlink;

	@Inject private Emailer emailer;
	
	public Classunit(){
		title = lang.get("class.title");
		
		showClass = null;
		String classid = getParamValue("classid");

        if (classid == null) {
			setRedirect(classeslink);
			return;
		} else {
			showClass = dao.read(classid);

			if (showClass != null && utils.typesMatch(showClass)) {
				title = title + " - " + showClass.getIdentifier();
			} else {
				setRedirect(classeslink);
				return;
			}
		}

		isLinkedToMe = (authenticated) ? showClass.isLinkedTo(authUser) : false;
		isMine = (authenticated) ? authUser.getId().equals(showClass.getCreatorid()) : false;
		isMine = isMine || inRole("admin");

		photoslink = classlink + "/" + showClass.getId() + "/photos";
		drawerlink = classlink + "/" + showClass.getId() + "/drawer";

		
		if(authenticated && (isMine || authUser.hasBadge(Badge.ENTHUSIAST)
				|| inRole("mod"))){
			canEdit = true;
		}else{
			canEdit = false;
		}

		if("photos".equals(showParam)){
			if(!authenticated){
				setRedirect("/p" + photoslink);
				return;
			}
			title += " - " + lang.get("photos.title");
			if(param("label")) title += " - " + getParamValue("label");
		}else if("drawer".equals(showParam)){
			if(!authenticated){
				setRedirect("/p" + drawerlink);
				return;
			}
			title += " - " + lang.get("drawer.title");
		}else if("chat".equals(showParam)){
			if(!authenticated){
				setRedirect(classlink + "/p/" + showClass.getId() + "/chat");
				return;
			}
			title += " - " + lang.get("class.chat");

		}else if("invite".equals(showParam) && isMine && isLinkedToMe){
			title = title + " - " + lang.get("invite");
			includeFBscripts = true;
		}else{
			peoplelist = showClass.getLinkedObjects(User.class, pagenum, itemcount);
			showPost = showClass.getBlackboard();
//			includeFBscripts = true;
		}
	}

	public void onRender(){
		if(showPost != null && canEdit)
			showPost.setEditForm(getPostEditForm(showPost));
	}

	public void onGet(){
		if(!authenticated || showClass == null) return;
		
		if ("photos".equals(showParam)) {
			processGalleryRequest(showClass, photoslink, canEdit);
		} else if("drawer".equals(showParam)) {
			proccessDrawerRequest(showClass, drawerlink, canEdit);
		}

		if(param("join")){
			showClass.linkToUser(authUser.getId());
			isLinkedToMe = true;
			peoplelist.add(authUser);
			if(!addBadgeOnce(Badge.FIRSTCLASS, true) && !isAjaxRequest())
				setRedirect(classlink+"/"+showClass.getId()+"?code=14&success=true");
		}else if(param("leave")){
			showClass.unlinkFromUser(authUser.getId());
			isLinkedToMe = false;
			peoplelist.remove(authUser);
			if(!isAjaxRequest())
				setRedirect(classlink+"/"+showClass.getId()+"?code=15");
		}else if(param("thisisme")){
			// allow new users to click on "this is me" and users with 20+ rep
			if(authUser.getReputation() >= 20 || authUser.getTimestamp() <
					(System.currentTimeMillis() + 24*60*60*1000)){ // regged within 1 day
				String name = getParamValue("thisisme");
				String newInactive = showClass.getInactiveusers();
				if(!StringUtils.isBlank(name) && !StringUtils.isBlank(newInactive)){
					name = ",".concat(name).concat(",");
					newInactive = newInactive.replaceFirst(name, "");
					showClass.setInactiveusers(StringUtils.trimToNull(newInactive));
					showClass.update();
				}
			}
			if (!isLinkedToMe) {
				setRedirect(classlink+"/"+showClass.getId()+"/join");
			} else {
				setRedirect(classlink+"/"+showClass.getId());
			}
			return;
		}else if(param("receivechat")){
			chatJSONResponse(showClass.receiveChat());
		}
	}

	public void onPost(){
		if(param("identifier")){
			String newIdent = getParamValue("identifier");
			if(newIdent != null && newIdent.length() >= 4){
				showClass.setIdentifier(newIdent);
				showClass.update();
			}
		}else if("photos".equals(showParam)){
			processImageEmbedRequest(showClass, photoslink, canEdit);
			processGalleryRequest(showClass, photoslink, canEdit);
		}else if("drawer".equals(showParam)) {
			proccessDrawerRequest(showClass, drawerlink, canEdit);
		}else if (param("addclassmates") && isMine && isLinkedToMe) {
			String inactive = showClass.getInactiveusers();
			if(inactive == null) inactive = ",";

			if(param("name") && param("email")){
				String[] fns = req.getParameterValues("name");
				String[] emails = req.getParameterValues("email");
				ArrayList<User> users = new ArrayList<User>();

				int max = (fns.length > Constants.MAX_INVITES) ?
					Constants.MAX_INVITES : fns.length;
				
				for (int i= 0; i < max; i++) {
					String name = fns[i];
					String email = "";
					if(i < emails.length) email = emails[i];
					if(!StringUtils.isBlank(email) && email.matches(".+@.+\\.[a-z]+") &&
							!StringUtils.isBlank(name)){
						// send email invite to...
						users.add(new User(email, false, UserType.STUDENT, name));
					}
					if(!StringUtils.isBlank(name)){
						name = name.replaceAll(",", "");
						inactive = inactive.concat(name).concat(",");
					}
				}

				if(!users.isEmpty()) sendInvites(users);
			}else{
				// facebook invite
				for(String name : req.getParameterValues("ids[]")){
					if(!StringUtils.isBlank(name)){
						name = name.replaceAll(",", "");
						inactive = inactive.concat(name).concat(",");
					}
				}
			}

			if(!inactive.equals(",")){
				inactive = Utils.fixCSV(inactive);
				showClass.setInactiveusers(inactive);
				showClass.update();
			}
			if(!isAjaxRequest()) setRedirect(classlink+"/"+showClass.getId());
		}else if (param("sendchat")) {
			chatJSONResponse(showClass.sendChat(getJSONMessage(getParamValue("message"))));
		}else{
			if(canEdit){
				Utils.populate(showClass, req.getParameterMap());
				showClass.update();
				if(!isAjaxRequest()) setRedirect(classlink+"/"+showClass.getId());
			}
		}
	}

	public boolean onPostEditClick(){
		if(isValidPostEditForm(showPost.getEditForm())){
			String escapelink = classlink+"/"+showClass.getId();
			processPostEditRequest(showPost, escapelink, canEdit);
		}
		return false;
	}

	private void sendInvites(ArrayList<User> users){
		String s1 = Utils.formatMessage(lang.get("class.invite.text"), authUser.getName());
		String body =  s1 + "  http://scoold.com/class/" + showClass.getId();
		String subject = lang.get("class.invitation");
		ArrayList<String> emails = new ArrayList<String>();
		for (User user : users) emails.add(user.getEmail());
		emailer.sendEmail(emails, subject, body);
	}
	
	private void chatJSONResponse(String chat){
		try {
			ArrayList<Object> list = new ArrayList<Object>(); 
			
			JSONArray arr = new JSONArray(chat);
			for (int i = 0; i < arr.length(); i++) {
				list.add(arr.get(i));
			}
			addModel("chatMessages", list);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
		} catch (JSONException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
	}
	
	private String getJSONMessage(String msg){
		JSONObject jo = new JSONObject();
		try {
			jo.put("id", authUser.getId());
			jo.put("name", authUser.getName());
			jo.put("groups", authUser.getGroups());
			jo.put("message", msg);
			jo.put("stamp", System.currentTimeMillis());
		} catch (JSONException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		
		return jo.toString();
	}

}
