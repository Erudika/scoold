/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages.click;

import com.scoold.core.Language;
import com.scoold.pages.BasePage;
import java.util.Locale;
import java.util.Map;
import org.apache.click.Page;

/**
 *
 * @author alexb
 */
public class NotFound extends Page{

	public String title;
	public Map<String, String> lang;

	public NotFound() {
		Locale loc = Language.getProperLocale(getContext().getRequest().getLocale().getLanguage());
		lang = Language.readLanguage(loc);
        title = lang.get("notfound.title");
		
		addModel("APPNAME", BasePage.APPNAME);
		addModel("DESCRIPTION", BasePage.DESCRIPTION);
		addModel("KEYWORDS", BasePage.KEYWORDS);
		addModel("CDN_URL", BasePage.CDN_URL);
		addModel("currentLocale", loc);
		addModel("IN_PRODUCTION", BasePage.IN_PRODUCTION);
	}

	public String getTemplate() {
		return "click/not-found.htm";
	}
}
