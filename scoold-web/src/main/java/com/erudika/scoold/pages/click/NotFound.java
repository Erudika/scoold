/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages.click;

import com.erudika.para.i18n.LanguageUtils;
import com.erudika.scoold.pages.BasePage;
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
		Locale loc = LanguageUtils.getProperLocale(getContext().getRequest().getLocale().getLanguage());
		lang = LanguageUtils.readLanguage(loc.getLanguage());
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
