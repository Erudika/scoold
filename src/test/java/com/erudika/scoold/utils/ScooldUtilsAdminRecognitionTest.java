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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.User;
import com.erudika.para.core.email.Emailer;
import com.erudika.scoold.ScooldConfig;
import com.erudika.scoold.utils.avatars.AvatarRepositoryProxy;
import com.erudika.scoold.utils.avatars.GravatarAvatarGenerator;
import org.junit.Before;
import org.junit.Test;

public class ScooldUtilsAdminRecognitionTest {

	private ScooldUtils utils;

	@Before
	public void setUp() {
		ParaClient pc = mock(ParaClient.class);
		LanguageUtils langutils = mock(LanguageUtils.class);
		Emailer emailer = mock(Emailer.class);
		AvatarRepositoryProxy avatarRepo = mock(AvatarRepositoryProxy.class);
		GravatarAvatarGenerator gravatarGen = mock(GravatarAvatarGenerator.class);
		when(avatarRepo.getAnonymizedLink(anyString())).thenReturn("");
		utils = new ScooldUtils(pc, langutils, emailer, avatarRepo, gravatarGen);
	}

	@Test
	public void doesNotRecognizeDotlessIAsConfiguredAdminEmail() {
		String configuredAdmin = "admin.security-test@corp.io";
		utils.addFirstAdmin(configuredAdmin, new ScooldConfig() {
			@Override
			public boolean onboardingEnabled() {
				return true;
			}
		});

		User lookalike = new User();
		lookalike.setIdentifier("external-security-test-id");
		lookalike.setEmail("admin.security-test@corp.ıo");

		assertFalse(utils.isRecognizedAsAdmin(lookalike));
	}

	@Test
	public void recognizesConfiguredAdminEmailWithoutAsciiCaseSensitivity() {
		String configuredAdmin = "admin.case-test@corp.io";
		utils.addFirstAdmin(configuredAdmin, new ScooldConfig() {
			@Override
			public boolean onboardingEnabled() {
				return true;
			}
		});

		User admin = new User();
		admin.setIdentifier("external-case-test-id");
		admin.setEmail("ADMIN.CASE-TEST@CORP.IO");

		assertTrue(utils.isRecognizedAsAdmin(admin));
	}
}
