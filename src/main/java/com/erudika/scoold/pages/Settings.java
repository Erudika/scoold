/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.pages;

import com.erudika.para.utils.Utils;
import com.erudika.scoold.utils.AppConfig;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Settings extends Base {

	public String title;

	public Settings() {
		title = lang.get("settings.title");
	}

	public void onPost() {
		// check if the checksum is the same as the one in the link
		String redirectto = settingslink;
		if (param("deleteaccount") && StringUtils.equals(Utils.md5(authUser.getId()), getParamValue("key"))) {
			authUser.delete();
			clearSession();
			redirectto = signinlink + "?code=4&success=true";
		} else if (param("favtags")) {
			String cleanTags = getParamValue("favtags");
			Set<String> ts = new LinkedHashSet<String>();
			for (String tag : cleanTags.split(",")) {
				if (!StringUtils.isBlank(tag) && ts.size() <= AppConfig.MAX_FAV_TAGS) {
					ts.add(tag);
				}
			}
			authUser.setFavtags(new LinkedList<String>(ts));
			authUser.update();
		} else if (param("detachid")) {
			String ident = getParamValue("detachid");
			if (!StringUtils.isBlank(ident) && !ident.equals(authUser.getIdentifier())) {
				authUser.detachIdentifier(ident);
			}
		} else if (param("email")) {
			String newEmail = getParamValue("email");
			authUser.setEmail(newEmail);
			authUser.update();
		}

		if (!isAjaxRequest())
			setRedirect(redirectto);
	}

}
