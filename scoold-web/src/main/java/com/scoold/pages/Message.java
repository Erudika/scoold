/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 *
 * @author alexb
 */
public class Message extends BasePage{
	
	public String title;
	public com.scoold.core.Message showMessage;
	public boolean canEdit;

	public Message() {
		title = lang.get("message.title");
		canEdit = false;
	}

	public void onGet() {
		if(param("id")){
			Long id = NumberUtils.toLong(getParamValue("id"));
			showMessage = com.scoold.core.Message.getMessageDao().read(id);

			boolean isMine = authenticated && showMessage != null &&
					StringUtils.equals(showMessage.getTouuid(), authUser.getUuid());

			if(isMine || inRole("admin")){
				canEdit = true;
			}
		}
	}
}
