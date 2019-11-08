package communication;

import java.util.*;

import job.Task;
import constants.Constants;
import debug.Sim;

public class TaskDependencyGenerator {
	
	private final static int MAXIMUM_STRIDE = 1;
	private final static double HOT_DENSITY = 0.5;

	public static enum NetworkLoad {
		HOTSPOT, STRIDE, ONETOONE, RANDOM, SMALL_TOPO_CUSTOM, FLOW_TEST, TEST_TRANS,
		WSERVICE, WSEARCH, DNS, UNKNOWN
	}

	public static Map<Task, Vector<Task>> generateTaskDependencyGraph(
			Vector<Task> tasks, NetworkLoad wd) {
		if (tasks == null) {
			System.err.println("no tasks in the task set");
			return null;
		}
		
		//radom number generator
		Random rd = null;
		if(Sim.RANDOM_SEEDING){
			rd = new Random(System.currentTimeMillis());
		}else{
			rd = new Random(100000);
		}

		Map<Task, Vector<Task>> taskDependencyGraph = new HashMap<Task, Vector<Task>>();
		final int size = tasks.size();

		if (size == 1) {
			return null;
		}

		double hotDensity = HOT_DENSITY;
		int stride = size <= MAXIMUM_STRIDE ? size - 1 : MAXIMUM_STRIDE;


		switch (wd) {
			case HOTSPOT: {
				ArrayList<Integer> indexes = new ArrayList<Integer>();
			    for(int i = 0; i < tasks.size(); i++ ){
			    	indexes.add(i);
			    }
			    
			    Collections.shuffle(indexes);
			    Task sTask = tasks.get(indexes.get(0));
			    
			    for(int j = 0; j < hotDensity * tasks.size()-1; j++){
			    	Task dTask = tasks.get(indexes.get(j+1));
			    	Vector<Task> vTask = new Vector<Task>();
			    	vTask.add(sTask);
			    	taskDependencyGraph.put(dTask, vTask);
			    }
			    
			    break;
			}
	
			case STRIDE: {
				for (int i = 0; i + stride <= size - 1; i++) {
					Vector<Task> listOfTask = new Vector<Task>();
	
					Task source = tasks.get(i);
					Task dest = tasks.get(i + stride);
					listOfTask.add(source);
	
					taskDependencyGraph.put(dest, listOfTask);
	
				}
				break;
	
			}
	  
			case SMALL_TOPO_CUSTOM: {
				
				//assume only 3 tasks
				Vector<Task> listOfTask = new Vector<Task>();
				//Vector<Task> listOfTask1 = new Vector<Task>();
				listOfTask.add(tasks.get(0));				
				listOfTask.add(tasks.get(1));
				taskDependencyGraph.put(tasks.get(2),listOfTask );
			//	listOfTask1.add(tasks.get(2));
			//	taskDependencyGraph.put(tasks.get(3),listOfTask1 );

				
				break;
			}
			
		case FLOW_TEST: {

//			//assume 6 tasks
//			Vector<Task> listOfTask1 = new Vector<Task>();
//			Vector<Task> listOfTask2 = new Vector<Task>();
//			// Vector<Task> listOfTask1 = new Vector<Task>();
//			listOfTask1.add(tasks.get(2));
//			listOfTask1.add(tasks.get(3));
//			
//			listOfTask2.add(tasks.get(4));
//			listOfTask2.add(tasks.get(0));
//			
//			taskDependencyGraph.put(tasks.get(5), listOfTask1);
//			taskDependencyGraph.put(tasks.get(1), listOfTask2);
//			// listOfTask1.add(tasks.get(2));
//			// taskDependencyGraph.put(tasks.get(3),listOfTask1 );
			
			// For DNS jobs, 4 tasks
			Vector<Task> listOfTask1 = new Vector<Task>();
			// Vector<Task> listOfTask1 = new Vector<Task>();
			listOfTask1.add(tasks.get(0));
			listOfTask1.add(tasks.get(2));
			listOfTask1.add(tasks.get(3));
			
			taskDependencyGraph.put(tasks.get(1), listOfTask1);
			//System.out.println("dependenceGraph"+taskDependencyGraph);
			// listOfTask1.add(tasks.get(2));
			// taskDependencyGraph.put(tasks.get(3),listOfTask1 );
			
			// For Web search jobs, 6 tasks
//			Vector<Task> listOfTask1 = new Vector<Task>();
//			// Vector<Task> listOfTask1 = new Vector<Task>();
//			listOfTask1.add(tasks.get(0));
//			listOfTask1.add(tasks.get(2));
//			listOfTask1.add(tasks.get(3));
//			listOfTask1.add(tasks.get(4));
//			listOfTask1.add(tasks.get(5));
//			
//			taskDependencyGraph.put(tasks.get(1), listOfTask1);
			
			// For Web service jobs, 2 tasks
//			Vector<Task> listOfTask1 = new Vector<Task>();
//			// Vector<Task> listOfTask1 = new Vector<Task>();
//			listOfTask1.add(tasks.get(0));
//			//listOfTask1.add(tasks.get(2));
////			
//			taskDependencyGraph.put(tasks.get(1), listOfTask1);

			break;
		}
		
		    case DNS: {
		    	// For DNS jobs, 4 tasks
				Vector<Task> listOfTask1 = new Vector<Task>();
				// Vector<Task> listOfTask1 = new Vector<Task>();
				listOfTask1.add(tasks.get(0));
				listOfTask1.add(tasks.get(2));
				listOfTask1.add(tasks.get(3));
				
				taskDependencyGraph.put(tasks.get(1), listOfTask1);
				break;
		    }
		    
		    case WSERVICE: {
				// For Web service jobs, 2 tasks
				Vector<Task> listOfTask1 = new Vector<Task>();
				// Vector<Task> listOfTask1 = new Vector<Task>();
				listOfTask1.add(tasks.get(0));
				//listOfTask1.add(tasks.get(2));
				
				taskDependencyGraph.put(tasks.get(1), listOfTask1);
				break;
		    }
		    
		    case WSEARCH: {
				
				// For Web search jobs, 6 tasks
				Vector<Task> listOfTask1 = new Vector<Task>();
				// Vector<Task> listOfTask1 = new Vector<Task>();
				listOfTask1.add(tasks.get(0));
				listOfTask1.add(tasks.get(2));
				listOfTask1.add(tasks.get(3));
				listOfTask1.add(tasks.get(4));
				listOfTask1.add(tasks.get(5));
				
				taskDependencyGraph.put(tasks.get(1), listOfTask1);
				break;
		    }
		    
			case ONETOONE: {
				Vector<Task> tasksTemp = new Vector<Task>();
				
				int nSize = size;
				while (nSize >= 2) {
					// get a pair
					nSize = nSize - 2;
					int source, dest;
					if (nSize == 0) {
						source = 0;
						dest = 1;
					}
	
					else {
						source = rd.nextInt(Integer.MAX_VALUE) % nSize;
						while ((dest = rd.nextInt(Integer.MAX_VALUE) % nSize) == source) {
	
						}
					}
	
					Task src = tasks.get(source);
					Task dst = tasks.get(dest);
	
					Vector<Task> listOfTasks = new Vector<Task>();
					listOfTasks.add(src);
					taskDependencyGraph.put(dst, listOfTasks);
	
					tasks.remove(src);
					tasks.remove(dst);
					tasksTemp.add(src);
					tasksTemp.add(dst);
	
				}
				
				// Add tasks back to vector for use in network experiment
				for(int i = 0; i < tasksTemp.size(); i++) {
					tasks.add(tasksTemp.get(i));
				}
				break;
			}
	
			case RANDOM: {
				
				for (int i = 1; i < size; i++) {
					Task dest = tasks.get(i);
					Vector<Task> listOfTask = new Vector<Task>();
					
					for (int j = 0; j < i; j++){
						Task source = tasks.get(j);
						double havelink= rd.nextFloat();
						if(havelink<=Constants.PROB_OF_DEPEND)	{					
							listOfTask.add(source);						
						}						
					}					
					if(listOfTask.size()!=0){
						taskDependencyGraph.put(dest, listOfTask);
					}	
				}
				break;
			}
			
			case TEST_TRANS:{
				//should be 4 tasks, 1--->2, 3--->4
				if(tasks.size() != 4){
					System.err.println("there should be 4 tasks in TEST_TRANS workload, exit");
					System.exit(0);
				}
				
				Vector<Task> listOfTask1 = new Vector<Task>();
				listOfTask1.add(tasks.get(0));
				
				Vector<Task> listOfTask2 = new Vector<Task>();
				listOfTask2.add(tasks.get(2));
				
				taskDependencyGraph.put(tasks.get(1), listOfTask1);
				taskDependencyGraph.put(tasks.get(3), listOfTask2);
				break;
			}
			
			default:
				return null;

		}
		return taskDependencyGraph;
	}
}