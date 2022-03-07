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

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.App;
import com.erudika.para.core.email.Emailer;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.utils.ScooldEmailer;
import com.erudika.scoold.utils.ScooldRequestInterceptor;
import com.erudika.scoold.utils.ScooldUtils;
import com.erudika.scoold.velocity.VelocityConfigurer;
import com.erudika.scoold.velocity.VelocityViewResolver;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import javax.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.runtime.RuntimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@SpringBootApplication
public class ScooldServer extends SpringBootServletInitializer {

	static {
		System.setProperty("scoold.scoold", "-"); // prevents empty config
	}

	private static final ScooldConfig CONF = ScooldUtils.getConfig();

	static {
		// tells ParaClient where to look for classes that implement ParaObject
		System.setProperty("para.core_package_name", "com.erudika.scoold.core");
		System.setProperty("server.port", String.valueOf(CONF.serverPort()));
		System.setProperty("server.servlet.context-path", CONF.serverContextPath());
		System.setProperty("server.use-forward-headers", String.valueOf(CONF.inProduction()));
	}

	public static final String TOKEN_PREFIX = "ST_";
	public static final String HOMEPAGE = "/";
	public static final String AUTH_USER_ATTRIBUTE = TOKEN_PREFIX + "AUTH_USER";
	public static final String REST_ENTITY_ATTRIBUTE = "REST_ENTITY";
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
	public static final String APIDOCSLINK = HOMEPAGE + "apidocs";

	private static final Logger logger = LoggerFactory.getLogger(ScooldServer.class);

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(ScooldServer.class);
		initConfig();
		app.setAdditionalProfiles(CONF.environment());
		app.setWebApplicationType(WebApplicationType.SERVLET);
		app.run(args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder app) {
		initConfig();
		app.profiles(CONF.environment());
		app.web(WebApplicationType.SERVLET);
		return app.sources(ScooldServer.class);
	}

	private static void initConfig() {
		System.setProperty("server.servlet.session.timeout", String.valueOf(CONF.sessionTimeoutSec()));
		// JavaMail configuration properties
		System.setProperty("spring.mail.host", CONF.mailHost());
		System.setProperty("spring.mail.port", String.valueOf(CONF.mailPort()));
		System.setProperty("spring.mail.username", CONF.mailUsername());
		System.setProperty("spring.mail.password", CONF.mailPassword());
		System.setProperty("spring.mail.properties.mail.smtp.starttls.enable", Boolean.toString(CONF.mailTLSEnabled()));
		System.setProperty("spring.mail.properties.mail.smtp.ssl.enable", Boolean.toString(CONF.mailSSLEnabled()));
		System.setProperty("spring.mail.properties.mail.debug", Boolean.toString(CONF.mailDebugEnabled()));
	}

	@Bean
	public WebMvcConfigurer baseConfigurerBean(@Named final ScooldRequestInterceptor sri) {
		return new WebMvcConfigurer() {
			public void addInterceptors(InterceptorRegistry registry) {
				registry.addInterceptor(sri);
			}
		};
	}

	@Bean
	public ParaClient paraClientBean() {
		tryAutoInitParaApp();
		logger.info("Scoold server is listening on {}", CONF.serverUrl());
		String accessKey = CONF.paraAccessKey();
		ParaClient pc = new ParaClient(accessKey, CONF.paraSecretKey());
		pc.setEndpoint(CONF.paraEndpoint());
		pc.setChunkSize(CONF.batchRequestSize()); // unlimited batch size

		printRootAppConnectionNotice();
		printGoogleMigrationNotice();
		printFacebookMigrationNotice();
		printParaConfigChangeNotice();

		ScooldUtils.tryConnectToPara(() -> {
			pc.throwExceptionOnHTTPError(true);
			// update the Scoold App settings through the Para App settings API.
			pc.setAppSettings(CONF.getParaAppSettings());
			pc.throwExceptionOnHTTPError(false);
			boolean connected = pc.getTimestamp() > 0; // finally, check if app actually exists
			if (connected) {
				logger.info("Connected to Para on {} with credentials for '{}'.", pc.getEndpoint(), accessKey);
			}
			return connected;
		});
		return pc;
	}

	@Bean
	public Emailer scooldEmailerBean(JavaMailSender mailSender) {
		return new ScooldEmailer(mailSender);
	}

	/**
	 * @return Velocity config bean
	 */
	@Bean
	public VelocityConfigurer velocityConfigBean() {
		Properties velocityProperties = new Properties();
		velocityProperties.put(RuntimeConstants.VM_LIBRARY, "macro.vm");
		velocityProperties.put(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, CONF.inProduction());
		velocityProperties.put(RuntimeConstants.VM_LIBRARY_AUTORELOAD, !CONF.inProduction());
		velocityProperties.put(RuntimeConstants.VM_PERM_ALLOW_INLINE_REPLACE_GLOBAL, true);
		velocityProperties.put(RuntimeConstants.EVENTHANDLER_REFERENCEINSERTION,
				"org.apache.velocity.app.event.implement.EscapeHtmlReference");
		velocityProperties.put("eventhandler.escape.html.match", "^((?!_).)+$");

		VelocityConfigurer vc = new VelocityConfigurer();
		vc.setVelocityProperties(velocityProperties);
		vc.setResourceLoaderPath("classpath:templates/");
		vc.setPreferFileSystemAccess(!CONF.inProduction()); // use SpringResourceLoader only in production
		return vc;
	}

