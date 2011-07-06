/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import com.scoold.core.Post;
import com.scoold.core.Post.PostType;
import com.scoold.core.Report;
import com.scoold.core.School;
import com.scoold.core.User.Badge;
import com.scoold.core.Votable;
import com.scoold.db.AbstractDAOUtils;
import com.scoold.db.AbstractPostDAO;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
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
	public Map<Long, School> schoolsMap;
	public Form qForm;

	public Questions() {
		title = lang.get("home.title");
		pageMacroCode = "#questionspage($questionslist)";
		addModel("questionsSelected", "navbtn-hover");
		
		if(param("ask")){
			if(!authenticated){
				setRedirect(questionslink+"/ask");
			}else{
				title = lang.get("questions.title") + " - " + lang.get("posts.ask");
				schoolsMap = authUser.getSchoolsMap();
				qForm = getQuestionForm(null, schoolsMap);
			}
		}
	}

	public void onGet() {
		if (param("tag")) {
			String tag = getParamValue("tag");
			ArrayList<String> tags = new ArrayList<String>();
			tags.add(tag);
			questionslist = search.findPostsForTags(PostType.QUESTION,
					tags, pagenum, itemcount);
		}else if(param("schoolid")) {
			Long sid = NumberUtils.toLong(getParamValue("schoolid"));
			School school = School.getSchoolDao().read(sid);
			if(school != null){
				String sortBy = "timestamp";
				if("activity".equals(getParamValue("sortby"))) sortBy = "lastactivity";
				else if("votes".equals(getParamValue("sortby"))) sortBy = "votes";
				questionslist = school.getQuestions(sortBy, pagenum, itemcount);
			}else{
				setRedirect(questionslink);
				return;
			}
		}else if(param("voteup") || param("votedown")){
			String classname = getParamValue("classname");
			String uuid = getParamValue("uuid");
			Votable<Long> votable = getVotable(classname, uuid);

			boolean voteresult = processVoteRequest(votable, classname, uuid);
			addModel("voteresult", voteresult);

			//serve AJAX requests for each photo (comments)
			String refUrl = req.getHeader("Referer");
			if(!isAjaxRequest() && !StringUtils.isBlank(refUrl)){
				setRedirect(refUrl);
				return;
			}
		} else {
			AbstractPostDAO<Post, Long> dao = Post.getPostDao();

			if("activity".equals(getParamValue("sortby"))){
				questionslist = dao.readAllSortedBy("lastactivity", pagenum, itemcount, true);
			} else if ("votes".equals(getParamValue("sortby"))){
				questionslist = dao.readAllSortedBy("votes", pagenum, itemcount, true);
			}else if("unanswered".equals(getParamValue("sortby"))){
				questionslist = search.findUnansweredQuestions(pagenum, itemcount);
			}else{
				if(authenticated && authUser.hasFavtags()){
					questionslist = search.findPostsForTags(PostType.QUESTION,
							authUser.getFavtagsList(), pagenum, itemcount);
					addModel("tagfilteron", true);
				}else{
					questionslist = dao.readAllSortedBy("timestamp", pagenum, itemcount, true);
					addModel("tagFilterOn", false);
				}
			}
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

	private Votable<Long> getVotable(String classname, String uuid){
		if(StringUtils.isBlank(uuid) || StringUtils.isBlank(classname)) return null;

		Votable<Long> votable = null;
		try {
			votable = (Votable<Long>)
					AbstractDAOUtils.getDaoInstance(classname).read(uuid);
		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		return votable;
	}
	
	public boolean onAskClick(){
		if(isValidQuestionForm(qForm)){
			createAndGoToPost(PostType.QUESTION);
		}
		return false;
	}

	public boolean onSecurityCheck() {
		if(qForm == null) return true;
        return qForm.onSubmitCheck(this, questionslink+"/?code=7&error=true");
    }
}
