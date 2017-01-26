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
package com.erudika.scoold.utils;

import com.erudika.para.core.Translation;
import com.erudika.para.core.Sysprop;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

/**
 * Utility class for language operations.
 * These can be used to build a crowdsourced translation system.
 * @author Alex Bogdanovski [alex@erudika.com]
 * @see Translation
 */
public class LanguageUtils {

	private static final Logger logger = LoggerFactory.getLogger(LanguageUtils.class);

	private static final HashMap<String, Locale> ALL_LOCALES = new HashMap<String, Locale>();
	private Map<String, String> deflang;
	private String deflangCode;
	private final String keyPrefix = "language".concat(Config.SEPARATOR);
	private final String progressKey = keyPrefix.concat("progress");

	private static final int PLUS = -1;
	private static final int MINUS = -2;

	static {
		for (Locale loc : LocaleUtils.availableLocaleList()) {
			String locstr = loc.getLanguage();
			if (!StringUtils.isBlank(locstr)) {
				ALL_LOCALES.put(locstr, loc);
			}
		}
	}

	/**
	 * Default constructor.
	 * @param search a core search instance
	 * @param dao a core persistence instance
	 */
	public LanguageUtils() {
	}

	/**
	 * Returns a map of all translations for a given language.
	 * Defaults to the default language which must be set.
	 * @param appid appid name of the {@link com.erudika.para.core.App}
	 * @param langCode the 2-letter language code
	 * @return the language map
	 */
	public Map<String, String> readLanguage(String appid, String langCode) {
		if (StringUtils.isBlank(langCode) || !ALL_LOCALES.containsKey(langCode)) {
			return getDefaultLanguage();
		}

		Sysprop s = AppConfig.client().read(keyPrefix.concat(langCode));
		TreeMap<String, String> lang = new TreeMap<String, String>();

		if (s == null || s.getProperties().isEmpty()) {
//			Map<String, Object> terms = new HashMap<String, Object>();
//			terms.put("locale", langCode);
//			terms.put("approved", true);
//			List<Translation> tlist = AppConfig.client().findTerms(Utils.type(Translation.class), terms, true);
//
//			Sysprop saved = new Sysprop(keyPrefix.concat(langCode));
//			lang.putAll(getDefaultLanguage());	// copy default langmap
//			int approved = 0;
//
//			for (Translation trans : tlist) {
//				lang.put(trans.getThekey(), trans.getValue());
//				saved.addProperty(trans.getThekey(), trans.getValue());
//				approved++;
//			}
//			if (approved > 0) {
//				updateTranslationProgressMap(appid, langCode, approved);
//			}
//			AppConfig.client().create(saved);
			return getDefaultLanguage();
		} else {
			Map<String, Object> loaded = s.getProperties();
			for (String key : loaded.keySet()) {
				lang.put(key, loaded.get(key).toString());
			}
		}
		return lang;
	}

	/**
	 * Persists the language map in the data store. Overwrites any existing maps.
	 * @param appid appid name of the {@link com.erudika.para.core.App}
	 * @param langCode the 2-letter language code
	 * @param lang the language map
	 */
	public void writeLanguage(String appid, String langCode, Map<String, String> lang) {
		if (lang == null || lang.isEmpty() || StringUtils.isBlank(langCode) || !ALL_LOCALES.containsKey(langCode)) {
			return;
		}

		// this will overwrite a saved language map!
		Sysprop s = new Sysprop(keyPrefix.concat(langCode));
		Map<String, String> dlang = getDefaultLanguage();
		int approved = 0;

		for (String key : dlang.keySet()) {
			if (lang.containsKey(key)) {
				s.addProperty(key, lang.get(key));
				if (!dlang.get(key).equals(lang.get(key))) {
					approved++;
				}
			} else {
				s.addProperty(key, dlang.get(key));
			}
		}
		if (approved > 0) {
			updateTranslationProgressMap(appid, langCode, approved);
		}
		AppConfig.client().create(s);
	}

	/**
	 * Returns a non-null locale for a given language code.
	 * @param langCode the 2-letter language code
	 * @return a locale. default is English
	 */
	public Locale getProperLocale(String langCode) {
		langCode = StringUtils.substring(langCode, 0, 2);
		langCode = (StringUtils.isBlank(langCode) || !ALL_LOCALES.containsKey(langCode)) ?
				"en" : langCode.trim().toLowerCase();
		return ALL_LOCALES.get(langCode);
	}

	/**
	 * Returns the default language map.
	 * @return the default language map or an empty map if the default isn't set.
	 */
	public Map<String, String> getDefaultLanguage() {
		if (deflang == null) {
			logger.warn("Default language not set.");
			deflang = new HashMap<String, String>();
			getDefaultLanguageCode();
		}
		return deflang;
	}

	/**
	 * Sets the default language map. It is the basis language template which is to be translated.
	 * @param deflang the default language map
	 */
	public void setDefaultLanguage(Map<String, String> deflang) {
		this.deflang = deflang;
	}

	/**
	 * Returns the default language code.
	 * @return the 2-letter language code
	 */
	public String getDefaultLanguageCode() {
		if (deflangCode == null) {
			deflangCode = "en";
		}
		return deflangCode;
	}

