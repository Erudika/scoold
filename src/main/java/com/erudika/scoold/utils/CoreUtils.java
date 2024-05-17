/*
 * Copyright 2013-2024 Erudika. https://erudika.com
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

import com.erudika.para.core.Address;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.Tag;
import com.erudika.para.core.User;
import com.erudika.para.core.Vote;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.core.Badge;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Feedback;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.core.Revision;
import com.erudika.scoold.core.Sticky;
import com.erudika.scoold.core.UnapprovedQuestion;
import com.erudika.scoold.core.UnapprovedReply;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

/**
 * Core utils.
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@SuppressWarnings("unchecked")
public final class CoreUtils {

	private static final Set<String> CORE_TYPES;

	private static final Map<String, String> CORE_PARA_TYPES = new DualHashBidiMap();
	private static final Map<String, Class<? extends ParaObject>> CORE_CLASSES = new DualHashBidiMap();
	private static final Map<String, Class<? extends ParaObject>> CORE_PARA_CLASSES = new DualHashBidiMap();

	private CoreUtils() { }

	static {
		CORE_TYPES = new HashSet<>(Arrays.asList(
				Utils.type(Badge.class),
				Utils.type(Comment.class),
				Utils.type(Feedback.class),
				Utils.type(Profile.class),
				Utils.type(Question.class),
				Utils.type(Reply.class),
				Utils.type(Report.class),
				Utils.type(Revision.class),
				//Utils.type(Sticky.class),
				Utils.type(UnapprovedQuestion.class),
				Utils.type(UnapprovedReply.class),
				// Para core types
				Utils.type(Address.class),
				Utils.type(Sysprop.class),
				Utils.type(Tag.class),
				Utils.type(User.class),
				Utils.type(Vote.class)
		));
	}

	public static void registerCoreClasses() {
		Para.registerCoreClasses(
				Badge.class,
				Comment.class,
				Feedback.class,
				Profile.class,
				Question.class,
				Reply.class,
				Report.class,
				Revision.class,
				Sticky.class,
				UnapprovedQuestion.class,
				UnapprovedReply.class);
	}

	public static Set<String> getCoreTypes() {
		return Set.copyOf(CORE_TYPES);
	}

}
