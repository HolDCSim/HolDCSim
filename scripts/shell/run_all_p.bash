#!/bin/bash

p=0.01
index=1
while [ $index -le 16 ]
do
   ./ss_all_p.bash $p
   p=$(expr $p+0.05 | bc)
   ((index++))
done
