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

    private Game game;
    private BattleMap map;
    private Legion attacker;
    private Legion defender;
    private Legion activeLegion;
    private MasterBoard board;
    private MasterHex masterHex;
    private BattleDice battleDice;

    private int turnNumber;
    private int phase;
    private int summonState = NO_KILLS;
    private int carryDamage;
    private boolean chitSelected;
    private ArrayList critters = new ArrayList();

    /** Stack of critters moved, to allow multiple levels of undo. */
    private LinkedList lastCrittersMoved = new LinkedList();

    private boolean attackerElim;
    private boolean defenderElim;

    private boolean attackerEntered;
    private boolean conceded;
    private boolean driftDamageApplied;


    public Battle(MasterBoard board, Legion attacker, Legion defender,
        Legion activeLegion, MasterHex masterHex, boolean inProgress,
        int turnNumber, int phase)
    {
        this.board = board;
        this.masterHex = masterHex;
        this.defender = defender;
        this.attacker = attacker;
        this.activeLegion = activeLegion;
        this.turnNumber = turnNumber;
        this.phase = phase;
        if (board != null)
        {
            this.game = board.getGame();
        }

        if (!inProgress)
        {
            attacker.clearBattleTally();
            defender.clearBattleTally();
        }

        map = new BattleMap(board, masterHex, this, inProgress);

        setupPhase();

        if (game != null && game.getOption(Game.showDice))
        {
            initBattleDice();
        }
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
        return activeLegion.getPlayer();
    }


    public Legion getAttacker()
    {
        return attacker;
    }


    public Legion getDefender()
    {
        return defender;
    }


    public MasterHex getMasterHex()
    {
        return masterHex;
    }


    public int getPhase()
    {
        return phase;
    }


    public int getTurnNumber()
    {
        return turnNumber;
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
            if (activeLegion == attacker && !conceded)
            {
                attackerEntered = true;
            }
            phase = FIGHT;
            Game.logEvent("Battle phase advances to " + getPhaseName(phase));
            setupFight();
        }

        else if (phase == FIGHT)
        {
            if (activeLegion == defender)
            {
                activeLegion = attacker;
            }
            else
            {
                activeLegion = defender;
            }
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
                if (activeLegion == attacker)
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
                        // Time loss.  Attacker is eliminated but defender
                        //    gets no points.
                        if (attacker.numCreature(Creature.titan) != 0)
                        {
                            // This is the attacker's titan stack, so the
                            // defender gets his markers plus half points
                            // for his unengaged legions.
                            Player player = attacker.getPlayer();
                            attacker.remove();
                            player.die(defender.getPlayer(), true);
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
                        Game.logEvent(getActivePlayer().getName() +
                            "'s battle turn, number " + turnNumber);
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
        if (highlightMovableChits() < 1)
        {
            advancePhase();
        }
        else
        {
            map.setupMoveMenu();
        }
    }


    private void setupFight()
    {
        applyDriftDamage();

        // If there are no possible strikes, move on.
        if (highlightChitsWithTargets() < 1)
        {
            advancePhase();
        }
        else
        {
            map.setupFightMenu();

            // Automatically perform forced strikes if applicable.
            Player player = game.getActivePlayer();
            if (player.getOption(Game.autoForcedStrike))
            {
                makeForcedStrikes();
                // If there are no possible strikes left, move on.
                if (highlightChitsWithTargets() < 1)
                {
                    commitStrikes();
                    advancePhase();
                }
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
            if (attacker.canSummonAngel() && game != null)
            {
                game.createSummonAngel(attacker);
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
            map.placeNewChit(attacker);
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
        if (turnNumber == 4 && defender.canRecruit())
        {
            // Allow recruiting a reinforcement.
            Creature recruit = PickRecruit.pickRecruit(
                map.getFrame(), defender);
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


    public int getNumCritters()
    {
        return critters.size();
    }


    public Collection getCritters()
    {
        return critters;
    }


    public Critter getCritter(int i)
    {
        return (Critter)critters.get(i);
    }


    public void addCritter(Critter critter)
    {
        critters.add(critter);
    }


    /** Recursively find moves from this hex.  Return an array of hex IDs for
     *  all legal destinations.  Do not double back. */
    private Set findMoves(BattleHex hex, Creature creature,
        boolean flies, int movesLeft, int cameFrom)
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

                    int entryCost = neighbor.getEntryCost(creature,
                        reverseDir);
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

        if (!centerHex.isOccupied())
        {
            set.add(centerHex.getLabel());
        }
        for (int i = 0; i < 6; i++)
        {
            BattleHex hex = centerHex.getNeighbor(i);
            if (!hex.isOccupied())
            {
                set.add(hex.getLabel());
            }
        }

        return set;
    }


    /** Find all legal moves for this critter. */
    private Set showMoves(Critter critter)
    {
        Set set = new HashSet();

        if (!critter.hasMoved() && !critter.isInContact(false))
        {
            if (masterHex.getTerrain() == 'T' && getTurnNumber() == 1 &&
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
        chitSelected = false;

        if (!lastCrittersMoved.isEmpty())
        {
            Critter critter = (Critter)lastCrittersMoved.removeFirst();
            critter.undoMove();
        }

        highlightMovableChits();
    }


    public void undoAllMoves()
    {
        chitSelected = false;

        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.hasMoved())
            {
                critter.undoMove();
            }
        }

        highlightMovableChits();
    }


    /** Mark all of the conceding player's critters as dead. */
    private void concede(Player player)
    {
        conceded = true;

        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getPlayer() == player)
            {
                critter.setDead(true);
            }
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
    public Set findMovableChits()
    {
        HashSet set = new HashSet();
        Player player = getActivePlayer();

        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getPlayer() == player)
            {
                if (!critter.hasMoved() && !critter.isInContact(false))
                {
                    BattleHex hex = critter.getCurrentHex();
                    set.add(hex.getLabel());
                }
            }
        }

        return set;
    }

    /** Select all hexes containing critters eligible to move.
     *  Return the number of hexes selected (not the number
     *  of critters). */
    public int highlightMovableChits()
    {
        Set set = findMovableChits();
        map.unselectAllHexes();
        map.selectHexesByLabels(set);
        return set.size();
    }
    
    
    /** Return true if any creatures have been left off-board. */
    private boolean anyOffboardCreatures()
    {
        Player player = getActivePlayer();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getCurrentHex().isEntrance() &&
                critter.getPlayer() == player)
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


    /** If any chits were left off-board, kill them.  If they were newly
     *  summoned or recruited, unsummon or unrecruit them instead. */
    private void removeOffboardCreatures()
    {
        Player player = getActivePlayer();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getCurrentHex().isEntrance() &&
                critter.getPlayer() == player)
            {
                critter.setDead(true);
            }
        }
    }


    private void commitMoves()
    {
        lastCrittersMoved.clear();

        Iterator it = critters.iterator();
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
        if (masterHex.getTerrain() == 't' && phase == FIGHT &&
            !driftDamageApplied)
        {
            Iterator it = critters.iterator();
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

        Iterator it = critters.iterator();
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


    /** Move the passed Critter to the top of the critters array. */
    public void moveToTop(Critter critter)
    {
        if (critters.indexOf(critter) > 0)
        {
            critters.remove(critter);
            critters.add(0, critter);
            // Repainting just this chit doesn't cut it.
            critter.getCurrentHex().repaint();
        }
    }


    private void removeDeadCreatures()
    {
        // Initialize these to true, and then set them to false when a
        // non-dead chit is found.
        attackerElim = true;
        defenderElim = true;

        Legion donor = null;

        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Legion legion = critter.getLegion();
            if (critter.isDead())
            {
                // After turn 1, offboard creatures are returned to the
                // stacks or the legion they were summoned from, with
                // no points awarded.
                if (critter.getCurrentHex().isEntrance() &&
                    getTurnNumber() > 1)
                {
                    if (critter.getName().equals("Angel") ||
                        critter.getName().equals("Archangel"))
                    {
                        Player player = legion.getPlayer();
                        donor = player.getLastLegionSummonedFrom();
                        donor.addCreature(critter, false);
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
                else
                {
                    attacker.addToBattleTally(critter.getPointValue());

                    // Creatures left offboard do not trigger angel summoning.
                    if (summonState == Battle.NO_KILLS &&
                        !critter.getCurrentHex().isEntrance())
                    {
                        summonState = Battle.FIRST_BLOOD;
                    }
                }

                legion.removeCreature(critter, true, false);
                // If an angel or archangel was returned to its donor instead
                // of the stack, then the count must be adjusted.
                if (donor != null)
                {
                    game.getCaretaker().takeOne(critter);
                    donor = null;
                }

                if (critter.getName().equals("Titan"))
                {
                    legion.getPlayer().eliminateTitan();
                }

                BattleHex hex = critter.getCurrentHex();
                hex.removeCritter(critter);
                hex.repaint();

                // Remove from iterator rather than list to prevent
                // concurrent modification problems.
                it.remove();
            }
            else
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


    private void checkForElimination()
    {
        // Check for mutual Titan elimination.
        if (attacker.getPlayer().isTitanEliminated() &&
            defender.getPlayer().isTitanEliminated())
        {
            // Nobody gets any points.
            // Make defender die first, to simplify turn advancing.
            defender.getPlayer().die(null, false);
            attacker.getPlayer().die(null, true);
            cleanup();
        }

        // Check for single Titan elimination.
        else if (attacker.getPlayer().isTitanEliminated())
        {
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
        else if (defender.getPlayer().isTitanEliminated())
        {
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
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            critter.setStruck(false);
        }
    }

    /** Return the set of hex labels for hexes with critters that have
     *  valid strike targets. */
    public Set findChitsWithTargets()
    {
        Player player = getActivePlayer();
        HashSet set = new HashSet();

        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getPlayer() == player)
            {
                if (countStrikes(critter) > 0)
                {
                    set.add(critter.getCurrentHex().getLabel());
                }
            }
        }

        return set;
    }

    /** Select hexes containing critters that have valid strike targets.
     *  Return the number of selected hexes. */
    public int highlightChitsWithTargets()
    {
        Set set = findChitsWithTargets();
        map.unselectAllHexes();
        map.selectHexesByLabels(set);
        return set.size();
    }


    private boolean isForcedStrikeRemaining()
    {
        Player player = getActivePlayer();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getPlayer() == player)
            {
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
     *  generate carries, since there's only one target. */
    public void makeForcedStrikes()
    {
        Player player = getActivePlayer();
        boolean repeat = false;

        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            if (critter.getPlayer() == player)
            {
                if (!critter.hasStruck())
                {
                    Critter target = critter.getForcedStrikeTarget();
                    if (target != null)
                    {
                        critter.strike(target);

                        // If that strike killed the target, it's possible
                        // that some other creature that had two adjacent
                        // enemies now has only one.
                        if (target.isDead())
                        {
                            repeat = true;
                        }
                    }
                }
            }
        }
        if (repeat)
        {
            makeForcedStrikes();
        }
    }


    public void doneWithStrikes()
    {
        // Advance only if there are no unresolved strikes.
        if (isForcedStrikeRemaining())
        {
            highlightChitsWithTargets();
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
     *  critter may strike. */
    private Set findStrikes(Critter critter)
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
                if (targetHex != null && targetHex.isOccupied())
                {
                    Critter target = targetHex.getCritter();
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
        if (!adjacentEnemy && critter.isRangestriker() &&
            getPhase() != STRIKEBACK)
        {
            Iterator it = critters.iterator();
            while (it.hasNext())
            {
                Critter target = (Critter)it.next();
                if (target.getPlayer() != player && !target.isDead())
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


    public int countStrikes(Critter critter)
    {
        return findStrikes(critter).size();
    }


    /** Highlight all hexes with targets that the critter can strike.
     *  Return the number of hexes highlighted. */
    public int highlightStrikes(Critter critter)
    {
        Set set = findStrikes(critter);
        map.unselectAllHexes();
        map.selectHexesByLabels(set);
        return set.size();
    }

    /** Return the set of hex labels for hexes with valid carry targets. */
    public Set findCarryTargets()
    {
        HashSet set = new HashSet();

        Iterator it = critters.iterator();
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


    private void applyCarries(Critter target)
    {
        int dealt = carryDamage;
        carryDamage = target.wound(carryDamage);
        dealt -= carryDamage;
        target.setCarryFlag(false);

        Game.logEvent(dealt + (dealt == 1 ? " hit carries to " :
            " hits carry to ") + target.getName() + " in " +
            target.getCurrentHex().getLabel());

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
            if (game != null && game.getOption(Game.showDice))
            {
                battleDice.setCarries(carryDamage);
                battleDice.showRoll();
            }
        }
    }


    /** Return the range in hexes from hex1 to hex2.  Titan ranges are
     *  inclusive at both ends. */
    public static int getRange(BattleHex hex1, BattleHex hex2)
    {
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

        // Offboard creatures are out of range.
        if (x1 == -1 || x2 == -1)
        {
            xDist = BIGNUM;
        }

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
            if (masterHex.getTerrain() == 'T' && totalObstacles >= 2 &&
                getRange(initialHex, finalHex) == 3)
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

        // Chits block LOS, unless both striker and target are at higher
        //     elevation than the chit, or unless the chit is at the base of
        //     a cliff and the striker or target is atop it.
        if (nextHex.isOccupied() && nextHex.getElevation() >= strikeElevation
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

        int range = getRange(currentHex, targetHex);
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

        // Offboard chits are not allowed.
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

        // All chits block LOS.  (There are no height differences on maps
        //    with bramble.)
        if (nextHex.isOccupied())
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
            chitSelected = true;

            // Put selected chit at the top of the z-order.
            moveToTop(critter);

            switch (getPhase())
            {
                case MOVE:
                    // Highlight all legal destinations for this chit.
                    highlightMoves(critter);
                    break;

                case FIGHT:
                case STRIKEBACK:
                    // Leave carry mode.
                    clearAllCarries();

                    // Highlight all legal strikes for this chit.
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
                if (chitSelected)
                {
                    getCritter(0).moveToHex(hex);
                    chitSelected = false;
                }
                highlightMovableChits();
                break;

            case FIGHT:
            case STRIKEBACK:
                if (getCarryDamage() > 0)
                {
                    applyCarries(hex.getCritter());
                }
                else if (chitSelected)
                {
                    getCritter(0).strike(hex.getCritter());
                    chitSelected = false;
                }

                if (getCarryDamage() == 0)
                {
                    if (game != null)
                    {
                        Player player = game.getActivePlayer();
                        if (player.getOption(Game.autoForcedStrike))
                        {
                            makeForcedStrikes();
                        }
                    }
                    highlightChitsWithTargets();
                }
                break;

            default:
                break;
        }
    }


    public void actOnMisclick()
    {
        switch (getPhase())
        {
            case MOVE:
                chitSelected = false;
                highlightMovableChits();
                break;

            case FIGHT:
            case STRIKEBACK:
                chitSelected = false;
                highlightChitsWithTargets();
                break;

            default:
                break;
       }
    }


    public void cleanup()
    {
        disposeBattleDice();
        map.dispose();

        // Handle any after-battle angel summoning or recruiting.
        if (masterHex.getNumLegions() == 1)
        {
            Legion legion = masterHex.getLegion(0);
            if (legion == getAttacker())
            {
                // Summon angel
                if (legion.canSummonAngel())
                {
                    if (game != null)
                    {
                        game.createSummonAngel(attacker);
                    }
                }
            }
            else
            {
                // Recruit reinforcement
                if (legion.canRecruit() && attackerEntered)
                {
                    Creature recruit = PickRecruit.pickRecruit(
                        board.getFrame(), legion);
                    if (recruit != null && game != null)
                    {
                        game.doRecruit(recruit, legion, board.getFrame());
                    }
                }
            }

            // Make all creatures in the victorious legion visible.
            legion.revealAllCreatures();

            // Heal all creatures in the winning legion.
            legion.healAllCreatures();
        }

        if (game != null)
        {
            game.finishBattle();
        }
    }


    public static void main(String [] args)
    {
        Player player1 = new Player("Attacker", null);
        Player player2 = new Player("Defender", null);
        MasterHex hex = new MasterHex(0, 0, 0, false, null);
        hex.setTerrain('D');
        Legion attacker = new Legion("Bk01", null, hex, hex,
            Creature.archangel, Creature.troll, Creature.ranger,
            Creature.hydra, Creature.griffon, Creature.angel,
            Creature.warlock, null, player1);
        Legion defender = new Legion("Rd01", null, hex, hex,
            Creature.serpent, Creature.lion, Creature.gargoyle,
            Creature.cyclops, Creature.gorgon, Creature.guardian,
            Creature.minotaur, null, player2);
        attacker.setEntrySide(5);

        new Battle(null, attacker, defender, defender, hex, false, 1, MOVE);
    }
}

