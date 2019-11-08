package topology;

import java.util.ArrayList;
import java.util.Iterator;  
import java.util.Stack;
import java.util.Vector;

public class FindAllPath {

	/* 表示一个节点以及和这个节点相连的所有节点 */  
	public class Node  
	{  
		public String name = null; 
	    public int index = 0;
	    public ArrayList<Node> relationNodes = new ArrayList<Node>();  
	  
	    public String getName() {  
	        return name;  
	    }  
	  
	    public void setName(String name) {  
	        this.name = name;  
	    }  
	    
	    public void setIndex(int index) {  
	        this.index = index;  
	    }
	    
	    public int getIndex() {  
	        return index;  
	    }
	  
	    public ArrayList<Node> getRelationNodes() {  
	        return relationNodes;  
	    }  
	  
	    public void setRelationNodes(ArrayList<Node> relationNodes) {  
	        this.relationNodes = relationNodes;  
	    }
	}
	
	    /* 临时保存路径节点的栈 */  
	    public static Stack<Node> stack = new Stack<Node>();  
	    /* 存储路径的集合 */  
	    public static ArrayList<Object[]> sers = new ArrayList<Object[]>();  
	    // all the paths
	    public static Vector<Vector<Integer>> allPaths;
	  
	    /* 判断节点是否在栈中 */  
	    public static boolean isNodeInStack(Node node)  
	    {  
	        Iterator<Node> it = stack.iterator();  
	        while (it.hasNext()) {  
	            Node node1 = (Node) it.next();  
	            if (node == node1)  
	                return true;  
	        }  
	        return false;  
	    }  
	  
	    /* 此时栈中的节点组成一条所求路径，转储并打印输出 */  
	    public static void showAndSavePath()  
	    {   
	        Object[] o = stack.toArray();
	        Vector<Integer> path = new Vector<Integer>();
	        //System.out.println("path"+path+"allPath"+allPaths);
	        for (int i = 0; i < o.length; i++) {  
	            Node nNode = (Node) o[i];
	            path.add(nNode.getIndex()+1);
//	            if(i < (o.length - 1))  
//	                System.out.print(nNode.getName() + "->");  
//	            else  
//	                System.out.print(nNode.getName());  
	        }  
	        sers.add(o); /* 转储 */  
	        //System.out.println("\n");
	        //System.out.println("path:"+path);
	        allPaths.add(path);
	    }  
	  
	    /* 
	     * 寻找路径的方法  
	     * cNode: 当前的起始节点currentNode 
	     * pNode: 当前起始节点的上一节点previousNode 
	     * sNode: 最初的起始节点startNode 
	     * eNode: 终点endNode 
	     */  
	    public static boolean getPaths(Node cNode, Node pNode, Node sNode, Node eNode) {  
	        Node nNode = null;  
	        /* 如果符合条件判断说明出现环路，不能再顺着该路径继续寻路，返回false */  
	        if (cNode != null && pNode != null && cNode == pNode) {
	        		return false;
	        }
	             
	        if (cNode != null) {  
	            int i = 0;  
	            /* 起始节点入栈 */  
	            stack.push(cNode);  
	            /* 如果该起始节点就是终点，说明找到一条路径 */  
	            if (cNode == eNode)  
	            {  
	                /* 转储并打印输出该路径，返回true */  
	                showAndSavePath();  
	                //System.out.println("nodeC");
	                return true;  
	            }  
	            /* 如果不是,继续寻路 */  
	            else  
	            {  
	                /*  
	                 * 从与当前起始节点cNode有连接关系的节点集中按顺序遍历得到一个节点 
	                 * 作为下一次递归寻路时的起始节点  
	                 */  
	                nNode = cNode.getRelationNodes().get(i);  
	                while (nNode != null) {  
	                    /* 
	                     * 如果nNode是最初的起始节点或者nNode就是cNode的上一节点或者nNode已经在栈中 ，  
	                     * 说明产生环路 ，应重新在与当前起始节点有连接关系的节点集中寻找nNode 
	                     */  
	                    if (pNode != null  
	                            && (nNode == sNode || nNode == pNode || isNodeInStack(nNode))) {  
	                        i++;  
	                        if (i >= cNode.getRelationNodes().size())  
	                            nNode = null;  
	                        else  
	                            nNode = cNode.getRelationNodes().get(i);  
	                        continue;  
	                    }  
	                    /* 以nNode为新的起始节点，当前起始节点cNode为上一节点，递归调用寻路方法 */  
	                    if (getPaths(nNode, cNode, sNode, eNode))/* 递归调用 */  
	                    {  
	                        /* 如果找到一条路径，则弹出栈顶节点 */  
	                        stack.pop();  
	                    }  
	                    /* 继续在与cNode有连接关系的节点集中测试nNode */  
	                    i++;  
	                    if (i >= cNode.getRelationNodes().size())  
	                        nNode = null;  
	                    else  
	                        nNode = cNode.getRelationNodes().get(i);  
	                }  
	                /*  
	                 * 当遍历完所有与cNode有连接关系的节点后， 
	                 * 说明在以cNode为起始节点到终点的路径已经全部找到  
	                 */  
	                stack.pop();  
	                return false;  
	            }  
	        } else  
	            return false;  
	    }
	    
