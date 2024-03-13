package uMAF;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.BFSShortestPath;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;

public class LeafSet {
    public Set<Node> subgraphNodes1;
    public Set<Node> subgraphNodes2;
    public Set<Node> leaves;
    public Graph<Node, DefaultEdge> subgraph1;
    public Graph<Node, DefaultEdge> subgraph2;
    public Graph<Node, DefaultEdge> full_tree1;
    public Graph<Node, DefaultEdge> full_tree2;
    public boolean valid = false;
    public boolean checked_valid = false;
    public String name;
    public int ILPval;

    public LeafSet(Graph<Node, DefaultEdge> tree1, Graph<Node, DefaultEdge> tree2, Set<Node> newLeaves) {
        this.full_tree1 = tree1;
        this.full_tree2 = tree2;
        this.leaves = newLeaves;
    }

    public Set<Node> get_internal(int tree){
        if(tree==1) {
            if (subgraphNodes1 == null) {
                subgraphNodes1 = get_internal_nodes(full_tree1, leaves);
            }
            return subgraphNodes1;
        }else if(tree==2) {
            if (subgraphNodes2 == null) {
                subgraphNodes2 = get_internal_nodes(full_tree2, leaves);
            }
            return subgraphNodes2;
        }
        return null;
    }

    public boolean has_internal(int internal, int tree){
        if(tree==1){
            for(Node possibleNode: subgraphNodes1){
                if(possibleNode.id == internal){
                    return true;
                }
            }
        }else{
            for(Node possibleNode: subgraphNodes2){
                if(possibleNode.id == internal){
                    return true;
                }
            }
        }
        return false;
    }

    public Graph<Node, DefaultEdge> get_subgraph(int tree){
        if(tree==1) {
            if (subgraph1 == null) {
                get_internal(1);
                subgraph1 = new AsSubgraph(full_tree1, subgraphNodes1);
                subgraph1 = GraphMethods.remove_root(subgraph1);
            }
            return subgraph1;
        } else if(tree==2) {
            if (subgraph2 == null) {
                get_internal(2);
                subgraph2 = new AsSubgraph(full_tree2, subgraphNodes2);
                subgraph2 = GraphMethods.remove_root(subgraph2);
            }
            return subgraph2;
        }
        return null;
    }

    public Set<Node> get_internal_nodes(Graph<Node, DefaultEdge> tree, Set<Node> leafNodes) {
        Set<Node> internalNodes = new HashSet<>();
        List<Node> leafNodesList = new ArrayList<>(leafNodes);
        Node node1 = leafNodesList.get(0);

        for (Node node : leafNodes) {
            BFSShortestPath<Node, DefaultEdge> BFSShortestPath = new BFSShortestPath<Node, DefaultEdge>(tree);
            GraphPath<Node, DefaultEdge> shortestPath = BFSShortestPath.getPath(node, node1);
            List<Node> pathNodes = shortestPath.getVertexList();
            for (Node pathNode : pathNodes) {
                if(!internalNodes.contains(pathNode)){
                    internalNodes.add(pathNode);
                }
            }

        }
        return internalNodes;
    }

    public boolean is_valid(){
        if(!checked_valid){
            valid = GraphMethods.graphs_equal(get_subgraph(1), get_subgraph(2));
            checked_valid = true;
        }
        return valid;
    }

    public boolean contains(String leaf){
        for(Node node: leaves){
            if(node.name==leaf){
                return true;
            }
        }
        return false;
    }

}
