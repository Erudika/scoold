/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.util;

import com.scoold.core.Search;
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
import org.elasticsearch.client.Client;
import org.elasticsearch.node.NodeBuilder;


/**
 * Web application lifecycle listener.
 * @author alexb
 */

public class ScooldAppListener implements ServletContextListener, HttpSessionListener {

	private static final Logger logger = Logger.getLogger(ScooldAppListener.class.getName());
	private static boolean ELASTICSEARCH_ON = true;
	public static String SEARCH_CLIENT = "search-client";
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
		sc.setAttribute("showdownConverter", showdownConverter);
		 
		// elasticsearch init   
		if(ELASTICSEARCH_ON){
			NodeBuilder nb = NodeBuilder.nodeBuilder();
			nb.loadConfigSettings(true);
			nb.settings().put("node.river", "_none_"); 
			nb.settings().put("client.transport.sniff", true);
			
//			nb.settings().put("network.host", "localhost"); 
//			nb.settings().put("network.host", "_local_"); 
//			nb.settings().put("network.tcp.keep_alive", true);
//			
//			nb.settings().put("gateway.type", "s3");
//			nb.settings().put("gateway.s3.bucket", "com.scoold.elasticsearch");
//			
//			nb.settings().put("discovery.type", "ec2");
//			nb.settings().put("discovery.ec2.groups", "elasticsearch");
//			
//			nb.settings().put("cloud.aws.region", "eu-west-1");
//			nb.settings().put("cloud.aws.access_key", "AKIAI5WX2PJPYQEPWECQ");
//			nb.settings().put("cloud.aws.secret_key", "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");
//			nb.settings().put("cloud.aws.sqs.queue_url", "https://queue.amazonaws.com/374874639893/ScooldIndex");
			
			nb.clusterName(Search.INDEX_NAME);
			nb.client(true);
			nb.data(false);
			
			sc.setAttribute(SEARCH_CLIENT, nb.node().client());
		}
	}

	public void contextDestroyed(ServletContextEvent sce) {
		//logger.info("context is destroyed."); 
		Client searchClient = (Client) sce.getServletContext().getAttribute(SEARCH_CLIENT);
		if(ELASTICSEARCH_ON && searchClient != null){
			searchClient.close();
		}
	}

	public void sessionCreated(HttpSessionEvent se) {

//		File file = new File("/Users/alexb/Desktop/schools.txt");
//		int i = 1;
//		try {
//			List<String> lines = FileUtils.readLines(file, "UTF-8");
//			List<String> test = new ArrayList();
//			HashMap<String, String> cities = new HashMap();
//
//			School s = new School();
//			for (String line : lines) {
//				line = line.trim();
//				switch (i){
//					case 1:	s.setName(line); break;
//					case 2: s.setTypeString(line); break;
//					case 3: s.setLocation(line);
//
//					if(cities.containsKey(line)){
//						s.setLocation(cities.get(line));
//					}else{
//						List t = DAOUtils.readLocationForKeyword(line, Style.SHORT);
//						if(t.isEmpty()){
//							s.setLocation(line);
//							cities.put(line, line);
//						}else{
//							Toponym top = (Toponym) t.get(0);
//							String newloc = top.getName()+", Bulgaria";
//							cities.put(line, newloc);
//							test.add(s.getName()+" "+line+"="+newloc);
//							Thread.sleep(1000);
//							s.setLocation(newloc);
//						}
//
//					}
//
//					break;
//					case 4: s.setAddress(line); break;
//				}
//				i++;
//				if(i == 5){
//					i = 1;
//					logger.info("adding "+s.getLocation());
//					//schools.add(s);
//					s.create();
//					s = new School();
//				}
//			}
//			se.getSession().setAttribute("testis", test);
//		} catch (IOException ex) {
//			logger.log(Level.SEVERE, null, ex);
//		}
//		catch (InterruptedException e) {
//			logger.log(Level.SEVERE, null, e);
//		}

		// Set the time the user was last seen
//		HttpSession seshun = se.getSession();
//
//		SimplePrincipal userPricipal = SimplePrincipal.getPrincipal(seshun);
//		User authUser = null;
//		if (userPricipal != null) {
//			authUser = userPricipal.getUser();
//			if(authUser != null){
//				//update lastseen
//				authUser.setLastSeen(new Timestamp(System.currentTimeMillis()));
//				authUser.update();
//			}
//		}
		
	}

	public void sessionDestroyed(HttpSessionEvent se) {
	}
}