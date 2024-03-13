package uMAF;

import java.util.List;

public class PartialSolution {
    public List<LeafSet> leafSets;
    public List<LeafSet> leafSetsFixed0;
    public List<LeafSet> leafSetsFixed1;
    public List<Node> all_leaves;
    public List<Integer> internal1;
    public List<Integer> internal2;
    public LPResult solution = null;
    
    public PartialSolution(List<LeafSet> usedLeafSets, List<LeafSet> leafSetsFixed0, List<LeafSet> leafSetsFixed1, List<Node> all_leaves, List<Integer> internal1, List<Integer> internal2){
        this.leafSets = usedLeafSets;
        this.leafSetsFixed0 = leafSetsFixed0;
        this.leafSetsFixed1 = leafSetsFixed1;
        this.all_leaves = all_leaves;
        this.internal1 = internal1;
        this.internal2 = internal2;
    }


    public LPResult solve(){
        if(solution==null){
            solution = LP.solve(leafSets, leafSetsFixed0, leafSetsFixed1, all_leaves, internal1, internal2);
        }
        return solution;
    }

    public boolean is_integral(){
        if(solution==null){
            solve();
        }
        return (solution.value % 1) == 0;
    }

    public void addLeafSet(LeafSet newLeafSet){
        leafSets.add(newLeafSet);
        solution=null;
    }

}
