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
- SEO friendly
- Cookie consent (for GDPR, CCPA, etc.)

### [Buy Scoold Pro](https://paraio.com/scoold-pro) and also get these premium features:

- [Slack integration](https://scoold.com/slack.html)
- [Mattermost integration](https://scoold.com/mattermost.html)
- [Microsoft Teams integration](https://scoold.com/teams.html)
- SAML authentication support
- Custom authentication support
- Mentions with notifications
- File uploads (local, S3 or Imgur)
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

*Sometimes the demos might take a minute to load.*

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

[JDK 1.8 or higher](https://openjdk.java.net/) is required to build and run the project. All major operating systems are supported.

1. Create a new app on [ParaIO.com](https://paraio.com) and copy your access keys to a file
2. Create Scoold's configuration file named `application.conf` and add the following properties to it:
```ini
para.env = "development"
para.app_name = "Scoold"
para.access_key = "app:scoold"
para.secret_key = "scoold_secret_key_from_para"
# change to http://localhost:8080 if Para is running locally
para.endpoint = "https://paraio.com"
# add your email here
para.admins = "my@email.com"
# (optional) require login to view content
para.is_default_space_public = false
```
3. Start Scoold with `java -jar -Dconfig.file=./application.conf scoold-*.jar`
4. Open `http://localhost:8000/signin/register` in your browser
5. Register a new account with your email address (same as above - my@email.com)

If you want to login with a social account, first you *need* to create a developer app with
[Facebook](https://developers.facebook.com),
[Google](https://console.developers.google.com) or **any other identity provider** that you wish to use.
This isn't necessary if you're planning to login with LDAP, SAML or with email and password.
Save the obtained API keys in `application.conf`, as shown below.

> **Important:** Authorized redirect URLs for Facebook should look like this: `https://{your_scoold_host}`,
`https://{your_scoold_host}/signin`. For all the other identity providers you must whitelist the Para host with the
appropriate authentication endpoint. For example, for GitHub, the redirect URL would be: `https://paraio.com/github_auth`,
for OAuth 2 - `https://paraio.com/oauth2_auth` and [so on](http://paraio.org/docs/#029-passwordless).

### Quick Start with a self-hosted Para backend (harder)

**Note: The Para backend server is deployed separately and is required for Scoold to run.**

1. [run Para locally on port 8080](https://paraio.org/docs/#001-intro) and initialize it with `GET localhost:8080/v1/_setup`
2. Save the access keys for the root Para app somewhere safe, you'll need them to configure Para CLI tool below
3. Create a new directory for Scoold containing a file called `application.conf` and paste in the example configuration below.
4. Create a **new Para app** called `scoold` using [Para CLI](https://github.com/Erudika/para-cli) tool:
```sh
# You will need to have Node.js and NPM installed beforehand.
$ npm install -g para-cli
# run setup and enter the keys for the root app and endpoint 'http://localhost:8080'
$ para-cli setup
$ para-cli ping
$ para-cli new-app "scoold" --name "Scoold"
```
5. Save the keys inside Scoold's `application.conf`. The contents of this file should look like this:
```ini
para.env = "development"
para.app_name = "Scoold"
para.access_key = "app:scoold"
para.secret_key = "scoold_secret_key"
para.endpoint = "http://localhost:8080"
para.admins = "my@email.com"
```
6. Start Scoold with `java -jar -Dconfig.file=./application.conf scoold-*.jar` and keep an eye on the log for any error messages
7. Open `http://localhost:8000` in your browser and register an account with the same email you put in the configuration


> **Important: Do not use the same `application.conf` file for both Para and Scoold!**
Keep the two applications in separate directories, each with its own configuration file.
All settings shown below are meant to be kept in the Scoold config file.
If you want to sign up with an email and password, SMTP settings must be configured before deploying Scoold to production.

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

The most important settings are `para.endpoint` - the URL of the Para server, as well as,
`para.access_key` and `para.secret_key`. Connection to a Para server *is required* for Scoold to run.

Copy the Scoold example configuration below to your **`application.conf`** and edit it if necessary:
```ini
### Minimal configuration ###
# the name of the application
para.app_name = "Scoold"
# the port for Scoold
para.port = 8000
# change this to "production" later
para.env = "development"
# the public-facing URL where Scoold is hosted
para.host_url = "http://localhost:8000"
# the URL of Para - can also be "https://paraio.com"
para.endpoint = "http://localhost:8080"
# access key for your Para app
para.access_key = "app:scoold"
# secret key for your Para app
para.secret_key = ""
# the email or identifier of the admin user - check Para user object
para.admins = "admin@domain.com"
##############################

####### Authentication #######
# enable or disable email and password authentication
para.password_auth_enabled = true
# min. password length
para.min_password_length = 8
# min. password strength (1=Good, 2=Strong, 3=Very Strong)
para.min_password_strength = 2
# Session cookie name
para.auth_cookie = "scoold-auth"
# Facebook - create your own Facebook app first!
para.fb_app_id = "123456789"
# Google - create your own Google app first!
para.gp_app_id = "123-abcd.apps.googleusercontent.com"
para.gp_secret = ""
###############################

### Misc. ###
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
# enables syntax highlighting in posts
para.code_highlighting_enabled = true
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
# Post body limit (characters)
para.max_post_length = 20000
# Tags per post limit, must be < 100
para.max_tags_per_post = 5
# Sets the default tag for new questions
para.default_question_tag = "question"
# Enable/disable numeric pagination (< 1 2 3...N >)
para.numeric_pagination_enabled = false
# Selects the default language to load on startup, defaults to 'en'
para.default_language_code = ""
# Enable/disable basic HTML tags in the text editor
para.html_in_markdown_enabled = false
```

On startup, Scoold will try to connect to Para 10 times, with a 10 second interval between retries. After that it will
fail and the settings will not be persisted. If you set the maximum number of retries to `-1` there will be an infinite
number of attempts to connect to Para. These parameters are controlled by:

```
para.connection_retries_max = 10
para.connection_retry_interval_sec = 10
```

## Docker

Tagged Docker images for Scoold are located at `erudikaltd/scoold` on Docker Hub. **It's highly recommended that you
pull only release images like `:1.42.0` or `:latest_stable` because the `:latest` tag can be broken or unstable.**
The `:latest_stable` tag always points to the latest release version.
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

`JAVA_OPTS` - Java system properties, e.g. `-Dpara.port=8000`
`BOOT_SLEEP` - Startup delay, in seconds

**Docker Compose**

You can start the whole stack, Para + Scoold, with a single command using `docker-compose`.
First, create a new directory and copy [`docker-compose.yml`](docker-compose.yml) (for **Scoold Pro** the
[`docker-compose.yml` is here](https://raw.githubusercontent.com/Erudika/scoold-pro/master/docker-compose.yml))
to it from this repository. Also create these files in the same directory:

1. `para-application.conf` - containing the Para configuration
2. `scoold-application.conf` - containing the Scoold configuration

Example for `para-application.conf`:
```ini
para.env = "production"
para.dao = "H2DAO"
```

Example for `scoold-application.conf`:
```ini
para.env = "production"
para.app_name = "Scoold"
para.endpoint = "http://para:8080"
para.access_key = "app:scoold"
para.secret_key = "..."
```
Docker Compose automatically creates DNS names for each of the services.
This is why the exemplary `scoold-application.conf` contains
`http://para:8080` as the value for `para.endpoint`. The internal IP
of Para will be resolved by Docker automatically.

Then you can start both Scoold and Para with Docker Compose like so:
```
$ docker-compose up
```
**Follow the [quick start guide](#quick-start-with-a-self-hosted-para-backend-harder) above** to initialize Para and
create a new app for Scoold. Once you have the access keys for that app, update `scoold-application.conf`
with those and restart the Para + Scoold Docker stack:

1. Stop the containers using <kbd>Ctrl</kbd> + <kbd>C</kbd>
2. Rerun `docker-compose up`

The same pair of containers will be run, but this time Scoold has the proper
configuration, allowing it to communicate with Para successfully.

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
above but **replace all dots in the variable names with underscores**, e.g. `para.endpoint` -> `para_endpoint`.
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
Click "Reveal Config Vars". Configuration variables (config vars) **must not** contain dots ".", for example `para.endpoint`
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
para.oa2_app_id = "cognito_app_client_id"
para.oa2_secret = "cognito_app_client_secret"
para.security.oauth.authz_url = "https://scoold.auth.eu-west-1.amazoncognito.com/login"
para.security.oauth.token_url = "https://scoold.auth.eu-west-1.amazoncognito.com/oauth2/token"
para.security.oauth.profile_url = "https://scoold.auth.eu-west-1.amazoncognito.com/oauth2/userInfo"
para.security.oauth.provider = "Continue with Cognito"
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

To deploy Scoold at a different path instead of the root path, set `para.context_path = "/newpath`. The default value
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

This header is enabled by default for enhanced security. It can be disabled with `para.csp_header_enabled = false`.
The default value is modified through `para.csp_header = "new_value"`. The default CSP header is:
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

**Note:** If you get CSP violation errors, check your `para.host_url` and `para.cdn_url` configuration,
or edit the value of `para.csp_header`.

Additionally, there are 4 options to extend the values of `connect-src`, `frame-src`, `font-src` and `style-src`
respectively:
```ini
para.csp_connect_sources = "connect-domain1.com connect-domain2.com"
para.csp_frame_sources = "frame-domain1.com frame-domain2.com"
para.csp_font_sources = "font-domain1.com font-domain2.com"
para.csp_style_sources = "style-domain1.com style-domain2.com"
```

You can also enable or disable CSP violation reports (visible only to admins) by setting `para.csp_reports_enabled = true`.
Keep in mind that if your website has a lot of traffic, this will result in hundreds of new reports being created each hour.

## External scripts and JS snippets

You can append external scripts and JS snippets to the end of the page by setting the `para.external_scripts` property.
Scripts are loaded in alphabetical order based on their key.
```ini
# URL
para.external_scripts.myscript1 = "https://mydomain.com/script.js"
# Base64 encoded long JavaScript snippet
para.external_scripts.myscript2 = "J2Y2M3VlcH .... enZ2OScpOw=="
# Short raw JS snippet
para.external_scripts.myscript3 = "var x = 5; console.log('x is', x);"
```

**Important:** Watch out for console errors in the browser after you add external scripts. In such cases you might have to
modify the `frame-src` or `connect-src` portions of the CSP header (see the 4 options above).

If 3rd party cookie consent is enabled (for GDPR, CCPA), all external scripts will be disabled until the user gives their
consent. You can bypass that by prefixing its key with "bypassconsent", e.g. `para.external_scripts.bypassconsent_myscript2`.

Additionally, you can put scripts in the `<head>` element by prefixing their name with "head", for example:
`para.external_scripts.head_script`.

## External CSS stylesheets

You can inline short snippets of CSS using `para.inline_css`. Keep in mind that any inlined CSS rules **will
override** any of the previously declared stylesheets, including the main stylesheet rules.

```ini
para.inline_css = "body { color: #abcdef; }"
```
Another option is to add external stylesheets to the website:
```ini
para.external_styles = "https://mydomain.com/style1.css, https://mydomain.com/style2.css"
```
The last option is to completely replace the main stylesheet with a custom one. It's a good idea to copy the default
CSS rules from [`/styles/style.css`](https://live.scoold.com/styles/style.css) and modify those, then upload the new
custom stylesheet file to a public location and set:

```ini
para.stylesheet_url = "https://public.cdn.com/custom.css"
```

The order in which CSS rules are loaded is this (each overrides the previous ones):
1. main stylesheet, 2. external stylesheets, 3. inline CSS or custom theme

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

## Welcome email customization

To customize the message sent when a new user signs up with Scoold, modify these properties in your Scoold configuration
file:
```ini
para.emails.welcome_text1 = "You are now part of {0} - a friendly Q&A community..."
para.emails.welcome_text2 = "To get started, simply navigate to the "Ask question" page and ask a question..."
para.emails.welcome_text3 = "Best, <br>The {0} team<br><br>"
```

## Social login

For authenticating with Facebook or Google, you only need your Google client id
(e.g. `123-abcd.apps.googleusercontent.com`), or Facebook app id (only digits).
For all the other providers, GitHub, LinkedIn, Twitter, Slack, Amazon and Microsoft, you need to set both the app id
and secret key.
**Note:** if the credentials are blank, the sign in button is hidden for that provider.
```ini
# Facebook
para.fb_app_id = ""
# Google
para.gp_app_id = ""
para.gp_secret = ""
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
para.ms_tenant_id = ""
# Slack
para.sl_app_id = ""
para.sl_secret = ""
# Amazon
para.az_app_id = ""
para.az_secret = ""
```
You also need to set your host URL when running Scoold in production:
```ini
para.host_url = "https://your.scoold.url"
```
This is required for authentication requests to be redirected back to the origin.

**Important:** You must to whitelist the [Para endpoints](https://paraio.org/docs/#031-github) in the admin consoles of
each authentication provider. For example, for GitHub you need to whitelist `https://parahost.com/github_auth` as a
callback URL (redirect URL). Same thing applies for the other providers, **except Facebook**.
For these two providers you need to whitelist these two URLs, containing the public address of Scoold:
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
https:// or http://localhost". To make it work you can set `para.security.redirect_uri = "https://public-para.host"`
while still keeping `para.endpoint = "http://local-ip:8080"`.

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
para.security.oauth.parameters.given_name = "given_name"
para.security.oauth.parameters.family_name = "family_name"

# Sets the string on the login button
para.security.oauth.provider = "Continue with OpenID Connect"

# Enable/disable access token delegation
para.security.oauth.token_delegation_enabled = false
```

Make sure you **whitelist** your Para authentication endpoint `https://para_url/oauth2_auth` as a trusted redirect URL.

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
para.security.oauthsecond.parameters.given_name = "given_name"
para.security.oauthsecond.parameters.family_name = "family_name"

# Sets the string on the login button (second provider)
para.security.oauthsecond.provider = "Continue with Second OAuth 2.0 provider"

# Enable/disable access token delegation (second provider)
para.security.oauthsecond.token_delegation_enabled = false
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
para.oa2_app_id = "0oa123...."
para.oa2_secret = "secret"
para.security.oauth.authz_url = "https://${yourOktaDomain}/oauth2/v1/authorize"
para.security.oauth.token_url = "https://${yourOktaDomain}/oauth2/v1/token"
para.security.oauth.profile_url = "https://${yourOktaDomain}/oauth2/v1/userinfo"
para.security.oauth.scope = "openid email profile"
para.security.oauth.provider = "Continue with Okta"
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
8. Copy the *Application (client) ID* that you should be seeing now at the top - it is the value for `para.oa2_app_id`
   setting in your configuration.
9. Navigate to *Certificates and Secrets* in the sidebar on the left.
10. Create a new secret by clicking on *New client secret*.
11. Copy the generated secret (you will not be able to see that secret anymore on Azure Portal) - it is the value
    for `para.oa2_secret` setting in your configuration.
12. Fill in the configuration of Scoold:

```ini
para.oa2_app_id = "e538..."
para.oa2_secret = "secret"
para.security.oauth.authz_url = "https://login.microsoftonline.com/${yourAADTenantId}/oauth2/v2.0/authorize"
para.security.oauth.token_url = "https://login.microsoftonline.com/${yourAADTenantId}/oauth2/v2.0/token"
para.security.oauth.profile_url = "https://graph.microsoft.com/oidc/userinfo"
para.security.oauth.scope = "openid email profile"
para.security.oauth.provider = "Continue with AAD"
```

Make sure to replace `${yourAADTenantId}` with your actual AAD tenant ID.

13. Restart Scoold and login with an AAD user account

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

# extra options - change only if necessary
para.security.ldap.user_search_base = ""
para.security.ldap.user_search_filter = "(cn={0})"
para.security.ldap.password_attribute = "userPassword"
para.security.ldap.username_as_name = false

# Sets the string on the login button (PRO)
para.security.ldap.provider = "Continue with LDAP"

# automatic groups mapping
para.security.ldap.mods_group_node = ""
para.security.ldap.admins_group_node = ""
```

The search filter syntax allows you to use the placeholder `{0}` which gets replaced with the person's username.

You can also map LDAP DN nodes to Para user groups. For example, with the following configuration:
```
para.security.ldap.mods_group_node = "ou=Moderators"
para.security.ldap.admins_group_node = "cn=Admins"
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
para.security.ldap.user_search_filter = "(&(objectClass=user)(sAMAccountName={1}))"
para.security.ldap.base_dn = "ou=dev,dc=scoold,dc=com"
para.security.ldap.server_url = "ldap://192.168.123.70:389"
para.security.ldap.active_directory_domain = "scoold.com"
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
para.security.ldap.server_url = "ldap://server:389"
para.security.ldap.active_directory_domain = "domain.com"
para.security.ldap.user_search_filter = "userPrincipalName={0}"
para.security.ldap.base_dn = "ou=dev,dc=domain,dc=com"
```

### Local (internal) LDAP authentication

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
```ini
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
- `identifier` - a unique user id in the format `custom:123`
- `appid` - the app id (optional)

The JWT is signed with the value of `para.app_secret_key` and should have a short validity period (e.g. 10 min).
The JWT should also contain the claims `iat` and `exp` and, optionally, `nbf`. Supported signature algorithms for the JWT
are `HS256`, `HS384` or `HS512`.
Once you generate the JWT on your backend (step 4 above), redirect the successful login request back to Scoold:
```
GET https://scoold-host/signin/success?jwt=eyJhbGciOiJIUzI1NiI..&passwordless=true
```

The UI button initiating the authentication flow above can be customized like this:
```ini
para.security.custom.provider = "Continue with Acme Co."
# location of your company's login page
para.security.custom.login_url = ""
```

There's an [example login page](https://albogdano.github.io/scoold-login-page/) implementing this sort of authentication.

## Login and logout redirects

You can configure Scoold to redirect users straight to the identity provider when they click the "Sign in" button.
This feature is disabled by default:
```ini
para.redirect_signin_to_idp = false
```
This works only for social login identity providers (except Facebook) and SAML. It won't work for LDAP or
basic password authentication. When enabled and combined with `para.is_default_space_public = false`,
unauthenticated users will be sent directly to the IDP without seeing the "Sign in" page or any other page on Scoold.

You can also configure users to be redirected to an external location when they log out:
```ini
para.signout_url = "https://homepage.com"
```

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
```ini
# put space ids here, the "scooldspace:" prefix is optional
para.auto_assign_spaces = "my-space-one,my-other-space"
```
You can assign both the default space and a custom space to new users (values can contain spaces):
```ini
para.auto_assign_spaces = "default,My Custom Space"
```
When using the option above, new spaces are added to existing spaces for each user. You can configure auto-assigned
spaces to overwrite the existing user spaces (like the "default" space, assigned to everyone) by setting:
```ini
para.reset_spaces_on_new_assignment = true
```
So when you have that set to `false` and you have configured Scoold to assign custom spaces to new users
(e.g. "my-space-1" and "my-space-2"), those users will become members of  "my-space-1", "my-space-2" **and** the
default space. If the value is `true`, the default space gets overwritten by the custom spaces you have specified in
`para.auto_assign_spaces` and new users will only be members of "my-space-1" and "my-space-2".

This is turned on for all users authenticated with LDAP, SAML or OAuth 2.0.

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
You can add or remove webhooks in the "Administration" page. Webhooks can also be disabled and they will be
disabled automatically when the target URL doesn't respond to requests from Para.

Para will notify your target URL with a `POST` request containing the payload and a `X-Webhook-Signature` header. This
header should be verified by the receiving party by computing `Base64(HmacSHA256(payload, secret))`.

You can subscribe to custom events in Scoold using the REST API. This makes it easy to integrate Scoold with services
like Zapier because it implements the [RESTHooks](https://resthooks.org/) best practices.

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

## Anonymous profiles

People may wish to make their profile details anonymous from the Settings page. To allow this option set:
```
para.profile_anonimity_enabled = true
```

## Enabling the "Feedback" section

You can enable or disable the "Feedback" page where people can discuss topics about the website itself or submit general
feedback. This section is disabled by default and can be activated with `para.feedback_enabled = true`.

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

## File uploads

**PRO**
Files can be uploaded to the local file system, Imgur or S3. File uploads are enabled by default in Scoold Pro.
To disable file uploads altogether set `para.uploads_enabled = false`. To protect uploaded files from unauthenticated
access, set `para.uploads_require_auth = true`.

To upload a file just **drag & drop** the file onto the post editor area. A link will automatically appear
when the upload is finished. Uploads can fail either because their size is too large or because their format is not in
the white list of permitted formats (documents, images, archives, audio or video). You can extend the list of permitted
file types by configuring:
```ini
para.allowed_upload_formats = "yml,py:text/plain,json:application/json"
```
If the MIME type is not specified in the format `extension:mime_type`, the default `text/plain` is used when serving these
files.

Profile pictures (avatars) can also be changed by dragging a new image on top of the existing profile picture on a
user's `/profile` page. For best results, use a square image here.

### Local storage
Local file storage is used by default. To configure the directory on the server where files will be stored, set:
```
para.file_uploads_dir = "uploads"
```

### Imgur storage provider
To use Imgur for storing images, specify your Imgur API client id:
```
para.imgur_client_id = "x23e8t0askdj"
```
Keep in mind that *only images* can be uploaded to Imgur and other restrictions may apply.

### AWS S3 storage provider
To use S3 for file storage, specify the name of the S3 bucket where you want the files to be uploaded. AWS credentials
and region settings are optional as they can be picked up from the environment automatically.
```ini
# required
para.s3_bucket = ""
# path within the bucket (object prefix)
para.s3_path = "uploads"
# these are optional
para.s3_region = ""
para.s3_access_key = ""
para.s3_secret_key = ""
```

## Slack integration

Scoold **PRO** integrates with Slack on a number of levels. First, Scoold users can sign in with Slack. They can also
use slash commands to search and post questions. Also Scoold can notify Slack users when they are mentioned on Scoold.
Finally, Scoold allows you to map spaces to Slack workspaces or channels. By default, each Slack workspace (team) is
mapped to a single Scoold space when people sign in with Slack.

**Important:** Most of the Slack operations require a **valid Slack ID stored in Scoold** which enables the mapping of
Slack users to Scoold accounts and vice versa. Slack IDs are set automatically when a Scoold user signs in with Slack.

The integration endpoint for Slack is `/slack` - this is where Scoold will accept and process requests from Slack.
To enable the Slack integration you need to register for a Slack app first and set `para.sl_app_id` and `para.sl_secret`.

### [Getting started guide for Scoold + Slack](https://scoold.com/slack.html).

Here are the configuration properties for Slack:
```
para.slack.app_id = "SHORT_APPID"
para.slack.map_workspaces_to_spaces = true
para.slack.map_channels_to_spaces = false
para.slack.post_to_space = "workspace|scooldspace:myspace|default"

para.slack.notify_on_new_question = true
para.slack.notify_on_new_answer = true
para.slack.notify_on_new_comment = true
para.slack.dm_on_new_comment = false
para.slack.default_question_tags = "via-slack"
para.slack.auth_enabled = true
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

When `para.slack.dm_on_new_comment` is enabled, Scoold will send a direct message notification to the author of
the post on which somebody commented. By default, DMs are turned off and the notification is sent to the channel instead.

Slack authentication can be disabled with `para.slack.auth_enabled = false` and the "Continue with Slack" button
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
Console. Then set `para.mm_app_id` and `para.mm_secret`.

### [Getting started guide for Scoold + Mattermost](https://scoold.com/mattermost.html).

Here are the configuration properties for Mattermost:
```
para.mattermost.server_url = "http://localhost:8065"
para.mattermost.bot_username = "scoold"
para.mattermost.bot_icon_url = "http://localhost:8000/images/logowhite.png"
para.mattermost.map_workspaces_to_spaces = true
para.mattermost.map_channels_to_spaces = false
para.mattermost.post_to_space = "workspace|scooldspace:myspace|default"

para.mattermost.notify_on_new_question = true
para.mattermost.notify_on_new_answer = true
para.mattermost.notify_on_new_comment = true
para.mattermost.dm_on_new_comment = false
para.mattermost.default_question_tags = "via-mattermost"
```

**Note:** Mattermost does not support message actions like in Slack. This means that you can't create a question from
a any chat message. The reply dialog box can be opened from a "Reply" button under each question notification message or
via the `/scoold answer-form` command.
The dialog box for new questions is opened via the new slash command `/scoold ask-form`.

All the other slash commands and notifications work just like with Slack and are described above. The Mattermost
integration will automatically create a slash command for each channel linked to Scoold on the admin page.

When `para.mattermost.dm_on_new_comment` is enabled, Scoold will send a direct message notification to the author of
the post on which somebody commented. By default, DMs are turned off and the notification is sent to the channel instead.

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
Also set `para.ms_app_id` and `para.ms_secret` as you normally would for an OAuth2 authentication with Microsoft.

### [Getting started guide for Scoold + Teams](https://scoold.com/teams.html).

Here are the configuration properties for MS Teams:
```
para.teams.bot_id = ""
para.teams.bot_secret = ""
para.teams.map_workspaces_to_spaces = true
para.teams.map_channels_to_spaces = false
para.teams.post_to_space = "workspace|scooldspace:myspace|default"

para.teams.notify_on_new_question = true
para.teams.notify_on_new_answer = true
para.teams.notify_on_new_comment = true
para.teams.dm_on_new_comment = false
para.teams.default_question_tags = "via-teams"
```

You can type `@Scoold help` to get a list of all supported actions. All the other bot commands and notifications work
just like with Slack and Mattermost, described above. A bot registration is required for the Teams integration
and it has to be created manually from the [Teams Developer Portal](https://dev.teams.microsoft.com/bots).

When `para.teams.dm_on_new_comment` is enabled, Scoold will send a direct message notification to the author of
the post on which somebody commented. By default, DMs are turned off and the notification is sent to the channel instead.

## Notifications in Slack/Mattermost/Teams

Scoold will notify the channels where you have it installed, whenever a new question or answer is created, and also
whenever a user is mentioned. To install the application on multiple channels go to the Administration page and click
one of the "Add to Slack/Mattermost/Teams" buttons for each channel where you wish to get notifications. You can receive
notification on up to 10 channels simultaneously. Notifications for new posts will go to the channel associated with the
space in which the post was created. For example, when using Slack, if `para.slack.map_workspaces_to_spaces` is `true`,
and a question is created in space "Team1 #general", Scoold will search for webhook registrations matching that
team/channel combination and only send a notification there. Direct message webhooks will be used only if there's no
space-matching channel found.

## Approving new posts from Slack/Mattermost/Teams

This works if you have enabled `para.posts_need_approval`. When a new question or answer is created by a user with less
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
<details><summary>Run Scoold with this command which enables TLS, HTTP2 and mTLS.</summary>

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
<details><summary>Run Para with this command which enables TLS, HTTP2 and mTLS.</summary>

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
<details><summary>Run Scoold with this command which enables TLS, HTTP2 and mTLS.</summary>

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
para.host_url = "http://192.168.3.4:8000"
para.host_url = "http://192.168.3.4:8080"
```
This would work except for transactional emails where inbound links point to the wrong address. The solution is to add this
to your configuration:
```ini
para.rewrite_inbound_links_with_fqdn = "https://public-scoold-domain.com"
```

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
```ini
para.fixed_nav = false
para.show_branding = true
para.logo_url = "https://static.scoold.com/logo.svg"
para.logo_width = 90

# footer HTML - add your own links, etc., escape double quotes with \"
para.footer_html = "<a href=\"https://my.link\">My Link</a>"
# show standard footer links
para.footer_links_enabled = true
# favicon image location
para.favicon_url = "/favicon.ico"
# add your own external stylesheets
para.external_styles = "https://mydomain.com/style1.css, https://mydomain.com/style2.css"
# appends extra CSS rules to the main stylesheet
para.inline_css = ""
# edit the links in the footer of transactional emails
para.emails_footer_html = ""
# change the logo in transactional emails
para.small_logo_url = "https://scoold.com/logo.png"
# enable/disable dark mode
para.dark_mode_enabled = true

# custom navbar links
para.navbar_link1_url = ""
para.navbar_link2_url = ""
para.navbar_link1_text = "Link1"
para.navbar_link2_text = "Link2"

# custom navbar menu links (shown to logged in users)
para.navbar_menu_link1_url = ""
para.navbar_menu_link2_url = ""
para.navbar_menu_link1_text = "Menu Link1"
para.navbar_menu_link2_text = "Menu Link2"

# default email notification toggles for all users
para.favtags_emails_enabled = false
para.reply_emails_enabled = false
para.comment_emails_enabled = false
```

### Custom Logo
In Scoold Pro you can change the logo of the website just by dragging and dropping a new image of your choice.

If you wish to add just a few simple CSS rules to the `<head>` element, instead of replacing the whole stylesheet,
simply add them as inline CSS:
```ini
para.inline_css = ".scoold-logo { width: 100px; }"
```

### Custom welcome message (banner)
You can set a short welcome message for unauthenticated users which will be displayed on the top of the page and it
can also contain HTML (**use only single quotes or escape double quotes `\\\"`**):
```ini
para.welcome_message = "Hello and welcome to <a href='https://scoold.com'>Scoold</a>!"
```
You can also set a custom message for users who are already logged in:
```ini
para.welcome_message_onlogin = "<h2>Welcome back <img src=\\\"{{user.picture}}\\\" width=30> <b>{{user.name}}</b>!</h2>"
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
para.navbar_menu_link1_url = "https://homepage.com"
para.navbar_menu_link1_text = "Visit my page"
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
para.cookie_consent_required = true
para.external_styles = "https://cdn.jsdelivr.net/npm/cookieconsent@3/build/cookieconsent.min.css"
para.external_scripts.bypassconsent1 = "https://cdn.jsdelivr.net/npm/cookieconsent@3/build/cookieconsent.min.js"
para.external_scripts.bypassconsent2 = "d2luZG93LmNvb2tpZWNvbnNlbnQuaW5pdGlhbGlzZSh7CiAgInBhbGV0dGUiOiB7CiAgICAicG9wdXAiOiB7CiAgICAgICJiYWNrZ3JvdW5kIjogIiM0NDQ0NDQiCiAgICB9LAogICAgImJ1dHRvbiI6IHsKICAgICAgImJhY2tncm91bmQiOiAiIzc3Nzc3NyIKICAgIH0KICB9LAogICJ0aGVtZSI6ICJjbGFzc2ljIiwKICAicG9zaXRpb24iOiAiYm90dG9tLWxlZnQiLAogICJ0eXBlIjogIm9wdC1pbiIsCiAgIm9uU3RhdHVzQ2hhbmdlIjogZnVuY3Rpb24ocyl7bG9jYXRpb24ucmVsb2FkKCk7fQp9KTs="
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
para.api_enabled = true
# A random string min. 32 chars long
para.app_secret_key = "change_to_long_random_string"
```
The API can be accessed from `/api/*` and the OpenAPI documentation and console are located at `/apidocs`.
API keys can be generated from the "Administration" page and can be made to expire after a number of hours or never
(validity period = 0). Keys are in the JWT format and signed with the secret defined in `para.app_secret_key`.
API keys can also be generated with any JWT library. The body of the key should contain the `iat`, `appid` and `exp`
claims and must be signed with the secret `para.app_secret_key`.

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

You can also change the default language of Scoold for all users by setting `para.default_language_code = "en"`, where
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
