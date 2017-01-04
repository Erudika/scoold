/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.pages;

import com.erudika.para.core.User;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.ScooldUser;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Group extends Base{

	public String title;
    public com.erudika.scoold.core.Group showGroup;
	public boolean isLinkedToMe;
	public boolean isMine;
	public List<ScooldUser> memberslist;
	public List<ScooldUser> contactslist;
	public List<Post> postslist;

	public Group() {
		title = lang.get("group.title");

		showGroup = null;
		String groupid = getParamValue("groupid");

        if (groupid == null) {
			setRedirect(groupslink);
			return;
		} else {
            showGroup = pc.read(groupid);

            if (showGroup != null && ParaObjectUtils.typesMatch(showGroup)) {
                title = title + " - " + showGroup.getName();
            } else {
				setRedirect(groupslink);
				return;
			}
        }

		isLinkedToMe = (authenticated) ? showGroup.isLinkedTo(authUser) : false;
		isMine = (authenticated) ? authUser.getId().equals(showGroup.getCreatorid()) : false;
		isMine = isMine || inRole("admin");

//		if (!isLinkedToMe && !inRole("mod")) {
//			setRedirect(groupslink);
//			return;
//		}

		if ("members".equals(showParam)) {
			title = title + " - " + lang.get("group.members");
			memberslist = pc.getLinkedObjects(showGroup, Utils.type(User.class), itemcount);
			contactslist = pc.getLinkedObjects(new User(authUser.getId()), Utils.type(User.class), null, null);
		} else {
			String sortBy = "";
			if ("activity".equals(getParamValue("sortby"))) sortBy = "lastactivity";
			else if ("votes".equals(getParamValue("sortby"))) sortBy = "votes";
			itemcount.setSortby(sortBy);
			postslist = showGroup.getQuestions(sortBy, itemcount);
		}
	}

	public void onGet() {
	}

	public void onPost() {
		if (param("name")) {
			String newName = getParamValue("name");
			if (newName != null && newName.length() >= 4) {
				showGroup.setName(newName);
				showGroup.update();
			}
		} else if (param("description")) {
			showGroup.setDescription(StringUtils.abbreviate(getParamValue("description"), 200));
			showGroup.update();
		} else if (param("imageurl")) {
			String imgurl = getParamValue("imageurl");
			showGroup.setImageurl(imgurl.trim());
			showGroup.update();
			if (!isAjaxRequest()) setRedirect(grouplink+"/"+showGroup.getId());
		} else if ("members".equals(showParam)) {
			if (param("add")) {
				String[] ids = req.getParameterValues("memberids");
				ArrayList<String> uids = new ArrayList<String>();
				for (String id : ids) {
					if (id != null && !id.equals(authUser.getId())) uids.add(id);
				}
				showGroup.linkToUsers(uids);
			} else if (param("remove")) {
				String uid = getParamValue("remove");
				if (uid != null && !uid.equals(authUser.getId())) showGroup.unlinkFromUser(uid);
			}
			if (!isAjaxRequest()) setRedirect(grouplink+"/"+showGroup.getId()+"/members");
		} else if (param("leave") && isLinkedToMe && !isMine) {
			showGroup.unlinkFromUser(authUser.getId());
			if (!isAjaxRequest()) setRedirect(groupslink);
		} else if (param("delete") && isMine) {
			showGroup.delete();
			if (!isAjaxRequest()) setRedirect(groupslink);
		} else {
			if (isMine) {
				ParaObjectUtils.populate(showGroup, req.getParameterMap());
				showGroup.update();
			}
			if (!isAjaxRequest()) setRedirect(grouplink+"/"+showGroup.getId());
		}
	}

}
