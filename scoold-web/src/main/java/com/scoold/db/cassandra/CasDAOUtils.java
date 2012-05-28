/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scoold.db.cassandra;

import com.scoold.core.*;
import com.scoold.core.Post.PostType;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractDAOUtils;
import com.scoold.db.cassandra.CasDAOFactory.CF;
import com.scoold.db.cassandra.CasDAOFactory.Column;
import com.scoold.util.QueueFactory;
import com.scoold.util.ScooldAppListener;
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
import me.prettyprint.hector.api.beans.HCounterColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.*;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.mutable.MutableLong;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.*;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;


/**
 *
 * @author alexb
 */
final class CasDAOUtils extends AbstractDAOUtils {
	
	private static final Logger logger = Logger.getLogger(CasDAOUtils.class.getName());
	private static Client searchClient = ScooldAppListener.getSearchClient();
	private Serializer<String> strser = getSerializer(String.class);
	private static final int MAX_ITEMS = AbstractDAOFactory.MAX_ITEMS_PER_PAGE;
	private static final long TIMER_OFFSET = 1310084584692L;
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
	
	public CasDAOUtils(int port) {
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
		voteLockAfter = convertMsTimeToCasTime(keyspace, CasDAOFactory.VOTE_LOCK_AFTER_SEC*1000);
		initIdGen();
	}

	public Keyspace getKeyspace(){
		return keyspace;
	}

	/********************************************
	 *			CORE OBJECT CRUD FUNCTIONS
	********************************************/
 
	public Long create(ScooldObject so){
		Long id = create(so, mutator);
		mutator.execute();
		mutator.discardPendingMutations();
		return id;
	}

	public Long create(ScooldObject so, Mutator<String> mut){
		return create(null, so, mut);
	}

	public Long create(String key, ScooldObject so, Mutator<String> mut){
		if(so == null) return null;
		if(so.getId() == null || so.getId().longValue() == 0L) so.setId(getNewId());
		if(so.getTimestamp() == null) so.setTimestamp(timestamp());
		String kee = StringUtils.isBlank(key) ? so.getId().toString() : key;
		// store
		storeBean(kee, so, true, mut);
		
		if(isIndexable(so)) index(so, so.getClasstype());
		return so.getId();
	}

	public <T extends ScooldObject> T read(Class<T> clazz, String key) {
		if(StringUtils.isBlank(key) || clazz == null) return null;
		CF<String> cf = CasDAOFactory.OBJECTS;
		List<HColumn<String, String>> cols = readRow(key, cf,
				String.class, null, null, null, CasDAOFactory.DEFAULT_LIMIT, false);
		
		T so = fromColumns(clazz, cols);
		
		return so.getId() != null ? so : null;
	}
	
	public void update(ScooldObject so){
		update(so, mutator);
		mutator.execute();
		mutator.discardPendingMutations();
	}

	public void update(ScooldObject so, Mutator<String> mut){
		update(null, so, mut);
	}

	public <SN> void update(String key, ScooldObject so, Mutator<String> mut){
		if(so == null) return;
		String kee = (key == null) ? so.getId().toString() : key;
		storeBean(kee, so, false, mut);		
		
		if(isIndexable(so)) index(so, so.getClasstype());
	}

	public void delete(ScooldObject so){
		delete(so, mutator);
		mutator.execute();
		mutator.discardPendingMutations();
	}

	public void delete(ScooldObject so, Mutator<String> mut){
		delete(null, so, mut);
	}
	
