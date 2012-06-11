/*
 * Copyright 2008-2012 Marc Wick, geonames.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.scoold.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * provides static methods to access the <a
 * href="http://www.geonames.org/export/ws-overview.html">GeoNames web
 * services</a>.
 * <p>
 * Note : values for some fields are only returned with sufficient {@link Style}
 * . Accessing these fields (admin codes and admin names, elevation,population)
 * will throw an {@link InsufficientStyleException} if the {@link Style} was not
 * sufficient.
 * 
 * @author marc@geonames
 * 
 */
public class GeoNames {

	private static Logger logger = Logger.getLogger("org.geonames");

	private static String USER_AGENT = "gnwsc/1.1.6";

	private static boolean isAndroid = false;

	private static String geoNamesServer = "http://api.geonames.org";

	private static String geoNamesServerFailover = "http://api.geonames.org";

	private static long timeOfLastFailureMainServer;

	private static long averageConnectTime;

	private static long averageSampleSize = 20;

	private static Style defaultStyle = Style.MEDIUM;

	private static int readTimeOut = 120000;

	private static int connectTimeOut = 10000;

	private static String DATEFMT = "yyyy-MM-dd HH:mm:ss";

	static {
		USER_AGENT += " (";
		String os = System.getProperty("os.name");
		if (os != null) {
			USER_AGENT += os + ",";
		}
		String osVersion = System.getProperty("os.version");
		if (osVersion != null) {
			USER_AGENT += osVersion;
		}
		USER_AGENT += ")";

		// android version
		try {
			Class aClass = Class.forName("android.os.Build");
			if (aClass != null) {
				isAndroid = true;
				Field[] fields = aClass.getFields();
				if (fields != null) {
					for (Field field : fields) {
						if ("MODEL".equalsIgnoreCase(field.getName())) {
							USER_AGENT += "(" + field.get(aClass) + ", ";
						}
					}
				}
				aClass = Class.forName("android.os.Build$VERSION");
				if (aClass != null) {
					fields = aClass.getFields();
					if (fields != null) {
						for (Field field : fields) {
							if ("RELEASE".equalsIgnoreCase(field.getName())) {
								USER_AGENT += field.get(aClass);
							}
						}
					}
				}
				USER_AGENT += ")";
			}
		} catch (Throwable t) {
		}
	}

	/**
	 * user name to pass to commercial web services for authentication and
	 * authorization
	 */
	private static String userName;

	/**
	 * token to pass to as optional authentication parameter to the commercial
	 * web services.
	 */
	private static String token;

	/**
	 * adds the username stored in a static variable to the url. It also adds a
	 * token if one has been set with the static setter previously.
	 * 
	 * @param url
	 * @return url with the username appended
	 */
	private static String addUserName(String url) {
		if (userName != null) {
			url = url + "&username=" + userName;
		}
		if (token != null) {
			url = url + "&token=" + token;
		}
		return url;
	}

	/**
	 * adds the default style to the url. The default style can be set with the
	 * static setter. It is 'MEDIUM' if not set.
	 * 
	 * @param url
	 * @return url with the style parameter appended
	 */
	private static String addDefaultStyle(String url) {
		if (defaultStyle != Style.MEDIUM) {
			url = url + "&style=" + defaultStyle.name();
		}
		return url;
	}

	/**
	 * returns the currently active server. Normally this is the main server, if
	 * the main server recently failed then the failover server is returned. If
	 * the main server is not available we don't want to try with every request
	 * whether it is available again. We switch to the failover server and try
	 * from time to time whether the main server is again accessible.
	 * 
	 * @return
	 */
	private static String getCurrentlyActiveServer() {
		if (timeOfLastFailureMainServer == 0) {
			// no problems with main server
			return geoNamesServer;
		}
		// we had problems with main server
		if (System.currentTimeMillis() - timeOfLastFailureMainServer > 1000l * 60l * 10l) {
			// but is was some time ago and we switch back to the main server to
			// retry. The problem may have been solved in the mean time.
			timeOfLastFailureMainServer = 0;
			return geoNamesServer;
		}
		if (System.currentTimeMillis() < timeOfLastFailureMainServer) {
			throw new Error("time of last failure cannot be in future.");
		}
		// the problems have been very recent and we continue with failover
		// server
		if (geoNamesServerFailover != null) {
			return geoNamesServerFailover;
		}
		return geoNamesServer;
	}

	/**
	 * @return the isAndroid
	 */
	public static boolean isAndroid() {
		return isAndroid;
	}

	/**
	 * opens the connection to the url and sets the user agent. In case of an
	 * IOException it checks whether a failover server is set and connects to
	 * the failover server if it has been defined and if it is different from
	 * the normal server.
	 * 
	 * @param url
	 *            the url to connect to
	 * @return returns the inputstream for the connection
	 * @throws IOException
	 */
	private static InputStream connect(String url) throws IOException {
		int status = 0;
		String currentlyActiveServer = getCurrentlyActiveServer();
		try {
			long begin = System.currentTimeMillis();
			HttpURLConnection httpConnection = (HttpURLConnection) new URL(
					currentlyActiveServer + url).openConnection();
			httpConnection.setConnectTimeout(connectTimeOut);
			httpConnection.setReadTimeout(readTimeOut);
			httpConnection.setRequestProperty("User-Agent", USER_AGENT);
			InputStream in = httpConnection.getInputStream();
			status = httpConnection.getResponseCode();

			if (status == 200) {
				long elapsedTime = System.currentTimeMillis() - begin;
				averageConnectTime = (averageConnectTime
						* (averageSampleSize - 1) + elapsedTime)
						/ averageSampleSize;
				// if the average elapsed time is too long we switch server
				if (geoNamesServerFailover != null
						&& averageConnectTime > 5000
						&& !currentlyActiveServer
								.equals(geoNamesServerFailover)) {
					timeOfLastFailureMainServer = System.currentTimeMillis();
				}
				return in;
			}
		} catch (IOException e) {
			return tryFailoverServer(url, currentlyActiveServer, 0, e);
		}
		// we only get here if we had a statuscode <> 200
		IOException ioException = new IOException("status code " + status
				+ " for " + url);
		return tryFailoverServer(url, currentlyActiveServer, status,
				ioException);
	}

	private static synchronized InputStream tryFailoverServer(String url,
			String currentlyActiveServer, int status, IOException e)
			throws MalformedURLException, IOException {
		// we cannot reach the server
		logger.log(Level.WARNING, "problems connecting to geonames server "
				+ currentlyActiveServer, e);
		// is a failover server defined?
		if (geoNamesServerFailover == null
		// is it different from the one we are using?
				|| currentlyActiveServer.equals(geoNamesServerFailover)) {
			if (currentlyActiveServer.equals(geoNamesServerFailover)) {
				// failover server is not accessible, we throw exception
				// and switch back to main server.
				timeOfLastFailureMainServer = 0;
			}
			throw e;
		}
		timeOfLastFailureMainServer = System.currentTimeMillis();
		logger.info("trying to connect to failover server "
				+ geoNamesServerFailover);
		// try failover server
		URLConnection conn = new URL(geoNamesServerFailover + url)
				.openConnection();
		String userAgent = USER_AGENT + " failover from " + geoNamesServer;
		if (status != 0) {
			userAgent += " " + status;
		}
		conn.setRequestProperty("User-Agent", userAgent);
		InputStream in = conn.getInputStream();
		return in;
	}

	private static Element connectAndParse(String url)
			throws GeoNamesException, IOException, JDOMException {
		SAXBuilder parser = new SAXBuilder();
		Document doc = parser.build(connect(url));
		try {
			Element root = rootAndCheckException(doc);
			return root;
		} catch (GeoNamesException geoNamesException) {
			if (geoNamesException.getExceptionCode() == 13
					|| (geoNamesException.getMessage() != null && geoNamesException
							.getMessage()
							.indexOf(
									"canceling statement due to statement timeout") > -1)) {
				String currentlyActiveServer = getCurrentlyActiveServer();
				if (geoNamesServerFailover != null
						&& !currentlyActiveServer
								.equals(geoNamesServerFailover)) {
					timeOfLastFailureMainServer = System.currentTimeMillis();
					doc = parser.build(connect(url));
					Element root = rootAndCheckException(doc);
					return root;
				}
			}
			throw geoNamesException;
		}
	}

	private static Element rootAndCheckException(Document doc)
			throws GeoNamesException {
		Element root = doc.getRootElement();
		checkException(root);
		return root;
	}

	private static void checkException(Element root) throws GeoNamesException {
		Element message = root.getChild("status");
		if (message != null) {
			int code = 0;
			try {
				code = Integer.parseInt(message.getAttributeValue("value"));
			} catch (NumberFormatException numberFormatException) {
			}
			throw new GeoNamesException(code,
					message.getAttributeValue("message"));
		}
	}

	private static Toponym getToponymFromElement(Element toponymElement) {
		Toponym toponym = new Toponym();

		toponym.setName(toponymElement.getChildText("name"));
		toponym.setAlternateNames(toponymElement.getChildText("alternateNames"));
		toponym.setLatitude(Double.parseDouble(toponymElement
				.getChildText("lat")));
		toponym.setLongitude(Double.parseDouble(toponymElement
				.getChildText("lng")));

		String geonameId = toponymElement.getChildText("geonameId");
		if (geonameId != null) {
			toponym.setGeoNameId(Integer.parseInt(geonameId));
		}

		toponym.setContinentCode(toponymElement.getChildText("continentCode"));
		toponym.setCountryCode(toponymElement.getChildText("countryCode"));
		toponym.setCountryName(toponymElement.getChildText("countryName"));

		toponym.setFeatureClass(FeatureClass.fromValue(toponymElement
				.getChildText("fcl")));
		toponym.setFeatureCode(toponymElement.getChildText("fcode"));

		toponym.setFeatureClassName(toponymElement.getChildText("fclName"));
		toponym.setFeatureCodeName(toponymElement.getChildText("fCodeName"));

		String population = toponymElement.getChildText("population");
		if (population != null && !"".equals(population)) {
			toponym.setPopulation(Long.parseLong(population));
		}
		String elevation = toponymElement.getChildText("elevation");
		if (elevation != null && !"".equals(elevation)) {
			toponym.setElevation(Integer.parseInt(elevation));
		}

		toponym.setAdminCode1(toponymElement.getChildText("adminCode1"));
		toponym.setAdminName1(toponymElement.getChildText("adminName1"));
		toponym.setAdminCode2(toponymElement.getChildText("adminCode2"));
		toponym.setAdminName2(toponymElement.getChildText("adminName2"));
		toponym.setAdminCode3(toponymElement.getChildText("adminCode3"));
		toponym.setAdminCode4(toponymElement.getChildText("adminCode4"));

		Element timezoneElement = toponymElement.getChild("timezone");
		if (timezoneElement != null) {
			Timezone timezone = new Timezone();
			timezone.setTimezoneId(timezoneElement.getValue());
			timezone.setDstOffset(Double.parseDouble(timezoneElement
					.getAttributeValue("dstOffset")));
			timezone.setGmtOffset(Double.parseDouble(timezoneElement
					.getAttributeValue("gmtOffset")));
			toponym.setTimezone(timezone);
		}
		return toponym;
	}

	private static WikipediaArticle getWikipediaArticleFromElement(
			Element wikipediaArticleElement) {
		WikipediaArticle wikipediaArticle = new WikipediaArticle();
		wikipediaArticle.setLanguage(wikipediaArticleElement
				.getChildText("lang"));
		wikipediaArticle
				.setTitle(wikipediaArticleElement.getChildText("title"));
		wikipediaArticle.setSummary(wikipediaArticleElement
				.getChildText("summary"));
		wikipediaArticle.setFeature(wikipediaArticleElement
				.getChildText("feature"));
		wikipediaArticle.setWikipediaUrl(wikipediaArticleElement
				.getChildText("wikipediaUrl"));
		wikipediaArticle.setThumbnailImg(wikipediaArticleElement
				.getChildText("thumbnailImg"));

		wikipediaArticle.setLatitude(Double.parseDouble(wikipediaArticleElement
				.getChildText("lat")));
		wikipediaArticle.setLongitude(Double
				.parseDouble(wikipediaArticleElement.getChildText("lng")));

		wikipediaArticle.setRank(Integer.parseInt(wikipediaArticleElement
				.getChildText("rank")));

		String population = wikipediaArticleElement.getChildText("population");
		if (population != null && !"".equals(population)) {
			wikipediaArticle.setPopulation(Integer.parseInt(population));
		}

		String elevation = wikipediaArticleElement.getChildText("elevation");
		if (elevation != null && !"".equals(elevation)) {
			wikipediaArticle.setElevation(Integer.parseInt(elevation));
		}
		return wikipediaArticle;
	}

