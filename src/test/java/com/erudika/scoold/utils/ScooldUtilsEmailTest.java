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
import com.erudika.para.core.User;
import com.erudika.para.core.email.Emailer;
import com.erudika.para.core.utils.Para;
import com.erudika.scoold.ScooldConfig;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.avatars.AvatarRepositoryProxy;
import com.erudika.scoold.utils.avatars.GravatarAvatarGenerator;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;

public class ScooldUtilsEmailTest {

	private ParaClient pc;
	private Emailer emailer;
	private ScooldUtils utils;

	@Before
	public void setUp() throws Exception {
		pc = mock(ParaClient.class);
		LanguageUtils langutils = mock(LanguageUtils.class);
		emailer = mock(Emailer.class);
		AvatarRepositoryProxy avatarRepo = mock(AvatarRepositoryProxy.class);
		GravatarAvatarGenerator gravatarGen = mock(GravatarAvatarGenerator.class);

		when(avatarRepo.getAnonymizedLink(anyString())).thenReturn("");
		when(emailer.sendEmail(anyList(), anyString(), anyString())).thenReturn(true);

		utils = new ScooldUtils(pc, langutils, emailer, avatarRepo, gravatarGen);

		Field instanceField = ScooldUtils.class.getDeclaredField("instance");
		instanceField.setAccessible(true);
		instanceField.set(null, utils);
	}

	private void setupFindTermInList() {
		when(pc.findTermInList(anyString(), anyString(), anyList())).thenAnswer(invocation -> {
			List<String> terms = invocation.getArgument(2);
			List<User> users = new ArrayList<>(terms.size());
			for (String email : terms) {
				User u = new User();
				u.setId("uid_" + email);
				u.setEmail(email);
				u.setName("User " + email);
				users.add(u);
			}
			return users;
		});
	}

	private void setupReadAll() {
		when(pc.readAll(anyList())).thenAnswer(invocation -> {
			List<String> ids = invocation.getArgument(0);
			List<Profile> profiles = new ArrayList<>(ids.size());
			String sep = Para.getConfig().separator();
			String suffix = sep + "profile";
			for (String id : ids) {
				String creatorId = id.endsWith(suffix) ?
						id.substring(0, id.length() - suffix.length()) : id;
				Profile p = new Profile();
				p.setId(id);
				p.setCreatorid(creatorId);
				p.setSpaces(new HashSet<>(Collections.singletonList("space1:MySpace")));
				p.setFavspaces(Collections.emptySet());
				profiles.add(p);
			}
			return profiles;
		});
	}

	@Test
	public void testSendEmailsToSubscribersInSpace_1000emails() throws Exception {
		int emailCount = 1000;
		Set<String> emails = new LinkedHashSet<>();
		for (int i = 0; i < emailCount; i++) {
			emails.add("user" + i + "@example.com");
		}

		setupFindTermInList();
		setupReadAll();

		ScooldConfig conf = ScooldUtils.getConfig();
		int pageSize = conf.maxItemsPerPage();

		Method method = ScooldUtils.class.getDeclaredMethod(
				"sendEmailsToSubscribersInSpace", Set.class, String.class,
				String.class, String.class);
		method.setAccessible(true);

		long start = System.currentTimeMillis();
		method.invoke(utils, emails, "space1:MySpace", "Test Subject", "<p>Test</p>");
		long elapsed = System.currentTimeMillis() - start;

		ArgumentCaptor<List<String>> emailsCaptor = ArgumentCaptor.forClass(List.class);
		verify(emailer, atLeastOnce()).sendEmail(emailsCaptor.capture(),
				eq("Test Subject"), eq("<p>Test</p>"));

		int totalEmailsSent = 0;
		for (List<String> batch : emailsCaptor.getAllValues()) {
			totalEmailsSent += batch.size();
		}

		int expectedQueryBatches = (int) Math.ceil((double) emailCount / pageSize);
		System.out.println("\n=== sendEmailsToSubscribersInSpace performance ===");
		System.out.println("Emails: " + emailCount);
		System.out.println("Page size (maxItemsPerPage): " + pageSize);
		System.out.println("Query batches: " + expectedQueryBatches);
		System.out.println("emailer.sendEmail calls: "
				+ emailsCaptor.getAllValues().size());
		System.out.println("Total emails sent: " + totalEmailsSent);
		System.out.println("Elapsed: " + elapsed + " ms");

		assertTrue("Should have sent all 1000 emails", totalEmailsSent == emailCount);
		assertTrue("Should complete in reasonable time", elapsed < 30000);
	}