	public <SN> void delete(String key, ScooldObject so, Mutator<String> mut){
		if(so == null) return ;
		String kee = (key == null) ? so.getId().toString() : key;
		unstoreBean(kee, so, mut);
		
		if(isIndexable(so)) unindex(so, so.getClasstype());
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

	public <CV, N, SUBN> CV getColumn(String key, CF<N> cf, N colName) {
		if(StringUtils.isBlank(key) || cf == null || colName == null || colName == null) 
			return null;
		HColumn<N, String> col = getHColumn(key, cf, colName);
		return (col != null) ? (CV) col.getValue() : null;
	}
	
	public Long getCounterColumn(String key){
		if(StringUtils.isBlank(key)) return null;
		HCounterColumn<String> col = getHCounterColumn(key);
		return (col != null) ? col.getValue() : 0L;
	}

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
	
	public HCounterColumn<String> getHCounterColumn(String key){
		CounterQuery<String, String> cq = null;
		try {
			cq = HFactory.createCounterColumnQuery(keyspace, 
					strser, strser);
			cq.setColumnFamily(CasDAOFactory.COUNTERS.getName());
			cq.setKey(key);
			cq.setName(CasDAOFactory.CN_COUNTS_COUNT);
		} catch (Exception e) {
			logger.log(Level.SEVERE, null, e);
		}
		return (cq == null) ? null : cq.execute().get();
	}

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
				page.setValue(lastk);
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

	public <N, T extends ScooldObject> ArrayList<String> readAllKeys( String classtype, String keysKey, 
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
	
	public <N, T extends ScooldObject> ArrayList<T> readAll(Class<T> clazz, String classtype,
			String keysKey, CF<N> keysCf, Class<N> colNameClass,
			N startKey, MutableLong page, MutableLong itemcount, int maxItems,
			boolean reverse, boolean colNamesAreKeys, boolean countOnlyColumns){

		if(StringUtils.isBlank(keysKey) || keysCf == null) return new ArrayList<T>();
		if(StringUtils.isBlank(classtype)) classtype = clazz.getSimpleName().toLowerCase();
		
		ArrayList<String> keyz = readAllKeys(classtype, keysKey, keysCf, colNameClass, startKey, 
				page, itemcount, maxItems, reverse, colNamesAreKeys, countOnlyColumns);		
		
		return readAll(clazz, keyz);
	}
	
	public <T extends ScooldObject> ArrayList<T> readAll(Class<T> clazz, List<String> keys){
		if(keys == null || keys.isEmpty() ) return new ArrayList<T>();
		
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
			q.setRange(null, null, false, CasDAOFactory.DEFAULT_LIMIT);
			
			for (Row<String, String, String> row : q.execute().get()) {
				T so = fromColumns(clazz, row.getColumnSlice().getColumns());
				if (so.getId() != null){
					list.set(index.get(row.getKey()), so);
				}
			}
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
		List<Row<String, String, String>> list = rows.getList();
		return rows == null ? new ArrayList<Row<String, String, String>> () : list;
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
		cq.setRange(null, null, CasDAOFactory.DEFAULT_LIMIT);

		result = cq.execute().get();
		return result;
	}

	public Long getBeanCount(String classtype){
		return getCounterColumn(classtype);
	}

	protected void updateBeanCount(String classtype, boolean decrement, Mutator<String> mut){
		if (decrement) {
			mutator.decrementCounter(classtype, CasDAOFactory.COUNTERS.getName(), 
					CasDAOFactory.CN_COUNTS_COUNT, 1);
		} else {
			mutator.incrementCounter(classtype, CasDAOFactory.COUNTERS.getName(), 
					CasDAOFactory.CN_COUNTS_COUNT, 1);
		}
	}
	
	/********************************************
	 *				SEARCH FUNCTIONS
	********************************************/
	private static Map<String, XContentBuilder> searchables;
	
	private static XContentBuilder getMapping(String type) throws Exception{
		return XContentFactory.jsonBuilder().startObject().startObject(type).
					startObject("_source").
						field("enabled", "true").
						field("compress", "true").
					endObject().endObject().endObject();
	}
	
	static{
		try {
			searchables = new HashMap<String, XContentBuilder>(){
				private static final long serialVersionUID = 1L;
				{
					put(PostType.QUESTION.toString(), null);
					put(PostType.FEEDBACK.toString(), null);
					put(PostType.GROUPPOST.toString(), null);
					put(PostType.REPLY.toString(), null);
//					put(PostType.BLACKBOARD.toString(), sourceOff);
//					put(Revision.classtype, sourceOff);
//					put(Translation.classtype, sourceOff);
//					put(Comment.classtype, null);
					put(User.classtype, null);
					put(School.classtype, null);
					put(Classunit.classtype, null);
					put(Group.classtype, null);
					put(Media.classtype, null);
					put(Message.classtype, null);
					put(Report.classtype, null);
					put(Tag.classtype, null);
				}
			};
		} catch (Exception e) {
			logger.log(Level.WARNING, null, e);
		}
	}
	
	public boolean isIndexable(ScooldObject so){
		return so == null ? false : searchables.containsKey(so.getClasstype());
	}
	
	public void index(ScooldObject so, String type){
		if(so == null) return;
		if(StringUtils.isBlank(type)) type = so.getClasstype();
		try {
			String data = getAsJSON(so, type, true);
			if (AbstractDAOFactory.IN_PRODUCTION) {
				QueueFactory.getDefaultQueue().push(data);
			} else {
				searchClient.prepareIndex(AbstractDAOFactory.INDEX_NAME, type, so.getId().toString()).				
					setSource(AbstractDAOUtils.getAnnotatedFields(so, Stored.class)).execute();
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, null, e);
		}
	}
	
	public void unindex(ScooldObject so, String type){
		if(so == null || StringUtils.isBlank(type)) return;
		try{
			String data = getAsJSON(so, type, false);
			if (AbstractDAOFactory.IN_PRODUCTION) {
				QueueFactory.getDefaultQueue().push(data);
			} else {
				searchClient.prepareDelete(AbstractDAOFactory.INDEX_NAME, type, so.getId().toString()).
					setType(type).execute();
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, null, e);
		}
	}
	
	public void reindexAll(){
		String name = AbstractDAOFactory.INDEX_NAME;
		if(!existsIndex()) createIndex();
		
		try {
			BulkRequestBuilder brb = searchClient.prepareBulk();
			for (Row<String, String, String> row : readAll()) {
				List<HColumn<String, String>> cols = row.getColumnSlice().getColumns();
				if(cols != null && !cols.isEmpty()){
					HColumn<String, String> ctype = row.getColumnSlice().getColumnByName("classtype");
					if(ctype != null){
						String classtype = ctype.getValue();
						if(searchables.containsKey(classtype)){
							Map<String, Object> data = AbstractDAOUtils.getAnnotatedFields(
									fromColumns(classtypeToClass(classtype), cols), Stored.class);
							brb.add(searchClient.prepareIndex(name, classtype, row.getKey()).
									setSource(data));
						}
					}
				}
			}
			brb.execute();
		} catch (Exception e) {
			logger.log(Level.WARNING, null, e);
		}
	}
	
	public void createIndex(){
		try {
			String name = AbstractDAOFactory.INDEX_NAME;

			if(existsIndex()) return;
			
			NodeBuilder nb = NodeBuilder.nodeBuilder();
			nb.settings().put("number_of_shards", "4");
			nb.settings().put("number_of_replicas", "0");
			nb.settings().put("auto_expand_replicas", "0-all");
			nb.settings().put("analysis.analyzer.default.type", "standard");
			nb.settings().putArray("analysis.analyzer.default.stopwords", 
					"arabic", "armenian", "basque", "brazilian", "bulgarian", "catalan", 
					"czech", "danish", "dutch", "english", "finnish", "french", "galician", 
					"german", "greek", "hindi", "hungarian", "indonesian", "italian", 
					"norwegian", "persian", "portuguese", "romanian", "russian", "spanish", 
					"swedish", "turkish"); 

			CreateIndexRequestBuilder create = searchClient.admin().indices().prepareCreate(name).
					setSettings(nb.settings().build());

			for (Entry<String, XContentBuilder> searchable : searchables.entrySet()) {
				create.addMapping(searchable.getKey(), getMapping(searchable.getKey()));
			}

			create.execute().actionGet();
		} catch (Exception e) {
			logger.log(Level.WARNING, null, e);
		}
	}
	
	public void deleteIndex(){
		try {
			String name = AbstractDAOFactory.INDEX_NAME;
			if(existsIndex()){
				searchClient.admin().indices().prepareDelete(name).execute();
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, null, e);
		}
	}
	
	public boolean existsIndex(){
		boolean exists = false;
		try {
			String name = AbstractDAOFactory.INDEX_NAME;
			exists = searchClient.admin().indices().prepareExists(name).execute().
					actionGet().exists();
		} catch (Exception e) {
			logger.log(Level.WARNING, null, e);
		}
		return exists;
	}
	
	public <T extends ScooldObject> ArrayList<T> readAndRepair(Class<T> clazz, 
			ArrayList<String> keys, MutableLong itemcount){
		ArrayList<T> results = readAll(clazz, keys);
		if(!results.isEmpty()){
			boolean done = false;
			for (int i = 0; i < results.size() && !done; i++) {
				T t = results.get(i);
				if(t != null){
					repairIndex(t.getClasstype(), results, keys, itemcount);
					done = true;
				}
			}
		}
		return results;
	}
	
	public ArrayList<String> findTerm(String type, MutableLong page, MutableLong itemcount, 
			String field, Object term){
		return findTerm(type, page, itemcount, field, term, null, true, MAX_ITEMS);
	}
	
	public ArrayList<String> findTerm(String type, MutableLong page, MutableLong itemcount, 
			String field, Object term, String sortfield, boolean reverse, int max){
		return searchQuery(type, page, itemcount, QueryBuilders.termQuery(field, term), 
				sortfield, reverse, max);
	}
	
	public ArrayList<String> findPrefix(String type, MutableLong page, MutableLong itemcount, 
			String field, String prefix){
		return findPrefix(type, page, itemcount, field, prefix, null, true, MAX_ITEMS);
	}
	
	public ArrayList<String> findPrefix(String type, MutableLong page, MutableLong itemcount, 
			String field, String prefix, String sortfield, boolean reverse, int max){
		return searchQuery(type, page, itemcount, QueryBuilders.prefixQuery(field, prefix), 
				sortfield, reverse, max);
	}
	
	public ArrayList<String> findQuery(String type, MutableLong page, MutableLong itemcount, 
			String query){
		return findQuery(type, page, itemcount, query, null, true, MAX_ITEMS);
	}
	
	public ArrayList<String> findQuery(String type, MutableLong page, MutableLong itemcount, 
			String query, String sortfield, boolean reverse, int max){
		return searchQuery(type, page, itemcount, QueryBuilders.queryString(query), 
				sortfield, reverse, max);
	}

	public ArrayList<String> findWildcard(String type, MutableLong page, MutableLong itemcount, 
			String field, String wildcard){
		return findWildcard(type, page, itemcount, field, wildcard, null, true, MAX_ITEMS);
	}
	
	public ArrayList<String> findWildcard(String type, MutableLong page, MutableLong itemcount, 
			String field, String wildcard, String sortfield, boolean reverse, int max){
		return searchQuery(type, page, itemcount, QueryBuilders.wildcardQuery(field, wildcard), 
				sortfield, reverse, max);
	}
	
	public ArrayList<String> findTagged(String type, MutableLong page, MutableLong itemcount, 
			ArrayList<String> tags){
		OrFilterBuilder tagFilter = FilterBuilders.orFilter(
				FilterBuilders.termFilter("tags", tags.remove(0)));

		if (!tags.isEmpty()) {
			//assuming clean & safe tags here
			for (String tag : tags) {
				tagFilter.add(FilterBuilders.termFilter("tags", tag));
			}
		}
		// The filter looks like this: ("tag1" OR "tag2" OR "tag3") AND "type"
		AndFilterBuilder andFilter = FilterBuilders.andFilter(tagFilter);
		andFilter.add(FilterBuilders.termFilter("type", type));
		QueryBuilder query = QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), andFilter);

		return searchQuery(type, page, itemcount, query, null, true, MAX_ITEMS);
	}

