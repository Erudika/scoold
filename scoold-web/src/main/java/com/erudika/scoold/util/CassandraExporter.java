/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.util;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.core.Linker;
import com.erudika.scoold.core.Classunit;
import com.erudika.scoold.core.Group;
import com.erudika.scoold.core.School;
import com.erudika.para.core.PObject;
import com.erudika.para.core.Sysprop;
import com.erudika.para.core.Translation;
import com.erudika.para.persistence.AWSDynamoDAO;
import static com.erudika.para.persistence.AWSDynamoDAO.CN_ID;
import com.erudika.para.persistence.CassandraDAO;
import com.erudika.para.persistence.DAO;
import com.erudika.para.search.ElasticSearch;
import com.erudika.para.search.Search;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.User;
//import com.erudika.scoold.db.cassandra.CasDAOFactory;
//import com.erudika.scoold.db.cassandra.CasDAOUtils;
import java.util.ArrayList;
import java.util.Collection;
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
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 *
 * @author Alex Bogdanovski <albogdano@me.com>
 */
public class CassandraExporter {
	private static Keyspace keyspace;
	private static Serializer<String> strser = SerializerTypeInferer.getSerializer(String.class);
	private static AmazonDynamoDBClient ddb;
	private static final String ACCESSKEY = "AKIAJ2RII4MVDWEXQZHQ";
	private static final String SECRETKEY = "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";
	private static final String ENDPOINT = "dynamodb.eu-west-1.amazonaws.com";
	private static final Logger logger = Logger.getLogger(CassandraExporter.class.getName());
	
