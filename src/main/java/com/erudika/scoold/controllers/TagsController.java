/*
 * Copyright 2013-2021 Erudika. https://erudika.com
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

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Tag;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import static com.erudika.scoold.ScooldServer.TAGSLINK;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.utils.ScooldUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/tags")
public class TagsController {

	private static final Logger logger = LoggerFactory.getLogger(TagsController.class);

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public TagsController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping
	public String get(@RequestParam(required = false, defaultValue = "count") String sortby,
			HttpServletRequest req, Model model) {
		Pager itemcount = utils.getPager("page", req);
		itemcount.setSortby(sortby);

		Profile authUser = utils.getAuthUser(req);
		String currentSpace = utils.getSpaceIdFromCookie(authUser, req);
		List<Tag> tagslist = Collections.emptyList();
		if (utils.canAccessSpace(authUser, currentSpace)) {
			tagslist = pc.findTags("", itemcount);
		} else if (!utils.isDefaultSpacePublic()) {
			return "redirect:" + SIGNINLINK + "?returnto=" + TAGSLINK;
		}

		model.addAttribute("path", "tags.vm");
		model.addAttribute("title", utils.getLang(req).get("tags.title"));
		model.addAttribute("tagsSelected", "navbtn-hover");
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("tagslist", tagslist);
		return "base";
	}

	@PostMapping
	public String rename(@RequestParam String tag, @RequestParam String newtag, HttpServletRequest req,
			HttpServletResponse res, Model model) {
		Profile authUser = utils.getAuthUser(req);
		int count = 0;
		if (utils.isMod(authUser)) {
			Tag updated;
			Tag oldTag = new Tag(tag);
			Tag newTag = new Tag(newtag);
			Tag t = pc.read(Utils.type(Tag.class), oldTag.getId());
			if (t != null && !oldTag.getTag().equals(newTag.getTag())) {
				if (oldTag.getTag().equals(newTag.getTag())) {
					t.setCount(pc.getCount(Utils.type(Question.class),
							Collections.singletonMap(Config._TAGS, oldTag.getTag())).intValue());
					updated = pc.update(t);
				} else {
					pc.delete(t);
					t.setId(newtag);
					logger.info("User {} ({}) is renaming tag '{}' to '{}'.",
							authUser.getName(), authUser.getCreatorid(), oldTag.getTag(), t.getTag());

					t.setCount(pc.getCount(Utils.type(Question.class),
							Collections.singletonMap(Config._TAGS, newTag.getTag())).intValue());
					Pager pager = new Pager(1, "_docid", false, Config.MAX_ITEMS_PER_PAGE);
					List<Question> questionslist;
					do {
						questionslist = pc.findTagged(Utils.type(Question.class), new String[]{oldTag.getTag()}, pager);
						for (Question q : questionslist) {
							t.setCount(t.getCount() + 1);
							q.setTags(Optional.ofNullable(q.getTags()).orElse(Collections.emptyList()).stream().
									map(ts -> {
										if (ts.equals(newTag.getTag())) {
											t.setCount(t.getCount() - 1);
										}
										return ts.equals(oldTag.getTag()) ? t.getTag() : ts;
									}).distinct().
									collect(Collectors.toList()));
							logger.debug("Updated {} out of {} questions with new tag {}.",
									questionslist.size(), pager.getCount(), t.getTag());
						}
						if (!questionslist.isEmpty()) {
							pc.updateAll(questionslist);
						}
					} while (!questionslist.isEmpty());
					updated = pc.create(t); // overwrite new tag object
				}
				model.addAttribute("tag", updated);
				count = t.getCount();
			}
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			res.setContentType("application/json");
			try {
				res.getWriter().println("{\"count\":" + count + ", \"tag\":\"" + new Tag(newtag).getTag() + "\"}");
			} catch (IOException ex) { }
			return "blank";
		} else {
			return "redirect:" + TAGSLINK + "?" + req.getQueryString();
		}
	}

	@PostMapping("/delete")
	public String delete(@RequestParam String tag, HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = utils.getAuthUser(req);
		if (utils.isMod(authUser)) {
			Tag t = pc.read(Utils.type(Tag.class), new Tag(tag).getId());
			if (t != null) {
				pc.delete(t);
				logger.info("User {} ({}) deleted tag '{}'.",
						authUser.getName(), authUser.getCreatorid(), t.getTag());

				Pager pager = new Pager(1, "_docid", false, Config.MAX_ITEMS_PER_PAGE);
				List<Question> questionslist;
				do {
					questionslist = pc.findTagged(Utils.type(Question.class), new String[]{t.getTag()}, pager);
					for (Question q : questionslist) {
						t.setCount(t.getCount() + 1);
						q.setTags(Optional.ofNullable(q.getTags()).orElse(Collections.emptyList()).stream().
								filter(ts -> !ts.equals(t.getTag())).distinct().collect(Collectors.toList()));
						logger.debug("Removed tag {} from {} out of {} questions.",
								t.getTag(), questionslist.size(), pager.getCount());
					}
					if (!questionslist.isEmpty()) {
						pc.updateAll(questionslist);
					}
				} while (!questionslist.isEmpty());
			}
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "blank";
		} else {
			return "redirect:" + TAGSLINK + "?" + req.getQueryString();
		}
	}

	@ResponseBody
	@GetMapping(path = "/{keyword}", produces = MediaType.APPLICATION_JSON)
	public List<?> findTags(@PathVariable String keyword) {
		return pc.findTags(keyword, new Pager(10));
	}
}
