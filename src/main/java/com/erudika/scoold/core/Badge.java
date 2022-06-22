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
package com.erudika.scoold.core;

import com.erudika.para.core.Sysprop;
import com.erudika.para.core.annotations.Stored;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public class Badge extends Sysprop {

	private static final long serialVersionUID = 1L;
	private static final String PREFIX = Utils.type(Badge.class).concat(Para.getConfig().separator());

	@Stored private String style;
	@Stored private String icon;
	@Stored private String description;
	@Stored private String tag;

	public Badge() {
		this(null);
	}

	public Badge(String id) {
		if (StringUtils.startsWith(id, PREFIX)) {
			setName(id);
			setTag(id.replaceAll(PREFIX, ""));
			setId(PREFIX.concat(getTag()));
		} else if (id != null) {
			setName(id);
			setTag(id);
			setId(PREFIX.concat(getTag()));
		}
	}

	public String getStyle() {
		return StringEscapeUtils.escapeHtml4(StringUtils.trimToEmpty(style));
	}

	public void setStyle(String style) {
		this.style = style;
	}

	public String getIcon() {
		if (Utils.isValidURL(icon)) {
			return icon;
		}
		if (StringUtils.trimToEmpty(icon).startsWith(":")) {
			return ":" + StringUtils.substringBetween(icon, ":") + ":";
		}
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = Utils.noSpaces(Utils.stripAndTrim(tag, " "), "-");
	}
}
