import java.util.*;

/**
 * Class Battle holds data about a Titan battle.
 *
 * @version $Id$
 * @author David Ripton
 */

public class Battle 
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


    private BattleMap map;
    private Legion attacker;
    private Legion defender;
    private Legion activeLegion;
    private MasterBoard board;
    private MasterHex masterHex;
    private static BattleTurn turn;
    private SummonAngel summonAngel;
    private ShowDice showDice;

    private int turnNumber = 1;
    private int phase = MOVE;
    private int summonState = NO_KILLS;
    private boolean summoningAngel;
    private int carryDamage;
    private boolean chitSelected;
    private ArrayList critters = new ArrayList();
    private Critter lastCritterMoved;
    private Legion donor;

    private boolean attackerElim;
    private boolean defenderElim;


    public Battle(MasterBoard board, Legion attacker, Legion defender, 
        MasterHex masterHex)
    {
        this.board = board;
        this.masterHex = masterHex;
        this.defender = defender;
        this.attacker = attacker;
        activeLegion = defender;

        attacker.clearBattleTally();
        defender.clearBattleTally();

        map = new BattleMap(board, masterHex, this);

        turn = new BattleTurn(map, this);
        turn.setupMoveDialog();

        showDice = new ShowDice(map);
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
            Game.logEvent("Battle phase advances to " + 
                getPhaseName(phase));
            turn.setupMoveDialog();
        }
        
        else if (phase == RECRUIT)
        {
            phase = MOVE;
            Game.logEvent("Battle phase advances to " + 
                getPhaseName(phase));
            turn.setupMoveDialog();
        }

        else if (phase == MOVE)
        {
            phase = FIGHT;
            Game.logEvent("Battle phase advances to " + 
                getPhaseName(phase));
            turn.setupFightDialog();
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

            phase = STRIKEBACK;
            Game.logEvent("Battle phase advances to " + 
                getPhaseName(phase));
            turn.setupFightDialog();
        }

        else if (phase == STRIKEBACK)
        {
            removeDeadCreatures();
            checkForElimination();

            // Make sure the battle isn't over before continuing.
            if (masterHex.getNumLegions() == 2)
            {
                if (activeLegion == attacker)
                {
                    phase = SUMMON;
                    Game.logEvent(getActivePlayer().getName() + 
                        "'s battle turn, number " + turnNumber);
                    turn.setupSummonDialog();
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
                            attacker.removeLegion();
                            player.die(defender.getPlayer(), true);
                        }
                        else
                        {
                            attacker.removeLegion();
                        }
                        cleanup();
                    }
                    else
                    {
                        phase = RECRUIT;
                        turn.setupRecruitDialog();
                        Game.logEvent(getActivePlayer().getName() + 
                            "'s battle turn, number " + turnNumber);
                    }
                }
            }
        }
    }


    public BattleMap getBattleMap()
    {
        return map;
    }


    public SummonAngel getSummonAngel()
    {
        return summonAngel;
    }


    private void startSummoningAngel()
    {
        if (summonState == Battle.FIRST_BLOOD)
        {
            if (attacker.canSummonAngel())
            {
                summoningAngel = true;

                // Make sure the MasterBoard is visible.
                board.deiconify();
                board.show();

                summonAngel = new SummonAngel(board, attacker);
                board.getGame().setSummonAngel(summonAngel);
            }

            // This is the last chance to summon an angel until the
            // battle is over.
            summonState = Battle.TOO_LATE;
        }

        if (!summoningAngel)
        {
            if (phase == SUMMON)
            {
                advancePhase();
            }
        }
    }


    // This is called from MasterBoard after the SummonAngel finishes.
    public void finishSummoningAngel()
    {
        if (attacker.hasSummoned())
        {
            map.placeNewChit(attacker);
        }

        summoningAngel = false;
        summonAngel = null;

        if (phase == SUMMON)
        {
            advancePhase();
        }

        // Bring the BattleMap back to the front.
        map.show();
    }


    public void recruitReinforcement()
    {
        if (turnNumber == 4 && defender.canRecruit())
        {
            // Allow recruiting a reinforcement.
            new PickRecruit(map, defender);

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
        this.carryDamage = carryDamage;;
    }


    public boolean isChitSelected()
    {
        return chitSelected;
    }


    public void setChitSelected()
    {
        chitSelected = true;
    }
    
    
    public void clearChitSelected()
    {
        chitSelected = false;
    }
    

    public int getNumCritters()
    {
        return critters.size();
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
        TreeSet set = new TreeSet();

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


    private static Set findUnoccupiedTowerHexes()
    {
        TreeSet set = new TreeSet();

        BattleHex centerHex = BattleMap.getCenterTowerHex();

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
        Set set = new TreeSet();

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
        lastCritterMoved = critter;
    }


    public void clearLastCritterMoved()
    {
        lastCritterMoved = null;
    }


    public void undoLastMove()
    {
        clearChitSelected(); 

        if (lastCritterMoved != null)
        {
            lastCritterMoved.undoMove();
        }

        highlightMovableChits();
    }


    public void undoAllMoves()
    {
        clearChitSelected(); 

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


     // Mark all of the conceding player's critters as dead.
    private void concede(Player player)
    {
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
        new OptionDialog(map, "Confirm Concession",
            "Are you sure you want to concede the battle?",
            "Yes", "No");
        if (OptionDialog.getLastAnswer() == OptionDialog.YES_OPTION)
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
        TreeSet set = new TreeSet();
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


    // If any chits were left off-board, kill them.  If they were newly
    //   summoned or recruited, unsummon or unrecruit them instead.
    private void removeOffboardChits()
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
        clearLastCritterMoved();

        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            critter.commitMove();
        }
    }
    
    
    public void doneWithMoves()
    {
        removeOffboardChits();
        commitMoves();
        advancePhase();
    }


    public void applyDriftDamage()
    {
        // Drift hexes are only found on the tundra map.
        // Drift damage is applied only once per player turn,
        //    during the strike phase. 
        if (masterHex.getTerrain() == 't' && phase == FIGHT)
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
                }
            }
        }
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
        if (critters.indexOf(critter) != 0)
        {
            critters.remove(critter);
            critters.add(0, critter);
            critter.getChit().repaint();
        }
    }

    
    private void removeDeadCreatures()
    {
        // Initialize these to true, and then set them to false when a
        // non-dead chit is found.
        attackerElim = true;
        defenderElim = true;

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
                        donor = legion.getPlayer().getLastLegionSummonedFrom();
                        // Because addCreature grabs one from the stack, it
                        //     must be returned there.
                        critter.putOneBack();
                        donor.addCreature(critter);
                    }
                    else
                    {
                        critter.putOneBack();
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

                legion.removeCreature(critter);
                // If an angel or archangel was returned to its donor instead
                // of the stack, then the count must be adjusted.
                if (donor != null)
                {
                    critter.takeOne();
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
                defender.removeLegion();
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
                attacker.removeLegion();
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
            attacker.removeLegion();
            defender.removeLegion();
            cleanup();
        }

        // Check for single legion elimination.
        else if (attackerElim)
        {
            defender.addBattleTallyToPoints();
            attacker.removeLegion();
            cleanup();
        }
        else if (defenderElim)
        {
            attacker.addBattleTallyToPoints();
            defender.removeLegion();
            cleanup();
        }
    }


    private void commitStrikes()
    {
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            critter.commitStrike();
        }
    }

    /** Return the set of hex labels for hexes with critters that have
     *  valid strike targets. */
    public Set findChitsWithTargets()
    {
        Player player = getActivePlayer();
        TreeSet set = new TreeSet();

        for (int i = 0; i < getNumCritters(); i++)
        {
            Critter critter = getCritter(i);
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
                if (critter.isInContact(false) && !critter.hasStruck())
                {
                    return true;
                }
            }
        }

        return false;
    }


    public void doneWithStrikes()
    {
        // Advance only if there are no unresolved strikes.
        if (isForcedStrikeRemaining())
        {
            highlightChitsWithTargets();
            new MessageBox(map, "Engaged creatures must strike.");
        }
        else
        {
            commitStrikes();
            advancePhase();
        }
    }


    /** Return a set of hex labels for hexes containing targets that the 
     *  critter may strike.
     */
    private Set findStrikes(Critter critter)
    {
        TreeSet set = new TreeSet(); 

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
            for (int i = 0; i < getNumCritters(); i++)
            {
                Critter target = getCritter(i);
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
    public Set findCarries()
    {
        TreeSet set = new TreeSet();
        
        for (int i = 0; i < getNumCritters(); i++)
        {
            Critter target = getCritter(i);
            if (target.getCarryFlag())
            {
                set.add(target.getCurrentHex().getLabel());
            }
        }

        return set;
    }


    public int highlightCarries()
    {
        Set set = findCarries();
        map.unselectAllHexes();
        map.selectHexesByLabels(set);
        return set.size();
    }


    public void applyCarries(Critter target)
    {
        carryDamage = target.wound(carryDamage);
        if (carryDamage < 0)
        {
            clearAllCarries();
        }
        else
        {
            target.setCarryFlag(false);
            String label = target.getCurrentHex().getLabel();
            map.unselectHexByLabel(label);
            showDice.setCarries(carryDamage);
            showDice.setup();
        }
    }


    /** Return the range in hexes from hex1 to hex2.  Titan ranges are
     *  inclusive at both ends. */
    public int getRange(BattleHex hex1, BattleHex hex2)
    {
        int x1 = hex1.getXCoord();
        float y1 = hex1.getYCoord();
        int x2 = hex2.getXCoord();
        float y2 = hex2.getYCoord();

        // Hexes with odd X coordinates are pushed down half a hex.
        if ((x1 & 1) == 1)
        {
            y1 += 0.5;
        }
        if ((x2 & 1) == 1)
        {
            y2 += 0.5;
        }

        float xDist = Math.abs(x2 - x1);
        float yDist = Math.abs(y2 - y1);

        // Offboard creatures are out of range.
        if (x1 == -1 || x2 == -1)
        {
            xDist = 10;
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
    private boolean toLeft(float xDist, float yDist)
    {
        float ratio = xDist / yDist;
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
        float y1 = hex1.getYCoord();
        int x2 = hex2.getXCoord();
        float y2 = hex2.getYCoord();

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

        float xDist = x2 - x1;
        float yDist = y2 - y1;

        // Chits below the level of the strike do not block LOS.
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

        boolean clear = true;

        int range = getRange(currentHex, targetHex);
        int skill = critter.getSkill();

        if (range > skill)
        {
            clear = false;
        }

        // Only warlocks can rangestrike at range 2, rangestrike Lords,
        // or rangestrike without LOS.
        else if (!critter.getName().equals("Warlock") && (range < 3 ||
            target.isLord() || isLOSBlocked(currentHex, targetHex)))
        {
            clear = false;
        }

        return clear;
    }


    /** Return the hexside direction of the path from hex1 to hex2.
     *  Sometimes two directions are possible.  If the left parameter
     *  is set, the direction further left will be given.  Otherwise,
     *  the direction further right will be given. */
    public int getDirection(BattleHex hex1, BattleHex hex2, boolean left)
    {
        if (hex1 == hex2)
        {
            return -1;
        }

        int x1 = hex1.getXCoord();
        float y1 = hex1.getYCoord();
        int x2 = hex2.getXCoord();
        float y2 = hex2.getYCoord();

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

        float xDist = x2 - x1;
        float yDist = y2 - y1;


        if (xDist >= 0)
        {
            if (yDist > 1.5 * xDist)
            {
                return 3;
            }
            if (yDist == 1.5 * xDist)
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
            if (yDist < -1.5 * xDist)
            {
                return 0;
            }
            if (yDist == -1.5 * xDist)
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
            if (yDist > 0)
            {
                return 2;
            }
            if (yDist < 0)
            {
                return 1;
            }
            if (yDist == 0)
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

        if (xDist < 0)
        {
            if (yDist < 1.5 * xDist)
            {
                return 0;
            }
            if (yDist == 1.5 * xDist)
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
            if (yDist > -1.5 * xDist)
            {
                return 3;
            }
            if (yDist == -1.5 * xDist)
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
            if (yDist > 0)
            {
                return 4;
            }
            if (yDist < 0)
            {
                return 5;
            }
            if (yDist == 0)
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

        // Shouldn't be reached.
        return -1;
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
            return 10;
        }

        int direction = getDirection(hex1, hex2, left);

        BattleHex nextHex = hex1.getNeighbor(direction);
        if (nextHex == null)
        {
            return 10;
        }

        if (nextHex == hex2)
        {
            // Success!
            return count;
        }

        // Trees block LOS.
        if (nextHex.getTerrain() == 't')
        {
            return 10;
        }

        // All chits block LOS.  (There are no height differences on maps
        //    with bramble.)
        if (nextHex.isOccupied())
        {
            return 10;
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
        float y1 = hex1.getYCoord();
        int x2 = hex2.getXCoord();
        float y2 = hex2.getYCoord();

        // Offboard hexes are not allowed.
        if (x1 == -1 || x2 == -1)
        {
            return 10;
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

        float xDist = x2 - x1;
        float yDist = y2 - y1;

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


    public BattleTurn getTurn()
    {
        return turn;
    }


    public ShowDice getShowDice()
    {
        return showDice;
    }


    public void actOnCritter(Critter critter)
    {
        // Only the active player can move or strike.
        if (critter != null && critter.getPlayer() == getActivePlayer())
        {
            setChitSelected(); 

            // Put selected chit at the top of the Z-order.
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
                if (isChitSelected())
                {
                    getCritter(0).moveToHex(hex);
                    clearChitSelected();
                }
                highlightMovableChits();
                break;

            case FIGHT:
            case STRIKEBACK:
                if (getCarryDamage() > 0)
                {
                    applyCarries(hex.getCritter());
                }
                else if (isChitSelected())
                {
                    getCritter(0).strike(hex.getCritter());
                    clearChitSelected();
                }

                if (getCarryDamage() == 0)
                {
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
                highlightMovableChits();
                break;
    
            case FIGHT:
            case STRIKEBACK:
                highlightChitsWithTargets();
                break;
    
            default:
                break;
       }
    }


    public void cleanup()
    {
        // Handle any after-battle angel summoning or recruiting.
        if (masterHex.getNumLegions() == 1)
        {
            Legion legion = masterHex.getLegion(0);
            if (legion == getAttacker())
            {
                // Summon angel
                if (legion.canSummonAngel())
                {
                    if (board != null)
                    {
                        // Make sure the MasterBoard is visible.
                        board.deiconify();
                        // And bring it to the front.
                        board.show();
    
                        SummonAngel summonAngel = new SummonAngel(board,
                            getAttacker());
                        board.getGame().setSummonAngel(summonAngel);
                    }
                }
            }
            else
            {
                // Recruit reinforcement
                if (legion.canRecruit())
                {
                    new PickRecruit(map, legion);
                }
            }

            // Make all creatures in the victorious legion visible.
            legion.revealAllCreatures();

            // Heal all creatures in the winning legion.
            legion.healAllCreatures();
        }

        if (turn != null)
        {
            turn.cleanup();
        }

        map.dispose();

        board.getGame().finishBattle();
    }


    public static void main(String [] args)
    {
        Player player1 = new Player("Attacker", null);
        Player player2 = new Player("Defender", null);
        MasterHex hex = new MasterHex(0, 0, 0, false, null);
        hex.setTerrain('J');
        hex.setEntrySide(3);
        Legion attacker = new Legion(60, "Bk01", null, null, 7,
            hex, Creature.archangel, Creature.troll, Creature.ranger,
            Creature.hydra, Creature.griffon, Creature.angel,
            Creature.warlock, null, player1);
        Legion defender = new Legion(60, "Rd01", null, null, 7,
            hex, Creature.serpent, Creature.lion, Creature.gargoyle,
            Creature.cyclops, Creature.gorgon, Creature.guardian,
            Creature.minotaur, null, player2);

        new Battle(null, attacker, defender, hex);
    }
}

