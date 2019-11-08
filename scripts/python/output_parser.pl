#!/usr/bin/perl

use warnings;
use strict;
use feature 'say';

my $filename = $ARGV[0];

open (my $fh, '<', $filename) or die "Can't open $filename\n";

while(<$fh>) {

		if(/^Servers::/){
			say "Server #\tTasks Finished\tServiceTime\tIdleTime\tC6S3\tWakeupTime\tWakeupCounts\tTimeIdle\tEnergy";
		}

		if(/^server\s:/)
		{
			if(/\s:\s(\d+):(\d+):(\d+\.\d+):(\d+\.\d+):([\d]+\.\d+):([\d]+\.\d+):(\d+):(\d+\.\d+):(\d+\.\d+)/){
				say "$1\t\t$2\t\t$3\t\t$4\t\t$5\t$6\t\t$7\t\t$8\t\t$9";
			}
		}

		if(/^Switches::/){
			say "Switch#\tActive Time\tLPI1 Time\tLPI2 Time\tLPI3 Time\tOff Time";
		}

		if(/switch/){
			my @switch = split(/:/,$_);
			say"$switch[1]\t$switch[2]\t\t$switch[3]\t\t$switch[4]\t\t$switch[5]\t\t$switch[6]"
		}
		
		if(/^Execution\sTime\s(\d+\.\d+)/i){
			say "Execution Time:\t\t\t$1";
		}
		if(/^Normalized\sLatency\s(.*)/i){
			say "Normalized Latency:\t\t$1";
		}
		if(/^EnergyDistribution/i){
			my @val = split(/:/, $_);
			say "Total Energy:\t\t\t$val[1] J";
			say "C0S0 Idle Energy:\t\t$val[2] J";
			say "C6S0 Idle Energy:\t\t$val[3] J";
			say "C6S3 Idle Energy:\t\t$val[5] J";
			say "C6S0 Wakeup Energy:\t\t$val[4] J";
			say "C6S3 Wakeup Energy:\t\t$val[6] J";
			say "Productive Energy:\t\t$val[7] J";
		}

		if(/^StateDuration/i) {
			my @dur = split(/:/, $_);
			say "Service Time:\t\t\t$dur[1] sec";
			say "Wakeup Time:\t\t\t$dur[2] sec";
			say "C0S0 Time:\t\t\t$dur[3] sec";
			say "C6S0 Time:\t\t\t$dur[4] sec";
			say "C6S3 Time:\t\t\t$dur[5] sec";
		}

		if(/^allServerStat/i) {
			my @stat = split(/:/,$_);
			say "Date:\t$stat[1]";
			say "Number of Jobs:\t$stat[2]";
			say "Rou:\t$stat[3]";
			say "uBar:\t$stat[4]";
			say "Speed:\t$stat[5]";
			say "Sleep State Mode:\t$stat[6]";
			say "Number of Servers:\t$stat[7]";
			say "Ts:\t\t$stat[8]";
			say "Tw:\t\t$stat[9]";
			say "Execution Time:\t$stat[10] seconds";
			say "Avg Service Time:\t$stat[11] seconds";
			say "Average Latency:\t$stat[12]";
			say "Normalized Latency:\t$stat[13]";
			say "Total Energy:\t$stat[14]";
		}
		
		if(/^SerialResults/) {
			my @result = split(/\s+/, $_);
			say "Date:\t\t\t\t$result[1]";
			say "Number of Jobs:\t\t\t$result[2]";
			say "Rou:\t\t\t\t$result[3]";
			say "uBar:\t\t\t\t$result[4]";
			say "Arrival Type:\t\t\t$result[5]";
			say "Speed:\t\t\t\t$result[6]";
			say "Number of Servers:\t\t$result[7]";
			say "Execution Time:\t\t\t$result[8] seconds";
			say "Average Service Time:\t\t$result[9] seconds";
			say "Average Latency:\t\t$result[10] seconds";
			say "Normalized Latency:\t\t$result[11]";
			say "Total Energy\t\t\t$result[12] J";
			say "Average Power\t\t\t$result[13] W";
			say "50th Percentile Latency:\t$result[14]";
			say "90th Percentile Latency:\t$result[15]";
			say "95th Percentile Latency:\t$result[16]";
			say "99th Percentile Latency:\t$result[17]";
			say "90th Percentile Abs Latency:\t$result[18]";
			say "95th Percentile Abs Latency:\t$result[19]";
			say "Sleep State Mode:\t\t$result[20]";
			say "Ts:\t\t\t\t$result[21]";
			say "Tw:\t\t\t\t$result[22]";
			say "Queue Predictor Type:\t\t$result[23]";


		}



}

close $fh;
