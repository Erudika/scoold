/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import org.apache.commons.lang.math.NumberUtils;

/**
 *
 * @author alexb
 */
public class Media extends BasePage{

	public String title;
	public com.scoold.core.Media showMedia;
	public boolean canEdit;

	public Media() {
		title = lang.get("media.title");
		canEdit = false;
	}

	public void onGet() {
		if(param("id")){
			Long id = NumberUtils.toLong(getParamValue("id"));
			showMedia = com.scoold.core.Media.getMediaDao().read(id);
			
			if(showMedia == null || !daoutils.typesMatch(showMedia)){
				setRedirect(HOMEPAGE);
			}else{
				boolean isMine = authenticated && showMedia != null &&
						showMedia.getUserid().equals(authUser.getId());

				if(isMine || inRole("mod")){
					canEdit = true;
				}
			}
		}
	}
	
	public void onPost(){
		if(param("remove") && param("parentid") && canEdit){
			Long mid = NumberUtils.toLong(getParamValue("remove"));
			com.scoold.core.Media m = new com.scoold.core.Media();
			m.setParentid(NumberUtils.toLong(getParamValue("parentid")));
			m.setId(mid);
			m.delete();
		}
	}
}
