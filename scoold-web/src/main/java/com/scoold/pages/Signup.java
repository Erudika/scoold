/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages;

import com.scoold.core.User;
import com.scoold.core.User.Badge;
import com.scoold.core.User.UserGroup;
import com.scoold.core.User.UserType;
import com.scoold.util.ScooldAuthModule;
import org.apache.click.control.Field;
import org.apache.click.control.Form;
import org.apache.click.control.Radio;
import org.apache.click.control.RadioGroup;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 *
 * @author alexb
 */
public class Signup extends BasePage{

    public String title;
    public Form signupForm;
	public boolean isFacebookUser;
	public String identString;
	private User newUser;
	private static final String ADMIN_KEY = "albogdano.pip.verisignlabs.com";
	private static final String ADMIN_FB_ID = "517966023";
    
    public Signup() {
        title = lang.get("signup.title");
		identString = (String) seshun.getAttribute(ScooldAuthModule.IDENTIFIER);
		isFacebookUser = NumberUtils.isDigits(identString);
		includeFBscripts = isFacebookUser;
		makeForm();

		if (authenticated) {
			setRedirect(HOMEPAGE);
			return;
		}else if(identString == null) {
            setRedirect(signinlink);
			return;
        }

    }
    
    private void makeForm(){
		newUser = (User) seshun.getAttribute(ScooldAuthModule.NEW_USER);
		if(newUser == null) newUser = new User();
		String name = newUser.getFullname();
		String thisisme = (String) seshun.getAttribute(ScooldAuthModule.THIS_IS_ME);
		if(StringUtils.length(name) < StringUtils.length(thisisme) &&
				!StringUtils.isBlank(thisisme) && !thisisme.matches("^\\d+$")){
			name = thisisme;
		}
		signupForm = new Form();

		TextField identifier = new TextField("identifier", true);
		identifier.setValue(identString);
		identifier.setLabel(lang.get("openid.title"));
		identifier.setReadonly(true);

        TextField fullname = new TextField("fullname", true);
        fullname.setValue(name);
        fullname.setLabel(lang.get("signup.form.myname"));

        TextField email = new TextField("email", true);
        email.setValue(newUser.getEmail());
        email.setLabel(lang.get("signup.form.email"));

		TextField hideme = new TextField("additional", false);
		hideme.setLabel("Leave blank!");
		hideme.setAttribute("class", "hide");

        Submit s = new Submit("signup", lang.get("signin.title"),
				this, "onSignupClick");
        s.setAttribute("class", "button rounded3");
		s.setId("signup-btn");

		RadioGroup radioGroup = new RadioGroup("type", true);
        radioGroup.add(new Radio(UserType.STUDENT.toString(), lang.get("student")));
        radioGroup.add(new Radio(UserType.ALUMNUS.toString(), lang.get("alumnus")));
        radioGroup.add(new Radio(UserType.TEACHER.toString(), lang.get("teacher")));
        radioGroup.setValue(UserType.STUDENT.toString());
        radioGroup.setVerticalLayout(false);
		radioGroup.setLabel(lang.get("signup.form.youare"));
        
		signupForm.add(identifier);
        signupForm.add(fullname);
        signupForm.add(email);
		signupForm.add(radioGroup);
		signupForm.add(hideme);
        signupForm.add(s);
    }
    
    public boolean onSignupClick() {		
		Field name = signupForm.getField("fullname");
        Field email = signupForm.getField("email");
        Field additional = signupForm.getField("additional");
        Field ident = signupForm.getField("identifier");
		String mail = email.getValue();

		if(mail == null || mail.contains("<") || mail.contains(">") || mail.contains("\\") ||
				!(mail.indexOf(".") > 2) && (mail.indexOf("@") > 0)){
			email.setError(lang.get("signup.form.error.email"));
		}		
        if(User.exists(StringUtils.trim(email.getValue()))){
            //Email is claimed => user exists!
            email.setError(lang.get("signup.form.error.emailexists"));
        }        
		if (!StringUtils.isBlank(additional.getValue())){
			signupForm.setError("You are not supposed to do that!");
		}

        if(signupForm.isValid()){
			if(newUser == null) newUser = new User();
			if(StringUtils.isBlank(identString)){
				setRedirect(signinlink + "?code=2&error=true");
			}else{
				newUser.setActive(true);
				newUser.setEmail(getParamValue("email"));
				newUser.setFullname(getParamValue("fullname"));
				newUser.setTypeString(getParamValue("type"));				
				newUser.setIdentifier(identString);

				if (identString.contains(ADMIN_KEY) || identString.equals(ADMIN_FB_ID)){
					newUser.setGroups(UserGroup.ADMINS.toString());
				}
				
				if(IN_BETA){
					newUser.addBadge(Badge.TESTER);
				}

				Long uid = newUser.create();

				if (uid != null) {
					setRedirect(ScooldAuthModule.OPENID_ACTION + "?" +
							ScooldAuthModule.SIGNUP_SUCCESS + "=true");
				} else {
					setRedirect(signinlink + "?code=2&error=true");
				}
			}
		}

		return false;
    }

	public boolean onSecurityCheck() {
        return signupForm.onSubmitCheck(this, signinlink+"?code=2&error=true");
    }

}
