package uMAF1.colgen;

import ilog.concert.*;
import ilog.cplex.IloCplex;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.master.AbstractMaster;
import org.jorlib.frameworks.columnGeneration.master.OptimizationSense;
import org.jorlib.frameworks.columnGeneration.util.OrderedBiMap;
import uMAF1.misc.IloNumVarArray;
import uMAF1.misc.Node;
import uMAF1.model.MAF;

import java.util.*;

public final class Master extends AbstractMaster<MAF, Leafset, MAST, MAFData> {

    IloCplex cplex; //Cplex instance
    private IloObjective obj; //Objective function
    private IloRange[] rng; //Constraint
    private IloNumVarArray var;
    Duals duals;

    public Master(MAF dataModel, MAST pricingProblem) {
        super(dataModel, pricingProblem, OptimizationSense.MINIMIZE);
        duals = new Duals(dataModel.tree1, dataModel.tree2, dataModel.leafSets, dataModel.leaves, dataModel.internal1, dataModel.internal2);
    }

    /**
     * Build the cplex problem
     */
    @Override
    protected MAFData buildModel() {
        try {
            cplex =new IloCplex(); //Create cplex instance
            cplex.setOut(null); //Disable cplex output
            cplex.setParam(IloCplex.IntParam.Threads, config.MAXTHREADS); //Set number of threads that may be used by the cplex

            //Define the objective
            obj= cplex.addMinimize();
            rng = new IloRange[dataModel.leaves.size()+dataModel.internal1.size()+dataModel.internal2.size()];
            var = new IloNumVarArray();
            int i = 0;

            // Add RHS of each constraint
            for (Node leaf : dataModel.leaves) {
                rng[i++] = cplex.addRange(1.0, Double.MAX_VALUE, leaf.name);
            }
            for (int internalNode : dataModel.internal1) {
                rng[i++] = cplex.addRange(-Double.MAX_VALUE, 1.0, STR."internal\{internalNode}");
            }
            for (int internalNode : dataModel.internal2) {
                rng[i++] = cplex.addRange(-Double.MAX_VALUE, 1.0, STR."internal\{internalNode}");
            }
        } catch (IloException e) {
            e.printStackTrace();
        }

        //Define a container for the variables
        Map<MAST,OrderedBiMap<Leafset, IloNumVar>> varMap=new LinkedHashMap<>();
        varMap.put(pricingProblems.get(0),new OrderedBiMap<>());

        //Return a new data object which will hold data from the Master Problem. Since we are not working with inequalities in this example,
        //we can simply return the default.
        return new MAFData(varMap);
    }

    /**
     * Solve the cplex problem and return whether it was solved to optimality
     */
    @Override
    protected boolean solveMasterProblem(long timeLimit) throws TimeLimitExceededException {
        try {
            //Set time limit
            double timeRemaining=Math.max(1,(timeLimit-System.currentTimeMillis())/1000.0);
            cplex.setParam(IloCplex.DoubleParam.TiLim, timeRemaining); //set time limit in seconds
            //Potentially export the model
            if(config.EXPORT_MODEL) cplex.exportModel(config.EXPORT_MASTER_DIR+"master_"+this.getIterationCount()+".lp");

            //Solve the model
            if(!cplex.solve() || cplex.getStatus()!=IloCplex.Status.Optimal){
                if(cplex.getCplexStatus()==IloCplex.CplexStatus.AbortTimeLim) //Aborted due to time limit
                    throw new TimeLimitExceededException();
                else
                    throw new RuntimeException("Master problem solve failed! Status: "+ cplex.getStatus());
            }else{
                masterData.objectiveValue= cplex.getObjValue();
            }
        } catch (IloException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Store the dual information required by the pricing problems into the pricing problem object
     */
    @Override
    public void initializePricingProblem(MAST pricingProblem){
        pricingProblem.initPricingProblem(duals.extractDuals());
    }

    public IloColumn getColumn(Leafset leafSet) throws IloException {
        IloColumn column = cplex.column(obj, 1.0);
        if(leafSet==null){
            return column;
        }
        int i = 0;
        for (Node leaf : dataModel.leaves) {
            if(leafSet.contains(leaf)){
                //System.out.println("Contains "+leaf.name);
                column = column.and(cplex.column(rng[i++], 1.0));
            }else{
                column = column.and(cplex.column(rng[i++], 0.0));
            }
        }
        for (int internalNode : dataModel.internal1) {
            if(leafSet.has_internal(internalNode, dataModel.tree1)){
                //System.out.println("Contains internal"+internalNode);
                column = column.and(cplex.column(rng[i++], 1.0));
            }else{
                //System.out.println("Doesnt contain internal"+internalNode);
                column = column.and(cplex.column(rng[i++], 0.0));
            }
        }
        for (int internalNode : dataModel.internal2) {
            if(leafSet.has_internal(internalNode, dataModel.tree2)){
                //System.out.println("Contains internal"+internalNode);
                column = column.and(cplex.column(rng[i++], 1.0));
            }else{
                //System.out.println("Doesnt contain internal"+internalNode);
                column = column.and(cplex.column(rng[i++], 0.0));
            }
        }
        return column;
    }

    /**
     * Function which adds a new column to the cplex problem
     */
    public void addColumn(Leafset leafset) {
        try {
            IloColumn column = getColumn(leafset);
            duals.addLeafset(leafset);
            //Create the variable and store it
            IloNumVar variable= cplex.numVar(column, 0.0,1.0, IloNumVarType.Float, leafset.leaves.toString());
            var.add(variable);
            cplex.add(variable);
            masterData.addColumn(leafset, variable);
            dataModel.leafSets.add(leafset);
        } catch (IloException e) {
            e.printStackTrace();
        }
    }
    /**
     * Return the solution, i.e columns with non-zero values in the cplex problem
     */
    @Override
    public List<Leafset> getSolution() {
        List<Leafset> solution=new ArrayList<>();
        try {
            for(IloNumVar variable:var.getArray()) {
                if (cplex.getValue(variable) != 0) {
                    for (Leafset leafset : dataModel.leafSets) {
                        if (variable.getName().equals(leafset.leaves.toString())) {
                            solution.add(leafset);
                            break;
                        }
                    }

                }
            }
        } catch (IloCplex.UnknownObjectException e) {
            throw new RuntimeException(e);
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
        return solution;
    }

    /**
     * Close the cplex problem
     */
    @Override
    public void close() {
        cplex.end();
    }

    /**
     * Print the solution if desired
     */
    @Override
    public void printSolution() {
        System.out.println("Master solution:");
        for(Leafset cp : this.getSolution())
            System.out.println(cp);
    }

    /**
     * Export the model to a file
     */
    @Override
    public void exportModel(String fileName){
        try {
            cplex.exportModel(config.EXPORT_MASTER_DIR + fileName);
        } catch (IloException e) {
            e.printStackTrace();
        }
    }

}
