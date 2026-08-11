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
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.Tag;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import static com.erudika.scoold.utils.ScooldRequestInterceptor.logger;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

/** Computes the bounded, read-only data set rendered by the admin dashboard. */
@Component
public class DashboardService {

	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private static final long DAY = TimeUnit.DAYS.toMillis(1);
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d");
	private static final DateTimeFormatter DATE_FULL = DateTimeFormatter.ofPattern("MMM d, yyyy");
	private static final int CACHE_MINUTES = 2;
	private final ScooldUtils utils;
	private final ParaClient pc;
	private final Map<String, CachedDashboard> cache = new ConcurrentHashMap<>();

	public DashboardService(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	public Map<String, Object> getDashboard(String period) {
		String normalized = normalizePeriod(period);
		CachedDashboard cached = cache.get(normalized);
		long ttl = TimeUnit.MINUTES.toMillis(CACHE_MINUTES);
		if (cached != null && cached.created + ttl > System.currentTimeMillis()) {
			return cached.data;
		}
		Map<String, Object> data = buildDashboard(normalized);
		cache.put(normalized, new CachedDashboard(System.currentTimeMillis(), data));
		return data;
	}

	public void clearCache() {
		cache.clear();
	}

	public void trackActivityIfAllowed(Object handler, HttpServletRequest request) {
		if (CONF.activityTrackingEnabled() && handler instanceof HandlerMethod && isTrackedMethod(request)) {
			try {
				Profile user = utils.getAuthUser(request);
				Sysprop activity = new Sysprop();
				activity.setType("scooldactivity");
				activity.setName(user == null ? "Anonymous" : user.getName());
				activity.setCreatorid(user == null ? "anonymous" : user.getId());
				activity.addProperty("method", request.getMethod());
				activity.addProperty("ip", HttpUtils.getClientIp(request));
				String query = request.getQueryString();
				activity.addProperty("path", request.getRequestURI() + (StringUtils.isBlank(query) ? "" : "?" + query));
				utils.getParaClient().createAsync(activity);
			} catch (Exception ex) {
				logger.debug("Unable to record dashboard activity.", ex);
			}
		}
	}

	private boolean isTrackedMethod(HttpServletRequest request) {
		return "POST".equalsIgnoreCase(request.getMethod())
				|| "PUT".equalsIgnoreCase(request.getMethod())
				|| "PATCH".equalsIgnoreCase(request.getMethod())
				|| "DELETE".equalsIgnoreCase(request.getMethod());
	}

	private Map<String, Object> buildDashboard(String period) {
		long now = System.currentTimeMillis();
		long start = now - periodDays(period) * DAY;
		long previousStart = start - periodDays(period) * DAY;
		Map<String, Object> data = new LinkedHashMap<>();
		Map<String, Object> summary = new LinkedHashMap<>();
		long questions = countPosts(Question.class, start, now);
		long previousQuestions = countPosts(Question.class, previousStart, start);
		long answers = count(Reply.class, start, now);
		long previousAnswers = count(Reply.class, previousStart, start);
		long users = countRange("profile", start, now);
		long previousUsers = countRange("profile", previousStart, start);
		long totalQuestions = count(Question.class);
		long abandoned = countQuery(Utils.type(Question.class), "properties.answercount:0");

		summary.put("questions", metric(questions, delta(questions, previousQuestions)));
		summary.put("answers", metric(answers, delta(answers, previousAnswers)));
		summary.put("users", metric(users, delta(users, previousUsers)));
		summary.put("views", metric(totalViews(), 0));
		summary.put("averageAge", metric(formatDuration(averageQuestionAge()), 0));
		summary.put("clickRate", metric(formatNumber(averageQuestionViews()), 0));
		summary.put("abandonment", metric(String.valueOf(
				totalQuestions == 0 ? 0 : Math.round(abandoned * 10000.0 / totalQuestions) / 100.0), 0));
		data.put("summary", summary);
		data.put("period", period);
		data.put("questionsTrend", trend(Question.class, start, now));
		data.put("answersTrend", trend(Reply.class, start, now));
		data.put("trafficTrend", activityTrend(start, now));
//		data.put("reputationTrend", reputationTrend(start, now));
		data.put("tags", topTags());
		data.put("contributors", topContributors(start, now));
		data.put("trending", trendingQuestions());
		data.put("oldestUnanswered", oldestUnanswered());
		data.put("leaderboard", leaderboard(start, now));
		return data;
	}

	private Map<String, Object> metric(long value, double change) {
		Map<String, Object> metric = new LinkedHashMap<>();
		metric.put("value", value);
		metric.put("change", change);
		return metric;
	}

	private Map<String, Object> metric(String value, double change) {
		Map<String, Object> metric = new LinkedHashMap<>();
		metric.put("value", value);
		metric.put("change", change);
		return metric;
	}

	private long count(Class<? extends ParaObject> type) {
		return pc.getCount(Utils.type(type));
	}

	private long count(Class<? extends ParaObject> type, long start, long end) {
		return countRange(Utils.type(type), start, end);
	}

	private long countPosts(Class<? extends ParaObject> type, long start, long end) {
		return count(type, start, end) + countRange("sticky", start, end);
	}

	private long countRange(String type, long start, long end) {
		return countQuery(type, Config._TIMESTAMP + ":[" + start + " TO " + end + "]");
	}

	private long countQuery(String type, String query) {
		Pager pager = new Pager(1);
		pager.setLimit(1);
		pc.findQuery(type, query, pager);
		return pager.getCount();
	}

	private List<Map<String, Object>> trend(Class<? extends ParaObject> type, long start, long end) {
		List<ParaObject> objects = new ArrayList<>();
		objects.addAll(fetch(Utils.type(type), start, end, 2000));
		if (type == Question.class) {
			objects.addAll(fetch("sticky", start, end, 500));
		}
		return bucket(objects);
	}

	private List<Map<String, Object>> activityTrend(long start, long end) {
		if (!CONF.activityTrackingEnabled()) {
			return List.of();
		}
		return bucket(fetch("scooldactivity", start, end, 5000));
	}

//	private List<Map<String, Object>> reputationTrend(long start, long end) {
//		List<ParaObject> votes = fetch(Utils.type(Vote.class), start, end, 2000);
//		Map<String, Integer> points = new LinkedHashMap<>();
//		for (ParaObject object : votes) {
//			Vote vote = (Vote) object;
//			String label = dateLabel(vote.getTimestamp());
//			int reward = vote.isUpvote() ? CONF.voteupRewardAuthor() : -CONF.postVotedownPenaltyAuthor();
//			points.merge(label, reward, Integer::sum);
//		}
//		List<Map<String, Object>> result = new ArrayList<>();
//		points.forEach((label, value) -> result.add(Map.of("label", label, "value", value)));
//		return result;
//	}

	private List<ParaObject> fetch(String type, long start, long end, int limit) {
		Pager pager = new Pager(1);
		pager.setLimit(limit);
		pager.setSortby(Config._TIMESTAMP);
		pager.setDesc(false);
		return pc.findQuery(type, Config._TIMESTAMP + ":[" + start + " TO " + end + "]", pager);
	}

	private List<Map<String, Object>> bucket(List<ParaObject> objects) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		objects.stream().sorted(Comparator.comparing(ParaObject::getTimestamp, Comparator.nullsLast(Long::compareTo)))
				.forEach(object -> counts.merge(dateLabel(object.getTimestamp()), 1, Integer::sum));
		List<Map<String, Object>> result = new ArrayList<>();
		counts.forEach((label, value) -> result.add(Map.of("label", label, "value", value)));
		return result;
	}

