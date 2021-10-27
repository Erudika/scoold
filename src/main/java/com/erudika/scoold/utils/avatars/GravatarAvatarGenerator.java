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

	public String getLink(Profile profile) {
		String email = (profile == null || profile.getUser() == null) ? "" : profile.getUser().getEmail();
		return getLink(email);
	}

	public String getLink(String email) {
		return URL_BASE + computeToken(email) + "?size=400&d=retro";
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
