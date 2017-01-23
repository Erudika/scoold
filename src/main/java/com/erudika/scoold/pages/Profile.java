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
import com.erudika.scoold.core.Profile.Badge;
import static com.erudika.para.core.User.Groups.*;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import java.util.List;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Profile extends Base{

    public String title;
    public boolean isMyProfile;
	public boolean canEdit;
	public com.erudika.scoold.core.Profile showUser;
	public List<User> contactlist;
	public List<? extends Post> questionslist;
	public List<? extends Post> answerslist;
	public String gravatarPicture;
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

		String showuid = com.erudika.scoold.core.Profile.id(param("id") ? getParamValue("id") : authUser.getId());

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

		if (authenticated && (isMyid(showuid) || isAdmin)) {
			canEdit = true;
		}

		title = lang.get("profile.title") + " - " + showUser.getName();
		questionslist = showUser.getAllQuestions(qpager);
		answerslist = showUser.getAllAnswers(apager);
		gravatarPicture = "https://www.gravatar.com/avatar/" + Utils.md5(showUser.getUser().getEmail()) + "?size=400&d=mm";

		if (param("getsmallpersonbox")) {
			addModel("showAjaxUser", showUser);
		}
    }

    public void onGet() {
		if (!authenticated || showUser == null) return;
		if (!isMyProfile) {
			boolean isShowUserAdmin = User.Groups.ADMINS.toString().equals(showUser.getGroups());
			boolean isShowUserMod = User.Groups.MODS.toString().equals(showUser.getGroups());
			if (param("makemod") && isAdmin && !isShowUserAdmin) {
				boolean makemod = Boolean.parseBoolean(getParamValue("makemod")) && !isShowUserMod;
				showUser.setGroups(makemod ? MODS.toString() : USERS.toString());
				showUser.update();
			}
		}
    }

    public void onPost() {
		if (canEdit) {
			boolean update = false;

			if (param("name")) {
				showUser.setName(getParamValue("name"));
				update = true;
			}
			if (param("location")) {
				showUser.setLocation(getParamValue("location"));
				update = true;
			}
			if (param("website")) {
				showUser.setWebsite(getParamValue("website"));
				update = true;
			}
			if (param("aboutme")) {
				showUser.setAboutme(getParamValue("aboutme"));
				update = true;
			}
			if (param("picture") && !getParamValue("picture").equals(showUser.getUser().getPicture())) {
				showUser.setPicture(getParamValue("picture"));
				update = true;
			}

			addBadgeOnce(Badge.NICEPROFILE, showUser.isComplete() && isMyid(showUser.getId()));

			if (update) {
				showUser.update();
			}
			if (!isAjaxRequest())
				setRedirect(profilelink); //redirect after post
		}
	}

	private boolean isMyid(String id) {
		return authenticated && authUser.getId().equals(id);
	}
}
