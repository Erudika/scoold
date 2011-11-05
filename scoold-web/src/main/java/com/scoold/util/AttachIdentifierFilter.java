/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scoold.util;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author alexb
 */
public class AttachIdentifierFilter implements Filter {
	
	private static final boolean debug = false;
	// The filter configuration object we are associated with.  If
	// this value is null, this filter instance is not currently
	// configured. 
	private FilterConfig filterConfig = null;
	
	public AttachIdentifierFilter() {
	}	
	
	
	/**
	 *
	 * @param request The servlet request we are processing
	 * @param response The servlet response we are creating
	 * @param chain The filter chain we are processing
	 *
	 * @exception IOException if an input/output error occurs
	 * @exception ServletException if a servlet error occurs
	 */
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		
		if (debug) {
			log("AddIdentifierFilter:doFilter()");
		}
		
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;
		
		if (request.getMethod().equals("POST")) {
			ScooldAuthModule.authAction(null, null, request, response);
		}	
		
		chain.doFilter(request, response);
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
				log("AttachIdentifier:Initializing filter");
			}
		}
	}
	
	
	public void log(String msg) {
		filterConfig.getServletContext().log(msg);		
	}
}
