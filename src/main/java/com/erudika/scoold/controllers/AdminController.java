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
public class AdminController {

	public String title;

//	public AdminController() {
//		title = lang.get("admin.title");
//		Map<String, Object> configMap = new HashMap<String, Object>();
//		for (Map.Entry<String, ConfigValue> entry : Config.getConfig().entrySet()) {
//			ConfigValue value = entry.getValue();
//			configMap.put(Config.PARA + "_" + entry.getKey(), value != null ? value.unwrapped() : "-");
//		}
//		configMap.putAll(System.getenv());
//		addModel("configMap", configMap);
//		addModel("version", pc.getServerVersion());
//	}
//
//	public void onGet() {
//		if (!authenticated || !isAdmin) {
//			setRedirect(HOMEPAGE);
//		}
//	}
//
//	public void onPost() {
//		if (param("confirmdelete")) {
//			String id = getParamValue("id");
//			ParaObject sobject = pc.read(id);
//			if (sobject != null) {
//				sobject.delete();
//				logger.info("{} #{} deleted {} #{}", authUser.getName(), authUser.getId(),
//						sobject.getClass().getName(), sobject.getId());
//			}
//		}
//		setRedirect(adminlink);
//	}
}
