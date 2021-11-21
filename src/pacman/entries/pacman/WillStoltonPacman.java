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

        if(ghostTarget != null && game.wasGhostEaten(ghostTarget) && !game.wasPacManEaten()){
            ghostTarget = null;
        }
        Map<GHOST, Integer> edible = new HashMap<>();
        Map<GHOST, Integer> inedible = new HashMap<>();

        // Determine if ghost is edible or inedible and assign it so.
        for (GHOST ghost : ghosts) {
            if (game.getGhostLairTime(ghost) == 0) {
                int ghostIndex = game.getGhostCurrentNodeIndex(ghost);
                if (!game.isGhostEdible(ghost)) {
                    inedible.put(ghost, (int)game.getEuclideanDistance(ghostIndex, msPLocation));
                } else {
                    edible.put(ghost, game.getShortestPathDistance(ghostIndex, msPLocation));
                }
            }
        }

        boolean routeFound = false;
        // If there is a closest ghost, run away from it. But consider edible ghosts.
        for (Map.Entry<GHOST, Integer> entry : inedible.entrySet()) {
            if (entry.getValue() <= 5) {
                // Ghost needs to be closer than this (sweet spot).
                // If there is and edible ghost, chase them if possible.
                if(ghostTarget != null){
                    try{
                        routeFound = check(ghostTarget, game);
                    } catch (Exception ignored){
                        // no need to set routeFound to false as it already is.
                    }
                }
                // If there is a clear path to a pill while being chased, take that
                else{
                    routeFound = check(game, allEdibles, msPLocation, ghosts);
                }
                if(routeFound){
                    return chosenMove;
                }
                // otherwise, just run away.
                else{
                    return game.getNextMoveAwayFromTarget(msPLocation, game.getGhostCurrentNodeIndex(entry.getKey()), DM.EUCLID);
                }
            }
        }

        // Now look at edible ghosts
        ArrayList<GHOST> buffet = new ArrayList<>();
        int distance = Integer.MAX_VALUE;

        for (Map.Entry<GHOST, Integer> entry : edible.entrySet()) {
            int currentGhostDistance = entry.getValue();
            if (currentGhostDistance <= 100 && currentGhostDistance < distance) {
                buffet.add(entry.getKey());
                distance = currentGhostDistance;
            }
        }
        if(buffet.size() > 0) {
            ghostTarget = buffet.get(buffet.size()-1);
            routeFound = check(ghostTarget, game);
            if (routeFound) {
                return chosenMove;
            }
        }

        // Finally, if there are no edible ghosts and no immediately concerning ghosts, look at the pills.
        check(game, allEdibles, msPLocation, ghosts);
        return chosenMove;
    }


    private MOVE sim(Game game, int msPLocation, ArrayList<Integer> edible, ArrayList<Integer> power, ArrayList<Integer> pills){
        Map<Double, MOVE> aliveAndRoute = new HashMap<>();
        double counter = 0;
        MOVE returnThis = null;
        final int SIZE = 100;
        RandomPacMan rpm = new RandomPacMan();
        int lives = game.getPacmanNumberOfLivesRemaining();
        for(MOVE move : game.getPossibleMoves(msPLocation, game.getPacmanLastMoveMade())){
            int[] path = new int[SIZE];
            Game future = game.copy();
            counter = Double.MIN_VALUE;
            boolean dead = false;
            int round = 0;
            while (round != SIZE) {
                if(future.getPacmanNumberOfLivesRemaining() == lives-1){
                    dead = true;
                    break;
                }
//                else if(round == SIZE-1){
//                    path[round] = future.getPacmanCurrentNodeIndex();
//                    break;
//                }
                else {
                    path[round] = future.getPacmanCurrentNodeIndex();
                    round += 1;
                    if (round == 0) {
                        future.advanceGame(move, new Legacy().getMove());
                    } else {
                        future.advanceGame(rpm.getMove(future, System.currentTimeMillis()), new Legacy().getMove());
                    }
                }
            }
            for (Integer entry : path) {
                if (edible.contains(entry)) {
                    counter = counter * 2;
                } else if (power.contains(entry)) {
                    counter = counter * 1.5;
                } else if (pills.contains(entry)) {
                    counter = counter * 1.1;
                } else {
                    counter = counter + 1;
                }
            }
            if(!dead) {
                aliveAndRoute.put(counter, move);
            }
        }

        Double min = Double.MIN_VALUE;
        for(Map.Entry<Double, MOVE> entry : aliveAndRoute.entrySet()){
            if(entry.getKey() > min){
                returnThis = entry.getValue();
                min = entry.getKey();
            }
        }
        return returnThis;
    }


    /**
     * Confirm route to pill is clear
     * @param game game object
     * @param edibles array of edible items
     * @param msPLocation current location
     * @param ghosts array of ghosts
     * @return true if path clear, false if not.
     */
    private boolean check(Game game, int[] edibles, int msPLocation, GHOST[] ghosts) {
        int target = game.getClosestNodeIndexFromNodeIndex(msPLocation, edibles, DM.PATH);
        int[] path = game.getShortestPath(msPLocation, target);

        boolean youShallNotPass = false;
        for (int i = 0; (i < path.length) && !youShallNotPass; i++) {
            for (GHOST ghost : ghosts) {
                if (path[i] == game.getGhostCurrentNodeIndex(ghost) && !game.isGhostEdible(ghost)) {
                    youShallNotPass = true;
                    break;
                }
            }
        }
        // if no ghost in the way
        return ghostFree(game, target, msPLocation, youShallNotPass);
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
        return ghostFree(game, target, msPLocation, youShallNotPass);
    }


    /**
     * set the route to be followed if the path is clear
     * @param game game object
     * @param target target node
     * @param msPLocation Ms P location
     * @param youShallNotPass boolean
     * @return true or false
     */
    private boolean ghostFree(Game game, int target, int msPLocation, boolean youShallNotPass) {
        if(!youShallNotPass){
            GameView.addPoints(game, Color.MAGENTA, game.getShortestPath(msPLocation, target));
            chosenMove = game.getNextMoveTowardsTarget(msPLocation, target, DM.PATH);
            return true;
        }
        else{
            // if there is a ghost in the route, simulate a new path
            chosenMove = sim(game, msPLocation, edibleGhosts(game), powerPills(game), pills(game));
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