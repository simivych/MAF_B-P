package uMAF;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import static uMAF.GraphMethods.*;
import static uMAF.LoadFiles.get_trees;

public class Main {

    public static void main(String[] args){
        Graph<TreeNode, DefaultEdge>[] trees = get_trees("data/maindataset/TREEPAIR_50_5_50_01.tree");
        Graph<TreeNode, DefaultEdge> tree1 = remove_root(trees[0]);
        Graph<TreeNode, DefaultEdge> tree2 = remove_root(trees[1]);
        BandP bp = new BandP(tree1, tree2);
        System.out.println(STR."Final solution \{bp.solution}");
        }

}
