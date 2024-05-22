package uMAF1;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchCreator;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;

import uMAF1.BandP.BranchAndPrice;
import uMAF1.BandP.branching.BranchOnSize;
import uMAF1.colgen.Leafset;
import uMAF1.colgen.MAST;
import uMAF1.colgen.MASTSolver;
import uMAF1.colgen.Master;
import uMAF1.misc.Node;
import uMAF1.model.*;

import java.io.File;
import java.util.*;
import static uMAF1.misc.GraphMethods.remove_root;
import static uMAF1.misc.LoadFiles.get_trees;

public class uMAFSolver {
    private final MAF dataModel;

    public uMAFSolver(MAF dataModel){

        this.dataModel = dataModel;

        //Create the pricing problem
        MAST mast=new MAST(dataModel, "Maximimum Agreement Forest");


        //Create the master problem
        Master master=new Master(dataModel, mast);

        //Define which solvers to use
        List<Class<? extends AbstractPricingProblemSolver<MAF, Leafset, MAST>>> solvers= Collections.singletonList(MASTSolver.class);

        int upperBound = dataModel.leaves.size();

        //Create a set of initial columns.
        List<Leafset> initSolution=this.getInitialSolution(mast);

        //Lower bound on column generation solution (stronger is better): calculate least amount of finals needed to fulfil the order (ceil(\sum_j d_j*w_j /L)
        int lowerBound = 1;

        //Define Branch creators
        List<? extends AbstractBranchCreator<MAF, Leafset, MAST>> branchCreators = Collections.singletonList(new BranchOnSize(dataModel, mast));

        //Create a Branch-and-Price instance, and provide the initial solution as a warm-start
        BranchAndPrice bap = new BranchAndPrice(dataModel, master, mast, solvers, branchCreators, lowerBound, upperBound);
        bap.warmStart(upperBound, initSolution);

        //Solve the Graph Coloring problem through Branch-and-Price
        bap.runBranchAndPrice(System.currentTimeMillis() + 10000000000L);

        //Print solution:
        System.out.println("================ Solution ================");
        System.out.println("BAP terminated with objective: " + (bap.getObjective()));
        System.out.println("Total Number of Columns Generated: " + bap.getTotalNrIterations());
        System.out.println("Total Number of branching nodes: " + bap.getNumberOfProcessedNodes());
        System.out.println("Total solve time (ms): " + bap.getSolveTime());
        System.out.println("Total Time spent on master problems: " + bap.getMasterSolveTime() + " Total time spent on pricing problems: " + bap.getPricingSolveTime());
        System.out.println("Solution is optimal: " + bap.isOptimal());
        System.out.println("Final Leaf sets");
        System.out.println((bap.getObjective())+" "+ bap.getTotalNrIterations()+" "+ bap.getNumberOfProcessedNodes()+" "+ bap.getSolveTime() );

        List<Leafset> solution = bap.getSolution();
        for (Leafset column : solution)
            System.out.println(column);

        //Clean up:
        bap.close(); //This closes both the master and pricing problems
    }

    /**
     * Create an initial solution for the Cutting Stock Problem.
     * Simple initial solution: cut each final from its own raw/roll.
     * @param mast pricing problem
     * @return Initial solution
     */
    private List<Leafset> getInitialSolution(MAST mast){
        List<Leafset> initSolution=new ArrayList<>();
        for(Node leaf : dataModel.leaves){
            Set<Node> leafNode = new HashSet<>();
            leafNode.add(leaf);
            Leafset column = new Leafset("initSolution", false, leafNode, mast);
            initSolution.add(column);
        }
        return initSolution;
    }

    public static void main(String[] args){
        run_file("data/maindataset/TREEPAIR_150_35_90_04.tree");
        //run_folder("data/maindataset");
    }

    public static void run_file(String file){
        MAF maf = new MAF();
        Graph<Node, DefaultEdge>[] trees = get_trees(file);
        Graph<Node, DefaultEdge> tree1 = remove_root(trees[0]);
        Graph<Node, DefaultEdge> tree2 = remove_root(trees[1]);
        maf.set_vars(tree1,tree2);
        new uMAFSolver(maf);
    }

    public static void run_folder(String folder){
        File directory = new File(folder);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            for (File file : files) {
                System.out.println();
                System.out.println(file.getPath());
                run_file(file.getPath());
            }
        }
    }
}
