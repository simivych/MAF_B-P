package uMAF1.BandP.branching;

import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchCreator;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.BAPNode;

import java.util.*;


import uMAF1.colgen.Leafset;
import uMAF1.colgen.MAST;
import uMAF1.misc.Node;
import uMAF1.model.MAF;


/**
 * Class which creates new branches in the Branch-and-Price tree. This particular class branches on a pair of vertices, thereby creating
 * two branches. In one branch, these vertices receive the same color, whereas in the other branch they are colored differently
 * @author Joris Kinable
 * @version 29-6-2016
 */
public final class BranchOnSizeInternalRatio extends AbstractBranchCreator<MAF, Leafset, MAST> {

    /** Pair of vertices to branch on **/
    Leafset candidateLeafset =null;

    public BranchOnSizeInternalRatio(MAF dataModel, MAST pricingProblem) {
        super(dataModel, pricingProblem);
    }

    /**
     * Determine on which edge from the red or blue matchings we are going to branch.
     * @param solution Fractional column generation solution
     * @return true if a fractional edge exists
     */
    @Override
    protected boolean canPerformBranching(List<Leafset> solution) {
        //System.out.print("BRANCHING   ");
        boolean foundPair=false;
        double max_ratio = 0;
        Set<Node> unique = new HashSet<>();
        Set<Node> duplicates = new HashSet<>();
        for(Leafset ls:solution){
            for(Node leaf:ls.leaves) {
                if (!unique.add(leaf)) {
                    duplicates.add(leaf);
                }
            }
        }
        for(Leafset ls:solution){
            if(!Collections.disjoint(ls.leaves, duplicates)){
                double ratio = ((double) ls.leaves.size()/(double)ls.subgraphNodes1.size()+(double)ls.subgraphNodes2.size());
                if(ratio>max_ratio){
                    foundPair = true;
                    candidateLeafset = ls;
                    max_ratio = ls.leaves.size();

                }
            }
        }
        //System.out.println(candidateLeafset);
        //System.out.println();
        return foundPair;
    }

    /**
     * Create the branches:
     * <ol>
     * <li>branch 1: pair of vertices {@code vertexPair} must be assigned the same color,</li>
     * <li>branch 2: pair of vertices {@code vertexPair} must be assigned different colors,</li>
     * </ol>
     * @param parentNode Fractional node on which we branch
     * @return List of child nodes
     */
    @Override
    protected List<BAPNode<MAF, Leafset>> getBranches(BAPNode<MAF, Leafset> parentNode) {
        //Branch 1: used
        Used branchingDecision1=new Used(candidateLeafset);
        BAPNode<MAF,Leafset> node2=this.createBranch(parentNode, branchingDecision1, dataModel.leafSets, parentNode.getInequalities());

        //Branch 2: not used
        NotUsed branchingDecision2=new NotUsed(candidateLeafset);
        BAPNode<MAF,Leafset> node1=this.createBranch(parentNode, branchingDecision2, dataModel.leafSets, parentNode.getInequalities());

        return Arrays.asList(node1, node2);
    }
}
