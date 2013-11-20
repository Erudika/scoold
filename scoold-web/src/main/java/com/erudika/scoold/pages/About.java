/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.scoold.core.User.Badge;
import com.erudika.scoold.utils.AppConfig;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class About extends Base {
    
    public String title;
    
	
    public About() {
        title = lang.get("about.title");
		
		addModel("ANSWER_VOTEUP_REWARD_AUTHOR", AppConfig.ANSWER_VOTEUP_REWARD_AUTHOR);
		addModel("QUESTION_VOTEUP_REWARD_AUTHOR", AppConfig.QUESTION_VOTEUP_REWARD_AUTHOR);
		addModel("VOTEUP_REWARD_AUTHOR", AppConfig.VOTEUP_REWARD_AUTHOR);
		addModel("ANSWER_APPROVE_REWARD_AUTHOR", AppConfig.ANSWER_APPROVE_REWARD_AUTHOR);
		addModel("ANSWER_APPROVE_REWARD_VOTER", AppConfig.ANSWER_APPROVE_REWARD_VOTER);
		addModel("POST_VOTEDOWN_PENALTY_AUTHOR", AppConfig.POST_VOTEDOWN_PENALTY_AUTHOR);
		addModel("POST_VOTEDOWN_PENALTY_VOTER", AppConfig.POST_VOTEDOWN_PENALTY_VOTER);
		
		addModel("NICEPROFILE_BONUS", Badge.NICEPROFILE.getReward());
		addModel("SUPPORTER_BONUS",Badge.SUPPORTER.getReward());
		addModel("NOOB_BONUS", Badge.NOOB.getReward());
		addModel("GOODQUESTION_BONUS", Badge.GOODQUESTION.getReward());
		addModel("GOODANSWER_BONUS", Badge.GOODANSWER.getReward());
		addModel("CONNECTED_BONUS", Badge.CONNECTED.getReward());
		addModel("POLYGLOT_BONUS", Badge.POLYGLOT.getReward());
    }
} 
