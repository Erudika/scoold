/*
 * Copyright 2013-2021 Erudika. http://erudika.com
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
import com.erudika.scoold.ScooldServer;
import static com.erudika.scoold.ScooldServer.AUTH_COOKIE;
import static com.erudika.scoold.ScooldServer.CONTEXT_PATH;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

/**
 * Various utilities for HTTP stuff - cookies, AJAX, etc.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public final class HttpUtils {

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
							setConnectTimeout(timeout, TimeUnit.SECONDS).
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
		cookie.setPath(CONTEXT_PATH.isEmpty() ? "/" : CONTEXT_PATH);
		cookie.setSecure(StringUtils.startsWithIgnoreCase(ScooldServer.getServerURL(), "https://") || req.isSecure());
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
	 *
	 * /////////////////////////////////////
	 * THIS CODE IS CAUSING MORE PROBLEMS
	 * THAN IT SOLVES! CONSIDER DELETING!!!
	 * ////////////////////////////////////
	 *
	 * @param url image URL
	 * @param req request
	 * @param res response
	 * @return the content of the image or null
	 */
	public static void getAvatar(String url, HttpServletRequest req, HttpServletResponse res) {
		if (isLocalOrInsecureHost(url)) {
			getDefaultAvatarImage(res);
			return;
		}
		if (!ScooldUtils.getInstance().isAvatarValidationEnabled()) {
			return;
		}
		HttpGet get = new HttpGet(url);
		// attach auth cookie to requests for locally uploaded avatars - without this custom avatars will not be loaded!
		if (StringUtils.startsWithIgnoreCase(url, ScooldServer.getServerURL())) {
			get.setHeader("Cookie", AUTH_COOKIE + "=" + HttpUtils.getStateParam(AUTH_COOKIE, req));
		}
		get.setHeader(HttpHeaders.USER_AGENT, "Scoold Image Validator, https://scoold.com");
		try (CloseableHttpResponse img = HttpUtils.getHttpClient().execute(get)) {
			if (img.getCode() == HttpStatus.SC_OK && img.getEntity() != null) {
				if (isImage(img, url)) {
					for (Header header : img.getHeaders()) {
						res.setHeader(header.getName(), header.getValue());
					}
					if (!res.containsHeader(HttpHeaders.CACHE_CONTROL)) {
						res.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=" + TimeUnit.HOURS.toSeconds(24));
					}
					IOUtils.copy(img.getEntity().getContent(), res.getOutputStream());
				}
			} else {
				LoggerFactory.getLogger(HttpUtils.class).debug("Failed to get user avatar from {}, status: {} {}", url,
						img.getCode(), img.getReasonPhrase());
				getDefaultAvatarImage(res);
			}
		} catch (Exception ex) {
			getDefaultAvatarImage(res);
			LoggerFactory.getLogger(HttpUtils.class).debug("Failed to get user avatar from {}: {}", url, ex.getMessage());
		}
	}

	private static boolean isImage(CloseableHttpResponse img, String url) throws MalformedURLException {
		return img.getCode() == HttpStatus.SC_OK && img.getEntity() != null &&
				(StringUtils.equalsAnyIgnoreCase(img.getEntity().getContentType(),
						"image/gif", "image/jpeg", "image/jpg", "image/png", "image/webp", "image/bmp", "image/svg+xml") ||
				StringUtils.endsWithAny(new URL(url).getPath(), ".gif", ".jpeg", ".jpg", ".png", ".webp", ".svg", ".bmp"));
	}

	private static boolean isLocalOrInsecureHost(String url) {
		if (StringUtils.isBlank(url)) {
			return true;
		}
		if (Config.IN_DEVELOPMENT) {
			return false;
		}
		if (!StringUtils.startsWithIgnoreCase(url, "https://")) {
			return true;
		}
		try {
			InetAddress addr = InetAddress.getByName(StringUtils.substringBefore(StringUtils.substringAfter(url, "//"), "/"));
			return StringUtils.containsAnyIgnoreCase(addr.getHostAddress(),
					"localhost", "0177.0.0.1", "177.0.0.1", "0x7f.0.0.1", "0x7f000001", "2130706433", "017700000001") ||
					StringUtils.startsWithAny(addr.getHostAddress(), "127.", "177.");
		} catch (Exception e) {
			return true;
		}
	}

	private static void getDefaultAvatarImage(HttpServletResponse res) {
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
		int maxAge = Config.SESSION_TIMEOUT_SEC;
		String expires = DateFormatUtils.format(System.currentTimeMillis() + (maxAge * 1000),
				"EEE, dd-MMM-yyyy HH:mm:ss z", TimeZone.getTimeZone("GMT"));
		String path = CONTEXT_PATH.isEmpty() ? "/" : CONTEXT_PATH;
		StringBuilder sb = new StringBuilder();
		sb.append(AUTH_COOKIE).append("=").append(jwt).append(";");
		sb.append("Path=").append(path).append(";");
		sb.append("Expires=").append(expires).append(";");
		sb.append("Max-Age=").append(maxAge).append(";");
		sb.append("HttpOnly;");
		if (StringUtils.startsWithIgnoreCase(ScooldServer.getServerURL(), "https://") || req.isSecure()) {
			sb.append("Secure;");
		}
		sb.append("SameSite=Lax");
		res.addHeader(javax.ws.rs.core.HttpHeaders.SET_COOKIE, sb.toString());
	}

	/**
	 * @param req req
	 * @return the original protected URL visited before authentication
	 */
	public static String getBackToUrl(HttpServletRequest req) {
		String backtoFromCookie = Utils.urlDecode(HttpUtils.getStateParam("returnto", req));
		if (StringUtils.isBlank(backtoFromCookie)) {
			backtoFromCookie = req.getParameter("returnto");
		}
		String serverUrl = ScooldServer.getServerURL() + "/";
		String resolved = URI.create(serverUrl).resolve(Optional.ofNullable(backtoFromCookie).orElse("")).toString();
		if (!StringUtils.startsWithIgnoreCase(resolved, serverUrl)) {
			backtoFromCookie = "";
		} else {
			backtoFromCookie = resolved;
		}
		return (StringUtils.isBlank(backtoFromCookie) ? HOMEPAGE : backtoFromCookie);
	}
}
