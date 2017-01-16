/*
 * Copyright 2013-2017 Erudika. https://erudika.com
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
package com.erudika.scoold.pages;

import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Report;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Reports extends Base{

	public String title;
	public String showtype;
	public List<Report> reportslist;

	public Reports() {
		title = lang.get("reports.title");
		if (param("merge") && param("id") && inRole("admin")) {
			title += " - Merge operation";
		} else {
			title += lang.get("reports.title");
		}
	}

	public void onGet() {
		if (param("ref")) {
			setRedirect(getParamValue("ref"));
			return;
		}

		reportslist = pc.findQuery(Utils.type(Report.class), "*", itemcount);
	}

	public void onPost() {
		if (param("close")) {
			Report rep = pc.read(getParamValue("id"));
			if (rep != null && !rep.getClosed()) {
				String sol = getParamValue("solution");
				if (StringUtils.length(sol) >= 5 && StringUtils.length(sol) < 255) {
					rep.setClosed(true);
					rep.setSolution(sol);
					rep.update();
				}
			}
			if (!isAjaxRequest()) setRedirect(reportslink);
		} else if (param("delete") && inRole("admin")) {
			Report rep = pc.read(getParamValue("id"));
			if (rep != null) {
				rep.delete();
			}
			if (!isAjaxRequest()) setRedirect(reportslink);
		}
	}
}
