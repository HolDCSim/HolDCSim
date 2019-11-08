package network;

import java.util.*;

import debug.Sim;
import experiment.ExperimentConfig;
import topology.Topology.NetworkRoutingAlgorithm;

/**
 * Implements ElasticTree algorithm: https://www.usenix.org/legacy/event/nsdi10/tech/full_papers/heller.pdf
 * @author Kathy Nguyen
 * 
 * Some code borrowed from: https://stackoverflow.com/questions/17480022/java-find-shortest-path-between-2-points-in-a-distance-weighted-map
 */

class Node implements Comparable<Node>
{
    public final int matrixNodeId;
    public Vector<Link> adjacencies;
    public double minDistance = Double.POSITIVE_INFINITY;
    public Node previous;
    
    public Node(int matrixNodeId) {
    	this.matrixNodeId = matrixNodeId;
    }
    
    public String toString() {
    	return Integer.toString(matrixNodeId);
    }
    
    public int compareTo(Node other)
    {
        return Double.compare(minDistance, other.minDistance);
    }

}

/**
 * 2 nodes have a link between them if they are connected in topology
 */
class Link
{
    public final Node target;
    public double weight;
    private boolean available;
    
    public Link(Node target, double weight) {
    	this.target = target;
    	this.weight = weight;
    	available = true;
    }
    
    public void setAvailable(boolean available) {
    	this.available = available;
    }
    
    public boolean getAvailable() {
    	return available;
    }
}

public class Djikstra
{
	private double minBWPerFlow;
	private NetworkRoutingAlgorithm networkRoutingAlgorithm;
	private Vector<Node> nodes;
	private Random random; // To choose between ties in paths
	
	public Djikstra(int numNodes, ExperimentConfig expConfig) {
		// Initialize nodes
		nodes = new Vector<Node>();
		
		// Add all nodes to vertices
		for(int i = 1; i <= numNodes; i++) {
			nodes.add(new Node(i));
			
			// Initialize adjacencies vector
			nodes.get(i - 1).adjacencies = new Vector<Link>();
		}
		
		// Calculate min BW per flow based on QoS
		int qos = expConfig.getJobQoS();
		double avgFlowSize = expConfig.getAvgFlowSize();
		double edgeSwitchBW = expConfig.getEdgeSwitchBW(); // Edge switch has lowest BW. Use for QoS calculation
		
		double qosTimeConstraint = qos * (avgFlowSize / edgeSwitchBW);
		
		minBWPerFlow = avgFlowSize / qosTimeConstraint;
		
		// Min BW per flow cannot be greater than BW provided by edge switch
		if(minBWPerFlow > edgeSwitchBW) {
			minBWPerFlow = edgeSwitchBW;
		}
		
		// Initialize networkRoutingAlgorithm
		networkRoutingAlgorithm = expConfig.getNetworkRoutingAlgorithm();
		
		// Initialize random generator
		long randomSeed = expConfig.getSeed();
		if(randomSeed == -1) {
			// True random seed
			random = new Random(System.currentTimeMillis());
		}
		else {
			// Fixed random seed
			random = new Random(randomSeed);
		}
	}
	
    public void computePaths(int srcMatrixNodeID, int dstMatrixNodeID, boolean useUnavailableLinks) {
    	Node srcNode = nodes.get(srcMatrixNodeID - 1);
    	
        srcNode.minDistance = 0.;
        PriorityQueue<Node> nodeQueue = new PriorityQueue<Node>();
		HashSet<Node> visited = new HashSet<Node>();
        nodeQueue.add(srcNode);

        visited.add(srcNode);

        while (!nodeQueue.isEmpty()) {
            Node u = nodeQueue.poll();

            // Visit each edge exiting u
            for (Link link : u.adjacencies) {
            	if(!useUnavailableLinks) {
                	if(link.getAvailable()) {
                        Node v = link.target;
                        double weight = link.weight;
                        double distanceThroughU = u.minDistance + weight;

                        if (visited.contains(v)) {
                        	continue;
						}

						if (distanceThroughU < v.minDistance ) {
                            v.minDistance = distanceThroughU;
                            visited.add(v);
                            updatePrevious(nodeQueue, v, u);
                        }
                        else if(distanceThroughU == v.minDistance) {
                        	if(networkRoutingAlgorithm == NetworkRoutingAlgorithm.DJIKSTRA) {
                        		// If tie, choose random path out of tie
                        		double randomDouble = random.nextDouble();
                        		if(randomDouble >= 0.5) {
                        			visited.add(v);
                        			updatePrevious(nodeQueue, v, u);
                        		}
                        	}
                        	else if(networkRoutingAlgorithm == NetworkRoutingAlgorithm.ELASTIC_TREE ||
									networkRoutingAlgorithm == NetworkRoutingAlgorithm.POPCORNS ||
									networkRoutingAlgorithm == NetworkRoutingAlgorithm.WASP) {
                        		// If tie, choose node with smaller matrix node id. This solves the problem of arbitrary nodes being chosen based off the way Djikstra's algorithm iterates through different weight matrices

								if(v.previous !=null && u.matrixNodeId < v.previous.matrixNodeId && v.previous.matrixNodeId != srcMatrixNodeID) {
                        			updatePrevious(nodeQueue, v, u);
                        			visited.add(v);
                        		}
                        	}
                        }
                	}
            	}
            	else {
                    Node v = link.target;
                    double weight = link.weight;
                    double distanceThroughU = u.minDistance + weight;
                    if (distanceThroughU < v.minDistance && !visited.contains(v)) {
                        nodeQueue.remove(v);

                        v.minDistance = distanceThroughU ;
                        v.previous = u;
                        nodeQueue.add(v);
                        visited.add(v);
                    }
            	}
            }

        }
    }
    
