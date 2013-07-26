/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.erudika.scoold.db.dynamodb;

import com.erudika.scoold.db.cassandra.*;
import com.erudika.para.core.PObject;
import com.erudika.para.utils.DAO;
import com.erudika.para.utils.Search;
import com.erudika.para.utils.Stored;
import com.erudika.para.utils.Utils;
import com.erudika.scoold.db.cassandra.CasDAOFactory.CF;
import com.erudika.scoold.db.cassandra.CasDAOFactory.Column;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.prettyprint.cassandra.model.CqlQuery;
import me.prettyprint.cassandra.model.CqlRows;
import me.prettyprint.cassandra.serializers.SerializerTypeInferer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.FailoverPolicy;
import me.prettyprint.cassandra.service.OperationType;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.ConsistencyLevelPolicy;
import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.CountQuery;
import me.prettyprint.hector.api.query.MultigetSliceQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableLong;


/**
 *
 * @author alexb
 */
final class DynDAOUtils {
	
	private static final Logger logger = Logger.getLogger(DynDAOUtils.class.getName());
	private Serializer<String> strser = getSerializer(String.class);
	private static final int MAX_ITEMS = Utils.MAX_ITEMS_PER_PAGE;
	private Keyspace keyspace;
	private Mutator<String> mutator;
	private long voteLockAfter;

	//////////  ID GEN VARS  ////////////// 
	
	public static final long workerIdBits = 5L;
	public static final long dataCenterIdBits = 5L;
	public static final long maxWorkerId = -1L ^ (-1L << workerIdBits);
	public static final long maxDataCenterId = -1L ^ (-1L << dataCenterIdBits);
	public static final long sequenceBits = 12L;

	public static final long workerIdShift = sequenceBits;
	public static final long dataCenterIdShift = sequenceBits + workerIdBits;
	public static final long timestampLeftShift = sequenceBits + workerIdBits + dataCenterIdBits;
	public static final long sequenceMask = -1L ^ (-1L << sequenceBits);

	private long lastTimestamp = -1L;
	private long dataCenterId = 0L;	// only one datacenter atm
	private long workerId;
	private long sequence = 0L;
	
	////////////////////////////////////
	
	public DynDAOUtils(int port) {
		CassandraHostConfigurator config = new CassandraHostConfigurator();
		config.setHosts(System.getProperty("com.scoold.dbhosts","localhost:"+port));
		config.setPort(port);
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
		mutator = createMutator();
		voteLockAfter = DAO.VOTE_LOCK_AFTER_SEC * 1000;
	}

	public Keyspace getKeyspace(){
		return keyspace;
	}

	/********************************************
	 *			CORE OBJECT CRUD FUNCTIONS
	********************************************/
 
	public String create(PObject so){
		String id = create(so, mutator);
		mutator.execute();
		mutator.discardPendingMutations();
		return id;
	}

	public String create(PObject so, Mutator<String> mut){
		return create(null, so, mut);
	}

	public String create(String key, PObject so, Mutator<String> mut){
		if(so == null) return null;
		if(StringUtils.isBlank(so.getId())) so.setId(Utils.getNewId());
		if(so.getTimestamp() == null) so.setTimestamp(Utils.timestamp());
		String kee = StringUtils.isBlank(key) ? so.getId().toString() : key;
		// store
		storeBean(kee, so, true, mut);
		
		Search.index(so, so.getClassname());
		return so.getId();
	}

	public <T extends PObject> T read(Class<T> clazz, String key) {
		if(StringUtils.isBlank(key) || clazz == null) return null;
		CF<String> cf = CasDAOFactory.OBJECTS;
		List<HColumn<String, String>> cols = readRow(key, cf,
				String.class, null, null, null, Utils.DEFAULT_LIMIT, false);
		
		T so = fromColumns(clazz, cols);
		
		return so != null ? so : null;
	}
	
	public void update(PObject so){
		update(so, mutator);
		mutator.execute();
		mutator.discardPendingMutations();
	}

