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

public class GravatarAvatarGeneratorTest {
	private AvatarConfig config;
	private GravatarAvatarGenerator generator;

	@Before
	public void setUp(){
		this.config = mock(AvatarConfig.class);
		when(config.gravatarPattern()).thenReturn("retro");
		this.generator = new GravatarAvatarGenerator(config);
	}

	@Test
	public void getRawLink_should_return_url_for_specific_email() {
		String url1 = generator.getRawLink("toto@example.com");
		assertEquals("https://www.gravatar.com/avatar/75a4c35602d5866368dd4c959e249aba", url1);

		String url2 = generator.getRawLink("titi@example.com");
		assertEquals("https://www.gravatar.com/avatar/1ac9dec7bfff4aeb9bc9e625bfb9a4ce", url2);
	}

	@Test
	public void getRawLink_should_return_same_url_for_same_email() {
		String email = "toto@example.com";

		String url1 = generator.getRawLink(email);
		String url2 = generator.getRawLink(email);

		assertEquals(url1, url2);
	}

	@Test
	public void getRawLink_should_return_with_empty_email_if_null() {
		assertEquals(generator.getRawLink((String)null), generator.getRawLink(""));
	}

	@Test
	public void getRawLink_should_use_email_of_profile() {
		String email = "toto@example.com";
		Profile profile = getProfileWithEmail(email);
		AvatarFormat format = AvatarFormat.Square32;

		String urlOfEmail = generator.getRawLink(email);
		String urlOfProfile = generator.getRawLink(profile);

		assertEquals(urlOfEmail, urlOfProfile);
	}

	@Test
	public void getRawLink_should_return_with_empty_email_if_profile_is_null() {
		String urlOfEmpty = generator.getRawLink(getProfileWithEmail(""));

		assertEquals(generator.getRawLink((Profile)null), urlOfEmpty);
	}

	@Test
	public void configureLink_should_return_url_for_specific_format_size_and_config_pattern_and_all_public() {
		String rawLink = generator.getRawLink("toto@example.com");

		when(config.gravatarPattern()).thenReturn("retro");
		String link1 = generator.configureLink(rawLink, AvatarFormat.Square32);
		assertEquals("https://www.gravatar.com/avatar/75a4c35602d5866368dd4c959e249aba?s=32&r=g&d=retro", link1);

		when(config.gravatarPattern()).thenReturn("identicon");
		String link2 = generator.configureLink(rawLink, AvatarFormat.Square50);
		assertEquals("https://www.gravatar.com/avatar/75a4c35602d5866368dd4c959e249aba?s=50&r=g&d=identicon", link2);
	}

	@Test
	public void getLink_should_compture_and_configure_link() {
		when(config.gravatarPattern()).thenReturn("retro");
		AvatarFormat format = AvatarFormat.Square32;
		Profile profile = getProfileWithEmail("toto@example.com");

		String result = generator.getLink(profile, format);

		String expected = generator.configureLink(generator.getRawLink(profile), format);
		assertEquals(expected, result);
	}

	@Test
	public void isLink_should_be_true_if_gravatar_domain() {
		assertTrue(generator.isLink("https://www.gravatar.com/avatar/1ac9dec7bfff4aeb9bc9e625bfb9a4ce?s=50&r=g&d=identicon"));
		assertFalse(generator.isLink("https://avatar.com"));
	}

	private Profile getProfileWithEmail(String email) {
		User user = new User();
		Profile profile = new Profile();
		profile.setUser(user);
		user.setEmail(email);
		return profile;
	}
}
