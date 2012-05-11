
package com.scoold.core;


/**
 * Scoold persistable object
 *
 * @author alexb
 */
public interface ScooldObject {

		public Long getId();

		public void setId(Long id);

        public Long create();

        public void update();

        public void delete();

		public Long getTimestamp();

		public void setTimestamp(Long t);
		
		public String getClasstype();
}
