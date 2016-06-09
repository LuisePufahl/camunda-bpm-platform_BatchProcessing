package org.camunda.bpm.engine.impl.bpmn.behavior;

import java.util.List;

import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

public interface BatchBehavior {
	
	//void assign();
	
	//void composite();
	
	void executeBA(ActivityExecution execution);
	
	//void terminate();

	void composite(ActivityExecution batchExecution, List<ActivityExecution> executions);

}
