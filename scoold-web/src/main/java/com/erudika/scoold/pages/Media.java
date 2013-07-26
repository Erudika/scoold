/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.para.utils.DAO;

/**
 *
 * @author alexb
 */
public class Media extends BasePage{

	public String title;
	public com.erudika.scoold.core.Media showMedia;
	public boolean canEdit;

	public Media() {
		title = lang.get("media.title");
		canEdit = false;
	}

	public void onGet() {
		if(param("id")){
			String id = getParamValue("id");
			showMedia = dao.read(id);
			
			if(showMedia == null || !utils.typesMatch(showMedia)){
				setRedirect(HOMEPAGE);
			}else{
				boolean isMine = authenticated && showMedia != null &&
						showMedia.getCreatorid().equals(authUser.getId());

				if(isMine || inRole("mod")){
					canEdit = true;
				}
			}
		}
	}
	
	public void onPost(){
		if(param("remove") && param(DAO.CN_PARENTID) && canEdit){
			String mid = getParamValue("remove");
			com.erudika.scoold.core.Media m = new com.erudika.scoold.core.Media();
			m.setParentid(getParamValue(DAO.CN_PARENTID));
			m.setId(mid);
			m.delete();
		}
	}
}
