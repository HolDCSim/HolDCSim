import networkx as nx
import matplotlib.pyplot as plt
import csv

k = 8
m = k/2
G = nx.Graph()
Pos = {}
switchPos = {}
print(range(k))
for i in range(k):
    if(i==0):
        Pos.update( {
            str(i):(5, 4),
            str((i+1)):(13,4),
            str((i+2)):(21,4),
            str((i+3)):(29,4),
            str((i+4)):(5,1),
            str((i+5)):(13,1),
            str((i+6)):(21,1),
            str((i+7)):(29,1)

            })
    else:
        Pos.update( {
            str(i+8*i):(5+(i*34), 2),
            str((i+1)+i*8):(13+i*34,2),
            str((i+2)+i*8):(21+i*34,2),
            str((i+3)+i*8):(29+i*34,2),
            str((i+4)+i*8):(5+i*34,1),
            str((i+5)+i*8):(13+i*34,1),
            str((i+6)+8*i):(21+i*34,1),
            str((i+7)+i*8):(29+i*34,1)

            })


#Add the nodes to the graph
G.add_nodes_from(Pos.keys())

for i in range(k):
    for j in range(int(m)):
        G.add_edges_from([
            (str(i+8*i+4*j),str((i+4)+i*8)),       
            (str(i+8*i+4*j),str((i+5)+i*8)),      
            (str(i+8*i+4*j),str((i+6)+i*8)),       
            (str(i+8*i+4*j),str((i+7)+i*8))       

            ])


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

print(switch_color)
server_color.reverse()
print(server_color)



#Create the node-position mapping
nodeColor = ['gold', 'g', 'gold','g','r','r','g','g']

#Add the edges between the nodes
#G.add_edges_from([("A","C"),("A","D"),("B","C"),("B","D"),("C","E"),("C","F"),("D","G"),("D","H")])

#Draw and plot the graph
nodes = nx.draw_networkx_nodes(G, with_labels = True, pos=Pos )
nx.draw_networkx(G, with_labels = True, pos=Pos)
#cbar = plt.colorbar(nodes)

plt.savefig("graphs/topo_graph_1.png")
plt.show()
