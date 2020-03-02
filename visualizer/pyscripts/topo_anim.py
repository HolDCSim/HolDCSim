import networkx as nx
import matplotlib
import matplotlib.pyplot as plt
import csv
from matplotlib import animation
import numpy as np

def grab_colors(line_to_use):
    temp = []
    line = 0
    global sim_time
    with open(whichFile) as csvDataFile:
        csvReader=csv.reader(csvDataFile)
        for row in csvReader:
            if(line == line_to_use):
                sim_time = round(float(row[0]),2)
                for x in range(len(row)-2):
                    temp.append(float(row[x+1]))
            line = line+1
    # temp.reverse()
    return temp

k = 4
sim_time = 0
G = nx.Graph()
Pos = {}
switchPos = {}
node_color = []
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
edge_colors = np.linspace(0, 47, 48)
print(Pos)



# Build plot
fig, ax = plt.subplots(figsize=(6,4))

# Opens the whichFiles.txt to find the log file with the color arrays
with open('log/whichFiles.txt')as csvDataFile:
    csvReader = csv.reader(csvDataFile)
    for row in csvReader:
        whichFile = row[6]

# Parses the log file and loads the color arrays into the switch_color and server_color lists

whichFile = "log/"+whichFile
line=0

node_color = grab_colors(0)
print(node_color)

def simple_update(num, n, layout, G, ax):
    ax.clear()
    node_color = grab_colors(num)
    nodes = nx.draw_networkx_nodes(G, with_labels = True, 
            pos=Pos , node_color=node_color, cmap=plt.cm.RdYlGn)
    #edges = nx.draw_networkx_edges(G, with_labels = True, pos=Pos , 
    #        edge_color=edge_colors, 
    #        cmap=plt.cm.RdYlGn, 
    #        edge_cmap=plt.cm.RdYlGn,
    #        width=6, alpha=0.75)
    nx.draw_networkx(G, with_labels = True, pos=Pos , node_color=node_color, cmap=plt.cm.RdYlGn)

    # Set the title
    ax.set_title("Simulation Time {}".format(sim_time))


def simple_animation():


    # Create a graph and layout
    n = len(node_color) # Number of nodes
    print(sim_time , ":" , node_color)
    ani = animation.FuncAnimation(fig, simple_update, frames=100, fargs=(len(Pos), Pos, G, ax))
    ani.save('graphs/animation_1.gif', writer='imagemagick')

    plt.show()

    # print(line_to_use,":", sim_time , ":" , node_color)


simple_animation()
