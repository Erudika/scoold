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

import com.erudika.para.utils.Pager;
import static com.erudika.scoold.ScooldServer.questionslink;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Revision;
import com.erudika.scoold.utils.ScooldUtils;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/revisions")
public class RevisionsController {

	private final ScooldUtils utils;

	@Inject
	public RevisionsController(ScooldUtils utils) {
		this.utils = utils;
	}

	@GetMapping("/{postid}")
    public String get(@PathVariable String postid, HttpServletRequest req, Model model) {
		Post showPost = utils.getParaClient().read(postid);
		if (showPost == null) {
			return "redirect:" + questionslink;
		}
		Pager itemcount = utils.getPager("page", req);
		List<Revision> revisionslist = showPost.getRevisions(itemcount);
		model.addAttribute("path", "revisions.vm");
		model.addAttribute("title", utils.getLang(req).get("revisions.title"));
		model.addAttribute("showPost", showPost);
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("revisionslist", revisionslist);
        return "base";
    }
}