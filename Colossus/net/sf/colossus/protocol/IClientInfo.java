
package net.sf.colossus.protocol;

public interface IClientInfo
{
	/**
	   PLAYERS?
	   List all players.
	*/
	public String[] getPlayers();
	
	/** PLAYER? <player>
		For <player>, show score and legion markers remaining.
	*/
	public PlayerInfo getPlayerInfo(String strPlayer);
	
	/** LEGIONS? <player>
		Show all of <player>'s legions and their heights.
	*/
	public LegionSummaryInfo[] getLegionInfo(String strPlayer);

	/** LEGION? <legion>
		Show <legion>'s contents.  (If allowed.)
	*/
	public String[] getLegionContents(String strPlayer);


	/** 
		MOVES? <legion>
		Show valid masterboard moves for <legion>.  (If <legion> belongs
		to another player, then teleport moves will not be shown.)
	*/
	public int[] getMoves(LegionId id);
	
	/** 
		RECRUITS? <legion>
		Show valid recruits for <legion>.  (If <legion> belongs to 
		another player and its contents are unknown, this will not
		work.)
	*/
	public String[] getRecruits();

	/**
	   MANEUVERS? <battlehex>
	   Show valid battlemap moves for the unit in <battlehex>.
	*/
	public int[] getManeuvers(int nHex);	

	/**
	   MANEUVERS? <unit>
	   Show valid battlemap moves for offboard <unit>.
	*/
	public int[] getManeuvers(String strUnit);	
	
	/**
	   STRIKES? <battlehex>
	   Show valid strike targets, with number of dice and target
	   number, for the unit in <battlehex>.
	*/
	public StrikeInfo[] getStrikes();
	
	/** 
		COUNTS? {<unit>}
		Show the number of <unit> remaining in the stacks.  If <unit>
		is null, show the number of all unit types remaining.
	*/
	public CountInfo[] getRemainingCounts(String strUnit);
	
	/**
	   REPLAY?
	   Resend the stream of all significant events so far in this game to
	   this client.
	*/
	public void replay();
	
}
