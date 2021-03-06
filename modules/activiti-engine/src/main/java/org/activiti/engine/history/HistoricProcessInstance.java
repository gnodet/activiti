/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.engine.history;

import java.util.Date;

/** A single execution of a whole process definition that is stored permanently.
 * 
 * @author Christian Stettler
 */
public interface HistoricProcessInstance {
  
  String getId();
  
  String getProcessInstanceId();
  
  String getBusinessKey();

  String getProcessDefinitionId();

  Date getStartTime();

  Date getEndTime();

  Long getDurationInMillis();

  String getEndActivityId();
}
