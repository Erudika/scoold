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

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Translation;
import com.erudika.para.core.Sysprop;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Utility class for language operations.
 * These can be used to build a crowdsourced translation system.
 * @author Alex Bogdanovski [alex@erudika.com]
 * @see Translation
 */
@Component
@Singleton
public class LanguageUtils {

	private static final Logger logger = LoggerFactory.getLogger(LanguageUtils.class);

	private static final Map<String, Locale> ALL_LOCALES = new HashMap<String, Locale>();
	static {
		for (Locale loc : LocaleUtils.availableLocaleList()) {
			String locstr = loc.getLanguage();
			if (!StringUtils.isBlank(locstr)) {
				ALL_LOCALES.put(locstr, loc);
			}
		}
	}

	private static final Map<String, Map<String, String>> LANG_CACHE =
			new ConcurrentHashMap<String, Map<String, String>>(ALL_LOCALES.size());

	private String deflangCode;
	private final String keyPrefix = "language".concat(Config.SEPARATOR);
	private final String progressKey = keyPrefix.concat("progress");

	private static final int PLUS = -1;
	private static final int MINUS = -2;
	private final ParaClient pc;

	/**
	 * Default constructor.
	 * @param pc ParaClient
	 */
	@Inject
	public LanguageUtils(ParaClient pc) {
		this.pc = pc;
	}

	/**
	 * Reads localized strings from a file first, then the DB if a file is not found.
	 * Returns a map of all translations for a given language.
	 * Defaults to the default language which must be set.
	 * @param langCode the 2-letter language code
	 * @return the language map
	 */
	public Map<String, String> readLanguage(String langCode) {
		if (StringUtils.isBlank(langCode) || !ALL_LOCALES.containsKey(langCode) ||
				langCode.equals(getDefaultLanguageCode())) {
			return getDefaultLanguage();
		} else if (LANG_CACHE.containsKey(langCode)) {
			return LANG_CACHE.get(langCode);
		}

		// load language map from file
		Map<String, String> lang = readLanguageFromFile(langCode);
		if (lang == null || lang.isEmpty()) {
			// or try to load from DB
			Sysprop s = pc.read(keyPrefix.concat(langCode));
			if (s != null && !s.getProperties().isEmpty()) {
				Map<String, Object> loaded = s.getProperties();
				lang = new TreeMap<String, String>();
				for (Map.Entry<String, Object> entry : loaded.entrySet()) {
					lang.put(entry.getKey(), String.valueOf(entry.getValue()));
				}
			}
		}
		if (lang == null || lang.isEmpty()) {
			return getDefaultLanguage();
		} else {
			LANG_CACHE.put(langCode, lang);
			return Collections.unmodifiableMap(lang);
		}
	}

	/**
	 * Persists the language map to a file. Overwrites any existing files.
	 * @param langCode the 2-letter language code
	 * @param lang the language map
	 * @param writeToDatabase true if you want the language map to be stored in the DB as well
	 */
	public void writeLanguage(String langCode, Map<String, String> lang, boolean writeToDatabase) {
		if (lang == null || lang.isEmpty() || StringUtils.isBlank(langCode) || !ALL_LOCALES.containsKey(langCode)) {
			return;
		}
		writeLanguageToFile(langCode, lang);

		if (writeToDatabase) {
			// this will overwrite a saved language map!
			Sysprop s = new Sysprop(keyPrefix.concat(langCode));
			Map<String, String> dlang = getDefaultLanguage();
			for (Map.Entry<String, String> entry : dlang.entrySet()) {
				String key = entry.getKey();
				if (lang.containsKey(key)) {
					s.addProperty(key, lang.get(key));
				} else {
					s.addProperty(key, entry.getValue());
				}
			}
			pc.create(s);
		}
	}

	/**
	 * Returns a non-null locale for a given language code.
	 * @param langCode the 2-letter language code
	 * @return a locale. default is English
	 */
	public Locale getProperLocale(String langCode) {
		String lang = StringUtils.substring(langCode, 0, 2);
		lang = (StringUtils.isBlank(lang) || !ALL_LOCALES.containsKey(lang)) ? "en" : lang.trim().toLowerCase();
		return ALL_LOCALES.get(lang);
	}

