/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import org.apache.commons.lang.math.NumberUtils;
import com.scoold.core.Classunit;
import com.scoold.core.Media;
import com.scoold.core.Report;
import com.scoold.core.School;
import com.scoold.db.AbstractDAOFactory;
import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;
/**
 *
 * @author alexb
 */
public class Reports extends BasePage{

	public String title;
	public School showMergeSchool;
	public Classunit showMergeClass;
	public String showclassname;
	public final int maxPreviewReports = 2;
	public Media showmedia;
	public ArrayList<Report> reportslist;

	public Reports() {
		title = lang.get("reports.title");
		if(param("merge") && param("id") && inRole("admin")){
			title += " - Merge operation";
		}else{
			title += lang.get("reports.title");
		}
	}

	public void onGet(){
		if(param("ref")){
			setRedirect(getParamValue("ref"));
			return;
		}

		if(param("merge") && param("id") && inRole("admin")){
			Long id = NumberUtils.toLong(getParamValue("id"));
			if(getParamValue("merge").equals("school")){
				showMergeSchool = School.getSchoolDao().read(id);
			}else if(getParamValue("merge").equals("classunit")){
				showMergeClass = Classunit.getClassUnitDao().read(id);
			}
		}else{
			pageMacroCode = "#reportspage($reportslist)";
			reportslist = Report.getReportDAO().readAllSortedBy(
					"timestamp", pagenum,
					itemcount, true, AbstractDAOFactory.MAX_ITEMS_PER_PAGE);
		}

		if(param("close")){
			Report rep = Report.getReportDAO().read(NumberUtils.toLong(getParamValue("id")));
			if(rep != null && rep.getClosed()){
				rep.setClosed(false);
				rep.setSolution(null);
				rep.update();
				setRedirect(reportslink);
			}
		}
	}

	public void onPost(){
		if(param("confirmmerge") && req.getHeader("Referer") != null && inRole("admin")){
			String what = getParamValue("confirmmerge");

			Long id1 = NumberUtils.toLong(getParamValue("id1")); // primary id
			Long id2 = NumberUtils.toLong(getParamValue("id2")); // duplicate id

			boolean idsNotNull = !id1.equals(0L) && !id2.equals(0L);
			boolean done = false;
			String link = "";
			if("classunit".equals(what) && idsNotNull){
				//merge classes
				done = Classunit.getClassUnitDao().mergeClasses(id1, id2);
				link = classlink;
			}else if("school".equals(what) && idsNotNull){
				//merge schools
				done = School.getSchoolDao().mergeSchools(id1, id2);
				link = schoollink;
			}

			if(done) setRedirect(link+"/"+id1+"?mergesuccess=true");
			else setRedirect(link+"/"+id2+"?mergefail=true");
		}else if(param("close")){
			Report rep = Report.getReportDAO().read(NumberUtils.toLong(getParamValue("id")));
			if(rep != null && !rep.getClosed()){
				String sol = getParamValue("solution");
				if(StringUtils.length(sol) > 5 && StringUtils.length(sol) < 255){
					rep.setClosed(true);
					rep.setSolution(sol);
					rep.update();
				}
			}
		}else if(param("delete") && inRole("admin")){
			Report rep = Report.getReportDAO().read(NumberUtils.toLong(getParamValue("id")));
			if(rep != null){
				rep.delete();
			}
			if(!isAjaxRequest()) setRedirect(reportslink);
		}
	}
}
