package com.erudika.scoold.controllers;

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

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Votable;
import com.erudika.para.core.Vote;
import com.erudika.scoold.ScooldConfig;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
public class VoteController {

	private static final Logger logger = LoggerFactory.getLogger(VoteController.class);
	private static final ScooldConfig CONF = ScooldUtils.getConfig();

	private final ScooldUtils utils;
	private final ParaClient pc;
	private final Integer expiresAfterSec;
	private final Integer lockedAfterSec;

	@Inject
	public VoteController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
		expiresAfterSec = CONF.voteExpiresAfterSec();
		lockedAfterSec = CONF.voteLockedAfterSec();
	}

	@ResponseBody
	@PostMapping("/voteup/{type}/{id}")
	public Boolean voteup(@PathVariable String type, @PathVariable String id, HttpServletRequest req) {
		ParaObject votable = StringUtils.isBlank(type) ? pc.read(id) : pc.read(type, id);
		return processVoteRequest(true, votable, req);
	}

	@ResponseBody
	@PostMapping("/votedown/{type}/{id}")
	public Boolean votedown(@PathVariable String type, @PathVariable String id, HttpServletRequest req) {
		if (!CONF.downvotesEnabled()) {
			return false;
		}
		ParaObject votable = StringUtils.isBlank(type) ? pc.read(id) : pc.read(type, id);
		return processVoteRequest(false, votable, req);
	}

	boolean processVoteRequest(boolean isUpvote, ParaObject votable, HttpServletRequest req) {
		Profile author = null;
		Profile authUser = utils.getAuthUser(req);
		boolean result = false;
		boolean update = false;
		if (votable == null || authUser == null) {
			return false;
		}

		try {
			List<ParaObject> voteObjects = pc.readAll(Arrays.asList(votable.getCreatorid(),
					new Vote(authUser.getId(), votable.getId(), Votable.VoteValue.UP).getId(),
					new Vote(authUser.getId(), votable.getId(), Votable.VoteValue.DOWN).getId()));

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
				hideCommentAndReport(votable, votes, votable.getId(), req);
				update = updateReputationOnDownvote(votable, votes, authUser, author, isVoteCorrection);
			}
		} catch (Exception ex) {
			logger.error(null, ex);
			result = false;
		}
		utils.addBadgeOnce(authUser, SUPPORTER, authUser.getUpvotes() >= CONF.supporterIfHasRep());
		utils.addBadgeOnce(authUser, CRITIC, authUser.getDownvotes() >= CONF.criticIfHasRep());
		utils.addBadgeOnce(authUser, VOTER, authUser.getTotalVotes() >= CONF.voterIfHasRep());

		if (update) {
			pc.updateAll(Arrays.asList(author, authUser));
		}
		return result;
	}

	private boolean updateReputationOnUpvote(ParaObject votable, Integer votes,
			Profile authUser, Profile author, boolean isVoteCorrection) {
		if (author != null) {
			if (isVoteCorrection) {
				author.addRep(CONF.postVotedownPenaltyAuthor()); // revert penalty to author
				authUser.addRep(CONF.postVotedownPenaltyVoter()); // revert penalty to voter
				authUser.decrementDownvotes();
			} else {
				author.addRep(addReward(votable, author, votes));
				authUser.incrementUpvotes();
			}
			return true;
		}
		return false;
	}

	private boolean updateReputationOnDownvote(ParaObject votable, Integer votes,
			Profile authUser, Profile author, boolean isVoteCorrection) {
		if (author != null) {
			if (isVoteCorrection) {
				author.removeRep(addReward(votable, author, votes));
				authUser.decrementUpvotes();
			} else {
				author.removeRep(CONF.postVotedownPenaltyAuthor()); // small penalty to author
				authUser.removeRep(CONF.postVotedownPenaltyVoter()); // small penalty to voter
				authUser.incrementDownvotes();
			}
			return true;
		}
		return false;
	}

	private int addReward(Votable votable, Profile author, int votes) {
		int reward;
		if (votable instanceof Post) {
			Post p = (Post) votable;
			if (p.isReply()) {
				utils.addBadge(author, GOODANSWER, votes >= CONF.goodAnswerIfHasRep(), false);
				reward = CONF.answerVoteupRewardAuthor();
			} else if (p.isQuestion()) {
				utils.addBadge(author, GOODQUESTION, votes >= CONF.goodQuestionIfHasRep(), false);
				reward = CONF.questionVoteupRewardAuthor();
			} else {
				reward = CONF.voteupRewardAuthor();
			}
		} else {
			reward = CONF.voteupRewardAuthor();
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
		return pc.voteUp(votable, userid, expiresAfterSec, lockedAfterSec);
	}

	private boolean voteDown(ParaObject votable, String userid) {
		return pc.voteDown(votable, userid, expiresAfterSec, lockedAfterSec);
	}
}
