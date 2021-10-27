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
		String email = (profile == null || profile.getUser() == null) ? "" : profile.getUser().getEmail();
		return getLink(email, format);
	}

	public String getLink(String email, AvatarFormat format) {
		return URL_BASE + computeToken(email) + "?s=" + format.getSize() + "&r=g&d=" + config.gravatarPattern();
	}

	private String computeToken(String email) {
		if (StringUtils.isBlank(email)) {
			return "";
		}

		return Utils.md5(email.toLowerCase());
	}

	public boolean isLink(String link) {
		return StringUtils.contains(link, "gravatar.com");
	}
}
