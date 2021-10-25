package com.erudika.scoold.utils.avatars;

import com.erudika.scoold.core.Profile;

public interface AvatarRepository {
	String getLink(Profile user, AvatarFormat format);
	String getAnonymizedLink(String data);
}
