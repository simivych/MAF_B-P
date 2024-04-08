package uMAF;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import static uMAF.GraphMethods.*;
import static uMAF.LoadFiles.get_trees;

public class Main {

    public static void main(String[] args){
        Graph<Node, DefaultEdge>[] trees = get_trees("data/largetreedataset/TREEPAIR_500_35_50_02.tree");
        Graph<Node, DefaultEdge> tree1 = remove_root(trees[0]);
        Graph<Node, DefaultEdge> tree2 = remove_root(trees[1]);
        BandP bp = new BandP(tree1, tree2);
        System.out.println(STR."Final solution \{bp.solution}");
        }

}
