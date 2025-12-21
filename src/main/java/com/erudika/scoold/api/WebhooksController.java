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
package com.erudika.scoold.api;

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.api.ApiController.logger;
import com.erudika.scoold.utils.ScooldUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang3.Strings;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles various webhook events.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@RestController
@RequestMapping(value = "/webhooks", produces = "application/json")
public class WebhooksController {

	private final ScooldUtils utils;
	private final ParaClient pc;
	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private static String lastConfigUpdate = null;

	@Inject
	public WebhooksController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@PostMapping("/config")
	public void updateConfig(HttpServletRequest req, HttpServletResponse res) throws JsonProcessingException {
		Map<String, Object> entity = readEntity(req);
		if (entity.containsKey("signature") && entity.containsKey("payload") &&
				entity.getOrDefault("event", "").equals("config.update")) {
			String payload = (String) entity.get("payload");
			String signature = (String) entity.get("signature");
			String id = (String) entity.get(Config._ID);
			boolean alreadyUpdated = id.equals(lastConfigUpdate);
			if (Strings.CS.equals(signature, Utils.hmacSHA256(payload, CONF.paraSecretKey())) && !alreadyUpdated) {
				Map<String, Object> configMap = ParaObjectUtils.getJsonReader(Map.class).readValue(payload);
				configMap.entrySet().forEach((entry) -> {
					System.setProperty(entry.getKey(), entry.getValue().toString());
				});
				CONF.store();
				lastConfigUpdate = id;
			}
		}
	}

	private Map<String, Object> readEntity(HttpServletRequest req) {
		try {
			return ParaObjectUtils.getJsonReader(Map.class).readValue(req.getInputStream());
		} catch (Exception ex) {
			logger.error(null, ex);
		}
		return Collections.emptyMap();
	}

	public static void setLastConfigUpdate(String id) {
		lastConfigUpdate = id;
	}
}
