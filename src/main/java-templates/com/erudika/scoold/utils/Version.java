/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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
package com.erudika.scoold.utils;

/**
 * GENERATED CLASS. DO NOT MODIFY!
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public final class Version {

	private static final String VERSION = "${project.version}";
	private static final String GROUPID = "${project.groupId}";
	private static final String ARTIFACTID = "${project.artifactId}";
	private static final String GIT = "${project.scm.developerConnection}";
	private static final String GIT_BRANCH = "${scmBranch}";
	private static final String REVISION = "${buildNumber}";

	public static String getVersion() {
		return VERSION;
	}

	public static String getArtifactId() {
		return ARTIFACTID;
	}

	public static String getGroupId() {
		return GROUPID;
	}

	public static String getGIT() {
		return GIT;
	}

	public static String getRevision() {
		return REVISION;
	}

	public static String getGITBranch() {
		return GIT_BRANCH;
	}
}
