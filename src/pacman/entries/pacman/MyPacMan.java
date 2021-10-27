package pacman.entries.pacman;

import pacman.controllers.Controller;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getAction() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.pacman.mypackage).
 */
public class MyPacMan extends Controller<MOVE>
{
	private MOVE myMove=MOVE.NEUTRAL;
	
	public MOVE getMove(Game game, long timeDue) 
	{
		//Place your game logic here to play the game as Ms Pac-Man
		/*
		You can program your own player in the file entries directory –
		the file MyPacMan provides a dummy player that you can extend.
		To get started, I recommend implementing simple rules (rather than any search).
		For example, the rule that PacMan should try to collect all pills and all power pills,
		avoid inedible ghosts and try to eat edible ghosts.
		 */

		int[] pillLocations = game.getPillIndices();
		for(int pill : pillLocations){
			if (game.isPillStillAvailable(pill)){

			}
		}
		
		return myMove;
	}
}