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
	public void should_use_gravatar_then_custom_link_then_default_if_gravatar_enable() {
		when(gravatarAvatarGeneratorFake.isEnabled()).thenReturn(true);
		AvatarRepository repository = new AvatarRepositoryProxy(gravatarAvatarGeneratorFake, new AvatarConfig());

		when(gravatarAvatarGeneratorFake.getLink("A")).thenReturn("https://gravatarA");
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
		assertEquals(AvatarStorageResult.failed(), storageDefaultResult);

		profile.setPicture("bad:avatar");
		String defaultLink = repository.getLink(profile, AvatarFormat.Profile);
		assertEquals(new DefaultAvatarRepository().getLink(profile, AvatarFormat.Profile), defaultLink);
	}

	@Test
	public void should_not_use_gravatar_if_gravatar_disable() {
		when(gravatarAvatarGeneratorFake.isEnabled()).thenReturn(false);
		AvatarRepository repository = new AvatarRepositoryProxy(gravatarAvatarGeneratorFake, new AvatarConfig());

		when(gravatarAvatarGeneratorFake.getLink("A")).thenReturn("https://gravatarA");
		String result = repository.getAnonymizedLink("A");
		assertNotEquals("https://gravatarA", result);
	}
}
