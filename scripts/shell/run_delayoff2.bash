#!/bin/bash
cd ../
iterations=$((2*1*3*4))
progress=0
for  meanServiceTime in 4.2e-3 194e-3
 do
   for tSetup in 1
   do
      for k in 10 25 50
      do
           for rho in 0.1 0.3 0.5 0.7
           do
             ((progress++))
                 echo "current progress $progress/$iterations"
                 echo "java -jar ./DCSim.jar 50000 $rho $meanServiceTime 1.0 1 $k $tSetup $tWait  delayoff_exp"
                 java -jar ./DCSim.jar 50000 $rho  $meanServiceTime 1.0 1 $k $tSetup 10  delayoff_exp
          done
      done
   done
done
