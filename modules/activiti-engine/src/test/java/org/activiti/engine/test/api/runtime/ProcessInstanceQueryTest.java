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
package org.activiti.engine.test.api.runtime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.runtime.ProcessInstanceQueryProperty;
import org.activiti.engine.impl.test.ActivitiInternalTestCase;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.test.Deployment;

/**
 * @author Joram Barrez
 * @author Frederik Heremans
 */
public class ProcessInstanceQueryTest extends ActivitiInternalTestCase {

  private static String PROCESS_DEFINITION_KEY = "oneTaskProcess";
  private static String PROCESS_DEFINITION_KEY_2 = "oneTaskProcess2";
  
  private List<String> processInstanceIds;

  /**
   * Setup starts 4 process instances of oneTaskProcess 
   * and 1 instance of oneTaskProcess2
   */
  protected void setUp() throws Exception {
    super.setUp();
    repositoryService.createDeployment()
      .addClasspathResource("org/activiti/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
      .addClasspathResource("org/activiti/engine/test/api/runtime/oneTaskProcess2.bpmn20.xml")
      .deploy();

    processInstanceIds = new ArrayList<String>();
    for (int i = 0; i < 4; i++) {
      processInstanceIds.add(runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY, i + "").getId());
    }
    processInstanceIds.add(runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY_2, "1").getId());
  }

  protected void tearDown() throws Exception {
    for (org.activiti.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeploymentCascade(deployment.getId());
    }
    super.tearDown();
  }

  public void testQueryNoSpecificsList() {
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertEquals(5, query.count());
    assertEquals(5, query.list().size());
  }
  
  public void testQueryNoSpecificsSingleResult() {
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    try { 
      query.singleResult();
      fail();
    } catch (ActivitiException e) {
      // Exception is expected
    }
  }
  
  public void testQueryByProcessDefinitionKeySingleResult() {
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY_2);
    assertEquals(1, query.count());
    assertEquals(1, query.list().size());
    assertNotNull(query.singleResult());
  }
  
  public void testQueryByInvalidProcessDefinitionKey() {
    assertNull(runtimeService.createProcessInstanceQuery().processDefinitionKey("invalid").singleResult());
    assertEquals(0, runtimeService.createProcessInstanceQuery().processDefinitionKey("invalid").list().size());
  }

  public void testQueryByProcessDefinitionKeyMultipleResults() {
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY);
    assertEquals(4, query.count());
    assertEquals(4, query.list().size());

    try {
      query.singleResult();
      fail();
    } catch (ActivitiException e) {
      // Exception is expected
    }
  }

  public void testQueryByProcessInstanceId() {
    for (String processInstanceId : processInstanceIds) {
      assertNotNull(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult());
      assertEquals(1, runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).list().size());
    }
  }
  
  public void testQueryByBusinessKeyAndProcessDefinitionKey() {
    assertEquals(1, runtimeService.createProcessInstanceQuery().businessKey("0", PROCESS_DEFINITION_KEY).count());
    assertEquals(1, runtimeService.createProcessInstanceQuery().businessKey("1", PROCESS_DEFINITION_KEY).count());
    assertEquals(1, runtimeService.createProcessInstanceQuery().businessKey("2", PROCESS_DEFINITION_KEY).count());
    assertEquals(1, runtimeService.createProcessInstanceQuery().businessKey("3", PROCESS_DEFINITION_KEY).count());
    assertEquals(1, runtimeService.createProcessInstanceQuery().businessKey("1", PROCESS_DEFINITION_KEY_2).count());
  }
  
  public void testQueryByBusinessKey() {
    assertEquals(1, runtimeService.createProcessInstanceQuery().businessKey("0").count());
    assertEquals(2, runtimeService.createProcessInstanceQuery().businessKey("1").count());
  }
  
  public void testQueryByInvalidBusinessKey() {
    assertEquals(0, runtimeService.createProcessInstanceQuery().businessKey("invalid").count());
    
    try {
      runtimeService.createProcessInstanceQuery().businessKey(null).count();
      fail();
    } catch(ActivitiException e) {
      
    }
  }

  public void testQueryByInvalidProcessInstanceId() {
    assertNull(runtimeService.createProcessInstanceQuery().processInstanceId("I do not exist").singleResult());
    assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId("I do not exist").list().size());
  }
  
  @Deployment(resources = {"org/activiti/engine/test/api/runtime/superProcess.bpmn20.xml",
                           "org/activiti/engine/test/api/runtime/subProcess.bpmn20.xml"})
  public void testQueryBySuperProcessInstanceId() {
    ProcessInstance superProcessInstance = runtimeService.startProcessInstanceByKey("subProcessQueryTest");
    
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().superProcessInstance(superProcessInstance.getId());
    ProcessInstance subProcessInstance = query.singleResult();
    assertNotNull(subProcessInstance);
    assertEquals(1, query.list().size());
    assertEquals(1, query.count());
  }
  
  public void testQueryByInvalidSuperProcessInstanceId() {
    assertNull(runtimeService.createProcessInstanceQuery().superProcessInstance("invalid").singleResult());
    assertEquals(0, runtimeService.createProcessInstanceQuery().superProcessInstance("invalid").list().size());
  }
  
  @Deployment(resources = {"org/activiti/engine/test/api/runtime/superProcess.bpmn20.xml",
                           "org/activiti/engine/test/api/runtime/subProcess.bpmn20.xml"})
  public void testQueryBySubProcessInstanceId() {
    ProcessInstance superProcessInstance = runtimeService.startProcessInstanceByKey("subProcessQueryTest");
    
    ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstance(superProcessInstance.getId()).singleResult();
    assertNotNull(subProcessInstance);
    assertEquals(superProcessInstance.getId(), runtimeService.createProcessInstanceQuery().subProcessInstance(subProcessInstance.getId()).singleResult().getId());
  }
  
  public void testQueryByInvalidSubProcessInstanceId() {
    assertNull(runtimeService.createProcessInstanceQuery().subProcessInstance("invalid").singleResult());
    assertEquals(0, runtimeService.createProcessInstanceQuery().subProcessInstance("invalid").list().size());
  }
  
  // Nested subprocess make the query complexer, hence this test
  @Deployment(resources = {"org/activiti/engine/test/api/runtime/superProcessWithNestedSubProcess.bpmn20.xml",
                           "org/activiti/engine/test/api/runtime/nestedSubProcess.bpmn20.xml",
                           "org/activiti/engine/test/api/runtime/subProcess.bpmn20.xml"})
  public void testQueryBySuperProcessInstanceIdNested() {
    ProcessInstance superProcessInstance = runtimeService.startProcessInstanceByKey("nestedSubProcessQueryTest");
    
    ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstance(superProcessInstance.getId()).singleResult();
    assertNotNull(subProcessInstance);
    
    ProcessInstance nestedSubProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstance(subProcessInstance.getId()).singleResult();
    assertNotNull(nestedSubProcessInstance);
  }
  
  //Nested subprocess make the query complexer, hence this test
  @Deployment(resources = {"org/activiti/engine/test/api/runtime/superProcessWithNestedSubProcess.bpmn20.xml",
          "org/activiti/engine/test/api/runtime/nestedSubProcess.bpmn20.xml",
          "org/activiti/engine/test/api/runtime/subProcess.bpmn20.xml"})
  public void testQueryBySubProcessInstanceIdNested() {
    ProcessInstance superProcessInstance = runtimeService.startProcessInstanceByKey("nestedSubProcessQueryTest");
    
    ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstance(superProcessInstance.getId()).singleResult();
    assertEquals(superProcessInstance.getId(), runtimeService.createProcessInstanceQuery().subProcessInstance(subProcessInstance.getId()).singleResult().getId());
    
    ProcessInstance nestedSubProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstance(subProcessInstance.getId()).singleResult();
    assertEquals(subProcessInstance.getId(), runtimeService.createProcessInstanceQuery().subProcessInstance(nestedSubProcessInstance.getId()).singleResult().getId());
  }
  
  public void testQueryPaging() {
    assertEquals(2, runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).listPage(0, 2).size());
    assertEquals(3, runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).listPage(1, 3).size());

  }
  
  public void testQuerySorting() {
    assertEquals(5, runtimeService.createProcessInstanceQuery().orderBy(ProcessInstanceQueryProperty.PROCESS_INSTANCE_ID).asc().list().size());
    assertEquals(5, runtimeService.createProcessInstanceQuery().orderBy(ProcessInstanceQueryProperty.PROCESS_DEFINITION_ID).asc().list().size());
    assertEquals(5, runtimeService.createProcessInstanceQuery().orderBy(ProcessInstanceQueryProperty.PROCESS_DEFINITION_KEY).asc().list().size());
    
    assertEquals(5, runtimeService.createProcessInstanceQuery().orderBy(ProcessInstanceQueryProperty.PROCESS_INSTANCE_ID).desc().list().size());
    assertEquals(5, runtimeService.createProcessInstanceQuery().orderBy(ProcessInstanceQueryProperty.PROCESS_DEFINITION_ID).desc().list().size());
    assertEquals(5, runtimeService.createProcessInstanceQuery().orderBy(ProcessInstanceQueryProperty.PROCESS_DEFINITION_KEY).desc().list().size());
    
    assertEquals(4, runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).orderBy(ProcessInstanceQueryProperty.PROCESS_INSTANCE_ID).asc().list().size());
    assertEquals(4, runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).orderBy(ProcessInstanceQueryProperty.PROCESS_INSTANCE_ID).desc().list().size());
  }
  
  public void testQueryInvalidSorting() {
    try {
      runtimeService.createProcessInstanceQuery().orderByProcessDefinitionId().list(); // asc - desc not called -> exception
      fail();
    }catch (ActivitiException e) {}
  }
  
  @Deployment(resources={
    "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryStringVariable() {
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("stringVar", "abcdef");
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    vars = new HashMap<String, Object>();
    vars.put("stringVar", "abcdef");
    vars.put("stringVar2", "ghijkl");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    vars = new HashMap<String, Object>();
    vars.put("stringVar", "azerty");
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    // Test EQUAL on single string variable, should result in 2 matches
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValue("stringVar", "abcdef");
    List<ProcessInstance> processInstances = query.list();
    Assert.assertNotNull(processInstances);
    Assert.assertEquals(2, processInstances.size());
  
    // Test EQUAL on two string variables, should result in single match
    query = runtimeService.createProcessInstanceQuery().variableValue("stringVar", "abcdef").variableValue("stringVar2", "ghijkl");
    ProcessInstance resultInstance = query.singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance2.getId(), resultInstance.getId());
    
    // Test NOT_EQUAL, should return only 1 resultInstance
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueNot("stringVar", "abcdef").singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    // Test GREATER_THAN, should return only matching 'azerty'
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("stringVar", "abcdef").singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("stringVar", "z").singleResult();
    Assert.assertNull(resultInstance);
    
    // Test GREATER_THAN_OR_EQUAL, should return 3 results
    assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("stringVar", "abcdef").count());
    assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("stringVar", "z").count());
    
    // Test LESS_THAN, should return 2 results
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThan("stringVar", "abcdeg").list();
    Assert.assertEquals(2, processInstances.size());
    List<String> expecedIds = Arrays.asList(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<String>(Arrays.asList(processInstances.get(0).getId(), processInstances.get(1).getId()));
    ids.removeAll(expecedIds);
    assertTrue(ids.isEmpty());
    
    assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueLessThan("stringVar", "abcdef").count());
    assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "z").count());
    
    // Test LESS_THAN_OR_EQUAL
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "abcdef").list();
    Assert.assertEquals(2, processInstances.size());
    expecedIds = Arrays.asList(processInstance1.getId(), processInstance2.getId());
    ids = new ArrayList<String>(Arrays.asList(processInstances.get(0).getId(), processInstances.get(1).getId()));
    ids.removeAll(expecedIds);
    assertTrue(ids.isEmpty());
    
    assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "z").count());
    assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "aa").count());
    
    // Test LIKE
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueLike("stringVar", "azert%").singleResult();
    assertNotNull(resultInstance);
    assertEquals(processInstance3.getId(), resultInstance.getId());
    
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueLike("stringVar", "%y").singleResult();
    assertNotNull(resultInstance);
    assertEquals(processInstance3.getId(), resultInstance.getId());
    
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueLike("stringVar", "%zer%").singleResult();
    assertNotNull(resultInstance);
    assertEquals(processInstance3.getId(), resultInstance.getId());
    
    assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueLike("stringVar", "a%").count());
    assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueLike("stringVar", "%x%").count());
    
    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
  }
  
  @Deployment(resources={
    "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryLongVariable() {
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("longVar", 12345L);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    vars = new HashMap<String, Object>();
    vars.put("longVar", 12345L);
    vars.put("longVar2", 67890L);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    vars = new HashMap<String, Object>();
    vars.put("longVar", 55555L);
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    // Query on single long variable, should result in 2 matches
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValue("longVar", 12345L);
    List<ProcessInstance> processInstances = query.list();
    Assert.assertNotNull(processInstances);
    Assert.assertEquals(2, processInstances.size());
  
    // Query on two long variables, should result in single match
    query = runtimeService.createProcessInstanceQuery().variableValue("longVar", 12345L).variableValue("longVar2", 67890L);
    ProcessInstance resultInstance = query.singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance2.getId(), resultInstance.getId());
    
    // Query with unexisting variable value
    resultInstance = runtimeService.createProcessInstanceQuery().variableValue("longVar", 999L).singleResult();
    Assert.assertNull(resultInstance);
    
    // Test NOT_EQUALS
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueNot("longVar", 12345L).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    // Test GREATER_THAN
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("longVar", 44444L).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    Assert.assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueGreaterThan("longVar", 55555L).count());
    Assert.assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueGreaterThan("longVar",1L).count());
    
    // Test GREATER_THAN_OR_EQUAL
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("longVar", 44444L).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("longVar", 55555L).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    Assert.assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("longVar",1L).count());
    
    // Test LESS_THAN
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThan("longVar", 55555L).list();
    Assert.assertEquals(2, processInstances.size());
    
    List<String> expecedIds = Arrays.asList(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<String>(Arrays.asList(processInstances.get(0).getId(), processInstances.get(1).getId()));
    ids.removeAll(expecedIds);
    assertTrue(ids.isEmpty());
    
    Assert.assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueLessThan("longVar", 12345L).count());
    Assert.assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueLessThan("longVar",66666L).count());
  
    // Test LESS_THAN_OR_EQUAL
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("longVar", 55555L).list();
    Assert.assertEquals(3, processInstances.size());
    
    Assert.assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("longVar", 12344L).count());
    
    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
  }
  
  @Deployment(resources={
    "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryDoubleVariable() {
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("doubleVar", 12345.6789);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    vars = new HashMap<String, Object>();
    vars.put("doubleVar", 12345.6789);
    vars.put("doubleVar2", 9876.54321);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    vars = new HashMap<String, Object>();
    vars.put("doubleVar", 55555.5555);
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    // Query on single double variable, should result in 2 matches
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValue("doubleVar", 12345.6789);
    List<ProcessInstance> processInstances = query.list();
    Assert.assertNotNull(processInstances);
    Assert.assertEquals(2, processInstances.size());
  
    // Query on two double variables, should result in single value
    query = runtimeService.createProcessInstanceQuery().variableValue("doubleVar", 12345.6789).variableValue("doubleVar2", 9876.54321);
    ProcessInstance resultInstance = query.singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance2.getId(), resultInstance.getId());
    
    // Query with unexisting variable value
    resultInstance = runtimeService.createProcessInstanceQuery().variableValue("doubleVar", 9999.99).singleResult();
    Assert.assertNull(resultInstance);
    
    // Test NOT_EQUALS
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueNot("doubleVar", 12345.6789).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    // Test GREATER_THAN
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("doubleVar", 44444.4444).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    Assert.assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueGreaterThan("doubleVar", 55555.5555).count());
    Assert.assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueGreaterThan("doubleVar",1.234).count());
    
    // Test GREATER_THAN_OR_EQUAL
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("doubleVar", 44444.4444).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("doubleVar", 55555.5555).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    Assert.assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("doubleVar",1.234).count());
    
    // Test LESS_THAN
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThan("doubleVar", 55555.5555).list();
    Assert.assertEquals(2, processInstances.size());
    
    List<String> expecedIds = Arrays.asList(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<String>(Arrays.asList(processInstances.get(0).getId(), processInstances.get(1).getId()));
    ids.removeAll(expecedIds);
    assertTrue(ids.isEmpty());
    
    Assert.assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueLessThan("doubleVar", 12345.6789).count());
    Assert.assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueLessThan("doubleVar",66666.6666).count());
  
    // Test LESS_THAN_OR_EQUAL
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("doubleVar", 55555.5555).list();
    Assert.assertEquals(3, processInstances.size());
    
    Assert.assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("doubleVar", 12344.6789).count());
    
    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
  }
  
  @Deployment(resources={
    "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryIntegerVariable() {
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("integerVar", 12345);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    vars = new HashMap<String, Object>();
    vars.put("integerVar", 12345);
    vars.put("integerVar2", 67890);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    vars = new HashMap<String, Object>();
    vars.put("integerVar", 55555);
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    // Query on single integer variable, should result in 2 matches
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValue("integerVar", 12345);
    List<ProcessInstance> processInstances = query.list();
    Assert.assertNotNull(processInstances);
    Assert.assertEquals(2, processInstances.size());
  
    // Query on two integer variables, should result in single value
    query = runtimeService.createProcessInstanceQuery().variableValue("integerVar", 12345).variableValue("integerVar2", 67890);
    ProcessInstance resultInstance = query.singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance2.getId(), resultInstance.getId());
    
    // Query with unexisting variable value
    resultInstance = runtimeService.createProcessInstanceQuery().variableValue("integerVar", 9999).singleResult();
    Assert.assertNull(resultInstance);
    
    // Test NOT_EQUALS
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueNot("integerVar", 12345).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    // Test GREATER_THAN
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("integerVar", 44444).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    Assert.assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueGreaterThan("integerVar", 55555).count());
    Assert.assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueGreaterThan("integerVar",1).count());
    
    // Test GREATER_THAN_OR_EQUAL
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("integerVar", 44444).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("integerVar", 55555).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    Assert.assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("integerVar",1).count());
    
    // Test LESS_THAN
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThan("integerVar", 55555).list();
    Assert.assertEquals(2, processInstances.size());
    
    List<String> expecedIds = Arrays.asList(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<String>(Arrays.asList(processInstances.get(0).getId(), processInstances.get(1).getId()));
    ids.removeAll(expecedIds);
    assertTrue(ids.isEmpty());
    
    Assert.assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueLessThan("integerVar", 12345).count());
    Assert.assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueLessThan("integerVar",66666).count());
  
    // Test LESS_THAN_OR_EQUAL
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("integerVar", 55555).list();
    Assert.assertEquals(3, processInstances.size());
    
    Assert.assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("integerVar", 12344).count());
    
    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
  }
  
  @Deployment(resources={
    "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryShortVariable() {
    Map<String, Object> vars = new HashMap<String, Object>();
    short shortVar = 1234;
    vars.put("shortVar", shortVar);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    short shortVar2 = 6789;
    vars = new HashMap<String, Object>();
    vars.put("shortVar", shortVar);
    vars.put("shortVar2", shortVar2);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    vars = new HashMap<String, Object>();
    vars.put("shortVar", 5555);
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    // Query on single short variable, should result in 2 matches
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValue("shortVar", shortVar);
    List<ProcessInstance> processInstances = query.list();
    Assert.assertNotNull(processInstances);
    Assert.assertEquals(2, processInstances.size());
  
    // Query on two short variables, should result in single value
    query = runtimeService.createProcessInstanceQuery().variableValue("shortVar", shortVar).variableValue("shortVar2", shortVar2);
    ProcessInstance resultInstance = query.singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance2.getId(), resultInstance.getId());
    
    // Query with unexisting variable value
    short unexistingValue = 9999;
    resultInstance = runtimeService.createProcessInstanceQuery().variableValue("shortVar", unexistingValue).singleResult();
    Assert.assertNull(resultInstance);
    
    // Test NOT_EQUALS
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueNot("shortVar", (short)1234).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    // Test GREATER_THAN
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("shortVar", (short)4444).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    Assert.assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueGreaterThan("shortVar", (short)5555).count());
    Assert.assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueGreaterThan("shortVar",(short)1).count());
    
    // Test GREATER_THAN_OR_EQUAL
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("shortVar", (short)4444).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("shortVar", (short)5555).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    Assert.assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("shortVar",(short)1).count());
    
    // Test LESS_THAN
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThan("shortVar", (short)5555).list();
    Assert.assertEquals(2, processInstances.size());
    
    List<String> expecedIds = Arrays.asList(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<String>(Arrays.asList(processInstances.get(0).getId(), processInstances.get(1).getId()));
    ids.removeAll(expecedIds);
    assertTrue(ids.isEmpty());
    
    Assert.assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueLessThan("shortVar", (short)1234).count());
    Assert.assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueLessThan("shortVar",(short)6666).count());
  
    // Test LESS_THAN_OR_EQUAL
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("shortVar", (short)5555).list();
    Assert.assertEquals(3, processInstances.size());
    
    Assert.assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("shortVar", (short)1233).count());
    
    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
  }
  
  @Deployment(resources={
    "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryDateVariable() throws Exception {
    Map<String, Object> vars = new HashMap<String, Object>();
    Date date1 = Calendar.getInstance().getTime();
    vars.put("dateVar", date1);
    
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    Date date2 = Calendar.getInstance().getTime();
    vars = new HashMap<String, Object>();
    vars.put("dateVar", date1);
    vars.put("dateVar2", date2);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
  
    Calendar nextYear = Calendar.getInstance();
    nextYear.add(Calendar.YEAR, 1);
    vars = new HashMap<String, Object>();
    vars.put("dateVar",nextYear.getTime());
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    Calendar nextMonth = Calendar.getInstance();
    nextMonth.add(Calendar.MONTH, 1);   
    
    Calendar twoYearsLater = Calendar.getInstance();
    twoYearsLater.add(Calendar.YEAR, 2);    
    
    Calendar oneYearAgo = Calendar.getInstance();
    oneYearAgo.add(Calendar.YEAR, -1);    
    
    // Query on single short variable, should result in 2 matches
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValue("dateVar", date1);
    List<ProcessInstance> processInstances = query.list();
    Assert.assertNotNull(processInstances);
    Assert.assertEquals(2, processInstances.size());
  
    // Query on two short variables, should result in single value
    query = runtimeService.createProcessInstanceQuery().variableValue("dateVar", date1).variableValue("dateVar2", date2);
    ProcessInstance resultInstance = query.singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance2.getId(), resultInstance.getId());
    
    // Query with unexisting variable value
    Date unexistingDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/01/1989 12:00:00");
    resultInstance = runtimeService.createProcessInstanceQuery().variableValue("dateVar", unexistingDate).singleResult();
    Assert.assertNull(resultInstance);
    
    // Test NOT_EQUALS
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueNot("dateVar", date1).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    // Test GREATER_THAN
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("dateVar", nextMonth.getTime()).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    Assert.assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueGreaterThan("dateVar", nextYear.getTime()).count());
    Assert.assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueGreaterThan("dateVar", oneYearAgo.getTime()).count());
    
    // Test GREATER_THAN_OR_EQUAL
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("dateVar", nextMonth.getTime()).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("dateVar", nextYear.getTime()).singleResult();
    Assert.assertNotNull(resultInstance);
    Assert.assertEquals(processInstance3.getId(), resultInstance.getId());
    
    Assert.assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("dateVar",oneYearAgo.getTime()).count());
    
    // Test LESS_THAN
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThan("dateVar", nextYear.getTime()).list();
    Assert.assertEquals(2, processInstances.size());
    
    List<String> expecedIds = Arrays.asList(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<String>(Arrays.asList(processInstances.get(0).getId(), processInstances.get(1).getId()));
    ids.removeAll(expecedIds);
    assertTrue(ids.isEmpty());
    
    Assert.assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueLessThan("dateVar", date1).count());
    Assert.assertEquals(3, runtimeService.createProcessInstanceQuery().variableValueLessThan("dateVar", twoYearsLater.getTime()).count());
  
    // Test LESS_THAN_OR_EQUAL
    processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("dateVar", nextYear.getTime()).list();
    Assert.assertEquals(3, processInstances.size());
    
    Assert.assertEquals(0, runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("dateVar", oneYearAgo.getTime()).count());
    
    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
  }
  
  @Deployment(resources={
    "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryNullVariable() throws Exception {
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("nullVar", null);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    vars = new HashMap<String, Object>();
    vars.put("nullVar", "notnull");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    vars = new HashMap<String, Object>();
    vars.put("nullVarLong", "notnull");
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    vars = new HashMap<String, Object>();
    vars.put("nullVarDouble", "notnull");
    ProcessInstance processInstance4 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    vars = new HashMap<String, Object>();
    vars.put("nullVarByte", "testbytes".getBytes());
    ProcessInstance processInstance5 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    // Query on null value, should return one value
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValue("nullVar", null);
    List<ProcessInstance> processInstances = query.list();
    Assert.assertNotNull(processInstances);
    Assert.assertEquals(1, processInstances.size());
    Assert.assertEquals(processInstance1.getId(), processInstances.get(0).getId());
    
    // Test NOT_EQUALS null
    Assert.assertEquals(1, runtimeService.createProcessInstanceQuery().variableValueNot("nullVar", null).count());
    Assert.assertEquals(1, runtimeService.createProcessInstanceQuery().variableValueNot("nullVarLong", null).count());
    Assert.assertEquals(1, runtimeService.createProcessInstanceQuery().variableValueNot("nullVarDouble", null).count());
    // When a byte-array refrence is present, the variable is not considered null
    Assert.assertEquals(1, runtimeService.createProcessInstanceQuery().variableValueNot("nullVarByte", null).count());
    
    // All other variable queries with null should throw exception
    try {
      runtimeService.createProcessInstanceQuery().variableValueGreaterThan("nullVar", null);
      fail("Excetion expected");
    } catch(ActivitiException ae) {
      assertTextPresent("Nullvalue cannot be used in 'greater than' condition", ae.getMessage());
    }
    
    try {
      runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("nullVar", null);
      fail("Excetion expected");
    } catch(ActivitiException ae) {
      assertTextPresent("Nullvalue cannot be used in 'greater than or equal' condition", ae.getMessage());
    }
    
    try {
      runtimeService.createProcessInstanceQuery().variableValueLessThan("nullVar", null);
      fail("Excetion expected");
    } catch(ActivitiException ae) {
      assertTextPresent("Nullvalue cannot be used in 'less than' condition", ae.getMessage());
    }
    
    try {
      runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("nullVar", null);
      fail("Excetion expected");
    } catch(ActivitiException ae) {
      assertTextPresent("Nullvalue cannot be used in 'less than or equal' condition", ae.getMessage());
    }
    
    try {
      runtimeService.createProcessInstanceQuery().variableValueLike("nullVar", null);
      fail("Excetion expected");
    } catch(ActivitiException ae) {
      assertTextPresent("Nullvalue cannot be used in 'like' condition", ae.getMessage());
    }
    
    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance4.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance5.getId(), "test");
  }
  
  @Deployment(resources={
    "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryInvalidTypes() throws Exception {
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("bytesVar", "test".getBytes());
    vars.put("serializableVar",new DummySerializable());
    
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    try {
      runtimeService.createProcessInstanceQuery()
        .variableValue("bytesVar", "test".getBytes())
        .list();
      fail("Expected exception");
    } catch(ActivitiException ae) {
      assertTextPresent("Variables of type ByteArray cannot be used to query", ae.getMessage());
    }
    
    try {
      runtimeService.createProcessInstanceQuery()
        .variableValue("serializableVar", new DummySerializable())
        .list();
      fail("Expected exception");
    } catch(ActivitiException ae) {
      assertTextPresent("Variables of type ByteArray cannot be used to query", ae.getMessage());
    }   
  
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");
  }
  
  public void testQueryVariablesNullNameArgument() {
    try {
      runtimeService.createProcessInstanceQuery().variableValue(null, "value");
      fail("Expected exception");
    } catch(ActivitiException ae) {
      assertTextPresent("name is null", ae.getMessage());
    }   
    try {
      runtimeService.createProcessInstanceQuery().variableValueNot(null, "value");
      fail("Expected exception");
    } catch(ActivitiException ae) {
      assertTextPresent("name is null", ae.getMessage());
    }   
    try {
      runtimeService.createProcessInstanceQuery().variableValueGreaterThan(null, "value");
      fail("Expected exception");
    } catch(ActivitiException ae) {
      assertTextPresent("name is null", ae.getMessage());
    }   
    try {
      runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual(null, "value");
      fail("Expected exception");
    } catch(ActivitiException ae) {
      assertTextPresent("name is null", ae.getMessage());
    }   
    try {
      runtimeService.createProcessInstanceQuery().variableValueLessThan(null, "value");
      fail("Expected exception");
    } catch(ActivitiException ae) {
      assertTextPresent("name is null", ae.getMessage());
    }   
    try {
      runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual(null, "value");
      fail("Expected exception");
    } catch(ActivitiException ae) {
      assertTextPresent("name is null", ae.getMessage());
    }   
    try {
      runtimeService.createProcessInstanceQuery().variableValueLike(null, "value");
      fail("Expected exception");
    } catch(ActivitiException ae) {
      assertTextPresent("name is null", ae.getMessage());
    }   
  }
  
  @Deployment(resources={
    "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryAllVariableTypes() throws Exception {
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("nullVar", null);
    vars.put("stringVar", "string");
    vars.put("longVar", 10L);
    vars.put("doubleVar", 1.2);
    vars.put("integerVar", 1234);
    vars.put("shortVar", (short) 123);
    
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
      .variableValue("nullVar", null)
      .variableValue("stringVar", "string")
      .variableValue("longVar", 10L)
      .variableValue("doubleVar", 1.2)
      .variableValue("integerVar", 1234)
      .variableValue("shortVar", (short) 123);
    
    List<ProcessInstance> processInstances = query.list();
    Assert.assertNotNull(processInstances);
    Assert.assertEquals(1, processInstances.size());
    Assert.assertEquals(processInstance.getId(), processInstances.get(0).getId());
  
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");
  }


}
