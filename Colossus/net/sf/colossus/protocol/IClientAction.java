
package net.sf.colossus.protocol;

public interface IClientAction
{
	/**
	   NAME <name> {<message>}
	   Choose a name.  <name> is the short name that will be used in
	   most places.  <message> is the long version, which may be
	   used in some logs.  If <message> is null then <name> will be
	   used everywhere.
	*/
	public void chooseName(String strName, String strLongName);

	/**
	   COLOR <colorname>
	   Choose a color.
	*/
	public void chooseColor(String strColorName);

	/**
	   OBSERVER <name>
	   Join as an observer.  Observers can only chat, not make game
	   actions.
	*/
	public void joinObserver(String strName);

	/**
	   START
	   Starts the game. No more players can connect.
	*/
	public void startGame();

	/**
	   SPLIT <legion1> <legion2> <units>
	   Split off <units> from <legion1> to a new legion with marker
	   <legion2>.
	*/
	public void splitLegion(LegionId id1, LegionId id2, String[] strUnitsArray);

	/**
	   JOIN <legion1> <legion2>
	   Move all units from child <legion1> back into parent <legion2>.
	   (This is only allowed to cancel a split before movement is rolled,
	   or if neither legion has a legal move.)
	*/
	public void cancelSplit(LegionId idOriginal, LegionId idAborted);

	/**
	   ROLL
	   Finish splitting and wait for the movement roll.
	*/
	public void beginMasterMove(); // was roll
	
	/**
	   MULLIGAN
	   Re-roll movement.  In standard Titan, each player gets one
	   Mulligan on the first turn.
	*/
	public void chooseMulligan();

	/**
	   MOVE <legion> <land> {<entryside>}
	   Move <legion> to masterboard hex <land> entering on side
	   <entryside>.  If <entryside> is omitted, the server picks.
	*/
	public void moveLegion(LegionId id, int nMasterHex, int nSide);

	/**
	   TELEPORT <legion> <land> {<entryside>} {<unit>}
	   Teleport <legion> to <land> entering on <entryside> revealing
	   lord <unit>.  If <entryside> and/or <unit> is omitted, the
	   server picks.
	*/
	public void teleportLegion(LegionId id, int nMasterHex, 
							   int nSize, String strCharacter);


	/**
	   UNDOMOVE <legion>
	   Move <legion> back to its previous location.
	*/
	public void undoMoveLegion(LegionId id, int nOldHex);
	
	/**
	   RECRUIT <legion> <unit> <units>
	   Recruit a <unit> in legion <legion> using <units>
	*/
	public void recruit(LegionId id, String strRecruitee, 
						String[] strRecruitersArray);

	/**
	   UNDORECRUIT <legion> <unit>
	   <legion> undoes its recruit of <unit>
	*/
	public void undoRecruit(LegionId id, String strRecruitee);
	
	/**
		 ENGAGE <land>
		 Resolve the engagement in <land>.
	  */
	public void engage(int nMasterHex);

	/**
	   FLEE <legion>
	   <legion> flees.
	   
	   DONTFLEE <legion>
	   <legion> does not flee.
	*/
	public void flee(LegionId id, boolean bFlee);

	/**
	   CONCEDE <legion>
	   <legion> concedes the battle.
	   
	   DONTCONCEDE <legion>
	   <legion> does not concede the battle.
	*/
	public void concede(LegionId id, boolean bConcede);

	/**
	   NEGOTIATE <land> {<legion> <units>}
	   Offer a negotiated settlement to the engagement in <land> where 
	   <legion> wins, and is left with <units>  If <legion> is null, then 
	   offer mutual destruction.  If a player sends a NEGOTIATE request 
	   with arguments identical to those of a NEGOTIATE request previously 
	   sent by the opponent, then this signifies accepting the earlier 
	   proposal.
	*/
	public void negotiate(int nMasterHex, LegionId id, String[] strUnitsArray);

	/**
	   FIGHT <land>
	   Cease negotiations in <land> and fight a battle.
	*/
	public void fight(int nMasterHex);

	/**
	   ENTER <unit> <battlehex>
	   Move <unit> from offboard to <battlehex>. 
	*/
	public void enter(String strUnit, int nBattleHex);
	/**
	   
	   MANEUVER <battlehex1> <battlehex2>
	   Move unit in <battlehex1> to <battlehex2>.
	*/
	public void maneuver(int nFromBattleHex, int nToBattleHex);

	/**
	   UNDOMANEUVER <battlehex>
	   Move unit in <battlehex> back to its previous location.
	*/
	public void undomaneuver(int nToBattleHex, int nFromBattleHex);
	
	/**
	   DONEMANEUVERING
	   All battle moves are finished.  Move on to strike phase, or the
	   opponent's turn, as appropriate.
	*/
	public void doneManevering();

	/**
	   STRIKE <battlehex1> <battlehex2> <number1> <number2>
	   Unit in <battlehex1> strikes unit in <battlehex2>, rolling
	   <number1> dice with a target number of <number2>.
	*/
	public void strike(int nFromHex, int nToHex, int nDice, int nToHit);

	/**
	   CARRY <battlehex> <number>
	   Carry <number> hits to the unit in <battlehex>.
	*/
	public void carry(int nToHex, int nHits);
	
	/**
	   DONESTRIKING
	   All battle strikes are finished.  Move on to the opponent's turn.
	*/
	public void doneStriking();
	
	/**
	   SUMMON <legion1> <legion2> <unit>
	   <legion1> summons a <unit> from <legion2>
	*/
	public void summon(LegionId idToLegion, LegionId idFromLegion, 
					   String strUnit);


	/**
	   ACQUIRE <legion> <unit>
	   <legion> acquires a <unit>.
	*/
	public void acquire(LegionId id, String strUnit);

	/**
	   
	   NEXTTURN
	   Finish this player's entire game turn.
	*/
	public void doneTurn();

	/**
	   
	   WITHDRAW
	   This player withdraws from the entire game.
	*/
	public void withdraw();

	/**
	   
	   SAY <message>
	   Send message to all players and observers.
	*/
	public void say(String strMsg);
	/**
	   TELL <player> <message>
	   Send chat message to one player.
	*/
	public void tell(String strPlayer, String strMsg);
	/**
	   KIBBITZ <message>
	   Send chat message to all observers.
	*/
	public void kibbitz(String strMsg);
}









