/*
 * Copyright 2013-2021 Erudika. https://erudika.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For issues and patches go to: https://github.com/erudika
 */
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