	private static void init(){
		CassandraHostConfigurator config = new CassandraHostConfigurator();
		config.setHosts("localhost");
		config.setPort(9160);
		config.setRetryDownedHosts(true);
		config.setRetryDownedHostsDelayInSeconds(60);
		config.setAutoDiscoverHosts(false);
//		config.setAutoDiscoveryDelayInSeconds(60);
//		config.setMaxActive(100);
//		config.setMaxIdle(10);
		Cluster cluster = HFactory.getOrCreateCluster("scoold", config);
		keyspace = HFactory.createKeyspace("scoold", cluster,
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
		
		ddb = new AmazonDynamoDBClient(new BasicAWSCredentials(ACCESSKEY, SECRETKEY));
		ddb.setEndpoint(ENDPOINT);
		System.setProperty(Utils.CORE_PACKAGE, User.class.getPackage().getName());
//		logger.log(Level.INFO, "INDEX? {0}", new Object[]{search.xistsIndex(Utils.INDEX_ALIAS)});
	}
	
	public static void main(String[] args) {

//		if(true) return ;
		
		init();
		
		AWSDynamoDAO dao = new AWSDynamoDAO();
		ElasticSearch search = new ElasticSearch();
		search.setDao(dao);

		try {
			ArrayList<ParaObject> list = new ArrayList<ParaObject>();
			List<Row<String, String, String>> rows = readAll(DAO.OBJECTS);
			rows.addAll(readAll("AuthKeys"));
//			List<Row<String, String, String>> rows = readAll("54301126979227648", "54301251927543808", "54301366805336064", "54301183178706944");
			// read ALL OBJECTS
			for (Row<String, String, String> row : rows) {
				List<HColumn<String, String>> cols = row.getColumnSlice().getColumns();
				HColumn<String, String> ctype = row.getColumnSlice().getColumnByName("classtype");
				if(cols != null && !cols.isEmpty()){
					if(ctype != null){
						String classtype = ctype.getValue();
						
						////////////////////////////////////////////////////////////
						if(!classtype.equals("user")) continue;
//						if(classtype.equals("school") || classtype.equals("classunit") || classtype.equals("user")){
//							continue;
//						}
						///////////////////////////////////////////////////////////
						
						ParaObject obj = Utils.toClass(classtype).newInstance();
						obj.setId(row.getKey());
						
						// SCHEMA CHANGES!!!
						for (HColumn<String, String> hColumn : cols) {
							String name = hColumn.getName();
							String value = hColumn.getValue();
							if(name.equals("schoolid")){
								hColumn.setName(DAO.CN_PARENTID);
							}else if(name.equals("identifier") && NumberUtils.isDigits(value) && classtype.equals("user")){
								value = Utils.FB_PREFIX + value;
							}else if(name.equals("userid")){
								hColumn.setName(DAO.CN_CREATORID);
							}else if(name.equals("fullname")){
								hColumn.setName(DAO.CN_NAME);
							}else if(name.equals("classtype")){
								hColumn.setName(DAO.CN_CLASSNAME);
							}else if(name.equals("groups")){
								value = "users";
								if("admins".equals(hColumn.getValue()) || "mods".equals(hColumn.getValue())){
									value = hColumn.getValue();
								}
							}else if(name.equals("key") && classtype.equals("translation")){
								hColumn.setName("thekey");
								if(existsColumn("bg", "ApprovedTranslations", value)){
//									BeanUtils.setProperty(obj, "approved", "true");
									((Translation) obj).setApproved(true);
								}
							}
							
							if(!StringUtils.isBlank(value)){
								BeanUtils.setProperty(obj, hColumn.getName(), value);
							}							
						}
						
						logger.log(Level.INFO, "{0} -> {1}", new Object[]{row.getKey(), obj.getClassname()});
						list.add(obj);
					}else{
//						new User("54300224104960000").attachIdentifier("https://www.google.com/accounts/o8/id?id=AItOawmS_EkaWRmSheZ3u8zWGfUViu7_gyrypLE");
//						break;
						
//						String ident = row.getKey();
//						if(!cols.isEmpty()){
//							String uid = cols.get(1).getValue();
//							new User(uid).attachIdentifier(ident);
//							logger.log(Level.INFO, "CREATE IDENT: {0}->{1}", new Object[]{uid, ident});
//						}
					}
				}else{
					logger.log(Level.INFO, " COL EMPTY: {0}", row.getKey());
				}
			}
			
			if (!list.isEmpty()) {
				dao.createAll(list);
				list.clear();
			}
			
			// read ALL LINKERS
			List<Row<String, String, String>> users = readAll("UsersParents");
			List<Row<String, String, String>> schools = readAll("SchoolsParents");
			List<Row<String, String, String>> classes = readAll("ClassesParents");
			List<Row<String, String, String>> groups = readAll("GroupsParents");
			List[] arr = {users, schools, classes, groups};
			Class[] carr = {User.class, School.class, Classunit.class, Group.class};
			ArrayList<Linker> links = new ArrayList<Linker>();
			int lc = 0;
			
			for (int i = 0; i < arr.length; i++) {
				List<Row<String, String, String>> rowz = arr[i];
				for (Row<String, String, String> row : rowz) {
					List<HColumn<String, String>> cols = row.getColumnSlice().getColumns();
					if(cols != null && !cols.isEmpty()){
						List<HColumn<String,String>> parentCols = readRow(row.getKey(), DAO.OBJECTS, 
								String.class, null, null, null, Utils.DEFAULT_LIMIT, true);
						Class<? extends ParaObject> parentClass = null;
						for (HColumn<String, String> hColumn : parentCols) {
							if(hColumn.getName().equals("classtype")){
								parentClass = Utils.toClass(hColumn.getValue());
								logger.log(Level.INFO, "CLASS {0} -> ", parentClass);
								break;
							}
						}
						
						if(parentClass != null){
							for (HColumn<String, String> hColumn : cols) {
								if(hColumn.getName().equals("KEY")){
									assert hColumn.getValue().equals(row.getKey());
									continue;
								}
								String id2 = new Long(hColumn.getNameBytes().asLongBuffer().get()).toString();
								String value = hColumn.getValue();
								
								if(!StringUtils.isBlank(id2)){
									Linker link = new Linker(parentClass, carr[i], row.getKey(), id2);
									if(value.startsWith("from")){
										link.setMetadata(value);
									}
									links.add(link);
									logger.log(Level.INFO, "NEW LINKER id:{2} {0} -> {1}, META: {3}", 
											new Object[]{parentClass.getSimpleName(), carr[i].getSimpleName(), link.getId(), link.getMetadata()});
									lc++;
								}
							}
						}
					}
				}
			}
			
			if (!links.isEmpty()) {
				dao.createAll(links);
				links.clear();
			}
			
//			logger.log(Level.WARNING, " TOTAL: {0}", Integer.toString(rows.size() + lc));
		} catch (Exception e) {
			logger.log(Level.SEVERE, null, e);
		}
		
		System.exit(0);
	}
	
	private static List<Row<String, String, String>> readAll(String cf){
        CqlQuery<String, String, String> cqlQuery = new CqlQuery<String, String, String>(
                keyspace, strser, strser, strser);
        cqlQuery.setQuery("SELECT * FROM " + cf);
  
        QueryResult<CqlRows<String, String, String>> result = cqlQuery.execute();
        CqlRows<String, String, String> rows = result.get();
        return rows == null ? new ArrayList<Row<String, String, String>> () : rows.getList();
    }

	private static String getColumn(String key, String cf, String colName) {
		if(StringUtils.isBlank(key) || cf == null || colName == null) return null;
		HColumn<String, String> col = getHColumn(key, cf, colName);
		return (col != null) ? col.getValue() : null;
	}
	
	private static boolean existsColumn(String key, String cf, String columnName){
		if(StringUtils.isBlank(key)) return false;
		return getColumn(key, cf, columnName) != null;
	}
	
	private static HColumn<String, String> getHColumn(String key, String cf, String colName){
		if(cf == null) return null;
		HColumn<String, String> col = null;
		try {
			col = HFactory.createColumnQuery(keyspace, strser, strser, strser)
				.setKey(key)
				.setColumnFamily(cf)
				.setName(colName)
				.execute().get();
		} catch (Exception e) {
			logger.log(Level.SEVERE, null, e);
		}
		return col;
	}
	
	private static List<HColumn<String, String>> readRow(String key, String cf,
            Class<String> colNameClass, String startKey, MutableLong page,
            MutableLong itemcount, int maxItems, boolean reverse){
             
        if(StringUtils.isBlank(key) || cf == null)
            return new ArrayList<HColumn<String, String>>();
  
        ArrayList<HColumn<String, String>> list = new ArrayList<HColumn<String, String>>();
  
        try {
            SliceQuery<String, String, String> sq = HFactory.createSliceQuery(keyspace,
                    strser, strser, strser);
            sq.setKey(key);
            sq.setColumnFamily(cf);
            sq.setRange(startKey, null, reverse, maxItems);
  
            list.addAll((Collection<? extends HColumn<String, String>>)
                    sq.execute().get().getColumns());
  
            if(!list.isEmpty() && page != null){
                HColumn<?,?> last = list.get(list.size() - 1);
                Object lastk = ((HColumn<String, String>) last).getName();
                page.setValue((long) lastk);
                // showing max + 1 just to get the start key of next page so remove last
                if(maxItems > 1 && list.size() > maxItems){
                    list.remove(list.size() - 1); //remove last
                }
            }
//            // count keys
//            if(itemcount != null){
//                int count = countColumns(key, cf, colNameClass);
//                itemcount.setValue(count);
//            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
        }
  
        return list;
    }

}
