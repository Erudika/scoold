/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import com.scoold.core.Message;
import com.scoold.core.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 *
 * @author alexb
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
			messageslist = Message.getMessages(authUser.getId(), pagenum, itemcount);
			contactslist = User.getUserDao().readAllUsersForID(authUser.getId(), null, null);
			Message.markAllRead(authUser.getId());
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
			Long mid = NumberUtils.toLong(getParamValue("delete"), 0);
			new Message(mid, authUser.getId()).delete();
		}else if(param("deleteall")){
			Message.deleteAll(authUser.getId());
			setRedirect(messageslink);
			return;
		}
	}

}
