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
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.User;
import com.erudika.para.core.utils.Config;
import com.erudika.para.core.utils.Utils;
import com.erudika.scoold.ScooldConfig;
import static com.erudika.scoold.ScooldServer.SETTINGSLINK;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.HttpUtils;
import com.erudika.scoold.utils.ScooldUtils;
import com.erudika.scoold.utils.avatars.AvatarRepository;
import com.erudika.scoold.utils.avatars.AvatarRepositoryProxy;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/settings")
public class SettingsController {

	private final ScooldUtils utils;
	private final ParaClient pc;
	private final AvatarRepository avatarRepository;
	private static final ScooldConfig CONF = ScooldUtils.getConfig();

	@Inject
	public SettingsController(ScooldUtils utils, AvatarRepositoryProxy avatarRepository) {
		this.utils = utils;
		this.pc = utils.getParaClient();
		this.avatarRepository = avatarRepository;
	}

	@GetMapping
	public String get(HttpServletRequest req, Model model) {
		if (!utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + SETTINGSLINK;
		}
		model.addAttribute("path", "settings.vm");
		model.addAttribute("title", utils.getLang(req).get("settings.title"));
		model.addAttribute("newpostEmailsEnabled", utils.isSubscribedToNewPosts(req));
		model.addAttribute("newreplyEmailsEnabled", utils.isSubscribedToNewReplies(req));
		model.addAttribute("emailsAllowed", utils.isNotificationsAllowed());
		model.addAttribute("newpostEmailsAllowed", utils.isNewPostNotificationAllowed());
		model.addAttribute("favtagsEmailsAllowed", utils.isFavTagsNotificationAllowed());
		model.addAttribute("replyEmailsAllowed", utils.isReplyNotificationAllowed());
		model.addAttribute("commentEmailsAllowed", utils.isCommentNotificationAllowed());
		model.addAttribute("includeGMapsScripts", utils.isNearMeFeatureEnabled());
		return "base";
	}

