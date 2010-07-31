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

package org.activiti.engine.impl.persistence.history;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.persistence.PersistentObject;

/**
 * @author Christian Stettler
 */
public abstract class HistoricScopeInstanceImpl implements PersistentObject {

  private String id;
  protected String processInstanceId;
  protected String processDefinitionId;
  protected Date startTime;
  protected Date endTime;
  protected Long durationInMillis;

  protected HistoricScopeInstanceImpl() {
    // for ibatis
  }

  protected HistoricScopeInstanceImpl(String processInstanceId, String processDefinitionId, Date startTime) {
    if (processInstanceId == null) {
      throw new IllegalArgumentException("Process instance id must not be null");
    }
    if (processDefinitionId == null) {
      throw new IllegalArgumentException("Process definition id must not be null");
    }
    if (startTime == null) {
      throw new IllegalArgumentException("Start time must not be null");
    }

    this.processInstanceId = processInstanceId;
    this.processDefinitionId = processDefinitionId;
    this.startTime = startTime;
  }
  
  public Object getPersistentState() {
    Map<String, Object> persistentState = new HashMap<String, Object>();
    persistentState.put("endTime", endTime);
    persistentState.put("durationInMillis", durationInMillis);
    return persistentState;
  }

  protected void internalMarkEnded(Date endTime) {
    if (endTime == null) {
      throw new IllegalArgumentException("End time must not be null");
    }
    if (endTime.getTime() < startTime.getTime()) {
      throw new IllegalArgumentException("end time must not be before start time");
    }

    this.endTime = endTime;

    durationInMillis = endTime.getTime() - startTime.getTime();
  }
  
  // getters and setters //////////////////////////////////////////////////////

  public String getProcessInstanceId() {
    return processInstanceId;
  }

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  public Date getStartTime() {
    return startTime;
  }

  public Date getEndTime() {
    return endTime;
  }

  public Long getDurationInMillis() {
    return durationInMillis;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
