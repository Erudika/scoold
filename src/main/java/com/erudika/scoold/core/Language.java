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

package com.erudika.scoold.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
public final class Language {

	public static final Map<String, String> ENGLISH = Collections.unmodifiableMap(new LinkedHashMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			put("home.title", "Home");
			put("signin.title", "Sign in");
			put("about.title", "About");
			put("search.title", "Search");
			put("profile.title", "Profile");
			put("privacy.title", "Privacy policy");
			put("terms.title", "Terms of use");
			put("faq.title", "FAQ");
			put("bugs.title", "Bugs");
			put("contact.title", "Contact us");
			put("error.title", "We'll fix this... eventually.");
			put("notfound.title", "Page not found.");
			put("settings.title", "Settings");
			put("translate.title", "Translations");
			put("contacts.title", "Contacts");
			put("messages.title", "Messages");
			put("message.title", "Message");
			put("suggest.title", "Suggestions");
			put("people.title", "Users");
			put("comments.title", "Comments");
			put("comment.title", "Comment");
			put("reports.title", "Reports");
			put("tags.title", "Tags");
			put("revisions.title", "Revisions");
			put("questions.title", "Questions");
			put("answers.title", "Answers");
			put("badges.title", "Badges");
			put("feedback.title", "Feedback");
			put("accounts.title", "Accounts");
			put("voterep.title", "Voting and Reputation");

			put("hi", "Hi");
			put("hello", "Hello!");
			put("confirm", "Confirm");
			put("cancel", "Cancel");
			put("yes", "Yes");
			put("no", "No");
			put("send", "Send");
			put("delete", "Delete");
			put("deleteall", "Delete all");
			put("undelete", "Undelete");
			put("clear", "Clear");
			put("continue", "Continue");
			put("save", "Save");
			put("back", "Back");
			put("done", "Done!");
			put("remove", "Remove");
			put("close", "Close");
			put("click2close", "Click to close");
			put("dontshow", "Don't show");
			put("clickedit", "Click to edit");
			put("edit", "Edit");
			put("change", "Change");
			put("post", "Go");
			put("month", "Month");
			put("day", "Day");
			put("year", "Year");
			put("chooseone", "Choose one");
			put("from", "From");
			put("to", "To");
			put("date", "date");
			put("optional", "optional");
			put("add", "Add");
			put("results", "Results");
			put("on", "on");
			put("off", "off");
			put("male", "Male");
			put("female", "Female");
			put("name", "Name");
			put("description", "Description");
			put("error", "Error");
			put("unknown", "Unknown");
			put("restore", "Restore");
			put("original", "Original");
			put("admin", "Administrator");
			put("mod", "Moderator");
			put("details", "Details");
			put("newest", "Newest");
			put("votes", "Votes");
			put("anonymous", "Anonymous");
			put("active", "Active");
			put("inactive", "Inactive");
			put("reopen", "Reopen");
			put("open", "Open");
			put("epicfail", "Epic fail!");
			put("join", "Join");
			put("leave", "Leave");
			put(Profile.Badge.NICEPROFILE.toString(), "Nice Profile");
			put(Profile.Badge.TESTER.toString(), "Lab Rat");
			put(Profile.Badge.REPORTER.toString(), "Reporter");
			put(Profile.Badge.VOTER.toString(), "Voter");
			put(Profile.Badge.COMMENTATOR.toString(), "Commentator");
			put(Profile.Badge.CRITIC.toString(), "Critic");
			put(Profile.Badge.SUPPORTER.toString(), "Supporter");
			put(Profile.Badge.EDITOR.toString(), "Editor");
			put(Profile.Badge.BACKINTIME.toString(), "Back in Time");
//			put(Profile.Badge.VETERAN.toString(), "Veteran");
			put(Profile.Badge.ENTHUSIAST.toString(), "Enthusiast");
			put(Profile.Badge.FRESHMAN.toString(), "Freshman");
			put(Profile.Badge.SCHOLAR.toString(), "Scholar");
			put(Profile.Badge.TEACHER.toString(), "Teacher");
			put(Profile.Badge.PROFESSOR.toString(), "Professor");
			put(Profile.Badge.GEEK.toString(), "Geek");
			put(Profile.Badge.GOODQUESTION.toString(), "Good Question");
			put(Profile.Badge.GOODANSWER.toString(), "Good Answer");
			put(Profile.Badge.EUREKA.toString(), "Eureka!");
			put(Profile.Badge.SENIOR.toString(), "Senior");
			put(Profile.Badge.NOOB.toString(), "Newbie");
			put(Profile.Badge.DISCIPLINED.toString(), "Disciplined");
			put(Profile.Badge.POLYGLOT.toString(), "Polyglot");
			put("timeago", "{0} ago");
			put("links", "Links");
			put("signout", "Sign out");
			put("lang", "Language");
			put("addfriend", "Add to contacts");
			put("delfriend", "Remove from contacts");
			put("sendmsg", "Send a message");
			put("report", "Report");
			put("reportproblem", "Report a problem");
			put("nojavascript", "JavaScript is disabled. Strange things can happen.");
			put("sessiontimeout", "Your session has expired.");
			put("areyousure", "Are you sure you want to do this?");
			put("identifier", "Identifier");
			put("prev", "Previous");
			put("next", "Next");
			put("showall", "Show all");
			put("preview", "Preview");
			put("goback", "Go back");
			put("minlength", "At least {0} characters are required.");
			put("maxlength", "Too long. {0} characters max.");
			put("requiredfield", "This field is required and cannot be empty.");
			put("more", "More");
			put("addmore", "Add more");
			put("newbadges", "You've got new badges!");
			put("newbadge", "You've got a new badge!");
			put("reputation", "Reputation");
			put("writehere", "Write here...");
			put("website", "Website");
			put("pagenotfound", "This page cannot be found.");
			put("forbidden", "You shall not pass!");
			put("servererror", "Critical server error.");
			put("sitedown", "We're having a break. We'll be back soon.");
			put("maintenance", "We're doing some maintenance.");
			put("license", "Excluding logos, this site is licensed under a {0} license.");
			put("badrequest", "Bad request.");
			put("create", "Create");
			put("pause", "Pause");
			put("play", "Play");
			put("address", "Address");
			put("loading", "Loading...");
			put("map", "Map");
			put("points", "points");
			put("invalidyear", "Invalid year");
			put("learnmore", "Learn more");
			put("posts", "Posts");

			put("humantime.s", "{0} seconds ago");
			put("humantime.m", "{0} minutes ago");
			put("humantime.h", "{0} hours ago");
			put("humantime.d", "{0} days ago");
			put("humantime.n", "{0} months ago");
			put("humantime.y", "{0} years ago");

			put("signup.form.myname", "My name is");
			put("signup.form.iama", "I am a");
			put("signup.form.email", "Email");
			put("signup.form.termsofuse", "By clicking the button above, you agree to our 'Terms of use'.");

			put("signin.facebook", "Continue with Facebook");
			put("signin.google", "Continue with Google");

			put("profile.title", "Profile");
			put("profile.changepic", "Change picture");
			put("profile.usegravatar", "Use Gravatar");
			put("profile.about.title", "About me");
			put("profile.about.dob", "Date of birth");
			put("profile.about.location", "Location");
			put("profile.about.membersince", "Active since");
			put("profile.about.lastseen", "Last activity");
			put("profile.about.aboutme", "More about me");
			put("profile.deleted", "Deleted user");
			put("profile.posts.noquestions", "No questions.");
			put("profile.posts.noanswers", "No answers.");

			put("search.notfound", "Nothing found.");
			put("search.search", "Search");
			put("search.people", "Find a person");
			put("search.questions", "Find a question");

			put("comments.write", "Add comment");
			put("comments.show", "Show comment");
			put("comments.hidden", "Hidden.");

			put("settings.location", "You can filter questions based on your location - just point where you are on the map.");
			put("settings.tagfilter.text", "You can also filter questions by specifying the tags you are interested in.");
			put("settings.delete", "Delete my account!");
			put("settings.delete.confirm", "Please confirm that you really want to delete your account.");
			put("settings.filters", "Question filters");

			put("reports."+Report.ReportType.SPAM.toString(), "Spam or commercial content");
			put("reports."+Report.ReportType.OFFENSIVE.toString(), "Offensive content, violence or abuse");
			put("reports."+Report.ReportType.DUPLICATE.toString(), "Duplicate content");
			put("reports."+Report.ReportType.INCORRECT.toString(), "Incorrect or outdated content");
			put("reports."+Report.ReportType.OTHER.toString(), "Other problems that need attention");
			put("reports.all", "All reports");
			put("reports.post", "Posts");
			put("reports.user", "Users");
			put("reports.comment", "Comments");
			put("reports.translation", "Translations");
			put("reports.category", "Report category");
			put("reports.description", "Describe what's wrong");
			put("reports.actionstaken", "Actions taken");

			put("tags.tag", "Tag");
			put("tags.info", "Tags are used as keywords for questions. Each question is marked with a few tags which describe in essence what that question is about.");
			put("tags.find", "Find a tag");
			put("tags.toomany", "Too many tags. Max {0}.");

			put("posts.question", "Question");
			put("posts.answer", "Answer");
			put("posts.posted", "Posted");
			put("posts.views", "Views");
			put("posts.answered", "Answered");
			put("posts.ask", "Ask Question");
			put("posts.title", "Title");
			put("posts.writeanswer", "Write your answer");
			put("posts.youranswer", "Your answer");
			put("posts.closed", "This question is closed.");
			put("posts.error1", "Oops! Something went wrong.");
			put("posts.marked", "Marked for deletion");
			put("posts.forclosing", "Marked for closing");
			put("posts.noquestions", "No questions found.");
			put("posts.updated", "Updated");
			put("posts.locationfilter", "Filter by location");
			put("posts.unanswered", "Unanswered");
			put("posts.mostpopular", "Most popular questions");
			put("posts.tagged", "Questions with tag");
			put("posts.unloadconfirm", "Everything you've written will be lost!");
			put("posts.editby", "Edited by");
			put("posts.filtered", "Filtered");
			put("posts.none", "No posts.");
			put("posts.new", "New post");
			put("posts.post", "Post");
			put("posts.similar", "Similar posts");

			put("revisions.revision", "Revision");
			put("revisions.current", "This is the current revision.");

			put("feedback.write", "Write something");
			put("feedback.tagged", "Feedback tagged");
			put("feedback.type", "Type of feedback");
			put("feedback."+Feedback.FeedbackType.BUG.toString(), "Bug");
			put("feedback."+Feedback.FeedbackType.QUESTION.toString(), "Question");
			put("feedback."+Feedback.FeedbackType.SUGGESTION.toString(), "Suggestion");
			put("feedback.writereply", "Write a reply");

			put("translate.select", "Select language");
			put("translate.translate", "Translate");

			put("msgcode.3", "Authentication failed or was canceled.");
			put("msgcode.4", "Your account has been deleted.");
			put("msgcode.5", "You have been signed out.");
			put("msgcode.6", "OpenID authentication request failed!");
			put("msgcode.7", "Something went terribly wrong!");
			put("msgcode.8", "The message was sent.");
			put("msgcode.16", "This post was deleted.");

			put("about.scoold.1", "Scoold is an open source forum platform for knowledge sharing and collaboration. It was inspired by StackOverflow.com the sites of the Stack Exchange family and we have huge respect towards the people behind them.");
			put("about.scoold.2", "It's all about:");
			put("about.scoold.3", "learning new stuff,");
			put("about.scoold.4", "sharing knowledge,");
			put("about.scoold.5", "gathering and organizing information,");
			put("about.scoold.6", "helping your mates and earning reputation points while doing it.");
			put("about.scoold.7", "Information is organized around questions. Questions can be specific or they can be in the form of a discussion on a certain topic. Below every question are the answers which are sorted by votes. The number of votes indicates the quality of both answers and questions. Almost every post can be voted up (+) or down (-).");
			put("about.questions.9", "You can, of course, always answer you own question if you know the answer to it. This way you are helping others and it will also earn you some reputation. You can ask as many questions as you want but keep in mind that duplicate questions might be voted down or even deleted.");
			put("about.questions.10", "Everything related to the website itself should be discussed in the 'Feedback' section. There you can post all your site-related questions, suggestions, general feedback or feature requests.");
			put("about.questions.11", "A question might be closed for various reasons — it has too many answers, it's a duplicate of another question or it's just inappropriate. Closing a question means that no new answers can be posted but that doesn't affect the editing functionality. A question is automatically marked for closing once it reaches -5 votes.");
			put("about.questions.12", "Everyone with the 'Teacher' badge can edit questions.");
			put("about.answers.1", "Answers can be posted to any question that hasn't been closed yet. The good answers are always at the top and one of them can be approved as the best by the author of the question. The approved answer should be the one that's most helpful to the author of the question, rather than the one with the most votes. You should avoid posting answers that are one or two words long. Тhese should be posted as comments instead (see Comments).");
			put("about.answers.2", "Everyone with the 'Freshman' badge can edit answers.");
			put("about.revisions.1", "Every time you edit a question or an answer, a new revision is created. Revisions keep track of all the changes in the text, title and tags of questions and answers. The first revision is the original. The next edit you make will create revision #1, followed by #2 and so on.");
			put("about.revisions.2", "You can always switch back to the original or any other revision. Restoring back to the original does not delete any of the previous revisions but creates a new revision with the original text restored.");
			put("about.comments.1", "Comments can be posted on all questions and answers. Basically, anything that's too short for an answer should be a comment. Comments can also contain suggestions, corrections or criticism. Bad comments will be voted down and automatically hidden once they reach -5 votes.");
			put("about.comments.2", "Everyone with the 'Enthusiast' badge can post comments.");
			put("about.voterep.1", "Voting is an integral part of Scoold. If you like something useful or interesting, vote it up. Questions that are clear and well-written should also be given +1. The same goes for answers that are helpful. If something is poorly written, unclear, rude or inappropriate, vote it down.");
			put("about.voterep.2", "Your reputation points measure how much other people trust you and like you. Reputation is earned by posting good questions and answers. It is also awarded with certain badges.");
			put("about.voterep.3", "Voting is linked to reputation. You earn reputation points when your post gets voted up and you lose points if your post gets voted down. Voting down costs 1 reputation point.");
			put("about.voterep.4", "If your answer is voted up you get:");
			put("about.voterep.5", "If your question is voted up you get:");
			put("about.voterep.6", "If your comment, feedback or translation is voted up you get:");
			put("about.voterep.7", "If your answer is approved you get:");
			put("about.voterep.8", "If you approve an answer to your question you get:");
			put("about.voterep.9", "If your post gets voted down you loose:");
			put("about.voterep.10", "If you vote something down you loose:");
			put("about.badges.1", "Badges are only given to those who deserve them. Being friendly and treating people with respect will always earn you badges and reputation points. So be nice and get some of these badges:");
			put("about.badges."+Profile.Badge.NICEPROFILE.toString(), "your profile is complete");
			put("about.badges."+Profile.Badge.TESTER.toString(), "for people who helped test the site");
			put("about.badges."+Profile.Badge.REPORTER.toString(), "for each report you submit");
			put("about.badges."+Profile.Badge.VOTER.toString(), "voted more than 100 times");
			put("about.badges."+Profile.Badge.CRITIC.toString(), "voted down 10 times");
			put("about.badges."+Profile.Badge.SUPPORTER.toString(), "voted up 50 times");
			put("about.badges."+Profile.Badge.COMMENTATOR.toString(), "posted 100 comments");
			put("about.badges."+Profile.Badge.EDITOR.toString(), "edited your post for the first time");
			put("about.badges."+Profile.Badge.BACKINTIME.toString(), "each time you restore a post from a revision");
			put("about.badges."+Profile.Badge.NOOB.toString(), "approved an answer to your question for the first time");
			put("about.badges."+Profile.Badge.ENTHUSIAST.toString(), "reached 100 reputation points");
			put("about.badges."+Profile.Badge.FRESHMAN.toString(), "reached 300 reputation points");
			put("about.badges."+Profile.Badge.SCHOLAR.toString(), "reached 500 reputation points");
			put("about.badges."+Profile.Badge.TEACHER.toString(), "reached 1000 reputation points");
			put("about.badges."+Profile.Badge.PROFESSOR.toString(), "reached 5000 reputation points");
			put("about.badges."+Profile.Badge.GEEK.toString(), "reached 9000 reputation points");
			put("about.badges."+Profile.Badge.GOODQUESTION.toString(), "your question reached 20 votes");
			put("about.badges."+Profile.Badge.GOODANSWER.toString(), "your answer reached 10 votes");
			put("about.badges."+Profile.Badge.EUREKA.toString(), "every time you answer your own question");
			put("about.badges."+Profile.Badge.SENIOR.toString(), "your account is one year old");
			put("about.badges."+Profile.Badge.DISCIPLINED.toString(), "every time you delete your own comment");
			put("about.badges."+Profile.Badge.POLYGLOT.toString(), "every time your translation is approved");
			put("about.reports.1", "If you ever notice any problems on Scoold, report them! This includes bugs, missing or incorrect information, abusive, discriminatory or exploitative behavior. Every report you send helps us keep Scoold a nice and friendly place.");
			put("about.accounts.1", "You don't need to register for an account at Scoold — you simply sign in with your social account. The first time you sign in, we will only ask you for your name and email address. We promise to keep your email safe and never send you anything, unless it's really important.");
			put("about.feedback.1", "The feedback section is where you can tell us what you think about the site. You can also ask anything about the site itself, describe in details any problems you've encountered or suggest some improvements. So if you think there's something we've missed here, go there and tell us now!");

		}
	});
}