	public void update(PObject so, Mutator<String> mut){
		update(null, so, mut);
	}

	public void update(String key, PObject so, Mutator<String> mut){
		if(so == null) return;
		String kee = (key == null) ? so.getId().toString() : key;
		storeBean(kee, so, false, mut);		
		
		Search.index(so, so.getClassname());
	}

	public void delete(PObject so){
		delete(so, mutator);
		mutator.execute();
		mutator.discardPendingMutations();
	}

	public void delete(PObject so, Mutator<String> mut){
		delete(null, so, mut);
	}
	
	public void delete(String key, PObject so, Mutator<String> mut){
		if(so == null) return ;
		String kee = (key == null) ? so.getId().toString() : key;
		unstoreBean(kee, so, mut);
		
		Search.unindex(so.getId(), so.getClassname());
	}

	/********************************************
	 *				COLUMN FUNCTIONS
	********************************************/

	public <N, V> void putColumn(String key, CF<N> cf, N colName, V colValue){
		putColumn(key, cf, colName, colValue, 0);
	}

	public <N, V> void putColumn(String key, CF<N> cf, N colName, V colValue, int ttl){
		if(StringUtils.isBlank(key) || cf == null || colName == null || colValue == null) 
			return;

		HColumn<N, String> col = HFactory.createColumn(colName, colValue.toString(),
				getSerializer(colName), strser);

		if(ttl > 0) col.setTtl(ttl);

		mutator.insert(key, cf.getName(), col);
		mutator.discardPendingMutations();
	}

	public <N> String getColumn(String key, CF<N> cf, N colName) {
		if(StringUtils.isBlank(key) || cf == null || colName == null) return null;
		HColumn<N, String> col = getHColumn(key, cf, colName);
		return (col != null) ? col.getValue() : null;
	}
	
//	public Long getCounterColumn(String key){
//		if(StringUtils.isBlank(key)) return null;
//		HCounterColumn<String, String> col = getHCounterColumn(key);
//		return (col != null) ? col.getValue() : 0L;
//	}

