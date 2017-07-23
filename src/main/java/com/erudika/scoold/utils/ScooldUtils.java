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
package com.erudika.scoold.utils;

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.User;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.email.Emailer;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.erudika.para.validation.ValidationUtils;
import static com.erudika.scoold.ScooldServer.*;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import static com.erudika.scoold.core.Profile.Badge.ENTHUSIAST;
import static com.erudika.scoold.core.Profile.Badge.TEACHER;
import com.erudika.scoold.core.Revision;
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

	public Profile checkAuth(HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = null;
		if (HttpUtils.getStateParam(Config.AUTH_COOKIE, req) != null) {
			User u = pc.me(HttpUtils.getStateParam(Config.AUTH_COOKIE, req));
			if (u != null) {
				authUser = pc.read(Profile.id(u.getId()));
				if (authUser == null) {
					authUser = new Profile(u.getId(), u.getName());
					authUser.setPicture(u.getPicture());
					authUser.setAppid(u.getAppid());
					authUser.setCreatorid(u.getId());
					authUser.setTimestamp(u.getTimestamp());
					authUser.setGroups(u.getIdentifier().equals(Config.ADMIN_IDENT)
							? User.Groups.ADMINS.toString() : u.getGroups());
					authUser.create();
					sendWelcomeEmail(u, authUser, req);
				}
				authUser.setUser(u);
			}
		}
		initCSRFToken(req, res);
		return authUser;
	}

	private void sendWelcomeEmail(User user, Profile profile, HttpServletRequest req) {
		// send welcome email notification
		if (user != null && profile != null) {
			Map<String, Object> model = new HashMap<String, Object>();
			Map<String, String> lang = getLang(req);
			String subject = lang.get("signin.welcome");
			String body1 = lang.get("signin.welcome.body1") + "<br><br>";
			String body2 = lang.get("signin.welcome.body2") + "<br><br>";
			String body3 = "Best, <br>The Scoold team";
			model.put("logourl", Config.getConfigParam("small_logo_url", "https://scoold.com/logo.png"));
			model.put("heading", Utils.formatMessage(lang.get("signin.welcome.title"), user.getName()));
			model.put("body", body1 + body2 + body3);
			emailer.sendEmail(Arrays.asList(user.getEmail()), subject,
					Utils.compileMustache(model, loadEmailTemplate("notify")));
		}
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
		Locale requestLocale = langutils.getProperLocale(req.getLocale().getLanguage());
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
		Map<String, String> lang = langutils.readLanguage(currentLocale.getLanguage());
		if (lang == null || lang.isEmpty()) {
			lang = langutils.getDefaultLanguage();
		}
		return lang;
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

	public List<Post> getSimilarPosts(Post showPost, Pager pager) {
		List<Post> similarquestions = Collections.emptyList();
		if (!showPost.isReply()) {
			String likeTxt = Utils.stripAndTrim((showPost.getTitle() + " " +
					showPost.getBody() + " " + showPost.getTags()), " ");
			if (!StringUtils.isBlank(likeTxt)) {
				similarquestions = pc.findSimilar(showPost.getType(), showPost.getId(),
						new String[]{"properties.title", "properties.body", "properties.tags"}, likeTxt, pager);
			}
		}
		return similarquestions;
	}

	/**
	 * **** MISC ******
	 */
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

	public boolean canComment(Profile authUser, HttpServletRequest req) {
		return isAuthenticated(req) && (authUser.hasBadge(ENTHUSIAST) || isMod(authUser));
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
					if (firstValue != null) {
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
			}
		}
		return true;
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
			} catch (IOException ex) {}
		}
		return template;
	}

	public String getDefaultContentSecurityPolicy() {
		return "default-src 'self'; base-uri 'self'; "
				+ "connect-src 'self' scoold.com www.google-analytics.com; "
				+ "frame-src 'self' accounts.google.com staticxx.facebook.com; "
				+ "font-src cdnjs.cloudflare.com fonts.gstatic.com fonts.googleapis.com; "
				+ "script-src 'self' 'unsafe-eval' apis.google.com maps.googleapis.com connect.facebook.net "
					+ "cdnjs.cloudflare.com www.google-analytics.com code.jquery.com static.scoold.com; "
				+ "style-src 'self' 'unsafe-inline' fonts.googleapis.com cdnjs.cloudflare.com static.scoold.com; "
				+ "img-src 'self' https: data:; report-uri /reports/cspv";
	}
}
