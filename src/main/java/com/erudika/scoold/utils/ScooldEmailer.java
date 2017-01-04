/*
 * Copyright 2013-2017 Erudika. http://erudika.com
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

import com.erudika.para.email.Emailer;
import java.util.List;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class ScooldEmailer implements Emailer {

	@Override
	public boolean sendEmail(List<String> emails, String subject, String body) {
		System.out.println("EMAIL SENT: " + subject);
		return true;
	}

}
