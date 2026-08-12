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
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import static com.erudika.scoold.utils.ScooldRequestInterceptor.logger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Computes the bounded, read-only data set rendered by the admin dashboard.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Component
public class DashboardService {

	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private static final long DAY = TimeUnit.DAYS.toMillis(1);
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d");
	private static final DateTimeFormatter DATE_FULL = DateTimeFormatter.ofPattern("MMM d, yyyy");
	private static final int CACHE_MINUTES = 2;
	private static final String ACTIVITY_TYPE = "scooldactivity";
	private static final String TRAFFIC_TYPE = "scooldtraffic";
	private static final String VISIT_COOKIE = "scooldvisit";
	private static final long FLUSH_MINUTES = 5;
	private static final ZoneId ZONE = ZoneId.systemDefault();
	private final Map<LocalDate, LongAdder> visits = new ConcurrentHashMap<>();
	private final Map<String, CachedDashboard> cache = new ConcurrentHashMap<>();
	private long lastCountTimestamp;

	private final ScooldUtils utils;
	private final ParaClient pc;
	private final Map<LocalDate, LongAdder> views = new ConcurrentHashMap<>();
	private final Map<LocalDate, Set<String>> dailyUsers = new ConcurrentHashMap<>();
	private final Map<LocalDate, Map<String, LongAdder>> userActivity = new ConcurrentHashMap<>();

