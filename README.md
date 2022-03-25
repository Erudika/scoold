![Scoold Q&A](https://raw.githubusercontent.com/Erudika/scoold/master/assets/header.png)

## Scoold - Stack Overflow in a JAR

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.erudika%3Ascoold&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.erudika%3Ascoold)
[![Join the chat at https://gitter.im/Erudika/scoold](https://badges.gitter.im/Erudika/scoold.svg)](https://gitter.im/Erudika/scoold?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

**Scoold** is a Q&A/knowledge base platform written in Java. The project was created back in 2008, released in 2012 as
social network for schools inspired by Stack Overflow. In 2017 it was refactored, repackaged and open-sourced.

Scoold can run anywhere - Heroku, DigitalOcean, AWS, Azure or any VPS hosting provider. It's lightweight (~7000 LOC),
the backend is handled by a separate service called [Para](https://github.com/Erudika/para). Scoold does not require a
database, and the controller logic is really simple because all the heavy lifting is delegated to Para.
This makes the code easy to read and can be learned quickly by junior developers.

**Scoold Pro**, the paid version of Scoold, has premium features which make it the perfect knowledge sharing platform for
your company or team.

**This project is fully funded and supported by [Erudika](https://erudika.com) - an independent, bootstrapped company.**

### Features

- Full featured Q&A platform
- Database-agnostic, optimized for cloud deployment
- Full-text search
- Distributed object cache
- Location-based search and "near me" filtering of posts
- I18n with RTL language support
- Reputation and voting system with badges
- Spaces (Teams) - groups of isolated questions and users
- Webhooks with signature signing
- [Zapier integration](https://zapier.com/developer/public-invite/96144/23ee4c1c6f03dc964b479b0d8ed027bb/)
- Minimal frontend JS code based on jQuery
- Modern, responsive layout powered by Materialize CSS
- Suggestions for similar questions and hints for duplicate posts
- Email notifications for post replies and comments
- Backup and Restore
- RESTful API defined with OpenAPI 3.0
- Spring Boot project (single JAR)
- Mutual authentication support (mTLS)
- LDAP authentication support
- Social login (Facebook, Google, GitHub, LinkedIn, Microsoft, Slack, Amazon, Twitter) with Gravatar support
- Syntax highlighting for code in posts, GFM markdown support with tables, task lists and strikethrough
- Import data from Stack Overflow for Teams
- Emoji support - [cheat sheet](https://www.webpagefx.com/tools/emoji-cheat-sheet/)
- Support for uploading custom avatars (to Imgur, Cloudinary)
- SEO friendly
- Cookie consent (for GDPR, CCPA, etc.)

### [Buy Scoold Pro](https://paraio.com/scoold-pro) and also get these premium features:

- [Slack integration](https://scoold.com/slack.html)
- [Mattermost integration](https://scoold.com/mattermost.html)
- [Microsoft Teams integration](https://scoold.com/teams.html)
- SAML authentication support
- Custom authentication support
- SCIM 2.0 for automatic user provisioning
- Mentions with notifications
- File uploads (local, AWS S3, Azure Blob)
- Account suspensions (permabans)
- Anonymous posts
- Unlimited spaces
- Multiple admins
- Multiple identity domains
- Sticky / Favorite posts
- Advanced syntax highlighting
- Email digest of recent questions
- Security notifications
- Wiki-style answers

...and more!

## Live Demos

### [Scoold Demo](https://live.scoold.com)  |  [Scoold Pro Demo](https://pro.scoold.com)

For **admin** access, open the Scoold Pro demo and login with "Demo login".

## Scoold architecture intro (or 'what the heck is Para?')

Scoold is a client application of the [Para](https://paraio.org) backend server. Almost every request to
Scoold produces at least one request to Para as well. When you ask a question on Scoold, a `create` request is
sent to Para to the location `POST /v1/questions`. Here are a few key points about that architecture:
- A Para server can be hosted anywhere and store data inside any of the [supported databases](https://github.com/Erudika/para#database-integrations).
- Para is a multi-tenant server which stores data in isolated environments called "apps".
- One or more instance of Scoold (like a cluster) can connect to the same Para app environment and share the data.
- Multiple separate Scoold websites can be powered by the same Para backend using different Para apps.
- Each app environment is completely independent from others and has its own database table and search index.
- Both Scoold and Para can be hosted on the same machine or on multiple machines, on different networks.
- Scoold talks to Para via HTTP(S) so Para must be directly accessible from Scoold, but can also be hosted on a private network.

Here's an overview of the architecture:
<pre>
                              ┌────────────┐
                          ┌───►  Database  │
┌──────────┐  ┌────────┐  │   ┌────────────┤
│ Scoold 1 ├──►        │  │   └────────────┤
├──────────┤  │  Para  ◄──┼───►   Search   │
│ Scoold 2 ├──►        │  │   ┌────────────┤
└──────────┘  └────────┘  │   └────────────┤
                          └───►   Cache    │
                              └────────────┘
</pre>

### Quick Start with a managed Para backend (easier)

[JDK 11 or higher](https://openjdk.java.net/) is required to build and run the project. All major operating systems are supported.

1. Create a new app on [ParaIO.com](https://paraio.com) and copy your access keys to a file
2. Create Scoold's configuration file named `application.conf` and add the following properties to it:
```ini
scoold.env = "production"
scoold.app_name = "Scoold"
scoold.para_access_key = "app:scoold"
scoold.para_secret_key = "_secret_key_from_para_"
scoold.para_endpoint = "https://paraio.com"
# add your email here
scoold.admins = "my@email.com"
# (optional) require authentication for viewing content
scoold.is_default_space_public = false
```
3. Start Scoold with `java -jar -Dconfig.file=./application.conf scoold-*.jar`
4. Open [localhost:8000/signin/register](http://localhost:8000/signin/register) and
register a new account with same email you put in the configuration

If you want to login with a social account, first you *need* to create a developer app with
[Facebook](https://developers.facebook.com),
[Google](https://console.developers.google.com) or **any other identity provider** that you wish to use.
This isn't necessary if you're planning to login with LDAP, SAML or with email and password.
Save the obtained API keys in `application.conf`, as shown below.

> For all identity providers you must whitelist the Para host with the
appropriate authentication endpoint. For example, for GitHub, the redirect URL would be: `https://paraio.com/github_auth`,
for OAuth 2 - `https://paraio.com/oauth2_auth` and [so on](http://paraio.org/docs/#029-passwordless).

### Quick Start with a self-hosted Para backend (harder)

**Note: The Para backend server is deployed separately and is required for Scoold to run.**

1. [Follow this guide to run the Para backend server locally on port 8080](https://paraio.org/docs/#001-intro)
2. Create a separate folder `scoold` and inside, a new configuration file named `scoold-application.conf` (see example above)
3. Start Scoold with the following command, pointing it to the location of the Para configuration file:
```
java -jar -Dconfig.file=./scoold-application.conf \
  -Dscoold.autoinit.para_config_file=../para-application.conf scoold-*.jar`
```
4. Open [localhost:8000/signin/register](http://localhost:8000/signin/register) and
register a new account with same email you put in the configuration

Optionally, you can also install the [Para CLI](https://github.com/Erudika/para-cli) tool for browsing and managing
data in your Scoold app.

**Important:**
- Do not use the same `application.conf` file for both Para and Scoold!
- Run Para and Scoold in separate directories, each with its own configuration file.
- All settings shown here are meant to be kept inside the Scoold configuration file.
- SMTP settings must be configured before deploying Scoold to production.

[Read the Para docs](https://paraio.org/docs) for details on how to run and configure your Scoold backend.

### Hardware requirements

Scoold and Para can both be hosted on the same machine, provided it has at least 3 GB of RAM. Scoold requires:
- at least 500 MB RAM
- 1 vCPU or more
- 10 GB disk space or more (primarily for logs and storing images)

Para requires:
- at least 1 GB RAM
- 1 vCPU (2 are recommended)
- 10 GB disk space or more (unless the database is stored on the same machine)

JVM parameters: e.g. `java -jar -Xms600m -Xmx600m scoold-*.jar`

## Configuration

**Scoold requires a persistent and direct connection to a Para server to function properly.**

The most important configuration properties are:
- `scoold.para_endpoint` - the URL of the Para server
- `scoold.para_access_key` - the application identifier of your Para app
- `scoold.para_secret_key` - the secret key for your Para app

Copy the Scoold example configuration below to your **`application.conf`** and edit it if necessary:
```ini
### Minimal configuration ###
# the name of the application
scoold.app_name = "Scoold"
# the port for Scoold
scoold.port = 8000
# environment - "production" or "development"
scoold.env = "production"
# the public-facing URL where Scoold is hosted
scoold.host_url = "http://localhost:8000"
# the URL of Para - can also be "https://paraio.com"
scoold.para_endpoint = "http://localhost:8080"
# access key for your Para app
scoold.para_access_key = "app:scoold"
# secret key for your Para app
scoold.para_secret_key = ""
# the email or identifier of the admin user - check Para user object
scoold.admins = "admin@domain.com"
```
<details><summary><b>View ALL configuration options available in Scoold</b></summary>

## Core

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.app_name`<br>The formal name of the web application. | `Scoold` | `String`|
|`scoold.para_access_key` <kbd>requires restart</kbd><br>App identifier (access key) of the Para app used by Scoold. | `app:scoold` | `String`|
|`scoold.para_secret_key` <kbd>requires restart</kbd><br>Secret key of the Para app used by Scoold. | `x` | `String`|
|`scoold.para_endpoint` <kbd>requires restart</kbd><br>The URL of the Para server for Scoold to connects to. For hosted Para, use `https://paraio.com` | `http://localhost:8080` | `String`|
|`scoold.host_url`<br>The internet-facing (public) URL of this Scoold server. | `http://localhost:8000` | `String`|
|`scoold.port` <kbd>requires restart</kbd><br>The network port of this Scoold server. Port number should be a number above `1024`. | `8000` | `Integer`|
|`scoold.env` <kbd>requires restart</kbd><br>The environment profile to be used - possible values are `production` or `development` | `development` | `String`|
|`scoold.app_secret_key`<br>A random secret string, min. 32 chars long. *Must be different from the secret key of the Para app*. Used for generating JWTs and passwordless authentication tokens. | ` ` | `String`|
|`scoold.admins`<br>A comma-separated list of emails of people who will be promoted to administrators with full rights over the content on the site. This can also contain Para user identifiers. | ` ` | `String`|
|`scoold.is_default_space_public`<br>When enabled, all content in the default space will be publicly visible, without authentication, incl. users and tags. Disable to make the site private. | `true` | `Boolean`|
|`scoold.context_path` <kbd>requires restart</kbd><br>The context path (subpath) of the web application, defaults to the root path `/`. | ` ` | `String`|
|`scoold.webhooks_enabled`<br>Enable/disable webhooks support for events like `question.create`, `user.signup`, etc. | `true` | `Boolean`|
|`scoold.api_enabled`<br>Enable/disable the Scoold RESTful API. Disabled by default. | `false` | `Boolean`|
|`scoold.feedback_enabled`<br>Enable/disable the feedback page on the site. It is intended for internal discussion about the website itself. | `false` | `Boolean`|

## Emails

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.support_email`<br>The email address to use for sending transactional emails, like welcome/password reset emails. | `contact@scoold.com` | `String`|
|`scoold.mail.host`<br>The SMTP server host to use for sending emails. | ` ` | `String`|
|`scoold.mail.port`<br>The SMTP server port to use for sending emails. | `587` | `Integer`|
|`scoold.mail.username`<br>The SMTP server username. | ` ` | `String`|
|`scoold.mail.password`<br>The SMTP server password. | ` ` | `String`|
|`scoold.mail.tls`<br>Enable/disable TLS for the SMTP connection. | `true` | `Boolean`|
|`scoold.mail.ssl`<br>Enable/disable SSL for the SMTP connection. | `false` | `Boolean`|
|`scoold.mail.debug`<br>Enable/disable debug information when sending emails through SMTP. | `false` | `Boolean`|
|`scoold.favtags_emails_enabled`<br>Set the default toggle value for all users for receiving emails for new content with their favorite tags. | `false` | `Boolean`|
|`scoold.reply_emails_enabled`<br>Set the default toggle value for all users for receiving emails for answers to their questions. | `false` | `Boolean`|
|`scoold.comment_emails_enabled`<br>Set the default toggle value for all users for receiving emails for comments on their posts. | `false` | `Boolean`|
|`scoold.summary_email_period_days` <kbd>Pro</kbd><br>The time period between each content digest email, in days. | `7` | `Integer`|
|`scoold.summary_email_items`<br>The number of posts to include in the digest email (a summary of new posts). | `25` | `Integer`|
|`scoold.notification_emails_allowed`<br>Enable/disable *all* notification emails. | `true` | `Boolean`|
|`scoold.newpost_emails_allowed`<br>Enable/disable *all* email notifications for every new question that is posted on the site. | `true` | `Boolean`|
|`scoold.favtags_emails_allowed`<br>Enable/disable *all* email notifications for every new question tagged with a favorite tag. | `true` | `Boolean`|
|`scoold.reply_emails_allowed`<br>Enable/disable *all* email notifications for every new answer that is posted on the site. | `true` | `Boolean`|
|`scoold.comment_emails_allowed`<br>Enable/disable *all* email notifications for every new comment that is posted on the site. | `true` | `Boolean`|
|`scoold.mentions_emails_allowed` <kbd>Pro</kbd><br>Enable/disable *all* email notifications every time a user is mentioned. | `true` | `Boolean`|
|`scoold.summary_email_controlled_by_admins` <kbd>Pro</kbd><br>Controls whether admins can enable/disable summary emails for everyone from the 'Settings' page | `false` | `Boolean`|
|`scoold.mention_emails_controlled_by_admins` <kbd>Pro</kbd><br>Controls whether admins can enable/disable mention emails for everyone from the 'Settings' page | `false` | `Boolean`|
|`scoold.emails.welcome_text1`<br>Allows for changing the default text (first paragraph) in the welcome email message. | `You are now part of {0} - a friendly Q&A community...` | `String`|
|`scoold.emails.welcome_text2`<br>Allows for changing the default text (second paragraph) in the welcome email message. | `To get started, simply navigate to the "Ask question" page and ask a question...` | `String`|
|`scoold.emails.welcome_text3`<br>Allows for changing the default text (signature at the end) in the welcome email message. | `Best, <br>The {0} team` | `String`|
|`scoold.emails.default_signature`<br>The default email signature for all transactional emails sent from Scoold. | `Best, <br>The {0} team` | `String`|

## Security

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.approved_domains_for_signups`<br>A comma-separated list of domain names, which will be used to restrict the people who are allowed to sign up on the site. | ` ` | `String`|
|`scoold.security.allow_unverified_emails`<br>Enable/disable email verification after the initial user registration. Users with unverified emails won't be able to sign in, unless they use a social login provider. | `false` | `Boolean`|
|`scoold.session_timeout`<br>The validity period of the authentication cookie, in seconds. Default is 24h. | `86400` | `Integer`|
|`scoold.jwt_expires_after`<br>The validity period of the session token (JWT), in seconds. Default is 24h. | `86400` | `Integer`|
|`scoold.security.one_session_per_user`<br>If disabled, users can sign in from multiple locations and devices, keeping a few open sessions at once. Otherwise, only one session will be kept open, others will be closed. | `true` | `Boolean`|
|`scoold.min_password_length`<br>The minimum length of passwords. | `8` | `Integer`|
|`scoold.min_password_strength`<br>The minimum password strength - one of 3 levels: `1` good enough, `2` strong, `3` very strong. | `2` | `Integer`|
|`scoold.pass_reset_timeout`<br>The validity period of the password reset token sent via email for resetting users' passwords. Default is 30 min. | `1800` | `Integer`|
|`scoold.profile_anonimity_enabled`<br>Enable/disable the option for users to anonimize their profiles on the site, hiding their name and picture. | `false` | `Boolean`|
|`scoold.signup_captcha_site_key`<br>The reCAPTCHA v3 site key for protecting the signup and password reset pages. | ` ` | `String`|
|`scoold.signup_captcha_secret_key`<br>The reCAPTCHA v3 secret. | ` ` | `String`|
|`scoold.csp_reports_enabled`<br>Enable/disable automatic reports each time the Content Security Policy is violated. | `false` | `Boolean`|
|`scoold.csp_header_enabled`<br>Enable/disable the Content Security Policy (CSP) header. | `true` | `Boolean`|
|`scoold.csp_header`<br>The CSP header value which will overwrite the default one. This can contain one or more `{{nonce}}` placeholders, which will be replaced with an actual nonce on each request. | `Dynamically generated, with nonces` | `String`|
|`scoold.hsts_header_enabled`<br>Enable/disable the `Strict-Transport-Security` security header. | `true` | `Boolean`|
|`scoold.framing_header_enabled`<br>Enable/disable the `X-Frame-Options` security header. | `true` | `Boolean`|
|`scoold.xss_header_enabled`<br>Enable/disable the `X-XSS-Protection` security header. | `true` | `Boolean`|
|`scoold.contenttype_header_enabled`<br>Enable/disable the `X-Content-Type-Options` security header. | `true` | `Boolean`|
|`scoold.referrer_header_enabled`<br>Enable/disable the `Referrer-Policy` security header. | `true` | `Boolean`|
|`scoold.permissions_header_enabled`<br>Enable/disable the `Permissions-Policy` security header. | `true` | `Boolean`|
|`scoold.csp_connect_sources`<br>Additional sources to add to the `connect-src` CSP directive. Used when adding external scripts to the site. | ` ` | `String`|
|`scoold.csp_frame_sources`<br>Additional sources to add to the `frame-src` CSP directive. Used when adding external scripts to the site. | ` ` | `String`|
|`scoold.csp_font_sources`<br>Additional sources to add to the `font-src` CSP directive. Used when adding external fonts to the site. | ` ` | `String`|
|`scoold.csp_style_sources`<br>Additional sources to add to the `style-src` CSP directive. Used when adding external fonts to the site. | ` ` | `String`|

## Basic Authentication

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.password_auth_enabled`<br>Enabled/disable the ability for users to sign in with an email and password. | `true` | `Boolean`|
|`scoold.fb_app_id`<br>Facebook OAuth2 app ID. | ` ` | `String`|
|`scoold.fb_secret`<br>Facebook app secret key. | ` ` | `String`|
|`scoold.gp_app_id`<br>Google OAuth2 app ID. | ` ` | `String`|
|`scoold.gp_secret`<br>Google app secret key. | ` ` | `String`|
|`scoold.in_app_id`<br>LinkedIn OAuth2 app ID. | ` ` | `String`|
|`scoold.in_secret`<br>LinkedIn app secret key. | ` ` | `String`|
|`scoold.tw_app_id`<br>Twitter OAuth app ID. | ` ` | `String`|
|`scoold.tw_secret`<br>Twitter app secret key. | ` ` | `String`|
|`scoold.gh_app_id`<br>GitHub OAuth2 app ID. | ` ` | `String`|
|`scoold.gh_secret`<br>GitHub app secret key. | ` ` | `String`|
|`scoold.ms_app_id`<br>Microsoft OAuth2 app ID. | ` ` | `String`|
|`scoold.ms_secret`<br>Microsoft app secret key. | ` ` | `String`|
|`scoold.ms_tenant_id`<br>Microsoft OAuth2 tenant ID | `common` | `String`|
|`scoold.az_app_id`<br>Amazon OAuth2 app ID. | ` ` | `String`|
|`scoold.az_secret`<br>Amazon app secret key. | ` ` | `String`|
|`scoold.sl_app_id` <kbd>Pro</kbd><br>Slack OAuth2 app ID. | ` ` | `String`|
|`scoold.sl_secret` <kbd>Pro</kbd><br>Slack app secret key. | ` ` | `String`|
|`scoold.mm_app_id` <kbd>Pro</kbd><br>Mattermost OAuth2 app ID. | ` ` | `String`|
|`scoold.mm_secret` <kbd>Pro</kbd><br>Mattermost app secret key. | ` ` | `String`|
|`scoold.security.custom.provider` <kbd>Pro</kbd><br>The text on the button for signing in with the custom authentication scheme. | `Continue with Acme Co.` | `String`|
|`scoold.security.custom.login_url` <kbd>Pro</kbd><br>The URL address of an externally hosted, custom login page. | ` ` | `String`|

## LDAP Authentication

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.security.ldap.server_url`<br>LDAP server URL. LDAP will be disabled if this is blank. | ` ` | `String`|
|`scoold.security.ldap.base_dn`<br>LDAP base DN. | ` ` | `String`|
|`scoold.security.ldap.user_search_base`<br>LDAP search base, which will be used only if a direct bind is unsuccessfull. | ` ` | `String`|
|`scoold.security.ldap.user_search_filter`<br>LDAP search filter, for finding users if a direct bind is unsuccessful. | `(cn={0})` | `String`|
|`scoold.security.ldap.user_dn_pattern`<br>LDAP user DN pattern, which will be comined with the base DN to form the full path to theuser object, for a direct binding attempt. | `uid={0}` | `String`|
|`scoold.security.ldap.active_directory_domain`<br>AD domain name. Add this *only* if you are connecting to an Active Directory server. | ` ` | `String`|
|`scoold.security.ldap.password_attribute`<br>LDAP password attribute name. | `userPassword` | `String`|
|`scoold.security.ldap.bind_dn`<br>LDAP bind DN | ` ` | `String`|
|`scoold.security.ldap.bind_pass`<br>LDAP bind password. | ` ` | `String`|
|`scoold.security.ldap.username_as_name`<br>Enable/disable the use of usernames for names on Scoold. | `false` | `Boolean`|
|`scoold.security.ldap.provider` <kbd>Pro</kbd><br>The text on the LDAP sign in button. | `Continue with LDAP` | `String`|
|`scoold.security.ldap.mods_group_node`<br>Moderators group mapping, mapping LDAP users with this node, to moderators on Scoold. | ` ` | `String`|
|`scoold.security.ldap.admins_group_node`<br>Administrators group mapping, mapping LDAP users with this node, to administrators on Scoold. | ` ` | `String`|
|`scoold.security.ldap.compare_passwords`<br>LDAP compare passwords. | ` ` | `String`|
|`scoold.security.ldap.password_param`<br>LDAP password parameter name. | `password` | `String`|
|`scoold.security.ldap.username_param`<br>LDAP username parameter name. | `username` | `String`|
|`scoold.security.ldap.is_local` <kbd>Pro</kbd><br>Enable/disable local handling of LDAP requests, instead of sending those to Para. | `false` | `Boolean`|

## SAML Authentication

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.security.saml.idp.metadata_url` <kbd>Pro</kbd><br>SAML metadata URL. Scoold will fetch most of the necessary information for the authentication request from that XML document. This will overwrite all other IDP settings. | ` ` | `String`|
|`scoold.security.saml.sp.entityid` <kbd>Pro</kbd><br>SAML SP endpoint address - e.g. `https://paraio.com/saml_auth/scoold`. The IDP will call this address for authentication. | ` ` | `String`|
|`scoold.security.saml.sp.x509cert` <kbd>Pro</kbd><br>SAML client x509 certificate for the SP (public key). **Value must be Base64-encoded**. | ` ` | `String`|
|`scoold.security.saml.sp.privatekey` <kbd>Pro</kbd><br>SAML client private key in PKCS#8 format for the SP. **Value must be Base64-encoded**. | ` ` | `String`|
|`scoold.security.saml.attributes.id` <kbd>Pro</kbd><br>SAML attribute name of the user `id`. | `UserID` | `String`|
|`scoold.security.saml.attributes.picture` <kbd>Pro</kbd><br>SAML attribute name of the user `picture`. | `Picture` | `String`|
|`scoold.security.saml.attributes.email` <kbd>Pro</kbd><br>SAML attribute name of the user `email`. | `EmailAddress` | `String`|
|`scoold.security.saml.attributes.name` <kbd>Pro</kbd><br>SAML attribute name of the user `name`. | `GivenName` | `String`|
|`scoold.security.saml.attributes.firstname` <kbd>Pro</kbd><br>SAML attribute name of the user `firstname`. | `FirstName` | `String`|
|`scoold.security.saml.attributes.lastname` <kbd>Pro</kbd><br>SAML attribute name of the user `lastname`. | `LastName` | `String`|
|`scoold.security.saml.provider` <kbd>Pro</kbd><br>The text on the button for signing in with SAML. | `Continue with SAML` | `Boolean`|
|`scoold.security.saml.sp.assertion_consumer_service.url` <kbd>Pro</kbd><br>SAML ACS URL. | ` ` | `String`|
|`scoold.security.saml.sp.nameidformat` <kbd>Pro</kbd><br>SAML name id format. | `urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified` | `String`|
|`scoold.security.saml.idp.entityid` <kbd>Pro</kbd><br>SAML IDP entity id for manually setting the endpoint address of the IDP, instead of getting it from the provided metadata URL. | ` ` | `String`|
|`scoold.security.saml.idp.single_sign_on_service.url` <kbd>Pro</kbd><br>SAML SSO service URL of the IDP. | ` ` | `String`|
|`scoold.security.saml.idp.x509cert` <kbd>Pro</kbd><br>SAML server x509 certificate for the IDP (public key). **Value must be Base64-encoded**. | ` ` | `String`|
|`scoold.security.saml.security.authnrequest_signed` <kbd>Pro</kbd><br>Enable/disable SAML authentication request signing. | `false` | `Boolean`|
|`scoold.security.saml.security.want_messages_signed` <kbd>Pro</kbd><br>Enable/disable SAML message signing. | `false` | `Boolean`|
|`scoold.security.saml.security.want_assertions_signed` <kbd>Pro</kbd><br>Enable/disable SAML assertion signing. | `false` | `Boolean`|
|`scoold.security.saml.security.want_assertions_encrypted` <kbd>Pro</kbd><br>Enable/disable SAML assertion encryption. | `false` | `Boolean`|
|`scoold.security.saml.security.want_nameid_encrypted` <kbd>Pro</kbd><br>Enable/disable SAML name id encryption. | `false` | `Boolean`|
|`scoold.security.saml.security.sign_metadata` <kbd>Pro</kbd><br>Enable/disable SAML metadata signing. | `false` | `Boolean`|
|`scoold.security.saml.security.want_xml_validation` <kbd>Pro</kbd><br>Enable/disable SAML XML validation. | `true` | `Boolean`|
|`scoold.security.saml.security.signature_algorithm` <kbd>Pro</kbd><br>SAML signature algorithm. | ` ` | `String`|
|`scoold.security.saml.domain` <kbd>Pro</kbd><br>SAML domain name. | `paraio.com` | `String`|
|`scoold.security.saml.is_local` <kbd>Pro</kbd><br>Enable/disable local handling of SAML requests, instead of sending those to Para. | `false` | `Boolean`|

## OAuth 2.0 Authentication

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.oa2_app_id`<br>OAauth 2.0 client app identifier. Alternatives: `oa2second_app_id`, `oa2third_app_id` | ` ` | `String`|
|`scoold.oa2_secret`<br>OAauth 2.0 client app secret key. Alternatives: `oa2second_secret`, `oa2third_secret` | ` ` | `String`|
|`scoold.security.oauth.authz_url`<br>OAauth 2.0 client app authorization URL (login page). Alternatives: `security.oauthsecond.authz_url`, `security.oauththird.authz_url` | ` ` | `String`|
|`scoold.security.oauth.token_url`<br>OAauth 2.0 client app token endpoint URL. Alternatives: `security.oauthsecond.token_url`, `security.oauththird.token_url` | ` ` | `String`|
|`scoold.security.oauth.profile_url`<br>OAauth 2.0 client app user info endpoint URL. Alternatives: `security.oauthsecond.profile_url`, `security.oauththird.profile_url` | ` ` | `String`|
|`scoold.security.oauth.scope`<br>OAauth 2.0 client app scope. Alternatives: `security.oauthsecond.scope`, `security.oauththird.scope` | `openid email profile` | `String`|
|`scoold.security.oauth.accept_header`<br>OAauth 2.0 `Accept` header customization. Alternatives: `security.oauthsecond.accept_header`, `security.oauththird.accept_header` | ` ` | `String`|
|`scoold.security.oauth.parameters.id`<br>OAauth 2.0 attribute mapping for `id`. Alternatives: `security.oauthsecond.parameters.id`, `security.oauththird.parameters.id` | `sub` | `String`|
|`scoold.security.oauth.parameters.name`<br>OAauth 2.0 attribute mapping for `name`. Alternatives: `security.oauthsecond.parameters.name`, `security.oauththird.parameters.name` | `name` | `String`|
|`scoold.security.oauth.parameters.given_name`<br>OAauth 2.0 attribute mapping for `given_name`. Alternatives: `security.oauthsecond.parameters.given_name`, `security.oauththird.parameters.given_name` | `given_name` | `String`|
|`scoold.security.oauth.parameters.family_name`<br>OAauth 2.0 attribute mapping for `family_name`. Alternatives: `security.oauthsecond.parameters.family_name`, `security.oauththird.parameters.family_name` | `family_name` | `String`|
|`scoold.security.oauth.parameters.email`<br>OAauth 2.0 attribute mapping for `email`. Alternatives: `security.oauthsecond.parameters.email`, `security.oauththird.parameters.email` | `email` | `String`|
|`scoold.security.oauth.parameters.picture`<br>OAauth 2.0 attribute mapping for `picture`. Alternatives: `security.oauthsecond.parameters.picture`, `security.oauththird.parameters.picture` | `picture` | `String`|
|`scoold.security.oauth.download_avatars`<br>Enable/disable OAauth 2.0 avatar downloading to local disk. Used when avatars are large in size. Alternatives: `security.oauthsecond.download_avatars`, `security.oauththird.download_avatars` | `false` | `Boolean`|
|`scoold.security.oauth.token_delegation_enabled` <kbd>Pro</kbd><br>Enable/disable OAauth 2.0 token delegation. The ID and access tokens will be saved and delegated to Scoold from Para. Alternatives: `security.oauthsecond.token_delegation_enabled`, `security.oauththird.token_delegation_enabled` | `false` | `Boolean`|
|`scoold.security.oauth.spaces_attribute_name` <kbd>Pro</kbd><br>OAauth 2.0 attribute mapping for users' `spaces`. The spaces can be comma-separated. Alternatives: `security.oauthsecond.spaces_attribute_name`, `security.oauththird.spaces_attribute_name` | `spaces` | `String`|
|`scoold.security.oauth.groups_attribute_name` <kbd>Pro</kbd><br>OAauth 2.0 attribute mapping for users' `groups`. Alternatives: `security.oauthsecond.groups_attribute_name`, `security.oauththird.groups_attribute_name` | `roles` | `String`|
|`scoold.security.oauth.mods_equivalent_claim_value` <kbd>Pro</kbd><br>OAauth 2.0 claim used for mapping OAuth2 users having it, to moderators on Scoold. Alternatives: `security.oauthsecond.mods_equivalent_claim_value`, `security.oauththird.mods_equivalent_claim_value` | `mod` | `String`|
|`scoold.security.oauth.admins_equivalent_claim_value` <kbd>Pro</kbd><br>OAauth 2.0 claim used for mapping OAuth2 users having it, to administrators on Scoold. Alternatives: `security.oauthsecond.admins_equivalent_claim_value`, `security.oauththird.admins_equivalent_claim_value` | `admin` | `String`|
|`scoold.security.oauth.users_equivalent_claim_value` <kbd>Pro</kbd><br>OAauth 2.0 claim used for **denying access** to OAuth2 users **not** having it. Alternatives: `security.oauthsecond.users_equivalent_claim_value`, `security.oauththird.users_equivalent_claim_value` | ` ` | `String`|
|`scoold.security.oauth.domain`<br>OAauth 2.0 domain name for constructing user email addresses in case they are missing. Alternatives: `security.oauthsecond.domain`, `security.oauththird.domain` | ` ` | `String`|
|`scoold.security.oauth.provider`<br>The text on the button for signing in with OAuth2 or OIDC. | `Continue with OpenID Connect` | `String`|

## Posts

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.new_users_can_comment`<br>Enable/disable the ability for users with reputation below 100 to comments on posts. | `true` | `Boolean`|
|`scoold.posts_need_approval`<br>Enable/disable the need for approval of new posts by a moderator.  | `false` | `Boolean`|
|`scoold.wiki_answers_enabled` <kbd>Pro</kbd><br>Enable/disable the ability for users to create wiki-style answers, editable by everyone. | `true` | `Boolean`|
|`scoold.media_recording_allowed` <kbd>Pro</kbd><br>Enable/disable support for attaching recorded videos and voice messages to posts. | `true` | `Boolean`|
|`scoold.delete_protection_enabled`<br>Enable/disable the ability for authors to delete their own question, when it already has answers and activity. | `true` | `Boolean`|
|`scoold.max_text_length`<br>The maximum text length of each post (question or answer). Longer content will be truncated. | `20000` | `Integer`|
|`scoold.max_tags_per_post`<br>The maximum number of tags a question can have. The minimum is 0 - then the default tag is used. | `5` | `Integer`|
|`scoold.max_replies_per_post`<br>The maximum number of answers a question can have. | `500` | `Integer`|
|`scoold.max_comments_per_id`<br>The maximum number of comments a post can have. | `1000` | `Integer`|
|`scoold.max_comment_length`<br>The maximum length of each comment. | `600` | `Integer`|
|`scoold.max_mentions_in_posts` <kbd>Pro</kbd><br>The maximum number of mentioned users a post can have. | `10` | `Integer`|
|`scoold.anonymous_posts_enabled` <kbd>Pro</kbd><br>Enable/disable the ability for unathenticated users to create new questions. | `false` | `Boolean`|
|`scoold.nearme_feature_enabled`<br>Enable/disable the ability for users to attach geolocation data to questions and location-based filtering of questions. | `false` | `Boolean`|
|`scoold.merge_question_bodies`<br>Enable/disable the merging of question bodies when two questions are merged into one. | `true` | `Boolean`|
|`scoold.max_similar_posts`<br>The maximum number of similar posts which will be displayed on the side. | `7` | `Integer`|
|`scoold.default_question_tag`<br>The default question tag, used when no other tags are provided by its author. | `question` | `String`|
|`scoold.posts_rep_threshold`<br>The minimum reputation an author needs to create a post without approval by moderators. This is only required if new posts need apporval. | `100` | `Integer`|

## Spaces

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.auto_assign_spaces`<br>A comma-separated list of spaces to assign to all new users. | ` ` | `String`|
|`scoold.reset_spaces_on_new_assignment`<br>Spaces delegated from identity providers will overwrite the existing ones for users. | `true` | `Boolean`|

## Reputation and Rewards

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.answer_voteup_reward_author`<br>Reputation points given to author of answer as reward when a user upvotes it. | `10` | `Integer`|
|`scoold.question_voteup_reward_author`<br>Reputation points given to author of question as reward when a user upvotes it. | `5` | `Integer`|
|`scoold.voteup_reward_author`<br>Reputation points given to author of comment or other post as reward when a user upvotes it. | `2` | `Integer`|
|`scoold.answer_approve_reward_author`<br>Reputation points given to author of answer as reward when the question's author accepts it. | `10` | `Integer`|
|`scoold.answer_approve_reward_voter`<br>Reputation points given to author of question who accepted an answer. | `3` | `Integer`|
|`scoold.post_votedown_penalty_author`<br>Reputation points taken from author of post as penalty when their post was downvoted. | `3` | `Integer`|
|`scoold.post_votedown_penalty_voter`<br>Reputation points taken from the user who downvotes any content. Discourages downvoting slightly. | `1` | `Integer`|
|`scoold.voter_ifhas`<br>Number of votes (up or down) needed from a user for earning the `voter` badge. | `100` | `Integer`|
|`scoold.commentator_ifhas`<br>Number of comments a user needs to have posted for earning the `commentator` badge. | `100` | `Integer`|
|`scoold.critic_ifhas`<br>Number of cast downvotes needed from a user for earning the `critic` badge. | `10` | `Integer`|
|`scoold.supporter_ifhas`<br>Number of cast upvotes needed from a user for earning the `supporter` badge`. | `50` | `Integer`|
|`scoold.goodquestion_ifhas`<br>Votes needed on a question before its author gets to earn the `good question` badge. | `20` | `Integer`|
|`scoold.goodanswer_ifhas`<br>Votes needed on an answer before its author gets to earn the `good answer` badge. | `10` | `Integer`|
|`scoold.enthusiast_ifhas`<br>Reputation points needed for earning the `enthusiast` badge. | `100` | `Integer`|
|`scoold.freshman_ifhas`<br>Reputation points needed for earning the `freshman` badge. | `300` | `Integer`|
|`scoold.scholar_ifhas`<br>Reputation points needed for earning the `scholar` badge. | `500` | `Boolean`|
|`scoold.teacher_ifhas`<br>Reputation points needed for earning the `teacher` badge. | `1000` | `Integer`|
|`scoold.geek_ifhas`<br>Reputation points needed for earning the `geek` badge. | `9000` | `Integer`|

## File Storage

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.uploads_enabled` <kbd>Pro</kbd><br>Enable/disable file uploads. | `true` | `Boolean`|
|`scoold.file_uploads_dir` <kbd>Pro</kbd><br>The directory (local or in the cloud) where files will be stored. | `uploads` | `String`|
|`scoold.uploads_require_auth` <kbd>Pro</kbd><br>Enable/disable the requirement that uploaded files can only be accessed by authenticated users. | `false` | `Boolean`|
|`scoold.allowed_upload_formats` <kbd>Pro</kbd><br>A comma-separated list of allowed MIME types in the format `extension:mime_type`, e.g.`py:text/plain` or just the extensions `py,yml` | ` ` | `String`|
|`scoold.s3_bucket` <kbd>Pro</kbd><br>AWS S3 bucket name as target for storing files. | ` ` | `String`|
|`scoold.s3_path` <kbd>Pro</kbd><br>AWS S3 object prefix (directory) inside the bucket. | ` ` | `String`|
|`scoold.s3_region` <kbd>Pro</kbd><br>AWS S3 region. | ` ` | `String`|
|`scoold.s3_access_key` <kbd>Pro</kbd><br>AWS S3 access key. | ` ` | `String`|
|`scoold.s3_secret_key` <kbd>Pro</kbd><br>AWS S3 secret key. | ` ` | `String`|
|`scoold.blob_storage_account` <kbd>Pro</kbd><br>Azure Blob Storage account ID. | ` ` | `String`|
|`scoold.blob_storage_token` <kbd>Pro</kbd><br>Azure Blob Storage token. | ` ` | `String`|
|`scoold.blob_storage_container` <kbd>Pro</kbd><br>Azure Blob Storage container. | ` ` | `String`|
|`scoold.blob_storage_path` <kbd>Pro</kbd><br>Azure Blob Storage path prefix (subfolder) within a container. | ` ` | `String`|

## Customization

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.default_language_code`<br>The default language code to use for the site. Set this to make the site load a different language from English. | ` ` | `String`|
|`scoold.welcome_message`<br>Adds a brief intro text inside a banner at the top of the main page for new visitors to see. | ` ` | `String`|
|`scoold.welcome_message_onlogin`<br>Adds a brief intro text inside a banner at the top of the 'Sign in' page only. | ` ` | `String`|
|`scoold.dark_mode_enabled`<br>Enable/disable the option for users to switch to the dark theme. | `true` | `Boolean`|
|`scoold.meta_description`<br>The content inside the description `<meta>` tag. | ` ` | `String`|
|`scoold.meta_keywords`<br>The content inside the keywords `<meta>` tag. | ` ` | `String`|
|`scoold.show_branding`<br>Enable/disable the 'Powered by Scoold' branding in the footer. | `true` | `Boolean`|
|`scoold.mathjax_enabled` <kbd>Pro</kbd><br>Enable/disable support for MathJax and LaTeX for scientific expressions in Markdown. | `false` | `Boolean`|
|`scoold.gravatars_enabled`<br>Enable/disable support for Gravatars. | `true` | `Boolean`|
|`scoold.gravatars_pattern`<br>The pattern to use when displaying empty/anonymous gravatar pictures. | `retro` | `String`|
|`scoold.avatar_repository` <kbd>preview</kbd><br>The avatar repository - one of `imgur`, `cloudinary`. | ` ` | `String`|
|`scoold.footer_html`<br>Some custom HTML content to be added to the website footer. | ` ` | `String`|
|`scoold.navbar_link1_url`<br>The URL of an extra custom link which will be added to the top navbar. | ` ` | `String`|
|`scoold.navbar_link1_text`<br>The title of an extra custom link which will be added to the top navbar. | `Link1` | `String`|
|`scoold.navbar_link2_url`<br>The URL of an extra custom link which will be added to the top navbar. | ` ` | `String`|
|`scoold.navbar_link2_text`<br>The title of an extra custom link which will be added to the top navbar. | `Link2` | `String`|
|`scoold.navbar_menu_link1_url`<br>The URL of an extra custom link which will be added to user's dropdown menu. Only shown to authenticated users. | ` ` | `String`|
|`scoold.navbar_menu_link1_text`<br>The title of an extra custom link which will be added to the user's dropdown menu. | `Menu Link1` | `String`|
|`scoold.navbar_menu_link2_url`<br>The URL of an extra custom link which will be added to user's dropdown menu. Only shown to authenticated users. | ` ` | `String`|
|`scoold.navbar_menu_link2_text`<br>The title of an extra custom link which will be added to the user's dropdown menu. | `Menu Link2` | `String`|
|`scoold.always_hide_comment_forms`<br>Enable/disable a visual tweak which keeps all comment text editors closed at all times. | `true` | `Boolean`|
|`scoold.footer_links_enabled`<br>Enable/disable all links in the website footer. | `true` | `Boolean`|
|`scoold.emails_footer_html`<br>The HTML code snippet to embed at the end of each transactional email message. | `<a href="{host_url}">{app_name}</a> &bull; <a href="https://scoold.com">Powered by Scoold</a>` | `String`|
|`scoold.cookie_consent_required`<br>Enable/disable the cookie consent popup box and blocks all external JS scripts from loading. Used for compliance with GDPR/CCPA. | `false` | `Boolean`|
|`scoold.fixed_nav`<br>Enable/disable a fixed navigation bar. | `false` | `Boolean`|
|`scoold.logo_width`<br>The width of the logo image in the nav bar, in pixels. Used for fine adjustments to the logo size. | `100` | `Integer`|
|`scoold.code_highlighting_enabled`<br>Enable/disable support for syntax highlighting in code blocks. | `true` | `Boolean`|
|`scoold.max_pages`<br>Maximum number of pages to return as results. | `1000` | `Integer`|
|`scoold.numeric_pagination_enabled`<br>Enable/disable the numeric pagination style `(< 1 2 3...N >)`. | `false` | `Boolean`|
|`scoold.html_in_markdown_enabled`<br>Enable/disable the ability for users to insert basic HTML tags inside Markdown content. | `false` | `Boolean`|
|`scoold.max_items_per_page`<br>Maximum number of results to return in a single page of results. | `30` | `Integer`|
|`scoold.avatar_edits_enabled`<br>Enable/disable the ability for users to edit their profile pictures. | `true` | `Boolean`|
|`scoold.name_edits_enabled`<br>Enable/disable the ability for users to edit their name. | `true` | `Boolean`|

## Frontend Assets

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.logo_url`<br>The URL of the logo in the nav bar. | `/images/logo.svg` | `String`|
|`scoold.small_logo_url`<br>The URL of a smaller logo. Mainly used in transactional emails. | `/images/logowhite.png` | `String`|

## Miscellaneous

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.cdn_url`<br>A CDN URL where all static assets might be stored. | ` ` | `String`|

## Frontend Assets

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.stylesheet_url`<br>A stylesheet URL of a CSS file which will be used as the main stylesheet. *This will overwrite all existing CSS styles!* | `/styles/style.css` | `String`|
|`scoold.external_styles`<br>A comma-separated list of external CSS files. These will be loaded *after* the main stylesheet. | ` ` | `String`|
|`scoold.external_scripts._id_`<br>A map of external JS scripts. These will be loaded after the main JS script. For example: `scoold.external_scripts.script1 = "alert('Hi')"` | ` ` | `Map`|
|`scoold.inline_css`<br>Some short, custom CSS snippet to embed inside the `<head>` element. | ` ` | `String`|
|`scoold.favicon_url`<br>The URL of the favicon image. | `/images/favicon.ico` | `String`|
|`scoold.meta_app_icon`<br>The URL of the app icon image in the `<meta property='og:image'>` tag. | `/images/logowhite.png` | `String`|

## Mattermost Integration

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.mattermost.server_url` <kbd>Pro</kbd><br>Mattermost server URL. | ` ` | `String`|
|`scoold.mattermost.bot_username` <kbd>Pro</kbd><br>Mattermost bot username. | `scoold` | `String`|
|`scoold.mattermost.bot_icon_url` <kbd>Pro</kbd><br>Mattermost bot avatar URL. | `/images/logowhite.png` | `String`|
|`scoold.mattermost.post_to_space` <kbd>Pro</kbd><br>Default space on Scoold where questions created on Mattermost will be published. Set it to `workspace` for using the team's name. | ` ` | `String`|
|`scoold.mattermost.map_channels_to_spaces` <kbd>Pro</kbd><br>Enable/disable mapping of Mattermost channels to Scoold spaces. When enabled, will create a Scoold space for each Mattermost channel. | `false` | `Boolean`|
|`scoold.mattermost.map_workspaces_to_spaces` <kbd>Pro</kbd><br>Enable/disable mapping of Mattermost teams to Scoold spaces. When enabled, will create a Scoold space for each Mattermost team. | `true` | `Boolean`|
|`scoold.mattermost.max_notification_webhooks` <kbd>Pro</kbd><br>The maximum number of incoming webhooks which can be created on Scoold. Each webhook links a Mattermost channel to Scoold. | `10` | `Integer`|
|`scoold.mattermost.notify_on_new_answer` <kbd>Pro</kbd><br>Enable/disable the ability for Scoold to send notifications to Mattermost for new answers. | `true` | `Boolean`|
|`scoold.mattermost.notify_on_new_question` <kbd>Pro</kbd><br>Enable/disable the ability for Scoold to send notifications to Mattermost for new questions. | `true` | `Boolean`|
|`scoold.mattermost.notify_on_new_comment` <kbd>Pro</kbd><br>Enable/disable the ability for Scoold to send notifications to Mattermost for new comments. | `true` | `Boolean`|
|`scoold.mattermost.dm_on_new_comment` <kbd>Pro</kbd><br>Enable/disable the ability for Scoold to send direct messages to Mattermost users for new comments. | `false` | `Boolean`|
|`scoold.mattermost.default_question_tags` <kbd>Pro</kbd><br>Default question tags for questions created on Mattermost (comma-separated list). | `via-mattermost` | `String`|

## Slack Integration

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.slack.auth_enabled` <kbd>Pro</kbd><br>Enable/disable authentication with Slack. | `false` | `Boolean`|
|`scoold.slack.app_id` <kbd>Pro</kbd><br>The Slack app ID (first ID from the app's credentials, not the OAuth2 Client ID). | ` ` | `String`|
|`scoold.slack.signing_secret` <kbd>Pro</kbd><br>Slack signing secret key for verifying request signatures. | `x` | `String`|
|`scoold.slack.max_notification_webhooks` <kbd>Pro</kbd><br>The maximum number of incoming webhooks which can be created on Scoold. Each webhook links a Slack channel to Scoold. | `10` | `Integer`|
|`scoold.slack.map_channels_to_spaces` <kbd>Pro</kbd><br>Enable/disable mapping of Slack channels to Scoold spaces. When enabled, will create a Scoold space for each Slack channel. | `false` | `Boolean`|
|`scoold.slack.map_workspaces_to_spaces` <kbd>Pro</kbd><br>Enable/disable mapping of Slack teams to Scoold spaces. When enabled, will create a Scoold space for each Slack team. | `true` | `Boolean`|
|`scoold.slack.post_to_space` <kbd>Pro</kbd><br>Default space on Scoold where questions created on Slack will be published. Set it to `workspace` for using the team's name. | ` ` | `String`|
|`scoold.slack.default_title` <kbd>Pro</kbd><br>Default question title for questions created on Slack. | `A question from Slack` | `String`|
|`scoold.slack.notify_on_new_answer` <kbd>Pro</kbd><br>Enable/disable the ability for Scoold to send notifications to Slack for new answers. | `true` | `Boolean`|
|`scoold.slack.notify_on_new_question` <kbd>Pro</kbd><br>Enable/disable the ability for Scoold to send notifications to Slack for new questions. | `true` | `Boolean`|
|`scoold.slack.notify_on_new_comment` <kbd>Pro</kbd><br>Enable/disable the ability for Scoold to send notifications to Slack for new comments. | `true` | `Boolean`|
|`scoold.slack.dm_on_new_comment` <kbd>Pro</kbd><br>Enable/disable the ability for Scoold to send direct messages to Slack users for new comments. | `false` | `Boolean`|
|`scoold.slack.default_question_tags` <kbd>Pro</kbd><br>Default question tags for questions created on Slack (comma-separated list). | `via-slack` | `String`|

## Microsoft Teams Integration

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.teams.bot_id` <kbd>Pro</kbd><br>Teams bot ID. | ` ` | `String`|
|`scoold.teams.bot_secret` <kbd>Pro</kbd><br>Teams bot secret key. | ` ` | `String`|
|`scoold.teams.bot_service_url` <kbd>Pro</kbd><br>Teams bot service URL. | `https://smba.trafficmanager.net/emea/` | `String`|
|`scoold.teams.notify_on_new_answer` <kbd>Pro</kbd><br>Enable/disable the ability for Scoold to send notifications to Teams for new answers. | `true` | `Boolean`|
|`scoold.teams.notify_on_new_question` <kbd>Pro</kbd><br>Enable/disable the ability for Scoold to send notifications to Teams for new questions. | `true` | `Boolean`|
|`scoold.teams.notify_on_new_comment` <kbd>Pro</kbd><br>Enable/disable the ability for Scoold to send notifications to Teams for new comments. | `true` | `Boolean`|
|`scoold.teams.dm_on_new_comment` <kbd>Pro</kbd><br>Enable/disable the ability for Scoold to send direct messages to Teams users for new comments. | `false` | `Boolean`|
|`scoold.teams.post_to_space` <kbd>Pro</kbd><br>Default space on Scoold where questions created on Teams will be published. Set it to `workspace` for using the team's name. | ` ` | `String`|
|`scoold.teams.map_channels_to_spaces` <kbd>Pro</kbd><br>Enable/disable mapping of Teams channels to Scoold spaces. When enabled, will create a Scoold space for each Teams channel. | `false` | `Boolean`|
|`scoold.teams.map_workspaces_to_spaces` <kbd>Pro</kbd><br>Enable/disable mapping of Teams teams to Scoold spaces. When enabled, will create a Scoold space for each Teams team. | `true` | `Boolean`|
|`scoold.teams.max_notification_webhooks` <kbd>Pro</kbd><br>The maximum number of incoming webhooks which can be created on Scoold. Each webhook links a Teams channel to Scoold. | `10` | `Integer`|
|`scoold.teams.default_title` <kbd>Pro</kbd><br>Default question title for questions created on Teams. | `A question from Microsoft Teams` | `String`|
|`scoold.teams.default_question_tags` <kbd>Pro</kbd><br>Default question tags for questions created on Teams (comma-separated list). | `via-teams` | `String`|

## SCIM

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.scim_enabled` <kbd>Pro</kbd> <kbd>preview</kbd><br>Enable/disable support for SCIM user provisioning. | `false` | `Boolean`|
|`scoold.scim_secret_token` <kbd>Pro</kbd> <kbd>preview</kbd><br>SCIM secret token. | ` ` | `String`|
|`scoold.scim_allow_provisioned_users_only` <kbd>Pro</kbd> <kbd>preview</kbd><br>Enable/disable the restriction that only SCIM-provisioned users can sign in. | `false` | `Boolean`|
|`scoold.scim_map_groups_to_spaces` <kbd>Pro</kbd> <kbd>preview</kbd><br>Enable/disable mapping of SCIM groups to Scoold spaces. | `true` | `Boolean`|
|`scoold.security.scim.admins_group_equivalent_to` <kbd>Pro</kbd> <kbd>preview</kbd><br>SCIM group whose members will be promoted to administrators on Scoold. | `admins` | `String`|
|`scoold.security.scim.mods_group_equivalent_to` <kbd>Pro</kbd> <kbd>preview</kbd><br>SCIM group whose members will be promoted to moderators on Scoold. | `mods` | `String`|

## Miscellaneous

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.security.redirect_uri`<br>Publicly accessible, internet-facing URL of the Para endpoint where authenticated users will be redirected to, from the identity provider. Used when Para is hosted behind a proxy. | `http://localhost:8080` | `String`|
|`scoold.redirect_signin_to_idp`<br>Enable/disable the redirection of users from the signin page, directly to the IDP login page. | `false` | `Boolean`|
|`scoold.gmaps_api_key`<br>The Google Maps API key. Used for geolocation functionality, (e.g. 'posts near me', location). | ` ` | `String`|
|`scoold.imgur_client_id` <kbd>preview</kbd><br>Imgur API client id. Used for uploading avatars to Imgur. **Note:** Imgur have some breaking restrictions going on in their API and this might not work. | ` ` | `String`|
| `scoold.cloudinary_url` <kbd>preview</kbd><br>Cloudinary URL. Used for uploading avatars to Cloudinary. | ` ` | `String`|

## Posts

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.max_fav_tags`<br>Maximum number of favorite tags. | `50` | `Integer`|

## Miscellaneous

| Property key & Description | Default Value | Type |
|  ---                       | ---           | ---  |
|`scoold.batch_request_size`<br>Maximum batch size for the Para client pagination requests. | `0` | `Integer`|
|`scoold.signout_url`<br>The URL which users will be redirected to after they click 'Sign out'. Can be a page hosted externally. | `/signin?code=5&success=true` | `String`|
|`scoold.vote_expires_after_sec`<br>Vote expiration timeout, in seconds. Users can vote again on the same content after this period has elapsed. Default is 30 days. | `2592000` | `Integer`|
|`scoold.vote_locked_after_sec`<br>Vote locking period, in seconds. Vote cannot be changed after this period has elapsed. Default is 30 sec. | `30` | `Integer`|
|`scoold.import_batch_size`<br>Maximum number objects to read and send to Para when importing data from a backup. | `100` | `Integer`|
|`scoold.connection_retries_max`<br>Maximum number of connection retries to Para. | `10` | `Integer`|
|`scoold.connection_retry_interval_sec`<br>Para connection retry interval, in seconds. | `10` | `Integer`|
|`scoold.rewrite_inbound_links_with_fqdn`<br>If set, links to Scoold in emails will be replaced with a public-facing FQDN. | ` ` | `String`|
|`scoold.cluster_nodes`<br>Total number of nodes present in the cluster when Scoold is deployed behind a reverse proxy. | `1` | `Integer`|
|`scoold.autoinit.root_app_secret_key`<br>If configured, Scoold will try to automatically initialize itself with Para and create its own Para app, called `app:scoold`. The keys for that new app will be saved in the configuration file. | ` ` | `String`|
|`scoold.autoinit.para_config_file`<br>Does the same as `scoold.autoinit.root_app_secret_key` but tries to read the secret key for the root Para app from the Para configuration file, wherever that may be. | ` ` | `String`|

</details>

On startup, Scoold will try to connect to Para 10 times, with a 10 second interval between retries. After that it will
fail and the settings will not be persisted. If you set the maximum number of retries to `-1` there will be an infinite
number of attempts to connect to Para. These parameters are controlled by:

```ini
scoold.connection_retries_max = 10
scoold.connection_retry_interval_sec = 10
```

## Docker

Tagged Docker images for Scoold are located at `erudikaltd/scoold` on Docker Hub. **It's highly recommended that you
pull only release images like `:1.49.0` or `:latest_stable` because the `:latest` tag can be broken or unstable.**
The `:latest_stable` tag always points to the latest release version.

The *easiest way* to create the Scoold stack is to run `docker compose up`.

1. First, create a new directory and copy [`docker-compose.yml`](docker-compose.yml) (for **Scoold Pro**
[`docker-compose.yml` is here](https://raw.githubusercontent.com/Erudika/scoold-pro/master/docker-compose.yml))
to it from this repository.
2. Create the two configuration files in the same directory (both files can be left blank for now):
```
$ touch para-application.conf scoold-application.conf
```
3. `$ docker compose up`

To stop the containers use <kbd>Ctrl</kbd> + <kbd>C</kbd>.

**Important:** Scoold will connect to Para on `http://para:8080`, inside the Docker container environment.
That hostname may not be accessible from your local machine or the Internet, which will break authentication redirects.
For example, when signing in with Google, you will be redirected to Google and then back to Para, on the address
specified in `scoold.security.redirect_uri`. Para must be a publicly accessible on that address or the authentication
requests will fail.

If you prefer, you can run the Scoold container outside of Docker Compose.
First, have your Scoold `application.conf` configuration file ready in the current directory and run this command:

```
$ docker run -ti -p 8000:8000 --rm -v $(pwd)/application.conf:/scoold/application.conf \
  -e JAVA_OPTS="-Dconfig.file=/scoold/application.conf" erudikaltd/scoold:latest_stable
```

For **Scoold Pro** the images are located in a private registry. You can get access to it once you purchase a Pro license.
The run command for **Scoold Pro** is similar with the only difference being the uploads volume:
```
$ docker run -ti -p 8000:8000 --rm -v $(pwd)/application.conf:/scoold-pro/application.conf \
  -v scoold-uploads:/scoold-pro/uploads -e JAVA_OPTS="-Dconfig.file=/scoold-pro/application.conf" \
  374874639893.dkr.ecr.eu-west-1.amazonaws.com/scoold-pro:latest_stable
```

**Follow the [getting started guide](#quick-start-with-a-self-hosted-para-backend-harder)
after starting the Para and Scoold containers.**

**Environment variables**

`JAVA_OPTS` - Java system properties, e.g. `-Dscoold.port=8000`
`BOOT_SLEEP` - Startup delay, in seconds

## Kubernetes

There's a Helm chart inside the `helm/` folder. First edit `helm/scoold/values.yaml` and then you can deploy Scoold to
Kubernetes with a single command:

```
cd helm; helm install ./scoold
```

For more info, read the README at `helm/README.md`.

## Docker registry for Scoold Pro images

If you purchase Scoold Pro you can get access to the private Docker registry hosted on the AWS Elastic Container Registry.
Access to the private registry is not given automatically upon purchase - you have to request it. You will then be issued
a special access key and secret for AWS ECR. Then execute the following BASH commands (these require
[AWS CLI v2.x](https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html)):
1. Configure AWS CLI to use the new credentials:
```
aws configure
```
2. Authenticate Docker with ECR using the temporary access token:
```
aws ecr get-login-password --region eu-west-1 | \
	docker login --username AWS --password-stdin 374874639893.dkr.ecr.eu-west-1.amazonaws.com
```
3. Pull a Scoold Pro image with a specific tag:
```
aws ecr list-images --repository-name scoold-pro
docker pull 374874639893.dkr.ecr.eu-west-1.amazonaws.com/scoold-pro:{tag}
```

The `:latest` tag is not supported but you can use `:latest_stable`. The command `aws get-login-password`
gives you an access token to the private Docker registry which is valid for **12 hours**.

For connecting Kubernetes to AWS ECR, please refer to [this article](https://medium.com/@damitj07/how-to-configure-and-use-aws-ecr-with-kubernetes-rancher2-0-6144c626d42c).

In case you don't want to use AWS CLI for logging into the Scoold Pro registry, install the
[AWS ECR Docker Credentials Helper](https://github.com/awslabs/amazon-ecr-credential-helper).


## Deploying Scoold to Heroku

**One-click deployment**

[![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy?template=https://github.com/Erudika/scoold)

### Manual deployment - option 1 (code push)

1. First, clone this repository and create a new Heroku app
2. Add Heroku as a Git remote target and push your changes with `git push heroku master`
3. Go to the Heroku admin panel, under "Settings", "Reveal Config Vars" and set all the configuration variables shown
above but **replace all dots in the variable names with underscores**, e.g. `scoold.para_endpoint` -> `para_endpoint`.
4. Open the app in your browser at `https://{appname}.herokuapp.com`.

### Manual deployment - option 2 (JAR push)

1. Build the Scoold JAR file or acquire the Scoold Pro JAR package by [buying Pro](https://paraio.com/scoold-pro)
2. [Download and install the Heroku CLI](https://devcenter.heroku.com/articles/heroku-cli)
3. Create a Heroku app or use the id of an existing Heroku app where you want Scoold deployed
4. Add the `heroku/jvm` and `heroku/java` buildpacks to your Heroku app from the Settings page
5. Create a file `Procfile` containing this line:
```
web: java -Dserver.port=$PORT $JAVA_OPTS -jar scoold-*.jar $JAR_OPTS
```
6. Open a terminal in the directory containing the JAR file and execute:
```
$ heroku plugins:install java
$ heroku deploy:jar scoold-x.y.z.jar --app myscooldapp
```
Pushing JARs to Heroku is useful in cases where you have an existing Heroku app which hosts a free version of Scoold,
deployed through the "one-click" Heroku button, and you want to upgrade it to Scoold Pro.

### Configuring Scoold on Heroku

On Heroku you don't have a configuration file, instead you use Heroku's environment variables to configure Scoold.
You can add each Scoold configuration property as an environment variable on the Settings page of your Heroku admin page.
Click "Reveal Config Vars". Configuration variables (config vars) **must not** contain dots ".", for example `scoold.para_endpoint`
becomes `para_endpoint`. You **must** replace every dot with an underscore in order to convert a Scoold configuration
property to a Heroku environment variable.

It's also helpful to install the Heroku CLI tool. Using the CLI you can watch the Scoold logs with:
```
$ heroku logs --tail --app myscooldapp
```
Or you can restart your dyno with:
```
$ heroku restart --app myscooldapp
```

## Deploying Scoold to DigitalOcean

1. Create a droplet running Ubuntu and SSH into it
2. Create a user `ubuntu` with `adduser ubuntu`
3. Execute (as root) `wget https://raw.githubusercontent.com/Erudika/scoold/master/installer.sh && bash installer.sh`
4. Copy the configuration file to your droplet: `scp application.conf root@123.234.12.34:/home/ubuntu`
5. Restart Scoold with `ssh root@123.234.12.34 "systemctl restart scoold.service"`
6. Go to `http://123.234.12.34:8000` and verify that Scoold is running (use the correct IP address of your droplet)
7. Configure SSL on DigitalOcean or install nginx + letsencrypt on your droplet (see instructions below)

## Deploying Scoold to AWS

<a href="https://lightsail.aws.amazon.com/ls/webapp/create/instance" title="Deploy to Lightsail">
	<img src="https://s3-eu-west-1.amazonaws.com/com.scoold.files/awsdeploy.svg" height="32" alt="deploy to aws button">
</a>

**Lightsail**

1. Click the button above
2. Choose "Linux", "OS only", "Ubuntu 18.04 LTS"
3. Click "+ Add launch script" and copy/paste the contents of [installer.sh](https://github.com/Erudika/scoold/blob/master/installer.sh)
4. Download the default SSH key pair or upload your own
5. Choose the 512MB instance or larger (1GB recommended)
6. Wait for the instance to start and open the IP address in your browser at port `8000`

**Elatic Beanstalk**

1. Clone this repo and change directory to it
2. Generate a WAR package with `mvn -Pwar package`
3. [Create a new Beanstalk web app](https://console.aws.amazon.com/elasticbeanstalk/home?region=eu-west-1#/newApplication?applicationName=Scoold&platform=Tomcat&tierName=WebServer&instanceType=t1.micro)
4. Upload the WAR package `target/scoold-x.y.z.war` to Beanstalk, modify any additional options and hit "Create"

**Authentication with Amazon Cognito**

Scoold is fully compatible with Amazon Cognito because Cognito is just another OAuth 2.0 service provider. Here's how to
configure Scoold to work with Amazon Cognito:

1. Create a Cognito user pool (if you don't have one already)
2. Create a Cognito App client with the OAuth 2.0 authorization code grant enabled:
3. Create a Cognito login subdomain for your app client like this: `https://scoold.auth.eu-west-1.amazoncognito.com`
4. Edit the Scoold configuration file `application.conf` and add a new OAuth 2.0 authentication provider:
```ini
scoold.oa2_app_id = "cognito_app_client_id"
scoold.oa2_secret = "cognito_app_client_secret"
scoold.security.oauth.authz_url = "https://scoold.auth.eu-west-1.amazoncognito.com/login"
scoold.security.oauth.token_url = "https://scoold.auth.eu-west-1.amazoncognito.com/oauth2/token"
scoold.security.oauth.profile_url = "https://scoold.auth.eu-west-1.amazoncognito.com/oauth2/userInfo"
scoold.security.oauth.provider = "Continue with Cognito"
```
5. Restart Scoold and login with a user from your Cognito user pool

Make sure you whitelist your Para authentication endpoint with Cognito `https://para_url/oauth2_auth`.

## Deploying Scoold to Azure

[![Deploy to Azure](https://aka.ms/deploytoazurebutton)](https://portal.azure.com/#create/Microsoft.Template/uri/https%3A%2F%2Fraw.githubusercontent.com%2FErudika%2Fscoold%2Fmaster%2Fazuredeploy.json)

1. Click the button above
2. Fill in the required parameters
3. Launch the container
4. Go to your container and press "Connect" using `/bin/sh`
5. In the terminal type in `vi application.conf`, hit `i` and paste in your configuration
6. Hit `Esc` and type in `:wq` then restart your container
Another option is to attach a [secret volume](https://docs.microsoft.com/en-us/azure/container-instances/container-instances-volume-secret)
to your container, containing the configuration. It should be mounted as `/scoold/application.conf`.

## Deploying Scoold to Google App Engine

1. Clone this repo and change directory to it
2. Create a project in the Google Cloud Platform Console
3. Install the Google Cloud SDK
4. Delete `Dockerfile` and `app.yml`
4. Edit `app.gae.yaml` to suit your needs
6. Deploy it with `gcloud preview app deploy app.gae.yaml`

## Deploying Scoold to a servlet container

The instructions for Tomcat in particular are:

1. Generate a WAR package with `mvn -Pwar package`
2. Rename the WAR package to `ROOT.war` if you want it deployed to the root context or leave it as is
3. Put the WAR package in `Tomcat/webapps/` & start Tomcat
4. Put `application.conf` in `Tomcat/webapps/scoold-folder/WEB-INF/classes/` & restart Tomcat

Scoold is compatible with Tomcat 9+.

## Deploying Scoold under a specific context path

To deploy Scoold at a different path instead of the root path, set `scoold.context_path = "/newpath`. The default value
for this setting is blank, meaning Scoold will be deployed at the root directory.

## Migrating from one Para deployment to another

There are situations where you want to transfer your data from one Para server to another. This may be because you
decided to switch databases or hosting providers. This process is made simple with the backup and restore feature in
Scoold and Scoold Pro. Simply go to the Administration page and download all your data from the source installation.
Then on the target installation go to the Administration page and import the ZIP file which contains the backup.

**Important:** All data will be overwritten on restore, so it's highly recommended that the target Scoold installation
is fresh and containing no data.

When using the default H2 database, you can also copy the `./data` directory to the new installation or just copy all
`*.db` files. The data directory also contains Lucene index folders for each app, e.g. `./data/scoold-lucene`. These
folders can also be moved and copied or even deleted. You can easily restore the Lucene index for a Para app by running
a rebuild index task from the `para-cli` tool. Here's how to rebuild the root app `para` and a child app `scoold` with
just two simple commands:

```sh
$ npm i -g para-cli
$ para-cli rebuild-index --endpoint "http://localhost:8080" --accessKey "app:para" --secretKey "secret1"
$ para-cli rebuild-index --endpoint "http://localhost:8080" --accessKey "app:scoold" --secretKey "secret2"
```

## Migrating from Stack Overflow for Teams to Scoold

1. Start Scoold and login as admin
2. Download your data archive from Stack Overflow by browsing to 'Admin settings -> Account info -> Download data'
3. On Scoold's Administration page click 'Import' and select the Stack Overflow archive (.zip)
4. Check "This archive was exported from Stack Overflow" and click import

All the data for your team on Stack Overflow, except for user badges, will be imported into Scoold.

## Upgrading from Scoold to Scoold Pro

You can seamlessly upgrade from Scoold to Scoold Pro without changing the configuration or anything else in your
infrastructure. The process is very simple:

1. Get your Scoold Pro package (JAR or WAR)
2. Stop (undeploy) Scoold and replace its package with the Scoold Pro package
3. Start Scoold Pro (or redeploy your Scoold Pro WAR file)

## Content-Security-Policy header

This header is enabled by default for enhanced security. It can be disabled with `scoold.csp_header_enabled = false`.
The default value is modified through `scoold.csp_header = "new_value"`. The default CSP header is:
```ini
default-src 'self';
base-uri 'self';
connect-src 'self' scoold.com www.google-analytics.com www.googletagmanager.com accounts.google.com;
frame-src 'self' accounts.google.com staticxx.facebook.com;
font-src cdnjs.cloudflare.com fonts.gstatic.com fonts.googleapis.com;
style-src 'self' 'unsafe-inline' fonts.googleapis.com cdnjs.cloudflare.com static.scoold.com accounts.google.com;
img-src 'self' https: data:;
object-src 'none;
report-uri /reports/cspv;
script-src 'unsafe-inline' https: 'nonce-{{nonce}}' 'strict-dynamic';
```

The placeholder `{{nonce}}` will get replaced by the CSP nonce value used for whitelisting scripts.

**Note:** If you get CSP violation errors, check your `scoold.host_url` and `scoold.cdn_url` configuration,
or edit the value of `scoold.csp_header`.

Additionally, there are 4 options to extend the values of `connect-src`, `frame-src`, `font-src` and `style-src`
respectively:
```ini
scoold.csp_connect_sources = "connect-domain1.com connect-domain2.com"
scoold.csp_frame_sources = "frame-domain1.com frame-domain2.com"
scoold.csp_font_sources = "font-domain1.com font-domain2.com"
scoold.csp_style_sources = "style-domain1.com style-domain2.com"
```

You can also enable or disable CSP violation reports (visible only to admins) by setting `scoold.csp_reports_enabled = true`.
Keep in mind that if your website has a lot of traffic, this will result in hundreds of new reports being created each hour.

## External scripts and JS snippets

You can append external scripts and JS snippets to the end of the page by setting the `scoold.external_scripts` property.
Scripts are loaded in alphabetical order based on their key.
```ini
# URL
scoold.external_scripts.myscript1 = "https://mydomain.com/script.js"
# Base64 encoded long JavaScript snippet
scoold.external_scripts.myscript2 = "J2Y2M3VlcH .... enZ2OScpOw=="
# Short raw JS snippet
scoold.external_scripts.myscript3 = "var x = 5; console.log('x is', x);"
```

**Important:** Watch out for console errors in the browser after you add external scripts. In such cases you might have to
modify the `frame-src` or `connect-src` portions of the CSP header (see the 4 options above).

If 3rd party cookie consent is enabled (for GDPR, CCPA), all external scripts will be disabled until the user gives their
consent. You can bypass that by prefixing its key with "bypassconsent", e.g. `scoold.external_scripts.bypassconsent_myscript2`.

Additionally, you can put scripts in the `<head>` element by prefixing their name with "head", for example:
`scoold.external_scripts.head_script`.

## External CSS stylesheets

You can inline short snippets of CSS using `scoold.inline_css`. Keep in mind that any inlined CSS rules **will
override** any of the previously declared stylesheets, including the main stylesheet rules.

```ini
scoold.inline_css = "body { color: #abcdef; }"
```
Another option is to add external stylesheets to the website:
```ini
scoold.external_styles = "https://mydomain.com/style1.css, https://mydomain.com/style2.css"
```
The last option is to completely replace the main stylesheet with a custom one. It's a good idea to copy the default
CSS rules from [`/styles/style.css`](https://live.scoold.com/styles/style.css) and modify those, then upload the new
custom stylesheet file to a public location and set:

```ini
scoold.stylesheet_url = "https://public.cdn.com/custom.css"
```

The order in which CSS rules are loaded is this (each overrides the previous ones):
1. main stylesheet, 2. external stylesheets, 3. inline CSS or custom theme

## Serving static files from local disk

By default, Scoold will only serve static files from the `/static` folder on the classpath (inside the JAR).
If you want to configure it to serve additional resources from a local directory, set this system property:
```ini
spring.web.resources.static-locations = "classpath:/static/, file:/home/scoold/static-folder/"
```
Then, for example, a file located at `/home/scoold/static-folder/file.png` will be served from `localhost:8000/file.png`.
For more information, check the
[Spring Boot documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#web.servlet.spring-mvc.static-content).

## Serving static files from a CDN

Scoold will serve static files (JS, CSS, fonts) from the same domain where it is deployed. You can configure the
`scoold.cdn_url` to enable serving those files from a CDN. The value of the CDN URL *must not* end in "/".

## SMTP configuration

Scoold uses the JavaMail API to send emails. If you want Scoold to send notification emails you should add the
following SMTP settings to your config file:

```ini
# system email address
scoold.support_email = "support@scoold.com"

scoold.mail.host = "smtp.example.com"
scoold.mail.port = 587
scoold.mail.username = "user@example.com"
scoold.mail.password = "password"
scoold.mail.tls = true
scoold.mail.ssl = false

# enable SMTP debug logging
scoold.mail.debug = true
```
The email template is located in `src/main/resources/emails/notify.html`.

Email notifications are enabled by default but can also be turned off completely by changing these settings:
```ini
# disable all notifications
scoold.notification_emails_allowed = true
# disable notifications for new posts
scoold.newpost_emails_allowed = true
# disable notifications for new posts that match favorite tags
scoold.favtags_emails_allowed = true
# disable notifications for replies
scoold.reply_emails_allowed = true
# disable notifications for comments
scoold.comment_emails_allowed = true

# additional options for Scoold Pro
# disable notifications for mentions
scoold.mentions_emails_allowed = true
```

For **Gmail** you have to turn on "Less secure app access" in your Google account settings. There's no need to configure
`mail.tls` or `mail.ssl`, just set the `mail.host` to `smtp.gmail.com` and your Gmail email and password.

## Email verification

You can enable or disable the email verification step by setting `scoold.security.allow_unverified_emails = true`
(in Scoold's `application.conf`). By default, email verification is turned off when Scoold is running in development mode.
This will allow new users to register with fake emails and Scoold will not send them a confirmation email. It's useful
for testing purposes or in certain situations where you want to programmatically sign up users who don't have an email.

## reCAPTCHA support

You can protect signups and password reset functionality with reCAPTCHA v3. First you will need to register a new domain
at [Google reCAPTCHA](https://www.google.com/recaptcha/admin). Create a new reCAPTCHA v3, add your site to the whitelist
and copy the two keys - a clientside key (site key) and a serverside key (secret). Then, protect the pages
`/signin/register` and `/signin/iforgot` by adding these properties to your configuration:
```ini
scoold.signup_captcha_site_key = "site-key-from-google"
scoold.signup_captcha_secret_key = "secret-from-google"
```

## Delete protection for valuable content

By default, Scoold will protect valuable questions and answers from accidental deletion. If a question has at least one
answer, the author of that question will not be able to delete it. Or, if an answer is accepted by the author of the
question, the person who wrote the answer won't be able to delete it. You can turn this off with:
```ini
scoold.delete_protection_enabled = false
```

## Welcome email customization

To customize the message sent when a new user signs up with Scoold, modify these properties in your Scoold configuration
file:
```ini
scoold.emails.welcome_text1 = "You are now part of {0} - a friendly Q&A community..."
scoold.emails.welcome_text2 = "To get started, simply navigate to the "Ask question" page and ask a question..."
scoold.emails.welcome_text3 = "Best, <br>The {0} team<br><br>"
```

## Social login

For all social identity providers, you need to obtain both the OAuth2 client ID (app `id`) and secret key.
**Note:** if the credentials are blank, the sign in button is hidden for that provider.
```ini
# Facebook
scoold.fb_app_id = ""
scoold.fb_secret = ""
# Google
scoold.gp_app_id = ""
scoold.gp_secret = ""
# GitHub
scoold.gh_app_id = ""
scoold.gh_secret = ""
# LinkedIn
scoold.in_app_id = ""
scoold.in_secret = ""
# Twitter
scoold.tw_app_id = ""
scoold.tw_secret = ""
# Microsoft
scoold.ms_app_id = ""
scoold.ms_secret = ""
scoold.ms_tenant_id = ""
# Slack
scoold.sl_app_id = ""
scoold.sl_secret = ""
# Amazon
scoold.az_app_id = ""
scoold.az_secret = ""
```
You also need to set your host URL when running Scoold in production:
```ini
scoold.host_url = "https://your.scoold.url"
```
This is required for authentication requests to be redirected back to the origin.

**Important:** You must to whitelist the [Para endpoints](https://paraio.org/docs/#031-github) in the admin consoles of
each authentication provider. For example, for GitHub you need to whitelist `https://parahost.com/github_auth` as a
callback URL (redirect URL). Same thing applies for the other providers. For these two providers you need to whitelist
these two URLs, containing the public address of Scoold:
```
https://myscoold.com
https://myscoold.com/signin
```
For locally hosted authentication providers (SAML, LDAP, Mattermost, etc.) the authentication endpoints will also be
pointing to the URL of your Scoold server.

**If you skip this step, authentication will most likely not work.**

In some cases ([see related issue](https://github.com/Erudika/scoold/issues/199)) you want to have Scoold connect to
Para which is hosted somewhere on your local network and logging in with some authentication providers, like Microsoft,
doesn't work. In such cases you would see an error "redirect_uri mismatch" or "invalid redirect_uri - must start with
https:// or http://localhost". To make it work you can set `scoold.security.redirect_uri = "https://public-para.host"`
while still keeping `scoold.para_endpoint = "http://local-ip:8080"`.

## OAuth 2.0 login

You can authenticate users against any OAuth 2.0/OpenID Connect server through the generic OAuth 2 filter in Para.
Make sure you **whitelist** your Para authentication endpoint `https://para_url/oauth2_auth` as a trusted redirect URL.

Here are all the options which you can set in the Scoold configuration file:
```ini
# minimal setup
scoold.oa2_app_id = ""
scoold.oa2_secret = ""
scoold.security.oauth.authz_url = "https://your-idp.com/login"
scoold.security.oauth.token_url = "https://your-idp.com/token"
scoold.security.oauth.profile_url = "https://your-idp.com/userinfo"
scoold.security.oauth.scope = "openid email profile"

# extra options
scoold.security.oauth.accept_header = ""
scoold.security.oauth.domain = "paraio.com"
scoold.security.oauth.parameters.id = "sub"
scoold.security.oauth.parameters.picture = "picture"
scoold.security.oauth.parameters.email = "email"
scoold.security.oauth.parameters.name = "name"
scoold.security.oauth.parameters.given_name = "given_name"
scoold.security.oauth.parameters.family_name = "family_name"

# Sets the string on the login button
scoold.security.oauth.provider = "Continue with OpenID Connect"

# [PRO] Enable/disable access token delegation
scoold.security.oauth.token_delegation_enabled = false

# [PRO] Assigns spaces to each user from the OAuth2 claim 'spaces'
scoold.security.oauth.spaces_attribute_name = "spaces"

# [PRO] Assigns moderator/admin roles from the OAuth2 claim 'roles'
scoold.security.oauth.groups_attribute_name = "roles"
scoold.security.oauth.mods_equivalent_claim_value = "mod"
scoold.security.oauth.admins_equivalent_claim_value = "admin"
# if specified, users will be denied access when not members of group
scoold.security.oauth.users_equivalent_claim_value = ""

# Enable/disable avatar fetching from IDP
scoold.security.oauth.download_avatars = false
```

#### Access token delegation
**PRO** This is an additional security feature, where the access token from the identity provider (IDP)
is stored in the user's `idpAccessToken` field and validated on each authentication request with the IDP. If the IDP
revokes a delegated access token, then that user would automatically be logged out from Scoold Pro and denied access
immediately.

#### Advanced attribute mapping
The basic profile data attributes (name, email, etc.) can be extracted from a complex response payload which is returned
from the identity provider's `userinfo` endpoint. You can use JSON pointer syntax to locate attribute values within a
more complex JSON payload like this one:
```
{
    "sub": "gfreeman",
    "attributes": {
        "DisplayName": "Gordon Freeman",
        "DN": "uid=gordon,CN=Users,O=BlackMesa",
        "Email": "gordon.freeman@blackmesa.gov"
    }
}
```
The corresponding configuration to extract the name and email address would be:
```ini
scoold.security.oauth.parameters.email = "/attributes/Email"
scoold.security.oauth.parameters.name = "/attributes/DisplayName"
```

#### Advanced roles mapping
**PRO** This feature requires token delegation to be enabled with `scoold.security.oauth.token_delegation_enabled = true`.
When working with complex user profile payloads coming from the ID provider, you can specify the exact property
name where the roles data is contained. For example, having a JSON user profile response like this:
```
{
    "sub": "gfreeman",
    "DisplayName": "Gordon Freeman",
    "Roles": "Staff,Admins,TopSecret",
    "attributes": {
        "MemberOf": [
            "CN=Admins,CN=Lab,O=BlackMesa"
        ]
    }
}
```
we can map that user to the Scoold admins group with this configuration:
```ini
scoold.security.oauth.groups_attribute_name = "Roles"
scoold.security.oauth.admins_equivalent_claim_value = ".*?Admins.*"
```
Regular expressions are supported for searching within the roles attribute value. JSON pointers can also be used here
like so:
```ini
scoold.security.oauth.groups_attribute_name = "/attributes/MemberOf"
scoold.security.oauth.admins_equivalent_claim_value = "^CN=Admins.*"
```
Additionally, when assigning roles from OAuth2 claims, you can explicitly specify a subset of allowed users who can access
Scoold by setting `scoold.security.oauth.users_equivalent_claim_value`. For example, if the value of that is set
to `"scoold_user"`, and a user having the claim of `"roles": ["sales_rep"]` tries to login, they will be denied access.
By default, all OAuth2 users are allowed to log into Scoold.

#### Additional custom OAuth 2.0 providers
You can add two additional custom OAuth 2.0/OpenID connect providers called "second" and "third". Here's what the settings
look like for the "second" provider:

```ini
# minimal setup (second provider)
scoold.oa2second_app_id = ""
scoold.oa2second_secret = ""
scoold.security.oauthsecond.authz_url = "https://your-idp.com/login"
scoold.security.oauthsecond.token_url = "https://your-idp.com/token"
scoold.security.oauthsecond.profile_url = "https://your-idp.com/userinfo"
scoold.security.oauthsecond.scope = "openid email profile"

# extra options (second provider)
scoold.security.oauthsecond.accept_header = ""
scoold.security.oauthsecond.domain = "paraio.com"
scoold.security.oauthsecond.parameters.id = "sub"
scoold.security.oauthsecond.parameters.picture = "picture"
scoold.security.oauthsecond.parameters.email = "email"
scoold.security.oauthsecond.parameters.name = "name"
scoold.security.oauthsecond.parameters.given_name = "given_name"
scoold.security.oauthsecond.parameters.family_name = "family_name"

# Sets the string on the login button (second provider)
scoold.security.oauthsecond.provider = "Continue with Second OAuth 2.0 provider"

# Enable/disable access token delegation (second provider)
scoold.security.oauthsecond.token_delegation_enabled = false
```

For the "third" OAuth 2.0 provider it's the same configuration but replace "second" with "third".

**Note:** If Para and Scoold are hosted both on the same server and your Para instance is not publicly accessible from
the Internet, you need to expose `localhost:8080/oauth2_auth` by configuring a proxy server to forward
`yourdomain/oauth2_auth` requests to `localhost:8080/oauth2_auth`. If Para is publicly accessible this step is not necessary.

#### Sign in with Okta

This is an example guide for configuring Scoold to work with an authentication provider like Okta. The steps are similar
for other providers, such as Auth0.

1. Create a new client application (OAuth 2 client)
   - Add `http://para-host:8080/oauth2_auth` as a login redirect URI
   - Use the "Authorization Code" flow
   - Select	that you want **client credentials**
2. Copy the client credentials (client id, secret) to your Scoold `application.conf` file:
```ini
scoold.oa2_app_id = "0oa123...."
scoold.oa2_secret = "secret"
scoold.security.oauth.authz_url = "https://${yourOktaDomain}/oauth2/v1/authorize"
scoold.security.oauth.token_url = "https://${yourOktaDomain}/oauth2/v1/token"
scoold.security.oauth.profile_url = "https://${yourOktaDomain}/oauth2/v1/userinfo"
scoold.security.oauth.scope = "openid email profile"
scoold.security.oauth.provider = "Continue with Okta"
```
Make sure to replace `${yourOktaDomain}` with your actual Okta domain name.

3. Restart Scoold and login with an Okta user account

#### Sign in with Azure Active Directory (AAD)

This is an example guide for configuring Scoold to use Azure Active Directory (aka Microsoft Identity Platform)
as its authentication provider. The steps are similar to other OAuth2.0 identity providers setup.

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to your *Azure Acive Directory* tenant
3. Go to *App registrations* (in the left sidebar, under *Manage* section)
4. Choose *New registration*
5. Put the name of the new app, and select supported account types (accoding to your requirements)
6. Provide the *Redirect URI* - it needs to point to Para's `/oauth2_auth` endpoint (make sure that
   this URL is accessible from your users' devices). For development purposes `http://localhost:8080/oauth2_auth`
   is probably sufficient.
7. Click *Register*.
8. Copy the *Application (client) ID* that you should be seeing now at the top - it is the value for `scoold.oa2_app_id`
   setting in your configuration.
9. Navigate to *Certificates and Secrets* in the sidebar on the left.
10. Create a new secret by clicking on *New client secret*.
11. Copy the generated secret (you will not be able to see that secret anymore on Azure Portal) - it is the value
    for `scoold.oa2_secret` setting in your configuration.
12. Fill in the configuration of Scoold:

```ini
scoold.oa2_app_id = "e538..."
scoold.oa2_secret = "secret"
scoold.security.oauth.authz_url = "https://login.microsoftonline.com/${yourAADTenantId}/oauth2/v2.0/authorize"
scoold.security.oauth.token_url = "https://login.microsoftonline.com/${yourAADTenantId}/oauth2/v2.0/token"
scoold.security.oauth.profile_url = "https://graph.microsoft.com/oidc/userinfo"
scoold.security.oauth.scope = "openid email profile"
scoold.security.oauth.provider = "Continue with AAD"
```

Make sure to replace `${yourAADTenantId}` with your actual AAD tenant ID.

13. Restart Scoold and login with an AAD user account

## LDAP configuration

LDAP authentication is initiated with a request like this `POST /signin?provider=ldap&access_token=username:password`.
There are several configuration options which Para needs in order to connect to your LDAP server. These are the defaults:

```ini
# minimal setup
scoold.security.ldap.server_url = "ldap://localhost:8389/"
scoold.security.ldap.base_dn = "dc=springframework,dc=org"
scoold.security.ldap.user_dn_pattern = "uid={0}"
# add this ONLY if you are connecting to Active Directory
scoold.security.ldap.active_directory_domain = ""

# extra options - change only if necessary
scoold.security.ldap.user_search_base = ""
scoold.security.ldap.user_search_filter = "(cn={0})"
scoold.security.ldap.password_attribute = "userPassword"
scoold.security.ldap.username_as_name = false

# Sets the string on the login button (PRO)
scoold.security.ldap.provider = "Continue with LDAP"

# automatic groups mapping
scoold.security.ldap.mods_group_node = ""
scoold.security.ldap.admins_group_node = ""
```

The search filter syntax allows you to use the placeholder `{0}` which gets replaced with the person's username.

You can also map LDAP DN nodes to Para user groups. For example, with the following configuration:
```
scoold.security.ldap.mods_group_node = "ou=Moderators"
scoold.security.ldap.admins_group_node = "cn=Admins"
```
LDAP users with a DN `uid=Gordon,ou=Moderators,dc=domain,dc=org` will automatically become part of the `mods` group,
i.e. `groups: "mods"`. Similarly, if their DN contains `cn=Admins` they will become administrators, i.e. `groups: "admins"`.

### Active Directory LDAP

For **Active Directory** LDAP, the search filter defaults to `(&(objectClass=user)(userPrincipalName={0}))`.
A good alternative search filter would be `(&(objectClass=user)(sAMAccountName={1}))`. Keep in mind that the domain you
put in the configuration is actually the UPN suffix which gets appended to the username as `username@domain.com` if
the supplied login username doesn't end with a domain. The domain has nothing to do with the AD domain or the location
of the AD server.

The only valid configuration properties for AD are:
`user_search_filter`, `base_dn`, `server_url` and `active_directory_domain` - everything else is ignored so don't put
it in the config file at all!

Here's a working LDAP configuration for AD:
```ini
scoold.security.ldap.user_search_filter = "(&(objectClass=user)(sAMAccountName={1}))"
scoold.security.ldap.base_dn = "ou=dev,dc=scoold,dc=com"
scoold.security.ldap.server_url = "ldap://192.168.123.70:389"
scoold.security.ldap.active_directory_domain = "scoold.com"
```

For the above configuration the following logins should work, given that a user `joe` exists:
- `joe@scoold.com` + password
- `joe@some-other-domain.com` + password
- `joe` + password

As you can see the domain part is actually ignored because it is irrelevant. You cannot bind an AD user with their email.
You can bind them based on their `username` a.k.a. `sAMAccountName`. If the user has an email address where the alias is
the same as the `sAMAccountName` but the domain is different, then the login will succeed. If the user above has an email
`joe.smith@gmail.com` then the login with that email will fail because a bind is not possible,
and the LDAP search request will return no results.

The syntax for the search filter allows you to use the placeholders `{0}` (replaced with `username@domain`) and `{1}`
(replaced with `username` only).

Here's an example **Active Directory** configuration (note that any other settings than the ones below will be ignored):
```ini
scoold.security.ldap.server_url = "ldap://server:389"
scoold.security.ldap.active_directory_domain = "domain.com"
scoold.security.ldap.user_search_filter = "userPrincipalName={0}"
scoold.security.ldap.base_dn = "ou=dev,dc=domain,dc=com"
```

### FreeIPA LDAP

Scoold supports authentication with a FreeIPA server over LDAP. Here's a sample configuration for the free demo instance
provided by FreeIPA - [ipa.demo1.freeipa.org](https://ipa.demo1.freeipa.org):

```ini
scoold.security.ldap.server_url = "ldap://ipa.demo1.freeipa.org:389"
scoold.security.ldap.base_dn = "cn=users,cn=accounts,dc=demo1,dc=freeipa,dc=org"
scoold.security.ldap.user_dn_pattern = "uid={0}"
```
To test this, try logging in with user `manager` and password `Secret123`.

### Local (internal) LDAP authentication

**PRO** Scoold Pro can authenticate users with an internal (local) LDAP server, even if your Para backend is hosted outside
of your network (like ParaIO.com). This adds an extra layer of security and flexibility and doesn't require a publicly
accessible LDAP server. To enable this feature, add this to your configuration:
```ini
scoold.security.ldap.is_local = true
# required for passwordless authentication with Para
scoold.app_secret_key = "change_to_long_random_string"
```
Note that the secret key above is **not** the same as your Para secret key! You have to generate a random string for that
(min. 32 chars).

To print out debug information about LDAP requests, start Para with `-Dlogging.level.org.springframework.ldap=DEBUG`.
If you are connecting to an internal LDAP server, add the same system property to the Scoold command line.

Please, read the [LDAP docs for Para](https://paraio.org/docs/#030-ldap) to learn more about the settings above.

## SAML configuration

**PRO**
First, you have to setup Para as a SAML service provider using the config below. Then you can exchange SAML metadata with
your SAML identity provider (IDP). The SP metadata endpoint is `/saml_metadata/{appid}`. For example, if your Para
endpoint is `paraio.com` and your `appid` is `scoold`, then the metadata is available at
`https://paraio.com/saml_metadata/scoold`.

SAML authentication is initiated by sending users to the Para SAML authentication endpoint `/saml_auth/{appid}`.
For example, if your Para endpoint is `paraio.com` and your `appid` is `scoold`, then the user should be sent to
`https://paraio.com/saml_auth/scoold`. Para (the service provider) will handle the request and redirect to the SAML IDP.
Finally, upon successful authentication, the user is redirected back to `https://paraio.com/saml_auth/scoold` which is
also the assertion consumer service (ACS).

**Note:** The X509 certificate and private key must be encoded as Base64 in the configuration file. Additionally,
the private key must be in the **PKCS#8 format** (`---BEGIN PRIVATE KEY---`). To convert from PKCS#1 to PKCS#8, use this:
```
openssl pkcs8 -topk8 -inform pem -nocrypt -in sp.rsa_key -outform pem -out sp.pem
```
For simplicity, you can generate the certificates for SAML by executing the Bash script `gencerts.sh`,
located in this repository:
```
./gencerts.sh localhost secret
```
Then, simply Base64-encode the contents of the public key `localhost.pem` and private key `localhost.key`:
```
base64 localhost.key > localhost_saml_base64.key
base64 localhost.pem > localhost_saml_base64.pem
```

There are lots of configuration options but Para needs only a few of those in order to successfully
authenticate with your SAML IDP (listed in the first rows below).

```ini
# minimal setup
# IDP metadata URL, e.g. https://idphost/idp/shibboleth
scoold.security.saml.idp.metadata_url = ""

# SP endpoint, e.g. https://paraio.com/saml_auth/scoold
scoold.security.saml.sp.entityid = ""

# SP public key as Base64(x509 certificate)
scoold.security.saml.sp.x509cert = ""

# SP private key as Base64(PKCS#8 key)
scoold.security.saml.sp.privatekey = ""

# attribute mappings (usually required)
# e.g. urn:oid:0.9.2342.19200300.100.1.1
scoold.security.saml.attributes.id = ""
# e.g. urn:oid:0.9.2342.19200300.100.1.3
scoold.security.saml.attributes.email = ""
# e.g. urn:oid:2.5.4.3
scoold.security.saml.attributes.name = ""


# extra options (optional)
# this is usually the same as the "EntityId"
scoold.security.saml.sp.assertion_consumer_service.url = ""
scoold.security.saml.sp.nameidformat = ""

# IDP metadata is usually automatically fetched
scoold.security.saml.idp.entityid = ""
scoold.security.saml.idp.single_sign_on_service.url = ""
scoold.security.saml.idp.x509cert = ""

scoold.security.saml.security.authnrequest_signed = false
scoold.security.saml.security.want_messages_signed = false
scoold.security.saml.security.want_assertions_signed = false
scoold.security.saml.security.want_assertions_encrypted = false
scoold.security.saml.security.want_nameid_encrypted = false
scoold.security.saml.security.sign_metadata = false
scoold.security.saml.security.want_xml_validation = true
scoold.security.saml.security.signature_algorithm = ""

scoold.security.saml.attributes.firstname = ""
scoold.security.saml.attributes.picture = ""
scoold.security.saml.attributes.lastname = ""
scoold.security.saml.domain = "paraio.com"

# Sets the string on the login button
scoold.security.saml.provider = "Continue with SAML"
```

Scoold Pro can authenticate users with an internal (local) SAML provider, even if your Para backend is hosted outside of
your network (like ParaIO.com). This adds an extra layer of security and flexibility and doesn't require your SAML
endpoints to be publicly accessible. To enable this feature, add this to your configuration:
```ini
scoold.security.saml.is_local = true
# required for passwordless authentication with Para
scoold.app_secret_key = "change_to_long_random_string"
```
Note that the secret key above is **not** the same as your Para secret key! You have to generate a random string for that
(min. 32 chars).

## Custom authentication (Single Sign-on)

**PRO**
Para supports custom authentication providers through its "passwordless" filter. This means that you can send any
user info to Para and it will authenticate that user automatically without passwords. The only verification done here is
on this secret key value which you provide in your Scoold Pro configuration file:
```ini
scoold.app_secret_key = "change_to_long_random_string"
```
This key is used to protect requests to the passwordless filter and it's different from the Para secret key for your app.
Here's the basic authentication flow:

1. A user wants to sign in to Scoold Pro and clicks a button
2. The button redirects the user to a remote login page you or your company set up.
3. The user enters their credentials and logs in.
4. If the credentials are valid, you send back a special JSON Web Token (JWT) to Scoold with the user's basic information.
5. Scoold verifies the token and the user is signed in to Scoold

The JWT must contain the following claims:

- `email` - user's email address
- `name` - user's display name
- `identifier` - a unique user id in the format `custom:123`
- `appid` - the app id (optional)

The JWT is signed with the value of `scoold.app_secret_key` and should have a short validity period (e.g. 10 min).
The JWT should also contain the claims `iat` and `exp` and, optionally, `nbf`. Supported signature algorithms for the JWT
are `HS256`, `HS384` or `HS512`.
Once you generate the JWT on your backend (step 4 above), redirect the successful login request back to Scoold:
```
GET https://scoold-host/signin/success?jwt=eyJhbGciOiJIUzI1NiI..&passwordless=true
```

The UI button initiating the authentication flow above can be customized like this:
```ini
scoold.security.custom.provider = "Continue with Acme Co."
# location of your company's login page
scoold.security.custom.login_url = ""
```

There's an [example login page](https://albogdano.github.io/scoold-login-page/) implementing this sort of authentication.

## Login and logout redirects

You can configure Scoold to redirect users straight to the identity provider when they click the "Sign in" button.
This feature is disabled by default:
```ini
scoold.redirect_signin_to_idp = false
```
This works only for social login identity providers and SAML. It won't work for LDAP or
basic password authentication. When enabled and combined with `scoold.is_default_space_public = false`,
unauthenticated users will be sent directly to the IDP without seeing the "Sign in" page or any other page on Scoold.

You can also configure users to be redirected to an external location when they log out:
```ini
scoold.signout_url = "https://homepage.com"
```

## SCIM 2.0 support

**PRO**
Scoold Pro has a dedicated SCIM API endpoint for automatic user provisioning at `http://localhost:8000/scim`.
This allows you to manage Scoold Pro users externally, on an identity management platform of your choice.
Here's an example configuration for enabling SCIM in Scoold:

```ini
scoold.scim_enabled = true
scoold.scim_secret_token = "secret"
scoold.scim_map_groups_to_spaces = true
scoold.scim_allow_provisioned_users_only = false
```
By default, Scoold Pro will create a space for each SCIM `Group` it receives from your identity platform and assign the
members of that group to the corresponding space. Just make sure that groups are pushed from your IdM platform to Scoold.

If `scoold.scim_allow_provisioned_users_only = true`, user accounts which have not been SCIM-provisioned will be blocked
even if those users are members of your identity pool. This allows system administrators to provision a subset of the
user pool in Scoold.

**Important:** When users are provisioned from a SCIM client (Azure AD, Okta, OneLogin, etc.) ensure that the SCIM
`userName` attribute is unique. This means that when a SCIM client sends an update to Scoold Pro,
the user will be matched based on their `userName`.

You can also map groups from your identity pool to Para user groups. For example:
```ini
scoold.security.scim.mods_group_equivalent_to = "Moderators"
scoold.security.scim.admins_group_equivalent_to = "Administrators"
```

## Spaces (a.k.a. Teams)

Spaces are a way to organize users and questions into isolated groups. There's a default space, which is publicly
accessible by default. Each user can belong to one or more spaces, but a question can only belong to a single space.
Permission to access a space is given by an administrator. You can bulk edit users' spaces and also move a question to a
different space.

By default there's a public "default" space where all questions go. When you create a new space and assign users to it
they will still see all the other questions when they switch to the "default" space. To make the default space private
set `scoold.is_default_space_public = false`.

**PRO** In Scoold PRO you can create as many space as you need. The open source version is limited to 10 spaces. Also
in PRO you can automatically assign multiple spaces to new users, whereas in the OSS version you can only assign one.

If you want to assign space(s) to new users automatically, add this to your configuration:
```ini
# put space ids here, the "scooldspace:" prefix is optional
scoold.auto_assign_spaces = "my-space-one,my-other-space"
```
You can assign both the default space and a custom space to new users (values can contain spaces):
```ini
scoold.auto_assign_spaces = "default,My Custom Space"
```
When using the option above, new spaces are added to existing spaces for each user. You can configure auto-assigned
spaces to overwrite the existing user spaces (like the "default" space, assigned to everyone) by setting:
```ini
scoold.reset_spaces_on_new_assignment = true
```
So when you have that set to `false` and you have configured Scoold to assign custom spaces to new users
(e.g. "my-space-1" and "my-space-2"), those users will become members of  "my-space-1", "my-space-2" **and** the
default space. If the value is `true`, the default space gets overwritten by the custom spaces you have specified in
`scoold.auto_assign_spaces` and new users will only be members of "my-space-1" and "my-space-2".

This is turned on for all users authenticated with LDAP, SAML or OAuth 2.0.

Alternatively, Scoold Pro can have spaces and roles delegated to users from an OpenID Connect/OAuth 2.0 identity provider.
You have to enable access token delegation with `scoold.security.oauth.token_delegation_enabled = true` and Scoold Pro will
try to obtain spaces from a custom attribute like `spaces`. Such custom attributes can be configured in the IDP and
pushed to clients (Scoold Pro) embedded in access tokens. If you want to change the name of the custom attribute supplied
by your IDP, set `scoold.security.oauth.spaces_attribute_name`, which by default is equal to `spaces`. The value of that
attribute should contain comma-separated list of spaces. If the spaces pushed from the IDP do not exist, Scoold will
create them for you.

## Webhooks

Webhooks are enabled by default in Scoold. To disable this functionality set `scoold.webhooks_enabled = false`. If you
are self-hosting Para, you need to also enable webhooks there using the same configuration option.
You can add or remove webhooks in the "Administration" page. Webhooks can also be disabled and they will be
disabled automatically when the target URL doesn't respond to requests from Para.

Para will notify your target URL with a `POST` request containing the payload and a `X-Webhook-Signature` header. This
header should be verified by the receiving party by computing `Base64(HmacSHA256(payload, secret))`.

The standard events emitted by Scoold are:

- `question.create` - whenever a new question is created
- `question.close` - whenever a question is closed
- `answer.create` - whenever a new answer is created
- `answer.accept` - whenever an answer is accepted
- `report.create` - whenever a new report is created
- `comment.create` - whenever a new comment is created
- `user.signup` - whenever a new user is created
- `revision.restore` - whenever a revision is restored
- `user.ban` -  <kbd>Pro</kbd> whenever a user is banned
- `user.mention` -  <kbd>Pro</kbd> whenever a user is mentioned
- `question.like` - <kbd>Pro</kbd> whenever a question is favorited

In addition to the standard event, Para also sends webhooks to the following core (CRUD) events, for all object types:
`create`, `update`, `delete`, `createAll`, `updateAll`, `deleteAll`.

You can subscribe to any of these custom events in Scoold using the REST API like so:
```POST /api/webhooks
{
  "targetUrl": "https://myurl",
  "typeFilter": "revision",
  "urlEncoded": false,
  "create": true,
  "update": false,
  "delete": false,
  "createAll": true,
  "updateAll": false,
  "deleteAll": false,
  "customEvents": ["revision.create"]
}
```
This will create a new custom event `revision.create` which will fire whenever a new revision is created.
This makes it easy to integrate Scoold with services like Zapier because it implements the
[RESTHooks](https://resthooks.org/) best practices.

For more details about webhooks, please read the [Para docs on webhooks](https://paraio.org/docs/#011-webhooks).

## Session management and duration

By default, only one session is allowed per user/browser. When a user logs in from one device, they will automatically be
logged out from every other device. This can be disabled to allow multiple simultaneous sessions with:

```ini
scoold.security.one_session_per_user = false
```

User session cookies in Scoold expire after 24h. To change the session duration period to 6h for example, set
`scoold.session_timeout = 21600` (6h in seconds) and restart. In 6h the Scoold authentication cookie will expire and so
will the access token (JWT) inside the cookie.

## Domain-restricted user registrations

You can restrict signups only to users from a particular identity domain, say `acme-corp.com`. To do so, set the
following configuration property:
```ini
scoold.approved_domains_for_signups = "acme-corp.com"
```
Then a user with email `john@acme-corp.com` will be allowed to login (the identity provider is irrelevant), but user
`bob@gmail.com` will be denied access.

**PRO** In Scoold PRO this setting can also contain a comma-separated list of identity domains:
```ini
scoold.approved_domains_for_signups = "acme-corp.com,gmail.com"
```

## Admins

You can specify the user with administrative privileges in your `application.conf` file:
```ini
scoold.admins = "joe@example.com"
```
**PRO** In Scoold PRO you can have multiple admin users by specifying a comma-separated list of user identifiers.
This works both for new and existing users.
```ini
scoold.admins = "joe@example.com,fb:1023405345366,gh:1234124"
```

If you remove users who are already admins from the list of admins `scoold.admins`, they will be *demoted* to regular
users. Similarly, existing regular users will be *promoted* to admins if they appear in the list above.

## Anonymous posts

**PRO**
This feature is enabled with `scoold.anonymous_posts_enabled = true`. It allows everyone to ask questions and write
replies, without having a Scoold account. Posting to the "Feedback" section will also be open without requiring users
to sign in. This feature is disabled by default.

## Anonymous profiles

People may wish to make their profile details anonymous from the Settings page. To allow this option set:
```ini
scoold.profile_anonimity_enabled = true
```

## Enabling the "Feedback" section

You can enable or disable the "Feedback" page where people can discuss topics about the website itself or submit general
feedback. This section is disabled by default and can be activated with `scoold.feedback_enabled = true`.

## LaTeX/MathML support and advanced highlighting

**PRO**
You can enable this feature by setting `scoold.mathjax_enabled = true`. Then you can use MathML expressions by surrounding
them with `$$` signs, e.g. `$$ \frac {1}{2} $$` By default, MathJax is disabled.

The Prism syntax highlighter is included and it supports many different languages. You need to specify the language for
each code block if you want the highlighting to be more accurate (see [all supported languages](https://prismjs.com/#languages-list)).
For example:

    ```csharp
    var dict = new Dictionary<string>();
    ```

## File uploads

**PRO**
Files can be uploaded to the local file system or to cloud storage. File uploads are enabled by default in Scoold Pro.
To disable file uploads altogether set `scoold.uploads_enabled = false`. To protect uploaded files from unauthenticated
access, set `scoold.uploads_require_auth = true`.

To upload a file just **drag & drop** the file onto the post editor area. A link will automatically appear
when the upload is finished. Uploads can fail either because their size is too large or because their format is not in
the white list of permitted formats (documents, images, archives, audio or video). You can extend the list of permitted
file types by configuring:
```ini
scoold.allowed_upload_formats = "yml,py:text/plain,json:application/json"
```
If the MIME type is not specified in the format `extension:mime_type`, the default `text/plain` is used when serving these
files.

By default, the size of uploaded files is unrestricted. To limit the maximum size of uploads, set the following Spring
Boot system properties:
```ini
spring.servlet.multipart.max-file-size = "2MB"
spring.servlet.multipart.max-request-size = "10MB"
```

Scoold Pro also allows users to capture and upload video and audio from their input devices (mic/webcam).
This functionality is enabled by default:
```ini
scoold.media_recording_allowed = true
```

Profile pictures (avatars) can also be changed by dragging a new image on top of the existing profile picture on a
user's `/profile` page. For best results, use a square image here.

### Local storage
Local file storage is used by default. To configure the directory on the server where files will be stored, set:
```ini
scoold.file_uploads_dir = "uploads"
```

### AWS S3 storage provider
To use S3 for file storage, specify the name of the S3 bucket where you want the files to be uploaded. AWS credentials
and region settings are optional as they can be picked up from the environment automatically.
```ini
# required
scoold.s3_bucket = ""
# path within the bucket (object prefix)
scoold.s3_path = "uploads"
# these are optional
scoold.s3_region = ""
scoold.s3_access_key = ""
scoold.s3_secret_key = ""
```

### Azure Blob storage provider
To use Azure Blob storage for storing uploaded files, first you'll need to create a storage account and get a SAS URL
and token. Scoold Pro will need full permissions to your storage container so it's best that you dedicate a container
just for Scoold files.

```ini
# required
scoold.blob_storage_account = ""
scoold.blob_storage_token = ""
# name of the container
scoold.blob_storage_container = ""
# path prefix within a container (subfolder)
scoold.blob_storage_path = "uploads"
```

## Uploading custom avatars

This feature is available in both Scoold and Scoold Pro. You can configure Scoold to upload custom profile pictures to a
cloud-based image hosting service like Imgur or Cloudinary. In Scoold Pro, avatars are uploaded to the configured storage
provider, but by configuring Imgur or Cloudinary for avatars, you will essentially change where avatars are stored and
served from.

### To Imgur
To use Imgur for storing images, specify your Imgur API client id:
```
scoold.imgur_client_id = "x23e8t0askdj"
scoold.avatar_repository = "imgur"
```
Keep in mind that *only images* can be uploaded to Imgur and other restrictions may apply.

### To Cloudinary
To use Cloudinary for storing images, specify your Cloudinary API client id:
```
scoold.cloudinary_url = "cloudinary://123456:abcdefaddd@scoold"
scoold.avatar_repository = "cloudinary"
```
Keep in mind that *only images* can be uploaded to Cloudinary and other restrictions may apply.

## Slack integration

Scoold **PRO** integrates with Slack on a number of levels. First, Scoold users can sign in with Slack. They can also
use slash commands to search and post questions. Also Scoold can notify Slack users when they are mentioned on Scoold.
Finally, Scoold allows you to map spaces to Slack workspaces or channels. By default, each Slack workspace (team) is
mapped to a single Scoold space when people sign in with Slack.

**Important:** Most of the Slack operations require a **valid Slack ID stored in Scoold** which enables the mapping of
Slack users to Scoold accounts and vice versa. Slack IDs are set automatically when a Scoold user signs in with Slack.

The integration endpoint for Slack is `/slack` - this is where Scoold will accept and process requests from Slack.
To enable the Slack integration you need to register for a Slack app first and set `scoold.sl_app_id` and `scoold.sl_secret`.

### [Getting started guide for Scoold + Slack](https://scoold.com/slack.html).

Here are the configuration properties for Slack:
```ini
scoold.slack.app_id = "SHORT_APPID"
scoold.slack.map_workspaces_to_spaces = true
scoold.slack.map_channels_to_spaces = false
scoold.slack.post_to_space = "workspace|scooldspace:myspace|default"

scoold.slack.notify_on_new_question = true
scoold.slack.notify_on_new_answer = true
scoold.slack.notify_on_new_comment = true
scoold.slack.dm_on_new_comment = false
scoold.slack.default_question_tags = "via-slack"
scoold.slack.auth_enabled = true
```

Setting `scoold.slack.map_channels_to_spaces` will ask for additional permissions, namely `channels:read` and `groups:read`.
On sign in, Scoold will read all your channels and create spaces from them. The workspace space is always created if
`scoold.slack.map_workspaces_to_spaces = true`, which is the default setting.

When creating questions from Slack they are posted to the channel workspace by default, if
`scoold.slack.map_channels_to_spaces` is enabled. For example, in this case, for a team "My Team" and channel "chan",
your space will become "My Team #chan". This is controlled by `scoold.slack.post_to_space` which is blank by default.
If you set it to `workspace`, then questions will be posted to the "My Team" space. Or you could set a specific Scoold
space to post to with `scoold.slack.post_to_space = "scooldspace:myspace"`. If the value is `default` then all questions
posted from Slack will go to the default space.

You can also create answers to questions from Slack, either from the message action button or by typing in the
`/scoold ask` command. This requires the URL of a specific question you wish to answer.

Clicking the "Ask on Scoold" message action will also save the full message thread on Scoold. The action will create a new
question from the first message of the thread and save each reply as an answer on Scoold.

When `scoold.slack.dm_on_new_comment` is enabled, Scoold will send a direct message notification to the author of
the post on which somebody commented. By default, DMs are turned off and the notification is sent to the channel instead.

Slack authentication can be disabled with `scoold.slack.auth_enabled = false` and the "Continue with Slack" button
will be hidden away.

### Slash commands in Slack

Typing in `/scoold help` gives you a list of commands available:

- `/scoold` - Interact with your Scoold server
- `/scoold ask [text]` Ask a question directly, first sentence becomes the title. Example: `/scoold ask How does this work? @john I'm having troubles with...`
- `/scoold answer [question URL] [answer]` Answer a question directly. Example: `/scoold answer https://host/question/123 This is my answer to your question, @john.`
- `/scoold search [query]` Search for questions. Example: `/scoold search solution AND quick*`
- `/scoold search-people [query]` Search for people. Example: `/scoold search-people John*`
- `/scoold search-tags [query]` Search for tags. Example: `/scoold search-tags solution*`
- `/scoold version` Get version information for Scoold and Para.
- `/scoold whoami` Get information for your Scoold account.
- `/scoold stats` Get general statistics.

### Message actions in Slack

Here are the interactive message actions which are currently implemented:
- `create_question` - "Ask on Scoold", Creates a new question on Scoold directly from a chat message.
- `create_question_dialog` - "Edit & ask on Scoold", Opens up a dialog to edit question before posting.
- `create_answer_dialog` - "Answer on Scoold", Opens up a dialog to edit your answer before posting.

These allow you to perform actions from any channel and best of all, these can turn any chat message into a question or
answer.

If you get an error **"User not authorised to open dialogs"** it means that your Scoold user is not logged in via Slack
and Scoold doesn't have a Slack access token on record. Simply log into Scoold with Slack and the error should go away.

## Mattermost integration

Scoold **PRO** also integrates with Mattermost. Scoold users can sign in with Mattermost, use slash commands to interact
with Scoold and also get in-chat notification for mentions and new posts on Scoold. Scoold allows you to map spaces to
Mattermost teams or channels. By default, each Mattermost team is mapped to a single Scoold space when people sign in
with Mattermost.

**Important:** Most of the Mattermost operations require a **valid Mattermost ID stored in Scoold** which enables the
mapping of Mattermost users to Scoold accounts and vice versa. Mattermost IDs are set automatically when a Scoold user
signs in with Mattermost.

The integration endpoint for Mattermost is `/mattermost` - this is where Scoold will accept and process requests from
Mattermost. To enable the Mattermost integration you need to enable OAuth 2.0 apps and create one in Mattermost's System
Console. Then set `scoold.mm_app_id` and `scoold.mm_secret`.

### [Getting started guide for Scoold + Mattermost](https://scoold.com/mattermost.html).

Here are the configuration properties for Mattermost:
```ini
scoold.mattermost.server_url = "http://localhost:8065"
scoold.mattermost.bot_username = "scoold"
scoold.mattermost.bot_icon_url = "http://localhost:8000/images/logowhite.png"
scoold.mattermost.map_workspaces_to_spaces = true
scoold.mattermost.map_channels_to_spaces = false
scoold.mattermost.post_to_space = "workspace|scooldspace:myspace|default"

scoold.mattermost.notify_on_new_question = true
scoold.mattermost.notify_on_new_answer = true
scoold.mattermost.notify_on_new_comment = true
scoold.mattermost.dm_on_new_comment = false
scoold.mattermost.default_question_tags = "via-mattermost"
```

**Note:** Mattermost does not support message actions like in Slack. This means that you can't create a question from
a any chat message. The reply dialog box can be opened from a "Reply" button under each question notification message or
via the `/scoold answer-form` command.
The dialog box for new questions is opened via the new slash command `/scoold ask-form`.

All the other slash commands and notifications work just like with Slack and are described above. The Mattermost
integration will automatically create a slash command for each channel linked to Scoold on the admin page.

When `scoold.mattermost.dm_on_new_comment` is enabled, Scoold will send a direct message notification to the author of
the post on which somebody commented. By default, DMs are turned off and the notification is sent to the channel instead.

You can also save a full message thread on Scoold with the command `/scoold save {thread_link}` - this will create a new
question from the first message of the thread and save each reply as an answer on Scoold.

## Microsoft Teams integration

Scoold **PRO** also integrates with Microsoft Teams. Scoold users can sign in with a Microsoft account,
use bot commands to interact with Scoold and also get in-chat notification for mentions and new posts on Scoold.
Scoold allows you to map spaces to teams or channels. By default, each team in MS Teams is mapped to a single Scoold
space when people sign in with Microsoft.

**Important:** Most of the Teams operations require a **valid Microsoft ID stored in your Scoold profile** which enables the
mapping of Teams users to Scoold accounts and vice versa. Microsoft IDs are added to each profile automatically when a
Scoold user signs in with Microsoft.

The integration endpoint for Teams is `/teams` - this is where Scoold will accept and process requests from
MS Teams. To enable the Teams integration you first need to create a new bot for your app.
After creating the bot, take note of its ID and client secret and add those to your Scoold configuration file.
Then sideload (upload) the `Scoold.zip` app package in the [Teams Developer Portal](https://dev.teams.microsoft.com).
The app package can be downloaded from the Administration page after your bot has been created.
Also set `scoold.ms_app_id` and `scoold.ms_secret` as you normally would for an OAuth2 authentication with Microsoft.

**Note:** Clicking the "Ask on Scoold" message extension button will also save the full message thread on Scoold.
The action will create a new question from the first message of the thread and save each reply as an answer on Scoold.

### [Getting started guide for Scoold + Teams](https://scoold.com/teams.html).

Here are the configuration properties for MS Teams:
```ini
scoold.teams.bot_id = ""
scoold.teams.bot_secret = ""
scoold.teams.map_workspaces_to_spaces = true
scoold.teams.map_channels_to_spaces = false
scoold.teams.post_to_space = "workspace|scooldspace:myspace|default"

scoold.teams.notify_on_new_question = true
scoold.teams.notify_on_new_answer = true
scoold.teams.notify_on_new_comment = true
scoold.teams.dm_on_new_comment = false
scoold.teams.default_question_tags = "via-teams"
```

You can type `@Scoold help` to get a list of all supported actions. All the other bot commands and notifications work
just like with Slack and Mattermost, described above. A bot registration is required for the Teams integration
and it has to be created manually from the [Teams Developer Portal](https://dev.teams.microsoft.com/bots).

When `scoold.teams.dm_on_new_comment` is enabled, Scoold will send a direct message notification to the author of
the post on which somebody commented. By default, DMs are turned off and the notification is sent to the channel instead.

## Notifications in Slack/Mattermost/Teams

Scoold will notify the channels where you have it installed, whenever a new question or answer is created, and also
whenever a user is mentioned. To install the application on multiple channels go to the Administration page and click
one of the "Add to Slack/Mattermost/Teams" buttons for each channel where you wish to get notifications. You can receive
notification on up to 10 channels simultaneously. Notifications for new posts will go to the channel associated with the
space in which the post was created. For example, when using Slack, if `scoold.slack.map_workspaces_to_spaces` is `true`,
and a question is created in space "Team1 #general", Scoold will search for webhook registrations matching that
team/channel combination and only send a notification there. Direct message webhooks will be used only if there's no
space-matching channel found.

## Approving new posts from Slack/Mattermost/Teams

This works if you have enabled `scoold.posts_need_approval`. When a new question or answer is created by a user with less
reputation than the threshold, a notification message will be sent to Slack/Mattermost/Teams, giving you the option to
either approve or delete that post. The action can only be performed by moderators.

## Self-hosting Para and Scoold with HTTPS

The recommended way for enabling HTTPS with your own SSL certificate in a self-hosted environment is to run a
proxy server like NGINX in front of Scoold and Para. As an alternative you can use Apache or Lighttpd.

1. Start Para on port `:8080`
2. Start Scoold on port `:8000`
3. Start NGINX with the configuration below

<details><summary><b>Example configuration for NGINX</b></summary>

    server_tokens off;
    add_header X-XSS-Protection "1; mode=block";
    add_header X-Content-Type-Options nosniff;

    server {
      listen 80 default_server;
      listen [::]:80 default_server;
      server_name www.domain.com domain.com;

      # Redirect all HTTP requests to HTTPS with a 301 Moved Permanently response.
      return 301 https://$host$request_uri;
    }

    server {
      listen 443 ssl http2;
      listen [::]:443 ssl http2;
      server_name www.domain.com domain.com;

      # certs sent to the client in SERVER HELLO are concatenated in ssl_certificate
      ssl_certificate /etc/ssl/certs/domain.crt;
      ssl_certificate_key /etc/ssl/private/domain.key;
      ssl_session_timeout 1d;
      ssl_session_cache shared:SSL:50m;
      ssl_session_tickets off;

      # modern configuration. tweak to your needs.
      ssl_protocols TLSv1.2;
      ssl_ciphers 'ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES256-SHA384:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA256';
      ssl_prefer_server_ciphers on;

      # HSTS (ngx_http_headers_module is required) (15768000 seconds = 6 months)
      add_header Strict-Transport-Security max-age=15768000;

      # OCSP Stapling - fetch OCSP records from URL in ssl_certificate and cache them
      ssl_stapling on;
      ssl_stapling_verify on;

      # Verify chain of trust of OCSP response using Root CA and Intermediate certs
      #ssl_trusted_certificate /path/to/root_CA_cert_plus_intermediates;

      # Cloudflare DNS
      resolver 1.1.1.1;

      # Required for LE certificate enrollment using certbot
      # usually certbot would automatically modify this configuration
      #location '/.well-known/acme-challenge' {
      #  default_type "text/plain";
      #  root /var/www/html;
      #}

      location / {
        proxy_pass http://127.0.0.1:8000;
        proxy_redirect http:// $scheme://;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header Host $http_host;
      }
    }
</details>

As an alternative, you can enable SSL and HTTP2 directly in Scoold:
1. Run the script [`gencerts.sh`](gencerts.sh) to generate the required self-signed certificates
```
echo "scoold.local" | sudo tee -a /etc/hosts
./gencerts.sh scoold.local secret
```
The result of that command will be 8 files - `ScooldRootCA.(crt,key,pem)`, `scoold.local.(crt,key,pem)` as well as a
Java Keystore file `scoold-keystore.p12` and a Truststore file `scoold-truststore.p12`.
Optionally, you can run generate the server certificates using an existing `RootCA.pem` and `RootCA.key` files like so:
```
./gencerts.sh para.local secret /path/to/ca/RootCA
```

2. Run Scoold using the following command which enables SSL and HTTP2:
```
java -jar -Dconfig.file=./application.conf \
 -Dserver.ssl.key-store-type=PKCS12 \
 -Dserver.ssl.key-store=scoold-keystore.p12 \
 -Dserver.ssl.key-store-password=secret \
 -Dserver.ssl.key-password=secret \
 -Dserver.ssl.key-alias=scoold \
 -Dserver.ssl.enabled=true \
 -Dserver.http2.enabled=true \
scoold-*.jar
```
3. Trust the root CA file `ScooldRootCA.crt` by importing it in you OS keyring or browser (check Google for instructions).
4. Open `https://scoold.local:8000`

## Securing Scoold with TLS using Nginx and Certbot (Let's Encrypt)

First of all, configure the DNS records for your domain to point to the IP address where Scoold is hosted.

1. SSH into your Ubuntu server and install Nginx and Certbot
```
sudo apt-get install nginx certbot python-certbot-nginx
```
2. Get a certificate and autoconfigure nginx to use it
```
sudo certbot --nginx
```
3. Turn on the Ubuntu firewall to block port `8000` and only allow ports `80` and `443`.
```
ufw allow 'Nginx Full' && sudo ufw enable
```
4. Configure nginx to forward requests from the web on ports `80` and `443` to `localhost:8000`
```
location / {
	proxy_pass http://127.0.0.1:8000;
	proxy_redirect http:// $scheme://;
	proxy_set_header X-Real-IP $remote_addr;
	proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
	proxy_set_header X-Forwarded-Proto https;
	proxy_set_header Host $http_host;
}
```

That's it! If the Certbot validation above fails, your DNS is not configured properly or you have conflicting firewall rules.
Refer to [this article](https://www.digitalocean.com/community/tutorials/how-to-secure-nginx-with-let-s-encrypt-on-ubuntu-18-04)
for more details.

## Mutual TLS authentication

You can enable mTLS between Scoold and Para, as well as between Scoold and a proxy like Nginx.
There are two ways to do that:
- each service can trust each other's public certificate
- each service can use a TLS certificate signed by a CA which is trusted by all services

### mTLS between Scoold and an TLS-terminating Nginx proxy
To go the first route, execute the `getcerts.sh` script as shown above. You may need to run it once for Scoold and once
for Nginx, unless Nginx has its own certificate already. Then add the Nginx certificate to the Truststore.
```
./gencerts.sh scoold.local secret
keytool -v -importcert -file /path/to/nginx_public_cert.pem -alias nginx -keystore scoold-nginx-truststore.p12 -storepass secret -noprompt
```
Configure Nginx to trust the CA which was used to sign Scoold's server certificate:
```
server_name scoold-pro.local;
listen 443 ssl http2;
location / {
	proxy_pass https://scoold.local:8000;
	proxy_redirect http:// $scheme://;
	proxy_set_header X-Real-IP $remote_addr;
	proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
	proxy_set_header X-Forwarded-Proto https;
	proxy_set_header Host $http_host;

	proxy_ssl_certificate /path/to/nginx_public_cert.pem;
	proxy_ssl_certificate_key /path/to/nginx_public_cert.key;
	proxy_ssl_trusted_certificate /path/to/RootCA.pem;
  proxy_ssl_verify on;
  proxy_ssl_verify_depth 2;
}
```
<details><summary><b>Run Scoold with this command which enables TLS, HTTP2 and mTLS.</b></summary>

    java -jar -Dconfig.file=./application.conf \
     -Dserver.ssl.key-store-type=PKCS12 \
     -Dserver.ssl.key-store=scoold-keystore.p12 \
     -Dserver.ssl.key-store-password=secret \
     -Dserver.ssl.key-password=secret \
     -Dserver.ssl.trust-store=scoold-nginx-truststore.p12 \
     -Dserver.ssl.trust-store-password=secret \
     -Dserver.ssl.key-alias=scoold \
     -Dserver.ssl.client-auth=need \
     -Dserver.ssl.enabled=true \
     -Dserver.http2.enabled=true
    scoold-*.jar

</details>

If you want to trust the Root CA instead, the steps are similar but in the Nginx configuration use this line:
```
ssl_client_certificate /path/to/RootCA.pem;
```
And start Scoold using the previously generated Truststore `scoold-truststore.p12` which should already contain the Root CA.

### mTLS between Scoold and Para
To go the first route, execute the `getcerts.sh` script as shown above for both Scoold and Para. Then create a Truststore
for each service which contains the certificate of the other.
```
./gencerts.sh scoold.local secret
./gencerts.sh para.local secret
keytool -v -importcert -file scoold.local.pem -alias scoold -keystore para-scoold-truststore.p12 -storepass secret -noprompt
keytool -v -importcert -file para.local.pem -alias para -keystore scoold-para-truststore.p12 -storepass secret -noprompt
```
<details><summary><b>Run Para with this command which enables TLS, HTTP2 and mTLS.</b></summary>

    java -jar -Dconfig.file=/para/application.conf \
     -Dserver.ssl.key-store-type=PKCS12 \
     -Dserver.ssl.key-store=para-keystore.p12 \
     -Dserver.ssl.key-store-password=secret \
     -Dserver.ssl.key-password=secret \
     -Dserver.ssl.trust-store=para-scoold-truststore.p12 \
     -Dserver.ssl.trust-store-password=secret \
     -Dserver.ssl.key-alias=para \
     -Dserver.ssl.client-auth=need \
     -Dserver.ssl.enabled=true \
     -Dserver.http2.enabled=true
    para-*.jar

</details>
<details><summary><b>Run Scoold with this command which enables TLS, HTTP2 and mTLS.</b></summary>

    java -jar -Dconfig.file=/scoold/application.conf \
     -Dserver.ssl.key-store-type=PKCS12 \
     -Dserver.ssl.key-store=scoold-keystore.p12 \
     -Dserver.ssl.key-store-password=secret \
     -Dserver.ssl.key-password=secret \
     -Dpara.client.ssl_keystore=scoold-keystore.p12 \
     -Dpara.client.ssl_keystore_password=secret \
     -Dpara.client.ssl_truststore=scoold-para-truststore.p12 \
     -Dpara.client.ssl_truststore_password=secret \
     -Dserver.ssl.key-alias=scoold \
     -Dserver.ssl.enabled=true \
     -Dserver.http2.enabled=true
    scoold-*.jar

</details>

If you want to trust the Root CA instead, the steps are similar but using the previously generated Truststores
- `scoold-truststore.p12` and `para-truststore.p12` respectively, which should already contain the Root CA.

For end-to-end encryption of traffic, you can enable mTLS between your TLS-terminating proxy (like nginx), Scoold and Para.

### Complex proxy server setup

In some rare cases, your Scoold server may be behind more than one proxy server. In these cases you have configuration like
this:
```ini
scoold.host_url = "http://192.168.3.4:8000"
scoold.host_url = "http://192.168.3.4:8080"
```
This would work except for transactional emails where inbound links point to the wrong address. The solution is to add this
to your configuration:
```ini
scoold.rewrite_inbound_links_with_fqdn = "https://public-scoold-domain.com"
```

## Periodic summary emails (email digest)

**PRO**
You can choose to enable periodic summary emails for all users in Scoold or allow them to opt-in for these messages.
By default summary emails are disabled and users can unsubscribe if they are enabled by admins.
A summary email contains all new questions for a past period of time (daily, weekly). Admins can enable summary emails
for everyone from the Settings page if `scoold.summary_email_controlled_by_admins = true`. If that parameter is `false`
each person (by default) controls whether they want to receive summary emails or not.

The period for which a summary report is generated is controlled by:
```ini
scoold.summary_email_period_days = 2
```
The values of this setting can range from `1` to `30` days, where `2` means "every other day", `7` means "every week".
The summary email contains a list of the top 25 recent questions. For more questions set `scoold.summary_email_items = 30`.

## Mentions

**PRO**
In Scoold Pro you can mention anyone in a question, answer or comment with `@Name`. A popup menu will appear once you
start typing after `@` giving you a list of names to choose from. The selected user will be mentioned with a special
mention tag in the form of `@<userID|John Doe>`. You can edit the name part of that tag (after `|`) but nothing else,
if you want the mention to work. You can mention up to 10 people in a post.

Users can opt-in to receive email notifications when they are mentioned or that can be switched on/off by admins.
For the latter option set:
```ini
scoold.mention_emails_controlled_by_admins = true
```

## Security headers

Scoold attaches several security headers to each response. These can be enabled or disabled with the following configuration
properties:

```ini
# Strict-Transport-Security
scoold.hsts_header_enabled = true

# X-Frame-Options
scoold.framing_header_enabled = true

# X-XSS-Protection
scoold.xss_header_enabled = true

# X-Content-Type-Options
scoold.contenttype_header_enabled = true

# Referrer-Policy
scoold.referrer_header_enabled = true
```

## Voting

By default, votes expire after a certain period, meaning the same user can vote again on the same post
(after 30 days by default). Votes can also be amended within a certain number of seconds (30s by default).
There are two configurable parameters which allow you to modify the length of those periods:
```ini
scoold.vote_locked_after_sec = 30
scoold.vote_expires_after_sec = 2592000
```

## Customizing the UI

There are a number of settings that let you customize the appearance of the website without changing the code.
```ini
scoold.fixed_nav = false
scoold.show_branding = true
scoold.logo_url = "https://static.scoold.com/logo.svg"
scoold.logo_width = 90

# footer HTML - add your own links, etc., escape double quotes with \"
scoold.footer_html = "<a href=\"https://my.link\">My Link</a>"
# show standard footer links
scoold.footer_links_enabled = true
# favicon image location
scoold.favicon_url = "/favicon.ico"
# add your own external stylesheets
scoold.external_styles = "https://mydomain.com/style1.css, https://mydomain.com/style2.css"
# appends extra CSS rules to the main stylesheet
scoold.inline_css = ""
# edit the links in the footer of transactional emails
scoold.emails_footer_html = ""
# change the logo in transactional emails
scoold.small_logo_url = "https://scoold.com/logo.png"
# enable/disable dark mode
scoold.dark_mode_enabled = true
# enabled/disable Gravatars
scoold.gravatars_enabled = true
# pattern of default image of Gravatars (https://fr.gravatar.com/site/implement/images/)
scoold.gravatars_pattern = "retro"

# custom navbar links
scoold.navbar_link1_url = ""
scoold.navbar_link2_url = ""
scoold.navbar_link1_text = "Link1"
scoold.navbar_link2_text = "Link2"

# custom navbar menu links (shown to logged in users)
scoold.navbar_menu_link1_url = ""
scoold.navbar_menu_link2_url = ""
scoold.navbar_menu_link1_text = "Menu Link1"
scoold.navbar_menu_link2_text = "Menu Link2"

# default email notification toggles for all users
scoold.favtags_emails_enabled = false
scoold.reply_emails_enabled = false
scoold.comment_emails_enabled = false

# comment input box toggle
scoold.always_hide_comment_forms = true
```

### Custom Logo
In Scoold Pro you can change the logo of the website just by dragging and dropping a new image of your choice.

If you wish to add just a few simple CSS rules to the `<head>` element, instead of replacing the whole stylesheet,
simply add them as inline CSS:
```ini
scoold.inline_css = ".scoold-logo { width: 100px; }"
```

### Custom welcome message (banner)
You can set a short welcome message for unauthenticated users which will be displayed on the top of the page and it
can also contain HTML (**use only single quotes or escape double quotes `\\\"`**):
```ini
scoold.welcome_message = "Hello and welcome to <a href='https://scoold.com'>Scoold</a>!"
```
You can also set a custom message for users who are already logged in:
```ini
scoold.welcome_message_onlogin = "<h2>Welcome back <img src=\\\"{{user.picture}}\\\" width=30> <b>{{user.name}}</b>!</h2>"
```
Here you can use HTML tags and Mustache placeholders to show data from the `Profile` object of the logged in user.
For a list of available user properties, take a look at the
[`Profile`](https://github.com/Erudika/scoold/blob/master/src/main/java/com/erudika/scoold/core/Profile.java)
and [`Sysprop`](https://github.com/Erudika/para/blob/master/para-core/src/main/java/com/erudika/para/core/Sysprop.java)
classes.

### Custom links in navbar

There are a total of 4 slots for external links in the navbar area - two links publicly visible can go in the navbar and
another two links can go in the navbar menu, shown only to logged in users. Here's how to set a private link in the
navbar menu:
```ini
scoold.navbar_menu_link1_url = "https://homepage.com"
scoold.navbar_menu_link1_text = "Visit my page"
```

### Expert-level customization

If you want to completely customize the frontend code, clone this repository and edit the files you want:

- **HTML** templates are in `src/main/resources/templates/`
- **CSS** stylesheets can be found in `src/main/resources/static/styles/`
- **JavaScript** files can be found in `src/main/resources/static/scripts/`
- **Images** are in located in `src/main/resources/static/images/`
- **Themes** are in located in `src/main/resources/themes/`

In Scoold Pro, you don't have access to the files above but you can purchase the **Pro with Source code**
license, for full customization capability.

Also, please refer to the documentation for Spring Boot and Spring MVC.

## Third party cookie consent

Some countries have laws that require explicit cookie consent (e.g. GDPR, CCPA). Scoold can be integrated with Osano's
cookie consent script to enable the consent popup for compliance with those laws. Here's the configuration which enables
cookie consent:
```ini
scoold.cookie_consent_required = true
scoold.external_styles = "https://cdn.jsdelivr.net/npm/cookieconsent@3/build/cookieconsent.min.css"
scoold.external_scripts.bypassconsent1 = "https://cdn.jsdelivr.net/npm/cookieconsent@3/build/cookieconsent.min.js"
scoold.external_scripts.bypassconsent2 = "d2luZG93LmNvb2tpZWNvbnNlbnQuaW5pdGlhbGlzZSh7CiAgInBhbGV0dGUiOiB7CiAgICAicG9wdXAiOiB7CiAgICAgICJiYWNrZ3JvdW5kIjogIiM0NDQ0NDQiCiAgICB9LAogICAgImJ1dHRvbiI6IHsKICAgICAgImJhY2tncm91bmQiOiAiIzc3Nzc3NyIKICAgIH0KICB9LAogICJ0aGVtZSI6ICJjbGFzc2ljIiwKICAicG9zaXRpb24iOiAiYm90dG9tLWxlZnQiLAogICJ0eXBlIjogIm9wdC1pbiIsCiAgIm9uU3RhdHVzQ2hhbmdlIjogZnVuY3Rpb24ocyl7bG9jYXRpb24ucmVsb2FkKCk7fQp9KTs="
```
That last snippet of code is the Base64-encoded initialization of the cookie consent script:
```js
window.cookieconsent.initialise({
  "palette": {
    "popup": {
      "background": "#444444"
    },
    "button": {
      "background": "#777777"
    }
  },
  "theme": "classic",
  "position": "bottom-left",
  "type": "opt-in",
  "onStatusChange": function(s){location.reload();}
});
```
You can customize the above snippet however you like from [Osano's download page (Start coding link)](https://www.osano.com/cookieconsent/download/).
After you customize the snippet, it is important that you add `"onStatusChange": function(s){location.reload();}` at the end.

**Enabling cookie consent will automatically disable all external scripts (like Google Analytics, etc.),
until the user gives their explicit consent.**

Note: Any other script can be used instead, as long as it set a cookie `cookieconsent_status = "allow"`.

## REST API

The REST API can be enabled with the following configuration:
```ini
scoold.api_enabled = true
# A random string min. 32 chars long
scoold.app_secret_key = "change_to_long_random_string"
```
The API can be accessed from `/api/*` and the OpenAPI documentation and console are located at `/apidocs`.
API keys can be generated from the "Administration" page and can be made to expire after a number of hours or never
(validity period = 0). Keys are in the JWT format and signed with the secret defined in `scoold.app_secret_key`.
API keys can also be generated with any JWT library. The body of the key should contain the `iat`, `appid` and `exp`
claims and must be signed with the secret `scoold.app_secret_key`.

**Note:** The Scoold API also accepts Para "super" tokens (manually generated) or JWTs generated using the
[Para CLI tool](https://github.com/Erudika/para-cli).

You can use the public endpoint `http://localhost:8000/api` to check the health of the server. A `GET /api` will
return `200` if the server is healthy and connected to Para, otherwise status code `500` is returned.
The response body is similar to this:
```
{
  "healthy": true,
  "message": "Scoold API, see docs at http://localhost:8000/apidocs",
  "pro": false
}
```

API clients can be auto-generated using [Swagger Codegen](https://github.com/swagger-api/swagger-codegen). You can
also open the [API schema file](src/main/resources/templates/api.yaml) in [the Swagger Editor](https://editor.swagger.io/)
and generate the clients from there.

## Support

You can get support here by submitting an issue. Also you can head over to the Gitter chat room for help.
Issues related to **Scoold Pro** must be reported to [Erudika/scoold-pro](https://github.com/Erudika/scoold-pro/issues).
[Paid/priority support is also available](https://erudika.com/#support).

## Getting help

- Have a question? - [ask it on Gitter](https://gitter.im/Erudika/scoold)
- Found a bug? - submit a [bug report here](https://github.com/Erudika/scoold/issues)
- Ask a question on Stack Overflow using the [`scoold`](https://stackoverflow.com/tags/scoold/info) tag
- For questions related to Para, use the [`para`](https://stackoverflow.com/tags/para/info) tag on Stack Overflow

## Blog

### [Read more about Scoold on our blog](https://erudika.com/blog/tags/scoold/)

## Translating Scoold

You can translate Scoold to your language by copying the [English language file](https://github.com/Erudika/scoold/blob/master/src/main/resources/lang_en.properties)
and translating it. When you're done, change the file name from "lang_en.properties" to "lang_xx.properties"
where "xx" is the language code for your locale. Finally, open a pull request here.

| Language | File | Progress
--- | --- | ---
**Albanian** | [lang_sq.properties](src/main/resources/lang_sq.properties) | 0%
**Arabic** | [lang_ar.properties](src/main/resources/lang_ar.properties) | :heavy_check_mark: (Google Translate)
**Belarusian** | [lang_be.properties](src/main/resources/lang_be.properties) | 0%
**Bulgarian** | [lang_bg.properties](src/main/resources/lang_bg.properties) | :heavy_check_mark:
**Catalan** | [lang_ca.properties](src/main/resources/lang_ca.properties) | 0%
**Chinese (Traditional)** | [lang_zh_tw.properties](src/main/resources/lang_zh_tw.properties) | :heavy_check_mark: Thanks Kyon Cheng!
**Chinese (Simplified)** | [lang_zh_cn.properties](src/main/resources/lang_zh_cn.properties) | :heavy_check_mark: Thanks Kyon Cheng!
**Croatian** | [lang_hr.properties](src/main/resources/lang_hr.properties) | 0%
**Czech** | [lang_cs.properties](src/main/resources/lang_cs.properties) | 0%
**Danish** | [lang_da.properties](src/main/resources/lang_da.properties) | :heavy_check_mark: Thanks @viking1972!
**Dutch** | [lang_nl.properties](src/main/resources/lang_nl.properties) | :heavy_check_mark: Thanks Jan Halsema!
**English** | [lang_en.properties](src/main/resources/lang_en.properties) | :heavy_check_mark:
**Estonian** | [lang_et.properties](src/main/resources/lang_et.properties) | 0%
**Farsi** | [lang_fa.properties](src/main/resources/lang_fa.properties) | :heavy_check_mark: Thanks Sadegh G. Shohani!
**Finnish** | [lang_fi.properties](src/main/resources/lang_fi.properties) | 0%
**French** | [lang_fr.properties](src/main/resources/lang_fr.properties) | :heavy_check_mark: Thanks Charles Maheu!
**German** | [lang_de.properties](src/main/resources/lang_de.properties) | :heavy_check_mark: Thanks Patrick Gäckle!
**Greek** | [lang_el.properties](src/main/resources/lang_el.properties) | 0%
**Hebrew** | [lang_iw.properties](src/main/resources/lang_iw.properties) | :heavy_check_mark: Thanks David A.
**Hindi** | [lang_hi.properties](src/main/resources/lang_hi.properties) | :heavy_check_mark: Thanks Rakesh Gopathi!
**Hungarian** | [lang_hu.properties](src/main/resources/lang_hu.properties) | 0%
**Icelandic** | [lang_is.properties](src/main/resources/lang_is.properties) | 0%
**Indonesian** | [lang_in.properties](src/main/resources/lang_in.properties) | 0%
**Irish** | [lang_ga.properties](src/main/resources/lang_ga.properties) | 0%
**Italian** | [lang_it.properties](src/main/resources/lang_it.properties) | 0%
**Japanese** | [lang_ja.properties](src/main/resources/lang_ja.properties) | :heavy_check_mark: Thanks Mozy Okubo!
**Korean** | [lang_ko.properties](src/main/resources/lang_ko.properties) | :heavy_check_mark: Thanks HyunWoo Jo!
**Lithuanian** | [lang_lt.properties](src/main/resources/lang_lt.properties) | 0%
**Latvian** | [lang_lv.properties](src/main/resources/lang_lv.properties) | 0%
**Macedonian** | [lang_mk.properties](src/main/resources/lang_mk.properties) | 0%
**Malay** | [lang_ms.properties](src/main/resources/lang_ms.properties) | 0%
**Maltese** | [lang_mt.properties](src/main/resources/lang_mt.properties) | 0%
**Norwegian** | [lang_no.properties](src/main/resources/lang_no.properties) | 0%
**Polish** | [lang_pl.properties](src/main/resources/lang_pl.properties) | 0%
**Portuguese** | [lang_pt.properties](src/main/resources/lang_pt.properties) | :heavy_check_mark: Thanks Karina Varela!
**Romanian** | [lang_ro.properties](src/main/resources/lang_ro.properties) | 0%
**Russian** | [lang_ru.properties](src/main/resources/lang_ru.properties) | :heavy_check_mark: Thanks Vladimir Perevezentsev!
**Serbian** | [lang_sr.properties](src/main/resources/lang_sr.properties) | 0%
**Slovak** | [lang_sk.properties](src/main/resources/lang_sk.properties) | 0%
**Slovenian** | [lang_sl.properties](src/main/resources/lang_sl.properties) | 0%
**Spanish** | [lang_es.properties](src/main/resources/lang_es.properties) | :heavy_check_mark: Thanks Trisha Jariwala!
**Swedish** | [lang_sv.properties](src/main/resources/lang_sv.properties) | 0%
**Thai** | [lang_th.properties](src/main/resources/lang_th.properties) | 0%
**Turkish** | [lang_tr.properties](src/main/resources/lang_tr.properties) | :heavy_check_mark: Thanks Aysad Kozanoglu!
**Ukrainian** | [lang_uk.properties](src/main/resources/lang_uk.properties) | 0%
**Vietnamese** | [lang_vi.properties](src/main/resources/lang_vi.properties) | 0%

You can also change the default language of Scoold for all users by setting `scoold.default_language_code = "en"`, where
instead of "en" you enter the 2-letter code of the language of your choice.

## Building Scoold

To compile it you'll need JDK 8+ and Maven. Once you have it, just clone and build:

```sh
$ git clone https://github.com/erudika/scoold.git && cd scoold
$ mvn install
```
To run a local instance of Scoold for development, use:
```sh
$ mvn -Dconfig.file=./application.conf spring-boot:run
```

To generate a WAR package, run:
```sh
$ mvn -Pwar package
```

## Contributing

1. Fork this repository and clone the fork to your machine
2. Create a branch (`git checkout -b my-new-feature`)
3. Implement a new feature or fix a bug and add some tests
4. Commit your changes (`git commit -am 'Added a new feature'`)
5. Push the branch to **your fork** on GitHub (`git push origin my-new-feature`)
6. Create new Pull Request from your fork

Please try to respect the code style of this project. To check your code, run it through the style checker:

```sh
mvn validate
```

For more information see [CONTRIBUTING.md](https://github.com/Erudika/para/blob/master/CONTRIBUTING.md)


![Square Face](https://raw.githubusercontent.com/Erudika/scoold/master/assets/logosq.png)

## License
[Apache 2.0](LICENSE)
