package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.HexMap;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.events.UndoSummonEvent;
import net.sf.colossus.util.CompareDoubles;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.HazardTerrain;
import net.sf.colossus.variant.MasterBoardTerrain;
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

public final class BattleServerSide extends Battle
{
    public static enum AngelSummoningStates
    {
        NO_KILLS, FIRST_BLOOD, TOO_LATE
    }

    public static enum LegionTags
    {
        DEFENDER, ATTACKER
    }

    private static final Logger LOGGER = Logger
        .getLogger(BattleServerSide.class.getName());

    private Server server;
    private LegionTags activeLegionTag;
    private final MasterHex masterHex;
    private int turnNumber;
    private BattlePhase phase;
    private AngelSummoningStates summonState = AngelSummoningStates.NO_KILLS;
    private int carryDamage;
    private boolean attackerElim;
    private boolean defenderElim;
    private boolean battleOver;
    private boolean attackerEntered;
    private boolean conceded;
    private boolean driftDamageApplied = false;

    /**
     * Set of hexes for valid carry targets
     */
    private final Set<BattleHex> carryTargets = new HashSet<BattleHex>();
    private final PhaseAdvancer phaseAdvancer = new BattlePhaseAdvancer();
    private int pointsScored = 0;

    BattleServerSide(GameServerSide game, Legion attacker, Legion defender,
        LegionTags activeLegionTag, MasterHex masterHex,
        int turnNumber, BattlePhase phase)
    {
        super(game, attacker, defender, masterHex.getTerrain());

        this.server = game.getServer();
        this.masterHex = masterHex;
        this.activeLegionTag = activeLegionTag;
        this.turnNumber = turnNumber;
        this.phase = phase;

        // Set defender's entry side opposite attacker's.
        defender.setEntrySide(attacker.getEntrySide().getOpposingSide());
        // Make sure defender can recruit, even if savegame is off.
        defender.setRecruit(null);

        // Make sure donor is null, if it remained set from an earlier battle
        ((LegionServerSide)attacker).getPlayer().setDonor(null);

        LOGGER.info(attacker + " (" + attacker.getPlayer() + ") attacks "
            + defender + " (" + defender.getPlayer() + ")" + " in "
            + masterHex);

        placeLegion(attacker);
        placeLegion(defender);
    }

    // Used when loading a game
    public void setServer(Server server)
    {
        this.server = server;
    }

    public void cleanRefs()
    {
        this.server = null;
    }

    private void placeLegion(Legion legion)
    {
        BattleHex entrance = HexMap.getEntrance(masterHex.getTerrain(),
            legion.getEntrySide());
        for (CreatureServerSide critter : ((LegionServerSide)legion)
            .getCreatures())
        {

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

            critter.setBattleInfo(currentHex, startingHex, this);
        }
    }

    private void placeCritter(CreatureServerSide critter)
    {
        BattleHex entrance = HexMap.getEntrance(masterHex.getTerrain(),
            critter.getLegion().getEntrySide());
        critter.setBattleInfo(entrance, entrance, this);
        server.allPlaceNewChit(critter);
    }

    private void initBattleChits(LegionServerSide legion)
    {
        for (CreatureServerSide creature : legion.getCreatures())
        {
            server.allPlaceNewChit(creature);
        }
    }

    /** We need to do two-stage construction so that game.battle
     *  is non-null earlier. */
    void init()
    {
        server.allInitBattle(masterHex);
        initBattleChits(getAttackingLegion());
        initBattleChits(getDefendingLegion());

        boolean advance = false;
        switch (phase)
        {
            case SUMMON:
                advance = setupSummon();
                break;
            case RECRUIT:
                advance = setupRecruit();
                break;
            case MOVE:
                advance = setupMove();
                break;
            default:
                if (phase.isFightPhase())
                {
                    advance = setupFight();
                }
                else
                {
                    LOGGER.log(Level.SEVERE, "Bogus phase");
                }
        }
        if (advance)
        {
            advancePhase();
        }
    }

