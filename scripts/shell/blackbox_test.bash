#!/bin/bash
#pro-test
#
#if ! java  -jar DCSim.jar 100  0.3 4.2e-3 1.0 1 4  2 5  ee_exp
#then
#   echo "progrem exits with unhandled exceptions"
#fi
#

# first test MultiServer Experiment

#test num of server from 1 ~ 100 
# rho from 0.1 0.3 0.5 0.8 
#number of jobs 10000

buglog="buglog.txt";
jarName="DCSim.latest"
if [ -e "$buglog" ]
then
   rm $buglog
fi

if [ $1 ]
then
   jarName=$1
fi
numOfJobs=10000
totalIteration=$((15*4*2));
currentIteration=0;
bugsInMS=0;
for u in 4.2e-3 194e-3
do
   for numOfServers in $(seq 1 15)
   do
      for rho in 0.1 0.3 0.5 0.8
      do
            ((++currentIteration))
            
            if ! java  -jar $jarName $numOfJobs  $rho $u 1.0 1 $numOfServers  ms_exp
            then
               ((++bugsInMS))
               #insert newline to string
               echo -e "$numOfJobs $rho $u 1.0 1 $numOfServers \n" >> buglog
            fi
            #bc should read from file
            progress=`echo "scale=5; $currentIteration/$totalIteration*100" | bc`;
            echo "current progress is $progress/100"
         done
   done
done

echo -e "$bugsInMS failures in $totalIteration experiments for MS experiment \n"  >> buglog

# second test EnergyAware Experiment
numOfJobs=10000
totalIteration=$((15*4*2));
currentIteration=0;
buglog="buglog.txt";
bugsInEE=0;
for u in 4.2e-3 194e-3
do
   for numOfServers in $(seq 1 15)
   do
      for rho in 0.1 0.3 0.5 0.8
      do
            ((++currentIteration))
            
            if ! java  -jar $jarName $numOfJobs  $rho $u 1.0 1 $numOfServers 2 5  ee_exp
            then
               ((++bugsInEE))
               #insert newline to string
               echo -e "$numOfJobs $rho $u 1.0 1 $numOfServers 2 5 \n" >> buglog
            fi
            #bc should read from file
            progress=`echo "scale=5; $currentIteration/$totalIteration*100" | bc`;
            echo "current progress is $progress/100"
         done
   done
done

echo -e "$bugsInEE failures in $totalIteration experiments for EE experiment"  >> buglog