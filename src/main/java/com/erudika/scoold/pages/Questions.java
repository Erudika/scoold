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

import com.erudika.para.utils.Config;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.utils.AppConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Questions extends Base{

	public String title;
	public List<Question> questionslist;
	public int MAX_TEXT_LENGTH = AppConfig.MAX_TEXT_LENGTH;
	public Map<String, String> error = Collections.emptyMap();

	public Questions() {
		title = lang.get("home.title");

		if (param("ask") && !isAjaxRequest()) {
			if (!authenticated) {
				setRedirect(signinlink);
			} else {
				addModel("askSelected", "navbtn-hover");
				title = lang.get("questions.title") + " - " + lang.get("posts.ask");
			}
		} else {
			addModel("questionsSelected", "navbtn-hover");
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
		Question q = populate(new Question(), "title", "body", "tags|,");
		error = validate(q);
		if (error.isEmpty()) {
			createAndGoToPost(q);
		}
	}

}
