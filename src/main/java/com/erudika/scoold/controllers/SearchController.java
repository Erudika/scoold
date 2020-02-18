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
package com.erudika.scoold.controllers;

import com.erudika.para.client.ParaClient;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.ScooldServer;
import static com.erudika.scoold.ScooldServer.CONTEXT_PATH;
import com.erudika.scoold.core.Feedback;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.utils.ScooldUtils;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class SearchController {

	private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public SearchController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping({"/search/{type}/{query}", "/search"})
	public String get(@PathVariable(required = false) String type, @PathVariable(required = false) String query,
			@RequestParam(required = false) String q, HttpServletRequest req, Model model) {
		List<Profile> userlist = new ArrayList<Profile>();
		List<Post> questionslist = new ArrayList<Post>();
		List<Post> answerslist = new ArrayList<Post>();
		List<Post> feedbacklist = new ArrayList<Post>();
		Pager itemcount = utils.getPager("page", req);
		String queryString = StringUtils.isBlank(q) ? query : q;
		// [space query filter] + original query string
		String qs = utils.sanitizeQueryString(queryString, req);
		String qsUsers = qs.replaceAll("properties\\.space:", "properties.spaces:");

		if ("questions".equals(type)) {
			questionslist = pc.findQuery(Utils.type(Question.class), qs, itemcount);
		} else if ("answers".equals(type)) {
			answerslist = pc.findQuery(Utils.type(Reply.class), qs, itemcount);
		} else if ("feedback".equals(type)) {
			feedbacklist = pc.findQuery(Utils.type(Feedback.class), queryString, itemcount);
		} else if ("people".equals(type)) {
			userlist = pc.findQuery(Utils.type(Profile.class), qsUsers, itemcount);
		} else {
			questionslist = pc.findQuery(Utils.type(Question.class), qs);
			answerslist = pc.findQuery(Utils.type(Reply.class), qs);
			feedbacklist = pc.findQuery(Utils.type(Feedback.class), queryString);
			userlist = pc.findQuery(Utils.type(Profile.class), qsUsers);
		}
		ArrayList<Post> list = new ArrayList<Post>();
		list.addAll(questionslist);
		list.addAll(answerslist);
		list.addAll(feedbacklist);
		utils.fetchProfiles(list);

		model.addAttribute("path", "search.vm");
		model.addAttribute("title", utils.getLang(req).get("search.title"));
		model.addAttribute("searchSelected", "navbtn-hover");
		model.addAttribute("showParam", type);
		model.addAttribute("searchQuery", queryString);
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("userlist", userlist);
		model.addAttribute("questionslist", questionslist);
		model.addAttribute("answerslist", answerslist);
		model.addAttribute("feedbacklist", feedbacklist);

		return "base";
	}

	@ResponseBody
	@GetMapping("/opensearch.xml")
	public ResponseEntity<String> openSearch() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
				+ "<OpenSearchDescription xmlns=\"http://a9.com/-/spec/opensearch/1.1/\" "
				+ "  xmlns:moz=\"http://www.mozilla.org/2006/browser/search/\">\n"
				+ "  <ShortName>" + Config.APP_NAME + "</ShortName>\n"
				+ "  <Description>Search for questions and answers</Description>\n"
				+ "  <InputEncoding>UTF-8</InputEncoding>\n"
				+ "  <Image width=\"16\" height=\"16\" type=\"image/x-icon\">https://scoold.com/favicon.ico</Image>\n"
				+ "  <Url type=\"text/html\" method=\"get\" template=\"" + ScooldServer.getServerURL() + CONTEXT_PATH
				+ "/search?q={searchTerms}\"></Url>\n"
				+ "</OpenSearchDescription>";
		return ResponseEntity.ok().
				contentType(MediaType.APPLICATION_XML).
				cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS)).
				eTag(Utils.md5(xml)).
				body(xml);
	}

	@ResponseBody
	@GetMapping("/feed.xml")
	public ResponseEntity<String> feed() {
		String feed = "";
		try {
			feed = new SyndFeedOutput().outputString(getFeed());
		} catch (Exception ex) {
			logger.error("Could not generate feed", ex);
		}
		return ResponseEntity.ok().
				contentType(MediaType.APPLICATION_ATOM_XML).
				cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS)).
				eTag(Utils.md5(feed)).
				body(feed);
	}

	private SyndFeed getFeed() throws IOException, FeedException {
		List<Post> questions = pc.findQuery(Utils.type(Question.class), "*");
		List<SyndEntry> entries = new ArrayList<SyndEntry>();
		String baseurl = ScooldServer.getServerURL();
		baseurl = baseurl.endsWith("/") ? baseurl : baseurl + "/";

		SyndFeed feed = new SyndFeedImpl();
		feed.setFeedType("atom_1.0");
		feed.setTitle(Config.APP_NAME + " - Recent questions");
		feed.setLink(baseurl);
		feed.setDescription("A summary of the most recent questions on " + Config.APP_NAME);

		for (Post post : questions) {
			SyndEntry entry;
			SyndContent description;
			String baselink = baseurl.concat("question/").concat(post.getId());

			entry = new SyndEntryImpl();
			entry.setTitle(post.getTitle());
			entry.setLink(baselink);
			entry.setPublishedDate(new Date(post.getTimestamp()));
			entry.setAuthor(baseurl.concat("profile/").concat(post.getCreatorid()));
			entry.setUri(baselink.concat("/").concat(Utils.stripAndTrim(post.getTitle()).
					replaceAll("\\p{Z}+", "-").toLowerCase()));

			description = new SyndContentImpl();
			description.setType("text/html");
			description.setValue(Utils.markdownToHtml(post.getBody()));

			entry.setDescription(description);
			entries.add(entry);
		}
		feed.setEntries(entries);
		return feed;
	}
}
