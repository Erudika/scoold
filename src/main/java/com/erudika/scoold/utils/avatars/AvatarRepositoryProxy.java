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
import org.springframework.stereotype.Component;
import javax.inject.Singleton;
import software.amazon.awssdk.utils.StringUtils;

@Component
@Singleton
public class AvatarRepositoryProxy implements AvatarRepository {
	private final AvatarRepository repository;

	public AvatarRepositoryProxy(GravatarAvatarGenerator gravatarAvatarGenerator) {
		this.repository = addGravatarIfEnabled(addCloudinaryIfEnabled(addImgurIfEnabled(getDefault())), gravatarAvatarGenerator);
	}

	private AvatarRepository addGravatarIfEnabled(AvatarRepository repo, GravatarAvatarGenerator gravatarAvatarGenerator) {
		return ScooldUtils.isGravatarEnabled() ? new GravatarAvatarRepository(gravatarAvatarGenerator, repo) : repo;
	}

	private AvatarRepository addImgurIfEnabled(AvatarRepository repo) {
		return ScooldUtils.isImgurAvatarRepositoryEnabled() ? new ImgurAvatarRepository(repo) : repo;
	}

	private AvatarRepository addCloudinaryIfEnabled(AvatarRepository repo) {
		return ScooldUtils.isCloudinaryAvatarRepositoryEnabled() ? new CloudinaryAvatarRepository(repo) : repo;
	}

	private AvatarRepository getDefault() {
		return new DefaultAvatarRepository();
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
	public boolean store(Profile profile, String url) {
		if (profile != null && StringUtils.equals(profile.getPicture(), url)) {
			return false;
		}
		return repository.store(profile, url);
	}
}
