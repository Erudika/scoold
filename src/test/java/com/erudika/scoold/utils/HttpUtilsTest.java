/*
 * Copyright 2013-2026 Erudika. https://erudika.com
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

import static com.erudika.scoold.utils.HttpUtils.getDefaultPort;
import java.net.URI;
import static org.junit.Assert.*;
import org.junit.Test;

public class HttpUtilsTest {

	@Test
	public void testIsSameOrigin_SameServer() {
		assertTrue(isSameOrigin(
				"https://example.com/scoold/questions",
				"https://example.com"));
	}

	@Test
	public void testIsSameOrigin_SameServerDifferentPath() {
		assertTrue(isSameOrigin(
				"https://example.com/other/path",
				"https://example.com"));
	}

	@Test
	public void testIsSameOrigin_ExploitSubdomain() {
		assertFalse(isSameOrigin(
				"https://example.com.evil.com/steal",
				"https://example.com"));
	}

	@Test
	public void testIsSameOrigin_DifferentScheme() {
		assertFalse(isSameOrigin(
				"http://example.com/scoold",
				"https://example.com"));
	}

	@Test
	public void testIsSameOrigin_DifferentHost() {
		assertFalse(isSameOrigin(
				"https://evil.com/path",
				"https://example.com"));
	}

	@Test
	public void testIsSameOrigin_ExplicitPort() {
		assertTrue(isSameOrigin(
				"https://example.com:443/scoold",
				"https://example.com"));
	}

	@Test
	public void testIsSameOrigin_DifferentPort() {
		assertFalse(isSameOrigin(
				"https://example.com:8443/admin",
				"https://example.com"));
	}

	@Test
	public void testIsSameOrigin_CredentialsInUrl() {
		assertFalse(isSameOrigin(
				"https://evil.com@example.com/scoold",
				"https://example.com"));
	}

	@Test
	public void testIsSameOrigin_NullHost() {
		assertFalse(isSameOrigin("", "https://example.com"));
	}

	@Test
	public void testIsSameOrigin_InvalidUri() {
		assertFalse(isSameOrigin("///bad", "https://example.com"));
	}

	@Test
	public void testGetDefaultPort() {
		assertEquals(443, getDefaultPort("https"));
		assertEquals(80, getDefaultPort("http"));
		assertEquals(-1, getDefaultPort("ftp"));
	}

	@Test
	public void testIsSameOrigin_HttpDefaultPort() {
		assertTrue(isSameOrigin(
				"http://example.com/scoold",
				"http://example.com"));
		assertTrue(isSameOrigin(
				"http://example.com:80/scoold",
				"http://example.com"));
	}

	@Test
	public void testIsSameOrigin_StartsWithAttack() {
		assertFalse(isSameOrigin(
				"https://example.com@evil.com/steal",
				"https://example.com"));
	}

	private static boolean isSameOrigin(String uri1, String uri2) {
		return HttpUtils.isSameOrigin(URI.create(uri1), URI.create(uri2));
	}
}
