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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

public class DefaultAvatarRepository implements AvatarRepository {

	public DefaultAvatarRepository() {
	}

	@Override
	public String getLink(Profile profile, AvatarFormat format) {
		return (profile == null || StringUtils.isBlank(profile.getPicture())) ? ScooldUtils.getDefaultAvatar() : profile.getPicture();
	}

	@Override
	public String getAnonymizedLink(String data) {
		return ScooldUtils.getDefaultAvatar();
	}

	@Override
	public boolean store(Profile profile, String url) {
		if (StringUtils.isBlank(url) || !url.equalsIgnoreCase(profile.getOriginalPicture())) {
			if (Strings.CI.startsWith(url, ScooldUtils.getConfig().serverUrl())) {
				profile.setPicture(url);
			} else {
				profile.setPicture(ScooldUtils.getDefaultAvatar());
			}
		} else {
			profile.setPicture(profile.getOriginalPicture());
		}
		return true;
	}
}
