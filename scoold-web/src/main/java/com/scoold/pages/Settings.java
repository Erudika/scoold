/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scoold.pages;

import com.scoold.core.Media;
import com.scoold.core.User;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractDAOUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import org.apache.click.control.Form;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alexb 
 */
public class Settings extends BasePage {

	public String title;
	public ArrayList<String> openidlist;
	public Form changeEmailForm;
	public boolean canDetachOpenid;
	public boolean canAttachOpenid;

	// photo import enabled
	public boolean PI_ENABLED = false;

//	private HttpClient http;
	private String isoDatePattern =
			DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern();

	// keeps track of imported photos to prevent duplicates
	private HashSet<String> photoIDs = new HashSet<String>();

	public Settings() {
		title = lang.get("settings.title");
		includeFBscripts = true;
		sortIdentifiers(authUser.getIdentifiers());
		canDetachOpenid = canDetachOpenid();
		canAttachOpenid = canAttachOpenid();
		changeEmailForm = new Form();

		TextField email = new TextField("email", true);
		email.setMinLength(5);
		email.setMaxLength(250);
		email.setLabel("");
		email.setValue(authUser.getEmail());

		Submit s = new Submit("save", lang.get("save"), this, "onSaveClick");
		s.setAttribute("class", "button rounded3");
		s.setId("change-email-btn");

		changeEmailForm.add(email);
		changeEmailForm.add(s);

//		HttpParams params = new BasicHttpParams();
//		params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000L);
//		http = new DefaultHttpClient(params);
	}

	public boolean onSaveClick() {
		String mail = changeEmailForm.getFieldValue("email");
		if(mail == null || mail.contains("<") || mail.contains(">") || mail.contains("\\") ||
				!(mail.indexOf(".") > 2) && (mail.indexOf("@") > 0)){
			changeEmailForm.getField("email").setError("signup.form.error.email");
		}else if(User.exists(StringUtils.trim(mail))){
            //Email is claimed => user exists!
            changeEmailForm.getField("email").setError(lang.get("signup.form.error.emailexists"));
        }
		if (changeEmailForm.isValid()) {
			authUser.setEmail(getParamValue("email"));
			authUser.update();
			changeEmailForm.clearValues();
			setRedirect(settingslink);
		}
		return false;
	}

	public void onPost(){
		// check if the checksum is the same as the one in the link
		String redirectto = settingslink;
		if (param("deleteaccount") && StringUtils.equals(
				AbstractDAOUtils.MD5(authUser.getId().toString()), 
				getParamValue("key"))) {

			authUser.delete();
			clearSession();
			redirectto = signinlink + "?code=4&success=true";
		}else if(param("clearallmedia") && PI_ENABLED){
			authUser.deleteAllMedia();
			authUser.removeBadge(User.Badge.PHOTOLOVER);
			authUser.setPhotos(0L);
			authUser.update();
			redirectto = settingslink + "?code=1&success=true";
		}else if (param("importphotos") && PI_ENABLED){
			String from = getParamValue("importphotos");
			if ("facebook".equals(from) && param("authtoken")) {
				processFacebookImportRequest();
			} else if("flickr".equals(from) && param("username")) {
				proccessFlickrImportRequest();
			}else if("picasa".equals(from) && param("username")){
				proccessPicasaImportRequest();
			}
		}else if(param("favtags") && !StringUtils.isBlank(getParamValue("favtags"))){
			String cleanTags = AbstractDAOUtils.fixCSV(getParamValue("favtags"),
					AbstractDAOFactory.MAX_FAV_TAGS);
			authUser.setFavtags(cleanTags);
			authUser.update();
		}else if (param("detachid") && canDetachOpenid) {
			int oid = NumberUtils.toInt(getParamValue("detachid"), 10);

			if (oid == 0 || oid == 1) {
				authUser.detachIdentifier(openidlist.get(oid));
				openidlist.remove(oid);
				canAttachOpenid = canAttachOpenid();
				canDetachOpenid = canDetachOpenid();
			}
		}

		if(!isAjaxRequest())
			setRedirect(redirectto);
	}