	@PostMapping
	public String post(@RequestParam(required = false) String tags, @RequestParam(required = false) String latlng,
			@RequestParam(required = false) String replyEmailsOn, @RequestParam(required = false) String commentEmailsOn,
			@RequestParam(required = false) String oldpassword, @RequestParam(required = false) String newpassword,
			@RequestParam(required = false) String newpostEmailsOn, @RequestParam(required = false) String favtagsEmailsOn,
			@RequestParam(required = false) List<String> favspaces, @RequestParam(required = false) String newreplyEmailsOn,
			HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			Profile authUser = utils.getAuthUser(req);
			setFavTags(authUser, tags);
			setFavSpaces(authUser, favspaces);
			if (!StringUtils.isBlank(latlng)) {
				authUser.setLatlng(latlng);
			}
			setAnonymity(authUser, req.getParameter("anon"));
			setDarkMode(authUser, req.getParameter("dark"));
			authUser.setReplyEmailsEnabled(Boolean.valueOf(replyEmailsOn) && utils.isReplyNotificationAllowed());
			authUser.setCommentEmailsEnabled(Boolean.valueOf(commentEmailsOn) && utils.isCommentNotificationAllowed());
			authUser.setFavtagsEmailsEnabled(Boolean.valueOf(favtagsEmailsOn) && utils.isFavTagsNotificationAllowed());
			authUser.update();

			if (Boolean.valueOf(newpostEmailsOn) && utils.isNewPostNotificationAllowed()) {
				utils.subscribeToNewPosts(authUser.getUser());
			} else {
				utils.unsubscribeFromNewPosts(authUser.getUser());
			}
			if ("on".equals(newreplyEmailsOn) && utils.isReplyNotificationAllowed() && utils.isMod(authUser)) {
				utils.subscribeToNewReplies(authUser.getUser());
			} else {
				utils.unsubscribeFromNewReplies(authUser.getUser());
			}

			if (resetPasswordAndUpdate(authUser.getUser(), oldpassword, newpassword)) {
				utils.clearSession(req, res);
				return "redirect:" + SETTINGSLINK + "?passChanged=true";
			}
		}
		return "redirect:" + SETTINGSLINK;
	}

	@PostMapping("/goodbye")
	public String deleteAccount(HttpServletRequest req, HttpServletResponse res) {
		if (utils.isAuthenticated(req)) {
			utils.getAuthUser(req).delete();
			utils.clearSession(req, res);
		}
		return "redirect:" + CONF.signoutUrl(4);
	}

	@PostMapping("/toggle-twofa")
	public String toggle2FA(@RequestParam String code, @RequestParam(required = false, defaultValue = "") String backupCode,
			HttpServletRequest req, HttpServletResponse res, Model model) {
		if (utils.isAuthenticated(req)) {
			String jwt = HttpUtils.getStateParam(CONF.authCookie(), req);
			User user = pc.me(jwt);
			if (user != null && (!StringUtils.isBlank(code) || !StringUtils.isBlank(backupCode))) {
				if (utils.isValid2FACode(user.getTwoFAkey(), NumberUtils.toInt(code, 0), 0) ||
						Utils.bcryptMatches(backupCode, user.getTwoFAbackupKeyHash())) {
					user.setTwoFA(!user.getTwoFA());
					Date issueTime = utils.getUnverifiedClaimsFromJWT(jwt).getIssueTime();
					if (user.getTwoFA()) {
						String backup = Utils.generateSecurityToken(20, true);
						user.setTwoFAbackupKeyHash(Utils.bcrypt(backup));
						model.addAttribute("backupCode", backup);
						HttpUtils.set2FACookie(user, issueTime, req, res);
					} else {
						user.setTwoFAkey("");
						user.setTwoFAbackupKeyHash("");
						HttpUtils.set2FACookie(null, null, req, res);
					}
					pc.update(user);
					utils.getAuthUser(req).setUser(user);
					return get(req, model);
				}
				return "redirect:" + SETTINGSLINK + "?code=signin.invalidcode&error=true";
			}
		}
		return "redirect:" + SETTINGSLINK;
	}

	@PostMapping("/reset-2fa")
	public String reset2FA(HttpServletRequest req, HttpServletResponse res) {
		if (!utils.isAuthenticated(req)) {
			return null;
		}
		return "redirect:" + SETTINGSLINK;
	}

	@GetMapping(path = "/qr", produces = "image/png")
	public void generate2FAQRCode(HttpServletRequest req, HttpServletResponse res) {
		if (!utils.isAuthenticated(req)) {
			return;
		}
		String jwt = HttpUtils.getStateParam(CONF.authCookie(), req);
		User user = pc.me(jwt);
		if (user == null) {
			return;
		}
		if (StringUtils.isBlank(user.getTwoFAkey())) {
			user.setTwoFAkey(Utils.generateSecurityToken(32, true));
			pc.update(user);
		}
		String otpProtocol = Utils.formatMessage("otpauth://totp/" + CONF.appName() + ":{0}?secret={1}&issuer=Scoold",
				user.getEmail(), new Base32().encodeAsString(user.getTwoFAkey().
				replaceAll("=", "").getBytes()).replaceAll("=", ""));

		QRCodeWriter writer = new QRCodeWriter();
		final BitMatrix matrix;
		try {
			matrix = writer.encode(otpProtocol, BarcodeFormat.QR_CODE, 300, 300);
			StreamingOutput qrCode = new StreamingOutput() {
				@Override
				public void write(OutputStream os) throws IOException, WebApplicationException {
					MatrixToImageWriter.writeToStream(matrix, "PNG", os);
					os.flush();
				}
			};
			res.setContentType("image/png");
			qrCode.write(res.getOutputStream());
		} catch (Exception ex) {
			return;
		}
	}

	private boolean resetPasswordAndUpdate(User u, String pass, String newpass) {
		if (u != null && !StringUtils.isBlank(pass) && !StringUtils.isBlank(newpass) &&
				u.getIdentityProvider().equals("generic")) {
			Sysprop s = pc.read(u.getEmail());
			if (s != null && Utils.bcryptMatches(pass, (String) s.getProperty(Config._PASSWORD))) {
				String hashed = Utils.bcrypt(newpass);
				s.addProperty(Config._PASSWORD, hashed);
				u.setPassword(hashed);
				pc.update(s);
				return true;
			}
		}
		return false;
	}

	private void setFavTags(Profile authUser, String tags) {
		if (!StringUtils.isBlank(tags)) {
			Set<String> ts = new LinkedHashSet<String>();
			for (String tag : tags.split(",")) {
				if (!StringUtils.isBlank(tag) && ts.size() <= CONF.maxFavoriteTags()) {
					ts.add(tag);
				}
			}
			authUser.setFavtags(new LinkedList<String>(ts));
		} else {
			authUser.setFavtags(null);
		}
	}

	private void setFavSpaces(Profile authUser, List<String> spaces) {
		authUser.setFavspaces(null);
		if (spaces != null && !spaces.isEmpty()) {
			for (String space : spaces) {
				String spaceId = utils.getSpaceId(space);
				if (!StringUtils.isBlank(spaceId) && utils.canAccessSpace(authUser, spaceId)) {
					authUser.getFavspaces().add(spaceId);
				}
			}
		}
	}

	private void setAnonymity(Profile authUser, String anonParam) {
		if (utils.isAnonymityEnabled()) {
			if ("true".equalsIgnoreCase(anonParam)) {
				anonymizeProfile(authUser);
			} else if (authUser.getAnonymityEnabled()) {
				deanonymizeProfile(authUser);
			}
		}
	}

	private void setDarkMode(Profile authUser, String darkParam) {
		if (utils.isDarkModeEnabled()) {
			authUser.setDarkmodeEnabled("true".equalsIgnoreCase(darkParam));
			pc.update(authUser);
		}
	}

	private void anonymizeProfile(Profile authUser) {
		authUser.setName("Anonymous");
		authUser.setOriginalPicture(authUser.getPicture());
		authUser.setPicture(avatarRepository.getAnonymizedLink(authUser.getId() + "@scooldemail.com"));
		authUser.setAnonymityEnabled(true);
	}

	private void deanonymizeProfile(Profile authUser) {
		authUser.setName(authUser.getOriginalName());
		authUser.setPicture(authUser.getOriginalPicture());
		authUser.setAnonymityEnabled(false);
	}
}
