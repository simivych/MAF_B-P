package uMAF1.BandP.branching;

import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.AbstractInequality;
import uMAF1.colgen.Leafset;
import uMAF1.model.MAF;

/**
 * Ensure that two vertices are colored differently
 * @author Joris Kinable
 * @version 29-6-2016
 */
public final class NotUsed implements BranchingDecision<MAF, Leafset> {

    /** Vertices to be colored differently **/
    public final Leafset leafset;

    public NotUsed(Leafset leafset){
        this.leafset=leafset;
    }

    /**
     * Determine whether the given column remains feasible for the child node
     * @param column column
     * @return true if the column is compliant with the branching decision
     */
    @Override
    public boolean columnIsCompatibleWithBranchingDecision(Leafset column) {
        // if this column isn't used it doesn't prevent any other leafsets to be used
        return !column.equals(leafset);
    }

    /**
     * Determine whether the given inequality remains feasible for the child node
     * @param inequality inequality
     * @return true
     */
    @Override
    public boolean inEqualityIsCompatibleWithBranchingDecision(AbstractInequality inequality) {
        return true; //Cuts are not added in this example
    }

    @Override
    public String toString(){
        return "NotUsed "+leafset;
    }
}