	public <N> void removeColumn(String key, CF<N> cf, N colName) {
		if(StringUtils.isBlank(key) || cf == null) return;
		
		mutator.delete(key, cf.getName(), colName, getSerializer(colName));
		mutator.discardPendingMutations();
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void batchPut(Column ... cols){
		batchPut(Arrays.asList(cols));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public void batchPut(List<Column> cols){
		if(cols == null || cols.isEmpty()) return;
		Mutator<String> mut = createMutator();
		for (Column column : cols) {
			addInsertion(column, mut);
		}
		mut.execute();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public void batchRemove(Column ... cols){
		batchRemove(Arrays.asList(cols));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public void batchRemove(List<Column> cols){
		if(cols == null || cols.isEmpty()) return;
		Mutator<String> mut = createMutator();
		for (Column column : cols) {
			addDeletion(column, mut);
		}
		mut.execute();
	}

	public <N, V> void addInsertion(Column<N, V> col, Mutator<String> mut){
		if(mut != null && col != null){
			HColumn<N, String> hCol = HFactory.createColumn(col.getName(), 
					col.getValue().toString(), getSerializer(col.getName()), strser);

			if(col.getTtl() > 0) hCol.setTtl(col.getTtl());
			mut.addInsertion(col.getKey(), col.getCf().getName(), hCol);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public void addInsertions(List<Column> col, Mutator<String> mut){
		for (Column column : col) {
			addInsertion(column, mut);
		}
	}

	public <N, V> void addDeletion(Column<N, V> col, Mutator<String> mut){
		if(mut != null && col != null){			
			mut.addDeletion(col.getKey(), col.getCf().getName(), col.getName(),
					getSerializer(col.getName()));
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public void addDeletions(List<Column> col, Mutator<String> mut){
		for (Column column : col) {
			addDeletion(column, mut);
		}
	}


	/********************************************
	 *				RAW COLUMN FUNCTIONS
	********************************************/

	public <N> HColumn<N, String> getHColumn(String key, CF<N> cf, N colName){
		if(cf == null) return null;
		HColumn<N, String> col = null;
		try {
			col = HFactory.createColumnQuery(keyspace, strser,
					getSerializer(colName), strser)
				.setKey(key)
				.setColumnFamily(cf.getName())
				.setName(colName)
				.execute().get();
		} catch (Exception e) {
			logger.log(Level.SEVERE, null, e);
		}
		return col;
	}
	
//	public HCounterColumn<String, String> getHCounterColumn(String key){
//		CounterQuery<String> cq = null;
//		try {
//			cq = HFactory.createCounterColumnQuery(keyspace, 
//					strser, strser);
//			cq.setColumnFamily(CasDAOFactory.COUNTERS.getName());
//			cq.setKey(key);
//			cq.setName(CasDAOFactory.CN_COUNTS_COUNT);
//		} catch (Exception e) {
//			logger.log(Level.SEVERE, null, e);
//		}
//		return (cq == null) ? null : cq.execute().get();
//	}

	/********************************************
	 *				ROW FUNCTIONS
	********************************************/

	public <N> String createRow(String key, CF<N> cf, List<HColumn<N, String>> row,
			Mutator<String> mut){
		if(row == null || row.isEmpty() || cf == null) return null;

		for (HColumn<N, String> col : row){
			mut.addInsertion(key, cf.getName(), col);
		}
		
		return key;
	}

	public <N, S> List<HColumn<N, String>> readRow(String key, CF<N> cf,
			Class<N> colNameClass, S startKey, MutableLong page,
			MutableLong itemcount, int maxItems, boolean reverse){
			
		if(StringUtils.isBlank(key) || cf == null)
			return new ArrayList<HColumn<N, String>>();

		ArrayList<HColumn<N, String>> list = new ArrayList<HColumn<N, String>>();

		try {
			SliceQuery<String, N, String> sq = HFactory.createSliceQuery(keyspace,
					strser, getSerializer(colNameClass), strser);
			sq.setKey(key);
			sq.setColumnFamily(cf.getName());
			sq.setRange((N) startKey, null, reverse, maxItems);

			list.addAll((Collection<? extends HColumn<N, String>>)
					sq.execute().get().getColumns());

			if(!list.isEmpty() && page != null){
				HColumn<?,?> last = list.get(list.size() - 1);
				Object lastk = ((HColumn<N, String>) last).getName();
				page.setValue(NumberUtils.toLong(lastk.toString()));
				// showing max + 1 just to get the start key of next page so remove last
				if(maxItems > 1 && list.size() > maxItems){
					list.remove(list.size() - 1); //remove last
				}
			}
			// count keys
			if(itemcount != null){
				int count = countColumns(key, cf, colNameClass);
				itemcount.setValue(count);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, null, e);
		}

		return list;
	}

	public void deleteRow(String key, CF<?> cf, Mutator<String> mut){
		if(StringUtils.isBlank(key) || cf == null) return;
		mut.addDeletion(key, cf.getName(), null, strser);
	}

	/********************************************
	 *				READ ALL FUNCTIONS
	********************************************/

	public <N, T extends PObject> ArrayList<String> readAllKeys( String classtype, String keysKey, 
			CF<N> keysCf, Class<N> colNameClass, N startKey, MutableLong page, MutableLong itemcount, 
			int maxItems, boolean reverse, boolean colNamesAreKeys, boolean countOnlyColumns){
		
		ArrayList<String> keyz = new ArrayList<String>();
		ArrayList<HColumn<N, String>> keys = new ArrayList<HColumn<N, String>>();
		
		if(StringUtils.isBlank(keysKey) || keysCf == null) return keyz;
		
		if(itemcount != null){
			// count keys
			if (countOnlyColumns) {
				itemcount.setValue(countColumns(keysKey, keysCf, colNameClass));
			} else {
				itemcount.setValue(getBeanCount(classtype));
			}
		}
		
		try {
		// get keys from linker table
			SliceQuery<String, N, String> sq = HFactory.createSliceQuery(keyspace,
					strser,	getSerializer(colNameClass), strser);
			
			sq.setKey(keysKey);
			sq.setColumnFamily(keysCf.getName());
			sq.setRange(startKey, null, reverse, maxItems + 1);

			keys.addAll(sq.execute().get().getColumns());

			if(keys == null || keys.isEmpty()) return keyz;

			if(page != null){
				Long lastKey = 0L;
				if (colNamesAreKeys) {
					lastKey = (Long) keys.get(keys.size() - 1).getName();
				}else{
					lastKey =  NumberUtils.toLong(keys.get(keys.size() - 1).getValue());
				}

				page.setValue(lastKey);
	//			Long startK = Arrays.equals(startKey, new byte[0]) ? 0L :
	//				getLongFromBytes(startKey);

				if(maxItems > 1 && keys.size() > maxItems){
					keys.remove(keys.size() - 1); // remove last
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, null, e);
		}
			
		for (HColumn<N, String> col : keys){
			if (colNamesAreKeys) {
				keyz.add(col.getName().toString());
			} else {
				keyz.add(col.getValue());
			}
		}

		return keyz;
	}
	
	public <N, T extends PObject> ArrayList<T> readAll(Class<T> clazz, String classtype,
			String keysKey, CF<N> keysCf, Class<N> colNameClass,
			N startKey, MutableLong page, MutableLong itemcount, int maxItems,
			boolean reverse, boolean colNamesAreKeys, boolean countOnlyColumns){

		if(StringUtils.isBlank(keysKey) || keysCf == null) return new ArrayList<T>();
		if(StringUtils.isBlank(classtype)) classtype = clazz.getSimpleName().toLowerCase();
		
		ArrayList<String> keyz = readAllKeys(classtype, keysKey, keysCf, colNameClass, startKey, 
				page, itemcount, maxItems, reverse, colNamesAreKeys, countOnlyColumns);		
		
		return readAll(clazz, keyz);
	}
	
	public <T extends PObject> ArrayList<T> readAll(Class<T> clazz, List<String> keys){
		if(keys == null || keys.isEmpty() || clazz == null) return new ArrayList<T>();
		
		CF<String> cf = CasDAOFactory.OBJECTS;
		ArrayList<T> list = new ArrayList<T>(keys.size());
		Map<String, Integer> index = new HashMap<String, Integer>(keys.size());
		for (int i = 0; i < keys.size(); i++) {
			index.put(keys.get(i), i);
			list.add(null);
		}
		
		try{
			MultigetSliceQuery<String, String, String> q =
					HFactory.createMultigetSliceQuery(keyspace,
					strser, strser, strser);
									
			q.setColumnFamily(cf.getName());
			q.setKeys(keys);
			q.setRange(null, null, false, Utils.DEFAULT_LIMIT);
			
			for (Row<String, String, String> row : q.execute().get()) {
				T so = fromColumns(clazz, row.getColumnSlice().getColumns());
				if (so != null){
					list.set(index.get(row.getKey()), so);
				}
			}
			list.remove(null);		
		} catch (Exception e) {
			logger.log(Level.SEVERE, null, e);
		}		
		return list;
	}

	public List<Row<String, String, String>> readAll(CF<String> cf){
		CqlQuery<String, String, String> cqlQuery = new CqlQuery<String, String, String>(
				keyspace, strser, strser, strser);
		cqlQuery.setQuery("SELECT * FROM " + cf.getName());

		QueryResult<CqlRows<String, String, String>> result = cqlQuery.execute();
		CqlRows<String, String, String> rows = result.get();
		return rows == null ? new ArrayList<Row<String, String, String>> () : rows.getList();
	}
	
	public List<Row<String, String, String>> readAll(){
		return readAll(CasDAOFactory.OBJECTS);
	}
	
	/****************************************************
	 *		COLUMN EXISTANCE CHECK & COUNT FUNCTIONS
	*****************************************************/

	public <N> boolean existsColumn(String key, CF<N> cf, N columnName){
		if(StringUtils.isBlank(key)) return false;
		return getColumn(key, cf, columnName) != null;
	}

	public <N> int countColumns(String key, CF<N> cf, Class<N> colNameClass){
		if(StringUtils.isBlank(key) || cf == null || colNameClass == null) return 0;

		int result = 0;
		CountQuery<String, N> cq = HFactory.createCountQuery(keyspace,
				strser, getSerializer(colNameClass));

		cq.setKey(key);
		cq.setColumnFamily(cf.getName());
		cq.setRange(null, null, Utils.DEFAULT_LIMIT);

		result = cq.execute().get();
		return result;
	}

	public Long getBeanCount(String classtype){
		return 0L;
	}

	/********************************************
	 *				MISC FUNCTIONS
	********************************************/


//	public void setSystemColumn(String colName, String colValue, int ttl) {
//		if(StringUtils.isBlank(colName)) return ;
//		if (StringUtils.isBlank(colValue)) {
//			removeColumn(DAO.SYSTEM_OBJECTS_KEY, CasDAOFactory.OBJECTS, colName);
//		} else {
//			HColumn<String, String> col = HFactory.createColumn(colName, colValue);
//			if (ttl > 0 ) col.setTtl(ttl);
//			mutator.insert(DAO.SYSTEM_OBJECTS_KEY, CasDAOFactory.OBJECTS.getName(), col);
//			mutator.discardPendingMutations();
//		}
//	}
//
//	public String getSystemColumn(String colName) {
//		if(StringUtils.isBlank(colName)) return null; 
//		return getColumn(DAO.SYSTEM_OBJECTS_KEY, CasDAOFactory.OBJECTS, colName);
//	}
//	
//	public Map<String, String[]> getSystemColumns(){
//		Map<String, String[]> map = new HashMap<String, String[]>();
//		for (HColumn<String, String> hColumn : readRow(DAO.SYSTEM_OBJECTS_KEY, 
//				CasDAOFactory.OBJECTS, String.class, null, null, null, 
//				Utils.DEFAULT_LIMIT, true)) {
//			map.put(hColumn.getName(), new String[]{hColumn.getValue(), 
//				Integer.toString(hColumn.getTtl())});
//		}
//		return map;
//	}
//		
	
	private void storeBean(String key, PObject so, boolean creation, Mutator<String> mut){
		if(so == null) return;
		CF<String> cf = CasDAOFactory.OBJECTS;
		String id = so.getId();
		Map<String, Object> propsMap = Utils.getAnnotatedFields(so, Stored.class, null);
		
		propsMap.put(DAO.CN_ID, id);

		for (Entry<String, Object> entry : propsMap.entrySet()) {
			String field = entry.getKey();
			Object value = entry.getValue();

			if(value != null){
				HColumn<String, String> col = HFactory.createColumn(
						field, value.toString(), strser, strser);
				mut.addInsertion(key, cf.getName(), col);
			}else if(value == null && !creation) {
				mut.addDeletion(key, cf.getName(), field, strser);
			}
		}

		//update count
//		updateBeanCount(so.getClassname(), false, mut);
	}

	private void unstoreBean(String key, PObject so, Mutator<String> mut){
		if(so == null) return;
		CF<String> cf = CasDAOFactory.OBJECTS;
		deleteRow(key, cf, mut);
//		updateBeanCount(so.getClassname(), true, mut);
	}
	
	public final Mutator<String> createMutator(){
		return HFactory.createMutator(keyspace, strser);
	}
	
	private <T extends PObject> T fromColumns(Class<T> clazz,
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

	public <T> Serializer<T> getSerializer(Class<T> clazz) {
		return SerializerTypeInferer.getSerializer(clazz);
	}
	
	public <T> Serializer<T> getSerializer(T obj){
		return (Serializer<T>) (obj == null ? strser : getSerializer(obj.getClass()));
	}

}
