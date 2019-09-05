/*
 * Copyright 2013-2019 Erudika. https://erudika.com
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
import com.erudika.para.core.App;
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
import com.erudika.scoold.core.Feedback;
import com.erudika.scoold.core.Post;
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
import com.typesafe.config.ConfigObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.ws.rs.WebApplicationException;
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
	private static final Map<String, String> EMAIL_TEMPLATES = new ConcurrentHashMap<String, String>();
	private static final Set<String> APPROVED_DOMAINS = new HashSet<>();
	private static final Set<String> ADMINS = new HashSet<>();
	private static final String EMAIL_ALERTS_PREFIX = "email-alerts" + Config.SEPARATOR;

	private static Set<String> coreTypes;
	static {
		coreTypes = new HashSet<>(Arrays.asList(Utils.type(Comment.class),
				Utils.type(Feedback.class),
				Utils.type(Profile.class),
				Utils.type(Question.class),
				Utils.type(Reply.class),
				Utils.type(Report.class),
				Utils.type(Revision.class),
				Utils.type(UnapprovedQuestion.class),
				Utils.type(UnapprovedReply.class)));
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
		boolean isApiRequest = isApiRequest(req);
		if (isApiRequest) {
			return checkApiAuth(req);
		} else if (HttpUtils.getStateParam(Config.AUTH_COOKIE, req) != null &&
				!StringUtils.endsWithAny(req.getRequestURI(), ".js", ".css", ".svg", ".png", ".jpg")) {
			User u = pc.me(HttpUtils.getStateParam(Config.AUTH_COOKIE, req));
			if (u != null && isEmailDomainApproved(u.getEmail())) {
				authUser = getOrCreateProfile(u, req);
				authUser.setUser(u);
				if (promoteOrDemoteUser(authUser, u) || updateProfilePictureAndName(authUser, u)) {
					authUser.update();
				}
			} else {
				clearSession(req, res);
				if (u != null) {
					logger.warn("Attempted signin from an unknown domain: {}", u.getEmail());
				} else {
					logger.info("Invalid JWT found in cookie {}.", Config.AUTH_COOKIE);
				}
				res.sendRedirect(SIGNINLINK + "?code=3&error=true");
				return null;
			}
		}
		return authUser;
	}

	private ParaObject checkApiAuth(HttpServletRequest req) {
		if (req.getRequestURI().equals("/api")) {
			return null;
		}
		String superToken = StringUtils.removeStart(req.getHeader(HttpHeaders.AUTHORIZATION), "Bearer ");
		if (StringUtils.isBlank(superToken)) {
			throw new WebApplicationException(401);
		}
		ParaObject app = pc.me(superToken);
		if (app == null || !(app instanceof App)) {
			throw new WebApplicationException(401);
		}
		return app;
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
			}
		}
		return false;
	}

	private Profile getOrCreateProfile(User u, HttpServletRequest req) {
		Profile authUser = pc.read(Profile.id(u.getId()));
		if (authUser == null) {
			authUser = new Profile(u.getId(), u.getName());
			authUser.setOriginalName(u.getName());
			authUser.setPicture(u.getPicture());
			authUser.setAppid(u.getAppid());
			authUser.setCreatorid(u.getId());
			authUser.setTimestamp(u.getTimestamp());
			authUser.setGroups(isRecognizedAsAdmin(u)
					? User.Groups.ADMINS.toString() : u.getGroups());
			// auto-assign spaces to new users
			String space = Config.getConfigParam("auto_assign_spaces", "");
			if (!StringUtils.isBlank(space) && !isDefaultSpace(space)) {
				Sysprop s = pc.read(getSpaceId(space));
				if (s != null) {
					authUser.getSpaces().add(s.getId() + Config.SEPARATOR + s.getName());
				}
			}

			authUser.create();
			if (!u.getIdentityProvider().equals("generic")) {
				sendWelcomeEmail(u, false, req);
			}
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

	public void sendWelcomeEmail(User user, boolean verifyEmail, HttpServletRequest req) {
		// send welcome email notification
		if (user != null) {
			Map<String, Object> model = new HashMap<String, Object>();
			Map<String, String> lang = getLang(req);
			String subject = Utils.formatMessage(lang.get("signin.welcome"), Config.APP_NAME);
			String body1 = Utils.formatMessage(lang.get("signin.welcome.body1"), Config.APP_NAME)  + "<br><br>";
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
		if (!APPROVED_DOMAINS.isEmpty()) {
			return APPROVED_DOMAINS.contains(StringUtils.substringAfter(email, "@"));
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

	@SuppressWarnings("unchecked")
	public void sendNewPostNotifications(Post question) {
		if (question != null) {
			Profile postAuthor = question.getAuthor(); // the current user - same as utils.getAuthUser(req)
			Map<String, Object> model = new HashMap<String, Object>();
			String name = postAuthor.getName();
			String body = Utils.markdownToHtml(question.getBody());
			String picture = Utils.formatMessage("<img src='{0}' width='25'>", postAuthor.getPicture());
			String postURL = getServerURL() + question.getPostLink(false, false);
			model.put("logourl", Config.getConfigParam("small_logo_url", "https://scoold.com/logo.png"));
			model.put("heading", Utils.formatMessage("{0} {1} posted:", picture, name));
			model.put("body", Utils.formatMessage("<h2><a href='{0}'>{1}</a></h2><div>{2}</div>",
					postURL, question.getTitle(), body));

			if (postsNeedApproval() && question instanceof UnapprovedQuestion) {
				Report rep = new Report();
				rep.setDescription("New question awaiting approval");
				rep.setSubType(Report.ReportType.OTHER);
				rep.setLink(question.getPostLink(false, false));
				rep.setAuthorName(question.getAuthor().getName());
				rep.create();
			}

			Set<String> emails = getNotificationSubscribers(EMAIL_ALERTS_PREFIX + "new_post_subscribers");
			if (emails != null) {
				emailer.sendEmail(new ArrayList<String>(emails),
						name + " posted the question '" + Utils.abbreviate(question.getTitle(), 255) +
								(question.getTitle().length() > 255 ? "...'" : "'"),
						Utils.compileMustache(model, loadEmailTemplate("notify")));
			}
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
						name + " replied to '" + Utils.abbreviate(reply.getTitle(), 255) +
								(reply.getTitle().length() > 255 ? "...'" : "'"),
						Utils.compileMustache(model, loadEmailTemplate("notify")));
			}
		}
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

	public boolean isDefaultSpacePublic() {
		return Config.getConfigBoolean("is_default_space_public", true);
	}

	public boolean isWebhooksEnabled() {
		return Config.getConfigBoolean("webhooks_enabled", true);
	}

	public Set<String> getCoreScooldTypes() {
		return Collections.unmodifiableSet(coreTypes);
	}

	public Pager getPager(String pageParamName, HttpServletRequest req) {
		return new Pager(NumberUtils.toInt(req.getParameter(pageParamName), 1), Config.MAX_ITEMS_PER_PAGE);
	}

	public Pager pagerFromParams(HttpServletRequest req) {
		Pager p = new Pager();
		p.setPage(NumberUtils.toLong(req.getParameter("page"), 1));
		p.setDesc(Boolean.parseBoolean(req.getParameter("desc")));
		p.setLimit(NumberUtils.toInt(req.getParameter("limit"), Config.MAX_ITEMS_PER_PAGE));
		String lastKey = req.getParameter("lastKey");
		String sort = req.getParameter("sort");
		if (!StringUtils.isBlank(lastKey)) {
			p.setLastKey(lastKey);
		}
		if (!StringUtils.isBlank(sort)) {
			p.setSortby(sort);
		}
		return p;
	}

	public String getLanguageCode(HttpServletRequest req) {
		String cookieLoc = getCookieValue(req, LOCALE_COOKIE);
		Locale requestLocale = langutils.getProperLocale(req.getLocale().toString());
		return (cookieLoc != null) ? cookieLoc : requestLocale.getLanguage();
	}

	public Locale getCurrentLocale(String langname) {
		Locale currentLocale = langutils.getProperLocale(langname);
		if (currentLocale == null) {
			currentLocale = langutils.getProperLocale("en");
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

	public boolean param(HttpServletRequest req, String param) {
		return req.getParameter(param) != null;
	}

	public boolean isAjaxRequest(HttpServletRequest req) {
		return req.getHeader("X-Requested-With") != null || req.getParameter("X-Requested-With") != null;
	}

	public boolean isApiRequest(HttpServletRequest req) {
		return req.getRequestURI().startsWith("/api/") || req.getRequestURI().equals("/api");
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

	public boolean isDefaultSpace(String space) {
		return DEFAULT_SPACE.equalsIgnoreCase(getSpaceId(space));
	}

	public boolean canAccessSpace(Profile authUser, String targetSpaceId) {
		if (authUser == null) {
			return isDefaultSpacePublic();
		}
		if (isMod(authUser)) {
			return true;
		}
		if (isDefaultSpace(targetSpaceId)) {
			// can user access the default space (blank)
			return isDefaultSpacePublic() || isMod(authUser) || !authUser.hasSpaces();
		}
		boolean isMemberOfSpace = false;
		for (String space : authUser.getSpaces()) {
			if (StringUtils.startsWithIgnoreCase(space, getSpaceId(targetSpaceId) + Config.SEPARATOR)) {
				isMemberOfSpace = true;
				break;
			}
		}
		return isMemberOfSpace;
	}

	public String getSpaceIdFromCookie(Profile authUser, HttpServletRequest req) {
		return getValidSpaceId(authUser, Utils.base64dec(getCookieValue(req, SPACE_COOKIE)));
	}

	public void storeSpaceIdInCookie(String space, HttpServletRequest req, HttpServletResponse res) {
		HttpUtils.setRawCookie(SPACE_COOKIE, Utils.base64encURL(space.getBytes()),
				req, res, false, StringUtils.isBlank(space) ? 0 : 365 * 24 * 60 * 60);
	}

	public String getValidSpaceId(Profile authUser, String space) {
		if (authUser == null) {
			return DEFAULT_SPACE;
		}
		String defaultSpace = authUser.hasSpaces() ? authUser.getSpaces().iterator().next() : DEFAULT_SPACE;
		String s = canAccessSpace(authUser, space) ? space : defaultSpace;
		return StringUtils.isBlank(s) ? DEFAULT_SPACE : s;
	}

	public String getSpaceName(String space) {
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

	public String getSpaceFilteredQuery(HttpServletRequest req) {
		Profile authUser = getAuthUser(req);
		String currentSpace = getSpaceIdFromCookie(authUser, req);
		return isDefaultSpace(currentSpace) ? (canAccessSpace(authUser, currentSpace) ? "*" : "") :
				"properties.space:\"" + currentSpace + "\"";
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
			q = q.replaceAll("[\\*\\?]", "").trim();
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
		if (email == null) {
			return "https://www.gravatar.com/avatar?d=retro&size=400";
		}
		return "https://www.gravatar.com/avatar/" + Utils.md5(email.toLowerCase()) + "?size=400&d=retro";
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
			HttpUtils.removeStateParam(Config.AUTH_COOKIE, req, res);
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
		if (in != null) {
			try (Scanner s = new Scanner(in).useDelimiter("\\A")) {
				template = s.hasNext() ? s.next() : "";
				if (!StringUtils.isBlank(template)) {
					EMAIL_TEMPLATES.put(name, template);
				}
			} catch (Exception ex) {
				logger.info("Couldn't load email template '{0}'. - {1}", name, ex.getMessage());
			} finally {
				try {
					in.close();
				} catch (IOException ex) {
					logger.error(null, ex);
				}
			}
		}
		return template;
	}

	public void setSecurityHeaders(String nonce, HttpServletRequest request, HttpServletResponse response) {
		// CSP Header
		if (Config.getConfigBoolean("csp_header_enabled", true)) {
			response.addHeader("Content-Security-Policy",
					Config.getConfigParam("csp_header", getDefaultContentSecurityPolicy(request.isSecure())).
							replaceFirst("\\{\\{nonce\\}\\}", nonce));
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

	public Map<String, Object> getExternalScripts() {
		if (Config.getConfig().hasPath("external_scripts")) {
			ConfigObject extScripts = Config.getConfig().getObject("external_scripts");
			if (extScripts != null && !extScripts.isEmpty()) {
				return extScripts.unwrapped();
			}
		}
		return Collections.emptyMap();
	}

	public String getCSPNonce() {
		return Utils.generateSecurityToken(16);
	}

	public String getDefaultContentSecurityPolicy(boolean isSecure) {
		return (isSecure ? "upgrade-insecure-requests; " : "")
				+ "default-src 'self'; "
				+ "base-uri 'self'; "
				+ "form-action 'self'; "
				+ "connect-src 'self' " + (Config.IN_PRODUCTION ? getServerURL() : "")
				+ " scoold.com www.google-analytics.com www.googletagmanager.com " + Config.getConfigParam("csp_connect_sources", "") + "; "
				+ "frame-src 'self' accounts.google.com staticxx.facebook.com " + Config.getConfigParam("csp_frame_sources", "") + "; "
				+ "font-src 'self' cdnjs.cloudflare.com fonts.gstatic.com fonts.googleapis.com " + Config.getConfigParam("csp_font_sources", "") + "; "
				+ "style-src 'self' 'unsafe-inline' fonts.googleapis.com cdnjs.cloudflare.com unpkg.com "
				+ (CDN_URL.startsWith("/") ? "" : CDN_URL) + " " + Config.getConfigParam("csp_style_sources", "") + "; "
				+ "img-src 'self' https: data:; "
				+ "object-src 'none'; "
				+ "report-uri /reports/cspv; "
				+ "script-src 'unsafe-inline' https: 'nonce-{{nonce}}' 'strict-dynamic';";
	}
}
