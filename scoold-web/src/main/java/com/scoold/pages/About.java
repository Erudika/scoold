/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import com.scoold.core.User;
import com.scoold.core.User.Badge;

/**
 *
 * @author alexb
 */
public class About extends BasePage {
    
    public String title;
    
	
    public About() {
        title = lang.get("about.title");
		
		addModel("ANSWER_VOTEUP_REWARD_AUTHOR", User.ANSWER_VOTEUP_REWARD_AUTHOR);
		addModel("QUESTION_VOTEUP_REWARD_AUTHOR", User.QUESTION_VOTEUP_REWARD_AUTHOR);
		addModel("VOTEUP_REWARD_AUTHOR", User.VOTEUP_REWARD_AUTHOR);
		addModel("ANSWER_ACCEPT_REWARD_AUTHOR", User.ANSWER_ACCEPT_REWARD_AUTHOR);
		addModel("ANSWER_ACCEPT_REWARD_VOTER", User.ANSWER_ACCEPT_REWARD_VOTER);
		addModel("POST_VOTEDOWN_PENALTY_AUTHOR", User.POST_VOTEDOWN_PENALTY_AUTHOR);
		addModel("POST_VOTEDOWN_PENALTY_VOTER", User.POST_VOTEDOWN_PENALTY_VOTER);
		
		addModel("NICEPROFILE_BONUS", Badge.NICEPROFILE.getReward());
		addModel("SUPPORTER_BONUS",Badge.SUPPORTER.getReward());
		addModel("NOOB_BONUS", Badge.NOOB.getReward());
		addModel("GOODQUESTION_BONUS", Badge.GOODQUESTION.getReward());
		addModel("GOODANSWER_BONUS", Badge.GOODANSWER.getReward());
		addModel("CONNECTED_BONUS", Badge.CONNECTED.getReward());
		addModel("POLYGLOT_BONUS", Badge.POLYGLOT.getReward());
    }
	
//	public void onGet(){
//		logger.log(Level.WARNING, "start ");
//		Queue<String> q = QueueFactory.getQueue(QueueFactory.SCOOLD_INDEX);
//		
//		if (param("push")) {
//			String[] push = req.getParameterValues("push");
//			for (String string : push) {
//				q.push(string);
//			}
//		} else if(param("pull")) {
//			addModel("msgs", q.pull());
//		} else if(param("create")){
//			((AmazonQueue) q).create(getParamValue("create"));
//		} else if(param("delete")){
//			((AmazonQueue) QueueFactory.getQueue(getParamValue("delete"))).delete();
//		}else if(param("list")){
//			// List queues
//			addModel("queues", (List<String>) ((AmazonQueue) q).listQueues());
//		}
//				
//		logger.log(Level.WARNING, "stop ");		
//	}
	
} 
