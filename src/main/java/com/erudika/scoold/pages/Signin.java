/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.para.core.User;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Utils;

public class Signin extends Base{

    public String title;

    public Signin() {
        title = lang.get("signin.title");
		includeFBscripts = true;
    }

	public void onGet() {
		if (authenticated) {
			setRedirect(HOMEPAGE);
		} else {
			if (param("access_token")) {
				User u = pc.signIn("facebook", getParamValue("access_token"), false);
				Utils.setStateParam(Config.AUTH_COOKIE, u.getPassword(), req, resp, true);
				setRedirect(HOMEPAGE);
			}
		}
	}

	public void	onPost() {
		if (authenticated) {
			if (param("signout")) {
				clearSession();
				if (authUser.isFacebookUser()) {
					setRedirect(signinlink + "?code=5&success=true&fblogout=true");
				} else {
					setRedirect(signinlink + "?code=5&success=true");
				}
			} else {
				setRedirect(HOMEPAGE);
			}
        }
	}
}
