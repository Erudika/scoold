/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class Message extends BasePage{
	
	public String title;
	public com.erudika.scoold.core.Message showMessage;
	public boolean canEdit;

	public Message() {
		title = lang.get("message.title");
		canEdit = false;
	}

	public void onGet() {
		if(param("id")){
			String id = getParamValue("id");
			showMessage = dao.read(id);
			
			if (showMessage == null || !utils.typesMatch(showMessage)) {
				setRedirect(HOMEPAGE);
			} else {
				boolean isMine = authenticated && showMessage != null &&
						showMessage.getToid().equals(authUser.getId());

				if(isMine || inRole("admin")){
					canEdit = true;
				}
			}
		}
	}
}
