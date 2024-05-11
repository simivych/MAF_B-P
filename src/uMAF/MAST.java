package uMAF;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.shortestpath.BFSShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.*;

public class MAST {
    Graph<TreeNode, DefaultEdge> tree1;
    Graph<TreeNode, DefaultEdge> tree2;
    Map<String, Double> duals;
    Map<Integer, Subtree> lookupTable = new HashMap<>();
    int used_hashed = 0;
    public MAST(Graph<TreeNode, DefaultEdge> tree1, Graph<TreeNode, DefaultEdge> tree2, Map<String, Double> duals){
        this.tree1 = tree1;
        this.tree2 = tree2;
        this.duals = duals;
    }

    /**
     * Method call to get MAST
     * Does initial spilts into rooted trees and calls solveMAST
     * @return Leafset
     */
    public LeafSet getMAST(){
        // initial split into rooted subtrees
        double max_size = -Double.MAX_VALUE;
        Subtree MAST = null;
        for(DefaultEdge e1: tree1.edgeSet()){
            Graph<TreeNode, DefaultEdge>[] p = get_e_subtrees(tree1, e1);
            Graph<TreeNode, DefaultEdge> p1 = p[0];
            Graph<TreeNode, DefaultEdge> p2 = p[1];
            for(DefaultEdge e2: tree2.edgeSet()){
                Graph<TreeNode, DefaultEdge>[] q = get_e_subtrees(tree2, e2);
                Graph<TreeNode, DefaultEdge> q1 = q[0];
                Graph<TreeNode, DefaultEdge> q2 = q[1];

                Subtree subtree1 = solveMAST(p1,q1);
                Subtree subtree2 = solveMAST(p2,q2);
                Subtree subtree12 = this.new Subtree(subtree1,subtree2);

                Subtree subtree3 = solveMAST(p2,q1);
                Subtree subtree4 = solveMAST(p1,q2);
                Subtree subtree34 = this.new Subtree(subtree3,subtree4);

                if(subtree12.weightedSize>max_size){
                    MAST = subtree12;
                    max_size = subtree12.weightedSize;
                }
                if(subtree34.weightedSize>max_size){
                    MAST = subtree34;
                    max_size = subtree34.weightedSize;
                }
            }
        }
        if(MAST==null || max_size <= 1 ){
            return null;
        }
        return new LeafSet(tree1, tree2, MAST.leaves, MAST.subgraphNodes1, MAST.subgraphNodes2);
    }

    /**
     * Solves MAST for rooted subtrees
     * @param p
     * @param q
     * @return
     */
    public Subtree solveMAST(Graph<TreeNode, DefaultEdge> p, Graph<TreeNode, DefaultEdge> q){
        // If it has already been calculated return previous result
        long hash = get_hash_value(p,q);
        Subtree hased_subtree = get_hash(hash);
        if (hased_subtree!=null){
            used_hashed++;
            return hased_subtree;
        }
        // if one of the trees is a single node return the intersection
        if (p.vertexSet().size() <= 3 || q.vertexSet().size() <= 3) {
            Set<TreeNode> intersectionSet = new HashSet<>(p.vertexSet());
            intersectionSet.retainAll(q.vertexSet());
            Subtree subtree = this.new Subtree(intersectionSet);
            set_hash(hash, subtree);
            return subtree;
        }
        // Calculate the MAST recursively
        Graph<TreeNode, DefaultEdge>[] p_sub = get_e_subtrees(p);
        Graph<TreeNode, DefaultEdge>[] q_sub = get_e_subtrees(q);

        double max_weight = -Double.MAX_VALUE;
        Subtree max_subtree = null;

        for(Graph<TreeNode, DefaultEdge> q_prime: q_sub){
            Subtree p_qprime = solveMAST(p, q_prime);
            if(max_weight<p_qprime.weightedSize){
                max_weight = p_qprime.weightedSize;
                max_subtree = p_qprime;
            }

        }
        for(Graph<TreeNode, DefaultEdge> p_prime: p_sub){
            Subtree pprime_q = solveMAST(p_prime, q);
            if(max_weight<pprime_q.weightedSize){
                max_weight = pprime_q.weightedSize;
                max_subtree = pprime_q;
            }
        }
        // Matching 1
        Subtree p0q0 = solveMAST(p_sub[0], q_sub[0]);
        Subtree p1q1 = solveMAST(p_sub[1], q_sub[1]);
        Subtree matching1 = new Subtree(p0q0,p1q1);
        if(max_weight<matching1.weightedSize){
            max_weight = matching1.weightedSize;
            max_subtree = matching1;
        }

        // Matching 2
        Subtree p0q1 = solveMAST(p_sub[0], q_sub[1]);
        Subtree p1q0 = solveMAST(p_sub[1], q_sub[0]);
        Subtree matching2 = new Subtree(p0q1, p1q0);
        if(max_weight<matching2.weightedSize){
            max_weight = matching2.weightedSize;
            max_subtree = matching2;
        }

        // save the result in the hash table
        set_hash(hash, max_subtree);
        return max_subtree;
    }

