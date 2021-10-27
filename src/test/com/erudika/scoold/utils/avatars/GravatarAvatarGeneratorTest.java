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
	public void getLink_should_return_url_for_specific_email_and_format_size_and_config_pattern_and_all_public() {
		when(config.gravatarPattern()).thenReturn("retro");
		String url1 = generator.getLink("toto@example.com", AvatarFormat.Square32);
		assertEquals("https://www.gravatar.com/avatar/75a4c35602d5866368dd4c959e249aba?s=32&r=g&d=retro", url1);

		when(config.gravatarPattern()).thenReturn("identicon");
		String url2 = generator.getLink("titi@example.com", AvatarFormat.Square50);
		assertEquals("https://www.gravatar.com/avatar/1ac9dec7bfff4aeb9bc9e625bfb9a4ce?s=50&r=g&d=identicon", url2);
	}

	@Test
	public void getLink_should_return_same_url_for_same_email() {
		String email = "toto@example.com";
		AvatarFormat format = AvatarFormat.Square32;
		String url1 = generator.getLink(email, format);
		String url2 = generator.getLink(email, format);
		assertEquals(url1, url2);

		String urlOfOtherEmail = generator.getLink("titi@example.com", format);
		assertNotEquals(url1, urlOfOtherEmail);
	}

	@Test
	public void getLink_should_return_with_empty_email_if_null() {
		AvatarFormat format = AvatarFormat.Square32;
		assertEquals(generator.getLink((String)null, format), generator.getLink("", format));
	}

	@Test
	public void getLink_should_use_email_of_profile() {
		User user = new User();
		Profile profile = new Profile();
		profile.setUser(user);
		String email = "toto@example.com";
		user.setEmail(email);
		AvatarFormat format = AvatarFormat.Square32;

		String urlOfEmail = generator.getLink(email, format);
		String urlOfProfile = generator.getLink(profile, format);

		assertEquals(urlOfEmail, urlOfProfile);
	}

	@Test
	public void getLink_should_return_with_empty_email_if_profile_is_null() {
		AvatarFormat format = AvatarFormat.Square32;
		String urlOfEmpty = generator.getLink("", format);

		assertEquals(generator.getLink((Profile)null, format), urlOfEmpty);
		User user = new User();
		Profile profile = new Profile();
		profile.setUser(user);
		assertEquals(generator.getLink(profile, format), urlOfEmpty);
	}

	@Test
	public void isLink_should_be_true_if_gravatar_domain() {
		assertTrue(generator.isLink("https://www.gravatar.com/avatar/1ac9dec7bfff4aeb9bc9e625bfb9a4ce?s=50&r=g&d=identicon"));
		assertFalse(generator.isLink("https://avatar.com"));
	}
}
