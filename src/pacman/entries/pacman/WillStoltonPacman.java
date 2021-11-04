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
import pacman.game.internal.AStar;
import pacman.game.internal.Maze;
import pacman.game.internal.Node;

import java.util.*;
import java.util.stream.IntStream;

public class WillStoltonPacman extends Controller<MOVE> {

    private MOVE myMove=MOVE.NEUTRAL;

    public MOVE getMove(Game game, long timeDue)
    {
        // We'll need this a lot throughout so making it easy.
        int msPLocation = game.getPacmanCurrentNodeIndex();
        // Want to avoid this position if a ghost is about to leave the lair.
        int lairNodeIndex = game.getGhostInitialNodeIndex();

        /*
        AVOID LAIR if a ghost is about to spawn.
         */
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
        int ghostDist = 10;
        for(GHOST ghost: GHOST.values()){
            // If the ghost is still in the lair, we can ignore it.
            if(game.getGhostLairTime(ghost) == 0 && game.getGhostEdibleTime(ghost) == 0){
                // If it isn't in the lair, it's after Ms P.
                if (game.getShortestPathDistance(game.getGhostCurrentNodeIndex(ghost), msPLocation) < ghostDist){
                    closestDangerousGhost = ghost;
                    ghostDist = game.getShortestPathDistance(game.getGhostCurrentNodeIndex(ghost), msPLocation);
                }
            }
        }
        // If there is a closest ghost, run away from it. Now.
        if(closestDangerousGhost != null){
            return game.getNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(closestDangerousGhost), msPLocation, DM.PATH);
        }

        /*
        EAT GHOST - VERY similar to running away.
         */
        // If we've eaten a power pill, ghosts can be consumed, so lets go for them.
        GHOST closestEdibleGhost = null;
        int closestGhostDistance = Integer.MAX_VALUE;
        for(GHOST ghost: GHOST.values()){
            // if the ghost has more than 0 seconds being edible, head to it
            if(game.getGhostEdibleTime(ghost) > 0){
                if(game.getShortestPathDistance(msPLocation, game.getGhostCurrentNodeIndex(ghost)) < closestGhostDistance){
                    closestEdibleGhost = ghost;
                    closestGhostDistance = game.getShortestPathDistance(msPLocation, game.getGhostCurrentNodeIndex(ghost));
                }
            }
        }
        // If there is an edible ghost, snack.
        if(closestEdibleGhost != null) {
            return game.getNextMoveTowardsTarget(msPLocation,
                    game.getGhostCurrentNodeIndex(closestEdibleGhost), DM.PATH);
        }

        /*
        EAT PILLS
         */
        // If the above two sections don't return anything, we want to return an action to go for pills.
        ArrayList<Integer> allPills = routeToPills(game);
        ArrayList<Integer> allPowerPills = routeToPowerPills(game);
        ArrayList<Integer> ghosts = allGhosts(game);
        return search(game.copy(), msPLocation, allPills, allPowerPills, ghosts);
    }

//    private ArrayList<Integer> routeToPills(Game game, int msPLocation) {
//        ArrayList<Integer> allPills = new ArrayList<>();
//        int[] pills = game.getActivePillsIndices();
//        int[] powerPills = game.getActivePowerPillsIndices();
//
//        for (int i = 0; i < pills.length; i++) {
//            if (game.isPillStillAvailable(i)) {
//                allPills.add(pills[i]);
//            }
//        }
//        for (int i = 0; i < powerPills.length; i++) {
//            if (game.isPowerPillStillAvailable(i)) {
//                allPills.add(powerPills[i]);
//            }
//        }
//
//        int[] targets = new int[allPills.size()];
//        for (int i = 0; i < targets.length; i++) {
//            targets[i] = allPills.get(i);
//        }
//
//        return allPills;
//    }
//
//
//    private MOVE search(Game copy, int msPLocation, ArrayList<Integer> pills){
//
//        PriorityQueue<Integer> frontier = new PriorityQueue<>();
//        PriorityQueue<Integer> visited = new PriorityQueue<>();
//        frontier.add(msPLocation);
//        ArrayList<Integer> solutionIndexes = new ArrayList<>();
//
//        while(!frontier.isEmpty()){
//            int location = frontier.remove();
//            int[] neighbours = copy.getNeighbouringNodes(location);
//            for(int entry : neighbours){
//                if()
//                    frontier.add(entry);
//            }
//        }
//        return null;
//    }

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

    private ArrayList<Integer> allGhosts(Game game) {
        ArrayList<Integer> ghosts = new ArrayList<>();
        for (GHOST ghost : GHOST.values()) {
            ghosts.add(game.getGhostCurrentNodeIndex(ghost));
        }
        return ghosts;
    }


    private MOVE search(Game copy, int msPLocation, ArrayList<Integer> allPills, ArrayList<Integer> allPowerPills, ArrayList<Integer> allGhosts){

//        int[] targets = new int[allPills.size()];
//        for (int i = 0; i < targets.length; i++) {
//            targets[i] = allPills.get(i);
//        }

        PriorityQueue<Integer> frontier = new PriorityQueue<>();
        PriorityQueue<Integer> visited = new PriorityQueue<>();
        frontier.add(msPLocation);
        ArrayList<Integer> solutionIndexes = new ArrayList<>();
        int score = 0;
        while(!frontier.isEmpty()){
            int location = frontier.remove();
            int[] neighbours = copy.getNeighbouringNodes(location);
            for(int entry : neighbours){
                if(allPills.contains(entry)){
                    score += 10;
                }
                else if(allPowerPills.contains(entry)){
                    score += 50;
                }
                else if(allGhosts.contains(entry)){
                    score -= 200;
                }
                frontier.add(entry);
            }
        }
        return null;
    }
}