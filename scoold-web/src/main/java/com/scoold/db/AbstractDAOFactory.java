package com.scoold.db;

import com.scoold.core.ScooldObject;
import com.scoold.db.cassandra.CasDAOFactory;
import org.apache.commons.lang.BooleanUtils;

public abstract class AbstractDAOFactory {

	public static final String DEFAULT_ENCODING = "UTF-8";
	// GLOBAL LIMITS
	public static final int MAX_COLUMNS = 1024;
	public static final int VOTE_LOCKED_FOR_SEC = 4*7*24*60*60; //1 month in seconds
	public static final long VOTE_LOCK_AFTER_SEC = 2*60; //2 minutes in ms
	public static final int MAX_ITEMS_PER_PAGE = 30;
	public static final int MAX_CONTACTS_PER_USER = 2000;
	public static final int MAX_SCHOOLS_PER_USER = 50;
	public static final int MAX_CLASSES_PER_USER = 100;
	public static final int MAX_GROUPS_PER_USER = 2000;
	public static final int MAX_MEDIA_PER_ID = 2000;
	public static final int MAX_COMMENTS_PER_ID = MAX_COLUMNS;
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
	public static final int	DEFAULT_LIMIT = Integer.MAX_VALUE;
	public static final String SEPARATOR = ":";
	
	public static final int SESSION_TIMEOUT_SEC = 24 * 60 * 60;
	public static final int MAX_IMG_SIZE_PX = 730;
	
	public static final String INDEX_NAME = "scoold";
	public static final String SCOOLD_INDEX = "ScooldIndex";

	// column names and keys
	public static final String CN_COUNTS_COUNT = "count";
	public static final String DEFAULT_COUNTER_KEY = "Counter1";
	public static final String CN_ID = "id";
	public static final String DEFAULT_KEY = "key";
	public static final String SYSTEM_OBJECTS_KEY = "1";
	public static final String SYSTEM_MESSAGE_KEY = "system-message";
	
	public static final boolean IN_PRODUCTION = 
			BooleanUtils.toBoolean(System.getProperty("com.scoold.production"));

	public static enum FactoryType {
//		MYSQL,
//		SIMPLEDB,
		CASSANDRA
	}

    public static AbstractDAOFactory getDAOFactory (FactoryType type) {
        switch(type){
            case CASSANDRA: return CasDAOFactory.getInstance(); 
            default: return getDefaultDAOFactory();
        }
    }

    public static AbstractDAOFactory getDefaultDAOFactory(){
        return getDAOFactory(FactoryType.CASSANDRA);
    }

	public abstract AbstractDAOUtils getDAOUtils();

	public abstract <T extends ScooldObject> GenericDAO<?, ?> getDAO(Class<T> clazz);
}

