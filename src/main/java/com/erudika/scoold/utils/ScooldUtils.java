/*
 * Copyright 2013-2021 Erudika. https://erudika.com
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

import com.erudika.para.Para;
import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Address;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.Tag;
import com.erudika.para.core.User;
import com.erudika.para.core.Vote;
import com.erudika.para.core.Webhook;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.email.Emailer;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.erudika.para.validation.ValidationUtils;
import com.erudika.scoold.ScooldServer;
import static com.erudika.scoold.ScooldServer.*;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Feedback;
import com.erudika.scoold.core.Post;
import static com.erudika.scoold.core.Post.ALL_MY_SPACES;
import static com.erudika.scoold.core.Post.DEFAULT_SPACE;
import com.erudika.scoold.core.Profile;
import static com.erudika.scoold.core.Profile.Badge.ENTHUSIAST;
import static com.erudika.scoold.core.Profile.Badge.TEACHER;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.core.Revision;
import com.erudika.scoold.core.UnapprovedQuestion;
import com.erudika.scoold.core.UnapprovedReply;
import static com.erudika.scoold.utils.HttpUtils.getCookieValue;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.typesafe.config.ConfigObject;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.ws.rs.WebApplicationException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Component
@Named
public final class ScooldUtils {

	private static final Logger logger = LoggerFactory.getLogger(ScooldUtils.class);
	private static final Map<String, String> FILE_CACHE = new ConcurrentHashMap<String, String>();
	private static final Set<String> APPROVED_DOMAINS = new HashSet<>();
	private static final Set<String> ADMINS = new HashSet<>();
	private static final String EMAIL_ALERTS_PREFIX = "email-alerts" + Config.SEPARATOR;

	private static final Profile API_USER;
	private static final Set<String> CORE_TYPES;
	private static final Set<String> HOOK_EVENTS;
	private static final Map<String, String> WHITELISTED_MACROS;
	private static final Map<String, Object> API_KEYS = new LinkedHashMap<>(); // jti => jwt

	private List<Sysprop> allSpaces;

	static {
		API_USER = new Profile("1", "System");
		API_USER.setVotes(1);
		API_USER.setCreatorid("1");
		API_USER.setTimestamp(Utils.timestamp());
		API_USER.setPicture(getGravatar(Config.SUPPORT_EMAIL));
		API_USER.setGroups(User.Groups.ADMINS.toString());

		CORE_TYPES = new HashSet<>(Arrays.asList(Utils.type(Comment.class),
				Utils.type(Feedback.class),
				Utils.type(Profile.class),
				Utils.type(Question.class),
				Utils.type(Reply.class),
				Utils.type(Report.class),
				Utils.type(Revision.class),
				Utils.type(UnapprovedQuestion.class),
				Utils.type(UnapprovedReply.class),
				// Para core types
				Utils.type(Address.class),
				Utils.type(Sysprop.class),
				Utils.type(Tag.class),
				Utils.type(User.class),
				Utils.type(Vote.class)
		));

		HOOK_EVENTS = new HashSet<>(Arrays.asList(
				"question.create",
				"question.close",
				"answer.create",
				"answer.accept",
				"report.create",
				"comment.create",
				"user.signup",
				"revision.restore"));

		WHITELISTED_MACROS = new HashMap<String, String>();
		WHITELISTED_MACROS.put("spaces", "#spacespage($spaces)");
		WHITELISTED_MACROS.put("webhooks", "#webhookspage($webhooks)");
		WHITELISTED_MACROS.put("comments", "#commentspage($commentslist)");
		WHITELISTED_MACROS.put("simplecomments", "#simplecommentspage($commentslist)");
		WHITELISTED_MACROS.put("postcomments", "#commentspage($showpost.comments)");
		WHITELISTED_MACROS.put("replies", "#answerspage($answerslist $showPost)");
		WHITELISTED_MACROS.put("feedback", "#questionspage($feedbacklist)");
		WHITELISTED_MACROS.put("people", "#peoplepage($userlist)");
		WHITELISTED_MACROS.put("questions", "#questionspage($questionslist)");
		WHITELISTED_MACROS.put("compactanswers", "#compactanswerspage($answerslist)");
		WHITELISTED_MACROS.put("answers", "#answerspage($answerslist)");
		WHITELISTED_MACROS.put("reports", "#reportspage($reportslist)");
		WHITELISTED_MACROS.put("revisions", "#revisionspage($revisionslist $showPost)");
		WHITELISTED_MACROS.put("tags", "#tagspage($tagslist)");
	}

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
		// multiple domains/admins are now allowed only in Scoold PRO
		String approvedDomains = Config.getConfigParam("approved_domains_for_signups", "");
		if (!StringUtils.isBlank(approvedDomains)) {
			APPROVED_DOMAINS.add(approvedDomains);
		}
		// multiple admins are now allowed only in Scoold PRO
		String admins = Config.getConfigParam("admins", "");
		if (!StringUtils.isBlank(admins)) {
			ADMINS.add(admins);
		}
	}

	public static void tryConnectToPara(Callable<Boolean> callable) {
		retryConnection(callable, 0);
	}

	private static void retryConnection(Callable<Boolean> callable, int retryCount) {
		try {
			if (!callable.call()) {
				throw new Exception();
			} else if (retryCount > 0) {
				logger.info("Connected to Para backend.");
			}
		} catch (Exception e) {
			int maxRetries = Config.getConfigInt("connection_retries_max", 10);
			int retryInterval = Config.getConfigInt("connection_retry_interval_sec", 10);
			int count = ++retryCount;
			logger.error("No connection to Para backend. Retrying connection in {}s (attempt {} of {})...",
					retryInterval, count, maxRetries);
			if (maxRetries < 0 || retryCount < maxRetries) {
				Para.asyncExecute(new Runnable() {
					public void run() {
						try {
							Thread.sleep(retryInterval * 1000L);
						} catch (InterruptedException ex) {
							logger.error(null, ex);
							Thread.currentThread().interrupt();
						}
						retryConnection(callable, count);
					}
				});
			}
		}
	}

	public ParaObject checkAuth(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		Profile authUser = null;
		String jwt = HttpUtils.getStateParam(AUTH_COOKIE, req);
		if (isApiRequest(req)) {
			return checkApiAuth(req);
		} else if (jwt != null && !StringUtils.endsWithAny(req.getRequestURI(),
				".js", ".css", ".svg", ".png", ".jpg", ".ico", ".gif", ".woff2", ".woff", "people/avatar")) {
			User u = pc.me(jwt);
			if (u != null && isEmailDomainApproved(u.getEmail())) {
				authUser = getOrCreateProfile(u, req);
				authUser.setUser(u);
				authUser.setOriginalPicture(u.getPicture());
				boolean updatedRank = promoteOrDemoteUser(authUser, u);
				boolean updatedProfile = updateProfilePictureAndName(authUser, u);
				if (updatedRank || updatedProfile) {
					authUser.update();
				}
			} else {
				clearSession(req, res);
				logger.info("Invalid JWT found in cookie {}.", AUTH_COOKIE);
				res.sendRedirect(getServerURL() + CONTEXT_PATH + SIGNINLINK + "?code=3&error=true");
				return null;
			}
		}
		return authUser;
	}

	private ParaObject checkApiAuth(HttpServletRequest req) {
		if (req.getRequestURI().equals(CONTEXT_PATH + "/api")) {
			return null;
		}
		String apiKeyJWT = StringUtils.removeStart(req.getHeader(HttpHeaders.AUTHORIZATION), "Bearer ");
		if (req.getRequestURI().equals(CONTEXT_PATH + "/api/ping")) {
			return API_USER;
		} else if (req.getRequestURI().equals(CONTEXT_PATH + "/api/stats") && isValidJWToken(apiKeyJWT)) {
			return API_USER;
		} else if (!isApiEnabled() || StringUtils.isBlank(apiKeyJWT) || !isValidJWToken(apiKeyJWT)) {
			throw new WebApplicationException(401);
		}
		return API_USER;
	}

	private boolean promoteOrDemoteUser(Profile authUser, User u) {
		if (authUser != null) {
			if (!isAdmin(authUser) && isRecognizedAsAdmin(u)) {
				logger.info("User '{}' with id={} promoted to admin.", u.getName(), authUser.getId());
				authUser.setGroups(User.Groups.ADMINS.toString());
				return true;
			} else if (isAdmin(authUser) && !isRecognizedAsAdmin(u)) {
				logger.info("User '{}' with id={} demoted to regular user.", u.getName(), authUser.getId());
				authUser.setGroups(User.Groups.USERS.toString());
				return true;
			} else if (!isMod(authUser) && u.isModerator()) {
				authUser.setGroups(User.Groups.MODS.toString());
				return true;
			}
		}
		return false;
	}

	private Profile getOrCreateProfile(User u, HttpServletRequest req) {
		Profile authUser = pc.read(Profile.id(u.getId()));
		if (authUser == null) {
			authUser = Profile.fromUser(u);
			authUser.create();
			if (!u.getIdentityProvider().equals("generic")) {
				sendWelcomeEmail(u, false, req);
			}
			Map<String, Object> payload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(authUser, false));
			payload.put("user", u);
			triggerHookEvent("user.signup", payload);
			logger.info("Created new user '{}' with id={}, groups={}, spaces={}.",
					u.getName(), authUser.getId(), authUser.getGroups(), authUser.getSpaces());
		}
		return authUser;
	}

	private boolean updateProfilePictureAndName(Profile authUser, User u) {
		boolean update = false;
		if (!StringUtils.equals(u.getPicture(), authUser.getPicture())
				&& !StringUtils.contains(authUser.getPicture(), "gravatar.com")
				&& !Config.getConfigBoolean("avatar_edits_enabled", true)) {
			authUser.setPicture(u.getPicture());
			update = true;
		}
		if (!Config.getConfigBoolean("name_edits_enabled", true) &&
				!StringUtils.equals(u.getName(), authUser.getName())) {
			authUser.setName(u.getName());
			update = true;
		}
		if (!StringUtils.equals(u.getName(), authUser.getOriginalName())) {
			authUser.setOriginalName(u.getName());
			update = true;
		}
		return update;
	}

	public boolean isDarkModeEnabled(Profile authUser, HttpServletRequest req) {
		return (authUser != null && authUser.getDarkmodeEnabled()) ||
				"1".equals(HttpUtils.getCookieValue(req, "dark-mode"));
	}

	public void sendWelcomeEmail(User user, boolean verifyEmail, HttpServletRequest req) {
		// send welcome email notification
		if (user != null) {
			Map<String, Object> model = new HashMap<String, Object>();
			Map<String, String> lang = getLang(req);
			String subject = Utils.formatMessage(lang.get("signin.welcome"), Config.APP_NAME);
			String body1 = Utils.formatMessage(Config.getConfigParam("emails.welcome_text1",
					lang.get("signin.welcome.body1") + "<br><br>"), Config.APP_NAME);
			String body2 = Config.getConfigParam("emails.welcome_text2", lang.get("signin.welcome.body2") + "<br><br>");
			String body3 = Utils.formatMessage(Config.getConfigParam("emails.welcome_text3", "Best, <br>The {0} team<br><br>"),
					Config.APP_NAME);

			if (verifyEmail && !user.getActive() && !StringUtils.isBlank(user.getIdentifier())) {
				Sysprop s = pc.read(user.getIdentifier());
				if (s != null) {
					String token = Utils.base64encURL(Utils.generateSecurityToken().getBytes());
					s.addProperty(Config._EMAIL_TOKEN, token);
					pc.update(s);
					token = getServerURL() + CONTEXT_PATH + SIGNINLINK + "/register?id=" + user.getId() + "&token=" + token;
					body3 = "<b><a href=\"" + token + "\">" + lang.get("signin.welcome.verify") + "</a></b><br><br>" + body3;
				}
			}

			model.put("logourl", Config.getConfigParam("small_logo_url", "https://scoold.com/logo.png"));
			model.put("heading", Utils.formatMessage(lang.get("signin.welcome.title"), user.getName()));
			model.put("body", body1 + body2 + body3);
			emailer.sendEmail(Arrays.asList(user.getEmail()), subject, compileEmailTemplate(model));
		}
	}

	public void sendVerificationEmail(String email, HttpServletRequest req) {
		if (!StringUtils.isBlank(email)) {
			Map<String, Object> model = new HashMap<String, Object>();
			Map<String, String> lang = getLang(req);
			String subject = Utils.formatMessage(lang.get("signin.welcome"), Config.APP_NAME);
			String body = Utils.formatMessage(Config.getConfigParam("emails.welcome_text3", "Best, <br>The {0} team<br><br>"),
					Config.APP_NAME);

			Sysprop s = pc.read(email);
			if (s != null) {
				String token = Utils.base64encURL(Utils.generateSecurityToken().getBytes());
				s.addProperty(Config._EMAIL_TOKEN, token);
				pc.update(s);
				token = getServerURL() + CONTEXT_PATH + SIGNINLINK + "/register?id=" + s.getCreatorid() + "&token=" + token;
				body = "<b><a href=\"" + token + "\">" + lang.get("signin.welcome.verify") + "</a></b><br><br>" + body;
			}

			model.put("logourl", Config.getConfigParam("small_logo_url", "https://scoold.com/logo.png"));
			model.put("heading", lang.get("hello"));
			model.put("body", body);
			emailer.sendEmail(Arrays.asList(email), subject, compileEmailTemplate(model));
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
			String body3 = "Best, <br>The " + Config.APP_NAME + " team<br><br>";

			model.put("logourl", Config.getConfigParam("small_logo_url", "https://scoold.com/logo.png"));
			model.put("heading", lang.get("hello"));
			model.put("body", body1 + body2 + body3);
			emailer.sendEmail(Arrays.asList(email), subject, compileEmailTemplate(model));
		}
	}

	@SuppressWarnings("unchecked")
	public void subscribeToNotifications(String email, String channelId) {
		if (!StringUtils.isBlank(email) && !StringUtils.isBlank(channelId)) {
			Sysprop s = pc.read(channelId);
			if (s == null || !s.hasProperty("emails")) {
				s = new Sysprop(channelId);
				s.addProperty("emails", new LinkedList<>());
			}
			Set<String> emails = new HashSet<>((List<String>) s.getProperty("emails"));
			if (emails.add(email)) {
				s.addProperty("emails", emails);
				pc.create(s);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void unsubscribeFromNotifications(String email, String channelId) {
		if (!StringUtils.isBlank(email) && !StringUtils.isBlank(channelId)) {
			Sysprop s = pc.read(channelId);
			if (s == null || !s.hasProperty("emails")) {
				s = new Sysprop(channelId);
				s.addProperty("emails", new LinkedList<>());
			}
			Set<String> emails = new HashSet<>((List<String>) s.getProperty("emails"));
			if (emails.remove(email)) {
				s.addProperty("emails", emails);
				pc.create(s);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public Set<String> getNotificationSubscribers(String channelId) {
		return ((List<String>) Optional.ofNullable(((Sysprop) pc.read(channelId))).
				orElse(new Sysprop()).getProperties().getOrDefault("emails", Collections.emptyList())).
				stream().collect(Collectors.toSet());
	}

	public void unsubscribeFromAllNotifications(Profile p) {
		User u = p.getUser();
		if (u != null) {
			unsubscribeFromNewPosts(u);
		}
	}

	public boolean isEmailDomainApproved(String email) {
		if (StringUtils.isBlank(email)) {
			return false;
		}
		if (!APPROVED_DOMAINS.isEmpty() && !APPROVED_DOMAINS.contains(StringUtils.substringAfter(email, "@"))) {
			logger.warn("Attempted signin from an unknown domain - email {} is part of an unapproved domain.", email);
			return false;
		}
		return true;
	}

	public Object isSubscribedToNewPosts(HttpServletRequest req) {
		Profile authUser = getAuthUser(req);
		if (authUser != null) {
			User u = authUser.getUser();
			if (u != null) {
				return getNotificationSubscribers(EMAIL_ALERTS_PREFIX + "new_post_subscribers").contains(u.getEmail());
			}
		}
		return false;
	}

	public void subscribeToNewPosts(User u) {
		if (u != null) {
			subscribeToNotifications(u.getEmail(), EMAIL_ALERTS_PREFIX + "new_post_subscribers");
		}
	}

	public void unsubscribeFromNewPosts(User u) {
		if (u != null) {
			unsubscribeFromNotifications(u.getEmail(), EMAIL_ALERTS_PREFIX + "new_post_subscribers");
		}
	}

	private Map<String, Profile> buildProfilesMap(List<User> users) {
		if (users != null && !users.isEmpty()) {
			Map<String, User> userz = users.stream().collect(Collectors.toMap(u -> u.getId(), u -> u));
			List<Profile> profiles = pc.readAll(userz.keySet().stream().
					map(uid -> Profile.id(uid)).collect(Collectors.toList()));
			Map<String, Profile> profilesMap = new HashMap<String, Profile>(users.size());
			profiles.forEach(pr -> profilesMap.put(userz.get(pr.getCreatorid()).getEmail(), pr));
			return profilesMap;
		}
		return Collections.emptyMap();
	}

	private void sendEmailsToSubscribersInSpace(Set<String> emails, String space, String subject, String html) {
		int i = 0;
		int max = Config.MAX_ITEMS_PER_PAGE;
		List<String> terms = new ArrayList<>(max);
		for (String email : emails) {
			terms.add(email);
			if (++i == max) {
				emailer.sendEmail(buildProfilesMap(pc.findTermInList(Utils.type(User.class), Config._EMAIL, terms)).
						entrySet().stream().filter(e -> canAccessSpace(e.getValue(), space) &&
								!isIgnoredSpaceForNotifications(e.getValue(), space)).
						map(e -> e.getKey()).collect(Collectors.toList()), subject, html);
				i = 0;
				terms.clear();
			}
		}
		if (!terms.isEmpty()) {
			emailer.sendEmail(buildProfilesMap(pc.findTermInList(Utils.type(User.class), Config._EMAIL, terms)).
					entrySet().stream().filter(e -> canAccessSpace(e.getValue(), space) &&
							!isIgnoredSpaceForNotifications(e.getValue(), space)).
					map(e -> e.getKey()).collect(Collectors.toList()), subject, html);
		}
	}

	private Set<String> getFavTagsSubscribers(List<String> tags) {
		if (!tags.isEmpty()) {
			Set<String> emails = new LinkedHashSet<>();
			// find all user objects even if there are more than 10000 users in the system
			Pager pager = new Pager(1, "_docid", false, Config.MAX_ITEMS_PER_PAGE);
			List<Profile> profiles;
			do {
				profiles = pc.findQuery(Utils.type(Profile.class),
						"properties.favtags:(" + tags.stream().
								map(t -> "\"".concat(t).concat("\"")).distinct().
								collect(Collectors.joining(" ")) + ") AND properties.favtagsEmailsEnabled:true", pager);
				if (!profiles.isEmpty()) {
					List<User> users = pc.readAll(profiles.stream().map(p -> p.getCreatorid()).
							distinct().collect(Collectors.toList()));

					users.stream().forEach(u -> emails.add(u.getEmail()));
				}
			} while (!profiles.isEmpty());
			return emails;
		}
		return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public void sendUpdatedFavTagsNotifications(Post question, List<String> addedTags) {
		// sends a notification to subscibers of a tag if that tag was added to an existing question
		if (question != null && !question.isReply() && addedTags != null && !addedTags.isEmpty()) {
			Profile postAuthor = question.getAuthor(); // the current user - same as utils.getAuthUser(req)
			Map<String, Object> model = new HashMap<String, Object>();
			String name = postAuthor.getName();
			String body = Utils.markdownToHtml(question.getBody());
			String picture = Utils.formatMessage("<img src='{0}' width='25'>", postAuthor.getPicture());
			String postURL = getServerURL() + question.getPostLink(false, false);
			String tagsString = Optional.ofNullable(question.getTags()).orElse(Collections.emptyList()).stream().
					map(t -> "<span class=\"tag\">" + (addedTags.contains(t) ? "<b>" + t + "<b>" : t) + "</span>").
					collect(Collectors.joining("&nbsp;"));
			model.put("logourl", Config.getConfigParam("small_logo_url", "https://scoold.com/logo.png"));
			model.put("heading", Utils.formatMessage("{0} {1} edited:", picture, name));
			model.put("body", Utils.formatMessage("<h2><a href='{0}'>{1}</a></h2><div>{2}</div><br>{3}",
					postURL, question.getTitle(), body, tagsString));

			Set<String> emails = getFavTagsSubscribers(addedTags);
			sendEmailsToSubscribersInSpace(emails, question.getSpace(),
					name + " edited question '" + Utils.abbreviate(question.getTitle(), 255) + "'",
					compileEmailTemplate(model));
		}
	}

	@SuppressWarnings("unchecked")
	public void sendNewPostNotifications(Post question) {
		if (question == null) {
			return;
		}
		// the current user - same as utils.getAuthUser(req)
		Profile postAuthor = question.getAuthor() != null ? question.getAuthor() : pc.read(question.getCreatorid());
		if (!question.getType().equals(Utils.type(UnapprovedQuestion.class))) {
			Map<String, Object> model = new HashMap<String, Object>();
			String name = postAuthor.getName();
			String body = Utils.markdownToHtml(question.getBody());
			String picture = Utils.formatMessage("<img src='{0}' width='25'>", postAuthor.getPicture());
			String postURL = getServerURL() + question.getPostLink(false, false);
			String tagsString = Optional.ofNullable(question.getTags()).orElse(Collections.emptyList()).stream().
					map(t -> "<span class=\"tag\">" + t + "</span>").
					collect(Collectors.joining("&nbsp;"));
			model.put("logourl", Config.getConfigParam("small_logo_url", "https://scoold.com/logo.png"));
			model.put("heading", Utils.formatMessage("{0} {1} posted:", picture, name));
			model.put("body", Utils.formatMessage("<h2><a href='{0}'>{1}</a></h2><div>{2}</div><br>{3}",
					postURL, question.getTitle(), body, tagsString));

			Set<String> emails = new HashSet<String>(getNotificationSubscribers(EMAIL_ALERTS_PREFIX + "new_post_subscribers"));
			emails.addAll(getFavTagsSubscribers(question.getTags()));
			sendEmailsToSubscribersInSpace(emails, question.getSpace(),
					name + " posted the question '" + Utils.abbreviate(question.getTitle(), 255) + "'",
					compileEmailTemplate(model));
		} else if (postsNeedApproval() && question instanceof UnapprovedQuestion) {
			Report rep = new Report();
			rep.setDescription("New question awaiting approval");
			rep.setSubType(Report.ReportType.OTHER);
			rep.setLink(question.getPostLink(false, false));
			rep.setAuthorName(postAuthor.getName());
			rep.create();
		}
	}

	public void sendReplyNotifications(Post parentPost, Post reply) {
		// send email notification to author of post except when the reply is by the same person
		if (parentPost != null && reply != null && !StringUtils.equals(parentPost.getCreatorid(), reply.getCreatorid())) {
			Profile replyAuthor = reply.getAuthor(); // the current user - same as utils.getAuthUser(req)
			Map<String, Object> model = new HashMap<String, Object>();
			String name = replyAuthor.getName();
			String body = Utils.markdownToHtml(reply.getBody());
			String picture = Utils.formatMessage("<img src='{0}' width='25'>", replyAuthor.getPicture());
			String postURL = getServerURL() + parentPost.getPostLink(false, false);
			model.put("logourl", Config.getConfigParam("small_logo_url", "https://scoold.com/logo.png"));
			model.put("heading", Utils.formatMessage("New reply to <a href='{0}'>{1}</a>", postURL, parentPost.getTitle()));
			model.put("body", Utils.formatMessage("<h2>{0} {1}:</h2><div>{2}</div>", picture, name, body));

			Profile authorProfile = pc.read(parentPost.getCreatorid());
			if (authorProfile != null) {
				User author = authorProfile.getUser();
				if (author != null) {
					if (authorProfile.getReplyEmailsEnabled()) {
						parentPost.addFollower(author);
					}
				}
			}

			if (postsNeedApproval() && reply instanceof UnapprovedReply) {
				Report rep = new Report();
				rep.setDescription("New reply awaiting approval");
				rep.setSubType(Report.ReportType.OTHER);
				rep.setLink(parentPost.getPostLink(false, false) + "#post-" + reply.getId());
				rep.setAuthorName(reply.getAuthor().getName());
				rep.create();
			}

			if (parentPost.hasFollowers()) {
				emailer.sendEmail(new ArrayList<String>(parentPost.getFollowers().values()),
						name + " replied to '" + Utils.abbreviate(reply.getTitle(), 255) + "'",
						compileEmailTemplate(model));
			}
		}
	}

	public void sendCommentNotification(Post parentPost, Comment comment, Profile commentAuthor) {
		// send email notification to author of post except when the comment is by the same person
		if (parentPost != null && comment != null) {
			parentPost.setAuthor(pc.read(Profile.id(parentPost.getCreatorid()))); // parent author is not current user (authUser)
			// get the last 5-6 commentators who want to be notified - https://github.com/Erudika/scoold/issues/201
			Pager p = new Pager(1, Config._TIMESTAMP, false, 5);
			boolean isCommentatorThePostAuthor = StringUtils.equals(parentPost.getCreatorid(), comment.getCreatorid());
			Set<String> last5ids = pc.findChildren(parentPost, Utils.type(Comment.class),
					"!(" + Config._CREATORID + ":\"" + comment.getCreatorid() + "\")", p).
					stream().map(c -> c.getCreatorid()).distinct().collect(Collectors.toSet());
			if (!isCommentatorThePostAuthor && !last5ids.contains(parentPost.getCreatorid())) {
				last5ids = new HashSet<>(last5ids);
				last5ids.add(parentPost.getCreatorid());
			}
			List<Profile> last5commentators = pc.readAll(new ArrayList<>(last5ids));
			last5commentators = last5commentators.stream().filter(u -> u.getCommentEmailsEnabled()).collect(Collectors.toList());
			pc.readAll(last5commentators.stream().map(u -> u.getCreatorid()).collect(Collectors.toList())).forEach(author -> {
				Map<String, Object> model = new HashMap<String, Object>();
				String name = commentAuthor.getName();
				String body = Utils.markdownToHtml(comment.getComment());
				String pic = Utils.formatMessage("<img src='{0}' width='25'>", commentAuthor.getPicture());
				String postURL = getServerURL() + parentPost.getPostLink(false, false);
				model.put("logourl", Config.getConfigParam("small_logo_url", "https://scoold.com/logo.png"));
				model.put("heading", Utils.formatMessage("New comment on <a href='{0}'>{1}</a>", postURL, parentPost.getTitle()));
				model.put("body", Utils.formatMessage("<h2>{0} {1}:</h2><div class='panel'>{2}</div>", pic, name, body));
				emailer.sendEmail(Arrays.asList(((User) author).getEmail()), name + " commented on '" +
						parentPost.getTitle() + "'", compileEmailTemplate(model));

				Map<String, Object> payload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(comment, false));
				payload.put("parent", parentPost);
				payload.put("author", commentAuthor);
				triggerHookEvent("comment.create", payload);
			});
		}
	}

	public Profile readAuthUser(HttpServletRequest req) {
		Profile authUser = null;
		User u = pc.me(HttpUtils.getStateParam(AUTH_COOKIE, req));
		if (u != null && isEmailDomainApproved(u.getEmail())) {
			return getOrCreateProfile(u, req);
		}
		return authUser;
	}

	public Profile getAuthUser(HttpServletRequest req) {
		return (Profile) req.getAttribute(AUTH_USER_ATTRIBUTE);
	}

	public boolean isAuthenticated(HttpServletRequest req) {
		return getAuthUser(req) != null;
	}

	public boolean isNearMeFeatureEnabled() {
		return Config.getConfigBoolean("nearme_feature_enabled", !Config.getConfigParam("gmaps_api_key", "").isEmpty());
	}

	public boolean isFeedbackEnabled() {
		return Config.getConfigBoolean("feedback_enabled", false);
	}

	public boolean isDefaultSpacePublic() {
		return Config.getConfigBoolean("is_default_space_public", true);
	}

	public boolean isWebhooksEnabled() {
		return Config.getConfigBoolean("webhooks_enabled", true);
	}

	public boolean isAnonymityEnabled() {
		return Config.getConfigBoolean("profile_anonimity_enabled", false);
	}

	public boolean isApiEnabled() {
		return Config.getConfigBoolean("api_enabled", false);
	}

	public boolean isFooterLinksEnabled() {
		return Config.getConfigBoolean("footer_links_enabled", true);
	}

	public boolean isDarkModeEnabled() {
		return Config.getConfigBoolean("dark_mode_enabled", true);
	}

	public boolean isAvatarValidationEnabled() {
		return Config.getConfigBoolean("avatar_validation_enabled", false); // this should be deleted in the future
	}

	public static boolean isGravatarEnabled() {
		return Config.getConfigBoolean("gravatars_enabled", true);
	}

	public String getFooterHTML() {
		return Config.getConfigParam("footer_html", "");
	}

	public boolean isNavbarLink1Enabled() {
		return !StringUtils.isBlank(getNavbarLink1URL());
	}

	public String getNavbarLink1URL() {
		return Config.getConfigParam("navbar_link1_url", "");
	}

	public String getNavbarLink1Text() {
		return Config.getConfigParam("navbar_link1_text", "Link1");
	}

	public boolean isNavbarLink2Enabled() {
		return !StringUtils.isBlank(getNavbarLink2URL());
	}

	public String getNavbarLink2URL() {
		return Config.getConfigParam("navbar_link2_url", "");
	}

	public String getNavbarLink2Text() {
		return Config.getConfigParam("navbar_link2_text", "Link2");
	}

	public boolean isNavbarMenuLink1Enabled() {
		return !StringUtils.isBlank(getNavbarMenuLink1URL());
	}

	public String getNavbarMenuLink1URL() {
		return Config.getConfigParam("navbar_menu_link1_url", "");
	}

	public String getNavbarMenuLink1Text() {
		return Config.getConfigParam("navbar_menu_link1_text", "Menu Link1");
	}

	public boolean isNavbarMenuLink2Enabled() {
		return !StringUtils.isBlank(getNavbarMenuLink2URL());
	}

	public String getNavbarMenuLink2URL() {
		return Config.getConfigParam("navbar_menu_link2_url", "");
	}

	public String getNavbarMenuLink2Text() {
		return Config.getConfigParam("navbar_menu_link2_text", "Menu Link2");
	}

	public Set<String> getCoreScooldTypes() {
		return Collections.unmodifiableSet(CORE_TYPES);
	}

	public Set<String> getCustomHookEvents() {
		return Collections.unmodifiableSet(HOOK_EVENTS);
	}

	public Pager getPager(String pageParamName, HttpServletRequest req) {
		return pagerFromParams(pageParamName, req);
	}

	public Pager pagerFromParams(HttpServletRequest req) {
		return pagerFromParams("page", req);
	}

	public Pager pagerFromParams(String pageParamName, HttpServletRequest req) {
		Pager p = new Pager();
		p.setPage(Math.min(NumberUtils.toLong(req.getParameter(pageParamName), 1), Config.MAX_PAGES));
		p.setLimit(NumberUtils.toInt(req.getParameter("limit"), Config.MAX_ITEMS_PER_PAGE));
		String lastKey = req.getParameter("lastKey");
		String sort = req.getParameter("sortby");
		String desc = req.getParameter("desc");
		if (!StringUtils.isBlank(desc)) {
			p.setDesc(Boolean.parseBoolean(desc));
		}
		if (!StringUtils.isBlank(lastKey)) {
			p.setLastKey(lastKey);
		}
		if (!StringUtils.isBlank(sort)) {
			p.setSortby(sort);
		}
		return p;
	}

	public String getLanguageCode(HttpServletRequest req) {
		String langCodeFromConfig = Config.getConfigParam("default_language_code", "");
		String cookieLoc = getCookieValue(req, LOCALE_COOKIE);
		Locale requestLocale = langutils.getProperLocale(req.getLocale().toString());
		return (cookieLoc != null) ? cookieLoc : (StringUtils.isBlank(langCodeFromConfig) ?
				requestLocale.getLanguage() : langutils.getProperLocale(langCodeFromConfig).getLanguage());
	}

	public Locale getCurrentLocale(String langname) {
		Locale currentLocale = langutils.getProperLocale(langname);
		if (currentLocale == null) {
			currentLocale = langutils.getProperLocale(langutils.getDefaultLanguageCode());
		}
		return currentLocale;
	}

	public Map<String, String> getLang(HttpServletRequest req) {
		return getLang(getCurrentLocale(getLanguageCode(req)));
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
		// add system profile
		authors.put(API_USER.getId(), API_USER);
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
			long clSize = (cl == null) ? 0 : cl.size();
			if (post.getCommentIds().size() != clSize) {
				forUpdate.add(reloadFirstPageOfComments(post));
				clSize = post.getComments().size();
			} else {
				post.setComments(cl);
			}
			post.getItemcount().setCount(clSize + 1L); // hack to show the "more" button
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
			String postviews = StringUtils.trimToEmpty(HttpUtils.getStateParam("postviews", req));
			if (!StringUtils.contains(postviews, showPost.getId())) {
				long views = (showPost.getViewcount() == null) ? 0 : showPost.getViewcount();
				showPost.setViewcount(views + 1); //increment count
				HttpUtils.setStateParam("postviews", (postviews.isEmpty() ? "" : postviews + ".") + showPost.getId(),
						req, res);
				pc.update(showPost);
			}
		}
	}

	public List<Post> getSimilarPosts(Post showPost, Pager pager) {
		List<Post> similarquestions = Collections.emptyList();
		if (!showPost.isReply()) {
			String likeTxt = Utils.stripAndTrim((showPost.getTitle() + " " + showPost.getBody()));
			if (likeTxt.length() > 1000) {
				// read object on the server to prevent "URI too long" errors
				similarquestions = pc.findSimilar(showPost.getType(), showPost.getId(),
						new String[]{"properties.title", "properties.body", "properties.tags"},
						"id:" + showPost.getId(), pager);
			} else if (!StringUtils.isBlank(likeTxt)) {
				similarquestions = pc.findSimilar(showPost.getType(), showPost.getId(),
						new String[]{"properties.title", "properties.body", "properties.tags"}, likeTxt, pager);
			}
		}
		return similarquestions;
	}

	public String getFirstLinkInPost(String postBody) {
		postBody = StringUtils.trimToEmpty(postBody);
		Pattern p = Pattern.compile("^!?\\[.*\\]\\((.+)\\)");
		Matcher m = p.matcher(postBody);

		if (m.find()) {
			return m.group(1);
		}
		return "";
	}

	public boolean param(HttpServletRequest req, String param) {
		return req.getParameter(param) != null;
	}

	public boolean isAjaxRequest(HttpServletRequest req) {
		return req.getHeader("X-Requested-With") != null || req.getParameter("X-Requested-With") != null;
	}

	public boolean isApiRequest(HttpServletRequest req) {
		return req.getRequestURI().startsWith(CONTEXT_PATH + "/api/") || req.getRequestURI().equals(CONTEXT_PATH + "/api");
	}

	public boolean isAdmin(Profile authUser) {
		return authUser != null && User.Groups.ADMINS.toString().equals(authUser.getGroups());
	}

	public boolean isMod(Profile authUser) {
		return authUser != null && (isAdmin(authUser) || User.Groups.MODS.toString().equals(authUser.getGroups()));
	}

	public boolean isRecognizedAsAdmin(User u) {
		return u.isAdmin() || ADMINS.contains(u.getIdentifier()) ||
				ADMINS.stream().filter(s -> s.equalsIgnoreCase(u.getEmail())).findAny().isPresent();
	}

	public boolean canComment(Profile authUser, HttpServletRequest req) {
		return isAuthenticated(req) && ((authUser.hasBadge(ENTHUSIAST) ||
				Config.getConfigBoolean("new_users_can_comment", true) ||
				isMod(authUser)));
	}

	public boolean postsNeedApproval() {
		return Config.getConfigBoolean("posts_need_approval", false);
	}

	public boolean postNeedsApproval(Profile authUser) {
		return postsNeedApproval() &&
				authUser.getVotes() < Config.getConfigInt("posts_rep_threshold", ENTHUSIAST_IFHAS) &&
				!isMod(authUser);
	}

	public String getWelcomeMessage(Profile authUser) {
		return authUser == null ? Config.getConfigParam("welcome_message", "") : "";
	}

	public String getWelcomeMessageOnLogin(Profile authUser) {
		if (authUser == null) {
			return "";
		}
		String welcomeMsgOnlogin = Config.getConfigParam("welcome_message_onlogin", "");
		if (StringUtils.contains(welcomeMsgOnlogin, "{{")) {
			welcomeMsgOnlogin = Utils.compileMustache(Collections.singletonMap("user",
					ParaObjectUtils.getAnnotatedFields(authUser, false)), welcomeMsgOnlogin);
		}
		return welcomeMsgOnlogin;
	}

	public boolean isDefaultSpace(String space) {
		return DEFAULT_SPACE.equalsIgnoreCase(getSpaceId(space));
	}

	public String getDefaultSpace() {
		return DEFAULT_SPACE;
	}

	public boolean isAllSpaces(String space) {
		return ALL_MY_SPACES.equalsIgnoreCase(getSpaceId(space));
	}

	public List<Sysprop> getAllSpaces() {
		if (allSpaces == null || allSpaces.isEmpty()) {
			allSpaces = new LinkedList<>(pc.findQuery("scooldspace", "*", new Pager(Config.DEFAULT_LIMIT)));
		}
		return allSpaces;
	}

	public boolean canAccessSpace(Profile authUser, String targetSpaceId) {
		if (authUser == null) {
			return isDefaultSpacePublic() && isDefaultSpace(targetSpaceId);
		}
		if (isMod(authUser) || isAllSpaces(targetSpaceId)) {
			return true;
		}
		if (StringUtils.isBlank(targetSpaceId) || targetSpaceId.length() < 2) {
			return false;
		}
		// this is confusing - let admins control who is in the default space
		//if (isDefaultSpace(targetSpaceId)) {
		//	// can user access the default space (blank)
		//	return isDefaultSpacePublic() || isMod(authUser) || !authUser.hasSpaces();
		//}
		boolean isMemberOfSpace = false;
		for (String space : authUser.getSpaces()) {
			String spaceId = getSpaceId(targetSpaceId);
			if (StringUtils.startsWithIgnoreCase(space, spaceId + Config.SEPARATOR) || space.equalsIgnoreCase(spaceId)) {
				isMemberOfSpace = true;
				break;
			}
		}
		return isMemberOfSpace;
	}

	private boolean isIgnoredSpaceForNotifications(Profile profile, String space) {
		return profile != null && !profile.getFavspaces().isEmpty() && !profile.getFavspaces().contains(getSpaceId(space));
	}

	public String getSpaceIdFromCookie(Profile authUser, HttpServletRequest req) {
		if (isAdmin(authUser) && req.getParameter("space") != null) {
			Sysprop s = pc.read(getSpaceId(req.getParameter("space"))); // API override
			if (s != null) {
				return s.getId() + Config.SEPARATOR + s.getName();
			}
		}
		String spaceAttr = (String) req.getAttribute(SPACE_COOKIE);
		String spaceValue = StringUtils.isBlank(spaceAttr) ? Utils.base64dec(getCookieValue(req, SPACE_COOKIE)) : spaceAttr;
		String space = getValidSpaceId(authUser, spaceValue);
		return (isAllSpaces(space) && isMod(authUser)) ? DEFAULT_SPACE : verifyExistingSpace(authUser, space);
	}

	public void storeSpaceIdInCookie(String space, HttpServletRequest req, HttpServletResponse res) {
		// directly set the space on the requests, overriding the cookie value
		// used for setting the space from a direct URL to a particular space
		req.setAttribute(SPACE_COOKIE, space);
		HttpUtils.setRawCookie(SPACE_COOKIE, Utils.base64encURL(space.getBytes()),
				req, res, false, StringUtils.isBlank(space) ? 0 : 365 * 24 * 60 * 60);
	}

	public String verifyExistingSpace(Profile authUser, String space) {
		if (!isDefaultSpace(space) && !isAllSpaces(space)) {
			Sysprop s = pc.read(getSpaceId(space));
			if (s == null) {
				if (authUser != null) {
					authUser.removeSpace(space);
					pc.update(authUser);
				}
				return DEFAULT_SPACE;
			} else {
				return s.getId() + Config.SEPARATOR + s.getName(); // updates current space name in case it was renamed
			}
		}
		return space;
	}

	public String getValidSpaceIdExcludingAll(Profile authUser, String space, HttpServletRequest req) {
		String s = StringUtils.isBlank(space) ? getSpaceIdFromCookie(authUser, req) : space;
		return isAllSpaces(s) ? getValidSpaceId(authUser, "x") : s;
	}

	private String getValidSpaceId(Profile authUser, String space) {
		if (authUser == null) {
			return DEFAULT_SPACE;
		}
		String defaultSpace = authUser.hasSpaces() ? authUser.getSpaces().iterator().next() : DEFAULT_SPACE;
		String s = canAccessSpace(authUser, space) ? space : defaultSpace;
		return StringUtils.isBlank(s) ? DEFAULT_SPACE : s;
	}

	public String getSpaceName(String space) {
		if (DEFAULT_SPACE.equalsIgnoreCase(space)) {
			return "";
		}
		return RegExUtils.replaceAll(space, "^scooldspace:[^:]+:", "");
	}

	public String getSpaceId(String space) {
		if (StringUtils.isBlank(space)) {
			return DEFAULT_SPACE;
		}
		String s = StringUtils.contains(space, Config.SEPARATOR) ?
				StringUtils.substring(space, 0, space.lastIndexOf(Config.SEPARATOR)) : "scooldspace:" + space;
		return "scooldspace".equals(s) ? space : s;
	}

	public String getSpaceFilteredQuery(Profile authUser, String currentSpace) {
		return canAccessSpace(authUser, currentSpace) ? getSpaceFilter(authUser, currentSpace) : "";
	}

	public String getSpaceFilteredQuery(HttpServletRequest req) {
		Profile authUser = getAuthUser(req);
		String currentSpace = getSpaceIdFromCookie(authUser, req);
		return getSpaceFilteredQuery(authUser, currentSpace);
	}

	public String getSpaceFilteredQuery(HttpServletRequest req, boolean isSpaceFiltered, String spaceFilter, String defaultQuery) {
		Profile authUser = getAuthUser(req);
		String currentSpace = getSpaceIdFromCookie(authUser, req);
		if (isSpaceFiltered) {
			return StringUtils.isBlank(spaceFilter) ? getSpaceFilter(authUser, currentSpace) : spaceFilter;
		}
		return canAccessSpace(authUser, currentSpace) ? defaultQuery : "";
	}

	public String getSpaceFilter(Profile authUser, String spaceId) {
		if (isAllSpaces(spaceId)) {
			if (authUser != null && authUser.hasSpaces()) {
				return "(" + authUser.getSpaces().stream().map(s -> "properties.space:\"" + s + "\"").
						collect(Collectors.joining(" OR ")) + ")";
			} else {
				return "properties.space:\"" + DEFAULT_SPACE + "\"";
			}
		} else if (isDefaultSpace(spaceId) && isMod(authUser)) { // DO NOT MODIFY!
			return "*";
		} else {
			return "properties.space:\"" + spaceId + "\"";
		}
	}

	public Sysprop buildSpaceObject(String space) {
		space = Utils.abbreviate(space, 255);
		space = space.replaceAll(Config.SEPARATOR, "");
		String spaceId = getSpaceId(Utils.noSpaces(Utils.stripAndTrim(space, " "), "-"));
		Sysprop s = new Sysprop(spaceId);
		s.setType("scooldspace");
		s.setName(space);
		return s;
	}

	public String sanitizeQueryString(String query, HttpServletRequest req) {
		String qf = getSpaceFilteredQuery(req);
		String defaultQuery = "*";
		String q = StringUtils.trimToEmpty(query);
		if (qf.isEmpty() || qf.length() > 1) {
			q = q.replaceAll("[\\?<>]", "").trim();
			q = q.replaceAll("$[\\*]*", "");
			q = RegExUtils.removeAll(q, "AND");
			q = RegExUtils.removeAll(q, "OR");
			q = RegExUtils.removeAll(q, "NOT");
			q = q.trim();
			defaultQuery = "";
		}
		if (qf.isEmpty()) {
			return defaultQuery;
		} else if ("*".equals(qf)) {
			return q;
		} else if ("*".equals(q)) {
			return qf;
		} else {
			if (q.isEmpty()) {
				return qf;
			} else {
				return qf + " AND " + q;
			}
		}
	}

	public String getUsersSearchQuery(String qs, String spaceFilter) {
		qs = Utils.stripAndTrim(qs).toLowerCase();
		if (!StringUtils.isBlank(qs)) {
			String wildcardLower = qs.matches("[\\p{IsAlphabetic}]*") ? qs + "*" : qs;
			String wildcardUpper = StringUtils.capitalize(wildcardLower);
			String template = "(name:({1}) OR name:({2} OR {3}) OR properties.location:({0}) OR "
					+ "properties.aboutme:({0}) OR properties.groups:({0}))";
			qs = (StringUtils.isBlank(spaceFilter) ? "" : spaceFilter + " AND ") +
					Utils.formatMessage(template, qs, StringUtils.capitalize(qs), wildcardLower, wildcardUpper);
		} else {
			qs = StringUtils.isBlank(spaceFilter) ? "*" : spaceFilter;
		}
		return qs;
	}

	public List<Post> fullQuestionsSearch(String query, Pager... pager) {
		String typeFilter = Config._TYPE + ":(" + String.join(" OR ",
						Utils.type(Question.class), Utils.type(Reply.class), Utils.type(Comment.class)) + ")";
		String qs = StringUtils.isBlank(query) || query.startsWith("*") ? typeFilter : query + " AND " + typeFilter;
		List<ParaObject> mixedResults = pc.findQuery("", qs, pager);
		Predicate<ParaObject> isQuestion =  obj -> obj.getType().equals(Utils.type(Question.class));

		Map<String, ParaObject> idsToQuestions = new HashMap<>(mixedResults.stream().filter(isQuestion).
				collect(Collectors.toMap(q -> q.getId(), q -> q)));
		Set<String> toRead = new LinkedHashSet<>();
		mixedResults.stream().filter(isQuestion.negate()).forEach(obj -> {
			if (!idsToQuestions.containsKey(obj.getParentid())) {
				toRead.add(obj.getParentid());
			}
		});
		// find all parent posts but this excludes parents of parents - i.e. won't work for comments in answers
		List<Post> parentPostsLevel1 = pc.readAll(new ArrayList<>(toRead));
		parentPostsLevel1.stream().filter(isQuestion).forEach(q -> idsToQuestions.put(q.getId(), q));

		toRead.clear();

		// read parents of parents if any
		parentPostsLevel1.stream().filter(isQuestion.negate()).forEach(obj -> {
			if (!idsToQuestions.containsKey(obj.getParentid())) {
				toRead.add(obj.getParentid());
			}
		});
		List<Post> parentPostsLevel2 = pc.readAll(new ArrayList<>(toRead));
		parentPostsLevel2.stream().forEach(q -> idsToQuestions.put(q.getId(), q));

		ArrayList<Post> results = new ArrayList<Post>(idsToQuestions.size());
		for (ParaObject result : idsToQuestions.values()) {
			if (result instanceof Post) {
				results.add((Post) result);
			}
		}
		return results;
	}

	public String getMacroCode(String key) {
		return WHITELISTED_MACROS.getOrDefault(key, "");
	}

	public boolean isMine(Post showPost, Profile authUser) {
		// author can edit, mods can edit & ppl with rep > 100 can edit
		return showPost != null && authUser != null ? authUser.getId().equals(showPost.getCreatorid()) : false;
	}

	public boolean canEdit(Post showPost, Profile authUser) {
		return authUser != null ? (authUser.hasBadge(TEACHER) || isMod(authUser) || isMine(showPost, authUser)) : false;
	}

	@SuppressWarnings("unchecked")
	public <P extends ParaObject> P populate(HttpServletRequest req, P pobj, String... paramName) {
		if (pobj == null || paramName == null) {
			return pobj;
		}
		Map<String, Object> data = new LinkedHashMap<String, Object>();
		if (isApiRequest(req)) {
			try {
				data = (Map<String, Object>) req.getAttribute(REST_ENTITY_ATTRIBUTE);
				if (data == null) {
					data = ParaObjectUtils.getJsonReader(Map.class).readValue(req.getInputStream());
				}
			} catch (IOException ex) {
				logger.error(null, ex);
				data = Collections.emptyMap();
			}
		} else {
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
					if (values != null && values.length > 0) {
						data.put(param, values.length > 1 ? Arrays.asList(values) :
								Arrays.asList(values).iterator().next());
					}
				}
			}
		}
		if (!data.isEmpty()) {
			ParaObjectUtils.setAnnotatedFields(pobj, data, null);
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

	public static String getGravatar(String email) {
		if (!isGravatarEnabled()) {
			return getServerURL() + CONTEXT_PATH +  PEOPLELINK + "/avatar";
		}
		if (StringUtils.isBlank(email)) {
			return "https://www.gravatar.com/avatar?d=retro&size=400";
		}
		return "https://www.gravatar.com/avatar/" + Utils.md5(email.toLowerCase()) + "?size=400&d=retro";
	}

	public static String getGravatar(Profile profile) {
		if (!isGravatarEnabled()) {
			return getServerURL() + CONTEXT_PATH +  PEOPLELINK + "/avatar";
		}
		if (profile == null || profile.getUser() == null) {
			return "https://www.gravatar.com/avatar?d=retro&size=400";
		} else {
			return getGravatar(profile.getUser().getEmail());
		}
	}

	public String getFullAvatarURL(Profile profile) {
		if (profile == null) {
			return getGravatar("");
		}
		return isAvatarValidationEnabled() ? PEOPLELINK + "/avatar?url=" + Utils.urlEncode(profile.getPicture()) :
				profile.getPicture();
	}

	public void clearSession(HttpServletRequest req, HttpServletResponse res) {
		if (req != null) {
			HttpUtils.removeStateParam(AUTH_COOKIE, req, res);
			HttpUtils.removeStateParam("dark-mode", req, res);
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

	private String loadEmailTemplate(String name) {
		return loadResource("emails/" + name + ".html");
	}

	public String loadResource(String filePath) {
		if (filePath == null) {
			return "";
		}
		if (FILE_CACHE.containsKey(filePath)) {
			return FILE_CACHE.get(filePath);
		}
		String template = "";
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(filePath)) {
			try (Scanner s = new Scanner(in).useDelimiter("\\A")) {
				template = s.hasNext() ? s.next() : "";
				if (!StringUtils.isBlank(template)) {
					FILE_CACHE.put(filePath, template);
				}
			}
		} catch (Exception ex) {
			logger.info("Couldn't load resource '{}'.", filePath);
		}
		return template;
	}

	public String compileEmailTemplate(Map<String, Object> model) {
		model.put("footerhtml", Config.getConfigParam("emails_footer_html",
				"<a href=\"" + ScooldServer.getServerURL() + "\">" + Config.APP_NAME + "</a> &bull; "
				+ "<a href=\"https://scoold.com\">Powered by Scoold</a>"));
		String fqdn = Config.getConfigParam("rewrite_inbound_links_with_fqdn", "");
		if (!StringUtils.isBlank(fqdn)) {
			model.entrySet().stream().filter(e -> (e.getValue() instanceof String)).forEachOrdered(e -> {
				model.put(e.getKey(), StringUtils.replace((String) e.getValue(), ScooldServer.getServerURL(), fqdn));
			});
		}
		return Utils.compileMustache(model, loadEmailTemplate("notify"));
	}

	public boolean isValidJWToken(String jwt) {
		try {
			String secret = Config.getConfigParam("app_secret_key", "");
			if (secret != null && jwt != null) {
				JWSVerifier verifier = new MACVerifier(secret);
				SignedJWT sjwt = SignedJWT.parse(jwt);
				if (sjwt.verify(verifier)) {
					Date referenceTime = new Date();
					JWTClaimsSet claims = sjwt.getJWTClaimsSet();

					Date expirationTime = claims.getExpirationTime();
					Date notBeforeTime = claims.getNotBeforeTime();
					String jti = claims.getJWTID();
					boolean expired = expirationTime != null && expirationTime.before(referenceTime);
					boolean notYetValid = notBeforeTime != null && notBeforeTime.after(referenceTime);
					boolean jtiRevoked = isApiKeyRevoked(jti, expired);
					return !(expired || notYetValid || jtiRevoked);
				}
			}
		} catch (JOSEException e) {
			logger.warn(null, e);
		} catch (ParseException ex) {
			logger.warn(null, ex);
		}
		return false;
	}

	public SignedJWT generateJWToken(Map<String, Object> claims) {
		return generateJWToken(claims, Config.JWT_EXPIRES_AFTER_SEC);
	}

	public SignedJWT generateJWToken(Map<String, Object> claims, long validitySeconds) {
		String secret = Config.getConfigParam("app_secret_key", "");
		if (!StringUtils.isBlank(secret)) {
			try {
				Date now = new Date();
				JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder();
				claimsSet.issueTime(now);
				if (validitySeconds > 0) {
					claimsSet.expirationTime(new Date(now.getTime() + (validitySeconds * 1000)));
				}
				claimsSet.notBeforeTime(now);
				claimsSet.claim(Config._APPID, Config.getConfigParam("access_key", "x"));
				claims.entrySet().forEach((claim) -> claimsSet.claim(claim.getKey(), claim.getValue()));
				JWSSigner signer = new MACSigner(secret);
				SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet.build());
				signedJWT.sign(signer);
				return signedJWT;
			} catch (JOSEException e) {
				logger.warn("Unable to sign JWT: {}.", e.getMessage());
			}
		}
		logger.error("Failed to generate JWT token - app_secret_key is blank.");
		return null;
	}

	public boolean isApiKeyRevoked(String jti, boolean expired) {
		if (StringUtils.isBlank(jti)) {
			return false;
		}
		if (API_KEYS.isEmpty()) {
			Sysprop s = pc.read("api_keys");
			if (s != null) {
				API_KEYS.putAll(s.getProperties());
			}
		}
		if (API_KEYS.containsKey(jti) && expired) {
			revokeApiKey(jti);
		}
		return !API_KEYS.containsKey(jti);
	}

	public void registerApiKey(String jti, String jwt) {
		if (StringUtils.isBlank(jti) || StringUtils.isBlank(jwt)) {
			return;
		}
		API_KEYS.put(jti, jwt);
		saveApiKeysObject();
	}

	public void revokeApiKey(String jti) {
		API_KEYS.remove(jti);
		saveApiKeysObject();
	}

	public Map<String, Object> getApiKeys() {
		return Collections.unmodifiableMap(API_KEYS);
	}

	public Map<String, Long> getApiKeysExpirations() {
		return API_KEYS.keySet().stream().collect(Collectors.toMap(k -> k, k -> {
			try {
				Date exp = SignedJWT.parse((String) API_KEYS.get(k)).getJWTClaimsSet().getExpirationTime();
				if (exp != null) {
					return exp.getTime();
				}
			} catch (ParseException ex) {
				logger.error(null, ex);
			}
			return 0L;
		}));
	}

	private void saveApiKeysObject() {
		Sysprop s = new Sysprop("api_keys");
		s.setProperties(API_KEYS);
		pc.create(s);
	}

	public Profile getSystemUser() {
		return API_USER;
	}

	public void triggerHookEvent(String eventName, Object payload) {
		if (isWebhooksEnabled() && HOOK_EVENTS.contains(eventName)) {
			Para.asyncExecute(() -> {
				Webhook trigger = new Webhook();
				trigger.setTriggeredEvent(eventName);
				trigger.setCustomPayload(payload);
				pc.create(trigger);
			});
		}
	}

	public void setSecurityHeaders(String nonce, HttpServletRequest request, HttpServletResponse response) {
		// CSP Header
		if (Config.getConfigBoolean("csp_header_enabled", true)) {
			response.setHeader("Content-Security-Policy",
					Config.getConfigParam("csp_header", getDefaultContentSecurityPolicy(request.isSecure())).
							replaceAll("\\{\\{nonce\\}\\}", nonce));
		}
		// HSTS Header
		if (Config.getConfigBoolean("hsts_header_enabled", true)) {
			response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
		}
		// Frame Options Header
		if (Config.getConfigBoolean("framing_header_enabled", true)) {
			response.setHeader("X-Frame-Options", "SAMEORIGIN");
		}
		// XSS Header
		if (Config.getConfigBoolean("xss_header_enabled", true)) {
			response.setHeader("X-XSS-Protection", "1; mode=block");
		}
		// Content Type Header
		if (Config.getConfigBoolean("contenttype_header_enabled", true)) {
			response.setHeader("X-Content-Type-Options", "nosniff");
		}
		// Referrer Header
		if (Config.getConfigBoolean("referrer_header_enabled", true)) {
			response.setHeader("Referrer-Policy", "strict-origin");
		}
	}

	public boolean cookieConsentGiven(HttpServletRequest request) {
		return !Config.getConfigBoolean("cookie_consent_required", false) ||
				"allow".equals(HttpUtils.getCookieValue(request, "cookieconsent_status"));
	}

	public String base64DecodeScript(String encodedScript) {
		if (StringUtils.isBlank(encodedScript)) {
			return "";
		}
		try {
			String decodedScript = Base64.isBase64(encodedScript) ? Utils.base64dec(encodedScript) : "";
			return StringUtils.isBlank(decodedScript) ? encodedScript : decodedScript;
		} catch (Exception e) {
			return encodedScript;
		}
	}

	public Map<String, Object> getExternalScripts() {
		if (Config.getConfig().hasPath("external_scripts")) {
			ConfigObject extScripts = Config.getConfig().getObject("external_scripts");
			if (extScripts != null && !extScripts.isEmpty()) {
				return new TreeMap<>(extScripts.unwrapped());
			}
		}
		return Collections.emptyMap();
	}

	public List<String> getExternalStyles() {
		String extStyles = Config.getConfigParam("external_styles", "");
		if (!StringUtils.isBlank(extStyles)) {
			String[] styles = extStyles.split("\\s*,\\s*");
			if (!StringUtils.isBlank(extStyles) && styles != null && styles.length > 0) {
				ArrayList<String> list = new ArrayList<String>();
				for (String style : styles) {
					if (!StringUtils.isBlank(style)) {
						list.add(style);
					}
				}
				return list;
			}
		}
		return Collections.emptyList();
	}

	public String getInlineCSS() {
		try {
			Sysprop custom = getCustomTheme();
			String themeName = custom.getName();
			String inline = Config.getConfigParam("inline_css", "");
			String loadedTheme;
			if ("default".equalsIgnoreCase(themeName) || StringUtils.isBlank(themeName)) {
				return inline;
			} else if ("custom".equalsIgnoreCase(themeName)) {
				loadedTheme = (String) custom.getProperty("theme");
			} else {
				loadedTheme = loadResource(getThemeKey(themeName));
				if (StringUtils.isBlank(loadedTheme)) {
					FILE_CACHE.put("theme", "default");
					custom.setName("default");
					pc.update(custom);
					return inline;
				} else {
					FILE_CACHE.put("theme", themeName);
				}
			}
			loadedTheme = StringUtils.replaceEachRepeatedly(loadedTheme,
					new String[] {"<", "</", "<script", "<SCRIPT"}, new String[] {"", "", "", ""});
			return loadedTheme + "\n/*** END OF THEME CSS ***/\n" + inline;
		} catch (Exception e) {
			logger.debug("Failed to load inline CSS.");
		}
		return "";
	}

	public void setCustomTheme(String themeName, String themeCSS) {
		String id = "theme" + Config.SEPARATOR + "custom";
		boolean isCustom = "custom".equalsIgnoreCase(themeName);
		String css = isCustom ? themeCSS : "";
		Sysprop custom = new Sysprop(id);
		custom.setName(StringUtils.isBlank(css) && isCustom ? "default" : themeName);
		custom.addProperty("theme", css);
		pc.create(custom);
		FILE_CACHE.put("theme", themeName);
		FILE_CACHE.put(getThemeKey(themeName), isCustom ? css : loadResource(getThemeKey(themeName)));
	}

	public Sysprop getCustomTheme() {
		String id = "theme" + Config.SEPARATOR + "custom";
		return (Sysprop) Optional.ofNullable(pc.read(id)).orElseGet(this::getDefaultThemeObject);
		// !!!!!!!: make this more efficient by storing the selected theme in cookie, then get from cache.
//		String selectedTheme = FILE_CACHE.getOrDefault("theme", "default");
//		if (selectedTheme != null && FILE_CACHE.containsKey(getThemeKey(selectedTheme))) {
//			Sysprop s = new Sysprop(id);
//			s.setName(selectedTheme);
//			s.addProperty("theme", FILE_CACHE.get(getThemeKey(selectedTheme)));
//			return s;
//		} else if ("custom".equalsIgnoreCase(selectedTheme)) {
//			return (Sysprop) Optional.ofNullable(pc.read("theme" + Config.SEPARATOR + "custom")).
//					orElseGet(() -> getDefaultThemeObject());
//		}
//		return getDefaultThemeObject();
	}

	private Sysprop getDefaultThemeObject() {
		String themeName = "default";
		Sysprop s = new Sysprop("theme" + Config.SEPARATOR + "custom");
		s.setName(themeName);
		s.addProperty("theme", "");
		FILE_CACHE.put("theme", themeName);
		FILE_CACHE.put(getThemeKey(themeName), loadResource(getThemeKey(themeName)));
		return s;
	}

	private String getThemeKey(String themeName) {
		return "themes/" + themeName + ".css";
	}

	public String getDefaultTheme() {
		return loadResource("themes/default.css");
	}

	public String getCSPNonce() {
		return Utils.generateSecurityToken(16);
	}

	public String getDefaultContentSecurityPolicy(boolean isSecure) {
		return (isSecure ? "upgrade-insecure-requests; " : "")
				+ "default-src 'self'; "
				+ "base-uri 'self'; "
				+ "form-action 'self' " + Config.getConfigParam("signout_url", "") + "; "
				+ "connect-src 'self' " + (Config.IN_PRODUCTION ? getServerURL() : "")
				+ " scoold.com www.google-analytics.com www.googletagmanager.com accounts.google.com " + Config.getConfigParam("csp_connect_sources", "") + "; "
				+ "frame-src 'self' accounts.google.com staticxx.facebook.com " + Config.getConfigParam("csp_frame_sources", "") + "; "
				+ "font-src 'self' cdnjs.cloudflare.com fonts.gstatic.com fonts.googleapis.com " + Config.getConfigParam("csp_font_sources", "") + "; "
				+ "style-src 'self' 'unsafe-inline' fonts.googleapis.com accounts.google.com " // unsafe-inline required by MathJax and Google Maps!
				+ (CDN_URL.startsWith("/") ? "" : CDN_URL) + " " +
					Config.getConfigParam("csp_style_sources", Config.getConfigParam("stylesheet_url", "") + " " +
							Config.getConfigParam("external_styles", "").replaceAll(",", "")) + "; "
				+ "img-src 'self' https: data:; "
				+ "object-src 'none'; "
				+ "report-uri /reports/cspv; "
				+ "script-src 'unsafe-inline' https: 'nonce-{{nonce}}' 'strict-dynamic';"; // CSP2 backward compatibility
	}

	public String getGoogleLoginURL() {
		return "https://accounts.google.com/o/oauth2/v2/auth?" +
				"client_id=" + Config.GPLUS_APP_ID + "&response_type=code&scope=openid%20profile%20email&redirect_uri="
				+ getParaEndpoint() + "/google_auth&state=" + getParaAppId();
	}

	public String getGitHubLoginURL() {
		return "https://github.com/login/oauth/authorize?response_type=code&client_id=" + Config.GITHUB_APP_ID +
				"&scope=user%3Aemail&state=" + getParaAppId() +
				"&redirect_uri=" + getParaEndpoint() + "/github_auth";
	}

	public String getLinkedInLoginURL() {
		return "https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=" + Config.LINKEDIN_APP_ID +
				"&scope=r_liteprofile%20r_emailaddress&state=" + getParaAppId() +
				"&redirect_uri=" + getParaEndpoint() + "/linkedin_auth";
	}

	public String getTwitterLoginURL() {
		return getParaEndpoint() + "/twitter_auth?state=" + getParaAppId();
	}

	public String getMicrosoftLoginURL() {
		return "https://login.microsoftonline.com/" + Config.getConfigParam("ms_tenant_id", "common") +
				"/oauth2/v2.0/authorize?response_type=code&client_id=" + Config.MICROSOFT_APP_ID +
				"&scope=https%3A%2F%2Fgraph.microsoft.com%2Fuser.read&state=" + getParaAppId() +
				"&redirect_uri=" + getParaEndpoint() + "/microsoft_auth";
	}

	public String getSlackLoginURL() {
		return "https://slack.com/oauth/v2/authorize?response_type=code&client_id=" + Config.SLACK_APP_ID +
				"&user_scope=identity.basic%20identity.email%20identity.team%20identity.avatar&state=" + getParaAppId() +
				"&redirect_uri=" + getParaEndpoint() + "/slack_auth";
	}

	public String getAmazonLoginURL() {
		return "https://www.amazon.com/ap/oa?response_type=code&client_id=" + Config.AMAZON_APP_ID +
				"&scope=profile&state=" + getParaAppId() +
				"&redirect_uri=" + getParaEndpoint() + "/amazon_auth";
	}

	public String getOAuth2LoginURL() {
		return Config.getConfigParam("security.oauth.authz_url", "") + "?" +
				"response_type=code&client_id=" + Config.getConfigParam("oa2_app_id", "") +
				"&scope=" + Config.getConfigParam("security.oauth.scope", "") + "&state=" + getParaAppId() +
				"&redirect_uri=" + getParaEndpoint() + "/oauth2_auth";
	}

	public String getOAuth2SecondLoginURL() {
		return Config.getConfigParam("security.oauthsecond.authz_url", "") + "?" +
				"response_type=code&client_id=" + Config.getConfigParam("oa2second_app_id", "") +
				"&scope=" + Config.getConfigParam("security.oauthsecond.scope", "") + "&state=" + getParaAppId() +
				"&redirect_uri=" + getParaEndpoint() + "/oauth2_auth";
	}

	public String getOAuth2ThirdLoginURL() {
		return Config.getConfigParam("security.oauththird.authz_url", "") + "?" +
				"response_type=code&client_id=" + Config.getConfigParam("oa2third_app_id", "") +
				"&scope=" + Config.getConfigParam("security.oauththird.scope", "") + "&state=" + getParaAppId() +
				"&redirect_uri=" + getParaEndpoint() + "/oauth2_auth";
	}

	public String getParaEndpoint() {
		return Config.getConfigParam("security.redirect_uri", pc.getEndpoint());
	}

	public String getParaAppId() {
		return StringUtils.removeStart(Config.getConfigParam("access_key", "app:scoold"), "app:");
	}

	public String getFirstConfiguredLoginURL() {
		if (!Config.GPLUS_APP_ID.isEmpty()) {
			return getGoogleLoginURL();
		}
		if (!Config.GITHUB_APP_ID.isEmpty()) {
			return getGitHubLoginURL();
		}
		if (!Config.LINKEDIN_APP_ID.isEmpty()) {
			return getLinkedInLoginURL();
		}
		if (!Config.TWITTER_APP_ID.isEmpty()) {
			return getTwitterLoginURL();
		}
		if (!Config.MICROSOFT_APP_ID.isEmpty()) {
			return getMicrosoftLoginURL();
		}
		if (!Config.SLACK_APP_ID.isEmpty()) {
			return getSlackLoginURL();
		}
		if (!Config.AMAZON_APP_ID.isEmpty()) {
			return getAmazonLoginURL();
		}
		if (!Config.getConfigParam("oa2_app_id", "").isEmpty()) {
			return getOAuth2LoginURL();
		}
		if (!Config.getConfigParam("oa2second_app_id", "").isEmpty()) {
			return getOAuth2SecondLoginURL();
		}
		if (!Config.getConfigParam("oa2third_app_id", "").isEmpty()) {
			return getOAuth2ThirdLoginURL();
		}
		return SIGNINLINK + "?code=3&error=true";
	}
}
