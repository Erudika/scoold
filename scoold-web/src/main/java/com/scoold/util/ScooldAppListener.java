/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.util;

import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import name.aikesommer.authenticator.Registry;


/**
 * Web application lifecycle listener.
 * @author alexb
 */

public class ScooldAppListener implements ServletContextListener, HttpSessionListener {

	private static final Logger logger = Logger.getLogger(ScooldAppListener.class.getName());
//	private static boolean ELASTICSEARCH_ON = true;
	public static String SHOWDOWN_CONV = "showdown-converter";
	
	public void contextInitialized(ServletContextEvent sce) {
		//logger.info("initializing context.");
		ServletContext sc = sce.getServletContext();		
		
		// authentic roast
		Registry.forContext(sc).register(new com.scoold.util.ScooldAuthModule());
		
		// showdown.js - markdown 
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine jsEngine = manager.getEngineByName("js");
		Object showdownConverter = null;
		try{
			jsEngine.eval(new InputStreamReader(sc.getResourceAsStream("/scripts/showdown.js")));
			showdownConverter = jsEngine.eval("new Showdown.converter()");
		}catch (Exception e){
			logger.log(Level.SEVERE, "could not create showdown converter", e);
		}
		sc.setAttribute(SHOWDOWN_CONV, showdownConverter);
	}

	public void contextDestroyed(ServletContextEvent sce) { }
	public void sessionCreated(HttpSessionEvent se) { }
	public void sessionDestroyed(HttpSessionEvent se) { }
}