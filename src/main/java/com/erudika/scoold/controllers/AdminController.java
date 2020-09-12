/*
 * Copyright 2013-2020 Erudika. https://erudika.com
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

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.App;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.Tag;
import com.erudika.para.core.User;
import com.erudika.para.core.Webhook;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import static com.erudika.scoold.ScooldServer.ADMINLINK;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.utils.ScooldUtils;
import com.typesafe.config.ConfigValue;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.nimbusds.jwt.SignedJWT;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Entity;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

	private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
	private final String scooldVersion = getClass().getPackage().getImplementationVersion();
	private static final int MAX_SPACES = 10; // Hey! It's cool to edit this, but please consider buying Scoold Pro! :)
	private final String soDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public AdminController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping
	public String get(HttpServletRequest req, Model model) {
		if (utils.isAuthenticated(req) && !utils.isAdmin(utils.getAuthUser(req))) {
			return "redirect:" + HOMEPAGE;
		} else if (!utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + ADMINLINK;
		}
		Map<String, Object> configMap = new LinkedHashMap<String, Object>();
		for (Map.Entry<String, ConfigValue> entry : Config.getConfig().entrySet()) {
			ConfigValue value = entry.getValue();
			configMap.put(entry.getKey(), value != null ? value.unwrapped() : "-");
		}
		configMap.putAll(System.getenv());

		Pager itemcount = utils.getPager("page", req);
		Pager itemcount1 = utils.getPager("page1", req);
		itemcount.setLimit(40);
		model.addAttribute("path", "admin.vm");
		model.addAttribute("title", utils.getLang(req).get("administration.title"));
		model.addAttribute("configMap", configMap);
		model.addAttribute("version", pc.getServerVersion());
		model.addAttribute("endpoint", pc.getEndpoint());
		model.addAttribute("paraapp", Config.getConfigParam("access_key", "x"));
		model.addAttribute("spaces", pc.findQuery("scooldspace", "*", itemcount));
		model.addAttribute("webhooks", pc.findQuery(Utils.type(Webhook.class), "*", itemcount1));
		model.addAttribute("scooldimports", pc.findQuery("scooldimport", "*", new Pager(7)));
		model.addAttribute("coreScooldTypes", utils.getCoreScooldTypes());
		model.addAttribute("customHookEvents", utils.getCustomHookEvents());
		model.addAttribute("apiKeys", utils.getApiKeys());
		model.addAttribute("apiKeysExpirations", utils.getApiKeysExpirations());
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("itemcount1", itemcount1);
		model.addAttribute("isDefaultSpacePublic", utils.isDefaultSpacePublic());
		model.addAttribute("scooldVersion", Optional.ofNullable(scooldVersion).orElse("unknown"));
		String importedCount = req.getParameter("imported");
		if (importedCount != null) {
			if (req.getParameter("success") != null) {
				model.addAttribute("infoStripMsg", "Successfully imported " + importedCount + " objects from archive.");
			} else {
				model.addAttribute("infoStripMsg", "Imported operation failed!" +
						("0".equals(importedCount) ? "" : " Partially imported " + importedCount + " objects from archive."));
			}
		}
		Sysprop theme = utils.getCustomTheme();
		String themeCSS = (String) theme.getProperty("theme");
		model.addAttribute("selectedTheme", theme.getName());
		model.addAttribute("customTheme", StringUtils.isBlank(themeCSS) ? utils.getDefaultTheme() : themeCSS);
		return "base";
	}

	@PostMapping("/add-space")
	public String addSpace(@RequestParam String space, HttpServletRequest req, HttpServletResponse res, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (!StringUtils.isBlank(space) && utils.isAdmin(authUser)) {
			Sysprop spaceObj = utils.buildSpaceObject(space);
			if (utils.isDefaultSpace(spaceObj.getId()) || pc.getCount("scooldspace") >= MAX_SPACES ||
					pc.read(spaceObj.getId()) != null) {
				if (utils.isAjaxRequest(req)) {
					res.setStatus(400);
					return "space";
				} else {
					return "redirect:" + ADMINLINK + "?code=7&error=true";
				}
			} else {
				if (pc.create(spaceObj) != null) {
					authUser.getSpaces().add(spaceObj.getId() + Config.SEPARATOR + spaceObj.getName());
					authUser.update();
					model.addAttribute("space", spaceObj);
					utils.getAllSpaces().add(spaceObj);
				} else {
					model.addAttribute("error", Collections.singletonMap("name", utils.getLang(req).get("posts.error1")));
				}
			}
		} else {
			model.addAttribute("error", Collections.singletonMap("name", utils.getLang(req).get("requiredfield")));
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(model.containsAttribute("error") ? 400 : 200);
			return "space";
		} else {
			return "redirect:" + ADMINLINK;
		}
	}

	@PostMapping("/remove-space")
	public String removeSpace(@RequestParam String space, HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = utils.getAuthUser(req);
		if (!StringUtils.isBlank(space) && utils.isAdmin(authUser)) {
			Sysprop s = new Sysprop(utils.getSpaceId(space));
			pc.delete(s);
			authUser.getSpaces().remove(space);
			authUser.update();
			utils.getAllSpaces().remove(s);
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "space";
		} else {
			return "redirect:" + ADMINLINK;
		}
	}

	@PostMapping("/create-webhook")
	public String createWebhook(@RequestParam String targetUrl, @RequestParam(required = false) String type,
			@RequestParam Boolean json, @RequestParam Set<String> events, HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (Utils.isValidURL(targetUrl) && utils.isAdmin(authUser) && utils.isWebhooksEnabled()) {
			Webhook webhook = new Webhook(targetUrl);
			webhook.setCreate(events.contains("create"));
			webhook.setUpdate(events.contains("update"));
			webhook.setDelete(events.contains("delete"));
			webhook.setCreateAll(events.contains("createAll"));
			webhook.setUpdateAll(events.contains("updateAll"));
			webhook.setDeleteAll(events.contains("deleteAll"));
			webhook.setCustomEvents(events.stream().filter(e -> !StringUtils.equalsAny(e,
					"create", "update", "delete", "createAll", "updateAll", "deleteAll")).collect(Collectors.toList()));
			if (utils.getCoreScooldTypes().contains(type)) {
				webhook.setTypeFilter(type);
			}
			webhook.setUrlEncoded(!json);
			webhook.resetSecret();
			pc.create(webhook);
		} else {
			model.addAttribute("error", Collections.singletonMap("targetUrl", utils.getLang(req).get("requiredfield")));
			return "base";
		}
		return "redirect:" + ADMINLINK;
	}

	@PostMapping("/toggle-webhook")
	public String toggleWebhook(@RequestParam String id, HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = utils.getAuthUser(req);
		if (!StringUtils.isBlank(id) && utils.isAdmin(authUser) && utils.isWebhooksEnabled()) {
			Webhook webhook = pc.read(id);
			if (webhook != null) {
				webhook.setActive(!webhook.getActive());
				pc.update(webhook);
			}
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "base";
		} else {
			return "redirect:" + ADMINLINK;
		}
	}

	@PostMapping("/delete-webhook")
	public String deleteWebhook(@RequestParam String id, HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = utils.getAuthUser(req);
		if (!StringUtils.isBlank(id) && utils.isAdmin(authUser) && utils.isWebhooksEnabled()) {
			Webhook webhook = new Webhook();
			webhook.setId(id);
			pc.delete(webhook);
		}
		if (utils.isAjaxRequest(req)) {
			res.setStatus(200);
			return "base";
		} else {
			return "redirect:" + ADMINLINK;
		}
	}

	@PostMapping
	public String forceDelete(@RequestParam Boolean confirmdelete, @RequestParam String id, HttpServletRequest req) {
		Profile authUser = utils.getAuthUser(req);
		if (confirmdelete && utils.isAdmin(authUser)) {
			ParaObject object = pc.read(id);
			if (object != null) {
				object.delete();
				logger.info("{} #{} deleted {} #{}", authUser.getName(), authUser.getId(),
						object.getClass().getName(), object.getId());
			}
		}
		return "redirect:" + Optional.ofNullable(req.getParameter("returnto")).orElse(ADMINLINK);
	}

	@GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<StreamingResponseBody> backup(HttpServletRequest req, HttpServletResponse response) {
		Profile authUser = utils.getAuthUser(req);
		if (!utils.isAdmin(authUser)) {
			return new ResponseEntity<StreamingResponseBody>(HttpStatus.UNAUTHORIZED);
		}
		String fileName = App.identifier(Config.getConfigParam("access_key", "scoold")) + "_" +
					Utils.formatDate("YYYYMMdd_HHmmss", Locale.US);
		response.setContentType("application/zip");
		response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".zip");
		return new ResponseEntity<StreamingResponseBody>(out -> {
			ObjectWriter writer = ParaObjectUtils.getJsonWriterNoIdent().without(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
			try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
				long count = 0;
				int partNum = 0;
				// find all objects even if there are more than 10000 users in the system
				Pager pager = new Pager(1, "_docid", false, Config.MAX_ITEMS_PER_PAGE);
				List<ParaObject> objects;
				do {
					objects = pc.findQuery("", "*", pager);
					ZipEntry zipEntry = new ZipEntry(fileName + "_part" + (++partNum) + ".json");
					zipOut.putNextEntry(zipEntry);
					writer.writeValue(zipOut, objects);
					count += objects.size();
				} while (!objects.isEmpty());
				logger.info("Exported {} objects to {}. Downloaded by {} (pager.count={})", count, fileName + ".zip",
						authUser.getCreatorid() + " " + authUser.getName(), pager.getCount());
			} catch (final IOException e) {
				logger.error("Failed to export data.", e);
			}
		}, HttpStatus.OK);
	}

	@PostMapping("/import")
	public String restore(@RequestParam("file") MultipartFile file,
			@RequestParam(required = false, defaultValue = "false") Boolean isso,
			HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = utils.getAuthUser(req);
		if (!utils.isAdmin(authUser)) {
			res.setStatus(403);
			return null;
		}
		ObjectReader reader = ParaObjectUtils.getJsonMapper().readerFor(new TypeReference<List<Sysprop>>() { });
		ObjectReader mapReader = ParaObjectUtils.getJsonMapper().readerFor(new TypeReference<List<Map<String, Object>>>() { });
		Map<String, String> comments2authors = new LinkedHashMap<>();
		int	count = 0;
		String filename = file.getOriginalFilename();
		Sysprop s = new Sysprop();
		s.setType("scooldimport");
		try (InputStream inputStream = file.getInputStream()) {
			if (StringUtils.endsWithIgnoreCase(filename, ".zip")) {
				try (ZipInputStream zipIn = new ZipInputStream(inputStream)) {
					ZipEntry zipEntry;
					while ((zipEntry = zipIn.getNextEntry()) != null) {
						List<ParaObject> objects;
						if (isso) {
							objects = importFromSOArchive(zipIn, zipEntry, mapReader, comments2authors);
						} else {
							objects = pc.createAll(reader.readValue(new FilterInputStream(zipIn) {
								public void close() throws IOException {
									zipIn.closeEntry();
								}
							}));
						}
						count += objects.size();
					}
					if (isso) {
						updateSOCommentAuthors(comments2authors);
					}
				}
			} else if (StringUtils.endsWithIgnoreCase(filename, ".json")) {
				List<Sysprop> objects = reader.readValue(inputStream);
				count = objects.size();
				pc.createAll(objects);
			}
			s.setCreatorid(authUser.getCreatorid());
			s.setName(authUser.getName());
			s.addProperty("count", count);
			s.addProperty("file", filename);
			logger.info("Imported {} objects to {}. Executed by {}", count,
					Config.getConfigParam("access_key", "scoold"), authUser.getCreatorid() + " " + authUser.getName());

			if (count > 0) {
				pc.create(s);
			}
		} catch (Exception e) {
			logger.error("Failed to import " + filename, e);
			return "redirect:" + ADMINLINK + "?error=true&imported=" + count;
		}
		return "redirect:" + ADMINLINK + "?success=true&imported=" + count;
	}

	@PostMapping("/set-theme")
	public String setTheme(@RequestParam String theme, @RequestParam String css, HttpServletRequest req) {
		Profile authUser = utils.getAuthUser(req);
		if (utils.isAdmin(authUser)) {
			utils.setCustomTheme(Utils.stripAndTrim(theme, "", true), css);
		}
		return "redirect:" + ADMINLINK;
	}

	@ResponseBody
	@PostMapping(path = "/generate-api-key", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> generateAPIKey(@RequestParam Integer validityHours,
			HttpServletRequest req, Model model) throws ParseException {
		Profile authUser = utils.getAuthUser(req);
		if (utils.isAdmin(authUser)) {
			String jti = UUID.randomUUID().toString();
			Map<String, Object> claims = Collections.singletonMap("jti", jti);
			SignedJWT jwt = utils.generateJWToken(claims, TimeUnit.HOURS.toSeconds(validityHours));
			if (jwt != null) {
				String jwtString = jwt.serialize();
				Date exp = jwt.getJWTClaimsSet().getExpirationTime();
				utils.registerApiKey(jti, jwtString);
				Map<String, Object> data = new HashMap<String, Object>();
				data.put("jti", jti);
				data.put("jwt", jwtString);
				data.put("exp", exp == null ? 0L : Utils.formatDate(exp.getTime(), "YYYY-MM-dd HH:mm", Locale.UK));
				return ResponseEntity.ok().body(data);
			}
		}
		return ResponseEntity.status(403).build();
	}

	@ResponseBody
	@PostMapping(path = "/revoke-api-key", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> revokeAPIKey(@RequestParam String jti, HttpServletRequest req, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (utils.isAdmin(authUser)) {
			utils.revokeApiKey(jti);
			return ResponseEntity.ok().build();
		}
		return ResponseEntity.status(403).build();
	}

	private List<ParaObject> importFromSOArchive(ZipInputStream zipIn, ZipEntry zipEntry,
			ObjectReader mapReader, Map<String, String> comments2authors) throws IOException, ParseException {
		if (zipEntry.getName().endsWith(".json")) {
			List<Map<String, Object>> objs = mapReader.readValue(new FilterInputStream(zipIn) {
				public void close() throws IOException {
					zipIn.closeEntry();
				}
			});
			List<ParaObject> toImport = new LinkedList<>();
			switch (zipEntry.getName()) {
				case "posts.json":
					importPostsFromSO(objs, toImport);
					break;
				case "tags.json":
					importTagsFromSO(objs, toImport);
					break;
				case "comments.json":
					importCommentsFromSO(objs, toImport, comments2authors);
					break;
				case "users.json":
					importUsersFromSO(objs, toImport);
					break;
				case "users2badges.json":
					// nice to have...
					break;
				case "accounts.json":
					importAccountsFromSO(objs);
					break;
				default:
					break;
			}
			// IN PRO: rewrite all image links to relative local URLs
			return toImport;
		} else {
			// IN PRO: store files in ./uploads
			return Collections.emptyList();
		}
	}

	private void importPostsFromSO(List<Map<String, Object>> objs, List<ParaObject> toImport)
			throws ParseException {
		logger.info("Importing {} posts...", objs.size());
		for (Map<String, Object> obj : objs) {
			Post p;
			if ("question".equalsIgnoreCase((String) obj.get("postType"))) {
				p = new Question();
				p.setTitle((String) obj.get("title"));
				String t = StringUtils.stripStart(StringUtils.stripEnd((String) obj.
						getOrDefault("tags", ""), "|"), "|");
				p.setTags(Arrays.asList(t.split("\\|")));
				p.setAnswercount(((Integer) obj.getOrDefault("answerCount", 0)).longValue());
				Integer answerId = (Integer) obj.getOrDefault("acceptedAnswerId", null);
				p.setAnswerid(answerId != null ? "post_" + answerId : null);
			} else {
				p = new Reply();
				Integer parentId = (Integer) obj.getOrDefault("parentId", null);
				p.setParentid(parentId != null ? "post_" + parentId : null);
			}
			p.setId("post_" + (Integer) obj.getOrDefault("id", Utils.getNewId()));
			p.setBody((String) obj.get("bodyMarkdown"));
			p.setVotes((Integer) obj.getOrDefault("score", 0));
			p.setTimestamp(DateUtils.parseDate((String) obj.get("creationDate"), soDateFormat).getTime());
			Integer creatorId = (Integer) obj.getOrDefault("ownerUserId", null);
			if (creatorId == null || creatorId == -1) {
				p.setCreatorid(utils.getSystemUser().getId());
			} else {
				p.setCreatorid(Profile.id("user_" + creatorId)); // add prefix to avoid conflicts
			}
			toImport.add(p);
		}
		pc.createAll(toImport);
	}

	private void importTagsFromSO(List<Map<String, Object>> objs, List<ParaObject> toImport) {
		logger.info("Importing {} tags...", objs.size());
		for (Map<String, Object> obj : objs) {
			Tag t = new Tag((String) obj.get("name"));
			t.setCount((Integer) obj.getOrDefault("count", 0));
			toImport.add(t);
		}
		pc.createAll(toImport);
	}

	private void importCommentsFromSO(List<Map<String, Object>> objs, List<ParaObject> toImport,
			Map<String, String> comments2authors) throws ParseException {
		logger.info("Importing {} comments...", objs.size());
		for (Map<String, Object> obj : objs) {
			Comment c = new Comment();
			c.setId("comment_" + (Integer) obj.get("id"));
			c.setComment((String) obj.get("text"));
			Integer parentId = (Integer) obj.getOrDefault("postId", null);
			c.setParentid(parentId != null ? "post_" + parentId : null);
			c.setTimestamp(DateUtils.parseDate((String) obj.get("creationDate"), soDateFormat).getTime());
			Integer creatorId = (Integer) obj.getOrDefault("userId", null);
			String userid = "user_" + creatorId;
			c.setCreatorid(creatorId != null ? Profile.id(userid) : utils.getSystemUser().getId());
			comments2authors.put(c.getId(), userid);
			toImport.add(c);
		}
		pc.createAll(toImport);
	}

	private void importUsersFromSO(List<Map<String, Object>> objs, List<ParaObject> toImport)
			throws ParseException {
		logger.info("Importing {} users...", objs.size());
		for (Map<String, Object> obj : objs) {
			User u = new User();
			u.setId("user_" + (Integer) obj.get("id"));
			u.setTimestamp(DateUtils.parseDate((String) obj.get("creationDate"), soDateFormat).getTime());
			u.setActive(true);
			u.setCreatorid(((Integer) obj.get("accountId")).toString());
			u.setGroups("admin".equalsIgnoreCase((String) obj.get("userTypeId"))
					? User.Groups.ADMINS.toString() : User.Groups.USERS.toString());
			u.setEmail(u.getId() + "@scoold.com");
			u.setIdentifier(u.getEmail());
			u.setName((String) obj.get("realName"));
			String lastLogin = (String) obj.get("lastLoginDate");
			u.setUpdated(StringUtils.isBlank(lastLogin) ? null : DateUtils.parseDate(lastLogin, soDateFormat).getTime());
			u.setPicture((String) obj.get("profileImageUrl"));
			u.setPassword(Utils.generateSecurityToken(10));

			Profile p = Profile.fromUser(u);
			p.setVotes((Integer) obj.get("reputation"));
			p.setAboutme((String) obj.getOrDefault("title", ""));
			p.setLastseen(u.getUpdated());
			toImport.add(u);
			toImport.add(p);
		}
		pc.createAll(toImport);
	}

	private void importAccountsFromSO(List<Map<String, Object>> objs) {
		logger.info("Importing {} accounts...", objs.size());
		List<Map<String, String>> toPatch = new LinkedList<>();
		Map<String, String> accounts = objs.stream().collect(Collectors.
				toMap(k -> ((Integer) k.get("accountId")).toString(), v -> (String) v.get("verifiedEmail")));
		// find all user objects even if there are more than 10000 users in the system
		Pager pager = new Pager(1, "_docid", false, Config.MAX_ITEMS_PER_PAGE);
		List<User> users;
		do {
			users = pc.findQuery(Utils.type(User.class), "*", pager);
			if (!users.isEmpty()) {
				users.stream().forEach(u -> {
					if (accounts.containsKey(u.getCreatorid())) {
						u.setEmail(accounts.get(u.getCreatorid()));
						Map<String, String> user = new HashMap<>();
						user.put(Config._ID, u.getId());
						user.put(Config._EMAIL, u.getEmail());
						user.put(Config._IDENTIFIER, u.getEmail());
						toPatch.add(user);
					}
				});
			}
			pc.invokePatch("_batch", Entity.json(toPatch));
			toPatch.clear();
		} while (!users.isEmpty());
	}

	private void updateSOCommentAuthors(Map<String, String> comments2authors) {
		if (!comments2authors.isEmpty()) {
			// fetch & update comment author names
			Map<String, ParaObject> authors = pc.readAll(new ArrayList<>(comments2authors.values())).stream().
					collect(Collectors.toMap(k -> k.getId(), v -> v));
			List<Map<String, String>> toPatch = new LinkedList<>();
			for (Map.Entry<String, String> entry : comments2authors.entrySet()) {
				Map<String, String> user = new HashMap<>();
				user.put(Config._ID, entry.getKey());
				if (authors.containsKey(entry.getValue())) {
					user.put("authorName", authors.get(entry.getValue()).getName());
				}
				toPatch.add(user);
			}
			pc.invokePatch("_batch", Entity.json(toPatch));
		}
	}
}
