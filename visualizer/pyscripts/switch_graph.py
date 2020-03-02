import csv
import matplotlib.pyplot as plt
import numpy as np
header = ['Active','C1S1','C3S1','C6S1','C6S3']
time = []
active = []
c1s1 = []
c3s1 = []
c6s1 = []
c6s3 = []
with open('log/whichFiles.txt') as csvDataFile:
    csvReader = csv.reader(csvDataFile)
    for row in csvReader:
        whichFile = row[3]

whichFile = "log/"+whichFile
print(whichFile)

with open(whichFile) as csvDataFile:
    csvReader = csv.reader(csvDataFile)
    for row in csvReader:
        time.append(float(row[0]))
        active.append(float(row[1]))
        c1s1.append(float(row[2]))
        c3s1.append(float(row[3]))
        c6s1.append(float(row[4]))
        c6s3.append(float(row[5]))

plt.title("Number of Switches in Each Sleep State")
p1 = plt.plot(time, active)
p2 = plt.plot(time, c1s1)
p3 = plt.plot(time, c3s1)
p4 = plt.plot(time, c6s1)
p5 = plt.plot(time, c6s3)
plt.xlabel('Time(s)')
plt.ylabel('# of Switches')
plt.legend((p1[0],p2[0],p3[0],p4[0],p5[0]),(header[0], header[1], header[2], header[3], header[4]), loc='upper right', fontsize=12, ncol=5, framealpha=0, fancybox=True)
plt.savefig("graphs/switch_graph1.png")

#ind = np.arange(len(time))
ind = np.linspace(0, len(time)+1, num=len(time))
print(len(ind) , "and " , len(active))
width = 1

idx = np.arange(0, len(time), len(time)/10)
length = len(time)*0.00005
rang = np.arange(0, length, length/10)
plt.xticks(idx, rang, rotation=65, fontsize=6)
plt.title("Number of Switches in Each Sleep State")
p1 = plt.bar(ind, active, width)
p2 = plt.bar(ind, c1s1, width, bottom=active)
p3 = plt.bar(ind, c3s1, width, bottom=[sum(x) for x in zip(active, c1s1)])
p4 = plt.bar(ind, c6s1, width, bottom=[sum(x) for x in zip(active, c1s1,c3s1)])
p5 = plt.bar(ind, c6s3, width, bottom=[sum(x) for x in zip(active, c1s1,c3s1,c6s1)])
plt.legend((p1[0],p2[0],p3[0],p4[0],p5[0]),(header[0], header[1], header[2], header[3], header[4]), loc='upper right', fontsize=12, ncol=5, framealpha=0, fancybox=True)
plt.savefig("graphs/switch_graph2.png")