	private void proccessPicasaImportRequest(){
		String username = getParamValue("username");
		String url = "http://picasaweb.google.com/data/feed/api/user/" +
				AbstractDAOUtils.urlEncode(username) +
				"?kind=photo&alt=json-in-script&max-results=" +
				AbstractDAOFactory.MAX_PHOTOS_PER_UUID +
				"&thumbsize=144u&imgmax=800&callback=callback";

		JSONObject obj = getRemoteJSON(url);

		try {
			if(obj != null && obj.has("feed")){
				JSONObject feed = obj.getJSONObject("feed");
				JSONArray entries = feed.getJSONArray("entry");

				importPicasaPhotos(entries);

				addModel("importsuccess", true);
			}else{
				addModel("importsuccess", false);
				return;
			}
		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
			addModel("importsuccess", false);
		}
	}

	private void proccessFlickrImportRequest(){
		String username = getParamValue("username");
		String url = "http://api.flickr.com/services/rest/?format=json&api_key=" +
				AbstractDAOUtils.urlEncode(FLICKR_API_KEY);
		String unameQuery = "&method=flickr.people.findByUsername&username=";
		String photosQuery = "&method=flickr.people.getPublicPhotos&per_page=500"
				+ "&jsoncallback=callback&extras=description,date_upload,"
				+ "url_sq,url_o,url_m,url_l&user_id=";

		JSONObject uname = getRemoteJSON(url.concat(unameQuery).concat(username));

		try {
			if(uname != null && uname.has("user")){
				JSONObject user = uname.getJSONObject("user");
				String nsid = user.getString("nsid");
				JSONObject p = getRemoteJSON(
						url.concat(photosQuery).concat(nsid));

				JSONObject photos = p.getJSONObject("photos");
				JSONArray pageOne = photos.getJSONArray("photo");
				int pages = photos.getInt("pages");

				importFlickrPhotos(pageOne);

				if(pages > 1){
					// get all the rest starting from page 2
					for (int i = 2; i <= pages; i++) {
						//get next page of photos
						JSONObject pn = getRemoteJSON(url.concat(photosQuery)
								.concat(nsid).concat("&page="+i));

						JSONObject photoz = pn.getJSONObject("photos");
						JSONArray pageN = photoz.getJSONArray("photo");

						importFlickrPhotos(pageN);
					}
				}

				addModel("importsuccess", true);
			}else{
				addModel("importsuccess", false);
				return;
			}
		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
			addModel("importsuccess", false);
		}
	}

	private void processFacebookImportRequest(){
		String token = getParamValue("authtoken");
		String url = "https://graph.facebook.com";
		String photos = url + "/me/photos";
		String albums = url + "/me/albums";
		String query = "?access_token=" + AbstractDAOUtils.urlEncode(token);

		JSONObject tagged = getRemoteJSON(photos + query);
		JSONObject albumz = getRemoteJSON(albums + query);

		try {
			// get user-created photos
			if(albumz != null){
				JSONArray arr = albumz.getJSONArray("data");
				if(arr == null) return;
				//get all photos for album
				for (int i = 0; i < arr.length(); i++) {
					JSONObject album = arr.getJSONObject(i);
					String photosUrl = url.concat("/")
							.concat(album.getString("id"))
							.concat("/photos").concat(query);

					String albumName = album.has("name") ?
						album.getString("name") : "facebook";

					JSONObject subalbum = getRemoteJSON(photosUrl);
					if(subalbum != null){
						JSONArray parr = subalbum.getJSONArray("data");
						importFacebookPhotos(parr, albumName);
					}
				}
			}

			// get tagged photos
			if(tagged != null){
				JSONArray tarr = tagged.getJSONArray("data");
				importFacebookPhotos(tarr, null);
			}

		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
			addModel("importsuccess", false);
		}
		addModel("importsuccess", true);
	}

