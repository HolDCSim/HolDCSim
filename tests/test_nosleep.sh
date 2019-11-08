day="$( date +"%Y%m%d" )"
today="Sleepexp_${day}"
number=0

while test -e "logs/$today$suffix"; do
	(( ++number ))
	suffix="$( printf -- '-%02d' "$number" )"
done

fname="$today$suffix"
mkdir -p logs/$today$suffix
singletao=0.5
sim_file="popcorns_sims2.config"
ts=0.5
tw=1.5

printf 'Will use "%s" as filename\n' "$fname"
echo "algorithm utilization k arr jobsize flow_size numserv numcores qos_limit serverfull serversaved networkfull networksaved Normalizedlatency95 server-occupancy NE_savings SE_savings" > logs/${today}${suffix}/${k}_combined.csv 

for k in 12; do
	sed -i "s/^topo_params.*.*/topo_params       k=${k}/" ${sim_file}
	#	while [ $arr -ge 0.000001 ] ; do; 
	#	for arr in 0.1 0.12 0.0100 0.0091 0.0075 0.0040 0.0020 0.0001  ; do
	numserv=`echo "$k * $k * $k / 4 "  | bc  `
	#for singletao in 0.01 0.1 0.3 0.5 0.7 1.0; do 
	for jobsize in 0.100; do 
		for qos_limit in 10;  do
			sed -i "s/^job_QoS.*/job_QoS          ${qos_limit}/" ${sim_file}
			#		for ts in 0.13 ; do 
			#			tw1=`echo "$ts+0.5" | bc -l`
			#			tw2=`echo "$ts+1.5" | bc -l`
			#			for tw in $tw2; do 
			for numcores in 1;  do 
				oldutil=0
				for flowsize in 10; do 
					sed -i "s/^flow_size.*/flow_size          ${flowsize}/" ${sim_file}
					for uti in 30;do
						arr=`echo "2 * ($jobsize * 2 + (${flowsize} * ${qos_limit}/500)) / (($numcores * $numserv) * $uti / 100)" | bc -l`
						echo "\nArrate $arr \n"

						for linecardsleep in no_management; do
							sed -i "s/^line_card_power_policy.*/line_card_power_policy        ${linecardsleep}/" ${sim_file}

							for serversleep in 1 2; do

								for algo in popcorns; do
									#						for algo in wasp ; do
									sed -i "s/^network_routing_algorithm.*/network_routing_algorithm    ${algo}/" ${sim_file}
									#rm log/whichFiles.txt
									rm log/*
									set -x
									/usr/lib/jvm/java-11-openjdk-amd64/bin/java -Xmx64g -Dfile.encoding=UTF-8 -classpath /home/saisan/dev/eclipse-workspace/popcorns-final/bin:/home/saisan/dev/eclipse-workspace/popcorns-final/junit-4.10.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/junit.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/master.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/org.hamcrest.core_1.1.0.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/powercap.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/delayoff_parameter1/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/delayoff_parameter2/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/delayoff_pt1/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/delayoff_pt2/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/delayoff_pt3/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/delayoff_pt4/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/delayoff_pt5/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/delayoff_pt6/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/windowed_pt1/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/windowed_pt2/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/slave.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/ssj.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/lib/commons-cli-1.3.1.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/lib/commons-collections4-4.0.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/lib/commons-lang3-3.4.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/lib/commons-math3-3.5.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/lib/ssj.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/lib/menta-queue-1.3.1.jar experiment.SimMain -path ${sim_file} -jobspserv 100 -simtime -1 -rou 0.01 -ubar ${jobsize} -speed 1.0 -sleepstate 1 -sleepunitparam $serversleep -numcores ${numcores} -numserv $numserv -ts ${ts} -tw ${tw} -predtype -1 -singletao ${singletao} -b 10 -servqueuethres 10 -avgInterArrivalTime ${arr} -globalqueue 1 -exptype lcnetwork_exp 2>&1 | tee logs/$today$suffix/log_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${algo}_${numcores}_${qos_limit}_${flowsize}

									set +x
									serversaved=`cat logs/$today$suffix/log_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${algo}_${numcores}_${qos_limit}_${flowsize} | grep "Server Energy:" | cut -d' ' -f3`
									networkfull=`cat logs/$today$suffix/log_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${algo}_${numcores}_${qos_limit}_${flowsize} | grep "Network energy if all the linecards remain active but port LPI exists, energy consumption:" | cut -d' ' -f15`
									networksaved=`cat logs/$today$suffix/log_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${algo}_${numcores}_${qos_limit}_${flowsize} | grep "Network energy if all linecard has sleep state but all the ports remain active, energy consumption:" | cut -d' ' -f17`
									serverfull=`cat logs/$today$suffix/log_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${algo}_${numcores}_${qos_limit}_${flowsize} | grep "If servers are never turned off:" | cut -d' ' -f7 `
									latencyratio=`cat logs/$today$suffix/log_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${algo}_${numcores}_${qos_limit}_${flowsize} | grep "Normalized latency:" | cut -d'=' -f2 | cut -d'}' -f1 `
									latency95=`cat logs/$today$suffix/log_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${algo}_${numcores}_${qos_limit}_${flowsize} |  grep "95 percentile normalized latency:" | cut -d' ' -f5`
									serveutilization=`cat logs/$today$suffix/log_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${algo}_${numcores}_${qos_limit}_${flowsize} | grep "Server Utilization" | cut -d' ' -f3 | cut -d'%' -f1` 

									NE_savings=`printf "(%f - %f)/%f*100\n" $networkfull $networksaved $networkfull | bc -l`;
									SE_savings=`printf "(%f - %f)/%f*100\n" $serverfull $serversaved $serverfull | bc -l`;


									echo $algo $uti $k $arr $jobsize $flowsize $numserv $numcores $qos_limit $serverfull $serversaved $networkfull $networksaved $latency95 $serveutilization $NE_savings $SE_savings $linecardsleep $serversleep >> logs/${today}${suffix}/${k}_combined.csv 
									#							OUT=$(cut -f 1 -d , log/whichFiles.txt)
									#							PWD=$(pwd)
									#							./scripts/python/output_parser.pl "$PWD/log/$OUT"
									#						(python3 scripts/python/pyscripts/graph.py)
									#						(python3 scripts/python/pyscripts/graph2.py 1 2 5 8)
									#						(python3 scripts/python/pyscripts/switch_graph.py)
									#						(python3 scripts/python/pyscripts/switch_graph2.py 1 2 5 8 )
									#							(python3 scripts/python/pyscripts/topo.py)
									#							cp graphs/topo_graph_1.png logs/$today$suffix/heatmap_${algo}_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${numcores}_${qos_limit}_${flowsize}.png
									#							convert  logs/$today$suffix/heatmap_${algo}_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${numcores}_${qos_limit}_${flowsize}.png -set label ${algo} logs/$today$suffix/heatmap_${algo}_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${numcores}_${qos_limit}_${flowsize}.png
								done
								#						montage -geometry +0+0 logs/$today$suffix/heatmap*.png logs/$today$suffix/montage_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${numcores}_${qos_limit}_${flowsize}.png
								rm logs/$today$suffix/heatmap*.png
								#						display logs/$today$suffix/montage_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${numcores}_${qos_limit}_${flowsize}.png
							done
						done
					done
				done
			done
		done
	done
done
