package com.erudika.scoold.controllers;

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

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Votable;
import com.erudika.para.core.Vote;
import com.erudika.para.utils.Config;
import static com.erudika.scoold.ScooldServer.ANSWER_VOTEUP_REWARD_AUTHOR;
import static com.erudika.scoold.ScooldServer.CRITIC_IFHAS;
import static com.erudika.scoold.ScooldServer.GOODANSWER_IFHAS;
import static com.erudika.scoold.ScooldServer.GOODQUESTION_IFHAS;
import static com.erudika.scoold.ScooldServer.POST_VOTEDOWN_PENALTY_AUTHOR;
import static com.erudika.scoold.ScooldServer.POST_VOTEDOWN_PENALTY_VOTER;
import static com.erudika.scoold.ScooldServer.QUESTION_VOTEUP_REWARD_AUTHOR;
import static com.erudika.scoold.ScooldServer.SUPPORTER_IFHAS;
import static com.erudika.scoold.ScooldServer.VOTER_IFHAS;
import static com.erudika.scoold.ScooldServer.VOTEUP_REWARD_AUTHOR;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import static com.erudika.scoold.core.Profile.Badge.CRITIC;
import static com.erudika.scoold.core.Profile.Badge.GOODANSWER;
import static com.erudika.scoold.core.Profile.Badge.GOODQUESTION;
import static com.erudika.scoold.core.Profile.Badge.SUPPORTER;
import static com.erudika.scoold.core.Profile.Badge.VOTER;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.utils.ScooldUtils;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class VoteController {

	private static final Logger logger = LoggerFactory.getLogger(VoteController.class);

	private final ScooldUtils utils;
	private final ParaClient pc;
	private final String expiresAfter;
	private final String lockedAfter;
	private final Integer expiresAfterSec;
	private final Integer lockedAfterSec;

	@Inject
	public VoteController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
		expiresAfter = Config.getConfigParam("vote_expires_after_sec", null);
		lockedAfter = Config.getConfigParam("vote_expires_after_sec", null);
		expiresAfterSec = NumberUtils.toInt(expiresAfter, Config.VOTE_EXPIRES_AFTER_SEC);
		lockedAfterSec = NumberUtils.toInt(lockedAfter, Config.VOTE_LOCKED_AFTER_SEC);
	}

	@ResponseBody
	@GetMapping("/voteup/{type}/{id}")
	public Boolean voteup(@PathVariable String type, @PathVariable String id, HttpServletRequest req) {
		//addModel("voteresult", result);
		return processVoteRequest(true, type, id, req);
	}

	@ResponseBody
	@GetMapping("/votedown/{type}/{id}")
	public Boolean votedown(@PathVariable String type, @PathVariable String id, HttpServletRequest req) {
		//addModel("voteresult", result);
		return processVoteRequest(false, type, id, req);
	}

	boolean processVoteRequest(boolean isUpvote, String type, String id, HttpServletRequest req) {
		ParaObject votable = pc.read(type, id);
		Profile author = null;
		Profile authUser = utils.getAuthUser(req);
		boolean result = false;
		boolean update = false;
		if (votable == null || authUser == null) {
			return false;
		}

		try {
			List<ParaObject> voteObjects = pc.readAll(Arrays.asList(votable.getCreatorid(),
					new Vote(authUser.getId(), id, Votable.VoteValue.UP).getId(),
					new Vote(authUser.getId(), id, Votable.VoteValue.DOWN).getId()));

			author = (Profile) voteObjects.stream().filter((p) -> p instanceof Profile).findFirst().orElse(null);
			Integer votes = votable.getVotes() != null ? votable.getVotes() : 0;
			boolean upvoteExists = voteObjects.stream().anyMatch((v) -> v instanceof Vote && ((Vote) v).isUpvote());
			boolean downvoteExists = voteObjects.stream().anyMatch((v) -> v instanceof Vote && ((Vote) v).isDownvote());
			boolean isVoteCorrection = (isUpvote && downvoteExists) || (!isUpvote && upvoteExists);

			if (isUpvote && voteUp(votable, authUser.getId())) {
				votes++;
				result = true;
				update = updateReputationOnUpvote(votable, votes, authUser, author, isVoteCorrection);
			} else if (!isUpvote && voteDown(votable, authUser.getId())) {
				votes--;
				result = true;
				hideCommentAndReport(votable, votes, id, req);
				update = updateReputationOnDownvote(votable, votes, authUser, author, isVoteCorrection);
			}
		} catch (Exception ex) {
			logger.error(null, ex);
			result = false;
		}
		utils.addBadgeOnce(authUser, SUPPORTER, authUser.getUpvotes() >= SUPPORTER_IFHAS);
		utils.addBadgeOnce(authUser, CRITIC, authUser.getDownvotes() >= CRITIC_IFHAS);
		utils.addBadgeOnce(authUser, VOTER, authUser.getTotalVotes() >= VOTER_IFHAS);

		if (update) {
			pc.updateAll(Arrays.asList(author, authUser));
		}
		return result;
	}

	private boolean updateReputationOnUpvote(ParaObject votable, Integer votes,
			Profile authUser, Profile author, boolean isVoteCorrection) {
		if (author != null) {
			if (!isVoteCorrection) {
				author.addRep(addReward(votable, author, votes));
				authUser.incrementUpvotes();
				return true;
			}
		}
		return false;
	}

	private boolean updateReputationOnDownvote(ParaObject votable, Integer votes,
			Profile authUser, Profile author, boolean isVoteCorrection) {
		if (author != null) {
			if (isVoteCorrection) {
				author.removeRep(addReward(votable, author, votes));
			} else {
				authUser.incrementDownvotes();
			}
			author.removeRep(POST_VOTEDOWN_PENALTY_AUTHOR);
			//small penalty to voter
			authUser.removeRep(POST_VOTEDOWN_PENALTY_VOTER);
			return true;
		}
		return false;
	}

	private int addReward(Votable votable, Profile author, int votes) {
		int reward;
		if (votable instanceof Post) {
			Post p = (Post) votable;
			if (p.isReply()) {
				utils.addBadge(author, GOODANSWER, votes >= GOODANSWER_IFHAS, false);
				reward = ANSWER_VOTEUP_REWARD_AUTHOR;
			} else if (p.isQuestion()) {
				utils.addBadge(author, GOODQUESTION, votes >= GOODQUESTION_IFHAS, false);
				reward = QUESTION_VOTEUP_REWARD_AUTHOR;
			} else {
				reward = VOTEUP_REWARD_AUTHOR;
			}
		} else {
			reward = VOTEUP_REWARD_AUTHOR;
		}
		return reward;
	}

	private void hideCommentAndReport(Votable votable, int votes, String id, HttpServletRequest req) {
		if (votable instanceof Comment && votes <= -5) {
			//treat comment as offensive or spam - hide
			((Comment) votable).setHidden(true);
		} else if (votable instanceof Post && votes <= -5) {
			Post p = (Post) votable;
			//mark post for closing
			Report rep = new Report();
			rep.setParentid(id);
			rep.setLink(p.getPostLink(false, false));
			rep.setDescription(utils.getLang(req).get("posts.forclosing"));
			rep.setSubType(Report.ReportType.OTHER);
			rep.setAuthorName("System");
			rep.create();
		}
	}

	private boolean voteUp(ParaObject votable, String userid) {
		if (StringUtils.isBlank(expiresAfter) && StringUtils.isBlank(lockedAfter)) {
			return pc.voteUp(votable, userid);
		}
		return pc.voteUp(votable, userid, expiresAfterSec, lockedAfterSec);
	}

	private boolean voteDown(ParaObject votable, String userid) {
		if (StringUtils.isBlank(expiresAfter) && StringUtils.isBlank(lockedAfter)) {
			return pc.voteDown(votable, userid);
		}
		return pc.voteDown(votable, userid, expiresAfterSec, lockedAfterSec);
	}
}
