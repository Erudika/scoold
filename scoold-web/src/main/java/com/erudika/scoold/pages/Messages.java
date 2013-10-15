/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.scoold.core.Message;
import com.erudika.scoold.core.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class Messages extends BasePage{

	public String title;
	public ArrayList<Message> messageslist;
	public ArrayList<User> contactslist;
	
	public Messages(){
		title = lang.get("messages.title");
	}

	public void onGet(){
		if(!isAjaxRequest()){
			messageslist = authUser.getChildren(Message.class, pagenum, itemcount, null, MAX_ITEMS_PER_PAGE);
			contactslist = authUser.getLinkedObjects(User.class, null, null);
			authUser.markAllMessagesAsRead();
		}
	}

	public void onPost(){
		if(param("newmessage")){
			String message = getParamValue("body");
			String[] uids = getContext().getRequestParameterValues("toids");

			if((uids == null || uids.length < 1 || StringUtils.isBlank(message))
					&& !isAjaxRequest()){
				setRedirect(messageslink+"/new");
			}else{
				HashSet<String> ids = new HashSet<String>();
				ids.addAll(Arrays.asList(uids));
				
				Message msg = new Message(ids, authUser.getId(), false, message);
				boolean done = msg.send();
				
				if(!isAjaxRequest()){
					if(done) setRedirect(messageslink+"/new?code=8&success=true");
					else setRedirect(messageslink+"/new");
				}
			}
		}else if(param("delete")){
			String mid = getParamValue("delete");
			new Message(mid, authUser.getId()).delete();
		}else if(param("deleteall")){
			authUser.deleteAllMessages();
			setRedirect(messageslink);
		}
	}

}
