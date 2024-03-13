package uMAF;

public class Node {
    public static int prev = 0;
    public int id;
    public String name = "";

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
        if(name.equals("")){
            return "internal"+id;
        }
        return name;
    }
}
