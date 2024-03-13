/* --------------------------------------------------------------------------
 * File: LPex1.java
 * Version 22.1.1
 * --------------------------------------------------------------------------
 * Licensed Materials - Property of IBM
 * 5725-A06 5725-A29 5724-Y48 5724-Y49 5724-Y54 5724-Y55 5655-Y21
 * Copyright IBM Corporation 2001, 2022. All Rights Reserved.
 *
 * US Government Users Restricted Rights - Use, duplication or
 * disclosure restricted by GSA ADP Schedule Contract with
 * IBM Corp.
 * --------------------------------------------------------------------------
 *
 * LPex1.java - Entering and optimizing an LP problem
 *
 * Demonstrates different methods for creating a problem.  The user has to
 * choose the method on the command line:
 *
 *    java LPex1  -r     generates the problem by adding constraints
 *    java LPex1  -c     generates the problem by adding variables
 *    java LPex1  -n     generates the problem by adding expressions
 */

import ilog.concert.*;
import ilog.cplex.*;
import uMAF.Branching;

import java.util.Arrays;


public class testCPLEX {

    static int[][] leavesInSubsets = {
            {1},         // x_1
            {2},         // x_2
            {3},         // x_3
            {4},         // x_4
            {1, 2},      // x_5
            {1, 3},      // x_6
            {1, 4},      // x_7
            {2, 3},      // x_8
            {2, 4},      // x_9
            {3, 4},      // x_10
            {1, 2, 3},   // x_11
            {1, 2, 4},   // x_12
            {1, 3, 4},   // x_13
            {2, 3, 4},   // x_14
            {1, 2, 3, 4} // x_15
    };

    public static void main(String[] args) {
        // Create the modeler/solver object
        try (IloCplex cplex = new IloCplex()) {

            IloNumVar[][] var = new IloNumVar[1][];
            IloRange[][]  rng = new IloRange[1][];

            populateByRow1(cplex, var, rng);

            if  ( cplex.solve(new Branching(var[0])) ) {
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
                }
            }
        }
        catch (IloException e) {
            e.printStackTrace();
        }
    }


    // The following methods all populate the problem with data for the following
    // linear program:
    //
    //    Maximize
    //     x1 + 2 x2 + 3 x3
    //    Subject To
    //     - x1 + x2 + x3 <= 20
    //     x1 - 3 x2 + x3 <= 30
    //    Bounds
    //     0 <= x1 <= 40
    //    End
    //
    // using the IloMPModeler API


    static void populateByRow1(IloMPModeler model,
                              IloNumVar[][] var,
                              IloRange[][] rng) throws IloException {

        int numSubtrees = leavesInSubsets.length;
        int numLeaves = 4;

        double[] lb = new double[numSubtrees];
        Arrays.fill(lb, 0.0);
        double[] ub = new double[numSubtrees];
        Arrays.fill(ub, 1.0);

        IloNumVar[] x = new IloNumVar[numSubtrees];
        for (int i = 0; i < numSubtrees; i++) {
            x[i] = model.numVar(0,1,"x_" + (i + 1));
        }
        var[0] = x;

        double[] objvals = new double[numSubtrees];
        Arrays.fill(objvals, 1.0);
        model.addMinimize(model.scalProd(x, objvals));

        rng[0] = new IloRange[numLeaves];
        for (int leafIndex = 1; leafIndex <= numLeaves; leafIndex++) {
            IloLinearNumExpr leafConstraint = model.linearNumExpr();
            for (int subsetIndex = 0; subsetIndex < numSubtrees; subsetIndex++) {
                if (isLeafInSubset(leafIndex, leavesInSubsets[subsetIndex])) {
                    leafConstraint.addTerm(1.0, x[subsetIndex]);
                }
            }
            rng[0][leafIndex - 1] = model.addEq(leafConstraint, 1.0, "leaf_" + leafIndex);
        }
        System.out.println(Arrays.deepToString(rng));
    }

    private static boolean isLeafInSubset(int leaf, int[] subsetLeaves) {
        for (int subsetLeaf : subsetLeaves) {
            if (subsetLeaf == leaf) {
                return true;
            }
        }
        return false;
    }

}