	private static TimeZone utc = TimeZone.getTimeZone("UTC");

	private static WeatherObservation getWeatherObservationFromElement(
			Element weatherObservationElement) throws ParseException {
		WeatherObservation weatherObservation = new WeatherObservation();
		weatherObservation.setObservation(weatherObservationElement
				.getChildText("observation"));
		SimpleDateFormat df = new SimpleDateFormat(DATEFMT);
		df.setTimeZone(utc);
		weatherObservation.setObservationTime(df
				.parse(weatherObservationElement
						.getChildText("observationTime")));
		weatherObservation.setStationName(weatherObservationElement
				.getChildText("stationName"));
		weatherObservation.setIcaoCode(weatherObservationElement
				.getChildText("ICAO"));
		weatherObservation.setCountryCode(weatherObservationElement
				.getChildText("countryCode"));
		String elevation = weatherObservationElement.getChildText("elevation");
		if (elevation != null && !"".equals(elevation)) {
			weatherObservation.setElevation(Integer.parseInt(elevation));
		}
		weatherObservation.setLatitude(Double
				.parseDouble(weatherObservationElement.getChildText("lat")));
		weatherObservation.setLongitude(Double
				.parseDouble(weatherObservationElement.getChildText("lng")));
		String temperature = weatherObservationElement
				.getChildText("temperature");
		if (temperature != null && !"".equals(temperature)) {
			weatherObservation.setTemperature(Double.parseDouble(temperature));
		}
		String dewPoint = weatherObservationElement.getChildText("dewPoint");
		if (dewPoint != null && !"".equals(dewPoint)) {
			weatherObservation.setDewPoint(Double.parseDouble(dewPoint));
		}
		String humidity = weatherObservationElement.getChildText("humidity");
		if (humidity != null && !"".equals(humidity)) {
			weatherObservation.setHumidity(Double.parseDouble(humidity));
		}
		weatherObservation.setClouds(weatherObservationElement
				.getChildText("clouds"));
		weatherObservation.setWeatherCondition(weatherObservationElement
				.getChildText("weatherCondition"));
		weatherObservation.setWindSpeed(weatherObservationElement
				.getChildText("windSpeed"));
		return weatherObservation;

	}

	/**
	 * returns a list of postal codes for the given parameters. This method is
	 * for convenience.
	 * 
	 * @param postalCode
	 * @param placeName
	 * @param countryCode
	 * @return
	 * @throws Exception
	 */
	public static List<PostalCode> postalCodeSearch(String postalCode,
			String placeName, String countryCode) throws Exception {
		PostalCodeSearchCriteria postalCodeSearchCriteria = new PostalCodeSearchCriteria();
		postalCodeSearchCriteria.setPostalCode(postalCode);
		postalCodeSearchCriteria.setPlaceName(placeName);
		postalCodeSearchCriteria.setCountryCode(countryCode);
		return postalCodeSearch(postalCodeSearchCriteria);
	}

	/**
	 * returns a list of postal codes for the given search criteria matching a
	 * full text search on the GeoNames postal codes database.
	 * 
	 * @param postalCodeSearchCriteria
	 * @return
	 * @throws Exception
	 */
	public static List<PostalCode> postalCodeSearch(
			PostalCodeSearchCriteria postalCodeSearchCriteria) throws Exception {
		List<PostalCode> postalCodes = new ArrayList<PostalCode>();

		String url = "/postalCodeSearch?";
		if (postalCodeSearchCriteria.getPostalCode() != null) {
			url = url
					+ "postalcode="
					+ URLEncoder.encode(
							postalCodeSearchCriteria.getPostalCode(), "UTF8");
		}
		if (postalCodeSearchCriteria.getPlaceName() != null) {
			if (!url.endsWith("&")) {
				url = url + "&";
			}
			url = url
					+ "placename="
					+ URLEncoder.encode(
							postalCodeSearchCriteria.getPlaceName(), "UTF8");
		}
		if (postalCodeSearchCriteria.getAdminCode1() != null) {
			url = url
					+ "&adminCode1="
					+ URLEncoder.encode(
							postalCodeSearchCriteria.getAdminCode1(), "UTF8");
		}

		if (postalCodeSearchCriteria.getCountryCode() != null) {
			if (!url.endsWith("&")) {
				url = url + "&";
			}
			url = url + "country=" + postalCodeSearchCriteria.getCountryCode();
		}
		if (postalCodeSearchCriteria.getCountryBias() != null) {
			if (!url.endsWith("&")) {
				url = url + "&";
			}
			url = url + "countryBias="
					+ postalCodeSearchCriteria.getCountryBias();
		}
		if (postalCodeSearchCriteria.getMaxRows() > 0) {
			url = url + "&maxRows=" + postalCodeSearchCriteria.getMaxRows();
		}
		if (postalCodeSearchCriteria.getStartRow() > 0) {
			url = url + "&startRow=" + postalCodeSearchCriteria.getStartRow();
		}
		if (postalCodeSearchCriteria.isOROperator()) {
			url = url + "&operator=OR";
		}
		if (postalCodeSearchCriteria.isReduced() != null) {
			url = url + "&isReduced="
					+ postalCodeSearchCriteria.isReduced().toString();
		}
		url = addUserName(url);

		Element root = connectAndParse(url);
		for (Object obj : root.getChildren("code")) {
			Element codeElement = (Element) obj;
			PostalCode code = new PostalCode();
			code.setPostalCode(codeElement.getChildText("postalcode"));
			code.setPlaceName(codeElement.getChildText("name"));
			code.setCountryCode(codeElement.getChildText("countryCode"));
			code.setAdminCode1(codeElement.getChildText("adminCode1"));
			code.setAdminCode2(codeElement.getChildText("adminCode2"));
			code.setAdminName1(codeElement.getChildText("adminName1"));
			code.setAdminName2(codeElement.getChildText("adminName2"));

			code.setLatitude(Double.parseDouble(codeElement.getChildText("lat")));
			code.setLongitude(Double.parseDouble(codeElement
					.getChildText("lng")));

			postalCodes.add(code);
		}

		return postalCodes;
	}

	/**
	 * returns a list of postal codes
	 * 
	 * @param postalCodeSearchCriteria
	 * @return
	 * @throws Exception
	 */
	public static List<PostalCode> findNearbyPostalCodes(
			PostalCodeSearchCriteria postalCodeSearchCriteria) throws Exception {

		List<PostalCode> postalCodes = new ArrayList<PostalCode>();

		String url = "/findNearbyPostalCodes?";
		if (postalCodeSearchCriteria.getPostalCode() != null) {
			url = url
					+ "&postalcode="
					+ URLEncoder.encode(
							postalCodeSearchCriteria.getPostalCode(), "UTF8");
		}
		if (postalCodeSearchCriteria.getPlaceName() != null) {
			url = url
					+ "&placename="
					+ URLEncoder.encode(
							postalCodeSearchCriteria.getPlaceName(), "UTF8");
		}
		if (postalCodeSearchCriteria.getCountryCode() != null) {
			url = url + "&country=" + postalCodeSearchCriteria.getCountryCode();
		}

		if (postalCodeSearchCriteria.getLatitude() != null) {
			url = url + "&lat=" + postalCodeSearchCriteria.getLatitude();
		}
		if (postalCodeSearchCriteria.getLongitude() != null) {
			url = url + "&lng=" + postalCodeSearchCriteria.getLongitude();
		}
		if (postalCodeSearchCriteria.getStyle() != null) {
			url = url + "&style=" + postalCodeSearchCriteria.getStyle();
		}
		if (postalCodeSearchCriteria.getMaxRows() > 0) {
			url = url + "&maxRows=" + postalCodeSearchCriteria.getMaxRows();
		}

		if (postalCodeSearchCriteria.getRadius() > 0) {
			url = url + "&radius=" + postalCodeSearchCriteria.getRadius();
		}
		url = addUserName(url);

		Element root = connectAndParse(url);
		for (Object obj : root.getChildren("code")) {
			Element codeElement = (Element) obj;
			PostalCode code = new PostalCode();
			code.setPostalCode(codeElement.getChildText("postalcode"));
			code.setPlaceName(codeElement.getChildText("name"));
			code.setCountryCode(codeElement.getChildText("countryCode"));

			code.setLatitude(Double.parseDouble(codeElement.getChildText("lat")));
			code.setLongitude(Double.parseDouble(codeElement
					.getChildText("lng")));

			code.setAdminName1(codeElement.getChildText("adminName1"));
			code.setAdminCode1(codeElement.getChildText("adminCode1"));
			code.setAdminName2(codeElement.getChildText("adminName2"));
			code.setAdminCode2(codeElement.getChildText("adminCode2"));

			if (codeElement.getChildText("distance") != null) {
				code.setDistance(Double.parseDouble(codeElement
						.getChildText("distance")));
			}

			postalCodes.add(code);
		}

		return postalCodes;
	}

	/**
	 * convenience method for
	 * {@link #findNearbyPlaceName(double,double,double,int)}
	 * 
	 * @param latitude
	 * @param longitude
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public static List<Toponym> findNearbyPlaceName(double latitude,
			double longitude) throws IOException, Exception {
		return findNearbyPlaceName(latitude, longitude, 0, 0);
	}

	public static List<Toponym> findNearbyPlaceName(double latitude,
			double longitude, double radius, int maxRows) throws IOException,
			Exception {
		List<Toponym> places = new ArrayList<Toponym>();

		String url = "/findNearbyPlaceName?";

		url = url + "&lat=" + latitude;
		url = url + "&lng=" + longitude;
		if (radius > 0) {
			url = url + "&radius=" + radius;
		}
		if (maxRows > 0) {
			url = url + "&maxRows=" + maxRows;
		}
		url = addUserName(url);
		url = addDefaultStyle(url);

		Element root = connectAndParse(url);
		for (Object obj : root.getChildren("geoname")) {
			Element toponymElement = (Element) obj;
			Toponym toponym = getToponymFromElement(toponymElement);
			places.add(toponym);
		}

		return places;
	}

	public static List<Toponym> findNearby(double latitude, double longitude,
			FeatureClass featureClass, String[] featureCodes)
			throws IOException, Exception {
		return findNearby(latitude, longitude, 0, featureClass, featureCodes,
				null, 0);
	}

	/* Overload function to allow backward compatibility */
	/**
	 * Based on the following inforamtion: Webservice Type : REST
	 * ws.geonames.org/findNearbyWikipedia? Parameters : lang : language code
	 * (around 240 languages) (default = en) lat,lng, radius (in km), maxRows
	 * (default = 10) featureClass featureCode Example:
	 * http://ws.geonames.org/findNearby?lat=47.3&lng=9
	 * 
	 * @param: latitude
	 * @param: longitude
	 * @param: radius
	 * @param: feature Class
	 * @param: feature Codes
	 * @param: language
	 * @param: maxRows
	 * @return: list of wikipedia articles
	 * @throws: Exception
	 */
	public static List<Toponym> findNearby(double latitude, double longitude,
			double radius, FeatureClass featureClass, String[] featureCodes,
			String language, int maxRows) throws IOException, Exception {
		List<Toponym> places = new ArrayList<Toponym>();

		String url = "/findNearby?";

		url += "&lat=" + latitude;
		url += "&lng=" + longitude;
		if (radius > 0) {
			url = url + "&radius=" + radius;
		}
		if (maxRows > 0) {
			url = url + "&maxRows=" + maxRows;
		}

		if (language != null) {
			url = url + "&lang=" + language;
		}

		if (featureClass != null) {
			url += "&featureClass=" + featureClass;
		}
		if (featureCodes != null && featureCodes.length > 0) {
			for (String featureCode : featureCodes) {
				url += "&featureCode=" + featureCode;
			}
		}

		url = addUserName(url);
		url = addDefaultStyle(url);

		Element root = connectAndParse(url);
		for (Object obj : root.getChildren("geoname")) {
			Element toponymElement = (Element) obj;
			Toponym toponym = getToponymFromElement(toponymElement);
			places.add(toponym);
		}

		return places;
	}

