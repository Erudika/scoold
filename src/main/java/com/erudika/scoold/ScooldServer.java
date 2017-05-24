/*
 * Copyright 2013-2017 Erudika. https://erudika.com
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
	}

	public static final String LOCALE_COOKIE = Config.APP_NAME_NS + "-locale";
	public static final String CSRF_COOKIE = Config.APP_NAME_NS + "-csrf";
	public static final String TOKEN_PREFIX = "ST_";
	public static final String HOMEPAGE = "/";
	public static final String HOST_URL = Config.getConfigParam("host_url", HOMEPAGE);
	public static final String CDN_URL = Config.getConfigParam("cdn_url", Config.IN_PRODUCTION ? HOST_URL : HOMEPAGE);
	public static final String AUTH_USER_ATTRIBUTE = TOKEN_PREFIX + "AUTH_USER";
	public static final String IMAGESLINK = (Config.IN_PRODUCTION ? CDN_URL + "/" : HOMEPAGE) + "images";
	public static final String SCRIPTSLINK = (Config.IN_PRODUCTION ? CDN_URL + "/" : HOMEPAGE) + "scripts";
	public static final String STYLESLINK = (Config.IN_PRODUCTION ? CDN_URL + "/" : HOMEPAGE) + "styles";

	public static final int MAX_CONTACTS_PER_USER = Config.getConfigInt("max_contacts_per_user", 2000);
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

	public static final String peoplelink = HOMEPAGE + "people";
	public static final String profilelink = HOMEPAGE + "profile";
	public static final String searchlink = HOMEPAGE + "search";
	public static final String signinlink = HOMEPAGE + "signin";
	public static final String signoutlink = HOMEPAGE + "signout";
	public static final String aboutlink = HOMEPAGE + "about";
	public static final String privacylink = HOMEPAGE + "privacy";
	public static final String termslink = HOMEPAGE + "terms";
	public static final String tagslink = HOMEPAGE + "tags";
	public static final String settingslink = HOMEPAGE + "settings";
	public static final String translatelink = HOMEPAGE + "translate";
	public static final String reportslink = HOMEPAGE + "reports";
	public static final String adminlink = HOMEPAGE + "admin";
	public static final String votedownlink = HOMEPAGE + "votedown";
	public static final String voteuplink = HOMEPAGE + "voteup";
	public static final String questionlink = HOMEPAGE + "question";
	public static final String questionslink = HOMEPAGE + "questions";
	public static final String commentlink = HOMEPAGE + "comment";
	public static final String postlink = HOMEPAGE + "post";
	public static final String revisionslink = HOMEPAGE + "revisions";
	public static final String feedbacklink = HOMEPAGE + "feedback";
	public static final String languageslink = HOMEPAGE + "languages";

	private static final Logger logger = LoggerFactory.getLogger(ScooldServer.class);

	public static void main(String[] args) {
		((ch.qos.logback.classic.Logger) logger).setLevel(ch.qos.logback.classic.Level.TRACE);
		SpringApplication app = new SpringApplication(new Object[]{ScooldServer.class});
		System.setProperty("spring.velocity.cache", Boolean.toString(Config.IN_PRODUCTION));
		System.setProperty("spring.velocity.prefer-file-system-access", Boolean.toString(!Config.IN_PRODUCTION));
		System.setProperty("spring.velocity.properties.velocimacro.library.autoreload", Boolean.toString(!Config.IN_PRODUCTION));
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
		settings.put("signin_success", getServerURL() + "/signin/success?jwt=?");
		settings.put("signin_failure", getServerURL() + "/signin?code=3&error=true");
		try {
			pc.setAppSettings(settings);
		} catch (Exception e) {
			logger.error("No connection to Para backend.");
		}
		return pc;
	}

	@Bean
	public Emailer scooldEmailerBean() {
		// Dummy emailer. You can implement your own emailer and instantiate it here
		return new ScooldEmailer();
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
				epr.addErrorPages(new ErrorPage(HttpStatus.UNAUTHORIZED, "/error/403"));
				epr.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error/500"));
				epr.addErrorPages(new ErrorPage(HttpStatus.SERVICE_UNAVAILABLE, "/error/503"));
				epr.addErrorPages(new ErrorPage(HttpStatus.BAD_REQUEST, "/error/400"));
				epr.addErrorPages(new ErrorPage(HttpStatus.METHOD_NOT_ALLOWED, "/error/405"));
				epr.addErrorPages(new ErrorPage(Exception.class, "/error/500"));
			}
		};
	}
}
