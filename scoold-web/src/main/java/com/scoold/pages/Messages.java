/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import com.scoold.core.Message;
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

	public Messages(){
		title = lang.get("messages.title");
	}

	public void onGet(){
		if(!isAjaxRequest()){
			messageslist = Message.getMessages(authUser.getUuid(), pagenum, itemcount);
			Message.markAllRead(authUser.getUuid());
		}
	}

	public void onPost(){
		if(param("newmessage")){
			String message = getParamValue("body");
			String[] uids = getContext().getRequestParameterValues("touuids");

			if((uids == null || uids.length < 1 || StringUtils.isBlank(message))
					&& !isAjaxRequest()){
				setRedirect(messageslink+"/new?code=7&error=true");
			}else{
				HashSet<String> uuids = new HashSet<String>();
				uuids.addAll(Arrays.asList(uids));
				
				Message msg = new Message(uuids, authUser.getId(), false, message);
				boolean done = msg.send();
				
				if(!isAjaxRequest()){
					if(done) setRedirect(messageslink+"/new?code=8&success=true");
					else setRedirect(messageslink+"/new?code=7&error=true");
				}
			}
		}else if(param("delete")){
			Long mid = NumberUtils.toLong(getParamValue("delete"), 0);
			new Message(mid).delete();
		}else if(param("deleteall")){
			Message.deleteAll(authUser.getUuid());
			setRedirect(messageslink);
			return;
		}
	}

}
