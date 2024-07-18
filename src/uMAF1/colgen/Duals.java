package uMAF1.colgen;

import ilog.concert.*;
import ilog.cplex.IloCplex;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jorlib.frameworks.columnGeneration.master.MasterData;
import uMAF1.misc.IloNumVarArray;
import uMAF1.misc.IloRangeArray;
import uMAF1.misc.Node;

import java.util.*;

public class Duals {

    boolean first = true;
    int TECHNIQUE = 2;  // 0 is no constraints     1 is decreasing internal nodes   2 is decreasing internal nodes + maximising minimum
    private final List<Node> leaves;
    final private List<Integer> internal1;
    final private List<Integer> internal2;
    public Graph<Node, DefaultEdge> tree1;
    public Graph<Node, DefaultEdge> tree2;
    public IloCplex cplex;
    public int size;

    IloNumVarArray var;
    IloRangeArray rng;
    IloObjective objective;

    public Duals(Graph<Node, DefaultEdge> tree1, Graph<Node, DefaultEdge> tree2, List<Leafset> leafSets, List<Node> leaves, List<Integer> internal1, List<Integer> internal2){
        this.tree1 = tree1;
        this.tree2 = tree2;
        this.leaves = leaves;
        this.internal1 = internal1;
        this.internal2 = internal2;
        this.size = 0;
        for(Integer i:internal1){
            if(size<i){
                size = i;
            }
        }
        for(Integer i:internal2){
            if(size<i){
                size = i;
            }
        }
        writeInitialDual(leafSets);
    }

