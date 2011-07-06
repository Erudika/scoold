/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.core;

import com.scoold.db.AbstractDAOUtils;
import com.scoold.db.AbstractPostDAO;
import com.scoold.db.AbstractDAOFactory;
import java.io.Serializable;
import java.util.ArrayList;
import org.apache.click.control.Form;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
public class Post implements ScooldObject, Votable<Long>,
		Commentable, Searchable<Post>, Serializable{

	private Long id;
	private String uuid;
	@Indexed
	@Stored private String body;
	@Indexed
	@Stored private String title;
	@Stored private Long userid;
	@Stored private Long timestamp;
	@Indexed
	@Stored private String parentuuid;
	@Stored private String type;
	@Stored private Long viewcount;
	@Stored private Long answerid;
	@Stored private String revisionuuid;
	@Stored private Long closerid;
	@Indexed
	@Stored private String tags;
	@Indexed
	@Stored private Long answercount;
	@Indexed
	@Stored private Long lastactivity;
	@Stored private Boolean deleteme;
	@Stored private Long lasteditby;
	@Stored private Integer votes;
	@Stored private Long commentcount;
	//this is the question id. 
	//similar to parentuuid but always 
	//shows the parent/current question id
	@Indexed
	@Stored private Long parentpostid;
	@Stored private Long deletereportid;

	private Integer oldvotes;
	private Long oldactivity;

	private transient User author;
	private transient User lastEditor;
	private transient ArrayList<Comment> comments;
	private transient Form editForm;
	private transient  Long pagenum;

	public static enum PostType{
		QUESTION, ANSWER, BLACKBOARD, FEEDBACK, TRANSLATION, UNKNOWN
	}

	public static enum FeedbackType{
		BUG, QUESTION, SUGGESTION;
		
		public String toString(){
			return super.toString().toLowerCase();
		}
	}

	private transient static AbstractPostDAO<Post, Long> mydao;

	public static AbstractPostDAO<Post, Long> getPostDao(){
		return (mydao != null) ? mydao : (AbstractPostDAO<Post, Long>)
				AbstractDAOFactory.getDefaultDAOFactory().getDAO(Post.class);
	}

	public Post(){
		votes = 0;
		answercount = 0L;
		viewcount = 0L;
		commentcount = 0L;
		pagenum = 0L;
		deleteme = false;
	}

	public Post(Long id) {
		this.id = id;
		votes = 0;
		answercount = 0L;
		viewcount = 0L;
		commentcount = 0L;
		pagenum = 0L;
		deleteme = false;
	}

	public Post(String uuid) {
		this.uuid = uuid;
		votes = 0;
		answercount = 0L;
		viewcount = 0L;
		commentcount = 0L;
		pagenum = 0L;
		deleteme = false;
	}

	public Long getPagenum() {
		return pagenum;
	}

	public void setPagenum(Long pagenum) {
		this.pagenum = pagenum;
	}

	/**
	 * Get the value of deletereportid
	 *
	 * @return the value of deletereportid
	 */
	public Long getDeletereportid() {
		return deletereportid;
	}

	/**
	 * Set the value of deletereportid
	 *
	 * @param deletereportid new value of deletereportid
	 */
	public void setDeletereportid(Long deletereportid) {
		this.deletereportid = deletereportid;
	}

	/**
	 * Get the value of parentpostid
	 *
	 * @return the value of parentpostid
	 */
	public Long getParentpostid() {
		return parentpostid;
	}

	/**
	 * Set the value of parentpostid
	 *
	 * @param parentpostid new value of parentpostid
	 */
	public void setParentpostid(Long parentpostid) {
		this.parentpostid = parentpostid;
	}

	/**
	 * Get the value of oldactivity
	 *
	 * @return the value of oldactivity
	 */
	public Long getOldactivity() {
		return oldactivity;
	}

	/**
	 * Set the value of oldactivity
	 *
	 * @param oldactivity new value of oldactivity
	 */
	public void setOldactivity(Long oldactivity) {
		this.oldactivity = oldactivity;
	}

	/**
	 * Get the value of oldvotes
	 *
	 * @return the value of oldvotes
	 */
	public Integer getOldvotes() {
		return oldvotes;
	}

	/**
	 * Set the value of oldvotes
	 *
	 * @param oldvotes new value of oldvotes
	 */
	public void setOldvotes(Integer oldvotes) {
		this.oldvotes = oldvotes;
	}

	/**
	 * Get the value of editForm
	 *
	 * @return the value of editForm
	 */
	public Form getEditForm() {
		return editForm;
	}

	/**
	 * Set the value of editForm
	 *
	 * @param editForm new value of editForm
	 */
	public void setEditForm(Form editForm) {
		this.editForm = editForm;
	}


	/**
	 * Get the value of lasteditby
	 *
	 * @return the value of lasteditby
	 */
	public Long getLasteditby() {
		return lasteditby;
	}

	/**
	 * Set the value of lasteditby
	 *
	 * @param lasteditby new value of lasteditby
	 */
	public void setLasteditby(Long lasteditby) {
		this.lasteditby = lasteditby;
	}

	/**
	 * Get the value of deleteme
	 *
	 * @return the value of deleteme
	 */
	public Boolean getDeleteme() {
		return deleteme;
	}

	/**
	 * Set the value of deleteme
	 *
	 * @param deleteme new value of deleteme
	 */
	public void setDeleteme(Boolean deleteme) {
		this.deleteme = deleteme;
	}

	/**
	 * Get the value of lastactivity
	 *
	 * @return the value of lastactivity
	 */
	public Long getLastactivity() {
		return lastactivity;
	}

	/**
	 * Set the value of lastrevisiondate
	 *
	 * @param lastactivity new value of lastrevisiondate
	 */
	public void setLastactivity(Long lastactivity) {
		this.lastactivity = lastactivity;
	}

	/**
	 * Get the value of answercount
	 *
	 * @return the value of answercount
	 */
	public Long getAnswercount() {
		return answercount;
	}

	/**
	 * Set the value of answercount
	 *
	 * @param answercount new value of answercount
	 */
	public void setAnswercount(Long answercount) {
		this.answercount = answercount;
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
	 * Get the value of closed
	 *
	 * @return the value of closed
	 */
	public Long getCloserid() {
		return closerid;
	}

	/**
	 * Set the value of closed
	 *
	 * @param closed new value of closed
	 */
	public void setCloserid(Long closed) {
		this.closerid = closed;
	}

	/**
	 * Get the value of revisionuuid
	 *
	 * @return the value of revisionuuid
	 */
	public String getRevisionuuid() {
		return revisionuuid;
	}

	/**
	 * Set the value of revisionuuid
	 *
	 * @param revisionuuid new value of revisionuuid
	 */
	public void setRevisionuuid(String revisionuuid) {
		this.revisionuuid = revisionuuid;
	}


	/**
	 * Get the value of answerid
	 *
	 * @return the value of answerid
	 */
	public Long getAnswerid() {
		return answerid;
	}

	/**
	 * Set the value of answerid
	 *
	 * @param answerid new value of answerid
	 */
	public void setAnswerid(Long answerid) {
		this.answerid = answerid;
	}

	/**
	 * Get the value of viewcount
	 *
	 * @return the value of viewcount
	 */
	public Long getViewcount() {
		return viewcount;
	}

	/**
	 * Set the value of viewcount
	 *
	 * @param viewcount new value of viewcount
	 */
	public void setViewcount(Long viewcount) {
		this.viewcount = viewcount;
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
		this.type = type;
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
	 * Get the value of body
	 *
	 * @return the value of body
	 */
	public String getBody() {
		return body;
	}

	/**
	 * Set the value of body
	 *
	 * @param body new value of body
	 */
	public void setBody(String body) {
		this.body = body;
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
		setOldvotes(this.votes);
		this.votes = votes;
	}

	public Long getId(){
		return id;
	}

	public void setId(Long id){
		this.id = id;
	}

	public boolean isClosed(){
		return this.closerid != null;
	}

	public String getTagsString(){
		if(StringUtils.isBlank(tags)) return "";
		return tags.substring(1, tags.length() - 1).replaceAll(",", ", ");
	}

	public void fixTags(){
		tags = AbstractDAOUtils.fixCSV(tags);
	}

	public void fixTitle(){
		if(StringUtils.trimToNull(title) == null) return;
		title = title.replaceAll("\\p{S}", "");
		title = title.replaceAll("\\p{C}", "");
	}

	public Long create() {
		this.id = getPostDao().create(this);
		return this.id;
	}

	public void update() {
		getPostDao().update(this);
	}

	public void delete() {
		getPostDao().delete(this);
	}

	public boolean updateAndCreateRevision(Post originalPost){
		return getPostDao().updateAndCreateRevision(this, originalPost);
	}

	public User getAuthor(){
		if(userid == null) return null;
		if(author == null) author = User.getUser(userid);
		return author;
	}

	public User getLastEditor(){
		if(lasteditby == null) return null;
		if(lastEditor == null) lastEditor = User.getUser(lasteditby);
		return lastEditor;
	}

	public void restoreRevisionAndUpdate(Long revisionid){
		Revision.getRevisionDao().restoreRevision(revisionid, this);
	}

	public boolean voteUp(Long userid) {
		return AbstractDAOFactory.getDefaultDAOFactory().getDAOUtils().voteUp(userid, this);
	}

	public boolean voteDown(Long userid) {
		return AbstractDAOFactory.getDefaultDAOFactory().getDAOUtils().voteDown(userid, this);
	}

	public void setComments(ArrayList<Comment> comments) {
		this.comments = comments;
	}

	public ArrayList<Comment> getComments(){
		return this.comments;
	}

	public ArrayList<Comment> getComments(MutableLong page) {
		this.comments = Comment.getCommentDao()
				.readAllCommentsForUUID(uuid, page, new MutableLong(commentcount));
		this.pagenum = page.longValue();
		return this.comments;
	}

	public Long getCommentcount() {
		return this.commentcount;
	}

	public void setCommentcount(Long count){
		this.commentcount = count;
	}

	public ArrayList<Post> getAnswers(String sortby, MutableLong page, MutableLong itemcount){
		return getPostDao().readAllPostsForUUID(PostType.ANSWER, this.uuid,
				sortby, page, itemcount);
	}

	public ArrayList<Revision> getRevisions(MutableLong page, MutableLong itemcount){
		return Revision.getRevisionDao().readAllRevisionsForPost(this.uuid, page, itemcount);
	}

	public boolean isAnswer(){
		return isOfType(PostType.ANSWER);
	}

	public boolean isQuestion(){
		return isOfType(PostType.QUESTION);
	}

	public boolean isBlackboard(){
		return isOfType(PostType.BLACKBOARD);
	}

	public boolean isFeedback(){
		return isOfType(PostType.FEEDBACK);
	}

	public boolean isTranslation(){
		return isOfType(PostType.TRANSLATION);
	}

	private boolean isOfType(PostType type){
		if(this.type == null) return false;
		return this.type.equalsIgnoreCase(type.name());
	}

	public void setPostType(PostType type){
		if(type != null) this.type = type.name();
	}

	public PostType getPostType(){
		if(type == null) return PostType.UNKNOWN;
		try {
			return PostType.valueOf(type);
		} catch (Exception e) {
			return PostType.UNKNOWN;
		}
	}

	public void updateLastActivity(){
		setOldactivity(lastactivity);
		setLastactivity(System.currentTimeMillis());
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Post other = (Post) obj;
		if ((this.body == null) ? (other.body != null) : !this.body.equals(other.body)) {
			return false;
		}
		if ((this.title == null) ? (other.title != null) : !this.title.equals(other.title)) {
			return false;
		}
		if ((this.type == null) ? (other.type != null) : !this.type.equals(other.type)) {
			return false;
		}
		if ((this.tags == null) ? (other.tags != null) : !this.tags.equals(other.tags)) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + (this.body != null ? this.body.hashCode() : 0);
		hash = 97 * hash + (this.type != null ? this.type.hashCode() : 0);
		return hash;
	}

	public void index() {
		Search.indexCreate(this);
	}

	public void reindex() {
		Search.indexUpdate(this);
	}

	public void unindex() {
		Search.indexDelete(this);
	}

	public ArrayList<Post> readAllForKeys(ArrayList<String> keys) {
		if(keys == null || keys.isEmpty()) return new ArrayList<Post> ();
		return getPostDao().readAllForKeys(keys);
	}

	public class Question extends Post{
		// dummy class
	}

	public class Answer extends Post{
		// dummy class
	}

	public class Feedback extends Post{
		// dummy class
	}

}
