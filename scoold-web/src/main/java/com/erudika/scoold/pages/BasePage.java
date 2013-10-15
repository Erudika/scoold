package com.erudika.scoold.pages;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import com.erudika.para.persistence.DAO;
import com.erudika.para.core.ParaObject;
import com.erudika.para.search.Search;
import com.erudika.para.core.Votable;
import com.erudika.para.core.PObject;
import com.erudika.para.i18n.CurrencyUtils;
import com.erudika.para.i18n.LanguageUtils;
import com.erudika.para.security.AuthModule;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.ContactDetail;
import com.erudika.scoold.core.ContactDetail.ContactDetailType;
import com.erudika.scoold.core.Feedback.FeedbackType;
import com.erudika.scoold.core.Grouppost;
import com.erudika.scoold.core.Language;
import com.erudika.scoold.core.Media;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.core.Report.ReportType;
import com.erudika.scoold.core.User;
import com.erudika.scoold.core.User.Badge;
import com.erudika.scoold.util.Constants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.StringUtils;
/**
 *
 * @author Alex Bogdanovski <albogdano@me.com> 
 */
public class BasePage extends Page {
	private static final long serialVersionUID = 1L;
	
	public static final String APPNAME = Utils.PRODUCT_NAME; //app name
	public static final String CDN_URL = "http://static.scoold.com"; 
	public static final boolean IN_BETA = true;
	public static final boolean USE_SESSIONS = false;
	public static final boolean IN_PRODUCTION = Utils.IN_PRODUCTION;
	public static final boolean IN_DEVELOPMENT = !Utils.IN_PRODUCTION;
	public static final int MAX_ITEMS_PER_PAGE = Utils.MAX_ITEMS_PER_PAGE;
	public static final int SESSION_TIMEOUT_SEC = Utils.SESSION_TIMEOUT_SEC;
	public static final long ONE_YEAR = 365L*24L*60L*60L*1000L;
	public static final String SEPARATOR = Utils.SEPARATOR;
	public static final String AUTH_USER = Utils.AUTH_COOKIE;
	
	public static final String FEED_KEY_SALT = ":scoold";
	public static final String MOBILE_COOKIE = "scoold-mobile";
	public static final String FB_APP_ID = Utils.FB_APP_ID;
	public static final Logger logger = Logger.getLogger(BasePage.class.getName());

	public final String prefix = getContext().getServletContext().getContextPath()+"/";
	public final String localeCookieName = APPNAME.toLowerCase() + "-locale";
	public final String countryCookieName = APPNAME.toLowerCase() + "-country";
	public final String messageslink = prefix + "messages";
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
	public final String signuplink = prefix + "signup";
	public final String aboutlink = prefix + "about";
	public final String privacylink = prefix + "privacy";
	public final String termslink = prefix + "terms";
	public final String settingslink = prefix + "settings";
	public final String translatelink = prefix + "translate";
	public final String changepasslink = prefix + "changepass";
	public final String activationlink = prefix + "activation";
	public final String reportslink = prefix + "reports";
	public final String adminlink = prefix + "admin";
	public final String deletevideolink = prefix + "delete/video";
	public final String votedownlink = prefix + "votedown";
	public final String voteuplink = prefix + "voteup";
	public final String questionlink = prefix + "question";
	public final String questionslink = prefix + "questions";
	public final String commentlink = prefix + "comment";
	public final String medialink = prefix + "media";
	public final String messagelink = prefix + "message";
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
	public User authUser;
	public ArrayList<Comment> commentslist;
	public ArrayList<Media> medialist;
	public ArrayList<String> badgelist;
	public MutableLong mediacount;
	public MutableLong pagenum;
	public MutableLong itemcount;
	public String showParam;
	
	public transient Utils utils = Utils.getInstance();
	public transient CurrencyUtils currutils = CurrencyUtils.getInstance();
	public transient HttpServletRequest req = getContext().getRequest();
	public transient HttpServletResponse resp = getContext().getResponse();
	public static Map<String, String> deflang = Language.ENGLISH;
	public Map<String, String> lang = deflang;
	
	@Inject public transient DAO dao;
	@Inject public transient Search search;
	@Inject public transient LanguageUtils langutils;
		