	/**
	 * 
	 * @param geoNameId
	 * @param language
	 *            - optional
	 * @param style
	 *            - optional
	 * @return the toponym for the geoNameId
	 * @throws IOException
	 * @throws Exception
	 */
	public static Toponym get(int geoNameId, String language, String style)
			throws IOException, Exception {
		String url = "/get?";

		url += "geonameId=" + geoNameId;

		if (language != null) {
			url = url + "&lang=" + language;
		}

		if (style != null) {
			url = url + "&style=" + style;
		} else {
			url = addDefaultStyle(url);
		}
		url = addUserName(url);

		Element root = connectAndParse(url);
		Toponym toponym = getToponymFromElement(root);
		return toponym;
	}

	public static Address findNearestAddress(double latitude, double longitude)
			throws IOException, Exception {

		String url = "/findNearestAddress?";

		url = url + "&lat=" + latitude;
		url = url + "&lng=" + longitude;
		url = addUserName(url);

		Element root = connectAndParse(url);
		for (Object obj : root.getChildren("address")) {
			Element codeElement = (Element) obj;
			Address address = new Address();
			address.setStreet(codeElement.getChildText("street"));
			address.setStreetNumber(codeElement.getChildText("streetNumber"));
			address.setMtfcc(codeElement.getChildText("mtfcc"));

			address.setPostalCode(codeElement.getChildText("postalcode"));
			address.setPlaceName(codeElement.getChildText("placename"));
			address.setCountryCode(codeElement.getChildText("countryCode"));

			address.setLatitude(Double.parseDouble(codeElement
					.getChildText("lat")));
			address.setLongitude(Double.parseDouble(codeElement
					.getChildText("lng")));

			address.setAdminName1(codeElement.getChildText("adminName1"));
			address.setAdminCode1(codeElement.getChildText("adminCode1"));
			address.setAdminName2(codeElement.getChildText("adminName2"));
			address.setAdminCode2(codeElement.getChildText("adminCode2"));

			address.setDistance(Double.parseDouble(codeElement
					.getChildText("distance")));

			return address;
		}

		return null;
	}

	public static Intersection findNearestIntersection(double latitude,
			double longitude) throws Exception {
		return findNearestIntersection(latitude, longitude, 0);
	}

	public static Intersection findNearestIntersection(double latitude,
			double longitude, double radius) throws Exception {

		String url = "/findNearestIntersection?";

		url = url + "&lat=" + latitude;
		url = url + "&lng=" + longitude;
		if (radius > 0) {
			url = url + "&radius=" + radius;
		}
		url = addUserName(url);

		Element root = connectAndParse(url);
		for (Object obj : root.getChildren("intersection")) {
			Element e = (Element) obj;
			Intersection intersection = new Intersection();
			intersection.setStreet1(e.getChildText("street1"));
			intersection.setStreet2(e.getChildText("street2"));
			intersection.setLatitude(Double.parseDouble(e.getChildText("lat")));
			intersection
					.setLongitude(Double.parseDouble(e.getChildText("lng")));
			intersection.setDistance(Double.parseDouble(e
					.getChildText("distance")));
			intersection.setPostalCode(e.getChildText("postalcode"));
			intersection.setPlaceName(e.getChildText("placename"));
			intersection.setCountryCode(e.getChildText("countryCode"));
			intersection.setAdminName2(e.getChildText("adminName2"));
			intersection.setAdminCode1(e.getChildText("adminCode1"));
			intersection.setAdminName1(e.getChildText("adminName1"));
			return intersection;
		}
		return null;
	}

	/**
	 * 
	 * @see <a * href=
	 *      "http://www.geonames.org/maps/reverse-geocoder.html#findNearbyStreets"
	 *      > web service documentation</a>
	 * 
	 * @param latitude
	 * @param longitude
	 * @param radius
	 * @return
	 * @throws Exception
	 */
	public static List<StreetSegment> findNearbyStreets(double latitude,
			double longitude, double radius) throws Exception {

		String url = "/findNearbyStreets?";

		url = url + "&lat=" + latitude;
		url = url + "&lng=" + longitude;
		if (radius > 0) {
			url = url + "&radius=" + radius;
		}
		url = addUserName(url);

		List<StreetSegment> segments = new ArrayList<StreetSegment>();

		Element root = connectAndParse(url);
		for (Object obj : root.getChildren("streetSegment")) {
			Element e = (Element) obj;
			StreetSegment streetSegment = new StreetSegment();
			String line = e.getChildText("line");
			String[] points = line.split(",");
			double[] latArray = new double[points.length];
			double[] lngArray = new double[points.length];
			for (int i = 0; i < points.length; i++) {
				String[] coords = points[i].split(" ");
				lngArray[i] = Double.parseDouble(coords[0]);
				latArray[i] = Double.parseDouble(coords[1]);
			}

			streetSegment.setCfcc(e.getChildText("cfcc"));
			streetSegment.setName(e.getChildText("name"));
			streetSegment.setFraddl(e.getChildText("fraddl"));
			streetSegment.setFraddr(e.getChildText("fraddr"));
			streetSegment.setToaddl(e.getChildText("toaddl"));
			streetSegment.setToaddr(e.getChildText("toaddr"));
			streetSegment.setPostalCode(e.getChildText("postalcode"));
			streetSegment.setPlaceName(e.getChildText("placename"));
			streetSegment.setCountryCode(e.getChildText("countryCode"));
			streetSegment.setAdminName2(e.getChildText("adminName2"));
			streetSegment.setAdminCode1(e.getChildText("adminCode1"));
			streetSegment.setAdminName1(e.getChildText("adminName1"));
			segments.add(streetSegment);
		}
		return segments;
	}

	public static List<StreetSegment> findNearbyStreetsOSM(double latitude,
			double longitude, double radius) throws Exception {

		String url = "/findNearbyStreetsOSM?";

		url = url + "&lat=" + latitude;
		url = url + "&lng=" + longitude;
		if (radius > 0) {
			url = url + "&radius=" + radius;
		}
		url = addUserName(url);

		List<StreetSegment> segments = new ArrayList<StreetSegment>();

		Element root = connectAndParse(url);
		for (Object obj : root.getChildren("streetSegment")) {
			Element e = (Element) obj;
			StreetSegment streetSegment = new StreetSegment();
			String line = e.getChildText("line");
			String[] points = line.split(",");
			double[] latArray = new double[points.length];
			double[] lngArray = new double[points.length];
			for (int i = 0; i < points.length; i++) {
				String[] coords = points[i].split(" ");
				lngArray[i] = Double.parseDouble(coords[0]);
				latArray[i] = Double.parseDouble(coords[1]);
			}

			streetSegment.setName(e.getChildText("name"));
			segments.add(streetSegment);
		}
		return segments;
	}

	/**
	 * convenience method for {@link #search(ToponymSearchCriteria)}
	 * 
	 * @see <a href="http://www.geonames.org/export/geonames-search.html">search
	 *      web service documentation</a>
	 * 
	 * @param q
	 * @param countryCode
	 * @param name
	 * @param featureCodes
	 * @param startRow
	 * @return
	 * @throws Exception
	 */
	public static ToponymSearchResult search(String q, String countryCode,
			String name, String[] featureCodes, int startRow) throws Exception {
		return search(q, countryCode, name, featureCodes, startRow, null, null,
				null);
	}

	/**
	 * convenience method for {@link #search(ToponymSearchCriteria)}
	 * 
	 * The string fields will be transparently utf8 encoded within the call.
	 * 
	 * @see <a href="http://www.geonames.org/export/geonames-search.html">search
	 *      web service documentation</a>
	 * 
	 * @param q
	 *            search over all fields
	 * @param countryCode
	 * @param name
	 *            search over name only
	 * @param featureCodes
	 * @param startRow
	 * @param language
	 * @param style
	 * @param exactName
	 * @return
	 * @throws Exception
	 */
	public static ToponymSearchResult search(String q, String countryCode,
			String name, String[] featureCodes, int startRow, String language,
			Style style, String exactName) throws Exception {
		ToponymSearchCriteria searchCriteria = new ToponymSearchCriteria();
		searchCriteria.setQ(q);
		searchCriteria.setCountryCode(countryCode);
		searchCriteria.setName(name);
		searchCriteria.setFeatureCodes(featureCodes);
		searchCriteria.setStartRow(startRow);
		searchCriteria.setLanguage(language);
		searchCriteria.setStyle(style);
		searchCriteria.setNameEquals(exactName);
		return search(searchCriteria);
	}

	/**
	 * full text search on the GeoNames database.
	 * 
	 * This service gets the number of toponyms defined by the 'maxRows'
	 * parameter. The parameter 'style' determines which fields are returned by
	 * the service.
	 * 
	 * @see <a href="http://www.geonames.org/export/geonames-search.html">search
	 *      web service documentation</a>
	 * 
	 * <br>
	 * 
	 *      <pre>
	 * ToponymSearchCriteria searchCriteria = new ToponymSearchCriteria();
	 * searchCriteria.setQ(&quot;z&amp;uumlrich&quot;);
	 * ToponymSearchResult searchResult = GeoNames.search(searchCriteria);
	 * for (Toponym toponym : searchResult.toponyms) {
	 * 	System.out.println(toponym.getName() + &quot; &quot; + toponym.getCountryName());
	 * }
	 * </pre>
	 * 
	 * 
	 * @param searchCriteria
	 * @return
	 * @throws Exception
	 */
	public static ToponymSearchResult search(
			ToponymSearchCriteria searchCriteria) throws Exception {
		ToponymSearchResult searchResult = new ToponymSearchResult();

		String url = "/search?";

		if (searchCriteria.getQ() != null) {
			url = url + "q=" + URLEncoder.encode(searchCriteria.getQ(), "UTF8");
		}
		if (searchCriteria.getNameEquals() != null) {
			url = url + "&name_equals="
					+ URLEncoder.encode(searchCriteria.getNameEquals(), "UTF8");
		}
		if (searchCriteria.getNameStartsWith() != null) {
			url = url
					+ "&name_startsWith="
					+ URLEncoder.encode(searchCriteria.getNameStartsWith(),
							"UTF8");
		}

		if (searchCriteria.getName() != null) {
			url = url + "&name="
					+ URLEncoder.encode(searchCriteria.getName(), "UTF8");
		}

		if (searchCriteria.getTag() != null) {
			url = url + "&tag="
					+ URLEncoder.encode(searchCriteria.getTag(), "UTF8");
		}

		if (searchCriteria.getCountryCode() != null) {
			url = url + "&country=" + searchCriteria.getCountryCode();
		}
		if (searchCriteria.getCountryBias() != null) {
			if (!url.endsWith("&")) {
				url = url + "&";
			}
			url = url + "countryBias=" + searchCriteria.getCountryBias();
		}
		if (searchCriteria.getContinentCode() != null) {
			url = url + "&continentCode=" + searchCriteria.getContinentCode();
		}

		if (searchCriteria.getAdminCode1() != null) {
			url = url + "&adminCode1="
					+ URLEncoder.encode(searchCriteria.getAdminCode1(), "UTF8");
		}
		if (searchCriteria.getAdminCode2() != null) {
			url = url + "&adminCode2="
					+ URLEncoder.encode(searchCriteria.getAdminCode2(), "UTF8");
		}
		if (searchCriteria.getAdminCode3() != null) {
			url = url + "&adminCode3="
					+ URLEncoder.encode(searchCriteria.getAdminCode3(), "UTF8");
		}
		if (searchCriteria.getAdminCode4() != null) {
			url = url + "&adminCode4="
					+ URLEncoder.encode(searchCriteria.getAdminCode4(), "UTF8");
		}

		if (searchCriteria.getLanguage() != null) {
			url = url + "&lang=" + searchCriteria.getLanguage();
		}

		if (searchCriteria.getFeatureClass() != null) {
			url = url + "&featureClass=" + searchCriteria.getFeatureClass();
		}

		if (searchCriteria.getFeatureCodes() != null) {
			for (String featureCode : searchCriteria.getFeatureCodes()) {
				url = url + "&fcode=" + featureCode;
			}
		}
		if (searchCriteria.getMaxRows() > 0) {
			url = url + "&maxRows=" + searchCriteria.getMaxRows();
		}
		if (searchCriteria.getStartRow() > 0) {
			url = url + "&startRow=" + searchCriteria.getStartRow();
		}

		if (searchCriteria.getStyle() != null) {
			url = url + "&style=" + searchCriteria.getStyle();
		} else {
			url = addDefaultStyle(url);
		}
		url = addUserName(url);

		Element root = connectAndParse(url);
		searchResult.totalResultsCount = Integer.parseInt(root
				.getChildText("totalResultsCount"));
		searchResult.setStyle(Style.valueOf(root.getAttributeValue("style")));

		for (Object obj : root.getChildren("geoname")) {
			Element toponymElement = (Element) obj;
			Toponym toponym = getToponymFromElement(toponymElement);
			toponym.setStyle(searchResult.getStyle());
			searchResult.toponyms.add(toponym);
		}

		return searchResult;
	}

