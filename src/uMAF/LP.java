package uMAF;

import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.shortestpath.BFSShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

public class LP {
    public List<LeafSet> leafSets;
    public List<Node> leaves;
    public List<Integer> internal1;
    public List<Integer> internal2;
    public Graph<Node, DefaultEdge> tree1;
    public Graph<Node, DefaultEdge> tree2;
    public long startTime;
    public long duration;

    public LP(List<LeafSet> leafSets, List<Node> leaves, List<Integer> internal1, List<Integer> internal2, Graph<Node, DefaultEdge> tree1, Graph<Node, DefaultEdge> tree2){
        this.leafSets = leafSets;
        this.leaves = leaves;
        this.internal1 = internal1;
        this.internal2 = internal2;
        this.tree1 = tree1;
        this.tree2 = tree2;
        startTime = System.currentTimeMillis();
        duration = 5 * 60 * 1000; // 5 minutes in milliseconds

    }
    public double solve() {
        // Create the modeler/solver object
        try (IloCplex cplex = new IloCplex()) {
            int numConstraints = leaves.size() + internal1.size() + internal2.size();

            IloNumVarArray var = new IloNumVarArray();
            IloRange[]  rng = new IloRange[numConstraints];
            IloObjective MAFsize = cplex.addMinimize();

            populate(cplex, var, rng, MAFsize);

            cplex.setParam(IloCplex.Param.RootAlgorithm, IloCplex.Algorithm.Primal);

            cplex.solve(new BranchingAndPrice(var, rng, MAFsize));

            return cplex.getObjValue();
        }
        catch (IloException e) {
            e.printStackTrace();
        }
        return leaves.size();

    }

    void populate(IloMPModeler model, IloNumVarArray var, IloRange[] rng, IloObjective MAFsize) throws IloException {

        int i = 0;

        // Add RHS of each constraint
        for (Node leaf : leaves) {
            rng[i++] = model.addRange(1.0, 1.0, leaf.name);
        }
        for (int internalNode : internal1) {
            rng[i++] = model.addRange(-Double.MAX_VALUE, 1.0, "internal" + internalNode);
        }
        for (int internalNode : internal2) {
            rng[i++] = model.addRange(-Double.MAX_VALUE, 1.0, "internal" + internalNode);
        }

        // Add LHS of constraints
        for (LeafSet leafSet: leafSets){
            IloColumn column = leafSetColumn(leafSet, model, MAFsize, rng);
            var.add(model.numVar(column, 0.0, 1.0));
        }
    }

    IloColumn leafSetColumn(LeafSet leafSet, IloMPModeler model, IloObjective MAFsize, IloRange[] rng) throws IloException {
        IloColumn column = model.column(MAFsize, 1.0);
        int i = 0;
        for (Node leaf : leaves) {
            if(leafSet.contains(leaf)){
                column = column.and(model.column(rng[i++], 1.0));
            }else{
                column = column.and(model.column(rng[i++], 0.0));
            }
        }
        for (int internalNode : internal1) {
            if(leafSet.has_internal(internalNode, 1)){
                column = column.and(model.column(rng[i++], 1.0));
            }else{
                column = column.and(model.column(rng[i++], 0.0));
            }
        }
        for (int internalNode : internal2) {
            if(leafSet.has_internal(internalNode, 2)){
                column = column.and(model.column(rng[i++], 1.0));
            }else{
                column = column.and(model.column(rng[i++], 0.0));
            }
        }
        return column;
    }

    static class IloNumVarArray {
        int _num           = 0;
        IloNumVar[] _array = new IloNumVar[32];

        void add(IloNumVar ivar) {
            if ( _num >= _array.length ) {
                IloNumVar[] array = new IloNumVar[2 * _array.length];
                System.arraycopy(_array, 0, array, 0, _num);
                _array = array;
            }
            _array[_num++] = ivar;
        }

        IloNumVar getElement(int i) {
            return _array[i];
        }
        int getSize() {
            return _num;
        }
        IloNumVar[] getArray(){
            IloNumVar[] array = new IloNumVar[_num];
            for(int i=0;i<_num;i++){
                array[i]=_array[i];
            }
            return array;
        }
    }
    public class BranchingAndPrice  extends IloCplex.Goal{
        IloNumVarArray vars;
        IloRange[] rng;
        IloObjective MAFsize;

        public BranchingAndPrice(IloNumVarArray vars, IloRange[] rng, IloObjective MAFsize) {
            this.vars = vars;
            this.rng = rng;
            this.MAFsize = MAFsize;
        }

        // Branch on var with largest objective coefficient
        // among those that are not ints
        public IloCplex.Goal execute(IloCplex cplex) throws IloException {
            // Add columns until none add to the objective function
            IloColumn column = getColumn(cplex);
            while(column != null) {
                // Add column to cplex and get new
                vars.add(cplex.numVar(column, 0.0, 1.0));
                cplex.solve();
                column = getColumn(cplex);
            }

            double[] x   = cplex.getValues(vars.getArray());

            double maxinf = 0.0;
            int    bestj  = -1;
            int    cols   = vars.getSize();
            for (int j = 0; j < cols; ++j) {
                if ( x[j] % 1 != 0 ) { // if it is not an int
                    double xj_inf = x[j] - Math.floor(x[j]);
                    if ( xj_inf > 0.5 ) {
                        xj_inf = 1.0 - xj_inf;
                    }
                    if (xj_inf >= maxinf) {
                        bestj  = j;
                        maxinf = xj_inf;
                    }
                }
            }

            if ( bestj >= 0 && System.currentTimeMillis() - startTime < duration) {
                return cplex.and(cplex.or(cplex.geGoal(vars.getElement(bestj), Math.floor(x[bestj])+1), cplex.leGoal(vars.getElement(bestj), Math.floor(x[bestj]))), this);
            }
            else
                return null;
        }
        public IloColumn getColumn(IloCplex cplex) throws IloException{
            double[] pi = cplex.getDuals(rng);
            Map<String, Double> duals = new HashMap<>();
            int nconts = pi.length;
            for (int i = 0; i < nconts; ++i) {
                duals.put(rng[i].getName(), pi[i]);
            }
            MAST mast = new MAST(tree1, tree2, duals);
            LeafSet newLeafSet = mast.getMAST();
            if(newLeafSet == null){
                return null;
            }
            return leafSetColumn(newLeafSet, cplex, MAFsize, rng);
        }
    }


}