	public ArrayList<String> findSimilar(String type, String filterKey, String[] fields, String liketext, int max){
		return searchQuery(type, null, null, QueryBuilders.filteredQuery(QueryBuilders.moreLikeThisQuery(fields).
			likeText(liketext), FilterBuilders.notFilter(FilterBuilders.idsFilter(type).addIds(filterKey))), 
				SortBuilders.scoreSort().order(SortOrder.DESC), max);
	}

	public ArrayList<Tag> findTags(String keyword, int max){
		ArrayList<Tag> keys = new ArrayList<Tag>();
		if(StringUtils.isBlank(keyword)) return keys;
		String type = Tag.classtype;
		
		SearchResponse response = searchClient.prepareSearch(AbstractDAOFactory.INDEX_NAME)
			.setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setTypes(type)
			.setQuery(QueryBuilders.wildcardQuery("tag", keyword.concat("*")))
			.addSort(SortBuilders.fieldSort("count").order(SortOrder.DESC))
			.setSize(max).execute().actionGet();

		try {
			for (SearchHit hit : response.getHits()) {
				Map<String, Object> src = hit.getSource();
				if (src != null){
					Tag tag = new Tag();
					BeanUtils.populate(tag, src);
					keys.add(tag);
				}
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, null, e);
		}
		
		return keys;
	}
	
