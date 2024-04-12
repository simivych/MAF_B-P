package uMAF;

public class Node {
    public static int prev = 0;
    public int id; // unique to each node
    public String name = ""; // label of leaf node (empty if internal)

    public Node(){
        this.id = prev;
        prev++;
    }

    public Node(String name){
        this.id = prev;
        prev++;
        this.name = name;
    }

    public boolean equals(Node node){
        return this.name.equals(node.name);
    }

    public String toString(){
        if(name.isEmpty()){
            return STR."internal\{id}";
        }
        return name;
    }
}
