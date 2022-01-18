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
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.core.Profile;
import org.apache.commons.lang3.StringUtils;


public class CustomLinkAvatarRepository implements AvatarRepository {
	private final GravatarAvatarGenerator gravatarAvatarGenerator;
	private final AvatarRepository nextRepository;

	public CustomLinkAvatarRepository(GravatarAvatarGenerator gravatarAvatarGenerator, AvatarRepository nextRepository) {
		this.gravatarAvatarGenerator = gravatarAvatarGenerator;
		this.nextRepository = nextRepository;
	}

	@Override
	public String getLink(Profile profile, AvatarFormat format) {
		String avatar = extractPicture(profile);
		if (StringUtils.isBlank(avatar)) {
			return nextRepository.getLink(profile, format);
		}

		if (avatar.matches("^(http:|https:).*")) {
			return avatar;
		}

		if (avatar.matches("^(data:).*")) {
			return avatar;
		}

		return nextRepository.getLink(profile, format);
	}


	private String extractPicture(Profile profile) {
		if (profile == null) {
			return "";
		}

		String picture = profile.getPicture();
		if (!gravatarAvatarGenerator.isLink(picture)) {
			return picture;
		}

		String originalPicture = profile.getOriginalPicture();
		if (!gravatarAvatarGenerator.isLink(originalPicture)) {
			return originalPicture;
		}

		return "";
	}

	@Override
	public String getAnonymizedLink(String data) {

		return nextRepository.getAnonymizedLink(data);
	}

	@Override
	public AvatarStorageResult store(Profile profile, String url) {
		if (!Utils.isValidURL(url) && !url.startsWith("data:")) {
			return nextRepository.store(profile, url);
		}

		profile.setPicture(url);

		User user = profile.getUser();
		if (!user.getPicture().equals(url)) {
			user.setPicture(url);

			return AvatarStorageResult.userChanged();
		}

		return AvatarStorageResult.profileChanged();
	}
}
