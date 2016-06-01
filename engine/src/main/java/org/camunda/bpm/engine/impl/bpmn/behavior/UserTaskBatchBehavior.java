package org.camunda.bpm.engine.impl.bpmn.behavior;



import java.util.List;
import java.util.Map;
import java.util.Set;

import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.camunda.bpm.engine.impl.task.TaskDecorator;
import org.unipotsdam.hpi.batch.BatchCluster;
import org.unipotsdam.hpi.batch.BatchRegion;

public class UserTaskBatchBehavior extends UserTaskActivityBehavior implements
BatchBehavior {
	public BatchRegion getBatchRegion() {
		return batchRegion;
	}


   private BatchRegion batchRegion;

   
	
	public UserTaskBatchBehavior(TaskDecorator taskDecorator, BatchRegion batchRegion) {
		super(taskDecorator);
		this.batchRegion = batchRegion;
	}
	
	public void execute(ActivityExecution execution){
		batchRegion.assignToCluster(execution);
	}
	
	public void signal(ActivityExecution execution, String signalName, Object signalData) throws Exception {
		
		BatchCluster batchCluster = batchRegion.getClusterforPI(execution.getProcessInstanceId());
		
		List<ActivityExecution> instances = batchCluster.getInstances();
		
		Map<String,Object> variables = Context.getCommandContext().getExecutionManager().findExecutionById(execution.getId()).getVariables();
		
		for(ActivityExecution instance:instances){
			
			//writing over the variables
			for(String variableId:variables.keySet()){
				if (variableId.contains("batch")){
					String varId = variableId.replace("batch", "");
					//copy.setId(fieldHandler.getId() + executionId.replace("-", "_"));
					Context.getCommandContext()
						.getExecutionManager()
						.findExecutionById(instance.getId())
						.setVariable(varId,variables.get(variableId));
				}
				
				if (variableId.contains(instance.getId().replace("-", "_"))){
					String varId = variableId.replace(instance.getId().replace("-", "_"), "");
							//copy.setId(fieldHandler.getId() + executionId.replace("-", "_"));
					Context.getCommandContext()
						.getExecutionManager()
						.findExecutionById(instance.getId())
						.setVariable(varId,variables.get(variableId));
				}
			}
			
			if(instance.getId().equals(execution.getId())){
				
			}else{
				ActivityExecution otherExecution = Context.getCommandContext().getExecutionManager().findExecutionById(instance.getId());
				//terminate activity instance
				super.signal(otherExecution, signalName, signalData);
			}
		}
		
		batchCluster.terminate(execution);
		
	    super.signal(execution, signalName, signalData);
	}

	public void buildJSON(ActivityExecution execution, List<ActivityExecution> instances){
	
	//get names of ProcessVariables
	Set<String> names = execution.getVariableNames();
	// open JSON
	String JSONvariable = "{";
	// loop over all processInstances
	int i = 1;
	for (ActivityExecution processInstance : instances){	
		String processInstanceID = processInstance.getProcessInstanceId();
		JSONvariable = JSONvariable + "\"" + processInstanceID + "\" : {";
		// loop over all processVariables
		int j = 1;
		for (String name : names){	
			JSONvariable = JSONvariable + "\"" + name + "\" : ";
			Object value = execution.getVariable(name);
			JSONvariable = JSONvariable + "\""+ value.toString()+"\"";
			// place "," between attribute-key-value-pairs except for last pair
			if (j < names.size()){
				JSONvariable = JSONvariable + ",";
			}
			j++;
		}
		// close Attributes
		JSONvariable = JSONvariable + "}";
		// place "," between processinstances except for last instance
		if (i < instances.size()){
			JSONvariable = JSONvariable + ",";
		}
		i++;
	}
	// close JSON
	JSONvariable = JSONvariable + "}";
	Context.getCommandContext().getExecutionManager().findExecutionById(execution.getId()).setVariable("caseListString", JSONvariable);
	}
	
	@Override
	public void composite(List<ActivityExecution> instances) {
		
		ActivityExecution batchExecution = instances.get(0);
			
		buildJSON(batchExecution,instances);
		
	}
	
	
	
	
	
	
	public void addNewInstances(ActivityExecution execution, ActivityExecution instance){

		String JSONString = (String) execution.getVariable("caseListString");
		String instanceID = instance.getProcessInstanceId();
		Set<String> names = instance.getVariableNames();
		JSONString = JSONString + ",{\"" + instanceID + "\" : {";
		int i = 1;
		for (String name : names){	
			JSONString = JSONString + "\"" + name + "\" : ";
			Object value = instance.getVariable(name);
			JSONString = JSONString + "\""+ value.toString()+"\"";
			// place "," between attribute-key-value-pairs except for last pair
			if (i < names.size()){
				JSONString = JSONString + ",";
			}
			i++;
		}
		JSONString = JSONString + "}}";
		Context.getCommandContext().getExecutionManager().findExecutionById(execution.getId()).setVariable("caseListString", JSONString);
				
	}
	

	@Override
	public void executeBA(ActivityExecution batchExecution) {
		try {
			super.execute(batchExecution);
			Context.getCommandContext().getDbSqlSession().flush();
			//set TaskDecorator back to the original one
			super.taskDecorator = this.taskDecorator;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
