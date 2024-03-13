package uMAF;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;

public class BandP {
    public Stack<PartialSolution> branchingSolutions;
    public Graph<Node, DefaultEdge> tree1;
    public Graph<Node, DefaultEdge> tree2;
    public List<Node> leaves;
    public List<Integer> internal1;
    public List<Integer> internal2;
    public int solution;

    public BandP(Graph<Node, DefaultEdge> tree1, Graph<Node, DefaultEdge> tree2){
        this.tree1 = tree1;
        this.tree2 = tree2;
        generate();

    }

    private void generate() {
        leaves = new ArrayList<>();
        internal1 = new ArrayList<>();
        for (Node node: tree1.vertexSet()){
            if(!node.name.equals("")){
                leaves.add(node);
            }else{
                internal1.add(node.id);
            }
        }
        internal2 = new ArrayList<>();
        for (Node node: tree2.vertexSet()){
            if(node.name.equals("")){
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
        PartialSolution root = new PartialSolution(initialLeafSets, new ArrayList<LeafSet>(), new ArrayList<LeafSet>(), leaves,  internal1, internal2);
        branchingSolutions = new Stack<>();
        branchingSolutions.add(root);
        solution = leaves.size();
    }

    public void run(){
        // while we have branching solutions to check
        while(branchingSolutions.size()!=0){
            // get current branching solution and solve
            PartialSolution ps = branchingSolutions.pop();
            LPResult lpResult = ps.solve();
            double current_solution = lpResult.value;
            PartialSolution column_gen = generate_column(ps, lpResult.duals);
            // if we can generate columns continue until no columns can be added
            while(column_gen!=null){
                ps = column_gen; // update partial solution based on columns generated
                lpResult = ps.solve();
                current_solution = lpResult.value;
                column_gen = generate_column(ps, lpResult.duals); // see if more columnns can be generated
            }
            // once no columns can be generated check if the solution is integral
            // if integral save the best solution
            if(ps.is_integral()){
                if(current_solution < solution) {
                    solution = (int) current_solution;
                }
            // if not integral branch
            }else{
                branch(ps.leafSets, ps.leafSetsFixed0, ps.leafSetsFixed1);
            }

        }

    }

    private void branch(List<LeafSet> leafSets, List<LeafSet> leafSetsFixed0, List<LeafSet> leafSetsFixed1) {
        //TODO add fixed constraints and create new partial solutions
        // and add them to branchingSolutions (do not change leaf sets)
    }

    public PartialSolution generate_column(PartialSolution ps, Map<String, Double> duals){
        MAST mast = new MAST(tree1, tree2, duals);
        Set<Node> newLeaves = mast.getMAST();
        if(newLeaves!=null){
            LeafSet newLeafSet = new LeafSet(tree1, tree2, newLeaves);
            ps.addLeafSet(newLeafSet);
            return ps;
        }
        return null;
    }

}
