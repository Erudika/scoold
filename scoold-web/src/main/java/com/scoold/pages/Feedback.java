/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import com.scoold.core.Post;
import com.scoold.core.Post.PostType;
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
		if (param("tag")) {
			String tag = getParamValue("tag");
			ArrayList<String> tags = new ArrayList<String>();
			tags.add(tag);
			feedbacklist = daoutils.readAndRepair(Post.class, daoutils.findTagged(
					PostType.FEEDBACK.toString(), pagenum, itemcount, tags), itemcount);
		} else if(param("search")){
			feedbacklist = new ArrayList<Post> ();
			feedbacklist.addAll(daoutils.readAndRepair(Post.class, daoutils.findQuery(
					PostType.FEEDBACK.toString(), pagenum, itemcount, 
					getParamValue("search")), itemcount));
		} else {
			String sortBy = "";
			if("activity".equals(getParamValue("sortby"))){
				sortBy = "lastactivity";
			} else if ("votes".equals(getParamValue("sortby"))){
				sortBy = "votes";
			}

			feedbacklist = daoutils.readAndRepair(Post.class, daoutils.findQuery(
					PostType.FEEDBACK.toString(), pagenum, itemcount, "*", sortBy, 
					true, MAX_ITEMS_PER_PAGE), itemcount);
		}		
	}

	public boolean onAskClick(){
		if(isValidQuestionForm(fForm)){
			createAndGoToPost(PostType.FEEDBACK);
		}
		return false;
	}
}
