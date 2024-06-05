package uMAF1.colgen;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.shortestpath.BFSShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;
import uMAF1.misc.Node;
import uMAF1.model.*;

import java.util.*;

public final class MASTSolver extends AbstractPricingProblemSolver<MAF, Leafset, MAST> {

    Map<Long, Subtree> lookupTable = new HashMap<>();
    double[] duals;

    public MASTSolver(MAF dataModel, MAST mast) {
        super(dataModel, mast);
    }

    @Override
    public void close() {
        ;
    }

    @Override
    protected List<Leafset> generateNewColumns() {
        List<Leafset> newLeafSets=new ArrayList<>();
        System.out.println("GENERATING");
        duals = this.pricingProblem.dualCosts;
        lookupTable = new HashMap<>();
        Leafset newLS = getMAST();
        System.out.println(newLS);
        if(newLS!=null)
            newLeafSets.add(newLS);
        return newLeafSets;
    }

    @Override
    protected void setObjective() {
        ;
    }
    public Leafset getMAST(){
        // initial split into rooted subtrees
        double max_size = -Double.MAX_VALUE;
        Subtree MAST = null;
        for(DefaultEdge e1: dataModel.tree1.edgeSet()){
            Root[] p = get_e_subtrees(dataModel.tree1, e1);
            Root p1 = p[0];
            Root p2 = p[1];
            for(DefaultEdge e2: dataModel.tree2.edgeSet()){
                Root[] q = get_e_subtrees(dataModel.tree2, e2);
                Root q1 = q[0];
                Root q2 = q[1];

                Subtree subtree1 = solveMAST(p1,q1);
                Subtree subtree2 = solveMAST(p2,q2);
                Subtree subtree12 = this.new Subtree(subtree1,subtree2);

                Subtree subtree3 = solveMAST(p2,q1);
                Subtree subtree4 = solveMAST(p1,q2);
                Subtree subtree34 = this.new Subtree(subtree3,subtree4);

                if(subtree12.weightedSize>=max_size){
                    MAST = subtree12;
                    max_size = subtree12.weightedSize;
                }
                if(subtree34.weightedSize>=max_size){
                    MAST = subtree34;
                    max_size = subtree34.weightedSize;
                }
            }
        }
        if(MAST==null || max_size <= 1.000001){
            return null;
        }
        return new Leafset("pricing", false, MAST.leaves, MAST.subgraphNodes1, MAST.subgraphNodes2, pricingProblem);
    }

