package com.erudika.scoold.pages;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import com.erudika.para.client.ParaClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.User;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.i18n.CurrencyUtils;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.ContactDetail;
import com.erudika.scoold.core.ContactDetail.ContactDetailType;
import com.erudika.scoold.core.Feedback.FeedbackType;
import com.erudika.scoold.core.Grouppost;
import com.erudika.scoold.core.Language;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.core.Report.ReportType;
import com.erudika.scoold.core.ScooldUser;
import com.erudika.scoold.core.ScooldUser.Badge;
import com.erudika.scoold.utils.AppConfig;
import com.erudika.scoold.utils.LanguageUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.click.Page;
import org.apache.click.control.Field;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Option;
import org.apache.click.control.Radio;
import org.apache.click.control.RadioGroup;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.control.TextArea;
import org.apache.click.control.TextField;
import org.apache.click.util.ClickUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Base extends Page {
	private static final long serialVersionUID = 1L;

	public static final String APPNAME = Config.APP_NAME; //app name
	public static final String CDN_URL = "http://static.scoold.com";
	public static final boolean IN_BETA = true;
	public static final boolean USE_SESSIONS = false;
	public static final boolean IN_PRODUCTION = Config.IN_PRODUCTION;
	public static final boolean IN_DEVELOPMENT = !IN_PRODUCTION;
	public static final int MAX_ITEMS_PER_PAGE = Config.MAX_ITEMS_PER_PAGE;
	public static final long SESSION_TIMEOUT_SEC = Config.SESSION_TIMEOUT_SEC;
	public static final long ONE_YEAR = 365 * 24 * 60 * 60 * 1000;
	public static final String SEPARATOR = Config.SEPARATOR;
	public static final String AUTH_USER = Config.AUTH_COOKIE;

	public static final String FEED_KEY_SALT = ":scoold";
	public static final String MOBILE_COOKIE = "scoold-mobile";
	public static final String FB_APP_ID = Config.FB_APP_ID;
	public static final Logger logger = LoggerFactory.getLogger(Base.class);

	public final String prefix = getContext().getServletContext().getContextPath()+"/";
	public final String localeCookieName = Config.APP_NAME_NS + "-locale";
	public final String countryCookieName = Config.APP_NAME_NS + "-country";
	public final String schoolslink = prefix + "schools";
	public final String peoplelink = prefix + "people";
	public final String classeslink = prefix + "classes";
	public final String groupslink = prefix + "groups";
	public final String schoollink = prefix + "school";
	public final String classlink = prefix + "class";
	public final String grouplink = prefix + "group";
	public final String profilelink = prefix + "profile";

	public String minsuffix = "-min";
	public String imageslink = prefix + "images";
	public String scriptslink = prefix + "scripts";
	public String styleslink = prefix + "styles";

	public final String searchlink = prefix + "search";
	public final String searchquestionslink = searchlink + "/questions";
	public final String searchfeedbacklink = searchlink + "/feedback";
	public final String searchpeoplelink = searchlink + "/people";
	public final String searchclasseslink = searchlink + "/classes";
	public final String searchschoolslink = searchlink + "/schools";
	public final String signinlink = prefix + "signin";
	public final String signoutlink = prefix + "signout";
	public final String aboutlink = prefix + "about";
	public final String privacylink = prefix + "privacy";
	public final String termslink = prefix + "terms";
	public final String settingslink = prefix + "settings";
	public final String translatelink = prefix + "translate";
	public final String changepasslink = prefix + "changepass";
	public final String activationlink = prefix + "activation";
	public final String reportslink = prefix + "reports";
	public final String adminlink = prefix + "admin";
	public final String votedownlink = prefix + "votedown";
	public final String voteuplink = prefix + "voteup";
	public final String questionlink = prefix + "question";
	public final String questionslink = prefix + "questions";
	public final String commentlink = prefix + "comment";
	public final String postlink = prefix + "post";
	public final String feedbacklink = prefix + "feedback";
	public final String grouppostlink = grouplink + "/post";
	public final String languageslink = prefix + "languages";
	public String HOMEPAGE = "/";

	public String infoStripMsg = "";
	public boolean authenticated;
	public boolean canComment;
	public boolean includeFBscripts;
	public boolean isMobile;
	public ScooldUser authUser;
	public List<Comment> commentslist;
	public List<String> badgelist;
	public Pager itemcount;
	public String showParam;

	public transient Utils utils = Utils.getInstance();
	public transient CurrencyUtils currutils = CurrencyUtils.getInstance();
	public transient HttpServletRequest req = getContext().getRequest();
	public transient HttpServletResponse resp = getContext().getResponse();
	public static Map<String, String> deflang = Language.ENGLISH;
	public Map<String, String> lang = deflang;
	public Locale currentLocale;

	public transient ParaClient pc = AppConfig.client();
	public transient LanguageUtils langutils = new LanguageUtils();

	public Base() {
		checkAuth();
		cdnSwitch();
		includeFBscripts = false;
		itemcount = new Pager();
		showParam = getParamValue("show");
		canComment = authenticated && (authUser.hasBadge(Badge.ENTHUSIAST) || authUser.isModerator());
		commentslist = new ArrayList<Comment>();
		badgelist = new ArrayList<String>();
		isMobile = "true".equals(getStateParam(MOBILE_COOKIE));
		if (authenticated && !StringUtils.isBlank(authUser.getNewbadges())) {
			badgelist.addAll(Arrays.asList(authUser.getNewbadges().split(",")));
			authUser.setNewbadges("none");
		}
		addModel("userip", req.getRemoteAddr());
		addModel("mobilehide", (isMobile ? "hide" : "noclass"));
		addModel("isAjaxRequest", isAjaxRequest());
		addModel("reportTypes", ReportType.values());
	}

	public void onInit() {
		super.onInit(); //To change body of generated methods, choose Tools | Templates.
		initLanguage();
	}

	/* * PRIVATE METHODS * */

	private void cdnSwitch() {
		if (IN_PRODUCTION) {
			scriptslink = CDN_URL;
			imageslink = CDN_URL;
			styleslink = CDN_URL;
			minsuffix = "-min";
		} else {
			scriptslink = prefix + "scripts";
			imageslink = prefix + "images";
			styleslink = prefix + "styles";
			minsuffix = "";
		}
	}

	private void checkAuth() {
		try{
			if (getStateParam(Config.AUTH_COOKIE) != null) {
				User u = pc.read((String) parseJWTCookie().get("sub"));
				if (u != null) {
					HOMEPAGE = profilelink.concat("/").concat(u.getId());
					authUser = new ScooldUser(u.getEmail(), u.getActive(), ScooldUser.UserType.STUDENT, u.getName());
					authUser.setId(u.getId());
					authUser.setTimestamp(u.getTimestamp());
					authUser.setLastseen(u.getUpdated());
					// TODO: create new ScooldUser if missing

//					newUser.setActive(true);
//					newUser.setEmail(getParamValue("email"));
//					newUser.setFullname(getParamValue("name"));
//					newUser.setSubType(getParamValue("type"));
//					newUser.setIdentifier(identString);
//
//					if (identString.equals(ScooldAuthModule.ADMIN_FB_ID)) {
//						newUser.setGroups(Groups.ADMINS.toString());
//					}
//
//					if (IN_BETA) {
//						newUser.addBadge(Badge.TESTER);
//					}
//
//					Long uid = newUser.create();





//					long delta = System.currentTimeMillis() - authUser.getLastseen();
//					if (delta > 2 * 60 * 60 * 1000) {
//						// last seen 2 hours ago -> update
//						authUser.setLastseen(System.currentTimeMillis());
//						authUser.update();
//					}
					Utils.removeStateParam("intro", req, resp);
					infoStripMsg = "";
					authenticated = true;
				}
			} else {
				String intro = getStateParam("intro");
				infoStripMsg = "intro";
				if ("0".equals(intro)) {
					infoStripMsg = "";
				} else {
					Utils.setStateParam("intro", "1", req, resp);
				}
				authenticated = false;
			}
		} catch (Exception e) {
			authenticated = false;
			logger.warn("CheckAuth failed for {}: {}", req.getRemoteUser(), e);
			clearSession();
			if (!req.getRequestURI().startsWith("/index.htm"))
				setRedirect(HOMEPAGE);
		}
	}

	private void initLanguage() {
		langutils.setDefaultLanguage(Language.ENGLISH);
		String cookieLoc = ClickUtils.getCookieValue(req, localeCookieName);
		Locale requestLocale = langutils.getProperLocale(req.getLocale().getLanguage());
		String langFromLocation = getLanguageFromLocation();
		String langname = (cookieLoc != null) ? cookieLoc : (langFromLocation != null) ?
				langFromLocation : requestLocale.getLanguage();
		//locale cookie set?
		setCurrentLocale(langname, false);
	}

	/* * PUBLIC METHODS * */

	public final void setCurrentLocale(String langname, boolean setCookie) {
		currentLocale = langutils.getProperLocale(langname);
		lang = langutils.readLanguage(Config.APP_NAME_NS, currentLocale.getLanguage());

		if (setCookie) {
			//create a cookie
			int maxAge = 5 * 60 * 60 * 24 * 365;  //5 years
			ClickUtils.setCookie(req, resp, localeCookieName, currentLocale.getLanguage(), maxAge, "/");
		}
	}

	private String getLanguageFromLocation() {
		String language = null;
		try {
			String country = ClickUtils.getCookieValue(req, countryCookieName);
			if (country != null) {
				Locale loc = currutils.getLocaleForCountry(country.toUpperCase());
				if (loc != null) {
					language = loc.getLanguage();
				}
			}
		} catch(Exception exc) {}

		return language;
    }

	public final List<ContactDetailType> getContactDetailTypeArray() {
        ArrayList<ContactDetailType> mt = new ArrayList<ContactDetailType>();

        for(ContactDetailType s : ContactDetailType.values()) {
            if (s != com.erudika.scoold.core.ContactDetail.ContactDetailType.UNKNOWN)
				mt.add(s);
        }
        return mt;
    }

	public final boolean isAjaxRequest() {
		//context.isAjaxRequest()
		return getContext().isAjaxRequest();
	}

	public void processContactsRequest(ParaObject obj) {
		Map params = req.getParameterMap();
		if (param("contacts")) {
			try {
				PropertyUtils.setProperty(obj, "contacts", ContactDetail.getContactsFromParamsMap(params));
			} catch (Exception e) {}
		}
	}

	/* COMMENTS */

	public final void processNewCommentRequest(Sysprop parent) {
		if (param("deletecomment") && authenticated) {
			String id = getParamValue("deletecomment");
			Comment c = pc.read(id);
			if (c != null && (c.getCreatorid().equals(authUser.getId()) || inRole("mod"))) {
				// check parent and correct (for multi-parent-object pages)
				if (parent == null || !c.getParentid().equals(parent.getId())) {
					parent = pc.read(c.getParentid());
				}
				c.delete();
				if (!inRole("mod")) {
					addBadge(Badge.DISCIPLINED, true);
				}
				if (parent != null) {
					try {
//						Long count = (Long) PropertyUtils.getProperty(parent, "commentcount");
//						pc.putColumn(parent.getId(), "commentcount", Long.toString(count - 1));
						parent.addProperty("commentcount", Long.toString(((Long) parent.getProperty("commentcount")) - 1));
						parent.update();
					} catch (Exception ex) {
						logger.error(null, ex);
					}
				}
			}
		} else if (canComment && param("comment") && parent != null) {
			String comment = getParamValue("comment");
			String parentid = parent.getId();
			if (StringUtils.isBlank(comment)) return;
			Comment lastComment = new Comment();
			lastComment.setComment(comment);
			lastComment.setParentid(parentid);
			lastComment.setCreatorid(authUser.getId());
			lastComment.setAuthor(authUser.getName());

			if (lastComment.create() != null) {
				long commentCount = authUser.getComments();
				addBadgeOnce(Badge.COMMENTATOR, commentCount >= AppConfig.COMMENTATOR_IFHAS);
				authUser.setComments(commentCount + 1);
				authUser.update();
				commentslist.add(lastComment);
				addModel("newcomment", lastComment);

				try{
//					Long count = (Long) PropertyUtils.getProperty(parent, "commentcount");
//					pc.putColumn(parent.getId(), "commentcount", Long.toString(count + 1));
					parent.addProperty("commentcount", Long.toString(((Long) parent.getProperty("commentcount")) + 1));
					parent.update();
				} catch (Exception ex) {
					logger.error(null, ex);
				}
			}
		}
	}

	/****  POSTS  ****/

	public final void processPostEditRequest(Post post, String escapelink, boolean canEdit) {
		if (!canEdit || post == null) return;

		boolean isMine = (authenticated) ? authUser.getId().equals(post.getCreatorid()) : false;

		if (param("answer")) {
			// add new answer
			if (!post.isClosed() && !post.isReply() && !post.isBlackboard() &&
					post.getAnswercount() < AppConfig.MAX_REPLIES_PER_POST) {
				//create new answer
				Reply newq = new Reply();
				newq.setCreatorid(authUser.getId());
				newq.setParentid(post.getId());
				newq.setBody(getParamValue("body"));
				newq.create();

				post.setAnswercount(post.getAnswercount() + 1);
				if (post.getAnswercount() >= AppConfig.MAX_REPLIES_PER_POST) {
					post.setCloserid("0");
				}
				post.updateLastActivity();
				post.update();

				addBadge(Badge.EUREKA, newq.getCreatorid().equals(post.getCreatorid()));
//				if (!isAjaxRequest()) setRedirect(escapelink+"#post-"+newq.getId());
			}
		} else if (param("approve")) {
			String ansid = getParamValue("answerid");
			if (canEdit && ansid != null && isMine) {
				Reply answer = (Reply) pc.read(ansid);

				if (answer != null && answer.isReply()) {
					ScooldUser author = pc.read(answer.getCreatorid());
					if (author != null && authenticated) {
						boolean same = author.equals(authUser);

						if (ansid.equals(post.getAnswerid())) {
							// Answer approved award - UNDO
							post.setAnswerid(null);
							if (!same) {
								author.removeRep(AppConfig.ANSWER_APPROVE_REWARD_AUTHOR);
								authUser.removeRep(AppConfig.ANSWER_APPROVE_REWARD_VOTER);
							}
						} else {
							// Answer approved award - GIVE
							post.setAnswerid(ansid);
							if (!same) {
								author.addRep(AppConfig.ANSWER_APPROVE_REWARD_AUTHOR);
								authUser.addRep(AppConfig.ANSWER_APPROVE_REWARD_VOTER);
								addBadgeOnce(Badge.NOOB, true);
							}
						}

						author.update();
						authUser.update();
						post.update();
					}
				}
			}
		} else if (param("editpostid") || param("title")) {
			// edit post
			Post beforeUpdate = null;
			try {
				beforeUpdate = (Post) BeanUtils.cloneBean(post);
			} catch (Exception ex) {
				logger.error(null, ex);
			}

			//update post
			String title = getParamValue("title");
			if (!StringUtils.isBlank(title) && title.length() > 10) {
				post.setTitle(title);
			}
			if (param("body")) {
				post.setBody(getParamValue("body"));
			}
			if (param("tags") && post.isQuestion()) {
				post.setTags(Arrays.asList(StringUtils.split(getParamValue("tags"), ",")));
			}

			post.setLasteditby(authUser.getId());
			//note: update only happens if something has changed
			if (!post.equals(beforeUpdate)) {
				post.update();
				addBadgeOnce(Badge.EDITOR, true);
			}

//			if (!isAjaxRequest()) setRedirect(escapelink);

		} else if (param("close")) {
			if (inRole("mod")) {
				if (post.isClosed()) {
					post.setCloserid(null);
				} else {
					post.setCloserid(authUser.getId());
				}
				post.update();
			}
		} else if (param("restore")) {
			String revid = getParamValue("revisionid");
			if (canEdit && revid != null) {
				addBadge(Badge.BACKINTIME, true);
				post.restoreRevisionAndUpdate(revid);
			}
		} else if (param("delete")) {
			if (!post.isReply()) {
				if ((isMine || inRole("mod")) && !post.getDeleteme()) {
					post.setDeleteme(true);

					Report rep = new Report();
					rep.setParentid(post.getId());
					rep.setLink(escapelink);
					rep.setDescription(lang.get("posts.marked"));
					rep.setSubType(ReportType.OTHER);
					rep.setAuthor(authUser.getName());
					rep.setCreatorid(authUser.getId());

					String rid = rep.create();
					post.setDeletereportid(rid);
					post.update();
				}
			} else if (post.isReply()) {
				if (isMine || inRole("mod")) {
					Post parent = pc.read(post.getParentid());
					parent.setAnswercount(parent.getAnswercount() - 1);
					parent.update();
					post.delete();
				}
			}
		} else if (param("undelete")) {
			if (!post.isReply()) {
				if ((isMine || inRole("mod")) && post.getDeleteme()) {
					post.setDeleteme(false);
					Report rep = pc.read(post.getDeletereportid());
					if (rep != null) rep.delete();
					post.setDeletereportid(null);
					post.update();
				}
			}
		}

		if (escapelink != null && !isAjaxRequest()) {
			setRedirect(escapelink);
		}
	}

	public final <T extends Sysprop> Form getQuestionForm(Class<T> clazz, String parentID,
			Map<String, T> askablesMap) {

		if (!authenticated || askablesMap == null)
			return null;

		Class<?> type = com.erudika.scoold.core.Group.class.equals(clazz) ?
				Grouppost.class : Question.class;
		Form qForm = getPostForm(type, "qForm", "ask-question-form");
		Field pid = null;
		addModel("postParentClass", clazz.getSimpleName().toLowerCase());

//		Askable selected = askablesMap.get(parentID);
		if (StringUtils.isBlank(parentID)) {
			pid = new Select(Config._PARENTID, lang.get("posts.selectschool"), true);
			((Select) pid).add(new Option("", lang.get("chooseone")));
			for (ParaObject askable : askablesMap.values()) {
				((Select) pid).add(new Option(askable.getId(), askable.getName()));
			}
		} else {
			addModel("postParentId", parentID);
			pid = new HiddenField(Config._PARENTID, parentID);
		}

		qForm.add(pid);

		return qForm;
	}

	public final Form getAnswerForm() {
		Form aForm = new Form("aForm");
		aForm.setId("answer-question-form");

		TextArea body = new TextArea("body", lang.get("posts.answer"), true);
		body.setMinLength(15);
		body.setRows(4);
		body.setCols(5);

		TextField hideme = new TextField("additional", false);
		hideme.setLabel("Leave blank!");
		hideme.setAttribute("class", "hide");

        Submit submit = new Submit("answerbtn", lang.get("post"), this, "onAnswerClick");
        submit.setAttribute("class", "button rounded3");
		submit.setId("answer-btn");

		aForm.add(hideme);
        aForm.add(body);
        aForm.add(submit);

		return aForm;
	}

	public final Form getFeedbackForm() {
		return getPostForm(Feedback.class, "fForm", "write-feedback-form");
	}

	public final Form getPostForm(Class<?> type, String name, String id) {
		if (!authenticated) return null;

		Form qForm = new Form(name);
		qForm.setId(id);

        TextField title = new TextField("title", true);
        title.setLabel(lang.get("posts.title"));
        title.setMaxLength(255);
		title.setMinLength(10);
		title.setTabIndex(1);

		TextArea body = new TextArea("body", true);
		body.setLabel(lang.get("messages.text"));
		body.setMinLength(10);
		body.setMaxLength(AppConfig.MAX_TEXT_LENGTH);
		body.setRows(4);
		body.setCols(5);
		body.setTabIndex(2);

		Field tags = null;
		if (type.equals(Question.class) || type.equals(Grouppost.class)) {
			tags = new TextField("tags", true);
			tags.setLabel(lang.get("tags.title"));
			((TextField) tags).setMaxLength(255);
			tags.setTabIndex(3);
		} else if (type.equals(Feedback.class)) {
			tags = new RadioGroup("tags", lang.get("feedback.type"), true);
			String bug = FeedbackType.BUG.toString();
			String question = FeedbackType.QUESTION.toString();
			String suggestion = FeedbackType.SUGGESTION.toString();

			((RadioGroup) tags).add(new Radio(question, lang.get("feedback." + question)));
			((RadioGroup) tags).add(new Radio(suggestion, lang.get("feedback." + suggestion)));
			((RadioGroup) tags).add(new Radio(bug, lang.get("feedback." + bug)));
			((RadioGroup) tags).setVerticalLayout(false);
		}

		TextField hideme = new TextField("additional", false);
		hideme.setLabel("Leave blank!");
		hideme.setAttribute("class", "hide");

        Submit submit = new Submit("askbtn", lang.get("post"), this, "onAskClick");
        submit.setAttribute("class", "button rounded3");
		submit.setId("ask-btn");

        qForm.add(title);
		qForm.add(hideme);
        qForm.add(body);
		qForm.add(tags);
        qForm.add(submit);

		return qForm;
	}

	public final Form getPostEditForm(Post post) {
		if (post == null) return null;
		Form form = new Form("editPostForm"+post.getId());
		form.setId("post-edit-form-"+post.getId());

		TextArea  body = new TextArea("body", true);
		if (post.isReply()) {
			body.setLabel(lang.get("posts.answer"));
		} else {
			body.setLabel(lang.get("posts.question"));
		}
		body.setMinLength(15);
		body.setMaxLength(AppConfig.MAX_TEXT_LENGTH);
		body.setRows(4);
		body.setCols(5);
		body.setValue(post.getBody());

		if (post.isQuestion()) {
			TextField tags = new TextField("tags", false);
			tags.setLabel(lang.get("tags.tags"));
			tags.setMaxLength(255);
			tags.setValue(post.getTagsString());
			form.add(tags);
		}

		TextField hideme = new TextField("additional", false);
		hideme.setLabel("Leave blank!");
		hideme.setAttribute("class", "hide");

        Submit submit = new Submit("editbtn",
				lang.get("save"), this, "onPostEditClick");
        submit.setAttribute("class", "button rounded3 post-edit-btn");
		submit.setId("post-edit-btn-"+post.getId());

		form.add(hideme);
        form.add(body);
        form.add(submit);

		return form;
	}

	public final boolean isValidPostEditForm(Form qForm) {
        String tags = qForm.getFieldValue("tags");
		String additional = qForm.getFieldValue("additional");

		if (!StringUtils.isBlank(additional)) {
			qForm.setError("You are not supposed to do that!");
		}
		if (tags != null && StringUtils.split(tags, ",").length >
				AppConfig.MAX_TAGS_PER_POST) {
			qForm.setError(lang.get("tags.toomany"));
		}

		return qForm.isValid();
	}

	public final boolean isValidQuestionForm(Form qForm) {
        String tags = qForm.getFieldValue("tags");
		String additional = qForm.getFieldValue("additional");

		if (!StringUtils.isBlank(additional)) {
			qForm.setError("You are not supposed to do that!");
		}
		if (StringUtils.split(tags, ",").length > AppConfig.MAX_TAGS_PER_POST) {
			qForm.setError(lang.get("tags.toomany"));
		}

		return qForm.isValid();
	}

	public final boolean isValidAnswerForm(Form aForm, Post showPost) {
		String additional = aForm.getFieldValue("additional");
		if (!StringUtils.isBlank(additional)) {
			aForm.setError("You are not supposed to do that!");
		}
        return aForm.isValid();
	}

	public final void createAndGoToPost(Class<? extends Post> type) {
		String parentid = getParamValue(Config._PARENTID);

		Post newq = null;
		try {
			newq = type.getConstructor().newInstance();
			newq.setTitle(getParamValue("title"));
			newq.setBody(getParamValue("body"));
			newq.setTags(Arrays.asList(StringUtils.split(getParamValue("tags"), ",")));
			if (parentid != null) newq.setParentid(parentid);
			newq.setCreatorid(authUser.getId());
			newq.create();
		} catch (Exception ex) {
			logger.error(null, ex);
		}

		setRedirect(getPostLink(newq, false, false));
	}

	/******** VOTING ********/

	public void processVoteRequest(String type, String id) {
		if (id == null) return;
		ParaObject votable = pc.read(id);
		boolean result = false;
		Integer votes = 0;

		if (votable != null && authenticated) {
			try {
				ScooldUser author = pc.read(votable.getCreatorid());
				votes = (Integer) PropertyUtils.getProperty(votable, "votes");

				if (param("voteup")) {
					result = votable.voteUp(authUser.getId());
					if (!result) return;
					votes++;
					authUser.incrementUpvotes();

					int award = 0;

					if (votable instanceof Post) {
						Post p = (Post) votable;
						if (p.isReply()) {
							addBadge(Badge.GOODANSWER, votes >= AppConfig.GOODANSWER_IFHAS);
							award = AppConfig.ANSWER_VOTEUP_REWARD_AUTHOR;
						} else if (p.isQuestion()) {
							addBadge(Badge.GOODQUESTION, votes >= AppConfig.GOODQUESTION_IFHAS);
							award = AppConfig.QUESTION_VOTEUP_REWARD_AUTHOR;
						} else {
							award = AppConfig.VOTEUP_REWARD_AUTHOR;
						}
					} else {
						award = AppConfig.VOTEUP_REWARD_AUTHOR;
					}

					if (author != null) {
						author.addRep(award);
						author.update();
					}

				} else if (param("votedown")) {
					result = votable.voteDown(authUser.getId());
					if (!result) return;
					votes--;
					authUser.incrementDownvotes();

					if (StringUtils.equalsIgnoreCase(type,
							Comment.class.getSimpleName()) && votes <= -5) {
						//treat comment as offensive or spam - hide
						((Comment) votable).setHidden(true);
					} else if (StringUtils.equalsIgnoreCase(type,
							Post.class.getSimpleName()) && votes <= -5) {
						Post p = (Post) votable;

						//mark post for closing
						Report rep = new Report();
						rep.setParentid(id);
						rep.setLink(getPostLink(p, false, false));
						rep.setDescription(lang.get("posts.forclosing"));
						rep.setSubType(ReportType.OTHER);
						rep.setAuthor("System");

						rep.create();
					}

					if (author != null) {
						author.removeRep(AppConfig.POST_VOTEDOWN_PENALTY_AUTHOR);
						author.update();
						//small penalty to voter
						authUser.removeRep(AppConfig.POST_VOTEDOWN_PENALTY_VOTER);
					}
				}
			} catch (Exception ex) {
				logger.error(null, ex);
			}

			addBadgeOnce(Badge.SUPPORTER, authUser.getUpvotes() >= AppConfig.SUPPORTER_IFHAS);
			addBadgeOnce(Badge.CRITIC, authUser.getDownvotes() >= AppConfig.CRITIC_IFHAS);
			addBadgeOnce(Badge.VOTER, authUser.getTotalVotes() >= AppConfig.VOTER_IFHAS);

			if (result) {
				votable.setVotes(votes);
				votable.update();
//				pc.putColumn(votable.getId(), "votes", votes.toString());
			}
		}

		addModel("voteresult", result);
	}

	/******  MISC *******/

	public final boolean param(String param) {
		return getContext().getRequestParameter(param) != null;
	}

	public final String getParamValue(String param) {
		return getContext().getRequestParameter(param);
	}

	public String getPostLink(Post p, boolean plural, boolean noid) {
		return p.getPostLink(plural, noid, questionslink, questionlink,
				feedbacklink, grouplink, grouppostlink, classeslink, classlink);
	}

	public final int getIndexInBounds(int index, int count) {
		if (index >= count) index = count - 1;
		if (index < 0) index = 0;
		return index;
	}

	public final void setStateParam(String name, String value) {
		Utils.setStateParam(name, value, req, resp,
				USE_SESSIONS);
	}

	public final String getStateParam(String name) {
		return Utils.getStateParam(name, req);
	}

	public final void removeStateParam(String name) {
		Utils.removeStateParam(name, req, resp);
	}

	public final void clearSession() {
		if (req != null) {
			HttpSession session = req.getSession(false);
			if (session != null) {
				session.invalidate();
			}
			removeStateParam(Config.AUTH_COOKIE);
		}
	}

	public final Map<String, Object> parseJWTCookie() {
		try {
			String[] cookie = StringUtils.split(getStateParam(Config.AUTH_COOKIE), ".");
			if (cookie != null && cookie.length > 1) {
				return ParaObjectUtils.getJsonReader(Map.class).readValue(Utils.base64dec(cookie[1]));
			}
		} catch (Exception ex) {}
		return Collections.emptyMap();
	}

	public boolean inRole(String role) {
		return req.isUserInRole(role);
	}

	public final boolean addBadgeOnce(Badge b, boolean condition) {
		return addBadge(b, condition && !authUser.hasBadge(b));
	}

	public final boolean addBadge(Badge b, boolean condition) {
		return addBadge(b, null, condition);
	}

	public final boolean addBadge(Badge b, ScooldUser u, boolean condition) {
		if (u == null) u = authUser;
		if (!authenticated || !condition) return false;

		String newb = StringUtils.isBlank(u.getNewbadges()) ? "" : u.getNewbadges().concat(",");
		newb = newb.concat(b.toString());

		u.addBadge(b);
		u.setNewbadges(newb);

		if (!authUser.getId().equals(u.getId())) {
			u.update();
		}

		return true;
	}

	public final boolean removeBadge(Badge b, ScooldUser u, boolean condition) {
		if (u == null) u = authUser;
		if (!authenticated || !condition) return false;

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

	public void onDestroy() {
		if (authenticated && !isAjaxRequest()) {
			long oneYear = authUser.getTimestamp() + ONE_YEAR;

			addBadgeOnce(Badge.ENTHUSIAST, authUser.getVotes() >= AppConfig.ENTHUSIAST_IFHAS);
			addBadgeOnce(Badge.FRESHMAN, authUser.getVotes() >= AppConfig.FRESHMAN_IFHAS);
			addBadgeOnce(Badge.SCHOLAR, authUser.getVotes() >= AppConfig.SCHOLAR_IFHAS);
			addBadgeOnce(Badge.TEACHER, authUser.getVotes() >= AppConfig.TEACHER_IFHAS);
			addBadgeOnce(Badge.PROFESSOR, authUser.getVotes() >= AppConfig.PROFESSOR_IFHAS);
			addBadgeOnce(Badge.GEEK, authUser.getVotes() >= AppConfig.GEEK_IFHAS);
			addBadgeOnce(Badge.SENIOR, System.currentTimeMillis() >= oneYear);

			if (!StringUtils.isBlank(authUser.getNewbadges())) {
				if (authUser.getNewbadges().equals("none")) {
					authUser.setNewbadges(null);
				}
				authUser.update();
			}
		}
	}

	public static final String DESCRIPTION = "Scoold is friendly place where students and teachers "
			+ "can help each other and talk about anything related to education.";

	public static final String KEYWORDS = "scoold, knowledge sharing, collaboration, wiki, "
			+ "schools, students, classmates, alumni, teachers, contacts, social, "
			+ "network, classes, classroom, education";

	public String getTemplate() {
		return "base.htm";
	}
}
