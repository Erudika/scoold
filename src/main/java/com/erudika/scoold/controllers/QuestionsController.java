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
import com.erudika.para.core.Address;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.Tag;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.QUESTIONSLINK;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.UnapprovedQuestion;
import com.erudika.scoold.utils.HttpUtils;
import com.erudika.scoold.utils.ScooldUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class QuestionsController {

	private static final ScooldConfig CONF = ScooldUtils.getConfig();

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	private QuestionController questionController;

	@Inject
	public QuestionsController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping({"/", "/questions"})
	public String get(@RequestParam(required = false) String sortby, HttpServletRequest req, Model model) {
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + QUESTIONSLINK;
		}
		getQuestions(sortby, null, req, model);
		model.addAttribute("path", "questions.vm");
		model.addAttribute("title", utils.getLang(req).get("questions.title"));
		model.addAttribute("questionsSelected", "navbtn-hover");
		model.addAttribute("questionsTypeFilter", HttpUtils.getCookieValue(req, "questions-type-filter"));
		return "base";
	}

	@GetMapping({"/questions/{id}", "/questions/{id}/{title}", "/questions/{id}/{title}/*"})
	public String getAlias(@PathVariable String id, @PathVariable(required = false) String title,
			@RequestParam(required = false) String sortby, HttpServletRequest req, HttpServletResponse res, Model model) {
		return questionController.get(id, title, sortby, req, res, model);
	}

	@GetMapping("/questions/tag/{tag}")
	public String getTagged(@PathVariable String tag, HttpServletRequest req, Model model) {
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + req.getRequestURI();
		}
		Pager itemcount = utils.getPager("page", req);
		List<Question> questionslist = Collections.emptyList();
		String type = Utils.type(Question.class);
		String qf = utils.getSpaceFilteredQuery(req);
		if (!qf.isEmpty()) {
			if (qf.equals("*")) {
				questionslist = pc.findTagged(type, new String[]{tag}, itemcount);
			} else {
				questionslist = pc.findQuery(type, qf + " AND " + Config._TAGS + ":" + tag + "*", itemcount);
			}
		}
		int c = (int) itemcount.getCount();
		Tag t = pc.read(new Tag(tag).getId());
		if (t != null && t.getCount() != c && utils.isMod(utils.getAuthUser(req))) {
			t.setCount(c);
			pc.update(t);
		}
		utils.getProfiles(questionslist);
		model.addAttribute("path", "questions.vm");
		model.addAttribute("title", utils.getLang(req).get("posts.tagged") + " - " + tag);
		model.addAttribute("questionsSelected", "navbtn-hover");
		model.addAttribute("tag", tag);
		model.addAttribute("tagDescription", t != null ? t.getDescription() : "");
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("questionslist", questionslist);
		return "base";
	}

	@GetMapping("/questions/similar/{like}")
	public void getSimilarAjax(@PathVariable String like, HttpServletRequest req, HttpServletResponse res) throws IOException {
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			res.setStatus(401);
			return;
		}
		Pager pager = new Pager(1, "votes", true, CONF.maxSimilarPosts());
		Profile authUser = utils.getAuthUser(req);
		StringBuilder sb = new StringBuilder();
		Question q = new Question();
		q.setTitle(like);
		q.setBody("");
		q.setTags(Arrays.asList(""));
		for (Post similarPost : utils.getSimilarPosts(q, pager)) {
			if (utils.isMod(authUser) || utils.canAccessSpace(authUser, similarPost.getSpace())) {
				boolean hasAnswer = !StringUtils.isBlank(similarPost.getAnswerid());
				sb.append("<span class=\"lightborder phm").append(hasAnswer ? " light-green white-text" : "").append("\">");
				sb.append(similarPost.getVotes());
				sb.append("</span> <a href=\"").append(similarPost.getPostLink(false, false)).append("\">");
				sb.append(HtmlUtils.htmlEscape(similarPost.getTitle())).append("</a><br>");
			}
		}
		res.setCharacterEncoding("UTF-8");
		res.getWriter().print(sb.toString());
		res.setStatus(200);
	}

	@GetMapping({"/questions/favtags", "/questions/local"})
	public String getSorted(@RequestParam(required = false) String sortby, HttpServletRequest req, Model model) {
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + req.getRequestURI();
		}
		getQuestions(sortby, req.getServletPath().endsWith("/favtags") ? "favtags" : "local", req, model);
		model.addAttribute("path", "questions.vm");
		model.addAttribute("title", utils.getLang(req).get("questions.title"));
		model.addAttribute("questionsSelected", "navbtn-hover");
		return "base";
	}

	@PostMapping("/questions/apply-filter")
	public String applyFilter(@RequestParam(required = false) String sortby,
			@RequestParam(required = false) String typeFilter, @RequestParam(required = false) String tab,
			@RequestParam(required = false, defaultValue = "false") String compactViewEnabled,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		if (req.getParameter("clear") != null) {
			HttpUtils.removeStateParam("questions-filter", req, res);
			HttpUtils.removeStateParam("questions-type-filter", req, res);
			HttpUtils.removeStateParam("questions-view-compact", req, res);
		} else {
			Pager p = utils.pagerFromParams(req);
			if (!StringUtils.isBlank(req.getParameter(Config._TAGS))) {
				boolean matchAll = "true".equals(req.getParameter("matchAllTags"));
				p.setName("with_tags:" + (matchAll ? "+" : "") + req.getParameter(Config._TAGS));
			}
			if (!StringUtils.isBlank(typeFilter)) {
				HttpUtils.setRawCookie("questions-type-filter", typeFilter,
					req, res, "Strict", (int) TimeUnit.DAYS.toSeconds(365));
			} else {
				HttpUtils.removeStateParam("questions-type-filter", req, res);
			}
			savePagerToCookie(req, res, p);
			HttpUtils.setRawCookie("questions-view-compact", compactViewEnabled,
					req, res, "Strict", (int) TimeUnit.DAYS.toSeconds(365));
		}
		return "redirect:" + QUESTIONSLINK + (StringUtils.isBlank(sortby) ? "" : "?sortby="
				+ Optional.ofNullable(StringUtils.trimToNull(sortby)).orElse(tab));
	}

	@GetMapping("/questions/ask")
	public String ask(HttpServletRequest req, Model model) {
		if (!utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + QUESTIONSLINK + "/ask";
		}
		model.addAttribute("path", "questions.vm");
		model.addAttribute("askSelected", "navbtn-hover");
		model.addAttribute("defaultTag", CONF.defaultQuestionTag());
		model.addAttribute("includeGMapsScripts", utils.isNearMeFeatureEnabled());
		model.addAttribute("includeEmojiPicker", true);
		model.addAttribute("title", utils.getLang(req).get("posts.ask"));

		Question draft = utils.populate(req, new Question(), "title", "body", "tags|,", "location", "space");
		String sid = Post.DEFAULT_SPACE;
		if (utils.isAuthenticated(req)) {
			sid = utils.getSpaceId(utils.getSpaceIdFromCookie(utils.getAuthUser(req), req));
			sid = StringUtils.replace(sid, "*", "default");
		}
		Sysprop spaceObj =  pc.read(sid);
		if (spaceObj != null) {
			String title = (String) spaceObj.getProperty("titleTemplate");
			String body = (String) spaceObj.getProperty("bodyTemplate");
			String tags = (String) spaceObj.getProperty("tagsTemplate");
			if (!StringUtils.isBlank(title)) {
				draft.setTitle(title);
			}
			if (!StringUtils.isBlank(body)) {
				draft.setBody(body);
			}
			if (!StringUtils.isBlank(tags)) {
				draft.setTags(Arrays.asList(tags.split(",")));
			}
		}
		model.addAttribute("draftQuestion", draft);
		return "base";
	}

	@PostMapping("/questions/ask")
	public String post(@RequestParam(required = false) String location, @RequestParam(required = false) String latlng,
			@RequestParam(required = false) String address, String space, String postId,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		if (utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			String currentSpace = utils.getValidSpaceIdExcludingAll(authUser, space, req);
			boolean needsApproval = utils.postsNeedApproval(req) && utils.userNeedsApproval(authUser);
			Question q = utils.populate(req, needsApproval ? new UnapprovedQuestion() : new Question(),
					"title", "body", "tags|,", "location");
			q.setCreatorid(authUser.getId());
			q.setAuthor(authUser);
			q.setSpace(currentSpace);
			if (StringUtils.isBlank(q.getTagsString())) {
				q.setTags(Arrays.asList(CONF.defaultQuestionTag().isBlank() ? "" : CONF.defaultQuestionTag()));
			}
			Map<String, String> error = utils.validateQuestionTags(q, utils.validate(q), req);
			q = handleSpam(q, authUser, error, req);
			if (error.isEmpty()) {
				String qid = StringUtils.isBlank(postId) ? Utils.getNewId() : postId;
				q.setId(qid);
				q.setLocation(location);
				q.create();
				utils.sendNewPostNotifications(q, needsApproval, req);
				if (!StringUtils.isBlank(latlng)) {
					Address addr = new Address(qid + Para.getConfig().separator() + Utils.type(Address.class));
					addr.setAddress(address);
					addr.setCountry(location);
					addr.setLatlng(latlng);
					addr.setParentid(qid);
					addr.setCreatorid(authUser.getId());
					pc.create(addr);
				}
				authUser.setLastseen(System.currentTimeMillis());
				model.addAttribute("newpost", getNewQuestionPayload(q));
			} else {
				model.addAttribute("error", error);
				model.addAttribute("draftQuestion", q);
				model.addAttribute("defaultTag", "");
				model.addAttribute("path", "questions.vm");
				model.addAttribute("includeGMapsScripts", utils.isNearMeFeatureEnabled());
				model.addAttribute("askSelected", "navbtn-hover");
				res.setStatus(400);
				return "base";
			}
			if (utils.isAjaxRequest(req)) {
				res.setStatus(200);
				res.setContentType("application/json");
				try {
					res.getWriter().println("{\"url\":\"" + q.getPostLink(false, false) + "\"}");
				} catch (IOException ex) { }
				return "blank";
			} else {
				return "redirect:" + q.getPostLinkForRedirect();
			}
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(400);
			return "blank";
		} else {
			return "redirect:" + SIGNINLINK + "?returnto=" + QUESTIONSLINK + "/ask";
		}
	}

	@GetMapping({"/questions/space/{space}", "/questions/space"})
	public String setSpace(@PathVariable(required = false) String space,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		if ("all".equals(space) || utils.isAllSpaces(space)) {
			space = Post.ALL_MY_SPACES + ":" + utils.getLang(req).get("allspaces");
		} else {
			Sysprop spaceObj = pc.read(utils.getSpaceId(space));
			if (!StringUtils.isBlank(space) && spaceObj == null) {
				Profile authUser = utils.getAuthUser(req);
				if (authUser != null && utils.canAccessSpace(authUser, space)) {
					authUser.removeSpace(space);
					authUser.update();
				}
			}
			if (spaceObj != null && !utils.isDefaultSpace(spaceObj.getId())) {
				space = spaceObj.getId().concat(Para.getConfig().separator()).concat(spaceObj.getName());
			} else {
				space = Post.DEFAULT_SPACE;
			}
		}
		utils.storeSpaceIdInCookie(space, req, res);
		String backTo = HttpUtils.getBackToUrl(req, true);
		if (!utils.isAuthenticated(req) && !(utils.isDefaultSpace(space) || utils.isAllSpaces(space))) {
			return "redirect:" + SIGNINLINK + "?returnto=" + req.getRequestURI();
		}
		if (StringUtils.isBlank(backTo) || backTo.equalsIgnoreCase(req.getRequestURI())) {
			return get(req.getParameter("sortby"), req, model);
		} else {
			return "redirect:" + backTo;
		}
	}

	@PostMapping("/questions/save-template")
	public String saveTemplate(@RequestParam(required = false) String title, @RequestParam(required = false) String body,
			@RequestParam(required = false) String tags, HttpServletRequest req, HttpServletResponse res, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (utils.isAdmin(authUser)) {
			String space = utils.getSpaceIdFromCookie(authUser, req);
			Sysprop spaceObj = pc.read(utils.getSpaceId(space));
			if (spaceObj == null) {
				spaceObj = utils.buildSpaceObject("default");
			}
			spaceObj.addProperty("titleTemplate", title);
			spaceObj.addProperty("bodyTemplate", body);
			spaceObj.addProperty("tagsTemplate", tags);
			pc.update(spaceObj);
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "blank";
		} else {
			return "redirect:" + QUESTIONSLINK + "/ask";
		}
	}

	public List<Question> getQuestions(String sortby, String filter, HttpServletRequest req, Model model) {
		Pager itemcount = getPagerFromCookie(req, utils.getPager("page", req));
		List<Question> questionslist = Collections.emptyList();
		String type = Utils.type(Question.class);
		Profile authUser = utils.getAuthUser(req);
		String currentSpace = utils.getSpaceIdFromCookie(authUser, req);
		String query = getQuestionsQuery(req, authUser, sortby, currentSpace, itemcount);

		if (!StringUtils.isBlank(filter) && authUser != null) {
			if ("favtags".equals(filter)) {
				if (!authUser.hasFavtags() && req.getParameterValues("favtags") != null) {
					authUser.setFavtags(Arrays.asList(req.getParameterValues("favtags"))); // API override
				}
				if (isSpaceFilteredRequest(authUser, currentSpace) && authUser.hasFavtags()) {
					questionslist = pc.findQuery(type, getSpaceFilteredFavtagsQuery(currentSpace, authUser), itemcount);
				} else {
					questionslist = pc.findTermInList(type, Config._TAGS, authUser.getFavtags(), itemcount);
				}
			} else if ("local".equals(filter)) {
				String latlng = Optional.ofNullable(authUser.getLatlng()).orElse(req.getParameter("latlng"));
				String[] ll =  latlng == null ? new String[0] : latlng.split(",");
				if (ll.length == 2) {
					double lat = NumberUtils.toDouble(ll[0]);
					double lng = NumberUtils.toDouble(ll[1]);
					questionslist = pc.findNearby(type, query, 25, lat, lng, itemcount);
				}
			}
			model.addAttribute("localFilterOn", "local".equals(filter));
			model.addAttribute("tagFilterOn", "favtags".equals(filter));
			model.addAttribute("filter", "/" + Utils.stripAndTrim(filter));
		} else {
			questionslist = pc.findQuery(type, query, itemcount);
		}

		if (utils.postsNeedApproval(req) && utils.isMod(authUser)) {
			Pager p = new Pager(itemcount.getPage(), itemcount.getLimit());
			List<UnapprovedQuestion> uquestionslist = pc.findQuery(Utils.type(UnapprovedQuestion.class), query, p);
			List<Question> qlist = new LinkedList<>(uquestionslist);
			itemcount.setCount(itemcount.getCount() + p.getCount());
			qlist.addAll(questionslist);
			questionslist = qlist;
		}

		utils.getProfiles(questionslist);
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("questionslist", questionslist);
		return questionslist;
	}

	private String getSpaceFilteredFavtagsQuery(String currentSpace, Profile authUser) {
		StringBuilder sb = new StringBuilder(utils.getSpaceFilter(authUser, currentSpace));
		if (authUser.hasFavtags()) {
			// should we specify the tags property here? like: tags:(tag1 OR tag2)
			sb.append(" AND (").append(authUser.getFavtags().stream().collect(Collectors.joining(" OR "))).append(")");
		}
		return sb.toString();
	}

	private String getQuestionsQuery(HttpServletRequest req, Profile authUser, String sortby, String currentSpace, Pager p) {
		boolean spaceFiltered = isSpaceFilteredRequest(authUser, currentSpace);
		String query = utils.getSpaceFilteredQuery(req, spaceFiltered, null, utils.getSpaceFilter(authUser, currentSpace));
		String spaceFilter = utils.getSpaceFilter(authUser, currentSpace);
		spaceFilter = StringUtils.isBlank(spaceFilter) || spaceFilter.startsWith("*") ? "" : spaceFilter + " AND ";
		if ("activity".equals(sortby)) {
			p.setSortby("properties.lastactivity");
		} else if ("votes".equals(sortby)) {
			p.setSortby("votes");
		} else if ("answered".equals(sortby)) {
			p.setSortby("timestamp");
			String q = "properties.answerid:[* TO *]";
			query = utils.getSpaceFilteredQuery(req, spaceFiltered, spaceFilter + q, q);
		} else if ("unanswered".equals(sortby)) {
			p.setSortby("timestamp");
			if ("default_pager".equals(p.getName()) && p.isDesc()) {
				p.setDesc(false);
			}
			String q = "properties.answercount:0";
			query = utils.getSpaceFilteredQuery(req, spaceFiltered, spaceFilter + q, q);
		}
		String tags = StringUtils.trimToEmpty(StringUtils.removeStart(p.getName(), "with_tags:"));
		if (StringUtils.startsWith(p.getName(), "with_tags:") && !StringUtils.isBlank(tags)) {
			String logicalOperator = tags.startsWith("+") ? " AND " : " OR ";
			tags = StringUtils.remove(tags, "+");
			StringBuilder sb = new StringBuilder("*".equals(query) ? "" : query.concat(" AND "));
			// should we specify the tags property here? like: tags:(tag1 OR tag2)
			sb.append("tags").append(":(").append(tags.replaceAll(",", logicalOperator)).append(")");
			query = sb.toString();
		}
		return getQueryWithPossibleExtension(query, req);
	}

	private String getQueryWithPossibleExtension(String query, HttpServletRequest req) {
		String queryExt = req.getParameter("q");
		if (StringUtils.isBlank(queryExt) || queryExt.startsWith("*")) {
			queryExt = StringUtils.trimToEmpty(HttpUtils.getCookieValue(req, "questions-type-filter"));
		}
		queryExt = Utils.urlDecode(queryExt);
		if (!queryExt.isBlank()) {
			return query.equals("*") ? queryExt : query + " AND (" + queryExt + ")";
		}
		return query;
	}

	private boolean isSpaceFilteredRequest(Profile authUser, String space) {
		return utils.canAccessSpace(authUser, space);
	}

	private Pager getPagerFromCookie(HttpServletRequest req, Pager defaultPager) {
		try {
			defaultPager.setName("default_pager");
			String cookie = HttpUtils.getCookieValue(req, "questions-filter");
			if (StringUtils.isBlank(cookie)) {
				return defaultPager;
			}
			Pager pager = ParaObjectUtils.getJsonReader(Pager.class).readValue(Utils.base64dec(cookie));
			pager.setPage(defaultPager.getPage());
			pager.setLastKey(null);
			pager.setCount(0);
			return pager;
		} catch (JsonProcessingException ex) {
			return Optional.ofNullable(defaultPager).orElse(new Pager(CONF.maxItemsPerPage()) {
				public String getName() {
					return "default_pager";
				}
			});
		}
	}

	private void savePagerToCookie(HttpServletRequest req, HttpServletResponse res, Pager p) {
		try {
			HttpUtils.setRawCookie("questions-filter", Utils.base64enc(ParaObjectUtils.getJsonWriterNoIdent().
					writeValueAsBytes(p)), req, res, "Strict", (int) TimeUnit.DAYS.toSeconds(365));
		} catch (JsonProcessingException ex) { }
	}

	private Map<String, Object> getNewQuestionPayload(Question q) {
		Map<String, Object> payload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(q, false));
		payload.put("author", q == null ? null : q.getAuthor());
		utils.triggerHookEvent("question.create", payload);
		return payload;
	}

	private Question handleSpam(Question q, Profile authUser, Map<String, String> error, HttpServletRequest req) {
		boolean isSpam = utils.isSpam(q, authUser, req);
		if (isSpam && CONF.automaticSpamProtectionEnabled()) {
			error.put("body", "spam");
		} else if (isSpam && !CONF.automaticSpamProtectionEnabled()) {
			UnapprovedQuestion spamq = new UnapprovedQuestion();
			spamq.setTitle(q.getTitle());
			spamq.setBody(q.getBody());
			spamq.setTags(q.getTags());
			spamq.setLocation(q.getLocation());
			spamq.setCreatorid(q.getCreatorid());
			spamq.setAuthor(authUser);
			spamq.setSpace(q.getSpace());
			spamq.setSpam(true);
			return spamq;
		}
		return q;
	}
}
