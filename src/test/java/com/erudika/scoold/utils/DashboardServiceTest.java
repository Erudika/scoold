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

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DashboardServiceTest {

	@Test
	public void deltaUsesPreviousPeriod() {
		assertEquals(25.0, DashboardService.delta(125, 100), 0.001);
		assertEquals(-50.0, DashboardService.delta(50, 100), 0.001);
	}

	@Test
	public void deltaHandlesEmptyPreviousPeriod() {
		assertEquals(100.0, DashboardService.delta(3, 0), 0.001);
		assertEquals(0.0, DashboardService.delta(0, 0), 0.001);
	}

	@Test
	public void normalizePeriodRejectsUnknownValues() {
		assertEquals("30d", DashboardService.normalizePeriod(null));
		assertEquals("30d", DashboardService.normalizePeriod("year-to-date"));
		assertEquals("7d", DashboardService.normalizePeriod("7d"));
	}
}
