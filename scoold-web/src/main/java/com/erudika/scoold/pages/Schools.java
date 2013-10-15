/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.para.core.PObject;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.School;
import java.util.ArrayList;
import org.apache.click.control.Form;
import org.apache.click.control.Option;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class Schools extends BasePage{

	public String title;
	public ArrayList<School> topSchools;
	public ArrayList<School> schoollist;
	public Form createSchoolForm; 

	public Schools() {
		if(param("create") && authenticated){
			title = lang.get("schools.title") + " - " + lang.get("school.create");
			addModel("includeGMapsScripts", true);
			makeforms();
		}else{
			title = lang.get("schools.title");
			addModel("includeGMapsScripts", false);
		}
		
		addModel("schoolsSelected", "navbtn-hover");
	}
	
	public void onGet(){
		if (!param("create")) {
			String sortBy = "";
			if("votes".equals(getParamValue("sortby"))) sortBy = "votes";
			schoollist = search.findQuery(PObject.classname(School.class), pagenum, itemcount, 
					"*", sortBy, true, MAX_ITEMS_PER_PAGE);
		}
	}

	private void makeforms(){
		createSchoolForm = new Form("createSchoolForm");

        TextField name = new TextField("name", true);
        name.setLabel(lang.get("profile.myschools.create.name"));
		name.setFocus(true);

        Select type = new Select("type", true);
        type.setLabel(lang.get("profile.myschools.create.type"));
        type.add(new Option("", lang.get("chooseone")));
        type.addAll(School.getSchoolTypeMap(lang));

        TextField location = new TextField("location", true);
        location.setLabel(lang.get("profile.about.location"));

        Submit create = new Submit("createschool", lang.get("create"),
                this, "onCreateSchoolClick");
        create.setAttribute("class", "button rounded3");

        createSchoolForm.add(name);
        createSchoolForm.add(type);
        createSchoolForm.add(location);
        createSchoolForm.add(create).setId("createschool");
	}

	public boolean onCreateSchoolClick() {//
		if(createSchoolForm.isValid()){
			if(param("create")){
				School school = new School();
				Utils.populate(school, req.getParameterMap());
				school.setCreatorid(authUser.getId());
				school.create();

				// automaticlly add to my schools
				school.linkToUser(authUser.getId());

				setRedirect(schoollink+"/"+school.getId());
			}
		}
        return false;
    }

}
