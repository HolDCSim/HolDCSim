#!/bin/bash
#note that this is part of an executable shell command
#this file needs to be included 
if [ $1 -a $2 -a $3 -a $4 ]
then
  tWait=`echo "e($4*l(10))" | bc -l`
  #echo "java -xmx3g -jar ./DCSim.jar 50000 0.3 $meanServiceTime 1.0 1 $k $tSetup $tWait  delayoff_exp"
  echo " java -Xmx3g -jar ./DCSim.jar 80000 0.3 $1 1.0 1 $3 $2 $tWait  delayoff_exp"
  java -Xmx3g -jar ./DCSim.jar 80000 0.3 $1 1.0 1 $3 $2 $tWait  delayoff_exp
  #else
  #    echo "java -jar ./DCSim.jar 25000 0.3 $meanServiceTime 1.0 1 $k $tSetup $tWait delayoff_exp"
  #    java -jar ./DCSim.jar 20000 0.3 $meanServiceTime 1.0 1 $k $tSetup $tWait delayoff_exp
  #
else
   echo "number of parameters passed are incorrect"
   exit;
fi
