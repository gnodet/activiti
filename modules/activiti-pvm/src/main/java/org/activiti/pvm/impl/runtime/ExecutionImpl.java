/* Licensed under the Apache License, ersion 2.0 (the "License");
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
package org.activiti.pvm.impl.runtime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.pvm.PvmException;
import org.activiti.pvm.activity.ActivityExecution;
import org.activiti.pvm.event.EventListenerExecution;
import org.activiti.pvm.impl.process.ActivityImpl;
import org.activiti.pvm.impl.process.ProcessDefinitionImpl;
import org.activiti.pvm.impl.process.ScopeImpl;
import org.activiti.pvm.impl.process.TransitionImpl;
import org.activiti.pvm.impl.process.VariableDeclaration;
import org.activiti.pvm.process.PvmActivity;
import org.activiti.pvm.process.PvmProcessDefinition;
import org.activiti.pvm.process.PvmProcessElement;
import org.activiti.pvm.process.PvmTransition;
import org.activiti.pvm.runtime.PvmExecution;
import org.activiti.pvm.runtime.PvmProcessInstance;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class ExecutionImpl implements
        Serializable,
        ActivityExecution, 
        EventListenerExecution, 
        PvmProcessInstance,
        PvmExecution {
  
  private static final long serialVersionUID = 1L;
  
  private static Logger log = Logger.getLogger(ExecutionImpl.class.getName());
  
  // current position /////////////////////////////////////////////////////////
  
  protected ProcessDefinitionImpl processDefinition;

  /** current activity */
  protected ActivityImpl activity;
  
  /** current activity */
  protected ScopeImpl scope;
  
  /** current transition.  is null when there is no transition being taken. */
  protected TransitionImpl transition = null;

  /** the process instance.  this is the root of the execution tree.  
   * the processInstance of a process instance is a self reference. */
  protected ExecutionImpl processInstance;
  
  /** the parent execution */
  protected ExecutionImpl parent;
  
  /** nested executions representing scopes or concurrent paths */
  protected List<ExecutionImpl> executions;
  
  /** super execution, not-null if this execution is part of a subprocess */
  protected ExecutionImpl superExecution;
  
  /** reference to a subprocessinstance, not-null if currently subprocess is started from this execution */
  protected ExecutionImpl subProcessInstance;
  
  // state/type of execution ////////////////////////////////////////////////// 
  
  /** indicates if this execution represents an active path of execution.
   * Executions are made inactive in the following situations:
   * <ul>
   *   <li>an execution enters a nested scope</li>
   *   <li>an execution is split up into multiple concurrent executions, then the parent is made inactive.</li>
   *   <li>an execution has arrived in a parallel gateway or join and that join has not yet activated/fired.</li>
   *   <li>an execution is ended.</li>
   * </ul>*/ 
  protected boolean isActive = true;
  protected boolean isConcurrent = false;
  protected boolean isEnded = false;
  
  protected Map<String, Object> variables = null;
  
  // events ///////////////////////////////////////////////////////////////////
  
  protected String eventName;
  protected PvmProcessElement eventSource;
  protected int eventListenerIndex = 0;
  protected String deleteReason;

  // atomic operations ////////////////////////////////////////////////////////

  /** next operation.  process execution is in fact runtime interpretation of the process model.
   * each operation is a logical unit of interpretation of the process.  so sequentially processing 
   * the operations drives the interpretation or execution of a process. 
   * @see AtomicOperation
   * @see #performOperation(AtomicOperation) */
  protected AtomicOperation nextOperation;
  protected boolean isOperating = false;

  /* Default constructor for ibatis/jpa/etc. */
  public ExecutionImpl() {
  }
  
  // lifecycle methods ////////////////////////////////////////////////////////
  
  /** creates a new execution. properties processDefinition, processInstance and activity will be initialized. */  
  public ExecutionImpl createExecution() {
    // create the new child execution
    ExecutionImpl createdExecution = newExecution();

    // manage the bidirectional parent-child relation
    ensureExecutionsInitialized();
    executions.add(createdExecution); 
    createdExecution.setParent(this);
    
    // initialize the new execution
    createdExecution.setProcessDefinition(getProcessDefinition());
    createdExecution.setProcessInstance(getProcessInstance());
    createdExecution.setActivity(getActivity());
    
    return createdExecution;
  }
  
  /** instantiates a new execution.  can be overridden by subclasses */
  protected ExecutionImpl newExecution() {
    return new ExecutionImpl();
  }

  public PvmProcessInstance createSubProcessInstance(PvmProcessDefinition processDefinition) {
    ExecutionImpl subProcessInstance = newExecution();
    
    // manage bidirectional super-subprocess relation
    subProcessInstance.setSuperExecution(this);
    this.setSubProcessInstance(subProcessInstance);
    
    // Initialize the new execution
    subProcessInstance.setProcessDefinition((ProcessDefinitionImpl) processDefinition);
    subProcessInstance.setProcessInstance(subProcessInstance);

    return subProcessInstance;
  }
  
  public void initialize() {
    if (isScope()) {
      log.fine("initializing "+this);
      for (VariableDeclaration variableDeclaration: getScope().getVariableDeclarations()) {
        variableDeclaration.initialize(this);
      }
    }
  }
  
  public void destroy() {
    log.fine("destroying "+this);
    setScope(null);
    variables = null;
  }
  
  public void remove() {
    if (parent!=null) {
      parent.ensureExecutionsInitialized();
      parent.executions.remove(this);
    }
  }
  
  public void migrateScope(ExecutionImpl other) {
    setScope(other.getScope());
    setVariables(other.getVariables());
  }
  
  // parent ///////////////////////////////////////////////////////////////////

  /** ensures initialization and returns the parent */
  public ExecutionImpl getParent() {
    ensureParentInitialized();
    return parent;
  }

  /** all updates need to go through this setter as subclasses can override this method */
  protected void setParent(ExecutionImpl parent) {
    this.parent = parent;
  }

  /** must be called before memberfield parent is used. 
   * can be used by subclasses to provide parent member field initialization. */
  protected void ensureParentInitialized() {
  }

  // executions ///////////////////////////////////////////////////////////////  

  /** ensures initialization and returns the non-null executions list */
  public List<ExecutionImpl> getExecutions() {
    ensureExecutionsInitialized();
    return executions;
  }
  
  public ExecutionImpl getSuperExecution() {
    ensureSuperExecutionInitialized();
    return superExecution;
  }

  public void setSuperExecution(ExecutionImpl superExecution) {
    this.superExecution = superExecution;
    if (superExecution != null) {
      superExecution.setSubProcessInstance(null);
    }
  }
  
  // Meant to be overridden by persistent subclasseses
  protected void ensureSuperExecutionInitialized() {
  }
  
  public ExecutionImpl getSubProcessInstance() {
    ensureSubProcessInstanceInitialized();
    return subProcessInstance;
  }
  
  public void setSubProcessInstance(ExecutionImpl subProcessInstance) {
    this.subProcessInstance = subProcessInstance;
  }

  // Meant to be overridden by persistent subclasses
  protected void ensureSubProcessInstanceInitialized() {
  }

  public void deleteCascade(String deleteReason) {
    this.deleteReason = deleteReason;
    performOperation(AtomicOperation.DELETE_CASCADE);
  }
  
  /** removes an execution. if there are nested executions, those will be ended recursively.
   * if there is a parent, this method removes the bidirectional relation 
   * between parent and this execution. */
  public void end() {
    isActive = false;
    isEnded = true;
    performOperation(AtomicOperation.ACTIVITY_END);
  }

  /** searches for an execution positioned in the given activity */
  public ExecutionImpl findExecution(String activityId) {
    if ( (getActivity()!=null)
         && (getActivity().getId().equals(activityId))
       ) {
      return this;
    }
    for (ExecutionImpl nestedExecution : getExecutions()) {
      ExecutionImpl result = nestedExecution.findExecution(activityId);
      if (result != null) {
        return result;
      }
    }
    return null;
  }
  
  public List<String> findActiveActivityIds() {
    List<String> activeActivityIds = new ArrayList<String>();
    collectActiveActivityIds(activeActivityIds);
    return activeActivityIds;
  }

  protected void collectActiveActivityIds(List<String> activeActivityIds) {
    ensureActivityInitialized();
    if (isActive && activity!=null) {
      activeActivityIds.add(activity.getId());
    }
    ensureExecutionsInitialized();
    for (ExecutionImpl execution: executions) {
      execution.collectActiveActivityIds(activeActivityIds);
    }
  }

  /** must be called before memberfield executions is used. 
   * can be used by subclasses to provide executions member field initialization. */
  protected void ensureExecutionsInitialized() {
    if (executions==null) {
      executions = new ArrayList<ExecutionImpl>();
    }
  }

  // process definition ///////////////////////////////////////////////////////
  
  /** ensures initialization and returns the process definition. */
  public ProcessDefinitionImpl getProcessDefinition() {
    ensureProcessDefinitionInitialized();
    return processDefinition;
  }
  
  public String getProcessDefinitionId() {
    return getProcessDefinition().getId();
  }

  /** for setting the process definition, this setter must be used as subclasses can override */  

  /** must be called before memberfield processDefinition is used. 
   * can be used by subclasses to provide processDefinition member field initialization. */
  protected void ensureProcessDefinitionInitialized() {
  }
  
  // process instance /////////////////////////////////////////////////////////

  /** ensures initialization and returns the process instance. */
  public ExecutionImpl getProcessInstance() {
    ensureProcessInstanceInitialized();
    return processInstance;
  }
  
  /** for setting the process instance, this setter must be used as subclasses can override */  
  public void setProcessInstance(ExecutionImpl processInstance) {
    this.processInstance = processInstance;
  }

  /** must be called before memberfield processInstance is used. 
   * can be used by subclasses to provide processInstance member field initialization. */
  protected void ensureProcessInstanceInitialized() {
  }
  
  // activity /////////////////////////////////////////////////////////////////
  
  /** ensures initialization and returns the activity */
  public ActivityImpl getActivity() {
    ensureActivityInitialized();
    return activity;
  }
  
  /** sets the current activity.  can be overridden by subclasses.  doesn't 
   * require initialization. */
  public void setActivity(ActivityImpl activity) {
    this.activity = activity;
  }

  /** must be called before the activity member field or getActivity() is called */
  protected void ensureActivityInitialized() {
  }
  
  // scopes ///////////////////////////////////////////////////////////////////
  
  protected void ensureScopeInitialized() {
  }
  
  public ScopeImpl getScope() {
    if (isProcessInstance()) {
      return processDefinition;
    }
    ensureScopeInitialized();
    return scope;
  }

  public boolean isScope() {
    if (isProcessInstance()) {
      return true;
    }
    ensureScopeInitialized();
    return scope!=null;
  }
  
  // process instance start implementation ////////////////////////////////////

  public void start() {
    performOperation(AtomicOperation.PROCESS_START);
  }
  
  // methods that translate to operations /////////////////////////////////////

  public void signal(String signalName, Object signalData) {
    performOperation(new AtomicOperationSignal(signalName, signalData));
  }
  
  public void take(PvmTransition transition) {
    if (this.transition!=null) {
      throw new PvmException("already taking a transition");
    }
    setTransition((TransitionImpl) transition);
    performOperation(AtomicOperation.TRANSITION_NOTIFY_LISTENER_END);
  }
  
  public void executeActivity(ActivityImpl activity) {
    setActivity(activity);
    performOperation(AtomicOperation.ACTIVITY_EXECUTE);
  }

  public List<ActivityExecution> findInactiveConcurrentExecutions(PvmActivity activity) {
    List<ActivityExecution> inactiveConcurrentExecutionsInActivity = new ArrayList<ActivityExecution>();
    List<ActivityExecution> otherConcurrentExecutions = new ArrayList<ActivityExecution>();
    if (isConcurrent()) {
      List< ? extends ActivityExecution> concurrentExecutions = getParent().getExecutions();
      for (ActivityExecution concurrentExecution: concurrentExecutions) {
        if (concurrentExecution.getActivity()==activity) {
          if (concurrentExecution.isActive()) {
            throw new PvmException("didn't expect active execution in "+activity+". bug?");
          }
          inactiveConcurrentExecutionsInActivity.add(concurrentExecution);
        } else {
          otherConcurrentExecutions.add(concurrentExecution);
        }
      }
    } else {
      if (!isActive()) {
        inactiveConcurrentExecutionsInActivity.add(this);
      } else {
        otherConcurrentExecutions.add(this);
      }
    }
    if (log.isLoggable(Level.FINE)) {
      log.fine("inactive concurrent executions in '"+activity+"': "+inactiveConcurrentExecutionsInActivity);
      log.fine("other concurrent executions: "+otherConcurrentExecutions);
    }
    return inactiveConcurrentExecutionsInActivity;
  }

  @SuppressWarnings("unchecked")
  public void takeAll(List<PvmTransition> transitions, List<ActivityExecution> recyclableExecutions) {
    if (recyclableExecutions.size()>1) {
      for (ActivityExecution recyclableExecution: recyclableExecutions) {
        if (((ExecutionImpl)recyclableExecution).isScope()) {
          throw new PvmException("joining scope executions is not allowed");
        }
      }
    }

    transitions = new ArrayList<PvmTransition>(transitions);
    recyclableExecutions = (recyclableExecutions!=null ? new ArrayList<ActivityExecution>(recyclableExecutions) : new ArrayList<ActivityExecution>());
    
    ExecutionImpl concurrentRoot = (isConcurrent() ? getParent() : this);
    List<ExecutionImpl> concurrentActiveExecutions = new ArrayList<ExecutionImpl>();
    for (ExecutionImpl execution: concurrentRoot.getExecutions()) {
      if (execution.isActive()) {
        concurrentActiveExecutions.add(execution);
      }
    }

    if (log.isLoggable(Level.FINE)) {
      log.fine("transitions to take concurrent: " + transitions);
      log.fine("active concurrent executions: " + concurrentActiveExecutions);
    }

    if ( (transitions.size()==1)
         && (concurrentActiveExecutions.isEmpty())
       ) {

      List<ExecutionImpl> recyclableExecutionImpls = (List) recyclableExecutions;
      for (ExecutionImpl prunedExecution: recyclableExecutionImpls) {
        // End the pruned executions if necessary.
        // Some recyclable executions are inactivated (joined executions)
        // Others are already ended (end activities)
        if (!prunedExecution.isEnded()) {
          log.fine("pruning execution " + prunedExecution);
          prunedExecution.remove();
        }
      }

      log.fine("activating the concurrent root "+concurrentRoot+" as the single path of execution going forward");
      concurrentRoot.setActive(true);
      concurrentRoot.setActivity(activity);
      concurrentRoot.setConcurrent(false);
      concurrentRoot.take(transitions.get(0));

    } else {
      
      List<OutgoingExecution> outgoingExecutions = new ArrayList<OutgoingExecution>();

      recyclableExecutions.remove(concurrentRoot);
  
      log.fine("recyclable executions for reused: " + recyclableExecutions);
      
      // first create the concurrent executions
      while (!transitions.isEmpty()) {
        PvmTransition outgoingTransition = transitions.remove(0);

        if (recyclableExecutions.isEmpty()) {
          ActivityExecution outgoingExecution = concurrentRoot.createExecution();
          outgoingExecution.setActive(true);
          outgoingExecution.setConcurrent(true);
          outgoingExecutions.add(new OutgoingExecution(outgoingExecution, outgoingTransition, true));
          log.fine("new "+outgoingExecution+" created to take transition "+outgoingTransition);
        } else {
          ActivityExecution outgoingExecution = recyclableExecutions.remove(0);
          outgoingExecution.setActive(true);
          outgoingExecution.setConcurrent(true);
          outgoingExecutions.add(new OutgoingExecution(outgoingExecution, outgoingTransition, true));
          log.fine("recycled "+outgoingExecution+" to take transition "+outgoingTransition);
        }
      }

      // prune the executions that are not recycled 
      for (ActivityExecution prunedExecution: recyclableExecutions) {
        log.info("pruning execution "+prunedExecution);
        prunedExecution.end();
      }

      // then launch all the concurrent executions
      for (OutgoingExecution outgoingExecution: outgoingExecutions) {
        outgoingExecution.take();
      }
    }
  }
  
  public void executeActivity(PvmActivity activity) {
    setActivity((ActivityImpl) activity);
    performOperation(AtomicOperation.ACTIVITY_EXECUTE);
  }

  
  protected void performOperation(AtomicOperation executionOperation) {
    this.nextOperation = executionOperation;
    if (!isOperating) {
      isOperating = true;
      while (nextOperation!=null) {
        AtomicOperation currentOperation = this.nextOperation;
        this.nextOperation = null;
        if (log.isLoggable(Level.FINEST)) {
          log.finest("AtomicOperation: " + currentOperation + " on " + this);
        }
        currentOperation.execute(this);
      }
      isOperating = false;
    }
  }

  
  public boolean isActive(String activityId) {
    return findExecution(activityId)!=null;
  }

  // variables ////////////////////////////////////////////////////////////////

  public Object getVariable(String variableName) {
    ensureVariablesInitialized();
    
    // If value is found in this scope, return it
    if (variables.containsKey(variableName)) {
      return variables.get(variableName);
    }
    
    // If value not found in this scope, check the parent scope
    ensureParentInitialized();
    if (parent != null) {
      return parent.getVariable(variableName);        
    }
    
    // Variable is nowhere to be found
    return null;
  }

  public Map<String, Object> getVariables() {
    Map<String, Object> collectedVariables = new HashMap<String, Object>();
    collectVariables(collectedVariables);
    return collectedVariables;
  }
  
  protected void collectVariables(Map<String, Object> collectedVariables) {
    ensureParentInitialized();
    if (parent!=null) {
      parent.collectVariables(collectedVariables);
    }
    ensureVariablesInitialized();
    collectedVariables.putAll(variables);
  }

  public void setVariables(Map<String, Object> variables) {
    ensureVariablesInitialized();
    if (variables!=null) {
      for (Map.Entry<String, Object> entry: variables.entrySet()) {
        setVariable(entry.getKey(), entry.getValue());
      }
    }
  }

  public void setVariable(String variableName, Object value) {
    ensureVariablesInitialized();
    if (variables.containsKey(variableName)) {
      setVariableLocally(variableName, value);
    } else {
      ensureParentInitialized();
      if (parent!=null) {
        parent.setVariable(variableName, value);
      } else {
        setVariableLocally(variableName, value);
      }
    }
  }

  protected void setVariableLocally(String variableName, Object value) {
    log.fine("setting variable '"+variableName+"' to value '"+value+"' on "+this);
    variables.put(variableName, value);
  }
  
  public boolean hasVariable(String variableName) {
    ensureVariablesInitialized();
    if (variables.containsKey(variableName)) {
      return true;
    }
    ensureParentInitialized();
    if (parent!=null) {
      return parent.hasVariable(variableName);
    }
    return false;
  }

  protected void ensureVariablesInitialized() {
    if (variables==null) {
      variables = new HashMap<String, Object>();
    }
  }
  
  // toString /////////////////////////////////////////////////////////////////
  
  public String toString() {
    if (isProcessInstance()) {
      return "ProcessInstance["+System.identityHashCode(this)+"]";
    } else {
      return (isConcurrent? "Concurrent" : "")+(isScope() ? "Scope" : "")+"Execution["+System.identityHashCode(this)+"]";
    }
  }
  
  // customized getters and setters ///////////////////////////////////////////

  public boolean isProcessInstance() {
    ensureParentInitialized();
    return parent==null;
  }
  
  public void inactivate() {
    this.isActive = false;
  }
  
  // getters and setters //////////////////////////////////////////////////////

  public TransitionImpl getTransition() {
    return transition;
  }
  public void setTransition(TransitionImpl transition) {
    this.transition = transition;
  }
  public Integer getEventListenerIndex() {
    return eventListenerIndex;
  }
  public void setEventListenerIndex(Integer eventListenerIndex) {
    this.eventListenerIndex = eventListenerIndex;
  }
  public boolean isConcurrent() {
    return isConcurrent;
  }
  public void setConcurrent(boolean isConcurrent) {
    this.isConcurrent = isConcurrent;
  }
  public boolean isActive() {
    return isActive;
  }
  public void setActive(boolean isActive) {
    this.isActive = isActive;
  }
  public boolean isEnded() {
    return isEnded;
  }
  public void setProcessDefinition(ProcessDefinitionImpl processDefinition) {
    this.processDefinition = processDefinition;
  }
  public String getEventName() {
    return eventName;
  }
  public void setEventName(String eventName) {
    this.eventName = eventName;
  }
  public PvmProcessElement getEventSource() {
    return eventSource;
  }
  public void setEventSource(PvmProcessElement eventSource) {
    this.eventSource = eventSource;
  }
  public String getDeleteReason() {
    return deleteReason;
  }
  public void setDeleteReason(String deleteReason) {
    this.deleteReason = deleteReason;
  }
  public void setScope(ScopeImpl scope) {
    this.scope = scope;
  }
}
