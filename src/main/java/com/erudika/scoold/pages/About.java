/*
 * Copyright 2013-2017 Erudika. https://erudika.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For issues and patches go to: https://github.com/erudika
 */

package com.erudika.scoold.pages;

import com.erudika.scoold.core.Profile.Badge;
import com.erudika.scoold.utils.AppConfig;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
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
		addModel("POLYGLOT_BONUS", Badge.POLYGLOT.getReward());
    }
}
