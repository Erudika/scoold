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
package com.erudika.scoold.controllers;

import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldServer;
import com.erudika.scoold.utils.ScooldUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class ApiDocsController {

	private final ScooldUtils utils;

	@Inject
	public ApiDocsController(ScooldUtils utils) {
		this.utils = utils;
	}

	@GetMapping({"/apidocs", "/api.html"})
	public String get(HttpServletRequest req, Model model) {
		if (!utils.isApiEnabled()) {
			return "redirect:" + ScooldServer.HOMEPAGE;
		}
		if (req.getServletPath().endsWith(".html")) {
			return "redirect:" + ScooldServer.APIDOCSLINK;
		}
		model.addAttribute("path", "apidocs.vm");
		model.addAttribute("title", "API documentation");
		return "base";
	}

	@ResponseBody
	@GetMapping(path = "/api.json", produces = "text/javascript")
	public ResponseEntity<String> json(HttpServletRequest req, HttpServletResponse res) throws JsonProcessingException {
		if (!utils.isApiEnabled()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		Yaml yaml = new Yaml();
		String yml = utils.loadResource("templates/api.yaml");
		yml = StringUtils.replaceOnce(yml, "{{serverUrl}}", ScooldUtils.getConfig().serverUrl());
		yml = StringUtils.replaceOnce(yml, "{{contextPath}}", ScooldUtils.getConfig().serverContextPath());
		String result = ParaObjectUtils.getJsonWriter().writeValueAsString(yaml.load(yml));
		return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS)).eTag(Utils.md5(result)).body(result);
	}
}
