package net.sf.colossus.server;


import java.util.*;
import javax.swing.*;
import java.awt.event.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.client.MasterBoard;
import net.sf.colossus.client.MasterHex;
import net.sf.colossus.client.BattleHex;
import net.sf.colossus.client.HexMap;
import net.sf.colossus.client.BattleMap;


/**
 * Class Battle holds data about a Titan battle. It has utility functions
 * related to incrementing the phase, managing moves, and managing
 * strikes
 *
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

public final class Battle
{
    private Game game;
    private Server server;
    private String attackerId;
    private String defenderId;
    private String [] legions = new String[2];
    private int activeLegionNum;
    private String masterHexLabel;
    private char terrain;
    private int turnNumber;
    private int phase;
    private int summonState = Constants.NO_KILLS;
    private int carryDamage;
    private boolean attackerElim;
    private boolean defenderElim;
    private boolean battleOver;
    private boolean attackerEntered;
    private boolean conceded;
    private boolean driftDamageApplied = false;
    /** Set of hexLabels for valid carry targets */
    private Set carryTargets = new HashSet();
    private PhaseAdvancer phaseAdvancer = new BattlePhaseAdvancer();


    Battle(Game game, String attackerId, String defenderId,
        int activeLegionNum, String masterHexLabel, int turnNumber, int phase)
    {
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
            Log.warn("Fixing bogus entry side: " + side);
            // If invalid, default to bottom, which is always valid.
            attacker.setEntrySide(3);
        }
        Legion defender = getDefender();
        defender.setEntrySide((side + 3) % 6);
        // Make sure defender can recruit, even if savegame is off.
        defender.setRecruitName(null);

        Log.event(attacker.getLongMarkerName() + " (" +
            attacker.getPlayerName() + ") attacks " +
            defender.getLongMarkerName() + " (" +
            defender.getPlayerName() + ")" + " in " +
            MasterBoard.getHexByLabel(masterHexLabel).getDescription());

        placeLegion(attacker);
        placeLegion(defender);
    }


    private void placeLegion(Legion legion)
    {
        BattleHex entrance = BattleMap.getEntrance(terrain, masterHexLabel,
            legion.getEntrySide());
        String entranceLabel = entrance.getLabel();
        Iterator it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();

            String currentHexLabel = critter.getCurrentHexLabel();
            if (currentHexLabel == null)
            {
                currentHexLabel = entranceLabel;
            }
            String startingHexLabel = critter.getStartingHexLabel();
            if (startingHexLabel == null)
            {
                startingHexLabel = entranceLabel;
            }

            critter.addBattleInfo(currentHexLabel, startingHexLabel,
                this);
        }
    }

    private void placeCritter(Critter critter)
    {
        BattleHex entrance = BattleMap.getEntrance(terrain, masterHexLabel,
            critter.getLegion().getEntrySide());
        String entranceLabel = entrance.getLabel();
        critter.addBattleInfo(entranceLabel, entranceLabel, this);
        server.allPlaceNewChit(critter);
    }


    private synchronized void initBattleChits(Legion legion)
    {
        Iterator it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
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
        switch (getPhase())
        {
            case Constants.SUMMON:
                advance = setupSummon();
                break;
            case Constants.RECRUIT:
                advance = setupRecruit();
                break;
            case Constants.MOVE:
                advance = setupMove();
                break;
            case Constants.FIGHT:
            case Constants.STRIKEBACK:
                advance = setupFight();
                break;
            default:
                Log.error("Bogus phase");
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


    Player getActivePlayer()
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
        return game.getLegionByMarkerId(attackerId);
    }

    String getDefenderId()
    {
        return defenderId;
    }

    Legion getDefender()
    {
        return game.getLegionByMarkerId(defenderId);
    }


    int getActiveLegionNum()
    {
        return activeLegionNum;
    }


    Legion getActiveLegion()
    {
        return getLegion(activeLegionNum);
    }


    Legion getInactiveLegion()
    {
        return getLegion((activeLegionNum + 1) & 1);
    }


    Legion getLegion(int legionNum)
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


    Legion getLegionByPlayerName(String playerName)
    {
        Legion attacker = getAttacker();
        if (attacker != null && attacker.getPlayerName().equals(
            playerName))
        {
            return attacker;
        }
        Legion defender = getDefender();
        if (defender != null && defender.getPlayerName().equals(
            playerName))
        {
            return defender;
        }
        return null;
    }


    String getMasterHexLabel()
    {
        return masterHexLabel;
    }

    MasterHex getMasterHex()
    {
        return MasterBoard.getHexByLabel(masterHexLabel);
    }


    char getTerrain()
    {
        return terrain;
    }


    int getPhase()
    {
        return phase;
    }

    int getTurnNumber()
    {
        return turnNumber;
    }


    boolean isAttackerElim()
    {
        return attackerElim;
    }


    boolean isDefenderElim()
    {
        return defenderElim;
    }


    boolean isOver()
    {
        return battleOver;
    }


    void advancePhase()
    {
        phaseAdvancer.advancePhase();
    }

    class BattlePhaseAdvancer extends PhaseAdvancer
    {
        private boolean again = false;
    

        /** Advance to the next battle phase. */
        void advancePhase()
        {
            if (!isOver())
            {
                advancePhaseInternal();
            }
        }
    
        void advancePhaseInternal()
        {
            if (phase == Constants.SUMMON)
            {
                phase = Constants.MOVE;
                Log.event("Battle phase advances to " + 
                    Constants.getBattlePhaseName(phase));
                again = setupMove();
            }
    
            else if (phase == Constants.RECRUIT)
            {
                phase = Constants.MOVE;
                Log.event("Battle phase advances to " + 
                    Constants.getBattlePhaseName(phase));
                again = setupMove();
            }
    
            else if (phase == Constants.MOVE)
            {
                // IF the attacker makes it to the end of his first movement
                // phase without conceding, even if he left all legions
                // off-board, the defender can recruit.
                if (activeLegionNum == Constants.ATTACKER && !conceded)
                {
                    attackerEntered = true;
                }
                phase = Constants.FIGHT;
                Log.event("Battle phase advances to " + 
                    Constants.getBattlePhaseName(phase));
                again = setupFight();
            }
    
            else if (phase == Constants.FIGHT)
            {
                // We switch the active legion between the fight and strikeback
                // phases, not at the end of the player turn.
                activeLegionNum = (activeLegionNum + 1) & 1;
                driftDamageApplied = false;
                phase = Constants.STRIKEBACK;
                Log.event("Battle phase advances to " + 
                    Constants.getBattlePhaseName(phase));
                again = setupFight();
            }
    
            else if (phase == Constants.STRIKEBACK)
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

        void advanceTurn()
        {
            if (isOver())
            {
                return;
            }

            // Active legion is the one that was striking back.
            if (activeLegionNum == Constants.ATTACKER)
            {
                phase = Constants.SUMMON;
                Log.event(getActivePlayerName() + "'s battle turn, number " +
                    turnNumber);
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
                    phase = Constants.RECRUIT;
                    again = setupRecruit();
                    if (getActivePlayer() != null)
                    {
                        Log.event(getActivePlayerName() +
                            "'s battle turn, number " + turnNumber);
                    }
                }
            }
        }

        private void timeLoss()
        {
            Log.event("Time loss");
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
        if (phase == Constants.SUMMON)
        {
            advancePhase();
        }
    }


    private boolean recruitReinforcement()
    {
        Legion defender = getDefender();
        if (turnNumber == 4 && defender.canRecruit())
        {
Log.debug("Calling Game.reinforce() from Battle.recruitReinforcement()");
            game.reinforce(defender);
            return false;
        }
        return true;
    }

    /** Needs to be called when reinforcement is done. */
    void doneReinforcing()
    {
Log.debug("Called Battle.doneReinforcing()");
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
    private Set findMoves(BattleHex hex, Critter critter, boolean flies,
        int movesLeft, int cameFrom, boolean ignoreMobileAllies)
    {
        Set set = new HashSet();
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
                    if (bogey == null || (ignoreMobileAllies &&
                        bogey.getMarkerId().equals(critter.getMarkerId())
                        && !bogey.isInContact(false)))
                    {
                        entryCost = neighbor.getEntryCost(critter, reverseDir);
                    }
                    else
                    {
                        entryCost = BattleHex.IMPASSIBLE_COST;
                    }

                    if (entryCost <= movesLeft)
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
                                ignoreMobileAllies));
                        }
                    }

                    // Fliers can fly over any hex for 1 movement point,
                    // but some Hex cannot be flown over by some creatures.
                    if (flies &&
                        movesLeft > 1 &&
                        neighbor.canBeFlownOverBy(critter))
                    {
                        set.addAll(findMoves(neighbor, critter, flies,
                            movesLeft - 1, reverseDir, ignoreMobileAllies));
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
    private Set findUnoccupiedStartlistHexes(boolean ignoreMobileAllies, char t)
    {
        Set set = new HashSet();
        Iterator it = HexMap.getTowerStartList(t).iterator();
        while (it.hasNext())
        {
            BattleHex hex = HexMap.getHexByLabel(t, (String)it.next());
            if (ignoreMobileAllies || !isOccupied(hex))
            {
                set.add(hex.getLabel());
            }
        }
        return set;
    }


    Set showMoves(int tag)
    {
        Critter critter = getActiveLegion().getCritterByTag(tag);
        return showMoves(critter, false);
    }

    /** Find all legal moves for this critter. The returned list
     *  contains hex IDs, not hexes. */
    Set showMoves(Critter critter, boolean ignoreMobileAllies)
    {
        Set set = new HashSet();
        if (!critter.hasMoved() && !critter.isInContact(false))
        {
            if (HexMap.terrainHasStartlist(terrain) && (turnNumber == 1) &&
                activeLegionNum == Constants.DEFENDER)
            {
                set =
                    findUnoccupiedStartlistHexes(ignoreMobileAllies, terrain);
            }
            else
            {
                set = findMoves(critter.getCurrentHex(), critter,
                    critter.isFlier(), critter.getSkill(), -1,
                    ignoreMobileAllies);
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
            Log.error("Undo move error: no critter in " + hexLabel);
        }
    }

    void undoAllMoves()
    {
        Iterator it = getActiveLegion().getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.hasMoved())
            {
                critter.undoMove();
            }
        }
    }



    /** Mark all of the conceding player's critters as dead. */
    void concede(String playerName)
    {
        Legion legion = getLegionByPlayerName(playerName);
        String markerId = legion.getMarkerId();
        Log.event(markerId + " concedes the battle");
        conceded = true;

        Iterator it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            critter.setDead(true);
        }

        if (legion.getPlayerName().equals(getActivePlayerName()))
        {
            advancePhase();
        }
    }


    /** Return a set of hex labels for hex labels with critters eligible
     *  to move. */
    Set findMobileCritters()
    {
        Set set = new HashSet();
        Legion legion = getActiveLegion();

        Iterator it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (!critter.hasMoved() && !critter.isInContact(false))
            {
                BattleHex hex = critter.getCurrentHex();
                set.add(hex.getLabel());
            }
        }

        return set;
    }


    /** If any creatures were left off-board, kill them.  If they were newly
     *  summoned or recruited, unsummon or unrecruit them instead. */
    private void removeOffboardCreatures()
    {
        Legion legion = getActiveLegion();
        Iterator it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getCurrentHex().isEntrance())
            {
                critter.setDead(true);
            }
        }
        removeDeadCreatures();
    }


    private void commitMoves()
    {
        Iterator it = getActiveLegion().getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            critter.commitMove();
        }
    }

    void doneWithMoves()
    {
        removeOffboardCreatures();
        commitMoves();
        advancePhase();
    }


    void applyDriftDamage()
    {
        // Drift damage is applied only once per player turn,
        //    during the strike phase.
        if (phase == Constants.FIGHT && !driftDamageApplied)
        {
            Iterator it = getAllCritters().iterator();
            driftDamageApplied = true;
            while (it.hasNext())
            {
                Critter critter = (Critter)it.next();
                int dam = critter.getCurrentHex().damageToCreature(critter);
                if (dam > 0)
                {
                    critter.wound(dam);
                    Log.event(critter.getDescription() + " takes Hex damage");
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

        if (attacker.getPlayer() == null || 
            attacker.getPlayer().isTitanEliminated())
        {
            attackerElim = true;
        }
        if (defender.getPlayer() == null || 
            defender.getPlayer().isTitanEliminated())
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
        List critters = legion.getCritters();
        if (critters != null)
        {
            Iterator it = critters.iterator();
            while (it.hasNext())
            {
                Critter critter = (Critter)it.next();
                if (critter.isDead())
                {
                    cleanupOneDeadCritter(critter);
                    it.remove();
                }
                else  // critter is alive
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
        Legion legion = critter.getLegion();
        Legion donor = null;

        // After turn 1, offboard creatures are returned to the
        // stacks or the legion they were summoned from, with
        // no points awarded.
        if (critter.getCurrentHex().isEntrance() && getTurnNumber() > 1)
        {
            // XXX If a critter is both summonable and a recruitable
            // reinforcement, then this logic fails.
            if (critter.isSummonable())
            {
                Player player = legion.getPlayer();
                donor = player.getDonor();
                if (donor != null)
                {
                    Log.error("Null donor in Battle.cleanupOneDeadCritter()");
                    donor.addCreature(critter, false);
                    server.allTellAddCreature(donor.getMarkerId(), 
                        critter.getName());
                    // This summon doesn't count; the player can
                    // summon again later this turn.
                    player.setSummoned(false);
                }
            }
            else
            {
                // Reinforcement.
                game.getCaretaker().putOneBack(critter);
                // This recruit doesn't count.
                legion.setRecruitName(null);
            }
        }
        else if (legion == getAttacker())
        {
            getDefender().addToBattleTally(critter.getPointValue());
        }
        else  // defender
        {
            getAttacker().addToBattleTally(critter.getPointValue());

            // Creatures left offboard do not trigger angel
            // summoning.
            if (summonState == Constants.NO_KILLS &&
                !critter.getCurrentHex().isEntrance())
            {
                summonState = Constants.FIRST_BLOOD;
            }
        }

        // If an angel or archangel was returned to its donor instead of 
        // the stack, then don't put it back on the stack.
        legion.prepareToRemoveCritter(critter, 
            (donor == null || !critter.isSummonable()));

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
            defender.addBattleTallyToPoints();
            attacker.remove();
            cleanup();
        }
        else if (defenderElim)
        {
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
            Iterator it = legion.getCritters().iterator();
            while (it.hasNext())
            {
                Critter critter = (Critter)it.next();
                critter.setStruck(false);
            }
        }
    }

    /** Return the set of hex labels for hexes with critters that have
     *  valid strike targets. */
    Set findCrittersWithTargets()
    {
        Set set = new HashSet();
        Iterator it = getActiveLegion().getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (countStrikes(critter, true) > 0)
            {
                set.add(critter.getCurrentHexLabel());
            }
        }

        return set;
    }


    boolean isForcedStrikeRemaining()
    {
        Legion legion = getActiveLegion();
        if (legion != null)
        {
            Iterator it = legion.getCritters().iterator();
            while (it.hasNext())
            {
                Critter critter = (Critter)it.next();
                if (!critter.hasStruck() && critter.isInContact(false))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /** Perform strikes for any creature that is forced to strike
     *  and has only one legal target. Forced strikes will never
     *  generate carries, since there's only one target. If
     *  rangestrike is true, also perform rangestrikes for
     *  creatures with only one target, even though they're not
     *  technically forced. */
    synchronized void makeForcedStrikes(boolean rangestrike)
    {
        if (getPhase() != Constants.FIGHT && 
            getPhase() != Constants.STRIKEBACK)
        {
            Log.error("Called Battle.makeForcedStrikes() in wrong phase");
            return;
        }
        Legion legion = getActiveLegion();
        boolean repeat;
        do
        {
            repeat = false;
            Iterator it = legion.getCritters().iterator();
            while (it.hasNext())
            {
                Critter critter = (Critter)it.next();
                if (!critter.hasStruck())
                {
                    Set set = findStrikes(critter, rangestrike);
                    if (set.size() == 1)
                    {
                        String hexLabel = (String)(set.iterator().next());
                        Critter target = getCritter(hexLabel);
                        critter.strike(target);

                        // If that strike killed the target, it's possible
                        // that some other creature that had two targets
                        // now has only one.
                        if (target.isDead())
                        {
                            repeat = true;
                        }
                    }
                }
            }
        }
        while (repeat);
    }

    /** Return true if okay, or false if forced strikes remain. */
    boolean doneWithStrikes()
    {
        // Advance only if there are no unresolved strikes.
        if (isForcedStrikeRemaining())
        {
            Log.error(server.getPlayerName() + 
                " called battle.doneWithStrikes() illegally");
            return false;
        }
        else
        {
            commitStrikes();
            advancePhase();
            return true;
        }
    }


    Set findStrikes(int tag)
    {
        Critter critter = getActiveLegion().getCritterByTag(tag);
        return findStrikes(critter, true);
    }

    /** Return a set of hex labels for hexes containing targets that the
     *  critter may strike.  Only include rangestrikes if rangestrike
     *  is true. */
    Set findStrikes(Critter critter, boolean rangestrike)
    {
        Set set = new HashSet();

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
        if (rangestrike && !adjacentEnemy && critter.isRangestriker() &&
            getPhase() != Constants.STRIKEBACK &&
            critter.getLegion() == getActiveLegion())
        {
            Iterator it = getInactiveLegion().getCritters().iterator();
            while (it.hasNext())
            {
                Critter target = (Critter)it.next();
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


    int countStrikes(Critter critter, boolean rangestrike)
    {
        return findStrikes(critter, rangestrike).size();
    }


    /** Return the set of hex labels for hexes with valid carry targets. */
    Set getCarryTargets()
    {
        return Collections.unmodifiableSet(carryTargets);
    }

    Set getCarryTargetDescriptions()
    {
        Set set = new HashSet();
        Iterator it = getCarryTargets().iterator();
        while (it.hasNext())
        {
            String hexLabel = (String)it.next();
            Critter critter = getCritter(hexLabel);    
            set.add(critter.getDescription());
        }
        return set;
    }

    void clearCarryTargets()
    {
        carryTargets.clear();
    }

    void setCarryTargets(Set carryTargets)
    {
        this.carryTargets.clear();
        this.carryTargets.addAll(carryTargets);
    }

    void addCarryTarget(String hexLabel)
    {
        carryTargets.add(hexLabel);
    }

    void removeCarryTarget(String hexLabel)
    {
        carryTargets.remove(hexLabel);
    }

    void applyCarries(Critter target)
    {
        if (!carryTargets.contains(target.getCurrentHexLabel()))
        {
            Log.warn("Tried illegal carry to " + target.getDescription());
            return;
        }
        int dealt = carryDamage;
        carryDamage = target.wound(carryDamage);
        dealt -= carryDamage;
        carryTargets.remove(target.getCurrentHexLabel());

        Log.event(dealt + (dealt == 1 ? " hit carries to " :
            " hits carry to ") + target.getDescription());

        if (carryDamage <= 0 || getCarryTargets().isEmpty())
        {
            leaveCarryMode();
        }
        else
        {
            Log.event(carryDamage + (carryDamage == 1 ?
                " carry available" : " carries available"));
        }
        server.allTellCarryResults(target, dealt, carryDamage, 
            getCarryTargets());
    }


    /** Return the range in hexes from hex1 to hex2.  Titan ranges are
     *  inclusive at both ends. */
    public static int getRange(BattleHex hex1, BattleHex hex2,
        boolean allowEntrance)
    {
        if (hex1 == null || hex2 == null)
        {
            Log.warn("passed null hex to getRange()");
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
                else  // hex2.isEntrance()
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
            return (int) Math.ceil(xDist + 1);
        }
        else if (xDist >= yDist)
        {
            return (int) Math.floor(xDist + 2);
        }
        else if (yDist >= 2 * xDist)
        {
            return (int) Math.ceil(yDist + 1);
        }
        else
        {
            return (int) Math.floor(yDist + 2);
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


    /** Return the titan range (inclusive at both ends) from the critter to the
     *  closest enemy critter.  Return OUT_OF_RANGE if there are none. */
    int minRangeToEnemy(Critter critter)
    {
        BattleHex hex = critter.getCurrentHex();
        int min = Constants.OUT_OF_RANGE;

        Legion enemy = getInactiveLegion();
        Iterator it = enemy.getCritters().iterator();
        while (it.hasNext())
        {
            Critter target = (Critter)it.next();
            BattleHex targetHex = target.getCurrentHex();
            int range = getRange(hex, targetHex, false);
            // Exit early if adjacent.
            if (range == 2)
            {
                return range;
            }
            else if (range < min)
            {
                 min = range;
            }
        }
        return min;
    }

    /** Caller must ensure that yDist != 0 */
    private static boolean toLeft(double xDist, double yDist)
    {
        double ratio = xDist / yDist;
        if (ratio >= 1.5 || (ratio >= 0 && ratio <= .75) ||
            (ratio >= -1.5 && ratio <= -.75))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    static boolean isObstacle(char hexside)
    {
        return (hexside != ' ') && (hexside != 'r');
    }

    /** Check LOS, going to the left of hexspines if argument left is true, or
     *  to the right if it is false. */
    private boolean isLOSBlockedDir(BattleHex initialHex, BattleHex currentHex,
        BattleHex finalHex, boolean left, int strikeElevation,
        boolean strikerAtop, boolean strikerAtopCliff, boolean midObstacle,
        boolean midCliff, boolean midChit, int totalObstacles, int totalWalls)
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
                if (hexside == 'w')
                {
                    totalWalls++;
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
                if (hexside == 'w')
                {
                    totalWalls++;
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
                    totalWalls++;
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
                if (hexside == 'w')
                {
                    totalWalls++;
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
            if (totalObstacles >= 3 && (!strikerAtop || !targetAtop) &&
                (!strikerAtopCliff && !targetAtopCliff))
            {
                return true;
            }

            // If there are two walls, striker or target must be at elevation
            //     2 and range must not be 3.
            if (totalWalls >= 2 &&
                getRange(initialHex, finalHex, false) == 3)
            {
                return true;
            }

            // Success!
            return false;
        }
        else
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
                if (hexside == 'w')
                {
                    totalWalls++;
                }
            }
        }

        // hes that block LOS.
        if (nextHex.blockLineOfSight())
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
            strikeElevation, strikerAtop, strikerAtopCliff,
            midObstacle, midCliff, midChit, totalObstacles, totalWalls);
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
        int strikeElevation = Math.min(hex1.getElevation(),
            hex2.getElevation());

        if (yDist == 0 || Math.abs(yDist) == 1.5 * Math.abs(xDist))
        {
            // Hexspine; try both sides.
            return (isLOSBlockedDir(hex1, hex1, hex2, true, strikeElevation,
                false, false, false, false, false, 0, 0) &&
                isLOSBlockedDir(hex1, hex1, hex2, false, strikeElevation,
                false, false, false, false, false, 0, 0));
        }
        else
        {
            return isLOSBlockedDir(hex1, hex1, hex2, toLeft(xDist, yDist),
                strikeElevation, false, false, false, false, false, 0, 0);
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
        else if (!critter.useMagicMissile() && (range < 3 ||
            target.isLord() || isLOSBlocked(currentHex, targetHex)))
        {
            return false;
        }

        return true;
    }

    /** Return the hexside direction of the path from hex1 to hex2.
     *  Sometimes two directions are possible.  If the left parameter
     *  is set, the direction further left will be given.  Otherwise,
     *  the direction further right will be given. */
    public static int getDirection(BattleHex hex1, BattleHex hex2,
        boolean left)
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
            else  // yDist == 0
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
        else  // xDist < 0
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
            else  // yDist == 0
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
    private static int countBrambleHexesDir(BattleHex hex1, BattleHex hex2,
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
        if (nextHex.getTerrain() == 'r')
        {
            count++;
        }

        return countBrambleHexesDir(nextHex, hex2, left, count);
    }

    // Return the number of intervening bramble hexes.  If LOS is along a
    // hexspine and there are two choices, pick the lower one.
    public static int countBrambleHexes(BattleHex hex1, BattleHex hex2)
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
            // Hexspine; try both sides.
            return Math.min(countBrambleHexesDir(hex1, hex2, true, 0),
                countBrambleHexesDir(hex1, hex2, false, 0));
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
        if (critter == null)
        {
            return false;
        }

        // Allow null moves.
        if (hexLabel.equals(critter.getCurrentHexLabel()))
        {
            Log.event(critter.getDescription() + " does not move");
            // Call moveToHex() anyway to sync client.
            critter.moveToHex(hexLabel, true);
            return true;
        }
        else if (showMoves(critter, false).contains(hexLabel))
        {
            Log.event(critter.getName() + " moves from " +
                critter.getCurrentHexLabel() + " to " + hexLabel);
            critter.moveToHex(hexLabel, true);
            return true;
        }
        else
        {
            Log.warn(critter.getName() + " in " +
                critter.getCurrentHexLabel() +
                " tried to illegally move to " + hexLabel);
            return false;
        }
    }

    /** A streamlined version of doMove for the AI. If legal, move critter
     *  to hex and return true. Else return false.  Do not allow null moves.
     */
    boolean testMove(Critter critter, String hexLabel)
    {
        if (showMoves(critter, false).contains(hexLabel))
        {
            critter.moveToHex(hexLabel, false);
            return true;
        }
        return false;
    }


    void cleanup()
    {
        battleOver = true;
        game.finishBattle(masterHexLabel, attackerEntered);
    }


    /** Return a list of all critters in the battle. */
    private List getAllCritters()
    {
        List critters = new ArrayList();
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


    boolean isOccupied(String hexLabel)
    {
        Iterator it = getAllCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (hexLabel.equals(critter.getCurrentHexLabel()))
            {
                return true;
            }
        }
        return false;
    }

    boolean isOccupied(BattleHex hex)
    {
        return isOccupied(hex.getLabel());
    }

    Critter getCritter(BattleHex hex)
    {
        return getCritter(hex.getLabel());
    }

    Critter getCritter(String hexLabel)
    {
        Iterator it = getAllCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (hexLabel.equals(critter.getCurrentHexLabel()))
            {
                return critter;
            }
        }
        return null;
    }

    Critter getCritter(int tag)
    {
        Iterator it = getAllCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getTag() == tag)
            {
                return critter;
            }
        }
        return null;
    }
}

