import networkx as nx
import matplotlib
import matplotlib.pyplot as plt
import csv
from matplotlib import animation
import numpy as np

k = 4
G = nx.Graph()
Pos = {}
switchPos = {}
# Adding nodes for Edge and Aggregate Switches
for i in range(k):
    Pos.update( {
        "A"+str(i):(1+i*8,1),
        "B"+str(i):(5+i*8,1), 
        "C"+str(i):(1+i*8,2), 
        "D"+str(i):(5+i*8,2)})

# Adding nodes for Core Swithces
for i in range(k):
    Pos.update({"Core"+str(i):(9+i*4, 3)})

# Adding nodes for servers
for i in range(k):
    Pos.update({
        "E"+str(i):(0+i*8,0), 
        "F"+str(i):(2+i*8,0), 
        "G"+str(i):(4+i*8,0), 
        "H"+str(i):(6+i*8,0)})

# Add the nodes to the graph
G.add_nodes_from(Pos.keys())


# Adding edges between the core switches and aggregate switches, the aggregate and edge switches and between the aggregate switches and appropriate servers
for i in range(k):
    G.add_edges_from([("A"+str(i),"C"+str(i)),
        ("A"+str(i),"D"+str(i)),
        ("B"+str(i),"C"+str(i)),
        ("B"+str(i),"D"+str(i)),
        ("A"+str(i),"E"+str(i)), 
        ("A"+str(i),"F"+str(i)), 
        ("B"+str(i),"H"+str(i)), 
        ("B"+str(i),"G"+str(i))
        ])
    G.add_edges_from([("C"+str(i),"Core0"),
        ("C"+str(i), "Core1"),
        ("D"+str(i), "Core2"), 
        ("D"+str(i), "Core3")
        ])

print(Pos)

server_color = []
switch_color = []


# Build plot
fig, ax = plt.subplots(figsize=(6,4))

# Opens the whichFiles.txt to find the log file with the color arrays
with open('log/whichFiles.txt')as csvDataFile:
    csvReader = csv.reader(csvDataFile)
    for row in csvReader:
        whichFile = row[5]

# Parses the log file and loads the color arrays into the switch_color and server_color lists
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

server_color.reverse()
node_color = switch_color+server_color

#Draw and plot the graph
nodes = nx.draw_networkx_nodes(G, with_labels = True, pos=Pos , node_color=node_color, cmap=plt.cm.RdYlGn)
edges = nx.draw_networkx(G, with_labels = True, pos=Pos , node_color=switch_color+server_color, cmap=plt.cm.RdYlGn)

cbar = plt.colorbar(nodes)

plt.savefig("graphs/topo_graph_1.png")



def simple_update(num, n, layout, G, ax):
    ax.clear()

    random_colors = np.random.randint(low=0, high=4, size=n)
    nodes = nx.draw_networkx_nodes(G, with_labels = True, pos=Pos , node_color=random_colors, cmap=plt.cm.RdYlGn)
    edges = nx.draw_networkx(G, with_labels = True, pos=Pos , node_color=random_colors, cmap=plt.cm.RdYlGn)
    # Set the title
    ax.set_title("Frame {}".format(num))


def simple_animation():


    # Create a graph and layout
    n = len(node_color) # Number of nodes

    ani = animation.FuncAnimation(fig, simple_update, frames=10, fargs=(len(Pos), Pos, G, ax))
    ani.save('animation_1.gif', writer='imagemagick')

    plt.show()

simple_animation()

































