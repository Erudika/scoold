
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

import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.scoold.utils.ScooldUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.apache.commons.lang3.Strings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class ErrorController {

	private final ScooldUtils utils;

	public ErrorController(ScooldUtils utils) {
		this.utils = utils;
	}

	@GetMapping("/error/{code}")
	public String get(@PathVariable String code, HttpServletRequest req, HttpServletResponse res, Model model) throws IOException {
		model.addAttribute("path", "error.vm");
		model.addAttribute("title", utils.getLang(req).get("error.title"));
		model.addAttribute("status", req.getAttribute("jakarta.servlet.error.status_code"));
		model.addAttribute("reason", req.getAttribute("jakarta.servlet.error.message"));
		model.addAttribute("code", code);

		if (Strings.CS.startsWith((CharSequence) req.getAttribute("jakarta.servlet.forward.request_uri"), "/api/")) {
			res.setContentType(MediaType.APPLICATION_JSON_VALUE);
			ParaObjectUtils.getJsonWriterNoIdent().writeValue(res.getOutputStream(),
					Collections.singletonMap("error", code + " - " + req.getAttribute("jakarta.servlet.error.message")));
		}
		return "base";
	}
}
