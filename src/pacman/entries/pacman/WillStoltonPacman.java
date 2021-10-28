package pacman.entries.pacman;

import pacman.controllers.Controller;
import pacman.game.Constants.MOVE;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.DM;
import pacman.game.Game;
import pacman.game.internal.Ghost;

import java.util.ArrayList;

public class WillStoltonPacman extends Controller<MOVE> {

    private MOVE myMove= MOVE.NEUTRAL;

    private int previousLocation = 0;

    public MOVE getMove(Game game, long timeDue)
    {
        //Place your game logic here to play the game as Ms Pac-Man
        /*
        The brief suggests trying to implement simple rules first.
            - collect pills and power pills
            - avoid inedible ghosts
            - eat edible ghosts
        I feel the best order here would be to avoid ghosts first, then eat ghosts, then eat pills.
         */

        // But first Ms.Pacman's location is required
        int msPacmanLocation = game.getPacmanCurrentNodeIndex();

        // avoid ghosts
        for(GHOST ghost: GHOST.values()){
            if(game.getGhostEdibleTime(ghost) > 0) {
                // there's no need to continue this loop if the ghosts are edible
                break;
            }
            // if the ghosts are not edible, we need to run so check if it's in the lair
            if(game.getGhostLairTime(ghost) == 0){
                // if it isn't in the lair, it's after Ms P.
                if (game.getShortestPathDistance(game.getGhostCurrentNodeIndex(ghost), msPacmanLocation) < 40){
                    // if the ghost in question is closer than 10, we need to evade it
                    return game.getNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghost),
                            previousLocation, DM.PATH);
                }
                //TODO: look at previous location use above.
            }
        }

        // eat ghosts
        for(GHOST ghost: GHOST.values()){
            if(game.isGhostEdible(ghost) && game.getGhostEdibleTime(ghost) >= 1 ){

            }
        }


        // eat pills
        int[] pills = game.getActivePillsIndices();
        int[] powerUps = game.getActivePowerPillsIndices();

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






        // set msPacman's previous location at the end of the go. This will possibly be used for running away
        previousLocation = msPacmanLocation;
        return game.getNextMoveTowardsTarget(msPacmanLocation, targets[0], DM.PATH);
    }

}
