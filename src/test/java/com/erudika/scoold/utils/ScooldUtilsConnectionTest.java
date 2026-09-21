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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.erudika.para.core.utils.Para;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ScooldUtilsConnectionTest {

	@Before
	public void setUp() {
		System.setProperty("para.executor_service_enabled", "false");
		System.setProperty("connection_retries_max", "10");
		System.setProperty("connection_retry_interval_sec", "10");
		System.setProperty("scoold.connected", "false");
	}

	@After
	public void tearDown() {
		System.clearProperty("para.executor_service_enabled");
		System.clearProperty("connection_retries_max");
		System.clearProperty("connection_retry_interval_sec");
		System.clearProperty("scoold.connected");
	}

	@Test
	public void failedConnectionDoesNotRetryWhenExecutorIsDisabled() {
		long started = System.nanoTime();

		ScooldUtils.tryConnectToPara(() -> false);

		long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
		assertFalse(ScooldUtils.isConnectedToPara());
		assertTrue("Connection failure must not sleep in Lambda mode.", elapsedMillis < 1000);
		assertFalse(Para.getConfig().executorServiceEnabled());
	}

	@Test
	public void successfulConnectionRestoresConnectedState() {
		System.setProperty("scoold.connected", "false");

		ScooldUtils.tryConnectToPara(() -> true);

		assertTrue(ScooldUtils.isConnectedToPara());
	}
}
