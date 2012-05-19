package com.scoold.pages;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import com.scoold.core.CanHasMedia;
import com.scoold.core.Askable;
import com.scoold.core.Comment;
import com.scoold.core.Commentable;
import com.scoold.core.ContactDetail.ContactDetailType;
import com.scoold.core.Language;
import com.scoold.core.Media;
import com.scoold.core.Media.MediaType;
import com.scoold.core.Post;
import com.scoold.core.Post.FeedbackType;
import com.scoold.core.Post.PostType;
import com.scoold.core.Report;
import com.scoold.core.Report.ReportType;
import com.scoold.core.User;
import com.scoold.core.User.Badge;
import com.scoold.core.Votable;
import com.scoold.db.AbstractDAOUtils;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.util.ScooldAuthModule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
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
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.mutable.MutableLong;
import org.apache.commons.lang.StringUtils;
/**
 *
 * @author alexb 
 */
public class BasePage extends Page {

	private static final long serialVersionUID = 1L;

	public static final String APPNAME = "Scoold"; //app name
	public static final String CDN_URL = "http://static.scoold.com"; 
	public static final boolean IN_BETA = true;
	public static final boolean USE_SESSIONS = ScooldAuthModule.USE_SESSIONS;
	public static final boolean IN_PRODUCTION = AbstractDAOFactory.IN_PRODUCTION;
	public static final boolean IN_DEVELOPMENT = AbstractDAOFactory.IN_DEVELOPMENT;
	public static final int MAX_ITEMS_PER_PAGE = AbstractDAOFactory.MAX_ITEMS_PER_PAGE;
	public static final int SESSION_TIMEOUT_SEC = AbstractDAOFactory.SESSION_TIMEOUT_SEC;
	public static final long ONE_YEAR = 365L*24L*60L*60L*1000L;
	public static final String SEPARATOR = AbstractDAOFactory.SEPARATOR;
	public static final String AUTH_USER = ScooldAuthModule.AUTH_USER;
	
	public static final String FEED_KEY_SALT = ":scoold";
	public static final String FB_APP_ID = ScooldAuthModule.FB_APP_ID;
	public static final Logger logger = Logger.getLogger(BasePage.class.getName());

	public final String prefix = getContext().getServletContext().getContextPath()+"/";
	public final String localeCookieName = APPNAME + "-locale";
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
	public final String HOMEPAGE = prefix;
	
	public String infoStripMsg = "";
	public boolean authenticated;
	public boolean canComment;
	public HttpServletRequest req;
	public boolean isFBconnected;
	public boolean includeFBscripts;
	public User authUser;
	public AbstractDAOUtils daoutils;
	public ArrayList<Comment> commentslist;
	public ArrayList<Media> medialist;
	public ArrayList<String> labelslist;
	public ArrayList<String> badgelist;
	public MutableLong mediacount;
	public MutableLong pagenum;
	public MutableLong itemcount;
	public String showParam;
	public Map<String, String> lang = Language.getDefaultLanguage();
	
	public BasePage() {
		req = getContext().getRequest();
		initLanguage();
		checkAuth();
		cdnSwitch();
		includeFBscripts = false;
		daoutils = AbstractDAOFactory.getDefaultDAOFactory().getDAOUtils();
		itemcount = new MutableLong(0);
		mediacount = new MutableLong(0);
		showParam = getParamValue("show");
		pagenum = new MutableLong(NumberUtils.toLong(getParamValue("page"), 1));
		if(pagenum.longValue() <= 0L) pagenum = new MutableLong(1);
		canComment = authenticated && (authUser.hasBadge(Badge.ENTHUSIAST) || authUser.isModerator());
		commentslist = new ArrayList<Comment> ();
		badgelist = new ArrayList<String> (); 
		if(authenticated && !StringUtils.isBlank(authUser.getNewbadges())){
			badgelist.addAll(Arrays.asList(authUser.getNewbadges().split(",")));
			authUser.setNewbadges("none");
		}
		addModel("isAjaxRequest", isAjaxRequest());
		addModel("reportTypes", ReportType.values());
		addModel("systemMessage", StringUtils.trimToEmpty(daoutils.
				getSystemColumn(AbstractDAOFactory.SYSTEM_MESSAGE_KEY)));
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
		isFBconnected = false;
		if (req.getRemoteUser() != null) {
			authenticated = true;
			String uid = req.getRemoteUser();
			if (authUser == null) {
				authUser = User.getUser(NumberUtils.toLong(uid, 0));
				long delta = System.currentTimeMillis() - authUser.getLastseen();
				if(delta > 2 * 60 * 60 * 1000){
					// last seen 2 hours ago -> update
					authUser.setLastseen(System.currentTimeMillis());
					authUser.update();
				}
			}
			isFBconnected = authUser != null && !authUser.getIdentifier().startsWith("http");
			AbstractDAOUtils.removeStateParam("intro", req, getContext().getResponse(), false);
			infoStripMsg = "";
		} else {
			authenticated = false;
			String intro = AbstractDAOUtils.getStateParam("intro", 
					req, getContext().getResponse(), false);
			infoStripMsg = "intro";
			if ("0".equals(intro)) {
				infoStripMsg = "";
			}else{
				AbstractDAOUtils.setStateParam("intro", "1", req, getContext().getResponse(), false);
			}
		} 
	}

