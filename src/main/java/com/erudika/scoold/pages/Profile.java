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

import com.erudika.para.core.User;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.ScooldUser.Badge;
import static com.erudika.para.core.User.Groups.*;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.utils.Pager;
import com.erudika.scoold.core.ScooldUser;
import java.util.List;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Profile extends Base{

    public String title;
    public boolean isMyProfile;
	public boolean canEdit;
	public ScooldUser showUser;
	public List<User> contactlist;
	public List<? extends Post> questionslist;
	public List<? extends Post> answerslist;
	public Pager qpager;
	public Pager apager;

    public Profile() {
        title = lang.get("profile.title");
		canEdit = false;
		qpager = new Pager();
		apager = new Pager();
		
		if (!authenticated && !param("id")) {
			setRedirect(HOMEPAGE);
			return;
		}

		String showuid = ScooldUser.id(param("id") ? getParamValue("id") : authUser.getId());

		if (isMyid(showuid)) {
			//requested userid !exists or = my userid => show my profile
			showUser = authUser;
			isMyProfile = true;
		} else if (showuid != null) {
			showUser = pc.read(showuid);
			isMyProfile = false;
		}

		if (showUser == null || !ParaObjectUtils.typesMatch(showUser)) {
			setRedirect(profilelink);
			return;
		}

		if (authenticated && (isMyid(showuid) || inRole("admin"))) {
			canEdit = true;
		}

		title = lang.get("profile.title") + " - " + showUser.getName();
		questionslist = showUser.getAllQuestions(qpager);
		answerslist = showUser.getAllAnswers(apager);

		if (param("getsmallpersonbox")) {
			addModel("showAjaxUser", showUser);
		}
    }

    public void onGet() {
		if (!authenticated || showUser == null) return;

		if (!isMyProfile) {
			if (param("makemod") && inRole("admin") && !showUser.isAdmin()) {
				boolean makemod = Boolean.parseBoolean(getParamValue("makemod")) && !showUser.isModerator();
				showUser.setGroups(makemod ? MODS.toString() : USERS.toString());
				showUser.update();
			}
		}
    }

    public void onPost() {
		if (canEdit) {
			showUser.setName(getParamValue("name"));
			showUser.setLocation(getParamValue("location"));
			showUser.setWebsite(getParamValue("website"));
			showUser.setAboutme(getParamValue("aboutme"));
			addBadgeOnce(Badge.NICEPROFILE, showUser.isComplete() && isMyid(showUser.getId()));
			showUser.update();

			if (!isAjaxRequest())
				setRedirect(profilelink); //redirect after post
		}
	}

	private boolean isMyid(String id) {
		return authenticated && authUser.getId().equals(id);
	}
}
