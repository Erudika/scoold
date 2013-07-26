package com.erudika.scoold.db;

import com.erudika.para.core.PObject;
import com.erudika.scoold.db.cassandra.CasDAOFactory;
import com.erudika.scoold.db.dynamodb.DynDAOFactory;

public abstract class AbstractDAOFactory {

	// GLOBAL LIMITS
//	public static final int MAX_PAGES = 10000;
//	public static final int VOTE_LOCKED_FOR_SEC = 4*7*24*60*60; //1 month in seconds
//	public static final long VOTE_LOCK_AFTER_SEC = 2*60; //2 minutes in ms
//	public static final int MAX_ITEMS_PER_PAGE = 30;
	
//	public static final int MAX_CONTACTS_PER_USER = 2000;
//	public static final int MAX_SCHOOLS_PER_USER = 100;
//	public static final int MAX_CLASSES_PER_USER = 100;
//	public static final int MAX_GROUPS_PER_USER = 2000;
//	public static final int MAX_MEDIA_PER_ID = 2000;
//	public static final int MAX_COMMENTS_PER_ID = 1000;
//	public static final int MAX_TEXT_LENGTH = 20000;
//	public static final int MAX_TEXT_LENGTH_SHORT = 5000;
//	public static final int MAX_MESSAGES_PER_USER = 5000;
//	public static final int MAX_MULTIPLE_RECIPIENTS = 50;
//	public static final int MAX_TAGS_PER_POST = 5;
//	public static final int MAX_REPLIES_PER_POST = 500;
//	public static final int MAX_LABELS_PER_MEDIA = 5;
//	public static final int MAX_CONTACT_DETAILS = 15;
//	public static final int MAX_IDENTIFIERS_PER_USER = 2;
//	public static final int MAX_FAV_TAGS = 50;
//	public static final int MAX_INVITES = 50;
	
//	public static final int	DEFAULT_LIMIT = Integer.MAX_VALUE;
//	public static final String SEPARATOR = ":";
	
//	public static final int SESSION_TIMEOUT_SEC = 24 * 60 * 60;
//	public static final int MAX_IMG_SIZE_PX = 730;
	
//	public static final String INDEX_NAME = "scoold";
//	public static final String SCOOLD_INDEX = "ScooldIndex";

	// column names and keys
//	public static final String CN_COUNTS_COUNT = "count";
//	public static final String CN_AUTHSTAMP = "authstamp";
//	public static final String DEFAULT_COUNTER_KEY = "Counter1";
//	public static final String CN_ID = "id";
//	public static final String DEFAULT_KEY = "key";
//	public static final String SYSTEM_OBJECTS_KEY = "1";
//	public static final String SYSTEM_MESSAGE_KEY = "system-message";
	
	public static enum FactoryType {
//		MYSQL,
//		SIMPLEDB,
		CASSANDRA,
		DYNAMODB
	}

    public static AbstractDAOFactory getDAOFactory (FactoryType type) {
        switch(type){
            case CASSANDRA: return CasDAOFactory.getInstance(); 
            case DYNAMODB: return DynDAOFactory.getInstance(); 
            default: return getDefaultDAOFactory();
        }
    }

    public static AbstractDAOFactory getDefaultDAOFactory(){
        return getDAOFactory(FactoryType.DYNAMODB);
    }

	public abstract <T extends PObject> GenericDAO<?> getDAO(Class<T> clazz);
}

