/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.pages;

import com.erudika.para.core.PObject;
import com.erudika.para.utils.AppListener;
import com.erudika.para.utils.Utils;
import com.erudika.para.utils.Search;
import org.apache.commons.lang3.StringUtils;
import com.erudika.scoold.core.School;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.client.transport.TransportClient;

/**
 *
 * @author alexb
 */
public class Admin extends BasePage {

	public String title;
	public long schoolcount;
	
	public Admin() {
		title = "";
		if (!authenticated || !authUser.isAdmin()) {
			setRedirect(HOMEPAGE);
			return;
		}
		
		TransportClient client = (TransportClient) AppListener.getSearchClient();
		
		if(client != null){
			addModel("eshosts", client.transportAddresses());
		}else{
			addModel("eshosts", "ElastiSearch not available.");
		}
		
		schoolcount = Search.getBeanCount(PObject.classname(School.class));
		addModel("indexExists", Search.existsIndex(Utils.INDEX_ALIAS));
		addModel("esindex", Search.getIndexName());
	}
	
	public void onPost() {
		if (param("confirmdelete")) {
			String classname = StringUtils.capitalize(getParamValue("confirmdelete"));
			String id = getParamValue("id");
			PObject sobject = dao.read(id);
			if (sobject != null) {
				sobject.delete();

				logger.log(Level.INFO, "{0} #{1} deleted {3} #{4}", new Object[]{
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
			Search.optimizeIndex(Utils.INDEX_ALIAS);
		} else if(param("rebuildindex")) {
			Search.rebuildIndex(Utils.INDEX_ALIAS + "_" + System.currentTimeMillis());
		} else if(param("deleteindex")) {
			Search.deleteIndex(Search.getIndexName());
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
					Logger.getLogger(Admin.class.getName()).log(
							Level.INFO, "{0}. created school {1} in {2}", new Object[]{i, id, starr[2]});
					i++;
				}
			}
		} catch (Exception ex) {
			Logger.getAnonymousLogger().log(Level.SEVERE, null, ex);
		}
	}
}
