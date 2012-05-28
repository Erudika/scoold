package com.scoold.core;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHandler.STATE;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.Response;
import com.scoold.core.Media.MediaType;
import com.scoold.core.Post.PostType;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractDAOUtils;
import com.scoold.db.AbstractSchoolDAO;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.mutable.MutableLong;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * 
 * @author alexb
 */
public class School implements Votable<Long>, CanHasMedia, Askable, ScooldObject, Serializable{

    private Long id;
    @Stored private String name;
    @Stored private String location;
    @Stored private String type;
    @Stored private Integer fromyear;
    @Stored private Integer toyear;
	@Stored private String about;
	@Stored private Long userid;
	@Stored private Integer votes;
	@Stored private String contacts;
	@Stored private Long timestamp;
	@Stored private String iconurl;
	@Stored public static String classtype = School.class.getSimpleName().toLowerCase();

	public static enum SchoolType{
		UNKNOWN, HIGHSCHOOL, LYCEUM, COLLEGE, THEOLOGY, SEMINARY, ACADEMY, SPECIALIZED,
		PRIVATE, PRIMARY, SECONDARY, UNIVERSITY, ELEMENTARY, GYMNASIUM, MIDDLE,
		ARTS, SPORTS;

		public String toString(){
			return super.toString().toLowerCase();
		}
	};

    private transient static AbstractSchoolDAO<School, Long> mydao;

    public static AbstractSchoolDAO<School, Long> getSchoolDao() {
        return (mydao != null) ? mydao : (AbstractSchoolDAO<School, Long>)
				AbstractDAOFactory.getDefaultDAOFactory().getDAO(School.class);
    }

	private static final AsyncHttpClient httpClient;
	private static int redirectCount = 0;
	static {
		final AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
		builder.setCompressionEnabled(true)
			.setAllowPoolingConnection(true)
			.setRequestTimeoutInMs(5000)
			.build();

		httpClient = new AsyncHttpClient(builder.build());
	}

	public School() {
		this("", SchoolType.UNKNOWN.toString(), "");
    }

	public School(String name, String type, String location){
		this.name = name;
		this.location = location;
		this.votes = 0;
		this.about = "";
		this.type = getSchoolType(type).toString();
	}

	public School(Long id){
		this();
        this.id = id;
    }

	public String getClasstype() {
		return classtype;
	}

	public void setContacts(String contacts) {
		this.contacts = contacts;
	}
	
	/**
	 * Get the value of iconurl
	 *
	 * @return the value of iconurl
	 */
	public String getIconurl() {
		return iconurl;
	}

	/**
	 * Set the value of iconurl
	 *
	 * @param iconurl new value of iconurl
	 */
	public void setIconurl(String iconurl) {
		this.iconurl = iconurl;
	}

	/**
	 * Get the value of timestamp
	 *
	 * @return the value of timestamp
	 */
	public Long getTimestamp() {
		return timestamp;
	}

	/**
	 * Set the value of timestamp
	 *
	 * @param timestamp new value of timestamp
	 */
	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Get the value of contacts
	 *
	 * @return the value of contacts
	 */
	public String getContacts() {
		return contacts;
	}

	/**
	 * Get the value of userid
	 *
	 * @return the value of userid
	 */
	public Long getUserid() {
		return userid;
	}

	/**
	 * Set the value of userid
	 *
	 * @param userid new value of userid
	 */
	public void setUserid(Long userid) {
		this.userid = userid;
	}

	/**
     * Get the value of id
     *
     * @return the value of id
     */
    public Long getId() {
        return id;
    }

	public void setId(Long id){
		this.id = id;
	}

    /**
     * Get the value of city
     *
     * @return the value of city
     */
    public String getLocation() {
        return location;
    }

    /**
     * Set the value of city
     *
     * @param location
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Get the value of name
     *
     * @return the value of name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the value of name
     *
     * @param name new value of name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the value of type
     *
     * @return the value of type
     */
    public String getType() {
        return type;
    }

    /**
     * Set the value of type
     *
     * @param type new value of type
     */
    public void setType(String type) {
        this.type = getSchoolType(type).toString();
    }

    /**
     * Get the value of toyear
     *
     * @return the value of toyear
     */
    public Integer getToyear() {
        return toyear;
    }

    /**
     * Set the value of toyear
     *
     * @param toyear new value of toyear
     */
    public void setToyear(Integer toyear) {
        this.toyear = toyear;
    }

    /**
     * Get the value of fromyear
     *
     * @return the value of fromyear
     */
    public Integer getFromyear() {
        return fromyear;
    }

    /**
     * Set the value of fromyear
     *
     * @param fromyear new value of fromyear
     */
    public void setFromyear(Integer fromyear) {
        this.fromyear = fromyear;
    }

	/**
	 * Get the value of about
	 *
	 * @return the value of about
	 */
	public String getAbout() {
		return about;
	}

	/**
	 * Set the value of about
	 *
	 * @param about new value of about
	 */
	public void setAbout(String about) {
		this.about = about;
	}

