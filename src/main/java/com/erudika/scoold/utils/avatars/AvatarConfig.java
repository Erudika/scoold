package com.erudika.scoold.utils.avatars;

import com.erudika.para.utils.Config;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;

@Component
@Singleton
public class AvatarConfig {
	public boolean isAvatarValidationEnabled() {
		return Config.getConfigBoolean("avatar_validation_enabled", false); // this should be deleted in the future
	}
}
