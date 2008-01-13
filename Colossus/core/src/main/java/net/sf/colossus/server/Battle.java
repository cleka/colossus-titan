package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.BattleHex;
import net.sf.colossus.client.BattleMap;
import net.sf.colossus.client.HexMap;
import net.sf.colossus.game.PlayerState;
import net.sf.colossus.util.Options;
import net.sf.colossus.variant.HazardTerrain;
import net.sf.colossus.variant.MasterHex;


/**
 * Class Battle holds data about a Titan battle. It has utility functions
 * related to incrementing the phase, managing moves, and managing
 * strikes
 *
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

public final class Battle extends net.sf.colossus.game.Battle
{
    private static final Logger LOGGER = Logger.getLogger(Battle.class
        .getName());

    private Game game;
    private Server server;
    private final String attackerId;
    private final String defenderId;
    private final String[] legions = new String[2];
    private int activeLegionNum;
    private final String masterHexLabel;
    private final String terrain;
    private int turnNumber;
    private Constants.BattlePhase phase;
    private int summonState = Constants.NO_KILLS;
    private int carryDamage;
    private boolean attackerElim;
    private boolean defenderElim;
    private boolean battleOver;
    private boolean attackerEntered;
    private boolean conceded;
    private boolean driftDamageApplied = false;

    /**
     * Set of hexLabels for valid carry targets 
     *
     * TODO storing the hexes themselves would be better
     */
    private final Set<String> carryTargets = new HashSet<String>();
    private final PhaseAdvancer phaseAdvancer = new BattlePhaseAdvancer();
    private int pointsScored = 0;

    Battle(Game game, String attackerId, String defenderId,
        int activeLegionNum, String masterHexLabel, int turnNumber,
        Constants.BattlePhase phase)
    {
        super(game.getLegionByMarkerId(attackerId), game
            .getLegionByMarkerId(defenderId), null);
        this.game = game;
        server = game.getServer();
        this.masterHexLabel = masterHexLabel;
        this.defenderId = defenderId;
        this.attackerId = attackerId;
        legions[0] = defenderId;
        legions[1] = attackerId;
        this.activeLegionNum = activeLegionNum;
        this.turnNumber = turnNumber;
        this.phase = phase;

        terrain = getMasterHex().getTerrain();

        // Set defender's entry side opposite attacker's.
        Legion attacker = getAttacker();
        int side = attacker.getEntrySide();
        if (side != 1 && side != 3 && side != 5)
        {
            LOGGER.log(Level.WARNING, "Fixing bogus entry side: " + side);
            // If invalid, default to bottom, which is always valid.
            attacker.setEntrySide(3);
        }
        Legion defender = getDefender();
        defender.setEntrySide((side + 3) % 6);
        // Make sure defender can recruit, even if savegame is off.
        defender.setRecruitName(null);

        LOGGER.log(Level.INFO, attacker.getLongMarkerName()
            + " ("
            + attacker.getPlayerName()
            + ") attacks "
            + defender.getLongMarkerName()
            + " ("
            + defender.getPlayerName()
            + ")"
            + " in "
            + game.getVariant().getMasterBoard().getHexByLabel(masterHexLabel)
                .getDescription());

        placeLegion(attacker);
        placeLegion(defender);
    }

    public void cleanRefs()
    {
        this.server = null;
        this.game = null;
    }

    private void placeLegion(Legion legion)
    {
        BattleHex entrance = BattleMap.getEntrance(terrain, legion
            .getEntrySide());
        Iterator<Critter> it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();

            BattleHex currentHex = critter.getCurrentHex();
            if (currentHex == null)
            {
                currentHex = entrance;
            }
            BattleHex startingHex = critter.getStartingHex();
            if (startingHex == null)
            {
                startingHex = entrance;
            }

            critter.addBattleInfo(currentHex, startingHex, this);
        }
    }

    private void placeCritter(Critter critter)
    {
        BattleHex entrance = BattleMap.getEntrance(terrain, ((Legion)critter
            .getLegion()).getEntrySide());
        critter.addBattleInfo(entrance, entrance, this);
        server.allPlaceNewChit(critter);
    }

    private synchronized void initBattleChits(Legion legion)
    {
        Iterator<Critter> it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
            server.allPlaceNewChit(critter);
        }
    }

    /** We need to do two-stage construction so that game.battle
     *  is non-null earlier. */
    void init()
    {
        server.allInitBattle(masterHexLabel);
        initBattleChits(getAttacker());
        initBattleChits(getDefender());

        boolean advance = false;
        Constants.BattlePhase phase = getBattlePhase();
        if (phase == Constants.BattlePhase.SUMMON)
        {
            advance = setupSummon();
        }
        else if (phase == Constants.BattlePhase.RECRUIT)
        {
            advance = setupRecruit();
        }
        else if (phase == Constants.BattlePhase.MOVE)
        {
            advance = setupMove();
        }
        else if (phase.isFightPhase())
        {
            advance = setupFight();
        }
        else
        {
            LOGGER.log(Level.SEVERE, "Bogus phase");
        }
        if (advance)
        {
            advancePhase();
        }
    }

    Game getGame()
    {
        return game;
    }

    private PlayerState getActivePlayer()
    {
        return game.getPlayerByMarkerId(legions[activeLegionNum]);
    }

    String getActivePlayerName()
    {
        if (getActivePlayer() == null)
        {
            return null;
        }
        return getActivePlayer().getName();
    }

    String getAttackerId()
    {
        return attackerId;
    }

    Legion getAttacker()
    {
        return (Legion)getAttackingLegion();
    }

    String getDefenderId()
    {
        return defenderId;
    }

    Legion getDefender()
    {
        return (Legion)getDefendingLegion();
    }

    Legion getActiveLegion()
    {
        return getLegion(activeLegionNum);
    }

    private Legion getInactiveLegion()
    {
        return getLegion((activeLegionNum + 1) & 1);
    }

    private Legion getLegion(int legionNum)
    {
        if (legionNum == Constants.DEFENDER)
        {
            return getDefender();
        }
        else if (legionNum == Constants.ATTACKER)
        {
            return getAttacker();
        }
        else
        {
            return null;
        }
    }

    private Legion getLegionByPlayerName(String playerName)
    {
        Legion attacker = getAttacker();
        if (attacker != null && attacker.getPlayerName().equals(playerName))
        {
            return attacker;
        }
        Legion defender = getDefender();
        if (defender != null && defender.getPlayerName().equals(playerName))
        {
            return defender;
        }
        return null;
    }

    String getMasterHexLabel()
    {
        return masterHexLabel;
    }

    private MasterHex getMasterHex()
    {
        return game.getVariant().getMasterBoard()
            .getHexByLabel(masterHexLabel);
    }

    String getTerrain()
    {
        return terrain;
    }

    Constants.BattlePhase getBattlePhase()
    {
        return phase;
    }

    int getTurnNumber()
    {
        return turnNumber;
    }

    private boolean isOver()
    {
        return battleOver;
    }

    private void advancePhase()
    {
        phaseAdvancer.advancePhase();
    }

    private class BattlePhaseAdvancer implements PhaseAdvancer
    {
        private boolean again = false;

        /** Advance to the next battle phase. */
        public void advancePhase()
        {
            if (!isOver())
            {
                advancePhaseInternal();
            }
        }

        public void advancePhaseInternal()
        {
            if (phase == Constants.BattlePhase.SUMMON)
            {
                phase = Constants.BattlePhase.MOVE;
                LOGGER.log(Level.INFO, "Battle phase advances to " + phase);
                again = setupMove();
            }

            else if (phase == Constants.BattlePhase.RECRUIT)
            {
                phase = Constants.BattlePhase.MOVE;
                LOGGER.log(Level.INFO, "Battle phase advances to " + phase);
                again = setupMove();
            }

            else if (phase == Constants.BattlePhase.MOVE)
            {
                // IF the attacker makes it to the end of his first movement
                // phase without conceding, even if he left all legions
                // off-board, the defender can recruit.
                if (activeLegionNum == Constants.ATTACKER && !conceded)
                {
                    attackerEntered = true;
                }
                phase = Constants.BattlePhase.FIGHT;
                LOGGER.log(Level.INFO, "Battle phase advances to " + phase);
                again = setupFight();
            }

            else if (phase == Constants.BattlePhase.FIGHT)
            {
                // We switch the active legion between the fight and strikeback
                // phases, not at the end of the player turn.
                activeLegionNum = (activeLegionNum + 1) & 1;
                driftDamageApplied = false;
                phase = Constants.BattlePhase.STRIKEBACK;
                LOGGER.log(Level.INFO, "Battle phase advances to " + phase);
                again = setupFight();
            }

            else if (phase == Constants.BattlePhase.STRIKEBACK)
            {
                removeDeadCreatures();
                checkForElimination();
                advanceTurn();
            }

            if (again)
            {
                advancePhase();
            }
        }

        public void advanceTurn()
        {
            if (isOver())
            {
                return;
            }

            // Active legion is the one that was striking back.
            if (activeLegionNum == Constants.ATTACKER)
            {
                phase = Constants.BattlePhase.SUMMON;
                LOGGER.log(Level.INFO, getActivePlayerName()
                    + "'s battle turn, number " + turnNumber);
                again = setupSummon();
            }
            else
            {
                turnNumber++;
                if (turnNumber > 7)
                {
                    timeLoss();
                }
                else
                {
                    phase = Constants.BattlePhase.RECRUIT;
                    again = setupRecruit();
                    if (getActivePlayer() != null)
                    {
                        LOGGER.log(Level.INFO, getActivePlayerName()
                            + "'s battle turn, number " + turnNumber);
                    }
                }
            }
        }

        private void timeLoss()
        {
            LOGGER.log(Level.INFO, "Time loss");
            Legion attacker = getAttacker();
            // Time loss.  Attacker is eliminated but defender gets no points.
            if (attacker.hasTitan())
            {
                // This is the attacker's titan stack, so the defender gets 
                // his markers plus half points for his unengaged legions.
                Player player = attacker.getPlayer();
                attacker.remove();
                player.die(getDefender().getPlayerName(), true);
            }
            else
            {
                attacker.remove();
            }
            cleanup();
            again = false;
        }
    }

    private boolean setupSummon()
    {
        server.allSetupBattleSummon();
        boolean advance = true;
        if (summonState == Constants.FIRST_BLOOD)
        {
            if (getAttacker().canSummonAngel())
            {
                game.createSummonAngel(getAttacker());
                advance = false;
            }

            // This is the last chance to summon an angel until the
            // battle is over.
            summonState = Constants.TOO_LATE;
        }
        return advance;
    }

    private boolean setupRecruit()
    {
        server.allSetupBattleRecruit();
        return recruitReinforcement();
    }

    private boolean setupMove()
    {
        server.allSetupBattleMove();
        return false;
    }

    private boolean setupFight()
    {
        server.allSetupBattleFight();
        applyDriftDamage();
        return false;
    }

    int getSummonState()
    {
        return summonState;
    }

    void setSummonState(int summonState)
    {
        this.summonState = summonState;
    }

    /** Called from Game after the SummonAngel finishes. */
    void finishSummoningAngel(boolean placeNewChit)
    {
        if (placeNewChit)
        {
            Legion attacker = getAttacker();
            Critter critter = attacker.getCritter(attacker.getHeight() - 1);
            placeCritter(critter);
        }
        if (phase == Constants.BattlePhase.SUMMON)
        {
            advancePhase();
        }
    }

    private boolean recruitReinforcement()
    {
        Legion defender = getDefender();
        if (turnNumber == 4 && defender.canRecruit())
        {
            LOGGER.log(Level.FINEST, "Calling Game.reinforce()"
                + " from Battle.recruitReinforcement()");
            game.reinforce(defender);
            return false;
        }
        return true;
    }

    /** Needs to be called when reinforcement is done. */
    void doneReinforcing()
    {
        LOGGER.log(Level.FINEST, "Called Battle.doneReinforcing()");
        Legion defender = getDefender();
        if (defender.hasRecruited())
        {
            Critter newCritter = defender.getCritter(defender.getHeight() - 1);
            placeCritter(newCritter);
        }
        game.doneReinforcing();
        advancePhase();
    }

    int getCarryDamage()
    {
        return carryDamage;
    }

    void setCarryDamage(int carryDamage)
    {
        this.carryDamage = carryDamage;
    }

    /** Recursively find moves from this hex.  Return an array of hex IDs for
     *  all legal destinations.  Do not double back.  If ignoreMobileAllies
     *  is true, pretend that allied creatures that can move out of the
     *  way are not there. */
    private Set<String> findMoves(BattleHex hex, Critter critter,
        boolean flies, int movesLeft, int cameFrom,
        boolean ignoreMobileAllies, boolean first)
    {
        Set<String> set = new HashSet<String>();
        for (int i = 0; i < 6; i++)
        {
            // Do not double back.
            if (i != cameFrom)
            {
                BattleHex neighbor = hex.getNeighbor(i);
                if (neighbor != null)
                {
                    int reverseDir = (i + 3) % 6;
                    int entryCost;

                    Critter bogey = getCritter(neighbor);
                    if (bogey == null
                        || (ignoreMobileAllies
                            && bogey.getMarkerId().equals(
                                critter.getMarkerId()) && !bogey
                            .isInContact(false)))
                    {
                        entryCost = neighbor.getEntryCost(critter
                            .getCreature(), reverseDir, game
                            .getOption(Options.cumulativeSlow));
                    }
                    else
                    {
                        entryCost = BattleHex.IMPASSIBLE_COST;
                    }

                    if ((entryCost != BattleHex.IMPASSIBLE_COST)
                        && ((entryCost <= movesLeft) || (first && game
                            .getOption(Options.oneHexAllowed))))
                    {
                        // Mark that hex as a legal move.
                        set.add(neighbor.getLabel());

                        // If there are movement points remaining, continue
                        // checking moves from there.  Fliers skip this
                        // because flying is more efficient.
                        if (!flies && movesLeft > entryCost)
                        {
                            set.addAll(findMoves(neighbor, critter, flies,
                                movesLeft - entryCost, reverseDir,
                                ignoreMobileAllies, false));
                        }
                    }

                    // Fliers can fly over any hex for 1 movement point,
                    // but some Hex cannot be flown over by some creatures.
                    if (flies && movesLeft > 1
                        && neighbor.canBeFlownOverBy(critter.getCreature()))
                    {
                        set.addAll(findMoves(neighbor, critter, flies,
                            movesLeft - 1, reverseDir, ignoreMobileAllies,
                            false));
                    }
                }
            }
        }
        return set;
    }

    /** This method is called by the defender on turn 1 in a
     *  Startlisted Terrain,
     *  so we know that there are no enemies on board, and all allies
     *  are mobile.
     */
    private Set<String> findUnoccupiedStartlistHexes(
        boolean ignoreMobileAllies, String terrain)
    {
        Set<String> set = new HashSet<String>();
        Iterator<String> it = HexMap.getTowerStartList(terrain).iterator();
        while (it.hasNext())
        {
            BattleHex hex = HexMap.getHexByLabel(terrain, it.next());
            if (ignoreMobileAllies || !isOccupied(hex))
            {
                set.add(hex.getLabel());
            }
        }
        return set;
    }

    /** Find all legal moves for this critter. The returned list
     *  contains hex IDs, not hexes. */
    private Set<String> showMoves(Critter critter, boolean ignoreMobileAllies)
    {
        Set<String> set = new HashSet<String>();
        if (!critter.hasMoved() && !critter.isInContact(false))
        {
            if (HexMap.terrainHasStartlist(terrain) && (turnNumber == 1)
                && activeLegionNum == Constants.DEFENDER)
            {
                set = findUnoccupiedStartlistHexes(ignoreMobileAllies, terrain);
            }
            else
            {
                set = findMoves(critter.getCurrentHex(), critter, critter
                    .isFlier(), critter.getSkill(), -1, ignoreMobileAllies,
                    true);
            }
        }
        return set;
    }

    void undoMove(String hexLabel)
    {
        Critter critter = getCritter(hexLabel);
        if (critter != null)
        {
            critter.undoMove();
        }
        else
        {
            LOGGER.log(Level.SEVERE, "Undo move error: no critter in "
                + hexLabel);
        }
    }

    /** Mark all of the conceding player's critters as dead. */
    void concede(String playerName)
    {
        Legion legion = getLegionByPlayerName(playerName);
        String markerId = legion.getMarkerId();
        LOGGER.log(Level.INFO, markerId + " concedes the battle");
        conceded = true;

        Iterator<Critter> it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
            critter.setDead(true);
        }

        if (legion.getPlayerName().equals(getActivePlayerName()))
        {
            advancePhase();
        }
    }

    /** If any creatures were left off-board, kill them.  If they were newly
     *  summoned or recruited, unsummon or unrecruit them instead. */
    private void removeOffboardCreatures()
    {
        Legion legion = getActiveLegion();
        Iterator<Critter> it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
            if (critter.getCurrentHex().isEntrance())
            {
                critter.setDead(true);
            }
        }
        removeDeadCreatures();
    }

    private void commitMoves()
    {
        Iterator<Critter> it = getActiveLegion().getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
            critter.commitMove();
        }
    }

    void doneWithMoves()
    {
        removeOffboardCreatures();
        commitMoves();
        advancePhase();
    }

    private void applyDriftDamage()
    {
        // Drift damage is applied only once per player turn,
        //    during the strike phase.
        if (phase == Constants.BattlePhase.FIGHT && !driftDamageApplied)
        {
            Iterator<Critter> it = getAllCritters().iterator();
            driftDamageApplied = true;
            while (it.hasNext())
            {
                Critter critter = it.next();
                int dam = critter.getCurrentHex().damageToCreature(
                    critter.getCreature());
                if (dam > 0)
                {
                    critter.wound(dam);
                    LOGGER.log(Level.INFO, critter.getDescription()
                        + " takes Hex damage");
                    server.allTellHexDamageResults(critter, dam);
                }
            }
        }
    }

    boolean isDriftDamageApplied()
    {
        return driftDamageApplied;
    }

    void setDriftDamageApplied(boolean driftDamageApplied)
    {
        this.driftDamageApplied = driftDamageApplied;
    }

    void leaveCarryMode()
    {
        if (carryDamage > 0)
        {
            carryDamage = 0;
        }
        if (!carryTargets.isEmpty())
        {
            carryTargets.clear();
        }
    }

    private void removeDeadCreatures()
    {
        // Initialize these to true, and then set them to false when a
        // non-dead chit is found.
        attackerElim = true;
        defenderElim = true;

        Legion attacker = getAttacker();
        Legion defender = getDefender();

        removeDeadCreaturesFromLegion(defender);
        removeDeadCreaturesFromLegion(attacker);

        if (attacker.getPlayer() == null
            || attacker.getPlayer().isTitanEliminated())
        {
            attackerElim = true;
        }
        if (defender.getPlayer() == null
            || defender.getPlayer().isTitanEliminated())
        {
            defenderElim = true;
        }
        server.allRemoveDeadBattleChits();
    }

    private void removeDeadCreaturesFromLegion(Legion legion)
    {
        if (legion == null)
        {
            return;
        }
        List<Critter> critters = legion.getCritters();
        if (critters != null)
        {
            Iterator<Critter> it = critters.iterator();
            while (it.hasNext())
            {
                Critter critter = it.next();
                if (critter.isDead())
                {
                    cleanupOneDeadCritter(critter);
                    it.remove();
                }
                else
                // critter is alive
                {
                    if (legion == getAttacker())
                    {
                        attackerElim = false;
                    }
                    else
                    {
                        defenderElim = false;
                    }
                }
            }
        }
    }

    private void cleanupOneDeadCritter(Critter critter)
    {
        Legion legion = (Legion)critter.getLegion();
        Legion donor = null;

        // After turn 1, offboard creatures are returned to the
        // stacks or the legion they were summoned from, with
        // no points awarded.
        if (critter.getCurrentHex().isEntrance() && getTurnNumber() > 1)
        {
            if (legion == getAttacker())
            {
                // Summoned angel.
                Player player = legion.getPlayer();
                donor = player.getDonor();
                if (donor != null)
                {
                    donor.addCreature(critter.getCreature(), false);
                    server.allTellAddCreature(donor.getMarkerId(), critter
                        .getName(), true, Constants.reasonUndoSummon);
                    // This summon doesn't count; the player can
                    // summon again later this turn.
                    LOGGER.log(Level.INFO, "undosummon critter "
                        + critter.getName() + " back to marker "
                        + donor.getMarkerId() + "");
                    player.setSummoned(false);
                }
                else
                {
                    LOGGER.log(Level.SEVERE,
                        "Null donor in Battle.cleanupOneDeadCritter()");
                }
            }
            else
            {
                // Reinforcement.
                // This recruit doesn't count.
                legion.setRecruitName(null);
            }
        }
        else if (legion == getAttacker())
        {
            getDefender().addToBattleTally(critter.getPointValue());
        }
        else
        // defender
        {
            getAttacker().addToBattleTally(critter.getPointValue());

            // Creatures left offboard do not trigger angel
            // summoning.
            if (summonState == Constants.NO_KILLS
                && !critter.getCurrentHex().isEntrance())
            {
                summonState = Constants.FIRST_BLOOD;
            }
        }

        // If an angel or archangel was returned to its donor instead of 
        // the stack, then don't put it back on the stack.
        legion.prepareToRemoveCritter(critter, donor == null, true);

        if (critter.isTitan())
        {
            legion.getPlayer().eliminateTitan();
        }
    }

    private void checkForElimination()
    {
        Legion attacker = getAttacker();
        Legion defender = getDefender();
        Player attackerPlayer = attacker.getPlayer();
        Player defenderPlayer = defender.getPlayer();

        boolean attackerTitanDead = attackerPlayer.isTitanEliminated();
        boolean defenderTitanDead = defenderPlayer.isTitanEliminated();

        // Check for mutual Titan elimination.
        if (attackerTitanDead && defenderTitanDead)
        {
            // Nobody gets any points.
            // Make defender die first, to simplify turn advancing.
            defender.getPlayer().die(null, false);
            attacker.getPlayer().die(null, true);
            cleanup();
        }

        // Check for single Titan elimination.
        else if (attackerTitanDead)
        {
            String slayerName = defender.getPlayerName();
            if (defenderElim)
            {
                defender.remove();
            }
            else
            {
                pointsScored = defender.getBattleTally();
                defender.addBattleTallyToPoints();
            }
            attacker.getPlayer().die(slayerName, true);
            cleanup();
        }
        else if (defenderTitanDead)
        {
            String slayerName = attacker.getPlayerName();
            if (attackerElim)
            {
                attacker.remove();
            }
            else
            {
                pointsScored = attacker.getBattleTally();
                attacker.addBattleTallyToPoints();
            }
            defender.getPlayer().die(slayerName, true);
            cleanup();
        }

        // Check for mutual legion elimination.
        else if (attackerElim && defenderElim)
        {
            attacker.remove();
            defender.remove();
            cleanup();
        }

        // Check for single legion elimination.
        else if (attackerElim)
        {
            pointsScored = defender.getBattleTally();
            defender.addBattleTallyToPoints();
            attacker.remove();
            cleanup();
        }
        else if (defenderElim)
        {
            pointsScored = attacker.getBattleTally();
            attacker.addBattleTallyToPoints();
            defender.remove();
            cleanup();
        }
    }

    private void commitStrikes()
    {
        Legion legion = getActiveLegion();
        if (legion != null)
        {
            Iterator<Critter> it = legion.getCritters().iterator();
            while (it.hasNext())
            {
                Critter critter = it.next();
                critter.setStruck(false);
            }
        }
    }

    private boolean isForcedStrikeRemaining()
    {
        Legion legion = getActiveLegion();
        if (legion != null)
        {
            Iterator<Critter> it = legion.getCritters().iterator();
            while (it.hasNext())
            {
                Critter critter = it.next();
                if (!critter.hasStruck() && critter.isInContact(false))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /** Return true if okay, or false if forced strikes remain. */
    boolean doneWithStrikes()
    {
        // Advance only if there are no unresolved strikes.
        Constants.BattlePhase phase = getBattlePhase();
        if (!isForcedStrikeRemaining() && phase.isFightPhase())
        {
            commitStrikes();
            advancePhase();
            return true;
        }

        LOGGER.log(Level.SEVERE, server.getPlayerName()
            + " called battle.doneWithStrikes() illegally");
        return false;
    }

    /** Return a set of hex labels for hexes containing targets that the
     *  critter may strike.  Only include rangestrikes if rangestrike
     *  is true. */
    Set<String> findStrikes(Critter critter, boolean rangestrike)
    {
        Set<String> set = new HashSet<String>();

        // Each creature may strike only once per turn.
        if (critter.hasStruck())
        {
            return set;
        }

        PlayerState player = critter.getPlayer();
        BattleHex currentHex = critter.getCurrentHex();

        boolean adjacentEnemy = false;

        // First mark and count normal strikes.
        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not engaged.
            if (!currentHex.isCliff(i))
            {
                BattleHex targetHex = currentHex.getNeighbor(i);
                if (targetHex != null && isOccupied(targetHex))
                {
                    Critter target = getCritter(targetHex);
                    if (target.getPlayer() != player)
                    {
                        adjacentEnemy = true;
                        if (!target.isDead())
                        {
                            set.add(targetHex.getLabel());
                        }
                    }
                }
            }
        }

        // Then do rangestrikes if applicable.  Rangestrikes are not allowed
        // if the creature can strike normally, so only look for them if
        // no targets have yet been found.
        if (rangestrike && !adjacentEnemy && critter.isRangestriker()
            && getBattlePhase() != Constants.BattlePhase.STRIKEBACK
            && critter.getLegion() == getActiveLegion())
        {
            Iterator<Critter> it = getInactiveLegion().getCritters()
                .iterator();
            while (it.hasNext())
            {
                Critter target = it.next();
                if (!target.isDead())
                {
                    BattleHex targetHex = target.getCurrentHex();
                    if (isRangestrikePossible(critter, target))
                    {
                        set.add(targetHex.getLabel());
                    }
                }
            }
        }
        return set;
    }

    /** Return the set of hex labels for hexes with valid carry targets. */
    Set<String> getCarryTargets()
    {
        return Collections.unmodifiableSet(carryTargets);
    }

    Set<String> getCarryTargetDescriptions()
    {
        Set<String> set = new HashSet<String>();
        Iterator<String> it = getCarryTargets().iterator();
        while (it.hasNext())
        {
            String hexLabel = it.next();
            Critter critter = getCritter(hexLabel);
            set.add(critter.getDescription());
        }
        return set;
    }

    void clearCarryTargets()
    {
        carryTargets.clear();
    }

    void setCarryTargets(Set<String> carryTargets)
    {
        this.carryTargets.clear();
        this.carryTargets.addAll(carryTargets);
    }

    void addCarryTarget(String hexLabel)
    {
        carryTargets.add(hexLabel);
    }

    void applyCarries(Critter target)
    {
        if (!carryTargets.contains(target.getCurrentHex().getLabel()))
        {
            LOGGER.log(Level.WARNING, "Tried illegal carry to "
                + target.getDescription());
            return;
        }
        int dealt = carryDamage;
        carryDamage = target.wound(carryDamage);
        dealt -= carryDamage;
        carryTargets.remove(target.getCurrentHex().getLabel());

        LOGGER.log(Level.INFO, dealt
            + (dealt == 1 ? " hit carries to " : " hits carry to ")
            + target.getDescription());

        if (carryDamage <= 0 || getCarryTargets().isEmpty())
        {
            leaveCarryMode();
        }
        else
        {
            LOGGER.log(Level.INFO, carryDamage
                + (carryDamage == 1 ? " carry available"
                    : " carries available"));
        }
        server.allTellCarryResults(target, dealt, carryDamage,
            getCarryTargets());
    }

    /** Return the range in hexes from hex1 to hex2.  Titan ranges are
     *  inclusive at both ends. */
    static int getRange(BattleHex hex1, BattleHex hex2, boolean allowEntrance)
    {
        if (hex1 == null || hex2 == null)
        {
            LOGGER.log(Level.WARNING, "passed null hex to getRange()");
            return Constants.OUT_OF_RANGE;
        }
        if (hex1.isEntrance() || hex2.isEntrance())
        {
            if (allowEntrance)
            {
                // The range to an entrance is the range to the
                // closest of its neighbors, plus one.
                if (hex1.isEntrance())
                {
                    return 1 + minRangeToNeighbor(hex1, hex2);
                }
                else
                // hex2.isEntrance()
                {
                    return 1 + minRangeToNeighbor(hex2, hex1);
                }
            }
            else
            {
                // It's out of range.  No need to do the math.
                return Constants.OUT_OF_RANGE;
            }
        }

        int x1 = hex1.getXCoord();
        double y1 = hex1.getYCoord();
        int x2 = hex2.getXCoord();
        double y2 = hex2.getYCoord();

        // Hexes with odd X coordinates are pushed down half a hex.
        if ((x1 & 1) == 1)
        {
            y1 += 0.5;
        }
        if ((x2 & 1) == 1)
        {
            y2 += 0.5;
        }

        double xDist = Math.abs(x2 - x1);
        double yDist = Math.abs(y2 - y1);

        if (xDist >= 2 * yDist)
        {
            return (int)Math.ceil(xDist + 1);
        }
        else if (xDist >= yDist)
        {
            return (int)Math.floor(xDist + 2);
        }
        else if (yDist >= 2 * xDist)
        {
            return (int)Math.ceil(yDist + 1);
        }
        else
        {
            return (int)Math.floor(yDist + 2);
        }
    }

    /** Return the minimum range from any neighbor of hex1 to hex2. */
    private static int minRangeToNeighbor(BattleHex hex1, BattleHex hex2)
    {
        int min = Constants.OUT_OF_RANGE;
        for (int i = 0; i < 6; i++)
        {
            BattleHex hex = hex1.getNeighbor(i);
            if (hex != null)
            {
                int range = getRange(hex, hex2, false);
                if (range < min)
                {
                    min = range;
                }
            }
        }
        return min;
    }

    /** Caller must ensure that yDist != 0 */
    private static boolean toLeft(double xDist, double yDist)
    {
        double ratio = xDist / yDist;
        if (ratio >= 1.5 || (ratio >= 0 && ratio <= .75)
            || (ratio >= -1.5 && ratio <= -.75))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private static boolean isObstacle(char hexside)
    {
        return (hexside != ' ') && (hexside != 'r');
    }

    /** Check LOS, going to the left of hexspines if argument left is true, or
     *  to the right if it is false. */
    private boolean isLOSBlockedDir(BattleHex initialHex,
        BattleHex currentHex, BattleHex finalHex, boolean left,
        int strikeElevation, boolean strikerAtop, boolean strikerAtopCliff,
        boolean midObstacle, boolean midCliff, boolean midChit,
        int totalObstacles)
    {
        boolean targetAtop = false;
        boolean targetAtopCliff = false;

        if (currentHex == finalHex)
        {
            return false;
        }

        // Offboard hexes are not allowed.
        if (currentHex.getXCoord() == -1 || finalHex.getXCoord() == -1)
        {
            return true;
        }

        int direction = getDirection(currentHex, finalHex, left);

        BattleHex nextHex = currentHex.getNeighbor(direction);

        if (nextHex == null)
        {
            return true;
        }

        char hexside = currentHex.getHexside(direction);
        char hexside2 = currentHex.getOppositeHexside(direction);

        if (currentHex == initialHex)
        {
            if (isObstacle(hexside))
            {
                strikerAtop = true;
                totalObstacles++;
                if (hexside == 'c')
                {
                    strikerAtopCliff = true;
                }
            }

            if (isObstacle(hexside2))
            {
                midObstacle = true;
                totalObstacles++;
                if (hexside2 == 'c')
                {
                    midCliff = true;
                }
                if (hexside2 == 'w')
                {
                    // Down a wall -- blocked
                    return true;
                }
            }
        }
        else if (nextHex == finalHex)
        {
            if (isObstacle(hexside))
            {
                midObstacle = true;
                totalObstacles++;
                if (hexside == 'c')
                {
                    midCliff = true;
                }
                if (hexside == 'w')
                {
                    // Down a wall -- blocked
                    return true;
                }
            }

            if (isObstacle(hexside2))
            {
                targetAtop = true;
                totalObstacles++;
                if (hexside2 == 'c')
                {
                    targetAtopCliff = true;
                }
            }

            if (midChit && !targetAtopCliff)
            {
                return true;
            }

            if (midCliff && !strikerAtopCliff && !targetAtopCliff)
            {
                return true;
            }

            if (midObstacle && !strikerAtop && !targetAtop)
            {
                return true;
            }

            // If there are three slopes, striker and target must each
            //     be atop one.
            if (totalObstacles >= 3 && (!strikerAtop || !targetAtop)
                && (!strikerAtopCliff && !targetAtopCliff))
            {
                return true;
            }

            // Success!
            return false;
        }
        else
        // not leaving first or entering last hex
        {
            if (midChit)
            {
                // We're not in the initial or final hex, and we have already
                // marked an mid chit, so it's not adjacent to the base of a
                // cliff that the target is atop.
                return true;
            }

            if (isObstacle(hexside) || isObstacle(hexside2))
            {
                midObstacle = true;
                totalObstacles++;
                if (hexside == 'c' || hexside2 == 'c')
                {
                    midCliff = true;
                }
            }
        }

        if (nextHex.blocksLineOfSight())
        {
            return true;
        }

        // Creatures block LOS, unless both striker and target are at higher
        //     elevation than the creature, or unless the creature is at
        //     the base of a cliff and the striker or target is atop it.
        if (isOccupied(nextHex) && nextHex.getElevation() >= strikeElevation
            && (!strikerAtopCliff || currentHex != initialHex))
        {
            midChit = true;
        }

        return isLOSBlockedDir(initialHex, nextHex, finalHex, left,
            strikeElevation, strikerAtop, strikerAtopCliff, midObstacle,
            midCliff, midChit, totalObstacles);
    }

    /** Check to see if the LOS from hex1 to hex2 is blocked.  If the LOS
     *  lies along a hexspine, check both and return true only if both are
     *  blocked. */
    boolean isLOSBlocked(BattleHex hex1, BattleHex hex2)
    {
        if (hex1 == hex2)
        {
            return false;
        }

        int x1 = hex1.getXCoord();
        double y1 = hex1.getYCoord();
        int x2 = hex2.getXCoord();
        double y2 = hex2.getYCoord();

        // Offboard hexes are not allowed.
        if (x1 == -1 || x2 == -1)
        {
            return true;
        }

        // Hexes with odd X coordinates are pushed down half a hex.
        if ((x1 & 1) == 1)
        {
            y1 += 0.5;
        }
        if ((x2 & 1) == 1)
        {
            y2 += 0.5;
        }

        double xDist = x2 - x1;
        double yDist = y2 - y1;

        // Creatures below the level of the strike do not block LOS.
        int strikeElevation = Math.min(hex1.getElevation(), hex2
            .getElevation());

        if (yDist == 0 || Math.abs(yDist) == 1.5 * Math.abs(xDist))
        {
            // Hexspine; try both sides.
            return (isLOSBlockedDir(hex1, hex1, hex2, true, strikeElevation,
                false, false, false, false, false, 0) && isLOSBlockedDir(hex1,
                hex1, hex2, false, strikeElevation, false, false, false,
                false, false, 0));
        }
        else
        {
            return isLOSBlockedDir(hex1, hex1, hex2, toLeft(xDist, yDist),
                strikeElevation, false, false, false, false, false, 0);
        }
    }

    /** Return true if the rangestrike is possible. */
    private boolean isRangestrikePossible(Critter critter, Critter target)
    {
        BattleHex currentHex = critter.getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();

        int range = getRange(currentHex, targetHex, false);
        int skill = critter.getSkill();

        if (range > skill)
        {
            return false;
        }

        // Only magicMissile can rangestrike at range 2, rangestrike Lords,
        // or rangestrike without LOS.
        else if (!critter.useMagicMissile()
            && (range < 3 || target.isLord() || isLOSBlocked(currentHex,
                targetHex)))
        {
            return false;
        }

        return true;
    }

    /** Return the hexside direction of the path from hex1 to hex2.
     *  Sometimes two directions are possible.  If the left parameter
     *  is set, the direction further left will be given.  Otherwise,
     *  the direction further right will be given. */
    static int getDirection(BattleHex hex1, BattleHex hex2, boolean left)
    {
        if (hex1 == hex2)
        {
            return -1;
        }

        int x1 = hex1.getXCoord();
        double y1 = hex1.getYCoord();
        int x2 = hex2.getXCoord();
        double y2 = hex2.getYCoord();

        // Offboard creatures are not allowed.
        if (x1 == -1 || x2 == -1)
        {
            return -1;
        }

        // Hexes with odd X coordinates are pushed down half a hex.
        if ((x1 & 1) == 1)
        {
            y1 += 0.5;
        }
        if ((x2 & 1) == 1)
        {
            y2 += 0.5;
        }

        int xDist = x2 - x1;
        double yDist = y2 - y1;
        double xDistAndAHalf = 1.5 * xDist;

        if (xDist >= 0)
        {
            if (yDist > xDistAndAHalf)
            {
                return 3;
            }
            else if (yDist == xDistAndAHalf)
            {
                if (left)
                {
                    return 2;
                }
                else
                {
                    return 3;
                }
            }
            else if (yDist < -xDistAndAHalf)
            {
                return 0;
            }
            else if (yDist == -xDistAndAHalf)
            {
                if (left)
                {
                    return 0;
                }
                else
                {
                    return 1;
                }
            }
            else if (yDist > 0)
            {
                return 2;
            }
            else if (yDist < 0)
            {
                return 1;
            }
            else
            // yDist == 0
            {
                if (left)
                {
                    return 1;
                }
                else
                {
                    return 2;
                }
            }
        }
        else
        // xDist < 0
        {
            if (yDist < xDistAndAHalf)
            {
                return 0;
            }
            else if (yDist == xDistAndAHalf)
            {
                if (left)
                {
                    return 5;
                }
                else
                {
                    return 0;
                }
            }
            else if (yDist > -xDistAndAHalf)
            {
                return 3;
            }
            else if (yDist == -xDistAndAHalf)
            {
                if (left)
                {
                    return 3;
                }
                else
                {
                    return 4;
                }
            }
            else if (yDist > 0)
            {
                return 4;
            }
            else if (yDist < 0)
            {
                return 5;
            }
            else
            // yDist == 0
            {
                if (left)
                {
                    return 4;
                }
                else
                {
                    return 5;
                }
            }
        }
    }

    /** Return the number of intervening bramble hexes.  If LOS is along a
     *  hexspine, go left if argument left is true, right otherwise.  If
     *  LOS is blocked, return a large number. */
    private int countBrambleHexesDir(BattleHex hex1, BattleHex hex2,
        boolean left, int previousCount)
    {
        int count = previousCount;

        // Offboard hexes are not allowed.
        if (hex1.getXCoord() == -1 || hex2.getXCoord() == -1)
        {
            return Constants.BIGNUM;
        }

        int direction = getDirection(hex1, hex2, left);

        BattleHex nextHex = hex1.getNeighbor(direction);
        if (nextHex == null)
        {
            return Constants.BIGNUM;
        }

        if (nextHex == hex2)
        {
            // Success!
            return count;
        }

        // Add one if it's bramble.
        if (nextHex.getTerrain().equals(HazardTerrain.BRAMBLES))
        {
            count++;
        }

        return countBrambleHexesDir(nextHex, hex2, left, count);
    }

    // Return the number of intervening bramble hexes.  If LOS is along a
    // hexspine and there are two unblocked choices, pick the lower one.
    int countBrambleHexes(BattleHex hex1, BattleHex hex2)
    {
        if (hex1 == hex2)
        {
            return 0;
        }

        int x1 = hex1.getXCoord();
        double y1 = hex1.getYCoord();
        int x2 = hex2.getXCoord();
        double y2 = hex2.getYCoord();

        // Offboard hexes are not allowed.
        if (x1 == -1 || x2 == -1)
        {
            return Constants.BIGNUM;
        }

        // Hexes with odd X coordinates are pushed down half a hex.
        if ((x1 & 1) == 1)
        {
            y1 += 0.5;
        }
        if ((x2 & 1) == 1)
        {
            y2 += 0.5;
        }

        double xDist = x2 - x1;
        double yDist = y2 - y1;

        if (yDist == 0 || Math.abs(yDist) == 1.5 * Math.abs(xDist))
        {
            int strikeElevation = Math.min(hex1.getElevation(), hex2
                .getElevation());
            // Hexspine; try unblocked side(s)
            if (isLOSBlockedDir(hex1, hex1, hex2, true, strikeElevation,
                false, false, false, false, false, 0))
            {
                return countBrambleHexesDir(hex1, hex2, false, 0);
            }
            else if (isLOSBlockedDir(hex1, hex1, hex2, false, strikeElevation,
                false, false, false, false, false, 0))
            {
                return countBrambleHexesDir(hex1, hex2, true, 0);
            }
            else
            {
                return Math.min(countBrambleHexesDir(hex1, hex2, true, 0),
                    countBrambleHexesDir(hex1, hex2, false, 0));
            }
        }
        else
        {
            return countBrambleHexesDir(hex1, hex2, toLeft(xDist, yDist), 0);
        }
    }

    /** If legal, move critter to hex and return true. Else return false. */
    boolean doMove(int tag, String hexLabel)
    {
        Critter critter = getActiveLegion().getCritterByTag(tag);
        BattleHex hex = BattleMap.getHexByLabel(terrain, hexLabel);
        if (critter == null)
        {
            return false;
        }

        // Allow null moves.
        if (hex.equals(critter.getCurrentHex()))
        {
            LOGGER
                .log(Level.INFO, critter.getDescription() + " does not move");
            // Call moveToHex() anyway to sync client.
            critter.moveToHex(hex, true);
            return true;
        }
        else if (showMoves(critter, false).contains(hexLabel))
        {
            LOGGER.log(Level.INFO, critter.getName() + " moves from "
                + critter.getCurrentHex().getLabel() + " to " + hexLabel);
            critter.moveToHex(hex, true);
            return true;
        }
        else
        {
            Legion legion = getActiveLegion();
            String markerId = legion.getMarkerId();
            LOGGER.log(Level.WARNING, critter.getName() + " in "
                + critter.getCurrentHex().getLabel()
                + " tried to illegally move to " + hexLabel + " in " + terrain
                + " (" + attackerId + " attacking " + defenderId
                + ", active: " + markerId + ")");
            return false;
        }
    }

    private void cleanup()
    {
        battleOver = true;
        game.finishBattle(masterHexLabel, attackerEntered, pointsScored,
            turnNumber);
    }

    /** Return a list of all critters in the battle. */
    private List<Critter> getAllCritters()
    {
        List<Critter> critters = new ArrayList<Critter>();
        Legion defender = getDefender();
        if (defender != null)
        {
            critters.addAll(defender.getCritters());
        }
        Legion attacker = getAttacker();
        if (attacker != null)
        {
            critters.addAll(attacker.getCritters());
        }
        return critters;
    }

    private boolean isOccupied(String hexLabel)
    {
        Iterator<Critter> it = getAllCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
            if (hexLabel.equals(critter.getCurrentHex().getLabel()))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isOccupied(BattleHex hex)
    {
        return isOccupied(hex.getLabel());
    }

    Critter getCritter(BattleHex hex)
    {
        return getCritter(hex.getLabel());
    }

    Critter getCritter(String hexLabel)
    {
        Iterator<Critter> it = getAllCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = it.next();
            if (hexLabel.equals(critter.getCurrentHex().getLabel()))
            {
                return critter;
            }
        }
        return null;
    }
}
