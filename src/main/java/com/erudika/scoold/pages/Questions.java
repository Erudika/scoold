/*
 * Copyright 2013-2017 Erudika. https://erudika.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For issues and patches go to: https://github.com/erudika
 */

package com.erudika.scoold.pages;

import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.ScooldUser.Badge;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.click.control.Form;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Questions extends Base{

	public String title;
	public List<Question> questionslist;
	public Form qForm;

	public Questions() {
		title = lang.get("home.title");
		addModel("questionsSelected", "navbtn-hover");

		if (param("ask") && !isAjaxRequest()) {
			if (!authenticated) {
				setRedirect(signinlink);
			} else {
				title = lang.get("questions.title") + " - " + lang.get("posts.ask");
				qForm = getQuestionForm();
			}
		}
	}

	public void onGet() {
		String type = Utils.type(Question.class);
		if (!param("ask")) {
			if (param("tag")) {
				String tag = getParamValue("tag");
				questionslist = pc.findTagged(type, new String[]{tag}, itemcount);
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
			createAndGoToPost(Question.class);
		}
		return false;
	}
}