	private ArrayList<String> searchQuery(String type, MutableLong page, MutableLong itemcount,
			QueryBuilder query, String sortfield, boolean reverse, int max){
		SortOrder order = reverse ? SortOrder.DESC : SortOrder.ASC;
		SortBuilder sort = StringUtils.isBlank(sortfield) ? null : 
				SortBuilders.fieldSort(sortfield).order(order);
		return searchQuery(type, page, itemcount, query, sort, max);
	}
	
	private ArrayList<String> searchQuery(String type, MutableLong page, MutableLong itemcount, 
			QueryBuilder query, SortBuilder sort, int max){
		ArrayList<String> keys = new ArrayList<String>();
		if(StringUtils.isBlank(type) || query == null) return keys;
		if(sort == null) sort = SortBuilders.fieldSort("timestamp").order(SortOrder.DESC);
		int start = (page == null || page.intValue() < 1) ? 0 : (page.intValue() - 1) * max;
		
		try{
			SearchResponse response = searchClient.prepareSearch(AbstractDAOFactory.INDEX_NAME)
				.setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setTypes(type)
				.setQuery(query).addSort(sort).setFrom(start).setSize(max).execute().actionGet();

			SearchHits hits = response.getHits();
			if(itemcount != null)	itemcount.setValue(hits.getTotalHits());

			for (SearchHit hit : hits) {
				keys.add(hit.getId());
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, null, e);
		}
		
		return keys;
	}
	