	private void initLanguage() {
		String cookieLoc = ClickUtils.getCookieValue(req, localeCookieName);
		Locale loc = Language.getProperLocale(req.getLocale().getLanguage());
		String langname = (cookieLoc != null) ? cookieLoc : loc.getLanguage();
		//locale cookie set?
		setCurrentLocale(langname, false);
	}

	/* * PUBLIC METHODS * */
	
	public final void setCurrentLocale(String langname, boolean setCookie) {
		Locale loc = Language.getProperLocale(langname);
		addModel("showCurrentLocale", loc.getDisplayLanguage(loc));
		getContext().setLocale(loc);
		lang = Language.readLanguage(loc);

		if(setCookie){
			//create a cookie
			int maxAge = 5 * 60 * 60 * 24 * 365;  //5 years
			ClickUtils.setCookie(req, getContext().getResponse(), localeCookieName, 
					loc.getLanguage(), maxAge, "/");
		}
		setFBLocale(langname);
		addModel("currentLocale", loc);
	}

	private void setFBLocale(String langname){
		// get fb lang js file in the selected locale
		String fb_locale = fb_locales.get(langname);
		if(StringUtils.isBlank(fb_locale)) fb_locale = "en";
		addModel("fb_locale", fb_locale);
	}
	
	public final ArrayList<ContactDetailType> getContactDetailTypeArray(){
        ArrayList<ContactDetailType> mt = new ArrayList<ContactDetailType>();

        for(ContactDetailType s : ContactDetailType.values()){
            if(s != com.scoold.core.ContactDetail.ContactDetailType.UNKNOWN)
				mt.add(s);
        }
        return mt;
    }

	public final boolean isAjaxRequest(){
		//context.isAjaxRequest()
		return getContext().isAjaxRequest();
	}

	/* COMMENTS */

	public final void processNewCommentRequest(Commentable parent){
		if(param("deletecomment") && authenticated){
			Long id = NumberUtils.toLong(getParamValue("deletecomment"));
			Comment c = Comment.getCommentDao().read(id);
			if(c != null && (c.getUserid().equals(authUser.getId()) || inRole("mod"))){
				// check parent and correct (for multi-parent-object pages)
				if(parent == null || !c.getParentid().equals(parent.getId())){
					parent = (Commentable) AbstractDAOUtils.getObject(c.getParentid(),
							c.getClassname());
				}
				c.delete();
				if(!inRole("mod")){
					addBadge(Badge.DISCIPLINED, true);
				}
				if(parent != null){
					parent.setCommentcount(parent.getCommentcount() - 1);
					parent.update();
				}
			}
		}else if(canComment && param("comment") && parent != null){
			String comment = getParamValue("comment");
			Long parentid = parent.getId();
			if(StringUtils.isBlank(comment)) return;
			Comment lastComment = new Comment();
			lastComment.setComment(comment);
			lastComment.setParentid(parentid);
			lastComment.setUserid(authUser.getId());
			lastComment.setAuthor(authUser.getFullname());
			lastComment.setClassname(parent.getClass().getSimpleName());

			if(lastComment.create() != null){
				long commentCount = authUser.getComments();
				addBadgeOnce(Badge.COMMENTATOR, commentCount >= User.COMMENTATOR_IFHAS);
				authUser.setComments(commentCount + 1);
				authUser.update();
				commentslist.add(lastComment);
				addModel("newcomment", lastComment);
				
				parent.setCommentcount(parent.getCommentcount() + 1);
				parent.update();
			}
		}
	}

