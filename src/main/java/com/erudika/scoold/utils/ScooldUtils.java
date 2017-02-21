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
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.erudika.para.validation.ValidationUtils;
import static com.erudika.scoold.ScooldServer.*;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

	private ParaClient pc;
	private LanguageUtils langutils;

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

	public Profile checkAuth(HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = null;
		if (Utils.getStateParam(Config.AUTH_COOKIE, req) != null) {
			User u = pc.me(Utils.getStateParam(Config.AUTH_COOKIE, req));
			if (u != null) {
				authUser = pc.read(Profile.id(u.getId()));
				if (authUser == null) {
					authUser = new Profile(u.getId(), u.getName());
					authUser.setPicture(u.isGooglePlusUser() ? getGravatar(u.getEmail()) : u.getPicture());
					authUser.setAppid(u.getAppid());
					authUser.setCreatorid(u.getId());
					authUser.setTimestamp(u.getTimestamp());
					authUser.setGroups(u.getIdentifier().equals(Config.ADMIN_IDENT)
							? User.Groups.ADMINS.toString() : u.getGroups());
					authUser.create();
				}
				authUser.setUser(u);
			}
		}
		initCSRFToken(req, res);
		return authUser;
	}

	public void initCSRFToken(HttpServletRequest req, HttpServletResponse res) {
		String csrfInSession = (String) req.getSession(true).getAttribute(TOKEN_PREFIX + "CSRF");
		String csrfInCookie = Utils.getStateParam(CSRF_COOKIE, req);
		if (StringUtils.isBlank(csrfInSession)) {
			csrfInSession = Utils.generateSecurityToken();
			req.getSession(true).setAttribute(TOKEN_PREFIX + "CSRF", csrfInSession);
		}
		if (!csrfInSession.equals(csrfInCookie)) {
			Utils.setStateParam(CSRF_COOKIE, csrfInSession, req, res);
		}
	}

	public Profile getAuthUser(HttpServletRequest req) {
		return (Profile) req.getAttribute(AUTH_USER_ATTRIBUTE);
	}

	public boolean isAuthenticated(HttpServletRequest req) {
		return getAuthUser(req) != null;
	}

	public boolean canComment(Profile authUser, HttpServletRequest req) {
		return isAuthenticated(req) && (authUser.hasBadge(Profile.Badge.ENTHUSIAST) || isMod(authUser));
	}

	public Pager getPager(String pageParamName, HttpServletRequest req) {
		return new Pager(NumberUtils.toInt(req.getParameter(pageParamName), 1), Config.MAX_ITEMS_PER_PAGE);
	}

	public String getLanguageCode(HttpServletRequest req) {
		String cookieLoc = Utils.getCookieValue(req, LOCALE_COOKIE);
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

	public  void fetchProfiles(List<? extends Post> posts) {
		if (posts == null || posts.isEmpty()) {
			return;
		}
		Map<String, String> authorids = new HashMap<String, String>(posts.size());
		Map<String, Profile> authors = new HashMap<String, Profile>(posts.size());
		for (Post post : posts) {
			authorids.put(post.getId(), post.getCreatorid());
		}
		// read all post authors in batch
		for (ParaObject author : pc.readAll(new ArrayList<String>(new HashSet<String>(authorids.values())))) {
			authors.put(author.getId(), (Profile) author);
		}
		// set author object for each post
		for (Post post : posts) {
			post.setAuthor(authors.get(authorids.get(post.getId())));
		}
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
		return "https://www.gravatar.com/avatar/" + Utils.md5(email) + "?size=400&d=mm";
	}

	public void clearSession(HttpServletRequest req, HttpServletResponse res) {
		if (req != null) {
			HttpSession session = req.getSession(false);
			if (session != null) {
				session.invalidate();
			}
			Utils.removeStateParam(Config.AUTH_COOKIE, req, res);
			Utils.removeStateParam(CSRF_COOKIE, req, res);
		}
	}

	public boolean addBadgeOnce(Profile authUser, Profile.Badge b, boolean condition) {
		return addBadge(authUser, b, authUser, condition && !authUser.hasBadge(b), false);
	}

	public boolean addBadgeOnceAndUpdate(Profile authUser, Profile.Badge b, boolean condition) {
		return addBadgeAndUpdate(authUser, b, condition && authUser != null && !authUser.hasBadge(b));
	}

	public boolean addBadgeAndUpdate(Profile authUser, Profile.Badge b, boolean condition) {
		return addBadge(authUser, b, null, condition, true);
	}

	public boolean addBadge(Profile authUser, Profile.Badge b, Profile u, boolean condition, boolean update) {
		if (u == null) {
			u = authUser;
		}
		if (authUser == null || !condition) {
			return false;
		}

		String newb = StringUtils.isBlank(u.getNewbadges()) ? "" : u.getNewbadges().concat(",");
		newb = newb.concat(b.toString());

		u.addBadge(b);
		u.setNewbadges(newb);
		if (update) {
			u.update();
		}
		return true;
	}

	public boolean removeBadge(Profile authUser, Profile.Badge b, Profile u, boolean condition) {
		if (u == null) {
			u = authUser;
		}
		if (authUser == null || !condition) {
			return false;
		}

		if (StringUtils.contains(u.getNewbadges(), b.toString())) {
			String newb = u.getNewbadges();
			newb = newb.replaceAll(b.toString().concat(","), "");
			newb = newb.replaceAll(b.toString(), "");
			newb = newb.replaceFirst(",$", "");
			u.setNewbadges(newb);
		}

		u.removeBadge(b);
		u.update();

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
}