	private List<Map<String, Object>> topTags() {
		Pager pager = new Pager(1);
		pager.setLimit(10);
		pager.setSortby("properties.count");
		List<Tag> tags = pc.findQuery(Utils.type(Tag.class), "*", pager);
		List<Map<String, Object>> result = new ArrayList<>();
		for (Tag tag : tags) {
			result.add(Map.of("name", tag.getTag(), "value", Optional.ofNullable(tag.getCount()).orElse(0)));
		}
		return result;
	}

	private List<Map<String, Object>> trendingQuestions() {
		Pager pager = new Pager(1);
		pager.setLimit(6);
		pager.setSortby("properties.viewcount");
		List<Question> questions = pc.findQuery(Utils.type(Question.class), "*", pager);
		List<Map<String, Object>> result = new ArrayList<>();
		for (Question question : questions) {
			result.add(Map.of("id", question.getId(), "title", question.getTitle(),
					"views", Optional.ofNullable(question.getViewcount()).orElse(0L),
					"answers", Optional.ofNullable(question.getAnswercount()).orElse(0L)));
		}
		return result;
	}

	private List<Map<String, Object>> oldestUnanswered() {
		Pager pager = new Pager(1);
		pager.setLimit(10);
		pager.setSortby(Config._TIMESTAMP);
		pager.setDesc(false);
		List<Question> questions = pc.findQuery(Utils.type(Question.class), "properties.answercount:0", pager);
		List<Map<String, Object>> result = new ArrayList<>();
		for (Question question : questions) {
			result.add(Map.of("id", question.getId(), "title", question.getTitle(),
					"date", dateLabelFull(question.getTimestamp())));
		}
		return result;
	}

