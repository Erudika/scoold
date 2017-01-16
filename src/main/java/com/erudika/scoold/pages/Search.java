/*
 * Copyright 2013-2017 Erudika. https://erudika.com
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

package com.erudika.scoold.pages;

import com.erudika.para.core.User;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Feedback;
import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Search extends Base{

	public String title;
	public List<User> userlist;
	public List<Post> questionlist;
	public List<Post> feedbacklist;
	public String url;

	public Pager questioncount;
	public Pager feedbackcount;
	public Pager usercount;

	public long totalCount;
	public int maxSearchResults = 10;

	private static final String DEFAULT_FEED_TYPE = "atom_1.0";
    private static final String MIME_TYPE = "application/xml; charset=UTF-8";
    private static final String COULD_NOT_GENERATE_FEED_ERROR = "Could not generate feed";

	public Search() {
		title = lang.get("pc.title");
		questionlist = new ArrayList<Post>();
		feedbacklist = new ArrayList<Post>();
		userlist = new ArrayList<User>();

		questioncount = new Pager();
		feedbackcount = new Pager();
		usercount = new Pager();

		totalCount = 0;
	}

	public void onGet() {
		if (param("q") && !StringUtils.isBlank(getParamValue("q"))) {
			String query = getParamValue("q");

			if (showParam != null) {
				if ("questions".equals(showParam)) {
					questionlist = pc.findQuery(Utils.type(Question.class), query, itemcount);
				} else if ("feedback".equals(showParam)) {
					feedbacklist = pc.findQuery(Utils.type(Feedback.class), query, itemcount);
				} else if ("people".equals(showParam)) {
					userlist = pc.findQuery(Utils.type(User.class), query, itemcount);
				}
				totalCount = itemcount.getCount();
			} else {
				questionlist = pc.findQuery(Utils.type(Question.class), query, questioncount);
				feedbacklist = pc.findQuery(Utils.type(Feedback.class), query, feedbackcount);
				userlist = pc.findQuery(Utils.type(User.class), query, usercount);
				totalCount = (questioncount.getCount() + feedbackcount.getCount() + usercount.getCount());
			}
		} else if (param("feed")) {
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

		List<Post> questions = pc.findQuery(Utils.type(Question.class), "*");
		String baseuri = "http://scoold.com/";
		feed.setTitle("Scoold - Recent questions");
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

}