    private void writeInitialDual(List<Leafset> leafSets) {
        try {
            cplex = new IloCplex();
            cplex.setOut(null);
            cplex.setParam(IloCplex.Param.RandomSeed, 123);
            var = new IloNumVarArray();
            rng = new IloRangeArray();
            populate(leafSets);
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }
    public void populate(List<Leafset> leafSets) throws IloException {

        int numVars = leaves.size() + internal1.size() + internal2.size() + 1;

        if(TECHNIQUE==0) {
            for (Node leaf : leaves) {
                var.add(cplex.numVar(0, 1, leaf.toString()));
            }
        }else if(TECHNIQUE==1 || TECHNIQUE==2){
            for (Node leaf : leaves) {
                var.add(cplex.numVar(1, 1, leaf.toString()));
            }
        }
        for (int internalNode : internal1) {
            var.add(cplex.numVar(-Double.MAX_VALUE, 0, STR."\{internalNode}"));
        }
        for (int internalNode : internal2) {
            var.add(cplex.numVar(-Double.MAX_VALUE, 0, STR."\{internalNode}"));
        }

        double[] objvals = new double[numVars];
        Arrays.fill(objvals, 1000.0);
        objective = cplex.addMaximize(cplex.scalProd(var.getArray(), objvals));

        // add constraints so leaves are in exactly one set
        for (Leafset leafset : leafSets) {
            IloLinearNumExpr leafConstraint =  addConstraint(leafset);
            rng.add(cplex.addLe(leafConstraint, 1.0, leafset.toString()));
        }
    }

    private IloLinearNumExpr addConstraint(Leafset leafset) throws IloException {
        int i=0;
        IloLinearNumExpr leafConstraint = cplex.linearNumExpr();
        for (Node leaf : leaves) {
            if (leafset.contains(leaf)) {
                leafConstraint.addTerm(1.0, var.getElement(i));
            }
            i++;
        }
        for (int internalNode : internal1) {
            if (leafset.has_internal(internalNode, tree1, 1)) {
                leafConstraint.addTerm(1.0, var.getElement(i));
            }
            i++;
        }
        for (int internalNode : internal2) {
            if (leafset.has_internal(internalNode, tree2, 2)) {
                leafConstraint.addTerm(1.0, var.getElement(i));
            }
            i++;
        }
        return leafConstraint;
    }


    /**
     * Get duals based on solved cplex
     *
     * @return
     * @throws IloException
     */
    public double[] extractDuals() {
        if(first){
            first = false;
            return initialDuals();
        }
        double[] dualsMAP = new double[size+1];
        try{
            cplex.solve();
            for (int i=0;i< var._num;i++){
                IloNumVar v = var.getElement(i);
                if(!v.getName().contains("max")) {
                    int pos = Integer.parseInt(v.getName());
                    dualsMAP[pos]= cplex.getValue(v);
                    //System.out.println(v.getName()+" "+cplex.getValue(v));
                }
            }

        } catch (IloException e) {
            throw new RuntimeException(e);
        }
        return dualsMAP;
    }

    /**
     * THIS DOES NOT WORK AND THE DUALS RETURNED BY cplex.getDuals ARE INFEASIBLE
     * @param masterData
     * @return
     */
    public double[] extractDuals(MAFData masterData) {
        if(first){
            first = false;
            return initialDuals();
        }
        double[] dualsMAP = new double[size+1];
        try{
            masterData.cplex.setParam(IloCplex.IntParam.RootAlg,IloCplex.Algorithm.Dual);
            masterData.cplex.solve();
            System.out.println(masterData.cplex.isMIP());
            System.out.println(masterData.cplex.isDualFeasible());
            System.out.println(Arrays.toString(masterData.cplex.getDuals(masterData.rng)));
            for(IloRange range : masterData.rng) {
                int pos = parseStringToInt(range.getName());
                dualsMAP[pos]= masterData.cplex.getDual(range);
            }

        } catch (IloException e) {
            throw new RuntimeException(e);
        }
        return dualsMAP;
    }

    public static int parseStringToInt(String str) {
        // Check if the string is purely an integer
        if (str.matches("\\d+")) {
            return Integer.parseInt(str);
        }

        // If not, extract the digits
        String number = str.replaceAll("\\D+", "");

        // If there are no digits in the string, throw an exception
        if (number.isEmpty()) {
            throw new NumberFormatException("No digits found in the string");
        }

        // Convert the string of digits to an integer
        return Integer.parseInt(number);
    }


    /**
     * Adds new constraint to dual and solves
     * @param newLeafSet
     * @return
     */
    public void addLeafset(Leafset newLeafSet) {
        try {
            IloLinearNumExpr leafConstraint =  addConstraint(newLeafSet);
            rng.add(cplex.addLe(leafConstraint, 1.0, newLeafSet.toString()));
            // add constraints to that 'max' is the max of leaves or max of -internal
            IloColumn column = cplex.column(objective,  -1.0 /newLeafSet.leaves.size()*newLeafSet.leaves.size());
            if(TECHNIQUE==2&&newLeafSet.leaves.size()>1){
                var.add(cplex.numVar(column, -Double.MAX_VALUE, 1, STR."max\{newLeafSet.toString()}"));
                int i = leaves.size();

                for (int internaln1 : internal1) {
                    if (newLeafSet.has_internal(internaln1, tree1, 1)) {
                        IloLinearNumExpr maxConstraint = cplex.linearNumExpr();
                        maxConstraint.addTerm(-1.0, var.getElement(var._num - 1));
                        maxConstraint.addTerm(-1.0, var.getElement(i));
                        rng.add(cplex.addLe(maxConstraint, 0.0, STR."max_constraint\{internaln1}"));
                    }
                    i++;
                }
                //var.add(cplex.numVar(column, -Double.MAX_VALUE, 1, STR."max\{newLeafSet.toString()}2"));
                for (int internaln1 : internal2) {
                    if (newLeafSet.has_internal(internaln1, tree2, 2)) {
                        IloLinearNumExpr maxConstraint = cplex.linearNumExpr();
                        maxConstraint.addTerm(-1.0, var.getElement(var._num - 1));
                        maxConstraint.addTerm(-1.0, var.getElement(i));
                        rng.add(cplex.addLe(maxConstraint, 0.0, STR."max_constraint\{internaln1}"));

                    }
                    i++;
                }
                /**
                for (int internaln1 : internal1) {
                    int j = leaves.size();
                    for (int internaln2 : internal1) {
                        if (newLeafSet.has_internal(internaln1, tree1, 1) && newLeafSet.has_internal(internaln2, tree1, 1) &&internaln1!=internaln2) {
                            IloLinearNumExpr maxConstraint = cplex.linearNumExpr();
                            maxConstraint.addTerm(-1.0, var.getElement(var._num - 1));
                            maxConstraint.addTerm(-1.0, var.getElement(i));
                            maxConstraint.addTerm(1.0, var.getElement(j));
                            rng.add(cplex.addLe(maxConstraint, 0.0, STR."max_constraint\{internaln1}, \{internaln2}"));
                        }
                        j++;
                    }
                    i++;

                }
                for (int internaln1 : internal2) {
                    int j = leaves.size() + internal1.size();
                    for (int internaln2 : internal2) {
                        if (newLeafSet.has_internal(internaln1, tree2, 2) && newLeafSet.has_internal(internaln2, tree2, 2) &&internaln1!=internaln2) {
                            IloLinearNumExpr maxConstraint = cplex.linearNumExpr();
                            maxConstraint.addTerm(-1.0, var.getElement(var._num - 1));
                            maxConstraint.addTerm(-1.0, var.getElement(i));
                            maxConstraint.addTerm(1.0, var.getElement(j));
                            rng.add(cplex.addLe(maxConstraint, 0.0, STR."max_constraint\{internaln1}, \{internaln2}"));
                        }
                        j++;
                    }
                    i++;

                }
**/
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }




    /**
     * Get initial duals without need for solving
     * @return
     */
    public double[] initialDuals() {

        double[] dualsMAP = new double[size+1];
        for(Node n:leaves){
            int pos = n.id;
            dualsMAP[pos]= 1;
        }
        for(int n: internal1){
            dualsMAP[n]= 0;
        }
        for(int n: internal2){
            dualsMAP[n]= 0;
        }
        return dualsMAP;
    }


}

