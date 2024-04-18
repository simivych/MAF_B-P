package uMAF;

public class Node {
    public static int prev = 501; // MAKE SURE THIS IS ABOVE THE NUMBER OF LEAF NODES
    public int id; // unique to each node
    public String name = ""; // label of leaf node (empty if internal)

    public Node(){
        this.id = prev;
        prev++;
        this.name = STR."internal\{id}";;
    }

    public Node(String name){
        this.id = Integer.parseInt(name);
        this.name = name;
    }
    @Override
    public boolean equals(Object object){
        if (object == null || object.getClass() != getClass()) {
            return false;
        } else {
            Node node = (Node) object;
            return this.name.equals(node.name);
        }
    }

    public String toString(){
        return name;
    }
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public boolean isInternal(){
        return name.contains("internal");
    }
}
