import java.util.*;
import javax.swing.*;

/**
 * Class Battle holds data about a Titan battle.
 *
 * @version $Id$
 * @author David Ripton
 */

public final class Battle
{
    // Phases of a battle turn
    public static final int SUMMON = 0;
    public static final int RECRUIT = 1;
    public static final int MOVE = 2;
    public static final int FIGHT = 3;
    public static final int STRIKEBACK = 4;

    // Angel-summoning states
    public static final int NO_KILLS = 0;
    public static final int FIRST_BLOOD = 1;
    public static final int TOO_LATE = 2;

    // A big number
    public static final int BIGNUM = 99;

    // Legion tags
    public static final int DEFENDER = 0;
    public static final int ATTACKER = 1;


    private Game game;
    private BattleMap map;
    private String attackerId;
    private String defenderId;
    private String [] legions = new String[2];
    private int activeLegionNum;
    private String masterHexLabel;
    private BattleDice battleDice;
    private int turnNumber;
    private int phase;
    private int summonState = NO_KILLS;
    private int carryDamage;
    private boolean critterSelected;
    private boolean attackerElim;
    private boolean defenderElim;
    private boolean battleOver;
    private boolean attackerEntered;
    private boolean conceded;
    private boolean driftDamageApplied;
    /** Stack of critters moved, to allow multiple levels of undo. */
    private LinkedList lastCrittersMoved = new LinkedList();


    public Battle(Game game, String attackerId, String defenderId,
        int activeLegionNum, String masterHexLabel, int turnNumber, int phase)
    {
        this.game = game;
        this.masterHexLabel = masterHexLabel;
        this.defenderId = defenderId;
        this.attackerId = attackerId;
        legions[0] = defenderId;
        legions[1] = attackerId;
        this.activeLegionNum = activeLegionNum;
        this.turnNumber = turnNumber;
        this.phase = phase;
        map = new BattleMap(masterHexLabel, this);
    }


    /** We need to do two-stage construction so that game.battle
     *  is non-null earlier. */
    public void init()
    {
        setupPhase();

        if (game != null && game.getOption(Options.showDice))
        {
            initBattleDice();
        }
    }

    /** No-arg constructor for AICopy() */
    public Battle()
    {
    }

    /** Make a deep copy for the AI. */
    public Battle AICopy(Game game)
    {
        Battle newBattle = new Battle();

        newBattle.game = game;
        newBattle.masterHexLabel = masterHexLabel;
        newBattle.defenderId = defenderId;
        newBattle.attackerId = attackerId;
        newBattle.legions[0] = defenderId;
        newBattle.legions[1] = attackerId;
        newBattle.activeLegionNum = activeLegionNum;
        newBattle.turnNumber = turnNumber;
        newBattle.phase = phase;

        newBattle.map = map.AICopy();
        newBattle.map.setBattle(newBattle);

        newBattle.summonState = summonState;
        newBattle.carryDamage = carryDamage;
        newBattle.critterSelected = critterSelected;
        newBattle.attackerElim = attackerElim;
        newBattle.defenderElim = defenderElim;
        newBattle.attackerEntered = attackerEntered;
        newBattle.conceded = conceded;
        newBattle.driftDamageApplied = driftDamageApplied;

        return newBattle;
    }


    private static String getPhaseName(int phase)
    {
        switch (phase)
        {
            case SUMMON:
                return "Summon";
            case RECRUIT:
                return "Recruit";
            case MOVE:
                return "Move";
            case FIGHT:
                return "Fight";
            case STRIKEBACK:
                return "Strikeback";
            default:
                return "?????";
        }
    }


    public Game getGame()
    {
        return game;
    }


    public Player getActivePlayer()
    {
        return game.getPlayerByMarkerId(legions[activeLegionNum]);
    }


    public Legion getAttacker()
    {
        return game.getLegionByMarkerId(attackerId);
    }


    public Legion getDefender()
    {
        return game.getLegionByMarkerId(defenderId);
    }


    public int getActiveLegionNum()
    {
        return activeLegionNum;
    }


    public Legion getActiveLegion()
    {
        return getLegion(activeLegionNum);
    }


    public Legion getInactiveLegion()
    {
        return getLegion((activeLegionNum + 1) & 1);
    }


    public Legion getLegion(int legionNum)
    {
        if (legionNum == DEFENDER)
        {
            return getDefender();
        }
        else if (legionNum == ATTACKER)
        {
            return getAttacker();
        }
        else
        {
            return null;
        }
    }


