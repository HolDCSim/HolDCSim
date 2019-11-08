#!/bin/bash

rm log/whichFiles.txt

java -jar Untitled.jar -path confs/sim.config -jobspserv 10 -simtime -1 -rou 0.1 -ubar 4.2e-3 -speed 1.0 -sleepstate 1 -sleepunitparam 1 -numcores 4 -numserv 16 -ts 0.13 -tw 1.58 -predtype -1 -singletao 0.5  -b 10 -servqueuethres 10 -avgInterArrivalTime 10000 -globalqueue 1 -exptype lcnetwork_exp

OUT=$(cut -f 1 -d , log/whichFiles.txt)
PWD=$(pwd)
./output_parser.pl "$PWD/log/$OUT"
(python3 pyscripts/graph.py)
(python3 pyscripts/graph2.py 1 2 5 8)
(python3 pyscripts/switch_graph.py)
(python3 pyscripts/switch_graph2.py 1 2 5 8 )
(python3 pyscripts/topo.py)
