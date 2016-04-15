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

package org.camunda.bpm.engine.test.bpmn.usertask;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.impl.util.ClockUtil;
import org.camunda.bpm.engine.runtime.JobQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;


/**
 * @author Joram Barrez
 */
public class BatchTest extends PluggableProcessEngineTestCase {

  @Deployment
  public void testIt() {
    runtimeService.startProcessInstanceByKey("myProc");
    runtimeService.startProcessInstanceByKey("myProc");
    List<Task> tasks = taskService.createTaskQuery().list();
  
    Map<String, Object> formProperties = new HashMap<String, Object>();
    formProperties.put("firstname", "Kermit");
    formProperties.put("lastname", "TheFrog");
    
    
    taskService.complete(tasks.get(0).getId(),formProperties);
    
  //  formProperties.put("lastname", "bla");
    taskService.complete(tasks.get(1).getId(),formProperties);
        
    
    Task task = taskService.createTaskQuery().singleResult();
  	assertNotNull(task);
  	
  	//Object test= formService.getRenderedTaskForm(task.getId());
  	//taskService.complete(task.getId());
  	
    //runtimeService.startProcessInstanceByKey("myProc");
    
    // make sure user task exists
    //tasks = taskService.createTaskQuery().list();
  	
    //taskService.complete(tasks.get(1).getId(),formProperties);
    
    // first batch task
    Task batchTask = taskService.createTaskQuery().singleResult();
  	Object test= formService.getRenderedTaskForm(batchTask.getId());
  	Object test2= formService.getTaskFormVariables(batchTask.getId());
  	taskService.complete(batchTask.getId());
  	
  	//second batch task
  	batchTask = taskService.createTaskQuery().singleResult();
  	test= formService.getRenderedTaskForm(batchTask.getId());
  	taskService.complete(batchTask.getId());
  	
  	//test Timer
  	//ProcessInstance pi = runtimeService.startProcessInstanceByKey("myProc");
  	//task = taskService.createTaskQuery().singleResult();
  	//taskService.complete(task.getId(),formProperties);
  	
  	// Set the clock fixed
    //Date startTime = new Date();
    //JobQuery jobQuery = managementService.createJobQuery().processInstanceId(pi.getId());
    //assertEquals(1, jobQuery.count());

    // After setting the clock to time '50minutes and 5 seconds', the second timer should fire
    //ClockUtil.setCurrentTime(new Date(startTime.getTime() + ((1 * 60 * 1000) + 5000)));
    //waitForJobExecutorToProcessAllJobs(5000L);
    
  	
  }

}
