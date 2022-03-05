/*
 * Copyright 2013-2022 Erudika. https://erudika.com
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

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.User;
import com.erudika.para.core.annotations.Email;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.utils.HttpUtils;
import static com.erudika.scoold.utils.HttpUtils.getBackToUrl;
import static com.erudika.scoold.utils.HttpUtils.setAuthCookie;
import com.erudika.scoold.utils.ScooldUtils;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SigninController {

	private static final Logger logger = LoggerFactory.getLogger(SigninController.class);
	private static final ScooldConfig CONF = ScooldUtils.getConfig();

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
		if (CONF.redirectSigninToIdp() && !"5".equals(req.getParameter("code"))) {
			return "redirect:" + utils.getFirstConfiguredLoginURL();
		}
		model.addAttribute("path", "signin.vm");
		model.addAttribute("title", utils.getLang(req).get("signin.title"));
		model.addAttribute("signinSelected", "navbtn-hover");
		model.addAttribute("fbLoginEnabled", !CONF.facebookAppId().isEmpty());
		model.addAttribute("gpLoginEnabled", !CONF.googleAppId().isEmpty());
		model.addAttribute("ghLoginEnabled", !CONF.githubAppId().isEmpty());
		model.addAttribute("inLoginEnabled", !CONF.linkedinAppId().isEmpty());
		model.addAttribute("twLoginEnabled", !CONF.twitterAppId().isEmpty());
		model.addAttribute("msLoginEnabled", !CONF.microsoftAppId().isEmpty());
		model.addAttribute("slLoginEnabled", utils.isSlackAuthEnabled());
		model.addAttribute("azLoginEnabled", !CONF.amazonAppId().isEmpty());
		model.addAttribute("oa2LoginEnabled", !CONF.oauthAppId("").isEmpty());
		model.addAttribute("oa2secondLoginEnabled", !CONF.oauthAppId("second").isEmpty());
		model.addAttribute("oa2thirdLoginEnabled", !CONF.oauthAppId("third").isEmpty());
		model.addAttribute("ldapLoginEnabled", !CONF.ldapServerUrl().isEmpty());
		model.addAttribute("passwordLoginEnabled", CONF.passwordAuthEnabled());
		model.addAttribute("oa2LoginProvider", CONF.oauthProvider(""));
		model.addAttribute("oa2secondLoginProvider", CONF.oauthProvider("second"));
		model.addAttribute("oa2thirdLoginProvider", CONF.oauthProvider("third"));
		model.addAttribute("ldapLoginProvider", CONF.ldapProvider());
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
		model.addAttribute("nosmtp", StringUtils.isBlank(CONF.mailHost()));
		model.addAttribute("captchakey", CONF.captchaSiteKey());
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
		model.addAttribute("nosmtp", StringUtils.isBlank(CONF.mailHost()));
		model.addAttribute("captchakey", CONF.captchaSiteKey());
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
		boolean validCaptcha = HttpUtils.isValidCaptcha(req.getParameter("g-recaptcha-response"));
		if (!utils.isAuthenticated(req) && approvedDomain && validCaptcha) {
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
				model.addAttribute("token", token);
				model.addAttribute("verified", !error);
				model.addAttribute("captchakey", CONF.captchaSiteKey());
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
		logger.info("Password reset failed for {} - authenticated={}, approvedDomain={}, validCaptcha={}",
				email, utils.isAuthenticated(req), approvedDomain, validCaptcha);
		return "redirect:" + SIGNINLINK + "/iforgot";
	}

	@PostMapping("/signout")
	public String post(HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			utils.clearSession(req, res);
			return "redirect:" + CONF.signoutUrl();
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
		if (StringUtils.length(password) >= CONF.minPasswordLength()) {
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
			if (CONF.minPasswordStrength() <= 1) {
				return score >= 2;
			} else if (CONF.minPasswordStrength() == 2) {
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
						(Long.valueOf(s.getProperty("iforgotTimestamp").toString()) + TimeUnit.HOURS.toMillis(12))) {
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
			s.addProperty("iforgotTimestamp", 0);
			s.addProperty(Config._PASSWORD, Utils.bcrypt(newpass));
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
			long timeout = (long) CONF.passwordResetTimeoutSec() * 1000L;
			if (StringUtils.equals(storedToken, token) && (s.getUpdated() + timeout) > Utils.timestamp()) {
				return true;
			} else {
				logger.info("User {} tried to reset password with an expired reset token.", s.getId());
			}
		}
		return false;
	}
}
