package com.erudika.scoold.utils.avatars;

import java.util.Objects;

public final class AvatarStorageResult {
	private final boolean profileChanged;
	private final boolean userChanged;

	private AvatarStorageResult(boolean profileChanged, boolean userChanged) {
		this.profileChanged = profileChanged;
		this.userChanged = userChanged;
	}

	public static AvatarStorageResult failed() {
		return new AvatarStorageResult(false, false);
	}

	public static AvatarStorageResult userChanged() {
		return new AvatarStorageResult(true, true);
	}

	public static AvatarStorageResult profileChanged() {
		return new AvatarStorageResult(true, false);
	}

	public boolean isProfileChanged() {
		return profileChanged;
	}

	public boolean isUserChanged() {
		return userChanged;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AvatarStorageResult that = (AvatarStorageResult) o;
		return profileChanged == that.profileChanged && userChanged == that.userChanged;
	}

	@Override
	public int hashCode() {
		return Objects.hash(profileChanged, userChanged);
	}
}