	@Bean
	public ViewResolver viewResolver() {
		VelocityViewResolver viewr = new VelocityViewResolver();
		viewr.setRedirectHttp10Compatible(false);
		viewr.setSuffix(".vm");
		viewr.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return viewr;
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

	private void printGoogleMigrationNotice() {
		if (CONF.hasValue("google_client_id") && StringUtils.isBlank(CONF.googleAppId())) {
			logger.warn("Please migrate to the standard OAuth2 method for authenticating with Google. "
					+ "Change '{}.google_client_id' to '{}.gp_app_id' and also add the secret key for your OAuth2 "
					+ "app as '{}.gp_secret' in your configuration. https://console.cloud.google.com/apis/credentials",
					CONF.getConfigRootPrefix(), CONF.getConfigRootPrefix(), CONF.getConfigRootPrefix());
		}
	}

	private void printFacebookMigrationNotice() {
		if (!StringUtils.isBlank(CONF.facebookAppId()) && StringUtils.isBlank(CONF.facebookSecret())) {
			logger.warn("Please migrate to the standard OAuth2 method for authenticating with Facebook. "
					+ "Secret key is missing - add the secret key for your OAuth2 "
					+ "app as '{}.fb_secret' in your configuration. "
					+ "https://developers.facebook.com/apps/896508060362903/settings/basic/",
					CONF.getConfigRootPrefix(), CONF.facebookAppId());
		}
	}

	private void printRootAppConnectionNotice() {
		if (App.id(Config.PARA).equalsIgnoreCase(App.id(CONF.paraAccessKey()))) {
			logger.warn("You are connected to the root Para app - this is not recommended and can be problematic. "
					+ "Please create a separate Para app for Scoold to connect to.");
		}
	}

	private void printParaConfigChangeNotice() {
		String paraPrefix = Para.getConfig().getConfigRootPrefix();
		String scooldPrefix = CONF.getConfigRootPrefix();
		com.typesafe.config.Config paraPath = Para.getConfig().getConfig().atPath(paraPrefix);
		com.typesafe.config.Config scooldPath = CONF.getConfig().atPath(scooldPrefix);
		for (String confKey : Arrays.asList("access_key", "secret_key", "endpoint")) {
			if (paraPath.hasPath(paraPrefix + "." + confKey)) {
				logger.warn("Found deprecated configuration property '{}.{}' - please rename all Scoold "
						+ "configuration properties to start with prefix '{}', e.g. '{}.{}'.",
						paraPrefix, confKey, scooldPrefix, scooldPrefix, confKey);
			} else if (scooldPath.hasPath(scooldPrefix + "." + confKey)) {
				logger.warn("Found deprecated configuration property '{}.{}' - "
						+ "please rename it to '{}.para_{}'.", scooldPrefix, confKey, scooldPrefix, confKey);
			}
		}
	}

	private void tryAutoInitParaApp() {
		String rootSecret = null;
		String confFile = System.getProperty("config.file", "application.conf");
		if (!CONF.autoInitWithRootAppSecretKey().isBlank()) {
			rootSecret = CONF.autoInitWithRootAppSecretKey().trim();
		} else if (!CONF.autoInitWithParaConfigFile().isBlank()) {
			com.typesafe.config.Config paraConfig = ConfigFactory.parseFile(new File(CONF.autoInitWithParaConfigFile()));
			if (paraConfig.hasPath("para.root_secret_key")) {
				rootSecret = paraConfig.getString("para.root_secret_key");
			}
		}
		if (rootSecret != null) {
			ParaClient pcRoot = new ParaClient(App.id(Config.PARA), rootSecret);
			pcRoot.setEndpoint(CONF.paraEndpoint());
			String childApp = CONF.paraAccessKey();
			boolean connectionOk = pcRoot.getTimestamp() > 0;
			if (connectionOk && (pcRoot.getCount("app") == 1 || pcRoot.read(childApp) == null)) {
				Map<String, Object> credentials = pcRoot.invokeGet("_setup/" +
						Utils.urlEncode(App.identifier(childApp)), null, Map.class);
				if (credentials.containsKey("accessKey") && credentials.containsKey("secretKey")) {
					String acceessKey = (String) credentials.get("accessKey");
					System.setProperty("scoold.para_access_key", acceessKey);
					System.setProperty("scoold.para_secret_key", (String) credentials.get("secretKey"));
					logger.info("Auto-init succeeded - created new app '{}' and saved keys to {}.", acceessKey, confFile);
					CONF.store();
				}
			} else if (!connectionOk) {
				logger.error("Failed to auto-initialize {} - try updating your app's credentials manually.", childApp);
			}
		}
		if (!Files.exists(Paths.get(confFile).toAbsolutePath())) {
			System.setProperty("scoold.app_name", CONF.appName());
			logger.info("No configuration file found - configuration saved in {}.", confFile);
			CONF.store();
		}
	}
}