	private <T extends ScooldObject> void repairIndex(String type, ArrayList<T> list, 
			ArrayList<String> keys, MutableLong itemcount) {
		int countRemoved = 0;
		if(list.contains(null)){
			BulkRequestBuilder brb = searchClient.prepareBulk();
			for (int i = 0; i < list.size(); i++) {
				String id = keys.get(i);
				if(id == null){
					brb.add(searchClient.prepareDelete(AbstractDAOFactory.INDEX_NAME, 
							type, id).request());
					countRemoved++;
				}
			}
			brb.execute();
			list.removeAll(Collections.singleton(null));
		}
		if(itemcount != null && countRemoved > 0) {
			itemcount.setValue(itemcount.toLong() - countRemoved);
		}
	}
	

	/********************************************
	 *				MISC FUNCTIONS
	********************************************/
	
	public boolean voteUp(Long userid, Votable<Long> votable){
		return vote(userid, votable, true, voteLockAfter, CasDAOFactory.VOTE_LOCKED_FOR_SEC);
	}
	
	public boolean voteDown(Long userid, Votable<Long> votable){
		return vote(userid, votable, false, voteLockAfter, CasDAOFactory.VOTE_LOCKED_FOR_SEC);
	}

	protected boolean vote(Long userid, Votable<Long> votable, boolean isUpvote,
			long voteLockAfterMs, int voteLockedForSec) {
		//no voting on your own stuff!
		if(votable == null || userid == null || userid.equals(votable.getUserid()) ||
				votable.getId() == null) return false;

		boolean voteSuccess = false;
//		int upOrDown = updown;
		CF<String> cf = CasDAOFactory.VOTES;
		String key = votable.getId().toString();
		String colName = userid.toString();
		
		//read vote for user & id
		HColumn<String, String> vote = getHColumn(key, cf, colName);

		// if vote exists check timestamp for recent correction,
		// otherwise insert new vote
		Integer votes = (votable.getVotes() == null) ? 0 : votable.getVotes();
		Integer newVotes = votes;

		if (vote != null){
		//allow correction of vote within 2 min
			long timestamp = vote.getClock();
			long now = keyspace.createClock();
			boolean wasUpvote = BooleanUtils.toBoolean(vote.getValue()); //up or down

			if((timestamp + voteLockAfterMs) > now &&
					BooleanUtils.xor(new boolean[]{isUpvote, wasUpvote})) {
				// clear vote and restore votes to original count
				removeColumn(key, cf, colName);
				newVotes = wasUpvote ? --votes : ++votes;
				votable.setVotes(newVotes);
				voteSuccess = true;
			}
		}else{
			// save new vote & set expiration date to 1 month
			// users can vote again after vote lock period
			putColumn(key, cf, colName, Boolean.toString(isUpvote), voteLockedForSec);
			newVotes = isUpvote ? ++votes : --votes;
			votable.setVotes(newVotes);
			voteSuccess = true;
		}

		return voteSuccess;
	}

