package com.erudika.scoold.core;

import com.erudika.para.core.PObject;
import static com.erudika.para.core.PObject.classname;
import com.erudika.para.annotations.Stored;
import com.erudika.para.persistence.DAO;
import com.erudika.para.search.Search;
import com.erudika.para.utils.Config;
import com.erudika.scoold.utils.AppConfig;
import java.util.ArrayList;
import java.util.HashSet;
import org.apache.commons.lang3.mutable.MutableLong;

public class Media extends PObject {
	private static final long serialVersionUID = 1L;

	@Stored private String type;
	@Stored private String url;
	@Stored private String title;
    @Stored private String description;
    @Stored private String tags;
	@Stored private String thumburl;
	@Stored private Integer width;
	@Stored private Integer height;
	@Stored private String provider;
	@Stored private String originalurl;
	@Stored private Long commentcount;
	@Stored private Long commentpage;
	@Stored private String link;

	private transient User author;
	private ArrayList<Comment> comments = new ArrayList<Comment>(0);

	public static enum MediaType{ 
		UNKNOWN, PHOTO, RICH 
	};
	
	public Media() {
		this.type = MediaType.UNKNOWN.toString();
		this.commentcount = 0L;
    }

	public Media(String id) {
		this();
		setId(id);
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getOriginalurl() {
		return originalurl;
	}

	public void setOriginalurl(String originalurl) {
		this.originalurl = originalurl;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}

	public Integer getWidth() {
		return width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public String getThumburl() {
		return thumburl;
	}

	public void setThumburl(String thumburl) {
		this.thumburl = thumburl;
	}

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
		if(type == null){
			this.type = MediaType.UNKNOWN.name();
			return;
		}
        this.type = type.toUpperCase();
    }
    
	public Long getCommentcount() {
		return this.commentcount;
	}

	public void setCommentcount(Long count){
		this.commentcount = count;
	}

	public void setCommentpage(Long commentpage){
		this.commentpage = commentpage;
	}

	public Long getCommentpage(){
		return this.commentpage;
	}

	public boolean isVideo(){
		return MediaType.RICH.toString().equalsIgnoreCase(this.type);
	}

	public boolean isImage(){
		return MediaType.PHOTO.toString().equalsIgnoreCase(this.type);
	}

    public User getAuthor(){
		if(getCreatorid() == null) return null;
        if(author == null) author = getDao().read(getCreatorid());
		return author;
    }
    
    public boolean equals(Object obj){
        if(this == obj)
                return true;
        if((obj == null) || (obj.getClass() != this.getClass()) || this.url == null)
                return false;
        Media media = (Media)obj;
        return this.url.equals(media.getUrl());
    }

	public int hashCode() {
		int hash = 5; 
		hash = 41 * hash + (this.type != null ? this.type.hashCode() : 0);
		hash = 41 * hash + (this.url != null ? this.url.hashCode() : 0);
		return hash;
	}

    public String create() {
		int count = getSearch().getCount(getClassname(), DAO.CN_PARENTID, getParentid()).intValue();
		if(count > AppConfig.MAX_MEDIA_PER_ID) return null;
		return super.create();
    }

    public void delete() {
        super.delete();
		deleteChildren(Comment.class);
    }
	
	public static ArrayList<Media> getAllMedia(String parentid, MediaType type, MutableLong page, 
			MutableLong itemcount, boolean reverse, int max, Search search){
		return search.findTwoTerms(classname(Media.class), page, itemcount, DAO.CN_PARENTID, parentid, 
				"type", type.name(), null, reverse, max);
	}
	
	public ArrayList<Comment> getComments(MutableLong page){
		MutableLong itemcount = new MutableLong(); 
		this.comments = getChildren(Comment.class, page, itemcount, null, Config.MAX_ITEMS_PER_PAGE);
		commentcount = itemcount.longValue();
		return this.comments;
	}

	public ArrayList<Comment> getComments(){
		return this.comments;
	}

	public void setComments(ArrayList<Comment> comments) {
		this.comments = comments;
	}

	public MediaType getMediaType(){
		if(type == null) return MediaType.UNKNOWN;
		try {
			return MediaType.valueOf(type.toUpperCase());
		} catch (Exception e) {
			return MediaType.UNKNOWN;
		}
	}
	
	public static ArrayList<Media> readPhotosAndCommentsForID (String parentid,
				String label, Long photoid, MutableLong itemcount, Search search) {

		MediaType type = MediaType.PHOTO;
		ArrayList<Media> photos = getAllMedia(parentid, type, new MutableLong(photoid), itemcount, true, 1, search);

		if(photos.isEmpty()) return photos;
		Media curr = photos.get(0);
		Media next = new Media();
		Media prev = new Media();

		// CASE 1: one photo in list
		if(itemcount.longValue() == 1){
			prev = curr;
			next = curr;
		}else if(itemcount.longValue() == 2){
		// CASE 2: two photos in list
			if (photos.size() == 1) {
				photos.clear();
				photos = getAllMedia(parentid, type, new MutableLong(0), null, true, 1, search);
				next = photos.get(0);
				prev = next;
			} else if(photos.size() == 2) {
				next = photos.get(1);
				prev = next;
			}
		}else if(itemcount.longValue() >= 3){
		// CASE 3: 3 or more photos in list
			if (photos.size() == 1) {
				photos.clear();
				photos = getAllMedia(parentid, type, new MutableLong(0), null, true, 1, search);
				next = photos.get(0);
				// reverse to get previous
				photos.clear();
				photos = getAllMedia(parentid, type, new MutableLong(photoid), null, false, 1, search);
				prev = photos.get(1);
			} else if(photos.size() == 2) {
				next = photos.get(1);
				// reverse to get previous
				photos.clear();
				photos = getAllMedia(parentid, type, new MutableLong(photoid), null, false, 1, search);
				if (photos.size() == 1) {
					//get last
					photos.clear();
					photos = getAllMedia(parentid, type, new MutableLong(0), null, false, 1, search);
					prev = photos.get(0);
				} else if(photos.size() == 2) {
					prev = photos.get(1);
				}
			}
		}

		photos.clear();
		photos.add(curr);
		photos.add(prev);
		photos.add(next);

		HashSet<String> mids = new HashSet<String>(3);
		// read comments for every photo in list
		for (Media photo : photos) {
			if(!mids.contains(photo.getId())){
				MutableLong commentCount = new MutableLong(0);
				MutableLong commentPage = new MutableLong(0);
				ArrayList<Comment> commentz = photo.getChildren(Comment.class, commentPage, commentCount, 
						null, Config.MAX_ITEMS_PER_PAGE);

				photo.setComments(commentz);
				photo.setCommentcount(commentCount.longValue());
				photo.setCommentpage(commentPage.longValue());
				mids.add(photo.getId());
			}
		}

		return photos;
	}
	
}

