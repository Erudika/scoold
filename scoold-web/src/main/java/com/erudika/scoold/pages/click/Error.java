/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages.click;

import com.erudika.para.i18n.LanguageUtils;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.pages.BasePage;
import java.util.Locale;
import java.util.Map;
import org.apache.click.util.ErrorPage;

/**
 *
 * @author alexb
 */
public class Error extends ErrorPage {

    public String title;
	public Map<String, String> lang;
	public Utils utils;

    public Error() {
		utils = Utils.getInstance();
		Locale loc = LanguageUtils.getProperLocale(getContext().getRequest().getLocale().getLanguage());
		lang = LanguageUtils.readLanguage(loc.getLanguage());
        title = lang.get("error.title");
		
		addModel("APPNAME", BasePage.APPNAME);
		addModel("DESCRIPTION", BasePage.DESCRIPTION);
		addModel("KEYWORDS", BasePage.KEYWORDS);
		addModel("CDN_URL", BasePage.CDN_URL);
		addModel("IN_PRODUCTION", BasePage.IN_PRODUCTION);
    }

	public String getTemplate() {
		return "click/error.htm";
	}


} 
