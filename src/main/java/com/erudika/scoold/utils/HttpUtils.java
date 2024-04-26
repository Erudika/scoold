/*
 * Copyright 2013-2022 Erudika. http://erudika.com
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

import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Various utilities for HTTP stuff - cookies, AJAX, etc.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public final class HttpUtils {

	private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);
	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private static CloseableHttpClient httpclient;
	private static final String DEFAULT_AVATAR = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
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

	/**
	 * Default private constructor.
	 */
	private HttpUtils() { }

	static CloseableHttpClient getHttpClient() {
		if (httpclient == null) {
			int timeout = 5;
			httpclient = HttpClientBuilder.create().
//					setConnectionReuseStrategy(new NoConnectionReuseStrategy()).
//					setRedirectStrategy(new LaxRedirectStrategy()).
					setDefaultRequestConfig(RequestConfig.custom().
//							setConnectTimeout(timeout, TimeUnit.SECONDS).
							setConnectionRequestTimeout(timeout, TimeUnit.SECONDS).
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
		setRawCookie(name, value, req, res, null, -1);
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
		setRawCookie(name, "", req, res, null, 0);
	}

	/**
	 * Sets a cookie.
	 * @param name the name
	 * @param value the value
	 * @param req HTTP request
	 * @param res HTTP response
	 * @param sameSite SameSite flag
	 * @param maxAge max age
	 */
	public static void setRawCookie(String name, String value, HttpServletRequest req,
			HttpServletResponse res, String sameSite, int maxAge) {
		if (StringUtils.isBlank(name) || value == null || req == null || res == null) {
			return;
		}
		String expires = DateFormatUtils.format(System.currentTimeMillis() + (maxAge * 1000),
				"EEE, dd-MMM-yyyy HH:mm:ss z", TimeZone.getTimeZone("GMT"));
		String path = CONF.serverContextPath().isEmpty() ? "/" : CONF.serverContextPath();
		StringBuilder sb = new StringBuilder();
		sb.append(name).append("=").append(value).append(";");
		sb.append("Path=").append(path).append(";");
		sb.append("Expires=").append(expires).append(";");
		sb.append("Max-Age=").append(maxAge < 0 ? CONF.sessionTimeoutSec() : maxAge).append(";");
		sb.append("HttpOnly;"); // all cookies should be HttpOnly, JS does not need to read cookie values
		if (StringUtils.startsWithIgnoreCase(CONF.serverUrl(), "https://") || req.isSecure()) {
			sb.append("Secure;");
		}
		if (!StringUtils.isBlank(sameSite)) {
			sb.append("SameSite=").append(sameSite);
		}
		res.addHeader(jakarta.ws.rs.core.HttpHeaders.SET_COOKIE, sb.toString());
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

	public static String getFullUrl(HttpServletRequest req) {
		return getFullUrl(req, false);
	}

	public static String getFullUrl(HttpServletRequest req, boolean relative) {
		String queryString = req.getQueryString();
		String url = req.getRequestURL().toString();
		if (queryString != null) {
			url = req.getRequestURL().append('?').append(queryString).toString();
		}
		if (relative) {
			url = "/" + URI.create(CONF.serverUrl()).relativize(URI.create(url)).toString();
		}
		return url;
	}

	/**
	 * @param token CAPTCHA
	 * @return boolean
	 */
	public static boolean isValidCaptcha(String token) {
		if (StringUtils.isBlank(CONF.captchaSecretKey())) {
			return true;
		}
		if (StringUtils.isBlank(token)) {
			return false;
		}
		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("secret", CONF.captchaSecretKey()));
		params.add(new BasicNameValuePair("response", token));
		HttpPost post = new HttpPost("https://www.google.com/recaptcha/api/siteverify");
		post.setEntity(new UrlEncodedFormEntity(params));
		try {
			return HttpUtils.getHttpClient().execute(post, (resp) -> {
				if (resp.getCode() == HttpStatus.SC_OK && resp.getEntity() != null) {
					Map<String, Object> data = ParaObjectUtils.getJsonReader(Map.class).readValue(resp.getEntity().getContent());
					if (data != null && data.containsKey("success")) {
						return (boolean) data.getOrDefault("success", false);
					}
				}
				return false;
			});
		} catch (Exception ex) {
			LoggerFactory.getLogger(HttpUtils.class).debug("Failed to verify CAPTCHA: {}", ex.getMessage());
		}
		return false;
	}

	public static void getDefaultAvatarImage(HttpServletResponse res) {
		try {
			res.setContentType("image/svg+xml");
			res.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=" + TimeUnit.HOURS.toSeconds(24));
			res.setHeader(HttpHeaders.ETAG, Utils.md5(DEFAULT_AVATAR));
			IOUtils.copy(new ByteArrayInputStream(DEFAULT_AVATAR.getBytes()), res.getOutputStream());
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
		setRawCookie(CONF.authCookie(), jwt, req, res, "Lax", CONF.sessionTimeoutSec());
	}

	/**
	 * @param req req
	 * @return the original protected URL visited before authentication
	 */
	public static String getBackToUrl(HttpServletRequest req) {
		return getBackToUrl(req, false);
	}

	/**
	 * @param req req
	 * @param relative relative
	 * @return the original protected URL visited before authentication
	 */
	public static String getBackToUrl(HttpServletRequest req, boolean relative) {
		String backto = Optional.ofNullable(StringUtils.stripToNull(req.getParameter("returnto"))).
				orElse(Utils.urlDecode(HttpUtils.getStateParam("returnto", req)));
		String serverUrl = CONF.serverUrl() + CONF.serverContextPath();
		String resolved = "";
		try {
			resolved = URI.create(serverUrl).resolve(Optional.ofNullable(backto).orElse("")).toString();
		} catch (Exception e) {
			logger.warn("Invalid return-to URI: {}", e.getMessage());
		}
		if (!StringUtils.startsWithIgnoreCase(resolved, serverUrl)) {
			backto = "";
		} else {
			backto = resolved;
		}
		if (relative) {
			backto = "/" + URI.create(CONF.serverUrl()).relativize(URI.create(backto)).toString();
		}
		return (StringUtils.isBlank(backto) ? HOMEPAGE : backto);
	}
}
