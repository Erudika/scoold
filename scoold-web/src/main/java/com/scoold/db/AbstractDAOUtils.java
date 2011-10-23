/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db;

import com.scoold.core.ScooldObject;
import com.scoold.core.Stored;
import com.scoold.core.User;
import com.scoold.core.Votable;
import com.scoold.pages.BasePage;
import com.scoold.util.HumanTime;
import com.scoold.util.ScooldAuthModule;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormatSymbols;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.click.util.ClickUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import com.scoold.util.GeoNames;
import com.scoold.util.GeoNames.FeatureClass;
import com.scoold.util.GeoNames.Style;
import com.scoold.util.GeoNames.Toponym;
import com.scoold.util.GeoNames.ToponymSearchCriteria;
import com.scoold.util.GeoNames.ToponymSearchResult;
import org.jsoup.Jsoup;

/**
 *
 * @author alexb
 */
public abstract class AbstractDAOUtils {

	private static final Logger logger = Logger.getLogger(AbstractDAOUtils.class.getName());
	public static HumanTime humantime = new HumanTime();

	public static String MD5(String s) {
		return ClickUtils.toMD5Hash(s);
	}

	public static String formatDate(Long timestamp, String format, Locale loc) {
		return DateFormatUtils.format(timestamp, format, loc);
	}

	public static String formatDate(String format, Locale loc) {
		return DateFormatUtils.format(System.currentTimeMillis(), format, loc);
	}

