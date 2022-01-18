/*
 * Copyright 2013-2021 Erudika. https://erudika.com
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
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
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
		AvatarConfig config = mock(AvatarConfig.class);
		when(config.getDefaultAvatar()).thenReturn("/default_avatar");
		when(config.isGravatarEnabled()).thenReturn(true);
		when(config.isCustomLinkEnabled()).thenReturn(true);
		AvatarRepository repository = new AvatarRepositoryProxy(gravatarAvatarGeneratorFake, config);

		when(gravatarAvatarGeneratorFake.getRawLink("A")).thenReturn("https://gravatarA");
		String result = repository.getAnonymizedLink("A");
		assertEquals("https://gravatarA", result);

		when(gravatarAvatarGeneratorFake.isLink("https://gravatarA")).thenReturn(true);
		String gravatarAvatar = "https://gravatarA";
		AvatarStorageResult storageGravatarResult = repository.store(profile, gravatarAvatar);
		assertEquals(gravatarAvatar, profile.getPicture());
		assertEquals(AvatarStorageResult.profileChanged(), storageGravatarResult);

		String customLinkAvatar = "https://avatar";
		AvatarStorageResult storageCustomLinkResult = repository.store(profile, customLinkAvatar);
		assertEquals(customLinkAvatar, profile.getPicture());
		assertEquals(AvatarStorageResult.userChanged(), storageCustomLinkResult);

		AvatarStorageResult storageDefaultResult = repository.store(profile, "bad:avatar");
		assertEquals("", profile.getPicture());
		assertEquals(AvatarStorageResult.profileChanged(), storageDefaultResult);

		profile.setPicture("bad:avatar");
		String defaultLink = repository.getLink(profile, AvatarFormat.Profile);
		assertEquals(new DefaultAvatarRepository(config).getLink(profile, AvatarFormat.Profile), defaultLink);
	}

	@Test
	public void should_not_use_gravatar_if_gravatar_disable() {
		AvatarConfig config = mock(AvatarConfig.class);
		when(config.isGravatarEnabled()).thenReturn(false);
		when(config.isCustomLinkEnabled()).thenReturn(true);
		AvatarRepository repository = new AvatarRepositoryProxy(gravatarAvatarGeneratorFake, config);

		when(gravatarAvatarGeneratorFake.getRawLink("A")).thenReturn("https://gravatarA");
		String result = repository.getAnonymizedLink("A");
		assertNotEquals("https://gravatarA", result);
	}

	@Test
	public void should_not_use_custom_link_if_disable() {
		AvatarConfig config = mock(AvatarConfig.class);
		when(config.getDefaultAvatar()).thenReturn("/default_avatar");
		when(config.isGravatarEnabled()).thenReturn(true);
		when(config.isCustomLinkEnabled()).thenReturn(false);
		AvatarRepository repository = new AvatarRepositoryProxy(gravatarAvatarGeneratorFake, config);

		when(gravatarAvatarGeneratorFake.getRawLink("A")).thenReturn("https://gravatarA");
		String result = repository.getAnonymizedLink("A");
		assertEquals("https://gravatarA", result);

		when(gravatarAvatarGeneratorFake.isLink("https://gravatarA")).thenReturn(true);
		String gravatarAvatar = "https://gravatarA";
		AvatarStorageResult storageGravatarResult = repository.store(profile, gravatarAvatar);
		assertEquals(gravatarAvatar, profile.getPicture());
		assertEquals(AvatarStorageResult.profileChanged(), storageGravatarResult);

		String customLinkAvatar = "https://avatar";
		AvatarStorageResult storageCustomLinkResult = repository.store(profile, customLinkAvatar);
		assertEquals("", profile.getPicture());
		assertEquals(AvatarStorageResult.profileChanged(), storageCustomLinkResult);
	}
}
