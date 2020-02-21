/*
 * Copyright 2013-2020 Erudika. https://erudika.com
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
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
				ALL_LOCALES.putIfAbsent(locstr, Locale.forLanguageTag(locstr));
			}
		}
		ALL_LOCALES.remove("zh");
		ALL_LOCALES.putIfAbsent(Locale.SIMPLIFIED_CHINESE.toString(), Locale.SIMPLIFIED_CHINESE);
		ALL_LOCALES.putIfAbsent(Locale.TRADITIONAL_CHINESE.toString(), Locale.TRADITIONAL_CHINESE);
	}

	private static final Map<String, Map<String, String>> LANG_CACHE =
			new ConcurrentHashMap<String, Map<String, String>>(ALL_LOCALES.size());

	private static Sysprop langProgressCache = new Sysprop();

	private final String keyPrefix = "language".concat(Config.SEPARATOR);
	private final String progressKey = keyPrefix.concat("progress");
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
		if (StringUtils.isBlank(langCode) || langCode.equals(getDefaultLanguageCode())) {
			return getDefaultLanguage();
		} else if (langCode.length() > 2 && !ALL_LOCALES.containsKey(langCode)) {
			return readLanguage(langCode.substring(0, 2));
		} else if (LANG_CACHE.containsKey(langCode)) {
			return LANG_CACHE.get(langCode);
		}

		// load language map from file
		Map<String, String> lang = readLanguageFromFile(langCode);
		if (lang == null || lang.isEmpty()) {
			// or try to load from DB
			lang = new TreeMap<String, String>(getDefaultLanguage());
			Sysprop s = pc.read(keyPrefix.concat(langCode));
			if (s != null && !s.getProperties().isEmpty()) {
				Map<String, Object> loaded = s.getProperties();
				for (Map.Entry<String, String> entry : lang.entrySet()) {
					if (loaded.containsKey(entry.getKey())) {
						lang.put(entry.getKey(), String.valueOf(loaded.get(entry.getKey())));
					} else {
						lang.put(entry.getKey(), entry.getValue());
					}
				}
			}
		}
		LANG_CACHE.put(langCode, lang);
		return Collections.unmodifiableMap(lang);
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
		if (StringUtils.startsWith(langCode, "zh")) {
			if ("zh_tw".equalsIgnoreCase(langCode)) {
				return Locale.TRADITIONAL_CHINESE;
			} else {
				return Locale.SIMPLIFIED_CHINESE;
			}
		}
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
			Map<String, String> deflang = readLanguageFromFile(getDefaultLanguageCode());
			if (deflang != null && !deflang.isEmpty()) {
				LANG_CACHE.put(getDefaultLanguageCode(), deflang);
				return Collections.unmodifiableMap(deflang);
			}
		}
		return Collections.unmodifiableMap(LANG_CACHE.get(getDefaultLanguageCode()));
	}

	/**
	 * Returns the default language code.
	 * @return the 2-letter language code
	 */
	public String getDefaultLanguageCode() {
		return "en";
	}

	/**
	 * Returns a map of language codes and the percentage of translated string for that language.
	 * @return a map indicating translation progress
	 */
	public Map<String, Integer> getTranslationProgressMap() {
		Sysprop progress;
		if (langProgressCache.getProperties().isEmpty()) {
			progress = pc.read(progressKey);
			if (progress != null) {
				langProgressCache = progress;
			}
		} else {
			progress = langProgressCache;
		}
		Map<String, Integer> progressMap = new HashMap<String, Integer>(ALL_LOCALES.size());
		boolean isMissing = progress == null;
		if (isMissing) {
			progress = new Sysprop(progressKey);
			progress.addProperty(getDefaultLanguageCode(), 100);
		}
		for (String langCode : ALL_LOCALES.keySet()) {
			if (isMissing) {
				readLanguageFromFileAndUpdateProgress(langCode, progressMap);
				progress.getProperties().putAll(progressMap);
			} else {
				Object percent = progress.getProperties().get(langCode);
				if (percent != null && percent instanceof Number) {
					progressMap.put(langCode, (Integer) percent);
				} else {
					progressMap.put(langCode, 0);
				}
			}
		}
		if (isMissing) {
			langProgressCache = pc.create(progress);
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

	private int calculateProgressPercent(double approved, double defsize) {
		// allow 5 identical words per language (i.e. Email, etc)
		if (approved >= defsize - 5) {
			approved = defsize;
		}
		if (defsize == 0) {
			return 0;
		} else {
			return (int) ((approved / defsize) * 100.0);
		}
	}

	/**
	 * Updates the progress for all languages.
	 * @param langCode the 2-letter language code
	 * @param approved the total percent of completion (0-100)
	 */
	private void updateTranslationProgressMap(String langCode, int approved) {
		if (getDefaultLanguageCode().equals(langCode)) {
			return;
		}
		double defsize = getDefaultLanguage().size();
		Map<String, Integer> progress = getTranslationProgressMap();
		Integer percent = progress.get(langCode);
		progress.put(langCode, calculateProgressPercent(approved, defsize));
		Sysprop updatedProgress = new Sysprop(progressKey);
		for (Map.Entry<String, Integer> entry : progress.entrySet()) {
			updatedProgress.addProperty(entry.getKey(), entry.getValue());
		}
		langProgressCache = updatedProgress;
		if (percent < 100 && !percent.equals(progress.get(langCode))) {
			pc.create(updatedProgress);
		}
	}

	private Map<String, String> readLanguageFromFile(String langCode) {
		return readLanguageFromFileAndUpdateProgress(langCode, null);
	}

	private Map<String, String> readLanguageFromFileAndUpdateProgress(String langCode, Map<String, Integer> progressMap) {
		if (langCode != null) {
			Properties lang = new Properties();
			String file = "lang_" + langCode.toLowerCase() + ".properties";
			InputStream ins = null;
			try {
				ins = LanguageUtils.class.getClassLoader().getResourceAsStream(file);
				if (ins != null) {
					lang.load(ins);
					int progress = 0;
					Map<String, String> langmap = new TreeMap<String, String>();
					Set<String> keySet = langCode.equalsIgnoreCase(getDefaultLanguageCode()) ?
							lang.stringPropertyNames() : getDefaultLanguage().keySet();
					for (String propKey : keySet) {
						String propVal = lang.getProperty(propKey);
						if (!langCode.equalsIgnoreCase(getDefaultLanguageCode())) {
							String defaultVal = getDefaultLanguage().get(propKey);
							if (!StringUtils.isBlank(propVal) && !StringUtils.equalsIgnoreCase(propVal, defaultVal)) {
								progress++;
							} else if (StringUtils.isBlank(propVal)) {
								propVal = defaultVal;
							}
						}
						langmap.put(propKey, propVal);
					}
					if (langCode.equalsIgnoreCase(getDefaultLanguageCode())) {
						progress = langmap.size(); // 100%
					}
					if (progress > 0 && progressMap == null) {
						updateTranslationProgressMap(langCode, progress);
					} else {
						progressMap.put(langCode, calculateProgressPercent(progress, langmap.size()));
					}
					return langmap;
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
