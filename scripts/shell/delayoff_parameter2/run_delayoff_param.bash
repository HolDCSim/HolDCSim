#!/bin/bash
../monitor.bash
iterations=$((1*1*1*21))
progress=0
for  meanServiceTime in  194e-3
 do
   for tSetup in 1
   do
#      for k in 1 3 5
      for k in 10
      do
         for meanWaitTime in `seq 0 0.2 4`
         do
             ((progress++))
             echo "current progress $progress/$iterations"
#             tWait=`echo "e($meanWaitTime*l(10))" | bc -l`
#             if [[ "$k" = "10"  ]]
#             then
#                 echo "java -jar ./DCSim.jar 50000 0.3 $meanServiceTime 1.0 1 $k $tSetup $tWait  delayoff_exp"
#                 java -jar ./DCSim.jar 50000 0.3 $meanServiceTime 1.0 1 $k $tSetup $tWait  delayoff_exp
#             else
#                 echo "java -jar ./DCSim.jar 25000 0.3 $meanServiceTime 1.0 1 $k $tSetup $tWait delayoff_exp"
#                 java -jar ./DCSim.jar 20000 0.3 $meanServiceTime 1.0 1 $k $tSetup $tWait delayoff_exp
#
#             fi
             ../delayoff_main.bash $meanServiceTime  $tSetup $k $meanWaitTime
         done
      done
   done
done
