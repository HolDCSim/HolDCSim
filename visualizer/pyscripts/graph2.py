import csv
import sys
import matplotlib.pyplot as plt
import numpy as np

time  = []
server1 = []
server2 = []
server3 = []
server4 = []
server5 = []
server6 = []
server7 = []
server8 = []
which = []
header = []
arguments = len(sys.argv)-1
if (arguments > 0 ):
    for x in range(arguments):
        which.append(int(sys.argv[x+1]))
        header.append("Server " + sys.argv[x+1])
with open('log/whichFiles.txt') as csvDataFile:
    csvReader = csv.reader(csvDataFile)
    for row in csvReader:
        whichFile = row[2]

whichFile = "log/"+whichFile
print(whichFile)

with open(whichFile) as csvDataFile:
    csvReader = csv.reader(csvDataFile)
    for row in csvReader:
        time.append(float(row[0]))
        server1.append(float(row[which[0]]))
        if(arguments>1):
            server2.append(float(row[which[1]]))
        if(arguments>2):
            server3.append(float(row[which[2]]))
        if(arguments>3):
            server4.append(float(row[which[3]]))
        if(arguments>4):
            server5.append(float(row[which[4]]))
        if(arguments>5):
            server6.append(float(row[which[5]]))
        if(arguments>6):
            server7.append(float(row[which[6]]))
        if(arguments>7):
            server8.append(float(row[which[7]]))
            
fig = plt.figure(figsize=(16,9))
ax = fig.add_axes([0.1,0.1,0.75,0.75])

p1 = plt.plot(time, server1, label=header[0])
if(arguments>1):
    p2 = plt.plot(time, server2, label=header[1])
if(arguments>2):
    p3 = plt.plot(time, server3, label=header[2])
if(arguments>3):
    p4 = plt.plot(time, server4, label=header[3])
if(arguments>4):
    p5 = plt.plot(time, server5, label=header[4])
if(arguments>5):
    p5 = plt.plot(time, server6, label=header[5])
if(arguments>6):
    p5 = plt.plot(time, server7, label=header[6])
if(arguments>7):
    p5 = plt.plot(time, server8, label=header[7])

plt.title("Server Sleep State Trace")
plt.xlabel('Time(s)')
plt.ylabel('Sleep State (0-4)')
plt.yticks(np.arange(5), ('Active', 'C1S1', 'C3S1', 'C6S1', 'C6S3'))
plt.legend(bbox_to_anchor=(1.02,1), loc='upper left')
plt.savefig("graphs/server_trace_graph.png", loc='upper_right')
