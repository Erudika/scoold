/*
 * Copyright 2013-2021 Erudika. https://erudika.com
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
import com.erudika.para.core.utils.Config;
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

	private static final Map<String, Integer> LANG_PROGRESS_CACHE = new HashMap<String, Integer>(ALL_LOCALES.size());

	private final String keyPrefix = "language".concat(Config.SEPARATOR);
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
		Map<String, String> lang = readLanguageFromFileAndUpdateProgress(langCode);
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
			// initialize the language cache maps
			LANG_CACHE.put(getDefaultLanguageCode(), readLanguageFromFileAndUpdateProgress(getDefaultLanguageCode()));
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
		if (!LANG_PROGRESS_CACHE.isEmpty() && LANG_PROGRESS_CACHE.size() > 2) { // en + default user lang
			return Collections.unmodifiableMap(LANG_PROGRESS_CACHE);
		}
		for (String langCode : ALL_LOCALES.keySet()) {
			if (!langCode.equals(getDefaultLanguageCode())) {
				LANG_CACHE.put(langCode, readLanguageFromFileAndUpdateProgress(langCode));
			}
		}
		return Collections.unmodifiableMap(LANG_PROGRESS_CACHE);
	}

	/**
	 * Returns a map of all language codes and their locales.
	 * @return a map of language codes to locales
	 */
	public Map<String, Locale> getAllLocales() {
		return Collections.unmodifiableMap(ALL_LOCALES);
	}

	private int calculateProgressPercent(double approved, double defsize) {
		// allow 5 identical words per language (i.e. Email, etc)
		if (approved >= defsize - 10) {
			approved = defsize;
		}
		if (defsize == 0) {
			return 0;
		} else {
			return (int) ((approved / defsize) * 100.0);
		}
	}

	private Map<String, String> readLanguageFromFileAndUpdateProgress(String langCode) {
		if (langCode != null) {
			Properties lang = new Properties();
			String file = "lang_" + langCode.toLowerCase() + ".properties";
			try (InputStream ins = LanguageUtils.class.getClassLoader().getResourceAsStream(file)) {
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
					LANG_PROGRESS_CACHE.put(langCode, calculateProgressPercent(progress, langmap.size()));
					return langmap;
				}
			} catch (Exception e) {
				logger.info("Could not read language file " + file + ": ", e);
			}
		}
		return Collections.emptyMap();
	}
}
