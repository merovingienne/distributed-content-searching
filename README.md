# Distributed Content Searching

CS4262 - Distributed Systems project.

Develop a simple overlay-based solution that allows a set of nodes to share contents (e.g., music files) among each other. Consider a set of nodes connected via some overlay topology. Each of the nodes has a set of files that it is willing to share with other nodes. Suppose node `x` is interested in a file `f`. `x` issues a search query to the overlay to locate at least one node `y` containing that particular file. Once the node is identified, the file `f` can be exchanged between `x` and `y`.