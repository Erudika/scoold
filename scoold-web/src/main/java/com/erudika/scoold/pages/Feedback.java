/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Post;
import java.util.List;
import org.apache.click.control.Form;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Feedback <P extends Post> extends Base{

	public String title;
	public List<Post> feedbacklist;
	public Form fForm;

	public Feedback() {
		title = lang.get("feedback.title");

		if (param("write")) {
			title += " - " + lang.get("feedback.write");
			fForm = getFeedbackForm();
		}
	}

	public void onGet() {
		String feedbackType = Utils.type(com.erudika.scoold.core.Feedback.class);

		if (param("tag")) {
			String tag = getParamValue("tag");
			feedbacklist = pc.findTagged(feedbackType, new String[]{tag}, itemcount);
		} else if (param("search")) {
			feedbacklist = pc.findQuery(feedbackType, getParamValue("search"), itemcount);
		} else {
			String sortBy = "";
			if ("activity".equals(getParamValue("sortby"))) {
				sortBy = "lastactivity";
			} else if ("votes".equals(getParamValue("sortby"))) {
				sortBy = "votes";
			}
			itemcount.setSortby(sortBy);
			feedbacklist = pc.findQuery(feedbackType, "*", itemcount);
		}
	}

	public boolean onAskClick() {
		if (isValidQuestionForm(fForm)) {
			createAndGoToPost(com.erudika.scoold.core.Feedback.class);
		}
		return false;
	}
}
