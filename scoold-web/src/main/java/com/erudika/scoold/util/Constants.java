/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.util;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public abstract class Constants {
	
	// GLOBAL LIMITS
//	public static final int MAX_PAGES = 10000;
//	public static final int VOTE_LOCKED_FOR_SEC = 4*7*24*60*60; //1 month in seconds
//	public static final long VOTE_LOCK_AFTER_SEC = 2*60; //2 minutes in ms
//	public static final int MAX_ITEMS_PER_PAGE = 30;
	
	public static final int MAX_CONTACTS_PER_USER = 2000;
	public static final int MAX_SCHOOLS_PER_USER = 100;
	public static final int MAX_CLASSES_PER_USER = 100;
	public static final int MAX_GROUPS_PER_USER = 2000;
	public static final int MAX_MEDIA_PER_ID = 2000;
	public static final int MAX_COMMENTS_PER_ID = 1000;
	public static final int MAX_TEXT_LENGTH = 20000;
	public static final int MAX_TEXT_LENGTH_SHORT = 5000;
	public static final int MAX_MESSAGES_PER_USER = 5000;
	public static final int MAX_MULTIPLE_RECIPIENTS = 50;
	public static final int MAX_TAGS_PER_POST = 5;
	public static final int MAX_REPLIES_PER_POST = 500;
	public static final int MAX_LABELS_PER_MEDIA = 5;
	public static final int MAX_CONTACT_DETAILS = 15;
	public static final int MAX_IDENTIFIERS_PER_USER = 2;
	public static final int MAX_FAV_TAGS = 50;
	public static final int MAX_INVITES = 50;

	//////////////////////////////////////////////////////////////////
	
	public static final int ANSWER_VOTEUP_REWARD_AUTHOR = 10;	//
	public static final int QUESTION_VOTEUP_REWARD_AUTHOR = 5;	//
	public static final int VOTEUP_REWARD_AUTHOR = 2;		//
	public static final int ANSWER_APPROVE_REWARD_AUTHOR = 10;	//
	public static final int ANSWER_APPROVE_REWARD_VOTER = 3;		//
	public static final int POST_VOTEDOWN_PENALTY_AUTHOR = 3;	//
	public static final int POST_VOTEDOWN_PENALTY_VOTER = 1;	//

	public static final int VOTER_IFHAS = 100; // total votes	//
	public static final int COMMENTATOR_IFHAS = 100; // coments	//
	public static final int CRITIC_IFHAS = 10; // downvotes		//
	public static final int SUPPORTER_IFHAS = 50; // upvotes	//

	public static final int GOODQUESTION_IFHAS = 20; // votes	//
	public static final int GOODANSWER_IFHAS = 10; // votes		//
	public static final int PHOTOLOVER_IFHAS = 20; // photos	//
	public static final int CONNECTED_IFHAS = 10; // contacts	//

	public static final int ENTHUSIAST_IFHAS = 100; // rep		//
	public static final int FRESHMAN_IFHAS = 300; // rep		//
	public static final int SCHOLAR_IFHAS = 500; // rep			//
	public static final int TEACHER_IFHAS = 1000; // rep		//
	public static final int PROFESSOR_IFHAS = 5000; // rep		//
	public static final int GEEK_IFHAS = 9000; // rep IT"S OVER NINE THOUSAND!
	
}
