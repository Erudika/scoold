/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.core;

import com.scoold.core.Post.PostType;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractDAOUtils;
import com.scoold.db.cassandra.CasDAOUtils;
import com.scoold.util.AmazonQueue;
import com.scoold.util.Queue;
import com.scoold.util.QueueFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.mutable.MutableLong;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.OrFilterBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
/**
 *
 * @author alexb
 */

public final class Search{

	public static final String INDEX_NAME = "scoold";
	public static final String SCOOLD_INDEX = "ScooldIndex";
	
	private static final int MAX_ITEMS = AbstractDAOFactory.MAX_ITEMS_PER_PAGE;
    private static final Logger logger = Logger.getLogger(Search.class.getName());
	private static Client searchClient;
	private static Queue<String> queue;
	
    public Search(){
		if(searchClient == null){
			refreshClient();
		}
	}
	
	public static Client getClient(){
		return searchClient;
	}

	private static <E extends Serializable> Queue<E> getQueue(){
		if(queue == null) queue = QueueFactory.getQueue(SCOOLD_INDEX);
		return (Queue<E>) queue;
	}
	
	public static void index(Searchable<?> so){
		getQueue().push(getIndexableData(so, "index"));
	}

	public static void unindex(Searchable<?> so){
		getQueue().push(getIndexableData(so, "unindex"));
	}
	
	private static String getIndexableData(Searchable<?> so, String op){
		String json = "";
		if(so == null) return json;
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.createObjectNode(); // will be of type ObjectNode
		
		ScooldObject bean = (ScooldObject) so;
		String type = so.getClass().getSimpleName().toLowerCase();
		if(so.getClass().equals(Post.class) &&
				((Post) so).getPostType() == PostType.FEEDBACK){
			type = PostType.FEEDBACK.name().toLowerCase();
		}
		
		try {
			((ObjectNode) rootNode).put("_id", bean.getId().toString());
			((ObjectNode) rootNode).put("_type", type);
			if("index".equals(op)){
				((ObjectNode) rootNode).putPOJO("_data", 
						AbstractDAOUtils.getAnnotatedFields(bean, Indexed.class));
			}
			json = mapper.writeValueAsString(rootNode);
		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		
		return json;
	}

	public <T extends Searchable<?>> ArrayList<T> findByKeyword(Class<T> clazz,
			MutableLong page, MutableLong itemcount, String keywords, int max){

		if(searchClient == null || StringUtils.isBlank(keywords))
			return new ArrayList<T>(0);

		Long p = CasDAOUtils.toLong(page);
		int start = (p == null) ? 0 : (p.intValue() - 1) * max;
		// Types are used for posts: e.g. post of type answer, feedback, etc.
		String type = clazz.getSimpleName().toLowerCase();
		ArrayList<String> keys = new ArrayList<String>();
		ArrayList<T> list = new ArrayList<T>();
		
		try {
			SearchResponse response = searchClient.prepareSearch(INDEX_NAME)
				.setSearchType(SearchType.DFS_QUERY_AND_FETCH).setTypes(type)
				.setQuery(QueryBuilders.queryString(keywords).useDisMax(true))
				.setFrom(start).setSize(max).setExplain(true).execute().actionGet();

			SearchHits hits = response.getHits();
			if(itemcount != null)	itemcount.setValue(hits.getTotalHits());
			if(page != null)	page.setValue(page.longValue() + 1);

			for (SearchHit hit : hits) {
				if(clazz.equals(Post.class)){
					SearchHitField qid = hit.field("parentpostid");
					if(qid != null && qid.getValue() != null) {
						keys.add(qid.getValue().toString());
					} else {
						keys.add(hit.getId());
					}
				}else{
					keys.add(hit.getId());
				}
			}

			T so = clazz.newInstance();
			list = (ArrayList<T>) so.readAllForKeys(keys);
			repairIndex(clazz, list, keys, itemcount);
		} catch (Exception e) {
			logger.log(Level.WARNING, null, e);
			refreshClient();
		}
		
		return list;
	}

	public <T extends Searchable<?>> ArrayList<T> findByKeyword(Class<T> clazz,	
			MutableLong page, MutableLong itemcount, String keywords){
		return findByKeyword(clazz, page, itemcount, keywords, MAX_ITEMS);
	}
	
	public <T extends Searchable<?>> ArrayList<T> findByKeyword(String type, 
			String keywords, int max){
		return findByKeyword((Class<T>) AbstractDAOUtils.getClassname(type), 
				new MutableLong(0), new MutableLong(0), keywords, max);
	}

	public ArrayList<Tag> findTag(String keywords, int max){
		if(searchClient == null || StringUtils.isBlank(keywords))
			return new ArrayList<Tag>(0);

		ArrayList<Tag> tags = new ArrayList<Tag>();
		try {
			SearchResponse response = searchClient.prepareSearch(INDEX_NAME)
				.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
				.setTypes(Tag.class.getSimpleName().toLowerCase())
				.setQuery(QueryBuilders.wildcardQuery("tag", keywords.concat("*")))
				.setSize(max).setExplain(true).execute().actionGet();

			SearchHits hits = response.getHits();
			
			for (SearchHit hit : hits) {
				Tag tag = new Tag((String) hit.getSource().get("tag"));
				tag.setId(NumberUtils.toLong(hit.getId()));
				tags.add(tag);
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, null, e);
			refreshClient();
		}
		
		return tags;
	}

	public ArrayList<Post> findPostsForTags(PostType type, ArrayList<String> tags, 
			MutableLong page, MutableLong itemcount){

		if(searchClient == null || tags == null || tags.isEmpty())
			return new ArrayList<Post>(0);

		Long p = CasDAOUtils.toLong(page);
		int start = (p == null) ? 0 : (p.intValue() - 1) * MAX_ITEMS;
		
		ArrayList<String> keys = new ArrayList<String>();
		ArrayList<Post> list = new ArrayList<Post>();
		
		try {
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
			andFilter.add(FilterBuilders.termFilter("type", type.name()));

			SearchResponse response = searchClient.prepareSearch(INDEX_NAME)
				.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
				.setTypes(Post.class.getSimpleName().toLowerCase())
				.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
					andFilter))
				.addSort(SortBuilders.fieldSort("_id").order(SortOrder.DESC))
				.setFrom(start).setSize(MAX_ITEMS).setExplain(true).execute().actionGet();
			
			SearchHits hits = response.getHits();
			for (SearchHit hit : hits) {
				keys.add(hit.getId());
			}
			
			if (itemcount != null) itemcount.setValue(hits.getTotalHits());
			if (page != null) page.setValue(page.longValue() + 1);
			
			list = new Post().readAllForKeys(keys);
			repairIndex(Post.class, list, keys, itemcount);
		} catch (Exception e) {
			logger.log(Level.WARNING, null, e);
			refreshClient();
		}

