java -jar DCSim.jar
index=`date +%d%H%M`
d=stats_$index
rm serverPower.txt
mkdir $d
mv *statistics* $d
cp sim.config $d
