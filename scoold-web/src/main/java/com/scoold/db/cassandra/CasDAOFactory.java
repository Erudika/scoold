package com.scoold.db.cassandra;

import com.scoold.core.*;
import com.scoold.db.AbstractDAOUtils;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.GenericDAO;


/**
 * 
 * @author alexb
 */
public final class CasDAOFactory extends AbstractDAOFactory{

	public static final String CLUSTER = "scoold";
	public static final String KEYSPACE = CLUSTER;
	public static final int CASSANDRA_PORT = 9160;
	
	public static final CF<String> OBJECTS = new CF<String>("Objects");

	// linkers
	public static final CF<Long> REVISIONS_PARENTS = new CF<Long>("RevisionsParents");
	public static final CF<Long> COMMENTS_PARENTS = new CF<Long>("CommentsParents");
	public static final CF<Long> MEDIA_PARENTS = new CF<Long>("MediaParents");
	public static final CF<Long> MESSAGES_PARENTS = new CF<Long>("MessagesParents");
	public static final CF<Long> POSTS_PARENTS = new CF<Long>("PostsParents");
	public static final CF<Long> USERS_PARENTS = new CF<Long>("UsersParents");
	public static final CF<Long> CLASSES_PARENTS = new CF<Long>("ClassesParents");
	public static final CF<Long> SCHOOLS_PARENTS = new CF<Long>("SchoolsParents");
	public static final CF<Long> GROUPS_PARENTS = new CF<Long>("GroupsParents");
	
	// misc
//	public static final CF<String> COUNTERS = new CF<String>("Counters");
	public static final CF<String> VOTES = new CF<String>("Votes");
	public static final CF<String> AUTH_KEYS = new CF<String>("AuthKeys");
	public static final CF<String> USER_AUTH = new CF<String>("UserAuth");
	
	// media
	public static final CF<String> LABELS = new CF<String>("Labels");
	public static final CF<Long> LABELS_MEDIA = new CF<Long>("LabelsMedia");

	// language
	public static final CF<String> LOCALES_TRANSLATIONS = new CF<String>("LocalesTranslations");
	public static final CF<String> APPROVED_TRANSLATIONS = new CF<String>("ApprovedTranslations");
	public static final CF<String> LANGUAGE = new CF<String>("Language");

	private static final CasDAOUtils daoutils = new CasDAOUtils(CasDAOFactory.CASSANDRA_PORT);
	private static final CasDAOFactory instance = new CasDAOFactory();

    private CasDAOFactory() {}
    
	public <T extends ScooldObject> GenericDAO<?, ?> getDAO(Class<T> clazz) {
		if(clazz == null) return null;
		if(clazz.equals(User.class)) return new CasUserDAO<User, Long>();
		else if(clazz.equals(Media.class)) return new CasMediaDAO<Media, Long>();
		else if(clazz.equals(Revision.class)) return new CasRevisionDAO<Revision, Long>();
		else if(clazz.equals(School.class)) return new CasSchoolDAO<School, Long>();
		else if(clazz.equals(Classunit.class)) return new CasClassUnitDAO<Classunit, Long>();
		else if(clazz.equals(Group.class)) return new CasGroupDAO<Group, Long>();
		else if(clazz.equals(Message.class)) return new CasMessageDAO<Message, Long>();
		else if(clazz.equals(Comment.class)) return new CasCommentDAO<Comment, Long>();
		else if(clazz.equals(Report.class)) return new CasReportDAO<Report, Long>();
		else if(clazz.equals(Post.class)) return new CasPostDAO<Post, Long>();
		else if(clazz.equals(Tag.class)) return new CasTagDAO<Tag, Long>();
		else if(clazz.equals(Translation.class)) return new CasTranslationDAO<Translation, Long>();
		else return null;
    }

	public AbstractDAOUtils getDAOUtils() {
		return daoutils;
	}
	
	public static CasDAOFactory getInstance(){
		return instance;
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

