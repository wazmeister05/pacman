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
import java.util.ArrayList;
import java.util.Arrays;

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
        return routeToPills(game, msPLocation);
    }


    private MOVE routeToPills(Game game, int msPLocation) {
        ArrayList<Integer> nextPill = new ArrayList<>();
        int[] pills = game.getPillIndices();
        int[] powerUps = Arrays.stream(game.getPowerPillIndices()).toArray();

        for (int i = 0; i < pills.length; i++) {
            if (game.isPillStillAvailable(i)) {
                nextPill.add(pills[i]);
            }
        }
        for (int i = 0; i < powerUps.length; i++) {
            if (game.isPowerPillStillAvailable(i)) {
                nextPill.add(powerUps[i]);
            }
        }
        int[] targets = new int[nextPill.size()];
        for (int i = 0; i < targets.length; i++) {
            targets[i] = nextPill.get(i);
        }

        if (game.isJunction(msPLocation)) {
            //////////////////////////////////////////////////////////////////////////////////// FIX THIS
            //MOVE[] moves = game.getPossibleMoves(msPLocation);
            int[] path = game.getShortestPath(msPLocation, targets[0]);

        } else {
            return game.getNextMoveTowardsTarget(msPLocation,
                    game.getClosestNodeIndexFromNodeIndex(msPLocation, targets, DM.PATH),
                    DM.PATH);
        }
    }

    private void Pathfinder(){

    }

}