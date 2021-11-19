package pacman.entries.pacman;
/**
 Tree class for making the moves tree
 */

public class Graph {
    private Node root;


    /**
     * Tree constructor
     */
    Graph() {
    }

    /**
     * get root node
     * @return root node
     */
    Node getRoot() {
        return root;
    }

    /**
     * set the root node
     * @param root root node
     */
    void setRoot(Node root) {
        this.root = root;
    }

    void printTree(Node root){
        System.out.println(root.getIndex());
        for(Node child: root.getChildren()){
            System.out.println(child.getIndex());
        }
    }
}