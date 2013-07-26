/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.core;

import com.erudika.para.core.PObject;
import com.erudika.para.utils.Stored;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author alexb
 */
public class Tag extends PObject{
	private static final long serialVersionUID = 1L;

	@Stored private String tag;
	@Stored private Long count;

	public Tag(){
	}
	
	public Tag(String id) {
		setId(id);
		this.count = 1L;
	}
	
	public Long getCount() {
		return count;
	}

	public void setCount(Long count) {
		this.count = count;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public void incrementCount(){
		this.count++;
	}

	public void decrementCount(){
		if(this.count <= 1)
			delete();
		else	
			this.count--;
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
}
