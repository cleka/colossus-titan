package net.sf.colossus.client;


import java.util.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.util.Probs;
import net.sf.colossus.util.Perms;
import net.sf.colossus.util.Options;
import net.sf.colossus.parser.TerrainRecruitLoader;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Dice;
import net.sf.colossus.server.VariantSupport;

/**
 * Simple implementation of a Titan AI
 * @version $Id$
 * @author Bruce Sherrod, David Ripton
 */


public class SimpleAI implements AI
{
    private Client client;
    private int timeLimit = Constants.DEFAULT_AI_TIME_LIMIT;  // in s
    private boolean timeIsUp;


    public SimpleAI(Client client)
    {
        this.client = client;
    }


    public String pickColor(java.util.List colors, List favoriteColors)
    {
        Iterator it = favoriteColors.iterator();
        while (it.hasNext())
        {
            String preferredColor = (String)it.next();
            if (colors.contains(preferredColor))
            {
                return preferredColor;
            }
        }
        // Can't have one of our favorites, so take what's there.
        it = colors.iterator();
        if (it.hasNext())
        {
            return (String)it.next();
        }
        return null;
    }

    public String pickMarker(Set markerIds, String preferredShortColor)
    {
        Iterator it = markerIds.iterator();
        String markerId = null;
        while (it.hasNext())
        {
            markerId = (String)it.next();
            // Prefer own marker color.
            if (preferredShortColor != null && 
                markerId.startsWith(preferredShortColor))
            {
                return markerId;
            }
        }
        // Could not find one of own color -- return whatever we
        // could find.  Will be null if no markers are available.
        return markerId;
    }


    public void muster()
    {
        // Do not recruit if this legion is a scooby snack.
        double scoobySnackFactor = 0.15;
        int minimumSizeToRecruit = (int)(scoobySnackFactor *
            client.getAverageLegionPointValue());
        List markerIds = client.getLegionsByPlayer(client.getPlayerName());
        Iterator it = markerIds.iterator();
        while (it.hasNext())
        {
            String markerId = (String)it.next();
            LegionInfo legion = client.getLegionInfo(markerId);

            if (legion.hasMoved() && legion.canRecruit() &&
                (legion.hasTitan() || legion.getPointValue() >=
                    minimumSizeToRecruit))
            {
                Creature recruit = chooseRecruit(legion, legion.getHexLabel());

                if (recruit != null)
                {
                    List recruiters = client.findEligibleRecruiters(
                        markerId, recruit.getName());

                    String recruiterName = null;
                    if (!recruiters.isEmpty())
                    {
                        // Just take the first one.
                        recruiterName = (String)recruiters.get(0);
                    }
                    client.doRecruit(markerId, recruit.getName(), 
                        recruiterName);
                }
            }
        }
    }


    public void reinforce(LegionInfo legion)
    {
        Creature recruit = chooseRecruit(legion, legion.getHexLabel());
        String recruitName = null;
        String recruiterName = null;
        if (recruit != null)
        {
            recruitName = recruit.getName();
            List recruiters = client.findEligibleRecruiters(
                legion.getMarkerId(), recruit.getName());
            if (!recruiters.isEmpty())
            {
                recruiterName = (String)recruiters.get(0);
            }
        }
        // Call regardless to advance past recruiting.
        client.doRecruit(legion.getMarkerId(), recruitName, recruiterName);
    }


    private Creature chooseRecruit(LegionInfo legion, String hexLabel)
    {
        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);

        List recruits = client.findEligibleRecruits(legion.getMarkerId(), 
            hexLabel);

        if (recruits.size() == 0)
        {
            return null;
        }

        // pick the last creature in the list (which is the
        // best/highest recruit)
        Creature recruit = (Creature)recruits.get(recruits.size() - 1);

        // take third cyclops in brush
        if (recruit == Creature.getCreatureByName("Gorgon")
            && recruits.contains(Creature.getCreatureByName("Cyclops"))
            && legion.numCreature(Creature.getCreatureByName("Behemoth")) == 0
            && legion.numCreature(Creature.getCreatureByName("Cyclops")) == 2)
        {
            recruit = Creature.getCreatureByName("Cyclops");
        }
        // take a third lion/troll if we've got at least 1 way to desert/swamp
        // from here and we're not about to be attacked
        else if (recruits.contains(Creature.getCreatureByName("Lion"))
            && recruits.contains(Creature.getCreatureByName("Ranger"))
            && legion.numCreature(Creature.getCreatureByName("Lion")) == 2
            && getNumberOfWaysToTerrain(legion, hex, 'D') > 0)
        {
            recruit = Creature.getCreatureByName("Lion");
        }
        else if (recruits.contains(Creature.getCreatureByName("Troll"))
            && recruits.contains(Creature.getCreatureByName("Ranger"))
            && legion.numCreature(Creature.getCreatureByName("Troll")) == 2
            && getNumberOfWaysToTerrain(legion, hex, 'S') > 0)
        {
            recruit = Creature.getCreatureByName("Troll");
        }
        // tower creature selection:
        else if (recruits.contains(Creature.getCreatureByName("Ogre")) &&
                 recruits.contains(Creature.getCreatureByName("Centaur")) &&
                 recruits.contains(Creature.getCreatureByName("Gargoyle")) &&
                 recruits.size() == 3)
        {
            // if we have 2 centaurs or ogres, take a third
            if (legion.numCreature(Creature.getCreatureByName("Ogre")) == 2)
            {
                recruit = Creature.getCreatureByName("Ogre");
            }
            else if (legion.numCreature(
                Creature.getCreatureByName("Centaur")) == 2)
            {
                recruit = Creature.getCreatureByName("Centaur");
                // else if we have 1 of a tower creature, take a matching one
                // if more than one tower creature is a possible recruit,
                // take the one with the more higher-level creatures
                // still available
            }
            else if ((legion.numCreature(
                          Creature.getCreatureByName("Gargoyle")) == 1) ||
                     (legion.numCreature(
                          Creature.getCreatureByName("Ogre")) == 1) ||
                     (legion.numCreature(
                          Creature.getCreatureByName("Centaur")) == 1))
            {
                int cyclopsLeft = client.getCreatureCount(
                    Creature.getCreatureByName("Cyclops"));
                int lionLeft = client.getCreatureCount(
                    Creature.getCreatureByName("Lion"));
                int trollLeft = client.getCreatureCount(
                    Creature.getCreatureByName("Troll"));
                if (legion.numCreature(
                        Creature.getCreatureByName("Gargoyle")) != 1)
                { // don't take a gargoyle -> ignore cyclops
                    cyclopsLeft = -1;
                }
                if (legion.numCreature(
                        Creature.getCreatureByName("Ogre")) != 1)
                { // don't take an ogre -> ignore troll
                    trollLeft = -1;
                }
                if (legion.numCreature(
                        Creature.getCreatureByName("Centaur")) != 1)
                { // don't take a centaur -> ignore lion
                    lionLeft = -1;
                }
                if ((cyclopsLeft >= trollLeft) && (cyclopsLeft >= lionLeft))
                {
                    recruit = Creature.getCreatureByName("Gargoyle");
                }
                else if ((trollLeft >= cyclopsLeft) && (trollLeft >= lionLeft))
                {
                    recruit = Creature.getCreatureByName("Ogre");
                }
                else if ((lionLeft >= trollLeft) && (lionLeft >= cyclopsLeft))
                {
                    recruit = Creature.getCreatureByName("Centaur");
                }
                // else if there's cyclops left and we don't have 2
                // gargoyles, take a gargoyle
            }
            else if ((client.getCreatureCount(
                          Creature.getCreatureByName("Cyclops")) > 6) &&
                     (legion.numCreature(
                          Creature.getCreatureByName("Gargoyle")) < 2))
            {
                recruit = Creature.getCreatureByName("Gargoyle");
                // else if there's trolls left and we don't have 2 ogres,
                // take an ogre
            }
            else if ((client.getCreatureCount(
                          Creature.getCreatureByName("Troll")) > 6) &&
                     (legion.numCreature(
                          Creature.getCreatureByName("Ogre")) < 2))
            {
                recruit = Creature.getCreatureByName("Ogre");
                // else if there's lions left and we don't have 2 lions,
                // take a centaur
            }
            else if ((client.getCreatureCount(
                          Creature.getCreatureByName("Lion")) > 6) &&
                     (legion.numCreature(
                          Creature.getCreatureByName("Centaur")) < 2))
            {
                recruit = Creature.getCreatureByName("Centaur");
                // else we don't really care; take anything
            }
        }

