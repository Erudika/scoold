/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.util;

import com.scoold.db.AbstractDAOFactory;
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
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.NodeBuilder;


/**
 * Web application lifecycle listener.
 * @author alexb
 */

public class ScooldAppListener implements ServletContextListener, HttpSessionListener {

	private static final Logger logger = Logger.getLogger(ScooldAppListener.class.getName());
    public static Client searchClient;
	public static Object showdownJS;
			
	public void contextInitialized(ServletContextEvent sce) {
		ServletContext sc = sce.getServletContext();		
		// authentic roast
		Registry.forContext(sc).register(new com.scoold.util.ScooldAuthModule());
		
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine jsEngine = manager.getEngineByName("js");
		try{
			jsEngine.eval(new InputStreamReader(sc.getResourceAsStream("/scripts/showdown.js"), "UTF8"));
			showdownJS = jsEngine.eval("new Showdown.converter()");
		}catch (Exception e){
			logger.log(Level.SEVERE, "could not create showdown converter", e);
		}
		NodeBuilder nb = NodeBuilder.nodeBuilder();
		nb.clusterName(AbstractDAOFactory.INDEX_NAME);
//			if(inprod){
//				nb.settings().put("cloud.aws.region", "eu-west-1");
//				nb.settings().put("cloud.aws.access_key", AmazonQueue.ACCESSKEY);
//				nb.settings().put("cloud.aws.secret_key", AmazonQueue.SECRETKEY);
//				nb.settings().put("client.transport.sniff", false);
//				nb.settings().put("network.tcp.keep_alive", true);
//				nb.settings().put("discovery.type", "ec2");
//				nb.settings().put("discovery.ec2.groups", "elasticsearch");
//				nb.settings().put("discovery.ec2.availability_zones", "eu-west-1a");
//			}
		searchClient = new TransportClient(nb.settings());
		if(AbstractDAOFactory.IN_PRODUCTION){
			String[] eshosts = System.getProperty("com.scoold.eshosts", "localhost").split(",");
			for (String host : eshosts) {
				((TransportClient) searchClient).addTransportAddress(
						new InetSocketTransportAddress(host, 9300));				
			}
		}else{
			((TransportClient) searchClient).addTransportAddress(
						new InetSocketTransportAddress("localhost", 9300));				
		}
	}

	public void contextDestroyed(ServletContextEvent sce) {
		if(searchClient != null){
			searchClient.close();
		}
	}
	
	public void sessionCreated(HttpSessionEvent se) { }
	public void sessionDestroyed(HttpSessionEvent se) { }
}