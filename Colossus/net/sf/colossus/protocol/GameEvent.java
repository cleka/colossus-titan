
package net.sf.colossus.protocol;

import java.util.EventObject;

/**

NAK <number>
    Request <number> (starting from 0) from this client was denied.

NAME <name>
    A player with name <name> has connected.

COLOR <name> <colorname>
    Player <name> has chosen color <colorname>.

TOWER <name> <land>
    Player <name> starts on <land>.


ROLL <number>
    The active player rolled a <number>.

POINTS <player> <number> <legion>
    <player> receives <number> points for eliminating <legion>.

ELIMINATEPLAYER <player1> {<player2> <legions>}
    <player1> has been eliminated from the game by <player2>,
    who receives legion markers <legions>.  If two players
    mutually eliminate one another, then <player2> and
    <legions> will be null.

ENDGAME <player1> {player2}
    The game is over.  If only one player is listed, then he
    wins.  If two players are listed, then they draw.

RECRUIT <legion> <unit1> {<units>} <number>
    Recruit a <unit1> in <legion> using <units>.  After this recruit,
    there are <number> of <unit1> remaining in the stacks.  <units>
    will be null if recruiting a basic tower creature in a tower.

UNDORECRUIT <legion> <unit>
    <legion> undoes its recruit of <unit>

TURN <name>
    It's <name>'s turn.

PROMPTS:

COLOR? <colornames>
    Choose a color from those listed.

FLEE? <legion>
    <legion> needs to decide whether to flee.

CONCEDE? <legion>
    <legion> needs to decide whether to concede.


 */

public class GameEvent extends EventObject
{
    GameEvent(Object oSource)
	{
	    super(oSource);
	}
}
