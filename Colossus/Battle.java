import java.awt.*;
import java.awt.event.*;

/**
 * Class Battle holds data about a Titan battle.
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
    private BattleTurn turn;
    private SummonAngel summonAngel;

    private int turnNumber = 1;
    private int phase = MOVE;
    private int summonState = NO_KILLS;
    private boolean summoningAngel = false;
    private int carryDamage = 0;
    private boolean chitSelected = false;
    private int numCritters = 0;
    private Critter [] critters = new Critter[14];
    private Critter lastCritterMoved;
    private Legion donor;



    public Battle(MasterBoard board, Legion attacker, Legion defender, 
        MasterHex masterHex)
    {
        this.board = board;
        this.attacker = attacker;
        this.defender = defender;
        activeLegion = defender;
        this.masterHex = masterHex;

        attacker.clearBattleTally();
        defender.clearBattleTally();

        map = new BattleMap(board, masterHex, this);

        turn = map.getTurn();
        turn.setupMoveDialog();
    }


    public static String getPhaseName(int phase)
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


    public Legion getActiveLegion()
    {
        return activeLegion;
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
        try
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
    
                // Make sure the battle isn't over before continuing.
                if (attacker.getHeight() >= 1 && defender.getHeight() >= 1)
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
                            map.cleanup();
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
        catch (NullPointerException e)
        {
            e.printStackTrace();
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


    public void startSummoningAngel()
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
                board.setSummonAngel(summonAngel);
            }

            // This is the last chance to summon an angel until the
            // battle is over.
            summonState = Battle.TOO_LATE;
        }

        if (!summoningAngel)
        {
            if (phase == Battle.SUMMON)
            {
                advancePhase();
            }
        }
    }


    // This is called from MasterBoard after the SummonAngel finishes.
    public void finishSummoningAngel()
    {
        if (attacker.summoned())
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


    public void setCarryDamage(int damage)
    {
        carryDamage = damage;
    }


    public int getCarryDamage()
    {
        return carryDamage;
    }


    public boolean chitSelected()
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
        return numCritters;
    }


    public Critter getCritter(int i)
    {
        return critters[i];
    }
    

    public void addCritter(Critter critter)
    {
        critters[numCritters++] = critter;
    }


    public void removeCritter(Critter critter)
    {
        for (int i = 0; i < numCritters; i++)
        {
            if (critters[i] == critter)
            {
                for (int j = i; j < numCritters - 1; j++)
                {
                    critters[j] = critters[j + 1];
                }
                critters[--numCritters] = null;
            }
        }
    }


    // Recursively find moves from this hex.  Select all legal destinations.
    //    Do not double back.  Return the number of moves found.
    private void findMoves(BattleHex hex, Creature creature, boolean flies, 
        int movesLeft, int cameFrom)
    {
        for (int i = 0; i < 6; i++)
        {
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
                        neighbor.select();
                        neighbor.repaint();

                        // If there are movement points remaining, continue
                        // checking moves from there.  Fliers skip this
                        // because flying is more efficient.
                        if (!flies && movesLeft > entryCost)
                        {
                            findMoves(neighbor, creature, flies,
                                movesLeft - entryCost, reverseDir);
                        }
                    }

                    // Fliers can fly over any non-volcano hex for 1 movement
                    // point.  Only dragons can fly over volcanos.
                    if (flies && movesLeft > 1 && (neighbor.getTerrain() != 'v'
                        || creature.getName().equals("Dragon")))
                    {
                        findMoves(neighbor, creature, flies, movesLeft - 1,
                            reverseDir);
                    }
                }
            }
        }
    }


    // Find all legal moves for this critter.
    public void showMoves(Critter critter)
    {
        map.unselectAllHexes();

        if (!critter.hasMoved() && !critter.inContact(false))
        {
            if (masterHex.getTerrain() == 'T' && getTurnNumber() == 1 &&
                getActivePlayer() == getDefender().getPlayer())
            {
                map.highlightUnoccupiedTowerHexes();
            }
            else
            {
                findMoves(critter.getCurrentHex(), critter,
                    critter.flies(), critter.getSkill(), -1);
            }
        }
    }


    public void markLastCritterMoved(Critter critter)
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
    }


    public void undoAllMoves()
    {
        clearChitSelected(); 

        for (int i = 0; i < numCritters; i++)
        {
            Critter critter = critters[i];
            if (critter.hasMoved())
            {
                critter.undoMove();
            }
        }
    }


     // Mark all of the conceding player's critters as dead.
    public void concede(Player player)
    {
        for (int i = 0; i < numCritters; i++)
        {
            Critter critter = critters[i];
            if (critter.getPlayer() == player)
            {
                critter.setDead(true);
            }
        }
    }


    // If any chits were left off-board, kill them.  If they were newly
    //   summoned or recruited, unsummon or unrecruit them instead.
    public void removeOffboardChits()
    {
        Player player = getActivePlayer();
        for (int i = 0; i < numCritters; i++)
        {
            Critter critter = critters[i];
            if (critter.getCurrentHex().isEntrance() &&
                critter.getPlayer() == player)
            {
                critter.setDead(true);
            }
        }
    }


    public int highlightMovableChits()
    {
        map.unselectAllHexes();

        int count = 0;

        Player player = getActivePlayer();

        for (int i = 0; i < getNumCritters(); i++)
        {
            Critter critter = getCritter(i);
            if (critter.getPlayer() == player)
            {
                if (!critter.hasMoved() && !critter.inContact(false))
                {
                    count++;
                    BattleHex hex = critter.getCurrentHex();
                    hex.select();
                    hex.repaint();
                }
            }
        }

        return count;
    }


    public void commitMoves()
    {
        clearLastCritterMoved();

        for (int i = 0; i < numCritters; i++)
        {
            Critter critter = critters[i];
            critter.commitMove();
        }
    }


    public void applyDriftDamage()
    {
        // Drift hexes are only found on the tundra map.
        if (masterHex.getTerrain() == 't')
        {
            for (int i = 0; i < numCritters; i++)
            {
                Critter critter = critters[i];
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
        for (int i = 0; i < numCritters; i++)
        {
            Critter critter = critters[i];
            if (critter.getCarryFlag())
            {
                critter.setCarryFlag(false);
                critter.getCurrentHex().unselect();
                critter.getCurrentHex().repaint();
            }
        }
        setCarryDamage(0);
    }


    public boolean forcedStrikesRemain()
    {
        Player player = getActivePlayer();

        for (int i = 0; i < numCritters; i++)
        {
            Critter critter = critters[i];
            if (critter.getPlayer() == player)
            {
                if (critter.inContact(false) && !critter.hasStruck())
                {
                    return true;
                }
            }
        }

        return false;
    }


    // Move the passed Critter to the top of the critters array.
    // Shift the other critters up. 
    public void moveToTop(Critter critter)
    {
        int i = 0;
        while (critters[i] != critter)
        {
            i++;
        }

        if (i != 0)
        {
            for (int j = i; j > 0; j--)
            {
                critters[j] = critters[j - 1];
            }
            critters[0] = critter;
            critter.getChit().repaint();
        }
    }


    // Return the Critter whose chit contains the given point,
    //   or null if none does.
    public Critter getCritterContainingPoint(Point point)
    {
        for (int i = 0; i < numCritters; i++)
        {
            if (critters[i].getChit().contains(point)) 
            {
                return critters[i];
            }
        }

        return null;
    }
    
    
    public void removeDeadCreatures()
    {
        // Initialize these to true, and then set them to false when a
        // non-dead chit is found.
        boolean attackerElim = true;
        boolean defenderElim = true;

        for (int i = numCritters - 1; i >= 0; i--)
        {
            Critter critter = critters[i];
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

                removeCritter(critter);
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

        // Check for mutual Titan elimination.
        if (attacker.getPlayer().isTitanEliminated() &&
            defender.getPlayer().isTitanEliminated())
        {
            // Nobody gets any points.
            // Make defender die first, to simplify turn advancing.
            defender.getPlayer().die(null, false);
            attacker.getPlayer().die(null, true);
            map.cleanup();
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
            map.cleanup();
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
            map.cleanup();
        }

        // Check for mutual legion elimination.
        else if (attackerElim && defenderElim)
        {
            attacker.removeLegion();
            defender.removeLegion();
            map.cleanup();
        }

        // Check for single legion elimination.
        else if (attackerElim)
        {
            defender.addBattleTallyToPoints();
            attacker.removeLegion();
            map.cleanup();
        }
        else if (defenderElim)
        {
            attacker.addBattleTallyToPoints();
            defender.removeLegion();
            map.cleanup();
        }
    }


    public void commitStrikes()
    {
        for (int i = 0; i < numCritters; i++)
        {
            critters[i].commitStrike();
        }
    }


    public int highlightChitsWithTargets()
    {
        map.unselectAllHexes();

        int count = 0;
        Player player = getActivePlayer();

        for (int i = 0; i < getNumCritters(); i++)
        {
            Critter critter = getCritter(i);
            if (critter.getPlayer() == player)
            {
                if (countStrikes(critter) > 0)
                {
                    count++;
                    BattleHex hex = critter.getCurrentHex();
                    hex.select();
                    hex.repaint();
                }
            }
        }

        return count;
    }


    // Count the number of targets that a creature may strike.  If highlight
    //     is true, also select their hexes.
    private int countAndMaybeHighlightStrikes(Critter critter, boolean
        highlight)
    {
        int count = 0;

        if (highlight)
        {
            map.unselectAllHexes();
        }

        // Each creature may strike only once per turn.
        if (critter.hasStruck())
        {
            return 0;
        }

        Player player = critter.getPlayer();
        BattleHex currentHex = critter.getCurrentHex();

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
                    if (target.getPlayer() != player && !target.isDead())
                    {
                        if (highlight)
                        {
                            targetHex.select();
                            targetHex.repaint();
                        }
                        count++;
                    }
                }
            }
        }

        // Then do rangestrikes if applicable.  Rangestrikes are not allowed
        // if the creature can strike normally.
        if (!critter.inContact(true) && critter.rangeStrikes() &&
            getPhase() != Battle.STRIKEBACK)
        {
            for (int i = 0; i < getNumCritters(); i++)
            {
                Critter target = getCritter(i);
                if (target.getPlayer() != player && !target.isDead())
                {
                    BattleHex targetHex = target.getCurrentHex();

                    // Can't rangestrike if it can be struck normally.
                    if (!targetHex.isSelected())
                    {
                        if (rangestrikePossible(critter, target))
                        {
                            if (highlight)
                            {
                                targetHex.select();
                                targetHex.repaint();
                            }
                            count++;
                        }
                    }
                }
            }
        }

        return count;
    }


    public int countStrikes(Critter critter)
    {
        return countAndMaybeHighlightStrikes(critter, false);
    }


    public int highlightStrikes(Critter critter)
    {
        return countAndMaybeHighlightStrikes(critter, true);
    }


    public int highlightCarries(int damage)
    {
        map.unselectAllHexes();

        int count = 0;

        for (int i = 0; i < getNumCritters(); i++)
        {
            Critter target = getCritter(i);
            if (target.getCarryFlag())
            {
                BattleHex targetHex = target.getCurrentHex();
                targetHex.select();
                targetHex.repaint();
                count++;
            }
        }

        if (count > 0)
        {
            setCarryDamage(damage);
        }

        return count;
    }


    public void applyCarries(Critter target)
    {
        setCarryDamage(target.wound(getCarryDamage()));
        if (getCarryDamage() < 0)
        {
            clearAllCarries();
        }
        else
        {
            target.setCarryFlag(false);
            target.getCurrentHex().unselect();
        }
    }


    // Returns the range in hexes from hex1 to hex2.  Titan ranges are
    // inclusive at both ends.
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


    // Know that yDist != 0
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


    // Check LOS, going to the left of hexspines if argument left is true, or
    // to the right if it is false.
    private boolean LOSBlockedDir(BattleHex initialHex, BattleHex currentHex,
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

        return LOSBlockedDir(initialHex, nextHex, finalHex, left,
            strikeElevation, strikerAtop, strikerAtopCliff,
            midObstacle, midCliff, midChit, totalObstacles);
    }


    // Check to see if the LOS from hex1 to hex2 is blocked.  If the LOS
    // lies along a hexspine, check both and return true only if both are
    // blocked.
    public boolean LOSBlocked(BattleHex hex1, BattleHex hex2)
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
            return (LOSBlockedDir(hex1, hex1, hex2, true, strikeElevation,
                false, false, false, false, false, 0) &&
                LOSBlockedDir(hex1, hex1, hex2, false, strikeElevation,
                false, false, false, false, false, 0));
        }
        else
        {
            return LOSBlockedDir(hex1, hex1, hex2, toLeft(xDist, yDist),
                strikeElevation, false, false, false, false, false, 0);
        }
    }


    // Return true if the rangestrike is possible.
    public boolean rangestrikePossible(Critter critter, Critter target)
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
            target.isLord() || LOSBlocked(currentHex, targetHex)))
        {
            clear = false;
        }

        return clear;
    }


    // Returns the hexside direction of the path from hex1 to hex2.
    // Sometimes two directions are possible.  If the left parameter
    // is set, the direction further left will be given.  Otherwise,
    // the direction further right will be given.
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

    // Return the number of intervening bramble hexes.  If LOS is along a
    // hexspine, go left if argument left is true, right otherwise.  If
    // LOS is blocked, return a large number.
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


    public MasterBoard getBoard()
    {
        return board;
    }


    public BattleTurn getTurn()
    {
        return turn;
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

