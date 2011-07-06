/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.core;

import com.scoold.db.AbstractReportDAO;
import com.scoold.db.AbstractDAOFactory;

/**
 *
 * @author alexb
 */
public class Report implements ScooldObject{

	public static enum ReportType{
		SPAM, OFFENSIVE, DUPLICATE, INCORRECT, OTHER;

		public String toString(){
			return super.toString().toLowerCase();
		}
	}

	private Long id;
	private String uuid;
	@Stored private String parentuuid;
	@Stored private String type;
	@Stored private String description;
	@Stored private Long userid;
	@Stored private String classname;
	@Stored private Long timestamp;
	@Stored private String author;
	@Stored private String link;
	@Stored private Long parentid;
	@Stored private String grandparentuuid;
	@Stored private String solution;
	@Stored private Boolean closed;

	private transient static AbstractReportDAO<Report, Long> mydao;

	public static AbstractReportDAO<Report, Long> getReportDAO(){
		return (mydao != null) ? mydao : (AbstractReportDAO<Report, Long>)
				AbstractDAOFactory.getDefaultDAOFactory().getDAO(Report.class);
	}

	public Report() {
		this.closed = false;
	}

	public Report(Long id) {
		this.id = id;
		this.closed = false;
	}

	public Report(String parentuuid, String type, String description,
			Long userid, String classname) {
		this.parentuuid = parentuuid;
		this.type = type;
		this.description = description;
		this.userid = userid;
		this.classname = classname;
		this.closed = false;
	}

	/**
	 * Get the value of closed
	 *
	 * @return the value of closed
	 */
	public Boolean getClosed() {
		return closed;
	}

	/**
	 * Set the value of closed
	 *
	 * @param closed new value of closed
	 */
	public void setClosed(Boolean closed) {
		this.closed = closed;
	}

	/**
	 * Get the value of solution
	 *
	 * @return the value of solution
	 */
	public String getSolution() {
		return solution;
	}

	/**
	 * Set the value of solution
	 *
	 * @param solution new value of solution
	 */
	public void setSolution(String solution) {
		this.solution = solution;
	}

	/**
	 * Get the value of grandparentuuid
	 *
	 * @return the value of grandparentuuid
	 */
	public String getGrandparentuuid() {
		return grandparentuuid;
	}

	/**
	 * Set the value of grandparentuuid
	 *
	 * @param grandparentuuid new value of grandparentuuid
	 */
	public void setGrandparentuuid(String grandparentuuid) {
		this.grandparentuuid = grandparentuuid;
	}

	/**
	 * Get the value of parentid
	 *
	 * @return the value of parentid
	 */
	public Long getParentid() {
		return parentid;
	}

	/**
	 * Set the value of parentid
	 *
	 * @param parentid new value of parentid
	 */
	public void setParentid(Long parentid) {
		this.parentid = parentid;
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
	 * Get the value of author
	 *
	 * @return the value of author
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * Set the value of author
	 *
	 * @param author new value of author
	 */
	public void setAuthor(String author) {
		this.author = author;
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
	 * Get the value of classname
	 *
	 * @return the value of classname
	 */
	public String getClassname() {
		return classname;
	}

	/**
	 * Set the value of classname
	 *
	 * @param classname new value of classname
	 */
	public void setClassname(String classname) {
		this.classname = classname;
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

	public Long getId(){
		return id;
	}

	public void setId(Long id){
		this.id = id;
	}

	public Long create() {
		this.id = getReportDAO().create(this);
		return this.id;
	}

	public void update() {
		getReportDAO().update(this);
	}

	public void delete() {
		getReportDAO().delete(this);
	}

	public ReportType[] getReportTypes(){
		return ReportType.values();
	}

	public void setType(ReportType type){
		if(type != null) this.type = type.name();
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Report other = (Report) obj;
		if ((this.parentuuid == null) ? (other.parentuuid != null) : !this.parentuuid.equals(other.parentuuid)) {
			return false;
		}
		if ((this.type == null) ? (other.type != null) : !this.type.equals(other.type)) {
			return false;
		}
		if (this.userid != other.userid && (this.userid == null || !this.userid.equals(other.userid))) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int hash = 5;
		hash = 37 * hash + (this.parentuuid != null ? this.parentuuid.hashCode() : 0);
		hash = 37 * hash + (this.type != null ? this.type.hashCode() : 0);
		hash = 37 * hash + (this.userid != null ? this.userid.hashCode() : 0);
		return hash;
	}

}
