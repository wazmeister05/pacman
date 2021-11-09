package pacman.entries.pacman;

import pacman.controllers.Controller;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.DM;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
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

    /**
     * Main AI logic. Avoid the lair or other ghosts, then chase edible ghosts and finally
     * get the best route to the pills.
     * @param game A copy of the current game
     * @param timeDue The time the next move is due
     * @return the move for the AI to follow
     */
    public MOVE getMove(Game game, long timeDue)
    {
        // We'll need this a lot throughout so making it easy.
        int msPLocation = game.getPacmanCurrentNodeIndex();

        GHOST[] ghosts = GHOST.values();

        // Now build the tree for the later search
        // buildTree(msPLocation, game);

        // AVOID LAIR if a ghost is about to spawn.
        // Want to avoid this position if a ghost is about to leave the lair.
        int lairNodeIndex = game.getGhostInitialNodeIndex();
//        for(GHOST ghost: ghosts){
//            if(game.getGhostLairTime(ghost) < 2 && Arrays.asList(game.getNeighbouringNodes(msPLocation)).contains(lairNodeIndex)){
//                return game.getNextMoveAwayFromTarget(lairNodeIndex, msPLocation, DM.PATH);
//            }
//        }
        // RUN AWAY
        // Need to avoid ghosts first and foremost. No point doing everything else if there are ghosts nearby.
        GHOST closestDangerousGhost = null;
        int closestGhostIndex = 0;
        int nextClosestGhostIndex = 0;
        int ghostDist = 10;
        GHOST closestEdibleGhost = null;
        int closestGhostDistance = Integer.MAX_VALUE;
        int closestEdibleGhostIndex = 0;

        for(GHOST ghost: ghosts){
            // If the ghost is still in the lair or edible, we can ignore it.
            int shortestPathDist = game.getShortestPathDistance(game.getGhostCurrentNodeIndex(ghost), msPLocation);
            if(game.getGhostLairTime(ghost) == 0  && shortestPathDist < ghostDist){
                // If it isn't in the lair, it's after Ms P.
                closestDangerousGhost = ghost;
                ghostDist = shortestPathDist;
                closestGhostIndex = game.getGhostCurrentNodeIndex(ghost);

            }
            else if(game.getGhostLairTime(ghost) == 0 && game.getGhostEdibleTime(ghost) != 0){
                if(shortestPathDist < closestGhostDistance){
                    closestEdibleGhost = ghost;
                    closestGhostDistance = shortestPathDist;
                    closestEdibleGhostIndex = game.getGhostCurrentNodeIndex(ghost);
                }
            }
            else if(game.getGhostLairTime(ghost) < 3 && Arrays.asList(game.getNeighbouringNodes(msPLocation)).contains(lairNodeIndex)){
                return game.getNextMoveAwayFromTarget(lairNodeIndex, msPLocation, DM.PATH);
            }
            else{
                nextClosestGhostIndex = game.getGhostCurrentNodeIndex(ghost);
            }
        }
        // If there is a closest ghost, run away from it. Now.
        if(closestDangerousGhost != null){
            return game.getNextMoveAwayFromTarget(closestGhostIndex, msPLocation, DM.PATH);
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
            return search(game, msPLocation);
        }
    }


    /**
     * Get the index of active pills still in play
     * @param game game object
     * @return arraylist of pill locations
     */
    private ArrayList<Integer> routeToPills(Game game) {
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
    private ArrayList<Integer> routeToPowerPills(Game game) {
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
    private ArrayList<Integer> allGhosts(Game game) {
        ArrayList<Integer> ghosts = new ArrayList<>();
        for (GHOST ghost : GHOST.values()) {
            ghosts.add(game.getGhostCurrentNodeIndex(ghost));
        }
        return ghosts;
    }


    /**
     * Gets the tree
     * @return tree object
     */
    public Tree getTree(){
        return tree;
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
        ArrayList<Integer> ghosts = allGhosts(game);
        ArrayList<Integer> pills = routeToPills(game);
        ArrayList<Integer> powerPills = routeToPowerPills(game);
        buildTree(root, game, ghosts, pills, powerPills);
    }


    /**
     * Continue to build the tree after having added the root
     * @param parent parent node object
     * @param game game object
     * @param ghosts list of ghost locations
     * @param pills list of pill locations
     * @param powerPills list of powerpill locations
     */
    private void buildTree(Node parent, Game game, ArrayList<Integer> ghosts, ArrayList<Integer> pills, ArrayList<Integer> powerPills){
        int index = parent.getIndex();
        if(!visited.contains(index)) {
            visited.add(index);
            if (ghosts.contains(index)) {
                parent.setScore(-200);
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
                buildTree(newNode, game, ghosts, pills, powerPills);
            }
        }
    }


    /**
     * Search for the best route
     * @param game game object
     * @param msPLocation mrs P location
     * @return return a move to the AI
     */
    private MOVE search(Game game, int msPLocation){

        /*
        keep this for now
         */
        ArrayList<Integer> pills = routeToPills(game);
        ArrayList<Integer> powerPills = routeToPowerPills(game);
        int[] finalRoute = new int[pills.size() + powerPills.size()];
        for(int i = 0; i < pills.size(); i++){
            finalRoute[i] = pills.get(i);
        }
        for(int i = 0; i < powerPills.size(); i++){
            finalRoute[i] = powerPills.get(i);
        }
        return game.getNextMoveTowardsTarget(msPLocation,
                game.getClosestNodeIndexFromNodeIndex(msPLocation, finalRoute, DM.PATH),
                DM.PATH);
        /*
        EOF
         */

//        Node start = tree.getRoot();
//        int destination = execute(start);
//
//        if(destination == -200){
//            return game.getNextMoveAwayFromTarget(destination, msPLocation, DM.PATH);
//        }
//        else if(destination == -1){
//            return MOVE.NEUTRAL;
//        }
//        else {
//            return game.getNextMoveTowardsTarget(msPLocation,
//                    game.getClosestNodeIndexFromNodeIndex(msPLocation, finalRoute, DM.PATH),
//                    DM.PATH);
//            //return game.getNextMoveTowardsTarget(msPLocation, destination, DM.PATH);
//        }

    }

    int depth = 0;

    public int execute(Node startNode) {
        Stack<Node> nodeStack = new Stack<>();
        ArrayList<Node> visitedNodes = new ArrayList<>();
        nodeStack.add(startNode);

        depth = 0;

        while (!nodeStack.isEmpty()) {
            if (depth <= 10) {
                Node current = nodeStack.pop();
                if (current.getScore() == -200) {
                    System.out.print(visitedNodes);
                    System.out.println("Enemy node found");
                    return current.getIndex();
                } else {
                    visitedNodes.add(current);
                    nodeStack.addAll(current.getChildren());
                    depth++;
                }
            } else {
                return visitedNodes.get(visitedNodes.size() - 1).getIndex();
            }
        }
        return -1;
    }
}
