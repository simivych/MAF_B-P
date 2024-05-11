package uMAF;

public class TreeNode {
    public static int prev = 501; // MAKE SURE THIS IS ABOVE THE NUMBER OF LEAF NODES
    public int id; // unique to each node
    public String name = ""; // label of leaf node (empty if internal)

    public TreeNode(){
        this.id = prev;
        prev++;
        this.name = STR."internal\{id}";;
    }

    public TreeNode(String name){
        this.id = Integer.parseInt(name);
        this.name = name;
    }
    @Override
    public boolean equals(Object object){
        if (object == null || object.getClass() != getClass()) {
            return false;
        } else {
            TreeNode node = (TreeNode) object;
            return this.id == node.id;
        }
    }
    public boolean is_equal(Object object){
        if (object == null || object.getClass() != getClass()) {
            return false;
        }
        TreeNode node = (TreeNode) object;
        if(node.isInternal()&&isInternal()) {
            return true;
        } else {
            return this.id == node.id;
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
