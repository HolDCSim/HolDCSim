#!/bin/bash

#taos=(  0.0 1e-4 1e-3 1e-2 1.0 )
#echo "${taos[1]}*2"
#echo $t1;

#Strings=( 0.12,36.31 0.52,478.63 0.17,0.63 0.17,52.48 )
#for string in ${Strings[@]}
#do
#   echo "$string"
#done

strings=0.48,3
IFS=',' 
read -a array <<< "$strings"
echo ${array[0]}

for string in ${array[@]}
do
   echo "$string"
done
