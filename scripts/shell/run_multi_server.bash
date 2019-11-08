#!/bin/bash


rous=( 0.1 0.3 0.5 0.8)
#freqs=( 1  0.95  0.9   0.85  0.8   0.75  0.7   0.65  0.6   0.55  0.5   0.45  0.4   0.35  0.3   0.25  0.2   0.15  0.1   0.05 )
freqs=( 1   0.9   0.7   0.5  0.3 )
for u in  4.2e-3 194e-3
do
# for p in 0.1 0.3 0.5 0.8
  for p in 0.3 

  do
  #for freq in ${freqs[@]}
# for i in `seq 1 5`;
    for i in 1;
    do
  #    for i in `seq 1 5`;
       for freq in 1.0 0.9 0.7 0.5 0.3 
       do
  #      if [ "$p" = "0.8" ]
  #      then
  #          jobs=80000
  #      else
  #         jobs=10000
  #      fi
         echo "$jobs $p $i $freq" 
         echo "java -jar DCSim.jar rou:  $p  u :$u freq: $freq sleepState: $i"
  java -jar DCSim.jar 20000 $p $u  $freq $i multi_server
  #      mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
  #      mv ./idle_distribution $RESULT/job_distributions_f${freq}s${i}p${p}
  #      mv ./job_completed_stats $RESULT/job_distributions_f${freq}s${i}p${p}
  #      mv ./queued_jobs_stats $RESULT/job_distributions_f${freq}s${i}p${p}
  
       done
     done
  done
done
