import java.awt.*;
import java.awt.event.*;

/**
 * Class BattleMap implements the GUI for a Titan battlemap.
 * @version $Id$
 * @author David Ripton
 */

public class BattleMap extends Frame implements MouseListener,
    WindowListener
{
    private BattleHex[][] h = new BattleHex[6][6];

    // ne, e, se, sw, w, nw
    private BattleHex [] entrances = new BattleHex[6];

    private int numCritters;
    private BattleChit[] chits = new BattleChit[14];
    private Critter lastCritterMoved;

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
    private Graphics offGraphics;
    private Dimension offDimension;
    private MediaTracker tracker;
    private boolean imagesLoaded = false;
    private boolean eraseFlag = false;

    private static int scale;
    private static int chitScale;

    private Legion attacker;
    private Legion defender;

    // 5 is left, 1 is right, 3 is bottom
    private int entrySide;

    private BattleTurn turn;
    private MasterBoard board;
    private MasterHex masterHex;
    private ShowDice showDice;

    // B,D,H,J,m,M,P,S,T,t,W
    // Brush, Desert, Hills, Jungle, mountains, Marsh, Plains,
    // Swamp, Tower, tundra, Woods
    private char terrain;

    private boolean chitSelected = false;
    private int carryDamage = 0;

    public static final int NO_KILLS = 0;
    public static final int FIRST_BLOOD = 1;
    public static final int TOO_LATE = 2;
    private int summonState = NO_KILLS;
    Legion donor = null;
    private static Point location;


    public BattleMap(MasterBoard board, Legion attacker, Legion defender,
        MasterHex masterHex, int entrySide)
    {
        super(attacker.getMarkerId() + " (" + attacker.getPlayer().getName() +
            ") attacks " + defender.getMarkerId() + " (" +
            defender.getPlayer().getName() + ")" + " in " + 
            masterHex.getTerrainName().toLowerCase());

        this.attacker = attacker;
        this.defender = defender;
        this.masterHex = masterHex;
        this.terrain = masterHex.getTerrain();
        this.board = board;
        this.entrySide = entrySide;

        setLayout(null);

        // Make sure the board fits on the screen.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        if (d.height < 1000)
        {
            scale = 30 * d.height / 1000;
        }
        else
        {
            scale = 30;
        }
        chitScale = 2 * scale;

        pack();
        setSize(getPreferredSize());

        setupIcon();

        setBackground(Color.white);
        addWindowListener(this);
        addMouseListener(this);

        validate();

        // Initialize the hexmap.
        setupHexes();

        tracker = new MediaTracker(this);

        int attackerHeight = attacker.getHeight();
        numCritters = attackerHeight + defender.getHeight();

        BattleHex entrance = getEntrance(attacker);
        for (int i = 0; i < attackerHeight; i++)
        {
            Critter critter = attacker.getCritter(i);
            chits[i] = new BattleChit(chitScale, critter.getImageName(false),
                this, critter);
            tracker.addImage(chits[i].getImage(), 0);
            critter.addBattleInfo(entrance, this, chits[i]);
            entrance.addCritter(chits[i].getCritter());
        }
        entrance.alignChits();

        entrance = getEntrance(defender);
        for (int i = attackerHeight; i < numCritters; i++)
        {
            Critter critter = defender.getCritter(i - attackerHeight);
            chits[i] = new BattleChit(chitScale, critter.getImageName(true),
                this, critter);
            tracker.addImage(chits[i].getImage(), 0);
            critter.addBattleInfo(entrance, this, chits[i]);
            entrance.addCritter(chits[i].getCritter());
        }
        entrance.alignChits();

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            new MessageBox(this, e.toString() + " waitForAll was interrupted");
        }
        imagesLoaded = true;

        turn = new BattleTurn(this, this, attacker, defender);
        showDice = new ShowDice(this);

        attacker.clearBattleTally();
        defender.clearBattleTally();

        pack();
        
        if (location == null)
        {
            location = new Point(scale, scale);
        }
        setLocation(location);

        setVisible(true);
        repaint();
    }


    private void setupIcon()
    {
        if (board != null && !board.getGame().isApplet())
        {
            try
            {
                setIconImage(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource(Creature.colossus.getImageName())));
            }
            catch (NullPointerException e)
            {
                System.out.println(e.toString() + " Couldn't find " + 
                    Creature.colossus.getImageName());
                dispose();
            }
        }
    }


    public void placeNewChit(Legion legion)
    {
        imagesLoaded = false;
        tracker = new MediaTracker(this);

        BattleHex entrance = getEntrance(legion);
        int height = legion.getHeight();
        Critter critter = legion.getCritter(height - 1);

        chits[numCritters] = new BattleChit(chitScale,
            critter.getImageName(legion == defender), this, critter);
        tracker.addImage(chits[numCritters].getImage(), 0);
        critter.addBattleInfo(entrance, this, chits[numCritters]);
        entrance.addCritter(chits[numCritters].getCritter());

        numCritters++;

        entrance.alignChits();

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            new MessageBox(this, e.toString() + "waitForAll was interrupted");
        }
        imagesLoaded = true;
    }


    public void unselectAllHexes()
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


    public int highlightMovableChits()
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

        for (int i = 0; i < numCritters; i++)
        {
            Critter critter = chits[i].getCritter();
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


    // Find all legal moves for this chit.
    public void showMoves(Critter critter)
    {
        unselectAllHexes();

        if (!critter.hasMoved() && !critter.inContact(false))
        {
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
        chitSelected = false;

        for (int i = 0; i < numCritters; i++)
        {
            Critter critter = chits[i].getCritter();
            if (critter == lastCritterMoved)
            {
                critter.undoMove();
            }
        }
    }


    public void undoAllMoves()
    {
        chitSelected = false;

        for (int i = 0; i < numCritters; i++)
        {
            Critter critter = chits[i].getCritter();
            if (critter.hasMoved())
            {
                critter.undoMove();
            }
        }
    }


    // If any chits were left off-board, kill them.  If they were newly
    //   summoned or recruited, unsummon or unrecruit them instead.
    public void removeOffboardChits()
    {
        Player player = turn.getActivePlayer();
        for (int i = 0; i < numCritters; i++)
        {
            Critter critter = chits[i].getCritter();
            if (critter.getCurrentHex().isEntrance() &&
                critter.getPlayer() == player)
            {
                critter.setDead(true);
            }
        }
    }


    // Mark all of the conceding player's critters as dead.
    public void concede(Player player)
    {
        for (int i = 0; i < numCritters; i++)
        {
            Critter critter = chits[i].getCritter();
            if (critter.getPlayer() == player)
            {
                critter.setDead(true);
            }
        }
    }


    public void commitMoves()
    {
        clearLastCritterMoved();

        for (int i = 0; i < numCritters; i++)
        {
            Critter critter = chits[i].getCritter();
            critter.commitMove();
        }
    }


    public void applyDriftDamage()
    {
        // Drift hexes are only found on the tundra map.
        if (terrain == 't')
        {
            for (int i = 0; i < numCritters; i++)
            {
                Critter critter = chits[i].getCritter();
                if (critter.getCurrentHex().getTerrain() == 'd' &&
                    !critter.isNativeDrift())
                {
                    int totalDamage = critter.getHits();
                    totalDamage++;
                    critter.setHits(totalDamage);
                    critter.checkForDeath();
                    critter.getChit().repaint();
                }
            }
        }
    }


    public int highlightChitsWithTargets()
    {
        unselectAllHexes();

        int count = 0;
        Player player = turn.getActivePlayer();

        for (int i = 0; i < numCritters; i++)
        {
            Critter critter = chits[i].getCritter();
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
            unselectAllHexes();
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
                BattleHex hex = currentHex.getNeighbor(i);
                if (hex != null && hex.isOccupied())
                {
                    Critter bogie = hex.getCritter();
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

        // Then do rangestrikes if applicable.  Rangestrikes are not allowed
        // if the creature can strike normally.
        if (!critter.inContact(true) && critter.rangeStrikes() &&
            turn.getPhase() != turn.STRIKEBACK)
        {
            for (int i = 0; i < numCritters; i++)
            {
                Critter bogie = chits[i].getCritter();
                if (bogie.getPlayer() != player && !bogie.isDead())
                {
                    BattleHex hex = bogie.getCurrentHex();

                    // Can't rangestrike if it can be struck normally.
                    if (!hex.isSelected())
                    {
                        if (rangestrikePossible(critter, bogie))
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


    public int countStrikes(Critter critter)
    {
        return countAndMaybeHighlightStrikes(critter, false);
    }


    public int highlightStrikes(Critter critter)
    {
        return countAndMaybeHighlightStrikes(critter, true);
    }


    private void setCarryDamage(int damage)
    {
        carryDamage = damage;
    }


    private int getCarryDamage()
    {
        return carryDamage;
    }


    public int highlightCarries(int damage)
    {
        unselectAllHexes();

        int count = 0;

        for (int i = 0; i < numCritters; i++)
        {
            Critter target = chits[i].getCritter();
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
        int totalDamage = target.getHits();
        totalDamage += getCarryDamage();
        int power = target.getPower();
        if (totalDamage > power)
        {
            setCarryDamage(totalDamage - power);
            totalDamage = power;
            target.setCarryFlag(false);
        }
        else
        {
            clearAllCarries();
        }
        target.setHits(totalDamage);
        target.checkForDeath();
        target.getCurrentHex().unselect();
        target.getChit().repaint();
    }


    public void clearAllCarries()
    {
        for (int i = 0; i < numCritters; i++)
        {
            Critter critter = chits[i].getCritter();
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
        Player player = turn.getActivePlayer();

        for (int i = 0; i < numCritters; i++)
        {
            Critter critter = chits[i].getCritter();
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
            if (terrain == 'T' && totalObstacles >= 2 &&
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


    public void commitStrikes()
    {
        for (int i = 0; i < numCritters; i++)
        {
            chits[i].getCritter().commitStrike();
        }
    }


    public MasterBoard getBoard()
    {
        return board;
    }


    public int getSummonState()
    {
        return summonState;
    }


    public void setSummonState(int state)
    {
        summonState = state;
    }


    public void cleanup()
    {
        // Handle any after-battle angel summoning or recruiting.
        if (masterHex.getNumLegions() == 1)
        {
            Legion legion = masterHex.getLegion(0);
            if (legion == attacker)
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
                            attacker);
                        board.setSummonAngel(summonAngel);
                    }
                }
            }
            else
            {
                // Recruit reinforcement
                if (legion.canRecruit())
                {
                    new PickRecruit(this, legion);
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

        // Save location for next object.
        location = getLocation();

        dispose();

        masterHex.unselect();
        masterHex.repaint();
        board.finishBattle();
    }


    public void removeDeadCreatures()
    {
        // Initialize these to true, and then set them to false when a
        // non-dead chit is found.
        boolean attackerElim = true;
        boolean defenderElim = true;

        for (int i = numCritters - 1; i >= 0; i--)
        {
            Critter critter = chits[i].getCritter();
            Legion legion = critter.getLegion();
            if (critter.isDead())
            {
                // After turn 1, offboard creatures are returned to the 
                // stacks or the legion they were summoned from, with 
                // no points awarded.
                if (critter.getCurrentHex().isEntrance() &&
                    turn.getTurnNumber() > 1)
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
                    if (summonState == NO_KILLS &&
                        !critter.getCurrentHex().isEntrance())
                    {
                        summonState = FIRST_BLOOD;
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

                for (int j = i; j < numCritters - 1; j++)
                {
                    chits[j] = chits[j + 1];
                }
                chits[numCritters - 1] = null;
                numCritters--;
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


    public BattleTurn getTurn()
    {
        return turn;
    }


    public ShowDice getShowDice()
    {
        return showDice;
    }


    private void setupHexes()
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
                    h[i][j] = new BattleHex
                        ((int) Math.round(cx + 3 * i * scale),
                        (int) Math.round(cy + (2 * j + (i & 1)) *
                        BattleHex.SQRT3 * scale), scale, this, i, j);
                }
            }
        }


        // Initialize entrances.
        entrances[0] = new BattleHex(cx + 15 * scale,
            (int) Math.round(cy + 1 * scale), scale, this, -1, 0);
        entrances[1] = new BattleHex(cx + 21 * scale,
            (int) Math.round(cy + 10 * scale), scale, this, -1, 1);
        entrances[2] = new BattleHex(cx + 17 * scale,
            (int) Math.round(cy + 22 * scale), scale, this, -1, 2);
        entrances[3] = new BattleHex(cx + 2 * scale,
            (int) Math.round(cy + 21 * scale), scale, this, -1, 3);
        entrances[4] = new BattleHex(cx - 3 * scale,
            (int) Math.round(cy + 10 * scale), scale, this, -1, 4);
        entrances[5] = new BattleHex(cx + 1 * scale,
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
                h[3][0].setHexside(2, 's');
                h[3][0].setHexside(3, 's');
                h[3][0].setHexside(4, 's');
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
                        h[i][j].setNeighbor(1, h[i + 1][j - ((i + 1) & 1)]);
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


    public BattleHex getEntrance(Legion legion)
    {
        if (legion == attacker)
        {
            return entrances[entrySide];
        }
        else
        {
            return entrances[(entrySide + 3) % 6];
        }
    }


    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();
        Player player = turn.getActivePlayer();

        for (int i = 0; i < numCritters; i++)
        {
            // Only the active player can move or strike.
            if (chits[i].select(point) && 
                chits[i].getCritter().getPlayer() == player)
            {
                chitSelected = true;

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
                        showMoves(chits[0].getCritter());
                        break;

                    case BattleTurn.FIGHT:
                    case BattleTurn.STRIKEBACK:
                        // Highlight all legal strikes for this chit.
                        highlightStrikes(chits[0].getCritter());

                        // Leave carry mode.
                        clearAllCarries();
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
                            if (chitSelected)
                            {
                                chits[0].getCritter().moveToHex(h[i][j]);
                                chitSelected = false;
                            }
                            highlightMovableChits();
                            return;

                        case BattleTurn.FIGHT:
                        case BattleTurn.STRIKEBACK:
                            if (getCarryDamage() > 0)
                            {
                                applyCarries(h[i][j].getCritter());
                            }
                            else if (chitSelected)
                            {
                                chits[0].getCritter().strike(
                                    h[i][j].getCritter());
                                chitSelected = false;
                            }

                            if (getCarryDamage() == 0)
                            {
                                highlightChitsWithTargets();
                            }
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


    public void mouseReleased(MouseEvent e)
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
        if (board != null)
        {
            board.disposeGame();
        }
        dispose();
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
    public void setEraseFlag()
    {
        eraseFlag = true;
    }


    public void update(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        // Abort if called too early.
        rectClip = g.getClipBounds();
        if (rectClip == null)
        {
            return;
        }
        
        Dimension d = getSize();

        // Create the back buffer only if we don't have a good one.
        if (offGraphics == null || d.width != offDimension.width ||
            d.height != offDimension.height)
        {
            offDimension = d;
            offImage = createImage(2 * d.width, 2 * d.height);
            offGraphics = offImage.getGraphics();
        }

        // If the erase flag is set, erase the background.
        if (eraseFlag)
        {
            offGraphics.clearRect(0, 0, d.width, d.height);
            eraseFlag = false;
        }

        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j] && rectClip.intersects(h[i][j].getBounds()))
                {
                    h[i][j].paint(offGraphics);
                }
            }
        }

        // Draw chits from back to front.
        for (int i = numCritters - 1; i >= 0; i--)
        {
            if (rectClip.intersects(chits[i].getBounds()))
            {
                chits[i].paint(offGraphics);
            }
        }

        g.drawImage(offImage, 0, 0, this);
    }


    public void paint(Graphics g)
    {
        // Double-buffer everything.
        update(g);
    }


    public Dimension getMinimumSize()
    {
        return new Dimension(30 * scale, 28 * scale);
    }


    public Dimension getPreferredSize()
    {
        return new Dimension(30 * scale, 30 * scale);
    }


    public Dimension getMapSize()
    {
        Rectangle xRect = h[5][3].getBounds();
        Rectangle yRect = h[3][5].getBounds();
        return new Dimension(xRect.x + xRect.width, yRect.y + yRect.height);
    }


    public static void main(String [] args)
    {
        Player player1 = new Player("Attacker", null);
        Player player2 = new Player("Defender", null);
        Legion attacker = new Legion(chitScale, "Bk01", null, null, 7,
            null, Creature.archangel, Creature.troll, Creature.ranger,
            Creature.hydra, Creature.griffon, Creature.angel,
            Creature.warlock, null, player1);
        Legion defender = new Legion(chitScale, "Rd01", null, null, 7,
            null, Creature.serpent, Creature.lion, Creature.gargoyle,
            Creature.cyclops, Creature.gorgon, Creature.guardian,
            Creature.minotaur, null, player2);
        MasterHex hex = new MasterHex(0, 0, 0, false, null);
        hex.setTerrain('J');
        new BattleMap(null, attacker, defender, hex, 3);
    }
}
