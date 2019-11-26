![Scoold Q&A](https://raw.githubusercontent.com/Erudika/scoold/master/assets/header.png)

## Stack Overflow in a JAR

[![Join the chat at https://gitter.im/Erudika/scoold](https://badges.gitter.im/Erudika/scoold.svg)](https://gitter.im/Erudika/scoold?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

**Scoold** is a Q&A platform written in Java. The project was created back in 2008, released in 2012 as social network for
schools inspired by Stack Overflow. In 2017 it was refactored, repackaged and open-sourced.

Scoold can run anywhere - Heroku, DigitalOcean, AWS, Azure or any VPS hosting provider. It's lightweight (~4000 LOC),
the backend is handled by a separate service called [Para](https://github.com/Erudika/para). Scoold does not require a
database, and the controller logic is really simple because all the heavy lifting is delegated to Para.
This makes the code easy to read and can be learned quickly by junior developers.

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
- Minimal frontend JS code based on jQuery
- Modern, responsive layout powered by Materialize CSS
- Suggestions for similar questions and hints for duplicate posts
- Email notifications for post replies and comments
- Spring Boot project (single JAR)
- LDAP authentication support
- Social login (Facebook, Google, GitHub, LinkedIn, Microsoft, Slack, Twitter) with Gravatar support
- Syntax highlighting for code in posts, GFM markdown support with tables, task lists and strikethrough
- Emoji support - [cheat sheet](https://www.webpagefx.com/tools/emoji-cheat-sheet/)
- SEO friendly

### Pro Features

- Slack integration
- SAML support
- Anonymous posts
- Unlimited spaces
- Multiple admins
- Multiple identity domains
- Sticky / Favorite posts
- Advanced syntax highlighting
- Image uploads
- Security notifications
- Email digest of recent questions
- Account suspensions/permabans
- Wiki-style answers
- Mentions with notifications
- Custom authentication support

## [Buy Scoold Pro 299 EUR](https://paraio.com/scoold-pro)

## Live Demo

*The demo is deployed on a free dyno and it might take a minute to wake up.*
### [Live demo on Heroku](https://live.scoold.com)

### Quick Start (option 1 - managed Para backend, easier)

[JDK 1.8 or higher](https://openjdk.java.net/) is required to build and run the project.

0. First, you *need* to create a developer app with [Facebook](https://developers.facebook.com),
[Google](https://console.developers.google.com) or **any other identity provider** that you wish to use.
This isn't necessary if you're planning to login with LDAP, SAML or with a email and password.
Save the obtained API keys in `application.conf`, as shown below.

> **Important:** Authorized redirect URLs for Google and Facebook should look like this: `https://{your_scoold_host}`,
`https://{your_scoold_host}/signin`. For all the other identity providers you must whitelist the Para host with the
appropriate authentication endpoint. For example, for GitHub, the redirect URL could be: `https://paraio.com/github_auth`.

1. Create a new app on [ParaIO.com](https://paraio.com) and copy your access keys to a file
2. Click one of the quick deploy buttons:

<a href="https://heroku.com/deploy?template=https://github.com/Erudika/scoold" title="Deploy to Heroku">
	<img src="https://www.herokucdn.com/deploy/button.svg" alt="btn">
</a>
<a href="https://lightsail.aws.amazon.com/ls/webapp/create/instance" title="Deploy to Lightsail">
	<img src="https://s3-eu-west-1.amazonaws.com/com.scoold.files/awsdeploy.svg" height="32" alt="deploy to aws button">
</a>
<a href="https://deploy.azure.com/?repository=https://github.com/Erudika/scoold" title="Install on Azure">
	<img src="https://azuredeploy.net/deploybutton.svg" height="32" alt="btn">
</a>

### Quick Start (option 2 - self-hosted Para backend, harder)

**Note: The Para backend server is deployed separately and is required for Scoold to run.**

1. Create a new app on [ParaIO.com](https://paraio.com) and save the access keys in `application.conf` OR [run Para locally on port 8080](https://paraio.org/docs/#001-intro)
2. Create a *separate* `application.conf` for Scoold and configure it to connect to Para on port 8080
3. Start Scoold on port 8000: `java -jar -Dserver.port=8000 -Dconfig.file=./application.conf scoold.jar` OR `mvn spring-boot:run`
4. Open `http://localhost:8000` in your browser

If you run Para locally, use the [Para CLI](https://github.com/Erudika/para-cli) tool to create a separate app for Scoold:
```sh
# run setup and enter the keys for the root app and endpoint 'http://localhost:8080'
$ para-cli setup
$ para-cli ping
$ para-cli new-app "scoold" --name "Scoold"
```

> **Important: Do not use the same `application.conf` file for both Para and Scoold!**
Keep the two applications in separate directories, each with its own configuration file.
The settings shown below are all meant to be part of the Scoold config file.

[Read the Para docs for more information.](https://paraio.org/docs)

**SMTP settings must be configured correctly before you start using Scoold if you want to signup with an email and password.**

## Configuration

> **Important:** Create a separate app for Scoold, instead of using `app:para`, when hosting your own instance of Para.

The most important settings are `para.endpoint` - the URL of the Para server, as well as,
`para.access_key` and `para.secret_key`. Connection to a Para server *is required* for Scoold to run.

Copy the Scoold example configuration below to your **`application.conf`** and edit it if necessary:
```ini
para.app_name = "Scoold"
# the port for Scoold
para.port = 8000
# Session cookie name
para.auth_cookie = "scoold-auth"
# change this to "production" later
para.env = "development"
# the URL where Scoold is hosted, or http://localhost:8000
para.host_url = "https://your-scoold-domain.com"
# the URL of Para - could also be "http://localhost:8080"
para.endpoint = "https://paraio.com"
# access key for your Para app
para.access_key = "app:scoold"
# secret key for your Para app
para.secret_key = ""
# enable or disable email and password authentication
para.password_auth_enabled = true
# if false, commenting is allowed after 100+ reputation
para.new_users_can_comment = true
# if true, posts by new users require approval from moderator
para.posts_need_approval = false
# reputation needed for posts to be auto-approved
para.posts_rep_threshold = 100
# needed for geolocation filtering of posts
para.gmaps_api_key = ""
# Enable/disable near me feature (geolocation)
para.nearme_feature_enabled = false
# the identifier of admin user - check Para user object
para.admins = "admin@domain.com"
# GA code
para.google_analytics_id = "UA-123456-7"
# enables syntax highlighting in posts
para.code_highlighting_enabled = true
# Facebook - create your own Facebook app first!
para.fb_app_id = "123456789"
# Google - create your own Google app first!
para.google_client_id = "123-abcd.apps.googleusercontent.com"
# If true, the default space will be accessible by everyone
para.is_default_space_public = true
# If true, users can change their profile pictures
para.avatar_edits_enabled = true
# If true, users can change their names
para.name_edits_enabled = true
# Enable/disable webhooks support
para.webhooks_enabled = true
# Enable/disable wiki style answers
para.wiki_answers_enabled = true
# Comment limits
para.max_comments_per_id = 1000
para.max_comment_length = 255
```

On startup, Scoold will try to connect to Para 10 times, with a 10 second interval between retries. After that it will
fail and the settings will not be persisted. If you set the maximum number of retries to `-1` there will be an infinite
number of attempts to connect to Para. These parameters are controlled by:

```
para.connection_retries_max = 10
para.connection_retry_interval_sec = 10
```

## Docker

Tagged Docker images for Scoold are located at `erudikaltd/scoold` on Docker Hub.
First, have your Scoold `application.conf` configuration file ready in the current directory and run this command:

```
$ docker run -ti -p 8000:8000 --rm -v $(pwd)/application.conf:/scoold/application.conf \
  -e JAVA_OPTS="-Dconfig.file=/scoold/application.conf" erudikaltd/scoold
```

**Environment variables**

`JAVA_OPTS` - Java system properties, e.g. `-Dpara.port=8000`
`BOOT_SLEEP` - Startup delay, in seconds

**Docker Compose**

You can start the whole stack, Para + Scoold, with a single command using `docker-compose`.
First, create a new directory and copy `docker-compose.yml` to it from this repository. Create these 4 files:

1. `para.env` - containing environment variables for Para, like `JAVA_OPTS`
2. `scoold.env` - containing environment variables for Scoold, like `JAVA_OPTS`
3. `para-application.conf` - containing the Para configuration (see example below)
4. `scoold-application.conf` - containing the Scoold configuration (see example above)

An example `para.env` file:
```sh
JAVA_OPTS=-Dconfig.file=/para/application.conf
```
Example for `scoold.env`:
```sh
JAVA_OPTS=-Dconfig.file=/scoold/application.conf
BOOT_SLEEP=6
```
Example for `para-application.conf`:
```ini
para.env = "production"
para.dao = "H2DAO"
```

Then you can start both Scoold and Para with Docker Compose like so:
```
$ docker-compose up
```
Follow the quick start guide above to initialize Para and create a new app for Scoold. Once you have the access keys
for that app, update `scoold-application.conf` with those and restart the Para + Scoold Docker stack.

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
a special access key and secret for AWS ECR which you save as a new profile in your `~/.aws/credentials`. Then execute
the following BASH commands:
```
# authenticate docker with ECR using the temporary access token
$(aws --profile ScooldEcrProfile ecr get-login --no-include-email --region eu-west-1)
# list all tags
aws --profile ScooldEcrProfile ecr list-images --repository-name scoold-pro
# pull image with a specific tag
docker pull 374874639893.dkr.ecr.eu-west-1.amazonaws.com/scoold-pro:{tag}
```

The `:latest` tag is not supported. The command `aws get-login` gives you an access token to the private Docker registry
which is valid for **12 hours**.

For connecting Kubernetes to AWS ECR, please refer to [this article](https://medium.com/@damitj07/how-to-configure-and-use-aws-ecr-with-kubernetes-rancher2-0-6144c626d42c).


## Deploying Scoold to Heroku

**One-click deployment**

[![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy?template=https://github.com/Erudika/scoold)

**Manual deployment**

1. First, clone this repository and create a new Heroku app
2. Add Heroku as a Git remote target and push your changes with `git push heroku master`
3. Go to the Heroku admin panel, under "Settings", "Reveal Config Vars" and set all the configuration variables shown above.
4. Open the app in your browser at `https://{appname}.herokuapp.com`.

**Note**: On Heroku, all configuration variables (config vars) **must** be set without dots ".", for example `para.endpoint`
becomes `para_endpoint`.

It's also helpful to install the Heroku CLI tool.

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

**Elastic Container Service**

1. Find [Scoold on the AWS Marketplace](https://aws.amazon.com/marketplace/pp/B07M97M63H)
2. Click "Subscribe" and "Continue to Configuration/Launch"
3. Follow the usage instructions and pull the container image from the given ECR registry URL

**Elatic Beanstalk**

1. Clone this repo and change directory to it
2. Generate a WAR package with `mvn -Pwar package`
3. [Create a new Beanstalk web app](https://console.aws.amazon.com/elasticbeanstalk/home?region=eu-west-1#/newApplication?applicationName=Scoold&platform=Tomcat&tierName=WebServer&instanceType=t1.micro)
4. Upload the WAR package `target/scoold-x.y.z.war` to Beanstalk, modify any additional options and hit "Create"

## Deploying Scoold to Azure

[![Deploy to Azure](https://azuredeploy.net/deploybutton.svg)](https://deploy.azure.com/?repository=https://github.com/Erudika/scoold)

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

To deploy Scoold at a different path instead of the root path, set `para.context_path = "/newpath`. The default value
for this setting is blank, meaning Scoold will be deployed at the root directory.

## Content-Security-Policy header

This header is enabled by default for enhanced security. It can be disabled with `para.csp_header_enabled = false`.
The default value is modified through `para.csp_header = "new_value"`. The default CSP header is:
```ini
default-src 'self';
base-uri 'self';
connect-src 'self' scoold.com www.google-analytics.com www.googletagmanager.com;
frame-src 'self' accounts.google.com staticxx.facebook.com;
font-src cdnjs.cloudflare.com fonts.gstatic.com fonts.googleapis.com;
style-src 'self' 'unsafe-inline' fonts.googleapis.com cdnjs.cloudflare.com static.scoold.com;
img-src 'self' https: data:;
object-src 'none;
report-uri /reports/cspv;
script-src 'unsafe-inline' https: 'nonce-{{nonce}}' 'strict-dynamic';
```

The placeholder `{{nonce}}` will get replaced by the CSP nonce value used for whitelisting scripts.

**Note:** If you get CSP violation errors, check your `para.host_url` and `para.cdn_url` configuration,
or edit the value of `para.csp_header`.

Additionally, there are 4 options to extend the values of `connect-src`, `frame-src`, `font-src` and `style-src`
respectively:
```
para.csp_connect_sources = "connect-domain1.com connect-domain2.com"
para.csp_frame_sources = "frame-domain1.com frame-domain2.com"
para.csp_font_sources = "font-domain1.com font-domain2.com"
para.csp_style_sources = "style-domain1.com style-domain2.com"
```

You can also enable or disable CSP violation reports (visible only to admins) by setting `para.csp_reports_enabled = true`.
Keep in mind that if your website has a lot of traffic, this will result in hundreds of new reports being created each hour.

## External scripts and JS snippets

You can append external scripts and JS snippets to the end of the page by setting the `para.external_scripts` property.
```
# URL
para.external_scripts.myscript1 = "https://mydomain.com/script.js"
# Base64 encoded JavaScript snippet
para.external_scripts.myscript2 = "J2Y2M3VlcH .... enZ2OScpOw=="
```

**Important:** Watch out for console errors in the browser after you add external scripts. In such cases you might have to
modify the `frame-src` or `connect-src` portions of the CSP header (see the 4 options above).

## Serving static files from a CDN

Scoold will serve static files (JS, CSS, fonts) from the same domain where it is deployed. You can configure the
`para.cdn_url` to enable serving those files from a CDN. The value of the CDN URL *must not* end in "/".

## SMTP configuration

Scoold uses the JavaMail API to send emails. If you want Scoold to send notification emails you should add the
following SMTP settings to your config file:

```ini
# system email address
para.support_email = "support@scoold.com"

para.mail.host = "smtp.example.com"
para.mail.port = 587
para.mail.username = "user@example.com"
para.mail.password = "password"
para.mail.tls = true
para.mail.ssl = false

# enable SMTP debug logging
para.mail.debug = true
```
The email template is located in `src/main/resources/emails/notify.html`.

For **Gmail** you have to turn on "Less secure app access" in your Google account settings. There's no need to configure
`mail.tls` or `mail.ssl`, just set the `mail.host` to `smtp.gmail.com` and your Gmail email and password.

## Email verification

You can enable or disable the email verification step by setting `para.security.allow_unverified_emails = true`
(in Scoold's `application.conf`). By default, email verification is turned off when Scoold is running in development mode.
This will allow new users to register with fake emails and Scoold will not send them a confirmation email. It's useful
for testing purposes or in certain situations where you want to programmatically sign up users who don't have an email.

## Social login

For authenticating with Facebook or Google, you only need your Google client id
(e.g. `123-abcd.apps.googleusercontent.com`), or Facebook app id (only digits).
For all the other providers, GitHub, LinkedIn, Twitter, Slack and Microsoft, you need to set both the app id and secret key.
**Note:** if the credentials are blank, the sign in button is hidden for that provider.
```ini
# Facebook
para.fb_app_id = "123456789"
# Google
para.google_client_id = "123-abcd.apps.googleusercontent.com"
# GitHub
para.gh_app_id = ""
para.gh_secret = ""
# LinkedIn
para.in_app_id = ""
para.in_secret = ""
# Twitter
para.tw_app_id = ""
para.tw_secret = ""
# Microsoft
para.ms_app_id = ""
para.ms_secret = ""
# Slack
para.sl_app_id = ""
para.sl_secret = ""
```
You also need to set your host URL when running Scoold in production:
```ini
para.host_url = "https://your.scoold.url"
```
This is required for authentication requests to be redirected back to the origin.

**Important:** You must to whitelist the [Para endpoints](https://paraio.org/docs/#031-github) in the admin consoles of
each authentication provider. For example, for GitHub you need to whitelist `https://parahost.com/github_auth` as a
callback URL (redirect URL). Same thing applies for the other providers, **except Facebook and Google**.
For these two providers you need to whitelist these two URLs, containing the public address of Scoold:
```
https://myscoold.com
https://myscoold.com/signin
```
For locally hosted authentication providers (SAML, LDAP, Mattermost, etc.) the authentication endpoints will also be
pointing to the URL of your Scoold server.

**If you skip this step, authentication will most likely not work.**

## OAuth 2.0 login

You can authenticate users against any OAuth 2.0/OpenID Connect server through the generic OAuth 2 filter in Para.
Here are all the options which you can set in the Scoold configuration file:
```ini
# minimal setup
para.oa2_app_id = ""
para.oa2_secret = ""
para.security.oauth.authz_url = "https://your-idp.com/login"
para.security.oauth.token_url = "https://your-idp.com/token"
para.security.oauth.profile_url = "https://your-idp.com/userinfo"
para.security.oauth.scope = "openid email profile"

# extra options
para.security.oauth.accept_header = ""
para.security.oauth.domain = "paraio.com"
para.security.oauth.parameters.id = "sub"
para.security.oauth.parameters.picture = "picture"
para.security.oauth.parameters.email = "email"
para.security.oauth.parameters.name = "name"

# Sets the string on the login button
para.security.oauth.provider = "Continue with OpenID Connect"

# Enable/disable access token delegation
para.security.oauth.token_delegation_enabled = false
```

**Access token delegation** is an additional security feature, where the access token from the identity provider (IDP)
is stored in the user's `idpAccessToken` field and validated on each authentication request with the IDP. If the IDP
revokes a delegated access token, then that user would automatically be logged out from Scoold and denied access
immediately.

You can add two additional custom OAuth 2.0/OpenID connect providers called "second" and "third". Here's what the settings
look like for the "second" provider:

```ini
# minimal setup (second provider)
para.oa2second_app_id = ""
para.oa2second_secret = ""
para.security.oauthsecond.authz_url = "https://your-idp.com/login"
para.security.oauthsecond.token_url = "https://your-idp.com/token"
para.security.oauthsecond.profile_url = "https://your-idp.com/userinfo"
para.security.oauthsecond.scope = "openid email profile"

# extra options (second provider)
para.security.oauthsecond.accept_header = ""
para.security.oauthsecond.domain = "paraio.com"
para.security.oauthsecond.parameters.id = "sub"
para.security.oauthsecond.parameters.picture = "picture"
para.security.oauthsecond.parameters.email = "email"
para.security.oauthsecond.parameters.name = "name"

# Sets the string on the login button (second provider)
para.security.oauthsecond.provider = "Continue with Second OAuth 2.0 provider"

# Enable/disable access token delegation (second provider)
para.security.oauthsecond.token_delegation_enabled = false
```

For the "third" OAuth 2.0 provider it's the same configuration but replace "second" with "third".

## LDAP configuration

LDAP authentication is initiated with a request like this `POST /signin?provider=ldap&access_token=username:password`.
There are several configuration options which Para needs in order to connect to your LDAP server. These are the defaults:

```ini
# minimal setup
para.security.ldap.server_url = "ldap://localhost:8389/"
para.security.ldap.base_dn = "dc=springframework,dc=org"
para.security.ldap.user_dn_pattern = "uid={0}"
# add this ONLY if you are connecting to Active Directory
para.security.ldap.active_directory_domain = ""

# extra options
para.security.ldap.user_search_base = ""
para.security.ldap.user_search_filter = "(cn={0})"
para.security.ldap.password_attribute = "userPassword"
para.security.ldap.username_as_name = false

# Sets the string on the login button (PRO)
para.security.ldap.provider = "Continue with LDAP"
```

For Active Directory LDAP, the search filter defaults to `(&(objectClass=user)(userPrincipalName={0}))`. The syntax for
this allows either `{0}` (replaced with `username@domain`) or `{1}` (replaced with `username` only).
For regular LDAP, only `{0}` is a valid placeholder and it gets replaced with the person's username.

**PRO** Scoold Pro can authenticate users with an internal (local) LDAP server, even if your Para backend is hosted outside
of your network (like ParaIO.com). This adds an extra layer of security and flexibility and doesn't require a publicly
accessible LDAP server. To enable this feature, add this to your configuration:
```
para.security.ldap.is_local = true
# required for passwordless authentication with Para
para.app_secret_key = "change_to_long_random_string"
```
Note that the secret key above is **not** the same as your Para secret key! You have to generate a random string for that
(min. 32 chars).

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

There are lots of configuration options but Para needs only a few of those in order to successfully
authenticate with your SAML IDP (listed in the first rows below).

```ini
# minimal setup
# IDP metadata URL, e.g. https://idphost/idp/shibboleth
para.security.saml.idp.metadata_url = ""

# SP endpoint, e.g. https://paraio.com/saml_auth/scoold
para.security.saml.sp.entityid = ""

# SP public key as Base64(x509 certificate)
para.security.saml.sp.x509cert = ""

# SP private key as Base64(PKCS#8 key)
para.security.saml.sp.privatekey = ""

# attribute mappings (usually required)
# e.g. urn:oid:0.9.2342.19200300.100.1.1
para.security.saml.attributes.id = ""
# e.g. urn:oid:0.9.2342.19200300.100.1.3
para.security.saml.attributes.email = ""
# e.g. urn:oid:2.5.4.3
para.security.saml.attributes.name = ""


# extra options (optional)
# this is usually the same as the "EntityId"
para.security.saml.sp.assertion_consumer_service.url = ""
para.security.saml.sp.nameidformat = ""

# IDP metadata is usually automatically fetched
para.security.saml.idp.entityid = ""
para.security.saml.idp.single_sign_on_service.url = ""
para.security.saml.idp.x509cert = ""

para.security.saml.security.authnrequest_signed = false
para.security.saml.security.want_messages_signed = false
para.security.saml.security.want_assertions_signed = false
para.security.saml.security.want_assertions_encrypted = false
para.security.saml.security.want_nameid_encrypted = false
para.security.saml.security.sign_metadata = false
para.security.saml.security.want_xml_validation = true
para.security.saml.security.signature_algorithm = ""

para.security.saml.attributes.firstname = ""
para.security.saml.attributes.picture = ""
para.security.saml.attributes.lastname = ""
para.security.saml.domain = "paraio.com"

# Sets the string on the login button
para.security.saml.provider = "Continue with SAML"
```

Scoold Pro can authenticate users with an internal (local) SAML provider, even if your Para backend is hosted outside of
your network (like ParaIO.com). This adds an extra layer of security and flexibility and doesn't require your SAML
endpoints to be publicly accessible. To enable this feature, add this to your configuration:
```
para.security.saml.is_local = true
# required for passwordless authentication with Para
para.app_secret_key = "change_to_long_random_string"
```
Note that the secret key above is **not** the same as your Para secret key! You have to generate a random string for that
(min. 32 chars).

## Custom authentication (Single Sign-on)

**PRO**
Para supports custom authentication providers through its "passwordless" filter. This means that you can send any
user info to Para and it will authenticate that user automatically without passwords. The only verification done here is
on this secret key value which you provide in your Scoold Pro configuration file:
```
para.app_secret_key = "change_to_long_random_string"
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
- `identifier` - some unique ID for that user
- `appid` - the app id (optional)

The JWT is signed with the value of `para.app_secret_key` and should have a short validity period (e.g. 10 min).
The JWT should also contain the claims `iat` and `exp` and, optionally, `nbf`.
Once you generate the JWT on your backend (step 4 above), redirect the successful login request back to Scoold:
```
GET https://scoold-host/signin/success?jwt=eyJhbGciOiJIUzI1NiI..&passwordless=true
```

The UI button initiating the authentication flow above can be customized like this:
```
para.security.custom.provider = "Continue with Acme Co."
# location of your company's login page
para.security.custom.login_url = ""
```

There's an [example login page](https://albogdano.github.io/scoold-login-page/) implementing this sort of authentication.

## Spaces (a.k.a. Teams)

Spaces are a way to organize users and questions into isolated groups. There's a default space, which is publicly
accessible by default. Each user can belong to one or more spaces, but a question can only belong to a single space.
Permission to access a space is given by an administrator. You can bulk edit users' spaces and also move a question to a
different space.

By default there's a public "default" space where all questions go. When you create a new space and assign users to it
they will still see all the other questions when they switch to the "default" space. To make the default space private
set `para.is_default_space_public = false`.

**PRO** In Scoold PRO you can create as many space as you need. The open source version is limited to 10 spaces. Also
in PRO you can automatically assign multiple spaces to new users, whereas in the OSS version you can only assign one.

If you want to assign space(s) to new users automatically, add this to your configuration:
```
# put space ids here, the "scooldspace:" prefix is optional
para.auto_assign_spaces = "my-space-one,my-other-space"
```

Alternatively, Scoold Pro can have spaces delegated to users from an OpenID Connect/OAuth 2.0 identity provider.
You have to enable access token delegation with `para.security.oauth.token_delegation_enabled = true` and Scoold will
try to obtain spaces from a custom attribute like `spaces`. Such custom attributes can be configured in the IDP and
pushed to clients (Scoold) embedded in access tokens. If you want to change the name of the custom attribute supplied
by your IDP, set `para.security.oauth.spaces_attribute_name`, which by default is equal to `spaces`. The value of that
attribute should contain comma-separated list of spaces. If the spaces pushed from the IDP do not exist, Scoold will
create them for you.

## Webhooks

Webhooks are enabled by default in Scoold. To disable this functionality set `para.webhooks_enabled = false`. If you
are self-hosting Para, you need to also enable webhooks there using the same configuration option.
You can add/remove webhooks in the "Administration" page.

For more details about webhooks, please read the [Para docs on webhooks](https://paraio.org/docs/#011-webhooks).

## Domain-restricted user registrations

You can restrict signups only to users from a particular identity domain, say `acme-corp.com`. To do so, set the
following configuration property:
```
para.approved_domains_for_signups = "acme-corp.com"
```
Then a user with email `john@acme-corp.com` will be allowed to login (the identity provider is irrelevant), but user
`bob@gmail.com` will be denied access.

**PRO** In Scoold PRO this setting can also contain a comma-separated list of identity domains:
```
para.approved_domains_for_signups = "acme-corp.com,gmail.com"
```

## Admins

You can specify the user with administrative privileges in your `application.conf` file:
```
para.admins = "joe@example.com"
```
**PRO** In Scoold PRO you can have multiple admin users by specifying a comma-separated list of user identifiers.
This works both for new and existing users.
```
para.admins = "joe@example.com,fb:1023405345366,gh:1234124"
```

If you remove users who are already admins from the list of admins `para.admins`, they will be *demoted* to regular
users. Similarly, existing regular users will be *promoted* to admins if they appear in the list above.

## Anonymous posts

**PRO**
This feature is enabled with `para.anonymous_posts_enabled = true`. It allows everyone to ask questions and write
replies, without having a Scoold account. Posting to the "Feedback" section will also be open without requiring users
to sign in. This feature is disabled by default.

## Disabling the "Feedback" section

**PRO**
In Scoold PRO you can disable the "Feedback" functionality of the site by setting `para.feedback_enabled = false`.
This will remove the link to `/feedback` and disable the feature entirely.

## LaTeX/MathML support and advanced highlighting

**PRO**
You can enable this feature by setting `para.mathjax_enabled = true`. Then you can use MathML expressions by surrounding
them with `$$` signs, e.g. `$$ \frac {1}{2} $$` By default, MathJax is disabled.

The Prism syntax highlighter is included and it supports many different languages. You need to specify the language for
each code block if you want the highlighting to be more accurate (see [all supported languages](https://prismjs.com/#languages-list)).
For example:

    ```csharp
    var dict = new Dictionary<string>();
    ```

## Image uploads

**PRO**
Images can be uploaded to the local file system or Imgur. If you set the Imgur client id, Imgur is automatically selected
as the storage service. To use Imgur for file storage, specify your Imgur API client id:
```
para.imgur_client_id = "x23e8t0askdj"
```

Local file storage is used by default. To configure the directory on the server where files will be stored, set:
```
para.file_uploads_dir = "uploads"
```

In the future, more upload services could be supported such as S3. To initiate a new
image upload, open up the Markdown editor and drag'n'drop the image you want to upload. A link will automatically appear
when the upload is finished.

Profile pictures (avatars) can also be changed by dragging a new image on top of the existing profile picture on a
user's `/profile` page. For best results, use a square image here.

## Slack integration

Scoold **PRO** integrates with Slack on a number of levels. First, Scoold users can sign in with Slack. They can also
use slash commands to search and post questions. Also Scoold can notify Slack users when they are mentioned on Scoold.
Finally, Scoold allows you to map spaces to Slack workspaces or channels. By default, each Slack workspace (team) is
mapped to a single Scoold space when people sign in with Slack.

**Important:** Most of the Slack operations require a **valid Slack ID stored in Scoold** which enables the mapping of
Slack users to Scoold accounts and vice versa. Slack IDs are set automatically when a Scoold user signs in with Slack.

The integration endpoint for Slack is `/slack` - this is where Scoold will accept and process requests from Slack.
To enable the Slack integration you need to register for a Slack app first and set `para.sl_app_id` and `para.sl_secret`.
Follow the [detailed instructions here](https://scoold.com/slack.html).

Here are the configuration properties for Slack:
```
para.slack.app_id = "SHORT_APPID"
para.slack.map_workspaces_to_spaces = true
para.slack.map_channels_to_spaces = false
para.slack.post_to_space = "workspace|scooldspace:myspace|default"

para.slack.notify_on_new_question = true
para.slack.notify_on_new_answer = true
```

Setting `para.slack.map_channels_to_spaces` will ask for additional permissions, namely `channels:read` and `groups:read`.
On sign in, Scoold will read all your channels and create spaces from them. The workspace space is always created if
`para.slack.map_workspaces_to_spaces = true`, which is the default setting.

When creating quesitons from Slack they are posted to the channel workspace by default, if
`para.slack.map_channels_to_spaces` is enabled. For example, in this case, for a team "My Team" and channel "chan",
your space will become "My Team #chan". This is controlled by `para.slack.post_to_space` which is blank by default.
If you set it to `workspace`, then questions will be posted to the "My Team" space. Or you could set a specific Scoold
space to post to with `para.slack.post_to_space = "scooldspace:myspace"`. If the value is `default` then all questions
posted from Slack will go to the default space.

You can also create answers to questions from Slack, either from the message action button or by typing in the
`/scoold ask` command. This requires the URL of a specific question you wish to answer.

### Slack notifications

Scoold will notify the channels where you have it installed, whenever a new question or answer is created. To install
the application on multiple channels go to the Administration page and click the "Add to Slack" button for each channel.
You can receive notification on up to 10 channels simultaneously. Notification for new posts will go to the channel
associated with the space in which the post was created. For example if `para.slack.map_workspaces_to_spaces` is `true`,
and a question is created in space "Team1 #general", Scoold will search for webhook registrations matching that
team/channel combination and send a notification there. Direct message webhooks will be used only if there's no
space-matching channel found.

### Approving new posts from Slack

This works if you have enabled `para.posts_need_approval`. When a new question or answer is created by a user with less
reputation than the threshold, a notification message will be sent to Slack, giving you the option to either approve or
delete that post. The action can only be performed by moderators.

### Slash commands

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

### Message actions

Here are the interactive message actions which are currently implemented:
- `create_question` - "Ask on Scoold", Creates a new question on Scoold directly from a chat message.
- `create_question_dialog` - "Edit & ask on Scoold", Opens up a dialog to edit question before posting.
- `create_answer_dialog` - "Answer on Scoold", Opens up a dialog to edit your answer before posting.

These allow you to perform actions from any channel and best of all, these can turn any chat message into a question or
answer.

## Self-hosting Para and Scoold through SSL

The recommended way for enabling HTTPS with your own SSL certificate in a self-hosted environment is to run a
reverse-proxy server like NGINX in front of Scoold. As an alternative you can use Apache or Lighttpd.

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
      ssl_certificate /path/to/signed_cert_plus_intermediates;
      ssl_certificate_key /path/to/private_key;
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
      ssl_trusted_certificate /path/to/root_CA_cert_plus_intermediates;

      # Cloudflare DNS
      resolver 1.1.1.1;

      # Required for LE certificate enrollment using certbot
      location '/.well-known/acme-challenge' {
        default_type "text/plain";
        root /var/www/html;
      }

      location / {
        proxy_pass http://localhost:8000;
        proxy_redirect http:// $scheme://;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header Host $http_host;
      }
    }
</details>

## Securing Scoold with SSL using Nginx and Certbot (Let's Encrypt)

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
	proxy_pass http://localhost:8000;
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

## Periodic summary emails (email digest)

**PRO**
You can choose to enable periodic summary emails for all users in Scoold or allow them to opt-in for these messages.
By default summary emails are disabled and users can unsubscribe if they are enabled by admins.
A summary email contains all new questions for a past period of time (daily, weekly). Admins can enable summary emails
for everyone from the Settings page if `para.summary_email_controlled_by_admins = true`. If that parameter is `false`
each person (by default) controls whether they want to receive summary emails or not.

The period for which a summary report is generated is controlled by:
```
para.summary_email_period_days = 2
```
The values of this setting can range from `1` to `30` days, where `2` means "every other day", `7` means "every week".
The summary email contains a list of the top 25 recent questions. For more questions set `para.summary_email_items = 30`.

## Mentions

**PRO**
In Scoold Pro you can mention anyone in a question, answer or comment with `@Name`. A popup menu will appear once you
start typing after `@` giving you a list of names to choose from. The selected user will be mentioned with a special
mention tag in the form of `@<userID|John Doe>`. You can edit the name part of that tag (after `|`) but nothing else,
if you want the mention to work. You can mention up to 10 people in a post.

Users can opt-in to receive email notifications when they are mentioned or that can be switched on/off by admins.
For the latter option set:
```
para.mention_emails_controlled_by_admins = true
```

## Security headers

Scoold attaches several security headers to each response. These can be enabled or disabled with the following configuration
properties:

```
# Strict-Transport-Security
para.hsts_header_enabled = true

# X-Frame-Options
para.framing_header_enabled = true

# X-XSS-Protection
para.xss_header_enabled = true

# X-Content-Type-Options
para.contenttype_header_enabled = true

# Referrer-Policy
para.referrer_header_enabled = true
```

## Voting

By default, votes expire after a certain period, meaning the same user can vote again on the same post
(after 30 days by default). Votes can also be amended within a certain number of seconds (30s by default).
There are two configurable parameters which allow you to modify the length of those periods:
```
para.vote_locked_after_sec = 30
para.vote_expires_after_sec = 2592000
```

## Customizing the UI

There are a number of settings that let you customize the appearance of the website without changing the code.
```
para.fixed_nav = false
para.show_branding = true
para.logo_url = "/logo.svg"
para.logo_width = 90
para.stylesheet_url = "/style.css"
```

You can set a short welcome message for unauthenticated users which will be displayed on the top of the page:
```
para.welcome_message = "Hello and welcome to Scoold!"
```

Alternatively, clone this repository and edit the following:

- **HTML** templates are in `src/main/resources/templates/`
- **CSS** stylesheets can be found in `src/main/resources/static/styles/`
- **JavaScript** files can be found in `src/main/resources/static/scripts`
- **Images** are in located in `src/main/resources/static/images/`

Also, please refer to the documentation for Spring Boot and Spring MVC.

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
**Portuguese** | [lang_pt.properties](src/main/resources/lang_pt.properties) | 0%
**Romanian** | [lang_ro.properties](src/main/resources/lang_ro.properties) | 0%
**Russian** | [lang_ru.properties](src/main/resources/lang_ru.properties) | 0%
**Serbian** | [lang_sr.properties](src/main/resources/lang_sr.properties) | 0%
**Slovak** | [lang_sk.properties](src/main/resources/lang_sk.properties) | 0%
**Slovenian** | [lang_sl.properties](src/main/resources/lang_sl.properties) | 0%
**Spanish** | [lang_es.properties](src/main/resources/lang_es.properties) | :heavy_check_mark: Thanks Trisha Jariwala!
**Swedish** | [lang_sv.properties](src/main/resources/lang_sv.properties) | 0%
**Thai** | [lang_th.properties](src/main/resources/lang_th.properties) | 0%
**Turkish** | [lang_tr.properties](src/main/resources/lang_tr.properties) | :heavy_check_mark: Thanks Aysad Kozanoglu!
**Ukrainian** | [lang_uk.properties](src/main/resources/lang_uk.properties) | 0%
**Vietnamese** | [lang_vi.properties](src/main/resources/lang_vi.properties) | 0%


## Scoold API

**The REST API is a work in progress.** The API can be accessed from `/api` and the Swagger documentation and console
are located at `/api.html`.

You can use the public endpoint `/api` to check the health of the server. A `GET /api` will return `200` if the server
is healthy and connected to Para, otherwise status code `500` is returned. The response body is similar to this:
```
{
  "healthy": true,
  "message": "Scoold API, see docs at http://localhost:8000/api.html"
}
```

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

## Support

You can get support here by submitting an issue. Also you can head over to the Gitter chat room for help.
Issues related to **Scoold Pro** must be reported to [Erudika/scoold-pro](https://github.com/Erudika/scoold-pro/issues).
[Paid/priority support is also available](https://erudika.com/#support).

## Blog

### [Read more about Scoold on our blog](https://erudika.com/blog/tags/scoold/)

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
