package uMAF;

import org.jgrapht.Graph;
import org.jgrapht.GraphMapping;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.isomorphism.VF2SubgraphIsomorphismInspector;
import org.jgrapht.alg.shortestpath.BFSShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.*;

public class GraphMethods {

    /**
     * Creates unrooted tree from rooted tree
     * @param tree
     * @return
     */
    public static Graph<TreeNode, DefaultEdge> remove_root(Graph<TreeNode, DefaultEdge> tree){
        List<TreeNode> nodesToRemove = new ArrayList<>();
        for (TreeNode node : tree.vertexSet()) {
            if (tree.degreeOf(node) == 2) {
                nodesToRemove.add(node);
            }
        }

        for (TreeNode node : nodesToRemove) {
            List<TreeNode> neighbors = new ArrayList<>();
            for (DefaultEdge edge : tree.edgesOf(node)) {
                TreeNode n1 = tree.getEdgeTarget(edge);
                TreeNode n2 = tree.getEdgeSource(edge);
                if(n1==node){
                    neighbors.add(n2);
                }else{
                    neighbors.add(n1);
                }
            }
            tree.addEdge(neighbors.get(0), neighbors.get(1));
            tree.removeVertex(node);
        }
        return tree;
    }

    public static Graph<TreeNode, DefaultEdge> remove_root(Graph<TreeNode, DefaultEdge> full_tree,Graph<TreeNode, DefaultEdge> tree, TreeNode n){
        if(!tree.vertexSet().contains(n)){
            int shortest = 500;
            TreeNode closest = tree.vertexSet().iterator().next();
            for(TreeNode tree_node:tree.vertexSet()) {
                BFSShortestPath<TreeNode, DefaultEdge> BFSShortestPath = new BFSShortestPath<TreeNode, DefaultEdge>(full_tree);
                GraphPath<TreeNode, DefaultEdge> shortestPath = BFSShortestPath.getPath(tree_node, n);
                if(shortest>shortestPath.getVertexList().size()){
                    closest = tree_node;
                    shortest = shortestPath.getVertexList().size();
                }
            }
            n = closest;
        }
        List<TreeNode> nodesToRemove = new ArrayList<>();
        for (TreeNode node : tree.vertexSet()) {
            if (tree.degreeOf(node) == 2 && !node.equals(n)) {
                nodesToRemove.add(node);
            }
        }

        for (TreeNode node : nodesToRemove) {
            List<TreeNode> neighbors = new ArrayList<>();
            for (DefaultEdge edge : tree.edgesOf(node)) {
                TreeNode n1 = tree.getEdgeTarget(edge);
                TreeNode n2 = tree.getEdgeSource(edge);
                if(n1==node){
                    neighbors.add(n2);
                }else{
                    neighbors.add(n1);
                }
            }
            tree.addEdge(neighbors.get(0), neighbors.get(1));
            tree.removeVertex(node);
        }
        return tree;
    }

    /**
     * Checks if trees are isomorphic
     * Possible to use as check when testing
     * @param g1
     * @param g2
     * @return
     */
    public static boolean graphs_equal(Graph<TreeNode, DefaultEdge> g1, Graph<TreeNode, DefaultEdge> g2){
        VF2SubgraphIsomorphismInspector ii = new VF2SubgraphIsomorphismInspector(g1, g2);

        for (Iterator<GraphMapping> it = ii.getMappings(); it.hasNext(); ) {
            boolean this_map = true;
            GraphMapping map = it.next();
            Map<String, TreeNode> vertexMap = new TreeMap<>();
            Set<TreeNode> vertexSet = g1.vertexSet();
            for (TreeNode v : vertexSet) {
                vertexMap.put(v.toString(), v);
            }
            for (Map.Entry<String, TreeNode> entry : vertexMap.entrySet()) {
                TreeNode u = (TreeNode) map.getVertexCorrespondence(entry.getValue(), true);
                if(!u.is_equal(entry.getValue())){
                    this_map = false;
                    break;
                }
            }
            if(this_map){
                return true;
            }
        }
        return false;
    }

    public static Graph<TreeNode, DefaultEdge> subgraph(Graph<TreeNode, DefaultEdge> tree, Set<TreeNode> subgraphNodes) {
        DefaultUndirectedGraph<TreeNode, DefaultEdge> copied = new DefaultUndirectedGraph<>(DefaultEdge.class);
        for(TreeNode n : subgraphNodes){
            copied.addVertex(n);
        }
        for(DefaultEdge e : tree.edgeSet()){
            TreeNode n1 = tree.getEdgeTarget(e);
            TreeNode n2 = tree.getEdgeSource(e);
            if(subgraphNodes.contains(n1)&&subgraphNodes.contains(n2)){
                copied.addEdge(n1,n2);
            }
        }
        return copied;


    }
}
