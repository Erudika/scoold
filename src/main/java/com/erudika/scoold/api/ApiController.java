/*
 * Copyright 2013-2019 Erudika. https://erudika.com
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
package com.erudika.scoold.api;

import com.erudika.para.client.ParaClient;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.ScooldServer;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.UnapprovedQuestion;
import com.erudika.scoold.utils.ScooldUtils;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * WORK IN PROGRESS.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@RestController
@RequestMapping("/api")
public class ApiController {

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public ApiController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping
	public Map<String, Object> get(HttpServletRequest req, HttpServletResponse res) {
		Map<String, Object> intro = new HashMap<>();
		intro.put("message", Config.APP_NAME + " API, see docs at " + ScooldServer.getServerURL() + "/api.html");
		boolean healthy;
		try {
			healthy = pc != null && pc.getTimestamp() > 0;
		} catch (Exception e) {
			healthy = false;
		}
		intro.put("healthy", healthy);
		if (!healthy) {
			res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		return intro;
	}


	@RequestMapping(value = "/questions", method = RequestMethod.GET)
	public List<Question> getQuestions(@RequestParam(defaultValue = "*") String q, HttpServletRequest req) {
		Pager pager = utils.pagerFromParams(req);
		List<Question> questionslist = pc.findQuery(Utils.type(Question.class), q, pager);
		if (utils.postsNeedApproval()) {
			Pager p = new Pager(pager.getPage(), pager.getLimit());
			List<UnapprovedQuestion> uquestionslist = pc.findQuery(Utils.type(UnapprovedQuestion.class), q, p);
			List<Question> qlist = new LinkedList<>(uquestionslist);
			pager.setCount(pager.getCount() + p.getCount());
			qlist.addAll(questionslist);
			questionslist = qlist;
		}

		utils.fetchProfiles(questionslist);
		return questionslist;
	}

}
