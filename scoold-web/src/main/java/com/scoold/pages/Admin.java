/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scoold.pages;

import com.scoold.core.ScooldObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import com.scoold.core.School;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractDAOUtils;
import com.scoold.util.ScooldAppListener;
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
		
		TransportClient client = (TransportClient) ScooldAppListener.getSearchClient();
		
		if(client != null){
			addModel("eshosts", client.transportAddresses());
		}else{
			addModel("eshosts", "ElastiSearch not available.");
		}
		
		schoolcount = daoutils.getBeanCount(School.classtype);
		addModel("indexExists", daoutils.existsIndex());
		addModel("esindex", AbstractDAOFactory.INDEX_NAME);
		addModel("sysprops", System.getProperties());
		addModel("syscolumns", daoutils.getSystemColumns());
	}
	
	public void onPost() {
		if (param("confirmdelete")) {
			String classname = StringUtils.capitalize(getParamValue("confirmdelete"));
			Long id = NumberUtils.toLong(getParamValue("id"));
			ScooldObject sobject = AbstractDAOUtils.getObject(id, classname);
			if (sobject != null) {
				sobject.delete();

				logger.log(Level.INFO, "{0} #{1} deleted {3} #{4}", new Object[]{
							authUser.getFullname(),
							authUser.getId(),
							sobject.getClass().getName(),
							sobject.getId()
						});
			}
		} else if(param("reindex")) {
			daoutils.reindexAll();
		} else if(param("deleteindex")) {
			daoutils.deleteIndex();
		} else if(param("sysparam")) {
			int ttl = NumberUtils.toInt(getParamValue("ttl"), 0);
			daoutils.setSystemColumn(getParamValue("name"), getParamValue("value"), ttl);
		} else {
			if (param("createschools") && schoolcount == 0L) {
				long startTime = System.nanoTime();
				createSchools();
				logger.log(Level.WARNING, "Executed createSchools().");
				long estimatedTime = System.nanoTime() - startTime;
				logger.log(Level.WARNING, "Time {0}", new Object[]{estimatedTime});
			} 
		}

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
					Long id = s.create();
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
