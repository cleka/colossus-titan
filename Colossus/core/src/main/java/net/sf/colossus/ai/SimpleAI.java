package net.sf.colossus.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import net.sf.colossus.ai.helper.LegionMove;
import net.sf.colossus.client.Client;
import net.sf.colossus.client.CritterMove;
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.client.PlayerClientSide;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.Dice;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.game.SummonInfo;
import net.sf.colossus.util.Glob;
import net.sf.colossus.util.InstanceTracker;
import net.sf.colossus.util.PermutationIterator;
import net.sf.colossus.util.Probs;
import net.sf.colossus.util.ValueRecorder;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardTerrain;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 * Simple implementation of a Titan AI
 *
 * TODO somehow we call client.getOwningPlayer() a lot -- there should probably be a better
 * link between AI and player, after all the AI either IS_A player or PLAYS_FOR a player
 *
 *
 * @author Bruce Sherrod, David Ripton
 * @author Romain Dolbeau
 */
public class SimpleAI extends AbstractAI
{
    private static final Logger LOGGER = Logger.getLogger(SimpleAI.class
        .getName());

    /**
     * Stores the skill and power bonuses for a single terrain.
     *
     * For internal use only, so we don't bother with encapsulation.
     */
    private static class TerrainBonuses
    {
        final int attackerPower;
        final int defenderPower;
        final int attackerSkill;
        final int defenderSkill;

        TerrainBonuses(int attackerPower, int defenderPower,
            int attackerSkill, int defenderSkill)
        {
            this.attackerPower = attackerPower;
            this.defenderPower = defenderPower;
            this.attackerSkill = attackerSkill;
            this.defenderSkill = defenderSkill;
        }
    }

    /**
     * Maps the terrain names to their matching bonuses.
     *
     * Only the terrains that have bonuses are in this map, so
     * users have to expect to retrieve null values. Note that
     * the terrain names include names for master board and
     * hazard terrains, so it can be used for lookup up either
     * type.
     *
     * TODO there seems to be some overlap with
     * {@link HazardTerrain#isNativeBonusTerrain()} and
     * {@link HazardTerrain#isNonNativePenaltyTerrain()}.
     *
     * This is a Map<String,TerrainBonuses>.
     *
     * TODO: this shouldn't be here, this is a property of the Variant player
     * (well, not yet for Hazard, but it should be, someday).
     * Actually, this doesn't make sense to me (RD). tower has bonus for
     * both attacker & defender (because of walls, I assume), but is special-
     * cased for attacker & defender. Brush and Jungle assumes Brushes, but
     * Jungle has Tree, too. And the comments themselves makes clear that
     * 'Sand' is actually 'Slope', but mixing up Attacker & Native and Defender
     * & non-native.
     * This and calcBonus should be reviewed thoroughly.
     */
    private static final Map<String, TerrainBonuses> TERRAIN_BONUSES = new HashMap<String, TerrainBonuses>();
    static
    {
        // strike down wall, defender strike up
        TERRAIN_BONUSES.put("Tower", new TerrainBonuses(0, 0, 1, 1));
        // native in bramble has skill to hit increased by 1
        TERRAIN_BONUSES.put("Brush", new TerrainBonuses(0, 0, 0, 1));
        TERRAIN_BONUSES.put("Jungle", new TerrainBonuses(0, 0, 0, 1));
        TERRAIN_BONUSES.put("Brambles", new TerrainBonuses(0, 0, 0, 1));
        // native gets an extra die when attack down slope
        // non-native loses 1 skill when attacking up slope
        TERRAIN_BONUSES.put("Hills", new TerrainBonuses(1, 0, 0, 1));
        // native gets an extra 2 dice when attack down dune
        // non-native loses 1 die when attacking up dune
        TERRAIN_BONUSES.put("Desert", new TerrainBonuses(2, 1, 0, 0));
        TERRAIN_BONUSES.put("Sand", new TerrainBonuses(2, 1, 0, 0));
        // Native gets extra 1 die when attack down slope
        // non-native loses 1 skill when attacking up slope
        TERRAIN_BONUSES.put("Mountains", new TerrainBonuses(1, 0, 0, 1));
        TERRAIN_BONUSES.put("Volcano", new TerrainBonuses(1, 0, 0, 1));
        // the other types have only movement bonuses
    }

    protected int timeLimit = Constants.DEFAULT_AI_TIME_LIMIT; // in s
    boolean timeIsUp;
    private int splitsDone = 0;
    private int splitsAcked = 0;
    private List<String> remainingMarkers = null;

    public SimpleAI(Client client)
    {
        super(client);
        // small bonus to the 'kill value' of a creature when there's some
        // terrain bonus to combat.
        cvc.HAS_NATIVE_COMBAT_BONUS = 3;
        // initialize the creature info needed by the AI
        InstanceTracker.register(this, client.getOwningPlayer().getName());
    }

    public PlayerColor pickColor(List<PlayerColor> colors,
        List<PlayerColor> favoriteColors)
    {
        for (PlayerColor preferredColor : favoriteColors)
        {
            if (colors.contains(preferredColor))
            {
                return preferredColor;
            }
        }
        // Can't have one of our favorites, so take what's there.
        for (PlayerColor color : colors)
        {
            return color;
        }
        return null;
    }

    /* prepare a list of markers, first those with preferred color,
     * then all the others, but inside these groups shuffled.
     * Caller can then always just take next to get a "random" marker.
     * @param markerIds list of available markers to prepare
     * @param preferredShortColor thos with this color first
     * @returns list of markers
     */
    private List<String> prepareMarkers(Set<String> markerIds,
        String preferredShortColor)
    {
        List<String> myMarkerIds = new ArrayList<String>();
        List<String> otherMarkerIds = new ArrayList<String>();
        List<String> allMarkerIds = new ArrayList<String>();

        // split between own / other
        for (String markerId : markerIds)
        {
            if (preferredShortColor != null
                && markerId.startsWith(preferredShortColor))
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
            allMarkerIds.addAll(myMarkerIds);
        }

        if (!(otherMarkerIds.isEmpty()))
        {
            Collections.shuffle(otherMarkerIds, random);
            allMarkerIds.addAll(otherMarkerIds);
        }
        return allMarkerIds;
    }

    public String pickMarker(Set<String> markerIds, String preferredShortColor)
    {
        List<String> myMarkerIds = new ArrayList<String>();
        List<String> otherMarkerIds = new ArrayList<String>();
        // split between own / other
        for (String markerId : markerIds)
        {
            if (preferredShortColor != null
                && markerId.startsWith(preferredShortColor))
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
            return myMarkerIds.get(0);
        }

        if (!(otherMarkerIds.isEmpty()))
        {
            Collections.shuffle(otherMarkerIds, random);
            return otherMarkerIds.get(0);
        }
        return null;
    }

    public void muster()
    {
        client.resetRecruitReservations();

        // Do not recruit if this legion is a scooby snack.
        double scoobySnackFactor = 0.15;
        int minimumSizeToRecruit = (int)(scoobySnackFactor * client
            .getGameClientSide().getAverageLegionPointValue());
        for (LegionClientSide legion : client.getOwningPlayer().getLegions())
        {
            if (client.canRecruit(legion)
                && (legion.hasTitan() || legion.getPointValue() >= minimumSizeToRecruit))
            {
                CreatureType recruit = chooseRecruit(legion,
                    legion.getCurrentHex(), true);
                if (recruit != null)
                {
                    List<String> recruiters = client.findEligibleRecruiters(
                        legion, recruit);

                    String recruiterName = null;
                    if (!recruiters.isEmpty())
                    {
                        // Just take the first one.
                        recruiterName = recruiters.get(0);
                    }
                    client.doRecruit(legion, recruit.getName(), recruiterName);
                    client.reserveRecruit(recruit);
                }
            }
        }
        client.resetRecruitReservations();
    }

    public void reinforce(Legion legion)
    {
        CreatureType recruit = chooseRecruit(((LegionClientSide)legion),
            legion.getCurrentHex(), false);
        String recruitName = null;
        String recruiterName = null;
        if (recruit != null)
        {
            recruitName = recruit.getName();
            List<String> recruiters = client.findEligibleRecruiters(legion,
                recruit);
            if (!recruiters.isEmpty())
            {
                recruiterName = recruiters.get(0);
            }
        }
        // Call regardless to advance past recruiting.
        client.doRecruit(legion, recruitName, recruiterName);
    }

    CreatureType chooseRecruit(LegionClientSide legion, MasterHex hex,
        boolean considerReservations)
    {
        List<CreatureType> recruits = client.findEligibleRecruits(legion, hex,
            considerReservations);
        if (recruits.size() == 0)
        {
            return null;
        }

        CreatureType recruit = getVariantRecruitHint(legion, hex, recruits);

        /* use the hinted value as the actual recruit */
        return recruit;
    }

    public boolean split()
    {
        Player player = client.getOwningPlayer();
        remainingMarkers = prepareMarkers(player.getMarkersAvailable(),
            player.getShortColor());

        splitsDone = 0;
        splitsAcked = 0;
        for (Legion legion : player.getLegions())
        {
            if (remainingMarkers.isEmpty())
            {
                break;
            }
            splitOneLegion(player, legion);
        }
        remainingMarkers.clear();
        remainingMarkers = null;

        // if we did splits, don't signal to client that it's done;
        // because it would do doneWithSplits immediately;
        // instead the last didSplit callback will do doneWithSplits
        // (This is done to avoid the advancePhase illegally messages)
        return splitsDone <= 0;
    }

    /** Unused in this AI; just return true to indicate done. */
    public boolean splitCallback(Legion parent, Legion child)
    {
        splitsAcked++;
        return splitsAcked >= splitsDone;
    }

    private void splitOneLegion(Player player, Legion legion)
    {
        if (legion.getHeight() < 7)
        {
            return;
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
            for (int roll = 1; roll <= 6; roll++)
            {
                Set<MasterHex> moves = client.getMovement().listAllMoves(
                    legion, legion.getCurrentHex(), roll);
                int safeMoves = 0;
                for (MasterHex hex : moves)
                {
                    if (client.getGameClientSide()
                        .getEnemyLegions(hex, player).size() == 0)
                    {
                        safeMoves++;
                        if (!goodRecruit && couldRecruitUp(legion, hex, null))
                        {
                            goodRecruit = true;
                        }
                    }
                    else
                    {
                        Legion enemy = client.getGameClientSide()
                            .getFirstEnemyLegion(hex, player);
                        int result = estimateBattleResults(legion, true,
                            enemy, hex);

                        if (result == WIN_WITH_MINIMAL_LOSSES)
                        {
                            LOGGER
                                .finest("We can safely split AND attack with "
                                    + legion);
                            safeMoves++;

                            // Also consider acquiring angel.
                            if (!goodRecruit
                                && couldRecruitUp(legion, hex, enemy))
                            {
                                goodRecruit = true;
                            }
                        }

                        int result2 = estimateBattleResults(legion, false,
                            enemy, hex);

                        if (result2 == WIN_WITH_MINIMAL_LOSSES
                            && result != WIN_WITH_MINIMAL_LOSSES && roll <= 4)
                        {
                            // don't split so that we can attack!
                            LOGGER.finest("Not splitting " + legion
                                + " because we want the muscle to attack");

                            forcedToAttack = 999;
                            return;
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
                    return;
                }
            }
            if (!goodRecruit)
            {
                // No point in splitting, since we can't improve.
                LOGGER.finest("Not splitting " + legion
                    + " because it can't improve from here");
                return;
            }
        }

        if (remainingMarkers.isEmpty())
        {
            LOGGER.finest("Not splitting " + legion
                + " because no markers available");
            return;
        }

        String newMarkerId = remainingMarkers.get(0);
        remainingMarkers.remove(0);

        List<CreatureType> creatures = chooseCreaturesToSplitOut(legion);
        // increment BEFORE calling client
        // (instead of: return true and caller increments).
        // Otherwise we might have a race situation, if callback is quicker
        // than caller incrementing the splitsDone value...
        this.splitsDone++;
        client.sendDoSplitToServer(legion, newMarkerId, creatures);
    }

    /** Decide how to split this legion, and return a list of
     *  Creatures to remove.  */
    private List<CreatureType> chooseCreaturesToSplitOut(Legion legion)
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
            return doInitialGameSplit(legion.getCurrentHex());
        }

        return findWeakestTwoCritters((LegionClientSide)legion);
    }

