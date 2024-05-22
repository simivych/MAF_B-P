package uMAF1.BandP.branching;

import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.AbstractInequality;
import uMAF1.colgen.Leafset;
import uMAF1.model.MAF;

import java.util.Collections;

/**
 * Ensure that two vertices are colored differently
 * @author Joris Kinable
 * @version 29-6-2016
 */
public final class Used implements BranchingDecision<MAF, Leafset> {

    /** Vertices to be colored differently **/
    public final Leafset leafset;

    public Used(Leafset leafset){
        this.leafset=leafset;
    }

    /**
     * Determine whether the given column remains feasible for the child node
     * @param column column
     * @return true if the column is compliant with the branching decision
     */
    @Override
    public boolean columnIsCompatibleWithBranchingDecision(Leafset column) {
        if(column.equals(leafset)){
            return true;
        }
        return (Collections.disjoint(column.leaves, leafset.leaves));
    }

    /**
     * Determine whether the given inequality remains feasible for the child node
     * @param inequality inequality
     * @return true
     */
    @Override
    public boolean inEqualityIsCompatibleWithBranchingDecision(AbstractInequality inequality) {
        return true; //Cuts are no in this example
    }

    @Override
    public String toString(){
        return "Used "+leafset;
    }
}
