/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.engine;



/**
 * @author Joram Barrez
 */
public interface TablePageQuery {
  
  /** 
   * the name of the table of which a page must be fetched. 
   */
  TablePageQuery tableName(String tableName);
  
  /**
   * orders the resulting table page rows by the given column in ascending order. 
   */
  TablePageQuery orderAsc(String column);
  
  /**
   * orders the resulting table page rows by the given column in descending order. 
   */
  TablePageQuery orderDesc(String column);
  
  /**
   * executes the query and returns the {@link TablePage}. 
   */
  TablePage listPage(int firstResult, int maxResults);
}
