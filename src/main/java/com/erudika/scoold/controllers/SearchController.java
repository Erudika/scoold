/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.SEARCHLINK;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Feedback;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.Sticky;
import com.erudika.scoold.utils.ScooldUtils;
import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpHeaders;
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
	private static final ScooldConfig CONF = ScooldUtils.getConfig();

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
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + SEARCHLINK + "?q=" + Optional.ofNullable(query).orElse("*");
		}
		List<Profile> userlist = new ArrayList<Profile>();
		List<Post> questionslist = new ArrayList<Post>();
		List<Post> answerslist = new ArrayList<Post>();
		List<Post> feedbacklist = new ArrayList<Post>();
		List<Post> commentslist = new ArrayList<Post>();
		Pager itemcount = utils.getPager("page", req);
		String queryString = StringUtils.trimToEmpty(StringUtils.isBlank(q) ? query : q);
		// [space query filter] + original query string
		String qs = utils.sanitizeQueryString(queryString, req);
		boolean usersPublic = CONF.usersDiscoverabilityEnabled(utils.isAdmin(utils.getAuthUser(req)));

		if ("questions".equals(type)) {
			questionslist = utils.fullQuestionsSearch(qs, itemcount);
		} else if ("answers".equals(type)) {
			answerslist = pc.findQuery(Utils.type(Reply.class), qs, itemcount);
		} else if ("feedback".equals(type) && utils.isFeedbackEnabled()) {
			feedbacklist = pc.findQuery(Utils.type(Feedback.class), queryString, itemcount);
		} else if ("people".equals(type) && usersPublic) {
			userlist = pc.findQuery(Utils.type(Profile.class), getUsersSearchQuery(queryString, req), itemcount);
		} else if ("comments".equals(type)) {
			commentslist = pc.findQuery(Utils.type(Comment.class), qs, itemcount);
		} else {
			questionslist = utils.fullQuestionsSearch(qs);
			answerslist = pc.findQuery(Utils.type(Reply.class), qs);
			if (utils.isFeedbackEnabled()) {
				feedbacklist = pc.findQuery(Utils.type(Feedback.class), queryString);
			}
			if (usersPublic) {
				userlist = pc.findQuery(Utils.type(Profile.class), getUsersSearchQuery(queryString, req));
			}
			commentslist = pc.findQuery(Utils.type(Comment.class), qs, itemcount);
		}
		ArrayList<Post> list = new ArrayList<Post>();
		list.addAll(questionslist);
		list.addAll(answerslist);
		list.addAll(feedbacklist);
		utils.getProfiles(list);

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
		model.addAttribute("commentslist", commentslist);

		triggerSearchEvent(type, qs, userlist.size() + questionslist.size() + answerslist.size() + commentslist.size(), req);
		return "base";
	}

	private String getUsersSearchQuery(String qs, HttpServletRequest req) {
		String spaceFilter = utils.sanitizeQueryString("", req).replaceAll("properties\\.space:", "properties.spaces:");
		return utils.getUsersSearchQuery(qs, spaceFilter);
	}

	@ResponseBody
	@GetMapping("/opensearch.xml")
	public ResponseEntity<String> openSearch(HttpServletRequest req) {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
				+ "<OpenSearchDescription xmlns=\"http://a9.com/-/spec/opensearch/1.1/\" "
				+ "  xmlns:moz=\"http://www.mozilla.org/2006/browser/search/\">\n"
				+ "  <ShortName>" + CONF.appName() + "</ShortName>\n"
				+ "  <Description>" + utils.getLang(req).get("search.description") + "</Description>\n"
				+ "  <InputEncoding>UTF-8</InputEncoding>\n"
				+ "  <Image width=\"16\" height=\"16\" type=\"image/x-icon\">" +
				CONF.serverUrl() + CONF.serverContextPath() + "/favicon.ico</Image>\n"
				+ "  <Url type=\"text/html\" method=\"get\" template=\"" + CONF.serverUrl() + CONF.serverContextPath()
				+ "/search?q={searchTerms}\"></Url>\n"
				+ "</OpenSearchDescription>";
		return ResponseEntity.ok().
				contentType(MediaType.APPLICATION_XML).
				cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS)).
				eTag(Utils.md5(xml)).
				body(xml);
	}

	@ResponseBody
	@GetMapping("/manifest.webmanifest")
	public ResponseEntity<String> webmanifest(HttpServletRequest req) {
		String json = "{\n"
				+ "    \"theme_color\": \"#03a9f4\",\n"
				+ "    \"background_color\": \"#FFFFFF\",\n"
				+ "    \"display\": \"minimal-ui\",\n"
				+ "    \"scope\": \"/\",\n"
				+ "    \"start_url\": \"" + CONF.serverContextPath() + "/\",\n"
				+ "    \"name\": \"" + CONF.appName() + "\",\n"
				+ "    \"description\": \"" + CONF.metaDescription() + "\",\n"
				+ "    \"short_name\": \"" + CONF.appName() + "\",\n"
				+ "    \"icons\": [\n"
				+ "        {\n"
				+ "            \"src\": \"" + CONF.logoUrl() + "\",\n"
				+ "            \"sizes\": \"any\",\n"
				+ "            \"type\": \"image/svg-xml\"\n"
				+ "        },{\n"
				+ "            \"src\": \"" + CONF.imagesLink() + "/maskable512.png\",\n"
				+ "            \"sizes\": \"512x512\",\n"
				+ "            \"purpose\": \"maskable\",\n"
				+ "            \"type\": \"image/png\"\n"
				+ "        },{\n"
				+ "            \"src\": \"" + CONF.metaAppIconUrl() + "\",\n"
				+ "            \"sizes\": \"any\",\n"
				+ "            \"type\": \"image/png\"\n"
				+ "        }\n"
				+ "    ]\n"
				+ "}";
		return ResponseEntity.ok().
				contentType(MediaType.APPLICATION_JSON).
				cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS)).
				eTag(Utils.md5(json)).
				body(json);
	}

	@ResponseBody
	@GetMapping("/feed.xml")
	public ResponseEntity<String> feed(HttpServletRequest req) {
		String feed = "";
		try {
			feed = new SyndFeedOutput().outputString(getFeed(req));
		} catch (Exception ex) {
			logger.error("Could not generate feed", ex);
		}
		return ResponseEntity.ok().
				contentType(MediaType.APPLICATION_ATOM_XML).
				cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS)).
				eTag(Utils.md5(feed)).
				body(feed);
	}

	private SyndFeed getFeed(HttpServletRequest req) throws IOException, FeedException {
		boolean canList = utils.isDefaultSpacePublic() || utils.isAuthenticated(req);
		List<Post> questions = canList ? utils.fullQuestionsSearch("*") : Collections.emptyList();
		List<SyndEntry> entries = new ArrayList<SyndEntry>();
		String baseurl = CONF.serverUrl() + CONF.serverContextPath();
		baseurl = baseurl.endsWith("/") ? baseurl : baseurl + "/";

		Map<String, String> lang = utils.getLang(req);

		SyndFeed feed = new SyndFeedImpl();
		feed.setFeedType("atom_1.0");
		feed.setTitle(Utils.formatMessage(lang.get("feed.title"), CONF.appName()));
		feed.setLink(baseurl);
		feed.setDescription(Utils.formatMessage(lang.get("feed.description"), CONF.appName()));

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

	@ResponseBody
	@GetMapping("/sitemap.xml")
	public ResponseEntity<String> sitemap(HttpServletRequest req) {
		if (!CONF.sitemapEnabled()) {
			return ResponseEntity.notFound().build();
		}
		String sitemap = "";
		try {
			sitemap = getSitemap(req);
		} catch (Exception ex) {
			logger.error("Could not generate sitemap", ex);
		}
		return ResponseEntity.ok().
				contentType(MediaType.APPLICATION_XML).
				cacheControl(CacheControl.maxAge(6, TimeUnit.HOURS)).
				eTag(Utils.md5(sitemap)).
				body(sitemap);
	}

	private String getSitemap(HttpServletRequest req) throws IOException, FeedException {
		boolean canList = utils.isDefaultSpacePublic() || utils.isAuthenticated(req);
		if (canList) {
			List<Post> questions = new LinkedList<>();
			pc.readEverything(pager -> {
				pager.setLimit(100);
				List<Post> results = pc.findQuery("", Config._TYPE + ":(" + String.join(" OR ",
						Utils.type(Question.class), Utils.type(Sticky.class)) + ")", pager);
				questions.addAll(results);
				return results;
			});
			logger.debug("Found {} questions while generating sitemap.", questions.size());
			if (!questions.isEmpty()) {
				String baseurl = CONF.serverUrl() + CONF.serverContextPath();
				WebSitemapGenerator generator = new WebSitemapGenerator(baseurl);
				for (Post post : questions) {
					String baselink = baseurl.concat(post.getPostLink(false, false));
					generator.addUrl(new WebSitemapUrl.Options(baselink).lastMod(new Date(post.getTimestamp())).build());
				}
				return generator.writeAsStrings().get(0);
			}
		}
		logger.debug("Sitemap generation skipped - public={} auth={}", utils.isDefaultSpacePublic(), utils.isAuthenticated(req));
		return "<_/>";
	}

	private void triggerSearchEvent(String type, String query, int results, HttpServletRequest req) {
		if (req != null) {
			Profile authUser = utils.getAuthUser(req);
			Map<String, Object> payload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(authUser, false));
			if (authUser != null) {
				payload.put("visitor", ParaObjectUtils.getAnnotatedFields(authUser, false));
			} else {
				payload.put("visitor", Collections.emptyMap());
			}
			Map<String, String> headers = new HashMap<>();
			headers.put(HttpHeaders.REFERER, req.getHeader(HttpHeaders.REFERER));
			headers.put(HttpHeaders.USER_AGENT, req.getHeader(HttpHeaders.USER_AGENT));
			headers.put("User-IP", req.getRemoteAddr());
			payload.put("headers", headers);
			payload.put("category", type);
			payload.put("query", query);
			payload.put("results", results);
			utils.triggerHookEvent("user.search", payload);
		}
	}
}
