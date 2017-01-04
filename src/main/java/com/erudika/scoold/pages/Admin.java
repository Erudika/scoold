/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.pages;

import com.erudika.para.core.ParaObject;
import com.erudika.para.utils.Config;
import java.util.Collections;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Admin extends Base {

	public String title;

	public Admin() {
		title = lang.get("admin.title");
		if (!authenticated || !authUser.isAdmin()) {
			setRedirect(HOMEPAGE);
			return;
		}

		addModel("eshosts", Collections.emptyMap());
		addModel("indexExists",  true);
		addModel("esindex", Config.APP_NAME_NS);
	}

	public void onPost() {
		if (param("confirmdelete")) {
			String id = getParamValue("id");
			ParaObject sobject = pc.read(id);
			if (sobject != null) {
				sobject.delete();

				logger.info("{} #{} deleted {} #{}",
						authUser.getName(), authUser.getId(), sobject.getClass().getName(), sobject.getId());
			}
		}

		if (param("reindex")) {
			String id = getParamValue("reindex");
			pc.update(pc.read(id));
		} else if (param("optimizeindex")) {
//			ElasticSearchUtils.optimizeIndex(Config.APP_NAME_NS);
		} else if (param("rebuildindex")) {
			logger.info("Not supported.");
		} else if (param("deleteindex")) {
			logger.info("Not supported.");
		}
//		if (param("createschools") && schoolcount == 0L) {
//			long startTime = System.nanoTime();
//			createSchools();
//			logger.log(Level.WARNING, "Executed createSchools().");
//			long estimatedTime = System.nanoTime() - startTime;
//			logger.log(Level.WARNING, "Time {}", estimatedTime);
//		}

		setRedirect(adminlink);
	}
}
