/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db;

import com.scoold.core.Translation;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public abstract class AbstractTranslationDAO<T, PK> implements GenericDAO<Translation, Long>{

	public abstract ArrayList<T> readAllTranslationsForKey(String locale, String key,
			MutableLong pagenum, MutableLong itemcount);

	public abstract void approve(T t);
	public abstract void disapprove(T t);
	public abstract Map<String, Long> readApprovedIdsForLocale(String locale);
	public abstract Map<String, Integer> calculateProgressForAll(int total);
	public abstract void disapproveAllForKey(String key);
	public abstract Map<String, String> readLanguage(Locale locale);
	public abstract Map<String, Integer> readTranslationCountForAllKeys(String locale, ArrayList<String> keys);
}
