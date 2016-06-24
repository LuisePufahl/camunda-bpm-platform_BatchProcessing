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
package org.camunda.bpm.example.invoice;

import static org.camunda.bpm.engine.variable.Variables.*;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;

import org.camunda.bpm.application.PostDeploy;
import org.camunda.bpm.application.ProcessApplication;
import org.camunda.bpm.application.impl.ServletProcessApplication;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.authorization.Groups;
import org.camunda.bpm.engine.impl.util.ClockUtil;
import org.camunda.bpm.engine.impl.util.IoUtil;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;

/**
 * Process Application exposing this application's resources the process engine.
 */
@ProcessApplication
public class InvoiceProcessApplication extends ServletProcessApplication {

  /**
   * In a @PostDeploy Hook you can interact with the process engine and access
   * the processes the application has deployed.
   */
  @PostDeploy
  public void startFirstProcess(ProcessEngine processEngine) {

    createUsers(processEngine);
    startProcessInstance(processEngine);
  }

  private void startProcessInstance(ProcessEngine processEngine) {

    //InputStream invoiceInputStream = InvoiceProcessApplication.class.getClassLoader().getResourceAsStream("invoice.pdf");

    // process instance 1
    processEngine.getRuntimeService().startProcessInstanceByKey("retailerProcess", createVariables()
        .putValue("custName", "John")
        .putValue("custAdress", "Madrid")
        .putValue("orderItems", "TV, Soundbar"));

    // process instance 2
    processEngine.getRuntimeService().startProcessInstanceByKey("retailerProcess", createVariables()
            .putValue("custName", "John")
            .putValue("custAdress", "Madrid")
            .putValue("orderItems", "TV cable"));
     

      //processEngine.getIdentityService().setAuthentication("demo", Arrays.asList(Groups.CAMUNDA_ADMIN));
      //Task task = processEngine.getTaskService().createTaskQuery().processInstanceId(pi.getId()).singleResult();
      //processEngine.getTaskService().claim(task.getId(), "demo");
      //processEngine.getTaskService().complete(task.getId(), createVariables().putValue("approved", true));

  }

  private void createUsers(ProcessEngine processEngine) {

    // create demo users
    new DemoDataGenerator().createUsers(processEngine);
  }
}