    private Graph<TreeNode, DefaultEdge>[] get_e_subtrees(Graph<TreeNode, DefaultEdge> tree, DefaultEdge edge) {
        TreeNode root1 = tree.getEdgeSource(edge);
        TreeNode root2 = tree.getEdgeTarget(edge);
        Graph<TreeNode, DefaultEdge> e_subtrees1 = dfs(tree, root1, edge);
        Graph<TreeNode, DefaultEdge> e_subtrees2 = dfs(tree, root2, edge);
        Graph<TreeNode, DefaultEdge>[] e_subtrees = new Graph[]{e_subtrees1, e_subtrees2};
        return e_subtrees;
    }

    private Graph<TreeNode, DefaultEdge>[] get_e_subtrees(Graph<TreeNode, DefaultEdge> tree) {
        TreeNode root = get_root(tree);
        Graph<TreeNode, DefaultEdge>[] e_subtrees = new Graph[2];
        int i = 0;
        for(DefaultEdge edge: tree.edgesOf(root)){
            TreeNode n = Graphs.getOppositeVertex(tree, edge, root);
            e_subtrees[i] = dfs(tree, n, edge);
            i++;
        }
        return e_subtrees;
    }

    private Graph<TreeNode, DefaultEdge> dfs(Graph<TreeNode, DefaultEdge> tree, TreeNode root, DefaultEdge removing_edge) {
        Stack<TreeNode> stack = new Stack<>();
        stack.push(root);
        DefaultUndirectedGraph<TreeNode, DefaultEdge> e_subtree = new DefaultUndirectedGraph<>(DefaultEdge.class);
        e_subtree.addVertex(root);

        while (!stack.isEmpty()) {
            TreeNode parent = stack.pop();
            for (DefaultEdge edge : tree.edgesOf(parent)) {
                if(!edge.equals(removing_edge)){
                    TreeNode child = Graphs.getOppositeVertex(tree, edge, parent);
                    if(!e_subtree.containsVertex(child)) {
                        e_subtree.addVertex(child);
                        e_subtree.addEdge(parent, child);
                        stack.add(child);
                    }
                }
            }
        }
        return e_subtree;
    }
    public TreeNode get_root(Graph<TreeNode, DefaultEdge> tree){
        if(tree.vertexSet().size()==1){
            return tree.vertexSet().iterator().next();
        }
        for(TreeNode n: tree.vertexSet()){
            if(tree.degreeOf(n)==2){
                return n;
            }
        }
        return null;
    }

    public TreeNode[] get_root_and_children(Graph<TreeNode, DefaultEdge> tree){
        if(tree.vertexSet().size()==1){
            return new TreeNode[]{tree.vertexSet().iterator().next()};
        }
        for(TreeNode n: tree.vertexSet()){
            if(tree.degreeOf(n)==2){
                TreeNode[] root_children = new TreeNode[3];
                root_children[0] = n;
                int i = 1;
                for (DefaultEdge edge : tree.edgesOf(n)) {
                    TreeNode child = Graphs.getOppositeVertex(tree, edge, n);
                    root_children[i]=child;
                    i++;
                }
                return root_children;
            }
        }
        return null;
    }