    /**
     * Solves MAST for rooted subtrees
     * @param p
     * @param q
     * @return
     */
    public Subtree solveMAST(Root p, Root q){
        // If it has already been calculated return previous result
        long hash = get_hash_value(p,q);
        Subtree hased_subtree = get_hash(hash);
        if (hased_subtree!=null){
            return hased_subtree;
        }
        // if one of the trees is a single node return the intersection
        if (size_less_3(p, 1)|| size_less_3(q, 2)) {
            Set<Node> intersectionSet = new HashSet<>(get_leaves(p, 1));
            intersectionSet.retainAll(get_leaves(q, 2));
            Subtree subtree = this.new Subtree(intersectionSet);
            set_hash(hash, subtree);
            return subtree;
        }
        // Calculate the MAST recursively
        Root[] p_sub = get_e_subtrees(p, 1);
        Root[] q_sub = get_e_subtrees(q, 2);

        double max_weight = -Double.MAX_VALUE;
        Subtree max_subtree = null;

        for(Root q_prime: q_sub){
            Subtree p_qprime = solveMAST(p, q_prime);
            if(max_weight<p_qprime.weightedSize){
                max_weight = p_qprime.weightedSize;
                max_subtree = p_qprime;
            }

        }
        for(Root p_prime: p_sub){
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


    private Root[] get_e_subtrees(Graph<Node, DefaultEdge> tree, DefaultEdge edge) {
        Node root1 = tree.getEdgeSource(edge);
        Node root2 = tree.getEdgeTarget(edge);
        Root e_subtrees1 = dfs(tree, root1, edge);
        Root e_subtrees2 = dfs(tree, root2, edge);
        return new Root[]{e_subtrees1, e_subtrees2};
    }

    private Root[] get_e_subtrees(Root tree, int full_tree) {
        Root[] children = new Root[2];
        int i = 0;
        Graph<Node, DefaultEdge> full_tree_graph;
        if(full_tree==1) {
            full_tree_graph = dataModel.tree1;
        }
        else {
            full_tree_graph = dataModel.tree2;
        }
        for (Node n : tree.children) {
            if(!n.isInternal()){
                children[i] = new Root(n);
                i++;
            }else {
                Node[] children_new = new Node[2];
                int j = 0;
                for (DefaultEdge e : full_tree_graph.edgesOf(n)) {
                    Node child = Graphs.getOppositeVertex(full_tree_graph, e, n);
                    if (child != tree.root) {
                        children_new[j] = child;
                        j++;
                    }
                }
                children[i] = new Root(n, children_new);
                i++;
            }
        }
        return children;
    }

    private boolean size_less_3(Root root, int i) {
        if(root.children.length<1){
            return true;
        }
        return !root.children[0].isInternal() && !root.children[1].isInternal();
    }

    public Set<Node> get_leaves(Root root, int tree){
        Set<Node> leaves = new HashSet<>();
        if (!root.root.isInternal()) {
            leaves.add(root.root);
            return leaves;
        }
        Stack<Node> stack = new Stack<>();
        Set<Node> all_nodes = new HashSet<>();
        stack.push(root.root);
        all_nodes.add(root.root);
        Graph<Node, DefaultEdge> full_tree_graph;
        if(tree==1) {
            full_tree_graph = dataModel.tree1;
        }
        else {
            full_tree_graph = dataModel.tree2;
        }

        while (!stack.isEmpty()) {
            Node parent = stack.pop();
            for (DefaultEdge edge : full_tree_graph.edgesOf(parent)) {
                Node child = Graphs.getOppositeVertex(full_tree_graph, edge, parent);
                if(parent.equals(root.root) && !child.equals(root.children[0])&& !child.equals(root.children[1])){
                    ;
                }
                else if (!all_nodes.contains(child)) {
                    stack.add(child);
                    all_nodes.add(child);
                    if (!child.isInternal()) {
                        leaves.add(child);
                    }
                }
            }
        }
        return leaves;
    }

    private Root dfs(Graph<Node, DefaultEdge> tree, Node root, DefaultEdge removing_edge) {
        if(!root.isInternal()){
            return new Root(root);
        }
        Node[] children = new Node[2];
        int i = 0;
        for (DefaultEdge edge : tree.edgesOf(root)) {
            if (!edge.equals(removing_edge)) {
                Node child = Graphs.getOppositeVertex(tree, edge, root);
                children[i] = child;
                i++;
            }
        }
        return new Root(root, children);
    }

    public long get_hash_value(Root tree1, Root tree2) {
        long tree1_hash;
        if(tree1.children.length>1){
            Node parent = null;
            for (DefaultEdge edge : dataModel.tree1.edgesOf(tree1.root)) {
                Node child = Graphs.getOppositeVertex(dataModel.tree1, edge, tree1.root);
                if(child.id != tree1.children[0].id && child.id != tree1.children[1].id){
                    parent = child;
                    break;
                }
            }
            tree1_hash = Szudzik(tree1.root.id, parent.id);
        }else{
            tree1_hash = Szudzik(tree1.root.id, 0);
        }
        long tree2_hash;
        if(tree2.children.length>1){
            Node parent = null;
            for (DefaultEdge edge : dataModel.tree2.edgesOf(tree2.root)) {
                Node child = Graphs.getOppositeVertex(dataModel.tree2, edge, tree2.root);
                if(child.id != tree2.children[0].id && child.id != tree2.children[1].id){
                    parent = child;
                    break;
                }
            }
            tree2_hash = Szudzik(tree2.root.id, parent.id);
        }else{
            tree2_hash = Szudzik(tree2.root.id, 0);
        }
        return Szudzik(tree1_hash, tree2_hash);
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
        if(lookupTable.containsKey(hash)){
            return lookupTable.get(hash);
        }
        return null;
    }
    public void set_hash(long hash, Subtree subtree){
        lookupTable.put( hash, subtree);

    }

    /**
     * A class that will store the leaf set and its weight of a partial MAST
     */

    public class Subtree {
        public Set<Node> subgraphNodes1 = new HashSet<>();
        public Set<Node> subgraphNodes2 = new HashSet<>();
        public double weightedSize = 0;
        public final Set<Node> leaves;

        public Subtree(Set<Node> leaves) {
            this.leaves = leaves;
            getWeightedSize();
        }

        public Subtree(Subtree subtree1, Subtree subtree2) {
            Set<Node> all = new HashSet<>(subtree1.leaves);
            all.addAll(subtree2.leaves);
            this.leaves = all;
            getWeightedSizeFromSubtrees(subtree1,subtree2);
        }

        private void getWeightedSizeFromSubtrees(Subtree subtree1, Subtree subtree2) {
            if(subtree1.leaves.isEmpty()){
                this.subgraphNodes1 = subtree2.subgraphNodes1;
                this.subgraphNodes2 = subtree2.subgraphNodes2;
                this.weightedSize = subtree2.weightedSize;
                return;
            }
            if(subtree2.leaves.isEmpty()){
                this.subgraphNodes1 = subtree1.subgraphNodes1;
                this.subgraphNodes2 = subtree1.subgraphNodes2;
                this.weightedSize = subtree1.weightedSize;
                return;
            }
            this.subgraphNodes1 = new HashSet<>(subtree1.subgraphNodes1);
            this.subgraphNodes1.addAll(subtree2.subgraphNodes1);
            this.subgraphNodes2 = new HashSet<>(subtree1.subgraphNodes2);
            this.subgraphNodes2.addAll(subtree2.subgraphNodes2);
            Node leaf1 = subtree1.leaves.iterator().next();
            Node leaf2 = subtree2.leaves.iterator().next();

            BFSShortestPath<Node, DefaultEdge> BFSShortestPath = new BFSShortestPath<Node, DefaultEdge>(dataModel.tree1);
            GraphPath<Node, DefaultEdge> shortestPath = BFSShortestPath.getPath(leaf1, leaf2);
            List<Node> pathNodes = shortestPath.getVertexList();
            pathNodes.remove(leaf1);
            pathNodes.remove(leaf2);
            subgraphNodes1.addAll(pathNodes);

            BFSShortestPath = new BFSShortestPath<Node, DefaultEdge>(dataModel.tree2);
            shortestPath = BFSShortestPath.getPath(leaf1, leaf2);
            pathNodes = shortestPath.getVertexList();
            pathNodes.remove(leaf1);
            pathNodes.remove(leaf2);
            subgraphNodes2.addAll(pathNodes);

            calculateSize();
        }

        private void getWeightedSize() {
            if (leaves.size() <= 1) {
                for (Node n : leaves) {
                    weightedSize += duals[n.id];
                }
            } else {
                subgraphNodes1 = getInternalNodes(dataModel.tree1);
                subgraphNodes2 = getInternalNodes(dataModel.tree2);
                calculateSize();
            }
        }

        private void calculateSize() {
            weightedSize = 0;
            for (Node n : subgraphNodes1) {
                weightedSize += duals[n.id];
            }
            for (Node n : subgraphNodes2) {
                weightedSize += duals[n.id];
            }
            for (Node n : leaves) {
                weightedSize += duals[n.id];
            }
        }

        public Set<Node> getInternalNodes(Graph<Node, DefaultEdge> tree) {
            Set<Node> internal = new HashSet<>();
            List<Node> leafNodesList = new ArrayList<>(leaves);
            Node node1 = leafNodesList.getFirst();
            for (Node node : leaves) {
                if (!node.equals(node1)) {
                    BFSShortestPath<Node, DefaultEdge> BFSShortestPath = new BFSShortestPath<Node, DefaultEdge>(tree);
                    GraphPath<Node, DefaultEdge> shortestPath = BFSShortestPath.getPath(node, node1);
                    List<Node> pathNodes = shortestPath.getVertexList();
                    pathNodes.remove(shortestPath.getEndVertex());
                    pathNodes.remove(shortestPath.getStartVertex());
                    internal.addAll(pathNodes);
                }
            }
            return internal;
        }
    }

    public class Root {
        Node root;
        Node[] children;

        public Root(Node root, Node[] children) {
            this.root = root;
            this.children = children;
        }

        public Root(Node root) {
            this.root = root;
            this.children = new Node[0];
        }

        @Override
        public String toString() {
            return root.name;
        }

    }

}
