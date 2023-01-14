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
package com.erudika.scoold;

import com.erudika.para.core.App;
import com.erudika.para.core.annotations.Documented;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Para;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import static com.erudika.scoold.ScooldServer.SIGNOUTLINK;
import com.typesafe.config.ConfigObject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;

/**
 * Scoold configuration.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Component
@Named
public class ScooldConfig extends Config {

	@Override
	public com.typesafe.config.Config getFallbackConfig() {
		if (StringUtils.isBlank(System.getProperty("scoold.autoinit.para_config_file")) &&
				StringUtils.isBlank(System.getenv("scoold.autoinit.para_config_file"))) {
			return Para.getConfig().getConfig(); // fall back to para.* config
		}
		return com.typesafe.config.ConfigFactory.empty();
	}

	@Override
	public Set<String> getKeysExcludedFromRendering() {
		return Set.of("scoold", "security.ignored", "security.protected.admin");
	}

	@Override
	public String getConfigRootPrefix() {
		return "scoold";
	}

	/* **************************************************************************************************************
	 * Core                                                                                                    Core *
	 ****************************************************************************************************************/

	@Documented(position = 10,
			identifier = "app_name",
			value = "Scoold",
			category = "Core",
			description = "The formal name of the web application.")
	public String appName() {
		return getConfigParam("app_name", "Scoold");
	}

	@Documented(position = 20,
			identifier = "para_access_key",
			value = "app:scoold",
			category = "Core",
			tags = {"requires restart"},
			description = "App identifier (access key) of the Para app used by Scoold.")
	public String paraAccessKey() {
		return App.id(getConfigParam("para_access_key", getConfigParam("access_key", "app:scoold")));
	}

	@Documented(position = 30,
			identifier = "para_secret_key",
			value = "x",
			category = "Core",
			tags = {"requires restart"},
			description = "Secret key of the Para app used by Scoold.")
	public String paraSecretKey() {
		return getConfigParam("para_secret_key", getConfigParam("secret_key", "x"));
	}

	@Documented(position = 40,
			identifier = "para_endpoint",
			value = "http://localhost:8080",
			category = "Core",
			tags = {"requires restart"},
			description = "The URL of the Para server for Scoold to connects to. For hosted Para, use `https://paraio.com`")
	public String paraEndpoint() {
		return getConfigParam("para_endpoint", getConfigParam("endpoint", "http://localhost:8080"));
	}

	@Documented(position = 50,
			identifier = "host_url",
			value = "http://localhost:8000",
			category = "Core",
			description = "The internet-facing (public) URL of this Scoold server.")
	public String serverUrl() {
		return StringUtils.removeEnd(getConfigParam("host_url", "http://localhost:" + serverPort()), "/");
	}

	@Documented(position = 60,
			identifier = "port",
			value = "8000",
			type = Integer.class,
			category = "Core",
			tags = {"requires restart"},
			description = "The network port of this Scoold server. Port number should be a number above `1024`.")
	public int serverPort() {
		return NumberUtils.toInt(System.getProperty("server.port"), getConfigInt("port", 8000));
	}

	@Documented(position = 70,
			identifier = "env",
			value = "development",
			category = "Core",
			tags = {"requires restart"},
			description = "The environment profile to be used - possible values are `production` or `development`")
	public String environment() {
		return getConfigParam("env", "development");
	}

	@Documented(position = 80,
			identifier = "app_secret_key",
			category = "Core",
			description = "A random secret string, min. 32 chars long. *Must be different from the secret key of "
					+ "the Para app*. Used for generating JWTs and passwordless authentication tokens.")
	public String appSecretKey() {
		return getConfigParam("app_secret_key", "");
	}

	@Documented(position = 90,
			identifier = "admins",
			category = "Core",
			description = "A comma-separated list of emails of people who will be promoted to administrators with "
					+ "full rights over the content on the site. This can also contain Para user identifiers.")
	public String admins() {
		return getConfigParam("admins", "");
	}

	@Documented(position = 100,
			identifier = "is_default_space_public",
			value = "true",
			type = Boolean.class,
			category = "Core",
			description = "When enabled, all content in the default space will be publicly visible, "
					+ "without authentication, incl. users and tags. Disable to make the site private.")
	public boolean isDefaultSpacePublic() {
		return getConfigBoolean("is_default_space_public", true);
	}

	@Documented(position = 110,
			identifier = "context_path",
			category = "Core",
			tags = {"requires restart"},
			description = "The context path (subpath) of the web application, defaults to the root path `/`.")
	public String serverContextPath() {
		String context = getConfigParam("context_path", "");
		return StringUtils.stripEnd((StringUtils.isBlank(context) ?
				System.getProperty("server.servlet.context-path", "") : context), "/");
	}

	@Documented(position = 120,
			identifier = "webhooks_enabled",
			value = "true",
			type = Boolean.class,
			category = "Core",
			description = "Enable/disable webhooks support for events like `question.create`, `user.signup`, etc.")
	public boolean webhooksEnabled() {
		return getConfigBoolean("webhooks_enabled", true);
	}

	@Documented(position = 130,
			identifier = "api_enabled",
			value = "false",
			type = Boolean.class,
			category = "Core",
			description = "Enable/disable the Scoold RESTful API. Disabled by default.")
	public boolean apiEnabled() {
		return getConfigBoolean("api_enabled", false);
	}

	@Documented(position = 140,
			identifier = "feedback_enabled",
			value = "false",
			type = Boolean.class,
			category = "Core",
			description = "Enable/disable the feedback page on the site. It is intended for internal discussion "
					+ "about the website itself.")
	public boolean feedbackEnabled() {
		return getConfigBoolean("feedback_enabled", false);
	}


	/* **************************************************************************************************************
	 * Emails                                                                                                Emails *
	 ****************************************************************************************************************/

	@Documented(position = 150,
			identifier = "support_email",
			value = "contact@scoold.com",
			category = "Emails",
			description = "The email address to use for sending transactional emails, like welcome/password reset emails.")
	public String supportEmail() {
		return getConfigParam("support_email", "contact@scoold.com");
	}

	@Documented(position = 160,
			identifier = "mail.host",
			category = "Emails",
			description = "The SMTP server host to use for sending emails.")
	public String mailHost() {
		return getConfigParam("mail.host", "");
	}

	@Documented(position = 170,
			identifier = "mail.port",
			value = "587",
			type = Integer.class,
			category = "Emails",
			description = "The SMTP server port to use for sending emails.")
	public int mailPort() {
		return getConfigInt("mail.port", 587);
	}

	@Documented(position = 180,
			identifier = "mail.username",
			category = "Emails",
			description = "The SMTP server username.")
	public String mailUsername() {
		return getConfigParam("mail.username", "");
	}

	@Documented(position = 190,
			identifier = "mail.password",
			category = "Emails",
			description = "The SMTP server password.")
	public String mailPassword() {
		return getConfigParam("mail.password", "");
	}

	@Documented(position = 200,
			identifier = "mail.tls",
			value = "true",
			type = Boolean.class,
			category = "Emails",
			description = "Enable/disable TLS for the SMTP connection.")
	public boolean mailTLSEnabled() {
		return getConfigBoolean("mail.tls", true);
	}

	@Documented(position = 210,
			identifier = "mail.ssl",
			value = "false",
			type = Boolean.class,
			category = "Emails",
			description = "Enable/disable SSL for the SMTP connection.")
	public boolean mailSSLEnabled() {
		return getConfigBoolean("mail.ssl", false);
	}

	@Documented(position = 220,
			identifier = "mail.debug",
			value = "false",
			type = Boolean.class,
			category = "Emails",
			description = "Enable/disable debug information when sending emails through SMTP.")
	public boolean mailDebugEnabled() {
		return getConfigBoolean("mail.debug", false);
	}

	@Documented(position = 230,
			identifier = "favtags_emails_enabled",
			value = "false",
			type = Boolean.class,
			category = "Emails",
			description = "Set the default toggle value for all users for receiving emails for new content "
					+ "with their favorite tags.")
	public boolean favoriteTagsEmailsEnabled() {
		return getConfigBoolean("favtags_emails_enabled", false);
	}

	@Documented(position = 240,
			identifier = "reply_emails_enabled",
			value = "false",
			type = Boolean.class,
			category = "Emails",
			description = "Set the default toggle value for all users for receiving emails for answers to their questions.")
	public boolean replyEmailsEnabled() {
		return getConfigBoolean("reply_emails_enabled", false);
	}

	@Documented(position = 250,
			identifier = "comment_emails_enabled",
			value = "false",
			type = Boolean.class,
			category = "Emails",
			description = "Set the default toggle value for all users for receiving emails for comments on their posts.")
	public boolean commentEmailsEnabled() {
		return getConfigBoolean("comment_emails_enabled", false);
	}

	@Documented(position = 260,
			identifier = "summary_email_period_days",
			value = "7",
			type = Integer.class,
			category = "Emails",
			tags = {"Pro"},
			description = "The time period between each content digest email, in days.")
	public int emailsSummaryIntervalDays() {
		return getConfigInt("summary_email_period_days", 7);
	}

	@Documented(position = 270,
			identifier = "summary_email_items",
			value = "25",
			type = Integer.class,
			category = "Emails",
			description = "The number of posts to include in the digest email (a summary of new posts).")
	public int emailsSummaryItems() {
		return getConfigInt("summary_email_items", 25);
	}

	@Documented(position = 280,
			identifier = "notification_emails_allowed",
			value = "true",
			type = Boolean.class,
			category = "Emails",
			description = "Enable/disable *all* notification emails.")
	public boolean notificationEmailsAllowed() {
		return getConfigBoolean("notification_emails_allowed", true);
	}

	@Documented(position = 290,
			identifier = "newpost_emails_allowed",
			value = "true",
			type = Boolean.class,
			category = "Emails",
			description = "Enable/disable *all* email notifications for every new question that is posted on the site.")
	public boolean emailsForNewPostsAllowed() {
		return getConfigBoolean("newpost_emails_allowed", true);
	}

	@Documented(position = 300,
			identifier = "favtags_emails_allowed",
			value = "true",
			type = Boolean.class,
			category = "Emails",
			description = "Enable/disable *all* email notifications for every new question tagged with a favorite tag.")
	public boolean emailsForFavtagsAllowed() {
		return getConfigBoolean("favtags_emails_allowed", true);
	}

	@Documented(position = 310,
			identifier = "reply_emails_allowed",
			value = "true",
			type = Boolean.class,
			category = "Emails",
			description = "Enable/disable *all* email notifications for every new answer that is posted on the site.")
	public boolean emailsForRepliesAllowed() {
		return getConfigBoolean("reply_emails_allowed", true);
	}

	@Documented(position = 320,
			identifier = "comment_emails_allowed",
			value = "true",
			type = Boolean.class,
			category = "Emails",
			description = "Enable/disable *all* email notifications for every new comment that is posted on the site.")
	public boolean emailsForCommentsAllowed() {
		return getConfigBoolean("comment_emails_allowed", true);
	}

	@Documented(position = 330,
			identifier = "mentions_emails_allowed",
			value = "true",
			type = Boolean.class,
			category = "Emails",
			tags = {"Pro"},
			description = "Enable/disable *all* email notifications every time a user is mentioned.")
	public boolean emailsForMentionsAllowed() {
		return getConfigBoolean("mentions_emails_allowed", true);
	}

	@Documented(position = 340,
			identifier = "summary_email_controlled_by_admins",
			value = "false",
			type = Boolean.class,
			category = "Emails",
			tags = {"Pro"},
			description = "Controls whether admins can enable/disable summary emails for everyone from the 'Settings' page")
	public boolean emailsForSummaryControlledByAdmins() {
		return getConfigBoolean("summary_email_controlled_by_admins", false);
	}

	@Documented(position = 350,
			identifier = "mention_emails_controlled_by_admins",
			value = "false",
			type = Boolean.class,
			category = "Emails",
			tags = {"Pro"},
			description = "Controls whether admins can enable/disable mention emails for everyone from the 'Settings' page")
	public boolean emailsForMentionsControlledByAdmins() {
		return getConfigBoolean("mention_emails_controlled_by_admins", false);
	}

	@Documented(position = 360,
			identifier = "emails.welcome_text1",
			value = "You are now part of {0} - a friendly Q&A community...",
			category = "Emails",
			description = "Allows for changing the default text (first paragraph) in the welcome email message.")
	public String emailsWelcomeText1(Map<String, String> lang) {
		return getConfigParam("emails.welcome_text1", lang.get("signin.welcome.body1") + "<br><br>");
	}

	@Documented(position = 370,
			identifier = "emails.welcome_text2",
			value = "To get started, simply navigate to the \"Ask question\" page and ask a question...",
			category = "Emails",
			description = "Allows for changing the default text (second paragraph) in the welcome email message.")
	public String emailsWelcomeText2(Map<String, String> lang) {
		return getConfigParam("emails.welcome_text2", lang.get("signin.welcome.body2") + "<br><br>");
	}

	@Documented(position = 380,
			identifier = "emails.welcome_text3",
			value = "Best, <br>The {0} team",
			category = "Emails",
			description = "Allows for changing the default text (signature at the end) in the welcome email message.")
	public String emailsWelcomeText3(Map<String, String> lang) {
		return getConfigParam("emails.welcome_text3", lang.get("notification.signature") + "<br><br>");
	}

	@Documented(position = 390,
			identifier = "emails.default_signature",
			value = "Best, <br>The {0} team",
			category = "Emails",
			description = "The default email signature for all transactional emails sent from Scoold.")
	public String emailsDefaultSignatureText(String defaultText) {
		return getConfigParam("emails.default_signature", defaultText);
	}

	/* **************************************************************************************************************
	 * Security                                                                                            Security *
	 ****************************************************************************************************************/

	@Documented(position = 400,
			identifier = "approved_domains_for_signups",
			category = "Security",
			description = "A comma-separated list of domain names, which will be used to restrict the people who "
					+ "are allowed to sign up on the site.")
	public String approvedDomainsForSignups() {
		return getConfigParam("approved_domains_for_signups", "");
	}

	@Documented(position = 410,
			identifier = "security.allow_unverified_emails",
			value = "false",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable email verification after the initial user registration. Users with unverified "
					+ "emails won't be able to sign in, unless they use a social login provider.")
	public boolean allowUnverifiedEmails() {
		return getConfigBoolean("security.allow_unverified_emails", StringUtils.isBlank(mailHost()));
	}

	@Documented(position = 420,
			identifier = "session_timeout",
			value = "86400",
			type = Integer.class,
			category = "Security",
			description = "The validity period of the authentication cookie, in seconds. Default is 24h.")
	public int sessionTimeoutSec() {
		return getConfigInt("session_timeout", Para.getConfig().sessionTimeoutSec());
	}

	@Documented(position = 430,
			identifier = "jwt_expires_after",
			value = "86400",
			type = Integer.class,
			category = "Security",
			description = "The validity period of the session token (JWT), in seconds. Default is 24h.")
	public int jwtExpiresAfterSec() {
		return getConfigInt("jwt_expires_after", Para.getConfig().jwtExpiresAfterSec());
	}

	@Documented(position = 440,
			identifier = "security.one_session_per_user",
			value = "true",
			type = Boolean.class,
			category = "Security",
			description = "If disabled, users can sign in from multiple locations and devices, keeping a few open "
					+ "sessions at once. Otherwise, only one session will be kept open, others will be closed.")
	public boolean oneSessionPerUser() {
		return getConfigBoolean("security.one_session_per_user", true);
	}

	@Documented(position = 450,
			identifier = "min_password_length",
			value = "8",
			type = Integer.class,
			category = "Security",
			description = "The minimum length of passwords.")
	public int minPasswordLength() {
		return getConfigInt("min_password_length", Para.getConfig().minPasswordLength());
	}

	@Documented(position = 460,
			identifier = "min_password_strength",
			value = "2",
			type = Integer.class,
			category = "Security",
			description = "The minimum password strength - one of 3 levels: `1` good enough, `2` strong, `3` very strong.")
	public int minPasswordStrength() {
		return getConfigInt("min_password_strength", 2);
	}

	@Documented(position = 470,
			identifier = "pass_reset_timeout",
			value = "1800",
			type = Integer.class,
			category = "Security",
			description = "The validity period of the password reset token sent via email for resetting users' "
					+ "passwords. Default is 30 min.")
	public int passwordResetTimeoutSec() {
		return getConfigInt("pass_reset_timeout", Para.getConfig().passwordResetTimeoutSec());
	}

	@Documented(position = 480,
			identifier = "profile_anonimity_enabled",
			value = "false",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable the option for users to anonimize their profiles on the site, "
					+ "hiding their name and picture.")
	public boolean profileAnonimityEnabled() {
		return getConfigBoolean("profile_anonimity_enabled", false);
	}

	@Documented(position = 490,
			identifier = "signup_captcha_site_key",
			category = "Security",
			description = "The reCAPTCHA v3 site key for protecting the signup and password reset pages.")
	public String captchaSiteKey() {
		return getConfigParam("signup_captcha_site_key", "");
	}

	@Documented(position = 500,
			identifier = "signup_captcha_secret_key",
			category = "Security",
			description = "The reCAPTCHA v3 secret.")
	public String captchaSecretKey() {
		return getConfigParam("signup_captcha_secret_key", "");
	}
	@Documented(position = 510,
			identifier = "csp_reports_enabled",
			value = "false",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable automatic reports each time the Content Security Policy is violated.")
	public boolean cspReportsEnabled() {
		return getConfigBoolean("csp_reports_enabled", false);
	}

	@Documented(position = 520,
			identifier = "csp_header_enabled",
			value = "true",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable the Content Security Policy (CSP) header.")
	public boolean cspHeaderEnabled() {
		return getConfigBoolean("csp_header_enabled", true);
	}

	@Documented(position = 530,
			identifier = "csp_header",
			category = "Security",
			description = "The CSP header value which will overwrite the default one. This can contain one or more "
					+ "`{{nonce}}` placeholders, which will be replaced with an actual nonce on each request.")
	public String cspHeader(String nonce) {
		return getConfigParam("csp_header", getDefaultContentSecurityPolicy()).replaceAll("\\{\\{nonce\\}\\}", nonce);
	}

	@Documented(position = 540,
			identifier = "hsts_header_enabled",
			value = "true",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable the `Strict-Transport-Security` security header.")
	public boolean hstsHeaderEnabled() {
		return getConfigBoolean("hsts_header_enabled", true);
	}

	@Documented(position = 550,
			identifier = "framing_header_enabled",
			value = "true",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable the `X-Frame-Options` security header.")
	public boolean framingHeaderEnabled() {
		return getConfigBoolean("framing_header_enabled", true);
	}

	@Documented(position = 560,
			identifier = "xss_header_enabled",
			value = "true",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable the `X-XSS-Protection` security header.")
	public boolean xssHeaderEnabled() {
		return getConfigBoolean("xss_header_enabled", true);
	}

	@Documented(position = 570,
			identifier = "contenttype_header_enabled",
			value = "true",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable the `X-Content-Type-Options` security header.")
	public boolean contentTypeHeaderEnabled() {
		return getConfigBoolean("contenttype_header_enabled", true);
	}

	@Documented(position = 580,
			identifier = "referrer_header_enabled",
			value = "true",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable the `Referrer-Policy` security header.")
	public boolean referrerHeaderEnabled() {
		return getConfigBoolean("referrer_header_enabled", true);
	}

	@Documented(position = 590,
			identifier = "permissions_header_enabled",
			value = "true",
			type = Boolean.class,
			category = "Security",
			description = "Enable/disable the `Permissions-Policy` security header.")
	public boolean permissionsHeaderEnabled() {
		return getConfigBoolean("permissions_header_enabled", true);
	}

	@Documented(position = 600,
			identifier = "csp_connect_sources",
			category = "Security",
			description = "Additional sources to add to the `connect-src` CSP directive. "
					+ "Used when adding external scripts to the site.")
	public String cspConnectSources() {
		return getConfigParam("csp_connect_sources", "");
	}

	@Documented(position = 610,
			identifier = "csp_frame_sources",
			category = "Security",
			description = "Additional sources to add to the `frame-src` CSP directive. "
					+ "Used when adding external scripts to the site.")
	public String cspFrameSources() {
		return getConfigParam("csp_frame_sources", "");
	}

	@Documented(position = 620,
			identifier = "csp_font_sources",
			category = "Security",
			description = "Additional sources to add to the `font-src` CSP directive. "
					+ "Used when adding external fonts to the site.")
	public String cspFontSources() {
		return getConfigParam("csp_font_sources", "");
	}

	@Documented(position = 630,
			identifier = "csp_style_sources",
			value = "",
			category = "Security",
			description = "Additional sources to add to the `style-src` CSP directive. "
					+ "Used when adding external fonts to the site.")
	public String cspStyleSources() {
		return getConfigParam("csp_style_sources", serverUrl() + stylesheetUrl() + " " +
				externalStyles().replaceAll(",", ""));
	}

	/* **************************************************************************************************************
	 * Basic Authentication                                                                    Basic Authentication *
	 ****************************************************************************************************************/

	@Documented(position = 640,
			identifier = "password_auth_enabled",
			value = "true",
			type = Boolean.class,
			category = "Basic Authentication",
			description = "Enabled/disable the ability for users to sign in with an email and password.")
	public boolean passwordAuthEnabled() {
		return getConfigBoolean("password_auth_enabled", true);
	}

	@Documented(position = 650,
			identifier = "fb_app_id",
			category = "Basic Authentication",
			description = "Facebook OAuth2 app ID.")
	public String facebookAppId() {
		return getConfigParam("fb_app_id", "");
	}

	@Documented(position = 660,
			identifier = "fb_secret",
			category = "Basic Authentication",
			description = "Facebook app secret key.")
	public String facebookSecret() {
		return getConfigParam("fb_secret", "");
	}

	@Documented(position = 670,
			identifier = "gp_app_id",
			category = "Basic Authentication",
			description = "Google OAuth2 app ID.")
	public String googleAppId() {
		return getConfigParam("gp_app_id", "");
	}

	@Documented(position = 680,
			identifier = "gp_secret",
			category = "Basic Authentication",
			description = "Google app secret key.")
	public String googleSecret() {
		return getConfigParam("gp_secret", "");
	}

	@Documented(position = 690,
			identifier = "in_app_id",
			category = "Basic Authentication",
			description = "LinkedIn OAuth2 app ID.")
	public String linkedinAppId() {
		return getConfigParam("in_app_id", "");
	}

	@Documented(position = 700,
			identifier = "in_secret",
			category = "Basic Authentication",
			description = "LinkedIn app secret key.")
	public String linkedinSecret() {
		return getConfigParam("in_secret", "");
	}

	@Documented(position = 710,
			identifier = "tw_app_id",
			category = "Basic Authentication",
			description = "Twitter OAuth app ID.")
	public String twitterAppId() {
		return getConfigParam("tw_app_id", "");
	}

	@Documented(position = 720,
			identifier = "tw_secret",
			category = "Basic Authentication",
			description = "Twitter app secret key.")
	public String twitterSecret() {
		return getConfigParam("tw_secret", "");
	}

	@Documented(position = 730,
			identifier = "gh_app_id",
			category = "Basic Authentication",
			description = "GitHub OAuth2 app ID.")
	public String githubAppId() {
		return getConfigParam("gh_app_id", "");
	}

	@Documented(position = 740,
			identifier = "gh_secret",
			category = "Basic Authentication",
			description = "GitHub app secret key.")
	public String githubSecret() {
		return getConfigParam("gh_secret", "");
	}

	@Documented(position = 750,
			identifier = "ms_app_id",
			category = "Basic Authentication",
			description = "Microsoft OAuth2 app ID.")
	public String microsoftAppId() {
		return getConfigParam("ms_app_id", "");
	}

	@Documented(position = 760,
			identifier = "ms_secret",
			category = "Basic Authentication",
			description = "Microsoft app secret key.")
	public String microsoftSecret() {
		return getConfigParam("ms_secret", "");
	}

	@Documented(position = 770,
			identifier = "ms_tenant_id",
			value = "common",
			category = "Basic Authentication",
			description = "Microsoft OAuth2 tenant ID")
	public String microsoftTenantId() {
		return getConfigParam("ms_tenant_id", "common");
	}

	@Documented(position = 780,
			identifier = "az_app_id",
			category = "Basic Authentication",
			description = "Amazon OAuth2 app ID.")
	public String amazonAppId() {
		return getConfigParam("az_app_id", "");
	}

	@Documented(position = 790,
			identifier = "az_secret",
			category = "Basic Authentication",
			description = "Amazon app secret key.")
	public String amazonSecret() {
		return getConfigParam("az_secret", "");
	}

	@Documented(position = 800,
			identifier = "sl_app_id",
			category = "Basic Authentication",
			tags = {"Pro"},
			description = "Slack OAuth2 app ID.")
	public String slackAppId() {
		return getConfigParam("sl_app_id", "");
	}

	@Documented(position = 810,
			identifier = "sl_secret",
			category = "Basic Authentication",
			tags = {"Pro"},
			description = "Slack app secret key.")
	public String slackSecret() {
		return getConfigParam("sl_secret", "");
	}

	@Documented(position = 820,
			identifier = "mm_app_id",
			category = "Basic Authentication",
			tags = {"Pro"},
			description = "Mattermost OAuth2 app ID.")
	public String mattermostAppId() {
		return getConfigParam("mm_app_id", "");
	}

	@Documented(position = 830,
			identifier = "mm_secret",
			category = "Basic Authentication",
			tags = {"Pro"},
			description = "Mattermost app secret key.")
	public String mattermostSecret() {
		return getConfigParam("mm_secret", "");
	}

	@Documented(position = 840,
			identifier = "security.custom.provider",
			value = "Continue with Acme Co.",
			category = "Basic Authentication",
			tags = {"Pro"},
			description = "The text on the button for signing in with the custom authentication scheme.")
	public String customLoginProvider() {
		return getConfigParam("security.custom.provider", "Continue with Acme Co.");
	}

	@Documented(position = 850,
			identifier = "security.custom.login_url",
			category = "Basic Authentication",
			tags = {"Pro"},
			description = "The URL address of an externally hosted, custom login page.")
	public String customLoginUrl() {
		return getConfigParam("security.custom.login_url", "");
	}

	/* **************************************************************************************************************
	 * LDAP Authentication                                                                      LDAP Authentication *
	 ****************************************************************************************************************/

	@Documented(position = 860,
			identifier = "security.ldap.server_url",
			category = "LDAP Authentication",
			description = "LDAP server URL. LDAP will be disabled if this is blank.")
	public String ldapServerUrl() {
		return getConfigParam("security.ldap.server_url", "");
	}

	@Documented(position = 870,
			identifier = "security.ldap.base_dn",
			category = "LDAP Authentication",
			description = "LDAP base DN.")
	public String ldapBaseDN() {
		return getConfigParam("security.ldap.base_dn", "");
	}

	@Documented(position = 880,
			identifier = "security.ldap.user_search_base",
			category = "LDAP Authentication",
			description = "LDAP search base, which will be used only if a direct bind is unsuccessfull.")
	public String ldapUserSearchBase() {
		return getConfigParam("security.ldap.user_search_base", "");
	}

	@Documented(position = 890,
			identifier = "security.ldap.user_search_filter",
			value = "(cn={0})",
			category = "LDAP Authentication",
			description = "LDAP search filter, for finding users if a direct bind is unsuccessful.")
	public String ldapUserSearchFilter() {
		return getConfigParam("security.ldap.user_search_filter", "(cn={0})");
	}

	@Documented(position = 900,
			identifier = "security.ldap.user_dn_pattern",
			value = "uid={0}",
			category = "LDAP Authentication",
			description = "LDAP user DN pattern, which will be comined with the base DN to form the full path to the"
					+ "user object, for a direct binding attempt.")
	public String ldapUserDNPattern() {
		return getConfigParam("security.ldap.user_dn_pattern", "uid={0}");
	}

	@Documented(position = 901,
			identifier = "security.ldap.ad_mode_enabled",
			value = "false",
			type = Boolean.class,
			category = "LDAP Authentication",
			description = "Enable/disable support for authenticating with Active Directory. If `true`, AD is enabled.")
	public Boolean ldapActiveDirectoryEnabled() {
		return getConfigBoolean("security.ldap.ad_mode_enabled", false);
	}

	@Documented(position = 910,
			identifier = "security.ldap.active_directory_domain",
			category = "LDAP Authentication",
			description = "AD domain name. Add this *only* if you are connecting to an Active Directory server.")
	public String ldapActiveDirectoryDomain() {
		return getConfigParam("security.ldap.active_directory_domain", "");
	}

	@Documented(position = 920,
			identifier = "security.ldap.password_attribute",
			value = "userPassword",
			category = "LDAP Authentication",
			description = "LDAP password attribute name.")
	public String ldapPasswordAttributeName() {
		return getConfigParam("security.ldap.password_attribute", "userPassword");
	}

	@Documented(position = 930,
			identifier = "security.ldap.bind_dn",
			category = "LDAP Authentication",
			description = "LDAP bind DN")
	public String ldapBindDN() {
		return getConfigParam("security.ldap.bind_dn", "");
	}

	@Documented(position = 940,
			identifier = "security.ldap.bind_pass",
			category = "LDAP Authentication",
			description = "LDAP bind password.")
	public String ldapBindPassword() {
		return getConfigParam("security.ldap.bind_pass", "");
	}

	@Documented(position = 950,
			identifier = "security.ldap.username_as_name",
			value = "false",
			type = Boolean.class,
			category = "LDAP Authentication",
			description = "Enable/disable the use of usernames for names on Scoold.")
	public boolean ldapUsernameAsName() {
		return getConfigBoolean("security.ldap.username_as_name", false);
	}

	@Documented(position = 960,
			identifier = "security.ldap.provider",
			value = "Continue with LDAP",
			category = "LDAP Authentication",
			tags = {"Pro"},
			description = "The text on the LDAP sign in button.")
	public String ldapProvider() {
		return getConfigParam("security.ldap.provider", "Continue with LDAP");
	}

	@Documented(position = 970,
			identifier = "security.ldap.mods_group_node",
			category = "LDAP Authentication",
			description = "Moderators group mapping, mapping LDAP users with this node, to moderators on Scoold.")
	public String ldapModeratorsGroupNode() {
		return getConfigParam("security.ldap.mods_group_node", "");
	}

	@Documented(position = 980,
			identifier = "security.ldap.admins_group_node",
			category = "LDAP Authentication",
			description = "Administrators group mapping, mapping LDAP users with this node, to administrators on Scoold.")
	public String ldapAdministratorsGroupNode() {
		return getConfigParam("security.ldap.admins_group_node", "");
	}

	@Documented(position = 990,
			identifier = "security.ldap.compare_passwords",
			category = "LDAP Authentication",
			description = "LDAP compare passwords.")
	public String ldapComparePasswords() {
		return getConfigParam("security.ldap.compare_passwords", "");
	}

	@Documented(position = 1000,
			identifier = "security.ldap.password_param",
			value = "password",
			category = "LDAP Authentication",
			description = "LDAP password parameter name.")
	public String ldapPasswordParameter() {
		return getConfigParam("security.ldap.password_param", "password");
	}

	@Documented(position = 1010,
			identifier = "security.ldap.username_param",
			value = "username",
			category = "LDAP Authentication",
			description = "LDAP username parameter name.")
	public String ldapUsernameParameter() {
		return getConfigParam("security.ldap.username_param", "username");
	}

	@Documented(position = 1020,
			identifier = "security.ldap.is_local",
			value = "false",
			type = Boolean.class,
			category = "LDAP Authentication",
			tags = {"Pro"},
			description = "Enable/disable local handling of LDAP requests, instead of sending those to Para.")
	public boolean ldapIsLocal() {
		return getConfigBoolean("security.ldap.is_local", false);
	}

	/* **************************************************************************************************************
	 * SAML Authentication                                                                      SAML Authentication *
	 ****************************************************************************************************************/

	@Documented(position = 1030,
			identifier = "security.saml.idp.metadata_url",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML metadata URL. Scoold will fetch most of the necessary information for the authentication"
					+ " request from that XML document. This will overwrite all other IDP settings.")
	public String samlIDPMetadataUrl() {
		return getConfigParam("security.saml.idp.metadata_url", "");
	}

	@Documented(position = 1040,
			identifier = "security.saml.sp.entityid",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML SP endpoint address - e.g. `https://paraio.com/saml_auth/scoold`. The IDP will call "
					+ "this address for authentication.")
	public String samlSPEntityId() {
		if (samlIsLocal()) {
			return serverUrl() + serverContextPath() + "/saml_auth";
		}
		return getConfigParam("security.saml.sp.entityid", "");
	}

	@Documented(position = 1050,
			identifier = "security.saml.sp.x509cert",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML client x509 certificate for the SP (public key). **Value must be Base64-encoded**.")
	public String samlSPX509Certificate() {
		return getConfigParam("security.saml.sp.x509cert", "");
	}

	@Documented(position = 1060,
			identifier = "security.saml.sp.privatekey",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML client private key in PKCS#8 format for the SP. **Value must be Base64-encoded**.")
	public String samlSPX509PrivateKey() {
		return getConfigParam("security.saml.sp.privatekey", "");
	}

	@Documented(position = 1070,
			identifier = "security.saml.attributes.id",
			value = "UserID",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML attribute name of the user `id`.")
	public String samlIdAttribute() {
		return getConfigParam("security.saml.attributes.id", "UserID");
	}

	@Documented(position = 1080,
			identifier = "security.saml.attributes.picture",
			value = "Picture",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML attribute name of the user `picture`.")
	public String samlPictureAttribute() {
		return getConfigParam("security.saml.attributes.picture", "Picture");
	}

	@Documented(position = 1090,
			identifier = "security.saml.attributes.email",
			value = "EmailAddress",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML attribute name of the user `email`.")
	public String samlEmailAttribute() {
		return getConfigParam("security.saml.attributes.email", "EmailAddress");
	}

	@Documented(position = 1100,
			identifier = "security.saml.attributes.name",
			value = "GivenName",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML attribute name of the user `name`.")
	public String samlNameAttribute() {
		return getConfigParam("security.saml.attributes.name", "GivenName");
	}

	@Documented(position = 1110,
			identifier = "security.saml.attributes.firstname",
			value = "FirstName",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML attribute name of the user `firstname`.")
	public String samlFirstNameAttribute() {
		return getConfigParam("security.saml.attributes.firstname", "FirstName");
	}

	@Documented(position = 1120,
			identifier = "security.saml.attributes.lastname",
			value = "LastName",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML attribute name of the user `lastname`.")
	public String samlLastNameAttribute() {
		return getConfigParam("security.saml.attributes.lastname", "LastName");
	}

	@Documented(position = 1130,
			identifier = "security.saml.provider",
			value = "Continue with SAML",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "The text on the button for signing in with SAML.")
	public String samlProvider() {
		return getConfigParam("security.saml.provider", "Continue with SAML");
	}

	@Documented(position = 1140,
			identifier = "security.saml.sp.assertion_consumer_service.url",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML ACS URL.")
	public String samlSPAssertionConsumerServiceUrl() {
		return getConfigParam("security.saml.sp.assertion_consumer_service.url", samlIsLocal() ? samlSPEntityId() : "");
	}

	@Documented(position = 1150,
			identifier = "security.saml.sp.nameidformat",
			value = "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML name id format.")
	public String samlSPNameIdFormat() {
		return getConfigParam("security.saml.sp.nameidformat", "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
	}

	@Documented(position = 1160,
			identifier = "security.saml.idp.entityid",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML IDP entity id for manually setting the endpoint address of the IDP, instead of getting "
					+ "it from the provided metadata URL.")
	public String samlIDPEntityId() {
		return getConfigParam("security.saml.idp.entityid", "");
	}

	@Documented(position = 1170,
			identifier = "security.saml.idp.single_sign_on_service.url",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML SSO service URL of the IDP.")
	public String samlIDPSingleSignOnServiceUrl() {
		return getConfigParam("security.saml.idp.single_sign_on_service.url", "");
	}

	@Documented(position = 1180,
			identifier = "security.saml.idp.x509cert",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML server x509 certificate for the IDP (public key). **Value must be Base64-encoded**.")
	public String samlIDPX509Certificate() {
		return getConfigParam("security.saml.idp.x509cert", "");
	}

	@Documented(position = 1190,
			identifier = "security.saml.security.authnrequest_signed",
			value = "false",
			type = Boolean.class,
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "Enable/disable SAML authentication request signing.")
	public boolean samlAuthnRequestSigningEnabled() {
		return getConfigBoolean("security.saml.security.authnrequest_signed", false);
	}

	@Documented(position = 1200,
			identifier = "security.saml.security.want_messages_signed",
			value = "false",
			type = Boolean.class,
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "Enable/disable SAML message signing.")
	public boolean samlMessageSigningEnabled() {
		return getConfigBoolean("security.saml.security.want_messages_signed", false);
	}

	@Documented(position = 1210,
			identifier = "security.saml.security.want_assertions_signed",
			value = "false",
			type = Boolean.class,
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "Enable/disable SAML assertion signing.")
	public boolean samlAssertionSigningEnabled() {
		return getConfigBoolean("security.saml.security.want_assertions_signed", false);
	}

	@Documented(position = 1220,
			identifier = "security.saml.security.want_assertions_encrypted",
			value = "false",
			type = Boolean.class,
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "Enable/disable SAML assertion encryption.")
	public boolean samlAssertionEncryptionEnabled() {
		return getConfigBoolean("security.saml.security.want_assertions_encrypted", false);
	}

	@Documented(position = 1230,
			identifier = "security.saml.security.want_nameid_encrypted",
			value = "false",
			type = Boolean.class,
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "Enable/disable SAML NameID encryption.")
	public boolean samlNameidEncryptionEnabled() {
		return getConfigBoolean("security.saml.security.want_nameid_encrypted", false);
	}

	@Documented(position = 1231,
			identifier = "security.saml.security.want_nameid",
			value = "true",
			type = Boolean.class,
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "Enable/disable SAML NameID requirement.")
	public boolean samlNameidEnabled() {
		return getConfigBoolean("security.saml.security.want_nameid", true);
	}

	@Documented(position = 1240,
			identifier = "security.saml.security.sign_metadata",
			value = "false",
			type = Boolean.class,
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "Enable/disable SAML metadata signing.")
	public boolean samlMetadataSigningEnabled() {
		return getConfigBoolean("security.saml.security.sign_metadata", false);
	}

	@Documented(position = 1250,
			identifier = "security.saml.security.want_xml_validation",
			value = "true",
			type = Boolean.class,
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "Enable/disable SAML XML validation.")
	public boolean samlXMLValidationEnabled() {
		return getConfigBoolean("security.saml.security.want_xml_validation", true);
	}

	@Documented(position = 1260,
			identifier = "security.saml.security.signature_algorithm",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML signature algorithm.")
	public String samlSignatureAlgorithm() {
		return getConfigParam("security.saml.security.signature_algorithm", "");
	}

	@Documented(position = 1270,
			identifier = "security.saml.domain",
			value = "paraio.com",
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "SAML domain name.")
	public String samlDomain() {
		return getConfigParam("security.saml.domain", "paraio.com");
	}

	@Documented(position = 1280,
			identifier = "security.saml.is_local",
			value = "false",
			type = Boolean.class,
			category = "SAML Authentication",
			tags = {"Pro"},
			description = "Enable/disable local handling of SAML requests, instead of sending those to Para.")
	public boolean samlIsLocal() {
		return getConfigBoolean("security.saml.is_local", false);
	}

	/* **************************************************************************************************************
	 * OAuth 2.0 authentication                                                            OAuth 2.0 authentication *
	 ****************************************************************************************************************/

	@Documented(position = 1290,
			identifier = "oa2_app_id",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 client app identifier. Alternatives: `oa2second_app_id`, `oa2third_app_id`")
	public String oauthAppId(String a) {
		return getConfigParam("oa2" + a + "_app_id", "");
	}

	@Documented(position = 1300,
			identifier = "oa2_secret",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 client app secret key. Alternatives: `oa2second_secret`, `oa2third_secret`")
	public String oauthSecret(String a) {
		return getConfigParam("oa2" + a + "_secret", "");
	}

	@Documented(position = 1310,
			identifier = "security.oauth.authz_url",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 client app authorization URL (login page). Alternatives: "
					+ "`security.oauthsecond.authz_url`, `security.oauththird.authz_url`")
	public String oauthAuthorizationUrl(String a) {
		return getConfigParam("security.oauth" + a + ".authz_url", "");
	}

	@Documented(position = 1320,
			identifier = "security.oauth.token_url",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 client app token endpoint URL. Alternatives: `security.oauthsecond.token_url`, "
					+ "`security.oauththird.token_url`")
	public String oauthTokenUrl(String a) {
		return getConfigParam("security.oauth" + a + ".token_url", "");
	}

	@Documented(position = 1330,
			identifier = "security.oauth.profile_url",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 client app user info endpoint URL. Alternatives: `security.oauthsecond.profile_url`, "
					+ "`security.oauththird.profile_url`")
	public String oauthProfileUrl(String a) {
		return getConfigParam("security.oauth" + a + ".profile_url", "");
	}

	@Documented(position = 1340,
			identifier = "security.oauth.scope",
			value = "openid email profile",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 client app scope. Alternatives: `security.oauthsecond.scope`, "
					+ "`security.oauththird.scope`")
	public String oauthScope(String a) {
		return getConfigParam("security.oauth" + a + ".scope", "openid email profile");
	}

	@Documented(position = 1350,
			identifier = "security.oauth.accept_header",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 `Accept` header customization. Alternatives: `security.oauthsecond.accept_header`, "
					+ "`security.oauththird.accept_header`")
	public String oauthAcceptHeader(String a) {
		return getConfigParam("security.oauth" + a + ".accept_header", "");
	}

	@Documented(position = 1360,
			identifier = "security.oauth.parameters.id",
			value = "sub",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 attribute mapping for `id`. Alternatives: `security.oauthsecond.parameters.id`, "
					+ "`security.oauththird.parameters.id`")
	public String oauthIdParameter(String a) {
		return getConfigParam("security.oauth" + a + ".parameters.id", null);
	}

	@Documented(position = 1370,
			identifier = "security.oauth.parameters.name",
			value = "name",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 attribute mapping for `name`. Alternatives: `security.oauthsecond.parameters.name`, "
					+ "`security.oauththird.parameters.name`")
	public String oauthNameParameter(String a) {
		return getConfigParam("security.oauth" + a + ".parameters.name", null);
	}

	@Documented(position = 1380,
			identifier = "security.oauth.parameters.given_name",
			value = "given_name",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 attribute mapping for `given_name`. Alternatives: "
					+ "`security.oauthsecond.parameters.given_name`, `security.oauththird.parameters.given_name`")
	public String oauthGivenNameParameter(String a) {
		return getConfigParam("security.oauth" + a + ".parameters.given_name", null);
	}

	@Documented(position = 1390,
			identifier = "security.oauth.parameters.family_name",
			value = "family_name",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 attribute mapping for `family_name`. Alternatives: "
					+ "`security.oauthsecond.parameters.family_name`, `security.oauththird.parameters.family_name`")
	public String oauthFamiliNameParameter(String a) {
		return getConfigParam("security.oauth" + a + ".parameters.family_name", null);
	}

	@Documented(position = 1400,
			identifier = "security.oauth.parameters.email",
			value = "email",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 attribute mapping for `email`. Alternatives: `security.oauthsecond.parameters.email`, "
					+ "`security.oauththird.parameters.email`")
	public String oauthEmailParameter(String a) {
		return getConfigParam("security.oauth" + a + ".parameters.email", null);
	}

	@Documented(position = 1410,
			identifier = "security.oauth.parameters.picture",
			value = "picture",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 attribute mapping for `picture`. Alternatives: `security.oauthsecond.parameters.picture`, "
					+ "`security.oauththird.parameters.picture`")
	public String oauthPictureParameter(String a) {
		return getConfigParam("security.oauth" + a + ".parameters.picture", null);
	}

	@Documented(position = 1420,
			identifier = "security.oauth.download_avatars",
			value = "false",
			type = Boolean.class,
			category = "OAuth 2.0 Authentication",
			description = "Enable/disable OAauth 2.0 avatar downloading to local disk. Used when avatars are large in size. "
					+ "Alternatives: `security.oauthsecond.download_avatars`, `security.oauththird.download_avatars`")
	public boolean oauthAvatarDownloadingEnabled(String a) {
		return getConfigBoolean("security.oauth" + a + ".download_avatars", false);
	}

	@Documented(position = 1430,
			identifier = "security.oauth.token_delegation_enabled",
			value = "false",
			type = Boolean.class,
			category = "OAuth 2.0 Authentication",
			tags = {"Pro"},
			description = "Enable/disable OAauth 2.0 token delegation. The ID and access tokens will be saved and "
					+ "delegated to Scoold from Para. Alternatives: `security.oauthsecond.token_delegation_enabled`, "
					+ "`security.oauththird.token_delegation_enabled`")
	public boolean oauthTokenDelegationEnabled(String a) {
		return getConfigBoolean("security.oauth" + a + ".token_delegation_enabled", false);
	}

	@Documented(position = 1440,
			identifier = "security.oauth.spaces_attribute_name",
			value = "spaces",
			category = "OAuth 2.0 Authentication",
			tags = {"Pro"},
			description = "OAauth 2.0 attribute mapping for users' `spaces`. The spaces can be comma-separated. "
					+ "Alternatives: `security.oauthsecond.spaces_attribute_name`, "
					+ "`security.oauththird.spaces_attribute_name`")
	public String oauthSpacesAttributeName(String a) {
		return getConfigParam("security.oauth" + a + ".spaces_attribute_name", "spaces");
	}

	@Documented(position = 1450,
			identifier = "security.oauth.groups_attribute_name",
			category = "OAuth 2.0 Authentication",
			tags = {"Pro"},
			description = "OAauth 2.0 attribute mapping for users' `groups`. "
					+ "Use this for mapping `admin`, `mod` and `user` roles to Scoold users."
					+ "Alternatives: `security.oauthsecond.groups_attribute_name`, "
					+ "`security.oauththird.groups_attribute_name`")
	public String oauthGroupsAttributeName(String a) {
		return getConfigParam("security.oauth" + a + ".groups_attribute_name", "");
	}

	@Documented(position = 1460,
			identifier = "security.oauth.mods_equivalent_claim_value",
			value = "mod",
			category = "OAuth 2.0 Authentication",
			tags = {"Pro"},
			description = "OAauth 2.0 claim used for mapping OAuth2 users having it, "
					+ "to moderators on Scoold. Alternatives: `security.oauthsecond.mods_equivalent_claim_value`, "
					+ "`security.oauththird.mods_equivalent_claim_value`")
	public String oauthModeratorsEquivalentClaim(String a) {
		return getConfigParam("security.oauth" + a + ".mods_equivalent_claim_value", "mod");
	}

	@Documented(position = 1470,
			identifier = "security.oauth.admins_equivalent_claim_value",
			value = "admin",
			category = "OAuth 2.0 Authentication",
			tags = {"Pro"},
			description = "OAauth 2.0 claim used for mapping OAuth2 users having it, "
					+ "to administrators on Scoold. Alternatives: `security.oauthsecond.admins_equivalent_claim_value`, "
					+ "`security.oauththird.admins_equivalent_claim_value`")
	public String oauthAdministratorsEquivalentClaim(String a) {
		return getConfigParam("security.oauth" + a + ".admins_equivalent_claim_value", "admin");
	}

	@Documented(position = 1480,
			identifier = "security.oauth.users_equivalent_claim_value",
			category = "OAuth 2.0 Authentication",
			tags = {"Pro"},
			description = "OAauth 2.0 claim used for **denying access** to OAuth2 users **not** having it, *unless*"
					+ "they already have the admin or moderator roles assigned. "
					+ "Alternatives: `security.oauthsecond.users_equivalent_claim_value`, "
					+ "`security.oauththird.users_equivalent_claim_value`")
	public String oauthUsersEquivalentClaim(String a) {
		return getConfigParam("security.oauth" + a + ".users_equivalent_claim_value", "");
	}

	@Documented(position = 1490,
			identifier = "security.oauth.domain",
			category = "OAuth 2.0 Authentication",
			description = "OAauth 2.0 domain name for constructing user email addresses in case they are missing. "
					+ "Alternatives: `security.oauthsecond.domain`, `security.oauththird.domain`")
	public String oauthDomain(String a) {
		return getConfigParam("security.oauth" + a + ".domain", null);
	}

	@Documented(position = 1500,
			identifier = "security.oauth.provider",
			value = "Continue with OpenID Connect",
			category = "OAuth 2.0 Authentication",
			description = "The text on the button for signing in with OAuth2 or OIDC.")
	public String oauthProvider(String a) {
		return getConfigParam("security.oauth" + a + ".provider", "Continue with " + a + "OpenID Connect");
	}

	@Documented(position = 1501,
			identifier = "security.oauth.appid_in_state_param_enabled",
			value = "true",
			type = Boolean.class,
			category = "OAuth 2.0 Authentication",
			description = "Enable/disable the use of the OAauth 2.0 state parameter to designate your Para app id. "
					+ "Some OAauth 2.0 servers throw errors if the length of the state parameter is less than 8 chars.")
	public boolean oauthAppidInStateParamEnabled(String a) {
		return getConfigBoolean("security.oauth" + a + ".appid_in_state_param_enabled", true);
	}

	/* **************************************************************************************************************
	 * Posts                                                                                                  Posts *
	 ****************************************************************************************************************/

	@Documented(position = 1510,
			identifier = "new_users_can_comment",
			value = "true",
			type = Boolean.class,
			category = "Posts",
			description = "Enable/disable the ability for users with reputation below 100 to comments on posts.")
	public boolean newUsersCanComment() {
		return getConfigBoolean("new_users_can_comment", true);
	}

	@Documented(position = 1520,
			identifier = "posts_need_approval",
			value = "false",
			type = Boolean.class,
			category = "Posts",
			description = "Enable/disable the need for approval of new posts by a moderator. ")
	public boolean postsNeedApproval() {
		return getConfigBoolean("posts_need_approval", false);
	}

	@Documented(position = 1521,
			identifier = "answers_approved_by",
			value = "default",
			category = "Posts",
			description = "Controls who is able to mark an answer as accepted/approved. "
					+ "Possible values are `default` (author and moderators), `admins` (admins only), `moderators` "
					+ "(moderators and admins).")
	public String answersApprovedBy() {
		return getConfigParam("answers_approved_by", "default");
	}

	@Documented(position = 1530,
			identifier = "wiki_answers_enabled",
			value = "true",
			type = Boolean.class,
			category = "Posts",
			tags = {"Pro"},
			description = "Enable/disable the ability for users to create wiki-style answers, editable by everyone.")
	public boolean wikiAnswersEnabled() {
		return getConfigBoolean("wiki_answers_enabled", true);
	}

	@Documented(position = 1540,
			identifier = "media_recording_allowed",
			value = "true",
			type = Boolean.class,
			category = "Posts",
			tags = {"Pro"},
			description = "Enable/disable support for attaching recorded videos and voice messages to posts.")
	public boolean mediaRecordingAllowed() {
		return getConfigBoolean("media_recording_allowed", true);
	}

	@Documented(position = 1550,
			identifier = "delete_protection_enabled",
			value = "true",
			type = Boolean.class,
			category = "Posts",
			description = "Enable/disable the ability for authors to delete their own question, when it already has "
					+ "answers and activity.")
	public boolean deleteProtectionEnabled() {
		return getConfigBoolean("delete_protection_enabled", true);
	}

	@Documented(position = 1560,
			identifier = "max_text_length",
			value = "20000",
			type = Integer.class,
			category = "Posts",
			description = "The maximum text length of each post (question or answer). Longer content will be truncated.")
	public int maxPostLength() {
		return getConfigInt("max_post_length", 20000);
	}

	@Documented(position = 1570,
			identifier = "max_tags_per_post",
			value = "5",
			type = Integer.class,
			category = "Posts",
			description = "The maximum number of tags a question can have. The minimum is 0 - then the default tag is used.")
	public int maxTagsPerPost() {
		return getConfigInt("max_tags_per_post", 5);
	}

	@Documented(position = 1571,
			identifier = "min_tags_per_post",
			value = "0",
			type = Integer.class,
			category = "Posts",
			description = "The minimum number of tags a question must have. The minimum is 0.")
	public int minTagsPerPost() {
		return getConfigInt("min_tags_per_post", 0);
	}

	@Documented(position = 1580,
			identifier = "max_replies_per_post",
			value = "500",
			type = Integer.class,
			category = "Posts",
			description = "The maximum number of answers a question can have.")
	public int maxRepliesPerPost() {
		return getConfigInt("max_replies_per_post", 500);
	}

	@Documented(position = 1590,
			identifier = "max_comments_per_id",
			value = "1000",
			type = Integer.class,
			category = "Posts",
			description = "The maximum number of comments a post can have.")
	public int maxCommentsPerPost() {
		return getConfigInt("max_comments_per_id", 1000);
	}

	@Documented(position = 1600,
			identifier = "max_comment_length",
			value = "600",
			type = Integer.class,
			category = "Posts",
			description = "The maximum length of each comment.")
	public int maxCommentLength() {
		return getConfigInt("max_comment_length", 600);
	}

	@Documented(position = 1610,
			identifier = "max_mentions_in_posts",
			value = "10",
			type = Integer.class,
			category = "Posts",
			tags = {"Pro"},
			description = "The maximum number of mentioned users a post can have.")
	public int maxMentionsInPosts() {
		return getConfigInt("max_mentions_in_posts", 10);
	}

	@Documented(position = 1620,
			identifier = "anonymous_posts_enabled",
			value = "false",
			type = Boolean.class,
			category = "Posts",
			tags = {"Pro"},
			description = "Enable/disable the ability for unathenticated users to create new questions.")
	public boolean anonymousPostsEnabled() {
		return getConfigBoolean("anonymous_posts_enabled", false);
	}

	@Documented(position = 1630,
			identifier = "nearme_feature_enabled",
			value = "false",
			type = Boolean.class,
			category = "Posts",
			description = "Enable/disable the ability for users to attach geolocation data to questions and "
					+ "location-based filtering of questions.")
	public boolean postsNearMeEnabled() {
		return getConfigBoolean("nearme_feature_enabled", !googleMapsApiKey().isEmpty());
	}

	@Documented(position = 1640,
			identifier = "merge_question_bodies",
			value = "true",
			type = Boolean.class,
			category = "Posts",
			description = "Enable/disable the merging of question bodies when two questions are merged into one.")
	public boolean mergeQuestionBodies() {
		return getConfigBoolean("merge_question_bodies", true);
	}

	@Documented(position = 1650,
			identifier = "max_similar_posts",
			value = "7",
			type = Integer.class,
			category = "Posts",
			description = "The maximum number of similar posts which will be displayed on the side.")
	public int maxSimilarPosts() {
		return getConfigInt("max_similar_posts", 7);
	}

	@Documented(position = 1660,
			identifier = "default_question_tag",
			category = "Posts",
			description = "The default question tag, used when no other tags are provided by its author.")
	public String defaultQuestionTag() {
		return getConfigParam("default_question_tag", "");
	}

	@Documented(position = 1670,
			identifier = "posts_rep_threshold",
			value = "100",
			type = Integer.class,
			category = "Posts",
			description = "The minimum reputation an author needs to create a post without approval by moderators. "
					+ "This is only required if new posts need apporval.")
	public int postsReputationThreshold() {
		return getConfigInt("posts_rep_threshold", enthusiastIfHasRep());
	}

	/* **************************************************************************************************************
	 * Spaces                                                                                                Spaces *
	 ****************************************************************************************************************/

	@Documented(position = 1680,
			identifier = "auto_assign_spaces",
			category = "Spaces",
			description = "A comma-separated list of spaces to assign to all new users.")
	public String autoAssignSpaces() {
		return getConfigParam("auto_assign_spaces", "");
	}

	@Documented(position = 1690,
			identifier = "reset_spaces_on_new_assignment",
			value = "true",
			type = Boolean.class,
			category = "Spaces",
			description = "Spaces delegated from identity providers will overwrite the existing ones for users.")
	public boolean resetSpacesOnNewAssignment(boolean def) {
		return getConfigBoolean("reset_spaces_on_new_assignment", def);
	}

	/* **************************************************************************************************************
	 * Reputation and Rewards                                                                Reputation and Rewards *
	 ****************************************************************************************************************/

	@Documented(position = 1700,
			identifier = "answer_voteup_reward_author",
			value = "10",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points given to author of answer as reward when a user upvotes it.")
	public int answerVoteupRewardAuthor() {
		return getConfigInt("answer_voteup_reward_author", 10);
	}

	@Documented(position = 1710,
			identifier = "question_voteup_reward_author",
			value = "5",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points given to author of question as reward when a user upvotes it.")
	public int questionVoteupRewardAuthor() {
		return getConfigInt("question_voteup_reward_author", 5);
	}

	@Documented(position = 1720,
			identifier = "voteup_reward_author",
			value = "2",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points given to author of comment or other post as reward when a user upvotes it.")
	public int voteupRewardAuthor() {
		return getConfigInt("voteup_reward_author", 2);
	}

	@Documented(position = 1730,
			identifier = "answer_approve_reward_author",
			value = "10",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points given to author of answer as reward when the question's author accepts it.")
	public int answerApprovedRewardAuthor() {
		return getConfigInt("answer_approve_reward_author", 10);
	}

	@Documented(position = 1740,
			identifier = "answer_approve_reward_voter",
			value = "3",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points given to author of question who accepted an answer.")
	public int answerApprovedRewardVoter() {
		return getConfigInt("answer_approve_reward_voter", 3);
	}

	@Documented(position = 1750,
			identifier = "post_votedown_penalty_author",
			value = "3",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points taken from author of post as penalty when their post was downvoted.")
	public int postVotedownPenaltyAuthor() {
		return getConfigInt("post_votedown_penalty_author", 3);
	}

	@Documented(position = 1760,
			identifier = "post_votedown_penalty_voter",
			value = "1",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points taken from the user who downvotes any content. Discourages downvoting slightly.")
	public int postVotedownPenaltyVoter() {
		return getConfigInt("post_votedown_penalty_voter", 1);
	}

	@Documented(position = 1770,
			identifier = "voter_ifhas",
			value = "100",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Number of votes (up or down) needed from a user for earning the `voter` badge.")
	public int voterIfHasRep() {
		return getConfigInt("voter_ifhas", 100);
	}

	@Documented(position = 1780,
			identifier = "commentator_ifhas",
			value = "100",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Number of comments a user needs to have posted for earning the `commentator` badge.")
	public int commentatorIfHasRep() {
		return getConfigInt("commentator_ifhas", 100);
	}

	@Documented(position = 1790,
			identifier = "critic_ifhas",
			value = "10",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Number of cast downvotes needed from a user for earning the `critic` badge.")
	public int criticIfHasRep() {
		return getConfigInt("critic_ifhas", 10);
	}

	@Documented(position = 1800,
			identifier = "supporter_ifhas",
			value = "50",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Number of cast upvotes needed from a user for earning the `supporter` badge.")
	public int supporterIfHasRep() {
		return getConfigInt("supporter_ifhas", 50);
	}

	@Documented(position = 1810,
			identifier = "goodquestion_ifhas",
			value = "20",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Votes needed on a question before its author gets to earn the `good question` badge.")
	public int goodQuestionIfHasRep() {
		return getConfigInt("goodquestion_ifhas", 20);
	}

	@Documented(position = 1820,
			identifier = "goodanswer_ifhas",
			value = "10",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Votes needed on an answer before its author gets to earn the `good answer` badge.")
	public int goodAnswerIfHasRep() {
		return getConfigInt("goodanswer_ifhas", 10);
	}

	@Documented(position = 1830,
			identifier = "enthusiast_ifhas",
			value = "100",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points needed for earning the `enthusiast` badge.")
	public int enthusiastIfHasRep() {
		return getConfigInt("enthusiast_ifhas", 100);
	}

	@Documented(position = 1840,
			identifier = "freshman_ifhas",
			value = "300",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points needed for earning the `freshman` badge.")
	public int freshmanIfHasRep() {
		return getConfigInt("freshman_ifhas", 300);
	}

	@Documented(position = 1850,
			identifier = "scholar_ifhas",
			value = "500",
			type = Boolean.class,
			category = "Reputation and Rewards",
			description = "Reputation points needed for earning the `scholar` badge.")
	public int scholarIfHasRep() {
		return getConfigInt("scholar_ifhas", 500);
	}

	@Documented(position = 1860,
			identifier = "teacher_ifhas",
			value = "1000",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points needed for earning the `teacher` badge.")
	public int teacherIfHasRep() {
		return getConfigInt("teacher_ifhas", 1000);
	}

	@Documented(position = 1870,
			identifier = "",
			value = "5000",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points needed for earning the `professor` badge.")
	public int professorIfHasRep() {
		return getConfigInt("professor_ifhas", 5000);
	}

	@Documented(position = 1880,
			identifier = "geek_ifhas",
			value = "9000",
			type = Integer.class,
			category = "Reputation and Rewards",
			description = "Reputation points needed for earning the `geek` badge.")
	public int geekIfHasRep() {
		return getConfigInt("geek_ifhas", 9000);
	}

	/* **************************************************************************************************************
	 * File Storage                                                                                    File Storage *
	 ****************************************************************************************************************/

	@Documented(position = 1890,
			identifier = "uploads_enabled",
			value = "true",
			type = Boolean.class,
			category = "File Storage",
			tags = {"Pro"},
			description = "Enable/disable file uploads.")
	public boolean uploadsEnabled() {
		return getConfigBoolean("uploads_enabled", true);
	}

	@Documented(position = 1900,
			identifier = "file_uploads_dir",
			value = "uploads",
			category = "File Storage",
			tags = {"Pro"},
			description = "The directory (local or in the cloud) where files will be stored.")
	public String fileUploadsDirectory() {
		return getConfigParam("file_uploads_dir", "uploads");
	}

	@Documented(position = 1910,
			identifier = "uploads_require_auth",
			value = "false",
			type = Boolean.class,
			category = "File Storage",
			tags = {"Pro"},
			description = "Enable/disable the requirement that uploaded files can only be accessed by authenticated users.")
	public boolean uploadsRequireAuthentication() {
		return getConfigBoolean("uploads_require_auth", !isDefaultSpacePublic());
	}

	@Documented(position = 1920,
			identifier = "allowed_upload_formats",
			category = "File Storage",
			tags = {"Pro"},
			description = "A comma-separated list of allowed MIME types in the format `extension:mime_type`, e.g."
					+ "`py:text/plain` or just the extensions `py,yml`")
	public String allowedUploadFormats() {
		return getConfigParam("allowed_upload_formats", "");
	}

	@Documented(position = 1930,
			identifier = "s3_bucket",
			category = "File Storage",
			tags = {"Pro"},
			description = "AWS S3 bucket name as target for storing files.")
	public String s3Bucket() {
		return getConfigParam("s3_bucket", "");
	}

	@Documented(position = 1940,
			identifier = "s3_path",
			category = "File Storage",
			tags = {"Pro"},
			description = "AWS S3 object prefix (directory) inside the bucket.")
	public String s3Path() {
		return getConfigParam("s3_path", "uploads");
	}

	@Documented(position = 1950,
			identifier = "s3_region",
			category = "File Storage",
			tags = {"Pro"},
			description = "AWS S3 region.")
	public String s3Region() {
		return getConfigParam("s3_region", "");
	}

	@Documented(position = 1951,
			identifier = "s3_endpoint",
			category = "File Storage",
			tags = {"Pro"},
			description = "AWS S3 endpoint override. The S3 region will be ignored if this is set. "
					+ "Can be used for connecting to S3-compatible storage providers.")
	public String s3Endpoint() {
		return getConfigParam("s3_endpoint", "");
	}

	@Documented(position = 1960,
			identifier = "s3_access_key",
			category = "File Storage",
			tags = {"Pro"},
			description = "AWS S3 access key.")
	public String s3AccessKey() {
		return getConfigParam("s3_access_key", "");
	}

	@Documented(position = 1970,
			identifier = "s3_secret_key",
			category = "File Storage",
			tags = {"Pro"},
			description = "AWS S3 secret key.")
	public String s3SecretKey() {
		return getConfigParam("s3_secret_key", "");
	}

	@Documented(position = 1980,
			identifier = "blob_storage_account",
			category = "File Storage",
			tags = {"Pro"},
			description = "Azure Blob Storage account ID.")
	public String azureStorageAccount() {
		return getConfigParam("blob_storage_account", "");
	}

	@Documented(position = 1990,
			identifier = "blob_storage_token",
			category = "File Storage",
			tags = {"Pro"},
			description = "Azure Blob Storage token.")
	public String azureStorageToken() {
		return getConfigParam("blob_storage_token", "");
	}

	@Documented(position = 2000,
			identifier = "blob_storage_container",
			category = "File Storage",
			tags = {"Pro"},
			description = "Azure Blob Storage container.")
	public String azureStorageContainer() {
		return getConfigParam("blob_storage_container", "");
	}

	@Documented(position = 2010,
			identifier = "blob_storage_path",
			category = "File Storage",
			tags = {"Pro"},
			description = "Azure Blob Storage path prefix (subfolder) within a container.")
	public String azureStoragePath() {
		return getConfigParam("blob_storage_path", "uploads");
	}

	/* **************************************************************************************************************
	 * Customization                                                                                  Customization *
	 ****************************************************************************************************************/

	@Documented(position = 2020,
			identifier = "default_language_code",
			category = "Customization",
			description = "The default language code to use for the site. Set this to make the site load a "
					+ "different language from English.")
	public String defaultLanguageCode() {
		return getConfigParam("default_language_code", "");
	}

	@Documented(position = 2030,
			identifier = "welcome_message",
			category = "Customization",
			description = "Adds a brief intro text inside a banner at the top of the main page for new visitors to see.")
	public String welcomeMessage() {
		return getConfigParam("welcome_message", "");
	}

	@Documented(position = 2040,
			identifier = "welcome_message_onlogin",
			category = "Customization",
			description = "Adds a brief intro text inside a banner at the top of the 'Sign in' page only.")
	public String welcomeMessageOnLogin() {
		return getConfigParam("welcome_message_onlogin", "");
	}

	@Documented(position = 2050,
			identifier = "dark_mode_enabled",
			value = "true",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable the option for users to switch to the dark theme.")
	public boolean darkModeEnabled() {
		return getConfigBoolean("dark_mode_enabled", true);
	}

	@Documented(position = 2060,
			identifier = "meta_description",
			value = "Scoold is friendly place for knowledge sharing and collaboration...",
			category = "Customization",
			description = "The content inside the description `<meta>` tag.")
	public String metaDescription() {
		return getConfigParam("meta_description", appName() + " is friendly place for knowledge sharing and collaboration. "
				+ "Ask questions, post answers and comments, earn reputation points.");
	}

	@Documented(position = 2070,
			identifier = "meta_keywords",
			value = "knowledge base, knowledge sharing, collaboration, wiki...",
			category = "Customization",
			description = "The content inside the keywords `<meta>` tag.")
	public String metaKeywords() {
		return getConfigParam("meta_keywords", "knowledge base, knowledge sharing, collaboration, wiki, "
				+ "forum, Q&A, questions and answers, internal communication, project management, issue tracker, "
				+ "bug tracker, support tool");
	}

	@Documented(position = 2080,
			identifier = "show_branding",
			value = "true",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable the 'Powered by Scoold' branding in the footer.")
	public boolean scooldBrandingEnabled() {
		return getConfigBoolean("show_branding", true);
	}

	@Documented(position = 2090,
			identifier = "mathjax_enabled",
			value = "false",
			type = Boolean.class,
			category = "Customization",
			tags = {"Pro"},
			description = "Enable/disable support for MathJax and LaTeX for scientific expressions in Markdown.")
	public boolean mathjaxEnabled() {
		return getConfigBoolean("mathjax_enabled", false);
	}

	@Documented(position = 2100,
			identifier = "gravatars_enabled",
			value = "true",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable support for Gravatars.")
	public boolean gravatarsEnabled() {
		return getConfigBoolean("gravatars_enabled", true);
	}

	@Documented(position = 2110,
			identifier = "gravatars_pattern",
			value = "retro",
			category = "Customization",
			description = "The pattern to use when displaying empty/anonymous gravatar pictures.")
	public String gravatarsPattern() {
		return getConfigParam("gravatars_pattern", "retro");
	}

	@Documented(position = 2120,
			identifier = "avatar_repository",
			category = "Customization",
			tags = {"preview"},
			description = "The avatar repository - one of `imgur`, `cloudinary`.")
	public String avatarRepository() {
		return getConfigParam("avatar_repository", "");
	}

	@Documented(position = 2130,
			identifier = "footer_html",
			category = "Customization",
			description = "Some custom HTML content to be added to the website footer.")
	public String footerHtml() {
		return getConfigParam("footer_html", "");
	}

	@Documented(position = 2140,
			identifier = "navbar_link1_url",
			category = "Customization",
			description = "The URL of an extra custom link which will be added to the top navbar.")
	public String navbarCustomLink1Url() {
		return getConfigParam("navbar_link1_url", "");
	}

	@Documented(position = 2150,
			identifier = "navbar_link1_text",
			value = "Link1",
			category = "Customization",
			description = "The title of an extra custom link which will be added to the top navbar.")
	public String navbarCustomLink1Text() {
		return getConfigParam("navbar_link1_text", "Link1");
	}

	@Documented(position = 2151,
		identifier = "navbar_link1_target",
		category = "Customization",
		description = "The target attribute of an extra custom link which will be added to the top navbar.")
	public String navbarCustomLink1Target() {
		return getConfigParam("navbar_link1_target", "");
	}
	@Documented(position = 2160,
			identifier = "navbar_link2_url",
			category = "Customization",
			description = "The URL of an extra custom link which will be added to the top navbar.")
	public String navbarCustomLink2Url() {
		return getConfigParam("navbar_link2_url", "");
	}

	@Documented(position = 2170,
			identifier = "navbar_link2_text",
			value = "Link2",
			category = "Customization",
			description = "The title of an extra custom link which will be added to the top navbar.")
	public String navbarCustomLink2Text() {
		return getConfigParam("navbar_link2_text", "Link2");
	}

	@Documented(position = 2171,
		identifier = "navbar_link2_target",
		category = "Customization",
		description = "The target attribute of an extra custom link which will be added to the top navbar.")
	public String navbarCustomLink2Target() {
		return getConfigParam("navbar_link2_target", "");
	}

	@Documented(position = 2180,
			identifier = "navbar_menu_link1_url",
			category = "Customization",
			description = "The URL of an extra custom link which will be added to user's dropdown menu."
					+ " Only shown to authenticated users.")
	public String navbarCustomMenuLink1Url() {
		return getConfigParam("navbar_menu_link1_url", "");
	}

	@Documented(position = 2190,
			identifier = "navbar_menu_link1_text",
			value = "Menu Link1",
			category = "Customization",
			description = "The title of an extra custom link which will be added to the user's dropdown menu.")
	public String navbarCustomMenuLink1Text() {
		return getConfigParam("navbar_menu_link1_text", "Menu Link1");
	}

	@Documented(position = 2191,
		identifier = "navbar_menu_link1_target",
		category = "Customization",
		description = "The target attribute of an extra custom link which will be added to user's dropdown menu.")
	public String navbarCustomMenuLink1Target() {
		return getConfigParam("navbar_menu_link1_target", "");
	}

	@Documented(position = 2200,
			identifier = "navbar_menu_link2_url",
			category = "Customization",
			description = "The URL of an extra custom link which will be added to user's dropdown menu."
					+ " Only shown to authenticated users.")
	public String navbarCustomMenuLink2Url() {
		return getConfigParam("navbar_menu_link2_url", "");
	}

	@Documented(position = 2210,
			identifier = "navbar_menu_link2_text",
			value = "Menu Link2",
			category = "Customization",
			description = "The title of an extra custom link which will be added to the user's dropdown menu.")
	public String navbarCustomMenuLink2Text() {
		return getConfigParam("navbar_menu_link2_text", "Menu Link2");
	}

	@Documented(position = 2211,
		identifier = "navbar_menu_link2_target",
		category = "Customization",
		description = "The target attribute of an extra custom link which will be added to the user's dropdown menu.")
	public String navbarCustomMenuLink2Target() {
		return getConfigParam("navbar_menu_link2_target", "");
	}

	@Documented(position = 2220,
			identifier = "always_hide_comment_forms",
			value = "true",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable a visual tweak which keeps all comment text editors closed at all times.")
	public boolean alwaysHideCommentForms() {
		return getConfigBoolean("always_hide_comment_forms", true);
	}

	@Documented(position = 2230,
			identifier = "footer_links_enabled",
			value = "true",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable all links in the website footer.")
	public boolean footerLinksEnabled() {
		return getConfigBoolean("footer_links_enabled", true);
	}

	@Documented(position = 2240,
			identifier = "emails_footer_html",
			value = "<a href=\"{host_url}\">{app_name}</a> &bull; <a href=\"https://scoold.com\">Powered by Scoold</a>",
			category = "Customization",
			description = "The HTML code snippet to embed at the end of each transactional email message.")
	public String emailsFooterHtml() {
		return getConfigParam("emails_footer_html", "<a href=\"" + serverUrl() + serverContextPath() + "\">" +
				appName() + "</a> &bull; " + "<a href=\"https://scoold.com\">Powered by Scoold</a>");
	}

	@Documented(position = 2250,
			identifier = "cookie_consent_required",
			value = "false",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable the cookie consent popup box and blocks all external JS scripts from loading. "
					+ "Used for compliance with GDPR/CCPA.")
	public boolean cookieConsentRequired() {
		return getConfigBoolean("cookie_consent_required", false);
	}

	@Documented(position = 2260,
			identifier = "fixed_nav",
			value = "false",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable a fixed navigation bar.")
	public boolean fixedNavEnabled() {
		return getConfigBoolean("fixed_nav", false);
	}

	@Documented(position = 2270,
			identifier = "logo_width",
			value = "100",
			type = Integer.class,
			category = "Customization",
			description = "The width of the logo image in the nav bar, in pixels. Used for fine adjustments to the logo size.")
	public int logoWidth() {
		return getConfigInt("logo_width", 100);
	}

	@Documented(position = 2280,
			identifier = "code_highlighting_enabled",
			value = "true",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable support for syntax highlighting in code blocks.")
	public boolean codeHighlightingEnabled() {
		return getConfigBoolean("code_highlighting_enabled", true);
	}

	@Documented(position = 2290,
			identifier = "max_pages",
			value = "1000",
			type = Integer.class,
			category = "Customization",
			description = "Maximum number of pages to return as results.")
	public int maxPages() {
		return getConfigInt("max_pages", 1000);
	}

	@Documented(position = 2300,
			identifier = "numeric_pagination_enabled",
			value = "false",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable the numeric pagination style `(< 1 2 3...N >)`.")
	public boolean numericPaginationEnabled() {
		return getConfigBoolean("numeric_pagination_enabled", false);
	}

	@Documented(position = 2310,
			identifier = "html_in_markdown_enabled",
			value = "false",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable the ability for users to insert basic HTML tags inside Markdown content.")
	public boolean htmlInMarkdownEnabled() {
		return getConfigBoolean("html_in_markdown_enabled", false);
	}

	@Documented(position = 2320,
			identifier = "max_items_per_page",
			value = "30",
			type = Integer.class,
			category = "Customization",
			description = "Maximum number of results to return in a single page of results.")
	public int maxItemsPerPage() {
		return getConfigInt("max_items_per_page", Para.getConfig().maxItemsPerPage());
	}

	@Documented(position = 2330,
			identifier = "avatar_edits_enabled",
			value = "true",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable the ability for users to edit their profile pictures.")
	public boolean avatarEditsEnabled() {
		return getConfigBoolean("avatar_edits_enabled", true);
	}

	@Documented(position = 2340,
			identifier = "name_edits_enabled",
			value = "true",
			type = Boolean.class,
			category = "Customization",
			description = "Enable/disable the ability for users to edit their name.")
	public boolean nameEditsEnabled() {
		return getConfigBoolean("name_edits_enabled", true);
	}

	/* **************************************************************************************************************
	 * Frontend Assets                                                                              Frontend Assets *
	 ****************************************************************************************************************/

	@Documented(position = 2350,
			identifier = "logo_url",
			value = "/images/logo.svg",
			category = "Frontend Assets",
			description = "The URL of the logo in the nav bar. Use a PNG, SVG, JPG or WebP format.")
	public String logoUrl() {
		return getConfigParam("logo_url", imagesLink() + "/logo.svg");
	}

	@Documented(position = 2351,
			identifier = "logo_dark_url",
			value = "/images/logo.svg",
			category = "Frontend Assets",
			description = "The URL of the logo in the nav bar used in dark mode. Use a PNG, SVG, JPG or WebP format.")
	public String logoDarkUrl() {
		return getConfigParam("logo_dark_url", logoUrl());
	}

	@Documented(position = 2360,
			identifier = "small_logo_url",
			value = "/images/logowhite.png",
			category = "Frontend Assets",
			description = "The URL of a smaller logo (only use PNG/JPG!). Used in transactional emails and the meta `og:image`.")
	public String logoSmallUrl() {
		return getConfigParam("small_logo_url", serverUrl() + imagesLink() + "/logowhite.png");
	}

	@Documented(position = 2370,
			identifier = "cdn_url",
			category = "Frontend Assets",
			description = "A CDN URL where all static assets might be stored.")
	public String cdnUrl() {
		return StringUtils.stripEnd(getConfigParam("cdn_url", serverContextPath()), "/");
	}

	@Documented(position = 2380,
			identifier = "stylesheet_url",
			value = "/styles/style.css",
			category = "Frontend Assets",
			description = "A stylesheet URL of a CSS file which will be used as the main stylesheet. *This will overwrite"
					+ " all existing CSS styles!*")
	public String stylesheetUrl() {
		return getConfigParam("stylesheet_url", stylesLink() + "/style.css");
	}

	@Documented(position = 2381,
			identifier = "dark_stylesheet_url",
			value = "/styles/dark.css",
			category = "Frontend Assets",
			description = "A stylesheet URL of a CSS file which will be used when dark mode is enabled. *This will overwrite"
					+ " all existing dark CSS styles!*")
	public String darkStylesheetUrl() {
		return getConfigParam("dark_stylesheet_url", stylesLink() + "/dark.css");
	}

	@Documented(position = 2390,
			identifier = "external_styles",
			category = "Frontend Assets",
			description = "A comma-separated list of external CSS files. These will be loaded *after* the main stylesheet.")
	public String externalStyles() {
		return getConfigParam("external_styles", "");
	}

	@Documented(position = 2400,
			identifier = "external_scripts._id_",
			type = Map.class,
			category = "Frontend Assets",
			description = "A map of external JS scripts. These will be loaded after the main JS script. For example: "
					+ "`scoold.external_scripts.script1 = \"alert('Hi')\"`")
	public Map<String, Object> externalScripts() {
		if (getConfig().hasPath("external_scripts")) {
			ConfigObject extScripts = getConfig().getObject("external_scripts");
			if (extScripts != null && !extScripts.isEmpty()) {
				return new LinkedHashMap<>(extScripts.unwrapped());
			}
		}
		return Collections.emptyMap();
	}

	@Documented(position = 2410,
			identifier = "inline_css",
			category = "Frontend Assets",
			description = "Some short, custom CSS snippet to embed inside the `<head>` element.")
	public String inlineCSS() {
		return getConfigParam("inline_css", "");
	}

	@Documented(position = 2420,
			identifier = "favicon_url",
			value = "/images/favicon.ico",
			category = "Frontend Assets",
			description = "The URL of the favicon image.")
	public String faviconUrl() {
		return getConfigParam("favicon_url", imagesLink() + "/favicon.ico");
	}

	@Documented(position = 2430,
			identifier = "meta_app_icon",
			value = "/images/logowhite.png",
			category = "Frontend Assets",
			description = "The URL of the app icon image in the `<meta property='og:image'>` tag.")
	public String metaAppIconUrl() {
		return getConfigParam("meta_app_icon", logoSmallUrl());
	}

	/* **************************************************************************************************************
	 * Mattermost Integration                                                                Mattermost Integration *
	 ****************************************************************************************************************/

	@Documented(position = 2431,
			identifier = "mattermost.auth_enabled",
			value = "false",
			type = Boolean.class,
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Enable/disable authentication with Mattermost.")
	public boolean mattermostAuthEnabled() {
		return getConfigBoolean("mattermost.auth_enabled", !mattermostAppId().isEmpty());
	}

	@Documented(position = 2440,
			identifier = "mattermost.server_url",
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Mattermost server URL.")
	public String mattermostServerUrl() {
		return getConfigParam("mattermost.server_url", "");
	}

	@Documented(position = 2450,
			identifier = "mattermost.bot_username",
			value = "scoold",
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Mattermost bot username.")
	public String mattermostBotUsername() {
		return getConfigParam("mattermost.bot_username", "scoold");
	}

	@Documented(position = 2460,
			identifier = "mattermost.bot_icon_url",
			value = "/images/logowhite.png",
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Mattermost bot avatar URL.")
	public String mattermostBotIconUrl() {
		return getConfigParam("mattermost.bot_icon_url", serverUrl() + imagesLink() + "/logowhite.png");
	}

	@Documented(position = 2470,
			identifier = "mattermost.post_to_space",
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Default space on Scoold where questions created on Mattermost will be published. Set it to "
					+ "`workspace` for using the team's name.")
	public String mattermostPostToSpace() {
		return getConfigParam("mattermost.post_to_space", "");
	}

	@Documented(position = 2480,
			identifier = "mattermost.map_channels_to_spaces",
			value = "false",
			type = Boolean.class,
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Enable/disable mapping of Mattermost channels to Scoold spaces. When enabled, will create a "
					+ "Scoold space for each Mattermost channel.")
	public boolean mattermostMapChannelsToSpaces() {
		return getConfigBoolean("mattermost.map_channels_to_spaces", false);
	}

	@Documented(position = 2490,
			identifier = "mattermost.map_workspaces_to_spaces",
			value = "true",
			type = Boolean.class,
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Enable/disable mapping of Mattermost teams to Scoold spaces. When enabled, will create a "
					+ "Scoold space for each Mattermost team.")
	public boolean mattermostMapWorkspacesToSpaces() {
		return getConfigBoolean("mattermost.map_workspaces_to_spaces", true);
	}

	@Documented(position = 2500,
			identifier = "mattermost.max_notification_webhooks",
			value = "10",
			type = Integer.class,
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "The maximum number of incoming webhooks which can be created on Scoold. Each webhook links a"
					+ " Mattermost channel to Scoold.")
	public int mattermostMaxNotificationWebhooks() {
		return getConfigInt("mattermost.max_notification_webhooks", 10);
	}

	@Documented(position = 2510,
			identifier = "mattermost.notify_on_new_answer",
			value = "true",
			type = Boolean.class,
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send notifications to Mattermost for new answers.")
	public boolean mattermostNotifyOnNewAnswer() {
		return getConfigBoolean("mattermost.notify_on_new_answer", true);
	}

	@Documented(position = 2520,
			identifier = "mattermost.notify_on_new_question",
			value = "true",
			type = Boolean.class,
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send notifications to Mattermost for new questions.")
	public boolean mattermostNotifyOnNewQuestion() {
		return getConfigBoolean("mattermost.notify_on_new_question", true);
	}

	@Documented(position = 2530,
			identifier = "mattermost.notify_on_new_comment",
			value = "true",
			type = Boolean.class,
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send notifications to Mattermost for new comments.")
	public boolean mattermostNotifyOnNewComment() {
		return getConfigBoolean("mattermost.notify_on_new_comment", true);
	}

	@Documented(position = 2540,
			identifier = "mattermost.dm_on_new_comment",
			value = "false",
			type = Boolean.class,
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send direct messages to Mattermost users for new comments.")
	public boolean mattermostDmOnNewComment() {
		return getConfigBoolean("mattermost.dm_on_new_comment", false);
	}

	@Documented(position = 2550,
			identifier = "mattermost.default_question_tags",
			value = "via-mattermost",
			category = "Mattermost Integration",
			tags = {"Pro"},
			description = "Default question tags for questions created on Mattermost (comma-separated list).")
	public String mattermostDefaultQuestionTags() {
		return getConfigParam("mattermost.default_question_tags", "via-mattermost");
	}

	/* **************************************************************************************************************
	 * Slack Integration                                                                          Slack Integration *
	 ****************************************************************************************************************/

	@Documented(position = 2560,
			identifier = "slack.auth_enabled",
			value = "false",
			type = Boolean.class,
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Enable/disable authentication with Slack.")
	public boolean slackAuthEnabled() {
		return getConfigBoolean("slack.auth_enabled", !slackAppId().isEmpty());
	}

	@Documented(position = 2570,
			identifier = "slack.app_id",
			category = "Slack Integration",
			tags = {"Pro"},
			description = "The Slack app ID (first ID from the app's credentials, not the OAuth2 Client ID).")
	public String slackIntegrationAppId() {
		return getConfigParam("slack.app_id", "");
	}

	@Documented(position = 2580,
			identifier = "slack.signing_secret",
			value = "x",
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Slack signing secret key for verifying request signatures.")
	public String slackSigningSecret() {
		return getConfigParam("slack.signing_secret", "x");
	}

	@Documented(position = 2590,
			identifier = "slack.max_notification_webhooks",
			value = "10",
			type = Integer.class,
			category = "Slack Integration",
			tags = {"Pro"},
			description = "The maximum number of incoming webhooks which can be created on Scoold. Each webhook links a"
					+ " Slack channel to Scoold.")
	public int slackMaxNotificationWebhooks() {
		return getConfigInt("slack.max_notification_webhooks", 10);
	}

	@Documented(position = 2600,
			identifier = "slack.map_channels_to_spaces",
			value = "false",
			type = Boolean.class,
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Enable/disable mapping of Slack channels to Scoold spaces. When enabled, will create a "
					+ "Scoold space for each Slack channel.")
	public boolean slackMapChannelsToSpaces() {
		return getConfigBoolean("slack.map_channels_to_spaces", false);
	}

	@Documented(position = 2610,
			identifier = "slack.map_workspaces_to_spaces",
			value = "true",
			type = Boolean.class,
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Enable/disable mapping of Slack teams to Scoold spaces. When enabled, will create a "
					+ "Scoold space for each Slack team.")
	public boolean slackMapWorkspacesToSpaces() {
		return getConfigBoolean("slack.map_workspaces_to_spaces", true);
	}

	@Documented(position = 2620,
			identifier = "slack.post_to_space",
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Default space on Scoold where questions created on Slack will be published. Set it to "
					+ "`workspace` for using the team's name.")
	public String slackPostToSpace() {
		return getConfigParam("slack.post_to_space", "");
	}

	@Documented(position = 2630,
			identifier = "slack.default_title",
			value = "A question from Slack",
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Default question title for questions created on Slack.")
	public String slackDefaultQuestionTitle() {
		return getConfigParam("slack.default_title", "A question from Slack");
	}

	@Documented(position = 2640,
			identifier = "slack.notify_on_new_answer",
			value = "true",
			type = Boolean.class,
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send notifications to Slack for new answers.")
	public boolean slackNotifyOnNewAnswer() {
		return getConfigBoolean("slack.notify_on_new_answer", true);
	}

	@Documented(position = 2650,
			identifier = "slack.notify_on_new_question",
			value = "true",
			type = Boolean.class,
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send notifications to Slack for new questions.")
	public boolean slackNotifyOnNewQuestion() {
		return getConfigBoolean("slack.notify_on_new_question", true);
	}

	@Documented(position = 2660,
			identifier = "slack.notify_on_new_comment",
			value = "true",
			type = Boolean.class,
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send notifications to Slack for new comments.")
	public boolean slackNotifyOnNewComment() {
		return getConfigBoolean("slack.notify_on_new_comment", true);
	}

	@Documented(position = 2670,
			identifier = "slack.dm_on_new_comment",
			value = "false",
			type = Boolean.class,
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send direct messages to Slack users for new comments.")
	public boolean slacDmOnNewComment() {
		return getConfigBoolean("slack.dm_on_new_comment", false);
	}

	@Documented(position = 2680,
			identifier = "slack.default_question_tags",
			value = "via-slack",
			category = "Slack Integration",
			tags = {"Pro"},
			description = "Default question tags for questions created on Slack (comma-separated list).")
	public String slackDefaultQuestionTags() {
		return getConfigParam("slack.default_question_tags", "via-slack");
	}

	/* **************************************************************************************************************
	 * Microsoft Teams Integration                                                      Microsoft Teams Integration *
	 ****************************************************************************************************************/

	@Documented(position = 2681,
			identifier = "teams.auth_enabled",
			value = "false",
			type = Boolean.class,
			category = "Microsoft Teams Integration",
			description = "Enable/disable authentication with Microsoft.")
	public boolean teamsAuthEnabled() {
		return getConfigBoolean("teams.auth_enabled", !microsoftAppId().isEmpty());
	}

	@Documented(position = 2690,
			identifier = "teams.bot_id",
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Teams bot ID.")
	public String teamsBotId() {
		return getConfigParam("teams.bot_id", "");
	}

	@Documented(position = 2700,
			identifier = "teams.bot_secret",
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Teams bot secret key.")
	public String teamsBotSecret() {
		return getConfigParam("teams.bot_secret", "");
	}

	@Documented(position = 2710,
			identifier = "teams.bot_service_url",
			value = "https://smba.trafficmanager.net/emea/",
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Teams bot service URL.")
	public String teamsBotServiceUrl() {
		return getConfigParam("teams.bot_service_url", "https://smba.trafficmanager.net/emea/");
	}

	@Documented(position = 2720,
			identifier = "teams.notify_on_new_answer",
			value = "true",
			type = Boolean.class,
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send notifications to Teams for new answers.")
	public boolean teamsNotifyOnNewAnswer() {
		return getConfigBoolean("teams.notify_on_new_answer", true);
	}

	@Documented(position = 2730,
			identifier = "teams.notify_on_new_question",
			value = "true",
			type = Boolean.class,
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send notifications to Teams for new questions.")
	public boolean teamsNotifyOnNewQuestion() {
		return getConfigBoolean("teams.notify_on_new_question", true);
	}

	@Documented(position = 2740,
			identifier = "teams.notify_on_new_comment",
			value = "true",
			type = Boolean.class,
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send notifications to Teams for new comments.")
	public boolean teamsNotifyOnNewComment() {
		return getConfigBoolean("teams.notify_on_new_comment", true);
	}

	@Documented(position = 2750,
			identifier = "teams.dm_on_new_comment",
			value = "false",
			type = Boolean.class,
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Enable/disable the ability for Scoold to send direct messages to Teams users for new comments.")
	public boolean teamsDmOnNewComment() {
		return getConfigBoolean("teams.dm_on_new_comment", false);
	}

	@Documented(position = 2760,
			identifier = "teams.post_to_space",
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Default space on Scoold where questions created on Teams will be published. Set it to "
					+ "`workspace` for using the team's name.")
	public String teamsPostToSpace() {
		return getConfigParam("teams.post_to_space", "");
	}

	@Documented(position = 2770,
			identifier = "teams.map_channels_to_spaces",
			value = "false",
			type = Boolean.class,
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Enable/disable mapping of Teams channels to Scoold spaces. When enabled, will create a "
					+ "Scoold space for each Teams channel.")
	public boolean teamsMapChannelsToSpaces() {
		return getConfigBoolean("teams.map_channels_to_spaces", false);
	}

	@Documented(position = 2780,
			identifier = "teams.map_workspaces_to_spaces",
			value = "true",
			type = Boolean.class,
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Enable/disable mapping of Teams teams to Scoold spaces. When enabled, will create a "
					+ "Scoold space for each Teams team.")
	public boolean teamsMapWorkspacesToSpaces() {
		return getConfigBoolean("teams.map_workspaces_to_spaces", true);
	}

	@Documented(position = 2790,
			identifier = "teams.max_notification_webhooks",
			value = "10",
			type = Integer.class,
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "The maximum number of incoming webhooks which can be created on Scoold. Each webhook links a"
					+ " Teams channel to Scoold.")
	public int teamsMaxNotificationWebhooks() {
		return getConfigInt("teams.max_notification_webhooks", 10);
	}

	@Documented(position = 2800,
			identifier = "teams.default_title",
			value = "A question from Microsoft Teams",
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Default question title for questions created on Teams.")
	public String teamsDefaultQuestionTitle() {
		return getConfigParam("teams.default_title", "A question from Microsoft Teams");
	}

	@Documented(position = 2810,
			identifier = "teams.default_question_tags",
			value = "via-teams",
			category = "Microsoft Teams Integration",
			tags = {"Pro"},
			description = "Default question tags for questions created on Teams (comma-separated list).")
	public String teamsDefaultQuestionTags() {
		return getConfigParam("teams.default_question_tags", "via-teams");
	}

	/* **************************************************************************************************************
	 * SCIM                                                                                                    SCIM *
	 ****************************************************************************************************************/

	@Documented(position = 2820,
			identifier = "scim_enabled",
			value = "false",
			type = Boolean.class,
			category = "SCIM",
			tags = {"Pro", "preview"},
			description = "Enable/disable support for SCIM user provisioning.")
	public boolean scimEnabled() {
		return getConfigBoolean("scim_enabled", false);
	}

	@Documented(position = 2830,
			identifier = "scim_secret_token",
			category = "SCIM",
			tags = {"Pro", "preview"},
			description = "SCIM secret token.")
	public String scimSecretToken() {
		return getConfigParam("scim_secret_token", "");
	}

	@Documented(position = 2840,
			identifier = "scim_allow_provisioned_users_only",
			value = "false",
			type = Boolean.class,
			category = "SCIM",
			tags = {"Pro", "preview"},
			description = "Enable/disable the restriction that only SCIM-provisioned users can sign in.")
	public boolean scimAllowProvisionedUsersOnly() {
		return getConfigBoolean("scim_allow_provisioned_users_only", false);
	}

	@Documented(position = 2850,
			identifier = "scim_map_groups_to_spaces",
			value = "true",
			type = Boolean.class,
			category = "SCIM",
			tags = {"Pro", "preview"},
			description = "Enable/disable mapping of SCIM groups to Scoold spaces.")
	public boolean scimMapGroupsToSpaces() {
		return getConfigBoolean("scim_map_groups_to_spaces", true);
	}

	@Documented(position = 2860,
			identifier = "security.scim.admins_group_equivalent_to",
			value = "admins",
			category = "SCIM",
			tags = {"Pro", "preview"},
			description = "SCIM group whose members will be promoted to administrators on Scoold.")
	public String scimAdminsGroupEquivalentTo() {
		return getConfigParam("security.scim.admins_group_equivalent_to", "admins");
	}

	@Documented(position = 2870,
			identifier = "security.scim.mods_group_equivalent_to",
			value = "mods",
			category = "SCIM",
			tags = {"Pro", "preview"},
			description = "SCIM group whose members will be promoted to moderators on Scoold.")
	public String scimModeratorsGroupEquivalentTo() {
		return getConfigParam("security.scim.mods_group_equivalent_to", "mods");
	}

	/* **************************************************************************************************************
	 * Miscellaneous                                                                                  Miscellaneous *
	 ****************************************************************************************************************/

	@Documented(position = 2880,
			identifier = "security.redirect_uri",
			value = "http://localhost:8080",
			category = "Miscellaneous",
			description = "Publicly accessible, internet-facing URL of the Para endpoint where authenticated users "
					+ "will be redirected to, from the identity provider. Used when Para is hosted behind a proxy.")
	public String redirectUri() {
		return getConfigParam("security.redirect_uri", paraEndpoint());
	}

	@Documented(position = 2890,
			identifier = "redirect_signin_to_idp",
			value = "false",
			type = Boolean.class,
			category = "Miscellaneous",
			description = "Enable/disable the redirection of users from the signin page, directly to the IDP login page.")
	public boolean redirectSigninToIdp() {
		return getConfigBoolean("redirect_signin_to_idp", false);
	}

	@Documented(position = 2900,
			identifier = "gmaps_api_key",
			category = "Miscellaneous",
			description = "The Google Maps API key. Used for geolocation functionality, (e.g. 'posts near me', location).")
	public String googleMapsApiKey() {
		return getConfigParam("gmaps_api_key", "");
	}

	@Documented(position = 2910,
			identifier = "imgur_client_id",
			category = "Miscellaneous",
			tags = {"preview"},
			description = "Imgur API client id. Used for uploading avatars to Imgur. **Note:** Imgur have some breaking "
					+ "restrictions going on in their API and this might not work.")
	public String imgurClientId() {
		return getConfigParam("imgur_client_id", "");
	}

	@Documented(position = 2911,
		identifier = "cloudinary_url",
		category = "Miscellaneous",
		tags = {"preview"},
		description = "Cloudinary URL. Used for uploading avatars to Cloudinary.")
	public String cloudinaryUrl() {
		return getConfigParam("cloudinary_url", "");
	}

	@Documented(position = 2920,
			identifier = "max_fav_tags",
			value = "50",
			type = Integer.class,
			category = "Miscellaneous",
			description = "Maximum number of favorite tags.")
	public int maxFavoriteTags() {
		return getConfigInt("max_fav_tags", 50);
	}

	@Documented(position = 2930,
			identifier = "batch_request_size",
			value = "0",
			type = Integer.class,
			category = "Miscellaneous",
			description = "Maximum batch size for the Para client pagination requests.")
	public int batchRequestSize() {
		return getConfigInt("batch_request_size", 0);
	}

	@Documented(position = 2940,
			identifier = "signout_url",
			value = "/signin?code=5&success=true",
			category = "Miscellaneous",
			description = "The URL which users will be redirected to after they click 'Sign out'. Can be a page hosted"
					+ " externally.")
	public String signoutUrl(int... code) {
		if (code == null || code.length < 1) {
			code = new int[]{5};
		}
		return getConfigParam("signout_url", SIGNINLINK + "?code=" + code[0] + "&success=true");
	}

	@Documented(position = 2950,
			identifier = "vote_expires_after_sec",
			value = "2592000",
			type = Integer.class,
			category = "Miscellaneous",
			description = "Vote expiration timeout, in seconds. Users can vote again on the same content after "
					+ "this period has elapsed. Default is 30 days.")
	public int voteExpiresAfterSec() {
		return getConfigInt("vote_expires_after_sec", Para.getConfig().voteExpiresAfterSec());
	}

	@Documented(position = 2960,
			identifier = "vote_locked_after_sec",
			value = "30",
			type = Integer.class,
			category = "Miscellaneous",
			description = "Vote locking period, in seconds. Vote cannot be changed after this period has elapsed. "
					+ "Default is 30 sec.")
	public int voteLockedAfterSec() {
		return getConfigInt("vote_locked_after_sec", Para.getConfig().voteLockedAfterSec());
	}

	@Documented(position = 2961,
			identifier = "downvotes_enabled",
			value = "true",
			type = Boolean.class,
			category = "Miscellaneous",
			description = "Enable/disable negative votes.")
	public boolean downvotesEnabled() {
		return getConfigBoolean("downvotes_enabled", true);
	}

	@Documented(position = 2970,
			identifier = "import_batch_size",
			value = "100",
			type = Integer.class,
			category = "Miscellaneous",
			description = "Maximum number objects to read and send to Para when importing data from a backup.")
	public int importBatchSize() {
		return getConfigInt("import_batch_size", 100);
	}

	@Documented(position = 2980,
			identifier = "connection_retries_max",
			value = "10",
			type = Integer.class,
			category = "Miscellaneous",
			description = "Maximum number of connection retries to Para.")
	public int paraConnectionRetryAttempts() {
		return getConfigInt("connection_retries_max", 10);
	}

	@Documented(position = 2990,
			identifier = "connection_retry_interval_sec",
			value = "10",
			type = Integer.class,
			category = "Miscellaneous",
			description = "Para connection retry interval, in seconds.")
	public int paraConnectionRetryIntervalSec() {
		return getConfigInt("connection_retry_interval_sec", 10);
	}

	@Documented(position = 3000,
			identifier = "rewrite_inbound_links_with_fqdn",
			category = "Miscellaneous",
			description = "If set, links to Scoold in emails will be replaced with a public-facing FQDN.")
	public String rewriteInboundLinksWithFQDN() {
		return getConfigParam("rewrite_inbound_links_with_fqdn", "");
	}

	@Documented(position = 3010,
			identifier = "cluster_nodes",
			value = "1",
			type = Integer.class,
			category = "Miscellaneous",
			description = "Total number of nodes present in the cluster when Scoold is deployed behind a reverse proxy.")
	public int clusterNodes() {
		return getConfigInt("cluster_nodes", 1);
	}

	@Documented(position = 3020,
			identifier = "autoinit.root_app_secret_key",
			category = "Miscellaneous",
			description = "If configured, Scoold will try to automatically initialize itself with Para and create its "
					+ "own Para app, called `app:scoold`. The keys for that new app will be saved in the configuration file.")
	public String autoInitWithRootAppSecretKey() {
		return getConfigParam("autoinit.root_app_secret_key", "");
	}

	@Documented(position = 3030,
			identifier = "autoinit.para_config_file",
			category = "Miscellaneous",
			description = "Does the same as `scoold.autoinit.root_app_secret_key` but tries to read the secret key for"
					+ " the root Para app from the Para configuration file, wherever that may be.")
	public String autoInitWithParaConfigFile() {
		return getConfigParam("autoinit.para_config_file", "");
	}

	@Documented(position = 3040,
			identifier = "sitemap_enabled",
			value = "true",
			type = Boolean.class,
			category = "Miscellaneous",
			description = "Enable/disable the generation of `/sitemap.xml`.")
	public boolean sitemapEnabled() {
		return getConfigBoolean("sitemap_enabled", true);
	}

	@Documented(position = 3050,
			identifier = "access_log_enabled",
			value = "false",
			type = Boolean.class,
			category = "Miscellaneous",
			description = "Enable/disable the Scoold access log.")
	public boolean accessLogEnabled() {
		return getConfigBoolean("access_log_enabled", false);
	}

	@Documented(position = 3060,
			identifier = "user_autocomplete_details_enabled",
			value = "false",
			type = Boolean.class,
			category = "Miscellaneous",
			tags = {"pro"},
			description = "Enable/disable extra details when displaying user results in autocomplete.")
	public boolean userAutocompleteDetailsEnabled() {
		return getConfigBoolean("user_autocomplete_details_enabled", false);
	}

	@Documented(position = 3070,
			identifier = "user_autocomplete_max_results",
			value = "10",
			type = Integer.class,
			category = "Miscellaneous",
			tags = {"pro"},
			description = "Controls the maximum number of search results in users' autocomplete.")
	public int userAutocompleteMaxResults() {
		return getConfigInt("user_autocomplete_max_results", 10);
	}

	/* **********************************************************************************************************/

	public boolean inDevelopment() {
		return environment().equals("development");
	}

	public boolean inProduction() {
		return environment().equals("production");
	}

	public boolean hasValue(String key) {
		return !StringUtils.isBlank(getConfigParam(key, ""));
	}

	private String getAppId() {
		return App.identifier(paraAccessKey());
	}

	public String localeCookie() {
		return getAppId() + "-locale";
	}

	public String spaceCookie() {
		return getAppId() + "-space";
	}

	public String authCookie() {
		return getAppId() + "-auth";
	}

	public String imagesLink() {
		return (inProduction() ? cdnUrl() : serverContextPath()) + "/images";
	}

	public String scriptsLink() {
		return (inProduction() ? cdnUrl() : serverContextPath()) + "/scripts";
	}

	public String stylesLink() {
		return (inProduction() ? cdnUrl() : serverContextPath()) + "/styles";
	}

	public Map<String, Object> oauthSettings(String alias) {
		String a = StringUtils.trimToEmpty(alias);
		Map<String, Object> settings = new LinkedHashMap<>();
		settings.put("oa2" + a + "_app_id", oauthAppId(a));
		settings.put("oa2" + a + "_secret", oauthSecret(a));
		settings.put("security.oauth" + a + ".token_url", oauthTokenUrl(a));
		settings.put("security.oauth" + a + ".profile_url", oauthProfileUrl(a));
		settings.put("security.oauth" + a + ".scope", oauthScope(a));
		settings.put("security.oauth" + a + ".accept_header", oauthAcceptHeader(a));
		settings.put("security.oauth" + a + ".parameters.id", oauthIdParameter(a));
		settings.put("security.oauth" + a + ".parameters.name", oauthNameParameter(a));
		settings.put("security.oauth" + a + ".parameters.given_name", oauthGivenNameParameter(a));
		settings.put("security.oauth" + a + ".parameters.family_name", oauthFamiliNameParameter(a));
		settings.put("security.oauth" + a + ".parameters.email", oauthEmailParameter(a));
		settings.put("security.oauth" + a + ".parameters.picture", oauthPictureParameter(a));
		settings.put("security.oauth" + a + ".download_avatars", oauthAvatarDownloadingEnabled(a));
		settings.put("security.oauth" + a + ".domain", oauthDomain(a));
		settings.put("security.oauth" + a + ".token_delegation_enabled", oauthTokenDelegationEnabled(a));
		return settings;
	}

	public Map<String, Object> ldapSettings() {
		Map<String, Object> settings = new LinkedHashMap<>();
		settings.put("security.ldap.server_url", ldapServerUrl());
		settings.put("security.ldap.base_dn", ldapBaseDN());
		settings.put("security.ldap.bind_dn", ldapBindDN());
		settings.put("security.ldap.bind_pass", ldapBindPassword());
		settings.put("security.ldap.user_search_base", ldapUserSearchBase());
		settings.put("security.ldap.user_search_filter", ldapUserSearchFilter());
		settings.put("security.ldap.user_dn_pattern", ldapUserDNPattern());
		settings.put("security.ldap.password_attribute", ldapPasswordAttributeName());
		settings.put("security.ldap.username_as_name", ldapUsernameAsName());
		settings.put("security.ldap.ad_mode_enabled", ldapActiveDirectoryEnabled());
		settings.put("security.ldap.active_directory_domain", ldapActiveDirectoryDomain());
		settings.put("security.ldap.mods_group_node", ldapModeratorsGroupNode());
		settings.put("security.ldap.admins_group_node", ldapAdministratorsGroupNode());
		if (!ldapComparePasswords().isEmpty()) {
			settings.put("security.ldap.compare_passwords", ldapComparePasswords());
		}
		return settings;
	}

	public Map<String, Object> getParaAppSettings() {
		Map<String, Object> settings = new LinkedHashMap<String, Object>();
		settings.put("gp_app_id", googleAppId());
		settings.put("gp_secret", googleSecret());
		settings.put("fb_app_id", facebookAppId());
		settings.put("fb_secret", facebookSecret());
		settings.put("gh_app_id", githubAppId());
		settings.put("gh_secret", githubSecret());
		settings.put("in_app_id", linkedinAppId());
		settings.put("in_secret", linkedinSecret());
		settings.put("tw_app_id", twitterAppId());
		settings.put("tw_secret", twitterSecret());
		settings.put("ms_app_id", microsoftAppId());
		settings.put("ms_secret", microsoftSecret());
		settings.put("sl_app_id", slackAppId());
		settings.put("sl_secret", slackSecret());
		settings.put("az_app_id", amazonAppId());
		settings.put("az_secret", amazonSecret());
		// Microsoft tenant id support - https://github.com/Erudika/scoold/issues/208
		settings.put("ms_tenant_id", microsoftTenantId());
		// OAuth 2 settings
		settings.putAll(oauthSettings(""));
		settings.putAll(oauthSettings("second"));
		settings.putAll(oauthSettings("third"));
		// LDAP settings
		settings.putAll(ldapSettings());
		// secret key
		settings.put("app_secret_key", appSecretKey());
		// email verification
		settings.put("security.allow_unverified_emails", allowUnverifiedEmails());
		// sessions
		settings.put("security.one_session_per_user", oneSessionPerUser());
		settings.put("session_timeout", sessionTimeoutSec());

		// URLs for success and failure
		settings.put("signin_success", serverUrl() + serverContextPath() + SIGNINLINK + "/success?jwt=id");
		settings.put("signin_failure", serverUrl() + serverContextPath() + SIGNINLINK + "?code=3&error=true");
		return settings;
	}

	String getDefaultContentSecurityPolicy() {
		return "default-src 'self'; "
				+ "base-uri 'self'; "
				+ "media-src 'self' blob:; "
				+ "form-action 'self' " + serverUrl() + serverContextPath() + SIGNOUTLINK + "; "
				+ "connect-src 'self' " + (inProduction() ? serverUrl() : "")
				+ " maps.googleapis.com api.imgur.com api.cloudinary.com accounts.google.com " + cspConnectSources() + "; "
				+ "frame-src 'self' *.google.com staticxx.facebook.com " + cspFrameSources() + "; "
				+ "font-src 'self' cdnjs.cloudflare.com fonts.gstatic.com fonts.googleapis.com " + cspFontSources() + "; "
				// unsafe-inline required by MathJax and Google Maps!
				+ "style-src 'self' 'unsafe-inline' fonts.googleapis.com accounts.google.com "
				+ (cdnUrl().startsWith("/") ? "" : cdnUrl() + " ") + cspStyleSources() + "; "
				+ "img-src 'self' https: data:; "
				+ "object-src 'none'; "
				+ "report-uri /reports/cspv; "
				+ "script-src 'unsafe-inline' https: 'nonce-{{nonce}}' 'strict-dynamic';"; // CSP2 backward compatibility
	}

}