    public Legion getLegionByPlayer(Player player)
    {
        Legion attacker = getAttacker();
        if (attacker != null && attacker.getPlayerName().equals(
            player.getName()))
        {
            return attacker;
        }
        else
        {
            Legion defender = getDefender();
            if (defender != null && defender.getPlayerName().equals(
                player.getName()))
            return defender;
            else
            {
                return null;
            }
        }
    }


    public MasterHex getMasterHex()
    {
        return MasterBoard.getHexFromLabel(masterHexLabel);
    }


    public char getTerrain()
    {
        return getMasterHex().getTerrain();
    }


    public int getPhase()
    {
        return phase;
    }


    public int getTurnNumber()
    {
        return turnNumber;
    }


    public boolean isAttackerElim()
    {
        return attackerElim;
    }


    public boolean isDefenderElim()
    {
        return defenderElim;
    }


    public boolean isOver()
    {
        return battleOver;
    }


    public void advancePhase()
    {
        if (phase == SUMMON)
        {
            phase = MOVE;
            Game.logEvent("Battle phase advances to " + getPhaseName(phase));
            setupMove();
        }

        else if (phase == RECRUIT)
        {
            phase = MOVE;
            Game.logEvent("Battle phase advances to " + getPhaseName(phase));
            setupMove();
        }

        else if (phase == MOVE)
        {
            // IF the attacker makes it to the end of his first movement
            // phase without conceding, even if he left all legions
            // off-board, the defender can recruit.
            if (activeLegionNum == ATTACKER && !conceded)
            {
                attackerEntered = true;
            }
            phase = FIGHT;
            Game.logEvent("Battle phase advances to " + getPhaseName(phase));
            setupFight();
        }

        else if (phase == FIGHT)
        {
            // We switch the active legion between the fight and strikeback
            // phases, not at the end of the player turn.
            activeLegionNum = (activeLegionNum + 1) & 1;
            driftDamageApplied = false;
            phase = STRIKEBACK;
            Game.logEvent("Battle phase advances to " + getPhaseName(phase));
            setupFight();
        }

        else if (phase == STRIKEBACK)
        {
            removeDeadCreatures();
            checkForElimination();

            // Make sure the battle isn't over before continuing.
            if (!attackerElim && !defenderElim)
            {
                // Active legion is the one that was striking back.
                if (activeLegionNum == ATTACKER)
                {
                    phase = SUMMON;
                    Game.logEvent(getActivePlayer().getName() +
                        "'s battle turn, number " + turnNumber);
                    setupSummon();
                    startSummoningAngel();
                }
                else
                {
                    turnNumber++;
                    if (turnNumber > 7)
                    {
                        Game.logEvent("Time loss");
                        Legion attacker = getAttacker();
                        // Time loss.  Attacker is eliminated but defender
                        //    gets no points.
                        if (attacker.hasTitan())
                        {
                            // This is the attacker's titan stack, so the
                            // defender gets his markers plus half points
                            // for his unengaged legions.
                            Player player = attacker.getPlayer();
                            attacker.remove();
                            player.die(getDefender().getPlayer(), true);
                        }
                        else
                        {
                            attacker.remove();
                        }
                        cleanup();
                    }
                    else
                    {
                        phase = RECRUIT;
                        setupRecruit();
                        Player player = getActivePlayer();
                        if (player != null)
                        {
                            Game.logEvent(player.getName() +
                            "'s battle turn, number " + turnNumber);
                        }
                    }
                }
            }
        }
    }


    private void setupPhase()
    {
        switch (getPhase())
        {
            case Battle.SUMMON:
                setupSummon();
                break;
            case Battle.RECRUIT:
                setupRecruit();
                break;
            case Battle.MOVE:
                setupMove();
                break;
            case Battle.FIGHT:
            case Battle.STRIKEBACK:
                setupFight();
                break;
            default:
                Game.logError("Bogus phase");
        }
    }


    private void setupSummon()
    {
        map.setupSummonMenu();
    }


    private void setupRecruit()
    {
        map.setupRecruitMenu();
        recruitReinforcement();
    }


    private void setupMove()
    {
        // If there are no legal moves, move on.
        if (highlightMovableCritters() < 1)
        {
            advancePhase();
        }
        else
        {
            map.setupMoveMenu();

            Player player = getActivePlayer();
            if (player.getOption(Options.autoBattleMove))
            {
                player.aiBattleMove();
            }
        }
    }


