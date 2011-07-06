/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import com.scoold.util.AmazonQueue;
import com.scoold.util.Queue;
import com.scoold.util.QueueFactory;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author alexb
 */
public class About extends BasePage {
    
    public String title;
    
	
    public About() {
        title = lang.get("about.title");
    }
	
	public void onGet(){
		logger.log(Level.WARNING, "start ");
		Queue<String> q = QueueFactory.getQueue(QueueFactory.SCOOLD_INDEX);
		
		if (param("push")) {
			String[] push = req.getParameterValues("push");
			for (String string : push) {
				q.push(string);
			}
		} else if(param("pull")) {
			addModel("msgs", q.pull());
		} else if(param("create")){
			((AmazonQueue) q).create(getParamValue("create"));
		} else if(param("delete")){
			((AmazonQueue) QueueFactory.getQueue(getParamValue("delete"))).delete();
		}else if(param("list")){
			// List queues
			addModel("queues", (List<String>) ((AmazonQueue) q).listQueues());
		}
				
		logger.log(Level.WARNING, "stop ");		
	}
	
} 
