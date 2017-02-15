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
public class SettingsController {

	public String title;
	public boolean includeGMapsScripts = true;

//	public SettingsController() {
//		title = lang.get("settings.title");
//	}
//
//	public void onPost() {
//		// check if the checksum is the same as the one in the link
//		String redirectto = settingslink;
//		if (param("deleteaccount")) {
//			authUser.delete();
//			clearSession();
//			redirectto = signinlink + "?code=4&success=true";
//		} else {
//			String favtags = getParamValue("tags");
//			String latlng = getParamValue("latlng");
//			if (!StringUtils.isBlank(favtags)) {
//				Set<String> ts = new LinkedHashSet<String>();
//				for (String tag : favtags.split(",")) {
//					if (!StringUtils.isBlank(tag) && ts.size() <= AppConfig.MAX_FAV_TAGS) {
//						ts.add(tag);
//					}
//				}
//				authUser.setFavtags(new LinkedList<String>(ts));
//			}
//			if (!StringUtils.isBlank(latlng)) {
//				authUser.setLatlng(latlng);
//			}
//			authUser.update();
//		}
//
//		if (!isAjaxRequest())
//			setRedirect(redirectto);
//	}

}