    /**
     * Find the two weakest creatures in a legion according to
     * @link {@link #chooseCreaturesToSplitOut(Legion)}
     *
     * @param legion
     * @return List containing the CreatureTypes of the two weakest critters
     */
    List<CreatureType> findWeakestTwoCritters(LegionClientSide legion)
    {
        CreatureType weakest1 = null;
        CreatureType weakest2 = null;

        for (CreatureType critter : legion.getCreatureTypes())
        {
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
            else if (getHintedRecruitmentValue(critter, legion,
                hintSectionUsed) < getHintedRecruitmentValue(weakest1, legion,
                hintSectionUsed))
            {
                weakest1 = critter;
            }
            else if (getHintedRecruitmentValue(critter, legion,
                hintSectionUsed) < getHintedRecruitmentValue(weakest2, legion,
                hintSectionUsed))
            {
                weakest2 = critter;
            }
            else if (getHintedRecruitmentValue(critter, legion,
                hintSectionUsed) == getHintedRecruitmentValue(weakest1,
                legion, hintSectionUsed)
                && getHintedRecruitmentValue(critter, legion, hintSectionUsed) == getHintedRecruitmentValue(
                    weakest2, legion, hintSectionUsed))
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

        List<CreatureType> weakestTwoCritters = new ArrayList<CreatureType>();

        weakestTwoCritters.add(weakest1);
        weakestTwoCritters.add(weakest2);

        return weakestTwoCritters;
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
    // but two player games are messed up.
    //

    /** Return a list of exactly four creatures (including one lord) to
     *  split out. */
    List<CreatureType> doInitialGameSplit(MasterHex hex)
    {
        List<CreatureType> hintSuggestedSplit = getInitialSplitHint(hex);

        /* Log.debug("HINT: suggest splitting " + hintSuggestedSplit +
         " in " + label); */

        if (!((hintSuggestedSplit == null) || (hintSuggestedSplit.size() != 4)))
        {
            return hintSuggestedSplit;
        }

        CreatureType[] startCre = TerrainRecruitLoader
            .getStartingCreatures(hex);
        // in CMU style splitting, we split centaurs in even towers,
        // ogres in odd towers.
        final boolean oddTower = "100".equals(hex.getLabel())
            || "300".equals(hex.getLabel()) || "500".equals(hex.getLabel());
        final CreatureType splitCreature = oddTower ? startCre[2]
            : startCre[0];
        final CreatureType nonsplitCreature = oddTower ? startCre[0]
            : startCre[2];

        // XXX Hardcoded to default board.
        // don't split gargoyles in tower 3 or 6 (because of the extra jungles)
        if ("300".equals(hex.getLabel()) || "600".equals(hex.getLabel()))
        {
            return CMUsplit(false, splitCreature, nonsplitCreature, hex);
        }
        //
        // new idea due to David Ripton: split gargoyles in tower 2 or
        // 5, because on a 5 we can get to brush and jungle and get 2
        // gargoyles.  I'm not sure if this is really better than recruiting
        // a cyclops and leaving the other group in the tower, but it's
        // interesting so we'll try it.
        //
        else if ("200".equals(hex.getLabel()) || "500".equals(hex.getLabel()))
        {
            return MITsplit(true, splitCreature, nonsplitCreature, hex);
        }
        //
        // otherwise, mix it up for fun
        else
        {
            if (Dice.rollDie() <= 3)
            {
                return MITsplit(true, splitCreature, nonsplitCreature, hex);
            }
            else
            {
                return CMUsplit(true, splitCreature, nonsplitCreature, hex);
            }
        }
    }

    /** Keep the gargoyles together. */
    private List<CreatureType> CMUsplit(boolean favorTitan,
        CreatureType splitCreature, CreatureType nonsplitCreature,
        MasterHex hex)
    {
        CreatureType[] startCre = TerrainRecruitLoader
            .getStartingCreatures(hex);
        List<CreatureType> splitoffs = new LinkedList<CreatureType>();

        if (favorTitan)
        {
            if (Dice.rollDie() <= 3)
            {
                splitoffs.add(variant.getCreatureByName(Constants.titan));
                splitoffs.add(startCre[1]);
                splitoffs.add(startCre[1]);
                splitoffs.add(splitCreature);
            }
            else
            {
                splitoffs.add(variant.getCreatureByName(variant
                    .getPrimaryAcquirable()));
                splitoffs.add(nonsplitCreature);
                splitoffs.add(nonsplitCreature);
                splitoffs.add(splitCreature);
            }
        }
        else
        {
            if (Dice.rollDie() <= 3)
            {
                splitoffs.add(variant.getCreatureByName(Constants.titan));
            }
            else
            {
                splitoffs.add(variant.getCreatureByName(variant
                    .getPrimaryAcquirable()));
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
    private List<CreatureType> MITsplit(boolean favorTitan,
        CreatureType splitCreature, CreatureType nonsplitCreature,
        MasterHex hex)
    {
        CreatureType[] startCre = TerrainRecruitLoader
            .getStartingCreatures(hex);
        List<CreatureType> splitoffs = new LinkedList<CreatureType>();

        if (favorTitan)
        {
            if (Dice.rollDie() <= 3)
            {
                splitoffs.add(variant.getCreatureByName(Constants.titan));
                splitoffs.add(nonsplitCreature);
                splitoffs.add(nonsplitCreature);
                splitoffs.add(startCre[1]);
            }
            else
            {
                splitoffs.add(variant.getCreatureByName(variant
                    .getPrimaryAcquirable()));
                splitoffs.add(splitCreature);
                splitoffs.add(splitCreature);
                splitoffs.add(startCre[1]);
            }
        }
        else
        {
            if (Dice.rollDie() <= 3)
            {
                splitoffs.add(variant.getCreatureByName(Constants.titan));
            }
            else
            {
                splitoffs.add(variant.getCreatureByName(variant
                    .getPrimaryAcquirable()));
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

    /** Do a masterboard move (or consider taking mulligan, if feasible).
     *
     *  Returns true if we need to run this method again after the server
     *  updates the client with the results of a move or mulligan.
     */
    public boolean masterMove()
    {
        boolean moved = false;

        PlayerClientSide player = client.getOwningPlayer();

        // consider mulligans
        if (handleMulligans(player))
        {
            return true;
        }

        /** cache all places enemies can move to, for use in risk analysis. */
        Map<MasterHex, List<Legion>>[] enemyAttackMap = buildEnemyAttackMap(player);

        // A mapping from Legion to List of MoveInfo objects,
        // listing all moves that we've evaluated.  We use this if
        // we're forced to move.
        Map<Legion, List<MoveInfo>> moveMap = new HashMap<Legion, List<MoveInfo>>();

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
        if (!player.hasMoved())
        {
            moved = handleForcedSingleMove(player, moveMap);
            // Earlier here was a comment:
            // "always need to retry" and hardcoded returned true.
            // In [ 1748718 ] Game halt in Abyssal9 this lead to a deadlock;
            // - so, if here is returned "false" as for "I won't do any more
            // move", that problem does not occur (server recognizes that
            // there is no legal move and accepts it)
            // -- does this cause negative side effects elsewhere??
            // Let's try ;-)

            return moved;
        }
        return false;
    }

    /**
     * Take a mulligan if roll is 2 or 5 in first turn, and can still take
     * a mulligan.
     * Returns true if AI took a mulligan, false otherwise.
     */
    boolean handleMulligans(Player player)
    {
        // TODO: This is really stupid.  Do something smart here.
        if (client.getTurnNumber() == 1
            && player.getMulligansLeft() > 0
            && (client.getGame().getMovementRoll() == 2 || client.getGame()
                .getMovementRoll() == 5) && !client.tookMulligan())
        {
            client.mulligan();
            // TODO Need to wait for new movement roll.
            return true;
        }
        return false;
    }

    /** Return true if we moved something. */
    private boolean handleVoluntaryMoves(PlayerClientSide player,
        Map<Legion, List<MoveInfo>> moveMap,
        Map<MasterHex, List<Legion>>[] enemyAttackMap)
    {
        boolean moved = false;
        // TODO this is still List<LegionClientSide> to get the Comparable
        // -> use a Comparator instead since we are the only ones needing this
        List<LegionClientSide> legions = player.getLegions();

        // Sort markerIds in descending order of legion importance.
        Collections.sort(legions, Legion.ORDER_TITAN_THEN_POINTS);

        for (LegionClientSide legion : legions)
        {
            if (legion.hasMoved() || legion.getCurrentHex() == null)
            {
                continue;
            }

            // compute the value of sitting still
            List<MoveInfo> moveList = new ArrayList<MoveInfo>();
            moveMap.put(legion, moveList);

            ValueRecorder why = new ValueRecorder();
            MoveInfo sitStillMove = new MoveInfo(legion, null, evaluateMove(
                legion, legion.getCurrentHex(), false, enemyAttackMap, why),
                0, why);
            moveList.add(sitStillMove);

            // find the best move (1-ply search)
            MasterHex bestHex = null;
            MoveInfo bestMove = null;
            int bestValue = Integer.MIN_VALUE;
            Set<MasterHex> set = client.getMovement().listAllMoves(legion,
                legion.getCurrentHex(), client.getGame().getMovementRoll());

            for (MasterHex hex : set)
            {
                // TODO
                // Do not consider moves onto hexes where we already have a
                // legion. This is sub-optimal since the legion in this hex
                // may be able to move and "get out of the way"
                if (client.getGameClientSide().getFriendlyLegions(hex, player)
                    .size() > 0)
                {
                    continue;
                }
                ValueRecorder whyR = new ValueRecorder();
                final int value = evaluateMove(legion, hex, true,
                    enemyAttackMap, whyR);

                MoveInfo move = new MoveInfo(legion, hex, value, value
                    - sitStillMove.value, whyR);
                if (value > bestValue || bestHex == null)
                {
                    bestValue = value;
                    bestHex = hex;
                    bestMove = move;
                }
                moveList.add(move);
            }

            // if we found a move that's better than sitting still, move
            if (bestValue > sitStillMove.value && bestHex != null)
            {
                moved = doMove(legion, bestHex);
                if (moved)
                {
                    if (bestMove != null)
                    {
                        LOGGER.finer("Moved " + legion + " to " + bestHex
                            + " after evaluating: " + bestMove.why.toString()
                            + " is better than sitting tight: "
                            + sitStillMove.why.toString());
                    }
                    else
                    {
                        LOGGER.warning("Moved " + legion + " to " + bestHex
                            + " after evaluating: "
                            + " BEST MOVE IS NULL! RUN FOR COVER! "
                            + " is better than sitting tight: "
                            + sitStillMove.why.toString());
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /** Return true if we moved something. */
    private boolean handleForcedSplitMoves(Player player,
        Map<Legion, List<MoveInfo>> moveMap)
    {
        for (Legion legion : player.getLegions())
        {
            List<Legion> friendlyLegions = client.getGameClientSide()
                .getFriendlyLegions(legion.getCurrentHex(), player);

            if (friendlyLegions.size() > 1
                && !client
                    .getMovement()
                    .listNormalMoves(legion, legion.getCurrentHex(),
                        client.getGame().getMovementRoll()).isEmpty())
            {
                // Pick the legion in this hex whose best move has the
                // least difference with its sitStillValue, scaled by
                // the point value of the legion, and force it to move.
                LOGGER.finest("Ack! forced to move a split group");

                // first, concatenate all the moves for all the
                // legions that are here, and sort them by their
                // difference from sitting still multiplied by
                // the value of the legion.
                List<MoveInfo> allmoves = new ArrayList<MoveInfo>();
                for (Legion friendlyLegion : friendlyLegions)
                {
                    List<MoveInfo> moves = moveMap.get(friendlyLegion);
                    if (moves != null)
                    {
                        allmoves.addAll(moves);
                    }
                }

                Collections.sort(allmoves, new Comparator<MoveInfo>()
                {
                    public int compare(MoveInfo m1, MoveInfo m2)
                    {
                        return m2.difference * (m2.legion).getPointValue()
                            - m1.difference * (m1.legion).getPointValue();
                    }
                });

                // now, one at a time, try applying moves until we
                // have handled our split problem.
                for (MoveInfo move : allmoves)
                {
                    if (move.hex == null)
                    {
                        continue; // skip the sitStill moves
                    }
                    LOGGER.finest("forced to move split legion " + move.legion
                        + " to " + move.hex + " taking penalty "
                        + move.difference
                        + " in order to handle illegal legion " + legion);

                    boolean moved = doMove(move.legion, move.hex);
                    if (moved)
                    {
                        LOGGER.finer("Moved " + move.legion + " to "
                            + move.hex + " after evaluating: "
                            + move.why.toString()
                            + " as we couldn't sit tight");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean handleForcedSingleMove(Player player,
        Map<Legion, List<MoveInfo>> moveMap)
    {
        LOGGER.finest("Ack! forced to move someone");

        // Pick the legion whose best move has the least
        // difference with its sitStillValue, scaled by the
        // point value of the legion, and force it to move.

        // first, concatenate all the moves all legions, and
        // sort them by their difference from sitting still

        List<MoveInfo> allmoves = new ArrayList<MoveInfo>();
        for (Legion friendlyLegion : player.getLegions())
        {
            List<MoveInfo> moves = moveMap.get(friendlyLegion);
            if (moves != null)
            {
                allmoves.addAll(moves);
            }
        }

        Collections.sort(allmoves, new Comparator<MoveInfo>()
        {
            public int compare(MoveInfo m1, MoveInfo m2)
            {
                return m2.difference * (m2.legion).getPointValue()
                    - m1.difference * (m1.legion).getPointValue();
            }
        });

        // now, one at a time, try applying moves until we have moved a legion
        for (MoveInfo move : allmoves)
        {
            if (move.hex == null)
            {
                continue; // skip the sitStill moves
            }

            LOGGER.finest("forced to move " + move.legion + " to " + move.hex
                + " taking penalty " + move.difference
                + " in order to handle illegal legion " + move.legion);

            boolean moved = doMove(move.legion, move.hex);
            if (moved)
            {
                LOGGER.finer("Moved " + move.legion + " to " + move.hex
                    + " after evaluating: " + move.why.toString()
                    + " as we must move somebody.");
                return true;
            }
        }

        LOGGER.warning("handleForcedSingleMove() didn't move anyone - "
            + "probably no legion can move?? "
            + "(('see [ 1748718 ] Game halt in Abyssal9'))");
        // Let's hope the server sees it the same way, otherwise
        // we'll loop forever...
        return false;
    }

    private boolean doMove(Legion legion, MasterHex hex)
    {
        return client.doMove(legion, hex);
    }

    /** cheap, inaccurate evaluation function.  Returns a value for
     * moving this legion to this hex.  The value defines a distance
     * metric over the set of all possible moves.
     *
     * TODO: should be parameterized with weights
     * TODO: the hex parameter is probably not needed anymore now that we
     * pass the legion instead of just the marker [RD: actually,
     * handleVoluntaryMove sees to call this one with several different
     * hexes, so we probably can't remove it]
     */
    private int evaluateMove(LegionClientSide legion, MasterHex hex,
        boolean moved, Map<MasterHex, List<Legion>>[] enemyAttackMap,
        ValueRecorder value)
    {
        // Avoid using MIN_VALUE and MAX_VALUE because of possible overflow.
        final int WIN_GAME = Integer.MAX_VALUE / 2;
        final int LOSE_LEGION = -10000;

        //int value = 0;
        // consider making an attack
        final Legion enemyLegion = client.getGameClientSide()
            .getFirstEnemyLegion(hex, legion.getPlayer());

        if (enemyLegion != null)
        {
            final int enemyPointValue = enemyLegion.getPointValue();
            final int result = estimateBattleResults(legion, enemyLegion, hex);

            switch (result)
            {
                case WIN_WITH_MINIMAL_LOSSES:
                    LOGGER.finest("legion " + legion + " can attack "
                        + enemyLegion + " in " + hex
                        + " and WIN_WITH_MINIMAL_LOSSES");

                    // we score a fraction of a basic acquirable
                    value
                        .add(
                            ((variant.getCreatureByName(variant
                                .getPrimaryAcquirable())).getPointValue() * enemyPointValue)
                                / getAcqStepValue(),
                            "Fraction Basic Acquirable");
                    // plus a fraction of a titan strength
                    // TODO Should be by variant
                    value.add(
                        (6 * enemyPointValue)
                            / variant.getTitanImprovementValue(),
                        "Fraction Titan Strength");
                    // plus some more for killing a group (this is arbitrary)
                    value.add((10 * enemyPointValue) / 100,
                        "Arbitrary For Killing");

                    // TODO if enemy titan, we also score half points
                    // (this may make the AI unfairly gun for your titan)
                    break;

                case WIN_WITH_HEAVY_LOSSES:
                    LOGGER.finest("legion " + legion + " can attack "
                        + enemyLegion + " in " + hex
                        + " and WIN_WITH_HEAVY_LOSSES");
                    // don't do this with our titan unless we can win the game
                    boolean haveOtherSummonables = false;
                    Player player = legion.getPlayer();
                    for (Legion l : player.getLegions())
                    {
                        if (l.equals(legion))
                        {
                            continue;
                        }

                        if (!l.hasSummonable())
                        {
                            continue;
                        }

                        haveOtherSummonables = true;

                        break;
                    }

                    if (legion.hasTitan())
                    {
                        // unless we can win the game with this attack
                        if ((enemyLegion).hasTitan()
                            && client.getGameClientSide()
                                .getNumLivingPlayers() == 2)
                        {
                            // do it and win the game
                            value.add(enemyPointValue,
                                "Killing Last Enemy Titan");
                        }
                        else
                        {
                            // ack! we'll mess up our titan group
                            value.add(LOSE_LEGION + 10,
                                "<Profanity> Our Titan Group");
                        }
                    }
                    // don't do this if we'll lose our only summonable group
                    // and won't score enough points to make up for it
                    else if (legion.hasSummonable() && !haveOtherSummonables
                        && enemyPointValue < getAcqStepValue() * .88)
                    {
                        value.add(LOSE_LEGION + 5, "Lose Legion");
                    }
                    else
                    {
                        // we score a fraction of a basic acquirable
                        value
                            .add(
                                ((variant.getCreatureByName(variant
                                    .getPrimaryAcquirable())).getPointValue() * enemyPointValue)
                                    / getAcqStepValue(),
                                "Fraction Basic Acquirable 2");
                        // plus a fraction of a titan strength
                        value.add(
                            (6 * enemyPointValue)
                                / variant.getTitanImprovementValue(),
                            "Fraction Titan Strength 2");
                        // but we lose this group
                        value.add(-(20 * legion.getPointValue()) / 100,
                            "Lost This Group");
                        // TODO: if we have no other angels, more penalty here
                        // TODO: if enemy titan, we also score half points
                        // (this may make the AI unfairly gun for your titan)
                    }
                    break;

                case DRAW:
                    LOGGER.finest("legion " + legion + " can attack "
                        + enemyLegion + " in " + hex + " and DRAW");

                    // If this is an unimportant group for us, but
                    // is enemy titan, do it.  This might be an
                    // unfair use of information for the AI
                    if (legion.numLords() == 0 && enemyLegion.hasTitan())
                    {
                        // Arbitrary value for killing a player but
                        // scoring no points: it's worth a little
                        // If there are only 2 players, we should do this.
                        if (client.getGameClientSide().getNumLivingPlayers() == 2)
                        {
                            value.resetTo(WIN_GAME, "WinGame");
                        }
                        else
                        {
                            value.add(enemyPointValue / 6,
                                "Unfairly Assault Titan");
                        }
                    }
                    else
                    {
                        // otherwise no thanks
                        value.add(LOSE_LEGION + 2, "Lose Legion 2");
                    }
                    break;

                case LOSE_BUT_INFLICT_HEAVY_LOSSES:
                    LOGGER.finest("legion " + legion + " can attack "
                        + enemyLegion + " in " + hex
                        + " and LOSE_BUT_INFLICT_HEAVY_LOSSES");

                    // TODO: how important is it that we damage his group?
                    value.add(LOSE_LEGION + 1, "Lose Legion 3");
                    break;

                case LOSE:
                    LOGGER.finest("legion " + legion + " can attack "
                        + enemyLegion + " in " + hex + " and LOSE");

                    value.add(LOSE_LEGION, "Lose Legion 4");
                    break;

                default:
                    LOGGER.severe("Bogus battle result case");
            }
        }

        // consider what we can recruit
        CreatureType recruit = null;

        if (moved)
        {
            recruit = chooseRecruit(legion, hex, false);

            if (recruit != null)
            {
                int oldval = value.getValue();

                if (legion.getHeight() <= 5)
                {
                    value.add(
                        getHintedRecruitmentValue(recruit, legion,
                            hintSectionUsed), "Hinted Recruitment Value");
                }
                else if (legion.getHeight() == 6)
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
                    LOGGER.finest("--- 6-HIGH SPECIAL CASE");

                    List<CreatureType> weakestTwo = findWeakestTwoCritters(legion);

                    CreatureType weakest1 = weakestTwo.get(0);
                    CreatureType weakest2 = weakestTwo.get(1);

                    int minCreaturePV = Math.min(
                        getHintedRecruitmentValue(weakest1, legion,
                            hintSectionUsed),
                        getHintedRecruitmentValue(weakest2, legion,
                            hintSectionUsed));
                    int maxCreaturePV = Math.max(
                        getHintedRecruitmentValue(weakest1, legion,
                            hintSectionUsed),
                        getHintedRecruitmentValue(weakest2, legion,
                            hintSectionUsed));
                    // point value of my best 5 pieces right now
                    int oldPV = legion.getPointValue() - minCreaturePV;
                    // point value of my best 5 pieces after adding this
                    // recruit and then splitting off my 2 weakest
                    int newPV = legion.getPointValue()
                        - getHintedRecruitmentValue(weakest1, legion,
                            hintSectionUsed)
                        - getHintedRecruitmentValue(weakest2, legion,
                            hintSectionUsed)
                        + Math.max(
                            maxCreaturePV,
                            getHintedRecruitmentValue(recruit, legion,
                                hintSectionUsed));

                    value
                        .add(
                            (newPV - oldPV)
                                + getHintedRecruitmentValue(recruit, legion,
                                    hintSectionUsed),
                            "Hinted Recruitment Value 2");
                }
                else if (legion.getHeight() == 7)
                {
                    // Cannot recruit, unless we have an angel to summon out,
                    // and we're not fighting, and someone else is, and that
                    // other stack summons out our angel.
                    // Since we don't have enough information about other
                    // stacks to be sure that someone will summon from us,
                    // just give a small bonus for the possible recruit, if
                    // we're not fighting and have a summonable.
                    if (enemyLegion == null && legion.hasSummonable())
                    {
                        // This is total fudge.  Removing an angel may hurt
                        // this legion, or may help it if the recruit is even
                        // better.  But it'll help someone else.  And we don't
                        // know which legion is more important.  So just give
                        // a small bonus for possibly being able to summon out
                        // an angel and recruit.
                        double POSSIBLE_SUMMON_FACTOR = 0.1;
                        value.add(
                            (int)Math.round(POSSIBLE_SUMMON_FACTOR
                                * getHintedRecruitmentValue(recruit, legion,
                                    hintSectionUsed)),
                            "Hinted Recruitment Value 3");
                    }
                }
                else
                {
                    LOGGER.severe("Bogus legion height "
                        + (legion).getHeight() + " in legion "
                        + legion.getMarkerId() + "; content: "
                        + Glob.glob(",", legion.getCreatures()));
                }

                LOGGER.finest("--- if " + legion + " moves to " + hex
                    + " then recruit " + recruit.toString() + " (adding "
                    + (value.getValue() - oldval) + ")");
            }
        }

        // consider what we might be able to recruit next turn, from here
        for (int roll = 1; roll <= 6; roll++)
        {
            int nextTurnValue = 0;
            // XXX Should ignore friends.
            Set<MasterHex> moves = client.getMovement().listAllMoves(legion,
                hex, roll);
            int bestRecruitVal = 0;

            for (MasterHex nextHex : moves)
            {
                // if we have to fight in that hex and we can't
                // WIN_WITH_MINIMAL_LOSSES, then assume we can't
                // recruit there.  IDEA: instead of doing any of this
                // work, perhaps we could recurse here to get the
                // value of being in _that_ hex next turn... and then
                // maximize over choices and average over die rolls.
                // this would be essentially minimax but ignoring the
                // others players ability to move.
                Legion enemy = client.getGameClientSide().getFirstEnemyLegion(
                    nextHex, legion.getPlayer());

                if (enemy != null
                    && estimateBattleResults(legion, enemy, nextHex) != WIN_WITH_MINIMAL_LOSSES)
                {
                    continue;
                }

                List<CreatureType> nextRecruits = client.findEligibleRecruits(
                    legion, nextHex);

                if (nextRecruits.size() == 0)
                {
                    continue;
                }

                CreatureType nextRecruit = nextRecruits.get(nextRecruits
                    .size() - 1);
                // Reduced val by 5 to make current turn recruits more
                // valuable than next turn's recruits
                int val = nextRecruit.getPointValue() - 5;
                if (val > bestRecruitVal)
                {
                    bestRecruitVal = val;
                }
            }

            nextTurnValue += bestRecruitVal;
            nextTurnValue /= 6; // 1/6 chance of each happening
            value
                .add(nextTurnValue, "Next Turn Value (for roll " + roll + ")");
        }

        // consider risk of being attacked
        if (enemyAttackMap != null)
        {
            if (moved)
            {
                LOGGER.finest("considering risk of moving " + legion + " to "
                    + hex);
            }
            else
            {
                LOGGER.finest("considering risk of leaving " + legion + " in "
                    + hex);
            }

            Map<MasterHex, List<Legion>>[] enemiesThatCanAttackOnA = enemyAttackMap;
            int roll;

            for (roll = 1; roll <= 6; roll++)
            {
                List<Legion> enemies = enemiesThatCanAttackOnA[roll].get(hex);

                if (enemies == null)
                {
                    continue;
                }

                for (Legion enemy : enemies)
                {
                    final int result = estimateBattleResults(enemy, false,
                        legion, hex, recruit);

                    if (result == WIN_WITH_MINIMAL_LOSSES || result == DRAW
                        && (legion).hasTitan())
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
                    risk = -legion.getPointValue() / 2.0 * chanceToAttack;
                }

                value.add((int)Math.round(risk), "Risk (not the trademark)");
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
        LOGGER.finest("EVAL " + legion + (moved ? " move to " : " stay in ")
            + hex + " = " + value);

        return value.getValue();
    }

    private static final int WIN_WITH_MINIMAL_LOSSES = 0;
    private static final int WIN_WITH_HEAVY_LOSSES = 1;
    private static final int DRAW = 2;
    private static final int LOSE_BUT_INFLICT_HEAVY_LOSSES = 3;
    private static final int LOSE = 4;

    /* can be overloaded by subclass -> not final */
    // TODO turn into some more Javaish code, particularly in terms of naming conventions,
    // ideally this should be all encapsulated in a configuration object
    double RATIO_WIN_MINIMAL_LOSS()
    {
        return 1.30;
    }

    double RATIO_WIN_HEAVY_LOSS()
    {
        return 1.15;
    }

    double RATIO_DRAW()
    {
        return 0.85;
    }

    double RATIO_LOSE_HEAVY_LOSS()
    {
        return 0.70;
    }

    private int estimateBattleResults(Legion attacker, Legion defender,
        MasterHex hex)
    {
        return estimateBattleResults(attacker, false, defender, hex, null);
    }

    private int estimateBattleResults(Legion attacker,
        boolean attackerSplitsBeforeBattle, Legion defender, MasterHex hex)
    {
        return estimateBattleResults(attacker, attackerSplitsBeforeBattle,
            defender, hex, null);
    }

    private int estimateBattleResults(Legion attacker,
        boolean attackerSplitsBeforeBattle, Legion defender, MasterHex hex,
        CreatureType recruit)
    {
        MasterBoardTerrain terrain = hex.getTerrain();
        double attackerPointValue = getCombatValue(attacker, terrain);

        if (attackerSplitsBeforeBattle)
        {
            // remove PV of the split
            List<CreatureType> creaturesToRemove = chooseCreaturesToSplitOut(attacker);
            for (CreatureType creature : creaturesToRemove)
            {
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

        if (hex.getTerrain().isTower())
        {
            // defender in the tower!  ouch!
            defenderPointValue *= 1.2;
        }
        else if (hex.getTerrain().getDisplayName().equals("Abyss")) // The Abyss, in variants
        {
            // defender in the abyss!  Kill!
            defenderPointValue *= 0.8;
        }

        // really dumb estimator
        double ratio = attackerPointValue / defenderPointValue;

        if (ratio >= RATIO_WIN_MINIMAL_LOSS())
        {
            return WIN_WITH_MINIMAL_LOSSES;
        }
        else if (ratio >= RATIO_WIN_HEAVY_LOSS())
        {
            return WIN_WITH_HEAVY_LOSSES;
        }
        else if (ratio >= RATIO_DRAW())
        {
            return DRAW;
        }
        else if (ratio >= RATIO_LOSE_HEAVY_LOSS())
        {
            return LOSE_BUT_INFLICT_HEAVY_LOSSES;
        }
        else
        // ratio less than 0.70
        {
            return LOSE;
        }
    }

    // This is a really dumb placeholder.  TODO Make it smarter.
    // In particular, the AI should pick a side that will let it enter
    // as many creatures as possible.
    public EntrySide pickEntrySide(MasterHex hex, Legion legion,
        Set<EntrySide> entrySides)
    {
        // Default to bottom to simplify towers.
        if (entrySides.contains(EntrySide.BOTTOM))
        {
            return EntrySide.BOTTOM;
        }
        if (entrySides.contains(EntrySide.RIGHT))
        {
            return EntrySide.RIGHT;
        }
        if (entrySides.contains(EntrySide.LEFT))
        {
            return EntrySide.LEFT;
        }
        return null;
    }

    public MasterHex pickEngagement()
    {
        Set<MasterHex> hexes = client.getGameClientSide().findEngagements();

        // Bail out early if we have no real choice.
        int numChoices = hexes.size();
        if (numChoices == 0)
        {
            return null;
        }
        if (numChoices == 1)
        {
            return hexes.iterator().next();
        }

        MasterHex bestChoice = null;
        int bestScore = Integer.MIN_VALUE;

        for (MasterHex hex : hexes)
        {
            int score = evaluateEngagement(hex);
            if (score > bestScore)
            {
                bestScore = score;
                bestChoice = hex;
            }
        }
        return bestChoice;
    }

    private int evaluateEngagement(MasterHex hex)
    {
        // Fight losing battles last, so that we don't give away
        //    points while they may be used against us this turn.
        // Fight battles with angels first, so that those angels
        //    can be summoned out.
        // Try not to lose potential angels and recruits by having
        //    scooby snacks flee to 7-high stacks (or 6-high stacks
        //    that could recruit later this turn) and push them
        //    over 100-point boundaries.

        Player player = client.getActivePlayer();
        Legion attacker = client.getGameClientSide().getFirstFriendlyLegion(
            hex, player);
        Legion defender = client.getGameClientSide().getFirstEnemyLegion(hex,
            player);
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
            int currentScore = player.getScore();
            int fleeValue = ((LegionClientSide)defender).getPointValue() / 2;
            if (((currentScore + fleeValue) / getAcqStepValue()) > (currentScore / getAcqStepValue()))
            {
                if (attacker.getHeight() == 7 || attacker.getHeight() == 6
                    && client.canRecruit(attacker))
                {
                    value -= 10;
                }
                else
                {
                    // Angels go best in Titan legions.
                    if ((attacker).hasTitan())
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
        if (result <= WIN_WITH_HEAVY_LOSSES
            && ((LegionClientSide)attacker).hasSummonable())
        {
            value += 5;
        }

        return value;
    }

    public boolean flee(Legion legion, Legion enemy)
    {
        if ((legion).hasTitan())
        {
            return false;
        } // Titan never flee !

        int result = estimateBattleResults(enemy, legion,
            legion.getCurrentHex());
        switch (result)
        {
            case WIN_WITH_HEAVY_LOSSES:
            case DRAW:
            case LOSE_BUT_INFLICT_HEAVY_LOSSES:
            case LOSE:
                LOGGER.finest("Legion " + legion.getMarkerId()
                    + " doesn't flee " + " before " + enemy.getMarkerId()
                    + " with result " + result + " ("
                    + ((LegionClientSide)legion).getPointValue() + " vs. "
                    + ((LegionClientSide)enemy).getPointValue() + " in "
                    + legion.getCurrentHex().getTerrainName() + ")");
                return false;

            case WIN_WITH_MINIMAL_LOSSES:
                // don't bother unless we can try to weaken the titan stack
                // and we aren't going to help him by removing cruft
                // also, 7-height stack never flee and wimpy stack always flee
                if ((legion).getHeight() < 6)
                {
                    LOGGER.finest("Legion " + legion.getMarkerId() + " flee "
                        + " as they are just " + (legion).getHeight()
                        + " wimps !");
                    return true;
                }
                if ((((LegionClientSide)enemy).getPointValue() * 0.5) > ((LegionClientSide)legion)
                    .getPointValue())
                {
                    LOGGER.finest("Legion " + legion.getMarkerId() + " flee "
                        + " as they are less than half as strong as "
                        + enemy.getMarkerId());
                    return true;
                }
                if ((enemy).getHeight() == 7)
                {
                    List<CreatureType> recruits = client.findEligibleRecruits(
                        enemy, legion.getCurrentHex());
                    if (recruits.size() > 0)
                    {
                        CreatureType best = recruits.get(recruits.size() - 1);
                        int lValue = ((LegionClientSide)enemy).getPointValue();
                        if (best.getPointValue() > (lValue / (enemy)
                            .getHeight()))
                        {
                            LOGGER.finest("Legion " + legion + " flee "
                                + " to prevent " + enemy
                                + " to be able to recruit " + best);
                            return true;
                        }
                    }
                }
                if ((enemy).hasTitan())
                {
                    LOGGER.finest("Legion " + legion.getMarkerId()
                        + " doesn't flee " + " to fight the Titan in "
                        + enemy.getMarkerId());
                }
                if ((legion).getHeight() == 7)
                {
                    LOGGER.finest("Legion " + legion.getMarkerId()
                        + " doesn't flee " + " they are the magnificent 7 !");
                }
                return !((enemy).hasTitan() || ((legion).getHeight() == 7));
        }
        return false;
    }

    public boolean concede(Legion legion, Legion enemy)
    {
        // Never concede titan legion.
        if ((legion).hasTitan())
        {
            return false;
        }

        // Wimpy legions should concede if it costs the enemy an
        // angel or good recruit.
        MasterBoardTerrain terrain = legion.getCurrentHex().getTerrain();
        int height = (enemy).getHeight();
        if (getCombatValue(legion, terrain) < 0.5 * getCombatValue(enemy,
            terrain) && height >= 6)
        {
            int currentScore = enemy.getPlayer().getScore();
            int pointValue = ((LegionClientSide)legion).getPointValue();
            boolean canAcquireAngel = ((currentScore + pointValue)
                / getAcqStepValue() > (currentScore / getAcqStepValue()));
            // Can't use Legion.getRecruit() because it checks for
            // 7-high legions.
            boolean canRecruit = !client.findEligibleRecruits(enemy,
                enemy.getCurrentHex()).isEmpty();
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
    public CreatureType acquireAngel(Legion legion, List<CreatureType> angels)
    {
        // TODO If the legion is a tiny scooby snack that's about to get
        // smooshed, turn down the angel.

        CreatureType bestAngel = getBestCreature(angels);
        if (bestAngel == null)
        {
            return null;
        }

        // Don't take an angel if 6 high and a better recruit is available.
        // TODO Make this also work for post-battle reinforcements
        if (legion.getHeight() == 6 && client.canRecruit(legion))
        {
            List<CreatureType> recruits = client.findEligibleRecruits(legion,
                legion.getCurrentHex());
            CreatureType bestRecruit = recruits.get(recruits.size() - 1);
            if (getKillValue(bestRecruit) > getKillValue(bestAngel))
            {
                LOGGER.finest("AI declines acquiring to recruit "
                    + bestRecruit.getName());
                return null;
            }
        }
        return bestAngel;
    }

    /**
     * Return the most important Creature in the list of Creatures.
     */
    private CreatureType getBestCreature(List<CreatureType> creatures)
    {
        if (creatures == null || creatures.isEmpty())
        {
            return null;
        }
        CreatureType best = null;
        for (CreatureType creature : creatures)
        {
            if (best == null || getKillValue(creature) > getKillValue(best))
            {
                best = creature;
            }
        }
        return best;
    }

    /**
     * Return a SummonInfo object, containing the summoner, donor and unittype.
     */
    public SummonInfo summonAngel(Legion summoner, List<Legion> donors)
    {
        // Always summon the biggest possible angel, from the least
        // important legion that has one.
        //
        // TODO Sometimes leave room for recruiting.

        Legion bestLegion = null;
        CreatureType bestAngel = null;

        for (Legion legion : donors)
        {
            // Do not summon an angel out of a small Titan stack
            // (Late game it could be desirable, but this is a good start)
            if (legion.hasTitan() && legion.getHeight() <= 5)
            {
                continue;
            }

            for (CreatureType candidate : legion.getCreatureTypes())
            {
                if (candidate.isSummonable())
                {
                    if (bestAngel == null
                        || bestLegion == null
                        || candidate.getPointValue() > bestAngel
                            .getPointValue()
                        || Legion.ORDER_TITAN_THEN_POINTS.compare(legion,
                            bestLegion) > 0
                        && candidate.getPointValue() == bestAngel
                            .getPointValue())
                    {
                        bestLegion = legion;
                        bestAngel = candidate;
                    }
                }
            }
        }
        return bestAngel == null || bestLegion == null ? new SummonInfo()
            : new SummonInfo(summoner, bestLegion, bestAngel);
    }

    private BattleCritter findBestTarget()
    {
        BattleCritter bestTarget = null;
        MasterBoardTerrain terrain = client.getBattleSite().getTerrain();

        // Create a map containing each target and the likely number
        // of hits it would take if all possible creatures attacked it.
        Map<BattleCritter, Double> map = generateDamageMap();
        for (Entry<BattleCritter, Double> entry : map.entrySet())
        {
            BattleCritter target = entry.getKey();
            double h = entry.getValue().doubleValue();

            if (h + target.getHits() >= target.getPower())
            {
                // We can probably kill this target.
                if (bestTarget == null
                    || getKillValue(target, terrain) > getKillValue(
                        bestTarget, terrain))
                {
                    bestTarget = target;
                }
            }
            else
            {
                // We probably can't kill this target.
                // But if it is a Titan it may be more valuable to do fractional damage
                if (bestTarget == null
                    || (0.5 * ((h + target.getHits()) / target.getPower())
                        * getKillValue(target, terrain) > getKillValue(
                            bestTarget, terrain)))
                {
                    bestTarget = target;
                }
            }
        }
        return bestTarget;
    }

    // TODO Have this actually find the best one, not the first one.
    private BattleCritter findBestAttacker(BattleCritter target)
    {
        for (BattleCritter critter : client.getActiveBattleUnits())
        {
            if (client.getBattleCS().canStrike(critter, target))
            {
                return critter;
            }
        }
        return null;
    }

    /** Apply carries first to the biggest creature that could be killed
     *  with them, then to the biggest creature.  carryTargets are
     *  hexLabel description strings. */
    public void handleCarries(int carryDamage, Set<String> carryTargets)
    {
        MasterBoardTerrain terrain = client.getBattleSite().getTerrain();
        BattleCritter bestTarget = null;

        for (String desc : carryTargets)
        {
            String targetHexLabel = desc.substring(desc.length() - 2);
            BattleHex targetHex = terrain.getHexByLabel(targetHexLabel);
            BattleCritter target = getBattleUnit(targetHex);

            if (target.wouldDieFrom(carryDamage))
            {
                if (bestTarget == null
                    || !bestTarget.wouldDieFrom(carryDamage)
                    || getKillValue(target, terrain) > getKillValue(
                        bestTarget, terrain))
                {
                    bestTarget = target;
                }
            }
            else
            {
                if (bestTarget == null
                    || (!bestTarget.wouldDieFrom(carryDamage) && getKillValue(
                        target, terrain) > getKillValue(bestTarget, terrain)))
                {
                    bestTarget = target;
                }
            }
        }
        if (bestTarget == null)
        {
            LOGGER.warning("No carry target but " + carryDamage
                + " points of available carry damage");
            client.leaveCarryMode();
        }
        else
        {
            LOGGER.finest("Best carry target is "
                + bestTarget.getDescription());
            client.applyCarries(bestTarget.getCurrentHex());
        }
    }

    /** Pick one of the list of String strike penalty options. */
    public String pickStrikePenalty(List<String> choices)
    {
        // XXX Stupid placeholder.
        return choices.get(choices.size() - 1);
    }

    /** Simple one-ply group strike algorithm.  Return false if there were
     *  no strike targets. */
    public boolean strike(Legion legion)
    {
        LOGGER.finest("Called ai.strike() for " + legion);
        // PRE: Caller handles forced strikes before calling this.

        // Pick the most important target that can likely be killed this
        // turn.  If none can, pick the most important target.
        // TODO If none can, and we're going to lose the battle this turn,
        // pick the easiest target to kill.

        BattleCritter bestTarget = findBestTarget();
        if (bestTarget == null)
        {
            LOGGER.finest("Best target is null, aborting");
            return false;
        }
        // LOGGER.finest("Best target is " + bestTarget.getDescription());

        // Having found the target, pick an attacker.  The
        // first priority is finding one that does not need
        // to worry about carry penalties to hit this target.
        // The second priority is using the weakest attacker,
        // so that more information is available when the
        // stronger attackers strike.

        BattleCritter bestAttacker = findBestAttacker(bestTarget);
        if (bestAttacker == null)
        {
            return false;
        }

        // LOGGER.finest("Best attacker is " + bestAttacker.getDescription());

        // Having found the target and attacker, strike.
        // Take a carry penalty if there is still a 95%
        // chance of killing this target.
        client.strike(bestAttacker.getTag(), bestTarget.getCurrentHex());
        return true;
    }

    private static int getCombatValue(BattleCritter battleUnit,
        MasterBoardTerrain terrain)
    {
        int val = battleUnit.getPointValue();
        CreatureType creature = battleUnit.getType();

        if (creature.isFlier())
        {
            val++;
        }

        if (creature.isRangestriker())
        {
            val++;
        }

        if (terrain.hasNativeCombatBonus(creature))
        {
            val++;
        }

        return val;
    }

    /** XXX Inaccurate for titans. */
    private int getCombatValue(CreatureType creature,
        MasterBoardTerrain terrain)
    {
        if (creature.isTitan())
        {
            // Don't know the power, so just estimate.
            LOGGER.warning("Called SimpleAI.getCombatValue() for Titan");
            return 6 * variant.getCreatureByName("Titan").getSkill();
        }

        int val = (creature).getPointValue();

        if ((creature).isFlier())
        {
            val++;
        }

        if ((creature).isRangestriker())
        {
            val++;
        }

        if (terrain.hasNativeCombatBonus(creature))
        {
            val++;
        }

        return val;
    }

    private int getTitanCombatValue(int power)
    {
        int titan_skill = variant.getCreatureByName("Titan").getSkill();
        int val = power * titan_skill;
        // Weak Titans do not contribute much to a stack (in fact they're a
        // liability because they need to be protected), so reduce their value.
        // Formula chosen to scale from 0 at power 6, to full strength (48) at
        // power 12 for a 4 skill factor Titan.  5 skill factor Titans are not
        // as vulnerable so reduce them a little less.
        if (power < 12)
        {
            val -= (8 - titan_skill) * (12 - power);
        }
        return val;
    }

    private int getCombatValue(Legion legion, MasterBoardTerrain terrain)
    {
        int val = 0;
        for (CreatureType creature : legion.getCreatureTypes())
        {
            if (creature.isTitan())
            {
                val += getTitanCombatValue(legion.getPlayer().getTitanPower());
            }
            else
            {
                val += getCombatValue(creature, terrain);
            }
        }

        return val;
    }

    protected class PowerSkill
    {
        private final String name;
        private final int power_attack;
        private final int power_defend; // how many dice attackers lose
        private final int skill_attack;
        private final int skill_defend;
        private double hp; // how many hit points or power left
        private final double value;

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
            this(nm, pa, pa, 0, sa, sa);
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

        @Override
        public String toString()
        {
            return name + "(" + hp + ")";
        }
    }

    // return power and skill of a given creature given the terrain
    // terrain here is either a board hex label OR
    // a Hex terrain label
    // TODO this either or is dangerous and forces us to use the label
    //      instead of the objects
    private PowerSkill calcBonus(CreatureType creature, String terrain,
        boolean defender)
    {
        int power = creature.getPower();
        int skill = creature.getSkill();

        TerrainBonuses bonuses = TERRAIN_BONUSES.get(terrain);
        if (bonuses == null)
        {
            // terrain has no special bonuses
            return new PowerSkill(creature.getName(), power, skill);
        }
        else if (terrain.equals("Tower") && defender == false)
        {
            // no attacker bonus for tower
            return new PowerSkill(creature.getName(), power, skill);
        }
        else if ((terrain.equals("Mountains") || terrain.equals("Volcano"))
            && defender == true && creature.getName().equals("Dragon"))
        {
            // Dragon gets an extra 3 die when attack down slope
            // non-native loses 1 skill when attacking up slope
            return new PowerSkill(creature.getName(), power, power + 3,
                bonuses.defenderPower, skill + bonuses.attackerSkill, skill
                    + bonuses.defenderSkill);
        }
        else
        {
            return new PowerSkill(creature.getName(), power, power
                + bonuses.attackerPower, bonuses.defenderPower, skill
                + bonuses.attackerSkill, skill + bonuses.defenderSkill);
        }
    }

    // return power and skill of a given creature given the terrain
    // board hex label
    // *WARNING* CreatureType.getPower doesn't work for Titan.
    protected PowerSkill getNativeValue(CreatureType creature,
        MasterBoardTerrain terrain, boolean defender)
    {
        // TODO checking the tower via string is unsafe -- maybe terrain.isTower()
        //      is meant anyway
        if (!(terrain.hasNativeCombatBonus(creature) || (terrain.getId()
            .equals("Tower") && defender == true)))
        {
            return new PowerSkill(creature.getName(), creature.getPower(),
                creature.getSkill());
        }

        return calcBonus(creature, terrain.getId(), defender);

    }

    /** Return a list of critter moves, in best move order. */
    public List<CritterMove> battleMove()
    {
        LOGGER.finest("Called battleMove()");

        // Defer setting time limit until here where it's needed, to
        // avoid initialization timing issues.
        timeLimit = client.getOptions().getIntOption(Options.aiTimeLimit);

        // Consider one critter at a time, in order of importance.
        // Examine all possible moves for that critter not already
        // taken by a more important one.

        // TODO Handle summoned/recruited critters, in particular
        // getting stuff out of the way so that a reinforcement
        // has room to enter.

        Collection<LegionMove> legionMoves = findBattleMoves();
        LegionMove bestLegionMove = findBestLegionMove(legionMoves);
        List<CritterMove> bestMoveOrder = findMoveOrder(bestLegionMove);

        return bestMoveOrder;
    }

    /** Try another move for creatures whose moves failed. */
    public void retryFailedBattleMoves(List<CritterMove> bestMoveOrder)
    {
        if (bestMoveOrder == null)
        {
            return;
        }
        for (CritterMove cm : bestMoveOrder)
        {
            BattleCritter critter = cm.getCritter();

            // LOGGER.finest(critter.getDescription() + " failed to move");
            List<CritterMove> moveList = findBattleMovesOneCritter(critter);
            if (!moveList.isEmpty())
            {
                CritterMove cm2 = moveList.get(0);
                /* LOGGER.finest("Moving " + critter.getDescription() + " to "
                  + cm2.getEndingHex().getLabel() + " (startingHexLabel was "
                  + cm.getStartingHex().getLabel() + ")"); */
                client.tryBattleMove(cm2);
            }
        }
    }

    private List<CritterMove> findMoveOrder(LegionMove lm)
    {
        if (lm == null)
        {
            return null;
        }

        int perfectScore = 0;

        List<CritterMove> critterMoves = new ArrayList<CritterMove>();
        critterMoves.addAll(lm.getCritterMoves());

        Iterator<CritterMove> itCrit = critterMoves.iterator();
        while (itCrit.hasNext())
        {
            CritterMove cm = itCrit.next();
            if (cm.getStartingHex().equals(cm.getEndingHex()))
            {
                // Prune non-movers
                itCrit.remove();
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

        int bestScore = 0;
        List<CritterMove> bestOrder = null;
        boolean bestAllOK = false;

        int count = 0;
        Timer findMoveTimer = setupTimer();

        Iterator<List<CritterMove>> it = new PermutationIterator<CritterMove>(
            critterMoves);
        while (it.hasNext())
        {
            List<CritterMove> order = it.next();

            count++;

            boolean allOK = true;
            int score = testMoveOrder(order, null);
            if (score < 0)
            {
                allOK = false;
                score = -score;
            }
            if (score > bestScore)
            {
                bestOrder = new ArrayList<CritterMove>(order);
                bestScore = score;
                bestAllOK = allOK;
                if (score >= perfectScore)
                {
                    break;
                }
            }

            // Bail out early, if there is at least some valid move.
            if (timeIsUp)
            {
                if (bestScore > 0)
                {
                    break;
                }
                else
                {
                    LOGGER
                        .warning("Time is up figuring move order, but we ignore "
                            + "it (no valid moveOrder found yet... "
                            + " - buggy break)");
                    timeIsUp = false;
                }
            }
        }
        findMoveTimer.cancel();
        if (!bestAllOK)
        {
            List<CritterMove> newOrder = new ArrayList<CritterMove>();
            testMoveOrder(bestOrder, newOrder);
            bestOrder = new ArrayList<CritterMove>(newOrder);
        }
        LOGGER.finest("Got score " + bestScore + " in " + count
            + " permutations");
        return bestOrder;
    }

    /** Try each of the moves in order.  Return the number that succeed,
     *  scaled by the importance of each critter.
     *  In newOrder, if not null, place the moves that are valid.
     */
    private int testMoveOrder(List<CritterMove> order,
        List<CritterMove> newOrder)
    {
        boolean allOK = true;
        int val = 0;
        for (CritterMove cm : order)
        {
            BattleCritter critter = cm.getCritter();
            BattleHex hex = cm.getEndingHex();
            if (client.testBattleMove(critter, hex))
            {
                // XXX Use kill value instead?
                val += critter.getPointValue();
                if (newOrder != null)
                {
                    newOrder.add(cm);
                }
            }
            else
            {
                allOK = false;
            }
        }

        // Move them all back where they started.
        for (CritterMove cm : order)
        {
            BattleCritter critter = cm.getCritter();
            BattleHex hex = cm.getStartingHex();
            critter.setCurrentHex(hex);
        }

        if (!allOK)
        {
            val = -val;
        }
        return val;
    }

    private final int MAX_LEGION_MOVES = 10000;

    /** Find the maximum number of moves per creature to test, such that
     *  numMobileCreaturesInLegion ^ N <= LEGION_MOVE_LIMIT, but we must
     *  have at least as many moves as mobile creatures to ensure that
     *  every creature has somewhere to go. */
    protected int getCreatureMoveLimit() // NO_UCD
    {
        int mobileCritters = client.findMobileBattleUnits().size();
        if (mobileCritters <= 1)
        {
            // Avoid infinite logs and division by zero, and just try
            // all possible moves.
            return Constants.BIGNUM;
        }
        int max = (int)Math.floor(Math.log(MAX_LEGION_MOVES)
            / Math.log(mobileCritters));
        return (Math.min(max, mobileCritters));
    }

    private Collection<LegionMove> findBattleMoves()
    {
        LOGGER.finest("Called findBattleMoves()");

        // Consider one critter at a time in isolation.
        // Find the best N moves for each critter.

        // TODO Do not consider immobile critters.  Also, do not allow
        // non-flying creatures to move through their hexes.

        // TODO Handle summoned/recruited critters, in particular
        // getting stuff out of the way so that a reinforcement
        // has room to enter.

        // The caller is responsible for actually making the moves.
        final List<List<CritterMove>> allCritterMoves = new ArrayList<List<CritterMove>>();

        for (BattleCritter critter : client.getActiveBattleUnits())
        {
            List<CritterMove> moveList = findBattleMovesOneCritter(critter);

            // Add this critter's moves to the list.
            allCritterMoves.add(moveList);

            // Put all critters back where they started.
            Iterator<List<CritterMove>> it2 = allCritterMoves.iterator();
            while (it2.hasNext())
            {
                moveList = it2.next();
                CritterMove cm = moveList.get(0);
                BattleCritter critter2 = cm.getCritter();
                critter2.moveToHex(cm.getStartingHex());
            }
        }

        Collection<LegionMove> legionMoves = findLegionMoves(allCritterMoves);
        return legionMoves;
    }

    private List<CritterMove> findBattleMovesOneCritter(BattleCritter critter)
    {
        BattleHex currentHex = critter.getCurrentHex();

        // moves is a list of hex labels where one critter can move.

        // Sometimes friendly critters need to get out of the way to
        // clear a path for a more important critter.  We consider
        // moves that the critter could make, disregarding mobile allies.

        // XXX Should show moves including moving through mobile allies.
        Set<BattleHex> moves = client.showBattleMoves(critter);

        // TODO Make less important creatures get out of the way.

        // Not moving is also an option.
        moves.add(currentHex);

        List<CritterMove> moveList = new ArrayList<CritterMove>();

        for (BattleHex hex : moves)
        {
            ValueRecorder why = new ValueRecorder();
            CritterMove cm = new CritterMove(critter, currentHex, hex);

            // Need to move the critter to evaluate.
            critter.moveToHex(hex);

            // Compute and save the value for each CritterMove.
            cm.setValue(evaluateCritterMove(critter, null, why));
            moveList.add(cm);
            // Move the critter back where it started.
            critter.moveToHex(critter.getStartingHex());
        }

        // Sort critter moves in descending order of score.
        Collections.sort(moveList, new Comparator<CritterMove>()
        {
            public int compare(CritterMove cm1, CritterMove cm2)
            {
                return cm2.getValue() - cm1.getValue();
            }
        });

        // Show the moves considered.
        StringBuilder buf = new StringBuilder("Considered " + moveList.size()
            + " moves for " + critter.getTag() + " "
            + critter.getType().getName() + " in " + currentHex.getLabel()
            + ":");
        for (CritterMove cm : moveList)
        {
            buf.append(" " + cm.getEndingHex().getLabel());
        }
        LOGGER.finest(buf.toString());

        return moveList;
    }

    Timer setupTimer()
    {
        // java.util.Timer, not Swing Timer
        Timer timer = new Timer();
        timeIsUp = false;
        final int MS_PER_S = 1000;
        if (timeLimit < Constants.MIN_AI_TIME_LIMIT
            || timeLimit > Constants.MAX_AI_TIME_LIMIT)
        {
            timeLimit = Constants.DEFAULT_AI_TIME_LIMIT;
        }
        timer.schedule(new TriggerTimeIsUp(), MS_PER_S * timeLimit);
        return timer;
    }

    protected final static int MIN_ITERATIONS = 50;

    /** Evaluate all legion moves in the list, and return the best one.
     *  Break out early if the time limit is exceeded. */
    protected LegionMove findBestLegionMove(Collection<LegionMove> legionMoves)
    {
        int bestScore = Integer.MIN_VALUE;
        LegionMove best = null;

        if (legionMoves instanceof List)
            Collections.shuffle((List<LegionMove>)legionMoves, random);

        Timer findBestLegionMoveTimer = setupTimer();

        int count = 0;
        for (LegionMove lm : legionMoves)
        {
            int score = evaluateLegionBattleMove(lm);
            if (score > bestScore)
            {
                bestScore = score;
                best = lm;
                LOGGER.finest("INTERMEDIATE Best legion move: "
                    + lm.getStringWithEvaluation() + " (" + score + ")");
            }
            else
            {
                LOGGER.finest("INTERMEDIATE      legion move: "
                    + lm.getStringWithEvaluation() + " (" + score + ")");
            }

            count++;

            if (timeIsUp)
            {
                if (count >= MIN_ITERATIONS)
                {
                    LOGGER.finest("findBestLegionMove() time up after "
                        + count + " iterations");
                    break;
                }
                else
                {
                    LOGGER.finest("findBestLegionMove() time up after "
                        + count + " iterations, but we keep searching until "
                        + MIN_ITERATIONS);
                }
            }
        }
        findBestLegionMoveTimer.cancel();
        LOGGER.finer("Best legion move of " + count + " checked (turn "
            + client.getBattleTurnNumber() + "): "
            + ((best == null) ? "none " : best.getStringWithEvaluation())
            + " (" + bestScore + ")");
        return best;
    }

    /** allCritterMoves is a List of sorted MoveLists.  A MoveList is a
     *  sorted List of CritterMoves for one critter.  Return a sorted List
     *  of LegionMoves.  A LegionMove is a List of one CritterMove per
     *  mobile critter in the legion, where no two critters move to the
     *  same hex.
     */
    Collection<LegionMove> findLegionMoves(
        final List<List<CritterMove>> allCritterMoves)
    {
        return generateLegionMoves(allCritterMoves, false);
    }

    @SuppressWarnings("unused")
    protected int evaluateLegionBattleMoveAsAWhole(LegionMove lm,
        Map<BattleHex, Integer> strikeMap, ValueRecorder value)
    {
        // This is empty, to be overidden by subclasses.
        return 0;
    }

    /** this compute the special case of the Titan critter */
    protected void evaluateCritterMove_Titan(final BattleCritter critter,
        ValueRecorder value, final MasterBoardTerrain terrain,
        final BattleHex hex, final Legion legion, final int turn)
    {
        if (hex.isEntrance())
        {
            return;
        }
        // Reward titans sticking to the edges of the back row
        // surrounded by allies.  We need to relax this in the
        // last few turns of the battle, so that attacking titans
        // don't just sit back and wait for a time loss.
        BattleHex entrance = terrain.getEntrance(legion.getEntrySide());
        if (!critter.isTitan())
        {
            LOGGER
                .warning("evaluateCritterMove_Titan called on non-Titan critter");
            return;
        }
        if (terrain.isTower() && legion.equals(client.getDefender()))
        {
            // Stick to the center of the tower.
            value.add(bec.TITAN_TOWER_HEIGHT_BONUS * hex.getElevation(),
                "TitanTowerHeightBonus");
        }
        else
        {
            if (turn <= 4)
            {
                value.add(
                    bec.TITAN_FORWARD_EARLY_PENALTY
                        * Battle.getRange(hex, entrance, true),
                    "TitanForwardEarlyPenalty");
                for (int i = 0; i < 6; i++)
                {
                    BattleHex neighbor = hex.getNeighbor(i);
                    if (neighbor == null /* Edge of the map */
                        || neighbor.getTerrain().blocksGround()
                        || (neighbor.getTerrain().isGroundNativeOnly() && !hasOpponentNativeCreature(neighbor
                            .getTerrain())))
                    {
                        value.add(bec.TITAN_BY_EDGE_OR_BLOCKINGHAZARD_BONUS,
                            "TitanByEdgeOrBlockingHazard (" + i + ")");
                    }
                }
            }
        }
        // Treat damage to Titan as 4 times worse than for a normal critter
        value.add(
            4 * bec.PENALTY_DAMAGE_TERRAIN
                * hex.damageToCreature(critter.getType()),
            "TerrainDamageToTitanIsExtraBad");
    }

    /** This compute the influence of terrain */
    private void evaluateCritterMove_Terrain(
        final BattleCritter critter, // NO_UCD
        ValueRecorder value, final MasterBoardTerrain terrain,
        final BattleHex hex, final int power, final int skill)
    {
        if (hex.isEntrance())
        {
            // Staying offboard to die is really bad.
            value.add(
                bec.OFFBOARD_DEATH_SCALE_FACTOR
                    * getCombatValue(critter, terrain), "StayingOffboard");
            return;
        }
        PowerSkill ps = calcBonus(critter.getType(), hex.getTerrain()
            .getName(), true);
        int native_power = ps.getPowerAttack() + (ps.getPowerDefend() + power);
        int native_skill = ps.getSkillAttack() + ps.getSkillDefend();
        // Add for sitting in favorable terrain.
        // Subtract for sitting in unfavorable terrain.
        if (hex.isNativeBonusTerrain()
            && critter.getType().isNativeIn(hex.getTerrain()))
        {
            value.add(bec.NATIVE_BONUS_TERRAIN, "NativeBonusTerrain");

            // Above gives a small base value.
            // Scale remaining bonus to size of benefit

            if (hex.getElevation() > 0)
            {
                native_skill += 1; // guess at bonus
            }

            int bonus = (native_power - 2 * power) * skill
                + (native_skill - 2 * skill) * power;

            value.add(3 * bonus, "More NativeBonusTerrain");

            // We want marsh natives to slightly prefer moving to bog hexes,
            // even though there's no real bonus there, to leave other hexes
            // clear for non-native allies.
            if (hex.getTerrain().equals(HazardTerrain.getTerrainByName("Bog")))
            {
                value.add(bec.NATIVE_BOG, "NativeBog");
            }
        }
        else
        // Critter is not native or the terrain is not beneficial
        {
            if (hex.isNonNativePenaltyTerrain()
                && (!critter.getType().isNativeIn(hex.getTerrain())))
            {
                value.add(bec.NON_NATIVE_PENALTY_TERRAIN, "NonNativePenalty");

                // Above gives a small base value.
                // Scale remaining bonus to size of benefit
                int bonus = (native_power - 2 * power) * skill
                    + (native_skill - 2 * skill) * power;

                value.add(3 * bonus, "More NonNativePenalty");
            }
        }

        /* damage is positive, healing is negative, so we can always add */
        value.add(
            bec.PENALTY_DAMAGE_TERRAIN
                * hex.damageToCreature(critter.getType()),
            "PenaltyDamageTerrain");
    }

    /** this compute for non-titan attacking critter */
    @SuppressWarnings({ "unused", "deprecation" })
    private void evaluateCritterMove_Attacker(
        final BattleCritter critter, // NO_UCD
        ValueRecorder value, final MasterBoardTerrain terrain,
        final BattleHex hex, final LegionClientSide legion, final int turn)
    {
        if (hex.isEntrance())
        {
            return;
        }
        // Attacker, non-titan, needs to charge.
        // Head for enemy creatures.
        value.add(bec.ATTACKER_DISTANCE_FROM_ENEMY_PENALTY
            * client.getBattleCS().minRangeToEnemy(critter),
            "AttackerDistanceFromEnemyPenalty");
    }

    /** this compute for non-titan defending critter */
    @SuppressWarnings("unused")
    protected void evaluateCritterMove_Defender(final BattleCritter critter,
        ValueRecorder value, final MasterBoardTerrain terrain,
        final BattleHex hex, final LegionClientSide legion, final int turn)
    {
        if (hex.isEntrance())
        {
            return;
        }
        // Encourage defending critters to hang back.
        BattleHex entrance = terrain.getEntrance(legion.getEntrySide());
        if (terrain.isTower())
        {
            // Stick to the center of the tower.
            value.add(bec.DEFENDER_TOWER_HEIGHT_BONUS * hex.getElevation(),
                "DefenderTowerHeightBonus");
        }
        else
        {
            int range = Battle.getRange(hex, entrance, true);

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
                value.add(
                    bec.DEFENDER_FORWARD_EARLY_PENALTY
                        * Math.abs(range - preferredRange),
                    "DefenderForwardEarlyPenalty");
            }
            for (int i = 0; i < 6; i++)
            {
                BattleHex neighbor = hex.getNeighbor(i);
                if (neighbor == null /* Edge of the map */
                    || neighbor.getTerrain().blocksGround()
                    || (neighbor.getTerrain().isGroundNativeOnly() && !hasOpponentNativeCreature(neighbor
                        .getTerrain())))
                {
                    value.add(bec.DEFENDER_BY_EDGE_OR_BLOCKINGHAZARD_BONUS,
                        "DefenderByEdgeOrBlockingHazard (" + i + ")");
                }
            }
        }
    }

    @SuppressWarnings("unused")
    protected void evaluateCritterMove_Rangestrike(
        final BattleCritter critter, final Map<BattleHex, Integer> strikeMap,
        ValueRecorder value, final MasterBoardTerrain terrain,
        final BattleHex hex, final int power, final int skill,
        final LegionClientSide legion, final int turn,
        final Set<BattleHex> targetHexes)
    {
        if (hex.isEntrance())
        {
            return;
        }
        int numTargets = targetHexes.size();
        // Rangestrikes.
        value.add(bec.FIRST_RANGESTRIKE_TARGET, "FirstRangestrikeTarget");

        // Having multiple targets is good, in case someone else
        // kills one.
        if (numTargets >= 2)
        {
            value.add(bec.EXTRA_RANGESTRIKE_TARGET, "ExtraRangestrikeTarget");
        }

        // Non-warlock skill 4 rangestrikers should slightly prefer
        // range 3 to range 4.  Non-brush rangestrikers should
        // prefer strikes not through bramble.  Warlocks should
        // try to rangestrike titans.
        boolean penalty = true;
        for (BattleHex targetHex : targetHexes)
        {
            BattleCritter target = getBattleUnit(targetHex);
            if (target.isTitan())
            {
                value.add(bec.RANGESTRIKE_TITAN, "RangestrikeTitan");
            }
            int strikeNum = getBattleStrike().getStrikeNumber(critter, target);
            if (strikeNum <= 4 - skill + target.getSkill())
            {
                penalty = false;
            }

            // Reward ganging up on enemies.
            if (strikeMap != null)
            {
                int numAttackingThisTarget = strikeMap.get(targetHex)
                    .intValue();
                if (numAttackingThisTarget > 1)
                {
                    value.add(bec.GANG_UP_ON_CREATURE,
                        "GangUpOnCreature RangeStrike");
                }
            }
        }
        if (!penalty)
        {
            value.add(bec.RANGESTRIKE_WITHOUT_PENALTY,
                "RangestrikeWithoutPenalty");
        }
    }

    @SuppressWarnings("unused")
    protected void evaluateCritterMove_Strike(final BattleCritter critter,
        final Map<BattleHex, Integer> strikeMap, ValueRecorder value,
        final MasterBoardTerrain terrain, final BattleHex hex,
        final int power, final int skill, final LegionClientSide legion,
        final int turn, final Set<BattleHex> targetHexes)
    {
        if (hex.isEntrance())
        {
            return;
        }
        // Normal strikes.  If we can strike them, they can strike us.

        // Reward being adjacent to an enemy if attacking.
        if (legion.equals(client.getAttacker()))
        {
            value.add(bec.ATTACKER_ADJACENT_TO_ENEMY,
                "AttackerAdjacentToEnemy");
        }
        // Slightly penalize being adjacent to an enemy if defending.
        else
        {
            value.add(bec.DEFENDER_ADJACENT_TO_ENEMY,
                "DefenderAdjacentToEnemy");
        }

        int killValue = 0;
        int numKillableTargets = 0;
        int hitsExpected = 0;

        for (BattleHex targetHex : targetHexes)
        {
            BattleCritter target = getBattleUnit(targetHex);

            // Reward being next to enemy titans.  (Banzai!)
            if (target.isTitan())
            {
                value.add(bec.ADJACENT_TO_ENEMY_TITAN, "AdjacentToEnemyTitan");
            }

            // Reward being next to a rangestriker, so it can't hang
            // back and plink us.
            if (target.isRangestriker() && !critter.isRangestriker())
            {
                value.add(bec.ADJACENT_TO_RANGESTRIKER,
                    "AdjacenttoRangestriker");
            }

            // Attack Warlocks so they don't get Titan
            if (target.getType().getName().equals("Warlock"))
            {
                value.add(bec.ADJACENT_TO_BUDDY_TITAN, "AdjacentToBuddyTitan");
            }

            // Reward being next to an enemy that we can probably
            // kill this turn.
            int dice = getBattleStrike().getDice(critter, target);
            int strikeNum = getBattleStrike().getStrikeNumber(critter, target);
            double meanHits = Probs.meanHits(dice, strikeNum);
            if (meanHits + target.getHits() >= target.getPower())
            {
                numKillableTargets++;
                int targetValue = getKillValue(target, terrain);
                killValue = Math.max(targetValue, killValue);
            }
            else
            {
                // reward doing damage to target - esp. titan.
                int targetValue = getKillValue(target, terrain);
                killValue = (int)(0.5 * (meanHits / target.getPower()) * Math
                    .max(targetValue, killValue));
            }

            // Reward ganging up on enemies.
            if (strikeMap != null)
            {
                int numAttackingThisTarget = strikeMap.get(targetHex)
                    .intValue();
                if (numAttackingThisTarget > 1)
                {
                    value.add(bec.GANG_UP_ON_CREATURE,
                        "GangUpOnCreature Strike");
                }
            }

            // Penalize damage that we can take this turn,
            {
                dice = getBattleStrike().getDice(target, critter);
                strikeNum = getBattleStrike().getStrikeNumber(target, critter);
                hitsExpected += Probs.meanHits(dice, strikeNum);
            }
        }

        if (legion.equals(client.getAttacker()))
        {
            value.add(bec.ATTACKER_KILL_SCALE_FACTOR * killValue,
                "AttackerKillValueScaled");
            value.add(bec.KILLABLE_TARGETS_SCALE_FACTOR * numKillableTargets,
                "AttackerNumKillable");
        }
        else
        {
            value.add(bec.DEFENDER_KILL_SCALE_FACTOR * killValue,
                "DefenderKillValueScaled");
            value.add(bec.KILLABLE_TARGETS_SCALE_FACTOR * numKillableTargets,
                "DefenderNumKillable");
        }

        int hits = critter.getHits();

        // XXX Attacking legions late in battle ignore damage.
        // the isTitan() here should be moved to _Titan function above ?
        if (legion.equals(client.getDefender()) || critter.isTitan()
            || turn <= 4)
        {
            if (hitsExpected + hits >= power)
            {
                if (legion.equals(client.getAttacker()))
                {
                    value.add(bec.ATTACKER_GET_KILLED_SCALE_FACTOR
                        * getKillValue(critter, terrain), "AttackerGetKilled");
                }
                else
                {
                    value.add(bec.DEFENDER_GET_KILLED_SCALE_FACTOR
                        * getKillValue(critter, terrain), "DefenderGetKilled");
                }
            }
            else
            {
                if (legion.equals(client.getAttacker()))
                {
                    value.add(bec.ATTACKER_GET_HIT_SCALE_FACTOR
                        * getKillValue(critter, terrain), "AttackerGetHit");
                }
                else
                {
                    value.add(bec.DEFENDER_GET_HIT_SCALE_FACTOR
                        * getKillValue(critter, terrain), "DefendergetHit");
                }
            }
        }
    }

    /** strikeMap is optional */
    private int evaluateCritterMove(BattleCritter critter,
        Map<BattleHex, Integer> strikeMap, ValueRecorder value)
    {
        final MasterBoardTerrain terrain = client.getBattleSite().getTerrain();
        final LegionClientSide legion = (LegionClientSide)client
            .getMyEngagedLegion();
        final int skill = critter.getSkill();
        final int power = critter.getPower();
        final BattleHex hex = critter.getCurrentHex();
        final int turn = client.getBattleTurnNumber();

        PowerSkill ps = calcBonus(critter.getType(), hex.getTerrain()
            .getName(), true);

        int native_power = ps.getPowerAttack() + (ps.getPowerDefend() + power);
        int native_skill = ps.getSkillAttack() + ps.getSkillDefend();

        evaluateCritterMove_Terrain(critter, value, terrain, hex, power, skill);

        if (hex.isEntrance())
        {
            return value.getValue();
        }

        Set<BattleHex> targetHexes = client.findStrikes(critter.getTag());
        int numTargets = targetHexes.size();

        if (numTargets >= 1)
        {
            if (!client.isInContact(critter, true))
            {
                evaluateCritterMove_Rangestrike(critter, strikeMap, value,
                    terrain, hex, power, skill, legion, turn, targetHexes);
            }
            else
            {
                evaluateCritterMove_Strike(critter, strikeMap, value, terrain,
                    hex, power, skill, legion, turn, targetHexes);
            }
        }

        if (critter.isTitan())
        {
            evaluateCritterMove_Titan(critter, value, terrain, hex, legion,
                turn);
        }
        else if (legion.equals(client.getDefender()))
        {
            evaluateCritterMove_Defender(critter, value, terrain, hex, legion,
                turn);
        }
        else
        {
            evaluateCritterMove_Attacker(critter, value, terrain, hex, legion,
                turn);
        }

        // Adjacent buddies
        for (int i = 0; i < 6; i++)
        {
            if (!hex.isCliff(i))
            {
                BattleHex neighbor = hex.getNeighbor(i);
                if (neighbor != null
                    && client.getGame().getBattle().isOccupied(neighbor))
                {
                    BattleCritter other = getBattleUnit(neighbor);
                    if (other.isDefender() == critter.isDefender())
                    {
                        // Buddy
                        if (other.isTitan())
                        {
                            value.add(bec.ADJACENT_TO_BUDDY_TITAN,
                                "AdjacentToBuddyTitan 2");
                            value.add(
                                native_skill
                                    * (native_power - critter.getHits()),
                                "More AdjacentToBuddyTitan 2");
                        }
                        else
                        {
                            value
                                .add(bec.ADJACENT_TO_BUDDY, "AdjacentToBuddy");
                        }
                    }
                }
            }
        }

        return value.getValue();
    }

    protected int evaluateLegionBattleMove(LegionMove lm)
    {
        lm.resetEvaluate();

        // First we need to move all critters into position.
        for (CritterMove cm : lm.getCritterMoves())
        {
            cm.getCritter().moveToHex(cm.getEndingHex());
        }

        Map<BattleHex, Integer> strikeMap = findStrikeMap();

        // Then find the sum of all critter evals.
        int sum = 0;
        for (CritterMove cm : lm.getCritterMoves())
        {
            ValueRecorder why = new ValueRecorder();
            int val = evaluateCritterMove(cm.getCritter(), strikeMap, why);
            lm.setEvaluate(cm, why.toString());
            sum += val;
        }

        // whole position evaluation
        {
            ValueRecorder why = new ValueRecorder();
            int val = evaluateLegionBattleMoveAsAWhole(lm, strikeMap, why);
            lm.setEvaluate(why.toString());
            sum += val;
        }

        // Then move them all back.
        for (CritterMove cm : lm.getCritterMoves())
        {
            cm.getCritter().moveToHex(cm.getStartingHex());
        }

        lm.setValue(sum);

        return sum;
    }

    protected class TriggerTimeIsUp extends TimerTask
    {
        @Override
        public void run()
        {
            timeIsUp = true;
        }
    }
}
