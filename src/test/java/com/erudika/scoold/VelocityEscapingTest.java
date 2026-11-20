/*
 * Copyright 2013-2026 Erudika. https://erudika.com
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
import com.erudika.scoold.ScooldServer;
import com.erudika.scoold.velocity.VelocityConfigurer;
import java.io.StringWriter;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

public class VelocityEscapingTest {

	private static VelocityEngine velocityEngine;

	@BeforeClass
	public static void setUpVelocityEngine() throws Exception {
		VelocityConfigurer configurer = new ScooldServer().velocityConfigBean();
		configurer.afterPropertiesSet();
		velocityEngine = configurer.getVelocityEngine();
	}

	@Test
	public void escapesHtmlInTemplateReferencesByDefault() {
		assertEquals("&lt;b&gt;unsafe&lt;/b&gt;",
				render("$value", "value", "<b>unsafe</b>"));
	}

	@Test
	public void leavesHtmlUnescapedForExplicitlyRawVariable() {
		assertEquals("<b>trusted</b>",
				render("$_value", "_value", "<b>trusted</b>"));
	}

	@Test
	public void escapesHtmlForVariableWithUnderscoreInTheMiddle() {
		assertEquals("&lt;b&gt;unsafe&lt;/b&gt;",
				render("$value_two", "value_two", "<b>unsafe</b>"));
	}

	private static String render(String template, String key, String value) {
		VelocityContext context = new VelocityContext();
		context.put(key, value);
		StringWriter output = new StringWriter();
		velocityEngine.evaluate(context, output, "VelocityEscapingTest", template);
		return output.toString();
	}
}
