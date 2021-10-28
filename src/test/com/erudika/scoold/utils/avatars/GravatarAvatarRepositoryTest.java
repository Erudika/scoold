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

	@Before
	public void setUp(){
		this.profile = new Profile();
		this.profile.setUser(new User());
		AvatarConfig config = new AvatarConfig();
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
	public void getLink_should_return_gravatar_link_with_email() {
		profile.getUser().setEmail("toto@example.com");

		String avatar = repository.getLink(profile, AvatarFormat.Square32);

		assertEquals(gravatarGenerator.getLink("toto@example.com", AvatarFormat.Square32), avatar);
	}

	@Test
	public void getLink_should_return_default_if_picture_isnot_empty() {
		profile.getUser().setEmail("toto@example.com");
		profile.setPicture("https://avatar");

		String avatar = repository.getLink(profile, AvatarFormat.Profile);

		assertEquals(defaultRepository.getLink(null, AvatarFormat.Profile), avatar);
	}

	@Test
	public void getLink_should_return_gravatar_if_picture_is_a_gravatar() {
		profile.getUser().setEmail("toto@example.com");
		profile.setPicture(gravatarGenerator.getLink("titi@example.com", AvatarFormat.Profile));

		String avatar = repository.getLink(profile, AvatarFormat.Square32);

		assertEquals(gravatarGenerator.getLink("toto@example.com", AvatarFormat.Square32), avatar);
	}

	@Test
	public void getAnonymizedLink_should_return_gravatar_with_data() {
		String avatar = repository.getAnonymizedLink("A");

		assertEquals(gravatarGenerator.getLink("A", AvatarFormat.Profile), avatar);
	}

	@Test
	public void store_should_change_profile_picture() {
		profile.getUser().setEmail("toto@example.com");
		String avatar = gravatarGenerator.getLink("toto@example.com", AvatarFormat.Profile);

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
}
