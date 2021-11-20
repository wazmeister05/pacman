package pacman.entries.pacman;

import pacman.controllers.Controller;
import pacman.controllers.examples.Legacy;
import pacman.controllers.examples.RandomPacMan;
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


    private GHOST ghostTarget;
    private MOVE chosenMove;

    /**
     * Main AI logic. Avoid the lair or other ghosts, then chase edible ghosts and finally
     * get the best route to the pills.
     *
     * @param game    A copy of the current game
     * @param timeDue The time the next move is due
     * @return the move for the AI to follow
     */
    public MOVE getMove(Game game, long timeDue) {
        // We'll need these throughout so make them now.
        int msPLocation = game.getPacmanCurrentNodeIndex();
        GHOST[] ghosts = GHOST.values();
        int[] allEdibles = getAllEdibles(game);

        return sim(game, msPLocation, ghosts, allEdibles);
    }


    private MOVE sim(Game game, int msPLocation, GHOST[] ghosts, int[] allEdibles){
        Map<Boolean, MOVE> scoreAndRoute = new HashMap<>();
        int counter = 0;
        MOVE returnThis = null;
        final int SIZE = 100;
        RandomPacMan rpm = new RandomPacMan();
        int lives = game.getPacmanNumberOfLivesRemaining();
        for(MOVE move : game.getPossibleMoves(msPLocation, game.getPacmanLastMoveMade())){
            int[] path = new int[SIZE];
            Game future = game.copy();
            counter = Integer.MIN_VALUE;
            boolean dead = false;
            int round = 0;
            while (round != SIZE) {
                if(future.getPacmanNumberOfLivesRemaining() == lives-1){
                    dead = true;
                    break;
                }
                else if(round == SIZE-1){
                    path[round] = future.getPacmanCurrentNodeIndex();
                    break;
                }
                else {
                    path[round] = future.getPacmanCurrentNodeIndex();
                    round += 1;
                    counter = counter + future.getScore();
                    if (round == 0) {
                        future.advanceGame(move, new Legacy().getMove());
                    } else {
                        future.advanceGame(rpm.getMove(future, System.currentTimeMillis()), new Legacy().getMove());
                    }
                }
            }
            scoreAndRoute.put(dead, move);
        }

        for(Map.Entry<Boolean, MOVE> entry : scoreAndRoute.entrySet()){
            if(!entry.getKey()){
                returnThis = entry.getValue();
            }
        }
        return returnThis;
    }



    /**
     * Check to confirm route to edible ghost is free of non-edible ghosts
     * @param ghostToEat ghost target to consume
     * @param game game object
     * @return true if path clear, false if not.
     */
    private boolean check(GHOST ghostToEat, Game game) {
        int target = game.getGhostCurrentNodeIndex(ghostToEat);
        int msPLocation = game.getPacmanCurrentNodeIndex();
        int[] path = game.getShortestPath(game.getPacmanCurrentNodeIndex(), target);
        boolean youShallNotPass = false;
        for (int i = 0; (i < path.length) && !youShallNotPass; i++) {
            for (GHOST ghost : GHOST.values()) {
                if (path[i] == game.getGhostCurrentNodeIndex(ghost) && !game.isGhostEdible(ghost)) {
                    youShallNotPass = true;
                    break;
                }
            }
        }
        // if no ghost in the way
        if (!youShallNotPass) {
            GameView.addPoints(game, Color.MAGENTA, game.getShortestPath(msPLocation, target));
            chosenMove = game.getNextMoveTowardsTarget(msPLocation, target, DM.PATH);
            return true;
        }
        else{
            int[] chosenRoute = sim(game, msPLocation);
            chosenMove = game.getNextMoveTowardsTarget(msPLocation, chosenRoute[0], DM.PATH);
            return false;
        }
    }


    /////////////////////////////////////////////////////////////////////////////
    /////////////////  Create and build arrays and stuff  ///////////////////////
    /////////////////////////////////////////////////////////////////////////////


    /**
     * Get the index of active pills still in play
     *
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
     *
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
     *
     * @param game game object
     * @return arraylist of ghost locations
     */
    private ArrayList<Integer> nonEdibleGhosts(Game game) {
        ArrayList<Integer> ghosts = new ArrayList<>();
        for (GHOST ghost : GHOST.values()) {
            if (!game.isGhostEdible(ghost))
                ghosts.add(game.getGhostCurrentNodeIndex(ghost));
        }
        return ghosts;
    }


    /**
     * Get the index of the ghosts that are edible
     *
     * @param game game object
     * @return arraylist of ghost locations
     */
    private ArrayList<Integer> edibleGhosts(Game game) {
        ArrayList<Integer> ghosts = new ArrayList<>();
        for (GHOST ghost : GHOST.values()) {
            if (game.isGhostEdible(ghost)) {
                ghosts.add(game.getGhostCurrentNodeIndex(ghost));
            }
        }
        return ghosts;
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

        for (int i = 0; i < gSize; i++) {
            allEdibles[i] = edibleGhostLocs.get(i);
        }
        for (int i = 0; i < ppSize; i++) {
            allEdibles[i + gSize] = powerPills.get(i);
        }
        for (int i = 0; i < pSize; i++) {
            allEdibles[i + gSize + ppSize] = pills.get(i);
        }
        return allEdibles;
    }
}