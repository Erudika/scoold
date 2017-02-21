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

import com.erudika.para.utils.Config;
import com.erudika.para.utils.Utils;
import static com.erudika.scoold.ScooldServer.*;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Report.ReportType;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Component
@Named
public class ScooldRequestInterceptor extends HandlerInterceptorAdapter {

	public static final Logger logger = LoggerFactory.getLogger(ScooldRequestInterceptor.class);
	private final ScooldUtils utils;

	@Inject
	public ScooldRequestInterceptor(ScooldUtils utils) {
		this.utils = utils;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if (utils == null) {
			throw new IllegalStateException("ScooldUtils not initialized properly.");
		}
		request.setAttribute(AUTH_USER_ATTRIBUTE, utils.checkAuth(request, response));
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {

		if (StringUtils.startsWith(modelAndView.getViewName(), "redirect:")) {
			return; // skip if redirect
		}

		/*============================*
		 * COMMON MODEL FOR ALL PAGES *
		 *============================*/

		// Misc
		modelAndView.addObject("HOMEPAGE", HOMEPAGE);
		modelAndView.addObject("APPNAME", Config.APP_NAME);
		modelAndView.addObject("CDN_URL", CDN_URL);
		modelAndView.addObject("DESCRIPTION", Config.getConfigParam("meta_description", ""));
		modelAndView.addObject("KEYWORDS", Config.getConfigParam("meta_keywords", ""));
		modelAndView.addObject("IN_PRODUCTION", Config.IN_PRODUCTION);
		modelAndView.addObject("IN_DEVELOPMENT", !Config.IN_PRODUCTION);
		modelAndView.addObject("MAX_ITEMS_PER_PAGE", Config.MAX_ITEMS_PER_PAGE);
		modelAndView.addObject("SESSION_TIMEOUT_SEC", Config.SESSION_TIMEOUT_SEC);
		modelAndView.addObject("TOKEN_PREFIX", TOKEN_PREFIX);
		modelAndView.addObject("FB_APP_ID", Config.FB_APP_ID);
		modelAndView.addObject("GOOGLE_CLIENT_ID", Config.getConfigParam("google_client_id", ""));
		modelAndView.addObject("isAjaxRequest", utils.isAjaxRequest(request));
		modelAndView.addObject("reportTypes", ReportType.values());
		modelAndView.addObject("returnto", request.getRequestURI());
		// Configurable constants
		modelAndView.addObject("MAX_CONTACTS_PER_USER", MAX_CONTACTS_PER_USER);
		modelAndView.addObject("MAX_TEXT_LENGTH", MAX_TEXT_LENGTH);
		modelAndView.addObject("MAX_TAGS_PER_POST", MAX_TAGS_PER_POST);
		modelAndView.addObject("MAX_REPLIES_PER_POST", MAX_REPLIES_PER_POST);
		modelAndView.addObject("MAX_FAV_TAGS", MAX_FAV_TAGS);
		modelAndView.addObject("ANSWER_VOTEUP_REWARD_AUTHOR", ANSWER_VOTEUP_REWARD_AUTHOR);
		modelAndView.addObject("QUESTION_VOTEUP_REWARD_AUTHOR", QUESTION_VOTEUP_REWARD_AUTHOR);
		modelAndView.addObject("VOTEUP_REWARD_AUTHOR", VOTEUP_REWARD_AUTHOR);
		modelAndView.addObject("ANSWER_APPROVE_REWARD_AUTHOR", ANSWER_APPROVE_REWARD_AUTHOR);
		modelAndView.addObject("ANSWER_APPROVE_REWARD_VOTER", ANSWER_APPROVE_REWARD_VOTER);
		modelAndView.addObject("POST_VOTEDOWN_PENALTY_AUTHOR", POST_VOTEDOWN_PENALTY_AUTHOR);
		modelAndView.addObject("POST_VOTEDOWN_PENALTY_VOTER", POST_VOTEDOWN_PENALTY_VOTER);
		modelAndView.addObject("VOTER_IFHAS", VOTER_IFHAS);
		modelAndView.addObject("COMMENTATOR_IFHAS", COMMENTATOR_IFHAS);
		modelAndView.addObject("CRITIC_IFHAS", CRITIC_IFHAS);
		modelAndView.addObject("SUPPORTER_IFHAS", SUPPORTER_IFHAS);
		modelAndView.addObject("GOODQUESTION_IFHAS", GOODQUESTION_IFHAS);
		modelAndView.addObject("GOODANSWER_IFHAS", GOODANSWER_IFHAS);
		modelAndView.addObject("ENTHUSIAST_IFHAS", ENTHUSIAST_IFHAS);
		modelAndView.addObject("FRESHMAN_IFHAS", FRESHMAN_IFHAS);
		modelAndView.addObject("SCHOLAR_IFHAS", SCHOLAR_IFHAS);
		modelAndView.addObject("TEACHER_IFHAS", TEACHER_IFHAS);
		modelAndView.addObject("PROFESSOR_IFHAS", PROFESSOR_IFHAS);
		modelAndView.addObject("GEEK_IFHAS", GEEK_IFHAS);
		// Paths
		modelAndView.addObject("localeCookieName", LOCALE_COOKIE);
		modelAndView.addObject("csrfCookieName", CSRF_COOKIE);
		modelAndView.addObject("peoplelink", peoplelink);
		modelAndView.addObject("profilelink", profilelink);
		modelAndView.addObject("imageslink", IMAGESLINK);
		modelAndView.addObject("scriptslink", SCRIPTSLINK);
		modelAndView.addObject("styleslink", STYLESLINK);
		modelAndView.addObject("searchlink", searchlink);
		modelAndView.addObject("signinlink", signinlink);
		modelAndView.addObject("signoutlink", signoutlink);
		modelAndView.addObject("aboutlink", aboutlink);
		modelAndView.addObject("privacylink", privacylink);
		modelAndView.addObject("termslink", termslink);
		modelAndView.addObject("tagslink", tagslink);
		modelAndView.addObject("settingslink", settingslink);
		modelAndView.addObject("translatelink", translatelink);
		modelAndView.addObject("reportslink", reportslink);
		modelAndView.addObject("adminlink", adminlink);
		modelAndView.addObject("votedownlink", votedownlink);
		modelAndView.addObject("voteuplink", voteuplink);
		modelAndView.addObject("questionlink", questionlink);
		modelAndView.addObject("questionslink", questionslink);
		modelAndView.addObject("commentlink", commentlink);
		modelAndView.addObject("postlink", postlink);
		modelAndView.addObject("feedbacklink", feedbacklink);
		modelAndView.addObject("languageslink", languageslink);
		// Visual customization
		modelAndView.addObject("navbarFixedClass", Config.getConfigBoolean("fixed_nav", false) ? "navbar-fixed" : "none");
		modelAndView.addObject("showBranding", Config.getConfigBoolean("show_branding", true));
		modelAndView.addObject("logoUrl", Config.getConfigParam("logo_url", IMAGESLINK + "/logo.svg"));
		modelAndView.addObject("logoWidth", Config.getConfigInt("logo_width", 90));
		modelAndView.addObject("bgcolor1", Config.getConfigParam("background_color1", "#03a9f4"));
		modelAndView.addObject("bgcolor2", Config.getConfigParam("background_color2", "#0277bd"));
		modelAndView.addObject("bgcolor3", Config.getConfigParam("background_color3", "#e0e0e0"));
		modelAndView.addObject("txtcolor1", Config.getConfigParam("text_color1", "#039be5"));
		modelAndView.addObject("txtcolor2", Config.getConfigParam("text_color2", "#ec407a"));
		modelAndView.addObject("txtcolor3", Config.getConfigParam("text_color3", "#444444"));
		// Auth & Badges
		Profile authUser = (Profile) request.getAttribute(AUTH_USER_ATTRIBUTE);
		modelAndView.addObject("infoStripMsg", "");
		modelAndView.addObject("authenticated", authUser != null);
		modelAndView.addObject("canComment", utils.canComment(authUser, request));
		modelAndView.addObject("isMod", utils.isMod(authUser));
		modelAndView.addObject("isAdmin", utils.isAdmin(authUser));
		modelAndView.addObject("utils", Utils.getInstance());
		modelAndView.addObject("authUser", authUser);
		modelAndView.addObject("badgelist", utils.checkForBadges(authUser, request));
		modelAndView.addObject("request", request);
		// Language
		Locale currentLocale = utils.getCurrentLocale(utils.getLanguageCode(request), request);
		modelAndView.addObject("currentLocale", currentLocale);
		modelAndView.addObject("lang", utils.getLang(currentLocale));
		// Pagination
		// check for AJAX pagination requests
		if (utils.isAjaxRequest(request) && (utils.param(request, "page") ||
				utils.param(request, "page1") || utils.param(request, "page2"))) {
			modelAndView.setViewName("pagination"); // switch to page fragment view
		}
	}
}
