package uMAF;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

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

    public static Graph<TreeNode, DefaultEdge>[] get_trees(String file){
        String[] tree_strings = read(file);
        Graph<TreeNode, DefaultEdge> graph1 = convert(tree_strings[0]);
        Graph<TreeNode, DefaultEdge> graph2 = convert(tree_strings[1]);

        return new Graph[]{graph1, graph2};

    }

    public static List<String> get_leaves(String tree){
        List<String> leaves = new ArrayList<>();
        for (int i=0; i<tree.length();i++) {
            char c = tree.charAt(i);
            if (Character.isDigit(c)) {
                StringBuilder node_builder = new StringBuilder();
                node_builder.append(tree.charAt(i));
                while (Character.isDigit(tree.charAt(i+1))) {
                    node_builder.append(tree.charAt(i+1));
                    i++;
                }
                leaves.add(node_builder.toString());
            }
        }
        return leaves;
    }

    /**
     * Convers Newick format to Graph<Node, DefaultEdge>
     * @param newick
     * @return
     */
    public static Graph<TreeNode, DefaultEdge> convert(String newick) {
        DefaultUndirectedGraph<TreeNode, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        Stack<TreeNode> stack = new Stack<>();
        for (int i=0; i<newick.length();i++){
            char c = newick.charAt(i);
            if (c == '(') {
                TreeNode n = new TreeNode();
                graph.addVertex(n);
                if(!stack.isEmpty()){
                    TreeNode parent = stack.peek();
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

                TreeNode node = new TreeNode(node_builder.toString());
                graph.addVertex(node);
                TreeNode parent = stack.peek();
                graph.addEdge(parent, node);
            }
        }
        return graph;
    }


}
