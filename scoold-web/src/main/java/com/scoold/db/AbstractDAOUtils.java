/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db;

import com.scoold.core.*;
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
import com.scoold.util.HumanTime;
import com.scoold.util.ScooldAppListener;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.mutable.MutableLong;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.jsoup.Jsoup;

/**
 *
 * @author alexb
 */
public abstract class AbstractDAOUtils {

	private static final Logger logger = Logger.getLogger(AbstractDAOUtils.class.getName());
	private static HumanTime humantime = new HumanTime();
		
	private static final Map<String, Locale> COUNTRY_TO_LOCALE_MAP = new HashMap<String, Locale>();
	static {
		Locale[] locales = Locale.getAvailableLocales();
		for (Locale l : locales) {
			COUNTRY_TO_LOCALE_MAP.put(l.getCountry(), l);
		}
	}
	
	public static String MD5(String s) {
		return (s == null) ? "" : ClickUtils.toMD5Hash(s); 
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
		classname = ScooldObject.class.getPackage().getName().concat(".").concat(classname);
		try {
			clazz = (Class<? extends ScooldObject>) Class.forName(classname);
		} catch (ClassNotFoundException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		
		return (clazz == null) ? null : AbstractDAOFactory.getDefaultDAOFactory().getDAO(clazz);
	}

	public static String markdownToHtml(String markdownString) {
		if (ScooldAppListener.showdownJS == null || StringUtils.isEmpty(markdownString)) return "";

		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine jsEngine = manager.getEngineByName("js");
		try {
			return ((Invocable) jsEngine).invokeMethod(ScooldAppListener.showdownJS, "makeHtml",
					markdownString) + "";
		} catch (Exception e) {
			logger.log(Level.SEVERE, "error while converting markdown to html", e);
			return "";
		}
	}
	
	public static String stripHtml(String html){
		if(html == null) return "";
		return Jsoup.parse(html).text();
	}
	
	public static String escapeJavascript(String str){
		return StringEscapeUtils.escapeJavaScript(str);
	}

	public static <T extends ScooldObject> void populate(T transObject, Map<String, String[]> paramMap) {
		if (transObject == null || paramMap.isEmpty()) return;

		HashMap<String, Object> fields = getAnnotatedFields(transObject, Stored.class);
		// populate an object with converted param values from param map.
		try {
			for (Map.Entry<String, String[]> ks : paramMap.entrySet()) {
				String param = ks.getKey();
				String[] values = ks.getValue();
				String value = values[0];
				// filter out any params that are different from the core params
				if(fields.containsKey(param)){
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
									contacts = contacts.concat(tuParts[0]).concat(",").
											concat(tuParts[1]).concat(";");
								}
							}
						}
						contacts = StringUtils.removeEnd(contacts, ";");
						PropertyUtils.setProperty(transObject, param, contacts);
					}else{
						//set property WITH CONVERSION
						BeanUtils.setProperty(transObject, param, value);
					}
				}
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
		
		str = str.replaceAll("\\p{S}", "");
		str = str.replaceAll("\\p{P}", "");
		str = str.replaceAll("\\p{C}", "");

