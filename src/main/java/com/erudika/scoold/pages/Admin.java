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

import com.erudika.para.core.ParaObject;
import com.erudika.para.utils.Config;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Admin extends Base {

	public String title;

	public Admin() {
		title = lang.get("admin.title");
		addModel("configMap", Config.getConfig());
		addModel("version", pc.getServerVersion());
	}

	public void onGet() {
		if (!authenticated || !isAdmin) {
			setRedirect(HOMEPAGE);
		}
	}

	public void onPost() {
		if (param("confirmdelete")) {
			String id = getParamValue("id");
			ParaObject sobject = pc.read(id);
			if (sobject != null) {
				sobject.delete();
				logger.info("{} #{} deleted {} #{}", authUser.getName(), authUser.getId(),
						sobject.getClass().getName(), sobject.getId());
			}
		}
		setRedirect(adminlink);
	}
}