	/**
	 * Sets the default language code.
	 * @param langCode the 2-letter language code
	 */
	public void setDefaultLanguageCode(String langCode) {
		this.deflangCode = langCode;
	}

	/**
	 * Returns a list of translations for a specific string.
	 * @param appid appid name of the {@link com.erudika.para.core.App}
	 * @param locale a locale
	 * @param key the string key
	 * @param pager the pager object
	 * @return a list of translations
	 */
	public List<Translation> readAllTranslationsForKey(String appid, String locale, String key, Pager pager) {
		Map<String, Object> terms = new HashMap<String, Object>(2);
		terms.put("thekey", key);
		terms.put("locale", locale);
		return AppConfig.client().findTerms(Utils.type(Translation.class), terms, true, pager);
	}

	/**
	 * Returns the set of all approved translations.
	 * @param appid appid name of the {@link com.erudika.para.core.App}
	 * @param langCode the 2-letter language code
	 * @return a set of keys for approved translations
	 */
	public Set<String> getApprovedTransKeys(String appid, String langCode) {
		HashSet<String> approvedTransKeys = new HashSet<String>();
		if (StringUtils.isBlank(langCode)) {
			return approvedTransKeys;
		}

		for (Map.Entry<String, String> entry : readLanguage(appid, langCode).entrySet()) {
			if (!getDefaultLanguage().get(entry.getKey()).equals(entry.getValue())) {
				approvedTransKeys.add(entry.getKey());
			}
		}
		return approvedTransKeys;
	}

	/**
	 * Returns a map of language codes and the percentage of translated string for that language.
	 * @param appid appid name of the {@link com.erudika.para.core.App}
	 * @return a map indicating translation progress
	 */
	public Map<String, Integer> getTranslationProgressMap(String appid) {
		Sysprop progress = getProgressMap(appid);
		Map<String, Integer> progressMap = new HashMap<String, Integer>(ALL_LOCALES.size());
		for (String key : progress.getProperties().keySet()) {
			progressMap.put(key, (Integer) progress.getProperties().get(key));
		}
		return progressMap;
	}

	/**
	 * Returns a map of all language codes and their locales.
	 * @return a map of language codes to locales
	 */
	public Map<String, Locale> getAllLocales() {
		return ALL_LOCALES;
	}

	/**
	 * Approves a translation for a given language.
	 * @param appid appid name of the {@link com.erudika.para.core.App}
	 * @param langCode the 2-letter language code
	 * @param key the translation key
	 * @param value the translated string
	 * @return true if the operation was successful
	 */
	public boolean approveTranslation(String appid, String langCode, String key, String value) {
		if (langCode == null || key == null || value == null || getDefaultLanguageCode().equals(langCode)) {
			return false;
		}
		Sysprop s = AppConfig.client().read(keyPrefix.concat(langCode));

		if (s != null && !value.equals(s.getProperty(key))) {
			s.addProperty(key, value);
			AppConfig.client().update(s);
			updateTranslationProgressMap(appid, langCode, PLUS);
			return true;
		}
		return false;
	}

	/**
	 * Disapproves a translation for a given language.
	 * @param appid appid name of the {@link com.erudika.para.core.App}
	 * @param langCode the 2-letter language code
	 * @param key the translation key
	 * @return true if the operation was successful
	 */
	public boolean disapproveTranslation(String appid, String langCode, String key) {
		if (langCode == null || key == null || getDefaultLanguageCode().equals(langCode)) {
			return false;
		}
		Sysprop s = AppConfig.client().read(keyPrefix.concat(langCode));
		String defStr = getDefaultLanguage().get(key);

		if (s != null && !defStr.equals(s.getProperty(key))) {
			s.addProperty(key, defStr);
			AppConfig.client().update(s);
			updateTranslationProgressMap(appid, langCode, MINUS);
			return true;
		}
		return false;
	}

	/**
	 * Updates the progress for all languages.
	 * @param appid appid name of the {@link com.erudika.para.core.App}
	 * @param langCode the 2-letter language code
	 * @param value {@link #PLUS}, {@link #MINUS} or the total percent of completion (0-100)
	 */
	private void updateTranslationProgressMap(String appid, String langCode, int value) {
		if (getDefaultLanguageCode().equals(langCode)) {
			return;
		}

		double defsize = getDefaultLanguage().size();
		double approved = value;

		Sysprop progress = getProgressMap(appid);

		if (value == PLUS) {
			approved = Math.round((Integer) progress.getProperty(langCode) * (defsize / 100) + 1);
		} else if (value == MINUS) {
			approved = Math.round((Integer) progress.getProperty(langCode) * (defsize / 100) - 1);
		}

		if (approved > defsize) {
			approved = defsize;
		}

		if (defsize == 0) {
			progress.addProperty(langCode, 0);
		} else {
			progress.addProperty(langCode, (int) ((approved / defsize) * 100));
		}
		AppConfig.client().update(progress);
	}

	private Sysprop getProgressMap(String appid) {
		Sysprop progress = AppConfig.client().read(progressKey);
		if (progress == null) {
			progress = new Sysprop(progressKey);
			for (String langCode : ALL_LOCALES.keySet()) {
				progress.addProperty(langCode, 0);
			}
			progress.addProperty(getDefaultLanguageCode(), 100);
			AppConfig.client().create(progress);
		}
		return progress;
	}
}
