/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

public class Signin extends Base{
     
    public String title;
	
    public Signin() {
        title = lang.get("signin.title");
		includeFBscripts = true;
    }
	
	public void onGet(){
		if (authenticated ) {
			setRedirect(HOMEPAGE);
		}
	}
	
	public void	onPost(){
		if (authenticated ) {
			if(param("signout")){
				clearSession();
				if (authUser.isFacebookUser()) {
					setRedirect(signinlink + "?code=5&success=true&fblogout=true");
				}else{
					setRedirect(signinlink + "?code=5&success=true");
				}
			}else{
				setRedirect(HOMEPAGE);
			}
        }
	}
}