	/**
	 * Get the value of votes
	 *
	 * @return the value of votes
	 */
	public Integer getVotes() {
		return votes;
	}

	/**
	 * Set the value of votes
	 *
	 * @param votes new value of votes
	 */
	public void setVotes(Integer votes) {
		this.votes = votes;
	}


	public ArrayList<User> getAllUsers(MutableLong page, MutableLong itemcount){
		return User.getUserDao().readAllUsersForID(this.id, page, itemcount);
	}
    
    public ArrayList<Classunit> getAllClassUnits(MutableLong page, MutableLong itemcount){
        return getSchoolDao().readAllClassUnitsForSchool(this.id, page, itemcount);
    }
        
    public boolean linkToUser(Long userid){
		return getSchoolDao().createUserSchoolLink(userid, this.id, this.fromyear, this.toyear);
    }

    public void unlinkFromUser(Long userid){
        getSchoolDao().deleteUserSchoolLink(userid, this);
    }
    
	private SchoolType getSchoolType(String type){
		if(type == null) return SchoolType.UNKNOWN;
		try{			
			return SchoolType.valueOf(type.trim().toUpperCase());
		}catch(IllegalArgumentException e){
            //oh shit!
			return SchoolType.UNKNOWN;
        }
	}
 
    public static  Map<String, String> getSchoolTypeMap(Map<String, String> lang){
        SchoolType[] starr = SchoolType.values();
        Map<String, String> st = new HashMap<String, String>();
		if(lang == null) return st;
		
        for(SchoolType s : starr){
			if(s != SchoolType.UNKNOWN){
				String locs = lang.get("school."+s.toString());
				if(locs == null) locs = s.toString();
				st.put(s.toString(), locs);
			}
        }
		//st.remove(0);	// removes "unknown" from the list
        return st;
    }
	
	public boolean isLinkedTo(User u){
		return getSchoolDao().isLinkedToUser(this.id, u.getId());
	}

	public ArrayList<ContactDetail> getAllContactDetails(){
		return ContactDetail.toContactsList(contacts);
	}

	public String getWebsite(){
		String ws = ContactDetail.ContactDetailType.WEBSITE.name();
		int wsl = ws.length() + 1;
		if(!StringUtils.isBlank(contacts) && StringUtils.contains(contacts, ws)){
			String www = contacts.substring(contacts.indexOf(ws) + wsl);
			if(StringUtils.contains(www, ContactDetail.SEPARATOR)){
				www = www.substring(0, www.indexOf(ContactDetail.SEPARATOR));
			}
			return AbstractDAOUtils.isValidURL(www) ? www : null;
		}
		return null;
	}

    public Long create(){
        this.id = getSchoolDao().create(this);
        return this.id;
    }
    
    public void update(){
		getSchoolDao().update(this);
		updateSchoolPic();
    }
    
    public void delete(){
        getSchoolDao().delete(this);
    }

	private void updateSchoolPic(){
		String www = getWebsite();
		if(!StringUtils.isBlank(www) && (StringUtils.isBlank(iconurl) ||
				!StringUtils.contains(iconurl, AbstractDAOUtils.getHostFromURL(www)))){
			final String website = www.endsWith("/") ? www : www.concat("/");
			try {
				String filename = "apple-touch-icon.png";
				httpClient.prepareHead(website.concat(filename))
						.execute(headHandler(website, filename));
			} catch (IOException ex) {
				Logger.getLogger(School.class.getName()).log(Level.WARNING, null, ex);
			}
		}
	}

	private AsyncHandler<Response> headHandler(final String website, final String filename){
		return (website == null) ? null : new AsyncHandler<Response>() {
			private final Response.ResponseBuilder builder = new Response.ResponseBuilder();
			
			public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
				int code = responseStatus.getStatusCode();
				if (code == HttpServletResponse.SC_OK ||
						code == HttpServletResponse.SC_NOT_MODIFIED) {
					iconurl = website.contains(filename) ? website : website.concat(filename);
					update();
				}else if(code == HttpServletResponse.SC_MOVED_TEMPORARILY ||
						code == HttpServletResponse.SC_MOVED_PERMANENTLY){
					return STATE.CONTINUE;
				}else{
//					httpclient.execute(new HttpGet(website), getReqCallback(website));
					String baseurl = AbstractDAOUtils.getBaseURL(website);
					if(baseurl != null){
						baseurl = baseurl.endsWith("/") ? baseurl : baseurl.concat("/");
						redirectCount = 0;
						httpClient.prepareGet(baseurl).execute(getHandler(baseurl));
					}
				}
				return STATE.ABORT;
			}

			public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
				Map<String, List<String>> heads = headers.getHeaders();
				if(heads.containsKey("Location") && redirectCount <= 3){
					String newUrl = heads.get("Location").get(0);
					// retry request
					redirectCount++;
					httpClient.prepareHead(newUrl).execute(headHandler(newUrl, filename));
					return STATE.ABORT;
				}else{
					redirectCount = 0;
				}
				return STATE.CONTINUE;
			}

