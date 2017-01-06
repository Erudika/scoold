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

import com.erudika.para.utils.Config;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import org.apache.click.ClickServlet;
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
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Configuration
@SpringBootApplication
public class ScooldServer {

	private static final Logger logger = LoggerFactory.getLogger(ScooldServer.class);

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(new Object[]{ScooldServer.class});
		app.setAdditionalProfiles(Config.ENVIRONMENT);
		app.setWebEnvironment(true);
		app.run(args);
	}

	/**
	 * @return Jetty config bean
	 */
	@Bean
	public EmbeddedServletContainerFactory jettyConfigBean() {
		JettyEmbeddedServletContainerFactory jef = new JettyEmbeddedServletContainerFactory();
		int defaultPort = Config.getConfigInt("port", 8080);
		jef.setPort(NumberUtils.toInt(System.getProperty("server.port"), defaultPort));
		logger.info("Listening on port {}...", jef.getPort());
		return jef;
	}

	/**
	 * @return Click servlet bean
	 */
	@Bean
	public ServletRegistrationBean clickServletRegistrationBean() {
		ServletRegistrationBean reg = new ServletRegistrationBean(new ClickServlet(), "*.htm");
		logger.debug("Initializing Click servlet...");
		reg.setName("ClickServlet");
		reg.setAsyncSupported(true);
		reg.setEnabled(true);
		reg.setOrder(0);
		return reg;
	}

	/**
	 * @return URL Rewrite filter bean
	 */
	@Bean
	public FilterRegistrationBean urlRewriteFilterRegistrationBean() {
		// This filter is required for AngularJS to work with HTML5 URLs
		String path = "/*";
		logger.debug("Initializing UrlRewrite filter [{}]...", path);
		FilterRegistrationBean frb = new FilterRegistrationBean(new UrlRewriteFilter());
		frb.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR, DispatcherType.FORWARD));
//		frb.addInitParameter("confPath", Config.IN_PRODUCTION ? "/WEB-INF/urw-prod.xml" : "/WEB-INF/urw-dev.xml");
//		frb.addInitParameter("confPath", "urlrewrite.xml");
		frb.addInitParameter("statusEnabled", "false");
		frb.addInitParameter("logLevel", "slf4j");
		frb.setName("urlRewriteFilter");
		frb.setAsyncSupported(true);
		frb.addUrlPatterns(path);
		frb.setMatchAfter(true);
		frb.setEnabled(true);
		frb.setOrder(1);
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
				epr.addErrorPages(new ErrorPage(HttpStatus.FORBIDDEN, "/403"));
				epr.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/500"));
				epr.addErrorPages(new ErrorPage(HttpStatus.SERVICE_UNAVAILABLE, "/503"));
				epr.addErrorPages(new ErrorPage(HttpStatus.BAD_REQUEST, "/400"));
			}
		};
	}
}
