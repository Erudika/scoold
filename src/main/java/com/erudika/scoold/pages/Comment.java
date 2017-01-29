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

import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.utils.Config;
import static com.erudika.scoold.pages.Base.pc;
import com.erudika.scoold.utils.AppConfig;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Comment extends Base{

	public com.erudika.scoold.core.Comment showComment;

	public String title;

	public Comment() {
		title = lang.get("comment.title");
	}

	public void onGet() {
		if (param("id")) {
			String id = getParamValue("id");
			showComment = pc.read(id);
			if (showComment == null || !ParaObjectUtils.typesMatch(showComment)) {
				setRedirect(HOMEPAGE);
			}
		}
	}

	public void onPost() {
		if (param("deletecomment") && authenticated) {
			String id = getParamValue("deletecomment");
			com.erudika.scoold.core.Comment c = pc.read(id);
			if (c != null && (c.getCreatorid().equals(authUser.getId()) || isMod)) {
				// check parent and correct (for multi-parent-object pages)
				c.delete();
				if (!isMod) {
					addBadge(com.erudika.scoold.core.Profile.Badge.DISCIPLINED, true);
				}
			}
		} else if (canComment && param("comment")) {
			String comment = getParamValue("comment");
			String parentid = getParamValue(Config._PARENTID);
			if (StringUtils.isBlank(comment) || StringUtils.isBlank(parentid)) {
				com.erudika.scoold.core.Comment lastComment = new com.erudika.scoold.core.Comment();
				lastComment.setComment(comment);
				lastComment.setParentid(parentid);
				lastComment.setCreatorid(authUser.getId());
				lastComment.setAuthorName(authUser.getName());

				if (lastComment.create() != null) {
					long commentCount = authUser.getComments();
					addBadgeOnce(com.erudika.scoold.core.Profile.Badge.COMMENTATOR,
							commentCount >= AppConfig.COMMENTATOR_IFHAS);
					authUser.setComments(commentCount + 1);
					authUser.update();
					commentslist.add(lastComment);
					addModel("newcomment", lastComment);
				}
			}
		}
	}

}