			public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
				return STATE.CONTINUE;
			}

			public Response onCompleted() throws Exception {return builder.build();}
			public void onThrowable(Throwable t) {}
		};
	}

	private AsyncHandler<Response> getHandler(final String website){
		return (website == null) ? null : new AsyncHandler<Response>() {
			private final Response.ResponseBuilder builder = new Response.ResponseBuilder();
			private boolean abort4Real = false;

			public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
				int code = responseStatus.getStatusCode();
				
				if (code == HttpServletResponse.SC_OK ||
					code == HttpServletResponse.SC_NOT_MODIFIED ||
					code == HttpServletResponse.SC_MOVED_TEMPORARILY ||
					code == HttpServletResponse.SC_MOVED_PERMANENTLY) {
					builder.accumulate(responseStatus);
					abort4Real = false;
					return STATE.CONTINUE;
				}
				abort4Real = true;
				return STATE.ABORT;
			}

			public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
				// check for text/html
				Map<String, List<String>> heads = headers.getHeaders();

				if(heads.containsKey("Location") && redirectCount <= 3){
					String newUrl = heads.get("Location").get(0);
					// retry request
					redirectCount++;
					httpClient.prepareGet(newUrl).execute(getHandler(newUrl));
				}else if(heads.containsKey("Content-Type")){
					String ct = heads.get("Content-Type").get(0);
					if(StringUtils.startsWith(ct, "application/xhtml+xml") ||
							StringUtils.startsWith(ct, "text/html")){
						builder.accumulate(headers);
						redirectCount = 0;
						abort4Real = false;
						return STATE.CONTINUE;
					}
				}
				abort4Real = true;
				return STATE.ABORT;
			}

			public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
				builder.accumulate(bodyPart);
				return STATE.CONTINUE;
			}

			public Response onCompleted() throws Exception {
				Response res = builder.build();
				if(!abort4Real){
					InputStream ins = res.getResponseBodyAsStream();
					parseHTML(ins);

				}
				builder.reset();
				return res;
			}
			public void onThrowable(Throwable t) {}

			private	void parseHTML(InputStream ins) throws IOException{
				final HashMap<String, String> map = new HashMap<String, String>();
				map.put("shortcut icon", null);
				map.put("icon", null);
				map.put("apple-touch-icon", null);
				Document html = Jsoup.parse(ins, null, website);
				// find apple-touch-icon OR icon links
				Elements icons = html.select("link[rel*=icon]");
				for (Element elem : icons) {
					String rel = elem.attr("rel");
					String href = elem.absUrl("href");

					if(map.containsKey(rel) && !StringUtils.isBlank(href))
						map.put(rel, href);
				}

				if(map.get("apple-touch-icon") != null){
					iconurl = map.get("apple-touch-icon");
				}else if(map.get("icon") != null){
					iconurl = map.get("icon");
				}else{
					iconurl = map.get("shortcut icon");
				}

				iconurl = StringUtils.trim(iconurl);
				boolean isImg = AbstractDAOUtils.endsWithAny(iconurl,
						new String[]{"ico", "png", "gif", "jpg"});
				if(!StringUtils.isBlank(iconurl) && isImg){
					update();
				}else{
					// finally look for favicon.ico at root of url
					httpClient.prepareHead(website.concat("favicon.ico"))
							.execute(headHandler(website, "favicon.ico"));
				}
			}
		};

	}

	public ArrayList<Media> getMedia(MediaType type, String label, MutableLong pagenum,
			MutableLong itemcount, int maxItems, boolean reverse) {
		return Media.getMediaDao().readAllMediaForID(this.id, type, label,
				pagenum, itemcount, maxItems, reverse);
	}

	public void deleteAllMedia(){
		Media.getMediaDao().deleteAllMediaForID(id);
	}

    public boolean equals(Object obj){
        if(this == obj)
                return true;
        if((obj == null) || (obj.getClass() != this.getClass()) ||
				this.name == null || this.location == null)
                return false;
        School school = (School)obj;
        return (this.name.equals(school.getName()) &&
				this.location.equals(school.getLocation()));
    }

    public int hashCode() {
        return (this.name + this.location).hashCode();
    }

	public boolean voteUp(Long userid) {
		return AbstractDAOFactory.getDefaultDAOFactory().getDAOUtils().voteUp(userid, this); //+1 up
	}

	public boolean voteDown(Long userid) {	//-1 down
		return AbstractDAOFactory.getDefaultDAOFactory().getDAOUtils().voteDown(userid, this);
	}

	public ArrayList<Post> getQuestions(String sortBy, MutableLong pagenum, MutableLong itemcount) {
		return Post.getPostDao().readAllPostsForID(PostType.QUESTION, this.id,
				sortBy, pagenum, itemcount, AbstractDAOFactory.MAX_ITEMS_PER_PAGE);
	}
}

