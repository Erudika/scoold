/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scoold.util;

import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractDAOUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.click.util.ClickUtils;
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
	private String[] blacklist_useragents =
		{"java","jakarta","idbot","id-search","user-agent","compatible ;","ibwww",
		 "lwp-trivial","curl","PHP/","urllib","GT::WWW","Snoopy","MFC_Tear_Sample",
		 "HTTP::Lite","PHPCrawl","URI::Fetch","Zend_Http_Client","http client",
		 "PECL::HTTP","panscient.com","IBM EVV","Bork-edition","Fetch API Request",
		 "PleaseCrawl","WEP Search","Wells Search II","Missigua Locator",
		 "ISC Systems iRc Search 2.1","Microsoft URL Control","Indy Library",
		 "ia_archiver","heritrix","larbin","Nutch,ConveraCrawler"};

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
		}
	}

	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain)
			throws IOException, ServletException {
		
		final HttpServletRequest request = (HttpServletRequest) req;
		final HttpServletResponse response = (HttpServletResponse) resp;
		
		// anti-CSRF token validation
		if(request.getMethod().equals("POST") && !StringUtils.isBlank(request.getRemoteUser())){
			String token = request.getParameter("stoken");
			String salt = request.getParameter("pepper");
			String authToken = AbstractDAOUtils.getStateParam(ScooldAuthModule.AUTH_USER, 
					request, response, ScooldAuthModule.USE_SESSIONS);
			
			if(!StringUtils.isBlank(token) && !StringUtils.isBlank(authToken) 
					&& !StringUtils.isBlank(salt) && token.equals(AbstractDAOUtils.
					MD5(authToken.concat(AbstractDAOFactory.SEPARATOR).concat(salt)))){
				
				chain.doFilter(request, response);
			}else{
				badrequest(response, request.getRemoteHost(), request.getRemoteAddr(), 
						request.getHeader("User-Agent"), ClickUtils.isAjaxRequest(request));
			}
		}else{
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
}
