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
package com.erudika.scoold.utils;

import com.erudika.para.core.email.Emailer;
import com.erudika.para.core.utils.Para;
import com.erudika.scoold.ScooldConfig;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;

/**
 * A simple JavaMail implementation of {@link Emailer}.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class ScooldEmailer implements Emailer {

	private static final Logger logger = LoggerFactory.getLogger(ScooldEmailer.class);
	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private JavaMailSender mailSender;

	public ScooldEmailer(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	@Override
	public boolean sendEmail(final List<String> emails, final String subject, final String body) {
		if (emails == null || emails.isEmpty()) {
			return false;
		}
		Para.asyncExecute(() -> {
			emails.forEach(email -> {
				try {
					mailSender.send((MimeMessage mimeMessage) -> {
						MimeMessageHelper msg = new MimeMessageHelper(mimeMessage);
						msg.setTo(email);
						msg.setSubject(subject);
						msg.setFrom(CONF.supportEmail(), CONF.appName());
						msg.setText(body, true); // body is assumed to be HTML
					});
					logger.debug("Email sent to {}, {}", email, subject);
				} catch (MailException ex) {
					logger.error("Failed to send email to {} with body [{}]. {}", email, body, ex.getMessage());
				}
			});
		});
		return true;
	}


	@Override
	public boolean sendEmail(List<String> emails, String subject, String body, InputStream attachment, String mimeType, String fileName) {
		if (emails == null || emails.isEmpty()) {
			return false;
		}
		Para.asyncExecute(() -> {
			MimeMessagePreparator preparator = (MimeMessage mimeMessage) -> {
				MimeMessageHelper msg = new MimeMessageHelper(mimeMessage);
				Iterator<String> emailz = emails.iterator();
				msg.setTo(emailz.next());
				while (emailz.hasNext()) {
					msg.addBcc(emailz.next());
				}
				msg.setSubject(subject);
				msg.setFrom(Para.getConfig().supportEmail());
				msg.setText(body, true); // body is assumed to be HTML
				if (attachment != null) {
					msg.addAttachment(fileName, new ByteArrayDataSource(attachment, mimeType));
				}
			};
			try {
				mailSender.send(preparator);
				logger.debug("Email sent to {}, {}", emails, subject);
			} catch (MailException ex) {
				logger.error("Failed to send email. {}", ex.getMessage());
			}
		});
		return true;
	}
}
