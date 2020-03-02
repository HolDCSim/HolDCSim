import networkx as nx
import matplotlib.pyplot as plt
import csv

k = 8
m = k/2
core = m ** 2
G = nx.Graph()
Pos = {}
switchPos = {}
print(range(k))
for i in range(k):
    if(i==0):
        Pos.update( {
            str(i):(5, 2),
            str((i+1)):(13,2),
            str((i+2)):(21,2),
            str((i+3)):(29,2),
            str((i+4)):(5,4),
            str((i+5)):(13,4),
            str((i+6)):(21,4),
            str((i+7)):(29,4)

            })
    else:
        Pos.update( {
            str(8*i):(5+(i*34), 2),
            str(1+i*8):(13+i*34,2),
            str((2)+i*8):(21+i*34,2),
            str((3)+i*8):(29+i*34,2),
            str((4)+i*8):(5+i*34,4),
            str((5)+i*8):(13+i*34,4),
            str((6)+8*i):(21+i*34,4),
            str((7)+i*8):(29+i*34,4)

            })


for i in range(int(core)):
    Pos.update({
        str(64+i):(76+8*i, 5)
        })

for i in range(k):
    for j in range(int(m)):
        Pos.update({
            str(80+16*i+4*j):(2+8*j+34*i, 1),
            str(81+16*i+4*j):(4+8*j+34*i, 1),
            str(82+16*i+4*j):(6+8*j+34*i, 1),
            str(83+16*i+4*j):(8+8*j+34*i, 1),
            })
#Add the nodes to the graph
G.add_nodes_from(Pos.keys())

for i in range(k):
    for j in range(int(m)):
        G.add_edges_from([
            (str(8*i+j),str((4)+i*8)),       
            (str(8*i+j),str((5)+i*8)),      
            (str(8*i+j),str((6)+i*8)),       
            (str(8*i+j),str((7)+i*8))       

            ])
for j in range(k):
    for i in range(int(m)):
        G.add_edges_from([
            (str(8*j+i+4),str(64+i*4)),
            (str(8*j+i+4),str(65+i*4)),
            (str(8*j+i+4),str(66+i*4)), 
            (str(8*j+i+4),str(67+i*4)) 

            ])

for i in range(k):
    for j in range(int(m)):
        G.add_edges_from([
            (str(i*8+j),(str(80+i*16+4*j))),       
            (str(i*8+j),(str(81+i*16+4*j))),       
            (str(i*8+j),(str(82+i*16+4*j))),       
            (str(i*8+j),(str(83+i*16+4*j)))       
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
print(server_color)
print(len(switch_color+server_color))



#Add the edges between the nodes
#G.add_edges_from([("A","C"),("A","D"),("B","C"),("B","D"),("C","E"),("C","F"),("D","G"),("D","H")])

#Draw and plot the graph
nodes = nx.draw_networkx_nodes(G, with_labels = True, pos=Pos, node_color=switch_color+server_color, cmap = plt.cm.RdYlGn, node_size=80 )
nx.draw_networkx(G, with_labels = True, pos=Pos, node_size=80, node_color=switch_color+server_color, cmap = plt.cm.RdYlGn, font_size=8)
#cbar = plt.colorbar(nodes)
cbar = plt.colorbar(nodes)
plt.savefig("graphs/topo_graph_1.png")
plt.show()
