/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.pages;

import com.erudika.scoold.core.User;
import com.erudika.scoold.core.User.Badge;
import com.erudika.scoold.core.User.UserType;
import org.apache.click.control.Field;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Radio;
import org.apache.click.control.RadioGroup;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class Signup extends BasePage{

    public String title;
    public Form signupForm;
	public boolean isFacebookUser;
	public String identString;
	public User newUser;
    
    public Signup() {
//        title = lang.get("signup.title");
//		identString = getStateParam(ScooldAuthModule.IDENTIFIER);
//		isFacebookUser = NumberUtils.isDigits(identString);
//		includeFBscripts = isFacebookUser;
//		makeForm();

		setRedirect(HOMEPAGE);
		
//		if (authenticated) {
//			setRedirect(HOMEPAGE);
//		}else if(identString == null) {
//            setRedirect(signinlink);
//        }else if(User.exists(StringUtils.trim(identString))){
//            //RARE CASE! Identifier is claimed => user exists! 
//			ScooldAuthModule.clearAuthCookie(req, resp);
//			setRedirect(signinlink + "?code=2&error=true");
//        }
    }
    
//    private void makeForm(){
//		newUser = new User();
//		String n = getStateParam(ScooldAuthModule.NEW_USER_NAME);
//		String e = getStateParam(ScooldAuthModule.NEW_USER_EMAIL);
//		if(!StringUtils.isBlank(n)) newUser.setFullname(n);
//		if(!StringUtils.isBlank(e)) newUser.setEmail(e);
//			
//		String name = newUser.getName();
//		String thisisme = getStateParam(ScooldAuthModule.THIS_IS_ME);
//		if(StringUtils.length(name) < StringUtils.length(thisisme) &&
//				!StringUtils.isBlank(thisisme) && !thisisme.matches("^\\d+$")){
//			name = thisisme;
//		}
//		signupForm = new Form();
//
//		HiddenField identifier = new HiddenField("identifier", identString);
//		identifier.setRequired(true);
//
//        TextField name = new TextField("name", true);
//        name.setValue(name);
//		name.setMinLength(4);
//		name.setMaxLength(255);
//        name.setLabel(lang.get("signup.form.myname"));
//
//        TextField email = new TextField("email", true);
//        email.setValue(newUser.getEmail());
//        email.setLabel(lang.get("signup.form.email"));
//
//		TextField hideme = new TextField("additional", false);
//		hideme.setLabel("Leave blank!");
//		hideme.setAttribute("class", "hide");
//
//        Submit s = new Submit("signup", lang.get("signin.title"),
//				this, "onSignupClick");
//        s.setAttribute("class", "button rounded3");
//		s.setId("signup-btn");
//
//		RadioGroup radioGroup = new RadioGroup("type", true);
//        radioGroup.add(new Radio(UserType.STUDENT.toString(), lang.get("student")));
//        radioGroup.add(new Radio(UserType.ALUMNUS.toString(), lang.get("alumnus")));
//        radioGroup.add(new Radio(UserType.TEACHER.toString(), lang.get("teacher")));
//        radioGroup.setValue(UserType.STUDENT.toString());
//        radioGroup.setVerticalLayout(false);
//		radioGroup.setLabel(lang.get("signup.form.iama"));
//        
//		signupForm.add(identifier);
//        signupForm.add(name);
//        signupForm.add(email);
//		signupForm.add(radioGroup);
//		signupForm.add(hideme);
//        signupForm.add(s);
//    }
//    
//    public boolean onSignupClick() {		
//        Field email = signupForm.getField("email");
//        Field additional = signupForm.getField("additional");
//		String mail = email.getValue();
//
//		
//		if(mail == null || mail.contains("<") || mail.contains(">") || mail.contains("\\") ||
//				!(mail.indexOf(".") > 2) && (mail.indexOf("@") > 0)){
//			email.setError(lang.get("signup.form.error.email"));
//		}		
//		
//		if(!utils.findTerm(User.getClassname(), null, null, "email", 
//				StringUtils.trim(mail)).isEmpty()){
//			email.setError(lang.get("signup.form.error.emailexists"));
//		}
//		
//		if (!StringUtils.isBlank(additional.getValue())){
//			signupForm.setError("You are not supposed to do that!");
//		}
//
//        if(signupForm.isValid()){
//			if(newUser == null) newUser = new User();
//			if(StringUtils.isBlank(identString)){
//				setRedirect(signinlink + "?code=2&error=true");
//			}else{
//				newUser.setActive(true);
//				newUser.setEmail(getParamValue("email"));
//				newUser.setFullname(getParamValue("name"));
//				newUser.setType(getParamValue("type"));				
//				newUser.setIdentifier(identString);
//
//				if (identString.equals(ScooldAuthModule.ADMIN_FB_ID)){
//					newUser.setGroups(Groups.ADMINS.toString());
//				}
//				
//				if(IN_BETA){
//					newUser.addBadge(Badge.TESTER);
//				}
//
//				Long uid = newUser.create();
//
//				if (uid != null) {
//					setRedirect(ScooldAuthModule.OPENID_ACTION + "?" +
//							ScooldAuthModule.SIGNUP_SUCCESS + "=true");
//				} else {
//					setRedirect(signinlink + "?code=2&error=true");
//				}
//			}
//		}
//
//		return false;
//    }
//
//	public boolean onSecurityCheck() {
//        return signupForm.onSubmitCheck(this, signinlink+"?code=2&error=true");
//    }

}