    private void setupFight()
    {
        applyDriftDamage();

        // If there are no possible strikes, move on.
        if (highlightCrittersWithTargets() < 1)
        {
            advancePhase();
        }
        else
        {
            map.setupFightMenu();

            // Automatically perform forced strikes if applicable.
            Player player = getActivePlayer();
            if (player.getOption(Options.autoStrike))
            {
                player.aiStrike(getActiveLegion(), this, false, false);
            }
            else if (player.getOption(Options.autoForcedStrike))
            {
                makeForcedStrikes(false);
            }

            // If there are no possible strikes left, move on.
            if (highlightCrittersWithTargets() < 1)
            {
                commitStrikes();
                advancePhase();
            }
        }
    }


    public BattleMap getBattleMap()
    {
        return map;
    }


    public int getSummonState()
    {
        return summonState;
    }


    public void setSummonState(int summonState)
    {
        this.summonState = summonState;
    }


    private void startSummoningAngel()
    {
        if (summonState == Battle.FIRST_BLOOD)
        {
            if (getAttacker().canSummonAngel() && game != null)
            {
                game.createSummonAngel(getAttacker());
            }

            // This is the last chance to summon an angel until the
            // battle is over.
            summonState = Battle.TOO_LATE;
        }

        if (game == null || game.getSummonAngel() == null)
        {
            if (phase == SUMMON)
            {
                advancePhase();
            }
        }
    }


    /** Called from Game after the SummonAngel finishes. */
    public void finishSummoningAngel(boolean placeNewChit)
    {
        if (placeNewChit)
        {
            map.placeNewChit(getAttacker());
        }

        if (phase == SUMMON)
        {
            advancePhase();
        }

        // Bring the BattleMap back to the front.
        map.getFrame().show();
    }


    public void recruitReinforcement()
    {
        Legion defender = getDefender();
        if (turnNumber == 4 && defender.canRecruit())
        {
            // Allow recruiting a reinforcement.
            Creature recruit;
            Player player = defender.getPlayer();
            if (player.getOption(Options.autoRecruit))
            {
                recruit = player.aiReinforce(defender);
            }
            else
            {
                recruit = PickRecruit.pickRecruit(map.getFrame(), defender);
            }
            if (recruit != null && game != null)
            {
                game.doRecruit(recruit, defender, map.getFrame());
            }

            if (defender.hasRecruited())
            {
                map.placeNewChit(defender);
            }
        }

        advancePhase();
    }


    public int getCarryDamage()
    {
        return carryDamage;
    }


    public void setCarryDamage(int carryDamage)
    {
        this.carryDamage = carryDamage;
    }


    /** Recursively find moves from this hex.  Return an array of hex IDs for
     *  all legal destinations.  Do not double back. */
    private Set findMoves(BattleHex hex, Creature creature, boolean flies,
        int movesLeft, int cameFrom)
    {
        HashSet set = new HashSet();

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
                    if (isOccupied(neighbor))
                    {
                        entryCost = BattleHex.IMPASSIBLE_COST;
                    }
                    else
                    {
                        entryCost = neighbor.getEntryCost(creature,
                            reverseDir);
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
                            set.addAll(findMoves(neighbor, creature, flies,
                                movesLeft - entryCost, reverseDir));
                        }
                    }

