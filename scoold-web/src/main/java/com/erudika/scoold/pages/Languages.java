/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.para.utils.Config;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Languages extends Base{

	public String title;
	public Map<String, Integer> langProgressMap;

	public Languages() {
		title = lang.get("translate.select");

		addModel("allLocales", new TreeMap<String, Locale>(langutils.getAllLocales()));
		langProgressMap = langutils.getTranslationProgressMap(Config.APP_NAME_NS);
	}

	public void onPost() {
		if (param("setlocale")) {
			String loc = getParamValue("setlocale");
			setCurrentLocale(loc, true);
			setRedirect(languageslink);
		}
	}
}
