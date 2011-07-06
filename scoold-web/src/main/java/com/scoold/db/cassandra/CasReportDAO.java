/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db.cassandra;

import com.scoold.core.Report;
import com.scoold.db.AbstractReportDAO;
import com.scoold.db.cassandra.CasDAOFactory.CF;
import com.scoold.db.cassandra.CasDAOFactory.Column;
import java.util.ArrayList;
import java.util.logging.Logger;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public final class CasReportDAO<T, PK> extends AbstractReportDAO<Report, Long> {

    private static final Logger logger = Logger.getLogger(CasReportDAO.class.getName());
	private CasDAOUtils cdu = new CasDAOUtils();
	
	public CasReportDAO(){
	}

	public Report read (Long id) {
		return cdu.read(Report.class, id.toString(), CasDAOFactory.REPORTS);
	}

	public Report read(String uuid) {
		ArrayList<Report> rep = cdu.readAll(Report.class, uuid,
				CasDAOFactory.REPORTS_UUIDS, CasDAOFactory.REPORTS, String.class,
				null, null, null, 1, true, false, false);

		if(rep == null || rep.isEmpty()) return null;

		return rep.get(0);
	}

	public Long create (Report newInstance) {
		Long id = cdu.create(newInstance, CasDAOFactory.REPORTS);
		if(id != null){
			Mutator<String> mut = CasDAOUtils.createMutator();

			CasDAOUtils.addInsertion(new Column(newInstance.getUuid(),
					CasDAOFactory.REPORTS_UUIDS, id.toString(), id.toString()), mut);

			cdu.addTimesortColumn(null, id, CasDAOFactory.REPORTS_BY_TIMESTAMP, id, null, mut);
			
			mut.execute();
		}

		return id;
	}

	public void update(Report transientObject) {
		cdu.update(transientObject, CasDAOFactory.REPORTS);
	}

	public void delete (Report persistentObject) {
		Report rep = persistentObject;

		if(rep.getUuid() == null){
			rep = cdu.read(Report.class, rep.getId().toString(),
					CasDAOFactory.REPORTS);
		}

		Mutator<String> mut = CasDAOUtils.createMutator();
		String uuid = rep.getUuid();

		cdu.delete(rep, CasDAOFactory.REPORTS, mut);
		cdu.deleteRow(uuid, CasDAOFactory.REPORTS_UUIDS, mut);
		cdu.removeTimesortColumn(null, CasDAOFactory.REPORTS_BY_TIMESTAMP, rep.getId(), mut);

		mut.execute();
	}

	public ArrayList<Report> readAllSortedBy(String sortColumnFamilyName, MutableLong page,
			MutableLong itemcount, boolean reverse, int maxPerPage) {

		CF<Long> colFamily = null;
		//check if the sort order is defined as a column family
		if(sortColumnFamilyName.equalsIgnoreCase("timestamp")){
			colFamily = CasDAOFactory.REPORTS_BY_TIMESTAMP;
		}else{
			return new ArrayList<Report>();
		}

		return cdu.readAll(Report.class, CasDAOFactory.DEFAULT_KEY,  
			colFamily, CasDAOFactory.REPORTS, Long.class, CasDAOUtils.toLong(page),
			page, itemcount, maxPerPage, reverse, false, false);
	}

	public ArrayList<Report> readAllSortedBy(String sortColumnFamilyName, MutableLong page,
			MutableLong itemcount, boolean reverse) {
		return readAllSortedBy(sortColumnFamilyName, page, itemcount, reverse,
				CasDAOFactory.MAX_ITEMS_PER_PAGE);
	}

}