	public BasePage() {
		req = getContext().getRequest();
		checkAuth();
		cdnSwitch();
		includeFBscripts = false;
		itemcount = new MutableLong(0);
		mediacount = new MutableLong(0);
		showParam = getParamValue("show");
		pagenum = new MutableLong(NumberUtils.toLong(getParamValue("page"), 1));
		if(pagenum.longValue() <= 0L) pagenum = new MutableLong(1);
		canComment = authenticated && (authUser.hasBadge(Badge.ENTHUSIAST) || authUser.isModerator());
		commentslist = new ArrayList<Comment> ();
		badgelist = new ArrayList<String> (); 
		isMobile = "true".equals(getStateParam(MOBILE_COOKIE));
		if(authenticated && !StringUtils.isBlank(authUser.getNewbadges())){
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
	
	private void cdnSwitch(){
		if (IN_PRODUCTION) {
			scriptslink = CDN_URL;
			imageslink = CDN_URL;
			styleslink = CDN_URL;
			minsuffix = "-min";
		}else{			
			scriptslink = prefix + "scripts";
			imageslink = prefix + "images";
			styleslink = prefix + "styles";
			minsuffix = "";
		}
	}
	
	private void checkAuth() {	
		try{
			if (AuthModule.getAuthenticatedUser() != null) {
				authUser = (User) AuthModule.getAuthenticatedUser();
				if(authUser != null){
					addModel("sectoken", AuthModule.getCSRFtoken(authUser));
					HOMEPAGE = profilelink.concat("/").concat(authUser.getId());
					long delta = System.currentTimeMillis() - authUser.getLastseen();
					if(delta > 2 * 60 * 60 * 1000){
						// last seen 2 hours ago -> update
						authUser.setLastseen(System.currentTimeMillis());
						authUser.update();
					}
					Utils.removeStateParam("intro", req, resp);
					infoStripMsg = "";
					authenticated = true;
				}
			} else {
				String intro = Utils.getStateParam("intro", req, resp);
				infoStripMsg = "intro";
				if ("0".equals(intro)) {
					infoStripMsg = "";
				}else{
					Utils.setStateParam("intro", "1", req, resp);
				}
				authenticated = false; 
			} 
		} catch (Exception e) {
			authenticated = false;
			logger.log(Level.WARNING, "CheckAuth failed for {0}: {1}", new Object[]{req.getRemoteUser(), e});
			clearSession();
			if(!req.getRequestURI().startsWith("/index.htm"))
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
		Locale loc = langutils.getProperLocale(langname);
		addModel("showCurrentLocale", loc.getDisplayLanguage(loc));
//		getContext().setLocale(loc);
		lang = langutils.readLanguage(loc.getLanguage());

		if(setCookie){
			//create a cookie
			int maxAge = 5 * 60 * 60 * 24 * 365;  //5 years
			ClickUtils.setCookie(req, resp, localeCookieName, 
					loc.getLanguage(), maxAge, "/");
		}
		addModel("currentLocale", loc);
	}
	
	private String getLanguageFromLocation(){
		String language = null;
		try {
			String country = ClickUtils.getCookieValue(req, countryCookieName);
			if(country != null){
				Locale loc = currutils.getLocaleForCountry(country.toUpperCase());
				if (loc != null) {
					language = loc.getLanguage();
				}
			}
		} catch(Exception exc){}
		
		return language;
    }
	
	public final ArrayList<ContactDetailType> getContactDetailTypeArray(){
        ArrayList<ContactDetailType> mt = new ArrayList<ContactDetailType>();

        for(ContactDetailType s : ContactDetailType.values()){
            if(s != com.erudika.scoold.core.ContactDetail.ContactDetailType.UNKNOWN)
				mt.add(s);
        }
        return mt;
    }

	public final boolean isAjaxRequest(){
		//context.isAjaxRequest()
		return getContext().isAjaxRequest();
	}
	
	public void processContactsRequest(ParaObject obj){
		Map params = req.getParameterMap();
		if(param("contacts")){
			try {
				PropertyUtils.setProperty(obj, "contacts", ContactDetail.getContactsFromParamsMap(params));
			} catch (Exception e) {}
		}
	}

	/* COMMENTS */

	public final void processNewCommentRequest(ParaObject parent) {
		if(param("deletecomment") && authenticated){
			String id = getParamValue("deletecomment");
			Comment c = dao.read(id);
			if(c != null && (c.getCreatorid().equals(authUser.getId()) || inRole("mod"))){
				// check parent and correct (for multi-parent-object pages)
				if(parent == null || !c.getParentid().equals(parent.getId())){
					parent = dao.read(c.getParentid());
				}
				c.delete();
				if(!inRole("mod")){
					addBadge(Badge.DISCIPLINED, true);
				}
				if(parent != null){
					try {
						Long count = (Long) PropertyUtils.getProperty(parent, "commentcount");
						dao.putColumn(id, DAO.OBJECTS, "commentcount", Long.toString(count - 1));
					} catch (Exception ex) {
						logger.log(Level.SEVERE, null, ex);
					}
				}
			}
		}else if(canComment && param("comment") && parent != null){
			String comment = getParamValue("comment");
			String parentid = parent.getId();
			if(StringUtils.isBlank(comment)) return;
			Comment lastComment = new Comment();
			lastComment.setComment(comment);
			lastComment.setParentid(parentid);
			lastComment.setCreatorid(authUser.getId());
			lastComment.setAuthor(authUser.getName());

			if(lastComment.create() != null){
				long commentCount = authUser.getComments();
				addBadgeOnce(Badge.COMMENTATOR, commentCount >= Constants.COMMENTATOR_IFHAS);
				authUser.setComments(commentCount + 1);
				authUser.update();
				commentslist.add(lastComment);
				addModel("newcomment", lastComment);
				
				try{
					Long count = (Long) PropertyUtils.getProperty(parent, "commentcount");
					dao.putColumn(parent.getId(), DAO.OBJECTS, "commentcount", Long.toString(count + 1));
				} catch (Exception ex) {
					logger.log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	/* * DRAWER * */

	public final void proccessDrawerRequest(ParaObject parent, String escapeUrl, boolean canEdit){
		if(!canEdit) return;

		if(param("remove") && param(DAO.CN_PARENTID)){
			String mid = getParamValue("remove");
			if(mid != null){
				Media m = new Media();
				m.setParentid(getParamValue(DAO.CN_PARENTID));
				m.setId(mid);
				m.delete();
			}
			if(!isAjaxRequest())
				setRedirect(escapeUrl);
		}else if("POST".equals(req.getMethod())){
			if (param("update-description")) {
				String id = getParamValue("mid");
				if(id != null){
					Media media = dao.read(id);
					if(media != null){
						media.setDescription(getParamValue("description"));
						media.update();
					}
				}
			} else {
				Media video = new Media();
				Utils.populate(video, req.getParameterMap());
				video.setCreatorid(authUser.getId());
				video.setParentid(parent.getId());
				video.create();

				addModel("lastMedia", video);
			}
		}else{
			medialist = Media.getAllMedia(parent.getId(), Media.MediaType.PHOTO, 
					pagenum, mediacount, true, MAX_ITEMS_PER_PAGE, search);
		}

	}

	public final void processImageEmbedRequest(ParaObject parent, String escapeUrl,
			boolean canEdit){
		if(!canEdit) return;

		Media image = new Media();
		Utils.populate(image, req.getParameterMap());
		image.setCreatorid(authUser.getId());
		image.setParentid(parent.getId());
		image.setType(Media.MediaType.PHOTO.name());
		if(image.getUrl() != null){
			String ext = image.getUrl().toLowerCase().
					substring(image.getUrl().lastIndexOf(".") + 1);
			ext = StringUtils.trim(ext);

			if(StringUtils.endsWithAny(ext, "jpg", "png", "gif", "jpeg")){
				image.create();
				addModel("lastMedia", image);
				addModel("mediaBaseUrl", escapeUrl);
			}
		}
	}

	/* * PHOTOS * */

	public final void processGalleryRequest(ParaObject parent, String escapeUrl,
			boolean canEdit){
		if(param("comment")){
			Media media = dao.read(getParamValue(DAO.CN_PARENTID));
			if(media != null) processNewCommentRequest(media);
		} else if(param("remove")){
			if(!canEdit || !authenticated || !req.getMethod().equals("POST")) return;
				
			String mid = getParamValue("remove");
			Media m = new Media();
//			m.setParentid(getParamValue(AWSDynamoDAO.CN_PARENTID));
			m.setParentid(parent.getId());
			m.setId(mid);
			m.delete();

			if(authUser.getId().equals(m.getParentid())){
				long pc = authUser.getPhotos();
				authUser.setPhotos(pc - 1);
				authUser.update();
			}

			if(!isAjaxRequest()) 
				setRedirect(escapeUrl);
		}else{
			String parentid = parent.getId();
			String label = null;
			if(param("label")) label = getParamValue("label");
			
			Long index = NumberUtils.toLong(getParamValue("mid"), 0);

			if (index.longValue() != 0L) {
				// single photo view - show one photo
				if(isAjaxRequest() && param("pageparam") && param(DAO.CN_PARENTID)){
					// the only thing that uses pagination here is comments
					String cparentid = getParamValue(DAO.CN_PARENTID);
					commentslist = new Comment(cparentid).getChildren(Comment.class, 
							pagenum, itemcount, null, MAX_ITEMS_PER_PAGE);
				}else{
					// depending on the navigation request we decide what photos to get
					// starting with ALL - get 3 photos - current, prev, next
					// then moving forward to NEXT(+1) - get only current and next
					// else if moving backwards PREV(-1) - get only current and prev
					// i.e. -1 get prev, +1 get next, else get all three
					medialist = Media.readPhotosAndCommentsForID(parentid, label, index, mediacount, search);
					if(!medialist.isEmpty() && !isAjaxRequest() ){
	//					&& param("pageparam") && param(AWSDynamoDAO.CN_PARENTID)
						Media current = medialist.get(0);
						// this is used when js is off or it is a pagination request
						commentslist = current.getComments(pagenum);
						itemcount.setValue(current.getCommentcount());
					}else if(!isAjaxRequest()){ 
						setRedirect(escapeUrl);
					}
				}
			} else {
				// gallery view - show a page of thumbnails
				medialist = Media.getAllMedia(parentid, Media.MediaType.PHOTO, 
						pagenum, itemcount, true, MAX_ITEMS_PER_PAGE, search);
			}
		}
	}
	
	/****  POSTS  ****/

//	public final void processPostRequest(Post showPost, String escapelink, 
//			boolean canEdit, boolean isMine){
//
//	}

	public final void processPostEditRequest(Post post, String escapelink, boolean canEdit){
		if(!canEdit || post == null) return;

		boolean isMine = (authenticated) ? authUser.getId().equals(post.getCreatorid()) : false;
		
		if(param("answer")){
			// add new answer
			if(!post.isClosed() && !post.isReply() && !post.isBlackboard() &&
					post.getAnswercount() < Constants.MAX_REPLIES_PER_POST){
				//create new answer
				Reply newq = new Reply();
				newq.setCreatorid(authUser.getId());
				newq.setParentid(post.getId());
				newq.setBody(getParamValue("body"));
				newq.create();

				post.setAnswercount(post.getAnswercount() + 1);
				if(post.getAnswercount() >= Constants.MAX_REPLIES_PER_POST){
					post.setCloserid("0");
				}
				post.updateLastActivity();
				post.update();

				addBadge(Badge.EUREKA, newq.getCreatorid().equals(post.getCreatorid()));
//				if(!isAjaxRequest()) setRedirect(escapelink+"#post-"+newq.getId());
			}
		}else if(param("approve")){
			String ansid = getParamValue("answerid");
			if(canEdit && ansid != null && isMine){
				Reply answer = (Reply) dao.read(ansid);

				if(answer != null && answer.isReply()){
					User author = dao.read(answer.getCreatorid());
					if(author != null && authenticated){
						boolean same = author.equals(authUser);
						
						if(ansid.equals(post.getAnswerid())){
							// Answer approved award - UNDO
							post.setAnswerid(null);
							if (!same) {
								author.removeRep(Constants.ANSWER_APPROVE_REWARD_AUTHOR);
								authUser.removeRep(Constants.ANSWER_APPROVE_REWARD_VOTER);
							}
						}else{
							// Answer approved award - GIVE
							post.setAnswerid(ansid);
							if(!same){
								author.addRep(Constants.ANSWER_APPROVE_REWARD_AUTHOR);
								authUser.addRep(Constants.ANSWER_APPROVE_REWARD_VOTER);
								addBadgeOnce(Badge.NOOB, true);
							}
						}

						author.update();
						authUser.update();
						post.update();
					}
				}
			}
		}else if(param("editpostid") || param("title")){
			// edit post
			Post beforeUpdate = null;
			try {
				beforeUpdate = (Post) BeanUtils.cloneBean(post);
				//			beforeUpdate.setType(post.getType());
				//			beforeUpdate.setTitle(post.getTitle());
				//			beforeUpdate.setBody(post.getBody());
				//
				//			}
				//				beforeUpdate.setTags(post.getTags());
				//			}
			} catch (Exception ex) {
				logger.log(Level.SEVERE, null, ex);
			}

			//update post
			String title = getParamValue("title");
			if(!StringUtils.isBlank(title) && title.length() > 10){
				post.setTitle(title);
			}
			if(param("body")){
				post.setBody(getParamValue("body"));
			}
			if (param("tags") && post.isQuestion()) {
				post.setTags(getParamValue("tags"));
			}

			post.setLasteditby(authUser.getId());
			//note: update only happens if something has changed
			if(!post.equals(beforeUpdate)){
				post.update();
				addBadgeOnce(Badge.EDITOR, true);
			}

//			if(!isAjaxRequest()) setRedirect(escapelink);
			
		}else if(param("close")){
			if(inRole("mod")){
				if (post.isClosed()) {
					post.setCloserid(null);
				} else {
					post.setCloserid(authUser.getId());
				}
				post.update();
			}
		}else if(param("restore")){
			String revid = getParamValue("revisionid");
			if(canEdit && revid != null){
				addBadge(Badge.BACKINTIME, true);
				post.restoreRevisionAndUpdate(revid);
			}
		}else if(param("delete")){
			if (!post.isReply()) {
				if((isMine || inRole("mod")) && !post.getDeleteme()){
					post.setDeleteme(true);

					Report rep = new Report();
					rep.setParentid(post.getId());
					rep.setLink(escapelink);
					rep.setDescription(lang.get("posts.marked"));
					rep.setType(ReportType.OTHER);
					rep.setAuthor(authUser.getName());
					rep.setCreatorid(authUser.getId());

					String rid = rep.create();
					post.setDeletereportid(rid);
					post.update();
				}
			} else if(post.isReply()) {
				if(isMine || inRole("mod")){
					Post parent = dao.read(post.getParentid());
					parent.setAnswercount(parent.getAnswercount() - 1);
					parent.update();
					post.delete();
				}
			}
		}else if(param("undelete")){
			if (!post.isReply()) {
				if((isMine || inRole("mod")) && post.getDeleteme()){
					post.setDeleteme(false);
					Report rep = dao.read(post.getDeletereportid());
					if(rep != null) rep.delete();
					post.setDeletereportid(null);
					post.update();
				}
			}
		}

		if(escapelink != null && !isAjaxRequest()){
			setRedirect(escapelink);
		}
//		else{
//			showPost.getComments(new MutableLong(1));
//		}
	}
	
	public final <T extends PObject> Form getQuestionForm(Class<T> clazz, String parentID, 
			Map<String, T> askablesMap){
		
		if(!authenticated || askablesMap == null || askablesMap.isEmpty())
			return null;
		
		Class<?> type = com.erudika.scoold.core.Group.class.equals(clazz) ? 
				Grouppost.class : Question.class;
		Form qForm = getPostForm(type, "qForm", "ask-question-form");
		Field pid = null;
		addModel("postParentClass", clazz.getSimpleName().toLowerCase());
		
//		Askable selected = askablesMap.get(parentID);
		if(StringUtils.isBlank(parentID)){
			pid = new Select(DAO.CN_PARENTID, lang.get("posts.selectschool"), true);
			((Select) pid).add(new Option("", lang.get("chooseone")));
			for (ParaObject askable : askablesMap.values()) {
				((Select) pid).add(new Option(askable.getId(), askable.getName()));
			}
		} else {
			addModel("postParentId", parentID);
			pid = new HiddenField(DAO.CN_PARENTID, parentID);
		}
		
		qForm.add(pid);

		return qForm;
	}

	public final Form getAnswerForm(){
		Form aForm = new Form("aForm");
		aForm.setId("answer-question-form");

		TextArea body = new TextArea("body", lang.get("posts.answer"), true);
		body.setMinLength(15);
		body.setRows(4);
		body.setCols(5);

		TextField hideme = new TextField("additional", false);
		hideme.setLabel("Leave blank!");
		hideme.setAttribute("class", "hide");

		HiddenField timer = new HiddenField("timer", "");
		timer.setValue(""+System.currentTimeMillis());
		timer.setRequired(true);

        Submit submit = new Submit("answerbtn", lang.get("post"), this, "onAnswerClick");
        submit.setAttribute("class", "button rounded3");
		submit.setId("answer-btn");

		aForm.add(hideme);
        aForm.add(body);
		aForm.add(timer);
        aForm.add(submit);

		return aForm;
	}

	public final Form getFeedbackForm(){
		return getPostForm(Feedback.class, "fForm", "write-feedback-form");
	}

	public final Form getPostForm(Class<?> type, String name, String id){
		if(!authenticated) return null;

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
		body.setMaxLength(Constants.MAX_TEXT_LENGTH);
		body.setRows(4);
		body.setCols(5);
		body.setTabIndex(2);

		Field tags = null;
		if (type.equals(Question.class) || type.equals(Grouppost.class)) {
			tags = new TextField("tags", true);
			tags.setLabel(lang.get("tags.title"));
			((TextField) tags).setMaxLength(255);
			tags.setTabIndex(3);
		} else if(type.equals(Feedback.class)) {
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

		HiddenField timer = new HiddenField("timer", "");
		timer.setValue(""+System.currentTimeMillis());
		timer.setRequired(true);

        Submit submit = new Submit("askbtn", lang.get("post"), this, "onAskClick");
        submit.setAttribute("class", "button rounded3");
		submit.setId("ask-btn");

        qForm.add(title);
		qForm.add(hideme);
        qForm.add(body);
		qForm.add(tags);
		qForm.add(timer);
        qForm.add(submit);

		return qForm;
	}

	public final Form getPostEditForm(Post post){
		if(post == null) return null;
		Form form = new Form("editPostForm"+post.getId());
		form.setId("post-edit-form-"+post.getId());

		TextArea  body = new TextArea("body", true);
		if (post.isReply()) {
			body.setLabel(lang.get("posts.answer"));
		} else {
			body.setLabel(lang.get("posts.question"));
		}
		body.setMinLength(15);
		body.setMaxLength(Constants.MAX_TEXT_LENGTH);
		body.setRows(4);
		body.setCols(5);
		body.setValue(post.getBody());

		if(post.isQuestion()){
			TextField tags = new TextField("tags", false);
			tags.setLabel(lang.get("tags.tags"));
			tags.setMaxLength(255);
			tags.setValue(post.getTagsString());
			form.add(tags);
		}

		TextField hideme = new TextField("additional", false);
		hideme.setLabel("Leave blank!");
		hideme.setAttribute("class", "hide");

		HiddenField timer = new HiddenField("timer", "");
		timer.setValue("" + System.currentTimeMillis());
		timer.setRequired(true);

        Submit submit = new Submit("editbtn",
				lang.get("save"), this, "onPostEditClick");
        submit.setAttribute("class", "button rounded3 post-edit-btn");
		submit.setId("post-edit-btn-"+post.getId());

		form.add(hideme);
        form.add(body);
		form.add(timer);
        form.add(submit);
		
		return form;
	}

	public final boolean isValidPostEditForm(Form qForm){
		int timer = 10*1000; // 10 sec

        String tags = qForm.getFieldValue("tags");
		String additional = qForm.getFieldValue("additional");
		String time = qForm.getFieldValue("timer");

		if (!StringUtils.isBlank(additional)){
			qForm.setError("You are not supposed to do that!");
		}
		if (System.currentTimeMillis() - Long.parseLong(time) < timer){
			qForm.setError("You are too quick!");
		}
		if(tags != null && StringUtils.split(tags, ",").length >
				Constants.MAX_TAGS_PER_POST){
			qForm.setError(lang.get("tags.toomany"));
		}

		return qForm.isValid();
	}

	public final boolean isValidQuestionForm(Form qForm){

		int timer = 10*1000; // 10 sec

        String tags = qForm.getFieldValue("tags");
		String additional = qForm.getFieldValue("additional");
		String time = qForm.getFieldValue("timer");

		if (!StringUtils.isBlank(additional)){
			qForm.setError("You are not supposed to do that!");
		}
		if (System.currentTimeMillis() - Long.parseLong(time) < timer){
			qForm.setError("You are too quick!");
		}
		if(StringUtils.split(tags, ",").length > Constants.MAX_TAGS_PER_POST){
			qForm.setError(lang.get("tags.toomany"));
		}

		return qForm.isValid();
	}

	public final boolean isValidAnswerForm(Form aForm, Post showPost){
		int timer = 5*1000; // 5 sec

//        String body = aForm.getFieldValue("body");
//        String tags = aForm.getFieldValue("tags");
		String additional = aForm.getFieldValue("additional");
		String time = aForm.getFieldValue("timer");

		if (!StringUtils.isBlank(additional)){
			aForm.setError("You are not supposed to do that!");
		}
		if (System.currentTimeMillis() - Long.parseLong(time) < timer){
			aForm.setError("You are too quick!");
		}
		
        return aForm.isValid();
	}

	public final void createAndGoToPost(Class<? extends Post> type){
		String parentid = getParamValue(DAO.CN_PARENTID);
		
		Post newq = null;
		try {
			newq = type.newInstance();
			newq.setTitle(getParamValue("title"));
			newq.setBody(getParamValue("body"));
			newq.setTags(getParamValue("tags"));
			if(parentid != null) newq.setParentid(parentid);
			newq.setCreatorid(authUser.getId());
			newq.create();
		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		setRedirect(getPostLink(newq, false, false));
	}

	/******** VOTING ********/

	public void processVoteRequest(String classname, String id){
		if(id == null) return;
		Votable votable = dao.read(id);
		boolean result = false;
		Integer votes = 0;
		
		if(votable != null && authenticated){
			try {
				User author = dao.read(votable.getCreatorid());
				votes = (Integer) PropertyUtils.getProperty(votable, "votes");

				if(param("voteup")){
					result = votable.voteUp(authUser.getId());
					if(!result) return;
					votes++;
					authUser.incrementUpvotes();

					int award = 0;

					if(votable instanceof Post){
						Post p = (Post) votable;
						if(p.isReply()){
							addBadge(Badge.GOODANSWER, votes >= Constants.GOODANSWER_IFHAS);
							award = Constants.ANSWER_VOTEUP_REWARD_AUTHOR;
						}else if(p.isQuestion()){
							addBadge(Badge.GOODQUESTION, votes >= Constants.GOODQUESTION_IFHAS);
							award = Constants.QUESTION_VOTEUP_REWARD_AUTHOR;
						}else{
							award = Constants.VOTEUP_REWARD_AUTHOR;
						}
					}else{
						award = Constants.VOTEUP_REWARD_AUTHOR;
					}

					if(author != null){
						author.addRep(award);
						author.update();
					}

				}else if(param("votedown")){
					result = votable.voteDown(authUser.getId());
					if(!result) return;
					votes--;
					authUser.incrementDownvotes();

					if(StringUtils.equalsIgnoreCase(classname,
							Comment.class.getSimpleName()) && votes <= -5){
						//treat comment as offensive or spam - hide
						((Comment) votable).setHidden(true);
					}else if(StringUtils.equalsIgnoreCase(classname,
							Media.class.getSimpleName()) &&	votes <= -10){
						//treat media as offensive - delete
						((Media) votable).delete();
						result = false;
					}else if(StringUtils.equalsIgnoreCase(classname,
							Post.class.getSimpleName()) && votes <= -5){
						Post p = (Post) votable;

						//mark post for closing
						Report rep = new Report();
						rep.setParentid(id);
						rep.setLink(getPostLink(p, false, false));
						rep.setDescription(lang.get("posts.forclosing"));
						rep.setType(ReportType.OTHER);
						rep.setAuthor("System");

						rep.create();
					}

					if(author != null){
						author.removeRep(Constants.POST_VOTEDOWN_PENALTY_AUTHOR);
						author.update();
						//small penalty to voter
						authUser.removeRep(Constants.POST_VOTEDOWN_PENALTY_VOTER);
					}
				}
			} catch (Exception ex) {
				logger.severe(ex.toString());
			}
		
			addBadgeOnce(Badge.SUPPORTER, authUser.getUpvotes() >= Constants.SUPPORTER_IFHAS);
			addBadgeOnce(Badge.CRITIC, authUser.getDownvotes() >= Constants.CRITIC_IFHAS);
			addBadgeOnce(Badge.VOTER, authUser.getTotalVotes() >= Constants.VOTER_IFHAS);
			
			if(result){
	//			votable.update();
				dao.putColumn(votable.getId(), DAO.OBJECTS, "votes", votes.toString());
			}
		}
		
		addModel("voteresult", result);
	}

	/******  MISC *******/
	
	public final boolean param(String param){
		return getContext().getRequestParameter(param) != null;
	}

	public final String getParamValue(String param){
		return getContext().getRequestParameter(param);
	}

	public String getPostLink(Post p, boolean plural, boolean noid){
		return p.getPostLink(plural, noid, questionslink, questionlink, 
				feedbacklink, grouplink, grouppostlink, classeslink, classlink);
	}
	
	public final int getIndexInBounds(int index, int count){
		if(index >= count) index = count - 1;
		if(index < 0) index = 0;
		return index;
	}
	
	public final void setStateParam(String name, String value){
		Utils.setStateParam(name, value, req, resp, 
				USE_SESSIONS);
	}
	
	public final String getStateParam(String name){
		return Utils.getStateParam(name, req, resp);
	}

	public final void removeStateParam(String name){
		Utils.removeStateParam(name, req, resp);
	}
	
	public final void clearSession(){
		AuthModule.clearSession(req, resp);
	}
		
	public boolean inRole(String role){
		return req.isUserInRole(role);
	}

	public final boolean addBadgeOnce(Badge b, boolean condition){
		return addBadge(b, condition && !authUser.hasBadge(b));
	}

	public final boolean addBadge(Badge b, boolean condition){
		return addBadge(b, null, condition);
	}

	public final boolean addBadge(Badge b, User u, boolean condition){
		if(u == null) u = authUser;
		if(!authenticated || !condition) return false;
		
		String newb = StringUtils.isBlank(u.getNewbadges()) ? "" : u.getNewbadges().concat(",");
		newb = newb.concat(b.toString());
		
		u.addBadge(b);		
		u.setNewbadges(newb);
		
		if(!authUser.getId().equals(u.getId())){
			u.update();
		}
		
		return true;
	}

	public final boolean removeBadge(Badge b, User u, boolean condition){
		if(u == null) u = authUser;
		if(!authenticated || !condition) return false;
		
		if(StringUtils.contains(u.getNewbadges(), b.toString())){
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
	
	public String getTemplate() {
		return "basetemplate.htm";
	}

	public void onDestroy(){
		if(authenticated && !isAjaxRequest()){
			long oneYear = authUser.getTimestamp() + ONE_YEAR;

			addBadgeOnce(Badge.ENTHUSIAST, authUser.getReputation() >= Constants.ENTHUSIAST_IFHAS);
			addBadgeOnce(Badge.FRESHMAN, authUser.getReputation() >= Constants.FRESHMAN_IFHAS);
			addBadgeOnce(Badge.SCHOLAR, authUser.getReputation() >= Constants.SCHOLAR_IFHAS);
			addBadgeOnce(Badge.TEACHER, authUser.getReputation() >= Constants.TEACHER_IFHAS);
			addBadgeOnce(Badge.PROFESSOR, authUser.getReputation() >= Constants.PROFESSOR_IFHAS);
			addBadgeOnce(Badge.GEEK, authUser.getReputation() >= Constants.GEEK_IFHAS);
			addBadgeOnce(Badge.SENIOR, System.currentTimeMillis() >= oneYear);
			addBadgeOnce(Badge.PHOTOLOVER, authUser.getPhotos() >= Constants.PHOTOLOVER_IFHAS);
			
			if(!StringUtils.isBlank(authUser.getNewbadges())){
				if(authUser.getNewbadges().equals("none")){
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
}
