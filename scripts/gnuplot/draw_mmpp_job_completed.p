#set title "benchmark without ripret" font "Times-Roman Bold,22" 
set terminal postscript 
set output '| ps2pdf - ./mmpp_job_completed_p0.1.pdf'
#set grid
set size ratio 0.5
set autoscale y
set xlabel "time" font "Times-Roman,18"
#set ylabel "number of job generated" font "Times-Roman,12" offset 2 
set yrange [0:10000]
set ylabel "number of job finished" font "Times-Roman,18" offset 2 
#set xtics 100
set ytics 1000
set key off
#set style line lw 2
plot "mmpp_job_completed_stats_p0.1" using 1:0 with lines lw 3 lc 2
#     "mmpp_arrivals_stats_p0.1" using 1:0 with lines lw 3 lc 3 
#set format x "%.0s*10^%T"
#plot "mmpp_arrivals_stats" u ($2) with lines lw 3 
set output

#plot usage

#plot "my.dat" every A:B:C:D:E:F
#
#A: line increment
#B: data block increment
#C: The first line
#D: The first data block
#E: The last line
#F: The last data block
#
#plot the data starting from line 10:
#
#plot "my.dat" every ::10
#To plot the data from line 10 to line 100:
#
#plot "my.dat" every ::10::100
#To plot data starting from the first line till line 100:
#
#plot "my.dat" every ::::100
#An alternative way to plot specific rows of data file is to use input redirection. That is, use any external program to process raw data file, and redirect the output to gnuplot. Assuming that sec program is installed on your system, you can simply do:
#
#plot "<(sed -n '10,100p' my.dat)"
