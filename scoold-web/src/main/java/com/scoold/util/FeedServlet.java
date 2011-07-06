/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.util;

import com.scoold.core.Post;
import com.scoold.core.Post.PostType;
import com.scoold.core.Search;
import com.scoold.core.User;
import com.scoold.db.AbstractDAOUtils;
import com.scoold.pages.BasePage;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.client.Client;

/**
 *
 * @author alexb
 */
public class FeedServlet extends HttpServlet {


	private static final String DEFAULT_FEED_TYPE = "atom_1.0";
    private static final String FEED_TYPE = "type";
    private static final String MIME_TYPE = "application/xml; charset=UTF-8";
    private static final String COULD_NOT_GENERATE_FEED_ERROR = "Could not generate feed";

	private boolean personalized;
	private Search search;
	private Object convert;

	public void init() {
		Client searchClient = (Client) getServletContext().getAttribute("searchClient");
		search = new Search(searchClient);
		convert = getServletContext().getAttribute("showdownConverter");
		personalized = false;
    }

    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException {
         try {
            SyndFeed feed = getFeed(req);

            String feedType = req.getParameter(FEED_TYPE);
            feedType = (feedType != null) ? feedType : DEFAULT_FEED_TYPE;
            feed.setFeedType(feedType);

            res.setContentType(MIME_TYPE);
            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed,res.getWriter());
        }
        catch (FeedException ex) {
            String msg = COULD_NOT_GENERATE_FEED_ERROR;
            log(msg,ex);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,msg);
        }

    }

	protected SyndFeed getFeed(HttpServletRequest req) throws IOException,FeedException {
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
			description.setValue(AbstractDAOUtils.markdownToHtml(post.getBody(), convert));
			
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
				questionslist = Post.getPostDao().
						readAllSortedBy("timestamp", null, null, true);
				personalized = false;
			}else{
				personalized = true;
				questionslist = search.findPostsForTags(PostType.QUESTION, favtags, null, null);
			}
		}else{
			//show top questions
			personalized = false;
			questionslist = Post.getPostDao().
					readAllSortedBy("timestamp", null, null, true);
		}

		return questionslist;
	}

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
