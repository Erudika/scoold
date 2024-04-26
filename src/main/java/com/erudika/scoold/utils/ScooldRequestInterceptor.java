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
package com.erudika.scoold.utils;

import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.*;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Report.ReportType;
import java.net.ConnectException;
import java.util.Collections;
import java.util.Locale;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Component
@Named
public class ScooldRequestInterceptor implements HandlerInterceptor {

	public static final Logger logger = LoggerFactory.getLogger(ScooldRequestInterceptor.class);
	private static final ScooldConfig CONF = ScooldUtils.getConfig();
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
			if (e.getCause() instanceof ConnectException || e.getMessage().contains("Connection refused")) {
				//response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value()); // breaks site
				logger.error("No connection to Para backend.", e.getMessage());
			} else if (e instanceof UnauthorizedException && isApiRequest) {
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
		// Misc
		modelAndView.addObject("HOMEPAGE", HOMEPAGE);
		modelAndView.addObject("APPNAME", CONF.appName());
		modelAndView.addObject("CDN_URL", CONF.cdnUrl());
		modelAndView.addObject("IN_PRODUCTION", CONF.inProduction());
		modelAndView.addObject("IN_DEVELOPMENT", !CONF.inProduction());
		modelAndView.addObject("MAX_ITEMS_PER_PAGE", CONF.maxItemsPerPage());
		modelAndView.addObject("SESSION_TIMEOUT_SEC", CONF.sessionTimeoutSec());
		modelAndView.addObject("TOKEN_PREFIX", TOKEN_PREFIX);
		modelAndView.addObject("CONTEXT_PATH", CONF.serverContextPath());
		modelAndView.addObject("FB_APP_ID", CONF.facebookAppId());
		modelAndView.addObject("GMAPS_API_KEY", CONF.googleMapsApiKey());
		modelAndView.addObject("IMGUR_CLIENT_ID", CONF.imgurClientId());
		modelAndView.addObject("IMGUR_ENABLED", ScooldUtils.isImgurAvatarRepositoryEnabled());
		modelAndView.addObject("CLOUDINARY_ENABLED", ScooldUtils.isCloudinaryAvatarRepositoryEnabled());
		modelAndView.addObject("RTL_ENABLED", utils.isLanguageRTL(utils.getCurrentLocale(utils.getLanguageCode(request)).getLanguage()));
		modelAndView.addObject("MAX_TAGS_PER_POST", CONF.maxTagsPerPost());
		modelAndView.addObject("includeHighlightJS", CONF.codeHighlightingEnabled());
		modelAndView.addObject("isAjaxRequest", utils.isAjaxRequest(request));
		modelAndView.addObject("reportTypes", ReportType.values());
		modelAndView.addObject("returnto", StringUtils.removeStart(request.getRequestURI(), CONF.serverContextPath()));
		modelAndView.addObject("rev", StringUtils.substring(Utils.md5(Version.getVersion() + CONF.paraSecretKey()), 0, 12));
		// Configurable constants
		modelAndView.addObject("MAX_PAGES", CONF.maxPages());
		modelAndView.addObject("MAX_TEXT_LENGTH", CONF.maxPostLength());
		modelAndView.addObject("MAX_TAGS_PER_POST", CONF.maxTagsPerPost());
		modelAndView.addObject("MAX_REPLIES_PER_POST", CONF.maxRepliesPerPost());
		modelAndView.addObject("MAX_FAV_TAGS", CONF.maxFavoriteTags());
		modelAndView.addObject("MIN_PASS_LENGTH", CONF.minPasswordLength());
		modelAndView.addObject("ANSWER_VOTEUP_REWARD_AUTHOR", CONF.answerVoteupRewardAuthor());
		modelAndView.addObject("QUESTION_VOTEUP_REWARD_AUTHOR", CONF.questionVoteupRewardAuthor());
		modelAndView.addObject("VOTEUP_REWARD_AUTHOR", CONF.voteupRewardAuthor());
		modelAndView.addObject("ANSWER_APPROVE_REWARD_AUTHOR", CONF.answerApprovedRewardAuthor());
		modelAndView.addObject("ANSWER_APPROVE_REWARD_VOTER", CONF.answerApprovedRewardVoter());
		modelAndView.addObject("POST_VOTEDOWN_PENALTY_AUTHOR", CONF.postVotedownPenaltyAuthor());
		modelAndView.addObject("POST_VOTEDOWN_PENALTY_VOTER", CONF.postVotedownPenaltyVoter());
		modelAndView.addObject("VOTER_IFHAS", CONF.voterIfHasRep());
		modelAndView.addObject("COMMENTATOR_IFHAS", CONF.commentatorIfHasRep());
		modelAndView.addObject("CRITIC_IFHAS", CONF.criticIfHasRep());
		modelAndView.addObject("SUPPORTER_IFHAS", CONF.supporterIfHasRep());
		modelAndView.addObject("GOODQUESTION_IFHAS", CONF.goodQuestionIfHasRep());
		modelAndView.addObject("GOODANSWER_IFHAS", CONF.goodAnswerIfHasRep());
		modelAndView.addObject("ENTHUSIAST_IFHAS", CONF.enthusiastIfHasRep());
		modelAndView.addObject("FRESHMAN_IFHAS", CONF.freshmanIfHasRep());
		modelAndView.addObject("SCHOLAR_IFHAS", CONF.scholarIfHasRep());
		modelAndView.addObject("TEACHER_IFHAS", CONF.teacherIfHasRep());
		modelAndView.addObject("PROFESSOR_IFHAS", CONF.professorIfHasRep());
		modelAndView.addObject("GEEK_IFHAS", CONF.geekIfHasRep());
		// Cookies
		modelAndView.addObject("localeCookieName", CONF.localeCookie());
		// Paths
		Profile authUser = (Profile) request.getAttribute(AUTH_USER_ATTRIBUTE);
		modelAndView.addObject("imageslink", CONF.imagesLink()); // do not add context path prefix!
		modelAndView.addObject("scriptslink", CONF.scriptsLink()); // do not add context path prefix!
		modelAndView.addObject("styleslink", CONF.stylesLink()); // do not add context path prefix!
		modelAndView.addObject("peoplelink", CONF.serverContextPath() + PEOPLELINK);
		modelAndView.addObject("profilelink", CONF.usersDiscoverabilityEnabled(utils.isAdmin(authUser)) ? CONF.serverContextPath() + PROFILELINK : "#");
		modelAndView.addObject("searchlink", CONF.serverContextPath() + SEARCHLINK);
		modelAndView.addObject("signinlink", CONF.serverContextPath() + SIGNINLINK);
		modelAndView.addObject("signoutlink", CONF.serverContextPath() + SIGNOUTLINK);
		modelAndView.addObject("aboutlink", CONF.serverContextPath() + ABOUTLINK);
		modelAndView.addObject("privacylink", CONF.serverContextPath() + PRIVACYLINK);
		modelAndView.addObject("termslink", CONF.serverContextPath() + TERMSLINK);
		modelAndView.addObject("tagslink", CONF.serverContextPath() + TAGSLINK);
		modelAndView.addObject("settingslink", CONF.serverContextPath() + SETTINGSLINK);
		modelAndView.addObject("reportslink", CONF.serverContextPath() + REPORTSLINK);
		modelAndView.addObject("adminlink", CONF.serverContextPath() + ADMINLINK);
		modelAndView.addObject("votedownlink", CONF.serverContextPath() + VOTEDOWNLINK);
		modelAndView.addObject("voteuplink", CONF.serverContextPath() + VOTEUPLINK);
		modelAndView.addObject("questionlink", CONF.serverContextPath() + QUESTIONLINK);
		modelAndView.addObject("questionslink", CONF.serverContextPath() + QUESTIONSLINK);
		modelAndView.addObject("commentlink", CONF.serverContextPath() + COMMENTLINK);
		modelAndView.addObject("postlink", CONF.serverContextPath() + POSTLINK);
		modelAndView.addObject("revisionslink", CONF.serverContextPath() + REVISIONSLINK);
		modelAndView.addObject("feedbacklink", CONF.serverContextPath() + FEEDBACKLINK);
		modelAndView.addObject("languageslink", CONF.serverContextPath() + LANGUAGESLINK);
		modelAndView.addObject("apidocslink", CONF.serverContextPath() + APIDOCSLINK);
		// Visual customization
		modelAndView.addObject("navbarFixedClass", CONF.fixedNavEnabled() ? "navbar-fixed" : "none");
		modelAndView.addObject("showBranding", CONF.scooldBrandingEnabled());
		modelAndView.addObject("logoUrl", utils.getLogoUrl(authUser, request));
		modelAndView.addObject("logoWidth", CONF.logoWidth());
		modelAndView.addObject("stylesheetUrl", CONF.stylesheetUrl());
		modelAndView.addObject("darkStylesheetUrl", CONF.darkStylesheetUrl());
		modelAndView.addObject("faviconUrl", CONF.faviconUrl());
		modelAndView.addObject("inlineUserCSS", utils.getInlineCSS());
		modelAndView.addObject("compactViewEnabled", "true".equals(HttpUtils.getCookieValue(request, "questions-view-compact")));
		modelAndView.addObject("compactUsersViewEnabled", "true".equals(HttpUtils.getCookieValue(request, "users-view-compact")));
		modelAndView.addObject("darkModeEnabled", utils.isDarkModeEnabled(authUser, request));
		// Auth & Badges
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
		modelAndView.addObject("numericPaginationEnabled", CONF.numericPaginationEnabled());
		// Markdown with HTML
		modelAndView.addObject("htmlInMarkdownEnabled", CONF.htmlInMarkdownEnabled());
		// check for AJAX pagination requests
		if (utils.isAjaxRequest(request) && (utils.param(request, "page") || utils.param(request, "page1") ||
				utils.param(request, "page2") || utils.param(request, "page3"))) {
			modelAndView.setViewName("pagination"); // switch to page fragment view
		}
		// External scripts
		modelAndView.addObject("externalScripts", utils.getExternalScripts());
		// External styles
		modelAndView.addObject("externalStyles", utils.getExternalStyles());
		// GDPR
		modelAndView.addObject("cookieConsentGiven", utils.cookieConsentGiven(request));
		// CSP nonce
		String cspNonce = utils.getCSPNonce();
		modelAndView.addObject("cspNonce", cspNonce);
		// CSP, HSTS, etc, headers. See https://securityheaders.com
		utils.setSecurityHeaders(cspNonce, request, response);
		// default metadata for social meta tags
		if (StringUtils.isBlank(modelAndView.getModel().getOrDefault("title", "").toString())) {
			modelAndView.addObject("title", CONF.appName());
		}
		if (StringUtils.isBlank(modelAndView.getModel().getOrDefault("description", "").toString())) {
			modelAndView.addObject("description", CONF.metaDescription());
		}
		if (StringUtils.isBlank(modelAndView.getModel().getOrDefault("keywords", "").toString())) {
			modelAndView.addObject("keywords", CONF.metaKeywords());
		}
		if (StringUtils.isBlank(modelAndView.getModel().getOrDefault("ogimage", "").toString())) {
			modelAndView.addObject("ogimage", CONF.metaAppIconUrl());
		}
	}
}
