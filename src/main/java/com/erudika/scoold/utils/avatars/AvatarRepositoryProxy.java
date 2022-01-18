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

import com.erudika.scoold.core.Profile;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;

@Component
@Singleton
public class AvatarRepositoryProxy implements AvatarRepository {
	private final AvatarRepository repository;

	public AvatarRepositoryProxy(GravatarAvatarGenerator gravatarAvatarGenerator, AvatarConfig config) {
		this.repository = addGravatarIfEnabled(addCustomLinkIfEnabled(getDefault(config), gravatarAvatarGenerator, config), gravatarAvatarGenerator, config);
	}

	private AvatarRepository addGravatarIfEnabled(AvatarRepository repo, GravatarAvatarGenerator gravatarAvatarGenerator, AvatarConfig config) {
		return config.isGravatarEnabled() ? new GravatarAvatarRepository(gravatarAvatarGenerator, repo) : repo;
	}

	private AvatarRepository addCustomLinkIfEnabled(AvatarRepository repo, GravatarAvatarGenerator gravatarAvatarGenerator, AvatarConfig config) {
		return config.isCustomLinkEnabled() ? new CustomLinkAvatarRepository(gravatarAvatarGenerator, repo) : repo;
	}

	private AvatarRepository getDefault(AvatarConfig config) {
		return new DefaultAvatarRepository(config);
	}

	@Override
	public String getLink(Profile profile, AvatarFormat format) {
		return repository.getLink(profile, format);
	}

	@Override
	public String getAnonymizedLink(String data) {
		return repository.getAnonymizedLink(data);
	}

	@Override
	public AvatarStorageResult store(Profile profile, String url) {
		return repository.store(profile, url);
	}
}
