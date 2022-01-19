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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ImgurAvatarRepositoryTest {

	private ImgurAvatarRepository repository;
	private AvatarRepository defaultRepository;
	private Profile profile;

	@Before
	public void setUp(){
		this.profile = new Profile();
		this.profile.setUser(new User());
		this.defaultRepository = new DefaultAvatarRepository();
		this.repository = new ImgurAvatarRepository(defaultRepository);
	}

	@Test
	public void getLink_should_return_default_if_no_profile() {
		String avatar = repository.getLink(null, AvatarFormat.Profile);

		assertEquals(defaultRepository.getLink(null, AvatarFormat.Profile), avatar);
	}

	@Test
	public void getLink_should_return_default_if_no_avatar() {
		profile.setPicture("");

		String avatar = repository.getLink(profile, AvatarFormat.Profile);

		assertEquals(defaultRepository.getLink(null, AvatarFormat.Profile), avatar);
	}

	@Test
	public void getLink_should_return_default_if_picture_has_unknown_format() {
		profile.setPicture("bad:AvAtar");

		String avatar = repository.getLink(profile, AvatarFormat.Profile);

		assertEquals(defaultRepository.getLink(null, AvatarFormat.Profile), avatar);
	}

	@Test
	public void getAnonymizedLink_should_use_default_repository() {
		AvatarRepository defaultRepository = mock(AvatarRepository.class);
		AvatarRepository repository = new ImgurAvatarRepository(defaultRepository);
		when(defaultRepository.getAnonymizedLink("A")).thenReturn("https://avatar");

		String avatar = repository.getAnonymizedLink("A");

		assertEquals("https://avatar", avatar);
	}

	@Test
	public void store_should_change_profile_picture_but_not_user_is_already_this_picture() {
		String avatar = "https://i.imgur.com/123";
		profile.getUser().setPicture(avatar);

		boolean result = repository.store(profile, avatar);

		assertEquals(true, result);
		assertEquals(avatar, profile.getPicture());
		assertEquals(avatar, profile.getUser().getPicture());
	}

	@Test
	public void store_should_call_next_repository_if_bad_url() {
		AvatarRepository defaultRepository = mock(AvatarRepository.class);
		AvatarRepository repository = new ImgurAvatarRepository(defaultRepository);
		String avatar = "bad:avatar";
		when(defaultRepository.store(profile, avatar)).thenReturn(false);

		boolean result = repository.store(profile, avatar);

		verify(defaultRepository).store(profile, avatar);
		assertEquals(false, result);
		assertNotEquals(avatar, profile.getPicture());
		assertNotEquals(avatar, profile.getUser().getPicture());
	}

}
