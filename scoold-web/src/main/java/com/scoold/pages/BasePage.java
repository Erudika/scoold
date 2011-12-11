package com.scoold.pages;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import com.scoold.core.CanHasMedia;
import com.scoold.core.Classunit;
import com.scoold.core.Comment;
import com.scoold.core.Commentable;
import com.scoold.core.ContactDetail.ContactDetailType;
import com.scoold.core.Language;
import com.scoold.core.Media;
import com.scoold.core.Media.MediaType;
import com.scoold.core.Post;
import com.scoold.core.Post.FeedbackType;
import com.scoold.core.Post.PostType;
import com.scoold.core.Search;
import com.scoold.core.School;
import com.scoold.core.Report;
import com.scoold.core.Report.ReportType;
import com.scoold.core.User;
import com.scoold.core.User.Badge;
import com.scoold.core.Votable;
import com.scoold.db.AbstractDAOUtils;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.util.ScooldAppListener;
import com.scoold.util.ScooldAuthModule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
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
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.mutable.MutableLong;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/**
 *
 * @author alexb 
 */
public class BasePage extends Page {

	private static final long serialVersionUID = 1L;

	public static final String APPNAME = "Scoold"; //app name
	public static final String CDN_URL = "http://static.scoold.com"; 
	public static final boolean IN_BETA = true;
	public static final boolean USE_SESSIONS = false;
	public static final int MAX_ITEMS_PER_PAGE = AbstractDAOFactory.MAX_ITEMS_PER_PAGE;
	public static final int MAX_IMG_SIZE_PX = 730;
	public static final int SESSION_TIMEOUT_SEC = 24 * 60 * 60;
	public static final String SEPARATOR = AbstractDAOFactory.SEPARATOR;
	public static final String AUTH_USER = ScooldAuthModule.AUTH_USER;
	public static boolean IN_PRODUCTION = false;
	
	public static final String FEED_KEY_SALT = ":scoold";
	public static final String FB_APP_ID = "99517177417";
	public static final String FB_API_KEY = "bc6c5faabc3b00982b97b2a5a9d4d13f";
	public static final String FLICKR_API_KEY = "834dc1c2493561354a02d6d739a970a4";
	public static final Logger logger = Logger.getLogger(BasePage.class.getName());

	public String prefix = getContext().getServletContext().getContextPath()+"/";
	public String minsuffix = "-min";
	public String imageslink = prefix + "images";
	public String scriptslink = prefix + "scripts";
	public String styleslink = prefix + "styles";
	public String messageslink = prefix + "messages";
	public String schoolslink = prefix + "schools";
	public String peoplelink = prefix + "people";
	public String classeslink = prefix + "classes";
	public String schoollink = prefix + "school";
	public String classlink = prefix + "class";
	public String profilelink = prefix + "profile";
	public String myprofilelink = profilelink;
	public String mycontactslink;
	public String myquestionslink;
	public String myanswerslink;
	public String myphotoslink;
	public String mydrawerlink;
	public String searchlink = prefix + "search";
	public String searchquestionslink = searchlink + "/questions";
	public String searchfeedbacklink = searchlink + "/feedback";
	public String searchpeoplelink = searchlink + "/people";
	public String searchclasseslink = searchlink + "/classes";
	public String searchschoolslink = searchlink + "/schools";
	public String signinlink = prefix + "signin";
	public String signoutlink = prefix + "signout";
	public String signuplink = prefix + "signup";
	public String aboutlink = prefix + "about";
	public String privacylink = prefix + "privacy";
	public String termslink = prefix + "terms";
	public String settingslink = prefix + "settings";
	public String translatelink = prefix + "translate";
	public String changepasslink = prefix + "changepass";
	public String activationlink = prefix + "activation";
	public String reportslink = prefix + "reports";
	public String adminlink = prefix + "admin";
	public String deletevideolink = prefix + "delete/video";
	public String votedownlink = prefix + "votedown";
	public String voteuplink = prefix + "voteup";
	public String questionlink = prefix + "question";
	public String questionslink = prefix + "questions";
	public String commentlink = prefix + "comment";
	public String medialink = prefix + "media";
	public String messagelink = prefix + "message";
	public String postlink = prefix + "post";
	public String feedbacklink = prefix + "feedback";
	public String languageslink = prefix + "languages";
	
