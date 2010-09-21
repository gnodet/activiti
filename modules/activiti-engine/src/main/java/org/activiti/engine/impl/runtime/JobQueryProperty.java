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

package org.activiti.engine.impl.runtime;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.runtime.JobQuery;

/**
 * Contains the possible properties that can be used in a {@link JobQuery}.
 * 
 * @author Joram Barrez
 */
public class JobQueryProperty {
  
  private static final Map<String, JobQueryProperty> properties = new HashMap<String, JobQueryProperty>();

  public static final JobQueryProperty ID = new JobQueryProperty("ID_");
  public static final JobQueryProperty PROCESS_INSTANCE_ID = new JobQueryProperty("J.PROCESS_INSTANCE_ID_");
  public static final JobQueryProperty EXECUTION_ID = new JobQueryProperty("J.EXECUTION_ID_");
  public static final JobQueryProperty DUEDATE = new JobQueryProperty("J.DUEDATE_");
  public static final JobQueryProperty RETRIES = new JobQueryProperty("J.RETRIES_");

  private String name;

  public JobQueryProperty(String name) {
    this.name = name;
    properties.put(name, this);
  }

  public String getName() {
    return name;
  }
  
  public static JobQueryProperty findByName(String propertyName) {
    return properties.get(propertyName);
  }

}
