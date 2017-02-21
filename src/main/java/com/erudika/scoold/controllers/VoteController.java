package com.erudika.scoold.controllers;

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

import com.erudika.scoold.utils.ScooldUtils;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class VoteController {

	private final ScooldUtils utils;

	@Inject
	public VoteController(ScooldUtils utils) {
		this.utils = utils;
	}

	@ResponseBody
	@GetMapping(path = "/voteup/{type}/{id}")
	public Boolean voteup(@PathVariable String type, @PathVariable String id, HttpServletRequest req) {
		//addModel("voteresult", result);
		return utils.processVoteRequest(true, type, id, req);
	}

	@ResponseBody
	@GetMapping(path = "/votedown/{type}/{id}")
	public Boolean votedown(@PathVariable String type, @PathVariable String id, HttpServletRequest req) {
		//addModel("voteresult", result);
		return utils.processVoteRequest(false, type, id, req);
	}
}
