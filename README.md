<div align="center">
  <br>
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://scoold.com/logo-dark.svg"/>
    <source media="(prefers-color-scheme: light)" srcset="https://scoold.com/logo.svg"/>
    <img width="360" alt="Scoold Logo" src="https://scoold.com/logo.svg"/>
  </picture>
  <br><br>
  <h3>
    The most effective way to share knowledge within your team or organization
  </h3>
</div>
<div align="center">

**Scoold** is a Q&A and a knowledge sharing platform for teams, inspired by Stack Overflow.
<br>
Scoold is enterprise-ready, super customizable and lightweight. 

**Scoold Pro**, the premium version of Scoold, is the perfect knowledge sharing platform 
for your company or team. The pricing is flat, without limits, the license - perpetual.
<br><br>
Are you still paying per seat in 2026? **Scoold Pro** is the knowledge sharing platform **to migrate to!**

[Compare Features](https://scoold.com/pricing/)
</div>

<div align="center">

[![Docker pulls](https://img.shields.io/docker/pulls/erudikaltd/scoold)](https://hub.docker.com/r/erudikaltd/scoold)
[![Docker pulls](https://img.shields.io/docker/image-size/erudikaltd/scoold)](https://hub.docker.com/r/erudikaltd/scoold)
[![Join the chat at https://gitter.im/Erudika/scoold](https://badges.gitter.im/Erudika/scoold.svg)](https://gitter.im/Erudika/scoold?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

</div>

<div align="center">

<a href="https://scoold.com">Website</a> &nbsp;-&nbsp; <a href="https://scoold.com/documentation/">Documentation</a> &nbsp;-&nbsp; <a href="https://demo.scoold.com">Demo</a> &nbsp;-&nbsp; <a href="https://scoold.com/documentation/reference/scoold-api/">API</a>

</div>

## Scoold - open-source knowledge sharing platform

**Scoold** is a Q&A and a knowledge sharing platform for teams. The project was created back in 2008, released in 2012 as
social network for schools inspired by Stack Overflow. In 2017 it was refactored, repackaged and open-sourced.

Scoold can be deployed anywhere - Heroku, DigitalOcean, AWS, Azure or any VPS hosting provider. It's lightweight,
the backend is handled by a separate service called [Para](https://github.com/Erudika/para). All the heavy lifting is
delegated to Para which can also be configured to store the data in any of the popular databases. This makes the Scoold
code base easy to read and can be learned quickly, even by junior developers.

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
- Custom badges - add your own text, icon and color
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
- Emojis! - using this [cheat sheet](https://www.webpagefx.com/tools/emoji-cheat-sheet/) or inline Unicode
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

## [Live DEMO](https://demo.scoold.com)

For **admin** access, go to the demo login page and click "Continue with Demo Login".

## Documentation

The full documentation for Scoold is at [scoold.com/documentation](https://scoold.com/documentation/)

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

Head over to the [documentation](https://scoold.com/documentation/intro/architecture/) for detailed explanation of the Scoold architecture.

## Scoold Pro cloud hosting


## Support

You can get support here by submitting an issue. Also you can head over to the Gitter chat room for help.
Issues related to **Scoold Pro** must be reported to [Erudika/scoold-pro](https://github.com/Erudika/scoold-pro/issues).
[Paid/priority support is also available](https://erudika.com/support/).

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
**Italian** | [lang_it.properties](src/main/resources/lang_it.properties) | :heavy_check_mark: Thanks Marco Livrieri!
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
git clone https://github.com/erudika/scoold.git && cd scoold
mvn install
```

To run a local instance of Scoold for development, use:

```sh
mvn -Dconfig.file=./scoold-application.conf spring-boot:run
```

To generate a WAR package, run:

```sh
mvn -Pwar package
```

To generate the native image for your particaluar achitecture, run:

```sh
mvn -Pnative clean package
```

GraalVM is required to be installed before compiling the native image.


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

For more information, see [CONTRIBUTING.md](https://github.com/Erudika/para/blob/master/CONTRIBUTING.md)

## License

[Apache 2.0](LICENSE)

<div align="center">

![Scoold Q&A](https://raw.githubusercontent.com/Erudika/scoold/master/assets/header.png)

Stack Overflow in a JAR

Made in the EU 🇪🇺 by Erudika
</div>
