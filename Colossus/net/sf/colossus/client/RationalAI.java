package net.sf.colossus.client;


import java.util.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.util.Probs;
import net.sf.colossus.util.Perms;
import net.sf.colossus.util.Options;
import net.sf.colossus.util.DevRandom;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Dice;
import net.sf.colossus.server.VariantSupport;


/**
 * Simple implementation of a Titan AI
 * @version $Id$
 * @author Bruce Sherrod, David Ripton
 * @author Romain Dolbeau
 * @author Corwin Joy, extensively rewritten on 02-Oct-2003
 */
public class RationalAI implements AI
{
    Client client;
    String[] hintSectionUsed = { Constants.sectionOffensiveAI };
    private int timeLimit = Constants.DEFAULT_AI_TIME_LIMIT;  // in s
    private boolean timeIsUp;
    private Random random = new DevRandom();
    protected boolean I_HATE_HUMANS = false;
    private List legionsToSplit = new ArrayList();

    public RationalAI(Client client)
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
        List myMarkerIds = new ArrayList();
        List otherMarkerIds = new ArrayList();

        // split between own / other
        while (it.hasNext())
        {
            markerId = (String)it.next();
            if (preferredShortColor != null &&
                    markerId.startsWith(preferredShortColor))
            {
                myMarkerIds.add(markerId);
            }
            else
            {
                otherMarkerIds.add(markerId);
            }
        }

        if (!(myMarkerIds.isEmpty()))
        {
            Collections.shuffle(myMarkerIds, random);
            return (String)(myMarkerIds.get(0));
        }

