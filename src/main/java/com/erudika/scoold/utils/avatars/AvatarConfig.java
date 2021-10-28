package com.erudika.scoold.utils.avatars;

import com.erudika.para.utils.Config;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;

import static com.erudika.scoold.ScooldServer.IMAGESLINK;

@Component
@Singleton
public class AvatarConfig {
	private static final String DEFAULT_URL = IMAGESLINK + "/anon.svg";

	public boolean isAvatarValidationEnabled() {
		return Config.getConfigBoolean("avatar_validation_enabled", false); // this should be deleted in the future
	}

	public boolean isGravatarEnabled() {
		return Config.getConfigBoolean("gravatars_enabled", true);
	}

	public String gravatarPattern() {
		return Config.getConfigParam("gravatars_pattern", "retro");
	}

	public boolean isCustomLinkEnabled() {
		return Config.getConfigBoolean("avatar_custom_link_accepted", true);
	}

	public String getDefaultAvatar() {
		return DEFAULT_URL;
	}
}
