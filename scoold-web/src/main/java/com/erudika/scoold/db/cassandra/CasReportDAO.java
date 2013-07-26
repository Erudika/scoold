/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.db.cassandra;

import com.erudika.scoold.core.Report;
import com.erudika.scoold.db.AbstractReportDAO;
import java.util.logging.Logger;

/**
 *
 * @author alexb
 */
final class CasReportDAO<T> extends AbstractReportDAO<Report> {

    private static final Logger logger = Logger.getLogger(CasReportDAO.class.getName());
	private CasDAOUtils cdu = new CasDAOUtils(CasDAOFactory.CASSANDRA_PORT);
	
	public CasReportDAO(){
	}

	public Report read (String id) {
		return cdu.read(Report.class, id.toString());
	}

	public String create (Report newInstance) {
		return cdu.create(newInstance);
	}

	public void update(Report transientObject) {
		cdu.update(transientObject);
	}

	public void delete (Report persistentObject) {
		cdu.delete(persistentObject);
	}
}