	public void importPicasaPhotos(JSONArray photos) throws Exception{
		if(photos == null) return;
		long photoCount = authUser.getPhotos();

		for (int i= 0; i < photos.length(); i++) {
			JSONObject photo = photos.getJSONObject(i);

			Media m = new Media();
			m.setType(Media.MediaType.PHOTO.name());
			m.setUserid(authUser.getId());
			m.setParentuuid(authUser.getUuid());
			m.addLabel("picasa");

			if(photo.has("media$group")){
				JSONObject mediaGroup = photo.getJSONObject("media$group");
				JSONArray mc = mediaGroup.getJSONArray("media$content");
				if(mc != null && !mc.isNull(0)){
					JSONObject info = mc.getJSONObject(0);
					if(info.has("url")){
						m.setUrl(info.getString("url"));
					}
					if(info.has("height") && info.has("width")){
						m.setHeight(info.getInt("height"));
						m.setWidth(info.getInt("width"));
					}
				}
				JSONObject desc = mediaGroup.getJSONObject("media$description");
				if(desc.has("$t") && StringUtils.isBlank(desc.getString("$t"))){
					m.setDescription(desc.getString("$t"));
				}
				JSONArray tarr = mediaGroup.getJSONArray("media$thumbnail");
				if(tarr != null && !tarr.isNull(0)){
					JSONObject thumb = tarr.getJSONObject(0);
					if(thumb.has("url")){
						m.setThumburl(thumb.getString("url"));
					}
				}
			}

			if(photo.has("gphoto$timestamp")){
				m.setTimestamp(NumberUtils.toLong(
						photo.getString("gphoto$timestamp"), System.currentTimeMillis()));
			}

			if(photo.has("link")){
				JSONArray link = photo.getJSONArray("link");
				for (int j = 0; j < link.length(); j++) {
					JSONObject o = link.getJSONObject(j);
					if(o.has("rel") && "alternate".equals(o.getString("rel"))){
						m.setOriginalurl(o.getString("href"));
					}
				}
			}
			m.setProvider("Picasa");

			m.create();
			photoCount++;
		}
		if(photoCount > authUser.getPhotos()){
			authUser.setPhotos(photoCount);
			authUser.update();
		}
	}

	private void importFlickrPhotos(JSONArray photos) throws Exception{
		if(photos == null) return;
		long photoCount = authUser.getPhotos();

		for (int i= 0; i < photos.length(); i++) {
			JSONObject photo = photos.getJSONObject(i);

			Media m = new Media();
			m.setType(Media.MediaType.PHOTO.name());
			m.setUserid(authUser.getId());
			m.setParentuuid(authUser.getUuid());
			if(photo.has("tags")){
				String tags = photo.getString("tags");
				tags = ",".concat(tags.trim().replaceAll("\\s+", ",")).concat(",");
				m.setLabels(tags);
			}
			if(StringUtils.isBlank(m.getLabels())){
				m.addLabel("flickr");
			}

			int	wo = 0;
			int ho = 0;
			boolean isOriginalGood = false;
			if(photo.has("url_o")){
				ho = photo.getInt("height_o");
				wo = photo.getInt("width_o");
				m.setOriginalurl(photo.getString("url_o"));
				if(ho < 1000 && wo < 1000) isOriginalGood = true;
			}

			if (photo.has("url_l")) {
				m.setHeight(photo.getInt("height_l"));
				m.setWidth(photo.getInt("width_l"));
				m.setUrl(photo.getString("url_l"));
			}else if(isOriginalGood){
				m.setHeight(photo.getInt("height_o"));
				m.setWidth(photo.getInt("width_o"));
				m.setUrl(m.getOriginalurl());
			}else if(photo.has("url_m")) {
				m.setHeight(photo.getInt("height_m"));
				m.setWidth(photo.getInt("width_m"));
				m.setUrl(photo.getString("url_m"));
			}

			if(photo.has("url_sq")){
				m.setThumburl(photo.getString("url_sq"));
			}

			if(photo.has("description")){
				JSONObject desc = photo.getJSONObject("description");
				m.setDescription(desc.getString("_content"));
			}
			if(photo.has("title")){
				m.setTitle(photo.getString("title"));
			}
			if(photo.has("dateupload")){
				m.setTimestamp(NumberUtils.toLong(photo.getString("dateupload"),
						System.currentTimeMillis()));
			}
			m.setProvider("Flickr");
			
			m.create();
			photoCount++;
		}
		if(photoCount > authUser.getPhotos()){
			authUser.setPhotos(photoCount);
			authUser.update();
		}
	}

