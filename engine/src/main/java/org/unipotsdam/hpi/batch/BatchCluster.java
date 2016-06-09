package org.unipotsdam.hpi.batch;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.camunda.bpm.engine.impl.bpmn.behavior.BatchBehavior;
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskBatchBehavior;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

public class BatchCluster {
	
	private List<ActivityExecution> instances = new LinkedList<ActivityExecution>();
	private List<ActivityExecution> nextActInst = new LinkedList<ActivityExecution>();
	
	public List<ActivityExecution> getInstances() {
		return instances;
	}

	private String currentState;
	private BatchRegion batchRegion;
	private Boolean userTaskExists;
	private ActivityExecution batchExecution;
	
	public String INIT = "init";
	public String READY = "ready";
	public String RUNNING = "running";
	public String TERMINATED = "terminated";
	public String MAXLOADED = "maxloaded";
	
	public BatchCluster(ActivityExecution batchExecution, BatchRegion batchRegion){
		this.currentState = this.INIT;
		System.out.println("BatchCluster "+this+" in state \""+ currentState+ "\"");
		this.batchRegion = batchRegion;
		this.userTaskExists = false;
		this.batchExecution =  batchExecution;
	}
	
	

	public void addInstance(ActivityExecution instance) {
		instances.add(instance);
		if (instances.size()>= batchRegion.getMaxBatchSize()){
			// now the batch region will not assign any new instances to this cluster
			currentState = this.MAXLOADED;
			System.out.println("BatchCluster "+this+" in state \""+ currentState+ "\"");
		}else{
			checkActivationRuleFor();
		}
		
		if (currentState == this.READY || currentState == this.MAXLOADED){
			
			
			
			if (userTaskExists){
				/*List<TaskEntity> tasks = Context.getCommandContext().getTaskManager().findTasksByExecutionId(instances.get(0).getId());
				//TODO improve, that it is the batch task
				TaskEntity task = tasks.get(0);
				
				
				
				((UserTaskBatchBehavior) instance.getActivity().getActivityBehavior()).addNewInstances(batchExecution, instance);*/
				((UserTaskBatchBehavior) instance.getActivity().getActivityBehavior()).addNewInstances(batchExecution, instance);
								
			}else{
				executeCluster();
				//TODO usually the task is now in state running, if it is not a userBatchTask
			}
			
		}
			
		
	}

	private void executeCluster() {
		((BatchBehavior)batchExecution.getActivity().getActivityBehavior()).composite(batchExecution, instances);
		this.userTaskExists = true;
		((BatchBehavior)batchExecution.getActivity().getActivityBehavior()).executeBA(batchExecution);
		
	}


	private void checkActivationRuleFor() {
	//TODO extend to time out	
		if (instances.size()>= batchRegion.getThreshold()){
			currentState = this.READY;
			System.out.println("BatchCluster "+this+" in state \""+ currentState+ "\"");
		}
		
	}
	
	public void terminate(ActivityExecution execution){
		
		// only when the last activity of a batch region is executed the batch cluster is removed and terminated
		if (execution.getActivity().getId().equals(batchRegion.getExitActivity())){
			batchRegion.removeCluster(this);
			this.currentState=this.TERMINATED;
			System.out.println("BatchCluster "+this+" in state \""+ currentState+ "\"");
		}
		
	}
	
	
	
	public String getCurrentState(){
		return this.currentState;
	}
	
	public void setToRunning(){
	   if (this.currentState!=this.RUNNING){
		   this.currentState=this.RUNNING;
		   System.out.println("BatchCluster "+this+" in state \""+ currentState+ "\"");
	   }
	   
	}
	
	public void activate(){
		executeCluster();
		currentState = this.READY;
		System.out.println("BatchCluster "+this+" in state \""+ currentState+ "\"");
	}


	protected void continueWithExecution(ActivityExecution execution) {
		nextActInst.add(execution);
		
		if(nextActInst.size()==instances.size()){
			instances.clear();
			instances.addAll(nextActInst);
			batchExecution = instances.get(0);
			executeCluster();
			nextActInst.clear();
		}
	}


}
