/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scoold.pages;

import java.util.ArrayList;
import com.scoold.core.Group;
import com.scoold.core.User;
import com.scoold.db.AbstractDAOUtils;
import java.util.Map;
import org.apache.click.control.*;

/**
 *
 * @author alexb
 */
public class Groups extends BasePage{
	
	public String title;
    public ArrayList<Group> grouplist;
	public Form createGroupForm;
	public Map<Long, String> groupsMap;

	public Groups() {
		if (param("create")) {
			title = lang.get("groups.title") + " - " + lang.get("group.create");
			makeforms();
		} else {
			title = lang.get("groups.title");
			grouplist = User.getUserDao().readAllGroupsForUser(
					authUser.getId(), pagenum, itemcount);
		}
		
		addModel("groupsSelected", "navbtn-hover");
	}

	public void onGet() {
	}

	private void makeforms(){
		if(!authenticated) return;
		
        createGroupForm = new Form("createGroupForm");

        TextField name = new TextField("name", true);
		name.setMinLength(4);
        name.setLabel(lang.get("name"));

		TextArea description = new TextArea("description", false);
		description.setMaxLength(200);
		description.setLabel(lang.get("description"));
		
        TextField imgurl = new TextField("imageurl", false);
		imgurl.setValue("http://");
        imgurl.setLabel(lang.get("group.image"));
		
		Submit create = new Submit("creategroup", lang.get("create"),
                this, "onCreateGroupClick");
        create.setAttribute("class", "button rounded3");

        createGroupForm.add(name);
        createGroupForm.add(description);
        createGroupForm.add(imgurl);
        createGroupForm.add(create).setId("creategroup");
		createGroupForm.clearErrors();
	}
	
	public boolean onCreateGroupClick() {  //
		if(createGroupForm.isValid()){
			Map<String, String[]> paramMap = req.getParameterMap();
			Group group = new Group();
			AbstractDAOUtils.populate(group, paramMap);
			group.setUserid(authUser.getId());
			group.create();

			setRedirect(grouplink+"/"+group.getId());
		}
		return false;
    }
	
}
