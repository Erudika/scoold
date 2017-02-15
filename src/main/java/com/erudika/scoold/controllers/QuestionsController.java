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

package com.erudika.scoold.controllers;

import com.erudika.scoold.core.Question;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class QuestionsController {

	public String title;
	public List<Question> questionslist;
	public Map<String, String> error = Collections.emptyMap();

//	public QuestionsController() {
//		title = lang.get("home.title");
//
//		if (param("ask") && !isAjaxRequest()) {
//			if (!authenticated) {
//				setRedirect(signinlink);
//			} else {
//				addModel("askSelected", "navbtn-hover");
//				addModel("includeGMapsScripts", true);
//				title = lang.get("questions.title") + " - " + lang.get("posts.ask");
//			}
//		} else {
//			addModel("questionsSelected", "navbtn-hover");
//		}
//	}
//
//	public void onGet() {
//		String type = Utils.type(Question.class);
//		if (!param("ask")) {
//			if (param("tag")) {
//				String tag = getParamValue("tag");
//				questionslist = pc.findTagged(type, new String[]{tag}, itemcount);
//			} else if (param("voteup") || param("votedown")) {
//				processVoteRequest(getParamValue("type"), getParamValue("id"));
//
//				//serve AJAX requests for each photo (comments)
//				String refUrl = req.getHeader("Referer");
//				if (!isAjaxRequest() && !StringUtils.isBlank(refUrl)) {
//					setRedirect(refUrl);
//					return;
//				}
//			} else {
//				if ("activity".equals(getParamValue("sortby"))) {
//					itemcount.setSortby("updated");
//					questionslist = pc.findQuery(type, "*", itemcount);
//				} else if ("votes".equals(getParamValue("sortby"))) {
//					itemcount.setSortby("votes");
//					questionslist = pc.findQuery(type, "*", itemcount);
//				} else if ("unanswered".equals(getParamValue("sortby"))) {
//					itemcount.setSortby("timestamp");
//					itemcount.setDesc(false);
//					questionslist = pc.findQuery(type, "properties.answercount:0", itemcount);
//				} else if ("filter".equals(getParamValue("sortby")) && authenticated) {
//					if ("favtags".equals(getParamValue("filter")) && authUser.hasFavtags()) {
//						addModel("tagFilterOn", true);
//						questionslist = pc.findTermInList(type,
//								Config._TAGS, new ArrayList<String>(authUser.getFavtags()), itemcount);
//					} else if (!StringUtils.isBlank(authUser.getLatlng())) {
//						addModel("localFilterOn", true);
//						String[] ll = authUser.getLatlng().split(",");
//						if (ll.length == 2) {
//							double lat = NumberUtils.toDouble(ll[0]);
//							double lng = NumberUtils.toDouble(ll[1]);
//							questionslist = pc.findNearby(type, "*", 25, lat, lng, itemcount);
//						}
//					}
//				} else {
//					questionslist = pc.findQuery(type, "*", itemcount);
//				}
//			}
//			fetchProfiles(questionslist);
//		}
//	}
//
//	public void onPost() {
//		Question q = populate(new Question(), "title", "body", "tags|,", "location");
//		q.setCreatorid(authUser.getId());
//		error = validate(q);
//		if (error.isEmpty()) {
//			q.setLocation(getParamValue("location"));
//			String qid = q.create();
//			String latlng = getParamValue("latlng");
//			if (!StringUtils.isBlank(latlng)) {
//				Address addr = new Address();
//				addr.setAddress(getParamValue("address"));
//				addr.setCountry(getParamValue("location"));
//				addr.setLatlng(latlng);
//				addr.setParentid(qid);
//				addr.setCreatorid(authUser.getId());
//				pc.create(addr);
//			}
//			authUser.setLastseen(System.currentTimeMillis());
//			setRedirect(getPostLink(q, false, false));
//		}
//	}

}