    /**
     * Override with covariant return type to ease transition into new model.
     */
    @Override
    public GameServerSide getGame()
    {
        return (GameServerSide)super.getGame();
    }

    Player getActivePlayer()
    {
        return getLegion(activeLegionTag).getPlayer();
    }

    /**
     * Override with covariant return type to ease transition into new model.
     */
    @Override
    public LegionServerSide getAttackingLegion()
    {
        return (LegionServerSide)super.getAttackingLegion();
    }

    /**
     * Override with covariant return type to ease transition into new model.
     */
    @Override
    public LegionServerSide getDefendingLegion()
    {
        return (LegionServerSide)super.getDefendingLegion();
    }

    LegionServerSide getActiveLegion()
    {
        return getLegion(activeLegionTag);
    }

    private LegionServerSide getInactiveLegion()
    {
        return getLegion((activeLegionTag == LegionTags.ATTACKER) ? LegionTags.DEFENDER
            : LegionTags.ATTACKER);
    }

    private LegionServerSide getLegion(LegionTags legionTag)
    {
        switch (legionTag)
        {
            case DEFENDER:
                return getDefendingLegion();
            case ATTACKER:
                return getAttackingLegion();
        }
        throw new IllegalArgumentException("Parameter out of range");
    }

    public MasterHex getMasterHex()
    {
        return masterHex;
    }

    BattlePhase getBattlePhase()
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
            if (phase == BattlePhase.SUMMON)
            {
                phase = BattlePhase.MOVE;
                LOGGER.log(Level.INFO, "Battle phase advances to " + phase);
                again = setupMove();
            }

            else if (phase == BattlePhase.RECRUIT)
            {
                phase = BattlePhase.MOVE;
                LOGGER.log(Level.INFO, "Battle phase advances to " + phase);
                again = setupMove();
            }

            else if (phase == BattlePhase.MOVE)
            {
                // IF the attacker makes it to the end of his first movement
                // phase without conceding, even if he left all legions
                // off-board, the defender can recruit.
                if (activeLegionTag == LegionTags.ATTACKER
                    && !conceded)
                {
                    attackerEntered = true;
                }
                phase = BattlePhase.FIGHT;
                LOGGER.log(Level.INFO, "Battle phase advances to " + phase);
                again = setupFight();
            }

            else if (phase == BattlePhase.FIGHT)
            {
                // We switch the active legion between the fight and strikeback
                // phases, not at the end of the player turn.
                activeLegionTag = (activeLegionTag == LegionTags.ATTACKER) ? LegionTags.DEFENDER
                    : LegionTags.ATTACKER;
                driftDamageApplied = false;
                phase = BattlePhase.STRIKEBACK;
                LOGGER.log(Level.INFO, "Battle phase advances to " + phase);
                again = setupFight();
            }

