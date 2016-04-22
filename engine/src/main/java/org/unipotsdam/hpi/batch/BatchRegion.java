package org.unipotsdam.hpi.batch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.impl.bpmn.behavior.BatchBehavior;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

/**
 *  
 * @author Luise Pufahl, Jan Selke
 */
public class BatchRegion {
		
	//batch region configuration		
		private int maxBatchSize;
		private int threshold;
		private String timeout;
		private List <String> groupingChar;
		
		private String exitActivity;
		private String entryActivity;
	
	//Maps for managing the batch clusters
		
		/**
		 * This maps DataViews as List of Strings to the BatchClusters for which they are
		 * the GroupingCharacteristic
		 */
		private Map<List <String>,List<BatchCluster>> clusters;
		
		/**
		 * This maps ProcessInstanceIDs as Strings to their assigned BatchCluster
		 */
		private Map<String,BatchCluster> assignedPIsToClusters;
		
		
		
		//timeout
		//https://docs.camunda.org/manual/7.3/guides/user-guide/#process-engine-the-job-executor
		//https://en.wikipedia.org/wiki/ISO_8601#Durations
		

	/**
	 * Constructor for a BatchRegion
	 * 
	 * initializes a BathcRegion with the specified Parameters,
	 * additionally the two Maps clusters and assignedPIsToClusters are
	 * initialized for later management of BatchClusters.
	 * @param groupingCharactistic DataViews that determine which ProcessInstances are
	 * assigned to a BatchCluster of this BatchRegion according to the data attribute
	 * values of their data objects
	 * @param maxCapacity maximum capacity for process instances of the BatchClusters for
	 * this BatchRegion
	 * @param pthreshold minimum amount of process instances needed for Activation of a
	 * BatchCluster for this BatchRegion
	 * @param timeOut time interval after witch the BatchRegion will start execution after
	 * arrival of a ProcessInstance
	 * @param entryAct first Activity of the BatchRegion, starts the timeOut-Interval
	 * @param exitAct last Activity of the BatchRegion, after its Execution the
	 * associated BatchCluster is removed and terminated.
	 */
		public BatchRegion(List <String> groupingCharactistic, int maxCapacity, int pthreshold, String timeOut, String entryAct, String exitAct) {
			maxBatchSize = (maxCapacity > 1) ? maxCapacity : 1;
			threshold = (pthreshold > 1) ? pthreshold : 1;
			groupingChar = groupingCharactistic;
			timeout = timeOut;
			entryActivity = entryAct;
			exitActivity = exitAct;
			clusters = new HashMap<List <String>,List<BatchCluster>>();	
			assignedPIsToClusters = new HashMap<String,BatchCluster>();
			
		}
	