	/**
	 * returns the children in the administrative hierarchy of a toponym.
	 * 
	 * @param geonameId
	 * @param language
	 * @param style
	 * @return
	 * @throws Exception
	 */
	public static ToponymSearchResult children(int geonameId, String language,
			Style style) throws Exception {
		ToponymSearchResult searchResult = new ToponymSearchResult();

		String url = "/children?";

		url = url + "geonameId=" + geonameId;

		if (language != null) {
			url = url + "&lang=" + language;
		}

		if (style != null) {
			url = url + "&style=" + style;
		} else {
			url = addDefaultStyle(url);
		}
		url = addUserName(url);

		Element root = connectAndParse(url);
		searchResult.totalResultsCount = Integer.parseInt(root
				.getChildText("totalResultsCount"));
		searchResult.setStyle(Style.valueOf(root.getAttributeValue("style")));

		for (Object obj : root.getChildren("geoname")) {
			Element toponymElement = (Element) obj;
			Toponym toponym = getToponymFromElement(toponymElement);
			searchResult.toponyms.add(toponym);
		}

		return searchResult;
	}

	/**
	 * returns the neighbours of a toponym.
	 * 
	 * @param geonameId
	 * @param language
	 * @param style
	 * @return
	 * @throws Exception
	 */
	public static ToponymSearchResult neighbours(int geonameId,
			String language, Style style) throws Exception {
		ToponymSearchResult searchResult = new ToponymSearchResult();

		String url = "/neighbours?";

		url = url + "geonameId=" + geonameId;

		if (language != null) {
			url = url + "&lang=" + language;
		}

		if (style != null) {
			url = url + "&style=" + style;
		} else {
			url = addDefaultStyle(url);
		}
		url = addUserName(url);

		Element root = connectAndParse(url);
		searchResult.totalResultsCount = Integer.parseInt(root
				.getChildText("totalResultsCount"));
		searchResult.setStyle(Style.valueOf(root.getAttributeValue("style")));

		for (Object obj : root.getChildren("geoname")) {
			Element toponymElement = (Element) obj;
			Toponym toponym = getToponymFromElement(toponymElement);
			searchResult.toponyms.add(toponym);
		}

		return searchResult;
	}

	/**
	 * returns the hierarchy for a geonameId
	 * 
	 * @see <a
	 *      href="http://www.geonames.org/export/place-hierarchy.html#hierarchy">Hierarchy
	 *      service description</a>
	 * 
	 * @param geonameId
	 * @param language
	 * @param style
	 * @return
	 * @throws Exception
	 */
	public static List<Toponym> hierarchy(int geonameId, String language,
			Style style) throws Exception {

		String url = "/hierarchy?";

		url = url + "geonameId=" + geonameId;

		if (language != null) {
			url = url + "&lang=" + language;
		}

		if (style != null) {
			url = url + "&style=" + style;
		} else {
			url = addDefaultStyle(url);
		}
		url = addUserName(url);

		Element root = connectAndParse(url);
		List<Toponym> toponyms = new ArrayList<Toponym>();
		for (Object obj : root.getChildren("geoname")) {
			Element toponymElement = (Element) obj;
			Toponym toponym = getToponymFromElement(toponymElement);
			toponyms.add(toponym);
		}

		return toponyms;
	}

	public static void saveTags(String[] tags, Toponym toponym,
			String username, String password) throws Exception {
		if (toponym.getGeoNameId() == 0) {
			throw new Error("no geonameid specified");
		}

		// FIXME proper url
		String url = "/servlet/geonames?srv=61";

		url = url + "&geonameId=" + toponym.getGeoNameId();
		url = addUserName(url);

		StringBuilder tagsCommaseparated = new StringBuilder();
		for (String tag : tags) {
			tagsCommaseparated.append(tag + ",");
		}
		url = url + "&tag=" + tagsCommaseparated;

		Element root = connectAndParse(url);
	}

	/**
	 * full text search on geolocated wikipedia articles.
	 * 
	 * @param q
	 * @param language
	 * @return
	 * @throws Exception
	 */
	public static List<WikipediaArticle> wikipediaSearch(String q,
			String language) throws Exception {
		List<WikipediaArticle> articles = new ArrayList<WikipediaArticle>();

		String url = "/wikipediaSearch?";

		url = url + "q=" + URLEncoder.encode(q, "UTF8");

		if (language != null) {
			url = url + "&lang=" + language;
		}
		url = addUserName(url);

		Element root = connectAndParse(url);
		for (Object obj : root.getChildren("entry")) {
			Element wikipediaArticleElement = (Element) obj;
			WikipediaArticle wikipediaArticle = getWikipediaArticleFromElement(wikipediaArticleElement);
			articles.add(wikipediaArticle);
		}

		return articles;
	}

	/**
	 * full text search on geolocated wikipedia articles.
	 * 
	 * @param title
	 * @param language
	 * @return
	 * @throws Exception
	 */
	public static List<WikipediaArticle> wikipediaSearchForTitle(String title,
			String language) throws Exception {
		List<WikipediaArticle> articles = new ArrayList<WikipediaArticle>();

		String url = "/wikipediaSearch?";

		url = url + "title=" + URLEncoder.encode(title, "UTF8");

		if (language != null) {
			url = url + "&lang=" + language;
		}
		url = addUserName(url);

		Element root = connectAndParse(url);
		for (Object obj : root.getChildren("entry")) {
			Element wikipediaArticleElement = (Element) obj;
			WikipediaArticle wikipediaArticle = getWikipediaArticleFromElement(wikipediaArticleElement);
			articles.add(wikipediaArticle);
		}

		return articles;
	}

	public static List<WikipediaArticle> findNearbyWikipedia(double latitude,
			double longitude, String language) throws Exception {
		return findNearbyWikipedia(latitude, longitude, 0, language, 0);
	}

	/* Overload function to allow backward compatibility */
	/**
	 * Based on the following inform: Webservice Type : REST
	 * ws.geonames.org/findNearbyWikipedia? Parameters : lang : language code
	 * (around 240 languages) (default = en) lat,lng, radius (in km), maxRows
	 * (default = 5) Example:
	 * http://ws.geonames.org/findNearbyWikipedia?lat=47&lng=9
	 * 
	 * @param: latitude
	 * @param: longitude
	 * @param: radius
	 * @param: language
	 * @param: maxRows
	 * @return: list of wikipedia articles
	 * @throws: Exception
	 */
	public static List<WikipediaArticle> findNearbyWikipedia(double latitude,
			double longitude, double radius, String language, int maxRows)
			throws Exception {

		List<WikipediaArticle> articles = new ArrayList<WikipediaArticle>();

		String url = "/findNearbyWikipedia?";

		url = url + "lat=" + latitude;
		url = url + "&lng=" + longitude;
		if (radius > 0) {
			url = url + "&radius=" + radius;
		}
		if (maxRows > 0) {
			url = url + "&maxRows=" + maxRows;
		}

		if (language != null) {
			url = url + "&lang=" + language;
		}
		url = addUserName(url);

		Element root = connectAndParse(url);
		for (Object obj : root.getChildren("entry")) {
			Element wikipediaArticleElement = (Element) obj;
			WikipediaArticle wikipediaArticle = getWikipediaArticleFromElement(wikipediaArticleElement);
			articles.add(wikipediaArticle);
		}

		return articles;
	}

	/**
	 * GTOPO30 is a global digital elevation model (DEM) with a horizontal grid
	 * spacing of 30 arc seconds (approximately 1 kilometer). GTOPO30 was
	 * derived from several raster and vector sources of topographic
	 * information.
	 * 
	 * @param latitude
	 * @param longitude
	 * @return a single number giving the elevation in meters according to
	 *         gtopo30, ocean areas have been masked as "no data" and have been
	 *         assigned a value of -9999
	 * @throws IOException
	 */
	public static int gtopo30(double latitude, double longitude)
			throws IOException {
		String url = "/gtopo30?lat=" + latitude + "&lng=" + longitude;
		url = addUserName(url);
		BufferedReader in = new BufferedReader(new InputStreamReader(
				connect(url), "UTF8"));
		String gtopo30 = in.readLine();
		in.close();
		return Integer.parseInt(gtopo30);
	}

	/**
	 * Shuttle Radar Topography Mission (SRTM) elevation data. SRTM consisted of
	 * a specially modified radar system that flew onboard the Space Shuttle
	 * Endeavour during an 11-day mission in February of 2000. The dataset
	 * covers land areas between 60 degrees north and 56 degrees south. This web
	 * service is using SRTM3 data with data points located every 3-arc-second
	 * (approximately 90 meters) on a latitude/longitude grid.
	 * 
	 * @param latitude
	 * @param longitude
	 * @return elevation or -32768 if unknown
	 * @throws IOException
	 */
	public static int srtm3(double latitude, double longitude)
			throws IOException {
		String url = "/srtm3?lat=" + latitude + "&lng=" + longitude;
		url = addUserName(url);
		BufferedReader in = new BufferedReader(new InputStreamReader(
				connect(url), "UTF8"));
		String srtm3 = in.readLine();
		in.close();
		return Integer.parseInt(srtm3);
	}

	public static int[] srtm3(double[] latitude, double[] longitude)
			throws IOException {
		if (latitude.length != longitude.length) {
			throw new Error("number of lats and longs must be equal");
		}
		int[] elevation = new int[latitude.length];
		String lats = "";
		String lngs = "";
		for (int i = 0; i < elevation.length; i++) {
			lats += latitude[i] + ",";
			lngs += longitude[i] + ",";
		}
		String url = "/srtm3?lats=" + lats + "&lngs=" + lngs;
		url = addUserName(url);
		BufferedReader in = new BufferedReader(new InputStreamReader(
				connect(url), "UTF8"));
		for (int i = 0; i < elevation.length; i++) {
			String srtm3 = in.readLine();
			elevation[i] = Integer.parseInt(srtm3);
		}
		in.close();
		return elevation;
	}

	public static int astergdem(double latitude, double longitude)
			throws IOException {
		String url = "/astergdem?lat=" + latitude + "&lng=" + longitude;
		url = addUserName(url);
		BufferedReader in = new BufferedReader(new InputStreamReader(
				connect(url), "UTF8"));
		String astergdem = in.readLine();
		in.close();
		return Integer.parseInt(astergdem);
	}

	public static int[] astergdem(double[] latitude, double[] longitude)
			throws IOException {
		if (latitude.length != longitude.length) {
			throw new Error("number of lats and longs must be equal");
		}
		int[] elevation = new int[latitude.length];
		String lats = "";
		String lngs = "";
		for (int i = 0; i < elevation.length; i++) {
			lats += latitude[i] + ",";
			lngs += longitude[i] + ",";
		}
		String url = "/astergdem?lats=" + lats + "&lngs=" + lngs;
		url = addUserName(url);
		BufferedReader in = new BufferedReader(new InputStreamReader(
				connect(url), "UTF8"));
		for (int i = 0; i < elevation.length; i++) {
			String astergdem = in.readLine();
			elevation[i] = Integer.parseInt(astergdem);
		}
		in.close();
		return elevation;
	}

	/**
	 * The iso country code of any given point. It is calling
	 * {@link #countryCode(double, double, double)} with radius=0.0
	 * 
	 * @param latitude
	 * @param longitude
	 * @return
	 * @throws IOException
	 */
	public static String countryCode(double latitude, double longitude)
			throws IOException {
		return countryCode(latitude, longitude, 0);
	}

