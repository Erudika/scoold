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
package com.erudika.scoold.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.erudika.scoold.utils.ScooldUtils;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class OnboardingControllerSecurityTest {

	private String previousSetupRequired;
	private OnboardingController controller;

	@Before
	public void setUp() {
		previousSetupRequired = System.getProperty("scoold.setuprequired");
		ScooldUtils.setSetupRequired(true);
		controller = new OnboardingController(null, null);
	}

	@After
	public void tearDown() {
		if (previousSetupRequired == null) {
			System.clearProperty("scoold.setuprequired");
		} else {
			System.setProperty("scoold.setuprequired", previousSetupRequired);
		}
	}

	@Test
	public void blocksPrivateParaAndSmtpTargetsBeforeConnecting() {
		ResponseEntity<Map<String, Object>> paraResult = controller.testParaConnection(
				"http://169.254.169.254:80", "access", "secret");
		ResponseEntity<Map<String, Object>> ipv6ParaResult = controller.testParaConnection(
				"http://[fc00::1]:8080", "access", "secret");
		ResponseEntity<Map<String, Object>> smtpResult = controller.testSmtpConnection(
				"192.168.1.10", 22, "", "", true, false, "");
		ResponseEntity<Map<String, Object>> ipv6SmtpResult = controller.testSmtpConnection(
				"::1", 25, "", "", true, false, "");

		assertEquals(HttpStatus.OK, paraResult.getStatusCode());
		assertEquals(Boolean.FALSE, paraResult.getBody().get("connected"));
		assertEquals(Boolean.FALSE, ipv6ParaResult.getBody().get("connected"));
		assertEquals(HttpStatus.BAD_REQUEST, smtpResult.getStatusCode());
		assertEquals(Boolean.FALSE, smtpResult.getBody().get("connected"));
		assertEquals(HttpStatus.BAD_REQUEST, ipv6SmtpResult.getStatusCode());
		assertEquals(Boolean.FALSE, ipv6SmtpResult.getBody().get("connected"));
	}

	@Test
	public void blocksConnectionTestsAfterSetup() {
		ScooldUtils.setSetupRequired(false);

		ResponseEntity<?> paraResult = controller.testParaConnection("http://8.8.8.8", "access", "secret");
		ResponseEntity<?> smtpResult = controller.testSmtpConnection("8.8.8.8", 587, "", "", true, false, "");

		assertEquals(HttpStatus.NOT_FOUND, paraResult.getStatusCode());
		assertEquals(HttpStatus.NOT_FOUND, smtpResult.getStatusCode());
		assertNull(paraResult.getBody());
		assertNull(smtpResult.getBody());
	}
}
