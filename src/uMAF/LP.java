package uMAF;

import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LP {
    public static LPResult solve(List<LeafSet> leafSets, List<LeafSet> leafSetsFixed0, List<LeafSet> leafSetsFixed1, List<Node> leaves, List<Integer> internal1, List<Integer> internal2) {
        // Create the modeler/solver object
        try (IloCplex cplex = new IloCplex()) {

            IloNumVar[][] var = new IloNumVar[1][];
            IloRange[][]  rng = new IloRange[1][];
            Map<String, Double> duals = new HashMap();

            populate(cplex, var, rng, leafSets, leaves, leafSetsFixed0, leafSetsFixed1, internal1, internal2);


            // solve the model and display the solution if one was found
            if ( cplex.solve() ) {
                double[] x     = cplex.getValues(var[0]);
                double[] pi    = cplex.getDuals(rng[0]);


                cplex.output().println("Solution status = " + cplex.getStatus());
                cplex.output().println("Solution value  = " + cplex.getObjValue());

                int nvars = x.length;
                for (int j = 0; j < nvars; ++j) {
                    cplex.output().println("Variable " + j +
                            ": Value = " + x[j]);
                }

                int ncons = pi.length;
                for (int i = 0; i < ncons; ++i) {
                    cplex.output().println("Constraint " + rng[0][i].getName() +
                            " Pi = " + pi[i] );
                    duals.put(rng[0][i].getName(), pi[i]);
                }
            }
            LPResult lpResult = new LPResult(duals, cplex.getObjValue());
            return lpResult;
        }
        catch (IloException e) {
            e.printStackTrace();
        }
        return null;

    }

    static void populate(IloMPModeler model,
                               IloNumVar[][] var,
                               IloRange[][] rng, List<LeafSet> leavesInSubsets,  List<Node> leaves, List<LeafSet> leafSetsFixed0, List<LeafSet> leafSetsFixed1, List<Integer> internal1, List<Integer> internal2) throws IloException {

        int numSubtrees = leavesInSubsets.size();
        int numConstraints = leaves.size() + leafSetsFixed0.size() + leafSetsFixed1.size() + internal1.size() + internal2.size();

        double[] lb = new double[numSubtrees];
        Arrays.fill(lb, 0.0);
        double[] ub = new double[numSubtrees];
        Arrays.fill(ub, 1.0);

        IloNumVar[] x = new IloNumVar[numSubtrees];
        int i = 0;
        for (LeafSet leafset : leavesInSubsets) {
            x[i] = model.numVar(0,1,leafset.name);
            leafset.ILPval = i;
            i++;
        }
        var[0] = x;

        double[] objvals = new double[numSubtrees];
        Arrays.fill(objvals, 1.0);
        model.addMinimize(model.scalProd(x, objvals));

        rng[0] = new IloRange[numConstraints];
        i = 0;
        // add constraints so leaves are in exactly one set
        for (Node leaf : leaves) {
            IloLinearNumExpr leafConstraint = model.linearNumExpr();
            for (LeafSet leafset : leavesInSubsets) {
                if (leafset.contains(leaf.name)) {
                    leafConstraint.addTerm(1.0, x[leafset.ILPval]);
                }
            }
            rng[0][i] = model.addEq(leafConstraint, 1.0, leaf.name);
            i++;
        }
        // add constraints so internal nodes dont overlap
        for (int internalNode : internal1) {
            IloLinearNumExpr leafConstraint = model.linearNumExpr();
            for (LeafSet leafSet : leavesInSubsets){
                if(leafSet.has_internal(internalNode, 1)){
                    leafConstraint.addTerm(1.0, x[leafSet.ILPval]);
                }
            }
            rng[0][i] = model.addLe(leafConstraint, 1.0, "internal" + internalNode);
            i++;
        }
        for (int internalNode : internal2) {
            IloLinearNumExpr leafConstraint = model.linearNumExpr();
            for (LeafSet leafSet : leavesInSubsets){
                if(leafSet.has_internal(internalNode, 2)){
                    leafConstraint.addTerm(1.0, x[leafSet.ILPval]);
                }
            }
            rng[0][i] = model.addLe(leafConstraint, 1.0, "internal" + internalNode);
            i++;
        }

        // add branching constraints
        for (LeafSet leafset : leafSetsFixed0) {
            IloLinearNumExpr leafConstraint = model.linearNumExpr();
            leafConstraint.addTerm(1.0, x[leafset.ILPval]);
            rng[0][i] = model.addEq(leafConstraint, 0.0, "fixed" + i);
            i++;
        }
        for (LeafSet leafset : leafSetsFixed1) {
            IloLinearNumExpr leafConstraint = model.linearNumExpr();
            leafConstraint.addTerm(1.0, x[leafset.ILPval]);
            rng[0][i] = model.addEq(leafConstraint, 1.0, "fixed" + i);
            i++;
        }
        System.out.println(Arrays.deepToString(rng));
    }

}
