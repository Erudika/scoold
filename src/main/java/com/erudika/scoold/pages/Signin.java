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

import com.erudika.para.core.User;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Utils;

public class Signin extends Base{

    public String title;

    public Signin() {
        title = lang.get("signin.title");
    }

	public void onGet() {
		if (authenticated) {
			setRedirect(HOMEPAGE);
		} else {
			if (param("access_token")) {
				User u = pc.signIn(getParamValue("provider"), getParamValue("access_token"), false);
				if (u != null) {
					Utils.setStateParam(Config.AUTH_COOKIE, u.getPassword(), req, resp, true);
					Utils.setStateParam(csrfCookieName, Utils.generateSecurityToken(), req, resp);
					setRedirect(HOMEPAGE);
				} else {
					setRedirect(signinlink + "?code=3&error=true");
				}
			}
		}
	}

	public void	onPost() {
		if (authenticated) {
			if (param("signout")) {
				clearSession();
				setRedirect(signinlink + "?code=5&success=true");
			} else {
				setRedirect(HOMEPAGE);
			}
        }
	}
}
