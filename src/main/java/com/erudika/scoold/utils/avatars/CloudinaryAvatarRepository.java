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
import org.apache.commons.lang3.Strings;

public class CloudinaryAvatarRepository implements AvatarRepository {
	private static final String BASE_URL = "https://res.cloudinary.com/";

	private final AvatarRepository nextRepository;

	public CloudinaryAvatarRepository(AvatarRepository nextRepository) {
		this.nextRepository = nextRepository;
	}

	@Override
	public String getLink(Profile profile, AvatarFormat format) {
		if (profile == null) {
			return nextRepository.getLink(profile, format);
		}

		String picture = profile.getPicture();
		if (!isCloudinaryLink(picture)) {
			return nextRepository.getLink(profile, format);
		}

		return configureLink(picture, format);
	}

	@Override
	public String getAnonymizedLink(String data) {
		return nextRepository.getAnonymizedLink(data);
	}

	@Override
	public boolean store(Profile profile, String url) {
		if (!isCloudinaryLink(url)) {
			return nextRepository.store(profile, url);
		}

		return applyChange(profile, url);
	}

	private boolean applyChange(Profile profile, String url) {
		profile.setPicture(url);

		return true;
	}

	private boolean isCloudinaryLink(String url) {
		return Strings.CS.startsWith(url, BASE_URL);
	}

	private String configureLink(String url, AvatarFormat format) {
		return url.replace("/upload/", "/upload/t_" + getTransformName(format) + "/");
	}

	private String getTransformName(AvatarFormat format) {
		return format.name().toLowerCase();
	}
}
