
package com.scoold.core;


/**
 * Scoold persistable object
 *
 * @param <PK> - the id of the object AKA PRIMARY KEY
 * @author alexb
 */
public interface ScooldObject {

		public Long getId();

		public void setId(Long id);

		public String getUuid();

		public void setUuid(String uuid);

        public Long create();

        public void update();

        public void delete();

		public Long getTimestamp();

		public void setTimestamp(Long t);
		
}
