package com.erudika.scoold.controllers;

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

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.Sysprop;
import com.erudika.para.utils.Pager;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/ajax")
public class AjaxController {

	private static Sysprop s = new Sysprop();
	private static ParaClient pc;
//	@RequestMapping("/greeting")
//	public String greeting(@RequestParam(value = "name", required = false, defaultValue = "World") String name, Model model) {
//		model.addAttribute("name", name);
//		return "greeting";
//	}

	@Inject
	public AjaxController(ParaClient pc) {
		this.pc = pc;
	}


//	@RequestMapping(value = "/ajax", method = RequestMethod.GET)
//	public ModelAndView showForm(@RequestParam(value = "name", required = false, defaultValue = "World") String name) {
//		return new ModelAndView("ajax", "ajax", s);
//	}

	@ResponseBody
	@GetMapping(path = "/{keyword}", produces = MediaType.APPLICATION_JSON)
	public List<?> findTags(@PathVariable String keyword) {
		return pc.findTags(keyword, new Pager(10));
	}

//	@RequestMapping(value = "/greeting", method = RequestMethod.GET)
//	public ModelAndView showForm(@RequestParam(value = "name", required = false, defaultValue = "World") String name) {
//		return new ModelAndView("greeting", "greeting", s);
//	}
//
//	@ResponseBody
//	@RequestMapping(value = "/greeting.json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
//	public Sysprop showFormJ(@RequestParam(value = "name", required = false, defaultValue = "World") String name) {
//		return s;
//	}
//
//	@RequestMapping(value = "/addGreeting", method = RequestMethod.POST)
//	public String submit(@Valid @ModelAttribute("greeting") Sysprop greeting, BindingResult result, ModelMap model) {
//		if (!result.hasErrors()) {
//			s.setName(greeting.getName());
////			model.addAttribute("name", s.getName());
//		}
////		model.addAttribute("contactNumber", greeting.getCreatorid());
////		model.addAttribute("id", greeting.getId());
//		return result.hasErrors() ? "greeting" : "redirect:greeting";
//	}

}
