/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;


public class ImgurAvatarRepository implements AvatarRepository {

	private final AvatarRepository nextRepository;

	public ImgurAvatarRepository(AvatarRepository nextRepository) {
		this.nextRepository = nextRepository;
	}

	@Override
	public String getLink(Profile profile, AvatarFormat format) {
		if (profile == null || StringUtils.isBlank(profile.getPicture())) {
			return nextRepository.getLink(profile, format);
		}
		String picture = profile.getPicture();
		if (isImgurLink(picture)) {
			return picture;
		}
		return ScooldUtils.getDefaultAvatar();
	}

	private boolean isImgurLink(String picture) {
		return Strings.CI.startsWith(picture, "https://i.imgur.com/");
	}

	@Override
	public String getAnonymizedLink(String data) {
		return nextRepository.getAnonymizedLink(data);
	}

	@Override
	public boolean store(Profile profile, String url) {
		if (!isImgurLink(url)) {
			return nextRepository.store(profile, url);
		}
		profile.setPicture(url);
		return true;
	}
}
