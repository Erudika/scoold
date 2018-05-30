![Scoold Q&A](https://raw.githubusercontent.com/Erudika/scoold/master/assets/header.png)

## Stack Overflow in a JAR

[![Join the chat at https://gitter.im/Erudika/scoold](https://badges.gitter.im/Erudika/scoold.svg)](https://gitter.im/Erudika/scoold?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

**Scoold** is a Q&A platform written in Java. The project was created back in 2008, released in 2012 as social network for
schools inspired by StackOverflow, and as of 2017 it has been refactored, repackaged and open-sourced.
The primary goal of this project is educational but it can also work great as a Q&A/support section for your website.

Scoold can run on Heroku or any other PaaS. It's lightweight (~4000 LOC) - the backend is handled by a separate service called
[Para](https://github.com/Erudika/para). Scoold does not require a database, and the controller logic is really simple
because all the heavy lifting is delegated to Para. This makes the code easy to read and can be learned quickly by junior developers.

**This project is fully funded and supported by [Erudika](https://erudika.com) - an independent, bootstrapped company.**

### Features

- Full featured Q&A platform
- Database-agnostic, optimized for cloud deployment
- Full-text search
- Distributed object cache
- Location-based search and "near me" filtering of posts
- I18n with RTL language support
- Reputation and voting system with badges
- Spaces - groups of isolated questions and users
- Minimal frontend JS code based on jQuery
- Modern, responsive layout powered by Materialize CSS
- Suggestions for similar questions and hints for duplicate posts
- Email notifications for post replies and comments
- Spring Boot project (single JAR)
- LDAP authentication support
- Social login (Facebook, Google, GitHub, LinkedIn, Microsoft, Twitter) with Gravatar support
- Syntax highlighting for code in posts, GFM markdown support with tables, task lists and strikethrough
- Emoji support - [cheat sheet](https://www.webpagefx.com/tools/emoji-cheat-sheet/)
- SEO friendly

### Live Demo

*Scoold is deployed on a free dyno and it might take a minute to wake up.*
### [Live demo on Heroku](https://live.scoold.com)

### Quick Start

**Note: The Para backend server is deployed separately and is required for Scoold to run.**

0. You first *need* to create a developer app with [Facebook](https://developers.facebook.com),
[Google](https://console.developers.google.com) or any other identity provider that you wish to use.
This isn't necessary only if you enable LDAP or password authentication. Save the obtained API keys in `application.conf`,
as shown below.

1. Create a new app on [ParaIO.com](https://paraio.com) and save the access keys in `application.conf`
2. Click here => [![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy?template=https://github.com/Erudika/scoold)

**OR**

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

**Important: Do not use the same `application.conf` file for both Para and Scoold!**
Keep the two applications in separate directories, each with its own configuration file.
The settings shown below are all meant to part of the Scoold config file.

[Read the Para docs for more information.](https://paraio.org/docs)

### Configuration

The most important settings are `para.endpoint` - the URL of the Para server, as well as,
`para.access_key` and `para.secret_key`. Connection to a Para server *is required* for Scoold to run.

This is an example of what your **`application.conf`** should look like:
```ini
para.app_name = "Scoold"
# the port for Scoold
para.port = 8000
# change this to "production" later
para.env = "development"
# the URL where Scoold is hosted, or http://localhost:8000
para.host_url = "https://your-scoold-domain.com"
# the URL of Para - could also be "http://localhost:8080"
para.endpoint = "https://paraio.com"
# access key for your Para app
para.access_key = "app:scoold"
# secret key for your Para app
para.secret_key = "*****************"
# enable or disable email&password authentication
para.password_auth_enabled = false
# if false, commenting is allowed after 100+ reputation
para.new_users_can_comment = true
# needed for geolocation filtering of posts
para.gmaps_api_key = "********************************"
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
```

**Note**: On Heroku, the config variables above **must** be set without dots ".", for example `para.endpoint` becomes `para_endpoint`.
These are set through the Heroku admin panel, under "Settings", "Reveal Config Vars".

### Docker

Tagged Docker images for Scoold are located at `erudikaltd/scoold` on Docker Hub.
First, create an `application.conf` file in a directory and run this command:

```
$ docker run -ti -p 8080:8080 --rm -v $(pwd)/application.conf:/para/application.conf \
  -e JAVA_OPTS="-Dconfig.file=/para/application.conf" erudikaltd/scoold
```

**Environment variables**

`JAVA_OPTS` - Java system properties, e.g. `-Dpara.port=8000`
`BOOT_SLEEP` - Startup delay, in seconds

**Docker Compose**

You can start the whole stack, Para + Scoold, with a single command using `docker-compose`.
First, create a new directory and copy `docker-compose.yml` to it from this repository. Create these 4 files:

1. `para.env` - containing environment variables for Para, like `JAVA_OPTS`
2. `scoold.env` - containing environment variables for Scoold, like `JAVA_OPTS`
3. `para-application.conf` - containing the Para configuration
4. `scoold-application.conf` - containing the Scoold configuration

Then you can start both Scoold and Para with Docker Compose like so:
```
$ docker-compose up
```

### Content-Security-Policy header

This header is enabled by default for enhanced security. It can be disabled with `para.csp_header_enabled = false`.
The default value is modified through `para.csp_header = "new_value"`. The default CSP header is:
```ini
default-src 'self';
base-uri 'self';
connect-src 'self' scoold.com www.google-analytics.com;
frame-src 'self' accounts.google.com staticxx.facebook.com;
font-src cdnjs.cloudflare.com fonts.gstatic.com fonts.googleapis.com;
script-src 'self' 'unsafe-eval' apis.google.com maps.googleapis.com connect.facebook.net cdnjs.cloudflare.com www.google-analytics.com code.jquery.com static.scoold.com;
style-src 'self' 'unsafe-inline' fonts.googleapis.com cdnjs.cloudflare.com static.scoold.com;
img-src 'self' https: data:; report-uri /reports/cspv
```

**Note:** If you get CSP violation errors, check you `para.host_url` configuration, or edit the value of `para.csp_header`.

### Deploying Scoold under a specific context path

To deploy Scoold at a different path instead of the root path, set `para.context_path = "/newpath`. The default value
for this setting is blank, meaning Scoold will be deployed at the root directory.

### Serving static files from a CDN

Scoold will serve static files (JS, CSS, fonts) from the same domain where it is deployed. You can configure the
`para.cdn_url` to enable serving those files from a CDN. The value of the CDN URL will override `para.host_url` and
must not end in "/".

### SMTP configuration

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
```
The email template is located in `src/main/resources/emails/notify.html`.

## Social login

For authenticating with Facebook or Google, you only need your Google client id
(e.g. `123-abcd.apps.googleusercontent.com`), or Facebook app id (only digits).
For all the other providers, GitHub, LinkedIn, Twitter and Microsoft, you need to set both the app id and secret key.
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
```
You also need to set your host URL when running Scoold in production:
```ini
para.host_url = "https://your.scoold.url"
```
This is required for authentication requests to be redirected back to the origin.

## Spaces

Spaces are a way to organize users and questions into isolated groups. There's a default space, which is publicly
accessible by default. Each user can belong to one or more spaces, but a question can only belong to a single space.
Permission to access a space is given by an administrator. You can bulk edit users' spaces and also move a question to a different
space.

By default there's a public "default" space where all questions go. When you create a new space and assign users to it
they will still see all the other questions when they switch to the "default" space. To make the default space private
set `para.is_default_space_public = false`.

## Domain-restricted user registrations

You can restrict signups only to users from a particular domain, say `acme-corp.com`. To do so, set the following
configuration property:
```
para.approved_domains_for_signups = "acme-corp.com"
```
Then a user with email `john@acme-corp.com` will be allowed to login (the identity provider is irrelevant), but user
`bob@gmail.com` will be denied access. The setting can also contain comma-separated list of domains:
```
para.approved_domains_for_signups = "acme-corp.com,gmail.com"
```

## LDAP configuration

LDAP authentication is initiated with a request like this `GET /signin?provider=ldap&access_token=username:password`.
There are several configuration options which Para needs in order to connect to your LDAP server. These are the defaults:

```ini
para.security.ldap.server_url = "ldap://localhost:8389/"
para.security.ldap.base_dn = "dc=springframework,dc=org"
para.security.ldap.bind_dn = ""
para.security.ldap.bind_pass = ""
para.security.ldap.user_search_base = ""
para.security.ldap.user_search_filter = "(cn={0})"
para.security.ldap.user_dn_pattern = "uid={0},ou=people"
para.security.ldap.password_attribute = "userPassword"
# set this only if you are connecting to Active Directory
para.security.ldap.active_directory_domain = ""
```

For Active Directory LDAP, the search filter defaults to `(&(objectClass=user)(userPrincipalName={0}))`. The syntax for
this allows either `{0}` (replaced with `username@domain`) or `{1}` (replaced with `username` only).

## Admins

You can configure Scoold with one or more admin users in your `application.conf` file:
```
para.admins = "joe@example.com,fb:1023405345366,gh:1234124"
```
Here you can enter comma-separated values of either an email address or a user identifier
(the id from your social identity provider). This works both for new and existing Scoold users.
If you remove users who are already admins from the list of admins `para.admins`,
they will be *demoted* to regular users. Similarly, existing regular users will be
*promoted* to admins if they appear in the list above.

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
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header Host $http_host;
      }
    }
</details>

## Customizing the UI

There are a number of settings that let you customize the appearance of the website without changing the code.
```
para.fixed_nav = false
para.show_branding = true
para.logo_url = "/logo.svg"
para.logo_width = 90
para.stylesheet_url = "/style.css"
```

Alternatively, clone this repository and edit the following:

- **HTML** templates are in `src/main/resources/templates/`
- **CSS** stylesheets can be found in `src/main/resources/static/styles/`
- **JavaScript** files can be found in `src/main/resources/static/scripts`
- **Images** are in located in `src/main/resources/static/images/`

To deploy, setup Heroku as a remote to your modified Scoold repo and push your changes with:
```
$ git push heroku master
```
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
**Danish** | [lang_da.properties](src/main/resources/lang_da.properties) | 0%
**Dutch** | [lang_nl.properties](src/main/resources/lang_nl.properties) | :heavy_check_mark: Thanks Jan Halsema!
**English** | [lang_en.properties](src/main/resources/lang_en.properties) | :heavy_check_mark:
**Estonian** | [lang_et.properties](src/main/resources/lang_et.properties) | 0%
**Finnish** | [lang_fi.properties](src/main/resources/lang_fi.properties) | 0%
**French** | [lang_fr.properties](src/main/resources/lang_fr.properties) | :heavy_check_mark: Thanks Charles Maheu!
**German** | [lang_de.properties](src/main/resources/lang_de.properties) | :heavy_check_mark: Thanks Patrick Gäckle!
**Greek** | [lang_el.properties](src/main/resources/lang_el.properties) | 0%
**Hebrew** | [lang_iw.properties](src/main/resources/lang_iw.properties) | 0%
**Hindi** | [lang_hi.properties](src/main/resources/lang_hi.properties) | :heavy_check_mark: Thanks Rakesh Gopathi!
**Hungarian** | [lang_hu.properties](src/main/resources/lang_hu.properties) | 0%
**Icelandic** | [lang_is.properties](src/main/resources/lang_is.properties) | 0%
**Indonesian** | [lang_in.properties](src/main/resources/lang_in.properties) | 0%
**Irish** | [lang_ga.properties](src/main/resources/lang_ga.properties) | 0%
**Italian** | [lang_it.properties](src/main/resources/lang_it.properties) | 0%
**Japanese** | [lang_ja.properties](src/main/resources/lang_ja.properties) | 0%
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
