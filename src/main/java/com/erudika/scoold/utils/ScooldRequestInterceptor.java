/*
 * Copyright 2013-2020 Erudika. https://erudika.com
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

import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Utils;
import static com.erudika.scoold.ScooldServer.*;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Report.ReportType;
import java.net.ConnectException;
import java.util.Collections;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
		ScooldUtils.setInstance(utils);
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if (utils == null) {
			throw new IllegalStateException("ScooldUtils not initialized properly.");
		}
		boolean isApiRequest = utils.isApiRequest(request);
		try {
			request.setAttribute(AUTH_USER_ATTRIBUTE, utils.checkAuth(request, response));
		} catch (Exception e) {
			if (e.getCause() instanceof ConnectException) {
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				logger.error("No connection to Para backend.", e.getMessage());
			} else if (e instanceof WebApplicationException && isApiRequest) {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
			} else {
				logger.error("Auth check failed:", e);
			}
			if (isApiRequest) {
				ParaObjectUtils.getJsonWriter().writeValue(response.getWriter(),
						Collections.singletonMap("error", "Unauthenticated request! " + e.getMessage()));
				return false;
			}
		}
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {

		if (modelAndView == null || StringUtils.startsWith(modelAndView.getViewName(), "redirect:")) {
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
		modelAndView.addObject("GMAPS_API_KEY", Config.getConfigParam("gmaps_api_key", ""));
		modelAndView.addObject("GOOGLE_CLIENT_ID", Config.getConfigParam("google_client_id", ""));
		modelAndView.addObject("GOOGLE_ANALYTICS_ID", Config.getConfigParam("google_analytics_id", ""));
		modelAndView.addObject("includeHighlightJS", Config.getConfigBoolean("code_highlighting_enabled", true));
		modelAndView.addObject("isAjaxRequest", utils.isAjaxRequest(request));
		modelAndView.addObject("reportTypes", ReportType.values());
		modelAndView.addObject("returnto", StringUtils.removeStart(request.getRequestURI(), CONTEXT_PATH));
		// Configurable constants
		modelAndView.addObject("MAX_PAGES", Config.MAX_PAGES);
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
		// Cookies
		modelAndView.addObject("localeCookieName", LOCALE_COOKIE);
		// Paths
		modelAndView.addObject("imageslink", IMAGESLINK); // do not add context path prefix!
		modelAndView.addObject("scriptslink", SCRIPTSLINK); // do not add context path prefix!
		modelAndView.addObject("styleslink", STYLESLINK); // do not add context path prefix!
		modelAndView.addObject("peoplelink", CONTEXT_PATH + PEOPLELINK);
		modelAndView.addObject("profilelink", CONTEXT_PATH + PROFILELINK);
		modelAndView.addObject("searchlink", CONTEXT_PATH + SEARCHLINK);
		modelAndView.addObject("signinlink", CONTEXT_PATH + SIGNINLINK);
		modelAndView.addObject("signoutlink", CONTEXT_PATH + SIGNOUTLINK);
		modelAndView.addObject("aboutlink", CONTEXT_PATH + ABOUTLINK);
		modelAndView.addObject("privacylink", CONTEXT_PATH + PRIVACYLINK);
		modelAndView.addObject("termslink", CONTEXT_PATH + TERMSLINK);
		modelAndView.addObject("tagslink", CONTEXT_PATH + TAGSLINK);
		modelAndView.addObject("settingslink", CONTEXT_PATH + SETTINGSLINK);
		modelAndView.addObject("reportslink", CONTEXT_PATH + REPORTSLINK);
		modelAndView.addObject("adminlink", CONTEXT_PATH + ADMINLINK);
		modelAndView.addObject("votedownlink", CONTEXT_PATH + VOTEDOWNLINK);
		modelAndView.addObject("voteuplink", CONTEXT_PATH + VOTEUPLINK);
		modelAndView.addObject("questionlink", CONTEXT_PATH + QUESTIONLINK);
		modelAndView.addObject("questionslink", CONTEXT_PATH + QUESTIONSLINK);
		modelAndView.addObject("commentlink", CONTEXT_PATH + COMMENTLINK);
		modelAndView.addObject("postlink", CONTEXT_PATH + POSTLINK);
		modelAndView.addObject("revisionslink", CONTEXT_PATH + REVISIONSLINK);
		modelAndView.addObject("feedbacklink", CONTEXT_PATH + FEEDBACKLINK);
		modelAndView.addObject("languageslink", CONTEXT_PATH + LANGUAGESLINK);
		// Visual customization
		modelAndView.addObject("navbarFixedClass", Config.getConfigBoolean("fixed_nav", false) ? "navbar-fixed" : "none");
		modelAndView.addObject("showBranding", Config.getConfigBoolean("show_branding", true));
		modelAndView.addObject("logoUrl", Config.getConfigParam("logo_url", IMAGESLINK + "/logo.svg"));
		modelAndView.addObject("logoWidth", Config.getConfigInt("logo_width", 90));
		modelAndView.addObject("stylesheetUrl", Config.getConfigParam("stylesheet_url", STYLESLINK + "/style.css"));
		modelAndView.addObject("faviconUrl", Config.getConfigParam("favicon_url", IMAGESLINK + "/favicon.ico"));
		modelAndView.addObject("inlineUserCSS", Config.getConfigParam("inline_css", ""));
		// Auth & Badges
		Profile authUser = (Profile) request.getAttribute(AUTH_USER_ATTRIBUTE);
		modelAndView.addObject("infoStripMsg", authUser == null ? Config.getConfigParam("welcome_message", "") : "");
		modelAndView.addObject("authenticated", authUser != null);
		modelAndView.addObject("canComment", utils.canComment(authUser, request));
		modelAndView.addObject("isMod", utils.isMod(authUser));
		modelAndView.addObject("isAdmin", utils.isAdmin(authUser));
		modelAndView.addObject("utils", Utils.getInstance());
		modelAndView.addObject("scooldUtils", utils);
		modelAndView.addObject("authUser", authUser);
		modelAndView.addObject("badgelist", utils.checkForBadges(authUser, request));
		modelAndView.addObject("request", request);
		// Spaces
		String currentSpace = utils.getSpaceIdFromCookie(authUser, request);
		modelAndView.addObject("currentSpace", utils.isDefaultSpace(currentSpace) ? "" : currentSpace);
		// Language
		Locale currentLocale = utils.getCurrentLocale(utils.getLanguageCode(request));
		modelAndView.addObject("currentLocale", currentLocale);
		modelAndView.addObject("lang", utils.getLang(currentLocale));
		modelAndView.addObject("langDirection", utils.isLanguageRTL(currentLocale.getLanguage()) ? "RTL" : "LTR");
		// Pagination
		modelAndView.addObject("numericPaginationEnabled", Config.getConfigBoolean("numeric_pagination_enabled", false));
		// check for AJAX pagination requests
		if (utils.isAjaxRequest(request) && (utils.param(request, "page") || utils.param(request, "page1") ||
				utils.param(request, "page2") || utils.param(request, "page3"))) {
			modelAndView.setViewName("pagination"); // switch to page fragment view
		}
		// External scripts
		modelAndView.addObject("externalScripts", utils.getExternalScripts());
		// CSP nonce
		String cspNonce = utils.getCSPNonce();
		modelAndView.addObject("cspNonce", cspNonce);
		// CSP, HSTS, etc, headers. See https://securityheaders.com
		utils.setSecurityHeaders(cspNonce, request, response);
		// default metadata for social meta tags
		if (!modelAndView.getModel().containsKey("title")) {
			modelAndView.addObject("title", Config.APP_NAME);
		}
		if (!modelAndView.getModel().containsKey("description")) {
			modelAndView.addObject("description", Config.getConfigParam("meta_description", ""));
		}
		if (!modelAndView.getModel().containsKey("ogimage")) {
			modelAndView.addObject("ogimage", IMAGESLINK + "/logowhite.png");
		}
	}
}
