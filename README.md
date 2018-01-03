![Scoold Q&A](assets/header.png)

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
- I18n and branding customization
- Spring Boot project (single JAR)
- Reputation and voting system with badges
- Minimal frontend JS code based on jQuery
- Modern, responsive layout powered by Materialize CSS
- Suggestions for similar questions and hints for duplicate posts
- Email notifications for post replies and comments
- LDAP authentication support
- Syntax highlighting for code in posts
- Emoji support - [cheat sheet](https://www.webpagefx.com/tools/emoji-cheat-sheet/)
- Social login (Facebook, Google, GitHub, LinkedIn, Microsoft, Twitter) with Gravatar support
- SEO friendly

### Live Demo

*Scoold is deployed on a free dyno and it might take a minute to wake up.*
### [Live demo on Heroku](https://live.scoold.com)

### Quick Start

**Note: The Para backend server is deployed separately and is required for Scoold to run.**

0. You first *need* to create a developer app with [Facebook](https://developers.facebook.com),
[Google](https://console.developers.google.com) or any other identity provider that you wish to use.
This isn't necessary only if you enable LDAP or password authentication.

1. Create a new app on [ParaIO.com](https://paraio.com) and save the access keys
2. Click here => [![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy?template=https://github.com/Erudika/scoold)

**OR**

1. Create a new app on [ParaIO.com](https://paraio.com) and save the access keys OR [run Para locally on port 8080](https://paraio.org/docs/#001-intro)
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
# the URL where Scoold is hosted, e.g. https://scoold.yourhost.com
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
para.admin_ident = "admin@domain.com"
# GA code
para.google_analytics_id = "UA-123456-7"
# enables syntax highlighting in posts
para.code_highlighting_enabled = true
# Facebook - create your own Facebook app first!
para.fb_app_id = "123456789"
# Google - create your own Google app first!
para.google_client_id = "123-abcd.apps.googleusercontent.com"
```

**Note**: On Heroku, the config variables above **must** be set without dots ".", for example `para.endpoint` becomes `para_endpoint`.
These are set through the Heroku admin panel, under "Settings", "Reveal Config Vars".

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

For authenticating with Facebook or Google, you only need your Gogle client id
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

## Creating admins

You can configure with `para.admin_ident = admin@example.com` and then register with that same email.
You'll be given admin status after you verify your email. Alternatively, you can programmatically create an
administrator account like this:

```java
ParaClient pc = new ParaClient("app:my-scoold-app", "secret");
// create the user first
pc.signIn("password", "test@gmail.com:test7:test123");
// read identifier first to get the user id
ParaObject identifier = pc.read("test@gmail.com");
User user = pc.read(identifier.getCreatorid());
user.setActive(true); // optional, manual user activation
user.setGroups("admins"); // use 'users' to revert back
User updated = pc.create(user); // overwrite the user above
```
Copy the code above and execute it as a simple Java program.
To overwrite an existing user skip the first line of code `pc.signIn()`.

## Customizing the UI

Clone this repository and edit the following:

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

You can translate Scoold to your language at https://live.scoold.com/languages
Also you can take the [language file](https://github.com/Erudika/scoold/blob/master/src/main/resources/lang_en.properties)
and translate it, then change the file suffix from "_en" to your locale, and finally, open a pull request here.

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


![Square Face](assets/logosq.png)

## License
[Apache 2.0](LICENSE)
