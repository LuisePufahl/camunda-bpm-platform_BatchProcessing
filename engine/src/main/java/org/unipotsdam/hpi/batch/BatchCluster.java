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
	
	public String INIT = "init";
	public String READY = "ready";
	public String RUNNING = "running";
	public String TERMINATED = "terminated";
	public String MAXLOADED = "maxloaded";
	
	public BatchCluster(BatchRegion batchRegion){
		this.currentState = this.INIT;
		System.out.println("BatchCluster "+this+" in state \""+ currentState+ "\"");
		this.batchRegion = batchRegion;
		this.userTaskExists = false;
	}
	
	

	public void addInstance(ActivityExecution instance) {
		instances.add(instance);
		if (instances.size()>= batchRegion.getMaxBatchSize()){
			currentState = this.MAXLOADED;
			System.out.println("BatchCluster "+this+" in state \""+ currentState+ "\"");
		}else{
			checkActivationRuleFor();
		}
		
		if (currentState == this.READY || currentState == this.MAXLOADED){
			
			if (userTaskExists){
				List<TaskEntity> tasks = Context.getCommandContext().getTaskManager().findTasksByExecutionId(instances.get(0).getId());
				//TODO improve, that it is the batch task
				TaskEntity task = tasks.get(0);
				
				
				
				((UserTaskBatchBehavior) instance.getActivity().getActivityBehavior()).addNewInstances((ActivityExecution) task, instance);
								
			}else{
				executeCluster(instances.get(0));
				//TODO usually the task is now in state running, if it is not a userBatchTask
			}
			
			
			
			
		}
		
	}

	private void executeCluster(ActivityExecution exInst) {
		((BatchBehavior)exInst.getActivity().getActivityBehavior()).composite(instances);
		this.userTaskExists = true;
		((BatchBehavior)exInst.getActivity().getActivityBehavior()).executeBA(exInst);
		
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
		executeCluster(instances.get(0));
		currentState = this.READY;
		System.out.println("BatchCluster "+this+" in state \""+ currentState+ "\"");
	}


	protected void continueWithExecution(ActivityExecution execution) {
		nextActInst.add(execution);
		
		if(nextActInst.size()==instances.size()){
			instances.clear();
			instances.addAll(nextActInst);
			executeCluster(nextActInst.get(0));
			nextActInst.clear();
		}
	}


}
