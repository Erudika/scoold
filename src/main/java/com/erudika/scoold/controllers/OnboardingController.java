/*
 * Copyright 2013-2026 Erudika. https://erudika.com
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
import com.erudika.para.core.User;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.AUTH_USER_ATTRIBUTE;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
import static com.erudika.scoold.ScooldServer.ONBOARDINGLINK;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.HttpUtils;
import static com.erudika.scoold.utils.HttpUtils.setRawCookie;
import com.erudika.scoold.utils.ScooldUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/start")
public class OnboardingController {

	private static final Logger logger = LoggerFactory.getLogger(OnboardingController.class);
	private static ScooldConfig config = ScooldUtils.getConfig();
	private static final int TOTAL_STEPS = 4;
	private final ScooldUtils utils;
	private final AdminController adminController;

	public OnboardingController(ScooldUtils utils, AdminController adminController) {
		this.utils = utils;
		this.adminController = adminController;
	}

	@GetMapping
	public String get(HttpServletRequest req, Model model) {
		return get(1, req, model);
	}

	@GetMapping("/{step}")
	public String get(@PathVariable(required = false) Integer step, HttpServletRequest req, Model model) {
		if (!ScooldUtils.isSetupRequired()) {
			return "redirect:" + HOMEPAGE;
		}
		List<Locale> locales = utils.getLangutils().getTranslationProgressMap().entrySet().stream().
				map(e -> Locale.forLanguageTag(StringUtils.replaceChars(e.getKey(), "_", "-"))).toList();
		model.addAttribute("path", "onboarding.vm");
		model.addAttribute("title", utils.getLang(req).get("onboarding.title"));
		model.addAttribute("hidenav", true);
		model.addAttribute("onboarding", true);
		model.addAttribute("step", step);
		model.addAttribute("totalSteps", TOTAL_STEPS);
		model.addAttribute("availableLocales", locales);
		model.addAttribute("inDevelopment", config.inDevelopment());
		model.addAttribute("isConnectedToPara", ScooldUtils.isConnectedToPara());
		model.addAttribute("smtpConfigured", !StringUtils.isBlank(config.mailHost()));
		model.addAttribute("paraEndpoint", config.paraEndpoint());
		model.addAttribute("paraAccessKey", config.paraAccessKey());
		model.addAttribute("paraSecretKey", config.paraSecretKey());
		model.addAttribute("appName", config.appName());
		model.addAttribute("adminName", System.getProperty("scoold.admin_name", "Admin"));
		model.addAttribute("adminEmail", config.admins());
		model.addAttribute("hostUrl", config.serverUrl());
		model.addAttribute("gpAppId", config.googleAppId());
		model.addAttribute("gpSecret", config.googleSecret());
		model.addAttribute("fbAppId", config.facebookAppId());
		model.addAttribute("fbSecret", config.facebookSecret());
		model.addAttribute("msAppId", config.microsoftAppId());
		model.addAttribute("msSecret", config.microsoftSecret());
		model.addAttribute("msTenant", config.microsoftTenantId());
		model.addAttribute("mailHost", config.mailHost());
		model.addAttribute("mailPort", String.valueOf(config.mailPort()));
		model.addAttribute("mailUsername", config.mailUsername());
		model.addAttribute("mailPassword", config.mailPassword());
		model.addAttribute("mailTls", config.mailTLSEnabled());
		model.addAttribute("mailSsl", config.mailSSLEnabled());
		model.addAttribute("isDefaultSpacePublic", config.isDefaultSpacePublic());
		model.addAttribute("redirectToIdp", config.redirectSigninToIdp());
		model.addAttribute("passwordAuthEnabled", config.passwordAuthEnabled());

		System.setProperty("scoold.csp_header_enabled", "false"); // allow redirects to IDP after POST
		return "base";
	}

	@PostMapping("/language")
	public String saveLanguage(@RequestParam String locale, HttpServletRequest req, HttpServletResponse res) {
		if (!ScooldUtils.isSetupRequired()) {
			return "redirect:" + HOMEPAGE;
		}
		if (!StringUtils.isBlank(locale)) {
			HttpUtils.setRawCookie(config.localeCookie(), locale, req, res, "Lax", 31536000);
		}
		return "redirect:" + ONBOARDINGLINK + "/2";
	}

	@PostMapping("/para-setup")
	public String saveParaSetup(
			@RequestParam(required = false, defaultValue = "") String rawConfig,
			@RequestParam(required = false, defaultValue = "") String paraEndpoint,
			@RequestParam(required = false, defaultValue = "") String paraAccessKey,
			@RequestParam(required = false, defaultValue = "") String paraSecretKey,
			HttpServletRequest req, HttpServletResponse res) {
		if (!ScooldUtils.isSetupRequired()) {
			return "redirect:" + HOMEPAGE;
		}
		if (!StringUtils.isBlank(rawConfig)) {
			Config c = com.typesafe.config.ConfigFactory.parseString(rawConfig).getConfig(config.getConfigRootPrefix());
			c = c.withValue("onboarding_enabled", ConfigValueFactory.fromAnyRef(true)); // ignore this property until setup is done
			ScooldConfig conf = (ScooldConfig) config.overwriteConfig(c);
			conf.store();
			paraEndpoint = conf.paraEndpoint();
			paraAccessKey = conf.paraAccessKey();
			paraSecretKey = conf.paraSecretKey();
			config = conf;
		}
		if (!connectedToPara(config.paraEndpoint(), config.paraAccessKey(), config.paraSecretKey())) {
			return "redirect:" + ONBOARDINGLINK + "/2?test";
		}
		if (!StringUtils.isBlank(paraEndpoint)) {
			saveConfigKey("para_endpoint", paraEndpoint, req, res);
		}
		if (!StringUtils.isBlank(paraAccessKey)) {
			saveConfigKey("para_access_key", paraAccessKey, req, res);
		}
		if (!StringUtils.isBlank(paraSecretKey)) {
			saveConfigKey("para_secret_key", paraSecretKey, req, res);
		}
		utils.reconnectParaClient(config.paraEndpoint(), config.paraAccessKey(), config.paraSecretKey());
		return "redirect:" + ONBOARDINGLINK + "/3";
	}

	@GetMapping(path = "/test-para-connection", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> testParaConnection(
			@RequestParam String paraEndpoint,
			@RequestParam String paraAccessKey,
			@RequestParam String paraSecretKey) {
		Map<String, Object> result = new LinkedHashMap<>();
		try {
			result.put("connected", connectedToPara(paraEndpoint, paraAccessKey, paraSecretKey));
		} catch (Exception e) {
			result.put("connected", false);
			result.put("error", e.getMessage());
		}
		return ResponseEntity.ok(result);
	}

	@PostMapping("/site-setup")
	public String saveSiteSetup(
			@RequestParam(required = false, defaultValue = "") String appName,
			@RequestParam(required = false, defaultValue = "") String hostUrl,
			@RequestParam(required = false, defaultValue = "true") Boolean isDefaultSpacePublic,
			@RequestParam(required = false, defaultValue = "false") Boolean redirectToIdp,
			@RequestParam(required = false, defaultValue = "false") Boolean darkMode,
			@RequestParam(required = false, defaultValue = "password") String authMethod,
			@RequestParam(required = false, defaultValue = "") String adminEmail,
			@RequestParam(required = false, defaultValue = "") String adminName,
			@RequestParam(required = false, defaultValue = "") String adminPassword,
			@RequestParam(required = false, defaultValue = "") String gpAppId,
			@RequestParam(required = false, defaultValue = "") String gpSecret,
			@RequestParam(required = false, defaultValue = "") String fbAppId,
			@RequestParam(required = false, defaultValue = "") String fbSecret,
			@RequestParam(required = false, defaultValue = "") String msAppId,
			@RequestParam(required = false, defaultValue = "") String msSecret,
			@RequestParam(required = false, defaultValue = "common") String msTenant,
			HttpServletRequest req,
			HttpServletResponse res) {
		if (!ScooldUtils.isSetupRequired()) {
			return "redirect:" + HOMEPAGE;
		}
		if (!StringUtils.isBlank(appName)) {
			saveConfigKey("app_name", appName, req, res);
		}
		if (StringUtils.isBlank(config.appSecretKey())) {
			saveConfigKey("app_secret_key", Utils.generateSecurityToken(), req, res);
		}
		if (!StringUtils.isBlank(hostUrl)) {
			saveConfigKey("host_url", hostUrl, req, res);
		}
		saveConfigKey("is_default_space_public", String.valueOf(isDefaultSpacePublic), req, res);
		saveConfigKey("redirect_signin_to_idp", String.valueOf(redirectToIdp), req, res);
		String redirect = saveAdminAndAuth(authMethod, adminEmail, adminName,
				adminPassword, gpAppId, gpSecret, fbAppId,
				fbSecret, msAppId, msSecret, msTenant, darkMode, req, res);
		return "redirect:" + redirect;
	}

	@PostMapping("/smtp-setup")
	public String saveSmtpSetup(
			@RequestParam(required = false, defaultValue = "") String mailHost,
			@RequestParam(required = false, defaultValue = "587") Integer mailPort,
			@RequestParam(required = false, defaultValue = "") String mailUsername,
			@RequestParam(required = false, defaultValue = "") String mailPassword,
			@RequestParam(required = false, defaultValue = "true") Boolean mailTLS,
			@RequestParam(required = false, defaultValue = "false") Boolean mailSSL,
			@RequestParam(required = false, defaultValue = "false") Boolean skipSmtp,
			HttpServletRequest req, HttpServletResponse res) {
		if (!config.onboardingEnabled()) {
			return "redirect:" + HOMEPAGE;
		}
		if (!skipSmtp && !StringUtils.isBlank(mailHost) &&
				!StringUtils.isBlank(mailUsername) && !StringUtils.isBlank(mailPassword)) {
			saveConfigKey("mail.host", mailHost, req, res);
			saveConfigKey("mail.port", String.valueOf(mailPort), req, res);
			saveConfigKey("mail.username", mailUsername, req, res);
			saveConfigKey("mail.password", mailPassword, req, res);
			saveConfigKey("mail.tls", String.valueOf(mailTLS), req, res);
			saveConfigKey("mail.ssl", String.valueOf(mailSSL), req, res);
		}
		ScooldUtils.setSetupRequired(false);
		System.clearProperty("scoold.csp_header_enabled");
		saveConfigKey("onboarding_enabled", "false", req, res);
		return "redirect:" + HOMEPAGE;
	}

	@GetMapping(path = "/test-smtp-connection",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> testSmtpConnection(
			@RequestParam String mailHost,
			@RequestParam(defaultValue = "587") int mailPort,
			@RequestParam(required = false, defaultValue = "") String mailUsername,
			@RequestParam(required = false, defaultValue = "") String mailPassword,
			@RequestParam(defaultValue = "true") Boolean mailTLS,
			@RequestParam(defaultValue = "false") Boolean mailSSL,
			@RequestParam(required = false, defaultValue = "")
			String testRecipient) {
		Map<String, Object> result = new LinkedHashMap<>();
		try {
			JavaMailSenderImpl sender = new JavaMailSenderImpl();
			sender.setHost(mailHost);
			sender.setPort(mailPort);
			sender.setUsername(StringUtils.isBlank(mailUsername) ? null : mailUsername);
			sender.setPassword(StringUtils.isBlank(mailPassword) ? null : mailPassword);
			Properties props = sender.getJavaMailProperties();
			props.put("mail.transport.protocol", "smtp");
			props.put("mail.smtp.auth", String.valueOf(!StringUtils.isBlank(mailUsername)));
			props.put("mail.smtp.starttls.enable", String.valueOf(mailTLS));
			props.put("mail.smtp.ssl.enable", String.valueOf(mailSSL));
			props.put("mail.smtp.ssl.trust", "*");
			props.put("mail.smtp.timeout", "5000");
			props.put("mail.smtp.connectiontimeout", "5000");
			if (!StringUtils.isBlank(testRecipient)) {
				MimeMessage msg = sender.createMimeMessage();
				MimeMessageHelper helper = new MimeMessageHelper(msg);
				helper.setFrom(config.supportEmail(), config.appName());
				helper.setTo(testRecipient);
				helper.setSubject("Scoold SMTP Test");
				helper.setText("This is a test email from your Scoold onboarding setup.", true);
				sender.send(msg);
			} else {
				sender.testConnection();
			}
			result.put("connected", true);
		} catch (Exception e) {
			result.put("connected", false);
			result.put("error", e.getMessage());
		}
		return ResponseEntity.ok(result);
	}

	private String saveAdminAndAuth(String authMethod,
			String adminEmails, String adminName,
			String adminPassword,
			String gpAppId, String gpSecret,
			String fbAppId, String fbSecret,
			String msAppId, String msSecret, String msTenant,
			Boolean darkMode,
			HttpServletRequest req,
			HttpServletResponse res) {
		String adminEmail = StringUtils.substringBefore(adminEmails, ",").trim();
		if (!StringUtils.isBlank(adminEmails)) {
			saveConfigKey("admins", adminEmails, req, res);
		}
		System.setProperty("scoold.admin_name", adminName); // temporary storage for admin name if user redirects to IDP
		switch (authMethod) {
			case "password":
				saveConfigKey("password_auth_enabled", "true", req, res);
				if (!StringUtils.isBlank(adminEmail) && !StringUtils.isBlank(adminName) && !StringUtils.isBlank(adminPassword)) {
					try {
						pc().throwExceptionOnHTTPError(true);
						User u = pc().signIn("password", adminEmail + ":" + adminName + ":" + adminPassword, false);

						pc().throwExceptionOnHTTPError(false);
						if (u != null && u.getActive()) {
							String jwt = u.getPassword();
							u.setGroups(User.Groups.ADMINS.toString());
							Profile authUser = Profile.fromUser(u);
							authUser.setDarkmodeEnabled(darkMode);
							authUser.setGroups(User.Groups.ADMINS.toString());
							authUser.create();
							if (!StringUtils.isBlank(config.mailHost())) {
								utils.sendWelcomeEmail(u, false, req);
							}
							req.setAttribute(AUTH_USER_ATTRIBUTE, authUser);
							setRawCookie(config.authCookie(), jwt, req, res, "Lax", config.sessionTimeoutSec());
							utils.addFirstAdmin(adminEmail, config);
						}
					} catch (Exception e) {
						logger.error("Failed to create admin account: {}", e.getMessage());
					}
				}
				break;
			case "google":
				saveConfigKey("password_auth_enabled", "false", req, res);
				saveConfigKey("gp_app_id", gpAppId, req, res);
				saveConfigKey("gp_secret", gpSecret, req, res);
				HttpUtils.setRawCookie("returnto", Utils.urlEncode(config.serverUrl() + config.serverContextPath() +
						"/start/4"), req, res, "Lax", 360);
				return utils.getGoogleLoginURL();
			case "facebook":
				saveConfigKey("password_auth_enabled", "false", req, res);
				saveConfigKey("fb_app_id", fbAppId, req, res);
				saveConfigKey("fb_secret", fbSecret, req, res);
				HttpUtils.setRawCookie("returnto", Utils.urlEncode(config.serverUrl() + config.serverContextPath() +
						"/start/4"), req, res, "Lax", 360);
				return utils.getFacebookLoginURL();
			case "microsoft":
				saveConfigKey("password_auth_enabled", "false", req, res);
				saveConfigKey("ms_app_id", msAppId, req, res);
				saveConfigKey("ms_secret", msSecret, req, res);
				saveConfigKey("ms_tenant_id", msTenant, req, res);
				HttpUtils.setRawCookie("returnto", Utils.urlEncode(config.serverUrl() + config.serverContextPath() +
						"/start/4"), req, res, "Lax", 360);
				return utils.getMicrosoftLoginURL();
			default:
				break;
		}
		return ONBOARDINGLINK + "/4";
	}

	private void saveConfigKey(String key, String value, HttpServletRequest req, HttpServletResponse res) {
		if (!utils.isAuthenticated(req)) {
			req.setAttribute(AUTH_USER_ATTRIBUTE, utils.getSystemUser());
		}
		adminController.saveConfig(key, value, req, res);
	}

	private ParaClient pc() {
		return utils.getParaClient();
	}

	private boolean connectedToPara(String paraEndpoint, String paraAccessKey, String paraSecretKey) {
		ParaClient testPc = new ParaClient(paraAccessKey, paraSecretKey);
		testPc.setEndpoint(paraEndpoint);
		ScooldUtils.setParaEndpointAndApiPath(testPc);
		return testPc.getTimestamp() > 0;
	}
}
