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
package com.erudika.scoold.utils;

import com.erudika.para.client.ParaClient;
import com.erudika.para.email.Emailer;
import com.erudika.para.utils.Config;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public final class AppConfig {

	private static final ParaClient pc;
	private static final Emailer emailer;

	private AppConfig() {}

	static {
		pc = new ParaClient(Config.getConfigParam("access_key", "x"), Config.getConfigParam("secret_key", "x"));
		pc.setEndpoint(Config.getConfigParam("endpoint", null));
		emailer = new ScooldEmailer();
	}

	public static ParaClient client() {
		return pc;
	}

	public static Emailer emailer() {
		return emailer;
	}

	public static final int MAX_CONTACTS_PER_USER = 2000;
	public static final int MAX_COMMENTS_PER_ID = 1000;
	public static final int MAX_TEXT_LENGTH = 20000;
	public static final int MAX_TEXT_LENGTH_SHORT = 5000;
	public static final int MAX_MULTIPLE_RECIPIENTS = 50;
	public static final int MAX_TAGS_PER_POST = 5;
	public static final int MAX_REPLIES_PER_POST = 500;
	public static final int MAX_CONTACT_DETAILS = 15;
	public static final int MAX_IDENTIFIERS_PER_USER = 2;
	public static final int MAX_FAV_TAGS = 50;

	public static final int ANSWER_VOTEUP_REWARD_AUTHOR = 10;
	public static final int QUESTION_VOTEUP_REWARD_AUTHOR = 5;
	public static final int VOTEUP_REWARD_AUTHOR = 2;
	public static final int ANSWER_APPROVE_REWARD_AUTHOR = 10;
	public static final int ANSWER_APPROVE_REWARD_VOTER = 3;
	public static final int POST_VOTEDOWN_PENALTY_AUTHOR = 3;
	public static final int POST_VOTEDOWN_PENALTY_VOTER = 1;

	public static final int VOTER_IFHAS = 100;
	public static final int COMMENTATOR_IFHAS = 100;
	public static final int CRITIC_IFHAS = 10;
	public static final int SUPPORTER_IFHAS = 50;
	public static final int GOODQUESTION_IFHAS = 20;
	public static final int GOODANSWER_IFHAS = 10;
	public static final int CONNECTED_IFHAS = 10;
	public static final int ENTHUSIAST_IFHAS = 100;
	public static final int FRESHMAN_IFHAS = 300;
	public static final int SCHOLAR_IFHAS = 500;
	public static final int TEACHER_IFHAS = 1000;
	public static final int PROFESSOR_IFHAS = 5000;
	public static final int GEEK_IFHAS = 9000;

}
