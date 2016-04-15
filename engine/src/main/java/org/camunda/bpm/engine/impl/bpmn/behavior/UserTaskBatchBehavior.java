package org.camunda.bpm.engine.impl.bpmn.behavior;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.el.ExpressionManager;
import org.camunda.bpm.engine.impl.el.FixedValue;
import org.camunda.bpm.engine.impl.form.handler.DefaultFormHandler;
import org.camunda.bpm.engine.impl.form.handler.DefaultTaskFormHandler;
import org.camunda.bpm.engine.impl.form.handler.DelegateTaskFormHandler;
import org.camunda.bpm.engine.impl.form.handler.FormFieldHandler;
import org.camunda.bpm.engine.impl.form.handler.FormFieldValidationConstraintHandler;
import org.camunda.bpm.engine.impl.form.handler.TaskFormHandler;
import org.camunda.bpm.engine.impl.form.type.AbstractFormFieldType;
import org.camunda.bpm.engine.impl.form.type.StringFormType;
import org.camunda.bpm.engine.impl.form.validator.FormFieldValidator;
import org.camunda.bpm.engine.impl.form.validator.FormFieldValidatorContext;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
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


private TaskDecorator taskDecorator;
   private BatchRegion batchRegion;

   
   //in order to differentiate the TaskDef
   private int numOfBatches;
	
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
		
		numOfBatches++;
		
		ActivityExecution batchExecution = instances.get(0);
			
		buildJSON(batchExecution,instances);
		
	}
	
	
	private FormFieldHandler createSeparatorForExecution(String executionID) {
		
		FormFieldHandler formFieldHandler = new FormFieldHandler();
		ExpressionManager expressionManager = Context
		        .getProcessEngineConfiguration()
		        .getExpressionManager();
		
//		FormFieldHandler.setId("Seperator"+executionID.replace("-", "_"));
//		FormFieldHandler.setLabel("---------------------------------------" +
//												"Process instance - " + executionID.replace("-", "_") +
//												"---------------------------------------");
//		//FormFieldHandler.setWritable(false);
//		//FormFieldHandler.setReadable(true);
//		
//		FormTypes formTypes = Context
//		        .getProcessEngineConfiguration()
//		        .getFormTypes();
//		AbstractFormFieldType formType = formTypes.getFormType("string");
//		
//		FormFieldHandler.setType((AbstractFormFieldType)formType);
//		
//		//Expression defaultExpression = expressionManager.createExpression("Execution no. " + executionID.replace("-", "_"));
//        //FormFieldHandler.setDefaultExpression(defaultExpression);
		
		
		
		
		formFieldHandler.setLabel(new FixedValue("---------------------------------------" +
												"Process instance - " + executionID +
												"---------------------------------------"));
		//formFieldHandler.setDefaultValueExpression(new FixedValue("Execution no. " + executionID));
		formFieldHandler.setType(new StringFormType());
		formFieldHandler.setId("Seperator"+executionID.replace("-", "_"));
		// Make the separator readonly
		List<FormFieldValidationConstraintHandler> validationHandlers = new ArrayList<FormFieldValidationConstraintHandler>();
		
		FormFieldValidationConstraintHandler readonlyHandler = new FormFieldValidationConstraintHandler();
		// Set the field to readonly
		readonlyHandler.setName("readonly");
		// Actually a ReadOnlyValidator does not work the expected way
		readonlyHandler.setValidator(new FormFieldValidator() {
			@Override
			public boolean validate(Object submittedValue, FormFieldValidatorContext validatorContext) {
				return true;
			}
		});
		validationHandlers.add(readonlyHandler);
		formFieldHandler.setValidationHandlers(validationHandlers);
        
        return formFieldHandler;
	}
	
	private FormFieldHandler copyFormFieldHandler(FormFieldHandler fieldHandler, String executionId, ActivityExecution execution) {
		FormFieldHandler copy = new FormFieldHandler();
		copy.setLabel(fieldHandler.getLabel());
		// Make sure that the id is unique
		copy.setId(fieldHandler.getId() + executionId.replace("-", "_"));
		
		//copy.se(fieldHandler.getId()+ executionId.replace("-", "_"));
		copy.setDefaultValueExpression(fieldHandler.getDefaultValueExpression());
		copy.setType((AbstractFormFieldType)fieldHandler.getType());
		copy.setValidationHandlers(fieldHandler.getValidationHandlers());			
		// Now copy the value of the variable
		Object variable = Context.getCommandContext().getExecutionManager().findExecutionById(executionId).getVariable(fieldHandler.getId());
		Context.getCommandContext().getExecutionManager().findExecutionById(execution.getId()).setVariable(copy.getId(), variable);
		
		return copy;
	}
	
	private FormFieldHandler copyBatchFormFieldHandler(FormFieldHandler fieldHandler) {
		FormFieldHandler copy = new FormFieldHandler();
		copy.setLabel(fieldHandler.getLabel());
		// Make sure that the id is unique
		copy.setId(fieldHandler.getId() + "batch");
		
		//copy.se(fieldHandler.getId()+ executionId.replace("-", "_"));
		copy.setDefaultValueExpression(fieldHandler.getDefaultValueExpression());
		copy.setType((AbstractFormFieldType)fieldHandler.getType());
		copy.setValidationHandlers(fieldHandler.getValidationHandlers());			
		
		//assumption, batch variable has to current value so far
		
		return copy;
	}
	
	private TaskDefinition copyTaskDef(TaskDefinition taskDef, TaskFormHandler formHandler, ActivityExecution execution) {
		TaskDefinition newTaskDefinition = new TaskDefinition(formHandler);
				
		newTaskDefinition.setAssigneeExpression(taskDef.getAssigneeExpression());
		newTaskDefinition.setDescriptionExpression(taskDef.getDescriptionExpression());
		newTaskDefinition.setDueDateExpression(taskDef.getDueDateExpression());
		Expression name = taskDef.getNameExpression();
//TODO data View ausgeben		
        newTaskDefinition.setNameExpression((Expression)new FixedValue(name + " [Batch Activity] DataViewDefinition: " + batchRegion.getGroupingChar()));
		//newTaskDefinition.setNameExpression(taskDef.getNameExpression());
		newTaskDefinition.setPriorityExpression(taskDef.getPriorityExpression());
		newTaskDefinition.setTaskListeners(taskDef.getTaskListeners());
		newTaskDefinition.setKey(taskDef.getKey()+"_Batch"+numOfBatches);
		
			
		return newTaskDefinition;
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
