/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scoold.pages;

import com.scoold.core.ScooldObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import com.scoold.core.School;
import com.scoold.core.Searchable;
import com.scoold.db.AbstractDAOUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author alexb
 */
public class Admin extends BasePage {

	public String title;

	public Admin() {
		title = "";
		if (!authenticated || !authUser.isAdmin()) {
			setRedirect(HOMEPAGE);
		}
	}

	public void onPost() {
		String ref = param("returnto") ? getParamValue("returnto") : adminlink;

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
		} else {
			long startTime = System.nanoTime();
			if (param("createschools")) {
				if(daoutils.getBeanCount(School.class) == 0L){
					createSchools();
					logger.log(Level.WARNING, "Executed createSchools().");
				}
			} else if (param("reindex")) {
				reindex(getParamValue("reindex"));
				logger.log(Level.WARNING, "Executed reindex().");
			}
			long estimatedTime = System.nanoTime() - startTime;
			logger.log(Level.WARNING, "Time {0}", new Object[]{estimatedTime});
		}

		setRedirect(ref);
	}

	private void reindex(String what) {
		if (what == null) {
			return;
		}
		ArrayList<Searchable<?>> list = new ArrayList<Searchable<?>>();
		// TODO: all
		if (what.startsWith("school")) {
		} else if (what.startsWith("classunit")) {
		} else if (what.startsWith("question")) {
		} else if (what.startsWith("answer")) {
		} else if (what.startsWith("post")) {
		} else if (what.startsWith("feedback")) {
		} else if (what.startsWith("user")) {
		} else if (what.startsWith("tag")) {
		}

		for (Searchable<?> searchable : list) {
			searchable.index();
		}
	}

	private void createSchools() {
		String filepath = IN_PRODUCTION ? "/home/ubuntu/schools.txt"
				: "/Users/alexb/Desktop/schools.txt";
		File file = new File(filepath);
		int i = 1;
		try {
			List<String> lines = FileUtils.readLines(file, "UTF-8");

			for (String line : lines) {
				if (StringUtils.isBlank(line)) {
					continue;
				}
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
		} catch (Exception ex) {
			Logger.getAnonymousLogger().log(Level.SEVERE, null, ex);
		}
	}
	
//	private void filldb(){
//		//		Client searchClient = (Client) getContext().getServletContext().
////				getAttribute("searchClient");
////		new DeleteIndexRequestBuilder(searchClient.admin().indices(), "scoold").execute();
//
//		LinkedList<User> users = new LinkedList<User>();
//		User first = null;
//		for (int k = 0; k < 10; k++) {
//			User user = new User();
//			user.setAboutme(RandomStringUtils.randomAlphabetic(55));
//			user.setEmail(System.currentTimeMillis()+"@user.com");
//			user.setFullname(RandomStringUtils.randomAlphabetic(5)+ " "+
//					RandomStringUtils.randomAlphabetic(10));
//			Long id = user.create();
//			users.add(user);
//			logger.log(Level.WARNING, "created user {0}", id);
//
//			if(k > 0){
//				User u = users.poll();
//				user.addContact(u);
//				users.add(u);
//			}else{
//				first = user;
//			}
//		}
//		User u = users.poll();
//		first.addContact(u);
//		users.add(u);
//
//		// generate schools
//		ArrayList<School> schools = new ArrayList<School>();
//
//		for (int k = 0; k < 10; k++) {
//			School school = new School();
//			school.setName("New school " + k);
//			school.setLocation("Sofia, Bulgaria");
//			school.setType(School.SchoolType.HIGHSCHOOL.toString());
//			Long id = school.create();
//
////			generate questions for school
//			for (int x = 0; x < 2; x++) {
//				Post question = new Post();
//				question.setBody(RandomStringUtils.randomAlphabetic(20));
//				question.setTitle(RandomStringUtils.randomAlphabetic(10));
//				question.setTags("test, proba");
//				question.setPostType(PostType.QUESTION);
//				question.setUserid(users.get(getIndex1(users.size())).getId());
//				question.setParentuuid(school.getUuid());
//				Long qid = question.create();
//				logger.log(Level.WARNING, "created question {0}", qid);
//			}
//			resetIndex1();
//
//			schools.add(school);
//			logger.log(Level.WARNING, "created school {0}", id);
//		}
//
////			 generate classes
//		for (int k = 0; k < 10; k++) {
//			Classunit classunit = new Classunit();
//			classunit.setIdentifier(RandomStringUtils.randomAlphabetic(10));
//			classunit.setGradyear(2011);
//			classunit.setSchoolid(schools.get(getIndex1(schools.size())).getId());
//			Long id = classunit.create();
//			logger.log(Level.WARNING, "created class {0}, bbid {1}", 
//					new Object[]{id, classunit.getBlackboardid()});
//
//			for (int x = 0; x < 5; x++) {
//				classunit.linkToUser(users.get(getIndex2(users.size())).getId());
//			}
//		}
//		resetIndex1();
//		resetIndex2();
//	}
//
//	int i = 0;
//	private int getIndex1(int size){
//		if(++i >= size) i = 0;
//		return i;
//	}
//
//	private void resetIndex1(){i = 0;}
//
//	int j = 0;
//	private int getIndex2(int size){
//		if(++j >= size) j = 0;
//		return j;
//	}
//
//	private void resetIndex2(){j = 0;}
}
