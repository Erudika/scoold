package com.scoold.db.cassandra;

import com.scoold.core.Classunit;
import com.scoold.core.Comment;
import com.scoold.core.Media;
import com.scoold.core.Message;
import com.scoold.core.Post;
import com.scoold.core.Report;
import com.scoold.core.Revision;
import com.scoold.core.School;
import com.scoold.core.ScooldObject;
import com.scoold.core.Tag;
import com.scoold.core.Translation;
import com.scoold.core.User;
import com.scoold.db.AbstractDAOUtils;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.GenericDAO;


/**
 * 
 * @author alexb
 */
public class CasDAOFactory extends AbstractDAOFactory{

	public static final String CLUSTER = "scoold";
	public static final String KEYSPACE = "scoold";
	public static final int CASSANDRA_PORT = 9160;
	
	public static final CF<String> USERS = new CF<String>("Users");
	public static final CF<String> SCHOOLS = new CF<String>("Schools");
	public static final CF<String> CLASSES = new CF<String>("Classes");
	public static final CF<String> POSTS = new CF<String>("Posts");
	public static final CF<String> REVISIONS = new CF<String>("Revisions");
	public static final CF<String> COMMENTS = new CF<String>("Comments");
	public static final CF<String> MEDIA = new CF<String>("Media");
	public static final CF<String> MESSAGES = new CF<String>("Messages");
	public static final CF<String> REPORTS = new CF<String>("Reports");
	public static final CF<String> TAGS = new CF<String>("Tags");	//TAGS ARE KEYS here
	public static final CF<String> TRANSLATIONS = new CF<String>("Translations"); // -*-

	public static final CF<String> USERS_UUIDS = new CF<String>("UsersUuids");
	public static final CF<String> SCHOOLS_UUIDS = new CF<String>("SchoolsUuids");
	public static final CF<String> CLASSES_UUIDS = new CF<String>("ClassesUuids");
	public static final CF<String> POSTS_UUIDS = new CF<String>("PostsUuids");
 	public static final CF<String> REPORTS_UUIDS = new CF<String>("ReportsUuids");
	public static final CF<String> REVISIONS_UUIDS = new CF<String>("RevisionsUuids");
	public static final CF<String> COMMENTS_UUIDS = new CF<String>("CommentsUuids");
	public static final CF<String> MEDIA_UUIDS = new CF<String>("MediaUuids");
	public static final CF<String> MESSAGES_UUIDS = new CF<String>("MessagesUuids");

	public static final CF<Long> REVISIONS_PARENTUUIDS = new CF<Long>("RevisionsParentUuids");
	public static final CF<Long> COMMENTS_PARENTUUIDS = new CF<Long>("CommentsParentUuids");
	public static final CF<Long> MEDIA_PARENTUUIDS = new CF<Long>("MediaParentUuids");
	public static final CF<Long> MESSAGES_PARENTUUIDS = new CF<Long>("MessagesParentUuids");
	// misc
	public static final CF<String> COUNTERS = new CF<String>("Counters");
	public static final CF<String> VOTES = new CF<String>("Votes");
	// users
	public static final CF<String> AUTH_KEYS = new CF<String>("AuthKeys");
	public static final CF<String> USER_AUTH = new CF<String>("UserAuth");
	public static final CF<String> EMAILS = new CF<String>("Emails");
	public static final CF<String> USERS_BY_REPUTATION = new CF<String>("UsersByReputation");
	public static final CF<Long> USERS_BY_TIMESTAMP = new CF<Long>("UsersByTimestamp");
	public static final CF<Long> CONTACTS = new CF<Long>("Contacts");
	public static final CF<Long> USER_SCHOOLS = new CF<Long>("UserSchools");
	public static final CF<Long> USER_CLASSES = new CF<Long>("UserClasses");
	public static final CF<Long> USER_QUESTIONS = new CF<Long>("UserQuestions");
	public static final CF<Long> USER_ANSWERS = new CF<Long>("UserAnswers");
	public static final CF<Long> NEW_MESSAGES = new CF<Long>("NewMessages");
	// media
	public static final CF<Long> PHOTOS = new CF<Long>("Photos");
	public static final CF<Long> DRAWER = new CF<Long>("Drawer");
	public static final CF<String> LABELS = new CF<String>("Labels");
	public static final CF<Long> LABELS_MEDIA = new CF<Long>("LabelsMedia");
	//schools
	public static final CF<Long> SCHOOL_CLASSES = new CF<Long>("SchoolClasses");
	public static final CF<Long> SCHOOL_USERS = new CF<Long>("SchoolUsers");
	public static final CF<Long> SCHOOLS_BY_TIMESTAMP = new CF<Long>("SchoolsByTimestamp");
	public static final CF<String> SCHOOLS_BY_VOTES = new CF<String>("SchoolsByVotes");
	// classes
	public static final CF<Long> CLASS_USERS = new CF<Long>("ClassUsers");
	public static final CF<Long> CLASSES_BY_TIMESTAMP = new CF<Long>("ClassesByTimestamp");
	// posts
	public static final CF<Long> QUESTIONS = new CF<Long>("Questions");
	public static final CF<String> QUESTIONS_BY_VOTES = new CF<String>("QuestionsByVotes");
	public static final CF<Long> QUESTIONS_BY_ACTIVITY = new CF<Long>("QuestionsByActivity");
	public static final CF<Long> ANSWERS = new CF<Long>("Answers");
	public static final CF<String> ANSWERS_BY_VOTES = new CF<String>("AnswersByVotes");
	public static final CF<Long> ALL_QUESTIONS = new CF<Long>("AllQuestions");
	public static final CF<String> ALL_QUESTIONS_BY_VOTES = new CF<String>("AllQuestionsByVotes");
	public static final CF<Long> ALL_QUESTIONS_BY_ACTIVITY = new CF<Long>("AllQuestionsByActivity");
	public static final CF<Long> FEEDBACK = new CF<Long>("Feedback");
	public static final CF<String> FEEDBACK_BY_VOTES = new CF<String>("FeedbackByVotes");
	public static final CF<Long> FEEDBACK_BY_ACTIVITY = new CF<Long>("FeedbackByActivity");
	// reports
	public static final CF<Long> REPORTS_BY_TIMESTAMP = new CF<Long>("ReportsByTimestamp");
	// language
	public static final CF<String> LOCALES_TRANSLATIONS = new CF<String>("LocalesTranslations");
	public static final CF<String> APPROVED_TRANSLATIONS = new CF<String>("ApprovedTranslations");
	public static final CF<String> LANGUAGE = new CF<String>("Language");

