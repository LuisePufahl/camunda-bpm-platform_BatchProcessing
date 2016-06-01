package org.camunda.bpm.engine.impl.jobexecutor;

import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.unipotsdam.hpi.batch.BatchCluster;
import org.unipotsdam.hpi.batch.BatchRegion;
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskBatchBehavior;

public class BatchTimerJobHandler extends TimerEventJobHandler {

	public static final String TYPE = "batch-timer";
	
	@Override
	public String getType() {
		return TYPE;
	}



	@Override
	public void execute(TimerJobConfiguration configuration, ExecutionEntity execution, CommandContext commandContext,
			String tenantId) {
				// get related batch cluster
				BatchRegion batchRegion = ((UserTaskBatchBehavior)execution.getActivity().getActivityBehavior()).getBatchRegion();
				//set batch cluster in running
				BatchCluster bCluster = batchRegion.getClusterforPI(execution.getProcessInstanceId());
				
				if (bCluster.getCurrentState() == bCluster.INIT ){
					bCluster.activate();
				}
		
	}


}
