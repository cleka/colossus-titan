
package net.sf.colossus.protocol;
/**
   
TOWER <name> <land>
    Player <name> starts on <land>.

SPLIT <legion1> <legion2> <number> {<units>}
    A player splits off <number> units from <legion1> to a new
    legion with marker <legion2>.  The units split off into
    <legion2> were <units>.  (The units will only be
    identified if this client is allowed to see the contents
    of this stack.)

MULLIGAN
    The active player took a mulligan.

MOVE <legion> <land> <entryside>
    Move <legion> to masterboard hex <land> entering via <entryside>.

TELEPORT <legion> <land> <entryside> <unit>
    Teleport <legion> to <land> entering on <entryside> revealing
    lord <unit>.

UNDOMOVE <legion>
    Move <legion> back to its previous location.

ENGAGE <land>
    Resolve the engagement in <land>.

FLEE <legion> <units>
    <legion> flees, losing <units>

DONTFLEE <legion>
    <legion> did not flee.

CONCEDE <legion> <units>
    <legion> concedes, losing <units>

DONTCONCEDE <legion>
    <legion> did not concede.

NEGOTIATE <player> {<legion> <units>}
    <player> offered a negotiated settlement where <legion> would
    survive and be left with <units>.  (If <legion> and <units>
    are null, then <player> offered mutual destruction.)

ACCEPT <legion1> <units1> <legion2> <number> {<units2>}
    A previous offer has been accepted.  <legion1> consisting of
    <units1> has been eliminated.  <legion2> has <number1>
    creatures left, and has lost creatures <units2>  (If there
    was a mutual, then <number> will be 0.  If the winning
    legion lost no creatures, then <units2> will be null.)


REVEAL <legion> <units>
    Specify all of the units contained in <legion>.

PROMPTS:

MOVES <legion> <lands>
    <legion> has valid non-teleport masterboard moves to <lands>.    
    (If <lands> is null, <legion> has no legal non-teleport moves.)

TELEPORTS <legion> <lands>
    <legion> has valid teleport masterboard moves to <lands>.    
    (If <lands> is null, <legion> has no teleport moves.)

RECRUITS <legion> <units>
    <legion> can recruit <units>.  If <units> is null, <legion>
    cannot recruit.

 */

public class MasterBoardEvent extends GameEvent
{
	MasterBoardEvent(Object oSource)
		{
			super(oSource);
		}
}
