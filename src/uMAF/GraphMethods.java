package uMAF;

import org.jgrapht.Graph;
import org.jgrapht.GraphMapping;
import org.jgrapht.alg.isomorphism.VF2SubgraphIsomorphismInspector;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;

public class GraphMethods {

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
            tree.removeVertex(node);
            tree.addEdge(neighbors.get(0), neighbors.get(1));
        }
        return tree;
    }

    // Possibly used as a check after output
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
                System.out.print(u+" "+entry.getValue()+"    ");
                System.out.print(u.equals(entry.getValue()));
                if(!u.equals(entry.getValue())){
                    this_map = false;
                    break;
                }
            }
            System.out.println(map);
            if(this_map){
                return true;
            }
        }
        return false;
    }

}
