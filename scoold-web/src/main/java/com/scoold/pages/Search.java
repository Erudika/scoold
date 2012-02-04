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
	
	public MutableLong questionpage;
	public MutableLong feedbackpage;
	public MutableLong userpage;
	public MutableLong schoolpage;
	public MutableLong classpage;
	
	public int totalCount;

	private int max = 10;

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
		
		questionpage = new MutableLong(0);
		feedbackpage = new MutableLong(0);
		userpage = new MutableLong(0);
		schoolpage = new MutableLong(0);
		classpage = new MutableLong(0);
		
		totalCount = 0; 
	} 

	public void onGet(){
		if(param("q")){
			String q = getParamValue("q");

			if (showParam != null) {
				if ("questions".equals(showParam)) {
					questionlist = search.findByKeyword(Post.class, pagenum, itemcount, q);
				} else if("feedback".equals(showParam)) {
					feedbacklist = search.findByKeyword(Post.class, pagenum, itemcount, q);
				} else if("people".equals(showParam)) {
					userlist = search.findByKeyword(User.class, pagenum, itemcount, q);
				} else if("schools".equals(showParam)) {
					schoollist = search.findByKeyword(School.class, pagenum, itemcount, q);
				} else if("classes".equals(showParam)) {
					classlist = search.findByKeyword(Classunit.class, pagenum, itemcount, q);
				}
				totalCount = itemcount.intValue();
			} else { 
				questionlist = search.findByKeyword(Post.class, questionpage, questioncount, q, max);
				feedbacklist = search.findByKeyword(Post.class, feedbackpage, feedbackcount, q, max);
				userlist = search.findByKeyword(User.class, userpage, usercount, q, max);
				schoollist = search.findByKeyword(School.class, schoolpage, schoolcount, q, max);
				classlist = search.findByKeyword(Classunit.class, classpage, classcount, q, max);
				totalCount = (questioncount.intValue() + feedbackcount.intValue() +
						usercount.intValue() + schoolcount.intValue() + classcount.intValue());
			}
			
		}
	}
}
