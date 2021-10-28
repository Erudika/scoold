package com.erudika.scoold.utils.avatars;

import com.erudika.para.core.User;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Profile;
import org.apache.commons.lang3.StringUtils;

import static com.erudika.scoold.ScooldServer.PEOPLELINK;

public class CustomLinkAvatarRepository implements AvatarRepository {
	private final GravatarAvatarGenerator gravatarAvatarGenerator;
	private final AvatarConfig config;
	private final AvatarRepository nextRepository;

	public CustomLinkAvatarRepository(GravatarAvatarGenerator gravatarAvatarGenerator, AvatarConfig config, AvatarRepository nextRepository) {
		this.gravatarAvatarGenerator = gravatarAvatarGenerator;
		this.config = config;
		this.nextRepository = nextRepository;
	}

	@Override
	public String getLink(Profile profile, AvatarFormat format) {
		String avatar = extractPicture(profile);
		if (StringUtils.isBlank(avatar)) {
			return nextRepository.getLink(profile, format);
		}

		if (avatar.matches("^(http:|https:).*")) {
			return config.isAvatarValidationEnabled()
				? PEOPLELINK + "/avatar?url=" + Utils.urlEncode(avatar)
				: avatar;
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
