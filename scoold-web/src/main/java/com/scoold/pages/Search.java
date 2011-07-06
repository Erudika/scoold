/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import com.scoold.core.Classunit;
import com.scoold.core.Post;
import com.scoold.core.User;
import com.scoold.core.School;
import java.util.ArrayList;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public class Search extends BasePage{

	public String title;
	public ArrayList<User> userlist;
	public ArrayList<Classunit> classlist;
	public ArrayList<School> schoollist;
	public ArrayList<Post> questionlist;
	public ArrayList<Post> feedbacklist;
	public String url;
	public MutableLong questioncount;
	public MutableLong feedbackcount;
	public MutableLong usercount;
	public MutableLong schoolcount;
	public MutableLong classcount;

	private int max = 5;

	public Search() {
		title = lang.get("search.title");
		questionlist = new ArrayList<Post>();
		feedbacklist = new ArrayList<Post>();
		userlist = new ArrayList<User>();
		schoollist = new ArrayList<School>();
		classlist = new ArrayList<Classunit>();

		questioncount = new MutableLong(0);
		feedbackcount = new MutableLong(0);
		usercount = new MutableLong(0);
		schoolcount = new MutableLong(0);
		classcount = new MutableLong(0);
	} 

	public void onGet(){
		if(param("q")){
			String q = getParamValue("q");
			String qtype = Post.Question.class.getSimpleName();
			String ftype = Post.Feedback.class.getSimpleName();

			if ("questions".equals(showParam)) {
				questionlist = search.findByKeyword(Post.class, qtype, pagenum, questioncount, q);
			} else if("feedback".equals(showParam)) {
				feedbacklist = search.findByKeyword(Post.class, ftype, pagenum, feedbackcount, q);
			} else if("people".equals(showParam)) {
				userlist = search.findByKeyword(User.class, pagenum, usercount, q);
			} else if("schools".equals(showParam)) {
				schoollist = search.findByKeyword(School.class, pagenum, schoolcount, q);
			} else if("classes".equals(showParam)) {
				classlist = search.findByKeyword(Classunit.class, pagenum, classcount, q);
			} else {
				questionlist = search.findByKeyword(Post.class, qtype, pagenum, questioncount, q, max);
				feedbacklist = search.findByKeyword(Post.class, ftype, pagenum, feedbackcount, q, max);
				userlist = search.findByKeyword(User.class, null, pagenum, usercount, q, max);
				schoollist = search.findByKeyword(School.class, null, pagenum, schoolcount, q, max);
				classlist = search.findByKeyword(Classunit.class, null, pagenum, classcount, q, max);

				addModel("totalCount", (questioncount.longValue() + feedbackcount.longValue() +
						usercount.longValue() + schoolcount.longValue() + classcount.longValue()));
			}

		}
	}
}
