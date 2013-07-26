package com.erudika.scoold.db.dynamodb;

import com.erudika.scoold.core.School;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.core.Revision;
import com.erudika.scoold.core.Message;
import com.erudika.scoold.core.Tag;
import com.erudika.para.core.PObject;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Group;
import com.erudika.scoold.core.Classunit;
import com.erudika.scoold.core.Media;
import com.erudika.scoold.core.User;
import com.erudika.scoold.db.AbstractDAOFactory;
import com.erudika.scoold.db.GenericDAO;

/**
 * 
 * @author alexb
 */
public final class DynDAOFactory extends AbstractDAOFactory{

	public static final String CLUSTER = "scoold";
	public static final String KEYSPACE = CLUSTER;
	public static final int CASSANDRA_PORT = 9160;
	
	public static final CF<String> OBJECTS = new CF<String>("Objects");

	// linkers
	public static final CF<String> REVISIONS_PARENTS = new CF<String>("RevisionsParents");
	public static final CF<String> COMMENTS_PARENTS = new CF<String>("CommentsParents");
	public static final CF<String> MEDIA_PARENTS = new CF<String>("MediaParents");
	public static final CF<String> MESSAGES_PARENTS = new CF<String>("MessagesParents");
	public static final CF<String> POSTS_PARENTS = new CF<String>("PostsParents");
	public static final CF<String> USERS_PARENTS = new CF<String>("UsersParents");
	public static final CF<String> CLASSES_PARENTS = new CF<String>("ClassesParents");
	public static final CF<String> SCHOOLS_PARENTS = new CF<String>("SchoolsParents");
	public static final CF<String> GROUPS_PARENTS = new CF<String>("GroupsParents");
	
	// misc
//	public static final CF<String> COUNTERS = new CF<String>("Counters");
	public static final CF<String> VOTES = new CF<String>("Votes");
	public static final CF<String> AUTH_KEYS = new CF<String>("AuthKeys");
	public static final CF<String> USER_AUTH = new CF<String>("UserAuth");
	
	// media
	public static final CF<String> LABELS = new CF<String>("Labels");
	public static final CF<String> LABELS_MEDIA = new CF<String>("LabelsMedia");

	// language
	public static final CF<String> LOCALES_TRANSLATIONS = new CF<String>("LocalesTranslations");
	public static final CF<String> APPROVED_TRANSLATIONS = new CF<String>("ApprovedTranslations");
	public static final CF<String> LANGUAGE = new CF<String>("Language");

	private static final DynDAOUtils daoutils = new DynDAOUtils(DynDAOFactory.CASSANDRA_PORT);
	private static final DynDAOFactory instance = new DynDAOFactory();

    private DynDAOFactory() {}
    
	public <T extends PObject> GenericDAO<?> getDAO(Class<T> clazz) {
		if(clazz == null) return null;
		if(clazz.equals(User.class)) return new DynUserDAO<User>();
		else if(clazz.equals(Media.class)) return new DynMediaDAO<Media>();
		else if(clazz.equals(Revision.class)) return new DynRevisionDAO<Revision>();
		else if(clazz.equals(School.class)) return new DynSchoolDAO<School>();
		else if(clazz.equals(Classunit.class)) return new DynClassUnitDAO<Classunit>();
		else if(clazz.equals(Group.class)) return new DynGroupDAO<Group>();
		else if(clazz.equals(Message.class)) return new DynMessageDAO<Message>();
		else if(clazz.equals(Comment.class)) return new DynCommentDAO<Comment>();
		else if(clazz.equals(Report.class)) return new DynReportDAO<Report>();
		else if(clazz.equals(Post.class)) return new DynPostDAO<Post>();
		else if(clazz.equals(Tag.class)) return new DynTagDAO<Tag>();
		else return null;
    }
	
	public static DynDAOFactory getInstance(){
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

