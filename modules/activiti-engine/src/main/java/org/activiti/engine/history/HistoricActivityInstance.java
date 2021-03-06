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

/**
 * Represents a historic activity instance that is stored permanently.
 * 
 * @author Christian Stettler
 */
public interface HistoricActivityInstance {

  String getActivityId();

  String getActivityName();

  String getActivityType();

  String getProcessDefinitionId();

  String getProcessInstanceId();

  String getExecutionId();

  String getAssignee();

  Date getStartTime();

  Date getEndTime();

  Long getDurationInMillis();
}
