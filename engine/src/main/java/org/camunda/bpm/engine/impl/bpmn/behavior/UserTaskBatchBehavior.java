package org.camunda.bpm.engine.impl.bpmn.behavior;



import java.util.List;
import java.util.Map;
import java.util.Set;

import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.el.FixedValue;
import org.camunda.bpm.engine.impl.form.handler.TaskFormHandler;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.camunda.bpm.engine.impl.task.TaskDecorator;
import org.camunda.bpm.engine.impl.task.TaskDefinition;
import org.unipotsdam.hpi.batch.BatchCluster;
import org.unipotsdam.hpi.batch.BatchRegion;



public class UserTaskBatchBehavior extends UserTaskActivityBehavior implements
BatchBehavior {
	public BatchRegion getBatchRegion() {
		return batchRegion;
	}


   private BatchRegion batchRegion;
   private int numOfBatches;
   private TaskDecorator taskDecorator;
   
   
	
	public UserTaskBatchBehavior(TaskDecorator taskDecorator, BatchRegion batchRegion) {
		super(taskDecorator);
		this.taskDecorator = taskDecorator;
		this.batchRegion = batchRegion;
		this.numOfBatches = 0;
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
				if (variableId.contains("batch") && !variableId.equals("batchCluster")){
					String varId = variableId.replace("batch", "");
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


	private void buildJSON(ActivityExecution batchExecution, List<ActivityExecution> instances){
	
	//get names of ProcessVariables
	Set<String> names = batchExecution.getVariableNames();
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
			if (name.equals("batchCluser")){
				
			}else{
				JSONvariable = JSONvariable + "\"" + name + "\" : ";
				Object value = processInstance.getVariable(name);
				JSONvariable = JSONvariable + "\""+ value.toString()+"\"";
				// place "," between attribute-key-value-pairs except for last pair
				if (j < names.size()){
					JSONvariable = JSONvariable + ",";
				}
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
	Context.getCommandContext().getExecutionManager().findExecutionById(batchExecution.getId()).setVariable("batchCluster", JSONvariable);
	}
	
	@Override
	public void composite(ActivityExecution batchExecution, List<ActivityExecution> instances) {
		
		numOfBatches++;
			
		buildJSON(batchExecution,instances);
		
		TaskDefinition newTaskDef = copyTaskDef(this.taskDecorator.getTaskDefinition(), batchExecution);
		TaskDecorator adaptedTaskDecorator = new TaskDecorator (newTaskDef, taskDecorator.getExpressionManager()); 
		super.taskDecorator = adaptedTaskDecorator;
		

		
	}
	
	@SuppressWarnings("deprecation")
	private TaskDefinition copyTaskDef(TaskDefinition taskDef, ActivityExecution batchExecution) {
		TaskDefinition newTaskDefinition = new TaskDefinition(taskDef.getTaskFormHandler());
		newTaskDefinition.setAssigneeExpression(taskDef.getAssigneeExpression());
		newTaskDefinition.setDescriptionExpression(taskDef.getDescriptionExpression());
		newTaskDefinition.setDueDateExpression(taskDef.getDueDateExpression());
		Expression name = (Expression) taskDef.getNameExpression();
		
		//TODO data View ausgeben
		String dataView = "";
		for (String groupCharElement:batchRegion.getGroupingChar()){
			dataView = dataView + Context.getCommandContext().getExecutionManager().findExecutionById(batchExecution.getId()).getVariable(groupCharElement);
		}
		
        newTaskDefinition.setNameExpression((Expression)new FixedValue(name + " [Batch Activity_"+dataView+"]"));
		//newTaskDefinition.setNameExpression(taskDef.getNameExpression());
		newTaskDefinition.setPriorityExpression(taskDef.getPriorityExpression());
		newTaskDefinition.setTaskListeners(taskDef.getTaskListeners());
		newTaskDefinition.setKey(taskDef.getKey()+"_Batch"+numOfBatches);
		newTaskDefinition.setFormKey(taskDef.getFormKey());
		
		//add new batch task definition to existing task definitions of the process definition
		((ProcessDefinitionEntity) batchExecution.getActivity().getProcessDefinition()).getTaskDefinitions()
		.put(newTaskDefinition.getKey(), newTaskDefinition);
		
		//Context.getBpmnExecutionContext().getProcessDefinition().getTaskDefinitions().put(newTaskDefinition.getKey(), newTaskDefinition);
					
		return newTaskDefinition;
	}
	
		
	public void addNewInstances(ActivityExecution batchExecution, ActivityExecution instance){

		String JSONString = (String) Context.getCommandContext().getExecutionManager().findExecutionById(batchExecution.getId()).getVariable("batchCluster");
		String instanceID = instance.getProcessInstanceId();
		Set<String> names = batchExecution.getVariableNames();
		JSONString = JSONString.substring(0, JSONString.length()-1); 
		JSONString = JSONString + ",\"" + instanceID + "\" : {";
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
		Context.getCommandContext().getExecutionManager().findExecutionById(batchExecution.getId()).setVariable("batchCluster", JSONString);
				
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