	private void importFacebookPhotos(JSONArray photos, String label) throws Exception{
		if(photos == null) return;
		long photoCount = authUser.getPhotos();

		for (int i = 0; i < photos.length(); i++) {
			JSONObject photo = photos.getJSONObject(i);
			String pid = photo.getString("id");
			if(!photoIDs.contains(pid)){
				photoIDs.add(pid);

				Media m = new Media();
				m.setType(Media.MediaType.PHOTO.name());
				m.setUserid(authUser.getId());
				m.setParentuuid(authUser.getUuid());
				if(!StringUtils.isBlank(label)){
					m.addLabel(label);
				}
				if(photo.has("height") && photo.has("width")){
					m.setHeight(photo.getInt("height"));
					m.setWidth(photo.getInt("width"));
				}
				if(photo.has("name")){
					m.setDescription(photo.getString("name"));
				}
				if(photo.has("created_time")){
					m.setTimestamp(DateUtils.parseDate(photo.getString("created_time"),
						new String[] { isoDatePattern }).getTime());
				}
				if(photo.has("link")){
					m.setOriginalurl(photo.getString("link"));
				}
				m.setProvider("Facebook");
				m.setThumburl(photo.getString("picture"));
				m.setUrl(photo.getString("source"));

				m.create();
				photoCount++;
			}
		}
		if(photoCount > authUser.getPhotos()){
			authUser.setPhotos(photoCount);
			authUser.update();
		}
	}

	private JSONObject getRemoteJSON(String url){
		//TODO: change this to use AsyncHTTPClient
//		//establish a connection within 5 seconds
//
//		JSONObject json = null;
//		//execute the method
//		String responseBody = null;
//		try {
//			HttpGet get = new HttpGet(url);
//			HttpResponse res = http.execute(get);
////			responseBody = method.getResponseBodyAsString();
//			InputStream stream = res.getEntity().getContent();
//			responseBody = IOUtils.toString(stream, AbstractDAOFactory.DEFAULT_ENCODING);
//
//			if(!StringUtils.isBlank(responseBody)){
//				responseBody = responseBody.trim();
//				if(responseBody.matches("^\\p{Alpha}+\\(.+")){
//					// assume JSONP
//					responseBody = responseBody.substring(
//							responseBody.indexOf("(") + 1, responseBody.lastIndexOf(")"));
//				}
//
//				json = new JSONObject(responseBody);
//			}
//		} catch (Exception ex) {
//			logger.log(Level.SEVERE, null, ex);
//		} finally {
//			//clean up the connection resources
//			http.getConnectionManager().shutdown();
//		}
//		return json;
		return null;
	}

	private void sortIdentifiers(ArrayList<String> list){
		openidlist = new ArrayList<String>();
		if(list != null && list.size() > 0){
			String id1 = authUser.getIdentifier();
			if(list.contains(id1)){
				openidlist.add(id1);
			}else{
				openidlist.add(list.get(0));
			}

			if(list.size() > 1){
				list.remove(id1);
				String id2 = list.get(0);
				openidlist.add(id2);
			}
		}
	}

	private boolean canDetachOpenid() {
		//make sure you we don't delete the user's primary openid
		return (openidlist.size() == 1) ? false : true;
	}

	//allow max 2 openids per account besides FB connect
	private boolean canAttachOpenid() {
		return  (openidlist.size() >= AbstractDAOFactory.MAX_IDENTIFIERS_PER_USER)
				? false : true;
	}
}
