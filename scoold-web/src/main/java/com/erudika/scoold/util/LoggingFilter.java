/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author alexb
 */
public class LoggingFilter implements Filter {

	private static final boolean debug = false;

	// The filter configuration object we are associated with.  If
	// this value is null, this filter instance is not currently
	// configured. 
	private FilterConfig filterConfig = null;
	private Level level; //default 
	
	public LoggingFilter() {		
	}
	
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain)
			throws IOException, ServletException {

		if (debug) {
			log("LoggingFilter:doFilter()");
		}

		HttpServletRequest request = (HttpServletRequest) req;
//		HttpServletResponse response = (HttpServletResponse) resp;
		
		if(request.getRemoteUser() != null){
			String user = request.getRemoteUser();
			String address = request.getRemoteAddr();
			String method = request.getMethod();
			String query = request.getRequestURI()+"?"+request.getQueryString();
			String userAgent = request.getHeader("User-Agent");

			if(level == Level.FINE || level == Level.FINER || level == Level.FINEST){
				log("User:"+user+"@"+address+","+method+"->"+query+", ("+userAgent+")");
			}else{
				log("User:"+user+"@"+address+","+method+" "+query);
			}		
		}
							
		chain.doFilter(req, resp);
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
	}

	/**
	 * Init method for this filter 
	 */
	public void init(FilterConfig filterConfig) {
		this.filterConfig = filterConfig;
		if (filterConfig != null) {
			if (debug) {
				log("LoggingFilter: Initializing filter");
			}
			try {
				level = Level.parse(filterConfig.getInitParameter("level").toUpperCase());
			} catch (IllegalArgumentException e) {
				Logger.getLogger(LoggingFilter.class.getName()).severe(e.toString());
				level = Level.INFO;
			}
		}		
	}

	/**
	 * Return a String representation of this object.
	 */
	@Override
	public String toString() {
		if (filterConfig == null) {
			return ("LoggingFilter()");
		}
		StringBuffer sb = new StringBuffer("LoggingFilter(");
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
		//filterConfig.getServletContext().log(msg);
		Logger.getLogger(LoggingFilter.class.getName()).log(level, msg);
	}
}
