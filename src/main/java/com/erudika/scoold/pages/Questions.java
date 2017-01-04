/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.core.School;
import com.erudika.scoold.core.Group;
import com.erudika.scoold.core.Grouppost;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.ScooldUser.Badge;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.click.control.Form;
import org.apache.click.util.ClickUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Questions extends Base{

	public String title;
	public List<Question> questionslist;
	public Map<String, ?> askablesMap;
	public Form qForm;

	public Questions() {
		title = lang.get("home.title");
		addModel("questionsSelected", "navbtn-hover");

		if (param("ask") && !isAjaxRequest()) {
			if (!authenticated) {
				setRedirect(signinlink);
			} else {
				title = lang.get("questions.title") + " - " + lang.get("posts.ask");
				if (param("schoolid")) {
					qForm = getQuestionForm(School.class, getParamValue("schoolid"), authUser.getSchoolsMap());
				} else if (param("groupid")) {
					qForm = getQuestionForm(Group.class, getParamValue("groupid"), authUser.getGroupsMap());
				} else {
					qForm = getQuestionForm(School.class, null, authUser.getSchoolsMap());
				}
			}
		}
	}

	public void onGet() {
		String type = Utils.type(param("groupid") ? Grouppost.class : Question.class);
		if (!param("ask")) {
			if (param("tag")) {
				String tag = getParamValue("tag");
				questionslist = pc.findTagged(type, new String[]{tag}, itemcount);
			} else if (param("schoolid")) {
				String sid = getParamValue("schoolid");
				School school = pc.read(sid);
				if (school != null) {
					String sortBy = "";
					if ("activity".equals(getParamValue("sortby"))) sortBy = "lastactivity";
					else if ("votes".equals(getParamValue("sortby"))) sortBy = "votes";
					itemcount.setSortby(sortBy);
					questionslist = school.getQuestions(itemcount);
				} else {
					setRedirect(questionslink);
					return;
				}
			} else if (param("voteup") || param("votedown")) {
				processVoteRequest(getParamValue("type"), getParamValue("id"));

				//serve AJAX requests for each photo (comments)
				String refUrl = req.getHeader("Referer");
				if (!isAjaxRequest() && !StringUtils.isBlank(refUrl)) {
					setRedirect(refUrl);
					return;
				}
			} else {
				if ("activity".equals(getParamValue("sortby"))) {
					itemcount.setSortby("lastactivity");
					questionslist = pc.findQuery(type, "*", itemcount);
				} else if ("votes".equals(getParamValue("sortby"))) {
					itemcount.setSortby("votes");
					questionslist = pc.findQuery(type, "*", itemcount);
				} else if ("unanswered".equals(getParamValue("sortby"))) {
					itemcount.setSortby("answercount");
					questionslist = pc.findTerms(type, Collections.singletonMap("answercount", 0L), true, itemcount);
				} else {
					if (authenticated && authUser.hasFavtags()) {
						questionslist = pc.findTermInList(type,
								Config._TAGS, new ArrayList<String>(authUser.getFavtags()), itemcount);
					} else {
						questionslist = pc.findQuery(type, "*", itemcount);
					}
				}
			}
			addModel("tagFilterOn", authenticated && authUser.hasFavtags());
		}

		if (param("mobile")) {
			String m = getParamValue("mobile");
			if ("true".equals(m)) {
				setStateParam(MOBILE_COOKIE, "true");
				ClickUtils.setCookie(req, resp, MOBILE_COOKIE, "true", (int) (Config.SESSION_TIMEOUT_SEC * 365), "/");
			} else {
				ClickUtils.setCookie(req, resp, MOBILE_COOKIE, "", 0, "/");
			}
			if (!isAjaxRequest())
				setRedirect(HOMEPAGE);
		}
	}

	public void onPost() {
		if (param("report")) {
			Report rep = new Report();
			ParaObjectUtils.populate(rep, req.getParameterMap());
			if (authenticated) {
				rep.setAuthor(authUser.getName());
				rep.setCreatorid(authUser.getId());

				addBadge(Badge.REPORTER, true);
			} else {
				//allow anonymous reports
				rep.setAuthor(lang.get("anonymous"));
			}
			rep.create();
		}
	}

	public boolean onAskClick() {
		if (isValidQuestionForm(qForm)) {
			createAndGoToPost(param("groupid") ? Grouppost.class : Question.class);
		}
		return false;
	}
}
