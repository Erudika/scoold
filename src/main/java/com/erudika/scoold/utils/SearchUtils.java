/*
 * Copyright 2013-2026 Erudika. https://erudika.com
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

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.Sticky;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.text.WordUtils;

/**
 * Helper methods around full-text search.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public final class SearchUtils {

	private static final SearchUtils INSTANCE = new SearchUtils();

	/**
	 * Default constructor.
	 */
	private SearchUtils() {
	}

	public static SearchUtils getInstance() {
		return INSTANCE;
	}

	/**
	 * Resolves the ScooldUtils singleton lazily - at call time rather than at
	 * construction/class-load time - so this class can be safely loaded before the
	 * web layer has wired {@link ScooldUtils#setInstance} without NPE'ing.
	 * @return the current ScooldUtils instance
	 */
	private ScooldUtils utils() {
		ScooldUtils utils = ScooldUtils.getInstance();
		if (utils == null) {
			throw new IllegalStateException("ScooldUtils not initialized yet.");
		}
		return utils;
	}

	private ParaClient paraClient() {
		return utils().getParaClient();
	}

	/**
	 * Builds the space-filter prefix for the "users" templates. User-profile
	 * searches search on {@code properties.spaces} (plural), unlike the generic
	 * post search which uses {@code properties.space}.
	 * @param req the HTTP request
	 * @return a pre-built space-filter string, possibly blank
	 */
	private String getUsersSpaceFilter(HttpServletRequest req) {
		return sanitizeQueryString("", req).queryString().replaceAll("properties\\.space:", "properties.spaces:");
	}

	public SanitizedQuery sanitizeQueryString(String query, Profile authUser, String space) {
		return sanitizeQueryString(utils().getSpaceFilteredQuery(authUser, space), query);
	}

	public SanitizedQuery sanitizeQueryString(String query, HttpServletRequest req) {
		return sanitizeQueryString(utils().getSpaceFilteredQuery(req), query);
	}

	private SanitizedQuery sanitizeQueryString(String qf, String query) {
		String q = StringUtils.trimToEmpty(query);
		// Sanitize the user's query (escape special chars, strip trailing wildcards,
		// phrase-quote multi-word input). Applied regardless of qf so the search
		// backend receives a well-formed query in both the unfiltered (qf == "*")
		// and space-filtered cases. Previously this block was gated on
		// `qf.isEmpty() || qf.length() > 1` which skipped entirely for qf == "*",
		// leaving multi-word queries raw and causing exact-title searches to be
		// decomposed into OR/AND-separated tokens.
		if (!q.isEmpty()) {
			q = q.replaceAll("[\\?<>]", "").trim();
			// Strip trailing wildcards from non-trivial queries so users can't
			// inject Lucene prefix syntax. Preserve the literal "*" (match-all).
			if (!"*".equals(q)) {
				q = q.replaceAll("\\*+$", "");
			}
			q = RegExUtils.replaceAll(q, ":\\s+", "\\\\: ");
			q = RegExUtils.replaceAll(q, "(?<!:)\\((.*?)\\)", "\\\\($1\\\\)");
			q = q.trim();
			// Multi-word plain-text queries are wrapped in double quotes so the
			// search backend treats them as a phrase rather than decomposing them
			// into OR/AND-separated tokens (which buries exact-title matches).
			// Queries that already look like Lucene syntax (a field ":", a bare
			// operator, are already quoted, or contain group parens) are left
			// untouched. Single words and the literal "*" are not quoted.
			if (q.contains(" ") && !q.startsWith("\"") && new SanitizedQuery("", q).isSimpleTermQuery()) {
				q = "\"" + q + "\"";
			}
		}
		return new SanitizedQuery(qf, q);
	}

	/**
	 * Users page query: combines the space filter with the raw search term, and
	 * optionally further filters by having/not-having spaces and a tag.
	 * @param q the raw query string
	 * @param havingSpaces required spaces
	 * @param notHavingSpaces excluded spaces
	 * @param tag optional tag (or a {@code groups:...} filter)
	 * @param req the HTTP request
	 * @return the composed Lucene query string
	 */
	public String getUserQuery(String q, Set<String> havingSpaces, Set<String> notHavingSpaces, String tag, HttpServletRequest req) {
		// [space query filter] + original query string
		String qs = StringUtils.strip(q).replaceAll("[\\*\"\\)\\(]", "").replaceAll("_", " ");

		Profile authUser = utils().getAuthUser(req);
		// [space query filter] + original query string
		SanitizedQuery sanitizedQ = sanitizeQueryString(utils().getSpaceFilteredQuery(req), qs);
		qs = sanitizedQ.queryString();
		if (req.getParameter("bulkedit") != null && utils().isAdmin(authUser)) {
			qs = q;
		} else {
			qs = qs.replaceAll("properties\\.space:", "properties.spaces:");
		}

		if (!qs.endsWith("*") && q.equals("*")) {
			// admins are members of every space and always visible
			qs += (qs.isBlank() ? "" : " OR ") + "properties.groups:(admins OR mods)";
		}

		if (!Strings.CS.equalsAny(q.trim(), "", "*")) {
			qs = getUsersSearchQuery(q, req);
		}

		String havingSpacesFilter = "";
		String notHavingSpacesFilter = "";
		if (!havingSpaces.isEmpty()) {
			havingSpacesFilter = "+\"" + String.join("\" +\"", havingSpaces) + "\" ";
		}
		if (!notHavingSpaces.isEmpty()) {
			notHavingSpacesFilter = "-\"" + String.join("\" -\"", notHavingSpaces) + "\"";
			if (havingSpaces.isEmpty()) {
				// at least one + keyword is needed otherwise no search results are returned
				havingSpacesFilter = "+\"" + utils().getDefaultSpace() + "\"";
			}
		}
		if (utils().isMod(authUser) && (!havingSpaces.isEmpty() || !notHavingSpaces.isEmpty())) {
			StringBuilder sb = new StringBuilder("*".equals(qs) ? "" : "(".concat(qs).concat(") AND "));
			sb.append("properties.spaces").append(":(").append(havingSpacesFilter).append(notHavingSpacesFilter).append(")");
			qs = sb.toString();
		}
		if (!StringUtils.isBlank(tag)) {
			StringBuilder sb = new StringBuilder("*".equals(qs) ? "" : "(".concat(qs).concat(") AND "));
			if (tag.startsWith("groups:")) {
				sb.append("properties.").append(tag);
			} else {
				sb.append(Config._TAGS).append(":(\"").append(tag).append("\")");
			}
			qs = sb.toString();
		}
		return qs;
	}

	/**
	 * Legacy people-finder: matches a single user by display name or email, scoped
	 * to the request's space.
	 */
	public String getUserSearchQuery(String qs, HttpServletRequest req) {
		String spaceFilter = getUsersSpaceFilter(req);
		qs = Utils.stripAndTrim(qs).toLowerCase();
		if (!StringUtils.isBlank(qs)) {
			String wildcardLower = qs.matches("[\\p{IsAlphabetic}]*") ? qs + "*" : qs;
			String template = "((type:profile AND (name:({1}) OR name:({2}) OR properties.originalName:({1}) OR "
					+ "properties.originalName:({2}))) OR (type:user AND email:({0}*)))";
			if (qs.contains(" ")) {
				template = "(type:profile AND (name:(\"{0}\") OR name:(\"{1}\") OR properties.originalName:(\"{0}\") OR "
					+ "properties.originalName:(\"{1}\")))";
			}
			qs = (StringUtils.isBlank(spaceFilter) ? "" : spaceFilter + " AND ")
					+ Utils.formatMessage(template, qs, WordUtils.capitalize(qs), wildcardLower);
		} else {
			qs = StringUtils.isBlank(spaceFilter) ? "*" : spaceFilter;
		}
		return qs;
	}

	/**
	 * Users list template for a specific user + space (used by chat apps).
	 * @param qs the raw search term
	 * @param authUser the space filter is resolved against this user's space access
	 * @param space the space id to scope the search to
	 */
	public String getUsersSearchQuery(String qs, Profile authUser, String space) {
		String spaceFilter = sanitizeQueryString("", authUser, space).
					queryString().replaceAll("properties\\.space:", "properties.spaces:");
		return getUsersSearchQuery(qs, spaceFilter);
	}

	/**
	 * Users list template for a request's space.
	 * @param qs the raw search term
	 * @param req the HTTP request
	 */
	public String getUsersSearchQuery(String qs, HttpServletRequest req) {
		return getUsersSearchQuery(qs, getUsersSpaceFilter(req));
	}

	private String getUsersSearchQuery(String qs, String spaceFilter) {
		qs = Utils.stripAndTrim(qs).toLowerCase();
		if (!StringUtils.isBlank(qs)) {
			String wildcardLower = qs.matches("[\\p{IsAlphabetic}]*") ? qs + "*" : qs;
			String wildcardUpper = StringUtils.capitalize(wildcardLower);
			String template = "(name:({1}) OR name:({2} OR properties.originalName:{1} OR properties.originalName:{2} OR {3}) "
					+ "OR properties.location:({0}) OR properties.aboutme:({0}) OR properties.groups:({0}))";
			return (StringUtils.isBlank(spaceFilter) ? "" : spaceFilter + " AND ") +
					Utils.formatMessage(template, qs, StringUtils.capitalize(qs), wildcardLower, wildcardUpper);
		}
		return StringUtils.isBlank(spaceFilter) ? "*" : spaceFilter;
	}

	/**
	 * Searches questions, sticky posts, replies and comments. Simple term queries
	 * are boosted against title/body/tags; the space-filter prefix from the
	 * sanitized query is preserved.
	 */
	public List<Post> fullQuestionsSearch(SanitizedQuery sq, Pager... pager) {
		String typeFilter = Config._TYPE + ":(" + String.join(" OR ",
						Utils.type(Question.class), Utils.type(Sticky.class),
						Utils.type(Reply.class), Utils.type(Comment.class)) + ")";
		String qs;
		if (sq.isBrowseAll()) {
			// "browse all" - no meaningful relevance, so sort by newest-first
			// (the old default) rather than by (equal) score.
			qs = typeFilter;
			if (pager != null && pager.length > 0) {
				Pager p = pager[0];
				if (p != null && StringUtils.isBlank(p.getSortby())) {
					p.setSortby(Config._TIMESTAMP);
				}
			}
		} else if (sq.isSimpleTermQuery()) {
			// Boost question title matches above body and untargeted matches so
			// that a post whose title IS the search query ranks first, rather
			// than being buried among docs that merely mention the words. The
			// three-way OR preserves recall (any-field matches are still
			// included) while title matches get a 3x relevance boost and body
			// matches a 2x boost. The space-filter prefix from the SanitizedQuery
			// record is preserved.
			String boostPhrase = sq.query();
			String spacePrefix = StringUtils.isBlank(sq.queryFilter()) || "*".equals(sq.queryFilter()) ?
					null : sq.queryFilter();
			String boostClause = "(properties.title:" + boostPhrase + "^3"
					+ " OR properties.body:" + boostPhrase + "^2"
					+ " OR tags:" + boostPhrase + "^2"
					+ " OR " + boostPhrase + ")";
			qs = (spacePrefix == null ? boostClause
					: spacePrefix + " AND " + boostClause) + " AND " + typeFilter;
		} else {
			qs = sq.queryString() + " AND " + typeFilter;
		}
		List<ParaObject> mixedResults = paraClient().findQuery("", qs, pager);
		Predicate<ParaObject> isQuestion = obj ->
				obj.getType().equals(Utils.type(Question.class)) || obj.getType().equals(Utils.type(Sticky.class));

		// mixedResults arrive in relevance (score) order from the search backend.
		// Keep them in a LinkedHashMap so that iterating its values() later does
		// NOT scramble the score ordering the way a HashMap iteration would.
		Map<String, ParaObject> idsToQuestions = new LinkedHashMap<>(mixedResults.stream().filter(isQuestion).
				collect(Collectors.toMap(x -> x.getId(), x -> x, (a, b) -> a, LinkedHashMap::new)));
		Set<String> toRead = new LinkedHashSet<>();
		mixedResults.stream().filter(isQuestion.negate()).forEach(obj -> {
			if (!idsToQuestions.containsKey(obj.getParentid())) {
				toRead.add(obj.getParentid());
			}
		});
		// find all parent posts but this excludes parents of parents - i.e. won't work for comments in answers
		List<Post> parentPostsLevel1 = paraClient().readAll(new ArrayList<>(toRead));
		parentPostsLevel1.stream().filter(isQuestion).forEach(x -> idsToQuestions.put(x.getId(), x));

		toRead.clear();

		// read parents of parents if any
		parentPostsLevel1.stream().filter(isQuestion.negate()).forEach(obj -> {
			if (!idsToQuestions.containsKey(obj.getParentid())) {
				toRead.add(obj.getParentid());
			}
		});
		List<Post> parentPostsLevel2 = paraClient().readAll(new ArrayList<>(toRead));
		parentPostsLevel2.forEach(x -> idsToQuestions.put(x.getId(), x));

		ArrayList<Post> results = new ArrayList<>(idsToQuestions.size());
		for (ParaObject result : idsToQuestions.values()) {
			if (result instanceof Post) {
				results.add((Post) result);
			}
		}
		return results;
	}

	/**
	 * A sanitized search query, split into the space-filter prefix (queryFilter)
	 * and the sanitized core query. The core may be a "simple" term query
	 * (single word or quoted phrase) eligible for title/body relevance boosting.
	 */
	public record SanitizedQuery(String queryFilter, String query) {

		/**
		 * Reconstructs the full query string sent to the search backend - exactly
		 * what the old sanitizeQueryString returned: an optional space filter
		 * AND-ed with the parenthesised core query.
		 * @return the composed query string
		 */
		public String queryString() {
			if (StringUtils.isBlank(queryFilter)) {
				return "";
			} else if ("*".equals(queryFilter)) {
				return query;
			} else if ("*".equals(query)) {
				return queryFilter;
			} else {
				return StringUtils.isBlank(query) ? queryFilter : queryFilter + " AND (" + query + ")";
			}
		}

		/**
		 * @return true when the query matches everything ("browse all")
		 */
		public boolean isBrowseAll() {
			if (StringUtils.isBlank(queryFilter)) {
				return true;
			}
			return "*".equals(queryFilter) && (StringUtils.isBlank(query) || query.startsWith("*"));
		}

		/**
		 * Checks whether a query string is a "simple" search query - i.e. a single or multi word or a phrase-quoted
		 * multi-word string - that can be safely boosted by targeting the {@code properties.title} and
		 * {@code properties.body} fields.
		 * @param q the sanitized query string
		 * @return true if the query is a single word or a quoted phrase
		 */
		public boolean isSimpleTermQuery() {
			if (StringUtils.isBlank(query) || Strings.CS.containsAny(query, ":", " AND ", " OR ", " NOT ", "(")) {
				return false;
			}
			return !query.contains(" ") || query.startsWith("\"") || query.matches("[\\w\\s]+");
		}
	}
}