        if (!(otherMarkerIds.isEmpty()))
        {
            Collections.shuffle(otherMarkerIds, random);
            return (String)(otherMarkerIds.get(0));
        }
        return null;
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
                    (legion.hasTitan() ||
                    legion.getPointValue() >= minimumSizeToRecruit))
            {
                // Log.debug("Calling chooseRecruit() in muster().");
                Creature recruit = chooseRecruit(legion, legion.getHexLabel());

                if (recruit != null)
                {
                    List recruiters = client.findEligibleRecruiters(markerId,
                            recruit.getName());

                    String recruiterName = null;

                    if (!recruiters.isEmpty())
                    {
                        // Just take the first one.
                        recruiterName = (String)recruiters.get(0);
                    }
                    client.doRecruit(markerId, recruit.getName(), recruiterName);
                }
            }
        }
    }

    public void reinforce(LegionInfo legion)
    {
        // Log.debug("Calling chooseRecruit() in reinforce().");
        Creature recruit = chooseRecruit(legion, legion.getHexLabel());
        String recruitName = null;
        String recruiterName = null;

        if (recruit != null)
        {
            recruitName = recruit.getName();
            List recruiters = client.findEligibleRecruiters(legion.getMarkerId(),
                    recruit.getName());

            if (!recruiters.isEmpty())
            {
                recruiterName = (String)recruiters.get(0);
            }
        }
        // Call regardless to advance past recruiting.
        client.doRecruit(legion.getMarkerId(), recruitName, recruiterName);
    }

    Creature chooseRecruit(LegionInfo legion, String hexLabel)
    {
        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
        List recruits = client.findEligibleRecruits(legion.getMarkerId(),
                hexLabel);

        if (recruits.size() == 0)
        {
            return null;
        }

        /*
         // graph code disabled ATM
         Creature temprecruit =
         getBestRecruitmentOneTurnAhead(legion, hex, recruits);
         Creature temprecruit2 =
         getBestRecruitmentInfinityAhead(legion, hex, recruits);
         Creature temprecruit3 =
         getBestRecruitmentPlacesNextTurn(legion, hex, recruits);
         */

        Creature recruit = getVariantRecruitHint(legion, hex, recruits);

        /*
         // graph code disabled ATM
         if (temprecruit != recruit)
         {
         // let's pick-up the creature with the best next turn opportunity
         // if it has the best 'infinity ahead', and in 4 out of 6 case
         // if it doesn't.
         if ((temprecruit == temprecruit2) ||
         (Dice.rollDie() > 2))
         {
         recruit = temprecruit;
         }
         }
         */

        /* use the hinted value as the actual recruit */
        return recruit;
    }

    public boolean split()
    {
        legionsToSplit.clear();

        PlayerInfo player = client.getPlayerInfo();
        Iterator it = player.getLegionIds().iterator();
        while (it.hasNext())
        {
            String markerId = (String)it.next();
            legionsToSplit.add(markerId);
        }
        return fireSplits();
    }

    /** Return true if done with all splits, false if splits or callbacks remain. */
    private boolean fireSplits()
    {
        if (legionsToSplit.isEmpty())
        {
            return true;
        }
        String markerId = (String)legionsToSplit.remove(0);
        boolean done = splitOneLegion(client.getPlayerInfo(), markerId);
        if (done)
        {
            return fireSplits();
        }
        return false;
    }

    // XXX Undoing a split could release the marker needed to do another
    // split, so we need to synchronize access.
    /** If parentId and childId are null, this is a callback to an undo split */
    public boolean splitCallback(String parentId, String childId)
    {
        if (parentId == null && childId == null)
        {
            // Undo split is done; fire off the next split
            return fireSplits();
        }
        boolean done = splitOneLegionCallback(parentId, childId);
        if (done)
        {
            return fireSplits();
        }
        return false;
    }

    // Compute the expected value of a split legion
    // If we want to compute just a single legion, pass null for the child_legion
    private double expectedValueSplitLegion(Map[] enemyAttackMap, LegionInfo
            legion, LegionInfo child_legion)
    {
        double split_value = 0.0;

        // Compute value of staying for each split stack
        double stay_here_value1 = hexRisk(legion, legion.getCurrentHex(),
                enemyAttackMap, null);
        double stay_here_value2;

        if (child_legion != null)
        {
            stay_here_value2 = hexRisk(child_legion, legion.getCurrentHex(),
                    enemyAttackMap, null);
            Log.debug("expectedValueSplitLegion(), value of " +
                    "staying here for split legion1 " + stay_here_value1);
            Log.debug("expectedValueSplitLegion(), value of " +
                    "staying here for split legion1 " + stay_here_value2);
        }
        else
        {
            Log.debug("expectedValueSplitLegion(), value of " +
                    "staying here for unsplit legion " + stay_here_value1);
            stay_here_value2 = 0.0;
        }

        for (int roll = 1; roll <= 6; roll++)
        {
            Log.debug("expectedValueSplitLegion: Roll " + roll);
            Set moves = client.getMovement().listAllMoves(legion,
                    legion.getCurrentHex(), roll);

            int size1 = moves.size() + 1;
            int size2;

            if (child_legion != null)
            {
                size2 = moves.size() + 1;
            }
            else
            {
                size2 = 2;
            }

            double[] valueStack1 = new double[size1];
            double[] valueStack2 = new double[size2];
            Iterator moveIt = moves.iterator();
            int move_i = 0;

            while (moveIt.hasNext())
            {
                String hexLabel = (String)moveIt.next();
                MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
                double risk_payoff1 = evaluateMove(legion, hex, RECRUIT_TRUE,
                        enemyAttackMap, 1, true);

                valueStack1[move_i] = risk_payoff1;
                if (child_legion != null)
                {
                    double risk_payoff2 = evaluateMove(child_legion, hex,
                            RECRUIT_TRUE,
                            enemyAttackMap, 1, true);

                    valueStack2[move_i] = risk_payoff2;
                }
                move_i++;
            }
            // add no-move as an option for each stack
            valueStack1[move_i] = stay_here_value1;
            if (child_legion != null)
            {
                valueStack2[move_i] = stay_here_value2;
            }
            else
            {
                valueStack2[0] = 0.0;  // for loop below we need 2 available "stay here" values
                valueStack2[0] = 0.0;
            }

            // find optimal move for this roll
            // iterate through move combinations of stack1 and stack2 to find max
            double max_split;

            max_split = Integer.MIN_VALUE;

            for (int i = 0; i < size1; i++)
            {
                double val_i = valueStack1[i];

                for (int j = 0; j < size2; j++)
                {
                    if (i == j)
                    {
                        continue;
                    } // split stacks can't move to same hex
                    double val_j = valueStack2[j];
                    double val = val_i + val_j;

                    if (val > max_split)
                    {
                        max_split = val;
                    }
                }
            }
            split_value += max_split;
        } // end for roll

        return split_value;
    }

    /** Return true if done, false if waiting for callback. */
    private boolean splitOneLegion(PlayerInfo player, String markerId)
    {
        Log.setShowDebug(true);
        Log.debug("splitOneLegion()");

        LegionInfo legion = client.getLegionInfo(markerId);

        // Allow aggressive splits - especially early in the game it is better
        // to split more often -- this should get toned down later in the
        // game by the scooby snack factor
        if (legion.getHeight() < 6)
        {
            Log.debug("No split: height < 6");
            return true;
        }

        Map[] enemyAttackMap = buildEnemyAttackMap(client.getPlayerInfo());

        double stay_here_risk = hexRisk(legion, legion.getCurrentHex(),
                enemyAttackMap, null);

        boolean at_risk = false;
        if (stay_here_risk > 0 || legion.hasTitan())
        {
            at_risk = true;
        }

        StringBuffer results = new StringBuffer();
        boolean hasMustered = false;

        MusteredCreatures mc = chooseCreaturesToSplitOut(legion, at_risk);
        List creatures = mc.creatures;

        hasMustered = mc.mustered;
        Iterator it = creatures.iterator();
        int child_value = 0;

        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();

            child_value += creature.getPointValue();
            results.append(creature.getName());
            if (it.hasNext())
            {
                results.append(",");
            }
        }

        // don't split a stack if it has not mustered
        if (legion.getHeight() < 7 && !hasMustered)
        {
            Log.debug("No split: height < 7 and not mustered");
            return true;
        }

        // Do the split.  If we don't like the result we will undo it at the end

        // Log.debug("previous # of splits " + num_split);
        String newMarkerId = pickMarker(player.getMarkersAvailable(),
                player.getShortColor());

        if (newMarkerId == null)
        {
            Log.debug("No split.  No markers available.");
            return true;
        }

        Log.debug("Wait for split callback");
        client.doSplit(legion.getMarkerId(), newMarkerId, results.toString());
        return false;
    }

    /** Return true if done, false if waiting for undo split */
    private boolean splitOneLegionCallback(String markerId, String newMarkerId)
    {
        LegionInfo legion = client.getLegionInfo(markerId);
        Log.debug("Split complete");

        if (client.getTurnNumber() == 1)
        { // first turn
            Log.debug("First turn split");
            return true;
        }

        LegionInfo child_legion = client.getLegionInfo(newMarkerId);

        // Compute split value
        Log.debug("splitOneLegion(): Expected value with split");
        Map[] enemyAttackMap = buildEnemyAttackMap(client.getPlayerInfo());
        double split_value = expectedValueSplitLegion(enemyAttackMap, legion,
                child_legion); // expected value of split    

        // find expected value of no split
        Log.debug("splitOneLegionCallback(): Expected value with no split");
        double no_split_value = 0.0;
        if (client.getTurnNumber() > 1)
        {
            no_split_value = expectedValueSplitLegion(enemyAttackMap, legion,
                    null);
        }

        // For Titan group, try to only split when at 7
        // The only exception should be if we are under
        // severe attack and splitting can save us
        if (legion.hasTitan() &&
                (legion.getHeight() + child_legion.getHeight()) < 7)
        {
            split_value -= 10000;
            no_split_value += 10000;
        }

        // If expected value of split + 5 <= no split, do not split.
        // This gives tendency to split if not under attack.
        // If no_split_value is < -20 , we are under attack and trapped.
        // Do not split.
        Log.debug("no split value: " + no_split_value);
        Log.debug("split value: " + split_value);
        if (split_value * 1.02 <= no_split_value ||
                split_value < -20)
        {
            // Undo the split  
            client.undoSplit(newMarkerId);
            Log.debug("undo split - better to keep stack together");
            return false;
        }
        else
        {
            Log.debug("keep the split");
            return true;
        }
    }

    /** Find value of recruiting, including possibly attacking an enemy
     set enemy = null to indicate no enemy */
    private double recruitValue(LegionInfo legion, String hexLabel,
            LegionInfo enemy, String terrain)
    {
        int value = 0;

        // Allow recruits even at 7 high for purposes of calculating
        // the mobility value of a particular hex in evaluateMove

        /*
         if (legion.getHeight() > 6)
         {
         return 0;
         }
         **/

        // Consider recruiting.
        List recruits = client.findEligibleRecruits(legion.getMarkerId(),
                hexLabel);

        if (!recruits.isEmpty())
        {
            Creature bestRecruit = (Creature)recruits.get(recruits.size() - 1);

            value = ghrv(bestRecruit, legion, hintSectionUsed);
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
                java.util.List ral = TerrainRecruitLoader.getRecruitableAcquirableList(terrain,
                        nextScore);
                java.util.Iterator it = ral.iterator();

                while (it.hasNext())
                {
                    Creature tempRecruit = Creature.getCreatureByName((String)it.next());

                    if ((bestRecruit == null) ||
                            (ghrv(tempRecruit, legion, hintSectionUsed) >=
                            ghrv(bestRecruit, legion, hintSectionUsed)))
                    {
                        bestRecruit = tempRecruit;
                    }
                }
                nextScore += arv;
            }

            // add value of any angels
            if (bestRecruit != null)
            {
                value += ghrv(bestRecruit, legion, hintSectionUsed);
            }
        }

        return value;
    }

    // Given a list of creatures sorted by value, figure out which ones are 
    // redundant / have mustered. 
    // Removes any creatures that have mustered from the original sorted list
    List removeMustered(List sortedCreatures)
    {
        // Look at 4 lowest valued creatures
        // Try to pull out pair that has already mustered.
        List creaturesThatHaveMustered = new ArrayList();

        outer:
        for (int index1 = 0; index1 < 4 && index1 < sortedCreatures.size(); index1++)
        {
            String critter1 = ((Creature)sortedCreatures.get(index1)).getName();

            for (int index2 = index1 + 1; index2 < sortedCreatures.size(); index2++)
            {
                String critter2 = ((Creature)sortedCreatures.get(index2)).getName();

                if (critter1 == critter2)
                { // mustering yourself does not count
                    continue;
                }
                // Log.debug("Can muster " + critter1 + " up to " + critter2 + "?"); 
                if (TerrainRecruitLoader.getRecruitGraph().isRecruitDistanceLessThan(critter1,
                        critter2, 2))
                {// this creature has mustered
                    creaturesThatHaveMustered.add(sortedCreatures.get(index1));
                    sortedCreatures.remove(index1);
                    index1--; // adjust index to account for removal
                    // Log.debug("Yes.");
                    continue outer;
                }
                // Log.debug("No.");
            }
        }

        return creaturesThatHaveMustered;
    }

    public class compCreaturesByValueName implements Comparator
    {
        private LegionInfo legion;

        public compCreaturesByValueName(LegionInfo l)
        {
            legion = l;
        }

        public final int compare(Object o1, Object o2)
        {
            int val1 = ghrv((Creature)o1, legion, hintSectionUsed);
            int val2 = ghrv((Creature)o2, legion, hintSectionUsed);

            if (val1 < val2)
            {
                return -1;
            }
            if (val1 > val2)
            {
                return 1;
            }
            // val1 == val2, compare string
            return ((Creature)o1).getName().compareTo(((Creature)o2).getName());
        }
    }

    // Sort creatures first by value then by name.
    // Exclude titan.
    List sortCreaturesByValueName(List Creatures, LegionInfo legion)
    {
        List sortedCreatures = new ArrayList();
        Iterator critterIt = Creatures.iterator();

        // copy list excluding titan
        while (critterIt.hasNext())
        {
            String name = (String)critterIt.next();
            Creature critter = Creature.getCreatureByName(name);

            // Never split out the titan.
            if (critter.isTitan())
            {
                continue;
            }
            sortedCreatures.add(critter);
        }
        Collections.sort(sortedCreatures, new compCreaturesByValueName(legion));
        return sortedCreatures;
    }

    // Count number of creatures in the stack that have mustered
    int countMustered(LegionInfo legion)
    {
        List sortedCreatures = sortCreaturesByValueName(legion.getContents(),
                legion);
        List creaturesThatHaveMustered = removeMustered(sortedCreatures);

        return creaturesThatHaveMustered.size();
    }

    class MusteredCreatures
    {
        public boolean mustered;
        public List creatures;
        MusteredCreatures(boolean m, List c)
        {
            mustered = m;
            creatures = c;
        }
    }

    /** Decide how to split this legion, and return a list of
     *  Creatures to remove + status flag indicating if these
     creatures have mustered or not*/
    MusteredCreatures chooseCreaturesToSplitOut(LegionInfo legion, boolean at_risk)
    {
        //
        // split a 5 to 8 high legion 
        //
        // idea: pick the 2 weakest creatures and kick them
        // out. if there are more than 2 weakest creatures,
        // try to split out ones that have already mustered.
        // return split + status flag to indicate if these
        // creatures have mustered or not
        //
        // Also: when splitting, in the case of cyclops, ogres
        // or centaurs try to split out three rather than 2 if
        // these have already mustered
        //
        if (legion.getHeight() == 8)
        {
            List creatures = doInitialGameSplit(legion.getHexLabel());

            return new MusteredCreatures(true, creatures);
        }

        Log.debug("sortCreaturesByValueName() in chooseCreaturesToSplitOut");

        List sortedCreatures = sortCreaturesByValueName(legion.getContents(),
                legion);

        Log.debug("Sorted stack - minus titan: " + sortedCreatures);
        // Look at lowest valued creatures
        // Try to pull out pair that has already mustered.
        Log.debug("removeMustered() in chooseCreaturesToSplitOut");
        List creaturesThatHaveMustered = removeMustered(sortedCreatures);

        Log.debug("List of mustered creatures: " + creaturesThatHaveMustered);
        boolean hasMustered = false;

        if (creaturesThatHaveMustered.size() < 1)
        {
            hasMustered = false;
        }
        else
        {
            hasMustered = true;
        }

        List creaturesToRemove = new ArrayList();

        // Try to pull out pair that has already mustered.
        Log.debug("build final split list in chooseCreaturesToSplitOut");
        Iterator sortIt = creaturesThatHaveMustered.iterator();
        boolean split_all_mustered = false;

        /*
         if (!at_risk)
         {
         split_all_mustered = true;
         }
         **/

        while (sortIt.hasNext() &&
                (creaturesToRemove.size() < 2 || split_all_mustered) &&
                creaturesToRemove.size() < 4)
        {
            Creature critter = (Creature)sortIt.next();
            creaturesToRemove.add(critter);
        }

        // If we have 3 mustered creatures, check if we have 3
        // Centaur, Ogre, Cyclops, Troll, Lion.  If so, try to keep the
        // 3 together for maximum mustering potential 
        // it is a bit aggressive to keep trying to do 3/4 splits
        // but it seems to give a better result
        if (sortIt.hasNext() && !at_risk)
        {
            Creature first_remove = (Creature)creaturesToRemove.get(0);
            Creature critter = (Creature)sortIt.next();
            String s_first = first_remove.getName();
            String s_critter = critter.getName();

            if (s_first.compareTo(s_critter) == 0) // 3 identical, due to sort
            {
                if (s_first.compareTo("Centaur") == 0 ||
                        s_first.compareTo("Ogre") == 0 ||
                        s_first.compareTo("Cyclops") == 0 ||
                        s_first.compareTo("Troll") == 0 ||
                        s_first.compareTo("Lion") == 0
                        )
                {
                    creaturesToRemove.add(critter);   // remove the 3rd creature 
                    Log.debug("Triple found!");
                    Log.debug("Creatures to remove: " + creaturesToRemove);
                    return new MusteredCreatures(hasMustered, creaturesToRemove);
                }

            }

        }
        // If mustered creatures don't come up to 2
        // start pulling out lowest valued un-mustered creatures
        sortIt = sortedCreatures.iterator();
        while (sortIt.hasNext() && creaturesToRemove.size() < 2)
        {
            Creature critter = (Creature)sortIt.next();

            creaturesToRemove.add(critter);
        }
        Log.debug("Creatures to remove: " + creaturesToRemove);

        return  new MusteredCreatures(hasMustered, creaturesToRemove);

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
    List doInitialGameSplit(String label)
    {
        java.util.List hintSuggestedSplit = getInitialSplitHint(label);

        /* Log.debug("HINT: suggest splitting " + hintSuggestedSplit +
         " in " + label); */

        if (!((hintSuggestedSplit == null) || (hintSuggestedSplit.size() != 4)))
        {
            return hintSuggestedSplit;
        }

        Creature[] startCre = TerrainRecruitLoader.getStartingCreatures(MasterBoard.getHexByLabel(label).getTerrain());
        // in CMU style splitting, we split centaurs in even towers,
        // ogres in odd towers.
        final boolean oddTower = "100".equals(label) || "300".equals(label) ||
                "500".equals(label);
        final Creature splitCreature = oddTower ? startCre[2] : startCre[0];
        final Creature nonsplitCreature = oddTower ? startCre[0] : startCre[2];

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
    private List CMUsplit(boolean favorTitan, Creature splitCreature,
            Creature nonsplitCreature, String label)
    {
        Creature[] startCre = TerrainRecruitLoader.getStartingCreatures(MasterBoard.getHexByLabel(label).getTerrain());
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
                splitoffs.add(Creature.getCreatureByName(TerrainRecruitLoader.getPrimaryAcquirable()));
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
                splitoffs.add(Creature.getCreatureByName(TerrainRecruitLoader.getPrimaryAcquirable()));
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
    private List MITsplit(boolean favorTitan, Creature splitCreature,
            Creature nonsplitCreature, String label)
    {
        Creature[] startCre = TerrainRecruitLoader.getStartingCreatures(MasterBoard.getHexByLabel(label).getTerrain());
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
                splitoffs.add(Creature.getCreatureByName(TerrainRecruitLoader.getPrimaryAcquirable()));
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
                splitoffs.add(Creature.getCreatureByName(TerrainRecruitLoader.getPrimaryAcquirable()));
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

    List getInitialSplitHint(String label)
    {
        List byName = VariantSupport.getInitialSplitHint(label, hintSectionUsed);

        if (byName == null)
        {
            return null;
        }

        List byCreature = new ArrayList();

        Iterator it = byName.iterator();

        while (it.hasNext())
        {
            String name = (String)it.next();
            Creature cre = Creature.getCreatureByName(name);

            if (cre == null)
            {
                Log.error("HINT: Unknown creature in hint (" + name +
                        "), aborting.");
                return null;
            }
            byCreature.add(cre);
        }
        return byCreature;
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


    // little helper class to store possible moves by legion
    private class LegionBoardMove
    {
        final String markerId;
        final String fromHex;
        final String toHex;
        final double val;
        final boolean noMove;

        LegionBoardMove(String markerId, String fromHex, String toHex, double val, boolean noMove)
        {
            this.markerId = markerId;
            this.fromHex = fromHex;
            this.toHex = toHex;
            this.val = val;
            this.noMove = noMove;
        }
    }

    /** Return true if we need to run this method again after the server
     *  updates the client with the results of a move or mulligan. */
    public boolean masterMove()
    {
        // Log.debug("This is RationalAI.");

        boolean failure = false;

        PlayerInfo player = client.getPlayerInfo();

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

        handleVoluntaryMoves(player, moveMap, enemyAttackMap);

        return false;
    }

    /** Return true if AI took a mulligan. */
    boolean handleMulligans(PlayerInfo player)
    {
        // TODO: This is really stupid.  Do something smart here.
        if (client.getTurnNumber() == 1 && player.getMulligansLeft() > 0 &&
                (client.getMovementRoll() == 2 || client.getMovementRoll() == 5) &&
                !client.tookMulligan())
        {
            client.mulligan();
            // XXX Need to wait for new movement roll.
            return true;
        }
        return false;
    }

    private class LegionIndex
    {
        int nIndex;
        int[] indexes;
        int[] sizes;
        boolean overflow;

        LegionIndex(List all_legionMoves)
        {
            nIndex = all_legionMoves.size();
            indexes = new int[nIndex];
            sizes = new int[nIndex];
            List legionMoves;
            ListIterator legit = all_legionMoves.listIterator();
            for (int i = 0; i < nIndex; i++)
            {
                indexes[i] = 0;
                legionMoves = (List)legit.next();
                sizes[i] = legionMoves.size();
            }
            indexes[nIndex - 1] = -1;
            overflow = false;
        }

        LegionIndex(LegionIndex li)
        {
            nIndex = li.nIndex;
            indexes = new int[nIndex];
            sizes = new int[nIndex];
            for (int i = 0; i < nIndex; i++)
            {
                indexes[i] = li.indexes[i];
                sizes[i] = li.sizes[i];
            }
            overflow = li.overflow;
        }

        private void next_index(int index)
        {
            if (index < 0)
            {
                // number has rolled over maximum.
                overflow = true;
                return;
            }

            if (index >= nIndex)
            {
                return;
            }

            indexes[index]++;
            if (indexes[index] >= sizes[index])
            {
                // when we reach the max size, roll over to zero and carry 
                indexes[index] = 0;
                next_index(index - 1);
            }
        }

        void set_next_index(int index)
        {
            if (index == nIndex - 1)
            {
                return;
            }

            next_index(index);
            indexes[nIndex - 1] = -1;

        }

        void print_index()
        {
            String desc = "";
            for (int i = 0; i < nIndex; i++)
            {
                desc += indexes[i] + ",";
            }
            Log.debug(desc);
        }

        private void next()
        {
            next_index(nIndex - 1);
            // print_index();
        }

        int index(int i)
        {
            return indexes[i];
        }

        boolean hasNext()
        {
            next();
            if (overflow)
            {
                return false;
            }

            return true;
        }

    }

    private boolean findMoveList(Map[] enemyAttackMap, List markerIds,
            List all_legionMoves, TreeSet occupiedHexes, boolean teleportsOnly)
    {
        boolean moved = false;
        Iterator it = markerIds.iterator();
        while (it.hasNext())
        {
            String markerId = (String)it.next();
            LegionInfo legion = client.getLegionInfo(markerId);

            Log.debug("consider marker " + markerId);

            if (legion.hasMoved() && !teleportsOnly)
            {
                // return if already called
                Log.debug("Ack.  handleVoluntaryMoves has already been called.");
                return true;
                // moved = true;  // note that we have already moved legions
                // continue;
            }

            if (legion.getCurrentHex() == null)
            {
                continue;
            }

            // compute the value of sitting still
            List legionMoves = new ArrayList();
            MasterHex hex = MasterBoard.getHexByLabel(legion.getCurrentHex().getLabel());
            double value = evaluateMove(legion, hex, RECRUIT_FALSE,
                    enemyAttackMap, 2, true);
            LegionBoardMove lmove = new LegionBoardMove(markerId,
                    legion.getCurrentHex().getLabel(),
                    legion.getCurrentHex().getLabel(), value, true);

            if (!teleportsOnly)
            {
                legionMoves.add(lmove);
                occupiedHexes.add(legion.getCurrentHex().getLabel());

                Log.debug("value of sitting still at hex " + hex.getLabel() +
                        " : " +
                        value);
            }

            // find the expected value of all moves for this legion
            Set set;

            if (!teleportsOnly)
            {
                // exclude teleport moves
                set = client.getMovement().listNormalMoves(legion,
                        legion.getCurrentHex(), client.getMovementRoll());
            }
            else
            {
                // only teleport moves
                set = client.getMovement().listTeleportMoves(legion,
                        legion.getCurrentHex(), client.getMovementRoll());
            }

            Iterator moveIterator = set.iterator();

            while (moveIterator.hasNext())
            {
                final String hexLabel = (String)moveIterator.next();
                hex = MasterBoard.getHexByLabel(hexLabel);
                value = evaluateMove(legion, hex, RECRUIT_TRUE, enemyAttackMap,
                        2, true);

                Log.debug("value hex " + hexLabel + " value: " + value);

                lmove = new LegionBoardMove(markerId,
                        legion.getCurrentHex().getLabel(),
                        hexLabel, value, false);
                legionMoves.add(lmove);
            }

            // Sort moves in the order of descending value.
            Collections.sort(legionMoves, new Comparator()
            {
                public int compare(Object o1, Object o2)
                {
                    LegionBoardMove L1 = (LegionBoardMove)o1;
                    LegionBoardMove L2 = (LegionBoardMove)o2;
                    return (int)L2.val - (int)L1.val; // want descending order
                }
            }
            );

            all_legionMoves.add(legionMoves);
        }

        return moved;

    }

    private void handleVoluntaryMoves(PlayerInfo player, Map moveMap,
            Map[] enemyAttackMap)
    {
        Log.debug("handleVoluntaryMoves()");

        boolean moved = false;
        List markerIds = player.getLegionIds();
        List all_legionMoves = new ArrayList();

        // N.B.!! We must use a multi-set type container here
        // since a from hex may show up twice in the case of a split
        // legion and we must not show this hex as available unless
        // both of the legions move off of the hex.
        TreeSet occupiedHexes = new TreeSet();

        if (findMoveList(enemyAttackMap, markerIds,
                all_legionMoves, occupiedHexes, false))
        {
            return;
        }

        Log.debug("done computing move values for legions");

        // handle teleports
        // XXX
        // just take the best teleport.  this is not quite right
        // since it may stick the legion that does not get to
        // teleport with a really bad move.  it is not easy
        // to figure this out though.
        if (client.getMovementRoll() == 6)
        {
            List teleport_legionMoves = new ArrayList();
            TreeSet dummy = new TreeSet();
            findMoveList(enemyAttackMap, markerIds, teleport_legionMoves, dummy,
                    true);

            ListIterator legit = teleport_legionMoves.listIterator();
            LegionBoardMove best_move = new LegionBoardMove("", "", "", 0, true);
            double best_value = 0;
            while (legit.hasNext())
            {
                List legionMoves = (List)legit.next();
                if (legionMoves.size() < 6)
                {
                    continue;  // not a teleporting legion
                }
                LegionBoardMove lm = (LegionBoardMove)legionMoves.get(0);
                if (lm.val > best_value)
                {
                    Log.debug("found new teleport best move " + lm.markerId +
                            " to " + lm.toHex + " value " + lm.val);
                    best_value = lm.val;
                    best_move = lm;
                }
            }

            if (best_value > 0)
            {
                if (!best_move.noMove)
                {
                    Log.debug("found teleport:  " + best_move.markerId + " to " +
                            best_move.toHex + " value " + best_move.val);
                    boolean moved_legion = doMove(best_move.markerId,
                            best_move.toHex);
                    boolean found = false;
                    if (moved_legion)
                    {
                        // remove list of moves from all_legionMoves - we are done with this legion
                        Iterator legit2 = all_legionMoves.iterator();
                        while (legit2.hasNext())
                        {
                            List legionMoves2 = (List)legit2.next();
                            if (legionMoves2.isEmpty())
                            {
                                continue;
                            }
                            LegionBoardMove lm2 = (LegionBoardMove)legionMoves2.get(0);

                            // is lm2 == lm1 ?
                            if (lm2.markerId.equals(best_move.markerId))
                            {
                                found = true;
                                break;
                            }
                        }
                        if (found)
                        {
                            legit2.remove();
                        }
                        occupiedHexes.remove(best_move.fromHex); // hex is now free
                        occupiedHexes.add(best_move.toHex);
                        moved = true;
                    }
                }
            }
        }

        /*
         // Find any legions that have a best move which
         // is not also a best move for another legion.
         // In that case, just move the legion.
         *
         * KILL this.  In does not correctly handle
         * legions that are split and must move.  Why?
         * Because legion A could move to a spot that prevents
         * split legion B from moving which is illegal.
         ListIterator legit = all_legionMoves.listIterator();
         while (legit.hasNext())
         {
         List legionMoves = (List)legit.next();
         if (legionMoves.isEmpty())
         {
         continue;
         }
         LegionBoardMove lm = (LegionBoardMove)legionMoves.get(0);
         String best_hex = lm.toHex;

         if (occupiedHexes.contains(best_hex))
         {
         // If we are moving into an occupied hex, assume it may
         // cause a conflict.  This is to handle moving into hexes
         // where legions have split.
         continue;
         }
         Iterator legit2 = all_legionMoves.iterator();
         boolean duplicate = false;
         // Does another legion have this as a best move?
         while (legit2.hasNext())
         {
         List legionMoves2 = (List)legit2.next();
         if (legionMoves2.isEmpty())
         {
         continue;
         }
         LegionBoardMove lm2 = (LegionBoardMove)legionMoves2.get(0);

         // is lm2 == lm1 ?
         if (lm2.markerId.equals(lm.markerId))
         {
         continue;
         }

         String best_hex2 = lm2.toHex;
         if (best_hex2.equals(best_hex))
         {
         duplicate = true; // found a duplicate
         Log.debug("Found duplicate for " + lm.markerId + " at " +
         lm.toHex + " duplicate is " + lm2.markerId + " at " +
         lm2.toHex);
         break;
         }
         }

         if (!duplicate)
         {
         Log.debug("No conflict. Optimum move for " + lm.markerId +
         " is " + lm.toHex);
         if (!lm.noMove)
         {
         boolean moved_legion = doMove(lm.markerId, lm.toHex);
         if (moved_legion)
         {
         // remove list of moves - we are done with this legion
         legit.remove();
         occupiedHexes.remove(lm.fromHex); // hex is now free
         occupiedHexes.add(lm.toHex);
         moved = true;
         }
         }
         }
         }

         Log.debug("done moving legions with no conflicts");
         **/

        // find initial optimum, assuming optimum will move a legion
        boolean conflicted = handleConflictedMoves(all_legionMoves, !moved,
                occupiedHexes);
    }

    // find optimimum move for a set of legion moves given by all_legionMoves.
    // optimimum move is the one that gives the maximum sum(expected value)
    // across the set of legions.  If the flag mustMove is set to true
    // then we add the constraint that at least one of these legions must
    // move.
    private boolean handleConflictedMoves(List all_legionMoves,
            boolean mustMove, TreeSet occupiedHexes)
    {
        if (mustMove)
        {
            Log.debug("Ack! Combined optimum has constraint that we must move.");
        }

        boolean moved = false;

        if (all_legionMoves.size() < 1)
        {
            Log.error("No moves available!  Probable hang!");
            return false;
        }

        // Assume these legions may share a best
        // move with another legion.  We optimize the sum of the 
        // expected value across all legions

        LegionIndex li = new LegionIndex(all_legionMoves);
        double best_value = Double.NEGATIVE_INFINITY;
        LegionIndex best_move = li;

        Log.debug("handleConflictedMoves");
        int iter = 0;

        setupTimer();

        legion_move_combination:
        while (li.hasNext())
        {
            iter++;
            if (timeIsUp)
            {
                if (iter >= 1000)
                {
                    Log.debug("handleVoluntaryMoves() time up after " + iter +
                            " iterations");
                    break;
                }
            }

            double value = 0;
            // Log.debug("Considering combined move number: " + iter);

            // Compute intial value of move
            for (int i = 0; i < all_legionMoves.size(); i++)
            {
                List legionMoves = (List)all_legionMoves.get(i);
                LegionBoardMove lm = (LegionBoardMove)legionMoves.get(
                        li.index(i));
                value += lm.val;
            }

            if (value <= best_value)
            {
                // We already have a better move than this
                continue;
            }

            // Check to see if move is valid
            Set hexes = new HashSet();
            boolean hasMove = false;
            boolean hasEmptyHexMove = false;

            for (int i = 0; i < all_legionMoves.size(); i++)
            {
                List legionMoves = (List)all_legionMoves.get(i);
                LegionBoardMove lm = (LegionBoardMove)legionMoves.get(
                        li.index(i));
                String hex = lm.toHex;
                // Log.debug(lm.markerId + " moves to " + lm.toHex);
                if (hexes.contains(hex))
                {
                    // if this combination has two legions moving
                    // to the same hex.  move on to the next feasible
                    // combination
                    li.set_next_index(i);
                    continue legion_move_combination;
                }

                if (!hasEmptyHexMove)
                {
                    if (!occupiedHexes.contains(hex) || lm.noMove)
                    {
                        hasEmptyHexMove = true;
                    }
                }

                hexes.add(hex);

                if (!lm.noMove)
                {
                    hasMove = true;
                }
            }
            // Log.debug("Combined move value: " + value);

            // apply constraint. at least one legion must move to
            // a place that is not currently occupied by our legions.
            // this prevents illegal exchanges of A moves to B and B moves to A
            if (!hasEmptyHexMove)
            {
                continue;
            }

            // apply constraint. if one of these legions must
            // move this set of moves is not allowed
            if (!hasMove && mustMove)
            {
                continue;
            }

            // Log.debug("New best move!");
            best_value = value;
            best_move = new LegionIndex(li);

        }
        Log.debug("Done computing best move.");

        // take the best_move and apply it.
        List bestMoves = new ArrayList();
        for (int i = 0; i < all_legionMoves.size(); i++)
        {
            List legionMoves = (List)all_legionMoves.get(i);
            LegionBoardMove lm = (LegionBoardMove)legionMoves.get(
                    best_move.index(i));
            LegionInfo legion = client.getLegionInfo(lm.markerId);
            Log.debug("Had conflict. Optimum move for " + lm.markerId +
                    " is " + lm.toHex);
            if (lm.noMove == false)
            {
                bestMoves.add(lm);
            }
        }

        // apply moves.  go to empty hexes first.
        // we have a max number of iterations here
        // since the set of moves may contain a cycle
        // that is not detected by the simple hasEmptyHexMove
        // flag.  In this case, there should be at least one
        // move to an empty hex which makes this a legal move set.
        int tries = 0;
        int MAX_TRIES = 20;
        while (bestMoves.size() > 0 && tries < MAX_TRIES)
        {
            ListIterator bm = bestMoves.listIterator();
            while (bm.hasNext())
            {
                LegionBoardMove lm = (LegionBoardMove)bm.next();

                // first make moves which are not going to hexes
                // currently occupied by our legions

                if (!occupiedHexes.contains(lm.toHex) ||
                        lm.fromHex.equals(lm.toHex) || tries > MAX_TRIES - 3)
                {
                    boolean moved_legion = doMove(lm.markerId, lm.toHex);
                    if (moved_legion)
                    {
                        Log.debug("Successfully moved? " + lm.markerId);
                        occupiedHexes.remove(lm.fromHex); // hex is now free
                        occupiedHexes.add(lm.toHex);
                        bm.remove(); // move has been made
                        moved = true;
                    }
                    else
                    {
                        Log.debug("Did not move? " + lm.markerId);
                    }
                }
            }
            tries++;
        }

        return moved;
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
            Iterator legionIt = client.getLegionsByPlayer(enemyPlayerName).iterator();

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
                            List list = (List)enemyMap[effectiveRoll].get(hexlabel);

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

    final int LOSE_LEGION = -10000;
    final int TITAN_SURVIVAL = 30; // safety margin for Titan

    // Compute risk of being attacked
    // Value returned is expected point value cost
    double hexRisk(LegionInfo legion, MasterHex hex,
            Map[] enemyAttackMap, Creature recruit)
    {

        double risk = 0.0;

        // ignore all fear of attack on first turn
        if (client.getTurnNumber() < 2)
        {
            return 0.0;
        }

        if (enemyAttackMap != null)
        {
            Log.debug("considering risk of " + legion + " in " + hex);

            int roll;

            int result = 0;

            roll_loop:
            for (roll = 1; roll <= 6; roll++)
            {
                // Log.debug("considering risk of roll " + roll);
                List enemies = (List)enemyAttackMap[roll].get(hex.getLabel());

                if (enemies == null)
                {
                    continue;
                }

                Iterator it = enemies.iterator();
                double worst_result_this_roll = 0.0;

                while (it.hasNext())
                {
                    LegionInfo enemy = (LegionInfo)it.next();
                    result = evaluateCombat(enemy, legion, hex, enemyAttackMap);

                    if (result > worst_result_this_roll)
                    {
                        worst_result_this_roll = result;
                    }
                }
                risk -= worst_result_this_roll;
            }

            risk /= 6.0;

        }

        Log.debug("compute final attack risk as " + risk);

        return risk;
    }

    int evaluateCombat(LegionInfo attacker, LegionInfo defender, MasterHex hex,
            Map[] enemyAttackMap)
    {
        if (attacker.getPlayerName().equals(defender.getPlayerName()))
        {
            return 0;
        }

        // Avoid using MIN_VALUE and MAX_VALUE because of possible overflow.
        final int WIN_GAME = Integer.MAX_VALUE / 10;
        final int LOSE_GAME = Integer.MIN_VALUE / 10;

        final int NO_ATTACK_BEFORE_TURN = 3;

        final int defenderPointValue = defender.getPointValue();
        final int attackerPointValue = attacker.getPointValue();
        final BattleResults result = estimateBattleResults(attacker, defender,
                hex);

        // Log.debug("evaluateCombat()");
        int value = (int)result.getExpectedValue();

        if (client.getTurnNumber() < NO_ATTACK_BEFORE_TURN)
        {
            return value - (int)5 * result.getAttackerDead();
        } // apply penalty to early attacks

        // Log.debug("Raw expected value " + value);
        boolean defenderTitan = defender.hasTitan();

        if (result.getExpectedValue() > 0)
        {
            if (attacker.hasTitan())
            {
                // unless we can win the game with this attack
                if (defenderTitan)
                {
                    if (client.getNumLivingPlayers() == 2 &&
                            (attackerPointValue - result.getAttackerDead()) >
                            TITAN_SURVIVAL)
                    {
                        // do it and win the game
                        value = 1000 + (int)result.getExpectedValue() * 1000;
                    }
                    else if (result.getAttackerDead() < attackerPointValue / 2 &&
                            (attackerPointValue - result.getAttackerDead()) >
                            TITAN_SURVIVAL)
                    {
                        // our titan stack will be badly damaged but it is worth it
                        value = 100 +
                                (int)result.getExpectedValue() * 100;
                    }
                    else
                    {
                        // ack! we'll fuck up our titan group
                        // use metric below so that if we have no choice but to attack
                        // we pick the least losing battle
                        value = (int)result.getAttackerDead() *
                                -100;
                    }
                }
                else if (result.getAttackerDead() > attackerPointValue / 2)
                // (1/4) will usually be about 3 pieces since titan
                // will be large part of value
                {
                    // ack! we'll fuck up our titan group
                    // use metric below so that if we have no choice but to attack
                    // we pick the least losing battle
                    value = -100 +
                            (int)result.getAttackerDead() *
                            -100;
                }
                else  // win with minimal loss
                {
                    // value = result.getExpectedValue(); // default value
                }
            }
            else if (defenderTitan)
            {
                value = (1000 + (int)result.getExpectedValue() * 1000) /
                        client.getNumLivingPlayers();
            }
        }
        else
        // we expect to lose on this battle.
        // but if the enemy is a titan stack it may be worth it
        {
            if (!attacker.hasTitan() && defenderTitan)
            {
                // gun for the titan stack if we can knock out
                // more than 80% of the value
                if (result.getDefenderDead() > defenderPointValue * .8)
                {
                    // value should be proportional to amount of Titan stack
                    // killed since we may be able to attack with more
                    // than one legion
                    value = result.getDefenderDead() * 100 /
                            client.getNumLivingPlayers() / 2;
                }
            }
            else if (attacker.hasTitan())
            {
                // ack! we'll kill our titan group
                // use metric below so that if we have no choice but to attack
                // we pick the least losing battle
                value = (-1000 + (int)result.getExpectedValue() * 1000) /
                        client.getNumLivingPlayers();
            }
        }

        // Log.debug("evaluateCombat() value after accounting for titans " + value);
        return value;
    }

    // evaluate the attack payoff of moving into the hex given by 'hex'.
    // This will typically be negative to indicate that we might lose,
    // zero if the hex is empty, or positive if the hex is occupied
    // by a weak legion
    private final int RECRUIT_FALSE = 0; // don't allow recruiting by attacker
    private final int RECRUIT_TRUE = 1; // allow recruiting by attacker
    private final int RECRUIT_AT_7 = 2; // allow recruiting by attacker 7 high

    int evaluateHexAttack(LegionInfo attacker, MasterHex hex, int canRecruitHere,
            Map[] enemyAttackMap)
    {

        // Log.debug("evaluateHexAttack()");
        int value = 0;
        // consider making an attack
        final String enemyMarkerId = client.getFirstEnemyLegion(hex.getLabel(),
                attacker.getPlayerName());

        if (enemyMarkerId != null)
        {
            // Log.debug("evaluateHexAttack(): found a legion in the target hex " + hex.getLabel());
            LegionInfo defender = client.getLegionInfo(enemyMarkerId);

            if (!attacker.getPlayerName().equals(defender.getPlayerName()))
            {
                value = evaluateCombat(attacker, defender, hex, enemyAttackMap);
            }

            if (I_HATE_HUMANS && !isHumanLegion(defender))
            {
                // try not to attack other AIs                
                if (value > -5000)
                {
                    value = -5000;
                }

                if (attacker.hasTitan())
                {
                    value -= 1000;
                }

                if (defender.hasTitan())
                {
                    value -= 1000;
                }
            }

            return value;
        }

        if ((canRecruitHere == RECRUIT_TRUE && attacker.getHeight() < 7) ||
                canRecruitHere == RECRUIT_AT_7)
        {
            if (!attacker.hasTitan())
            {
                value += recruitValue(attacker, hex.getLabel(), null,
                        hex.getTerrain());
            }
            else
            {
                // prefer recruiting with Titan legion
                value += recruitValue(attacker, hex.getLabel(), null,
                        hex.getTerrain()) *
                        1.1;
            }
        }

        return value;
    }

    // cheap, inaccurate evaluation function.  Returns am expected value for
    // moving this legion to this hex.  The value defines a distance
    // metric over the set of all possible moves.
    int evaluateMove(LegionInfo legion, MasterHex hex,
            int canRecruitHere, Map[] enemyAttackMap, int depth, boolean addHexRisk)
    {
        // evaluateHexAttack includes recruit value
        double value = evaluateHexAttack(legion, hex, canRecruitHere,
                enemyAttackMap);

        // Log.debug("depth " + depth + " evaluateHexAttack at hex " + hex.getLabel() + " : " + value);

        // if we get killed at this hex there can be no further musters
        if (value < 0)
        {
            return (int)value;
        }

        // for speed, at last depth ignore hex risk
        if (depth == 0)
        {
            return (int)value;
        }

        // consider what we might be able to recruit next turn, from here
        double nextTurnValue = 0.0;
        double stay_at_hex = 0.0;

        if (addHexRisk)
        {
            // value of staying at hex we move to
            // i.e. what is risk we will be attacked if we stay at this hex
            stay_at_hex = hexRisk(legion, hex, enemyAttackMap, null);

            // Log.debug("evaluateMove: hexRisk: " + stay_at_hex);

            // when we move to this hex we may get attacked and not have
            // a next turn
            value += stay_at_hex;

            // if we are very likely to be attacked and die here then just return value
            if (value < -10)
            {
                return (int)value;
            }

        }

        // squares that are further away are more likely to be blocked
        double DISC_FACTOR = 1.0;
        double discount = DISC_FACTOR;

        // value of next turn
        for (int roll = 1; roll <= 6; roll++)
        {
            Set moves = client.getMovement().listAllMoves(legion, hex, roll);
            double bestMoveVal = stay_at_hex; // can always stay here
            Iterator nextMoveIt = moves.iterator();

            while (nextMoveIt.hasNext())
            {
                String nextLabel = (String)nextMoveIt.next();
                MasterHex nextHex = MasterBoard.getHexByLabel(nextLabel);

                double nextMoveVal = evaluateMove(legion, nextHex,
                        RECRUIT_AT_7,
                        enemyAttackMap, depth - 1, false);

                if (nextMoveVal > bestMoveVal)
                {
                    bestMoveVal = nextMoveVal;
                }
            }
            bestMoveVal *= discount;
            nextTurnValue += bestMoveVal;
            discount *= DISC_FACTOR; // squares that are further away are more likely to be blocked
        }

        nextTurnValue /= 6.0;     // 1/6 chance of each happening
        value += 0.9 * nextTurnValue; // discount future moves some

        Log.debug("depth " + depth + " EVAL " + legion +
                (canRecruitHere != RECRUIT_FALSE ? " move to " : " stay in ") +
                hex + " = " + value);

        return (int)value;
    }

    private boolean isHumanLegion(LegionInfo legion)
    {
        String name = legion.getPlayerInfo().getName();

        // XXX
        // BAD! way to guess if player is an AI.
        // Look at the name, and if it is a standard color
        // assume this is an AI.
        // Better would be to ask the server via Player.isHuman() but this
        // would require updating the server socket protocol
        for (int i = 0; i < Constants.colorNames.length; i++)
        {
            if (Constants.colorNames[i].equals(name))
            {
                return false;
            }
        }

        return true;

    }

    private class BattleResults
    {
        private double ev; // expected value of attack
        private int att_dead;
        private int def_dead;

        public BattleResults(double e, int a, int d)
        {
            ev = e;
            att_dead = a;
            def_dead = d;
        }

        public double getExpectedValue()
        {
            return ev;
        }

        public int getAttackerDead()
        {
            return att_dead;
        }

        public int getDefenderDead()
        {
            return def_dead;
        }

    }

    private BattleResults estimateBattleResults(LegionInfo attacker,
            LegionInfo defender, MasterHex hex)
    {
        return estimateBattleResults(attacker, defender, hex, null);
    }

    // add in value of points received for killing group / 100
    // * (fraction of angel value = angel + arch = 24 + 12/5 
    //    + titan value = 10 -- arbitrary, it's worth more than 4)
    final double KILLPOINTS = (24 + 12 / 5 + 10) / 100;

    private BattleResults estimateBattleResults(LegionInfo attacker,
            LegionInfo defender,
            MasterHex hex, Creature recruit)
    {
        if (I_HATE_HUMANS &&
                !isHumanLegion(attacker) && !isHumanLegion(defender))
        {
            // assume no risk that AIs will attack each other
            return new BattleResults(0, 0, 0);
        }
        String terrain = hex.getTerrain();

        // Log.debug("estimateBattleResults() in terrain " + terrain);

        // double attackerPointValue = attacker.getPointValue();
        // double defenderPointValue = defender.getPointValue();

        // Get list of PowerSkill creatures
        List attackerCreatures = getCombatList(attacker, terrain, false);
        List defenderCreatures = getCombatList(defender, terrain, true);
        ListIterator a;

        // Debug print group contents
        //        
        // Log.debug("starting creatures");
        // Log.debug("attacker PowerSkill list: ");

        /*
         a = attackerCreatures.listIterator();
         while (a.hasNext()) 
         {
         PowerSkill psa = (PowerSkill)a.next();
         Log.debug(psa.getName() + ": Pow: " + 
         psa.getPowerAttack() + "/" + psa.getPowerDefend() +
         " Skill: " + psa.getSkillAttack() + "/" + psa.getSkillDefend()
         + " HP: " + psa.getHP());
         }
         Log.debug(" ");
         Log.debug("defender PowerSkill list - excluding titans with power < 9: ");
         a = defenderCreatures.listIterator();
         while (a.hasNext()) 
         {
         PowerSkill psa = (PowerSkill)a.next();
         Log.debug(psa.getName() + ": Pow: " + 
         psa.getPowerAttack() + "/" + psa.getPowerDefend() +
         " Skill: " + psa.getSkillAttack() + "/" + psa.getSkillDefend()
         + " HP: " + psa.getHP());
         }
         */


        // simulate combat
        int attackerKilled = 0;
        int defenderKilled = 0;
        int defenderMuster = 0;
        int round;
        boolean summonedAngel = false;

        round_loop:
        for (round = 0; round < 7; round++)
        {
            // DO NOT ADD ANGEL!
            // If attacker cannot win without angel then this
            // will often leave a weak group with an angel in it
            // that is a scooby snack.  Also, this makes the AI
            // too aggressive about attacking and too conservative
            // about moving and mustering
            if (I_HATE_HUMANS)
            {
                // angel call
                if (!summonedAngel && defenderKilled > 0)
                {
                    // String summonAngel = summonAngel(attacker.getMarkerId());
                    // if (summonAngel != null)
                    // {
                    PowerSkill angel = new PowerSkill("Angel", 6, 4);
                    defenderCreatures.add(angel);
                    summonedAngel = true;
                    // }
                }
            }

            // 4th round muster
            if (round == 4 && defenderCreatures.size() > 1)
            {
                // add in enemy's most likely turn 4 recruit
                List recruits = client.findEligibleRecruits(defender.getMarkerId(),
                        hex.getLabel());

                if (!recruits.isEmpty())
                {
                    Creature bestRecruit = (Creature)recruits.get(recruits.size() -
                            1);

                    defenderMuster = ghrv(bestRecruit, defender,
                            hintSectionUsed);
                    defenderCreatures.add(getNativeValue(bestRecruit, terrain,
                            true));
                }
            }

            for (int move = 0; move < 2; move++)
            {
                if (attackerCreatures.size() == 0)
                {
                    break round_loop;
                }

                if (defenderCreatures.size() == 0)
                {
                    break round_loop;
                }

                for (int phase = 0; phase < 2; phase++)
                {
                    // mutual attacks
                    a = attackerCreatures.listIterator();
                    ListIterator d = defenderCreatures.listIterator();
                    double rollover_power = 0;
                    int attack_skill = 3;
                    for (int attacks = 0; attacks <
                            Math.max(attackerCreatures.size(),
                            defenderCreatures.size()); attacks++)
                    {
                        // if we have reached the end of the attackers, roll-over
                        if (!a.hasNext())
                        {
                            if (phase == 0)
                            { // done attacker strikes
                                break;
                            }
                            a = attackerCreatures.listIterator();
                        }

                        if (!d.hasNext())
                        {
                            if (phase == 1)
                            { // done defender strikes
                                break;
                            }
                            d = defenderCreatures.listIterator();
                        }

                        int defend_skill;
                        int attack_power;
                        PowerSkill psa;
                        PowerSkill psd;

                        if (phase == 0)
                        {
                            psa = (PowerSkill)a.next();
                            psd = (PowerSkill)d.next();
                        }
                        else
                        {
                            psa = (PowerSkill)d.next();
                            psd = (PowerSkill)a.next();
                        }

                        double current_carry = 0;

                        defend_skill = psd.getSkillDefend();

                        if (rollover_power > 0)
                        {
                            rollover_power = Math.max(rollover_power -
                                    psd.getPowerDefend(),
                                    0);
                            // N.B. use old attack skill & new defend skill
                            current_carry = rollover_power *
                                    Math.min(attack_skill - defend_skill + 3, 6) /
                                    6.0;
                        }
                        attack_skill = psa.getSkillAttack();

                        // when computing attacking power, subtract any dice lost due to native defence
                        attack_power = psa.getPowerAttack() -
                                psd.getPowerDefend();

                        double attack_prob = Math.min(Math.max(attack_skill -
                                defend_skill + 3,
                                1),
                                6) /
                                6.0;
                        double expected_damage = attack_prob * attack_power +
                                current_carry;

                        psd.addDamage(expected_damage);

                        if (psd.getHP() < 0) // damage exceeded that needed to kill
                        {
                            rollover_power = -1.0 * psd.getHP();
                            rollover_power /= attack_prob; // remaining power
                            psd.setHP(0.0);
                        }
                        else
                        {
                            rollover_power = 0;
                        }

                        // update defender in list
                        if (phase == 0)
                        {
                            d.set(psd);
                        }
                        else
                        {
                            a.set(psd);
                        }

                    }
                }

                // remove dead bodies
                a = attackerCreatures.listIterator();
                while (a.hasNext())
                {
                    PowerSkill psa = (PowerSkill)a.next();
                    if (psa.getHP() <= 0)
                    {
                        attackerKilled += psa.getPointValue();
                        a.remove();
                    }
                }

                a = defenderCreatures.listIterator();
                while (a.hasNext())
                {
                    PowerSkill psa = (PowerSkill)a.next();
                    if (psa.getHP() <= 0)
                    {
                        defenderKilled += psa.getPointValue();
                        a.remove();
                    }
                }

                /* 
                 * Debug print group contents
                 *
                 Log.debug(" ");
                 Log.debug("after round " + round + " move " + move);
                 Log.debug("attacker PowerSkill list: ");
                 a = attackerCreatures.listIterator();
                 while (a.hasNext()) 
                 {
                 PowerSkill psa = (PowerSkill)a.next();
                 Log.debug(psa.getName() + ": Pow: " + 
                 psa.getPowerAttack() + "/" + psa.getPowerDefend() +
                 " Skill: " + psa.getSkillAttack() + "/" + psa.getSkillDefend()
                 + " HP: " + psa.getHP());
                 }
                 Log.debug("defender PowerSkill list: ");
                 a = defenderCreatures.listIterator();
                 while (a.hasNext()) 
                 {
                 PowerSkill psa = (PowerSkill)a.next();
                 Log.debug(psa.getName() + ": Pow: " + 
                 psa.getPowerAttack() + "/" + psa.getPowerDefend() +
                 " Skill: " + psa.getSkillAttack() + "/" + psa.getSkillDefend()
                 + " HP: " + psa.getHP());
                 }
                 */
            }
        }

        // add in attackers final recruit  
        double attackerMuster = 0;
        if (attackerCreatures.size() > 2)
        {
            // add in attacker's most likely recruit
            List recruits = client.findEligibleRecruits(attacker.getMarkerId(),
                    hex.getLabel());

            if (!recruits.isEmpty())
            {
                Creature bestRecruit = (Creature)recruits.get(recruits.size() -
                        1);
                attackerMuster = bestRecruit.getPointValue();
            }
        }

        // add in defender's final recruit, if the combat lasted < 4 rounds
        if (round < 4 && defenderCreatures.size() > 1)
        {
            // add in enemy's most likely turn 4 recruit
            List recruits = client.findEligibleRecruits(defender.getMarkerId(),
                    hex.getLabel());

            if (!recruits.isEmpty())
            {
                Creature bestRecruit = (Creature)recruits.get(recruits.size() -
                        1);

                defenderMuster = ghrv(bestRecruit, defender, hintSectionUsed);
            }
        }

        // tally results
        // divide value of enemy killed legions by # of other players
        int numOtherPlayers = client.getNumLivingPlayers() - 1;

        if (I_HATE_HUMANS)
        {
            numOtherPlayers = 1;
        }

        double expectedValue;

        expectedValue = defenderKilled / numOtherPlayers - attackerKilled +
                attackerMuster - defenderMuster / numOtherPlayers;

        if (attackerCreatures.size() > 1)
        {
            double pointsValue = defenderKilled * KILLPOINTS;
            // Log.debug("attacker won. points value: "+ pointsValue);
            expectedValue += pointsValue;
        }

        if (defenderCreatures.size() > 1)
        {
            double pointsValue = defenderKilled * KILLPOINTS;
            // Log.debug("attacker won. points value: "+ pointsValue);
            expectedValue -= pointsValue / numOtherPlayers;
        }

        // Log.debug("attacker killed "+ attackerKilled);
        // Log.debug("defender killed " + defenderKilled);
        // Log.debug("attacker muster " + attackerMuster);
        // Log.debug("defender muster " + defenderMuster);
        // Log.debug("expected value " + expectedValue);

        return new BattleResults(expectedValue, attackerKilled,
                defenderKilled - defenderMuster);
    }

    private int[] getNumberAndPVForBestNextTurnRecruitment(LegionInfo legion,
            MasterHex hex,
            Creature base)
    {
        int mun = TerrainRecruitLoader.getRecruitGraph().getMaximumUsefulNumber(base.getName());
        int already = legion.numCreature(base);
        int maxPV = -1;
        int num = 0;

        for (int i = 1; i <= mun; i++)
        {
            List all = TerrainRecruitLoader.getRecruitGraph().getAllTerrainsWhereThisNumberOfCreatureRecruit(base.getName(),
                    i);

            Iterator it = all.iterator();

            while (it.hasNext())
            {
                String terrain = (String)it.next();

                String dest = TerrainRecruitLoader.getRecruitGraph().getRecruitFromRecruiterTerrainNumber(base.getName(),
                        terrain, i);

                if ((dest != null) &&
                        (getNumberOfWaysToTerrain(legion, hex, terrain) > 0))
                {
                    Creature cdest = Creature.getCreatureByName(dest);

                    if (legion.numCreature(cdest) == 0)
                    {
                        // don't bother if we already have next stage
                        // this will not catch if we already have
                        // the after-next stage (i.e. it will try to take
                        // a third Cyclops over a Gorgon even if we have a
                        // Serpent, as long as we don't have a Behemoth)
                        int pv = ghrv(cdest, legion, hintSectionUsed);

                        if (pv > maxPV)
                        {
                            maxPV = pv;
                            num = i;
                        }
                    }
                }
            }
        }

        int[] r = new int[2];

        r[0] = num;
        r[1] = maxPV;

        return r;
    }

    private Creature getBestRecruitmentOneTurnAhead(LegionInfo legion,
            MasterHex hex,
            List recruits)
    {
        Creature recruit = (Creature)recruits.get(recruits.size() - 1);
        String basic = recruit.getName();
        int[] r = getNumberAndPVForBestNextTurnRecruitment(legion, hex, recruit);

        // say the best we can do ATM is either what we can recruit next
        // turn, or the value of the recruit itself;
        int maxPV = ((r[0] == (1 + legion.numCreature(recruit))) ?
                r[1] :
                ghrv(recruit, legion, hintSectionUsed));

        Iterator it = recruits.iterator();

        while (it.hasNext())
        {
            Creature base = (Creature)it.next();

            r = getNumberAndPVForBestNextTurnRecruitment(legion, hex, base);

            if ((r[0] == (1 + legion.numCreature(base))) && (r[1] > maxPV) &&
                    (base != recruit))
            {
                // by recruiting one more "base", we could have a better
                // recruit next turn than what our best this turn could do.
                // So we recruit. Example: instead of recruiting a 18pts
                // Gorgon we'll get a third Cyclops if the 24pts Behemoth
                // is in range. Ditto for the third Lion/Troll if we can
                // reach a Griffon/Wywern (20/21pts) instead of a 16pts
                // ranger. OTOH, in a variant were the ranger can recruit,
                // we might take the ranger anyway (it its recruit is
                // better than a Griffon or Wywern).
                recruit = base;
                maxPV = r[1];
            }
        }

        if (!(basic.equals(recruit.getName())))
        {
            Log.debug("GRAPH: (" + hex.getLabel() +
                    ") OneTurnAhead suggest recruiting " + recruit.getName() +
                    " instead of " + basic +
                    " because we can get better next turn");
        }

        return recruit;
    }

    private Creature getBestRecruitmentInfinityAhead(LegionInfo legion,
            MasterHex hex,
            List recruits)
    {
        ListIterator it = recruits.listIterator(recruits.size());
        Creature best = null;
        String basic = ((Creature)recruits.get(recruits.size() - 1)).getName();
        int maxVP = -1;

        while (it.hasPrevious())
        {
            Creature recruit = (Creature)it.previous();

            String temp = TerrainRecruitLoader.getRecruitGraph().getBestPossibleRecruitEver(recruit.getName(),
                    legion);

            int vp = ghrv(Creature.getCreatureByName(temp), legion,
                    hintSectionUsed);

            if (vp > maxVP)
            {
                maxVP = vp; /* vp of recruit */
                best = recruit; /* best is recruit_er_ */
            }
        }

        if (!(basic.equals(best.getName())))
        {
            Log.debug("GRAPH: (" + hex.getLabel() +
                    ") InfinityAhead suggest recruiting " + best.getName() +
                    " instead of " + basic +
                    " because it has the best creature in its tree");
        }

        return best;
    }

    /* this next one is fairly dumb, as a single hex will be counted
     multiple times if there's an enemy stack on it - so it will
     suggest taking an Ogre over a Troll if there's someone sitting
     on a nearby Hills... */
    private Creature getBestRecruitmentPlacesNextTurn(LegionInfo legion,
            MasterHex hex,
            List recruits)
    {
        ListIterator it = recruits.listIterator(recruits.size());
        Creature best = null;
        String basic = ((Creature)recruits.get(recruits.size() - 1)).getName();
        int maxwnum = 0;

        while (it.hasPrevious())
        {
            Creature recruit = (Creature)it.previous();

            int rnum = legion.numCreature(recruit);
            java.util.List tl = TerrainRecruitLoader.getRecruitGraph().getAllTerrainsWhereThisNumberOfCreatureRecruit(recruit.getName(),
                    rnum + 1);
            int wnum = getNumberOfWaysToTerrains(legion, hex, tl);

            if (wnum > maxwnum)
            {
                best = recruit;
                maxwnum = wnum;
            }
        }
        if (best == null)
        {
            best = (Creature)recruits.get(recruits.size() - 1);
        }
        if (!(basic.equals(best.getName())))
        {
            Log.debug("GRAPH: (" + hex.getLabel() +
                    ") PlacesNextTurn suggest recruiting " + best.getName() +
                    " instead of " + basic +
                    " because it recruits in the greater number of places (" +
                    maxwnum + ")");
        }
        return best;
    }

    private class SimpleAIOracle implements
                net.sf.colossus.server.HintOracleInterface
    {
        LegionInfo legion;
        MasterHex hex;
        List recruits;
        Map[] enemyAttackMap = null;

        SimpleAIOracle(LegionInfo legion,
                MasterHex hex,
                List recruits)
        {
            this.legion = legion;
            this.hex = hex;
            this.recruits = recruits;

        }

        public boolean canReach(String terrain)
        {
            int now = getNumberOfWaysToTerrain(legion, hex, terrain);

            // Log.debug("ORACLE: now is " + now +
            // " ( from hex " + hex.getLabel() + " to a " + terrain + ")");
            return (now > 0);
        }

        public int creatureAvailable(String name)
        {
            int count = client.getCreatureCount(Creature.getCreatureByName(name));

            // Log.debug("ORACLE: count is " + count);
            return count;
        }

        public boolean otherFriendlyStackHasCreature(java.util.List allNames)
        {
            java.util.List all = client.getFriendlyLegions(client.getPlayerName());

            Iterator it = all.iterator();

            while (it.hasNext())
            {
                String markerId = (String)it.next();

                if (!(legion.getMarkerId().equals(markerId)))
                {
                    LegionInfo other = client.getLegionInfo(markerId);
                    boolean hasAll = true;

                    Iterator it2 = allNames.iterator();

                    while (it2.hasNext() && hasAll)
                    {
                        String name = (String)it2.next();

                        if (other.numCreature(name) <= 0)
                        {
                            hasAll = false;
                        }
                    }
                    if (hasAll)
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean hasCreature(String name)
        {
            int num = legion.numCreature(name);

            // Log.debug("ORACLE: num is " + num);
            return (num > 0);
        }

        public boolean canRecruit(String name)
        {
            boolean contains = recruits.contains(Creature.getCreatureByName(name));

            // Log.debug("ORACLE: contains is " + false);
            return contains;
        }

        public int stackHeight()
        {
            return legion.getHeight();
        }

        public String hexLabel()
        {
            return hex.getLabel();
        }

        public int biggestAttackerHeight()
        {
            if (enemyAttackMap == null)
            {
                enemyAttackMap = buildEnemyAttackMap(client.getPlayerInfo());
            }
            int worst = 0;

            for (int i = 1; i < 6; i++)
            {
                List enemyList = (List)enemyAttackMap[i].get(legion.getHexLabel());

                if (enemyList != null)
                {
                    Iterator it = enemyList.iterator();

                    while (it.hasNext())
                    {
                        LegionInfo enemy = (LegionInfo)it.next();

                        if (enemy.getHeight() > worst)
                        {
                            worst = enemy.getHeight();
                        }
                    }
                }
            }
            return worst;
        }
    }

    Creature getVariantRecruitHint(LegionInfo legion,
            MasterHex hex,
            List recruits)
    {
        String recruitName = VariantSupport.getRecruitHint(hex.getTerrain(),
                legion, recruits, new SimpleAIOracle(legion, hex, recruits),
                hintSectionUsed);

        if (recruitName == null)
        {
            // Log.debug("HINT: \"null\" found for " + hex.getLabel() + "...");
            return (Creature)recruits.get(recruits.size() - 1);
        }
        if ((recruitName.equals("nothing")) || (recruitName.equals("Nothing")))
        { // suggest recruiting nothing
            return null;
        }

        Creature recruit = Creature.getCreatureByName(recruitName);
        String basic = ((Creature)recruits.get(recruits.size() - 1)).getName();

        if (!(recruits.contains(recruit)))
        {
            Log.warn("HINT: Invalid Hint for this variant ! (can't recruit " +
                    recruitName + ")");
            return ((Creature)recruits.get(recruits.size() - 1));
        }

        /*
         if (!(basic.equals(recruit.getName())))
         {
         Log.debug("HINT: (" + hex.getLabel() +
         ") variant hint suggest recruiting " +
         recruitName + " instead of " + basic);
         }
         */
        return recruit;
    }

    private int getNumberOfWaysToTerrain(LegionInfo legion,
            MasterHex hex, String terrainType)
    {
        int total = 0;

        for (int roll = 1; roll <= 6; roll++)
        {
            Set set = client.getMovement().listAllMoves(legion, hex, roll, true);

            if (setContainsHexWithTerrain(set, terrainType))
            {
                total++;
            }
        }
        return total;
    }

    private int getNumberOfWaysToTerrains(LegionInfo legion,
            MasterHex hex, java.util.List tl)
    {
        Iterator it = tl.iterator();
        int total = 0;

        while (it.hasNext())
        {
            String terrain = (String)it.next();

            total += getNumberOfWaysToTerrain(legion, hex, terrain);
        }

        return total;
    }

    private static boolean setContainsHexWithTerrain(Set set,
            String terrainType)
    {
        Iterator it = set.iterator();

        while (it.hasNext())
        {
            String hexLabel = (String)it.next();
            String terrain = MasterBoard.getHexByLabel(hexLabel).getTerrain();

            if (terrain.equals(terrainType))
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
        // points while they may be used against us this turn.
        // Fight battles with angels first, so that those angels
        // can be summoned out.
        // Try not to lose potential angels and recruits by having
        // scooby snacks flee to 7-high stacks (or 6-high stacks
        // that could recruit later this turn) and push them
        // over 100-point boundaries.

        String playerName = client.getActivePlayerName();
        LegionInfo attacker = client.getLegionInfo(client.getFirstFriendlyLegion(hexLabel,
                playerName));
        LegionInfo defender = client.getLegionInfo(client.getFirstEnemyLegion(hexLabel,
                playerName));
        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
        int value = 0;

        BattleResults br = estimateBattleResults(attacker, defender, hex);
        final int result = (int)br.getExpectedValue();

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

            if (((currentScore + fleeValue) /
                    TerrainRecruitLoader.getAcquirableRecruitmentsValue()) >
                    (currentScore /
                    TerrainRecruitLoader.getAcquirableRecruitmentsValue()))
            {
                if (attacker.getHeight() == 7 ||
                        attacker.getHeight() == 6 && attacker.canRecruit())
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
        if (br.getAttackerDead() <= attacker.getPointValue() * 2 / 7 &&
                attacker.hasSummonable())
        {
            value += 5;
        }

        return value;
    }

    public boolean flee(LegionInfo legion, LegionInfo enemy)
    {
        if (legion.hasTitan())
        {
            return false;
        } // Titan never flee !

        BattleResults br = estimateBattleResults(enemy, legion,
                legion.getCurrentHex());
        int result = (int)br.getExpectedValue();

        // For the first four turns never flee
        // Make attacker pay to minimize their future mustering
        // capability
        if (client.getTurnNumber() < 5)
        {
            return false;
        }

        if (result < 0)
        {
            // defender wins
            return false;
        }

        // defender loses but might not flee if

        if (enemy.hasTitan())
        {
            if (br.getAttackerDead() > enemy.getPointValue() * 2 / 7)
            {
                // attacker loses at least 2 significant pieces from Titan stack
                return false;
            }
        }

        // find attacker's most likely recruit
        double attackerMuster = 0;
        List recruits = client.findEligibleRecruits(enemy.getMarkerId(),
                legion.getCurrentHex().getLabel());

        if (!recruits.isEmpty())
        {
            Creature bestRecruit = (Creature)recruits.get(recruits.size() - 1);
            attackerMuster = bestRecruit.getPointValue();
        }

        if (br.getAttackerDead() < enemy.getPointValue() * 2 / 7 &&
                enemy.getHeight() >= 6)
        {
            int currentScore = enemy.getPlayerInfo().getScore();
            int pointValue = legion.getPointValue();
            boolean canAcquireAngel = ((currentScore + pointValue) /
                    TerrainRecruitLoader.getAcquirableRecruitmentsValue() >
                    (currentScore /
                    TerrainRecruitLoader.getAcquirableRecruitmentsValue()));

            // flee to deny a muster
            if (canAcquireAngel || attackerMuster > 0)
            {
                return true;
            }
        }

        if (enemy.getHeight() >= 6)
        {
            attackerMuster = 0;
        }

        if (br.getExpectedValue() >
                (legion.getPointValue() / 2) * KILLPOINTS + attackerMuster)
        {
            // more valuable not to concede
            return false;
        }

        // flee
        return true;
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
        String terrain = legion.getCurrentHex().getTerrain();
        int height = enemy.getHeight();

        BattleResults br = estimateBattleResults(legion, enemy,
                legion.getCurrentHex());

        if (br.getDefenderDead() < enemy.getPointValue() * 2 / 7 && height >= 6)
        {
            int currentScore = enemy.getPlayerInfo().getScore();
            int pointValue = legion.getPointValue();
            boolean canAcquireAngel = ((currentScore + pointValue) /
                    TerrainRecruitLoader.getAcquirableRecruitmentsValue() >
                    (currentScore /
                    TerrainRecruitLoader.getAcquirableRecruitmentsValue()));
            // Can't use Legion.getRecruit() because it checks for
            // 7-high legions.
            boolean canRecruit = !client.findEligibleRecruits(enemy.getMarkerId(), enemy.getHexLabel()).isEmpty();

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
    public String acquireAngel(String markerId, List angels)
    {
        // TODO If the legion is a tiny scooby snack that's about to get
        // smooshed, turn down the angel.

        Creature bestAngel = getBestCreature(angels);

        if (bestAngel == null)
        {
            return null;
        }

        // Don't take an angel if 6 high and a better recruit is available.
        // TODO Make this also work for post-battle reinforcements
        LegionInfo legion = client.getLegionInfo(markerId);

        if (legion.getHeight() == 6 && legion.canRecruit())
        {
            List recruits = client.findEligibleRecruits(markerId,
                    legion.getHexLabel());
            Creature bestRecruit = (Creature)recruits.get(recruits.size() - 1);

            if (getKillValue(bestRecruit) > getKillValue(bestAngel))
            {
                Log.debug("AI declines acquiring to recruit " +
                        bestRecruit.getName());
                return null;
            }
        }
        return bestAngel.getName();
    }

    /** Return the most important Creature in the list of Creatures or
     * creature name strings.. */
    Creature getBestCreature(List creatures)
    {
        if (creatures == null || creatures.isEmpty())
        {
            return null;
        }
        Creature best = null;
        Iterator it = creatures.iterator();

        while (it.hasNext())
        {
            Object ob = it.next();
            Creature creature = null;

            if (ob instanceof Creature)
            {
                creature = (Creature)ob;
            }
            else if (ob instanceof String)
            {
                creature = Creature.getCreatureByName((String)ob);
            }
            if (best == null || getKillValue(creature) > getKillValue(best))
            {
                best = creature;
            }
        }
        return best;
    }

    /** Return a string of form angeltype:donorId, or null. */
    public String summonAngel(String summonerId)
    {
        // Always summon the biggest possible angel, from the least
        // important legion that has one.
        //
        // TODO Sometimes leave room for recruiting.

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
        String terrain = client.getBattleTerrain();

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
                        getKillValue(target, terrain) >
                        getKillValue(bestTarget, terrain))
                {
                    bestTarget = target;
                    canKillSomething = true;
                }
            }
            else
            {
                // We probably can't kill this target.
                if (bestTarget == null ||
                        (!canKillSomething &&
                        getKillValue(target, terrain) >
                        getKillValue(bestTarget, terrain)))
                {
                    bestTarget = target;
                }
            }
        }
        return bestTarget;
    }

    // TODO Have this actually find the best one, not the first one.
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
        String terrain = client.getBattleTerrain();
        BattleChit bestTarget = null;

        Iterator it = carryTargets.iterator();

        while (it.hasNext())
        {
            String desc = (String)it.next();
            String targetHexLabel = desc.substring(desc.length() - 2);
            BattleChit target = client.getBattleChit(targetHexLabel);

            if (target.wouldDieFrom(carryDamage))
            {
                if (bestTarget == null || !bestTarget.wouldDieFrom(carryDamage) ||
                        getKillValue(target, terrain) >
                        getKillValue(bestTarget, terrain))
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
    public boolean strike(LegionInfo legion)
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

    static int getCombatValue(BattleChit chit, String terrain)
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

    class PowerSkill
    {
        private String name;
        private int power_attack;
        private int power_defend; // how many dice attackers lose
        private int skill_attack;
        private int skill_defend;
        private double hp;  // how many hit points or power left
        private double value;

        public PowerSkill(String nm, int p, int pa, int pd, int sa, int sd)
        {
            name = nm;
            power_attack = pa;
            power_defend = pd;
            skill_attack = sa;
            skill_defend = sd;
            hp = p; // may not be the same as power_attack!
            value = p * Math.min(sa, sd);
        }

        public PowerSkill(String nm, int pa, int sa)
        {
            name = nm;
            power_attack = pa;
            power_defend = 0;
            skill_attack = sa;
            skill_defend = sa;
            hp = pa;
            value = pa * sa;
        }

        public int getPowerAttack()
        {
            return power_attack;
        }

        public int getPowerDefend()
        {
            return power_defend;
        }

        public int getSkillAttack()
        {
            return skill_attack;
        }

        public int getSkillDefend()
        {
            return skill_defend;
        }

        public double getHP()
        {
            return hp;
        }

        public void setHP(double h)
        {
            hp = h;
        }

        public void addDamage(double d)
        {
            hp -= d;
        }

        public double getPointValue()
        {
            return value;
        }

        public String getName()
        {
            return name;
        }
    }

    // return power and skill of a given creature given the terrain
    private PowerSkill getNativeValue(Creature creature, String terrain, boolean defender)
    {

        /*
         *Not sure what to do about fliers and rangestrikers
         *
         if (creature.isFlier())
         {
         val++;
         }

         if (creature.isRangestriker())
         {
         val++;
         }
         */

        int power = creature.getPower();
        int skill = creature.getSkill();

        if (MasterHex.isNativeCombatBonus(creature, terrain) ||
                (terrain.equals("Tower") && defender == true))
        {
            // list of terrain bonuses
            // format is
            // "Terrain", "power_attack_bonus",  "power_defend_bonus",
            // "skill_attack_bonus", "skill_defend_bonus"
            // where the bonuses are versus a non-native creature

            int terrains = 7;

            String[][] allTerrains = { {"Plains", "0", "0", "0", "0"}, // strike down wall, defender strike up
                {"Tower", "0", "0", "1", "1"}, // natvie in bramble has skill to hit increased by 1
                {"Brush", "0", "0", "0", "1"}, {"Jungle", "0", "0", "0", "1"}, // native gets an extra die when attack down hill
                // non-native loses 1 skill when attacking up hill
                {"Hills", "1", "0", "0", "1"}, // native gets an extra 2 die when attack down hill
                // non-native loses 1 die  when attacking up hill
                {"Desert", "2", "1", "0", "0"}, // Native gets extra 1 die when attack down mountain
                // non-native loses 1 skill  when attacking up hill
                {"Mountains", "1", "0", "0", "1"} // the other types have only movement bonuses
            };

            int POWER_ATT = 1;
            int POWER_DEF = 2;
            int SKILL_ATT = 3;
            int SKILL_DEF = 4;

            for (int i = 0; i < terrains; i++)
            {
                if (terrain.equals(allTerrains[i][0]))
                {
                    if (terrain.equals("Tower") && defender == false)
                    {
                        // no attacker bonus for tower
                        return new PowerSkill(creature.getName(), power, skill);
                    }
                    else if (terrain.equals("Mountains") && defender == true &&
                            creature.getName().equals("Dragon"))
                    {
                        // Dragon gets an extra 3 die when attack down mountain
                        // non-native loses 1 skill  when attacking up hill
                        return new PowerSkill(
                                creature.getName(),
                                power,
                                power + 3,
                                Integer.parseInt(allTerrains[i][POWER_DEF]),
                                skill +
                                Integer.parseInt(allTerrains[i][SKILL_ATT]),
                                skill +
                                Integer.parseInt(allTerrains[i][SKILL_DEF]));
                    }
                    else
                    {
                        return new PowerSkill(
                                creature.getName(),
                                power,
                                power +
                                Integer.parseInt(allTerrains[i][POWER_ATT]),
                                Integer.parseInt(allTerrains[i][POWER_DEF]),
                                skill +
                                Integer.parseInt(allTerrains[i][SKILL_ATT]),
                                skill +
                                Integer.parseInt(allTerrains[i][SKILL_DEF]));
                    }
                }
            }
        }

        // no special bonus found
        return new PowerSkill(creature.getName(), power, skill);
    }

    public List getCombatList(LegionInfo legion, String terrain, boolean defender)
    {
        // Log.debug("getCombatList()");
        List powerskills = new ArrayList();
        Iterator it = legion.getContents().iterator();

        // Log.debug("getCombatList(): iterate through legion contents");
        while (it.hasNext())
        {
            String name = (String)it.next();

            // Log.debug("getCombatList(): add " + name);
            if (name.startsWith(Constants.titan))
            {
                PowerSkill ps;
                int titanPower = legion.getPlayerInfo().getTitanPower();

                // Assume that Titans 
                // take only a minimal part in the combat.
                // Here we have to include them in the list
                // of creatures so that the AI knows to jump
                // titan singletons
                ps = new PowerSkill(
                        "Titan",
                        Math.max(titanPower - 5, 1),
                        Creature.getCreatureByName("Titan").getSkill());

                /* Including titans only at power 9 or above is a bad idea
                 * It makes these stacks think they are invulnerable and
                 * it becomes too easy to jump them.
                 
                 if (titanPower < 9) 
                 {
                 ps = new PowerSkill(
                 "Titan",
                 1,
                 Creature.getCreatureByName("Titan").getSkill());
                 }
                 else if (terrain.equals("Tower")) 
                 {
                 ps = new PowerSkill(
                 "Titan",
                 titanPower,
                 Creature.getCreatureByName("Titan").getSkill()+1);
                 }
                 else
                 {
                 ps = new PowerSkill(
                 "Titan",
                 titanPower,
                 Creature.getCreatureByName("Titan").getSkill());
                 }
                 **/

                powerskills.add(ps);

            }
            else
            {
                Creature creature = Creature.getCreatureByName(name);
                PowerSkill ps = getNativeValue(creature, terrain, defender);
                powerskills.add(ps);
            }
        }

        return powerskills;
    }

    static int getKillValue(Creature creature)
    {
        return getKillValue(creature, null);
    }

    // XXX titan power
    static int getKillValue(BattleChit chit, String terrain)
    {
        return getKillValue(chit.getCreature(), terrain);
    }

    static int getKillValue(Creature creature, String terrain)
    {
        int val = 10 * creature.getPointValue();

        if (creature.getSkill() >= 4)
        {
            val += 2;
        }
        if (creature.getSkill() <= 2)
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
        if (terrain != null && MasterHex.isNativeCombatBonus(creature, terrain))
        {
            val += 3;
        }
        if (creature.isTitan())
        {
            val += 1000;
        }
        return val;
    }

    // //////////////////////////////////////////////////////////////
    // Battle move stuff
    // //////////////////////////////////////////////////////////////

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


    /** Return a list of critter moves, in best move order. */
    public java.util.List battleMove()
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

        return bestMoveOrder;
    }

    /** Try another move for creatures whose moves failed. */
    public void retryFailedBattleMoves(List bestMoveOrder)
    {
        if (bestMoveOrder == null)
        {
            return;
        }
        Iterator it = bestMoveOrder.iterator();

        while (it.hasNext())
        {
            CritterMove cm = (CritterMove)it.next();
            BattleChit critter = cm.getCritter();
            String startingHexLabel = cm.getStartingHexLabel();
            String endingHexLabel = cm.getEndingHexLabel();

            Log.debug(critter.getDescription() + " failed to move");
            List moveList = findBattleMovesOneCritter(critter);

            if (!moveList.isEmpty())
            {
                CritterMove cm2 = (CritterMove)moveList.get(0);

                Log.debug("Moving " + critter.getDescription() + " to " +
                        cm2.getEndingHexLabel() + " (startingHexLabel was " +
                        startingHexLabel + ")");
                client.tryBattleMove(cm2);
            }
        }
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
                Log.warn("Time is up figuring move order, but we ignore it " +
                        "(buggy break)");
                timeIsUp = false;
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
            List moveList = findBattleMovesOneCritter(critter);

            // Add this critter's moves to the list.
            allCritterMoves.add(moveList);

            // Put all critters back where they started.
            Iterator it2 = allCritterMoves.iterator();

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

    private List findBattleMovesOneCritter(BattleChit critter)
    {
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

            CritterMove cm = new CritterMove(critter, currentHexLabel, hexLabel);

            // Need to move the critter to evaluate.
            critter.moveToHex(hexLabel);

            // Compute and save the value for each CritterMove.
            cm.setValue(evaluateCritterMove(critter, null));
            moveList.add(cm);
            // Move the critter back where it started.
            critter.moveToHex(critter.getStartingHexLabel());
        }

        // Sort critter moves in descending order of score.
        Collections.sort(moveList, new Comparator()
        {
            public int compare(Object o1, Object o2)
            {
                CritterMove cm1 = (CritterMove)o1;
                CritterMove cm2 = (CritterMove)o2;

                return cm2.getValue() - cm1.getValue();
            }
        }
        );

        // Show the moves considered.
        StringBuffer buf = new StringBuffer("Considered " + moveList.size() +
                " moves for " + critter.getTag() + " " +
                critter.getCreatureName() + " in " + currentHexLabel + ":");

        it2 = moveList.iterator();
        while (it2.hasNext())
        {
            CritterMove cm = (CritterMove)it2.next();

            buf.append(" " + cm.getEndingHexLabel());
        }
        Log.debug(buf.toString());

        return moveList;
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

    private final static int MIN_ITERATIONS = 50;

    /** Evaluate all legion moves in the list, and return the best one.
     *  Break out early if the time limit is exceeded. */
    LegionMove findBestLegionMove(List legionMoves)
    {
        int bestScore = Integer.MIN_VALUE;
        LegionMove best = null;

        Collections.shuffle(legionMoves, random);

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
                if (count >= MIN_ITERATIONS)
                {
                    Log.debug("findBestLegionMove() time up after " + count +
                            " iterations");
                    break;
                }
                else
                {
                    Log.debug("findBestLegionMove() time up after " + count +
                            " iterations, but we keep searching until " +
                            MIN_ITERATIONS);
                }
            }
        }
        Log.debug("Best legion move: " +
                ((best == null) ? "none " : best.toString()) + " (" + bestScore +
                ")");
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

        while (trimCritterMoves(critterMoves))
        {
        }  // Just trimming

        // Now that the list is as small as possible, start finding combos.
        List legionMoves = new ArrayList();
        final int limit = getCreatureMoveLimit();
        int[] indexes = new int[critterMoves.size()];

        nestForLoop(indexes, indexes.length - 1, critterMoves, legionMoves);

        Log.debug("Got " + legionMoves.size() + " legion moves");
        return legionMoves;
    }

    private Set duplicateHexChecker = new HashSet();

    private void nestForLoop(int[] indexes, final int level, final
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

            LegionMove lm = makeLegionMove(indexes, critterMoves);

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

    private LegionMove makeLegionMove(int[] indexes, List critterMoves)
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
        int PENALTY_DAMAGE_TERRAIN = -200;
        int FIRST_RANGESTRIKE_TARGET = 300;
        int EXTRA_RANGESTRIKE_TARGET = 100;
        int RANGESTRIKE_TITAN = 500;
        int RANGESTRIKE_WITHOUT_PENALTY = 100;
        int ATTACKER_ADJACENT_TO_ENEMY = 400;
        int DEFENDER_ADJACENT_TO_ENEMY = 100; // -20;
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
        int ADJACENT_TO_BUDDY = 150;
        int ADJACENT_TO_BUDDY_TITAN = 300;
        int GANG_UP_ON_CREATURE = 50;
    }

    /** Return a map of target hex label to number of friendly creatures that can strike it */
    private Map findStrikeMap()
    {
        Map map = new HashMap();

        Iterator it = client.getActiveBattleChits().iterator();

        while (it.hasNext())
        {
            BattleChit critter = (BattleChit)it.next();
            Set targets = client.findStrikes(critter.getTag());
            Iterator it2 = targets.iterator();

            while (it2.hasNext())
            {
                String hexLabel = (String)it2.next();
                Integer old = (Integer)map.get(hexLabel);

                if (old == null)
                {
                    map.put(hexLabel, new Integer(1));
                }
                else
                {
                    map.put(hexLabel, new Integer(old.intValue() + 1));
                }
            }
        }
        return map;
    }

    /** strikeMap is optional */
    private int evaluateCritterMove(BattleChit critter, Map strikeMap)
    {
        final String terrain = client.getBattleTerrain();
        final String masterHexLabel = client.getBattleSite();
        final LegionInfo legion = client.getLegionInfo(client.getMyEngagedMarkerId());
        final LegionInfo enemy = client.getLegionInfo(client.getBattleInactiveMarkerId());
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
        else if (hex.isNativeBonusTerrain() &&
                critter.getCreature().isNativeTerrain(hex.getTerrain()))
        {
            value += bec.NATIVE_BONUS_TERRAIN;
            // We want marsh natives to slightly prefer moving to bog hexes,
            // even though there's no real bonus there, to leave other hexes
            // clear for non-native allies.
            if (hex.getTerrain().equals("Bog"))
            {
                value += bec.NATIVE_BOG;
            }
        }
        else  // Critter is not native or the terrain is not beneficial
        {
            if (hex.isNonNativePenaltyTerrain() &&
                    (!critter.getCreature().isNativeTerrain(hex.getTerrain())))
            {
                value += bec.NON_NATIVE_PENALTY_TERRAIN;
            }
        }

        /* damage is positive, healing is negative, so we can always add */
        value += bec.PENALTY_DAMAGE_TERRAIN *
                hex.damageToCreature(critter.getCreature());

        Set targetHexLabels = client.findStrikes(critter.getTag());
        int numTargets = targetHexLabels.size();

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

                    // Reward ganging up on enemies.
                    if (strikeMap != null)
                    {
                        int numAttackingThisTarget = ((Integer)strikeMap.get(hexLabel)).intValue();

                        if (numAttackingThisTarget > 1)
                        {
                            value += bec.GANG_UP_ON_CREATURE;
                        }
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

                    // Reward ganging up on enemies.
                    if (strikeMap != null)
                    {
                        int numAttackingThisTarget = ((Integer)strikeMap.get(hexLabel)).intValue();

                        if (numAttackingThisTarget > 1)
                        {
                            value += bec.GANG_UP_ON_CREATURE;
                        }
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
                if (legion.getMarkerId().equals(client.getDefenderMarkerId()) ||
                        critter.isTitan() || turn <= 4)
                {
                    if (hitsExpected + hits >= power)
                    {
                        if (legion.getMarkerId().equals(client.getAttackerMarkerId()))
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
                        if (legion.getMarkerId().equals(client.getAttackerMarkerId()))
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
            if (HexMap.terrainIsTower(terrain) &&
                    legion.getMarkerId().equals(client.getDefenderMarkerId()))
            {
                // Stick to the center of the tower.
                value += bec.TITAN_TOWER_HEIGHT_BONUS * hex.getElevation();
            }
            else
            {
                if (turn <= 4)
                {
                    value += bec.TITAN_FORWARD_EARLY_PENALTY *
                            Strike.getRange(hex, entrance, true);
                    for (int i = 0; i < 6; i++)
                    {
                        BattleHex neighbor = hex.getNeighbor(i);

                        if (neighbor == null ||
                                neighbor.getTerrain().equals("Tree"))
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
                int range = Strike.getRange(hex, entrance, true);

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
                    BattleChit other = client.getBattleChit(neighbor.getLabel());

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

        Map strikeMap = findStrikeMap();

        // Then find the sum of all critter evals.
        int sum = 0;

        it = lm.getCritterMoves().iterator();
        while (it.hasNext())
        {
            CritterMove cm = (CritterMove)it.next();

            sum += evaluateCritterMove(cm.getCritter(), strikeMap);
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

    private int ghrv(Creature creature, LegionInfo legion)
    {
        if (!creature.isTitan())
        {
            return creature.getHintedRecruitmentValue();
        }
        PlayerInfo player = legion.getPlayerInfo();
        int power = player.getTitanPower();
        int skill = creature.getSkill();

        return power * skill *
                VariantSupport.getHintedRecruitmentValueOffset(creature.getName());
    }

    private int ghrv(Creature creature, LegionInfo legion, String[] section)
    {
        if (!creature.isTitan())
        {
            return creature.getHintedRecruitmentValue(section);
        }
        PlayerInfo player = legion.getPlayerInfo();
        int power = player.getTitanPower();
        int skill = creature.getSkill();

        return power * skill *
                VariantSupport.getHintedRecruitmentValueOffset(creature.getName(),
                section);
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
