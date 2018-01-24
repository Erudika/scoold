/*
 * Copyright 2013-2018 Erudika. https://erudika.com
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

import com.erudika.para.client.ParaClient;
import com.erudika.para.email.Emailer;
import com.erudika.para.utils.Config;
import com.erudika.scoold.utils.ScooldRequestInterceptor;
import com.erudika.scoold.utils.CsrfFilter;
import com.erudika.scoold.utils.ScooldEmailer;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Named;
import javax.servlet.DispatcherType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.boot.web.servlet.ErrorPageRegistrar;
import org.springframework.boot.web.servlet.ErrorPageRegistry;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@SpringBootApplication
public class ScooldServer {

	static {
		// tells ParaClient where to look for classes that implement ParaObject
		System.setProperty("para.core_package_name", "com.erudika.scoold.core");
		System.setProperty("server.context-path", Config.getConfigParam("context_path", System.getProperty("server.context-path", "")));
	}

	public static final String LOCALE_COOKIE = Config.getRootAppIdentifier() + "-locale";
	public static final String CSRF_COOKIE = Config.getRootAppIdentifier() + "-csrf";
	public static final String SPACE_COOKIE = Config.getRootAppIdentifier() + "-space";
	public static final String TOKEN_PREFIX = "ST_";
	public static final String HOMEPAGE = "/";
	public static final String CONTEXT_PATH = StringUtils.stripEnd(System.getProperty("server.context-path", ""), "/");
	public static final String CDN_URL = StringUtils.stripEnd(Config.getConfigParam("cdn_url", CONTEXT_PATH), "/");
	public static final String AUTH_USER_ATTRIBUTE = TOKEN_PREFIX + "AUTH_USER";
	public static final String IMAGESLINK = (Config.IN_PRODUCTION ? CDN_URL : CONTEXT_PATH) + "/images";
	public static final String SCRIPTSLINK = (Config.IN_PRODUCTION ? CDN_URL : CONTEXT_PATH) + "/scripts";
	public static final String STYLESLINK = (Config.IN_PRODUCTION ? CDN_URL : CONTEXT_PATH) + "/styles";

	public static final int MAX_COMMENTS_PER_ID = Config.getConfigInt("max_comments_per_id", 1000);
	public static final int MAX_TEXT_LENGTH = Config.getConfigInt("max_text_length", 20000);
	public static final int MAX_TAGS_PER_POST = Config.getConfigInt("max_tags_per_post", 5);
	public static final int MAX_REPLIES_PER_POST = Config.getConfigInt("max_replies_per_post", 500);
	public static final int MAX_FAV_TAGS = Config.getConfigInt("max_fav_tags", 50);

	public static final int ANSWER_VOTEUP_REWARD_AUTHOR = Config.getConfigInt("answer_voteup_reward_author", 10);
	public static final int QUESTION_VOTEUP_REWARD_AUTHOR = Config.getConfigInt("question_voteup_reward author", 5);
	public static final int VOTEUP_REWARD_AUTHOR = Config.getConfigInt("voteup_reward_author", 2);
	public static final int ANSWER_APPROVE_REWARD_AUTHOR = Config.getConfigInt("answer_approve_reward_author", 10);
	public static final int ANSWER_APPROVE_REWARD_VOTER = Config.getConfigInt("answer_approve_reward_voter", 3);
	public static final int POST_VOTEDOWN_PENALTY_AUTHOR = Config.getConfigInt("post_votedown_penalty_author", 3);
	public static final int POST_VOTEDOWN_PENALTY_VOTER = Config.getConfigInt("post_votedown_penalty_voter", 1);

	public static final int VOTER_IFHAS = Config.getConfigInt("voter_ifhas", 100);
	public static final int COMMENTATOR_IFHAS = Config.getConfigInt("commentator_ifhas", 100);
	public static final int CRITIC_IFHAS = Config.getConfigInt("critic_ifhas", 10);
	public static final int SUPPORTER_IFHAS = Config.getConfigInt("supporter_ifhas", 50);
	public static final int GOODQUESTION_IFHAS = Config.getConfigInt("goodquestion_ifhas", 20);
	public static final int GOODANSWER_IFHAS = Config.getConfigInt("goodanswer_ifhas", 10);
	public static final int ENTHUSIAST_IFHAS = Config.getConfigInt("enthusiast_ifhas", 100);
	public static final int FRESHMAN_IFHAS = Config.getConfigInt("freshman_ifhas", 300);
	public static final int SCHOLAR_IFHAS = Config.getConfigInt("scholar_ifhas", 500);
	public static final int TEACHER_IFHAS = Config.getConfigInt("teacher_ifhas", 1000);
	public static final int PROFESSOR_IFHAS = Config.getConfigInt("professor_ifhas", 5000);
	public static final int GEEK_IFHAS = Config.getConfigInt("geek_ifhas", 9000);

	public static final String PEOPLELINK = HOMEPAGE + "people";
	public static final String PROFILELINK = HOMEPAGE + "profile";
	public static final String SEARCHLINK = HOMEPAGE + "search";
	public static final String SIGNINLINK = HOMEPAGE + "signin";
	public static final String SIGNOUTLINK = HOMEPAGE + "signout";
	public static final String ABOUTLINK = HOMEPAGE + "about";
	public static final String PRIVACYLINK = HOMEPAGE + "privacy";
	public static final String TERMSLINK = HOMEPAGE + "terms";
	public static final String TAGSLINK = HOMEPAGE + "tags";
	public static final String SETTINGSLINK = HOMEPAGE + "settings";
	public static final String TRANSLATELINK = HOMEPAGE + "translate";
	public static final String REPORTSLINK = HOMEPAGE + "reports";
	public static final String ADMINLINK = HOMEPAGE + "admin";
	public static final String VOTEDOWNLINK = HOMEPAGE + "votedown";
	public static final String VOTEUPLINK = HOMEPAGE + "voteup";
	public static final String QUESTIONLINK = HOMEPAGE + "question";
	public static final String QUESTIONSLINK = HOMEPAGE + "questions";
	public static final String COMMENTLINK = HOMEPAGE + "comment";
	public static final String POSTLINK = HOMEPAGE + "post";
	public static final String REVISIONSLINK = HOMEPAGE + "revisions";
	public static final String FEEDBACKLINK = HOMEPAGE + "feedback";
	public static final String LANGUAGESLINK = HOMEPAGE + "languages";

	private static final Logger logger = LoggerFactory.getLogger(ScooldServer.class);

	public static void main(String[] args) {
		((ch.qos.logback.classic.Logger) logger).setLevel(ch.qos.logback.classic.Level.TRACE);
		SpringApplication app = new SpringApplication(new Object[]{ScooldServer.class});
		initConfig();
		app.setAdditionalProfiles(Config.ENVIRONMENT);
		app.setWebEnvironment(true);
		app.run(args);
	}

	/**
	 * @return the host URL of this Scoold server
	 */
	public static String getServerURL() {
		String defaultHost = "http://localhost:" + getServerPort();
		String host = Config.IN_PRODUCTION ? Config.getConfigParam("host_url", defaultHost) : defaultHost;
		return StringUtils.removeEnd(host, "/");
	}

	/**
	 * @return the port of this Scoold server
	 */
	public static int getServerPort() {
		return NumberUtils.toInt(System.getProperty("server.port"), Config.getConfigInt("port", 8080));
	}

	private static void initConfig() {
		System.setProperty("spring.velocity.cache", Boolean.toString(Config.IN_PRODUCTION));
		System.setProperty("spring.velocity.prefer-file-system-access", Boolean.toString(!Config.IN_PRODUCTION));
		System.setProperty("spring.velocity.properties.velocimacro.library.autoreload", Boolean.toString(!Config.IN_PRODUCTION));
		// JavaMail configuration properties
		System.setProperty("spring.mail.host", Config.getConfigParam("mail.host", ""));
		System.setProperty("spring.mail.port", String.valueOf(Config.getConfigInt("mail.port", 587)));
		System.setProperty("spring.mail.username", Config.getConfigParam("mail.username", ""));
		System.setProperty("spring.mail.password", Config.getConfigParam("mail.password", ""));
		System.setProperty("spring.mail.properties.mail.smtp.starttls.enable",
				Boolean.toString(Config.getConfigBoolean("mail.tls", true)));
		System.setProperty("spring.mail.properties.mail.smtp.ssl.enable",
				Boolean.toString(Config.getConfigBoolean("mail.ssl", false)));
	}

	@Bean
	public WebMvcConfigurerAdapter baseConfigurerBean(@Named final ScooldRequestInterceptor sri) {
		return new WebMvcConfigurerAdapter() {
			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				super.addInterceptors(registry);
				registry.addInterceptor(sri);
			}

//			@Override
//			public void addResourceHandlers(ResourceHandlerRegistry registry) {
//				registry.addResourceHandler("/images/**").addResourceLocations("/static/images/")
//					.setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic());
//				registry.addResourceHandler("/styles/**").addResourceLocations("/static/styles/")
//					.setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic());
//				registry.addResourceHandler("/scripts/**").addResourceLocations("/static/scripts/")
//					.setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic());
//			}
		};
	}

	@Bean
	public ParaClient paraClientBean() {
		String accessKey = Config.getConfigParam("access_key", "x");
		ParaClient pc = new ParaClient(accessKey, Config.getConfigParam("secret_key", "x"));
		pc.setEndpoint(Config.getConfigParam("endpoint", null));

		try {
			boolean connectedToPara = pc.getTimestamp() > 0;
			if (!connectedToPara) {
				throw new Exception();
			}
		} catch (Exception e) {
			logger.error("No connection to Para backend - make sure that your keys are valid.");
			return pc;
		}

		logger.info("Initialized ParaClient with endpoint {} and access key '{}'.", pc.getEndpoint(), accessKey);
		// update the Scoold App settings through the Para App settings API.
		Map<String, Object> settings = new HashMap<String, Object>();
		settings.put("gh_app_id", Config.GITHUB_APP_ID);
		settings.put("gh_secret", Config.GITHUB_SECRET);
		settings.put("in_app_id", Config.LINKEDIN_APP_ID);
		settings.put("in_secret", Config.LINKEDIN_SECRET);
		settings.put("tw_app_id", Config.TWITTER_APP_ID);
		settings.put("tw_secret", Config.TWITTER_SECRET);
		settings.put("ms_app_id", Config.MICROSOFT_APP_ID);
		settings.put("ms_secret", Config.MICROSOFT_SECRET);
		// LDAP settings
		settings.put("security.ldap.server_url", Config.getConfigParam("security.ldap.server_url", ""));
		settings.put("security.ldap.base_dn", Config.getConfigParam("security.ldap.base_dn", ""));
		settings.put("security.ldap.bind_dn", Config.getConfigParam("security.ldap.bind_dn", ""));
		settings.put("security.ldap.bind_pass", Config.getConfigParam("security.ldap.bind_pass", ""));
		settings.put("security.ldap.user_search_base", Config.getConfigParam("security.ldap.user_search_base", ""));
		settings.put("security.ldap.user_search_filter", Config.getConfigParam("security.ldap.user_search_filter", ""));
		settings.put("security.ldap.user_dn_pattern", Config.getConfigParam("security.ldap.user_dn_pattern", ""));
		settings.put("security.ldap.password_attribute", Config.getConfigParam("security.ldap.password_attribute", ""));
		settings.put("security.ldap.active_directory_domain", Config.getConfigParam("security.ldap.active_directory_domain", ""));
		if (!Config.getConfigParam("security.ldap.compare_passwords", "").isEmpty()) {
			settings.put("security.ldap.compare_passwords", Config.getConfigParam("security.ldap.compare_passwords", ""));
		}
		// URLs for success and failure
		settings.put("signin_success", getServerURL() + CONTEXT_PATH + SIGNINLINK + "/success?jwt=?");
		settings.put("signin_failure", getServerURL() + CONTEXT_PATH + SIGNINLINK + "?code=3&error=true");
		try {
			pc.setAppSettings(settings);
		} catch (Exception e) {
			logger.error("No connection to Para backend.");
		}
		return pc;
	}

	@Bean
	public Emailer scooldEmailerBean(JavaMailSender mailSender) {
		return new ScooldEmailer(mailSender);
	}

	/**
	 * @return Jetty config bean
	 */
	@Bean
	public EmbeddedServletContainerFactory jettyConfigBean() {
		JettyEmbeddedServletContainerFactory jef = new JettyEmbeddedServletContainerFactory();
		jef.setPort(getServerPort());
		logger.info("Listening on port {}...", jef.getPort());
		return jef;
	}

	/**
	 * @return CSRF protection filter bean
	 */
	@Bean
	public FilterRegistrationBean csrfFilterRegistrationBean() {
		String path = "/*";
		logger.debug("Initializing CSRF filter [{}]...", path);
		FilterRegistrationBean frb = new FilterRegistrationBean(new CsrfFilter());
		frb.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
		frb.setName("csrfFilter");
		frb.setAsyncSupported(true);
		frb.addUrlPatterns(path);
		frb.setMatchAfter(false);
		frb.setEnabled(true);
		frb.setOrder(2);
		return frb;
	}

	/**
	 * @return Error page registry bean
	 */
	@Bean
	public ErrorPageRegistrar errorPageRegistrar() {
		return new ErrorPageRegistrar() {
			@Override
			public void registerErrorPages(ErrorPageRegistry epr) {
				epr.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/not-found"));
				epr.addErrorPages(new ErrorPage(HttpStatus.FORBIDDEN, "/error/403"));
				epr.addErrorPages(new ErrorPage(HttpStatus.UNAUTHORIZED, "/error/401"));
				epr.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error/500"));
				epr.addErrorPages(new ErrorPage(HttpStatus.SERVICE_UNAVAILABLE, "/error/503"));
				epr.addErrorPages(new ErrorPage(HttpStatus.BAD_REQUEST, "/error/400"));
				epr.addErrorPages(new ErrorPage(HttpStatus.METHOD_NOT_ALLOWED, "/error/405"));
				epr.addErrorPages(new ErrorPage(Exception.class, "/error/500"));
			}
		};
	}
}