    public Vector<Integer> getShortestPathTo(int dstMatrixNodeID, int srcMatrixNodeID) {
    	Node dstNode = nodes.get(dstMatrixNodeID - 1);
    	
        List<Node> path = new ArrayList<Node>();
        for (Node node = dstNode; node != null; node = node.previous) {
        	path.add(node);
        }

        Collections.reverse(path);
        
        // Reset nodes for next path calculation
        for(Node node : nodes) {
        	node.minDistance = Double.POSITIVE_INFINITY;
        	node.previous = null;
        }
        
        // No path available. Need to use unavailable links to get to dst
        if(path.get(0).matrixNodeId != srcMatrixNodeID) {
        	computePaths(srcMatrixNodeID, dstMatrixNodeID,true);
        	return getShortestPathTo(dstMatrixNodeID, srcMatrixNodeID);
        }
        else {
            Vector<Integer> returnPath = new Vector<Integer>();
            for(Node node: path) {
            	returnPath.add(node.matrixNodeId);
            }
            
            return returnPath;
        }
    }
    
    public void setAdjacency(int matrixNodeID, int targetMatrixNodeID, double weight) {
    	nodes.get(matrixNodeID - 1).adjacencies.add(new Link(nodes.get(targetMatrixNodeID - 1), weight));
    }
    
    /**
     * Update link bi-directionally
     */
    public void updateLink(int srcMatrixNodeID, int dstMatrixNodeID, double linkRate, int numAllocatedFlows) {
    	Link direction1Link = null;
    	Link direction2Link = null;
    	
    	Node srcNode = nodes.get(srcMatrixNodeID - 1);
    	for(Link link : srcNode.adjacencies) {
    		if(link.target.matrixNodeId == dstMatrixNodeID) {
    			direction1Link = link;
    			updateWeight(direction1Link, linkRate, numAllocatedFlows);
    			break;
    		}
    	}
    	
    	Node dstNode = nodes.get(dstMatrixNodeID - 1);
    	for(Link link : dstNode.adjacencies) {
    		if(link.target.matrixNodeId == srcMatrixNodeID) {
    			direction2Link = link;
    			updateWeight(direction2Link, linkRate, numAllocatedFlows);
    			break;
    		}
    	}
    	
    	// Removed adjacency if it will violate minBWPerFlow constraint upon next flow being scheduled
    	if(linkRate / (numAllocatedFlows + 1) < minBWPerFlow) {
    		direction1Link.setAvailable(false);
    		direction2Link.setAvailable(false);
    	}
    	// Add adjacency back if it will no longer violate minBWPerFlow constraint upon next flow being scheduled
    	else if(!direction1Link.getAvailable() || !direction2Link.getAvailable()) {
    		direction1Link.setAvailable(true);
    		direction2Link.setAvailable(true);
    	}
    	
    	if(direction1Link == null || direction2Link == null) {
    		Sim.fatalError("Couldn't updated link between nodes " + srcMatrixNodeID + " and " + dstMatrixNodeID);
    	}
    }
    
    /**
     * Update previous node in Djikstra's path
     */
    public void updatePrevious(PriorityQueue<Node> nodeQueue, Node targetNode, Node previousNode) {
    	nodeQueue.remove(targetNode);
    	targetNode.previous = previousNode;
        nodeQueue.add(targetNode);
    }
    
    /**
     * Update weights of links based on network routing algorithm bandwidth for Elastic Tree algorithm
     */
    public void updateWeight(Link link, double linkRate, int numAllocatedFlows) {
    	// Elastic Tree algorithm updates weights based off bandwidth
    	if(networkRoutingAlgorithm == NetworkRoutingAlgorithm.ELASTIC_TREE) {
			if(numAllocatedFlows == 0) {
				link.weight = linkRate;
			}
			else {
				link.weight = linkRate / numAllocatedFlows;
			}
    	}
    }
    
    /**
     * Update weights of links based on weight matrix calculated in PopCorns algorithm
     */
    public void updateWeightPopCorns(int srcMatrixNodeID, int dstMatrixNodeID, double weight) {
    	boolean updatedLink1 = false;
    	boolean updatedLink2 = false;
    	
    	Node srcNode = nodes.get(srcMatrixNodeID - 1);
    	for(Link link : srcNode.adjacencies) {
    		if(link.target.matrixNodeId == dstMatrixNodeID) {
    			link.weight = weight;
    			updatedLink1 = true;
    			break;
    		}
    	}
    	
    	Node dstNode = nodes.get(dstMatrixNodeID - 1);
    	for(Link link : dstNode.adjacencies) {
    		if(link.target.matrixNodeId == srcMatrixNodeID) {
    			link.weight = weight;
    			updatedLink2 = true;
    			break;
    		}
    	}
    	
    	if(!updatedLink1 || !updatedLink2) {
    		Sim.fatalError("Couldn't updated link between nodes " + srcMatrixNodeID + " and " + dstMatrixNodeID);
    	}
    }
}