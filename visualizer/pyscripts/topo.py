import networkx as nx
import matplotlib.pyplot as plt
import csv

k = 4
G = nx.Graph()
Pos = {}
switchPos = {}
for i in range(k):
    Pos.update( {"A"+str(i):(1+i*8,1), "B"+str(i):(5+i*8,1), "C"+str(i):(1+i*8,2), "D"+str(i):(5+i*8,2)})

for i in range(k):
    Pos.update({"Core"+str(i):(9+i*4, 3)})

for i in range(k):
    Pos.update({"E"+str(i):(0+i*8,0), "F"+str(i):(2+i*8,0), "G"+str(i):(4+i*8,0), "H"+str(i):(6+i*8,0)})
#Add the nodes to the graph
G.add_nodes_from(Pos.keys())


for i in range(k):
    G.add_edges_from([("A"+str(i),"C"+str(i)),("A"+str(i),"D"+str(i)),("B"+str(i),"C"+str(i)),("B"+str(i),"D"+str(i)),("A"+str(i),"E"+str(i)), ("A"+str(i),"F"+str(i)), ("B"+str(i),"H"+str(i)), ("B"+str(i),"G"+str(i))])
    G.add_edges_from([("C"+str(i),"Core0"), ("C"+str(i), "Core1"), ("D"+str(i), "Core2"), ("D"+str(i), "Core3")])

print(Pos)

server_color = []
switch_color = []

with open('log/whichFiles.txt')as csvDataFile:
    csvReader = csv.reader(csvDataFile)
    for row in csvReader:
        whichFile = row[5]

whichFile = "log/"+whichFile
line=0
with open(whichFile) as csvDataFile:
    csvReader=csv.reader(csvDataFile)
    for row in csvReader:
        if line == 1:
            for x in range(len(row)-1):
                switch_color.append(float(row[x]))
        if line == 2:
            for x in range(len(row)-1):
                server_color.append(float(row[x]))
        line = line+1

#print(switch_color)
server_color.reverse()
#print(server_color)



#Create the node-position mapping
nodeColor = ['gold', 'g', 'gold','g','r','r','g','g']

#Add the edges between the nodes
#G.add_edges_from([("A","C"),("A","D"),("B","C"),("B","D"),("C","E"),("C","F"),("D","G"),("D","H")])

#Draw and plot the graph
nodes = nx.draw_networkx_nodes(G, with_labels = True, pos=Pos , node_color=switch_color+server_color, cmap=plt.cm.RdYlGn)
nx.draw_networkx(G, with_labels = True, pos=Pos , node_color=switch_color+server_color, cmap=plt.cm.RdYlGn)

cbar = plt.colorbar(nodes)

plt.savefig("graphs/topo_graph_1.png")
plt.show()
