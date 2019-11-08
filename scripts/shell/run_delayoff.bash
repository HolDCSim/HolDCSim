#!/bin/bash
cd ../
iterations=$((3*2*3*9))
progress=0
for  meanServiceTime in 1 10
 do
   for tSetup in 10  50 100
   do
      for k in 10 25 50
      do
         for meanWaitTime in `seq 0 0.5 4`
         do
             ((progress++))
             echo "current progress $progress/$iterations"
             tWait=`echo "e($meanWaitTime*l(10))" | bc -l`
             if [[ "$meanServiceTime" = "1"  ]]
             then
                 echo "java -jar ./DCSim.jar 50000 0.3 $meanServiceTime 1.0 1 $k $tSetup $tWait  delayoff_exp"
#                 java -jar ./DCSim.jar 50000 0.3 $meanServiceTime 1.0 1 $k $tSetup $tWait  delayoff_exp
             else
                 echo "java -jar ./DCSim.jar 20000 0.3 $meanServiceTime 1.0 1 $k $tSetup $tWait delayoff_exp"
 #                java -jar ./DCSim.jar 20000 0.3 $meanServiceTime 1.0 1 $k $tSetup $tWait delayoff_exp

             fi
         done
      done
   done
done
