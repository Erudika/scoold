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
		pageMacroCode = "#questionspage($feedbacklist)";

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
			feedbacklist = search.findPostsForTags(PostType.FEEDBACK,
					tags, pagenum, itemcount);
		} else if(param("search")){
			feedbacklist = new ArrayList<Post> ();
			feedbacklist.addAll(search.findByKeyword(Post.Feedback.class,
					pagenum, itemcount, getParamValue("search")));
		} else {
			String sortBy = "timestamp";
			if("activity".equals(getParamValue("sortby"))){
				sortBy = "lastactivity";
			} else if ("votes".equals(getParamValue("sortby"))){
				sortBy = "votes";
			}

			feedbacklist = Post.getPostDao().
					readFeedbackSortedBy(sortBy, pagenum, itemcount, true);
		}
	}

	public boolean onAskClick(){
		if(isValidQuestionForm(fForm)){
			createAndGoToPost(PostType.FEEDBACK);
		}
		return false;
	}
}