	/**
	 * The iso country code of any given point with radius for coastal areas.
	 * 
	 * @param latitude
	 * @param longitude
	 * @param radius
	 * 
	 * @return iso country code for the given latitude/longitude
	 * @throws IOException
	 */
	public static String countryCode(double latitude, double longitude,
			double radius) throws IOException {
		String url = "/countryCode?lat=" + latitude + "&lng=" + longitude;
		if (radius != 0) {
			url += "&radius=" + radius;
		}
		url = addUserName(url);
		BufferedReader in = new BufferedReader(new InputStreamReader(
				connect(url), "UTF8"));
		String cc = in.readLine();
		in.close();
		if (cc != null && cc.length() == 2) {
			return cc;
		}
		return null;
	}

	/**
	 * get the timezone for a given location
	 * 
	 * @param latitude
	 * @param longitude
	 * @return timezone at the given location
	 * @throws IOException
	 * @throws Exception
	 */
	public static Timezone timezone(double latitude, double longitude)
			throws IOException, Exception {

		String url = "/timezone?";

		url = url + "&lat=" + latitude;
		url = url + "&lng=" + longitude;
		url = addUserName(url);

		Element root = connectAndParse(url);
		for (Object obj : root.getChildren("timezone")) {
			Element codeElement = (Element) obj;
			Timezone timezone = new Timezone();
			timezone.setTimezoneId(codeElement.getChildText("timezoneId"));
			timezone.setCountryCode(codeElement.getChildText("countryCode"));

			if (codeElement.getChildText("time") != null) {
				String minuteDateFmt = "yyyy-MM-dd HH:mm";
				SimpleDateFormat df = null;
				if (codeElement.getChildText("time").length() == minuteDateFmt
						.length()) {
					df = new SimpleDateFormat(minuteDateFmt);
				} else {
					df = new SimpleDateFormat(DATEFMT);
				}
				timezone.setTime(df.parse(codeElement.getChildText("time")));
				if (codeElement.getChildText("sunrise") != null) {
					timezone.setSunrise(df.parse(codeElement
							.getChildText("sunrise")));
				}
				if (codeElement.getChildText("sunset") != null) {
					timezone.setSunset(df.parse(codeElement
							.getChildText("sunset")));
				}
				timezone.setGmtOffset(Double.parseDouble(codeElement
						.getChildText("gmtOffset")));
				timezone.setDstOffset(Double.parseDouble(codeElement
						.getChildText("dstOffset")));
			}
			return timezone;
		}

		return null;
	}

	/**
	 * 
	 * @param latitude
	 * @param longitude
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public static WeatherObservation findNearByWeather(double latitude,
			double longitude) throws IOException, Exception {

		String url = "/findNearByWeatherXML?";

		url = url + "&lat=" + latitude;
		url = url + "&lng=" + longitude;
		url = addUserName(url);

		Element root = connectAndParse(url);
		for (Object obj : root.getChildren("observation")) {
			Element weatherObservationElement = (Element) obj;
			WeatherObservation weatherObservation = getWeatherObservationFromElement(weatherObservationElement);
			return weatherObservation;
		}

		return null;
	}

	public static WeatherObservation weatherIcao(String icaoCode)
			throws IOException, Exception {

		String url = "/weatherIcaoXML?";

		url = url + "&ICAO=" + icaoCode;
		url = addUserName(url);

		Element root = connectAndParse(url);
		for (Object obj : root.getChildren("observation")) {
			Element weatherObservationElement = (Element) obj;
			WeatherObservation weatherObservation = getWeatherObservationFromElement(weatherObservationElement);
			return weatherObservation;
		}

		return null;
	}

	/**
	 * @return the geoNamesServer, default is http://ws.geonames.org
	 */
	public static String getGeoNamesServer() {
		return geoNamesServer;
	}

	/**
	 * @return the geoNamesServerFailover
	 */
	public static String getGeoNamesServerFailover() {
		return geoNamesServerFailover;
	}

	/**
	 * sets the server name for the GeoNames server to be used for the requests.
	 * Default is ws.geonames.org
	 * 
	 * @param geoNamesServer
	 *            the geonamesServer to set
	 */
	public static void setGeoNamesServer(String pGeoNamesServer) {
		if (pGeoNamesServer == null) {
			throw new Error();
		}
		pGeoNamesServer = pGeoNamesServer.trim().toLowerCase();
		// add default http protocol if it is missing
		if (!pGeoNamesServer.startsWith("http://")
				&& !pGeoNamesServer.startsWith("https://")) {
			pGeoNamesServer = "http://" + pGeoNamesServer;
		}
		GeoNames.geoNamesServer = pGeoNamesServer;
	}

	/**
	 * sets the default failover server for requests in case the main server is
	 * not accessible. Default is ws.geonames.org<br>
	 * The failover server is only called if it is different from the main
	 * server.<br>
	 * The failover server is used for commercial GeoNames web service users.
	 * 
	 * @param geoNamesServerFailover
	 *            the geoNamesServerFailover to set
	 */
	public static void setGeoNamesServerFailover(String geoNamesServerFailover) {
		if (geoNamesServerFailover != null) {
			geoNamesServerFailover = geoNamesServerFailover.trim()
					.toLowerCase();
			if (!geoNamesServerFailover.startsWith("http://")) {
				geoNamesServerFailover = "http://" + geoNamesServerFailover;
			}
		}
		GeoNames.geoNamesServerFailover = geoNamesServerFailover;
	}

	/**
	 * @return the userName
	 */
	public static String getUserName() {
		return userName;
	}

	/**
	 * Sets the user name to be used for the requests. Needed to access the
	 * commercial GeoNames web services.
	 * 
	 * @param userName
	 *            the userName to set
	 */
	public static void setUserName(String userName) {
		GeoNames.userName = userName;
	}

	/**
	 * @return the token
	 */
	public static String getToken() {
		return token;
	}

	/**
	 * sets the token to be used to authenticate the requests. This is an
	 * optional parameter for the commercial version of the GeoNames web
	 * services.
	 * 
	 * @param token
	 *            the token to set
	 */
	public static void setToken(String token) {
		GeoNames.token = token;
	}

	/**
	 * @return the defaultStyle
	 */
	public static Style getDefaultStyle() {
		return defaultStyle;
	}

	/**
	 * @param defaultStyle
	 *            the defaultStyle to set
	 */
	public static void setDefaultStyle(Style defaultStyle) {
		GeoNames.defaultStyle = defaultStyle;
	}

	/**
	 * @return the readTimeOut
	 */
	public static int getReadTimeOut() {
		return readTimeOut;
	}

	/**
	 * @param readTimeOut
	 *            the readTimeOut to set
	 */
	public static void setReadTimeOut(int readTimeOut) {
		GeoNames.readTimeOut = readTimeOut;
	}

	/**
	 * @return the connectTimeOut
	 */
	public static int getConnectTimeOut() {
		return connectTimeOut;
	}

	/**
	 * @param connectTimeOut
	 *            the connectTimeOut to set
	 */
	public static void setConnectTimeOut(int connectTimeOut) {
		GeoNames.connectTimeOut = connectTimeOut;
	}


	/**
	* a street address
	* 
	* @author Mark Thomas
	* 
	*/
	public static class Address extends PostalCode {

		private String street;

		private String streetNumber;

		private String mtfcc;

		/**
		* @return the street
		*/
		public String getStreet() {
			return street;
		}

		/**
		* @param street
		*            the street to set
		*/
		public void setStreet(String street) {
			this.street = street;
		}

		/**
		* @return the streetNumber
		*/
		public String getStreetNumber() {
			return streetNumber;
		}

		/**
		* @param streetNumber
		*            the streetNumber to set
		*/
		public void setStreetNumber(String streetNumber) {
			this.streetNumber = streetNumber;
		}

		/**
		* @return the mtfcc
		*/
		public String getMtfcc() {
			return mtfcc;
		}

		/**
		* @param mtfcc
		*            the mtfcc to set
		*/
		public void setMtfcc(String mtfcc) {
			this.mtfcc = mtfcc;
		}

	}

	/**
	* Enumeration for the GeoNames feature classes A,H,L,P,R,S,T,U,V
	* 
	* @author marc
	* 
	*/
	public enum FeatureClass {
		/**
		* Administrative Boundary Features
		*/
		A,
		/**
		* Hydrographic Features
		*/
		H,
		/**
		* Area Features
		*/
		L,
		/**
		* Populated Place Features
		*/
		P,
		/**
		* Road / Railroad Features
		*/
		R,
		/**
		* Spot Features
		*/
		S,
		/**
		* Hypsographic Features
		*/
		T,
		/**
		* Undersea Features
		*/
		U,
		/**
		* Vegetation Features
		*/
		V;

		public static FeatureClass fromValue(String value) {
			if (value == null || "".equals(value)) {
				return null;
			}
			return valueOf(value);
		}
	}

	/**
	* @author marc
	* @since 20.01.2011
	* 
	*/
	public static class GeoNamesException extends Exception {

		private static final long serialVersionUID = 746586385626445380L;
		private String message;
		private int exceptionCode;

		public GeoNamesException(int exceptionCode, String msg) {
			super(msg);
			this.message = msg;
			this.exceptionCode = exceptionCode;
		}

		public GeoNamesException(String msg) {
			super(msg);
			this.message=msg;
		}

		/**
		* @return the message
		*/
		public String getMessage() {
			return message;
		}

		/**
		* @return the exceptionCode
		*/
		public int getExceptionCode() {
			return exceptionCode;
		}

	}

	/**
	* Is thrown when trying to access a field that has not been set as the style
	* for the request was not sufficiently verbose to return this information.
	* 
	* @author marc
	* 
	*/
	public static class InsufficientStyleException extends GeoNamesException {
		public InsufficientStyleException(String msg) {
			super(msg);
		}
	}

	/**
	* an intersection between two streets
	* 
	* @author Mark Thomas
	* 
	*/
	public static class Intersection {
		private String street2;

		private Address address = new Address();

		public double getDistance() {
			return address.getDistance();
		}

		public void setDistance(double d) {
			address.setDistance(d);
		}

		public String getAdminCode1() {
			return address.getAdminCode1();
		}

		public void setAdminCode1(String s) {
			address.setAdminCode1(s);
		}

		public String getAdminName1() {
			return address.getAdminName1();
		}

		public void setAdminName1(String s) {
			address.setAdminName1(s);
		}

		public String getAdminName2() {
			return address.getAdminName2();
		}

		public void setAdminName2(String s) {
			address.setAdminName2(s);
		}

		public String getCountryCode() {
			return address.getCountryCode();
		}

		public void setCountryCode(String s) {
			address.setCountryCode(s);
		}

		public double getLatitude() {
			return address.getLatitude();
		}

		public void setLatitude(double d) {
			address.setLatitude(d);
		}

		public double getLongitude() {
			return address.getLongitude();
		}

		public void setLongitude(double d) {
			address.setLongitude(d);
		}

		public String getPlaceName() {
			return address.getPlaceName();
		}

		public void setPlaceName(String s) {
			address.setPlaceName(s);
		}

		public String getPostalCode() {
			return address.getPostalCode();
		}

		public void setPostalCode(String s) {
			address.setPostalCode(s);
		}

		public String getStreet1() {
			return address.getStreet();
		}

		public void setStreet1(String s) {
			address.setStreet(s);
		}

		public String getStreet2() {
			return street2;
		}

		public void setStreet2(String street2) {
			this.street2 = street2;
		}

	}

	/**
	* is thrown when the search criteria is initialized with obviously invalid parameters,
	* such as an invalid country code.
	* 
	* @author marc
	* 
	*/
	public static class InvalidParameterException extends GeoNamesException {

		public InvalidParameterException(String msg) {
			super(msg);
		}

	}

	/**
	* a postal code
	* 
	* @author marc@geonames
	* 
	*/
	public static class PostalCode {

		private String postalCode;

		private String placeName;

		private String countryCode;

		private double latitude;

		private double longitude;

		private String adminName1;

		private String adminCode1;

		private String adminName2;

		private String adminCode2;

		private double distance;

		/**
		* @return Returns the distance.
		*/
		public double getDistance() {
			return distance;
		}

		/**
		* @param distance
		*            The distance to set.
		*/
		public void setDistance(double distance) {
			this.distance = distance;
		}

