import java.awt.*;
import java.awt.event.*;

/**
 * Class MasterBoard implements the GUI for a Titan masterboard.
 * @version $Id$
 * @author David Ripton
 */

class MasterBoard extends Frame implements MouseListener,
    MouseMotionListener, WindowListener
{
    // There are a total of 96 hexes
    // Their Titan labels are:
    // Middle ring: 1-42
    // Outer ring: 101-142
    // Towers: 100, 200, 300, 400, 500, 600
    // Inner ring: 1000, 2000, 3000, 4000, 5000, 6000

    // For easy of mapping to the GUI, they'll be stored
    // in a 15x8 array, with some empty elements.

    private static MasterHex[][] h = new MasterHex[15][8];

    private static final boolean[][] show =
    {
        {false, false, false, true, true, false, false, false},
        {false, false, true, true, true, true, false, false},
        {false, true, true, true, true, true, true, false},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {false, true, true, true, true, true, true, false},
        {false, false, true, true, true, true, false, false},
        {false, false, false, true, true, false, false, false}
    };

    private Image offImage;
    private Graphics offGraphics;
    private Dimension offDimension;
    private MediaTracker tracker;
    private boolean imagesLoaded;
    private static int scale = 17;
    private static Game game;
    private boolean eraseFlag = false;
    private boolean summoningAngel = false;
    private SummonAngel summonAngel;
    private BattleMap map;
    private Turn turn;


    public MasterBoard(Game game, boolean newgame)
    {
        super("MasterBoard");

        this.game = game;

        setLayout(null);

        // Make sure that the board fits on the screen.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        if (d.height < 1000)
        {
            scale = scale * d.height / 1000;
        }

        setSize(getPreferredSize());
        try
        {
            setIconImage(Toolkit.getDefaultToolkit().getImage(
                getClass().getResource(Creature.colossus.getImageName())));
        }
        catch (NullPointerException e)
        {
            System.out.println(e.toString() + " Couldn't find " +
                Creature.colossus.getImageName());
            System.exit(1);
        }

        setBackground(Color.black);

        addWindowListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        imagesLoaded = false;

        // Initialize the hexmap.
        setupHexes();

        // This initialization needs to be skipped if we loaded a
        // game rather than starting a new one.
        if (newgame)
        {
            // Each player needs to pick his first legion marker.
            for (int i = 0; i < game.getNumPlayers(); i++)
            {
                do
                {
                    new PickMarker(this, game.getPlayer(i));
                }
                while (game.getPlayer(i).getSelectedMarker() == null);
                // Update status window to reflect marker taken.
                game.updateStatusScreen();
            }

            // Place initial legions.
            for (int i = 0; i < game.getNumPlayers(); i++)
            {
                // Lookup coords for chit starting from player[i].getTower()
                MasterHex hex = getHexFromLabel(100 *
                    game.getPlayer(i).getTower());
    
                Creature.titan.takeOne();
                Creature.angel.takeOne();
                Creature.ogre.takeOne();
                Creature.ogre.takeOne();
                Creature.centaur.takeOne();
                Creature.centaur.takeOne();
                Creature.gargoyle.takeOne();
                Creature.gargoyle.takeOne();

                Legion legion = new Legion(0, 0, 3 * scale,
                    game.getPlayer(i).getSelectedMarker(), null, this, 8,
                    hex, Creature.titan, Creature.angel, Creature.ogre,
                    Creature.ogre, Creature.centaur, Creature.centaur,
                    Creature.gargoyle, Creature.gargoyle, game.getPlayer(i));
    
                game.getPlayer(i).addLegion(legion);
                hex.addLegion(legion);
            }
        }

        // Update status window to reflect new legions.
        game.updateStatusScreen();

        tracker = new MediaTracker(this);

        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            Player player = game.getPlayer(i);
            for (int j = 0; j < player.getNumLegions(); j++)
            {
                tracker.addImage(player.getLegion(j).getMarker().getImage(), 0);
            }
        }

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            new MessageBox(this, e.toString() + " waitForAll was interrupted");
        }

        imagesLoaded = true;

        if (newgame)
        {
            finishInit(true);
        }
    }


    // These steps need to be delayed if we're loading a game. 
    public void finishInit(boolean newgame)
    {
        setVisible(true);
        repaint();
            
        if (!newgame)
        {
            // Move all legions into their hexes.
            for (int i = 0; i < game.getNumPlayers(); i++)
            {
                Player player = game.getPlayer(i);
                for (int j = 0; j < player.getNumLegions(); j++)
                {
                    Legion legion = player.getLegion(j);
                    MasterHex hex = legion.getCurrentHex();
                    hex.addLegion(legion);
                }
            }
        }

        turn = new Turn(this, game, this);
    }


    public int getScale()
    {
        return scale;
    }


    // Do a brute-force search through the hex array, looking for
    //    a match.  Return the hex.
    public MasterHex getHexFromLabel(int label)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j] && h[i][j].getLabel() == label)
                {
                    return h[i][j];
                }
            }
        }

        // Error, so return a bogus hex.
        System.out.println("Could not find hex " + label);
        return new MasterHex(-1, -1, -1, false, null);
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


    // Clear all entry side and teleport information from all hexes occupied
    // by one or fewer legions.
    public void clearAllNonFriendlyOccupiedEntrySides(Player player)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j] && h[i][j].getNumFriendlyLegions(player) == 0)
                {
                    h[i][j].clearAllEntrySides();
                    h[i][j].clearTeleported();
                }
            }
        }
    }


    // Clear all entry side and teleport information from all hexes.
    public void clearAllEntrySides()
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    h[i][j].clearAllEntrySides();
                    h[i][j].clearTeleported();
                }
            }
        }
    }


    // Recursively find conventional moves from this hex.  Select
    //    all legal final destinations.  If block >= 0, go only
    //    that way.  If block == -1, use arches and arrows.  If
    //    block == -2, use only arrows.  Do not double back in
    //    the direction you just came from.  Return the number of
    //    moves found.
    private int findMoves(MasterHex hex, Player player, Legion legion,
        int roll, int block, int cameFrom, boolean show)
    {
        int count = 0;

        // If there are enemy legions in this hex, mark it
        // as a legal move and stop recursing.  If there is
        // also a friendly legion there, just stop recursing.
        if (hex.getNumEnemyLegions(player) > 0)
        {
            if (hex.getNumFriendlyLegions(player) == 0)
            {
                if (show)
                {
                    hex.select();
                    hex.repaint();

                    // Set the entry side relative to the hex label.
                    hex.setEntrySide((6 + cameFrom - hex.getLabelSide()) % 6);
                }
                
            }
            return count;
        }

        if (roll == 0)
        {
            // This hex is the final destination.  Mark it as legal if
            // it is unoccupied by friendly legions.
            for (int i = 0; i < player.getNumLegions(); i++)
            {
                // Account for spin cycles.
                if (player.getLegion(i).getCurrentHex() == hex &&
                    player.getLegion(i) != legion)
                {
                    return count;
                }
            }
            if (show)
            {
                hex.select();
                hex.repaint();
                // Need to set entry sides even if no possible engagement,
                // for MasterHex.chooseWhetherToTeleport()
                hex.setEntrySide((6 + cameFrom - hex.getLabelSide()) % 6);
            }

            count++;
            return count;
        }


        if (block >= 0)
        {
            count += findMoves(hex.getNeighbor(block), player, legion,
                roll - 1, -2, (block + 3) % 6, show);
        }
        else if (block == -1)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= MasterHex.ARCH && i != cameFrom)
                {
                    count += findMoves(hex.getNeighbor(i), player, legion,
                        roll - 1, -2, (i + 3) % 6, show);
                }
            }
        }
        else if (block == -2)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= MasterHex.ARROW && i != cameFrom)
                {
                    count += findMoves(hex.getNeighbor(i), player, legion,
                        roll - 1, -2, (i + 3) % 6, show);
                }
            }
        }

        return count;
    }


    // Recursively find tower teleport moves from this hex.  That's
    // all unoccupied hexes within 6 hexes.  Teleports to towers
    // are handled separately.  Do not double back.
    private void findTowerTeleportMoves(MasterHex hex, Player player,
        Legion legion, int roll, int cameFrom, boolean show)
    {
        // This hex is the final destination.  Mark it as legal if
        // it is unoccupied.

        if (!hex.isOccupied())
        {
            if (show)
            {
                hex.select();
                hex.repaint();
            }
            // Mover can choose side of entry.
            hex.setTeleported();
        }

        if (roll > 0)
        {
            for (int i = 0; i < 6; i++)
            {
                if (i != cameFrom && (hex.getExitType(i) != MasterHex.NONE ||
                   hex.getEntranceType(i) != MasterHex.NONE))
                {
                    findTowerTeleportMoves(hex.getNeighbor(i), player, legion,
                        roll - 1, (i + 3) % 6, show);
                }
            }
        }
    }

    
    // Return number of legal non-teleport moves.
    public int countMoves(Legion legion)
    {
        return countAndMaybeShowMoves(legion, false);
    }
    
    
    // Return number of legal non-teleport moves.
    public int showMoves(Legion legion)
    {
        return countAndMaybeShowMoves(legion, true);
    }


    // Return number of legal non-teleport moves.
    private int countAndMaybeShowMoves(Legion legion, boolean show)
    {
        if (show)
        {
            unselectAllHexes();
        }

        if (legion.hasMoved())
        {
            return 0;
        }
        
        Player player = legion.getPlayer();

        clearAllNonFriendlyOccupiedEntrySides(player);

        int count = 0;

        MasterHex hex = legion.getCurrentHex();

        // Conventional moves

        // First, look for a block.
        int block = -1;
        for (int j = 0; j < 6; j++)
        {
            if (hex.getExitType(j) == MasterHex.BLOCK)
            {
                // Only this path is allowed.
                block = j;
            }
        }

        count += findMoves(hex, player, legion, player.getMovementRoll(),
            block, -1, show);

        if (player.getMovementRoll() == 6)
        {
            // Tower teleport
            if (hex.getTerrain() == 'T' && legion.numLords() > 0 &&
                player.canTeleport())
            {
                // Mark every unoccupied hex within 6 hexes.
                findTowerTeleportMoves(hex, player, legion, 6, -1, show);

                // Mark every unoccupied tower.
                for (int tower = 100; tower <= 600; tower += 100)
                {
                    hex = getHexFromLabel(tower);
                    if (!hex.isOccupied())
                    {
                        if (show)
                        {
                            hex.select();
                            hex.repaint();
                            // Mover can choose side of entry.
                            hex.setTeleported();
                        }
                    }
                }
            }

            // Titan teleport
            if (player.canTitanTeleport() &&
                legion.numCreature(Creature.titan) > 0)
            {
                // Mark every hex containing an enemy stack that does not
                // already contain a friendly stack.
                for (int i = 0; i < game.getNumPlayers(); i++)
                {
                    if (game.getPlayer(i) != player)
                    {
                        for (int j = 0; j < game.getPlayer(i).getNumLegions();
                            j++)
                        {
                            hex = game.getPlayer(i).getLegion(j).
                                getCurrentHex();
                            if (!hex.isEngagement())
                            {
                                if (show)
                                {
                                    hex.select();
                                    hex.repaint();
                                    // Mover can choose side of entry.
                                    hex.setTeleported();
                                }
                            }
                        }
                    }
                }
            }
        }

        return count;
    }


    public void highlightUnmovedLegions()
    {
        unselectAllHexes();

        Player player = game.getActivePlayer();
        player.unselectLegion();

        for (int i = 0; i < player.getNumLegions(); i++)
        {
            Legion legion = player.getLegion(i);
            if (!legion.hasMoved())
            {
                MasterHex hex = legion.getCurrentHex();
                hex.select();
            }
        }

        repaint();
    }


    // Returns number of engagements found.
    public int highlightEngagements()
    {
        int count = 0;
        Player player = game.getActivePlayer();

        unselectAllHexes();

        for (int i = 0; i < player.getNumLegions(); i++)
        {
            Legion legion = player.getLegion(i);
            MasterHex hex = legion.getCurrentHex();
            if (hex.getNumEnemyLegions(player) > 0)
            {
                count++;
                hex.select();
                hex.repaint();
            }
        }

        return count;
    }


    // Returns number of legions with summonable angels.
    public int highlightSummonableAngels(Legion legion)
    {
        unselectAllHexes();

        Player player = legion.getPlayer();
        player.unselectLegion();

        int count = 0;

        for (int i = 0; i < player.getNumLegions(); i++)
        {
            Legion candidate = player.getLegion(i);
            if (candidate != legion)
            {
                MasterHex hex = candidate.getCurrentHex();
                if ((candidate.numCreature(Creature.angel) > 0 ||
                    candidate.numCreature(Creature.archangel) > 0) &&
                    !hex.isEngagement())
                {

                    count++;
                    hex.select();
                    hex.repaint();
                }
            }
        }

        if (count > 0)
        {
            summoningAngel = true;
        }

        return count;
    }


    // Returns number of legions that can recruit.
    public int highlightPossibleRecruits()
    {
        int count = 0;
        Player player = game.getActivePlayer();

        for (int i = 0; i < player.getNumLegions(); i++)
        {
            Legion legion = player.getLegion(i);
            if (legion.hasMoved() && legion.canRecruit())
            {
                Creature [] recruits = new Creature[5];
                if (PickRecruit.findEligibleRecruits(legion, recruits) >= 1)
                {
                    MasterHex hex = legion.getCurrentHex();
                    hex.select();
                    hex.repaint();
                    count++;
                }
            }
        }

        return count;
    }


    public void finishSummoningAngel()
    {
        summoningAngel = false;
        highlightEngagements();
        summonAngel = null;
        if (map != null)
        {
            map.getTurn().finishSummoningAngel();
        }
    }


    public void setSummonAngel(SummonAngel summonAngel)
    {
        this.summonAngel = summonAngel;
    }


    public void finishBattle()
    {
        show();

        if (summoningAngel && summonAngel != null)
        {
            highlightSummonableAngels(summonAngel.getLegion());
            summonAngel.repaint();
        }
        else
        {
            highlightEngagements();
        }
        map = null;

        turn.setVisible(true);
        turn.setEnabled(true);
    }


    private void setupHexes()
    {
        int cx = 3 * scale;
        int cy = 2 * scale;

        // Initialize hexes.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    h[i][j] = new MasterHex
                        (cx + 4 * i * scale,
                        (int) Math.round(cy + (3 * j + (i & 1) *
                        (1 + 2 * (j / 2)) + ((i + 1) & 1) * 2 * ((j + 1) / 2))
                        * MasterHex.SQRT3 * scale), scale, ((i + j) & 1) == 0,
                        this);
                }
            }
        }



        // Add terrain types, id labels, label sides, and exits to hexes.
        h[0][3].setTerrain('S');
        h[0][3].setLabel(132);
        h[0][3].setLabelSide(2);
        h[0][3].setExitType(1, MasterHex.ARROWS);

        h[0][4].setTerrain('P');
        h[0][4].setLabel(133);
        h[0][4].setLabelSide(1);
        h[0][4].setExitType(0, MasterHex.ARROWS);

        h[1][2].setTerrain('B');
        h[1][2].setLabel(130);
        h[1][2].setLabelSide(2);
        h[1][2].setExitType(1, MasterHex.ARROWS);

        h[1][3].setTerrain('M');
        h[1][3].setLabel(131);
        h[1][3].setLabelSide(5);
        h[1][3].setExitType(0, MasterHex.ARROWS);
        h[1][3].setExitType(2, MasterHex.ARCH);

        h[1][4].setTerrain('B');
        h[1][4].setLabel(134);
        h[1][4].setLabelSide(4);
        h[1][4].setExitType(1, MasterHex.ARCH);
        h[1][4].setExitType(5, MasterHex.ARROWS);

        h[1][5].setTerrain('J');
        h[1][5].setLabel(135);
        h[1][5].setLabelSide(1);
        h[1][5].setExitType(0, MasterHex.ARROWS);

        h[2][1].setTerrain('D');
        h[2][1].setLabel(128);
        h[2][1].setLabelSide(2);
        h[2][1].setExitType(1, MasterHex.ARROWS);

        h[2][2].setTerrain('P');
        h[2][2].setLabel(129);
        h[2][2].setLabelSide(5);
        h[2][2].setExitType(0, MasterHex.ARROWS);
        h[2][2].setExitType(2, MasterHex.ARCH);

        h[2][3].setTerrain('H');
        h[2][3].setLabel(32);
        h[2][3].setLabelSide(2);
        h[2][3].setExitType(3, MasterHex.ARROWS);
        h[2][3].setExitType(5, MasterHex.BLOCK);

        h[2][4].setTerrain('J');
        h[2][4].setLabel(33);
        h[2][4].setLabelSide(1);
        h[2][4].setExitType(2, MasterHex.ARROWS);
        h[2][4].setExitType(4, MasterHex.BLOCK);

        h[2][5].setTerrain('M');
        h[2][5].setLabel(136);
        h[2][5].setLabelSide(4);
        h[2][5].setExitType(1, MasterHex.ARCH);
        h[2][5].setExitType(5, MasterHex.ARROWS);

        h[2][6].setTerrain('B');
        h[2][6].setLabel(137);
        h[2][6].setLabelSide(1);
        h[2][6].setExitType(0, MasterHex.ARROWS);

        h[3][0].setTerrain('M');
        h[3][0].setLabel(126);
        h[3][0].setLabelSide(2);
        h[3][0].setExitType(1, MasterHex.ARROWS);

        h[3][1].setTerrain('B');
        h[3][1].setLabel(127);
        h[3][1].setLabelSide(5);
        h[3][1].setExitType(0, MasterHex.ARROWS);
        h[3][1].setExitType(2, MasterHex.ARCH);

        h[3][2].setTerrain('T');
        h[3][2].setLabel(500);
        h[3][2].setLabelSide(2);
        h[3][2].setExitType(1, MasterHex.ARROW);
        h[3][2].setExitType(3, MasterHex.ARROW);
        h[3][2].setExitType(5, MasterHex.ARROW);

        h[3][3].setTerrain('B');
        h[3][3].setLabel(31);
        h[3][3].setLabelSide(5);
        h[3][3].setExitType(0, MasterHex.ARCH);
        h[3][3].setExitType(4, MasterHex.ARROWS);

        h[3][4].setTerrain('P');
        h[3][4].setLabel(34);
        h[3][4].setLabelSide(4);
        h[3][4].setExitType(1, MasterHex.ARROWS);
        h[3][4].setExitType(3, MasterHex.ARCH);

        h[3][5].setTerrain('T');
        h[3][5].setLabel(600);
        h[3][5].setLabelSide(1);
        h[3][5].setExitType(0, MasterHex.ARROW);
        h[3][5].setExitType(2, MasterHex.ARROW);
        h[3][5].setExitType(4, MasterHex.ARROW);

        h[3][6].setTerrain('P');
        h[3][6].setLabel(138);
        h[3][6].setLabelSide(4);
        h[3][6].setExitType(1, MasterHex.ARCH);
        h[3][6].setExitType(5, MasterHex.ARROWS);

        h[3][7].setTerrain('D');
        h[3][7].setLabel(139);
        h[3][7].setLabelSide(1);
        h[3][7].setExitType(0, MasterHex.ARROWS);

        h[4][0].setTerrain('J');
        h[4][0].setLabel(125);
        h[4][0].setLabelSide(3);
        h[4][0].setExitType(2, MasterHex.ARROWS);

        h[4][1].setTerrain('J');
        h[4][1].setLabel(26);
        h[4][1].setLabelSide(2);
        h[4][1].setExitType(3, MasterHex.ARROWS);
        h[4][1].setExitType(5, MasterHex.BLOCK);

        h[4][2].setTerrain('M');
        h[4][2].setLabel(27);
        h[4][2].setLabelSide(5);
        h[4][2].setExitType(2, MasterHex.ARROWS);
        h[4][2].setExitType(4, MasterHex.ARCH);

        h[4][3].setTerrain('W');
        h[4][3].setLabel(30);
        h[4][3].setLabelSide(2);
        h[4][3].setExitType(3, MasterHex.ARCH);
        h[4][3].setExitType(5, MasterHex.ARROWS);

        h[4][4].setTerrain('D');
        h[4][4].setLabel(35);
        h[4][4].setLabelSide(1);
        h[4][4].setExitType(0, MasterHex.ARCH);
        h[4][4].setExitType(2, MasterHex.ARROWS);

        h[4][5].setTerrain('B');
        h[4][5].setLabel(38);
        h[4][5].setLabelSide(4);
        h[4][5].setExitType(3, MasterHex.ARROWS);
        h[4][5].setExitType(5, MasterHex.ARCH);

        h[4][6].setTerrain('W');
        h[4][6].setLabel(39);
        h[4][6].setLabelSide(1);
        h[4][6].setExitType(2, MasterHex.ARROWS);
        h[4][6].setExitType(4, MasterHex.BLOCK);

        h[4][7].setTerrain('M');
        h[4][7].setLabel(140);
        h[4][7].setLabelSide(0);
        h[4][7].setExitType(5, MasterHex.ARROWS);

        h[5][0].setTerrain('P');
        h[5][0].setLabel(124);
        h[5][0].setLabelSide(0);
        h[5][0].setExitType(1, MasterHex.ARROWS);
        h[5][0].setExitType(3, MasterHex.ARCH);

        h[5][1].setTerrain('W');
        h[5][1].setLabel(25);
        h[5][1].setLabelSide(3);
        h[5][1].setExitType(0, MasterHex.BLOCK);
        h[5][1].setExitType(4, MasterHex.ARROWS);

        h[5][2].setTerrain('S');
        h[5][2].setLabel(28);
        h[5][2].setLabelSide(2);
        h[5][2].setExitType(1, MasterHex.ARCH);
        h[5][2].setExitType(3, MasterHex.ARROWS);

        h[5][3].setTerrain('P');
        h[5][3].setLabel(29);
        h[5][3].setLabelSide(5);
        h[5][3].setExitType(2, MasterHex.ARCH);
        h[5][3].setExitType(4, MasterHex.ARROWS);

        h[5][4].setTerrain('M');
        h[5][4].setLabel(36);
        h[5][4].setLabelSide(4);
        h[5][4].setExitType(1, MasterHex.ARCH);
        h[5][4].setExitType(3, MasterHex.ARROWS);

        h[5][5].setTerrain('H');
        h[5][5].setLabel(37);
        h[5][5].setLabelSide(1);
        h[5][5].setExitType(2, MasterHex.ARCH);
        h[5][5].setExitType(4, MasterHex.ARROWS);

        h[5][6].setTerrain('J');
        h[5][6].setLabel(40);
        h[5][6].setLabelSide(0);
        h[5][6].setExitType(1, MasterHex.ARROWS);
        h[5][6].setExitType(3, MasterHex.BLOCK);

        h[5][7].setTerrain('B');
        h[5][7].setLabel(141);
        h[5][7].setLabelSide(3);
        h[5][7].setExitType(0, MasterHex.ARCH);
        h[5][7].setExitType(4, MasterHex.ARROWS);

        h[6][0].setTerrain('B');
        h[6][0].setLabel(123);
        h[6][0].setLabelSide(3);
        h[6][0].setExitType(2, MasterHex.ARROWS);

        h[6][1].setTerrain('B');
        h[6][1].setLabel(24);
        h[6][1].setLabelSide(0);
        h[6][1].setExitType(1, MasterHex.ARCH);
        h[6][1].setExitType(5, MasterHex.ARROWS);

        h[6][2].setTerrain('H');
        h[6][2].setLabel(23);
        h[6][2].setLabelSide(3);
        h[6][2].setExitType(0, MasterHex.ARROWS);
        h[6][2].setExitType(4, MasterHex.ARCH);

        h[6][3].setTerrain('m');
        h[6][3].setLabel(5000);
        h[6][3].setLabelSide(2);
        h[6][3].setExitType(1, MasterHex.ARROW);
        h[6][3].setExitType(3, MasterHex.ARROW);
        h[6][3].setExitType(5, MasterHex.BLOCK);

        h[6][4].setTerrain('t');
        h[6][4].setLabel(6000);
        h[6][4].setLabelSide(1);
        h[6][4].setExitType(0, MasterHex.ARROW);
        h[6][4].setExitType(2, MasterHex.ARROW);
        h[6][4].setExitType(4, MasterHex.BLOCK);

        h[6][5].setTerrain('S');
        h[6][5].setLabel(42);
        h[6][5].setLabelSide(0);
        h[6][5].setExitType(1, MasterHex.ARROWS);
        h[6][5].setExitType(5, MasterHex.ARCH);

        h[6][6].setTerrain('M');
        h[6][6].setLabel(41);
        h[6][6].setLabelSide(3);
        h[6][6].setExitType(0, MasterHex.ARROWS);
        h[6][6].setExitType(2, MasterHex.ARCH);

        h[6][7].setTerrain('S');
        h[6][7].setLabel(142);
        h[6][7].setLabelSide(0);
        h[6][7].setExitType(5, MasterHex.ARROWS);

        h[7][0].setTerrain('M');
        h[7][0].setLabel(122);
        h[7][0].setLabelSide(0);
        h[7][0].setExitType(1, MasterHex.ARROWS);
        h[7][0].setExitType(3, MasterHex.ARCH);

        h[7][1].setTerrain('T');
        h[7][1].setLabel(400);
        h[7][1].setLabelSide(3);
        h[7][1].setExitType(0, MasterHex.ARROW);
        h[7][1].setExitType(2, MasterHex.ARROW);
        h[7][1].setExitType(4, MasterHex.ARROW);

        h[7][2].setTerrain('M');
        h[7][2].setLabel(22);
        h[7][2].setLabelSide(0);
        h[7][2].setExitType(3, MasterHex.ARCH);
        h[7][2].setExitType(5, MasterHex.ARROWS);

        h[7][3].setTerrain('t');
        h[7][3].setLabel(4000);
        h[7][3].setLabelSide(3);
        h[7][3].setExitType(0, MasterHex.BLOCK);
        h[7][3].setExitType(2, MasterHex.ARROW);
        h[7][3].setExitType(4, MasterHex.ARROW);

        h[7][4].setTerrain('m');
        h[7][4].setLabel(1000);
        h[7][4].setLabelSide(0);
        h[7][4].setExitType(1, MasterHex.ARROW);
        h[7][4].setExitType(3, MasterHex.BLOCK);
        h[7][4].setExitType(5, MasterHex.ARROW);

        h[7][5].setTerrain('P');
        h[7][5].setLabel(1);
        h[7][5].setLabelSide(3);
        h[7][5].setExitType(0, MasterHex.ARCH);
        h[7][5].setExitType(2, MasterHex.ARROWS);

        h[7][6].setTerrain('T');
        h[7][6].setLabel(100);
        h[7][6].setLabelSide(0);
        h[7][6].setExitType(1, MasterHex.ARROW);
        h[7][6].setExitType(3, MasterHex.ARROW);
        h[7][6].setExitType(5, MasterHex.ARROW);

        h[7][7].setTerrain('P');
        h[7][7].setLabel(101);
        h[7][7].setLabelSide(3);
        h[7][7].setExitType(0, MasterHex.ARCH);
        h[7][7].setExitType(4, MasterHex.ARROWS);

        h[8][0].setTerrain('S');
        h[8][0].setLabel(121);
        h[8][0].setLabelSide(3);
        h[8][0].setExitType(2, MasterHex.ARROWS);

        h[8][1].setTerrain('P');
        h[8][1].setLabel(20);
        h[8][1].setLabelSide(0);
        h[8][1].setExitType(3, MasterHex.ARROWS);
        h[8][1].setExitType(5, MasterHex.ARCH);

        h[8][2].setTerrain('D');
        h[8][2].setLabel(21);
        h[8][2].setLabelSide(3);
        h[8][2].setExitType(2, MasterHex.ARCH);
        h[8][2].setExitType(4, MasterHex.ARROWS);

        h[8][3].setTerrain('m');
        h[8][3].setLabel(3000);
        h[8][3].setLabelSide(4);
        h[8][3].setExitType(1, MasterHex.BLOCK);
        h[8][3].setExitType(3, MasterHex.ARROW);
        h[8][3].setExitType(5, MasterHex.ARROW);

        h[8][4].setTerrain('t');
        h[8][4].setLabel(2000);
        h[8][4].setLabelSide(5);
        h[8][4].setExitType(0, MasterHex.ARROW);
        h[8][4].setExitType(2, MasterHex.BLOCK);
        h[8][4].setExitType(4, MasterHex.ARROW);

        h[8][5].setTerrain('W');
        h[8][5].setLabel(2);
        h[8][5].setLabelSide(0);
        h[8][5].setExitType(1, MasterHex.ARCH);
        h[8][5].setExitType(3, MasterHex.ARROWS);

        h[8][6].setTerrain('B');
        h[8][6].setLabel(3);
        h[8][6].setLabelSide(3);
        h[8][6].setExitType(2, MasterHex.ARROWS);
        h[8][6].setExitType(4, MasterHex.ARCH);

        h[8][7].setTerrain('B');
        h[8][7].setLabel(102);
        h[8][7].setLabelSide(0);
        h[8][7].setExitType(5, MasterHex.ARROWS);

        h[9][0].setTerrain('B');
        h[9][0].setLabel(120);
        h[9][0].setLabelSide(0);
        h[9][0].setExitType(1, MasterHex.ARROWS);
        h[9][0].setExitType(3, MasterHex.ARCH);

        h[9][1].setTerrain('J');
        h[9][1].setLabel(19);
        h[9][1].setLabelSide(3);
        h[9][1].setExitType(0, MasterHex.BLOCK);
        h[9][1].setExitType(4, MasterHex.ARROWS);

        h[9][2].setTerrain('W');
        h[9][2].setLabel(16);
        h[9][2].setLabelSide(4);
        h[9][2].setExitType(1, MasterHex.ARROWS);
        h[9][2].setExitType(5, MasterHex.ARCH);

        h[9][3].setTerrain('P');
        h[9][3].setLabel(15);
        h[9][3].setLabelSide(1);
        h[9][3].setExitType(0, MasterHex.ARROWS);
        h[9][3].setExitType(4, MasterHex.ARCH);

        h[9][4].setTerrain('M');
        h[9][4].setLabel(8);
        h[9][4].setLabelSide(2);
        h[9][4].setExitType(1, MasterHex.ARROWS);
        h[9][4].setExitType(5, MasterHex.ARCH);

        h[9][5].setTerrain('D');
        h[9][5].setLabel(7);
        h[9][5].setLabelSide(5);
        h[9][5].setExitType(0, MasterHex.ARROWS);
        h[9][5].setExitType(4, MasterHex.ARCH);

        h[9][6].setTerrain('H');
        h[9][6].setLabel(4);
        h[9][6].setLabelSide(0);
        h[9][6].setExitType(1, MasterHex.ARROWS);
        h[9][6].setExitType(3, MasterHex.BLOCK);

        h[9][7].setTerrain('M');
        h[9][7].setLabel(103);
        h[9][7].setLabelSide(3);
        h[9][7].setExitType(0, MasterHex.ARCH);
        h[9][7].setExitType(4, MasterHex.ARROWS);

        h[10][0].setTerrain('P');
        h[10][0].setLabel(119);
        h[10][0].setLabelSide(3);
        h[10][0].setExitType(2, MasterHex.ARROWS);

        h[10][1].setTerrain('H');
        h[10][1].setLabel(18);
        h[10][1].setLabelSide(4);
        h[10][1].setExitType(1, MasterHex.BLOCK);
        h[10][1].setExitType(5, MasterHex.ARROWS);

        h[10][2].setTerrain('B');
        h[10][2].setLabel(17);
        h[10][2].setLabelSide(1);
        h[10][2].setExitType(0, MasterHex.ARROWS);
        h[10][2].setExitType(2, MasterHex.ARCH);

        h[10][3].setTerrain('S');
        h[10][3].setLabel(14);
        h[10][3].setLabelSide(4);
        h[10][3].setExitType(3, MasterHex.ARCH);
        h[10][3].setExitType(5, MasterHex.ARROWS);

        h[10][4].setTerrain('H');
        h[10][4].setLabel(9);
        h[10][4].setLabelSide(5);
        h[10][4].setExitType(0, MasterHex.ARCH);
        h[10][4].setExitType(2, MasterHex.ARROWS);

        h[10][5].setTerrain('P');
        h[10][5].setLabel(6);
        h[10][5].setLabelSide(2);
        h[10][5].setExitType(1, MasterHex.ARCH);
        h[10][5].setExitType(5, MasterHex.ARROWS);

        h[10][6].setTerrain('J');
        h[10][6].setLabel(5);
        h[10][6].setLabelSide(5);
        h[10][6].setExitType(0, MasterHex.ARROWS);
        h[10][6].setExitType(2, MasterHex.BLOCK);

        h[10][7].setTerrain('J');
        h[10][7].setLabel(104);
        h[10][7].setLabelSide(0);
        h[10][7].setExitType(5, MasterHex.ARROWS);

        h[11][0].setTerrain('D');
        h[11][0].setLabel(118);
        h[11][0].setLabelSide(4);
        h[11][0].setExitType(3, MasterHex.ARROWS);

        h[11][1].setTerrain('M');
        h[11][1].setLabel(117);
        h[11][1].setLabelSide(1);
        h[11][1].setExitType(2, MasterHex.ARROWS);
        h[11][1].setExitType(4, MasterHex.ARCH);

        h[11][2].setTerrain('T');
        h[11][2].setLabel(300);
        h[11][2].setLabelSide(4);
        h[11][2].setExitType(1, MasterHex.ARROW);
        h[11][2].setExitType(3, MasterHex.ARROW);
        h[11][2].setExitType(5, MasterHex.ARROW);

        h[11][3].setTerrain('M');
        h[11][3].setLabel(13);
        h[11][3].setLabelSide(1);
        h[11][3].setExitType(0, MasterHex.ARCH);
        h[11][3].setExitType(4, MasterHex.ARROWS);

        h[11][4].setTerrain('B');
        h[11][4].setLabel(10);
        h[11][4].setLabelSide(2);
        h[11][4].setExitType(1, MasterHex.ARROWS);
        h[11][4].setExitType(3, MasterHex.ARCH);

        h[11][5].setTerrain('T');
        h[11][5].setLabel(200);
        h[11][5].setLabelSide(5);
        h[11][5].setExitType(0, MasterHex.ARROW);
        h[11][5].setExitType(2, MasterHex.ARROW);
        h[11][5].setExitType(4, MasterHex.ARROW);

        h[11][6].setTerrain('B');
        h[11][6].setLabel(106);
        h[11][6].setLabelSide(2);
        h[11][6].setExitType(3, MasterHex.ARROWS);
        h[11][6].setExitType(5, MasterHex.ARCH);

        h[11][7].setTerrain('P');
        h[11][7].setLabel(105);
        h[11][7].setLabelSide(5);
        h[11][7].setExitType(4, MasterHex.ARROWS);

        h[12][1].setTerrain('B');
        h[12][1].setLabel(116);
        h[12][1].setLabelSide(4);
        h[12][1].setExitType(3, MasterHex.ARROWS);

        h[12][2].setTerrain('P');
        h[12][2].setLabel(115);
        h[12][2].setLabelSide(1);
        h[12][2].setExitType(2, MasterHex.ARROWS);
        h[12][2].setExitType(4, MasterHex.ARCH);

        h[12][3].setTerrain('J');
        h[12][3].setLabel(12);
        h[12][3].setLabelSide(4);
        h[12][3].setExitType(1, MasterHex.BLOCK);
        h[12][3].setExitType(5, MasterHex.ARROWS);

        h[12][4].setTerrain('W');
        h[12][4].setLabel(11);
        h[12][4].setLabelSide(5);
        h[12][4].setExitType(0, MasterHex.ARROWS);
        h[12][4].setExitType(2, MasterHex.BLOCK);

        h[12][5].setTerrain('M');
        h[12][5].setLabel(108);
        h[12][5].setLabelSide(2);
        h[12][5].setExitType(3, MasterHex.ARROWS);
        h[12][5].setExitType(5, MasterHex.ARCH);

        h[12][6].setTerrain('D');
        h[12][6].setLabel(107);
        h[12][6].setLabelSide(5);
        h[12][6].setExitType(4, MasterHex.ARROWS);

        h[13][2].setTerrain('J');
        h[13][2].setLabel(114);
        h[13][2].setLabelSide(4);
        h[13][2].setExitType(3, MasterHex.ARROWS);

        h[13][3].setTerrain('B');
        h[13][3].setLabel(113);
        h[13][3].setLabelSide(1);
        h[13][3].setExitType(2, MasterHex.ARROWS);
        h[13][3].setExitType(4, MasterHex.ARCH);

        h[13][4].setTerrain('P');
        h[13][4].setLabel(110);
        h[13][4].setLabelSide(2);
        h[13][4].setExitType(3, MasterHex.ARROWS);
        h[13][4].setExitType(5, MasterHex.ARCH);

        h[13][5].setTerrain('B');
        h[13][5].setLabel(109);
        h[13][5].setLabelSide(5);
        h[13][5].setExitType(4, MasterHex.ARROWS);

        h[14][3].setTerrain('M');
        h[14][3].setLabel(112);
        h[14][3].setLabelSide(4);
        h[14][3].setExitType(3, MasterHex.ARROWS);

        h[14][4].setTerrain('S');
        h[14][4].setLabel(111);
        h[14][4].setLabelSide(5);
        h[14][4].setExitType(4, MasterHex.ARROWS);

        // Derive entrances from exits.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    for (int k = 0; k < 6; k++)
                    {
                        int gateType = h[i][j].getExitType(k);
                        if (gateType != 0)
                        {
                            switch (k)
                            {
                                case 0:
                                    h[i][j - 1].setEntranceType(3, gateType);
                                    break;
                                case 1:
                                    h[i + 1][j].setEntranceType(4, gateType);
                                    break;
                                case 2:
                                    h[i + 1][j].setEntranceType(5, gateType);
                                    break;
                                case 3:
                                    h[i][j + 1].setEntranceType(0, gateType);
                                    break;
                                case 4:
                                    h[i - 1][j].setEntranceType(1, gateType);
                                    break;
                                case 5:
                                    h[i - 1][j].setEntranceType(2, gateType);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Add references to neighbor hexes.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    if (h[i][j].getExitType(0) != MasterHex.NONE ||
                        h[i][j].getEntranceType(0) != MasterHex.NONE)
                    {
                        h[i][j].setNeighbor(0, h[i][j - 1]);
                    }
                    if (h[i][j].getExitType(1) != MasterHex.NONE ||
                        h[i][j].getEntranceType(1) != MasterHex.NONE)
                    {
                        h[i][j].setNeighbor(1, h[i + 1][j]);
                    }
                    if (h[i][j].getExitType(2) != MasterHex.NONE ||
                        h[i][j].getEntranceType(2) != MasterHex.NONE)
                    {
                        h[i][j].setNeighbor(2, h[i + 1][j]);
                    }
                    if (h[i][j].getExitType(3) != MasterHex.NONE ||
                        h[i][j].getEntranceType(3) != MasterHex.NONE)
                    {
                        h[i][j].setNeighbor(3, h[i][j + 1]);
                    }
                    if (h[i][j].getExitType(4) != MasterHex.NONE ||
                        h[i][j].getEntranceType(4) != MasterHex.NONE)
                    {
                        h[i][j].setNeighbor(4, h[i - 1][j]);
                    }
                    if (h[i][j].getExitType(5) != MasterHex.NONE ||
                        h[i][j].getEntranceType(5) != MasterHex.NONE)
                    {
                        h[i][j].setNeighbor(5, h[i - 1][j]);
                    }
                }
            }
        }
    }


    private void rescale(int scale)
    {
        this.scale = scale;
        int cx = 3 * scale;
        int cy = 2 * scale;

        setSize(69 * scale, 69 * scale);

        // Initialize hexes.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    h[i][j].rescale(cx, cy, scale);
                }
            }
        }
    }


    public void deiconify()
    {
        // setState(Frame.NORMAL) does not work under 1.1
        // removeNotify() then addNotify() causes problems with the Turn dialog 
    }


    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();

        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            Player player = game.getPlayer(i);
            for (int j = 0; j < player.getNumLegions(); j++)
            {
                Legion legion = player.getLegion(j);
                if (legion.getMarker().select(point))
                {
                    // What to do depends on which mouse button was used
                    // and the current phase of the turn.

                    // Right-click or means to show the contents of the 
                    // legion.
                    if (((e.getModifiers() & InputEvent.BUTTON2_MASK) ==
                        InputEvent.BUTTON2_MASK) || ((e.getModifiers() &
                        InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK))
                    {
                        new ShowLegion(this, legion, point, 
                            i == game.getActivePlayerNum());
                        return;
                    }
                    else
                    {
                        // Only the current player can manipulate his legions.
                        if (i == game.getActivePlayerNum())
                        {
                            switch (game.getPhase())
                            {
                                case Game.SPLIT:
                                    // Need a legion marker to split.
                                    if (player.getNumMarkersAvailable() == 0)
                                    {
                                        new MessageBox(this,
                                            "No markers are available.");
                                        return;
                                    }
                                    // Don't allow extra splits in turn 1.
                                    if (game.getTurnNumber() == 1 &&
                                        player.getNumLegions() > 1)
                                    {
                                        new MessageBox(this,
                                            "Cannot split twice on Turn 1.");
                                        return;
                                    }

                                    new SplitLegion(this, legion, player);
                                    // Update status window.
                                    game.updateStatusScreen();
                                    // If we split, unselect this hex.
                                    if (legion.getHeight() < 7)
                                    {
                                        MasterHex hex = legion.getCurrentHex();
                                        hex.unselect();
                                        hex.repaint();
                                    }
                                    return;

                                case Game.MOVE:
                                    // Mark this legion as active.
                                    player.selectLegion(legion);

                                    // Highlight all legal destinations
                                    // for this legion.
                                    showMoves(legion);
                                    return;

                                case Game.FIGHT:
                                    // Fall through, to allow clicking on
                                    // either engaged legion or the hex.
                                    break;

                                case Game.MUSTER:
                                    if (legion.hasMoved() && 
                                        legion.canRecruit())
                                    {
                                        new PickRecruit(this, legion);
                                    }
                                    // If we recruited, unselect this hex.
                                    if (!legion.canRecruit())
                                    {
                                        legion.getCurrentHex().unselect();
                                        legion.getCurrentHex().repaint();

                                        // Update status window.
                                        game.updateStatusScreen();
                                    }
                                    return;
                            }
                        }
                    }
                }
            }
        }

        // No hits on chits, so check map.

        Player player = game.getActivePlayer();
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j] && h[i][j].contains(point))
                {
                    MasterHex hex = h[i][j];

                    // Right-click or alt-click means to show the contents
                    // of the hex.
                    if (((e.getModifiers() & InputEvent.BUTTON2_MASK) ==
                        InputEvent.BUTTON2_MASK) || ((e.getModifiers() &
                        InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK))
                    {
                        new ShowMasterHex(this, hex, point);
                        return;
                    }

                    // Otherwise, the action to take depends on the phase.
                    switch (game.getPhase())
                    {
                        // If we're moving, and have selected a legion which
                        // has not yet moved, and this hex is a legal
                        // destination, move the legion here.
                        case Game.MOVE:
                            Legion legion = player.getSelectedLegion();
                            if (legion != null && hex.isSelected())
                            {
                                // Pick teleport or normal move if necessary.
                                if (hex.teleported() && hex.canEnterViaLand())
                                {
                                    hex.chooseWhetherToTeleport();
                                }

                                // If this is a tower hex, set the entry side
                                // to '3', regardless.
                                if (hex.getTerrain() == 'T')
                                {
                                    hex.clearAllEntrySides();
                                    hex.setEntrySide(3);
                                }
                                // If this is a teleport to a non-tower hex,
                                // then allow entry from all three sides.
                                else if (hex.teleported())
                                {
                                    hex.setEntrySide(1);
                                    hex.setEntrySide(3);
                                    hex.setEntrySide(5);
                                }

                                // Pick entry side if hex is enemy-occupied
                                // and there is more than one possibility.
                                if (hex.isOccupied() &&
                                    hex.getNumEntrySides() > 1)
                                {
                                    new PickEntrySide(this, hex);
                                }

                                // Unless the PickEntrySide was cancelled,
                                // execute the move.
                                if (!hex.isOccupied() ||
                                    hex.getNumEntrySides() == 1)
                                {
                                    // If the legion teleported, reveal a lord.
                                    if (hex.teleported())
                                    {

                                        // If it was a Titan teleport, that 
                                        // lord must be the titan.
                                        if (hex.isOccupied())
                                        {
                                            legion.revealCreatures(
                                                Creature.titan, 1);
                                        }
                                        else
                                        {
                                            legion.revealTeleportingLord(this);
                                        }
                                    }

                                    legion.moveToHex(hex);
                                    legion.getStartingHex().repaint();
                                    hex.repaint();
                                }

                                highlightUnmovedLegions();
                            }
                            else
                            {
                                highlightUnmovedLegions();
                            }
                            break;

                        // If we're fighting and there is an engagement here,
                        // resolve it.  If an angel is being summoned, mark
                        // the donor legion instead.
                        case Game.FIGHT:
                            if (summoningAngel)
                            {
                                Legion attacker =
                                    hex.getFriendlyLegion(player);
                                player.selectLegion(attacker);
                                if (summonAngel == null)
                                {
                                    summonAngel =
                                        map.getTurn().getSummonAngel();
                                }
                                summonAngel.repaint();
                            }

                            // Do not allow clicking on engagements if one is
                            // already being resolved.
                            else if (hex.isEngagement() && map == null)
                            {
                                Legion attacker =
                                    hex.getFriendlyLegion(player);
                                Legion defender =
                                    hex.getEnemyLegion(player);

                                if (defender.canFlee())
                                {
                                    // Fleeing gives half points and denies the
                                    // attacker the chance to summon an angel.
                                    new Concede(this, defender, attacker,
                                        true);
                                }

                                if (hex.isEngagement())
                                {
                                    // The attacker may concede now without
                                    // allowing the defender a reinforcement.

                                    new Concede(this, attacker, defender,
                                        false);

                                    // The players may agree to a negotiated
                                    // settlement.
                                    if (hex.isEngagement())
                                    {
                                        new Negotiate(this, attacker,
                                            defender);
                                    }


                                    if (!hex.isEngagement())
                                    {
                                        if (hex.getLegion(0) == defender &&
                                            defender.canRecruit())
                                        {
                                            // If the defender won the battle
                                            // by agreement, he may recruit.
                                            new PickRecruit(this, defender);
                                        }
                                        else if (hex.getLegion(0) == attacker
                                            && attacker.getHeight() < 7
                                            && player.canSummonAngel())
                                        {
                                            // If the attacker won the battle
                                            // by agreement, he may summon an
                                            // angel.
                                            summonAngel = new
                                                SummonAngel(this, attacker);
                                        }
                                    }

                                    // Battle
                                    if (hex.isEngagement())
                                    {
                                        // Hide turn to keep it out of the way.
                                        turn.setVisible(false);
                                        turn.setEnabled(false);

                                        // Reveal both legions to all players.
                                        attacker.revealAllCreatures();
                                        defender.revealAllCreatures();
                                        map = new BattleMap(this, attacker,
                                            defender, hex, hex.getEntrySide());
                                    }
                                }

                                highlightEngagements();
                            }
                            break;

                        default:
                            break;
                    }

                    hex.repaint();
                    return;
                }
            }
        }

        // No hits on chits or map, so re-highlight.
        switch (game.getPhase())
        {
            case Game.MOVE:
                highlightUnmovedLegions();
                break;

            case Game.FIGHT:
                if (summoningAngel && summonAngel != null)
                {
                    highlightSummonableAngels(summonAngel.getLegion());
                    summonAngel.repaint();
                }
                else
                {
                    highlightEngagements();
                }
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


    // Double-buffer everything.
    public void paint(Graphics g)
    {
        update(g);
    }


    // This is used to fix artifacts from legions hanging outside hexes.
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

        Dimension d = getSize();
        Rectangle rectClip = g.getClipBounds();

        // Create the back buffer only if we don't have a good one.
        if (offGraphics == null || d.width != offDimension.width ||
            d.height != offDimension.height)
        {
            offDimension = d;
            offImage = createImage(d.width, d.height);
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

        // Paint in reverse order to make visible z-order match clicks.
        for (int i = game.getNumPlayers() - 1; i >= 0; i--)
        {
            Player player = game.getPlayer(i);
            for (int j = player.getNumLegions() - 1; j >= 0; j--)
            {
                if (rectClip.intersects(
                    player.getLegion(j).getMarker().getBounds()))
                {
                    player.getLegion(j).getMarker().paint(offGraphics);
                }
            }
        }

        g.drawImage(offImage, 0, 0, this);
    }


    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }


    public Dimension getPreferredSize()
    {
        return new Dimension(64 * scale, 58 * scale);
    }
}
