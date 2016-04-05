package org.camunda.bpm.engine.test.history;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.camunda.bpm.engine.history.HistoricIdentityLink;
import org.camunda.bpm.engine.history.HistoricIdentityLinkQuery;
import org.camunda.bpm.engine.impl.history.event.HistoryEventTypes;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.impl.util.ClockUtil;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.IdentityLink;
import org.camunda.bpm.engine.task.IdentityLinkType;
import org.camunda.bpm.engine.test.Deployment;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */
public class HistoricIdentityLinkQueryTest extends PluggableProcessEngineTestCase {
  private static final String A_USER_ID = "aUserId";
  private static final String A_GROUP_ID = "aGroupId";
  private static final int numberOfUsers = 3;
  private static final String A_ASSIGNER_ID = "aAssignerId";

  private static final String INVALID_USER_ID = "InvalidUserId";
  private static final String INVALID_TASK_ID = "InvalidTask";
  private static final String INVALID_GROUP_ID = "InvalidGroupId";
  private static final String INVALID_ASSIGNER_ID = "InvalidAssignerId";
  private static final String INVALID_HISTORY_EVENT_TYPE = "InvalidEventType";
  private static final String INVALID_IDENTITY_LINK_TYPE = "InvalidIdentityLinkType";
  private static final String GROUP_1 = "Group1";
  private static final String USER_1 = "User1";
  private static String PROCESS_DEFINITION_KEY = "oneTaskProcess";

  @Deployment(resources = { "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testQueryAddTaskCandidateforAddIdentityLink() {

    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);

    // Query test
    HistoricIdentityLink historicIdentityLink = historyService.createHistoricIdentityLinkQuery().singleResult();
    assertEquals(historicIdentityLink.getUserId(), A_USER_ID);
    assertEquals(historicIdentityLink.getTaskId(), taskId);
    assertEquals(historicIdentityLink.getType(), IdentityLinkType.CANDIDATE);
    assertEquals(historicIdentityLink.getAssignerId(), A_ASSIGNER_ID);
    assertEquals(historicIdentityLink.getGroupId(), null);
    assertEquals(historicIdentityLink.getOperationType(), HistoryEventTypes.IDENTITY_LINK_ADD.getEventName());
  }

