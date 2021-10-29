package pacman.entries.pacman;

import pacman.controllers.Controller;
import pacman.game.Constants.MOVE;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.DM;
import pacman.game.Game;

import java.util.ArrayList;

public class WillStoltonPacman extends Controller<MOVE> {

    private MOVE myMove= MOVE.NEUTRAL;

    private int previousLocation = 0;

    public MOVE getMove(Game game, long timeDue)
    {
        previousLocation = game.getPacmanCurrentNodeIndex();
        //Place your game logic here to play the game as Ms Pac-Man
        /*
        The brief suggests trying to implement simple rules first.
            - collect pills and power pills
            - avoid inedible ghosts
            - eat edible ghosts
        I feel the best order here would be to avoid ghosts first, then eat ghosts, then eat pills.
         */

        // avoid ghosts
        for(GHOST ghost: GHOST.values()){

            // if the ghosts are not edible, we need to run so check if it's in the lair
            if(game.getGhostLairTime(ghost) == 0){
                // if it isn't in the lair, it's after Ms P.
                if (game.getManhattanDistance(game.getGhostCurrentNodeIndex(ghost),
                        game.getPacmanCurrentNodeIndex()) < 40){
                    // if the ghost in question is closer than 10, we need to evade it
                    return game.getNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghost),
                            previousLocation, DM.PATH);
                }
                //TODO: look at previous location use above.
            }
        }

        //TODO: this came from the internet
        // prep for eating pills
        int[] pills = game.getPillIndices();
        int[] powerUps = game.getPowerPillIndices();

        ArrayList<Integer> nextPill = new ArrayList<>();

        for(int i = 0; i < pills.length; i++){
            if(game.isPillStillAvailable(i)){
                nextPill.add(pills[i]);
            }
        }
        for(int i = 0; i < powerUps.length; i++){
            if(game.isPowerPillStillAvailable(i)){
                nextPill.add(powerUps[i]);
            }
        }

        int[] targets = new int[nextPill.size()];

        for(int i = 0; i < targets.length; i++){
            targets[i] = nextPill.get(i);
        }

        // eat ghosts
        //TODO: Currently with this pacman eschews pills for ghosts regardless of distance
        for(GHOST ghost: GHOST.values()){
            // if the ghost has more than 0 seconds being edible, head to it
            if(game.isGhostEdible(ghost) && game.getGhostEdibleTime(ghost) != 0){
                if(game.getManhattanDistance(game.getPacmanCurrentNodeIndex(), game.getGhostCurrentNodeIndex(ghost))
                        <
                        game.getManhattanDistance(game.getPacmanCurrentNodeIndex(), game.getPillIndex(targets[0]))){
                    return game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(),
                            game.getGhostCurrentNodeIndex(ghost), DM.PATH);
                }
            }
        }

        return game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(),
                game.getClosestNodeIndexFromNodeIndex(game.getPacmanCurrentNodeIndex(), targets, DM.PATH),
                DM.PATH);
    }

}
