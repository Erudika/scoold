/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.util;

import com.scoold.core.User;
import com.scoold.pages.BasePage;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

/**
 *
 * @author alexb
 */
public class MailTask extends TimerTask{

	private static final String JAVA_MAIL_NAME = "mail/scoold";
	private static final String MAIL_FROM = "no-reply@scoold.com";
	private static final String MAIL_FROM_NAME = BasePage.APPNAME + ".com";
	private User recipient;
	private String messageBody;
	private String subject;
	private ArrayList<User> recipients;

	/**
	 * Get the value of recipients
	 *
	 * @return the value of recipients
	 */
	public ArrayList<User> getRecipients() {
		return recipients;
	}

	/**
	 * Set the value of recipients
	 *
	 * @param recipients new value of recipients
	 */
	public void setRecipients(ArrayList<User> recipients) {
		this.recipients = recipients;
	}

	/**
	 * Get the value of subject
	 *
	 * @return the value of subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * Set the value of subject
	 *
	 * @param subject new value of subject
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}

	/**
	 * Get the value of messageBody
	 *
	 * @return the value of messageBody
	 */
	public String getMessageBody() {
		return messageBody;
	}

	/**
	 * Set the value of messageBody
	 *
	 * @param messageBody new value of messageBody
	 */
	public void setMessageBody(String messageBody) {
		this.messageBody = messageBody;
	}

	/**
	 * Get the value of recipient
	 *
	 * @return the value of recipient
	 */
	public User getRecipient() {
		return recipient;
	}

	/**
	 * Set the value of recipient
	 *
	 * @param recipient new value of recipient
	 */
	public void setRecipient(User recipient) {
		this.recipient = recipient;
	}

	public MailTask(){
	}

	public MailTask(User recipient, String messageBody, String subject){
		this.recipient = recipient;
		this.recipients = null;
		this.messageBody = messageBody;
		this.subject = subject;
	}

	public MailTask(ArrayList<User> recipients, String messageBody, String subject){
		this.recipient = null;
		this.recipients = recipients;
		this.messageBody = messageBody;
		this.subject = subject;
	}

	public void run() {
		if(messageBody == null || (recipient == null && recipients == null)) return;

		if(recipients == null && recipient != null){
			recipients = new ArrayList<User> ();
			recipients.add(recipient);
		}

		sendMail(recipients, MAIL_FROM, MAIL_FROM_NAME, subject, messageBody);
	}

	private boolean sendMail(ArrayList<User> to, String from, String fromName,
			String subject,	String body){
		
		if(to.isEmpty()) return false;

		boolean success = false;


		SimpleEmail mailer = new SimpleEmail();

		try {
			mailer.setMailSessionFromJNDI(JAVA_MAIL_NAME);
		} catch (NamingException ex) {
			Logger.getLogger(MailTask.class.getName()).log(Level.SEVERE, null, ex);
		}


		try {
			mailer.setCharset("utf-8");

			for (User user : to) {
				String email = user.getEmail();
				String name = user.getFullname();
				mailer.addTo(email, name);
			}

			mailer.setFrom(from, fromName);
			mailer.setSubject(subject);
			mailer.setMsg(body);

			if(mailer.getHostName() != null){
				mailer.send();
				success = true;
			}

		} catch (EmailException ex) {
			Logger.getAnonymousLogger().log(Level.SEVERE, null, ex);
		}

		return success;
	}
	
	
//	public void testMail(){
//		Date after1sec = new Date(System.currentTimeMillis() + 1000);
//		String body = "testis";
//		String subject = "this is a test";
//		User u = new User();
//		u.setEmail("dft@abv.bg");
//		u.setFullname("Alex b");
//		TimerTask mailtask = new MailTask(u, body, subject);
//		Timer t = new Timer();
//		t.schedule(mailtask, after1sec);
//	}

}
