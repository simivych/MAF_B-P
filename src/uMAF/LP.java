package uMAF;

import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

public class LP {
    public List<LeafSet> leafSets;
    public final List<Node> leaves;
    public final List<Integer> internal1;
    public final List<Integer> internal2;
    public final Graph<Node, DefaultEdge> tree1;
    public final Graph<Node, DefaultEdge> tree2;
    public long startTime;
    public long duration;
    public Dual dual;

    public LP(List<LeafSet> leafSets, List<Node> leaves, List<Integer> internal1, List<Integer> internal2, Graph<Node, DefaultEdge> tree1, Graph<Node, DefaultEdge> tree2){
        this.leafSets = leafSets;
        this.leaves = leaves;
        this.internal1 = internal1;
        this.internal2 = internal2;
        this.tree1 = tree1;
        this.tree2 = tree2;
        this.dual = new Dual(leafSets, leaves, internal1, internal2);
        startTime = System.currentTimeMillis();
        duration = 5 * 60 * 1000; // 5 minutes in milliseconds

    }

    /**
     * set up and run LP
     * @return
     */
    public double solve() {
        // Create the modeler/solver object
        try (IloCplex cplex = new IloCplex()) {
            int numConstraints = leaves.size() + internal1.size() + internal2.size();

            IloNumVarArray var = new IloNumVarArray();
            IloRange[]  rng = new IloRange[numConstraints];
            IloObjective MAFsize = cplex.addMinimize();

            populate(cplex, var, rng, MAFsize);

            cplex.setParam(IloCplex.Param.RootAlgorithm, IloCplex.Algorithm.Primal);

            Map<String, Double> duals = dual.initialDuals();
            MAST mast = new MAST(tree1, tree2, duals);
            LeafSet newLeafSet = mast.getMAST();
            IloColumn column = leafSetColumn(newLeafSet, cplex, MAFsize, rng);
            while(column != null) {
                // Add column to cplex and solve
                var.add(cplex.numVar(column, 0.0, 1.0, newLeafSet.toString()));
                cplex.solve();
                System.out.println();
                System.out.println("VALUES");
                for (int i = 0; i < var._num; ++i) {
                    System.out.println(STR."  Leafset\{var.getElement(i).getName()} = \{cplex.getValue(var.getElement(i))}   \{cplex.getBasisStatus(var.getElement(i))}");
                }
                // get new duals
                duals = dual.extractDuals(newLeafSet);
                // get new column
                mast = new MAST(tree1, tree2, duals);
                newLeafSet = mast.getMAST();
                column = leafSetColumn(newLeafSet, cplex, MAFsize, rng);

            }

            cplex.solve(new BranchingAndPrice(var, rng, MAFsize));
            for (int i = 0; i < var._num; ++i) {
                System.out.println(STR."  Leafset\{var.getElement(i).getName()} = \{cplex.getValue(var.getElement(i))}");
            }

            return cplex.getObjValue()-1;
        }
        catch (IloException e) {
            e.printStackTrace();
        }
        return leaves.size()-1;

    }

    /**
     * Creates initial LP model
     * @param model
     * @param var
     * @param rng
     * @param MAFsize
     * @throws IloException
     */
    void populate(IloMPModeler model, IloNumVarArray var, IloRange[] rng, IloObjective MAFsize) throws IloException {

        int i = 0;

        // Add RHS of each constraint
        for (Node leaf : leaves) {
            rng[i++] = model.addRange(1.0, Double.MAX_VALUE, leaf.name);
        }
        for (int internalNode : internal1) {
            rng[i++] = model.addRange(-Double.MAX_VALUE, 1.0, STR."internal\{internalNode}");
        }
        for (int internalNode : internal2) {
            rng[i++] = model.addRange(-Double.MAX_VALUE, 1.0, STR."internal\{internalNode}");
        }

        // Add LHS of constraints
        for (LeafSet leafSet: leafSets){
            IloColumn column = leafSetColumn(leafSet, model, MAFsize, rng);
            var.add(model.numVar(column, 0.0, 1.0, leafSet.toString()));
        }
    }

    /**
     * Does column generation for a new leafset
     * @param leafSet
     * @param model
     * @param MAFsize
     * @param rng
     * @return
     * @throws IloException
     */
    IloColumn leafSetColumn(LeafSet leafSet, IloMPModeler model, IloObjective MAFsize, IloRange[] rng) throws IloException {
        if(leafSet==null){
            return null;
        }
        IloColumn column = model.column(MAFsize, 1.0);
        int i = 0;
        for (Node leaf : leaves) {
            if(leafSet.contains(leaf)){
                //System.out.println("Contains "+leaf.name);
                column = column.and(model.column(rng[i++], 1.0));
            }else{
                column = column.and(model.column(rng[i++], 0.0));
            }
        }
        for (int internalNode : internal1) {
            if(leafSet.has_internal(internalNode, 1)){
                //System.out.println("Contains internal"+internalNode);
                column = column.and(model.column(rng[i++], 1.0));
            }else{
                //System.out.println("Doesnt contain internal"+internalNode);
                column = column.and(model.column(rng[i++], 0.0));
            }
        }
        for (int internalNode : internal2) {
            if(leafSet.has_internal(internalNode, 2)){
                //System.out.println("Contains internal"+internalNode);
                column = column.and(model.column(rng[i++], 1.0));
            }else{
                //System.out.println("Doesnt contain internal"+internalNode);
                column = column.and(model.column(rng[i++], 0.0));
            }
        }
        return column;
    }

    /**
     * Branching interface
     */
    public class BranchingAndPrice  extends IloCplex.Goal{
        IloNumVarArray vars;
        IloRange[] rng;
        IloObjective MAFsize;

        public BranchingAndPrice(IloNumVarArray vars, IloRange[] rng, IloObjective MAFsize) {
            this.vars = vars;
            this.rng = rng;
            this.MAFsize = MAFsize;
        }

        /**
         * Branch and Price execution
         * @param cplex
         * @return
         * @throws IloException
         */
        public IloCplex.Goal execute(IloCplex cplex) throws IloException {
            // Add columns until none add to the objective function
            LeafSet newLeafSet = getMAST();
            IloColumn column = getColumn(cplex, newLeafSet);
            while(column != null) {
                // Add column to cplex and get new
                vars.add(cplex.numVar(column, 0.0, 1.0, newLeafSet.toString()));
                cplex.solve();
                newLeafSet = getMAST(newLeafSet);
                column = getColumn(cplex, newLeafSet);
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

        private LeafSet getMAST(LeafSet newLeafset) throws IloException {
            Map<String, Double> duals = dual.extractDuals(newLeafset);
            MAST mast = new MAST(tree1, tree2, duals);
            LeafSet newLeafSet = mast.getMAST();
            return newLeafSet;
        }

        private LeafSet getMAST() throws IloException {
            Map<String, Double> duals = dual.extractDuals();
            MAST mast = new MAST(tree1, tree2, duals);
            LeafSet newLeafSet = mast.getMAST();
            return newLeafSet;
        }

        /**
         * Calls MAST and generate column
         * @param cplex
         * @return a new generated column
         * @throws IloException
         */
        public IloColumn getColumn(IloCplex cplex, LeafSet newLeafSet) throws IloException {
            if(newLeafSet == null){
                return null;
            }
            return leafSetColumn(newLeafSet, cplex, MAFsize, rng);
        }
    }


}

