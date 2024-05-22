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
    public Set<Node> subgraphNodes1;
    public Set<Node> subgraphNodes2;

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

    public Leafset(String creator, boolean isArtificial, Set<Node> leaves, Set<Node> subgraphNodes1, Set<Node> subgraphNodes2, MAST pricingProblem) {
        super(pricingProblem, isArtificial, creator);
        this.leaves=leaves;
        this.subgraphNodes1 = subgraphNodes1;
        this.subgraphNodes2 = subgraphNodes2;
    }

    public boolean contains(Node node){
        return leaves.contains(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Leafset other = (Leafset) o;
        return this.leaves.containsAll(other.leaves) && other.leaves.containsAll(this.leaves);
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return leaves.toString();
    }

    public boolean has_internal(int internalNode, Graph<Node, DefaultEdge> tree, int tree_int) {
        if(tree_int==1){
            if(subgraphNodes1==null){
                subgraphNodes1 = get_internal_nodes(tree);
            }
            for(Node possibleNode: subgraphNodes1){
                if(possibleNode.id == internalNode){
                    return true;
                }
            }
        }else{
            if(subgraphNodes2==null){
                subgraphNodes2 = get_internal_nodes(tree);
            }
            for(Node possibleNode: subgraphNodes2){
                if(possibleNode.id == internalNode){
                    return true;
                }
            }
        }

        return false;
    }

    public boolean has_internal(int internalNode, int tree) {
        if(tree==1){
            for(Node n:subgraphNodes1){
                if(n.id==internalNode){
                    return true;
                }
            }
            return false;
        }else{
            for(Node n:subgraphNodes2){
                if(n.id==internalNode){
                    return true;
                }
            }
            return false;
        }
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