	public void setSystemColumn(String colName, String colValue, int ttl) {
		if(StringUtils.isBlank(colName)) return ;
		if (StringUtils.isBlank(colValue)) {
			removeColumn(AbstractDAOFactory.SYSTEM_OBJECTS_KEY, CasDAOFactory.OBJECTS, colName);
		} else {
			HColumn<String, String> col = HFactory.createColumn(colName, colValue);
			if (ttl > 0 ) col.setTtl(ttl);
			mutator.insert(AbstractDAOFactory.SYSTEM_OBJECTS_KEY, CasDAOFactory.OBJECTS.getName(), col);
			mutator.discardPendingMutations();
		}
	}

	public String getSystemColumn(String colName) {
		if(StringUtils.isBlank(colName)) return null; 
		return getColumn(AbstractDAOFactory.SYSTEM_OBJECTS_KEY, CasDAOFactory.OBJECTS, colName);
	}
	
	public Map<String, String[]> getSystemColumns(){
		Map<String, String[]> map = new HashMap<String, String[]>();
		for (HColumn<String, String> hColumn : readRow(AbstractDAOFactory.SYSTEM_OBJECTS_KEY, 
				CasDAOFactory.OBJECTS, String.class, null, null, null, 
				AbstractDAOFactory.DEFAULT_LIMIT, true)) {
			map.put(hColumn.getName(), new String[]{hColumn.getValue(), 
				Integer.toString(hColumn.getTtl())});
		}
		return map;
	}
	
	protected static long convertMsTimeToCasTime(Keyspace ks, long ms){
		long t1 = ks.createClock();
		long t2 = System.currentTimeMillis();
		long delta = Math.abs(t2 - t1);

		if (delta < 100) {
		  return ms;
		} else if (delta >= 1000 ) {
		  return ms * 1000;
		} else if (delta <= 1000) {
		  return ms / 1000;
		}else{
			return ms;
		}
	}

