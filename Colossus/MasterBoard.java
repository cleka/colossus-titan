import java.awt.*;
import java.awt.event.*;

/**
 * Class MasterBoard implements the GUI for a Titan masterboard.
 * @version $Id$
 * @author David Ripton
 */

public class MasterBoard extends Frame implements MouseListener,
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

    final static private boolean[][] show =
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
        {false, false, false, true, true, false, false, false},
    };
    private Rectangle rectClip;
    private Image offImage;
    private Graphics gBack;
    private Dimension offDimension;
    private MediaTracker tracker;
    private boolean imagesLoaded;
    private static int scale;
    private static Game game;


    public MasterBoard(Game game)
    {
        super("MasterBoard");

        this.game = game;

        scale = 17;

        setLayout(null);
        setSize(getPreferredSize());
        setBackground(java.awt.Color.black);
        addWindowListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        imagesLoaded = false;

        // Initialize the hexmap.
        setupHexes();

        // Each player needs to pick his first legion marker.
        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            do
            {
                new PickMarker(this, game.player[i]);
            }
            while (game.player[i].getSelectedMarker() == null);
            // Update status window to reflect marker taken.
            game.updateStatusScreen();
        }


        // Place initial legions.
        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            // Lookup coords for chit starting from player[i].getTower()
            MasterHex hex = getHexFromLabel(100 * game.player[i].getTower());

            Legion legion = new Legion(0, 0, 3 * scale, 
                game.player[i].getSelectedMarker(), null, this, 8, 
                hex, Creature.titan, Creature.angel, Creature.ogre, 
                Creature.ogre, Creature.centaur, Creature.centaur, 
                Creature.gargoyle, Creature.gargoyle, game.player[i]);

            game.player[i].addLegion(legion);
            hex.addLegion(legion);
        }

        // Update status window to reflect new legions.
        game.updateStatusScreen();

        tracker = new MediaTracker(this);

        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            tracker.addImage(game.player[i].legions[0].chit.getImage(), 0);
        }

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            new MessageBox(this, "waitForAll was interrupted");
        }

        imagesLoaded = true;
        setVisible(true);
        repaint();

        new Turn(this, game, this);
    }


    // Do a brute-force search through the hex array, looking for
    //    a match.  Return the hex.
    static MasterHex getHexFromLabel(int label)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j] && h[i][j].label == label)
                {
                    return h[i][j];
                }
            }
        }

        // Error, so return a bogus hex.
        return new MasterHex(-1, -1, -1, false);
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
                    Rectangle clip = new Rectangle(h[i][j].getBounds());
                    repaint(clip.x, clip.y, clip.width, clip.height);
                }
            }
        }
    }


    // Recursively find conventional moves from this hex.  Select
    //    all legal final destinations.  If block >= 0, go only
    //    that way.  If block == -1, use arches and arrows.  If
    //    block == -2, use only arrows.  Do not double back in
    //    the direction you just came from.
    void findMoves(MasterHex hex, Player player, Legion legion, 
        int roll, int block, int cameFrom)
    {
        // If there are enemy legions in this hex, mark it
        // as a legal move and stop recursing.  If there is
        // also a friendly legion there, just stop recursing.
        if (hex.getNumEnemyLegions(player) > 0)
        {
            if (hex.getNumFriendlyLegions(player) == 0)
            {
                hex.select();
                Rectangle clip = new Rectangle(hex.getBounds());
                repaint(clip.x, clip.y, clip.width, clip.height);
            }
            return;
        }

        if (roll == 0)
        {
            // This hex is the final destination.  Mark it as legal if
            // it is unoccupied by friendly legions.
            for (int i = 0; i < player.getNumLegions(); i++)
            {
                // Account for spin cycles.
                if (player.legions[i].getCurrentHex() == hex &&
                    player.legions[i] != legion)
                {
                    return;
                }
            }
            hex.select();
            Rectangle clip = new Rectangle(hex.getBounds());
            repaint(clip.x, clip.y, clip.width, clip.height);
            return;
        }


        if (block >= 0)
        {
            findMoves(hex.neighbor[block], player, legion, roll - 1, -2,
                (block + 3) % 6);
        }
        else if (block == -1)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.exitType[i] >= MasterHex.ARCH && i != cameFrom)
                {
                    findMoves(hex.neighbor[i], player, legion, roll - 1, -2,
                        (i + 3) % 6);
                }
            }
        }
        else if (block == -2)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.exitType[i] >= MasterHex.ARROW && i != cameFrom)
                {
                    findMoves(hex.neighbor[i], player, legion, roll - 1, -2,
                        (i + 3) % 6);
                }
            }
        }
    }
    
    
    // Recursively find tower teleport moves from this hex.  That's
    // all unoccupied towers except this one, plus all unoccupied
    // hexes within 6 hexes.  Do not double back.
    void findTowerTeleportMoves(MasterHex hex, Player player, Legion legion,
        int roll, int cameFrom)
    {
        // This hex is the final destination.  Mark it as legal if
        // it is unoccupied.

        if (hex.getNumLegions() == 0)
        {
            hex.select();
            Rectangle clip = new Rectangle(hex.getBounds());
            repaint(clip.x, clip.y, clip.width, clip.height);
        }

        if (roll > 0)
        {
            for (int i = 0; i < 6; i++)
            {
                if ((hex.exitType[i] != MasterHex.NONE ||
                   hex.entranceType[i] != MasterHex.NONE) && (i != cameFrom))
                {
                    findTowerTeleportMoves(hex.neighbor[i], player, legion, 
                        roll - 1, (i + 3) % 6);
                }
            }
        }
    }


    void showMoves(Legion legion, Player player)
    {
        unselectAllHexes();

        if (legion.hasMoved())
        {
            return;
        }

        MasterHex hex = legion.getCurrentHex();

        // Conventional moves

        // First, look for a block.
        int block = -1;
        for (int j = 0; j < 6; j++)
        {
            if (hex.exitType[j] == MasterHex.BLOCK)
            {
                // Only this path is allowed. 
                block = j;
            }
        }

        findMoves(hex, player, legion, player.getMovementRoll(), block, -1);

        if (player.getMovementRoll() == 6)
        {
            // Tower teleport
            if (hex.terrain == 'T' && (legion.numCreature(Creature.titan) > 0 ||
                legion.numCreature(Creature.angel) > 0 ||
                legion.numCreature(Creature.archangel) > 0))
            {
                // Mark every unoccupied hex within 6 hexes.
                findTowerTeleportMoves(hex, player, legion, 6, -1);

                // Mark every unoccupied tower.
                for (int tower = 100; tower <= 600; tower += 100)
                {
                    hex = getHexFromLabel(tower);
                    if (hex.getNumLegions() == 0)
                    {
                        hex.select();
                        Rectangle clip = new Rectangle(hex.getBounds());
                        repaint(clip.x, clip.y, clip.width, clip.height);
                    }
                }
            }

            // Titan teleport
            if (player.canTitanTeleport() && 
                legion.numCreature(Creature.titan) > 0)
            {
                // Mark every hex containing an enemy unit. 
                for (int i = 0; i < game.getNumPlayers(); i++)
                {
                    if (game.player[i] != player)
                    {
                        for (int j = 0; j < game.player[i].getNumLegions(); 
                            j++)
                        {
                            game.player[i].legions[j].getCurrentHex().select();
                            Rectangle clip = new Rectangle(h[i][j].
                                getBounds());
                            repaint(clip.x, clip.y, clip.width, clip.height);
                        }
                    }
                }
            }
        }
    }


    void setupHexes()
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
                        * MasterHex.SQRT3 * scale), scale, ((i + j) & 1) == 0);
                }
            }
        }



        // Add terrain types, id labels, and exits to hexes.
        h[0][3].terrain='S';
        h[0][3].label=132;
        h[0][3].exitType[1]=4;

        h[0][4].terrain='P';
        h[0][4].label=133;
        h[0][4].exitType[0]=4;

        h[1][2].terrain='B';
        h[1][2].label=130;
        h[1][2].exitType[1]=4;

        h[1][3].terrain='M';
        h[1][3].label=131;
        h[1][3].exitType[0]=4;
        h[1][3].exitType[2]=2;

        h[1][4].terrain='B';
        h[1][4].label=134;
        h[1][4].exitType[1]=2;
        h[1][4].exitType[5]=4;

        h[1][5].terrain='J';
        h[1][5].label=135;
        h[1][5].exitType[0]=4;

        h[2][1].terrain='D';
        h[2][1].label=128;
        h[2][1].exitType[1]=4;

        h[2][2].terrain='P';
        h[2][2].label=129;
        h[2][2].exitType[0]=4;
        h[2][2].exitType[2]=2;

        h[2][3].terrain='H';
        h[2][3].label=32;
        h[2][3].exitType[3]=4;
        h[2][3].exitType[5]=1;

        h[2][4].terrain='J';
        h[2][4].label=33;
        h[2][4].exitType[2]=4;
        h[2][4].exitType[4]=1;

        h[2][5].terrain='M';
        h[2][5].label=136;
        h[2][5].exitType[1]=2;
        h[2][5].exitType[5]=4;

        h[2][6].terrain='B';
        h[2][6].label=137;
        h[2][6].exitType[0]=4;

        h[3][0].terrain='M';
        h[3][0].label=126;
        h[3][0].exitType[1]=4;

        h[3][1].terrain='B';
        h[3][1].label=127;
        h[3][1].exitType[0]=4;
        h[3][1].exitType[2]=2;

        h[3][2].terrain='T';
        h[3][2].label=500;
        h[3][2].exitType[1]=3;
        h[3][2].exitType[3]=3;
        h[3][2].exitType[5]=3;

        h[3][3].terrain='B';
        h[3][3].label=31;
        h[3][3].exitType[0]=2;
        h[3][3].exitType[4]=4;

        h[3][4].terrain='P';
        h[3][4].label=34;
        h[3][4].exitType[1]=4;
        h[3][4].exitType[3]=2;

        h[3][5].terrain='T';
        h[3][5].label=600;
        h[3][5].exitType[0]=3;
        h[3][5].exitType[2]=3;
        h[3][5].exitType[4]=3;

        h[3][6].terrain='P';
        h[3][6].label=138;
        h[3][6].exitType[1]=2;
        h[3][6].exitType[5]=4;

        h[3][7].terrain='D';
        h[3][7].label=139;
        h[3][7].exitType[0]=4;

        h[4][0].terrain='J';
        h[4][0].label=125;
        h[4][0].exitType[2]=4;

        h[4][1].terrain='J';
        h[4][1].label=26;
        h[4][1].exitType[3]=4;
        h[4][1].exitType[5]=1;

        h[4][2].terrain='M';
        h[4][2].label=27;
        h[4][2].exitType[2]=4;
        h[4][2].exitType[4]=2;

        h[4][3].terrain='W';
        h[4][3].label=30;
        h[4][3].exitType[3]=2;
        h[4][3].exitType[5]=4;

        h[4][4].terrain='D';
        h[4][4].label=35;
        h[4][4].exitType[0]=2;
        h[4][4].exitType[2]=4;

        h[4][5].terrain='B';
        h[4][5].label=38;
        h[4][5].exitType[3]=4;
        h[4][5].exitType[5]=2;

        h[4][6].terrain='W';
        h[4][6].label=39;
        h[4][6].exitType[2]=4;
        h[4][6].exitType[4]=1;

        h[4][7].terrain='M';
        h[4][7].label=140;
        h[4][7].exitType[5]=4;

        h[5][0].terrain='P';
        h[5][0].label=124;
        h[5][0].exitType[1]=4;
        h[5][0].exitType[3]=2;

        h[5][1].terrain='W';
        h[5][1].label=25;
        h[5][1].exitType[0]=1;
        h[5][1].exitType[4]=4;

        h[5][2].terrain='S';
        h[5][2].label=28;
        h[5][2].exitType[1]=2;
        h[5][2].exitType[3]=4;

        h[5][3].terrain='P';
        h[5][3].label=29;
        h[5][3].exitType[2]=2;
        h[5][3].exitType[4]=4;

        h[5][4].terrain='M';
        h[5][4].label=36;
        h[5][4].exitType[1]=2;
        h[5][4].exitType[3]=4;

        h[5][5].terrain='H';
        h[5][5].label=37;
        h[5][5].exitType[2]=2;
        h[5][5].exitType[4]=4;

        h[5][6].terrain='J';
        h[5][6].label=40;
        h[5][6].exitType[1]=4;
        h[5][6].exitType[3]=1;

        h[5][7].terrain='B';
        h[5][7].label=141;
        h[5][7].exitType[0]=2;
        h[5][7].exitType[4]=4;

        h[6][0].terrain='B';
        h[6][0].label=123;
        h[6][0].exitType[2]=4;

        h[6][1].terrain='B';
        h[6][1].label=24;
        h[6][1].exitType[1]=2;
        h[6][1].exitType[5]=4;

        h[6][2].terrain='H';
        h[6][2].label=23;
        h[6][2].exitType[0]=4;
        h[6][2].exitType[4]=2;

        h[6][3].terrain='m';
        h[6][3].label=5000;
        h[6][3].exitType[1]=3;
        h[6][3].exitType[3]=3;
        h[6][3].exitType[5]=1;

        h[6][4].terrain='t';
        h[6][4].label=6000;
        h[6][4].exitType[0]=3;
        h[6][4].exitType[2]=3;
        h[6][4].exitType[4]=1;

        h[6][5].terrain='S';
        h[6][5].label=42;
        h[6][5].exitType[1]=4;
        h[6][5].exitType[5]=2;

        h[6][6].terrain='M';
        h[6][6].label=41;
        h[6][6].exitType[0]=4;
        h[6][6].exitType[2]=2;

        h[6][7].terrain='S';
        h[6][7].label=142;
        h[6][7].exitType[5]=4;

        h[7][0].terrain='M';
        h[7][0].label=122;
        h[7][0].exitType[1]=4;
        h[7][0].exitType[3]=2;

        h[7][1].terrain='T';
        h[7][1].label=400;
        h[7][1].exitType[0]=3;
        h[7][1].exitType[2]=3;
        h[7][1].exitType[4]=3;

        h[7][2].terrain='M';
        h[7][2].label=22;
        h[7][2].exitType[3]=2;
        h[7][2].exitType[5]=4;

        h[7][3].terrain='t';
        h[7][3].label=4000;
        h[7][3].exitType[0]=1;
        h[7][3].exitType[2]=3;
        h[7][3].exitType[4]=3;

        h[7][4].terrain='m';
        h[7][4].label=1000;
        h[7][4].exitType[1]=3;
        h[7][4].exitType[3]=1;
        h[7][4].exitType[5]=3;

        h[7][5].terrain='P';
        h[7][5].label=1;
        h[7][5].exitType[0]=2;
        h[7][5].exitType[2]=4;

        h[7][6].terrain='T';
        h[7][6].label=100;
        h[7][6].exitType[1]=3;
        h[7][6].exitType[3]=3;
        h[7][6].exitType[5]=3;

        h[7][7].terrain='P';
        h[7][7].label=101;
        h[7][7].exitType[0]=2;
        h[7][7].exitType[4]=4;

        h[8][0].terrain='S';
        h[8][0].label=121;
        h[8][0].exitType[2]=4;

        h[8][1].terrain='P';
        h[8][1].label=20;
        h[8][1].exitType[3]=4;
        h[8][1].exitType[5]=2;

        h[8][2].terrain='D';
        h[8][2].label=21;
        h[8][2].exitType[2]=2;
        h[8][2].exitType[4]=4;

        h[8][3].terrain='m';
        h[8][3].label=3000;
        h[8][3].exitType[1]=1;
        h[8][3].exitType[3]=3;
        h[8][3].exitType[5]=3;

        h[8][4].terrain='t';
        h[8][4].label=2000;
        h[8][4].exitType[0]=3;
        h[8][4].exitType[2]=1;
        h[8][4].exitType[4]=3;

        h[8][5].terrain='W';
        h[8][5].label=2;
        h[8][5].exitType[1]=2;
        h[8][5].exitType[3]=4;

        h[8][6].terrain='B';
        h[8][6].label=3;
        h[8][6].exitType[2]=4;
        h[8][6].exitType[4]=2;

        h[8][7].terrain='B';
        h[8][7].label=102;
        h[8][7].exitType[5]=4;

        h[9][0].terrain='B';
        h[9][0].label=120;
        h[9][0].exitType[1]=4;
        h[9][0].exitType[3]=2;

        h[9][1].terrain='J';
        h[9][1].label=19;
        h[9][1].exitType[0]=1;
        h[9][1].exitType[4]=4;

        h[9][2].terrain='W';
        h[9][2].label=16;
        h[9][2].exitType[1]=4;
        h[9][2].exitType[5]=2;

        h[9][3].terrain='P';
        h[9][3].label=15;
        h[9][3].exitType[0]=4;
        h[9][3].exitType[4]=2;

        h[9][4].terrain='M';
        h[9][4].label=8;
        h[9][4].exitType[1]=4;
        h[9][4].exitType[5]=2;

        h[9][5].terrain='D';
        h[9][5].label=7;
        h[9][5].exitType[0]=4;
        h[9][5].exitType[4]=2;

        h[9][6].terrain='H';
        h[9][6].label=4;
        h[9][6].exitType[1]=4;
        h[9][6].exitType[3]=1;

        h[9][7].terrain='M';
        h[9][7].label=103;
        h[9][7].exitType[0]=2;
        h[9][7].exitType[4]=4;

        h[10][0].terrain='P';
        h[10][0].label=119;
        h[10][0].exitType[2]=4;

        h[10][1].terrain='H';
        h[10][1].label=18;
        h[10][1].exitType[1]=1;
        h[10][1].exitType[5]=4;

        h[10][2].terrain='B';
        h[10][2].label=17;
        h[10][2].exitType[0]=4;
        h[10][2].exitType[2]=2;

        h[10][3].terrain='S';
        h[10][3].label=14;
        h[10][3].exitType[3]=2;
        h[10][3].exitType[5]=4;

        h[10][4].terrain='H';
        h[10][4].label=9;
        h[10][4].exitType[0]=2;
        h[10][4].exitType[2]=4;

        h[10][5].terrain='P';
        h[10][5].label=6;
        h[10][5].exitType[1]=2;
        h[10][5].exitType[5]=4;

        h[10][6].terrain='J';
        h[10][6].label=5;
        h[10][6].exitType[0]=4;
        h[10][6].exitType[2]=1;

        h[10][7].terrain='J';
        h[10][7].label=104;
        h[10][7].exitType[5]=4;

        h[11][0].terrain='D';
        h[11][0].label=118;
        h[11][0].exitType[3]=4;

        h[11][1].terrain='M';
        h[11][1].label=117;
        h[11][1].exitType[2]=4;
        h[11][1].exitType[4]=2;

        h[11][2].terrain='T';
        h[11][2].label=300;
        h[11][2].exitType[1]=3;
        h[11][2].exitType[3]=3;
        h[11][2].exitType[5]=3;

        h[11][3].terrain='M';
        h[11][3].label=13;
        h[11][3].exitType[0]=2;
        h[11][3].exitType[4]=4;

        h[11][4].terrain='B';
        h[11][4].label=10;
        h[11][4].exitType[1]=4;
        h[11][4].exitType[3]=2;

        h[11][5].terrain='T';
        h[11][5].label=200;
        h[11][5].exitType[0]=3;
        h[11][5].exitType[2]=3;
        h[11][5].exitType[4]=3;

        h[11][6].terrain='B';
        h[11][6].label=106;
        h[11][6].exitType[3]=4;
        h[11][6].exitType[5]=2;

        h[11][7].terrain='P';
        h[11][7].label=105;
        h[11][7].exitType[4]=4;

        h[12][1].terrain='B';
        h[12][1].label=116;
        h[12][1].exitType[3]=4;

        h[12][2].terrain='P';
        h[12][2].label=115;
        h[12][2].exitType[2]=4;
        h[12][2].exitType[4]=2;

        h[12][3].terrain='J';
        h[12][3].label=12;
        h[12][3].exitType[1]=1;
        h[12][3].exitType[5]=4;

        h[12][4].terrain='W';
        h[12][4].label=11;
        h[12][4].exitType[0]=4;
        h[12][4].exitType[2]=1;

        h[12][5].terrain='M';
        h[12][5].label=108;
        h[12][5].exitType[3]=4;
        h[12][5].exitType[5]=2;

        h[12][6].terrain='D';
        h[12][6].label=107;
        h[12][6].exitType[4]=4;

        h[13][2].terrain='J';
        h[13][2].label=114;
        h[13][2].exitType[3]=4;

        h[13][3].terrain='B';
        h[13][3].label=113;
        h[13][3].exitType[2]=4;
        h[13][3].exitType[4]=2;

        h[13][4].terrain='P';
        h[13][4].label=110;
        h[13][4].exitType[3]=4;
        h[13][4].exitType[5]=2;

        h[13][5].terrain='B';
        h[13][5].label=109;
        h[13][5].exitType[4]=4;

        h[14][3].terrain='M';
        h[14][3].label=112;
        h[14][3].exitType[3]=4;

        h[14][4].terrain='S';
        h[14][4].label=111;
        h[14][4].exitType[4]=4;

        // Derive entrances from exits.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    for (int k = 0; k < 6; k++)
                    {
                        int gateType = h[i][j].exitType[k];
                        if (gateType != 0)
                        {
                            switch(k)
                            {
                                case 0:
                                    h[i][j - 1].entranceType[3] = gateType;
                                    break;
                                case 1:
                                    h[i + 1][j].entranceType[4] = gateType;
                                    break;
                                case 2:
                                    h[i + 1][j].entranceType[5] = gateType;
                                    break;
                                case 3:
                                    h[i][j + 1].entranceType[0] = gateType;
                                    break;
                                case 4:
                                    h[i - 1][j].entranceType[1] = gateType;
                                    break;
                                case 5:
                                    h[i - 1][j].entranceType[2] = gateType;
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
                    if (h[i][j].exitType[0] != MasterHex.NONE ||
                        h[i][j].entranceType[0] != MasterHex.NONE)
                    {
                        h[i][j].neighbor[0] = h[i][j - 1];
                    }
                    if (h[i][j].exitType[1] != MasterHex.NONE ||
                        h[i][j].entranceType[1] != MasterHex.NONE)
                    {
                        h[i][j].neighbor[1] = h[i + 1][j];
                    }
                    if (h[i][j].exitType[2] != MasterHex.NONE ||
                        h[i][j].entranceType[2] != MasterHex.NONE)
                    {
                        h[i][j].neighbor[2] = h[i + 1][j];
                    }
                    if (h[i][j].exitType[3] != MasterHex.NONE ||
                        h[i][j].entranceType[3] != MasterHex.NONE)
                    {
                        h[i][j].neighbor[3] = h[i][j + 1];
                    }
                    if (h[i][j].exitType[4] != MasterHex.NONE ||
                        h[i][j].entranceType[4] != MasterHex.NONE)
                    {
                        h[i][j].neighbor[4] = h[i - 1][j];
                    }
                    if (h[i][j].exitType[5] != MasterHex.NONE ||
                        h[i][j].entranceType[5] != MasterHex.NONE)
                    {
                        h[i][j].neighbor[5] = h[i - 1][j];
                    }
                }
            }
        }

    }


    void rescale(int scale)
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


    public void mouseDragged(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();

        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            Player player = game.player[i];
            for (int j = 0; j < player.getNumLegions(); j++)
            {
                Legion legion = player.legions[j];
                if (legion.chit.select(point))
                {
                    // What to do depends on which mouse button was used
                    // and the current phase of the turn.

                    // Right-click or alt-click means to show the contents
                    // of the legion.
                    if (((e.getModifiers() & InputEvent.BUTTON2_MASK) ==
                        InputEvent.BUTTON2_MASK) || ((e.getModifiers() & 
                        InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK))
                    {
                        ShowLegion showlegion = new ShowLegion(this, legion, 
                            point);
                        return;
                    }
                    else
                    {
                        // Only the current player can manipulate his legions.
                        if (i == game.getActivePlayerNum())
                        {
                            switch(game.getPhase())
                            {
                                case Game.SPLIT:
                                    // Don't allow extra splits in turn 1.
                                    if (game.getTurnNumber() == 1 &&
                                        player.getNumLegions() > 1)
                                    {
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
                                    }
                                    // XXX Repaint only affected areas?
                                    repaint();
                                    return;

                                case Game.MOVE:
                                    // Mark this legion as active.
                                    player.selectLegion(legion);

                                    // Find all legal destinations for this 
                                    // legion and highlight them.
                                    showMoves(legion, player);
                                    return;

                                case Game.FIGHT:
                                    // Fall through, to allow clicking on 
                                    // either engaged legion or the hex.
                                    break;

                                case Game.MUSTER:
                                    if (legion.getHeight() < 7 && 
                                        legion.hasMoved())
                                    {
                                        PickRecruit pickrecruit = new 
                                            PickRecruit(this, legion);
                                    }
                                    // If we recruited, unselect this hex.
                                    if (legion.hasMoved() == false)
                                    {
                                        MasterHex hex = legion.getCurrentHex();
                                        hex.unselect();
                                    }
                                    // XXX Repaint only affected areas?
                                    repaint();
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
                    switch(game.getPhase())
                    {
                        // If we're moving, and have selected a legion which 
                        // has not yet moved, and this hex is a legal 
                        // destination, move the legion here.
                        case Game.MOVE:
                            Legion legion = player.getSelectedLegion();
                            if (legion != null && hex.isSelected())
                            {
                                legion.moveToHex(hex);
                                unselectAllHexes();
                                // XXX Repaint only affected hexes and chits?
                                repaint();
                            }
                            break;

                        // If we're fighting and there is an engagement here,
                        // resolve it.
                        case Game.FIGHT:
                            if (hex.getNumFriendlyLegions(player) > 0 &&
                                hex.getNumEnemyLegions(player) > 0)
                            {
                                Legion attacker = 
                                    hex.getFriendlyLegion(player);
                                Legion defender = 
                                    hex.getEnemyLegion(player);

                                if (defender.canFlee()) 
                                {
                                    new Concede(this, defender, attacker, 
                                        true);
                                }

                                if (hex.getNumEnemyLegions(player) > 0)
                                {
                                    // Either player may concede.  Prompt the
                                    // attacker.  Only bother to prompt the
                                    // defender if he couldn't flee.

                                    new Concede(this, attacker, defender, 
                                        false);
                                    if (hex.getNumFriendlyLegions(player) > 0) 
                                    {
                                        if (defender.canFlee() == false)
                                        {
                                            new Concede(this, defender, 
                                                attacker, false);
                                        }
                                    }

                                        // XXX: Add negotiation and battle
                                }
                            }
                            break;

                        case Game.MUSTER:
                            break;

                        default:
                            break;
                    }

                    Rectangle clip = new Rectangle(hex.getBounds());
                    repaint(clip.x, clip.y, clip.width, clip.height);
                    return;
                }
            }
        }
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


    public void windowActivated(WindowEvent event)
    {
    }

    public void windowClosed(WindowEvent event)
    {
    }

    public void windowClosing(WindowEvent event)
    {
        System.exit(0);
    }

    public void windowDeactivated(WindowEvent event)
    {
    }
    
    public void windowDeiconified(WindowEvent event)
    {
    }

    public void windowIconified(WindowEvent event)
    {
    }

    public void windowOpened(WindowEvent event)
    {
    }


    // Double-buffer everything.
    public void paint(Graphics g)
    {
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
            offImage = createImage(d.width, d.height);
            gBack = offImage.getGraphics();
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

        // Paint in reverse order to make visible z-order match clicks.
        for (int i = game.getNumPlayers() - 1; i >= 0; i--)
        {
            for (int j = game.player[i].getNumLegions() - 1; j >= 0; j--)
            {
                if (rectClip.intersects(
                    game.player[i].legions[j].chit.getBounds()))
                {
                    game.player[i].legions[j].chit.paint(gBack);
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
