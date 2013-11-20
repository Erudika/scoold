/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.pages;

import com.erudika.para.core.ParaObject;
import com.erudika.para.utils.Config;
import org.apache.commons.lang3.StringUtils;
import com.erudika.scoold.core.School;
import java.io.File;
import java.util.List;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class Admin extends Base {

	public String title;
	
	public Admin() {
		title = lang.get("admin.title");
		if (!authenticated || !authUser.isAdmin()) {
			setRedirect(HOMEPAGE);
			return;
		}
		
		addModel("eshosts", search.getSearchClusterMetadata());
		addModel("indexExists",  search.existsIndex(Config.INDEX_ALIAS));
		addModel("esindex", search.getIndexName());
	}
	
	public void onPost() {
		if (param("confirmdelete")) {
			String id = getParamValue("id");
			ParaObject sobject = dao.read(id);
			if (sobject != null) {
				sobject.delete();

				logger.info("{0} #{1} deleted {3} #{4}", new Object[]{
							authUser.getName(),
							authUser.getId(),
							sobject.getClass().getName(),
							sobject.getId()
						});
			}
		}
			
		if(param("reindex")) {
			String id = getParamValue("reindex");
			dao.update(dao.read(id));
		} else if(param("optimizeindex")) {
			search.optimizeIndex(Config.INDEX_ALIAS);
		} else if(param("rebuildindex")) {
			search.rebuildIndex(Config.INDEX_ALIAS + "_" + System.currentTimeMillis());
		} else if(param("deleteindex")) {
			search.deleteIndex(search.getIndexName());
		}
//		if (param("createschools") && schoolcount == 0L) {
//			long startTime = System.nanoTime();
//			createSchools();
//			logger.log(Level.WARNING, "Executed createSchools().");
//			long estimatedTime = System.nanoTime() - startTime;
//			logger.log(Level.WARNING, "Time {0}", new Object[]{estimatedTime});
//		} 

		setRedirect(adminlink);
	}

	private void createSchools() {
		String filepath = IN_PRODUCTION ? "/home/ubuntu/schools.txt"
				: "/Users/alexb/Desktop/schools.txt";
		File file = new File(filepath);
		int i = 1;
		try {
			List<String> lines = FileUtils.readLines(file, "UTF-8");

			for (String line : lines) {
				if (!StringUtils.isBlank(line)) {
					School s = new School();
					line = line.trim();

					String[] starr = line.split("\\|");
					s.setType(starr[0]);
					s.setName(starr[1]);
					s.setLocation(starr[2]);
					if (starr.length > 3) {
						s.setContacts(starr[3]);
					}
					String id = s.create();
					logger.info("{0}. created school {1} in {2}", new Object[]{i, id, starr[2]});
					i++;
				}
			}
		} catch (Exception ex) {
			logger.error(null, ex);
		}
	}
}
