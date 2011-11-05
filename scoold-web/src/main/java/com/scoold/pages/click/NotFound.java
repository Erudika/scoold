/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages.click;

import com.scoold.core.Language;
import com.scoold.pages.BasePage;
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
		lang = Language.readLanguage(getContext().getLocale());
        title = lang.get("notfound.title");
		
		addModel("APPNAME", BasePage.APPNAME);
		addModel("DESCRIPTION", BasePage.DESCRIPTION);
		addModel("KEYWORDS", BasePage.KEYWORDS);
		addModel("CDN_URL", BasePage.CDN_URL);
		addModel("currentLocale", getContext().getLocale());
		addModel("styleslink", BasePage.CDN_URL);
		addModel("minsuffix", "-min");
	}

	public String getTemplate() {
		return "click/not-found.htm";
	}
}
