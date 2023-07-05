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

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Address;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.Tag;
import com.erudika.para.core.User;
import com.erudika.para.core.Votable;
import com.erudika.para.core.Vote;
import com.erudika.para.core.Webhook;
import com.erudika.para.core.email.Emailer;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.para.core.validation.ValidationUtils;
import com.erudika.scoold.ScooldConfig;
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
import com.erudika.scoold.utils.avatars.AvatarFormat;
import com.erudika.scoold.utils.avatars.AvatarRepository;
import com.erudika.scoold.utils.avatars.AvatarRepositoryProxy;
import com.erudika.scoold.utils.avatars.GravatarAvatarGenerator;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
import java.util.UUID;
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
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringEscapeUtils;
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
	private static final String EMAIL_ALERTS_PREFIX = "email-alerts" + Para.getConfig().separator();

	private static final Profile API_USER;
	private static final Set<String> CORE_TYPES;
	private static final Set<String> HOOK_EVENTS;
	private static final Map<String, String> WHITELISTED_MACROS;
	private static final Map<String, Object> API_KEYS = new LinkedHashMap<>(); // jti => jwt

	private Set<Sysprop> allSpaces;
	private Set<String> autoAssignedSpacesFromConfig;

	private static final ScooldConfig CONF = new ScooldConfig();

	static {
		API_USER = new Profile("1", "System");
		API_USER.setVotes(1);
		API_USER.setCreatorid("1");
		API_USER.setTimestamp(Utils.timestamp());
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

	private final ParaClient pc;
	private final LanguageUtils langutils;
	private final AvatarRepository avatarRepository;
	private final GravatarAvatarGenerator gravatarAvatarGenerator;
	private static ScooldUtils instance;
	private Sysprop customTheme;
	@Inject private Emailer emailer;

	public static final int MAX_SPACES = 10; // Hey! It's cool to edit this, but please consider buying Scoold Pro! :)

	@Inject
	public ScooldUtils(ParaClient pc, LanguageUtils langutils, AvatarRepositoryProxy avatarRepository,
			GravatarAvatarGenerator gravatarAvatarGenerator) {
		this.pc = pc;
		this.langutils = langutils;
		this.avatarRepository = avatarRepository;
		this.gravatarAvatarGenerator = gravatarAvatarGenerator;
		API_USER.setPicture(avatarRepository.getAnonymizedLink(CONF.supportEmail()));
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

	public static ScooldConfig getConfig() {
		return CONF;
	}

	static {
		// multiple domains/admins are allowed only in Scoold PRO
		String approvedDomain = StringUtils.substringBefore(CONF.approvedDomainsForSignups(), ",");
		if (!StringUtils.isBlank(approvedDomain)) {
			APPROVED_DOMAINS.add(approvedDomain);
		}
		// multiple admins are allowed only in Scoold PRO
		String admin = StringUtils.substringBefore(CONF.admins(), ",");
		if (!StringUtils.isBlank(admin)) {
			ADMINS.add(admin);
		}
	}

	public static void setParaEndpointAndApiPath(ParaClient pc) {
		try {
			URL endpoint = new URL(CONF.paraEndpoint());
			if (!StringUtils.isBlank(endpoint.getPath()) && !"/".equals(endpoint.getPath())) {
				// support Para deployed under a specific context path
				pc.setEndpoint(StringUtils.removeEnd(CONF.paraEndpoint(), endpoint.getPath()));
				pc.setApiPath(StringUtils.stripEnd(endpoint.getPath(), "/") + pc.getApiPath());
			} else {
				pc.setEndpoint(CONF.paraEndpoint());
			}
		} catch (Exception e) {
			logger.error("Invalid Para endpoint URL: {}", CONF.paraEndpoint());
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
			int maxRetries = CONF.paraConnectionRetryAttempts();
			int retryInterval = CONF.paraConnectionRetryIntervalSec();
			int count = ++retryCount;
			logger.error("No connection to Para backend. Retrying connection in {}s (attempt {} of {})...",
					retryInterval, count, maxRetries);
			if (maxRetries < 0 || retryCount < maxRetries) {
				Para.asyncExecute(() -> {
					try {
						Thread.sleep(retryInterval * 1000L);
					} catch (InterruptedException ex) {
						logger.error(null, ex);
						Thread.currentThread().interrupt();
					}
					retryConnection(callable, count);
				});
			}
		}
	}

	public ParaObject checkAuth(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		Profile authUser = null;
		String jwt = HttpUtils.getStateParam(CONF.authCookie(), req);
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
				logger.info("Invalid JWT found in cookie {}.", CONF.authCookie());
				res.sendRedirect(CONF.serverUrl() + CONF.serverContextPath() + SIGNINLINK + "?code=3&error=true");
				return null;
			}
		}
		return authUser;
	}

	private ParaObject checkApiAuth(HttpServletRequest req) {
		if (req.getRequestURI().equals(CONF.serverContextPath() + "/api")) {
			return null;
		}
		String apiKeyJWT = StringUtils.removeStart(req.getHeader(HttpHeaders.AUTHORIZATION), "Bearer ");
		if (req.getRequestURI().equals(CONF.serverContextPath() + "/api/ping")) {
			return API_USER;
		} else if (req.getRequestURI().equals(CONF.serverContextPath() + "/api/stats") && isValidJWToken(apiKeyJWT)) {
			return API_USER;
		} else if (!isApiEnabled() || StringUtils.isBlank(apiKeyJWT) || !isValidJWToken(apiKeyJWT)) {
			throw new UnauthorizedException();
		}
		return API_USER;
	}

	private boolean promoteOrDemoteUser(Profile authUser, User u) {
		if (authUser != null && authUser.getEditorRoleEnabled()) {
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
				&& !gravatarAvatarGenerator.isLink(authUser.getPicture())
				&& !CONF.avatarEditsEnabled()) {
			authUser.setPicture(u.getPicture());
			update = true;
		}
		if (!CONF.nameEditsEnabled() &&	!StringUtils.equals(u.getName(), authUser.getName())) {
			authUser.setName(StringUtils.abbreviate(u.getName(), 256));
			update = true;
		}
		if (!StringUtils.equals(u.getName(), authUser.getOriginalName())) {
			authUser.setOriginalName(u.getName());
			update = true;
		}
		if (authUser.isComplete()) {
			update = addBadgeOnce(authUser, Profile.Badge.NICEPROFILE, authUser.isComplete()) || update;
		}
		return update;
	}

	public boolean isDarkModeEnabled(Profile authUser, HttpServletRequest req) {
		return (authUser != null && authUser.getDarkmodeEnabled()) ||
				"1".equals(HttpUtils.getCookieValue(req, "dark-mode"));
	}

	private String getDefaultEmailSignature(String defaultText) {
		String template = CONF.emailsDefaultSignatureText(defaultText);
		return Utils.formatMessage(template, CONF.appName());
	}

	public void sendWelcomeEmail(User user, boolean verifyEmail, HttpServletRequest req) {
		// send welcome email notification
		if (user != null) {
			Map<String, Object> model = new HashMap<String, Object>();
			Map<String, String> lang = getLang(req);
			String subject = Utils.formatMessage(lang.get("signin.welcome"), CONF.appName());
			String body1 = Utils.formatMessage(CONF.emailsWelcomeText1(lang), CONF.appName());
			String body2 = CONF.emailsWelcomeText2(lang);
			String body3 = getDefaultEmailSignature(CONF.emailsWelcomeText3(lang));

			if (verifyEmail && !user.getActive() && !StringUtils.isBlank(user.getIdentifier())) {
				Sysprop s = pc.read(user.getIdentifier());
				if (s != null) {
					String token = Utils.base64encURL(Utils.generateSecurityToken().getBytes());
					s.addProperty(Config._EMAIL_TOKEN, token);
					pc.update(s);
					token = CONF.serverUrl() + CONF.serverContextPath() + SIGNINLINK + "/register?id=" + user.getId() + "&token=" + token;
					body3 = "<b><a href=\"" + token + "\">" + lang.get("signin.welcome.verify") + "</a></b><br><br>" + body3;
				}
			}

			model.put("subject", escapeHtml(subject));
			model.put("logourl", getSmallLogoUrl());
			model.put("heading", Utils.formatMessage(lang.get("signin.welcome.title"), escapeHtml(user.getName())));
			model.put("body", body1 + body2 + body3);
			emailer.sendEmail(Arrays.asList(user.getEmail()), subject, compileEmailTemplate(model));
		}
	}

	public void sendVerificationEmail(Sysprop identifier, String redirectUrl, HttpServletRequest req) {
		if (identifier != null) {
			Map<String, Object> model = new HashMap<String, Object>();
			Map<String, String> lang = getLang(req);
			String subject = CONF.appName() + " - " + lang.get("msgcode.6");
			String body = getDefaultEmailSignature(CONF.emailsWelcomeText3(lang));
			redirectUrl = StringUtils.isBlank(redirectUrl) ? SIGNINLINK + "/register" : redirectUrl;

			String token = Utils.base64encURL(Utils.generateSecurityToken().getBytes());
			identifier.addProperty(Config._EMAIL_TOKEN, token);
			identifier.addProperty("confirmationTimestamp", Utils.timestamp());
			pc.update(identifier);
			token = CONF.serverUrl() + CONF.serverContextPath() + redirectUrl + "?id=" +
					identifier.getCreatorid() + "&token=" + token;
			body = "<b><a href=\"" + token + "\">" + lang.get("signin.welcome.verify") + "</a></b><br><br>" + body;

			model.put("subject", escapeHtml(subject));
			model.put("logourl", getSmallLogoUrl());
			model.put("heading", lang.get("hello"));
			model.put("body", body);
			emailer.sendEmail(Arrays.asList(identifier.getId()), subject, compileEmailTemplate(model));
		}
	}

	public void sendPasswordResetEmail(String email, String token, HttpServletRequest req) {
		if (email != null && token != null) {
			Map<String, Object> model = new HashMap<String, Object>();
			Map<String, String> lang = getLang(req);
			String url = CONF.serverUrl() + CONF.serverContextPath() + SIGNINLINK + "/iforgot?email=" + email + "&token=" + token;
			String subject = lang.get("iforgot.title");
			String body1 = lang.get("notification.iforgot.body1") + "<br><br>";
			String body2 = Utils.formatMessage("<b><a href=\"{0}\">" + lang.get("notification.iforgot.body2") +
					"</a></b><br><br>", url);
			String body3 = getDefaultEmailSignature(lang.get("notification.signature") + "<br><br>");

			model.put("subject", escapeHtml(subject));
			model.put("logourl", getSmallLogoUrl());
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
		if (!isNewPostNotificationAllowed()) {
			return false;
		}

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
		int max = CONF.maxItemsPerPage();
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
			pc.readEverything(pager -> {
				List<Profile> profiles = pc.findQuery(Utils.type(Profile.class),
						"properties.favtags:(" + tags.stream().
								map(t -> "\"".concat(t).concat("\"")).distinct().
								collect(Collectors.joining(" ")) + ") AND properties.favtagsEmailsEnabled:true", pager);
				if (!profiles.isEmpty()) {
					List<User> users = pc.readAll(profiles.stream().map(p -> p.getCreatorid()).
							distinct().collect(Collectors.toList()));

					users.stream().forEach(u -> emails.add(u.getEmail()));
				}
				return profiles;
			});
			return emails;
		}
		return Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	public void sendUpdatedFavTagsNotifications(Post question, List<String> addedTags, HttpServletRequest req) {
		if (!isFavTagsNotificationAllowed()) {
			return;
		}
		// sends a notification to subscibers of a tag if that tag was added to an existing question
		if (question != null && !question.isReply() && addedTags != null && !addedTags.isEmpty()) {
			Profile postAuthor = question.getAuthor(); // the current user - same as utils.getAuthUser(req)
			Map<String, Object> model = new HashMap<String, Object>();
			Map<String, String> lang = getLang(req);
			String name = postAuthor.getName();
			String body = Utils.markdownToHtml(question.getBody());
			String picture = Utils.formatMessage("<img src='{0}' width='25'>", escapeHtmlAttribute(avatarRepository.
					getLink(postAuthor, AvatarFormat.Square25)));
			String postURL = CONF.serverUrl() + question.getPostLink(false, false);
			String tagsString = Optional.ofNullable(question.getTags()).orElse(Collections.emptyList()).stream().
					map(t -> "<span class=\"tag\">" +
							(addedTags.contains(t) ? "<b>" + escapeHtml(t) + "<b>" : escapeHtml(t)) + "</span>").
					collect(Collectors.joining("&nbsp;"));
			String subject = Utils.formatMessage(lang.get("notification.favtags.subject"), name,
					Utils.abbreviate(question.getTitle(), 255));
			model.put("subject", escapeHtml(subject));
			model.put("logourl", getSmallLogoUrl());
			model.put("heading", Utils.formatMessage(lang.get("notification.favtags.heading"), picture, escapeHtml(name)));
			model.put("body", Utils.formatMessage("<h2><a href='{0}'>{1}</a></h2><div>{2}</div><br>{3}",
					postURL, escapeHtml(question.getTitle()), body, tagsString));

			Set<String> emails = getFavTagsSubscribers(addedTags);
			sendEmailsToSubscribersInSpace(emails, question.getSpace(), subject, compileEmailTemplate(model));
		}
	}

	@SuppressWarnings("unchecked")
	public void sendNewPostNotifications(Post question, HttpServletRequest req) {
		if (question == null) {
			return;
		}
		// the current user - same as utils.getAuthUser(req)
		Profile postAuthor = question.getAuthor() != null ? question.getAuthor() : pc.read(question.getCreatorid());
		if (!question.getType().equals(Utils.type(UnapprovedQuestion.class))) {
			if (!isNewPostNotificationAllowed()) {
				return;
			}

			Map<String, Object> model = new HashMap<String, Object>();
			Map<String, String> lang = getLang(req);
			String name = postAuthor.getName();
			String body = Utils.markdownToHtml(question.getBody());
			String picture = Utils.formatMessage("<img src='{0}' width='25'>", escapeHtmlAttribute(avatarRepository.
					getLink(postAuthor, AvatarFormat.Square25)));
			String postURL = CONF.serverUrl() + question.getPostLink(false, false);
			String tagsString = Optional.ofNullable(question.getTags()).orElse(Collections.emptyList()).stream().
					map(t -> "<span class=\"tag\">" + escapeHtml(t) + "</span>").
					collect(Collectors.joining("&nbsp;"));
			String subject = Utils.formatMessage(lang.get("notification.newposts.subject"), name,
					Utils.abbreviate(question.getTitle(), 255));
			model.put("subject", escapeHtml(subject));
			model.put("logourl", getSmallLogoUrl());
			model.put("heading", Utils.formatMessage(lang.get("notification.newposts.heading"), picture, escapeHtml(name)));
			model.put("body", Utils.formatMessage("<h2><a href='{0}'>{1}</a></h2><div>{2}</div><br>{3}",
					postURL, escapeHtml(question.getTitle()), body, tagsString));

			Set<String> emails = new HashSet<String>(getNotificationSubscribers(EMAIL_ALERTS_PREFIX + "new_post_subscribers"));
			emails.addAll(getFavTagsSubscribers(question.getTags()));
			sendEmailsToSubscribersInSpace(emails, question.getSpace(), subject, compileEmailTemplate(model));
		} else if (postsNeedApproval() && question instanceof UnapprovedQuestion) {
			Report rep = new Report();
			rep.setDescription("New question awaiting approval");
			rep.setSubType(Report.ReportType.OTHER);
			rep.setLink(question.getPostLink(false, false));
			rep.setAuthorName(postAuthor.getName());
			rep.create();
		}
	}

	public void sendReplyNotifications(Post parentPost, Post reply, HttpServletRequest req) {
		// send email notification to author of post except when the reply is by the same person
		if (parentPost != null && reply != null && !StringUtils.equals(parentPost.getCreatorid(), reply.getCreatorid())) {
			Profile replyAuthor = reply.getAuthor(); // the current user - same as utils.getAuthUser(req)
			Map<String, Object> model = new HashMap<String, Object>();
			Map<String, String> lang = getLang(req);
			String name = replyAuthor.getName();
			String body = Utils.markdownToHtml(reply.getBody());
			String picture = Utils.formatMessage("<img src='{0}' width='25'>", escapeHtmlAttribute(avatarRepository.
					getLink(replyAuthor, AvatarFormat.Square25)));
			String postURL = CONF.serverUrl() + parentPost.getPostLink(false, false);
			String subject = Utils.formatMessage(lang.get("notification.reply.subject"), name,
					Utils.abbreviate(reply.getTitle(), 255));
			model.put("subject", escapeHtml(subject));
			model.put("logourl", getSmallLogoUrl());
			model.put("heading", Utils.formatMessage(lang.get("notification.reply.heading"),
					Utils.formatMessage("<a href='{0}'>{1}</a>", postURL, escapeHtml(parentPost.getTitle()))));
			model.put("body", Utils.formatMessage("<h2>{0} {1}:</h2><div>{2}</div>", picture, escapeHtml(name), body));

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

			if (isReplyNotificationAllowed() && parentPost.hasFollowers()) {
				emailer.sendEmail(new ArrayList<String>(parentPost.getFollowers().values()),
						subject,
						compileEmailTemplate(model));
			}
		}
	}

	public void sendCommentNotifications(Post parentPost, Comment comment, Profile commentAuthor, HttpServletRequest req) {
		// send email notification to author of post except when the comment is by the same person
		if (parentPost != null && comment != null) {
			parentPost.setAuthor(pc.read(Profile.id(parentPost.getCreatorid()))); // parent author is not current user (authUser)
			Map<String, Object> payload = new LinkedHashMap<>(ParaObjectUtils.getAnnotatedFields(comment, false));
			payload.put("parent", parentPost);
			payload.put("author", commentAuthor);
			triggerHookEvent("comment.create", payload);
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
			Map<String, String> lang = getLang(req);
			List<Profile> last5commentators = pc.readAll(new ArrayList<>(last5ids));
			last5commentators = last5commentators.stream().filter(u -> u.getCommentEmailsEnabled()).collect(Collectors.toList());
			pc.readAll(last5commentators.stream().map(u -> u.getCreatorid()).collect(Collectors.toList())).forEach(author -> {
				if (isCommentNotificationAllowed()) {
					Map<String, Object> model = new HashMap<String, Object>();
					String name = commentAuthor.getName();
					String body = Utils.markdownToHtml(comment.getComment());
					String pic = Utils.formatMessage("<img src='{0}' width='25'>",
						escapeHtmlAttribute(avatarRepository.getLink(commentAuthor, AvatarFormat.Square25)));
					String postURL = CONF.serverUrl() + parentPost.getPostLink(false, false);
					String subject = Utils.formatMessage(lang.get("notification.comment.subject"), name, parentPost.getTitle());
					model.put("subject", escapeHtml(subject));
					model.put("logourl", getSmallLogoUrl());
					model.put("heading", Utils.formatMessage(lang.get("notification.comment.heading"),
							Utils.formatMessage("<a href='{0}'>{1}</a>", postURL, escapeHtml(parentPost.getTitle()))));
					model.put("body", Utils.formatMessage("<h2>{0} {1}:</h2><div class='panel'>{2}</div>", pic, escapeHtml(name), body));
					emailer.sendEmail(Arrays.asList(((User) author).getEmail()), subject, compileEmailTemplate(model));
				}
			});
		}
	}

	private String escapeHtmlAttribute(String value) {
		return StringUtils.trimToEmpty(value)
				.replaceAll("'", "%27")
				.replaceAll("\"", "%22")
				.replaceAll("\\\\", "");
	}

	private String escapeHtml(String value) {
		return StringEscapeUtils.escapeHtml4(value);
	}

	public Profile getAuthUser(HttpServletRequest req) {
		return (Profile) req.getAttribute(AUTH_USER_ATTRIBUTE);
	}

	public boolean isAuthenticated(HttpServletRequest req) {
		return getAuthUser(req) != null;
	}

	public boolean isFeedbackEnabled() {
		return CONF.feedbackEnabled();
	}

	public boolean isNearMeFeatureEnabled() {
		return CONF.postsNearMeEnabled();
	}

	public boolean isDefaultSpacePublic() {
		return CONF.isDefaultSpacePublic();
	}

	public boolean isWebhooksEnabled() {
		return CONF.webhooksEnabled();
	}

	public boolean isAnonymityEnabled() {
		return CONF.profileAnonimityEnabled();
	}

	public boolean isApiEnabled() {
		return CONF.apiEnabled();
	}

	public boolean isFooterLinksEnabled() {
		return CONF.footerLinksEnabled();
	}

	public boolean isNotificationsAllowed() {
		return CONF.notificationEmailsAllowed();
	}

	public boolean isNewPostNotificationAllowed() {
		return isNotificationsAllowed() && CONF.emailsForNewPostsAllowed();
	}

	public boolean isFavTagsNotificationAllowed() {
		return isNotificationsAllowed() && CONF.emailsForFavtagsAllowed();
	}

	public boolean isReplyNotificationAllowed() {
		return isNotificationsAllowed() && CONF.emailsForRepliesAllowed();
	}

	public boolean isCommentNotificationAllowed() {
		return isNotificationsAllowed() && CONF.emailsForCommentsAllowed();
	}

	public boolean isDarkModeEnabled() {
		return CONF.darkModeEnabled();
	}

	public boolean isSlackAuthEnabled() {
		return CONF.slackAuthEnabled();
	}

	public boolean isMicrosoftAuthEnabled() {
		return CONF.teamsAuthEnabled();
	}

	public static boolean isGravatarEnabled() {
		return CONF.gravatarsEnabled();
	}

	public static String gravatarPattern() {
		return CONF.gravatarsPattern();
	}

	public static String getDefaultAvatar() {
		return CONF.imagesLink() + "/anon.svg";
	}

	public static boolean isAvatarUploadsEnabled() {
		return isImgurAvatarRepositoryEnabled() || isCloudinaryAvatarRepositoryEnabled();
	}

	public static boolean isImgurAvatarRepositoryEnabled() {
		return !StringUtils.isBlank(CONF.imgurClientId()) && "imgur".equalsIgnoreCase(CONF.avatarRepository());
	}

	public static boolean isCloudinaryAvatarRepositoryEnabled() {
		return !StringUtils.isBlank(CONF.cloudinaryUrl()) && "cloudinary".equalsIgnoreCase(CONF.avatarRepository());
	}

	public String getFooterHTML() {
		return CONF.footerHtml();
	}

	public boolean isNavbarLink1Enabled() {
		return !StringUtils.isBlank(getNavbarLink1URL());
	}

	public String getNavbarLink1URL() {
		return CONF.navbarCustomLink1Url();
	}

	public String getNavbarLink1Text() {
		return CONF.navbarCustomLink1Text();
	}

	public boolean isNavbarLink2Enabled() {
		return !StringUtils.isBlank(getNavbarLink2URL());
	}

	public String getNavbarLink2URL() {
		return CONF.navbarCustomLink2Url();
	}

	public String getNavbarLink2Text() {
		return CONF.navbarCustomLink2Text();
	}

	public boolean isNavbarMenuLink1Enabled() {
		return !StringUtils.isBlank(getNavbarMenuLink1URL());
	}

	public String getNavbarMenuLink1URL() {
		return CONF.navbarCustomMenuLink1Url();
	}

	public String getNavbarMenuLink1Text() {
		return CONF.navbarCustomMenuLink1Text();
	}

	public boolean isNavbarMenuLink2Enabled() {
		return !StringUtils.isBlank(getNavbarMenuLink2URL());
	}

	public String getNavbarMenuLink2URL() {
		return CONF.navbarCustomMenuLink2Url();
	}

	public String getNavbarMenuLink2Text() {
		return CONF.navbarCustomMenuLink2Text();
	}

	public String getNavbarLink1Target() {
		return CONF.navbarCustomLink1Target();
	}

	public String getNavbarLink2Target() {
		return CONF.navbarCustomLink2Target();
	}

	public String getNavbarMenuLink1Target() {
		return CONF.navbarCustomMenuLink1Target();
	}

	public String getNavbarMenuLink2Target() {
		return CONF.navbarCustomMenuLink2Target();
	}

	public boolean alwaysHideCommentForms() {
		return CONF.alwaysHideCommentForms();
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
		Pager p = new Pager(CONF.maxItemsPerPage());
		p.setPage(Math.min(NumberUtils.toLong(req.getParameter(pageParamName), 1), CONF.maxPages()));
		String paramSuffix = StringUtils.substringAfter(pageParamName, "page");
		String lastKey = Optional.ofNullable(req.getParameter("lastKey")).orElse(req.getParameter("lastKey" + paramSuffix));
		String sort = Optional.ofNullable(req.getParameter("sortby")).orElse(req.getParameter("sortby" + paramSuffix));
		String desc = Optional.ofNullable(req.getParameter("desc")).orElse(req.getParameter("desc" + paramSuffix));
		String limit = Optional.ofNullable(req.getParameter("limit")).orElse(req.getParameter("limit" + paramSuffix));
		if (!StringUtils.isBlank(desc)) {
			p.setDesc(Boolean.parseBoolean(desc));
		}
		if (!StringUtils.isBlank(lastKey)) {
			p.setLastKey(lastKey);
		}
		if (!StringUtils.isBlank(sort)) {
			p.setSortby(sort);
		}
		if (!StringUtils.isBlank(limit)) {
			p.setLimit(NumberUtils.toInt(limit, CONF.maxItemsPerPage()));
		}
		return p;
	}

	public String getLanguageCode(HttpServletRequest req) {
		String langCodeFromConfig = CONF.defaultLanguageCode();
		String cookieLoc = getCookieValue(req, CONF.localeCookie());
		Locale fromReq = (req == null) ? Locale.getDefault() : req.getLocale();
		Locale requestLocale = langutils.getProperLocale(fromReq.toString());
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

	public void getProfiles(List<? extends ParaObject> objects) {
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
				if (clSize == post.getItemcount().getLimit() && pc.getCount(Utils.type(Comment.class),
						Collections.singletonMap("parentid", post.getId())) > clSize) {
					clSize++; // hack to show the "more" button
				}
			}
			post.getItemcount().setCount(clSize);
		}
		if (!forUpdate.isEmpty()) {
			pc.updateAll(allPosts);
		}
	}

	public void getLinkedComment(Post showPost, HttpServletRequest req) {
		if (showPost != null && req.getParameter("commentid") != null) {
			Comment c = pc.read(req.getParameter("commentid"));
			if (c != null) {
				if (showPost.getComments() == null) {
					showPost.setComments(List.of(c));
					showPost.getItemcount().setCount(1);
				} else {
					Set<Comment> comments = new LinkedHashSet<>(showPost.getComments());
					comments.add(c);
					showPost.setComments(List.of(comments.toArray(Comment[]::new)));
				}
			}
		}
	}

	public void getVotes(List<Post> allPosts, Profile authUser) {
		if (authUser == null) {
			return;
		}
		Map<String, Vote> allVotes = new HashMap<>();
		List<String> allVoteIds = new ArrayList<String>();
		for (Post post : allPosts) {
			allVoteIds.add(new Vote(authUser.getId(), post.getId(), Votable.VoteValue.UP).getId());
		}
		if (!allVoteIds.isEmpty()) {
			for (ParaObject vote : pc.readAll(allVoteIds)) {
				allVotes.put(((Vote) vote).getParentid(), (Vote) vote);
			}
		}
		for (Post post : allPosts) {
			post.setVote(allVotes.get(post.getId()));
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
		return req.getRequestURI().startsWith(CONF.serverContextPath() + "/api/") || req.getRequestURI().equals(CONF.serverContextPath() + "/api");
	}

	public boolean isAdmin(Profile authUser) {
		return authUser != null &&
				(User.Groups.ADMINS.toString().equals(authUser.getGroups()) && authUser.getEditorRoleEnabled());
	}

	public boolean isMod(Profile authUser) {
		return authUser != null && (isAdmin(authUser) ||
				(User.Groups.MODS.toString().equals(authUser.getGroups()) && authUser.getEditorRoleEnabled()));
	}

	public boolean isRecognizedAsAdmin(User u) {
		return u.isAdmin() || ADMINS.contains(u.getIdentifier()) ||
				ADMINS.stream().filter(s -> s.equalsIgnoreCase(u.getEmail())).findAny().isPresent();
	}

	public boolean canComment(Profile authUser, HttpServletRequest req) {
		return isAuthenticated(req) && ((authUser.hasBadge(ENTHUSIAST) || CONF.newUsersCanComment() || isMod(authUser)));
	}

	public boolean postsNeedApproval() {
		return CONF.postsNeedApproval();
	}

	public boolean postNeedsApproval(Profile authUser) {
		return postsNeedApproval() && authUser.getVotes() < CONF.postsReputationThreshold() && !isMod(authUser);
	}

	public String getWelcomeMessage(Profile authUser) {
		return authUser == null ? CONF.welcomeMessage().replaceAll("'", "&apos;") : "";
	}

	public String getWelcomeMessageOnLogin(Profile authUser) {
		if (authUser == null) {
			return "";
		}
		String welcomeMsgOnlogin = CONF.welcomeMessageOnLogin();
		if (StringUtils.contains(welcomeMsgOnlogin, "{{")) {
			welcomeMsgOnlogin = Utils.compileMustache(Collections.singletonMap("user",
					ParaObjectUtils.getAnnotatedFields(authUser, false)), welcomeMsgOnlogin);
		}
		return welcomeMsgOnlogin.replaceAll("'", "&apos;");
	}

	public String getWelcomeMessagePreLogin(Profile authUser, HttpServletRequest req) {
		if (StringUtils.startsWithIgnoreCase(req.getRequestURI(), CONF.serverContextPath() + SIGNINLINK)) {
			return authUser == null ? CONF.welcomeMessagePreLogin().replaceAll("'", "&apos;") : "";
		}
		return "";
	}

	public boolean isDefaultSpace(String space) {
		return DEFAULT_SPACE.equalsIgnoreCase(getSpaceId(space));
	}

	public boolean isDefaultSpace(Sysprop space) {
		return space != null && isDefaultSpace(space.getId());
	}

	public String getDefaultSpace() {
		return DEFAULT_SPACE;
	}

	public boolean isAllSpaces(String space) {
		return ALL_MY_SPACES.equalsIgnoreCase(getSpaceId(space));
	}

	public Set<Sysprop> getAllSpaces() {
		if (allSpaces == null) {
			allSpaces = new LinkedHashSet<>(pc.findQuery("scooldspace", "*", new Pager(Config.DEFAULT_LIMIT)));
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
			if (StringUtils.startsWithIgnoreCase(space, spaceId + Para.getConfig().separator()) || space.equalsIgnoreCase(spaceId)) {
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
				return s.getId() + Para.getConfig().separator() + s.getName();
			}
		}
		String spaceAttr = (String) req.getAttribute(CONF.spaceCookie());
		String spaceValue = StringUtils.isBlank(spaceAttr) ? Utils.base64dec(getCookieValue(req, CONF.spaceCookie())) : spaceAttr;
		String space = getValidSpaceId(authUser, spaceValue);
		return verifyExistingSpace(authUser, space);
	}

	public void storeSpaceIdInCookie(String space, HttpServletRequest req, HttpServletResponse res) {
		// directly set the space on the requests, overriding the cookie value
		// used for setting the space from a direct URL to a particular space
		req.setAttribute(CONF.spaceCookie(), space);
		HttpUtils.setRawCookie(CONF.spaceCookie(), Utils.base64encURL(space.getBytes()),
				req, res, true, "Strict", StringUtils.isBlank(space) ? 0 : 365 * 24 * 60 * 60);
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
				return s.getId() + Para.getConfig().separator() + s.getName(); // updates current space name in case it was renamed
			}
		}
		return space;
	}

	public String getValidSpaceIdExcludingAll(Profile authUser, String space, HttpServletRequest req) {
		String s = StringUtils.isBlank(space) ? getSpaceIdFromCookie(authUser, req) : space;
		return isAllSpaces(s) ? DEFAULT_SPACE : s;
	}

	private String getValidSpaceId(Profile authUser, String space) {
		if (authUser == null) {
			return DEFAULT_SPACE;
		}
		String defaultSpace = authUser.hasSpaces() ? ALL_MY_SPACES : DEFAULT_SPACE;
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
		if (StringUtils.isBlank(space) || "default".equalsIgnoreCase(space)) {
			return DEFAULT_SPACE;
		}
		String s = StringUtils.contains(space, Para.getConfig().separator()) ?
				StringUtils.substring(space, 0, space.lastIndexOf(Para.getConfig().separator())) : "scooldspace:" + space;
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
			if (isMod(authUser)) {
				return "*";
			} else if (authUser != null && authUser.hasSpaces()) {
				return "(" + authUser.getSpaces().stream().map(s -> "properties.space:\"" + s + "\"").
						collect(Collectors.joining(" OR ")) + ")";
			} else {
				return "properties.space:\"" + DEFAULT_SPACE + "\"";
			}
//		} else if (isDefaultSpace(spaceId) && isMod(authUser)) { // DO NOT MODIFY!
//			return "*";
		} else {
			return "properties.space:\"" + spaceId + "\"";
		}
	}

	public Sysprop buildSpaceObject(String space) {
		String spaceId, spaceName;
		String col = Para.getConfig().separator();
		if (space.startsWith(getSpaceId(space))) {
			spaceId = StringUtils.substringBefore(StringUtils.substringAfter(space, col), col);
			spaceName = StringUtils.substringAfterLast(space, col);
		} else {
			spaceId = space;
			spaceName = Utils.abbreviate(space, 255).replaceAll(col, "");
		}
		Sysprop s = new Sysprop();
		s.setType("scooldspace");
		s.setId(getSpaceId(Utils.noSpaces(Utils.stripAndTrim(spaceId, " "), "-")));
		s.setName(isDefaultSpace(space) ? "default" : spaceName);
		return s;
	}

	public boolean isAutoAssignedSpace(Sysprop space) {
		return space != null && (isAutoAssignedSpaceInConfig(space) ||
				Optional.ofNullable(space.getTags()).orElse(List.of()).contains("assign-to-all"));
	}

	public boolean isAutoAssignedSpaceInConfig(Sysprop space) {
		return space != null && (getAutoAssignedSpacesFromConfig().contains(space.getName()) ||
				getAutoAssignedSpacesFromConfig().stream().map(s -> buildSpaceObject(s).getId()).
						anyMatch(i -> i.equalsIgnoreCase(space.getId())));
	}

	public Set<String> getAutoAssignedSpacesFromConfig() {
		if (autoAssignedSpacesFromConfig == null) {
			autoAssignedSpacesFromConfig = Set.of(ScooldUtils.getConfig().autoAssignSpaces().split("\\s*,\\s*"));
		}
		return autoAssignedSpacesFromConfig;
	}

	public String[] getAllAutoAssignedSpaces() {
		Set<String> allAutoAssignedSpaces = new LinkedHashSet<>();
		allAutoAssignedSpaces.addAll(getAllSpaces().parallelStream().
				filter(this::isAutoAssignedSpace).
				filter(Predicate.not(this::isDefaultSpace)).
				map(s -> s.getId() + Para.getConfig().separator() + s.getName()).collect(Collectors.toSet()));
		allAutoAssignedSpaces.addAll(getAutoAssignedSpacesFromConfig());
		return allAutoAssignedSpaces.toArray(String[]::new);
	}

	public boolean assignSpacesToUser(Profile authUser, String... spaces) {
		if (spaces != null && spaces.length > 0) {
			//DO: CHECK IF SPACES HAVE CHANGED FIRST! NO CHANGE - NO OP
			Map<String, Sysprop> spaceObjectsMap = new HashMap<>(spaces.length);
			for (String space : spaces) {
				Sysprop s = buildSpaceObject(space);
				spaceObjectsMap.put(s.getId(), s);
			}
			List<Sysprop> spacez = pc.readAll(new ArrayList<>(spaceObjectsMap.keySet()));
			Set<String> assignedSpaces = new HashSet<>(spacez.size());
			for (Sysprop space : spacez) {
				assignedSpaces.add(space.getId() + Para.getConfig().separator() + space.getName());
				spaceObjectsMap.remove(space.getId());
			}
			if (CONF.resetSpacesOnNewAssignment(authUser.getUser().isOAuth2User()
					|| authUser.getUser().isLDAPUser() || authUser.getUser().isSAMLUser())) {
				authUser.setSpaces(assignedSpaces);
			} else {
				authUser.getSpaces().addAll(assignedSpaces);
			}
			if (!spaceObjectsMap.isEmpty()) {
				// create the remaining spaces which were missing
				ArrayList<Sysprop> missingSpaces = new ArrayList<>(spaceObjectsMap.size());
				for (Sysprop missingSpace : spaceObjectsMap.values()) {
					authUser.getSpaces().add(missingSpace.getId() + Para.getConfig().separator() + missingSpace.getName());
					missingSpaces.add(missingSpace);
					getAllSpaces().add(missingSpace); // if we don't add it admins won't see the new space in the list
				}
				pc.createAll(missingSpaces);
				return true;
			}
			// Please, consider buying Scoold Pro which doesn't have this limitation.
			if (authUser.getSpaces().size() > MAX_SPACES) {
				authUser.setSpaces(authUser.getSpaces().stream().limit(MAX_SPACES).collect(Collectors.toSet()));
			}
		}
		return false;
	}

	public void assingSpaceToAllUsers(Sysprop space) {
		if (space == null) {
			return;
		}
		Para.asyncExecute(() -> {
			pc.updateAllPartially((toUpdate, pager) -> {
				List<Profile> profiles = pc.findQuery(Utils.type(Profile.class), "*", pager);
				profiles.stream().forEach(p -> {
					Map<String, Object> profile = new HashMap<>();
					profile.put(Config._ID, p.getId());
					p.getSpaces().add(space.getId() + Para.getConfig().separator() + space.getName());
					profile.put("spaces", p.getSpaces());
					toUpdate.add(profile);
				});
				return profiles;
			});
		});
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
			String template = "(name:({1}) OR name:({2} OR properties.originalName:{1} OR properties.originalName:{2} OR {3}) "
					+ "OR properties.location:({0}) OR properties.aboutme:({0}) OR properties.groups:({0}))";
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

	public boolean canDelete(Post showPost, Profile authUser) {
		return canDelete(showPost, authUser, null);
	}

	public boolean canDelete(Post showPost, Profile authUser, String approvedAnswerId) {
		if (authUser == null) {
			return false;
		}
		if (CONF.deleteProtectionEnabled()) {
			if (showPost.isReply()) {
				return isMine(showPost, authUser) && !StringUtils.equals(approvedAnswerId, showPost.getId());
			} else {
				return isMine(showPost, authUser) && showPost.getAnswercount() == 0;
			}
		}
		return isMine(showPost, authUser);
	}

	public boolean canApproveReply(Post showPost, Profile authUser) {
		switch (CONF.answersApprovedBy()) {
			case "admins":
				return isAdmin(authUser);
			case "moderators":
			case "mods":
				return isMod(authUser);
			default:
				return canEdit(showPost, authUser) && (isMine(showPost, authUser) || isMod(authUser));
		}
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

	public Map<String, String> validateQuestionTags(Question q, Map<String, String> errors, HttpServletRequest req) {
		Set<String> tagz = Optional.ofNullable(q.getTags()).orElse(List.of()).stream().
				filter(t -> !StringUtils.isBlank(t)).distinct().collect(Collectors.toSet());
		long tagCount = tagz.size();
		if (!CONF.tagCreationAllowed() && !ScooldUtils.getInstance().isMod(q.getAuthor())) {
			q.setTags(pc.findByIds(tagz.stream().map(t -> new Tag(t).getId()).collect(Collectors.toList())).stream().
					map(tt -> ((Tag) tt).getTag()).collect(Collectors.toList()));
			tagCount = q.getTags().size();
		}
		if (CONF.minTagsPerPost() > tagCount) {
			errors.put(Config._TAGS, Utils.formatMessage(getLang(req).get("tags.toofew"), CONF.minTagsPerPost()));
		}
		return errors;
	}

	public String getFullAvatarURL(Profile profile, AvatarFormat format) {
		return avatarRepository.getLink(profile, format);
	}

	public void clearSession(HttpServletRequest req, HttpServletResponse res) {
		if (req != null) {
			String jwt = HttpUtils.getStateParam(CONF.authCookie(), req);
			if (!StringUtils.isBlank(jwt)) {
				if (CONF.oneSessionPerUser()) {
					logger.debug("Trying to revoke all user tokens for user...");
					ParaClient pcc = new ParaClient(CONF.paraAccessKey(), CONF.paraSecretKey());
					setParaEndpointAndApiPath(pcc);
					pcc.setAccessToken(jwt);
					pcc.revokeAllTokens();
					pcc.signOut();
				}
				HttpUtils.removeStateParam(CONF.authCookie(), req, res);
			}
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
			addBadgeOnce(authUser, Profile.Badge.ENTHUSIAST, authUser.getVotes() >= CONF.enthusiastIfHasRep());
			addBadgeOnce(authUser, Profile.Badge.FRESHMAN, authUser.getVotes() >= CONF.freshmanIfHasRep());
			addBadgeOnce(authUser, Profile.Badge.SCHOLAR, authUser.getVotes() >= CONF.scholarIfHasRep());
			addBadgeOnce(authUser, Profile.Badge.TEACHER, authUser.getVotes() >= CONF.teacherIfHasRep());
			addBadgeOnce(authUser, Profile.Badge.PROFESSOR, authUser.getVotes() >= CONF.professorIfHasRep());
			addBadgeOnce(authUser, Profile.Badge.GEEK, authUser.getVotes() >= CONF.geekIfHasRep());
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
		model.put("footerhtml", CONF.emailsFooterHtml());
		String fqdn = CONF.rewriteInboundLinksWithFQDN();
		if (!StringUtils.isBlank(fqdn)) {
			model.entrySet().stream().filter(e -> (e.getValue() instanceof String)).forEachOrdered(e -> {
				model.put(e.getKey(), StringUtils.replace((String) e.getValue(), CONF.serverUrl(), fqdn));
			});
		}
		return Utils.compileMustache(model, loadEmailTemplate("notify"));
	}

	public boolean isValidJWToken(String jwt) {
		String appSecretKey = CONF.appSecretKey();
		String masterSecretKey = CONF.paraSecretKey();
		return isValidJWToken(appSecretKey, jwt) || isValidJWToken(masterSecretKey, jwt);
	}

	boolean isValidJWToken(String secret, String jwt) {
		try {
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
		return generateJWToken(claims, CONF.jwtExpiresAfterSec());
	}

	public SignedJWT generateJWToken(Map<String, Object> claims, long validitySeconds) {
		String secret = CONF.appSecretKey();
		if (!StringUtils.isBlank(secret)) {
			try {
				Date now = new Date();
				JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder();
				claimsSet.issueTime(now);
				if (validitySeconds > 0) {
					claimsSet.expirationTime(new Date(now.getTime() + (validitySeconds * 1000)));
				}
				claimsSet.notBeforeTime(now);
				claimsSet.claim(Config._APPID, CONF.paraAccessKey());
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
		if (CONF.cspHeaderEnabled()) {
			response.setHeader("Content-Security-Policy",
					(request.isSecure() ? "upgrade-insecure-requests; " : "") + CONF.cspHeader(nonce));
		}
		// HSTS Header
		if (CONF.hstsHeaderEnabled()) {
			response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
		}
		// Frame Options Header
		if (CONF.framingHeaderEnabled()) {
			response.setHeader("X-Frame-Options", "SAMEORIGIN");
		}
		// XSS Header
		if (CONF.xssHeaderEnabled()) {
			response.setHeader("X-XSS-Protection", "1; mode=block");
		}
		// Content Type Header
		if (CONF.contentTypeHeaderEnabled()) {
			response.setHeader("X-Content-Type-Options", "nosniff");
		}
		// Referrer Header
		if (CONF.referrerHeaderEnabled()) {
			response.setHeader("Referrer-Policy", "strict-origin");
		}
		// Permissions Policy Header
		if (CONF.permissionsHeaderEnabled()) {
			response.setHeader("Permissions-Policy", "geolocation=()");
		}
	}

	public boolean cookieConsentGiven(HttpServletRequest request) {
		return !CONF.cookieConsentRequired() || "allow".equals(HttpUtils.getCookieValue(request, "cookieconsent_status"));
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
		return CONF.externalScripts();
	}

	public List<String> getExternalStyles() {
		String extStyles = CONF.externalStyles();
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
			String inline = CONF.inlineCSS();
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
					customTheme = pc.update(custom);
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
		String id = "theme" + Para.getConfig().separator() + "custom";
		boolean isCustom = "custom".equalsIgnoreCase(themeName);
		String css = isCustom ? themeCSS : "";
		Sysprop custom = new Sysprop(id);
		custom.setName(StringUtils.isBlank(css) && isCustom ? "default" : themeName);
		custom.addProperty("theme", css);
		customTheme = pc.create(custom);
		FILE_CACHE.put("theme", themeName);
		FILE_CACHE.put(getThemeKey(themeName), isCustom ? css : loadResource(getThemeKey(themeName)));
	}

	public Sysprop getCustomTheme() {
		String id = "theme" + Para.getConfig().separator() + "custom";
		if (customTheme == null) {
			customTheme = (Sysprop) Optional.ofNullable(pc.read(id)).orElseGet(this::getDefaultThemeObject);
		}
		return customTheme;
	}

	private Sysprop getDefaultThemeObject() {
		String themeName = "default";
		Sysprop s = new Sysprop("theme" + Para.getConfig().separator() + "custom");
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

	public String getLogoUrl(Profile authUser, HttpServletRequest req) {
		return isDarkModeEnabled(authUser, req) ? CONF.logoDarkUrl() : CONF.logoUrl();
	}

	public String getSmallLogoUrl() {
		String defaultLogo = CONF.serverUrl() + CONF.imagesLink() + "/logowhite.png";
		String logoUrl = CONF.logoSmallUrl();
		String defaultMainLogoUrl = CONF.imagesLink() + "/logo.svg";
		String mainLogoUrl = CONF.logoUrl();
		if (!defaultLogo.equals(logoUrl)) {
			return logoUrl;
		} else if (!mainLogoUrl.equals(defaultMainLogoUrl)) {
			return mainLogoUrl;
		}
		return logoUrl;
	}

	public boolean isPasswordStrongEnough(String password) {
		if (StringUtils.length(password) >= CONF.minPasswordLength()) {
			int score = 0;
			if (password.matches(".*[a-z].*")) {
				score++;
			}
			if (password.matches(".*[A-Z].*")) {
				score++;
			}
			if (password.matches(".*[0-9].*")) {
				score++;
			}
			if (password.matches(".*[^\\w\\s\\n\\t].*")) {
				score++;
			}
			// 1 = good strength, 2 = medium strength, 3 = high strength
			if (CONF.minPasswordStrength() <= 1) {
				return score >= 2;
			} else if (CONF.minPasswordStrength() == 2) {
				return score >= 3;
			} else {
				return score >= 4;
			}
		}
		return false;
	}

	public String getCSPNonce() {
		return Utils.generateSecurityToken(16);
	}

	public String getFacebookLoginURL() {
		return "https://www.facebook.com/dialog/oauth?client_id=" + CONF.facebookAppId() +
				"&response_type=code&scope=email&redirect_uri=" + getParaEndpoint() +
				"/facebook_auth&state=" + getParaAppId();
	}

	public String getGoogleLoginURL() {
		return "https://accounts.google.com/o/oauth2/v2/auth?" +
				"client_id=" + CONF.googleAppId() + "&response_type=code&scope=openid%20profile%20email&redirect_uri="
				+ getParaEndpoint() + "/google_auth&state=" + getParaAppId();
	}

	public String getGitHubLoginURL() {
		return "https://github.com/login/oauth/authorize?response_type=code&client_id=" + CONF.githubAppId() +
				"&scope=user%3Aemail&state=" + getParaAppId() +
				"&redirect_uri=" + getParaEndpoint() + "/github_auth";
	}

	public String getLinkedInLoginURL() {
		return "https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=" + CONF.linkedinAppId() +
				"&scope=r_liteprofile%20r_emailaddress&state=" + getParaAppId() +
				"&redirect_uri=" + getParaEndpoint() + "/linkedin_auth";
	}

	public String getTwitterLoginURL() {
		return getParaEndpoint() + "/twitter_auth?state=" + getParaAppId();
	}

	public String getMicrosoftLoginURL() {
		return "https://login.microsoftonline.com/" + CONF.microsoftTenantId() +
				"/oauth2/v2.0/authorize?response_type=code&client_id=" + CONF.microsoftAppId() +
				"&scope=https%3A%2F%2Fgraph.microsoft.com%2Fuser.read&state=" + getParaAppId() +
				"&redirect_uri=" + getParaEndpoint() + "/microsoft_auth";
	}

	public String getSlackLoginURL() {
		return "https://slack.com/oauth/v2/authorize?response_type=code&client_id=" + CONF.slackAppId() +
				"&user_scope=identity.basic%20identity.email%20identity.team%20identity.avatar&state=" + getParaAppId() +
				"&redirect_uri=" + getParaEndpoint() + "/slack_auth";
	}

	public String getAmazonLoginURL() {
		return "https://www.amazon.com/ap/oa?response_type=code&client_id=" + CONF.amazonAppId() +
				"&scope=profile&state=" + getParaAppId() +
				"&redirect_uri=" + getParaEndpoint() + "/amazon_auth";
	}

	public String getOAuth2LoginURL() {
		return CONF.oauthAuthorizationUrl("") + "?" +
				"response_type=code&client_id=" + CONF.oauthAppId("") +
				"&scope=" + CONF.oauthScope("") + getOauth2StateParam("") +
				"&redirect_uri=" + getParaEndpoint() + "/oauth2_auth" + getOauth2AppidParam("");
	}

	public String getOAuth2SecondLoginURL() {
		return CONF.oauthAuthorizationUrl("second") + "?" +
				"response_type=code&client_id=" + CONF.oauthAppId("second") +
				"&scope=" +  CONF.oauthScope("second") + getOauth2StateParam("second") +
				"&redirect_uri=" + getParaEndpoint() + "/oauth2_auth" + getOauth2AppidParam("second");
	}

	public String getOAuth2ThirdLoginURL() {
		return CONF.oauthAuthorizationUrl("third") + "?" +
				"response_type=code&client_id=" + CONF.oauthAppId("third") +
				"&scope=" +  CONF.oauthScope("third") + getOauth2StateParam("third") +
				"&redirect_uri=" + getParaEndpoint() + "/oauth2_auth" + getOauth2AppidParam("third");
	}

	public String getParaEndpoint() {
		return CONF.redirectUri();
	}

	public String getParaAppId() {
		return StringUtils.removeStart(CONF.paraAccessKey(), "app:");
	}

	private String getOauth2StateParam(String a) {
		return "&state=" + (CONF.oauthAppidInStateParamEnabled(a) ? getParaAppId() : UUID.randomUUID().toString());
	}

	private String getOauth2AppidParam(String a) {
		return CONF.oauthAppidInStateParamEnabled(a) ? "" : "?appid=" + getParaAppId();
	}

	public String getFirstConfiguredLoginURL() {
		if (!CONF.facebookAppId().isEmpty()) {
			return getFacebookLoginURL();
		}
		if (!CONF.googleAppId().isEmpty()) {
			return getGoogleLoginURL();
		}
		if (!CONF.githubAppId().isEmpty()) {
			return getGitHubLoginURL();
		}
		if (!CONF.linkedinAppId().isEmpty()) {
			return getLinkedInLoginURL();
		}
		if (!CONF.twitterAppId().isEmpty()) {
			return getTwitterLoginURL();
		}
		if (isMicrosoftAuthEnabled()) {
			return getMicrosoftLoginURL();
		}
		if (isSlackAuthEnabled()) {
			return getSlackLoginURL();
		}
		if (!CONF.amazonAppId().isEmpty()) {
			return getAmazonLoginURL();
		}
		if (!CONF.oauthAppId("").isEmpty()) {
			return getOAuth2LoginURL();
		}
		if (!CONF.oauthAppId("second").isEmpty()) {
			return getOAuth2SecondLoginURL();
		}
		if (!CONF.oauthAppId("third").isEmpty()) {
			return getOAuth2ThirdLoginURL();
		}
		return SIGNINLINK + "?code=3&error=true";
	}
}
