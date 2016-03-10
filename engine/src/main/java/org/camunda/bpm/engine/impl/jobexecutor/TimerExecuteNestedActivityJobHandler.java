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
package org.camunda.bpm.engine.impl.jobexecutor;

import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.core.instance.CoreExecution;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;


/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class TimerExecuteNestedActivityJobHandler extends TimerEventJobHandler {

  public static final String TYPE = "timer-transition";

  public String getType() {
    return TYPE;
  }

  public void execute(String configuration, CoreExecution context, CommandContext commandContext, String tenantId) {
    String activityId = getKey(configuration);
    ExecutionEntity execution = (ExecutionEntity) context;
    ActivityImpl activity = execution.getProcessDefinition().findActivity(activityId);

    ensureNotNull("Error while firing timer: boundary event activity " + configuration + " not found", "boundary event activity", activity);

    try {

      execution.executeEventHandlerActivity(activity);

    } catch (RuntimeException e) {
      throw e;

    } catch (Exception e) {
      throw new ProcessEngineException("exception during timer execution: " + e.getMessage(), e);
    }
  }
}
