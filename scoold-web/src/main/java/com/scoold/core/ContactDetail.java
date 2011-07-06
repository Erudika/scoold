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
		MSN, AIM, GTALK, QQ, YAHOO, ICQ, XFIRE, EBUDDY;

		public String toString(){
			switch(this){
				case UNKNOWN: return "Unknown";
				case ADDRESS: return "Address";
				case XFIRE: return "Xfire";
				case QQ: return "Tencent QQ";
				case FACEBOOK: return "Facebook";
				case TWITTER: return "Twitter";
				case EBUDDY: return "eBuddy";
				case WEBSITE: return "Website";
				case SKYPE: return "Skype";
				case MSN: return "Windows Live";
				case AIM: return "AOL IM";
				case YAHOO: return "Yahoo! Messenger";
				case GTALK: return "Google Talk";
				case ICQ: return "ICQ";
				default: return UNKNOWN.toString();
			}
		}
	};


	public ContactDetail() {
		this.value = "";
    }

	public ContactDetail(ContactDetailType type, String value) {
		this.type = type.name();
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
        this.value = value;
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

	public static ArrayList<ContactDetail> toContactsList(String details){
		ArrayList<ContactDetail> list = new ArrayList<ContactDetail>();
		if(StringUtils.isBlank(details)) return list;
		for (String detail : details.split(SEPARATOR)) {
			String[] twoParts = detail.split(",");
			if(twoParts.length != 2) continue;

			ContactDetailType cdt;
			try {
				cdt = ContactDetailType.valueOf(twoParts[0]);
			} catch (Exception e) {
				cdt = ContactDetailType.UNKNOWN;
			}

			list.add(new ContactDetail(cdt, twoParts[1]));
		}
		return list;
	}

	public String getTypeString(){
		if(type == null) return ContactDetailType.UNKNOWN.toString();
		return ContactDetailType.valueOf(type.toUpperCase()).toString();
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
