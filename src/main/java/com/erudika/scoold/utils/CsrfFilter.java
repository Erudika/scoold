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
package com.erudika.scoold.utils;

import static com.erudika.scoold.ScooldServer.CSRF_COOKIE;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class CsrfFilter implements Filter {

	private static final Logger logger = LoggerFactory.getLogger(CsrfFilter.class);

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		logger.debug("filter init() called");
		if (filterConfig == null) {
			logger.error("unable to init filter as filter config is null");
		}
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		final HttpServletRequest request = (HttpServletRequest) req;
		final HttpServletResponse response = (HttpServletResponse) res;
		boolean isCSPReportRequest = request.getRequestURI().startsWith("/reports/cspv");

		if ("POST".equals(request.getMethod()) && !isCSPReportRequest) {
			String csrfToken = request.getParameter("_csrf");
			String csrfInCookie = HttpUtils.getStateParam(CSRF_COOKIE, request);

			Long time = NumberUtils.toLong(request.getParameter("_time"), 0);
			String timekey = request.getParameter("_timekey");

			if (timekey != null) {
				Long timeInSession = (Long) request.getSession().getAttribute(timekey);
				request.getSession().setAttribute(timekey, System.currentTimeMillis());
				if (!time.equals(timeInSession)) {
					logger.warn("Time token mismatch. {}, {}", request.getRemoteAddr(), request.getRequestURL());
					// response.sendError(403, "Time token mismatch.");
					response.sendRedirect(request.getRequestURI());
					return;
				}
			}

			if (csrfToken == null) {
				csrfToken = request.getHeader("X-CSRF-TOKEN");
				if (csrfToken == null) {
					csrfToken = request.getHeader("X-XSRF-TOKEN");
				}
			}

			if (csrfToken == null || StringUtils.isBlank(csrfInCookie) || !csrfToken.equals(csrfInCookie)) {
				logger.warn("CSRF token mismatch. {}, {}", request.getRemoteAddr(), request.getRequestURL());
				response.sendError(403, "CSRF token mismatch.");
				return;
			}
		}
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		logger.debug("filter destroy() called");
	}

}
