/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db.cassandra;

import com.scoold.core.Report;
import com.scoold.db.AbstractReportDAO;
import java.util.logging.Logger;

/**
 *
 * @author alexb
 */
final class CasReportDAO<T, PK> extends AbstractReportDAO<Report, Long> {

    private static final Logger logger = Logger.getLogger(CasReportDAO.class.getName());
	private CasDAOUtils cdu = (CasDAOUtils) CasDAOFactory.getInstance().getDAOUtils();
	
	public CasReportDAO(){
	}

	public Report read (Long id) {
		return cdu.read(Report.class, id.toString());
	}

	public Long create (Report newInstance) {
		return cdu.create(newInstance);
	}

	public void update(Report transientObject) {
		cdu.update(transientObject);
	}

	public void delete (Report persistentObject) {
		cdu.delete(persistentObject);
	}
}
