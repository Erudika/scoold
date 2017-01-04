/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages.click;

import com.erudika.para.utils.Config;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Language;
import com.erudika.scoold.pages.Base;
import com.erudika.scoold.utils.LanguageUtils;
import java.util.Locale;
import java.util.Map;
import org.apache.click.util.ErrorPage;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Error extends ErrorPage {

    public String title;
	public Map<String, String> lang;
	public Utils utils;

	public LanguageUtils langutils;

    public Error() {
		utils = Utils.getInstance();
		langutils = new LanguageUtils();
		addModel("APPNAME", Base.APPNAME);
		addModel("DESCRIPTION", Base.DESCRIPTION);
		addModel("KEYWORDS", Base.KEYWORDS);
		addModel("CDN_URL", Base.CDN_URL);
		addModel("IN_PRODUCTION", Base.IN_PRODUCTION);
    }

	public void onInit() {
		super.onInit();
		langutils.setDefaultLanguage(Language.ENGLISH);
		Locale loc = langutils.getProperLocale(getContext().getRequest().getLocale().getLanguage());
		lang = langutils.readLanguage(Config.APP_NAME_NS, loc.getLanguage());
        title = lang.get("error.title");
	}

	public String getTemplate() {
		return "click/error.htm";
	}

}
