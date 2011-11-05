/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

public class Signin extends BasePage{
     
    public String title;
	
    public Signin() {
        title = lang.get("signin.title");
		includeFBscripts = true;
    }
	
	public void	onPost(){
		if (authenticated ) {
			if(param("signout")){
				clearSession();
				if (isFBconnected) {
					setRedirect(signinlink + "?code=5&success=true&fblogout=true");
				}else{
					setRedirect(signinlink + "?code=5&success=true");
				}
				return;
			}else{
				setRedirect(HOMEPAGE);
				return;
			}
        }
	}
}
