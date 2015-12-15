package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.actions.SummonUndo;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.MasterHex;


/**
 * Class Battle holds data about a Titan battle.
 *
 * It has utility functions related to incrementing the phase, managing
 * moves, and managing strikes.
 *
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
    private BattlePhase phase;
    private AngelSummoningStates summonState = AngelSummoningStates.NO_KILLS;
    private int carryDamage;
    private boolean attackerElim;
    private boolean defenderElim;
    private boolean battleOver;
    private boolean attackerEntered;
    private boolean conceded;
    private boolean preStrikeEffectsApplied = false;

    /**
     * Set of hexes for valid carry targets
     */
    private final Set<BattleHex> carryTargets = new HashSet<BattleHex>();
    private final PhaseAdvancer phaseAdvancer = new BattlePhaseAdvancer();
    private int pointsScored = 0;

    private final BattleMovementServerSide battleMovement;

    BattleServerSide(GameServerSide game, Legion attacker, Legion defender,
        LegionTags activeLegionTag, MasterHex masterHex, BattlePhase phase)
    {
        super(game, attacker, defender, masterHex);

        this.server = game.getServer();
        this.activeLegionTag = activeLegionTag;
        this.phase = phase;

        this.battleMovement = new BattleMovementServerSide(game.getOptions(),
            getGame());

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
        BattleHex entrance = getLocation().getTerrain().getEntrance(
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
        BattleHex entrance = getLocation().getTerrain().getEntrance(
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
        server.allInitBattle(getLocation());
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

    // TODO perhaps temporary, only for BattleMovementServerSide right now
    public boolean isDefenderActive()
    {
        return activeLegionTag == LegionTags.DEFENDER;
    }

    @Override
    public Legion getBattleActiveLegion()
    {
        return getLegion(activeLegionTag);
    }

    Player getBattleActivePlayer()
    {
        return getBattleActiveLegion().getPlayer();
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

    BattlePhase getBattlePhase()
    {
        return phase;
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
                if (activeLegionTag == LegionTags.ATTACKER && !conceded)
                {
                    attackerEntered = true;
                }
                phase = BattlePhase.FIGHT;
                LOGGER.log(Level.INFO, "Battle phase advances to " + phase);
                if (conceded)
                {
                    LOGGER
                        .log(Level.INFO, "Conceded - setting again to true.");
                    again = true;
                }
                else
                {
                    again = setupFight();
                }
            }

            else if (phase == BattlePhase.FIGHT)
            {
                // We switch the active legion between the fight and strikeback
                // phases, not at the end of the player turn.
                activeLegionTag = (activeLegionTag == LegionTags.ATTACKER) ? LegionTags.DEFENDER
                    : LegionTags.ATTACKER;
                preStrikeEffectsApplied = false;
                phase = BattlePhase.STRIKEBACK;
                LOGGER.log(Level.INFO, "Battle phase advances to " + phase);
                if (conceded)
                {
                    LOGGER
                        .log(Level.INFO, "Conceded - setting again to true.");
                    again = true;
                }
                else
                {
                    again = setupFight();
                }
            }

            else if (phase == BattlePhase.STRIKEBACK)
            {
                removeDeadCreatures();
                checkForElimination();
                advanceTurn();
            }

            // Comment related to the 2 if conceded again=true else again=...
            // blocks above:
            // If conceded, ripple through all remaining phases automatically.
            // See also the comment in "void concede(Player player)", which is
            // related to the bug tracker items
            // 3133960 and 3160873 ("Conceding battle leads to hung game").
            // Doing "if (again || conceded)" would leave to strange side
            // effects (I tried that ;-)
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
                LOGGER.log(Level.INFO, getBattleActivePlayer()
                    + "'s battle turn, number " + battleTurnNumber);
                again = setupSummon();
            }
            else
            {
                battleTurnNumber++;
                if (battleTurnNumber > 7)
                {
                    timeLoss();
                }
                else
                {
                    phase = BattlePhase.RECRUIT;
                    again = setupRecruit();
                    if (getBattleActivePlayer() != null)
                    {
                        LOGGER.log(Level.INFO, getBattleActivePlayer()
                            + "'s battle turn, number " + battleTurnNumber);
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
        applyPreStrikeEffects();
        server.allSetupBattleFight();
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
        if (battleTurnNumber == 4 && defender.canRecruit())
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

    void undoMove(BattleHex hex)
    {
        CreatureServerSide critter = getCreatureSS(hex);
        if (critter != null)
        {
            BattleHex formerHexLabel = critter.getCurrentHex();
            critter.undoMove();
            getGame().getServer().allTellBattleMove(critter.getTag(),
                formerHexLabel, critter.getCurrentHex(), true);
        }
        else
        {
            LOGGER.log(Level.SEVERE,
                "Undo move error: no critter in " + hex.getLabel());
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

        // To fix 3133960 and 3160873 ("Conceding battle leads to hung game"):
        // Rather be not totally compliant to rules, instead of having
        // games hung (if player concedes hopeless case, or bails out
        // (e.g. connection lost), etc.)
        // TODO Created Feature Request 3182336 to get this back to track, ...
        // ... eventually.
        //
        // if (legion.getPlayer().equals(getBattleActivePlayer()))
        // {
        //    advancePhase();
        // }
        // To prevent the hang, do it also for not-phasing player:
        advancePhase();
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

    private void applyPreStrikeEffects()
    {
        // Certain effects are applied once per player turn,
        // during the strike phase, before the actual strike takes place.
        // These currently include damage from Drifts, damage from Poison,
        // healing from Springs and slowing from TarPits

        if (phase == BattlePhase.FIGHT && !preStrikeEffectsApplied)
        {
            preStrikeEffectsApplied = true;
            for (BattleCritter c : getAllCritters())
            {
                CreatureServerSide critter = (CreatureServerSide)c;
                int dam = critter.getCurrentHex().damageToCreature(
                    critter.getType());
                dam += critter.getPoisonDamage();
                if (dam != 0)
                {
                    critter.adjustHits(dam);
                    LOGGER.log(Level.INFO, critter.getDescription()
                        + " preStrikeEffects: " + dam + " adjust to hits");
                    server.allTellHexDamageResults(critter, dam);
                }
                int slowValue = critter.getCurrentHex().slowsCreature(
                    critter.getType());
                if (slowValue != 0)
                {
                    critter.addSlowed(slowValue);
                    server.allTellHexSlowResults(critter, slowValue);
                }
            }
        }
    }

    boolean arePreStrikeEffectsApplied()
    {
        return preStrikeEffectsApplied;
    }

    void setPreStrikeEffectsApplied(boolean preStrikeEffectsApplied)
    {
        this.preStrikeEffectsApplied = preStrikeEffectsApplied;
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
        server.allUpdatePlayerInfo("removeDeadCreatures");
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
        LegionServerSide legion = (LegionServerSide)critter.getLegion();
        LegionServerSide donor = null;
        boolean reinforced = false;

        PlayerServerSide player = legion.getPlayer();

        // After turn 1, off-board creatures are returned to the
        // stacks or the legion they were summoned from, with
        // no points awarded.
        if (critter.getCurrentHex().isEntrance() && battleTurnNumber > 1)
        {
            if (legion == getAttackingLegion())
            {
                // Summoned angel.
                donor = player.getDonor();
                if (donor != null)
                {
                    donor.addCreature(critter.getType(), false);
                    server.allTellAddCreature(
                        new SummonUndo(donor, critter.getType()), true,
                        Constants.reasonUndoSummon);
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
                // This reinforcement doesn't count.
                // Tell legion to do undo the reinforcement and trigger
                // sending of needed messages to clients:
                reinforced = true;
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

        if (donor != null)
        {
            // If an angel or archangel was returned to its donor instead of
            // the stack, then don't put it back on the stack.
            legion.prepareToRemoveCritter(critter, false, true);
        }
        else if (reinforced)
        {
            // undoReinforce does the remove already ( = back to caretaker)
            // and also creates the removeCreatureEvent.
            // legion.prepareToRemoveCritter(critter, false, true);
        }
        else
        {
            legion.prepareToRemoveCritter(critter, true, true);
        }

        if (critter.isTitan())
        {
            (legion.getPlayer()).eliminateTitan();
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

    public boolean isForcedStrikeRemaining()
    {
        LegionServerSide legion = getActiveLegion();
        if (legion != null)
        {
            for (CreatureServerSide critter : legion.getCreatures())
            {
                if (!critter.hasStruck() && isInContact(critter, false))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /** Checks now all at same place in Server */
    void doneWithStrikes()
    {
        commitStrikes();
        advancePhase();
    }

    /**
      *  Return a set of hexes containing targets that the critter may strike
      *
      *  @param battleUnit the striking creature
      *  @param rangestrike Whether to include rangestrike targets
      *  @return a set of hexes containing targets
      */
    Set<BattleHex> findTargetHexes(CreatureServerSide critter,
        boolean rangestrike)
    {
        Set<BattleHex> set = new HashSet<BattleHex>();

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
                    CreatureServerSide target = getCreatureSS(targetHex);
                    if (target.getPlayer() != player)
                    {
                        adjacentEnemy = true;
                        if (!target.isDead())
                        {
                            set.add(targetHex);
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
                        set.add(targetHex);
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
            CreatureServerSide critter = getCreatureSS(hex);
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
            LOGGER.log(Level.WARNING,
                "Tried illegal carry to " + target.getDescription());
            return;
        }
        int dealt = carryDamage;
        carryDamage = target.adjustHits(carryDamage);
        dealt -= carryDamage;
        carryTargets.remove(target.getCurrentHex());

        LOGGER.log(Level.INFO,
            dealt + (dealt == 1 ? " hit carries to " : " hits carry to ")
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

    /** If legal, move critter to hex and return true. Else return false. */
    String doMove(int tag, BattleHex hex)
    {
        String reasonFail = null;

        CreatureServerSide critter = getActiveLegion().getCritterByTag(tag);
        if (critter == null)
        {
            reasonFail = "No critter with tag " + tag + " found from legion "
                + getActiveLegion().getMarkerId() + " - can't move it to hex "
                + hex.getLabel();
            LOGGER.severe(reasonFail);
            return reasonFail;
        }

        // Allow null moves.
        if (!critter.hasMoved() && hex.equals(critter.getCurrentHex()))
        {
            // Warning, for now, because actually this should never happen, at
            // least not for human players...
            LOGGER.warning(critter.getDescription() + " does not move");
            // Call moveToHex() anyway to sync client.
            moveCritterToHexAndInformClients(critter, hex);
            return null;
        }
        else if (battleMovement.showMoves(critter, false).contains(hex))
        {
            LOGGER
                .log(
                    Level.INFO,
                    critter.getName() + " moves from "
                        + critter.getCurrentHex().getLabel() + " to "
                        + hex.getLabel());
            moveCritterToHexAndInformClients(critter, hex);
            return null;
        }
        else
        {
            LegionServerSide legion = getActiveLegion();
            String markerId = legion.getMarkerId();
            LOGGER.log(Level.WARNING, critter.getName() + " in "
                + critter.getCurrentHex().getLabel()
                + " tried to illegally move to " + hex.getLabel() + " in "
                + getLocation().getTerrain().getDisplayName() + " ("
                + getAttackingLegion().getMarkerId() + " attacking "
                + getDefendingLegion().getMarkerId() + ", active: " + markerId
                + ")");
            reasonFail = critter.getName() + " in "
                + critter.getCurrentHex().getLabel() + " can't move to "
                + hex.getLabel() + " (Have you clicked this move "
                + "before previous move was completed by server?)";
            return reasonFail;
        }
    }

    private void moveCritterToHexAndInformClients(CreatureServerSide critter,
        BattleHex hex)
    {
        critter.moveToHex(hex);
        getGame().getServer().allTellBattleMove(critter.getTag(),
            critter.getStartingHex(), critter.getCurrentHex(), false);
    }

    private void cleanup()
    {
        battleOver = true;
        getGame().finishBattle(getLocation(), attackerEntered, pointsScored,
            battleTurnNumber);
    }

    @Override
    public List<BattleCritter> getAllCritters()
    {
        List<BattleCritter> critters = new ArrayList<BattleCritter>();
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

    // TODO use getCritter() instead
    CreatureServerSide getCreatureSS(BattleHex hex)
    {
        return (CreatureServerSide)getCritter(hex);
    }

    /** Return true if there are any enemies adjacent to this critter.
     *
     * @param critter The critter to check whether it is in contact with any enemy critter
     * @param countDead Dead critters count as being in contact only if countDead is true.
     */
    @Override
    public boolean isInContact(BattleCritter critter, boolean countDead)
    {
        BattleHex hex = critter.getCurrentHex();

        // Offboard creatures are not in contact.
        if (hex.isEntrance())
        {
            return false;
        }

        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not in contact.
            if (!hex.isCliff(i))
            {
                BattleHex neighbor = hex.getNeighbor(i);
                if (neighbor != null)
                {
                    BattleCritter other = getCreatureSS(neighbor);
                    if (other != null
                        && other.isDefender() != critter.isDefender()
                        && (countDead || !other.isDead()))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /** Return the number of enemy creatures in contact with this critter.
     *  Dead critters count as being in contact only if countDead is true. */
    public int numInContact(BattleCritter striker, boolean countDead)
    {
        BattleHex hex = striker.getCurrentHex();

        // Offboard creatures are not in contact.
        if (hex.isEntrance())
        {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not in contact.
            if (!hex.isCliff(i))
            {
                BattleHex neighbor = hex.getNeighbor(i);
                if (neighbor != null)
                {
                    BattleCritter other = getCreatureSS(neighbor);
                    if (other != null
                        && other.isDefender() != striker.isDefender()
                        && (countDead || !other.isDead()))
                    {
                        count++;
                    }
                }
            }
        }

        return count;
    }

}
