package com.scoold.core;

import com.scoold.db.AbstractMediaDAO;
import com.scoold.db.AbstractDAOFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.lang.StringUtils;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang.mutable.MutableLong;

public class Media implements Votable<Long>, Commentable, 
		ScooldObject, Serializable {

    private Long id;
	private String uuid;
	@Stored private String type;
	@Stored private String url;
	@Stored private String title;
    @Stored private String description;
    @Stored private String tags;
    @Stored private Long userid;
	@Stored private String parentuuid;
	@Stored private String thumburl;
	@Stored private Long timestamp;
	@Stored private String labels;	// --> ,label,label,label, <--
	@Stored private Integer width;
	@Stored private Integer height;
	@Stored private String provider;
	@Stored private Integer votes;
	@Stored private String originalurl;
	@Stored private Long commentcount;
	@Stored private Long commentpage;
	@Stored private String link;
	@Stored private String oldlabels;

	private transient User author;
    private transient static AbstractMediaDAO<Media, Long> mydao;
	private ArrayList<Comment> comments = new ArrayList<Comment>(0);

	public static enum MediaType{ UNKNOWN, PHOTO, VIDEO, PRODUCT, AUDIO, TEXT, RICH };
	
    public static AbstractMediaDAO<Media, Long> getMediaDao(){
        return (mydao != null) ? mydao : (AbstractMediaDAO<Media, Long>)
				AbstractDAOFactory.getDefaultDAOFactory().getDAO(Media.class);
    }

	public Media(String uuid) {
		this();
		this.uuid = uuid;
	}
    
	public Media() {
		this.type = MediaType.UNKNOWN.toString();
		this.commentcount = 0L;
		this.votes = 0;
    }

	public Media(Long id) {
		this();
		this.id = id;
	}

	/**
	 * Get the value of link
	 *
	 * @return the value of link
	 */
	public String getLink() {
		return link;
	}

	/**
	 * Set the value of link
	 *
	 * @param link new value of link
	 */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * Get the value of originalurl
	 *
	 * @return the value of originalurl
	 */
	public String getOriginalurl() {
		return originalurl;
	}

	/**
	 * Set the value of originalurl
	 *
	 * @param originalurl new value of originalurl
	 */
	public void setOriginalurl(String originalurl) {
		this.originalurl = originalurl;
	}

	/**
	 * Get the value of sourceurl
	 *
	 * @return the value of sourceurl
	 */
	public String getProvider() {
		return provider;
	}

	/**
	 * Set the value of sourceurl
	 *
	 * @param sourceurl new value of sourceurl
	 */
	public void setProvider(String provider) {
		this.provider = provider;
	}


	/**
	 * Get the value of height
	 *
	 * @return the value of height
	 */
	public Integer getHeight() {
		return height;
	}

	/**
	 * Set the value of height
	 *
	 * @param height new value of height
	 */
	public void setHeight(Integer height) {
		this.height = height;
	}

	/**
	 * Get the value of width
	 *
	 * @return the value of width
	 */
	public Integer getWidth() {
		return width;
	}

	/**
	 * Set the value of width
	 *
	 * @param width new value of width
	 */
	public void setWidth(Integer width) {
		this.width = width;
	}

	/**
	 * Get the value of labels
	 *
	 * @return the value of labels
	 */
	public String getLabels() {
		return labels;
	}

	/**
	 * Set the value of labels
	 *
	 * @param labels new value of labels
	 */
	public void setLabels(String labels) {
		this.labels = labels;
	}

	/**
	 * Get the value of labels
	 *
	 * @return the value of labels
	 */
	public String getOldlabels() {
		return oldlabels;
	}

	/**
	 * Set the value of labels
	 *
	 * @param labels new value of labels
	 */
	public void setOldlabels(String labels) {
		this.oldlabels = labels;
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
	 * Get the value of thumburl
	 *
	 * @return the value of thumburl
	 */
	public String getThumburl() {
		return thumburl;
	}

	/**
	 * Set the value of thumburl
	 *
	 * @param thumburl new value of thumburl
	 */
	public void setThumburl(String thumburl) {
		this.thumburl = thumburl;
	}

	/**
	 * Get the value of parentuuid
	 *
	 * @return the value of parentuuid
	 */
	public String getParentuuid() {
		return parentuuid;
	}

	/**
	 * Set the value of parentuuid
	 *
	 * @param parentuuid new value of parentuuid
	 */
	public void setParentuuid(String parentuuid) {
		this.parentuuid = parentuuid;
	}

	/**
	 * Get the value of uuid
	 *
	 * @return the value of uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the value of uuid
	 *
	 * @param uuid new value of uuid
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
    /**
     * Get the value of authorid
     *
     * @return the value of authorid
     */
    public Long getUserid() {
        return userid;
    }

    /**
     * Set the value of authorid
     *
     * @param authorid new value of authorid
     */
    public void setUserid(Long userid) {
        this.userid = userid;
    }

    /**
     * Get the value of tags
     *
     * @return the value of tags
     */
    public String getTags() {
        return tags;
    }

    /**
     * Set the value of tags
     *
     * @param tags new value of tags
     */
    public void setTags(String tags) {
        this.tags = tags;
    }

    /**
     * Get the value of description
     *
     * @return the value of description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the value of description
     *
     * @param description new value of description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the value of title
     *
     * @return the value of title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the value of title
     *
     * @param title new value of title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get the value of url
     *
     * @return the value of url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the value of url
     *
     * @param url new value of url
     */
    public void setUrl(String url) {
        this.url = url;
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
		if(type == null){
			this.type = MediaType.UNKNOWN.name();
			return;
		}
        this.type = type.toUpperCase();
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

	public Long getId() {
		return id;
	}

	public void setId(Long id){
		this.id = id;
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

	public boolean addLabel(String label){
		//label is already in labels
		if(StringUtils.contains(labels, ","+label+",")) return false;
		if(StringUtils.isBlank(labels)) labels = ",";
		label = label.replaceAll(",", ""); // no commas in label!
		labels += label + ",";
		return true;
	}

	public void addLabels(String[] larr){
		for (String label : larr) {
			label = label.trim();
			addLabel(label);
		}
	}

	public void removeLabel(String label){
		if(StringUtils.isBlank(labels)) return;
		label = ",".concat(label).concat(",");

		if(labels.contains(label)){
			labels = labels.replaceAll(label, ",");
		}
		if(StringUtils.isBlank(labels.replaceAll(",", ""))){
			labels = "";
		}
	}

	public Set<String> getLabelsSet(){
		Set<String> ls = new TreeSet<String>();
		if(StringUtils.isBlank(labels)) return ls;
		String allLabels = labels;
		if(!StringUtils.isBlank(allLabels)){
			allLabels = StringUtils.stripStart(allLabels, ",");
			allLabels = StringUtils.stripEnd(allLabels, ",");
			ls.addAll(Arrays.asList(allLabels.split(",")));
		}
		ls.remove("");
		return ls;
	}

	public boolean isLabeled(String label){
		return !StringUtils.isBlank(labels) && labels.contains(","+label+",");
	}

	public boolean isVideo(){
		return MediaType.RICH.toString().equalsIgnoreCase(this.type);
	}

	public boolean isImage(){
		return MediaType.PHOTO.toString().equalsIgnoreCase(this.type);
	}

    public User getAuthor(){
		if(userid == null) return null;
        if(author == null) author = User.getUser(userid);
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

    public Long create() {
        this.id = getMediaDao().create(this);
        return this.id;
    }

    public void update() {
        getMediaDao().update(this);
    }

    public void delete() {
        getMediaDao().delete(this);
    }

	public boolean voteUp(Long userid) {
		return AbstractDAOFactory.getDefaultDAOFactory().getDAOUtils().voteUp(userid, this);
	}

	public boolean voteDown(Long userid) {	
		return AbstractDAOFactory.getDefaultDAOFactory().getDAOUtils().voteDown(userid, this);
	}

	public ArrayList<Comment> getComments(MutableLong page){
		MutableLong itemcount = new MutableLong(); 
		this.comments = Comment.getCommentDao()
				.readAllCommentsForUUID(uuid, page, itemcount);
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
	
}