	// column names and keys
	public static final String CN_COUNTS_COUNT = "count";
	public static final String DEFAULT_COUNTER_KEY = "Counter1";
	public static final String CN_ID = "id";
	public static final String CN_UUID = "uuid";
	public static final String DEFAULT_KEY = "key";
	public static final String SEPARATOR = ":";


	private static final CasDAOUtils utils = new CasDAOUtils();

    public CasDAOFactory() {}
    
	public <T extends ScooldObject> GenericDAO<?, ?> getDAO(Class<T> clazz) {
		if(clazz == null) return null;
		if(clazz.equals(User.class)) return new CasUserDAO<User, Long>();
		else if(clazz.equals(Media.class)) return new CasMediaDAO<Media, Long>();
		else if(clazz.equals(Revision.class)) return new CasRevisionDAO<Revision, Long>();
		else if(clazz.equals(School.class)) return new CasSchoolDAO<School, Long>();
		else if(clazz.equals(Classunit.class)) return new CasClassUnitDAO<Classunit, Long>();
		else if(clazz.equals(Message.class)) return new CasMessageDAO<Message, Long>();
		else if(clazz.equals(Comment.class)) return new CasCommentDAO<Comment, Long>();
		else if(clazz.equals(Report.class)) return new CasReportDAO<Report, Long>();
		else if(clazz.equals(Post.class)) return new CasPostDAO<Post, Long>();
		else if(clazz.equals(Tag.class)) return new CasTagDAO<Tag, Long>();
		else if(clazz.equals(Translation.class)) return new CasTranslationDAO<Translation, Long>();
		else return null;
    }

	public AbstractDAOUtils getDAOUtils() {
		return utils;
	}
	
	public static final class CF<T>{

		public CF() {}
		
		public CF(String name) {
			this.name = name;
		}
		
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static final class SCF<T, ST>{

		public SCF() {}
		
		public SCF(String name) {
			this.name = name;
		}

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static final class Column<N, V> {

		public Column(String key, CF<N> cf, N name, V value, int ttl){
			this.key = key;
			this.cf = cf;
			this.name = name;
			this.value = value;
			this.ttl = ttl;
		}

		public Column(String key, CF<N> cf, N name, V value){
			this.key = key;
			this.cf = cf;
			this.name = name;
			this.value = value;
			this.ttl = 0;
		}

		public Column(String key, CF<N> cf) {
			this.cf = cf;
			this.key = key;
			this.name = null;
			this.value = null;
			this.ttl = 0;
		}

		private CF<N> cf;
		private String key;
		private int ttl;
		private V value;
		private N name;

		public CF<N> getCf() {
			return cf;
		}

		public void setCf(CF<N> cf) {
			this.cf = cf;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public int getTtl() {
			return ttl;
		}

		public void setTtl(int ttl) {
			this.ttl = ttl;
		}

		public V getValue() {
			return value;
		}

		public void setValue(V value) {
			this.value = value;
		}

		public N getName() {
			return name;
		}

		public void setName(N name) {
			this.name = name;
		}
	}

}

