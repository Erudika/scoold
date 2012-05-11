/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scoold.pages;

import com.scoold.core.Post;
import com.scoold.core.User;
import com.scoold.db.AbstractDAOUtils;
import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 *
 * @author alexb
 */
public class Group extends BasePage{
	
	public String title;
    public com.scoold.core.Group showGroup;
	public boolean isLinkedToMe;
	public boolean isMine;	
	public ArrayList<User> memberslist;
	public ArrayList<User> contactslist;
	public ArrayList<Post> postslist;
	
	public Group() {
		title = lang.get("group.title");
		
		showGroup = null;
		Long groupid = NumberUtils.toLong(getParamValue("groupid"), 0);

        if(groupid.longValue() == 0L){
			setRedirect(groupslink);
			return;
		}else{
            showGroup = com.scoold.core.Group.getGroupDao().read(groupid);

            if(showGroup != null && daoutils.typesMatch(showGroup)){
                title = title + " - " + showGroup.getName();
            }else{
				setRedirect(groupslink);
				return;
			}
        }
		
		isLinkedToMe = (authenticated) ? showGroup.isLinkedTo(authUser) : false;
		isMine = (authenticated) ? authUser.getId().equals(showGroup.getUserid()) : false;
		isMine = isMine || inRole("admin");
		
		if(!isLinkedToMe && !inRole("mod")){
			setRedirect(groupslink);
			return;
		}
		
		if("members".equals(showParam)){
			title = title + " - " + lang.get("group.members");
			memberslist = showGroup.getAllUsers(pagenum, itemcount);
			contactslist = User.getUserDao().readAllUsersForID(authUser.getId(), null, null);
		}else{
			String sortBy = "";
			if("activity".equals(getParamValue("sortby"))) sortBy = "lastactivity";
			else if("votes".equals(getParamValue("sortby"))) sortBy = "votes";
			postslist = showGroup.getQuestions(sortBy, pagenum, itemcount);
		}
	}

	public void onGet() {
	}

	public void onPost() {
		if(param("name")){
			String newName = getParamValue("name");
			if(newName != null && newName.length() >= 4){
				showGroup.setName(newName);
				showGroup.update();
			}
		}else if(param("description")){
			showGroup.setDescription(StringUtils.abbreviate(getParamValue("description"), 200));
			showGroup.update();
		}else if(param("imageurl")){
			String imgurl = getParamValue("imageurl");
			showGroup.setImageurl(imgurl.trim());
			showGroup.update();
			if(!isAjaxRequest()) setRedirect(grouplink+"/"+showGroup.getId());
		}else if("members".equals(showParam)){
			if(param("add")){
				String[] ids = req.getParameterValues("memberids");
				ArrayList<Long> uids = new ArrayList<Long> ();
				for (String id : ids) {
					Long uid = NumberUtils.toLong(id, 0);
					if(uid != 0L && !uid.equals(authUser.getId())) uids.add(uid);
				}
				showGroup.linkToUsers(uids);
			}else if(param("remove")){
				Long uid = NumberUtils.toLong(getParamValue("remove"), 0);
				if(uid != 0L && !uid.equals(authUser.getId())) showGroup.unlinkFromUser(uid);
			}
			if(!isAjaxRequest()) setRedirect(grouplink+"/"+showGroup.getId()+"/members");
		}else if(param("leave") && isLinkedToMe && !isMine){
			showGroup.unlinkFromUser(authUser.getId());
			if(!isAjaxRequest()) setRedirect(groupslink);
		}else if(param("delete") && isMine){
			showGroup.delete();
			if(!isAjaxRequest()) setRedirect(groupslink);
		}else{
			if(isMine) {
				AbstractDAOUtils.populate(showGroup, req.getParameterMap());
				showGroup.update();
			}
			if(!isAjaxRequest()) setRedirect(grouplink+"/"+showGroup.getId());
		}
	}
	
}
