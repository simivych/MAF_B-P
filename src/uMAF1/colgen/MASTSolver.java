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

    Map<Long, Subtree> W_lookupTable = new HashMap<>();
    Map<Long, Subtree> V_lookupTable = new HashMap<>();
    double[] duals;
    int count = 0;

    public MASTSolver(MAF dataModel, MAST mast) {
        super(dataModel, mast);
    }
    public MASTSolver(double[] duals, Graph tree1, Graph tree2) {
        super(new MAF(), null);
        this.duals = duals;
        this.dataModel.tree1 = tree1;
        this.dataModel.tree2 = tree2;
        getMAST();
    }

    @Override
    public void close() {
        ;
    }

    @Override
    protected List<Leafset> generateNewColumns() {
        List<Leafset> newLeafSets=new ArrayList<>();
        //System.out.println("GENERATING");
        count++;
        //long time = System.currentTimeMillis();
        duals = this.pricingProblem.dualCosts;
        W_lookupTable = new HashMap<>();
        V_lookupTable = new HashMap<>();
        Leafset newLS = getMAST();
        //long taken = System.currentTimeMillis() - time;
        //System.out.println(newLS);
        if(newLS!=null)
            newLeafSets.add(newLS);
        return newLeafSets;
    }


    public static int[] generateRandomNumbers(int size, int min, int max) {
        int[] numbers = new int[size];
        Random random = new Random();

        for (int i = 0; i < size; i++) {
            numbers[i] = random.nextInt((max - min) + 1) + min;
        }

        return numbers;
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
            final Root[] p = get_e_subtrees(dataModel.tree1, e1);
            Root p1 = p[0];
            Root p2 = p[1];
            for(DefaultEdge e2: dataModel.tree2.edgeSet()){
                final Root[] q = get_e_subtrees(dataModel.tree2, e2);

                Root q1 = q[0];
                Root q2 = q[1];

                Subtree[] subtree1 = solveMAST(p1,q1);
                Subtree[] subtree2 = solveMAST(p2,q2);
                Subtree Wsubtree1 = this.new Subtree(subtree1[0]);
                Subtree Wsubtree2 = this.new Subtree(subtree2[0]);
                Subtree Vsubtree12 = this.new Subtree(subtree1[1],subtree2[1]);

                Subtree[] subtree3 = solveMAST(p2,q1);
                Subtree[] subtree4 = solveMAST(p1,q2);
                Subtree Wsubtree3 = this.new Subtree(subtree3[0]);
                Subtree Wsubtree4 = this.new Subtree(subtree4[0]);
                Subtree Vsubtree34 = this.new Subtree(subtree3[1],subtree4[1]);

                if(Wsubtree1.getWeight()>=max_size){
                    MAST = Wsubtree1;
                    MAST.calculateSize();
                    max_size = Wsubtree1.getWeight();
                }
                if(Wsubtree2.getWeight()>=max_size){
                    MAST = Wsubtree2;
                    MAST.calculateSize();
                    max_size = Wsubtree2.getWeight();
                }
                if(Vsubtree12.getWeight()>=max_size){
                    MAST = Vsubtree12;
                    MAST.calculateSize();
                    max_size = Vsubtree12.getWeight();
                }
                if(Wsubtree3.getWeight()>=max_size){
                    MAST = Wsubtree3;
                    MAST.calculateSize();
                    max_size = Wsubtree3.getWeight();
                }
                if(Wsubtree4.getWeight()>=max_size){
                    MAST = Wsubtree4;
                    MAST.calculateSize();
                    max_size = Wsubtree4.getWeight();
                }
                if(Vsubtree34.getWeight()>=max_size){
                    MAST = Vsubtree34;
                    MAST.calculateSize();
                    max_size = Vsubtree34.getWeight();
                }

            }
        }
        if(MAST==null || max_size <= 1.000001){
            return null;
        }
        MAST = new Subtree(MAST.leaves);
        return new Leafset("pricing", false, MAST.leaves, MAST.subgraphNodes1, MAST.subgraphNodes2, pricingProblem);
    }

    /**
     * Solves MAST for rooted subtrees
     * @param p
     * @param q
     * @return
     */
    public Subtree[] solveMAST(Root p, Root q){
        // If it has already been calculated return previous result
        long hash = get_hash_value(p,q);
        Subtree[] hased_subtrees = get_hash(hash);
        if (hased_subtrees!=null){
            return hased_subtrees;
        }
        // if one of the trees is a single node return the intersection
        if (isLeaf(p) || isLeaf(q)) {
            Set<Node> intersectionSet = new HashSet<>(get_leaves(p, 1));
            intersectionSet.retainAll(get_leaves(q, 2));
            Subtree subtree1 = this.new Subtree(intersectionSet);
            Subtree subtree2 = createV(p, q, intersectionSet);
            set_hash(hash, subtree1, subtree2);
            return new Subtree[]{subtree1, subtree2};
        }
        // Calculate the MAST recursively
        final Root[] p_sub = get_e_subtrees(p, 1);
        final Root[] q_sub = get_e_subtrees(q, 2);

        Subtree max_W_subtree = null;
        Subtree max_V_subtree = null;

        for(Root q_prime: q_sub){
            Subtree[] p_qprime = solveMAST(p, q_prime);
            p_qprime[1].addRoots(p.root, q.root);
            if(isLarger(p_qprime[0], max_W_subtree)){
                max_W_subtree = new Subtree(p_qprime[0]);
            }
            if(isLarger(p_qprime[1], max_V_subtree)){
                max_V_subtree = new Subtree(p_qprime[1]);
            }
        }
        for(Root p_prime: p_sub){
            Subtree[] pprime_q = solveMAST(p_prime, q);
            pprime_q[1].addRoots(p.root, q.root);
            if(isLarger(pprime_q[0], max_W_subtree)){
                max_W_subtree = new Subtree(pprime_q[0]);
            }
            if(isLarger(pprime_q[1], max_V_subtree)){
                max_V_subtree = new Subtree(pprime_q[1]);
            }
        }
        // Matching 1 done on V
        Subtree[] p0q0 = solveMAST(p_sub[0], q_sub[0]);
        Subtree[] p1q1 = solveMAST(p_sub[1], q_sub[1]);
        Subtree matching1 = matching(p0q0[1],p1q1[1], p.root, q.root);
        if(isLarger(matching1, max_W_subtree)){
            max_W_subtree = new Subtree(matching1);
        }
        if(isLarger(matching1, max_V_subtree)){
            max_V_subtree = new Subtree(matching1);
        }

        // Matching 2 done on V
        Subtree[] p0q1 = solveMAST(p_sub[0], q_sub[1]);
        Subtree[] p1q0 = solveMAST(p_sub[1], q_sub[0]);
        Subtree matching2 = matching(p0q1[1], p1q0[1], p.root, q.root);
        if(isLarger(matching2, max_W_subtree)){
            max_W_subtree = new Subtree(matching2);
        }
        if(isLarger(matching2, max_V_subtree)){
            max_V_subtree = new Subtree(matching2);
        }

        // save the result in the hash table
        set_hash(hash, max_W_subtree, max_V_subtree);

        return new Subtree[]{max_W_subtree, max_V_subtree};
    }

    private Subtree createV(Root p, Root q, Set<Node> leaves) {
        Set<Node> subgraphNodes1 = new HashSet<>();
        Set<Node> subgraphNodes2 = new HashSet<>();
        if(leaves.isEmpty()){
            return new Subtree();
        }
        if(isLeaf(p)&&!isLeaf(q)){
            Node leaf = p.root;
            BFSShortestPath<Node, DefaultEdge> BFSShortestPath = new BFSShortestPath<Node, DefaultEdge>(dataModel.tree2);
            GraphPath<Node, DefaultEdge> shortestPath = BFSShortestPath.getPath(leaf, q.root);
            List<Node> pathNodes = shortestPath.getVertexList();
            pathNodes.remove(leaf);
            //pathNodes.remove(q.root);
            subgraphNodes2.addAll(pathNodes);
        }else if (!isLeaf(p)&&isLeaf(q)){
            Node leaf = q.root;
            BFSShortestPath<Node, DefaultEdge> BFSShortestPath = new BFSShortestPath<Node, DefaultEdge>(dataModel.tree1);
            GraphPath<Node, DefaultEdge> shortestPath = BFSShortestPath.getPath(leaf, p.root);
            List<Node> pathNodes = shortestPath.getVertexList();
            pathNodes.remove(leaf);
            //pathNodes.remove(p.root);
            subgraphNodes1.addAll(pathNodes);
        }
        return new Subtree(leaves, subgraphNodes1, subgraphNodes2);
    }

    public boolean areEqual(double x, double y, double epsilon){
        return Math.abs(x - y) < epsilon;
    }

    public boolean isLarger(Subtree test, Subtree max){
        if(max==null){
            return true;
        }else if(areEqual(test.getWeight(),max.getWeight(),0.001)){
            return test.leaves.size()>max.leaves.size();
        }else return test.getWeight()>max.getWeight();
    }

    private Subtree matching(Subtree t1, Subtree t2, Node p, Node q) {
        if(t1.leaves.isEmpty()){
            Subtree s = new Subtree(t2);
            s.subgraphNodes1.add(p);
            s.subgraphNodes2.add(q);
            s.calculateSize();
            return s;
        }else if(t2.leaves.isEmpty()){
            Subtree s = new Subtree(t1);
            s.subgraphNodes1.add(p);
            s.subgraphNodes2.add(q);
            s.calculateSize();
            return s;
        }
        Set<Node> leaves = new HashSet<>(t1.leaves);
        leaves.addAll(t2.leaves);
        Set<Node> subgraphNodes1 = new HashSet<>();
        subgraphNodes1.addAll(t1.subgraphNodes1);
        subgraphNodes1.addAll(t2.subgraphNodes1);
        subgraphNodes1.add(p);
        Set<Node> subgraphNodes2 = new HashSet<>();
        subgraphNodes2.addAll(t1.subgraphNodes2);
        subgraphNodes2.addAll(t2.subgraphNodes2);
        subgraphNodes2.add(q);
        return new Subtree(leaves, subgraphNodes1, subgraphNodes2);
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

    private boolean isLeaf(Root root) {
        return root.children.length < 1;
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
        long hash = Szudzik(tree1_hash, tree2_hash);
        return hash;
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

    public Subtree[] get_hash(long hash){
        if(W_lookupTable.containsKey(hash)){
            Subtree[] subtrees = new Subtree[2];
            subtrees[0] = new Subtree(W_lookupTable.get(hash));
            subtrees[1] = new Subtree(V_lookupTable.get(hash));
            return subtrees;
        }
        return null;
    }
    public void set_hash(long hash, Subtree Wsubtree, Subtree Vsubtree){
        W_lookupTable.put(hash, new Subtree(Wsubtree));
        V_lookupTable.put(hash, new Subtree(Vsubtree));
    }

    /**
     * A class that will store the leaf set and its weight of a partial MAST
     */

    public class Subtree {
        public Set<Node> subgraphNodes1 = new HashSet<>();
        public Set<Node> subgraphNodes2 = new HashSet<>();
        private double weightedSize = 0;

        public final Set<Node> leaves;

        public Subtree(Set<Node> leaves) {
            this.leaves = new HashSet<>(leaves);
            getWeightedSize();
        }
        public Subtree() {
            this.leaves = new HashSet<>();
        }

        double getWeight(){
            return weightedSize;
        }

        public Subtree(Subtree subtree) {
            this.subgraphNodes1 = new HashSet<>(subtree.subgraphNodes1);
            this.subgraphNodes2 = new HashSet<>(subtree.subgraphNodes2);
            this.leaves = new HashSet<>(subtree.leaves);
            this.weightedSize = subtree.getWeight();
        }

        public Subtree(Subtree subtree1, Subtree subtree2) {
            Set<Node> all = new HashSet<>(subtree1.leaves);
            all.addAll(subtree2.leaves);
            this.leaves = all;
            getWeightedSizeFromSubtrees(subtree1,subtree2);
        }

        public Subtree(Set<Node> leaves, Set<Node> subgraphNodes1, Set<Node> subgraphNodes2) {
            this.leaves = new HashSet<>(leaves);
            this.subgraphNodes1 = new HashSet<>(subgraphNodes1);
            this.subgraphNodes2 = new HashSet<>(subgraphNodes2);
            calculateSize();
        }

        private void getWeightedSizeFromSubtrees(Subtree subtree1, Subtree subtree2) {
            if(subtree1.leaves.isEmpty()){
                this.subgraphNodes1 = new HashSet<>(subtree2.subgraphNodes1);
                this.subgraphNodes2 = new HashSet<>(subtree2.subgraphNodes2);
                this.weightedSize = subtree2.getWeight();
                return;
            }
            if(subtree2.leaves.isEmpty()){
                this.subgraphNodes1 = new HashSet<>(subtree1.subgraphNodes1);
                this.subgraphNodes2 = new HashSet<>(subtree1.subgraphNodes2);
                this.weightedSize = subtree1.getWeight();
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

        public void addRoots(Node Proot, Node Qroot) {
            if(!leaves.isEmpty()) {
                if (!subgraphNodes1.contains(Proot)) {
                    subgraphNodes1.add(Proot);
                    weightedSize += duals[Proot.id];
                }
                if (!subgraphNodes2.contains(Qroot)) {
                    subgraphNodes2.add(Qroot);
                    weightedSize += duals[Qroot.id];
                }
            }
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
