package uMAF;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.shortestpath.BFSShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.*;

public class MAST {
    Graph<Node, DefaultEdge> tree1;
    Graph<Node, DefaultEdge> tree2;
    Map<String, Double> duals;
    Map<Integer, Subtree> lookupTable = new HashMap<>();

    public MAST( Graph<Node, DefaultEdge> tree1, Graph<Node, DefaultEdge> tree2, Map<String, Double> duals){
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
            for(DefaultEdge e2: tree2.edgeSet()){
                Graph<Node, DefaultEdge>[] p = get_e_subtrees(tree1, e1);
                Graph<Node, DefaultEdge> p1 = p[0];
                Graph<Node, DefaultEdge> p2 = p[1];
                Graph<Node, DefaultEdge>[] q = get_e_subtrees(tree2, e2);
                Graph<Node, DefaultEdge> q1 = q[0];
                Graph<Node, DefaultEdge> q2 = q[1];

                Subtree subtree1 = solveMAST(p1,q1);
                Subtree subtree2 = solveMAST(p1,q2);
                Subtree subtree12 = this.new Subtree(subtree1,subtree2);

                Subtree subtree3 = solveMAST(p2,q1);
                Subtree subtree4 = solveMAST(p2,q2);
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
        if(MAST==null || max_size < 1 ){
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
    public Subtree solveMAST(Graph<Node, DefaultEdge> p, Graph<Node, DefaultEdge> q){
        // If it has already been calculated return previous result
        int hash = get_hash_value(p,q);
        Subtree hased_subtree = get_hash(hash);
        if (hased_subtree!=null){
            return hased_subtree;
        }
        // if one of the trees is a single node return the intersection
        if (p.vertexSet().size() == 1 || q.vertexSet().size() == 1) {
            Set<Node> intersectionSet = new HashSet<>(p.vertexSet());
            intersectionSet.retainAll(q.vertexSet());
            Subtree subtree = this.new Subtree(intersectionSet);
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
        //TODO bipartite matching of p_prime and q_prime pairs


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

    public int get_hash_value(Graph<Node, DefaultEdge> tree1, Graph<Node, DefaultEdge> tree2){
        Node root1 = get_root(tree1);
        Node root2 = get_root(tree2);
        // Using Szudzik pairing function
        int hash;
        if(root1.id < root2.id){
            hash = root1.id + root2.id^2;
        }else{
            hash = root1.id^2 + root1.id + root2.id;
        }
        return hash;
    }

    public Subtree get_hash(int hash){
        if(lookupTable.containsKey(hash)){
            return lookupTable.get(hash);
        }
        return null;
    }
    public void set_hash(int hash, Subtree subtree){
        lookupTable.put(hash, subtree);

    }

    /**
     * A class that will store the leaf set and its weight of a partial MAST
      */

    public class Subtree {
        public Set<Node> subgraphNodes1 = new HashSet<>();
        public Set<Node> subgraphNodes2 = new HashSet<>();
        public double weightedSize;
        public Set<Node> leaves;

        public Subtree(Set<Node> leaves){
            this.leaves = leaves;
            getWeightedSize();
        }

        public Subtree(Subtree subtree1, Subtree subtree2) {
            this.leaves = subtree1.leaves;
            this.leaves.addAll(subtree2.leaves);
            getWeightedSize();
        }

        public void getWeightedSize(){
            weightedSize = 0;
            if(leaves.size()<=1){
                for(Node n: leaves){
                    weightedSize += duals.get(n.name);
                }
            }else {
                Set<Node> internal1 = getInternalNodes(tree1);
                Set<Node> internal2 = getInternalNodes(tree2);
                for (Node n : internal1) {
                    weightedSize += duals.get(n.name);
                    subgraphNodes1.add(n);
                }
                for (Node n : internal2) {
                    weightedSize += duals.get(n.name);
                    subgraphNodes1.add(n);
                }
                for (Node n : leaves) {
                    weightedSize += duals.get(n.name);
                }
            }
        }

        public Set<Node> getInternalNodes(Graph<Node, DefaultEdge> tree){
            Set<Node> internal = new HashSet<>();
            List<Node> leafNodesList = new ArrayList<>(leaves);
            Node node1 = leafNodesList.getFirst();

            for (Node node : leaves) {
                if(node.equals(node1)){
                    break;
                }
                BFSShortestPath<Node, DefaultEdge> BFSShortestPath = new BFSShortestPath<Node, DefaultEdge>(tree);
                GraphPath<Node, DefaultEdge> shortestPath = BFSShortestPath.getPath(node, node1);
                List<Node> pathNodes = shortestPath.getVertexList();
                pathNodes.remove(shortestPath.getEndVertex());
                pathNodes.remove(shortestPath.getStartVertex());
                for (Node pathNode : pathNodes) {
                    if(!internal.contains(pathNode)){
                        internal.add(pathNode);
                    }
                }

            }
            return internal;
        }
    }
}
