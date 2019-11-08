# Week of July 28

---

1. Reconfigured the output of the LCNetwork Experiment so it is more compact and more easily parsable 
2. Solved the bug that was causing none of the switch sleep state transitions to be recorded properly
3. Implemented the graphing method for the switch traces and developed a python scripts to graph the data collected and output by the simulator
	* Can graph the total number of linecards in each sleep state
	* Can graph the sleep state traces for up to 8 specified linecards

TODO:  

* Further develop the topology generation using NetworX Python library 

* Further develop the output of the topology from the simulator so it can be easily graphed using NetworkX

* Parameterize the sampling rate of the graphing methods to allow the user reduce the number of samples being graphed


Example Switch Trace Graph\
![Switch Trace](switch_trace.png)
