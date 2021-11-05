/*
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
package pacman.entries.pacman;

import pacman.controllers.Controller;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.DM;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import java.util.*;

public class WillStoltonPacman extends Controller<MOVE> {

    public MOVE getMove(Game game, long timeDue)
    {
        // We'll need this a lot throughout so making it easy.
        int msPLocation = game.getPacmanCurrentNodeIndex();

        //////////////////////////////////// NEED THIS?
        /*
        AVOID LAIR if a ghost is about to spawn.
         */
        // Want to avoid this position if a ghost is about to leave the lair.
        int lairNodeIndex = game.getGhostInitialNodeIndex();
        for(GHOST ghost: GHOST.values()){
            if(game.getGhostLairTime(ghost) < 1 && Arrays.asList(game.getNeighbouringNodes(msPLocation)).contains(lairNodeIndex)){
                return game.getNextMoveAwayFromTarget(lairNodeIndex, msPLocation, DM.PATH);
            }
        }


        /*
        RUN AWAY.
         */
        // Need to avoid ghosts first and foremost. No point doing everything else if there are ghosts nearby.
        GHOST closestDangerousGhost = null;
        int closestGhostIndex = 0;
        int nextClosestGhostIndex = 0;
        int ghostDist = 12;
        for(GHOST ghost: GHOST.values()){
            // If the ghost is still in the lair or edible, we can ignore it.
            if(game.getGhostLairTime(ghost) == 0 && game.getGhostEdibleTime(ghost) == 0){
                // If it isn't in the lair, it's after Ms P.
                int shortestPathDist = game.getShortestPathDistance(game.getGhostCurrentNodeIndex(ghost), msPLocation);
                if (shortestPathDist < ghostDist){
                    closestDangerousGhost = ghost;
                    ghostDist = shortestPathDist;
                    closestGhostIndex = game.getGhostCurrentNodeIndex(ghost);
                }
                else if(shortestPathDist == ghostDist){
                    nextClosestGhostIndex = game.getGhostCurrentNodeIndex(ghost);
                }
            }
        }
        // If there is a closest ghost, run away from it. Now.
        if(closestDangerousGhost != null){
            return game.getNextMoveAwayFromTarget(closestGhostIndex, msPLocation, DM.PATH);
        }


        /*
        EAT GHOST - VERY similar to running away.
         */
        // If we've eaten a power pill, ghosts can be consumed, so lets go for them.
        GHOST closestEdibleGhost = null;
        int closestGhostDistance = Integer.MAX_VALUE;
        int closestEdibleGhostIndex = 0;
        for(GHOST ghost: GHOST.values()){
            // if the ghost has more than 0 seconds being edible, head to it
            if(game.getGhostEdibleTime(ghost) > 0){
                int shortestPath = game.getShortestPathDistance(msPLocation, game.getGhostCurrentNodeIndex(ghost));
                if(shortestPath < closestGhostDistance){
                    closestEdibleGhost = ghost;
                    closestGhostDistance = shortestPath;
                    closestEdibleGhostIndex = game.getGhostCurrentNodeIndex(ghost);
                }
            }
        }

        // If there is an edible ghost, snack, but not if there is a dangerous ghost in the way.
        if(closestEdibleGhost != null) {
            int[] pathToSnack = game.getShortestPath(msPLocation, closestEdibleGhostIndex);
            for(int entry : pathToSnack){
                if(entry == nextClosestGhostIndex){
                    return game.getNextMoveAwayFromTarget(nextClosestGhostIndex, msPLocation, DM.PATH);
                }
            }
            return game.getNextMoveTowardsTarget(msPLocation, closestEdibleGhostIndex, DM.PATH);
        }


        /*
        EAT PILLS
         */
        // If the above two sections don't return anything, we want to return an action to go for pills.
        return search(game, msPLocation);
    }


    // Get the index of active pills still in play
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


    // Get the index of active power pills still in play
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


    // Get the index of the ghosts
    private ArrayList<Integer> allGhosts(Game game) {
        ArrayList<Integer> ghosts = new ArrayList<>();
        for (GHOST ghost : GHOST.values()) {
            ghosts.add(game.getGhostCurrentNodeIndex(ghost));
        }
        return ghosts;
    }


    // Search for the best route
    private MOVE search(Game game, int msPLocation){
        ArrayList<Integer> allPills = routeToPills(game);
        ArrayList<Integer> allPowerPills = routeToPowerPills(game);
        ArrayList<Integer> ghosts = allGhosts(game);
        int[] targets = new int[allPills.size() + allPowerPills.size()];
        for (int i = 0; i < allPills.size(); i++) {
            targets[i] = allPills.get(i);
        }
        for (int i = allPills.size(); i < allPowerPills.size(); i++) {
            targets[i] = allPowerPills.get(i);
        }

        PriorityQueue<Integer> frontier = new PriorityQueue<>();
        PriorityQueue<Integer> visited = new PriorityQueue<>();
        frontier.add(msPLocation);
        ArrayList<Integer> solutionIndexes = new ArrayList<>();
        int score = 0;

//        while(!frontier.isEmpty()){
//            int location = frontier.remove();
//            int[] neighbours = game.getNeighbouringNodes(location);
//            for(int entry : neighbours){
//                if(allPills.contains(entry)){
//                    score += 10;
//                }
//                else if(allPowerPills.contains(entry)){
//                    score += 50;
//                }
//                else if(ghosts.contains(entry)){
//                    score -= 200;
//                }
//                frontier.add(entry);
//            }
//        }

        // default return so I can test that I still get 50%
        return game.getNextMoveTowardsTarget(msPLocation,
                game.getClosestNodeIndexFromNodeIndex(msPLocation, targets, DM.PATH),
                DM.PATH);
    }
}