		return str.trim();
	}
	
	public static String spacesToDashes(String str) {
		if (StringUtils.isBlank(str)) return "";
		return stripAndTrim(str).replaceAll("\\p{Z}+","-").toLowerCase();
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
			tag = tag.replaceAll("-", " ");
			tag = AbstractDAOUtils.stripAndTrim(tag);
			tag = tag.replaceAll(" ", "-");
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
		boolean done = false;
		// Go through the array backwards, so we do the largest first
		for (int i = abbrev.length - 1; i >= 0 && !done; i--) {
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
				done = true;
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

	public static Class<?> getClassname(String classname){
		if(StringUtils.isBlank(classname)) return null;
		Class<?> clazz = null;
		try {
			clazz = Class.forName(ScooldObject.class.getPackage().getName().concat(".").
					concat(StringUtils.capitalize(classname.toLowerCase())));
		} catch (Exception ex) {
			logger.severe(ex.toString());
		}

		return clazz;
	}
	
	public static Class<? extends ScooldObject> classtypeToClass(String classtype){
		Class<? extends ScooldObject> clazz;
		if (Post.PostType.contains(classtype)) {
			clazz = Post.class;
		} else {
			clazz = (Class<? extends ScooldObject>) getClassname(classtype);
		}
		return clazz;
	}
	
	public boolean typesMatch(ScooldObject so){
		return (so == null) ? false : so.getClass().equals(classtypeToClass(so.getClasstype()));
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
		if(StringUtils.isBlank(ext) || string == null) return false;
		boolean res = false;
		for (String string1 : string) {
			if(ext.endsWith(string1)){
				res = true;
			}
		}		
		return res;
	}
	
	public static boolean containsAny(String ext, String[] string) {
		if(StringUtils.isBlank(ext) || string == null) return false;
		boolean res = false;
		for (String string1 : string) {
			if(StringUtils.contains(ext, string1)){
				res = true;
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
				ClickUtils.setCookie(req, res, name, value, AbstractDAOFactory.SESSION_TIMEOUT_SEC, "/");
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
			if(c != null) ClickUtils.setCookie(req, res, name, "", 0, "/");
		}
	}
	
	public static void setRawCookie(String name, String value, HttpServletRequest req, 
			HttpServletResponse res, boolean httpOnly, boolean clear){
		long exp = System.currentTimeMillis() + (AbstractDAOFactory.SESSION_TIMEOUT_SEC * 1000);
		String date = clear ? "Thu, 01-Jan-1970 00:00:01" : DateFormatUtils.format(exp, 
				"EEE, dd-MMM-yyyy HH:mm:ss", TimeZone.getTimeZone("GMT"));
		String httponly = httpOnly ? "; HttpOnly" : "";
		String cookie = name+"="+value+"; Path=/; Expires="+date+" GMT"+httponly;
		res.setHeader("Set-Cookie", cookie);
	}
	
	public static String getSystemProperty(String name){
		return System.getProperty(name);
	}

	public static Long toLong(MutableLong page){
		return (page != null && page.longValue() > 1) ?	page.longValue() : null;
	}
	
	public static int[] getMaxImgSize(int h, int w){
		int[] size = {h, w};
		int max = AbstractDAOFactory.MAX_IMG_SIZE_PX;
		if(Math.max(h, w) > max){
			int ratio = (100 * max) / Math.max(h, w);
			if(h > w){
				size[0] = max;
				size[1] = (w * ratio) / 100;
			}else{
				size[0] = (h * ratio) / 100;
				size[1] = max;
			}
		}
		return size;
	}
	
	public static String toIndexableJSON(ScooldObject so, String type, boolean hasData){
		String json = "";
		if(so == null || StringUtils.isBlank(type)) return json;
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.createObjectNode(); // will be of type ObjectNode
		
		try {
			((ObjectNode) rootNode).put("_id", so.getId().toString());
			((ObjectNode) rootNode).put("_type", type);
			((ObjectNode) rootNode).put("_index", AbstractDAOFactory.INDEX_NAME);
			
			if(hasData){
				((ObjectNode) rootNode).putPOJO("_data", 
						AbstractDAOUtils.getAnnotatedFields(so, Stored.class));
			}
			json = mapper.writeValueAsString(rootNode);
		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		
		return json;
	}
	
	public static Locale getLocaleForCountry(String countryCode){
		return COUNTRY_TO_LOCALE_MAP.get(countryCode);
	}
	
	public static String getPostLink(Post p, boolean plural, boolean noid, 
			String questionslink, String questionlink, String feedbacklink, 
			String grouplink, String grouppostlink, String classeslink, String classlink){
		if(p == null) return "";
		String ptitle = AbstractDAOUtils.spacesToDashes(p.getTitle());
		String pid = (noid ? "" : "/"+p.getId()+"/"+ ptitle);
		if (p.isQuestion()) {
			return plural ? questionslink : questionlink + pid;
		} else if(p.isFeedback()) {
			return plural ? feedbacklink : feedbacklink + pid;
		} else if(p.isGrouppost()){
			return plural ? grouplink+"/"+p.getParentid() : grouppostlink + pid;
		} else if(p.isReply()){
			Post parentp = Post.getPostDao().read(p.getParentid());
			if(parentp != null){
				return getPostLink(parentp, plural, noid, questionslink, questionlink, 
						feedbacklink, grouplink, grouppostlink, classeslink, classlink);
			}
		}else if(p.isBlackboard()){
			return plural ? classeslink : classlink + (noid ? "" : "/" + p.getParentid());
		}
		return "";
	}
	
	public abstract boolean voteUp(Long userid, Votable<Long> votable);
	public abstract boolean voteDown(Long userid, Votable<Long> votable);
	public abstract Long getBeanCount(String classtype);
	
	public abstract void setSystemColumn(String colName, String colValue, int ttl);
	public abstract String getSystemColumn(String colName);
	public abstract Map<String, String[]> getSystemColumns();
	public abstract Long getAuthstamp(String ident);
	public abstract void setAuthstamp(String ident, Long authstamp);
  
	// search methods
	public abstract boolean isIndexable(ScooldObject so);
	public abstract void index(ScooldObject so, String type);
	public abstract void unindex(ScooldObject so, String type);
	public abstract void reindexAll();
	public abstract void createIndex();
	public abstract void deleteIndex();
	public abstract boolean existsIndex();
	public abstract <T extends ScooldObject> ArrayList<T> readAndRepair(String clazz, ArrayList<String> keys);
	public abstract <T extends ScooldObject> ArrayList<T> readAndRepair(Class<T> clazz, ArrayList<String> keys, MutableLong itemcount);
	public abstract ArrayList<String> findTerm(String type, MutableLong page, MutableLong itemcount, String field, Object term);
	public abstract ArrayList<String> findTerm(String type, MutableLong page, MutableLong itemcount, String field, Object term, String sortfield, boolean reverse, int max);
	public abstract ArrayList<String> findPrefix(String type, MutableLong page, MutableLong itemcount, String field, String prefix);
	public abstract ArrayList<String> findPrefix(String type, MutableLong page, MutableLong itemcount, String field, String prefix, String sortfield, boolean reverse, int max);
	public abstract ArrayList<String> findQuery(String type, MutableLong page, MutableLong itemcount, String query);
	public abstract ArrayList<String> findQuery(String type, MutableLong page, MutableLong itemcount, String query, String sortfield, boolean reverse, int max);
	public abstract ArrayList<String> findWildcard(String type, MutableLong page, MutableLong itemcount, String field, String wildcard);
	public abstract ArrayList<String> findWildcard(String type, MutableLong page, MutableLong itemcount, String field, String wildcard, String sortfield, boolean reverse, int max);
	public abstract ArrayList<String> findTagged(String type, MutableLong page, MutableLong itemcount, ArrayList<String> tags);
	public abstract ArrayList<String> findSimilar(String type, String filterKey, String[] fields, String liketext, int max);
	public abstract ArrayList<Tag> findTags(String keywords, int max);
	
}
