package uMAF1.misc;

import org.jgrapht.Graph;
import org.jgrapht.GraphMapping;
import org.jgrapht.alg.isomorphism.VF2SubgraphIsomorphismInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.*;

public class GraphMethods {

    /**
     * Creates unrooted tree from rooted tree
     * @param tree
     * @return
     */
    public static Graph<Node, DefaultEdge> remove_root(Graph<Node, DefaultEdge> tree){
        List<Node> nodesToRemove = new ArrayList<>();
        for (Node node : tree.vertexSet()) {
            if (tree.degreeOf(node) == 2) {
                nodesToRemove.add(node);
            }
        }

        for (Node node : nodesToRemove) {
            List<Node> neighbors = new ArrayList<>();
            for (DefaultEdge edge : tree.edgesOf(node)) {
                Node n1 = tree.getEdgeTarget(edge);
                Node n2 = tree.getEdgeSource(edge);
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
    public static boolean graphs_equal(Graph<Node, DefaultEdge> g1, Graph<Node, DefaultEdge> g2){
        VF2SubgraphIsomorphismInspector ii = new VF2SubgraphIsomorphismInspector(g1, g2);

        for (Iterator<GraphMapping> it = ii.getMappings(); it.hasNext(); ) {
            boolean this_map = true;
            GraphMapping map = it.next();
            Map<String, Node> vertexMap = new TreeMap<>();
            Set<Node> vertexSet = g1.vertexSet();
            for (Node v : vertexSet) {
                vertexMap.put(v.toString(), v);
            }
            for (Map.Entry<String, Node> entry : vertexMap.entrySet()) {
                Node u = (Node) map.getVertexCorrespondence(entry.getValue(), true);
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

    public static Graph<Node, DefaultEdge> subgraph(Graph<Node, DefaultEdge> tree, Set<Node> subgraphNodes) {
        DefaultUndirectedGraph<Node, DefaultEdge> copied = new DefaultUndirectedGraph<>(DefaultEdge.class);
        for(Node n : subgraphNodes){
            copied.addVertex(n);
        }
        for(DefaultEdge e : tree.edgeSet()){
            Node n1 = tree.getEdgeTarget(e);
            Node n2 = tree.getEdgeSource(e);
            if(subgraphNodes.contains(n1)&&subgraphNodes.contains(n2)){
                copied.addEdge(n1,n2);
            }
        }
        return copied;


    }
}
