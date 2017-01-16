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
package com.erudika.scoold.controllers;

import com.erudika.para.core.Sysprop;
import javax.validation.Valid;
import javax.ws.rs.core.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class GreetingController {

	private static Sysprop s = new Sysprop();

//	@RequestMapping("/greeting")
//	public String greeting(@RequestParam(value = "name", required = false, defaultValue = "World") String name, Model model) {
//		model.addAttribute("name", name);
//		return "greeting";
//	}

	@RequestMapping(value = "/greeting", method = RequestMethod.GET)
	public ModelAndView showForm(@RequestParam(value = "name", required = false, defaultValue = "World") String name) {
		return new ModelAndView("greeting", "greeting", s);
	}

	@ResponseBody
	@RequestMapping(value = "/greeting.json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
	public Sysprop showFormJ(@RequestParam(value = "name", required = false, defaultValue = "World") String name) {
		return s;
	}

	@RequestMapping(value = "/addGreeting", method = RequestMethod.POST)
	public String submit(@Valid @ModelAttribute("greeting") Sysprop greeting, BindingResult result, ModelMap model) {
		if (!result.hasErrors()) {
			s.setName(greeting.getName());
//			model.addAttribute("name", s.getName());
		}
//		model.addAttribute("contactNumber", greeting.getCreatorid());
//		model.addAttribute("id", greeting.getId());
		return result.hasErrors() ? "greeting" : "redirect:greeting";
	}

}
