package com.erudika.scoold.utils.avatars;

import org.junit.Test;

import static org.junit.Assert.*;

public class AvatarStorageResultTest {
	@Test
	public void failed_should_flags_as_no_change() {
		AvatarStorageResult result = AvatarStorageResult.failed();

		assertFalse(result.isProfileChanged());
		assertFalse(result.isUserChanged());
	}

	@Test
	public void profileChanged_should_flags_as_profile_changed_but_not_user() {
		AvatarStorageResult result = AvatarStorageResult.profileChanged();

		assertTrue(result.isProfileChanged());
		assertFalse(result.isUserChanged());
	}

	@Test
	public void userChanged_should_flags_as_profile_and_user_change() {
		AvatarStorageResult result = AvatarStorageResult.userChanged();

		assertTrue(result.isProfileChanged());
		assertTrue(result.isUserChanged());
	}

	@Test
	public void can_compare() {
		assertEquals(AvatarStorageResult.userChanged(), AvatarStorageResult.userChanged());
		assertEquals(AvatarStorageResult.failed(), AvatarStorageResult.failed());
		assertNotEquals(AvatarStorageResult.failed(), AvatarStorageResult.userChanged());
	}
}
