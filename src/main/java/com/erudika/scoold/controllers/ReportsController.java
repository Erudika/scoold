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
package com.erudika.scoold.controllers;

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.core.Profile;
import static com.erudika.scoold.core.Profile.Badge.REPORTER;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.utils.ScooldUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import static com.erudika.scoold.ScooldServer.REPORTSLINK;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/reports")
public class ReportsController {

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public ReportsController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping
	public String get(@RequestParam(required = false, defaultValue = Config._TIMESTAMP) String sortby,
			HttpServletRequest req, Model model) {
		if (!utils.isMod(utils.getAuthUser(req))) {
			return "redirect:" + REPORTSLINK;
		}
		Pager itemcount = utils.getPager("page", req);
		itemcount.setSortby(sortby);
		List<Report> reportslist = pc.findQuery(Utils.type(Report.class), "*", itemcount);
		model.addAttribute("path", "reports.vm");
		model.addAttribute("title", utils.getLang(req).get("reports.title"));
		model.addAttribute("reportsSelected", "navbtn-hover");
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("reportslist", reportslist);
		return "base";
	}

	@GetMapping("/form")
	public String getReportForm(@RequestParam String parentid, @RequestParam String type,
			@RequestParam(required = false) String link, Model model) {
		model.addAttribute("getreportform", true);
		model.addAttribute("parentid", parentid);
		model.addAttribute("type", type);
		model.addAttribute("link", link);
		return "reports";
	}

	@PostMapping
	public void create(HttpServletRequest req, HttpServletResponse res, Model model) {
		Report rep = utils.populate(req, new Report(), "link", "description", "parentid", "subType", "authorName");
		Map<String, String> error = utils.validate(rep);
		if (error.isEmpty()) {
			if (utils.isAuthenticated(req)) {
				Profile authUser = utils.getAuthUser(req);
				rep.setAuthorName(authUser.getName());
				rep.setCreatorid(authUser.getId());
				utils.addBadgeAndUpdate(authUser, REPORTER, true);
			} else {
				//allow anonymous reports
				rep.setAuthorName(utils.getLang(req).get("anonymous"));
			}
			rep.create();
			model.addAttribute("newreport", rep);
			res.setStatus(200);
		} else {
			model.addAttribute("error", error);
			res.setStatus(400);
		}
	}

	@PostMapping("/cspv")
	@SuppressWarnings("unchecked")
	public void createCSPViolationReport(HttpServletRequest req, HttpServletResponse res) throws IOException {
		if (Config.getConfigBoolean("csp_reports_enabled", false)) {
			Report rep = new Report();
			rep.setDescription("CSP Violation Report");
			rep.setSubType(Report.ReportType.OTHER);
			rep.setLink("-");
			rep.setAuthorName(Config.APP_NAME);
			Map<String, Object> body = ParaObjectUtils.getJsonReader(Map.class).readValue(req.getInputStream());
			if (body != null && !body.isEmpty()) {
				rep.setProperties((Map<String, Object>) (body.containsKey("csp-report") ? body.get("csp-report") : body));
				if (rep.getProperties().containsKey("document-uri")) {
					rep.setLink((String) rep.getProperties().get("document-uri"));
				} else if (rep.getProperties().containsKey("source-file")) {
					rep.setLink((String) rep.getProperties().get("source-file"));
				}
				body.remove("original-policy");
				body.put("userAgent", req.getHeader("User-Agent") + "");
				body.put("userHost", req.getRemoteHost() + "");
			}
			rep.create();
			res.setStatus(200);
		} else {
			res.setStatus(403);
		}
	}

	@PostMapping("/{id}/close")
	public String close(@PathVariable String id, @RequestParam(required = false, defaultValue = "") String solution,
			HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			Report report = pc.read(id);
			if (report != null && !report.getClosed() && utils.isMod(authUser)) {
				report.setClosed(true);
				report.setSolution(solution);
				report.update();
			}
		}
		if (!utils.isAjaxRequest(req)) {
			return "redirect:" + REPORTSLINK;
		}
		return "base";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable String id, HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			Report rep = pc.read(id);
			if (rep != null && utils.isAdmin(authUser)) {
				rep.delete();
			}
		}
		if (!utils.isAjaxRequest(req)) {
			return "redirect:" + REPORTSLINK;
		}
		return "base";
	}
}
