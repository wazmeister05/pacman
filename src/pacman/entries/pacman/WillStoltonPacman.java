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
    int depth = 0;

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
        int lairNodeIndex = game.getGhostInitialNodeIndex();
        GHOST[] ghosts = GHOST.values();
        //buildTree(msPLocation, game);
        int[] allEdibles = getAllEdibles(game);

        //MOVE move = search(game, msPLocation, allEdibles, nonEdibleGhosts(game));
        MOVE move = check(game, msPLocation, ghosts, allEdibles);
        // if the move is null, there is a ghost in front of ms p
        if(move == null){

        }

        GHOST closestDangerousGhost = null;
        int closestDangerousGhostIndex = 0;
        int ghostDist = 10;

        GHOST nextClosestGhost = null;
        int nextClosestGhostIndex = Integer.MAX_VALUE;
        int nextClosestGhostDistance = -200;

        GHOST closestEdibleGhost = null;
        int closestEdibleGhostDistance = Integer.MAX_VALUE;
        int closestEdibleGhostIndex = 0;

        for(GHOST ghost: ghosts){
            // Get ghost location and remaining lair time
            int shortestPathDist = game.getShortestPathDistance(game.getGhostCurrentNodeIndex(ghost), msPLocation);
            int lairTime = game.getGhostLairTime(ghost);

            // if they're not in the lair and are closer than 10 away
            if(lairTime == 0  && shortestPathDist <= ghostDist){
                // If it isn't in the lair, it's after Ms P.
                closestDangerousGhost = ghost;
                ghostDist = shortestPathDist;
            }
            // If the ghost is edible, we can snack on it so get it's location
            else if(lairTime == 0 && game.getGhostEdibleTime(ghost) != 0){
                if(shortestPathDist < closestEdibleGhostDistance){
                    closestEdibleGhost = ghost;
                    closestEdibleGhostDistance = shortestPathDist;
                    closestEdibleGhostIndex = game.getGhostCurrentNodeIndex(ghost);
                }
            }
            // AVOID LAIR if a ghost is about to spawn.
            // Want to avoid this position if a ghost is about to leave the lair, more important than checking for nearby ghosts.
            else if(lairTime < 2 && game.getNeighbour(msPLocation, move) == lairNodeIndex){
                return game.getNextMoveAwayFromTarget(lairNodeIndex, msPLocation, DM.PATH);
            }
            else{
                if(nextClosestGhostDistance == Integer.MAX_VALUE || nextClosestGhostDistance < shortestPathDist){
                    nextClosestGhostDistance = shortestPathDist;
                    nextClosestGhostIndex = game.getGhostCurrentNodeIndex(ghost);
                    nextClosestGhost = ghost;
                }
            }
        }

        // If there is a closest ghost, run away from it. Now.
        if(closestDangerousGhost != null){
            return game.getNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(closestDangerousGhost), msPLocation, DM.PATH);
        }
        // But if there isn't, and there is an edible ghost, take that instead.
        else if(closestEdibleGhost != null){
            int[] pathToSnack = game.getShortestPath(msPLocation, closestEdibleGhostIndex);
            for(int entry : pathToSnack){
                if(entry == nextClosestGhostIndex){
                    return game.getNextMoveAwayFromTarget(nextClosestGhostIndex, msPLocation, DM.PATH);
                }
            }
            return game.getNextMoveTowardsTarget(msPLocation, closestEdibleGhostIndex, DM.PATH);
        }
        else{
            return move;
        }
       // return move;
    }


    /**
     * Search for the best route
     * @param game game object
     * @param msPLocation mrs P location
     * @return return a move to the AI
     */
    private MOVE search(Game game, int msPLocation, int[] allEdibles, ArrayList<Integer> dangerGhosts){
//        HashMap<MOVE, Integer> movesAndScores = new HashMap<>();
//        if(copy.isJunction(msPLocation)){
//            MOVE[] moves = copy.getPossibleMoves(msPLocation);
//            for(MOVE move : moves){
//                copy.updatePacMan(move);
//
//                if(!copy.wasPacManEaten()){
//                    movesAndScores.put(move, -200);
//                }
//                else if(copy.wasPowerPillEaten()){
//                    movesAndScores.put(move, 50);
//                }
//                else if(copy.wasPillEaten()){
//                    movesAndScores.put(move, 10);
//                }
//                else {
//                    movesAndScores.put(move, 1);
//                }
//            }
//
//            int maxValueInMap = (Collections.max(movesAndScores.values()));
//            for (Map.Entry<MOVE, Integer> entry : movesAndScores.entrySet()) {
//                if (entry.getValue() == maxValueInMap) {
//                    // Print the key with max value
//                    return entry.getKey();
//                }
//            }
//        }
        return game.getNextMoveTowardsTarget(msPLocation,
                game.getClosestNodeIndexFromNodeIndex(msPLocation, allEdibles, DM.PATH),
                DM.PATH);
    }


    public MOVE check(Game game, int msPLocation, GHOST[] ghosts, int[] edibles) {
        int target = game.getClosestNodeIndexFromNodeIndex(msPLocation, edibles, DM.PATH);
        int target2 = game.getClosestNodeIndexFromNodeIndex(target, edibles, DM.PATH);
        int[] path = game.getShortestPath(msPLocation, target2);

        boolean ghostExists = false;
        for (int step = 0; (step < path.length) && !ghostExists; step++) {
            for (GHOST ghost: ghosts) {
                if (path[step] == game.getGhostCurrentNodeIndex(ghost) && !game.isGhostEdible(ghost)) {
                    ghostExists = true;
                    //System.out.println("GHOST");
                    break;
                }
            }
        }

        // if no ghost in the way
        if (!ghostExists) {
            visitedJunctions.clear();
            GameView.addPoints(game, Color.MAGENTA, game.getShortestPath(msPLocation, target2));
            return game.getNextMoveTowardsTarget(msPLocation, target2, DM.PATH);
        }
        return null;
    }

    public static ArrayList<Integer> visitedJunctions = new ArrayList<Integer>();

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
        int[] pills = game.getActivePillsIndices();
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
        int[] powerPills = game.getActivePowerPillsIndices();

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





