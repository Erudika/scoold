/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.pages;

import com.erudika.para.core.PObject;
import com.erudika.para.utils.Utils;
import com.erudika.para.search.ElasticSearch;
import com.erudika.scoold.core.User;
import com.erudika.scoold.util.Constants;
import java.util.ArrayList;
import java.util.List;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com> 
 */
public class Settings extends BasePage {

	public String title;
	public ArrayList<String> openidlist;
	public boolean canDetachOpenid;
	public boolean canAttachOpenid;

	public Settings() {
		title = lang.get("settings.title");
		includeFBscripts = true;
		sortIdentifiers(authUser.getIdentifiers());
		canDetachOpenid = canDetachOpenid();
		canAttachOpenid = canAttachOpenid();
	}
	
	public void onPost(){
		// check if the checksum is the same as the one in the link
		String redirectto = settingslink;
		if (param("deleteaccount") && StringUtils.equals(Utils.MD5(authUser.getId()), getParamValue("key"))) {
			authUser.delete();
			clearSession();
			redirectto = signinlink + "?code=4&success=true";
		}else if(param("favtags")){
			String cleanTags = Utils.fixCSV(getParamValue("favtags"), Constants.MAX_FAV_TAGS);
			authUser.setFavtags(cleanTags);
			authUser.update();
		}else if (param("detachid") && canDetachOpenid) {
			String ident = getParamValue("detachid");
			if(!StringUtils.isBlank(ident)){
				authUser.detachIdentifier(ident);
				openidlist.remove(ident);
				canAttachOpenid = canAttachOpenid();
				canDetachOpenid = canDetachOpenid();
			}
		}else if(param("email")){
			String newEmail = getParamValue("email");
			authUser.setEmail(newEmail);
			authUser.update();
		}

		if(!isAjaxRequest())
			setRedirect(redirectto);
	}

	private void sortIdentifiers(List<String> list){
		openidlist = new ArrayList<String>();
		if(list != null && list.size() > 0){
			String id1 = authUser.getIdentifier();
			openidlist.add(id1);
			
			for (String ident : list) {
				if(!ident.equals(id1)){
					openidlist.add(ident);
				}
			}
		}
	}

	private boolean canDetachOpenid() {
		//make sure you we don't delete the user's primary openid
		return (openidlist.size() == 1) ? false : true;
	}

	//allow max 2 openids per account besides FB connect
	private boolean canAttachOpenid() {
		return  (openidlist.size() >= Constants.MAX_IDENTIFIERS_PER_USER)
				? false : true;
	}
}
