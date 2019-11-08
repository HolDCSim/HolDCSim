#!/bin/bash

index=1

while [ $index -le 10 ]
do
   echo "$index"
   echo " java -jar DCSim.jar 10000 0.1 4.2e-3 1 1 60 random"
   java -jar DCSim.jar 10000 0.1 4.2e-3 1 1 60 random
   ((index++))
done

index=1
while [ $index -le 10 ]
do
   echo "$index"
   echo " java -jar DCSim.jar 10000 0.1 4.2e-3 1 1 30 random"
   java -jar DCSim.jar 10000 0.1 4.2e-3 1 1 30 random
   ((index++))
done

index=1
while [ $index -le 10 ]
do
   echo "$index"
   echo " java -jar DCSim.jar 10000 0.1 194e-3 1 1 60 random"
   java -jar DCSim.jar 10000 0.1 194e-3 1 1 60 random
   ((index++))
done

index=1
while [ $index -le 10 ]
do
   echo "$index"
   echo " java -jar DCSim.jar 10000 0.1 194e-3 1 1 30 random"
   java -jar DCSim.jar 10000 0.1 194e-3 1 1 30 random
   ((index++))
done
