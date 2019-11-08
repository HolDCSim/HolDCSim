#!/bin/bash

############################################################
#Google-like
export RESULT=~/Dropbox/2014_network_energy_efficiency/Data/stats/ss_power_performance/google_power_breakdown

#run sets of experiment

rous=( 0.1 0.5 0.8 )
freqs=( 0.1 0.5 1.0 )
for p in ${rous[@]}
do
  for freq in ${freqs[@]}
  do
     for i in `seq 1 5`;
     do
      if [ "$p" = "0.8" ]
      then
         jobs=80000
      else
         jobs=10000
      fi
#       echo "$jobs $p $freq $i"
      echo "java -jar DCSim.jar $jobs $p 4.2e-3 $freq $i ss"
      java -jar DCSim.jar $jobs $p 4.2e-3 $freq $i ss
      mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
      mv ./idle_distribution $RESULT/job_distributions_f${freq}s${i}p${p}
      mv ./job_completed_stats $RESULT/job_distributions_f${freq}s${i}p${p}
      mv ./queued_jobs_stats $RESULT/job_distributions_f${freq}s${i}p${p}

     done
   done
done


############################################################
#DNS-like
export RESULT=~/Dropbox/2014_network_energy_efficiency/Data/stats/ss_power_performance/dns_power_breakdown
for p in ${rous[@]}
do
  for freq in ${freqs[@]}
  do
     for i in `seq 1 5`;
     do
      if [ "$p" = "0.8" ]
      then
         jobs=80000
      else
         jobs=10000
      fi
#       echo "$jobs $p $freq $i"
      echo "java -jar DCSim.jar $jobs $p 194e-3 $freq $i ss"
      java -jar DCSim.jar $jobs $p 194e-3 $freq $i ss
      mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
      mv ./idle_distribution $RESULT/job_distributions_f${freq}s${i}p${p}
      mv ./job_completed_stats $RESULT/job_distributions_f${freq}s${i}p${p}
      mv ./queued_jobs_stats $RESULT/job_distributions_f${freq}s${i}p${p}

     done
   done
done

