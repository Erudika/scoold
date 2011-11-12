/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.core;

import com.scoold.db.AbstractTagDAO;
import com.scoold.db.AbstractDAOFactory;
import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author alexb
 */
public class Tag implements ScooldObject, Searchable<Tag>{

	private Long id;
	private String uuid;
	@Indexed
	private String tag;
	@Stored private Long count;
	@Stored private Long timestamp;

	private transient static AbstractTagDAO<Tag, Long> mydao;

	public static AbstractTagDAO<Tag, Long> getTagDao(){
		return (mydao != null) ? mydao : (AbstractTagDAO<Tag, Long>)
				AbstractDAOFactory.getDefaultDAOFactory().getDAO(Tag.class);
	}

	public Tag(Long id) {
		this.id = id;
	}

	public Tag(String tag) {
		this.tag = tag;
		this.count = 1L;
	}

	public Tag(){
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
	 * Get the value of count
	 *
	 * @return the value of count
	 */
	public Long getCount() {
		return count;
	}

	/**
	 * Set the value of count
	 *
	 * @param count new value of count
	 */
	public void setCount(Long count) {
		this.count = count;
	}


	/**
	 * Get the value of tag
	 *
	 * @return the value of tag
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * Set the value of tag
	 *
	 * @param tag new value of tag
	 */
	public void setTag(String tag) {
		this.tag = tag;
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


	public Long getId(){
		return id;
	}

	public void setId(Long id){
		this.id = id;
	}

	public void incrementCount(){
		this.count++;
	}

	public void decrementCount(){
		if(this.count == 1)
			delete();
		else	
			this.count--;
	}
	
	public Long create() {
		this.id = getTagDao().create(this);
		return this.id;
	}

	public void update() {
		getTagDao().update(this);
	}

	public void delete() {
		getTagDao().delete(this);
	}

	public static boolean isValidTagString(String tags){
		return !StringUtils.isBlank(tags) && tags.startsWith(",") && tags.endsWith(",");
	}


	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Tag other = (Tag) obj;
		if ((this.tag == null) ? (other.tag != null) : !this.tag.equalsIgnoreCase(other.tag)) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int hash = 3;
		hash = 97 * hash + (this.tag != null ? this.tag.hashCode() : 0);
		return hash;
	}

	public void index() {
		Search.index(this);
	}

	public void unindex() {
		Search.unindex(this);
	}

	public ArrayList<Tag> readAllForKeys(ArrayList<String> keys) {
		if(keys == null || keys.isEmpty()) return new ArrayList<Tag> ();
		return getTagDao().readAllForKeys(keys);
	}
}
