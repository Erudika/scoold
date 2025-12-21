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
package com.erudika.scoold.utils.avatars;

import com.erudika.para.core.User;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.ScooldUtils;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AvatarRepositoryProxyTest {
	private GravatarAvatarGenerator gravatarAvatarGeneratorFake;
	private Profile profile;

	@Before
	public void setUp() {
		this.gravatarAvatarGeneratorFake = mock(GravatarAvatarGenerator.class);
		this.profile = new Profile();
		this.profile.setUser(new User());
	}

	@Test
	public void should_use_gravatar_then_custom_link_then_default_if_gravatar_enable_and_custom_link_enable() {
		AvatarRepository repository = new AvatarRepositoryProxy(gravatarAvatarGeneratorFake);

		when(gravatarAvatarGeneratorFake.getRawLink("A")).thenReturn("https://gravatarA");
		String result = repository.getAnonymizedLink("A");
		assertEquals("https://gravatarA", result);

		when(gravatarAvatarGeneratorFake.isLink("https://gravatarA")).thenReturn(true);
		String gravatarAvatar = "https://gravatarA";
		boolean storageGravatarResult = repository.store(profile, gravatarAvatar);
		assertEquals(gravatarAvatar, profile.getPicture());
		assertTrue(storageGravatarResult);

		String customLinkAvatar = "https://avatar";
		boolean storageCustomLinkResult = repository.store(profile, customLinkAvatar);
		assertEquals(ScooldUtils.getDefaultAvatar(), profile.getPicture());
		assertTrue(storageCustomLinkResult);

		boolean storageDefaultResult = repository.store(profile, "bad:avatar");
		assertEquals(ScooldUtils.getDefaultAvatar(), profile.getPicture());
		assertTrue(storageDefaultResult);

		profile.setPicture("bad:avatar");
		String defaultLink = repository.getLink(profile, AvatarFormat.Profile);
		assertEquals(new DefaultAvatarRepository().getLink(profile, AvatarFormat.Profile), defaultLink);
	}

	@Test
	public void should_not_use_gravatar_if_gravatar_disable() {
		System.setProperty("scoold.gravatars_enabled", "false");
		AvatarRepository repository = new AvatarRepositoryProxy(gravatarAvatarGeneratorFake);

		when(gravatarAvatarGeneratorFake.getRawLink("A")).thenReturn("https://gravatarA");
		String result = repository.getAnonymizedLink("A");
		assertNotEquals("https://gravatarA", result);
		System.setProperty("scoold.gravatars_enabled", "true");
	}

	@Test
	public void should_not_use_custom_link_if_disable() {
		AvatarRepository repository = new AvatarRepositoryProxy(gravatarAvatarGeneratorFake);

		when(gravatarAvatarGeneratorFake.getRawLink("A")).thenReturn("https://gravatarA");
		String result = repository.getAnonymizedLink("A");
		assertEquals("https://gravatarA", result);

		when(gravatarAvatarGeneratorFake.isLink("https://gravatarA")).thenReturn(true);
		String gravatarAvatar = "https://gravatarA";
		boolean storageGravatarResult = repository.store(profile, gravatarAvatar);
		assertEquals(gravatarAvatar, profile.getPicture());
		assertTrue(storageGravatarResult);

		String customLinkAvatar = "https://avatar";
		boolean storageCustomLinkResult = repository.store(profile, customLinkAvatar);
		assertEquals(ScooldUtils.getDefaultAvatar(), profile.getPicture());
		assertTrue(storageCustomLinkResult);
	}

	@Test
	public void should_use_cloudinary_if_enable() {
		System.setProperty("scoold.cloudinary_url", "cloudinary://123456:abcdefaddd@scoold");
		System.setProperty("scoold.avatar_repository", "cloudinary");
		AvatarRepository repository = new AvatarRepositoryProxy(gravatarAvatarGeneratorFake);

		when(gravatarAvatarGeneratorFake.getRawLink("A")).thenReturn("https://gravatarA");
		String result = repository.getAnonymizedLink("A");
		assertEquals("https://gravatarA", result);

		when(gravatarAvatarGeneratorFake.isLink("https://gravatarA")).thenReturn(true);
		String gravatarAvatar = "https://gravatarA";
		boolean storageGravatarResult = repository.store(profile, gravatarAvatar);
		assertEquals(gravatarAvatar, profile.getPicture());
		assertTrue(storageGravatarResult);

		String cloudinaryAvatar = "https://res.cloudinary.com/test/image/upload/avatar.jpg";
		repository.store(profile, cloudinaryAvatar);
		assertEquals("https://res.cloudinary.com/test/image/upload/t_square32/avatar.jpg", repository.getLink(profile, AvatarFormat.Square32));
	}
}
