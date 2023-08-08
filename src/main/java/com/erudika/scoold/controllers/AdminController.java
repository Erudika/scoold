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
package com.erudika.scoold.controllers;

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.App;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.Tag;
import com.erudika.para.core.User;
import com.erudika.para.core.Webhook;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Pager;
import com.erudika.para.core.utils.Para;
import com.erudika.para.core.utils.ParaObjectUtils;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.ADMINLINK;
import static com.erudika.scoold.ScooldServer.HOMEPAGE;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.utils.ScooldUtils;
import static com.erudika.scoold.utils.ScooldUtils.MAX_SPACES;
import com.erudika.scoold.utils.Version;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.nimbusds.jwt.SignedJWT;
import com.typesafe.config.ConfigValue;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
	private static final ScooldConfig CONF = ScooldUtils.getConfig();
	private final String soDateFormat1 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	private final String soDateFormat2 = "yyyy-MM-dd'T'HH:mm:ss'Z'";
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
		for (Map.Entry<String, ConfigValue> entry : CONF.getConfig().entrySet()) {
			ConfigValue value = entry.getValue();
			configMap.put(entry.getKey(), value != null ? value.unwrapped() : "-");
		}

		Pager itemcount = utils.getPager("page", req);
		Pager itemcount1 = utils.getPager("page1", req);
		itemcount.setLimit(40);
		model.addAttribute("path", "admin.vm");
		model.addAttribute("title", utils.getLang(req).get("administration.title"));
		model.addAttribute("configMap", configMap);
		model.addAttribute("version", pc.getServerVersion());
		model.addAttribute("endpoint", CONF.redirectUri());
		model.addAttribute("paraapp", CONF.paraAccessKey());
		model.addAttribute("spaces", getSpaces(itemcount));
		model.addAttribute("webhooks", pc.findQuery(Utils.type(Webhook.class), "*", itemcount1));
		model.addAttribute("scooldimports", pc.findQuery("scooldimport", "*", new Pager(7)));
		model.addAttribute("coreScooldTypes", utils.getCoreScooldTypes());
		model.addAttribute("customHookEvents", utils.getCustomHookEvents());
		model.addAttribute("apiKeys", utils.getApiKeys());
		model.addAttribute("apiKeysExpirations", utils.getApiKeysExpirations());
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("itemcount1", itemcount1);
		model.addAttribute("isDefaultSpacePublic", utils.isDefaultSpacePublic());
		model.addAttribute("scooldVersion", Version.getVersion());
		model.addAttribute("scooldRevision", Version.getRevision());
		String importedCount = req.getParameter("imported");
		if (importedCount != null) {
			if (req.getParameter("success") != null) {
				model.addAttribute("infoStripMsg", "Started a new data import task. ");
			} else {
				model.addAttribute("infoStripMsg", "Data import task failed! The archive was partially imported.");
			}
		}
		Sysprop theme = utils.getCustomTheme();
		String themeCSS = (String) theme.getProperty("theme");
		model.addAttribute("selectedTheme", theme.getName());
		model.addAttribute("customTheme", StringUtils.isBlank(themeCSS) ? utils.getDefaultTheme() : themeCSS);
		return "base";
	}

	@PostMapping("/add-space")
	public String addSpace(@RequestParam String space,
			@RequestParam(required = false, defaultValue = "false") Boolean assigntoall,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		Profile authUser = utils.getAuthUser(req);
		if (!StringUtils.isBlank(space) && utils.isAdmin(authUser)) {
			Sysprop spaceObj = utils.buildSpaceObject(space);
			if (utils.isDefaultSpace(spaceObj.getId()) || pc.getCount("scooldspace") >= MAX_SPACES ||
					pc.read(spaceObj.getId()) != null) {
				model.addAttribute("error", Map.of("name", "Space exists or maximum number of spaces reached."));
			} else {
				if (assigntoall) {
					spaceObj.setTags(List.of("assign-to-all"));
					utils.assingSpaceToAllUsers(spaceObj);
				}
				if (pc.create(spaceObj) != null) {
					authUser.getSpaces().add(spaceObj.getId() + Para.getConfig().separator() + spaceObj.getName());
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
		if (!StringUtils.isBlank(space) && !utils.isDefaultSpace(space) && utils.isAdmin(authUser)) {
			Sysprop s = utils.buildSpaceObject(space);
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

	@PostMapping("/rename-space")
	public String renameSpace(@RequestParam String space,
			@RequestParam(required = false, defaultValue = "false") Boolean assigntoall,
			@RequestParam String newspace, HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = utils.getAuthUser(req);
		Sysprop s = pc.read(utils.getSpaceId(space));
		if (s != null && !utils.isDefaultSpace(space) && utils.isAdmin(authUser)) {
			String origSpace = s.getId() + Para.getConfig().separator() + s.getName();
			String newSpace = s.getId() + Para.getConfig().separator() + newspace;
			if (!origSpace.equals(newSpace)) {
				s.setName(newspace);
				pc.update(s);
				utils.getAllSpaces().parallelStream().
						filter(ss -> ss.getId().equals(s.getId())).
						forEach(e -> e.setName(newspace));
				pc.updateAllPartially((toUpdate, pager) -> {
					String query = "properties.spaces:(\"" + origSpace + "\")";
					List<Profile> profiles = pc.findQuery(Utils.type(Profile.class), query, pager);
					profiles.stream().forEach(p -> {
						p.getSpaces().remove(origSpace);
						p.getSpaces().add(newSpace);
						Map<String, Object> profile = new HashMap<>();
						profile.put(Config._ID, p.getId());
						profile.put("spaces", p.getSpaces());
						toUpdate.add(profile);
					});
					return profiles;
				});
				pc.updateAllPartially((toUpdate, pager) -> {
					String query = "properties.space:(\"" + origSpace + "\")";
					List<Post> posts = pc.findQuery("", query, pager);
					posts.stream().forEach(p -> {
						Map<String, Object> post = new HashMap<>();
						post.put(Config._ID, p.getId());
						post.put("space", newSpace);
						toUpdate.add(post);
					});
					return posts;
				});
			}
			if (utils.isAutoAssignedSpace(s) ^ assigntoall) {
				s.setTags(assigntoall ? List.of("assign-to-all") : List.of());
				utils.assingSpaceToAllUsers(assigntoall ? s : null);
				pc.update(s);
				utils.getAllSpaces().parallelStream().
						filter(ss -> ss.getId().equals(s.getId())).
						forEach(e -> e.setTags(s.getTags()));
			}
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
		return "redirect:" + ADMINLINK + "#webhooks-tab";
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
			return "redirect:" + ADMINLINK + "#webhooks-tab";
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
			return "redirect:" + ADMINLINK + "#webhooks-tab";
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

	@GetMapping(value = "/export", produces = "application/zip")
	public ResponseEntity<StreamingResponseBody> backup(HttpServletRequest req, HttpServletResponse response) {
		Profile authUser = utils.getAuthUser(req);
		if (!utils.isAdmin(authUser)) {
			return new ResponseEntity<StreamingResponseBody>(HttpStatus.UNAUTHORIZED);
		}
		String fileName = App.identifier(CONF.paraAccessKey()) + "_" + Utils.formatDate("YYYYMMdd_HHmmss", Locale.US);
		response.setContentType("application/zip");
		response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".zip");
		return new ResponseEntity<StreamingResponseBody>(out -> {
			// export all fields, even those which are JSON-ignored
			ObjectWriter writer = JsonMapper.builder().disable(MapperFeature.USE_ANNOTATIONS).build().writer().
					without(SerializationFeature.INDENT_OUTPUT).
					without(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
			try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
				long count = 0;
				int partNum = 0;
				// find all objects even if there are more than 10000 users in the system
				Pager pager = new Pager(1, "_docid", false, CONF.maxItemsPerPage());
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
			@RequestParam(required = false, defaultValue = "false") Boolean deleteall,
			HttpServletRequest req, HttpServletResponse res) {
		Profile authUser = utils.getAuthUser(req);
		if (!utils.isAdmin(authUser)) {
			res.setStatus(403);
			return null;
		}
		if (deleteall) {
			logger.info("Deleting all existing objects before import...");
			pc.readEverything((pager) -> {
				pager.setSelect(Collections.singletonList(Config._ID));
				List<Sysprop> objects = pc.findQuery("", "*", pager);
				pc.deleteAll(objects.stream().map(r -> r.getId()).collect(Collectors.toList()));
				return objects;
			});
		}
		ObjectReader reader = ParaObjectUtils.getJsonMapper().readerFor(new TypeReference<List<Map<String, Object>>>() { });
		String filename = file.getOriginalFilename();
		Sysprop s = new Sysprop();
		s.setType("scooldimport");
		s.setCreatorid(authUser.getCreatorid());
		s.setName(authUser.getName());
		s.addProperty("status", "pending");
		s.addProperty("count", 0);
		s.addProperty("file", filename);
		Sysprop si = pc.create(s);

		Para.asyncExecute(() -> {
			Map<String, String> comments2authors = new LinkedHashMap<>();
			Map<String, User> accounts2emails = new LinkedHashMap<>();
			try (InputStream inputStream = file.getInputStream()) {
				if (StringUtils.endsWithIgnoreCase(filename, ".zip")) {
					try (ZipInputStream zipIn = new ZipInputStream(inputStream)) {
						ZipEntry zipEntry;
						List<ParaObject> toCreate = new LinkedList<ParaObject>();
						long countUpdated = Utils.timestamp();
						while ((zipEntry = zipIn.getNextEntry()) != null) {
							if (isso) {
								importFromSOArchive(zipIn, zipEntry, reader, comments2authors, accounts2emails, si);
							} else if (zipEntry.getName().endsWith(".json")) {
								List<Map<String, Object>> objects = reader.readValue(new FilterInputStream(zipIn) {
									public void close() throws IOException {
										zipIn.closeEntry();
									}
								});
								objects.forEach(o -> toCreate.add(ParaObjectUtils.setAnnotatedFields(o)));
								if (toCreate.size() >= CONF.importBatchSize()) {
									pc.createAll(toCreate);
									toCreate.clear();
								}
								si.addProperty("count", ((int) si.getProperty("count")) + objects.size());
							} else {
								logger.error("Expected JSON but found unknown file type to import: {}", zipEntry.getName());
							}
							if (Utils.timestamp() > countUpdated + TimeUnit.SECONDS.toMillis(5)) {
								pc.update(si);
								countUpdated = Utils.timestamp();
							}
						}
						if (!toCreate.isEmpty()) {
							pc.createAll(toCreate);
						}
						if (isso) {
							// apply additional fixes to data
							updateSOCommentAuthors(comments2authors);
							updateSOUserAccounts(accounts2emails);
						}
					}
				} else if (StringUtils.endsWithIgnoreCase(filename, ".json")) {
					List<Map<String, Object>> objects = reader.readValue(inputStream);
					List<ParaObject> toCreate = new LinkedList<ParaObject>();
					objects.forEach(o -> toCreate.add(ParaObjectUtils.setAnnotatedFields(o)));
					si.addProperty("count", objects.size());
					pc.createAll(toCreate);
				}
				logger.info("Imported {} objects to {}. Executed by {}", si.getProperty("count"),
						CONF.paraAccessKey(), authUser.getCreatorid() + " " + authUser.getName());
				si.addProperty("status", "done");
			} catch (Exception e) {
				logger.error("Failed to import " + filename, e);
				si.addProperty("status", "failed");
			} finally {
				pc.update(si);
			}
		});
		return "redirect:" + ADMINLINK + "?success=true&imported=1#backup-tab";
	}

	@PostMapping("/set-theme")
	public String setTheme(@RequestParam String theme, @RequestParam String css, HttpServletRequest req) {
		Profile authUser = utils.getAuthUser(req);
		if (utils.isAdmin(authUser)) {
			utils.setCustomTheme(Utils.stripAndTrim(theme, "", true), css);
		}
		return "redirect:" + ADMINLINK + "#themes-tab";
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

	@PostMapping("/reindex")
	public String reindex(HttpServletRequest req, Model model) {
		if (utils.isAdmin(utils.getAuthUser(req))) {
			Para.asyncExecute(() -> pc.rebuildIndex());
			logger.info("Started rebuilding the search index for '{}'...", CONF.paraAccessKey());
		}
		return "redirect:" + ADMINLINK;
	}

	private List<ParaObject> importFromSOArchive(ZipInputStream zipIn, ZipEntry zipEntry, ObjectReader mapReader,
			Map<String, String> comments2authors, Map<String, User> accounts2emails, Sysprop si)
			throws IOException, ParseException {
		if (zipEntry.getName().endsWith(".json")) {
			List<Map<String, Object>> objs = mapReader.readValue(new FilterInputStream(zipIn) {
				public void close() throws IOException {
					zipIn.closeEntry();
				}
			});
			List<ParaObject> toImport = new LinkedList<>();
			switch (zipEntry.getName()) {
				case "posts.json":
					importPostsFromSO(objs, toImport, si);
					break;
				case "tags.json":
					importTagsFromSO(objs, toImport, si);
					break;
				case "comments.json":
					importCommentsFromSO(objs, toImport, comments2authors, si);
					break;
				case "users.json":
					importUsersFromSO(objs, toImport, accounts2emails, si);
					break;
				case "users2badges.json":
					// nice to have...
					break;
				case "accounts.json":
					importAccountsFromSO(objs, accounts2emails);
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

	private void importPostsFromSO(List<Map<String, Object>> objs, List<ParaObject> toImport, Sysprop si)
			throws ParseException {
		logger.info("Importing {} posts...", objs.size());
		int imported = 0;
		for (Map<String, Object> obj : objs) {
			Post p;
			if (StringUtils.equalsAnyIgnoreCase((String) obj.get("postType"), "question", "article")) {
				p = new Question();
				p.setTitle((String) obj.get("title"));
				String t = StringUtils.stripStart(StringUtils.stripEnd((String) obj.
						getOrDefault("tags", ""), "|"), "|");
				p.setTags(Arrays.asList(t.split("\\|")));
				p.setAnswercount(((Integer) obj.getOrDefault("answerCount", 0)).longValue());
				p.setViewcount(((Integer) obj.getOrDefault("viewCount", 0)).longValue());
				Integer answerId = (Integer) obj.getOrDefault("acceptedAnswerId", null);
				p.setAnswerid(answerId != null ? "post_" + answerId : null);
			} else if ("answer".equalsIgnoreCase((String) obj.get("postType"))) {
				p = new Reply();
				Integer parentId = (Integer) obj.getOrDefault("parentId", null);
				p.setParentid(parentId != null ? "post_" + parentId : null);
			} else {
				continue;
			}
			p.setId("post_" + (Integer) obj.getOrDefault("id", Utils.getNewId()));
			p.setBody((String) obj.get("bodyMarkdown"));
			p.setSpace((String) obj.getOrDefault("space", Post.DEFAULT_SPACE)); // optional
			p.setVotes((Integer) obj.getOrDefault("score", 0));
			p.setTimestamp(DateUtils.parseDate((String) obj.get("creationDate"), soDateFormat1, soDateFormat2).getTime());
			Integer creatorId = (Integer) obj.getOrDefault("ownerUserId", null);
			if (creatorId == null || creatorId == -1) {
				p.setCreatorid(utils.getSystemUser().getId());
			} else {
				p.setCreatorid(Profile.id("user_" + creatorId)); // add prefix to avoid conflicts
			}
			toImport.add(p);
			imported++;
			if (toImport.size() >= CONF.importBatchSize()) {
				pc.createAll(toImport);
				toImport.clear();
			}
		}
		if (!toImport.isEmpty()) {
			pc.createAll(toImport);
			toImport.clear();
		}
		si.addProperty("count", ((int) si.getProperty("count")) + imported);
	}

	private void importTagsFromSO(List<Map<String, Object>> objs, List<ParaObject> toImport, Sysprop si) {
		logger.info("Importing {} tags...", objs.size());
		int imported = 0;
		for (Map<String, Object> obj : objs) {
			Tag t = new Tag((String) obj.get("name"));
			t.setCount((Integer) obj.getOrDefault("count", 0));
			toImport.add(t);
			imported++;
			if (toImport.size() >= CONF.importBatchSize()) {
				pc.createAll(toImport);
				toImport.clear();
			}
		}
		if (!toImport.isEmpty()) {
			pc.createAll(toImport);
			toImport.clear();
		}
		si.addProperty("count", ((int) si.getProperty("count")) + imported);
	}

	private void importCommentsFromSO(List<Map<String, Object>> objs, List<ParaObject> toImport,
			Map<String, String> comments2authors, Sysprop si) throws ParseException {
		logger.info("Importing {} comments...", objs.size());
		int imported = 0;
		for (Map<String, Object> obj : objs) {
			Comment c = new Comment();
			c.setId("comment_" + (Integer) obj.get("id"));
			c.setComment((String) obj.get("text"));
			Integer parentId = (Integer) obj.getOrDefault("postId", null);
			c.setParentid(parentId != null ? "post_" + parentId : null);
			c.setTimestamp(DateUtils.parseDate((String) obj.get("creationDate"), soDateFormat1, soDateFormat2).getTime());
			Integer creatorId = (Integer) obj.getOrDefault("userId", null);
			String userid = "user_" + creatorId;
			c.setCreatorid(creatorId != null ? Profile.id(userid) : utils.getSystemUser().getId());
			comments2authors.put(c.getId(), userid);
			toImport.add(c);
			imported++;
			if (toImport.size() >= CONF.importBatchSize()) {
				pc.createAll(toImport);
				toImport.clear();
			}
		}
		if (!toImport.isEmpty()) {
			pc.createAll(toImport);
			toImport.clear();
		}
		si.addProperty("count", ((int) si.getProperty("count")) + imported);
	}

	@SuppressWarnings("unchecked")
	private void importUsersFromSO(List<Map<String, Object>> objs, List<ParaObject> toImport,
			Map<String, User> accounts2emails, Sysprop si) throws ParseException {
		logger.info("Importing {} users...", objs.size());
		int imported = 0;
		for (Map<String, Object> obj : objs) {
			User u = new User();
			u.setId("user_" + (Integer) obj.get("id"));
			u.setTimestamp(DateUtils.parseDate((String) obj.get("creationDate"), soDateFormat1, soDateFormat2).getTime());
			u.setActive(true);
			u.setCreatorid(((Integer) obj.get("accountId")).toString());
			u.setGroups("admin".equalsIgnoreCase((String) obj.get("userTypeId"))
					? User.Groups.ADMINS.toString() :
					("mod".equalsIgnoreCase((String) obj.get("userTypeId")) ?
							User.Groups.MODS.toString() : User.Groups.USERS.toString()));
			u.setEmail(u.getId() + "@scoold.com");
			u.setIdentifier(u.getEmail());
			u.setName((String) obj.get("realName"));
			String lastLogin = (String) obj.get("lastLoginDate");
			u.setUpdated(StringUtils.isBlank(lastLogin) ? null :
					DateUtils.parseDate(lastLogin, soDateFormat1, soDateFormat2).getTime());
			u.setPicture((String) obj.get("profileImageUrl"));

			Sysprop s = new Sysprop();
			s.setId(u.getIdentifier());
			s.setName(Config._IDENTIFIER);
			s.setCreatorid(u.getId());
			String password = (String) obj.getOrDefault("passwordHash", Utils.bcrypt(Utils.generateSecurityToken(10)));
			if (!StringUtils.isBlank(password)) {
				s.addProperty(Config._PASSWORD, password);
				u.setPassword(password);
			}

			Profile p = Profile.fromUser(u);
			p.setVotes((Integer) obj.get("reputation"));
			p.setAboutme((String) obj.getOrDefault("title", ""));
			p.setLastseen(u.getUpdated());
			p.setSpaces(new HashSet<String>((List<String>) obj.getOrDefault("spaces", List.of(Post.DEFAULT_SPACE))));
			toImport.add(u);
			toImport.add(p);
			toImport.add(s);
			imported += 2;

			User cachedUser = accounts2emails.get(u.getCreatorid());
			if (cachedUser == null) {
				User cu = new User(u.getId());
				accounts2emails.put(u.getCreatorid(), cu);
			} else {
				cachedUser.setId(u.getId());
			}

			if (toImport.size() >= CONF.importBatchSize()) {
				pc.createAll(toImport);
				toImport.clear();
			}
		}
		if (!toImport.isEmpty()) {
			pc.createAll(toImport);
			toImport.clear();
		}
		si.addProperty("count", ((int) si.getProperty("count")) + imported);
	}

	private void importAccountsFromSO(List<Map<String, Object>> objs, Map<String, User> accounts2emails) {
		logger.info("Importing {} accounts...", objs.size());
		for (Map<String, Object> obj : objs) {
			String accountId = ((Integer) obj.get("accountId")).toString();
			String email = (String) obj.get("verifiedEmail");
			User cachedUser = accounts2emails.get(accountId);
			if (cachedUser == null) {
				User cu = new User();
				cu.setEmail(email);
				cu.setIdentifier(email);
				accounts2emails.put(accountId, cu);
			} else {
				cachedUser.setEmail(email);
				cachedUser.setIdentifier(email);
			}
		}
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
				if (toPatch.size() >= CONF.importBatchSize()) {
					pc.invokePatch("_batch", toPatch, Map.class);
					toPatch.clear();
				}
			}
			if (!toPatch.isEmpty()) {
				pc.invokePatch("_batch", toPatch, Map.class);
				toPatch.clear();
			}
		}
	}

	private void updateSOUserAccounts(Map<String, User> accounts2emails) {
		List<Map<String, String>> toPatch = new LinkedList<>();
		for (Map.Entry<String, User> entry : accounts2emails.entrySet()) {
			User u = entry.getValue();
			Map<String, String> user = new HashMap<>();
			user.put(Config._ID, u.getId());
			user.put(Config._EMAIL, u.getEmail());
			user.put(Config._IDENTIFIER, u.getEmail());
			toPatch.add(user);
			if (toPatch.size() >= CONF.importBatchSize()) {
				pc.invokePatch("_batch", toPatch, Map.class);
				toPatch.clear();
			}
		}
		if (!toPatch.isEmpty()) {
			pc.invokePatch("_batch", toPatch, Map.class);
			toPatch.clear();
		}
	}

	private List<Sysprop> getSpaces(Pager itemcount) {
		Set<Sysprop> spaces = utils.getAllSpaces();
		itemcount.setCount(spaces.size());
		LinkedList<Sysprop> list = new LinkedList<>(spaces.stream().
				filter(s -> !utils.isDefaultSpace(s.getName())).collect(Collectors.toList()));
		if (itemcount.getPage() <= 1) {
			list.addFirst(utils.buildSpaceObject("default"));
		}
		return list;
	}
}
