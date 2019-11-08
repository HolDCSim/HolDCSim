day="$( date +"%Y%m%d" )"
today="Sleepexp_${day}"
number=0

while test -e "logs/$today$suffix"; do
	(( ++number ))
	suffix="$( printf -- '-%02d' "$number" )"
done

cp tests/popcorns_sim_base.config tests/popcorns_sims_tmp.config
fname="$today$suffix"
mkdir -p logs/$today$suffix
singletao=0.5
sim_file="tests/popcorns_sims_tmp.config"
ts=0.5
tw=1.5

k=4
sed -i "s/^topo_params.*.*/topo_params       k=${k}/" ${sim_file}
numserv=`echo "$k * $k * $k / 4 "  | bc  `
#for singletao in 0.01 0.1 0.3 0.5 0.7 1.0; do 
jobsize=0.100
qos_limit=10;
sed -i "s/^job_QoS.*/job_QoS          ${qos_limit}/" ${sim_file}
numcores=1
flowsize=10; 
sed -i "s/^flow_size.*/flow_size          ${flowsize}/" ${sim_file}
uti=30;
arr=`echo "2 * ($jobsize * 2 + (${flowsize} * ${qos_limit}/500)) / (($numcores * $numserv) * $uti / 100)" | bc -l`

linecardsleep=linecard_sleep;
sed -i "s/^line_card_power_policy.*/line_card_power_policy        ${linecardsleep}/" ${sim_file}

serversleep=2;

algo=popcorns
sed -i "s/^network_routing_algorithm.*/network_routing_algorithm    ${algo}/" ${sim_file}
/usr/lib/jvm/java-11-openjdk-amd64/bin/java -Xmx64g -Dfile.encoding=UTF-8 -classpath /home/saisan/dev/eclipse-workspace/popcorns-final/bin:/home/saisan/dev/eclipse-workspace/popcorns-final/junit-4.10.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/junit.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/master.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/org.hamcrest.core_1.1.0.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/powercap.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/delayoff_parameter1/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/delayoff_parameter2/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/delayoff_pt1/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/delayoff_pt2/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/delayoff_pt3/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/delayoff_pt4/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/delayoff_pt5/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/delayoff_pt6/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/windowed_pt1/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/scripts/shell/windowed_pt2/DCSim.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/slave.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/ssj.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/lib/commons-cli-1.3.1.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/lib/commons-collections4-4.0.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/lib/commons-lang3-3.4.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/lib/commons-math3-3.5.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/lib/ssj.jar:/home/saisan/dev/eclipse-workspace/popcorns-final/lib/menta-queue-1.3.1.jar experiment.SimMain -path ${sim_file} -jobspserv 100 -simtime -1 -rou 0.01 -ubar ${jobsize} -speed 1.0 -sleepstate 1 -sleepunitparam $serversleep -numcores ${numcores} -numserv $numserv -ts ${ts} -tw ${tw} -predtype -1 -singletao ${singletao} -b 10 -servqueuethres 10 -avgInterArrivalTime ${arr} -globalqueue 1 -exptype lcnetwork_exp 2>&1 > logs/$today$suffix/log_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${algo}_${numcores}_${qos_limit}_${flowsize}

serversaved=`cat logs/$today$suffix/log_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${algo}_${numcores}_${qos_limit}_${flowsize} | grep "Server Energy:" | cut -d' ' -f3`
networkfull=`cat logs/$today$suffix/log_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${algo}_${numcores}_${qos_limit}_${flowsize} | grep "Network energy if all the linecards remain active but port LPI exists, energy consumption:" | cut -d' ' -f15`
networksaved=`cat logs/$today$suffix/log_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${algo}_${numcores}_${qos_limit}_${flowsize} | grep "Network energy if all linecard has sleep state but all the ports remain active, energy consumption:" | cut -d' ' -f17`
serverfull=`cat logs/$today$suffix/log_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${algo}_${numcores}_${qos_limit}_${flowsize} | grep "If servers are never turned off:" | cut -d' ' -f7 `
latencyratio=`cat logs/$today$suffix/log_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${algo}_${numcores}_${qos_limit}_${flowsize} | grep "Normalized latency:" | cut -d'=' -f2 | cut -d'}' -f1 `
latency95=`cat logs/$today$suffix/log_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${algo}_${numcores}_${qos_limit}_${flowsize} |  grep "95 percentile normalized latency:" | cut -d' ' -f5`
serveutilization=`cat logs/$today$suffix/log_${k}_${arr}_${singletao}_${jobsize}_${ts}_${tw}_${algo}_${numcores}_${qos_limit}_${flowsize} | grep "Server Utilization" | cut -d' ' -f3 | cut -d'%' -f1` 

NE_savings=`printf "(%f - %f)/%f*100\n" $networkfull $networksaved $networkfull | bc -l`;
SE_savings=`printf "(%f - %f)/%f*100\n" $serverfull $serversaved $serverfull | bc -l`;

if [ $networksaved = $networkfull ]; then
    echo -e "\n$1: FAILED Linecard no sleep"
    exit 0
else
    echo "$1: test successful"
    exit 1
fi
