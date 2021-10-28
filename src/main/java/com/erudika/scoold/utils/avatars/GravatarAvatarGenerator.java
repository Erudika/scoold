package com.erudika.scoold.utils.avatars;

import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Profile;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;

@Component
@Singleton
public class GravatarAvatarGenerator {
	private static final String URL_BASE = "https://www.gravatar.com/avatar/";
	private final AvatarConfig config;

	public GravatarAvatarGenerator(AvatarConfig config) {
		this.config = config;
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
		return url + (url.endsWith("?") ? "&" : "?") + "s=" + format.getSize() + "&r=g&d=" + config.gravatarPattern();
	}

	public boolean isLink(String link) {
		return StringUtils.contains(link, "gravatar.com");
	}
}
