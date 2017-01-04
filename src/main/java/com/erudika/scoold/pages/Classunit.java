/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

//import com.amazonaws.util.json.JSONArray;
//import com.amazonaws.util.json.JSONException;
//import com.amazonaws.util.json.JSONObject;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.User;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.email.Emailer;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.ScooldUser.Badge;
import com.erudika.scoold.utils.AppConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.click.control.RadioGroup;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Classunit extends Base{

	public String title;
    public boolean fullaccess;
	public com.erudika.scoold.core.Classunit showClass;
	public List<User> peoplelist;
	public RadioGroup radioGroup;
	public boolean canEdit;
	public Post showPost;
	public boolean isLinkedToMe;
	public boolean isMine;

	private Emailer emailer;

	public Classunit() {
		title = lang.get("class.title");
		emailer = AppConfig.emailer();
		showClass = null;
		String classid = getParamValue("classid");

        if (classid == null) {
			setRedirect(classeslink);
			return;
		} else {
			showClass = pc.read(classid);

			if (showClass != null && ParaObjectUtils.typesMatch(showClass)) {
				title = title + " - " + showClass.getIdentifier();
			} else {
				setRedirect(classeslink);
				return;
			}
		}

		isLinkedToMe = (authenticated) ? showClass.isLinkedTo(authUser) : false;
		isMine = (authenticated) ? authUser.getId().equals(showClass.getCreatorid()) : false;
		isMine = isMine || inRole("admin");

		if (authenticated && (isMine || authUser.hasBadge(Badge.ENTHUSIAST)
				|| inRole("mod"))) {
			canEdit = true;
		} else {
			canEdit = false;
		}

		if ("chat".equals(showParam)) {
			if (!authenticated) {
				setRedirect(classlink + "/p/" + showClass.getId() + "/chat");
				return;
			}
			title += " - " + lang.get("class.chat");
		} else {
			peoplelist = new ArrayList<User>();
			for (ParaObject user : pc.getLinkedObjects(showClass, Utils.type(User.class), itemcount)) {
				peoplelist.add((User) user);
			}
			showPost = showClass.getBlackboard();
//			includeFBscripts = true;
		}
	}

	public void onRender() {
		if (showPost != null && canEdit)
			showPost.setEditForm(getPostEditForm(showPost));
	}

	public void onGet() {
		if (!authenticated || showClass == null) return;

		if (param("join")) {
			showClass.linkToUser(authUser.getId());
			isLinkedToMe = true;
			peoplelist.add(authUser);
			if (!addBadgeOnce(Badge.FIRSTCLASS, true) && !isAjaxRequest())
				setRedirect(classlink+"/"+showClass.getId()+"?code=14&success=true");
		} else if (param("leave")) {
			showClass.unlinkFromUser(authUser.getId());
			isLinkedToMe = false;
			peoplelist.remove(authUser);
			if (!isAjaxRequest())
				setRedirect(classlink+"/"+showClass.getId()+"?code=15");
		} else if (param("thisisme")) {
			// allow new users to click on "this is me" and users with 20+ rep
			if (authUser.getVotes() >= 20 || authUser.getTimestamp() <
					(System.currentTimeMillis() + 24*60*60*1000)) { // regged within 1 day
				String name = getParamValue("thisisme");
				String newInactive = showClass.getInactiveusers();
				if (!StringUtils.isBlank(name) && !StringUtils.isBlank(newInactive)) {
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
		} else if (param("receivechat")) {
			chatJSONResponse(showClass.receiveChat());
		}
	}

	public void onPost() {
		if (param("identifier")) {
			String newIdent = getParamValue("identifier");
			if (newIdent != null && newIdent.length() >= 4) {
				showClass.setIdentifier(newIdent);
				showClass.update();
			}
		} else if (param("sendchat")) {
			chatJSONResponse(showClass.sendChat(getJSONMessage(getParamValue("message"))));
		} else {
			if (canEdit) {
				ParaObjectUtils.populate(showClass, req.getParameterMap());
				showClass.update();
				if (!isAjaxRequest()) setRedirect(classlink+"/"+showClass.getId());
			}
		}
	}

	public boolean onPostEditClick() {
		if (isValidPostEditForm(showPost.getEditForm())) {
			String escapelink = classlink+"/"+showClass.getId();
			processPostEditRequest(showPost, escapelink, canEdit);
		}
		return false;
	}

	private void chatJSONResponse(String chat) {
		try {
			ArrayList<Object> list = ParaObjectUtils.getJsonReader(ArrayList.class).readValue(chat);

//			JSONArray arr = new JSONArray(chat);
//			for (int i = 0; i < arr.length(); i++) {
//				list.add(arr.get(i));
//			}
			addModel("chatMessages", list);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
		} catch (Exception ex) {
			logger.error(null, ex);
		}
	}

	private String getJSONMessage(String msg) {
		Map<String, Object> jo = new HashMap<String, Object>();
//		JSONObject jo = new JSONObject();
		try {
			jo.put("id", authUser.getId());
			jo.put("name", authUser.getName());
			jo.put("groups", authUser.getGroups());
			jo.put("message", msg);
			jo.put("stamp", System.currentTimeMillis());
			return ParaObjectUtils.getJsonWriterNoIdent().writeValueAsString(jo);
		} catch (Exception ex) {
			logger.error(null, ex);
		}
		return "{}";
	}

}
