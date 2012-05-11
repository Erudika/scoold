/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scoold.util;

import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractDAOUtils;
import com.scoold.pages.BasePage;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.apache.click.util.ClickUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author alexb
 */
public class SecurityFilter implements Filter {

	private static final boolean debug = false;

	// The filter configuration object we are associated with.  If
	// this value is null, this filter instance is not currently
	// configured.
	private FilterConfig filterConfig = null;
	//comma separated bad useragents
//	private String[] safe_extensions = {".png",".jpg",".jpeg",".css",".js",".ico",".gif", ".eot", ".woff", ".ttf", ".svg", ".htc"};
	private String[] blacklist_useragents =
		{"java","jakarta","idbot","id-search","user-agent","compatible ;","ibwww",
		 "lwp-trivial","curl","PHP/","urllib","GT::WWW","Snoopy","MFC_Tear_Sample",
		 "HTTP::Lite","PHPCrawl","URI::Fetch","Zend_Http_Client","http client",
		 "PECL::HTTP","panscient.com","IBM EVV","Bork-edition","Fetch API Request",
		 "PleaseCrawl","WEP Search","Wells Search II","Missigua Locator",
		 "ISC Systems iRc Search 2.1","Microsoft URL Control","Indy Library",
		 "ia_archiver","heritrix","larbin","Nutch,ConveraCrawler"};
	//hostnames
	private String[] blacklist_hosts = {}; 
	// ips
	private Map<String, Boolean> blacklist_ips = new HashMap<String, Boolean>();	//ip addresses
//	private boolean ratelimit;
	private int MAX_HITS = 100;
	private int MAX_PERIOD = 30;
	private static final String HITS_KEY = "SecurityFilter.hits";
	private static final String PERIOD_KEY = "SecurityFilter.period";
	private static final String TIMES_KEY = "SecurityFilter.times";

	public SecurityFilter() {
	}

