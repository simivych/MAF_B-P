package uMAF;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;

public class BandP {
    public Graph<Node, DefaultEdge> tree1;
    public Graph<Node, DefaultEdge> tree2;
    public List<Node> leaves;
    public List<Integer> internal1;
    public List<Integer> internal2;
    public double solution;

    public BandP(Graph<Node, DefaultEdge> tree1, Graph<Node, DefaultEdge> tree2){
        this.tree1 = tree1;
        this.tree2 = tree2;
        solve();

    }

    /**
     * Creates initail leaf sets (individual leaves to ensure feasibility)
     * and calls LP to solve with BandP
     */
    private void solve() {
        // initialise individual leaves as leafsets to ensure feasibility
        leaves = new ArrayList<>();
        internal1 = new ArrayList<>();
        for (Node node: tree1.vertexSet()){
            if(!node.isInternal()){
                leaves.add(node);
            }else{
                internal1.add(node.id);
            }
        }
        internal2 = new ArrayList<>();
        for (Node node: tree2.vertexSet()){
            if(node.isInternal()){
                internal2.add(node.id);
            }
        }
        List<LeafSet> initialLeafSets = new ArrayList<>();
        for(Node leaf : leaves){
            Set<Node> leafNode = new HashSet<>();
            leafNode.add(leaf);
            LeafSet leafSet = new LeafSet(tree1, tree2, leafNode);
            initialLeafSets.add(leafSet);
        }
        LP lp = new LP(initialLeafSets, leaves, internal1, internal2, tree1, tree2);
        solution = lp.solve();
    }

}
