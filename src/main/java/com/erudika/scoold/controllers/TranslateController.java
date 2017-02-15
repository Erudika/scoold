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

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class TranslateController {

	public String title;

//	public Locale showLocale;
//	public int showLocaleProgress;
//	public List<Translation> translationslist;	// only the translations for given key
//	public List<String> langkeys;
//	public int showIndex;
//	public boolean isTranslated = false;
//	public Map<String, String> deflang;
//
//	public TranslateController() {
//		title = lang.get("translate.title");
//		langkeys = new ArrayList<String>();
//		deflang = langutils.getDefaultLanguage();
//		if (param("locale")) {
//			showLocale = langutils.getProperLocale(getParamValue("locale"));
//			if (showLocale == null || showLocale.getLanguage().equals("en")) {
//				setRedirect(languageslink);
//				return;
//			}
//			title += " - " + showLocale.getDisplayName(showLocale);
//
//			for (String key : deflang.keySet()) {
//				langkeys.add(key);
//			}
//
//			if (param("index")) {
//				showIndex = NumberUtils.toInt(getParamValue("index"), 1) - 1;
//				if (showIndex <= 0) {
//					showIndex = 0;
//				} else if (showIndex >= langkeys.size()) {
//					showIndex = langkeys.size() - 1;
//				}
//			}
//		} else {
//			setRedirect(languageslink);
//		}
//	}
//
//	public void onGet() {
//		if (isAjaxRequest()) return;
//		if (param("locale")) {
//			showLocaleProgress = langutils.getTranslationProgressMap().get(showLocale.getLanguage());
//			if (param("index")) {
//				// this is what is currently shown for translation
//				String langkey = langkeys.get(showIndex);
//				translationslist = langutils.readAllTranslationsForKey(showLocale.getLanguage(),
//						langkey, itemcount);
//			}
//		}
//	}
//
//	public void onPost() {
//		if (param("locale") && param("gettranslationhtmlcode")) {
//			String value = StringUtils.trim(getParamValue("value"));
//			String langkey = langkeys.get(showIndex);
//			Set<String> approved = langutils.getApprovedTransKeys(showLocale.getLanguage());
//			isTranslated = approved.contains(langkey);
//			if (!StringUtils.isBlank(value) && (!isTranslated || isAdmin)) {
//				Translation trans = new Translation(showLocale.getLanguage(), langkey, value);
//				trans.setCreatorid(authUser.getId());
//				trans.setAuthorName(authUser.getName());
//				trans.setTimestamp(System.currentTimeMillis());
//				pc.create(trans);
//				addModel("newtranslation", trans);
//			}
//			if (!isAjaxRequest()) {
//				setRedirect(translatelink + "/" + showLocale.getLanguage()+"/"+getNextIndex(showIndex, approved));
//			}
//		} else if (param("approve") && isAdmin) {
//			String id = getParamValue("approve");
//			Translation trans = (Translation) pc.read(id);
//			if (trans != null) {
//				if (trans.getApproved()) {
//					trans.setApproved(false);
//					langutils.disapproveTranslation(trans.getLocale(), trans.getId());
//				} else {
//					trans.setApproved(true);
//					langutils.approveTranslation(trans.getLocale(), trans.getThekey(), trans.getValue());
//					addBadge(Badge.POLYGLOT, (Profile) pc.read(trans.getCreatorid()), true, true);
//				}
//				pc.update(trans);
//			}
//			if (!isAjaxRequest())
//				setRedirect(translatelink+"/"+showLocale.getLanguage()+"/"+(showIndex+1));
//		} else if (param("delete")) {
//			String id = getParamValue("delete");
//			if (id != null) {
//				Translation trans = (Translation) pc.read(id);
//				if (authUser.getId().equals(trans.getCreatorid()) || isAdmin) {
//					langutils.disapproveTranslation(trans.getLocale(), trans.getId());
//					pc.delete(trans);
//				}
//				if (!isAjaxRequest())
//					setRedirect(translatelink+"/"+showLocale.getLanguage()+"/"+(showIndex+1));
//			}
//		}
//	}
//
//	private int getNextIndex(int start, Set<String> approved) {
//		if (start < 0) start = 0;
//		if (start >= approved.size()) start = approved.size() - 1;
//		int nexti = (start + 1) >= langkeys.size() ? 0 : (start + 1);
//
//		// if there are untranslated strings go directly there
//		if (approved.size() != langkeys.size()) {
//			while(approved.contains(langkeys.get(nexti))) {
//				nexti = (nexti + 1) >= langkeys.size() ? 0 : (nexti + 1);
//			}
//		}
//
//		return nexti;
//	}

}
