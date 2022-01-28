/*
 * Copyright 2013-2021 Erudika. https://erudika.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For issues and patches go to: https://github.com/erudika
 */
package com.erudika.scoold.controllers;

import com.erudika.para.core.annotations.Email;
import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.User;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Utils;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
import static com.erudika.scoold.ScooldServer.MIN_PASS_LENGTH;
import static com.erudika.scoold.ScooldServer.MIN_PASS_STRENGTH;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.utils.HttpUtils;
import com.erudika.scoold.utils.ScooldUtils;
import java.util.Collections;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import static com.erudika.scoold.utils.HttpUtils.getBackToUrl;
import static com.erudika.scoold.utils.HttpUtils.setAuthCookie;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class SigninController {

	private static final Logger logger = LoggerFactory.getLogger(SigninController.class);

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public SigninController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping("/signin")
	public String get(@RequestParam(name = "returnto", required = false, defaultValue = HOMEPAGE) String returnto,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		if (utils.isAuthenticated(req)) {
			return "redirect:" + (StringUtils.startsWithIgnoreCase(returnto, SIGNINLINK) ? HOMEPAGE : getBackToUrl(req));
		}
		if (!HOMEPAGE.equals(returnto) && !SIGNINLINK.equals(returnto)) {
			HttpUtils.setStateParam("returnto", Utils.urlEncode(getBackToUrl(req)), req, res);
		} else {
			HttpUtils.removeStateParam("returnto", req, res);
		}
		if (Config.getConfigBoolean("redirect_signin_to_idp", false) && !"5".equals(req.getParameter("code"))) {
			return "redirect:" + utils.getFirstConfiguredLoginURL();
		}
		model.addAttribute("path", "signin.vm");
		model.addAttribute("title", utils.getLang(req).get("signin.title"));
		model.addAttribute("signinSelected", "navbtn-hover");
		model.addAttribute("fbLoginEnabled", !Config.FB_APP_ID.isEmpty());
		model.addAttribute("gpLoginEnabled", !Config.GPLUS_APP_ID.isEmpty());
		model.addAttribute("ghLoginEnabled", !Config.GITHUB_APP_ID.isEmpty());
		model.addAttribute("inLoginEnabled", !Config.LINKEDIN_APP_ID.isEmpty());
		model.addAttribute("twLoginEnabled", !Config.TWITTER_APP_ID.isEmpty());
		model.addAttribute("msLoginEnabled", !Config.MICROSOFT_APP_ID.isEmpty());
		model.addAttribute("slLoginEnabled", !Config.SLACK_APP_ID.isEmpty());
		model.addAttribute("azLoginEnabled", !Config.AMAZON_APP_ID.isEmpty());
		model.addAttribute("oa2LoginEnabled", !Config.getConfigParam("oa2_app_id", "").isEmpty());
		model.addAttribute("oa2secondLoginEnabled", !Config.getConfigParam("oa2second_app_id", "").isEmpty());
		model.addAttribute("oa2thirdLoginEnabled", !Config.getConfigParam("oa2third_app_id", "").isEmpty());
		model.addAttribute("ldapLoginEnabled", !Config.getConfigParam("security.ldap.server_url", "").isEmpty());
		model.addAttribute("passwordLoginEnabled", Config.getConfigBoolean("password_auth_enabled", true));
		model.addAttribute("oa2LoginProvider", Config.getConfigParam("security.oauth.provider",
				"Continue with OpenID Connect"));
		model.addAttribute("oa2secondLoginProvider", Config.getConfigParam("security.oauthsecond.provider",
				"Continue with OpenID Connect 2"));
		model.addAttribute("oa2thirdLoginProvider", Config.getConfigParam("security.oauththird.provider",
				"Continue with OpenID Connect 3"));
		model.addAttribute("ldapLoginProvider", Config.getConfigParam("security.ldap.provider", "Continue with LDAP"));
		return "base";
	}

	@PostMapping(path = "/signin", params = {"access_token", "provider"})
	public String signinPost(@RequestParam("access_token") String accessToken, @RequestParam("provider") String provider,
			HttpServletRequest req, HttpServletResponse res) {
		return getAuth(provider, accessToken, req, res);
	}

	@GetMapping("/signin/success")
	public String signinSuccess(@RequestParam String jwt, HttpServletRequest req, HttpServletResponse res, Model model) {
		if (!StringUtils.isBlank(jwt)) {
			loginWithIdToken(jwt, req, res);
		} else {
			return "redirect:" + SIGNINLINK + "?code=3&error=true";
		}
		return "redirect:" + getBackToUrl(req);
	}

	@GetMapping(path = "/signin/register")
	public String register(@RequestParam(name = "verify", required = false, defaultValue = "false") Boolean verify,
			@RequestParam(name = "resend", required = false, defaultValue = "false") Boolean resend,
			@RequestParam(name = "id", required = false) String id,
			@RequestParam(name = "token", required = false) String token,
			HttpServletRequest req, Model model) {
		if (utils.isAuthenticated(req)) {
			return "redirect:" + HOMEPAGE;
		}
		model.addAttribute("path", "signin.vm");
		model.addAttribute("title", utils.getLang(req).get("signup.title"));
		model.addAttribute("signinSelected", "navbtn-hover");
		model.addAttribute("emailPattern", Email.EMAIL_PATTERN);
		model.addAttribute("register", true);
		model.addAttribute("verify", verify);
		model.addAttribute("resend", resend);
		model.addAttribute("bademail", req.getParameter("email"));
		model.addAttribute("nosmtp", StringUtils.isBlank(Config.getConfigParam("mail.host", "")));
		model.addAttribute("captchakey", Config.getConfigParam("signup_captcha_site_key", ""));
		if (id != null && token != null) {
			User u = (User) pc.read(id);
			boolean verified = activateWithEmailToken(u, token);
			if (verified) {
				model.addAttribute("verified", verified);
				model.addAttribute("verifiedEmail", u.getEmail());
			} else {
				return "redirect:" + SIGNINLINK;
			}
		}
		return "base";
	}

	@PostMapping("/signin/register")
	public String signup(@RequestParam String name, @RequestParam String email, @RequestParam String passw,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		boolean approvedDomain = utils.isEmailDomainApproved(email);
		if (!utils.isAuthenticated(req) && approvedDomain &&
				HttpUtils.isValidCaptcha(req.getParameter("g-recaptcha-response"))) {
			boolean goodPass = isPasswordStrongEnough(passw);
			if (!isEmailRegistered(email) && isSubmittedByHuman(req) && goodPass) {
				User u = pc.signIn("password", email + ":" + name + ":" + passw, false);
				if (u != null && u.getActive()) {
					setAuthCookie(u.getPassword(), req, res);
					return "redirect:" + getBackToUrl(req);
				} else {
					verifyEmailIfNecessary(name, email, req);
				}
			} else {
				model.addAttribute("path", "signin.vm");
				model.addAttribute("title", utils.getLang(req).get("signup.title"));
				model.addAttribute("signinSelected", "navbtn-hover");
				model.addAttribute("register", true);
				model.addAttribute("name", name);
				model.addAttribute("bademail", email);
				model.addAttribute("emailPattern", Email.EMAIL_PATTERN);
				if (!goodPass) {
					model.addAttribute("error", Collections.singletonMap("passw", utils.getLang(req).get("msgcode.8")));
				} else {
					model.addAttribute("error", Collections.singletonMap("email", utils.getLang(req).get("msgcode.1")));
				}
				return "base";
			}
		}
		return "redirect:" + SIGNINLINK + (approvedDomain ? "/register?verify=true" : "?code=3&error=true");
	}

	@PostMapping("/signin/register/resend")
	public String resend(@RequestParam String email, HttpServletRequest req, HttpServletResponse res, Model model) {
		if (!utils.isAuthenticated(req) && HttpUtils.isValidCaptcha(req.getParameter("g-recaptcha-response"))) {
			Sysprop ident = pc.read(email);
			// confirmation emails can be resent once every 6h
			if (ident != null && !StringUtils.isBlank((String) ident.getProperty(Config._EMAIL_TOKEN))) {
				if (!ident.hasProperty("confirmationTimestamp") || Utils.timestamp() >
					((long) ident.getProperty("confirmationTimestamp") + TimeUnit.HOURS.toMillis(6))) {
					User u = pc.read(Utils.type(User.class), ident.getCreatorid());
					if (u != null && !u.getActive()) {
						utils.sendVerificationEmail(ident, req);
					}
				} else {
					logger.warn("Failed to send email confirmation to '{}' - this can only be done once every 6h.", email);
				}
			} else {
				logger.warn("Failed to send email confirmation to '{}' - user has not signed in with that email yet.", email);
			}
		}
		return "redirect:" + SIGNINLINK + "/register?verify=true";
	}

	@GetMapping(path = "/signin/iforgot")
	public String iforgot(@RequestParam(name = "verify", required = false, defaultValue = "false") Boolean verify,
			@RequestParam(name = "email", required = false) String email,
			@RequestParam(name = "token", required = false) String token,
			HttpServletRequest req, Model model) {
		if (utils.isAuthenticated(req)) {
			return "redirect:" + HOMEPAGE;
		}
		model.addAttribute("path", "signin.vm");
		model.addAttribute("title", utils.getLang(req).get("iforgot.title"));
		model.addAttribute("signinSelected", "navbtn-hover");
		model.addAttribute("iforgot", true);
		model.addAttribute("verify", verify);
		model.addAttribute("nosmtp", StringUtils.isBlank(Config.getConfigParam("mail.host", "")));
		model.addAttribute("captchakey", Config.getConfigParam("signup_captcha_site_key", ""));
		if (email != null && token != null) {
			model.addAttribute("email", email);
			model.addAttribute("token", token);
		}
		return "base";
	}

	@PostMapping("/signin/iforgot")
	public String changePass(@RequestParam String email,
			@RequestParam(required = false) String newpassword,
			@RequestParam(required = false) String token,
			HttpServletRequest req, Model model) {
		boolean approvedDomain = utils.isEmailDomainApproved(email);
		if (!utils.isAuthenticated(req) && approvedDomain &&
				HttpUtils.isValidCaptcha(req.getParameter("g-recaptcha-response"))) {
			if (StringUtils.isBlank(token)) {
				generatePasswordResetToken(email, req);
				return "redirect:" + SIGNINLINK + "/iforgot?verify=true";
			} else {
				boolean error = !resetPassword(email, newpassword, token);
				model.addAttribute("path", "signin.vm");
				model.addAttribute("title", utils.getLang(req).get("iforgot.title"));
				model.addAttribute("signinSelected", "navbtn-hover");
				model.addAttribute("iforgot", true);
				model.addAttribute("email", email);
				model.addAttribute("token", "");
				model.addAttribute("verified", !error);
				if (error) {
					if (!isPasswordStrongEnough(newpassword)) {
						model.addAttribute("error", Collections.singletonMap("newpassword", utils.getLang(req).get("msgcode.8")));
					} else {
						model.addAttribute("error", Collections.singletonMap("email", utils.getLang(req).get("msgcode.7")));
					}
				}
				return "base";
			}
		}
		return "redirect:" + SIGNINLINK + "/iforgot";
	}

	@PostMapping("/signout")
	public String post(HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			utils.clearSession(req, res);
			return "redirect:" + Config.getConfigParam("signout_url", SIGNINLINK + "?code=5&success=true");
		}
		return "redirect:" + HOMEPAGE;
	}

	private String getAuth(String provider, String accessToken, HttpServletRequest req, HttpServletResponse res) {
		if (!utils.isAuthenticated(req)) {
			if (StringUtils.equalsAnyIgnoreCase(accessToken, "password", "ldap")) {
				accessToken = req.getParameter("username") + ":" +
						("password".equals(accessToken) ? ":" : "") +
						req.getParameter("password");
			}
			String email = getEmailFromAccessToken(accessToken);
			if ("password".equals(provider) && !isEmailRegistered(email)) {
				return "redirect:" + SIGNINLINK + "?code=3&error=true";
			}
			User u = pc.signIn(provider, accessToken, false);
			if (u == null && isAccountLocked(email)) {
				return "redirect:" + SIGNINLINK + "?code=6&error=true&email=" + email;
			}
			return onAuthSuccess(u, req, res);
		}
		return "redirect:" + getBackToUrl(req);
	}

	private void loginWithIdToken(String jwt, HttpServletRequest req, HttpServletResponse res) {
		User u = pc.signIn("passwordless", jwt, false);
		if (u != null) {
			setAuthCookie(u.getPassword(), req, res);
			onAuthSuccess(u, req, res);
		}
	}

	private String onAuthSuccess(User u, HttpServletRequest req, HttpServletResponse res) {
		if (u != null && utils.isEmailDomainApproved(u.getEmail())) {
			// the user password in this case is a Bearer token (JWT)
			setAuthCookie(u.getPassword(), req, res);
			return "redirect:" + getBackToUrl(req);
		} else if (u != null && !utils.isEmailDomainApproved(u.getEmail())) {
			logger.warn("Signin failed for {} because that domain is not in the whitelist.", u.getEmail());
		}
		return "redirect:" + SIGNINLINK + "?code=3&error=true";
	}

	private boolean activateWithEmailToken(User u, String token) {
		if (u != null && token != null) {
			Sysprop s = pc.read(u.getIdentifier());
			if (s != null && token.equals(s.getProperty(Config._EMAIL_TOKEN))) {
				s.addProperty(Config._EMAIL_TOKEN, "");
				pc.update(s);
				u.setActive(true);
				pc.update(u);
				return true;
			}
			logger.warn("Failed to verify user with email '{}' - invalid verification token.", u.getEmail());
		}
		return false;
	}

	private String getEmailFromAccessToken(String accessToken) {
		String[] tokenParts = StringUtils.split(accessToken, ":");
		return (tokenParts != null && tokenParts.length > 0) ? tokenParts[0] : "";
	}

	private boolean isEmailRegistered(String email) {
		Sysprop ident = pc.read(email);
		return ident != null && ident.hasProperty(Config._PASSWORD);
	}

	private boolean isAccountLocked(String email) {
		Sysprop ident = pc.read(email);
		if (ident != null && !StringUtils.isBlank((String) ident.getProperty(Config._EMAIL_TOKEN))) {
			User u = pc.read(Utils.type(User.class), ident.getCreatorid());
			return u != null && !u.getActive();
		}
		return false;
	}

	private void verifyEmailIfNecessary(String name, String email, HttpServletRequest req) {
		Sysprop ident = pc.read(email);
		if (ident != null && !ident.hasProperty(Config._EMAIL_TOKEN)) {
			User u = new User(ident.getCreatorid());
			u.setActive(false);
			u.setName(name);
			u.setEmail(email);
			u.setIdentifier(email);
			utils.sendWelcomeEmail(u, true, req);
		}
	}

	private boolean isSubmittedByHuman(HttpServletRequest req) {
		long time = NumberUtils.toLong(req.getParameter("timestamp"), 0L);
		return StringUtils.isBlank(req.getParameter("leaveblank")) && (System.currentTimeMillis() - time >= 7000);
	}

	private boolean isPasswordStrongEnough(String password) {
		if (StringUtils.length(password) >= MIN_PASS_LENGTH) {
			int score = 0;
			if (password.matches(".*[a-z].*")) {
				score++;
			}
			if (password.matches(".*[A-Z].*")) {
				score++;
			}
			if (password.matches(".*[0-9].*")) {
				score++;
			}
			if (password.matches(".*[^\\w\\s\\n\\t].*")) {
				score++;
			}
			// 1 = good strength, 2 = medium strength, 3 = high strength
			if (MIN_PASS_STRENGTH <= 1) {
				return score >= 2;
			} else if (MIN_PASS_STRENGTH == 2) {
				return score >= 3;
			} else {
				return score >= 4;
			}
		}
		return false;
	}

	private String generatePasswordResetToken(String email, HttpServletRequest req) {
		if (StringUtils.isBlank(email)) {
			return "";
		}
		Sysprop s = pc.read(email);
		// pass reset emails can be sent once every 12h
		if (s != null) {
			if (!s.hasProperty("iforgotTimestamp") || Utils.timestamp() >
						((long) s.getProperty("iforgotTimestamp") + TimeUnit.HOURS.toMillis(12))) {
				String token = Utils.generateSecurityToken(42, true);
				s.addProperty(Config._RESET_TOKEN, token);
				s.addProperty("iforgotTimestamp", Utils.timestamp());
				s.setUpdated(Utils.timestamp());
				if (pc.update(s) != null) {
					utils.sendPasswordResetEmail(email, token, req);
				}
				return token;
			} else {
				logger.warn("Failed to send password reset email to '{}' - this can only be done once every 12h.", email);
			}
		} else {
			logger.warn("Failed to send password reset email to '{}' - user has not signed in with that email and passowrd.", email);
		}
		return "";
	}

	private boolean resetPassword(String email, String newpass, String token) {
		if (StringUtils.isBlank(newpass) || StringUtils.isBlank(token) || !isPasswordStrongEnough(newpass)) {
			return false;
		}
		Sysprop s = pc.read(email);
		if (isValidResetToken(s, Config._RESET_TOKEN, token)) {
			s.addProperty(Config._RESET_TOKEN, ""); // avoid removeProperty method because it won't be seen by server
			String hashed = Utils.bcrypt(newpass);
			s.addProperty(Config._PASSWORD, hashed);
			//setPassword(hashed);
			pc.update(s);
			return true;
		}
		return false;
	}

	private boolean isValidResetToken(Sysprop s, String key, String token) {
		if (StringUtils.isBlank(token)) {
			return false;
		}
		if (s != null && s.hasProperty(key)) {
			String storedToken = (String) s.getProperty(key);
			// tokens expire afer a reasonably short period ~ 30 mins
			long timeout = (long) Config.PASSRESET_TIMEOUT_SEC * 1000L;
			if (StringUtils.equals(storedToken, token) && (s.getUpdated() + timeout) > Utils.timestamp()) {
				return true;
			}
		}
		return false;
	}
}
