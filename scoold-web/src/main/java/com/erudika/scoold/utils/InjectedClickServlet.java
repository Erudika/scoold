/*
 * Copyright 2013 Alex Bogdanovski <albogdano@me.com>.
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
 * You can reach the author at: https://github.com/albogdano
 */
package com.erudika.scoold.utils;

import com.erudika.para.Para;
import javax.servlet.http.HttpServletRequest;
import org.apache.click.ClickServlet;
import org.apache.click.Page;
import org.apache.click.util.ErrorPage;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class InjectedClickServlet extends ClickServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected Page newPageInstance(String path, Class<? extends Page> pageClass, HttpServletRequest request)
			throws Exception {
		Page page = super.newPageInstance(path, pageClass, request);
		Para.injectInto(page);
		return page;
	}
	
	@Override
	protected ErrorPage createErrorPage(Class<? extends Page> pageClass, Throwable exception) {
		try {
			ErrorPage errorPage = (ErrorPage) configService.getErrorPageClass().newInstance();
			Para.injectInto(errorPage);
			return errorPage;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
	}
	
}
