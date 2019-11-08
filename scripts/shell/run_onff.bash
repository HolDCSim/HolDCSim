#!/bin/bash
cd ../
progress=0

for setupT in 20 40 60 80 100
do
   for meanServiceT in 1 3 5 7 9
   do
      ((++progress))
      echo "the current progress is $progress/25"
      java -jar ./DCSim.jar 50000 0.3 $meanServiceT 1.0 1  10 $setupT  onoff_exp
   done
done
