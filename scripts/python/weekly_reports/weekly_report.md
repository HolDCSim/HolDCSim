# Week of July 21

---

1. Modularized the graphing methods to better work with every experiment type
2. Found and fixed a bug with one of the graphs when experiment time/data set was too large
3. Found a python library to use to draw the topology -- NetworkX
	* Working to create an output of the topology from the simulator in a format which I can use to draw the network using NetworkX
	* Have discussed with Kathy the way in which the topology data is formatted and stored
	* Will also use NetworkX to assign colors to each switch and server in the diagram equivalent to their average sleep state -- generates a heat map of utilization across the network\

Example Figure generated with NetworkX:\
![Topology](topo_graph_1.png)
