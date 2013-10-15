/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class Languages extends BasePage{

	public String title;
	public Map<String, Integer> langProgressMap;

	public Languages() {
		title = lang.get("translate.select");

		addModel("allLocales", new TreeMap<String, Locale>(langutils.getAllLocales()));
		langProgressMap = langutils.getTranslationProgressMap();
	}

	public void onPost() {
		if(param("setlocale")) {
			String loc = getParamValue("setlocale");
			setCurrentLocale(loc, true);
			setRedirect(languageslink);
		}
	}
}
