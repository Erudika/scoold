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

import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.ScooldUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import javax.inject.Singleton;

@Component
@Singleton
public class GravatarAvatarGenerator {
	private static final String URL_BASE = "https://www.gravatar.com/avatar/";

	public GravatarAvatarGenerator() {
	}

	public String getLink(Profile profile, AvatarFormat format) {
		return configureLink(getRawLink(profile), format);
	}

	public String getRawLink(Profile profile) {
		String email = (profile == null || profile.getUser() == null) ? "" : profile.getUser().getEmail();
		return getRawLink(email);
	}

	public String getRawLink(String email) {
		return URL_BASE + computeToken(email);
	}

	private String computeToken(String email) {
		if (StringUtils.isBlank(email)) {
			return "";
		}

		return Utils.md5(email.toLowerCase());
	}

	public String configureLink(String url, AvatarFormat format) {
		return url + (url.endsWith("?") ? "&" : "?") + "s=" + format.getSize() + "&r=g&d=" + ScooldUtils.gravatarPattern();
	}

	public boolean isLink(String link) {
		return StringUtils.contains(link, "gravatar.com");
	}
}