		/**
		 * adds a ProcessInstance to a BatchCluster.
		 * 
		 * If the ProcessInstanceID is allready assigned to a BatchCluster, the Execution
		 * will be continued.
		 * When the ProcessInstanceID has not been assigned allready, it will be tried to
		 * find a BatchCluster that is assigned to the DataView of the ProcessInstance
		 * and currently in state INIT or READY for Assignment of the ProcessInstance.
		 * In Case said BatchCluster does not exists, a new one will be created and the
		 * ProcessInstance will be assigned accordingly.
		 * @param execution Execution that represents the ProcessInstance
		 */
		public void assignToCluster(ActivityExecution execution) {
			
			List <String> dataView = new ArrayList <String>();
			
			// check whether PI has already a cluster to which it is assigned
			
			if (assignedPIsToClusters.containsKey(execution.getProcessInstanceId())){
				
				assignedPIsToClusters.get(execution.getProcessInstanceId()).continueWithExecution(execution);
				return;
				
			}else{
				//assignment of PI to a cluster
				
				if (!groupingChar.isEmpty()){
					for(int i=0; i < groupingChar.size(); i++)
					dataView.add((String) execution.getVariable(groupingChar.get(i)));

					
				}else{
					dataView = groupingChar;
				}
					
				// Find the right cluster
				if(clusters.containsKey(dataView)){
					List<BatchCluster> dataViewclusters = clusters.get(dataView);
					for (BatchCluster cluster : dataViewclusters) {
						if (cluster.getCurrentState()==cluster.INIT || cluster.getCurrentState()==cluster.READY) {
							cluster.addInstance(execution);
							assignedPIsToClusters.put(execution.getProcessInstanceId(), cluster);
							
// TODO							dataViewclusters.add(cluster);
							
							return;
						}
					}
					// Assigning to a fresh BatchCluster if none of the existing BatchClusters was in state INIT or READY
					BatchCluster newCluster = new BatchCluster(this);
					newCluster.addInstance(execution);
					assignedPIsToClusters.put(execution.getProcessInstanceId(), newCluster);
					dataViewclusters.add(newCluster);
					clusters.put(dataView, dataViewclusters);
					
					return;
									
					
				}else{	// If there are no BatchClusters for the given DataView, the ProcessInstance is assigned to a freshly created BatchCluster			
					BatchCluster newCluster = new BatchCluster(this);
					newCluster.addInstance(execution);
					assignedPIsToClusters.put(execution.getProcessInstanceId(), newCluster);
					List<BatchCluster> dataViewclusters = new ArrayList<BatchCluster>();
					dataViewclusters.add(newCluster);
					clusters.put(dataView, dataViewclusters);
					return;
					
				}
			}
						
							
		}
		
		/**
		 * Removes a BatchCluster from a BatchRegion
		 * @param batchCluster BatchCluster to be removed
		 */
		protected void removeCluster(BatchCluster batchCluster){
			clusters.remove(batchCluster);
			List<String>tobeRemovedPis = new ArrayList<String>();
			for(String pi:assignedPIsToClusters.keySet()){
				if (assignedPIsToClusters.get(pi)==batchCluster){
					tobeRemovedPis.add(pi);
				}
			}
			
			for(String tobeRemovedPi:tobeRemovedPis){
				assignedPIsToClusters.remove(tobeRemovedPi);
			}
			
			
		}

		/**
		 * Returns the assigned BatchCluster for a given ProcessInstanceID
		 * @param processInstId ProcessInstanceID, String that identifies the ProcessInstance
		 * @return BatchCluster to which the ProcessInstanceID has been asigned
		 */
		public BatchCluster getClusterforPI(String processInstId) {
			return assignedPIsToClusters.get(processInstId);
		}
		
		/**
		 * Returns the Map of DataViews and their assigned BatchClusters
		 * @return Map that that assigns DataViews as Strings to their BatchClusters for which they are the GroupingCharacteristic
		 */
		public Map<List <String>, List<BatchCluster>> getClusters() {
			return clusters;
		}

		/**
		 * Returns the maximum capacity for process instances of a BatchCluster for this
		 * BatchRegion
		 * @return maximum capacity of a BatchCluster for this BatchRegion
		 */
		public int getMaxBatchSize() {
			return maxBatchSize;
		}

		/**
		 * Returns the minimum amount of process instances needed for Activation of a
		 * BatchCluster for this BatchRegion
		 * @return minimum Activation-Treshold of a BatchCluster for this BatchRegion
		 */
		public int getThreshold() {
			return threshold;
		}
		
		/**
		 * Returns the ExitActivity of the BatchRegion which is the last Activity of the BatchCluster.
		 * After the Execution of the ExitActivity the BatchCluster is removed and terminated.
		 * @return ExitActivity of the BatchRegion
		 */
		public String getExitActivity() {
			return exitActivity;
		}
		
		/**
		 * the EntryActivity of the BatchRegion 
		 * @return the EntryActivity of the BatchRegion
		 */
		public String getEntryActivity() {
			return entryActivity;
		}
		
		/**
		 * Returns the time interval after which the BatchRegion starts execution upon
		 * arrival of a ProcessInstance 
		 * @return Timeout
		 */
		public String getTimeout() {
			return timeout;
		}
	
		public String getGroupingChar(){			
			return(groupingChar.toString());
		}
	
}