	public static GenericDAO<?, ?> getDaoInstance(String classname) {
		if (StringUtils.isBlank(classname)) return null;
		Class<? extends ScooldObject> clazz = null;
		classname = StringUtils.capitalize(classname);
		classname = User.class.getPackage().getName().concat(".").concat(classname);
		try {
			clazz = (Class<? extends ScooldObject>) Class.forName(classname);
		} catch (ClassNotFoundException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		
		return (clazz == null) ? null : AbstractDAOFactory.getDefaultDAOFactory().getDAO(clazz);
	}

	public static String markdownToHtml(String markdownString, Object showdownConverter) {
		if (showdownConverter == null || StringUtils.isEmpty(markdownString)) return "";

		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine jsEngine = manager.getEngineByName("js");
		try {
			return ((Invocable) jsEngine).invokeMethod(showdownConverter, "makeHtml",
					markdownString) + "";
		} catch (Exception e) {
			logger.log(Level.SEVERE, "error while converting markdown to html", e);
			return "[could not convert input]";
		}
	}
	
	public static String stripHtml(String html){
		if(html == null) return "";
		return Jsoup.parse(html).text();
	}

	public static <T extends ScooldObject> void populate(T transObject, Map<String, String[]> paramMap) {
		if (transObject == null || paramMap.isEmpty()) return;

		HashMap<String, Object> fields = getAnnotatedFields(transObject, Stored.class);
		// populate an object with converted param values from param map.
		try {
			for (String param : paramMap.keySet()) {
				String[] values = paramMap.get(param);
				String value = values[0];
				// filter out any params that are different from the core params
				if(!fields.containsKey(param)) continue;
				if (StringUtils.containsIgnoreCase(param, "date") &&
						!StringUtils.equals(param, "update")) {
					
					//a special case of multi-value param - date
					param = StringUtils.replace(param.toLowerCase(), "date", "", 1);
					Long dateval = null;
					if (values.length == 3 && StringUtils.isNotEmpty(values[2]) &&
							StringUtils.isNotEmpty(values[1]) &&
							StringUtils.isNotEmpty(values[0])) {

						String date = values[2] + "-" + values[1] + "-" + values[0];
						//set property WITHOUT CONVERSION
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
						dateval = sdf.parse(date).getTime();
					}
					//set property WITHOUT CONVERSION
					PropertyUtils.setProperty(transObject, param, dateval);
					continue;
				} else if (StringUtils.equalsIgnoreCase(param, "contacts")) {
					//a special case of multi-value param - contacts
					String contacts = "";
					int max = (values.length > AbstractDAOFactory.MAX_CONTACT_DETAILS) ?
						AbstractDAOFactory.MAX_CONTACT_DETAILS : values.length;
					for (int i = 0; i < max; i++) {
						String contact = values[i];
						if (!StringUtils.isBlank(contact)) {
							String[] tuParts = contact.split(",");
							if (tuParts.length == 2) {
								tuParts[1] = tuParts[1].replaceAll(";", "");
								contacts += tuParts[0] + "," + tuParts[1] + ";";
							}
						}
					}
					contacts = StringUtils.removeEnd(contacts, ";");
					PropertyUtils.setProperty(transObject, param, contacts);
					continue;
				}
				//set property WITH CONVERSION
				BeanUtils.setProperty(transObject, param, value);
			}
		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
		}
	}

	public static List<Toponym> readLocationForKeyword(String q) {
		return readLocationForKeyword(q, Style.FULL);
	}

	public static List<Toponym> readLocationForKeyword(String q, Style style) {
		List<Toponym> list = new ArrayList<Toponym> ();
		ToponymSearchResult locationSearchResult = null;
		ToponymSearchCriteria searchLocation = new ToponymSearchCriteria();
		searchLocation.setMaxRows(7);
		searchLocation.setFeatureClass(FeatureClass.P);
		searchLocation.setStyle(style);		
		searchLocation.setQ(q);
		try {
			GeoNames.setUserName("erudika");
			locationSearchResult = GeoNames.search(searchLocation);
			if(locationSearchResult != null) 
				list.addAll(locationSearchResult.getToponyms());
		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		return list;
	}

	public static int round(float d) {
		return Math.round(d);
	}

	public static String stripAndTrim(String str) {
		if (StringUtils.isBlank(str)) return "";
		
		str = str.trim();
		str = str.replaceAll("\\p{S}", "");
		str = str.replaceAll("\\p{Po}", "");
		str = str.replaceAll("\\p{C}", "");

		return str;
	}

	public static String formatMessage(String msg, Object... params){
		return MessageFormat.format(msg, params);
	}

	public static String urlDecode(String s) {
		if (s == null) {
			return "";
		}
		String decoded = s;
		try {
			decoded = URLDecoder.decode(s, AbstractDAOFactory.DEFAULT_ENCODING);
		} catch (UnsupportedEncodingException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		return decoded;
	}

	public static String urlEncode(String s) {
		if (s == null) {
			return "";
		}
		String encoded = s;
		try {
			encoded = URLEncoder.encode(s, AbstractDAOFactory.DEFAULT_ENCODING);
		} catch (UnsupportedEncodingException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		return encoded;
	}

	public static String getPreparedQuery(String fromQuery, int paramCount){
		if(paramCount <= 0) return null;

		StringBuilder b = new StringBuilder(fromQuery);
		b.append("(");
		for (int i = 0; i < paramCount; i++) 	b.append("?,");
		b.deleteCharAt(b.lastIndexOf(","));
		b.append(")");

		return b.toString();
	}

	public static HashMap<String, Object> getAnnotatedFields(ScooldObject bean,
			Class<? extends Annotation> anno) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		try {
			for (Field field : bean.getClass().getDeclaredFields()) {
				if(field.isAnnotationPresent(anno)){
					map.put(field.getName(), PropertyUtils.getProperty(bean, field.getName()));
				}
			}
		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		return map;
	}

	public static String fixCSV(String s, int maxValues){
		if(StringUtils.trimToNull(s) == null) return "";
		String t = ",";
		HashSet<String> tset = new HashSet<String>();
		String[] split = s.split(",");
		int max = (maxValues == 0) ? split.length : maxValues;

		if(max >= split.length) max = split.length;
		
		for(int i = 0; i < max; i++) {
			String tag = split[i];
			tag = AbstractDAOUtils.stripAndTrim(tag);
			if(!tag.isEmpty() && !tset.contains(tag)){
				tset.add(tag);
				t = t.concat(tag).concat(",");
			}
		}
		return t;
	}

	// turn ,badtag, badtag  into ,cleantag,cleantag,
	public static String fixCSV(String s){
		return fixCSV(s, 0);
	}

	public static int getCurrentYear() {
		return Calendar.getInstance().get(Calendar.YEAR);
	}

	public static long timestamp(){
		return System.currentTimeMillis();
	}

	public static String abbreviate(String str, int max){
		return StringUtils.abbreviate(str, max);
	}

	public static String abbreviateInt(Number number, int decPlaces){
		if(number == null) return "";
		String abbrevn = number.toString();
		// 2 decimal places => 100, 3 => 1000, etc
		decPlaces = (int) Math.pow(10, decPlaces);
		// Enumerate number abbreviations
		String[] abbrev = {"K", "M", "B", "T"};
		// Go through the array backwards, so we do the largest first
		for (int i = abbrev.length - 1; i >= 0; i--) {
			// Convert array index to "1000", "1000000", etc
			int size = (int) Math.pow(10, (i + 1) * 3);
			// If the number is bigger or equal do the abbreviation
			if(size <= number.intValue()) {
				// Here, we multiply by decPlaces, round, and then divide by decPlaces.
				// This gives us nice rounding to a particular decimal place.
				number = Math.round(number.intValue()*decPlaces/size)/decPlaces;
				// Add the letter for the abbreviation
				abbrevn = number + abbrev[i];
				// We are done... stop
				break;
			}
		}
		return abbrevn;
	}

	public static ScooldObject getObject(Long id, String classname){
		Class<? extends ScooldObject> clazz = 
				(Class<? extends ScooldObject>) getClassname(classname);
		ScooldObject sobject = null;

		if(clazz != null){
			sobject = (ScooldObject) AbstractDAOFactory.getDefaultDAOFactory()
					.getDAO(clazz).read(id);
		}
		return sobject;
	}

	public static ScooldObject getObject(String uuid, String classname){
		Class<? extends ScooldObject> clazz = 
				(Class<? extends ScooldObject>) getClassname(classname);
		ScooldObject sobject = null;

		if(clazz != null){
			sobject = (ScooldObject) AbstractDAOFactory.getDefaultDAOFactory()
					.getDAO(clazz).read(uuid);
		}
		return sobject;
	}

	public static Class<?> getClassname(String classname){
		if(StringUtils.isBlank(classname)) return null;
		Class<?> clazz = null;
		try {
			clazz = Class.forName(
					ScooldObject.class.getPackage().getName().concat(".").concat(classname));
		} catch (Exception ex) {
			logger.severe(ex.toString());
		}

		return clazz;
	}

	public static HumanTime getHumanTime(){
		return humantime;
	}

	public static String[] getMonths(Locale locale) {
		DateFormatSymbols dfs = DateFormatSymbols.getInstance(locale);
		return dfs.getMonths();
	}
	
	public static boolean isBadString(String s) {
		if (StringUtils.isBlank(s)) {
			return false;
		} else if (s.contains("<") ||
				s.contains(">") ||
				s.contains("&") ||
				s.contains("/")) {
			return true;
		} else {
			return false;
		}
	}
	
	public static String cleanString(String s){
		if(StringUtils.isBlank(s)) return "";
		return s.replaceAll("<", "").
				replaceAll(">", "").
				replaceAll("&", "").
				replaceAll("/", "");
	}

	public static boolean isValidURL(String url){
		return getHostFromURL(url) != null;
	}

	public static String getHostFromURL(String url){
		URL u = toURL(url);
		String host = (u == null) ? null : u.getHost();
		return host;
	}

	/*
	 * Get <scheme>:<authority>
	 */
	public static String getBaseURL(String url){
		URL u = toURL(url);
		String base = null;
		if(u != null){
			try {
				base = u.toURI().getScheme().concat("://").concat(u.getAuthority());
			} catch (URISyntaxException ex) {
				base = null;
			}
		}
		return base;
	}

	private static URL toURL(String url){
		if(StringUtils.isBlank(url)) return null;
		URL u = null;
		try {
			u = new URL(url);
		} catch (MalformedURLException e) {
			// the URL is not in a valid form
			u = null;
		}
		return u;
	}

	public static boolean endsWithAny(String ext, String[] string) {
		if(ext == null || ext.trim().isEmpty() || string == null) return false;
		boolean res = false;
		for (String string1 : string) {
			if(ext.endsWith(string1)){
				res = true;
				break;
			}
		}		
		return res;
	}
	
	public static boolean containsAny(String ext, String[] string) {
		if(ext == null || ext.trim().isEmpty() || string == null) return false;
		boolean res = false;
		for (String string1 : string) {
			if(StringUtils.contains(ext, string1)){
				res = true;
				break;
			}
		}		
		return res;
	}
	
	public static void setStateParam(String name, String value, HttpServletRequest req,
			HttpServletResponse res, boolean useSessions){
		setStateParam(name, value, req, res, useSessions, false);
	}
	
	public static void setStateParam(String name, String value, HttpServletRequest req,
			HttpServletResponse res, boolean useSessions, boolean httpOnly){
		HttpSession session = useSessions ? req.getSession() : null;
		if (useSessions) {
			session.setAttribute(name, value);
		} else {
			if (httpOnly) {
				setRawCookie(name, value, req, res, httpOnly, false);
			} else {
				ClickUtils.setCookie(req, res, name, value, BasePage.SESSION_TIMEOUT_SEC, "/");
			}
		}
	}
	
	public static String getStateParam(String name, HttpServletRequest req, 
			HttpServletResponse res, boolean useSessions){
		HttpSession session = useSessions ? req.getSession() : null;
		String param = null;
		if (useSessions) {
			param = (String) session.getAttribute(name);
		}else{
			param = ClickUtils.getCookieValue(req, name);
		}
		return param;
	}
	
	public static void removeStateParam(String name, HttpServletRequest req, 
			HttpServletResponse res, boolean useSessions){
		HttpSession session = useSessions ? req.getSession() : null;
		if (useSessions) {
			session.removeAttribute(name);
		} else {
			Cookie c = ClickUtils.getCookie(req, name);
			if(c != null) ClickUtils.setCookie(req, res, name, "", 0, c.getPath());
		}
	}
	
	public static void clearSession(HttpServletRequest req, HttpServletResponse res, 
			boolean useSessions){
		req.getSession().invalidate();
		clearAuthCookie(req, res);
	}
	
	public static void clearAuthCookie(HttpServletRequest req, HttpServletResponse res){
		Cookie c = ClickUtils.getCookie(req, ScooldAuthModule.AUTH_USER);
		if(c != null){
			setRawCookie(ScooldAuthModule.AUTH_USER, "", req, res, true, true);
			removeStateParam(ScooldAuthModule.IDENTIFIER, req, res, false);
			removeStateParam(ScooldAuthModule.NEW_USER_NAME, req, res, false);
			removeStateParam(ScooldAuthModule.NEW_USER_EMAIL, req, res, false);
		}
	}
	
	public static void setRawCookie(String name, String value, HttpServletRequest req, 
			HttpServletResponse res, boolean httpOnly, boolean clear){
		long exp = System.currentTimeMillis() + (BasePage.SESSION_TIMEOUT_SEC * 1000);
		String date = clear ? "Thu, 01-Jan-1970 00:00:01" : DateFormatUtils.format(exp, 
				"EEE, dd-MMM-yyyy HH:mm:ss", TimeZone.getTimeZone("GMT"));
		String httponly = httpOnly ? "; HttpOnly" : "";
		String cookie = name+"="+value+"; Path=/; Expires="+date+" GMT"+httponly;
		res.setHeader("Set-Cookie", cookie);
	}

	public abstract boolean voteUp(Long userid, Votable<Long> votable);
	public abstract boolean voteDown(Long userid, Votable<Long> votable);
  
	public abstract <T> Long getBeanCount(Class<T> clazz);
}
