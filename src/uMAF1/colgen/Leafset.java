package uMAF1.colgen;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.BFSShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jorlib.frameworks.columnGeneration.colgenMain.AbstractColumn;
import uMAF1.misc.Node;
import uMAF1.model.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Leafset extends AbstractColumn<MAF, MAST> {

    public Set<Node> leaves;

    /**
     * Constructs a new column
     *
     * @param pricingProblem Pricing problem to which this column belongs
     * @param isArtificial             Is this an artificial column?
     * @param creator                  Who/What created this column?
     */

    public Leafset(String creator, boolean isArtificial, Set<Node> leaves, MAST pricingProblem) {
        super(pricingProblem, isArtificial, creator);
        this.leaves=leaves;
    }
    public boolean contains(Node node){
        return leaves.contains(node);
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return leaves.toString();
    }

    public boolean has_internal(int internalNode, Graph<Node, DefaultEdge> tree) {
        Set<Node> subgraphNodes1 = get_internal_nodes(tree);
        for(Node possibleNode: subgraphNodes1){
            if(possibleNode.id == internalNode){
                return true;
            }
        }
        return false;
    }

    public Set<Node> get_internal_nodes(Graph<Node, DefaultEdge> tree) {
        Set<Node> internalNodes = new HashSet<>();
        List<Node> leafNodesList = new ArrayList<>(leaves);
        Node node1 = leafNodesList.getFirst();
        for (Node node : leaves) {
            if(!node.equals(node1)) {
                BFSShortestPath<Node, DefaultEdge> BFSShortestPath = new BFSShortestPath<Node, DefaultEdge>(tree);
                GraphPath<Node, DefaultEdge> shortestPath = BFSShortestPath.getPath(node, node1);
                List<Node> pathNodes = shortestPath.getVertexList();
                for (Node pathNode : pathNodes) {
                    if (!internalNodes.contains(pathNode)) {
                        internalNodes.add(pathNode);
                    }
                }
            }

        }
        return internalNodes;
    }

}
