/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.para.core.PObject;
import com.erudika.scoold.core.User;
import java.util.ArrayList;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class People extends Base{

	public String title;
	public ArrayList<User> userlist;

	public People() {
		title = lang.get("people.title");
		addModel("peopleSelected", "navbtn-hover");
	}
	
	public void onGet(){
		String sortBy = "";
		if("rep".equals(getParamValue("sortby"))) sortBy = "reputation";
		userlist = search.findQuery(PObject.classname(User.class), pagenum, itemcount, "*", sortBy, true, MAX_ITEMS_PER_PAGE);
	}
}
