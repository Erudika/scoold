/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.core;

import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractTranslationDAO;
import java.io.Serializable;

/**
 *
 * @author alexb
 */
public class Translation implements Votable<Long>, ScooldObject, Serializable{
	private static final long serialVersionUID = 1L;

	private Long id;
	@Stored private String locale;
	@Stored private String key;
	@Stored private String value;
	@Stored private Integer votes;
	@Stored private Long userid;
	@Stored private Long timestamp;
	@Stored private Integer oldvotes;
	@Stored public static final String classtype = Translation.class.getSimpleName().toLowerCase();

	private transient User author;

	private transient static AbstractTranslationDAO<Translation, Long> mydao;

	public static AbstractTranslationDAO<Translation, Long> getTranslationDao(){
		return (mydao != null) ? mydao : (AbstractTranslationDAO<Translation, Long>)
				AbstractDAOFactory.getDefaultDAOFactory().getDAO(Translation.class);
	}

	public Translation() {
		this(null, null, null);
	}

	public Translation(Long id) {
		this();
		this.id = id;
	}

	public Translation(String locale, String key, String value) {
		this.locale = locale;
		this.key = key;
		this.value = value;
		this.votes = 0;
	}
	
	public String getClasstype() {
		return classtype;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Integer getVotes() {
		return votes;
	}

	public void setVotes(Integer votes) {
		setOldvotes(this.votes);
		this.votes = votes;
	}

	public Integer getOldvotes() {
		return oldvotes;
	}

	public void setOldvotes(Integer oldvotes) {
		this.oldvotes = oldvotes;
	}

	public Long getUserid() {
		return userid;
	}

	public void setUserid(Long userid){
		this.userid = userid;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public User getAuthor(){
		if(userid == null) return null;
		if(author == null) author = User.getUser(userid);
		return author;
	}

	public void approve(){
		getTranslationDao().approve(this);
	}

	public void disapprove(){
		getTranslationDao().disapprove(this);
	}

	public boolean voteUp(Long userid) {
		return AbstractDAOFactory.getDefaultDAOFactory().getDAOUtils().voteUp(userid, this);
	}

	public boolean voteDown(Long userid) {
		return AbstractDAOFactory.getDefaultDAOFactory().getDAOUtils().voteDown(userid, this);
	}

	public Long create() {
		this.id = getTranslationDao().create(this);
		return this.id;
	}

	public void update() {
		getTranslationDao().update(this);
	}

	public void delete() {
		getTranslationDao().delete(this);
	}

}
