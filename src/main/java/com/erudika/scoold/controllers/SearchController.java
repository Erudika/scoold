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

package com.erudika.scoold.controllers;

import com.erudika.scoold.core.Post;
import java.util.List;
import com.erudika.scoold.core.Profile;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class SearchController {

	public String title;
	public List<Profile> userlist;
	public List<Post> questionlist;
	public List<Post> answerlist;
	public List<Post> feedbacklist;
	public String url;

	public long totalCount;

	private static final String DEFAULT_FEED_TYPE = "atom_1.0";
    private static final String MIME_TYPE = "application/xml; charset=UTF-8";
    private static final String COULD_NOT_GENERATE_FEED_ERROR = "Could not generate feed";

//	public SearchController() {
//		title = lang.get("search.title");
//		addModel("searchSelected", "navbtn-hover");
//		questionlist = new ArrayList<Post>();
//		answerlist = new ArrayList<Post>();
//		feedbacklist = new ArrayList<Post>();
//		userlist = new ArrayList<Profile>();
//		totalCount = 0;
//	}
//
//	public void onGet() {
//		if (param("q") && !StringUtils.isBlank(getParamValue("q"))) {
//			String query = getParamValue("q");
//
//			if (showParam != null) {
//				if ("questions".equals(showParam)) {
//					questionlist = pc.findQuery(Utils.type(Question.class), query, itemcount);
//				} else if ("answers".equals(showParam)) {
//					answerlist = pc.findQuery(Utils.type(Reply.class), query, itemcount);
//				} else if ("feedback".equals(showParam)) {
//					feedbacklist = pc.findQuery(Utils.type(Feedback.class), query, itemcount);
//				} else if ("people".equals(showParam)) {
//					userlist = pc.findQuery(Utils.type(Profile.class), query, itemcount);
//				}
//			} else {
//				List<ParaObject> results = pc.findQuery(null, query, itemcount);
//				for (ParaObject result : results) {
//					if (Utils.type(Question.class).equals(result.getType())) {
//						questionlist.add((Post) result);
//					} else if (Utils.type(Reply.class).equals(result.getType())) {
//						answerlist.add((Post) result);
//					} else if (Utils.type(Feedback.class).equals(result.getType())) {
//						feedbacklist.add((Post) result);
//					} else if (Utils.type(Profile.class).equals(result.getType())) {
//						userlist.add((Profile) result);
//					}
//				}
//
//			}
//			ArrayList<Post> list = new ArrayList<Post>();
//			list.addAll(questionlist);
//			list.addAll(answerlist);
//			list.addAll(feedbacklist);
//			fetchProfiles(list);
//			totalCount = itemcount.getCount();
//		} else if (param("feed")) {
//			try {
//				SyndFeed feed = getFeed();
//				String feedType = (param("type")) ? getParamValue("type") : DEFAULT_FEED_TYPE;
//				feed.setFeedType(feedType);
//
//				resp.setContentType(MIME_TYPE);
//				SyndFeedOutput output = new SyndFeedOutput();
//				output.output(feed, resp.getWriter());
//			} catch (Exception ex) {
//				logger.error(COULD_NOT_GENERATE_FEED_ERROR, ex);
//				try {
//					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
//							COULD_NOT_GENERATE_FEED_ERROR);
//				} catch (IOException ex1) {
//					logger.error(null, ex1);
//				}
//			}
//		}
//	}
//
//	private SyndFeed getFeed() throws IOException,FeedException {
//		SyndFeed feed = new SyndFeedImpl();
//
//		List<Post> questions = pc.findQuery(Utils.type(Question.class), "*");
//		String baseurl = Config.getConfigParam("base_url", "http://scoold.com");
//		baseurl = baseurl.endsWith("/") ? baseurl : baseurl + "/";
//		feed.setTitle("Scoold - Recent questions");
//        feed.setLink(baseurl);
//        feed.setDescription("A summary of the most recent questions asked on Scoold.");
//
//        List<SyndEntry> entries = new ArrayList<SyndEntry>();
//
//		for (Post post : questions) {
//			SyndEntry entry;
//			SyndContent description;
//			String baselink = baseurl.concat("question/").concat(post.getId());
//
//			entry = new SyndEntryImpl();
//			entry.setTitle(post.getTitle());
//			entry.setLink(baselink);
//			entry.setPublishedDate(new Date(post.getTimestamp()));
//			entry.setAuthor(baseurl.concat("profile/").concat(post.getCreatorid()));
//			entry.setUri(baselink.concat("/").concat(Utils.stripAndTrim(post.getTitle()).
//					replaceAll("\\p{Z}+","-").toLowerCase()));
//
//			description = new SyndContentImpl();
//			description.setType("text/html");
//			description.setValue(Utils.markdownToHtml(post.getBody()));
//
//			entry.setDescription(description);
//			entries.add(entry);
//		}
//
//		feed.setEntries(entries);
//
//        return feed;
//    }

}
