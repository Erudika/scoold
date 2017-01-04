/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages.click;

import com.erudika.scoold.pages.Base;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class NotFound extends Base {

	public String title;

	public NotFound() {
        title = lang.get("notfound.title");
		
		addModel("APPNAME", Base.APPNAME);
		addModel("DESCRIPTION", Base.DESCRIPTION);
		addModel("KEYWORDS", Base.KEYWORDS);
		addModel("CDN_URL", Base.CDN_URL);
		addModel("IN_PRODUCTION", Base.IN_PRODUCTION);
	}

	public String getTemplate() {
		return "click/not-found.htm";
	}
}
