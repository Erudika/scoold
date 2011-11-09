/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.river.amazonsqs;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import java.io.IOException;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.action.bulk.BulkRequestBuilder;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.river.AbstractRiverComponent;
import org.elasticsearch.river.River;
import org.elasticsearch.river.RiverName;
import org.elasticsearch.river.RiverSettings;
import java.util.List;
import java.util.Map;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.type.TypeReference;
import org.elasticsearch.client.action.delete.DeleteRequestBuilder;
import org.elasticsearch.client.action.index.IndexRequestBuilder;

/**
 * @author aleski
 */
public class AmazonsqsRiver extends AbstractRiverComponent implements River {

    private final Client client;
	private final AmazonSQSAsyncClient sqs;
	private final ObjectMapper mapper;
	
	private final String INDEX;
    private final String ACCESSKEY;
    private final String SECRETKEY;
    private final String QUEUE_URL;
    private final int MAX_MESSAGES;
    private final int TIMEOUT;

    private volatile boolean closed = false;
    private volatile Thread thread;
	
    @SuppressWarnings({"unchecked"})
    @Inject 
	public AmazonsqsRiver(RiverName riverName, RiverSettings settings, Client client) {
        super(riverName, settings);
        this.client = client;
				
        if (settings.settings().containsKey("amazonsqs")) {
            Map<String, Object> sqsSettings = (Map<String, Object>) settings.settings().get("amazonsqs");
            ACCESSKEY = XContentMapValues.nodeStringValue(sqsSettings.get("accesskey"), "null");
            SECRETKEY = XContentMapValues.nodeStringValue(sqsSettings.get("secretkey"), "null");
            QUEUE_URL = XContentMapValues.nodeStringValue(sqsSettings.get("queue_url"), "null");
        } else {
            ACCESSKEY = settings.globalSettings().get("cloud.aws.access_key");
            SECRETKEY = settings.globalSettings().get("cloud.aws.secret_key");
            QUEUE_URL = settings.globalSettings().get("cloud.aws.sqs.queue_url");
        }
		
		if (settings.settings().containsKey("index")) {
			Map<String, Object> indexSettings = (Map<String, Object>) settings.settings().get("index");
            INDEX = XContentMapValues.nodeStringValue(indexSettings.get("index"), "elasticsearch");
            MAX_MESSAGES = XContentMapValues.nodeIntegerValue(indexSettings.get("max_messages"), 10);
            TIMEOUT = XContentMapValues.nodeIntegerValue(indexSettings.get("timeout_seconds"), 10);
		} else {
			INDEX = settings.globalSettings().get("cluster.name");
			MAX_MESSAGES = 10;
			TIMEOUT = 10;
		}
		
		sqs = new AmazonSQSAsyncClient(new BasicAWSCredentials(ACCESSKEY, SECRETKEY));
		mapper = new ObjectMapper();
    }

    public void start() {
        logger.info("creating amazonsqs river using queue ", QUEUE_URL);

        thread = EsExecutors.daemonThreadFactory(settings.globalSettings(), "amazonsqs_river").newThread(new Consumer());
        thread.start();
    }

    public void close() {
        if (closed) {
            return;
        }
        logger.info("closing amazonsqs river");
        closed = true;
        thread.interrupt();
    }

    private class Consumer implements Runnable {
		private int idleCount = 0;
		
		public void run() {
			String id = null;	// document id
			String type = null;	// document type
			String op = null;	// operation type - INDEX, CREATE, DELETE
			Map<String, Object> data = null; // document data for indexing
			
            while (!closed) {
				// pull
				String task = pull();
				int sleeptime = TIMEOUT * 1000;
				
				// index
				if (!isBlank(task)) {
					BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
					
					try {
						JsonNode jsonTasks = mapper.readTree(task);
						
						if(jsonTasks.isArray()){
							for (JsonNode msg : jsonTasks) {
								if(msg.has("_id") && msg.has("_type") && msg.has("_op") 
										&& msg.has("_data")){
									id = msg.get("_id").getTextValue();
									type = msg.get("_type").getTextValue();
									op = msg.get("_op").getTextValue();
									data = mapper.readValue(msg.get("_data"), 
											new TypeReference<Map<String, Object>>(){});
								}else{
									continue;
								}

								if (op.equalsIgnoreCase("create")) {
									bulkRequestBuilder.add(new IndexRequestBuilder(client, INDEX).
											setId(id).
											setType(type).
											setCreate(true).
											setSource(data).request());
								} else if(op.equalsIgnoreCase("update")) {
									bulkRequestBuilder.add(new IndexRequestBuilder(client, INDEX).
											setId(id).
											setType(type).
											setSource(data).request());
								} else if(op.equalsIgnoreCase("delete")) {
									bulkRequestBuilder.add(new DeleteRequestBuilder(client, INDEX).
											setId(id).
											setType(type).request());
								}else{
									continue;
								}
							}

							// sleep less when there are lots of messages in queue
							// sleep more when idle
							if(bulkRequestBuilder.numberOfActions() > 0){
								BulkResponse response = bulkRequestBuilder.execute().actionGet();
								if (response.hasFailures()) {
									logger.warn("Bulk operation completed with errors: " + response.buildFailureMessage());
								}
								// many tasks in queue => throttle up
								if (bulkRequestBuilder.numberOfActions() >= (MAX_MESSAGES / 2)) {
									sleeptime = 1000;
								}else if (bulkRequestBuilder.numberOfActions() == MAX_MESSAGES) {
									sleeptime = 100;
								}
								idleCount = 0;
							}else{
								idleCount++;
								// no tasks in queue => throttle down
								if(idleCount >= 3) {
									sleeptime *= 10; 
								}
							}
						}
					} catch (Exception e) {
						logger.error("Bulk index operation failed {}", e);
						continue;
					}
				}
				
				try {
					Thread.sleep(sleeptime);
				} catch (InterruptedException e) {
					if (closed) break;
				}
            }
        }

		private String pull() {
			String task = "[]";
			JsonNode rootNode = mapper.createArrayNode(); 
			if(!isBlank(QUEUE_URL)){
				try {
					ReceiveMessageRequest receiveReq = new ReceiveMessageRequest(QUEUE_URL);
					receiveReq.setMaxNumberOfMessages(MAX_MESSAGES);
					List<Message> list = sqs.receiveMessage(receiveReq).getMessages();

					if (list != null && !list.isEmpty()) {
						for (Message message : list) {
							if(!isBlank(message.getBody())){
								JsonNode node = mapper.readTree(message.getBody());
								((ArrayNode) rootNode).add(node);
							}
							sqs.deleteMessage(new DeleteMessageRequest(QUEUE_URL, 
									message.getReceiptHandle()));
						}
					}
					task = mapper.writeValueAsString(rootNode);
				} catch (IOException ex) {
					logger.error(ex.getMessage());
				} catch (AmazonServiceException ase) {
					logException(ase);
				} catch (AmazonClientException ace) {
					logger.error("Could not reach SQS. {}", ace.getMessage());
				}
			}
			return task;
		}

		private void logException(AmazonServiceException ase){
			logger.error("AmazonServiceException: error={}, statuscode={}, "
				+ "awserrcode={}, errtype={}, reqid={}", 
				new Object[]{ase.getMessage(), ase.getStatusCode(), 
					ase.getErrorCode(), ase.getErrorType(), ase.getRequestId()});
		}
    }
	
	private boolean isBlank(String str){
		return str == null || str.isEmpty() || str.trim().isEmpty();
	}
}
