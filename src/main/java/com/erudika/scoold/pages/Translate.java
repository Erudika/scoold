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
package com.erudika.scoold.pages;

import com.erudika.para.core.Translation;
import com.erudika.para.utils.Config;
import com.erudika.scoold.core.ScooldUser;
import com.erudika.scoold.core.ScooldUser.Badge;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Translate extends Base{

	public String title;

	public Locale showLocale;
	public int showLocaleProgress;
	public List<Translation> translationslist;	// only the translations for given key
	public List<String> langkeys;
	public int showIndex;
	public boolean isTranslated = false;

	public Translate() {
		title = lang.get("translate.title");
		langkeys = new ArrayList<String>();

		if (param("locale")) {
			showLocale = langutils.getAllLocales().get(getParamValue("locale"));
			if (showLocale == null || showLocale.getLanguage().equals("en")) {
				setRedirect(languageslink);
				return;
			}
			title += " - " + showLocale.getDisplayName(showLocale);

			for (String key : deflang.keySet()) {
				langkeys.add(key);
			}

			if (param("index")) {
				showIndex = NumberUtils.toInt(getParamValue("index"), 1) - 1;
				if (showIndex <= 0) {
					showIndex = 0;
				} else if (showIndex >= langkeys.size()) {
					showIndex = langkeys.size() - 1;
				}
			}
		} else {
			setRedirect(languageslink);
		}
	}

	public void onGet() {
		if (isAjaxRequest()) return;
		if (param("locale")) {
			showLocaleProgress = langutils.getTranslationProgressMap(Config.APP_NAME_NS).get(showLocale.getLanguage());
			if (param("index")) {
				// this is what is currently shown for translation
				String langkey = langkeys.get(showIndex);
				translationslist = langutils.readAllTranslationsForKey(Config.APP_NAME_NS, showLocale.getLanguage(),
						langkey, itemcount);
			}
		}
	}

	public void onPost() {
		if (param("locale") && param("gettranslationhtmlcode")) {
			String value = StringUtils.trim(getParamValue("value"));
			String langkey = langkeys.get(showIndex);
			Set<String> approved = langutils.getApprovedTransKeys(Config.APP_NAME_NS, showLocale.getLanguage());
			isTranslated = approved.contains(langkey);
			if (!StringUtils.isBlank(value) && (!isTranslated || inRole("admin"))) {
				Translation trans = new Translation(showLocale.getLanguage(), langkey, value);
				trans.setCreatorid(authUser.getId());
				trans.create();
				addModel("newtranslation", trans);
			}
			if (!isAjaxRequest()) {
				setRedirect(translatelink + "/" + showLocale.getLanguage()+"/"+getNextIndex(showIndex, approved));
			}
		} else if (param("reset") && inRole("admin")) {
			String key = getParamValue("reset");
			if (lang.containsKey(key)) {
				if (param("global")) {
					// global reset: delete all approved translations for this key
//					LanguageUtils.disapproveAllForKey(key);
				} else {
					// loca reset: delete all approved translations for this key and locale
//					LanguageUtils.disapproveAllForKey(key, showLocale.getLanguage());
				}
				if (!isAjaxRequest())
					setRedirect(translatelink+"/"+showLocale.getLanguage());
			}
		} else if (param("approve") && inRole("admin")) {
			String id = getParamValue("approve");
			Translation trans = (Translation) pc.read(id);
			if (trans != null) {
				if (trans.isApproved()) {
					trans.disapprove();
				} else {
					trans.approve();
					addBadge(Badge.POLYGLOT, (ScooldUser) pc.read(trans.getCreatorid()), true);
				}
			}
			if (!isAjaxRequest())
				setRedirect(translatelink+"/"+showLocale.getLanguage()+"/"+(showIndex+1));
		} else if (param("delete")) {
			String id = getParamValue("delete");
			if (id != null) {
				Translation t = (Translation) pc.read(id);
				if (authUser.getId().equals(t.getCreatorid()) || inRole("admin")) {
					t.disapprove();
					t.delete();
				}
				if (!isAjaxRequest())
					setRedirect(translatelink+"/"+showLocale.getLanguage()+"/"+(showIndex+1));
			}
		}
	}

	private int getNextIndex(int start, Set<String> approved) {
		if (start < 0) start = 0;
		if (start >= approved.size()) start = approved.size() - 1;
		int nexti = (start + 1) >= langkeys.size() ? 0 : (start + 1);

		// if there are untranslated strings go directly there
		if (approved.size() != langkeys.size()) {
			while(approved.contains(langkeys.get(nexti))) {
				nexti = (nexti + 1) >= langkeys.size() ? 0 : (nexti + 1);
			}
		}

		return nexti;
	}

}
