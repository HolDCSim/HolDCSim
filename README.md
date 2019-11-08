# HolDCSim -- A Holistic Simulator for Data Centers

## Description

### Experiments
**Linecard Network Experiment:** Experiment which implements sleep states in both the servers and line cards. Has the ability to use a global queue by user speicification. User can also set the amount of time each Line Card waits in the idle state before going into sleep. User can also set the job inter arrival time using the parameters.

**Sleep Scale Experiment:** Experiment with 1 server, and provides 5 sleep states that the server can go into: Active state, idle state, shallow sleep state, deep sleep state, and deepest sleep state. This experiment allows users to explore CPU C-States in a server.

**Windowed Sleep Scale Experiment:** Extension of Sleep Scale Experiment. User specify a “window” param. The “window” param’s unit is in ms. The param enforces the server to spend this much time in each sleep state upon each transition

**Multi Server Experiment:** Simple, default experiment where servers are always on. Takes 1 parameter- the # of servers

**Shallow Deep Experiment:** Introduces the concept of shallow and deep sleep states. Experiment where two traffic thresholds are defined in user params- Ts and Tw. Ts is the lower bound, and Tw is the higher bound. When the traffic rate dips lower than Ts, servers will be put into shallow sleep until the traffic rate is >= Ts. When the traffic rate goes to higher than Tw, servers will be woken up (starting from servers in the lightest sleep state) until the traffic rate is <= Tw. The purpose of the shallow deep experiment is to maintain an incoming job traffic rate between Ts and Tw. After a server has entered shallow sleep state, it has the potential to enter deep sleep state(s), as specified by experiments that extend the Shallow Deep Experiment (ERover Experiment, Dual ERover Experiment).

**ERover Experiment:** Extension of Shallow Deep Experiment. User specifies a param which is the delay time between shallow sleep state and deep sleep state(s).

**Dual ERover Experiment:** Extension of the ERover experiment. A portion of the servers have a low delay time between shallow sleep state and deep sleep state(s). The other portion of servers have a longer delay time between shallow sleep state and deep sleep state(s). These delay times are specified by user params

**On Off Experiment:** Experiment where server will immediately be turned off when all 4 cores are idle, and then be turned on again once a core is needed for an incoming job.

**Delay Off Experiment:** Experiment where server will be turned off after they are idle for a specified delay time. This delay time is specified by user params

**Delay Doze Experiment:** Experiment where servers will go to deep sleep state after they are idle for a specified delay time. This delay time is specified by user params

**Dual Delay Doze Experiment:** Experiment where a portion of the servers have a low delay time before going into deep sleep after being idle. The other portion of servers have a longer delay timer before going into deep sleep. THese delay times are specified by user params




