/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import com.scoold.core.Post;
import com.scoold.core.User;
import com.scoold.core.User.Badge;
import com.scoold.core.User.UserType;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractDAOUtils;
import com.scoold.util.MailTask;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import org.apache.click.control.RadioGroup;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author alexb
 */
public class Classunit extends BasePage{

	public String title;
    public boolean fullaccess;
	public com.scoold.core.Classunit showClass;
	public ArrayList<User> peoplelist;
	public RadioGroup radioGroup;
	public boolean canEdit;
	public Post showPost;
	public boolean isLinkedToMe;
	public boolean isMine;

	public String photoslink;
	public String drawerlink;
	
	public Classunit(){
		title = lang.get("class.title");
		
		showClass = null;
		Long classid = NumberUtils.toLong(getParamValue("classid"), 0);

        if(classid.longValue() == 0L){
			setRedirect(classeslink);
			return;
		}else{
            showClass =	com.scoold.core.Classunit.getClassUnitDao().read(classid);

            if(showClass != null && daoutils.typesMatch(showClass)){
                title = title + " - " + showClass.getIdentifier();
			}else{
				setRedirect(classeslink);
				return;
			}
        }

		isLinkedToMe = (authenticated) ? showClass.isLinkedTo(authUser) : false;
		isMine = (authenticated) ? authUser.getId().equals(showClass.getUserid()) : false;
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
			peoplelist = showClass.getAllUsers(pagenum, itemcount);
			showPost = showClass.getBlackboard();
//			includeFBscripts = true;
		}
	}

	public void onRender(){
		if(showPost != null && canEdit)
			showPost.setEditForm(getPostEditForm(showPost));
	}

	public void onGet(){
		if(authenticated){
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
				chatJSONResponse(com.scoold.core.Classunit.getClassUnitDao().
						receiveChat(showClass.getId()));
			}
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

			if(param("fullname") && param("email")){
				String[] fns = req.getParameterValues("fullname");
				String[] emails = req.getParameterValues("email");
				ArrayList<User> users = new ArrayList<User>();

				int max = (fns.length > AbstractDAOFactory.MAX_INVITES) ?
					AbstractDAOFactory.MAX_INVITES : fns.length;
				
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
				inactive = AbstractDAOUtils.fixCSV(inactive);
				showClass.setInactiveusers(inactive);
				showClass.update();
			}
			if(!isAjaxRequest()) setRedirect(classlink+"/"+showClass.getId());
		}else if (param("sendchat")) {
			chatJSONResponse(com.scoold.core.Classunit.getClassUnitDao().
					sendChat(showClass.getId(), getJSONMessage(getParamValue("message"))));
		}else{
			if(canEdit){
				AbstractDAOUtils.populate(showClass, req.getParameterMap());
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
		Date after10sec = new Date(System.currentTimeMillis() + 10000);
		String s1 = AbstractDAOUtils.formatMessage(lang.get("class.invite.text"),
				authUser.getFullname());
		String body =  s1 + "  http://scoold.com/class/" + showClass.getId();
		String subject = lang.get("class.invitation");
		
		TimerTask mailtask = new MailTask(users, body, subject);
		Timer timer = new Timer();
		timer.schedule(mailtask, after10sec);
	}
	
	private void chatJSONResponse(String chat){
		try {
			ArrayList<Object> list = new ArrayList<Object>(); 
			JSONArray arr = new JSONArray(chat);
			for (int i = 0; i < arr.length(); i++) {
				list.add(arr.get(i));
			}
			addModel("chatMessages", list);
			getContext().getResponse().setContentType("application/json");
			getContext().getResponse().setCharacterEncoding("UTF-8");
		} catch (JSONException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
	}
	
	private String getJSONMessage(String msg){
		JSONObject jo = new JSONObject();
		try {
			jo.put("id", authUser.getId());
			jo.put("fullname", authUser.getFullname());
			jo.put("groups", authUser.getGroups());
			jo.put("message", msg);
			jo.put("stamp", System.currentTimeMillis());
		} catch (JSONException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		
		return jo.toString();
	}

}
