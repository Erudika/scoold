/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.para.persistence.DAO;
import com.erudika.para.core.PObject;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Classunit;
import java.util.ArrayList;
import java.util.Map;
import org.apache.click.control.Form;
import org.apache.click.control.Option;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.commons.lang3.math.NumberUtils;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */ 
public class Classunits extends BasePage{

	public String title;
	public ArrayList<Classunit> classlist;
	public Form createClassForm;
	public Map<String, String> schoolsMap;
	
	public Classunits(){
		if(param("create") && authenticated){
			title = lang.get("classes.title") + " - " + lang.get("class.create");
			makeforms();
		}else{
			title = lang.get("classes.title");
		}
		
		addModel("classesSelected", "navbtn-hover");
	} 

	public void onGet(){
		if(!param("create")){
			classlist = search.findQuery(PObject.classname(Classunit.class), pagenum, itemcount, "*");
		}
	}

	private void makeforms(){
		if(!authenticated) return;
		
        createClassForm = new Form("createClassForm");
		schoolsMap = authUser.getSimpleSchoolsMap();

        TextField identifier = new TextField("identifier", true);
		identifier.setMinLength(4);
        identifier.setLabel(lang.get("profile.myclasses.create.name"));

        Select schoolselect = new Select(DAO.CN_PARENTID, true);
        schoolselect.setLabel(lang.get("profile.myclasses.create.schoollink"));
        schoolselect.add(new Option("", lang.get("chooseone")));
        schoolselect.addAll(schoolsMap);
		
		Select gradyear = new Select("gradyear", true);
		gradyear.setLabel(lang.get("profile.myclasses.gradyear"));
		int year = Utils.getCurrentYear(); 
		gradyear.add(new Option("", lang.get("chooseone")));
		for (int i = (year-50); i <= (year+20); i++) {
			gradyear.add(new Option(String.valueOf(i), String.valueOf(i)));
		}
		
		Submit create = new Submit("createclass", lang.get("create"),
                this, "onCreateClassClick");
        create.setAttribute("class", "button rounded3");

        createClassForm.add(identifier);
        createClassForm.add(schoolselect);
		createClassForm.add(gradyear);
        createClassForm.add(create).setId("createclass");
		createClassForm.clearErrors();
	}

    public boolean onCreateClassClick() {  //
		int year = NumberUtils.toInt(getParamValue("gradyear"), 0);
		int currentYear = Utils.getCurrentYear();
		if(year <= 0 || year < (currentYear - 100) || year > (currentYear + 100)){
			createClassForm.getField("gradyear").setError("Invalid year");
		}
		if(createClassForm.isValid()){
			Map<String, String[]> paramMap = req.getParameterMap();
			Classunit classu = new Classunit();
			Utils.populate(classu, paramMap);
			classu.setCreatorid(authUser.getId());
			classu.create();

			setRedirect(classlink+"/"+classu.getId());
		}
		return false;
    }

}
