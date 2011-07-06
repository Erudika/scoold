/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.util;

import com.scoold.core.Indexed;
import com.scoold.core.Post;
import com.scoold.core.Post.PostType;
import com.scoold.core.ScooldObject;
import com.scoold.core.Searchable;
import com.scoold.db.AbstractDAOUtils;
import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

/**
 *
 * @author alexb
 */
public abstract class QueueFactory {

	public static final String SCOOLD_INDEX = "ScooldIndex";
	
	public static <E extends Serializable> Queue<E> getQueue(String name){
//		return new HazelcastQueue<E>(name);
		return new AmazonQueue<E>(name);
	}
	
	public static <E extends Serializable> E getIndexableData(Searchable<?> so, String op){
		String json = "";
		if(so == null || StringUtils.isBlank(op)) return (E) json;
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.createObjectNode(); // will be of type ObjectNode
		
		ScooldObject bean = (ScooldObject) so;
		String type = so.getClass().getSimpleName().toLowerCase();
		if(so.getClass().equals(Post.class) &&
				((Post) so).getPostType() == PostType.FEEDBACK){
			type = PostType.FEEDBACK.name().toLowerCase();
		}
		
		HashMap<String, Object> map = AbstractDAOUtils.getAnnotatedFields(bean, Indexed.class);
		
		try {
			((ObjectNode) rootNode).put("_id", bean.getId().toString());
			((ObjectNode) rootNode).put("_type", type);
			((ObjectNode) rootNode).put("_op", op);
			((ObjectNode) rootNode).putPOJO("_data", map);
			json = mapper.writeValueAsString(rootNode);
		} catch (Exception ex) {
			Logger.getLogger(QueueFactory.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		return (E) json;
	}
	
}
