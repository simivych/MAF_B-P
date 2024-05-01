package uMAF;

import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.util.*;

public class Dual {
    int TECHNIQUE = 1;  // 0 is decreasing leaves     1 is decreasing internal nodes
    private final List<Node> leaves;
    final private List<Integer> internal1;
    final private List<Integer> internal2;
    public IloCplex cplex;

    IloNumVar[][] var;
    IloRangeArray  rng;

    public Dual(List<LeafSet> leafSets, List<Node> leaves, List<Integer> internal1, List<Integer> internal2){
        this.leaves = leaves;
        this.internal1 = internal1;
        this.internal2 = internal2;
        writeInitialDual(leafSets);
    }

    private void writeInitialDual(List<LeafSet> leafSets) {
        try {
            cplex = new IloCplex();
            var = new IloNumVar[1][];
            rng = new IloRangeArray();
            populate(cplex, var, rng, leafSets);
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }
    public void populate(IloMPModeler model, IloNumVar[][] var, IloRangeArray rng, List<LeafSet> leafSets) throws IloException {

        int numVars = leaves.size() + internal1.size() + internal2.size();

        IloNumVar[] x = new IloNumVar[numVars];
        int i = 0;
        if(TECHNIQUE==0) {
            for (Node leaf : leaves) {
                x[i] = model.numVar(0, 1, leaf.toString());
                i++;
            }
            for (int internalNode : internal1) {
                x[i] = model.numVar(0, 0, STR."internal\{internalNode}");
                i++;
            }
            for (int internalNode : internal2) {
                x[i] = model.numVar(0, 0, STR."internal\{internalNode}");
                i++;
            }
        }else{
            for (Node leaf : leaves) {
                x[i] = model.numVar(1, 1, leaf.toString());
                i++;
            }
            for (int internalNode : internal1) {
                x[i] = model.numVar(-Double.MAX_VALUE, 0, STR."internal\{internalNode}");
                i++;
            }
            for (int internalNode : internal2) {
                x[i] = model.numVar(-Double.MAX_VALUE, 0, STR."internal\{internalNode}");
                i++;
            }
        }
        var[0] = x;

        double[] objvals = new double[numVars];
        Arrays.fill(objvals, 1.0);
        model.addMaximize(model.scalProd(x, objvals));

        // add constraints so leaves are in exactly one set
        for (LeafSet leafset : leafSets) {
            IloLinearNumExpr leafConstraint =  addConstraint(leafset);
            rng.add(model.addLe(leafConstraint, 1.0, leafset.toString()));
        }
    }

    private IloLinearNumExpr addConstraint(LeafSet leafset) throws IloException {
        int i=0;
        IloLinearNumExpr leafConstraint = cplex.linearNumExpr();
        for (Node leaf : leaves) {
            if (leafset.contains(leaf)) {
                leafConstraint.addTerm(1.0, var[0][i]);
            }
            i++;
        }
        for (int internalNode : internal1) {
            if (leafset.has_internal(internalNode, 1)) {
                leafConstraint.addTerm(1.0, var[0][i]);
            }
            i++;
        }
        for (int internalNode : internal2) {
            if (leafset.has_internal(internalNode, 2)) {
                leafConstraint.addTerm(1.0, var[0][i]);
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
            for (IloNumVar v : var[0]) {
                dualsMAP.put(v.getName(), cplex.getValue(v));
            }

        } catch (IloException e) {
            throw new RuntimeException(e);
        }
        printd(dualsMAP);
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
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
        return extractDuals();
    }

    public void printd(Map<String, Double> duals){
        for (String s: duals.keySet()) {
            System.out.println(STR."  \{s}    \{duals.get(s)}");
        }

    }


    /**
     * Get initial duals without need for solving
     * @return
     */
    public Map<String, Double> initialDuals() {
        Map<String, Double> duals = new HashMap<>();
        for(Node n:leaves){
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