	    public Vector<Vector<Integer>> FindAllPath(int c, int p, int s, int e, Vector<Vector<Integer>> nodeConnectivity) {
	    	allPaths = new Vector<Vector<Integer>>();
	    /* 定义节点关系 */
	    //	int nodeRalation [][] = new int [36][4];
	    	Vector<Vector<Integer>> nodeRalation = new Vector<Vector<Integer>>();
	    	for (int k = 0; k < nodeConnectivity.size(); k++) {
	    		Vector<Integer> CurrentnodeRelation = new Vector<Integer>();
	    		CurrentnodeRelation = nodeConnectivity.get(k);
	    		//int currentRelation [] = new int[4];
	    		Vector<Integer> currentRelation = new Vector<Integer>();
	    		for (int j = 0; j < CurrentnodeRelation.size(); j++) {
	    			if (CurrentnodeRelation.get(j) == 10) {
	    				currentRelation.add(j);
	    				//System.out.println("hhhhhh"+currentRelation[m]);
	    			}
	    		}
	    		nodeRalation.add(currentRelation);
	    	}
	    //	System.out.println("hhhhhh"+nodeRalation);
	    
//        int nodeRalation[][] =  
//        {  
////            {1},      //0  
////            {0,5,2,3},//1  
////            {1,4},    //2  
////            {1,4},    //3  
////            {2,3,5},  //4  
////            {1,4}     //5  
//        		  {2, 3, 20, 21},	//1
//        		  {2, 3, 22, 23},	//2
//        		  {0, 1, 16, 17},	//3
//        		  {0, 1, 18, 19},	//4
//        		  {6, 7, 24, 25},   //4
//        		  {6, 7, 26, 27},	//5
//        		  {4, 5, 16, 17},   //6
//        		  {4, 5, 18, 19},	//7
//        		  {10, 11, 28, 29},	//8
//        		  {10, 11, 30, 31},	//9
//        		  {8, 9, 16, 17}, //10
//        		  {8, 9, 18, 19}, //11
//        		  {14, 15, 32, 33},	//12
//        		  {14, 15, 34, 35},	//13
//        		  {12, 13, 16, 17}, //14
//        		  {12, 13, 18, 19},	//15
//        		  {2, 6, 10, 14},	//16
//        		  {2, 6, 10, 14}, 	//17
//        		  {3, 7, 11, 15},	//18
//        		  {3, 7, 11, 15},	//19
//        		  {0},	//20
//        		  {0},	//21
//        		  {1},	//22
//        		  {1},	//23
//        		  {4},	//24
//        		  {4},	//25
//        		  {5},	//26
//        		  {5},	//27
//        		  {8},	//28
//        		  {8},	//29
//        		  {9},	//30
//        		  {9},	//31
//        		  {12},	//32
//        		  {12},	//33
//        		  {13},	//34
//        		  {13}	//35
//        };
         //for (int i = 1;)
          
        /* 定义节点数组 */  
        //Node[] node = new Node[nodeRalation.length]; 
	    	Node[] node = new Node[nodeRalation.size()];
	    	for(int i=0;i<nodeRalation.size();i++)   
        //for(int i=0;i<nodeRalation.length;i++)  
        {  
            node[i] = new Node();  
            node[i].setName("node" + i); 
            node[i].setIndex(i);
        }  
          
        /* 定义与节点相关联的节点集合 */ 
	    	for(int i=0;i<nodeRalation.size();i++)  
        //for(int i=0;i<nodeRalation.length;i++)  
        {  
            ArrayList<Node> List = new ArrayList<Node>();
            for(int j=0;j<nodeRalation.get(i).size();j++)  
            //for(int j=0;j<nodeRalation[i].length;j++)  
            {  
                //List.add(node[nodeRalation[i][j]]);
            	List.add(node[nodeRalation.get(i).get(j)]);
            }  
            node[i].setRelationNodes(List);  
            List = null;  //释放内存  
        }
	    /* 开始搜索所有路径 */  
        getPaths(node[c], null, node[s], node[e]);
        //System.out.println("c"+c+"e"+e);
        return allPaths;
	    }
}
	

