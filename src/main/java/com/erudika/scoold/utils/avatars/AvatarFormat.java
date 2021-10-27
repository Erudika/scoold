package com.erudika.scoold.utils.avatars;

public enum AvatarFormat {
	Square25(25),
	Square32(32),
	Square50(50),
	Square127(127),
	Profile(404);

	private final int size;

	AvatarFormat(int size) {
		this.size = size;
	}

	public int getSize() {
		return this.size;
	}
}
