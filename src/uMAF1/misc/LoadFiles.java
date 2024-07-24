package uMAF1.misc;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class LoadFiles {

    public static String[] read(String filePath){
        String[] tree_strings = new String[2];
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int i = 0;
            while ((line = br.readLine()) != null) {
                tree_strings[i] = line;
                i++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return tree_strings;

    }

    public static Graph<Node, DefaultEdge>[] get_trees(String file){
        String[] tree_strings = read(file);
        if(file.contains(".tree")) {
            Graph<Node, DefaultEdge> graph1 = convert(tree_strings[0]);
            Graph<Node, DefaultEdge> graph2 = convert(tree_strings[1]);
            return new Graph[]{graph1, graph2};
        }else{
            Set<Integer> leaves = get_leaves(tree_strings[0]);
            List<String> subtrees = get_subtrees(tree_strings[0]);
            HashMap<String, Integer> subtree_mapping = get_subtree_mapping(leaves, subtrees);
            Graph<Node, DefaultEdge> graph1 = convert_reduced(tree_strings[0], subtree_mapping);
            Graph<Node, DefaultEdge> graph2 = convert_reduced(tree_strings[1], subtree_mapping);
            return new Graph[]{graph1, graph2};
        }

    }

    private static HashMap<String, Integer> get_subtree_mapping(Set<Integer> leaves, List<String> subtrees) {
        HashMap<String, Integer> mapping = new HashMap<>();
        int currentNumber = 1;
        for (String subtree : subtrees) {
            if (!mapping.containsKey(subtree)) {
                while (leaves.contains(currentNumber)) {
                    currentNumber++;
                }
                mapping.put(subtree, currentNumber);
                currentNumber++;
            }
        }

        return mapping;
    }

    public static Set<Integer> get_leaves(String tree){
        Set<Integer> leaves = new HashSet<>();
        for (int i=0; i<tree.length();i++) {
            char c = tree.charAt(i);
            if (Character.isDigit(c) && tree.charAt(i-1)!='T') {
                StringBuilder node_builder = new StringBuilder();
                node_builder.append(tree.charAt(i));
                while (Character.isDigit(tree.charAt(i+1))) {
                    node_builder.append(tree.charAt(i+1));
                    i++;
                }
                int leaf;
                try {
                    leaf = Integer.parseInt(node_builder.toString());
                }
                catch (NumberFormatException e) {
                    leaf = 0;
                }
                leaves.add(leaf);

            }
        }
        return leaves;
    }
    public static List<String> get_subtrees(String tree){
        List<String> subtrees = new ArrayList<>();
        for (int i=0; i<tree.length();i++) {
            char c = tree.charAt(i);
            if (c=='S') {
                StringBuilder node_builder = new StringBuilder();
                node_builder.append(tree.charAt(i));
                i++;
                node_builder.append(tree.charAt(i));
                while (Character.isDigit(tree.charAt(i+1))) {
                    node_builder.append(tree.charAt(i+1));
                    i++;
                }
                subtrees.add(node_builder.toString());

            }
        }
        return subtrees;
    }
    /**
     * Convers Newick format to Graph<Node, DefaultEdge>, assuming all leaves are ints or ST_
     * @param newick
     * @return
     */
    public static Graph<Node, DefaultEdge> convert_reduced(String newick, HashMap<String, Integer> subtree_mapping) {
        DefaultUndirectedGraph<Node, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        Stack<Node> stack = new Stack<>();
        for (int i=0; i<newick.length();i++){
            char c = newick.charAt(i);
            if (c == '(' || c=='[') {
                Node n = new Node();
                graph.addVertex(n);
                if(!stack.isEmpty()){
                    Node parent = stack.peek();
                    graph.addEdge(parent, n);
                }
                stack.push(n);
            }else if (c == ')'|| c==']') {
                stack.pop();
            } else if (Character.isDigit(c)){
                StringBuilder node_builder = new StringBuilder();
                node_builder.append(newick.charAt(i));
                while (Character.isDigit(newick.charAt(i+1))) {
                    node_builder.append(newick.charAt(i+1));
                    i++;
                }

                Node node = new Node(node_builder.toString());
                graph.addVertex(node);
                Node parent = stack.peek();
                graph.addEdge(parent, node);
            }else if (c=='S'){
                StringBuilder node_builder = new StringBuilder();
                node_builder.append(newick.charAt(i));
                i++;
                node_builder.append(newick.charAt(i));
                while (Character.isDigit(newick.charAt(i+1))) {
                    node_builder.append(newick.charAt(i+1));
                    i++;
                }
                int new_id = subtree_mapping.get(node_builder.toString());
                Node node = new Node(new_id);
                graph.addVertex(node);
                Node parent = stack.peek();
                graph.addEdge(parent, node);
            }
        }
        return graph;
    }

    /**
     * Convers Newick format to Graph<Node, DefaultEdge>, assuming all leaves are ints
     * @param newick
     * @return
     */
    public static Graph<Node, DefaultEdge> convert(String newick) {
        DefaultUndirectedGraph<Node, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        Stack<Node> stack = new Stack<>();
        for (int i=0; i<newick.length();i++){
            char c = newick.charAt(i);
            if (c == '(') {
                Node n = new Node();
                graph.addVertex(n);
                if(!stack.isEmpty()){
                    Node parent = stack.peek();
                    graph.addEdge(parent, n);
                }
                stack.push(n);
            }else if (c == ')') {
                stack.pop();
            } else if (Character.isDigit(c)){
                StringBuilder node_builder = new StringBuilder();
                node_builder.append(newick.charAt(i));
                while (Character.isDigit(newick.charAt(i+1))) {
                    node_builder.append(newick.charAt(i+1));
                    i++;
                }

                Node node = new Node(node_builder.toString());
                graph.addVertex(node);
                Node parent = stack.peek();
                graph.addEdge(parent, node);
            }
        }
        return graph;
    }


    private static int get_subtree(int prevSubtree, List<Integer> leaves) {
        while(true){
            prevSubtree++;
            if(!leaves.contains(prevSubtree)){
                return prevSubtree;
            }
        }
    }

}