            else if (phase == BattlePhase.STRIKEBACK)
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
            if (activeLegionTag == LegionTags.ATTACKER)
            {
                phase = BattlePhase.SUMMON;
                LOGGER.log(Level.INFO, getActivePlayer()
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
                    phase = BattlePhase.RECRUIT;
                    again = setupRecruit();
                    if (getActivePlayer() != null)
                    {
                        LOGGER.log(Level.INFO, getActivePlayer()
                            + "'s battle turn, number " + turnNumber);
                    }
                }
            }
        }

        private void timeLoss()
        {
            LOGGER.log(Level.INFO, "Time loss");
            LegionServerSide attacker = getAttackingLegion();
            // Time loss.  Attacker is eliminated but defender gets no points.
            if (attacker.hasTitan())
            {
                // This is the attacker's titan stack, so the defender gets
                // his markers plus half points for his unengaged legions.
                PlayerServerSide player = attacker.getPlayer();
                attacker.remove();
                player.die(getDefendingLegion().getPlayer());
                getGame().checkForVictory();
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
        if (summonState == AngelSummoningStates.FIRST_BLOOD)
        {
            if (getAttackingLegion().canSummonAngel())
            {
                getGame().createSummonAngel(getAttackingLegion());
                advance = false;
            }

            // This is the last chance to summon an angel until the
            // battle is over.
            summonState = AngelSummoningStates.TOO_LATE;
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

    AngelSummoningStates getSummonState()
    {
        return summonState;
    }

    void setSummonState(AngelSummoningStates summonState)
    {
        this.summonState = summonState;
    }

    /** Called from Game after the SummonAngel finishes. */
    void finishSummoningAngel(boolean placeNewChit)
    {
        if (placeNewChit)
        {
            LegionServerSide attacker = getAttackingLegion();
            CreatureServerSide critter = attacker.getCritter(attacker
                .getHeight() - 1);
            placeCritter(critter);
        }
        if (phase == BattlePhase.SUMMON)
        {
            advancePhase();
        }
    }

    private boolean recruitReinforcement()
    {
        LegionServerSide defender = getDefendingLegion();
        if (turnNumber == 4 && defender.canRecruit())
        {
            LOGGER.log(Level.FINEST, "Calling Game.reinforce()"
                + " from Battle.recruitReinforcement()");
            getGame().reinforce(defender);
            return false;
        }
        return true;
    }

    /** Needs to be called when reinforcement is done. */
    void doneReinforcing()
    {
        LOGGER.log(Level.FINEST, "Called Battle.doneReinforcing()");
        LegionServerSide defender = getDefendingLegion();
        if (defender.hasRecruited())
        {
            CreatureServerSide newCritter = defender.getCritter(defender
                .getHeight() - 1);
            placeCritter(newCritter);
        }
        getGame().doneReinforcing();
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

    /** Recursively find moves from this hex.  Return a set of string hex IDs
     *  for all legal destinations.  Do not double back.  If ignoreMobileAllies
     *  is true, pretend that allied creatures that can move out of the
     *  way are not there. */
    private Set<BattleHex> findMoves(BattleHex hex, CreatureServerSide critter,
        boolean flies, int movesLeft, int cameFrom,
        boolean ignoreMobileAllies, boolean first)
    {
        Set<BattleHex> set = new HashSet<BattleHex>();
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

                    CreatureServerSide bogey = getCritter(neighbor);
                    if (bogey == null
                        || (ignoreMobileAllies
                            && bogey.getMarkerId().equals(
                                critter.getMarkerId()) && !bogey
                            .isInContact(false)))
                    {
                        entryCost = neighbor.getEntryCost(critter.getType(),
                            reverseDir, getGame().getOption(
                                Options.cumulativeSlow));
                    }
                    else
                    {
                        entryCost = BattleHex.IMPASSIBLE_COST;
                    }

                    if ((entryCost != BattleHex.IMPASSIBLE_COST)
                        && ((entryCost <= movesLeft) || (first && getGame()
                            .getOption(Options.oneHexAllowed))))
                    {
                        // Mark that hex as a legal move.
                        set.add(neighbor);

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
                        && neighbor.canBeFlownOverBy(critter.getType()))
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
    private Set<BattleHex> findUnoccupiedStartlistHexes(
        boolean ignoreMobileAllies, MasterBoardTerrain terrain)
    {
        assert terrain != null;
        Set<BattleHex> set = new HashSet<BattleHex>();
        for (String hexLabel : terrain.getStartList())
        {
            BattleHex hex = HexMap.getHexByLabel(terrain, hexLabel);
            if (ignoreMobileAllies || !isOccupied(hex))
            {
                set.add(hex);
            }
        }
        return set;
    }

    /**
     * Find all legal moves for this critter.
     */
    private Set<BattleHex> showMoves(CreatureServerSide critter,
        boolean ignoreMobileAllies)
    {
        Set<BattleHex> set = new HashSet<BattleHex>();
        if (!critter.hasMoved() && !critter.isInContact(false))
        {
            if (masterHex.getTerrain().hasStartList() && (turnNumber == 1)
                && activeLegionTag == LegionTags.DEFENDER)
            {
                set = findUnoccupiedStartlistHexes(ignoreMobileAllies,
                    masterHex.getTerrain());
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

    void undoMove(BattleHex hex)
    {
        CreatureServerSide critter = getCritter(hex);
        if (critter != null)
        {
            critter.undoMove();
        }
        else
        {
            LOGGER.log(Level.SEVERE, "Undo move error: no critter in "
                + hex.getLabel());
        }
    }

    /** Mark all of the conceding player's critters as dead. */
    void concede(Player player)
    {
        Legion legion = getLegionByPlayer(player);
        String markerId = legion.getMarkerId();
        LOGGER.log(Level.INFO, markerId + " concedes the battle");
        conceded = true;

        for (Creature creature : legion.getCreatures())
        {
            creature.setDead(true);
        }

        if (legion.getPlayer().equals(getActivePlayer()))
        {
            advancePhase();
        }
    }

    /** If any creatures were left off-board, kill them.  If they were newly
     *  summoned or recruited, unsummon or unrecruit them instead. */
    private void removeOffboardCreatures()
    {
        LegionServerSide legion = getActiveLegion();
        for (Creature critter : legion.getCreatures())
        {
            if (critter.getCurrentHex().isEntrance())
            {
                critter.setDead(true);
            }
        }
        removeDeadCreatures();
    }

    private void commitMoves()
    {
        for (Creature critter : getActiveLegion().getCreatures())
        {
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
        if (phase == BattlePhase.FIGHT && !driftDamageApplied)
        {
            driftDamageApplied = true;
            for (CreatureServerSide critter : getAllCritters())
            {
                int dam = critter.getCurrentHex().damageToCreature(
                    critter.getType());
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

        LegionServerSide attacker = getAttackingLegion();
        LegionServerSide defender = getDefendingLegion();

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
        // to update number of creatures in status window:
        server.allUpdatePlayerInfo();
    }

    private void removeDeadCreaturesFromLegion(LegionServerSide legion)
    {
        if (legion == null)
        {
            return;
        }
        List<CreatureServerSide> critters = legion.getCreatures();
        if (critters != null)
        {
            Iterator<CreatureServerSide> it = critters.iterator();
            while (it.hasNext())
            {
                CreatureServerSide critter = it.next();
                if (critter.isDead())
                {
                    cleanupOneDeadCritter(critter);
                    it.remove();
                }
                else
                // critter is alive
                {
                    if (legion == getAttackingLegion())
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

    private void cleanupOneDeadCritter(Creature critter)
    {
        Legion legion = critter.getLegion();
        LegionServerSide donor = null;

        PlayerServerSide player = (PlayerServerSide)legion.getPlayer();

        // After turn 1, off-board creatures are returned to the
        // stacks or the legion they were summoned from, with
        // no points awarded.
        if (critter.getCurrentHex().isEntrance() && getTurnNumber() > 1)
        {
            if (legion == getAttackingLegion())
            {
                // Summoned angel.
                donor = player.getDonor();
                if (donor != null)
                {
                    donor.addCreature(critter.getType(), false);
                    server.allTellAddCreature(new UndoSummonEvent(donor,
                        critter.getType()), true);
                    LOGGER.log(Level.INFO, "undosummon critter " + critter
                        + " back to marker " + donor + "");
                    // This summon doesn't count; the player can
                    // summon again later this turn.
                    player.setSummoned(false);
                    player.setDonor(null);
                }
                else
                {
                    LOGGER.log(Level.SEVERE,
                        "Null donor in Battle.cleanupOneDeadCritter()");
                }
            }
            else
            {
                // This reinforcment doesn't count.
                // Tell legion to do undo the reinforcement and trigger
                // sending of needed messages to clients:
                player.undoReinforcement(legion);
            }
        }
        else if (legion == getAttackingLegion())
        {
            getDefendingLegion().addToBattleTally(critter.getPointValue());
        }
        else
        // defender
        {
            getAttackingLegion().addToBattleTally(critter.getPointValue());

            // Creatures left off board do not trigger angel
            // summoning.
            if (summonState == AngelSummoningStates.NO_KILLS
                && !critter.getCurrentHex().isEntrance())
            {
                summonState = AngelSummoningStates.FIRST_BLOOD;
            }
        }

        // If an angel or archangel was returned to its donor instead of
        // the stack, then don't put it back on the stack.
        ((LegionServerSide)legion).prepareToRemoveCritter(critter,
            donor == null, true);

        if (critter.isTitan())
        {
            ((PlayerServerSide)legion.getPlayer()).eliminateTitan();
        }
    }

    private void checkForElimination()
    {
        LegionServerSide attacker = getAttackingLegion();
        LegionServerSide defender = getDefendingLegion();
        PlayerServerSide attackerPlayer = attacker.getPlayer();
        PlayerServerSide defenderPlayer = defender.getPlayer();

        boolean attackerTitanDead = attackerPlayer.isTitanEliminated();
        boolean defenderTitanDead = defenderPlayer.isTitanEliminated();

        // Check for mutual Titan elimination.
        if (attackerTitanDead && defenderTitanDead)
        {
            // Nobody gets any points.
            // Make defender die first, to simplify turn advancing.
            defender.getPlayer().die(null);
            attacker.getPlayer().die(null);
            getGame().checkForVictory();
            cleanup();
        }

        // Check for single Titan elimination.
        else if (attackerTitanDead)
        {
            if (defenderElim)
            {
                defender.remove();
            }
            else
            {
                pointsScored = defender.getBattleTally();
                // award points and handle acquiring
                defender.addBattleTallyToPoints();
            }
            attacker.getPlayer().die(defender.getPlayer());
            getGame().checkForVictory();
            cleanup();
        }
        else if (defenderTitanDead)
        {
            if (attackerElim)
            {
                attacker.remove();
            }
            else
            {
                pointsScored = attacker.getBattleTally();
                // award points and handle acquiring
                attacker.addBattleTallyToPoints();
            }
            defender.getPlayer().die(attacker.getPlayer());
            getGame().checkForVictory();
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
            // award points and handle acquiring
            defender.addBattleTallyToPoints();
            attacker.remove();
            cleanup();
        }
        else if (defenderElim)
        {
            pointsScored = attacker.getBattleTally();
            // award points and handle acquiring
            attacker.addBattleTallyToPoints();
            defender.remove();
            cleanup();
        }
    }

    private void commitStrikes()
    {
        LegionServerSide legion = getActiveLegion();
        if (legion != null)
        {
            for (Creature critter : legion.getCreatures())
            {
                critter.setStruck(false);
            }
        }
    }

    private boolean isForcedStrikeRemaining()
    {
        LegionServerSide legion = getActiveLegion();
        if (legion != null)
        {
            for (CreatureServerSide critter : legion.getCreatures())
            {
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
    Set<String> findStrikes(CreatureServerSide critter, boolean rangestrike)
    {
        Set<String> set = new HashSet<String>();

        // Each creature may strike only once per turn.
        if (critter.hasStruck())
        {
            return set;
        }

        Player player = critter.getPlayer();
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
                    CreatureServerSide target = getCritter(targetHex);
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
            && getBattlePhase() != BattlePhase.STRIKEBACK
            && critter.getLegion() == getActiveLegion())
        {
            for (Creature target : getInactiveLegion().getCreatures())
            {
                if (!target.isDead())
                {
                    BattleHex targetHex = target.getCurrentHex();
                    if (isRangestrikePossible(critter, target, currentHex,
                        targetHex))
                    {
                        set.add(targetHex.getLabel());
                    }
                }
            }
        }
        return set;
    }

    /** Return the set of hexes with valid carry targets. */
    Set<BattleHex> getCarryTargets()
    {
        return Collections.unmodifiableSet(carryTargets);
    }

    Set<String> getCarryTargetDescriptions()
    {
        Set<String> set = new HashSet<String>();
        for (BattleHex hex : getCarryTargets())
        {
            CreatureServerSide critter = getCritter(hex);
            set.add(critter.getDescription());
        }
        return set;
    }

    void clearCarryTargets()
    {
        carryTargets.clear();
    }

    void setCarryTargets(Set<BattleHex> carryTargets)
    {
        this.carryTargets.clear();
        this.carryTargets.addAll(carryTargets);
    }

    void addCarryTarget(BattleHex hex)
    {
        carryTargets.add(hex);
    }

    void applyCarries(CreatureServerSide target)
    {
        if (!carryTargets.contains(target.getCurrentHex()))
        {
            LOGGER.log(Level.WARNING, "Tried illegal carry to "
                + target.getDescription());
            return;
        }
        int dealt = carryDamage;
        carryDamage = target.wound(carryDamage);
        dealt -= carryDamage;
        carryTargets.remove(target.getCurrentHex());

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
            getCarryTargetDescriptions());
    }

    /** Return the number of intervening bramble hexes.  If LOS is along a
     *  hexspine, go left if argument left is true, right otherwise.  If
     *  LOS is blocked, return a large number.
     * @deprecated another function with explicit reference to Bramble
     * that should be fixed.
     */
    @Deprecated
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

    /* Return the number of intervening bramble hexes.  If LOS is along a
     * hexspine and there are two unblocked choices, pick the lower one.
     * @deprecated another function with explicit reference to Bramble
     * that should be fixed.
     */
    @Deprecated
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

        if (CompareDoubles.almostEqual(yDist, 0.0)
            || CompareDoubles.almostEqual(Math.abs(yDist), 1.5 * Math
                .abs(xDist)))
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
    boolean doMove(int tag, BattleHex hex)
    {
        CreatureServerSide critter = getActiveLegion().getCritterByTag(tag);
        if (critter == null)
        {
            return false; // TODO shouldn't this be an error?
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
        else if (showMoves(critter, false).contains(hex))
        {
            LOGGER.log(Level.INFO, critter.getName() + " moves from "
                + critter.getCurrentHex().getLabel() + " to " + hex.getLabel());
            critter.moveToHex(hex, true);
            return true;
        }
        else
        {
            LegionServerSide legion = getActiveLegion();
            String markerId = legion.getMarkerId();
            LOGGER.log(Level.WARNING, critter.getName() + " in "
                + critter.getCurrentHex().getLabel()
                + " tried to illegally move to " + hex.getLabel() + " in "
                + masterHex.getTerrain() + " (" + getAttackingLegion().getMarkerId()
                + " attacking " + getDefendingLegion().getMarkerId() + ", active: "
                + markerId + ")");
            return false;
        }
    }

    private void cleanup()
    {
        battleOver = true;
        getGame().finishBattle(masterHex, attackerEntered, pointsScored,
            turnNumber);
    }

    /** Return a list of all critters in the battle. */
    private List<CreatureServerSide> getAllCritters()
    {
        List<CreatureServerSide> critters = new ArrayList<CreatureServerSide>();
        LegionServerSide defender = getDefendingLegion();
        if (defender != null)
        {
            critters.addAll(defender.getCreatures());
        }
        LegionServerSide attacker = getAttackingLegion();
        if (attacker != null)
        {
            critters.addAll(attacker.getCreatures());
        }
        return critters;
    }

    // TODO get rid of String-based access
    private boolean isOccupied(String hexLabel)
    {
        for (Creature critter : getAllCritters())
        {
            if (hexLabel.equals(critter.getCurrentHex().getLabel()))
            {
                return true;
            }
        }
        return false;
    }
    @Override
    protected boolean isOccupied(BattleHex hex)
    {
        return isOccupied(hex.getLabel());
    }

    CreatureServerSide getCritter(BattleHex hex)
    {
        assert hex != null;
        for(CreatureServerSide creature: getAllCritters()) {
            if(hex.equals(creature.getCurrentHex())) {
                return creature;
            }
        }
        // TODO check if this is feasible, otherwise assert false here
        return null;
    }
}
