/*
 * Copyright 2013-2019 Erudika. http://erudika.com
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

import com.erudika.para.utils.Config;
import com.erudika.para.utils.Utils;
import static com.erudika.scoold.ScooldServer.AUTH_COOKIE;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

/**
 * Various utilities for HTTP stuff - cookies, AJAX, etc.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public final class HttpUtils {

	private static CloseableHttpClient httpclient;

	/**
	 * Default private constructor.
	 */
	private HttpUtils() { }

	static CloseableHttpClient getHttpClient() {
		if (httpclient == null) {
			int timeout = 5 * 1000;
			httpclient = HttpClientBuilder.create().
					setConnectionReuseStrategy(new NoConnectionReuseStrategy()).
					setDefaultRequestConfig(RequestConfig.custom().
							setConnectTimeout(timeout).
							setConnectionRequestTimeout(timeout).
							setCookieSpec(CookieSpecs.STANDARD).
							setSocketTimeout(timeout).
							build()).
					build();
		}
		return httpclient;
	}

	/**
	 * Checks if a request comes from JavaScript.
	 * @param request HTTP request
	 * @return true if AJAX
	 */
	public static boolean isAjaxRequest(HttpServletRequest request) {
		return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With")) ||
				"XMLHttpRequest".equalsIgnoreCase(request.getParameter("X-Requested-With"));
	}

	/////////////////////////////////////////////
	//    	   COOKIE & STATE UTILS
	/////////////////////////////////////////////

	/**
	 * Sets a cookie.
	 * @param name the name
	 * @param value the value
	 * @param req HTTP request
	 * @param res HTTP response
	 */
	public static void setStateParam(String name, String value, HttpServletRequest req, HttpServletResponse res) {
		setStateParam(name, value, req, res, false);
	}

	/**
	 * Sets a cookie.
	 * @param name the name
	 * @param value the value
	 * @param req HTTP request
	 * @param res HTTP response
	 * @param httpOnly HTTP only flag
	 */
	public static void setStateParam(String name, String value, HttpServletRequest req,
			HttpServletResponse res, boolean httpOnly) {
		setRawCookie(name, value, req, res, httpOnly, -1);
	}

	/**
	 * Reads a cookie.
	 * @param name the name
	 * @param req HTTP request
	 * @return the cookie value
	 */
	public static String getStateParam(String name, HttpServletRequest req) {
		return getCookieValue(req, name);
	}

	/**
	 * Deletes a cookie.
	 * @param name the name
	 * @param req HTTP request
	 * @param res HTTP response
	 */
	public static void removeStateParam(String name, HttpServletRequest req,
			HttpServletResponse res) {
		setRawCookie(name, "", req, res, false, 0);
	}

	/**
	 * Sets a cookie.
	 * @param name the name
	 * @param value the value
	 * @param req HTTP request
	 * @param res HTTP response
	 * @param httpOnly HTTP only flag
	 * @param maxAge max age
	 */
	public static void setRawCookie(String name, String value, HttpServletRequest req,
			HttpServletResponse res, boolean httpOnly, int maxAge) {
		if (StringUtils.isBlank(name) || value == null || req == null || res == null) {
			return;
		}
		Cookie cookie = new Cookie(name, value);
		cookie.setHttpOnly(httpOnly);
		cookie.setMaxAge(maxAge < 0 ? Config.SESSION_TIMEOUT_SEC : maxAge);
		cookie.setPath("/");
		cookie.setSecure(req.isSecure());
		res.addCookie(cookie);
	}

	/**
	 * Reads a cookie.
	 * @param name the name
	 * @param req HTTP request
	 * @return the cookie value
	 */
	public static String getCookieValue(HttpServletRequest req, String name) {
		if (StringUtils.isBlank(name) || req == null) {
			return null;
		}
		Cookie[] cookies = req.getCookies();
		if (cookies == null) {
			return null;
		}
		//Otherwise, we have to do a linear scan for the cookie.
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(name)) {
				return cookie.getValue();
			}
		}
		return null;
	}

	/**
	 * Fetches an avatar at a given URL.
	 * @param url image URL
	 * @param res response
	 * @return the content of the image or null
	 */
	public static void getAvatar(String url, HttpServletResponse res) {
		if (StringUtils.isBlank(url)) {
			getDefaultAvatarImage(res);
			return;
		}
		HttpGet get = new HttpGet(url);
		get.setHeader(HttpHeaders.USER_AGENT, "Scoold Image Validator, https://scoold.com");
		try (CloseableHttpResponse img = HttpUtils.getHttpClient().execute(get)) {
			if (img.getStatusLine().getStatusCode() == HttpStatus.SC_OK && img.getEntity() != null) {
				String contentType = img.getEntity().getContentType().getValue();
				if (StringUtils.equalsAnyIgnoreCase(contentType, "image/gif", "image/jpeg", "image/jpg", "image/png",
						"image/webp", "image/bmp", "image/svg+xml")) {
					for (Header header : img.getAllHeaders()) {
						res.setHeader(header.getName(), header.getValue());
					}
					if (!res.containsHeader(org.apache.http.HttpHeaders.CACHE_CONTROL)) {
						res.setHeader(org.apache.http.HttpHeaders.CACHE_CONTROL, "max-age=" + TimeUnit.HOURS.toSeconds(24));
					}
					IOUtils.copy(img.getEntity().getContent(), res.getOutputStream());
				}
			} else {
				getDefaultAvatarImage(res);
			}
		} catch (IOException ex) {
			getDefaultAvatarImage(res);
			LoggerFactory.getLogger(HttpUtils.class).debug("Failed to get user avatar from {}: {}", url, ex.getMessage());
		}
	}

	private static void getDefaultAvatarImage(HttpServletResponse res) {
		try {
			String anon = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
					+ "<svg xmlns=\"http://www.w3.org/2000/svg\" id=\"svg8\" width=\"756\" height=\"756\" "
					+ "version=\"1\" viewBox=\"0 0 200 200\">\n"
					+ "  <g id=\"layer1\" transform=\"translate(0 -97)\">\n"
					+ "    <rect id=\"rect1433\" width=\"282\" height=\"245\" x=\"-34\" y=\"79\" fill=\"#ececec\" rx=\"2\"/>\n"
					+ "  </g>\n"
					+ "  <g id=\"layer2\" fill=\"gray\">\n"
					+ "    <circle id=\"path1421\" cx=\"102\" cy=\"-70\" r=\"42\" transform=\"scale(1 -1)\"/>\n"
					+ "    <ellipse id=\"path1423\" cx=\"101\" cy=\"201\" rx=\"71\" ry=\"95\"/>\n"
					+ "  </g>\n"
					+ "</svg>";
			res.setContentType("image/svg+xml");
			res.setHeader(org.apache.http.HttpHeaders.CACHE_CONTROL, "max-age=" + TimeUnit.HOURS.toSeconds(24));
			res.setHeader(org.apache.http.HttpHeaders.ETAG, Utils.md5(anon));
			IOUtils.copy(new ByteArrayInputStream(anon.getBytes()), res.getOutputStream());
		} catch (IOException e) {
			LoggerFactory.getLogger(HttpUtils.class).
					debug("Failed to set default user avatar. {}", e.getMessage());
		}
	}

	/**
	 * Sets the session cookie.
	 * @param jwt a JWT from Para
	 * @param req req
	 * @param res res
	 */
	public static void setAuthCookie(String jwt, HttpServletRequest req, HttpServletResponse res) {
		if (StringUtils.isBlank(jwt)) {
			return;
		}
		int maxAge = Config.SESSION_TIMEOUT_SEC;
		String expires = DateFormatUtils.format(System.currentTimeMillis() + (maxAge * 1000),
				"EEE, dd-MMM-yyyy HH:mm:ss z", TimeZone.getTimeZone("GMT"));

		StringBuilder sb = new StringBuilder();
		sb.append(AUTH_COOKIE).append("=").append(jwt).append(";");
		sb.append("Path=/;");
		sb.append("Expires=").append(expires).append(";");
		sb.append("Max-Age=").append(maxAge).append(";");
		sb.append("HttpOnly;");
		sb.append("SameSite=Lax");
		res.addHeader(javax.ws.rs.core.HttpHeaders.SET_COOKIE, sb.toString());
	}

	/**
	 * @param req req
	 * @return the original protected URL visited before authentication
	 */
	public static String getBackToUrl(HttpServletRequest req) {
		String backtoFromCookie = Utils.urlDecode(HttpUtils.getStateParam("returnto", req));
		return (StringUtils.isBlank(backtoFromCookie) ? HOMEPAGE : backtoFromCookie);
	}
}
