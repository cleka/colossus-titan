
package net.sf.colossus.protocol;

/**

MANEUVER <battlehex1> <battlehex2>
    Move unit in <battlehex1> to <battlehex>.

UNDOMANEUVER <battlehex>
    Move unit in <battlehex> back to its previous location.

STRIKE <battlehex1> <battlehex2> <number1> <number2> <number3>
       <number4> <numbers>
    Unit in <battlehex1> strikes unit in <battlehex2>, rolling
    <number1> dice with a target number of <number2>.  There
    were <number3> hits and <number4> carries.  The die rolls
    were <numbers>

DRIFT  <battlehex>
    Unit in <battlehex> takes one point of drift damage.

ELIMINATE <battlehex>
    Unit in <battlehex> is eliminated.

SUMMON <legion1> <legion2> <unit>
    <legion1> summons a <unit> from <legion2>

ACQUIRE <legion> <unit> <number>
    <legion> acquires a <unit>.  Afterwards, there are <number>
    of <unit> remaining in the stacks.

ENDBATTLE
    The battle is over.

PROMPTS:

CARRY? <number> <battlehexes>
    <number> hits of damage can be carried to the units in
    <battlehexes>

SUMMON? <legion1> <legion2> <units1> {<legion3> <units2>}
    <legion1> can summon.  Eligible donor legions include
    <legion2> ... and the types of units that can be summoned
    include <units1> ...

REINFORCE? <legion> <units>
    <legion> can summon a reinforcement from <units>

ACQUIRE? <legion> <units>
    <legion> can acquire one of the listed units.


MANEUVERS <battlehex> <battlehexes>
    Unit in <battlehex> can maneuver to <battlehexes>.  If 
    <battlehexes> is null, the unit cannot move.

MANEUVERS <unit> <battlehexes>
    All offboard units of type <unit> can maneuver to <battlehexes>.
    If null, they cannot enter the board.

STRIKES <battlehex0> <battlehex1> <number11> <number12> ...
    The unit in <battlehex0> can strike the unit in <battlehex1>
    with <number11> dice and strike number <number12>.  Etc.

*/

public class BattleBoardEvent extends GameEvent
{
    BattleBoardEvent(Object oSource)
	{
	    super(oSource);
	}
}
