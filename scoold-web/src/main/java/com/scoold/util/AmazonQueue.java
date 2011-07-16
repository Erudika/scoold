/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scoold.util;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

/**
 *
 * @author alexb
 */
public class AmazonQueue<E extends Serializable> implements Queue<E> {
	
	// This queue contains only messages in JSON format!
	
	public static final String SQS_URL = "https://queue.amazonaws.com/";
	protected static final String SQS_ACCOUNT_ID = "374874639893";
	protected static final String ACCESSKEY = "AKIAI5WX2PJPYQEPWECQ";
	protected static final String SECRETKEY = "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";
	
	private static final int TIMEOUT = 30; //sec
	private static final int MAX_MESSAGES = 10;  //max in bulk
	private String QUEUE_URL;
	private AmazonSQSAsyncClient sqs;
	private static final Logger logger = Logger.getLogger(AmazonQueue.class.getName());
	private ObjectMapper mapper;
	
	public AmazonQueue(String name){
		sqs = new AmazonSQSAsyncClient(new BasicAWSCredentials(ACCESSKEY, SECRETKEY));		
		QUEUE_URL = SQS_URL.concat(SQS_ACCOUNT_ID).concat("/").concat(name);
		mapper = new ObjectMapper();
	}

	public void push(final E task) {
		if(!StringUtils.isBlank(QUEUE_URL) && task != null){
			// only allow strings - ie JSON
			if (task instanceof String && !StringUtils.isBlank((String) task)) {
				// Send a message
				try {
					SendMessageRequest sendReq = new SendMessageRequest();
					sendReq.setQueueUrl(QUEUE_URL);					
					sendReq.setMessageBody((String) task);
					
					sqs.sendMessageAsync(sendReq);
				} catch (AmazonServiceException ase) {
					logException(ase);
				} catch (AmazonClientException ace) {
					logger.log(Level.SEVERE, "Could not reach SQS. {0}", ace.getMessage());
				}
			}
		}
	}

	public E pull() {
		String task = "[]";
		JsonNode rootNode = mapper.createArrayNode(); 
		if(!StringUtils.isBlank(QUEUE_URL)){
			try {
				ReceiveMessageRequest receiveReq = new ReceiveMessageRequest(QUEUE_URL);
				receiveReq.setMaxNumberOfMessages(MAX_MESSAGES);
				List<Message> list = sqs.receiveMessage(receiveReq).getMessages();
				
				if (list != null && !list.isEmpty()) {
					for (Message message : list) {
						if(!StringUtils.isBlank(message.getBody())){
							JsonNode node = mapper.readTree(message.getBody());
							((ArrayNode) rootNode).add(node);
						}
						sqs.deleteMessage(new DeleteMessageRequest(QUEUE_URL, 
								message.getReceiptHandle()));
					}
				}
				task = mapper.writeValueAsString(rootNode);
			} catch (IOException ex) {
				logger.log(Level.SEVERE, null, ex);
			} catch (AmazonServiceException ase) {
				logException(ase);
			} catch (AmazonClientException ace) {
				logger.log(Level.SEVERE, "Could not reach SQS. {0}", ace.getMessage());
			}
		}
		return (E) task;
	}
	
	public String create(String name){
		String url = null;
		try{
			url = sqs.createQueue(new CreateQueueRequest(name, TIMEOUT)).getQueueUrl();
		} catch (AmazonServiceException ase) {
			logException(ase);
		} catch (AmazonClientException ace) {
			logger.log(Level.SEVERE, "Could not reach SQS. {0}", ace.getMessage());
		}	
		return url;
	}
	
	public void delete(){
		try {
			sqs.deleteQueue(new DeleteQueueRequest(QUEUE_URL));
		} catch (AmazonServiceException ase) {
			logException(ase);
		} catch (AmazonClientException ace) {
			logger.log(Level.SEVERE, "Could not reach SQS. {0}", ace.getMessage());
		}
	}
	
	public List<String> listQueues(){
		List<String> list = null;
		try {
			list = sqs.listQueues().getQueueUrls();
		} catch (AmazonServiceException ase) {
			logException(ase);
		} catch (AmazonClientException ace) {
			logger.log(Level.SEVERE, "Could not reach SQS. {0}", ace.getMessage());
		}
		return list;
	}
	
	private void logException(AmazonServiceException ase){
		logger.log(Level.SEVERE, "AmazonServiceException: error={0}, statuscode={1}, "
			+ "awserrcode={2}, errtype={3}, reqid={4}", 
			new Object[]{ase.getMessage(), ase.getStatusCode(), 
				ase.getErrorCode(), ase.getErrorType(), ase.getRequestId()});
	}
}
