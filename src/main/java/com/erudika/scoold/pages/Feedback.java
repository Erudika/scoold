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
package com.erudika.scoold.pages;

import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.utils.AppConfig;
import java.util.List;
import org.apache.click.control.Form;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Feedback <P extends Post> extends Base{

	public String title;
	public List<Post> feedbacklist;
	public Form fForm;
	public String error = "";

	public Feedback() {
		title = lang.get("feedback.title");

		if (param("write")) {
			title += " - " + lang.get("feedback.write");
			fForm = getFeedbackForm();
		}
	}

	public void onGet() {
		String feedbackType = Utils.type(com.erudika.scoold.core.Feedback.class);

		if (param("tag")) {
			String tag = getParamValue("tag");
			feedbacklist = pc.findTagged(feedbackType, new String[]{tag}, itemcount);
		} else if (param("search")) {
			feedbacklist = pc.findQuery(feedbackType, getParamValue("search"), itemcount);
		} else {
			String sortBy = "";
			if ("activity".equals(getParamValue("sortby"))) {
				sortBy = "lastactivity";
			} else if ("votes".equals(getParamValue("sortby"))) {
				sortBy = "votes";
			}
			itemcount.setSortby(sortBy);
			feedbacklist = pc.findQuery(feedbackType, "*", itemcount);
		}
	}

	public void onPost() {
		if (isValidQuestionForm()) {
			createAndGoToPost(com.erudika.scoold.core.Feedback.class);
		}
	}

	private boolean isValidQuestionForm() {
		String head = getParamValue("title");
		String body = getParamValue("body");
		String tags = getParamValue("tags");

		if (StringUtils.length(head) < 6) {
			error += "\n " + Utils.formatMessage(lang.get("minlength"), 6);
		}
		if (StringUtils.length(body) < 10) {
			error += "\n " + Utils.formatMessage(lang.get("minlength"), 10);
		}
		if (StringUtils.isBlank(tags)) {
			error += lang.get("tags.title") + " - " + lang.get("requiredfield");
		}
		if (StringUtils.split(tags, ",").length > AppConfig.MAX_TAGS_PER_POST) {
			error += lang.get("tags.toomany");
		}

		return error.isEmpty();
	}
}
