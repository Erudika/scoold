/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.para.i18n.LanguageUtils;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author alexb
 */
public class Languages extends BasePage{

	public String title;
	public Map<String, Object> langProgressMap;

	public Languages() {
		title = lang.get("translate.select");

		addModel("allLocales", new TreeMap<String, Locale>(LanguageUtils.ALL_LOCALES));
		langProgressMap = LanguageUtils.getTranslationProgressMap();
	}

	public void onPost() {
		if(param("setlocale")) {
			String loc = getParamValue("setlocale");
			setCurrentLocale(loc, true);
			setRedirect(languageslink);
		}
	}
}