	/* * DRAWER * */

	public final void proccessDrawerRequest(CanHasMedia parent, String escapeUrl, boolean canEdit){
		if(!canEdit) return;

		if(param("remove") && param("parentid")){
			Long mid = NumberUtils.toLong(getParamValue("remove"));
			Media m = new Media();
			m.setParentid(NumberUtils.toLong(getParamValue("parentid")));
			m.setId(mid);
			m.delete();
			if(!isAjaxRequest())
				setRedirect(escapeUrl);
		}else if("POST".equals(req.getMethod())){
			if (param("update-description")) {
				Long id = NumberUtils.toLong(getParamValue("mid"), 0);
				if(id.longValue() != 0){
					Media media = Media.getMediaDao().read(id);
					if(media != null){
						media.setDescription(getParamValue("description"));
						media.update();
					}
				}
			} else {
				Media video = new Media();
				AbstractDAOUtils.populate(video, req.getParameterMap());
				video.setUserid(authUser.getId());
				video.setParentid(parent.getId());
				video.create();

				addModel("lastMedia", video);
			}
		}else{
			medialist = parent.getMedia(MediaType.RICH, null,
						pagenum, itemcount, MAX_ITEMS_PER_PAGE, true);
		}

	}

	public final void processImageEmbedRequest(CanHasMedia parent, String escapeUrl,
			boolean canEdit){
		if(!canEdit) return;

		Media image = new Media();
		AbstractDAOUtils.populate(image, req.getParameterMap());
		image.setUserid(authUser.getId());
		image.setParentid(parent.getId());
		image.setType(Media.MediaType.PHOTO.name());
		if(image.getUrl() != null){
			String ext = image.getUrl().toLowerCase().
					substring(image.getUrl().lastIndexOf(".") + 1);
			ext = StringUtils.trim(ext);

			if(AbstractDAOUtils.endsWithAny(ext, new String[]{"jpg", "png", "gif", "jpeg"})){
				image.create();
				addModel("lastMedia", image);
				addModel("mediaBaseUrl", escapeUrl);
			}
		}
	}

	/* * PHOTOS * */

