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

import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.ScooldUtils;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class DefaultAvatarRepositoryTest {
	private DefaultAvatarRepository repository;

	@Before
	public void setUp(){
		this.repository = new DefaultAvatarRepository();
	}

	@Test
	public void getLink_should_return_always_default_avatar() {
		Profile profile = new Profile();
		assertEquals(ScooldUtils.getDefaultAvatar(), repository.getLink(profile, AvatarFormat.Profile));
		assertEquals(repository.getLink(new Profile(), AvatarFormat.Square32), repository.getLink(new Profile(), AvatarFormat.Profile));
	}

	@Test
	public void getAnonymizedLink_should_always_return_default_avatar() {
		assertEquals(ScooldUtils.getDefaultAvatar(), repository.getAnonymizedLink("A"));
		assertEquals(repository.getAnonymizedLink("A"), repository.getAnonymizedLink("B"));
	}

	@Test
	public void store_should_nothing() {
		Profile profile = new Profile();

		boolean result = repository.store(profile, "https://avatar");
		assertEquals(ScooldUtils.getDefaultAvatar(), profile.getPicture());
		assertEquals(true, result);
	}
}