### Configuration
| Parameter 				| Description 										| Value 				|
|---------------			|---------------									|---------------		|
|`topo_type` 				| Network Topology to be used 						|`fattree`,`bcube`,`camcube`,`smalltopo`,`fbfly`,`flowtesttopo`	|
|`topo_params`				| k value for topolgoy								| `k=(int)`				|
|`num_of_sockets`			| Number of CPU sockets								| _Integer_ (default = 1)|
|`cores_per_socket`			| Number of cores per CPU socket					| _Integer_ (default = 1)|
|`network_routing_algorithm`| Which networking algorithm the experiment will use | `popcorns`,`elastc_tree`,`djikstra` |
|`core_switch_bw`			| Bandwidth of the Core Switches 					| _Integer_ (default=100000) |
|`aggregate_switch_bw`		| Bandwidth of the Aggregate Switches   			| _Integer_ (default=10000)  |
|`edge_switch_bw`			| Bandwidth of the Edge Switches					| _Integer_ (default=1000)	 |
|`core_lc_active`			| Core Switch Active Power (Watts)					| _Integer_ (default=300)	 |
|`core_lc_lpi1`				| Core Switch LPI1 Power (Watts)					| _Integer_ (default=176)    |
|`core_lc_lpi2`				| Core Switch LPI2 Power (Watts)					| _Integer_ (default=117)    |
|`core_lc_lpi3`				| Core Switch LPI3 Power (Watts)        			| _Integer_ (default=72)     |
|`aggregate_lc_active`		| Aggregate Switch Active Power (Watts)				| _Integer_ (default=200)  	|
|`aggregate_lc_lpi1`		| Aggregate Switch LPI1 Power (Watts) 				| _Integer_ (default=120)	|
|`aggregate_lc_lpi2`		| Aggregate Switch LPI2 Power (Watts)				| _Integer_ (default=78)	|
|`aggregate_lc_lpi3`		| Aggregate Switch LPI3	Power (Watts)				| _Integer_ (default=48)	|
|`edge_lc_active`			| Edge Switch Active Power (Watts)					| _Integer_ (default=150)	|
|`edge_lc_lpi1`				| Edge Switch LPI1 Power (Watts) 					| _Integer_ (default=75)	|
|`edge_lc_lpi2` 			| Edge Switch LPI2 Power (Watts)					| _Integer_ (default=58)	|
|`edge_lc_lpi3`				| Edge Switch LPI3 Power (Watts) 					| _Integer_ (default=28)	|
|`lc_active_to_lpi1_time`	| Time before Switch transisitions from Active to LPI1 | _Double_ (default=0.250)|
|`lc_lpi1_to_lpi2_time`		| Time before Switch transitions from LPI1 to LPI2  |_Double_ (default=0.750)	|
|`lc_lpi2_to_lpi3_time`		| Time before Switch transitions from LPI2 to LPI3	|_Double_ (default=1.80)	|
|`lc_lpi3_to_off_time`		| Time before Switch transitions from LPI3 to Off	|_Double_ (default=4.00)	|
|`Server_active_to_1_Time`	| Time before Server transitions from Active to Shallow Sleep |_Double_ (default=0.01)|	
|`Server_1_to_4_Time`		| Time before Server transitions from Shallow to Deep Sleep	|_Double_ (default=0.5)|
|`Server_4_to_5_Time`		| Time before Server transitions from Deep to Deepest Sleep |_Double_ (default=1)|
|`port_power_policy`		| Power Policy of device Ports						| `no_management`,`port_lpi` 		|
|`line_card_power_policy`	| Power Policy of Line Cards						| `no_management`,`linecard_sleep` 	|
|`ports_per_line_card`		| Number of ports per Line Card						| _Integer_ (default=4)				|
|`num_of_jobs`				| Number of Jobs for the Experiment					| `fixed (int)`,`random`			|
|`job_QoS`					| Job Quality of Service Constraint					| _Integer_
|`num_of_tasks`				| Number of Tasks									| `fixed (int)` , `random`			|
|`task_size`				| Size of Each Task									| `fixed (int)` , `random`			|
|`all_jobs`					| The type of workload | `stride`,`hotspot`,`onetoone`,`random`,`small_topo_custom`,`test_trans`,`flow_test`,`WSEARCH`,`WSERVICE`,`DNS`|
|`packet_num`				| Number of packets data is split into for communication 	| `fixed (int)`, `random` | 
|`flow_size`				| Size of the flows in the experiment						| _Integer_ (default = 1) |
|`job_workload`				| The Job Workload to be used for the experiment | `poisson`,`mmpp`,`mixed`,`mmpp_fluent`,`mmpp_oracle`,`trace`,`user_specify`|
|`mixture`					| Proportion of Mixed Workload							| Double between 0 and 1 (default = 0.950) |
|`service_dis`				| Service Distribution, either exponential or uniform 	| `unif`, `expo` |
|`trace_filename`			| Filename for workload trace 							| |
|`path_preolad`				| Whether or not to preload the path when initializing the experiment | _Boolean_ `0`,`1` |
|`seed`						| Seed used to randomize values during the experiment	| _Integer_	|
|`verbose`					| Whether outputs are printed while running the experiement | _Boolean_ `0`,`1` |
|`debug_level`				| Sets the debug level for the experiment, the higher the debug level, the more information is printed | `0`,`1`,`2`,`3` |
|`task_scheduler`			| Sets the task scheduler to be used in the experiment | `energy_aware`,`simple`,`test_trans`,`one_server`|
|`sche_policy`				| Sets the scheduling poicy, either first pick or random available 				| `first_pick`, `random_avail` | 
|`multi_states`				| Whether the Servers are set to use multiple sleep state levels 						| _Boolean_ `0`,`1` |
|`job_arrivals`				| Whether to log the job arrival distribution 											| _Boolean_ `0`,`1` |
|`job_completes`			| Whether to log the job completion distribuition 										| _Boolean_ `0`,`1` |
|`collect_stats`			| Whether to collect server statistics throughout experiment 							| _Boolean_ `0`,`1` |
|`print_full_his`			| Whether to print the full statistics history to log file 								| _Boolean_ `0`,`1` |
|`print_agg_stats`			| Whether to write the task start and completion time statistics to a log file 			| _Boolean_ `0`,`1` |
|`allserver_stats`			| Whether to write all server stats to an output file for later parsing 				| _Boolean_ `0`,`1` |
|`idle_distribuition`		| Whether to write the server idle distribuition to a log file 							| _Boolean_ `0`,`1` |
|`state_durations`			| Whether to write the amount of time spent in each sleep state 						| _Boolean_ `0`,`1` | 
|`energy_distributions`		| Whether to log the amount of energy used by each sleep state level 					| _Boolean_ `0`,`1` |
|`abs_percentile_90`		| Whether or not to calculate and show the 90th Percentile Latency						| _Boolean_ `0`,`1` |
|`skip_initial_jobs`		| The number of jobs to skip as warm-up jobs during initialization 						| _Boolean_ `0`,`1` |
|`timing_check`				| Whether or not to perform a timing check to validate a successful simulation 			| _Boolean_ `0`,`1` |
|`dump_prediction`			| Whether or not to log the prediction history 											| _Boolean_ `0`,`1` |
|`dump_sch_serverhis`		| Whether to log the history of when servers are set to active by the scheduler 		| _Boolean_ `0`,`1` | 
|`dump_act_serverhis`		| Whether to log the active servers throughout the experiment 							| _Boolean_ `0`,`1` |  
|`dump_queue_his`			| Whether to log the queue history 														| _Boolean_ `0`,`1` | 
|`latency_percent`			| The latency percent used when calculating job latency statistics 						| _Double_ betwen 0 and 1 |
|`asyn_log`					| Enables asynchronous logging, dump_prediction, dump_sch_serverhis no longer take effect when enabled |  _Boolean_ `0`,`1` |
|`cycle`					| The length of the time window for MMPP workload (seconds)								| _Integer_ (default = 30)|
|`aratio`					| Arrival Rate for MMPP workload														| _Double_ (default = 4.0)|
|`tratio`					| Ratio of Active Time to Inactive Time 												| Between 1 and 4 |
|`s4_provision`				| Percentage of servers required to be in the s4 sleep state during ERover Experiment 	| _Integer_ |
|`sampling_rate`			| The number of samples which are calculated and logged for server and switch graphs 	| _Integer_ |
|`frames`					| The number of frames for which data is collected for topology animation 				| _Integer_ |