	/**
	 * Returns the default language map.
	 * @return the default language map or an empty map if the default isn't set.
	 */
	public Map<String, String> getDefaultLanguage() {
		if (!LANG_CACHE.containsKey(getDefaultLanguageCode())) {
			logger.info("Default language map not set, loading English.");
			Map<String, String> deflang = readLanguageFromFile(getDefaultLanguageCode());
			if (deflang != null && !deflang.isEmpty()) {
				LANG_CACHE.put(getDefaultLanguageCode(), deflang);
				return Collections.unmodifiableMap(deflang);
			}
		}
		return Collections.unmodifiableMap(LANG_CACHE.get(getDefaultLanguageCode()));
	}

	/**
	 * Sets the default language map. It is the basis language template which is to be translated.
	 * @param deflang the default language map
	 */
	public void setDefaultLanguage(Map<String, String> deflang) {
		if (deflang != null && !deflang.isEmpty()) {
			LANG_CACHE.put(getDefaultLanguageCode(), deflang);
		}
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
	 * @param locale a locale
	 * @param key the string key
	 * @param pager the pager object
	 * @return a list of translations
	 */
	public List<Translation> readAllTranslationsForKey(String locale, String key, Pager pager) {
		Map<String, Object> terms = new HashMap<String, Object>(2);
		terms.put("thekey", key);
		terms.put("locale", locale);
		return pc.findTerms(Utils.type(Translation.class), terms, true, pager);
	}

	/**
	 * Returns the set of all approved translations.
	 * @param langCode the 2-letter language code
	 * @return a set of keys for approved translations
	 */
	public Set<String> getApprovedTransKeys(String langCode) {
		HashSet<String> approvedTransKeys = new HashSet<String>();
		if (StringUtils.isBlank(langCode)) {
			return approvedTransKeys;
		}

		for (Map.Entry<String, String> entry : readLanguage(langCode).entrySet()) {
			if (!getDefaultLanguage().get(entry.getKey()).equals(entry.getValue())) {
				approvedTransKeys.add(entry.getKey());
			}
		}
		return approvedTransKeys;
	}

	/**
	 * Returns a map of language codes and the percentage of translated string for that language.
	 * @return a map indicating translation progress
	 */
	public Map<String, Integer> getTranslationProgressMap() {
		Sysprop progress = pc.read(progressKey);
		Map<String, Integer> progressMap = new HashMap<String, Integer>(ALL_LOCALES.size());
		boolean isMissing = progress == null;
		if (isMissing) {
			progress = new Sysprop(progressKey);
			progress.addProperty(getDefaultLanguageCode(), 100);
		}
		for (String langCode : ALL_LOCALES.keySet()) {
			Object percent = progress.getProperties().get(langCode);
			if (percent != null && percent instanceof Number) {
				progressMap.put(langCode, (Integer) percent);
			} else {
				progressMap.put(langCode, 0);
			}
		}
		if (isMissing) {
			pc.create(progress);
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
	 * @param langCode the 2-letter language code
	 * @param key the translation key
	 * @param value the translated string
	 * @return true if the operation was successful
	 */
	public boolean approveTranslation(String langCode, String key, String value) {
		if (langCode == null || key == null || value == null || getDefaultLanguageCode().equals(langCode)) {
			return false;
		}
		Sysprop s = pc.read(keyPrefix.concat(langCode));

		if (s != null && !value.equals(s.getProperty(key))) {
			s.addProperty(key, value);
			pc.update(s);
			updateTranslationProgressMap(langCode, PLUS);
			return true;
		}
		return false;
	}

	/**
	 * Disapproves a translation for a given language.
	 * @param langCode the 2-letter language code
	 * @param key the translation key
	 * @return true if the operation was successful
	 */
	public boolean disapproveTranslation(String langCode, String key) {
		if (langCode == null || key == null || getDefaultLanguageCode().equals(langCode)) {
			return false;
		}
		Sysprop s = pc.read(keyPrefix.concat(langCode));
		String defStr = getDefaultLanguage().get(key);

		if (s != null && !defStr.equals(s.getProperty(key))) {
			s.addProperty(key, defStr);
			pc.update(s);
			updateTranslationProgressMap(langCode, MINUS);
			return true;
		}
		return false;
	}

	/**
	 * Updates the progress for all languages.
	 * @param langCode the 2-letter language code
	 * @param value {@link #PLUS}, {@link #MINUS} or the total percent of completion (0-100)
	 */
	private void updateTranslationProgressMap(String langCode, int value) {
		if (getDefaultLanguageCode().equals(langCode)) {
			return;
		}

		double defsize = getDefaultLanguage().size();
		double approved = value;

		Map<String, Integer> progress = getTranslationProgressMap();
		Integer percent = progress.get(langCode);
		if (value == PLUS) {
			approved = Math.round(percent * (defsize / 100) + 1);
		} else if (value == MINUS) {
			approved = Math.round(percent * (defsize / 100) - 1);
		}

		// allow 3 identical words per language (i.e. Email, etc)
		if (approved >= defsize - 3) {
			approved = defsize;
		}

		if (((int) defsize) == 0) {
			progress.put(langCode, 0);
		} else {
			progress.put(langCode, (int) ((approved / defsize) * 100));
		}
		if (percent < 100 && !percent.equals(progress.get(langCode))) {
			Sysprop s = new Sysprop(progressKey);
			for (Map.Entry<String, Integer> entry : progress.entrySet()) {
				s.addProperty(entry.getKey(), entry.getValue());
			}
			pc.create(s);
		}
	}

	private Map<String, String> readLanguageFromFile(String langCode) {
		if (langCode != null && langCode.length() == 2) {
			Properties lang = new Properties();
			String file = "lang_" + langCode + ".properties";
			InputStream ins = null;
			try {
				ins = LanguageUtils.class.getClassLoader().getResourceAsStream(file);
				if (ins != null) {
					lang.load(ins);
					if (!lang.isEmpty()) {
						int progress = 0;
						Map<String, String> langmap = new TreeMap<String, String>();
						for (String propKey : lang.stringPropertyNames()) {
							String propVal = lang.getProperty(propKey);
							if (!langCode.equals(getDefaultLanguageCode())) {
								String defaultVal = getDefaultLanguage().get(propKey);
								if (!StringUtils.isBlank(propVal) && !StringUtils.equalsIgnoreCase(propVal, defaultVal)) {
									progress++;
								}
							}
							langmap.put(propKey, propVal);
						}
						if (langCode.equals(getDefaultLanguageCode())) {
							progress = langmap.size(); // 100%
						}
						if (progress > 0) {
							updateTranslationProgressMap(langCode, progress);
						}
						return langmap;
					}
				}
			} catch (Exception e) {
				logger.info("Could not read language file " + file + ": ", e);
			} finally {
				try {
					if (ins != null) {
						ins.close();
					}
				} catch (IOException ex) {
					logger.error(null, ex);
				}
			}
		}
		return null;
	}

	private void writeLanguageToFile(String langCode, Map<String, String> lang) {
		if (lang != null && !lang.isEmpty() && langCode != null && langCode.length() == 2) {
			FileOutputStream fos = null;
			try {
				Properties langProps = new Properties();
				langProps.putAll(lang);
				File file = new File("lang_" + langCode + ".properties");
				fos = new FileOutputStream(file);
				langProps.store(fos, langCode);

				int progress = 0;
				for (Map.Entry<String, String> entry : lang.entrySet()) {
					if (!getDefaultLanguage().get(entry.getKey()).equals(entry.getValue())) {
						progress++;
					}
				}
				if (progress > 0) {
					updateTranslationProgressMap(langCode, progress);
				}
			} catch (Exception ex) {
				logger.error("Could not write language to file: ", ex);
			} finally {
				try {
					if (fos != null) {
						fos.close();
					}
				} catch (IOException ex) {
					logger.error(null, ex);
				}
			}
		}
	}
}
