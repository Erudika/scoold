/*
 * Copyright 2002-2012 the original author or authors.
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
 */

package com.erudika.scoold.velocity;

import jakarta.servlet.ServletContext;
import java.io.IOException;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.web.context.ServletContextAware;

/**
 * JavaBean to configure Velocity for web usage, via the "configLocation"
 * and/or "velocityProperties" and/or "resourceLoaderPath" bean properties.
 * The simplest way to use this class is to specify just a "resourceLoaderPath";
 * you do not need any further configuration then.
 * *
 * This bean must be included in the application context of any application
 * using Spring's {@link VelocityView} for web MVC. It exists purely to configure
 * Velocity; it is not meant to be referenced by application components (just
 * internally by VelocityView). This class implements {@link VelocityConfig}
 * in order to be found by VelocityView without depending on the bean name of
 * this configurer. Each DispatcherServlet may define its own VelocityConfigurer
 * if desired, potentially with different template loader paths.
 *
 * <p>Note that you can also refer to a pre-configured VelocityEngine
 * instance via the "velocityEngine" property.
 * This allows to share a VelocityEngine for web and email usage, for example.
 *
 * <p>This configurer registers the "spring.vm" Velocimacro library for web views
 * (contained in this package and thus in {@code spring.jar}), which makes
 * all of Spring's default Velocity macros available to the views.
 * This allows for using the Spring-provided macros such as follows:
 *
 * <pre class="code">
 * #springBind("person.age")
 * age is ${status.value}</pre>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Darren Davison
 * @see #setConfigLocation
 * @see #setVelocityProperties
 * @see #setResourceLoaderPath
 * @see #setVelocityEngine
 * @see VelocityView
 */
public class VelocityConfigurer extends VelocityEngineFactory
		implements VelocityConfig, InitializingBean, ResourceLoaderAware, ServletContextAware {

	private VelocityEngine velocityEngine;

	private ServletContext servletContext;


	/**
	 * Set a pre-configured VelocityEngine to use for the Velocity web
	 * configuration.
	 * <p>Note that the Spring macros will <i>not</i> be enabled automatically in
	 * case of an external VelocityEngine passed in here. Make sure to include
	 * {@code spring.vm} in your template loader path in such a scenario
	 * (if there is an actual need to use those macros).
	 * <p>If this is not set, VelocityEngineFactory's properties
	 * (inherited by this class) have to be specified.
	 * @param velocityEngine engine
	 */
	public void setVelocityEngine(VelocityEngine velocityEngine) {
		this.velocityEngine = velocityEngine;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * Initialize VelocityEngineFactory's VelocityEngine
	 * if not overridden by a pre-configured VelocityEngine.
	 * @see #createVelocityEngine
	 * @see #setVelocityEngine
	 */
	@Override
	public void afterPropertiesSet() throws IOException, VelocityException {
		if (this.velocityEngine == null) {
			this.velocityEngine = createVelocityEngine();
		}
	}

	/**
	 * Provides a ClasspathResourceLoader in addition to any default or user-defined
	 * loader in order to load the spring Velocity macros from the class path.
	 * @see org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
	 * @param velocityEngine engine
	 */
	@Override
	protected void postProcessVelocityEngine(VelocityEngine velocityEngine) {
		velocityEngine.setApplicationAttribute(ServletContext.class.getName(), this.servletContext);
	}

	@Override
	public VelocityEngine getVelocityEngine() {
		return this.velocityEngine;
	}

}