                    // Fliers can fly over any non-volcano hex for 1 movement
                    // point.  Only dragons can fly over volcanos.
                    if (flies && movesLeft > 1 && (neighbor.getTerrain() != 'v'
                        || creature.getName().equals("Dragon")))
                    {
                        set.addAll(findMoves(neighbor, creature, flies,
                            movesLeft - 1, reverseDir));
                    }
                }
            }
        }

        return set;
    }


    private Set findUnoccupiedTowerHexes()
    {
        HashSet set = new HashSet();

        BattleHex centerHex = map.getCenterHex();

        if (!isOccupied(centerHex))
        {
            set.add(centerHex.getLabel());
        }
        for (int i = 0; i < 6; i++)
        {
            BattleHex hex = centerHex.getNeighbor(i);
            if (!isOccupied(hex))
            {
                set.add(hex.getLabel());
            }
        }

        return set;
    }


    /** Find all legal moves for this critter. The returned list
     *  contains hex IDs, not hexes. */
    public Set showMoves(Critter critter)
    {
        Set set = new HashSet();

        if (!critter.hasMoved() && !critter.isInContact(false))
        {
            if (getTerrain() == 'T' && getTurnNumber() == 1 &&
                getActivePlayer() == getDefender().getPlayer())
            {
                set = findUnoccupiedTowerHexes();
            }
            else
            {
                set = findMoves(critter.getCurrentHex(), critter,
                    critter.isFlier(), critter.getSkill(), -1);
            }
        }

        return set;
    }


    public void highlightMoves(Critter critter)
    {
        Set set = showMoves(critter);
        map.unselectAllHexes();
        map.selectHexesByLabels(set);
    }


    public void setLastCritterMoved(Critter critter)
    {
        lastCrittersMoved.addFirst(critter);
    }


    public void undoLastMove()
    {
        critterSelected = false;

        if (!lastCrittersMoved.isEmpty())
        {
            Critter critter = (Critter)lastCrittersMoved.removeFirst();
            critter.undoMove();
        }

        highlightMovableCritters();
    }


    public void undoAllMoves()
    {
        critterSelected = false;

        Iterator it = getActiveLegion().getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.hasMoved())
            {
                critter.undoMove();
            }
        }

        highlightMovableCritters();
    }


    /** Mark all of the conceding player's critters as dead. */
    private void concede(Player player)
    {
        conceded = true;

        Legion legion = getLegionByPlayer(player);
        Iterator it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            critter.setDead(true);
        }
    }


    private void tryToConcede(Player player)
    {
        // XXX: Concession timing is tricky.

        String [] options = new String[2];
        options[0] = "Yes";
        options[1] = "No";
        int answer = JOptionPane.showOptionDialog(map,
            "Are you sure you wish to concede the battle?",
            "Confirm Concession?",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, options, options[1]);

        if (answer == JOptionPane.YES_OPTION)
        {
            Game.logEvent(player.getName() + " concedes the battle");
            concede(player);
            advancePhase();
        }
    }


    public void tryToConcede()
    {
        tryToConcede(getActivePlayer());
    }


    /** Return a set of hex labels for hex labels with critters eligible
     *  to move. */
    public Set findMovableCritters()
    {
        HashSet set = new HashSet();
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

    /** Select all hexes containing critters eligible to move.
     *  Return the number of hexes selected (not the number
     *  of critters). */
    public int highlightMovableCritters()
    {
        Set set = findMovableCritters();
        map.unselectAllHexes();
        map.selectHexesByLabels(set);
        return set.size();
    }


    /** Return true if any creatures have been left off-board. */
    private boolean anyOffboardCreatures()
    {
        Legion legion = getActiveLegion();
        Iterator it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getCurrentHex().isEntrance())
            {
                return true;
            }
        }
        return false;
    }


    private boolean confirmLeavingCreaturesOffboard()
    {
        String [] options = new String[2];
        options[0] = "Yes";
        options[1] = "No";
        int answer = JOptionPane.showOptionDialog(map,
            "Are you sure you want to leave creatures offboard?",
            "Confirm Leaving Creatures Offboard?",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, options, options[1]);

        return (answer == JOptionPane.YES_OPTION);
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
    }


    private void commitMoves()
    {
        lastCrittersMoved.clear();

        Legion legion = getActiveLegion();
        Iterator it = legion.getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            critter.commitMove();
        }
    }


    public void doneWithMoves()
    {
        if (anyOffboardCreatures())
        {
            if (confirmLeavingCreaturesOffboard())
            {
                removeOffboardCreatures();
            }
            else
            {
                return;
            }
        }
        commitMoves();
        advancePhase();
    }


    public void applyDriftDamage()
    {
        // Drift hexes are only found on the tundra map.
        // Drift damage is applied only once per player turn,
        //    during the strike phase.
        if (getTerrain() == 't' && phase == FIGHT &&
            !driftDamageApplied)
        {
            Iterator it = getAllCritters().iterator();
            while (it.hasNext())
            {
                Critter critter = (Critter)it.next();
                if (critter.getCurrentHex().getTerrain() == 'd' &&
                    !critter.isNativeDrift())
                {
                    Game.logEvent(critter.getName() + " takes drift damage");
                    critter.wound(1);
                    driftDamageApplied = true;
                }
            }
        }
    }


    public boolean isDriftDamageApplied()
    {
        return driftDamageApplied;
    }


    public void setDriftDamageApplied(boolean driftDamageApplied)
    {
        this.driftDamageApplied = driftDamageApplied;
    }

    public void clearAllCarries()
    {
        boolean needToRepaint = false;

        Iterator it = getInactiveLegion().getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getCarryFlag())
            {
                critter.setCarryFlag(false);
                needToRepaint = true;
            }
        }
        carryDamage = 0;

        if (needToRepaint)
        {
            map.unselectAllHexes();
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

        Legion donor = null;

        for (int i = DEFENDER; i <= ATTACKER; i++)
        {
            Legion legion = getLegion(i);
            Iterator it = legion.getCritters().iterator();
            while (it.hasNext())
            {
                Critter critter = (Critter)it.next();
                if (critter.isDead())
                {
                    // After turn 1, offboard creatures are returned to the
                    // stacks or the legion they were summoned from, with
                    // no points awarded.
                    if (critter.getCurrentHex().isEntrance() &&
                        getTurnNumber() > 1)
                    {
                        if (critter.isAngel())
                        {
                            Player player = legion.getPlayer();
                            donor = player.getLastLegionSummonedFrom();
                            donor.addCreature(critter, false);
                            donor.getCurrentHex().repaint();
                            // This summon doesn't count; the player can
                            // summon again later this turn.
                            player.setSummoned(false);
                        }
                        else
                        {
                            // Reinforcement.
                            game.getCaretaker().putOneBack(critter);
                            // This recruit doesn't count.
                            legion.setRecruited(false);
                        }
                    }
                    else if (legion == attacker)
                    {
                        defender.addToBattleTally(critter.getPointValue());
                    }
                    else  // legion == defender
                    {
                        attacker.addToBattleTally(critter.getPointValue());

                        // Creatures left offboard do not trigger angel
                        // summoning.
                        if (summonState == Battle.NO_KILLS &&
                            !critter.getCurrentHex().isEntrance())
                        {
                            summonState = Battle.FIRST_BLOOD;
                        }
                    }

                    boolean putBack = true;
                    // If an angel or archangel was returned to its donor
                    // instead of the stack, then don't put it back on
                    // the stack.
                    if (critter.isAngel() && donor != null)
                    {
                        putBack = false;
                        donor = null;
                    }
                    legion.prepareToRemoveCritter(critter, putBack);

                    if (critter.isTitan())
                    {
                        legion.getPlayer().eliminateTitan();
                    }

                    BattleHex hex = critter.getCurrentHex();
                    // Remove critter from iterator rather than list to
                    // prevent concurrent modification problems.
                    it.remove();
                    hex.repaint();
                }
                else  // critter is alive
                {
                    if (legion == attacker)
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

        Player player = attacker.getPlayer();
        if (player == null || player.isTitanEliminated())
        {
            attackerElim = true;
        }
        player = defender.getPlayer();
        if (player == null || player.isTitanEliminated())
        {
            defenderElim = true;
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
Game.logDebug("mutual titan elimination");
            defender.getPlayer().die(null, false);
            attacker.getPlayer().die(null, true);
            cleanup();
        }

        // Check for single Titan elimination.
        else if (attackerTitanDead)
        {
Game.logDebug("attacker titan elimination");
            if (defenderElim)
            {
                defender.remove();
            }
            else
            {
                defender.addBattleTallyToPoints();
            }
            attacker.getPlayer().die(defender.getPlayer(), true);
            cleanup();
        }
        else if (defenderTitanDead)
        {
Game.logDebug("defender titan elimination");
            if (attackerElim)
            {
                attacker.remove();
            }
            else
            {
                attacker.addBattleTallyToPoints();
            }
            defender.getPlayer().die(attacker.getPlayer(), true);
            cleanup();
        }

        // Check for mutual legion elimination.
        else if (attackerElim && defenderElim)
        {
Game.logDebug("mutual");
            attacker.remove();
            defender.remove();
            cleanup();
        }

        // Check for single legion elimination.
        else if (attackerElim)
        {
Game.logDebug("attacker eliminated");
            defender.addBattleTallyToPoints();
            attacker.remove();
            cleanup();
        }
        else if (defenderElim)
        {
Game.logDebug("defender eliminated");
            attacker.addBattleTallyToPoints();
            defender.remove();
            cleanup();
        }
    }


    private void commitStrikes()
    {
        Iterator it = getActiveLegion().getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            critter.setStruck(false);
        }
    }

    /** Return the set of hex labels for hexes with critters that have
     *  valid strike targets. */
    public Set findCrittersWithTargets()
    {
        HashSet set = new HashSet();
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

    /** Select hexes containing critters that have valid strike targets.
     *  Return the number of selected hexes. */
    public int highlightCrittersWithTargets()
    {
        Set set = findCrittersWithTargets();
        map.unselectAllHexes();
        map.selectHexesByLabels(set);
        return set.size();
    }


    private boolean isForcedStrikeRemaining()
    {
        Iterator it = getActiveLegion().getCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (!critter.hasStruck() && critter.isInContact(false))
            {
                return true;
            }
        }
        return false;
    }


    public Critter getCritterFromHexLabel(String hexLabel)
    {
        return getCritter(hexLabel);
    }


    /** Perform strikes for any creature that is forced to strike
     *  and has only one legal target. Forced strikes will never
     *  generate carries, since there's only one target. If
     *  rangestrike is true, also perform rangestrikes for
     *  creatures with only one target, even though they're not
     *  technically forced. */
    public void makeForcedStrikes(boolean rangestrike)
    {
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
                        Critter target = getCritterFromHexLabel(hexLabel);
                        critter.strike(target, false);

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


    public void doneWithStrikes()
    {
        // Advance only if there are no unresolved strikes.
        if (isForcedStrikeRemaining())
        {
            highlightCrittersWithTargets();
            JOptionPane.showMessageDialog(map,
                "Engaged creatures must strike.");
        }
        else
        {
            commitStrikes();
            advancePhase();
        }
    }


    /** Return a set of hex labels for hexes containing targets that the
     *  critter may strike.  Only include rangestrikes if rangestrike
     *  is true. */
    public Set findStrikes(Critter critter, boolean rangestrike)
    {
        HashSet set = new HashSet();

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
            if (currentHex.getHexside(i) != 'c' &&
                currentHex.getOppositeHexside(i) != 'c')
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
            getPhase() != STRIKEBACK &&
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


    public int countStrikes(Critter critter, boolean rangestrike)
    {
        return findStrikes(critter, rangestrike).size();
    }


    /** Highlight all hexes with targets that the critter can strike.
     *  Return the number of hexes highlighted. */
    public int highlightStrikes(Critter critter)
    {
        Set set = findStrikes(critter, true);
        map.unselectAllHexes();
        map.selectHexesByLabels(set);
        return set.size();
    }

    /** Return the set of hex labels for hexes with valid carry targets. */
    public Set findCarryTargets()
    {
        HashSet set = new HashSet();

        Iterator it = getInactiveLegion().getCritters().iterator();
        while (it.hasNext())
        {
            Critter target = (Critter)it.next();
            if (target.getCarryFlag())
            {
                set.add(target.getCurrentHex().getLabel());
            }
        }

        return set;
    }


    public int highlightCarries()
    {
        Set set = findCarryTargets();
        map.unselectAllHexes();
        map.selectHexesByLabels(set);
        return set.size();
    }


    public void applyCarries(Critter target)
    {
        int dealt = carryDamage;
        carryDamage = target.wound(carryDamage);
        dealt -= carryDamage;
        target.setCarryFlag(false);

        Game.logEvent(dealt + (dealt == 1 ? " hit carries to " :
            " hits carry to ") + target.getDescription());

        if (carryDamage <= 0 || findCarryTargets().isEmpty())
        {
            clearAllCarries();
        }
        else
        {
            String label = target.getCurrentHex().getLabel();
            map.unselectHexByLabel(label);
            Game.logEvent(carryDamage + (carryDamage == 1 ?
                " carry available" : " carries available"));
            if (game != null && game.getOption(Options.showDice))
            {
                battleDice.setCarries(carryDamage);
                battleDice.showRoll();
            }
        }
    }


    private static final int OUT_OF_RANGE = 5;

    /** Return the range in hexes from hex1 to hex2.  Titan ranges are
     *  inclusive at both ends. */
    public static int getRange(BattleHex hex1, BattleHex hex2,
        boolean allowEntrance)
    {
        if (hex1 == null || hex2 == null)
        {
            Game.logDebug("passed null hex to getRange()");
            return OUT_OF_RANGE;
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
                return OUT_OF_RANGE;
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
        int min = OUT_OF_RANGE;
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


    /** Check LOS, going to the left of hexspines if argument left is true, or
     *  to the right if it is false. */
    private boolean isLOSBlockedDir(BattleHex initialHex, BattleHex currentHex,
        BattleHex finalHex, boolean left, int strikeElevation,
        boolean strikerAtop, boolean strikerAtopCliff, boolean midObstacle,
        boolean midCliff, boolean midChit, int totalObstacles)
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
            if (hexside != ' ')
            {
                strikerAtop = true;
                totalObstacles++;
                if (hexside == 'c')
                {
                    strikerAtopCliff = true;
                }
            }

            if (hexside2 != ' ')
            {
                midObstacle = true;
                totalObstacles++;
                if (hexside2 == 'c')
                {
                    midCliff = true;
                }
            }
        }
        else if (nextHex == finalHex)
        {
            if (hexside != ' ')
            {
                midObstacle = true;
                totalObstacles++;
                if (hexside == 'c')
                {
                    midCliff = true;
                }
            }

            if (hexside2 != ' ')
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
            if (totalObstacles >= 3 && (!strikerAtop || !targetAtop) &&
                (!strikerAtopCliff && !targetAtopCliff))
            {
                return true;
            }

            // If there are two walls, striker or target must be at elevation
            //     2 and range must not be 3.
            if (getTerrain() == 'T' && totalObstacles >= 2 &&
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

            if (hexside != ' ' || hexside2 != ' ')
            {
                midObstacle = true;
                totalObstacles++;
                if (hexside == 'c' || hexside2 == 'c')
                {
                    midCliff = true;
                }
            }
        }

        // Trees block LOS.
        if (nextHex.getTerrain() == 't')
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
            midObstacle, midCliff, midChit, totalObstacles);
    }


    /** Check to see if the LOS from hex1 to hex2 is blocked.  If the LOS
     *  lies along a hexspine, check both and return true only if both are
     *  blocked. */
    private boolean isLOSBlocked(BattleHex hex1, BattleHex hex2)
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
                false, false, false, false, false, 0) &&
                isLOSBlockedDir(hex1, hex1, hex2, false, strikeElevation,
                false, false, false, false, false, 0));
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

        // Only warlocks can rangestrike at range 2, rangestrike Lords,
        // or rangestrike without LOS.
        else if (!critter.getName().equals("Warlock") && (range < 3 ||
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
    private int countBrambleHexesDir(BattleHex hex1, BattleHex hex2,
        boolean left, int previousCount)
    {
        int count = previousCount;

        // Offboard hexes are not allowed.
        if (hex1.getXCoord() == -1 || hex2.getXCoord() == -1)
        {
            return BIGNUM;
        }

        int direction = getDirection(hex1, hex2, left);

        BattleHex nextHex = hex1.getNeighbor(direction);
        if (nextHex == null)
        {
            return BIGNUM;
        }

        if (nextHex == hex2)
        {
            // Success!
            return count;
        }

        // Trees block LOS.
        if (nextHex.getTerrain() == 't')
        {
            return BIGNUM;
        }

        // All creatures block LOS.  (There are no height differences on
        // maps with bramble.)
        if (isOccupied(nextHex))
        {
            return BIGNUM;
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
    public int countBrambleHexes(BattleHex hex1, BattleHex hex2)
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
            return BIGNUM;
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

        else if (xDist / yDist > 0)
        {
            // LOS to left
            return countBrambleHexesDir(hex1, hex2, true, 0);
        }

        else
        {
            // LOS to right
            return countBrambleHexesDir(hex1, hex2, false, 0);
        }
    }


    public BattleDice getBattleDice()
    {
        return battleDice;
    }


    public void initBattleDice()
    {
        battleDice = new BattleDice(game);
    }


    public void disposeBattleDice()
    {
        if (battleDice != null)
        {
            battleDice.dispose();
            battleDice = null;
        }
    }


    public void actOnCritter(Critter critter)
    {
        // Only the active player can move or strike.
        if (critter != null && critter.getPlayer() == getActivePlayer())
        {
            critterSelected = true;

            // Put selected chit at the top of the z-order.
            if (getActiveLegion().moveToTop(critter));
            {
                critter.getCurrentHex().repaint();
            }

            switch (getPhase())
            {
                case MOVE:
                    // Highlight all legal destinations for this critter.
                    highlightMoves(critter);
                    break;

                case FIGHT:
                case STRIKEBACK:
                    // Leave carry mode.
                    clearAllCarries();

                    // Highlight all legal strikes for this critter.
                    highlightStrikes(critter);
                    break;

                default:
                    break;
            }
        }
    }


    public void actOnHex(BattleHex hex)
    {
        switch (getPhase())
        {
            case MOVE:
                if (critterSelected)
                {
                    doMove(getActiveLegion().getCritter(0), hex);
                }
                break;

            case FIGHT:
            case STRIKEBACK:
                if (getCarryDamage() > 0)
                {
                    applyCarries(getCritter(hex));
                }
                else if (critterSelected)
                {
                    getActiveLegion().getCritter(0).strike(
                        getCritter(hex), false);
                    critterSelected = false;
                }

                if (getCarryDamage() == 0)
                {
                    if (game != null)
                    {
                        Player player = getActivePlayer();
                        if (player.getOption(Options.autoForcedStrike))
                        {
                            makeForcedStrikes(false);
                        }
                    }
                    highlightCrittersWithTargets();
                }
                break;

            default:
                break;
        }
    }


    /** If legal, move critter to hex *  and return true. Else return false. */
    public boolean doMove(Critter critter, BattleHex hex)
    {
        String hexLabel = hex.getLabel();

        // Allow null moves.
        if (hexLabel.equals(critter.getCurrentHexLabel()))
        {
            Game.logEvent(critter.getDescription() + " does not move");
            return true;
        }
        else if (showMoves(critter).contains(hexLabel))
        {
            Game.logEvent(critter.getName() + " moves from " +
                critter.getCurrentHexLabel() + " to " + hexLabel);
            critter.moveToHex(hex);
            critterSelected = false;
            highlightMovableCritters();
            return true;
        }
        else
        {
            Game.logEvent(critter.getName() + " in " +
                critter.getCurrentHexLabel() +
                " tried to illegally move to " + hexLabel);
            return false;
        }
    }


    public void actOnMisclick()
    {
        switch (getPhase())
        {
            case MOVE:
                critterSelected = false;
                highlightMovableCritters();
                break;

            case FIGHT:
            case STRIKEBACK:
                critterSelected = false;
                highlightCrittersWithTargets();
                break;

            default:
                break;
       }
    }


    public void cleanup()
    {
        disposeBattleDice();
        map.dispose();
        battleOver = true;
        if (game != null)
        {
            game.finishBattle(masterHexLabel, attackerEntered);
        }
    }


    /** Return a list of all critters in the battle. */
    public ArrayList getAllCritters()
    {
        ArrayList critters = new ArrayList();
        critters.addAll(getDefender().getCritters());
        critters.addAll(getAttacker().getCritters());
        return critters;
    }


    public boolean isOccupied(String hexLabel)
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

    public boolean isOccupied(BattleHex hex)
    {
        return isOccupied(hex.getLabel());
    }

    public Critter getCritter(BattleHex hex)
    {
        return getCritter(hex.getLabel());
    }

    public Critter getCritter(String hexLabel)
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

    public ArrayList getCritters(String hexLabel)
    {
        ArrayList critters = new ArrayList();
        Iterator it = getAllCritters().iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (hexLabel.equals(critter.getCurrentHexLabel()))
            {
                critters.add(critter);
            }
        }
        return critters;
    }


    public static void main(String [] args)
    {
        Game game = new Game();
        game.addPlayer("Attacker");
        Player player1 = game.getPlayer(0);
        game.addPlayer("Defender");
        Player player2 = game.getPlayer(1);
        MasterHex hex = MasterBoard.getHexFromLabel("130");
        Legion attacker = new Legion("Bk01", null, hex.getLabel(),
            hex.getLabel(), Creature.archangel, Creature.troll,
            Creature.ranger, Creature.hydra, Creature.griffon,
            Creature.angel, Creature.warlock, null, player1.getName(),
            game);
        player1.addLegion(attacker);
        Legion defender = new Legion("Rd01", null, hex.getLabel(),
            hex.getLabel(), Creature.serpent, Creature.lion,
            Creature.gargoyle, Creature.cyclops, Creature.gorgon,
            Creature.guardian, Creature.minotaur, null, player2.getName(),
            game);
        player2.addLegion(defender);
        attacker.setEntrySide(5);

        Battle battle = new Battle(game, attacker.getMarkerId(),
            defender.getMarkerId(), DEFENDER, hex.getLabel(), 1, MOVE);
        battle.init();
    }
}

