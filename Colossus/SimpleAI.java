/**
 *
 * Simple implementation of a Titan AI
 *
 * @author Bruce Sherrod
 */

import java.io.*;
import java.util.*;

class SimpleAI implements AI
{
    private static final boolean DEBUG = true;
    private Minimax minimax = new Minimax();

    public void muster (Game game)
    {
        Player player = game.getActivePlayer();
        List legions = player.getLegions();
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (legion.hasMoved() && legion.canRecruit())
            {
                List recruits = game.findEligibleRecruits(legion);

                // The last recruit is the biggest.
                Creature recruit = (Creature)recruits.get(recruits.size() - 1);

                // Recruit a third cyclops instead of a gorgon.
                if (recruit == Creature.gorgon &&
                    recruits.contains(Creature.cyclops) &&
                    legion.numCreature(Creature.cyclops) == 2)
                {
                    recruit = Creature.cyclops;
                }

		// TODO: warlock over guardian when appropriate
		// TOOD: third lion or troll when appropriate
		// TODO: better tower creature selection

                if (recruit != null)
                {
                    game.doRecruit(recruit, legion, game.masterFrame);
                }
            }
        }
        game.board.unselectAllHexes();
        game.updateStatusScreen();
    }

    public void split (Game game)
    {
        Player player = game.getActivePlayer();
        List legions = player.getLegions();
        Iterator it = legions.iterator();
	LinkedList legionsToAdd = new LinkedList();
        while (it.hasNext())
        {
	    Legion legion = (Legion) it.next();
	    if (legion.getHeight() < 7) 
		continue;

	    // create the new legion
	    String selectedMarkerId = player.getFirstAvailableMarker();
            selectedMarkerId = player.selectMarkerId(selectedMarkerId);
	    if (selectedMarkerId == null)
	    {
		// ack! we're out of legion markers!
		return;
	    }
	    Legion newLegion = new Legion(selectedMarkerId, 
					  legion.getMarkerId(), 
					  legion.getCurrentHex(), // current hex
					  legion.getCurrentHex(), // starting hex
					  null, null, null, null, // creatures
					  null, null, null, null, // creatures
					  player);
	    String imageName = selectedMarkerId;
	    Marker newMarker = new Marker(legion.getMarker().getBounds().width,
					  imageName, game.board, newLegion);
	    newLegion.setMarker(newMarker); 
	    MasterHex hex = newLegion.getCurrentHex();
	    newLegion.getCurrentHex().addLegion(newLegion, false);
	    legionsToAdd.addLast(newLegion);

	    if (legion.getHeight() == 8)
	    {
		// initial game split:
		// move either titan or angel at random (thus giving away no info)
		//
		// CMU style: if we're in an odd tower, split ogres.
		// if we're in an even tower, split centaurs.
		//
		// TODO: MIT style: split gargoyles 
		// TODO: consider splitting 1-1-1.  (why?)
		// TODO: consider keeping titan with gargoyles more often than 50%
		//   if many players in the game
		//
		Creature lord = (game.random.nextInt(2) % 2 == 0)?
		    legion.removeCreature( Creature.titan, false, false)
		    : legion.removeCreature( Creature.angel, false, false);
		newLegion.addCreature(lord,false); 
		String label = hex.getLabel();
		if ("100".equals(label) || "300".equals(label) || "500".equals(label))
		{
		    Creature c1 = legion.removeCreature(Creature.centaur, false, false);
		    Creature c2 = legion.removeCreature(Creature.centaur, false, false);
		    Creature c3 = legion.removeCreature(Creature.ogre, false, false);
		    newLegion.addCreature(c1,false);
		    newLegion.addCreature(c2,false);
		    newLegion.addCreature(c3,false);
		}
		else
		{
		    Creature c1 = legion.removeCreature(Creature.ogre, false, false);
		    Creature c2 = legion.removeCreature(Creature.ogre, false, false);
		    Creature c3 = legion.removeCreature(Creature.centaur, false, false);
		    newLegion.addCreature(c1,false);
		    newLegion.addCreature(c2,false);
		    newLegion.addCreature(c3,false);
		} 
	    }
	    else
	    {
		//
		// split a 7 high legion somehow
		//
		// idea: pick the 2 weakest creatures and kick them
		// out. if there are more than 2 weakest creatures,
		// prefer a pair of matching ones.  if none match,
		// kick out the left-most ones (the oldest ones)
		//
		Critter weakest1 = null;
		Critter weakest2 = null;
		for (Iterator critterIt = legion.getCritters().iterator(); 
		     critterIt.hasNext(); )
		{
		    Critter critter = (Critter) critterIt.next();
		    if (weakest1 == null)
			weakest1 = critter;
		    else if (weakest2 == null)
			weakest2 = critter;
		    else if (critter.getPointValue() < weakest1.getPointValue())
			weakest1 = critter;
		    else if (critter.getPointValue() < weakest2.getPointValue())
			weakest2 = critter;
		    else if ( critter.getPointValue() == weakest1.getPointValue()
			&& critter.getPointValue() == weakest2.getPointValue())
		    {
			if (critter.getName().equals(weakest1.getName()))
			    weakest2 = critter;
			else if (critter.getName().equals(weakest2.getName()))
			    weakest1 = critter;
		    }
		}
		newLegion.addCreature(legion.removeCreature(weakest1,false,false),false);
		newLegion.addCreature(legion.removeCreature(weakest2,false,false),false);
	    }
	} 

	// must do this after so that we don't get a CoModificationException from
	// the legion iterator above.
	while (legionsToAdd.size() > 0)
	{
	    Legion newLegion = (Legion) legionsToAdd.removeFirst();
	    player.addLegion(newLegion);
	    player.setLastLegionSplitOff(newLegion);
	}
    } 

    public void move (Game game)
    {
	simpleMove(game);
    }

    public void simpleMove (Game game)
    {
	// TODO: make sure we move at least one legion
	// TODO: consider turn 1 mulligans

        Player player = game.getActivePlayer();
        List legions = player.getLegions();
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
	    Legion legion = (Legion) it.next();

	    // null hypothesis: sit still
	    int bestRecruitValue = evaluateMove(game, legion,legion.getCurrentHex(),false);
	    MasterHex bestHex = legion.getCurrentHex();

	    // BUG: for some reason, on a 6 in the upper swamp/desert,
	    // we don't consider doing a full loop and "moving" to where we started. 

	    // hard-coded 1-ply search
	    Set set = game.listMoves(legion, true);
	    for (Iterator moveIterator = set.iterator(); moveIterator.hasNext();)
	    {
		String hexLabel = (String) moveIterator.next();
		MasterHex hex = game.board.getHexFromLabel(hexLabel); 
		int value = evaluateMove(game, legion,hex,true); 
		if (value > bestRecruitValue)
		{
		    bestRecruitValue = value;
		    bestHex = hex;
		}
	    }

	    // move legion to hex
	    if (bestHex != null)
	    {
		MasterHex hex = bestHex; 
		game.actOnLegion(legion);
		game.actOnHex(hex);
	    }
	}
    }

    //
    // cheap, inaccurate evaluation function
    //
    // TODO: should be parameterized with weights 
    //
    private int evaluateMove(Game game, Legion legion, MasterHex hex, boolean canRecruitHere)
    {
	int value =0;

	// TODO: if we're 7 high, we can't recruit (unless exploring further turns?)

	// consider making an attack
	Legion enemyLegion = hex.getEnemyLegion(legion.getPlayer());
	if (enemyLegion != null)
	{
	    // very conservative attack here
	    int discountedPointValue = (3 * legion.getPointValue()) / 4;
	    if (legion.numCreature(Creature.titan) > 0)
		discountedPointValue -= 10; // be a bit cautious attacking w/titan
	    // TODO: add our angel call 
	    int enemyPointValue = enemyLegion.getPointValue();
	    // TODO: add in enemy's turn 4 recruit
	    if (discountedPointValue > enemyPointValue)
	    {
		// we can win
		value += (40 * enemyPointValue) / 100; 
	    }
	    else
	    {
		// we'll lose
		value = Integer.MIN_VALUE; 
	    }
	}

	// consider what we can recruit
	if (canRecruitHere)
	{
	    // TODO: this should be a call to our recruiting code
	    // to see what recruit we'd actually take
	    List recruits = game.findEligibleRecruits(legion,hex);
	    if (DEBUG) System.out.println("recruits " + recruits);
	    if (recruits.size() > 0)
	    {
		int oldval = value;
		Creature recruit = (Creature)recruits.get(recruits.size() - 1);
		if (legion.getHeight() < 6)
		    value += recruit.getSkill() * recruit.getPower();
		else
		{
		    // Idea:
		    // if we're 6-high, then the value of a recruit is
		    // equal to the improvement in the value of the
		    // pieces we'll have after splitting.

		    // TODO this should call our splitting code to see
		    // what split decision we would make

		    if (DEBUG) System.out.println("--- 6-HIGH SPECIAL CASE");
		    
		    Critter weakest1 = null;
		    Critter weakest2 = null;
		    for (Iterator critterIt = legion.getCritters().iterator(); 
			 critterIt.hasNext(); )
		    {
			Critter critter = (Critter) critterIt.next();
			if (weakest1 == null)
			    weakest1 = critter;
			else if (weakest2 == null)
			    weakest2 = critter;
			else if (critter.getPointValue() < weakest1.getPointValue())
			    weakest1 = critter;
			else if (critter.getPointValue() < weakest2.getPointValue())
			    weakest2 = critter;
			else if ( critter.getPointValue() == weakest1.getPointValue()
				  && critter.getPointValue() == weakest2.getPointValue())
			{
			    if (critter.getName().equals(weakest1.getName()))
				weakest2 = critter;
			else if (critter.getName().equals(weakest2.getName()))
			    weakest1 = critter;
			}
		    }
		    int minCreaturePV 
			= Math.min(weakest1.getPointValue(),weakest2.getPointValue());
		    int maxCreaturePV 
			= Math.max(weakest1.getPointValue(),weakest2.getPointValue());
		    // point value of my best 5 pieces right now
		    int oldPV = legion.getPointValue() - minCreaturePV;
		    // point value of my best 5 pieces after adding this recruit
		    // and then splitting off my 2 weakest
		    int newPV = legion.getPointValue() 
			- weakest1.getPointValue()
			- weakest2.getPointValue() 
			+ Math.max(maxCreaturePV,recruit.getPointValue());
		    value += newPV - oldPV;
		} 
		if (DEBUG) System.out.println("--- if " + legion
				   + " moves to " + hex
				   + " then it could recruit " 
				   +  recruit.toString()
				   + " (adding " + (value-oldval) + " to score)" );
	    }
	}

	// consider what we might be able to recruit next turn, from here
	int nextTurnValue = 0;
	for (int roll = 1; roll <= 6; roll++)
	{
	    Set moves = game.listMoves(legion,true,hex,roll);
	    int bestRecruitVal = 0;
	    Creature bestRecruit = null;
	    for (Iterator nextMoveIt = moves.iterator(); nextMoveIt.hasNext();)
	    {
		String nextLabel = (String) nextMoveIt.next();
		MasterHex nextHex = game.board.getHexFromLabel(nextLabel);
		List nextRecruits = game.findEligibleRecruits(legion,nextHex);
		if (nextRecruits.size() == 0)
		    continue;
		Creature nextRecruit = 
		    (Creature)nextRecruits.get(nextRecruits.size() - 1);
		int val = nextRecruit.getSkill() * nextRecruit.getPower();
		if (val > bestRecruitVal)
		{
		    bestRecruitVal = val;
		    bestRecruit = nextRecruit;
		}
	    }
	    nextTurnValue += bestRecruitVal; 
	    if (DEBUG && bestRecruit != null)
		System.out.println("--- if " + legion
				   + " moves to " + hex
				   + " then it could recruit " 
				   +  bestRecruit.toString()
				   + " on a " + roll +  " next turn..."
				   + " (adding " + (bestRecruitVal/6.0) + " to score)"
				   );
	}
	nextTurnValue /= 6; // 1/6 chance of each happening
	value += nextTurnValue;
	System.out.println("--- if " + legion
			   + " moves to " + hex
			   + " total score " + value);
		
	// TODO: consider mobility.  e.g., penalty for suckdown
	// squares, bonus if next to tower

	// TODO: consider risk of being attacked

	// TODO: consider nearness to our other legions

	// TODO: consider being a scooby snack

	// TODO: consider risk of being scooby snacked (this might be inherent)

	// TODO: consider splitting up our good recruitment rolls
	// (i.e. if another legion has warbears under the top that
	// recruit on 1,3,5, and we have a behemoth with choice of 3/5
	// to jungle or 2/6 to jungle, prefer the 2/6 location).
	return value;
    }

    
    class MasterBoardPosition implements Minimax.GamePosition
    {
	private Game game;
	public MasterBoardPosition(Game game)
	{
	    this.game = game;
	}

	// always maximize score
	public int maximize() 
	{ 
	    throw new Error("not implemented");
	}

	public int evaluation() 
	{ 
	    // value of this board position is the sum of the values
	    // of moving each legion to its current location.  we'll
	    // need to cache this stuff for sure note: evaluation fn
	    // must somehow have a lower score for legions at risk of
	    // enemy attack, so that the MINIMIZE pass makes sense
	    throw new Error("not implemented");
	}

	/** plausible move generator
	 * may be lazy; also, may implement forward pruning.
	 * @return an iterator which returns Move's, in the order
	 * that they should be evaluated by Minimax.  The moves should 
	 * extend Move, and be handled by applyMove().
	 */
	public Iterator generateMoves()
	{
	    // forward pruning idea:  if a legion
	    // does not interact with any other legions (i.e. all its moves
	    // are unblocked), then reduce its choices to only the move with
	    // the best evaluation.
	    throw new Error("not implemented");
	}

	/** 
	 * advance game state ( this should adjust maximize() as necessary ) 
	 */
	public Minimax.GamePosition applyMove(Minimax.Move move)
	{
	    throw new Error("not implemented");
	}
    }
    
    class MasterBoardPositionMove implements Minimax.Move
    {
	private int value;
	public void setValue(int value) { this.value = value; }
	public int getValue() { return value; }
    }
}
