package uMAF1.model;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jorlib.frameworks.columnGeneration.model.ModelInterface;
import uMAF1.misc.Node;
import uMAF1.colgen.Leafset;

import java.util.ArrayList;
import java.util.List;

public final class MAF implements ModelInterface {

    public Graph<Node, DefaultEdge> tree1;
    public Graph<Node, DefaultEdge> tree2;

    public List<Node> leaves;
    public List<Integer> internal1;
    public List<Integer> internal2;
    public List<Leafset> leafSets;

    public void set_vars(Graph<Node, DefaultEdge> tree1, Graph<Node, DefaultEdge> tree2){
        this.tree1 = tree1;
        this.tree2 = tree2;
        leaves = new ArrayList<>();
        internal1 = new ArrayList<>();
        internal2 = new ArrayList<>();
        for(Node n:tree1.vertexSet()){
            if(n.isInternal()){
                internal1.add(n.id);
            }else{
                leaves.add(n);
            }
        }
        for(Node n:tree2.vertexSet()){
            if(n.isInternal()) {
                internal2.add(n.id);
            }
        }
        leafSets = new ArrayList<>();
    }

    @Override
    public String getName() {
        return "CuttingStockExample";
    }

    public void add_leafset(Leafset leafset) {
        leafSets.add(leafset);
    }
}