		/**
		* @return Returns the adminCode1.
		*/
		public String getAdminCode1() {
			return adminCode1;
		}

		/**
		* @param adminCode1
		*            The adminCode1 to set.
		*/
		public void setAdminCode1(String adminCode1) {
			this.adminCode1 = adminCode1;
		}

		/**
		* @return Returns the adminCode2.
		*/
		public String getAdminCode2() {
			return adminCode2;
		}

		/**
		* @param adminCode2
		*            The adminCode2 to set.
		*/
		public void setAdminCode2(String adminCode2) {
			this.adminCode2 = adminCode2;
		}

		/**
		* @return Returns the adminName1.
		*/
		public String getAdminName1() {
			return adminName1;
		}

		/**
		* @param adminName1
		*            The adminName1 to set.
		*/
		public void setAdminName1(String adminName1) {
			this.adminName1 = adminName1;
		}

		/**
		* @return Returns the adminName2.
		*/
		public String getAdminName2() {
			return adminName2;
		}

		/**
		* @param adminName2
		*            The adminName2 to set.
		*/
		public void setAdminName2(String adminName2) {
			this.adminName2 = adminName2;
		}

		/**
		* @return Returns the ISO 3166-1-alpha-2 countryCode.
		*/
		public String getCountryCode() {
			return countryCode;
		}

		/**
		* @param countryCode
		*            The ISO 3166-1-alpha-2 countryCode to set.
		*/
		public void setCountryCode(String countryCode) {
			this.countryCode = countryCode;
		}

		/**
		* latitude in WGS84
		* 
		* @return Returns the latitude.
		*/
		public double getLatitude() {
			return latitude;
		}

		/**
		* @param latitude
		*            The latitude to set.
		*/
		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		/**
		* longitude in WGS84
		* 
		* @return Returns the longitude.
		*/
		public double getLongitude() {
			return longitude;
		}

		/**
		* @param longitude
		*            The longitude to set.
		*/
		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		/**
		* @return Returns the placeName.
		*/
		public String getPlaceName() {
			return placeName;
		}

		/**
		* @param placeName
		*            The placeName to set.
		*/
		public void setPlaceName(String placeName) {
			this.placeName = placeName;
		}

		/**
		* @return Returns the postalCode.
		*/
		public String getPostalCode() {
			return postalCode;
		}

		/**
		* @param postalCode
		*            The postalCode to set.
		*/
		public void setPostalCode(String postalCode) {
			this.postalCode = postalCode;
		}

	}

	/**
	* search criteria for web services returning postal codes
	* 
	* @author marc@geonames
	* 
	*/
	public static class PostalCodeSearchCriteria {

		private String postalCode;

		private String placeName;

		private String adminCode1;

		private String countryCode;

		private String countryBias;

		private Double latitude;

		private Double longitude;

		private double radius;

		private Style style;

		private int maxRows;

		private int startRow;

		private boolean isOROperator = false;

		private Boolean isReduced;

		/**
		* @return Returns the style.
		*/
		public Style getStyle() {
			return style;
		}

		/**
		* @param style
		*            The style to set.
		*/
		public void setStyle(Style style) {
			this.style = style;
		}

		/**
		* @return Returns the ISO 3166-1-alpha-2 countryCode.
		*/
		public String getCountryCode() {
			return countryCode;
		}

		/**
		* @param countryCode
		*            The ISO 3166-1-alpha-2 countryCode to set.
		*/
		public void setCountryCode(String countryCode)
				throws InvalidParameterException {
			if (countryCode != null && countryCode.length() != 2) {
				throw new InvalidParameterException("invalid country code "
						+ countryCode);
			}
			this.countryCode = countryCode;
		}

		/**
		* @return Returns the latitude.
		*/
		public Double getLatitude() {
			return latitude;
		}

		/**
		* @param latitude
		*            The latitude to set.
		*/
		public void setLatitude(double latitude) throws InvalidParameterException {
			if (latitude > 90.0 || latitude < -90.0) {
				throw new InvalidParameterException("invalid latitude " + latitude);
			}
			this.latitude = new Double(latitude);
		}

		/**
		* @return Returns the longitude.
		*/
		public Double getLongitude() {
			return longitude;
		}

		/**
		* @param longitude
		*            The longitude to set.
		*/
		public void setLongitude(double longitude) throws InvalidParameterException {
			if (longitude > 180.0 || longitude < -180.0) {
				throw new InvalidParameterException("invalid longitude "
						+ longitude);
			}
			this.longitude = new Double(longitude);
		}

		/**
		* @return Returns the placeName.
		*/
		public String getPlaceName() {
			return placeName;
		}

		/**
		* @param placeName
		*            The placeName to set.
		*/
		public void setPlaceName(String placeName) {
			this.placeName = placeName;
		}

		/**
		* @return Returns the postalCode.
		*/
		public String getPostalCode() {
			return postalCode;
		}

		/**
		* @param postalCode
		*            The postalCode to set.
		*/
		public void setPostalCode(String postalCode) {
			this.postalCode = postalCode;
		}

		/**
		* @return the maxRows
		*/
		public int getMaxRows() {
			return maxRows;
		}

		/**
		* @param maxRows
		*            the maxRows to set
		*/
		public void setMaxRows(int maxRows) {
			this.maxRows = maxRows;
		}

		/**
		* @param isOROperator
		*            the isOROperator to set
		*/
		public void setOROperator(boolean isOROperator) {
			this.isOROperator = isOROperator;
		}

		/**
		* @return the isOROperator
		*/
		public boolean isOROperator() {
			return isOROperator;
		}

		/**
		* @return the adminCode1
		*/
		public String getAdminCode1() {
			return adminCode1;
		}

		/**
		* @param adminCode1
		*            the adminCode1 to set
		*/
		public void setAdminCode1(String adminCode1) {
			this.adminCode1 = adminCode1;
		}

		/**
		* the radius in km to be used for reverse geocoding.
		* 
		* @param radius
		*            the radius to set
		*/
		public void setRadius(double radius) {
			this.radius = radius;
		}

		/**
		* @return the radius
		*/
		public double getRadius() {
			return radius;
		}

		/**
		* @return the countryBias
		*/
		public String getCountryBias() {
			return countryBias;
		}

		/**
		* @param countryBias
		*            the countryBias to set
		*/
		public void setCountryBias(String countryBias) {
			this.countryBias = countryBias;
		}

		/**
		* @return the startRow
		*/
		public int getStartRow() {
			return startRow;
		}

		/**
		* @param startRow
		*            the startRow to set
		*/
		public void setStartRow(int startRow) {
			this.startRow = startRow;
		}

		/**
		* @return the isReduced
		*/
		public Boolean isReduced() {
			return isReduced;
		}

		/**
		* @param isReduced
		*            the isReduced to set
		*/
		public void setIsReduced(Boolean isReduced) {
			this.isReduced = isReduced;
		}

	}

	/**
	* @author marc
	* @since 23.07.2011
	* 
	*/
	public static class Run {

		/**
		* @param args
		* @throws Exception
		* @throws IOException
		*/
		public static void main(String[] args) throws IOException, Exception {
			GeoNames.setUserName("marc");
			WeatherObservation weatherObservation = GeoNames.findNearByWeather(
					47.4, 8.5);
			System.out.println(weatherObservation.getObservationTime());
		}

	}

	/**
	* a street line segment. Includes house number information for the beginning
	* and end of the line as well as right and left hand side of the line.
	* 
	* @author marc@geonames
	* 
	*/
	public static class StreetSegment extends PostalCode {

		private double[] latArray;

		private double[] lngArray;

		/**
		* census feature class codes see
		* http://www.geonames.org/maps/Census-Feature-Class-Codes.txt
		*/
		private String cfcc;

		private String name;

		/**
		* from address left
		*/
		private String fraddl;

		/**
		* from address right
		*/
		private String fraddr;

		/**
		* to address left
		*/
		private String toaddl;

		/**
		* to address right
		*/
		private String toaddr;

		/**
		* @return the latArray
		*/
		public double[] getLatArray() {
			return latArray;
		}

		/**
		* @param latArray
		*            the latArray to set
		*/
		public void setLatArray(double[] latArray) {
			this.latArray = latArray;
		}

		/**
		* @return the lngArray
		*/
		public double[] getLngArray() {
			return lngArray;
		}

		/**
		* @param lngArray
		*            the lngArray to set
		*/
		public void setLngArray(double[] lngArray) {
			this.lngArray = lngArray;
		}

		/**
		* @return the cfcc
		*/
		public String getCfcc() {
			return cfcc;
		}

		/**
		* @param cfcc
		*            the cfcc to set
		*/
		public void setCfcc(String cfcc) {
			this.cfcc = cfcc;
		}

		/**
		* @return the name
		*/
		public String getName() {
			return name;
		}

		/**
		* @param name
		*            the name to set
		*/
		public void setName(String name) {
			this.name = name;
		}

		/**
		* @return the fraddl
		*/
		public String getFraddl() {
			return fraddl;
		}

		/**
		* @param fraddl
		*            the fraddl to set
		*/
		public void setFraddl(String fraddl) {
			this.fraddl = fraddl;
		}

		/**
		* @return the fraddr
		*/
		public String getFraddr() {
			return fraddr;
		}

		/**
		* @param fraddr
		*            the fraddr to set
		*/
		public void setFraddr(String fraddr) {
			this.fraddr = fraddr;
		}

		/**
		* @return the toaddl
		*/
		public String getToaddl() {
			return toaddl;
		}

		/**
		* @param toaddl
		*            the toaddl to set
		*/
		public void setToaddl(String toaddl) {
			this.toaddl = toaddl;
		}

		/**
		* @return the toaddr
		*/
		public String getToaddr() {
			return toaddr;
		}

		/**
		* @param toaddr
		*            the toaddr to set
		*/
		public void setToaddr(String toaddr) {
			this.toaddr = toaddr;
		}

	}

	/**
	* Enumeration for style parameter specifying the verbosity of geonames web
	* services
	* 
	* @author marc@geonames
	* 
	*/
	public enum Style {
		SHORT, MEDIUM, LONG, FULL
	}

	/**
	* gmtOffset and dstOffset are computed on the server with the
	* {@link java.util.TimeZone} and included in the web service as not all
	* geonames users are using java.
	* 
	* @author marc
	* 
	*/
	public static class Timezone {

		private String timezoneId;
		private String countryCode;
		private Date time;
		private Date sunrise;
		private Date sunset;

		@Deprecated
		private double gmtOffset;

		@Deprecated
		private double dstOffset;

		/**
		* the dstOffset as of first of July of current year
		* 
		* @return the dstOffset
		*/
		@Deprecated
		public double getDstOffset() {
			return dstOffset;
		}

		/**
		* @param dstOffset
		*            the dstOffset to set
		*/
		public void setDstOffset(double dstOffset) {
			this.dstOffset = dstOffset;
		}

		/**
		* the gmtOffset as of first of January of current year
		* 
		* @return the gmtOffset
		*/
		@Deprecated
		public double getGmtOffset() {
			return gmtOffset;
		}

		/**
		* @param gmtOffset
		*            the gmtOffset to set
		*/
		public void setGmtOffset(double gmtOffset) {
			this.gmtOffset = gmtOffset;
		}

		/**
		* the timezoneId (example : "Pacific/Honolulu")
		* 
		* see also {@link java.util.TimeZone} and
		* http://www.twinsun.com/tz/tz-link.htm
		* 
		* @return the timezoneId
		*/
		public String getTimezoneId() {
			return timezoneId;
		}

		/**
		* @param timezoneId
		*            the timezoneId to set
		*/
		public void setTimezoneId(String timezoneId) {
			this.timezoneId = timezoneId;
		}

		/**
		* @return the countryCode
		*/
		public String getCountryCode() {
			return countryCode;
		}

		/**
		* @param countryCode
		*            the countryCode to set
		*/
		public void setCountryCode(String countryCode) {
			this.countryCode = countryCode;
		}

		/**
		* @return the time
		*/
		public Date getTime() {
			return time;
		}

		/**
		* @param time
		*            the time to set
		*/
		public void setTime(Date time) {
			this.time = time;
		}

		/**
		* @return the sunrise
		*/
		public Date getSunrise() {
			return sunrise;
		}

