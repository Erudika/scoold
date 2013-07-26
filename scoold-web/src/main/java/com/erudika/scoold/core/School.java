package com.erudika.scoold.core;

import com.erudika.para.core.Linker;
import com.erudika.para.core.PObject;
import com.erudika.para.utils.DAO;
import com.erudika.para.utils.Stored;
import com.erudika.para.utils.Utils;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHandler.STATE;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.Response;
import com.erudika.scoold.core.Media.MediaType;
import com.erudika.scoold.util.Constants;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


/**
 * 
 * @author alexb
 */
public class School extends PObject{
	private static final long serialVersionUID = 1L;

    @Stored private String location;
    @Stored private String type;
    @Stored private Integer fromyear;
    @Stored private Integer toyear;
	@Stored private String about;
	@Stored private Integer votes;
	@Stored private String contacts;
	@Stored private String iconurl;

	public static enum SchoolType{
		UNKNOWN, HIGHSCHOOL, LYCEUM, COLLEGE, THEOLOGY, SEMINARY, ACADEMY, SPECIALIZED,
		PRIVATE, PRIMARY, SECONDARY, UNIVERSITY, ELEMENTARY, GYMNASIUM, MIDDLE,
		ARTS, SPORTS;

		public String toString(){
			return super.toString().toLowerCase();
		}
	};

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
		setName(name);
		this.location = location;
		this.votes = 0;
		this.about = "";
		this.type = getSchoolType(type).toString();
	}

	public School(String id){
		this();
        setId(id);
    }

	public void setContacts(String contacts) {
		this.contacts = contacts;
	}
	
	public String getIconurl() {
		return iconurl;
	}

	public void setIconurl(String iconurl) {
		this.iconurl = iconurl;
	}


	public String getContacts() {
		return contacts;
	}

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = getSchoolType(type).toString();
    }

    public Integer getToyear() {
        return toyear;
    }

    public void setToyear(Integer toyear) {
        this.toyear = toyear;
    }

    public Integer getFromyear() {
        return fromyear;
    }

    public void setFromyear(Integer fromyear) {
        this.fromyear = fromyear;
    }

	public String getAbout() {
		return about;
	}

	public void setAbout(String about) {
		this.about = about;
	}

	public Integer getVotes() {
		return votes;
	}

	public void setVotes(Integer votes) {
		this.votes = votes;
	}

	public ArrayList<User> getAllUsers(MutableLong page, MutableLong itemcount){
		return this.getLinkedObjects(User.class, page, itemcount);
	}
    
    public ArrayList<Classunit> getAllClassUnits(MutableLong page, MutableLong itemcount){
		return this.getLinkedObjects(Classunit.class, page, itemcount);
    }
        
    public boolean linkToUser(String userid){
		if(userid == null) return false;
		// auto add to my schools
		User u = new User(userid);
		long count = u.countLinks(School.class);
		if(count < Constants.MAX_SCHOOLS_PER_USER){
			u.link(School.class, getParentid());
			return true;
		}
		return false;
    }

    public void unlinkFromUser(String userid){
		this.unlink(User.class, userid);
    }
	
	public boolean isLinkedTo(User u){
		return this.isLinked(User.class, u.getId());
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
			return Utils.isValidURL(www) ? www : null;
		}
		return null;
	}

	public String create() {
		linkToUser(getCreatorid());
		return super.create();
	}
    
    public void update(){
		super.update();
		updateSchoolPic();
    }
    
    public void delete(){
		super.delete();
		deleteChildren(Classunit.class);
		deleteChildren(Media.class);
		unlinkAll();
    }

	private void updateSchoolPic(){
		String www = getWebsite();
		if(!StringUtils.isBlank(www) && (StringUtils.isBlank(iconurl) ||
				!StringUtils.contains(iconurl, Utils.getHostFromURL(www)))){
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
					String baseurl = Utils.getBaseURL(website);
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
				boolean isImg = StringUtils.endsWithAny(iconurl, "ico", "png", "gif", "jpg");
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
		return Media.getAllMedia(getId(), type, pagenum, itemcount, reverse, maxItems);
	}

	public void deleteAllMedia(){
		deleteChildren(Media.class);
	}

	public static boolean mergeSchools(String primarySchoolid, String duplicateSchoolid){
		Classunit primarySchool = DAO.getInstance().read(primarySchoolid);
		Classunit duplicateSchool = DAO.getInstance().read(duplicateSchoolid);
		
		if(primarySchool == null || duplicateSchool == null) return false;
		else if(!duplicateSchool.getParentid().equals(primarySchool.getParentid())) return false;

		// STEP 1:
		// Move every user to the primary class
		// STEP 2:
		// move all classes to primary school
		// STEP 3:
		// move media to primary class
		ArrayList<Linker> allLinks = new ArrayList<Linker>();
		allLinks.addAll(duplicateSchool.getAllLinks(User.class));
		allLinks.addAll(duplicateSchool.getAllLinks(Classunit.class));
		allLinks.addAll(duplicateSchool.getAllLinks(Media.class));
		
		for (Linker link : allLinks) {
			try {
				PropertyUtils.setProperty(link, link.getFirstIdFieldName(), primarySchoolid);
			} catch (Exception ex) {
				Logger.getLogger(Classunit.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		
		DAO.getInstance().updateAll(allLinks);

		// STEP 4:
		// delete duplicate
		duplicateSchool.delete();

		return true;
	}
	
    public boolean equals(Object obj){
        if(this == obj)
                return true;
        if((obj == null) || (obj.getClass() != this.getClass()) ||
				getName() == null || this.location == null)
                return false;
        School school = (School)obj;
        return (getName().equals(school.getName()) &&
				this.location.equals(school.getLocation()));
    }

    public int hashCode() {
        return (getName() + this.location).hashCode();
    }

	public ArrayList<Post> getQuestions(String sortBy, MutableLong pagenum, MutableLong itemcount) {
		return getChildren(Question.class, pagenum, itemcount, sortBy, Utils.MAX_ITEMS_PER_PAGE);
	}
}

