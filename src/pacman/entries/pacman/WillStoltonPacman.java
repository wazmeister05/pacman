package pacman.entries.pacman;

import pacman.controllers.Controller;
import pacman.controllers.examples.Legacy;
import pacman.controllers.examples.RandomPacMan;
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


    private GHOST ghostTarget;
    Map<GHOST, Integer> edible = new HashMap<>();
    Map<GHOST, Integer> inedible = new HashMap<>();
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

        if(ghostTarget != null && game.wasGhostEaten(ghostTarget) && !game.wasPacManEaten()){
            ghostTarget = null;
        }
        int msPLocation = game.getPacmanCurrentNodeIndex();
        GHOST[] ghosts = GHOST.values();
        //int[] allEdibles = getAllEdibles(game);



        // Determine if ghost is edible or inedible and assign it so.
        extracted(game, msPLocation, ghosts, edible, inedible);

        // If there is a closest ghost, run away from it. But consider edible ghosts.
        for (Map.Entry<GHOST, Integer> entry : inedible.entrySet()) {
            if (entry.getValue() <= 10) {
                return game.getNextMoveAwayFromTarget(msPLocation, game.getGhostCurrentNodeIndex(entry.getKey()), DM.EUCLID);
            }
        }

        return sim(game, msPLocation, ghosts, powerPills(game), pills(game), edible, inedible);
    }

    private void extracted(Game game, int msPLocation, GHOST[] ghosts, Map<GHOST, Integer> edible, Map<GHOST, Integer> inedible) {
        for (GHOST ghost : ghosts) {
            if (game.getGhostLairTime(ghost) == 0) {
                int ghostIndex = game.getGhostCurrentNodeIndex(ghost);
                if (!game.isGhostEdible(ghost)) {
                    inedible.put(ghost, (int) game.getEuclideanDistance(ghostIndex, msPLocation));
                } else {
                    edible.put(ghost, game.getShortestPathDistance(ghostIndex, msPLocation));
                }
            }
        }
    }


    private MOVE sim(Game game, int msPLocation, GHOST[] ghosts, ArrayList<Integer> powerPills,
                     ArrayList<Integer> pills, Map<GHOST, Integer> edible, Map<GHOST, Integer> inedible){
        Map<Double, MOVE> scoreAndRoute = new HashMap<>();
        ArrayList<GHOST> buffet = getGhosts(edible);
        double counter;
        MOVE returnThis = null;
        final int SIZE = 100;
        RandomPacMan rpm = new RandomPacMan();
        boolean routeFound = false;
        int lives = game.getPacmanNumberOfLivesRemaining();
        while(!routeFound){
            for(MOVE move : game.getPossibleMoves(msPLocation, game.getPacmanLastMoveMade())) {
                int[] path = new int[SIZE];
                Game future = game.copy();
                counter = Double.MIN_VALUE;
                int round = 0;
                while (round != SIZE) {
                    pills = pills(future);
                    if (future.getPacmanNumberOfLivesRemaining() == lives - 1) {
                        path = null;
                        break;
                    } else if (round == SIZE - 1) {
                        path[round] = future.getPacmanCurrentNodeIndex();
                        break;
                    } else {
                        path[round] = future.getPacmanCurrentNodeIndex();
                        round += 1;
                        if (round == 0) {
                            future.advanceGame(move, new Legacy().getMove());
                        }
                        else if(buffet.size() > 0){
                            if(ghostTarget != null && future.wasGhostEaten(ghostTarget)){
                                buffet = getGhosts(edible);
                                ghostTarget = buffet.get(0);
                            }
                            if(ghostTarget != null && future.isGhostEdible(ghostTarget)) {
                                future.advanceGame(future.getNextMoveTowardsTarget(future.getPacmanCurrentNodeIndex(), future.getGhostCurrentNodeIndex(ghostTarget),
                                        future.getPacmanLastMoveMade(), DM.PATH), new Legacy().getMove());
                            }
                            else if(ghostTarget != null && !future.isGhostEdible(ghostTarget)){
                                ghostTarget = null;
                                buffet.clear();
                            }
                            else{
                                try {
                                    ghostTarget = buffet.get(0);
                                    buffet.remove(ghostTarget);
                                    future.advanceGame(future.getNextMoveTowardsTarget(future.getPacmanCurrentNodeIndex(), future.getGhostCurrentNodeIndex(ghostTarget),
                                            future.getPacmanLastMoveMade(), DM.PATH), new Legacy().getMove());
                                }catch(Exception e){
                                    future.advanceGame(rpm.getMove(future, System.currentTimeMillis()), new Legacy().getMove());
                                }
                            }
                        }
                        else {
                            future.advanceGame(future.getNextMoveTowardsTarget(future.getPacmanCurrentNodeIndex(), pills.get(0),
                                    future.getPacmanLastMoveMade() ,DM.PATH), new Legacy().getMove());
                        }
                    }
                }
                if(path != null) {
                    for (Integer entry : path) {
                        if (edible.containsValue(entry)) {
                            counter = counter * 2;
                        } else if (powerPills.contains(entry)) {
                            counter = counter * 1.5;
                        } else if (pills.contains(entry)) {
                            counter = counter * 1.1;
                        } else {
                            counter = counter + 1;
                        }
                    }

                    if (!check(game, path, msPLocation, ghosts)) {
                        scoreAndRoute.put(counter, move);
                        routeFound = true;
                    } else {
                        scoreAndRoute.put(Double.MIN_VALUE, move);
                        routeFound = true;
                    }
                }
            }
        }

        int temp = Integer.MIN_VALUE;
        for(Map.Entry<Double, MOVE> entry : scoreAndRoute.entrySet()){
            if(entry.getKey() > temp){
                returnThis = entry.getValue();
            }
        }
        return returnThis;
    }

    private ArrayList<GHOST> getGhosts(Map<GHOST, Integer> edible) {
        double counter = Integer.MIN_VALUE;
        ArrayList<GHOST> buffet = new ArrayList<>();
        int distance = Integer.MAX_VALUE;
        for (Map.Entry<GHOST, Integer> entry : edible.entrySet()) {
            int currentGhostDistance = entry.getValue();
            if (currentGhostDistance <= 100 && currentGhostDistance < distance) {
                buffet.add(entry.getKey());
                distance = currentGhostDistance;
            }
        }
        return buffet;
    }


    /**
     * Confirm route to pill is clear
     * @param game game object
     * @param msPLocation current location
     * @param ghosts array of ghosts
     * @return true if path clear, false if not.
     */
    private boolean check(Game game, int[] route, int msPLocation, GHOST[] ghosts) {
        int target = game.getClosestNodeIndexFromNodeIndex(msPLocation, route, DM.PATH);
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
        return youShallNotPass;
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
        //ArrayList<Integer> edibleGhostLocs = edibleGhosts(game);
        int pSize = pills.size();
        int ppSize = powerPills.size();
        //int gSize = edibleGhostLocs.size();
        //int[] allEdibles = new int[pSize + ppSize + gSize];
        int[] allEdibles = new int[pSize + ppSize];

//        for (int i = 0; i < gSize; i++) {
//            allEdibles[i] = edibleGhostLocs.get(i);
//        }
        for (int i = 0; i < ppSize; i++) {
            //allEdibles[i + gSize] = powerPills.get(i);
            allEdibles[i] = powerPills.get(i);
        }
        for (int i = 0; i < pSize; i++) {
            allEdibles[i + ppSize] = pills.get(i);
            //allEdibles[i + gSize + ppSize] = pills.get(i);
        }
        return allEdibles;
    }
}