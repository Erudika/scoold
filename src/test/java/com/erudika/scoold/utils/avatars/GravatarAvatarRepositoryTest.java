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
import static org.mockito.Mockito.*;

public class GravatarAvatarRepositoryTest {
	private GravatarAvatarRepository repository;
	private AvatarRepository defaultRepository;
	private Profile profile;
	private GravatarAvatarGenerator gravatarGenerator;
	private AvatarConfig config;

	@Before
	public void setUp(){
		this.profile = new Profile();
		this.profile.setUser(new User());
		this.config = new AvatarConfig();
		this.defaultRepository = new DefaultAvatarRepository(config);
		this.gravatarGenerator = new GravatarAvatarGenerator(config);
		this.repository = new GravatarAvatarRepository(gravatarGenerator, defaultRepository);
	}

	@Test
	public void getLink_should_return_default_if_no_profile() {
		String avatar = repository.getLink(null, AvatarFormat.Profile);

		assertEquals(defaultRepository.getLink(null, AvatarFormat.Profile), avatar);
	}

	@Test
	public void getLink_should_return_gravatar_link_if_no_picture() {
		profile.setPicture("");

		String avatar = repository.getLink(profile, AvatarFormat.Profile);

		assertEquals(gravatarGenerator.configureLink(gravatarGenerator.getRawLink(profile), AvatarFormat.Profile), avatar);
	}

	@Test
	public void getLink_should_return_gravatar_link_with_email() {
		profile.getUser().setEmail("toto@example.com");

		String avatar = repository.getLink(profile, AvatarFormat.Square32);

		assertEquals(gravatarGenerator.getLink(profile, AvatarFormat.Square32), avatar);
	}

	@Test
	public void getLink_should_return_default_if_picture_isnot_empty() {
		profile.getUser().setEmail("toto@example.com");
		profile.setPicture("https://avatar");

		String avatar = repository.getLink(profile, AvatarFormat.Profile);

		assertEquals(defaultRepository.getLink(null, AvatarFormat.Profile), avatar);
	}

	@Test
	public void getLink_should_configure_link_if_picture_is_a_gravatar() {
		profile.getUser().setEmail("toto@example.com");
		profile.setPicture(gravatarGenerator.getRawLink("titi@example.com"));

		String avatar = repository.getLink(profile, AvatarFormat.Square32);

		assertEquals(gravatarGenerator.configureLink(gravatarGenerator.getRawLink("titi@example.com"), AvatarFormat.Square32), avatar);
	}

	@Test
	public void getAnonymizedLink_should_return_gravatar_with_data() {
		String avatar = repository.getAnonymizedLink("A");

		assertEquals(gravatarGenerator.getRawLink("A"), avatar);
	}

	@Test
	public void store_should_change_profile_picture() {
		profile.getUser().setEmail("toto@example.com");
		String avatar = gravatarGenerator.configureLink(gravatarGenerator.getRawLink("toto@example.com"), AvatarFormat.Profile);

		AvatarStorageResult result = repository.store(profile, avatar);

		assertEquals(AvatarStorageResult.profileChanged(), result);
		assertEquals(avatar, profile.getPicture());
		assertNotEquals(avatar, profile.getUser().getPicture());
	}

	@Test
	public void store_should_nothing_if_url_is_not_gravatar_link() {
		AvatarRepository defaultRepository = mock(AvatarRepository.class);
		AvatarRepository repository = new GravatarAvatarRepository(gravatarGenerator, defaultRepository);
		profile.getUser().setEmail("toto@example.com");
		String avatar = "https://avatar";
		when(defaultRepository.store(profile, avatar)).thenReturn(AvatarStorageResult.failed());

		AvatarStorageResult result = repository.store(profile, avatar);

		assertEquals(AvatarStorageResult.failed(), result);
		assertNotEquals(avatar, profile.getPicture());
		assertNotEquals(avatar, profile.getUser().getPicture());
		verify(defaultRepository, times(1)).store(profile, avatar);
	}

	@Test
	public void store_should_change_profile_picture_if_url_is_empty() {
		profile.getUser().setEmail("toto@example.com");
		String avatar = gravatarGenerator.getRawLink("toto@example.com");

		AvatarStorageResult result = repository.store(profile, "");

		assertEquals(AvatarStorageResult.profileChanged(), result);
		assertEquals(avatar, profile.getPicture());
		assertNotEquals(avatar, profile.getUser().getPicture());
	}

	@Test
	public void store_should_change_profile_picture_if_url_is_default_avatar() {
		profile.getUser().setEmail("toto@example.com");
		String avatar = gravatarGenerator.getRawLink("toto@example.com");

		AvatarStorageResult result = repository.store(profile, config.getDefaultAvatar());

		assertEquals(AvatarStorageResult.profileChanged(), result);
		assertEquals(avatar, profile.getPicture());
		assertNotEquals(avatar, profile.getUser().getPicture());
	}
}
