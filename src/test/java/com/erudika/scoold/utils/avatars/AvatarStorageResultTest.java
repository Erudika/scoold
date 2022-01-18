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
