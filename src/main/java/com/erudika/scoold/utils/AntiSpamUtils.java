/*
 * Copyright 2013-2025 Erudika. https://erudika.com
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

import com.erudika.para.core.ParaObject;
import com.erudika.para.core.User;
import com.erudika.para.core.utils.Para;
import com.erudika.scoold.ScooldConfig;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Report;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spam filtering utils, mainly for talking to the Akismet API.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public final class AntiSpamUtils {

	private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);
	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private static final String AKISMET_USER_AGENT = "Scoold/" + Para.getVersion() + " | Akismet/1.1";

	/**
	 * Default private constructor.
	 */
	private AntiSpamUtils() {
	}

	public static Map<String, String> buildAkismetComment(ParaObject pobj, Profile authUser, HttpServletRequest req) {
		if (pobj == null) {
			return null;
		}
		final Map<String, String> comment = createBaseAkismetPayload(req);
		if (pobj instanceof Comment) {
			comment.put("comment_content", ((Comment) pobj).getComment());
			comment.put("permalink", CONF.serverUrl() + "/comment/" + ((Comment) pobj).getId());
			comment.put("comment_type", "comment");
		} else if (pobj instanceof Post) {
			comment.put("comment_content", ((Post) pobj).getTitle() + " \n " + ((Post) pobj).getBody());
			comment.put("permalink", CONF.serverUrl() + ((Post) pobj).getPostLinkForRedirect());
			comment.put("comment_type", ((Post) pobj).isReply() ? "reply" : "forum-post");
		}
		if (authUser != null) {
			comment.put("comment_author", authUser.getName());
			User u = authUser.getUser();
			if (u != null) {
				comment.put("comment_author_email", u.getEmail());
			}
		}
		if (ScooldUtils.getInstance().isMod(authUser)) {
			comment.put("user_role", "administrator");
		}
		return comment;
	}

	public static Map<String, String> buildAkismetCommentFromReport(Report rep, HttpServletRequest req) {
		if (rep == null) {
			return null;
		}
		final Map<String, String> comment = createBaseAkismetPayload(req);
		comment.put("comment_content", rep.getContent());
		comment.put("permalink", rep.getLink());
		comment.put("comment_type", rep.getLink().contains("/comment/") ? "comment" : "forum-post");

		Profile authUser = ScooldUtils.getInstance().getParaClient().read(rep.getCreatorid());
		if (authUser != null) {
			comment.put("comment_author", authUser.getName());
			User u = authUser.getUser();
			if (u != null) {
				comment.put("comment_author_email", u.getEmail());
			}
		}
		if (ScooldUtils.getInstance().isMod(authUser)) {
			comment.put("user_role", "administrator");
		}
		return comment;
	}

	public static boolean isSpam(ParaObject pobj, Profile authUser, HttpServletRequest req) {
		if (pobj == null || StringUtils.isBlank(CONF.akismetApiKey())) {
			return false;
		}
		final Map<String, String> comment = buildAkismetComment(pobj, authUser, req);
		final boolean isSpam = checkAkismetComment(comment);
		confirmSpam(comment, isSpam, CONF.automaticSpamProtectionEnabled(), req);
		return isSpam;
	}

	public static void confirmSpam(Map<String, String> comment, boolean isSpam, boolean submit, HttpServletRequest req) {
		if (comment == null || StringUtils.isBlank(CONF.akismetApiKey()) || !submit) {
			return;
		}
		final String action = isSpam ? "submit-spam" : "submit-ham";
		Optional<String> response = executeAkismetRequest(action, comment);
		if (response.isEmpty()) {
			return;
		}
		String author = comment.get("comment_author");
		String permalink = comment.get("permalink");
		if (isSpam) {
			logger.info("Detected spam post by user {} which was blocked, URL {}, '{}'.",
					author, permalink, comment.get("comment_content"));
		} else {
			logger.debug("Confirmed ham for user {} at {}.", author, permalink);
		}
		if (!"Thanks for making the web a better place.".equalsIgnoreCase(response.get().trim())) {
			logger.debug("Akismet {} response: {}", action, response.get());
		}
	}

	private static Map<String, String> createBaseAkismetPayload(HttpServletRequest req) {
		Map<String, String> payload = new LinkedHashMap<>();
		payload.put("blog", CONF.serverUrl());
		payload.put("blog_charset", StandardCharsets.UTF_8.name());
		Locale locale = req != null ? req.getLocale() : Locale.getDefault();
		if (locale != null) {
			payload.put("blog_lang", locale.toLanguageTag());
		}
		String ip = getClientIp(req);
		if (StringUtils.isNotBlank(ip)) {
			payload.put("user_ip", ip);
		}
		if (req != null) {
			String ua = req.getHeader("User-Agent");
			if (StringUtils.isNotBlank(ua)) {
				payload.put("user_agent", ua);
			}
			String ref = req.getHeader("Referer");
			if (StringUtils.isNotBlank(ref)) {
				payload.put("referrer", ref);
			}
		}
		return payload;
	}

	private static boolean checkAkismetComment(Map<String, String> comment) {
		if (comment == null) {
			return false;
		}
		Optional<String> response = executeAkismetRequest("comment-check", comment);
		if (response.isEmpty()) {
			return false;
		}
		String body = response.get().trim();
		if ("invalid".equalsIgnoreCase(body)) {
			logger.warn("Akismet returned an invalid response for permalink {}", comment.get("permalink"));
			return false;
		}
		return "true".equalsIgnoreCase(body);
	}

	private static Optional<String> executeAkismetRequest(String action, Map<String, String> payload) {
		if (StringUtils.isBlank(CONF.akismetApiKey()) || payload == null || payload.isEmpty()) {
			return Optional.empty();
		}
		HttpPost post = new HttpPost(String.format("https://%s.rest.akismet.com/1.1/%s",
				CONF.akismetApiKey(), action));
		post.setHeader("User-Agent", AKISMET_USER_AGENT);
		post.setEntity(new UrlEncodedFormEntity(toFormParams(payload), StandardCharsets.UTF_8));
		try {
			return Optional.ofNullable(HttpUtils.getHttpClient().execute(post, response -> {
				String responseBody = response.getEntity() != null
						? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8) : "";
				if (response.getCode() != HttpStatus.SC_OK) {
					logger.warn("Akismet {} returned HTTP {} with body {}", action, response.getCode(), responseBody);
				}
				return responseBody;
			}));
		} catch (IOException ex) {
			logger.error("Failed to reach Akismet API for {}: {}", action, ex.getMessage());
			return Optional.empty();
		}
	}

	private static List<NameValuePair> toFormParams(Map<String, String> payload) {
		List<NameValuePair> params = new ArrayList<>(payload.size());
		payload.forEach((key, value) -> {
			if (value != null) {
				params.add(new BasicNameValuePair(key, value));
			}
		});
		return params;
	}

	private static String getClientIp(HttpServletRequest req) {
		if (req == null) {
			return null;
		}
		String ip = req.getHeader("X-Forwarded-For");
		if (StringUtils.isNotBlank(ip)) {
			return ip.split(",")[0].trim();
		}
		ip = req.getHeader("X-Real-IP");
		if (StringUtils.isNotBlank(ip)) {
			return ip;
		}
		return req.getRemoteAddr();
	}

}
