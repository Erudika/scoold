package com.erudika.scoold.utils.avatars;

import com.erudika.para.utils.Config;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Profile;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;

import static com.erudika.scoold.ScooldServer.*;

@Component
@Singleton
public class GravatarAvatarGenerator {
	public boolean isEnabled() {
		return Config.getConfigBoolean("gravatars_enabled", true);
	}

	public String getLink(Profile profile) {
		if (!isEnabled()) {
			return getServerURL() + CONTEXT_PATH +  PEOPLELINK + "/avatar";
		}
		if (profile == null || profile.getUser() == null) {
			return "https://www.gravatar.com/avatar?d=retro&size=400";
		} else {
			return getLink(profile.getUser().getEmail());
		}
	}

	public String getLink(String email) {
		if (!isEnabled()) {
			return getServerURL() + CONTEXT_PATH +  PEOPLELINK + "/avatar";
		}

		if (StringUtils.isBlank(email)) {
			return "https://www.gravatar.com/avatar?d=retro&size=400";
		}
		return "https://www.gravatar.com/avatar/" + Utils.md5(email.toLowerCase()) + "?size=400&d=retro";
	}

	public boolean isLink(String link) {
		return StringUtils.contains(link, "gravatar.com");
	}
}
