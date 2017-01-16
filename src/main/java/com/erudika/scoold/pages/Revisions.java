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

import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Revision;
import java.util.List;


/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Revisions extends Base {

    public String title;
	public List<Revision> revisionslist;
	public Post showPost;

    public Revisions() {
        title = title + " - " + lang.get("revisions.title");
		showPost = pc.read(getParamValue("id"));
		if (showPost != null) {
			revisionslist = showPost.getRevisions(itemcount);
		} else {
			setRedirect(questionlink);
		}
    }

	public void onGet() {
	}
}