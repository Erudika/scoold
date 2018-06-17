/*
 * Copyright 2013-2018 Erudika. https://erudika.com
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
package com.erudika.scoold.utils;

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.User;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.email.Emailer;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.erudika.para.validation.ValidationUtils;
import static com.erudika.scoold.ScooldServer.*;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import static com.erudika.scoold.core.Profile.Badge.ENTHUSIAST;
import static com.erudika.scoold.core.Profile.Badge.TEACHER;
import com.erudika.scoold.core.Revision;
import static com.erudika.scoold.utils.HttpUtils.getCookieValue;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.ConstraintViolation;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Component
@Named
public final class ScooldUtils {

	private static final Logger logger = LoggerFactory.getLogger(ScooldUtils.class);
	private static final Map<String, String> EMAIL_TEMPLATES = new ConcurrentHashMap<String, String>();
	private static final Set<String> APPROVED_DOMAINS = new HashSet<>();
	private static final Set<String> ADMINS = new HashSet<>();

	private ParaClient pc;
	private LanguageUtils langutils;
	private static ScooldUtils instance;
	@Inject private Emailer emailer;

	@Inject
	public ScooldUtils(ParaClient pc, LanguageUtils langutils) {
		this.pc = pc;
		this.langutils = langutils;
	}

	public ParaClient getParaClient() {
		return pc;
	}

	public LanguageUtils getLangutils() {
		return langutils;
	}

	public static ScooldUtils getInstance() {
		return instance;
	}

	static void setInstance(ScooldUtils instance) {
		ScooldUtils.instance = instance;
	}

	static {
		String approved = Config.getConfigParam("approved_domains_for_signups", "");
		String[] domains = approved.split("\\s*,\\s*");
		if (!StringUtils.isBlank(approved) && domains != null && domains.length > 0) {
			for (String domain : domains) {
				if (!StringUtils.isBlank(domain)) {
					APPROVED_DOMAINS.add(domain);
				}
			}
		}
		String adminz = Config.getConfigParam("admins", "");
		String[] admins = adminz.split("\\s*,\\s*");
		if (!StringUtils.isBlank(adminz) && admins != null && admins.length > 0) {
			for (String admin : admins) {
				if (!StringUtils.isBlank(admin)) {
					ADMINS.add(admin);
				}
			}
		}
	}

	public Profile checkAuth(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		Profile authUser = null;
		if (HttpUtils.getStateParam(Config.AUTH_COOKIE, req) != null) {
			User u = pc.me(HttpUtils.getStateParam(Config.AUTH_COOKIE, req));
			if (u != null && isEmailDomainApproved(u.getEmail())) {
				authUser = pc.read(Profile.id(u.getId()));
				if (authUser == null) {
					authUser = new Profile(u.getId(), u.getName());
					authUser.setPicture(u.getPicture());
					authUser.setAppid(u.getAppid());
					authUser.setCreatorid(u.getId());
					authUser.setTimestamp(u.getTimestamp());
					authUser.setGroups(isRecognizedAsAdmin(u)
							? User.Groups.ADMINS.toString() : u.getGroups());
					authUser.create();
					if (!u.getIdentityProvider().equals("generic")) {
						sendWelcomeEmail(u, false, req);
					}
					logger.info("Created new user '{}' with id={}, groups={}.",
							u.getName(), authUser.getId(), authUser.getGroups());
				}
				boolean update = false;
				if (!isAdmin(authUser) && isRecognizedAsAdmin(u)) {
					logger.info("User '{}' with id={} promoted to admin.", u.getName(), authUser.getId());
					authUser.setGroups(User.Groups.ADMINS.toString());
					update = true;
				} else if (isAdmin(authUser) && !isRecognizedAsAdmin(u)) {
					logger.info("User '{}' with id={} demoted to regular user.", u.getName(), authUser.getId());
					authUser.setGroups(User.Groups.USERS.toString());
					update = true;
				}
				authUser.setUser(u);
				if (!StringUtils.equals(u.getPicture(), authUser.getPicture()) &&
						!StringUtils.contains(authUser.getPicture(), "gravatar.com")) {
					authUser.setPicture(u.getPicture());
					update = true;
				}
				if (update) {
					authUser.update();
				}
			} else {
				clearSession(req, res);
				logger.warn("Attempted signin from an unknown domain: {}", u != null ? u.getEmail() : "unknown");
				res.setStatus(401);
			}
		}
		initCSRFToken(req, res);
		return authUser;
	}

	public void sendWelcomeEmail(User user, boolean verifyEmail, HttpServletRequest req) {
		// send welcome email notification
		if (user != null) {
			Map<String, Object> model = new HashMap<String, Object>();
			Map<String, String> lang = getLang(req);
			String subject = lang.get("signin.welcome");
			String body1 = lang.get("signin.welcome.body1") + "<br><br>";
			String body2 = lang.get("signin.welcome.body2") + "<br><br>";
			String body3 = "Best, <br>The Scoold team";

			if (verifyEmail && !user.getActive() && !StringUtils.isBlank(user.getIdentifier())) {
				Sysprop s = pc.read(user.getIdentifier());
				if (s != null) {
					String token = Utils.base64encURL(Utils.generateSecurityToken().getBytes());
					s.addProperty(Config._EMAIL_TOKEN, token);
					pc.update(s);
					token = getServerURL() + CONTEXT_PATH + SIGNINLINK + "/register?id=" + user.getId() + "&token=" + token;
					body3 = "<b><a href=\"" + token + "\">" + lang.get("signin.welcome.verify") + "</a></b><br><br>";
					body3 += "Best, <br>The Scoold team<br><br>";
				}
			}

			model.put("logourl", Config.getConfigParam("small_logo_url", "https://scoold.com/logo.png"));
			model.put("heading", Utils.formatMessage(lang.get("signin.welcome.title"), user.getName()));
			model.put("body", body1 + body2 + body3);
			emailer.sendEmail(Arrays.asList(user.getEmail()), subject,
					Utils.compileMustache(model, loadEmailTemplate("notify")));
		}
	}

	public void sendPasswordResetEmail(String email, String token, HttpServletRequest req) {
		if (email != null && token != null) {
			Map<String, Object> model = new HashMap<String, Object>();
			Map<String, String> lang = getLang(req);
			String url = getServerURL() + CONTEXT_PATH + SIGNINLINK + "/iforgot?email=" + email + "&token=" + token;
			String subject = lang.get("iforgot.title");
			String body1 = "Open the link below to change your password:<br><br>";
			String body2 = Utils.formatMessage("<b><a href=\"{0}\">RESET PASSWORD</a></b><br><br>", url);
			String body3 = "Best, <br>The Scoold team<br><br>";

			model.put("logourl", Config.getConfigParam("small_logo_url", "https://scoold.com/logo.png"));
			model.put("heading", lang.get("hello"));
			model.put("body", body1 + body2 + body3);
			emailer.sendEmail(Arrays.asList(email), subject, Utils.compileMustache(model, loadEmailTemplate("notify")));
		}
	}

	public boolean isEmailDomainApproved(String email) {
		if (StringUtils.isBlank(email)) {
			return false;
		}
		if (!APPROVED_DOMAINS.isEmpty()) {
			return APPROVED_DOMAINS.contains(StringUtils.substringAfter(email, "@"));
		}
		return true;
	}

	public void initCSRFToken(HttpServletRequest req, HttpServletResponse res) {
		String csrfInSession = (String) req.getSession(true).getAttribute(TOKEN_PREFIX + "CSRF");
		//String csrfInCookie = Utils.getStateParam(CSRF_COOKIE, req);
		if (StringUtils.isBlank(csrfInSession)) {
			csrfInSession = Utils.generateSecurityToken();
			req.getSession(true).setAttribute(TOKEN_PREFIX + "CSRF", csrfInSession);
		}
		HttpUtils.setStateParam(CSRF_COOKIE, csrfInSession, req, res);
	}

	public Profile getAuthUser(HttpServletRequest req) {
		return (Profile) req.getAttribute(AUTH_USER_ATTRIBUTE);
	}

	public boolean isAuthenticated(HttpServletRequest req) {
		return getAuthUser(req) != null;
	}

	public Pager getPager(String pageParamName, HttpServletRequest req) {
		return new Pager(NumberUtils.toInt(req.getParameter(pageParamName), 1), Config.MAX_ITEMS_PER_PAGE);
	}

	public String getLanguageCode(HttpServletRequest req) {
		String cookieLoc = HttpUtils.getCookieValue(req, LOCALE_COOKIE);
		Locale requestLocale = langutils.getProperLocale(req.getLocale().toString());
		return (cookieLoc != null) ? cookieLoc : requestLocale.getLanguage();
	}

	public Locale getCurrentLocale(String langname, HttpServletRequest req) {
		Locale currentLocale = langutils.getProperLocale(langname);
		if (currentLocale == null) {
			currentLocale = langutils.getProperLocale("en");
		}
		return currentLocale;
	}

	public Map<String, String> getLang(HttpServletRequest req) {
		return getLang(getCurrentLocale(getLanguageCode(req), req));
	}

	public Map<String, String> getLang(Locale currentLocale) {
		Map<String, String> lang = langutils.readLanguage(currentLocale.toString());
		if (lang == null || lang.isEmpty()) {
			lang = langutils.getDefaultLanguage();
		}
		return lang;
	}

	public boolean isLanguageRTL(String langCode) {
		return StringUtils.equalsAnyIgnoreCase(langCode, "ar", "he", "dv", "iw", "fa", "ps", "sd", "ug", "ur", "yi");
	}

	public void fetchProfiles(List<? extends ParaObject> objects) {
		if (objects == null || objects.isEmpty()) {
			return;
		}
		Map<String, String> authorids = new HashMap<String, String>(objects.size());
		Map<String, Profile> authors = new HashMap<String, Profile>(objects.size());
		for (ParaObject obj : objects) {
			if (obj.getCreatorid() != null) {
				authorids.put(obj.getId(), obj.getCreatorid());
			}
		}
		List<String> ids = new ArrayList<String>(new HashSet<String>(authorids.values()));
		if (ids.isEmpty()) {
			return;
		}
		// read all post authors in batch
		for (ParaObject author : pc.readAll(ids)) {
			authors.put(author.getId(), (Profile) author);
		}
		// set author object for each post
		for (ParaObject obj : objects) {
			if (obj instanceof Post) {
				((Post) obj).setAuthor(authors.get(authorids.get(obj.getId())));
			} else if (obj instanceof Revision) {
				((Revision) obj).setAuthor(authors.get(authorids.get(obj.getId())));
			}
		}
	}

	//get the comments for each answer and the question
	public void getComments(List<Post> allPosts) {
		Map<String, List<Comment>> allComments = new HashMap<String, List<Comment>>();
		List<String> allCommentIds = new ArrayList<String>();
		List<Post> forUpdate = new ArrayList<Post>(allPosts.size());
		// get the comment ids of the first 5 comments for each post
		for (Post post : allPosts) {
			// not set => read comments if any and embed ids in post object
			if (post.getCommentIds() == null) {
				forUpdate.add(reloadFirstPageOfComments(post));
				allComments.put(post.getId(), post.getComments());
			} else {
				// ids are set => add them to list for bulk read
				allCommentIds.addAll(post.getCommentIds());
			}
		}
		if (!allCommentIds.isEmpty()) {
			// read all comments for all posts on page in bulk
			for (ParaObject comment : pc.readAll(allCommentIds)) {
				List<Comment> postComments = allComments.get(comment.getParentid());
				if (postComments == null) {
					allComments.put(comment.getParentid(), new ArrayList<Comment>());
				}
				allComments.get(comment.getParentid()).add((Comment) comment);
			}
		}
		// embed comments in each post for use within the view
		for (Post post : allPosts) {
			List<Comment> cl = allComments.get(post.getId());
			int clSize = (cl == null) ? 0 : cl.size();
			if (post.getCommentIds().size() != clSize) {
				forUpdate.add(reloadFirstPageOfComments(post));
				clSize = post.getComments().size();
			} else {
				post.setComments(cl);
			}
			post.getItemcount().setCount(clSize + 1); // hack to show the "more" button
		}
		if (!forUpdate.isEmpty()) {
			pc.updateAll(allPosts);
		}
	}

	public Post reloadFirstPageOfComments(Post post) {
		List<Comment> commentz = pc.getChildren(post, Utils.type(Comment.class), post.getItemcount());
		ArrayList<String> ids = new ArrayList<String>(commentz.size());
		for (Comment comment : commentz) {
			ids.add(comment.getId());
		}
		post.setCommentIds(ids);
		post.setComments(commentz);
		return post;
	}

	public void updateViewCount(Post showPost, HttpServletRequest req, HttpServletResponse res) {
		//do not count views from author
		if (showPost != null && !isMine(showPost, getAuthUser(req))) {
			String postviews = HttpUtils.getStateParam("postviews", req);
			if (!StringUtils.contains(postviews, showPost.getId())) {
				long views = (showPost.getViewcount() == null) ? 0 : showPost.getViewcount();
				showPost.setViewcount(views + 1); //increment count
				HttpUtils.setStateParam("postviews", postviews + "," + showPost.getId(), req, res);
				pc.update(showPost);
			}
		}
	}

	public List<Post> getSimilarPosts(Post showPost, Pager pager) {
		List<Post> similarquestions = Collections.emptyList();
		if (!showPost.isReply()) {
			String likeTxt = Utils.abbreviate(Utils.stripAndTrim((showPost.getTitle() + " " + showPost.getBody())), 2000);
			if (!StringUtils.isBlank(likeTxt)) {
				similarquestions = pc.findSimilar(showPost.getType(), showPost.getId(),
						new String[]{"properties.title", "properties.body", "properties.tags"}, likeTxt, pager);
			}
		}
		return similarquestions;
	}

	public boolean param(HttpServletRequest req, String param) {
		return req.getParameter(param) != null;
	}

	public boolean isAjaxRequest(HttpServletRequest req) {
		return req.getHeader("X-Requested-With") != null || req.getParameter("X-Requested-With") != null;
	}

	public boolean isAdmin(Profile authUser) {
		return authUser != null && User.Groups.ADMINS.toString().equals(authUser.getGroups());
	}

	public boolean isMod(Profile authUser) {
		return authUser != null && (isAdmin(authUser) || User.Groups.MODS.toString().equals(authUser.getGroups()));
	}

	public boolean isRecognizedAsAdmin(User u) {
		return u.isAdmin() || ADMINS.contains(u.getIdentifier()) || ADMINS.contains(u.getEmail());
	}

	public boolean canComment(Profile authUser, HttpServletRequest req) {
		return isAuthenticated(req) && (authUser.hasBadge(ENTHUSIAST) ||
				Config.getConfigBoolean("new_users_can_comment", true) ||
				isMod(authUser));
	}

	public boolean canAccessSpace(Profile authUser, String targetSpaceId) {
		boolean isDefaultSpacePublic = Config.getConfigBoolean("is_default_space_public", true);
		if (authUser == null || targetSpaceId == null) {
			return isDefaultSpacePublic;
		}
		if (StringUtils.isBlank(targetSpaceId)) {
			// can user access the default space (blank)
			return isDefaultSpacePublic || isMod(authUser) || !authUser.hasSpaces();
		}
		boolean isMemberOfSpace = false;
		for (String space : authUser.getSpaces()) {
			if (StringUtils.startsWithIgnoreCase(space, getSpaceId(targetSpaceId) + ":")) {
				isMemberOfSpace = true;
				break;
			}
		}
		return isMemberOfSpace;
	}

	public String getValidSpaceId(Profile authUser, String space) {
		if (authUser == null) {
			return "";
		}
		String defaultSpace = authUser.hasSpaces() ? authUser.getSpaces().get(0) : "";
		String s = canAccessSpace(authUser, space) ? space : defaultSpace;
		return s == null ? "" : s;
	}

	public String getSpaceName(String space) {
		return StringUtils.replaceAll(space, "^scooldspace:[^:]+:", "");
	}

	public String getSpaceId(String space) {
		String s = StringUtils.contains(space, ":") ? StringUtils.substring(space, 0, space.lastIndexOf(":")) : "";
		return "scooldspace".equals(s) ? space : s;
	}

	public String getSpaceFilteredQuery(HttpServletRequest req) {
		Profile authUser = getAuthUser(req);
		String currentSpace = getValidSpaceId(authUser, getCookieValue(req, SPACE_COOKIE));
		return StringUtils.isBlank(currentSpace) ? (canAccessSpace(authUser, currentSpace) ? "*" : "") :
				"properties.space:\"" + currentSpace + "\"";
	}

	public String sanitizeQueryString(String query, HttpServletRequest req) {
		String qf = getSpaceFilteredQuery(req);
		String defaultQuery = "*";
		String q = StringUtils.trimToEmpty(query);
		if (qf.isEmpty() || qf.length() > 1) {
			q = q.replaceAll("[\\*\\?]", "").trim();
			q = StringUtils.removeAll(q, "AND");
			q = StringUtils.removeAll(q, "OR");
			q = StringUtils.removeAll(q, "NOT");
			q = q.trim();
			defaultQuery = "";
		}
		if (qf.isEmpty()) {
			return defaultQuery;
		} else if ("*".equals(qf)) {
			return q;
		} else {
			if (q.isEmpty()) {
				return qf;
			} else {
				return qf + " AND " + q;
			}
		}
	}

	public boolean isMine(Post showPost, Profile authUser) {
		// author can edit, mods can edit & ppl with rep > 100 can edit
		return showPost != null && authUser != null ? authUser.getId().equals(showPost.getCreatorid()) : false;
	}

	public boolean canEdit(Post showPost, Profile authUser) {
		return authUser != null ? (authUser.hasBadge(TEACHER) || isMod(authUser) || isMine(showPost, authUser)) : false;
	}

	public <P extends ParaObject> P populate(HttpServletRequest req, P pobj, String... paramName) {
		if (pobj != null && paramName != null) {
			HashMap<String, Object> data = new HashMap<String, Object>();
			for (String param : paramName) {
				String[] values;
				if (param.matches(".+?\\|.$")) {
					// convert comma-separated value to list of strings
					String cleanParam = param.substring(0, param.length() - 2);
					values = req.getParameterValues(cleanParam);
					String firstValue = (values != null && values.length > 0) ? values[0] : null;
					String separator = param.substring(param.length() - 1);
					if (!StringUtils.isBlank(firstValue)) {
						data.put(cleanParam, Arrays.asList(firstValue.split(separator)));
					}
				} else {
					values = req.getParameterValues(param);
					String firstValue = (values != null && values.length > 0) ? values[0] : null;
					if (values != null && values.length > 1) {
						data.put(param, Arrays.asList(values));
					} else if (firstValue != null) {
						data.put(param, firstValue);
					}
				}
			}
			if (!data.isEmpty()) {
				ParaObjectUtils.setAnnotatedFields(pobj, data, null);
			}
		}
		return pobj;
	}

	public <P extends ParaObject> Map<String, String> validate(P pobj) {
		HashMap<String, String> error = new HashMap<String, String>();
		if (pobj != null) {
			Set<ConstraintViolation<P>> errors = ValidationUtils.getValidator().validate(pobj);
			for (ConstraintViolation<P> err : errors) {
				error.put(err.getPropertyPath().toString(), err.getMessage());
			}
		}
		return error;
	}

	public String getGravatar(String email) {
		return "https://www.gravatar.com/avatar/" + Utils.md5(email) + "?size=400&d=retro";
	}

	public String getGravatar(Profile profile) {
		if (profile == null || profile.getUser() == null) {
			return "https://www.gravatar.com/avatar?d=retro&size=400";
		} else {
			return getGravatar(profile.getUser().getEmail());
		}
	}

	public void clearSession(HttpServletRequest req, HttpServletResponse res) {
		if (req != null) {
			HttpSession session = req.getSession(false);
			if (session != null) {
				session.invalidate();
			}
			HttpUtils.removeStateParam(Config.AUTH_COOKIE, req, res);
			HttpUtils.removeStateParam(CSRF_COOKIE, req, res);
		}
	}

	public boolean addBadgeOnce(Profile authUser, Profile.Badge b, boolean condition) {
		return addBadge(authUser, b, condition && !authUser.hasBadge(b), false);
	}

	public boolean addBadgeOnceAndUpdate(Profile authUser, Profile.Badge b, boolean condition) {
		return addBadgeAndUpdate(authUser, b, condition && authUser != null && !authUser.hasBadge(b));
	}

	public boolean addBadgeAndUpdate(Profile authUser, Profile.Badge b, boolean condition) {
		return addBadge(authUser, b, condition, true);
	}

	public boolean addBadge(Profile user, Profile.Badge b, boolean condition, boolean update) {
		if (user != null && condition) {
			String newb = StringUtils.isBlank(user.getNewbadges()) ? "" : user.getNewbadges().concat(",");
			newb = newb.concat(b.toString());

			user.addBadge(b);
			user.setNewbadges(newb);
			if (update) {
				user.update();
				return true;
			}
		}
		return false;
	}

	public List<String> checkForBadges(Profile authUser, HttpServletRequest req) {
		List<String> badgelist = new ArrayList<String>();
		if (authUser != null && !isAjaxRequest(req)) {
			long oneYear = authUser.getTimestamp() + (365 * 24 * 60 * 60 * 1000);
			addBadgeOnce(authUser, Profile.Badge.ENTHUSIAST, authUser.getVotes() >= ENTHUSIAST_IFHAS);
			addBadgeOnce(authUser, Profile.Badge.FRESHMAN, authUser.getVotes() >= FRESHMAN_IFHAS);
			addBadgeOnce(authUser, Profile.Badge.SCHOLAR, authUser.getVotes() >= SCHOLAR_IFHAS);
			addBadgeOnce(authUser, Profile.Badge.TEACHER, authUser.getVotes() >= TEACHER_IFHAS);
			addBadgeOnce(authUser, Profile.Badge.PROFESSOR, authUser.getVotes() >= PROFESSOR_IFHAS);
			addBadgeOnce(authUser, Profile.Badge.GEEK, authUser.getVotes() >= GEEK_IFHAS);
			addBadgeOnce(authUser, Profile.Badge.SENIOR, (System.currentTimeMillis() - authUser.getTimestamp()) >= oneYear);

			if (!StringUtils.isBlank(authUser.getNewbadges())) {
				badgelist.addAll(Arrays.asList(authUser.getNewbadges().split(",")));
				authUser.setNewbadges(null);
				authUser.update();
			}
		}
		return badgelist;
	}

	public String loadEmailTemplate(String name) {
		if (name == null) {
			return "";
		}
		if (EMAIL_TEMPLATES.containsKey(name)) {
			return EMAIL_TEMPLATES.get(name);
		}
		String template = "";
		InputStream in = getClass().getClassLoader().getResourceAsStream("emails/" + name + ".html");
		try {
			if (in != null) {
				Scanner s = new Scanner(in).useDelimiter("\\A");
				template = s.hasNext() ? s.next() : "";
				s.close();
				if (!StringUtils.isBlank(template)) {
					EMAIL_TEMPLATES.put(name, template);
				}
			}
		} catch (Exception ex) {
			logger.info("Couldn't load email template '{0}'. - {1}", name, ex.getMessage());
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				logger.error(null, ex);
			}
		}
		return template;
	}

	public void setSecurityHeaders(HttpServletRequest request, HttpServletResponse response) {
		// CSP Header
		if (Config.getConfigBoolean("csp_header_enabled", true)) {
			response.addHeader("Content-Security-Policy",
					Config.getConfigParam("csp_header", getDefaultContentSecurityPolicy(request.isSecure())));
		}
		// HSTS Header
		if (Config.getConfigBoolean("hsts_header_enabled", true)) {
			response.addHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
		}
		// Frame Options Header
		if (Config.getConfigBoolean("framing_header_enabled", true)) {
			response.addHeader("X-Frame-Options", "SAMEORIGIN");
		}
		// XSS Header
		if (Config.getConfigBoolean("xss_header_enabled", true)) {
			response.addHeader("X-XSS-Protection", "1; mode=block");
		}
		// Content Type Header
		if (Config.getConfigBoolean("contenttype_header_enabled", true)) {
			response.addHeader("X-Content-Type-Options", "nosniff");
		}
		// Referrer Header
		if (Config.getConfigBoolean("referrer_header_enabled", true)) {
			response.addHeader("Referrer-Policy", "strict-origin");
		}
	}

	public String getDefaultContentSecurityPolicy(boolean isSecure) {
		return (isSecure ? "upgrade-insecure-requests; " : "")
				+ "default-src 'self'; "
				+ "base-uri 'self'; "
				+ "form-action 'self'; "
				+ "connect-src 'self' scoold.com www.google-analytics.com; "
				+ "frame-src 'self' accounts.google.com staticxx.facebook.com; "
				+ "font-src cdnjs.cloudflare.com fonts.gstatic.com fonts.googleapis.com; "
				+ "script-src 'self' 'unsafe-eval' apis.google.com maps.googleapis.com connect.facebook.net "
					+ "cdnjs.cloudflare.com www.google-analytics.com code.jquery.com static.scoold.com; "
				+ "style-src 'self' 'unsafe-inline' fonts.googleapis.com cdnjs.cloudflare.com static.scoold.com; "
				+ "img-src 'self' https: data:; "
				+ "report-uri /reports/cspv";
	}
}
