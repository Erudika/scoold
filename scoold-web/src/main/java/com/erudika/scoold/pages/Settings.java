/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.pages;

import com.erudika.para.core.PObject;
import com.erudika.para.utils.Utils;
import com.erudika.para.utils.Search;
import com.erudika.scoold.core.User;
import com.erudika.scoold.util.Constants;
import java.util.ArrayList;
import java.util.List;
import org.apache.click.control.Form;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

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
		}else if(!Search.findTerm(PObject.classname(User.class), null, null, "email", StringUtils.trim(mail)).isEmpty()){
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
		if (param("deleteaccount") && StringUtils.equals(Utils.MD5(authUser.getId()), getParamValue("key"))) {
			authUser.delete();
			clearSession();
			redirectto = signinlink + "?code=4&success=true";
		}else if(param("favtags")){
			String cleanTags = Utils.fixCSV(getParamValue("favtags"), Constants.MAX_FAV_TAGS);
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

	private void sortIdentifiers(List<String> list){
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
		return  (openidlist.size() >= Constants.MAX_IDENTIFIERS_PER_USER)
				? false : true;
	}
}
