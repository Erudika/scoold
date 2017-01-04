/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.para.core.User;
import com.erudika.para.utils.Utils;
import java.util.List;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class People extends Base{

	public String title;
	public List<User> userlist;

	public People() {
		title = lang.get("people.title");
		addModel("peopleSelected", "navbtn-hover");
	}

	public void onGet() {
		String sortBy = "";
		if ("rep".equals(getParamValue("sortby"))) sortBy = "votes";
		itemcount.setSortby(sortBy);
		userlist = pc.findQuery(Utils.type(User.class), "*", itemcount);
	}
}
