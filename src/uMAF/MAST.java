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

    public MAST( Graph<Node, DefaultEdge> tree1, Graph<Node, DefaultEdge> tree2, Map<String, Double> duals){
        this.tree1 = tree1;
        this.tree2 = tree2;
        this.duals = duals;
    }

    public Set<Node> getMAST(){
        // initial split into rooted subtrees
        double max_size = 0;
        Set<Node> MAST_leaves = null;
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
                    MAST_leaves = subtree12.leaves;
                    max_size = subtree12.weightedSize;
                }
                if(subtree34.weightedSize>max_size){
                    MAST_leaves = subtree34.leaves;
                    max_size = subtree34.weightedSize;
                }
            }
        }

        return MAST_leaves;
    }

    public Subtree solveMAST(Graph<Node, DefaultEdge> tree1, Graph<Node, DefaultEdge> tree2){
        Stack<Graph<Node, DefaultEdge>[]> stack = new Stack<>();
        Graph<Node, DefaultEdge>[] graphArray = (Graph<Node, DefaultEdge>[]) new Graph<?, ?>[]{tree1, tree2};
        stack.push(graphArray);

        while (!stack.isEmpty()) {
            Graph<Node, DefaultEdge>[] graphs = stack.pop();
            Graph<Node, DefaultEdge> p = graphs[0];
            Graph<Node, DefaultEdge> q = graphs[1];
            if (p.vertexSet().size() == 1 || q.vertexSet().size() == 1) {
                Set<Node> intersectionSet = new HashSet<>(p.vertexSet());
                intersectionSet.retainAll(q.vertexSet());
                return this.new Subtree(intersectionSet);
            }

        }

        //TODO weighted MAST iteratively
        // compute list of 'child' pairs for p,q
        // for each list calculate solveMAST
        // return max

        return null;
    }

    private Graph<Node, DefaultEdge>[] get_e_subtrees(Graph<Node, DefaultEdge> tree, DefaultEdge edge) {
        Node root1 = tree.getEdgeSource(edge);
        Node root2 = tree.getEdgeTarget(edge);
        tree.removeEdge(edge);
        Graph<Node, DefaultEdge> e_subtrees1 = dfs(tree, root1);
        Graph<Node, DefaultEdge> e_subtrees2 = dfs(tree, root2);
        Graph<Node, DefaultEdge>[] e_subtrees = new Graph[]{e_subtrees1, e_subtrees2};
        return e_subtrees;
    }

    private Graph<Node, DefaultEdge> dfs(Graph<Node, DefaultEdge> tree, Node root) {
        Stack<Node> stack = new Stack<>();
        stack.push(root);
        DefaultUndirectedGraph<Node, DefaultEdge> e_subtree = new DefaultUndirectedGraph<>(DefaultEdge.class);
        e_subtree.addVertex(root);

        while (!stack.isEmpty()) {
            Node parent = stack.pop();
            for (DefaultEdge edge : tree.edgesOf(parent)) {
                Node child = Graphs.getOppositeVertex(tree, edge, parent);
                e_subtree.addVertex(child);
                e_subtree.addEdge(parent, child);
                stack.add(child);
            }
        }
        return e_subtree;
    }

    // A class that will store the leaf set and its weight of a partial MAST
    public class Subtree {
        public Set<Node> nodes;
        public double weightedSize;
        public Set<Node> leaves;

        public Subtree(Set<Node> nodes){
            this.nodes = nodes;
            getWeightedSize();
        }

        public Subtree(Subtree subtree1, Subtree subtree2) {
            this.leaves = subtree1.leaves;
            this.leaves.addAll(subtree2.leaves);
            getWeightedSize();
        }

        public void getWeightedSize(){
            Set<Node> internal1 = getInternalNodes(tree1);
            Set<Node> internal2 = getInternalNodes(tree2);
            weightedSize = 0;
            for(Node n: internal1){
                weightedSize += duals.get(n.name);
            }
            for(Node n: internal2){
                weightedSize += duals.get(n.name);
            }
            for(Node n: leaves){
                weightedSize += duals.get(n.name);
            }
        }

        public Set<Node> getInternalNodes(Graph<Node, DefaultEdge> tree){
            Set<Node> internal = new HashSet<>();
            List<Node> leafNodesList = new ArrayList<>(leaves);
            Node node1 = leafNodesList.get(0);

            for (Node node : leaves) {
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
