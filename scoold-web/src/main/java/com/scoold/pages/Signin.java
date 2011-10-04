/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import com.scoold.util.ScooldAuthModule;

public class Signin extends BasePage{
     
    public String title;
	
    public Signin() {
        title = lang.get("signin.title");
		includeFBscripts = true;
    }
	
	public void	onGet(){
		if (authenticated ) {
			if(param("signout")){
				clearSession();
				if (isFBconnected) {
					setRedirect(signinlink + "?code=5&fblogout=true");
				}else{
					setRedirect(signinlink + "?code=5");
				}
				return;
			}else{
				setRedirect(HOMEPAGE);
				return;
			}
        }
	}
}
