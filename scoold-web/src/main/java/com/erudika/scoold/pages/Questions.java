/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.para.core.PObject;
import com.erudika.para.utils.Utils;
import com.erudika.para.utils.Search;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.core.School;
import com.erudika.scoold.core.Group;
import com.erudika.scoold.core.Grouppost;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.User.Badge;
import java.util.ArrayList;
import java.util.Map;
import org.apache.click.control.Form;
import org.apache.click.util.ClickUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author alexb
 */
public class Questions extends BasePage{

	public String title;
	public ArrayList<Post> questionslist;
	public Map<String, ?> askablesMap;
	public Form qForm;

	public Questions() {
		title = lang.get("home.title");
		addModel("questionsSelected", "navbtn-hover");
		
		if(param("ask") && !isAjaxRequest()){
			if(!authenticated){
				setRedirect(questionslink+"/ask");
			}else{
				title = lang.get("questions.title") + " - " + lang.get("posts.ask");
				if(param("schoolid")){
					qForm = getQuestionForm(School.class, getParamValue("schoolid"), authUser.getSchoolsMap());
				}else if(param("groupid")){
					qForm = getQuestionForm(Group.class, getParamValue("groupid"), authUser.getGroupsMap());
				}else{
					qForm = getQuestionForm(School.class, null, authUser.getSchoolsMap());
				}
			}
		}
	}

	public void onGet() {
		String type = PObject.classname(param("groupid") ? Grouppost.class : Question.class);
		if(!param("ask")) {
			if (param("tag")) {
				String tag = getParamValue("tag");
				ArrayList<String> tags = new ArrayList<String>();
				tags.add(tag);
				questionslist = Search.findTagged(type, pagenum, itemcount, tags);
			}else if(param("schoolid")) {
				String sid = getParamValue("schoolid");
				School school = dao.read(sid);
				if(school != null){
					String sortBy = "";
					if("activity".equals(getParamValue("sortby"))) sortBy = "lastactivity";
					else if("votes".equals(getParamValue("sortby"))) sortBy = "votes";
					questionslist = school.getQuestions(sortBy, pagenum, itemcount);
				}else{
					setRedirect(questionslink);
					return;
				}
			}else if(param("voteup") || param("votedown")){
				processVoteRequest(getParamValue("classname"), getParamValue("id"));

				//serve AJAX requests for each photo (comments)
				String refUrl = req.getHeader("Referer");
				if(!isAjaxRequest() && !StringUtils.isBlank(refUrl)){
					setRedirect(refUrl);
					return;
				}
			} else {
				if("activity".equals(getParamValue("sortby"))){
					questionslist = Search.findQuery(type, pagenum, itemcount, "*", "lastactivity", true, MAX_ITEMS_PER_PAGE);
				} else if ("votes".equals(getParamValue("sortby"))){
					questionslist = Search.findQuery(type, pagenum, itemcount, "*", "votes", true, MAX_ITEMS_PER_PAGE);
				}else if("unanswered".equals(getParamValue("sortby"))){
					questionslist = Search.findTerm(type, pagenum, itemcount, "answercount", 0L);
				}else{
					if(authenticated && authUser.hasFavtags()){
						questionslist = Search.findTagged(type, pagenum, itemcount, authUser.getFavtagsList());
					}else{
						questionslist = Search.findQuery(type, pagenum, itemcount, "*");
					}
				}
			}
			addModel("tagFilterOn", authenticated && authUser.hasFavtags());
		}
		
		if(param("mobile")){
			String m = getParamValue("mobile");
			if ("true".equals(m)) {
				setStateParam(MOBILE_COOKIE, "true");
				ClickUtils.setCookie(req, resp, MOBILE_COOKIE, "true", 
						Utils.SESSION_TIMEOUT_SEC * 365, "/");
			} else {
				ClickUtils.setCookie(req, resp, MOBILE_COOKIE, "", 0, "/");
			}
			if(!isAjaxRequest()) 
				setRedirect(HOMEPAGE);
		}
	}

	public void onPost(){
		if(param("report")){
			Report rep = new Report();
			Utils.populate(rep, req.getParameterMap());
			if(authenticated){
				rep.setAuthor(authUser.getName());
				rep.setCreatorid(authUser.getId());

				addBadge(Badge.REPORTER, true);
			}else{
				//allow anonymous reports
				rep.setAuthor(lang.get("anonymous"));
			}
			rep.create();
		}
	}
	
	public boolean onAskClick(){
		if(isValidQuestionForm(qForm)){
			createAndGoToPost(param("groupid") ? Grouppost.class : Question.class);
		}
		return false;
	}
}
