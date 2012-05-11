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
		addModel("ANSWER_APPROVE_REWARD_AUTHOR", User.ANSWER_APPROVE_REWARD_AUTHOR);
		addModel("ANSWER_APPROVE_REWARD_VOTER", User.ANSWER_APPROVE_REWARD_VOTER);
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
} 