	@Test
	public void testSendEmailsToSubscribersInSpace_countsApiCalls() throws Exception {
		int emailCount = 1000;
		Set<String> emails = new LinkedHashSet<>();
		for (int i = 0; i < emailCount; i++) {
			emails.add("user" + i + "@example.com");
		}

		ScooldConfig conf = ScooldUtils.getConfig();
		int pageSize = conf.maxItemsPerPage();
		int expectedQueryBatches = (int) Math.ceil((double) emailCount / pageSize);

		AtomicInteger findTermCalls = new AtomicInteger();
		AtomicInteger readAllCalls = new AtomicInteger();

		when(pc.findTermInList(anyString(), anyString(), anyList())).thenAnswer(invocation -> {
			findTermCalls.incrementAndGet();
			List<String> terms = invocation.getArgument(2);
			List<User> users = new ArrayList<>(terms.size());
			for (String email : terms) {
				User u = new User();
				u.setId("uid_" + email);
				u.setEmail(email);
				u.setName("User " + email);
				users.add(u);
			}
			return users;
		});

		when(pc.readAll(anyList())).thenAnswer(invocation -> {
			readAllCalls.incrementAndGet();
			List<String> ids = invocation.getArgument(0);
			List<Profile> profiles = new ArrayList<>(ids.size());
			String sep = Para.getConfig().separator();
			String suffix = sep + "profile";
			for (String id : ids) {
				String creatorId = id.endsWith(suffix) ?
						id.substring(0, id.length() - suffix.length()) : id;
				Profile p = new Profile();
				p.setId(id);
				p.setCreatorid(creatorId);
				p.setSpaces(new HashSet<>(Collections.singletonList("space1:MySpace")));
				p.setFavspaces(Collections.emptySet());
				profiles.add(p);
			}
			return profiles;
		});

		Method method = ScooldUtils.class.getDeclaredMethod(
				"sendEmailsToSubscribersInSpace", Set.class, String.class,
				String.class, String.class);
		method.setAccessible(true);

		long start = System.currentTimeMillis();
		method.invoke(utils, emails, "space1:MySpace", "Test Subject", "<p>Test</p>");
		long elapsed = System.currentTimeMillis() - start;

		ArgumentCaptor<List<String>> emailsCaptor = ArgumentCaptor.forClass(List.class);
		verify(emailer, atLeastOnce()).sendEmail(emailsCaptor.capture(),
				eq("Test Subject"), eq("<p>Test</p>"));

		System.out.println("\n=== API call analysis for " + emailCount + " emails ===");
		System.out.println("findTermInList calls: " + findTermCalls.get());
		System.out.println("readAll calls: " + readAllCalls.get());
		System.out.println("Total ParaClient calls: "
				+ (findTermCalls.get() + readAllCalls.get()));
		System.out.println("Query batches needed: " + expectedQueryBatches);
		System.out.println("emailer.sendEmail calls: "
				+ emailsCaptor.getAllValues().size());
		System.out.println("Elapsed: " + elapsed + " ms");

		assertEquals("findTermInList should be called once per query batch",
				expectedQueryBatches, findTermCalls.get());
		assertEquals("readAll should be called once per query batch",
				expectedQueryBatches, readAllCalls.get());
		assertEquals("sendEmail should be called once", 1,
				emailsCaptor.getAllValues().size());
	}

	@Test
	public void testSendEmailsToSubscribersInSpace_emptySet() throws Exception {
		setupFindTermInList();
		setupReadAll();

		Method method = ScooldUtils.class.getDeclaredMethod(
				"sendEmailsToSubscribersInSpace", Set.class, String.class,
				String.class, String.class);
		method.setAccessible(true);

		method.invoke(utils, Collections.emptySet(), "space1:MySpace",
				"Test", "<p>Test</p>");

		verify(emailer, never()).sendEmail(anyList(), anyString(), anyString());
		verify(pc, never()).findTermInList(anyString(), anyString(), anyList());
	}
}
