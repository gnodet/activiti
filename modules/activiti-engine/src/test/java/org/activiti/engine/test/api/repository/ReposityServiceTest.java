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

package org.activiti.engine.test.api.repository;

import java.util.List;

import junit.framework.Assert;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.test.ActivitiInternalTestCase;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.test.Deployment;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class ReposityServiceTest extends ActivitiInternalTestCase {

  @Deployment(resources = { "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml" })
  public void testDeleteDeploymentWithRunningInstances() {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
    assertEquals(1, processDefinitions.size());
    ProcessDefinition processDefinition = processDefinitions.get(0);

    runtimeService.startProcessInstanceById(processDefinition.getId());

    // Try to delete the deployment
    try {
      repositoryService.deleteDeployment(processDefinition.getDeploymentId());
      fail("ActivitiException expected");
    } catch (RuntimeException ae) {
      // Exception expected when deleting deployment with running process
    }
  }

  @Deployment(resources = { "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml" })
  public void testDeleteDeploymentCascadeWithRunningInstances() {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
    assertEquals(1, processDefinitions.size());
    ProcessDefinition processDefinition = processDefinitions.get(0);

    runtimeService.startProcessInstanceById(processDefinition.getId());

    // Try to delete the deployment, no exception should be thrown
    repositoryService.deleteDeploymentCascade(processDefinition.getDeploymentId());
  }
  
  @Deployment(resources = { 
          "org/activiti/examples/taskforms/VacationRequest.bpmn20.xml", 
          "org/activiti/examples/taskforms/approve.form", 
          "org/activiti/examples/taskforms/request.form", 
          "org/activiti/examples/taskforms/adjustRequest.form" })
  public void testGetStartFormByProcessDefinitionId() {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
    assertEquals(1, processDefinitions.size());
    ProcessDefinition processDefinition = processDefinitions.get(0);
    
    Object startForm = repositoryService.getStartFormById(processDefinition.getId());
    Assert.assertNotNull(startForm);
  }
  
  @Deployment(resources = { "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml" })
  public void testGetStartFormByProcessDefinitionIdWithoutStartform() {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
    assertEquals(1, processDefinitions.size());
    ProcessDefinition processDefinition = processDefinitions.get(0);
    
    Object startForm = repositoryService.getStartFormById(processDefinition.getId());
    Assert.assertNull(startForm);
  }
  
  @Deployment(resources = {
  "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml"})
public void testStartProcessInstanceById() {
  List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
  assertEquals(1, processDefinitions.size());

  ProcessDefinition processDefinition = processDefinitions.get(0);
  assertEquals("oneTaskProcess", processDefinition.getKey());
  assertNotNull(processDefinition.getId());
}

@Deployment(resources={
  "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml"})
public void testFindProcessDefinitionById() {
  List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery().list();
  assertEquals(1, definitions.size());

  ProcessDefinition processDefinition = repositoryService.findProcessDefinitionById(definitions.get(0).getId());
  assertNotNull(processDefinition);
  assertEquals("oneTaskProcess", processDefinition.getKey());
  assertEquals("The One Task Process", processDefinition.getName());
}

public void testFindProcessDefinitionByNullId() {
  try {
    repositoryService.findProcessDefinitionById(null);
    fail();
  } catch (ActivitiException e) {
    assertTextPresent("processDefinitionId is null", e.getMessage());
  }
}
}