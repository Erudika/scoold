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
import com.erudika.para.core.email.Emailer;
import com.erudika.scoold.utils.avatars.AvatarRepositoryProxy;
import com.erudika.scoold.utils.avatars.GravatarAvatarGenerator;
import java.lang.reflect.Method;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class ScooldUtilsSearchTest {

	private ScooldUtils utils;

	@Before
	public void setUp() throws Exception {
		ParaClient pc = mock(ParaClient.class);
		LanguageUtils langutils = mock(LanguageUtils.class);
		Emailer emailer = mock(Emailer.class);
		AvatarRepositoryProxy avatarRepo = mock(AvatarRepositoryProxy.class);
		GravatarAvatarGenerator gravatarGen = mock(GravatarAvatarGenerator.class);
		when(avatarRepo.getAnonymizedLink(anyString())).thenReturn("");
		utils = new ScooldUtils(pc, langutils, emailer, avatarRepo, gravatarGen);
		java.lang.reflect.Field instanceField = ScooldUtils.class.getDeclaredField("instance");
		instanceField.setAccessible(true);
		instanceField.set(null, utils);
	}

	/**
	 * Invokes the private SearchUtils.sanitizeQueryString(String qf, String query)
	 * with a given space-filter string. Common values: "*", "", "properties.space:\"...\"".
	 */
	private String sanitize(String qf, String query) throws Exception {
		Method m = SearchUtils.class.getDeclaredMethod("sanitizeQueryString", String.class, String.class);
		m.setAccessible(true);
		return ((SearchUtils.SanitizedQuery) m.invoke(SearchUtils.getInstance(), qf, query)).queryString();
	}

	@Test
	public void testSanitize_singleWord_notQuoted() throws Exception {
		assertEquals("Post", sanitize("*", "Post"));
		assertEquals("singleword", sanitize("*", "singleword"));
	}

	@Test
	public void testSanitize_multiWord_phraseQuoted() throws Exception {
		assertEquals("\"My Awesome Title\"", sanitize("*", "My Awesome Title"));
		assertEquals("\"hello world\"", sanitize("*", "hello world"));
	}

	@Test
	public void testSanitize_multiWordCaps_phraseQuoted() throws Exception {
		assertEquals("\"MY AWESOME TITLE\"", sanitize("*", "MY AWESOME TITLE"));
	}

	@Test
	public void testSanitize_copiedSentence_phraseQuoted() throws Exception {
		String sentence = "How to resolve errors in your website";
		assertEquals("\"" + sentence + "\"", sanitize("*", sentence));
	}

	@Test
	public void testSanitize_quotedQuery_preserved() throws Exception {
		// already-quoted: not re-wrapped
		assertEquals("\"My Title\"", sanitize("*", "\"My Title\""));
	}

	@Test
	public void testSanitize_luceneSyntaxQuery_preserved() throws Exception {
		// field:syntax preserved (not phrase-quoted — guard sees the ":")
		assertEquals("properties.title:foo", sanitize("*", "properties.title:foo"));
		// AND queries preserved (guard sees " AND ")
		assertEquals("foo AND bar", sanitize("*", "foo AND bar"));
		// OR queries preserved (guard sees " OR ")
		assertEquals("foo OR bar", sanitize("*", "foo OR bar"));
		// grouped queries: bare parens (not preceded by ':') are escaped by the
		// paren-escape regex, and the resulting string still contains '(' which
		// trips the phrase-quote guard, so it is NOT phrase-quoted.
		assertEquals("\\(foo bar\\)", sanitize("*", "(foo bar)"));
	}

	@Test
	public void testSanitize_emptyQuery_returnsDefault() throws Exception {
		// Empty query with qf="*" returns q="" (no sanitization runs, then the
		// `"*".equals(qf)` branch returns q). An empty query + match-all filter
		// yields no results — consistent with the original behavior.
		assertEquals("", sanitize("*", ""));
		assertEquals("", sanitize("*", null));
		assertEquals("", sanitize("*", "   "));
	}

	@Test
	public void testSanitize_starQuery_returnsSpaceFilter() throws Exception {
		assertEquals("properties.space:\"x\"", sanitize("properties.space:\"x\"", "*"));
	}

	@Test
	public void testSanitize_emptyQuery_withSpaceFilter_returnsSpaceFilter() throws Exception {
		assertEquals("properties.space:\"x\"", sanitize("properties.space:\"x\"", ""));
	}

	@Test
	public void testSanitize_emptyQf_returnsDefault() throws Exception {
		// qf.isEmpty() short-circuits to defaultQuery
		assertEquals("", sanitize("", "anything"));
	}

	@Test
	public void testSanitize_spaceFilterJoinedWithPhrase() throws Exception {
		// multi-word query + space filter → qf AND ("phrase")
		assertEquals("properties.space:\"x\" AND (\"My Title\")",
				sanitize("properties.space:\"x\"", "My Title"));
	}

	@Test
	public void testSanitize_spaceFilterJoinedWithSingleWord() throws Exception {
		assertEquals("properties.space:\"x\" AND (Post)",
				sanitize("properties.space:\"x\"", "Post"));
	}

	@Test
	public void testSanitize_trailingWildcard_stripped() throws Exception {
		// single word with trailing * → stripped (validates the previously no-op regex)
		assertEquals("test", sanitize("*", "test*"));
		assertEquals("test", sanitize("*", "test**"));
		assertEquals("foo", sanitize("*", "foo*"));
	}

	@Test
	public void testSanitize_trailingWildcard_multiWord_phraseQuoted() throws Exception {
		// multi-word with trailing * → stripped then phrase-quoted
		assertEquals("\"My Title\"", sanitize("*", "My Title*"));
	}

	@Test
	public void testSanitize_forbiddenChars_stripped() throws Exception {
		// ?, <, > are stripped
		assertEquals("foo", sanitize("*", "fo?o"));
		assertEquals("oo", sanitize("*", "<oo>"));
	}

	@Test
	public void testSanitize_parensInText_escapedNotConsumed() throws Exception {
		// "(test)" should escape the parens to "\(test\)" without dropping
		// the leading character. This validates the data-loss fix.
		// Single word (no space) → not phrase-quoted regardless.
		String result = sanitize("*", "(test)");
		assertEquals("\\(test\\)", result);
	}

	@Test
	public void testSanitize_parensInMultiWordText_notPhraseQuotedBecauseGuardSeesParen() throws Exception {
		// "foo (bar)" → parens escaped to "foo \(bar\)" — the resulting string
		// still contains "(" (as part of "\("), so the phrase-quote guard
		// trips and the query is left un-quoted. Lucene will receive
		// "foo \(bar\)" which it parses as the literal token sequence
		// "foo", "(", "bar", ")" — parens preserved as text, not as a group.
		String result = sanitize("*", "foo (bar)");
		assertEquals("foo \\(bar\\)", result);
		assertFalse("multi-word query with parens should NOT be phrase-quoted",
				result.startsWith("\""));
	}

	@Test
	public void testSanitize_colonFollowedBySpace_escaped() throws Exception {
		// "foo: bar" → "foo\: bar" (escape the colon-space)
		assertEquals("foo\\: bar", sanitize("*", "foo: bar"));
	}

	@Test
	public void testSanitize_realTitle_phraseQuoted() throws Exception {
		String title = "How to find frequently visiting users in scoold";
		assertEquals("\"" + title + "\"", sanitize("*", title));
	}

	@Test
	public void testSanitize_singleWordWithSpace_afterTrimming() throws Exception {
		// leading/trailing whitespace is trimmed, interior whitespace preserved
		assertEquals("\"foo   bar\"", sanitize("*", "  foo   bar  "));
		assertEquals("foo", sanitize("*", "  foo  "));
	}

	// ---- isSimpleSearchQuery ----

	@Test
	public void testIsSimpleQuery_singleWord() {
		assertTrue(new SearchUtils.SanitizedQuery("", "Post").isSimpleTermQuery());
		assertTrue(new SearchUtils.SanitizedQuery("", "hello").isSimpleTermQuery());
	}

	@Test
	public void testIsSimpleQuery_multiWord() {
		assertTrue(new SearchUtils.SanitizedQuery("", "Post Now").isSimpleTermQuery());
		assertTrue(new SearchUtils.SanitizedQuery("", "hello world").isSimpleTermQuery());
	}

	@Test
	public void testIsSimpleQuery_quotedPhrase() {
		assertTrue(new SearchUtils.SanitizedQuery("", "\"My Awesome Title\"").isSimpleTermQuery());
	}

	@Test
	public void testIsSimpleQuery_luceneSyntax_notSimple() {
		assertFalse(new SearchUtils.SanitizedQuery("", "properties.title:foo").isSimpleTermQuery());
		assertFalse(new SearchUtils.SanitizedQuery("", "foo AND bar").isSimpleTermQuery());
		assertFalse(new SearchUtils.SanitizedQuery("", "foo OR bar").isSimpleTermQuery());
		assertFalse(new SearchUtils.SanitizedQuery("", "(foo bar)").isSimpleTermQuery());
	}

	@Test
	public void testIsSimpleQuery_emptyOrBlank_notSimple() {
		assertFalse(new SearchUtils.SanitizedQuery("", "").isSimpleTermQuery());
		assertFalse(new SearchUtils.SanitizedQuery("", null).isSimpleTermQuery());
		assertFalse(new SearchUtils.SanitizedQuery("", "   ").isSimpleTermQuery());
	}

	// ---- fullQuestionsSearch query construction ----

/**
	 * Calls fullQuestionsSearch with a mock ParaClient that captures the query
	 * string passed to findQuery, and returns the captured query for assertion.
	 * Uses a dedicated ScooldUtils instance (set as the global singleton) whose
	 * ParaClient is the mock, so that SearchUtils.getInstance() resolves to it.
	 */
	private String captureSearchQuery(String queryFilter, String query) throws Exception {
		ParaClient pc = mock(ParaClient.class);
		when(pc.findQuery(anyString(), anyString(), any())).thenReturn(java.util.Collections.emptyList());
		when(pc.readAll(anyList())).thenReturn(java.util.Collections.emptyList());
		LanguageUtils langutils = mock(LanguageUtils.class);
		Emailer emailer = mock(Emailer.class);
		AvatarRepositoryProxy avatarRepo = mock(AvatarRepositoryProxy.class);
		GravatarAvatarGenerator gravatarGen = mock(GravatarAvatarGenerator.class);
		when(avatarRepo.getAnonymizedLink(anyString())).thenReturn("");
		ScooldUtils localUtils = new ScooldUtils(pc, langutils, emailer, avatarRepo, gravatarGen);
		java.lang.reflect.Field instanceField = ScooldUtils.class.getDeclaredField("instance");
		instanceField.setAccessible(true);
		instanceField.set(null, localUtils);

		SearchUtils.getInstance().fullQuestionsSearch(new SearchUtils.SanitizedQuery(queryFilter, query));

		org.mockito.ArgumentCaptor<String> typeCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
		org.mockito.ArgumentCaptor<String> queryCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
		verify(pc).findQuery(typeCaptor.capture(), queryCaptor.capture());
		return queryCaptor.getValue();
	}

	@Test
	public void testFullQuestionsSearch_singleWord_boostedTitleAndBody() throws Exception {
		String qs = captureSearchQuery("*", "Post");
		assertTrue("should target title with 3x boost: " + qs, qs.contains("properties.title:Post^3"));
		assertTrue("should target body with 2x boost: " + qs, qs.contains("properties.body:Post^2"));
		assertTrue("should include untargeted fallback: " + qs, qs.contains(" OR Post)"));
		assertTrue("should include type filter: " + qs, qs.contains("type:("));
	}

	@Test
	public void testFullQuestionsSearch_phraseQuoted_boostedTitleAndBody() throws Exception {
		String qs = captureSearchQuery("*", "\"My Awesome Title\"");
		assertTrue("should target title with phrase and 3x boost: " + qs,
				qs.contains("properties.title:\"My Awesome Title\"^3"));
		assertTrue("should target body with phrase and 2x boost: " + qs,
				qs.contains("properties.body:\"My Awesome Title\"^2"));
		assertTrue("should include untargeted phrase fallback: " + qs,
				qs.contains(" OR \"My Awesome Title\")"));
	}

	@Test
	public void testFullQuestionsSearch_luceneSyntax_notBoosted() throws Exception {
		String qs = captureSearchQuery("*", "properties.title:foo AND bar");
		assertFalse("Lucene syntax should NOT be boosted: " + qs, qs.contains("^3"));
		assertFalse("Lucene syntax should NOT be boosted: " + qs, qs.contains("^2"));
		assertTrue("should be passed through with type filter: " + qs,
				qs.contains("properties.title:foo AND bar AND type:("));
	}

	@Test
	public void testFullQuestionsSearch_blankQuery_typeFilterOnly() throws Exception {
		String qs = captureSearchQuery("*", "");
		assertFalse("blank query should NOT have field targeting: " + qs, qs.contains("properties.title"));
		assertTrue("blank query should be type filter only: " + qs, qs.startsWith("type:("));
	}

	@Test
	public void testFullQuestionsSearch_starQuery_typeFilterOnly() throws Exception {
		String qs = captureSearchQuery("*", "*");
		assertFalse("star query should NOT have field targeting: " + qs, qs.contains("properties.title"));
		assertTrue("star query should be type filter only: " + qs, qs.startsWith("type:("));
	}

	@Test
	public void testFullQuestionsSearch_booleanQuery_notBoosted() throws Exception {
		String qs = captureSearchQuery("*", "foo OR bar");
		assertFalse("OR query should NOT be boosted: " + qs, qs.contains("^3"));
		assertTrue("should be passed through: " + qs, qs.contains("foo OR bar AND type:("));
	}

	@Test
	public void testFullQuestionsSearch_spaceFilterPrefix_phraseBoosted() throws Exception {
		// Reproduces the reported issue: sanitizeQueryString wraps a multi-word
		// query as "<space_filter> AND (\"phrase\")". The boost must apply to
		// the inner phrase, and the space filter prefix must be preserved.
		String qs = captureSearchQuery("properties.space:\"scooldspace:default\"",
				"\"Developer Laptop Setup\"");
		assertTrue("should preserve space filter prefix: " + qs,
				qs.contains("properties.space:\"scooldspace:default\" AND "));
		assertTrue("should boost title with phrase: " + qs,
				qs.contains("properties.title:\"Developer Laptop Setup\"^3"));
		assertTrue("should boost body with phrase: " + qs,
				qs.contains("properties.body:\"Developer Laptop Setup\"^2"));
		assertTrue("should include untargeted phrase fallback: " + qs,
				qs.contains(" OR \"Developer Laptop Setup\")"));
		assertTrue("should include type filter: " + qs, qs.contains("type:("));
	}

	@Test
	public void testFullQuestionsSearch_spaceFilterPrefix_singleWordBoosted() throws Exception {
		String qs = captureSearchQuery("properties.space:\"scooldspace:default\"", "Developer");
		assertTrue("should preserve space filter prefix: " + qs,
				qs.contains("properties.space:\"scooldspace:default\" AND "));
		assertTrue("should boost title with single word: " + qs,
				qs.contains("properties.title:Developer^3"));
		assertTrue("should boost body with single word: " + qs,
				qs.contains("properties.body:Developer^2"));
	}

	@Test
	public void testFullQuestionsSearch_spaceFilterPrefix_notSimple_passesThrough() throws Exception {
		// A Lucene-syntax query inside the space-filter prefix must NOT be
		// boosted - it's passed through untouched.
		String qs = captureSearchQuery("properties.space:\"scooldspace:default\"", "properties.name:foo");
		assertFalse("Lucene-syntax inner should NOT be boosted: " + qs, qs.contains("^3"));
		assertTrue("should pass query through: " + qs,
				qs.contains("properties.space:\"scooldspace:default\" AND (properties.name:foo) AND type:("));
	}
}