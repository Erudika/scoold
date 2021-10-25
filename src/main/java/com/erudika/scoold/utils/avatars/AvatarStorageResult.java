package com.erudika.scoold.utils.avatars;

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
}
