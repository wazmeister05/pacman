package pacman.entries.pacman;

import pacman.controllers.Controller;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.DM;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.GameView;
import java.awt.*;
import java.util.*;


/**
The brief suggests trying to implement simple rules first.
    - collect pills and power pills
    - avoid inedible ghosts
    - eat edible ghosts
I feel the best order here would be to avoid the lair first, then ghosts, then eat ghosts, then eat pills.
Why?
- If an enemy is about to spawn as we pass the lair, we'll die.
- Getting caught by a ghost removes one of only a handful of lives, so surviving is the most important part.
- Then, if ghosts are edible, they are worth more AND remove the ghost from play temporarily.
- Finally, if there are no ghosts of concern, keep eating as standard.
 */

public class WillStoltonPacman extends Controller<MOVE> {

    private Tree tree;
    private Set<Integer> visited;
    private GHOST ghostTarget;
    private MOVE chosenMove;


    /**
     * Main AI logic. Avoid the lair or other ghosts, then chase edible ghosts and finally
     * get the best route to the pills.
     * @param game A copy of the current game
     * @param timeDue The time the next move is due
     * @return the move for the AI to follow
     */
    public MOVE getMove(Game game, long timeDue)
    {
        // We'll need these throughout so make them now.
        int msPLocation = game.getPacmanCurrentNodeIndex();
        GHOST[] ghosts = GHOST.values();
        //buildTree(msPLocation, game);

        Map<GHOST,Integer> edible = new HashMap<>();
        Map<GHOST,Integer> inedible = new HashMap<>();

        for(GHOST ghost: ghosts){
            if(game.getGhostLairTime(ghost) == 0) {
                int ghostIndex = game.getGhostCurrentNodeIndex(ghost);
                if (!game.isGhostEdible(ghost)) {
                    inedible.put(ghost, game.getShortestPathDistance(ghostIndex, msPLocation));
                }
                else{
                    edible.put(ghost, game.getShortestPathDistance(ghostIndex, msPLocation));
                }
            }
        }

        // If there is a closest ghost, run away from it. Now.
        for(Map.Entry<GHOST, Integer> entry : inedible.entrySet()){
            if(entry.getValue() <= 10){
                return game.getNextMoveAwayFromTarget(msPLocation, game.getGhostCurrentNodeIndex(entry.getKey()), DM.PATH);
            }
        }

        for (Map.Entry<GHOST, Integer> entry : edible.entrySet()) {
            boolean routeFound = false;
            if (entry.getValue() <= 200) {
                ghostTarget = entry.getKey();
                //GameView.addPoints(game, Color.RED, game.getShortestPath(msPLocation, game.getGhostCurrentNodeIndex(entry.getKey())));
                routeFound = check(ghostTarget, game);
                if(routeFound) {
                    return chosenMove;
                }
            }
        }

        int i = 0;
        int[] allEdibles = getAllEdibles(game);
        boolean routeFound = false;
        while(i < allEdibles.length){
            int[] ediblesTrun = Arrays.copyOfRange(allEdibles, i, allEdibles.length);
            try {
                routeFound = check(game, ediblesTrun, msPLocation, ghosts);
            }
            catch(Exception e){
                routeFound = false;
            }
            if(routeFound){
                break;
            }
            i++;
        }
        if(routeFound) {
            return chosenMove;
        }
        else{
            return game.getNextMoveTowardsTarget(msPLocation,
                game.getClosestNodeIndexFromNodeIndex(msPLocation, allEdibles, DM.PATH),
                DM.PATH);
        }
    }