    public long get_hash_value(Graph<TreeNode, DefaultEdge> tree1, Graph<TreeNode, DefaultEdge> tree2) {
        TreeNode[] tree1_root_children = get_root_and_children(tree1);
        TreeNode[] tree2_root_children = get_root_and_children(tree2);
        long tree1_hash;
        if(tree1_root_children.length>1){
            tree1_hash = Szudzik(tree1_root_children[0].id, Szudzik(tree1_root_children[1].id, tree1_root_children[2].id));
        }else{
            tree1_hash = Szudzik(tree1_root_children[0].id, 0);
        }
        long tree2_hash;
        if(tree2_root_children.length>1){
            tree2_hash = Szudzik(tree2_root_children[0].id, Szudzik(tree2_root_children[1].id, tree2_root_children[2].id));
        }else{
            tree2_hash = Szudzik(tree2_root_children[0].id, 0);
        }
        long l = Szudzik(tree1_hash, tree2_hash);
        return l;
    }

    public long Szudzik(long v1, long v2){
        // Using Szudzik pairing function
        long hash;
        if(v1 < v2){
            hash = v1 + v2*v2;
        }else{
            hash = v1*v1 + v1 + v2;
        }
        return hash;
    }

    public Subtree get_hash(long hash){
        if(lookupTable.containsKey((int) hash)){
            return lookupTable.get((int) hash);
        }
        return null;
    }
    public void set_hash(long hash, Subtree subtree){
        lookupTable.put((int) hash, subtree);

    }

    /**
     * A class that will store the leaf set and its weight of a partial MAST
      */

    public class Subtree {
        public Set<TreeNode> subgraphNodes1 = new HashSet<>();
        public Set<TreeNode> subgraphNodes2 = new HashSet<>();
        public double weightedSize = -Double.MAX_VALUE;
        public final Set<TreeNode> leaves;

        public Subtree(Set<TreeNode> leaves) {
            this.leaves = leaves;
            getWeightedSize();
        }


        public Subtree(Subtree subtree1, Subtree subtree2) {
             Set<TreeNode> all = new HashSet<>(subtree1.leaves);
             all.addAll(subtree2.leaves);
             this.leaves = all;
             getWeightedSize();
        }

        private void getWeightedSize(){
            weightedSize = 0;
            if(leaves.size()<=1){
                for(TreeNode n: leaves){
                    weightedSize += duals.get(n.name);
                }
            }else {
                subgraphNodes1= getInternalNodes(tree1);
                subgraphNodes2 = getInternalNodes(tree2);
                for (TreeNode n : subgraphNodes1) {
                    weightedSize += duals.get(n.name);
                }
                for (TreeNode n : subgraphNodes2) {
                    weightedSize += duals.get(n.name);
                }
                for (TreeNode n : leaves) {
                    weightedSize += duals.get(n.name);
                }
            }
        }

        public Set<TreeNode> getInternalNodes(Graph<TreeNode, DefaultEdge> tree){
            Set<TreeNode> internal = new HashSet<>();
            List<TreeNode> leafNodesList = new ArrayList<>(leaves);
            TreeNode node1 = leafNodesList.getFirst();

            for (TreeNode node : leaves) {
                if(!node.equals(node1)) {
                    BFSShortestPath<TreeNode, DefaultEdge> BFSShortestPath = new BFSShortestPath<TreeNode, DefaultEdge>(tree);
                    GraphPath<TreeNode, DefaultEdge> shortestPath = BFSShortestPath.getPath(node, node1);
                    List<TreeNode> pathNodes = shortestPath.getVertexList();
                    pathNodes.remove(shortestPath.getEndVertex());
                    pathNodes.remove(shortestPath.getStartVertex());
                    for (TreeNode pathNode : pathNodes) {
                        if (!internal.contains(pathNode)) {
                            internal.add(pathNode);
                        }
                    }
                }

            }
            return internal;
        }
    }
}
