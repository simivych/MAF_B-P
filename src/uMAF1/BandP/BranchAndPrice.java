package uMAF1.BandP;


import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchAndPrice;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchCreator;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.BAPNode;
import org.jorlib.frameworks.columnGeneration.master.AbstractMaster;
import org.jorlib.frameworks.columnGeneration.master.MasterData;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;
import uMAF1.colgen.Leafset;
import uMAF1.colgen.MAST;
import uMAF1.model.MAF;

import java.util.*;

/**
 * Branch-and-Price implementation
 * @author Joris Kinable
 * @version 29-6-2016
 */
public final class BranchAndPrice extends AbstractBranchAndPrice<MAF, Leafset, MAST> {

    public BranchAndPrice(MAF dataModel,
                          AbstractMaster<MAF, Leafset, MAST, ? extends MasterData> master,
                          MAST pricingProblem,
                          List<Class<? extends AbstractPricingProblemSolver<MAF, Leafset, MAST>>> solvers,
                          List<? extends AbstractBranchCreator<MAF, Leafset, MAST>> abstractBranchCreators,
                          double lowerBoundOnObjective,
                          double upperBoundOnObjective) {
        super(dataModel, master, pricingProblem, solvers, abstractBranchCreators, lowerBoundOnObjective, upperBoundOnObjective);
    }

    /**
     * Generates an artificial solution. Columns in the artificial solution are of high cost such that they never end up in the final solution
     * if a feasible solution exists, since any feasible solution is assumed to be cheaper than the artificial solution. The artificial solution is used
     * to guarantee that the master problem has a feasible solution.
     *
     * @return artificial solution
     */
    @Override
    protected List<Leafset> generateInitialFeasibleSolution(BAPNode<MAF, Leafset> node) {
        return new ArrayList<>();
    }

    @Override
    public List<Leafset> getSolution() {
        return master.getSolution();
    }

    /**
     * Checks whether the given node is integer
     * @param node Node in the Branch-and-Price tree
     * @return true if the solution is an integer solution
     */
    @Override
    protected boolean isIntegerNode(BAPNode<MAF, Leafset> node) {
        for(Leafset var : node.getSolution()) {
            if (var.value != 0.0 && var.value != 1.0) {
                return false;
            }
        }
        return true;
    }
}