	/**
	 * Init method for this filter
	 */
	public void init(FilterConfig filterConfig) {
		this.filterConfig = filterConfig;
		if (filterConfig != null) {
			if (debug) {
				log("SecurityFilter:Initializing filter");
			}
//			blacklist_ips.put("", true);
		}

		String initParameterHits = filterConfig.getInitParameter(HITS_KEY);
		if (initParameterHits != null) {
				MAX_HITS = Integer.parseInt(initParameterHits);
		}
		String initParameterPeriod = filterConfig.getInitParameter(PERIOD_KEY);
		if (initParameterPeriod != null) {
				MAX_PERIOD = Integer.parseInt(initParameterPeriod);
		}
	}

	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain)
			throws IOException, ServletException {
		
		final HttpServletRequest request = (HttpServletRequest) req;
		final HttpServletResponse response = (HttpServletResponse) resp;
//		final String address = request.getRemoteAddr();
//		final String host = request.getRemoteHost();
//		final String userAgent = request.getHeader("User-Agent");
		
//		if(!request.getRequestURI().contains("error")){
//			if(StringUtils.isBlank(userAgent) || isBlocked(host, address, userAgent)){
//				//BLOCK!
//				forbidden(response, host, address, userAgent);
//				return ;
//			}
//		}
//
//		if (!StringUtils.endsWithAny(request.getRequestURI(), safe_extensions)) {
//			if (session != null) {
//				synchronized (session.getId().intern()) {
//					LinkedBlockingDeque<Long> times = getSessionAttribute(session, TIMES_KEY);
//					if (times == null) {
//						times = new LinkedBlockingDeque<Long>();
//						session.setAttribute(TIMES_KEY, times);
//					}
//					final long currentTimeMillis = System.currentTimeMillis();
//					times.push(Long.valueOf(currentTimeMillis));
//					final long cutoff = currentTimeMillis - (MAX_PERIOD * 1000);
//					Long oldest = times.peekLast();
//					while (oldest != null && oldest.longValue() < cutoff) {
//						times.removeLast();
//						oldest = times.peekLast();
//					}
//					if (times.size() > MAX_HITS) {
//						addResponseHeaderNoCache(response);
//						response.sendError(HttpServletResponse.SC_FORBIDDEN, 
//								"Slow down, robot! Try again in "+ MAX_PERIOD + " seconds.");
//						log("ratelimit: "+host+"/"+address+" ("+userAgent+")");
//						return;
//					}
//				}
//			}
//		}

		
		// anti-CSRF token validation
		if(request.getMethod().equals("POST") && !StringUtils.isBlank(request.getRemoteUser())){
			String token = request.getParameter("stoken");
			String salt = request.getParameter("pepper");
			String authToken = AbstractDAOUtils.getStateParam(ScooldAuthModule.AUTH_USER, 
					request, response, ScooldAuthModule.USE_SESSIONS);
			
			if(!StringUtils.isBlank(token) && !StringUtils.isBlank(authToken) 
					&& !StringUtils.isBlank(salt) && token.equals(AbstractDAOUtils.
					MD5(authToken.concat(AbstractDAOFactory.SEPARATOR).concat(salt)))){
				
				// Uncomment to enable XSS Filter for all req params
				//chain.doFilter(new RequestWrapper(request), response);
				chain.doFilter(request, response);
			}else{
				badrequest(response, request.getRemoteHost(), request.getRemoteAddr(), 
						request.getHeader("User-Agent"), ClickUtils.isAjaxRequest(request));
				return ;
			}
		}else{
			// Uncomment to enable XSS Filter for all req params
			//chain.doFilter(new RequestWrapper(request), response);
			chain.doFilter(request, response);
		}
	}
	
	private void forbidden(HttpServletResponse response, String host, String address, 
			String userAgent) throws IOException{
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied!");
		log("forbidden: "+host+"/"+address+" ("+userAgent+")");
	}
	
	private void badrequest(HttpServletResponse response, String host, String address, 
			String userAgent, boolean isAjax) throws IOException{
		log("badrequest: "+host+"/"+address+" ("+userAgent+")");
		String path = filterConfig.getServletContext().getContextPath();
		if(isAjax){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad request.");
		}else{
			response.sendRedirect(path + "/" + HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	/**
	 * Return the filter configuration object for this filter.
	 */
	public FilterConfig getFilterConfig() {
		return (this.filterConfig);
	}

	/**
	 * Set the filter configuration object for this filter.
	 *
	 * @param filterConfig The filter configuration object
	 */
	public void setFilterConfig(FilterConfig filterConfig) {
		this.filterConfig = filterConfig;
	}

	/**
	 * Destroy method for this filter
	 */
	public void destroy() {
		filterConfig = null;
	}

	/**
	 * Return a String representation of this object.
	 */
	public String toString() {
		if (filterConfig == null) {
			return ("SecurityFilter()");
		}
		StringBuilder sb = new StringBuilder("SecurityFilter(");
		sb.append(filterConfig);
		sb.append(")");
		return (sb.toString());
	}

	public static String getStackTrace(Throwable t) {
		String stackTrace = null;
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			pw.close();
			sw.close();
			stackTrace = sw.getBuffer().toString();
		} catch (Exception ex) {
		}
		return stackTrace;
	}

	public void log(String msg) {
		filterConfig.getServletContext().log(msg);
	}

	private boolean isBlocked(String host, String address, String useragent) {
		return AbstractDAOUtils.containsAny(useragent, blacklist_useragents) ||
			AbstractDAOUtils.containsAny(host, blacklist_hosts) ||
			blacklist_ips.containsKey(address);
	}

	private void addResponseHeaderNoCache(final HttpServletResponse response) {
		response.addHeader("Cache-Control",	"no-store, no-cache, must-revalidate, "
				+ "max-stale=0, max-age=0, post-check=0, pre-check=0");
		response.addHeader("Pragma", "no-cache");
		response.addHeader("Expires", "0");
	}


	private final class RequestWrapper extends HttpServletRequestWrapper {

		public RequestWrapper(HttpServletRequest servletRequest) {
			super(servletRequest);
		}

		public String[] getParameterValues(String parameter) {

			String[] values = super.getParameterValues(parameter);
			if (values == null) {
				return null;
			}
			int count = values.length;
			String[] encodedValues = new String[count];
			for (int i = 0; i < count; i++) {
				encodedValues[i] = cleanXSS(values[i]);
			}
			return encodedValues;
		}

		public String getParameter(String parameter) {
			String value = super.getParameter(parameter);
			if (value == null) {
				return null;
			}
			return cleanXSS(value);
		}

		public String getHeader(String name) {
			String value = super.getHeader(name);
			if (value == null) {
				return null;
			}
			return cleanXSS(value);

		}

		private String cleanXSS(String value) {
			//You'll need to remove the spaces from the html entities below
//			value = value.replaceAll("<", "& lt;").replaceAll(">", "& gt;");
//			value = value.replaceAll("\\(", "& #40;").replaceAll("\\)", "& #41;");
//			value = value.replaceAll("'", "& #39;");
//			value = value.replaceAll("eval\\((.*)\\)", "");
//			value = value.replaceAll("[\\\"\\\'][\\s]*javascript:(.*)[\\\"\\\']", "\"\"");
//			value = value.replaceAll("script", "");
			value = StringEscapeUtils.escapeHtml(value);
			value = StringEscapeUtils.escapeJavaScript(value);
			return value;
		}
	}

}
