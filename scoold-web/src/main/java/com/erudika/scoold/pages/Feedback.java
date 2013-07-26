/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.para.core.PObject;
import com.erudika.para.utils.Search;
import com.erudika.scoold.core.Post;
import java.util.ArrayList;
import org.apache.click.control.Form;

/**
 *
 * @author alexb
 */
public class Feedback <P extends Post> extends BasePage{

	public String title;
	public ArrayList<Post> feedbacklist;
	public Form fForm;

	public Feedback(){
		title = lang.get("feedback.title");

		if(param("write")){
			title += " - " + lang.get("feedback.write");
			fForm = getFeedbackForm();
		}
	}

	public void onGet(){
		String feedbackType = PObject.classname(com.erudika.scoold.core.Feedback.class);
		
		if (param("tag")) {
			String tag = getParamValue("tag");
			ArrayList<String> tags = new ArrayList<String>();
			tags.add(tag);
			feedbacklist = Search.findTagged(feedbackType, pagenum, itemcount, tags);
		} else if(param("search")){
			feedbacklist = Search.findQuery(feedbackType, pagenum, itemcount, getParamValue("search"));
		} else {
			String sortBy = "";
			if("activity".equals(getParamValue("sortby"))){
				sortBy = "lastactivity";
			} else if ("votes".equals(getParamValue("sortby"))){
				sortBy = "votes";
			}

			feedbacklist = Search.findQuery(feedbackType, pagenum, itemcount, "*", sortBy, true, MAX_ITEMS_PER_PAGE);
		}		
	}

	public boolean onAskClick(){
		if(isValidQuestionForm(fForm)){
			createAndGoToPost(com.erudika.scoold.core.Feedback.class);
		}
		return false;
	}
}
