import java.awt.*;
import java.awt.event.*;

/**
 * Class BattleMap implements the GUI for a Titan battlemap.
 * @version $Id$
 * @author David Ripton
 */

public class BattleMap extends Frame implements MouseListener,
    MouseMotionListener, WindowListener
{
    private Hex[][] h = new Hex[6][6];

    // ne, e, se, sw, w, nw
    private Hex [] entrances = new Hex[6];

    private int numChits;
    private BattleChit[] chits = new BattleChit[14];
    private BattleChit lastChitMoved;

    private static final boolean[][] show =
    {
        {false,false,true,true,true,false},
        {false,true,true,true,true,false},
        {false,true,true,true,true,true},
        {true,true,true,true,true,true},
        {false,true,true,true,true,true},
        {false,true,true,true,true,false}
    };
    private Rectangle rectClip = new Rectangle();
    private Image offImage;
    private Graphics gBack;
    private Dimension offDimension;
    private MediaTracker tracker;
    private boolean imagesLoaded = false;
    private boolean eraseFlag = false;

    private static int scale = 30;
    private static int chitScale = 2 * scale;
    private Dimension preferredSize;

    private Legion attacker;
    private Legion defender;

    // l = left (5), r = right (1), b = bottom (3)
    private char side;

    // B,D,H,J,m,M,P,S,T,t,W
    // Brush, Desert, Hills, Jungle, mountains, Marsh, Plains,
    // Swamp, Tower, tundra, Woods
    private char terrain;
    private BattleTurn turn;

    private int attackerPoints = 0;
    private int defenderPoints = 0;

    private boolean summonedAngel = false;
    private boolean recruitedReinforcement = false;


    public BattleMap(Legion attacker, Legion defender, char terrain, char side)
    {
        super(attacker.getMarkerId() + " attacks " + defender.getMarkerId());

        this.attacker = attacker;
        this.defender = defender;
        this.terrain = terrain;

        // All tower attacks come from the bottom side.
        if (terrain == 'T')
        {
            this.side = 'b';
        }
        else
        {
            this.side = side;
        }

        preferredSize = new Dimension(30 * scale, 30 * scale);
        setSize(preferredSize);
        setResizable(false);

        setBackground(java.awt.Color.white);
        addWindowListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        pack();
        validate();

        // Initialize the hexmap.
        setupHexes();

        tracker = new MediaTracker(this);

        int attackerHeight = attacker.getHeight();
        numChits = attackerHeight + defender.getHeight();

        Hex entrance = getAttackerEntrance();
        for (int i = 0; i < attackerHeight; i++)
        {
            chits[i] = new BattleChit(0, 0, chitScale,
                attacker.getCreature(i).getImageName(), this,
                attacker.getCreature(i), entrance,
                attacker, false, this);
            tracker.addImage(chits[i].getImage(), 0);
            entrance.addChit(chits[i]);
        }
        entrance.alignChits();

        entrance = getDefenderEntrance();
        for (int i = attackerHeight; i < numChits; i++)
        {
            chits[i] = new BattleChit(0, 0, chitScale,
                defender.getCreature(i - attackerHeight).getImageName(), this,
                defender.getCreature(i - attackerHeight), entrance,
                defender, true, this);
            tracker.addImage(chits[i].getImage(), 0);
            entrance.addChit(chits[i]);
        }
        entrance.alignChits();


        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            new MessageBox(this, "waitForAll was interrupted");
        }
        imagesLoaded = true;

        turn = new BattleTurn(this, this, attacker, defender);

        setVisible(true);
        repaint();
    }


    void unselectAllHexes()
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j] && h[i][j].isSelected())
                {
                    h[i][j].unselect();
                    h[i][j].repaint();
                }
            }
        }
    }


    int highlightMovableChits()
    {
        unselectAllHexes();

        int count = 0;

        Player player;
        // This gets called from BattleTurn's constructor, so it cannot
        // be assumed that turn is valid.
        if (turn == null)
        {
            player = defender.getPlayer();
        }
        else
        {
            player = turn.getActivePlayer();
        }

        for (int i = 0; i < numChits; i++)
        {
            BattleChit chit = chits[i];
            if (chit.getPlayer() == player)
            {
                if (!chit.hasMoved() && !chit.inContact(false))
                {
                    count++;
                    Hex hex = chit.getCurrentHex();
                    hex.select();
                    hex.repaint();
                }
            }
        }

        return count;
    }


    // Recursively find moves from this hex.  Select all legal destinations.
    //    Do not double back.  Return the number of moves found.
    private void findMoves(Hex hex, BattleChit chit, Creature creature,
        boolean flies, int movesLeft, int cameFrom)
    {
        for (int i = 0; i < 6; i++)
        {
            if (i != cameFrom)
            {
                Hex neighbor = hex.getNeighbor(i);
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
                        if (flies == false && movesLeft > entryCost)
                        {
                            findMoves(neighbor, chit, creature, flies,
                                movesLeft - entryCost, reverseDir);
                        }
                    }

                    // Fliers can fly over any non-volcano hex for 1 movement
                    // point.  Only dragons can fly over volcanos.
                    if (flies && movesLeft > 1 && (neighbor.getTerrain() != 'v'
                        || creature == Creature.dragon))
                    {
                        findMoves(neighbor, chit, creature, flies,
                            movesLeft - 1, reverseDir);
                    }
                }
            }
        }
    }


    // Find all legal moves for this chit.
    void showMoves(BattleChit chit)
    {
        unselectAllHexes();

        if (!chit.hasMoved() && !chit.inContact(false))
        {
            Creature creature = chit.getCreature();

            if (terrain == 'T' && turn.getTurnNumber() == 1 &&
                turn.getActivePlayer() == defender.getPlayer())
            {
                // Mark all unoccupied tower hexes.
                if (!h[3][1].isOccupied())
                {
                    h[3][1].select();
                    h[3][1].repaint();
                }
                for (int i = 2; i <= 4; i++)
                {
                    for (int j = 2; j <= 3; j++)
                    {
                        if (!h[i][j].isOccupied())
                        {
                            h[i][j].select();
                            h[i][j].repaint();
                        }
                    }
                }
            }
            else
            {
                findMoves(chit.getCurrentHex(), chit, creature, 
                    creature.flies(), creature.getSkill(), -1);
            }
        }
    }


    void markLastChitMoved(BattleChit chit)
    {
        lastChitMoved = chit;
    }


    void clearLastChitMoved()
    {
        lastChitMoved = null;
    }


    void undoLastMove()
    {
        for (int i = 0; i < numChits; i++)
        {
            if (chits[i] == lastChitMoved)
            {
                chits[i].undoMove();
            }
        }
    }


    void undoAllMoves()
    {
        for (int i = 0; i < numChits; i++)
        {
            if (chits[i].hasMoved())
            {
                chits[i].undoMove();
            }
        }
    }


    // If any chits were left off-board, kill them.
    // XXX: If they were newly summoned, unsummon them instead.
    void removeOffboardChits()
    {
        Player player = turn.getActivePlayer();
        for (int i = 0; i < numChits; i++)
        {
            if (chits[i].getCurrentHex().getXCoord() == -1 &&
                chits[i].getPlayer() == player)
            {
                chits[i].setDead(true);
            }
        }
    }


    void commitMoves()
    {
        clearLastChitMoved();

        for (int i = 0; i < numChits; i++)
        {
            chits[i].commitMove();
        }
    }


    int highlightChitsWithTargets()
    {
        unselectAllHexes();

        int count = 0;
        Player player = turn.getActivePlayer();

        for (int i = 0; i < numChits; i++)
        {
            BattleChit chit = chits[i];
            if (chit.getPlayer() == player)
            {
                if (countStrikes(chit) > 0)
                {
                    count++;
                    Hex hex = chit.getCurrentHex();
                    hex.select();
                    hex.repaint();
                }
            }
        }

        return count;
    }


    // Count the number of targets that chit may strike.  If highlight
    //     is true, select their hexes.
    private int countAndMaybeHighlightStrikes(BattleChit chit, boolean
        highlight)
    {
        int count = 0;

        if (highlight)
        {
            unselectAllHexes();
        }

        // Each chit may strike only once per turn.
        if (chit.hasStruck())
        {
            return 0;
        }

        Player player = chit.getPlayer();
        Hex currentHex = chit.getCurrentHex();

        // First mark and count normal strikes.
        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not engaged.
            if (currentHex.getHexside(i) != 'c')
            {
                Hex hex = currentHex.getNeighbor(i);
                if (hex != null)
                {
                    if (hex.isOccupied())
                    {
                        BattleChit bogie = hex.getChit();
                        if (bogie.getPlayer() != player && !bogie.isDead())
                        {
                            if (highlight)
                            {
                                hex.select();
                                hex.repaint();
                            }
                            count++;
                        }
                    }
                }
            }
        }

        // Then do rangestrikes if applicable.  Rangestrikes are not allowed
        // if the creature can strike normally.
        Creature creature = chit.getCreature();
        if (count == 0 && creature.rangeStrikes() && turn.getPhase() !=
            turn.STRIKEBACK)
        {
            int skill = creature.getSkill();

            for (int i = 0; i < numChits; i++)
            {
                BattleChit bogie = chits[i];
                if (bogie.getPlayer() != player && !bogie.isDead())
                {
                    Hex hex = bogie.getCurrentHex();

                    // Can't rangestrike if it can be struck normally.
                    if (!hex.isSelected())
                    {
                        if (rangestrikePossible(chit, bogie))
                        {
                            if (highlight)
                            {
                                hex.select();
                                hex.repaint();
                            }
                            count++;
                        }
                    }
                }
            }
        }

        return count;
    }


    int countStrikes(BattleChit chit)
    {
        return countAndMaybeHighlightStrikes(chit, false);
    }


    int highlightStrikes(BattleChit chit)
    {
        return countAndMaybeHighlightStrikes(chit, true);
    }


    boolean forcedStrikesRemain()
    {
        Player player = turn.getActivePlayer();

        for (int i = 0; i < numChits; i++)
        {
            BattleChit chit = chits[i];
            if (chit.getPlayer() == player)
            {
                if (chit.inContact(false) && !chit.hasStruck())
                {
                    return true;
                }
            }
        }

        return false;
    }


    // Returns the range in hexes from hex1 to hex2.  Titan ranges are
    // inclusive at both ends.
    int getRange(Hex hex1, Hex hex2)
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
        
        // Offboard chits are out of range.
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

    
    // Check LOS, going to the left of hexspines if argument left is true, or
    // to the right if it is false.
    private boolean LOSBlockedDir(Hex initialHex, Hex currentHex, Hex finalHex, 
        boolean left, int strikeElevation, boolean strikerAtop, boolean
        strikerAtopCliff, boolean midObstacle, boolean midCliff, boolean 
        midChit, int totalObstacles)
    {
        boolean targetAtop = false;
        boolean targetAtopCliff = false;

        if (currentHex == finalHex)
        {
            return false;
        }
        
        int x1 = currentHex.getXCoord();
        float y1 = currentHex.getYCoord();
        int x2 = finalHex.getXCoord();
        float y2 = finalHex.getYCoord();

        // Offboard hexes are not allowed.
        if (x1 == -1 || x2 == -1)
        {
            return true;
        }
        
        int direction = getDirection(currentHex, finalHex, left);

        Hex nextHex = currentHex.getNeighbor(direction);

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
    boolean LOSBlocked(Hex hex1, Hex hex2)
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
        else if (xDist / yDist > 0)
        {
            // LOS to left
            return LOSBlockedDir(hex1, hex1, hex2, true, strikeElevation,
                false, false, false, false, false, 0);
        }
        else
        {
            // LOS to right
            return LOSBlockedDir(hex1, hex1, hex2, false, strikeElevation,
                false, false, false, false, false, 0);
        }
    }


    // Return true if the rangestrike is possible.
    boolean rangestrikePossible(BattleChit chit, BattleChit target)
    {
        Hex currentHex = chit.getCurrentHex();
        Hex targetHex = target.getCurrentHex();
        Creature creature = chit.getCreature(); 

        boolean clear = true;

        int range = getRange(currentHex, targetHex);
        int skill = creature.getSkill();

        if (range > skill)
        {
            clear = false;
        }

        // Only warlocks can rangestrike at range 2, rangestrike Lords,
        // or rangestrike without LOS.
        else if (creature != Creature.warlock && (range < 3 ||
            target.getCreature().isLord() || 
            LOSBlocked(currentHex, targetHex)))
        {
            clear = false;
        }

        return clear;
    }


    // Returns the hexside direction of the path from hex1 to hex2.
    // Sometimes two directions are possible.  If the left parameter
    // is set, the direction further left will be given.  Otherwise,
    // the direction further right will be given.
    int getDirection(Hex hex1, Hex hex2, boolean left)
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
    private int countBrambleHexesDir(Hex hex1, Hex hex2,
        boolean left, int previousCount)
    {
        int count = previousCount;

        int x1 = hex1.getXCoord();
        float y1 = hex1.getYCoord();
        int x2 = hex2.getXCoord();
        float y2 = hex2.getYCoord();

        // Offboard hexes are not allowed.
        if (x1 == -1 || x2 == -1)
        {
            return 10;
        }
        
        int direction = getDirection(hex1, hex2, left);

        Hex nextHex = hex1.getNeighbor(direction);
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
    int countBrambleHexes(Hex hex1, Hex hex2)
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


    void commitStrikes()
    {
        for (int i = 0; i < numChits; i++)
        {
            chits[i].commitStrike();
        }
    }


    void removeDeadChits()
    {
        boolean attackerElim = true;
        boolean defenderElim = true;

        for (int i = numChits - 1; i >= 0; i--)
        {
            if (chits[i].isDead())
            {
                Legion legion = chits[i].getLegion();
                Creature creature = chits[i].getCreature();
                if (legion == attacker)
                {
                    defenderPoints += creature.getPointValue();
                }
                else
                {
                    attackerPoints += creature.getPointValue();
                }

                // XXX: Need to remove the exact chit?
                legion.removeCreature(creature);

                if (creature == Creature.titan)
                {
                    legion.getPlayer().eliminateTitan();
                }

                Hex hex = chits[i].getCurrentHex();
                hex.removeChit(chits[i]);
                hex.repaint();

                for (int j = i; j < numChits - 1; j++)
                {
                    chits[j] = chits[j + 1];
                }
                chits[numChits - 1] = null;
                numChits--;
            }
            else
            {
                if (chits[i].getLegion() == attacker)
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
            attacker.getPlayer().die(null);
            defender.getPlayer().die(null);
            turn.dispose();
            dispose();
        }

        // Check for single Titan elimination.  Victor gets full points
        // for eliminated chits, and half points for what's left, except for 
        // legions engaged with other players, who then get the half points.
        else if (attacker.getPlayer().isTitanEliminated())
        {
            if (!defenderElim)
            {
                defender.addPoints(defenderPoints);
            }
            attacker.getPlayer().die(defender.getPlayer());
            turn.dispose();
            dispose();
        }
        else if (defender.getPlayer().isTitanEliminated())
        {
            if (!attackerElim)
            {
                attacker.addPoints(defenderPoints);
            }
            defender.getPlayer().die(attacker.getPlayer());
            turn.dispose();
            dispose();
        }

        // Check for mutual legion elimination.
        else if (attackerElim && defenderElim)
        {
            attacker.removeLegion();
            defender.removeLegion();
            turn.dispose();
            dispose();
        }

        // Check for single legion elimination.
        else if (attackerElim)
        {
            defender.addPoints(defenderPoints);
            attacker.removeLegion();
            turn.dispose();
            dispose();
        }
        else if (defenderElim)
        {
            attacker.addPoints(attackerPoints);
            defender.removeLegion();
            turn.dispose();
            dispose();
        }
    }


    void setupHexes()
    {
        int cx = 6 * scale;
        int cy = 3 * scale;

        // Initialize hexes.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    h[i][j] = new Hex
                        ((int) Math.round(cx + 3 * i * scale),
                        (int) Math.round(cy + (2 * j + (i & 1)) *
                        Hex.SQRT3 * scale), scale, this, i, j);
                }
            }
        }


        // Initialize entrances.
        entrances[0] = new Hex(cx + 15 * scale,
            (int) Math.round(cy + 1 * scale), scale, this, -1, 0);
        entrances[1] = new Hex(cx + 21 * scale,
            (int) Math.round(cy + 10 * scale), scale, this, -1, 1);
        entrances[2] = new Hex(cx + 17 * scale,
            (int) Math.round(cy + 22 * scale), scale, this, -1, 2);
        entrances[3] = new Hex(cx + 2 * scale,
            (int) Math.round(cy + 21 * scale), scale, this, -1, 3);
        entrances[4] = new Hex(cx - 3 * scale,
            (int) Math.round(cy + 10 * scale), scale, this, -1, 4);
        entrances[5] = new Hex(cx + 1 * scale,
            (int) Math.round(cy + 1 * scale), scale, this, -1, 5);


        // Add terrain, hexsides, elevation, and exits to hexes.
        // Cliffs are bidirectional; other hexside obstacles are noted
        // only on the high side, since they only interfere with
        // uphill movement.
        switch (terrain)
        {
            case 'P':
                break;

            case 'W':
                h[0][2].setTerrain('t');
                h[2][3].setTerrain('t');
                h[3][5].setTerrain('t');
                h[4][1].setTerrain('t');
                h[4][3].setTerrain('t');

                h[0][2].setElevation(1);
                h[2][3].setElevation(1);
                h[3][5].setElevation(1);
                h[4][1].setElevation(1);
                h[4][3].setElevation(1);
                break;

            case 'D':
                h[0][3].setTerrain('s');
                h[0][4].setTerrain('s');
                h[1][3].setTerrain('s');
                h[3][0].setTerrain('s');
                h[3][1].setTerrain('s');
                h[3][2].setTerrain('s');
                h[3][5].setTerrain('s');
                h[4][1].setTerrain('s');
                h[4][2].setTerrain('s');
                h[4][5].setTerrain('s');
                h[5][1].setTerrain('s');

                h[0][3].setHexside(0, 'd');
                h[0][3].setHexside(1, 'd');
                h[1][3].setHexside(0, 'd');
                h[1][3].setHexside(1, 'd');
                h[1][3].setHexside(2, 'd');
                h[1][3].setHexside(3, 'c');
                h[3][1].setHexside(4, 'd');
                h[3][2].setHexside(2, 'd');
                h[3][2].setHexside(3, 'c');
                h[3][2].setHexside(4, 'c');
                h[3][2].setHexside(5, 'd');
                h[3][5].setHexside(0, 'd');
                h[3][5].setHexside(5, 'd');
                h[4][2].setHexside(2, 'd');
                h[4][2].setHexside(3, 'd');
                h[4][5].setHexside(0, 'c');
                h[4][5].setHexside(1, 'd');
                h[4][5].setHexside(5, 'd');
                break;

            case 'B':
                h[0][2].setTerrain('r');
                h[1][3].setTerrain('r');
                h[2][2].setTerrain('r');
                h[3][1].setTerrain('r');
                h[3][4].setTerrain('r');
                h[3][5].setTerrain('r');
                h[4][3].setTerrain('r');
                h[5][1].setTerrain('r');
                break;

            case 'J':
                h[0][3].setTerrain('r');
                h[1][1].setTerrain('t');
                h[2][1].setTerrain('r');
                h[2][3].setTerrain('r');
                h[2][5].setTerrain('r');
                h[3][2].setTerrain('r');
                h[3][3].setTerrain('t');
                h[4][4].setTerrain('r');
                h[5][1].setTerrain('r');
                h[5][2].setTerrain('t');

                h[1][1].setElevation(1);
                h[3][3].setElevation(1);
                h[5][2].setElevation(1);
                break;

            case 'M':
                h[0][2].setTerrain('o');
                h[2][3].setTerrain('o');
                h[2][4].setTerrain('o');
                h[3][1].setTerrain('o');
                h[4][3].setTerrain('o');
                h[4][5].setTerrain('o');
                break;

            case 'S':
                h[1][3].setTerrain('o');
                h[2][1].setTerrain('o');
                h[2][2].setTerrain('t');
                h[2][4].setTerrain('t');
                h[3][3].setTerrain('o');
                h[3][5].setTerrain('o');
                h[4][2].setTerrain('t');
                h[5][3].setTerrain('o');

                h[2][2].setElevation(1);
                h[2][4].setElevation(1);
                h[4][2].setElevation(1);
                break;

            case 'H':
                h[2][2].setTerrain('t');
                h[2][4].setTerrain('t');
                h[5][3].setTerrain('t');

                h[1][2].setElevation(1);
                h[1][4].setElevation(1);
                h[2][2].setElevation(1);
                h[2][4].setElevation(1);
                h[3][0].setElevation(1);
                h[3][4].setElevation(1);
                h[4][3].setElevation(1);
                h[5][3].setElevation(1);

                h[1][2].setHexside(0, 's');
                h[1][2].setHexside(1, 's');
                h[1][2].setHexside(2, 's');
                h[1][2].setHexside(3, 's');
                h[1][2].setHexside(4, 's');
                h[1][2].setHexside(5, 's');
                h[1][4].setHexside(0, 's');
                h[1][4].setHexside(1, 's');
                h[1][4].setHexside(2, 's');
                h[1][4].setHexside(5, 's');
                h[3][4].setHexside(0, 's');
                h[3][4].setHexside(1, 's');
                h[3][4].setHexside(2, 's');
                h[3][4].setHexside(3, 's');
                h[3][4].setHexside(4, 's');
                h[3][4].setHexside(5, 's');
                h[4][3].setHexside(0, 's');
                h[4][3].setHexside(1, 's');
                h[4][3].setHexside(2, 's');
                h[4][3].setHexside(3, 's');
                h[4][3].setHexside(4, 's');
                h[4][3].setHexside(5, 's');
                break;

            case 'm':
                h[3][2].setTerrain('v');

                h[0][4].setElevation(1);
                h[1][1].setElevation(1);
                h[1][3].setElevation(1);
                h[1][4].setElevation(2);
                h[2][1].setElevation(2);
                h[2][2].setElevation(1);
                h[2][5].setElevation(1);
                h[3][0].setElevation(2);
                h[3][1].setElevation(1);
                h[3][2].setElevation(2);
                h[3][3].setElevation(1);
                h[4][1].setElevation(1);
                h[4][2].setElevation(1);
                h[4][3].setElevation(1);
                h[5][1].setElevation(2);
                h[5][2].setElevation(1);
                h[5][3].setElevation(2);
                h[5][4].setElevation(1);

                h[0][4].setHexside(0, 's');
                h[1][1].setHexside(3, 's');
                h[1][1].setHexside(4, 's');
                h[1][3].setHexside(0, 's');
                h[1][3].setHexside(1, 's');
                h[1][3].setHexside(2, 's');
                h[1][3].setHexside(5, 's');
                h[1][4].setHexside(0, 's');
                h[1][4].setHexside(1, 'c');
                h[1][4].setHexside(2, 's');
                h[1][4].setHexside(5, 's');
                h[2][1].setHexside(2, 's');
                h[2][1].setHexside(3, 's');
                h[2][1].setHexside(4, 's');
                h[2][2].setHexside(3, 's');
                h[2][2].setHexside(4, 's');
                h[2][5].setHexside(0, 's');
                h[2][5].setHexside(1, 's');
                h[2][5].setHexside(2, 's');
                h[3][0].setHexside(2, 's');
                h[3][0].setHexside(3, 's');
                h[3][2].setHexside(0, 's');
                h[3][2].setHexside(1, 's');
                h[3][2].setHexside(2, 's');
                h[3][2].setHexside(3, 's');
                h[3][2].setHexside(4, 'c');
                h[3][2].setHexside(5, 's');
                h[3][3].setHexside(2, 's');
                h[3][3].setHexside(3, 's');
                h[3][3].setHexside(4, 's');
                h[3][3].setHexside(5, 's');
                h[4][3].setHexside(3, 's');
                h[5][1].setHexside(3, 's');
                h[5][1].setHexside(4, 's');
                h[5][1].setHexside(5, 's');
                h[5][3].setHexside(0, 's');
                h[5][3].setHexside(3, 's');
                h[5][3].setHexside(4, 'c');
                h[5][3].setHexside(5, 's');
                h[5][4].setHexside(4, 's');
                h[5][4].setHexside(5, 's');
                break;

            case 't':
                h[0][4].setTerrain('d');
                h[1][3].setTerrain('d');
                h[2][1].setTerrain('d');
                h[2][2].setTerrain('d');
                h[2][4].setTerrain('d');
                h[3][3].setTerrain('d');
                h[4][2].setTerrain('d');
                h[4][5].setTerrain('d');
                h[5][3].setTerrain('d');
                break;

            case 'T':
                h[2][2].setElevation(1);
                h[2][3].setElevation(1);
                h[3][1].setElevation(1);
                h[3][2].setElevation(2);
                h[3][3].setElevation(1);
                h[4][2].setElevation(1);
                h[4][3].setElevation(1);

                h[2][2].setHexside(0, 'w');
                h[2][2].setHexside(4, 'w');
                h[2][2].setHexside(5, 'w');
                h[2][3].setHexside(3, 'w');
                h[2][3].setHexside(4, 'w');
                h[2][3].setHexside(5, 'w');
                h[3][1].setHexside(0, 'w');
                h[3][1].setHexside(1, 'w');
                h[3][1].setHexside(5, 'w');
                h[3][2].setHexside(0, 'w');
                h[3][2].setHexside(1, 'w');
                h[3][2].setHexside(2, 'w');
                h[3][2].setHexside(3, 'w');
                h[3][2].setHexside(4, 'w');
                h[3][2].setHexside(5, 'w');
                h[3][3].setHexside(2, 'w');
                h[3][3].setHexside(3, 'w');
                h[3][3].setHexside(4, 'w');
                h[4][2].setHexside(0, 'w');
                h[4][2].setHexside(1, 'w');
                h[4][2].setHexside(2, 'w');
                h[4][3].setHexside(1, 'w');
                h[4][3].setHexside(2, 'w');
                h[4][3].setHexside(3, 'w');
                break;
        }


        // Add references to neighbor hexes.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    if (j > 0 && show[i][j - 1])
                    {
                        h[i][j].setNeighbor(0, h[i][j - 1]);
                    }

                    if (i < 5 && show[i + 1][j - ((i + 1) & 1)])
                    {
                        h[i][j].setNeighbor(1, h[i + 1][j - ((i + 1) & 1)]);;
                    }

                    if (i < 5 && j + (i & 1) < 6 && show[i + 1][j + (i & 1)])
                    {
                        h[i][j].setNeighbor(2, h[i + 1][j + (i & 1)]);
                    }

                    if (j < 5 && show[i][j + 1])
                    {
                        h[i][j].setNeighbor(3, h[i][j + 1]);
                    }

                    if (i > 0 && j + (i & 1) < 6 && show[i - 1][j + (i & 1)])
                    {
                        h[i][j].setNeighbor(4, h[i - 1][j + (i & 1)]);
                    }

                    if (i > 0 && show[i - 1][j - ((i + 1) & 1)])
                    {
                        h[i][j].setNeighbor(5, h[i - 1][j - ((i + 1) & 1)]);
                    }
                }
            }
        }


        // Add neighbors to entrances.
        entrances[0].setNeighbor(3, h[3][0]);
        entrances[0].setNeighbor(4, h[4][1]);
        entrances[0].setNeighbor(5, h[5][1]);

        entrances[1].setNeighbor(3, h[5][1]);
        entrances[1].setNeighbor(4, h[5][2]);
        entrances[1].setNeighbor(5, h[5][3]);
        entrances[1].setNeighbor(0, h[5][4]);

        entrances[2].setNeighbor(4, h[5][4]);
        entrances[2].setNeighbor(5, h[4][5]);
        entrances[2].setNeighbor(0, h[3][5]);

        entrances[3].setNeighbor(5, h[3][5]);
        entrances[3].setNeighbor(0, h[2][5]);
        entrances[3].setNeighbor(1, h[1][4]);
        entrances[3].setNeighbor(2, h[0][4]);

        entrances[4].setNeighbor(0, h[0][4]);
        entrances[4].setNeighbor(1, h[0][3]);
        entrances[4].setNeighbor(2, h[0][2]);

        entrances[5].setNeighbor(1, h[0][2]);
        entrances[5].setNeighbor(2, h[1][1]);
        entrances[5].setNeighbor(3, h[2][1]);
        entrances[5].setNeighbor(4, h[3][0]);
    }


    Hex getAttackerEntrance()
    {
        switch(side)
        {
            case 'l':
                return entrances[5];

            case 'r':
                return entrances[1];

            case 'b':
                return entrances[3];

            default:
                return null;
        }
    }


    Hex getDefenderEntrance()
    {
        switch(side)
        {
            case 'l':
                return entrances[2];

            case 'r':
                return entrances[4];

            case 'b':
                return entrances[0];

            default:
                return null;
        }
    }


    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();
        Player player = turn.getActivePlayer();

        for (int i = 0; i < numChits; i++)
        {
            // Only the active player can move or strike.
            if (chits[i].select(point) && chits[i].getPlayer() == player)
            {
                // Put selected chit at the top of the Z-order.
                if (i != 0)
                {
                    BattleChit tmpchit = chits[i];
                    for (int j = i; j > 0; j--)
                    {
                        chits[j] = chits[j - 1];
                    }
                    chits[0] = tmpchit;
                    chits[0].repaint();
                }

                switch (turn.getPhase())
                {
                    case BattleTurn.MOVE:
                        // Highlight all legal destinations for this chit.
                        showMoves(chits[0]);
                        break;

                    case BattleTurn.FIGHT:
                    case BattleTurn.STRIKEBACK:
                        // Highlight all legal strikes for this chit.
                        highlightStrikes(chits[0]);
                        break;

                    default:
                        break;
                }

                return;
            }
        }

        // No hits on chits, so check map.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j] && h[i][j].isSelected(point))
                {
                    switch (turn.getPhase())
                    {
                        case BattleTurn.MOVE:
                            chits[0].moveToHex(h[i][j]);
                            highlightMovableChits();
                            return;

                        case BattleTurn.FIGHT:
                        case BattleTurn.STRIKEBACK:
                            chits[0].strike(h[i][j].getChit());
                            highlightChitsWithTargets();
                            return;

                        default:
                            return;
                    }
                }
            }
        }

        // No hits on selected hexes, so clean up.
        switch (turn.getPhase())
        {
            case BattleTurn.MOVE:
                highlightMovableChits();
                break;

            case BattleTurn.FIGHT:
            case BattleTurn.STRIKEBACK:
                highlightChitsWithTargets();
                break;

            default:
                break;
       }
    }


    public void mouseDragged(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
    {
    }

    public void mouseMoved(MouseEvent e)
    {
    }

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }


    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
        System.exit(0);
    }

    public void windowDeactivated(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowOpened(WindowEvent e)
    {
    }


    // This is used to fix artifacts from chits outside visible hexes.
    void setEraseFlag()
    {
        eraseFlag = true;
    }


    public void paint(Graphics g)
    {
        // Double-buffer everything.
        update(g);
    }


    public void update(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        Dimension d = getSize();
        rectClip = g.getClipBounds();

        // Create the back buffer only if we don't have a good one.
        if (gBack == null || d.width != offDimension.width ||
            d.height != offDimension.height)
        {
            offDimension = d;
            offImage = createImage(2 * d.width, 2 * d.height);
            gBack = offImage.getGraphics();
        }

        // If the erase flag is set, erase the background.
        if (eraseFlag)
        {
            gBack.clearRect(0, 0, d.width, d.height);
            eraseFlag = false;
        }

        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j] && rectClip.intersects(h[i][j].getBounds()))
                {
                    h[i][j].paint(gBack);
                }
            }
        }

        // Draw chits from back to front.
        for (int i = numChits - 1; i >= 0; i--)
        {
            if (rectClip.intersects(chits[i].getBounds()))
            {
                chits[i].paint(gBack);
            }
        }

        g.drawImage(offImage, 0, 0, this);
    }


    public Dimension getMinimumSize()
    {
        return preferredSize;
    }

    public Dimension getPreferredSize()
    {
        return preferredSize;
    }

    public Dimension getMapSize()
    {
        Rectangle xRect = h[5][3].getBounds();
        Rectangle yRect = h[3][5].getBounds();
        return new Dimension(xRect.x + xRect.width, yRect.y + yRect.height);
    }


    public static void main(String args[])
    {
        Player player1 = new Player("Attacker", null);
        Player player2 = new Player("Defender", null);
        Legion attacker = new Legion(0, 0, chitScale, null, null, null, 7,
            null, Creature.ogre, Creature.troll, Creature.ranger,
            Creature.hydra, Creature.griffon, Creature.angel,
            Creature.warlock, null, player1);
        Legion defender = new Legion(0, 0, chitScale, null, null, null, 6,
            null, Creature.centaur, Creature.lion, Creature.gargoyle,
            Creature.cyclops, Creature.gorgon, Creature.guardian, null, null,
            player2);
        new BattleMap(attacker, defender, 'm', 'b');
    }
}
