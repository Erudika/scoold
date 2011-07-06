/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db;

import com.scoold.core.Report;
import java.util.ArrayList;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public abstract class AbstractReportDAO<T extends Report, PK>
		implements GenericDAO <Report, Long>{

	public abstract ArrayList<T> readAllSortedBy(String field, MutableLong page, MutableLong itemcount, boolean desc, int maxPerPage);

}
