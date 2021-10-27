package com.erudika.scoold.utils.avatars;

import com.erudika.scoold.core.Profile;

import static com.erudika.scoold.ScooldServer.IMAGESLINK;

public class DefaultAvatarRepository implements AvatarRepository {
	private static final String DEFAULT_URL = IMAGESLINK + "/anon.sgv";

	@Override
	public String getLink(Profile profile, AvatarFormat format) {
		return DEFAULT_URL;
	}

	@Override
	public String getAnonymizedLink(String data) {
		return DEFAULT_URL;
	}

	@Override
	public AvatarStorageResult store(Profile profile, String url) {
		return AvatarStorageResult.failed();
	}
}
