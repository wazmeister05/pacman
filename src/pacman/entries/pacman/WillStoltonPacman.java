package pacman.entries.pacman;

import pacman.game.Constants;
import pacman.game.Game;

import java.util.ArrayList;

public class WillStoltonPacman extends MyPacMan{

    private Constants.MOVE myMove= Constants.MOVE.NEUTRAL;

    public Constants.MOVE getMove(Game game, long timeDue)
    {
        //Place your game logic here to play the game as Ms Pac-Man
        /*
        The brief suggests trying to implement simple rules first.
            - collect pills and power pills
            - avoid inedible ghosts
            - eat edible ghosts
        I feel the best order here would be to avoid ghosts first, then eat ghosts, then eat pills.
        But first Ms.Pacman's location is required
         */

        int msPacmanLocation = game.getPacmanCurrentNodeIndex();

        // avoid ghosts
        ArrayList<Constants.GHOST> avoid = new ArrayList();
        for(Constants.GHOST ghost: Constants.GHOST.values()){
            if(game.getGhostEdibleTime(ghost) == 0 && game.getGhostLairTime(ghost) == 0)
                avoid.add(ghost);
        }
        for(Constants.GHOST ghost : avoid) {
            if (game.getShortestPathDistance(game.getGhostCurrentNodeIndex(ghost), msPacmanLocation) < 10){

            }
        }


        // eat ghosts
        for(Constants.GHOST ghost: Constants.GHOST.values()){
            if(game.isGhostEdible(ghost) && game.getGhostEdibleTime(ghost) >= 1 ){

            }
        }


        // eat pills



        return myMove;
    }

}