    private boolean check(Game game, int[] edibles, int msPLocation, GHOST[] ghosts) {
        int target = game.getClosestNodeIndexFromNodeIndex(msPLocation, edibles, DM.PATH);
        int[] path = game.getShortestPath(msPLocation, target);

        boolean ghostExists = false;
        for (int i = 0; (i < path.length) && !ghostExists; i++) {
            for (GHOST ghost: ghosts) {
                if (path[i] == game.getGhostCurrentNodeIndex(ghost) && !game.isGhostEdible(ghost)) {
                    ghostExists = true;
                    break;
                }
            }
        }
        // if no ghost in the way
        if (!ghostExists) {
            GameView.addPoints(game, Color.MAGENTA, game.getShortestPath(msPLocation, target));
            chosenMove = game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(), target, DM.PATH);
            return true;
        }
        return false;
    }

    private boolean check(GHOST ghostToEat, Game game){
        int target = game.getGhostCurrentNodeIndex(ghostToEat);
        int msPLocation = game.getPacmanCurrentNodeIndex();
        int[] path = game.getShortestPath(game.getPacmanCurrentNodeIndex(), target);

        boolean ghostExists = false;
        for (int i = 0; (i < path.length) && !ghostExists; i++) {
            for (GHOST ghost: GHOST.values()) {
                if (path[i] == game.getGhostCurrentNodeIndex(ghost) && !game.isGhostEdible(ghost)) {
                    ghostExists = true;
                    break;
                }
            }
        }
        // if no ghost in the way
        if (!ghostExists) {
            GameView.addPoints(game, Color.MAGENTA, game.getShortestPath(msPLocation, target));
            chosenMove = game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(), target, DM.PATH);
            return true;
        }
        return false;
    }


    /////////////////////////////////////////////////////////////////////////////
    /////////////////  Create and build arrays and stuff  ///////////////////////
    /////////////////////////////////////////////////////////////////////////////


    /**
     * Get the index of active pills still in play
     * @param game game object
     * @return arraylist of pill locations
     */
    private ArrayList<Integer> pills(Game game) {
        ArrayList<Integer> allPills = new ArrayList<>();
        int[] pills = game.getPillIndices();
        for (int i = 0; i < pills.length; i++) {
            if (game.isPillStillAvailable(i)) {
                allPills.add(pills[i]);
            }
        }
        return allPills;
    }


    /**
     * Get the index of active power pills still in play
     * @param game game object
     * @return arraylist of power pill locations
     */
    private ArrayList<Integer> powerPills(Game game) {
        ArrayList<Integer> allPills = new ArrayList<>();
        int[] powerPills = game.getPowerPillIndices();

        for (int i = 0; i < powerPills.length; i++) {
            if (game.isPowerPillStillAvailable(i)) {
                allPills.add(powerPills[i]);
            }
        }
        return allPills;
    }


    /**
     * Get the index of the ghosts
     * @param game game object
     * @return arraylist of ghost locations
     */
    private ArrayList<Integer> nonEdibleGhosts(Game game) {
        ArrayList<Integer> ghosts = new ArrayList<>();
        for (GHOST ghost : GHOST.values()) {
            if(!game.isGhostEdible(ghost))
                ghosts.add(game.getGhostCurrentNodeIndex(ghost));
        }
        return ghosts;
    }


    /**
     * Get the index of the ghosts that are edible
     * @param game game object
     * @return arraylist of ghost locations
     */
    private ArrayList<Integer> edibleGhosts(Game game) {
        ArrayList<Integer> ghosts = new ArrayList<>();
        for (GHOST ghost : GHOST.values()) {
            if(game.isGhostEdible(ghost)) {
                ghosts.add(game.getGhostCurrentNodeIndex(ghost));
            }
        }
        return ghosts;
    }


    /**
     * Create the root node and begin building the tree
     * @param msPLocation index of mrs p
     * @param game game object
     */
    private void buildTree(int msPLocation, Game game){
        tree = new Tree();
        Node root = new Node(msPLocation);
        tree.setRoot(root);
        visited = new HashSet<>();
        ArrayList<Integer> ghosts = nonEdibleGhosts(game);
        ArrayList<Integer> edibleGhosts = edibleGhosts(game);
        ArrayList<Integer> pills = pills(game);
        ArrayList<Integer> powerPills = powerPills(game);
        buildTree(root, game, ghosts, edibleGhosts, pills, powerPills);
    }


    /**
     * Continue to build the tree after having added the root
     * @param parent parent node object
     * @param game game object
     * @param ghosts list of ghost locations
     * @param pills list of pill locations
     * @param powerPills list of powerpill locations
     */
    private void buildTree(Node parent, Game game, ArrayList<Integer> ghosts, ArrayList<Integer> edibleGhosts, ArrayList<Integer> pills, ArrayList<Integer> powerPills){
        int index = parent.getIndex();
        if(!visited.contains(index)) {
            visited.add(index);
            if (ghosts.contains(index)) {
                parent.setScore(-200);
            } else if(edibleGhosts.contains(index)) {
                parent.setScore(200);
            } else if (powerPills.contains(index)) {
                parent.setScore(50);
            } else if (pills.contains(index)) {
                parent.setScore(10);
            }
            else{
                parent.setScore(1);
            }
            int[] children = game.getNeighbouringNodes(index);
            for (int child : children) {
                Node newNode = new Node(child);
                parent.addChild(newNode);
                buildTree(newNode, game, ghosts, edibleGhosts, pills, powerPills);
            }
        }
    }


    /**
     * @param game game object
     * @return list of all pills, powerpills and edible ghosts
     */
    private int[] getAllEdibles(Game game) {
        ArrayList<Integer> pills = pills(game);
        ArrayList<Integer> powerPills = powerPills(game);
        ArrayList<Integer> edibleGhostLocs = edibleGhosts(game);
        int pSize = pills.size();
        int ppSize = powerPills.size();
        int gSize = edibleGhostLocs.size();
        int[] allEdibles = new int[pSize + ppSize + gSize];

        for(int i = 0; i < gSize; i++){
            allEdibles[i] = edibleGhostLocs.get(i);
        }
        for(int i = 0; i < ppSize; i++){
            allEdibles[i+gSize] = powerPills.get(i);
        }
        for(int i = 0; i < pSize; i++){
            allEdibles[i+gSize+ppSize] = pills.get(i);
        }


        return allEdibles;
    }
}