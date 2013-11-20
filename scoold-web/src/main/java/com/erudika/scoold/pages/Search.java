/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.para.persistence.DAO;
import com.erudika.para.core.PObject;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Classunit;
import com.erudika.scoold.core.School;
import com.erudika.scoold.core.User;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Feedback;
import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;
import java.io.IOException;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class Search extends Base{

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
		if(param("q") && !StringUtils.isBlank(getParamValue("q"))){
			String query = getParamValue("q");
			
			if (showParam != null) {
				if ("questions".equals(showParam)) {
					questionlist = search.findQuery(PObject.classname(Question.class), pagenum, itemcount, query);
				} else if("feedback".equals(showParam)) {
					feedbacklist = search.findQuery(PObject.classname(Feedback.class), pagenum, itemcount, query);
				} else if("people".equals(showParam)) {
					userlist = search.findQuery(PObject.classname(User.class), pagenum, itemcount, query);
				} else if("schools".equals(showParam)) {
					schoollist = search.findQuery(PObject.classname(School.class), pagenum, itemcount, query);
				} else if("classes".equals(showParam)) {
					classlist = search.findQuery(PObject.classname(Classunit.class), pagenum, itemcount, query);
				}
				totalCount = itemcount.intValue();
			} else { 
				questionlist = search.findQuery(PObject.classname(Question.class), questionpage, questioncount, query, "", true, maxSearchResults);
				feedbacklist = search.findQuery(PObject.classname(Feedback.class), feedbackpage, feedbackcount, query, "", true, maxSearchResults);
				userlist = search.findQuery(PObject.classname(User.class), userpage, usercount, query, "", true, maxSearchResults);
				schoollist = search.findQuery(PObject.classname(School.class), schoolpage, schoolcount, query, "", true, maxSearchResults);
				classlist = search.findQuery(PObject.classname(Classunit.class), classpage, classcount, query, "", true, maxSearchResults);
				totalCount = (questioncount.intValue() + feedbackcount.intValue() +
						usercount.intValue() + schoolcount.intValue() + classcount.intValue());
			}
		}else if(param("feed")){
			try {
				SyndFeed feed = getFeed(req);
				String feedType = (param("type")) ? getParamValue("type") : DEFAULT_FEED_TYPE;
				feed.setFeedType(feedType);

				resp.setContentType(MIME_TYPE);
				SyndFeedOutput output = new SyndFeedOutput();
//				addModel("feedstring", output.outputString(feed));
				output.output(feed, resp.getWriter());
			} catch (Exception ex) {
				logger.error(COULD_NOT_GENERATE_FEED_ERROR, ex);
				try {
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
							COULD_NOT_GENERATE_FEED_ERROR);
				} catch (IOException ex1) {
					logger.error(null, ex1);
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
			String baselink = baseuri.concat("question/").concat(post.getId());

			entry = new SyndEntryImpl();
			entry.setTitle(post.getTitle());
			entry.setLink(baselink);
			entry.setPublishedDate(new Date(post.getTimestamp()));
			entry.setAuthor(baseuri.concat("profile/").concat(post.getCreatorid().toString()));
			entry.setUri(baselink.concat("/").concat(Utils.stripAndTrim(post.getTitle()).replaceAll("\\p{Z}+","-").toLowerCase()));
			
			description = new SyndContentImpl();
			description.setType("text/html");
			description.setValue(Utils.markdownToHtml(post.getBody()));
			
			entry.setDescription(description);
			entries.add(entry);
		}

		feed.setEntries(entries);

        return feed;
    }

	private ArrayList<Post> getQuestionsList(HttpServletRequest req){
		ArrayList<Post> questionslist = null;
		String uid = req.getParameter(DAO.CN_ID);
		String key = req.getParameter("key");
		User u = dao.read(uid);
		
		if(u != null && !StringUtils.isBlank(key) && key.equals(Utils.MD5(uid + Base.FEED_KEY_SALT))){
			
			//show selected questions
			ArrayList<String> favtags = u.getFavtagsList();
			if(favtags == null || favtags.isEmpty()){
				questionslist = search.findQuery(PObject.classname(Question.class), null, null, "*");
				personalized = false;
			}else{
				personalized = true;
				questionslist = search.findTagged(PObject.classname(Question.class), null, null, favtags);
			}
		}else{
			//show top questions
			personalized = false;
			questionslist = search.findQuery(PObject.classname(Question.class), null, null, "*");
		}

		return questionslist;
	}
}