  @Deployment(resources = { "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testGroupQueryTaskCandidateForAddAndDeleteIdentityLink() {

    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateGroup(taskId, A_GROUP_ID);

    // Query test
    HistoricIdentityLink historicIdentityLink = historyService.createHistoricIdentityLinkQuery().singleResult();
    assertEquals(historicIdentityLink.getUserId(), null);
    assertEquals(historicIdentityLink.getTaskId(), taskId);
    assertEquals(historicIdentityLink.getType(), IdentityLinkType.CANDIDATE);
    assertEquals(historicIdentityLink.getAssignerId(), A_ASSIGNER_ID);
    assertEquals(historicIdentityLink.getGroupId(), A_GROUP_ID);
    assertEquals(historicIdentityLink.getOperationType(), HistoryEventTypes.IDENTITY_LINK_ADD.getEventName());
  }

  @Deployment(resources = { "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testValidIndividualQueryTaskCandidateForAddAndDeleteIdentityLink() {

    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);
    taskService.deleteCandidateUser(taskId, A_USER_ID);

    // Valid Individual Query test
    HistoricIdentityLinkQuery query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.taskId(taskId).count(), 2);

    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.identityLinkType(IdentityLinkType.CANDIDATE).count(), 2);

    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.userId(A_USER_ID).count(), 2);

    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.assignerId(A_ASSIGNER_ID).count(), 2);

    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_DELETE.getEventName()).count(), 1);

    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_ADD.getEventName()).count(), 1);

  }

  @Deployment(resources = { "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testValidGroupQueryTaskCandidateForAddAndDeleteIdentityLink() {

    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);
    taskService.deleteCandidateUser(taskId, A_USER_ID);

    // Valid group query test
    HistoricIdentityLinkQuery query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.taskId(taskId).count(), 2);
    assertEquals(query.identityLinkType(IdentityLinkType.CANDIDATE).count(), 2);
    assertEquals(query.userId(A_USER_ID).count(), 2);
    assertEquals(query.assignerId(A_ASSIGNER_ID).count(), 2);
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_DELETE.getEventName()).count(), 1);
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_ADD.getEventName()).count(), 1);
  }

  @Deployment(resources = { "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testInvalidIndividualQueryTaskCandidateForAddAndDeleteIdentityLink() {

    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);
    taskService.deleteCandidateUser(taskId, A_USER_ID);

    // Invalid Individual Query test
    HistoricIdentityLinkQuery query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.taskId(INVALID_TASK_ID).count(), 0);

    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.identityLinkType(INVALID_IDENTITY_LINK_TYPE).count(), 0);

    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.userId(INVALID_USER_ID).count(), 0);

    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.groupId(INVALID_GROUP_ID).count(), 0);

    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.assignerId(INVALID_ASSIGNER_ID).count(), 0);

    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.operationType(INVALID_HISTORY_EVENT_TYPE).count(), 0);

  }

  @Deployment(resources = { "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testInvalidGroupQueryTaskCandidateForAddAndDeleteIdentityLink() {

    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);
    taskService.deleteCandidateUser(taskId, A_USER_ID);

    // Invalid Individual Query test
    HistoricIdentityLinkQuery query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.taskId(INVALID_TASK_ID).count(), 0);
    assertEquals(query.identityLinkType(INVALID_IDENTITY_LINK_TYPE).count(), 0);
    assertEquals(query.userId(INVALID_USER_ID).count(), 0);
    assertEquals(query.groupId(INVALID_GROUP_ID).count(), 0);
    assertEquals(query.assignerId(INVALID_ASSIGNER_ID).count(), 0);
    assertEquals(query.operationType(INVALID_HISTORY_EVENT_TYPE).count(), 0);

  }

  /**
   * Should add 3 history records of identity link addition at 01-01-2016
   * 00:00.00 Should add 3 history records of identity link deletion at
   * 01-01-2016 12:00.00
   * 
   * Should add 3 history records of identity link addition at 01-01-2016
   * 12:30.00 Should add 3 history records of identity link deletion at
   * 01-01-2016 21:00.00
   * 
   * Test case: Query the number of added records at different time interval.
   */
  @Deployment(resources = { "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testShouldAddTaskOwnerForAddandDeleteIdentityLinkByTimeStamp() {

    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    ClockUtil.setCurrentTime(newYearMorning(0));
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    // Adds aUserId1, deletes aUserID1, adds aUserId2, deletes aUserId2, Adds aUserId3 - 5
    addUserIdentityLinks(taskId);

    ClockUtil.setCurrentTime(newYearNoon(0));
    //Deletes aUserId3
    deleteUserIdentityLinks(taskId);

    ClockUtil.setCurrentTime(newYearNoon(30));
    addUserIdentityLinks(taskId);

    ClockUtil.setCurrentTime(newYearEvening());
    deleteUserIdentityLinks(taskId);

    // Query records with time before 12:20
    HistoricIdentityLinkQuery query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.dateBefore(newYearNoon(20)).count(), 6);
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_ADD.getEventName()).count(), 3);
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_DELETE.getEventName()).count(), 3);

    // Query records with time between 00:01 and 12:00
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.dateBefore(newYearNoon(0)).count(), 6);
    assertEquals(query.dateAfter(newYearMorning(1)).count(), 1);
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_ADD.getEventName()).count(), 0);
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_DELETE.getEventName()).count(), 1);

    // Query records with time after 12:45
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.dateAfter(newYearNoon(45)).count(), 1);
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_ADD.getEventName()).count(), 0);
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_DELETE.getEventName()).count(), 1);

    ClockUtil.setCurrentTime(new Date());
  }

  @SuppressWarnings("deprecation")
  @Deployment(resources = { "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testQueryAddAndRemoveIdentityLinksForProcessDefinition() throws Exception {

    ProcessDefinition latestProcessDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).singleResult();
    assertNotNull(latestProcessDef);
    List<IdentityLink> links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
    assertEquals(0, links.size());

    // Add candiate group with process definition
    repositoryService.addCandidateStarterGroup(latestProcessDef.getId(), GROUP_1);
    List<HistoricIdentityLink> historicIdentityLinks = historyService.createHistoricIdentityLinkQuery().list();
    assertEquals(historicIdentityLinks.size(), 1);
    // Query test
    HistoricIdentityLinkQuery query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.processDefId(latestProcessDef.getId()).count(), 1);
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_ADD.getEventName()).count(), 1);
    assertEquals(query.groupId(GROUP_1).count(), 1);

    // Add candidate user for process definition
    repositoryService.addCandidateStarterUser(latestProcessDef.getId(), USER_1);
    // Query test
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.processDefId(latestProcessDef.getId()).count(), 2);
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_ADD.getEventName()).count(), 2);
    assertEquals(query.userId(USER_1).count(), 1);

    // Delete candiate group with process definition
    repositoryService.deleteCandidateStarterGroup(latestProcessDef.getId(), GROUP_1);
    // Query test
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.processDefId(latestProcessDef.getId()).count(), 3);
    assertEquals(query.groupId(GROUP_1).count(), 2);
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_DELETE.getEventName()).count(), 1);

    // Delete candidate user for process definition
    repositoryService.deleteCandidateStarterUser(latestProcessDef.getId(), USER_1);
    // Query test
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.processDefId(latestProcessDef.getId()).count(), 4);
    assertEquals(query.userId(USER_1).count(), 2);
    query = historyService.createHistoricIdentityLinkQuery();
    assertEquals(query.operationType(HistoryEventTypes.IDENTITY_LINK_DELETE.getEventName()).count(), 2);
  }

  public void addUserIdentityLinks(String taskId) {
    for (int userIndex = 1; userIndex <= numberOfUsers; userIndex++)
      taskService.addUserIdentityLink(taskId, A_USER_ID + userIndex, IdentityLinkType.ASSIGNEE);
  }

  public void deleteUserIdentityLinks(String taskId) {
    for (int userIndex = 1; userIndex <= numberOfUsers; userIndex++)
      taskService.deleteUserIdentityLink(taskId, A_USER_ID + userIndex, IdentityLinkType.ASSIGNEE);
  }

  public Date newYearMorning(int minutes) {
    Calendar calendar = new GregorianCalendar();
    calendar.set(Calendar.YEAR, 2016);
    calendar.set(Calendar.MONTH, 0);
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, minutes);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date morning = calendar.getTime();
    return morning;
  }

  public Date newYearNoon(int minutes) {
    Calendar calendar = new GregorianCalendar();
    calendar.set(Calendar.YEAR, 2016);
    calendar.set(Calendar.MONTH, 0);
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 12);
    calendar.set(Calendar.MINUTE, minutes);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date morning = calendar.getTime();
    return morning;
  }

  public Date newYearEvening() {
    Calendar calendar = new GregorianCalendar();
    calendar.set(Calendar.YEAR, 2016);
    calendar.set(Calendar.MONTH, 0);
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 21);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date morning = calendar.getTime();
    return morning;
  }

  protected ProcessInstance startProcessInstance(String key) {
    return runtimeService.startProcessInstanceByKey(key);
  }

}
