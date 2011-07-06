/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import com.scoold.core.User;
import java.util.ArrayList;

/**
 *
 * @author alexb
 */
public class People extends BasePage{

	public String title;
	public ArrayList<User> userlist;

	public People() {
		title = lang.get("people.title");
		pageMacroCode = "#peoplepage($userlist)";
		addModel("peopleSelected", "navbtn-hover");
	}
	
	public void onGet(){
		String sortBy = "timestamp";
		if("rep".equals(getParamValue("sortby"))) sortBy = "reputation";
		userlist = User.getUserDao().readAllSortedBy(sortBy, pagenum, itemcount, true);
	}
}
