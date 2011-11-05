/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scoold.pages;

import com.scoold.core.User;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractDAOUtils;
import java.util.ArrayList;
import java.util.HashSet;
import org.apache.click.control.Form;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 *
 * @author alexb 
 */
public class Settings extends BasePage {

	public String title;
	public ArrayList<String> openidlist;
	public Form changeEmailForm;
	public boolean canDetachOpenid;
	public boolean canAttachOpenid;

	// keeps track of imported photos to prevent duplicates
	private HashSet<String> photoIDs = new HashSet<String>();

	public Settings() {
		title = lang.get("settings.title");
		includeFBscripts = true;
		sortIdentifiers(authUser.getIdentifiers());
		canDetachOpenid = canDetachOpenid();
		canAttachOpenid = canAttachOpenid();
		changeEmailForm = new Form();

		TextField email = new TextField("email", true);
		email.setMinLength(5);
		email.setMaxLength(250);
		email.setLabel("");
		email.setValue(authUser.getEmail());

		Submit s = new Submit("save", lang.get("save"), this, "onSaveClick");
		s.setAttribute("class", "button rounded3");
		s.setId("change-email-btn");

		changeEmailForm.add(email);
		changeEmailForm.add(s);
	}

	public boolean onSaveClick() {
		String mail = changeEmailForm.getFieldValue("email");
		if(mail == null || mail.contains("<") || mail.contains(">") || mail.contains("\\") ||
				!(mail.indexOf(".") > 2) && (mail.indexOf("@") > 0)){
			changeEmailForm.getField("email").setError("signup.form.error.email");
		}else if(User.exists(StringUtils.trim(mail))){
            //Email is claimed => user exists!
            changeEmailForm.getField("email").setError(lang.get("signup.form.error.emailexists"));
        }
		if (changeEmailForm.isValid()) {
			authUser.setEmail(getParamValue("email"));
			authUser.update();
			changeEmailForm.clearValues();
			setRedirect(settingslink);
		}
		return false;
	}

	public void onPost(){
		// check if the checksum is the same as the one in the link
		String redirectto = settingslink;
		if (param("deleteaccount") && StringUtils.equals(
				AbstractDAOUtils.MD5(authUser.getId().toString()), 
				getParamValue("key"))) {

			authUser.delete();
			clearSession();
			redirectto = signinlink + "?code=4&success=true";
		}else if(param("favtags") && !StringUtils.isBlank(getParamValue("favtags"))){
			String cleanTags = AbstractDAOUtils.fixCSV(getParamValue("favtags"),
					AbstractDAOFactory.MAX_FAV_TAGS);
			authUser.setFavtags(cleanTags);
			authUser.update();
		}else if (param("detachid") && canDetachOpenid) {
			int oid = NumberUtils.toInt(getParamValue("detachid"), 10);

			if (oid == 0 || oid == 1) {
				authUser.detachIdentifier(openidlist.get(oid));
				openidlist.remove(oid);
				canAttachOpenid = canAttachOpenid();
				canDetachOpenid = canDetachOpenid();
			}
		}

		if(!isAjaxRequest())
			setRedirect(redirectto);
	}

	private void sortIdentifiers(ArrayList<String> list){
		openidlist = new ArrayList<String>();
		if(list != null && list.size() > 0){
			String id1 = authUser.getIdentifier();
			if(list.contains(id1)){
				openidlist.add(id1);
			}else{
				openidlist.add(list.get(0));
			}

			if(list.size() > 1){
				list.remove(id1);
				String id2 = list.get(0);
				openidlist.add(id2);
			}
		}
	}

	private boolean canDetachOpenid() {
		//make sure you we don't delete the user's primary openid
		return (openidlist.size() == 1) ? false : true;
	}

	//allow max 2 openids per account besides FB connect
	private boolean canAttachOpenid() {
		return  (openidlist.size() >= AbstractDAOFactory.MAX_IDENTIFIERS_PER_USER)
				? false : true;
	}
}
