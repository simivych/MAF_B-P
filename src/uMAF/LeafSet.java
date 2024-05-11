package uMAF;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.BFSShortestPath;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;

public class LeafSet {
    public Set<TreeNode> subgraphNodes1;
    public Set<TreeNode> subgraphNodes2;
    public Set<TreeNode> leaves;
    public Graph<TreeNode, DefaultEdge> subgraph1;
    public Graph<TreeNode, DefaultEdge> subgraph2;
    public Graph<TreeNode, DefaultEdge> full_tree1;
    public Graph<TreeNode, DefaultEdge> full_tree2;
    public boolean valid = false;
    public boolean checked_valid = false;

    public LeafSet(Graph<TreeNode, DefaultEdge> tree1, Graph<TreeNode, DefaultEdge> tree2, Set<TreeNode> newLeaves) {
        this.full_tree1 = tree1;
        this.full_tree2 = tree2;
        this.leaves = newLeaves;
    }

    public LeafSet(Graph<TreeNode, DefaultEdge> tree1, Graph<TreeNode, DefaultEdge> tree2, Set<TreeNode> leaves, Set<TreeNode> subgraphNodes1, Set<TreeNode> subgraphNodes2) {
        this.full_tree1 = tree1;
        this.full_tree2 = tree2;
        this.leaves = leaves;
        this.subgraphNodes1 = subgraphNodes1;
        this.subgraphNodes2 = subgraphNodes2;
    }

    public boolean has_internal(int internal, int tree){
        if(tree==1){
            if (subgraphNodes1 == null) {
                subgraphNodes1 = get_internal_nodes(full_tree1);
            }
            for(TreeNode possibleNode: subgraphNodes1){
                if(possibleNode.id == internal){
                    return true;
                }
            }
        }else{
            if (subgraphNodes2 == null) {
                subgraphNodes2 = get_internal_nodes(full_tree2);
            }
            for(TreeNode possibleNode: subgraphNodes2){
                if(possibleNode.id == internal){
                    return true;
                }
            }
        }
        return false;
    }

    public Graph<TreeNode, DefaultEdge> get_subgraph(int tree){
        if(tree==1) {
            if (subgraph1 == null) {
                subgraphNodes1 = get_internal_nodes(full_tree1);
                subgraph1 = GraphMethods.subgraph(full_tree1, subgraphNodes1);
                subgraph1 = GraphMethods.remove_root(subgraph1);
            }
            return subgraph1;
        } else if(tree==2) {
            if (subgraph2 == null) {
                subgraphNodes2 = get_internal_nodes(full_tree2);
                subgraph2 = GraphMethods.subgraph(full_tree2, subgraphNodes2);
                subgraph2 = GraphMethods.remove_root(subgraph2);
            }
            return subgraph2;
        }
        return null;
    }

    public Set<TreeNode> get_internal_nodes(Graph<TreeNode, DefaultEdge> tree) {
        Set<TreeNode> internalNodes = new HashSet<>();
        List<TreeNode> leafNodesList = new ArrayList<>(leaves);
        TreeNode node1 = leafNodesList.getFirst();
        for (TreeNode node : leaves) {
            if(!node.equals(node1)) {
                BFSShortestPath<TreeNode, DefaultEdge> BFSShortestPath = new BFSShortestPath<TreeNode, DefaultEdge>(tree);
                GraphPath<TreeNode, DefaultEdge> shortestPath = BFSShortestPath.getPath(node, node1);
                List<TreeNode> pathNodes = shortestPath.getVertexList();
                for (TreeNode pathNode : pathNodes) {
                    if (!internalNodes.contains(pathNode)) {
                        internalNodes.add(pathNode);
                    }
                }
            }

        }
        return internalNodes;
    }

    public boolean is_valid(){
        if(!checked_valid){
            Graph<TreeNode, DefaultEdge> s1 = get_subgraph(1);
            Graph<TreeNode, DefaultEdge> s2 = get_subgraph(2);
            valid = GraphMethods.graphs_equal(get_subgraph(1), get_subgraph(2));
            checked_valid = true;
        }
        return valid;
    }

    public boolean is_valid_rooted(TreeNode n1, TreeNode n2){
        return GraphMethods.graphs_equal(get_subgraph_rooted(1, n1), get_subgraph_rooted(2, n2));
    }

    public Graph<TreeNode, DefaultEdge> get_subgraph_rooted(int tree, TreeNode n){
        if(tree==1) {
            subgraphNodes1 = get_internal_nodes(full_tree1);
            Graph<TreeNode, DefaultEdge>  subgraph_1 = GraphMethods.subgraph(full_tree1, subgraphNodes1);
            subgraph_1 = GraphMethods.remove_root(full_tree1, subgraph_1, n);
            return subgraph_1;
        } else if(tree==2) {
            subgraphNodes2 = get_internal_nodes(full_tree2);
            Graph<TreeNode, DefaultEdge>  subgraph_2 = GraphMethods.subgraph(full_tree2, subgraphNodes2);
            subgraph_2 = GraphMethods.remove_root(full_tree2, subgraph_2, n);
            return subgraph_2;
        }
        return null;
    }

    public boolean contains(TreeNode leaf){
        for(TreeNode node: leaves){
            if(node.name.equals(leaf.name)){
                return true;
            }
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (TreeNode node : leaves) {
            sb.append(node.name).append("_");
        }
        // Remove the last underscore if the set is not empty
        if (!leaves.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

}
