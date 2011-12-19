/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.core;

import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;


/**
 *
 * @author alexb
 */
public class ContactDetail {

    private String type;
    private String value;

	public static final String SEPARATOR = ";";

	public static enum ContactDetailType{
		UNKNOWN, WEBSITE, ADDRESS, FACEBOOK, TWITTER, SKYPE,
		MSN, AIM, GTALK, QQ, YAHOO, ICQ, EBUDDY;

		public String toString(){
			switch (this) {
				case WEBSITE: return "Website";
				case ADDRESS: return "Address";
				case FACEBOOK: return "Facebook";
				case TWITTER: return "Twitter";
				case SKYPE: return "Skype";
				case MSN: return "Live Messenger";
				case AIM: return "AOL IM";
				case GTALK: return "Google Talk";
				case QQ: return "Tencent QQ";
				case YAHOO: return "Yahoo!";
				case ICQ: return "ICQ";
				case EBUDDY: return "eBuddy";
			}
			return UNKNOWN.toString().toLowerCase();
		}
	};

	public ContactDetail() {
		this(ContactDetailType.UNKNOWN.toString(), "");
    }

	public ContactDetail(String type, String value) {
		this.setType(type);
		this.value = value;
	}

    /**
     * Get the value of value
     *
     * @return the value of value
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the value of value
     *
     * @param value new value of value
     */
    public void setValue(String value) {
        this.value = getContactDetailType(value).toString();
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
        this.type = getContactDetailType(type).name();
    }

	public static ArrayList<ContactDetail> toContactsList(String details){
		ArrayList<ContactDetail> list = new ArrayList<ContactDetail>();
		if(StringUtils.isBlank(details)) return list;
		for (String detail : details.split(SEPARATOR)) {
			String[] twoParts = detail.split(",");
			if(twoParts.length != 2) continue;
			list.add(new ContactDetail(twoParts[0], twoParts[1]));
		}
		return list;
	}

	private ContactDetailType getContactDetailType(String type){
		if(type == null) return ContactDetailType.UNKNOWN;
		try{
			return ContactDetailType.valueOf(type.toUpperCase());
        }catch(IllegalArgumentException e){
            //oh shit!
			return ContactDetailType.UNKNOWN;
        }
	}

	public String toString() {
		if(this.type == null) this.type = ContactDetailType.UNKNOWN.name();
		if(this.value == null) return null;
		return type+","+value;
	}

    public boolean equals(Object obj) {
        if(this == obj)
                return true;
        if((obj == null) || (obj.getClass() != this.getClass()))
                return false;
        ContactDetail other = (ContactDetail) obj;

        return (this.value.equals(other.getValue()) &&
				this.type.equals(other.getType()));
    }

    public int hashCode() {
        int hash = 3;
        hash = 19 * hash + (this.value != null ? this.value.hashCode() : 0);
        return hash;
    }

}
