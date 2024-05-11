package uMAF;

import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.util.*;

public class Dual {
    int TECHNIQUE = 1;  // 0 is decreasing leaves     1 is decreasing internal nodes
    private final List<TreeNode> leaves;
    final private List<Integer> internal1;
    final private List<Integer> internal2;
    public IloCplex cplex;

    IloNumVarArray var;
    IloRangeArray  rng;
    IloObjective objective;

    public Dual(List<LeafSet> leafSets, List<TreeNode> leaves, List<Integer> internal1, List<Integer> internal2){
        this.leaves = leaves;
        this.internal1 = internal1;
        this.internal2 = internal2;
        writeInitialDual(leafSets);
    }

    private void writeInitialDual(List<LeafSet> leafSets) {
        try {
            cplex = new IloCplex();
            var = new IloNumVarArray();
            rng = new IloRangeArray();
            populate(leafSets);
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }
    public void populate(List<LeafSet> leafSets) throws IloException {

        int numVars = leaves.size() + internal1.size() + internal2.size() + 1;

        if(TECHNIQUE==0) {
            for (TreeNode leaf : leaves) {
                var.add(cplex.numVar(0, 1, leaf.toString()));
            }
            for (int internalNode : internal1) {
                var.add(cplex.numVar(0, 0, STR."internal\{internalNode}"));
            }
            for (int internalNode : internal2) {
                var.add(cplex.numVar(0, 0, STR."internal\{internalNode}"));
            }
        }else{
            for (TreeNode leaf : leaves) {
                var.add(cplex.numVar(1, 1, leaf.toString()));
            }
            for (int internalNode : internal1) {
                var.add(cplex.numVar(-Double.MAX_VALUE, 0, STR."internal\{internalNode}"));
            }
            for (int internalNode : internal2) {
                var.add(cplex.numVar(-Double.MAX_VALUE, 0, STR."internal\{internalNode}"));
            }
        }

        double[] objvals = new double[numVars];
        Arrays.fill(objvals, 1000.0);
        objective = cplex.addMaximize(cplex.scalProd(var.getArray(), objvals));

        // add constraints so leaves are in exactly one set
        for (LeafSet leafset : leafSets) {
            IloLinearNumExpr leafConstraint =  addConstraint(leafset);
            rng.add(cplex.addLe(leafConstraint, 1.0, leafset.toString()));
        }
    }

    private IloLinearNumExpr addConstraint(LeafSet leafset) throws IloException {
        int i=0;
        IloLinearNumExpr leafConstraint = cplex.linearNumExpr();
        for (TreeNode leaf : leaves) {
            if (leafset.contains(leaf)) {
                leafConstraint.addTerm(1.0, var.getElement(i));
            }
            i++;
        }
        for (int internalNode : internal1) {
            if (leafset.has_internal(internalNode, 1)) {
                leafConstraint.addTerm(1.0, var.getElement(i));
            }
            i++;
        }
        for (int internalNode : internal2) {
            if (leafset.has_internal(internalNode, 2)) {
                leafConstraint.addTerm(1.0, var.getElement(i));
            }
            i++;
        }
        return leafConstraint;
    }


    /**
     * Get duals based on solved cplex
     * @return
     * @throws IloException
     */
    public Map<String, Double> extractDuals() {
        Map<String, Double> dualsMAP = new HashMap<>();
        try{
            cplex.solve();
            for (int i=0;i< var._num;i++){
                IloNumVar v = var.getElement(i);
                if(!v.getName().contains("max")) {
                    dualsMAP.put(v.getName(), cplex.getValue(v));

                }
            }

        } catch (IloException e) {
            throw new RuntimeException(e);
        }
        return dualsMAP;
    }

    /**
     * Adds new constraint to dual and solves
     * @param newLeafSet
     * @return
     */
    public Map<String, Double> extractDuals(LeafSet newLeafSet) {
        try {
            IloLinearNumExpr leafConstraint =  addConstraint(newLeafSet);
            rng.add(cplex.addLe(leafConstraint, 1.0, newLeafSet.toString()));
            // add constraints to that 'max' is the max of leaves or max of -internal
            IloColumn column = cplex.column(objective,  -1.0 /newLeafSet.leaves.size()*newLeafSet.leaves.size());
            var.add(cplex.numVar(column, -Double.MAX_VALUE, 1, STR."max\{newLeafSet.toString()}"));
            if(TECHNIQUE==0) {
                int i = 0;
                for (TreeNode leaf : leaves) {
                    if(newLeafSet.contains(leaf)) {
                        IloLinearNumExpr maxConstraint = cplex.linearNumExpr();
                        maxConstraint.addTerm(-1.0, var.getElement(var._num-1));
                        maxConstraint.addTerm(1.0, var.getElement(i));
                        rng.add(cplex.addLe(maxConstraint, 0.0, STR."max_constraint\{leaf}"));
                    }
                    i++;
                }
            }else{
                int i = leaves.size();
                for (int internal : internal1) {
                    if(newLeafSet.has_internal(internal,1)) {
                        IloLinearNumExpr maxConstraint = cplex.linearNumExpr();
                        maxConstraint.addTerm(-1.0, var.getElement(var._num-1));
                        maxConstraint.addTerm(-1.0, var.getElement(i));
                        rng.add(cplex.addLe(maxConstraint, 0.0, STR."max_constraint\{internal}"));
                    }
                    i++;
                }
                for (int internal : internal2) {
                    if(newLeafSet.has_internal(internal,2)) {
                        IloLinearNumExpr maxConstraint = cplex.linearNumExpr();
                        maxConstraint.addTerm(-1.0, var.getElement(var._num-1));
                        maxConstraint.addTerm(-1.0, var.getElement(i));
                        rng.add(cplex.addLe(maxConstraint, 0.0, STR."max_constraint\{internal}"));
                    }
                    i++;
                }
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
        return extractDuals();
    }




    /**
     * Get initial duals without need for solving
     * @return
     */
    public Map<String, Double> initialDuals() {
        Map<String, Double> duals = new HashMap<>();
        for(TreeNode n:leaves){
            duals.put(n.name, 1.0);
        }
        for(int n: internal1){
            duals.put(STR."internal\{n}", 0.0);
        }
        for(int n: internal2){
            duals.put(STR."internal\{n}", 0.0);
        }
        return duals;
    }


}
