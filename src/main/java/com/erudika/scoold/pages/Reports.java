/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Classunit;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.core.School;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Reports extends Base{

	public String title;
	public School showMergeSchool;
	public Classunit showMergeClass;
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

		if (param("merge") && param("id") && inRole("admin")) {
			String id = getParamValue("id");
			if (getParamValue("merge").equals("school")) {
				showMergeSchool = pc.read(id);
			} else if (getParamValue("merge").equals("classunit")) {
				showMergeClass = pc.read(id);
			}
		} else {
			reportslist = pc.findQuery(Utils.type(Report.class), "*", itemcount);
		}
	}

	public void onPost() {
		if (param("confirmmerge") && req.getHeader("Referer") != null && inRole("admin")) {
			String what = getParamValue("confirmmerge");

			String id1 = getParamValue("id1"); // primary id
			String id2 = getParamValue("id2"); // duplicate id

			boolean idsNotNull = !id1.equals(0L) && !id2.equals(0L);
			boolean done = false;
			String link = "";
			if ("classunit".equals(what) && idsNotNull) {
				//merge classes
				Classunit c1 = pc.read(id1);
				done = c1 != null && c1.mergeWith(id2);
				link = classlink;
			} else if ("school".equals(what) && idsNotNull) {
				//merge schools
				School s1 = pc.read(id1);
				done = s1 != null && s1.mergeWith(id2);
				link = schoollink;
			}

			if (done) setRedirect(link+"/"+id1+"?mergesuccess=true");
			else setRedirect(link+"/"+id2+"?mergefail=true");
		} else if (param("close")) {
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
