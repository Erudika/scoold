/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.erudika.scoold.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public final class Language {
	
	public static final Map<String, String> ENGLISH = Collections.unmodifiableMap(new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = 1L;
		{
			put("home.title", "Home");
			put("signin.title", "Sign in");
			put("signup.title", "Welcome to Scoold!");
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
			put("schools.title", "Schools");
			put("school.title", "School");
			put("class.title", "Class");
			put("photos.title", "Photos");
			put("drawer.title", "Drawer");
			put("messages.title", "Messages");
			put("message.title", "Message");
			put("classes.title", "Classes");
			put("groups.title", "Groups");
			put("group.title", "Group");
			put("suggest.title", "Suggestions");
			put("people.title", "People");
			put("comments.title", "Comments");
			put("comment.title", "Comment");
			put("reports.title", "Reports");
			put("media.title", "Photos and Videos");
			put("tags.title", "Tags");
			put("revisions.title", "Revisions");
			put("questions.title", "Questions");
			put("answers.title", "Answers");
			put("badges.title", "Badges");
			put("blackboard.title", "Blackboard");
			put("feedback.title", "Feedback");
			put("accounts.title", "Accounts");
			put("blog.title", "Blog");
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
			put(User.UserType.STUDENT.toString(), "Student");
			put(User.UserType.TEACHER.toString(), "Teacher");
			put(User.UserType.ALUMNUS.toString(), "Former student");
			put("students", "Students");
			put("teachers", "Teachers");
			put("alumni", "Former students");
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
			put("invited", "Invited");
			put("reopen", "Reopen");
			put("open", "Open");
			put("epicfail", "Epic fail!");
			put("join", "Join");
			put("leave", "Leave");
			put(User.Badge.NICEPROFILE.toString(), "Nice Profile");
			put(User.Badge.TESTER.toString(), "Lab Rat");
			put(User.Badge.REPORTER.toString(), "Reporter");
			put(User.Badge.VOTER.toString(), "Voter");
			put(User.Badge.COMMENTATOR.toString(), "Commentator");
			put(User.Badge.CRITIC.toString(), "Critic");
			put(User.Badge.SUPPORTER.toString(), "Supporter");
			put(User.Badge.EDITOR.toString(), "Editor");
			put(User.Badge.BACKINTIME.toString(), "Back in Time");
//			put(User.Badge.VETERAN.toString(), "Veteran");
			put(User.Badge.ENTHUSIAST.toString(), "Enthusiast");
			put(User.Badge.FRESHMAN.toString(), "Freshman");
			put(User.Badge.SCHOLAR.toString(), "Scholar");
//			put(User.Badge.TEACHER.toString(), "Teacher");
			put(User.Badge.PROFESSOR.toString(), "Professor");
			put(User.Badge.GEEK.toString(), "Geek");
			put(User.Badge.GOODQUESTION.toString(), "Good Question");
			put(User.Badge.GOODANSWER.toString(), "Good Answer");
			put(User.Badge.EUREKA.toString(), "Eureka!");
			put(User.Badge.SENIOR.toString(), "Senior");
			put(User.Badge.NOOB.toString(), "Newbie");
			put(User.Badge.PHOTOLOVER.toString(), "Photo Lover");
			put(User.Badge.FIRSTCLASS.toString(), "First Class");
			put(User.Badge.BACKTOSCHOOL.toString(), "Back to School");
			put(User.Badge.CONNECTED.toString(), "Connected");
			put(User.Badge.DISCIPLINED.toString(), "Disciplined");
			put(User.Badge.POLYGLOT.toString(), "Polyglot");
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
			put("bademail", "This email appears to be invalid.");
			put("emailexists", "This email is already registered.");
			put("invite", "Invite people");
			put("contactdetails", "Contact details");
			put("more", "More");
			put("addmore", "Add more");
			put("newbadges", "You've got new badges!");
			put("newbadge", "You've got a new badge!");
			put("reputation", "Reputation");
			put("writehere", "Write here...");
			put("website", "Website");
			put("merge", "Merge");
			put("pagenotfound", "This page cannot be found.");
			put("forbidden", "You shall not pass!");
			put("servererror", "Critical server error.");
			put("sitedown", "Scoold is down. We'll be back soon.");
			put("maintenance", "Scoold is being fixed. Expect some downtime.");
			put("license", "The content on this site is licensed under a {0} license.");
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
			put("mobile", "Mobile");
			put("classic", "Classic");
			
			put("humantime.s", "{0} seconds ago");
			put("humantime.m", "{0} minutes ago");
			put("humantime.h", "{0} hours ago");
			put("humantime.d", "{0} days ago");
			put("humantime.n", "{0} months ago");
			put("humantime.y", "{0} years ago");
			
			
			put("school."+School.SchoolType.PRIMARY.toString(), "Primary school");
			put("school."+School.SchoolType.ELEMENTARY.toString(), "Elementary school");
			put("school."+School.SchoolType.SECONDARY.toString(), "Secondary school");
			put("school."+School.SchoolType.HIGHSCHOOL.toString(), "High school");
			put("school."+School.SchoolType.GYMNASIUM.toString(), "Gymnasium");
			put("school."+School.SchoolType.LYCEUM.toString(), "Lyceum");
			put("school."+School.SchoolType.COLLEGE.toString(), "College");
			put("school."+School.SchoolType.THEOLOGY.toString(), "Theological school");
			put("school."+School.SchoolType.SEMINARY.toString(), "Seminary");
			put("school."+School.SchoolType.ACADEMY.toString(), "Academy");
			put("school."+School.SchoolType.SPECIALIZED.toString(), "Specialized school");
			put("school."+School.SchoolType.PRIVATE.toString(), "Private school");
			put("school."+School.SchoolType.UNIVERSITY.toString(), "University");
			put("school."+School.SchoolType.MIDDLE.toString(), "Middle school");
			put("school."+School.SchoolType.ARTS.toString(), "Art school");
			put("school."+School.SchoolType.SPORTS.toString(), "Sports school");

			put("signup.form.myname", "My name is");
			put("signup.form.iama", "I am a");
			put("signup.form.email", "Email");
			put("signup.form.termsofuse", "By clicking the button above, you agree to our 'Terms of use'.");

			put("signin.facebook", "Sign in with Facebook");
			put("signin.openid", "Sign in with OpenID");
			put("signin.openid.text", "OpenID allows you to use an existing account to sign in to multiple websites, without needing to create new passwords. It's a free and easy way to use a single digital identity across the Internet.");
			
			put("profile.about.title", "About me");
			put("profile.about.dob", "Date of birth");
			put("profile.about.location", "Location");
			put("profile.about.city", "City");
			put("profile.about.age", "Age");
			put("profile.about.membersince", "Active since");
			put("profile.about.lastseen", "Last activity");
			put("profile.about.aboutme", "More about me");
			put("profile.about.ilike", "Stuff I like");
			put("profile.contacts.title", "Contacts");
			put("profile.contacts.nocontacts", "No contacts.");
			put("profile.contacts.added", "You've added a new contact.");
			put("profile.status.txt", "Write something here...");
			put("profile.myschools.add.period", "Period of education");
			put("profile.myschools.create.type", "School type");
			put("profile.myschools.create.name", "School name");
			put("profile.myclasses.gradyear", "Graduation year");
			put("profile.myclasses.create.schoollink", "Select a school for this class");
			put("profile.myclasses.create.name", "Class name");
			put("profile.drawer.share", "Share something...");
			put("profile.drawer.embedly", "Here you can embed content from any of the services listed below.");
			put("profile.drawer.embedly.notanimage", "This is not an image.");
			put("profile.drawer.embedly.photosaved", "OK. Saved in Photos.");
			put("profile.thisisme", "This is me!");
			put("profile.deleted", "Deleted user");
			put("profile.posts.noquestions", "No questions.");
			put("profile.posts.noanswers", "No answers.");

			put("search.notfound", "Nothing found.");
			put("search.search", "Search");
			put("search.people", "Find a person");
			put("search.schools", "Find a school");
			put("search.classes", "Find a class");
			put("search.questions", "Find a question");

			put("school.create", "Create а school");
			put("school.created", "Created");
			put("school.someinfo", "Write some information about this school.");
			put("school.noclasses", "This school has no classes.");
			put("school.nopeople", "This school is empty.");

			put("classes.noschools", "You don't seem to have any schools linked to your account. You need to join at least one school before you can start creating classes.");
			put("class.create", "Create a class");
			put("class.classroom", "Classroom");
			put("class.classmates", "Classmates");
			put("class.blankboard", "This blackboard is blank.");
			put("class.chat", "Chat");
			put("class.chat.userin", "joined the chat");
			put("class.chat.userout", "left the chat");
			put("class.chat.connection.error", "Failed to connect.");
			put("class.chat.polling.error", "Connection lost. Let's try again...");
			put("class.chat.reconnect.error", "Reconnecting...");
			put("class.chat.whoshere", "Who's here?");
			put("class.invitepeople", "Invite people to join this class.");
			put("class.invite.text", "Hello! You were invited to join a class created by {0}. Follow this link:");
			put("class.copylink", "Copy and send this link to your friends");
			put("class.invitation", "Invitation");
			put("class.addclassmates", "Add your classmates");
			
			put("group.create", "Create a group");
			put("group.image", "Group picture");
			put("group.members", "Group members");
			put("group.add", "Add people to group");
			put("group.changepic", "Change picture");

			put("messages.nomessages", "You have no messages.");
			put("messages.to", "To");
			put("messages.text", "Text");
			put("messages.sent", "Message sent.");
			put("messages.nocontacts", "You don't have any contacts yet.");

			put("comments.write", "Add comment");
			put("comments.show", "Show comment");
			put("comments.hidden", "Hidden.");

			put("photos.gallery", "Gallery");
			put("photos.viewall", "View all");
			put("photos.labels", "Labels");
			put("photos.addlabels", "Add labels");
			put("photos.latest", "Latest photos");
			put("photos.vieworiginal", "View original");

			put("settings.authentication", "Authentication settings");
			put("settings.authentication.text", "Identifiers are your keys to the site. You can have one additional identifer linked to your Scoold account.");
			put("settings.primaryid", "Primary identifier");
			put("settings.secondaryid", "Secondary identifier");
			put("settings.email", "Change Email");
			put("settings.email.new", "New email address");
			put("settings.delete", "Delete my account!");
			put("settings.delete.text", "Permanently delete your Scoold account and data.");
			put("settings.delete.confirm", "Please confirm that you really want to delete your account.");
			put("settings.tagfilter", "Tag filter");
			put("settings.tagfilter.text", "You can filter questions by specifying the tags you are interested in.");

			put("reports."+Report.ReportType.SPAM.toString(), "Spam or commercial content");
			put("reports."+Report.ReportType.OFFENSIVE.toString(), "Offensive content, violence or abuse");
			put("reports."+Report.ReportType.DUPLICATE.toString(), "Duplicate content");
			put("reports."+Report.ReportType.INCORRECT.toString(), "Incorrect or outdated content");
			put("reports."+Report.ReportType.OTHER.toString(), "Other problems that need attention");
			put("reports.all", "All reports");
			put("reports.school", "Schools");
			put("reports.classunit", "Classes");
			put("reports.group", "Groups");
			put("reports.media", "Photos and Videos");
			put("reports.post", "Posts");
			put("reports.user", "People");
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
			put("posts.ask", "Ask a question");
			put("posts.title", "Title");
			put("posts.writeanswer", "Write your answer");
			put("posts.youranswer", "Your answer");
			put("posts.closed", "This question is closed.");
			put("posts.error1", "Oops! Something went wrong.");
			put("posts.marked", "Marked for deletion");
			put("posts.forclosing", "Marked for closing");
			put("posts.noquestions", "No questions found.");
			put("posts.updated", "Updated");
			put("posts.schoolfilter", "Filter by school");
			put("posts.unanswered", "Unanswered");
			put("posts.mostpopular", "Most popular questions");
			put("posts.tagged", "Questions with tag");
			put("posts.selectschool", "Select school");
			put("posts.noschools", "Questions are linked to schools. In order to ask a question you need to join at least one school.");
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
			put("msgcode.9", "You cаn't join this school.");
			put("msgcode.10", "You've left this school.");
			put("msgcode.11", "You've joined this school.");
			put("msgcode.12", "You've added this person to your contacts.");
			put("msgcode.13", "You've removed this person from your contacts.");
			put("msgcode.14", "You've joined this class.");
			put("msgcode.15", "You've left this class.");
			
			put("about.intro", "Got questions about your education? You're in the right place. We're all here to help each other.");
			
			put("about.scoold.1", "Scoold is a student network for knowledge sharing and collaboration. Its primary goal is to help students find the answers to all questions related to their education. But Scoold isn't just for students — teachers and former students are welcome, too! Anyone can ask a question about anything related to education.");
			put("about.scoold.2", "In general, Scoold is all about:");
			put("about.scoold.3", "learning new stuff,");
			put("about.scoold.4", "sharing knowledge,");
			put("about.scoold.5", "gathering and organizing information,");
			put("about.scoold.6", "helping your mates and earning reputation points while doing it.");
			put("about.scoold.7", "Information is organized around questions. Questions can be specific or they can be in the form of a discussion on a certain topic. Below every question are the answers which are sorted by votes. The number of votes indicates the quality of both answers and questions. Almost everything posted on Scoold can be voted up (+) or down (-).");
			put("about.scoold.8", "Scoold has been greatly influenced by the sites of the Stack Exchange family and we have huge respect towards the guys who built them. We wanted to create a website based on their clever model but targeted towards students and teachers.");
			
			put("about.questions.1", "You can ask or discuss anything about education on Scoold. For example you might ask questions about:");
			put("about.questions.2", "coursework or homework,");
			put("about.questions.3", "examinations,");
			put("about.questions.4", "the curriculum,");
			put("about.questions.5", "learning materials, books and research papers,");
			put("about.questions.6", "research and experiments,");
			put("about.questions.7", "internal school matters like timetables, graduations or other events.");
			put("about.questions.8", "Questions are linked to schools, so you need to join at least one school in order to ask a question. This way you can more easily find the information that is only relevant to your school and interests.");
			put("about.questions.9", "You can, of course, always answer you own question if you know the answer to it. This way you are helping others and it will also earn you some reputation. You can ask as many questions as you want, as long as they don't exist already for a particular school. Duplicate questions might be voted down or even deleted.");
			put("about.questions.10", "Everything related to Scoold itself should be discussed in the 'Feedback' section. There you can post all your site-related questions, suggestions, general feedback or feature requests.");
			put("about.questions.11", "A question might be closed for various reasons — it has too many answers, it's a duplicate of another question or it's just inappropriate. Closing a question means that no new answers can be posted but that doesn't affect the editing functionality. A question is automatically marked for closing once it reaches -5 votes.");
			put("about.questions.12", "Everyone with the 'Teacher' badge can edit questions.");
			
			put("about.answers.1", "Answers can be posted to any question that hasn't been closed yet. The good answers are always at the top and one of them can be approved as the best by the author of the question. The approved answer should be the one that's most helpful to the author of the question, rather than the one with the most votes. You should avoid posting answers that are one or two words long. Тhese should be posted as comments instead (see Comments).");
			put("about.answers.2", "Everyone with the 'Freshman' badge can edit answers.");

			put("about.revisions.1", "Every time you edit a question or an answer, a new revision is created. Revisions keep track of all the changes in the text, title and tags of questions and answers. The first revision is the original. The next edit you make will create revision #1, followed by #2 and so on.");
			put("about.revisions.2", "You can always switch back to the original or any other revision. Restoring back to the original does not delete any of the previous revisions but creates a new revision with the original text restored.");
			
			put("about.comments.1", "Comments can be posted on all questions and answers. Basically, anything that's too short for an answer should be a comment. Comments can also contain suggestions, corrections or criticism. Bad comments will be voted down and automatically hidden once they reach -5 votes.");
			put("about.comments.2", "Everyone with the 'Enthusiast' badge can post comments.");
			
			put("about.schools.1", "You can create school pages for any kind of school or university that exists around the world. Schools are open for everyone to join. On each school page you can write a brief description and contact information for that school.");
			put("about.schools.2", "Schools are linked to questions so that every school has a set of questions associated with it. This allows for questions to be organized in a way which allows them to be filtered by school.");
			put("about.schools.3", "Everyone with the 'Enthusiast' badge can edit school pages.");
			
			put("about.classes.1", "Classes are groups of people who study or have studied together. When creating a class you should specify the year of graduation (which might be in the past) and the school associated with that class. Also you can invite your friends to join you newly created class. ");
			put("about.classes.2", "Every class has a blackboard where you and your classmates can write anything like important messages or reminders. There's also a chat room for each class.");
			put("about.classes.3", "Everyone with the 'Enthusiast' badge can write on the blackboard.");
			
			put("about.groups.1", "Groups are like classes but private — the only way to join a group is to get invited by the creator of that group. As with schools, you can post questions and answers in your group but they will be visible only to the members of the group.");
			put("about.groups.2", "You can add as many people to a group as you wish. In order to add a person to your group you first have to add them as contact.")			;
						
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
			put("about.badges."+User.Badge.NICEPROFILE.toString(), "your profile is complete");
			put("about.badges."+User.Badge.TESTER.toString(), "for people who helped test the site");
			put("about.badges."+User.Badge.REPORTER.toString(), "for each report you submit");
			put("about.badges."+User.Badge.VOTER.toString(), "voted more than 100 times");
			put("about.badges."+User.Badge.CRITIC.toString(), "voted down 10 times");
			put("about.badges."+User.Badge.SUPPORTER.toString(), "voted up 50 times");
			put("about.badges."+User.Badge.COMMENTATOR.toString(), "posted 100 comments");
			put("about.badges."+User.Badge.EDITOR.toString(), "edited your post for the first time");
			put("about.badges."+User.Badge.BACKINTIME.toString(), "each time you restore a post from a revision");
			put("about.badges."+User.Badge.NOOB.toString(), "approved an answer to your question for the first time");
			put("about.badges."+User.Badge.ENTHUSIAST.toString(), "reached 100 reputation points");
			put("about.badges."+User.Badge.FRESHMAN.toString(), "reached 300 reputation points");
			put("about.badges."+User.Badge.SCHOLAR.toString(), "reached 500 reputation points");
			put("about.badges."+User.Badge.TEACHER.toString(), "reached 1000 reputation points");
			put("about.badges."+User.Badge.PROFESSOR.toString(), "reached 5000 reputation points");
			put("about.badges."+User.Badge.GEEK.toString(), "reached 9000 reputation points");
			put("about.badges."+User.Badge.GOODQUESTION.toString(), "your question reached 20 votes");
			put("about.badges."+User.Badge.GOODANSWER.toString(), "your answer reached 10 votes");
			put("about.badges."+User.Badge.EUREKA.toString(), "every time you answer your own question");
			put("about.badges."+User.Badge.SENIOR.toString(), "your account is one year old");
			put("about.badges."+User.Badge.PHOTOLOVER.toString(), "added 20 photos to your gallery");
			put("about.badges."+User.Badge.FIRSTCLASS.toString(), "joined a class for the first time");
			put("about.badges."+User.Badge.BACKTOSCHOOL.toString(), "joined a school for the first time");
			put("about.badges."+User.Badge.CONNECTED.toString(), "added 10 contacts");
			put("about.badges."+User.Badge.DISCIPLINED.toString(), "every time you delete your own comment");
			put("about.badges."+User.Badge.POLYGLOT.toString(), "every time your translation is approved");

			put("about.reports.1", "If you ever notice any problems on Scoold, report them! This includes bugs, missing or incorrect information, abusive, discriminatory or exploitative behavior. Every report you send helps us keep Scoold a nice and friendly place.");
			
			put("about.accounts.1", "You don't need to register for an account at Scoold — you simply sign in with your account at Facebook, Google, Yahoo, AOL or any other account that is compatible with OpenID. The first time you sign in, we will only ask you for your name and email address. We promise to keep your email safe and never send you anything, unless it's really important.");
			
			put("about.feedback.1", "The feedback section is where you can tell us what you think about the site. You can also ask anything about the site itself, describe in details any problems you've encountered or suggest some improvements. So if you think there's something we've missed here, go there and tell us now!");
				
		}
	});
}