		return list;
	}

	public ArrayList<Post> findSimilarQuestions(Post toThis, int max){
		if(searchClient == null || toThis == null)
			return new ArrayList<Post>(0);

		ArrayList<String> keys = new ArrayList<String>();
		ArrayList<Post> list = new ArrayList<Post>();
		
		try {
			String likeTxt = toThis.getTitle().concat(" ").concat(toThis.getBody()).
					concat(" ").concat(toThis.getTags());

			SearchResponse response = searchClient.prepareSearch(INDEX_NAME)
				.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
				.setTypes(Post.class.getSimpleName().toLowerCase())
				.setQuery(QueryBuilders.filteredQuery(
					QueryBuilders.moreLikeThisQuery("title", "body", "tags")
						.minWordLen(5).maxWordLen(1000).likeText(likeTxt),
					FilterBuilders.termFilter("type", PostType.QUESTION.name())))
				.addSort(SortBuilders.scoreSort().order(SortOrder.DESC))
				.setSize(MAX_ITEMS).setExplain(true).execute().actionGet();

			SearchHits hits = response.getHits();

			for (SearchHit hit : hits) {
				keys.add(hit.getId());
			}
			
			list = new Post().readAllForKeys(keys);
			repairIndex(Post.class, list, keys, null);
		} catch (Exception e) {
			logger.log(Level.WARNING, null, e);
			refreshClient();
		}

		return list;
	}

	public ArrayList<Post> findUnansweredQuestions(MutableLong page, MutableLong itemcount){
		if(searchClient == null) return new ArrayList<Post>(0);

		Long p = CasDAOUtils.toLong(page);
		int start = (p == null) ? 0 : (p.intValue() - 1) * MAX_ITEMS;
		
		ArrayList<String> keys = new ArrayList<String>();
		ArrayList<Post> list = new ArrayList<Post>();

		try {
			SearchResponse response = searchClient.prepareSearch(INDEX_NAME)
				.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
				.setTypes(Post.class.getSimpleName().toLowerCase())
				.setQuery(QueryBuilders.filteredQuery(
					QueryBuilders.fieldQuery("type", PostType.QUESTION.name()),
					FilterBuilders.termFilter("answercount", 0L)))
				.addSort(SortBuilders.fieldSort("_id").order(SortOrder.ASC))
				.setFrom(start).setSize(MAX_ITEMS).setExplain(true).execute().actionGet();

			SearchHits hits = response.getHits();

			for (SearchHit hit : hits) {
				keys.add(hit.getId());
			}
			
			if(itemcount != null)	itemcount.setValue(hits.getTotalHits());
			if(page != null)	page.setValue(page.longValue() + 1);
			
			list = new Post().readAllForKeys(keys);
			repairIndex(Post.class, list, keys, itemcount);
		} catch (Exception e) {
			logger.log(Level.WARNING, null, e);
			refreshClient();
		}

		return new Post().readAllForKeys(keys);
	}

	public ArrayList<User> findUser(String keywords, int max){
		if(searchClient == null || StringUtils.isBlank(keywords))
			return new ArrayList<User>(0);

		ArrayList<String> keys = new ArrayList<String>();
		ArrayList<User> list = new ArrayList<User>();
		
		try {
			SearchResponse response = searchClient.prepareSearch(INDEX_NAME)
				.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
				.setTypes(User.class.getSimpleName().toLowerCase())
				.setQuery(QueryBuilders.wildcardQuery("fullname",
					keywords.concat("*")))
				.setSize(max).setExplain(true).execute().actionGet();

			SearchHits hits = response.getHits();

			for (SearchHit hit : hits) {
				keys.add(hit.getId());
			}
			
			list = new User().readAllForKeys(keys);
			repairIndex(User.class, list, keys, null);
		} catch (Exception e) {
			logger.log(Level.WARNING, null, e);
			refreshClient();
		}
		
		return list;
	}

	public <T extends Searchable<?>> long getHits(Class<T> clazz, String keywords){

		if(searchClient == null || StringUtils.isBlank(keywords))
			return 0;

		CountResponse response = null;
		try {
			response = searchClient.prepareCount(INDEX_NAME)
				.setTypes(clazz.getSimpleName().toLowerCase())
				.setQuery(QueryBuilders.queryString(keywords).useDisMax(true))
				.execute().actionGet();
		} catch (Exception e) {
			logger.log(Level.WARNING, null, e);
			refreshClient();
		}
		
		return (response == null) ? 0L : response.getCount();
	}
	
	private <T extends Searchable<?>> void repairIndex(Class<T> clazz, ArrayList<T> list, 
			ArrayList<String> keys, MutableLong itemcount) throws Exception {
		int countRemoved = 0;
		if(list.contains(null)){
			for (int i = 0; i < list.size(); i++) {
				if(list.get(i) == null){
					String id = keys.get(i);
					T nul = clazz.newInstance();
					nul.setId(NumberUtils.toLong(id));
					unindex(nul);
					countRemoved++;
				}
			}
			list.removeAll(Collections.singleton(null));
		}
		if(itemcount != null && countRemoved > 0) {
			itemcount.setValue(itemcount.toLong() - countRemoved);
		}
	}
	
	private void refreshClient(){
		try {
			if(searchClient != null){
				searchClient.close();
				searchClient = null;
			}
			boolean inprod = "true".equals(System.getProperty("com.scoold.production", "false"));
			NodeBuilder nb = NodeBuilder.nodeBuilder();
			nb.clusterName(Search.INDEX_NAME);
			if(inprod){
				nb.settings().put("cloud.aws.region", "eu-west-1");
				nb.settings().put("cloud.aws.access_key", AmazonQueue.ACCESSKEY);
				nb.settings().put("cloud.aws.secret_key", AmazonQueue.SECRETKEY);
				nb.settings().put("client.transport.sniff", false);
				nb.settings().put("network.tcp.keep_alive", true);
				nb.settings().put("discovery.type", "ec2");
				nb.settings().put("discovery.ec2.groups", "elasticsearch");
				nb.settings().put("discovery.ec2.availability_zones", "eu-west-1a");
			}

			searchClient = new TransportClient(nb.settings());
			if(inprod){
				String[] eshosts = System.getProperty("com.scoold.eshosts", "localhost").split(",");
				for (String host : eshosts) {
					((TransportClient) searchClient).addTransportAddress(
							new InetSocketTransportAddress(host, 9300));				
				}
			}else{
				((TransportClient) searchClient).addTransportAddress(
							new InetSocketTransportAddress("localhost", 9300));				
			}		
			searchClient.admin().indices().refresh(Requests.refreshRequest()).actionGet();			
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to refresh search client: {0}", e.toString());		
			searchClient.close();
			searchClient = null;
		}
	}
}