	private <SN> void storeBean(String key, ScooldObject so, boolean creation, Mutator<String> mut){
		if(so == null) return;
		CF<String> cf = CasDAOFactory.OBJECTS;
		Long id = so.getId();
		Map<String, Object> propsMap = AbstractDAOUtils.getAnnotatedFields(so, Stored.class);
		
		propsMap.put(CasDAOFactory.CN_ID, id);

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
		updateBeanCount(so.getClasstype(), false, mut);
	}

	private <SN> void unstoreBean(String key, ScooldObject so, Mutator<String> mut){
		if(so == null) return;
		CF<String> cf = CasDAOFactory.OBJECTS;
		deleteRow(key, cf, mut);
		updateBeanCount(so.getClasstype(), true, mut);
	}
	
	private void initIdGen(){
		String workerID = System.getProperty("com.scoold.workerid");
		workerId = NumberUtils.toLong(workerID, maxWorkerId + 1);
				
		if (workerId > maxWorkerId || workerId < 0) {
			workerId = new Random().nextInt((int) maxWorkerId + 1);
		}

//		if (dataCenterId > maxDataCenterId || dataCenterId < 0) {
//			dataCenterId =  new Random().nextInt((int) maxDataCenterId+1);
//		}
	}
	
	public synchronized Long getNewId() {
		// OLD simple version - only unique for this JVM
//		return HFactory.createClockResolution(ClockResolution.MICROSECONDS_SYNC).
//				createClock() - TIMER_OFFSET - 1000;
		
		// NEW version - unique across JVMs as long as each has a different workerID
		// based on Twitter's Snowflake algorithm
		long timestamp = System.currentTimeMillis();

		if (lastTimestamp == timestamp) {
			sequence = (sequence + 1) & sequenceMask;
			if (sequence == 0) {
				timestamp = tilNextMillis(lastTimestamp);
			}
		} else {
			sequence = 0;
		}

		if (timestamp < lastTimestamp) {
			throw new IllegalStateException(String.format("Clock moved backwards.  "
					+ "Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
		}

		lastTimestamp = timestamp;
		return	 ((timestamp - TIMER_OFFSET) << timestampLeftShift) | 
								(dataCenterId << dataCenterIdShift) | 
										(workerId << workerIdShift) | 
														 (sequence) ;
		
	}
	
	private long tilNextMillis(long lastTimestamp) {
		long timestamp = System.currentTimeMillis();

		while (timestamp <= lastTimestamp) {
			timestamp = System.currentTimeMillis();
		}

		return timestamp;
	}

	public void addNumbersortColumn(String key, CF<String> cf, Long id,
			Number number, Number oldNumber, Mutator<String> mut){
		if((oldNumber != null && oldNumber.equals(number)) || cf == null) return;
		if(StringUtils.isBlank(key)) key = CasDAOFactory.DEFAULT_KEY;

		String compositeKey = number.toString().concat(
				AbstractDAOFactory.SEPARATOR).concat(id.toString());
		
		addInsertion(new Column<String, String>(key, cf, compositeKey, id.toString()), mut);

		//finally clean up old column
		if(oldNumber != null){
			removeNumbersortColumn(key, cf, id, oldNumber, mut);
		}
	}

	public void removeNumbersortColumn(String key, CF<String> cf, Long id, 
			Number number, Mutator<String> mut){

		if(cf == null || id == null || number == null) return;
		if(StringUtils.isBlank(key)) key = CasDAOFactory.DEFAULT_KEY;

		String compositeKey = number.toString().concat(AbstractDAOFactory.SEPARATOR)
				.concat(id.toString());
		addDeletion(new Column<String, String>(key, cf, compositeKey, null), mut);
	}
	
	public final Mutator<String> createMutator(){
		return HFactory.createMutator(keyspace, strser);
	}
	
	private <T extends ScooldObject> T fromColumns(Class<T> clazz,
			List<HColumn<String, String>> cols) {
		if (cols == null ) 	return null;

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
