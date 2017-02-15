/*
 * Copyright 2013-2017 Erudika. https://erudika.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For issues and patches go to: https://github.com/erudika
 */
package com.erudika.scoold.controllers;

import java.util.Map;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class LanguagesController {

	public String title;
	public Map<String, Integer> langProgressMap;

//	public LanguagesController() {
//		title = lang.get("translate.select");
//
//		addModel("allLocales", new TreeMap<String, Locale>(langutils.getAllLocales()));
//		langProgressMap = langutils.getTranslationProgressMap();
//	}
//
//	public void onPost() {
//		if (param("setlocale")) {
//			String loc = getParamValue("setlocale");
//			Locale locale = getCurrentLocale(loc, true);
//			if (locale != null) {
//				int maxAge = 60 * 60 * 24 * 365;  //1 year
//				Utils.setRawCookie(LOCALE_COOKIE, locale.getLanguage(), req, res, false, maxAge);
//			}
//			setRedirect(languageslink);
//		}
//	}
}
