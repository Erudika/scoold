/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.core;

import com.erudika.para.core.Tag;
import com.erudika.para.annotations.Stored;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.User;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.utils.AppConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.click.control.Form;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public abstract class Post extends Sysprop {
	private static final long serialVersionUID = 1L;

	@Stored private String body;
	@Stored private String title;
	@Stored private Long viewcount;
	@Stored private String answerid;
	@Stored private String revisionid;
	@Stored private String closerid;
	@Stored private Long answercount;
	@Stored private Long lastactivity;
	@Stored private Boolean deleteme;
	@Stored private String lasteditby;
	@Stored private Long commentcount;
	@Stored private String deletereportid;

	private transient User author;
	private transient User lastEditor;
	private transient List<Comment> comments;
	private transient Form editForm;
	private transient Long pagenum;

	public Post() {
		this.answercount = 0L;
		this.viewcount = 0L;
		this.commentcount = 0L;
		this.pagenum = 0L;
		this.deleteme = false;
	}

	public Long getPagenum() {
		return pagenum;
	}

	public void setPagenum(Long pagenum) {
		this.pagenum = pagenum;
	}

	public String getDeletereportid() {
		return deletereportid;
	}

	public void setDeletereportid(String deletereportid) {
		this.deletereportid = deletereportid;
	}

	public Form getEditForm() {
		return editForm;
	}

	public void setEditForm(Form editForm) {
		this.editForm = editForm;
	}

	public String getLasteditby() {
		return lasteditby;
	}

	public void setLasteditby(String lasteditby) {
		this.lasteditby = lasteditby;
	}

	public Boolean getDeleteme() {
		return deleteme;
	}

	public void setDeleteme(Boolean deleteme) {
		this.deleteme = deleteme;
	}

	public Long getLastactivity() {
		return lastactivity;
	}

	public void setLastactivity(Long lastactivity) {
		this.lastactivity = lastactivity;
	}

	public Long getAnswercount() {
		return answercount;
	}

	public void setAnswercount(Long answercount) {
		this.answercount = answercount;
	}

	public String getCloserid() {
		return closerid;
	}

	public void setCloserid(String closed) {
		this.closerid = closed;
	}

	public String getRevisionid() {
		return revisionid;
	}

	public void setRevisionid(String revisionid) {
		this.revisionid = revisionid;
	}

	public String getAnswerid() {
		return answerid;
	}

	public void setAnswerid(String answerid) {
		this.answerid = answerid;
	}

	public Long getViewcount() {
		return viewcount;
	}

	public void setViewcount(Long viewcount) {
		this.viewcount = viewcount;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		if (StringUtils.trimToNull(title) == null) return;
		title = title.replaceAll("\\p{S}", "");
		title = title.replaceAll("\\p{C}", "");
		this.title = title;
		setName(title);
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public boolean isClosed() {
		return this.closerid != null;
	}

	public String getTagsString() {
		if (getTags() == null || getTags().isEmpty()) return "";
		return StringUtils.join(getTags(), ", ");
	}

	public String create() {
		if (getRevisionid() == null && canHaveRevisions()) {
			//create initial revision = simply create a copy
			setRevisionid(Revision.createRevisionFromPost(this, true).create());
		}
		if (getTags() != null) {
			createTags(new HashSet<String>(getTags()));
		}
		Post p = AppConfig.client().create(this);
		if (p != null) {
			setId(p.getId());
			setTimestamp(p.getTimestamp());
			return p.getId();
		}
		return null;
	}

	public void update() {
		updateLastActivity();
		if (canHaveRevisions()) {
			setRevisionid(Revision.createRevisionFromPost(this, true).create());
		}
		createTags(new HashSet<String>(getTags()));
		AppConfig.client().update(this);
	}

	public void delete() {
		// delete post
		AppConfig.client().delete(this);
		ArrayList<ParaObject> children = new ArrayList<ParaObject>();
		ArrayList<String> ids = new ArrayList<String>();
		// delete Comments
		children.addAll(AppConfig.client().getChildren(this, Utils.type(Comment.class)));
		// delete Revisions
		children.addAll(AppConfig.client().getChildren(this, Utils.type(Revision.class)));

		if (canHaveChildren()) {
			for (ParaObject reply : AppConfig.client().getChildren(this, Utils.type(Reply.class))) {
				// delete Comments
				children.addAll(AppConfig.client().getChildren(reply, Utils.type(Comment.class)));
				// delete Revisions
				children.addAll(AppConfig.client().getChildren(reply, Utils.type(Revision.class)));
			}
		}
		for (ParaObject child : children) {
			ids.add(child.getId());
		}
		AppConfig.client().deleteAll(ids);
	}


	private void createTags(Set<String> newTags) {
		if (newTags == null) return;
		for (String ntag : newTags) {
			ntag = StringUtils.trimToEmpty(ntag);
			Tag t = new Tag(ntag);
			// create tag if it doesn't exist
			if (!StringUtils.isBlank(ntag) && AppConfig.client().getCount(Utils.type(Tag.class),
					Collections.singletonMap(Config._ID, t.getId())) == 0) {
				t.create();
			}
		}
	}

	public static <P extends Post> void readAllCommentsForPosts(List<P> list, int maxPerPage) {
		for (P post : list) {
			Pager page = new Pager(post.getPagenum(), null, true, 5);
			List<Comment> commentz = AppConfig.client().getChildren(post, Utils.type(Comment.class), page);
			post.setComments(commentz);
			post.setPagenum(page.getPage());
		}
	}

	@JsonIgnore
	public User getAuthor() {
		if (getCreatorid() == null) return null;
		if (author == null) author = AppConfig.client().read(getCreatorid());
		return author;
	}

	@JsonIgnore
	public User getLastEditor() {
		if (lasteditby == null) return null;
		if (lastEditor == null) lastEditor = AppConfig.client().read(lasteditby);
		return lastEditor;
	}

	public void restoreRevisionAndUpdate(String revisionid) {
		Revision rev = AppConfig.client().read(revisionid);
		if (rev != null) {
			//copy rev data to post
			setTitle(rev.getTitle());
			setBody(rev.getBody());
			setTags(rev.getTags());

			updateLastActivity();
			setRevisionid(rev.getId());
			//update post
			update();
		}
	}

	public void setComments(List<Comment> comments) {
		this.comments = comments;
	}

	public List<Comment> getComments() {
		return this.comments;
	}

	public List<Comment> getComments(Pager pager) {
		this.comments = AppConfig.client().getChildren(this, Utils.type(Comment.class), pager);
		this.commentcount = pager.getCount();
		this.pagenum = pager.getPage();
		return this.comments;
	}

	public Long getCommentcount() {
		return this.commentcount;
	}

	public void setCommentcount(Long count) {
		this.commentcount = count;
	}

	@JsonIgnore
	public List<Reply> getAnswers(Pager pager) {
		return AppConfig.client().getChildren(this, Utils.type(Reply.class), pager);
	}

	@JsonIgnore
	public List<Revision> getRevisions(Pager pager) {
		return AppConfig.client().getChildren(this, Utils.type(Revision.class), pager);
	}

	@JsonIgnore
	public boolean isReply() {
		return this instanceof Reply;
	}

	@JsonIgnore
	public boolean isQuestion() {
		return this instanceof Question;
	}

	@JsonIgnore
	public boolean isBlackboard() {
		return this instanceof Blackboard;
	}

	@JsonIgnore
	public boolean isFeedback() {
		return this instanceof Feedback;
	}

	@JsonIgnore
	public boolean isGrouppost() {
		return this instanceof Grouppost;
	}

	public void updateLastActivity() {
		setLastactivity(System.currentTimeMillis());
	}

	public String getPostLink(boolean plural, boolean noid, String questionslink, String questionlink, String feedbacklink,
			String grouplink, String grouppostlink, String classeslink, String classlink) {
		Post p = this;
		if (p == null) return "/";
		String ptitle = Utils.noSpaces(p.getTitle(), "-");
		String pid = (noid ? "" : "/"+p.getId()+"/"+ ptitle);
		if (p.isQuestion()) {
			return plural ? questionslink : questionlink + pid;
		} else if (p.isFeedback()) {
			return plural ? feedbacklink : feedbacklink + pid;
		} else if (p.isGrouppost()) {
			return plural ? grouplink+"/"+p.getParentid() : grouppostlink + pid;
		} else if (p.isReply()) {
			Post parentp = AppConfig.client().read(p.getParentid());
			if (parentp != null) {
				return parentp.getPostLink(plural, noid, questionslink, questionlink,
						feedbacklink, grouplink, grouppostlink, classeslink, classlink);
			}
		} else if (p.isBlackboard()) {
			return plural ? classeslink : classlink + (noid ? "" : "/" + p.getParentid());
		}
		return "";
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
		return true;
	}

	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + (this.body != null ? this.body.hashCode() : 0);
		return hash;
	}

	public abstract boolean canHaveChildren();

	public abstract boolean canHaveRevisions();
}
