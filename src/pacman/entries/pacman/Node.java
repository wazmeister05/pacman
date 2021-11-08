package pacman.entries.pacman;

import java.util.ArrayList;
import java.util.List;
/**
 * Node class for making nodes for tree
 */

public class Node {

    private boolean isMaxPlayer;
    private int score;
    private List<Node> children;
    private int index;

    public Node(int index) {
        this.index = index;
        this.isMaxPlayer = true;
        this.score = 0;
        this.children = new ArrayList<>();
    }

    boolean isMaxPlayer() {
        return isMaxPlayer;
    }

    int getIndex() {
        return index;
    }

    int getScore() {
        return score;
    }

    void setScore(int score) {
        this.score = score;
    }

    List<Node> getChildren() {
        return children;
    }

    void addChild(Node newNode) {
        children.add(newNode);
    }

}