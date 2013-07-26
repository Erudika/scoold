/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.util;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.ReturnConsumedCapacity;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.erudika.scoold.core.Classunit;
import com.erudika.scoold.core.Group;
import com.erudika.scoold.core.Media;
import com.erudika.scoold.core.Message;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Report;
import com.erudika.scoold.core.School;
import com.erudika.para.core.PObject;
import com.erudika.para.utils.DAO;
import com.erudika.para.utils.Search;
import com.erudika.para.utils.Stored;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Tag;
import com.erudika.scoold.core.User;
import com.erudika.scoold.db.AbstractDAOFactory;
import com.erudika.scoold.db.cassandra.CasDAOFactory;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.prettyprint.cassandra.model.CqlQuery;
import me.prettyprint.cassandra.model.CqlRows;
import me.prettyprint.cassandra.serializers.SerializerTypeInferer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.FailoverPolicy;
import me.prettyprint.cassandra.service.OperationType;
import static me.prettyprint.cassandra.service.OperationType.READ;
import static me.prettyprint.cassandra.service.OperationType.WRITE;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.ConsistencyLevelPolicy;
import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.node.NodeBuilder;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class CassandraExporter {
	private static Keyspace keyspace;
	private static Mutator<String> mutator;
	private static Serializer<String> strser = SerializerTypeInferer.getSerializer(String.class);
	private static HashMap<String, XContentBuilder> searchables;
	private static AmazonDynamoDBClient ddb;
	private static final String ACCESSKEY = "AKIAJ2RII4MVDWEXQZHQ";
	private static final String SECRETKEY = "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";
	private static final String ENDPOINT = "dynamodb.eu-west-1.amazonaws.com";
	private static final String TABLE = DAO.OBJECTS;
	private static final Logger logger = Logger.getLogger(CassandraExporter.class.getName());
	public static Client searchClient;
		
	private static void init(){
		NodeBuilder nb = NodeBuilder.nodeBuilder();
		nb.clusterName(CasDAOFactory.CLUSTER);
		searchClient = new TransportClient(nb.settings()).addTransportAddress(
						new InetSocketTransportAddress("localhost", 9300));				
		
		CassandraHostConfigurator config = new CassandraHostConfigurator();
		config.setHosts("localhost");
		config.setPort(9160);
		config.setRetryDownedHosts(true);
		config.setRetryDownedHostsDelayInSeconds(60);
		config.setAutoDiscoverHosts(false);
//		config.setAutoDiscoveryDelayInSeconds(60);
//		config.setMaxActive(100);
//		config.setMaxIdle(10);
		Cluster cluster = HFactory.getOrCreateCluster(CasDAOFactory.CLUSTER, config);
		keyspace = HFactory.createKeyspace(CasDAOFactory.KEYSPACE, cluster,
			new ConsistencyLevelPolicy() {
				public HConsistencyLevel get(OperationType arg0) { return getLevel(arg0); }
				public HConsistencyLevel get(OperationType arg0, String arg1) { return getLevel(arg0); }
				private HConsistencyLevel getLevel(OperationType arg0){
					switch(arg0){
						case READ: return HConsistencyLevel.ONE;
						case WRITE: return HConsistencyLevel.QUORUM;
						default: return HConsistencyLevel.ONE;
					}
				}
			}, FailoverPolicy.ON_FAIL_TRY_ALL_AVAILABLE);		
		mutator = HFactory.createMutator(keyspace, strser);
		
		ddb = new AmazonDynamoDBClient(new BasicAWSCredentials(ACCESSKEY, SECRETKEY));
		ddb.setEndpoint(ENDPOINT);
	}
	
	public static void main(String[] args) {
		init();
		
		String name = Utils.INDEX_ALIAS;
		if(!Search.existsIndex(name)) Search.createIndex(name);
		
		try {
			int i = 0;
			BulkRequestBuilder brb = searchClient.prepareBulk();
			HashMap<String, List<WriteRequest>> batch = new HashMap<String,  List<WriteRequest>>();
			ArrayList<WriteRequest> list = new ArrayList<WriteRequest>();
			
			List<Row<String, String, String>> rows = readAll();
//			List<Row<String, String, String>> rows = readAll("54301126979227648", "54301251927543808", "54301366805336064", "54301183178706944");
			
			// read from cassandra
			for (Row<String, String, String> row : rows) {
				List<HColumn<String, String>> cols = row.getColumnSlice().getColumns();
				if(cols != null && !cols.isEmpty()){
					HColumn<String, String> ctype = row.getColumnSlice().getColumnByName("classtype");
					if(ctype != null){
						String classtype = ctype.getValue();
						Map<String, Object> data = Utils.getAnnotatedFields(fromColumns(
								Utils.toClass(classtype), cols), Stored.class, null);
						
//						HashMap<String, AttributeValue> rou = new HashMap<String, AttributeValue>();
//						rou.put("key", new AttributeValue().withS(row.getKey()));
//						for (Map.Entry<String, Object> entry : data.entrySet()) {
//							String field = entry.getKey();
//							Object value = entry.getValue();
//
//							if(value != null && !StringUtils.isBlank(value.toString())){
//								rou.put(field, new AttributeValue(value.toString()));
//							}
//						}
						logger.log(Level.INFO, "read row {0} ctype = {1} i={2} rou {3}", new Object[]{row.getKey(), classtype, i, data});
//						if(i < 5){
//							list.add(new WriteRequest().withPutRequest(new PutRequest().withItem(rou)));
//							i++;
//						}else{
//							batch.put(TABLE, list);
//							createRows(batch);
//							batch.clear();
//							list.clear();
//							i = 0;
//							
//							Thread.sleep(500);
//						}
						
						// index
						logger.info(row.getKey()+" ----------------------------------------------------------- indexed!");
//						brb.add(searchClient.prepareIndex(name, classtype, row.getKey()).
//								setSource(data));
					}else{
						logger.log(Level.WARNING, " NO CLASSTYPE: {0}", row.getKey());
					}
				}
			}
//			if (!list.isEmpty()) {
//				batch.put(TABLE, list);
//				createRows(batch);
//			}
//			brb.execute();
		} catch (Exception e) {
			logger.log(Level.SEVERE, null, e);
		}
		
	}
	
	private static List<Row<String, String, String>> readAll(String ... ids){
		CqlQuery<String, String, String> cqlQuery = new CqlQuery<String, String, String>(
				keyspace, strser, strser, strser);
		if(ids == null || ids.length == 0){
			cqlQuery.setQuery("SELECT * FROM " + CasDAOFactory.OBJECTS.getName());
		}else{
			String idz = "";
			for (String id : ids) {
				idz = idz.concat("'").concat(id).concat("',");
			}
			if(idz.endsWith(",")) idz = idz.substring(0, idz.length() - 1);
			cqlQuery.setQuery("SELECT * FROM " + CasDAOFactory.OBJECTS.getName() + " WHERE key IN("+idz+")");
		}
		QueryResult<CqlRows<String, String, String>> result = cqlQuery.execute();
		CqlRows<String, String, String> rows = result.get();
		return rows == null ? new ArrayList<Row<String, String, String>> () : rows.getList();
	}

	private static void createRows(Map<String, List<WriteRequest>> row){
		try {
			BatchWriteItemResult res = ddb.batchWriteItem(new BatchWriteItemRequest().withRequestItems(row).
					withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL));
			logger.log(Level.WARNING, ">>> consumed {0}", res.getConsumedCapacity());
		} catch (Exception e) {
			logger.log(Level.SEVERE, null, e);
		}		
	}
	
	private static <T extends PObject> T fromColumns(Class<T> clazz,
			List<HColumn<String, String>> cols) {
		if (cols == null || cols.isEmpty())	return null;

		T transObject = null;
		try {
			transObject = clazz.newInstance();
			for (HColumn<String, String> col : cols) {
				String name = col.getName();
				String value = col.getValue();
				//set property WITH CONVERSION
				BeanUtils.setProperty(transObject, name, value);
				//set property WITHOUT CONVERSION
//				PropertyUtils.setProperty(transObject, name, value);
			}
		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		return transObject;
	}
	
	private static XContentBuilder getMapping(String type) throws Exception{
		return XContentFactory.jsonBuilder().startObject().startObject(type).
					startObject("_source").
						field("enabled", "true").
						field("compress", "true").
					endObject().endObject().endObject();
	}
}
