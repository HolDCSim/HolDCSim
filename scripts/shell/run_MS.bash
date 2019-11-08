#!/bin/bash
rous=( 0.1 0.3 0.5 0.8 )
freqs=( 1.0 )
#for u in 194e-3
#do	
#   for p in 0.1 0.3 0.5 0.8
#   do
#	for freq in 1.0
#	do
#		for servers in 2 4 8 16 
#		do
#			for T
#			do
#				echo "$jobs $p $i $freq"
#				echo "java -jar DCSim.jar rou: $p u: $u freq: $freq sleepState: $i servers: $servers activeK: $activeK"
#				 java -jar DCSim.jar 20000 $p $u $freq $i $servers $servers  multi_server
#
#			 done
#		done	
#		
#	done
#   done	   
#done
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  2 2 1 2  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  2 2 1 4  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  2 2 1 8  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  2 2 2 4  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  2 2 2 8  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  2 2 3 6  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  2 2 3 8  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  2 2 3 10  multi_server

java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  4 4  1 2  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  4 4  1 4  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  4 4  1 8  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  4 4  2 4  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  4 4  2 8  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  4 4  3 6  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  4 4  3 8  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  4 4  3 10  multi_server


java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  8 8  1 2  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  8 8  1 4  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  8 8  1 8  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  8 8  2 4  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  8 8  2 8  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  8 8  3 6  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  8 8  3 8  multi_server
java -jar DCSim.jar 20000 0.1 4.2e-3  1.0  1  8 8  3 10  multi_server
#done
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  2 2 1 2  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  2 2 1 4  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  2 2 1 8  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  2 2 2 4  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  2 2 2 8  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  2 2 3 6  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  2 2 3 8  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  2 2 3 10  multi_server


java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  4 4  1 2  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  4 4  1 4  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  4 4  1 8  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  4 4  2 4  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  4 4  2 8  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  4 4  3 6  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  4 4  3 8  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  4 4  3 10  multi_server


java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  8 8  1 2  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  8 8  1 4  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  8 8  1 8  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  8 8  2 4  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  8 8  2 8  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  8 8  3 6  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  8 8  3 8  multi_server
java -jar DCSim.jar 20000 0.3 4.2e-3  1.0  1  8 8  3 10  multi_server
#done
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  2 2 1 2  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  2 2 1 4  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  2 2 1 8  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  2 2 2 4  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  2 2 2 8  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  2 2 3 6  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  2 2 3 8  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  2 2 3 10  multi_server


java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  4 4  1 2  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  4 4  1 4  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  4 4  1 8  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  4 4  2 4  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  4 4  2 8  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  4 4  3 6  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  4 4  3 8  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  4 4  3 10  multi_server


java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  8 8  1 2  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  8 8  1 4  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  8 8  1 8  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  8 8  2 4  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  8 8  2 8  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  8 8  3 6  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  8 8  3 8  multi_server
java -jar DCSim.jar 20000 0.5 4.2e-3  1.0  1  8 8  3 10  multi_server
