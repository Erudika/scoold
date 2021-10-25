package com.erudika.scoold.utils.avatars;

import com.erudika.para.utils.Config;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.ScooldUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;

import static com.erudika.scoold.ScooldServer.*;

@Component
@Singleton
public class LegacyAvatarRepository implements AvatarRepository {
	private final GravatarAvatarGenerator gravatarAvatarGenerator;

	public LegacyAvatarRepository(GravatarAvatarGenerator gravatarAvatarGenerator) {
		this.gravatarAvatarGenerator = gravatarAvatarGenerator;
	}

	@Override
	public String getLink(Profile user, AvatarFormat format) {
		if (user == null || user.getPicture() == null) {
			return IMAGESLINK + "/anon.sgv";
		}

		String avatar = user.getPicture();
		if (avatar.matches("^(http:|https:).*")) {
			return isAvatarValidationEnabled()
				? PEOPLELINK + "/avatar?url=" + Utils.urlEncode(avatar)
				: avatar;
		}
		if (avatar.matches("^(data:).*")) {
			return avatar;
		}
		return IMAGESLINK + "/anon.sgv";
	}

	private String getUserPicture(Profile user) {
		String picture = user.getPicture();
		if (gravatarAvatarGenerator.isLink(picture) && !gravatarAvatarGenerator.isEnabled()) {
			String originalPicture = user.getOriginalPicture();
			if (gravatarAvatarGenerator.isLink(originalPicture)) {
				return gravatarAvatarGenerator.getLink(user); // returns default image, not gravatar
			} else {
				return StringUtils.isBlank(originalPicture) ? gravatarAvatarGenerator.getLink(user) : originalPicture;
			}
		}
		return picture;
	}

	@Override
	public String getAnonymizedLink(String data) {
		return gravatarAvatarGenerator.getLink(data);
	}

	public boolean isAvatarValidationEnabled() {
		return Config.getConfigBoolean("avatar_validation_enabled", false); // this should be deleted in the future
	}
}
