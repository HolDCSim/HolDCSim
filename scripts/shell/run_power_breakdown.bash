#!/bin/bash

############################################################
#Google-like
export RESULT=~/Dropbox/2014_network_energy_efficiency/Data/stats/ss_power_performance/google_power_breakdown

#run sets of experiment


freq=0.1
p=0.1
for i in `seq 1 5`;
do
 java -jar DCSim.jar 10000 4.2e-3 $freq $i ss
 mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
 mv ./idle_distribution $RESULT/job_distributions_f${freq}s${i}p${p}
 mv ./job_completed_stats $RESULT/job_distributions_f${freq}s${i}p${p}
 mv ./queued_jobs_stats $RESULT/job_distributions_f${freq}s${i}p${p}
done

#freq=0.5
#p=0.1
#for i in `seq 1 5`;
#do
# java -jar DCSim.jar 10000 4.2e-3 $freq $i ss
# mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
#done
#
#
#freq=1.0
#p=0.1
#for i in `seq 1 5`;
#do
# java -jar DCSim.jar 10000 4.2e-3 $freq $i ss
# mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
#done

freq=0.1
p=0.5
for i in `seq 1 5`;
do
 java -jar DCSim.jar 10000 4.2e-3 $freq $i ss
 mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
 mv ./idle_distribution $RESULT/job_distributions_f${freq}s${i}p${p}
 mv ./job_completed_stats $RESULT/job_distributions_f${freq}s${i}p${p}
 mv ./queued_jobs_stats $RESULT/job_distributions_f${freq}s${i}p${p}
done


freq=0.5
p=0.5
for i in `seq 1 5`;
do
 java -jar DCSim.jar 10000 4.2e-3 $freq $i ss
 mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
 mv ./idle_distribution $RESULT/job_distributions_f${freq}s${i}p${p}
 mv ./job_completed_stats $RESULT/job_distributions_f${freq}s${i}p${p}
 mv ./queued_jobs_stats $RESULT/job_distributions_f${freq}s${i}p${p}
done


freq=1.0
p=0.5
for i in `seq 1 5`;
do
 java -jar DCSim.jar 10000 4.2e-3 $freq $i ss
 mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
done


freq=0.1
p=0.8
for i in `seq 1 5`;
do
 java -jar DCSim.jar 80000 4.2e-3 $freq $i ss
 mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
done


freq=0.5
p=0.8
for i in `seq 1 5`;
do
 java -jar DCSim.jar 80000 4.2e-3 $freq $i ss
 mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
done


freq=1.0
p=0.8
for i in `seq 1 5`;
do
 java -jar DCSim.jar 80000 4.2e-3 $freq $i ss
 mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
done

############################################################
#DNS-like

export RESULT=~/Dropbox/2014_network_energy_efficiency/Data/stats/ss_power_performance/dns_power_breakdown

#run sets of experiment


freq=0.1
p=0.1
for i in `seq 1 5`;
do
 java -jar DCSim.jar 10000 192e-3 $freq $i ss
 mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
done

freq=0.5
p=0.1
for i in `seq 1 5`;
do
 java -jar DCSim.jar 10000 192e-3 $freq $i ss
 mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
done


freq=1.0
p=0.1
for i in `seq 1 5`;
do
 java -jar DCSim.jar 10000 192e-3 $freq $i ss
 mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
done

freq=0.1
p=0.5
for i in `seq 1 5`;
do
 java -jar DCSim.jar 10000 192e-3 $freq $i ss
 mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
done


freq=0.5
p=0.5
for i in `seq 1 5`;
do
 java -jar DCSim.jar 10000 192e-3 $freq $i ss
 mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
done


freq=1.0
p=0.5
for i in `seq 1 5`;
do
 java -jar DCSim.jar 10000 192e-3 $freq $i ss
 mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
done


freq=0.1
p=0.8
for i in `seq 1 5`;
do
 java -jar DCSim.jar 80000 192e-3 $freq $i ss
 mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
done


freq=0.5
p=0.8
for i in `seq 1 5`;
do
 java -jar DCSim.jar 80000 192e-3 $freq $i ss
 mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
done


freq=1.0
p=0.8
for i in `seq 1 5`;
do
 java -jar DCSim.jar 80000 192e-3 $freq $i ss
 mkdir $RESULT/job_distributions_f${freq}s${i}p${p}
done