        return recruit;
    }


    public void split()
    {
        PlayerInfo player = client.getPlayerInfo(client.getPlayerName());

        Iterator it = player.getLegionIds().iterator();
        outer: while (it.hasNext())
        {
            String markerId = (String)it.next();
            LegionInfo legion = client.getLegionInfo(markerId);

            if (legion.getHeight() < 7)
            {
                continue;
            }

            // Do not split if we're likely to be forced to attack and lose
            // Do not split if we're likely to want to fight and we need to
            //     be 7 high.
            // Do not split if there's no upwards recruiting or angel
            //     acquiring potential.

            // TODO: Don't split if we're about to be attacked and we
            // need the muscle

            // Only consider this if we're not doing initial game split
            if (legion.getHeight() == 7)
            {
                int forcedToAttack = 0;
                boolean goodRecruit = false;
                legion.sortContents();

                for (int roll = 1; roll <= 6; roll++)
                {
                    Set moves = client.getMovement().listAllMoves(legion, 
                        legion.getCurrentHex(), roll);
                    int safeMoves = 0;
                    Iterator moveIt = moves.iterator();
                    while (moveIt.hasNext())
                    {
                        String hexLabel = (String)moveIt.next();
                        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);

                        if (client.getNumEnemyLegions(hexLabel, 
                            player.getName()) == 0)
                        {
                            safeMoves++;
                            if (!goodRecruit && couldRecruitUp(legion,
                                hexLabel, null, hex.getTerrain()))
                            {
                                goodRecruit = true;
                            }
                        }
                        else
                        {
                            LegionInfo enemy = client.getLegionInfo(
                                client.getFirstEnemyLegion(
                                hexLabel, player.getName()));
                            int result = estimateBattleResults(legion, true,
                                enemy, hex);

                            if (result == WIN_WITH_MINIMAL_LOSSES)
                            {
                                Log.debug(
                                    "We can safely split AND attack with "
                                    + legion);
                                safeMoves++;

                                // Also consider acquiring angel.
                                if (!goodRecruit && couldRecruitUp(legion,
                                    hexLabel, enemy, hex.getTerrain()))
                                {
                                    goodRecruit = true;
                                }
                            }

                            int result2 = estimateBattleResults(legion, false,
                                enemy, hex);

                            if (result2 == WIN_WITH_MINIMAL_LOSSES
                                && result != WIN_WITH_MINIMAL_LOSSES
                                && roll <= 4)
                            {
                                // don't split so that we can attack!
                                Log.debug("Not splitting " + legion +
                                    " because we want the muscle to attack");

                                forcedToAttack = 999;
                                continue outer;
                            }
                        }
                    }

                    if (safeMoves == 0)
                    {
                        forcedToAttack++;
                    }
                    // If we'll be forced to attack on 2 or more rolls,
                    // don't split.
                    if (forcedToAttack >= 2)
                    {
                        continue outer;
                    }
                }
                if (!goodRecruit)
                {
                    // No point in splitting, since we can't improve.
                    Log.debug("Not splitting " + legion +
                        " because it can't improve from here");
                    continue outer;
                }
            }

            String newMarkerId = pickMarker(client.getMarkersAvailable(),
                player.getShortColor());

            StringBuffer results = new StringBuffer();
            List creatures = chooseCreaturesToSplitOut(legion);
            it = creatures.iterator();
            while (it.hasNext())
            {
                Creature creature = (Creature)it.next();
                results.append(",");
                results.append(creature.getName());
            }
            client.doSplit(legion.getMarkerId(), newMarkerId, 
                results.toString());
        }
    }

    /** Return true if the legion could recruit or acquire something
     *  better than its worst creature in hexLabel. */
    private boolean couldRecruitUp(LegionInfo legion, String hexLabel,
        LegionInfo enemy, char terrain)
    {
        legion.sortContents();
        Creature weakest = Creature.getCreatureByName(
            (String)legion.getContents().get(legion.getHeight() - 1));

        // Consider recruiting.
        List recruits = client.findEligibleRecruits(legion.getMarkerId(), 
            hexLabel);
        if (!recruits.isEmpty())
        {
            Creature bestRecruit = (Creature)recruits.get(recruits.size() - 1);
            if (bestRecruit != null && bestRecruit.getPointValue() >
                weakest.getPointValue())
            {
                return true;
            }
        }

        // Consider acquiring angels.
        if (enemy != null)
        {
            int pointValue = enemy.getPointValue();
            boolean wouldFlee = flee(enemy, legion);
            if (wouldFlee)
            {
                pointValue /= 2;
            }

            // should work with all variants
            int currentScore = legion.getPlayerInfo().getScore();
            int arv = TerrainRecruitLoader.getAcquirableRecruitmentsValue();
            int nextScore = ((currentScore / arv) + 1) * arv;

            Creature bestRecruit = null;
            while ((currentScore + pointValue) >= nextScore)
            {
                java.util.List ral = 
                    TerrainRecruitLoader.getRecruitableAcquirableList(terrain, 
                        nextScore);
                java.util.Iterator it = ral.iterator();
                while (it.hasNext())
                {
                    Creature tempRecruit =
                        Creature.getCreatureByName((String)it.next());
                    if ((bestRecruit == null) ||
                        (tempRecruit.getPointValue() >=
                         bestRecruit.getPointValue()))
                    {
                        bestRecruit = tempRecruit;
                    }
                }
                nextScore += arv;
            }

            if (bestRecruit != null && bestRecruit.getPointValue() >
                weakest.getPointValue())
            {
                return true;
            }
        }

        return false;
    }


    /** Decide how to split this legion, and return a list of
     *  Creatures to remove.  */
    static List chooseCreaturesToSplitOut(LegionInfo legion)
    {
        //
        // split a 7 or 8 high legion somehow
        //
        // idea: pick the 2 weakest creatures and kick them
        // out. if there are more than 2 weakest creatures,
        // prefer a pair of matching ones.
        //
        // For an 8-high starting legion, call helper
        // method doInitialGameSplit()
        //
        // TODO: keep 3 cyclops if we don't have a behemoth
        // (split out a gorgon instead)
        //
        // TODO: prefer to split out creatures that have no
        // recruiting value (e.g. if we have 1 angel, 2
        // centaurs, 2 gargoyles, and 2 cyclops, split out the
        // gargoyles)
        //
        if (legion.getHeight() == 8)
        {
            return doInitialGameSplit(legion.getHexLabel());
        }

        Creature weakest1 = null;
        Creature weakest2 = null;

        Iterator critterIt = legion.getContents().iterator();
        while (critterIt.hasNext())
        {
            String name = (String)critterIt.next();
            Creature critter = Creature.getCreatureByName(name);

            // Never split out the titan.
            if (critter.isTitan())
            {
                continue;
            }

            if (weakest1 == null)
            {
                weakest1 = critter;
            }
            else if (weakest2 == null)
            {
                weakest2 = critter;
            }
            else if (critter.getPointValue() < weakest1.getPointValue())
            {
                weakest1 = critter;
            }
            else if (critter.getPointValue() < weakest2.getPointValue())
            {
                weakest2 = critter;
            }
            else if (critter.getPointValue() == weakest1.getPointValue()
                     && critter.getPointValue() == weakest2.getPointValue())
            {
                if (critter.getName().equals(weakest1.getName()))
                {
                    weakest2 = critter;
                }
                else if (critter.getName().equals(weakest2.getName()))
                {
                    weakest1 = critter;
                }
            }
        }

        List creaturesToRemove = new ArrayList();

        creaturesToRemove.add(weakest1);
        creaturesToRemove.add(weakest2);

        return creaturesToRemove;
    }


    // From Hugh Moore:
    //
    // It really depends on how many players there are and how good I
    // think they are.  In a 5 or 6 player game, I will pretty much
    // always put my gargoyles together in my Titan group. I need the
    // extra strength, and I need it right away.  In 3-4 player
    // games, I certainly lean toward putting my gargoyles together.
    // If my opponents are weak, I sometimes split them for a
    // challenge.  If my opponents are strong, but my situation looks
    // good for one reason or another, I may split them.  I never
    // like to split them when I am in tower 3 or 6, for obvious
    // reasons. In two player games, I normally split the gargoyles,
    // but two player games are fucked up.
    //

    /** Return a list of exactly four creatures (including one lord) to
     *  split out. */
    static List doInitialGameSplit(String label)
    {
        Creature[] startCre = TerrainRecruitLoader.getStartingCreatures(
            MasterBoard.getHexByLabel(label).getTerrain());
        // in CMU style splitting, we split centaurs in even towers,
        // ogres in odd towers.
        final boolean oddTower = "100".equals(label) || "300".equals(label)
                || "500".equals(label);
        final Creature splitCreature = oddTower ? startCre[2]
                : startCre[0];
        final Creature nonsplitCreature = oddTower ? startCre[0]
                : startCre[2];

// XXX Hardcoded to default board.
        // don't split gargoyles in tower 3 or 6 (because of the extra jungles)
        if ("300".equals(label) || "600".equals(label))
        {
            return CMUsplit(false, splitCreature, nonsplitCreature, label);
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
            return MITsplit(true, splitCreature, nonsplitCreature, label);
        }
        //
        // otherwise, mix it up for fun
        else
        {
            if (Dice.rollDie() <= 3)
            {
                return MITsplit(true, splitCreature, nonsplitCreature, label);
            }
            else
            {
                return CMUsplit(true, splitCreature, nonsplitCreature, label);
            }
        }
    }


    /** Keep the gargoyles together. */
    private static List CMUsplit(boolean favorTitan, Creature splitCreature,
        Creature nonsplitCreature, String label)
    {
        Creature[] startCre = TerrainRecruitLoader.getStartingCreatures(
            MasterBoard.getHexByLabel(label).getTerrain());
        List splitoffs = new LinkedList();

        if (favorTitan)
        {
            if (Dice.rollDie() <= 3)
            {
                splitoffs.add(Creature.getCreatureByName(Constants.titan));
                splitoffs.add(startCre[1]);
                splitoffs.add(startCre[1]);
                splitoffs.add(splitCreature);
            }
            else
            {
                splitoffs.add(Creature.getCreatureByName(
                    TerrainRecruitLoader.getPrimaryAcquirable()));
                splitoffs.add(nonsplitCreature);
                splitoffs.add(nonsplitCreature);
                splitoffs.add(splitCreature);
            }
        }
        else
        {
            if (Dice.rollDie() <= 3)
            {
                splitoffs.add(Creature.getCreatureByName(Constants.titan));
            }
            else
            {
                splitoffs.add(Creature.getCreatureByName(
                    TerrainRecruitLoader.getPrimaryAcquirable()));
            }

            if (Dice.rollDie() <= 3)
            {
                splitoffs.add(startCre[1]);
                splitoffs.add(startCre[1]);
                splitoffs.add(splitCreature);
            }
            else
            {
                splitoffs.add(nonsplitCreature);
                splitoffs.add(nonsplitCreature);
                splitoffs.add(splitCreature);
            }
        }

        return splitoffs;
    }

    /** Split the gargoyles. */
    private static List MITsplit(boolean favorTitan, Creature splitCreature,
        Creature nonsplitCreature, String label)
    {
        Creature[] startCre = TerrainRecruitLoader.getStartingCreatures(
            MasterBoard.getHexByLabel(label).getTerrain());
        List splitoffs = new LinkedList();

        if (favorTitan)
        {
            if (Dice.rollDie() <= 3)
            {
                splitoffs.add(Creature.getCreatureByName(Constants.titan));
                splitoffs.add(nonsplitCreature);
                splitoffs.add(nonsplitCreature);
                splitoffs.add(startCre[1]);
            }
            else
            {
                splitoffs.add(Creature.getCreatureByName(
                    TerrainRecruitLoader.getPrimaryAcquirable()));
                splitoffs.add(splitCreature);
                splitoffs.add(splitCreature);
                splitoffs.add(startCre[1]);
            }
        }
        else
        {
            if (Dice.rollDie() <= 3)
            {
                splitoffs.add(Creature.getCreatureByName(Constants.titan));
            }
            else
            {
                splitoffs.add(Creature.getCreatureByName(
                    TerrainRecruitLoader.getPrimaryAcquirable()));
            }

            if (Dice.rollDie() <= 3)
            {
                splitoffs.add(nonsplitCreature);
                splitoffs.add(nonsplitCreature);
                splitoffs.add(startCre[1]);
            }
            else
            {
                splitoffs.add(splitCreature);
                splitoffs.add(splitCreature);
                splitoffs.add(startCre[1]);
            }
        }

        return splitoffs;
    }


    /** little helper to store info about possible moves */
    private class MoveInfo
    {
        final LegionInfo legion;
        /** hex to move to.  if hex == null, then this means sit still. */
        final MasterHex hex;
        final int value;
        final int difference;       // difference from sitting still

        MoveInfo(LegionInfo legion, MasterHex hex, int value, int difference)
        {
            this.legion = legion;
            this.hex = hex;
            this.value = value;
            this.difference = difference;
        }
    }

    /** Return true if we need to run this method again after the server
     *  updates the client with the results of a move or mulligan. */
    public boolean masterMove()
    {
        boolean moved = false;

        PlayerInfo player = client.getPlayerInfo(client.getPlayerName());

        // consider mulligans
        if (handleMulligans(player))
        {
            return true;
        }

        /** cache all places enemies can move to, for use in risk analysis. */
        Map[] enemyAttackMap = buildEnemyAttackMap(player);

        // A mapping from Legion to List of MoveInfo objects,
        // listing all moves that we've evaluated.  We use this if
        // we're forced to move.
        Map moveMap = new HashMap();

        moved = handleVoluntaryMoves(player, moveMap, enemyAttackMap);
        if (moved)
        {
            return true;
        }

        // make sure we move splits (when forced)
        moved = handleForcedSplitMoves(player, moveMap);
        if (moved)
        {
            return true;
        }

        // make sure we move at least one legion
        if (player.numLegionsMoved() == 0)
        {
            moved = handleForcedSingleMove(player, moveMap);
            // Always need to retry.
            return true;
        }
        return false;
    }

    /** Return true if AI took a mulligan. */
    boolean handleMulligans(PlayerInfo player)
    {
        // TODO: This is really stupid.  Do something smart here.
        if (client.getTurnNumber() == 1 && player.getMulligansLeft() > 0 &&
            (client.getMovementRoll() == 2 || client.getMovementRoll() == 5)
            && !client.tookMulligan())
        {
            client.mulligan();
            // XXX Need to wait for new movement roll.
            return true;
        }
        return false;
    }

    /** Return true if we moved something. */
    private boolean handleVoluntaryMoves(PlayerInfo player, Map moveMap,
        Map [] enemyAttackMap)
    {
        boolean moved = false;
        List markerIds = player.getLegionIds();

        // Sort markerIds in descending order of legion importance.
        Collections.sort(markerIds,
            new Comparator()
            {
                public int compare(Object o1, Object o2)
                {
                    String s1 = (String)o1;
                    String s2 = (String)o2;
                    LegionInfo li1 = client.getLegionInfo(s1);
                    LegionInfo li2 = client.getLegionInfo(s2);
                    return li1.compareTo(li2);
                }
            }
        );

        Iterator it = markerIds.iterator();
        while (it.hasNext())
        {
            String markerId = (String)it.next();
            LegionInfo legion = client.getLegionInfo(markerId);

            if (legion.hasMoved())
            {
                continue;
            }

            // compute the value of sitting still
            MoveList moveList = new MoveList();
            moveMap.put(legion, moveList);

            MoveInfo sitStillMove = new MoveInfo(legion, null,
                evaluateMove(legion, legion.getCurrentHex(), false,
                enemyAttackMap), 0);
            moveList.add(sitStillMove);

            // find the best move (1-ply search)
            MasterHex bestHex = null;
            int bestValue = Integer.MIN_VALUE;
            Set set = client.getMovement().listAllMoves(legion, 
                legion.getCurrentHex(), client.getMovementRoll());

            Iterator moveIterator = set.iterator();
            while (moveIterator.hasNext())
            {
                final String hexLabel = (String)moveIterator.next();
                final MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
                final int value = evaluateMove(legion, hex, true,
                    enemyAttackMap);

                if (value > bestValue || bestHex == null)
                {
                    bestValue = value;
                    bestHex = hex;
                }
                MoveInfo move = new MoveInfo(legion, hex, value,
                    value - sitStillMove.value);
                moveList.add(move);
            }

            // if we found a move that's better than sitting still, move
            if (bestValue > sitStillMove.value)
            {
                moved = doMove(legion.getMarkerId(), bestHex.getLabel());
                if (moved)
                {
                    return true;
                }
            }
        }
        return false;
    }

    /** Return true if we moved something. */
    private boolean handleForcedSplitMoves(PlayerInfo player, Map moveMap)
    {
        List markerIds = player.getLegionIds();
        Iterator it = markerIds.iterator();
        while (it.hasNext())
        {
            String markerId = (String)it.next();
            LegionInfo legion = client.getLegionInfo(markerId);
            String hexLabel = legion.getHexLabel();
            List friendlyLegions = client.getFriendlyLegions(hexLabel, 
                player.getName());

            while (friendlyLegions.size() > 1 &&
                !client.getMovement().listNormalMoves(legion, 
                    legion.getCurrentHex(), 
                    client.getMovementRoll()).isEmpty())
            {
                // Pick the legion in this hex whose best move has the
                // least difference with its sitStillValue, scaled by
                // the point value of the legion, and force it to move.
                Log.debug("Ack! forced to move a split group");

                // first, concatenate all the moves for all the
                // legions that are here, and sort them by their
                // difference from sitting still multiplied by
                // the value of the legion.
                List allmoves = new ArrayList();
                Iterator friendlyLegionIt = friendlyLegions.iterator();
                while (friendlyLegionIt.hasNext())
                {
                    String friendId = (String)friendlyLegionIt.next();
                    LegionInfo friendlyLegion = client.getLegionInfo(friendId);
                    List moves = (List)moveMap.get(friendlyLegion);
                    allmoves.addAll(moves);
                }

                Collections.sort(allmoves, new Comparator()
                {
                    public int compare(Object o1, Object o2)
                    {
                        MoveInfo m1 = (MoveInfo)o1;
                        MoveInfo m2 = (MoveInfo)o2;
                        return m2.difference * m2.legion.getPointValue() -
                            m1.difference * m1.legion.getPointValue();
                    }
                });

                // now, one at a time, try applying moves until we
                // have handled our split problem.
                Iterator moveIt = allmoves.iterator();
                while (moveIt.hasNext())
                {
                    MoveInfo move = (MoveInfo)moveIt.next();
                    if (move.hex == null)
                    {
                        continue;       // skip the sitStill moves
                    }
                    Log.debug("forced to move split legion " + move.legion + 
                        " to " + move.hex + " taking penalty " + 
                        move.difference + 
                        " in order to handle illegal legion " + legion);

                    boolean moved = doMove(move.legion.getMarkerId(), 
                        move.hex.getLabel());
                    if (moved)
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean handleForcedSingleMove(PlayerInfo player, Map moveMap)
    {
        Log.debug("Ack! forced to move someone");

        // Pick the legion whose best move has the least
        // difference with its sitStillValue, scaled by the
        // point value of the legion, and force it to move.

        // first, concatenate all the moves all legions, and
        // sort them by their difference from sitting still

        List allmoves = new ArrayList();
        List markerIds = player.getLegionIds();
        Iterator friendlyLegionIt = markerIds.iterator();
        while (friendlyLegionIt.hasNext())
        {
            String friendlyMarkerId = (String)friendlyLegionIt.next();
            LegionInfo friendlyLegion = client.getLegionInfo(friendlyMarkerId);
            List moves = (List)moveMap.get(friendlyLegion);
            allmoves.addAll(moves);
        }

        Collections.sort(allmoves, new Comparator()
        {
            public int compare(Object o1, Object o2)
            {
                MoveInfo m1 = (MoveInfo)o1;
                MoveInfo m2 = (MoveInfo)o2;
                return m2.difference * m2.legion.getPointValue() -
                    m1.difference * m1.legion.getPointValue();
            }
        });

        // now, one at a time, try applying moves until we
        // have moved a legion
        Iterator moveIt = allmoves.iterator();
        while (moveIt.hasNext() && player.numLegionsMoved() == 0 && 
            player.numMobileLegions() > 0)
        {
            MoveInfo move = (MoveInfo)moveIt.next();

            if (move.hex == null)
            {
                continue;       // skip the sitStill moves
            }

            Log.debug("forced to move " + move.legion + " to " + move.hex
                    + " taking penalty " + move.difference
                    + " in order to handle illegal legion " + move.legion);

            boolean moved = doMove(move.legion.getMarkerId(), 
                move.hex.getLabel());
            if (moved)
            {
                return true;
            }
        }

        Log.error("handleForcedSingleMove() didn't move anyone");
        // Try again, even though it'll probably loop forever.
        return true;
    }

    private boolean doMove(String markerId, String hexLabel)
    {
        // Don't *know* move succeeded without asking server.
        boolean moved = client.doMove(markerId, hexLabel);
        return moved;
    }

    Map[] buildEnemyAttackMap(PlayerInfo player)
    {
        Map[] enemyMap = new HashMap[7];

        for (int i = 1; i <= 6; i++)
        {
            enemyMap[i] = new HashMap();
        }

        // for each enemy player
        Iterator playerIt = client.getPlayerNames().iterator();
        while (playerIt.hasNext())
        {
            String enemyPlayerName = (String)playerIt.next();
            PlayerInfo enemyPlayer = client.getPlayerInfo(enemyPlayerName);

            if (enemyPlayer == player)
            {
                continue;
            }

            // for each legion that player controls
            Iterator legionIt = client.getLegionsByPlayer(
                enemyPlayerName).iterator();
            while (legionIt.hasNext())
            {
                String markerId = (String)legionIt.next();
                LegionInfo legion = client.getLegionInfo(markerId);

                // for each movement roll he might make
                for (int roll = 1; roll <= 6; roll++)
                {
                    // count the moves he can get to
                    Set set = client.getMovement().listAllMoves(legion, 
                        legion.getCurrentHex(), roll);
                    Iterator moveIt = set.iterator();
                    while (moveIt.hasNext())
                    {
                        String hexlabel = (String)moveIt.next();

                        for (int effectiveRoll = roll; effectiveRoll <= 6;
                            effectiveRoll++)
                        {
                            // legion can attack to hexlabel on a effectiveRoll
                            List list = (List)enemyMap[effectiveRoll].get(
                                hexlabel);

                            if (list == null)
                            {
                                list = new ArrayList();
                            }

                            if (list.contains(legion))
                            {
                                continue;
                            }

                            list.add(legion);
                            enemyMap[effectiveRoll].put(hexlabel, list);
                        }
                    }
                }
            }
        }

        return enemyMap;
    }

    //
    // cheap, inaccurate evaluation function.  Returns a value for
    // moving this legion to this hex.  The value defines a distance
    // metric over the set of all possible moves.
    //
    // TODO: should be parameterized with weights
    //
    int evaluateMove(LegionInfo legion, MasterHex hex, 
        boolean canRecruitHere, Map[] enemyAttackMap)
    {
        // Avoid using MIN_VALUE and MAX_VALUE because of possible overflow.
        final int WIN_GAME = Integer.MAX_VALUE / 2;
        final int LOSE_GAME = Integer.MIN_VALUE / 2;
        final int LOSE_LEGION = -10000;

        int value = 0;
        // consider making an attack
        final String enemyMarkerId = client.getFirstEnemyLegion(hex.getLabel(),
            legion.getPlayerName());

        if (enemyMarkerId != null)
        {
            LegionInfo enemyLegion = client.getLegionInfo(enemyMarkerId);
            final int enemyPointValue = enemyLegion.getPointValue();
            final int result = estimateBattleResults(legion, enemyLegion, hex);

            switch (result)
            {
                case WIN_WITH_MINIMAL_LOSSES:
                    Log.debug("legion " + legion + " can attack " + enemyLegion
                            + " in " + hex + " and WIN_WITH_MINIMAL_LOSSES");

                    // we score a fraction of a basic acquirable
                    value += ((Creature.getCreatureByName(TerrainRecruitLoader.getPrimaryAcquirable())).getPointValue() *
                              enemyPointValue) /
                        TerrainRecruitLoader.getAcquirableRecruitmentsValue();
                    // plus a fraction of a titan strength
// XXX Should be by variant
                    value += (6 * enemyPointValue) /
                        TerrainRecruitLoader.getTitanImprovementValue();
                    // plus some more for killing a group (this is arbitrary)
                    value += (10 * enemyPointValue) / 100;

                    // TODO: if enemy titan, we also score half points
                    // (this may make the AI unfairly gun for your titan)
                    break;

                case WIN_WITH_HEAVY_LOSSES:
                    Log.debug("legion " + legion + " can attack " + enemyLegion
                            + " in " + hex + " and WIN_WITH_HEAVY_LOSSES");
                    // don't do this with our titan unless we can win the game
                    boolean haveOtherSummonables = false;
                    PlayerInfo player = legion.getPlayerInfo();
                    List markerIds = player.getLegionIds();
                    Iterator it = markerIds.iterator();
                    while (it.hasNext())
                    {
                        String markerId = (String)it.next();
                        LegionInfo l = client.getLegionInfo(markerId);

                        if (l == legion)
                        {
                            continue;
                        }

                        if (l.numSummonableCreature() == 0)
                        {
                            continue;
                        }

                        haveOtherSummonables = true;

                        break;
                    }

                    if (legion.hasTitan())
                    {
                        // unless we can win the game with this attack
                        if (enemyLegion.hasTitan() &&
                            client.getNumLivingPlayers() == 2)
                        {
                            // do it and win the game
                            value += enemyPointValue;
                        }
                        else
                        {
                            // ack! we'll fuck up our titan group
                            value += LOSE_LEGION + 10;
                        }
                    }
                    // don't do this if we'll lose our only summonable group
                    // and won't score enough points to make up for it
                    else if (legion.numSummonableCreature() > 0 &&
                             !haveOtherSummonables && 
                             enemyPointValue < ((int)((float)TerrainRecruitLoader.getAcquirableRecruitmentsValue() * .88)))
                    {
                        value += LOSE_LEGION + 5;
                    }
                    else
                    {
                        // we score a fraction of a basic acquirable
                        value += ((Creature.getCreatureByName(TerrainRecruitLoader.getPrimaryAcquirable())).getPointValue() *
                                  enemyPointValue) /
                            TerrainRecruitLoader.getAcquirableRecruitmentsValue();
                        // plus a fraction of a titan strength
                        value += (6 * enemyPointValue) /
                            TerrainRecruitLoader.getTitanImprovementValue();
                        // but we lose this group
                        value -= (20 * legion.getPointValue()) / 100;
                        // TODO: if we have no other angels, more penalty here
                        // TODO: if enemy titan, we also score half points
                        // (this may make the AI unfairly gun for your titan)
                    }
                    break;

                case DRAW:
                    Log.debug("legion " + legion + " can attack " + 
                        enemyLegion + " in " + hex + " and DRAW");

                    // If this is an unimportant group for us, but
                    // is enemy titan, do it.  This might be an
                    // unfair use of information for the AI
                    if (legion.numLords() == 0 && enemyLegion.hasTitan())
                    {
                        // Arbitrary value for killing a player but
                        // scoring no points: it's worth a little
                        // If there are only 2 players, we should do this.
                        if (client.getNumLivingPlayers() == 2)
                        {
                            value = WIN_GAME;
                        }
                        else
                        {
                            value += enemyPointValue / 6;
                        }
                    }
                    else
                    {
                        // otherwise no thanks
                        value += LOSE_LEGION + 2;
                    }
                    break;

                case LOSE_BUT_INFLICT_HEAVY_LOSSES:
                    Log.debug("legion " + legion + " can attack " + enemyLegion
                            + " in " + hex
                            + " and LOSE_BUT_INFLICT_HEAVY_LOSSES");

                    // TODO: how important is it that we damage his group?
                    value += LOSE_LEGION + 1;
                    break;

                case LOSE:
                    Log.debug("legion " + legion + " can attack " + enemyLegion
                            + " in " + hex + " and LOSE");

                    value += LOSE_LEGION;
                    break;

                default:
                    Log.error("Bogus battle result case");
            }
        }

        // consider what we can recruit
        Creature recruit = null;

        if (canRecruitHere)
        {
            recruit = chooseRecruit(legion, hex.getLabel());

            if (recruit != null)
            {
                int oldval = value;

                if (legion.getHeight() < 6)
                {
                    value += recruit.getPointValue();
                }
                else
                {
                    // if we're 6-high, then the value of a recruit is
                    // equal to the improvement in the value of the
                    // pieces that we'll have after splitting.
                    // TODO this should call our splitting code to see
                    // what split decision we would make
                    // If the legion would never split, then ignore
                    // this special case.

                    // This special case was overkill.  A 6-high stack
                    // with 3 lions, or a 6-high stack with 3 clopses,
                    // sometimes refused to go to a safe desert/jungle,
                    // and 6-high stacks refused to recruit colossi,
                    // because the value of the recruit was toned down
                    // too much. So the effect has been reduced.
                    Log.debug("--- 6-HIGH SPECIAL CASE");

                    Creature weakest1 = null;
                    Creature weakest2 = null;

                    Iterator critterIt = legion.getContents().iterator();
                    while (critterIt.hasNext())
                    {
                        String name = (String)critterIt.next();
                        // XXX Titan
                        Creature critter = Creature.getCreatureByName(name);

                        if (weakest1 == null)
                        {
                            weakest1 = critter;
                        }
                        else if (weakest2 == null)
                        {
                            weakest2 = critter;
                        }
                        else if (critter.getPointValue()
                                 < weakest1.getPointValue())
                        {
                            weakest1 = critter;
                        }
                        else if (critter.getPointValue()
                                 < weakest2.getPointValue())
                        {
                            weakest2 = critter;
                        }
                        else if (critter.getPointValue()
                                 == weakest1.getPointValue()
                                 && critter.getPointValue()
                                    == weakest2.getPointValue())
                        {
                            if (critter.getName().equals(weakest1.getName()))
                            {
                                weakest2 = critter;
                            }
                            else if (critter.getName().equals(
                                weakest2.getName()))
                            {
                                weakest1 = critter;
                            }
                        }
                    }

                    int minCreaturePV = Math.min(weakest1.getPointValue(),
                            weakest2.getPointValue());
                    int maxCreaturePV = Math.max(weakest1.getPointValue(),
                            weakest2.getPointValue());
                    // point value of my best 5 pieces right now
                    int oldPV = legion.getPointValue() - minCreaturePV;
                    // point value of my best 5 pieces after adding this
                    // recruit and then splitting off my 2 weakest
                    int newPV = legion.getPointValue()
                        - weakest1.getPointValue()
                        - weakest2.getPointValue()
                        + Math.max(maxCreaturePV, recruit.getPointValue());

                    value += (newPV - oldPV) + recruit.getPointValue();
                }

                Log.debug("--- if " + legion + " moves to " + hex
                        + " then recruit " + recruit.toString() + " (adding "
                        + (value - oldval) + ")");
            }
        }

        // consider what we might be able to recruit next turn, from here
        int nextTurnValue = 0;

        for (int roll = 1; roll <= 6; roll++)
        {
            // XXX Should ignore friends.
            Set moves = client.getMovement().listAllMoves(legion, hex, roll);
            int bestRecruitVal = 0;
            Creature bestRecruit = null;

            Iterator nextMoveIt = moves.iterator();
            while (nextMoveIt.hasNext())
            {
                String nextLabel = (String)nextMoveIt.next();
                MasterHex nextHex = MasterBoard.getHexByLabel(nextLabel);
                // if we have to fight in that hex and we can't
                // WIN_WITH_MINIMAL_LOSSES, then assume we can't
                // recruit there.  IDEA: instead of doing any of this
                // work, perhaps we could recurse here to get the
                // value of being in _that_ hex next turn... and then
                // maximize over choices and average over die rolls.
                // this would be essentially minimax but ignoring the
                // others players ability to move.
                String markerId = client.getFirstEnemyLegion(
                    nextHex.getLabel(), legion.getPlayerName());
                LegionInfo enemy = null;
                if (markerId != null)
                {
                    enemy = client.getLegionInfo(markerId);
                }

                if (enemy != null
                    && estimateBattleResults(legion, enemy, nextHex)
                       != WIN_WITH_MINIMAL_LOSSES)
                {
                    continue;
                }

                List nextRecruits = client.findEligibleRecruits(
                    legion.getMarkerId(), nextLabel);

                if (nextRecruits.size() == 0)
                {
                    continue;
                }

                Creature nextRecruit =
                    (Creature)nextRecruits.get(nextRecruits.size() - 1);
                // Reduced val by 5 to make current turn recruits more
                // valuable than next turn's recruits
                int val = nextRecruit.getSkill() * nextRecruit.getPower()
                          - 5;

                if (val > bestRecruitVal)
                {
                    bestRecruitVal = val;
                    bestRecruit = nextRecruit;
                }
            }

            nextTurnValue += bestRecruitVal;
        }

        nextTurnValue /= 6;     // 1/6 chance of each happening
        value += nextTurnValue;

        // consider risk of being attacked
        if (enemyAttackMap != null)
        {
            if (canRecruitHere)
            {
                Log.debug("considering risk of moving " + legion + " to " + 
                    hex);
            }
            else
            {
                Log.debug("considering risk of leaving " + legion + " in " +
                    hex);
            }

            Map[] enemiesThatCanAttackOnA = enemyAttackMap;
            int roll;

            for (roll = 1; roll <= 6; roll++)
            {
                List enemies =
                    (List)enemiesThatCanAttackOnA[roll].get(hex.getLabel());

                if (enemies == null)
                {
                    continue;
                }

                Log.debug("got enemies that can attack on a " + roll + " :" + 
                    enemies);

                Iterator it = enemies.iterator();
                while (it.hasNext())
                {
                    LegionInfo enemy = (LegionInfo)it.next();
                    final int result = estimateBattleResults(enemy, false,
                        legion, hex, recruit);

                    if (result == WIN_WITH_MINIMAL_LOSSES ||
                        result == DRAW && legion.hasTitan())
                    {
                        break;
                        // break on the lowest roll from which we can
                        // be attacked and killed
                    }
                }
            }

            // Ignore all fear of attack on turn 1.  Not perfect,
            // but a pretty good rule of thumb.
            if (roll < 7 && client.getTurnNumber() > 1)
            {
                final double chanceToAttack = (7.0 - roll) / 6.0;
                final double risk;

                if (legion.hasTitan())
                {
                    risk = LOSE_LEGION * chanceToAttack;
                }
                else
                {
                    risk = -legion.getPointValue() / 2 * chanceToAttack;
                }

                value += risk;
            }
        }

        // TODO: consider mobility.  e.g., penalty for suckdown
        // squares, bonus if next to tower or under the top
        // TODO: consider what we can attack next turn from here
        // TODO: consider nearness to our other legions
        // TODO: consider being a scooby snack (if so, everything
        // changes: we want to be in a location with bad mobility, we
        // want to be at risk of getting killed, etc)
        // TODO: consider risk of being scooby snacked (this might be inherent)
        // TODO: consider splitting up our good recruitment rolls
        // (i.e. if another legion has warbears under the top that
        // recruit on 1,3,5, and we have a behemoth with choice of 3/5
        // to jungle or 4/6 to jungle, prefer the 4/6 location).
        Log.debug("EVAL " + legion
                + (canRecruitHere ? " move to " : " stay in ") + hex + " = "
                + value);

        return value;
    }

    static final int WIN_WITH_MINIMAL_LOSSES = 0;
    static final int WIN_WITH_HEAVY_LOSSES = 1;
    static final int DRAW = 2;
    static final int LOSE_BUT_INFLICT_HEAVY_LOSSES = 3;
    static final int LOSE = 4;

    /* can be overloaded by subclass -> not final */

    static double RATIO_WIN_MINIMAL_LOSS = 1.30;
    static double RATIO_WIN_HEAVY_LOSS = 1.15;
    static double RATIO_DRAW = 0.85;
    static double RATIO_LOSE_HEAVY_LOSS = 0.70;

    private static int estimateBattleResults(LegionInfo attacker, 
        LegionInfo defender, MasterHex hex)
    {
        return estimateBattleResults(attacker, false, defender, hex, null);
    }

    private static int estimateBattleResults(LegionInfo attacker,
        boolean attackerSplitsBeforeBattle, LegionInfo defender, MasterHex hex)
    {
        return estimateBattleResults(attacker, attackerSplitsBeforeBattle,
            defender, hex, null);
    }

    private static int estimateBattleResults(LegionInfo attacker,
        boolean attackerSplitsBeforeBattle, LegionInfo defender,
        MasterHex hex, Creature recruit)
    {
        char terrain = hex.getTerrain();
        double attackerPointValue = getCombatValue(attacker, terrain);

        if (attackerSplitsBeforeBattle)
        {
            // remove PV of the split
            List creaturesToRemove = chooseCreaturesToSplitOut(attacker);
            Iterator creatureIt = creaturesToRemove.iterator();
            while (creatureIt.hasNext())
            {
                Creature creature = (Creature)creatureIt.next();
                attackerPointValue -= getCombatValue(creature, terrain);
            }
        }

        if (recruit != null)
        {
            // Log.debug("adding in recruited " + recruit +
            // " when evaluating battle");
            attackerPointValue += getCombatValue(recruit, terrain);
        }
        // TODO: add angel call

        double defenderPointValue = getCombatValue(defender, terrain);
        // TODO: add in enemy's most likely turn 4 recruit

        if (HexMap.terrainIsTower(hex.getTerrain()))
        {
            // defender in the tower!  ouch!
            defenderPointValue *= 1.2;
        }

        // really dumb estimator
        double ratio = (double)attackerPointValue / (double)defenderPointValue;

        if (ratio >= RATIO_WIN_MINIMAL_LOSS)
        {
            return WIN_WITH_MINIMAL_LOSSES;
        }
        else if (ratio >= RATIO_WIN_HEAVY_LOSS)
        {
            return WIN_WITH_HEAVY_LOSSES;
        }
        else if (ratio >= RATIO_DRAW)
        {
            return DRAW;
        }
        else if (ratio >= RATIO_LOSE_HEAVY_LOSS)
        {
            return LOSE_BUT_INFLICT_HEAVY_LOSSES;
        }
        else    // ratio less than 0.70
        {
            return LOSE;
        }
    }


    private int getNumberOfWaysToTerrain(LegionInfo legion, 
        MasterHex hex, char terrainType)
    {
        int total = 0;
        for (int roll = 1; roll <= 6; roll++)
        {
            Set set = client.getMovement().listAllMoves(legion, hex, roll);
            if (setContainsHexWithTerrain(set, terrainType))
            {
                total++;
            }
        }
        return total;
    }

    private static boolean setContainsHexWithTerrain(Set set, char terrainType)
    {
        Iterator it = set.iterator();
        while (it.hasNext())
        {
            String hexLabel = (String)it.next();
            char terrain = MasterBoard.getHexByLabel(hexLabel).getTerrain();
            if (terrain == terrainType)
            {
                return true;
            }
        }
        return false;
    }


    // This is a really dumb placeholder.  TODO Make it smarter.
    // In particular, the AI should pick a side that will let it enter
    // as many creatures as possible.
    public String pickEntrySide(String hexLabel, String markerId, 
        Set entrySides)
    {
        // Default to bottom to simplify towers.
        if (entrySides.contains(Constants.bottom))
        {
            return Constants.bottom;
        }
        if (entrySides.contains(Constants.right))
        {
            return Constants.right;
        }
        if (entrySides.contains(Constants.left))
        {
            return Constants.left;
        }
        return null;
    }

    public String pickEngagement()
    {
        Set hexLabels = client.findEngagements();

        // Bail out early if we have no real choice.
        int numChoices = hexLabels.size();
        if (numChoices == 0)
        {
            return null;
        }
        if (numChoices == 1)
        {
            return (String)hexLabels.iterator().next();
        }

        String bestChoice = null;
        int bestScore = Integer.MIN_VALUE;

        Iterator it = hexLabels.iterator();
        while (it.hasNext())
        {
            String hexLabel = (String)it.next();
            int score = evaluateEngagement(hexLabel);
            if (score > bestScore)
            {
                bestScore = score;
                bestChoice = hexLabel;
            }
        }
        return bestChoice;
    }

    private int evaluateEngagement(String hexLabel)
    {
        // Fight losing battles last, so that we don't give away
        //    points while they may be used against us this turn.
        // Fight battles with angels first, so that those angels
        //    can be summoned out.
        // Try not to lose potential angels and recruits by having
        //    scooby snacks flee to 7-high stacks (or 6-high stacks
        //    that could recruit later this turn) and push them
        //    over 100-point boundaries.

        String playerName = client.getActivePlayerName();
        LegionInfo attacker = client.getLegionInfo(
            client.getFirstFriendlyLegion(hexLabel, playerName));
        LegionInfo defender = client.getLegionInfo(
            client.getFirstEnemyLegion(hexLabel, playerName));
        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
        int value = 0;

        final int result = estimateBattleResults(attacker, defender, hex);

        // The worse we expect to do, the more we want to put off this
        // engagement, either to avoid strengthening an enemy titan that
        // we may fight later this turn, or to increase our chances of
        // being able to call an angel.
        value -= result;

        // Avoid losing angels and recruits.
        boolean wouldFlee = flee(defender, attacker);
        if (wouldFlee)
        {
            int currentScore = client.getPlayerInfo(playerName).getScore();
            int fleeValue = defender.getPointValue() / 2;
            if (((currentScore + fleeValue) / TerrainRecruitLoader.getAcquirableRecruitmentsValue()) > (currentScore / TerrainRecruitLoader.getAcquirableRecruitmentsValue()))
            {
                if (attacker.getHeight() == 7 || attacker.getHeight() == 6 &&
                    attacker.canRecruit())
                {
                    value -= 10;
                }
                else
                {
                    // Angels go best in Titan legions.
                    if (attacker.hasTitan())
                    {
                        value += 6;
                    }
                    else
                    {
                        // A bird in the hand...
                        value += 2;
                    }
                }
            }
        }

        // Fight early with angel legions, so that others can summon.
        if (result <= WIN_WITH_HEAVY_LOSSES && attacker.hasSummonable())
        {
            value += 5;
        }

        return value;
    }


    public boolean flee(LegionInfo legion, LegionInfo enemy)
    {
        // TODO Make this smarter.
        char terrain = legion.getCurrentHex().getTerrain();
        if (getCombatValue(legion, terrain) < 0.7 * getCombatValue(enemy,
            terrain))
        {
            return true;
        }
        return false;
    }


    public boolean concede(LegionInfo legion, LegionInfo enemy)
    {
        // Never concede titan legion.
        if (legion.hasTitan())
        {
            return false;
        }

        // Wimpy legions should concede if it costs the enemy an
        // angel or good recruit.
        char terrain = legion.getCurrentHex().getTerrain();
        int height = enemy.getHeight();
        if (getCombatValue(legion, terrain) < 0.5 * getCombatValue(enemy,
            terrain) && height >= 6)
        {
            int currentScore = enemy.getPlayerInfo().getScore();
            int pointValue = legion.getPointValue();
            boolean canAcquireAngel =
                ((currentScore + pointValue) /
                 TerrainRecruitLoader.getAcquirableRecruitmentsValue() >
                 (currentScore /
                  TerrainRecruitLoader.getAcquirableRecruitmentsValue()));
            // Can't use Legion.getRecruit() because it checks for
            // 7-high legions.
            boolean canRecruit = !client.findEligibleRecruits(
                enemy.getMarkerId(), enemy.getHexLabel()).isEmpty();
            if (height == 7 && (canAcquireAngel || canRecruit))
            {
                return true;
            }
            if (canAcquireAngel && canRecruit) // know height == 6
            {
                return true;
            }
        }
        return false;
    }


    // should be correct for most variants.
    public String acquireAngel(String markerId, List recruits)
    {
        // TODO If the legion is 6 high and can recruit something better,
        // or if the legion is a tiny scooby snack that's about to get
        // smooshed, turn down the angel.

        java.util.List al = TerrainRecruitLoader.getAcquirableList();
        java.util.Iterator it = al.iterator();
        Creature c = null;

        // heuristic : pick-up the most valuable of all available.
        while (it.hasNext())
        {
            String name = (String)it.next();
            Creature tc = Creature.getCreatureByName(name);
            if (recruits.contains(name) &&
                ((c == null) || (tc.getPointValue() > c.getPointValue())))
                c = tc;
        }

        return (c == null ? null : c.getName());
    }


    /** Return a string of form angeltype:donorId, or null. */
    public String summonAngel(String summonerId)
    {
        // Always summon the biggest possible angel, from the least
        // important legion that has one.

        Set hexLabels = client.findSummonableAngelHexes(summonerId);

        LegionInfo bestLegion = null;
        String bestAngel = null;

        Iterator it = hexLabels.iterator();
        while (it.hasNext())
        {
            String hexLabel = (String)it.next();
            List legions = client.getLegionsByHex(hexLabel);
            if (legions.size() != 1)
            {
                Log.error("SimpleAI.summonAngel(): Engagement in " + hexLabel);
                continue;
            }
            String markerId = (String)legions.get(0);
            LegionInfo info = client.getLegionInfo(markerId);
            String myAngel = info.bestSummonable();
            if (myAngel == null)
            {
                Log.error("SimpleAI.summonAngel(): No angel in " + markerId);
                continue;
            }

            if (bestAngel == null || bestLegion == null ||
                Creature.getCreatureByName(myAngel).getPointValue() >
                    Creature.getCreatureByName(bestAngel).getPointValue() ||
                info.compareTo(bestLegion) > 0 &&
                    (Creature.getCreatureByName(myAngel).getPointValue() ==
                        Creature.getCreatureByName(bestAngel).getPointValue()))
            {
                bestLegion = info;
                bestAngel = myAngel;
            }
        }
        return bestAngel + ":" + bestLegion.getMarkerId();
    }


    /** Create a map containing each target and the number of hits it would 
     *  likely take if all possible creatures attacked it. */
    private Map generateDamageMap()
    {
        Map map = new HashMap();
        Iterator it = client.getActiveBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit critter = (BattleChit)it.next();
            // Offboard critters can't strike.
            if (critter.getCurrentHexLabel().startsWith("X"))
            {
                continue;
            }
            Set set = client.findStrikes(critter.getTag());
            Iterator it2 = set.iterator();
            while (it2.hasNext())
            {
                String hexLabel = (String)it2.next();
                BattleChit target = client.getBattleChit(hexLabel);
                int dice = client.getStrike().getDice(critter, target);
                int strikeNumber = client.getStrike().getStrikeNumber(critter,
                    target);
                double h = Probs.meanHits(dice, strikeNumber);

                if (map.containsKey(target))
                {
                    double d = ((Double)map.get(target)).doubleValue();
                    h += d;
                }

                map.put(target, new Double(h));
            }
        }
        return map;
    }

    private BattleChit findBestTarget()
    {
        boolean canKillSomething = false;
        BattleChit bestTarget = null;
        char terrain = client.getBattleTerrain();

        // Create a map containing each target and the likely number
        // of hits it would take if all possible creatures attacked it.
        Map map = generateDamageMap();

        Iterator it = map.keySet().iterator();
        while (it.hasNext())
        {
            BattleChit target = (BattleChit)it.next();
            double h = ((Double)map.get(target)).doubleValue();

            if (h + target.getHits() >= target.getPower())
            {
                // We can probably kill this target.
                if (bestTarget == null || !canKillSomething || 
                    getKillValue(target, terrain)
                        > getKillValue(bestTarget, terrain))
                {
                    bestTarget = target;
                    canKillSomething = true;
                }
            }
            else
            {
                // We probably can't kill this target.
                if (bestTarget == null || (!canKillSomething && 
                    getKillValue(target, terrain)
                        > getKillValue(bestTarget, terrain)))
                {
                    bestTarget = target;
                }
            }
        }
        return bestTarget;
    }

    private BattleChit findBestAttacker(BattleChit target)
    {
        BattleChit bestAttacker = null;

        Iterator it = client.getActiveBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit critter = (BattleChit)it.next();
            if (client.getStrike().canStrike(critter, target))
            {
                return critter;
            }
        }
        return null;
    }

    /** Apply carries first to the biggest creature that could be killed 
     *  with them, then to the biggest creature.  carryTargets are
     *  hexLabel description strings. */
    public void handleCarries(int carryDamage, Set carryTargets)
    {
        char terrain = client.getBattleTerrain();
        BattleChit bestTarget = null;

        Iterator it = carryTargets.iterator();
        while (it.hasNext())
        {
            String desc = (String)it.next();
            String targetHexLabel = desc.substring(desc.length() - 2);
            BattleChit target = client.getBattleChit(targetHexLabel);

            if (target.wouldDieFrom(carryDamage))
            {
                if (bestTarget == null ||
                    !bestTarget.wouldDieFrom(carryDamage) ||
                    getKillValue(target, terrain)
                       > getKillValue(bestTarget, terrain))
                {
                    bestTarget = target;
                }
            }
            else
            {
                if (bestTarget == null ||
                    (!bestTarget.wouldDieFrom(carryDamage) &&
                    getKillValue(target, terrain) >
                    getKillValue(bestTarget, terrain)))
                {
                    bestTarget = target;
                }
            }
        }
        if (bestTarget == null)
        {
            Log.warn("No carry target but " + carryDamage +
                " points of available carry damage");
            client.leaveCarryMode();
        }
        else
        {
            Log.debug("Best carry target is " + bestTarget.getDescription());
            client.applyCarries(bestTarget.getCurrentHexLabel());
        }
    }

    /** Simple one-ply group strike algorithm.  Return false if there were
     *  no strike targets. */
    public synchronized boolean strike(LegionInfo legion)
    {
Log.debug("Called ai.strike() for " + legion.getMarkerId());
        // PRE: Caller handles forced strikes before calling this.

        // Pick the most important target that can likely be killed this
        // turn.  If none can, pick the most important target.
        // TODO If none can, and we're going to lose the battle this turn, 
        // pick the easiest target to kill.

        BattleChit bestTarget = findBestTarget();
        if (bestTarget == null)
        {
Log.debug("Best target is null, aborting");
            return false;
        }
        Log.debug("Best target is " + bestTarget.getDescription());

        // Having found the target, pick an attacker.  The
        // first priority is finding one that does not need
        // to worry about carry penalties to hit this target.
        // The second priority is using the weakest attacker,
        // so that more information is available when the
        // stronger attackers strike.

        BattleChit bestAttacker = findBestAttacker(bestTarget);
        if (bestAttacker == null)
        {
            return false;
        }

        Log.debug("Best attacker is " + bestAttacker.getDescription());

        // Having found the target and attacker, strike.
        // Take a carry penalty if there is still a 95%
        // chance of killing this target.
        client.strike(bestAttacker.getTag(), bestTarget.getCurrentHexLabel());
        return true;
    }


    static int getCombatValue(BattleChit chit, char terrain)
    {
        int val = chit.getPointValue();
        Creature creature = chit.getCreature();

        if (creature.isFlier())
        {
            val++;
        }

        if (creature.isRangestriker())
        {
            val++;
        }

        if (MasterHex.isNativeCombatBonus(creature, terrain))
        {
            val++;
        }

        return val;
    }

    /** XXX Inaccurate for titans. */
    static int getCombatValue(Creature creature, char terrain) 
    {
        if (creature.isTitan())
        {
            // Don't know the power, so just estimate.
            return 24;
        }

        int val = creature.getPointValue();

        if (creature.isFlier())
        {
            val++;
        }

        if (creature.isRangestriker())
        {
            val++;
        }

        if (MasterHex.isNativeCombatBonus(creature, terrain))
        {
            val++;
        }

        return val;
    }

    static int getTitanCombatValue(int power)
    {
        int val = power * 4;
        if (power < 9)
        {
            val -= (6 + 2 * (9 - power));
        }
        return val;
    }


    static int getCombatValue(LegionInfo legion, char terrain)
    {
        int val = 0;
        Iterator it = legion.getContents().iterator();
        while (it.hasNext())
        {
            String name = (String)it.next();
            if (name.startsWith(Constants.titan))
            {
                val += getTitanCombatValue(
                    legion.getPlayerInfo().getTitanPower());
            }
            else
            {
                Creature creature = Creature.getCreatureByName(name);
                val += getCombatValue(creature, terrain);
            }
        }

        return val;
    }



    static int getKillValue(Creature creature)
    {
        return getKillValue(creature, 'P');
    }

    // XXX titan power
    static int getKillValue(BattleChit chit, char terrain)
    {
        return getKillValue(chit.getCreature(), terrain);
    }

    static int getKillValue(Creature creature, char terrain)
    {
        int val = 10 * creature.getPointValue();
        if (creature.getSkill() == 4)
        {
            val += 2;
        }
        if (creature.getSkill() == 2)
        {
            val += 1;
        }
        if (creature.isFlier())
        {
            val += 4;
        }
        if (creature.isRangestriker())
        {
            val += 5;
        }
        if (creature.useMagicMissile())
        {
            val += 4;
        }
        if (MasterHex.isNativeCombatBonus(creature, terrain))
        {
            val += 3;
        }
        if (creature.isTitan())
        {
            val += 1000;
        }
        return val;
    }



    ////////////////////////////////////////////////////////////////
    // Battle move stuff
    ////////////////////////////////////////////////////////////////

    /*
     Battles are 2-player games within the multiplayer titan game.
     They must be evaluated within that context.  So not all
     winning positions are equally good, and not all losing
     positions are equally bad, since the surviving contents of
     the winning stack matter. All results that kill the last
     enemy titan while leaving ours alive are equally good, though,
     and all results that get our titan killed are equally bad.

     We can greatly simplify analysis by assuming that every strike
     will score the average number of hits.  This may or may not
     be good enough.  In particular, exposing a titan in a situation
     where a slightly above average number of hits will kill it is
     probably unwise, so we need to hack in some extra caution for
     titans.

     There are 27 hexes on each battle map.  A fast creature starting
     near the middle of the map can move to all of them, terrain and
     other creatures permitting.  So the possible number of different
     positions after one move is huge.  So we can't really iterate over
     all possible moves.  We need to consider one creature at a time.
     But we also need to use team tactics.

     When finding all possible moves, we need to take into account
     that friendly creatures can block one another's moves.  That
     gets really complex, so instead assume that they don't when
     computing all possible moves, and then try to ensure that
     less important creatures get out of the way of more important
     ones.
     */


    public void battleMove()
    {
        Log.debug("Called battleMove()");

        // Defer setting time limit until here where it's needed, to
        // avoid initialization timing issues.
        timeLimit = client.getIntOption(Options.aiTimeLimit);

        // Consider one critter at a time, in order of importance.
        // Examine all possible moves for that critter not already
        // taken by a more important one.

        // TODO Handle summoned/recruited critters, in particular
        // getting stuff out of the way so that a reinforcement
        // has room to enter.

        List legionMoves = findBattleMoves();
        LegionMove bestLegionMove = findBestLegionMove(legionMoves);
        List bestMoveOrder = findMoveOrder(bestLegionMove);

        // Now that critters are sorted into the order in which they
        // should move, start moving them for real.  
        // XXX If the preferred move fails, fall back to the critter's 
        // remaining moves.

        if (bestMoveOrder != null)
        {
            Iterator it = bestMoveOrder.iterator();
            while (it.hasNext())
            {
                CritterMove cm = (CritterMove)it.next();
                BattleChit fakeCritter = cm.getCritter();

                String hexLabel = cm.getEndingHexLabel();
                Log.debug("try " + fakeCritter + " to " + hexLabel);
                client.doBattleMove(fakeCritter.getTag(), hexLabel);
                // XXX Need to test that the move was okay, and
                // try another one if it failed.
            }
        }

        Log.debug("Done with battleMove()");
    }


    private static final int MAX_MOVE_ORDER_PERMUTATIONS = 10000;

    List findMoveOrder(LegionMove lm)
    {
        if (lm == null)
        {
            return null;
        }

        int perfectScore = 0;

        ArrayList critterMoves = new ArrayList();
        critterMoves.addAll(lm.getCritterMoves());

        Iterator it = critterMoves.iterator();
        while (it.hasNext())
        {
            CritterMove cm = (CritterMove)it.next();
            if (cm.getStartingHexLabel().equals(cm.getEndingHexLabel()))
            {
                // Prune non-movers
                it.remove();
            }
            else
            {
                perfectScore += cm.getCritter().getPointValue();
            }
        }

        if (perfectScore == 0)
        {
            // No moves, so exit.
            return null;
        }

        // Figure the order in which creatures should move to get in
        // each other's way as little as possible.
        // Iterate through all permutations of critter move orders,
        // tracking how many critters get their preferred hex with each
        // order, until we find an order that lets every creature reach
        // its preferred hex.  If none does, take the best we can find.

        int turn = client.getBattleTurnNumber();
        int bestScore = 0;
        List bestOrder = null;
        List lastOrder = null;
        int count = 0;

        setupTimer();

        it = new Perms(critterMoves).iterator();
        while (it.hasNext())
        {
            ArrayList order = (ArrayList)it.next();

            // On turn 1, all moving critters of the same creature type are
            // identical (since all come from the same entrance and none
            // are wounded), so we should be able to skip testing any
            // permutations that are effectively identical.

            // TODO Generate and save all permutations, then sort, so that
            // we can eliminate all duplicates, not just consecutive ones.

            if (turn == 1 && creatureNames(order).equals(creatureNames(
                lastOrder)))
            {
                continue;
            }

            count++;

            int score = testMoveOrder(order);
            if (score > bestScore)
            {
                bestOrder = (ArrayList)order.clone();
                bestScore = score;
                if (score >= perfectScore)
                {
                    break;
                }
            }
            lastOrder = (List)order.clone();

            // Bail out early
            if (timeIsUp)
            {
                Log.debug("Time is up figuring move order");
                break;
            }
        }
        Log.debug("Got score " + bestScore + " in " + count + " permutations");
        return bestOrder;
    }


    /** Try each of the moves in order.  Return the number that succeed,
     *  scaled by the importance of each critter. */
    private int testMoveOrder(List order)
    {
        int val = 0;
        Iterator it = order.iterator();
        while (it.hasNext())
        {
            CritterMove cm = (CritterMove)it.next();
            BattleChit critter = cm.getCritter();
            String hexLabel = cm.getEndingHexLabel();
            if (client.testBattleMove(critter, hexLabel))
            {
                // XXX Use kill value instead?
                val += critter.getPointValue();
            }
        }
        // Move them all back where they started.
        it = order.iterator();
        while (it.hasNext())
        {
            CritterMove cm = (CritterMove)it.next();
            BattleChit critter = cm.getCritter();
            String hexLabel = cm.getStartingHexLabel();
            critter.setHexLabel(hexLabel);
        }
        return val;
    }



    private final int MAX_LEGION_MOVES = 10000;

    /** Find the maximum number of moves per creature to test, such that
     *  numMobileCreaturesInLegion ^ N <= LEGION_MOVE_LIMIT, but we must
     *  have at least as many moves as mobile creatures to ensure that
     *  every creature has somewhere to go. */
    int getCreatureMoveLimit()
    {
        int mobileCritters = client.findMobileBattleChits().size();
        if (mobileCritters <= 1)
        {
            // Avoid infinite logs and division by zero, and just try
            // all possible moves.
            return Constants.BIGNUM;
        }
        int max = (int)Math.floor(Math.log(MAX_LEGION_MOVES) / 
            Math.log(mobileCritters));
        return (Math.min(max, mobileCritters));
    }

    List findBattleMoves()
    {
Log.debug("Called findBattleMoves()");

        // Consider one critter at a time in isolation.  
        // Find the best N moves for each critter.
        
        // TODO Do not consider immobile critters.  Also, do not allow
        // non-flying creatures to move through their hexes.

        // TODO Handle summoned/recruited critters, in particular
        // getting stuff out of the way so that a reinforcement
        // has room to enter.

        // The caller is responsible for actually making the moves.


        // allCritterMoves is an ArrayList (for clone()) of moveLists.
        final ArrayList allCritterMoves = new ArrayList();

        Iterator it = client.getActiveBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit critter = (BattleChit)it.next();
            String currentHexLabel = critter.getCurrentHexLabel();

            // moves is a list of hex labels where one critter can move.

            // Sometimes friendly critters need to get out of the way to 
            // clear a path for a more important critter.  We consider 
            // moves that the critter could make, disregarding mobile allies.

            // XXX Should show moves including moving through mobile allies.
            Set moves = client.showBattleMoves(critter.getTag());

            // TODO Make less important creatures get out of the way.

            // Not moving is also an option.
            moves.add(currentHexLabel);

            List moveList = new MoveList();

            Iterator it2 = moves.iterator();
            while (it2.hasNext())
            {
                String hexLabel = (String)it2.next();

                CritterMove cm = new CritterMove(critter, currentHexLabel, 
                    hexLabel);

                // Need to move the critter to evaluate.
                critter.moveToHex(hexLabel);

                // Compute and save the value for each CritterMove.
                cm.setValue(evaluateCritterMove(critter));
                moveList.add(cm);
            }
            // Move the critter back where it started.
            critter.moveToHex(critter.getStartingHexLabel());

            // Sort critter moves in descending order of score.
            Collections.sort(moveList, new Comparator()
            {
                public int compare(Object o1, Object o2)
                {
                    CritterMove cm1 = (CritterMove)o1;
                    CritterMove cm2 = (CritterMove)o2;
                    return cm2.getValue() - cm1.getValue();
                }
            });

            // Show the moves considered.
            StringBuffer buf = new StringBuffer("Considered " +
                moveList.size() + " moves for " + critter.getTag() + " " +
                critter.getCreatureName() + " in " + currentHexLabel+ ":");
            it2 = moveList.iterator();
            while (it2.hasNext())
            {
                CritterMove cm = (CritterMove)it2.next();
                buf.append(" " + cm.getEndingHexLabel());
            }
            Log.debug(buf.toString());

            // Add this critter's moves to the list.
            allCritterMoves.add(moveList);

            // Put all critters back where they started.
            it2 = allCritterMoves.iterator();
            while (it2.hasNext())
            {
                moveList = (MoveList)it2.next();
                CritterMove cm = (CritterMove)moveList.get(0);
                BattleChit critter2 = cm.getCritter();
                critter2.moveToHex(cm.getStartingHexLabel());
            }
        }

        List legionMoves = findLegionMoves(allCritterMoves);
        return legionMoves;
    }


    private void setupTimer()
    {
        // java.util.Timer, not Swing Timer
        Timer timer = new Timer();
        timeIsUp = false;
        final int MS_PER_S = 1000;
        if (timeLimit < Constants.MIN_AI_TIME_LIMIT || 
            timeLimit > Constants.MAX_AI_TIME_LIMIT)
        {
            timeLimit = Constants.DEFAULT_AI_TIME_LIMIT;
        }
        timer.schedule(new TriggerTimeIsUp(), MS_PER_S * timeLimit);
    }


    /** Evaluate all legion moves in the list, and return the best one.
     *  Break out early if the time limit is exceeded. */
    LegionMove findBestLegionMove(List legionMoves)
    {
        int bestScore = Integer.MIN_VALUE;
        LegionMove best = null;

        setupTimer();

        int count = 0;
        Iterator it = legionMoves.iterator();
        while (it.hasNext())
        {
            LegionMove lm = (LegionMove)it.next();
            int score = evaluateLegionBattleMove(lm);
            if (score > bestScore)
            {
                bestScore = score;
                best = lm;
            }

            count++;
            if (timeIsUp)
            {
                Log.debug("findBestLegionMove() time up after " + count + 
                    " iterations");
                break;
            }
        }
        Log.debug("Best legion move: " + ((best == null) ? "none " : 
            best.toString()));
        return best;
    }


    /** allCritterMoves is a List of sorted MoveLists.  A MoveList is a 
     *  sorted List of CritterMoves for one critter.  Return a sorted List 
     *  of LegionMoves.  A LegionMove is a List of one CritterMove per
     *  mobile critter in the legion, where no two critters move to the
     *  same hex. */
    List findLegionMoves(final ArrayList allCritterMoves)
    {
        ArrayList critterMoves = (ArrayList)allCritterMoves.clone();
        while (trimCritterMoves(critterMoves)) {}  // Just trimming

        // Now that the list is as small as possible, start finding combos.
        List legionMoves = new ArrayList();
        final int limit = getCreatureMoveLimit();
        int [] indexes = new int[critterMoves.size()];

        nestForLoop(indexes, indexes.length - 1, critterMoves, legionMoves);

        Log.debug("Got " + legionMoves.size() + " legion moves");
        return legionMoves;
    }


    private Set duplicateHexChecker = new HashSet();

    private void nestForLoop(int [] indexes, final int level, final
        List critterMoves, List legionMoves)
    {
        // TODO See if doing the set test at every level is faster than
        // always going down to level 0 then checking.
        if (level == 0)
        {
            duplicateHexChecker.clear();
            boolean offboard = false;
            for (int j = 0; j < indexes.length; j++)
            {
                MoveList moveList = (MoveList)critterMoves.get(j);
                if (indexes[j] >= moveList.size())
                {
                    return;
                }
                CritterMove cm = (CritterMove)moveList.get(indexes[j]);
                String endingHexLabel = cm.getEndingHexLabel();
                if (endingHexLabel.startsWith("X"))
                {
                    offboard = true;
                }
                else if (duplicateHexChecker.contains(endingHexLabel))
                {
                    // Need to allow duplicate offboard moves, in case 2 or
                    // more creatures cannot enter.
                    return;
                }
                duplicateHexChecker.add(cm.getEndingHexLabel());
            }

            LegionMove lm  = makeLegionMove(indexes, critterMoves);
            // Put offboard moves last, so they'll be skipped if the AI
            // runs out of time.
            if (offboard)
            {
                legionMoves.add(lm);
            }
            else
            {
                legionMoves.add(0, lm);
            }
        }
        else
        {
            for (int i = 0; i < indexes.length; i++)
            {
                indexes[level] = i;
                nestForLoop(indexes, level - 1, critterMoves, legionMoves);
            }
        }
    }

    private LegionMove makeLegionMove(int [] indexes, List critterMoves) 
    {
        LegionMove lm = new LegionMove();
        for (int i = 0; i < indexes.length; i++)
        {
            MoveList moveList = (MoveList)critterMoves.get(i);
            CritterMove cm = (CritterMove)moveList.get(indexes[i]);
            lm.add(cm);
        }
        return lm;
    }



    /** Modify allCritterMoves in place, and return true if it changed. */
    boolean trimCritterMoves(ArrayList allCritterMoves)
    {
        Set takenHexLabels = new HashSet();  // XXX reuse?
        boolean changed = false;

        // First trim immobile creatures from the list, and add their
        // hexes to takenHexLabels.
        Iterator it = allCritterMoves.iterator();
        while (it.hasNext())
        {
            MoveList moveList = (MoveList)it.next();
            if (moveList.size() == 1)
            {
                // This critter is not mobile, and its hex is taken.
                CritterMove cm = (CritterMove)moveList.get(0);
                BattleChit critter2 = cm.getCritter();
                takenHexLabels.add(cm.getStartingHexLabel());
                it.remove();
                changed = true;
            }
        }

        // Now trim all moves to taken hexes from all movelists.
        it = allCritterMoves.iterator();
        while (it.hasNext())
        {
            List moveList = (List)it.next();
            Iterator it2 = moveList.iterator();
            while (it2.hasNext())
            {
                CritterMove cm = (CritterMove)it2.next();
                if (takenHexLabels.contains(cm.getEndingHexLabel()))
                {
                    it.remove();
                    changed = true;
                }
            }
        }

        return changed;
    }


    /** For an List of CritterMoves, concatenate all the creature names 
     *  in order.  If the list is null or empty, return an empty String. */
    private String creatureNames(List list)
    {
        if (list == null)
        {
            return "";
        }
        StringBuffer buf = new StringBuffer("");
        Iterator it = list.iterator();
        while (it.hasNext())
        {
            CritterMove cm = (CritterMove)it.next();
            BattleChit critter = cm.getCritter();
            buf.append(critter.getCreatureName());
        }
        return buf.toString();
    }


    BattleEvalConstants bec = new BattleEvalConstants();

    class BattleEvalConstants
    {
        int OFFBOARD_DEATH_SCALE_FACTOR = -150;
        int NATIVE_BONUS_TERRAIN = 50;
        int NATIVE_BOG = 20;
        int NON_NATIVE_PENALTY_TERRAIN = -100;
        int FIRST_RANGESTRIKE_TARGET = 300;
        int EXTRA_RANGESTRIKE_TARGET = 100;
        int RANGESTRIKE_TITAN = 500;
        int RANGESTRIKE_WITHOUT_PENALTY = 100;
        int ATTACKER_ADJACENT_TO_ENEMY = 400;
        int DEFENDER_ADJACENT_TO_ENEMY = -20;
        int ADJACENT_TO_ENEMY_TITAN = 1300;
        int ADJACENT_TO_RANGESTRIKER = 500;
        int KILL_SCALE_FACTOR = 100;
        int KILLABLE_TARGETS_SCALE_FACTOR = 10;
        int ATTACKER_GET_KILLED_SCALE_FACTOR = -20;
        int DEFENDER_GET_KILLED_SCALE_FACTOR = -40;
        int ATTACKER_GET_HIT_SCALE_FACTOR = -1;
        int DEFENDER_GET_HIT_SCALE_FACTOR = -2;
        int TITAN_TOWER_HEIGHT_BONUS = 2000;
        int DEFENDER_TOWER_HEIGHT_BONUS = 80;
        int TITAN_FORWARD_EARLY_PENALTY = -10000;
        int TITAN_BY_EDGE_OR_TREE_BONUS = 400;
        int DEFENDER_FORWARD_EARLY_PENALTY = -60;
        int ATTACKER_DISTANCE_FROM_ENEMY_PENALTY = -300;
        int ADJACENT_TO_BUDDY = 100;
        int ADJACENT_TO_BUDDY_TITAN = 200;
    }


    private int evaluateCritterMove(BattleChit critter)
    {
        final char terrain = client.getBattleTerrain();
        final String masterHexLabel = client.getBattleSite();
        final LegionInfo legion = client.getLegionInfo(
            client.getMyEngagedMarkerId());
        final LegionInfo enemy = client.getLegionInfo(
            client.getBattleInactiveMarkerId());
        final int skill = critter.getSkill();
        final BattleHex hex = client.getBattleHex(critter);
        final int turn = client.getBattleTurnNumber();


        int value = 0;

        // Add for sitting in favorable terrain.
        // Subtract for sitting in unfavorable terrain.
        if (hex.isEntrance())
        {
            // Staying offboard to die is really bad.
            value += bec.OFFBOARD_DEATH_SCALE_FACTOR *
                getCombatValue(critter, terrain);
        }
        else if (MasterHex.isNativeCombatBonus(critter.getCreature(), terrain))
        {
            if (hex.isNativeBonusTerrain())
            {
                value += bec.NATIVE_BONUS_TERRAIN;
            }
            // We want marsh natives to slightly prefer moving to bog hexes,
            // even though there's no real bonus there, to leave other hexes
            // clear for non-native allies.
            else if (hex.getTerrain() == 'o')
            {
                value += bec.NATIVE_BOG;
            }
        }
        else  // Critter is not native.
        {
            if (hex.isNonNativePenaltyTerrain())
            {
                value += bec.NON_NATIVE_PENALTY_TERRAIN;
            }
        }

        Set targetHexLabels = client.findStrikes(critter.getTag());
        int numTargets = targetHexLabels.size();

        // TODO Reward ganging up on enemies.

        if (numTargets >= 1)
        {
            if (!client.isInContact(critter, true))
            {
                // Rangestrikes.
                value += bec.FIRST_RANGESTRIKE_TARGET;

                // Having multiple targets is good, in case someone else
                // kills one.
                if (numTargets >= 2)
                {
                    value += bec.EXTRA_RANGESTRIKE_TARGET;
                }

                // Non-warlock skill 4 rangestrikers should slightly prefer
                // range 3 to range 4.  Non-brush rangestrikers should
                // prefer strikes not through bramble.  Warlocks should
                // try to rangestrike titans.
                boolean penalty = true;
                Iterator it = targetHexLabels.iterator();
                while (it.hasNext())
                {
                    String hexLabel = (String)it.next();
                    BattleChit target = client.getBattleChit(hexLabel);
                    if (target.isTitan())
                    {
                        value += bec.RANGESTRIKE_TITAN;
                    }
                    int strikeNum = client.getStrike().getStrikeNumber(critter,
                        target);
                    if (strikeNum <= 4 - skill + target.getSkill())
                    {
                        penalty = false;
                    }
                }
                if (!penalty)
                {
                    value += bec.RANGESTRIKE_WITHOUT_PENALTY;
                }
            }
            else
            {
                // Normal strikes.  If we can strike them, they can strike us.

                // Reward being adjacent to an enemy if attacking.
                if (legion.getMarkerId().equals(client.getAttackerMarkerId()))
                {
                    value += bec.ATTACKER_ADJACENT_TO_ENEMY;
                }
                // Slightly penalize being adjacent to an enemy if defending.
                else
                {
                    value += bec.DEFENDER_ADJACENT_TO_ENEMY;
                }

                int killValue = 0;
                int numKillableTargets = 0;
                int hitsExpected = 0;

                Iterator it = targetHexLabels.iterator();
                while (it.hasNext())
                {
                    String hexLabel = (String)it.next();
                    BattleChit target = client.getBattleChit(hexLabel);

                    // Reward being next to enemy titans.  (Banzai!)
                    if (target.isTitan())
                    {
                        value += bec.ADJACENT_TO_ENEMY_TITAN;
                    }

                    // Reward being next to a rangestriker, so it can't hang
                    // back and plink us.
                    if (target.isRangestriker() && !critter.isRangestriker())
                    {
                        value += bec.ADJACENT_TO_RANGESTRIKER;
                    }

                    // Reward being next to an enemy that we can probably
                    // kill this turn.
                    int dice = client.getStrike().getDice(critter, target);
                    int strikeNum = client.getStrike().getStrikeNumber(critter,
                        target);
                    double meanHits = Probs.meanHits(dice, strikeNum);
                    if (meanHits + target.getHits() >= target.getPower())
                    {
                        numKillableTargets++;
                        int targetValue = getKillValue(target, terrain);
                        killValue = Math.max(targetValue, killValue);
                    }

                    // Penalize damage that we can take this turn,
                    {
                        dice = client.getStrike().getDice(target, critter);
                        strikeNum = client.getStrike().getStrikeNumber(target,
                            critter);
                        hitsExpected += Probs.meanHits(dice, strikeNum);
                    }
                }

                value += bec.KILL_SCALE_FACTOR * killValue +
                    bec.KILLABLE_TARGETS_SCALE_FACTOR * numKillableTargets;

                int power = critter.getPower();
                int hits = critter.getHits();

                // XXX Attacking legions late in battle ignore damage.
                if (legion.getMarkerId().equals(
                    client.getDefenderMarkerId()) || critter.isTitan() ||
                    turn <= 4)
                {
                    if (hitsExpected + hits >= power)
                    {
                        if (legion.getMarkerId().equals(
                            client.getAttackerMarkerId()))
                        {
                            value += bec.ATTACKER_GET_KILLED_SCALE_FACTOR * 
                                getKillValue(critter, terrain);
                        }
                        else
                        {
                            value += bec.DEFENDER_GET_KILLED_SCALE_FACTOR * 
                                getKillValue(critter, terrain);
                        }
                    }
                    else
                    {
                        if (legion.getMarkerId().equals(
                            client.getAttackerMarkerId()))
                        {
                            value += bec.ATTACKER_GET_HIT_SCALE_FACTOR * 
                                getKillValue(critter, terrain);
                        }
                        else
                        {
                            value += bec.DEFENDER_GET_HIT_SCALE_FACTOR * 
                                getKillValue(critter, terrain);
                        }
                    }
                }
            }
        }

        BattleHex entrance = BattleMap.getEntrance(terrain, masterHexLabel,
            legion.getEntrySide());

        // Reward titans sticking to the edges of the back row
        // surrounded by allies.  We need to relax this in the
        // last few turns of the battle, so that attacking titans
        // don't just sit back and wait for a time loss.
        if (critter.isTitan())
        {
            if (HexMap.terrainIsTower(terrain))
            {
                // Stick to the center of the tower.
                value += bec.TITAN_TOWER_HEIGHT_BONUS * hex.getElevation();
            }
            else
            {
                if (turn <= 4)
                {
                    value += bec.TITAN_FORWARD_EARLY_PENALTY *
                        client.getStrike().getRange(hex, entrance, true);
                    for (int i = 0; i < 6; i++)
                    {
                        BattleHex neighbor = hex.getNeighbor(i);
                        if (neighbor == null || neighbor.getTerrain() == 't')
                        {
                            value += bec.TITAN_BY_EDGE_OR_TREE_BONUS;
                        }
                    }
                }
            }
        }

        // Encourage defending critters to hang back.
        else if (legion.getMarkerId().equals(client.getDefenderMarkerId()))
        {
            if (HexMap.terrainIsTower(terrain))
            {
                // Stick to the center of the tower.
                value += bec.DEFENDER_TOWER_HEIGHT_BONUS * hex.getElevation();
            }
            else
            {
                int range = client.getStrike().getRange(hex, entrance, true);

                // To ensure that defending legions completely enter
                // the board, prefer the second row to the first.  The
                // exception is small legions early in the battle,
                // when trying to survive long enough to recruit.
                int preferredRange = 3;
                if (legion.getHeight() <= 3 && turn < 4)
                {
                    preferredRange = 2;
                }
                if (range != preferredRange)
                {
                    value += bec.DEFENDER_FORWARD_EARLY_PENALTY *
                        Math.abs(range - preferredRange);
                }
            }
        }

        else  // Attacker, non-titan, needs to charge.
        {
            // Head for enemy creatures.
            value += bec.ATTACKER_DISTANCE_FROM_ENEMY_PENALTY *
                client.getStrike().minRangeToEnemy(critter);
        }

        // Adjacent buddies
        for (int i = 0; i < 6; i++)        
        {
            if (!hex.isCliff(i))
            {
                BattleHex neighbor = hex.getNeighbor(i);
                if (neighbor != null && client.isOccupied(neighbor))
                {
                    BattleChit other = client.getBattleChit(
                        neighbor.getLabel());
                    if (other.isInverted() == critter.isInverted())
                    {
                        // Buddy
                        if (other.isTitan())
                        {
                            value += bec.ADJACENT_TO_BUDDY_TITAN;
                        }
                        else
                        {
                            value += bec.ADJACENT_TO_BUDDY;
                        }
                    }
                }
            }
        }

        return value;
    }

    private int evaluateLegionBattleMove(LegionMove lm)
    {
        // First we need to move all critters into position.
        Iterator it = lm.getCritterMoves().iterator();
        while (it.hasNext())
        {
            CritterMove cm = (CritterMove)it.next();
            cm.getCritter().moveToHex(cm.getEndingHexLabel());
        }

        // Then find the sum of all critter evals.
        int sum = 0;
        it = lm.getCritterMoves().iterator();
        while (it.hasNext())
        {
            CritterMove cm = (CritterMove)it.next();
            sum += evaluateCritterMove(cm.getCritter());
        }

        // Then move them all back.
        it = lm.getCritterMoves().iterator();
        while (it.hasNext())
        {
            CritterMove cm = (CritterMove)it.next();
            cm.getCritter().moveToHex(cm.getStartingHexLabel());
        }

        return sum;
    }


    /** Fliers should move last, since they can fly over allies.
     *  Those moving far should move before those staying close to home.
     *  Non-natives should move before natives, since natives can often
     *     move through hexes that non-natives can't.
     *  More important creatures should move before less important
     *     creatures. */
    public final class MoveOrderComparator implements Comparator
    {
        MoveOrderComparator()
        {
        }

        public int compare(Object o1, Object o2)
        {
            final char terrain = client.getBattleTerrain();

            ArrayList moveList1 = (MoveList)o1;
            ArrayList moveList2 = (MoveList)o2;
            CritterMove cm1 = (CritterMove)moveList1.get(0);
            CritterMove cm2 = (CritterMove)moveList2.get(0);
            BattleChit critter1 = cm1.getCritter();
            BattleChit critter2 = cm2.getCritter();
            BattleHex desiredHex1 = cm1.getEndingHex(terrain);
            BattleHex desiredHex2 = cm2.getEndingHex(terrain);

            if (critter1.getCreature().isFlier() && 
                !critter2.getCreature().isFlier())
            {
                return 1;
            }
            else if (critter2.getCreature().isFlier() && 
                !critter1.getCreature().isFlier())
            {
                return -1;
            }

            int range1 = client.getStrike().getRange(
                client.getStartingBattleHex(critter1), desiredHex1, true);
            int range2 = client.getStrike().getRange(
                client.getStartingBattleHex(critter2), desiredHex2, true);
            int diff = range2 - range1;
            if (diff != 0)
            {
                return diff;
            }

            if (MasterHex.isNativeCombatBonus(critter1.getCreature(), 
                terrain) && !MasterHex.isNativeCombatBonus(
                critter2.getCreature(), terrain))
            {
                return 1;
            }
            else if (MasterHex.isNativeCombatBonus(critter2.getCreature(), 
                terrain) && !MasterHex.isNativeCombatBonus(
                critter1.getCreature(), terrain))
            {
                return -1;
            }

            diff = getKillValue(critter2, terrain) -
                getKillValue(critter1, terrain);
            if (diff != 0)
            {
                return diff;
            }
            else
            {
                return critter1.getCreatureName().compareTo(
                    critter2.getCreatureName());
            }
        }
    }


    class CritterMove
    {
        private int value;
        private BattleChit critter;
        private String startingHexLabel;
        private String endingHexLabel;

        CritterMove(BattleChit critter, String startingHexLabel,
            String endingHexLabel)
        {
            super();
            this.critter = critter;
            this.startingHexLabel = startingHexLabel;
            this.endingHexLabel = endingHexLabel;
        }

        public void setValue(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }

        BattleChit getCritter()
        {
            return critter;
        }

        String getStartingHexLabel()
        {
            return startingHexLabel;
        }

        String getEndingHexLabel()
        {
            return endingHexLabel;
        }

        BattleHex getStartingHex(char terrain)
        {
            return HexMap.getHexByLabel(terrain, startingHexLabel);
        }

        BattleHex getEndingHex(char terrain)
        {
            return HexMap.getHexByLabel(terrain, endingHexLabel);
        }

        public String toString()
        {
            return critter.getDescription() + " to " + getEndingHexLabel();
        }
    }

    /** MoveList is an ArrayList of CritterMoves */
    class MoveList extends ArrayList
    {
    }

    /** LegionMove has a List of one CritterMove per mobile critter
     *  in the legion. */
    class LegionMove
    {
        private List critterMoves = new ArrayList();

        void add(CritterMove cm)
        {
            critterMoves.add(cm);
        }

        List getCritterMoves()
        {
            return Collections.unmodifiableList(critterMoves);
        }

        public String toString()
        {
            StringBuffer sb = new StringBuffer();
            Iterator it = critterMoves.iterator();
            while (it.hasNext())
            {
                CritterMove cm = (CritterMove)it.next();
                sb.append(cm.toString());
                if (it.hasNext())
                {
                    sb.append(", ");    
                }
            }
            return sb.toString();
        }

        public boolean equals(Object ob)
        {
            if (!(ob instanceof LegionMove))
            {
                return false;
            }
            LegionMove lm = (LegionMove)ob;
            return toString().equals(lm.toString());
        }

        public int hashCode()
        {
            return toString().hashCode();
        }
    }


    class TriggerTimeIsUp extends TimerTask
    {
        public void run()
        {
            timeIsUp = true;
        }
    }
}
