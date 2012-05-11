/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import com.scoold.core.Post;
import com.scoold.core.Post.PostType;
import com.scoold.core.Report;
import com.scoold.core.School;
import com.scoold.core.Group;
import com.scoold.core.User.Badge;
import com.scoold.db.AbstractDAOUtils;
import java.util.ArrayList;
import java.util.Map;
import org.apache.click.control.Form;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 *
 * @author alexb
 */
public class Questions extends BasePage{

	public String title;
	public ArrayList<Post> questionslist;
	public Map<Long, ?> askablesMap;
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
					qForm = getQuestionForm(School.class, 
						NumberUtils.toLong(getParamValue("schoolid"), 0), 
						authUser.getSchoolsMap());
				}else if(param("groupid")){
					qForm = getQuestionForm(Group.class, 
						NumberUtils.toLong(getParamValue("groupid"), 0), 
						authUser.getGroupsMap());
				}else{
					qForm = getQuestionForm(School.class, null, authUser.getSchoolsMap());
				}
			}
		}
	}

	public void onGet() {
		PostType type = param("groupid") ? PostType.GROUPPOST : PostType.QUESTION;
		if(!param("ask")) {
			if (param("tag")) {
				String tag = getParamValue("tag");
				ArrayList<String> tags = new ArrayList<String>();
				tags.add(tag);
				questionslist = daoutils.readAndRepair(Post.class, daoutils.findTagged(
					type.toString(), pagenum, itemcount, tags), itemcount);
			}else if(param("schoolid")) {
				Long sid = NumberUtils.toLong(getParamValue("schoolid"));
				School school = School.getSchoolDao().read(sid);
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
				processVoteRequest(getParamValue("classname"), NumberUtils.toLong(getParamValue("id")));

				//serve AJAX requests for each photo (comments)
				String refUrl = req.getHeader("Referer");
				if(!isAjaxRequest() && !StringUtils.isBlank(refUrl)){
					setRedirect(refUrl);
					return;
				}
			} else {
				if("activity".equals(getParamValue("sortby"))){
					questionslist = daoutils.readAndRepair(Post.class, daoutils.findQuery(
						type.toString(), pagenum, itemcount, "*", "lastactivity", 
						true, MAX_ITEMS_PER_PAGE), itemcount);
				} else if ("votes".equals(getParamValue("sortby"))){
					questionslist = daoutils.readAndRepair(Post.class, daoutils.findQuery(
						type.toString(), pagenum, itemcount, "*", "votes", 
						true, MAX_ITEMS_PER_PAGE), itemcount);
				}else if("unanswered".equals(getParamValue("sortby"))){
					questionslist = daoutils.readAndRepair(Post.class, daoutils.findTerm(
						type.toString(), pagenum, itemcount, "answercount", 0L), itemcount);
				}else{
					if(authenticated && authUser.hasFavtags()){
						questionslist = daoutils.readAndRepair(Post.class, daoutils.findTagged(
							type.toString(), pagenum, itemcount, authUser.getFavtagsList()), itemcount);
					}else{
						questionslist = daoutils.readAndRepair(Post.class, daoutils.findQuery(
							type.toString(), pagenum, itemcount, "*"), itemcount);
					}
				}
			}
			addModel("tagFilterOn", authenticated && authUser.hasFavtags());
		}
	}

	public void onPost(){
		if(param("report")){
			Report rep = new Report();
			AbstractDAOUtils.populate(rep, req.getParameterMap());
			if(authenticated){
				rep.setAuthor(authUser.getFullname());
				rep.setUserid(authUser.getId());

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
			createAndGoToPost(param("groupid") ? PostType.GROUPPOST : PostType.QUESTION);
		}
		return false;
	}
}
