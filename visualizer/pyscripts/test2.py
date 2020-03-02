import numpy as np
import networkx as nx
import matplotlib.pyplot as plt
from matplotlib import animation


G = nx.Graph()

pos = {
        "1":(0,0),
        "2":(1,0),
        "3":(0,1),
        "4":(1,1)
        }

edges = {
        ("1","2"),
        ("2","4"),
        ("3","4"),
        ("1","3")
        }

fig, ax = plt.subplots(figsize=(6,4))

G.add_nodes_from(pos.keys())
G.add_edges_from(edges)

nc = np.random.randint(low=0, high=4, size=4)
nodes = nx.draw_networkx_nodes(G, with_labels=True, pos=pos, node_color=nc, cmap=plt.cm.RdYlGn)
nx.draw_networkx(G, with_lables=True, pos=pos, node_color=nc, cmap=plt.cm.RdYlGn)
cbar = plt.colorbar(nodes)

def simple_update(num, n, pos, G, ax):
    ax.clear()

    # Draw the graph with random node colors
    random_colors = np.random.randint(low=0, high=4, size=n)
    nodes = nx.draw_networkx_nodes(G, with_labels=True, pos=pos, node_color=nc, cmap=plt.cm.RdYlGn)
    nx.draw(G, with_labels=True,
            pos=pos, node_color=random_colors, ax=ax, cmap=plt.cm.RdYlGn)
    

    # Set the title
    ax.set_title("Frame {}".format(num))


def simple_animation():


    layout = nx.spring_layout(G)

    ani = animation.FuncAnimation(fig, simple_update, frames=10, fargs=(4, pos, G, ax))
    ani.save('animation_1.gif', writer='imagemagick')

    plt.show()

simple_animation()
