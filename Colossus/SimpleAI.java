/**
 *
 * Simple implementation of a Titan AI
 * @version $Id$
 * @author Bruce Sherrod
 *
 *
 * TODO: force AI to move at least one legion
 * TODO: force AI to move legions which split 
 */

import java.io.*;
import java.util.*;

class SimpleAI implements AI
{
    private static final boolean DEBUG = true;
    private Minimax minimax = new Minimax();

    public void muster (Game game)
    {
        // Do not recruit if this legion is a scooby snack.
        double scoobySnackFactor = 0.15;
        int minimumSizeToRecruit = (int)(scoobySnackFactor * 
            game.getAverageLegionPointValue());

        Player player = game.getActivePlayer();
        List legions = player.getLegions();
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (legion.hasMoved() && legion.canRecruit() && 
                (legion.numCreature(Creature.titan) >= 1 || 
                legion.getPointValue() >= minimumSizeToRecruit))
            {
		Creature recruit = chooseRecruit(game,legion, legion.getCurrentHex());
                if (recruit != null)
                {
                    game.doRecruit(recruit, legion, game.masterFrame);
                }
		
            }
        }
        game.board.unselectAllHexes();
        game.updateStatusScreen();
    }
		

    /**
     * pick what we would want to recruit with this legion in this hex.
     * @return the Creature to recruit or null 
     */
    private Creature chooseRecruit(Game game, Legion legion, MasterHex hex)
    {
	List recruits = game.findEligibleRecruits(legion, hex);
	if (recruits.size() == 0) return null;
	// pick the last creature in the list (which is the best/highest recruit)
	Creature recruit = (Creature)recruits.get(recruits.size() - 1); 
	// take third cyclops in brush 
	if (recruit == Creature.gorgon 
	    && recruits.contains(Creature.cyclops) 
	    && legion.numCreature(Creature.behemoth) == 0 
	    && legion.numCreature(Creature.cyclops) == 2)
	{
	    recruit = Creature.cyclops;
	} 
	// take a fourth cyclops in brush 
	// (so that we can split out a cyclops and still keep 3)
	else if (recruit == Creature.gorgon 
		 && recruits.contains(Creature.cyclops) 
		 && legion.getHeight() == 6
		 && legion.numCreature(Creature.behemoth) == 0
		 && legion.numCreature(Creature.gargoyle) == 0) 
	{
	    recruit = Creature.cyclops;
	} 
	// prefer warlock over guardian (should be built in now)
	else if ( recruits.contains(Creature.guardian) 
		  && recruits.contains(Creature.warlock) )
	{
	    recruit = Creature.warlock;
	}
	// take a third lion/troll if we've got at least 1 way to desert/swamp
	// from here and we're not about to be attacked 
	else if ( recruits.contains(Creature.lion) 
	     && recruits.contains(Creature.ranger) 
	     && legion.numCreature(Creature.lion) == 2
	     && getNumberOfWaysToTerrain(legion,hex,'D') > 0)
	{
	    recruit = Creature.lion;
	}
	else if ( recruits.contains(Creature.troll) 
	     && recruits.contains(Creature.ranger) 
	     && legion.numCreature(Creature.troll) == 2
	     && getNumberOfWaysToTerrain(legion,hex,'S') > 0)
	{
	    recruit = Creature.troll;
	} 
	// tower creature selection:
	else if ( recruits.contains(Creature.ogre)
		  && recruits.contains(Creature.centaur)
		  && recruits.contains(Creature.gargoyle)
		  && recruits.size() == 3)
	{
	    // if we have 2 centaurs or ogres, take a third
	    if (legion.numCreature(Creature.ogre) == 2)
		recruit = Creature.ogre;
	    else if (legion.numCreature(Creature.centaur) == 2)
		recruit = Creature.centaur;
	    // else if we have 1 of a tower creature, take a matching one
	    else if (legion.numCreature(Creature.gargoyle) == 1)
		recruit = Creature.gargoyle;
	    else if (legion.numCreature(Creature.ogre) == 1)
		recruit = Creature.ogre;
	    else if (legion.numCreature(Creature.centaur) == 1)
		recruit = Creature.centaur; 
	    // else if there's cyclops left and we don't have 2 gargoyles, take a gargoyle 
	    else if (game.getCaretaker().getCount(Creature.cyclops) > 6
		     && legion.numCreature(Creature.gargoyle) < 2 )
		recruit = Creature.gargoyle;
	    // else if there's trolls left and we don't have 2 ogres, take an ogre 
	    else if (game.getCaretaker().getCount(Creature.troll) > 6
		     && legion.numCreature(Creature.ogre) < 2 )
		recruit = Creature.ogre;
	    // else if there's lions left and we don't have 2 lions, take a centaur 
	    else if (game.getCaretaker().getCount(Creature.lion) > 6
		     && legion.numCreature(Creature.centaur) < 2 )
		recruit = Creature.centaur;
	    // else we dont really care; take anything 
	}
	return recruit;
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

	    // don't split if we'll be forced to attack 
	    int forcedToAttack = 0;
	    for (int roll = 1; roll <= 6; roll++)
	    {
		Set moves = game.listMoves(legion,true,legion.getCurrentHex(),roll);
		Iterator moveIt = moves.iterator();
		int safeMoves = 0;
		while (moveIt.hasNext())
		{
		    String hexLabel = (String) moveIt.next();
		    MasterHex hex = game.board.getHexFromLabel(hexLabel); 
		    if (hex.getNumEnemyLegions(player) == 0)
			safeMoves++;
		}
		if (safeMoves == 0)
		    forcedToAttack ++;
	    }
	    if (forcedToAttack >= 2)
	    {
		continue;  // we'll be forced to attack on 2 or more rolls; don't split
	    }

	    // TODO: don't split if we're about to be attacked and we
	    // need the muscle 
	    // TODO: don't split if we're about to attack and we need
	    // the muscle 
	    // TODO: don't split if there's no upwards recruiting
	    // potential from our current location

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
	    Marker oldMarker = legion.getMarker();
	    String imageName = selectedMarkerId;
	    Marker newMarker = new Marker(oldMarker.getBounds().width,
					  imageName, game.board, newLegion);
	    newLegion.setMarker(newMarker); 
	    MasterHex hex = newLegion.getCurrentHex();
	    newLegion.getCurrentHex().addLegion(newLegion, false);
	    legionsToAdd.addLast(newLegion);

	    if (legion.getHeight() == 8)
	    {
		doInitialGameSplit(game,hex,legion,newLegion);
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
		// TODO: keep 3 cyclops if we don't have a behemoth
		// (split out a gorgon instead)
		//
		// TODO: prefer to split out creatures that have no
		// recruiting value (e.g. if we have 1 angel, 2
		// centaurs, 2 gargoyles, and 2 cyclops, split out the
		// gargoyles)
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

    // From Hugh Moore:
    //
    //  It really depends on how many players there are and how good I
    //  think they are.  In a 5 or 6 player game, I will pretty much
    //  always put my gargoyles together in my Titan group. I need the
    //  extra strength, and I need it right away.  In 3-4 player
    //  games, I certainly lean toward putting my gargoyles together.
    //  If my opponents are weak, I sometimes split them for a
    //  challenge.  If my opponents are strong, but my situation looks
    //  good for one reason or another, I may split them.  I never
    //  like to split them when I am in tower 3 or 6, for obvious
    //  reasons. In two player games, I normally split the gargoyles,
    //  but two player games are fucked up.
    //

    /** helper for split */
    private void doInitialGameSplit(Game game, MasterHex hex, 
				    Legion legion, Legion newLegion)
    {
	final Creature titan = legion.removeCreature( Creature.titan, false, false);
	final Creature angel = legion.removeCreature( Creature.angel, false, false);
	final Creature gargoyle1 = legion.removeCreature( Creature.gargoyle, false, false);
	final Creature gargoyle2 = legion.removeCreature( Creature.gargoyle, false, false);
	final Creature ogre1 = legion.removeCreature( Creature.ogre, false, false);
	final Creature ogre2 = legion.removeCreature( Creature.ogre, false, false);
	final Creature centaur1 = legion.removeCreature( Creature.centaur, false, false);
	final Creature centaur2 = legion.removeCreature( Creature.centaur, false, false);
	final String label = hex.getLabel();
	// in CMU style splitting, we split centaurs in even towers,
	// ogres in odd towers.  
	final boolean oddTower = 
	    "100".equals(label) || "300".equals(label) || "500".equals(label);
	final Creature splitCreature1 = oddTower?ogre1:centaur1;
	final Creature splitCreature2 = oddTower?ogre2:centaur2;
	final Creature nonsplitCreature1 = oddTower?centaur1:ogre1;
	final Creature nonsplitCreature2 = oddTower?centaur2:ogre2;

	// randomize the two legion markers
	final Legion legion1; 
	final Legion legion2;
	if (game.random.nextInt(10) % 2 == 0)
	{
	    legion1 = legion; 
	    legion2 = newLegion;
	}
	else
	{
	    legion1 = legion; 
	    legion2 = newLegion;
	}
 
	// if lots of players, keep gargoyles with titan (we need the muscle)
	if (game.getNumPlayers() > 4)
	{
	    legion1.addCreature(titan);
	    legion1.addCreature(gargoyle1);
	    legion1.addCreature(gargoyle2);
	    legion1.addCreature(splitCreature1);
	    legion2.addCreature(angel);
	    legion2.addCreature(splitCreature2);
	    legion2.addCreature(nonsplitCreature1);
	    legion2.addCreature(nonsplitCreature2);
	}
	// don't split gargoyles in tower 3 or 6 (because of the extra jungles)
	else if ("300".equals(label) || "600".equals(label))
	{
	    if (game.random.nextInt(10) % 2 ==0)
	    {
		legion1.addCreature(titan);
		legion2.addCreature(angel);
	    }
	    else
	    {
		legion1.addCreature(angel);
		legion2.addCreature(titan);
	    } 
	    legion1.addCreature(gargoyle1);
	    legion1.addCreature(gargoyle2);
	    legion1.addCreature(splitCreature1);
	    legion2.addCreature(splitCreature2);
	    legion2.addCreature(nonsplitCreature1);
	    legion2.addCreature(nonsplitCreature2);
	}
	//
	// new idea due to David Ripton: split gargoyles in tower 2 or
	// 5, because on a 5 we can get to brush and jungle and get 2
	// gargoyles.  I'm not sure if this is really better than recruiting
	// a cyclops and leaving the other group in the tower, but it's
	// interesting so we'll try it.
	//
	else if ("200".equals(label) || "500".equals(label))
	{
	    // MIT split
	    legion1.addCreature(titan);
	    legion1.addCreature(nonsplitCreature1);
	    legion1.addCreature(nonsplitCreature2);
	    legion1.addCreature(gargoyle1); 
	    legion2.addCreature(angel);
	    legion2.addCreature(splitCreature1);
	    legion2.addCreature(splitCreature2);
	    legion2.addCreature(gargoyle2);
	}
	//
	// otherwise, mix it up for fun
	else
	{
	    if (game.random.nextInt(100) <= 33)
	    {
		// MIT split
		legion1.addCreature(titan);
		legion1.addCreature(nonsplitCreature1);
		legion1.addCreature(nonsplitCreature2);
		legion1.addCreature(gargoyle1); 
		legion2.addCreature(angel);
		legion2.addCreature(splitCreature1);
		legion2.addCreature(splitCreature2);
		legion2.addCreature(gargoyle2);
	    }
	    else
	    {
		// CMU split
		legion1.addCreature(titan);
		legion1.addCreature(gargoyle1);
		legion1.addCreature(gargoyle2);
		legion1.addCreature(splitCreature1);
		legion2.addCreature(angel);
		legion2.addCreature(splitCreature2);
		legion2.addCreature(nonsplitCreature1);
		legion2.addCreature(nonsplitCreature2);
	    }
	}
    }


    public void move (Game game)
    {
	if (true)
	{
	    simpleMove(game);
	}
	else
	{
	    if (DEBUG) System.out.println("minimax..");
	    PlayerMove playermove = (PlayerMove) minimax.search
		( new MasterBoardPosition(game,game.getActivePlayerNum()),1);
	    // apply the PlayerMove 
	    if (DEBUG) System.out.println("applying minimax moves..");
	    for (Iterator it = playermove.moves.entrySet().iterator(); it.hasNext();)
	    {
		Map.Entry entry = (Map.Entry) it.next();
		Legion legion = (Legion) entry.getKey();
		MasterHex hex = (MasterHex) entry.getValue();
		if (DEBUG) System.out.println("moving " + legion + " to " + hex);
		game.actOnLegion(legion);
		game.actOnHex(hex);
	    }
	    if (DEBUG) System.out.println("done applying minimax moves..");
	}
    }

    public void simpleMove (Game game)
    {
	// TODO: make sure we move at least one legion
        Player player = game.getActivePlayer();

	// consider mulligans
	// TODO: this is really stupid.  do something smart here.
	// idea: hard code an "opening book" of turn 1 moves based
	//  on our splits and die rolls.
	if (player.getMulligansLeft() > 0 && player.getMovementRoll() == 2)
	{ 
	    player.takeMulligan();
	    player.rollMovement();
	}

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
	final Legion enemyLegion = hex.getEnemyLegion(legion.getPlayer());
	if (enemyLegion != null)
	{
	    final int enemyPointValue = enemyLegion.getPointValue();
	    final int result = estimateBattleResults(legion, enemyLegion, hex);
	    switch (result)
	    {
		case WIN_WITH_MINIMAL_LOSSES:
		    // we score a fraction of an angel
		    value += (24 * enemyPointValue) / 100; 
		    // plus a fraction of a titan
		    value += (6 * enemyPointValue) / 100; 
		    // plus some more for killing a group (this is arbitrary)
		    value += (10 * enemyPointValue) / 100; 
		    // TODO: if enemy titan, we also score half points
		    // (this may make the AI unfairly gun for your titan)
		    break;
		case WIN_WITH_HEAVY_LOSSES:
		    // don't do this with our titan unless we can win the game
		    {
			if (legion.numCreature(Creature.titan) > 0
			    && game.getNumLivingPlayers() > 2)
			    // ack! we'll fuck up our titan group
			    value = Integer.MIN_VALUE;
			else
			{
			    // we score a fraction of an angel & titan strength
			    value += (30 * enemyPointValue) / 100; 
			    // but we lose this group
			    value -= (10 * legion.getPointValue()) / 100; 
			    // TODO: if we have no other angels, more penalty here
			    // TODO: if enemy titan, we also score half points
			    // (this may make the AI unfairly gun for your titan)
			} 
		    }
		    break;
		case DRAW:
		    {
			// If not our titan, but is enemy titan, do it.  
			// This might be an unfair use of
			// information for the AI
			if (legion.numCreature(Creature.titan) == 0
			    && enemyLegion.numCreature(Creature.titan) > 0)
			{
			    // arbitrary value for killing a player
			    // but scoring no points: it's worth a
			    // little 
			    // TODO: if there's only 2 players, we
			    // should do this
			    value += enemyPointValue / 6;
			}
			else
			{
			    // otherwise no thanks
			    value = Integer.MIN_VALUE;
			}
		    }
		    break;
		case LOSE_BUT_INFLICT_HEAVY_LOSSES:
		    {
			// TODO: how important is it that we damage
			// his group?
			value = Integer.MIN_VALUE; 
		    }
		    break;
		case LOSE:
		    {
			value = Integer.MIN_VALUE; 
		    }
		    break;
	    }
	}

	// consider what we can recruit
	if (canRecruitHere)
	{
	    Creature recruit = chooseRecruit(game,legion,hex);
	    if (recruit != null)
	    {
		int oldval = value;
		if (legion.getHeight() < 6)
		    value += recruit.getPointValue();
		else
		{
		    // Idea:
		    // if we're 6-high, then the value of a recruit is
		    // equal to the improvement in the value of the
		    // pieces that we'll have after splitting.

		    // TODO this should call our splitting code to see
		    // what split decision we would make

		    //if (DEBUG) System.out.println("--- 6-HIGH SPECIAL CASE");
		    
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
		//if (DEBUG) System.out.println("--- if " + legion
		//+ " moves to " + hex
		//+ " then it could recruit " 
		//+  recruit.toString()
		//+ " (adding " + (value-oldval) + " to score)" );
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
	    //if (DEBUG && bestRecruit != null)
		//System.out.println("--- if " + legion
				   //+ " moves to " + hex
				   //+ " then it could recruit " 
				   //+  bestRecruit.toString()
				   //+ " on a " + roll +  " next turn..."
				   //+ " (adding " + (bestRecruitVal/6.0) + " to score)"
				   //);
	}
	nextTurnValue /= 6; // 1/6 chance of each happening
	value += nextTurnValue;
	//if (DEBUG) System.out.println("--- if " + legion
			   //+ " moves to " + hex
			   //+ " total score " + value);
		
	// TODO: consider mobility.  e.g., penalty for suckdown
	// squares, bonus if next to tower or under the top

	// TODO: consider what we can attack next turn from here

	// TODO: consider risk of being attacked

	// TODO: consider nearness to our other legions

	// TODO: consider being a scooby snack

	// TODO: consider risk of being scooby snacked (this might be inherent)

	// TODO: consider splitting up our good recruitment rolls
	// (i.e. if another legion has warbears under the top that
	// recruit on 1,3,5, and we have a behemoth with choice of 3/5
	// to jungle or 4/6 to jungle, prefer the 4/6 location).
	return value;
    }


    private static final int WIN_WITH_MINIMAL_LOSSES = 0;
    private static final int WIN_WITH_HEAVY_LOSSES = 1;
    private static final int DRAW = 2;
    private static final int LOSE_BUT_INFLICT_HEAVY_LOSSES = 3;
    private static final int LOSE = 4;
    
    /**
     * estimate the outcome of a battle
     * @param attacker the attacking legion
     * @param defender the defending legion
     * @param hex the hex where the battle takes place (the hex should
     * contain info about which side the attacker enters from )
     * @return a constant int indicating the attack result.
     */
    private int estimateBattleResults(Legion attacker, Legion defender, MasterHex hex)
    {
	int attackerPointValue = attacker.getPointValue();
	// TODO: reduce PV slightly if titan is present and weak (and thus can't fight)
	// TODO: add angel call 
	int defenderPointValue = defender.getPointValue();
	// TODO: reduce PV slightly if titan is present and weak (and thus cant' fight)
	// TODO: add in enemy's most likely turn 4 recruit

	// TODO: adjust for natives / terrain type
	// TODO: adjust for entry side 

	// really dumb estimator
	double ratio = (double) attackerPointValue / (double) defenderPointValue; 
	if (ratio >= 1.25)
	    return WIN_WITH_MINIMAL_LOSSES;
	else if (ratio >= 1.10)
	    return WIN_WITH_HEAVY_LOSSES;
	else if (ratio >= 0.9)
	    return DRAW;
	else if (ratio >= 0.75)
	    return LOSE_BUT_INFLICT_HEAVY_LOSSES;
	else  // ratio less than 0.75
	    return LOSE;
    }


    /**
     *  Count the number of ways that this legion can get to a terrain
     *  type of the given type from the given hex.  Include lord
     *  teleport if possible, and account for blocking enemy groups.
     *
     *  This method is very similar to Game.listMoves().
     * 
     */
    private int getNumberOfWaysToTerrain(Legion legion, MasterHex hex, char terrainType)
    {
	// if moves[i] is true, then a roll of i can get us to terrainType.
	boolean[] moves = new boolean[7];

	//consider normal moves
        int block = Game.ARCHES_AND_ARROWS;
        for (int j = 0; j < 6; j++)
        {
            if (hex.getExitType(j) == MasterHex.BLOCK)
            {
                // Only this path is allowed.
                block = j;
            }
        } 
        findNormalMovesToTerrain(legion, legion.getPlayer(), hex, 
				 6, block, Game.NOWHERE, 
				 terrainType, moves);

	// consider tower teleport
	if (hex.getTerrain() == 'T' 
	    && legion.numLords() > 0 
	    && moves[6] == false)
	{
	    // hack: assume that we can always tower teleport to the terrain we
	    // want to go to. 
	    // TODO: check to make sure there's an open terrain to teleport to.
	    // (probably want a lookup table for this)
	    moves[6] = true;
	}
	int count = 0;
	for (int i =0; i < moves.length; i++)
	    if (moves[i]) count++;
	return count;
    }

    void findNormalMovesToTerrain(Legion legion, Player player, MasterHex hex, 
				  int roll, int block, int cameFrom, 
				  char terrainType, boolean[] moves)
    {
        // If there are enemy legions in this hex, mark it
        // as a legal move and stop recursing.  If there is
        // also a friendly legion there, just stop recursing.
        if (hex.getNumEnemyLegions(player) > 0)
        {
            if (hex.getNumFriendlyLegions(player) == 0)
            {
		// we can move to here
		if (hex.getTerrain() == terrainType)
		{
		    moves[roll] = true;
		}
            }
            return;
        } 
        if (roll == 0)
        {
            // This hex is the final destination.  Mark it as legal if
            // it is unoccupied by friendly legions.
            for (int i = 0; i < player.getNumLegions(); i++)
            {
                // Account for spin cycles.
                if (player.getLegion(i).getCurrentHex() == hex &&
                    player.getLegion(i) != legion)
                {
                    return;
                }
            } 
	    if (hex.getTerrain() == terrainType)
	    {
		moves[roll] = true;
	    } 
            return;
        } 
        if (block >= 0)
        {
	    findNormalMovesToTerrain( legion, player, hex.getNeighbor(block), 
				      roll-1, Game.ARROWS_ONLY, (block+3)%6, 
				      terrainType, moves);
        }
        else if (block == Game.ARCHES_AND_ARROWS)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= MasterHex.ARCH && i != cameFrom)
                {
		    findNormalMovesToTerrain( legion, player, hex.getNeighbor(i), 
					      roll - 1, Game.ARROWS_ONLY, (i + 3) % 6, 
					      terrainType, moves);
                }
            }
        }
        else if (block == Game.ARROWS_ONLY)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= MasterHex.ARROW && i != cameFrom)
                {
		    findNormalMovesToTerrain( legion, player, hex.getNeighbor(i), 
					      roll - 1, Game.ARROWS_ONLY, (i + 3) % 6, 
					      terrainType, moves);
                }
            }
        } 
        return;
    }




    ////////////////////////////////////////////////////////////////
    // minimax stuff
    ////////////////////////////////////////////////////////////////
    
    class MasterBoardPosition implements Minimax.GamePosition
    {
	// AICopy() of the game
	Game game;
	// the player for whom we're doing the evaluation.
	// note, this is NOT the same as Game.getActivePlayerNum()
	int activePlayerNum;

	public MasterBoardPosition(Game game, int activePlayerNum)
	{
	    this.game = game.AICopy();
	    this.activePlayerNum = activePlayerNum;
	}

	public MasterBoardPosition(MasterBoardPosition position)
	{
	    this.game = position.game.AICopy();
	    this.activePlayerNum = position.activePlayerNum;
	}

	public int maximize() 
	{ 
	    if (activePlayerNum < 0)
		return Minimax.AVERAGE;
	    else if (game.getActivePlayerNum() == activePlayerNum)
		return Minimax.MAXIMIZE;
	    else
		return Minimax.MINIMIZE; 
	}

	public int evaluation() 
	{ 
	    if (DEBUG) System.out.println("evaluating game position");

	    // TODO: need to correct for the fact that more material
	    // is not always better.
	    // idea: score for legion markers available?
	    final Player activePlayer = game.getPlayer(Math.abs(activePlayerNum)); 

	    // check for loss 
	    if (activePlayer.isDead())
	    {
		if (DEBUG) System.out.println("evaluation: loss! " + Integer.MIN_VALUE); 
		return Integer.MIN_VALUE;
	    }

	    // check for victory
	    {
		int playersRemaining = 0;
		Iterator it = game.getPlayers().iterator();
		while (it.hasNext())
		{
		    Player player = (Player)it.next();
		    if (!player.isDead())
		    {
			playersRemaining++;
		    }
		}
		switch (playersRemaining)
		{
		    case 0: 
			{
			    if (DEBUG) System.out.println("evaluation: draw! " + 0);
			    return 0; 
			}
		    case 1: 
			{
			    if (DEBUG) System.out.println("evaluation: win! " 
							  + Integer.MAX_VALUE);
			    return Integer.MAX_VALUE;
			}
		}
	    }

	    int value = 0;
	    for (Iterator playerIt = game.getPlayers().iterator(); playerIt.hasNext();)
	    {
		Player player = (Player) playerIt.next();
		if (player == activePlayer)
		{
		    for (Iterator it = player.getLegions().iterator(); it.hasNext();)
		    {
			Legion legion = (Legion) it.next();
			value += evaluateMove(game,
					      legion, 
					      legion.getCurrentHex(), 
					      legion.hasMoved());
		    }
		    // TODO: add additional value for player having
		    // stacks near each other
		}
		else
		{
		    for (Iterator it = player.getLegions().iterator(); it.hasNext();)
		    {
			Legion legion = (Legion) it.next();
			value += evaluateMove(game,
					      legion, 
					      legion.getCurrentHex(), 
					      legion.hasMoved());
		    }
		    // TODO: add additional value for player having
		    // his stacks near each other
		}
	    }
	    if (DEBUG) System.out.println("evaluation: " + value);
	    return value; 
	}

	/** plausible move generator
	 * may be lazy; also, may implement forward pruning.
	 * @return an iterator which returns Move's, in the order
	 * that they should be evaluated by Minimax.  The moves should 
	 * extend Move, and be handled by applyMove().
	 */
	public Iterator generateMoves()
	{
	    if (DEBUG) System.out.println("generating moves..");

	    // check for loss 
	    final Player activePlayer = game.getPlayer(Math.abs(activePlayerNum)); 
	    if (activePlayer.isDead()) // oops! we lost
		return new ArrayList().iterator();  // no moves

	    // check for victory
	    {
		int playersRemaining = 0;
		Iterator it = game.getPlayers().iterator();
		while (it.hasNext())
		{
		    Player player = (Player)it.next();
		    if (!player.isDead())
		    {
			playersRemaining++;
		    }
		}
		if (playersRemaining < 2) // draw or win
		    return new ArrayList().iterator();  // no moves
	    }
		
	    // dice moves
	    if (activePlayerNum < 0)
	    {
		// dice moves
		int playernum = game.getActivePlayerNum() * -1;
		ArrayList moves = new ArrayList(6);
		for (int i = 1; i <= 6; i++)
		{
		    moves.add(new DiceMove(i, this));
		} 
		return moves.iterator();
	    }


	    // enumerate moves for player i
	    System.out.println("hack! not considering all moves..");

	    // HACK: consider all moves for first legion only.
	    // (this is just for testing)
	    ArrayList allmoves = new ArrayList();
	    Legion legion = (Legion) game.getActivePlayer().getLegions().get(0);
	    for (Iterator it = game.listMoves(legion,true).iterator(); it.hasNext();)
	    {
		String hexLabel = (String) it.next();
		MasterHex hex = game.board.getHexFromLabel(hexLabel);
		HashMap moves = new HashMap();
		moves.put(legion, hex);
		PlayerMove move = new PlayerMove(moves, this);
		allmoves.add(move);
	    }

	    System.out.println("considering " + allmoves.size() + " possible moves " );

	    return allmoves.iterator();
	}

	/** 
	 * advance game state ( this should adjust maximize() as necessary ) 
	 */
	public Minimax.GamePosition applyMove(Minimax.Move move)
	{
	    if (DEBUG) System.out.println("applying moves..");

	    if (move instanceof DiceMove)
	    {
		if (DEBUG) System.out.println("applying dice move");
		// apply dice rolling
		DiceMove dicemove = (DiceMove) move;
		MasterBoardPosition position = new MasterBoardPosition(dicemove.position);
		position.activePlayerNum = Math.abs(activePlayerNum);
		int roll = dicemove.roll;
		position.game.getActivePlayer().setMovementRoll(roll);
		return position;
	    }
	    else if (move instanceof PlayerMove)
	    {
		if (DEBUG) System.out.println("applying player move");

		PlayerMove playermove = (PlayerMove) move;
		MasterBoardPosition position = new MasterBoardPosition(playermove.position); 
		// apply the PlayerMove moves
		for (Iterator it = playermove.moves.entrySet().iterator(); it.hasNext();)
		{
		    Map.Entry entry = (Map.Entry) it.next();
		    Legion legion = (Legion) entry.getKey();
		    MasterHex hex = (MasterHex) entry.getValue();
		    if (DEBUG) System.out.println("applymove: try " + legion + " to " +hex);
		    game.actOnLegion(legion);
		    game.actOnHex(hex);
		}
		// advance phases until we reach the next move phase
		game.advancePhase();
		while (game.getPhase() != Game.MOVE)
		{
		    switch (game.getPhase())
		    {
			case Game.FIGHT:
			    {
				// fake resolution for all fights
				// TODO: need more accurate fight estimator
				Player player = game.getActivePlayer();
				for (int i = 0; i < player.getNumLegions(); i++)
				{
				    Legion legion = player.getLegion(i);
				    MasterHex hex = legion.getCurrentHex();
				    Legion enemy = hex.getEnemyLegion(player);
				    if (enemy == null) continue;
				    Player enemyPlayer = enemy.getPlayer();
				    int myPV = legion.getPointValue();
				    int enemyPV = enemy.getPointValue();
				    boolean myTitan = legion.numCreature(Creature.titan)>0;
				    boolean enemyTitan = enemy.numCreature(Creature.titan)>0;
				    if (myPV * 0.8 > enemyPV)
				    {
					// i win
					enemy.remove();
					player.addPoints(enemyPV);
					if (enemyTitan)
					    enemyPlayer.die(player,false);
				    }
				    else if (enemyPV * 0.8 > myPV)
				    {
					// enemy wins
					legion.remove();
					enemyPlayer.addPoints(myPV);
					if (myTitan)
					    player.die(enemyPlayer,false);
				    }
				    else
				    {
					// both groups destroyed
					legion.remove();
					enemy.remove(); 
				    } 
				}
			    }
			    break;
			case Game.SPLIT:
			    {
				split(game); 
			    }
			    break;
			case Game.MUSTER:
			    {
				muster(game);
			    }
			    break;
		    }
		    // now advance again until we get to MOVE phase
		    game.advancePhase();
		}
		// set activePlayer negative so that we average over dice rolls
		position.activePlayerNum = -1 * Math.abs(activePlayerNum);
		return position;
	    }
	    else 
		throw new RuntimeException("ack! bad move type");
	}
    }
    
    class MasterBoardPositionMove implements Minimax.Move
    {
	protected MasterBoardPosition position;
	private int value;
	public void setValue(int value) { this.value = value; }
	public int getValue() { return value; }
    } 

    class DiceMove extends MasterBoardPositionMove 
    { 
	int roll;
	public DiceMove(int roll, MasterBoardPosition position)
	{
	    this.position = position;
	    this.roll = roll;
	}
    }

    class PlayerMove extends MasterBoardPositionMove 
    {
	/** map of Legion to Hex where the legion moves to  */
	HashMap moves;
	public PlayerMove(HashMap moves, MasterBoardPosition position)
	{
	    this.position = position;
	    this.moves = moves;
	}
    }
}
