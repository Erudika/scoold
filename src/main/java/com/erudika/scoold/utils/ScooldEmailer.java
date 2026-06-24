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
package com.erudika.scoold.utils;

import com.erudika.para.core.App;
import com.erudika.para.core.email.Emailer;
import static com.erudika.para.core.email.Emailer.logger;
import com.erudika.para.core.utils.Para;
import com.erudika.scoold.ScooldConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;

/**
 * A simple JavaMail implementation of {@link Emailer}.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class ScooldEmailer implements Emailer {

	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private final JavaMailSender mailSender;

	public ScooldEmailer(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	@Override
	public void sendSingleBatch(App app, List<String> emails, String subject, String body, ByteArrayDataSource attachment, String fileName) {
		MimeMessagePreparator preparator = (MimeMessage mimeMessage) -> {
			MimeMessageHelper msg = new MimeMessageHelper(mimeMessage);
			if (emails.size() > 1) {
				emails.stream().forEach(e -> {
					try {
						msg.addBcc(e);
					} catch (MessagingException ex) {
						logger.error("Failed to add email '" + e + "' to BCC list:", ex.getMessage());
					}
				});
				msg.setTo("noreply@" + StringUtils.substringAfter(CONF.supportEmail(), "@"));
			} else {
				msg.setTo(emails.iterator().next());
			}
			msg.setSubject(subject);
			msg.setFrom(CONF.supportEmail(), CONF.appName());
			msg.setText(body, true); // body is assumed to be HTML
			if (attachment != null) {
				msg.addAttachment(fileName, attachment);
			}
		};
		try {
			Para.asyncExecute(() -> mailSender.send(preparator));
			logger.debug("Email sent to {}, {}", emails, subject);
		} catch (MailException ex) {
			logger.error("Failed to send email. {}", ex.getMessage());
		}
	}
}
