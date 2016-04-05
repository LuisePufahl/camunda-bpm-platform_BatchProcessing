package org.camunda.bpm.engine.test.history;

import java.util.List;

import org.camunda.bpm.engine.history.HistoricIdentityLink;
import org.camunda.bpm.engine.history.HistoricIdentityLinkQuery;
import org.camunda.bpm.engine.impl.history.event.HistoryEventTypes;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.IdentityLink;
import org.camunda.bpm.engine.task.IdentityLinkType;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */
public class HistoricIdentityLinkTest extends PluggableProcessEngineTestCase {
  private static final String A_USER_ID = "aUserId";
  private static final String B_USER_ID = "bUserId";
  private static final String C_USER_ID = "cUserId";
  private static final int numberOfUsers = 3;
  private static final String A_GROUP_ID = "aGroupId";
  private static final String INVALID_USER_ID = "InvalidUserId";
  private static final String A_ASSIGNER_ID = "aAssignerId";
  private static String PROCESS_DEFINITION_KEY = "oneTaskProcess";
  private static final String GROUP_1 = "Group1";
  private static final String USER_1 = "User1";
  private static final String OWNER_1 = "Owner1";


  @Deployment(resources = { "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testShouldAddTaskCandidateforAddIdentityLink() {

    // Pre test
    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);

    historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 1);
  }

  @Deployment(resources = { "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testShouldAddDelegateTaskCandidateforAddIdentityLink() {

    // Pre test
    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addUserIdentityLink(taskId, A_USER_ID, IdentityLinkType.ASSIGNEE);
    taskService.delegateTask(taskId, B_USER_ID);
    taskService.deleteUserIdentityLink(taskId, B_USER_ID, IdentityLinkType.ASSIGNEE);
    historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    // Addition of A_USER, Deletion of A_USER, Addition of A_USER as owner, Addition of B_USER and deletion of B_USER
    assertEquals(historicIdentityLinks.size(), 5);
    
    //Query test
    HistoricIdentityLinkQuery query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.userId(A_USER_ID).count(), 3);
    
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.userId(B_USER_ID).count(), 2);
 
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_ADD.getEventName()).count(), 3);
    
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_DELETE.getEventName()).count(), 2);
    
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.identityLinkType(IdentityLinkType.ASSIGNEE).count(), 4);
    assertEquals(query.identityLinkType(IdentityLinkType.OWNER).count(), 1);
  }
  
  @Deployment(resources = { "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testShouldAddClaimTaskCandidateforAddIdentityLink() {

    // Pre test
    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.claim(taskId, A_USER_ID);
    
    historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 1);
    
    //Query test
    HistoricIdentityLinkQuery query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.userId(A_USER_ID).count(), 1);
    
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_ADD.getEventName()).count(), 1);
    
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_DELETE.getEventName()).count(), 0);
    
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.identityLinkType(IdentityLinkType.ASSIGNEE).count(), 1);
  }
  
  @Deployment(resources = { "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testShouldAddMultipleDelegateTaskCandidateforAddIdentityLink() {

    // Pre test
    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addUserIdentityLink(taskId, A_USER_ID, IdentityLinkType.ASSIGNEE);
    taskService.delegateTask(taskId, B_USER_ID);
    taskService.delegateTask(taskId, C_USER_ID);
    taskService.deleteUserIdentityLink(taskId, C_USER_ID, IdentityLinkType.ASSIGNEE);
    historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    // Addition of A_USER, Deletion of A_USER, Addition of A_USER as owner,
    // Addition of B_USER, Deletion of B_USER, Addition of C_USER, Deletion of C_USER
    assertEquals(historicIdentityLinks.size(), 7);
    
    //Query test
    HistoricIdentityLinkQuery query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.userId(A_USER_ID).count(), 3);
    
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.userId(B_USER_ID).count(), 2);
 

    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.userId(C_USER_ID).count(), 2);
    
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_ADD.getEventName()).count(), 4);
    
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_DELETE.getEventName()).count(), 3);
    
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.identityLinkType(IdentityLinkType.ASSIGNEE).count(), 6);
    
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.identityLinkType(IdentityLinkType.OWNER).count(), 1);
  }
  @Deployment(resources = { "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testShouldAddTaskCandidateForAddAndDeleteIdentityLink() {

    // Pre test
    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);
    taskService.deleteCandidateUser(taskId, A_USER_ID);

    // then
    historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 2);
  }

  @Deployment(resources = { "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testShouldAddGroupCandidateForAddAndDeleteIdentityLink() {

    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateGroup(taskId, A_GROUP_ID);
    taskService.deleteCandidateGroup(taskId, A_GROUP_ID);

    // then
    historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 2);

    // Basic Query test
    HistoricIdentityLinkQuery query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.groupId(A_GROUP_ID).count(), 2);
  }

  @Deployment(resources = { "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testShouldNotAddTaskCandidateForInvalidIdentityLinkDelete() {

    // Pre test
    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.deleteCandidateUser(taskId, INVALID_USER_ID);

    // then
    historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);
  }

  @Deployment(resources = { "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testShouldAddTaskAssigneeForAddandDeleteIdentityLink() {

    // Pre test
    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    addAndDeleteUserWithAssigner(taskId, IdentityLinkType.ASSIGNEE);
    // then
    historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 2);

    // Basic Query test
    HistoricIdentityLinkQuery query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.identityLinkType(IdentityLinkType.ASSIGNEE).count(), 2);
  }

  @SuppressWarnings("deprecation")
  @Deployment(resources = { "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testShouldAddAndRemoveIdentityLinksForProcessDefinition() throws Exception {

    // Pre test
    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);

    // Given
    ProcessDefinition latestProcessDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).singleResult();
    assertNotNull(latestProcessDef);
    List<IdentityLink> links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
    assertEquals(0, links.size());

    // Add candiate group with process definition
    repositoryService.addCandidateStarterGroup(latestProcessDef.getId(), GROUP_1);
    historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 1);

    // Add candidate user for process definition
    repositoryService.addCandidateStarterUser(latestProcessDef.getId(), USER_1);
    historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 2);

    // Delete candiate group with process definition
    repositoryService.deleteCandidateStarterGroup(latestProcessDef.getId(), GROUP_1);
    historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 3);

    // Delete candidate user for process definition
    repositoryService.deleteCandidateStarterUser(latestProcessDef.getId(), USER_1);
    historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 4);
  }

  @Deployment(resources = { "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testShouldAddTaskOwnerForAddandDeleteIdentityLink() {

    // Pre test
    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    addAndDeleteUserWithAssigner(taskId, IdentityLinkType.OWNER);

    // then
    historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 2);

    // Basic Query test
    HistoricIdentityLinkQuery query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.identityLinkType(IdentityLinkType.OWNER).count(), 2);
  }

  public void testShouldAddIdentityLinkForTaskCreationWithAssigneeAndOwner() {

    String taskAssigneeId = "Assigneee";
    String taskOwnerId = "Owner";
    // Pre test
    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);

    Task taskAssignee = taskService.newTask(taskAssigneeId);
    taskAssignee.setAssignee(USER_1);
    taskService.saveTask(taskAssignee);
    
    Task taskOwner = taskService.newTask(taskOwnerId);
    taskOwner.setOwner(OWNER_1);
    taskService.saveTask(taskOwner);
    
    Task taskEmpty = taskService.newTask();
    taskService.saveTask(taskEmpty);
    
    // then
    historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 2);
    
    // Basic Query test
    HistoricIdentityLinkQuery query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.identityLinkType(IdentityLinkType.ASSIGNEE).count(), 1);
    assertEquals(query.userId(USER_1).count(), 1);
    
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.identityLinkType(IdentityLinkType.OWNER).count(), 1);
    assertEquals(query.userId(OWNER_1).count(), 1);
    
    taskService.deleteTask(taskAssigneeId,true);
    taskService.deleteTask(taskOwnerId,true);
    taskService.deleteTask(taskEmpty.getId(), true);
  }
  public void addAndDeleteUserWithAssigner(String taskId, String identityLinkType) {
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addUserIdentityLink(taskId, A_USER_ID, identityLinkType);
    taskService.deleteUserIdentityLink(taskId, A_USER_ID, identityLinkType);
  }

  public void addUserIdentityLinks(String taskId) {
    for (int userIndex = 1; userIndex <= numberOfUsers; userIndex++)
      taskService.addUserIdentityLink(taskId, A_USER_ID + userIndex, IdentityLinkType.OWNER);
  }

  public void deleteUserIdentityLinks(String taskId) {
    for (int userIndex = 1; userIndex <= numberOfUsers; userIndex++)
      taskService.deleteUserIdentityLink(taskId, A_USER_ID + userIndex, IdentityLinkType.OWNER);
  }

  protected ProcessInstance startProcessInstance(String key) {
    return runtimeService.startProcessInstanceByKey(key);
  }

}
