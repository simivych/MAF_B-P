package uMAF1.colgen;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.shortestpath.BFSShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;
import uMAF1.misc.Node;
import uMAF1.model.*;

import java.util.*;

public final class MASTSolver extends AbstractPricingProblemSolver<MAF, Leafset, MAST> {

    Map<Integer, Subtree> lookupTable = new HashMap<>();
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
        List<Leafset> newleafsets=new ArrayList<>();
        System.out.println("Generating");
        duals = this.pricingProblem.dualCosts;
        Leafset newLS = getMAST();
        System.out.println(newLS);
        if(newLS!=null)
            newleafsets.add(newLS);
        return newleafsets;
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
            Graph<Node, DefaultEdge>[] p = get_e_subtrees(dataModel.tree1, e1);
            Graph<Node, DefaultEdge> p1 = p[0];
            Graph<Node, DefaultEdge> p2 = p[1];
            for(DefaultEdge e2: dataModel.tree2.edgeSet()){
                Graph<Node, DefaultEdge>[] q = get_e_subtrees(dataModel.tree2, e2);
                Graph<Node, DefaultEdge> q1 = q[0];
                Graph<Node, DefaultEdge> q2 = q[1];

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
        return new Leafset("pricing", false, MAST.leaves, pricingProblem);
    }

    /**
     * Solves MAST for rooted subtrees
     * @param p
     * @param q
     * @return
     */
    public Subtree solveMAST(Graph<Node, DefaultEdge> p, Graph<Node, DefaultEdge> q){
        // If it has already been calculated return previous result
        long hash = get_hash_value(p,q);
        Subtree hased_subtree = get_hash(hash);
        if (hased_subtree!=null){
            return hased_subtree;
        }
        // if one of the trees is a single node return the intersection
        if (p.vertexSet().size() <= 3 || q.vertexSet().size() <= 3) {
            Set<Node> intersectionSet = new HashSet<>(p.vertexSet());
            intersectionSet.retainAll(q.vertexSet());
            Subtree subtree = this.new Subtree(intersectionSet);
            set_hash(hash, subtree);
            return subtree;
        }
        // Calculate the MAST recursively
        Graph<Node, DefaultEdge>[] p_sub = get_e_subtrees(p);
        Graph<Node, DefaultEdge>[] q_sub = get_e_subtrees(q);

        double max_weight = -Double.MAX_VALUE;
        Subtree max_subtree = null;

        for(Graph<Node, DefaultEdge> q_prime: q_sub){
            Subtree p_qprime = solveMAST(p, q_prime);
            if(max_weight<p_qprime.weightedSize){
                max_weight = p_qprime.weightedSize;
                max_subtree = p_qprime;
            }

        }
        for(Graph<Node, DefaultEdge> p_prime: p_sub){
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

    private Graph<Node, DefaultEdge>[] get_e_subtrees(Graph<Node, DefaultEdge> tree, DefaultEdge edge) {
        Node root1 = tree.getEdgeSource(edge);
        Node root2 = tree.getEdgeTarget(edge);
        Graph<Node, DefaultEdge> e_subtrees1 = dfs(tree, root1, edge);
        Graph<Node, DefaultEdge> e_subtrees2 = dfs(tree, root2, edge);
        Graph<Node, DefaultEdge>[] e_subtrees = new Graph[]{e_subtrees1, e_subtrees2};
        return e_subtrees;
    }

    private Graph<Node, DefaultEdge>[] get_e_subtrees(Graph<Node, DefaultEdge> tree) {
        Node root = get_root(tree);
        Graph<Node, DefaultEdge>[] e_subtrees = new Graph[2];
        int i = 0;
        for(DefaultEdge edge: tree.edgesOf(root)){
            Node n = Graphs.getOppositeVertex(tree, edge, root);
            e_subtrees[i] = dfs(tree, n, edge);
            i++;
        }
        return e_subtrees;
    }

    private Graph<Node, DefaultEdge> dfs(Graph<Node, DefaultEdge> tree, Node root, DefaultEdge removing_edge) {
        Stack<Node> stack = new Stack<>();
        stack.push(root);
        DefaultUndirectedGraph<Node, DefaultEdge> e_subtree = new DefaultUndirectedGraph<>(DefaultEdge.class);
        e_subtree.addVertex(root);

        while (!stack.isEmpty()) {
            Node parent = stack.pop();
            for (DefaultEdge edge : tree.edgesOf(parent)) {
                if(!edge.equals(removing_edge)){
                    Node child = Graphs.getOppositeVertex(tree, edge, parent);
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
    public Node get_root(Graph<Node, DefaultEdge> tree){
        if(tree.vertexSet().size()==1){
            return tree.vertexSet().iterator().next();
        }
        for(Node n: tree.vertexSet()){
            if(tree.degreeOf(n)==2){
                return n;
            }
        }
        return null;
    }

    public Node[] get_root_and_children(Graph<Node, DefaultEdge> tree){
        if(tree.vertexSet().size()==1){
            return new Node[]{tree.vertexSet().iterator().next()};
        }
        for(Node n: tree.vertexSet()){
            if(tree.degreeOf(n)==2){
                Node[] root_children = new Node[3];
                root_children[0] = n;
                int i = 1;
                for (DefaultEdge edge : tree.edgesOf(n)) {
                    Node child = Graphs.getOppositeVertex(tree, edge, n);
                    root_children[i]=child;
                    i++;
                }
                return root_children;
            }
        }
        return null;
    }

    public long get_hash_value(Graph<Node, DefaultEdge> tree1, Graph<Node, DefaultEdge> tree2) {
        Node[] tree1_root_children = get_root_and_children(tree1);
        Node[] tree2_root_children = get_root_and_children(tree2);
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
        public Set<Node> subgraphNodes1 = new HashSet<>();
        public Set<Node> subgraphNodes2 = new HashSet<>();
        public double weightedSize = -Double.MAX_VALUE;
        public final Set<Node> leaves;

        public Subtree(Set<Node> leaves) {
            this.leaves = leaves;
            getWeightedSize();
        }


        public Subtree(Subtree subtree1, Subtree subtree2) {
            Set<Node> all = new HashSet<>(subtree1.leaves);
            all.addAll(subtree2.leaves);
            this.leaves = all;
            getWeightedSize();
        }

        private void getWeightedSize(){
            weightedSize = 0;
            if(leaves.size()<=1){
                for(Node n: leaves){
                    weightedSize += duals[n.id];
                }
            }else {
                subgraphNodes1= getInternalNodes(dataModel.tree1);
                subgraphNodes2 = getInternalNodes(dataModel.tree2);
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
        }

        public Set<Node> getInternalNodes(Graph<Node, DefaultEdge> tree){
            Set<Node> internal = new HashSet<>();
            List<Node> leafNodesList = new ArrayList<>(leaves);
            Node node1 = leafNodesList.getFirst();

            for (Node node : leaves) {
                if(!node.equals(node1)) {
                    BFSShortestPath<Node, DefaultEdge> BFSShortestPath = new BFSShortestPath<Node, DefaultEdge>(tree);
                    GraphPath<Node, DefaultEdge> shortestPath = BFSShortestPath.getPath(node, node1);
                    List<Node> pathNodes = shortestPath.getVertexList();
                    pathNodes.remove(shortestPath.getEndVertex());
                    pathNodes.remove(shortestPath.getStartVertex());
                    for (Node pathNode : pathNodes) {
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
