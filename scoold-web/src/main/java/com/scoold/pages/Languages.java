/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import com.scoold.core.Language;
import com.scoold.core.Translation;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author alexb
 */
public class Languages extends BasePage{

	public String title;
	public Map<String, Integer> langProgressMap;

	public Languages() {
		title = lang.get("translate.select");

		addModel("allLocales", new TreeMap<String, Locale>(Language.ALL_LOCALES));
		langProgressMap = Translation.getTranslationDao().calculateProgressForAll(lang.size());
	}

	public void onPost() {
		if(param("setlocale")) {
			String loc = getParamValue("setlocale");
			setCurrentLocale(loc, true);
			setRedirect(languageslink);
		}
	}
}