	public final void processGalleryRequest(CanHasMedia parent, String escapeUrl,
			boolean canEdit){
		if(param("addlabel") || param("removelabel")){
			if(!canEdit || !req.getMethod().equals("POST")) return ;
			
			String label = getParamValue("addlabel");
			Long mid = NumberUtils.toLong(getParamValue("mid"), 0);
			if(mid.longValue() == 0L) return;
			else if(label == null) label = getParamValue("removelabel");

			boolean addLabelSuccess = false;
			Media currentMedia = Media.getMediaDao().read(mid);

			if(!StringUtils.isBlank(label) && currentMedia != null){
				currentMedia.setOldlabels(currentMedia.getLabels());
				
				if(param("addlabel")){
					// add multiple labels
					if(label.contains(",")){
						currentMedia.addLabels(label.split(","));
						addLabelSuccess = true;
					}else{
						addLabelSuccess = currentMedia.addLabel(label);
					}
				}else{
					currentMedia.removeLabel(label);
					addLabelSuccess = true;
				}
				if(currentMedia.getLabelsSet().size() >	
						AbstractDAOFactory.MAX_LABELS_PER_MEDIA){
					addLabelSuccess = false;
				}

				if(addLabelSuccess) currentMedia.update();
			}
			addModel("labeladded", addLabelSuccess);
		} else if(param("comment")){
			Media media = Media.getMediaDao().read(NumberUtils.toLong(getParamValue("parentid")));
			if(media != null) processNewCommentRequest(media);
		} else if(param("remove")){
			if(!canEdit || !authenticated || !req.getMethod().equals("POST")) return;
				
			Long mid = NumberUtils.toLong(getParamValue("remove"));
			Media m = new Media();
//			m.setParentid(getParamValue("parentid"));
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
			Long parentid = parent.getId();
			String label = null;
			if(param("label")) label = getParamValue("label");
			
			Long index = NumberUtils.toLong(getParamValue("mid"), 0);

			if (index.longValue() != 0L) {
				// single photo view - show one photo
				if(isAjaxRequest() && param("pageparam") && param("parentid")){
					// the only thing that uses pagination here is comments
					Long cparentid = NumberUtils.toLong(getParamValue("parentid"));
					commentslist = Comment.getCommentDao().
								readAllCommentsForID(cparentid, pagenum, itemcount);
				}else{
					// depending on the navigation request we decide what photos to get
					// starting with ALL - get 3 photos - current, prev, next
					// then moving forward to NEXT(+1) - get only current and next
					// else if moving backwards PREV(-1) - get only current and prev
					// i.e. -1 get prev, +1 get next, else get all three
					medialist = Media.getMediaDao().readPhotosAndCommentsForID(parentid,
						label, index, mediacount);
					if(!medialist.isEmpty() && !isAjaxRequest() ){
	//					&& param("pageparam") && param("parentid")
						Media current = medialist.get(0);
						// this is used when js is off or it is a pagination request
						commentslist = current.getComments(pagenum);
						itemcount.setValue(current.getCommentcount());
						labelslist = Media.getMediaDao().readAllLabelsForID(parentid);
					}else if(!isAjaxRequest()){ 
						setRedirect(escapeUrl);
					}
				}
			} else {
				// gallery view - show a page of thumbnails
				medialist = parent.getMedia(Media.MediaType.PHOTO, label,
						pagenum, mediacount, MAX_ITEMS_PER_PAGE, true);
				labelslist = Media.getMediaDao().readAllLabelsForID(parentid);
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

		boolean isMine = (authenticated) ? authUser.getId().equals(post.getUserid()) : false;
		
		if(param("answer")){
			// add new answer
			if(!post.isClosed() && !post.isReply() && !post.isBlackboard() &&
					post.getAnswercount() < AbstractDAOFactory.MAX_REPLIES_PER_POST){
				//create new answer
				Post newq = new Post(PostType.REPLY);
				newq.setUserid(authUser.getId());
				newq.setParentid(post.getId());
				newq.setBody(getParamValue("body"));
				newq.create();

				post.setAnswercount(post.getAnswercount() + 1);
				if(post.getAnswercount() >= AbstractDAOFactory.MAX_REPLIES_PER_POST){
					post.setCloserid(0L);
				}
				post.updateLastActivity();
				post.update();

				addBadge(Badge.EUREKA, newq.getUserid().equals(post.getUserid()));
//				if(!isAjaxRequest()) setRedirect(escapelink+"#post-"+newq.getId());
			}
		}else if(param("approve")){
			Long ansid = NumberUtils.toLong(getParamValue("answerid"), 0);
			if(canEdit && ansid > 0L && isMine){
				Post answer = Post.getPostDao().read(ansid);

				if(answer != null && answer.isReply()){
					User author = User.getUserDao().read(answer.getUserid());
					if(author != null && authenticated){
						boolean same = author.equals(authUser);
						
						if(ansid.equals(post.getAnswerid())){
							// Answer approved award - UNDO
							post.setAnswerid(null);
							if (!same) {
								author.removeRep(User.ANSWER_APPROVE_REWARD_AUTHOR);
								authUser.removeRep(User.ANSWER_APPROVE_REWARD_VOTER);
							}
						}else{
							// Answer approved award - GIVE
							post.setAnswerid(ansid);
							if(!same){
								author.addRep(User.ANSWER_APPROVE_REWARD_AUTHOR);
								authUser.addRep(User.ANSWER_APPROVE_REWARD_VOTER);
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
			Post beforeUpdate = new Post(post.getPostType());
			beforeUpdate.setTitle(post.getTitle());
			beforeUpdate.setBody(post.getBody());
			
			if (post.isQuestion()) {
				beforeUpdate.setTags(post.getTags());
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
			boolean done = post.updateAndCreateRevision(beforeUpdate);
			addBadgeOnce(Badge.EDITOR, done);

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
			Long revid = NumberUtils.toLong(getParamValue("revisionid"));
			if(canEdit && revid.longValue() != 0L){
				addBadge(Badge.BACKINTIME, true);
				post.restoreRevisionAndUpdate(revid);
			}
		}else if(param("delete")){
			if (daoutils.isIndexable(post) && !post.isReply()) {
				if((isMine || inRole("mod")) && !post.getDeleteme()){
					post.setDeleteme(true);

					Report rep = new Report();
					rep.setParentid(post.getId());
					rep.setClassname(Post.class.getSimpleName());
					rep.setLink(escapelink);
					rep.setDescription(lang.get("posts.marked"));
					rep.setType(ReportType.OTHER);
					rep.setAuthor(authUser.getFullname());
					rep.setUserid(authUser.getId());

					Long rid = rep.create();
					post.setDeletereportid(rid);
					post.update();
				}
			} else if(post.isReply()) {
				if(isMine || inRole("mod")){
					Post parent = Post.getPostDao().read(post.getParentid());
					parent.setAnswercount(parent.getAnswercount() - 1);
					parent.update();
					post.delete();
				}
			}
		}else if(param("undelete")){
			if (daoutils.isIndexable(post) && !post.isReply()) {
				if((isMine || inRole("mod")) && post.getDeleteme()){
					post.setDeleteme(false);
					Report rep = Report.getReportDAO().read(post.getDeletereportid());
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
	
	public final <T extends Askable> Form getQuestionForm(Class<T> clazz, Long selectedId, 
			Map<Long, T> askablesMap){
		
		if(!authenticated || askablesMap == null || askablesMap.isEmpty())
			return null;
		
		PostType type = com.scoold.core.Group.class.equals(clazz) ? 
				PostType.GROUPPOST : PostType.QUESTION;
		Form qForm = getPostForm(type, "qForm", "ask-question-form");
		Field pid = null;
		Long parentID = selectedId == null ? 0L : selectedId;
		addModel("postParentClass", clazz.getSimpleName().toLowerCase());
		
//		Askable selected = askablesMap.get(parentID);
		if(parentID.longValue() == 0L){
			pid = new Select("parentid", lang.get("posts.selectschool"), true);
			((Select) pid).add(new Option("", lang.get("chooseone")));
			for (Askable askable : askablesMap.values()) {
				((Select) pid).add(new Option(askable.getId(), askable.getName()));
			}
		} else {
			addModel("postParentId", parentID);
			pid = new HiddenField("parentid", parentID);
		}
		
		qForm.add(pid);

		return qForm;
	}

	public final Form getAnswerForm(PostType type){
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
		return getPostForm(PostType.FEEDBACK, "fForm", "write-feedback-form");
	}

	public final Form getPostForm(PostType type, String name, String id){
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
		body.setMaxLength(AbstractDAOFactory.MAX_TEXT_LENGTH);
		body.setRows(4);
		body.setCols(5);
		body.setTabIndex(2);

		Field tags = null;
		if (type == PostType.QUESTION || type == PostType.GROUPPOST) {
			tags = new TextField("tags", true);
			tags.setLabel(lang.get("tags.title"));
			((TextField) tags).setMaxLength(255);
			tags.setTabIndex(3);
		} else if(type == PostType.FEEDBACK) {
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
		body.setMaxLength(AbstractDAOFactory.MAX_TEXT_LENGTH);
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
				AbstractDAOFactory.MAX_TAGS_PER_POST){
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
		if(StringUtils.split(tags, ",").length > AbstractDAOFactory.MAX_TAGS_PER_POST){
			qForm.setError(lang.get("tags.toomany"));
		}

		return qForm.isValid();
	}

	public final boolean isValidAnswerForm(Form aForm, Post showPost){
		int timer = 5*1000; // 5 sec

        String body = aForm.getFieldValue("body");
        String tags = aForm.getFieldValue("tags");
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

	public final void createAndGoToPost(PostType type){
		Long parentid = NumberUtils.toLong(getParamValue("parentid"), 0L);
		
		Post newq = new Post(type);
		newq.setTitle(getParamValue("title"));
		newq.setBody(getParamValue("body"));
		newq.setTags(getParamValue("tags"));
		if(parentid.longValue() != 0) newq.setParentid(parentid);
		newq.setUserid(authUser.getId());
		newq.create();

		setRedirect(getPostLink(newq, false, false));
	}

	/******** VOTING ********/

	public void processVoteRequest(String classname, Long id){
		if(id == null || StringUtils.isBlank(classname)) return;
		Class<?> clazz = AbstractDAOUtils.getClassname(classname);
		Votable<Long> votable = null;
		Object o;
		try {
			o = clazz.newInstance();
			if (o instanceof Votable) {
				votable = (Votable<Long>) AbstractDAOUtils.getDaoInstance(classname).read(id);
			}
		} catch (Exception ex) {
			logger.severe(ex.toString());
		}
		
		boolean result = false;
		if(votable != null && authenticated && votable != null){
			User author = User.getUser(votable.getUserid());
			Integer votes = (votable.getVotes() == null) ? 0 : votable.getVotes();

			if(param("voteup")){
				result = votable.voteUp(authUser.getId());

				if(!result) return;

				authUser.incrementUpvotes();
				int award = 0;

				if(StringUtils.equalsIgnoreCase(classname, Post.class.getSimpleName())){
					Post p = (Post) votable;
					if(p.isReply()){
						addBadge(Badge.GOODANSWER, votes >= User.GOODANSWER_IFHAS);
						award = User.ANSWER_VOTEUP_REWARD_AUTHOR;
					}else if(p.isQuestion()){
						addBadge(Badge.GOODQUESTION, votes >= User.GOODQUESTION_IFHAS);
						award = User.QUESTION_VOTEUP_REWARD_AUTHOR;
					}else{
						award = User.VOTEUP_REWARD_AUTHOR;
					}
				}else{
					award = User.VOTEUP_REWARD_AUTHOR;
				}

				if(author != null){
					author.addRep(award);
					author.update();
				}

			}else if(param("votedown")){
				result = votable.voteDown(authUser.getId());
				if(!result) return;

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

					if(daoutils.isIndexable(p)){
						//mark post for closing
						Report rep = new Report();
						rep.setParentid(id);
						rep.setClassname(Post.class.getSimpleName());
						rep.setLink(getPostLink(p, false, false));
						rep.setDescription(lang.get("posts.forclosing"));
						rep.setType(ReportType.OTHER);
						rep.setAuthor("System");

						rep.create();
					}					
				}
				
				if(author != null){
					author.removeRep(User.POST_VOTEDOWN_PENALTY_AUTHOR);
					author.update();
					//small penalty to voter
					authUser.removeRep(User.POST_VOTEDOWN_PENALTY_VOTER);
				}
			}

			addBadgeOnce(Badge.SUPPORTER, authUser.getUpvotes() >= User.SUPPORTER_IFHAS);
			addBadgeOnce(Badge.CRITIC, authUser.getDownvotes() >= User.CRITIC_IFHAS);
			addBadgeOnce(Badge.VOTER, authUser.getTotalVotes() >= User.VOTER_IFHAS);
		}

		if(result) votable.update();
		
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
		if(p == null) return "";
		String ptitle = AbstractDAOUtils.spacesToDashes(p.getTitle());
		String pid = (noid ? "" : "/"+p.getId()+"/"+ ptitle);
		if (p.isQuestion()) {
			return plural ? questionslink : questionlink + pid;
		} else if(p.isFeedback()) {
			return plural ? feedbacklink : feedbacklink + pid;
		} else if(p.isGrouppost()){
			return plural ? grouplink+"/"+p.getParentid() : grouppostlink + pid;
		} else if(p.isReply()){
			Post parentp = Post.getPostDao().read(p.getParentid());
			if(parentp != null){
				return getPostLink(parentp, plural, noid);
			}
		}else if(p.isBlackboard()){
			return plural ? classeslink : classlink + (noid ? "" : "/" + p.getParentid());
		}
		return "";
	}
	
	public final int getIndexInBounds(int index, int count){
		if(index >= count) index = count - 1;
		if(index < 0) index = 0;
		return index;
	}
	
	public final void setStateParam(String name, String value){
		AbstractDAOUtils.setStateParam(name, value, req, getContext().getResponse(), 
				USE_SESSIONS);
	}
	
	public final String getStateParam(String name){
		return AbstractDAOUtils.getStateParam(name, req, getContext().getResponse(), 
				USE_SESSIONS);
	}

	public final void removeStateParam(String name){
		AbstractDAOUtils.removeStateParam(name, req, getContext().getResponse(), USE_SESSIONS);
	}
	
	public final void clearSession(){
		ScooldAuthModule.clearSession(req, getContext().getResponse(), USE_SESSIONS);
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

			addBadgeOnce(Badge.ENTHUSIAST, authUser.getReputation() >= User.ENTHUSIAST_IFHAS);
			addBadgeOnce(Badge.FRESHMAN, authUser.getReputation() >= User.FRESHMAN_IFHAS);
			addBadgeOnce(Badge.SCHOLAR, authUser.getReputation() >= User.SCHOLAR_IFHAS);
			addBadgeOnce(Badge.TEACHER, authUser.getReputation() >= User.TEACHER_IFHAS);
			addBadgeOnce(Badge.PROFESSOR, authUser.getReputation() >= User.PROFESSOR_IFHAS);
			addBadgeOnce(Badge.GEEK, authUser.getReputation() >= User.GEEK_IFHAS);
			addBadgeOnce(Badge.SENIOR, System.currentTimeMillis() >= oneYear);
			addBadgeOnce(Badge.PHOTOLOVER, authUser.getPhotos() >= User.PHOTOLOVER_IFHAS);
			
			if(!StringUtils.isBlank(authUser.getNewbadges())){
				if(authUser.getNewbadges().equals("none")){
					authUser.setNewbadges(null);
				}
				authUser.update();			
			}
		}
	}

	public static final Map<String, String> fb_locales = new HashMap<String, String>(){
		public static final long serialVersionUID = 1L;
		{
			put("ca", "ca_ES"); put("cs", "cs_CZ"); put("cy", "cy_GB"); 
			put("da", "da_DK"); put("de", "de_DE"); put("eu", "eu_ES"); 
			put("ck", "ck_US"); put("en", "en_US"); put("es", "es_ES");
			put("fi", "fi_FI"); put("fr", "fr_FR"); put("gl", "gl_ES"); 
			put("hu", "hu_HU"); put("it", "it_IT"); put("ja", "ja_JP"); 
			put("ko", "ko_KR"); put("nb", "nb_NO"); put("nn", "nn_NO");
			put("nl", "nl_NL"); put("pl", "pl_PL"); put("pt", "pt_PT"); 
			put("ro", "ro_RO"); put("ru", "ru_RU"); put("sk", "sk_SK"); 
			put("sl", "sl_SI"); put("sv", "sv_SE"); put("th", "th_TH");
			put("tr", "tr_TR"); put("ku", "ku_TR"); put("zh", "zh_CN"); 
			put("af", "af_ZA"); put("sq", "sq_AL"); put("hy", "hy_AM"); 
			put("az", "az_AZ"); put("be", "be_BY"); put("bn", "bn_IN");
			put("bs", "bs_BA"); put("bg", "bg_BG"); put("hr", "hr_HR"); 
			put("tl", "tl_ST"); put("eo", "eo_EO"); put("et", "et_EE"); 
			put("fo", "fo_FO"); put("ka", "ka_GE"); put("el", "el_GR");
			put("gu", "gu_IN"); put("hi", "hi_IN"); put("is", "is_IS"); 
			put("id", "id_ID"); put("ga", "ga_IE"); put("jv", "jv_ID"); 
			put("kn", "kn_IN"); put("kk", "kk_KZ"); put("la", "la_VA");
			put("lv", "lv_LV"); put("li", "li_NL"); put("lt", "lt_LT"); 
			put("mk", "mk_MK"); put("mg", "mg_MG"); put("ms", "ms_MY"); 
			put("mt", "mt_MT"); put("mr", "mr_IN"); put("mn", "mn_MN");
			put("ne", "ne_NP"); put("pa", "pa_IN"); put("rm", "rm_CH"); 
			put("sa", "sa_IN"); put("sr", "sr_RS"); put("so", "so_SO"); 
			put("sw", "sw_KE"); put("ps", "ps_AF"); put("ta", "ta_IN");
			put("tt", "tt_RU"); put("te", "te_IN"); put("ml", "ml_IN"); 
			put("uk", "uk_UA"); put("uz", "uz_UZ"); put("vi", "vi_VN"); 
			put("xh", "xh_ZA"); put("zu", "zu_ZA"); put("km", "km_KH");
			put("tg", "tg_TJ"); put("ar", "ar_AR"); put("he", "he_IL"); 
			put("ur", "ur_PK"); put("fa", "fa_IR"); put("sy", "sy_SY"); 
			put("yi", "yi_DE"); put("gn", "gn_PY"); put("qu", "qu_PE");
			put("ay", "ay_BO"); put("se", "se_NO");
		}
	};

	public static String DESCRIPTION = "Scoold is friendly place where students and teachers "
			+ "can help each other and talk about anything related to education.";
	
	public static String KEYWORDS = "scoold, knowledge sharing, collaboration, wiki, "
			+ "schools, students, classmates, alumni, teachers, contacts, social, "
			+ "network, classes, classroom, education";
}