	public String HOMEPAGE = prefix;
	public String pageMacroCode = "";
	public String infoStripMsg = "";
	public boolean authenticated;
	public boolean canComment;
	public HttpServletRequest req;
	public boolean isFBconnected;
	public boolean includeFBscripts;
	public User authUser;
	public AbstractDAOUtils daoutils;
	public Search search;
	public JSONObject mediaDataObject;
	public ArrayList<Comment> commentslist;
	public ArrayList<Media> medialist;
	public ArrayList<String> labelslist;
	public ArrayList<String> badgelist;
	public MutableLong mediacount;
	public MutableLong pagenum;
	public MutableLong itemcount;
	public String showParam;
	public Report publicReport = new Report();
	public Object showdownJS;
	public Map<String, String> lang = Language.getDefaultLanguage();

	public BasePage() {
		IN_PRODUCTION = BooleanUtils.toBoolean(System.getProperty("com.scoold.production"));
		search = new Search();
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
		showdownJS = getContext().getServletContext().
				getAttribute(ScooldAppListener.SHOWDOWN_CONV);
		canComment = authenticated && (authUser.hasBadge(Badge.ENTHUSIAST) || authUser.isModerator());
		commentslist = new ArrayList<Comment> ();
		badgelist = new ArrayList<String> (); 
		if(authenticated && !StringUtils.isBlank(authUser.getNewbadges())){
			badgelist.addAll(Arrays.asList(authUser.getNewbadges().split(",")));
			authUser.setNewbadges("none");
		}
		addModel("isAjaxRequest", isAjaxRequest());
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
		String cookieLoc = ClickUtils.getCookieValue(req, "locale");
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
			ClickUtils.setCookie(req, getContext().getResponse(), "locale", 
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
				if(parent == null || !StringUtils.equals(c.getParentuuid(), parent.getUuid())){
					parent = (Commentable) AbstractDAOUtils.getObject(c.getParentuuid(),
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
			String parentUUID = parent.getUuid();
			if(StringUtils.isBlank(comment)) return;
			Comment lastComment = new Comment();
			lastComment.setComment(comment);
			lastComment.setParentuuid(parentUUID);
			lastComment.setUserid(authUser.getId());
			lastComment.setAuthor(authUser.getFullname());
			lastComment.setUuid(UUID.randomUUID().toString());
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

		if(param("remove") && param("parentuuid")){
			Long mid = NumberUtils.toLong(getParamValue("remove"));
			Media m = new Media();
			m.setParentuuid(getParamValue("parentuuid"));
			m.setId(mid);
			m.delete();
			if(!isAjaxRequest())
				setRedirect(escapeUrl);
		}else if("POST".equals(req.getMethod())){
			if (param("update-description")) {
				Long id = NumberUtils.toLong(getParamValue("id"), 0);
				if(id.longValue() > 0){
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
				video.setParentuuid(parent.getUuid());
				video.create();

				addModel("lastMedia", video);
			}
		}else{
			pageMacroCode = "#drawerpage($medialist)";
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
		image.setParentuuid(parent.getUuid());
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
			Long mid = NumberUtils.toLong(getParamValue("id"), 0);
			if(mid.longValue() == 0L) return;
			else if(label == null) label = getParamValue("removelabel");

			boolean addLabelSuccess = false;

			Media currentMedia = Media.getMediaDao().read(mid);
			String oldLables = currentMedia.getLabels();
			currentMedia.setOldlabels(oldLables);

			if(!StringUtils.isBlank(label)){
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
		} else if(param("remove")){
			if(!canEdit || !authenticated || !req.getMethod().equals("POST")) return;
				
			Long mid = NumberUtils.toLong(getParamValue("remove"));
			Media m = new Media();
//			m.setParentuuid(getParamValue("parentuuid"));
			m.setParentuuid(parent.getUuid());
			m.setId(mid);
			m.delete();

			if(authUser.getUuid().equals(m.getParentuuid())){
				long pc = authUser.getPhotos();
				authUser.setPhotos(pc - 1);
				authUser.update();
			}

			if(!isAjaxRequest()) 
				setRedirect(escapeUrl);
		}else{
			String parentuuid = parent.getUuid();
			String label = null;
			if(param("label")) label = getParamValue("label");
			
			Long index = NumberUtils.toLong(getParamValue("mid"), 0);

			if (index.longValue() != 0L) {
				// single photo view - show one photo
				pageMacroCode = "#commentspage($commentslist)";

				if(isAjaxRequest() && param("pageparam") && param("parentuuid")){
					// the only thing that uses pagination here is comments
					String cparuuid = getParamValue("parentuuid");
					commentslist = Comment.getCommentDao().
								readAllCommentsForUUID(cparuuid, pagenum, itemcount);
				}else{
					// depending on the navigation request we decide what photos to get
					// starting with ALL - get 3 photos - current, prev, next
					// then moving forward to NEXT(+1) - get only current and next
					// else if moving backwards PREV(-1) - get only current and prev
					// i.e. -1 get prev, +1 get next, else get all three
					int nextPrevAll = NumberUtils.toInt(getParamValue("nextprevall"));
					medialist = Media.getMediaDao().readPhotosAndCommentsForUUID(parentuuid,
						label, index, nextPrevAll, mediacount);
					mediaDataObject = getMediaJSONObject(medialist);
					if(!medialist.isEmpty() && !isAjaxRequest() ){
	//					&& param("pageparam") && param("parentuuid")
						Media current = medialist.get(0);
						// this is used when js is off or it is a pagination request
						commentslist = current.getComments(pagenum);
						itemcount.setValue(current.getCommentcount());
					}else if(!isAjaxRequest()){ 
						setRedirect(escapeUrl);
						return ;
					}
				}
			} else {
				// gallery view - show a page of thumbnails
				pageMacroCode = "#thumbspage($medialist $showlabel)";
				medialist = parent.getMedia(Media.MediaType.PHOTO, label,
						pagenum, mediacount, MAX_ITEMS_PER_PAGE, true);
				labelslist = Media.getMediaDao().readAllLabelsForUUID(parentuuid);
				addModel("alllabels", StringUtils.join(labelslist, ","));
			}
			
			if(isAjaxRequest() && param("getimagedataobject")){
					//AJAX get image json data
				setHeader("Content-Type", "application/json; charset=utf-8");
			}

		}
		
	}

	private JSONObject getMediaJSONObject(ArrayList<Media> medialist){
		JSONObject jsonDataObject = new JSONObject();
		JSONArray thumbs = new JSONArray();
		Locale loc = getContext().getLocale();
		try {
			for (Media photo : medialist) {
				JSONArray comments = new JSONArray();
				//only get first page of comments
				for (Comment comment : photo.getComments()) {

					boolean canDelete = authenticated && (
							(comment.getUserid().equals(authUser.getId())) ||
							inRole("mod"));

					comments.put(new JSONObject()
						.put("id", comment.getId())
						.put("uuid", comment.getUuid())
						.put("userid", comment.getUserid())
						.put("author", comment.getAuthor())
						.put("votes", photo.getVotes())
						.put("candelete", canDelete)
						.put("comment", comment.getComment())
						.put("timestamp", AbstractDAOUtils.formatDate(
							comment.getTimestamp(),
							"dd MMMM yyyy HH:mm", loc)));
				}

				thumbs.put(new JSONObject()
					.put("id", photo.getId())
					.put("uuid", photo.getUuid())
					.put("url", photo.getUrl())
					.put("thumburl", photo.getThumburl())
					.put("userid", photo.getUserid())
					.put("title", photo.getTitle())
					.put("description", photo.getDescription())
					.put("timestamp", photo.getTimestamp())
					.put("votes", photo.getVotes())
					.put("commentcount", photo.getCommentcount())
					.put("comments", comments)
					.put("pagenum", photo.getCommentpage())
					.put("originalurl", photo.getOriginalurl())
					.put("labels", photo.getLabelsSet()));
			}
			
			jsonDataObject.put("media", thumbs);
		} catch (JSONException ex) {
			logger.severe(ex.toString());
		}

		return jsonDataObject;
	}

	/****  POSTS  ****/

	public final void processPostRequest(Post showPost, String escapelink, 
			boolean canEdit, boolean isMine){

		boolean updated = false;
		String redirectTo = null;

		if(showPost == null){
			redirectTo = escapelink;
		}else if(param("getcomments") && param("parentuuid")){
			pageMacroCode = "#commentspage($commentslist)";
			String parentuuid = getParamValue("parentuuid");
			commentslist = Comment.getCommentDao().readAllCommentsForUUID(parentuuid,
					pagenum, null);

			if(isAjaxRequest()) return;
		}else if ("revisions".equals(showParam)) {
			if(showPost.isAnswer()){
				addModel("backtoid", showPost.getParentpostid());
			}
		}else{
			if(showPost.isBlackboard() && (!authenticated || !authUser.isAdmin())){
				redirectTo = classeslink;
			}else if(showPost.isAnswer() && !isAjaxRequest() && !param("uuid")){
				Long parentid = showPost.getParentpostid();
				if(parentid == null){
					redirectTo = escapelink;
				}else{
					redirectTo = escapelink+"/"+parentid+"#post-"+showPost.getId();
				}
			}

			if(param("close")){
				if(inRole("mod")){
					if (showPost.isClosed()) {
						showPost.setCloserid(null);
					} else {
						showPost.setCloserid(authUser.getId());
					}
					redirectTo = escapelink+"/"+showPost.getId();
					updated = true;
				}
			}else if(param("restore")){
				Long revid = NumberUtils.toLong(getParamValue("revisionid"));
				if(canEdit && revid.longValue() != 0L){
					addBadge(Badge.BACKINTIME, true);
					showPost.restoreRevisionAndUpdate(revid);
				}
			}else if(param("delete")){
				if (showPost.isQuestion() || showPost.isFeedback()) {
					if((isMine || inRole("mod")) && !showPost.getDeleteme()){
						//delete / undelete flags
						showPost.setDeleteme(true);
						
						Report rep = new Report();
						rep.setParentuuid(showPost.getUuid());
						rep.setClassname(Post.class.getSimpleName());
						rep.setLink(escapelink+"/"+showPost.getId());
						rep.setDescription(lang.get("posts.marked"));
						rep.setType(ReportType.OTHER);
						rep.setAuthor(authUser.getFullname());
						rep.setUserid(authUser.getId());

						Long rid = rep.create();
						showPost.setDeletereportid(rid);
						updated = true;
					}
				} else if(showPost.isAnswer() || showPost.isTranslation()) {
					if(isMine || inRole("mod")){
						if(showPost.getParentpostid() != null){
							redirectTo = escapelink+"/"+showPost.getParentpostid();
						}else{
							redirectTo = escapelink;
						}
						updated = false;
						showPost.delete();
						if(showPost.isAnswer()){
							Post parent = Post.getPostDao().read(showPost.getParentpostid());
							parent.setAnswercount(parent.getAnswercount() - 1);
							parent.update();
						}
					}
				}
			}else if(param("undelete")){
				if (showPost.isQuestion() || showPost.isFeedback()) {
					if((isMine || inRole("mod")) && showPost.getDeleteme()){
						showPost.setDeleteme(false);
						Report rep = Report.getReportDAO().read(showPost.getDeletereportid());
						if(rep != null) rep.delete();
						showPost.setDeletereportid(null);
						updated = true;
					}
				}
			}

			
			//update view count if this is a question
			if(showPost != null && (updated || updateViewCount(showPost))){
				showPost.update();
			}

			if(redirectTo != null){
				setRedirect(redirectTo);
			}else{
				showPost.getComments(new MutableLong(1));
			}
		}
	}

	public final void processPostEditRequest(Post post, String escapelink, boolean canEdit){
		if(!canEdit || post == null) return;

//		if (post.isQuestion() || post.isAnswer()) {
//			actionlink = questionlink;
//		} else if(post.isFeedback()){
//			actionlink = feedbacklink;
//		}

		if(param("answer")){
			// add new answer
			if(!post.isClosed() && !post.isAnswer() && !post.isBlackboard() &&
					post.getAnswercount() < AbstractDAOFactory.MAX_ANSWERS_PER_POST){
				//create new answer
				Post newq = new Post();
				newq.setPostType(PostType.ANSWER);
				newq.setUserid(authUser.getId());
				newq.setParentuuid(post.getUuid());
				newq.setParentpostid(post.getId());
				newq.setBody(getParamValue("body"));
				newq.create();

				post.setAnswercount(post.getAnswercount() + 1);
				if(post.getAnswercount() >= AbstractDAOFactory.MAX_ANSWERS_PER_POST){
					post.setCloserid(0L);
				}
				post.updateLastActivity();
				post.update();

				addBadge(Badge.EUREKA, newq.getUserid().equals(post.getUserid()));
				setRedirect(escapelink+"/"+post.getId()+"#post-"+newq.getId());
			}
		}else if(param("accept")){
			boolean isMine = (authenticated) ? authUser.getId()
					.equals(post.getUserid()) : false;
			Long ansid = NumberUtils.toLong(getParamValue("answerid"), 0);
			if(canEdit && ansid > 0L && isMine){
				Post answer = Post.getPostDao().read(ansid);

				if(answer != null && answer.isAnswer()){
					User author = User.getUserDao().read(answer.getUserid());
					if(author != null && authenticated){
						boolean same = author.equals(authUser);
						
						if(ansid.equals(post.getAnswerid())){
							// Answer accepted award - UNDO
							post.setAnswerid(null);
							if (!same) {
								author.removeRep(User.ANSWER_ACCEPT_REWARD_AUTHOR);
								authUser.removeRep(User.ANSWER_ACCEPT_REWARD_VOTER);
							}
						}else{
							// Answer accepted award - GIVE
							post.setAnswerid(ansid);
							if(!same){
								author.addRep(User.ANSWER_ACCEPT_REWARD_AUTHOR);
								authUser.addRep(User.ANSWER_ACCEPT_REWARD_VOTER);
								addBadgeOnce(Badge.NOOB, true);
							}
						}

						author.update();
						authUser.update();
						post.update();
					}
				}
			}
		}else if(param("editpostid")){
			// edit post
			Post beforeUpdate = new Post();

			beforeUpdate.setType(post.getType());
			beforeUpdate.setTitle(post.getTitle());
			beforeUpdate.setBody(post.getBody());
			if (post.isQuestion()) {
				beforeUpdate.setTags(post.getTags());
				beforeUpdate.fixTags();
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
				post.fixTags();
			}

			post.setLasteditby(authUser.getId());
			post.fixTags();
			//note: update only happens if something has changed
			boolean done = post.updateAndCreateRevision(beforeUpdate);

			addBadgeOnce(Badge.EDITOR, done);

			if(post.isAnswer()){
				Long parentPostId = NumberUtils.toLong(getParamValue("parentpostid"), 0);
				if (parentPostId.longValue() == 0L) {
					parentPostId = post.getParentpostid();
				}
				if(!isAjaxRequest()) setRedirect(escapelink+"/"+parentPostId);
			}else if(post.isQuestion() || post.isFeedback()){
				if(!isAjaxRequest()) setRedirect(escapelink+"/"+post.getId());
			}else if(post.isBlackboard()){
				if(!isAjaxRequest()) setRedirect(escapelink);
			}
		}
	}

	private boolean updateViewCount(Post showPost){
		//do not count views from author
		if(authenticated && authUser.getId().equals(showPost.getUserid())) return false;
		// inaccurate but... KISS!
		String list = getStateParam("postviews");
		
		if(list == null){			
			list = showPost.getId().toString();
			setStateParam("postviews", list);
		}
		
		if (!list.contains(showPost.getId().toString())) {
			long views = (showPost.getViewcount() == null) ? 0 : showPost.getViewcount();
			showPost.setViewcount(views + 1); //increment count
			list = list.concat(",").concat(showPost.getId().toString());
			setStateParam("postviews", list);
			return true;
		}
		return false;
	}
	
	public final Form getQuestionForm(String parentuuid, Map<Long, School> schoolsMap){
		if(!authenticated || (StringUtils.isBlank(parentuuid) &&
				(schoolsMap == null || schoolsMap.isEmpty())))
			return null;

		if(schoolsMap == null) schoolsMap = authUser.getSchoolsMap();

		Form qForm = getPostForm(PostType.QUESTION, "qForm", "ask-question-form");
		Field puuid = null;
		Long pid = 0L;

		if (param("schoolid")) {
			pid = NumberUtils.toLong(getParamValue("schoolid"), 0);
			addModel("postParentClass", School.class.getSimpleName().toLowerCase());
		} else if(param("classid")) {
			pid = NumberUtils.toLong(getParamValue("classid"), 0);
			addModel("postParentClass", Classunit.class.getSimpleName().toLowerCase());
		}

		School selected = schoolsMap.get(pid);
		if(selected != null){
			parentuuid = selected.getUuid();
			addModel("postParentId", pid);
		}

		if (StringUtils.isBlank(parentuuid)) {
			puuid = new Select("parentuuid", lang.get("posts.belongsto"), true);
			((Select) puuid).add(new Option("", lang.get("chooseone")));
			for (com.scoold.core.School school : schoolsMap.values()) {
				((Select) puuid).add(new Option(school.getUuid(), school.getName()));
			}
		} else {
			puuid = new HiddenField("parentuuid", parentuuid);
		}

		qForm.add(puuid);

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

		String submittxt = "Post";
		if (type == PostType.QUESTION) {
			submittxt = lang.get("posts.answerit");
		} else if(type == PostType.FEEDBACK) {
			submittxt = lang.get("feedback.reply");
		}

        Submit submit = new Submit("answerbtn",
				submittxt, this, "onAnswerClick");
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
		if (type == PostType.QUESTION) {
			tags = new TextField("tags", true);
			tags.setLabel(lang.get("tags.tags"));
			((TextField) tags).setMaxLength(255);
			tags.setTabIndex(3);
		} else if(type == PostType.FEEDBACK) {
			tags = new RadioGroup("tags", lang.get("feedback.type"), true);
			String bug = FeedbackType.BUG.name().toLowerCase();
			String question = FeedbackType.QUESTION.name().toLowerCase();
			String suggestion = FeedbackType.SUGGESTION.name().toLowerCase();

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

		String submittxt = lang.get("post");
		if (type == PostType.QUESTION) {
			submittxt = lang.get("posts.askit");
		}
		
        Submit submit = new Submit("askbtn", submittxt, this, "onAskClick");
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
		if (post.isAnswer()) {
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
		String parentuuid = getParamValue("parentuuid");
		if(StringUtils.isBlank(parentuuid) && type != PostType.FEEDBACK) {
			setRedirect(questionslink + "/ask");
			return;
		}

		Post newq = new Post();
		newq.setTitle(getParamValue("title"));
		newq.setBody(getParamValue("body"));
		newq.setTags(getParamValue("tags"));
		newq.setParentuuid(parentuuid);

		newq.setUserid(authUser.getId());
		newq.setPostType(type);
		newq.fixTags();

		Long id = newq.create();

//		String titleEnc = AbstractDAOUtils.urlEncode(newq.getTitle());
		if (type == PostType.QUESTION) {
			setRedirect(questionlink+"/"+id.toString());
		} else if(type == PostType.FEEDBACK){
			String tags = newq.getTagsString();
			if(tags.equalsIgnoreCase(FeedbackType.BUG.name()) ||
					tags.equalsIgnoreCase(FeedbackType.QUESTION.name()) ||
					tags.equalsIgnoreCase(FeedbackType.SUGGESTION.name())){
				newq.setTags(FeedbackType.QUESTION.name().toLowerCase());
				newq.fixTags();
			}
			setRedirect(feedbacklink+"/"+id.toString());
		}
	}

	/******** VOTING ********/

	public boolean processVoteRequest(Votable<Long> votable, String classname, String uuid){
		boolean result = false;
		if(votable != null && authenticated){
			User author = User.getUser(votable.getUserid());
			Integer votes = (votable.getVotes() == null) ? 0 : votable.getVotes();

			if(param("voteup")){
				result = votable.voteUp(authUser.getId());

				if(!result) return result;

				authUser.incrementUpvotes();
				int award = 0;

				if(StringUtils.equalsIgnoreCase(classname, Post.class.getSimpleName())){
					Post p = (Post) votable;
					if(p.isAnswer()){
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
				if(!result) return result;

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

					if(p.isQuestion() || p.isFeedback()){
						//mark post for closing
						Report rep = new Report();
						rep.setParentuuid(uuid);
						rep.setClassname(Post.class.getSimpleName());
						if (p.isQuestion()) {
							rep.setLink(questionlink+"/"+p.getId());
						} else if(p.isFeedback()) {
							rep.setLink(feedbacklink+"/"+p.getId());
						}
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

		return result;
	}

	/******  MISC *******/
	
	public final boolean param(String param){
		return getContext().getRequestParameter(param) != null;
	}

	public final String getParamValue(String param){
		return getContext().getRequestParameter(param);
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
		AbstractDAOUtils.clearSession(req, getContext().getResponse(), USE_SESSIONS);
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
		
		if(!authUser.getUuid().equals(u.getUuid())){
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

			long oneYear = authUser.getTimestamp() + (365 * 24 * 60 * 60 * 1000);
			long now = System.currentTimeMillis();

			addBadgeOnce(Badge.ENTHUSIAST, authUser.getReputation() >= User.ENTHUSIAST_IFHAS);
			addBadgeOnce(Badge.FRESHMAN, authUser.getReputation() >= User.FRESHMAN_IFHAS);
			addBadgeOnce(Badge.SCHOLAR, authUser.getReputation() >= User.SCHOLAR_IFHAS);
			addBadgeOnce(Badge.TEACHER, authUser.getReputation() >= User.TEACHER_IFHAS);
			addBadgeOnce(Badge.PROFESSOR, authUser.getReputation() >= User.PROFESSOR_IFHAS);
			addBadgeOnce(Badge.GEEK, authUser.getReputation() >= User.GEEK_IFHAS);
			addBadgeOnce(Badge.SENIOR, now >= oneYear);
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

	public static String DESCRIPTION = "Scoold is place for sharing knowledge. "
			+ "It's all about helping your mates and learning new stuff.";
	
	public static String KEYWORDS = "scoold, knowledge sharing, collaboration, wiki, "
			+ "schools, students, classmates, alumni, teachers, contacts, social, "
			+ "network, classes, classroom, education";
}