	private String dateLabelFull(Long timestamp) {
		if (timestamp == null) {
			return "Unknown";
		}
		return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FULL);
	}

	private List<Map<String, Object>> leaderboard(long start, long end) {
		if (!CONF.activityTrackingEnabled()) {
			return List.of();
		}
		Map<String, Map<String, Object>> users = new HashMap<>();
		for (ParaObject object : fetch("scooldactivity", start, end, 5000)) {
			Sysprop activity = (Sysprop) object;
			if ("anonymous".equals(activity.getCreatorid())) {
				continue;
			}
			Map<String, Object> row = users.computeIfAbsent(activity.getCreatorid(), id -> {
				Map<String, Object> value = new LinkedHashMap<>();
				value.put("id", id);
				value.put("name", activity.getName());
				value.put("visits", 0);
				return value;
			});
			row.put("visits", ((Integer) row.get("visits")) + 1);
		}
		return users.values().stream().sorted((a, b) -> Integer.compare((Integer) b.get("visits"), (Integer) a.get("visits")))
				.limit(10).toList();
	}

	private List<Map<String, Object>> topContributors(long start, long end) {
		Map<String, Integer> counts = new HashMap<>();
		for (ParaObject object : fetch(Utils.type(Question.class), start, end, 2000)) {
			counts.merge(object.getCreatorid(), 1, Integer::sum);
		}
		for (ParaObject object : fetch("sticky", start, end, 500)) {
			counts.merge(object.getCreatorid(), 1, Integer::sum);
		}
		for (ParaObject object : fetch(Utils.type(Reply.class), start, end, 2000)) {
			counts.merge(object.getCreatorid(), 1, Integer::sum);
		}
		List<Map<String, Object>> result = new ArrayList<>();
		counts.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(10)
				.forEach(entry -> {
					Map<String, Object> row = new LinkedHashMap<>();
					row.put("id", entry.getKey());
					row.put("posts", entry.getValue());
					result.add(row);
				});
		Map<String, String> names = userNames(result.stream().map(row -> (String) row.get("id"))
				.filter(Objects::nonNull).collect(Collectors.toList()));
		result.forEach(row -> row.put("name", names.getOrDefault(row.get("id"), (String) row.get("id"))));
		return result;
	}

	private Map<String, String> userNames(List<String> ids) {
		Map<String, String> names = new HashMap<>();
		if (!ids.isEmpty()) {
			for (ParaObject user : pc.findByIds(ids)) {
				names.put(user.getId(), user.getName());
			}
		}
		return names;
	}

	private long totalViews() {
		long views = 0;
		Pager pager = new Pager(1);
		pager.setLimit(500);
		for (ParaObject object : pc.findQuery(Utils.type(Question.class), "*", pager)) {
			Question question = (Question) object;
			views += Optional.ofNullable(question.getViewcount()).orElse(0L);
		}
		return views;
	}

	private double averageQuestionViews() {
		Pager pager = new Pager(1);
		pager.setLimit(500);
		List<Question> questions = pc.findQuery(Utils.type(Question.class), "*", pager);
		return questions.isEmpty() ? 0 : questions.stream().mapToLong(q -> Optional.ofNullable(q.getViewcount()).orElse(0L)).average().orElse(0);
	}

	private long averageQuestionAge() {
		Pager pager = new Pager(1);
		pager.setLimit(500);
		List<Question> questions = pc.findQuery(Utils.type(Question.class), "*", pager);
		return Math.round(questions.stream().mapToLong(q -> Math.max(0, System.currentTimeMillis() - Optional.ofNullable(q.getTimestamp()).orElse(System.currentTimeMillis())))
					.average().orElse(0));
	}

	private String formatDuration(long millis) {
		long days = TimeUnit.MILLISECONDS.toDays(millis);
		return days + "d";
	}

	private String formatNumber(double value) {
		return String.format(java.util.Locale.US, "%.1f", value);
	}

	private String dateLabel(Long timestamp) {
		if (timestamp == null) {
			return "Unknown";
		}
		return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().format(DATE);
	}

	public static double delta(long current, long previous) {
		if (previous == 0) {
			return current == 0 ? 0 : 100;
		}
		return Math.round((current - previous) * 10000.0 / previous) / 100.0;
	}

	public static String normalizePeriod(String period) {
		return period != null && List.of("7d", "30d", "12w", "12m").contains(period) ? period : "30d";
	}

	private long periodDays(String period) {
		return switch (period) {
			case "7d" -> 7;
			case "12w" -> 84;
			case "12m" -> 365;
			default -> 30;
		};
	}

	private record CachedDashboard(long created, Map<String, Object> data) { }
}