		/**
		* @param sunrise
		*            the sunrise to set
		*/
		public void setSunrise(Date sunrise) {
			this.sunrise = sunrise;
		}

		/**
		* @return the sunset
		*/
		public Date getSunset() {
			return sunset;
		}

		/**
		* @param sunset
		*            the sunset to set
		*/
		public void setSunset(Date sunset) {
			this.sunset = sunset;
		}

	}

	/**
	* a GeoNames toponym
	* 
	* @author marc@geonames
	* 
	*/
	public static class Toponym {

		private int geoNameId;

		private String name;

		private String alternateNames;

		private String continentCode;

		private String countryCode;

		private String countryName;

		private Long population;

		private Integer elevation;

		private FeatureClass featureClass;

		private String featureClassName;

		private String featureCode;

		private String featureCodeName;

		private double latitude;

		private double longitude;

		private String adminCode1;

		private String adminName1;

		private String adminCode2;

		private String adminName2;

		private String adminCode3;

		private String adminCode4;

		private Timezone timezone;

		private Style style;

		/**
		* @return the continentCode
		*/
		public String getContinentCode() {
			return continentCode;
		}

		/**
		* @param continentCode
		*            the continentCode to set
		*/
		public void setContinentCode(String continentCode) {
			this.continentCode = continentCode;
		}

		/**
		* @return Returns the ISO 3166-1-alpha-2 countryCode.
		*/
		public String getCountryCode() {
			return countryCode;
		}

		/**
		* @param countryCode
		*            The ISO 3166-1-alpha-2 countryCode to set.
		*/
		public void setCountryCode(String countryCode) {
			this.countryCode = countryCode;
		}

		/**
		* @return Returns the elevation in meter.
		*/
		public Integer getElevation() throws InsufficientStyleException {
			if (elevation == null && style != null
					&& Style.LONG.compareTo(style) > 0) {
				throw new InsufficientStyleException(
						"elevation not supported by style " + style.name());
			}
			return elevation;
		}

		/**
		* @param elevation
		*            The elevation im meter to set.
		*/
		public void setElevation(Integer elevation) {
			this.elevation = elevation;
		}

		/**
		* the feature class {@link FeatureClass}
		* 
		* @see <a href="http://www.geonames.org/export/codes.html">GeoNames Feature
		*      Codes</a>
		* @return Returns the featureClass.
		*/
		public FeatureClass getFeatureClass() {
			return featureClass;
		}

		/**
		* @param featureClass
		*            The featureClass to set.
		*/
		public void setFeatureClass(FeatureClass featureClass) {
			this.featureClass = featureClass;
		}

		/**
		* @see <a href="http://www.geonames.org/export/codes.html">GeoNames Feature
		*      Codes</a>
		* @return Returns the featureCode.
		*/
		public String getFeatureCode() {
			return featureCode;
		}

		/**
		* @param featureCode
		*            The featureCode to set.
		*/
		public void setFeatureCode(String featureCode) {
			this.featureCode = featureCode;
		}

		/**
		* latitude in decimal degrees (wgs84)
		* 
		* @return Returns the latitude.
		*/
		public double getLatitude() {
			return latitude;
		}

		/**
		* @param latitude
		*            The latitude to set.
		*/
		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		/**
		* longitude in decimal degrees (wgs84)
		* 
		* @return Returns the longitude.
		*/
		public double getLongitude() {
			return longitude;
		}

		/**
		* @param longitude
		*            The longitude to set.
		*/
		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		/**
		* @return Returns the name.
		*/
		public String getName() {
			return name;
		}

		/**
		* @param name
		*            The name to set.
		*/
		public void setName(String name) {
			this.name = name;
		}

		/**
		* @return Returns the population.
		*/
		public Long getPopulation() throws InsufficientStyleException {
			if (population == null && style != null
					&& Style.LONG.compareTo(style) > 0) {
				throw new InsufficientStyleException(
						"population not supported by style " + style.name());
			}
			return population;
		}

		/**
		* @param population
		*            The population to set.
		*/
		public void setPopulation(Long population) {
			this.population = population;
		}

		/**
		* @return Returns the geoNameId.
		*/
		public int getGeoNameId() {
			return geoNameId;
		}

		/**
		* @param geoNameId
		*            The geoNameId to set.
		*/
		public void setGeoNameId(int geonameId) {
			this.geoNameId = geonameId;
		}

		/**
		* @return Returns the featureClassName.
		*/
		public String getFeatureClassName() {
			return featureClassName;
		}

		/**
		* @param featureClassName
		*            The featureClassName to set.
		*/
		public void setFeatureClassName(String featureClassName) {
			this.featureClassName = featureClassName;
		}

		/**
		* @return Returns the featureCodeName.
		*/
		public String getFeatureCodeName() {
			return featureCodeName;
		}

		/**
		* @param featureCodeName
		*            The featureCodeName to set.
		*/
		public void setFeatureCodeName(String featureCodeName) {
			this.featureCodeName = featureCodeName;
		}

		/**
		* @return Returns the countryName.
		*/
		public String getCountryName() {
			return countryName;
		}

		/**
		* @param countryName
		*            The countryName to set.
		*/
		public void setCountryName(String countryName) {
			this.countryName = countryName;
		}

		/**
		* alternate names of this place as comma separated list
		* 
		* @return the alternateNames as comma separated list
		*/
		public String getAlternateNames() throws InsufficientStyleException {
			if (alternateNames == null && style != null
					&& Style.LONG.compareTo(style) > 0) {
				throw new InsufficientStyleException(
						"alternateNames not supported by style " + style.name());
			}
			return alternateNames;
		}

		/**
		* @param alternateNames
		*            the alternateNames to set
		*/
		public void setAlternateNames(String alternateNames) {
			this.alternateNames = alternateNames;
		}

		public String toString() {
			StringBuilder str = new StringBuilder();
			str.append("geoNameId=" + geoNameId + ",");
			str.append("name=" + name + ",");
			if (alternateNames != null) {
				str.append("alternateNames=" + alternateNames + ",");
			}
			str.append("latitude=" + latitude + ",");
			str.append("longitude=" + longitude + ",");
			str.append("countryCode=" + countryCode + ",");
			str.append("population=" + population + ",");
			str.append("elevation=" + elevation + ",");
			str.append("featureClass=" + featureClass + ",");
			str.append("featureCode=" + featureCode);
			return str.toString();
		}

		/**
		* @return the adminCode1
		*/
		public String getAdminCode1() throws InsufficientStyleException {
			if (adminCode1 == null && style != null
					&& Style.LONG.compareTo(style) > 0) {
				throw new InsufficientStyleException(
						"adminCode1 not supported by style " + style.name());
			}
			return adminCode1;
		}

		/**
		* @param adminCode1
		*            the adminCode1 to set
		*/
		public void setAdminCode1(String adminCode1) {
			this.adminCode1 = adminCode1;
		}

		/**
		* @return the adminCode2
		*/
		public String getAdminCode2() throws InsufficientStyleException {
			if (adminCode2 == null && style != null
					&& Style.LONG.compareTo(style) > 0) {
				throw new InsufficientStyleException(
						"adminCode2 not supported by style " + style.name());
			}
			return adminCode2;
		}

		/**
		* @param adminCode2
		*            the adminCode2 to set
		*/
		public void setAdminCode2(String adminCode2) {
			this.adminCode2 = adminCode2;
		}

		/**
		* @return the adminCode3
		*/
		public String getAdminCode3() throws InsufficientStyleException {
			if (adminCode3 == null && style != null
					&& Style.LONG.compareTo(style) > 0) {
				throw new InsufficientStyleException(
						"adminCode3 not supported by style " + style.name());
			}
			return adminCode3;
		}

		/**
		* @param adminCode3
		*            the adminCode3 to set
		*/
		public void setAdminCode3(String adminCode3) {
			this.adminCode3 = adminCode3;
		}

		/**
		* @return the adminCode4
		*/
		public String getAdminCode4() throws InsufficientStyleException {
			if (adminCode4 == null && style != null
					&& Style.LONG.compareTo(style) > 0) {
				throw new InsufficientStyleException(
						"adminCode4 not supported by style " + style.name());
			}
			return adminCode4;
		}

		/**
		* @param adminCode4
		*            the adminCode4 to set
		*/
		public void setAdminCode4(String adminCode4) {
			this.adminCode4 = adminCode4;
		}

		/**
		* @return the timezone
		*/
		public Timezone getTimezone() throws InsufficientStyleException {
			if (timezone == null && style != null
					&& Style.LONG.compareTo(style) > 0) {
				throw new InsufficientStyleException(
						"alternateNames not supported by style " + style.name());
			}
			return timezone;
		}

		/**
		* @param timezone
		*            the timezone to set
		*/
		public void setTimezone(Timezone timezone) {
			this.timezone = timezone;
		}

		/**
		* @return the adminName1
		*/
		public String getAdminName1() throws InsufficientStyleException {
			if (adminName1 == null && style != null
					&& Style.LONG.compareTo(style) > 0) {
				throw new InsufficientStyleException(
						"adminName1 not supported by style " + style.name());
			}
			return adminName1;
		}

		/**
		* @param adminName1
		*            the adminName1 to set
		*/
		public void setAdminName1(String adminName1) {
			this.adminName1 = adminName1;
		}

		/**
		* @return the adminName2
		*/
		public String getAdminName2() throws InsufficientStyleException {
			if (adminName2 == null && style != null
					&& Style.LONG.compareTo(style) > 0) {
				throw new InsufficientStyleException(
						"adminName2 not supported by style " + style.name());
			}
			return adminName2;
		}

		/**
		* @param adminName2
		*            the adminName2 to set
		*/
		public void setAdminName2(String adminName2) {
			this.adminName2 = adminName2;
		}

		/**
		* @return the style used when calling the web service that returned this
		*         toponym.
		*/
		public Style getStyle() {
			return style;
		}

		/**
		* @param style
		*            the style to set
		*/
		public void setStyle(Style style) {
			this.style = style;
		}

	}

	/**
	* search criteria for web services returning toponyms.
	* 
	* The string parameters do not have to be utf8 encoded. The encoding is done
	* transparently in the call to the web service.
	* 
	* The main parameter for the search over all fields is the 'q' parameter.
	* 
	* @see GeoNames#search
	* 
	* @see <a href="http://www.geonames.org/export/geonames-search.html">search
	*      webservice documentation< /a>
	* 
	* @author marc@geonames
	* 
	*/
	public static class ToponymSearchCriteria {

		private String q;

		private String countryCode;

		private String countryBias;

		private String continentCode;

		private String name;

		private String nameEquals;

		private String nameStartsWith;

		private String tag;

		private String language;

		private Style style;

		private FeatureClass featureClass;

		private String[] featureCodes;

		private String adminCode1;

		private String adminCode2;

		private String adminCode3;

		private String adminCode4;

		private int maxRows;

		private int startRow;

		/**
		* @return Returns the ISO 3166-1-alpha-2 countryCode.
		*/
		public String getCountryCode() {
			return countryCode;
		}

		/**
		* @param countryCode
		*            The ISO 3166-1-alpha-2 countryCode to set.
		*/
		public void setCountryCode(String countryCode)
				throws InvalidParameterException {
			if (countryCode != null && countryCode.length() != 2) {
				throw new InvalidParameterException("invalid country code "
						+ countryCode);
			}
			this.countryCode = countryCode;
		}

		/**
		* @return the countryBias
		*/
		public String getCountryBias() {
			return countryBias;
		}

		/**
		* @param countryBias
		*            the countryBias to set
		*/
		public void setCountryBias(String countryBias) {
			this.countryBias = countryBias;
		}

		/**
		* @return the continentCode
		*/
		public String getContinentCode() {
			return continentCode;
		}

		/**
		* @param continentCode
		*            the continentCode to set
		*/
		public void setContinentCode(String continentCode) {
			this.continentCode = continentCode;
		}

		/**
		* @return Returns the nameEquals.
		*/
		public String getNameEquals() {
			return nameEquals;
		}

		/**
		* @param nameEquals
		*            The nameEquals to set.
		*/
		public void setNameEquals(String exactName) {
			this.nameEquals = exactName;
		}

		/**
		* @return Returns the featureCodes.
		*/
		public String[] getFeatureCodes() {
			return featureCodes;
		}

