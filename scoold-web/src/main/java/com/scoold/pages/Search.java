/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import com.scoold.core.Classunit;
import com.scoold.core.School;
import com.scoold.core.User;
import com.scoold.core.Post;
import com.scoold.core.Post.PostType;
import com.scoold.db.AbstractDAOUtils;
import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public class Search extends BasePage{

	public String title;
	public ArrayList<User> userlist;
	public ArrayList<Classunit> classlist;
	public ArrayList<School> schoollist;
	public ArrayList<Post> questionlist;
	public ArrayList<Post> feedbacklist;
	public String url;
	
	public MutableLong questioncount;
	public MutableLong feedbackcount;
	public MutableLong usercount;
	public MutableLong schoolcount;
	public MutableLong classcount;
	
	public MutableLong questionpage;
	public MutableLong feedbackpage;
	public MutableLong userpage;
	public MutableLong schoolpage;
	public MutableLong classpage;
	
	public int totalCount;
	public int maxSearchResults = 10;
	
	private static final String DEFAULT_FEED_TYPE = "atom_1.0";
    private static final String MIME_TYPE = "application/xml; charset=UTF-8";
    private static final String COULD_NOT_GENERATE_FEED_ERROR = "Could not generate feed";

	private boolean personalized;
 
	public Search() {
		title = lang.get("search.title");
		questionlist = new ArrayList<Post>();
		feedbacklist = new ArrayList<Post>();
		userlist = new ArrayList<User>();
		schoollist = new ArrayList<School>();
		classlist = new ArrayList<Classunit>();

		questioncount = new MutableLong(0);
		feedbackcount = new MutableLong(0);
		usercount = new MutableLong(0);
		schoolcount = new MutableLong(0);
		classcount = new MutableLong(0);
		
		questionpage = new MutableLong(0);
		feedbackpage = new MutableLong(0);
		userpage = new MutableLong(0);
		schoolpage = new MutableLong(0);
		classpage = new MutableLong(0);
		
		totalCount = 0; 
	}
	
	public void onGet(){
		if(param("q")){
			String query = getParamValue("q");

			if (showParam != null) {
				if ("questions".equals(showParam)) {
					questionlist = daoutils.readAndRepair(Post.class, daoutils.findQuery(
					PostType.QUESTION.toString(), pagenum, itemcount, query), itemcount);
				} else if("feedback".equals(showParam)) {
					feedbacklist = daoutils.readAndRepair(Post.class, daoutils.findQuery(
					PostType.FEEDBACK.toString(), pagenum, itemcount, query), itemcount);
				} else if("people".equals(showParam)) {
					userlist = daoutils.readAndRepair(User.class, daoutils.findQuery(
					User.classtype, pagenum, itemcount, query), itemcount);
				} else if("schools".equals(showParam)) {
					schoollist = daoutils.readAndRepair(School.class, daoutils.findQuery(
					School.classtype, pagenum, itemcount, query), itemcount);
				} else if("classes".equals(showParam)) {
					classlist = daoutils.readAndRepair(Classunit.class, daoutils.findQuery(
					Classunit.classtype, pagenum, itemcount, query), itemcount);
				}
				totalCount = itemcount.intValue();
			} else { 
				questionlist = daoutils.readAndRepair(Post.class, daoutils.findQuery(
					PostType.QUESTION.toString(), questionpage, questioncount, query, "", 
					true, maxSearchResults), itemcount);
				
				feedbacklist = daoutils.readAndRepair(Post.class, daoutils.findQuery(
					PostType.FEEDBACK.toString(), feedbackpage, feedbackcount, query, "", 
					true, maxSearchResults), itemcount);
				userlist = daoutils.readAndRepair(User.class, daoutils.findQuery(
					User.classtype, userpage, usercount, query, "", 
					true, maxSearchResults), itemcount);
				schoollist = daoutils.readAndRepair(School.class, daoutils.findQuery(
					School.classtype, schoolpage, schoolcount, query, "", 
					true, maxSearchResults), itemcount);
				classlist = daoutils.readAndRepair(Classunit.class, daoutils.findQuery(
					Classunit.classtype, classpage, classcount, query, "", 
					true, maxSearchResults), itemcount);
				totalCount = (questioncount.intValue() + feedbackcount.intValue() +
						usercount.intValue() + schoolcount.intValue() + classcount.intValue());
			}
		}else if(param("feed")){
			try {
				SyndFeed feed = getFeed(req);
				String feedType = (param("type")) ? getParamValue("type") : DEFAULT_FEED_TYPE;
				feed.setFeedType(feedType);

				getContext().getResponse().setContentType(MIME_TYPE);
				SyndFeedOutput output = new SyndFeedOutput();
//				addModel("feedstring", output.outputString(feed));
				output.output(feed, getContext().getResponse().getWriter());
			} catch (Exception ex) {
				logger.log(Level.SEVERE, COULD_NOT_GENERATE_FEED_ERROR, ex);
				try {
					getContext().getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
							COULD_NOT_GENERATE_FEED_ERROR);
				} catch (IOException ex1) {
					logger.log(Level.SEVERE, null, ex1);
				}
			}
		}
	}

	private SyndFeed getFeed(HttpServletRequest req) throws IOException,FeedException {
		SyndFeed feed = new SyndFeedImpl();

		ArrayList<Post> questions = getQuestionsList(req);
		String baseuri = "http://scoold.com/";

		if(personalized){
			feed.setTitle("Scoold - Selected questions");
		}else{
			feed.setTitle("Scoold - Recent questions");
		}

        feed.setLink(baseuri);
        feed.setDescription("A summary of the most recent questions asked on Scoold.");

        List<SyndEntry> entries = new ArrayList<SyndEntry>();

		for (Post post : questions) {
			SyndEntry entry;
			SyndContent description;
			String baselink = baseuri.concat("question/").concat(post.getId().toString());

			entry = new SyndEntryImpl();
			entry.setTitle(post.getTitle());
			entry.setLink(baselink);
			entry.setPublishedDate(new Date(post.getTimestamp()));
			entry.setAuthor(baseuri.concat("profile/").concat(post.getUserid().toString()));
			entry.setUri(baselink.concat("/").concat(AbstractDAOUtils.
					stripAndTrim(post.getTitle()).replaceAll("\\p{Z}+","-").toLowerCase()));
			
			description = new SyndContentImpl();
			description.setType("text/html");
			description.setValue(AbstractDAOUtils.markdownToHtml(post.getBody()));
			
			entry.setDescription(description);
			entries.add(entry);
		}

		feed.setEntries(entries);

        return feed;
    }

	private ArrayList<Post> getQuestionsList(HttpServletRequest req){
		ArrayList<Post> questionslist = null;
		String uid = req.getParameter("userid");
		String key = req.getParameter("key");

		if(!StringUtils.isBlank(uid) && !StringUtils.isBlank(key) && 
				key.equals(AbstractDAOUtils.MD5(uid + BasePage.FEED_KEY_SALT))){

			//show selected questions
			ArrayList<String> favtags = User.getUserDao().
					getFavouriteTagsForUser(NumberUtils.toLong(uid));
			if(favtags == null || favtags.isEmpty()){
				questionslist = daoutils.readAndRepair(Post.class, daoutils.findQuery(
					PostType.QUESTION.toString(), null, null, "*"), null);
				personalized = false;
			}else{
				personalized = true;
				questionslist = daoutils.readAndRepair(Post.class, daoutils.findTagged(
					PostType.QUESTION.toString(), null, null, favtags), null);
			}
		}else{
			//show top questions
			personalized = false;
			questionslist = daoutils.readAndRepair(Post.class, daoutils.findQuery(
					PostType.QUESTION.toString(), null, null, "*"), null);
		}

		return questionslist;
	}
}