	public DashboardService(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@EventListener(ContextClosedEvent.class)
	public void stop() {
		flushTrackerSafely();
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

	/**
	 * Records various events for the security audit log.
	 * @param handler method handler
	 * @param request request
	 */
	public void trackActivityIfAllowed(Object handler, HttpServletRequest request) {
		if (CONF.activityTrackingEnabled() && handler instanceof HandlerMethod && isTrackedMethod(request)) {
			try {
				Profile user = utils.getAuthUser(request);
				Sysprop activity = new Sysprop();
				activity.setType(ACTIVITY_TYPE);
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

	/**
	 * Records one visit when this request rendered an HTML page for a new daily visitor.
	 * @param handler method handler
	 * @param req request
	 * @param res response
	 * @param modelAndView model
	 */
	public void trackPageView(Object handler, HttpServletRequest req, HttpServletResponse res,
			ModelAndView modelAndView) {
		if (!isEligiblePageView(handler, req, modelAndView)) {
			return;
		}
		if (Utils.timestamp() - lastCountTimestamp > TimeUnit.MINUTES.toMillis(FLUSH_MINUTES)) {
			lastCountTimestamp = Utils.timestamp();
			flushTrackerSafely();
		}
		LocalDate today = LocalDate.now(ZONE);
		Profile user = utils.getAuthUser(req);
		if (user != null && !isStaff(user)
				&& dailyUsers.computeIfAbsent(today, ignored -> ConcurrentHashMap.newKeySet()).add(user.getId())) {
			userActivity.computeIfAbsent(today, ignored -> new ConcurrentHashMap<>())
				.computeIfAbsent(user.getId(), ignored -> new LongAdder()).increment();
		}
		if (today.toString().equals(HttpUtils.getStateParam(VISIT_COOKIE, req))) {
			return;
		}
		visits.computeIfAbsent(today, ignored -> new LongAdder()).increment();
		long secondsUntilMidnight = Math.max(1, Duration.between(
				java.time.ZonedDateTime.now(ZONE), today.plusDays(1).atStartOfDay(ZONE)).getSeconds());
		HttpUtils.setRawCookie(VISIT_COOKIE, today.toString(), req, res, "Lax", (int) Math.min(Integer.MAX_VALUE,
				secondsUntilMidnight));
	}

	public void recordQuestionView() {
		views.computeIfAbsent(LocalDate.now(ZONE), ignored -> new LongAdder()).increment();
	}

	public void recordContribution(Profile user) {
		if (user == null || isStaff(user)) {
			return;
		}
		userActivity.computeIfAbsent(LocalDate.now(ZONE), ignored -> new ConcurrentHashMap<>())
				.computeIfAbsent(user.getId(), ignored -> new LongAdder()).increment();
	}

	public List<ParaObject> getAuditLog(Pager pager) {
		return pc.findQuery(ACTIVITY_TYPE, "*", pager);
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
		long periodViews = trafficTotal("views_", start, now);

		summary.put("questions", metric(questions, delta(questions, previousQuestions)));
		summary.put("answers", metric(answers, delta(answers, previousAnswers)));
		summary.put("users", metric(users, delta(users, previousUsers)));
		summary.put("views", metric(periodViews, 0));
		summary.put("clickRate", metric(formatNumber(questions == 0 ? 0 : (double) periodViews / questions), 0));
		summary.put("abandonment", metric(String.valueOf(
				totalQuestions == 0 ? 0 : Math.round(abandoned * 10000.0 / totalQuestions) / 100.0), 0));
		data.put("summary", summary);
		data.put("period", period);
		data.put("questionsTrend", trend(Question.class, start, now));
		data.put("answersTrend", trend(Reply.class, start, now));
		data.put("trafficTrend", trafficTrend(start, now));
//		data.put("reputationTrend", reputationTrend(start, now));
		data.put("tags", topTags());
		data.put("contributors", topContributors(start, now));
		data.put("trending", trendingQuestions());
		data.put("oldestUnanswered", oldestUnanswered());
		data.put("leaderboard", leaderboard(start, now, period));
		data.put("staff", staff());
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
		return count(type, start, end);
	}

	private long countRange(String type, long start, long end) {
		return countQuery(type, Config._TIMESTAMP + ":[" + start + " TO " + end + "]");
	}

	private long countQuery(String type, String query) {
		Pager pager = new Pager(1);
		pc.findQuery(type, query, pager);
		return pager.getCount();
	}

	private List<Map<String, Object>> trend(Class<? extends ParaObject> type, long start, long end) {
		List<ParaObject> objects = new LinkedList<>();
		objects.addAll(fetch(Utils.type(type), start, end, 300));
		return bucket(objects);
	}

	private List<Map<String, Object>> trafficTrend(long start, long end) {
		if (!CONF.activityTrackingEnabled()) {
			return List.of();
		}
		Map<LocalDate, Long> counts = trafficCounts("day_", start, end);
		return counts.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.map(entry -> Map.<String, Object>of("label", dateLabel(entry.getKey().atStartOfDay(ZoneId.systemDefault())
						.toInstant().toEpochMilli()), "value", entry.getValue())).toList();
	}

	private long trafficTotal(String prefix, long start, long end) {
		return trafficCounts(prefix, start, end).values().stream().mapToLong(Long::longValue).sum();
	}

	private Map<LocalDate, Long> trafficCounts(String prefix, long start, long end) {
		LocalDate from = Instant.ofEpochMilli(start).atZone(ZONE).toLocalDate();
		LocalDate to = Instant.ofEpochMilli(end).atZone(ZONE).toLocalDate();
		Map<LocalDate, Long> counts = new LinkedHashMap<>();
		for (YearMonth month = YearMonth.from(from); !month.isAfter(YearMonth.from(to)); month = month.plusMonths(1)) {
			Sysprop traffic = pc.read(trafficId(month));
			if (traffic == null) {
				continue;
			}
			traffic.getProperties().forEach((key, value) -> {
				if (key.startsWith(prefix) && value instanceof Number) {
					LocalDate date = LocalDate.parse(key.substring(prefix.length()));
					if (!date.isBefore(from) && !date.isAfter(to)) {
						counts.merge(date, ((Number) value).longValue(), Long::sum);
					}
				}
			});
		}
		Map<LocalDate, Long> pending = "views_".equals(prefix) ? pendingCounters(views) : pendingCounters(visits);
		pending.forEach((date, count) -> {
			if (!date.isBefore(from) && !date.isAfter(to)) {
				counts.merge(date, count, Long::sum);
			}
		});
		return counts;
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
//		List<Map<String, Object>> result = new LinkedList<>();
//		points.forEach((label, value) -> result.add(Map.of("label", label, "value", value)));
//		return result;
//	}

	private List<ParaObject> fetch(String type, long start, long end, int limit) {
		Pager pager = new Pager(limit > 500 ? 500 : limit);
		pager.setSortby("votes");
		pager.setDesc(false);
		return pc.findQuery(type, Config._TIMESTAMP + ":[" + start + " TO " + end + "]", pager);
	}

	private List<Map<String, Object>> bucket(List<ParaObject> objects) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		objects.stream().sorted(Comparator.comparing(ParaObject::getTimestamp, Comparator.nullsLast(Long::compareTo)))
				.forEach(object -> counts.merge(dateLabel(object.getTimestamp()), 1, Integer::sum));
		List<Map<String, Object>> result = new LinkedList<>();
		counts.forEach((label, value) -> result.add(Map.of("label", label, "value", value)));
		return result;
	}

	private List<Map<String, Object>> topTags() {
		Pager pager = new Pager(10);
		pager.setSortby("count");
		List<Tag> tags = pc.findQuery(Utils.type(Tag.class), "*", pager);
		List<Map<String, Object>> result = new LinkedList<>();
		for (Tag tag : tags) {
			result.add(Map.of("name", tag.getTag(), "value", Optional.ofNullable(tag.getCount()).orElse(0)));
		}
		return result;
	}

	private List<Map<String, Object>> trendingQuestions() {
		Pager pager = new Pager(7);
		pager.setSortby("properties.viewcount");
		List<Question> questions = pc.findQuery(Utils.type(Question.class), "*", pager);
		List<Map<String, Object>> result = new LinkedList<>();
		for (Question question : questions) {
			result.add(Map.of("id", question.getId(), "title", question.getTitle(),
					"views", Optional.ofNullable(question.getViewcount()).orElse(0L),
					"answers", Optional.ofNullable(question.getAnswercount()).orElse(0L)));
		}
		return result;
	}

	private List<Map<String, Object>> oldestUnanswered() {
		Pager pager = new Pager(10);
		pager.setSortby(Config._TIMESTAMP);
		pager.setDesc(false);
		List<Question> questions = pc.findQuery(Utils.type(Question.class), "properties.answercount:0", pager);
		List<Map<String, Object>> result = new LinkedList<>();
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

	private List<Map<String, Object>> leaderboard(long start, long end, String period) {
		if (!CONF.activityTrackingEnabled()) {
			return List.of();
		}
		Map<String, Integer> counts = new HashMap<>();
		trafficUserActivity(start, end).forEach((id, count) -> counts.merge(id, count.intValue(), Integer::sum));
		List<Map<String, Object>> result = new LinkedList<>();
		if (counts.isEmpty()) {
			getTopMembersForPeriod(result, period);
		} else {
			counts.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(10)
					.forEach(entry -> {
						Map<String, Object> row = new LinkedHashMap<>();
						row.put("id", entry.getKey());
						row.put("visits", entry.getValue());
						result.add(row);
					});
			Map<String, String> names = userNames(result.stream().map(row -> (String) row.get("id"))
					.filter(Objects::nonNull).collect(Collectors.toList()));
			result.forEach(row -> row.put("name", names.getOrDefault(row.get("id"), (String) row.get("id"))));
		}
		return result;
	}

	private void getTopMembersForPeriod(List<Map<String, Object>> result, String period) {
		Pager pager = new Pager(10);
		pager.setSortby(reputationField(period));
		pager.setDesc(true);
		List<Profile> profiles = pc.findQuery(Utils.type(Profile.class), "*", pager);
		for (Profile profile : profiles) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("id", profile.getCreatorid());
			row.put("name", profile.getName());
			row.put("visits", reputation(profile, period));
			result.add(row);
		}
	}

	private String reputationField(String period) {
		return switch (period) {
			case "7d" -> "properties.weeklyVotes";
			case "12w" -> "properties.quarterlyVotes";
			case "12m" -> "properties.yearlyVotes";
			default -> "properties.monthlyVotes";
		};
	}

	private int reputation(Profile profile, String period) {
		return switch (period) {
			case "7d" -> Optional.ofNullable(profile.getWeeklyVotes()).orElse(0);
			case "12w" -> Optional.ofNullable(profile.getQuarterlyVotes()).orElse(0);
			case "12m" -> Optional.ofNullable(profile.getYearlyVotes()).orElse(0);
			default -> Optional.ofNullable(profile.getMonthlyVotes()).orElse(0);
		};
	}

	private Map<String, Long> trafficUserActivity(long start, long end) {
		LocalDate from = Instant.ofEpochMilli(start).atZone(ZONE).toLocalDate();
		LocalDate to = Instant.ofEpochMilli(end).atZone(ZONE).toLocalDate();
		Map<String, Long> counts = new HashMap<>();
		for (YearMonth month = YearMonth.from(from); !month.isAfter(YearMonth.from(to)); month = month.plusMonths(1)) {
			Sysprop traffic = pc.read(trafficId(month));
			if (traffic == null) {
				continue;
			}
			traffic.getProperties().forEach((key, value) -> {
				if (key.startsWith("users_") && value instanceof Map<?, ?>) {
					LocalDate date = LocalDate.parse(key.substring("users_".length()));
					if (!date.isBefore(from) && !date.isAfter(to)) {
						mergeUserCounts(counts, (Map<?, ?>) value);
					}
				}
			});
		}
		userActivity.forEach((date, users) -> {
			if (!date.isBefore(from) && !date.isAfter(to)) {
				users.forEach((id, count) -> counts.merge(id, count.sum(), Long::sum));
			}
		});
		return counts;
	}

	private void mergeUserCounts(Map<String, Long> counts, Map<?, ?> users) {
		users.forEach((id, count) -> {
			if (id instanceof String && count instanceof Number) {
				counts.merge((String) id, ((Number) count).longValue(), Long::sum);
			}
		});
	}

	private Map<String, Object> staff() {
		Pager pager = new Pager(10);
		pager.setSortby("votes");
		List<Profile> profiles = pc.findQuery(Utils.type(Profile.class), "properties.groups:(admins OR mods)", pager);
		return Map.of("members", profiles, "hasMore", pager.getCount() > pager.getLimit());
	}

	private boolean isStaff(Profile profile) {
		return profile != null && (utils.isAdmin(profile) || utils.isModAnywhere(profile));
	}

	private List<Map<String, Object>> topContributors(long start, long end) {
		Map<String, Integer> counts = new HashMap<>();
		for (ParaObject object : fetch(Utils.type(Question.class), start, end, 300)) {
			counts.merge(object.getCreatorid(), 1, Integer::sum);
		}
		for (ParaObject object : fetch(Utils.type(Comment.class), start, end, 300)) {
			counts.merge(object.getCreatorid(), 1, Integer::sum);
		}
		for (ParaObject object : fetch(Utils.type(Reply.class), start, end, 300)) {
			counts.merge(object.getCreatorid(), 1, Integer::sum);
		}
		List<Map<String, Object>> result = new LinkedList<>();
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


	private boolean isEligiblePageView(Object handler, HttpServletRequest req, ModelAndView modelAndView) {
		String path = req.getServletPath();
		String accept = StringUtils.defaultString(req.getHeader("Accept"));
		return CONF.activityTrackingEnabled() && handler instanceof HandlerMethod
				&& "GET".equalsIgnoreCase(req.getMethod()) && modelAndView != null
				&& !Strings.CI.startsWith(modelAndView.getViewName(), "redirect:")
				&& !utils.isApiRequest(req) && !utils.isAjaxRequest(req)
				&& Strings.CI.contains(accept, "text/html")
				&& !Strings.CS.startsWithAny(path, "/admin", "/signin");
	}

	private void flushTrackerSafely() {
		TrafficPending pending = drain();
		if (pending.isEmpty()) {
			return;
		}
		flush(pending);
	}

	private TrafficPending drain() {
		Map<LocalDate, Long> visitCounts = drainCounters(visits);
		Map<LocalDate, Long> viewCounts = drainCounters(views);
		Map<LocalDate, Map<String, Long>> userCounts = new HashMap<>();
		userActivity.forEach((date, users) -> {
			Map<String, Long> counts = new HashMap<>();
			users.forEach((id, counter) -> {
				long count = counter.sumThenReset();
				if (count > 0) {
					counts.put(id, count);
				}
			});
			if (!counts.isEmpty()) {
				userCounts.put(date, counts);
			}
		});
		return new TrafficPending(visitCounts, viewCounts, userCounts);
	}

	private Map<LocalDate, Long> drainCounters(Map<LocalDate, LongAdder> counters) {
		Map<LocalDate, Long> result = new HashMap<>();
		counters.forEach((date, counter) -> {
			long count = counter.sumThenReset();
			if (count > 0) {
				result.put(date, count);
			}
		});
		return result;
	}

	private void flush(TrafficPending pending) {
		Set<YearMonth> months = new HashSet<>();
		pending.visits.keySet().forEach(date -> months.add(YearMonth.from(date)));
		pending.views.keySet().forEach(date -> months.add(YearMonth.from(date)));
		pending.users.keySet().forEach(date -> months.add(YearMonth.from(date)));
		months.forEach(month -> {
			try {
				String id = trafficId(month);
				Sysprop traffic = pc.read(id);
				boolean newObject = traffic == null;
				if (traffic == null) {
					traffic = new Sysprop(id);
					traffic.setType(TRAFFIC_TYPE);
				}
				for (Map.Entry<LocalDate, Long> entry : pending.visits.entrySet()) {
					if (YearMonth.from(entry.getKey()).equals(month)) {
						addCounter(traffic, "day_" + entry.getKey(), entry.getValue());
					}
				}
				for (Map.Entry<LocalDate, Long> entry : pending.views.entrySet()) {
					if (YearMonth.from(entry.getKey()).equals(month)) {
						addCounter(traffic, "views_" + entry.getKey(), entry.getValue());
					}
				}
				for (Map.Entry<LocalDate, Map<String, Long>> entry : pending.users.entrySet()) {
					if (YearMonth.from(entry.getKey()).equals(month)) {
						String key = "users_" + entry.getKey();
						Map<String, Object> stored = new HashMap<>();
						Object value = traffic.getProperty(key);
						if (value instanceof Map<?, ?>) {
							((Map<?, ?>) value).forEach((userId, count) -> {
								if (userId instanceof String && count instanceof Number) {
									stored.put((String) userId, ((Number) count).longValue());
								}
							});
						}
						entry.getValue().forEach((userId, count) -> {
							long existing = stored.get(userId) instanceof Number ? ((Number) stored.get(userId)).longValue() : 0;
							stored.put(userId, existing + count);
						});
						traffic.addProperty(key, stored);
					}
				}
				traffic.addProperty("updated", System.currentTimeMillis());
				if (newObject) {
					pc.create(traffic);
				} else {
					pc.update(traffic);
				}
			} catch (Exception ex) {
				pending.visits.forEach((date, count) -> {
					if (YearMonth.from(date).equals(month)) {
						visits.computeIfAbsent(date, ignored -> new LongAdder()).add(count);
					}
				});
				pending.views.forEach((date, count) -> {
					if (YearMonth.from(date).equals(month)) {
						views.computeIfAbsent(date, ignored -> new LongAdder()).add(count);
					}
				});
				pending.users.forEach((date, users) -> {
					if (YearMonth.from(date).equals(month)) {
						users.forEach((id, count) -> userActivity.computeIfAbsent(date, ignored -> new ConcurrentHashMap<>())
								.computeIfAbsent(id, ignored -> new LongAdder()).add(count));
					}
				});
				ScooldRequestInterceptor.logger.warn("Unable to flush dashboard traffic.", ex);
			}
		});
	}

	private void addCounter(Sysprop traffic, String key, long amount) {
		Object value = traffic.getProperty(key);
		long existing = value instanceof Number ? ((Number) value).longValue() : 0;
		traffic.addProperty(key, existing + amount);
	}

	static String trafficId(YearMonth month) {
		return "visits_" + month.getMonthValue() + "_" + month.getYear();
	}

	private Map<LocalDate, Long> pendingCounters(Map<LocalDate, LongAdder> counters) {
		Map<LocalDate, Long> pending = new HashMap<>();
		counters.forEach((date, counter) -> pending.put(date, counter.sum()));
		return pending;
	}

	private record TrafficPending(Map<LocalDate, Long> visits, Map<LocalDate, Long> views,
			Map<LocalDate, Map<String, Long>> users) {
		private boolean isEmpty() {
			return visits.isEmpty() && views.isEmpty() && users.isEmpty();
		}
	}

	private record CachedDashboard(long created, Map<String, Object> data) { }
}