		/**
		* @param featureCodes
		*            The featureCodes to set.
		*/
		public void setFeatureCodes(String[] featureCodes) {
			this.featureCodes = featureCodes;
		}

		public void setFeatureCode(String featureCode) {
			this.featureCodes = new String[] { featureCode };
		}

		/**
		* @return Returns the language.
		*/
		public String getLanguage() {
			return language;
		}

		/**
		* @param language
		*            The language to set.
		*/
		public void setLanguage(String language) {
			this.language = language;
		}

		/**
		* @return Returns the maxRows.
		*/
		public int getMaxRows() {
			return maxRows;
		}

		/**
		* @param maxRows
		*            The maxRows to set.
		*/
		public void setMaxRows(int maxRows) {
			this.maxRows = maxRows;
		}

		/**
		* @return Returns the name.
		*/
		public String getName() {
			return name;
		}

		/**
		* search over the name field only.
		* 
		* @param name
		*            The name to set.
		*/
		public void setName(String name) {
			this.name = name;
		}

		/**
		* @return Returns the q.
		*/
		public String getQ() {
			return q;
		}

		/**
		* The main search term. The search is executed over all fields (place name,
		* country name, admin names, etc)
		* 
		* @param q
		*            The q to set.
		*/
		public void setQ(String q) {
			this.q = q;
		}

		/**
		* @return Returns the startRow.
		*/
		public int getStartRow() {
			return startRow;
		}

		/**
		* @param startRow
		*            The startRow to set.
		*/
		public void setStartRow(int startRow) {
			this.startRow = startRow;
		}

		/**
		* @return Returns the style.
		*/
		public Style getStyle() {
			return style;
		}

		/**
		* @param style
		*            The style to set.
		*/
		public void setStyle(Style style) {
			this.style = style;
		}

		/**
		* @return Returns the tag.
		*/
		public String getTag() {
			return tag;
		}

		/**
		* @param tag
		*            The tag to set.
		*/
		public void setTag(String tag) {
			this.tag = tag;
		}

		/**
		* @return Returns the nameStartsWith.
		*/
		public String getNameStartsWith() {
			return nameStartsWith;
		}

		/**
		* @param nameStartsWith
		*            The nameStartsWith to set.
		*/
		public void setNameStartsWith(String nameStartsWith) {
			this.nameStartsWith = nameStartsWith;
		}

		/**
		* @return the featureClass
		*/
		public FeatureClass getFeatureClass() {
			return featureClass;
		}

		/**
		* @param featureClass
		*            the featureClass to set
		*/
		public void setFeatureClass(FeatureClass featureClass) {
			this.featureClass = featureClass;
		}

		/**
		* @return the adminCode1
		*/
		public String getAdminCode1() {
			return adminCode1;
		}

		/**
		* @param adminCode1
		*            the adminCode1 to set
		*/
		public void setAdminCode1(String adminCode1) {
			this.adminCode1 = adminCode1;
		}

		/**
		* @return the adminCode2
		*/
		public String getAdminCode2() {
			return adminCode2;
		}

		/**
		* @param adminCode2
		*            the adminCode2 to set
		*/
		public void setAdminCode2(String adminCode2) {
			this.adminCode2 = adminCode2;
		}

		/**
		* @return the adminCode3
		*/
		public String getAdminCode3() {
			return adminCode3;
		}

		/**
		* @param adminCode3
		*            the adminCode3 to set
		*/
		public void setAdminCode3(String adminCode3) {
			this.adminCode3 = adminCode3;
		}

		public String getAdminCode4() {
			return adminCode4;
		}

		public void setAdminCode4(String adminCode4) {
			this.adminCode4 = adminCode4;
		}

	}

	/**
	* a toponym search result as returned by the geonames webservice.
	* 
	* @author marc@geonames
	* 
	*/
	public static class ToponymSearchResult {

		List<Toponym> toponyms = new ArrayList<Toponym>();

		int totalResultsCount;

		Style style;

		/**
		* @return Returns the toponyms.
		*/
		public List<Toponym> getToponyms() {
			return toponyms;
		}

		/**
		* @param toponyms
		*            The toponyms to set.
		*/
		public void setToponyms(List<Toponym> toponyms) {
			this.toponyms = toponyms;
		}

		/**
		* @return Returns the totalResultsCount.
		*/
		public int getTotalResultsCount() {
			return totalResultsCount;
		}

		/**
		* @param totalResultsCount
		*            The totalResultsCount to set.
		*/
		public void setTotalResultsCount(int totalResultsCount) {
			this.totalResultsCount = totalResultsCount;
		}

		/**
		* @return the style
		*/
		public Style getStyle() {
			return style;
		}

		/**
		* @param style the style to set
		*/
		public void setStyle(Style style) {
			this.style = style;
		}

	}

	/**
	* @author marc
	* @since 25.11.2010
	* 
	*/
	public static class WeatherObservation {
		private String observation;
		private Date observationTime;
		private String stationName;
		private String icaoCode;
		private String countryCode;
		private Integer elevation;
		private double latitude;
		private double longitude;
		private double temperature;
		private double dewPoint;
		private double humidity;
		private String clouds;
		private String weatherCondition;
		private String windSpeed;

		/**
		* @return the observation
		*/
		public String getObservation() {
			return observation;
		}

		/**
		* @param observation
		*            the observation to set
		*/
		public void setObservation(String observation) {
			this.observation = observation;
		}

		/**
		* @return the observationTime
		*/
		public Date getObservationTime() {
			return observationTime;
		}

		/**
		* @param observationTime
		*            the observationTime to set
		*/
		public void setObservationTime(Date observationTime) {
			this.observationTime = observationTime;
		}

		/**
		* @return the stationName
		*/
		public String getStationName() {
			return stationName;
		}

		/**
		* @param stationName
		*            the stationName to set
		*/
		public void setStationName(String stationName) {
			this.stationName = stationName;
		}

		/**
		* @return the icaoCode
		*/
		public String getIcaoCode() {
			return icaoCode;
		}

		/**
		* @param icaoCode
		*            the icaoCode to set
		*/
		public void setIcaoCode(String icaoCode) {
			this.icaoCode = icaoCode;
		}

		/**
		* @return the countryCode
		*/
		public String getCountryCode() {
			return countryCode;
		}

		/**
		* @param countryCode
		*            the countryCode to set
		*/
		public void setCountryCode(String countryCode) {
			this.countryCode = countryCode;
		}

		/**
		* @return the elevation
		*/
		public Integer getElevation() {
			return elevation;
		}

		/**
		* @param elevation
		*            the elevation to set
		*/
		public void setElevation(Integer elevation) {
			this.elevation = elevation;
		}

		/**
		* @return the latitude
		*/
		public double getLatitude() {
			return latitude;
		}

		/**
		* @param latitude
		*            the latitude to set
		*/
		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		/**
		* @return the longitude
		*/
		public double getLongitude() {
			return longitude;
		}

		/**
		* @param longitude
		*            the longitude to set
		*/
		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		/**
		* @return the temperature
		*/
		public double getTemperature() {
			return temperature;
		}

		/**
		* @param temperature
		*            the temperature to set
		*/
		public void setTemperature(double temperature) {
			this.temperature = temperature;
		}

		/**
		* @return the dewPoint
		*/
		public double getDewPoint() {
			return dewPoint;
		}

		/**
		* @param dewPoint
		*            the dewPoint to set
		*/
		public void setDewPoint(double dewPoint) {
			this.dewPoint = dewPoint;
		}

		/**
		* @return the humidity
		*/
		public double getHumidity() {
			return humidity;
		}

		/**
		* @param humidity
		*            the humidity to set
		*/
		public void setHumidity(double humidity) {
			this.humidity = humidity;
		}

		/**
		* @return the clouds
		*/
		public String getClouds() {
			return clouds;
		}

		/**
		* @param clouds
		*            the clouds to set
		*/
		public void setClouds(String clouds) {
			this.clouds = clouds;
		}

		/**
		* @return the weatherCondition
		*/
		public String getWeatherCondition() {
			return weatherCondition;
		}

		/**
		* @param weatherCondition
		*            the weatherCondition to set
		*/
		public void setWeatherCondition(String weatherCondition) {
			this.weatherCondition = weatherCondition;
		}

		/**
		* @return the windSpeed
		*/
		public String getWindSpeed() {
			return windSpeed;
		}

		/**
		* @param windSpeed
		*            the windSpeed to set
		*/
		public void setWindSpeed(String windSpeed) {
			this.windSpeed = windSpeed;
		}

	}

	/**
	* a wikipedia article
	* 
	* @author marc@geonames
	* 
	*/
	public static class WikipediaArticle {

		private String language;

		private String title;

		private String summary;

		private String wikipediaUrl;

		private String feature;

		private int population;

		private Integer elevation;

		private double latitude;

		private double longitude;

		private String thumbnailImg;

		private int rank;

		/**
		* @return Returns the elevation.
		*/
		public Integer getElevation() {
			return elevation;
		}

		/**
		* @param elevation
		*            The elevation to set.
		*/
		public void setElevation(int elevation) {
			this.elevation = elevation;
		}

		/**
		* @return Returns the feature.
		*/
		public String getFeature() {
			return feature;
		}

		/**
		* @param feature
		*            The feature to set.
		*/
		public void setFeature(String feature) {
			this.feature = feature;
		}

		/**
		* @return Returns the language.
		*/
		public String getLanguage() {
			return language;
		}

		/**
		* @param language
		*            The language to set.
		*/
		public void setLanguage(String language) {
			this.language = language;
		}

		/**
		* @return Returns the latitude.
		*/
		public double getLatitude() {
			return latitude;
		}

		/**
		* @param latitude
		*            The latitude to set.
		*/
		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		/**
		* @return Returns the longitude.
		*/
		public double getLongitude() {
			return longitude;
		}

		/**
		* @param longitude
		*            The longitude to set.
		*/
		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		/**
		* @return Returns the population.
		*/
		public int getPopulation() {
			return population;
		}

		/**
		* @param population
		*            The population to set.
		*/
		public void setPopulation(int population) {
			this.population = population;
		}

		/**
		* @return Returns the summary.
		*/
		public String getSummary() {
			return summary;
		}

		/**
		* @param summary
		*            The summary to set.
		*/
		public void setSummary(String summary) {
			this.summary = summary;
		}

		/**
		* @return Returns the title.
		*/
		public String getTitle() {
			return title;
		}

		/**
		* @param title
		*            The title to set.
		*/
		public void setTitle(String title) {
			this.title = title;
		}

		/**
		* @return Returns the wikipediaUrl.
		*/
		public String getWikipediaUrl() {
			if (wikipediaUrl == null || GeoNames.isAndroid()) {
				String urlTitle = title.replace(' ', '_');
				try {
					urlTitle = URLEncoder.encode(urlTitle, "UTF8");
				} catch (UnsupportedEncodingException ex) {
				}
				String lang = language;
				if (GeoNames.isAndroid()) {
					lang += ".m";
				}
				return "http://" + lang + ".wikipedia.org/wiki/" + urlTitle;
			}
			return wikipediaUrl;
		}

		/**
		* @param wikipediaUrl
		*            The wikipediaUrl to set.
		*/
		public void setWikipediaUrl(String wikipediaURL) {
			this.wikipediaUrl = wikipediaURL;
		}

		/**
		* @return the thumbnailImg
		*/
		public String getThumbnailImg() {
			return thumbnailImg;
		}

		/**
		* @param thumbnailImg
		*            the thumbnailImg to set
		*/
		public void setThumbnailImg(String thumbnailImg) {
			this.thumbnailImg = thumbnailImg;
		}

		/**
		* @return the rank
		*/
		public int getRank() {
			return rank;
		}

		/**
		* @param rank
		*            the rank to set
		*/
		public void setRank(int rank) {
			this.rank = rank;
		}

		public final static Comparator<WikipediaArticle> ELEVATION_ORDER = new Comparator<WikipediaArticle>() {
			public int compare(WikipediaArticle o1, WikipediaArticle o2) {
				return Double.compare(o2.elevation, o1.elevation);
			}
		};

		public final static Comparator<WikipediaArticle> RANK_ORDER = new Comparator<WikipediaArticle>() {
			public int compare(WikipediaArticle o1, WikipediaArticle o2) {
				return Double.compare(o2.rank, o1.rank);
			}
		};
	}
}