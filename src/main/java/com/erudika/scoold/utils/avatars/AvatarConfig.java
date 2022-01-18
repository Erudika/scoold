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

import com.erudika.para.core.utils.Config;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;

import static com.erudika.scoold.ScooldServer.IMAGESLINK;

@Component
@Singleton
public class AvatarConfig {
	private static final String DEFAULT_URL = IMAGESLINK + "/anon.svg";

	public boolean isGravatarEnabled() {
		return Config.getConfigBoolean("gravatars_enabled", true);
	}

	public String gravatarPattern() {
		return Config.getConfigParam("gravatars_pattern", "retro");
	}

	public boolean isCustomLinkEnabled() {
		return Config.getConfigBoolean("avatar_custom_link_accepted", false);
	}

	public String getDefaultAvatar() {
		return DEFAULT_URL;
	}
}
