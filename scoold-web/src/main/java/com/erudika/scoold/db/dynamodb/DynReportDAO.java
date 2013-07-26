/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.db.dynamodb;

import com.erudika.scoold.core.Report;
import com.erudika.scoold.db.AbstractReportDAO;

/**
 *
 * @author alexb
 */
final class DynReportDAO<T> extends AbstractReportDAO<Report> {

	
	public DynReportDAO(){
	}

	public Report read (String id) {
//		return cdu.read(Report.class, id.toString());
		return null;
	}

	public String create (Report newInstance) {
//		return cdu.create(newInstance);
		return null;
	}

	public void update(Report transientObject) {
//		cdu.update(transientObject);
	}

	public void delete (Report persistentObject) {
//		cdu.delete(persistentObject);
	}
}
